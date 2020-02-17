package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class TravelRequestInfo implements ParameterHandler{

	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nlu_input;
	
	String found = "";
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nlu_input = nluInput;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nlu_input = nluResult.input;
	}
	
	@Override
	public String extract(String input) {
		String travelRequestInfo = "";
		if (language.equals(LANGUAGES.DE)){
			travelRequestInfo = NluTools.stringFindFirst(input, "wie lang(e|)|wann komme|wann bin|dauert|dauer|wie weit|entfernt|entfernung");
			found = travelRequestInfo;
			if (!travelRequestInfo.isEmpty()){
				//transform to google default types
				if (NluTools.stringContains(travelRequestInfo, "wie lang(e|)|wann komme|wann bin|dauert|dauer")){
					travelRequestInfo = "<duration>";
				}else if (NluTools.stringContains(travelRequestInfo, "wie weit|entfernt|entfernung")){
					travelRequestInfo = "<distance>";
				//default
				}else{
					travelRequestInfo = "<overview>";
				}
			}
		}else if (language.equals(LANGUAGES.EN)){
			travelRequestInfo = NluTools.stringFindFirst(input, "how long|when (am|are|do)|duration|how far|distance");
			found = travelRequestInfo;
			if (!travelRequestInfo.isEmpty()){
				//transform to google default types
				if (NluTools.stringContains(travelRequestInfo, "how long|when (am|are|do)|duration")){
					travelRequestInfo = "<duration>";
				}else if (NluTools.stringContains(travelRequestInfo, "how far|distance")){
					travelRequestInfo = "<distance>";
				//default
				}else{
					travelRequestInfo = "<overview>";
				}
			}
		}else{
			Debugger.println("TravelRequestInfo - missing language support for: " + language, 1);
			return "";
		}
		return travelRequestInfo;
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
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst("\\b(" + Pattern.quote(found) + ")\\b", "").trim();
		}else{
			input = input.replaceFirst("\\b(" + Pattern.quote(found) + ")\\b", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//expects a color tag!
		String commonValue = input.replaceAll("^<|>$", "").trim();
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}
	
	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
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
