package net.b07z.sepia.server.assist.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Timer;

public class Test_RssFeedReader {

	public static void main(String[] args) {

		long tic = Timer.tic();
		RssFeedReader rss = new RssFeedReader();
		int maxEntries = 10;
		boolean cacheIt = false;
		
		Map<String, String> feeds = new HashMap<>();		// = NewsRssFeeds.feedUrls.entrySet();
		feeds.put("1E9 Magazin", "https://1e9.community/c/magazin.rss");
		
		int tested = 0;
		int good = 0;
		for (Entry<String, String> e : feeds.entrySet()){
			System.out.println("\nPRODUCTION FEED: " + e.getValue() + "\n");
			JSONObject feed = rss.getFeed(e.getValue(), e.getKey(), maxEntries, cacheIt);
			tested++;
			if (testFeed(feed)){
				good++;
			}
		}
		System.out.println("\nDONE - tested: " + tested + " - good: " + good + " - took: " + Timer.toc(tic) + "ms");
	}
	
	public static boolean testFeed(JSONObject feed){
		JSONArray feedEntries = (JSONArray) feed.get("data");
		String title = JSON.getString(feed, "feedName");
		try{
			System.out.println("Title: " + title + ", Entries: " + feedEntries.size());
			System.out.println("Test: " + feedEntries.get(0).toString());
			return true;
		}catch(Exception e){
			//ignore or throw error
			return false;
		}
	}

}
