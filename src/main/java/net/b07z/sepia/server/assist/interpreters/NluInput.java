package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.parameters.ParameterResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

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
	public String text = "";				//text:		input text/question/query
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
	public String userTimeLocal = "";		//time_local:		system date and time at user location, default format 2016.12.31_22:44:11
	public User user;						//user:				holds all info about the user, can reload from account
	//... more to come
	public String deviceId = "";			//device_id:		an ID defined by the user to identify a certain device
	public String msgId = null;				//msg_id:			an ID to identify request, especially helpful in duplex scenarios
	public String duplexData = null;		//duplex_data:		data helpful to trace back a duplex call and answer or follow-up, e.g. the chat-channel-ID. Format is JSON, parse when required
	public String connection = "http";		//connection:		http request or WebSocket connection - has influence on delayed replies
	public boolean demoMode = false;		//demomode:			true/false if you want to use the demomode
	public String customData = null; 		//custom_data:		a dynamic variable to carry any data that does not fit to the pre-defined stuff. Should be a JSONObject converted to string.
	private JSONObject customDataJson = null;					//custom data is parsed when needed and the result is stored here. 
	
	//Stuff to cache during all processes from NLU to service result:
	
	//Some session cache variables:
	List<CmdMap> customCommandToServicesMappings;					//custom services session storage (currently not used?)
	private Map<String, ServiceInfo> sessionCacheServiceInfo;		//cache service info by canonical name, so we don't have to rebuild all the time
	private Map<String, ServiceAnswers> sessionCacheServiceAnswers; //cache custom service answers by trigger command, so we don't have to rebuild all the time
	public Map<String, ParameterResult> parameterResultStorage = new HashMap<>();	//parameter result storage for extracted ones during NLU processing
		
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
	
	/**
	 * Set user local time.
	 * @param timeString - Date in default format (Config.defaultSdf): "yyyy.MM.dd_HH:mm:ss"
	 */
	public void setTimeGMT(String timeString){
		this.userTimeLocal = timeString;
		this.userTime = DateTimeConverters.getUnixTimeOfDateGMT(userTimeLocal, Config.defaultSdf);
	}
	
	//handle (session) STORAGEs:
	
	//custom command to services mappings
	/**
	 * A session storage to prevent multiple DB calls. See:<br> 
	 * {@link net.b07z.sepia.server.assist.server.ConfigServices#restoreOrLoadCustomCommandMapping}
	 */
	public void setCustomCommandToServicesMappings(List<CmdMap> listOfMappings){
		customCommandToServicesMappings = listOfMappings;
	}
	/**
	 * A session storage to prevent multiple DB calls. See:<br> 
	 * {@link net.b07z.sepia.server.assist.server.ConfigServices#restoreOrLoadCustomCommandMapping}
	 */
	public List<CmdMap> getCustomCommandToServicesMappings(){
		return customCommandToServicesMappings;
	}
	/**
	 * A session storage to prevent multiple DB calls. See:<br> 
	 * {@link net.b07z.sepia.server.assist.server.ConfigServices#restoreOrLoadCustomCommandMapping}
	 */
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
	
	//Service info cache
	/**
	 * Cache the {@link ServiceInfo} of a certain command for this session.
	 * @param serviceCanonicalName - command that triggered the service with custom answers
	 * @param info - {@link ServiceInfo} to be cached 
	 */
	public void cacheServiceInfo(String serviceCanonicalName, ServiceInfo info){
		if (sessionCacheServiceInfo == null){
			sessionCacheServiceInfo = new HashMap<>();
		}
		sessionCacheServiceInfo.put(serviceCanonicalName, info);
	}
	/**
	 * Get the cached service info or null. 
	 * @param serviceCanonicalName - command that triggered the service with custom answers
	 * @return
	 */
	public ServiceInfo getCachedServiceInfo(String serviceCanonicalName){
		if (sessionCacheServiceInfo != null){
			return sessionCacheServiceInfo.get(serviceCanonicalName);
		}else{
			return null;
		}
	}
	/**
	 * Get service info cache size.
	 */
	public int getServiceInfoCacheSize(){
		if (sessionCacheServiceInfo != null){
			return sessionCacheServiceInfo.size();
		}else{
			return 0;
		}
	}
	
	//ServiceAnswers cache
	/**
	 * Cache the {@link ServiceAnswers} of a certain command for this session.
	 * @param serviceCommand - command that triggered the service with custom answers
	 * @param answers - {@link ServiceAnswers} to be cached 
	 */
	public void cacheServiceAnswers(String serviceCommand, ServiceAnswers answers){
		if (sessionCacheServiceAnswers == null){
			sessionCacheServiceAnswers = new HashMap<>();
		}
		sessionCacheServiceAnswers.put(serviceCommand, answers);
	}
	/**
	 * Get the cached custom service answers or null. 
	 * @param serviceCommand - command that triggered the service with custom answers
	 * @return
	 */
	public ServiceAnswers getCachedServiceAnswers(String serviceCommand){
		if (sessionCacheServiceAnswers != null){
			return sessionCacheServiceAnswers.get(serviceCommand);
		}else{
			return null;
		}
	}
	
	//Custom data
	/**
	 * Get a value of the custom-data object submitted by client.
	 * @param key - the field in the JSON object
	 * @return value or null
	 */
	public Object getCustomDataObject(String key){
		if (customDataJson != null){
			return customDataJson.get(key);
		}else if (Is.notNullOrEmpty(customData)){
			JSONObject cd = JSON.parseString(customData);
			if (cd != null){
				customDataJson = cd;
				return customDataJson.get(key);
			}else{
				return null;
			}
		}else{
			return null;
		}
	}
	
	//-------helper methods--------
	
	//connection type
	public boolean isDuplexConnection(){
		return (connection.equals("ws")); 		//more types can be added here when available
	}
	
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
