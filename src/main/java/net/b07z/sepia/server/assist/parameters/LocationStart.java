package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerStatics;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.Interview;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tools.GeoCoding;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class LocationStart implements Parameter_Handler{

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
		place = locations.get("location_start").trim();
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
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst("(?i)\\b(in |von |auf |am |an |bei |)"
					+ "(der |die |das |dem |den |einer |einem |eines |meiner |meinem |meines |)" + Pattern.quote(found) + "\\b", "").trim();
		}else{
			input = input.replaceFirst("(?i)\\b(in |at |from |)"
					+ "(a |the |my |)" + Pattern.quote(found) + "\\b", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		//TODO: improve
		if (language.equals(LANGUAGES.DE)){
			input = input.trim().replaceAll("(?i)^(in|auf|bei|von|am|an)\\b", "").trim();
			return input.replaceAll("(?i)^(der|die|das|dem|den|einer|einem|eines)\\b", "").trim();
		}else{
			input = input.trim().replaceAll("(?i)^(in|at|on|from)\\b", "").trim();
			return input.replaceAll("(?i)^(a|the)\\b", "").trim();
		}
	}

	@Override
	public String getFound() {
		return found;					//TODO: might not always work when tagged
	}

	@Override
	public String build(String input) {
		Object[] result = buildLocation(input, nlu_input, user);
		buildSuccess = (boolean) result[0];
		return (String) result[1];
	}
	/**
	 * Static version of build to be reusable for other location methods. Returns "buildSuccess", "result" and "resultAlternatives" String as Object[]. 
	 */
	public static Object[] buildLocation(String input, NluInput nluInput, User user) {
		//TODO: fuse all 3 types (specific location, personal location and contacts) into one?!?
		
		boolean buildSuccess = false;
		
		//-check for user specific location ("<user_home>")
		String specificLocation_tag = User.containsUserSpecificLocation(input, user);
		if (!specificLocation_tag.isEmpty()){
			JSONObject specificLocationJSON = LOCATION.getFullAddressJSON(user, specificLocation_tag);
			//System.out.println("SPEC. LOCATION: " + specificLocation_tag); 		//DEBUG
			//System.out.println("SPEC. LOCATION JSON: " + specificLocationJSON); 	//DEBUG
			if (specificLocationJSON != null && !specificLocationJSON.isEmpty()){
				String testLAT = (String) specificLocationJSON.get(LOCATION.LAT);
				String testLNG = (String) specificLocationJSON.get(LOCATION.LNG);
				if (testLAT == null || testLAT.isEmpty() || testLNG == null || testLNG.isEmpty()){
					return new Object[]{ buildSuccess, Interview.ERROR_API_FAIL + ";;" + Interview.TYPE_GEOCODING + ";;" + specificLocation_tag};
				}else{
					JSON.add(specificLocationJSON, InterviewData.INPUT, input); 	//add input always
					if (specificLocation_tag.equals("<user_location>")){
						JSON.add(specificLocationJSON, LOCATION.NAME, AnswerStatics.get(AnswerStatics.HERE, nluInput.language));
					}else if (specificLocation_tag.equals("<user_home>")){
						JSON.add(specificLocationJSON, LOCATION.NAME, AnswerStatics.get(AnswerStatics.HOME, nluInput.language));
					}else if (specificLocation_tag.equals("<user_work>")){
						JSON.add(specificLocationJSON, LOCATION.NAME, AnswerStatics.get(AnswerStatics.WORK, nluInput.language));
					}
					buildSuccess = true;
					//return specificLocationJSON.toJSONString();
					return new Object[]{ buildSuccess, specificLocationJSON.toJSONString()};
				}
			}else{
				if (specificLocation_tag.equals("<user_location>")){
					//return Interview.ERROR_MISSING + ";;" + Interview.TYPE_SPECIFIC_LOCATION + ";;" + specificLocation_tag;
					return new Object[]{ buildSuccess, Interview.ERROR_MISSING + ";;" + Interview.TYPE_SPECIFIC_LOCATION + ";;" + specificLocation_tag};
				}else{
					//return Interview.ACTION_ADD + ";;" + Interview.TYPE_SPECIFIC_LOCATION + ";;" + specificLocation_tag;
					return new Object[]{ buildSuccess, Interview.ACTION_ADD + ";;" + Interview.TYPE_SPECIFIC_LOCATION + ";;" + specificLocation_tag};
				}
			}
		}
		//-check for personal location ("my favorite restaurant")
		//TODO: needs rework!
		/*
		String[] personalInfo_data = User.containsPersonalUserInfo(input, user);
		String personalInfo_item = personalInfo_data[3];
		String personalInfo_str = personalInfo_data[0];
		if (!personalInfo_item.isEmpty()){
			JSONObject personalInfoJSON = JSON.parseStringOrFail(personalInfo_item);
			JSONObject personalLocationJSON = (JSONObject) personalInfoJSON.get(Address.FIELD); 
			if (personalLocationJSON != null && !personalLocationJSON.isEmpty()){
				JSON.add(personalLocationJSON, InterviewData.INPUT, input); 	//add input always
				JSON.add(personalLocationJSON, LOCATION.NAME, personalInfo_str);
				buildSuccess = true;
				//return personalLocationJSON.toJSONString();
				return new Object[]{ buildSuccess, personalLocationJSON.toJSONString()};
			}else{
				//return Interview.ACTION_ADD + ";;" + Interview.TYPE_PERSONAL_LOCATION + ";;" + personalInfo_str;
				return new Object[]{ buildSuccess, Interview.ACTION_ADD + ";;" + Interview.TYPE_PERSONAL_LOCATION + ";;" + personalInfo_str};
			}
		}
		*/
		//-check for contact location
		//TODO: needs rework!
		/*
		Object[] contactsSearch = User.containsContact(input, user);
		String contactMatch = User.getContact(contactsSearch);
		//TODO: choose when multiple hits
		//TODO: rework that completly?
		if (!contactMatch.isEmpty()){
			//multiple matches (name but no meta, should only happen in this case)
			if (contactMatch.contains("<meta>")){
				//get contact address from meta
				personalInfo_item = User.getAttribute(contactMatch, "meta");
				if (!personalInfo_item.isEmpty()){
					JSONObject personalInfoJSON = JSON.parseStringOrFail(personalInfo_item);
					JSONObject personalLocationJSON = (JSONObject) personalInfoJSON.get(Address.FIELD); 
					if (personalLocationJSON != null && !personalLocationJSON.isEmpty()){
						JSON.add(personalLocationJSON, InterviewData.INPUT, input); 	//add input always
						JSON.add(personalLocationJSON, LOCATION.NAME, contactMatch);
						buildSuccess = true;
						//return personalLocationJSON.toJSONString();
						return new Object[]{ buildSuccess, personalLocationJSON.toJSONString()};
					}
				}					
			}
			//return Interview.ACTION_ADD + ";;" + Interview.TYPE_PERSONAL_CONTACT + ";;" + User.getAttribute(contactMatch, "name");
			return new Object[]{ buildSuccess, Interview.ACTION_ADD + ";;" + Interview.TYPE_PERSONAL_CONTACT + ";;" + User.getAttribute(contactMatch, "name")};
		}
		*/
		//-check for point of interest
		JSONObject poiJSON = Place.getPOI(input, nluInput.language);
		String poi = (String) poiJSON.get("poi");
		if (!poi.isEmpty()){
			//System.out.println("inputPOI: " + input); 		//debug
			String searchPOI = "";
			String searchPOI_coarse = "";
			//System.out.println("POI to discuss: " + input); 	//debug
			//try to add a "close to" search if POI is the input or ends on the POI so chance is high that it is a local POI
			if (input.toLowerCase().equals(poi) || input.toLowerCase().matches(".* " + Pattern.quote(poi) + "$")){
				//build a string that is optimized for google places geocoder
				String poiLocation = (String) poiJSON.get("poiLocation");
				if (poiLocation.isEmpty()){
					JSONObject userLocationJSON = LOCATION.getFullAddressJSON(user, "<user_location>");
					if (userLocationJSON == null || userLocationJSON.isEmpty()){
						return new Object[]{ buildSuccess, Interview.ERROR_MISSING + ";;" + Interview.TYPE_SPECIFIC_LOCATION + ";;" + "<user_location>"};
					}
					//build close to here search
					searchPOI_coarse = input;
					searchPOI = Place.buildCloseToSearch(input, userLocationJSON);
					
				}else{
					//build close to location search
					String distanceTag = (String) poiJSON.get("distanceTag");
					if (distanceTag.equals("<in>")){
						searchPOI = input + " in " + poiLocation;
					}else if (distanceTag.equals("<close>")){
						searchPOI = input + " close to " + poiLocation;
					}else{
						//searchPOI = poi;
						searchPOI = input;
					}
					searchPOI_coarse = input;
				}
				
			//take what is given
			}else{
				searchPOI = input;
			}
			//System.out.println("POI selected: " + searchPOI); 	//debug
			//call the places API with new searchPOI
			//System.out.println("searchPOI: " + searchPOI); 		//debug
			String poiType = Place.getPoiType(searchPOI, nluInput.language);
			JSONArray places = GeoCoding.getPOI(searchPOI, poiType, "", "", nluInput.language);
			if (places.isEmpty() && !searchPOI_coarse.isEmpty()){
				places = GeoCoding.getPOI(searchPOI_coarse, poiType, "", "", nluInput.language);
				Debugger.println("LocationParameter POI - performed 2 tries to find: " + searchPOI, 3);
			}
			if (!places.isEmpty()){
				JSONObject locationJSON = (JSONObject) places.get(0);
				//System.out.println("places: " + places); 			//debug
				if (places.size() > 1){
					int maxOptions = 8;
					JSONArray options = new JSONArray();
					for (int i = 1; i<Math.min(maxOptions, places.size()); i++){
						JSON.add(options, (JSONObject) places.get(i));
					}
					JSON.add(locationJSON, InterviewData.OPTIONS, options);
				}
				//add input too
				JSON.add(locationJSON, InterviewData.INPUT, input);
				JSON.add(locationJSON, InterviewData.LOCATION_NAME, locationJSON.get(LOCATION.NAME));
				buildSuccess = true;
				//return locationJSON.toJSONString();
				return new Object[]{ buildSuccess, locationJSON.toJSONString()};
			}
			//no results
			else{
				return new Object[]{ buildSuccess, Interview.ERROR_API_FAIL + ";;" + Interview.TYPE_GEOCODING + ";;" + "places_poi"};
			}
		}
		//-the input was not empty and none of the personals gave a result. Now we have to work with what we have ^^
		//TODO: double-check the geocoder result for sanity
		//TODO: add a flag that this data is a guess
		JSONObject locationJSON = LOCATION.getInfoBySearch(input, nluInput); //new JSONObject();
		//System.out.println("loc: " + locationJSON.toJSONString()); 		//debug
		if (locationJSON == null || locationJSON.isEmpty()){
			return new Object[]{ buildSuccess, Interview.ERROR_API_FAIL + ";;" + Interview.TYPE_GEOCODING + ";;" + "get_coordinates"};
		}
		//add input too
		JSON.add(locationJSON, InterviewData.INPUT, input);
		buildSuccess = true;
		//return locationJSON.toJSONString();
		return new Object[]{ buildSuccess, locationJSON.toJSONString()}; 
	}

	@Override
	public boolean validate(String input) {
		return validateLocation(input);
	}
	/**
	 * Static version of "validate" to be reusable in other location parameter handlers. 
	 */
	public static boolean validateLocation(String input){
		if (input.matches("^\\{\".*\":.+\\}$") && input.contains("\"" + InterviewData.INPUT + "\"")){
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
