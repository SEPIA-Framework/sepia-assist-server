package net.b07z.sepia.server.assist.parameters;

import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interviews.Interview;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * THE class to handle date and time extraction.
 * 
 * @author Florian Quirin
 *
 */
public class DateAndTime implements ParameterHandler{
	
	//---data---
	
	//REGEX CONSTANTS
	public static final String TIME_TAGS_DE = "( |)(sekunde(n|)|minute(n|)|stunde(n|)|tag(en|e|)|woche(n|)|monat(en|e|)|jahr(en|e|))";
	public static final String TIME_TAGS_LARGE_DE = "( |)(tag(en|e|)|woche(n|)|monat(en|e|)|jahr(en|e|))";
	public static final String TIME_YEARS_DE = "( |)(jahr(en|e|)|j|a|yr|y)(\\.|)"; 
	public static final String TIME_MONTHS_DE = "( |)(monat(en|e|)|mo)(\\.|)";
	public static final String TIME_WEEKS_DE = "( |)(woche(n|)|wk)(\\.|)";
	public static final String TIME_DAYS_DE = "( |)(tag(en|e|)|d)(\\.|)";
	public static final String TIME_HOURS_DE = "( |)(stunde(n|)|std|hr|h)(\\.|)";
	public static final String TIME_MINUTES_DE = "( |)(minute(n|)|min)(\\.|)";
	public static final String TIME_SECONDS_DE = "( |)(sekunde(n|)|sek|sec|s)(\\.|)";
	
	public static final String TIME_TAGS_EN = "( |)(second(s|)|minute(s|)|hour(s|)|day(s|)|week(s|)|month(s|)|year(s|))";
	public static final String TIME_TAGS_LARGE_EN = "( |)(day(s|)|week(s|)|month(s|)|year(s|))";
	public static final String TIME_YEARS_EN = "( |)(year(s|)|a|yr|y)(\\.|)"; 
	public static final String TIME_MONTHS_EN = "( |)(month(s|)|mo)(\\.|)";
	public static final String TIME_WEEKS_EN = "( |)(week(s|)|wk)(\\.|)";
	public static final String TIME_DAYS_EN = "( |)(day(s|)|d)(\\.|)";
	public static final String TIME_HOURS_EN = "( |)(hour(s|)|hr|h)(\\.|)";
	public static final String TIME_MINUTES_EN = "( |)(minute(s|)|min)(\\.|)";
	public static final String TIME_SECONDS_EN = "( |)(second(s|)|sec|s)(\\.|)";
	
	public static final String TIME_TAGS_SHORT = "( |)(sec|sek|s|min|std|h|hr|d|wk|mo|j|a|yr|y)(\\.|)($|\\s)";
	
	public static final String DAY_TAGS_DE = "(montag|dienstag|mittwoch|donnerstag|freitag|samstag|sonntag)";
	public static final String DAY_TAGS_RELATIVE_DE = "(heute|jetzt|uebermorgen|(?<!" + DAY_TAGS_DE + ")morgen|wochenende|naechste woche|die(se|) woche|naechsten tage)";
	public static final String DAY_TAGS_EN = "(monday|tuesday|wednesday|thursday|friday|saturday|sunday|weekend)";
	public static final String DAY_TAGS_RELATIVE_EN = "(today|now|day after tomorrow|tomorrow|next week|(this|the) week|next days|next few days)";
	
	public static final String TIME_UNSPECIFIC_TAGS_DE = "(am |)((?<=" + DAY_TAGS_DE + ") morgen|am morgen|morgens|frueh|vormittag(s|)|mittag(s|)|nachmittag(s|)|abend(s|)|nacht(s|)|spaet)";
	public static final String TIME_UNSPECIFIC_TAGS_EN = "(in the |at the |at |by |)(morning|early|forenoon|midday|afternoon|noon|evening|night|late)";
	
	public static final String CLOCK_TAGS_DE =    "( |)(uhr)|"
												+ "(um (\\d{1,2}:\\d\\d|\\d\\d|\\d))(\\s?|$)|"
												+ "(fuer (\\d{1,2}:\\d\\d))(\\s?|$)";
	/*
	public static final String CLOCK_TAGS_DE = "( |)(uhr)|((um |fuer )(\\d\\d|\\d))(\\s|$)|"
														+ "(\\d(\\d|):\\d\\d)(\\s|$)|"
														+ "(^\\d(\\d|)$)"; 		//<- is that too general?
	*/
	public static final String CLOCK_TAGS_EN =    "( |)(o('|)clock( (p|a)(\\.|)m(\\.|)|)|(p|a)(\\.|)m(\\.|))|"
												+ "(at (\\d{1,2}:\\d\\d|\\d\\d|\\d))(( |)(p|a)(\\.|)m(\\.|)|)(\\s?|$)|"
												+ "(for (\\d{1,2}:\\d\\d))(( |)(p|a)(\\.|)m(\\.|)|)(\\s?|$)";
	/*
	public static final String CLOCK_TAGS_EN = "( |)(o('|)clock( p(\\.|)m(\\.|)| a(\\.|)m(\\.|)|)|p(\\.|)m(\\.|)|a(\\.|)m(\\.|))|"
														+ "((at |for )(\\d\\d|\\d))(\\s|$)|"
														+ "(\\d(\\d|):\\d\\d)(\\s|$)"
														+ "(^\\d(\\d|)$)";		//<- is that too general?
	*/
	public static final String DATE_DIRECT_TAGS = "("
											+ "\\d{1,2}\\.\\d{1,2}\\. \\d{4}|"
											+ "\\d{1,4}\\.\\d{1,2}\\.\\d{1,4}|"
											+ "\\d{1,2}\\.\\d{1,2}(\\.|)|"
											+ "\\d{1,2}\\.\\d{2,4}|"
											+ "\\d{1,2}\\.|"
											+ "\\d{1,4}/\\d{1,2}/\\d{1,4}|"
											+ "\\d{1,2}/\\d{1,4}"
											+ ")"; 
	
