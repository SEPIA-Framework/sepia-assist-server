package net.b07z.sepia.server.assist.answers;

import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.core.data.Answer;

/**
 * The default interface to load and get answers using answer_keys that point to a database.
 * 
 * @author Florian Quirin
 *
 */
public interface AnswerLoader {
	
	/**
	 * Get answer from default memory using a key string and all parameters stored in NluResult like language, mood etc.
	 * If the answer is not found it should return a "missing answer" or "error" notification suitable for the client! 
	 * The way this is supposed to work is loading a language specific answer from a pool of answers stored on the database
	 * by using key tags like "music_0a", "music_0b", "music_1a" ... etc.<br>
	 * Be sure to support direct questions when the key is tagged with &#60;direct&#62; at the beginning!<br>
	 * @param nluResult - NluResult including NluInput, contains all necessary info to make decisions and act user specific
	 * @param key - key string to identify answer e.g. greetings_0a
	 * @return answer as string
	 */
	public String getAnswer(NluResult nluResult, String key);
	
	/**
	 * Get answer from default memory using a key string and all parameters stored in NluResult like language, mood etc.
	 * If the answer is not found it should return a "missing answer" or "error" notification suitable for the client!
	 * The way this is supposed to work is loading a language specific answer from a pool of answers stored on the database
	 * by using key tags like "music_0a", "music_0b", "music_1a" ... etc. These answers can contain wildcards like "&#60;1&#62;", "&#60;2&#62;", 
	 * "&#60;user_name&#62;" etc. that get replaced with the actual values either here and/or at client-side.<br>
	 * Be sure to support direct questions when the key is tagged with &#60;direct&#62; at the beginning!<br>
	 * <br>
	 * @param nluResult - NluResult including NluInput, contains all necessary info to make decisions and act user specific
	 * @param key - key string to identify answer e.g. greetings_0a
	 * @param wildcards - replacements for wildcard text elements
	 * @return answer as string
	 */
	public String getAnswer(NluResult nluResult, String key, Object... wildcards);
	
	/**
	 * Get answer from user-defined memory using a key string and all parameters stored in NluResult like language, mood etc.
	 * Use this if you want to use your own pool of answers inside a custom plug-in. The plug-in should then pre-load the answers with "load_to_memory".<br>
	 * If the answer is not found it should return a "missing answer" or "error" notification suitable for the client! 
	 * The way this is supposed to work is loading a language specific answer from a pool of answers stored on the database
	 * by using key tags like "music_0a", "music_0b", "music_1a" ... etc.<br>
	 * Be sure to support direct questions when the key is tagged with &#60;direct&#62; at the beginning!<br>
	 * @param memory - HashMap storing answers for a certain language, previously loaded with "setupAnswers()"
	 * @param nluResult - NluResult including NluInput
	 * @param key - key string to identify answer e.g. greetings_0a
	 * @return answer as string
	 */
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key);
	
	/**
	 * Get answer from user-defined memory using a key string and all parameters stored in NluResult like language, mood etc.
	 * Use this if you want to use your own pool of answers inside a custom plug-in. The plug-in should then pre-load the answers with "load_to_memory".<br>
	 * If the answer is not found it should return a "missing answer" or "error" notification suitable for the client!
	 * The way this is supposed to work is loading a language specific answer from a pool of answers stored on the database
	 * by using key tags like "music_0a", "music_0b", "music_1a" ... etc. These answers can contain wildcards like "&#60;1&#62;", "&#60;2&#62;", 
	 * "&#60;user_name&#62;" etc. that get replace with the actual values either here and/or at client-side.<br>
	 * Be sure to support direct questions when the key is tagged with &#60;direct&#62; at the beginning!<br>
	 * <br>
	 * @param memory - HashMap storing answers for a certain language, previously loaded with "setupAnswers()"
	 * @param nluResult - NluResult including NluInput
	 * @param key - key string to identify answer e.g. greetings_0a
	 * @param wildcards - replacements for wildcard text elements
	 * @return answer as string
	 */
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key, Object... wildcards);
	
	/**
	 * This will be called on server initialization so if the interface implementation is working with 
	 * pre-loaded answers load them here or do any kind of initialization.
	 */
	public void setupAnswers();
	
	/**
	 * Set the pool of possible answers. Not every implementation has to use this, but if it does than this should be used to update
	 * pre-loaded answers.
	 * @param answersPool - pool of answers loaded from txt-files or database.
	 */
	public void updateAnswerPool(Map<String, Map<String, List<Answer>>> answersPool);
}
