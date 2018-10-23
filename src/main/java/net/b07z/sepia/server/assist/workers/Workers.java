package net.b07z.sepia.server.assist.workers;

import java.util.HashMap;
import java.util.Map;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class to set-up and run the workers
 * 
 * @author Florian Quirin
 *
 */
public class Workers {
	
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
	
	//A map with all available workers.
	public static Map<String, WorkerInterface> workers;
	
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
		openLigaWorkerBundesliga = new OpenLigaWorker(OpenLigaWorker.BUNDESLIGA, "2018"); 	//TODO: update automatically after season?
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
	 * Get a string that contains some status info about the workers.
	 */
	public static String getStatsReport(){
		String report = "";
		for (String workerName : Config.backgroundWorkers){
			WorkerInterface worker = workers.get(workerName.trim());
			if (worker != null){
				report += 
						"Worker report: " + worker.getName() 
						+ ", status: " + worker.getStatus() + ": " + worker.getStatusDescription() 
						+ ", next refresh: " + Math.round((double)worker.getNextRefreshTime()/(1000)) + "s<br>";
			}
		}
		return report;
	}

}
