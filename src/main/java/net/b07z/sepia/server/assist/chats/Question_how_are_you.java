package net.b07z.sepia.server.assist.chats;

import net.b07z.sepia.server.assist.apis.API;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.assistant.Assistant;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.Converters;

/**
 * Everyones favorite: the "how are you" question :-)
 * 
 * @author Florian Quirin
 *
 */
public class Question_how_are_you {

	public static ApiResult get(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//TODO: make it interactive! For now its simple
		api.mood = Assistant.mood_increase(api.mood);
		/*
		int mood = NLU_result.mood;
		if (answer is good){
			mood = Assistant.mood_increase(NLU_result.mood);
		}else if (answer is bad){
			mood = Assistant.mood_decrease(NLU_result.mood);
		}
		*/
		
		//get answer
		api.answer = Config.answers.getAnswer(NLU_result, "chat_how_are_you_0a");
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
