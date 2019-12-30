package net.b07z.sepia.server.assist.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.MusicService;
import net.b07z.sepia.server.assist.parameters.SearchSection;
import net.b07z.sepia.server.assist.parameters.WebSearchEngine;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Default Websearch class.
 * 
 * @author Florian Quirin
 *
 */
public class WebsearchBasic implements ServiceInterface{
	
	public static final String CARD_TYPE = "websearch";
	public static final String CARD_TYPE_SPECIAL_VIDEO = "videoSearch";
	
	//Search engines for random-selection
	private static ArrayList<String> commonEngines = new ArrayList<>();
	static{
		commonEngines.add(WebSearchEngine.GOOGLE);
		commonEngines.add(WebSearchEngine.BING);
		commonEngines.add(WebSearchEngine.DUCK_DUCK_GO);
		commonEngines.add(WebSearchEngine.YAHOO);
		//NOTE: add only "real" web-search engines here (not specialized things like YouTube)
	}
	
	//--- data ---
	public static String getButtonText(String engine, String language){
		if (language.equals(LANGUAGES.DE)){
			return "Mit " + engine + " suchen";
		}else{
			return "Search with " + engine;
		}
	}
	//-------------
	
	//info
	public ServiceInfo getInfo(String language){
		ServiceInfo info =  new ServiceInfo(Type.link, Content.redirect, false);
		
		//Parameters:
		//required
		Parameter p1 = new Parameter(PARAMETERS.WEBSEARCH_REQUEST)
				.setRequired(true)
				.setQuestion("websearch_ask_search_0a");
		info.addParameter(p1);
		//optional
		Parameter p2 = new Parameter(PARAMETERS.WEBSEARCH_ENGINE, "");
		info.addParameter(p2);
		Parameter p3 = new Parameter(PARAMETERS.SEARCH_SECTION, "");
		info.addParameter(p3);
		
		//Answers:
		info.addSuccessAnswer("websearch_1a")
			.addFailAnswer("error_0a")
			.addAnswerParameters("search", "engine", "section");
		
		return info;
	}

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get interview parameters
		
		//required
		JSONObject searchJSON = nluResult.getRequiredParameter(PARAMETERS.WEBSEARCH_REQUEST).getData();
		String search = JSON.getStringOrDefault(searchJSON, InterviewData.VALUE, "");
		String searchReduced = JSON.getStringOrDefault(searchJSON, InterviewData.VALUE_REDUCED, search);
		api.resultInfoPut("search", search);
		
		//optional
		Parameter sectionP = nluResult.getOptionalParameter(PARAMETERS.SEARCH_SECTION, "");
		String section = "";
		String sectionLocal = "";
		if (!sectionP.isDataEmpty()){
			section = (String) sectionP.getData().get(InterviewData.VALUE);
			sectionLocal = (String) sectionP.getData().get(InterviewData.VALUE_LOCAL);
		}
		api.resultInfoPut("section", sectionLocal);
		
		//TODO: choose best engine or user favorite when no engine is given
		Parameter engineP = nluResult.getOptionalParameter(PARAMETERS.WEBSEARCH_ENGINE, "google");
		String engine = "";
		if (!engineP.isDataEmpty()){
			engine = (String) engineP.getData().get(InterviewData.VALUE);
		}else{
			engine = (String) engineP.getDefaultValue();
		}
		api.resultInfoPut("engine", engine);
		
		Debugger.println("cmd: websearch, search='" + search + "' with " + engine + ", section: " + sectionLocal, 2);
		
		//one could add personal data somewhere in the web search
		/*
		String[] personal_search = User.containsPersonalUserInfo(search, NLU_result.input.user);
		if (!personal_search[1].isEmpty()){
				search = search.replaceFirst(Pattern.quote(personal_search[2]), personal_search[1]);
		}
		*/
		
