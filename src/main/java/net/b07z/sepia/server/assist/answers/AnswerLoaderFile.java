package net.b07z.sepia.server.assist.answers;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.database.DataLoader;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tools.RandomGen;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.tools.Debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
	public String getAnswer(NluResult params, String key) {
		return getAnswer(params, key, (Object[]) null);
	}
	//get answer using NLU_Result and key string
	public String getAnswer(NluResult params, String key, Object... wildcards) {
		if (key == null || key.isEmpty()){
			return "";
		}
		
		String language = params.language;
		
		//ALL
		Map<String, List<Answer>> answers_this = answers.get(language);
		if (answers_this != null && answers_this.size()>0){
			return getAnswer(answers_this, params, key, wildcards);
		
		//NOT SUPPORTED LANGUAGE
		}else{
			return missingResult(key, language);
		}
	}
	//get answer using NLU_Result and key string
	public String getAnswer(Map<String, List<Answer>> memory, NluResult params, String key) {
		return getAnswer(memory, params, key, (Object[]) null);
	}
	//get answer using NLU_Result and key string - complex answer, taking into account the last answer and the mood
	public String getAnswer(Map<String, List<Answer>> memory, NluResult params, String key, Object... wildcards) {
		String language = params.language;
		String last_cmd = params.input.lastCmd;
		int last_cmd_N = params.input.lastCmdN;
		int mood = params.mood;
			
		//check if last and new command are identical to decide if we choose from repeat-modified reply
		boolean cmd_repeat = false;
		boolean useContext = false;
		String contextSearch = "";
		if (params.input.inputType.matches("response")){
			//be less strict with responses
			String A = params.cmdSummary.replaceAll("parameter_set=.*?;;", "");
			A = A.replaceFirst(params.input.inputMiss + "=(.*?)(;;|$)", "");
			String B = last_cmd.replaceAll("parameter_set=.*?;;", "");
			B = B.replaceFirst(params.input.inputMiss + "=(.*?)(;;|$)", "");
			cmd_repeat = (A.matches(Pattern.quote(B)));
		}else if (params.getCommand().equals(CMD.NO_RESULT)){
			//be a bit less restrictive with no_result and have some random fun
			String A = params.cmdSummary.replaceAll("text=.*?;;", "").trim();
			String B = last_cmd.replaceAll("text=.*?;;", "").trim();
			if (A.matches(Pattern.quote(B))){		// && (Math.random() < 0.5f)
				cmd_repeat = true;
			}
			useContext = true;
			contextSearch = CMD.NO_RESULT;
		}else{
			//be very strict with the rest
			cmd_repeat = (params.cmdSummary.matches(Pattern.quote(last_cmd)));
		}
		int cmd_repeat_N;
		if (useContext){
			cmd_repeat_N = NluTools.countOccurrenceOf(params.input.context, contextSearch);
		}else{
			cmd_repeat_N = last_cmd_N+1;			//it always jumps from 0 to 1 but will only be taken into account with cmd_repeat=true
		}
		if (cmd_repeat_N > 2)	cmd_repeat_N = 2;	//so it is basically 1,false; 1,true; 2,true; 2,true; ... 
		
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
		//TODO: there is a slight inconsistency left that was build up to support easy custom answers and answer sets. Use "ANS_Tools.handleUserAnswerSets" to avoid it. 
		String answer = "";
		if (key.trim().matches("^<direct>.+")){
			answer = key.replaceFirst("^<direct>", "").trim();
		}else{
			//answer = getAnswer(language, key, cmd_repeat, cmd_repeat_N, mood);
			answer = getKeyResult(memory, language, key, cmd_repeat, cmd_repeat_N, mood);
		}
		//System.out.println("AnswerLoader - answer: " + answer); 	//debug
		if (wildcards != null && wildcards.length>0){
			int N = wildcards.length;
			//replace wildcards
			for (int i=0; i<N; i++){
				//System.out.println("AnswerLoader: " + answer); 		//debug
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
		answer = AnswerTools.replaceSpecialTags(answer, params);
		
		return answer;
	}
	
	//get key entry from memory - language independent (its just there for logging errors)
	public String getKeyResult(Map<String, List<Answer>> answers, String language, String key, boolean cmd_repeat, int cmd_repeat_N, int mood_i){
		String answer = "";
		
		//get possible answers from HashMap
		List<Answer> possible_answers = answers.get(key);
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
	
	//get key entry from memory - language independent (its just there for logging errors)
	/*
	public String getKeyResult(HashMap<String, ArrayList<String>> answers, String language, String key, boolean cmd_repeat, int cmd_repeat_N, int mood_i){
		String answer = "";
		
		//get possible answers from HashMap
		ArrayList<String> possible_answers = answers.get(key);
		//check it
		if (possible_answers != null && possible_answers.size()>0){
			ArrayList<String> exact_matches = new ArrayList<String>();		//mood and repetition matches
			ArrayList<String> almost_matches = new ArrayList<String>();		//rep. matches with mood==5
			ArrayList<String> almost_matches2 = new ArrayList<String>();	//mood matches and rep. is zero
			ArrayList<String> always_matches = new ArrayList<String>();		//rep. is zero
			//adjust reference mood - we don't support full scale 10 steps yet but only 3 stages (0-2, 3-6, 7-10) ^^  
			String ref_mood="5";
				if (mood_i == -1)		ref_mood = "5";
				else if (mood_i < 3)	ref_mood = "0";
				else if (mood_i > 7)	ref_mood = "10";
			//get reference for repeated commands
			String ref_repeat="0";
			if (cmd_repeat){
				ref_repeat = String.valueOf(cmd_repeat_N);
			}
			//
			String params, tmp_mood="", tmp_repeat="";
			//run through possible answers and find the one that fits best or choose randomly
			for (String s : possible_answers){
				params = s.split(";;")[0].trim();
				if (params.contains("mood=")){	tmp_mood = getParameter(params, "mood");	} else tmp_mood="5";
				if (params.contains("rep=")){	tmp_repeat = getParameter(params, "rep");	} else tmp_repeat="0";
				//check and add
				if (tmp_mood.matches(ref_mood) && tmp_repeat.matches(ref_repeat) ){
					exact_matches.add(s);
				}else if (tmp_mood.matches("5") && tmp_repeat.matches(ref_repeat)){
					almost_matches.add(s);
				}else if (tmp_repeat.matches("(0|)") && tmp_mood.matches(ref_mood)){
					almost_matches2.add(s);
				}else if (tmp_repeat.matches("(0|)")){
					always_matches.add(s);
				}
			}
			//select 
			if (exact_matches.size()>0)			possible_answers = exact_matches;
			else if (almost_matches.size()>0)	possible_answers = almost_matches;		//do we want a wrong mood answer? only if it is a mood==5 one
			else if (almost_matches2.size()>0)	possible_answers = almost_matches2;
			else 								possible_answers = always_matches;
			int N = possible_answers.size();
		    int index = Assistant.getRandomInt(N);
		    answer = possible_answers.get(index);
		    //clean answer
		    answer = answer.trim().replaceFirst(".*?;;","").trim();
		    return answer;
			
		}else{
			return missingResult(key, language);
		}
	}
	*/
	
	//extract parameter from line
	/*
	public String getParameter(String line, String param){
		return (line.replaceFirst(".*"+param+"=(.*?)(\\||$|\\s).*", "$1"));
	}
	*/
	
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
