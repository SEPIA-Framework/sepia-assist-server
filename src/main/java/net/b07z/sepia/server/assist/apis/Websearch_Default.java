package net.b07z.sepia.server.assist.apis;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.WebSearchEngine;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Default Websearch class.
 * 
 * @author Florian Quirin
 *
 */
public class Websearch_Default implements ApiInterface{
	
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
	public ApiInfo getInfo(String language){
		ApiInfo info =  new ApiInfo(Type.link, Content.redirect, false);
		
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
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result, getInfo(""));
		
		//get interview parameters
		
		//required
		JSONObject searchJSON = NLU_result.getRequiredParameter(PARAMETERS.WEBSEARCH_REQUEST).getData();
		String search = JSON.getStringOrDefault(searchJSON, InterviewData.VALUE, "");
		String searchReduced = JSON.getStringOrDefault(searchJSON, InterviewData.VALUE_REDUCED, search);
		api.resultInfo_add("search", search);
		
		//optional
		Parameter sectionP = NLU_result.getOptionalParameter(PARAMETERS.SEARCH_SECTION, "");
		String section = "";
		String sectionLocal = "";
		if (!sectionP.isDataEmpty()){
			section = (String) sectionP.getData().get(InterviewData.VALUE);
			sectionLocal = (String) sectionP.getData().get(InterviewData.VALUE_LOCAL);
		}
		api.resultInfo_add("section", sectionLocal);
		
		//TODO: choose best engine or user favorite when no engine is given
		Parameter engineP = NLU_result.getOptionalParameter(PARAMETERS.WEBSEARCH_ENGINE, "google");
		String engine = "";
		if (!engineP.isDataEmpty()){
			engine = (String) engineP.getData().get(InterviewData.VALUE);
		}else{
			engine = (String) engineP.getDefaultValue();
		}
		api.resultInfo_add("engine", engine);
		
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
		String[] getRes = getWebSearchUrl(engine, section, search, searchReduced);
		//get a 2nd random engine
		String[] getResRnd = getWebSearchUrl(getPseudoRandomEngine(engine, section), section, search, searchReduced);
		String search_url = getRes[0];
		String search_url_rnd = getResRnd[0];
		String engineName = getRes[1];
		String engineNameRnd = getResRnd[1];
		String engineIconUrl = getRes[2];
		String engineIconUrlRnd = getResRnd[2];
		
		//make action: in app browser url call and button
		api.actionInfo_add_action(ACTIONS.OPEN_IN_APP_BROWSER);
		api.actionInfo_put_info("url", search_url);
		
		String title1 = getButtonText(engineName, api.language);
		//Button 1 
		/*
		api.actionInfo_add_action(ACTIONS.BUTTON_IN_APP_BROWSER);
		api.actionInfo_put_info("url", search_url); 
		api.actionInfo_put_info("title", title1);
		*/
		//Card 1
		Card card = new Card(Card.TYPE_SINGLE);
		/*JSONObject linkCard = */
		card.addElement(ElementType.link, 
				JSON.make("title", title1 + ":", "desc", "<i>\"" + search + "\"</i>"),
				null, null, "", 
				search_url, 
				engineIconUrl, 
				null, null);
		//JSON.put(linkCard, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(card.getJSON());
		
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
				JSON.make("title", title2 + ":", "desc", "<i>\"" + search + "\"</i>"),
				null, null, "", 
				search_url_rnd, 
				engineIconUrlRnd, 
				null, null);
		//JSON.put(linkCard2, "imageBackground", "transparent");
		api.addCard(card2.getJSON());
		
		//api.hasAction = true;
		//api.hasCard = true;
		
		//get answer
		api.answer = Config.answers.getAnswer(NLU_result, "websearch_1a", search);
		api.answer_clean = Converters.removeHTML(api.answer);
		
		api.status = "success";
		
		//finally build the API_Result
		ApiResult result = api.build_API_result();
		
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
	public static String[] getWebSearchUrl(String engine, String section, String search, String searchReduced){
		String search_url = "";
		String iconUrl = Config.urlWebImages + "cards/search.png";
		engine = engine.toLowerCase();
		
		if (engine.contains("yahoo")){
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
		}else if (engine.contains("bing")){
			engine = "Bing";
			search_url = "https://www.bing.com/search?q=";
			if (section.equals("pictures")){
				search_url = "https://www.bing.com/images/search?q=";		search = searchReduced;
			}else if (section.equals("videos")){
				search_url = "https://www.bing.com/videos/search?q=";		search = searchReduced;
			}
		}else if (engine.contains("duck duck go")){
			engine = "DuckDuckGo";
			search_url = "https://duckduckgo.com/?kae=d&q=";
			if (section.equals("pictures")){
				search_url = "https://duckduckgo.com/?kae=d&ia=images&q=";	search = searchReduced;
			}else if (section.equals("videos")){
				search_url = "https://duckduckgo.com/?kae=d&ia=videos&q=";	search = searchReduced;
			}else if (section.equals("recipes")){
				search_url = "https://duckduckgo.com/?kae=d&ia=recipes&q=";	search = searchReduced;
			}
		}else{
			engine = "Google";
			search_url = "https://www.google.com/search?q=";		//<- default is google
			if (section.equals("pictures")){
				search_url = "https://www.google.com/search?tbm=isch&q=";	search = searchReduced;
			}else if (section.equals("videos")){
				search_url = "https://www.google.com/search?tbm=vid&q=";	search = searchReduced;
			}else if (section.equals("shares")){
				search_url = "https://www.google.com/finance?q=";			search = searchReduced;
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
		ArrayList<String> engines = WebSearchEngine.list;
		engines.remove(except);
		Random rand = new Random();
	    String randomEngine = engines.get(rand.nextInt(engines.size()));
	    //TODO: optimize for section
	    return randomEngine;
	}

}
