package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceResult;

/**
 * This class acts as a bridge between {@link ServiceResult} (that may be a question or comment) and {@link NluResult}.
 * 
 * @author Florian Quirin
 *
 */
public class InterviewResult {
	
	ServiceResult apiResult;
	NluResult nluResult;
	InterviewInfo iInfo;
	boolean isComplete = false;
	
	public InterviewResult(Interview interview, ServiceResult apiResult){
		this.nluResult = interview.nluResult;
		this.isComplete = interview.isFinished;
		this.apiResult = apiResult;
	}
	public InterviewResult(ServiceResult apiResult){
		this.apiResult = apiResult;
	}
	public InterviewResult(Interview interview){
		this.nluResult = interview.nluResult;
		this.isComplete = interview.isFinished;
	}
	
	/**
	 * If the interview results in a question, comment, exception, abort, etc. then it will contain an {@link ServiceResult} result.
	 */
	public ServiceResult getApiComment(){
		return apiResult;
	}
	
	/**
	 * If the interview is complete you will get this updated {@link NluResult}.
	 */
	public NluResult getUpdatedNLU(){
		return nluResult;
	}
	
	/**
	 * Add interview info to result.
	 */
	public void setInterviewInfo(InterviewInfo iInfo){
		this.iInfo = iInfo;
	}
	/**
	 * Get interview info generated while building result. 
	 */
	public InterviewInfo getInterviewInfo(){
		return iInfo;
	}
	
	/**
	 * Checks if the interview is finished and ready for service 
	 * or if it returns a result that is a e.g. question to continue gathering parameters.
	 */
	public boolean isComplete(){
		return isComplete;
	}

}
