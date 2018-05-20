package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Default Dashboard handler.
 * 
 * @author Florian Quirin
 *
 */
public class Open_Dashboard implements ServiceInterface{
	
	//info
	public ServiceInfo getInfo(String language){
		return new ServiceInfo(Type.other, Content.action, true);
	}

	//result
	public ServiceResult getResult(NluResult NLU_result){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result);
		
		//Parameters
		String type = NLU_result.getParameter(PARAMETERS.TYPE);			//common, addresses, settings, etc...
		String action = NLU_result.getParameter(PARAMETERS.ACTION);		//open, add, ... can be used to modify answer
		String info = NLU_result.getParameter(PARAMETERS.INFO);			//details
		String reply = NLU_result.getParameter(PARAMETERS.REPLY);		//custom reply tag used with ADD
		Debugger.println("cmd: Dashboard, type=" + type + ", action=" + action + ", info=" + info, 2);		//debug
		
		//check requirements
		if (type.isEmpty()){
			type = "common";		//default dashboard page (use: settings, addresses, contacts, user_name, user_work, user_home, ...)
		}
		if (action.isEmpty()){
			action = "open";
		}
		
		//make action: dashboard call
		api.addAction(ACTIONS.OPEN_DASHBOARD);
		api.putActionInfo(PARAMETERS.TYPE, type);
		api.putActionInfo(PARAMETERS.INFO, info);
		api.hasAction = true;
		
		//get answer
		if (action.equals("add")){
			if (!reply.isEmpty()){
				api.answer = Config.answers.getAnswer(NLU_result, reply);
			}else{
				api.answer = Config.answers.getAnswer(NLU_result, "dashboard_0b");
			}
		}else{
			api.answer = Config.answers.getAnswer(NLU_result, "dashboard_0a");
		}
		api.answerClean = Converters.removeHTML(api.answer);
		
		api.status = "success";
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		return result;
	}

}
