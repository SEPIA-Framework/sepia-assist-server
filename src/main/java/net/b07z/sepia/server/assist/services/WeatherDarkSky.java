package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.assistant.ActionBuilder;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.geo.GeoFactory;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.DateAndTime.DateType;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

import java.net.URLEncoder;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * DarkSky weather API.
 * 
 * @author Florian Quirin
 *
 */
public class WeatherDarkSky implements ServiceInterface {
	
	//some localizations
	public static HashMap<String, String> timeNames_de = new HashMap<>();
	public static HashMap<String, String> timeNames_en = new HashMap<>();
	static{
		timeNames_de.put("-1", "jetzt");
		timeNames_de.put("0", "Heute");
		timeNames_de.put("+1", "Morgen");
		timeNames_de.put("+2", "Übermorgen");
		timeNames_de.put("1", "Montag");		timeNames_de.put("Mo", "Montag");
		timeNames_de.put("2", "Dienstag");		timeNames_de.put("Die", "Dienstag");
		timeNames_de.put("3", "Mittwoch");		timeNames_de.put("Mi", "Mittwoch");
		timeNames_de.put("4", "Donnerstag");	timeNames_de.put("Do", "Donnerstag");
		timeNames_de.put("5", "Freitag");		timeNames_de.put("Fr", "Freitag");
		timeNames_de.put("6", "Samstag");		timeNames_de.put("Sa", "Samstag");
		timeNames_de.put("7", "Sonntag");		timeNames_de.put("So", "Sonntag");
		timeNames_de.put("monday", "Mo");		timeNames_de.put("montag", "Mo");
		timeNames_de.put("tuesday", "Die");		timeNames_de.put("dienstag", "Die");
		timeNames_de.put("wednesday", "Mi");	timeNames_de.put("mittwoch", "Mi");
		timeNames_de.put("thursday", "Do");		timeNames_de.put("donnerstag", "Do");
		timeNames_de.put("friday", "Fr");		timeNames_de.put("freitag", "Fr");
		timeNames_de.put("saturday", "Sa");		timeNames_de.put("samstag", "Sa");
		timeNames_de.put("sunday", "So");		timeNames_de.put("sonntag", "So");
		
		timeNames_en.put("-1", "now");
		timeNames_en.put("0", "Today");
		timeNames_en.put("+1", "Tomorrow");
		timeNames_en.put("+2", "Day after tomorrow");
		timeNames_en.put("1", "Monday");		timeNames_en.put("Mon", "Monday");
		timeNames_en.put("2", "Thuesday");		timeNames_en.put("Tue", "Thuesday");
		timeNames_en.put("3", "Wednesday");		timeNames_en.put("Wed", "Wednesday");
		timeNames_en.put("4", "Thursday");		timeNames_en.put("Thu", "Thursday");
		timeNames_en.put("5", "Friday");		timeNames_en.put("Fri", "Friday");
		timeNames_en.put("6", "Saturday");		timeNames_en.put("Sat", "Saturday");
		timeNames_en.put("7", "Sunday");		timeNames_en.put("Sun", "Sunday");
		timeNames_en.put("monday", "Mon");		timeNames_en.put("montag", "Mon");
		timeNames_en.put("tuesday", "Tue");		timeNames_en.put("dienstag", "Tue");
		timeNames_en.put("wednesday", "Wed");	timeNames_en.put("mittwoch", "Wed");
		timeNames_en.put("thursday", "Thu");	timeNames_en.put("donnerstag", "Thu");
		timeNames_en.put("friday", "Fri");		timeNames_en.put("freitag", "Fri");
		timeNames_en.put("saturday", "Sat");	timeNames_en.put("samstag", "Sat");
		timeNames_en.put("sunday", "Sun");		timeNames_en.put("sonntag", "Sun");
	}
	public static HashMap<String, String> placeNames_de = new HashMap<>();
	public static HashMap<String, String> placeNames_en = new HashMap<>();
	static{
		placeNames_de.put("<here>", "hier");
		placeNames_de.put("<there>", "dort");
		placeNames_de.put("<coordinates>", "diesen Koordinaten");
		placeNames_de.put("<place>", "diesem Ort");
		
		placeNames_en.put("<here>", "here");
		placeNames_en.put("<there>", "there");
		placeNames_en.put("<coordinates>", "this coordinates");
		placeNames_en.put("<place>", "this place");
	}
	
