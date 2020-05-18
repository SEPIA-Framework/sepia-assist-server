package net.b07z.sepia.server.assist.assistant;

import net.b07z.sepia.server.assist.parameters.TravelType;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * Class used to build direct-commands that can for example be used for button-actions.
 * 
 * @author Florian Quirin
 *
 */
public class CmdBuilder {
	
	//-------- pre-build command summaries ---------

	public static String getWayHomeInfo(String travelType, String language){
		String cmdSum = CMD.DIRECTIONS + ";;" 
				+ PARAMETERS.LOCATION_START + "=<user_location>;;"
				+ PARAMETERS.LOCATION_END + "=<user_home>;;"
				+ PARAMETERS.TRAVEL_REQUEST_INFO + "=<duration>;;";
		if (!travelType.isEmpty()){
			cmdSum += PARAMETERS.TRAVEL_TYPE + "={\"value\":\"" + travelType + "\",\"value_local\":\"" + TravelType.getLocal("<" + travelType + ">", language) + "\"};;";
		}
		return cmdSum;
	}

	public static String getWayToWorkInfo(String travelType, String language){
		String cmdSum = CMD.DIRECTIONS + ";;" 
				+ PARAMETERS.LOCATION_START + "=<user_location>;;"
				+ PARAMETERS.LOCATION_END + "=<user_work>;;"
				+ PARAMETERS.TRAVEL_REQUEST_INFO + "=<duration>;;";
		if (!travelType.isEmpty()){
			cmdSum += PARAMETERS.TRAVEL_TYPE + "={\"value\":\"" + travelType + "\",\"value_local\":\"" + TravelType.getLocal("<" + travelType + ">", language) + "\"};;";
		}
		return cmdSum;
	}

	public static String getSoccerTable(String league){
		String cmdSum = CMD.NEWS + ";;" 
				+ PARAMETERS.NEWS_TYPE + "={\"value\":\"table\"};;"
				+ PARAMETERS.NEWS_SECTION + "={\"value\":\"soccer\"};;";
		if (league != null && !league.isEmpty()){
			cmdSum += PARAMETERS.SPORTS_LEAGUE + "={\"value\":\"" + league + "\"};;";
		}
		return cmdSum;
	}

	public static String getSoccerResults(String league, String team){
		String cmdSum = CMD.NEWS + ";;" 
				+ PARAMETERS.NEWS_TYPE + "={\"value\":\"results\"};;"
				+ PARAMETERS.NEWS_SECTION + "={\"value\":\"soccer\"};;";
		if (team != null && !team.isEmpty()){
			cmdSum += PARAMETERS.SPORTS_TEAM + "={\"value\":\"" + team + "\"};;";
		}
		if (league != null && !league.isEmpty()){
			cmdSum += PARAMETERS.SPORTS_LEAGUE + "={\"value\":\"" + league + "\"};;";
		}
		return cmdSum;
	}

	public static String getFood(String foodType){
		return CMD.FOOD + ";;" 
				+ PARAMETERS.FOOD_CLASS + "=;;" 
				+ PARAMETERS.FOOD_ITEM + "=;;";
	}

	public static String getBrunchLocation(){
		return CMD.LOCATION + ";;" 
				+ PARAMETERS.SEARCH + "=<user_location>;;" 
				+ PARAMETERS.POI + "=brunch;;";
	}
	
	public static String getRestaurantLocation(){
		return CMD.LOCATION + ";;" 
				+ PARAMETERS.SEARCH + "=<user_location>;;" 
				+ PARAMETERS.POI + "=restaurant;;";
	}

	public static String getNews(String newsSection){
		String cmdSum =  CMD.NEWS + ";;";
		if (newsSection != null && !newsSection.isEmpty()){
			cmdSum += PARAMETERS.NEWS_SECTION + "={\"value\":\"" + newsSection + "\"};;";
		}
		return cmdSum;
	}

	public static String getWebSearch(String search){
		return CMD.WEB_SEARCH + ";;" 
				+ PARAMETERS.WEBSEARCH_REQUEST + "=" + search;
	}

	public static String getWiki(String search){
		return CMD.KNOWLEDGEBASE + ";;" 
				+ PARAMETERS.SEARCH + "=" + search;
	}

	public static String getRadioStation(String station){
		//return CMD.MUSIC_RADIO + ";;" + PARAMETERS.RADIO_STATION + "={\"value\":\"" + station + "\"};;";
		return CMD.MUSIC_RADIO + ";;" 
				+ PARAMETERS.RADIO_STATION + "=" + station + ";;";
	}
	public static String getRadioGenre(String genre){
		//return CMD.MUSIC_RADIO + ";;" + PARAMETERS.RADIO_STATION + "={\"value\":\"" + station + "\"};;";
		return CMD.MUSIC_RADIO + ";;" 
				+ PARAMETERS.MUSIC_GENRE + "=" + genre + ";;";
	}

	public static String getTvProgram(String time){
		return CMD.TV_PROGRAM + ";;";
	}

	public static String getCinemaProgram(){
		return CMD.NEWS + ";;";
	}

}
