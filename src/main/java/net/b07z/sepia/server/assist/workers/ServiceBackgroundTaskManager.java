package net.b07z.sepia.server.assist.workers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * If a services uses the 'runInBackground' method of {@link net.b07z.sepia.server.assist.services.ServiceBuilder} the tasks will be managed here.
 * 
 * @author Florian Quirin
 *
 */
public class ServiceBackgroundTaskManager {
	
	private static AtomicLong lastUsedBaseId = new AtomicLong(0); 	//NOTE: this will reset on server start but task don't survive restart anyways
	private static AtomicInteger activeThreads = new AtomicInteger(0);
	private static int maxActiveThreads = 0;
	private static Map<String, ServiceBackgroundTask> sbtMap = new ConcurrentHashMap<>();
	
	private static String getNewTaskId(){
		return "sbt" + lastUsedBaseId.incrementAndGet() + "_" + System.currentTimeMillis();
	}
	
	private static void addToSbtMap(String taskId, ServiceBackgroundTask sbt){
		sbtMap.put(taskId, sbt);
	}
	private static void removeFromSbtMap(String taskId){
		sbtMap.remove(taskId);
	}
	private static ServiceBackgroundTask getFromSbtMap(String taskId){
		return sbtMap.get(taskId);
	}
	
	/**
	 * Get the number of scheduled tasks, waiting to be run or currently running (they are removed after they finish).
	 */
	public static int getNumberOfScheduledTasks(){
		return sbtMap.size();
	}
	/**
	 * Get the number of active threads.
	 */
	public static int getNumberOfCurrentlyActiveThreads(){
		return activeThreads.get();
	}
	/**
	 * Get the maximum number of active threads tracked so far.
	 */
	public static int getMaxNumberOfActiveThreads(){
		return maxActiveThreads;
	}
	private static void increaseThreadCounter(){
		int n = activeThreads.incrementAndGet();
		if (n > maxActiveThreads) maxActiveThreads = n;
	}
	private static void decreaseThreadCounter(){
		activeThreads.decrementAndGet();
	}
	
	/**
	 * Cancel all scheduled tasks.<br>
	 * NOTE: This method can only be called from classes inside server package, e.g. {@link Config}!
	 * @return TRUE if cancel requests have been sent (this does not mean it actually worked)
	 */
	public static boolean cancelAllScheduledTasks(){
		String caller = getCallerClassName();
		if (caller.startsWith(Config.class.getPackage().getName())){ 		//TODO: is this a reasonable check???
			for (ServiceBackgroundTask sbt : sbtMap.values()) {
				sbt.cancelTask();
			}
			return true;
		}else{
			Debugger.println(ServiceBackgroundTaskManager.class.getSimpleName() + " - tried to call cancel without permission from: " + caller, 1);
			return false;
		}
	}
	/**
	 * Get a set of scheduled tasks (IDs) that are waiting for execution.<br>
	 * NOTE: This method can only be called from classes inside server package, e.g. {@link Config}!
	 * @return
	 */
	public static Set<String> listAllScheduledTasks(){
		String caller = getCallerClassName();
		if (caller.startsWith(Config.class.getPackage().getName())){ 		//TODO: is this a reasonable check???
			return sbtMap.keySet();
		}else{
			return null;
		}
	}
	
	//Security method
	private static String getCallerClassName(){
		try{
			return new Exception().getStackTrace()[2].getClassName(); 
		}catch(Exception e){
			return null;
		}
	}
	
	//--------------------------------------------------------------------------------------
	
	/**
	 * Run a given task in background and manage task references and threads.
	 * @param userId
	 * @param serviceId
	 * @param delayMs
	 * @param task
	 * @return
	 */
	public static ServiceBackgroundTask runOnceInBackground(String userId, String serviceId, long delayMs, Runnable task){
		//TODO: introduce max. delay ?
		String taskId = getNewTaskId();
		
		int corePoolSize = 1;
	    final ScheduledThreadPoolExecutor executor = ThreadManager.getNewScheduledThreadPool(corePoolSize);
	    executor.setRemoveOnCancelPolicy(true);
	    ScheduledFuture<?> future = executor.schedule(() -> {
	    	//run task and...
	    	try{
	    		increaseThreadCounter();
	    		task.run();
	    		decreaseThreadCounter();
	    	}catch (Exception e){
	    		decreaseThreadCounter();
			}
	    	//... remove yourself from manager
	    	removeFromSbtMap(taskId);
	    	executor.purge();
	    	executor.shutdown();
	    	
	    }, delayMs, TimeUnit.MILLISECONDS);
	    //other option (but does not support lambda expression):
	    //Timer timer = new Timer();
	    //timer.schedule(task, delayMs);
	    
	    BooleanSupplier cancelFun = () -> {
	    	if (future.isDone() || future.cancel(false)){
	    		removeFromSbtMap(taskId);
	    		executor.purge();
		    	executor.shutdown();
	    		return true;
	    	}else{
	    		executor.purge();
		    	executor.shutdown();
	    		return false;
	    	}
	    };
	    
	    ServiceBackgroundTask sbt = new ServiceBackgroundTask(serviceId, taskId, future, cancelFun);
	    addToSbtMap(taskId, sbt);
	    return sbt;
	}
	
	/**
	 * Cancel a scheduled task.<br>
	 * NOTE: Tasks can only be cancelled from within the service that created it (or from master class). 
	 * @param serverId
	 * @param serviceId
	 * @param taskId
	 */
	public static boolean cancelScheduledTask(String serverId, String serviceId, String taskId){
		String caller = getCallerClassName();
		//TODO: is this a reasonable check???
		if (caller.startsWith(serviceId) || caller.startsWith(Config.class.getPackage().getName())){
			ServiceBackgroundTask sbt = getFromSbtMap(taskId);
			if (sbt == null){
				return false;
			}else if (!serverId.equals(Config.localName)){
				//TODO: in future versions this could try to access the correct server automatically
				Debugger.println(ServiceBackgroundTaskManager.class.getSimpleName() + " - tried to call cancel from wrong server ID (cross-server call not supported yet): " + serverId, 1);
				return false;
			}else if (!sbt.getServiceId().equals(serviceId)){
				Debugger.println(ServiceBackgroundTaskManager.class.getSimpleName() + " - tried to call cancel from wrong service ID: " + serviceId, 1);
				return false;
			}else{
				return sbt.cancelTask();
			}
		}else{
			Debugger.println(ServiceBackgroundTaskManager.class.getSimpleName() + " - tried to call cancel without permission from: " + caller, 1);
			return false;
		}
	}
}
