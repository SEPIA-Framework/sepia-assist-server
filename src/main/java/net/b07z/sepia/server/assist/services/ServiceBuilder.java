package net.b07z.sepia.server.assist.services;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.Assistant;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.parameters.Confirm;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * This class is a helper to build services and it includes a default set of necessary variables.<br>
 * Use it to build the {@link ServiceResult}!<br> 
 * To a large degree its a duplication of {@link ServiceResult} which should be resolved at some point.<br>
 * NOTE: manipulations in the variables may have to be copied back to {@link NluResult} to avoid bugs like
 * the "wrong-answer-repeat" due to "input.input_miss"
 *  
 * @author Florian Quirin
 *
 */
public class ServiceBuilder {
	
	//statics
	public static final String RESPONSE_QUESTION = "question";	//question marker
	public static final String RESPONSE_INFO = "info";			//info marker
	public static final String REQUEST_TAG = "request_tag"; 		//a short tag describing the service (?)
	public static final String SUCCESS = "success";
	public static final String FAIL = "fail";
	public static final String OKAY = "still_ok";
	public static final String INCOMPLETE = "incomplete";
	
	//initialize result
	public String answer = "";							//spoken answer (can include html code like links, spans, line break etc.)
	public String answerClean = "";						//clean spoken answer (without html code)
	public String htmlInfo = "";						//extended html content to answer (e.g. for info center)
	public JSONArray cardInfo = new JSONArray();		//compact info for "cards"
	public JSONArray actionInfo = new JSONArray();		//info to execute an action
	public JSONObject resultInfo = new JSONObject();	//common result info, important for displaying result view details and build answer parameters
	public boolean hasInfo = false;						//is extended html content available?
	public boolean hasCard = false;						//is content for a "card" available?
	public boolean hasAction = false;					//is an action available?
	public String status = "still_ok";					//result status (fail/success/still_ok/incomplete) - fail is for serious errors without planned answer, success is for perfect answer as planned, still_ok is with answer but no result, incomplete is when you want to take control from inside service
	public JSONObject more = new JSONObject();			//flexible, additional parameters (more stuff I most likely forgot to implement right now or that is different from API to API)
	
	public String language = "en";				//language code
	public int mood = -1;						//mood tracking of the conversations between user and ILA (I mean SEPIA ;-) )
	public String context = "default";			//context of this command (can be simply the command itself or a modified version of it)
	public String environment = "default";		//environment the server is called from. Apps might expect a different result than browsers, android different than iOS, houses different than cars etc...

	public String cmdSummary = "";				//command summary of this service-request, needed to reconstruct it after client feedback is requested (and other stuff)
	public String responseType = RESPONSE_INFO;	//response type is used on client side to post info/answer/question
	public String inputMiss = "";				//"what did I ask again?" - pass around the missing parameter, hopefully the client sends it back ^^ 
	public int dialogStage = 0;					//dialog_stage as seen in NluInput (only set when modified inside ServiceBuilder) send to client and client should send back
	
	//not (yet) in JSON result included:
	private NluResult nluResult;						//keep a reference to the NluResult for building the ServiceResult
	public ServiceInfo serviceInfo;						//related service info
	public ServiceAnswers serviceAnswers;				//service answers pool defined by service itself
	public JSONObject customInfo = new JSONObject();	//any custom info that should be sent to the interview module (only, for client use "more")
	public Parameter incompleteParameter;				//set a parameter that is incomplete and needs to be asked
	public JSONArray extendedLog = new JSONArray();  	//list to write an extended log that could be sent to the client
	
