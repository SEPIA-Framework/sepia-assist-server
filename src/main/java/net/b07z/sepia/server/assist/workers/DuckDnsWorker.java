package net.b07z.sepia.server.assist.workers;

import java.util.Properties;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.ThreadManager;
import net.b07z.sepia.server.core.tools.URLBuilder;
import net.b07z.sepia.server.core.tools.ThreadManager.ThreadInfo;

/**
 * Worker that makes regular calls to DuckDNS to update IP.
 * 
 * @author Florian Quirin
 *
 */
public class DuckDnsWorker implements WorkerInterface {
	//TODO: there is some inconsistency in ALL worker classes about how to use static and instance variables :-| 
	
	//DuckDNS settings - worker will only run if token and domain are set
	public static String configFile = Config.xtensionsFolder + "DynamicDNS/duck-dns.properties"; 
	public static String workerName = "DuckDNS-worker"; 
	
	private String token = "";
	private String domain = "";
	
	//common
	String name = workerName;
	ThreadInfo worker;
	int workerStatus = -1;				//-1: offline, 0: ready to start, 1: waiting for next action, 2: in action
	private String statusDesc = "";		//text description of status
	boolean abort = false;
	long startDelay = 34000;			//start worker after this delay
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
	public static long customRefreshInterval = (5*60*1000);		//every 5min - can be set in config file
	public static long errorRefreshInterval = (10*60*1000);		//every 10min
	public long minTimeToLog = (60*60*1000);	//minimum time to wait until next log entry (handy when refreshes are made frequently, errors are always logged)
	public static long lastLog = 0;
	
	//variables
	public JSONObject workerData;
	
	//construct
	public DuckDnsWorker(){}
	
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
		abort = true;	 	//NOTE: once this flag is set it remains false and the worker is basically dead! Create a new instance afterwards.
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
		//Check if all is set-up properly
		if (!loadSettings()){
			return;
		}
		
		//start
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
	
	private boolean loadSettings(){
		boolean success = false;
		try{
			Properties settings = FilesAndStreams.loadSettings(configFile);
			//server
			token = settings.getProperty("token");	
			domain = settings.getProperty("domain");
			customRefreshInterval = Long.valueOf(settings.getProperty("refresh_time_ms"));
			//check
			if (Is.notNullOrEmpty(token) && Is.notNullOrEmpty(domain) && customRefreshInterval > 0){
				success = true;
			}else{
				success = false;
			}
		}catch (Exception e){
			success = false;
		}
		
		if (success){
			Debugger.println(name + ": loading settings from " + configFile + "... done. Worker will be used." , 3);
			return true;
		}else{
			Debugger.println(name + ": loading settings from " + configFile + "... failed! Please check token and domain settings!" , 1);
			return false;
		}
	}
	
	private boolean workerAction(long tic){
		//call DuckDNS API
    	boolean ipUpdated = false;
    	String error = "";
    	String url = URLBuilder.getString("https://www.duckdns.org/update", 
    			"?domains=", domain,
    			"&verbose=", "true",
    			"&token=", token
    			//[&ip={YOURVALUE}][&ipv6={YOURVALUE}][&clear=true]"
    	);
    	//make update call
    	JSONObject result = Connectors.httpGET(url);
    	//System.out.println("DuckDNS result: " + result); 	//DEBUG
    	
    	//check if it was successful
    	if (Connectors.httpSuccess(result)){
    		if (result.containsKey("STRING") && JSON.getString(result, "STRING").startsWith("OK")){
    			ipUpdated = true;
    		}else{
    			ipUpdated = false;
    			error = "Update was received but not successful. Reason unknown.";
    		}
    	}else{
    		ipUpdated = false;
    		error = "DuckDNS server NOT reached.";
    	}
    	if (ipUpdated){
    		executedRefreshs++;
    		if ((System.currentTimeMillis() - lastLog) > minTimeToLog){
    			lastLog = System.currentTimeMillis();
    			Debugger.println(name + ": IP has been updated! (" + executedRefreshs + " time(s)) It took (ms): " 
        				+ (System.currentTimeMillis()-tic) + ", average (ms): " + averageRefreshTime, 3);
    		}
    	}else{
    		Debugger.println(name + ": IP update FAILED! Info: " + error, 1);
    	}
    	return ipUpdated;
	}

}
