package net.b07z.sepia.server.assist.interviews;

import java.util.ArrayList;

import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.interpreters.NluResult;

/**
 * The interview interface that is used to negotiate missing parameters.
 *  
 * @author Florian Quirin
 *
 */
public interface Interview_Interface {
	
	/**
	 * Connect command from CMD... This is necessary to load the correct services etc..
	 */
	public void setCommand(String command);
	
	/**
	 * Set services used by this interview. If this is not used the interview module tries to load them from mapping.
	 */
	public void setServices(ArrayList<ApiInterface> services);
	
	/**
	 * Get the Interview_Result for this interview. Should interview the user until all required parameters are available.
	 * To make life easier start every "interview" with "Interview i = new Interview(NLU_result)" and use the i.xy methods.
	 * 
	 * @param NLU_result - the result of the natural language processing containing all the extracted parameters and user input.
	 */
	public InterviewResult getMissingParameters(NluResult NLU_result);
	
	/**
	 * Take an interview result and get the according service results.
	 */
	public ApiResult getServiceResults(InterviewResult interviewResult);
	
	/**
	 * Get info of interview module.
	 * @param language - language code ("de", "en", ...)
	 */
	public InterviewInfo getInfo(String language);

}

