package net.b07z.sepia.server.assist.data;

import java.util.HashMap;

/**
 * A word is so much more than a series of characters ^^.
 * Use this class to get all the stored meta info about a word like type (noun, verb, ...),
 * info (place, name, ...), most used commands, ...
 * 
 * @author Florian Quirin
 *
 */
public class Word {
	
	String word = "";						//characters of this word
	String generalizedName = "";			//a general name for this word (singular, no konjugation, no declination), e.g. "nicely" -> "nice", "houses" -> "house"
	HashMap<String, Double> type;					//noun, verb, adjective, ...
	HashMap<String, Double> info;					//place, name, ... whatever makes sense
	HashMap<String, Double> tags;					//used as replacement for these tags/parameters
	HashMap<String, Double> similarWords;			//a list of similar words and their relative similarity like "search, find, look for" ...
	HashMap<String, Double> mostUsedCommands;		//command with relative occurrence
	double importance = 0.0d;						//how important is this word for commands? 1/(Occurrence in commands)
	
	
	//-----useful methods-----
	
	/**
	 * Do words a and b match? Currently if word b is a tag then it matches everything
	 * @param a - first string, expected to have no tag
	 * @param b - second string, can be a tag
	 * @return true/false
	 */
	public static boolean matches(String a, String b){
		//TODO: can be extended later to check e.g. a tag like <place> against a word like "Berlin", or "find" vs "search", or "houses" vs "house"
		if (hasTag(b)){
			//TODO: proper check, e.g. tags.contains(getTag(b))
			return true;
		}
		return a.equals(b);
	}
	
	/**
	 * Has this word a tag inside?
	 * @param word - string to check
	 * @return true/false
	 */
	public static boolean hasTag(String word){
		return word.matches(".*<\\w+>.*");
	}
	/**
	 * Get tag in word without brackets.
	 * @param word - string to check
	 * @return tag string (without brackets) or empty
	 */
	public static String getTag(String word){
		if (hasTag(word)){
			return word.replaceFirst(".*?<", "").replaceFirst(">.*", "").trim();
		}else{
			return "";
		}
	}
}
