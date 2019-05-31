package net.b07z.sepia.server.assist.chats;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.assistant.Assistant;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.Converters;

/**
 * Everyones favorite: the "how are you" question :-)
 * 
 * @author Florian Quirin
 *
 */
public class HowAreYou {

	public static ServiceResult get(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
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
		api.answer = Answers.getAnswerString(nluResult, "chat_how_are_you_0a");
		api.answerClean = Converters.removeHTML(api.answer);
		
		api.status = "success";
		
		//anything else?
		api.context = CMD.CHAT;		//how do we handle chat contexts? Just like that and do the rest with cmd_summary?
		
		//DEBUG
		/*
		if (nluResult.input.isDuplexConnection()){
			System.out.println(nluResult.input.connection);
			System.out.println(nluResult.input.msgId);
			System.out.println(nluResult.input.duplexData);
			api.runInBackground(3000, () -> {
				//initialize follow-up result
				ServiceBuilder service = new ServiceBuilder(nluResult);
				service.answer = Answers.getAnswerString(nluResult, "<direct>Ach was ich noch sagen wollte. Habs vergessen.");
				service.status = "success";
				boolean wasSent = service.sendFollowUpMessage(nluResult.input, service.buildResult());
				return;
			});
		}
		*/
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		//return result_JSON.toJSONString();
		return result;
	}
}
