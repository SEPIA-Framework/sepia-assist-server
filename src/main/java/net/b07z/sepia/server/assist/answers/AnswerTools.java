package net.b07z.sepia.server.assist.answers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewInfo;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.RandomGen;

/**
 * Tools to prepare answers.
 * 
 * @author Florian Quirin
 *
 */
public class AnswerTools {
	
	/**
	 * Typically inside the framework answers are pulled from the database using a key like "test_0a" but the user
	 * can define his own answers and answer sets in apis or together with commands. For convenience user defined answers that don't call the database are
	 * tag-less meaning they come as plain text or start with only the tag &#60direct&#62 where as answers that refer to the database are
	 * enclosed within brackets completely. This method handles these command-specific answers and converts them to database-compatible format,
	 * it also handles the selection of random custom direct/database answers out of sets (separated by "||").<br>
	 * Typically this feature is used by passing the answer set in the command parameter "reply=..." like in chats commands.
	 * apis.Open_CustomLink is also making heavy use of this feature for answers and even question sets.
	 *  
	 * @param key_or_answer_set - a set of answer "keys" accessing the database (inside brackets) or plain direct answers
	 * @return internal standard format to put into "get_answer" method
	 */
	public static String handleUserAnswerSets(String key_or_answer_set){
		//choose a random answer if multiple answers are available
		String [] answers = key_or_answer_set.split("\\|\\|");
		String key_or_answer = answers[RandomGen.getInt(answers.length)].trim();		//random element in answer_set

		String result_key = "";
		//if the input comes in brackets <.*> it will be used to call a database link 
		if (key_or_answer.matches("^<.*>$") && !key_or_answer.matches(".+>.+$")){
			result_key = key_or_answer.replaceAll("(^<|>$)", "").trim();
			
		//if its already tagged with <direct> the question key will be replaced with a direct question which is "key" itself
		}else if (key_or_answer.matches("^<direct>.+")){
			result_key = key_or_answer;
			
		//tagged it with <direct> if it does not come inside brackets <.*>
		}else{
			result_key = "<direct>" + key_or_answer;
		}
		
		return result_key;
	}
	
	/**
	 * Replace wildcard parameters with actual values in answer string (if any).
	 * @param answer - answer string with optional wildcard parameters (&#60 1 &#62, &#60 2 &#62 ...).
	 * @param wildcards - values for wildcards, empty array or null
	 * @return replaced answer string
	 */
	public static String replaceWildcards(String answer, Object... wildcards){
		if (wildcards != null && wildcards.length > 0){
			int N = wildcards.length;
			//replace wildcards - TODO: this could be more efficient if we would only check the required wildcards
			for (int i=0; i<N; i++){
				//System.out.println("AnswerLoader: " + answer); 		//debug
				Object wco = wildcards[i];
				if (wco == null){
					Debugger.println("AnswerLoader - missing answerParameter " + (i+1) + " in answer: '" + answer + "'", 1);
					answer = answer.replaceFirst("<" + (i+1) + ">" , "null");
				}else{
					answer = answer.replaceFirst("<" + (i+1) + ">" , wco.toString());
				}
			}
		}
		return answer;
	}
	
	/**
	 * Replace all the user and assistant specific tags that can be obtained from the user account or server configuration like
	 * user and assistant names etc.
	 * @param answer - string to check for the tags
	 * @param info - gathered info on user as NLU_Result (including NLU_Input and input.User) 
	 * @return cleaned string
	 */
	public static String replaceSpecialTags(String answer, NluResult info){
		answer = answer.replaceAll("<user_name>", info.input.user.getName(Config.superuserServiceAccMng));
		answer = answer.replaceAll("<name>", Config.assistantName);
		answer = answer.replaceAll("<local_time_hhmm>", DateTimeConverters.getToday("H:mm", info.input));
		answer = answer.replaceAll("<local_date_ddMMyyyy>", DateTimeConverters.getToday("dd.MM.yyyy", info.input));
		answer = answer.replaceAll("<local_date_MMddyyyy>", DateTimeConverters.getToday("MM/dd/yyyy", info.input));
		return answer;
	}
	
	/**
	 * Clean the HTML answer from things like vocal smileys. Intended to be used AFTER answer_clean has been assigned
	 * and you want to change how the original answer is displayed in the chat.
	 */
	public static String cleanHtmlAnswer(String input){
		String output = input;
		//remove vocal smileys
		output = output.replaceAll("#\\w+\\d\\d#", "").trim();
		return output;
	}
	
	/**
	 * Build wildcard values from answer parameter list (usually taken from {@link InterviewInfo} or {@link ServiceInfo}) 
	 * and resultInfo (usually taken from {@link ServiceBuilder}. 
	 * @param ansParams - List of answer parameters defined by service/interview info
	 * @param resultInfo - key-value pairs holding values to answer parameters
	 * @return
	 */
	public static Object[] buildAnswerWildcardValues(List<String> ansParams, JSONObject resultInfo){
		Object[] aps = new Object[ansParams.size()];
		for (int i=0; i<aps.length; i++){
			aps[i] = resultInfo.get(ansParams.get(i));
		}
		return aps;
	}

