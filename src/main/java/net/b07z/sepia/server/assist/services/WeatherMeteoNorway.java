package net.b07z.sepia.server.assist.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Number;
import net.b07z.sepia.server.assist.parameters.DateAndTime.DateType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.ENVIRONMENTS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Connectors.HttpClientResult;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

/**
 * Weather service using Meteorologisk Institutt Norway data.
 * 
 * @author Florian Quirin
 */
public class WeatherMeteoNorway implements ServiceInterface {
	
	//Some API data
	private final static String API_HEADER_INFO = "Self-hosted open-source SEPIA assistant sepia-framework.github.io";
	
	//Some references
	private final static double rainReferenceMmPerHour = 1.0d;		//24mm in 24h = heavy rain
	private final static double snowReferenceMmPerHour = 1.0d;		//TODO: is this ok?
	private final static double sleetReferenceMmPerHour = 1.0d;		//TODO: is this ok?
	
	//Data
	public static final List<String> supportedLanguages = Arrays.asList(
			LANGUAGES.DE, 
			LANGUAGES.EN
	);
	private static final String DESC = "desc";		
	private static final String HEAVY = "heavy";
	private static final String LIGHT = "light";
	private static final String TYPE = "type";
	private static final String NOW = "now";
	private static final String FUTURE = "future";
	private static final String ICON = "sepiaIcon";
	public static Map<Integer, JSONObject> descBaseByType = new HashMap<>();
	static {
		//Type1
		descBaseByType.put(1, JSON.make( 
				LANGUAGES.DE, JSON.make(NOW, "Es ist {desc}", FUTURE, "Es wird {desc}"),
				LANGUAGES.EN, JSON.make(NOW, "It is {desc}", FUTURE, "It will be {desc}")
		));
		//Type2
		descBaseByType.put(2, JSON.make( 
				LANGUAGES.DE, JSON.make(NOW, "Es gibt {desc}", FUTURE, "Es wird {desc} geben"),
				LANGUAGES.EN, JSON.make(NOW, "There is {desc}", FUTURE, "There will be {desc}")
		));
		//Type3
		descBaseByType.put(3, JSON.make( 
				LANGUAGES.DE, JSON.make(NOW, "Es gibt {desc}", FUTURE, "Es wird {desc} geben"),
				LANGUAGES.EN, JSON.make(NOW, "There are {desc}", FUTURE, "There will be {desc}")
		));
	}
	public static Map<String, JSONObject> partOfDay = new HashMap<>();
	static {
		partOfDay.put("morning", JSON.make(
				LANGUAGES.DE, "Vormittag",
				LANGUAGES.EN, "morning"
		));
		partOfDay.put("afternoon", JSON.make(
				LANGUAGES.DE, "Nachmittag",
				LANGUAGES.EN, "afternoon"
		));
	}
	public static Map<String, JSONObject> iconData = new HashMap<>();
	static {
		iconData.put("clearsky", JSON.make(ICON, "clear-{time}", 
				LANGUAGES.DE, JSON.make(TYPE, 1, DESC, "klar, mit blauem Himmel"),
				LANGUAGES.EN, JSON.make(TYPE, 1, DESC, "clear with blue sky")
		));
		iconData.put("cloudy", JSON.make(ICON, "cloudy", 
				LANGUAGES.DE, JSON.make(TYPE, 1, DESC, "bewölkt", HEAVY, "stark", LIGHT, "leicht"),
				LANGUAGES.EN, JSON.make(TYPE, 1, DESC, "cloudy", HEAVY, "very", LIGHT, "slightly")
		));
		iconData.put("partlycloudy", JSON.make(ICON, "partly-cloudy-{time}", 
				LANGUAGES.DE, JSON.make(TYPE, 1, DESC, "teilweise bewölkt"),
				LANGUAGES.EN, JSON.make(TYPE, 1, DESC, "partly cloudy")
		));
		iconData.put("rain", JSON.make(ICON, "rain", 
				LANGUAGES.DE, JSON.make(TYPE, 1, DESC, "regnerisch", HEAVY, "sehr", LIGHT, "etwas"),
				LANGUAGES.EN, JSON.make(TYPE, 1, DESC, "rainy", HEAVY, "very", LIGHT, "a bit")
		));
		iconData.put("rainshowers", JSON.make(ICON, "rain", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Regenschauer", HEAVY, "heftige", LIGHT, "leichte"),
				LANGUAGES.EN, JSON.make(TYPE, 3, DESC, "rain showers", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("snow", JSON.make(ICON, "snow", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Schnee", HEAVY, "viel", LIGHT, "ein wenig"),
				LANGUAGES.EN, JSON.make(TYPE, 2, DESC, "snow", "a lot of", HEAVY, LIGHT, "a bit of")
		));
		iconData.put("snowshowers", JSON.make(ICON, "snow", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Schneeschauer", HEAVY, "heftige", LIGHT, "leichte"),
				LANGUAGES.EN, JSON.make(TYPE, 3, DESC, "snow showers", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("sleet", JSON.make(ICON, "sleet", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Graupel", HEAVY, "viel", LIGHT, "ein wenig"),
				LANGUAGES.EN, JSON.make(TYPE, 2, DESC, "sleet", HEAVY, "a lot of", LIGHT, "a bit of")
		));
		iconData.put("sleetshowers", JSON.make(ICON, "sleet", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Graupelschauer", HEAVY, "heftige", LIGHT, "leichte"),
				LANGUAGES.EN, JSON.make(TYPE, 3, DESC, "showers of sleet", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("fog", JSON.make(ICON, "fog", 
				LANGUAGES.DE, JSON.make(TYPE, 1, DESC, "neblig", HEAVY, "stark", LIGHT, "leicht"),
				LANGUAGES.EN, JSON.make(TYPE, 2, DESC, "fog", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("fair", JSON.make(ICON, "fair-{time}", 
				LANGUAGES.DE, JSON.make(TYPE, 1, DESC, "heiter"),
				LANGUAGES.EN, JSON.make(TYPE, 1, DESC, "fair")
		));
		iconData.put("wind", JSON.make(ICON, "wind", 
				LANGUAGES.DE, JSON.make(TYPE, 1, DESC, "windig", HEAVY, "sehr", LIGHT, "etwas"),
				LANGUAGES.EN, JSON.make(TYPE, 2, DESC, "wind", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("snowandthunder", JSON.make(ICON, "thunder-and-snow", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Unwetter mit Donner, Blitzen und Schnee", HEAVY, "heftiges", LIGHT, "leichtes"),
				LANGUAGES.EN, JSON.make(TYPE, 3, DESC, "storms with thunder, lightning and snow", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("sleetandthunder", JSON.make(ICON, "thunder-and-sleet", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Unwetter mit Donner, Blitzen und Graupel", HEAVY, "heftiges", LIGHT, "leichtes"),
				LANGUAGES.EN, JSON.make(TYPE, 3, DESC, "storms with thunder, lightning and sleet", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("rainandthunder", JSON.make(ICON, "thunder-and-rain", 
				LANGUAGES.DE, JSON.make(TYPE, 2, DESC, "Unwetter mit Donner, Blitzen und Regen", HEAVY, "heftiges", LIGHT, "leichtes"),
				LANGUAGES.EN, JSON.make(TYPE, 3, DESC, "storms with thunder, lightning and rain", HEAVY, "heavy", LIGHT, "light")
		));
		iconData.put("default", JSON.make(ICON, "default", 
				LANGUAGES.DE, JSON.make(TYPE, 3, DESC, "unknown conditions"),
				LANGUAGES.EN, JSON.make(TYPE, 3, DESC, "unbekannte Bedingungen")
		));
	}
	public static JSONObject getIconWithDescription(String weatherInfo, String language, String predictionTime){
		//supported?
		if (!supportedLanguages.contains(language)){
			language = LANGUAGES.EN;		//fallback EN
		}
		//get time, default to day - Note: 'polartwilight$' for now because we don't have icons for it
		String dayNightTwilight = NluTools.stringFindFirstPart(weatherInfo, "(day$|night$)");	
		if (Is.nullOrEmpty(dayNightTwilight)){
			dayNightTwilight = "day";
		}
		weatherInfo = weatherInfo.replaceFirst("_\\w+$", "").trim();		//we assume this must be (day|night|polartwilight)
		//heavy, light or normal?
		String heavyLight = NluTools.stringFindFirstPart(weatherInfo, "(heavy|light)");
		if (Is.notNullOrEmpty(heavyLight)){
			weatherInfo = weatherInfo.replace(heavyLight, "").trim();
		}
		//thunder?
		if (weatherInfo.contains("thunder")){
			weatherInfo = weatherInfo.replace("showers", "").trim();
		}
		//get data
		//System.out.println("weatherInfo: " + weatherInfo);		//DEBUG
		JSONObject data = iconData.get(weatherInfo);
		if (data == null){
			data = iconData.get("default");
		}
		String icon = JSON.getString(data, ICON);
		if (Is.notNullOrEmpty(dayNightTwilight)){
			//replace day/night tag
			icon = icon.replace("{time}", dayNightTwilight);
		}
		//description with modifiers
		JSONObject descriptionObj = JSON.getJObject(data, language);
		String description = JSON.getStringOrDefault(descriptionObj, DESC, "unknown conditions");
		if (Is.notNullOrEmpty(heavyLight)){
			description = (JSON.getStringOrDefault(descriptionObj, heavyLight, "") + " " + description).trim();
		}
		int baseType = JSON.getIntegerOrDefault(descriptionObj, TYPE, 3);
		String descBase = (String) JSON.getJObject(descBaseByType.get(baseType), language).get(predictionTime);
		String descLong = descBase.replace("{desc}", description);
		return JSON.make(
			ICON, icon,
			DESC, descLong,
			"descShort", description,
			TYPE, baseType 
		);
	}

	@Override
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.REST, Content.data, false);
		
		//Parameters:
		//optional
		Parameter p1 = new Parameter(PARAMETERS.PLACE, Parameter.Defaults.user_location);
		info.addParameter(p1);
		Parameter p2 = new Parameter(PARAMETERS.TIME, Parameter.Defaults.user_time);
		info.addParameter(p2);
		
		//Answers:
		info.addSuccessAnswer(answerToday)
			.addFailAnswer("weather_0a")							//for some reason the service failed
			.addCustomAnswer("tomorrowAnswer", answerTomorrow)		//specific for "tomorrow"
			.addCustomAnswer("dayOfWeekAnswer", answerDayOfWeek)	//specific for a certain day of the week
			.addCustomAnswer("weekAnswer", answerThisWeek)			//specific for anything even further than tomorrow
			.addCustomAnswer("missingDisplayForWeek", missingDisplayForWeek)	//a display to show results is missing 
			.addCustomAnswer("missingGeoData", missingGeoData)		//geo data like GPS coordinates are missing
			;
		info.addAnswerParameters("place", "temp", "description", "day", "context1", "context2");
		
		return info;
	}
	private static final String answerToday = "weather_today_1b";
	private static final String answerTomorrow = "weather_tomorrow_1b";
	private static final String answerDayOfWeek = "weather_day_1b";
	private static final String answerThisWeek = "weather_week_1b";
	private static final String missingDisplayForWeek = "weather_week_0a";
	private static final String missingGeoData = "error_geo_location_0b";
	
	@Override
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get interview parameters
		JSONObject placeJSON = nluResult.getRequiredParameter(PARAMETERS.PLACE).getData(); 	//note: we use getRequiredParameter here 
		JSONObject timeJSON = nluResult.getRequiredParameter(PARAMETERS.TIME).getData();	//... because we set default values in getInfo
				
		//parameter adaptation to service format
		String place = (String) placeJSON.get(InterviewData.LOCATION_CITY);
		if (place == null){
			place = (String) placeJSON.get(InterviewData.LOCATION_STATE);
		}
		if (place == null){
			place = WeatherDarkSky.getPlaceName("<coordinates>", service.language);
		}
		service.resultInfoPut("place", place);
		
		//adapt answer and search using given date
		String dateType = (String) timeJSON.get(InterviewData.TIME_TYPE); 
		long days = 0;
		boolean tooFarOrUnspecific = false;
		if (dateType.equals(DateType.unspecific.name())){
			//String unspecificDate = (String) timeJSON.get(InterviewData.TIME_UNSPECIFIC);
			days = 7; 		//NOTE: this is kind of arbitrary
			tooFarOrUnspecific = true;
			
		}else{
			JSONObject timeDiff = (JSONObject) timeJSON.get(InterviewData.TIME_DIFF);
			days = Converters.obj2LongOrDefault((timeDiff).get("dd"), null);
			long hours = Converters.obj2LongOrDefault((timeDiff).get("hh"), null);
			long minutes = Converters.obj2LongOrDefault((timeDiff).get("mm"), null);
			long seconds = Converters.obj2LongOrDefault((timeDiff).get("ss"), null);
			if (hours > 0 || minutes > 0 || seconds > 0){
				long correctedDiffDays = DateTimeConverters.getIntuitiveDaysDifference(nluResult.input, days, hours, minutes, seconds);
				days = correctedDiffDays;
			}
			
			//more than 7 is not possible
			if (days > 7){
				days = 7;
				tooFarOrUnspecific = true;
			}
		}
		boolean isToday = false;
		boolean isTomorrow = false;
		boolean isDayOfWeek = false;
		boolean isWholeWeek = false;
		if (tooFarOrUnspecific){
			isWholeWeek = true;
		}else if (days > 1){
			isDayOfWeek = true;
		}else if (days == 1){
			isTomorrow = true;
		}else{
			isToday = true;
		}
		
		Debugger.println("cmd: weather, place: " + place + ", days in future: " + days, 2);		//debug
		
		//Default user temperature unit - C or F
		String userPrefTempUnit = (String) nluResult.input.getCustomDataObject("prefTempUnit");
		if (userPrefTempUnit == null) userPrefTempUnit = "C";
		
		//GET DATA
		
		String lat = JSON.getString(placeJSON, InterviewData.LOCATION_LAT);
		String lng = JSON.getString(placeJSON, InterviewData.LOCATION_LNG);
		
		//valid coordinates?
		if (Is.nullOrEmpty(lat) || Is.nullOrEmpty(lng)){
			Statistics.addExternalApiHit("Weather MeteoNorway error");
			Statistics.addExternalApiTime("Weather MeteoNorway error", 0l);
						
			//build the API_Result and goodbye
			service.setStatusOkay();
			service.setCustomAnswer(missingGeoData);
			ServiceResult result = service.buildResult(); 
			return result;
		}
		
		String baseUrl = "https://api.met.no/weatherapi/locationforecast/2.0/.json";
		String url = URLBuilder.getString(baseUrl, "?lat=", lat, "&lon=", lng);
		//System.out.println("weather url: " + url);		//debug
		
		long tic = System.currentTimeMillis();
		HttpClientResult response = null;
		try{
			//NOTE: initially this was reading XML data that's why we use 'apacheHttpGET'
			//For XML see: https://www.baeldung.com/jackson-convert-xml-json (probably requires a correct class for the result)
			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			headers.put("Accept-Encoding", "gzip, deflate");
			headers.put("User-Agent", Connectors.USER_AGENT + " " + API_HEADER_INFO);
			response = Connectors.apacheHttpGET(url.trim(), headers);
			//System.out.println("weather RESULT: " + response.content);		//debug
		}catch (Exception e){
			Debugger.println("WeatherMeteoNorway API call failed with msg.: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
		}
		if (response == null || response.statusCode > 203){
			if (response != null){
				Debugger.println("WeatherMeteoNorway API call failed with msg.: " + response.statusLine + " - code: " + response.statusCode, 1);
				Debugger.println("WeatherMeteoNorway API error info: " + response.content, 1);
			}
			Statistics.addExternalApiHit("Weather MeteoNorway error");
			Statistics.addExternalApiTime("Weather MeteoNorway error", Debugger.toc(tic));
						
			//build the API_Result and goodbye
			service.setStatusFail();
			ServiceResult result = service.buildResult(); 
			return result;
			
		}else if (response.statusCode == 429){
			//Traffic limit reached
			Debugger.println("WeatherMeteoNorway API call failed due to traffic limitation! Please reduce your calls to the API.", 1);
			Statistics.addExternalApiHit("Weather MeteoNorway error");
			Statistics.addExternalApiTime("Weather MeteoNorway error", Debugger.toc(tic));
			//TODO: add a timeout?
						
			//build the API_Result and goodbye
			service.setStatusFail();
			ServiceResult result = service.buildResult(); 
			return result;
		
		//Convert data
		}else{
			try{
				JSONObject weatherData = JSON.parseString(response.content);
				//Units
				JSONObject unitsFound = JSON.getJObject(weatherData, new String[]{"properties", "meta", "units"});
				String tempUnitFound = JSON.getStringOrDefault(unitsFound, "air_temperature", "C");
				String tempUnitUsed = tempUnitFound;
				boolean convertToCelsius = false;
				boolean convertToFahrenheit = false;
				if (!tempUnitFound.equalsIgnoreCase(userPrefTempUnit)){
					if (tempUnitFound.equalsIgnoreCase("C") && userPrefTempUnit.equalsIgnoreCase("F")){
						convertToFahrenheit = true;
					}else if (tempUnitFound.equalsIgnoreCase("F") && userPrefTempUnit.equalsIgnoreCase("C")){
						convertToCelsius = true;
					}
				}
				//Data array
				JSONArray weatherTimeseries = JSON.getJArray(weatherData, new String[]{"properties", "timeseries"});
				
				//Handle timespan
				if (nluResult.input.userTimeLocal == null){
					nluResult.input.userTime = System.currentTimeMillis();
					nluResult.input.userTimeLocal = DateTime.getFormattedDate(Config.defaultSdf);
					Debugger.println("WeatherMeteoNorway service was missing 'userTimeLocal' input. User time was set to local server time!", 1);
				}
				String nowTime = DateTimeConverters.getToday("HH:mm:ss", nluResult.input);
				long secTillMidnight = DateTimeConverters.getTimeUntilMidnight(nowTime).get("total_s");
				
				long timeDiffStart;
				long timeDiffEnd;
				long nextMorningDiffSec = Long.MAX_VALUE;		//never reach by default
				long nextAfternoonDiffSec = Long.MAX_VALUE;		//never reach by default
				if (isTomorrow){
					timeDiffStart = secTillMidnight + 14320; 	//4 o'clock
					timeDiffEnd = timeDiffStart + 72080;  		//+20h
					nextMorningDiffSec = secTillMidnight + 21300;			//5:55 o'clock
					nextAfternoonDiffSec = nextMorningDiffSec + 21600;		//11:55 o'clock
				}else if (isDayOfWeek){
					long offset = (days-1) * 86400;
					timeDiffStart = secTillMidnight + 14320 + offset; 	//4 o'clock + x days
					timeDiffEnd = timeDiffStart + 72080;  		//+20h
					nextMorningDiffSec = secTillMidnight + 21300 + offset;			//5:55 o'clock
					nextAfternoonDiffSec = nextMorningDiffSec + 21600;		//11:55 o'clock
				}else if (isWholeWeek){
					//7 days
					timeDiffStart = 0;
					timeDiffEnd = 604800; //+7d
					nextMorningDiffSec = secTillMidnight + 21300;			//5:55 o'clock
					nextAfternoonDiffSec = nextMorningDiffSec + 21600;		//11:55 o'clock
				}else{
					//today
					timeDiffStart = 0;
					timeDiffEnd = 64880;  //+18h
					if (secTillMidnight < 21300){
						//nextMorningDiffSec = secTillMidnight + 21300;			//5:55 o'clock
						//TODO: correct value?
					}
					if (secTillMidnight < 42900){
						//nextAfternoonDiffSec = nextMorningDiffSec + 21600;		//11:55 o'clock
						//TODO: correct value?
					}
				}
				long sparseResultsThresholdSec = 172800 + secTillMidnight;	//~48h
				
				//Build results
				JSONObject dataOut = new JSONObject();
				JSONArray detailsArray = new JSONArray();
				
				//card type
				ElementType cardElementType = null;
				if (isToday){
					cardElementType = ElementType.weatherNow;
				}else if (isTomorrow){
					cardElementType = ElementType.weatherTmo;
				}else if (isDayOfWeek){
					cardElementType = ElementType.weatherDay;
				}else if (isWholeWeek){
					cardElementType = ElementType.weatherWeek;
				}
				
				//iterate days
				JSONObject morningSummary = null;
				JSONObject afternoonSummary = null;
				int n = 0;
				for (Object o : weatherTimeseries){
					JSONObject dayJson = (JSONObject) o;
					String dateUTC = JSON.getString(dayJson, "time");
					JSONObject dayJsonDetails = JSON.getJObject(dayJson, new String[]{"data", "instant", "details"});
					
					long unixTime = DateTimeConverters.getUnixTimeOfUtcDateString(dateUTC);
					long secondsDiff = (unixTime - nluResult.input.userTime)/1000l + 1l;	//1s offset?!
					
					if (isToday && n <= 1){
						//we always keep the first two for today
						n++;
					}else if (secondsDiff < timeDiffStart){
						continue;	//skip, because its too early
					}else if (secondsDiff > sparseResultsThresholdSec){
						//we keep everything when results get sparse (~48h) 
						n++;
					}else if ((isToday && n%2 == 0) || (!isToday && n%2 > 0)){
						n++;
						continue;	//skip every 2nd entry
					}else{
						n++;
					}
					
					//Common:
					
					//time
					String localDateForItem = DateTimeConverters.getTodayPlusX_seconds(
							"yyyy.MM.dd_EEEE_HH:mm", nluResult.input, secondsDiff
					);
					String[] tags = localDateForItem.split("_");
					String dayShort = WeatherDarkSky.getTimeName(tags[1].toLowerCase(), service.language);
					String dayLong = WeatherDarkSky.getTimeName(dayShort, service.language);
					
					//track morning and afternoon - NOTE: this only works if we get reliable data at 6 and 12 o'clock !
					boolean isMorning = false;
					boolean isAfternoon = false;
					if (secondsDiff > nextMorningDiffSec){
						//System.out.println("MORNING: " + dateUTC); 	//DEBUG
						//JSON.prettyPrint(dayJson); 					//DEBUG
						morningSummary = getSixHoursSummary(dayJson, tags[2], service);
						if (morningSummary != null){
							int i=0;
							do{
								//increase with safety check
								nextMorningDiffSec = nextMorningDiffSec + 86400;
								i++;
							}while(secondsDiff > nextMorningDiffSec && i<10);
							if (i>1){
								Debugger.println(WeatherMeteoNorway.class.getSimpleName() 
										+ " - Next 'morningSummary' seems to be inconsistent and is skipping results - i=" + i, 1);
							}
							isMorning = true;
							//System.out.println("CONFIRMED at i=" + i); 	//DEBUG
						}
					}else if (secondsDiff > nextAfternoonDiffSec){
						//System.out.println("NOON: " + dateUTC); 	//DEBUG
						//JSON.prettyPrint(dayJson); 				//DEBUG
						afternoonSummary = getSixHoursSummary(dayJson, tags[2], service);
						if (afternoonSummary != null){
							int i=0;
							do{
								//increase with safety check
								nextAfternoonDiffSec = nextAfternoonDiffSec + 86400;
								i++;
							}while(secondsDiff > nextAfternoonDiffSec && i<10);
							if (i>1){
								Debugger.println(WeatherMeteoNorway.class.getSimpleName() 
										+ " - Next 'afternoonSummary' seems to be inconsistent and is skipping results - i=" + i, 1);
							}
							isAfternoon = true;
							//System.out.println("CONFIRMED at i=" + i); 	//DEBUG
						}
					}
					
					//temperature
					String tempStr = JSON.getString(dayJsonDetails, "air_temperature");
					int tempInt;
					if (convertToFahrenheit || convertToCelsius){
						double newTemp = Number.convertTemperature(tempStr, tempUnitFound, userPrefTempUnit, userPrefTempUnit);
						tempInt = (int) Math.round(newTemp);
						tempUnitUsed = userPrefTempUnit;
					}else{
						tempInt = (int) Math.round(Double.parseDouble(tempStr));
						tempUnitUsed = tempUnitFound;
					}
					
					//Specific:
					
					//TODAY
					if (isToday){
						String predictTime = NOW;
						JSONObject oneHourSummary = getOneHourSummary(predictTime, dayJson, service);
						String icon = JSON.getString(oneHourSummary, ICON);
						double precipRelative = JSON.getDoubleOrDefault(oneHourSummary, "precipRelative", -1.0d);
						String precipType = JSON.getStringOrDefault(oneHourSummary, "precipType", null);
						
						//Details
						JSONObject thisDetail = new JSONObject();
						JSON.put(thisDetail, "tag", tags[2]);
						JSON.put(thisDetail, "timeUNIX", unixTime);
						JSON.put(thisDetail, "tempA", tempInt);
						JSON.put(thisDetail, "icon", icon);
						JSON.put(thisDetail, "precipRelative", precipRelative);
						JSON.put(thisDetail, "precipType", precipType);		//rain, snow, sleet or null
						JSON.add(detailsArray, thisDetail);
						
						//first entry - now time
						if (n == 1){
							String description = JSON.getString(oneHourSummary, DESC);
							
							//main block
							JSON.put(dataOut, "place", place);
							JSON.put(dataOut, "dateTag", WeatherDarkSky.getTimeName("0", service.language));
							JSON.put(dataOut, "date", tags[0]);
							JSON.put(dataOut, "time", tags[2]);
							JSON.put(dataOut, "timeUNIX", unixTime);
							JSON.put(dataOut, "desc", description);
							//JSON.put(dataOut, "desc48h", tempDayDesc);
							JSON.put(dataOut, "icon", icon);	//clear-day, clear-night,...
							//JSON.put(dataOut, "icon48h", iconDay);
							JSON.put(dataOut, "tempA", tempInt);
							JSON.put(dataOut, "tagA", WeatherDarkSky.getTimeName("-1", service.language));
							JSON.put(dataOut, "units", "°" + tempUnitUsed);
							
							//response info
							service.resultInfoPut("place", place);
							service.resultInfoPut("temp", tempInt);
							service.resultInfoPut("description", description);
							service.resultInfoPut("day", dayLong);
						
						}else if (n == 4){
							JSON.put(dataOut, "tempB", tempInt);
							JSON.put(dataOut, "tagB", tags[2]);
						
						}else if (n == 6){
							JSON.put(dataOut, "tempC", tempInt);
							JSON.put(dataOut, "tagC", tags[2]);
						}
						
					//NEXT 48h
					}else if (days <= 2){
						String predictTime = FUTURE;
						JSONObject oneHourSummary = getOneHourSummary(predictTime, dayJson, service);
						String icon = JSON.getString(oneHourSummary, ICON);
						double precipRelative = JSON.getDoubleOrDefault(oneHourSummary, "precipRelative", -1.0d);
						String precipType = JSON.getStringOrDefault(oneHourSummary, "precipType", null);
						
						//Details
						JSONObject thisDetail = new JSONObject();
						JSON.put(thisDetail, "tag", tags[2]);
						JSON.put(thisDetail, "timeUNIX", unixTime);
						JSON.put(thisDetail, "tempA", tempInt);
						JSON.put(thisDetail, "icon", icon);
						JSON.put(thisDetail, "precipRelative", precipRelative);
						JSON.put(thisDetail, "precipType", precipType);		//rain, snow, sleet or null
						JSON.add(detailsArray, thisDetail);
						
						//we collect daily summary when we have afternoon data 
						if (isAfternoon){
							double tempMaxMorning = JSON.getDoubleOrDefault(morningSummary, "tempMax", Double.MIN_VALUE);
							double tempMinMorning = JSON.getDoubleOrDefault(morningSummary, "tempMin", Double.MIN_VALUE);
							double tempMaxAfternoon = JSON.getDoubleOrDefault(afternoonSummary, "tempMax", Double.MIN_VALUE);
							double tempMinAfternoon = JSON.getDoubleOrDefault(afternoonSummary, "tempMin", Double.MIN_VALUE);
							int tempMin, tempMax;
							if (convertToFahrenheit || convertToCelsius){
								tempMaxMorning = Number.convertTemperature(tempMaxMorning + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempMinMorning = Number.convertTemperature(tempMinMorning + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempMaxAfternoon = Number.convertTemperature(tempMaxAfternoon + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempMinAfternoon = Number.convertTemperature(tempMinAfternoon + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempUnitUsed = userPrefTempUnit;
							}else{
								tempUnitUsed = tempUnitFound;
							}
							tempMin = (int) Math.round(Math.min(tempMinMorning, tempMinAfternoon));
							tempMax = (int) Math.round(Math.max(tempMaxMorning, tempMaxAfternoon));
							if (tempMin == tempMax) tempMin = tempMin - 1;		//we cheat here (it probably never happens anyway ^^)
							String description6hMorning = JSON.getString(morningSummary, DESC);
							String icon6hMorning = JSON.getString(morningSummary, ICON);
							
							//main block
							JSON.put(dataOut, "place", place);
							JSON.put(dataOut, "dateTag", dayLong);
							JSON.put(dataOut, "date", tags[0]);
							JSON.put(dataOut, "time", tags[2]);
							JSON.put(dataOut, "timeUNIX", unixTime);
							JSON.put(dataOut, "desc", description6hMorning);
							JSON.put(dataOut, "icon", icon6hMorning);	//clear-day, clear-night,...
							JSON.put(dataOut, "tempA", tempMin);
							JSON.put(dataOut, "tagA", "min");
							JSON.put(dataOut, "tempB", tempMax);
							JSON.put(dataOut, "tagB", "max");
							JSON.put(dataOut, "units", "°" + tempUnitUsed);
							
							//response info
							service.resultInfoPut("place", place);
							service.resultInfoPut("temp", tempMax);
							service.resultInfoPut("description", description6hMorning);
							service.resultInfoPut("day", dayLong);
							service.resultInfoPut("context1", tempMin);
						}
						
					//FUTURE
					}else{
						//we collect daily summary when we have afternoon data 
						if (isAfternoon){
							double tempMaxMorning = JSON.getDoubleOrDefault(morningSummary, "tempMax", Double.MIN_VALUE);
							double tempMinMorning = JSON.getDoubleOrDefault(morningSummary, "tempMin", Double.MIN_VALUE);
							double tempMaxAfternoon = JSON.getDoubleOrDefault(afternoonSummary, "tempMax", Double.MIN_VALUE);
							double tempMinAfternoon = JSON.getDoubleOrDefault(afternoonSummary, "tempMin", Double.MIN_VALUE);
							int tempMin, tempMax;
							if (convertToFahrenheit || convertToCelsius){
								tempMaxMorning = Number.convertTemperature(tempMaxMorning + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempMinMorning = Number.convertTemperature(tempMinMorning + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempMaxAfternoon = Number.convertTemperature(tempMaxAfternoon + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempMinAfternoon = Number.convertTemperature(tempMinAfternoon + "",	tempUnitFound, userPrefTempUnit, userPrefTempUnit);
								tempUnitUsed = userPrefTempUnit;
							}else{
								tempUnitUsed = tempUnitFound;
							}
							tempMin = (int) Math.round(Math.min(tempMinMorning, tempMinAfternoon));
							tempMax = (int) Math.round(Math.max(tempMaxMorning, tempMaxAfternoon));
							if (tempMin == tempMax) tempMin = tempMin - 1;		//we cheat here (it probably never happens anyway ^^)
							
							//Details morning
							String descriptionMorning = JSON.getString(morningSummary, DESC);
							String iconMorning = JSON.getString(morningSummary, ICON);
							double precipRelativeMorning = JSON.getDoubleOrDefault(morningSummary, "precipRelative", -1.0d);
							String precipTypeMorning = JSON.getStringOrDefault(morningSummary, "precipType", null);
							//add
							JSONObject thisDetailMorning = new JSONObject();
							//JSON.put(thisDetailMorning, "tag", dayShort + ". " + partOfDay.get("morning").get(service.language));
							JSON.put(thisDetailMorning, "tag", dayShort + ". " + JSON.getStringOrDefault(morningSummary, "time", ""));
							JSON.put(thisDetailMorning, "timeUNIX", unixTime);
							JSON.put(thisDetailMorning, "tempA", tempMinMorning);
							JSON.put(thisDetailMorning, "tempB", tempMaxMorning);
							JSON.put(thisDetailMorning, "icon", iconMorning);
							JSON.put(thisDetailMorning, "precipRelative", precipRelativeMorning);
							JSON.put(thisDetailMorning, "precipType", precipTypeMorning);		//rain, snow, sleet or null
							JSON.add(detailsArray, thisDetailMorning);
							
							//SPECIFIC DAY 
							if (!isWholeWeek){
								//Details afternoon
								String descriptionAfternoon = JSON.getString(afternoonSummary, DESC);
								String iconAfternoon = JSON.getString(afternoonSummary, ICON);
								double precipRelativeAfternoon = JSON.getDoubleOrDefault(afternoonSummary, "precipRelative", -1.0d);
								String precipTypeAfternoon = JSON.getStringOrDefault(afternoonSummary, "precipType", null);
								//add
								JSONObject thisDetailAfternoon = new JSONObject();
								//JSON.put(thisDetailAfternoon, "tag", dayShort + ". " + partOfDay.get("afternoon").get(service.language));
								JSON.put(thisDetailAfternoon, "tag", dayShort + ". " + JSON.getStringOrDefault(afternoonSummary, "time", ""));
								JSON.put(thisDetailAfternoon, "timeUNIX", unixTime);
								JSON.put(thisDetailAfternoon, "tempA", tempMinAfternoon);
								JSON.put(thisDetailAfternoon, "tempB", tempMaxAfternoon);
								JSON.put(thisDetailAfternoon, "icon", iconAfternoon);
								JSON.put(thisDetailAfternoon, "precipRelative", precipRelativeAfternoon);
								JSON.put(thisDetailAfternoon, "precipType", precipTypeAfternoon);	//rain, snow, sleet or null
								JSON.add(detailsArray, thisDetailAfternoon);
								
								//main block
								JSON.put(dataOut, "place", place);
								JSON.put(dataOut, "dateTag", dayLong);
								JSON.put(dataOut, "date", tags[0]);
								JSON.put(dataOut, "time", tags[2]);
								JSON.put(dataOut, "timeUNIX", unixTime);
								JSON.put(dataOut, "desc", descriptionMorning);
								JSON.put(dataOut, "icon", iconMorning);	//clear-day, clear-night,...
								JSON.put(dataOut, "tempA", tempMin);
								JSON.put(dataOut, "tagA", "min");
								JSON.put(dataOut, "tempB", tempMax);
								JSON.put(dataOut, "tagB", "max");
								JSON.put(dataOut, "units", "°" + tempUnitUsed);
								
								//response info
								service.resultInfoPut("place", place);
								service.resultInfoPut("temp", tempMax);
								service.resultInfoPut("description", descriptionMorning);
								service.resultInfoPut("day", dayLong);
								service.resultInfoPut("context1", tempMin);
							
							//WEEK
							}else{
								//main block - TODO: how to adapt this for WHOLE WEEK ??
								JSON.put(dataOut, "place", place);
								JSON.put(dataOut, "dateTag", "week");		//TODO: translate
								JSON.put(dataOut, "date", "");
								JSON.put(dataOut, "time", "");
								JSON.put(dataOut, "timeUNIX", unixTime);
								JSON.put(dataOut, "desc", "overview");		//TODO: translate
								JSON.put(dataOut, "icon", "default");	//clear-day, clear-night,...
								JSON.put(dataOut, "tempA", "");				//TODO: missing
								JSON.put(dataOut, "tagA", "min");			//TODO: missing
								JSON.put(dataOut, "tempB", "");
								JSON.put(dataOut, "tagB", "max");
								JSON.put(dataOut, "units", "°" + tempUnitUsed);
								
								//response info
								service.resultInfoPut("place", place);
								service.resultInfoPut("temp", "");
								service.resultInfoPut("description", "");	//TODO: missing
								service.resultInfoPut("day", "");
							}
						}
					}
										
					if (secondsDiff > timeDiffEnd){
						break;		//end, because its too late
					}
				}
				
				//summary
				JSONObject detailsOut = null;
				if (days <= 2){
					detailsOut = JSON.make("hourly", detailsArray);
				}else if (isWholeWeek){
					detailsOut = JSON.make("daily", detailsArray);
				}else{
					detailsOut = JSON.make("partsOfDay", detailsArray);
				}
				//JSON.prettyPrint(dataOut); 			//DEBUG
				//JSON.prettyPrint(detailsOut); 		//DEBUG
				
				//build card
				Card card = new Card(Card.TYPE_SINGLE);
				card.addElement(cardElementType, dataOut, detailsOut, null, "", "", "", null, null);
				//add it
				service.addCard(card.getJSON());
				service.hasCard = true;
				
				//all clear?
				if (isWholeWeek){
					if (ENVIRONMENTS.deviceHasActiveDisplay(nluResult.input.environment)){
						service.setStatusSuccess();
						service.setCustomAnswer(answerThisWeek);
					}else{
						service.setStatusOkay();
						service.setCustomAnswer(missingDisplayForWeek);
					}
				}else if (isDayOfWeek){
					service.setStatusSuccess();
					service.setCustomAnswer(answerDayOfWeek);
				}else if (isTomorrow){
					service.setStatusSuccess();
					service.setCustomAnswer(answerTomorrow);
				}else{
					service.setStatusSuccess();
					isToday = true;
					//stick with the default answer (today)
				}
				
				//finally build the API_Result
				ServiceResult result = service.buildResult();
				return result;
			
			//FAILED TO PROCESS
			}catch (Exception e){
				Debugger.println("WeatherMeteoNorway failed to process API response with msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				
				Statistics.addExternalApiHit("Weather MeteoNorway error");
				Statistics.addExternalApiTime("Weather MeteoNorway error", Debugger.toc(tic));
							
				//build the API_Result and goodbye
				service.setStatusFail();
				ServiceResult result = service.buildResult(); 
				return result;
			}
		}
	}
	
	//--------------------- Data Builder -----------------------
	
	//collect data in 'dataOut' and 'detailsArray' objects
	private static JSONObject getOneHourSummary(String predictTime,	JSONObject dayJson, ServiceBuilder service){
		
		JSONObject nextHour = JSON.getJObject(dayJson, new String[]{"data", "next_1_hours"});
		JSONObject oneHourSummary = JSON.getJObject(nextHour, "summary");
		JSONObject oneHourDetails = JSON.getJObject(nextHour, "details");
		
		String summarySymbol = JSON.getStringOrDefault(oneHourSummary, "symbol_code", "default");
		JSONObject summaryDescAndIcon = getIconWithDescription(summarySymbol, service.language, predictTime);
		String description = (String) summaryDescAndIcon.get(DESC);
		String icon = (String) summaryDescAndIcon.get(ICON);
		
		double precipitationAmount = JSON.getDoubleOrDefault(oneHourDetails, "precipitation_amount", 0.0d);
		double precipAmountRelative;
		String precipType = null;
		if (summarySymbol.contains("rain")){
			precipType = "rain";
			precipAmountRelative = precipitationAmount / rainReferenceMmPerHour;
		}else if (summarySymbol.contains("snow")){
			precipType = "snow";
			precipAmountRelative = precipitationAmount / snowReferenceMmPerHour;
		}else if (summarySymbol.contains("sleet")){
			precipType = "sleet";
			precipAmountRelative = precipitationAmount / sleetReferenceMmPerHour;
		}else{
			//unknown type?
			precipAmountRelative = precipitationAmount / rainReferenceMmPerHour;
		}
		
		JSONObject res = JSON.make(
			DESC, description,
			ICON, icon,
			"precipRelative", precipAmountRelative,
			"precipType", precipType
		);
		return res;
	}
	
	//get 6 hour prediction at point in time
	private static JSONObject getSixHoursSummary(JSONObject dayJson, String time, ServiceBuilder service){
		
		JSONObject nextSixHours = JSON.getJObject(dayJson, new String[]{"data", "next_6_hours"});
		if (nextSixHours == null){
			return null;
		}
		JSONObject nextSummary = JSON.getJObject(nextSixHours, "summary");
		JSONObject nextDetails = JSON.getJObject(nextSixHours, "details");
		
		String summarySymbol = JSON.getStringOrDefault(nextSummary, "symbol_code", "default");
		JSONObject summaryDescAndIcon = getIconWithDescription(summarySymbol, service.language, FUTURE);
		String description = (String) summaryDescAndIcon.get(DESC);
		String icon = (String) summaryDescAndIcon.get(ICON);
		
		double tempMax = JSON.getDoubleOrDefault(nextDetails, "air_temperature_max", Double.MIN_VALUE);
		double tempMin = JSON.getDoubleOrDefault(nextDetails, "air_temperature_min", Double.MIN_VALUE);
		
		double precipitationAmount = JSON.getDoubleOrDefault(nextDetails, "precipitation_amount", 0.0d);
		double precipAmountRelative;
		String precipType = null;
		if (summarySymbol.contains("rain")){
			precipType = "rain";
			precipAmountRelative = precipitationAmount / (rainReferenceMmPerHour * 6.0d);
		}else if (summarySymbol.contains("snow")){
			precipType = "snow";
			precipAmountRelative = precipitationAmount / (snowReferenceMmPerHour * 6.0d);
		}else if (summarySymbol.contains("sleet")){
			precipType = "sleet";
			precipAmountRelative = precipitationAmount / (sleetReferenceMmPerHour * 6.0d);
		}else{
			//unknown type?
			precipAmountRelative = precipitationAmount / (rainReferenceMmPerHour * 6.0d);
		}
		
		JSONObject res = JSON.make(
			DESC, description,
			ICON, icon,
			"precipRelative", precipAmountRelative,
			"precipType", precipType,
			"time", time
		);
		if (tempMax != Double.MIN_VALUE) JSON.put(res, "tempMax", tempMax);
		if (tempMin != Double.MIN_VALUE) JSON.put(res, "tempMin", tempMin);
		
		return res;
	}

}
