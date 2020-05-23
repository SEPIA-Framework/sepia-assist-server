package net.b07z.sepia.server.assist.workers;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.b07z.sepia.server.core.tools.Debugger;

/**
 * This class aims to manage threads in a central way or at least keep a rough overview of what's going on in the framework.
 * 
 * @author Florian Quirin
 *
 */
public class ThreadManager {
	
	private static AtomicInteger activeThreads = new AtomicInteger(0);		//TODO: add ServiceBackgroundTasks
	private static int maxActiveThreads = 0;
	
	/**
	 * Keep a reference of some sort to the running thread (TBD).
	 */
	public static class ThreadInfo {
		//TBD
	}
	
	/**
	 * Get the number of active threads (including [{@link ServiceBackgroundTask}).
	 */
	public static int getNumberOfCurrentlyActiveThreads(){
		int n1 = activeThreads.get();
		int n2 = ServiceBackgroundTaskManager.getNumberOfCurrentlyActiveThreads();
		return n1 + n2;
	}
	/**
	 * Get the maximum number of active threads tracked so far (including [{@link ServiceBackgroundTask}).
	 */
	public static int getMaxNumberOfActiveThreads(){
		int n1 = maxActiveThreads;
		int n2 = ServiceBackgroundTaskManager.getMaxNumberOfActiveThreads();
		return n1 + n2;
	}
	private static void increaseThreadCounter(){
		int n = activeThreads.incrementAndGet();
		if (n > maxActiveThreads) maxActiveThreads = n;
	}
	private static void decreaseThreadCounter(){
		activeThreads.decrementAndGet();
	}

	/**
	 * Run a simple action in a background thread.
	 * @param action - a {@link Runnable}
	 */
	public static ThreadInfo run(Runnable action){
		Thread worker = new Thread(() -> {
			increaseThreadCounter();
			try{
				action.run();
				decreaseThreadCounter();
			
			}catch (Exception e){
				decreaseThreadCounter();
				throw e;
			}
		});
		worker.start();
		return new ThreadInfo();
	}
	
	/**
	 * Run a task that is planned to stay active in background for quite some time or indefinitely.
	 * @param task - task to run
	 * @return reference to thread
	 */
	public static ThreadInfo runForever(Runnable task){
		//TODO: To be implemented
		run(task);
		return new ThreadInfo();
	}
	
	/**
	 * Run several (usually short lived, 1-10s) actions in parallel.
	 * @param <T> - item class
	 * @param customTag - any tag describing this call (handy to find errors)
	 * @param items - collection of items
	 * @param action - action to execute for each item
	 * @param timeout - max. wait until all actions have to complete
	 */
	public static <T> void runParallelAndWait(String customTag, Collection<T> items, Consumer<T> action, long timeout){
		ExecutorService pool = Executors.newCachedThreadPool(); //Executors.newFixedThreadPool(10);
		try{
			for (T item : items){
				pool.execute(() -> {
					try{
						increaseThreadCounter();
						action.accept(item);
						decreaseThreadCounter();
					
					}catch (Exception e){
						decreaseThreadCounter();
						throw e;
					}
				});
			}
			pool.shutdown();
			pool.awaitTermination(timeout, TimeUnit.MILLISECONDS);
		
		}catch (Exception e){
			Debugger.println("ThreadManager - one or more tasks had errors - Tag: " + customTag + " - Msg.: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
		
		}finally{
			try { pool.shutdown(); } catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	/**
	 * Return a scheduled thread pool executor.
	 * @param corePoolSize - size of pool
	 */
	public static ScheduledThreadPoolExecutor getNewScheduledThreadPool(int corePoolSize){
		return new ScheduledThreadPoolExecutor(corePoolSize);
	}
}
