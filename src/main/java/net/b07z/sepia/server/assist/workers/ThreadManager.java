package net.b07z.sepia.server.assist.workers;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import net.b07z.sepia.server.core.tools.Debugger;

/**
 * This class aims to manage threads in a central way or at least keep a rough overview of what's going on in the framework.
 * 
 * @author Florian Quirin
 *
 */
public class ThreadManager {
	
	private static AtomicLong activeThreads = new AtomicLong(0);
	
	/**
	 * Keep a reference of some sort to the running thread (TBD).
	 */
	public static class ThreadInfo {
		//TBD
	}
	
	/**
	 * Get the number of active threads.
	 */
	public static long getNumberOfActiveThreads(){
		return activeThreads.get();
	}

	/**
	 * Run a simple action in a background thread.
	 * @param action - a {@link Runnable}
	 */
	public static ThreadInfo run(Runnable action){
		Thread worker = new Thread(() -> {
			activeThreads.incrementAndGet();
			try {
				action.run();
				activeThreads.decrementAndGet();
			
			}catch (Exception e) {
				activeThreads.decrementAndGet();
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
					activeThreads.incrementAndGet();
					action.accept(item);
					activeThreads.decrementAndGet();
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
}
