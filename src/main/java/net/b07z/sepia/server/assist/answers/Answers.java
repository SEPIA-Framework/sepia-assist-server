package net.b07z.sepia.server.assist.answers;

import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * A convenience class to route a getAnswer request to the correct answers pool.
 * 
 * @author Florian Quirin
 *
 */
public class Answers {
	
	/**
	 * Get an answer using all info given by NLU result and answer key. Searches automatically for the right answer pool.
	 * @param nluResult - NLU result that led us here
	 * @param answerKey - answer ID we are looking for
	 * @param wildcards - the answer parameters (variables) to fill the answer with
	 * @return
	 */
	public static String getAnswerString(NluResult nluResult, String answerKey, Object... wildcards){
		String answer;
		if (answerKey.startsWith(ServiceAnswers.ANS_PREFIX)){
			//find the answers defined in a custom service
			String serviceCommand = nluResult.getCommand();
			
			//we might have them in the session cache
			ServiceAnswers answerPool = nluResult.getCachedServiceAnswers(serviceCommand);
			if (answerPool == null){
				//ok we need to load it then
				List<ServiceInterface> possibleServices = ConfigServices.getCustomOrSystemServices(
						nluResult.input, nluResult.input.user, serviceCommand);
				if (possibleServices != null){
					answerPool = possibleServices.get(0).getAnswersPool(nluResult.language);
				}
				//Debugger.println("Answers - needed to reload custom service answers for cmd '" + serviceCommand 
				//		+ "'. Check system for efficiency!", 3); 	//sometimes this cannot happen earlier
			}
			//found them?
			if (answerPool.containsAnswerFor(answerKey)){
				//found answers
				answer = getAnswerString(answerPool.getMap(), nluResult, answerKey, wildcards);
				
			}else{
				Debugger.println("Answers - failed to find custom answers for cmd '" + serviceCommand 
						+ "'. What happened?", 1);
				//take system answers in a last try
				answer = Config.answers.getAnswer(nluResult, answerKey, wildcards);
			}
			
		}else{
			//take system answers
			answer = Config.answers.getAnswer(nluResult, answerKey, wildcards);
		}
		return answer;
	}
	
	/**
	 * Get an answer using all info given by NLU result and answer key. Searches automatically for the right answer pool.
	 * @param nluResult - NLU result that led us here
	 * @param answerKey - answer ID we are looking for
	 * @return
	 */
	public static String getAnswerString(NluResult nluResult, String answerKey){
		return getAnswerString(nluResult, answerKey, (Object[]) null);
	}

	/**
	 * Get an answer using the given answer pool (map).
	 * @param answerPool - a map with custom answers
	 * @param nluResult - NLU result that led us here
	 * @param answerKey - answer ID we are looking for
	 * @param wildcards - the answer parameters (variables) to fill the answer with
	 * @return
	 */
	public static String getAnswerString(Map<String, List<Answer>> answerPool, NluResult nluResult, String answerKey,
			Object... wildcards){
		return Config.answers.getAnswer(answerPool, nluResult, answerKey, wildcards);
	}

}
