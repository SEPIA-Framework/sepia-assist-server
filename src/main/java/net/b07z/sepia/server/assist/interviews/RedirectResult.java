package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.assistant.CmdBuilder;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * In case there is a non-final result use this "quasi"-API class.
 * 
 * @author Florian Quirin
 *
 */
public class RedirectResult implements ServiceInterface{
	
	@Override
	public ServiceInfo getInfo(String language) {
		//type
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, true);
		
		//Parameters:
		//nada
		
		//Answers:
		info.addSuccessAnswer("redirect_result_0a")
			.addFailAnswer("abort_0a");
			//.addAnswerParameters("cmd");
		
		return info;
	}
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		return get(nluResult);
	}
	
	/**
	 * Get "redirect" result with default key "redirect_result_0a".
	 * @return
	 */
	public static ServiceResult get(NluResult nluResult){
		return get(nluResult, "redirect_result_0a");
	}
	/**
	 * Get "redirect" answer with custom answer key.
	 * @param nluResult
	 * @param answerKey - link to answer database like "redirect_result_0a"
	 * @return
	 */
	public static ServiceResult get(NluResult nluResult, String answerKey){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		Debugger.println("cmd: redirect_result", 2);		//debug
		Debugger.println("SERVICE REDIRECT: '" + nluResult.getCommand() + "' - text: " + nluResult.input.textRaw, 3);		//debug
		
		//get answer
		api.answer = Answers.getAnswerString(nluResult, answerKey);		//default is "redirect_result_0a"
		api.answerClean = Converters.removeHTML(api.answer);
		
		//websearch action
		api.addAction(ACTIONS.BUTTON_CMD);
		api.putActionInfo("title", "Web Search");
		api.putActionInfo("info", "direct_cmd");
		api.putActionInfo("cmd", CmdBuilder.getWebSearch(nluResult.input.textRaw));
		api.putActionInfo("options", JSON.make(ACTIONS.SKIP_TTS, true));
		
		api.resultInfoPut("cmd", nluResult.getCommand());
		
		//no result makes ILA sad :-(
		//api.mood = Assistant.mood_decrease(api.mood);
		
		//anything else?
		api.context = CMD.RESULT_REDIRECT;	//do we want to reset the context here? I think we should 'cause its really a completely unknown command
		
		//reset input to type: question (tell the client that this is not a question anymore if it was one)
		api.responseType = ServiceBuilder.RESPONSE_INFO;
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
