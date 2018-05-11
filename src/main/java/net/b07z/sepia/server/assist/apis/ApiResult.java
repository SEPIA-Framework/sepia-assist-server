package net.b07z.sepia.server.assist.apis;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * API_Result is the class used for results produced by APIs (who would have guessed ^^).
 * It stores at least a short answer in a specific language. Optimally it also comes with additional HTML info
 * and a compact info element for a "cards" or individual style representation of the answer.<br>
 * Preferred constructor is:<br> API_Result(status, answer, answer_clean, htmlInfo, cardInfo, hasInfo, hasCard, result_JSON)
 * <br>More parameters can be stored in the "more" HashMap<String, String><br>
 * A JSON string representation can be obtained using get_result_JSON(). 
 *   
 * @author Florian Quirin
 *
 */
public class ApiResult {
	
	String answer = "";							//spoken answer (can include html code like links, spans, line break etc.)
	String answerClean = "";					//clean spoken answer (without html code)
	String htmlInfo = "";						//extended html content to answer (e.g. for info center) - TODO: implement default classes e.g. <div class="b07z_header"> or so
	JSONArray cardInfo = new JSONArray();		//compact info for "cards"
	JSONArray actionInfo = new JSONArray();		//action parameters send to browser (actionType: open_link/execute/open_app ..., info: ...)
	JSONObject resultInfo = new JSONObject();	//common result info, important for displaying result view details 
	boolean hasInfo = false;					//is extended html content available?
	boolean hasCard = false;					//is content for a "card" available?
	boolean hasAction = false;					//is there an extra command for the client?
	String status = "still_ok";					//result status (fail/success/still_ok) - fail is for serious errors without planned answer, success is for perfect answer as planned, still_ok is with answer but no result
	JSONObject more;							//flexible, additional parameters (more stuff I most likely forgot to implement right now or that is different from API to API)
	
	public JSONObject result_JSON = new JSONObject();	//JSON result returned to client. Attention: this is updated directly sometimes and may include more info than the variables itself :/
														//usually this is build by API.java
	//included in more JSON on return
	String language = "en";
	String context = "default";
	int mood = -1;
	
	String responseType = "info";				//response type is used on client side to post info/answer question/action command
	String inputMiss = "";						//"what did I ask again?" - pass around the missing parameter, hopefully the client sends it back ^^ 
	int dialogStage = 0;						//dialog_stage as seen in NLU_Input (only set when modified inside API) send to client and client should send back
	
	//not (yet) in JSON result included:
	String environment = "default";
	ApiInfo apiInfo;
	JSONObject customInfo = new JSONObject();	//any custom info that should be sent to the interview module (only, for client use "more")
	Parameter incompleteParameter;				//a parameter that is incomplete and needs to be asked
	JSONArray extendedLog = new JSONArray();  	//list to write an extended log that could be sent to the client
	
	/**
	 * Default constructor. Don't use this one unless u know what you are doing ^^. Use either:
	 * <p>API_Result(status, answer, htmlInfo, hasInfo, result_JSON) or<br>
	 * API_Result(status, answer, answer_clean, htmlInfo, cardInfo, hasInfo, hasCard, result_JSON)</p>
	 */
	public ApiResult(){
	}
	/**
	 * The very basic stuff every API needs to implement
	 * @param status - did the API deliver a result (fail/success)
	 * @param answer - answer the assistant can speak (can include HTML code, client needs to filter)
	 * @param htmlInfo - extended HTML code for e.g. info center to give a nice presentation of the search result
	 * @param hasInfo - indicates if extended HTML is available (true/false)
	 * @param resulJSON - result as JSON object to send to client
	 */
	public ApiResult (String status, String answer, String htmlInfo, boolean hasInfo, JSONObject resulJSON){
		this.status = status;
		this.answer = answer;
		this.htmlInfo = htmlInfo;
		this.hasInfo = hasInfo;
		this.result_JSON = resulJSON;
	}
	/**
	 * In addition to the basic constructor this one includes a clean answer and an additional "cards" JSON Object 
	 * which can be used to build compact info-cards inside the client.
	 * @param status - did the API deliver a result (fail/success)
	 * @param answer - answer the assistant can speak (can include HTML code, client needs to filter)
	 * @param answer_clean - clean answer without any HTML code (for user convenience) 
	 * @param htmlInfo - extended HTML code for e.g. info center to give a nice presentation of the search result
	 * @param cardInfo - compact "cards" info stored as JSON Array (usually with up to 3 items)
	 * @param hasInfo - indicates if extended HTML is available (true/false)
	 * @param hasCard - is compact "cards" info available (true/false)
	 * @param result_JSON - result as JSON object to send to client
	 */
	@SuppressWarnings("unchecked")
	public ApiResult (String status, String answer, String answer_clean, String htmlInfo, JSONArray cardInfo, boolean hasInfo, boolean hasCard){
		this.status = status;
		this.answer = answer;
		this.answerClean = answer_clean;
		this.htmlInfo = htmlInfo;
		this.cardInfo = cardInfo;
		this.hasInfo = hasInfo;
		this.hasCard = hasCard;
		//create JSON result
		result_JSON.put("result", status);
		result_JSON.put("answer", answer);
		result_JSON.put("answer_clean", answer_clean);
		result_JSON.put("hasInfo", new Boolean(hasInfo));
		result_JSON.put("htmlInfo", htmlInfo);
		result_JSON.put("hasCard", new Boolean(hasCard));
		result_JSON.put("cardInfo", cardInfo);
		result_JSON.put("hasAction", new Boolean(hasAction));
		result_JSON.put("actionInfo", actionInfo);
	}
	/**
	 * In addition to the basic constructor this one includes a clean answer, an additional "cards" JSON Object 
	 * which can be used to build compact info-cards inside the client and information on "actions" send back to the client.
	 * @param status - did the API deliver a result (fail/success)
	 * @param answer - answer the assistant can speak (can include HTML code, client needs to filter)
	 * @param answer_clean - clean answer without any HTML code (for user convenience) 
	 * @param htmlInfo - extended HTML code for e.g. info center to give a nice presentation of the search result
	 * @param cardInfo - compact "cards" info stored as JSON Array (usually with up to 3 items)
	 * @param actionInfo - JSON object that can include additional info about actions send to the client (action type, info ...)
	 * @param hasInfo - indicates if extended HTML is available (true/false)
	 * @param hasCard - is compact "cards" info available (true/false)
	 * @param hasAction - is there an "action" request to the client like "open an app" etc.? 
	 * @param result_JSON - result as JSON object to send to client
	 */
	@SuppressWarnings("unchecked")
	public ApiResult (String status, String answer, String answer_clean, String htmlInfo, JSONArray cardInfo, JSONArray actionInfo, boolean hasInfo, boolean hasCard, boolean hasAction){
		this.status = status;
		this.answer = answer;
		this.answerClean = answer_clean;
		this.htmlInfo = htmlInfo;
		this.cardInfo = cardInfo;
		this.actionInfo = actionInfo;
		this.hasInfo = hasInfo;
		this.hasCard = hasCard;
		//create JSON result
		result_JSON.put("result", status);
		result_JSON.put("answer", answer);
		result_JSON.put("answer_clean", answer_clean);
		result_JSON.put("hasInfo", new Boolean(hasInfo));
		result_JSON.put("htmlInfo", htmlInfo);
		result_JSON.put("hasCard", new Boolean(hasCard));
		result_JSON.put("cardInfo", cardInfo);
		result_JSON.put("hasAction", new Boolean(hasAction));
		result_JSON.put("actionInfo", actionInfo);
	}
	
