package net.b07z.sepia.server.assist.apis;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.interviews.NoResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Mobility searching via Qixxit.
 * 
 * @author Florian Quirin
 *
 */
public class Mobility_Qixxit implements ApiInterface {
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.link, Content.redirect, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//get parameters
		String start = NLU_result.getParameter(PARAMETERS.LOCATION_START);
		String end = NLU_result.getParameter(PARAMETERS.LOCATION_END);
		String type = NLU_result.getParameter(PARAMETERS.TYPE);
		String time = NLU_result.getParameter(PARAMETERS.TIME);
		Debugger.println("cmd: mobility, from " + start + ", to " + end + ", by " + type + ", at " + time, 2);		//debug
		
		//check time and convert
		String date;
		String today = DateTimeConverters.getToday("dd.MM.yy", NLU_result.input);
		String clock = DateTimeConverters.getToday("HH:mm", NLU_result.input);
		if (!time.isEmpty()){
			//date = NLU_parameter_search.convert_date(time, "dd.MM.yy", NLU_result.input);
			//TODO: broken
			date = today;
			if (!date.equals(today)){
				clock = "06:00";		//default time for other days
			}
		//ASK?
		}else{
			date = today;
		}
		
		//check for user specific locations
		//end
		String[] user_spec_location = LOCATION.getUserSpecificLocation_4_Mobility(NLU_result.input.user, end);
		String end_param = user_spec_location[0];
		String end_to_say = end;
		if (!end_param.isEmpty()){
			end = user_spec_location[1];
			end_to_say = user_spec_location[2];
			//still empty? then say that and maybe ask the user to add the info to the account
			if (end.isEmpty() || end_to_say.isEmpty()){
				if (LOCATION.canBeAddedToAccount(end_param)){
					//return No_Result.get(NLU_result, "default_miss_info_0b");
					NLU_result.setParameter(PARAMETERS.TYPE, "addresses");
					NLU_result.setParameter(PARAMETERS.ACTION, "add");
					return ConfigServices.dashboard.getResult(NLU_result);
				}else{
					return NoResult.get(NLU_result, "default_miss_info_0a");
				}
			}
		}
		//start
		user_spec_location = LOCATION.getUserSpecificLocation_4_Mobility(NLU_result.input.user, start);
		String start_param = user_spec_location[0];
		String start_to_say = start;
		if (!start_param.isEmpty()){
			start = user_spec_location[1];
			start_to_say = user_spec_location[2];
			//still empty? then say that and maybe ask the user to add the info to the account
			if (start.isEmpty() || start_to_say.isEmpty()){
				if (LOCATION.canBeAddedToAccount(start_param)){
					//return No_Result.get(NLU_result, "default_miss_info_0b");
					NLU_result.setParameter(PARAMETERS.TYPE, "addresses");
					NLU_result.setParameter(PARAMETERS.ACTION, "add");
					return ConfigServices.dashboard.getResult(NLU_result);
				}else{
					return NoResult.get(NLU_result, "default_miss_info_0a");
				}
			}
		}
		
		//check requirements again!
		if (end.matches("")){
			return AskClient.question("directions_ask_end_0a", PARAMETERS.LOCATION_END, NLU_result);
		}
		//if there is and end but no start (not even "here" or "<user_location>", this can only happen by direct commands)
		else if (start.matches("")){
			return AskClient.question("directions_ask_start_0a", PARAMETERS.LOCATION_START, NLU_result);
		}
		
		//make answer - if more than one direct answer choose randomly
		api.answer = Config.answers.getAnswer(NLU_result, "mobility_1a", start_to_say, end_to_say, type, time);
		api.answerClean = Converters.removeHTML(api.answer);
		
		//make action: browser url call
		String call_url = "";
		try {
			call_url = "https://www.qixxit.de" + 
						"?&S=" + URLEncoder.encode(start, "UTF-8") + //"&SADR=1" + 
						"&Z=" + URLEncoder.encode(end, "UTF-8") + //"&ZADR=1" + 
						"&date=" + URLEncoder.encode(date, "UTF-8") +
						"time=" + URLEncoder.encode(clock, "UTF-8") +
						"&getstop=1";
			//date (optional) TT.MM.JJ
			//time (optional) HH:MM
		
		} catch (UnsupportedEncodingException e) {
			call_url = "https://www.qixxit.de";
			//e.printStackTrace();
		}
		//deactivated:
		//api.actionInfo_put_type(ACTIONS.OPEN_URL);
		//api.actionInfo_put_info("url", call_url);
		api.addAction(ACTIONS.OPEN_INFO);		//this is an action only used in small clients that cannot display all content simultaneously  
		api.hasAction = true;
		
		//build card
		Card card = new Card();
		String card_text = "<b>Qixxit</b><br><br>" + "<b>Start:</b> " + start_to_say + "<br><b>End:</b> " + end_to_say;
		String card_img = Config.urlWebImages + "qixxit-logo.png";
		card.addElement(card_text, call_url, card_img);
		//add it
		api.cardInfo = card.cardInfo;
		if (api.cardInfo.isEmpty()){
			api.hasCard = false;
		}else{
			api.hasCard = true;
		}
		
		//build html
		if (CLIENTS.hasWebView(NLU_result.input.clientInfo)){
			api.htmlInfo = "<object type='text/html' style='width: 100%; height: 100%; overflow-y: hidden;' data='" + call_url + "'></object>";
		}else{
			api.htmlInfo = call_url;
		}
		api.hasInfo = true;	
		
		api.status = "success";		//kind of success ^^
				
		//finally build the API_Result
		ApiResult result = api.buildApiResult();
				
		//return result_JSON.toJSONString();
		return result;
		
	}

}
