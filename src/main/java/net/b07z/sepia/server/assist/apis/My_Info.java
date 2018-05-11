package net.b07z.sepia.server.assist.apis;

import java.util.HashMap;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class handling favorite/personal item lists.
 * 
 * @author Florian Quirin
 *
 */
public class My_Info implements ApiInterface{
	
	private static int list_limit = 100;		//this many items are allowed in the list (per language)
	private static int element_limit = 200;		//maximum length of an item (not including formatting code)
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.account, Content.data, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		User user = NLU_result.input.user;
		
		//get parameters
		String type = NLU_result.getParameter(PARAMETERS.TYPE);			//key
		String action = NLU_result.getParameter(PARAMETERS.ACTION);		//action
		String info = NLU_result.getParameter(PARAMETERS.INFO);			//value
		Debugger.println("cmd: My_Info, type=" + type + ", action=" + action + ", info=" + info, 2);		//debug
		
		//check action - if it is empty assume "add" 
		if (action.isEmpty()){
			action = "add";
		}
		//check key type - everything is allowed
		if (type.isEmpty() && (action.equals("add") || action.equals("remove"))){
			if (action.equals("add")){
				return AskClient.question("my_info_ask_type_0a", PARAMETERS.TYPE, NLU_result);
			}else{
				return AskClient.question("my_info_ask_type_0b", PARAMETERS.TYPE, NLU_result);
			}
		}
		//check info value - everything is allowed
		if (info.isEmpty() && action.equals("add")){
			return AskClient.question("my_info_ask_info_0a", PARAMETERS.INFO, NLU_result, type);
		}
		
		//TODO: handle exceptions like name, home address, work address
				
		//Load list
		String listKey = "lists.favs" + "_" + api.language;				//TODO: keep an eye on this!
		int code = user.loadInfoFromAccount(Config.superuserApiMng, listKey);
		//System.out.println("RESULT CODE: " + code); 		//debug
		if (code != 0){
			//build answer - server communication error
			api.answer = Config.answers.getAnswer(NLU_result, "my_info_0a");
			api.answer_clean = Converters.removeHTML(api.answer);
		}
		//continue
		else{
		
			//check list
			HashMap<String, String> map = user.getInfo_Map(listKey);
			if (map == null){
				map = new HashMap<String, String>();
			}
			/*
			System.out.println("----list start----"); 		//debug
			for (Map.Entry<String, String> entry : map.entrySet()) {
				System.out.println(entry.getKey() + " = " + entry.getValue());
			}
			System.out.println("-----list end-----"); 		//debug
			*/
			
			//ACTIONS
			//-ADD
			if (action.equals("add")){
				
				//check size
				if ((info+type).length() > element_limit || map.size() >= list_limit){
					//build answer - element or list is too big
					api.answer = Config.answers.getAnswer(NLU_result, "my_info_0b");
					api.answer_clean = Converters.removeHTML(api.answer);
				}
				//add stuff
				else{
					//save as JSON to add meta info later
					JSONObject item = new JSONObject();
					JSON.add(item, "item", info);
					//map.put(type, info);
					map.put(type, item.toJSONString());
					code = user.saveInfoToAccount(Config.superuserApiMng, listKey, map);
					//System.out.println("RESULT CODE: " + code); 		//debug
					if (code == 0){
						//build answer - all fine I've added stuff
						api.answer = Config.answers.getAnswer(NLU_result, "my_info_1b", type, info);
						api.answer_clean = Converters.removeHTML(api.answer);
					}else{
						//build answer - server communication error
						api.answer = Config.answers.getAnswer(NLU_result, "my_info_0a");
						api.answer_clean = Converters.removeHTML(api.answer);
					}
				}
			}
			//-OPEN
			else if (action.equals("open")){
				//say it
				if (!type.isEmpty()){
					if (!map.isEmpty() && map.containsKey(type)){
						JSONObject item = JSON.parseString(map.get(type));
						//info = map.get(type);
						info = (String) item.get("item");
						//build answer - your x is y
						api.answer = Config.answers.getAnswer(NLU_result, "my_info_1d", type, info);
						api.answer_clean = Converters.removeHTML(api.answer);
					}else{
						//build answer - I have no idea
						api.answer = Config.answers.getAnswer(NLU_result, "my_info_1e");
						api.answer_clean = Converters.removeHTML(api.answer);
					}
					
				//show it
				}else{
					//build answer - ok let me open it
					api.answer = Config.answers.getAnswer(NLU_result, "my_info_1a");
					api.answer_clean = Converters.removeHTML(api.answer);
					
					//convert result
					JSONObject list_obj = new JSONObject();
					JSON.add(list_obj, "data", Converters.mapStrStr2Json(map));
							
					//build action - for apps indicate direct triggering of info view
					api.actionInfo_add_action(ACTIONS.OPEN_LIST);
					api.actionInfo_put_info("info", listKey);
					api.actionInfo_put_info("content", list_obj);
					api.hasAction = true;
				}
			}
			//-REMOVE
			else if (action.equals("remove")){
				//locate info
				if (!map.isEmpty()){
					map.remove(type);
				}
				code = user.saveInfoToAccount(Config.superuserApiMng, listKey, map);
				if (code == 0){
					//build answer - removed it
					api.answer = Config.answers.getAnswer(NLU_result, "my_info_1c", type);
					api.answer_clean = Converters.removeHTML(api.answer);
				}else{
					//build answer - server communication error
					api.answer = Config.answers.getAnswer(NLU_result, "my_info_0a");
					api.answer_clean = Converters.removeHTML(api.answer);
				}
			}
		}
				
		api.status = "success";
				
		//anything else?
		//you can put extras in api.more as you need ...
		
		//finally build the API_Result
		ApiResult result = api.build_API_result();
		
		return result;
	}	

}
