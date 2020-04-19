package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.assistant.CmdBuilder;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.NewsSection;
import net.b07z.sepia.server.assist.parameters.NewsSection.NSection;
import net.b07z.sepia.server.assist.parameters.NewsType.NType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.tools.RandomGen;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.ENVIRONMENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * News and soccer service.
 * 
 * @author Florian Quirin
 *
 */
public class NewsRssFeeds implements ServiceInterface{
	
	//TODO: separate "normal" news and soccer stuff... 
	
	public static List<NSection> specificSectionsList = new ArrayList<>();
	static{
		specificSectionsList = new ArrayList<>(Arrays.asList(NSection.values()));
		specificSectionsList.remove(NSection.main);
	}

	//default values for parameters
	private static final String N_SECTION = NSection.main.name();
	private static final String N_TYPE = NType.overview.name();
	
	//localizations
	public static HashMap<String, String> localTerms_de = new HashMap<>();
	public static HashMap<String, String> localTerms_en = new HashMap<>();
	static{
		//news
		localTerms_de.put("<latest>", "Aktuell");
		localTerms_de.put("<main>", "Aktuelle News");
		localTerms_de.put("<tech>", "Technik");
		localTerms_de.put("<sports>", "Sport");
		localTerms_de.put("<science>", "Wissenschaft");
		localTerms_de.put("<politics>", "Politik");
		localTerms_de.put("<soccer>", "Fussball");
		localTerms_de.put("<economy>", "Wirtschaft");
		localTerms_de.put("<health>", "Gesundheit");
		localTerms_de.put("<games>", "Games");
		localTerms_de.put("<music>", "Musik");
		localTerms_de.put("<cinema>", "Kino");
		localTerms_de.put("<tv>", "Tv Serien");
		localTerms_de.put("<startup>", "Start-Up");
		localTerms_de.put("<corona>", "Corona");
		//sport
		localTerms_de.put("<matchday>", "Spieltag");
		localTerms_de.put("<matches>", "Spiele");
		localTerms_de.put("<season>", "Saison");
		localTerms_de.put("<table>", "Tabelle");
		localTerms_de.put("<points>", "Punkte");
		localTerms_de.put("<wins>", "Siege");
		localTerms_de.put("<goals>", "Tore");
		
		//news
		localTerms_en.put("<latest>", "Latest");
		localTerms_en.put("<main>", "Latest news");
		localTerms_en.put("<tech>", "Tech");
		localTerms_en.put("<sports>", "Sports");
		localTerms_en.put("<science>", "Science");
		localTerms_en.put("<politics>", "Politics");
		localTerms_en.put("<soccer>", "Soccer");
		localTerms_en.put("<economy>", "Economy");
		localTerms_en.put("<health>", "Health");
		localTerms_en.put("<games>", "Games");
		localTerms_en.put("<music>", "Music");
		localTerms_en.put("<cinema>", "Cinema");
		localTerms_en.put("<tv>", "Tv series");
		localTerms_en.put("<startup>", "Start-up");
		localTerms_en.put("<corona>", "Corona");
		//sport
		localTerms_en.put("<matchday>", "Matchday");
		localTerms_en.put("<matches>", "Matches");
		localTerms_en.put("<season>", "Season");
		localTerms_en.put("<table>", "Table");
		localTerms_en.put("<points>", "Points");
		localTerms_en.put("<wins>", "Wins");
		localTerms_en.put("<goals>", "Goals");
	}
	public static String getLocal(NSection section, String language){
		return getLocal("<" + section.name() + ">", language);
	}
	public static String getLocal(String term, String language){
		String localTerm = "";
		if (language.equals(LANGUAGES.DE)){
			localTerm = localTerms_de.get(term);
		}else{
			localTerm = localTerms_en.get(term);
		}
		if (localTerm == null){
			Debugger.println("News_RssFeeds - getLocal() is missing language '" + language + "' mapping for: " + term, 1);
			return term;
		}
		return localTerm;
	}
	
	//----------------feeds----------------
	
	//NEWS OUTLETS and FEEDS
	public static final Map<String, String> feedUrls = new HashMap<>();
	public static final Map<String, String> feedNames = new HashMap<>();
	public static final JSONObject outletProperties;
	public static final Map<String, Map<String, List<String>>> outletGroupsByLanguage = new HashMap<>();
	
