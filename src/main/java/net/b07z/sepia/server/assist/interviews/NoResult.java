package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.assistant.CmdBuilder;
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
	 * @param NLU_result
	 * @param answer_key - link to answer database like "no_answer_0a"
	 * @return
	 */
	public static ServiceResult get(NluResult NLU_result, String answer_key){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result);
		
		Debugger.println("cmd: No Result", 2);		//debug
		Debugger.println("NO_RESULT: " + NLU_result.input.textRaw + " - LANGUAGE: " + NLU_result.language + " - CLIENT: " + NLU_result.input.clientInfo + " - API: " + Config.apiVersion, 3);		//debug
		
		//get answer
		api.answer = Config.answers.getAnswer(NLU_result, answer_key);		//default is "no_answer_0a"
		api.answerClean = Converters.removeHTML(api.answer);
		
		//websearch action
		api.addAction(ACTIONS.BUTTON_CMD);
		api.putActionInfo("title", "Websearch");
		api.putActionInfo("info", "direct_cmd");
		api.putActionInfo("cmd", CmdBuilder.getWebSearch(NLU_result.input.textRaw));
		api.putActionInfo("options", JSON.make(ACTIONS.SKIP_TTS, true));
		
		//help button
		api.addAction(ACTIONS.BUTTON_HELP);
		
		//teach UI button
		api.addAction(ACTIONS.BUTTON_TEACH_UI);
		api.putActionInfo("input", NLU_result.input.textRaw);
		
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
