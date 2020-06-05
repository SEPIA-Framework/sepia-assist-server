package net.b07z.sepia.server.assist.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Security;

/**
 * Collect useful DynamoDB methods here. Note: this is not an implementation of the Database_Interface.
 * 
 * @author Florian Quirin
 *
 */
public class DynamoDB {
	final public static String PRIMARY_USER_KEY = ACCOUNT.GUUID; 	//note: changing that here does not influence Authentication settings!
	final public static String PRIMARY_TICKET_KEY = "guid";
	
	//------------------------Connection---------------------------
	
	public static String makeExpressionAttributeName(String keyIn, JSONObject expressionAttributeNames){
		String keyOut = "";
		String[] elements = keyIn.split("\\.");
		for (String e : elements){
			String eNew = "#n" + expressionAttributeNames.size();
			JSON.add(expressionAttributeNames, eNew, e);
			keyOut += ("." + eNew);
		}
		return keyOut.replaceFirst("^\\.", "").trim();
	}
	
	/**
	 * Get an item inside a table by using the primaryKey to search.
	 * @param tableName - name of the table to check, usually "users"
	 * @param primaryKey - primary key to search for, e.g. "Guuid"
	 * @param keyValue - value of the primary key to check, e.g. "test@b07z.net"
	 * @param lookUp - array of strings to look up in the item
	 * @return JSONObject result (needs to be checked manually for success)
	 */
	public static JSONObject getItem(String tableName, String primaryKey, String keyValue, String... lookUp){
		if (lookUp == null || lookUp.length <= 0){
			JSONObject result =	new JSONObject();
			JSON.add(result, Connectors.HTTP_REST_SUCCESS, Boolean.FALSE);
			JSON.add(result, "error", "no data to lookup!");
			return result;
		}
		
		//operation:
		String operation = "GetItem";
		
		JSONObject expressionAttributeNames = new JSONObject();
		
		//get password, key token, basic info etc ... :
		String lookFor = "";
		for (String s : lookUp){
			lookFor += makeExpressionAttributeName(s, expressionAttributeNames) + ", ";
		}
		lookFor = lookFor.trim().replaceFirst(",$", "");
		
		//JSON request:
		JSONObject request = new JSONObject();
		JSON.add(request, "TableName", tableName);
		JSON.add(request, "Key", getSearchKey(primaryKey, keyValue.toLowerCase().trim())); 		//IDs are always lowerCase
		JSON.add(request, "ConsistentRead", Boolean.FALSE);	//eventually consistent should be enough
		JSON.add(request, "ReturnConsumedCapacity", "NONE");		//we don't need that info here .. yet
		JSON.add(request, "ProjectionExpression", lookFor);
		if (!expressionAttributeNames.isEmpty()){
			JSON.add(request, "ExpressionAttributeNames", expressionAttributeNames);
		}
		
		return request(operation, request.toJSONString());
	}
	/**
	 * Get item inside table by using secondary indices. Note that indexName must be attributName here.  
	 * @param tableName - name of the table to check, usually "users"
	 * @param indexName - indexName aka attributName to search for, e.g. "Guuid"
	 * @param indexValue - value of the attribute to check, e.g. "test@b07z.net"
	 * @param lookUp - array of strings to look up in the item
	 * @return JSONObject result (needs to be checked manually for success)
	 */
	public static JSONObject queryIndex(String tableName, String indexName, String indexValue, String... lookUp){
		//note: in this case indexName must be identical to attribute name. 
		//IndexName could also be the header for multiple attributes or independent from attribute name, but this is not supported here.
		if (lookUp == null || lookUp.length <= 0){
			JSONObject result =	new JSONObject();
			JSON.add(result, Connectors.HTTP_REST_SUCCESS, Boolean.FALSE);
			JSON.add(result, "error", "no data to lookup!");
			return result;
		}
		
		//operation:
		String operation = "Query";
		
		JSONObject expressionAttributeNames = new JSONObject();
		
		//get password, key token, basic info etc ... :
		String lookFor = "";
		for (String s : lookUp){
			lookFor += makeExpressionAttributeName(s, expressionAttributeNames) + ", ";
		}
		lookFor = lookFor.trim().replaceFirst(",$", "");
		
		//JSON request:
		JSONObject request = new JSONObject();
		JSON.add(request, "TableName", tableName);
		JSON.add(request, "IndexName", indexName); 
		JSON.add(request, "KeyConditionExpression", indexName + "= :ival");
			JSONObject expAttVal = new JSONObject();
				JSONObject ival = JSON.add(new JSONObject(), "S", indexValue.toLowerCase().trim()); 	//IDs are always lowerCase
			JSON.add(expAttVal, ":ival", ival);
		JSON.add(request, "ExpressionAttributeValues", expAttVal);
		JSON.add(request, "Limit", 1);
		JSON.add(request, "ConsistentRead", Boolean.FALSE);	//eventually consistent should be enough
		JSON.add(request, "ReturnConsumedCapacity", "NONE");		//we don't need that info here .. yet
		JSON.add(request, "ProjectionExpression", lookFor);
		if (!expressionAttributeNames.isEmpty()){
			JSON.add(request, "ExpressionAttributeNames", expressionAttributeNames);
		}
		
		return request(operation, request.toJSONString());
	}
	
