package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
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
import net.b07z.sepia.server.assist.parameters.SportsLeague;
import net.b07z.sepia.server.assist.parameters.SportsTeam;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.tools.RandomGen;
import net.b07z.sepia.server.assist.workers.OpenLigaWorker;
import net.b07z.sepia.server.assist.workers.Workers;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * News service test.
 * 
 * @author Florian Quirin
 *
 */
public class News_RssFeeds implements ApiInterface{
	
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
		localTerms_de.put("<games>", "Games");
		localTerms_de.put("<music>", "Musik");
		localTerms_de.put("<cinema>", "Kino");
		localTerms_de.put("<tv>", "Tv Serien");
		localTerms_de.put("<startup>", "Start-Up");
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
		localTerms_en.put("<games>", "Games");
		localTerms_en.put("<music>", "MusiC");
		localTerms_en.put("<cinema>", "Cinema");
		localTerms_en.put("<tv>", "Tv series");
		localTerms_en.put("<startup>", "Start-up");
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
	
	//NEWS PAGES
	public static final HashMap<String, String> feedUrls = new HashMap<>();
	static{
		feedUrls.put("SPIEGEL ONLINE", "http://www.spiegel.de/schlagzeilen/tops/index.rss");
		feedUrls.put("bento", "http://www.bento.de/rss/feed.rss");
		feedUrls.put("Tagesschau", "http://www.tagesschau.de/xml/rss2");
		feedUrls.put("Süddeutsche", "http://rss.sueddeutsche.de/rss/Topthemen");
		feedUrls.put("FAZ", "http://www.faz.net/rss/aktuell/");
		feedUrls.put("Bild", "https://www.bild.de/rssfeeds/vw-home/vw-home-16725562,sort=1,view=rss2.bild.xml");
		feedUrls.put("Sportschau", "https://www.sportschau.de//sportschauindex100~_type-rss.feed");
		feedUrls.put("Sportschau - Bundesliga", "https://www.sportschau.de/fussball/bundesliga/fussballbundesligaindex100~_type-rss.feed");
		feedUrls.put("Sportschau - Fussball", "https://www.sportschau.de/fussball/fussballindex100~_type-rss.feed");
		feedUrls.put("Sport1", "https://www.sport1.de/news.rss");
		feedUrls.put("Kicker", "http://rss.kicker.de/news/aktuell");
		feedUrls.put("Wired", "https://www.wired.de/feed/latest");
		feedUrls.put("Gruenderszene", "https://www.gruenderszene.de/feed");
		feedUrls.put("jetzt.de", "https://www.jetzt.de/alle_artikel.rss");
		feedUrls.put("11FREUNDE", "https://www.11freunde.de/feed");
		feedUrls.put("RevierSport", "http://www.reviersport.de/?news-rss-ama");
		feedUrls.put("golem.de", "http://rss.golem.de/rss.php?feed=RSS2.0");
		feedUrls.put("heise online", "https://www.heise.de/newsticker/heise-top-atom.xml");
		feedUrls.put("PCGames", "http://www.pcgames.de/feed.cfm?menu_alias=home");
		feedUrls.put("GameStar", "https://www.gamestar.de/news/rss/news.rss");
		//feedUrls.put("Yahoo eSports", "https://esports.yahoo.com/rss");
		//feedUrls.put("Yahoo LoL", "https://sports.yahoo.com/league-of-legends/rss");
		feedUrls.put("t3n", "https://t3n.de/rss.xml");
		feedUrls.put("Business Punk", "http://www.business-punk.com/feed/");
		feedUrls.put("RollingStone", "https://www.rollingstone.com/music/rss");
		feedUrls.put("MUSIC NEWS", "http://www.music-news.com/rss/UK/news");
		//feedUrls.put("CinemaxX", "https://www.cinemaxx.de/Site/GetRSS/Filme");
		feedUrls.put("CinemaxX", "https://www.presseportal.de/rss/pm_9588.rss2");
		feedUrls.put("Filmstarts.de - aktuell", "http://rss.filmstarts.de/fs/kinos/aktuelle?format=xml");
		feedUrls.put("Filmstarts.de - Serien", "http://rss.filmstarts.de/fs/news/serien?format=xml");
		//feedUrls.put("Serienjunkies - Kalender", "http://www.serienjunkies.de/docs/serienkalender-aktuell.html#feed");
		feedUrls.put("cinema.de - Trailer", "http://www.cinema.de/kino/trailer/video/rss.xml");
		feedUrls.put("SPIEGEL Kino", "http://www.spiegel.de/kultur/kino/index.rss");
		feedUrls.put("deutsche startups", "https://www.deutsche-startups.de/feed/");
	}
	public static final HashMap<String, String> feedNames = new HashMap<>();
	static{
		feedNames.put("SPIEGEL ONLINE", "<span style='color:#950000;'><b>SPIEGEL</b> ONLINE</span>");
		feedNames.put("bento", "<span style='color:#000;'><b>bento</b></span>");
		feedNames.put("Tagesschau", "<span style='color:#0065BE; font-family:serif'>tages<b>schau</b>.de</span>");
		feedNames.put("Süddeutsche", "<span style='color:#000; font-family:serif'>Süddeutsche Zeitung</span>");
		feedNames.put("FAZ", "<span style='color:#000; font-family:serif'>Frankfurter Allgemeine</span>");
		feedNames.put("Bild", "<span style='color:#D91918;'>Bild</span><span style='color:#000;'>.de</span>");
		feedNames.put("Sportschau", "<span style='color:#002C6B;'><b>SPORTSCHAU</b></span>");
		feedNames.put("Sportschau - Bundesliga", "<span style='color:#002C6B;'><b>SPORTSCHAU</b></span> - <span style='color:#EE1C25;'><b>BUNDESLIGA</b></span>");
		feedNames.put("Sportschau - Fussball", "<span style='color:#002C6B;'><b>SPORTSCHAU</b></span> - <span style='color:#000000;'><b>FUSSBALL</b></span>");
		feedNames.put("Sport1", "<span style='color:#3D464C;'>sport</span><span style='color:#F6A800;'><b>1</b></span>");
		feedNames.put("Kicker", "<span style='color:#CC0000;'><b>kicker</b></span>");
		feedNames.put("Wired", "<span style='color:#222222; font-family:serif'><b>W</b>I<b>R</b>E<b>D</b></span>");
		feedNames.put("Gruenderszene", "<span style='color:#000; font-family:serif'>GRÜNDER</span><span style='color:#456494; font-family:serif'>SZENE</span>");
		feedNames.put("jetzt.de", "<span style='color:#000; font-family:serif'>jetzt.de</span>");
		feedNames.put("11FREUNDE", "<span style='color:#000;'>11FREUNDE</span>");
		feedNames.put("RevierSport", "<span style='color:#000;'>RevierSport</span>");
		feedNames.put("golem.de", "<span style='color:#000;'>golem.</span><span style='color:#6EC5CD;'>d</span><span style='color:#9AC54B;'>e</span>");
		feedNames.put("heise online", "<span style='color:#999898;'>heise online</span>");
		feedNames.put("PCGames", "<span style='color:#1F83B5;'>PC</span><span style='color:#FBDB21;'>Games</span>");
		feedNames.put("GameStar", "<span style='color:#003372;'><b>GameStar</b></span>");
		//feedNames.put("Yahoo eSports", "<span style='color:#4500B0;'>Yahoo eSports</span>");
		//feedNames.put("Yahoo LoL", "<span style='color:#4500B0;'>Yahoo LoL</span>");
		feedNames.put("t3n", "<span style='color:#000;'>t</span><span style='color:#FF0000;'>3</span><span style='color:#000000;'>n</span>");
		feedNames.put("Business Punk", "<span style='color:#2C2C2C; font-family:serif;'>Business Punk</span>");
		feedNames.put("RollingStone", "<span style='color:#C81429; font-family:serif;'>RollingStone</span>");
		feedNames.put("MUSIC NEWS", "<span style='color:#ba1a56;'>MUSIC </span><span style='color:#000000;'>NEWS</span>");
		feedNames.put("CinemaxX", "<span style='color:#b90049;'><i>Cinemax<b>X</b></i></span>");
		feedNames.put("Filmstarts.de - aktuell", "<span style='color:#3d59ba;'>Filmstarts.de</span><span style='color:#000000;'> - aktuell</span>");
		feedNames.put("Filmstarts.de - Serien", "<span style='color:#3d59ba;'>Filmstarts.de</span><span style='color:#000000;'> - Serien</span>");
		//feedNames.put("Serienjunkies - Kalender","<span style='color:#414F5D;'>Serienjunkies</span><span style='color:#000000;'> - Kalender</span>");
		feedNames.put("cinema.de - Trailer", "<span style='color:#000000;'>cinema.</span><span style='color:#009ee1;'>de</span><span style='color:#000000;'> - Trailer</span>");
		feedNames.put("SPIEGEL Kino", "<span style='color:#950000;'><b>SPIEGEL</b> Kino</span>");
		feedNames.put("deutsche startups", "<span style='color:#3b65b1;'>deutsche startups</span>");
	}
	
