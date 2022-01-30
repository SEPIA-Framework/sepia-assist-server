package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceAccessManager;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Elasticsearch database access implementing the Account_Interface.
 * 
 * @author Florian Quirin
 *
 */
public class AccountElasticsearch implements AccountInterface{
	
	/**
	 * Get the database to query. Loads the Elasticsearch connection defined by settings. 
	 */
	private Elasticsearch getDB(){
		Elasticsearch es = new Elasticsearch();
		return es;
	}
	
	//----------Main methods----------

	@Override
	public int getInfos(User user, ServiceAccessManager api, String... keys) {
		
		long tic = System.currentTimeMillis();
		
		//check if the user and api are valid and authorized to access the database
		if (!user.getToken().authenticated() || !api.isSigned()){
			return 2;
		}
		
		String userId = user.getUserID();
		
		//filter keys to make sure we only read what we are allowed to
		ArrayList<String> checkedFields = ACCOUNT.filterServiceReadData(api, keys);
		
		//make sure we have filters
		if (checkedFields.isEmpty()){
			return 2;
		}
		
		//Connect
		JSONObject response = getDB().getItemFiltered(DB.USERS, "all", userId, checkedFields.toArray(new String[]{}));
		//System.out.println("RESPONSE: " + response.toJSONString());			//debug
		//System.out.println("Time needed: " + Debugger.toc(tic) + "ms");		//debug
		
		//Status?
		if (!Connectors.httpSuccess(response)){
			//no access, no connection, wrong search keys or unknown error
			return 4;
		}else{
			JSONObject item = (JSONObject) response.get("_source");
			if (item == null || item.isEmpty()){
				//best guess: no account found?
				return 3;
			}else{
				//all clear! get the stuff:
				
				//Run through all keys and save them
				for (String k : checkedFields){
					
					//we strictly use maps as containers! So we can split strings at "." to get attributes
					String[] levels = k.split("\\.");
					Object found = JSON.getObject(item, levels);
					if (found != null){
						//name
						if (k.equals(ACCOUNT.USER_NAME)){
							user.userName = new Name(Converters.object2HashMapStrObj(found));
							user.info.put(k, user.userName);
						
						//user_home - this is not officially part of the account anymore, 
						//but its all in user-data
						/*
						}else if (k.equals(ACCOUNT.USER_HOME)){
							user.userHome = new Address((JSONObject) found);
							user.info.put(k, user.userHome);

						//user_work
						}else if (k.equals(ACCOUNT.USER_WORK)){
							user.userWork = new Address((JSONObject) found);
							user.info.put(k, user.userWork);
						*/
						
						//add to info as is
						}else{
							user.info.put(k, found);
						}
						
						//TODO: add more specials?
					}
				}
				//save statistics on successful data transfer
				Statistics.add_DB_hit();
				Statistics.save_DB_total_time(tic);
				
				return 0;
			}
		}
	}
	
	@Override
	public Object getInfoObject(User user, ServiceAccessManager api, String key) {
		getInfos(user, api, key);
		return user.info.get(key);
	}

	@Override
	public int setInfos(User user, ServiceAccessManager api, JSONObject data) {
		long tic = System.currentTimeMillis();
		if (data.isEmpty()){
			Debugger.println("setInfo - No data was given. This will not return an error but it should not happen!", 1);	//debug
			return 0;
		}
		
		//check if the user is authorized to access the database
		if (!user.getToken().authenticated() || !api.isSigned()){
			return 2;
		}
		
		//filter the fields that are not allowed to be accessed - Note: applies to top level and some partially accessible direct children!
		JSONObject filteredData = ACCOUNT.filterServiceWriteData(api, data);
		if (filteredData.isEmpty()){
			Debugger.println("setInfo - Failed due to empty (allowed) data!", 1);			//debug
			return 2;
		}
		
		//Connect
		int code = getDB().updateDocument(DB.USERS, "all", user.getUserID(), filteredData);
		//System.out.println("Time needed: " + Debugger.toc(tic) + "ms");		//debug
		
		//Status?
		if (code != 0){
			Debugger.println("setInfo - Failed due to 'some' error!", 1);			//debug
			return 4;
		}else{
			//save statistics on successful data transfer
			Statistics.add_DB_hit();
			Statistics.save_DB_total_time(tic);			
			return 0;
		}
	}

	@Override
	public int setInfoObject(User user, ServiceAccessManager api, String key, Object object) {
		JSONObject data = new JSONObject();
		JSON.putWithDotPath(data, key, object);
		return setInfos(user, api, data);
	}
	
	@Override
	public int deleteInfos(User user, ServiceAccessManager api, String... keys) {
		/* this is too risky because it can delete essential parts of the account object  
		if (keys != null && keys.length == 1){
			return getDB().deleteFromDocument(DB.USERS, "all", user.getUserID(), keys[0]);
		}*/
		//Note: we don't actually delete objects, we just set the field value to empty string
		JSONObject dataAsJson = new JSONObject();
		for (String k : keys){
			JSON.putWithDotPath(dataAsJson, k, "");		//NOTE: this cannot delete objects 
		}
		return setInfos(user, api, dataAsJson);
	}
	
	//---------------------WRITE STATISTICS-------------------------
	
	@Override
	public boolean writeBasicStatistics(String userID){
		
		long tic = System.currentTimeMillis();
		
		//define script for updates
		JSONObject script = JSON.make("script", JSON.make(
				"inline", "ctx._source." + ACCOUNT.STATISTICS + ".totalCalls+=1; "
						+ "ctx._source." + ACCOUNT.STATISTICS + ".lastLogin=params.unixTime;",
				"params", JSON.make("unixTime", System.currentTimeMillis()),
				"lang", "painless"
			)
		);
				
		//Connect - TODO: this ends-up often in a failed 'retry' if we set it too small
		//long ts = System.currentTimeMillis();
		//System.out.println("writeBasicStatistics - UID: " + userID + " - ts: " + ts);									//debug
		int code = getDB().updateDocument(DB.USERS, "all", userID, script, 5);  	
		//About retry: https://github.com/elastic/elasticsearch/issues/13619
		//System.out.println("writeBasicStatistics - UID: " + userID + " - ts: " + ts + " - Time needed: " + Debugger.toc(tic) + "ms");	//debug
		
		if (code != 0){
			//errorCode = 3;
			Debugger.println("writeBasicStatistics - Failed due to 'some' error!", 1);			//debug
			return false;
		}else{
			//save statistics on successful data transfer
			Statistics.add_DB_hit();
			Statistics.save_DB_total_time(tic);
			
			//errorCode = 0;
			return true;
		}	
	}

}