	/**
	 * Write a protected account attribute. For server operations only!!!
	 * @param primaryKey - primaryKey of item in table
	 * @param keyValue - value of primaryKey to match
	 * @param keys - keys to write
	 * @param objects - values to put at key positions
	 * @return error code: 0 - all good, 2 - wrong or invalid keys, 3 - DB connection error
	 */
	public static int writeAny(String tableName, String primaryKey, String keyValue, String[] keys, Object[] objects){
		
		long tic = System.currentTimeMillis();
		int errorCode = 0;
		
		if (keys == null || keys.length <= 0){
			return 2;
		}
		
		//operation:
		String operation = "UpdateItem";
		
		//add this
		String updateExpressionSet = "SET ";
		String updateExpressionRemove = "REMOVE ";
		
		JSONObject expressionAttributeValues = new JSONObject();
		JSONObject expressionAttributeNames = new JSONObject();
		
		for (int i=0; i<keys.length; i++){
			if (objects[i].toString().isEmpty()){
				updateExpressionRemove += makeExpressionAttributeName(keys[i], expressionAttributeNames) + ", ";
			}else{
				updateExpressionSet += makeExpressionAttributeName(keys[i], expressionAttributeNames) + "=" + ":val"+i + ", ";
				//System.out.println("type: " + objects[i].getClass()); 		//debug
				if (objects[i].getClass().equals(JSONObject.class)){
					JSON.add(expressionAttributeValues, ":val"+i, objects[i]);
				}else{
					JSONObject jo = typeConversionDynamoDB(objects[i]);
					JSON.add(expressionAttributeValues, ":val"+i, jo);
				}
			}
		}
		//clean up:
		if (updateExpressionSet.trim().equals("SET")){
			updateExpressionSet = "";
		}
		if (updateExpressionRemove.trim().equals("REMOVE")){
			updateExpressionRemove = "";
		}
		//check if valid keys are left
		if (updateExpressionSet.isEmpty() && updateExpressionRemove.isEmpty()){
			//access to all requested keys was denied 
			return 2;
		}
		String updateExpression = (updateExpressionSet.trim().replaceFirst(",$", "") + " " +
								updateExpressionRemove.trim().replaceFirst(",$", "")).trim();
		
		//JSON request:
		JSONObject request = new JSONObject();
		JSON.add(request, "TableName", tableName);
		JSON.add(request, "Key", getSearchKey(primaryKey, keyValue.toLowerCase().trim())); 	//IDs are always lowerCase
		JSON.add(request, "UpdateExpression", updateExpression);
		if (!expressionAttributeNames.isEmpty()){
			JSON.add(request, "ExpressionAttributeNames", expressionAttributeNames);
		}
		if (!expressionAttributeValues.isEmpty()){
			JSON.add(request, "ExpressionAttributeValues", expressionAttributeValues);
		}
		JSON.add(request, "ReturnValues", "NONE");		//we don't need that info here .. yet
		
		//System.out.println("REQUEST: " + request.toJSONString());		//debug
		
		//Connect
		JSONObject response = request(operation, request.toJSONString());
		//System.out.println("RESPONSE: " + response.toJSONString());			//debug
		
		if (!Connectors.httpSuccess(response)){
			errorCode = 3;
			return errorCode;
		}else{
			//save statistics on successful data transfer
			Statistics.add_DB_hit();
			Statistics.save_DB_total_time(tic);
			
			errorCode = 0;
			return errorCode;
		}	
	}
	
