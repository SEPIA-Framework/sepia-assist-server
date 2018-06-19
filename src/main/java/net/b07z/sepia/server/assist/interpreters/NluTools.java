package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Helper methods to edit and analyze Strings etc. 
 * 
 * @author Florian Quirin
 *
 */
public class NluTools {
	
	/**
	 * Transforms a cmd_summary back to a NLU_Result adding the NLU_input and using it to restore the default
	 * values for context, mood, environment etc.
	 * @param input - NLU_Input with all the settings (language, environment, mood, etc...)
	 * @param cmd_sum - cmd_summary to be transformed back
	 * @return NLU_Result
	 */
	public static NluResult cmdSummaryToResult(NluInput input, String cmd_sum){
		NluResult result = cmdSummaryToResult(cmd_sum);
		result.setInput(input);
		return result;
	}
	/**
	 * Transforms a cmd_summary back to a NLU_Result. <br>Note: If you don't supply an NLU_input you have to add it to 
	 * the result later with result.set_input(NLU_Input input) or by adding all important stuff manually!
	 * @param cmd_sum - cmd_summary to be transformed back
	 * @return NLU_Result
	 */
	public static NluResult cmdSummaryToResult(String cmd_sum){
		//initialize
		List<String> possibleCMDs = new ArrayList<>();
		List<Map<String, String>> possibleParameters = new ArrayList<>();
		List<Integer> possibleScore = new ArrayList<>();
		int bestScoreIndex = 0;
		
		//split string
		String cmd;
		String params;
		if (cmd_sum.trim().matches(".*?;;.+")){
			String[] parts = cmd_sum.split(";;",2);			//TODO: change this whole ";;" structure to JSON?
			cmd = parts[0].trim();
			params = parts[1].trim();
		}else{
			cmd = cmd_sum.replaceFirst(";;$", "").trim();
			params = "";
		}
		
		//construct result
		possibleCMDs.add(cmd);
		possibleScore.add(1);
		Map<String, String> kv = new HashMap<>();
		for (String p : params.split(";;")){				//TODO: change this whole ";;" structure to JSON?
			String[] e = p.split("=",2);
			if (e.length == 2){
				String key = e[0].trim();
				String value = e[1].trim();
				kv.put(key, value);
			}
		}
		possibleParameters.add(kv);
		NluResult result = new NluResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex);
		result.certaintyLvl = 1.0d;
		
		//TODO: missing:	input parameters parsed to result (language, context, environment, etc. ...)
		
