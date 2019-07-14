package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * What type of alarm is this request, timer? alarm-clock? reminder?
 * 
 * @author Florian Quirin
 *
 */
public class AlarmType implements ParameterHandler{
	
	//------ data -------
	
	public enum Type {
		alarmClock,
		reminder,
		timer,
		appointment
	}
	
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		types_de.put("<alarmClock>", "Wecker");
		types_de.put("<reminder>", "Erinnerung");
		types_de.put("<timer>", "Timer");
		types_de.put("<appointment>", "Termin");
		
		types_en.put("<alarmClock>", "alarm");
		types_en.put("<reminder>", "reminder");
		types_en.put("<timer>", "timer");
		types_en.put("<appointment>", "appointment");
	}
	/**
	 * Translate generalized value to local name.
	 * If generalized value is unknown returns empty string
	 * @param genValue - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String genValue, String language){
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = types_de.get(genValue);
		}else if (language.equals(LANGUAGES.EN)){
			localName = types_en.get(genValue);
		}
		if (localName == null){
			Debugger.println("AlarmType.java - getLocal() has no '" + language + "' version for '" + genValue + "'", 3);
			return "";
		}
		return localName;
	}
	
	//-------------------

	User user;
	NluInput nluInput;
	String language;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.nluInput = nluInput;
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.nluInput = nluResult.input;
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	@Override
	public boolean isGeneric(){
		return false;
	}

	@Override
	public String extract(String input) {
		String type = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.ALARM_TYPE);
		if (pr != null){
			type = pr.getExtracted();
			this.found = pr.getFound();
			
			return type;
		}
		
		//GERMAN
		if (language.equals(LANGUAGES.DE)){
			//split named timer/alarms for better handling - this has to be done in every parameter extraction that refers to AlarmType ... :-(
			input = input.replaceAll("-", " ").replaceAll("(\\w+)(timer|wecker|alarm|countdown|erinnerung|stoppuhr|termin)", "$1 $2");
			
			found = NluTools.stringFindFirst(input, "wecker|wecke|weck|aufstehen|aufwachen|aufwecken|uhr (raus|los)|alarm(e|s|)|"
					+ "timer(s|)|stop(p| )uhr(en|)|countdown(s|)|zeitnehmer|zeitgeber|zeitmesser|"
					+ "erinner(ung|e|n)|termin(e|)|kalender");
			
			if (!found.isEmpty()){
				if (NluTools.stringContains(found, "wecker|wecke|weck|aufstehen|aufwachen|aufwecken|uhr (raus|los)|alarm(e|s|)")){
					type = "<" + Type.alarmClock.name() + ">";
				
				}else if (NluTools.stringContains(found, "timer(s|)|stop(p| )uhr(en|)|countdown(s|)|zeitnehmer|zeitgeber|zeitmesser")){
					type = "<" + Type.timer.name() + ">";
				
				}else if (NluTools.stringContains(found, "erinner(ung|e|n)")){
					type = "<" + Type.reminder.name() + ">";
				
				}else if (NluTools.stringContains(found, "termin(e|)|kalender")){
					type = "<" + Type.appointment.name() + ">";
				}
			}
		
		//OTHER
		}else{
			//split named timer/alarms for better handling - this has to be done in every parameter extraction that refers to AlarmType ... :-(
			input = input.replaceAll("-", " ").replaceAll("(\\w+)(timer|alarm|countdown|reminder|stopwatch|appointment)", "$1 $2");
			
			found = NluTools.stringFindFirst(input, "wake (me|up)|alarm(s|)|get up|"
					+ "timer(s|)|time clock|stop( |)watch(es|)|countdown(s|)|remind me|reminder(s|)|remember|appointment(s|)|calendar(s|)");
			
			if (!found.isEmpty()){
				if (NluTools.stringContains(found, "wake (me|up)|alarm(s|)|get up")){
					type = "<" + Type.alarmClock.name() + ">";
				
				}else if (NluTools.stringContains(found, "timer(s|)|time clock|stop( |)watch(es|)|countdown(s|)")){
					type = "<" + Type.timer.name() + ">";
				
				}else if (NluTools.stringContains(found, "remind me|reminder(s|)|remember")){
					type = "<" + Type.reminder.name() + ">";

				}else if (NluTools.stringContains(found, "appointment(s|)|calendar(s|)")){
					type = "<" + Type.appointment.name() + ">";
				}
			}
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.ALARM_TYPE, type, found);
		nluInput.addToParameterResultStorage(pr);
		
		return type;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}
	
	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		String inputLocal = getLocal(input, language);
		if (inputLocal.isEmpty()){
			return "";
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, inputLocal);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\":.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
			//System.out.println("IS VALID: " + input); 		//debug
			return true;
		}else{
			return false;
		}
	}

	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}

}
