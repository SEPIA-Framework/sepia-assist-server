package net.b07z.sepia.server.assist.chats;

import net.b07z.sepia.server.assist.apis.API;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.Converters;

/**
 * Ask for languages and change if required.
 * 
 * @author Florian Quirin
 *
 */
public class Question_languages {
	
	public static ApiResult get(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//TODO: add action: change language - if required change language before loading answer
				
		//get answer
		api.answer = Config.answers.getAnswer(NLU_result, "chat_languages_0a");
		api.answerClean = Converters.removeHTML(api.answer);
		
		api.status = "success";
		
		//anything else?
		api.context = CMD.CHAT;		//how do we handle chat contexts? Just like that and do the rest with cmd_summary?
		
		//finally build the API_Result
		ApiResult result = api.buildApiResult();
		
		//return result_JSON.toJSONString();
		return result;
	}

}
