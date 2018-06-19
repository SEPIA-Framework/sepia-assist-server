package net.b07z.sepia.server.assist.database;

import java.net.URLEncoder;
import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.database.DatabaseInterface;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to handle an Elasticsearch node.
 * 
 * @author Florian Quirin
 *
 */
public class Elasticsearch implements DatabaseInterface {
	
	//ElasticSearch address
	final String server = ConfigElasticSearch.getEndpoint(Config.defaultRegion);
	
	/**
	 * Create Elasticsearch class with server defined during Start.loadSettings(). 
	 */
	public Elasticsearch(){
		//changing server is not supported right now
	}
	
	//-------INTERFACE IMPLEMENTATIONS---------
	
	//SET
	public int setItemData(String index, String type, String item_id, JSONObject data) {
		return writeDocument(index, type, item_id, data);
	}
	public JSONObject setAnyItemData(String index, String type, JSONObject data) {
		return writeDocument(index, type, data);
	}
	//GET
	public JSONObject getItem(String index, String type, String item_id) {
		return getDocument(index, type, item_id);
	}
	public JSONObject getItemFiltered(String index, String type, String item_id, String[] filters) {
		//convert filters to sources-string
		String sources = "";
		for (String f : filters){
			sources += f.trim() + ",";
		}
		sources = sources.replaceFirst(",$", "").trim();
		return getDocument(index, type, item_id, sources);
	}
	//UPDATE
	public int updateItemData(String index, String type, String item_id, JSONObject data) {
		return updateDocument(index, type, item_id, data);
	}
	//SEARCH SIMPLE
	public JSONObject searchSimple(String path, String search_term){		
		//Build URL
		if (!path.endsWith("/")) { path = path + "/"; }
		try{
			String url = server + "/" + path + "_search?q=" + URLEncoder.encode(search_term, "UTF-8");
		
			JSONObject result = Connectors.httpGET(url);
			//System.out.println(result.toJSONString()); 		//debug
			
			//success?
			if (Connectors.httpSuccess(result)){
				return result;
			}
			//error
			else{
				return result;
			}
		//error
		}catch (Exception e){
			JSONObject res = new JSONObject();
			JSON.add(res, "error", "request failed! - e: " + e.getMessage());
			JSON.add(res, "code", -1);
			return res;
		}
	}
	//SEARCH COMPLEX
	public JSONObject searchByJson(String path, String jsonQuery) {
		if (!path.endsWith("/")) { path = path + "/"; }
		try{
			String url = server + "/" + path + "_search";
			//System.out.println("url: " + url); 		//debug
			//System.out.println("query: " + jsonQuery); 		//debug
			JSONObject result = Connectors.httpPOST(url, jsonQuery, null);
			//System.out.println(result.toJSONString()); 		//debug
			
			//success?
			if (Connectors.httpSuccess(result)){
				return result;
			}
			//error
			else{
				return result;
			}
		//error
		}catch (Exception e){
			JSONObject res = new JSONObject();
			JSON.add(res, "error", "request failed! - e: " + e.getMessage());
			JSON.add(res, "code", -1);
			return res;
		}
	}
	//DELETE
	public int deleteItem(String index, String type, String item_id) {
		return deleteDocument(index, type, item_id);
	}
	public int deleteAnything(String path) {
		return deleteAny(path);
	}
	//DELETE COMPLEX - delete item that matches query
	public JSONObject deleteByJson(String path, String jsonQuery) {
		if (!path.endsWith("/")) { path = path + "/"; }
		try{
			String url = server + "/" + path + "_delete_by_query";
			//System.out.println("url: " + url); 		//debug
			//System.out.println("query: " + jsonQuery); 		//debug
			JSONObject result = Connectors.httpPOST(url, jsonQuery, null);
			//System.out.println(result.toJSONString()); 		//debug
			
			//success?
			if (Connectors.httpSuccess(result)){
				return result;
			}
			//error
			else{
				return result;
			}
		//error
		}catch (Exception e){
			JSONObject res = new JSONObject();
			JSON.add(res, "error", "request failed! - e: " + e.getMessage());
			JSON.add(res, "code", -1);
			return res;
		}
	}
	
	//--------ELASTICSEARCH METHODS---------
	