	/**
	 * Get a localized name for a day of the week (1-7), today (0) and now (-1) or short localized name for a day (monday - Mon, montag - Mo).
	 * @param tag - numbers from -1 to 7 as string, days in different formats like "monday", "montag" or "Mon", "Mo", "Tue", "Die" etc., "week"
	 * @param language - ISO code
	 */
	public static String getTimeName(String tag, String language){
		String name = "";
		if (language.equals(LANGUAGES.DE)){
			name = timeNames_de.get(tag);
		}else{
			name = timeNames_en.get(tag);
		}
		return (name == null)? "" : name;
	}
	/**
	 * Get a localized name for a specific place name (&lt;here&gt;, &lt;there&gt;, &lt;coordinates&gt;, ...)
	 */
	public static String getPlaceName(String tag, String language){
		String name = "";
		if (language.equals(LANGUAGES.DE)){
			name = placeNames_de.get(tag);
		}else{
			name = placeNames_en.get(tag);
		}
		return (name == null)? "" : name;
	}
	
	//info
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
		info//.addSuccessAnswer("weather_today_1a")						//this one should work for any case
			.addSuccessAnswer(answerToday)
			.addCustomAnswer("tomorrowAnswer", answerTomorrow)		//specific for "tomorrow"
			.addCustomAnswer("dayOfWeekAnswer", answerDayOfWeek)	//specific for a certain day of the week
			.addCustomAnswer("weekAnswer", answerThisWeek)			//specific for anything even further than tomorrow
			.addFailAnswer("weather_0a")							//for some reason the service failed
			.addAnswerParameters("place", "temp", "description", "day");
		
