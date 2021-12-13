package net.b07z.sepia.server.assist.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import net.b07z.sepia.server.assist.assistant.ActionBuilder;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.geo.DirectionsApiInterface;
import net.b07z.sepia.server.assist.geo.DirectionsApiResult;
import net.b07z.sepia.server.assist.geo.GeoFactory;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.TravelType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Directions search.
 * 
 * @author Florian Quirin
 *
 */
public class DirectionsGoogleMaps implements ServiceInterface{
	
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
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.link, Content.redirect, false);
		
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
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get interview parameters:
		
		//required
		JSONObject endJSON = nluResult.getRequiredParameter(PARAMETERS.LOCATION_END).getData();
		//required but defaults to user location as optional
		JSONObject startJSON = nluResult.getOptionalParameter(PARAMETERS.LOCATION_START, "").getData();
		//JSON.printJSONpretty(startJSON); 		//DEBUG
		
		//optional
		Parameter waypointP = nluResult.getOptionalParameter(PARAMETERS.LOCATION_WAYPOINT, "");
		Parameter typeP = nluResult.getOptionalParameter(PARAMETERS.TRAVEL_TYPE, DEFAULT_TRAVEL_TYPE);
		Parameter infoP = nluResult.getOptionalParameter(PARAMETERS.TRAVEL_REQUEST_INFO, DEFAULT_TRAVEL_REQUEST_INFO);
						
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
		api.resultInfoPut("start", startToSay);
		
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
		api.resultInfoPut("end", endToSay);
		//end options
		JSONArray optionsArray = (JSONArray) endJSON.get(InterviewData.OPTIONS);
		boolean hasOptions = (optionsArray != null);
		boolean showAlternatives = Boolean.parseBoolean(nluResult.getParameter(SHOW_ALTERNATIVES));
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
		api.resultInfoPut("waypoint", wpToSay);
		
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
		api.resultInfoPut("travelType", travelTypeLocal); 		//<- might get overwritten if request is a duraton request with undefined travelMode to avoid random travelMode 
		
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
			ArrayList<Card> alternativeLinkCards = buildEndAlternativeCards(optionsArray, api.language, start, wp, googleTravelType, googleViews);
			
			//build cards
			for (Card c : alternativeLinkCards){
				api.addCard(c.getJSON());
			}
			
			//set missing stuff
			api.resultInfoPut("duration", "");
			api.resultInfoPut("distance", "");
			
			//set answer
			api.setCustomAnswer(alternatives);
			
			//all clear?
			api.status = "success";
			
			//build the API_Result
			ServiceResult result = api.buildResult();
			return result;
		}
		
		//MAP LINKS
		try {
			//GOOGLE
			googleMapsURL = makeGoogleMapsURL(start, end, wp, googleTravelType, googleViews);
			
			//APPLE
			if (CLIENTS.isAppleCompatible(nluResult.input.clientInfo)){
				appleMapsURL = makeAppleMapsURL(start, end, wp, googleTravelType);
			}
			
		} catch (UnsupportedEncodingException e) {
			Debugger.println("Directions - could not URLEncode locations: " + start + ", " + end + ", " + wp , 1);
			//e.printStackTrace();
			
			//build error result
			ServiceResult result = api.buildResult();
					
			//return result_JSON.toJSONString();
			return result;
		}
		Statistics.addExternalApiHit("Directions URL");
		Statistics.addExternalApiTime("Directions URL", tic);
		
		//DO API CALL?
		String distance = "";
		String duration = "";
		boolean triedApiCall = false;
		boolean missingApiSupport = false;
		if (travelRequestInfo.equals(REQUEST_INFO_DISTANCE) || travelRequestInfo.equals(REQUEST_INFO_DURATION)){
			//get API interface
			triedApiCall = true;
			DirectionsApiInterface directionsApi = GeoFactory.createDirectionsApi();
			if (directionsApi == null || !directionsApi.isSupported()){
				//API missing support
				missingApiSupport = true;
			}else{
				String[] wpLatLong = new String[]{"", ""};
				if (Is.notNullOrEmpty(wp)){
					wpLatLong = wp.split(",");
				}
				DirectionsApiResult directionsRes = directionsApi.getDurationAndDistance(
					JSON.getString(startJSON, InterviewData.LOCATION_LAT), JSON.getString(startJSON, InterviewData.LOCATION_LNG),
					JSON.getString(endJSON, InterviewData.LOCATION_LAT), JSON.getString(endJSON, InterviewData.LOCATION_LNG), 
					wpLatLong[0], wpLatLong[1],	googleTravelType, api.language
				);
				if (directionsRes == null){
					Debugger.println("Directions - could not get API answer for: " + start + ", " + end + ", " + wp, 1);
				}else{
					//update travel type (in case it changed)
					googleTravelType = directionsRes.travelMode;
					api.resultInfoPut("travelType", TravelType.getLocal("<" + googleTravelType + ">", api.language));
					//get info
					if (directionsRes.durationText != null){
						duration = directionsRes.durationText;
					}
					if (directionsRes.distanceText != null){
						distance = directionsRes.distanceText;
					}
				}
			}
		}
		api.resultInfoPut("duration", duration);
		api.resultInfoPut("distance", distance);
		
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
		
		//make action: browser URL call
		if (!isInfoAnswer && !hasOptions){
			api.addAction(ACTIONS.OPEN_IN_APP_BROWSER);
			api.putActionInfo("url", (appleMapsURL.isEmpty())? googleMapsURL : appleMapsURL);
		}
		//add API support info button?
		if (triedApiCall && missingApiSupport){
			ActionBuilder.addApiKeyInfoButton(api);
		}
		
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
			api.addAction(ACTIONS.BUTTON_CMD);
			api.putActionInfo("title", getLocalButtonText("<show_alternatives>", api.language));
			api.putActionInfo("info", "direct_cmd");
			api.putActionInfo("cmd", api.cmdSummary);
			api.putActionInfo("visibility", "inputHidden");
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
		//JSONObject linkCard1 = 
		googleCard.addElement(ElementType.link, 
				JSON.make(
					"title", "Google Maps", 
					"desc", description,
					"type", "maps"
				),
				null, null, "", 
				googleMapsURL, 
				Config.urlWebImages + "/brands/google-maps.png", 
				null, null);
		//JSON.put(linkCard1, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(googleCard.getJSON());
		
		//apple
		if (!appleMapsURL.isEmpty()){
			Card appleCard = new Card(Card.TYPE_SINGLE);
			//JSONObject linkCard2 = 
			appleCard.addElement(ElementType.link, 
					JSON.make(
						"title", "Apple Maps", 
						"desc", description,
						"type", "maps"
					),
					null, null, "", 
					appleMapsURL, 
					Config.urlWebImages + "/brands/apple-maps.png", 
					null, null);
			//JSON.put(linkCard2, "imageBackground", "transparent");	//use any CSS background option you wish
			api.addCard(appleCard.getJSON());
		}
		
		//all clear?
		api.status = "success";
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
				
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
			String locImage = JSON.getStringOrDefault(locJSON, InterviewData.LOCATION_IMAGE, "");
			boolean isDefaultImage = locImage.isEmpty();
			if (isDefaultImage){
				locImage = Config.urlWebImages + "cards/location_img_default.png";
			}
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
			//JSONObject linkCard = 
			card.addElement(ElementType.link, 
					JSON.make(
						"title", "",	//we dont use title because names are so loooong most of the time :( 
						"desc", (locName.isEmpty()? "" : ("<b>" + locName + "</b><br>")) + desc.trim(),
						"type", ((isDefaultImage)? "locationDefault" : "locationCustom")
					),
					null, null, "", 
					mapUrl, 
					locImage, 
					null, null);
			//JSON.put(linkCard, "imageBackground", "transparent");
			
			cardElements.add(card);
		}
		return cardElements;
	}

}
