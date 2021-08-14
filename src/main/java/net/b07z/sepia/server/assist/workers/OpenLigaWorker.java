package net.b07z.sepia.server.assist.workers;

import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.SportsLeague;
import net.b07z.sepia.server.assist.parameters.SportsTeam;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.ThreadManager;
import net.b07z.sepia.server.core.tools.ThreadManager.ThreadInfo;

public class OpenLigaWorker implements WorkerInterface {
	
	//common
	static String name = "OpenLigaDB-worker";
	ThreadInfo worker;
	int workerStatus = -1;				//-1: offline, 0: ready to start, 1: waiting for next action, 2: in action
	private String statusDesc = "";		//text description of status
	boolean abort = false;
	long startDelay = 4230;				//start worker after this delay
	long maxWait = 5000;				//maximum wait for kill method
	long waitInterval = 100;			//wait interval during kill request
	long nextRefresh = Long.MAX_VALUE;	//when will the service be refreshed next
	long averageRefreshTime = 10000;	//will be updated during worker runs
	long upperMaxRefreshWait = 15000;	//how long is the max wait time for a refresh
	int executedRefreshs = 0;			//how many times has it been executed after start() was called?	
	long lastUpdated = 0;				//when has the worker last done an update or tried to do one
	
	//specific
	HashSet<String> refreshFeeds;
	long customWaitInterval = 2602;			//custom wait time until the worker checks for an abort request and status changes
	public long customRefreshInterval = (5*60*60*1000);	//every 5h
	
	//even more specific for service
	public long matchTimeInterval = (10*60*1000);	//refresh data every 10min on a match day (if data changed)
	long initialErrorInterval = (5*60*1000);		//repeat every 5min if API gave an error and add the same amount on every consecutive fail (5, 10, 15, ...)
	long currentMatchDay = -1;
	long lastMatchDayUpdate = 0;
	long matchDayUpdateThreshold = (6*60*60*1000);	//6h, this should change only once or twice per week but just to be sure ...
	String lastDbRefreshDate = null;
	
	//Collected leagues data
	public static JSONObject openLigaData;				//data with current matches for all leagues
	public static final String SEASON = "season";
	public static final String LEAGUE = "league"; 		//for possible values see below in API section
	
	//variable
	public JSONObject workerData;
	public String league = ""; 			//league to observe
	public String season = "";			//season
	
	//construct
	public OpenLigaWorker(String league, String season){
		this.league = league;
		this.season = season;
		this.workerData = JSON.make(LEAGUE, league, SEASON, season);
	}
	
	@Override
	public void setup(){
		Debugger.println(name + ": Setting-up worker for league '" + this.league + "' and season '" + this.season + "'.", 3);
		
		if (openLigaData == null){
			openLigaData = new JSONObject();
		}
		//load backup
		JSONObject openLeagueData = JSON.readJsonFromFile(Workers.openLigaData_BackupFile);
		if (openLeagueData != null){
			JSONObject leagueData = (JSONObject) openLeagueData.get(league);
			if (leagueData != null){
				JSON.add(openLigaData, league, leagueData);
				Debugger.println(name + ": Backup restored with " + (leagueData.size()-1) + " match days.", 3);
			}
		}
		workerStatus = 0;
	}
	
	@Override
	public Object getData(String key){
		return workerData.get(key);
	}
	
	@Override
	public String getName(){
		return name;
	}
	
	@Override
	public int getStatus(){
		return workerStatus;
	}
	
	@Override
	public String getStatusDescription(){
		if (workerStatus == -1){
			statusDesc = "offline";
		}else if (workerStatus == 0){
			statusDesc = "ready to work";
		}else if (workerStatus == 1){
			statusDesc = "waiting for next action";
		}else if (workerStatus == 2){
			statusDesc = "refreshing data";
		}else{
			statusDesc = "unknown";
		}
		return statusDesc;
	}
	
	@Override
	public long getNextRefreshTime(){
		return nextRefresh; 
	}
	