	/**
	 * Delete whole item of a table by primaryKey.
	 * @param tableName - name of the table to check, usually "users"
	 * @param primaryKey - primary key to search for, e.g. "Guuid"
	 * @param keyValue - value of the primary key to check, e.g. "test@b07z.net"
	 * @return error code: 0 - all good, 3 - DB connection error
	 */
	public static int deleteItem(String tableName, String primaryKey, String keyValue) {
		
		int errorCode = 0;
		
		if (keyValue == null || keyValue.isEmpty()){
			Debugger.println("deleteUser() - key is NULL or EMPTY", 1);
			return 2;
		}
		
		//operation:
		String operation = "DeleteItem";
		
		//primaryKey:
		JSONObject prime = getSearchKey(primaryKey, keyValue);
		
		//JSON request:
		JSONObject request = new JSONObject();
		JSON.add(request, "TableName", tableName);
		JSON.add(request, "Key", prime);
		JSON.add(request, "ReturnValues", "NONE");
		
		//System.out.println("REQUEST: " + request.toJSONString());		//debug
		
		//Connect
		JSONObject response = DynamoDB.request(operation, request.toJSONString());
		//System.out.println("RESPONSE: " + response.toJSONString());			//debug
		
		if (!Connectors.httpSuccess(response)){
			errorCode = 3;
			Debugger.println("deleteUser() - DynamoDB Response: " + response.toJSONString(), 1);
			return errorCode;
		}else{
			errorCode = 0;
			return errorCode;
		}	
	}
	
	/**
	 * Create a table with a primary key (String) and optionally secondary index (also String). Provisioned throughput is 5 each.
	 * No RANG-Key specified.
	 * @param tableName - e.g. users
	 * @param primaryKey - e.g. Guuid
	 * @param secondaryIndex - e.g. Email
	 * @return request message as JSON
	 */
	public static JSONObject createSimpleTable(String tableName, String primaryKey, String secondaryIndex){
		JSONObject request = new JSONObject();
		
		JSON.put(request, "TableName", tableName);
		
		JSONArray attributeDefinitions = new JSONArray();
		JSON.add(attributeDefinitions, JSON.make("AttributeName", primaryKey, "AttributeType", "S"));
		if (Is.notNullOrEmpty(secondaryIndex)){
			JSON.add(attributeDefinitions, JSON.make("AttributeName", secondaryIndex, "AttributeType", "S"));
		}
		JSON.put(request, "AttributeDefinitions", attributeDefinitions);
		
		JSONArray keySchema = new JSONArray();
		JSON.add(keySchema, JSON.make("AttributeName", primaryKey, "KeyType", "HASH"));
		//JSON.add(attributeDefinitions, JSON.make("AttributeName", sortKey, "KeyType", "RANGE"));
		JSON.put(request, "KeySchema", keySchema);
		
		JSON.put(request, "ProvisionedThroughput", JSON.make("ReadCapacityUnits", 5, "WriteCapacityUnits", 5));
		
		if (Is.notNullOrEmpty(secondaryIndex)){
			JSONArray globalSecondaryIndexes = new JSONArray();
			JSONArray keySchema2 = new JSONArray();
			JSON.add(keySchema2, JSON.make("AttributeName", secondaryIndex, "KeyType", "HASH"));
			JSONObject globalSecondaryIndexA = JSON.make("IndexName", secondaryIndex,
					"KeySchema", keySchema2,
					"Projection", JSON.make("ProjectionType", "ALL"),
					"ProvisionedThroughput", JSON.make("ReadCapacityUnits", 5, "WriteCapacityUnits", 5));
			JSON.add(globalSecondaryIndexes, globalSecondaryIndexA);
			JSON.put(request, "GlobalSecondaryIndexes", globalSecondaryIndexes);
		}
		
		return request("CreateTable", request.toJSONString());
	}
	/**
	 * Delete table and return JSON answer to request.
	 */
	public static JSONObject deleteTable(String tableName){
		return request("DeleteTable", (JSON.make("TableName", tableName)).toJSONString());
	}
	/**
	 * Request table info and return JSON answer.
	 */
	public static JSONObject describeTable(String tableName){
		return request("DescribeTable", (JSON.make("TableName", tableName)).toJSONString());
	}
	/**
	 * Request table list and return first 10 tables.
	 */
	public static JSONObject listTables(){
		return request("ListTables", (JSON.make("Limit", 10)).toJSONString());
	}
	
