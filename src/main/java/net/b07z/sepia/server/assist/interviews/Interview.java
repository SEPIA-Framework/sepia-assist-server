package net.b07z.sepia.server.assist.interviews;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.ParameterHandler;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * This class is a helper to do the interviews necessary to get all important parameters.<br>
 * Use it to build the {@link InterviewResult}!<br>
 *  
 * @author Florian Quirin
 *
 */
public class Interview {
	
	//tags for specific errors or actions
	public static final String ACTION_REGEX = "^<action_.*"; 						//use this to match an action result
	public static final String ACTION_SELECT = "<action_select>";
	public static final String ACTION_CONFIRM = "<action_confirm>";
	public static final String ACTION_ADD = "<action_add>";
	public static final String ERROR_REGEX = "^<error_.*"; 							//use this to match an error result
	public static final String ERROR_MISSING = "<error_missing>";					// - missing info that should be in user account 
	public static final String ERROR_API_FAIL = "<error_api_fail>";					// - missing info that comes from an external API (like geocoder)
	//info type - e.g. what type of info is missing
	public static final String TYPE_PERSONAL_INFO = "<personal_info>";
	public static final String TYPE_PERSONAL_LOCATION = "<personal_location>";
	public static final String TYPE_SPECIFIC_LOCATION = "<specific_location>"; 		//should be fused with <personal_location>
	public static final String TYPE_PERSONAL_CONTACT = "<personal_contact>";
	public static final String TYPE_INPUT_INFO = "<input_info>";					//use this for things like user_location or user_time
	public static final String TYPE_GEOCODING = "<geocoding>"; 						 
	
	//
	public NluResult nluResult;				//keep a reference to the NLU_Result for building API_Result
	public User user;							//reference to the user created by NLU_Result
	private Set<String> finals;			//final parameters as list
	private Set<String> dynamics;		//dynamic parameters as list. Dynamic parameters are used to add parameters to the interview handler that were previously not in required or optionals etc. ...
	public boolean isFinished = false;	//interview finished?
	
	/**
	 * Default constructor for an interview. 
	 * @param NLU_result - result coming from NL-Processor
	 */
	public Interview(NluResult NLU_result){
		this.nluResult = NLU_result;
		this.user = NLU_result.input.user;
		//get final parameters
		finals = new HashSet<>();
		String finals_str = NLU_result.getParameter(PARAMETERS.FINAL).replaceAll("^\\[|\\]$", "");
		if (!finals_str.isEmpty()){
			for (String p : finals_str.split(",")){
				finals.add(p);
			}
		}
		//get dynamic parameters
		dynamics = new HashSet<>();
		String dynamics_str = NLU_result.getParameter(PARAMETERS.DYNAMIC).replaceAll("^\\[|\\]$", "");
		if (!dynamics_str.isEmpty()){
			for (String p : dynamics_str.split(",")){
				dynamics.add(p);
			}
		}
	}
	/**
	 * Build from InerviewResult.
	 */
	public Interview(InterviewResult interviewResult){
		this(interviewResult.nluResult);
	}
	
	/**
	 * Simply get the input that was submitted by client. Could be empty, a simple string or JSON data as string.
	 * The parameter build method usually handles the rest. Input is also saved in "Parameter" itself.
	 * @param parameter - Parameter as defined by service module or interview
	 */
	public String getParameterInput(Parameter parameter){
		String input = nluResult.getParameter(parameter.getName());
		parameter.setInput(input);
		return input;
	}
	
	/**
	 * Default ask method. Asks a maximum of 3 times for the same "parameter" with "question" alternating depending on "rep" value of question.
	 * Aborts with a default answer like "I think we are stuck here somehow, can you try again later?".
	 * @param parameter - a parameter from the pool PARAMETER. ...
	 * @param question - reference to a question in the database, e.g. "flights_ask_start_0a" or direct question with tag &lt;direct&gt;.
	 * @param failMsg - reference to an answer that will be given on the 3rd try, e.g. "Sorry I didn't get it! Try again later."
	 * @return API_Result to send back to client
	 */
	public ServiceResult ask(String parameter, String question, String failMsg){
		if (nluResult.input.isRepeatedAnswerToParameter(parameter) < 2){
			return AskClient.question(question, parameter, nluResult);
		}else{
			return NoResult.get(nluResult, failMsg);
		}
	}
	/**
	 * Same as ask(String parameter, String question, String failMsg) but using "abort_0b" for failMsg.
	 */
	public ServiceResult ask(String parameter, String question){
		return ask(parameter, question, "abort_0b");
	}
	/**
	 * Shortcut to "ask(String parameter, String question, String failMsg)" with default "failMsg". Automatically chooses custom or default question. 
	 */
	public ServiceResult ask(Parameter parameter){
		String questionFail = parameter.getQuestionFailAnswer();
		String question = parameter.getQuestion();
		if (question.isEmpty()){
			//TODO: add Reply.getQuestionLink() here?
			Debugger.println("missing default question for '" + parameter.getName() + "'", 1);
		}
		if (!questionFail.isEmpty()){
			return ask(parameter.getName(), question, questionFail);
		}else{
			return ask(parameter.getName(), question);
		}
	}
	
