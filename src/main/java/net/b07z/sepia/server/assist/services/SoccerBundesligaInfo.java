package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.assistant.CmdBuilder;
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
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.workers.OpenLigaWorker;
import net.b07z.sepia.server.assist.workers.Workers;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * News and soccer service.
 * 
 * @author Florian Quirin
 *
 */
public class SoccerBundesligaInfo implements ServiceInterface{
	
	//default values for parameters
	private static final String N_SECTION = NSection.sports.name();
	private static final String N_TYPE = NType.overview.name();
	
	//localizations
	public static String getLocal(NSection section, String language){
		return NewsRssFeeds.getLocal(section, language);
	}
	public static String getLocal(String term, String language){
		return NewsRssFeeds.getLocal(term, language);
	}
		
	//-------------------------------------
	
	//info
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.REST, Content.data, false);
		
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
			.addCustomAnswer("soccerResults", soccerResultsAns)
			.addCustomAnswer("soccerTable", soccerTableAns)
		.addAnswerParameters("topic")  
		;
		
		return info;
	}
	private static final String soccerResultsAns = "news_1b"; 		//TODO: improve
	private static final String soccerTableAns = "news_1c";

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//Button options:
		JSONObject optionsNoActions = JSON.make(ACTIONS.OPTION_SKIP_TTS, true, ACTIONS.OPTION_SKIP_TEXT, true, ACTIONS.OPTION_SKIP_ACTIONS, true);
		JSONObject optionsWithActions = JSON.make(ACTIONS.OPTION_SKIP_TTS, true, ACTIONS.OPTION_SKIP_TEXT, true);
						
		//parameter evaluation:
		
		//SPORTS TEAM
		Parameter sportsTeamP = nluResult.getOptionalParameter(PARAMETERS.SPORTS_TEAM, "");
		String sportsTeam = sportsTeamP.getDataFieldOrDefault(InterviewData.VALUE).toString();
		//System.out.println("SPORTS TEAM: " + sportsTeam); 		//debug
		
		//SPORTS TEAM
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
		
		//---------- Read league data -----------
		long tic = System.currentTimeMillis();
		
		//soccer results?
		if (!section.equals(NSection.soccer.name()) || !(type.equals(NType.results.name()) || type.equals(NType.table.name()))){
			//TODO: we are in the wrong place, how did we end-up here?
			return new NewsRssFeeds().getResult(nluResult);
		}
			
		//Only real support with detailed results is for Bundesliga right now
		boolean isBundesliga = (!sportsLeague.isEmpty() && sportsLeague.equals(SportsLeague.BUNDESLIGA)) 
				|| (!sportsTeam.isEmpty() && SportsTeam.isBundesligaTeam("<" + sportsTeam + ">"));
		
		//If it is Bundesliga we need the worker data
		if (isBundesliga &&	
				(Workers.openLigaWorkerBundesliga == null || Workers.openLigaWorkerBundesliga.getStatus() < 0)
			){
			//ABORT further processing with some generic data:
			
			String url = "http://m.sport1.de/fussball/";
			api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
			api.putActionInfo("url", url);
			api.hasAction = true;
			
			//card for URL
			Card card = new Card(Card.TYPE_SINGLE);
			card.addElement(ElementType.link, 
					JSON.make("title", "Sport1" + ":", "desc", "<i>\"" + getLocal(NSection.soccer, api.language) + "\"</i>"),
					null, null, "", 
					url, 
					Config.urlWebImages + "cards/search.png", 
					null, null);
			api.addCard(card.getJSON());
			
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
			ServiceResult result = api.buildResult();
					
			return result;
			//END
		}
		
		//TODO: make this variable, but we have only Bundesliga right now
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
		api.setStatusSuccess();
		
		//stats
		Statistics.addExternalApiHit("Soccer Bundesliga Info");
		Statistics.addExternalApiTime("Soccer Bundesliga Info", tic);
		
		//build the API_Result
		ServiceResult result = api.buildResult();
				
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
