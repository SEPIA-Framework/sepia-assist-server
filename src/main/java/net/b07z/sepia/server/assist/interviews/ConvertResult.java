package net.b07z.sepia.server.assist.interviews;

import java.util.List;

import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.ConfigServices;

/**
 * Switch from one service result to another.
 * 
 * @author Florian Quirin
 *
 */
public class ConvertResult {
	
	/**
	 * Convert the API_Result from one service to another. Both services must support the new standard!
	 * Supports one parameter transfer, if you need more or more complex changes you should edit the NLU_Result
	 * in advance. 
	 * @param cmd - target command that determines the service
	 * @param nluResult - result of old service
	 * @param oldParameter - old parameter to be replaced
	 * @param newParameter - new parameter for the new service
	 * @param newValue - new value of the new parameter, can be the old one or a modified version
	 */
	public static ApiResult switchService(String cmd, NluResult nluResult, 
							String oldParameter, String newParameter, String newValue){
		//rewrite NLU_Result
		nluResult.setParameter(newParameter, newValue);
		nluResult.removeParameter(oldParameter);
		
		List<ApiInterface> services = ConfigServices.getCustomOrSystemServices(nluResult.input, nluResult.input.user, cmd);
		//this should not be the case here but to be sure ...
		if (services == null || services.isEmpty()){
			return NoResult.get(nluResult);
		}
		//re-do the "interview"
		InterviewInterface interview = new AbstractInterview();
		interview.setCommand(cmd);
		interview.setServices(services);
		InterviewResult iResult = interview.getMissingParameters(nluResult);
		if (iResult.isComplete()){
			return interview.getServiceResults(iResult);
		}else{
			return iResult.getApiComment();
		}
	}

}
