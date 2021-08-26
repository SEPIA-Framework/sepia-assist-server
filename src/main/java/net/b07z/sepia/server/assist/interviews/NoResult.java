package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.assistant.ActionBuilder;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * In case there is no result use this "quasi"-API class.
 * No result can be reused for other APIs with custom answers. Therefore it has several get() methods, e.g.
 * get(NLU_result, answer_key) to link to another answer key than the default "no_answer_0a".
 * 
 * @author Florian Quirin
 *
 */
public class NoResult {
	
	/**
	 * Get "no answer" result with default key "no_answer_0a".
	 * @param NLU_result
	 * @return
	 */
	public static ServiceResult get(NluResult NLU_result){
		return get(NLU_result, "no_answer_0a");
	}
	/**
	 * Get "no answer" with custom answer key.
	 * @param nluResult
	 * @param answerKey - link to answer database like "no_answer_0a"
	 * @return
	 */
	public static ServiceResult get(NluResult nluResult, String answerKey){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		Debugger.println("cmd: No Result", 2);		//debug
		Debugger.println("NO_RESULT: " + nluResult.input.textRaw + " - LANGUAGE: " + nluResult.language + " - CLIENT: " + nluResult.input.clientInfo + " - API: " + Config.apiVersion, 3);		//debug
		
		//get answer
		api.answer = Answers.getAnswerString(nluResult, answerKey);		//default is "no_answer_0a"
		api.answerClean = Converters.removeHTML(api.answer);
		
		//websearch action
		ActionBuilder.addWebSearchButton(api, nluResult.input.textRaw, null);
		
		//help button
		api.addAction(ACTIONS.BUTTON_HELP);
		
		//teach UI button
		api.addAction(ACTIONS.BUTTON_TEACH_UI);
		api.putActionInfo("info", JSON.make("input", nluResult.input.textRaw));
		
		//no result makes ILA sad :-(
		//api.mood = Assistant.mood_decrease(api.mood);
		
		//anything else?
		api.context = CMD.NO_RESULT;	//do we want to reset the context here? I think we should 'cause its really a completely unknown command
		
		//reset input to type: question (tell the client that this is not a question anymore if it was one)
		api.responseType = ServiceBuilder.RESPONSE_INFO;
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
