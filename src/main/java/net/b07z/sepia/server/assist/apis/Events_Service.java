package net.b07z.sepia.server.assist.apis;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.API;
import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.events.EventsManager;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class that makes events available as service
 * 
 * @author Florian Quirin
 *
 */
public class Events_Service implements ApiInterface{
	
	@Override
	public ApiInfo getInfo(String language) {
		//type
		ApiInfo info = new ApiInfo(Type.other, Content.action, false);
		
		//Answers:
		info.addSuccessAnswer("<silent>events_personal_0a")		//"<direct>:-)"
			.addFailAnswer("error_0a");
		
		return info;
	}
	@Override
	public ApiResult getResult(NluResult NLU_result) {
		//initialize result
		API api = new API(NLU_result, getInfo(""));
		
		Debugger.println("cmd: trigger events as service", 2);		//debug
		
		try{
			//get events
			JSONObject actionsJSON = EventsManager.buildCommonEvents(NLU_result.input);
			
			//action
			api.actionInfo = (JSONArray) actionsJSON.get("actions");
			api.hasAction = true;
						
			api.status = "success";
		
		}catch (Exception e){
			Debugger.println("Events_Service - failed with message: " + e.getMessage(), 1);
		}
				
		//build the API_Result
		ApiResult result = api.build_API_result();
		
		return result;
	}

}
