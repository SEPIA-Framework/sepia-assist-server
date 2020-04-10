package net.b07z.sepia.server.assist.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.DateAndTime.DateType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Connectors.HttpClientResult;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

public class WeatherMeteoNorway implements ServiceInterface {
	
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
		descBaseByType.put(1, JSON.make( 
				LANGUAGES.DE, JSON.make(NOW, "es ist {desc}", FUTURE, "es wird {desc}"),
				LANGUAGES.EN, JSON.make(NOW, "it is {desc}", FUTURE, "it will be {desc}")
		));
		descBaseByType.put(2, JSON.make( 
				LANGUAGES.DE, JSON.make(NOW, "es gibt {desc}", FUTURE, "es wird {desc} geben"),
				LANGUAGES.EN, JSON.make(NOW, "there is {desc}", FUTURE, "there will be {desc}")
		));
		descBaseByType.put(3, JSON.make( 
				LANGUAGES.DE, JSON.make(NOW, "es gibt {desc}", FUTURE, "es wird {desc} geben"),
				LANGUAGES.EN, JSON.make(NOW, "there are {desc}", FUTURE, "there will be {desc}")
		));
	}
	//Type 1: (The report/forecast says) it is/will be 			| (Der Wetterbericht sagt) es ist/wird
	//Type 2: (Here is the report/forecast.) There is/will be 	| (Hier ist der Wetterberichtt.) Es gibt/wird ... geben
	//Type 3: (Here is the report/forecast.) There are/will be 	| (Hier ist der Wetterberichtt.) Es gibt/wird ... geben
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
		description = descBase.replace("{desc}", description);
		return JSON.make(
			ICON, icon,
			DESC, description
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
			.addCustomAnswer("missingGeoData", missingGeoData)		//geo data like GPS coordinates are missing
			;
		info.addAnswerParameters("place", "tempRequest", "tempDesc", "day");
		
		return info;
	}
	private static final String answerToday = "weather_today_1a";
	private static final String answerTomorrow = "weather_tomorrow_1a";
	private static final String answerDayOfWeek = "weather_day_1a";
	private static final String answerThisWeek = "weather_week_1a";
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
		String targetDate = "";
		long days = 0;
		boolean tooFarOrUnspecific = false;
		if (dateType.equals(DateType.unspecific.name())){
			//String unspecificDate = (String) timeJSON.get(InterviewData.TIME_UNSPECIFIC);
			days = 7; 		//TODO: this is arbitrary
			tooFarOrUnspecific = true;
			
		}else{
			targetDate = (String) timeJSON.get(InterviewData.DATE_DAY);
			//String targetTime = Tools_DateTime.convertDateFormat(targetDate, Config.default_sdf, "hh:mm");
			//targetDate = targetDate.replaceFirst("_.*", "").trim();
			
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
			service.setCustomAnswer(answerThisWeek);
			isWholeWeek = true;
		}else if (days > 1){
			service.setCustomAnswer(answerDayOfWeek);
			isDayOfWeek = true;
		}else if (days == 1){
			service.setCustomAnswer(answerTomorrow);
			isTomorrow = true;
		}else{
			isToday = true;
			//leave it with the default answer
		}
		
		Debugger.println("cmd: weather, place: " + place + ", days in future: " + days, 2);		//debug
		
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
			headers.put("User-Agent", Connectors.USER_AGENT + " " + "Self-hosted open-source SEPIA assistant sepia-framework.github.io");
			response = Connectors.apacheHttpGET(url.trim(), headers);
			//System.out.println("weather RESULT: " + response.toJSONString());		//debug
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
			System.out.println("weather RESULT: " + response.content);		//debug
			try{
				JSONObject weatherData = JSON.parseString(response.content);
				//Units
				JSONObject unitsFound = JSON.getJObject(weatherData, new String[]{"properties", "meta", "units"});
				String tempUnitFound = JSON.getStringOrDefault(unitsFound, "air_temperature", "C");
				//Data array
				JSONArray weatherTimeseries = JSON.getJArray(weatherData, new String[]{"properties", "timeseries"});
				
				//Handle timespan
				if (nluResult.input.userTimeLocal == null){
					nluResult.input.userTime = System.currentTimeMillis();
					nluResult.input.userTimeLocal = DateTime.getFormattedDate(Config.defaultSdf);
					Debugger.println("WeatherMeteoNorway service was missing 'userTimeLocal' input. User time was set to local server time!", 1);
				}
				String[] todayDate = DateTimeConverters.getToday("yyyy.MM.dd_HH:mm", nluResult.input).split("_");
				String nowDate = todayDate[0];
				String nowTime = todayDate[1];
				long secTillMidnight = DateTimeConverters.getTimeUntilMidnight(nowTime + ":59").get("total_s");
				
				long timeDiffStart;
				long timeDiffEnd;
				if (isTomorrow){
					timeDiffStart = secTillMidnight + 14320; 	//4 o'clock
					timeDiffEnd = timeDiffStart + 72080;  		//+20h
				}else if (isDayOfWeek){
					timeDiffStart = secTillMidnight + 14320 + ((days-1) * 86400); 	//4 o'clock + x days
					timeDiffEnd = timeDiffStart + 72080;  		//+20h
				//}else if (isWholeWeek){
				//	//TODO
				//604800 	//7d
				}else{
					//today
					timeDiffStart = 0;
					timeDiffEnd = 64880;  //+18h
				}
				long sparseResultsThresholdSec = 172800 + secTillMidnight;	//~48h
				
				//Build results
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
					
					//Today
					String summarySymbol;
					String predictTime;
					if (isToday){
						JSONObject oneHourSummary = JSON.getJObject(dayJson, new String[]{"data", "next_1_hours", "summary"});
						summarySymbol = JSON.getStringOrDefault(oneHourSummary, "symbol_code", "default");
						predictTime = NOW;
					}else{
						summarySymbol = "default";
						predictTime = FUTURE;
					}
					JSONObject summaryDescAndIcon = getIconWithDescription(summarySymbol, nluResult.input.language, predictTime);
					
					String localDateForItem = DateTimeConverters.getTodayPlusX_seconds(
							"yyyy.MM.dd_HH:mm:ss", nluResult.input, secondsDiff
					);
					System.out.println("Date UTC: " + dateUTC);		//debug
					System.out.println("Date Local: " + localDateForItem);		//debug
					System.out.println("Temp. (" + tempUnitFound + "): " + JSON.getString(dayJsonDetails, "air_temperature"));		//debug
					System.out.println("Desc.: " + summaryDescAndIcon.get(DESC));
					System.out.println("Icon: " + summaryDescAndIcon.get(ICON));
					System.out.println("");		//debug
					
					if (secondsDiff > timeDiffEnd){
						break;		//end, because its too late
					}
				}
			
			}catch (Exception e){
				Debugger.println("WeatherMeteoNorway failed to process API response with msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
			}
		}
		
		
		//------------------------
		Statistics.addExternalApiHit("Weather MeteoNorway");
		Statistics.addExternalApiTime("Weather MeteoNorway", Debugger.toc(tic));
					
		//build the API_Result and goodbye
		service.setStatusFail();
		ServiceResult result = service.buildResult(); 
		return result;
	}

}
