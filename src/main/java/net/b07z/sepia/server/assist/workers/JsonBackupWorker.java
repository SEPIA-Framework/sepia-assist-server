package net.b07z.sepia.server.assist.workers;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.workers.ThreadManager.ThreadInfo;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Worker that makes regular backups of certain data in JSON format.<br>
 * This is also the REFERENCE implementation for workers.
 * 
 * @author Florian Quirin
 *
 */
public class JsonBackupWorker implements WorkerInterface {
	
	//common
	String name = "JSON-Backup-worker";
	ThreadInfo worker;
	int workerStatus = -1;				//-1: offline, 0: ready to start, 1: waiting for next action, 2: in action
	private String statusDesc = "";		//text description of status
	boolean abort = false;
	long startDelay = 60000;			//start worker after this delay
	long maxWait = 10500;				//maximum wait for kill method
	long waitInterval = 100;			//wait interval during kill request
	long nextRefresh = Long.MAX_VALUE;	//when will the service be refreshed next
	long averageRefreshTime = 10000;	//will be updated during worker runs
	long totalRefreshTime = 0;			//will be updated during worker runs
	long upperMaxRefreshWait = 15000;	//how long is the max wait time for a refresh
	int executedRefreshs = 0;			//how many times has it been executed after start() was called?
	long lastUpdated = 0;				//when has the worker last done an update
	
	//specific
	long customWaitInterval = 5000;			//custom wait time until the worker checks for an abort request and status changes
	public static long customRefreshInterval = (24*60*60*1000);	//every 24h
	public static long errorRefreshInterval = (2*60*60*1000);	//2h
	
	//variables
	public JSONObject workerData;
	
	//construct
	public JsonBackupWorker(){}
	
	@Override
	public String getName(){
		return name;
	}
	
	@Override
	public void setup(){
		workerStatus = 0;
	}
	
	@Override
	public Object getData(String key){
		return workerData.get(key);
	}
	
	@Override
	public int getStatus(){
		return workerStatus;
	}
	
	@Override
	public String getStatusDescription(){
		if (workerStatus == -1){
			statusDesc = "offline";
		}else if (workerStatus == 0){
			statusDesc = "ready to work";
		}else if (workerStatus == 1){
			statusDesc = "waiting for next action";
		}else if (workerStatus == 2){
			statusDesc = "refreshing data";
		}else{
			statusDesc = "unknown";
		}
		return statusDesc;
	}
	
	@Override
	public long getNextRefreshTime(){
		return nextRefresh; 
	}
	
	@Override
	public boolean kill(){
		abort = true;		//NOTE: once this flag is set it remains false and the worker is basically dead! Create a new instance afterwards.
		long thisWait = 0; 
		if (executedRefreshs != 0){
			while (workerStatus > 0){
				Debugger.sleep(waitInterval);
				thisWait += waitInterval;
				if (thisWait >= maxWait){
					break;
				}
			}
		}
		if (workerStatus < 1 || executedRefreshs == 0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override 
	public void waitForWorker(){
		//if (nextRefresh > 100){	return;	}
		long thisWait = 0; 
		while (workerStatus == 2){
			Debugger.sleep(waitInterval);
			thisWait += waitInterval;
			if (thisWait >= Math.min(upperMaxRefreshWait, averageRefreshTime)){
				break;
			}
		}
	}
	
	@Override
	public void start(){
		start(startDelay);
	}
	@Override
	public void start(long customStartDelay){
		worker = ThreadManager.runForever(() -> {
	    	workerStatus = 1;
	    	Debugger.sleep(customStartDelay);
	    	totalRefreshTime = 0;
	    	executedRefreshs = 0;
	    	if (!abort){
	    		Debugger.println(name + ": START", 3);
	    	}else{
	    		Debugger.println(name + ": CANCELED before start", 3);
	    	}
	    	boolean success;
	    	while (!abort){
	    		workerStatus = 2;
		    	long tic = Debugger.tic();
		    	
		    	//ACTUAL WORKER ACTION
		    	success = workerAction(tic);
		    	
		    	long refreshInterval = customRefreshInterval;
		    	if (success){
			    	//report
			    	lastUpdated = System.currentTimeMillis();
			    	long thisRefreshTime = (System.currentTimeMillis()-tic); 
			    	totalRefreshTime += thisRefreshTime;
			    	averageRefreshTime = (long)((double)totalRefreshTime/(double)executedRefreshs);
			    	Statistics.addOtherApiHit("Worker: " + name);
					Statistics.addOtherApiTime("Worker: "  + name, tic);
		    	}else{
			    	//report
			    	Statistics.addOtherApiHit("Worker ERRORS: " + name);
					Statistics.addOtherApiTime("Worker ERRORS: " + name, tic);
					refreshInterval = errorRefreshInterval;
		    	}
				
				//wait for next interval
				workerStatus = 1;
				long thisWait = 0; 
				while(!abort && (thisWait < refreshInterval)){
					nextRefresh = refreshInterval-thisWait;
					Debugger.sleep(customWaitInterval);
					thisWait += customWaitInterval;
				}
	    	}
	    	workerStatus = 0;
		});
	}
	
	//---------- WORKER LOGIC -----------
	
	private boolean workerAction(long tic){
		//backup openLigaData
    	boolean backupOpenLigaSuccess = true;
    	if (OpenLigaWorker.openLigaData != null && !OpenLigaWorker.openLigaData.isEmpty()){
    		backupOpenLigaSuccess = JSON.writeJsonToFile(Workers.openLigaData_BackupFile, OpenLigaWorker.openLigaData);
    	}
    	//backup RSS feeds
    	boolean backupRssFeedsSuccess = true;
    	if (Config.rssReader != null && Config.rssReader.getCache() != null && !Config.rssReader.getCache().isEmpty()){
    		backupOpenLigaSuccess = JSON.writeJsonToFile(Workers.rssFeedsData_BackupFile, Config.rssReader.getCache());
    	}
    	
    	boolean success = (backupOpenLigaSuccess && backupRssFeedsSuccess);
    	if (success){
    		executedRefreshs++;
    		Debugger.println(name + ": Data has been stored! (" + executedRefreshs + " time(s)) It took (ms): " 
    				+ (System.currentTimeMillis()-tic) + ", average (ms): " + averageRefreshTime, 3);
    	}else{
    		Debugger.println(name + ": Data backup FAILED!", 3);
    	}
    	return success;
	}

}
