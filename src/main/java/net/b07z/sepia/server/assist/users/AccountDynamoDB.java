package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.ApiManager;
import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.DynamoDB;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * AWS DynamoDB database access implementing the Account_Interface.
 * 
 * @author Florian Quirin
 *
 */
public class AccountDynamoDB implements AccountInterface{
	
	//Configuration
	private static String tableName = DB.USERS;
	
	/**
	 * Set DynamoDB table like "Users" etc. ...
	 * @param path - table string created in DynamoDB
	 */
	public void setTable(String path){
		tableName = path;
	}
	
	//----------Main methods----------

	//get one or more data elements from database
	public int getInfos(User user, ApiManager api, String... keys) {
		
		long tic = System.currentTimeMillis();
		
		//check if the user and api are valid and authorized to access the database
		if (!user.getToken().authenticated() || !api.isSigned()){
			return 2;
		}
		
		//keys:
		ArrayList<String> checkedKeys = new ArrayList<>();
		for (String s : keys){
			//restrict access to database	TODO: keep this up to date!!! Introduce access levels?
			if (ACCOUNT.restrictReadAccess.contains(s.replaceFirst("\\..*", "").trim())){
				//password and tokens retrieval is NOT allowed! NEVER! only if you run this as an authenticator (own class)
				Debugger.println("DB read access to '" + s + "' has been denied!", 3);
				continue;
			}
			if (!api.isAllowedToAccess(s)){
				Debugger.println("API: " + api.getName() + " is NOT! allowed to access field " + s, 3);
				continue;
			}
			checkedKeys.add(s);
		}
		if (checkedKeys.isEmpty()){
			Debugger.println("getInfo - no valid keys (left)!", 1);
			return 2;
		}
				
		//Connect
		JSONObject response = DynamoDB.getItem(tableName, DynamoDB.PRIMARY_USER_KEY, user.getUserID(), 
				checkedKeys.toArray(new String[0]));
		//System.out.println("RESPONSE: " + response.toJSONString());			//debug
		//System.out.println("Time needed: " + Debugger.toc(tic) + "ms");		//debug
		
		//Status?
		if (!Connectors.httpSuccess(response)){
			//no access, no connection, wrong search keys or unknown error
			return 4;
		}else{
			if (response.size()==1){
				//best guess: no account found?
				return 3;
			}else{
				//all clear! get the stuff:
				//Item is the main container
				JSONObject item = (JSONObject) response.get("Item");
				//Run through all keys and save them
				for (String k : checkedKeys){
					
					//be sure to USE ONLY CHECKED KEYS HERE!!! 
										
					//we strictly use maps as containers! So we can split strings at "." to get attributes
					String[] levels = k.split("\\.");
					//dig deep if we must
					JSONObject value = DynamoDB.dig(item, levels, 0);
					if (value != null){
						//String type;
						Object found;
						//type conversion and save, DynamoDB keys possible: B,BS,BOOL,L,M,N,NS,S,SS,NULL
						found = DynamoDB.typeConversion(value);
						if (found != null){
							//type = found.getClass().toString();
							//System.out.println("found: " + found + " - k: " + k);		//debug
							
							//add to data strings?
							//user_name
							/* 
							if (k.matches(ACCOUNT.USER_NAME_FIRST)){
								user.user_name = User.setAttribute(user.user_name, levels[levels.length-1], found.toString());
							}else if (k.matches(ACCOUNT.USER_NAME_LAST)){
								user.user_name = User.setAttribute(user.user_name, levels[levels.length-1], found.toString());
							}else if (k.matches(ACCOUNT.USER_NAME_NICK)){
								user.user_name = User.setAttribute(user.user_name, levels[levels.length-1], found.toString());
							*/
							if (k.matches(ACCOUNT.USER_NAME)){
								Map<String, Object> uname = Converters.object2HashMapStrObj(found);
								if (uname != null){
									/*user.userName = new Name((String) uname.get(Name.FIRST),
											(String) uname.get(Name.LAST), (String) uname.get(Name.NICK));*/
									user.userName = new Name(uname);
									user.info.put(k, user.userName);
								}
							
							//user_home - this has been moved to user-data
							/*
							else if (k.matches(ACCOUNT.USER_HOME)){
								HashMap<String, Object> adr = Converters.object2HashMap_SO(found);
								if (adr != null){
									user.userHome = new Address(adr);
									user.info.put(k, user.userHome);
								}
							}
							//user_work
							else if (k.matches(ACCOUNT.USER_WORK)){
								HashMap<String, Object> adr = Converters.object2HashMap_SO(found);
								if (adr != null){
									user.userWork = new Address(adr);
									user.info.put(k, user.userWork);
								}
							*/
							
							//add to info as is
							}else{
								user.info.put(k, found);
								//user.info.put(k + ".type", type);
							}
							
							//TODO: add more, add type as extra key?
						}
					}
				}
				//save statistics on successful data transfer
				Statistics.add_DB_hit();
				Statistics.save_DB_total_time(tic);
				
				return 0;
			}
		}
	}
	
