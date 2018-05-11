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
import net.b07z.sepia.server.assist.parameters.DateAndTime;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Location searching via Maps.
 * 
 * @author Florian Quirin
 *
 */
public class Hotels_Expedia implements ApiInterface{
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.link, Content.redirect, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//get parameters
		String place = NLU_result.getParameter(PARAMETERS.PLACE);
		String date_s = NLU_result.getParameter(PARAMETERS.TIME);
		String date_e = NLU_result.getParameter(PARAMETERS.TIME_END);
				
		//TODO: fully implement dates
		date_s = DateAndTime.convertTagToDate(date_s, "", NLU_result.input)[0];
		date_s = DateTimeConverters.convertDateFormat(date_s, "yyyy.MM.dd_HH:mm:ss", "dd.MM.yyyy");
		date_e = DateAndTime.convertTagToDate(date_e, "", NLU_result.input)[0];
		date_e = DateTimeConverters.convertDateFormat(date_e, "yyyy.MM.dd_HH:mm:ss", "dd.MM.yyyy");
		if (date_s.isEmpty()){
			//today and tomorrow
			date_s = DateTimeConverters.getToday("dd.MM.yyyy", NLU_result.input);
			date_e = DateTimeConverters.getTomorrow("dd.MM.yyyy", NLU_result.input);
		}else if (date_e.isEmpty()){
			//get start +1 day
			String d_i = DateTimeConverters.convertDateFormat(date_s, "dd.MM.yyyy", Config.defaultSdf);
			date_e = DateTimeConverters.getDate_plus_X_minutes("dd.MM.yyyy", d_i, 60*24); 		//start +1 day
		}
		
		Debugger.println("cmd: hotels, place=" + place + ", time_s=" + date_s + ", time_e=" + date_e, 2);		//debug
		
		String[] user_spec_location;
		String place_param;
		
		user_spec_location = LOCATION.getUserSpecificLocation_4_Weather(NLU_result.input.user, place);
		place_param = user_spec_location[0];
				
		String place_to_say = place;
		//check place for personal locations
		if (!place_param.isEmpty()){
			place = user_spec_location[2];			//using no coordinates - is it possible with Expedia?
			place_to_say = user_spec_location[2];
			//still empty? then say that and maybe ask the user to add the info to the account
			if (place.isEmpty() || place_to_say.isEmpty()){
				if (LOCATION.canBeAddedToAccount(place_param)){
					//return No_Result.get(NLU_result, "default_miss_info_0b");
					NLU_result.setParameter(PARAMETERS.TYPE, "addresses");
					NLU_result.setParameter(PARAMETERS.ACTION, "add");
					return ConfigServices.dashboard.getResult(NLU_result);
				}else{
					return NoResult.get(NLU_result, "default_miss_info_0a");
				}
			}
		}
		
		//check again
		if (place.isEmpty()){
			return AskClient.question("hotels_ask_place_0a", PARAMETERS.PLACE, NLU_result);
		}else{
			/* TODO: check for individual locations
			String personal_end = User.containsPersonalUserInfo(place, NLU_result.input.user)[1];
			if (!personal_end.isEmpty()){
				place = personal_end;
				place_to_say = personal_end;
			}
			*/
		}
		
		//get answer
		api.answer = Config.answers.getAnswer(NLU_result, "hotels_1a", place_to_say);
		api.answer_clean = Converters.removeHTML(api.answer);
		
		//make action: browser url call
		api.hasAction = true;
		String url;
		try {
			url = "https://www.expedia.de/Hotel-Search?#" +
					"&destination=" + URLEncoder.encode(place, "UTF-8") + 
					"&startDate=" + URLEncoder.encode(date_s, "UTF-8") + 
					"&endDate=" + URLEncoder.encode(date_e, "UTF-8");
					
		} catch (UnsupportedEncodingException e) {
			url = "https://www.expedia.de";
			//e.printStackTrace();
		}
		
		//make action: browser url call
		if (CLIENTS.hasWebView(NLU_result.input.client_info)){
			//api.actionInfo_add_action(ACTIONS.OPEN_URL);
			//api.actionInfo_put_info("url", url);
			api.actionInfo_add_action(ACTIONS.OPEN_INFO);
			api.hasAction = true;
		}
				
		//build card
		Card card = new Card();
		String card_text = "<b>Hotels</b><br>" 	+ "<br><b>Place:</b> " + place_to_say 
												+ "<br><b>Check-In:</b> " + date_s
												+ "<br><b>Check-Out:</b> " + date_e;
		String card_img = Config.urlWebImages + "www-logo.png";
		card.addElement(card_text, url, card_img);
		//add it
		api.cardInfo = card.cardInfo;
		api.hasCard = true;
		
		//build html
		if (CLIENTS.hasWebView(NLU_result.input.client_info)){
			api.htmlInfo = "<object type='text/html' style='width: 100%; height: 300%; overflow-y: hidden;' data='" + url + "'></object>";
		}else{
			api.htmlInfo = url;
		}
		api.hasInfo = true;		
		
		//done
		api.status = "success";
				
		//finally build the API_Result
		ApiResult result = api.build_API_result();
				
		//return result_JSON.toJSONString();
		return result;
	}

}
