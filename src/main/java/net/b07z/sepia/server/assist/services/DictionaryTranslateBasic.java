package net.b07z.sepia.server.assist.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * German/English dictionary API by Linguee/DeepL and some others.
 * 
 * @author Florian Quirin
 *
 */
public class DictionaryTranslateBasic implements ServiceInterface{
	
	//TODO: update this service with 2.0 syntax and methods ...
	//TODO: do something with 'switch language'-action
	
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
		
		/* this will not work due to stand-alone mode of service ...
		Parameter searchP = nluResult.getOptionalParameter(PARAMETERS.SEARCH, "");
		String search = searchP.getDataFieldOrDefault(InterviewData.VALUE).toString();
		Parameter targetLangP = nluResult.getOptionalParameter(PARAMETERS.LANGUAGE, nluResult.input.language);
		String targetLang = targetLangP.getDataFieldOrDefault(InterviewData.VALUE).toString().replaceFirst("-.*", "").trim();
		*/
		String search = nluResult.getParameter(PARAMETERS.SEARCH);
		String targetLang = nluResult.getParameter(PARAMETERS.LANGUAGE);
		Debugger.println("cmd: dict_translate, search " + search + ", target_lang=" + targetLang, 2);		//debug
		
		//check'em
		if (search.isEmpty()){
			return AskClient.question("dict_translate_ask_0a", "search", nluResult);
		}
				
		String supportedLanguages = "(de|en|tr|es|fr)";		//add languages here when adding more target languages
		
		//make answer - if more than one direct answer choose randomly
		if (targetLang.matches(supportedLanguages)){
			//supported language
			api.answer = Answers.getAnswerString(nluResult, "dict_translate_1a", search, targetLang);
		}else{
			//unsupported target language
			api.answer = Answers.getAnswerString(nluResult, "dict_translate_1b", search, targetLang);
		}
		api.answerClean = Converters.removeHTML(api.answer);
		
		//make action: browser URL call
		String call_url = "";
		int numOfWords = NluTools.countWords(search);
		
		//German and English
		if ((targetLang.equals(LANGUAGES.DE) || targetLang.equals(LANGUAGES.EN)) && numOfWords == 1){
			try {
				call_url = "http://m.linguee.de/deutsch-englisch/search?source=auto&query=" + URLEncoder.encode(search, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				call_url = "http://m.linguee.de/deutsch-englisch/";
			}
		//Turkish Dictionary
		}else if (targetLang.equals(LANGUAGES.TR) && numOfWords == 1){
			try {
				//call_url = "http://detr.dict.cc/?s=" + URLEncoder.encode(search, "UTF-8");
				call_url = "http://www.seslisozluk.net/de/was-bedeutet-" + URLEncoder.encode(search, "UTF-8").replace("+", "%20");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				call_url = "http://detr.dict.cc";
			}
		//Japanese and Turkish text
		}else if (targetLang.equals(LANGUAGES.TR) || targetLang.equals(LANGUAGES.JA)) {
			call_url = "https://translate.google.com/?sl=" 
					+ nluResult.input.language + "&tl=" 
					+ targetLang + "&text=";
			try {
				call_url += URLEncoder.encode(search, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		//Others (not all might be supported)
		}else{
			call_url = "https://www.deepl.com/translator#" 
					+ nluResult.input.language + "/" 
					+ targetLang + "/";
			try {
				call_url += URLEncoder.encode(search, "UTF-8").replace("+", "%20");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		//action - for supported languages
		if (targetLang.matches(supportedLanguages)){
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