	//get object from database - use the bunch read method and directly recover the object
	public Object getInfoObject(User user, ApiManager api, String key) {
		getInfos(user, api, key);
		return user.info.get(key);
	}

	//set items in database
	public int setInfos(User user, ApiManager api, JSONObject data) {
		long tic = System.currentTimeMillis();
		if (data.isEmpty()){
			Debugger.println("setInfo - No data was given. This will not return an error but it should not happen!", 1);	//debug
			return 0;
		}
		
		//check if the user is authorized to access the database
		if (!user.getToken().authenticated() || !api.isSigned()){
			return 2;
		}
		
		//Convert it back to the old format - TODO: this is probably computational "heavy", but it was my best idea to restore compatibility 
		JSONObject flatJson = JSON.makeFlat(data, "", null);
		
		//Filter by access restrictions
		ArrayList<String> keys = new ArrayList<>();
		ArrayList<Object> objects = new ArrayList<>();
		for (Object kO : flatJson.keySet()){
			String k = kO.toString();
			//restrict access to database
			if (ACCOUNT.restrictWriteAccess.contains(k.replaceFirst("\\..*", "").trim())){
				Debugger.println("DB write access to '" + k + "' has been denied!", 3);
				continue;
			}
			if (!ACCOUNT.allowFlexAccess(k)){
				//password, tokens etc. can NOT be written here! NEVER!
				Debugger.println("DB write access to '" + k + "' has been denied!", 3);
				continue;
			}
			if (!api.isAllowedToAccess(k.toLowerCase())){
				Debugger.println("API: " + api.getName() + " is NOT! allowed to access field " + k, 3);
				continue;
			
			}else{
				keys.add(k);
				objects.add(flatJson.get(k));
			}
		}
		if (keys.isEmpty()){
			Debugger.println("setInfo - no valid keys (left)!", 1);
			return 2;
		}
		
		//Connect
		int code = DynamoDB.writeAny(tableName, DynamoDB.PRIMARY_USER_KEY, user.getUserID(), 
				keys.toArray(new String[0]), objects.toArray(new Object[0]));
		
		if (code == 3){
			Debugger.println("setInfo - DynamoDB connection error!", 1);		//debug
			return 1;
		}else if (code != 0){
			Debugger.println("setInfo - DynamoDB 'some' error!", 1);			//debug
			return 4;
		}else{
			//save statistics on successful data transfer
			Statistics.add_DB_hit();
			Statistics.save_DB_total_time(tic);
			
			return 0;
		}
	}

	//set object in database - use the bunch write method with single key and object
	public int setInfoObject(User user, ApiManager api, String key, Object object) {
		return setInfos(user, api, JSON.make(key, object));
	}
	
	@Override
	public int deleteInfos(User user, ApiManager api, String... keys) {
		//Note: we don't actually delete objects, we just set the field value to empty string
		JSONObject dataAsJson = new JSONObject();
		for (String k : keys)
		JSON.putWithDotPath(dataAsJson, k, "");
		return setInfos(user, api, dataAsJson);
		//TODO: it is not very efficient to create a JSON object just to make it flat again afterwards ...
	}
	
	//---------------------WRITE STATISTICS-------------------------
	
	//write basic statistics like last log-in and total usage
	public boolean writeBasicStatistics(String userID){
		
		long tic = System.currentTimeMillis();
		
		//operation:
		String operation = "UpdateItem";
		
		//add this
		JSONObject expressionAttributeValues = new JSONObject();

		String updateExpressionSet = "ADD ";
		updateExpressionSet += "statistics.totalCalls :val1";	// + ", ";
		JSON.add(expressionAttributeValues, ":val1", DynamoDB.typeConversionDynamoDB(new Integer(1)));
		
		updateExpressionSet += " SET ";
		updateExpressionSet += "statistics.lastLogin = :val2";  // + ", ";
		JSON.add(expressionAttributeValues, ":val2", DynamoDB.typeConversionDynamoDB(String.valueOf(System.currentTimeMillis())));

		//clean up:
		String updateExpression = updateExpressionSet.trim();
		
		//primaryKey:
		JSONObject prime = DynamoDB.getPrimaryUserKey(userID);
		
		//JSON request:
		JSONObject request = new JSONObject();
		JSON.add(request, "TableName", tableName);
		JSON.add(request, "Key", prime);
		JSON.add(request, "UpdateExpression", updateExpression);
		if (!expressionAttributeValues.isEmpty()){
			JSON.add(request, "ExpressionAttributeValues", expressionAttributeValues);
		}
		JSON.add(request, "ReturnValues", "NONE");		//we don't need that info here .. yet
		
		//System.out.println("REQUEST: " + request.toJSONString());		//debug
		
		//Connect
		JSONObject response = DynamoDB.request(operation, request.toJSONString());
		//System.out.println("RESPONSE: " + response.toJSONString());			//debug
		//System.out.println("Time needed: " + Debugger.toc(tic) + "ms");		//debug
		
		if (!Connectors.httpSuccess(response)){
			//errorCode = 3;
			Debugger.println("writeBasicStatistics - DynamoDB Response: " + response.toJSONString(), 1);			//debug
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