	/**
	 * Don't use this unless you know what you are doing ;-) 
	 * (e.g. building an action-array)
	 */
	public ServiceBuilder(){}
	/**
	 * Default constructor for a service. Some variables will be set to their proper values here. {@link ServiceInfo} 
	 * will be assumed "unspecified" working stand-alone.
	 * @param nluResult - result coming from NL-Processor
	 */
	public ServiceBuilder(NluResult nluResult){
		
		this(nluResult, new ServiceInfo(Type.unspecified, Content.unspecified, true));
	}
	/**
	 * Default constructor for a service including {@link ServiceInfo}. 
	 * Some variables will be set to their proper values here. 
	 */
	public ServiceBuilder(NluResult nluResult, ServiceInfo serviceInfo){
		
		this(nluResult, serviceInfo, null);
	}
	/**
	 * Default constructor for a service including {@link ServiceInfo} and custom {@link ServiceAnswers}. 
	 * Some variables will be set to their proper values here. 
	 */
	public ServiceBuilder(NluResult nluResult, ServiceInfo serviceInfo, ServiceAnswers customServiceAnswers){
		
		this.language = nluResult.language;
		this.mood = nluResult.mood;
		this.dialogStage = nluResult.input.dialogStage;
		this.context = nluResult.getCommand();
		this.environment = nluResult.environment;
		this.cmdSummary = nluResult.cmdSummary;
		this.resultInfoPut("cmd", nluResult.getCommand());
		
		this.nluResult = nluResult;
		this.serviceInfo = serviceInfo;
		this.serviceAnswers = customServiceAnswers;
	}
	
	/**
	 * Add the service info to the builder (if not done with constructor).
	 */
	public void setServiceInfo(ServiceInfo serviceInfo){
		this.serviceInfo = serviceInfo;
	}
	/**
	 * Add the custom answers pool to the builder (if not done with constructor).
	 */
	public void setServiceAnswers(ServiceAnswers serviceAnswers){
		this.serviceAnswers = serviceAnswers;
	}
	
	//--------- getter methods for SDK ---------
	
	/**
	 * Set service-data to validate access to certain user data (given by user when activating a service).
	 * @param service - active class 
	 * @param serviceName - name of service
	 * @param serviceApiKey - access key given by system
	 */
	public void setDataAccessAuthorization(Class<?> service, String serviceName, String serviceApiKey){
		//TODO: IMPLEMENT
		//check class for ApiInterface
		//service.getSimpleName();
		//...
	}
	/**
	 * Get info of client that is calling the server. (Must be authorized).
	 */
	public String getClientInfo(){
		//TODO: IMPLEMENT API access check
		return nluResult.input.clientInfo;
	}
	/**
	 * Get userId of user that is calling the server. (Must be authorized).
	 */
	public String getUserId(){
		//TODO: IMPLEMENT API access check
		return nluResult.input.user.getUserID();
	}
	
	//------------------------------------------
	
	/**
	 * Service performed all tasks and gave nice result.
	 */
	public void setStatusSuccess(){
		this.status = SUCCESS;
	}
	/**
	 * Some unexpected error occurred.
	 */
	public void setStatusFail(){
		this.status = FAIL;
	}
	/**
	 * Service performed all tasks but gave no result.
	 */
	public void setStatusOkay(){
		this.status = OKAY;
	}
	/**
	 * When the service can't complete within the normal interview-framework you can take control from inside the service. 
	 * Usually you continue from here with "setIncompleteParameter(..)" add a question and return a result.<br>
	 * Instead of using this method you can do all at once with "setIncompleteAndAsk(..)".
	 */
	public void setStatusIncomplete(){
		this.status = INCOMPLETE;
	}
	
	/**
	 * Set status "incomplete" and add the missing "parameter" with it's "question". Finish after this by building and returning the result.
	 * @param parameter - PARAMETERS value
	 * @param question - pool or direct question like "test_0a" or "&lt;direct&gt;what is this?"  
	 */
	public void setIncompleteAndAsk(String parameter, String question){
		setStatusIncomplete();
		Parameter p = new Parameter(parameter);
		p.setQuestion(question);
		setIncompleteParameter(p);
	}
	