	//SECTION BLOCKS
	public static final ArrayList<String> commonNews_de = new ArrayList<>();
	static{
		commonNews_de.add("SPIEGEL ONLINE");
		commonNews_de.add("Süddeutsche");
		commonNews_de.add("FAZ");
		commonNews_de.add("Bild");
		commonNews_de.add("Tagesschau");
		commonNews_de.add("Wired");
		commonNews_de.add("Gruenderszene");
		commonNews_de.add("jetzt.de");
		commonNews_de.add("bento");
	}
	public static final ArrayList<String> sportNews_de = new ArrayList<>();
	static{
		sportNews_de.add("Sport1");
		sportNews_de.add("Sportschau");
		sportNews_de.add("Kicker");
		//sportNews_de.add("Yahoo eSports");
	}
	public static final ArrayList<String> soccerNews_de = new ArrayList<>();
	static{
		//soccerNews_de.add("Sportschau - Bundesliga");
		soccerNews_de.add("Sportschau - Fussball");
		soccerNews_de.add("Kicker");
		soccerNews_de.add("Sport1");
		soccerNews_de.add("11FREUNDE");
		soccerNews_de.add("RevierSport");
	}
	public static final ArrayList<String> techNews_de = new ArrayList<>();
	static{
		techNews_de.add("Wired");
		techNews_de.add("t3n");
		techNews_de.add("golem.de");
		techNews_de.add("heise online");
		techNews_de.add("PCGames");
		//techNews_de.add("Yahoo eSports");
	}
	public static final ArrayList<String> gamesNews_de = new ArrayList<>();
	static{
		gamesNews_de.add("PCGames");
		gamesNews_de.add("GameStar");
		//gamesNews_de.add("Yahoo eSports");
		//gamesNews_de.add("Yahoo LoL");
	}
	public static final ArrayList<String> musicNews_de = new ArrayList<>();
	static{
		musicNews_de.add("RollingStone");
		musicNews_de.add("MUSIC NEWS");
	}
	public static final ArrayList<String> cinemaNews_de = new ArrayList<>();
	static{
		cinemaNews_de.add("CinemaxX");
		cinemaNews_de.add("Filmstarts.de - aktuell");
		cinemaNews_de.add("cinema.de - Trailer");
		cinemaNews_de.add("SPIEGEL Kino");
	}
	public static final ArrayList<String> tvNews_de = new ArrayList<>();
	static{
		tvNews_de.add("Filmstarts.de - Serien");
		//tvNews_de.add("Serienjunkies - Kalender");
	}
	public static final ArrayList<String> startUpNews_de = new ArrayList<>();
	static{
		startUpNews_de.add("Gruenderszene");
		startUpNews_de.add("deutsche startups");
		startUpNews_de.add("Business Punk");
	}
	