	/**
	 * Add a mapping to an index. You can use JSON.readJsonFromFile to get "data" form a JSON file.
	 * @return JSON response with error code.
	 */
	public JSONObject writeMapping(String index, JSONObject data){
		//Build URL
		String url = server + "/" + index;
		
		String dataStr = data.toJSONString();
		
		//Headers
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("Content-Length", Integer.toString(dataStr.getBytes().length));

		JSONObject result = Connectors.httpPUT(url, dataStr, headers);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return JSON.make("code", 0);
		}
		//error
		else{
			Debugger.println("putMapping - ElasticSearch - error in '" + index + "': " + result.toJSONString(), 1);
			return JSON.make("code", 1);
		}
	}
	
	/**
	 * Get all mappings.
	 * @return JSONObject with mappings as keys or null
	 */
	public JSONObject getMappings(){		
		//Build URL
		String url = server + "/" + "_mappings";
		
		JSONObject result = Connectors.httpGET(url);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return result;
		}
		//error
		else{
			Debugger.println("getMappings - ElasticSearch - found no DB or no mappings", 1);
			return null;
		}
	}
	
	/**
	 * Write document at "id" of "type" in "index".
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param id - id name/number, e.g. user_id
	 * @param data - JSON data to put inside id
	 * @return error code (0 - no error, 1 - no connection or fail)
	 */
	public int writeDocument(String index, String type, String id, JSONObject data){		
		//Build URL
		String url = server + "/" + index + "/" + type + "/" + id;
		
		String dataStr = data.toJSONString();
		
		//headers
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("Content-Length", Integer.toString(dataStr.getBytes().length));
		
		JSONObject result = Connectors.httpPUT(url, dataStr, headers);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return 0;
		}
		//error
		else{
			Debugger.println("writeDocument - ElasticSearch - error in '" + index + "/" + type + "': " + result.toJSONString(), 1);
			return 1;
		}
	}
	/**
	 * Write document at random id of "type" in "index".
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param data - JSON data to put inside id
	 * @return JSON with error "code" (0 - no error, 1 - no connection or fail) and "_id" if created
	 */
	public JSONObject writeDocument(String index, String type, JSONObject data){		
		//Build URL
		String url = server + "/" + index + "/" + type;
		//System.out.println("writeDocument URL: " + url); 		//debug
		
		String dataStr = data.toJSONString();
		
		//headers
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("Content-Length", Integer.toString(dataStr.getBytes().length));
		
		JSONObject result = Connectors.httpPOST(url, dataStr, headers);
		//System.out.println("writeDocument Result: " + result.toJSONString()); 				//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return JSON.make("code", 0, "_id", result.get("_id"));
		}
		//error
		else{
			Debugger.println("writeDocument - ElasticSearch - error in '" + index + "/" + type + "': " + result.toJSONString(), 1);
			return JSON.make("code", 1);
		}
	}
	
	/**
	 * Update or create document at "id" of "type" in "index".
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param id - id name/number, e.g. user_id
	 * @param data - JSON data to put inside id (can also include a script, upsert is added automatically)
	 * @return error code (0 - no error, 1 - no connection or fail)
	 */
	public int updateDocument(String index, String type, String id, JSONObject data){		
		return updateDocument(index, type, id, data, 0);
	}
	/**
	 * Update or create document at "id" of "type" in "index".
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param id - id name/number, e.g. user_id
	 * @param data - JSON data to put inside id (can also include a script, upsert is added automatically)
	 * @param retry - number of retries at conflict (default: 0, throws error)
	 * @return error code (0 - no error, 1 - no connection or fail)
	 */
	public int updateDocument(String index, String type, String id, JSONObject data, int retry){
		//Build URL
		String url = server + "/" + index + "/" + type + "/" + id + "/_update";
		if (retry != 0){
			url += ("?retry_on_conflict=" + retry);
		}
		
		//Check data for script and upsert to get update or create behavior
		if (!data.containsKey("script") && !data.containsKey("doc_as_upsert")){
			JSONObject dataUpdate = new JSONObject();
			JSON.put(dataUpdate, "doc", data);
			JSON.put(dataUpdate, "doc_as_upsert", new Boolean(true));
			data = dataUpdate;
		}
		String dataStr = data.toJSONString();
		
		//headers
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("Content-Length", Integer.toString(dataStr.getBytes().length));
		
		JSONObject result = Connectors.httpPOST(url, dataStr, headers);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return 0;
		}
		//error
		else{
			Debugger.println("updateDocument - ElasticSearch - error in '" + index + "/" + type + "': " + result.toJSONString(), 1);
			return 1;
		}
	}
	
	/**
	 * Remove field of document at index/type/id.
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param id - id name/number, e.g. user_id
	 * @param field - field in document to remove (can contain "." for fields of objects)
	 * @return error code: 0 (all good) or 1 (connection error)
	 */
	public int deleteFromDocument(String index, String type, String id, String field){		
		//Build URL
		String url = server + "/" + index + "/" + type + "/" + id + "/_update";
		
		JSONObject data;
		if (field.contains(".")){
			String[] paths = field.split("\\.");
			String path = "";
			String pathField = "";
			if (paths.length == 2){
				path = "." + paths[0];
				pathField = paths[1];
			}else{
				for (int i=0; i<paths.length-1; i++){
					path += ("." + paths[i]); 
				}
				pathField = paths[paths.length-1];
			}
			String inline = "ctx._source" + path + ".remove(params.remField)";
			data = JSON.make("script", JSON.make(
					"inline", inline,
					"params", JSON.make("remField", pathField),
					"lang", "painless"
				)
			);
		}else{
			data = JSON.make("script", JSON.make(
					"inline", "ctx._source.remove(params.remField)",
					"params", JSON.make("remField", field),
					"lang", "painless"
				)
			);
		}
		String dataStr = data.toJSONString();
		
		//headers
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("Content-Length", Integer.toString(dataStr.getBytes().length));
		
		JSONObject result = Connectors.httpPOST(url, dataStr, headers);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return 0;
		}
		//error
		else{
			Debugger.println("deleteFromDocument - ElasticSearch - error in '" + index + "/" + type + "': " + result.toJSONString(), 1);
			return 1;
		}
	}
	
	/**
	 * Get document at path "index/type/id".
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param id - id name/number, e.g. user_id
	 * @return JSONObject with document data or error
	 */
	public JSONObject getDocument(String index, String type, String id){		
		//Build URL
		String url = server + "/" + index + "/" + type + "/" + id;
		
		JSONObject result = Connectors.httpGET(url);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return result;
		}
		//error
		else{
			Debugger.println("getDocument - ElasticSearch - error in '" + index + "/" + type + "': " + result.toJSONString(), 1);
			return result;
		}
	}
	/**
	 * Get document at path "index/type/id" with filtered entries.
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param id - id name/number, e.g. user_id
	 * @param sources - entries in the document you want to retrieve, e.g. "name,address,email", separated by a simple ",". All empty space is removed.
	 * @return JSONObject with document data or null. If sources are missing they are ignored.
	 */
	public JSONObject getDocument(String index, String type, String id, String sources){
		return getDocument(index, type, id + "?_source=" + sources.replaceAll("\\s+", "").trim());
	}
	
	/**
	 * Delete document at "index/type/id".
	 * @param index - index name, e.g. "account"
	 * @param type - type name, e.g. "user"
	 * @param id - id name/number, e.g. user_id
	 * @return error code (0 - no error, 1 - no connection or fail)
	 */
	public int deleteDocument(String index, String type, String id){
		//Build URL
		String url = server + "/" + index + "/" + type + "/" + id;
		
		JSONObject result = Connectors.httpDELETE(url);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return 0;
		}
		//error
		else{
			Debugger.println("deleteDocument - ElasticSearch - error in '" + index + "/" + type + "': " + result.toJSONString(), 1);
			return 1;
		}
	}
	/**
	 * Delete anything.
	 * @param path - e.g. index/type/id
	 * @return error code (0 - no error, 1 - no connection or fail)
	 */
	public int deleteAny(String path){
		//Build URL
		String url = server + "/" + path;
		
		JSONObject result = Connectors.httpDELETE(url);
		//System.out.println(result.toJSONString()); 		//debug
		
		//success?
		if (Connectors.httpSuccess(result)){
			return 0;
		}
		//error
		else{
			Debugger.println("deleteAny - ElasticSearch - error in '" + path + "': " + result.toJSONString(), 1);
			return 1;
		}
	}
}
