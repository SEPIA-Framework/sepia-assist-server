package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

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
	public ServiceResult getResult(NluResult NLU_result) {
		return get( NLU_result);
	}
	
	/**
	 * Get "redirect" result with default key "redirect_result_0a".
	 * @return
	 */
	public static ServiceResult get(NluResult NLU_result){
		return get(NLU_result, "redirect_result_0a");
	}
	/**
	 * Get "redirect" answer with custom answer key.
	 * @param NLU_result
	 * @param answer_key - link to answer database like "redirect_result_0a"
	 * @return
	 */
	public static ServiceResult get(NluResult NLU_result, String answer_key){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result);
		
		Debugger.println("cmd: redirect_result", 2);		//debug
		Debugger.println("SERVICE REDIRECT: '" + NLU_result.getCommand() + "' - text: " + NLU_result.input.textRaw, 3);		//debug
		
		//get answer
		api.answer = Config.answers.getAnswer(NLU_result, answer_key);		//default is "redirect_result_0a"
		api.answerClean = Converters.removeHTML(api.answer);
		
		api.resultInfoPut("cmd", NLU_result.getCommand());
		
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
