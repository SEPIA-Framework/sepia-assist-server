package net.b07z.sepia.server.assist.answers;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.tools.RandomGen;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.tools.Debugger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This is an implementation of the answers loader interfaces (ANS_Loader_Interface) loading answers 
 * stored on the server. The answers can be pre-loaded during server start-up to a HashMap which is 
 * done by the Config class using the DataLoader, in case there are no pre-loaded answers it falls back 
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
	public String getAnswer(NluResult nlu_res, String key, Object... wildcards) {
		if (key == null || key.isEmpty()){
			return "";
		}
		String language = nlu_res.language;
		Map<String, List<Answer>> memory = new HashMap<>();
		if (answers != null && !answers.isEmpty()){
			memory = answers.get(language);
		}
		return getAnswer(memory, nlu_res, key, wildcards); 		//null or empty triggers auto-load here
	}
	//get answer using NluResult and key string
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nlu_res, String key) {
		return getAnswer(memory, nlu_res, key, (Object[]) null);
	}
	//get answer using NluResult and key string - complex answer, taking into account the last answer and the mood
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nlu_res, String key, Object... wildcards) {
		String language = nlu_res.language;
		String last_cmd = nlu_res.input.lastCmd;
		int last_cmd_N = nlu_res.input.lastCmdN;
		int mood = nlu_res.mood;
			
		//check if last and new command are identical to decide if we choose from repeat-modified reply
		boolean cmd_repeat = false;
		boolean useContext = false;
		String contextSearch = "";
		if (nlu_res.input.inputType.matches("response")){
			//be less strict with responses
			String A = nlu_res.cmdSummary.replaceAll("parameter_set=.*?;;", "");
			A = A.replaceFirst(nlu_res.input.inputMiss + "=(.*?)(;;|$)", "");
			String B = Pattern.quote(last_cmd).replaceAll("parameter_set=.*?;;", "");
			B = B.replaceFirst(nlu_res.input.inputMiss + "=(.*?)(;;|$)", "");
			cmd_repeat = (A.matches(B));
		}else if (nlu_res.getCommand().equals(CMD.NO_RESULT)){
			//be a bit less restrictive with no_result ... and have some random fun
			String A = nlu_res.cmdSummary.replaceAll("text=.*?;;", "").trim();
			String B = Pattern.quote(last_cmd).replaceAll("text=.*?;;", "").trim();
			if (A.matches(B)){		//&& (Math.random() < 0.5f)
				cmd_repeat = true;
			}
			useContext = true;
			contextSearch = CMD.NO_RESULT;
		}else{
			//be very strict with the rest
			cmd_repeat = (nlu_res.cmdSummary.matches(Pattern.quote(last_cmd)));
		}
		int cmd_repeat_N;
		if (useContext){
			cmd_repeat_N = NluTools.countOccurrenceOf(nlu_res.input.context, contextSearch);
		}else{
			cmd_repeat_N = last_cmd_N+1;			//it always jumps from 0 to 1 but will only be taken into account with cmd_repeat=true
		}
		if (cmd_repeat_N > 2)	cmd_repeat_N = 2;	//so it is basically 1,false; 1,true; 2,true; 2,true; ... 
		
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
				User user = nlu_res.input.user;
				Map<String, Object> filters = new HashMap<>();
				filters.put("language", language);
				//filters.put("includeSystem", true);
				List<Answer> possible_answers = user.getUserDataAccess().getPersonalAndSystemAnswersByType(user, key, filters);
				memory = new HashMap<>();
				//assume they have all the correct type (key)
				memory.put(key, (ArrayList<Answer>) possible_answers); 			//TODO: we got to replace all ArrayList with List!
				//---------------------------------------------------------
			}
			answer = getKeyResult(memory, language, key, cmd_repeat, cmd_repeat_N, mood);
		}
		if (wildcards != null && wildcards.length>0){
			int N = wildcards.length;
			//replace wildcards - TODO: this could be more efficient if we would only check the required wildcards
			for (int i=0; i<N; i++){
				Object wco = wildcards[i];
				if (wco == null){
					Debugger.println("AnswerLoader - missing answerParameter in answer: '" + answer + "'", 1);
					answer = answer.replaceFirst("<" + (i+1) + ">" , "null");
				}else{
					answer = answer.replaceFirst("<" + (i+1) + ">" , wco.toString());
				}
			}
		}
		
		//replace user- and assistant specific tags in answers
		answer = AnswerTools.replaceSpecialTags(answer, nlu_res);
		
		return answer;
	}
	
	//get key entry from memory - language independent (its just there for logging errors)
	public String getKeyResult(Map<String, List<Answer>> memory, String language, String key, boolean cmd_repeat, int cmd_repeat_N, int mood_i){
		String answer = "";
		
		//get answer for key
		List<Answer> possible_answers = memory.get(key);
		
		//check it
		if (possible_answers != null && possible_answers.size()>0){
			List<Answer> exact_matches = new ArrayList<>();		//mood and repetition matches
			List<Answer> almost_matches = new ArrayList<>();		//rep. matches with mood==5
			List<Answer> almost_matches2 = new ArrayList<>();	//mood matches and rep. is zero
			List<Answer> always_matches = new ArrayList<>();		//rep. is zero
			//adjust reference mood - we don't support full scale 10 steps yet but only 3 stages (0-2, 3-6, 7-10) ^^  
			int ref_mood=5;
				if (mood_i == -1)		ref_mood = 5;
				else if (mood_i < 3)	ref_mood = 0;
				else if (mood_i > 7)	ref_mood = 10;
			//get reference for repeated commands
			int ref_repeat=0;
			if (cmd_repeat){
				ref_repeat = cmd_repeat_N;
			}
			//
			int tmp_repeat = 0;
			int tmp_mood=5;
			//run through possible answers and find the one that fits best or choose randomly
			for (Answer ans : possible_answers){
				
				//TODO: prioritize personal answers?
				//TODO: filter character before?
				
				tmp_mood = ans.getMood();
				tmp_repeat = ans.getRepetition();
				//check and add
				if (tmp_mood == ref_mood && tmp_repeat == ref_repeat){
					exact_matches.add(ans);
				}else if (tmp_mood == 5 && tmp_repeat == ref_repeat){
					almost_matches.add(ans);
				}else if (tmp_repeat == 0 && tmp_mood == ref_mood){
					almost_matches2.add(ans);
				}else if (tmp_repeat == 0){
					always_matches.add(ans);
				}
			}
			//select 
			if (exact_matches.size()>0)			possible_answers = exact_matches;
			else if (almost_matches.size()>0)	possible_answers = almost_matches;		//do we want a wrong mood answer? only if it is a mood==5 one
			else if (almost_matches2.size()>0)	possible_answers = almost_matches2;
			else 								possible_answers = always_matches;
			int N = possible_answers.size();
		    int index = RandomGen.getInt(N);
		    answer = possible_answers.get(index).getText();
		    return answer;
			
		}else{
			return missingResult(key, language);
		}
	}
	
	//result is missing
	public String missingResult(String key, String language){
		String report =	"MISSING ANSWER OR TRANSLATION - KEY: " + key + ", LANG/FILE: " + language;
		Debugger.println(report, 1);		//ERROR
		
		//German error message to client
		if (language.matches(LANGUAGES.DE)){
			return "Ups, sieht so aus als wäre mir die Antwort darauf irgendwie verloren gegangen. Ich werd sie suchen, versprochen!";
		//English
		}else if (language.matches(LANGUAGES.EN)){
			return "Uups, it seems I've lost the answer to that somewhere. I'm going to look for it as soon as I can!";
		//and unsupported ...
		}else{
			return "S.O.S. a lost answer!";
		}
	}
	
}
