package net.b07z.sepia.server.assist.tts;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.core.tools.Is;

/**
 * Tools to edit text, pronunciation, etc. ..
 * 
 * @author Florian Quirin
 *
 */
public class TtsTools {
	
	/**
	 * TTS engines available.
	 */
	public static enum EngineType {
		undefined,
		espeak,
		espeak_mbrola,
		txt2pho_mbrola,
		pico,
		marytts,
		flite,
		acapela
	}
	/**
	 * TTS voice gender types.
	 */
	public static enum GenderCode {
		male,
		female,
		diverse,
		robot
	}

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
		if (text.matches(".*(^|\\s)((:|;)(-|)(\\)|\\])|\\^_\\^|😂|😃|😁|😆|😉|😊)(\\?|!|,|\\.|)($|\\s).*")){
			mood_index=1;
		}else if (text.matches(".*(^|\\s)((:|;)(-|)(\\(|\\[)|😣|😞)(\\?|!|,|\\.|)($|\\s).*")){
			mood_index=2;
		}else if (text.matches(".*(^|\\s)((:|;)(-|)(\\|)|\\-_\\-)(\\?|!|,|\\.|)($|\\s).*")){
			mood_index=0;
		}
		return mood_index;
	}
	
	/**
	 * Trim text before sending to TTS engine. Includes things like removing emojis etc.
	 * 
	 * @param text - text to trim
	 * @return trimmed/cleaned text
	 */
	public  static String trimText(String text){
		//remove emojis
		text = text.trim()
			.replaceAll("(^|\\s)(((:|;)(-|)(\\)|\\(|\\||\\]|\\[)+)(?<sym>\\?|!|,|\\.|)($|\\s))+", "${sym} ")
			.replaceAll("(?i)(^|\\s)((\\^_\\^|\\-_\\-|o_o)(?<sym>\\?|!|,|\\.|)($|\\s))+", "${sym} ")
		;
		return text.replaceAll("\\s+", " ").trim();
	}
	
	/**
	 * Optimize pronunciation for certain words and conditions.
	 * 
	 * @param input - input text
	 * @param language - input language
	 * @param engine - e.g. "marytts", "espeak" or null. See {@link EngineType}.
	 * @return text with optimized pronunciation
	 */
	public static String optimizePronunciation(String input, String language, String engine){
		//common
		input = input.replaceAll("(?i)\\b(SEPIA)\\b", "Sepia").trim();
		if (!input.matches(".*(!|\\?|\\.)$")){
			//make sure it ends with something
			input = input + ".";
		}
		input = input.replaceAll("\\[|\\]", " ").trim();
		
		//emojis
		input = input.replaceAll("😂|😃|😁|😆|😉|😊", " ");	//happy
		input = input.replaceAll("😣|😞", " ");			//sad
		
		//specific
		if (language.equals(LANGUAGES.DE)){
			//time and date
			input = input.replaceAll("(?i)\\b(?<hour>\\d{1,2}):(?<min>\\d\\d) Uhr\\b", "${hour} Uhr ${min}");
			//units
			input = input.replaceAll("(?i)(°C)\\b", " Grad Celsius");
			input = input.replaceAll("(?i)(°F)\\b", " Grad Fahrenheit");
			input = input.replaceAll("%", " Prozent ");			//Note: does this prevent variable expansion in Windows as well?
			//input = input.replaceAll("(?:^|\\s)\\$(\\d+)", " $1 Dollar ");
			input = input.replaceAll("(?:^|\\s)\\$(\\d+(\\.|,)\\d+|\\d+)(\\b)", " $1 Dollar ");
			input = input.replaceAll("(?:^|\\s)€(\\d+(\\.|,)\\d+|\\d+)(\\b)", " $1 Euro ");
			input = input.replaceAll("€(\\d+(\\.|,)\\d+|\\d+)", "$1 Euro");
			input = input.replaceAll("\\$", " Dollar ");		//Note: does this prevent variable expansion in Linux as well?
			input = input.replaceAll("€", " Euro ");
			
			//numbers
			input = input.replaceAll("(^|\\s)(\\d+)\\.(\\d+)(\\s|\\.$|\\.\\s)", "$1$2,$3$4");
			input = input.replaceAll("(^|\\s)(1/2)\\b", "$1ein halb");
			input = input.replaceAll("(^|\\s)(1/3)\\b", "$1ein drittel");
			input = input.replaceAll("(^|\\s)(1/4)\\b", "$1ein viertel");
			input = input.replaceAll("(^|\\s)(2/3)\\b", "$1zwei drittel");
			input = input.replaceAll("(^|\\s)(3/4)\\b", "$1drei viertel");
			
		}else if (language.equals(LANGUAGES.EN)){
			//time and date
			input = input.replaceAll("(?i)\\b(?<hour>\\d{1,2})(:|.)(?<min>\\d\\d)( |)(?<indi>o.clock|am|pm|a\\.m\\.|p\\.m\\.)(?<end>\\s|\\.|$)", "${hour} ${min} ${indi}${end}");
			//units
			input = input.replaceAll("(?i)(°C)\\b", " degrees Celsius");
			input = input.replaceAll("(?i)(°F)\\b", " degrees Fahrenheit");
			input = input.replaceAll("%", " percent ");			//Note: does this prevent variable expansion in Windows as well?
			input = input.replaceAll("(?:^|\\s)\\$(\\d+(\\.|,)\\d+|\\d+)(\\b)", " $1 dollar ");
			input = input.replaceAll("(?:^|\\s)€(\\d+(\\.|,)\\d+|\\d+)(\\b)", " $1 euro ");
			input = input.replaceAll("\\$", " dollar ");		//Note: does this prevent variable expansion in Linux as well?
			input = input.replaceAll("€", " euro ");
			
			//numbers
			input = input.replaceAll("(^|\\s)(\\d+),(\\d+)(\\s|\\.$|\\.\\s)", "$1$2.$3$4");
			input = input.replaceAll("(^|\\s)(1/2)\\b", "$1one half");
			input = input.replaceAll("(^|\\s)(1/3)\\b", "$1one third");
			input = input.replaceAll("(^|\\s)(1/4)\\b", "$1one quater");
			input = input.replaceAll("(^|\\s)(2/3)\\b", "$1two thirds");
			input = input.replaceAll("(^|\\s)(3/4)\\b", "$1three quaters");
			
			//other
			if (Is.typeEqual(engine, EngineType.marytts)){
				//time
				input = input.replaceAll("\\b(\\d\\d|\\d)\\.(\\d\\d|\\d)\\.(\\d\\d\\d\\d|\\d\\d)\\b", "$2/$1/$3");	//reorder date - prevents crash for German dates in MaryTTS!
				//specifics
				input = input.replaceAll("\\b(?i)(I('|´|`)m)\\b", "I am");	//fix I'm
			}
		
		}else{
			//"safety" stuff
			input = input.replaceAll("\\$", " \\$ ");		//Note: does this prevent variable expansion in Windows as well?
			input = input.replaceAll("%", " % ");		//Note: does this prevent variable expansion in Linux as well?
		}
		
		input = input.replaceAll("\\s+", " ").replaceAll(" \\?$", "?").replaceAll(" \\.$", ".").trim();
		return input;
	}

}
