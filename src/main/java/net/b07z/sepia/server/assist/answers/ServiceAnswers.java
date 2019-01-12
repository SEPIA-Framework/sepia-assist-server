package net.b07z.sepia.server.assist.answers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.tools.Is;

/**
 * A class to hold answers for a specific language defined inside a service. 
 * Basically just a wrapper around the answers map.
 * 
 * @author Florian Quirin
 *
 */
public class ServiceAnswers {
	
	private Map<String, List<Answer>> answersMap;
	//private String language; 		//this info usually is inside the answer object
	
	/**
	 * Create empty object that can be filled with {@link #addAnswer(Answer)}. 
	 */
	public ServiceAnswers(){}
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
	 * Note: Duplicates need to be handled manually.
	 * @param answer - answer to add
	 * @return
	 */
	public ServiceAnswers addAnswer(Answer answer){
		if (this.answersMap == null){
			this.answersMap = new HashMap<>();
		}
		String key = answer.getType();
		List<Answer> theseAnswers = this.answersMap.get(key);
		if (theseAnswers == null){
			theseAnswers = new ArrayList<>();
			this.answersMap.put(key, theseAnswers);
		}
		theseAnswers.add(answer);
		return this;
	}
	
	/**
	 * Check if there is a custom answer in the pool for a certain key.
	 * @param key - key for the answer, e.g. chat_hello_0a etc. (don't use modifiers like &ltdirect&gt)
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
