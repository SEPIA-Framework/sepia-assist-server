package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Place implements ParameterHandler{
	
	//-----data-----
	public static String pois_de = "bar|bars|pub|pubs|cafe|cafes|kaffee|coffeeshop|coffee-shop|kiosk|"
			+ "baeckerei(en|)|baecker|(pizza|doener|pommes|schnitzel)(-| |)(laden|laeden|bude|buden|imbiss|)|imbiss|"
			+ "(\\b\\w{3,}(sches |sche )|)(restaurant|restaurants|imbiss)|\\b\\w{3,}(sches |sche )(essen)|(et|)was (zu |)(essen|futtern)|futter|"
			+ "disco|discos|diskothek|club|clubs|bordell|nachtclub|puff|tanzen|feiern|party|"
			+ "krankenhaus|krankenhaeuser|"
			+ "apotheke|doktor|arzt|"
			+ "tankstelle|tankstellen|tanken|"
			+ "(polizei|feuerwehr)(-station|station| station|-wache| wache|wache|)(en|)|"
			+ "hotel|hotels|motel|hostel(s|)|"
			+ "kirche(n|)|moschee(n|)|tempel|synagoge(n|)|"
			+ "aldi|lidl|penny markt|penny|edeka|netto|rewe|kaufland|supermarkt|supermaerkte|markt|einkaufen|"
			+ "einkaufszentrum|shoppingcenter|shoppingzentrum|kaufen|shop|"
			+ "\\w*(-bahnhof|bahnhof|bahn)|hbf|haltestelle|flughafen|sixt|europcar|starcar|autovermietung|taxi stand|taxi|"
			+ "geldautomat(en|)|atm(s|)|bank(en|)|geld (besorgen|abholen|holen|abheben)|geld|"
			+ "blumen(| )(l(ae|a)den|geschaeft(e|))|"
			+ "werkstatt|werkstaetten|auto schuppen|schrauber bude|"
			+ "(fussball|basketball|football|baseball|hockey|)(| )(stadion|stadium)|"
			+ "uni|universitaet(en|)|"
			+ "kino|cinemaxx|uci|imax|"
			+ "zoo(s|)";
	
	public static String pois_en = "bar|bars|pub|pubs|cafe|cafes|coffeeshop|coffee-shop|kiosk|"
			+ "bakery|bakers|bakehouse|(pizza|doener|fries|schnitzel|kebab)(-| |)(shop|imbiss|place|)|imbiss|"
			+ "(\\b\\w{3,}(an |nese )|)(restaurant|restaurants|diner|takeaway|food)|something to eat|food|"
			+ "disco|discos|diskothek|club|clubs|brothel|nightclub|cathouse|party|dance|"
			+ "hospital(s|)|"
			+ "pharmacy|drugstore|doctor|"
			+ "gas station|gas-station|petrol station|petrol-station|get gas|get petrol|"
			+ "(police|fire)(-station|station| station|department|)(s|)|"
			+ "hotel|hotels|motel|hostel(s|)|"
			+ "church(es|)|mosque(s|)|temple(s|)|synagogue(s|)|synagog|"
			+ "aldi|lidl|penny markt|penny|edeka|netto|rewe|kaufland|supermarket(s|)|market|buy stuff|"
			+ "shopping (center|mall)(s|)|shop(s|)|"
			+ "\\w*(-station| station|train)|(bus|train) stop|airport|sixt|europcar|starcar|car rental|need a cab|taxi stand|taxi|"
			+ "cash machine|atm(s|)|bank(s|)|get (money|cash)|"
			+ "flower(| )(store|shop)(s|)|"
			+ "workshop(s|)|"
			+ "(soccer|football|basketball|baseball|hockey|)( stadium|stadium)(s|)|"
			+ "uni|university|universities|"
			+ "cinema|cinemaxx|uci|imax|"
			+ "zoo(s|)";
	
	/**
	 * Get a POI out of the list. 
	 * Careful if you search "Jesus Hospital SomeCity" then it will ONLY return "hospital"!
	 */
	public static JSONObject getPOI(String input, String language){
		JSONObject poiJSON = new JSONObject();
		String poi = "";
		String poiLocation = "";
		String distanceTag = "";
		input = input.toLowerCase();
		if (language.equals(LANGUAGES.DE)){
			poi = NluTools.stringFindFirst(input, pois_de);
			//exceptions
			if (!poi.isEmpty() && poi.matches("hauptbahnhof|bahnhof|hbf") && input.matches("(.*\\s|)" + Pattern.quote(poi) + "\\s\\w+")){
				poiLocation = input.replaceFirst(".*" + Pattern.quote(poi), "").trim();
				distanceTag = "<in>";
			}
			//regular
			else if (!poi.isEmpty()){
				poiLocation = NluTools.stringFindFirst(input, Pattern.quote(poi)
						+ "\\s((die |das |welches |)(hier |)(in der naehe|in naehe|nahe|nah|in|auf|bei)( von| zu| bei|)( der| dem| des|))\\s");
				if (!poiLocation.isEmpty()){
					poiLocation = NluTools.stringFindFirst(input, Pattern.quote(poiLocation)
							+ "(\\d+\\s|)(\\w+(-|, | )\\w+(-|, | )\\w+|\\w+(-|, | )\\w+|\\w+)");
					if (NluTools.stringContains(poiLocation, "naehe|nahe|nah|bei")){
						distanceTag = "<close>";
					}else{
						distanceTag = "<in>";
					}
					poiLocation = poiLocation.replaceAll(Pattern.quote(poi)
						+ "\\s((die |das |welches |)(hier |)(in der naehe|in naehe|nahe|nah|in|auf|bei)( von| zu| bei|)( der| dem| des|))", "").trim();
					//clean up
					poiLocation = poiLocation.replaceAll("^(hier |)(naehe|nahe|nah|hier)", "").trim();
					poiLocation = poiLocation.replaceAll("^(von|zu|bei)", "").trim();
					poiLocation = poiLocation.replaceAll("(sind|ist)$", "").trim();
				}
			}
			JSON.add(poiJSON, "poi", poi);
			JSON.add(poiJSON, "poiLocation", poiLocation);
			JSON.add(poiJSON, "distanceTag", distanceTag);
			
		}else if (language.equals(LANGUAGES.EN)){
			poi = NluTools.stringFindFirst(input, pois_en);
			//exceptions
			if (!poi.isEmpty() && (poi.equals("main station") || poi.equals("central station")) && input.matches("(.*\\s|)" + Pattern.quote(poi) + "\\s\\w+")){
				poiLocation = input.replaceFirst(".*" + Pattern.quote(poi), "").trim();
				distanceTag = "<in>";
			}
			//regular
			else if (!poi.isEmpty()){
				poiLocation = NluTools.stringFindFirst(input, Pattern.quote(poi)
						+ "\\s(((that |which )(is |are |)|)(here |)(in the (vicinity|proximity)|in (vicinity|proximity)|close|nearby|near|in|on|at)( by| to| of|)( the|))\\s");
				if (!poiLocation.isEmpty()){
					poiLocation = NluTools.stringFindFirst(input, Pattern.quote(poiLocation)
							+ "(\\d+\\s|)(\\w+(-|, | )\\w+(-|, | )\\w+|\\w+(-|, | )\\w+|\\w+)");
					if (NluTools.stringContains(poiLocation, "vicinity|proximity|nearby|near")){
						distanceTag = "<close>";
					}else{
						distanceTag = "<in>";
					}
					poiLocation = poiLocation.replaceAll(Pattern.quote(poi)
						+ "\\s(((that |which )(is |are |)|)(here |)(in the (vicinity|proximity)|in (vicinity|proximity)|close|nearby|near|in|on|at)( by| to| of|)( the|))", "").trim();
					//clean up
					poiLocation = poiLocation.replaceAll("^(here |)(vicinity|proximity|close|nearby|near|here)", "").trim();
					poiLocation = poiLocation.replaceAll("^(by|to|of)", "").trim();
					poiLocation = poiLocation.replaceAll("(are|is)$", "").trim();
				}
			}
			JSON.add(poiJSON, "poi", poi);
			JSON.add(poiJSON, "poiLocation", poiLocation);
			JSON.add(poiJSON, "distanceTag", distanceTag);
			
		}else{
			Debugger.println("Place.java - getPOI() has no support for language '" + language + "'", 1);
			JSON.add(poiJSON, "poi", "");
			JSON.add(poiJSON, "poiLocation", "");
			JSON.add(poiJSON, "distanceTag", "");
		}
		return poiJSON;
	}
	/**
	 * Get a POI type from an input that (should) contain a POI. Is e.g. used to optimize map search.
	 * No match returns empty.  
	 */
	public static String getPoiType(String input, String language){
		//DE
		if (language.equals(LANGUAGES.DE)){
			if (NluTools.stringContains(input, "bar|bars|pub|pubs|drinks|was trinken")){
				return "bar";
			}else if (NluTools.stringContains(input, "kiosk|bude")){
				return "store|liquor_store";
			}else if (NluTools.stringContains(input, "cafe|cafes|kaffee|coffeeshop|coffee-shop")){
				return "cafe";
			}else if (NluTools.stringContains(input, "(pizza|doener|pommes|schnitzel)(-| |)(laden|laeden|bude|buden|imbiss|)|imbiss|(\\b\\w{3,}(sches |sche )|)(restaurant|restaurants|imbiss)|\\b\\w{3,}(sches |sche )(essen)|(et|)was (zu |)(essen|futtern)|futter")){
				return "food|restaurant|meal_takeaway";
			}else if (NluTools.stringContains(input, "baeckerei(en|)|baecker")){
				return "bakery";
			}else if (NluTools.stringContains(input, "tankstelle|tankstellen|tanken")){
				return "gas_station";
			}else if (NluTools.stringContains(input, "auto waschen")){
				return "car_wash";
			}else if (NluTools.stringContains(input, "hotel|hotels|motel|hostel(s|)")){
				return "lodging";
			}else if (NluTools.stringContains(input, "disco|discos|diskothek|club|clubs|nachtclub|tanzen|feiern|party")){
				return "night_club";
			}else if (NluTools.stringContains(input, "\\w*(-bahnhof|bahnhof)|hbf")){
				return "train_station";
			}else if (NluTools.stringContains(input, "\\w*(-|)haltestelle|bus|u(-| |)bahn.*")){
				if (NluTools.stringContains(input, "u(-| |)bahn.*")){
					return "subway_station|transit_station";
				}else if (NluTools.stringContains(input, ".*bus.*")){
					return "bus_station";
				}else{
					return "transit_station|bus_station|subway_station|train_station";
				}
			}else if (NluTools.stringContains(input, "flughafen")){
				return "airport";
			}else if (NluTools.stringContains(input, "sixt|europcar|starcar|autovermietung")){
				return "car_rental";
			}else if (NluTools.stringContains(input, "taxi stand|taxi")){
				return "taxi_stand";
			}else if (NluTools.stringContains(input, "museum")){
				return "museum";
			}else if (NluTools.stringContains(input, "geldautomat(en|)|atm(s|)|geld (besorgen|abholen|holen|abheben)|geld")){
				return "atm|bank|finance";
			}else if (NluTools.stringContains(input, "bank(en|)")){
				return "bank|finance";
			}else if (NluTools.stringContains(input, "aldi|lidl|penny|edeka|netto|rewe|supermarkt|supermaerkte|markt|einkaufen")){
				return "grocery_or_supermarket";
			}else if (NluTools.stringContains(input, "krankenhaus|krankenhaeuser|notaufnahme")){
				return "hospital";
			}else if (NluTools.stringContains(input, "doktor|arzt")){
				return "doctor";
			}else if (NluTools.stringContains(input, "apotheke|medikamente")){
				return "pharmacy";
			}else if (NluTools.stringContains(input, "(polizei)(-station|station| station|-wache| wache|wache|)(en|)")){
				return "police";
			}else if (NluTools.stringContains(input, "(feuerwehr)(-station|station| station|-wache| wache|wache|)(en|)")){
				return "fire_station";
			}else if (NluTools.stringContains(input, "kirche(n|)|moschee(n|)|tempel|synagoge(n|)")){
				return "place_of_worship|church|hindu_temple|mosque|synagogue";
			}else if (NluTools.stringContains(input, "werkstatt|werkstaetten|auto schuppen|schrauber bude")){
				return "car_repair";
			}else if (NluTools.stringContains(input, "(fussball|basketball|football|baseball|hockey|)(| )(stadion|stadium)")){
				return "stadium";
			}else if (NluTools.stringContains(input, "uni|universitaet(en|)")){
				return "university";
			}else if (NluTools.stringContains(input, "kino|cinemaxx|uci|imax")){
				return "movie_theater";
			}else if (NluTools.stringContains(input, "einkaufszentrum|shoppingcenter|shoppingzentrum")){
				return "shopping_mall|store";
			}else if (NluTools.stringContains(input, "kaufen|shop|\\w*laden|geschaeft")){
				return "store";
			}else if (NluTools.stringContains(input, "casino")){
				return "casino";
			}else if (NluTools.stringContains(input, "bordell|puff")){
				return "establishment";
			}else if (NluTools.stringContains(input, "zoo")){
				return "zoo";
			}
		//EN	
		}else if (language.equals(LANGUAGES.EN)){
			if (NluTools.stringContains(input, "bar|bars|pub|pubs|drinks|get drunk")){
				return "bar|liquor_store";
			}else if (NluTools.stringContains(input, "cafe|cafes|coffee(-| |)(shop|)")){
				return "cafe";
			}else if (NluTools.stringContains(input, "liquor(-| |)store|get (alkohol|spirit|booze)")){
				return "liquor_store";
			}else if (NluTools.stringContains(input, "(pizza|doener|fries|schnitzel|kebab)(-| |)(shop|imbiss|place|)|imbiss|(\\b\\w{3,}(an |nese )|)(restaurant|restaurants|diner|takeaway|food)|something to eat|food")){
				return "food|restaurant|meal_takeaway";
			}else if (NluTools.stringContains(input, "bakery|bakers|bakehouse")){
				return "bakery";
			}else if (NluTools.stringContains(input, "disco|discos|diskothek|club|clubs|nightclub|party|dance")){
				return "night_club";
			}else if (NluTools.stringContains(input, "hospital(s|)|emergency room")){
				return "hospital";
			}else if (NluTools.stringContains(input, "pharmacy|drugstore")){
				return "pharmacy";
			}else if (NluTools.stringContains(input, "doctor")){
				return "doctor|pharmacy";
			}else if (NluTools.stringContains(input, "\\w*(-station| station|train)|(bus|train) stop")){
				if (NluTools.stringContains(input, ".*bus.*")){
					return "bus_station";
				}else if (NluTools.stringContains(input, ".*train.*")){
					return "train_station";
				}else if (NluTools.stringContains(input, ".*subway.*")){
					return "subway_station|train_station";
				}else{
					return "transit_station|train_station";
				}
			}else if (NluTools.stringContains(input, "airport")){
				return "airport";
			}else if (NluTools.stringContains(input, "sixt|europcar|starcar|car rental")){
				return "car_rental";
			}else if (NluTools.stringContains(input, "need a cab|taxi stand|taxi")){
				return "taxi_stand";
			}else if (NluTools.stringContains(input, "museum")){
				return "museum";
			}else if (NluTools.stringContains(input, "(gas|petrol)(-| |)station|get (gas|petrol)")){
				return "gas_station";
			}else if (NluTools.stringContains(input, "car(-| |)wash|wash\\b.* car")){
				return "car_wash";
			}else if (NluTools.stringContains(input, "(police)(-station|station| station|department|)(s|)")){
				return "police";
			}else if (NluTools.stringContains(input, "(fire)(-station|station| station|department|)(s|)")){
				return "fire_station";
			}else if (NluTools.stringContains(input, "hotel|hotels|motel|hostel(s|)")){
				return "lodging";
			}else if (NluTools.stringContains(input, "church(es|)|mosque(s|)|temple(s|)|synagogue(s|)|synagog")){
				return "place_of_worship|church|hindu_temple|mosque|synagogue";
			}else if (NluTools.stringContains(input, "aldi|lidl|penny|edeka|netto|rewe|supermarket(s|)|market|buy stuff")){
				return "grocery_or_supermarket";
			}else if (NluTools.stringContains(input, "cash machine|atm(s|)|get (money|cash)")){
				return "atm|bank|finance";
			}else if (NluTools.stringContains(input, "bank(s|)")){
				return "bank|finance";
			}else if (NluTools.stringContains(input, "workshop(s|)")){
				return "car_repair";
			}else if (NluTools.stringContains(input, "(soccer|football|basketball|baseball|hockey|)( stadium|stadium)")){
				return "stadium";
			}else if (NluTools.stringContains(input, "uni|university")){
				return "university";
			}else if (NluTools.stringContains(input, "cinema|cinemaxx|uci|imax")){
				return "movie_theater";
			}else if (NluTools.stringContains(input, "shopping (center|mall)")){
				return "shopping_mall|store";
			}else if (NluTools.stringContains(input, "\\w*shop|\\w*store|kiosk")){
				return "store";
			}else if (NluTools.stringContains(input, "brothel|cathouse")){
				return "establishment";
			}else if (NluTools.stringContains(input, "zoo")){
				return "zoo";
			}
		}
		else{
			Debugger.println("Place.java - getPoiType() has no support for language '" + language + "'", 1);
			return "";
		}
		return "";
	}
	
	public static HashMap<String, String> poiTypes_de = new HashMap<>();
	public static HashMap<String, String> poiTypes_en = new HashMap<>();
	static {
		poiTypes_de.put("<food>", "was zu Essen");
				
		poiTypes_en.put("<food>", "food");
	}
	/**
	 * Try to build a "close to location" search term that can be used e.g. in Google places API.
	 * @param place - place to start with, e.g. a POI
	 * @param locationJSON - location JSONObject containing the data of the "close to" place
	 */
	public static String buildCloseToSearch(String place, JSONObject locationJSON){
		//build close to there search
		String searchPlace = "";
		String city = (String) locationJSON.get(LOCATION.CITY);
		String street = (String) locationJSON.get(LOCATION.STREET);
		if ((city != null && !city.isEmpty()) && (street != null && !street.isEmpty())){
			searchPlace = place + " close to " + street + ", " + city;
		}else if (city != null && !city.isEmpty()){
			searchPlace = place + " close to " + city;
		}else if (street != null && !street.isEmpty()){
			searchPlace = place + " close to " + street;
		}else{
			searchPlace = place;
			Debugger.println("Place.buildCloseToSearch - location info is incomplete (req. city or street)!", 1);
		}
		return searchPlace;
	}
	/**
	 * Translate generalized POI type value (e.g. &lt;food&gt;) to local name (e.g. was zu Essen).
	 * If generalized value is unknown returns empty string.
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = poiTypes_de.get(value);
		}else if (language.equals(LANGUAGES.EN)){
			localName = poiTypes_en.get(value);
		}
		if (localName == null){
			Debugger.println("Place.java - getLocal() has no '" + language + "' version for '" + value + "'", 1);
			return "";
		}
		return localName;
	}
	//----------------

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
		place = locations.get("location").trim();
		found = place;
		
		//reconstruct original phrase to get proper item names
		Normalizer normalizer = Config.inputNormalizers.get(language);
		place = normalizer.reconstructPhrase(nlu_input.textRaw, place);

		return place;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}

	@Override
	public String getFound() {
		return found;			//TODO: might not always work if locations are special tags
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			input = input.trim().replaceFirst("(?i)\\b(in |nach |von |auf |ueber |am |an |bei |)"
					+ "(der |die |das |dem |den |einer |einem |eines |meiner |meinem |meines |)" + Pattern.quote(found) + "\\b", "").trim();
		}else{
			input = input.trim().replaceFirst("(?i)\\b(in |at |from |to |over |)"
					+ "(a |the |my |)" + Pattern.quote(found) + "\\b", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			return input.replaceAll("(?i).*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)( naechste|)\\b", "").trim();
		}else{
			return input.replaceAll("(?i).*\\b(a|the)( closest|)\\b", "").trim();
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