	public static final String MORNING = "08:00";
	public static final String FORENOON = "10:30";
	public static final String NOON = "13:00";
	public static final String AFTERNOON = "16:00";
	public static final String EVENING = "19:30";
	public static final String NIGHT = "22:30";

	/**
	 * Type of given date like "duration" (e.g. "for 10 minutes") or "pointInTime" (e.g. "at 10 o'clock")  
	 */
	public enum DateType {
		duration, 		//in 10 minutes
		pointInTime,	//at 10 o'clock
		now,			//...
		period,			//for the next 10 minutes
		unspecific,		//at the weekend
		unknown 		//just in case ...
	}
	
	public static HashMap<String, String> days_de = new HashMap<>();
	public static HashMap<String, String> days_en = new HashMap<>();
	static{
		days_de.put("+0", "heute");
		days_de.put("+1", "morgen");
		days_de.put("+2", "übermorgen");
		days_de.put(String.valueOf(Calendar.MONDAY), "Montag");
		days_de.put(String.valueOf(Calendar.TUESDAY), "Dienstag");
		days_de.put(String.valueOf(Calendar.WEDNESDAY), "Mittwoch");
		days_de.put(String.valueOf(Calendar.THURSDAY), "Donnerstag");
		days_de.put(String.valueOf(Calendar.FRIDAY), "Freitag");
		days_de.put(String.valueOf(Calendar.SATURDAY), "Samstag");
		days_de.put(String.valueOf(Calendar.SUNDAY), "Sonntag");
		
		days_en.put("+0", "today");
		days_en.put("+1", "tomorrow");
		days_en.put("+2", "the day after tomorrow");
		days_en.put(String.valueOf(Calendar.MONDAY), "Monday");
		days_en.put(String.valueOf(Calendar.TUESDAY), "Tuesday");
		days_en.put(String.valueOf(Calendar.WEDNESDAY), "Wednesday");
		days_en.put(String.valueOf(Calendar.THURSDAY), "Thursday");
		days_en.put(String.valueOf(Calendar.FRIDAY), "Friday");
		days_en.put(String.valueOf(Calendar.SATURDAY), "Saturday");
		days_en.put(String.valueOf(Calendar.SUNDAY), "Sunday");
	}
	public static String getDay(String input, String language){
		String day = "";
		if (language.equals(LANGUAGES.DE)){
			day = days_de.get(input);
		}else if (language.equals(LANGUAGES.EN)){
			day = days_en.get(input);
		}
		if (day == null || day.isEmpty()){
			Debugger.println("DateAndTime.java - missing language '" + language + "' version for local day: " + input, 1);
			return "";
		}
		return day;
	}
	public static HashMap<String, String> times_de = new HashMap<>();
	public static HashMap<String, String> times_en = new HashMap<>();
	static{
		times_de.put("<day>", "Tag");
		times_de.put("<days>", "Tage");
		times_de.put("<hour>", "Stunde");
		times_de.put("<hours>", "Stunden");
		times_de.put("<minute>", "Minute");
		times_de.put("<minutes>", "Minuten");
		times_de.put("<second>", "Sekunde");
		times_de.put("<seconds>", "Sekunden");
				
		times_en.put("<day>", "day");
		times_en.put("<days>", "days");
		times_en.put("<hour>", "hour");
		times_en.put("<hours>", "hours");
		times_en.put("<minute>", "minute");
		times_en.put("<minutes>", "minutes");
		times_en.put("<second>", "second");
		times_en.put("<seconds>", "seconds");
	}
	public static String getTimeNames(String input, String language){
		String time = "";
		if (language.equals(LANGUAGES.DE)){
			time = times_de.get(input);
		}else if (language.equals(LANGUAGES.EN)){
			time = times_en.get(input);
		}
		if (time == null || time.isEmpty()){
			Debugger.println("Date.DateAndTime - missing language '" + language + "' version for local time name: " + input, 1);
			return "";
		}
		return time;
	}
	//----------

	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nluInput;
	
