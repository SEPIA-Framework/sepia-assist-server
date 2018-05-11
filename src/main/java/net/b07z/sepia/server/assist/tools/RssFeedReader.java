package net.b07z.sepia.server.assist.tools;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.workers.Workers;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to handle RSS feeds.
 * 
 * @author Florian Quirin
 *
 */
public class RssFeedReader {
	
	//HashMap<String, JSONObject> feedCache;
	//HashMap<String, Long> feedCacheTS;
	JSONObject feedCache;
	JSONObject feedCacheTS;
	
	public RssFeedReader(){
		//feedCache = new HashMap<>();
		//feedCacheTS = new HashMap<>();
		feedCache = new JSONObject();
		feedCacheTS = new JSONObject();
	}
	
	public JSONObject getCache(){
		return feedCache;
	}
	public boolean loadBackup(){
		JSONObject backup = JSON.readJsonFromFile(Workers.rssFeedsData_BackupFile);
		if (backup != null && !backup.isEmpty()){
			this.feedCache = backup;
			Debugger.println("RssFeedReader - backup restored with " + feedCache.size() + " feeds.", 3);
			return true;
		}else{
			return false;
		}
	}
	public JSONObject getCacheTimestamps(){
		return feedCacheTS;
	}
	
	/**
	 * Create a JSONObject holding all feed data in the field "data".  
	 * @param url - where to find the feed
	 * @param feedName - name of the feed, is added as "nameClean" and used to cache the feed
	 * @param maxEntries - max result entries
	 * @param cacheIt - cache the feed for subsequent requests
	 */
	public JSONObject getFeed(String url, String feedName, int maxEntries, boolean cacheIt){
		//TODO: check cache and update feed
		boolean directRefresh = false;
		if (feedCache.containsKey(feedName)){
			Object tso = feedCacheTS.get(feedName);
			long ts = 0l;
			if (tso != null){
				ts = (long) tso;
			}
			//trust the worker to keep the feeds up to date
			if (Workers.rssWorker != null && (Workers.rssWorker.getStatus() > 0)){
				return (JSONObject) feedCache.get(feedName);
			
			//in case the worker crashed or is not active for some reason
			//TODO: what now?
			/*
			}else if (Workers.rssWorker != null){
				Debugger.println("RssFeedReader.getFeed() - WORKER ERROR, status: " + Workers.rssWorker.getStatus(), 1);
				return (JSONObject) feedCache.get(feedName);
			*/
				
			//if there is no worker
			}else if ((System.currentTimeMillis() - ts) < (60 * 60 * 1000)){
				return (JSONObject) feedCache.get(feedName);
			
			}else{
				directRefresh = true;
			}
		}
		
		String statusLine = "";
		String errorRedirect = "";
        JSONObject feedResult = new JSONObject();
        JSONArray feedEntries = new JSONArray();
        
        try {
	        try (CloseableHttpClient client = HttpClients.createMinimal()) {
	        	HttpUriRequest request = new HttpGet(url);
	        	try (CloseableHttpResponse response = client.execute(request);
	        							InputStream stream = response.getEntity().getContent()) {
	
	        		//FEED STUFF
	        		statusLine = response.getStatusLine().toString();
	        		if (response.getStatusLine().getStatusCode() == 301){
	        			errorRedirect = response.getFirstHeader("Location").getValue();
	        			statusLine += (", NEW URI: " + errorRedirect);
	        			Debugger.println("RSS-FEED: '" + url + "' has REDIRECT to: " + errorRedirect, 1);
	        		}
	            	SyndFeedInput input = new SyndFeedInput();
	    			SyndFeed feed = input.build(new XmlReader(stream));
	    			//System.out.println("Title: " + feed.getTitle());					//DEBUG
	    			
	    			//image
	    			String imgURL = "";
	    			/*
	    			try{
	    				imgURL = feed.getImage().getUrl();
	    				//check if its valid
	    				if (imgURL.endsWith(".ico")){
	    					imgURL = "";
	    				}
	    			}catch (Exception e){
	    				//leave empty
	    			}
	    			*/
	    		
	    			JSON.add(feedResult, "image", imgURL);
	    			JSON.add(feedResult, "feedName", feed.getTitle());
	    			JSON.add(feedResult, "nameClean", feedName);
	    			
	    			List<SyndEntry> entries = feed.getEntries();
	    			//System.out.println("Entries: " + entries.size());					//DEBUG
	    			int i = 1;
	    			for (SyndEntry se : entries){
	    				if (i > maxEntries){
	    					break;
	    				}
	    				JSONObject jo = new JSONObject();
	    				String title = se.getTitle();
	    				if (title.contains("[Anzeige]")){
	    					continue;
	    				}
	    				title = title.replaceAll("\\r\\n|\\r|\\n|\\t", " ").replaceAll("\u00A0|\u200B", " ")
	    						.replaceAll("\\s+", " ").trim(); 
	    				String link = se.getLink();
	    				link = link.replaceAll("\\r\\n|\\r|\\n|\\t", " ").replaceAll("\\s+", " ").trim();
	    				//some more tweaks:
	    				link = link.replaceAll("www\\.bild\\.de", "m.bild.de");
	    				
	    				JSON.add(jo, "title", title);
	    				JSON.add(jo, "link", link);
	    				Date d = se.getPublishedDate();
	    				JSON.add(jo, "pubDate", (d != null)? DateTime.getGMT(d, Config.defaultSdf) : "");
	    				SyndContent descCont = se.getDescription();
	    				String desc = (descCont != null)? descCont.getValue() : "";
	    				if (desc.isEmpty()){
	    					try{
	    						desc = se.getContents().get(0).getValue();
	    					}catch (Exception e){
	    						desc = "";
	    					}
	    				}
	    				desc = desc.replaceAll("<img.*?>", "")
	    						.replaceAll("\\r\\n|\\r|\\n|\\t", " ").replaceAll("\u00A0|\u200B", " ")
	    						.replaceAll(Pattern.quote("<!--more-->") + ".*", "")
	    						//.replaceAll("<a href=.*?>", "").replaceAll("</a>", "")
	    						.replaceAll("\\s+", " ").trim();
	    				JSON.add(jo, "description", desc);
	    				
	    				JSON.add(feedEntries, jo);
	    				//System.out.println("jo: " + jo.toJSONString());					//DEBUG
	    				i++;
	    			}
	    			JSON.add(feedResult, "data", feedEntries);
	    			
	    			//cache
	    			if (cacheIt){
	    				//feedCache.put(feedName, feedResult);
	    				//feedCacheTS.put(feedName, System.currentTimeMillis());
	    				JSON.add(feedCache, feedName, feedResult);
	    				JSON.add(feedCacheTS, feedName, System.currentTimeMillis());
	    				if (directRefresh){
	    					Debugger.println("RssFeedReader.getFeed() - feed refreshed: " + feedName, 3);
	    				}
	    			}
	        	}
	        }
        } catch (Exception e) {
			Debugger.println("RssFeedReader.getFeed() - failed at feed: " + feedName + ", URL: " + url 
					+ ", status: " + statusLine + ", msg: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
			feedResult = new JSONObject();
        }

		return feedResult;
	}

}
