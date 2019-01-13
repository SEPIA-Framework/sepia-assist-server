package net.b07z.sepia.server.assist.answers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Is;

/**
 * A class to hold answers for a specific language defined inside a service. 
 * Basically just a wrapper around the answers map.
 * 
 * @author Florian Quirin
 *
 */
public class ServiceAnswers {
	
	public static final String ANS_PREFIX = "service.";
	
	private Map<String, List<Answer>> answersMap;
	private String language; 		//used to determine the language during "addAnswer"
	
	/**
	 * Create empty object that can be filled with {@link #addAnswer(Answer)}. 
	 * @param language - language code used when you add answers
	 */
	public ServiceAnswers(String language){
		this.language = language;
	}
	/**
	 * Create a service answers object to hold a custom answer mapping.
	 * @param answersMap - map with answer-key and list of answers for a specific language 
	 */
	public ServiceAnswers(Map<String, List<Answer>> answersMap){
		this.answersMap = answersMap;
	}
	/**
	 * Get the custom answers mapping.
	 * @return
	 */
	public Map<String, List<Answer>> getMap(){
		return answersMap;
	}
	/**
	 * Set the custom answers mapping.
	 * @param answersMap
	 */
	public void setMap(Map<String, List<Answer>> answersMap){
		this.answersMap = answersMap;
	}
	
	/**
	 * Add an {@link Answer} to this pool.<br>
	 * Note: Duplicates need to be handled manually, but you can add multiple answers of the same type (key).
	 * They will be chosen randomly then.
	 * @param answer - answer to add (key/type has to start with ANS_PREFIX)
	 * @return
	 */
	public ServiceAnswers addAnswer(Answer answer){
		String key = answer.getType();
		if (!key.startsWith(ANS_PREFIX)){
			throw new RuntimeException("Answer key (type) for ServiceAnswers has to start with prefix '" + ANS_PREFIX + "'.");
		}
		if (this.answersMap == null){
			this.answersMap = new HashMap<>();
		}
		List<Answer> theseAnswers = this.answersMap.get(key);
		if (theseAnswers == null){
			theseAnswers = new ArrayList<>();
			this.answersMap.put(key, theseAnswers);
		}
		theseAnswers.add(answer);
		return this;
	}
	/**
	 * Shortcut to build an {@link Answer} with 'Character.neutral' and mood 5.<br>
	 * Note: Duplicates need to be handled manually, but you can add multiple answers of the same type (key).
	 * They will be chosen randomly then.
	 * @param key - answer key, basically an ID to find an answer in the pool (don't use '.' (dot) in the name!). Has to start with ANS_PREFIX.
	 * @param repeatStage - 0: first answer given, 1: first repetition, 2: 2nd and more repetitions 
	 * @param answerText - actual answer text
	 * @return
	 */
	public ServiceAnswers addAnswer(String key, int repeatStage, String answerText){
		return addAnswer(new Answer(Language.from(this.language), 
				key, answerText, Answer.Character.neutral, repeatStage, 5));
	}
	
	/**
	 * Check if there is a custom answer in the pool for a certain key.
	 * @param key - key for the answer, e.g. hello_0a etc. (don't use modifiers like &ltdirect&gt). Has to start with ANS_PREFIX.
	 * @return
	 */
	public boolean containsAnswerFor(String key){
		if (Is.notNullOrEmptyMap(answersMap) && answersMap.containsKey(key)){
			return true;
		}else{
			return false;
		}
	}
}