	//SECTION MAPPING
	public static final HashMap<String, ArrayList<String>> sectionNewsBlocks_de = new HashMap<>();
	static{
		sectionNewsBlocks_de.put(NSection.main.name(), commonNews_de);
		sectionNewsBlocks_de.put(NSection.economy.name(), commonNews_de);
		sectionNewsBlocks_de.put(NSection.politics.name(), commonNews_de);
		sectionNewsBlocks_de.put(NSection.science.name(), techNews_de);
		sectionNewsBlocks_de.put(NSection.tech.name(), techNews_de);
		sectionNewsBlocks_de.put(NSection.sports.name(), sportNews_de);
		sectionNewsBlocks_de.put(NSection.soccer.name(), soccerNews_de);
		sectionNewsBlocks_de.put(NSection.games.name(), gamesNews_de);
		sectionNewsBlocks_de.put(NSection.music.name(), musicNews_de);
		sectionNewsBlocks_de.put(NSection.cinema.name(), cinemaNews_de);
		sectionNewsBlocks_de.put(NSection.tv.name(), tvNews_de);
		sectionNewsBlocks_de.put(NSection.startup.name(), startUpNews_de);
	}
	
	/**
	 * Returns all available feeds for e.g. a feed refresh worker.
	 */
	public static final HashSet<String> getAllFeeds(){
		HashSet<String> allFeeds = new HashSet<>();
		
		allFeeds.addAll(commonNews_de);
		allFeeds.addAll(sportNews_de);
		allFeeds.addAll(soccerNews_de);
		allFeeds.addAll(techNews_de);
		allFeeds.addAll(gamesNews_de);
		allFeeds.addAll(musicNews_de);
		allFeeds.addAll(cinemaNews_de);
		allFeeds.addAll(tvNews_de);
		allFeeds.addAll(startUpNews_de);
		
		return allFeeds;
	}
	//-------------------------------------
	