		return info;
	}
	private static final String answerToday = "weather_today_1a";
	private static final String answerTomorrow = "weather_tomorrow_1a";
	private static final String answerDayOfWeek = "weather_day_1a";
	private static final String answerThisWeek = "weather_week_1a";
	private static final String answerNoApiKey = "default_no_access_0a";

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get interview parameters
		JSONObject placeJSON = nluResult.getRequiredParameter(PARAMETERS.PLACE).getData(); 	//note: we use getRequiredParameter here 
		JSONObject timeJSON = nluResult.getRequiredParameter(PARAMETERS.TIME).getData();	//... because we set default values in getInfo
				
		//parameter adaptation to service format
		String coords = null;
		boolean missingGeoCoder = false;
		if (placeJSON.containsKey("error") && !GeoFactory.createGeoCoder().isSupported()){
			missingGeoCoder = true;
		}else{
			coords = placeJSON.get(InterviewData.LOCATION_LAT).toString() + "," + placeJSON.get(InterviewData.LOCATION_LNG).toString();
		}
		String place = (String) placeJSON.get(InterviewData.LOCATION_CITY);
		if (place == null){
			place = (String) placeJSON.get(InterviewData.LOCATION_STATE);
		}
		if (place == null){
			place = getPlaceName("<coordinates>", api.language);
		}
		api.resultInfoPut("place", place);
		
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
		if (tooFarOrUnspecific){
			api.setCustomAnswer(answerThisWeek);
		}else if (days > 1){
			api.setCustomAnswer(answerDayOfWeek);
		}else if (days == 1){
			api.setCustomAnswer(answerTomorrow);
		}else{
			isToday = true;
			//leave it with the default answer
		}
		
		Debugger.println("cmd: weather, place: " + place + ", days in future: " + days, 2);		//debug
		
		//GET DATA
		
		//additional API parameters
		String add_params = "&exclude=minutely,hourly,alerts";
		if (isToday){
			add_params = "&exclude=minutely,alerts,daily";
		}
		String units = "si";	//"auto";	//TODO: handle Farenheit
		String unitsName = (units.equals("si"))? "°" : "F";
		
		//make the HTTP GET call to DarkSky API
		//TODO: add Accept-Encoding: gzip to request header ????
		String url = "";
		if (Is.nullOrEmpty(Config.forecast_io_key) || missingGeoCoder){
			//set all parameters to empty to avoid AnswerLoader complaints
			api.resultInfoFill(); 		//TODO: this is probably not required anymore
						
			//add some real info here about missing key
			api.setCustomAnswer(answerNoApiKey);
			//TODO: we can distinguish between weather API keys and e.g. Geo-Coder API keys for Googlemaps ...
			
			//add button that links to API-key help
			ActionBuilder.addApiKeyInfoButton(api);
			
			//all clear?
			api.setStatusOkay();
			
			//finally build the API_Result
			ServiceResult result = api.buildResult();
					
			//return result_JSON.toJSONString();
			return result;
			
			//throw new RuntimeException(DateTime.getLogDate() + " ERROR - Weather_DarkSky.java / Got no API key!");
		}
		try{
			url = "https://api.forecast.io/forecast/" + Config.forecast_io_key + "/" + URLEncoder.encode(coords, "UTF-8") + "?units=" + units + "&lang=" + api.language + add_params;
		}catch (Exception e){
			throw new RuntimeException(DateTime.getLogDate() + " ERROR - Weather_DarkSky.java / URLEncoder() - Failed to parse coords: " + coords, e);
		}
		//System.out.println("weather url: " + url);		//debug
		long tic = System.currentTimeMillis();
		JSONObject response = Connectors.httpGET(url.trim());
		//System.out.println("weather RESULT: " + response.toJSONString());		//debug
		
		//Check API result
		if (!Connectors.httpSuccess(response)){
			Statistics.addExternalApiHit("Weather DarkSky error");
			Statistics.addExternalApiTime("Weather DarkSky error", Debugger.toc(tic));
			
			//set all parameters to empty to avoid AnswerLoader complaints
			api.resultInfoFill();		//TODO: this is probably not required anymore
			api.setStatusFail();
			
			//build the API_Result and goodbye
			ServiceResult result = api.buildResult(); 
			return result;
		}else{
			Statistics.addExternalApiHit("Weather DarkSky");
			Statistics.addExternalApiTime("Weather DarkSky", Debugger.toc(tic));
		}
		
		//Build service answer
		try{
			JSONObject dataOut = new JSONObject();
			JSONObject detailsOut = new JSONObject();
			ElementType cardElementType;
			
			long now = System.currentTimeMillis();
			String timeZone = (String) response.get("timezone");
			
			JSONObject currently = (JSONObject) response.get("currently");
			JSONObject details;
			if (isToday){
				details = (JSONObject) response.get("hourly");
			}else{
				details = (JSONObject) response.get("daily");
			}
			JSONArray detailsData = (JSONArray) details.get("data");
			
			//now
			if (isToday){
				String[] todayDate = DateTimeConverters.getToday("yyyy.MM.dd_HH:mm", nluResult.input).split("_"); 
				String nowDate = "";
				String nowTime = "";
				if (todayDate.length == 2){
					nowDate = todayDate[0];
					nowTime = todayDate[1];
				}
				
				String dayLong = "";
				double tempNow = Double.parseDouble(currently.get("temperature").toString());
				tempNow = Math.round(tempNow);
				String tempNowDesc = ((String) currently.get("summary")).replaceFirst("\\.$", "").trim();
				String tempDayDesc = ((String) details.get("summary")).replaceFirst("\\.$", "").trim();
				String icon = ((String) currently.get("icon"));
				String iconDay = ((String) details.get("icon"));
				String precipType = ((String) currently.get("precipType"));
				double precipProb = Double.parseDouble(currently.get("precipProbability").toString());
				//build main info
				JSON.add(dataOut, "place", place);
				JSON.add(dataOut, "dateTag", getTimeName("0", api.language));
				JSON.add(dataOut, "date", nowDate);
				JSON.add(dataOut, "time", nowTime);
				JSON.add(dataOut, "timeUNIX", now);
				JSON.add(dataOut, "desc", tempNowDesc);
				JSON.add(dataOut, "desc48h", tempDayDesc);
				JSON.add(dataOut, "tempA", (int) tempNow);
				JSON.add(dataOut, "tagA", getTimeName("-1", api.language));
				JSON.add(dataOut, "icon", icon);	//clear-day, clear-night, rain, snow, sleet, wind, fog, cloudy, partly-cloudy-day, partly-cloudy-night or null
				JSON.add(dataOut, "icon48h", iconDay);
				JSON.add(dataOut, "units", unitsName);
				JSON.add(dataOut, "precipProb", precipProb);
				JSON.add(dataOut, "precipType", precipType);	//rain, snow, sleet or null
					
				JSONArray hours = new JSONArray();
				int i=0;
				for (Object o : detailsData){
					if (i > 18){
						break;
					}
					JSONObject hh = (JSONObject) o;
					double tempHour = Double.parseDouble(hh.get("temperature").toString());
					tempHour = Math.round(tempHour);
					long timeUnixHour = Converters.obj2LongOrDefault(hh.get("time"), null)*1000;
					String[] tags = DateTime.getDateAtTimeZone(timeUnixHour, "EEEE HH:mm", timeZone).split(" ");
					String dayShort = getTimeName(tags[0].toLowerCase(), api.language);
					dayLong = getTimeName(dayShort, api.language);
					String tag = tags[1];
					//String tag = Tools_DateTime.getDateAtTimeZone(timeUnixHour, "HH:mm", timeZone);
					String precipTypeHour = ((String) hh.get("precipType"));
					double precipProbHour = Double.parseDouble(hh.get("precipProbability").toString());
					long timeDiff = timeUnixHour-now;
					//+3
					if ((timeDiff < (210*60*1000)) && (timeDiff >= (150*60*1000))){
						JSON.add(dataOut, "tempB", (int) tempHour);
						JSON.add(dataOut, "tagB", tag);
					}
					//+6
					else if ((timeDiff < (390*60*1000)) && (timeDiff >= (330*60*1000))){
						JSON.add(dataOut, "tempC", (int) tempHour);
						JSON.add(dataOut, "tagC", tag);
					}
					if ((i & 1) == 0){
						i++;
						continue; 		//skip even i
					}else{
						i++;
					}
					JSONObject thisHour = new JSONObject();
					JSON.add(thisHour, "tag", tag);
					JSON.add(thisHour, "timeUNIX", timeUnixHour);
					JSON.add(thisHour, "tempA", (int) tempHour);
					JSON.add(thisHour, "precipProb", precipProbHour);
					JSON.add(thisHour, "precipType", precipTypeHour);
					JSON.add(hours, thisHour);
				}
				JSON.add(detailsOut, "hourly", hours);
				
				//response info
				api.resultInfoPut("place", place);
				api.resultInfoPut("temp", (int) tempNow);
				api.resultInfoPut("description", tempNowDesc);
				api.resultInfoPut("day", dayLong);
				
				//card type
				cardElementType = ElementType.weatherNow;
			
			//tomorrow and week
			}else{
				String dayLong = "";
				JSONArray daysData = new JSONArray();
				String targetDesc = "";
				double targetTemp = -273;
				
				String tempWeekDesc = ((String) details.get("summary")).replaceFirst("\\.$", "").trim();
				String iconWeek = ((String) details.get("icon"));
				JSON.add(dataOut, "place", place);
				JSON.add(dataOut, "units", unitsName);
				JSON.add(dataOut, "descWeek", tempWeekDesc);
				JSON.add(dataOut, "desc", tempWeekDesc);			//gets overwritten when needed
				JSON.add(dataOut, "iconWeek", iconWeek);
				JSON.add(dataOut, "icon", iconWeek);				//gets overwritten when needed
				JSON.add(dataOut, "date", targetDate);
				//JSON.add(dataOut, "dateTag", (days < 3)? getTimeName("+" + days, api.language) : targetDate);		//gets overwritten when needed
				
				int i=0;
				for (Object o : detailsData){
					if (i > 7){
						break;
					}
					i++;
					JSONObject dd = (JSONObject) o;
					double tempMin = Double.parseDouble(dd.get("temperatureMin").toString());
					tempMin = Math.round(tempMin);
					double tempMax = Double.parseDouble(dd.get("temperatureMax").toString());
					tempMax = Math.round(tempMax);
					long timeUnixDay = Converters.obj2LongOrDefault(dd.get("time"), null)*1000;
					String[] tags = DateTime.getDateAtTimeZone(timeUnixDay, "EEEE dd.MM.", timeZone).split(" ");
					String dayShort = getTimeName(tags[0].toLowerCase(), api.language);
					String thisDayLong = getTimeName(dayShort, api.language);
					String tag = dayShort + ". " + tags[1];
					String precipTypeDay = ((String) dd.get("precipType"));
					double precipProbDay = Double.parseDouble(dd.get("precipProbability").toString());
					JSONObject thisDay = new JSONObject();
						JSON.add(thisDay, "tag", tag);
						JSON.add(thisDay, "timeUNIX", timeUnixDay);
						JSON.add(thisDay, "tempA", (int) tempMin);
						JSON.add(thisDay, "tempB", (int) tempMax);
						JSON.add(thisDay, "precipProb", precipProbDay);
						JSON.add(thisDay, "precipType", precipTypeDay);
					JSON.add(daysData, thisDay);
					long timeDiff = timeUnixDay-now;
					long targetDiffUp = (days)*24*60*60*1000;
					long targetDiffLow = (days-1)*24*60*60*1000;
					//target day?
					if ((timeDiff >= targetDiffLow) && (timeDiff < targetDiffUp)){ 		
						//target day
						String tempDayDesc = ((String) dd.get("summary")).replaceFirst("\\.$", "").trim();
						String iconDay = ((String) dd.get("icon"));
						JSON.add(dataOut, "timeUNIX", timeUnixDay);
						JSON.add(dataOut, "tempA", (int) tempMin);
						JSON.add(dataOut, "tagA", "min");
						JSON.add(dataOut, "tempB", (int) tempMax);
						JSON.add(dataOut, "tagB", "max");
						JSON.add(dataOut, "precipProb", precipProbDay);
						JSON.add(dataOut, "precipType", precipTypeDay);
						
						//within 7 days range?
						if (!tooFarOrUnspecific){
							JSON.add(dataOut, "desc", tempDayDesc);
							JSON.add(dataOut, "icon", iconDay);
							dayLong = thisDayLong;
							JSON.add(dataOut, "dateTag", dayLong);
							targetDesc = tempDayDesc;
							targetTemp = tempMax;
						}else{
							JSON.add(dataOut, "date", DateTime.getDateAtTimeZone(timeUnixDay, "dd.MM.yyyy", timeZone));
							JSON.add(dataOut, "dateTag", "");
						}
					}
				}
				JSON.add(detailsOut, "daily", daysData);
				
				//response info and card type
				api.resultInfoPut("place", place);
				if (days == 1){
					api.resultInfoPut("temp", (int) targetTemp);
					api.resultInfoPut("description", targetDesc);
					api.resultInfoPut("day", dayLong);
					cardElementType = ElementType.weatherTmo;
				}else if (!tooFarOrUnspecific){
					api.resultInfoPut("temp", (int) targetTemp);
					api.resultInfoPut("description", targetDesc);
					api.resultInfoPut("day", dayLong);
					cardElementType = ElementType.weatherWeek;
				}else{
					api.resultInfoPut("temp", "");
					api.resultInfoPut("description", tempWeekDesc);
					api.resultInfoPut("day", "");
					cardElementType = ElementType.weatherWeek;
				}
			}
			
			//build card
			Card card = new Card(Card.TYPE_SINGLE);
			card.addElement(cardElementType, dataOut, detailsOut, null, "", "", "", null, null);
			//add it
			api.addCard(card.getJSON()); 	//pre-Alpha: api.cardInfo = card.cardInfo;
			api.hasCard = true;
			
			//all clear?
			api.status = "success";
			
			//finally build the API_Result
			ServiceResult result = api.buildResult();
					
			//return result_JSON.toJSONString();
			return result;
			
		//ERROR
		}catch (Exception e){
			Debugger.println("Weather - error: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
			
			//set all parameters to empty to avoid AnswerLoader complaints
			api.resultInfoFill();
			api.setStatusFail();
			
			//build the API_Result and goodbye
			ServiceResult result = api.buildResult(); 
			return result;
		}
	}

}
