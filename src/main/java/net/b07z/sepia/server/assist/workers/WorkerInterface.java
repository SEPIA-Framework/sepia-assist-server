package net.b07z.sepia.server.assist.workers;

/**
 * A worker is a script that runs constantly in the background to update data like new-feeds or API-data that needs to be buffered.
 * 
 * @author Florian Quirin
 *
 */
public interface WorkerInterface {
	
	/**
	 * Get name of this worker (used in statistics and dynamic setup etc.).
	 */
	public String getName();
	
	/**
	 * Setup the worker (load necessary stuff etc.)
	 */
	public void setup();
	
	/**
	 * Abstract way to get data from this worker given by "key".
	 * @param key - reference to the data (e.g. data might be stored in a map or JSONObject)
	 */
	public Object getData(String key);
	
	/**
	 * Get a status code that indicates the worker-state. E.g.:<br>
	 * -1: offline,<br>
	 *  0: ready to work,<br>
	 *  1: waiting for next action,<br>
	 *  2: refreshing data
	 */
	public int getStatus();
	
	/**
	 * Get a text description for the status code.
	 */
	public String getStatusDescription();

	/**
	 * Time to next action.
	 */
	public long getNextRefreshTime();
	
	/**
	 * Start the worker.
	 */
	public void start();
	/**
	 * Start the worker with a delay (handy if you don't want all workers start at the same time).
	 */
	public void start(long delay);
	
	/**
	 * Use this if you want to make sure that you don't execute any action 
	 * while the worker is refreshing data (state-code 2 usually). 
	 */
	public void waitForWorker();
	
	/**
	 * Stop the worker or at least try to.<br>
	 * This usually works as follows:<br>
	 * Set a flag that tells the worker loop to end (e.g. abort=true),
	 * wait until the worker has finished last cycle (optional),
	 * stop worker and wait a given max. time for success (true/false).
	 * @return true: if the worker stopped within the max. wait-time
	 */
	public boolean kill();
}