		return result;
	}
	
	//--conventional help methods---
	
	static String BREAK_S = "(\\b|\\s|<|>|^)"; 		//TODO: note that "-" is considered as a word ending (\\b)
	static String BREAK_E = "(\\b|\\s|>|<|$)";
	
	/**
	 * String contains one of these ...
	 * @param text - string with text to analyze
	 * @param match - string with matches separated by "|" e.g. a|b|c
	 * @return true/false
	 */
	public static boolean stringContains(String text, String match){
		if (text.matches(".*"+BREAK_S+"(" + match + ")"+BREAK_E+".*")){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * Same as stringContains(...) but with an array of words to match and probably much slower, so please don't use it so often ^^.
	 */
	public static boolean stringContainsOneOf(String text, String... matches){
		if (matches != null && matches.length > 0){
			for (String m : matches){
				if (!m.isEmpty() && stringContains(text, m)){
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * Find first match of these ...
	 * @param text - string with text to analyze
	 * @param find_first_of - string with terms to find separated by "|" e.g. a|b|c
	 * @return string with first match or empty string for "no match"
	 */
	public static String stringFindFirst(String text, String find_first_of){
		if (text.matches(".*?"+BREAK_S+"(" + find_first_of + ")"+BREAK_E+".*")){
			return text.replaceAll(".*?"+BREAK_S+"(" + find_first_of + ")"+BREAK_E+".*", "$2");
		}else{
			return "";
		}
	}
	/**
	 * Find last and minimal match of these. Runs greedy through the string and USUALLY finds a minimal version of the options.
	 * @param text - string with text to analyze
	 * @param find_last_of - string with terms to find separated by "|" e.g. a|b|c
	 * @return string with last match or empty string for "no match"
	 */
	public static String stringFindLastMin(String text, String find_last_of){
		if (text.matches(".*"+BREAK_S+"(" + find_last_of + ")"+BREAK_E+".*")){
			return text.replaceFirst(".*"+BREAK_S+"(" + find_last_of + ")"+BREAK_E+".*", "$2");
		}else{
			return "";
		}
	}
	/**
	 * Remove first match of these ...
	 * @param text - string with text to analyze
	 * @param remove_first_of - string with terms to remove the first match, separated by "|" e.g. a|b|c
	 * @return string with removed first match
	 */
	public static String stringRemoveFirst(String text, String remove_first_of){
		return text.replaceFirst(BREAK_S+"(" + remove_first_of + ")"+BREAK_E, " ").replaceAll("\\s+", " ").trim();
	}
	
	/**
	 * Counts the occurrence of a certain string inside another string. The string to count will be quoted to avoid regex matching.
	 * @param base - base string to search in 
	 * @param count_string - string to search for
	 * @return Integer with occurrence
	 */
	public static int countOccurrenceOf(String base, String count_string){
		int count = base.length() - base.replaceAll(Pattern.quote(count_string), "").length();
		count = count / count_string.length();
		return count;
	}
	
	/**
	 * Return number of words by splitting at spaces (\\s+).
	 */
	public static int countWords(String text){
		return text.split("\\s+").length;
	}
	
	/**
	 * Capitalize first letter of all words in a sentence.
	 * @param low - input with low case sentence
	 * @return sentence with every first letter of a word capital
	 */
	public static String capitalizeAll(String low){
		StringBuffer res = new StringBuffer();
	    String[] strArr = low.split("\\s+");
	    for (String str : strArr) {
	        char[] stringArray = str.trim().toCharArray();
	        stringArray[0] = Character.toUpperCase(stringArray[0]);
	        str = new String(stringArray);

	        res.append(str).append(" ");
	    }
	    return res.toString().trim();
	}
	
	/**
	 * Check if the short text is an abbreviation of the long text by comparing it to the first letters of the words in the long string. 
	 * @param i_short - assumed abbreviation 
	 * @param i_long - assumed long version of abbreviation
	 * @return true/false
	 */
	public static boolean isAbbreviation(String i_short, String i_long){
		i_short = i_short.replaceAll("-|\\.|_", "").toLowerCase();
		String[] words = i_long.replaceAll("-|_", " ").replaceAll(";|\\.|,|'|\\?|!", "").split("\\b+");
		String initials = "";
		for (String w : words){
			initials += w.substring(0, 1).trim();
		}
		initials = initials.trim().toLowerCase();
		if (i_short.matches(Pattern.quote(initials))){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Split text into sentences and return an array of those.
	 * @param text - input text with multiple sentences.
	 * @return array of sentences
	 */
	public static String[] splitSentence(String text){
		String[] sentences = text.split("(?<!\\s\\d{1,3})(?<![A-Z])(?<!Dr)(?<!Mr)(?<!Inc)(?<!vs)\\.( |$)");
		return sentences;
	}
	
	/**
	 * Check if the order of words is as expected. Checks for first pattern match.
	 * @param input - text to check - e.g. "the angry cat bites the scared dog"
	 * @param A - 1st string to check, e.g. "cat"
	 * @param B - 2nd string to check, e.g. "dog"
	 * @return true if there is a pattern where A comes before B, false if order is wrong or words don't exist at all
	 */
	public static boolean checkOrder(String input, String A, String B){
		if (input.matches(".*?(\\s|^)(" + Pattern.quote(A.trim()) + ")(\\s.*?|)\\s("+ Pattern.quote(B.trim()) + ")(\\s|$).*")){
			return true;
		}else{
			return false;
		}
	}
	
}