	/**
	 * Check if command or input was repeated in a way it would generate the same answer.
	 * @param nluResult - {@link NluResult} of new or ongoing conversation
	 * @return repetition level 0, 1 or 2 to choose optimized answer
	 */
	public static int checkRepetitionLevel(NluResult nluResult){
		String lastCmd = nluResult.input.lastCmd;
		boolean repeatCmd = false;		//do we still answer the same command?
		boolean useContext = false;
		String contextSearch = "";
		if (nluResult.input.inputType.matches("response")){
			//be less strict with responses - match without parameters (and missing input)
			String A = nluResult.cmdSummary.replaceAll("parameter_set=.*?;;", "");
			A = A.replaceFirst(nluResult.input.inputMiss + "=(.*?)(;;|$)", "");
			String B = lastCmd.replaceAll("parameter_set=.*?;;", "");
			B = B.replaceFirst(nluResult.input.inputMiss + "=(.*?)(;;|$)", "");
			repeatCmd = (A.matches(Pattern.quote(B)));
		}else if (nluResult.getCommand().equals(CMD.NO_RESULT)){
			//be a bit less restrictive with no_result (ignore exact text) and have some random fun
			String A = nluResult.cmdSummary.replaceAll("text=.*?;;", "").trim();
			String B = lastCmd.replaceAll("text=.*?;;", "").trim();
			if (A.matches(Pattern.quote(B))){		// && (Math.random() < 0.5f)
				repeatCmd = true;
			}
			useContext = true;
			contextSearch = CMD.NO_RESULT;
		}else{
			//be very strict with the rest
			repeatCmd = (nluResult.cmdSummary.matches(Pattern.quote(lastCmd)));
		}
		int cmdRepeatedN;
		if (useContext){
			cmdRepeatedN = NluTools.countOccurrenceOf(nluResult.input.context, contextSearch);
		}else{
			//it always jumps from 0 to 1 but will only be taken into account with repeatCmd=true
			cmdRepeatedN = nluResult.input.lastCmdN + 1;
		}
		if (cmdRepeatedN > 2) cmdRepeatedN = 2;
		if (repeatCmd){
			return cmdRepeatedN;
		}else{
			return 0;
		}
		//so it is basically 0,false; 1,true; 2,true; 2,true; ...
	}
	
	/**
	 * Get right answer from pool for given key, language, mood and repetition level.
	 * @param answers - A pool of answers for different languages
	 * @param language - current language code
	 * @param key - answer key
	 * @param cmdRepeatN - repetition level (0-2)
	 * @param mood - mood index (0-10)
	 * @return
	 */
	public static String getResultForKey(Map<String, List<Answer>> answers, String language, String key, int cmdRepeatN, int mood){
		//get possible answers from HashMap
		List<Answer> possibleAnswers = answers.get(key);
		//check it
		if (possibleAnswers != null && possibleAnswers.size() > 0){
			List<Answer> exactMatches = new ArrayList<>();		//mood and repetition matches
			List<Answer> almostMatches = new ArrayList<>();	//rep. matches with mood==5
			List<Answer> almostMatches2 = new ArrayList<>();	//mood matches and rep. is zero
			List<Answer> alwaysMatches = new ArrayList<>();	//rep. is zero
			//adjust reference mood - we don't support full scale 10 steps yet but only 3 stages (0-2, 3-6, 7-10) ^^  
			int refMood = 5;
			if (mood == -1)		refMood = 5;
			else if (mood < 3)	refMood = 0;
			else if (mood > 7)	refMood = 10;
			
			int tmpRepeat = 0;
			int tmpMood=5;
			//run through possible answers and find the one that fits best or choose randomly
			for (Answer ans : possibleAnswers){
				
				//TODO: prioritize personal answers?
				//TODO: filter character before?
				
				tmpMood = ans.getMood();
				tmpRepeat = ans.getRepetition();
				//check and add
				if (tmpMood == refMood && tmpRepeat == cmdRepeatN){
					exactMatches.add(ans);
				}else if (tmpMood == 5 && tmpRepeat == cmdRepeatN){
					almostMatches.add(ans);
				}else if (tmpRepeat == 0 && tmpMood == refMood){
					almostMatches2.add(ans);
				}else if (tmpRepeat == 0){
					alwaysMatches.add(ans);
				}
			}
			//select 
			if (exactMatches.size()>0)			possibleAnswers = exactMatches;
			else if (almostMatches.size()>0)	possibleAnswers = almostMatches;		//do we want a wrong mood answer? only if it is a mood==5 one
			else if (almostMatches2.size()>0)	possibleAnswers = almostMatches2;
			else 								possibleAnswers = alwaysMatches;
			
		    int index = RandomGen.getInt(possibleAnswers.size());
		    return possibleAnswers.get(index).getText();
		}else{
			return null;
		}
	}
	
	/**
	 * Create a generic "missing answer" result.
	 * @param key - key of answer that is missing
	 * @param language - current language code
	 * @return
	 */
	public static String missingResult(String key, String language){
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
