package net.b07z.sepia.server.assist.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * German/English dictionary API by Linguee and partially others.
 * 
 * @author Florian Quirin
 *
 */
public class DictionaryTranslateBasic implements ServiceInterface{
	
	//--- data ---
	public static String getButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "Wörterbuch öffnen";
		}else{
			return "Open dictionary";
		}
	}
	//-------------
	
	//info
	public ServiceInfo getInfo(String language){
		return new ServiceInfo(Type.link, Content.redirect, true);
	}

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		//get parameters
		String search = nluResult.getParameter(PARAMETERS.SEARCH);
		String target_lang = nluResult.getParameter(PARAMETERS.LANGUAGE);
		Debugger.println("cmd: dict_translate, search " + search + ", target_lang=" + target_lang, 2);		//debug
		
		//check'em
		if (search.isEmpty()){
			return AskClient.question("dict_translate_ask_0a", "search", nluResult);
		}
		if (target_lang.isEmpty()){
			//set default target language
			if (nluResult.language.matches("en")){
				target_lang = "de";
			}else{
				target_lang = "en";
			}
		}
		
		String supported_languages = "(de|en|tr)";		//add languages here when adding more target languages
		
		//make answer - if more than one direct answer choose randomly
		if (target_lang.matches(supported_languages)){
			//supported language
			api.answer = Answers.getAnswerString(nluResult, "dict_translate_1a", search, target_lang);
		}else{
			//unsupported target language
			api.answer = Answers.getAnswerString(nluResult, "dict_translate_1b", search, target_lang);
		}
		api.answerClean = Converters.removeHTML(api.answer);
		
		//make action: browser url call
		String call_url = "";
		if (target_lang.matches("(de|en)")){
			try {
				call_url = "http://m.linguee.de/deutsch-englisch/search?source=auto&query=" + URLEncoder.encode(search, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				call_url = "http://m.linguee.de/deutsch-englisch/";
			}
		}else if (target_lang.matches("(tr)")){
			try {
				//call_url = "http://detr.dict.cc/?s=" + URLEncoder.encode(search, "UTF-8");
				call_url = "http://www.seslisozluk.net/de/was-bedeutet-" + URLEncoder.encode(search, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				call_url = "http://detr.dict.cc";
			}
		}else{
			call_url = "http://m.linguee.de/";
		}
		
		//action - for supported languages
		if (target_lang.matches(supported_languages)){
			api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
			api.putActionInfo("url", call_url);

			api.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
			api.putActionInfo("url", call_url); 
			api.putActionInfo("title", getButtonText(api.language));
			
			api.hasAction = true;
		}
		
		//build card
		/*
		Card card = new Card();
		String card_text = "<b>Dictionary</b><br><br>" + "<b>Search: </b>"+ search; //+"<br>" + "<b>Language: </b>" + target_lang;
		String card_img = Config.url_web_images + "linguee-logo.png";
		card.addElement(card_text, call_url, card_img);
		//add it
		api.cardInfo = card.cardInfo;
		api.hasCard = false;
		*/
		
		//build html
		if (CLIENTS.hasWebView(nluResult.input.clientInfo)){
			api.htmlInfo = "<object type='text/html' style='width: 100%; height: 400%; overflow-y: hidden;' data='" + call_url + "'></object>";
		}else{
			api.htmlInfo = call_url;
		}
		api.hasInfo = true;	
		
		api.status = "success";
				
		//finally build the API_Result
		ServiceResult result = api.buildResult();
				
		//return result_JSON.toJSONString();
		return result;
	}

}
