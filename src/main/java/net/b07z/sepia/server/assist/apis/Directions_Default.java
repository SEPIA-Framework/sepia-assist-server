package net.b07z.sepia.server.assist.apis;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.TravelType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Directions search.
 * 
 * @author Florian Quirin
 *
 */
public class Directions_Default implements ApiInterface{
	
	//----data----
	private static final String SHOW_ALTERNATIVES = "show_alternatives"; 		//custom parameter
	
	//travel type mappings
	public static HashMap<String, String> travelTypeFlag = new HashMap<>();
	static {
		travelTypeFlag.put("transit", "r");
		travelTypeFlag.put("bicycling", "b");
		travelTypeFlag.put("driving", "d");
		travelTypeFlag.put("walking", "w");
		travelTypeFlag.put("none", "");
	}
	public static String getTravelTypeFlag(String in){
		if (in == null || in.isEmpty()){
			in = "none";
		}
		return travelTypeFlag.get(in);
	}
	private static final String defaultDurationTravelType = "driving"; 		//to make sure that a duration request is well defined when no type is submitted
	
	//get text for "start navigation" button
	public static HashMap<String, String> buttonTexts_de = new HashMap<>();
	public static HashMap<String, String> buttonTexts_en = new HashMap<>();
	static{
		buttonTexts_de.put("<start>", "Navigation starten");
		buttonTexts_de.put("<start_apple>", "Apple Maps");
		buttonTexts_de.put("<show_alternatives>", "Alternative Ziele");
		
		buttonTexts_en.put("<start>", "Start navigation");
		buttonTexts_en.put("<start_apple>", "Apple Maps");
		buttonTexts_en.put("<show_alternatives>", "Alternative destinations");
	}
	public static String getLocalButtonText(String type, String language){
		if (language.equals(LANGUAGES.DE)){
			return buttonTexts_de.get(type);
		}else{
			return buttonTexts_en.get(type);
		}
	}
	//-----------
	
	//local fallback words
	public static HashMap<String, String> fallbacks_de = new HashMap<>();
	public static HashMap<String, String> fallbacks_en = new HashMap<>();
	static {
		fallbacks_de.put("here", "hier");
		fallbacks_de.put("there", "dort");
		fallbacks_de.put("start", "Start");
		fallbacks_de.put("destination", "Ziel");
		fallbacks_de.put("waypoint", "Wegpunkt");
		
		fallbacks_en.put("here", "here");
		fallbacks_en.put("there", "there");
		fallbacks_en.put("start", "start");
		fallbacks_en.put("destination", "destination");
		fallbacks_en.put("waypoint", "waypoint");
	}
	public static String getLocalFallback(String input, String language){
		String fallback = "";
		if (language.equals(LANGUAGES.DE)){
			fallback = fallbacks_de.get(input);
		}else if (language.equals(LANGUAGES.EN)){
			fallback = fallbacks_en.get(input);
		}else{
			Debugger.println("Directions - getLocalFallback(..) - language not supported: " + language, 1);
		}
		if (fallback == null || fallback.isEmpty()){
			Debugger.println("Directions - getLocalFallback(..) - no fallback for: " + input + " - lang.: " + language, 1);
			return "";
		}else{
			return fallback;
		}
	}
	
	//defaults
	public static final String DEFAULT_TRAVEL_TYPE = "all";
	public static final String DEFAULT_TRAVEL_REQUEST_INFO = "overview";
	public static final String REQUEST_INFO_DURATION = "duration";
	public static final String REQUEST_INFO_DISTANCE = "distance";
	
