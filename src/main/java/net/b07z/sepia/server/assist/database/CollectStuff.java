package net.b07z.sepia.server.assist.database;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.database.DatabaseInterface;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Collect useful stuff to build up databases and corpi. Includes methods to collect asynchronous write processes to make 
 * one big BatchWrite in the end. 
 * 
 * @author Florian Quirin
 *
 */
public class CollectStuff {
	
	//TODO: implement collecting asynchronous write requests and write them in one big BatchWrite in the end. 
	//		Rewrite DB.saveAsync... methods	to redirect here
	
	/**
	 * Use this class to store an asynchronous write request to a database (defined by index) for later, e.g. to make a big
	 * BatchWrite action instead of multiple small ones.
	 */
	public class CollectRequest{
		String index = "";
		String type = "";
		String id = "";
		JSONObject data;
		
		public CollectRequest(String index, String type, String id, JSONObject data){
			this.index = index;
			this.type = type;
			this.id = id;
			this.data = data;
		}
	}
	
	/**
	 * Store useful data submitted by user in asynchronous way (without blocking the main).
	 * @param nluResult - {@link NluResult} object
	 */
	public static void saveAsync(NluResult nluResult){
		//asynchronous
		Thread thread = new Thread(){
		    public void run(){
		    	//time
		    	long tic = Debugger.tic();
		    	
		    	//database
		    	DatabaseInterface db = (DatabaseInterface) ClassBuilder.construct(Config.knowledgeDbModule);
		    	//TODO: do a batch-write instead of separate ones
		    	
		    	//geo-data
		    	int code1 = 0;
		    	if (Config.collectGeoData){
			    	String loc = nluResult.input.userLocation;
			    	if (loc != null && !loc.isEmpty()){
			    		JSONObject locJson = JSON.parseStringOrFail(loc);
			    		String city = JSON.getString(locJson, LOCATION.CITY);
				    	if (!city.isEmpty()){
				    		String index = DB.STORAGE;
				    		String type = "geo_data";
				    		String id = getGeoLocationID(locJson);
				    		if (!id.isEmpty()){
				    			//System.out.println("STORE: " + index + "/" + type + "/" + id + " - json: " + loc_js.toJSONString()); 		//debug
					    		code1 = db.setItemData(index, type, id, locJson);
				    		}
				    	}
			    	}
		    	}
		    	
		    	//sentences and command
		    	int code2 = 0;
		    	/*
		    	String text = nlu_res.input.text_raw;
		    	String language = nlu_res.input.language;
		    	String cmd_sum = nlu_res.cmd_summary;
		    	JSONObject nlu_js = new JSONObject();
		    	JSON.add(nlu_js, "text", text);
		    	JSON.add(nlu_js, "language", language);
		    	JSON.add(nlu_js, "cmd", cmd_sum);
		    	String index = DB.STORAGE;
	    		String type = "nlu_data";
		    	//System.out.println("STORE: " + index + "/" + type + "/[rnd] - json: " + nlu_js.toJSONString()); 		//debug
	    		code2 = db.setAnyItemData(index, type, nlu_js);
	    		*/
		    	
				if ((code1 + code2) != 0){
					Debugger.println("STORAGE STUFF ERROR! - TIME: " + System.currentTimeMillis(), 1);
				}else{
					Statistics.add_KDB_write_hit(); 	//1
					//Statistics.add_KDB_write_hit();		//2
					Statistics.save_KDB_write_total_time(tic);	//1
				}
		    	/*else{
					Debugger.println("KNOWLEDGE DB UPDATED! - PATH: " + index + "/" + type + "/[rnd] - TIME: " + System.currentTimeMillis(), 1);
				}*/
		    }
		};
		thread.start();
	}

	//-------------HELPERS-------------
	
	/**
	 * Get an ID of a location JSONObject to store it in the DB.
	 * Current implementation takes the geo coordinates with 4 digit precision (lat + "_" + lng). 
	 * @param location_data_js - JSONObject holding the necessary data (e.g. latitude, longitude)
	 * @return id string or empty string
	 */
	public static String getGeoLocationID(JSONObject location_data_js){
		double lat = Converters.obj2DoubleOrDefault(location_data_js.get(LOCATION.LAT), Double.NEGATIVE_INFINITY);
		double lng = Converters.obj2DoubleOrDefault(location_data_js.get(LOCATION.LNG), Double.NEGATIVE_INFINITY);
		if (lat != Double.NEGATIVE_INFINITY && lng != Double.NEGATIVE_INFINITY){
			String id = String.format("%.4f", lat) + "_" + String.format("%.4f", lng);
			id = id.replaceAll(",", ".");
			return id;
		}else{
			return "";
		}
	}
}
