package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.assist.parameters.ParameterResult;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.CmdMap;

/**
 * Use this to generate the input for any NL-Processor.
 * Language determines the general language for interpretation and response (answer and API calls).
 * The NLP can change behavior depending on parameters like mood, context, environment, last answer, etc...
 * 
 * @author Florian Quirin
 *
 */
public class NluInput {
	
	//input type constants
	public static final String TYPE_QUESTION = "question";
	public static final String TYPE_RESPONSE = "response";

	//main input	-						API param /	Description
	//public Request request;				//request data
	public String text = "";				//:text:	input text/question/query
	public String textRaw = "";				//			input text in its raw form, no replacements, no cmd transformation (e.g. for direct commands it tries to retain the text)
	public String language = "en";			//lang: 	language used for interpretation and results (ISO 639-1 code)
	public String context = "default";		//context:	context is what the user did/said before to answer queries like "do that again" or "and in Berlin?"
	public String environment = "default";	//env:		environments (web_app, android_app, ios_app) can be stuff like home, car, phone etc. to restrict and tweak some results
	public String clientInfo = "default";	//client:	information about client and client version
	public int mood = -1;					//mood:		mood value 0-10 of ILA (or whatever the name of the assistant is) can be passed around too, 10 is the best. Used e.g. in get_answers and TTS
	//last command
	public String lastCmd = "";				//last_cmd:		last used command (actually a summary of the last used command)
	public int lastCmdN = 0;				//last_cmd_N:	counts how many times the same last command has been used in a row
	//input type
	public String inputType = "question";	//input_type:		default is "question", extended dialogues can use "response"
	public String inputMiss = "";			//input_miss:		if input_type is "response" than this says what's missing (type, search, ..., for chats: yes_no, good_bad, ...)
	public int dialogStage = 0;				//dialog_stage:		extended dialogues can undergo multiple stages
	//personal info
	public String userLocation = "";		//user_location:	address, longitude, latitude coordinates of user
	public long userTime = -1;				//time:				system time at request sent
	public String userTimeLocal = "";		//time_local:		system date and time at locally at user location, default format 2016.12.31_22:44:11
	public User user;						//user:				holds all info about the user, can reload from account
	//... more to come
	public boolean demoMode = false;		//demomode:			true/false if you want to use the demomode
	
	//Stuff to cache during all processes from NLU to service result:
	
	//custom services storage
	List<CmdMap> customCommandToServicesMappings;
	
	//parameter result storage for extracted ones during NLU processing
	public Map<String, ParameterResult> parameterResultStorage = new HashMap<>();
		
	//CONTRUCTORs
	public NluInput(){
	}
	/**
	 * Create input.
	 * @param text - input text/question/query ...
	 */
	public NluInput(String text){
		this.text = text;
		this.textRaw = text;
	}
	/**
	 * Create input.
	 * @param text - input text/question/query ...
	 * @param language - language used to interpret text and inside APIs
	 */
	public NluInput(String text, String language){
		this.text = text;
		this.textRaw = text;
		this.language = language;
	}
	/**
	 * Create input.
	 * @param text - input text/question/query ...
	 * @param language - language used to interpret text and inside APIs
	 * @param context - context of a command like "do that again"
	 * @param mood - mood level 0-10 (10: super happy) of the assistant (may change answers ^^)
	 * @param environment - environments like home, car, mobile, watch ...
	 */
	public NluInput(String text, String language, String context, int mood, String environment){
		this.text = text;
		this.textRaw = text;
		this.language = language;
		this.context = context;
		this.mood = mood;
		this.environment = environment;
	}
	
	public void makeNewFromOld(NluInput input){
		
	}
	
	//handle STORAGEs:
	
	//custom command to services mappings
	public void setCustomCommandToServicesMappings(List<CmdMap> listOfMappings){
		customCommandToServicesMappings = listOfMappings;
	}
	public List<CmdMap> getCustomCommandToServicesMappings(){
		return customCommandToServicesMappings;
	}
	public void clearCustomCommandToServicesMappings(){
		customCommandToServicesMappings.clear();
	}
	
	//parameter results
	/**
	 * Add a parameter result to temporary session storage for usage in other parameters for example.
	 */
	public void addToParameterResultStorage(ParameterResult pr){
		parameterResultStorage.put(pr.getName(), pr);
	}
	/**
	 * Get a stored parameter result for this session or return null. <br>
	 * Note: you can use {@link ParameterResult#getResult} as well.
	 */
	public ParameterResult getStoredParameterResult(String pName){
		return parameterResultStorage.get(pName);
	}
	/**
	 * Clear session storage of parameters.
	 */
	public void clearParameterResultStorage(){
		parameterResultStorage = new HashMap<>();
	}
	/**
	 * Clear specific parameter buffered in session storage.
	 */
	public void clearParameterResult(String pName){
		parameterResultStorage.remove(pName);
	}
	
	//-------helper methods--------
	
	//question/answer handling
	/**
	 * Check if this input is an answer to a question asked by the assistant.
	 * @return true/false
	 */
	public boolean isAnswerToQuestion(){
		if (inputType.equals(TYPE_RESPONSE)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * Check if this input is an answer to the given parameter (PARAMETER...). This comes in handy if the answer of the user
	 * to a question is filtered and returns empty again because of a missing match. Use this to avoid endless loops.
	 * @param parameter - PARAMETER.xy that is required for the API
	 * @return true/false
	 */
	public boolean isAnswerToParameter(String parameter){
		if (inputType.equals(TYPE_RESPONSE) && inputMiss.equals(parameter)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * Check if this input is a repeated answer to the given parameter (PARAMETER...). This comes in handy if the answer of the user
	 * to a question is filtered and returns empty again because of a missing match. Use this to avoid endless loops. 
	 * Depends on unique parameters and is quiet safe BUT lags one answer behind giving 0,0,1,2 :-/
	 * 
	 * @param parameter - PARAMETER.xy that is required for the API
	 * @return number of repeats (note: it lags because it is evaluated in client and gives two times 0 at the beginning: 0,0,1,2)
	 */
	public int isRepeatedAnswerToParameter(String parameter){
		if (inputType.equals(TYPE_RESPONSE) && inputMiss.equals(parameter)){
			return lastCmdN; 		//last_cmd_N actually lags behind, giving 0,0,1,2 because it is evaluated in client ... i guess
		}else{
			return 0;
		}
	}
	/**
	 * Check if this input is a repeated answer to a question. Use this to avoid endless loops.
	 * Requires unique parameters.
	 *  
	 * @return true/false
	 */
	public boolean isRepeatedAnswer(String parameter){
		//return (last_cmd_N>0);
		/*
		String A = nlu_result.cmd_summary.replaceAll("parameter_set=.*?;;", "");
		String B = Pattern.quote(last_cmd).replaceAll("parameter_set=.*?;;", "");
		return (A.matches(B));
		*/
		if (inputType.equals(TYPE_RESPONSE) && inputMiss.equals(parameter)){
			return true;
		}else{
			return false;
		}
	}
}
