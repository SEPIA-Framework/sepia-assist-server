package net.b07z.sepia.server.assist.services;

import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.AlarmType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.tools.RandomGen;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Alarm clock and timers.
 * 
 * @author Florian Quirin
 *
 */
public class Alarms implements ServiceInterface{
	
	private static int list_limit = 50;			//this many alarms are allowed in each list (timer and alarm are seperate e.g.)

	@Override
	public ServiceInfo getInfo(String language) {
		//type
		ServiceInfo info = new ServiceInfo(Type.account, Content.data, false);
		
		//Parameters:
		
		//optional
		Parameter p1 = new Parameter(PARAMETERS.ACTION);
		Parameter p2 = new Parameter(PARAMETERS.TIME);
		Parameter p3 = new Parameter(PARAMETERS.CLOCK);
		Parameter p4 = new Parameter(PARAMETERS.ALARM_TYPE);
		Parameter p5 = new Parameter(PARAMETERS.ALARM_NAME);
		info.addParameter(p1).addParameter(p2).addParameter(p3).addParameter(p4).addParameter(p5);
		
		//Answers:
		info.addSuccessAnswer("<direct>Der nächste Alarm ist in <1>")
			.addOkayAnswer("<direct>Es gibt scheinbar keinen aktiven <3> zur Zeit.")
			.addFailAnswer("<direct>Dazu kann ich gerade nix finden irgendwie, versuch es bitte gleich noch mal.");
		info.addCustomAnswer("answerSetAlarmNear", answerSetAlarmNear)
			.addCustomAnswer("answerSetAlarmFar", answerSetAlarmFar)
			.addCustomAnswer("answerSetTimer", answerSetTimer)
			.addCustomAnswer("answerNextTimer", answerNextTimer)
			.addCustomAnswer("answerNextAlarm", answerNextAlarm)
			.addCustomAnswer("answerShowTimers", answerShowTimers)
			.addCustomAnswer("answerShowAlarms", answerShowAlarms)
			.addCustomAnswer("answerRemoveTimers", answerRemoveTimers)
			.addCustomAnswer("answerRemoveAlarms", answerRemoveAlarms)
			.addCustomAnswer("askAlarmClock", askAlarmClock)
			.addCustomAnswer("askTimerClock", askTimerClock)
			.addCustomAnswer("askReminderName", askReminderName)
			.addCustomAnswer("timeIsPast", timeIsPast)
			.addCustomAnswer("listTooLong", listTooLong)
			.addCustomAnswer("answerToDo", answerToDo)
			.addCustomAnswer("areYouSureTimer", areYouSureTimer)
			.addCustomAnswer("areYouSureAlarm", areYouSureAlarm)
			.addCustomAnswer("okNo", okNo);
		
		info.addAnswerParameters("time", "day", "type");
		
		return info;
	}
	static final String answerToDo = "<direct>Daran wird noch gearbeitet, sorry.";
	static final String answerSetAlarmNear = "<direct>Ok, Alarm ist auf <2> <1> Uhr gestellt.";
	static final String answerSetAlarmFar = "<direct>Ok, Alarm ist auf <1> Uhr am <2> gestellt.";
	static final String answerSetTimer = "<direct>Ok, Timer wird gestellt auf <1>.";
	static final String answerNextTimer = "<direct>Hier sind deine Timer, der nächste ist in <1>.";
	static final String answerNextAlarm = "<direct>Hier sind deine Wecker, der nächste is um <1> Uhr am <2>.";
	static final String answerShowTimers = "<direct>Hier sind deine Timer, zur Zeit ist keiner aktiv.";
	static final String answerShowAlarms = "<direct>Hier sind deine Wecker, zur Zeit ist keiner aktiv.";
	static final String answerRemoveTimers = "<direct>Ok, Timer wird gestoppt.";
	static final String answerRemoveAlarms = "<direct>Ok, Wecker wurde entfernt.";
	static final String askAlarmClock = "<direct>Für wann soll ich den Alarm stellen?";
	static final String askTimerClock = "<direct>Wie lange soll der Timer laufen?";
	static final String askReminderName = "<direct>Wie soll die Erinnerung heißen?";
	static final String timeIsPast = "<direct>Sorry <user_name>, aber der Zeitpunkt liegt meinem Kalender nach in der Vergangenheit.";
	static final String listTooLong = "<direct>Oh, die Liste ist leider voll bis obenhin.";
	static final String areYouSureTimer = "<direct>Bist du sicher, dass du den nächsten Timer stoppen willst?";
	static final String areYouSureAlarm = "<direct>Bist du sicher, dass du den nächsten Wecker entfernen willst?";
	static final String okNo = "<direct>Ok, dann lasse ich mal alles so.";
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, getInfo(""));
		User user = nluResult.input.user;
		
		//get parameters:
				
		//optional
		//-date and time
		Parameter dateTimeP = nluResult.getOptionalParameter(PARAMETERS.TIME, "");
		String dateDay = (String) dateTimeP.getDataFieldOrDefault(InterviewData.DATE_DAY);
				
		Parameter clockP = nluResult.getOptionalParameter(PARAMETERS.CLOCK, "");
		String dateTime = (String) clockP.getDataFieldOrDefault(InterviewData.DATE_TIME);
		
		api.resultInfoPut("day", dateDay);
		api.resultInfoPut("time", dateTime.replaceFirst(":\\d\\d$", "").trim());
		
		//-alarm type
		Parameter alarmTypeP = nluResult.getOptionalParameter(PARAMETERS.ALARM_TYPE, "");
		String alarmType = alarmTypeP.getValueAsString().replaceAll("^<|>$", "").trim();
		String alarmTypeToSay = (String) alarmTypeP.getDataFieldOrDefault(InterviewData.VALUE_LOCAL);
		if (!alarmTypeToSay.isEmpty()){
			api.resultInfoPut("type", alarmTypeToSay);
		}else{
			api.resultInfoPut("type", alarmType);
		}
		boolean isTimer = alarmType.equals(AlarmType.Type.timer.name());
		boolean isAlarm = alarmType.equals(AlarmType.Type.alarmClock.name());
		boolean isReminder = alarmType.equals(AlarmType.Type.reminder.name());
		boolean isAppointment = alarmType.equals(AlarmType.Type.appointment.name());
		
		//-alarm name
		Parameter alarmNameP = nluResult.getOptionalParameter(PARAMETERS.ALARM_NAME, "");
		String alarmName = alarmNameP.getValueAsString();
		
		//-action 
		Parameter actionP = nluResult.getOptionalParameter(PARAMETERS.ACTION, "");
		String action = ((String) actionP.getDataFieldOrDefault(InterviewData.VALUE)).replaceAll("^<|>$", "").trim();
		if (action.isEmpty()){
			//check some alarm-specific action expressions
			if (api.language.equals(LANGUAGES.DE) && NluTools.stringContains(nluResult.input.text, "(weck(e|)|(wecker|timer) (fuer|auf)|erinner(e|)|erinnerung (an|fuer)|aufstehen|aus .* bett)")){
				action = Action.Type.set.name();
			}else if (api.language.equals(LANGUAGES.EN) && NluTools.stringContains(nluResult.input.text, "(wake|remind|get up|out .* bed)")){
				action = Action.Type.set.name();
			}else{
				//make some smart action assumptions
				if (!dateDay.isEmpty() || !dateTime.isEmpty()){
					action = Action.Type.set.name();
				}else{
					action = Action.Type.show.name();
				}
			}
			api.overwriteParameter(PARAMETERS.ACTION, "<" + action + ">"); 		//NOTE: value has to be valid input for parameter build method!
		}
		boolean isActionSet = (action.equals(Action.Type.set.name()) || action.equals(Action.Type.add.name()) || action.equals(Action.Type.create.name()) || action.equals(Action.Type.on.name()));
		boolean isActionShow = (action.equals(Action.Type.show.name()) || action.equals(Action.Type.edit.name()));
		boolean isActionStop = action.equals(Action.Type.remove.name()) || action.equals(Action.Type.pause.name()) || action.equals(Action.Type.off.name());
		
		Debugger.println("cmd: " + CMD.TIMER + ", alarmType=" + alarmType + ", alarmName=" + alarmName + ", action=" + action + 
				", time=" + dateDay + "_" + dateTime, 2);	//debug
		
		/*
		System.out.println("DateTime:");
		JSON.printJSON(dateTimeP.getData()); 	//DEBUG
		System.out.println("Clock:");
		JSON.printJSON(clockP.getData()); 		//DEBUG
		*/
		
		//TYPE CHECK: abort if type is not supported
		if (isReminder || isAppointment){
			if (!alarmName.isEmpty()){
				//TODO: this is a quick workaround to make reminders possible as alarm clock if they are given in one sentence with "name"
				isReminder = false;
				isAppointment = false;
				isAlarm = true;
				alarmType = AlarmType.Type.alarmClock.name();
			}else{
				//TODO: this is a quick workaround to treat reminders and appointments as alarm clock but we need a name
				api.setIncompleteAndAsk(PARAMETERS.ALARM_NAME, askReminderName);
				ServiceResult result = api.buildResult();
				return result;
			}
		}
		if (!isTimer && !isAlarm){
			api.setStatusOkay();
			api.setCustomAnswer(answerToDo);
			
			ServiceResult result = api.buildResult();
			return result;
		}
		
		//SET / CREATE 
		if (isActionSet){
			
			//check some info
			if (dateTime.isEmpty()){
				//abort with question
				if (isTimer){
					api.setIncompleteAndAsk(PARAMETERS.CLOCK, askTimerClock);
				}else{
					api.setIncompleteAndAsk(PARAMETERS.CLOCK, askAlarmClock);
				}
				ServiceResult result = api.buildResult();
				return result;
			}
			//note: since we split date and time here and can ask separately for time we have to add it all together in the end
			
			long timeUnix = -1;
			long diffDays = -1;
			long diffHours = -1;
			long diffMinutes = -1;
			long diffSeconds = -1;
			long totalDiff_ms = -1;
			if (dateTimeP.getData().containsKey(InterviewData.DATE_TIME)){
				//get total difference from dateTime parameter since time was already there
				JSONObject diff = (JSONObject) dateTimeP.getData().get(InterviewData.TIME_DIFF);
				if (diff != null && !diff.isEmpty()){
					totalDiff_ms = JSON.getLongOrDefault(diff, "total_ms", -1);
					diffDays = JSON.getLongOrDefault(diff, "dd", -1);
					diffHours = JSON.getLongOrDefault(diff, "hh", -1);
					diffMinutes = JSON.getLongOrDefault(diff, "mm", -1);
					diffSeconds = JSON.getLongOrDefault(diff, "ss", -1);
				}
			}else{
				//construct new difference
				String newRefDate = dateDay + Config.defaultSdfSeparator + dateTime;
				HashMap<String, Long> diff = DateTimeConverters.dateDifference(nluResult.input.userTimeLocal, newRefDate);
				if (diff != null){
					totalDiff_ms = diff.get("total_ms");
					diffDays = diff.get("dd");
					diffHours = diff.get("hh");
					diffMinutes = diff.get("mm");
					diffSeconds = diff.get("ss");
				}
			}
			//TODO: what if the time lies in the past? Make some smart decisions!
			if (totalDiff_ms <= 0){
				//for now just abort
				api.setCustomAnswer(timeIsPast);
				api.setStatusOkay();
				
				ServiceResult result = api.buildResult();
				return result;
				
			}else{
				timeUnix = System.currentTimeMillis() + totalDiff_ms;
			}
			
			//make a (hopefully) unique ID
			long lastChange = System.currentTimeMillis();
			String eventIdSuffix = lastChange + "-" + RandomGen.getInt(100, 999);
			boolean activatedByUI = false;
			
			//Load list
			UserDataInterface userData =  user.getUserDataAccess();
			List<UserDataList> udlList = getTimeEventsList(userData, user, alarmType); 	//note: alarmType will be either timer or alarm here so the list is expected to be size=1
			UserDataList activeList = null;
			
			//server communication error?
			if (udlList == null){
				api.setStatusFail();
				ServiceResult result = api.buildResult();
				return result;
			
			//no list result found ... create one
			}else if (udlList.isEmpty()){
				activeList = new UserDataList(user.getUserID(), Section.timeEvents, UserDataList.IndexType.alarms.name(), alarmType, new JSONArray());
				udlList.add(activeList);
			
			//take the first list (should be only 1)
			}else{
				activeList = udlList.get(0);
				if (activeList.data == null){
					activeList.data = new JSONArray();
				}
				//check size of list
				if ((activeList.data.size() + 1) >= list_limit){
					//build answer - list is too big
					api.setStatusOkay();
					api.setCustomAnswer(listTooLong);
					ServiceResult result = api.buildResult();
					return result;
				}
			}
			
			//actions and answers
			if (isTimer){			
				//answer
				api.setCustomAnswer(answerSetTimer);
				api.resultInfoPut("time", DateTimeConverters.getSpeakableDuration(nluResult.language, diffDays, diffHours, diffMinutes, diffSeconds));
				
				String name = "";
				if (alarmName != null && !alarmName.isEmpty()){
					name = alarmName;
				}else{
					String speakableDate = DateTimeConverters.getSpeakableDate(dateDay, "yyyy.MM.dd", api.language);
					name = (speakableDate + " - " + dateTime);
				}
				
				//build data
				JSONObject data = JSON.make(
						"targetTimeUnix", timeUnix,
						"name", name,
						"eventId", "timer-" + eventIdSuffix,
						"lastChange", lastChange,
						"activated", new Boolean(activatedByUI)
				);
				JSON.put(data, "eleType", UserDataList.EleType.timer.name());
				JSON.add(activeList.data, data);
				
				//action
				//TODO: make action fields identical to listElements?
				api.addAction(ACTIONS.TIMER);
					api.putActionInfo("info", "set");
					api.putActionInfo("targetTimeUnix", timeUnix);
					api.putActionInfo("name", name);
					api.putActionInfo("eventId", "timer-" + eventIdSuffix);
					api.putActionInfo("eleType", UserDataList.EleType.timer.name());
				
			}else if (isAlarm){
				String repeat = "onetime";
								
				//answer
				//get a nice, speakable day by correcting diffDays for hours, minutes, seconds to midnight and convert
				long correctedDiffDays = DateTimeConverters.getIntuitiveDaysDifference(nluResult.input, diffDays, diffHours, diffMinutes, diffSeconds);
				String speakableDay = DateTimeConverters.getSpeakableDateSpecial(dateDay, correctedDiffDays, "yyyy.MM.dd", nluResult.input, false);
				String speakableDate = DateTimeConverters.getSpeakableDate(dateDay, "yyyy.MM.dd", api.language);
				if (correctedDiffDays<7){
					api.setCustomAnswer(answerSetAlarmNear);
				}else{
					api.setCustomAnswer(answerSetAlarmFar);
				}
				api.resultInfoPut("day", speakableDay);
				
				String name = "";
				if (alarmName != null && !alarmName.isEmpty()){
					name = alarmName;
				}else{
					name = speakableDay;
				}
				
				//build data
				JSONObject data = JSON.make(
						"targetTimeUnix", timeUnix,
						"day", speakableDay,
						"time", dateTime,
						"date", speakableDate,
						"repeat", repeat
				);
				JSON.put(data, "name", name);
				JSON.put(data, "eleType", UserDataList.EleType.alarm.name());
				JSON.put(data, "eventId", "alarm-" + eventIdSuffix);
				JSON.put(data, "lastChange", lastChange);
				JSON.put(data, "activated", new Boolean(activatedByUI));
				JSON.add(activeList.data, data);
				
				//action
				//TODO: make action fields identical to listElements?
				api.addAction(ACTIONS.ALARM);
					api.putActionInfo("info", "set");
					api.putActionInfo("targetTimeUnix", timeUnix);
					api.putActionInfo("day", speakableDay);
					api.putActionInfo("time", dateTime);
					api.putActionInfo("date", speakableDate);
					api.putActionInfo("repeat", repeat);
					api.putActionInfo("name", name);
					api.putActionInfo("eleType", UserDataList.EleType.alarm.name());
					api.putActionInfo("eventId", "alarm-" + eventIdSuffix);
			}
			
			//update list
			boolean updateSuccess = writeTimeEventsList(userData, user, activeList);
			if (!updateSuccess){
				api.setStatusFail();
				ServiceResult result = api.buildResult();
				return result;
			
			}else{
				api.setStatusSuccess();
			}
			
			ServiceResult result = api.buildResult();
			return result;
		
		//SHOW
		}else if (isActionShow){
			//Load list
			UserDataInterface userData =  user.getUserDataAccess();
			List<UserDataList> udlList = getTimeEventsList(userData, user, alarmType);
			
			//server communication error?
			if (udlList == null){
				api.setStatusFail();
				
			//no list result found ...
			}else if (udlList.isEmpty() || udlList.get(0).data.isEmpty()){
				//anything else???
				api.setStatusOkay();
			
			}else{
				//make card
				Card card = new Card(Card.TYPE_UNI_LIST);
				for (UserDataList udl : udlList){
					card.addGroupeElement(ElementType.userDataList, udl.indexType, udl.getJSON());
				}
				api.addCard(card);
				api.hasCard = true;
				
				//build answer with next alarm/timer
				JSONObject nextAlarm = getNextTimeEventInList(udlList);
				if (isTimer){
					if (nextAlarm == null){
						api.setCustomAnswer(answerShowTimers);
					}else{
						//get a nice, speakable time String
						HashMap<String, Long> diff = DateTimeConverters.dateDifference(System.currentTimeMillis(), JSON.getLongOrDefault(nextAlarm, "targetTimeUnix", 0));
						long diffDays = diff.get("dd");
						long diffHours = diff.get("hh");
						long diffMinutes = diff.get("mm");
						long diffSeconds = diff.get("ss");
						api.resultInfoPut("time", DateTimeConverters.getSpeakableDuration(api.language, diffDays, diffHours, diffMinutes, diffSeconds));
						api.setCustomAnswer(answerNextTimer);
					}
					
				}else{
					if (nextAlarm == null){
						api.setCustomAnswer(answerShowAlarms);
					}else{
						//day should be nice already
						//String speakableDay = Tools_DateTime.getSpeakableDate(JSON.getString(nextAlarm, "day"), "yyyy.MM.dd", api.language);
						api.resultInfoPut("time", JSON.getString(nextAlarm, "time").replaceFirst(":\\d\\d$", "").trim());
						api.resultInfoPut("day", JSON.getString(nextAlarm, "day"));
						api.setCustomAnswer(answerNextAlarm);
					}
				}
				api.setStatusSuccess();
			}
			
			ServiceResult result = api.buildResult();
			return result;
		
		//REMOVE
		}else if (isActionStop){
			//Confirmation test:
			
			//check confirm status for "do_remove"
			String confirmRemoveAction = "do_remove";
			int confirmStatus = api.getConfirmationStatusOf(confirmRemoveAction);
			if (confirmStatus == 0){
				//ASK CONFIRM
				if (isTimer){
					api.confirmActionOrParameter("do_remove", areYouSureTimer);
				}else{
					api.confirmActionOrParameter("do_remove", areYouSureAlarm);
				}
				ServiceResult result = api.buildResult();
				return result;
			
			}else if (confirmStatus == 1){
				//OK

				//Load list
				UserDataInterface userData =  user.getUserDataAccess();
				List<UserDataList> udlList = getTimeEventsList(userData, user, alarmType);
				
				//server communication error?
				if (udlList == null){
					api.setStatusFail();
					
				//no list result found ...
				}else if (udlList.isEmpty() || udlList.get(0).data.isEmpty()){
					//anything else???
					api.setStatusOkay();
				
				}else{
					//get next event and remove from list
					EventSearchResult searchResult = getNextTimeEventInListAndRemove(udlList);
					JSONObject nextAlarm = searchResult.listEntry;
					if (nextAlarm == null){
						api.setStatusOkay();
					
					}else{
						//save changes
						boolean updateSuccess = writeTimeEventsList(userData, user, searchResult.activeList);
						if (!updateSuccess){
							api.setStatusFail();
							ServiceResult result = api.buildResult();
							return result;
						
						}						
						//action
						api.addAction(ACTIONS.ALARM);
							api.putActionInfo("info", "remove");
							api.putActionInfo("targetTimeUnix", JSON.getLongOrDefault(nextAlarm, "targetTimeUnix", 0));
							api.putActionInfo("eventId", JSON.getString(nextAlarm, "eventId"));
							
						if (isTimer){
							api.setCustomAnswer(answerRemoveTimers);
						}else{
							api.setCustomAnswer(answerRemoveAlarms);
						}
						api.setStatusSuccess();
					}
				}
				
				ServiceResult result = api.buildResult();
				return result;
			
			}else{
				//CANCEL
				api.setStatusOkay();
				api.setCustomAnswer(okNo);
				
				ServiceResult result = api.buildResult();
				return result;
			}
		
		//Unknown action?
		}else{
			api.setStatusFail();
			Debugger.println(getClass().getName() + " - unknown action: '" + action + "', type: '" + alarmType + "' or error!", 3);
		}
		
		ServiceResult result = api.buildResult();
		return result;
	}
	
	//---------- helpers ----------
	
	private List<UserDataList> getTimeEventsList(UserDataInterface userData, User user, String alarmType){
		HashMap<String, Object> filters = new HashMap<>();
		if (!alarmType.isEmpty()) filters.put("title", alarmType);
		List<UserDataList> udlList = userData.getUserDataList(user, Section.timeEvents, UserDataList.IndexType.alarms.name(), filters);
		return udlList;
	}
	
	private boolean writeTimeEventsList(UserDataInterface userData, User user, UserDataList activeList){
		//System.out.println("DATA: " + activeList.data); 		//debug
		JSONObject writeResult = userData.setUserDataList(user, Section.timeEvents, activeList);
		//System.out.println("RESULT CODE: " + code); 		//debug
		if (writeResult.containsKey("status") && JSON.getString(writeResult, "status").equals("success")){
			return true;
		}else{
			return false;
		}
	}
	
	private JSONObject getNextTimeEventInList(List<UserDataList> udlList){
		JSONObject nextAlarm = null;
		long shortestTime = Long.MAX_VALUE;
		for (UserDataList udl : udlList){
			for (Object dataObj : udl.data){
				JSONObject data = (JSONObject) dataObj;
				long execTime = JSON.getLongOrDefault(data, "targetTimeUnix", Long.MAX_VALUE);
				if ((execTime - System.currentTimeMillis()) > 0 && (execTime < shortestTime)){
					nextAlarm = data;
					shortestTime = execTime;
				}
			}
		}
		return nextAlarm;
	}
	private EventSearchResult getNextTimeEventInListAndRemove(List<UserDataList> udlList){
		JSONObject nextAlarm = null;
		int nextListI = -1;
		int nextEntryI = -1;
		long shortestTime = Long.MAX_VALUE;
		int i=0, j=0;
		for (UserDataList udl : udlList){
			j=0;
			for (Object dataObj : udl.data){
				JSONObject data = (JSONObject) dataObj;
				long execTime = JSON.getLongOrDefault(data, "targetTimeUnix", Long.MAX_VALUE);
				if ((execTime - System.currentTimeMillis()) > 0 && (execTime < shortestTime)){
					nextAlarm = data;
					shortestTime = execTime;
					nextListI = i;
					nextEntryI = j;
				}
				j++;
			}
			i++;
		}
		if (nextEntryI > -1){
			udlList.get(nextListI).data.remove(nextEntryI);
			return new EventSearchResult(nextAlarm, udlList.get(nextListI));
		}else{
			return new EventSearchResult(null, null);
		}
	}
	
	//Helper Class
	private class EventSearchResult{
		UserDataList activeList;
		JSONObject listEntry;
		EventSearchResult(JSONObject data, UserDataList list){
			activeList = list;
			listEntry = data;
		}
	}

}