	/**
	 * Checks finals for this parameter.
	 */
	public boolean isFinal(String parameter){
		return finals.contains(parameter);
	}
	/**
	 * Make this parameter final.
	 */
	public void makeFinal(String parameter, String finalValue){
		finals.add(parameter);
		nluResult.setParameter(parameter, finalValue);
		nluResult.setParameter(PARAMETERS.FINAL, finals.toString().replaceAll("\\s+", ""));
	}
	
	/**
	 * Get currently active dynamic parameters.
	 */
	public Set<String> getDynamicParameters(){
		return dynamics;
	}
	/**
	 * Checks if parameter is dynamic.
	 */
	public boolean isDynamic(String parameter){
		return dynamics.contains(parameter);
	}
	/**
	 * Remove dynamic parameter.
	 */
	public void removeDynamic(String parameter){
		dynamics.remove(parameter);
		nluResult.setParameter(PARAMETERS.DYNAMIC, dynamics.toString().replaceAll("\\s+", ""));
	}
	
	/**
	 * Get all {@link ServiceResult} from services.
	 * @param refList - list of services (String)
	 */
	public List<ServiceResult> getServiceResultsFromStringList(List<String> refList){
		List<ServiceResult> res = new ArrayList<>();
		for (String a : refList){
			ServiceInterface api = (ServiceInterface) ClassBuilder.construct(a);
			res.add(api.getResult(nluResult));
		}
		return res;
	}
	/**
	 * Get all {@link ServiceResult} from services.
	 * @param apiList - list of services ({@link ServiceInterface})
	 */
	public List<ServiceResult> getServiceResults(List<ServiceInterface> apiList){
		List<ServiceResult> res = new ArrayList<>();
		for (ServiceInterface api : apiList){
			res.add(api.getResult(nluResult));
		}
		return res;
	}
	
	//---------------------- interview handlers (ACTION, ERROR, ...) --------------------------
	
	/**
	 * Handle result actions. Typically this is a request to the user to add info to his personals (aborts the interview) 
	 * or to confirm/select options etc..
	 * @param getResult - result that has been received by "getLocation", "getTime", etc. and that contains an "action"
	 * @param customReply - a direct reply (&lt;direct&gt;my reply) or reply tag (e.g. custom_add_request_0a) of the answer that should be given. Use empty for default.  
	 */
	public ServiceResult handleInterviewAction(String getResult, String customReply){
		String[] data = getResult.split(";;");
		//System.out.println("handleInterviewAction - res: " + getResult); 		//debug
		if (data[0].equals(ACTION_ADD)){
			if (data[1].contains(TYPE_SPECIFIC_LOCATION)){
				nluResult.setParameter(PARAMETERS.TYPE, "addresses");
			}else if (data[1].contains(TYPE_PERSONAL_LOCATION)){
				nluResult.setParameter(PARAMETERS.TYPE, "favorites");
			}else if (data[1].contains(TYPE_PERSONAL_CONTACT)){
				nluResult.setParameter(PARAMETERS.TYPE, "contacts");
			}
			nluResult.setParameter(PARAMETERS.INFO, data[2]);
			nluResult.setParameter(PARAMETERS.REPLY, customReply);
			nluResult.setParameter(PARAMETERS.ACTION, "add");
			return ConfigServices.dashboard.getResult(nluResult);
		}
		//TODO: add more actions
		Debugger.println("handleInterviewAction(...) is missing a handler for: " + getResult, 1);
		return null;
	}
	
	/**
	 * Handle result errors. Typically this happens when the input parameters required to fulfill a task are not complete like a 
	 * missing user-time or geo-location.
	 * @param getResult - result that has been received by "getLocation", "getTime", etc. and that contains an "action"
	 * @param customReply - a direct reply (&lt;direct&gt;my reply) or reply tag (e.g. custom_add_request_0a) of the answer that should be given. Use empty for default.
	 */
	public ServiceResult handleInterviewError(String getResult, String customReply){
		String[] data = getResult.split(";;");
		if (data[0].equals(ERROR_MISSING)){
			if (data[2] != null && data[2].equals("<user_location>")){
				Debugger.println("NO_RESULT: " + nluResult.input.textRaw + " - due to missing GPS", 3);		//debug
				return NoResult.get(nluResult, "default_miss_user_location_0a");
			}else{
				Debugger.println("NO_RESULT: " + nluResult.input.textRaw + " - due to missing DATA", 3);		//debug
				return NoResult.get(nluResult, "default_miss_info_0a");
			}
		}else if (data[0].equals(ERROR_API_FAIL)){
			if (data[1].equals(TYPE_GEOCODING) && data[2].matches("<user_location>|<user_home>|<user_work>")){
				Debugger.println("NO_RESULT: " + nluResult.input.textRaw + " - due to failed API request: " + data[1] + ";;" + data[2], 3);	//debug
				return NoResult.get(nluResult, "error_geo_location_personal_0a");
			}else if (data[1].equals(TYPE_GEOCODING)){
				Debugger.println("NO_RESULT: " + nluResult.input.textRaw + " - due to failed API request: " + data[1] + ";;" + data[2], 3);	//debug
				return NoResult.get(nluResult, "error_geo_location_0a");
			}else{
				Debugger.println("NO_RESULT: " + nluResult.input.textRaw + " - due to failed API request: " + data[1] + ";;" + data[2], 3);	//debug
				return NoResult.get(nluResult, "error_0a");
			}
		}
		//TODO: add more errors
		Debugger.println("handleInterviewError(...) is missing a handler for: " + getResult, 1);
		return null;
	}
	
