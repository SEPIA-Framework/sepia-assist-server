package net.b07z.sepia.server.assist.interpreters;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Helper methods to edit and analyze Strings etc. 
 * 
 * @author Florian Quirin
 *
 */
public class NluTools {
	
	//--conventional help methods---
	
	private final static String BREAK_S = "(?:\\b|\\s|<|>|^)"; 		//TODO: note that "-" is considered as a word ending (\\b)
	private final static String BREAK_E = "(?:\\b|\\s|>|<|$)";		//... and ?: triggers non-capture mode for the bracket
	
	/**
	 * String contains one of these ... <br>
	 * Note: The difference to String.contains is that is has to be a whole token match, e.g. "AB" does not contain "B" in this case. 
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
	 * Find first match.<br>
	 * NOTE: Matches separate tokens, e.g.: "b" in "a b c" but NOT in "abc"!
	 * @param text - string with text to analyze
	 * @param find_first_of - string with terms to find separated by "|" e.g. a|b|c
	 * @return string with first match or empty string for "no match"
	 */
	public static String stringFindFirst(String text, String find_first_of){
		if (text.matches(".*?"+BREAK_S+"(" + find_first_of + ")"+BREAK_E+".*")){
			return text.replaceAll(".*?"+BREAK_S+"(" + find_first_of + ")"+BREAK_E+".*", "$1");			//TODO: it would be better to name the group instead of using $1
		}else{
			return "";
		}
	}
	/**
	 * Find first match.<br>
	 * NOTE: Matches parts of words, e.g.: "b" in "a b c" AND in "abc"!
	 * @param text - string with text to analyze
	 * @param find_first_of - string with terms to find separated by "|" e.g. a|b|c
	 * @return string with first match or empty string for "no match"
	 */
	public static String stringFindFirstPart(String text, String find_first_of){
		if (text.matches(".*?(" + find_first_of + ").*")){
			return text.replaceAll(".*?(" + find_first_of + ").*", "$1");			//TODO: it would be better to name the group instead of using $1
		}else{
			return "";
		}
	}
	/**
	 * Find first match.<br>
	 * NOTE: Matches separate tokens, e.g.: "b" in "a b c" but NOT in "abc"!
	 * @param text - string with text to analyze
	 * @param find_first_of - string with terms to find separated by "|" e.g. a|b|c
	 * @return string with first match or empty string for "no match"
	 */
	public static String stringFindLongest(String text, String find_first_of){
		if (text.matches(".*?"+BREAK_S+"(" + find_first_of + ")"+BREAK_E+".*?")){
			return text.replaceAll(".*?"+BREAK_S+"(" + find_first_of + ")"+BREAK_E+"(.*?).*", "$1"); 	//TODO: it would be better to name the group instead of using $1
		}else{
			return "";
		}
	}
	/**
	 * Find last and minimal match of these. Runs greedy through the string and USUALLY finds a minimal version of the options.<br>
	 * NOTE: Matches separate tokens, e.g.: "b" in "a b c" but NOT in "abc"!
	 * @param text - string with text to analyze
	 * @param find_last_of - string with terms to find separated by "|" e.g. a|b|c
	 * @return string with last match or empty string for "no match"
	 */
	public static String stringFindLastMin(String text, String find_last_of){
		if (text.matches(".*"+BREAK_S+"(" + find_last_of + ")"+BREAK_E+".*")){
			return text.replaceFirst(".*"+BREAK_S+"(" + find_last_of + ")"+BREAK_E+".*", "$1");			//TODO: it would be better to name the group instead of using $1
		}else{
			return "";
		}
	}
	/**
	 * Remove first match.<br>
	 * NOTE 1: Matches separate tokens, e.g.: "b" in "a b c" but NOT in "abc"!<br>
	 * NOTE 2: Since its regExp you might need to make wise use of {@link Pattern#quote}.
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
	 * Get a new string that starts at the given string (last match) and includes the next N words (or less).
	 * Splits tokens at \\s+ to count words so you probably need to normalize first.
	 * @param input - whole string
	 * @param start - start word/token of new string
	 * @param n - including this many words (or less)
	 * @return new string
	 */
	public static String getStringAndNextWords(String input, String start, int n){
		String section = input.replaceFirst(".*\\b(" + start + ")", "$1").trim();
		String[] words = section.split("\\s+");
		String[] relevantWords = Arrays.copyOfRange(words, 0, Math.min(n+1, words.length));
		return String.join(" ", relevantWords);
	}
	
	/**
	 * Capitalize first letter of all words in a sentence.
	 * @param low - input with lower-case sentence
	 * @return sentence where ALL words begin with a capital letter
	 */
	public static String capitalizeAllFirstLetters(String low){
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