	/**
	 * Define the name of an action or parameter that you want to get confirmed by the user. The name can be anything and is only used to 
	 * check the confirmation status later. In detail this method creates a new dynamic confirm-parameter, sets it to incomplete and asks the user for it with the 'question' given.
	 */
	public void confirmActionOrParameter(String actionOrParameterName, String question){
		String dynamicConfirmParameter = Confirm.PREFIX + actionOrParameterName;
		nluResult.addDynamicParameter(dynamicConfirmParameter);
		setIncompleteAndAsk(dynamicConfirmParameter, question);
	}
	/**
	 * Check status of confirmation action or parameter. Returns:<br>
	 * 0 - unchecked<br>
	 * 1 - OK<br>
	 * -1 - NOT OK/CANCEL<br>
	 * @param actionOrParameterName - name defined in 'confirmActionOrParameter(..)'
	 * @return
	 */
	public int getConfirmationStatusOf(String actionOrParameterName){
		String dynamicConfirmParameter = Confirm.PREFIX + actionOrParameterName;
		Parameter confirmP = nluResult.getOptionalParameter(dynamicConfirmParameter, "");
		if (confirmP.isDataEmpty()){
			return 0;
		}else{
			String confirmValue = (String) confirmP.getDataFieldOrDefault(InterviewData.VALUE);
			if (confirmValue.equals(Confirm.OK)){
				return 1;
			}else{
				return -1;
			}
		}
	}
	
	/**
	 * Put "value" to "key" in current actionInfo element. E.g.: putActionInfo("url", call_url). The current JSONArray element (action) is previously
	 * set by using addAction(value), so be sure to first add an action and then add info for that action.<br>
	 * See also: {@link net.b07z.sepia.server.assist.assistant.ActionBuilder}
	 * @param key
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public void putActionInfo(String key, Object value){
		if (actionInfo != null && actionInfo.size()>0){
			JSONObject action = (JSONObject) actionInfo.get(actionInfo.size()-1);
			action.put(key, value);
		}else{
			Debugger.println("ERROR in actionInfo_put_info - add an action first!", 1);
		}
	}
	/**
	 * Add a new action to the queue, to be more specific: create a new JSONObject action, put "value" to "type"-key and add it to the actions array.
	 * Note: the order in which actions are created can matter because clients would usually execute them one after another. 
	 * E.g.: addAction(ACTIONS.OPEN_URL) or addAction(ACTIONS.OPEN_INFO).<br>
	 * See also: {@link net.b07z.sepia.server.assist.assistant.ActionBuilder}
	 * @param key
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public void addAction(Object value){
		JSONObject action = new JSONObject();
		action.put("type", value);
		actionInfo.add(action);
	}
	/**
	 * Remove action at index.
	 */
	public void removeAction(int index){
		actionInfo.remove(index);
	}
	
	/**
	 * Add (overwrite) a key value pair to resultInfo. Each command might have its own characteristic resultInfo.
	 * Usually the resultInfo is used to build the spoken answer and is used in the UI to give an overview
	 * of what has actually been searched.
	 */
	@SuppressWarnings("unchecked")
	public void resultInfoPut(String key, Object value){
		resultInfo.put(key, value);
	}
	/**
	 * Add all result info key-value pairs to this resultInfo. This is usually done when taking a resultInfo from a MASTER service
	 * and add the values to the interview resultInfo.
	 */
	@SuppressWarnings("unchecked")
	public void resultInfoAddAll(JSONObject resultInfo){
		resultInfo.forEach((k, v)->{
			this.resultInfo.put(k, v);
		});
	}
	/**
	 * Fill missing info with blanks. This is a solution/workaround to quickly fill up the answer-parameters that are not needed.
	 */
	public void resultInfoFill(){
		for (String ap : this.serviceInfo.answerParameters){
			if (this.resultInfo.get(ap) == null){
				resultInfoPut(ap, "");
				//resultInfoPut(ap, ap.toUpperCase());
			}
		}
	}
	/**
	 * Get a parameter from the resultInfo (previously added by e.g. "resultInfo_addAll").
	 * @param key - usually a PARAMETERS name or some value given in answerPrameters
	 */
	public Object resultInfoGet(String key){
		return resultInfo.get(key);
	}
	
	/**
	 * Add a tag that describes what has been requested by this service or to be more precise what the service "thinks" has been requested ;-)
	 */
	@SuppressWarnings("unchecked")
	public void addRequestTag(String comment){
		more.put(ServiceBuilder.REQUEST_TAG, comment);
	}
	