	String found = ""; 		//found raw text during extraction (that can be removed later)
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nluInput = nluInput;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nluInput = nluResult.input;
	}
	
	@Override
	public String extract(String input) {
		HashMap<String, String> dateMap;
		String date;
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.TIME);
		if (pr != null){
			date = pr.getExtracted();
			this.found = pr.getFound();
			
			return date;
		}
		
		dateMap = RegexParameterSearch.get_date(input, language);
		String dateTag = dateMap.get("date_tag");
		String timeTag = dateMap.get("time_tag");
		//System.out.println("input: " + input); 	//DEBUG
		//System.out.println("DATE-MAP: "); 		//DEBUG
		//Debugger.printMap(dateMap); 				//DEBUG
		
		//Nothing found?
		if (Is.nullOrEmpty(dateTag) && Is.nullOrEmpty(timeTag)){
			return "";
		}
		
		//TODO: date_tag can be normalized and differ from original input
		this.found = (dateTag.isEmpty()? timeTag : dateTag);
		
		//date = NLU_parameter_search.convert_date(dateTag, Config.default_sdf, nlu_input);
		String[] dateRes = convertTagToDate(dateMap, nluInput);
		date = dateRes[0] + "&&" + dateRes[1];
		//System.out.println("DATE: " + date); 				//DEBUG
		
		String dateType = DateType.unknown.name();
		
		if (!dateTag.isEmpty() && (dateRes[0].isEmpty() && dateRes[1].isEmpty())){
			//converter can't make sense of the phrase
			dateType = DateType.unspecific.name();
			date = "<" + dateType + ">&&" + dateTag; 
			//Debugger.println("Date.extract() date cannot be converted to default sdf: " + date_tag, 1);
			
		}else{
			if (!dateTag.isEmpty()){
				//german
				if (language.equals(LANGUAGES.DE)){
					if (NluTools.stringContains(dateTag, "(" + ".*\\d" + CLOCK_TAGS_DE + ")|"
														+ "(in \\d+)(" + TIME_TAGS_DE + "|" + "\\d+" + TIME_TAGS_SHORT + ")|"
														+ "(" + DAY_TAGS_DE + ")")){
						dateType = DateType.pointInTime.name();
						
					}else if (NluTools.stringContains(dateTag, "(jetzt)")){
						dateType = DateType.now.name();
					
					}else if (NluTools.stringContains(dateTag, "(" + TIME_TAGS_DE + "|" + "\\d+" + TIME_TAGS_SHORT + ")")){
						dateType = DateType.duration.name();
					}
					
				//other
				}else{
					if (NluTools.stringContains(dateTag, "(" + ".*\\d" + CLOCK_TAGS_EN + ")|"
														+ "(in \\d+)(" + TIME_TAGS_EN + "|" + "\\d+" + TIME_TAGS_SHORT + ")|"
														+ "(" + DAY_TAGS_EN + ")")){
						dateType = DateType.pointInTime.name();
					
					}else if (NluTools.stringContains(dateTag, "(now)")){
						dateType = DateType.now.name();
						
					}else if (NluTools.stringContains(dateTag, "(\\d+" + TIME_TAGS_EN + "|" + "\\d+" + TIME_TAGS_SHORT + ")")){
						dateType = DateType.duration.name();
					}
				}
			}
			if (!dateTag.isEmpty() && dateRes[0].isEmpty() && dateRes[1].isEmpty()){
				date = "<" + dateType + ">&&" + dateTag;
				
			}else if (dateTag.isEmpty() && dateRes[0].isEmpty() && dateRes[1].isEmpty()){
				date = "";
				
			}else{
				date = "<" + dateType + ">&&" + date;
			} 
		}
		date = date.replaceFirst("&&\\s*$", "").trim();
		
		//store it
		pr = new ParameterResult(PARAMETERS.TIME, date, found);
		nluInput.addToParameterResultStorage(pr);
		
		//System.out.println("DATE-FINAL: " + date); 		//DEBUG
		return date; 	//<-- note: if you change this format then you need to check DateClock parameter as well
	}
	
	@Override
	public String guess(String input) {
		return "";
	}

	@Override
	public String getFound() {
		return found;					//TODO: implement!!!
	}

	@Override
	public String remove(String input, String found) {
		return RegexParameterSearch.remove_date(input, found, language);
	}
	
	@Override
	public String responseTweaker(String input){
		/* removed, because extract does the same ...
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*?\\b(am|an dem)\\b", "").trim();
		}else{
			input = input.replaceAll(".*?\\b(at the|at)\\b", "").trim();
		}
		*/ 
		/* removed as well, 'extract' will find that too now 
		if (language.equals(LANGUAGES.DE)){
			if (input.matches("^(\\d|\\d\\d)$")){
				input = input + " uhr";
			}
		}else{
			if (input.matches("^(\\d|\\d\\d)$")){
				input = input + " oclock";
			}
		}
		//we need to convert the 'upgraded' input to right format again...
		nluInput.clearParameterResult(PARAMETERS.TIME);
		input = extract(input);
		*/
		return input;
	}

	@Override
	public String build(String input) {
		//check for proper user time
		if (nluInput.userTimeLocal.isEmpty()){
			return Interview.ERROR_MISSING + ";;" + Interview.TYPE_INPUT_INFO + ";;" + "<user_time_local>";
		}
		
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers)
		if (Is.notNullOrEmpty(input) && !input.startsWith("<")){
			input = extract(input);
			if (Is.nullOrEmpty(input)){
				return "";
			}
		
		//use UNIX tag?
		}else if (input.startsWith("<unix>")){
			input = extract(input.replaceFirst(Pattern.quote("<unix>"), ""));
		}
		
		//separate dateType
		String dateType = DateType.unknown.name();
		String day = "";
		String time = "";

		//extracted time-date
		if (input.startsWith("<") && input.contains("&&")){
			String[] dateSplit = input.split("&&", 2);
			dateType = dateSplit[0].replaceAll("^<|>$", "").trim();
			input = dateSplit[1].trim();
			
			if (input.contains("&&")){
				dateSplit = input.split("&&", 2);
				day = dateSplit[0];
				time = dateSplit[1];
				input = day + Config.defaultSdfSeparator + time;
				
			}else if (input.matches("\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d")){
				day = input;
			
			}else{
				//???
			}
			
		//user-time
		}else if (input.equals("<" + Parameter.Defaults.user_time + ">")){
			input = nluInput.userTimeLocal;
			String[] dateSplit = input.split(Config.defaultSdfSeparatorRegex, 2);
			day = dateSplit[0];
			time = dateSplit[1];
			dateType = DateType.pointInTime.name();
		
		//nothing or error?
		}else{
			//TODO: what does that mean? Empty input or raw, unconverted format?
			//End with build success or empty string?
			return "";
		}
		
		//check for unspecific date
		if (dateType.equals(DateType.unspecific.name())){
			//build default result
			JSONObject timeResultJSON = new JSONObject();
				JSON.add(timeResultJSON, InterviewData.TIME_UNSPECIFIC, input);
				JSON.add(timeResultJSON, InterviewData.TIME_TYPE, dateType);
				JSON.add(timeResultJSON, InterviewData.TIME_DIFF, "");
			
			buildSuccess = true;
			return timeResultJSON.toJSONString();
		}
		
		//TODO: can this still happen? Yes! We need to replace it with extract->repeat build
		//check for proper time string - should have already been done in responseHandler
		if ((time.isEmpty() && input.matches("\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d")) || input.matches(Config.defaultSdfRegex)){
			//all clear
		}else{
			Debugger.println("Date.build() input has unexpected format OR format changed: " + input, 1);
			/*
			//try to get proper result
			input = extract(input);
			System.out.println("DATE NEW INPUT: " + input);	//DEBUG
			if (!input.startsWith("<")){
				return "";
			}else{
				return build(input);
			}
			*/
			throw(new RuntimeException()); 					//DEBUG
		}
		
		//get time difference
		HashMap<String, Long> diff;
		if (time.isEmpty()){
			//calculate difference to same time of other date
			String nowTime = nluInput.userTimeLocal.split(Config.defaultSdfSeparatorRegex)[1];
			diff = DateTimeConverters.dateDifference(nluInput.userTimeLocal, input + Config.defaultSdfSeparator + nowTime);
		}else{
			//calculate exact difference
			diff = DateTimeConverters.dateDifference(nluInput.userTimeLocal, input);
		}
		
		//build default result
		JSONObject timeResultJSON = new JSONObject();
		JSON.put(timeResultJSON, InterviewData.TIME_TYPE, dateType);
		if (day.isEmpty()){
			JSON.put(timeResultJSON, InterviewData.DATE_INPUT, input);
		}else{
			JSON.put(timeResultJSON, InterviewData.DATE_DAY, day);
		}
		if (!time.isEmpty()) JSON.put(timeResultJSON, InterviewData.DATE_TIME, time);
		JSON.put(timeResultJSON, InterviewData.TIME_DIFF, diff);
		
		buildSuccess = true;
		//System.out.println("BUILD DATE: " + timeResultJSON.toJSONString()); 		//DEBUG
		return timeResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.TIME_TYPE + "\"") && input.contains("\"" + InterviewData.TIME_DIFF + "\"")){
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
	
	//--------------- HELPERS -----------------
	
	public static String[] convertTagToDate(String date_tag, String time_tag, NluInput nluInput){
		HashMap<String, String> dateMap = new HashMap<>();
		dateMap.put("date_tag", date_tag);
		dateMap.put("time_tag", time_tag);
		return convertTagToDate(dateMap, nluInput);
	}
	public static String[] convertTagToDate(HashMap<String, String> dateEx, NluInput nluInput){
		String dateTag = dateEx.get("date_tag");
		String timeTag = dateEx.get("time_tag");
		
		if (dateTag.isEmpty() && timeTag.isEmpty()){
			return (new String[]{"", ""});
		}
		if (dateTag.matches("\\d{12,14}")){
			long eventUnixTime = Long.parseLong(dateTag);
			//calculate difference to user time
			long secondsDiffToUser = (eventUnixTime - nluInput.userTime)/1000l;
			//TODO: this fails when the event is crossing summer/winter-time switch
			String[] eventDateRelativeToUser = DateTimeConverters.getTodayPlusX_seconds("yyyy.MM.dd_HH:mm:ss", nluInput, secondsDiffToUser).split("_");
			return (new String[]{eventDateRelativeToUser[0], eventDateRelativeToUser[1]});
		}
		if (dateTag.matches("\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d_\\d\\d:\\d\\d:\\d\\d")){
			String[] dateTimeRes = dateTag.split("_");
			String dateRes = dateTimeRes[0];
			String timeRes = dateTimeRes[1];
			return (new String[]{dateRes, timeRes});
		}
		
		String dayDate = "";
		String y,m,w,d,h,min,sec;
		boolean timeWasMissing = false;
		
		//System.out.println("IN: " + dateTag); 		//DEBUG
		
		//language independent - TODO: improve
		String formattedDate = NluTools.stringFindFirst(dateTag, "\\d{1,2}\\.\\d{1,2}\\.\\d{4,}" + "|" + "\\d{1,2}\\.\\d{1,2}\\.\\d{2,}"
				+ "|" + "\\d{1,2}\\.\\d{1,2}(\\.|)" + "|" + "\\d{1,2}\\.\\d{4,}" + "|" + "\\d{1,2}(\\.)"
				+ "|" + "\\d{1,2}/\\d{1,2}/\\d{4,}" + "|" + "\\d{1,2}/\\d{1,2}/\\d{2,}" + "|" + "\\d{1,2}/\\d{4,}" + "|" + "\\d{1,2}/\\d{1,2}" + "|" + "(\\d\\dth|\\dth)");
		
		if (!formattedDate.isEmpty()){
			//System.out.println("FORMATTED DAY: " + formattedDate);
			
			//eu
			if (formattedDate.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4,}")){
				dayDate = DateTimeConverters.convertDateFormat(formattedDate, "dd.MM.yyyy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{2,}")){
				dayDate = DateTimeConverters.convertDateFormat(formattedDate, "dd.MM.yy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d{1,2}\\.\\d{1,2}(\\.|)")){
				String year = DateTimeConverters.getToday("yyyy", nluInput);
				dayDate = DateTimeConverters.convertDateFormat((formattedDate+"."+year).replaceAll("\\.\\.", "."), "dd.MM.yyyy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d{1,2}\\.\\d{4,}")){
				String day = DateTimeConverters.getToday("dd", nluInput);
				dayDate = DateTimeConverters.convertDateFormat((day+"."+formattedDate).replaceAll("\\.\\.", "."), "dd.MM.yyyy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d{1,2}(\\.)")){
				String month_year = DateTimeConverters.getToday("MM.yyyy", nluInput);
				dayDate = DateTimeConverters.convertDateFormat((formattedDate+"."+month_year).replaceAll("\\.\\.", "."), "dd.MM.yyyy", "yyyy.MM.dd", nluInput.language);
			}
			//us
			else if (formattedDate.matches("\\d{1,2}/\\d{1,2}/\\d{4,}")){
				dayDate = DateTimeConverters.convertDateFormat(formattedDate, "MM/dd/yyyy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d{1,2}/\\d{1,2}/\\d{2,}")){
				dayDate = DateTimeConverters.convertDateFormat(formattedDate, "MM/dd/yy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d{1,2}/\\d{4,}")){
				String day = DateTimeConverters.getToday("dd", nluInput);
				dayDate = DateTimeConverters.convertDateFormat(day+"/"+formattedDate, "dd/MM/yyyy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d{1,2}/\\d{1,2}")){
				String year = DateTimeConverters.getToday("yyyy", nluInput);
				dayDate = DateTimeConverters.convertDateFormat(formattedDate+"/"+year, "MM/dd/yyyy", "yyyy.MM.dd", nluInput.language);
			}else if (formattedDate.matches("\\d\\dth|\\dth|1st|2nd|3rd")){
				formattedDate = formattedDate.replaceAll("th|st|nd|rd", "").trim(); 
				String month_year = DateTimeConverters.getToday("MM.yyyy", nluInput);
				dayDate = DateTimeConverters.convertDateFormat((formattedDate+"."+month_year).replaceAll("\\.\\.", "."), "dd.MM.yyyy", "yyyy.MM.dd", nluInput.language);
			}
			//System.out.println("FORMATTED DAY CONVERTED: " + dayDate);
		}
		//some cleaning
		String dataTagOrg = dateTag; 						//<- we need this for clockTag because of the dot 
		dateTag = dateTag.replaceAll("\\.", "").trim();
		
		//GERMAN
		if (nluInput.language.equals(LANGUAGES.DE)){
			//DAY
			if (dayDate.isEmpty()){
				String dayTag1 = NluTools.stringFindFirst(dateTag, DAY_TAGS_DE);
				String dayTag2 = NluTools.stringFindFirst(dateTag, DAY_TAGS_RELATIVE_DE);
				dayDate = getDateToDay((dayTag1.trim() + " " + dayTag2.trim()).trim(), "yyyy.MM.dd", nluInput);

				//System.out.println("DAY: " + dayDate); 			//DEBUG
			}
			
			//TIME
			String clockTag = NluTools.stringFindLongest(dataTagOrg, "(\\d{1,2}:\\d\\d|\\d{1,2})" + CLOCK_TAGS_DE);
			if (clockTag.isEmpty() && dataTagOrg.matches("^(\\d{1,2}:\\d\\d|\\d{1,2})$")){
				//TODO: is that safe to assume? Probably depends on many correlated parameters
				clockTag = dateTag;
			}
			if (timeTag.isEmpty() && NluTools.stringContains(dateTag, "jetzt")) timeTag = "jetzt"; 		//add now
			if (!clockTag.isEmpty() || !timeTag.isEmpty()){
				String baseDate = dayDate;
				if (baseDate.isEmpty()){
					baseDate = DateTimeConverters.getToday("yyyy.MM.dd", nluInput);
				}
				String clockDate = getDateToClock(clockTag, timeTag, baseDate, nluInput);
				if (!clockDate.isEmpty()){
					dayDate = clockDate;
				}
				//System.out.println("DAY+CLOCK: " + dayDate); 		//DEBUG
			
			}else if (!dayDate.isEmpty()){
				//add now time
				timeWasMissing = true;
				dayDate = (dayDate + "_" + DateTimeConverters.getToday("HH:mm:ss", nluInput));
			}
			
			y = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_YEARS_DE);
			m = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_MONTHS_DE);
			w = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_WEEKS_DE);
			d = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_DAYS_DE);
			h = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_HOURS_DE);
			min = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_MINUTES_DE);
			sec = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_SECONDS_DE);
		
		//OTHERS
		}else{
			//DAY
			if (dayDate.isEmpty()){				
				String dayTag1 = NluTools.stringFindFirst(dateTag, DAY_TAGS_EN);
				String dayTag2 = NluTools.stringFindFirst(dateTag, DAY_TAGS_RELATIVE_EN);
				dayDate = getDateToDay((dayTag1.trim() + " " + dayTag2.trim()).trim(), "yyyy.MM.dd", nluInput);

				//System.out.println("DAY: " + dayDate); 			//DEBUG
			}
			
			//TIME
			String clockTag = NluTools.stringFindLongest(dataTagOrg, "(\\d{1,2}:\\d\\d|\\d{1,2})" + CLOCK_TAGS_EN);
			if (clockTag.isEmpty() && dataTagOrg.matches("^(\\d{1,2}:\\d\\d|\\d{1,2})$")){
				//TODO: is that safe to assume? Probably depends on many correlated parameters
				clockTag = dateTag;
			}
			if (timeTag.isEmpty() && NluTools.stringContains(dateTag, "now")) timeTag = "now"; 		//add now
			if (!clockTag.isEmpty() || !timeTag.isEmpty()){
				String baseDate = dayDate;
				if (baseDate.isEmpty()){
					baseDate = DateTimeConverters.getToday("yyyy.MM.dd", nluInput);
				}
				String clockDate = getDateToClock(clockTag, timeTag, baseDate, nluInput);
				if (!clockDate.isEmpty()){
					dayDate = clockDate;
				}
				//System.out.println("DAY+CLOCK: " + dayDate); 	//DEBUG
			
			}else if (!dayDate.isEmpty()){
				//add now time
				timeWasMissing = true;
				dayDate = (dayDate + "_" + DateTimeConverters.getToday("HH:mm:ss", nluInput));
			}
			
			y = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_YEARS_EN);
			m = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_MONTHS_EN);
			w = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_WEEKS_EN);
			d = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_DAYS_EN);
			h = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_HOURS_EN);
			min = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_MINUTES_EN);
			sec = NluTools.stringFindFirst(dateTag, "\\d+" +  TIME_SECONDS_EN);
		}
		
		//System.out.println("y: " + y + ", MM: " + m + ", w: " + w + 
		//		", d: " + d + ", h: " + h + ", min: " + min + ", sec: " + sec); 		//DEBUG
		
		long Xy = (y.isEmpty())? 0 : Integer.parseInt(y.replaceAll("\\D", "").trim()) * 52l * 7l * 24l * 60l * 60l;
		long XMM = (m.isEmpty())? 0 : Integer.parseInt(m.replaceAll("\\D", "").trim()) * 30l * 24l * 60l * 60l;
		long Xw = (w.isEmpty())? 0 : Integer.parseInt(w.replaceAll("\\D", "").trim()) * 7l * 24l * 60l * 60l;
		long Xd = (d.isEmpty())? 0 : Integer.parseInt(d.replaceAll("\\D", "").trim()) * 24l * 60l * 60l;
		long Xh = (h.isEmpty())? 0 : (Integer.parseInt(h.replaceAll("\\D", "").trim()) * 60l * 60l);
		long Xm = (min.isEmpty())? 0 : (Integer.parseInt(min.replaceAll("\\D", "").trim()) * 60l);
		long Xs = (sec.isEmpty())? 0 : (Integer.parseInt(sec.replaceAll("\\D", "").trim()));
		long totalOffset = (Xy + XMM + Xw + Xd + Xh + Xm + Xs);
		
		String date = "";
		if (totalOffset > 0){
			//Has base date?
			if (dayDate.isEmpty()){
				dayDate = DateTimeConverters.getToday(Config.defaultSdf, nluInput);
			}
			//CONVERT - in case default changes
			if (!dayDate.matches(Config.defaultSdfRegex)){
				Debugger.println("DateAndTime - convertTagToDate - default date format has changed??? - see: " + dayDate, 3);
				dayDate = DateTimeConverters.convertDateFormat(dayDate, "yyyy.MM.dd_HH:mm:ss", Config.defaultSdf, nluInput.language);
			}
			//ADD
			//System.out.println("Total offset (s): " + totalOffset); 	//DEBUG
			//System.out.println("Day date: " + dayDate); 				//DEBUG
			date = DateTimeConverters.getDatePlusX_seconds(Config.defaultSdf, dayDate, totalOffset);
			
		}else{
			//CONVERT - in case default changes
			if (!dayDate.isEmpty() && !dayDate.matches(Config.defaultSdfRegex)){
				Debugger.println("DateAndTime - convertTagToDate - default date format has changed??? - see: " + dayDate, 3);
				dayDate = DateTimeConverters.convertDateFormat(dayDate, "yyyy.MM.dd_HH:mm:ss", Config.defaultSdf, nluInput.language);
			}
			date = dayDate;
		}
		
		//separate and finalize
		if (!dayDate.isEmpty()){
			String[] dateTimeRes = date.split(Config.defaultSdfSeparatorRegex);
			String dateRes = dateTimeRes[0];
			String timeRes = dateTimeRes[1];
			if (timeWasMissing){
				timeRes = "";
			}
			return (new String[]{dateRes, timeRes});
		}else{
			return (new String[]{"", ""});
		}
	}
	
	/**
	 * Convert a clock-tag to a real date string using a base-date to add the time to. Takes also care of "p.m." like tags.
	 * @param clockTag - things like 12:30 o'clock
	 * @param timeTag - a parameter describing an approximate time like "evening" or "night"
	 * @param baseDate - string in the format "yyyy.MM.dd"
	 * @param nluInput
	 * @return clock added to base-date as yyyy.MM.dd_HH:mm:ss or empty
	 */
	private static String getDateToClock(String clockTag, String timeTag, String baseDate, NluInput nluInput){
		boolean isPM = false;
		
		if ((clockTag.isEmpty() && timeTag.isEmpty()) || baseDate.isEmpty()){
			return "";
		}else if (clockTag.isEmpty()){
			//get time constants
			String timeConstant = "";
			if (NluTools.stringContains(timeTag, "now|jetzt")){	return (baseDate + "_" + DateTimeConverters.getToday("HH:mm:ss", nluInput));	}
			//GERMAN		
			if (nluInput.language.equals(LANGUAGES.DE)){
				if (NluTools.stringContains(timeTag, "morgen|morgens|frueh")){	timeConstant=MORNING;		}
				else if (NluTools.stringContains(timeTag, "vormittag(s|)")){	timeConstant=FORENOON;		}
				else if (NluTools.stringContains(timeTag, "mittag(s|)")){		timeConstant=NOON;			}
				else if (NluTools.stringContains(timeTag, "nachmittag(s|)")){	timeConstant=AFTERNOON;		}
				else if (NluTools.stringContains(timeTag, "abend(s|)|spaet")){	timeConstant=EVENING;		}
				else if (NluTools.stringContains(timeTag, "nacht(s|)")){		timeConstant=NIGHT;			}
			
			//OTHER
			}else{
				if (NluTools.stringContains(timeTag, "morning|early")){		timeConstant=MORNING;		}
				else if (NluTools.stringContains(timeTag, "forenoon")){		timeConstant=FORENOON;		}
				else if (NluTools.stringContains(timeTag, "midday|noon")){		timeConstant=NOON;			}
				else if (NluTools.stringContains(timeTag, "afternoon")){		timeConstant=AFTERNOON;		}
				else if (NluTools.stringContains(timeTag, "evening|late")){	timeConstant=EVENING;		}
				else if (NluTools.stringContains(timeTag, "night")){			timeConstant=NIGHT;			}
			}
			return (timeConstant.isEmpty()? "" : (baseDate + "_" + timeConstant + ":00"));
		}
		
		if (!timeTag.isEmpty()){
			//GERMAN		
			if (nluInput.language.equals(LANGUAGES.DE)){
				if (NluTools.stringContains(timeTag, "nachmittag(s|)|abend(s|)|nacht(s|)|spaet")){
					isPM = true;
				}
			
			//OTHER
			}else{
				if (NluTools.stringContains(timeTag, "p(\\.|)m(\\.|)|afternoon|evening|night|late") || NluTools.stringContains(clockTag, "p(\\.|)m(\\.|)")){
					isPM = true;
				}
			}
		}else{
			//ENGLISH
			if (nluInput.language.equals(LANGUAGES.EN)){
				if (NluTools.stringContains(clockTag, "(\\d|\\s)(p(\\.|)m(\\.|))")){
					isPM = true;
				}
			}
		}
		
		int hh = 0;	int mm = 0;
		if (clockTag.contains(":")){
			String[] splitClock = clockTag.split(":");
			hh = Integer.parseInt(splitClock[0].replaceAll("\\D", "").trim());
			mm = Integer.parseInt(splitClock[1].replaceAll("\\D", "").trim());
		}else{
			hh = Integer.parseInt(clockTag.replaceAll("\\D", "").trim());
		}
		
		//Is post midday :-)
		if (isPM && hh<=12){
			hh = hh+12;
		}
		
		return (baseDate + "_" + ((hh<10)? ("0"+hh) : hh) + ":" + ((mm<10)? ("0"+mm) : mm) + ":00");
	}
	
	/**
	 * Convert a date-tag to a real date in the desired format.
	 * @param dayTag - things like "monday", "tomorrow" or "weekend"
	 * @param format - e.g. yyyy.MM.dd
	 * @param nluInput
	 * @return Date as string or empty
	 */
	private static String getDateToDay(String dayTag, String format, NluInput nluInput){
		if (dayTag.isEmpty()){
			return "";
		}
		//GERMAN
		if (nluInput.language.equals(LANGUAGES.DE)){
			//Relative and absolute days
			if (NluTools.stringContains(dayTag, DAY_TAGS_DE + "|" + DAY_TAGS_RELATIVE_DE)){
				//today
				if (NluTools.stringContains(dayTag, "heute|jetzt")){
					return DateTimeConverters.getToday(format, nluInput);
				//day after tomorrow 
				}else if (NluTools.stringContains(dayTag, "uebermorgen")){
					return DateTimeConverters.getDayAfterTomorrow(format, nluInput);
				//tomorrow
				}else if (NluTools.stringContains(dayTag, "morgen")){
					return DateTimeConverters.getTomorrow(format, nluInput);
				}
				//monday to sunday and weekend
				int dow = 0;
				if (NluTools.stringContains(dayTag, "montag")){
					dow = Calendar.MONDAY;
				}else if (NluTools.stringContains(dayTag, "dienstag")){
					dow = Calendar.TUESDAY;
				}else if (NluTools.stringContains(dayTag, "mittwoch")){
					dow = Calendar.WEDNESDAY;
				}else if (NluTools.stringContains(dayTag, "donnerstag")){
					dow = Calendar.THURSDAY;
				}else if (NluTools.stringContains(dayTag, "freitag")){
					dow = Calendar.FRIDAY;
				}else if (NluTools.stringContains(dayTag, "samstag|wochenende")){
					dow = Calendar.SATURDAY;
				}else if (NluTools.stringContains(dayTag, "sonntag")){
					dow = Calendar.SUNDAY;
				
				}else{
					//TODO: support more unspecific dates
					return "";
				}
				return DateTimeConverters.getDateForDayOfWeek(dow, format, nluInput);
			}
		
		//OTHERS
		}else{
			//Relative and absolute days
			if (NluTools.stringContains(dayTag, DAY_TAGS_EN + "|" + DAY_TAGS_RELATIVE_EN)){
				//today tomorrow etc.
				if (NluTools.stringContains(dayTag, "today|now")){
					return DateTimeConverters.getToday(format, nluInput);
				//day after tomorrow 
				}else if (NluTools.stringContains(dayTag, "day after tomorrow")){
					return DateTimeConverters.getDayAfterTomorrow(format, nluInput);
				//tomorrow
				}else if (NluTools.stringContains(dayTag, "tomorrow")){
					return DateTimeConverters.getTomorrow(format, nluInput);
				}
				//monday to sunday and weekend
				int dow = 0;
				if (NluTools.stringContains(dayTag, "monday")){
					dow = Calendar.MONDAY;
				}else if (NluTools.stringContains(dayTag, "tuesday")){
					dow = Calendar.TUESDAY;
				}else if (NluTools.stringContains(dayTag, "wednesday")){
					dow = Calendar.WEDNESDAY;
				}else if (NluTools.stringContains(dayTag, "thursday")){
					dow = Calendar.THURSDAY;
				}else if (NluTools.stringContains(dayTag, "friday")){
					dow = Calendar.FRIDAY;
				}else if (NluTools.stringContains(dayTag, "saturday|weekend")){
					dow = Calendar.SATURDAY;
				}else if (NluTools.stringContains(dayTag, "sunday")){
					dow = Calendar.SUNDAY;
				
				}else{
					//TODO: support more unspecific dates
					return "";
				}
				return DateTimeConverters.getDateForDayOfWeek(dow, format, nluInput);
			}
		}
		return "";
	}

}
