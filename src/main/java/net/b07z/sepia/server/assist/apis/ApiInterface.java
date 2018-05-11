package net.b07z.sepia.server.assist.apis;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.interpreters.NluResult;

/**
 * The API interface for all plug-ins. It's pretty simple, just a get method that returns an API_Result :-)
 *  
 * @author Florian Quirin
 *
 */
public interface ApiInterface {
	
	/**
	 * Get the API_Result for this API. Should at least generate an answer, optionally include HTML info, card info
	 * and actions. Feel free to add whatever you like to the result, but keep in mind that the clients have to be specifically 
	 * adjusted to handle non-standard output besides answer, info, card and action.<br>
	 * To make life easier start every plug-in with "API api = new API(NLU_result)" and use the api.xy methods.
	 * 
	 * @param NLU_result - the result of the natural language processing containing all the extracted parameters and user input.
	 * @return API_Result
	 */
	public ApiResult getResult(NluResult NLU_result);
	
	/**
	 * Get all necessary info about the service/API.
	 * @param language - language code ("de", "en", ...)
	 * @return API_Info
	 */
	public ApiInfo getInfo(String language);
	
	/**
	 * Return a set of sample sentences for a specific language.
	 * @param language - language code ("de", "en", ...)
	 * @return Set of sentences
	 */
	public default TreeSet<String> getSampleSentences(String language){
		return new TreeSet<String>();
	}

}
