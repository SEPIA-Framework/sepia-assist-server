package net.b07z.sepia.server.assist.answers;

import net.b07z.sepia.server.assist.database.DataLoader;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.data.Answer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an implementation of the answer loader interface ({@link AnswerLoader}) loading answers 
 * stored on the server. The answers will be pre-loaded during server start-up to a HashMap
 * which is done by the {@link Config} using the DataLoader.
 * 
 * @author Florian Quirin
 *
 */
public class AnswerLoaderFile implements AnswerLoader {
	
	//stores the references to all answers in different languages
	Map<String, Map<String, List<Answer>>> answers = new HashMap<>();
	
	//preload answers from local txt files
	public void setupAnswers() {
		DataLoader dl = new DataLoader();
		answers = dl.loadAnswersFromFilebase(Config.answersPath + "answers"); 		
	}
	
	//Set or update the pool of possible answers.
	public void updateAnswerPool(Map<String, Map<String, List<Answer>>> answers_pool){
		this.answers = answers_pool;
	}
	
	//get answer using NLU_Result and key string
	public String getAnswer(NluResult nluResult, String key) {
		return getAnswer(nluResult, key, (Object[]) null);
	}
	//get answer using NLU_Result and key string
	public String getAnswer(NluResult nluResult, String key, Object... wildcards) {
		if (key == null || key.isEmpty()){
			return "";
		}
		//ALL
		Map<String, List<Answer>> answers_this = answers.get(nluResult.language);
		if (answers_this != null && answers_this.size()>0){
			return getAnswer(answers_this, nluResult, key, wildcards);
		
		//NOT SUPPORTED LANGUAGE
		}else{
			return AnswerTools.missingResult(key, nluResult.language);
		}
	}
	//get answer using NLU_Result and key string
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key) {
		return getAnswer(memory, nluResult, key, (Object[]) null);
	}
	//get answer using NLU_Result and key string - complex answer, taking into account the last answer and the mood
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key, Object... wildcards) {
		String language = nluResult.language;
			
		//check if last and new command are identical to decide if we choose from repeat-modified reply
		int cmdRepeatN = AnswerTools.checkRepetitionLevel(nluResult);
				
		//debug
		/*
		System.out.println("----");
		System.out.println("cmd sum.: " + params.cmd_summary);
		System.out.println("cmd sum. (cleaned): " + params.cmd_summary.replaceAll("text=.*?;;", "").trim());
		System.out.println("last cmd: " + last_cmd);
		System.out.println("last cmd (cleaned): " + last_cmd.replaceAll("text=.*?;;", "").trim());
		System.out.println("last_cmd_N: " + last_cmd_N);
		System.out.println("repeat: " + cmd_repeat);
		System.out.println("repetition: " + cmd_repeat_N);
		System.out.println("----");
		*/
			
		//support for direct answers - no search in database just directly the given answer
		//TODO: there is a slight inconsistency left that was build up to support easy custom answers and answer sets. Use "AnswerTools.handleUserAnswerSets" to avoid it. 
		String answer = "";
		if (key.trim().matches("^<direct>.+")){
			answer = key.replaceFirst("^<direct>", "").trim();
		}else{
			int mood = nluResult.mood;
			answer = AnswerTools.getResultForKey(memory, language, key, cmdRepeatN, mood);
			//missing?
			if (answer == null){
				answer = AnswerTools.missingResult(key, language);
			}
		}
		//System.out.println("AnswerLoader - answer: " + answer); 	//debug
		answer = AnswerTools.replaceWildcards(answer, wildcards);
				
		//replace user- and assistant specific tags in answers
		answer = AnswerTools.replaceSpecialTags(answer, nluResult);
		
		return answer;
	}
		
	//extract parameter from line
	/*
	public String getParameter(String line, String param){
		return (line.replaceFirst(".*"+param+"=(.*?)(\\||$|\\s).*", "$1"));
	}
	*/

}
