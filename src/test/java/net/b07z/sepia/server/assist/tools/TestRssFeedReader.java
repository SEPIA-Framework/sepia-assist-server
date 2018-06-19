package net.b07z.sepia.server.assist.tools;

import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.services.NewsRssFeeds;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Timer;

public class TestRssFeedReader {

	public static void main(String[] args) {

		long tic = Timer.tic();
		RssFeedReader rss = new RssFeedReader();
		int maxEntries = 10;
		boolean cacheIt = false;
		
		/*
		List<String> urlList = Arrays.asList(
			"https://www.11freunde.de/feed",
			"https://www.gruenderszene.de/feed",
			"http://t3n.de/feed/feed.atom",
			"http://t3n.de/rss.xml",
			"https://www.serienjunkies.de/docs/serienkalender-aktuell.html"
		);
		for (String url : urlList){
			System.out.println("\nTEST FEED: " + url + "\n");
			JSONObject feed = rss.getFeed(url, "TestFeed", maxEntries, cacheIt);
			testFeed(feed);
		}
		*/
		
		int tested = 0;
		int good = 0;
		for (Entry<String, String> e : NewsRssFeeds.feedUrls.entrySet()){
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