	//info
	public ApiInfo getInfo(String language){
		//type
		ApiInfo info = new ApiInfo(Type.RSS, Content.data, false);
		
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
			.addFailAnswer("abort_0c")
			.addAnswerParameters("topic") 		//be sure to use the same parameter names as in resultInfo
			.addCustomAnswer("soccerResults", soccerResultsAns)
			.addCustomAnswer("soccerTable", soccerTableAns);
		
		return info;
	}
	private static final String soccerResultsAns = "news_1b";
	private static final String soccerTableAns = "news_1c";

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result, getInfo(""));
		
		//Button options:
		JSONObject optionsNoActions = JSON.make(ACTIONS.SKIP_TTS, true, ACTIONS.SKIP_TEXT, true, ACTIONS.SKIP_ACTIONS, true);
		JSONObject optionsWithActions = JSON.make(ACTIONS.SKIP_TTS, true, ACTIONS.SKIP_TEXT, true);
						
		//parameter evaluation:
		
		//SPORTS TEAM
		Parameter sportsTeamP = NLU_result.getOptionalParameter(PARAMETERS.SPORTS_TEAM, "");
		String sportsTeam = sportsTeamP.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//System.out.println("SPORTS TEAM: " + sportsTeam); 		//debug
		
		//SPORTS TEAM
		Parameter sportsLeagueP = NLU_result.getOptionalParameter(PARAMETERS.SPORTS_LEAGUE, "");
		String sportsLeague = sportsLeagueP.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//System.out.println("SPORTS LEAGUE: " + sportsLeague);	//debug
		
		//TYPE
		Parameter nType = NLU_result.getOptionalParameter(PARAMETERS.NEWS_TYPE, N_TYPE);
		String type = nType.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//used in "topic" to build a nice answer (this could also come from a mapping in this class):
		String typeLocal = "";
		if (type.equals(NType.results.name())){
			typeLocal = nType.getDataFieldOrDefault(InterviewData.VALUE_LOCAL).toString();				//e.g. Ergebnisse <=> Results
		}
		
		//SECTION
		Parameter nSection = NLU_result.getOptionalParameter(PARAMETERS.NEWS_SECTION, "");
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
		Parameter nSearch = NLU_result.getOptionalParameter(PARAMETERS.NEWS_SEARCH, "");
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
			
			//Only real support with detailed results is for Bundesliga right now
			boolean isBundesliga = (!sportsLeague.isEmpty() && !sportsLeague.equals(SportsLeague.BUNDESLIGA)) 
					|| (!sportsTeam.isEmpty() && !SportsTeam.isBundesligaTeam("<" + sportsTeam + ">"));
			
			//If it is Bundesliga we need the worker data
			if (isBundesliga &&	(Workers.openLigaWorkerBundesliga == null 
					|| Workers.openLigaWorkerBundesliga.getStatus() < 0)
				){
				//ABORT further processing with some generic data:
				
				String url = "http://m.sport1.de/fussball/";
				api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
				api.putActionInfo("url", url);
				api.hasAction = true;
				
				//answer
				api.setCustomAnswer(soccerResultsAns);
				
				//sports news button?
				api.addAction(ACTIONS.BUTTON_CMD);
				api.putActionInfo("title", getLocal(NSection.sports, api.language));
				api.putActionInfo("info", "direct_cmd");
				api.putActionInfo("cmd", CmdBuilder.getNews(NSection.sports.name()));
				api.putActionInfo("options", optionsNoActions);
				
				//all clear?
				api.status = "success";
				
				//build the API_Result
				ApiResult result = api.buildApiResult();
						
				return result;
				//END
			}
			
			//TODO: make this variable, but we have only bundesliga right now
			String leagueTag = OpenLigaWorker.openLigaDB_mapping.get(SportsLeague.BUNDESLIGA); 		//note: openLigaDB tags are different from SportsLeague constants!!!
			String year = Workers.openLigaWorkerBundesliga.getData(OpenLigaWorker.SEASON).toString();
			String matchDayLocal = getLocal("<matchday>", api.language);
			String tableLocal = getLocal("<table>", api.language);
			
			//action
			String urlMatchDay = "http://m.sport1.de/fussball/bundesliga/spielplan#/";
			String urlTable = "http://m.sport1.de/fussball/bundesliga/tabelle#/";
							//"http://www.sportschau.de/fussball/bundesliga/spieltag/index.html";
							//"http://www.bundesligaplaner.sportschau.de/";
			
			//EXCEPTIONS (non 1. Bundesliga teams)
			if (!isBundesliga){
				String url = "http://m.sport1.de/fussball/";
				if (sportsTeam.equals("fc_kray")){
					url = "http://www.reviersport.de/vereine/401-fc-kray.html";
				}else if (sportsTeam.equals("galatasaray_istanbul")){
					url = "http://www.sport1.de/fussball/team/galatasaray-istanbul";
				}else if (sportsTeam.equals("fc_barcelona") || sportsTeam.equals("real_madrid") || sportsLeague.equals(SportsLeague.PRIMERA_DIVISION)){
					url = "http://www.sport1.de/internationaler-fussball/primera-division/spielplan#/";
				}else if (sportsTeam.equals("manchester_united") || sportsTeam.equals("fc_liverpool") || sportsLeague.equals(SportsLeague.PREMIER_LEAGUE)){
					url = "http://www.sport1.de/internationaler-fussball/premier-league/spielplan#/";
				}else if (sportsLeague.equals("world_championchip")){
					url = "http://www.sport1.de/fussball/wm";
				}else if (sportsLeague.equals(SportsLeague.DFB_POKAL)){
					url = "http://www.sport1.de/fussball/dfb-pokal/spielplan#/";
				}else if (sportsLeague.equals(SportsLeague.CHAMPIONS_LEAGUE)){
					url = "http://www.sport1.de/fussball/champions-league/spielplan#/";
				}else if (sportsLeague.equals("euro_league")){
					url = "http://www.sport1.de/fussball/europa-league/spielplan#/";
				}
				
				api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
				api.putActionInfo("url", url);
				api.hasAction = true;
				
				//answer
				api.setCustomAnswer(soccerResultsAns);
				//end
				
			//TABLE
			}else if (type.equals(NType.table.name())){
				try{
					JSONObject leagueInfo = (JSONObject) OpenLigaWorker.openLigaData.get(leagueTag + "_info");
					JSONArray leagueTable = (JSONArray) OpenLigaWorker.openLigaData.get(leagueTag + "_table");
					JSONObject leagueTableInfo = (JSONObject) OpenLigaWorker.openLigaData.get(leagueTag + "_table_info");
					long tableMatchDay = (long) leagueTableInfo.get("matchDay");
					String leagueName = "<span style='color:#f00;'>" + leagueInfo.get("leagueName") + "</span>";
					
					//start HTML
					String data = getHtmlTableStartWithHeader(leagueName + " - " + tableMatchDay + ". " + matchDayLocal);
					
					//list teams
					data += getTeamsAsTabelRows(leagueTable, api.language);
					
					//finish
					data += getHtmlTableEnd();
					
					//actions
					api.addAction(ACTIONS.BUTTON_CMD);
					api.putActionInfo("title", tableLocal);
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", CmdBuilder.getSoccerTable(sportsLeague));
					api.putActionInfo("options", optionsWithActions);
					
					api.addAction(ACTIONS.BUTTON_CMD);
					api.putActionInfo("title", matchDayLocal);
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", CmdBuilder.getSoccerResults(sportsLeague, sportsTeam));
					api.putActionInfo("options", optionsWithActions);
					
					api.addAction(ACTIONS.SHOW_HTML_RESULT);
					api.putActionInfo("data", data);
					api.hasAction = true;
					
				}catch (Exception e){
					Debugger.println("Soccer table - failed for team: " + sportsTeam + " - error: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
					api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
					api.putActionInfo("url", urlTable);
					api.hasAction = true;
				}
				//answer
				api.setCustomAnswer(soccerTableAns);
				//end
			
			//GET SINGLE TEAM RESULT
			}else if (!sportsTeam.isEmpty()){
				try{
					long teamID = SportsTeam.getSoccerTeamID("<" + sportsTeam + ">");
					JSONObject leagueInfo = (JSONObject) OpenLigaWorker.openLigaData.get(leagueTag + "_info");
					JSONObject leagueData = (JSONObject) OpenLigaWorker.openLigaData.get(leagueTag);
					//TODO: add null check
					long currentLeagueMatchDay = (long) leagueInfo.get("matchDay");
					JSONArray lastMatchDay = (JSONArray) leagueData.get(year + "/" + (currentLeagueMatchDay-1));
					JSONArray matchDay = (JSONArray) leagueData.get(year + "/" + (currentLeagueMatchDay));
					JSONArray nextMatchDay = (JSONArray) leagueData.get(year + "/" + (currentLeagueMatchDay+1));
					String leagueName = "<span style='color:#f00;'>" + leagueInfo.get("leagueName") + "</span>";
					
					//start HTML
					String data = getHtmlTableStartWithHeader(SportsTeam.shortNames.get(teamID) + " - " + leagueName);
					
					//match days
					ArrayList<Long> teams = new ArrayList<>();
					teams.add(teamID);
					
					//last match
					if (lastMatchDay != null && !lastMatchDay.isEmpty()){
						data += getMatchesAsTabelRows(lastMatchDay, teams);
					}
					
					//current match
					if (matchDay != null && !matchDay.isEmpty()){
						data += getMatchesAsTabelRows(matchDay, teams);
					}
					
					//next match
					if (nextMatchDay != null && !nextMatchDay.isEmpty()){
						data += getMatchesAsTabelRows(nextMatchDay, teams);
					}
					
					//finish HTML
					data += getHtmlTableEndWithFooter(urlMatchDay, urlTable, tableLocal);
					
					//actions
					api.addAction(ACTIONS.BUTTON_CMD);
					api.putActionInfo("title", tableLocal);
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", CmdBuilder.getSoccerTable(sportsLeague));
					api.putActionInfo("options", optionsWithActions);
					
					api.addAction(ACTIONS.BUTTON_CMD);
					api.putActionInfo("title", "Team");
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", CmdBuilder.getSoccerResults(sportsLeague, sportsTeam));
					api.putActionInfo("options", optionsWithActions);
						
					api.addAction(ACTIONS.SHOW_HTML_RESULT);
					api.putActionInfo("data", data);
					api.hasAction = true;
					
				}catch (Exception e){
					Debugger.println("Soccer results - failed for team: " + sportsTeam + " - error: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
					api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
					api.putActionInfo("url", urlMatchDay);
					api.hasAction = true;
				}
				//answer
				api.setCustomAnswer(soccerResultsAns);
				//end

			//GET ALL MATCHES OVERVIEW
			}else{ 
				try{
					JSONObject leagueInfo = (JSONObject) OpenLigaWorker.openLigaData.get(leagueTag + "_info");
					JSONObject leagueData = (JSONObject) OpenLigaWorker.openLigaData.get(leagueTag);
					long currentLeagueMatchDay = (long) leagueInfo.get("matchDay");
					JSONArray matchDay = (JSONArray) leagueData.get(year + "/" + (currentLeagueMatchDay));
					 
					String leagueName = "<span style='color:#f00;'>" + leagueInfo.get("leagueName") + "</span>";
					
					String data = getHtmlTableStartWithHeader(leagueName + " " + leagueInfo.get("matchDay") + ". " + matchDayLocal);
					data += getMatchesAsTabelRows(matchDay, new ArrayList<Long>());
					data += getHtmlTableEndWithFooter(urlMatchDay, urlTable, tableLocal);
						
					//actions
					api.addAction(ACTIONS.BUTTON_CMD);
					api.putActionInfo("title", tableLocal);
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", CmdBuilder.getSoccerTable(sportsLeague));
					api.putActionInfo("options", optionsWithActions);
					
					api.addAction(ACTIONS.BUTTON_CMD);
					api.putActionInfo("title", matchDayLocal);
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", CmdBuilder.getSoccerResults(sportsLeague, sportsTeam));
					api.putActionInfo("options", optionsWithActions);
					
					api.addAction(ACTIONS.SHOW_HTML_RESULT);
					api.putActionInfo("data", data);
					api.hasAction = true;
					
				}catch (Exception e){
					Debugger.println("Soccer results - failed for league: " + leagueTag + " - error: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
					api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
					api.putActionInfo("url", urlMatchDay);
					api.hasAction = true;
				}
				//answer
				api.setCustomAnswer(soccerResultsAns);
				//end
			}
			
			//sports news button?
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocal(NSection.sports, api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", CmdBuilder.getNews(NSection.sports.name()));
			api.putActionInfo("options", optionsNoActions);
			
			//all clear?
			api.status = "success";
			
			//build the API_Result
			ApiResult result = api.buildApiResult();
					
			return result;
		}
		
		//articles
		ArrayList<String> feeds;
		if (api.language.equals(LANGUAGES.DE)){
			feeds = sectionNewsBlocks_de.get(section);
		}else{
			feeds = sectionNewsBlocks_de.get(section);
		}
		
		Card card = new Card(Card.TYPE_GROUPED_LIST);
		int i = 1;
		for (String feedName : feeds){
			String nameHTML = feedNames.get(feedName);
			String url = feedUrls.get(feedName);
	        JSONObject feed = Config.rssReader.getFeed(url, feedName, 8, Config.cacheRssResults);
	        if (!feed.isEmpty()){
	        	JSON.add(feed, "name", nameHTML);
	        	if (feed.get("image").toString().isEmpty()){
	        		//TODO: add logo-url
	        	}
	        	card.addGroupeElement(ElementType.news, String.valueOf(i), feed);
	        	i++;
	        }
		}
		Statistics.addExternalApiHit("News RSS-Reader");
		Statistics.addExternalApiTime("News RSS-Reader", tic);
		//add it
		api.addCard(card.getJSON());
		api.hasCard = true;
		
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
		api.status = "success";
		
		//finally build the API_Result
		ApiResult result = api.buildApiResult();
				
		//System.out.println(result.result_JSON.toJSONString());
		return result;
	}

	//------------------------ HELPER ---------------------------
	
	public static String getHtmlTableStartWithHeader(String head){
		String data = "<div class='card-box'>"
				+ "<div style='padding:20px;text-align:center;background-color: #FFF;'>"
					+ "<p style='font-weight:400;'>" + head + "</p>"
					+ "<table style='width:100%;'>";
		return data;
	}
	public static String getHtmlTableEnd(){
		String data = "<tr><td height='15px'></td></tr></table>" + "</div>" + "</div>";
		return data;
	}
	public static String getHtmlTableEndWithFooter(String urlMatchDay, String urlTable, String tableLocal){
		String data = "<tr><td height='15px'></td></tr></table>"
					+ "<p>"
						+ "<a href=" + urlMatchDay + " target='_blank'><span style='color:#f00;'><u>" + "Details" + "</u></span></a>"
						//+ "&nbsp;&nbsp;-&nbsp;&nbsp;"
						//+ "<a href=" + urlTable + " target='_blank'><span style='color:#f00;'><u>" + tableLocal + "</u></span></a>"
					+ "</p>"
				+ "</div>"
			+ "</div>";
		return data;
	}
	
	public static String getMatchesAsTabelRows(JSONArray matchDay, List<Long> teamIDs){
		String styleMatchHeader = "style='font-size:90%;background-color:rgba(40,48,89,0.05);border:5px solid #fff;'";
		String data = "";
		String lastDate = "";
		for (Object o : matchDay){
			JSONObject match = (JSONObject) o;
			long teamId1 = (long)match.get("id1");		long teamId2 = (long)match.get("id2");
			if (teamIDs.isEmpty() || teamIDs.contains(teamId1) || teamIDs.contains(teamId2)){
				List<String> items = Arrays.asList(((String) match.get("summary")).split(","));
				if (items.size() < 2){
					items.add("-:-");
				}
				if (!items.get(0).equals(lastDate)){
					lastDate = items.get(0);
					data += "<tr>" + "<td class='text-center' " + styleMatchHeader + " colspan=3>" + items.get(0).trim() + "</td>" + "</tr>"; 
				}
				if ((boolean) match.get("isFinished")){
					items.set(2, "<b>" + items.get(2).trim() + "</b>");
				}
				data += "<tr>"
						+ "<td class='text-right' width='40%'>" + SportsTeam.getShortName(teamId1) + "</td>"
						+ "<td class='text-center'>" + items.get(2) + "</td>"
						+ "<td class='text-left' width='40%'>" + SportsTeam.getShortName(teamId2) + "</td>"
					+ "</tr>";
			}
		}
		return data;
	}
	
	public static String getTeamsAsTabelRows(JSONArray teams, String language){
		String styleTableHeader = "style='font-size:90%;font-weight:500;'";
		String styleOdd = "style='background-color:rgba(40,48,89,0.05);'";
		String data = "";
		data += "<tr>"
				+ "<th class='text-left' " + styleTableHeader + " >" + "Team" + "</th>"		//width='33%'
				+ "<th class='text-center' " + styleTableHeader + " >" + getLocal("<matches>", language) + "</th>"
				+ "<th class='text-center' " + styleTableHeader + " >" + getLocal("<wins>", language) + "</th>"
				//+ "<th class='text-center'>" + team.get("draw") + "</th>"
				//+ "<th class='text-center'>" + team.get("lost") + "</th>"
				+ "<th class='text-center' " + styleTableHeader + " >" + getLocal("<goals>", language) + "</th>"
				//+ "<th class='text-center'>" + team.get("goals_agains") + "</th>"
				+ "<th class='text-center' " + styleTableHeader + " ><b>" + getLocal("<points>", language) + "</b></th>"
			+ "</tr>";
		int i = 0;
		for (Object teamO : teams) {
			String style = styleOdd;
			i++;
			if ( (i & 1) == 0 ) {
				style = "";
			}
			JSONObject team = (JSONObject) teamO;
			data += "<tr>"
					+ "<td class='text-left' " + style + " >" + team.get("team") + "</td>"		//width='33%'
					+ "<td class='text-center' " + style + " >" + team.get("games") + "</td>"
					+ "<td class='text-center' " + style + " >" + team.get("won") + "</td>"
					//+ "<td class='text-center'>" + team.get("draw") + "</td>"
					//+ "<td class='text-center'>" + team.get("lost") + "</td>"
					+ "<td class='text-center' " + style + " >" + team.get("goals") + ":" + team.get("goals_against") + "</td>"
					//+ "<td class='text-center'>" + team.get("goals_agains") + "</td>"
					+ "<td class='text-center' " + style + " ><b>" + team.get("points") + "</b></td>"
				+ "</tr>";
		}
		return data;
	}
}