	//------------------ answer evaluations and basic result format conversion -------------------------
	
	//---------COMMON BUILD PROCEDURES---------
	
	//Note: this is a bit unorthodox as it writes the result in the nlu_result variable and returns 'null' as expected 'all good' result 
	//and an API_Result only if there is some action required 
	
	/**
	 * This method is called by an interview module when there is non-final input. It tries to build the default-format result
	 * for the "Parameter". Additional service-specific methods might be added via "InterviewInfo". 
	 */
	public ServiceResult buildParameterOrComment(Parameter p, InterviewInfo iInfo){
		//1) parameter extraction
		//-a) response handler does parameter search. TODO: Force parameter search here on direct input?
		//...
		//-b) add API specific extraction methods here ...
		//...
		//2) validate parameter
		//-a) add API specific validation methods here ...
		//...
		//-b) check if it is already a valid parameter. If not try to build the parameter result using input and account specific info (if necessary)
		String input = p.getInput();
		String parameter = p.getName();
		ParameterHandler handler = p.getHandler();
		handler.setup(nluResult);
		
		//return buildParameterOrComment(input, parameter, handler);
		
		//check first if the parameter is already valid (TODO: is this a redundant call? Maybe not ...)
		if (handler.validate(input)){
			makeFinal(parameter, input);
			return null; //this means "all fine, no comments" ^^
		}
		//handle it now
		String resultJSON = handler.build(input);
		if (handler.buildSuccess()){ 			//TODO: better use? (handler.validate(resultJSON)){
			makeFinal(parameter, resultJSON);
			return null; //this means "all fine, no comments" ^^
			
		//the build method decided to clear the result, probably because it was invalid or not yet known, so return once more to question if it is required
		}else if (resultJSON.isEmpty()){
			//ask if required
			if (p.isRequired()){
				return ask(p);
				
			//else clear and ignore
			}else{
				nluResult.removeParameter(p.getName());
				return null;
			}
			
		//check if result was an action - e.g. missing data to found element
		}else if (resultJSON.matches(ACTION_REGEX)){
			return handleInterviewAction(resultJSON, "");
			
		//check if result was an error - e.g. missing user data like geo-location
		}else if (resultJSON.matches(ERROR_REGEX)){
			return handleInterviewError(resultJSON, "");
			
		//everything else can only mean complete fail now. Usually this point should never be reached.
		}else{
			Debugger.println("buildParameterOrComment(...) gave no result at all! Parameter: " + parameter + ", Input was: " + input, 1);
			return NoResult.get(nluResult);
		}
	}
	/**
	 * This method is called by an interview module when there is non-final input. It tries to build the default-format result
	 * for the parameter. 
	 */
	/*
	public API_Result buildParameterOrComment(String input, String parameter){
		Parameter_Handler handler = (Parameter_Handler) Tools.construct(ParameterConfig.getHandler(parameter));
		handler.setup(nlu_result);
		return buildParameterOrComment(input, parameter, handler);
	}
	*/
	/**
	 * This method is called by an interview module when there is non-final input. It tries to build the default-format result
	 * for the parameter. 
	 */
	/*
	public API_Result buildParameterOrComment(String input, String parameter, Parameter_Handler handler){
		//check first if the parameter is already valid (TODO: is this a redundant call?)
		if (handler.validate(input)){
			makeFinal(parameter, input);
			return null; //this means "all fine, no comments" ^^
		}
		//handle it now
		String resultJSON = handler.build(input);
		if (handler.buildSuccess()){ 			//TODO: better use? (handler.validate(resultJSON)){
			makeFinal(parameter, resultJSON);
			return null; //this means "all fine, no comments" ^^
			
		//the build method decided to clear the result, probably because it was invalid or not yet known, so return to question once more
		}else if (resultJSON.isEmpty()){
			return ask(p);
			
		//check if result was an action - e.g. missing data to found element
		}else if (resultJSON.matches(ACTION_REGEX)){
			return handleInterviewAction(resultJSON, "");
			
		//check if result was an error - e.g. missing user data like geo-location
		}else if (resultJSON.matches(ERROR_REGEX)){
			return handleInterviewError(resultJSON, "");
			
		//everything else can only mean complete fail now. Usually this point should never be reached.
		}else{
			Debugger.println("buildParameterOrComment(...) gave no result at all! Parameter: " + parameter + ", Input was: " + input, 1);
			return NoResult.get(nlu_result);
		}
	}
	*/
}
