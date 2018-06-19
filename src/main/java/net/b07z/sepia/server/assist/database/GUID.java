package net.b07z.sepia.server.assist.database;

import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to generate global unique IDs.
 * 
 * @author Florian Quirin
 *
 */
public class GUID {
	
	public static final String INDEX = "guid";
	
	//server that handles the GUID
	private static String elasticGuidServer;
	
	//offset
	//private static long guidOffset = 1000; 	//IDs should have at least 4 digits - moved to config file as Config.guid_offset
	
	//last issued IDs
	private static long lastElasticGUID = -1;	//this is not a reliable way to track the GUID but it gives a rough idea where we are
	
	/**
	 * Setup GUID generator (e.g. define database).
	 */
	public static void setup(){
		elasticGuidServer = ConfigElasticSearch.getEndpoint(Config.defaultRegion);
	}
	
	/**
	 * Get a global unique ID for new users.
	 * @throws RuntimeException
	 */
	public static String getUserGUID() throws RuntimeException{
		long guid = makeElasticGUID();
		return Config.userIdPrefix + (Config.guidOffset + guid);
	}
	
	/**
	 * Get a global unique ID for general purpose.
	 * @throws RuntimeException
	 */
	public static String getTicketGUID() throws RuntimeException{
		long guid = makeElasticGUID();
		return "t" + (Config.guidOffset + guid);
	}
	
	/**
	 * Return the GUID that has been last issued by this server.
	 */
	public static long getLastIssuedGUID(){
		return lastElasticGUID;
	}
	
	//------------Implementations----------------
	
	/**
	 * Uses Elasticsearch to generate a GUID. 
	 */
	private static long makeElasticGUID(){
		//build URL
		String url = elasticGuidServer + "/" + INDEX + "/" + "sequence" + "/" + "ticket" + "/_update";
		
		//build data
		JSONObject data = new JSONObject();
			JSONObject doc = new JSONObject();
			JSON.add(doc, "near_id", lastElasticGUID + 1l); 	//note: this is only a rough indication
			JSON.add(doc, "offset", Config.guidOffset);
		JSON.add(data, "doc", doc);
		JSON.add(data, "detect_noop", false);
		
		//make update POST
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Content-Length", Integer.toString(data.toJSONString().getBytes().length));
		
		JSONObject result = Connectors.httpPOST(url, data.toJSONString(), headers);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		try{
			long version = (long) result.get("_version");
			long shards_success = (long) JSON.getJObject(result, "_shards").get("successful");
			if (shards_success == 1){
				lastElasticGUID = version;
				return version;
			}else{
				throw new RuntimeException("GUID.java - ES reports fail in shard check!");
			}

		//error
		}catch (Exception ex){
			String error = "GUID.java - ES failed to generate GUID!";
			//System.err.println(DateTime.getLogDate() + " WARNING - " + error);
			throw new RuntimeException(error, ex);
		}
	}

}
