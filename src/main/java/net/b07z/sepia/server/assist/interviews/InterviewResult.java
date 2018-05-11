package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.interpreters.NluResult;

/**
 * This class acts as a bridge between API_Result (that may be a question or comment) and NLU_Result.
 * 
 * @author Florian Quirin
 *
 */
public class InterviewResult {
	
	ApiResult apiResult;
	NluResult nluResult;
	InterviewInfo iInfo;
	boolean isComplete = false;
	
	public InterviewResult(Interview interview, ApiResult apiResult){
		this.nluResult = interview.nlu_result;
		this.isComplete = interview.isFinished;
		this.apiResult = apiResult;
	}
	public InterviewResult(ApiResult apiResult){
		this.apiResult = apiResult;
	}
	public InterviewResult(Interview interview){
		this.nluResult = interview.nlu_result;
		this.isComplete = interview.isFinished;
	}
	
	/**
	 * If the interview results in a question, comment, exception, abort, etc. then it will contain an API result.
	 */
	public ApiResult getApiComment(){
		return apiResult;
	}
	
	/**
	 * If the interview is complete you will get this updated NLU_Result.
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
	 * Checks if the interview is finished and ready for API 
	 * or if it returns a result that is a e.g. question to continue gathering parameters.
	 */
	public boolean isComplete(){
		return isComplete;
	}

}
