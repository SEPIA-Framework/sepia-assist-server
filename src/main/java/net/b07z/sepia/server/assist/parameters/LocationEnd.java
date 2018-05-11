package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

public class LocationEnd implements Parameter_Handler{

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
		String place;
		HashMap<String, String> locations = null;
	
		//check storage first
		ParameterResult pr = nlu_input.getStoredParameterResult(PARAMETERS.PLACE);		//locations always use PLACE for the general HashMap
		if (pr != null){
			locations = pr.getExtractedMap();
		}
		if (locations == null){
			//search all locations
			locations = RegexParameterSearch.get_locations(input, language);
			
			//store it
			pr = new ParameterResult(PARAMETERS.PLACE, locations, "");
			nlu_input.addToParameterResultStorage(pr);		
		}
		place = locations.get("location_end").trim();
		found = place;
		
		//reconstruct original phrase to get proper item names
		Normalizer normalizer = Config.inputNormalizers.get(language);
		place = normalizer.reconstructPhrase(nlu_input.text_raw, place);

		return place;
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
		Object[] result = LocationStart.buildLocation(input, nlu_input, user);
		buildSuccess = (boolean) result[0];
		return (String) result[1];
	}
	
	@Override
	public boolean validate(String input) {
		return LocationStart.validateLocation(input);
	}
	
	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}
	
}
