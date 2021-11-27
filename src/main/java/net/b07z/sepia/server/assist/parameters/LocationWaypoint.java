package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.Place.LocationBuildResult;
import net.b07z.sepia.server.assist.parameters.Place.LocationExtractResult;
import net.b07z.sepia.server.assist.users.User;

public class LocationWaypoint implements ParameterHandler{

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
		LocationExtractResult result = Place.extractLocation("location_waypoint", input, this.nlu_input, this.user);
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
		return found;					//TODO: might fail in some cases (tagged sepcial words)
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			input = input.trim().replaceFirst("(?i)\\b(zwischenstopp |stop |)(in |auf |ueber |am |an |bei |)"
					+ "(der |die |das |dem |den |einer |einem |eines |meiner |meinem |meines |)" + Pattern.quote(found) + "\\b", "").trim();
		}else{
			input = input.trim().replaceFirst("(?i)\\b(stopover |stop |)(in |at |over |)"
					+ "(a |the |my |)" + Pattern.quote(found) + "\\b", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll("(?i)^(ueber|in|bei|zwischenstopp|stop)\\b", "").trim();
			return input.replaceAll("(?i)^(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)\\b", "").trim();
		}else{
			input = input.replaceAll("(?i)^(at|in|over|stopover|stop)\\b", "").trim();
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
