package net.b07z.sepia.server.assist.services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.events.EventsManager;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class that makes events available as service.
 * @see EventsManager
 * 
 * @author Florian Quirin
 *
 */
public class EventsWrapper implements ServiceInterface{
	
	@Override
	public ServiceInfo getInfo(String language) {
		//type
		ServiceInfo info = new ServiceInfo(Type.other, Content.action, false);
		
		//Answers:
		info.addSuccessAnswer("<silent>events_personal_0a")		//"<direct>:-)"
			.addFailAnswer("error_0a");
		
		return info;
	}
	@Override
	public ServiceResult getResult(NluResult NLU_result) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result, getInfo(""));
		
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
		ServiceResult result = api.buildResult();
		
		return result;
	}

}
