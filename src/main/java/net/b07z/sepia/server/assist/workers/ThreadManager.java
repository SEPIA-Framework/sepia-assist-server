package net.b07z.sepia.server.assist.workers;

import java.util.concurrent.atomic.AtomicLong;

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
}
