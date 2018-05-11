package net.b07z.sepia.server.assist.endpoints;

import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Address;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import spark.Request;
import spark.Response;

/**
 * Endpoint to get and set database entries that are connected to users (everything not related to the teach-server).
 * Compared to the {@link AccountEndpoint} (small core set of info) this handles things like addresses, contact-lists, todo-lists etc.
 * 
 * @author Florian Quirin
 *
 */
public class UserDataEndpoint {

	/**---USER-DATA ACCESS API---<br>
	 * End-point to get and set database entries that are connected to account settings (everything not related to the teach-server).
	 */
	public static String userdataAPI(Request request, Response response) {
		long tic = System.currentTimeMillis();
	
		//get parameters (or throw error)
		RequestParameters params = new RequestPostParameters(request);
		
		//first try to authenticate the user
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}
		
		//get action - get/set/delete
		JSONObject get = params.getJson("get");
		JSONObject set = params.getJson("set");
		JSONObject delete = params.getJson("delete");
		
		//no action
		if (get == null && set == null && delete == null){
			//FAIL
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", "one of these fields is required: 'set', 'get' or 'delete' (all JSON objects)");
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}
		
		//create user
		User user = new User(null, token);
		
		//init. answer
		JSONObject msg = new JSONObject();
		
		//GET DATA
		if (get != null){
			
			JSONObject getResult = new JSONObject();
			
			//LISTS
			JSONArray lists = JSON.getJArray(get, ACCOUNT.LISTS);
			if (lists != null){
				JSONArray listsFound = new JSONArray();
				for (Object jo : lists){
					JSONObject list = (JSONObject) jo;
					String indexType = JSON.getString(list, "indexType");
					String sectionName = JSON.getString(list, "section");
					//TODO: add load by section instead of indexType?
					//NOTE: loading, storing and deleting stops after the first error and ignores the remaining requests!
					if (Is.nullOrEmpty(sectionName) || Is.nullOrEmpty(indexType) || !UserDataList.indexTypeContains(indexType)){
						JSON.put(getResult, ACCOUNT.LISTS, listsFound);
						JSON.put(msg, "get_result", getResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT load all lists! Missing or invalid 'section' or 'indexType'.");
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
					UserDataInterface userData = user.getUserDataAccess();
					HashMap<String, Object> filters = (HashMap<String, Object>) Converters.json2HashMap(list);
					List<UserDataList> udlList = userData.getUserDataList(user, Section.valueOf(sectionName), indexType, filters);
					if (udlList == null){
						JSON.put(getResult, ACCOUNT.LISTS, listsFound);
						JSON.put(msg, "get_result", getResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT load list: " + indexType + ", " + sectionName);
						JSON.add(msg, "result_code", 3);
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}else{
						//ADD result - NOTE: for each list request you get an array of results, so in the end its an array of arrays!
						JSON.add(listsFound, UserDataList.convertManyListsToJSONArray(udlList));
					}
				}
				JSON.put(getResult, ACCOUNT.LISTS, listsFound);
			}
			
			//ADDRESSES
			JSONArray addresses = JSON.getJArray(get, ACCOUNT.ADDRESSES);
			if (addresses != null){
				JSONArray addressesFound = new JSONArray();
				for (Object jo : addresses){
					JSONObject adr = (JSONObject) jo;
					String tag = JSON.getString(adr, "specialTag");
					//TODO: support name
					if (Is.nullOrEmpty(tag)){
						JSON.put(getResult, ACCOUNT.ADDRESSES, addressesFound);
						JSON.put(msg, "get_result", getResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT load all addresses! Missing or invalid 'specialTag' (or 'specialName').");
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
					UserDataInterface userData = user.getUserDataAccess();
					Address adrFound = userData.getAddressByTag(user, tag);
					if (adrFound == null){
						JSON.put(getResult, ACCOUNT.ADDRESSES, addressesFound);
						JSON.put(msg, "get_result", getResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT load address with specialTag: " + tag);
						JSON.add(msg, "result_code", 3);
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}else{
						//ADD result - it's an array because with 'name' we might get more than 1 result
						if (adrFound.wasCreatedEmpty()){
							JSON.add(addressesFound, JSON.makeArray(new JSONObject()));
						}else{
							JSON.add(addressesFound, JSON.makeArray(adrFound.buildJSON())); 
						}
					}
				}
				JSON.put(getResult, ACCOUNT.ADDRESSES, addressesFound);
			}
			
			JSON.put(msg, "get_result", getResult);
		}
		
		//SET DATA
		if (set != null){
			
			JSONObject setResult = new JSONObject();
			
			//LISTS
			JSONArray lists = JSON.getJArray(set, ACCOUNT.LISTS);
			if (lists != null){
				JSONArray listsSet = new JSONArray();
				for (Object jo : lists){
					JSONObject list = (JSONObject) jo;
					String indexType = JSON.getString(list, "indexType");
					String sectionName = JSON.getString(list, "section");
					if (Is.nullOrEmpty(sectionName) || Is.nullOrEmpty(indexType) || !UserDataList.indexTypeContains(indexType)){
						JSON.put(setResult, ACCOUNT.LISTS, listsSet);
						JSON.put(msg, "set_result", setResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT save all lists! Missing or invalid 'section' or 'indexType'.");
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
					UserDataInterface userData = user.getUserDataAccess();
					UserDataList udl = new UserDataList(list);
					JSONObject statusList = userData.setUserDataList(user, Section.valueOf(sectionName), udl);
					if (!statusList.containsKey("status") || !((String) statusList.get("status")).equals("success")){
						JSON.put(setResult, ACCOUNT.LISTS, listsSet);
						JSON.put(msg, "set_result", setResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT save list: " + indexType + ", " + sectionName);
						JSON.add(msg, "result_code", statusList.get("code"));
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					
					}else{
						//ADD result
						JSON.add(listsSet, JSON.make("_id", statusList.get("_id"), "indexType", indexType, "section", sectionName));
					}
				}
				JSON.put(setResult, ACCOUNT.LISTS, listsSet);
			}
			
			//ADDRESSES
			JSONArray addresses = JSON.getJArray(set, ACCOUNT.ADDRESSES);
			if (addresses != null){
				JSONArray addressesSet = new JSONArray();
				for (Object jo : addresses){
					JSONObject adr = (JSONObject) jo;
					String tag = JSON.getString(adr, "specialTag");
					//TODO: support name
					if (Is.nullOrEmpty(tag)){
						JSON.put(setResult, ACCOUNT.ADDRESSES, addressesSet);
						JSON.put(msg, "set_result", setResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT save all addresses! Missing or invalid 'tag' (or 'name').");
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
					UserDataInterface userData = user.getUserDataAccess();
					JSONObject statusAdr = userData.setOrUpdateSpecialAddress(user, tag, null, adr);
					if (!statusAdr.containsKey("status") || !((String) statusAdr.get("status")).equals("success")){
						JSON.put(setResult, ACCOUNT.ADDRESSES, addressesSet);
						JSON.put(msg, "set_result", setResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT save address with tag: " + tag);
						JSON.add(msg, "result_code", statusAdr.get("code"));
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					
					}else{
						//ADD result
						JSON.add(addressesSet, JSON.make("_id", statusAdr.get("_id"), "specialTag", tag));
					}
				}
				JSON.put(setResult, ACCOUNT.ADDRESSES, addressesSet);
			}
			
			JSON.put(msg, "set_result", setResult);
		}
		
		//DELETE DATA
		if (delete != null){
			
			JSONObject delResult = new JSONObject();
			
			//LISTS
			JSONArray lists = JSON.getJArray(delete, ACCOUNT.LISTS);
			if (lists != null){
				JSONArray listsDeleted = new JSONArray();
				for (Object jo : lists){
					JSONObject list = (JSONObject) jo;
					String id = JSON.getString(list, "_id");
					if (Is.nullOrEmpty(id)){
						JSON.put(delResult, ACCOUNT.LISTS, listsDeleted);
						JSON.put(msg, "delete_result", delResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT delete all lists! Missing or invalid 'id'.");
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
					UserDataInterface userData = user.getUserDataAccess();
					HashMap<String, Object> filters = (HashMap<String, Object>) Converters.json2HashMap(list);
					long deleteResult = userData.deleteUserDataList(user, id, filters);
					if (deleteResult == -1 || deleteResult == 0){
						JSON.put(delResult, ACCOUNT.LISTS, listsDeleted);
						JSON.put(msg, "delete_result", delResult);
						JSON.add(msg, "result", "fail");
						if (deleteResult == 0){
							JSON.add(msg, "error", "no matching list found for id: " + id);
							JSON.add(msg, "result_code", 0);
						}else{
							JSON.add(msg, "error", "could NOT delete list with id: " + id);
							JSON.add(msg, "result_code", 3);
						}
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}else{
						//ADD result
						JSON.add(listsDeleted, JSON.make("_id", id, "deleted", deleteResult));
					}
				}
				JSON.put(delResult, ACCOUNT.LISTS, listsDeleted);
			}
			
			//ADDRESSES
			JSONArray addresses = JSON.getJArray(delete, ACCOUNT.ADDRESSES);
			if (addresses != null){
				JSONArray adrsDeleted = new JSONArray();
				for (Object jo : addresses){
					JSONObject adr = (JSONObject) jo;
					//TODO
					String id = JSON.getString(adr, "_id");
					if (Is.nullOrEmpty(id)){
						JSON.put(delResult, ACCOUNT.ADDRESSES, adrsDeleted);
						JSON.put(msg, "delete_result", delResult);
						JSON.add(msg, "result", "fail");
						JSON.add(msg, "error", "could NOT delete all addresses! Missing or invalid 'id'.");
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
					UserDataInterface userData = user.getUserDataAccess();
					//HashMap<String, Object> filters = (HashMap<String, Object>) Converters.Json2HashMap(adr); 	//use?
					long deleteResult = userData.deleteAddress(user, id, null);
					if (deleteResult == -1 || deleteResult == 0){
						JSON.put(delResult, ACCOUNT.ADDRESSES, adrsDeleted);
						JSON.put(msg, "delete_result", delResult);
						JSON.add(msg, "result", "fail");
						if (deleteResult == 0){
							JSON.add(msg, "error", "no matching address found for id: " + id);
							JSON.add(msg, "result_code", 0);
						}else{
							JSON.add(msg, "error", "could NOT delete address with id: " + id);
							JSON.add(msg, "result_code", 3);
						}
						JSON.add(msg, "duration_ms", Debugger.toc(tic));
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}else{
						//ADD result
						JSON.add(adrsDeleted, JSON.make("_id", id, "deleted", deleteResult));
					}
				}
				JSON.put(delResult, ACCOUNT.ADDRESSES, adrsDeleted);
			}
			
			JSON.put(msg, "delete_result", delResult);
		}
		
		//write basic statistics for user
		user.saveStatistics();
		
		//all write success
		JSON.add(msg, "result", "success");
		JSON.add(msg, "duration_ms", Debugger.toc(tic));
		return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
	}

}