	//---------most basic stuff----------
	
	/**
	 * Request stuff from AWS DynamoDB via HTTP POST, Connectors.httpSuccess(result) can be used for POST status.
	 * @param operation - database operation (http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Operations.html)
	 * @param request_parameters - JSON string describing the operation
	 * @return JSON string with Connectors.httpSuccess(result):true and info or Connectors.httpSuccess(result):false and "error"
	 */
	public static JSONObject request(String operation, String request_parameters) {
		
		//AWS DynamoDB API Access
		//http://docs.aws.amazon.com/general/latest/gr/sigv4-signed-request-examples.html
		
		try{
			//method and connection
			String method = "POST";
			String service = ConfigDynamoDB.service;
			String region = ConfigDynamoDB.getRegion(Config.defaultRegion); 	//"us-east-1"; //"eu-central-1";
			String host = ConfigDynamoDB.getHost();
			String endpoint = ConfigDynamoDB.getEndpoint();
			String content_type = "application/x-amz-json-1.0"; //"application/json";
			
			//operation
			//String amz_target = "DynamoDB_20120810.DescribeTable";		//DynamoDB_<API version>.<operationName>
			String amz_target = "DynamoDB_20120810." + operation;
			//String request_parameters = "{\"TableName\": \"Users\"}";		//JSON formatted request according to operation
			String payload_hash = Security.bytearrayToHexString(Security.getSha256(request_parameters));
			String content_length = Integer.toString(request_parameters.getBytes("UTF-8").length);

			//time stamps
			Date date = new Date();
			String amz_date = DateTime.getGMT(date, "yyyyMMdd'T'HHmmss'Z'");
			String date_stamp = DateTime.getGMT(date, "yyyyMMdd");
			
			//*Debug
			//System.out.println(method);		System.out.println(endpoint);	System.out.println(request_parameters);
			//*/
			
			//canonicals
			String canonical_uri = "/";				
			String canonical_querystring = "";
			String canonical_headers = "content-length:" + content_length + "\n" + "content-type:" + content_type + "\n" + "host:" + host + "\n" + "x-amz-date:" + amz_date + "\n" + "x-amz-target:" + amz_target + "\n";
			String signed_headers = "content-length;content-type;host;x-amz-date;x-amz-target";
			String canonical_request = method + "\n" + canonical_uri + "\n" + canonical_querystring + "\n" + canonical_headers + "\n" + signed_headers + "\n" + payload_hash;

			//*Debug
			//System.out.println("---canonical req.---");		System.out.println(canonical_request);
			//*/
			
			//String to sign
			String algorithm = "AWS4-HMAC-SHA256";
			String credential_scope = date_stamp + "/" + region + "/" + service + "/" + "aws4_request";
			String string_to_sign = algorithm + "\n" +  amz_date + "\n" +  credential_scope + "\n" +  Security.bytearrayToHexString(Security.getSha256(canonical_request));
			
			//*Debug
			//System.out.println("---string to sign---");		System.out.println(string_to_sign);
			//*/
			
			byte[] signing_key = Security.getAwsSignatureKey(Config.amazon_dynamoDB_secret, date_stamp, region, service);
			String signature = Security.bytearrayToHexString(Security.HmacSHA256(string_to_sign, signing_key));
			
			//prepare headers for POST
			String authorization_header = algorithm + " " + "Credential=" + Config.amazon_dynamoDB_access + "/" + credential_scope + ", " +  "SignedHeaders=" + signed_headers + ", " + "Signature=" + signature;
			
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", content_type);
			headers.put("Content-Length", content_length);
			headers.put("X-Amz-Date", amz_date);
			headers.put("X-Amz-Target", amz_target);
			headers.put("Authorization", authorization_header);
					
			//POST request
			//System.out.println("time before POST: " + (System.currentTimeMillis()-tic) + "ms");		//debug
			JSONObject response = Connectors.httpPOST(endpoint, request_parameters, headers);
			//System.out.println("RESPONSE - read_basics: " + response.toJSONString());		//debug
			
			if (!Connectors.httpSuccess(response)){
				Debugger.println("DynamoDB.request - DynamoDB Response: " + response.toJSONString(), 1);		//debug
				Debugger.println("DynamoDB.request - DynamoDB Request was: " + request_parameters, 1);			//debug
			}
			
			return response;
			
		}catch (Exception e){
			JSONObject result =	new JSONObject();
			JSON.add(result, Connectors.HTTP_REST_SUCCESS, Boolean.FALSE);
			JSON.add(result, "error", e.toString());
			e.printStackTrace();
			return result;
		}
	}
	
//------------------------------------Tools---------------------------------------
	
