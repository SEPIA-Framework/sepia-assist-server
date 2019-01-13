package net.b07z.sepia.server.assist.interviews;

import java.util.List;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;

/**
 * The interview interface that is used to negotiate missing parameters.
 *  
 * @author Florian Quirin
 *
 */
public interface InterviewInterface {
	
	/**
	 * Connect command from CMD... This is necessary to load the correct services etc..
	 */
	public void setCommand(String command);
	
	/**
	 * Set services used by this interview. If this is not used the interview module tries to load them from mapping.
	 */
	public void setServices(List<ServiceInterface> services);
	
	/**
	 * Get the Interview_Result for this interview. Should interview the user until all required parameters are available.
	 * To make life easier start every "interview" with "Interview i = new Interview(NLU_result)" and use the i.xy methods.
	 * 
	 * @param nluResult - the result of the natural language processing containing all the extracted parameters and user input.
	 */
	public InterviewResult getMissingParameters(NluResult nluResult);
	
	/**
	 * Take an interview result and get the according service results.
	 */
	public ServiceResult getServiceResults(InterviewResult interviewResult);
	
	/**
	 * Get info of interview module.
	 * @param nluInput - NLU input
	 */
	public InterviewInfo getInfo(NluInput nluInput);

}

