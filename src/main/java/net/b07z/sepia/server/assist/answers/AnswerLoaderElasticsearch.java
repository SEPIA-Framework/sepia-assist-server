package net.b07z.sepia.server.assist.answers;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.Answer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an implementation of the answer loader interface ({@link AnswerLoader}) loading answers 
 * stored in the Elasticsearch DB. The answers can be pre-loaded during server start-up to a HashMap which is 
 * done by the {@link Config} using the DataLoader, in case there are no pre-loaded answers it falls back 
 * to live DB loading.
 * 
 * @author Florian Quirin
 *
 */
public class AnswerLoaderElasticsearch implements AnswerLoader {
	
	//stores the references to all answers in different languages
	Map<String, Map<String, List<Answer>>> answers = new HashMap<>();
	
	//preload answers from database - TODO: this was introduced in the file-loader, but will basically be 
	//empty here. We could start to buffer answers, but then we loose the fast-search abilities of the DB
	public void setupAnswers() { 	
		answers.clear();
	}
	
	//Set or update the pool of possible answers.
	public void updateAnswerPool(Map<String, Map<String, List<Answer>>> answers_pool){
		this.answers = answers_pool;
	}
	
	//get answer using NluResult and key string
	public String getAnswer(NluResult nlu_res, String key) {
		return getAnswer(nlu_res, key, (Object[]) null);
	}
	//get answer using NluResult and key string
	public String getAnswer(NluResult nluResult, String key, Object... wildcards) {
		if (key == null || key.isEmpty()){
			return "";
		}
		String language = nluResult.language;
		Map<String, List<Answer>> memory = new HashMap<>();
		if (answers != null && !answers.isEmpty()){
			memory = answers.get(language);
		}
		return getAnswer(memory, nluResult, key, wildcards); 		//null or empty triggers auto-load here
	}
	//get answer using NluResult and key string
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key) {
		return getAnswer(memory, nluResult, key, (Object[]) null);
	}
	//get answer using NluResult and key string - complex answer, taking into account the last answer and the mood
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key, Object... wildcards) {
		String language = nluResult.language;
		
		//check if last and new command are identical to decide if we choose from repeat-modified reply
		int cmdRepeatN = AnswerTools.checkRepetitionLevel(nluResult);
		
		//debug
		/*
		System.out.println("----");
		System.out.println("cmd sum.: " + params.cmd_summary);
		System.out.println("last cmd: " + last_cmd);
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
			//check if we need to load answer
			if (memory == null || memory.isEmpty()){
				//-----------get possible answers from database------------
				User user = nluResult.input.user;
				Map<String, Object> filters = new HashMap<>();
				filters.put("language", language);
				//filters.put("includeSystem", true);
				List<Answer> possible_answers = user.getUserDataAccess().getPersonalAndSystemAnswersByType(user, key, filters);
				memory = new HashMap<>();
				//assume they have all the correct type (key)
				memory.put(key, (ArrayList<Answer>) possible_answers); 			//TODO: we got to replace all ArrayList with List!
				//---------------------------------------------------------
			}
			int mood = nluResult.mood;
			answer = AnswerTools.getResultForKey(memory, language, key, cmdRepeatN, mood);
			//missing?
			if (answer == null){
				answer = AnswerTools.missingResult(key, language);
			}
		}
		//replace wildcards
		answer = AnswerTools.replaceWildcards(answer, wildcards);
				
		//replace user- and assistant specific tags in answers
		answer = AnswerTools.replaceSpecialTags(answer, nluResult);
		
		return answer;
	}
	
}
