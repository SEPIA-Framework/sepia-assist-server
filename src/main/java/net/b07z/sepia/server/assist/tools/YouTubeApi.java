package net.b07z.sepia.server.assist.tools;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Video search via YouTube API.
 * 
 * @author Florian Quirin
 *
 */
public class YouTubeApi {
	
	private static String youTubeSearchApi = "https://youtube.googleapis.com/youtube/v3/search";
	
	/**
	 * Send request to YouTubeAPI and return array of matches.
	 * @param searchTerm - YouTube search term
	 * @param maxResults - Max. number of results
	 * @return JSONObject with "status":200 and "result": [{...},...] or "status":(>=400) and "error":"..."
	 */
	public static JSONObject searchVideoForEmbedding(String searchTerm, int maxResults){
		if (Is.nullOrEmpty(searchTerm)){
			return JSON.make("result", new JSONArray(), "status", 200);		//empty query = empty result
		}
		if (Is.nullOrEmpty(Config.youtube_api_key)){
			return JSON.make("error", "Missing API key", "status", 400);
		}
		//optimize search term - NOTE: not really working as planned yet ^^
		/*
		if (!searchTerm.matches("(?i).*lesson(s|).*")){
			searchTerm += " -lesson";
		}
		*/
		try {
			//build URL
			String url = youTubeSearchApi
				+ "?part=snippet"
				+ "&maxResults=" + maxResults
				+ "&q=" + URLEncoder.encode(searchTerm, "UTF-8")
				+ "&order=relevance"
				//+ "&order=rating"
				+ "&type=video&videoCaption=none&videoEmbeddable=true&videoLicense=any"
				+ "&safeSearch=none"
				+ "&key=" + Config.youtube_api_key;
			
			//Header
			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			headers.put("Content-Type", "application/json");
			
			//Call
			//System.out.println("URL: " + url); 		//DEBUG
			long tic = System.currentTimeMillis();
			JSONObject res = Connectors.httpGET(url, null, headers);
			Statistics.addExternalApiHit("YouTubeApi searchVideoForEmbedding");
			Statistics.addExternalApiTime("YouTubeApi searchVideoForEmbedding", tic);
			
			//Get first item
			if (Connectors.httpSuccess(res)){
				JSONArray items = JSON.getJArray(res, "items");
				if (items == null){
					Statistics.addExternalApiHit("YouTubeApi-error searchVideoForEmbedding");
					return JSON.make("error", "missing results field 'items' or null", "status", 500);
				}
				//collect
				JSONArray results = new JSONArray();
				JSON.forEach(items, it -> {
					JSONObject match = new JSONObject();
					JSONObject item = (JSONObject) it;
					JSONObject snippet = JSON.getJObject(item, "snippet");
					JSONObject idObj = JSON.getJObject(item, "id");
					if (snippet != null){
						JSON.put(match, "title", JSON.getString(snippet, "title"));
						JSON.put(match, "description", JSON.getString(snippet, "description"));
						JSON.put(match, "channelId", JSON.getString(snippet, "channelId"));
						JSON.put(match, "channelTitle", JSON.getString(snippet, "channelTitle"));
					}
					if (idObj != null){
						JSON.put(match, "videoId", JSON.getString(idObj, "videoId"));
					}
					JSON.add(results, match);
				});
				return JSON.make("result", results, "status", 200);
				
			}else{
				Statistics.addExternalApiHit("YouTubeApi-error searchVideoForEmbedding");
				return JSON.make("error", res.get("error"), "status", res.get("code"));
			}
		}catch (Exception e){
			Statistics.addExternalApiHit("YouTubeApi-error searchVideoForEmbedding");
			return JSON.make("error", e.getMessage(), "status", 500);
		}
	}

}
