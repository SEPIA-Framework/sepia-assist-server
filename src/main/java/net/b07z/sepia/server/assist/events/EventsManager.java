package net.b07z.sepia.server.assist.events;

import java.util.Calendar;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.CmdBuilder;
import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.events.EventLabels.Constants;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.parameters.SportsLeague;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.workers.OpenLigaWorker;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Method to control events on end-point call.
 * 
 * @author Florian Quirin
 *
 */
public class EventsManager {
	
	//Constants
	private static final long nextCheckIn = 10*60*1000;//6*60*60*1000;
	
	//Ids
	public static long ID_NPS = 100001l; 
	
	/**
	 * Build the basic events.
	 */
	public static JSONObject buildCommonEvents(NluInput input){
		//event actions
		ServiceBuilder actionBuilder = new ServiceBuilder();  //use this only to build actions
		
		//User local environment
		//
		//time
		boolean localTimeIsKnown = true;
		Calendar myDate = DateTimeConverters.getUserCalendar(input);
		if (myDate == null){
			localTimeIsKnown = false;
			Debugger.println("buildCommonEvents() - Local calendar could not be set for client: " + input.clientInfo, 1);
			myDate = Calendar.getInstance();		//should be user-local date
		}
		int dow = myDate.get(Calendar.DAY_OF_WEEK);
		//int wom = myDate.get(Calendar.WEEK_OF_MONTH);
		int hod = myDate.get(Calendar.HOUR_OF_DAY);
		int moh = myDate.get(Calendar.MINUTE);
		boolean isWeekend = (dow == Calendar.SATURDAY) || (dow == Calendar.SUNDAY);
		boolean isMorning = (hod >= 5 && hod <= 10);
		boolean isEvening = (hod >= 18 && hod <= 23);
		boolean isNight = (hod >= 22 || hod <= 4);
		boolean isLunchTime = (hod >= 11 && hod <= 16);
		boolean isBrunchTime = (hod >= 9 && hod <= 12);
		boolean isWeekendLunchTime = (hod >= 12 && hod <= 17);
		boolean isTimeToWork = !isWeekend && (hod >= 5 && hod <= 11);
		//
		//place
		boolean locationIsKnown = true;
		double lat = Converters.obj2Double(input.user.getCurrentLocation(LOCATION.LAT));
		double lng = Converters.obj2Double(input.user.getCurrentLocation(LOCATION.LNG));
		if (lat == Double.NEGATIVE_INFINITY || lng == Double.NEGATIVE_INFINITY){
			locationIsKnown = false;
		}
		if (locationIsKnown){
			input.user.loadInfoFromAccount(Config.superuserApiMng, ACCOUNT.USER_HOME, ACCOUNT.USER_WORK);
		}
		boolean homeIsKnown = true;
		double latHome = Converters.obj2Double(input.user.getHomeLocation(LOCATION.LAT));
		double lngHome = Converters.obj2Double(input.user.getHomeLocation(LOCATION.LNG));
		if (latHome == Double.NEGATIVE_INFINITY || lngHome == Double.NEGATIVE_INFINITY){
			homeIsKnown = false;
		}
		double distanceToHome = -1.0f;
		if (locationIsKnown && homeIsKnown){
			distanceToHome = Math.sqrt(Math.pow(lat-latHome,2) + Math.pow(lng-lngHome,2));
		}
		boolean workIsKnown = true;
		double latWork = Converters.obj2Double(input.user.getWorkLocation(LOCATION.LAT));
		double lngWork = Converters.obj2Double(input.user.getWorkLocation(LOCATION.LNG));
		if (latWork == Double.NEGATIVE_INFINITY || lngWork == Double.NEGATIVE_INFINITY){
			workIsKnown = false;
		}
		double distanceToWork = -1.0f;
		if (locationIsKnown && workIsKnown){
			distanceToWork = Math.sqrt(Math.pow(lat-latWork,2) + Math.pow(lng-lngWork,2));
		}
		boolean isAtHome = (distanceToHome <= 0.01) && (distanceToHome > -1.0f);
		boolean isAtWork = false;
		if (!isAtHome){
			isAtWork = (distanceToWork <= 0.01) && (distanceToWork > -1.0f);
		}
		//System.out.println("Events: " + locationIsKnown + ", " + homeIsKnown + ", " + workIsKnown + ", " + distanceToHome); 		//debug
		
		//events start
		boolean hasButtons = false;
		actionBuilder.addAction(ACTIONS.EVENTS_START);
		actionBuilder.putActionInfo("info", "dividerWithTime");
		//actionBuilder.actionInfo_put_info("info", "divider");
		//actionBuilder.actionInfo_put_info("info", "quietText");
		//actionBuilder.actionInfo_put_info("text", "Events: ");
		
		//random messages to entertain the user
		if (localTimeIsKnown){
			addScheduledMessage(actionBuilder, EventDialogs.Type.randomMotivationMorning.name(), ((10l - hod) * 60l - (30l - moh)) * 60l * 1000l,
					EventDialogs.getMessage(EventDialogs.Type.randomMotivationMorning, input.language));
			addScheduledMessage(actionBuilder, EventDialogs.Type.haveLunch.name(), ((13l - hod) * 60l - moh) * 60l * 1000l,
					EventDialogs.getMessage(EventDialogs.Type.haveLunch, input.language));
			addScheduledMessage(actionBuilder, EventDialogs.Type.makeCoffebreak.name(), ((16l - hod) * 60l - moh) * 60l * 1000l,
					EventDialogs.getMessage(EventDialogs.Type.makeCoffebreak, input.language));
			addScheduledMessage(actionBuilder, EventDialogs.Type.beActive.name(), ((18l - hod) * 60l - moh) * 60l * 1000l,
					EventDialogs.getMessage(EventDialogs.Type.beActive, input.language));
		}
		
		//news buttons
		if (localTimeIsKnown && (isMorning || (isWeekend && (hod >= 5 && hod <= 13)))){
			//common news
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.news_common, input.language), 
					CmdBuilder.getNews("")
			);
			actionBuilder.putActionInfo("options", JSON.make(
					ACTIONS.SKIP_TTS, true,
					ACTIONS.SHOW_VIEW, true
			)); 
			hasButtons = true;
		}
		
		//radio buttons
		if (localTimeIsKnown && (hod >= 5 && hod <= 21)){
			//radio egoFM
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.radio, input.language), 
					CmdBuilder.getRadio("ego fm")
			);
			actionBuilder.putActionInfo("options", JSON.make(
					ACTIONS.SKIP_TTS, true,
					ACTIONS.SHOW_VIEW, true
			));
			hasButtons = true;
		}else if (localTimeIsKnown && isNight){
			//radio chill-out
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.radio_night, input.language), 
					CmdBuilder.getRadio("night")
			);
			actionBuilder.putActionInfo("options", JSON.make(
					ACTIONS.SKIP_TTS, true,
					ACTIONS.SHOW_VIEW, true
			));
			hasButtons = true;
		}
		
		//navigation buttons
		if (locationIsKnown && homeIsKnown && !isAtHome){
			//to home
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.duration_home, input.language), 
					CmdBuilder.getWayHomeInfo("", input.language)
			);
			hasButtons = true;
		}
		if (locationIsKnown && workIsKnown && !isAtWork && isTimeToWork){
			//to work
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.duration_work, input.language), 
					CmdBuilder.getWayToWorkInfo("", input.language)
			);
			hasButtons = true;
		}
		
		//food buttons
		if (localTimeIsKnown && ((!isWeekend && isLunchTime) || (isWeekend && isWeekendLunchTime))){
			//lunch
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.lunch, input.language), 
					CmdBuilder.getFood("")
			);
			//recipe of the day (change active time to morning?)
			actionBuilder.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
			actionBuilder.putActionInfo("url", "https://www.chefkoch.de/rezepte/was-koche-ich-heute/"); 
			//interesting? 
			//https://www.chefkoch.de/rezepte/zufallsrezept/
			//https://www.chefkoch.de/rezept-des-tages.php
			actionBuilder.putActionInfo("title", EventLabels.getLabel(Constants.recipe_of_the_day, input.language));
			hasButtons = true;
		}
		if (localTimeIsKnown && isWeekend && isBrunchTime){
			//brunch
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.brunch, input.language), 
					CmdBuilder.getBrunchLocation()
			);
			hasButtons = true;
		}
		
		//TV program
		if (localTimeIsKnown && isEvening){
			//evening TV
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.tv_program, input.language), 
					CmdBuilder.getTvProgram("")
			);
			//series news
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.tv_series, input.language), 
					CmdBuilder.getNews("tv")
			);
			hasButtons = true;
		}
		
		//Cinema
		if (localTimeIsKnown && isWeekend && (hod >= 16 && hod <= 24)){
			//cinema news
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.cinema_news, input.language), 
					CmdBuilder.getNews("cinema")
			);
			hasButtons = true;
		}
		
		//soccer buttons Bundesliga
		if (Math.abs(OpenLigaWorker.activeOrNextMatchTime.get(OpenLigaWorker.BUNDESLIGA) - System.currentTimeMillis()) < (9*60*60*1000)){
			//Bundesliga button
			addCommandButton(actionBuilder, EventLabels.getLabel(Constants.bundesliga_results, input.language), 
					CmdBuilder.getSoccerResults(SportsLeague.BUNDESLIGA, "")
			);
			hasButtons = true;
		}
		
		//NPS Feedback
		/*
		boolean isFeedbackDay = ((wom == 2) || (wom == 4)) && ((dow == Calendar.FRIDAY) || (dow == Calendar.SATURDAY));
		if (isFeedbackDay){
			actionBuilder.actionInfo_add_action(ACTIONS.SCHEDULE_CMD);
			actionBuilder.actionInfo_put_info("info", "direct_cmd");
			actionBuilder.actionInfo_put_info("cmd", CMD.FEEDBACK_NPS + ";;");
			actionBuilder.actionInfo_put_info("visibility", "inputHidden");		//TODO: deprecated - use options
			actionBuilder.actionInfo_put_info("waitForIdle", "true");
			actionBuilder.actionInfo_put_info("idleTime", 24*1000);
			actionBuilder.actionInfo_put_info("eventId", ID_NPS);
			actionBuilder.actionInfo_put_info("tryMax", "2");
		}
		*/
		
		//if there are no buttons remove the divider too
		if (!hasButtons){
			actionBuilder.removeAction(0);
		}

		//next check in ...
		long nextCheck = nextCheckIn;
		
		JSONObject ans = new JSONObject();
			JSON.add(ans, "result", "success");
			JSON.add(ans, "actions", actionBuilder.actionInfo);
			JSON.add(ans, "nextCheck", nextCheck);
			
		return ans;
	}
	
	/**
	 * Add a default command-button to actions
	 */
	private static void addCommandButton(ServiceBuilder actionBuilder, String title, String cmd){
		actionBuilder.addAction(ACTIONS.BUTTON_CMD);
		actionBuilder.putActionInfo("title", title);
		actionBuilder.putActionInfo("info", "direct_cmd");
		actionBuilder.putActionInfo("cmd", cmd);
		//default button options
		actionBuilder.putActionInfo("options", JSON.make(
				ACTIONS.SHOW_VIEW, true
		)); 
	}
	/**
	 * Add a scheduled message
	 */
	private static void addScheduledMessage(ServiceBuilder actionBuilder, String eventId, long triggerDelayMS, String message){
		actionBuilder.addAction(ACTIONS.SCHEDULE_MSG);
		actionBuilder.putActionInfo("info", "entertainWhileIdle");		//TODO: one could distinguish messages that are only triggered when the app is not in foreground vs. important notes etc ...
		actionBuilder.putActionInfo("eventId", eventId);
		actionBuilder.putActionInfo("triggerIn", triggerDelayMS);
		actionBuilder.putActionInfo("text", message);
		//actionBuilder.actionInfo_put_info("options", "inputHidden");
	}

}
