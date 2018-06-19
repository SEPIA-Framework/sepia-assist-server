package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * What is the name of the alarm
 * 
 * @author Florian Quirin
 *
 */
public class AlarmName implements Parameter_Handler{
	
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
		String name = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.ALARM_NAME);
		if (pr != null){
			name = pr.getExtracted();
			this.found = pr.getFound();
			
			return name;
		}
		
		//remove time first if present
		ParameterResult prTime = ParameterResult.getResult(nluInput, PARAMETERS.TIME, input);
		String exTime = prTime.getExtracted();
		if (!exTime.isEmpty()){
			input = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.TIME, prTime, input);
		}
		//then remove action
		ParameterResult prAction = ParameterResult.getResult(nluInput, PARAMETERS.ACTION, input);
		String exAction = prAction.getExtracted();
		if (!exAction.isEmpty()){
			input = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.ACTION, prAction, input);
		}
		
		//GERMAN
		if (language.equals(LANGUAGES.DE)){
			//split named timer/alarms for better handling - this has to be done in every parameter extraction that refers to AlarmType ... :-(
			input = input.replaceAll("-", " ").replaceAll("(\\w+)(timer|wecker|alarm|countdown|erinnerung|stoppuhr|termin)", "$1 $2");
						
			String alarm = "wecker|alarm(e|s|)|timer(s|)|stop(p| )uhr(en|)|countdown(s|)|zeitnehmer|zeitgeber|zeitmesser|"
					+ "erinner(ung|e|n)|termin(e|)";

			if (NluTools.stringContains(input, "(fuer(s|)|zum|zur|zu (dem|der)|wegen|daran|ans|an) .*")){
				found = input.replaceFirst(".*?\\b(fuer(s|)|zum|zur|zu (dem|der)|wegen|daran|ans|an)\\b", "").trim();
				if (found.matches("dass .* (muss|will|kann|soll|sollte|muesste)")){
					found = found.replaceFirst("dass( ich| du|)( noch|)( .* )(muss|will|kann|soll|sollte|muesste)", "$1 $4 $2 $3").replaceAll("\\s+", " ").trim();
				}else if (found.matches("dass .* \\w+$")){
					found = found.replaceFirst(".*?\\bdass( ich| du|)( noch|)\\b", "").trim();
					found = found.replaceFirst("(\\w+)e$", "$1en");
				}
				found = found.replaceAll("\\b(der|die|das|des|den|ein|einen|eine|mein|meinen|meine|dass) (" + alarm + ")\\b", "").trim();
				//reformat grammar
				found = found.replaceFirst("(.*)(zu)( |)(\\w+)$\\b", "$1 $4").replaceAll("\\s+", " ").trim();

			}else if (NluTools.stringContains(input, "\\w+ (" + alarm + ")")){
				found = input.replaceFirst("(.*) (" + alarm + ")\\b.*", "$1");
				found = found.replaceAll(".*\\b(der|die|das|des|den|ein|einen|eine|mein|meinen|meine|dass)\\b", "").trim();
			
			}else if (NluTools.stringContains(input, "(erinnerung|termin) \\w+")){
				found = input.replaceFirst(".*\\b(erinnerung|termin)", "").trim();
			}
			
			if (!found.isEmpty()){
				name = found;
			}
		
		//OTHER
		}else{
			//split named timer/alarms for better handling - this has to be done in every parameter extraction that refers to AlarmType ... :-(
			input = input.replaceAll("-", " ").replaceAll("(\\w+)(timer|alarm|countdown|reminder|stopwatch|appointment)", "$1 $2");
			
			String alarm = "alarm(s|)|timer(s|)|time clock|stop( |)watch(es|)|countdown(s|)|reminder(s|)|appointment(s|)";
			
			if (NluTools.stringContains(input, "(for|because of|remind( \\w+|) to) .*")){
				found = input.replaceFirst(".*?\\b(for|because of|remind( \\w+|) to)\\b", "").trim();
				found = found.replaceAll("\\b(the|a|my|that) (" + alarm + ")\\b", "").trim();
				
			}else if (NluTools.stringContains(input, "\\w+ (" + alarm + ")")){
				found = input.replaceFirst("(.*) (" + alarm + ")\\b.*", "$1");
				found = found.replaceAll(".*\\b(the|a|my|that)\\b", "").trim();

			}else if (NluTools.stringContains(input, "(reminder|appointment) \\w+")){
				found = input.replaceFirst(".*\\b(reminder|appointment)", "").trim();
			}
			
			if (!found.isEmpty()){
				name = found;
			}
		}
		
		//reconstruct original phrase to get proper item names
		if (!name.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(language);
			name = normalizer.reconstructPhrase(nluInput.textRaw, name);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.ALARM_NAME, name, found);
		nluInput.addToParameterResultStorage(pr);
		
		return name;
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
		return NluTools.stringRemoveFirst(input, found);			//TODO: improve with e.g. ... dass ich 'found'
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
		
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
