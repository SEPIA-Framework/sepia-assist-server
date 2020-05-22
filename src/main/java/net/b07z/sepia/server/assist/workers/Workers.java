package net.b07z.sepia.server.assist.workers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class to set-up and run the workers
 * 
 * @author Florian Quirin
 *
 */
public class Workers {
	//A map with all available workers.
	public static Map<String, WorkerInterface> workers;
	
	//A map with all available connections
	private static Map<String, DuplexConnectionInterface> connections;
	
	//RSS reader and worker
	public static WorkerInterface rssWorker;
	public static String rssFeedsData_BackupFile = Config.xtensionsFolder + "Backups/rssFeedsData.json";
	
	//Sports results worker
	public static WorkerInterface openLigaWorkerBundesliga;
	public static String openLigaData_BackupFile = Config.xtensionsFolder + "Backups/openLigaData.json";
	
	//Backup worker
	public static WorkerInterface backupWorker;
	
	//DuckDNS worker
	public static WorkerInterface duckDnsWorker;
	
	/**
	 * Setup all workers. First get the list from the config-file then construct the workers.<br>
	 * NOTE: Add new workers here to make them available in the framework.
	 */
	public static void setupWorkers(){
		
		//----NOTE: Add new ones here:
		workers = new HashMap<>();
		
		//RSS feeds
		rssWorker = new RssFeedWorker(); 		//uses 'Config.rssReader' by default
		workers.put(rssWorker.getName(), rssWorker);
		
		//openLiga - German soccer: Bundesliga
		openLigaWorkerBundesliga = new OpenLigaWorker(OpenLigaWorker.BUNDESLIGA, OpenLigaWorker.BUNDESLIGA_SEASON);
		workers.put(openLigaWorkerBundesliga.getName(), openLigaWorkerBundesliga);
		
		//Backups
		backupWorker = new JsonBackupWorker();
		workers.put(backupWorker.getName(), backupWorker);
		
		//DuckDNS
		duckDnsWorker = new DuckDnsWorker();
		workers.put(duckDnsWorker.getName(), duckDnsWorker);
		
		//---------------------------
		
		//Setup and start workers to be used
		int activeWorkers = 0;
		for (String workerName : Config.backgroundWorkers){
			WorkerInterface worker = workers.get(workerName.trim());
			if (worker != null){
				worker.setup();
				worker.start();
				activeWorkers++;
			}
		}
		Debugger.println("Active workers: " + activeWorkers, 3);
	}
	
	/**
	 * Register a connection so it can be managed by workers.
	 * @param conn - connection to be added
	 */
	public static void registerConnection(DuplexConnectionInterface conn){
		if (connections == null){
			connections = new ConcurrentHashMap<>();
		}
		connections.put(conn.getName(), conn);
	}
	/**
	 * Try to stop connections that have been registered.
	 */
	public static void closeConnections(){
		if (connections != null){
			for (DuplexConnectionInterface dCon : connections.values()){
				String connName = dCon.getName();
				Debugger.println("Closing connection: " + connName, 3);
				if (dCon.getStatus() == 0){
					dCon.disconnect();
					if (dCon.waitForState(2, -1)){
						Debugger.println("Success: " + connName + " closed", 3);
					}else{
						Debugger.println("Fail: " + connName + " could not be closed in time.", 1);
					}
				}else{
					Debugger.println("Fail: " + connName + " was not connected - status: " + dCon.getStatusDescription(), 3);
				}
				connections.remove(connName);
			}
		}
	}
	
	/**
	 * Try to stop workers and connections that have been started with last setup or during runtime.
	 */
	public static void stopWorkers(){
		//classic workers
		for (String workerName : Config.backgroundWorkers){
			WorkerInterface worker = workers.get(workerName.trim());
			if (worker != null){
				Debugger.println("Stopping worker: " + workerName, 3);
				if (worker.kill()){
					Debugger.println("Success: " + workerName + " stopped", 3);
				}else{
					Debugger.println("Fail: " + workerName + " could not be stopped in time.", 1);
				}
			}
		}
		//duplex connections
		closeConnections();
	}
	
	/**
	 * Get a string that contains some status info about the workers.
	 */
	public static String getStatsReport(){
		String report = "";
		long now = System.currentTimeMillis();
		for (String workerName : Config.backgroundWorkers){
			WorkerInterface worker = workers.get(workerName.trim());
			if (worker != null){
				report += 
						"- Worker report: " + worker.getName() 
						+ ", status: " + worker.getStatus() + ": " + worker.getStatusDescription() 
						+ ", next refresh: " + Math.round((double)worker.getNextRefreshTime()/(1000)) + "s<br>";
			}
		}
		if (connections != null){
			for (DuplexConnectionInterface dCon : connections.values()){
				report += 
						"- Connection report: " + dCon.getName() 
						+ ", status: " + dCon.getStatus() + ": " + dCon.getStatusDescription() 
						+ ", last activity: " + (now - dCon.getLastActivity()) + "ms ago<br>";
			}
		}
		return report;
	}

}