	/**
	 * Returns the status of the service result. Can be "success" for perfect answer, "still_ok" for planned answer for non-desired result 
	 * or "fail" for errors or other unplanned aborts.
	 */
	public String getStatus(){
		return status;
	}
	/**
	 * Was the service call a success with a nice answer?
	 */
	public boolean isSuccess(){
		return status.equals("success");
	}
	/**
	 * Was the service call a success but no answer has been found?
	 */
	public boolean isOkay(){
		return status.equals("still_ok");
	}
	
	/**
	 * Get JSON result as string. Note: the JSON result is actually already built in API.build... so if you change variables 
	 * of the API_Result this will have no effect anymore on the JSON :(. To make things a bit more safe the 3 basic data fields
	 * are updated once more before writing the result. 
	 */
	public String getResultJSON(){
		//the real info of this class is actually in the JSON as this is not generated here but the JSON itself is updated :(
		// ... oh crimes of my youth ...
		//TODO: make sure to update the class as well and not only the JSON
		//to make things a bit easier update the 3 basics data fields again
		JSON.add(result_JSON, "hasInfo", new Boolean(!htmlInfo.isEmpty()));
		JSON.add(result_JSON, "htmlInfo", htmlInfo);
		JSON.add(result_JSON, "hasCard", new Boolean(cardInfo != null && !cardInfo.isEmpty()));
		JSON.add(result_JSON, "cardInfo", cardInfo);
		JSON.add(result_JSON, "hasAction", new Boolean(actionInfo != null && !actionInfo.isEmpty()));
		JSON.add(result_JSON, "actionInfo", actionInfo);
		
		return result_JSON.toJSONString();
	}
	
	/**
	 * Get answer to be spoken to user.
	 * @return
	 */
	public String getAnswerString(){
		return answer;
	}
	/**
	 * Get clean (no HTML) answer to be spoken to user.
	 * @return
	 */
	public String getAnswerStringClean(){
		return answerClean;
	}
	
	/**
	 * Get card info array.
	 */
	public JSONArray getCardInfo(){
		return cardInfo;
	}
	
	/**
	 * Get HTML info block.
	 */
	public String getHtmlInfo(){
		return htmlInfo;
	}
	
	/**
	 * Get array with actions.
	 */
	public JSONArray getActionInfo(){
		return actionInfo;
	}
	
	/**
	 * Get the response type of this result ("question", "info", ...).
	 */
	public String getResponseType(){
		return responseType;
	}
	/**
	 * Get the missing input if there is one.
	 */
	public String getMissingInput(){
		return inputMiss;
	}
	
	/**
	 * Get the "more" map that includes changes assistant state parameters like mood, cmd_summary, context, etc. ...
	 */
	public JSONObject getMore(){
		return more;
	}
	
	/**
	 * Get result info.
	 */
	public JSONObject getResultInfo(){
		return resultInfo;
	}
	
	/**
	 * Get the custom info stored under "key" as object.
	 */
	public Object getCustomInfo(String key){
		return customInfo.get(key);
	}
	/**
	 * Not to be confused with "getAnswerString()". This method returns a blank answer tag (or direct answer) that can be used
	 * to fill in the answerParameters. It is used inside service modules when the default answers do not fit. The interview
	 * module handles this automatically so usually you don't need this.
	 */
	public String getCustomAnswerWorkpiece(){
		Object ca = getCustomInfo("customAnswer");
		if (ca != null){
			return (String) ca;
		}else{
			return "";
		}
	}
	
	/**
	 * If the status of the result is "incomplete" than this is the parameter that is missing.
	 */
	public Parameter getIncompleteParameter(){
		return incompleteParameter;
	}
	
	/**
	 * Get API_Info used to create this result.
	 */
	public ApiInfo getApiInfo(){
		return apiInfo;
	}
}
