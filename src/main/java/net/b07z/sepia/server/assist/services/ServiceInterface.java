package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.interpreters.NluResult;

/**
 * The interface for all services, aka: plug-ins, skills, abilities, APIs. 
 * It's pretty simple and returns e.g. {@link ServiceInfo} and {@link ServiceResult} :-)
 *  
 * @author Florian Quirin
 *
 */
public interface ServiceInterface {
	
	/**
	 * Get the {@link ServiceResult} for this service. Should at least generate an answer, optionally include HTML info, card info
	 * and actions. Feel free to add whatever you like to the result, but keep in mind that the clients have to be specifically 
	 * adjusted to handle non-standard output besides answer, info, card and action.<br>
	 * To make life easier start every plug-in with "{@link ServiceBuilder} sb = new ServiceBuilder(nluResult)" and use the sb.xy methods.
	 * 
	 * @param nluResult - the result of the natural language processing containing all the extracted parameters and user input.
	 * @return service result
	 */
	public ServiceResult getResult(NluResult nluResult);
	
	/**
	 * Get all necessary info about the service/API.
	 * @param language - language code ("de", "en", ...)
	 * @return service info
	 */
	public ServiceInfo getInfo(String language);
	
	/**
	 * Get the custom answers pool defined in this service. For system services this is usually null
	 * because they take answers from the global pool.
	 * @param language - language code ("de", "en", ...)
	 * return answer pool for this language or null
	 */
	public default ServiceAnswers getAnswersPool(String language){
		return null;
	}
	
	/**
	 * Return a set of sample sentences for a specific language.
	 * @param language - language code ("de", "en", ...)
	 * @return Set of sentences
	 */
	public default TreeSet<String> getSampleSentences(String language){
		return new TreeSet<String>();
	}

}
