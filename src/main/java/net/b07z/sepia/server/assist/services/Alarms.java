package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.events.ChangeEvent;
import net.b07z.sepia.server.assist.events.EventsBroadcaster;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.DialogTaskValues;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.interviews.InterviewMetaData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.AlarmType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.IndexType;
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
	public static boolean testMode = false; 	//use only for testing - skips database access

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
		info.addSuccessAnswer("alarms_0a")
			.addOkayAnswer("alarms_0b")
			.addFailAnswer("alarms_0c");
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
	static final String answerToDo = "default_under_construction_0a";
	static final String answerSetAlarmNear = "alarms_set_1a"; 			//"<direct>Ok, Alarm ist auf <2> <1> Uhr gestellt.";
	static final String answerSetAlarmFar = "alarms_set_1b";			//"<direct>Ok, Alarm ist auf <1> Uhr am <2> gestellt.";
	static final String answerSetTimer = "alarms_set_1c";				//"<direct>Ok, Timer wird gestellt auf <1>.";
	static final String answerNextTimer = "alarms_show_1a"; 			//"<direct>Hier sind deine Timer, der nächste ist in <1>.";
	static final String answerNextAlarm = "alarms_show_1b"; 			//"<direct>Hier sind deine Wecker, der nächste is um <1> Uhr am <2>.";
	static final String answerShowTimers = "alarms_show_1c"; 			//"<direct>Hier sind deine Timer, zur Zeit ist keiner aktiv.";
	static final String answerShowAlarms = "alarms_show_1d"; 			//"<direct>Hier sind deine Wecker, zur Zeit ist keiner aktiv.";
	static final String answerRemoveTimers = "alarms_stop_1a"; 			//"<direct>Ok, Timer wird gestoppt.";
	static final String answerRemoveAlarms = "alarms_stop_1b"; 			//"<direct>Ok, Wecker wurde entfernt.";
	static final String askAlarmClock = "alarms_ask_0a"; 				//"<direct>Für wann soll ich den Alarm stellen?";
	static final String askTimerClock = "alarms_ask_timer_0a"; 			//"<direct>Wie lange soll der Timer laufen?";
	static final String askReminderName = "alarms_ask_reminder_0a"; 	//"<direct>Wie soll die Erinnerung heißen?";
	static final String timeIsPast = "alarms_abort_is_past_0a"; 		//"<direct>Sorry <user_name>, aber der Zeitpunkt liegt meinem Kalender nach in der Vergangenheit.";
	static final String listTooLong = "alarms_abort_list_full_0a"; 		//"<direct>Oh, die Liste ist leider voll bis obenhin.";
	static final String areYouSureTimer = "alarms_confirm_delete_timer_0a"; //"<direct>Bist du sicher, dass du den nächsten Timer stoppen willst?";
	static final String areYouSureAlarm = "alarms_confirm_delete_0a"; 		//"<direct>Bist du sicher, dass du den nächsten Wecker entfernen willst?";
	static final String okNo = "default_abort_no_change_0a"; 			//"<direct>Ok, dann lasse ich mal alles so.";
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		User user = nluResult.input.user;
		
		//get parameters:
				
		//optional
		//-date and time
		Parameter dateTimeP = nluResult.getOptionalParameter(PARAMETERS.TIME, "");
		String dateDay = (String) dateTimeP.getDataFieldOrDefault(InterviewData.DATE_DAY);
				
		Parameter clockP = nluResult.getOptionalParameter(PARAMETERS.CLOCK, "");
		String dateTime = (String) clockP.getDataFieldOrDefault(InterviewData.DATE_TIME);
		
		api.resultInfoPut("day", dateDay);
		api.resultInfoPut("time", dateTime.replaceFirst(":\\d\\d$", "").trim()); 	//might be overwritten later
		
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
			}else if (api.language.equals(LANGUAGES.EN) && NluTools.stringContains(nluResult.input.text, "(wake|remind|remember|get up|out .* bed)")){
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
				InterviewMetaData metaInfo = new InterviewMetaData().setDialogTask(DialogTaskValues.TIME);
				if (isTimer){
					api.setIncompleteAndAsk(PARAMETERS.CLOCK, askTimerClock, metaInfo);
				}else{
					//TODO: this question accepts date and time as answer, but we should probably include the date in the question if given  
					api.setIncompleteAndAsk(PARAMETERS.CLOCK, askAlarmClock, metaInfo);
				}
				ServiceResult result = api.buildResult();
				return result;
			}
			//note: since we split date and time here and can ask separately for time we have to add it all together in the end
			
			long diffDays = -1;
			long diffHours = -1;
			long diffMinutes = -1;
			long diffSeconds = -1;
			long totalDiff_ms = -1;
			String diffExceptionNote = "unknown";
			if (dateTimeP.getData().containsKey(InterviewData.DATE_TIME)){
				//get total difference from dateTime parameter since time was already there
				JSONObject diff = (JSONObject) dateTimeP.getData().get(InterviewData.TIME_DIFF);
				if (diff != null && !diff.isEmpty()){
					totalDiff_ms = JSON.getLongOrDefault(diff, "total_ms", -1);
					diffDays = JSON.getLongOrDefault(diff, "dd", -1);
					diffHours = JSON.getLongOrDefault(diff, "hh", -1);
					diffMinutes = JSON.getLongOrDefault(diff, "mm", -1);
					diffSeconds = JSON.getLongOrDefault(diff, "ss", -1);
				}else{
					diffExceptionNote = "Failed to get time-diff. from parameter.";
				}
			}else{
				//construct new difference
				String newRefDate = dateDay + Config.defaultSdfSeparator + dateTime;
				//System.out.println("newRefDate: " + newRefDate); 	//DEBUG
				HashMap<String, Long> diff = DateTimeConverters.dateDifference(nluResult.input.userTimeLocal, newRefDate);
				if (diff != null){
					totalDiff_ms = diff.get("total_ms");
					diffDays = diff.get("dd");
					diffHours = diff.get("hh");
					diffMinutes = diff.get("mm");
					diffSeconds = diff.get("ss");
				}else{
					diffExceptionNote = "Failed to get time-diff. from new ref.: " + newRefDate;
				}
			}
			//what if the time lies in the past? Make some smart decisions!
			if (totalDiff_ms <= 0){
				boolean changedDateAndTime = false; 	//TODO: we could use this to modify the answer slightly (to point out the change) 
				
				//user probably meant next day?
				long absTotalDiff_ms = Math.abs(totalDiff_ms);
				long absDiffDays = Math.abs(diffDays);
				long maxHours = 23*60*60*1000;
				long minHours = 1*60*60*1000;
				if (absTotalDiff_ms > minHours && absTotalDiff_ms < maxHours){
					//next day
					dateDay = DateTimeConverters.getTomorrow("yyyy.MM.dd", nluResult.input);
					changedDateAndTime = true;
				}else if (absDiffDays >= 2 && absDiffDays < 365){
					//next year
					String oldYear = dateDay.split("\\.")[0];
					String nextYear = Long.toString(Long.parseLong(oldYear) + 1l);
					dateDay = dateDay.replace(oldYear, nextYear);
					changedDateAndTime = true;
				}
				//TODO: add more - there is another issue: it is a difference if the user says "alarm for 8am" or "alarm for [day-month-of-today] 8am" (we ignore this for now) 
				
				//update time ...
				if (changedDateAndTime){
					HashMap<String, Long> diff = DateTimeConverters.dateDifference(nluResult.input.userTimeLocal, 
							dateDay + Config.defaultSdfSeparator + dateTime);
					if (diff != null){
						totalDiff_ms = diff.get("total_ms");
						diffDays = diff.get("dd");
						diffHours = diff.get("hh");
						diffMinutes = diff.get("mm");
						diffSeconds = diff.get("ss");
					}else{
						diffExceptionNote = "Failed to get time-diff. from changed date.";
					}
					
				//...or just abort with "is past" message
				}else{
					api.setCustomAnswer(timeIsPast);
					api.setStatusOkay();
					
					ServiceResult result = api.buildResult();
					return result;
				}
			}
			//check again and finally assign
			long targetTimeUnix = -1;		//default is -1 for past or unknown
			if (totalDiff_ms > 0){
				targetTimeUnix = System.currentTimeMillis() + totalDiff_ms;
			
			//...or abort with error
			}else{
				api.setStatusFail();
				Debugger.println(getClass().getName() + " - 'targetTimeUnix' was unexpectedly in the past. Info: " + diffExceptionNote, 3);
				ServiceResult result = api.buildResult();
				return result;
			}
			
			//make a (hopefully) unique ID
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
			String eventId = null;	//will be auto-created, 
			
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
				JSONObject data = UserDataList.createEntryTimer(
					targetTimeUnix, 
					name, 
					null, 
					System.currentTimeMillis(), 
					activatedByUI
				);
				eventId = JSON.getString(data, "eventId"); 		//get auto-created ID
				JSON.add(activeList.data, data);
				
				//action
				//TODO: make action fields identical to listElements?
				api.addAction(ACTIONS.TIMER);
					api.putActionInfo("info", "set");
					api.putActionInfo("targetTimeUnix", targetTimeUnix);
					api.putActionInfo("name", name);
					api.putActionInfo("eventId", eventId);
					api.putActionInfo("eleType", UserDataList.EleType.timer.name());
				
			}else if (isAlarm){
				String repeat = "onetime";
								
				//answer:
				
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
				
				//... and for time
				String speakableTime = DateTimeConverters.getSpeakableTime(dateTime, "HH:mm:ss", api.language);
				api.resultInfoPut("time", speakableTime);
				
				String name = "";
				if (alarmName != null && !alarmName.isEmpty()){
					name = alarmName;
				}else{
					name = speakableDay;
				}
				name = name.trim();
				
				//build data
				JSONObject data = UserDataList.createEntryAlarm(
					targetTimeUnix, 
					speakableDay, 
					dateTime, 
					speakableDate, 
					repeat, 
					name, 
					null, 
					System.currentTimeMillis(), 
					activatedByUI
				);
				eventId = JSON.getString(data, "eventId"); 		//get auto-created ID
				JSON.add(activeList.data, data);
				
				//action
				//TODO: make action fields identical to listElements?
				api.addAction(ACTIONS.ALARM);
					api.putActionInfo("info", "set");
					api.putActionInfo("targetTimeUnix", targetTimeUnix);
					api.putActionInfo("day", speakableDay);
					api.putActionInfo("time", dateTime);
					api.putActionInfo("date", speakableDate);
					api.putActionInfo("repeat", repeat);
					api.putActionInfo("name", name);
					api.putActionInfo("eleType", UserDataList.EleType.alarm.name());
					api.putActionInfo("eventId", eventId);
			
			}else{
				//TODO: what about the other possible events?
				eventId = "";
			}
			
			//update list
			boolean updateSuccess = writeTimeEventsList(userData, user, activeList);
			if (!updateSuccess){
				api.setStatusFail();
				ServiceResult result = api.buildResult();
				return result;
			
			}else{
				broadcastUpdateEventToAllUserDevices(nluResult.input.user, nluResult.input.deviceId, 
						activeList.getId(), eventId);
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
				//build answer with next alarm/timer
				JSONObject nextAlarm = getNextTimeEventInList(udlList, true);
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
						api.resultInfoPut("time", DateTimeConverters.getSpeakableTime(JSON.getString(nextAlarm, "time"), "HH:mm:ss", api.language));
						api.resultInfoPut("day", JSON.getString(nextAlarm, "day"));
						api.setCustomAnswer(answerNextAlarm);
					}
				}
				
				//make card
				Card card = new Card(Card.TYPE_UNI_LIST);
				for (UserDataList udl : udlList){
					card.addGroupeElement(ElementType.userDataList, udl.indexType, udl.getJSON());
				}
				api.addCard(card);
				api.hasCard = true;
				
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
					api.confirmActionOrParameter("do_remove", areYouSureTimer,
						new InterviewMetaData().setDialogTask(DialogTaskValues.YES_OR_NO));
				}else{
					api.confirmActionOrParameter("do_remove", areYouSureAlarm,
						new InterviewMetaData().setDialogTask(DialogTaskValues.YES_OR_NO));
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
						}else{
							String eventId = JSON.getString(nextAlarm, "eventId");
							
							broadcastUpdateEventToAllUserDevices(nluResult.input.user, nluResult.input.deviceId, 
									searchResult.activeList.getId(), eventId);
							
							//action
							api.addAction(ACTIONS.ALARM);
								api.putActionInfo("info", "remove");
								api.putActionInfo("targetTimeUnix", JSON.getLongOrDefault(nextAlarm, "targetTimeUnix", 0));
								api.putActionInfo("eventId", eventId);
								
							if (isTimer){
								api.setCustomAnswer(answerRemoveTimers);
							}else{
								api.setCustomAnswer(answerRemoveAlarms);
							}
							api.setStatusSuccess();
						}
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
		List<UserDataList> udlList;
		if (testMode){
			udlList = new ArrayList<>();
			udlList.add(new UserDataList(user.getUserID(), Section.timeEvents, IndexType.alarms.name(), "Test", new JSONArray()));
		}else{
			HashMap<String, Object> filters = new HashMap<>();
			if (!alarmType.isEmpty()) filters.put("title", alarmType);
			udlList = userData.getUserDataList(user, Section.timeEvents, IndexType.alarms.name(), filters);
		}
		return udlList;
	}
	
	private boolean writeTimeEventsList(UserDataInterface userData, User user, UserDataList activeList){
		if (testMode){
			return true;
		}
		//System.out.println("DATA: " + activeList.data); 		//debug
		JSONObject writeResult = userData.setUserDataList(user, Section.timeEvents, activeList);
		//System.out.println("RESULT CODE: " + code); 		//debug
		if (writeResult.containsKey("status") && JSON.getString(writeResult, "status").equals("success")){
			return true;
		}else{
			return false;
		}
	}
	private void broadcastUpdateEventToAllUserDevices(User user, String senderDeviceId, String listId, String timeEventId){
		//sent time events update trigger after a short delay
		EventsBroadcaster.broadcastBackgroundDataSyncNotes(
			EventsBroadcaster.buildChangeEventsSet(
				new ChangeEvent(ChangeEvent.Type.timeEvents, listId, JSON.make("eventId", timeEventId))
			), user, senderDeviceId
		);
	}
	
	private JSONObject getNextTimeEventInList(List<UserDataList> udlList, boolean markAsNext){
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
		if (nextAlarm != null && markAsNext){
			JSON.put(nextAlarm, "isNextEvent", true);
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