		//build URL and clean up the search if necessary
		//"^(bild(ern|er|)|rezept(en|e|)|video(s|)|movie(s|)|film(en|e|)|aktie(n|)|buecher(n|)|buch) (von|ueber|mit|)"
		//get main
		String[] getRes = getWebSearchUrl(engine, section, search, searchReduced, nluResult.input);
		//get a 2nd random engine
		String[] getResRnd = getWebSearchUrl(getPseudoRandomEngine(engine, section), section, search, searchReduced, nluResult.input);
		String search_url = getRes[0];
		String search_url_rnd = getResRnd[0];
		String engineName = getRes[1];
		String engineNameRnd = getResRnd[1];
		String engineIconUrl = getRes[2];
		String engineIconUrlRnd = getResRnd[2];
		
		String title1 = getButtonText(engineName, api.language);
		//Button 1 
		/*
		api.actionInfo_add_action(ACTIONS.BUTTON_IN_APP_BROWSER);
		api.actionInfo_put_info("url", search_url); 
		api.actionInfo_put_info("title", title1);
		*/
		//Card 1
		boolean addUrlAction = true;	//this is usually enabled, but if engine is YouTube and embedding is possible we should skip this
		Card card = new Card(Card.TYPE_SINGLE);
		JSONObject cardData = JSON.make(
			"title", title1 + ":", 
			"desc", "<i>\"" + search + "\"</i>",
			"type", CARD_TYPE
		); 
		if (engine.contains(WebSearchEngine.YOUTUBE)){
			JSON.put(cardData, "type", CARD_TYPE_SPECIAL_VIDEO);			//overwrite
			JSON.put(cardData, "brand", MusicSearch.CARD_BRAND_YOUTUBE);
			JSON.put(cardData, "autoplay", false);
			//check if embedding is possible
			Object embeddingsObj = nluResult.input.getCustomDataObject(NluInput.DATA_EMBEDDED_MEDIA_PLAYERS);
			if (embeddingsObj != null){
				if (((JSONArray) embeddingsObj).contains(MusicService.Service.youtube.name())){
					addUrlAction = false;
					JSON.put(cardData, "embedded", true);
				}
			}
		}
		/*JSONObject linkCard = */
		card.addElement(ElementType.link, 
				cardData,
				null, null, "", 
				search_url, 
				engineIconUrl, 
				null, null);
		//JSON.put(linkCard, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(card.getJSON());
		
		//Action
		if (addUrlAction){
			api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
			api.putActionInfo("url", search_url);
		}
		
		String title2 = getButtonText(engineNameRnd, api.language);
		//Button 2
		/*
		api.actionInfo_add_action(ACTIONS.BUTTON_IN_APP_BROWSER);
		api.actionInfo_put_info("url", search_url_rnd); 
		api.actionInfo_put_info("title", title2);
		*/
		//Card 2
		Card card2 = new Card(Card.TYPE_SINGLE);
		/*JSONObject linkCard2 = */
		card2.addElement(ElementType.link, 
				JSON.make(
					"title", title2 + ":", 
					"desc", "<i>\"" + search + "\"</i>",
					"type", CARD_TYPE
				),
				null, null, "", 
				search_url_rnd, 
				engineIconUrlRnd, 
				null, null);
		//JSON.put(linkCard2, "imageBackground", "transparent");
		api.addCard(card2.getJSON());
		
		//api.hasAction = true;
		//api.hasCard = true;
		
		//get answer - not required anymore, done with setStatus...
		//api.answer = Config.answers.getAnswer(NLU_result, "websearch_1a", search);
		//api.answerClean = Converters.removeHTML(api.answer);
		