	//info
	public ApiInfo getInfo(String language){
		//type
		ApiInfo info = new ApiInfo(Type.link, Content.redirect, false);
		
		//Parameters:
		//required
		Parameter p1 = new Parameter(PARAMETERS.LOCATION_END)
				.setRequired(true)
				.setQuestion("directions_ask_end_0a");
		info.addParameter(p1);
		//optional
		Parameter p2 = new Parameter(PARAMETERS.LOCATION_START, Parameter.Defaults.user_location);
		info.addParameter(p2);
		Parameter p3 = new Parameter(PARAMETERS.LOCATION_WAYPOINT, "");
		info.addParameter(p3);
		Parameter p4 = new Parameter(PARAMETERS.TRAVEL_TYPE, "");
		info.addParameter(p4);
		Parameter p5 = new Parameter(PARAMETERS.TRAVEL_REQUEST_INFO, "");
		info.addParameter(p5);
				
		//Answers:
		info.addSuccessAnswer("directions_1a")				//this one should work for any case
			.addCustomAnswer("withWP", wpAnswer) 			//optimized for waypoint
			.addCustomAnswer("withInfo", infoAnswer) 		//optimized for duration, distance requests
			.addCustomAnswer("withWpAndInfo", infoWpAnswer)	//optimized for duration, distance requests plus waypoint
			.addCustomAnswer("startQ", startQ) 				//if for some reason this did not work
			.addCustomAnswer("alternatives", alternatives)	//alternative destinations
			.addFailAnswer("directions_0a")					//for some reason the service failed
			.addAnswerParameters("start", "end", "waypoint", "travelType", "duration", "distance");
		
		return info;
	}
	private static final String wpAnswer = "directions_1b";
	private static final String infoAnswer = "directions_1c";
	private static final String infoWpAnswer = "directions_1d";
	private static final String startQ = "directions_ask_start_0a";
	private static final String alternatives = "directions_2a";

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result, getInfo(""));
		
		//get interview parameters:
		
		//required
		JSONObject endJSON = NLU_result.getRequiredParameter(PARAMETERS.LOCATION_END).getData();
		//required but defaults to user location as optional
		JSONObject startJSON = NLU_result.getOptionalParameter(PARAMETERS.LOCATION_START, "").getData();
		//JSON.printJSONpretty(startJSON); 		//DEBUG
		
		//optional
		Parameter waypointP = NLU_result.getOptionalParameter(PARAMETERS.LOCATION_WAYPOINT, "");
		Parameter typeP = NLU_result.getOptionalParameter(PARAMETERS.TRAVEL_TYPE, DEFAULT_TRAVEL_TYPE);
		Parameter infoP = NLU_result.getOptionalParameter(PARAMETERS.TRAVEL_REQUEST_INFO, DEFAULT_TRAVEL_REQUEST_INFO);
						
		//parameter adaptation to service format
		
		//start
		String start = startJSON.get(InterviewData.LOCATION_LAT) + "," + startJSON.get(InterviewData.LOCATION_LNG); //<- should NEVER be null
		String startCity = JSON.getStringOrDefault(startJSON, InterviewData.LOCATION_CITY,  "");					//<- can be empty
		String startStreet = JSON.getStringOrDefault(startJSON, InterviewData.LOCATION_STREET,  "");				//<- can be empty
		String startName = JSON.getStringOrDefault(startJSON, InterviewData.LOCATION_NAME,  getLocalFallback("start", api.language));
		String startToSay = "";
		if (startCity.isEmpty() && startStreet.isEmpty()){
			startToSay = startName;
		}else{
			startToSay = ((startStreet.isEmpty())? "" : (startStreet + ", ")) + startCity;
		}
		api.resultInfo_add("start", startToSay);
		
		//end
		String end = endJSON.get(InterviewData.LOCATION_LAT) + "," + endJSON.get(InterviewData.LOCATION_LNG);
		String endCity = JSON.getStringOrDefault(endJSON, InterviewData.LOCATION_CITY,  "");
		String endStreet = JSON.getStringOrDefault(endJSON, InterviewData.LOCATION_STREET,  "");
		String endName = JSON.getStringOrDefault(endJSON, InterviewData.LOCATION_NAME,  getLocalFallback("destination", api.language));
		String endToSay = "";
		if (endCity.isEmpty() && endStreet.isEmpty()){
			endToSay = endName;
		}else{
			endToSay = ((endStreet.isEmpty())? "" : (endStreet + ", ")) + endCity;
		}
		api.resultInfo_add("end", endToSay);
		//end options
		JSONArray optionsArray = (JSONArray) endJSON.get(InterviewData.OPTIONS);
		boolean hasOptions = (optionsArray != null);
		boolean showAlternatives = Boolean.parseBoolean(NLU_result.getParameter(SHOW_ALTERNATIVES));
		if (hasOptions){
			//add (reduced) first result to options
			endJSON.remove(InterviewData.OPTIONS);
			JSON.add(optionsArray, endJSON);
		}
		
		//waypoint
		String wp = "";
		String wpToSay = "";
		String wpCity = "";
		String wpStreet = "";
		String wpName = "";
		if (!waypointP.isDataEmpty()){ 
			JSONObject wpJSON = waypointP.getData();
			wp = wpJSON.get(InterviewData.LOCATION_LAT) + "," + wpJSON.get(InterviewData.LOCATION_LNG);
			wpCity = JSON.getStringOrDefault(wpJSON, InterviewData.LOCATION_CITY,  "");
			wpStreet = JSON.getStringOrDefault(wpJSON, InterviewData.LOCATION_STREET,  "");
			wpName = JSON.getStringOrDefault(wpJSON, InterviewData.LOCATION_NAME,  getLocalFallback("waypoint", api.language));
			if (wpCity.isEmpty() && wpStreet.isEmpty()){
				wpToSay = wpName;
			}else{
				wpToSay = ((wpStreet.isEmpty())? "" : (wpStreet + ", ")) + wpCity;
			}
		}
		api.resultInfo_add("waypoint", wpToSay);
		
		//Travel type
		String travelType = "";
		String travelTypeLocal = "";
		if (!typeP.isDataEmpty()){
			travelType = (String) typeP.getData().get(InterviewData.VALUE);
			travelTypeLocal = (String) typeP.getData().get(InterviewData.VALUE_LOCAL);
		}else{
			travelType = (String) typeP.getDefaultValue();
			travelTypeLocal = TravelType.getLocal("<" + travelType + ">", api.language);
		}
		api.resultInfo_add("travelType", travelTypeLocal); 		//<- might get overwritten if request is a duraton request with undefined travelMode to avoid random travelMode 
		
		//Travel request info
		String travelRequestInfo = "";
		if (!infoP.isDataEmpty()){
			travelRequestInfo = (String) infoP.getData().get(InterviewData.VALUE);
		}
		
		Debugger.println("cmd: directions, start: " + start + ", end: " + end + ", wp: " + wp 
				+ ", travelType: " + travelType + ", travelRequestInfo: " + travelRequestInfo, 2);		//debug
		
		//GET DATA
		long tic = System.currentTimeMillis();
		String googleMapsURL = "";
		String appleMapsURL = "";
		//traffic?
		String googleViews="";
		String googleTravelType = travelType;
		if (!googleTravelType.matches("(transit|bicycling|driving|walking)"))	googleTravelType = "";	//use only supported for google
		else if (googleTravelType.matches("(driving|)")) googleViews = "&views=traffic";				//add traffic to car and none
		
		//ALTERNATIVES?
		if (hasOptions && showAlternatives){
			//build alternatives list
			//String data = buildEndAlternativesHTML(optionsArray, api.language, start, wp, googleTravelType, googleViews);
			//add action
			/*
			api.actionInfo_add_action(ACTIONS.SHOW_HTML_RESULT);
			api.actionInfo_put_info("data", data);
			api.hasAction = true;
			*/
			
			ArrayList<Card> alternativeLinkCards = buildEndAlternativeCards(optionsArray, api.language, start, wp, googleTravelType, googleViews);
			
			//build cards
			for (Card c : alternativeLinkCards){
				api.addCard(c.getJSON());
			}
			
			//set missing stuff
			api.resultInfo_add("duration", "");
			api.resultInfo_add("distance", "");
			
			//set answer
			api.setCustomAnswer(alternatives);
			
			//all clear?
			api.status = "success";
			
			//build the API_Result
			ApiResult result = api.build_API_result();
			return result;
		}
		
		//MAP LINKS
		try {
			//GOOGLE
			googleMapsURL = makeGoogleMapsURL(start, end, wp, googleTravelType, googleViews);
			
			//APPLE
			if (CLIENTS.isAppleCompatible(NLU_result.input.client_info)){
				appleMapsURL = makeAppleMapsURL(start, end, wp, googleTravelType);
			}
			
		} catch (UnsupportedEncodingException e) {
			Debugger.println("Directions - could not URLEncode locations: " + start + ", " + end + ", " + wp , 1);
			//e.printStackTrace();
			
			//build error result
			ApiResult result = api.build_API_result();
					
			//return result_JSON.toJSONString();
			return result;
		}
		Statistics.addExternalApiHit("Directions GoogleMaps");
		Statistics.addExternalApiTime("Directions GoogleMaps", tic);
		
		//DO API CALL?
		String distance = "";
		String duration = "";
		if (travelRequestInfo.equals(REQUEST_INFO_DISTANCE) || travelRequestInfo.equals(REQUEST_INFO_DURATION)){
			tic = System.currentTimeMillis();
			String googleDirectionsURL;
			try {
				googleTravelType = travelType;
				if (!googleTravelType.matches("(transit|bicycling|driving|walking)")){
					googleTravelType = defaultDurationTravelType;	//use only supported for google and fix it for no input here
					//overwrite the travelMode response
					api.resultInfo_add("travelType", TravelType.getLocal("<" + googleTravelType + ">", api.language));
				}
				
				if (wp.isEmpty()){
					googleDirectionsURL = "https://maps.googleapis.com/maps/api/directions/json" +
							"?origin=" + URLEncoder.encode(start, "UTF-8") +
							"&destination=" + URLEncoder.encode(end, "UTF-8") +
							//"&waypoints=" + URLEncoder.encode(wp, "UTF-8") +
							"&departure_time=" + System.currentTimeMillis()/1000 +
							"&mode=" + googleTravelType +
							"&region=" + api.language +
							"&language=" + api.language;
				}else{
					googleDirectionsURL = "https://maps.googleapis.com/maps/api/directions/json"+
							"?origin=" + URLEncoder.encode(start, "UTF-8") +
							"&destination=" + URLEncoder.encode(end, "UTF-8") +
							"&waypoints=via:" + URLEncoder.encode(wp, "UTF-8") +
							"&departure_time=" + System.currentTimeMillis()/1000 +
							"&mode=" + googleTravelType +
							"&region=" + api.language +
							"&language=" + api.language;
				}
				//System.out.println("HTTP GET URL: " + googleDirectionsURL); 						//debug
				JSONObject response = Connectors.httpGET(googleDirectionsURL.trim());
				Statistics.addExternalApiHit("Directions GoogleMaps with API");
				Statistics.addExternalApiTime("Directions GoogleMaps with API", tic);
				//System.out.println("HTTP GET Response: " + response); 						//debug
				
				if (Connectors.httpSuccess(response)){
					try{
						JSONArray routes = (JSONArray) response.get("routes");
						JSONArray legs = (JSONArray) ((JSONObject) routes.get(0)).get("legs");
						JSONObject durationJSON = (JSONObject) ((JSONObject) legs.get(0)).get("duration");
						JSONObject distanceJSON = (JSONObject) ((JSONObject) legs.get(0)).get("distance");
						duration = JSON.getString(durationJSON, "text");
						distance = JSON.getString(distanceJSON, "text");
					}catch (Exception e){
						Debugger.println("Directions - could not get API answer for: " + start + ", " + end + ", " + wp + " - Error: " + e.getMessage(), 1);
						duration = "";
						distance = "";
					}
				}
				
			//API Error
			} catch (Exception e) {
				Debugger.println("Directions - could not get API answer for: " + start + ", " + end + ", " + wp + " - Error: " + e.getMessage(), 1);
			}
		}
		api.resultInfo_add("duration", duration);
		api.resultInfo_add("distance", distance);
		
		//what answer to use?
		boolean isInfoAnswer = false;
		if (!wpToSay.isEmpty() && !distance.isEmpty()){
			api.setCustomAnswer(infoWpAnswer);
			isInfoAnswer = true;
		}else if (!distance.isEmpty()){
			api.setCustomAnswer(infoAnswer);
			isInfoAnswer = true;
		}else if (!wpToSay.isEmpty()){
			api.setCustomAnswer(wpAnswer);
		}
		
		//make action: browser url call
		/*
		if (!isInfoAnswer){
			api.actionInfo_add_action(ACTIONS.OPEN_URL);
			api.actionInfo_put_info("url", googleMapsURL); 		//<- first one is Google maps
		}
		*/
		/*
		//google button
		api.actionInfo_add_action(ACTIONS.BUTTON_URL);
		api.actionInfo_put_info("url", googleMapsURL);
		api.actionInfo_put_info("title", getLocalButtonText("<start>", api.language));
		//apple button
		if (!appleMapsURL.isEmpty()){
			api.actionInfo_add_action(ACTIONS.BUTTON_URL);
			api.actionInfo_put_info("url", appleMapsURL);
			api.actionInfo_put_info("title", getLocalButtonText("<start_apple>", api.language));
		}
		*/
		//options button?
		if (hasOptions){
			api.overwriteParameter(SHOW_ALTERNATIVES, "true");
			api.actionInfo_add_action(ACTIONS.BUTTON_CMD);
			api.actionInfo_put_info("title", getLocalButtonText("<show_alternatives>", api.language));
			api.actionInfo_put_info("info", "direct_cmd");
			api.actionInfo_put_info("cmd", api.cmd_summary);
			api.actionInfo_put_info("visibility", "inputHidden");
		}
		
		//build cards
		
		String description = "";
		if (isInfoAnswer){
			description = startToSay + " - " + endToSay + ", " + distance + ", " + duration;
		}else{
			description = startToSay + " - " + endToSay;
		}
		
		//google
		Card googleCard = new Card(Card.TYPE_SINGLE);
		JSONObject linkCard1 = googleCard.addElement(ElementType.link, 
				JSON.make("title", "Google Maps", "desc", description),
				null, null, "", 
				googleMapsURL, 
				Config.urlWebImages + "/brands/google-maps.png", 
				null, null);
		JSON.put(linkCard1, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(googleCard.getJSON());
		
		//apple
		if (!appleMapsURL.isEmpty()){
			Card appleCard = new Card(Card.TYPE_SINGLE);
			JSONObject linkCard2 = appleCard.addElement(ElementType.link, 
					JSON.make("title", "Apple Maps", "desc", description),
					null, null, "", 
					appleMapsURL, 
					Config.urlWebImages + "/brands/apple-maps.png", 
					null, null);
			JSON.put(linkCard2, "imageBackground", "transparent");	//use any CSS background option you wish
			api.addCard(appleCard.getJSON());
		}
		
		//all clear?
		api.status = "success";
		
		//finally build the API_Result
		ApiResult result = api.build_API_result();
				
		//JSON.printJSONpretty(result.result_JSON); 		//debug
		//return result_JSON.toJSONString();
		return result;
	}
	
	//------------- helpers --------------
	
	public static String makeGoogleMapsURL(String start, String end, String wp, String googleTravelType, String googleViews) throws UnsupportedEncodingException{
		String googleMapsURL = "";
		if (wp.isEmpty()){
			googleMapsURL = "https://maps.google.com/?"+
						"saddr=" 	+ URLEncoder.encode(start, "UTF-8") 	+
						"&daddr=" 	+ URLEncoder.encode(end, "UTF-8") 		+
						"&directionsmode=" + googleTravelType		+
						"&dirflg=" 	+ getTravelTypeFlag(googleTravelType) + googleViews +
						//"&mode=" 	+ type		+	"&center=" 	+ "51.4,6.8" 	+
						"&zoom=10";
		}else{
			googleMapsURL = "https://maps.google.com/?"+
						"saddr=" 	+ URLEncoder.encode(start, "UTF-8") 	+
						"&daddr=" 	+ URLEncoder.encode(wp, "UTF-8") 		+
						"+to:"		+ URLEncoder.encode(end, "UTF-8") 		+
						"&directionsmode=" + googleTravelType		+
						"&dirflg=" 	+ getTravelTypeFlag(googleTravelType) + googleViews +
						//	"&mode=" 	+ type		+	"&center=" 	+ "51.4,6.8" 	+
						"&zoom=10";
		}
		return googleMapsURL;
	}
	
	public static String makeAppleMapsURL(String start, String end, String wp, String googleTravelType) throws UnsupportedEncodingException{
		String appleMapsURL = "";
		appleMapsURL = "http://maps.apple.com/?"+
				"saddr=" 	+ URLEncoder.encode(start, "UTF-8") 	+
				"&daddr=" 	+ URLEncoder.encode(end, "UTF-8") 		+
				"&dirflg=" 	+ getTravelTypeFlag(googleTravelType);
		return appleMapsURL;
	}
	
	public static String buildEndAlternativesHTML(JSONArray options, String language, String start, String wp, String googleTravelType, String googleViews){
		String htmlData = "";
		for (Object o : options){
			JSONObject locJSON = (JSONObject) o;
			String loc = locJSON.get(InterviewData.LOCATION_LAT) + "," + locJSON.get(InterviewData.LOCATION_LNG);
			String locCity = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_CITY, "");
			String locStreet = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_STREET, "");
			String locAddr = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_ADDRESS_TEXT, "");
			String locName = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_NAME, "");
			String locImage = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_IMAGE, Config.urlWebImages + "cards/location_img_default.png");
			String mapUrl = "";
			try{
				mapUrl = makeGoogleMapsURL(start, loc, wp, googleTravelType, googleViews);
			}catch(Exception e){
				mapUrl = "";
			}
			String data = "<div class='card-box'><div class='inner-container'>";
				data += "<div class='img-container'>";
					data += "<img src='" + locImage + "'>";
				data += "</div>";
				data += "<div class='text-container'>";
					data += locName.isEmpty()? "" : (locName + "<br>");
					if (locCity.isEmpty() && locStreet.isEmpty())	data += (locAddr.isEmpty()? "" : locAddr);
					else data += ((locStreet.isEmpty())? "" : (locStreet + ", ")) + locCity;
				data += "</div>";
				data += "<a class='button-overlay' href='" + mapUrl + "' target='_blank'></a>";
			data += "</div></div>";
			htmlData += data;
		}
		return htmlData;
	}
	/**
	 * Build cards array from location options.
	 * @param options
	 * @param language
	 * @param start
	 * @param wp
	 * @param googleTravelType
	 * @param googleViews
	 * @return
	 */
	public static ArrayList<Card> buildEndAlternativeCards(JSONArray options, String language, String start, String wp, String googleTravelType, String googleViews){
		ArrayList<Card> cardElements = new ArrayList<>();
		for (Object o : options){
			JSONObject locJSON = (JSONObject) o;
			String loc = locJSON.get(InterviewData.LOCATION_LAT) + "," + locJSON.get(InterviewData.LOCATION_LNG);
			String locCity = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_CITY, "");
			String locStreet = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_STREET, "");
			String locAddr = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_ADDRESS_TEXT, "");
			String locName = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_NAME, "");
			String locImage = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_IMAGE, Config.urlWebImages + "cards/location_img_default.png");
			String mapUrl = "";
			try{
				mapUrl = makeGoogleMapsURL(start, loc, wp, googleTravelType, googleViews);
			}catch(Exception e){
				mapUrl = "";
			}
			
			String desc = "";
			if (locCity.isEmpty() && locStreet.isEmpty())	desc += (locAddr.isEmpty()? "" : locAddr);
			else desc += ((locStreet.isEmpty())? "" : (locStreet + ", ")) + locCity;
			
			Card card = new Card(Card.TYPE_SINGLE);
			JSONObject linkCard = card.addElement(ElementType.link, 
					JSON.make("title", "", "desc", (locName.isEmpty()? "" : ("<b>" + locName + "</b><br>")) + desc.trim()),		//we dont use title because names are so loooong most of the time :(
					null, null, "", 
					mapUrl, 
					locImage, 
					null, null);
			JSON.put(linkCard, "imageBackground", "transparent");
			
			cardElements.add(card);
		}
		return cardElements;
	}

}
