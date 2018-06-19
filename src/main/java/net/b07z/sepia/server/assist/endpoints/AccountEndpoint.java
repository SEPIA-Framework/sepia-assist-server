package net.b07z.sepia.server.assist.endpoints;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import spark.Request;
import spark.Response;

/**
 * API endpoint to handle user account data (smaller set of core data). For larger data check {@link UserDataEndpoint}.
 * 
 * @author Florian Quirin
 *
 */
public class AccountEndpoint {

	/**---ACCOUNT ACCESS API---<br>
	 * Endpoint to get and set account entries.
	 */
	public static String accountAPI(Request request, Response response) {
		long tic = System.currentTimeMillis();
	
		//get parameters (or throw error)
		RequestParameters params = new RequestPostParameters(request);
		
		//first try to authenticate the user
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}
		
		//get action - get/set/delete
		JSONObject set = params.getJson("set");
		JSONArray get = params.getJsonArray("get");
		JSONArray delete = params.getJsonArray("delete");
		
		//no action
		if (get == null && set == null && delete == null){
			//FAIL
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "one of these fields is required: 'set' (JSON object), 'get' or 'delete' (both JSON array)");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}
		
		//create user
		User user = new User(null, token);
		
		//init. answer
		JSONObject msg = new JSONObject();
		
		//GET DATA
		if (get != null){
			//using universal API because user is authenticated - is not really implemented yet ...
			Object[] attributeKeys = get.toArray();
			int statusGet = user.loadInfoFromAccount(Config.superuserApiMng, attributeKeys);
			if (statusGet == 0){
				//fill result with found attributes and make a JSON
				JSONObject res = new JSONObject();
				for (Object o : attributeKeys){
					String k = (String) o;
					JSON.add(res, k, user.info.get(k));
					//System.out.println("k: " + k + ", v: " + user.info.get(k)); 			//debug
				}
				//ADD result
				JSON.add(msg, "get_result", res);
				
			}else{
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "'get' failed - could NOT load attributes!");
				JSON.add(msg, "result_code", statusGet);
				JSON.add(msg, "duration_ms", Debugger.toc(tic));
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
		
		//SET DATA
		if (set != null){
			int statusSet = user.saveInfoToAccount(Config.superuserApiMng, set);
			if (statusSet == 0){
				//ADD result
				JSON.add(msg, "set_result", "saved");
				
			}else{
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "'set' failed - could NOT save data!");
				JSON.add(msg, "result_code", statusSet);
				JSON.add(msg, "duration_ms", Debugger.toc(tic));
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
		
		//DELETE DATA
		if (delete != null){
			Object[] attributeKeys = delete.toArray();
			int statusGet = user.deleteInfoFromAccount(Config.superuserApiMng, attributeKeys);
			if (statusGet == 0){
				//ADD result
				JSON.add(msg, "delete_result", "done");
				
			}else{
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "'delete' failed! Result code might give some more info.");
				JSON.add(msg, "result_code", statusGet);
				JSON.add(msg, "duration_ms", Debugger.toc(tic));
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
		
		//write basic statistics for user - NOTE: make sure to use this at the end or you might get an concurrency error
		user.saveStatistics();
		
		//all write success
		JSON.add(msg, "result", "success");
		JSON.add(msg, "duration_ms", Debugger.toc(tic));
		return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
	}

}