	/**
	 * Build the JSONObject used as primary key for user account requests.
	 * @param userID - user to lookup
	 */
	public static JSONObject getPrimaryUserKey(String userID){
		JSONObject prime = new JSONObject();
			JSONObject id = JSON.add(new JSONObject(), "S", userID.toLowerCase().trim());
		JSON.add(prime, PRIMARY_USER_KEY, id);
		return prime;
	}
	/**
	 * Build the JSONObject used as primary key for ticket table requests.
	 * @param ticketID - user to lookup
	 */
	public static JSONObject getPrimaryTicketKey(String ticketID){
		JSONObject prime = new JSONObject();
			JSONObject id = JSON.add(new JSONObject(), "S", ticketID.toLowerCase().trim());
		JSON.add(prime, PRIMARY_TICKET_KEY, id);
		return prime;
	}
	/**
	 * Build the JSONObject used as search key for DB requests (primary or secondary keys).
	 * @param key - primary or one of the secondary keys
	 * @param value - value of key to search
	 */
	public static JSONObject getSearchKey(String key, String value){
		JSONObject prime = new JSONObject();
			JSONObject id = JSON.add(new JSONObject(), "S", value.toLowerCase().trim());
		JSON.add(prime, key, id);
		return prime;
	}
	
	/**
	 * Dig down a DynamoDB map structure till the lowest level or null is reached. Toggles between value and "M" to go down.
	 * @param value - JSONObject to begin with
	 * @param nextKeys - set of keys to follow down the path (String[]) like {adr, uhome, city}
	 * @param level - level to start with, should be 0 usually, iteration happens automatically.
	 * @return last JSONObject in the path or null
	 */
	public static JSONObject dig(JSONObject value, String[] nextKeys, int level){
		if (value != null){
			JSONObject nextValue = (JSONObject) value.get(nextKeys[level]);
			if (nextValue != null && level < nextKeys.length-1){
				//return digOdd(nextValue, nextKeys, level);
				return dig((JSONObject) nextValue.get("M"), nextKeys, level+1);
			}else{
				return nextValue;
			}
		}else{
			return null;
		}
	}
	/**
	 * Dig down a DynamoDB "Item" to search key. The key can include "."-dots to describe the dig path. 
	 * @param item - top level answer of DynamoDB called "Item" 
	 * @param key - key to look for like ACCOUNT.USER_NAME_FIRST etc. ... 
	 * @return object or null
	 */
	public static JSONObject dig(JSONObject item, String key){
		return dig(item, key.split("\\."), 0);
	}
	
	/**
	 * Get the JSONObject result found by dig(...) and convert it to its real type. 
	 * @param item - item to convert, e.g. "{"S":"this is a string"}
	 * @return object or null
	 */
	public static Object typeConversion(JSONObject item){
		//DynamoDB keys possible: S,N,BOOL,M,L,B,BS,NS,SS,NULL
		//simple ones:
		if (item != null){
			try{
				//TODO: add more types here and in back conversion? Or just insist on ArrayList to be used for arrays and stuff?
				if (item.containsKey("S")){
					String found = (String) item.get("S");
					return found;
				}else if (item.containsKey("N")){
					double found = Double.valueOf((String) item.get("N"));
					return found;
				}else if (item.containsKey("BOOL")){
					boolean found = (boolean) item.get("BOOL");
					return found;
				}else if (item.containsKey("M")){
					HashMap<String, Object> found = jsonToMap((JSONObject) item.get("M"));
					return found;
				}else if (item.containsKey("L")){
					ArrayList<Object> found = jsonToList((JSONArray) item.get("L"));
					return found;
				}else{
					return null;
				}
				
			}catch (Exception e){
				e.printStackTrace();
				return null;
			}
		}else{
			return null;
		}
	}
	
