package net.b07z.sepia.server.assist.apis;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.API;
import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.events.EventsManager;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class that handles NPS feedback
 * 
 * @author Florian Quirin
 *
 */
public class Feedback_NPS implements ApiInterface{
	
	//---data---
	public static String getButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "jetzt teilnehmen";
		}else{
			return "participate now";
		}
	}
	
	private static String skipQuestionParameter = "skipQuestion"; 		//cutstom parameter: true, false
	//----------
	
	@Override
	public ApiInfo getInfo(String language) {
		//type
		ApiInfo info = new ApiInfo(Type.other, Content.action, false);
		
		//Parameters:
		//required
		Parameter p1 = new Parameter(PARAMETERS.YES_NO)
				.setRequired(true)
				.setQuestion("feedback_nps_ask_0a");
		info.addParameter(p1);
		
		//Answers:
		info.addSuccessAnswer("feedback_nps_1a")
			.addFailAnswer("feedback_nps_0a");
		
		return info;
	}
	@Override
	public ApiResult getResult(NluResult NLU_result) {
		//initialize result
		API api = new API(NLU_result, getInfo(""));
		
		//get interview parameters
		String skipQuestion = NLU_result.getParameter(skipQuestionParameter);
		JSONObject yesNoJSON = NLU_result.getRequiredParameter(PARAMETERS.YES_NO).getData();
		String yesNo = (String) yesNoJSON.get(InterviewData.VALUE);
		
		Debugger.println("cmd: trigger NPS feedback: " + yesNo, 2);		//debug
		
		if (yesNo.equals("yes") || skipQuestion.equals("true")){
			//action
			api.addAction(ACTIONS.FEEDBACK_NPS);
			api.hasAction = true;
					
			api.status = "success";
		}else{
			api.status = "fail";
			
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getButtonText(api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CMD.FEEDBACK_NPS + ";;" + PARAMETERS.YES_NO + "=yes;;" + skipQuestionParameter + "=true");
			api.putActionInfo("visibility", "inputHidden");
			
			api.addAction(ACTIONS.SCHEDULE_CMD);
			api.putActionInfo("waitForIdle", "true");
			api.putActionInfo("idleTime", 24*1000);
			api.putActionInfo("executeIn", 5*60*60*1000);
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CMD.FEEDBACK_NPS + ";;");
			api.putActionInfo("visibility", "inputHidden");
			api.putActionInfo("eventId", EventsManager.ID_NPS);
			api.putActionInfo("tryMax", "2");
		}
		//System.out.println("Feedback_NPS - array: " + api.actionInfo.toJSONString());	//debug
		
		//finally build the API_Result
		ApiResult result = api.buildApiResult();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
