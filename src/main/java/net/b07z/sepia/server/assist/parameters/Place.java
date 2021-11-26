package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.geo.GeoFactory;
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
			+ "museum|"		//add theater
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
			+ "museum|"		//add theater|theatre
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
	 * Get one or more POI types from an input that contains a POI. Usually used to optimize POI API search.<br>
	 * NOTE: The return string is Google-Maps compatible, for OSM it might need to be converted!<br>
	 * @param input - input string (normalized!) with term that might match a POI, e.g. "supermarket" 
	 * @param language - language code
	 * @return For example: ["hospital"] or ["shopping_mall", "store"] or empty array if no match is found
	 */
	public static String[] getPoiType(String input, String language){
		//DE
		if (language.equals(LANGUAGES.DE)){
			if (NluTools.stringContains(input, "bar|bars|pub|pubs|drinks|was trinken")){
				return new String[]{"bar"};
			}else if (NluTools.stringContains(input, "kiosk|bude")){
				return new String[]{"store", "liquor_store"};
			}else if (NluTools.stringContains(input, "cafe|cafes|kaffee|coffeeshop|coffee-shop")){
				return new String[]{"cafe"};
			}else if (NluTools.stringContains(input, "(pizza|doener|pommes|schnitzel)(-| |)(laden|laeden|bude|buden|imbiss|)|imbiss|(\\b\\w{3,}(sches |sche )|)(restaurant|restaurants|imbiss)|\\b\\w{3,}(sches |sche )(essen)|(et|)was (zu |)(essen|futtern)|futter")){
				return new String[]{"food", "restaurant", "meal_takeaway"};
			}else if (NluTools.stringContains(input, "baeckerei(en|)|baecker")){
				return new String[]{"bakery"};
			}else if (NluTools.stringContains(input, "tankstelle|tankstellen|tanken")){
				return new String[]{"gas_station"};
			}else if (NluTools.stringContains(input, "auto waschen")){
				return new String[]{"car_wash"};
			}else if (NluTools.stringContains(input, "hotel|hotels|motel|hostel(s|)")){
				return new String[]{"lodging"};
			}else if (NluTools.stringContains(input, "disco|discos|diskothek|club|clubs|nachtclub|tanzen|feiern|party")){
				return new String[]{"night_club"};
			}else if (NluTools.stringContains(input, "\\w*(-bahnhof|bahnhof)|hbf")){
				return new String[]{"train_station"};
			}else if (NluTools.stringContains(input, "\\w*(-|)haltestelle|bus|u(-| |)bahn.*")){
				if (NluTools.stringContains(input, "u(-| |)bahn.*")){
					return new String[]{"subway_station", "transit_station"};
				}else if (NluTools.stringContains(input, ".*bus.*")){
					return new String[]{"bus_station"};
				}else{
					return new String[]{"transit_station", "bus_station", "subway_station", "train_station"};
				}
			}else if (NluTools.stringContains(input, "flughafen")){
				return new String[]{"airport"};
			}else if (NluTools.stringContains(input, "sixt|europcar|starcar|autovermietung")){
				return new String[]{"car_rental"};
			}else if (NluTools.stringContains(input, "taxi stand|taxi")){
				return new String[]{"taxi_stand"};
			}else if (NluTools.stringContains(input, "museum")){
				return new String[]{"museum"};
			}else if (NluTools.stringContains(input, "geldautomat(en|)|atm(s|)|geld (besorgen|abholen|holen|abheben)|geld")){
				return new String[]{"atm", "bank", "finance"};
			}else if (NluTools.stringContains(input, "bank(en|)")){
				return new String[]{"bank", "finance"};
			}else if (NluTools.stringContains(input, "aldi|lidl|penny|edeka|netto|rewe|supermarkt|supermaerkte|markt|einkaufen")){
				return new String[]{"grocery_or_supermarket"};
			}else if (NluTools.stringContains(input, "krankenhaus|krankenhaeuser|notaufnahme")){
				return new String[]{"hospital"};
			}else if (NluTools.stringContains(input, "doktor|arzt")){
				return new String[]{"doctor"};
			}else if (NluTools.stringContains(input, "apotheke|medikamente")){
				return new String[]{"pharmacy"};
			}else if (NluTools.stringContains(input, "(polizei)(-station|station| station|-wache| wache|wache|)(en|)")){
				return new String[]{"police"};
			}else if (NluTools.stringContains(input, "(feuerwehr)(-station|station| station|-wache| wache|wache|)(en|)")){
				return new String[]{"fire_station"};
			}else if (NluTools.stringContains(input, "kirche(n|)|moschee(n|)|tempel|synagoge(n|)")){
				return new String[]{"place_of_worship", "church", "hindu_temple", "mosque", "synagogue"};
			}else if (NluTools.stringContains(input, "werkstatt|werkstaetten|auto schuppen|schrauber bude")){
				return new String[]{"car_repair"};
			}else if (NluTools.stringContains(input, "(fussball|basketball|football|baseball|hockey|)(| )(stadion|stadium)")){
				return new String[]{"stadium"};
			}else if (NluTools.stringContains(input, "uni|universitaet(en|)")){
				return new String[]{"university"};
			}else if (NluTools.stringContains(input, "kino|cinemaxx|uci|imax")){
				return new String[]{"movie_theater"};
			}else if (NluTools.stringContains(input, "einkaufszentrum|shoppingcenter|shoppingzentrum")){
				return new String[]{"shopping_mall", "store"};
			}else if (NluTools.stringContains(input, "kaufen|shop|\\w*laden|geschaeft")){
				return new String[]{"store"};
			}else if (NluTools.stringContains(input, "casino")){
				return new String[]{"casino"};
			}else if (NluTools.stringContains(input, "bordell|puff")){
				return new String[]{"establishment"};
			}else if (NluTools.stringContains(input, "zoo")){
				return new String[]{"zoo"};
			}
		//EN	
		}else if (language.equals(LANGUAGES.EN)){
			if (NluTools.stringContains(input, "bar|bars|pub|pubs|drinks|get drunk")){
				return new String[]{"bar", "liquor_store"};
			}else if (NluTools.stringContains(input, "cafe|cafes|coffee(-| |)(shop|)")){
				return new String[]{"cafe"};
			}else if (NluTools.stringContains(input, "liquor(-| |)store|get (alkohol|spirit|booze)")){
				return new String[]{"liquor_store"};
			}else if (NluTools.stringContains(input, "(pizza|doener|fries|schnitzel|kebab)(-| |)(shop|imbiss|place|)|imbiss|(\\b\\w{3,}(an |nese )|)(restaurant|restaurants|diner|takeaway|food)|something to eat|food")){
				return new String[]{"food", "restaurant", "meal_takeaway"};
			}else if (NluTools.stringContains(input, "bakery|bakers|bakehouse")){
				return new String[]{"bakery"};
			}else if (NluTools.stringContains(input, "disco|discos|diskothek|club|clubs|nightclub|party|dance")){
				return new String[]{"night_club"};
			}else if (NluTools.stringContains(input, "hospital(s|)|emergency room")){
				return new String[]{"hospital"};
			}else if (NluTools.stringContains(input, "pharmacy|drugstore")){
				return new String[]{"pharmacy"};
			}else if (NluTools.stringContains(input, "doctor")){
				return new String[]{"doctor", "pharmacy"};
			}else if (NluTools.stringContains(input, "\\w*(-station| station|train)|(bus|train) stop")){
				if (NluTools.stringContains(input, ".*bus.*")){
					return new String[]{"bus_station"};
				}else if (NluTools.stringContains(input, ".*train.*")){
					return new String[]{"train_station"};
				}else if (NluTools.stringContains(input, ".*subway.*")){
					return new String[]{"subway_station", "train_station"};
				}else{
					return new String[]{"transit_station", "train_station"};
				}
			}else if (NluTools.stringContains(input, "airport")){
				return new String[]{"airport"};
			}else if (NluTools.stringContains(input, "sixt|europcar|starcar|car rental")){
				return new String[]{"car_rental"};
			}else if (NluTools.stringContains(input, "need a cab|taxi stand|taxi")){
				return new String[]{"taxi_stand"};
			}else if (NluTools.stringContains(input, "museum")){
				return new String[]{"museum"};
			}else if (NluTools.stringContains(input, "(gas|petrol)(-| |)station|get (gas|petrol)")){
				return new String[]{"gas_station"};
			}else if (NluTools.stringContains(input, "car(-| |)wash|wash\\b.* car")){
				return new String[]{"car_wash"};
			}else if (NluTools.stringContains(input, "(police)(-station|station| station|department|)(s|)")){
				return new String[]{"police"};
			}else if (NluTools.stringContains(input, "(fire)(-station|station| station|department|)(s|)")){
				return new String[]{"fire_station"};
			}else if (NluTools.stringContains(input, "hotel|hotels|motel|hostel(s|)")){
				return new String[]{"lodging"};
			}else if (NluTools.stringContains(input, "church(es|)|mosque(s|)|temple(s|)|synagogue(s|)|synagog")){
				return new String[]{"place_of_worship", "church", "hindu_temple", "mosque", "synagogue"};
			}else if (NluTools.stringContains(input, "aldi|lidl|penny|edeka|netto|rewe|supermarket(s|)|market|buy stuff")){
				return new String[]{"grocery_or_supermarket"};
			}else if (NluTools.stringContains(input, "cash machine|atm(s|)|get (money|cash)")){
				return new String[]{"atm", "bank", "finance"};
			}else if (NluTools.stringContains(input, "bank(s|)")){
				return new String[]{"bank", "finance"};
			}else if (NluTools.stringContains(input, "workshop(s|)")){
				return new String[]{"car_repair"};
			}else if (NluTools.stringContains(input, "(soccer|football|basketball|baseball|hockey|)( stadium|stadium)")){
				return new String[]{"stadium"};
			}else if (NluTools.stringContains(input, "uni|university")){
				return new String[]{"university"};
			}else if (NluTools.stringContains(input, "cinema|cinemaxx|uci|imax")){
				return new String[]{"movie_theater"};
			}else if (NluTools.stringContains(input, "shopping (center|mall)")){
				return new String[]{"shopping_mall", "store"};
			}else if (NluTools.stringContains(input, "\\w*shop|\\w*store|kiosk")){
				return new String[]{"store"};
			}else if (NluTools.stringContains(input, "brothel|cathouse")){
				return new String[]{"establishment"};
			}else if (NluTools.stringContains(input, "zoo")){
				return new String[]{"zoo"};
			}
		}
		else{
			Debugger.println("Place.java - getPoiType() has no support for language '" + language + "'", 1);
			return new String[]{};
		}
		return new String[]{};
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
	 * @param language - langauge code
	 */
	public static String buildCloseToSearch(String place, JSONObject locationJSON, String language){
		//build close to there search
		String searchPlace = GeoFactory.createPoiFinder().buildCloseToSearch(place, locationJSON, language);
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
	public String responseTweaker(String input) {
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