	/**
	 * Get the real type of the object and make a dynamoDB element like "{"S":"some string"}".
	 * Note that this does a lot of unchecked class casting, so please test it thoroughly before using it!
	 * @param obj - object to cast, supported: (hashMap&#60;String, Object&#62;, ArrayList&#60;Object&#62;, String, Double, Integer, Boolean, Short, Float, Long)
	 * @return JSONObject in dynamoDB style (object can be empty)
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject typeConversionDynamoDB(Object obj){
		JSONObject dyn = new JSONObject();
		if (obj != null){
			Class<?> c = obj.getClass();
			//map
			if (c.equals((new HashMap<String, Object>()).getClass())){
				dyn = mapToJSON((HashMap<String, Object>) obj);
			//list
			}else if (c.equals(new ArrayList<Object>().getClass())){
				dyn = listToJSON((ArrayList<Object>) obj);
			//JSONArray list
			}else if (c.equals(new JSONArray().getClass())){
				dyn = jsonArrayToJSON((JSONArray) obj);
			//string
			}else if (c.equals(String.class)){
				dyn.put("S", obj.toString());
			//number
			}else if (c.equals(Integer.class) || c.equals(Double.class) || c.equals(Long.class) || c.equals(Float.class) || c.equals(Short.class)){
				dyn.put("N", obj.toString());
			//boolean
			}else if (c.equals(Boolean.class)){
				dyn.put("BOOL", obj.toString());
			//other is always string
			}else{
				dyn.put("S", obj.toString());
			}
		}
		return dyn;
	}
	
	/**
	 * Convert dynamoDB JSONObject map to java hashMap&#60;String, Object&#62;.
	 * @param item - map in JSONObject format to convert
	 * @return
	 */
	public static HashMap<String, Object> jsonToMap(JSONObject item){
		HashMap<String, Object> map = new HashMap<String, Object>();
		//populate map
		if (item != null){
			for (Object o : item.keySet()){
				String s = (String) o;
				Object element = typeConversion((JSONObject) item.get(s));
				map.put(s, element);
				//System.out.println("<" + s + ", " + element + ">");	 //debug
			}
		}
		return map;
	}
	/**
	 * Convert a hashMap&#60;String, Object&#62; to dynamoDB JSON map string. Note that this does a lot of unchecked 
	 * class casting, so please test it thoroughly before using it!
	 * @param map - hashMap&#60;String, Object&#62; to convert
	 * @return string compatible to dynamoDB map
	 */
	public static JSONObject mapToJSON(HashMap<String, Object> map){
		JSONObject result = new JSONObject();
		//go through all keys
		JSONObject kv = new JSONObject();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			//this key/value pair:
			String k = entry.getKey();
			Object o = entry.getValue();
			JSONObject v = typeConversionDynamoDB(o);
			//System.out.println(entry.getKey() + " = " + entry.getValue());
			JSON.add(kv, k, v);
		}
		JSON.add(result, "M", kv);
		return result;
	}
	
	/**
	 * Convert an ArrayList&#60;Object&#62; to dynamoDB JSON list string. Note that this does a lot of unchecked 
	 * class casting, so please test it thoroughly before using it!
	 * @param list - ArrayList&#60;Object&#62; to convert
	 * @return string compatible to dynamoDB list
	 */
	public static JSONObject listToJSON(ArrayList<Object> list){
		JSONObject result = new JSONObject();
		//go through all keys
		JSONArray a = new JSONArray();
		for (Object o : list) {
			JSONObject v = typeConversionDynamoDB(o);
			JSON.add(a, v);
		}
		JSON.add(result, "L", a);
		return result;
	}
	/**
	 * Convert an JSONArray to dynamoDB JSON list string. Note that this does a lot of unchecked 
	 * class casting, so please test it thoroughly before using it!
	 * @param list - JSONArray to convert
	 * @return string compatible to dynamoDB list
	 */
	public static JSONObject jsonArrayToJSON(JSONArray list){
		JSONObject result = new JSONObject();
		//go through all keys
		JSONArray a = new JSONArray();
		for (Object o : list) {
			JSONObject v = typeConversionDynamoDB(o);
			JSON.add(a, v);
		}
		JSON.add(result, "L", a);
		return result;
	}
	/**
	 * Convert dynamoDB JSONObject list to java ArrayList&#60;Object&#62;.
	 * @param item - map in JSONObject format to convert
	 * @return
	 */
	public static ArrayList<Object> jsonToList(JSONArray item){
		ArrayList<Object> list = new ArrayList<Object>();
		//populate list
		if (!item.isEmpty()){
			for (Object o : item){
				list.add(typeConversion((JSONObject) o));
			}
			//item.forEach(p -> list.add(typeConversion((JSONObject) p)));
		}
		return list;
	}

}
