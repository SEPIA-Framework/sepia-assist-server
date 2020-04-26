package net.b07z.sepia.server.assist.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.workers.Workers;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Connectors.HttpClientResult;
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
	
	public static long oldNewsAgeMs = 1000 * 60 * 60 * 24 * 3; 		//3 days in milliseconds
	
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
	
	/**
	 * Load the backup and return the last-modified timestamp of it or -1 if there was none.
	 * @return timestamp or -1
	 */
	public long loadBackup(){
		//check file
		File file = new File(Workers.rssFeedsData_BackupFile);
		if (!file.exists()){
			Debugger.println("RssFeedReader - no backup file found! This is ok if you start for the first time or cleaned the backup.", 1);
			return -1l;
		}
		JSONObject backup = JSON.readJsonFromFile(Workers.rssFeedsData_BackupFile);
		if (backup != null && !backup.isEmpty()){
			long lastMod = file.lastModified();
			this.feedCache = backup;
			Debugger.println("RssFeedReader - backup restored with " + feedCache.size() + " feeds. Last modified: " + (new SimpleDateFormat(Config.defaultSdf)).format(lastMod), 3);
			return lastMod;
		}else{
			Debugger.println("RssFeedReader - backup was corrupted! Please check or remove the file at: " + Workers.rssFeedsData_BackupFile, 1);
			return -1l;
		}
	}
	
	public JSONObject getCache(){
		return feedCache;
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
        JSONObject feedResult = new JSONObject();
        JSONArray feedEntries = new JSONArray();
        List<JSONObject> feedEntriesOlder = new ArrayList<>();
        
        try {
	        /*try (CloseableHttpClient client = HttpClients.createMinimal()) {
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
		    		*/
        	//Get content as String then create a stream (its safer than the previous method)
        	HttpClientResult httpRes = Connectors.apacheHttpGET(url, "");
        	statusLine = httpRes.statusLine;
        	if (httpRes.content == null || httpRes.content.isEmpty()){
        		throw new RuntimeException("Feed content not found. Code: " + httpRes.statusCode + ", Status: " + httpRes.statusLine);
        	}
			//System.out.println(httpRes.content.substring(0, 50));
        	
        	//clean-up before parsing
        	httpRes.content = httpRes.content.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFF]", "");
        	//NOTE: I think we could optimize performance using FilterInputStream at HTTP call to filter this 
        	
			InputStream stream;
			if (httpRes.encoding != null){
				stream = new ByteArrayInputStream(httpRes.content.getBytes(httpRes.encoding));
			}else{
				stream = new ByteArrayInputStream(httpRes.content.getBytes(StandardCharsets.UTF_8));
			}
			
        	SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(stream, true, "UTF-8"));
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
				long age = -1;
				if (d != null){
					age = System.currentTimeMillis() - d.getTime();
				}
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
				//clean up
				desc = desc.replaceAll("<img.*?>", "")
						.replaceAll("\\r\\n|\\r|\\n|\\t", " ").replaceAll("\u00A0|\u200B", " ")
						.replaceAll(Pattern.quote("<!--more-->") + ".*", "")
						.replaceAll("<video.*?</video>", "")
						.replaceAll("<audio.*?</audio>", "")
						.replaceAll("<iframe.*?</iframe>", "")
						.replaceAll("<object.*?</object>", "")
						//.replaceAll("<a href=.*?>", "").replaceAll("</a>", "")
						.replaceAll("\\s+", " ").trim();
				//max. 2000 characters
				if (desc.length() > 2000){
					desc = desc.replaceAll("<.*?>", " ").replaceAll("\\s+", " "); 	//<- most aggressive html clean-up
					desc = desc.substring(0, Math.min(2000, desc.length()));
				}
				JSON.add(jo, "description", desc);
				
				//filter by date and add
				if (age >= 0 && age < oldNewsAgeMs) {
					JSON.add(feedEntries, jo);
					i++;
				}else{
					JSON.put(jo, "age", age);
					feedEntriesOlder.add(jo);
				}
				//System.out.println("jo: " + jo.toJSONString());					//DEBUG
			}
			
			//check if we have enough "new" news. If not fill with older
			if (feedEntries.size() < maxEntries){
				//sort old list
				feedEntriesOlder.sort((jo1, jo2) -> {
					long a1 = (long) jo1.get("age");
					long a2 = (long) jo2.get("age");
					// -1 - less than, 1 - greater than, 0 - equal
			        return a1 < a2 ? -1 : (a1 > a2) ? 1 : 0;
				});
				//add newest first
				i = feedEntries.size();
    			for (Object e : feedEntriesOlder){
    				if (i > maxEntries){
    					break;
    				}
    				JSONObject je = (JSONObject) e;
    				je.remove("age");
    				//System.out.println(je.get("title") + " - " + je.get("age"));
    				JSON.add(feedEntries, je);
    				i++;
    			}
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
	        	/*}
	        }*/
        } catch (Exception e) {
			Debugger.println("RssFeedReader.getFeed() - failed at feed: " + feedName + ", URL: " + url 
					+ ", status: " + statusLine + ", msg: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
			feedResult = new JSONObject();
        }

		return feedResult;
	}

}