	//Load properties file (Outlets, groups by language etc.)
	static{
		String propFile = Config.servicePropertiesFolder + "news-outlets.json";
		outletProperties = JSON.readJsonFromFile(propFile);
		try{
			//Fill URL and NAME info
			JSONArray outletArray = JSON.getJArray(outletProperties, "outlets");
			for (Object jo : outletArray){
				JSONObject outlet = (JSONObject) jo;
				feedUrls.put(JSON.getString(outlet, "name"), JSON.getString(outlet, "url"));
				feedNames.put(JSON.getString(outlet, "name"), JSON.getString(outlet, "name_html"));
			}
			//Check again
			if (outletArray.isEmpty() || Is.nullOrEmptyMap(feedNames) || Is.nullOrEmptyMap(feedUrls)){
				throw new RuntimeException("Outlet array is empty or entries are broken.");
			}
			
			//Fill OUTLET GROUPS for each language
			JSONArray outletGroupsLanguageArray = JSON.getJArray(outletProperties, "groups");
			for (Object jo : outletGroupsLanguageArray){
				JSONObject languageGroup = (JSONObject) jo;
				String language = JSON.getString(languageGroup, "language");
				JSONArray languageData = JSON.getJArray(languageGroup, "data");
				
				//Add GROUP for this language each section - we basically only convert the JSONArray to a map
				Map<String, List<String>> sections = new HashMap<>();
				for (Object sectionObj : languageData){
					JSONObject section = (JSONObject) sectionObj;
					NSection sectionName = NSection.valueOf(JSON.getString(section, "section"));	//NOTE: name has to match one of the "NSection"s
					JSONArray sectionGroup = JSON.getJArray(section, "group");
					sections.put(sectionName.name(), Converters.object2ArrayListStr(sectionGroup));
				}
				//Check again
				if (sections.isEmpty()){
					throw new RuntimeException("Outlet section groups are empty for language: " + language);
				}
				outletGroupsByLanguage.put(language, sections);
			}
			
		}catch (Exception e){
			Debugger.println("Services:News - Error during initialization.", 1);
			throw new RuntimeException("News service outlet properties file missing or broken! "
					+ "File: " + propFile + " - Error: " + e.getMessage());
		}
		Debugger.println("Services:News - Loaded " + feedNames.keySet().size() 
				+ " outlets with groups for " + outletGroupsByLanguage.keySet().size() + " languages from: " + propFile, 3);
	}
		
	/**
	 * Returns all available feeds (names) for e.g. a feed refresh worker.
	 */
	public static final Set<String> getAllFeeds(){
		return feedNames.keySet();
	}
	//-------------------------------------
	
