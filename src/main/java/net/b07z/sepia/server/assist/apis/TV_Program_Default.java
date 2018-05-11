package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * TV Program default API.
 * 
 * @author Florian Quirin
 *
 */
public class TV_Program_Default implements ApiInterface{
	
	//--- data ---
	public static String getButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "Fernsehzeitung öffnen";
		}else{
			return "Open tv paper";
		}
	}
	//-------------
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.link, Content.redirect, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//get parameters
		String channel = NLU_result.getParameter("channel");
		String time = NLU_result.getParameter(PARAMETERS.TIME);
		Debugger.println("cmd: tv_program, channel " + channel + ", time " + time, 2);		//debug
		
		if (channel.isEmpty()){
			channel = "default";
		}
		
		//make answer - if more than one direct answer choose randomly
		api.answer = Config.answers.getAnswer(NLU_result, "tv_program_1a", channel, time);
		api.answer_clean = Converters.removeHTML(api.answer);
		
		//make action: browser url call
		String call_url = "";
		if (time.matches("(heute abend|tonight|this evening)")){
			//call_url = "http://www.tvspielfilm.de/tv-programm/sendungen/abends.html";
			call_url = "http://m.tvspielfilm.de/2015.html";
		}else if (time.matches("(heute nacht|night)")){
			//call_url = "http://www.tvspielfilm.de/tv-programm/sendungen/fernsehprogramm-nachts.html";
			call_url = "http://m.tvspielfilm.de/2200.html";
		}else if (time.matches("(morgen abend|tomorrow night|tomorrow|morgen)")){
			String date = DateTimeConverters.getTomorrow("yyyy-MM-dd", NLU_result.input);
			if (!date.isEmpty()){
				//call_url = "http://www.tvspielfilm.de/tv-programm/sendungen/?order=time&date=2015-11-02&time=20&channel=g%3A1";
				call_url = "http://m.tvspielfilm.de/suche.html?date=" + date + "&time=prime&channel=g%3A1&category=alle";
			}else{
				call_url = "http://m.tvspielfilm.de/tv-programm/";
			}
		}else{
			//call_url = "http://www.tvspielfilm.de/tv-programm/sendungen/jetzt.html";
			call_url = "http://m.tvspielfilm.de/tv-programm/";
		}
		
		//actions:
		api.actionInfo_add_action(ACTIONS.OPEN_IN_APP_BROWSER);
		api.actionInfo_put_info("url", call_url);
		
		api.actionInfo_add_action(ACTIONS.BUTTON_IN_APP_BROWSER);
		api.actionInfo_put_info("url", call_url); 
		api.actionInfo_put_info("title", getButtonText(api.language));
		
		api.hasAction = true;
		
		//build card
		/*
		Card card = new Card();
		String card_text = "<b>TV Program</b><br><br>" + "<b>"+ time +"</b>";
		String card_img = Config.url_web_images + "tv-spielfilm-logo.png";
		card.addElement(card_text, call_url, card_img);
		//add it
		api.cardInfo = card.cardInfo;
		api.hasCard = false;
		*/
		
		//build html
		if (CLIENTS.hasWebView(NLU_result.input.client_info)){
			api.htmlInfo = "<object type='text/html' style='width: 100%; height: 100%; overflow-y: hidden; background-color: #e8e8e8;' data='" + call_url + "'></object>";
			//api.htmlInfo = "<iframe style='width: 100%; height: 400%; overflow-y: hidden; background-color: #FFF;' src='" + call_url + "'></iframe>";
		}else{
			api.htmlInfo = call_url;
		}
		api.hasInfo = true;	
		
		api.status = "success";
				
		//finally build the API_Result
		ApiResult result = api.build_API_result();
				
		//return result_JSON.toJSONString();
		return result;
		
	}

}
