package net.b07z.sepia.server.assist.workers;

import java.util.Set;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.NewsRssFeeds;
import net.b07z.sepia.server.assist.tools.RssFeedReader;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Worker that reads news feeds and caches results.
 * 
 * @author Florian Quirin
 *
 */
public class RssFeedWorker implements WorkerInterface {
	
	//common
	String name = "RSS-feed-worker";
	Thread worker;
	int workerStatus = -1;				//-1: offline, 0: ready to start, 1: waiting for next action, 2: in action
	private String statusDesc = "";		//text description of status
	boolean abort = false;
	long startDelay = 2340;				//start worker after this delay
	long maxWait = 5000;				//maximum wait for kill method
	long waitInterval = 100;			//wait interval during kill request
	long nextRefresh = Long.MAX_VALUE;	//when will the service be refreshed next
	long averageRefreshTime = 10000;	//will be updated during worker runs
	long totalRefreshTime = 0;			//will be updated during worker runs
	long upperMaxRefreshWait = 15000;	//how long is the max wait time for a refresh
	int executedRefreshs = 0;			//how many times has it been executed after start() was called?
	long lastUpdated = 0;				//when has the worker last done an update
	
	//specific
	Set<String> refreshFeeds;
	long customWaitInterval = 2275;			//custom wait time until the worker checks for an abort request and status changes
	public static long customRefreshInterval = (3*60*60*1000);	//every 3h
	
	//even more specific for service
	int maxHeadlinesPerFeed = 8;
	
	//variables
	public JSONObject workerData;
	
	//construct
	public RssFeedWorker(){}
	
	@Override
	public void setup(){
		if (Config.rssReader == null){
			Config.rssReader = new RssFeedReader();
		}
		this.refreshFeeds = NewsRssFeeds.getAllFeeds();
	}
	
	@Override
	public Object getData(String key){
		return workerData.get(key);
	}
	
	@Override
	public String getName(){
		return name;
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
		abort = true;
		long thisWait = 0; 
		while (workerStatus > 0){
			try {	Thread.sleep(waitInterval);	} catch (Exception e){	e.printStackTrace(); return false;	}
			thisWait += waitInterval;
			if (thisWait >= maxWait){
				break;
			}
		}
		if (workerStatus < 1){
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
			try {	Thread.sleep(waitInterval);	} catch (Exception e){	e.printStackTrace(); break;	}
			thisWait += waitInterval;
			if (thisWait >= Math.min(upperMaxRefreshWait, averageRefreshTime)){
				break;
			}
		}
	}
	
	@Override
	public void start(){
		//load backup
		if (Config.rssReader.loadBackup()){
			workerStatus = 0;
			//start with refresh delay
			start(customRefreshInterval);
		}else{
			workerStatus = 0;
			//start with normal delay
			start(startDelay);
		}
	}
	@Override
	public void start(long startDelay){
		worker = new Thread(){
		    public void run(){
		    	workerStatus = 1;
		    	try {	Thread.sleep(startDelay);	} catch (Exception e){	e.printStackTrace(); }
		    	totalRefreshTime = 0;
		    	executedRefreshs = 0;
		    	Debugger.println(name + ": START", 3);
		    	while (!abort){
		    		workerStatus = 2;
			    	long tic = Debugger.tic();
			    	
			    	//WORKER ACTION
			    	workerAction(tic);
			    	
			    	//report
			    	long thisRefreshTime = (System.currentTimeMillis()-tic); 
			    	totalRefreshTime += thisRefreshTime;
			    	executedRefreshs++;
			    	averageRefreshTime = (long)((double)totalRefreshTime/(double)executedRefreshs);
			    	Statistics.addOtherApiHit("Worker: " + name);
					Statistics.addOtherApiTime("Worker: " + name, tic);
					
					//wait for next interval
					workerStatus = 1;
					long thisWait = 0; 
					while(!abort && (thisWait < customRefreshInterval)){
						nextRefresh = customRefreshInterval-thisWait;
						try {	Thread.sleep(customWaitInterval);	} catch (Exception e){	e.printStackTrace(); workerStatus=-1; break; }
						thisWait += customWaitInterval;
					}
		    	}
		    	workerStatus = 0;
		    }
		};
		worker.start();
	}

	//---------- WORKER LOGIC -----------
	
	private boolean workerAction(long tic){
		
		//get new reader
		RssFeedReader newRssReader = new RssFeedReader();
    	
    	//get feeds
    	int goodFeeds = 0;
    	for (String feedName : refreshFeeds){
			String url = NewsRssFeeds.feedUrls.get(feedName);
	        JSONObject feed = newRssReader.getFeed(url, feedName, maxHeadlinesPerFeed, true);
	        if (!feed.isEmpty()){
	        	goodFeeds++;
	        	//Debugger.println("NEWS-API: feed refreshed: " + feedName, 3);
	        }
		}
    	//overwrite old feeds
    	Config.rssReader = newRssReader;
    	lastUpdated = System.currentTimeMillis();
    	
    	Debugger.println(name + ": " + goodFeeds + " feeds have been updated! (" + executedRefreshs + " time(s)) It took (ms): " 
    			+ (System.currentTimeMillis()-tic) + ", average (ms): " + averageRefreshTime, 3);
		
		return true;
	}
}