	//info
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.RSS, Content.data, false);
		
		//Parameters:
		//optional
		Parameter p1 = new Parameter(PARAMETERS.NEWS_SECTION, ""); 			//main, sports, politics, economy, science, ... (mixed or list of sections should be possible too at some point)
		Parameter p2 = new Parameter(PARAMETERS.NEWS_TYPE, N_TYPE);			//overview, topic, results, ... e.g. used to distinguish between "show me some news" and "show me soccer results"
		Parameter p3 = new Parameter(PARAMETERS.NEWS_SEARCH, "");			//a search phrase like "earthquake in Timbuktu"
		Parameter p4 = new Parameter(PARAMETERS.SPORTS_TEAM, "");			//different sport teams
		Parameter p5 = new Parameter(PARAMETERS.SPORTS_LEAGUE, "");			//different sport leagues
		info.addParameter(p1).addParameter(p2).addParameter(p3).addParameter(p4).addParameter(p5);
		
		//Answers (these are the default answers, you can add a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer("news_1a")
			.addOkayAnswer("news_0a")
			.addFailAnswer("abort_0c")
			.addCustomAnswer("answerWoDisplay", answerWoDisplay);
		info.addAnswerParameters("topic", "firstTitle"); 		//be sure to use the same parameter names as in resultInfo
		
		return info;
	}
	private static final String answerWoDisplay = "news_1d"; 

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//Button options:
		JSONObject optionsNoActions = JSON.make(ACTIONS.SKIP_TTS, true, ACTIONS.SKIP_TEXT, true, ACTIONS.SKIP_ACTIONS, true);
		//JSONObject optionsWithActions = JSON.make(ACTIONS.SKIP_TTS, true, ACTIONS.SKIP_TEXT, true);
						
		//parameter evaluation:
		
		//SPORTS TEAM
		Parameter sportsTeamP = nluResult.getOptionalParameter(PARAMETERS.SPORTS_TEAM, "");
		String sportsTeam = sportsTeamP.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//System.out.println("SPORTS TEAM: " + sportsTeam); 		//debug
		
		//SPORTS LEAGUE
		Parameter sportsLeagueP = nluResult.getOptionalParameter(PARAMETERS.SPORTS_LEAGUE, "");
		String sportsLeague = sportsLeagueP.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//System.out.println("SPORTS LEAGUE: " + sportsLeague);	//debug
		
		//TYPE
		Parameter nType = nluResult.getOptionalParameter(PARAMETERS.NEWS_TYPE, N_TYPE);
		String type = nType.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//used in "topic" to build a nice answer (this could also come from a mapping in this class):
		String typeLocal = "";
		if (type.equals(NType.results.name())){
			typeLocal = nType.getDataFieldOrDefault(InterviewData.VALUE_LOCAL).toString();				//e.g. Ergebnisse <=> Results
		}
		
		//SECTION
		Parameter nSection = nluResult.getOptionalParameter(PARAMETERS.NEWS_SECTION, "");
		String section = nSection.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//used in "topic" to build a nice answer (this could also come from a mapping in this class):
		String sectionLocal = nSection.getDataFieldOrDefault(InterviewData.VALUE_LOCAL).toString(); 	//e.g. Wissenschaft <=> Science
		if (section.isEmpty() && (!sportsTeam.isEmpty() || !sportsLeague.isEmpty() || type.equals(NType.results.name()) || type.equals(NType.table.name()))){
			//TODO: since we only have soccer teams right now we can do that
			section = NSection.soccer.name();
			sectionLocal = NewsSection.getLocal("<" + section + ">", api.language);
		}else if (section.isEmpty()){
			section = N_SECTION;
			sectionLocal = NewsSection.getLocal("<" + section + ">", api.language);
		}
		
		//SEARCH
		Parameter nSearch = nluResult.getOptionalParameter(PARAMETERS.NEWS_SEARCH, "");
		String search = nSearch.getDataFieldOrDefault(InterviewData.INPUT).toString();
				
		//add result info - build a nice "topic" for the success answer - might fail for certain languages, but we can always make it more simple ^^
		String topic = "";
		if (!search.isEmpty()){
			topic = search; 		//this has a high chance to sound strange depending on how good the extraction of the search phrase works
		}else if (!typeLocal.isEmpty()){
			topic = sectionLocal + " " + typeLocal;
		}else{
			topic = sectionLocal;	//at this point "sectionLocal" should always have a value, in the worst case the default local value  
		}
		api.resultInfoPut("topic", topic);
		
		Debugger.println("cmd: news, section: " + section + ", type: " + type + ", search: " + search, 2);		//debug
		
		//---------- Read RSS feeds and build card -----------
		long tic = System.currentTimeMillis();
		
		//soccer results?
		if (section.equals(NSection.soccer.name()) && (type.equals(NType.results.name()) || type.equals(NType.table.name()))){
			//TODO: this is the first step of separating news and soccer results, ... next is to get rid of this by splitting the regEx
			return new SoccerBundesligaInfo().getResult(nluResult);
		}
		
		//articles
		Map<String, List<String>> sectionGroupsForLanguage = outletGroupsByLanguage.get(api.language);
		if (Is.nullOrEmptyMap(sectionGroupsForLanguage)){
			sectionGroupsForLanguage = outletGroupsByLanguage.get(LANGUAGES.EN);
		}
		List<String> feeds = sectionGroupsForLanguage.get(section);
		List<String> top3Titles = new ArrayList<>();
		
		Card card = new Card(Card.TYPE_GROUPED_LIST);
		int i = 1;
		for (String feedName : feeds){
			String nameHTML = feedNames.get(feedName);
			String url = feedUrls.get(feedName);
	        JSONObject feed = Config.rssReader.getFeed(url, feedName, 8, Config.cacheRssResults); 	//NOTE: maxEntries does not work when loading from cache (aka worker)
	        if (!feed.isEmpty()){
	        	JSON.add(feed, "name", nameHTML);
	        	/*if (feed.get("image").toString().isEmpty()){
	        		//TODO: add logo-url
	        	}*/
	        	card.addGroupeElement(ElementType.news, String.valueOf(i), feed);
	        	if (i <= 3){
	        		try{
	        			top3Titles.add((String) ((JSONObject) ((JSONArray) feed.get("data")).get(0)).get("title"));
	        		}catch(Exception e){}	//ignored for now
	        	}
	        	i++;
	        }
		}
		Statistics.addExternalApiHit("News RSS-Reader");
		Statistics.addExternalApiTime("News RSS-Reader", tic);
		//add it
		api.addCard(card.getJSON());
		api.hasCard = true;
		
		api.resultInfoPut("firstTitle", top3Titles.get(0));
		
		//actions
		
		//soccer, sports and main
		if (section.equals(NSection.soccer.name())){
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal(NSection.soccer, api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(NSection.soccer.name()));
			api.putActionInfo("options", optionsNoActions);
			
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal(NSection.sports, api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(NSection.sports.name()));
			api.putActionInfo("options", optionsNoActions);
			
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal(NSection.main, api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(""));
			api.putActionInfo("options", optionsNoActions);
		}
		//specific and main
		else if (!section.equals(NSection.main.name())){
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal("<" + section + ">", api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(section));
			api.putActionInfo("options", optionsNoActions);
			
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal(NSection.main, api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(""));
			api.putActionInfo("options", optionsNoActions);
		//main and random
		}else{
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal(NSection.main, api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(""));
			api.putActionInfo("options", optionsNoActions);
			
			NSection rndSection = (NSection) RandomGen.listValue(specificSectionsList); //RandomGen.enumValue(NSection.class);
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal(rndSection, api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(rndSection.name()));
			api.putActionInfo("options", optionsNoActions);
			
			//TODO: add a user favorite here?
		}
		
		//all clear?
		if (top3Titles.isEmpty()){
			//no news but no error?
			api.setStatusOkay();
			
		}else if (!ENVIRONMENTS.deviceHasActiveDisplay(nluResult.input.environment)){
			//no display answer
			api.setCustomAnswer(answerWoDisplay);
			api.setStatusSuccess();
		
		}else{
			//default
			api.setStatusSuccess();
		}
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
				
		//System.out.println(result.result_JSON.toJSONString());
		return result;
	}
}
