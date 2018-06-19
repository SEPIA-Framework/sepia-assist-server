package net.b07z.sepia.server.assist.tts;

import java.util.regex.Matcher;

/**
 * Tools to edit text, pronunciation, etc. ..
 * 
 * @author Florian Quirin
 *
 */
public class TtsTools {
	
	/**
	 * Gets mood according to text in sentence. Checks for example for smileys in the text.
	 * 
	 * @param text - input text that will be spoken
	 * @param current_mood_index - index currently set (default is 0)
	 * @return mood index 0-neutral, 1-happy, 2-sad, (...more to come)
	 */
	public static int getMoodIndex(String text, int current_mood_index){
		int mood_index = current_mood_index;
		//overwrite current
		if (text.matches(".*(:\\-\\)).*")){
			mood_index=1;
		}else if (text.matches(".*(:\\-\\().*")){
			mood_index=2;
		}else if (text.matches(".*(:\\-\\|).*")){
			mood_index=0;
		}
		return mood_index;
	}
	
	/**
	 * Trim text before sending to TTS engine. Includes things like removing smileys etc.
	 * 
	 * @param text - text to trim
	 * @return trimmed/cleaned text
	 */
	public  static String trimText(String text){
		//remove smileys
		text=text.trim().replaceAll("(:\\-\\)|;\\-\\)|:\\-\\(|:\\-\\|)", "");
		text=text.trim();
		return text;
	}
	
	/**
	 * Optimize pronunciation for certain words and conditions.
	 * 
	 * @param input - input text
	 * @param language - input language
	 * @return text with optimized pronunciation
	 */
	public static String optimizePronunciation(String input, String language){
		if (language.matches("en")){
			if (input.matches(".*\\d{4}\\.\\d{1,2}\\.\\d{1,2}.*") || input.matches(".*\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}+.*")){
				input = input.replaceAll("(\\d)\\.(\\d)(\\d){0,1}\\.", "$1/$2$3/");
			}
			input = input.replaceAll("\\b(rewe|Rewe|REWE)\\b", "Re-We");
			
		}else if (language.matches("de")){
			input = input.replaceAll("(\\d)\\.(\\d)", "$1,$2");
			input = input.replaceAll("\\b(moin|Moin)\\b", Matcher.quoteReplacement("\\prn=m OY n\\"));
			input = input.replaceAll("\\b(die ersten 2)\\b", "die ersten zwei");
			input = input.replaceAll("\\b(die ersten 3)\\b", "die ersten drei");
			input = input.replaceAll("\\b(sin |Sin )\\b", "ßinn "); 					//very funny: Sin City !!!
			input = input.replaceAll("\\b(rewe|Rewe|REWE)\\b", Matcher.quoteReplacement("\\prn=R e:1 v @\\"));
		}
		return input;
	}

}