	/**
	 * Add a key value pair to customInfo. customInfo can be used to transfer any service specific data to the interview module.
	 * The data stored here is not sent to the client, if you want to do this use the "more" method.
	 * @param key
	 * @param value
	 */
	public void customInfoAdd(String key, Object value){
		JSON.add(customInfo, key, value);
	}
	/**
	 * Use this to add a custom answer at a specific point in the service module if you realize that the default success or fail answer 
	 * do not fit. Has the highest priority that means it overrules all default answers. 
	 * @param customAnswer - answer tag (not the name but the tag! you used in {@link ServiceInfo}) or "&ltdirect&gtanswer"
	 */
	public void setCustomAnswer(String customAnswer){
		customInfoAdd("customAnswer", customAnswer);
	}
	/**
	 * Use this to tell the interview module that you are missing information. The incomplete parameter can be one that is known
	 * by the system or a custom parameter. If you use a known one make sure you don't use it twice in the service!
	 */
	public void setIncompleteParameter(Parameter p){
		this.incompleteParameter = p;
		this.nluResult.removeFinal(p.getName());
		//auto-set status here?
	}
	/**
	 * Add a key value pair to "more". It can be used to transfer any service specific data to the client.
	 * If you want to store data that is only required in the interview module use "customInfo".
	 * @param key
	 * @param value
	 */
	public void addMore(String key, Object value){
		JSON.add(more, key, value);
	}
	
	/**
	 * Add a message to the extended log. Extended log will be added to "more" if its not empty and if the service is not public.
	 * This is supposed to be used to debug services under development.
	 * @param logMessage - message to log.
	 */
	public void addToExtendedLog(String logMessage){
		JSON.add(extendedLog, logMessage);
	}
	
	/**
	 * Set or overwrite a certain parameter and reconstruct cmd_summary if command_type is set.
	 * @param parameter - parameter to set or overwrite
	 * @param new_value - new value for parameter. Note: has to be valid input for parameter build method!
	 */
	public void overwriteParameter(String parameter, String new_value){
		nluResult.setParameter(parameter, new_value);
		cmdSummary = nluResult.cmdSummary;
	}
	/**
	 * Remove a certain parameter and reconstruct cmd_summary if command_type is set.
	 * @param parameter - parameter to remove
	 */
	public void removeParameter(String parameter){
		nluResult.removeParameter(parameter);
		cmdSummary = nluResult.cmdSummary;
	}
	
	/**
	 * Set the JSONArray collection of cards to this value.
	 * @param cardInfo - a collection of cards (pre-Alpha: a collection of card elements)
	 */
	public void setCard(JSONArray cardInfo){
		this.cardInfo = cardInfo;
	}
	/**
	 * Add a card to the JSONArray collection of cards for this result.
	 */
	public void addCard(JSONObject card){
		JSON.add(cardInfo, card);
	}
	/**
	 * Add a card to the JSONArray collection of cards for this result.
	 */
	public void addCard(Card card){
		JSON.add(cardInfo, card.getJSON());
	}
	
	/**
	 * Change response type to "Question" and set missing input and dialog stage. 
	 * @param missing_input_param - what is missing? (PARAMETER.xyz ...)
	 */
	public void makeThisAQuestion(String missing_input_param){
		responseType = ServiceBuilder.RESPONSE_QUESTION;		//response type is used on client side to post info/answer question/execute command
		inputMiss = missing_input_param;			//"what did I ask again?" - pass around the missing parameter (search, type, start, end, etc...), hopefully the client sends it back ^^
		nluResult.input.inputMiss = missing_input_param;
		//overwrite wrong answer but make an exception for OPEN_LINK commands 'cause they need it
		if (!nluResult.getCommand().equals(CMD.OPEN_LINK)){ 
			nluResult.setParameter(missing_input_param, ""); 		//overwrite wrong answer if there was one
		}
		if (!nluResult.input.isRepeatedAnswer(missing_input_param)){
			dialogStage++;							//get dialogue stage and increase by 1 if this is a "new" question (not repeated)
		}
		//System.out.println("N=" + nlu_result.input.last_cmd_N + ", DS=" + dialog_stage); 	//debug
	}	
	
