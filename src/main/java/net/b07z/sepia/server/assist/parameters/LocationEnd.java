package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.Place.LocationBuildResult;
import net.b07z.sepia.server.assist.parameters.Place.LocationExtractResult;
import net.b07z.sepia.server.assist.users.User;

public class LocationEnd implements ParameterHandler{

	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nlu_input;	
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
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
		LocationExtractResult result = Place.extractLocation("location_end", input, this.nlu_input, this.user);
		String returnValue = result.returnValue;
		this.found = result.found;

		return returnValue;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}

	@Override
	public String getFound() {
		return found;					//TODO: might fail in some cases with tagged words
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			input = input.trim().replaceFirst("(?i)\\b(in |nach |auf |am |an |bei |)"
					+ "(der |die |das |dem |den |einer |einem |eines |meiner |meinem |meines |)" + Pattern.quote(found) + "\\b", "").trim();
		}else{
			input = input.trim().replaceFirst("(?i)\\b(in |at |from |)"
					+ "(a |the |my |)" + Pattern.quote(found) + "\\b", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.trim().replaceAll("(?i)^(bis|)(nach|zum|zur|zu)\\b", "").trim();
			return input.replaceAll("(?i)^(der|die|das|dem|den|einem|einer|eines)\\b", "").trim();
		}else{
			input = input.trim().replaceAll("(?i)^(to)\\b", "").trim();
			return input.replaceAll("(?i)^(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		LocationBuildResult result = Place.buildLocation(input, nlu_input, user);
		buildSuccess = result.buildSuccess;
		return ((result.errorData == null)? result.locationJSON.toJSONString() : result.errorData);
	}
	
	@Override
	public boolean validate(String input) {
		return Place.validateLocation(input);
	}
	
	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}
	
}
