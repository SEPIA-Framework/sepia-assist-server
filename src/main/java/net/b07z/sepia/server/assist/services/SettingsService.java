package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Default settings/menu handler. This is kind of a special "service" that can be used in other services as well to
 * trigger for example an info text and client action that opens the addresses menu as a helper to show the user what he can do.<br>
 * <br>
 * SEE AS WELL: {@link ConfigServices#settingsService}<br> 
 * <br>
 * NOTE: could partially be replaced with client-controls ?!
 * 
 * @author Florian Quirin
 *
 */
public class SettingsService implements ServiceInterface {
	
	//info
	public ServiceInfo getInfo(String language){
		return new ServiceInfo(Type.other, Content.action, true);
	}

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		//Parameters
		String type = nluResult.getParameter(PARAMETERS.TYPE);			//common, addresses, favorites, contacts, etc...
		String action = nluResult.getParameter(PARAMETERS.ACTION);		//open, add, ... can be used to modify answer
		String info = nluResult.getParameter(PARAMETERS.INFO);			//details
		String reply = nluResult.getParameter(PARAMETERS.REPLY);		//custom reply tag used with ADD
		Debugger.println("cmd: settings, type=" + type + ", action=" + action + ", info=" + info, 2);		//debug
		
		//check requirements
		if (type.isEmpty()){
			type = "common";		//default settings page (use: addresses, contacts, user_name, user_work, user_home, ...)
		}
		if (action.isEmpty()){
			action = "open";
		}
		
		//make action: dashboard call
		api.addAction(ACTIONS.OPEN_SETTINGS);
		api.putActionInfo("section", type);
		api.putActionInfo("info", info);
		api.hasAction = true;
		
		//get answer
		if (action.equals("add")){
			if (!reply.isEmpty()){
				api.answer = Answers.getAnswerString(nluResult, reply);
			}else{
				api.answer = Answers.getAnswerString(nluResult, "settings_0b"); 	//"missing but can be added via settings"
			}
		}else{
			api.answer = Answers.getAnswerString(nluResult, "settings_0a");			//"ok I'll open the settings"
		}
		api.answerClean = Converters.removeHTML(api.answer);
		
		api.status = "success";
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		return result;
	}

}
