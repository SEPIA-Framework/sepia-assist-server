package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.YesNo;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;

/**
 * In case a parameter is missing the server can ask the client for it.
 * Use this class to do that.
 * Note that this class does not implement the default "get" method but an extended "question"
 * method that requires a question-key (e.g.: directions_ask_end_0a) to load the appropriate
 * question from the database and a type of input that is asked for (search, start, end, type, etc...).
 * A direct question (not taken from the database) can be given by adding the tag &#60direct&#62 to the beginning
 * of the question key. 
 * 
 * @author Florian Quirin
 *
 */
public class AskClient {
	
	
	/**
	 * Create a result for a client question.
	 * @param questionKey - key to find question in database.
	 * @param missing_input - parameter type that is missing (search, start, end, etc... empty or default or text is standard)
	 * @param nluResult - result of the NLP
	 * @param wildcards - parameters given to the question to fill out wildcards
	 * @return
	 */
	public static ServiceResult question(String questionKey, String missingInputParam, NluResult nluResult, Object... wildcards){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		//debug
		//System.out.println("Dialog stage/Last CMD rep.: " + NLU_result.input.dialog_stage + "/" + NLU_result.input.last_cmd_N);
		
		//make this a question
		api.makeThisAQuestion(missingInputParam); 
		
		//get answer/question to client
		api.answer = Answers.getAnswerString(nluResult, questionKey, wildcards);
		api.answerClean = Converters.removeHTML(api.answer);
		//remove vocal smileys
		api.answer = AnswerTools.cleanHtmlAnswer(api.answer);
		
		api.status = "success";
		
		//anything else?
		api.context = nluResult.context;			//the context remains the previous one
		api.cmdSummary = nluResult.cmdSummary;		//cmd_summary is very important here because it's needed to fuse answer and initial command later
		
		//if there is a question offer abort button
		api.addAction(ACTIONS.SHOW_ABORT_OPTION);
		api.hasAction = true;
		
		//add yes / no button if the command fits
		if (missingInputParam.equals(PARAMETERS.YES_NO)){
			String yes = YesNo.getLocal("<yes>", nluResult.language);
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", yes);
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", api.cmdSummary.replaceFirst(PARAMETERS.YES_NO + "=.*?;;", PARAMETERS.YES_NO + "=<yes>"));
			api.putActionInfo("visibility", "inputHidden");
			
			String no = YesNo.getLocal("<no>", nluResult.language);
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", no);
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", api.cmdSummary.replaceFirst(PARAMETERS.YES_NO + "=.*?;;", PARAMETERS.YES_NO + "=<no>"));
			api.putActionInfo("visibility", "inputHidden");
		}
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();

		//return result_JSON.toJSONString();
		return result;
	}
	
	/**
	 * Increase access level. The way this works should be: this API asks for some info that justifies the increase, saves a unique token
	 * to the database and sends it back to the client. The client passes it on the next request to the server, the server automatically
	 * registers the increase token, compares it to the database and calls the old API (the one that needs the access) again with the new
	 * access level. 
	 */
	public static ServiceResult increaseAccessLevel(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		//debug
		//System.out.println("Dialog stage/Last CMD rep.: " + NLU_result.input.dialog_stage + "/" + NLU_result.input.last_cmd_N);
		
		//get answer/question to client
		api.answer = Answers.getAnswerString(nluResult, "default_ask_incaccess_0a");
		api.answerClean = Converters.removeHTML(api.answer);
		
		//TODO: IMPLEMENT AUTHORIZATION ACTION 
		
		api.status = "success";
		
		//anything else?
		api.context = nluResult.context;				//the context remains the previous one
		api.cmdSummary = nluResult.cmdSummary;		//cmd_summary is very important here because it's needed to fuse answer and initial command later
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();

		//return result_JSON.toJSONString();
		return result;
	}
}
