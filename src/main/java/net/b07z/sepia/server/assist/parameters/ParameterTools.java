package net.b07z.sepia.server.assist.parameters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Tools used in parameter classes.
 * 
 * @author Florian Quirin
 */
public class ParameterTools {
	
	//profiler check values
	private static final long performanceThreshold = 100;	//milliseconds
	private static final int allowedThresholdExceeds = 3;
	private static final long timeoutAfterThresholdExceeds = 15000;
	
	//Class to pass along profiler results
	private static class ProfilerResult {
		private AtomicInteger exceededThreshold = new AtomicInteger(0);
		private long lastCall = 0l;
		
		int getExceededThreshold(){
			return exceededThreshold.get();
		}
		void increaseExceededCounter(){
			if (this.exceededThreshold.incrementAndGet() > allowedThresholdExceeds){
				this.exceededThreshold.getAndSet(allowedThresholdExceeds);
			}
		}
		void decreaseExceededCounter(){
			if (this.exceededThreshold.decrementAndGet() < 0){
				this.exceededThreshold.getAndSet(0);
			}
		}
		long getTimePassedSinceLastCall(){
			return (System.currentTimeMillis() - lastCall);
		}
		void updateLastCall(){
			this.lastCall = System.currentTimeMillis();
		}
	}
	
	//profiler states
	private static AtomicLong idPoolForProfiling = new AtomicLong(0);
	private static Map<Long, ProfilerResult> methodProfilingResults = new ConcurrentHashMap<>();
		
	/**
	 * Get a global unique ID that can be used to profile a method call. 
	 * @param nameOfMethod - name to be connected with ID (written to log file)
	 */
	public static Long getNewIdForPerformanceProfiling(String nameOfMethod){
		long id = idPoolForProfiling.incrementAndGet();
		Debugger.println(ParameterTools.class.getSimpleName() + " - Method '" + nameOfMethod + "' has ID: " + id, 3);
		return id;
	}
	
	/**
	 * Run a method if performance is OK or skip it for a certain time. Behavior depends on {@link Config}.parameterPerformanceMode.<br>
	 * Info: Parameter extraction is performance critical, any undesired delay will slow down the whole NLU chain.
	 * Use this method to make sure that undesired delays don't happen too often.   
	 * @param idForProfiling - a unique ID for the run method. Use {@link #getNewIdForPerformanceProfiling} to get one and store it.
	 * @param fun - a method to run and profile. IMPORTANT: a result of 'null' will be handled as error by the profiler (will lead to skip events)
	 * @param argument - argument for the method
	 * @return result of 'fun' or null if skipped
	 */
	public static <T, R> R runOrSkipPerformanceCriticalMethod(Long idForProfiling, Function<T, R> fun, T argument){
		return runOrSkipPerformanceCriticalMethod(idForProfiling, performanceThreshold, fun, argument);
	}
	/**
	 * Run a method if performance is OK or skip it for a certain time. Behavior depends on {@link Config}.parameterPerformanceMode.<br>
	 * Info: Parameter extraction is performance critical, any undesired delay will slow down the whole NLU chain.
	 * Use this method to make sure that undesired delays don't happen too often.   
	 * @param idForProfiling - a unique ID for the run method. Use {@link #getNewIdForPerformanceProfiling} to get one and store it.
	 * @param performanceThresholdMs - time that should normally not be exceeded for this call (ms)
	 * @param fun - a method to run and profile. IMPORTANT: a result of 'null' will be handled as error by the profiler (will lead to skip events)
	 * @param argument - argument for the method
	 * @return result of 'fun' or null if skipped
	 */
	public static <T, R> R runOrSkipPerformanceCriticalMethod(Long idForProfiling, long performanceThresholdMs, Function<T, R> fun, T argument){
		R result;
		if (Config.parameterPerformanceMode == 2){
			//never run
			return null;
		}else if (Config.parameterPerformanceMode == 1){
			//always run (no profiling)
			result = fun.apply(argument);
		}else{
			//profiling with auto-skip
			ProfilerResult pr = methodProfilingResults.get(idForProfiling);
			if (pr == null){
				pr = new ProfilerResult();
				methodProfilingResults.put(idForProfiling, pr);
			}
			if (pr.getExceededThreshold() >= allowedThresholdExceeds){
				if (pr.getTimePassedSinceLastCall() < timeoutAfterThresholdExceeds){
					Debugger.println(ParameterTools.class.getSimpleName() + " - Skipped method due to performance issues - ID: " + idForProfiling, 3);
					return null;
				}
			}
			long tic = System.currentTimeMillis();
			try {
				result = fun.apply(argument);
			} catch (Exception e){
				Debugger.println(ParameterTools.class.getSimpleName() + " - Error in method with ID: " + idForProfiling + " - msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				result = null;
			}
			long toc = System.currentTimeMillis() - tic;
			//System.out.println("Toc: " + toc);							//DEBUG
			if (result == null || toc > performanceThreshold){
				pr.increaseExceededCounter();
			}else{
				pr.decreaseExceededCounter();
			}
			pr.updateLastCall();
		}
		return result;
	}

}