		//status
		api.setStatusSuccess();
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		return result;
	}
	
	/**
	 * Get a web-search URL depending on engine and section.
	 * @param engine - engines as seen in corresponding parameter like "yahoo", "google", "duck duck go" and "bing"
	 * @param section - as seen in corresponding parameter like "pictures", "videos", "books", "recipes", "shares"
	 * @param search - term to search like "pictures of Berlin"
	 * @param searchReduced - reduced term without section like "Berlin"
	 * @return encoded URL [0] and engine name [1]
	 */
	public static String[] getWebSearchUrl(String engine, String section, String search, String searchReduced, NluInput nluInput){
		String search_url = "";
		String iconUrl = Config.urlWebImages + "cards/search.png";
		engine = engine.toLowerCase();
		
		//TODO: section names should become an enumerator in SearchSection parameter
		
		if (engine.contains(WebSearchEngine.YAHOO)){
			engine = "Yahoo";
			search_url = "https://search.yahoo.com/search?p=";
			if (section.equals("pictures")){
				search_url = "https://images.search.yahoo.com/search/images?p=";	search = searchReduced;
			}else if (section.equals("videos")){
				search_url = "https://video.search.yahoo.com/search/video?p=";		search = searchReduced;
			}else if (section.equals("recipes")){
				search_url = "https://recipes.search.yahoo.com/search?p=";			search = searchReduced;
			}else if (section.equals("shares")){
				search_url = "https://finance.search.yahoo.com/search;?p=";			search = searchReduced;
			}
		}else if (engine.contains(WebSearchEngine.BING)){
			engine = "Bing";
			search_url = "https://www.bing.com/search?q=";
			if (section.equals("pictures")){
				search_url = "https://www.bing.com/images/search?q=";		search = searchReduced;
			}else if (section.equals("videos")){
				search_url = "https://www.bing.com/videos/search?q=";		search = searchReduced;
			}
		}else if (engine.contains(WebSearchEngine.DUCK_DUCK_GO)){
			engine = "DuckDuckGo";
			search_url = "https://duckduckgo.com/?kae=d&q=";
			if (section.equals("pictures")){
				search_url = "https://duckduckgo.com/?kae=d&ia=images&q=";	search = searchReduced;
			}else if (section.equals("videos")){
				search_url = "https://duckduckgo.com/?kae=d&ia=videos&q=";	search = searchReduced;
			}else if (section.equals("recipes")){
				search_url = "https://duckduckgo.com/?kae=d&ia=recipes&q=";	search = searchReduced;
			}
		}else if (engine.contains(WebSearchEngine.YOUTUBE)){
			engine = "YouTube";
			iconUrl = Config.urlWebImages + "brands/youtube-logo.png";
			search_url = "https://www.youtube.com/results?search_query=";
			if (section.equals("videos") || section.equals("music")){
				search = searchReduced;
			}
		}else{
			engine = "Google";
			search_url = "https://www.google.com/search?q=";		//<- default is google
			if (section.equals("pictures")){
				search_url = "https://www.google.com/search?tbm=isch&q=";	search = searchReduced;
			}else if (section.equals("videos")){
				search_url = "https://www.google.com/search?tbm=vid&q=";	search = searchReduced;
			}else if (section.equals("shares")){
				//if (CLIENTS.isWebSite("")){}
				search = searchReduced + " " + SearchSection.getLocal(section, nluInput.language);
				//search_url = "https://www.google.com/search?tbm=fin&q=";	search = searchReduced; 		//TODO: not working on mobile
			}else if (section.equals("books")){
				search_url = "https://www.google.com/search?tbm=bks&q=";	search = searchReduced;
			}
		}
		try {
			search_url = search_url	+ 	URLEncoder.encode(search, "UTF-8");

		} catch (UnsupportedEncodingException e) {
			search_url = "https://www.google.com";
			//e.printStackTrace();
		}
		
		return new String[]{search_url, engine, iconUrl};
	}
	/**
	 * Get a pseudo-random search engine. "Pseudo" because the result is (will be) optimized for section. 
	 * @param except - an engine to exclude
	 * @param section - optimize for this section (not yet working)
	 */
	public static String getPseudoRandomEngine(String except, String section){
		List<String> enginesToChoose = commonEngines.stream().filter(s -> {
			return !s.equals(except);
		}).collect(Collectors.toList());
		//enginesToChoose.remove(except);
		Random rand = new Random();
	    String randomEngine = enginesToChoose.get(rand.nextInt(enginesToChoose.size()));
	    //TODO: optimize for section
	    return randomEngine;
	}

}