	/**
	 * Run a task in the background, optionally with a delay.
	 * @param delayMs - start after this many ms
	 * @param task - use it like this: () -> { my code... }
	 */
	public void runInBackground(long delayMs, Runnable task){
		int corePoolSize = 1;
	    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(corePoolSize);
	    executor.schedule(task, delayMs, TimeUnit.MILLISECONDS);
		//other option (but does not support lambda expression):
		//Timer timer = new Timer();
	    //timer.schedule(task, delayMs);
	}
	
	/**
	 * WebSockets support duplex communication which means you can send an answer first and after a few seconds send a follow-up
	 * message to add more info/data to the previous reply. Test for nluInput.isDuplexConnection() first!
	 * @param nluInput - initial {@link NluInput} to follow-up
	 * @param serviceResult - {@link ServiceResult} as produced by services to send as follow-up
	 */
	public boolean sendFollowUpMessage(NluInput nluInput, ServiceResult serviceResult){
		return Clients.sendAssistantFollowUpMessage(nluInput, serviceResult);
	}
	
	/**
	 * Build {@link ServiceResult} from the info in this class. Handles some specific procedures like context management to generate all necessary info.
	 * 
	 * @return {@link ServiceResult} to be sent to user client.
	 */
	public ServiceResult buildResult(){
		//put some Stuff in more
		JSON.add(more, "language", language);
		JSON.add(more, "context", Assistant.addContext(context, nluResult));
		JSON.add(more, "cmd_summary", cmdSummary);
		JSON.add(more, "certainty_lvl", nluResult.getCertaintyLevel());
		if (mood >= 0){
			JSON.add(more, "mood", String.valueOf(mood));
		}
		//add the user, it's handy for chat apps
		JSON.add(more, "user", nluResult.input.user.getUserID());
		
		//check clean answer - NOTE: this is usually done in the AbstractInterview handler
		if (serviceInfo.worksStandalone){
			if (!answer.isEmpty() && answerClean.isEmpty()){
				answerClean = Converters.removeHTML(answer);
			}else if (answerClean.equals("<silent>")){
				answerClean = "";
			}
		}
		//TODO: but we might want to do this:
		resultInfoFill();
		
		//finally build the API_Result
		ServiceResult result = new ServiceResult(status, answer, answerClean, htmlInfo, cardInfo, actionInfo, hasInfo, hasCard, hasAction);
		
		//add ServiceInfo and ServiceAnswers - note: not included in JSON result (yet?)
		result.serviceInfo = serviceInfo;
		result.serviceAnswers = serviceAnswers;
		
		//add resultInfo
		/* sanity check
		if (apiInfo.answerParameters.size() > 0){
			for (String ap : apiInfo.answerParameters){
				if (!resultInfo.containsKey(ap)){
					throw new RuntimeException("API - resultInfo is missing answerParameters: " + ap);
				}
			}
		}*/
		result.resultInfo = resultInfo;
		JSON.add(result.resultJson, "resultInfo", resultInfo);
		
		//add customInfo
		result.customInfo = customInfo;
		
		//add incomplete parameter
		result.incompleteParameter = incompleteParameter;
		
		//add extended log
		result.extendedLog = extendedLog;
		if (!serviceInfo.makePublic && !extendedLog.isEmpty()){
			JSON.add(more, "extendedLog", extendedLog);
		}
		
		//add more - cause it is not in the constructor for API_Result
		if (!more.isEmpty()){
			result.more = more;
			JSON.add(result.resultJson, "more", more);
		}
		//check response type
		if (responseType.matches(RESPONSE_QUESTION)){
			//ask-specific parameters
			result.responseType = responseType;
			result.inputMiss = inputMiss;
			result.dialogStage = dialogStage;
			JSON.add(result.resultJson, "response_type", responseType);
			JSON.add(result.resultJson, "input_miss", inputMiss);
			JSON.add(result.resultJson, "dialog_stage", new Integer(dialogStage)); 	//should track this always?
		}
		
		return result;
	}

}