	@Override
	public boolean kill(){
		abort = true;		//NOTE: once this flag is set it remains false and the worker is basically dead! Create a new instance afterwards.
		long thisWait = 0; 
		if (executedRefreshs != 0){
			while (workerStatus > 0){
				Debugger.sleep(waitInterval);
				thisWait += waitInterval;
				if (thisWait >= maxWait){
					break;
				}
			}
		}
		if (workerStatus < 1 || executedRefreshs == 0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void waitForWorker(){
		//if (nextRefresh > 100){	return;	}
		long thisWait = 0; 
		while (workerStatus == 2){
			Debugger.sleep(waitInterval);
			thisWait += waitInterval;
			if (thisWait >= Math.min(upperMaxRefreshWait, averageRefreshTime)){
				break;
			}
		}
	}
	
	@Override
	public void start(){
		start(startDelay);
	}
	@Override
	public void start(long customStartDelay){
		worker = ThreadManager.runForever(() -> {
	    	workerStatus = 1;
	    	Debugger.sleep(customStartDelay);
	    	long totalRefreshTime = 0;
	    	executedRefreshs = 0;
	    	long errorMatchDayRetry = 5000; 		//retry getCurrentMatchDay in ...
	    	long errorLastDbRefreshRetry = 5000;	//retry getLastDbRefresh in ...
	    	long errorDataRefreshRetry = 5000;		//retry getData in ...
	    	boolean isMatchDayOk = false;
	    	boolean isDataUpdatePossible = true;
	    	String newDbRefreshDate = "";
	    	if (!abort){
	    		Debugger.println(name + ": START", 3);
	    	}else{
	    		Debugger.println(name + ": CANCELED before start", 3);
	    	}
	    	while (!abort){
	    		workerStatus = 2;
		    	long tic = Debugger.tic();
		    	JSONArray newData = new JSONArray();
		    	
		    	//get data
		    	boolean error = false;
		    	//get current match-day 
		    	if (currentMatchDay < 1 || ((System.currentTimeMillis() - lastMatchDayUpdate) > matchDayUpdateThreshold)){
		    		long newMatchDay = getCurrentMatchDay(league);
		    		if (newMatchDay > 0 && (currentMatchDay != newMatchDay)){
		    			//load league season so far?
		    			if (currentMatchDay == -1){
		    				//this is not guaranteed to give a result but saves all content in league.content.JSONArray
		    				JSONObject leagueData = getSeasonSoFar((JSONObject) openLigaData.get(league), league, newMatchDay, season);
		    				JSON.add(openLigaData, league, leagueData);
		    			}
		    			//store new match day
		    			currentMatchDay = newMatchDay;
		    			//get next matches
		    			String matchDayID = season + "/" + (currentMatchDay + 1);
		    			JSONArray nextData = getNextMatchDayData(league, matchDayID);
		    			if (nextData == null){
		    				//try again
		    				errorMatchDayRetry = errorMatchDayRetry + initialErrorInterval;
			    			error = true;
			    			isMatchDayOk = false;
		    			}else{
		    				//reset error timers and update data + time stamp
		    				JSONObject leagueData = (JSONObject) openLigaData.get(league);
			    			if (leagueData == null){
			    				leagueData = new JSONObject();
			    			}
			    			JSON.add(leagueData, matchDayID, nextData);
			    			JSON.add(openLigaData, league, leagueData);
		    				//JSON.add(Config.openLigaDataNext, league, nextData.get(league));
			    			//JSON.add(Config.openLigaDataNext, league + "_info", JSON.make("matchDay", currentMatchDay+1, "leagueName", getLeagueName(league, ""), "leagueTag", league));
			    			errorMatchDayRetry = initialErrorInterval;
			    			lastMatchDayUpdate = System.currentTimeMillis();
			    			isMatchDayOk = true;
		    			}
		    		}else if (newMatchDay > 0 && (currentMatchDay == newMatchDay)){
		    			errorMatchDayRetry = initialErrorInterval;
		    			lastMatchDayUpdate = System.currentTimeMillis();
		    			isMatchDayOk = true;
		    		}else{
		    			errorMatchDayRetry = errorMatchDayRetry + initialErrorInterval;
		    			error = true;
		    			isMatchDayOk = false;
		    		}
		    	}else{
		    		isMatchDayOk = true;
		    	}
		    	//get last DB refresh date
		    	if (!error && isMatchDayOk){
		    		newDbRefreshDate = getLastDbRefresh(league, season + "/" + currentMatchDay);
		    		if (newDbRefreshDate == null){
		    			errorLastDbRefreshRetry = errorLastDbRefreshRetry + initialErrorInterval;
		    			error = true;
		    			isDataUpdatePossible = false;
		    		}else if (!newDbRefreshDate.equals(lastDbRefreshDate)){
		    			isDataUpdatePossible = true;
		    			errorLastDbRefreshRetry = initialErrorInterval;
		    		}else{
		    			isDataUpdatePossible = false;
		    			errorLastDbRefreshRetry = initialErrorInterval;
		    		}
		    	}
		    	//get API data
		    	if (!error && isDataUpdatePossible){
		    		String matchDayID = season + "/" + currentMatchDay;
		    		newData = getMatchDayData(league, matchDayID); 
		    		if (newData == null){
		    			errorDataRefreshRetry = errorDataRefreshRetry + initialErrorInterval;
		    			error = true;
		    		}else{
			    		//overwrite old data
		    			JSONObject leagueData = (JSONObject) openLigaData.get(league);
		    			if (leagueData == null){
		    				leagueData = new JSONObject();
		    			}
		    			JSONArray leagueDataContent = (JSONArray) leagueData.get("content");
		    			if (leagueDataContent == null){
		    				leagueDataContent = new JSONArray();
		    			}
		    			JSON.add(leagueData, matchDayID, newData);
		    			if (!leagueDataContent.contains(matchDayID)){
		    				JSON.add(leagueData, "content", JSON.add(leagueDataContent, matchDayID));
		    			}
		    			JSON.add(openLigaData, league, leagueData);
		    			JSON.add(openLigaData, league + "_info", JSON.make("matchDay", currentMatchDay, "matchDayID", matchDayID, "leagueName", getLeagueName(league, ""), "leagueTag", league));
			    		lastUpdated = System.currentTimeMillis();
			    		lastDbRefreshDate = newDbRefreshDate;
			    		errorDataRefreshRetry = initialErrorInterval;
			    		//rebuild table
			    		JSONArray table = getSoccerLeagueTable(league, season, currentMatchDay);
			    		if (table != null){
			    			JSON.add(openLigaData, league + "_table", table);
			    			JSON.add(openLigaData, league + "_table_info", JSON.make("matchDay", currentMatchDay));
			    		}
		    		}
		    	}
		    	
		    	//report
		    	long thisRefreshTime = (System.currentTimeMillis()-tic); 
		    	if (error){
		    		Statistics.addOtherApiHit("Worker ERRORS: " + name);
					Statistics.addOtherApiTime("Worker ERRORS: " + name, tic);
		    		Debugger.println(name + ": Data NOT updated! (" + executedRefreshs + " time(s)) Try took (ms): " + thisRefreshTime + ", average (ms): " + averageRefreshTime, 3);
		    	}else{
		    		totalRefreshTime += thisRefreshTime;
			    	executedRefreshs++;
			    	averageRefreshTime = (long)((double)totalRefreshTime/(double)executedRefreshs);
			    	Statistics.addOtherApiHit("Worker: " + name);
					Statistics.addOtherApiTime("Worker: " + name, tic);
		    		Debugger.println(name + ": Data has been updated! (" + executedRefreshs + " time(s)) It took (ms): " + thisRefreshTime + ", average (ms): " + averageRefreshTime, 3);
		    		//JSON.printJSONpretty(Config.openLigaData); 		//debug
		    	}
				
				//set next interval
				long currentInterval = Long.MAX_VALUE;
				//Error timers
				if (error){
					if (!isMatchDayOk){
						currentInterval = errorMatchDayRetry;
					}else if (!isDataUpdatePossible){
						currentInterval = errorLastDbRefreshRetry;
					}else{
						currentInterval = errorDataRefreshRetry;
					}
				//Normal cycle
				}else{
					//TODO: this is a bit tricky and needs more testing
					long nextMatchTime = activeOrNextMatchTime.get(league);
					long nextMatchWait = nextMatchTime - System.currentTimeMillis();
					if (isDataUpdatePossible){
						currentInterval = matchTimeInterval;
					}else if (Math.abs(nextMatchWait) < (1000*60*60*3)){
						currentInterval = matchTimeInterval;
					}else if (nextMatchWait > 0){
						currentInterval = Math.min(customRefreshInterval, nextMatchWait);
					}else{
						currentInterval = customRefreshInterval;
					}
				}
		    	//debug
				/*
		    	System.out.println("OpenLigaDB - error: " + error);
		    	System.out.println("OpenLigaDB - isMatchDayOk: " + isMatchDayOk);
		    	System.out.println("OpenLigaDB - currentMatchDay: " + currentMatchDay + ", lastMatchDayUpdate: " + lastMatchDayUpdate);
		    	System.out.println("OpenLigaDB - isDataUpdatePossible: " + isDataUpdatePossible);
		    	System.out.println("OpenLigaDB - newDbRefreshDate: " + newDbRefreshDate);
		    	System.out.println("OpenLigaDB - lastUpdated: " + lastUpdated);
		    	System.out.println("OpenLigaDB - lastDbRefreshDate: " + lastDbRefreshDate);
		    	System.out.println("OpenLigaDB - currentInterval: " + currentInterval);
		    	*/
				
				//wait for next interval
				workerStatus = 1;
				long thisWait = 0; 
				while(!abort && (thisWait < currentInterval)){
					nextRefresh = currentInterval-thisWait;
					Debugger.sleep(customWaitInterval);
					thisWait += customWaitInterval;
				}
	    	}
	    	workerStatus = 0;
	    });
	}
	
	//----------------- API -------------------
	
	public static final String BUNDESLIGA = "bl1";
	public static final String BUNDESLIGA_SEASON = "2021";		//TODO: update automatically after season?
	public static final String BUNDESLIGA_2 = "bl2";
	public static final String DFB_POKAL = "DFB";
	public static final String PRIMERA_DIVISION = "PD";
	public static final String PREMIER_LEAGUE = "PL";
	public static final String CHAMPIONS_LEAGUE_16 = "cl1617"; 	//TODO: update (and use?)
	
	public static HashMap<String, String> openLigaDB_mapping = new HashMap<>();
	static{
		openLigaDB_mapping.put(SportsLeague.BUNDESLIGA, BUNDESLIGA);
		openLigaDB_mapping.put(SportsLeague.BUNDESLIGA_2, BUNDESLIGA_2);
		openLigaDB_mapping.put(SportsLeague.DFB_POKAL, DFB_POKAL);
		openLigaDB_mapping.put(SportsLeague.PRIMERA_DIVISION, PRIMERA_DIVISION);
		openLigaDB_mapping.put(SportsLeague.PREMIER_LEAGUE, PREMIER_LEAGUE);
		openLigaDB_mapping.put(SportsLeague.CHAMPIONS_LEAGUE, CHAMPIONS_LEAGUE_16);
	}
	public static HashMap<String, Long> activeOrNextMatchTime = new HashMap<>();
	static{
		activeOrNextMatchTime.put(BUNDESLIGA, Long.MAX_VALUE);
		activeOrNextMatchTime.put(BUNDESLIGA_2, Long.MAX_VALUE);
		activeOrNextMatchTime.put(DFB_POKAL, Long.MAX_VALUE);
		activeOrNextMatchTime.put(PRIMERA_DIVISION, Long.MAX_VALUE);
		activeOrNextMatchTime.put(PREMIER_LEAGUE, Long.MAX_VALUE);
		activeOrNextMatchTime.put(CHAMPIONS_LEAGUE_16, Long.MAX_VALUE);
	}
	public static HashMap<String, String> leagueNames = new HashMap<>();
	static{
		leagueNames.put(BUNDESLIGA, "1. Bundesliga");
		leagueNames.put(BUNDESLIGA_2, "2. Bundesliga");
		leagueNames.put(DFB_POKAL, "DFB Pokal");
		leagueNames.put(PRIMERA_DIVISION, "Primera Division");
		leagueNames.put(PREMIER_LEAGUE, "Premier League");
		leagueNames.put(CHAMPIONS_LEAGUE_16, "Champions League");
	}
	/**
	 * Get league name for openLiga API tag, e.g. "bl1".
	 */
	public static String getLeagueName(String key, String language){
		String leagueName = leagueNames.get(key);
		if (leagueName != null && !leagueName.isEmpty()){
			return leagueName;
		}else{
			Debugger.println(name + " - getLeagueName() - missing name for: " + key, 1);
			return "";
		}
	}
	
	/**
	 * Checks the API data for changes to decide if we should call the getMatchDayData method.
	 * @param leagueTag - as required by API, e.g. "bl1"
	 * @param matchDay - as required by API, e.g. "2016/12"
	 * @return Date like "2016-11-26T20:20:49.587" of the last database update or null
	 */
	public static String getLastDbRefresh(String leagueTag, String matchDay){
		String lastRefreshDate = "";
		String url = "https://www.openligadb.de/api/getlastchangedate/" + leagueTag + "/" + matchDay;
		try{
			lastRefreshDate = Connectors.simpleHtmlGet(url);
			if (lastRefreshDate.matches(".*\\d\\d\\d\\d-.*")){
				return lastRefreshDate;
			}else{
				Debugger.println(name + " - getLastDbRefresh() - unexpected result: " + lastRefreshDate, 1);
				return null;
			}
		}catch (Exception e){
			Debugger.println(name + " - getLastDbRefresh() - failed: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 2);
			return null;
		}
	}
	/**
	 * Get the current matchDay for the given league.
	 * @param leagueTag - as required by API, e.g. "bl1"
	 * @return positive integer for matchDay or -1 on error 
	 */
	public static long getCurrentMatchDay(String leagueTag){
		long matchDay = -1;
		String url = "https://www.openligadb.de/api/getcurrentgroup/" + leagueTag;
		try{
			matchDay = (long) Connectors.simpleJsonGet(url).get("GroupOrderID");
			if (matchDay > 0){
				return matchDay;
			}else{
				Debugger.println(name + " - getCurrentMatchDay() - unexpected result: " + matchDay, 1);
				return -1;
			}
		}catch (Exception e){
			Debugger.println(name + " - getCurrentMatchDay() - failed: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 2);
			return -1;
		}
	}
	/**
	 * Get the match-day data for the given league.
	 * @param leagueTag - as required by API, e.g. "bl1"
	 * @param matchDay - as required by API, e.g. "2016/12"
	 * @return JSONArray that holds all matches of the current match day ... or null 
	 */
	public static JSONArray getMatchDayData(String leagueTag, String matchDay){
		JSONArray apiResult = null;
		String url = "https://www.openligadb.de/api/getmatchdata/" + leagueTag + "/" + matchDay;
		try{
			JSONObject result = Connectors.httpGET(url);
			apiResult = (JSONArray) result.get("JSONARRAY");
			if (apiResult != null && !apiResult.isEmpty()){
				//System.out.println("OpenLigaDB API res.: " + apiResult.toJSONString()); 		//debug
				JSONArray leagueData = new JSONArray();
				long activeOrNextMatchUnixTime = Long.MAX_VALUE;
				String nextMatchDate = "";
				for (Object o : apiResult){
					JSONObject matchInput = (JSONObject) o;
					JSONObject matchOut = new JSONObject();
					String date = (String) matchInput.get("MatchDateTime"); 	//2016-12-02T20:30:00 - TODO: this HAS TO BE GMT! Is it?
					JSON.add(matchOut, "kickoff", date);
					long dateUNIX = DateTimeConverters.getUnixTimeOfDateGMT(date.replaceFirst("T", "_"), "yyyy-MM-dd_HH:mm:ss");
					if ((dateUNIX > System.currentTimeMillis()) || (System.currentTimeMillis() - dateUNIX) < (1000*60*30*5)){
						if (dateUNIX < activeOrNextMatchUnixTime){
							activeOrNextMatchUnixTime = dateUNIX;
							nextMatchDate = date;
						}
					}
					JSON.add(matchOut, "kickoffUNIX", dateUNIX);
					boolean isFinished = (boolean) matchInput.get("MatchIsFinished");
					JSON.add(matchOut, "isFinished", isFinished);
					String team1 = (String) ((JSONObject) matchInput.get("Team1")).get("TeamName");
					String team2 = (String) ((JSONObject) matchInput.get("Team2")).get("TeamName");
					JSON.add(matchOut, "team1", team1);
					JSON.add(matchOut, "id1", ((JSONObject) matchInput.get("Team1")).get("TeamId"));
					JSON.add(matchOut, "team2", team2);
					JSON.add(matchOut, "id2", ((JSONObject) matchInput.get("Team2")).get("TeamId"));
					JSONArray goals = (JSONArray) matchInput.get("Goals");
					long goals1 = 0;
					long goals2 = 0;
					if (goals != null && !goals.isEmpty()){
						Object matchMinute = ((JSONObject) goals.get(goals.size()-1)).get("MatchMinute");
						if (matchMinute != null){
							goals1 = (long) ((JSONObject) goals.get(goals.size()-1)).get("ScoreTeam1");
							goals2 = (long) ((JSONObject) goals.get(goals.size()-1)).get("ScoreTeam2");
						}
					}
					//System.out.println(team1 + " - " + team2 + " " + goals1 + ":" + goals2);
					JSON.add(matchOut, "goals1", goals1);
					JSON.add(matchOut, "goals2", goals2);
					if (isFinished){
						if (goals1 > goals2){
							JSON.add(matchOut, "result1", "W");
							JSON.add(matchOut, "result2", "L");
						}else if (goals1 < goals2){
							JSON.add(matchOut, "result1", "L");
							JSON.add(matchOut, "result2", "W");
						}else{
							JSON.add(matchOut, "result1", "D");
							JSON.add(matchOut, "result2", "D");
						}
					}else{
						JSON.add(matchOut, "result1", "");
						JSON.add(matchOut, "result2", "");
					}
					String summary = "";
					if((System.currentTimeMillis() - dateUNIX) > 0){
						summary = DateTimeConverters.convertDateFormat(date.replaceFirst("T", "_"), "yyyy-MM-dd_HH:mm:ss", "dd.MM.' - 'HH:mm'h'") 
								+ ", " + team1 + " - " + team2 + ", " + goals1 + ":" + goals2;
					}else{
						summary = DateTimeConverters.convertDateFormat(date.replaceFirst("T", "_"), "yyyy-MM-dd_HH:mm:ss", "dd.MM.' - 'HH:mm'h'") 
								+ ", " + team1 + " - " + team2 + ", " + "-" + ":" + "-";
					}
					JSON.add(matchOut, "summary", summary);
					
					JSON.add(leagueData, matchOut);
				}
				if (activeOrNextMatchUnixTime < Long.MAX_VALUE){
					activeOrNextMatchTime.put(leagueTag, activeOrNextMatchUnixTime);
					Debugger.println(name + " - League: " + leagueTag + ", active or next match: " + nextMatchDate + " (" + activeOrNextMatchUnixTime + ")", 3);
				}
				//System.out.println("OpenLigaDB Current Matches: " + leagueData.toJSONString()); 		//debug
				return leagueData;
			}else{
				Debugger.println(name + " - getMatchDayData() - unexpected result: " + result, 1);
				return null;
			}
		}catch (Exception e){
			Debugger.println(name + " - getMatchDayData() - failed: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 2);
			return null;
		}
	}
	//TODO: as quick solution I just copied getMatchDayData and remove some field ... this could be done better ;-) 
	/**
	 * Get the next match-day data for the given league.
	 * @param leagueTag - as required by API, e.g. "bl1"
	 * @param matchDay - as required by API, e.g. "2016/12"
	 * @return JSONArray for the games ... or null 
	 */
	public static JSONArray getNextMatchDayData(String leagueTag, String matchDay){
		JSONArray apiResult = null;
		String url = "https://www.openligadb.de/api/getmatchdata/" + leagueTag + "/" + matchDay;
		try{
			JSONObject result = Connectors.httpGET(url);
			apiResult = (JSONArray) result.get("JSONARRAY");
			if (apiResult == null && !result.containsKey("STRING")){
				Debugger.println(name + " - getNextMatchDayData() - unexpected result: " + result, 1);
				return null;
			}else if (result.containsKey("STRING") || apiResult.isEmpty()){
				return new JSONArray();
			}else{
				//System.out.println("OpenLigaDB API res.: " + apiResult.toJSONString()); 		//debug
				JSONArray leagueData = new JSONArray();
				long nextMatchUnixTime = Long.MAX_VALUE;
				for (Object o : apiResult){
					JSONObject matchInput = (JSONObject) o;
					JSONObject matchOut = new JSONObject();
					String date = (String) matchInput.get("MatchDateTime"); 	//2016-12-02T20:30:00 - TODO: this HAS TO BE GMT! Is it?
					JSON.add(matchOut, "kickoff", date);
					long dateUNIX = DateTimeConverters.getUnixTimeOfDateGMT(date.replaceFirst("T", "_"), "yyyy-MM-dd_HH:mm:ss");
					if (dateUNIX > System.currentTimeMillis()){
						nextMatchUnixTime = Math.min(nextMatchUnixTime, dateUNIX);
					}
					JSON.add(matchOut, "kickoffUNIX", dateUNIX);
					boolean isFinished = (boolean) matchInput.get("MatchIsFinished");
					JSON.add(matchOut, "isFinished", isFinished);
					String team1 = (String) ((JSONObject) matchInput.get("Team1")).get("TeamName");
					String team2 = (String) ((JSONObject) matchInput.get("Team2")).get("TeamName");
					JSON.add(matchOut, "team1", team1);
					JSON.add(matchOut, "id1", ((JSONObject) matchInput.get("Team1")).get("TeamId"));
					JSON.add(matchOut, "team2", team2);
					JSON.add(matchOut, "id2", ((JSONObject) matchInput.get("Team2")).get("TeamId"));
					String summary = DateTimeConverters.convertDateFormat(date.replaceFirst("T", "_"), "yyyy-MM-dd_HH:mm:ss", "dd.MM.' - 'HH:mm'h'") 
							+ ", " + team1 + " - " + team2 + ", " + "-" + ":" + "-";
					JSON.add(matchOut, "summary", summary);
					
					JSON.add(leagueData, matchOut);
				}
				//System.out.println("OpenLigaDB Next Matches: " + leagueData.toJSONString()); 		//debug
				return leagueData;
			}
		}catch (Exception e){
			Debugger.println(name + " - getNextMatchDayData() - failed: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 2);
			return null;
		}
	}
	
	/**
	 * Get all (missing) matchDays of a league and season up to the currentMatchDay.
	 * @param leagueData - existing data or null, if not null it will be updated
	 * @param league - tag for league
	 * @param currentMatchDay - current match day
	 * @param season - season, e.g. 2016
	 * @return the league match days up to current
	 */
	public static JSONObject getSeasonSoFar(JSONObject leagueData, String league, long currentMatchDay, String season){
		JSONArray leagueDataContent;
		JSONArray addedContent = new JSONArray();
		if (leagueData == null || leagueData.isEmpty()){
			leagueData = new JSONObject();
			leagueDataContent = new JSONArray();
		}else{
			leagueDataContent = (JSONArray) leagueData.get("content");
			if (leagueDataContent == null){
				leagueDataContent = new JSONArray();
			}
		}
		String matchDayID = "";
		JSONArray matchDayData;
		for (long d=1; d<=currentMatchDay; d++){
			matchDayID = season + "/" + d;
			if (!leagueDataContent.contains(matchDayID)){
				matchDayData = getMatchDayData(league, matchDayID);
				if (matchDayData != null){
					JSON.add(leagueData, matchDayID, matchDayData);
					JSON.add(leagueDataContent, matchDayID);
					JSON.add(addedContent, matchDayID);
				}
			}
		}
		JSON.add(leagueData, "content", leagueDataContent);
		if (!addedContent.isEmpty()){
			Debugger.println(name + " - getSeasonSoFar() - added: " + addedContent, 3);
		}
		return leagueData;
	}
	
	/**
	 * Get table of a soccer league and season up to match day.<br> 
	 * Form: "id":{"team":"FC XY","games":8,"points":20,"won":6,"draw":2,"lost":0,"goals":12,"goals_agains":4}<br>
	 * JSONObject or null if data is incomplete, note: data may be imprecise in case of same points and goals.
	 */
	public static JSONArray getSoccerLeagueTable(String league, String season, long matchDay){
		try{
			JSONObject leagueData = (JSONObject) openLigaData.get(league);
			JSONObject teams = new JSONObject();
			for (long d=1; d <= matchDay; d++){
				String matchDayTag = season + "/" + d;
				JSONArray matchDayData = (JSONArray) leagueData.get(matchDayTag);
				for (Object o : matchDayData){
					JSONObject match = (JSONObject) o;
					boolean isFinished = (boolean) match.get("isFinished");
					if (isFinished){
						long id1 = (long) match.get("id1");
						String team1 = (String) match.get("team1");
						String team2 = (String) match.get("team2");
						long id2 = (long) match.get("id2");
						long goals1 = (long) match.get("goals1");
						long goals2 = (long) match.get("goals2");
						String res1 = (String) match.get("result1");
						long points1 = 0;	long points2 = 0;
						long won1 = 0;		long won2 = 0;
						long draw1 = 0;		long draw2 = 0;
						long lost1 = 0;		long lost2 = 0;
						if (res1.equals("W")){
							points1 = 3;
							won1 = 1;		lost2 = 1;
						}else if (res1.equals("D")){
							points1 = 1;	points2 = 1;
							draw1 = 1;		draw2 = 1;
						}else{
							points2 = 3;
							won2 = 1;		lost1 = 1;
						}
						teamAddStats(teams, id1, team1, points1, goals1, goals2, won1, draw1, lost1);
						teamAddStats(teams, id2, team2, points2, goals2, goals1, won2, draw2, lost2);
					}
				}
			}
			//make array and sort result
			JSONArray sortedTable = new JSONArray();
			for (Object teamO : teams.values()){
				JSON.add(sortedTable, teamO);
			}
			JSON.sortArrayInverseByLong(sortedTable, "points", "goal_diff");
			/* debug
			for (Object o : sortedTable){
				JSONObject jo = (JSONObject) o;
				System.out.println("team: " + jo.get("team") + " - points: " + jo.get("points") + " - goal_diff: " + jo.get("goal_diff"));
			}*/
			//return teams;
			return sortedTable;
		
		}catch (Exception e){
			Debugger.println(name + " - getLeagueTable() failed with msg: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 2);
			return null;
		}
	}
	private static JSONObject teamAddStats(JSONObject teams, long id, String teamName, long addPoints, long addGoals, long addGoalsAgainst, long addWon, long addDraw, long addLost){
		JSONObject team = (JSONObject) teams.get(String.valueOf(id));
		if (team == null){
			team = new JSONObject();
			if (teamName.length() > 18){
				teamName = SportsTeam.shortNames.get(id);
			}
			JSON.add(team, "id", id);
			JSON.add(team, "team", teamName);
			JSON.add(team, "games", addWon + addDraw + addLost);
			JSON.add(team, "won", addWon);
			JSON.add(team, "draw", addDraw);
			JSON.add(team, "lost", addLost);
			JSON.add(team, "points", addPoints);
			JSON.add(team, "goals", addGoals);
			JSON.add(team, "goals_against", addGoalsAgainst);
			JSON.add(team, "goal_diff", addGoals - addGoalsAgainst);
		}else{
			JSON.add(team, "games", ((long) team.get("games")) + (addWon + addDraw + addLost));
			JSON.add(team, "won", ((long) team.get("won")) + addWon);
			JSON.add(team, "draw", ((long) team.get("draw")) + addDraw);
			JSON.add(team, "lost", ((long) team.get("lost")) + addLost);
			JSON.add(team, "points", ((long) team.get("points")) + addPoints);
			JSON.add(team, "goals", ((long) team.get("goals")) + addGoals);
			JSON.add(team, "goals_against", ((long) team.get("goals_against")) + addGoalsAgainst);
			JSON.add(team, "goal_diff", ((long) team.get("goal_diff")) + (addGoals - addGoalsAgainst));
		}
		JSON.add(teams, String.valueOf(id), team); 
		return teams;
	}

}
