package net.b07z.sepia.server.assist.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import net.b07z.sepia.server.assist.answers.AnswerStatics;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.parameters.DateAndTime;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Tools to extract dates and times and to create them.
 * 
 * @author Florian Quirin
 *
 */
public class DateTimeConverters {
	
	public static long DAY_MS = (1000l * 60l * 60l * 24l);
	public static long HOUR_MS = (1000l * 60l * 60l);
	public static long MINUTE_MS = (1000l * 60l);
	public static long SECOND_MS = (1000l);
	
	//Note: some base-methods for dates are in core-tools 'DateTime'
	
	/**
	 * Get the local calendar of the user to determine things like dayOfWeek etc..
	 * Returns null if the user_time_local is missing.
	 */
	public static Calendar getUserCalendar(NluInput input){
		String userTimeLocal = input.userTimeLocal;
		return getUserCalendar(userTimeLocal);
	}
	/**
	 * Get the local calendar of the user to determine things like dayOfWeek etc..
	 * Returns null if the user_time_local is null, empty or in wrong format.
	 */
	public static Calendar getUserCalendar(String userTimeLocal){
		if (userTimeLocal != null && !userTimeLocal.isEmpty()){
			//parse local date
			Date date;
			try {
				date = new SimpleDateFormat(Config.defaultSdf).parse(userTimeLocal);
			} catch (ParseException e) {
				Debugger.println("getUserCalendar() - failed to parse: " + userTimeLocal, 1);
				return null;
			}
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			return c;
		}else{
			return null;
		}
	}
	
	/**
	 * Convert a date string from one format to another. Make sure the input is actually given in the expected format or the
	 * result will be empty!
	 * @param date_string - string with date in expected format
	 * @param format_in - expected input format, e.g. "dd.MM.yyyy"
	 * @param format_out - desired output format
	 * @return date string in desired format or empty
	 */
	public static String convertDateFormat(String date_string, String format_in, String format_out){
		//parse input
		SimpleDateFormat sdf_in = new SimpleDateFormat(format_in);
		Date date;
		try {
			date = sdf_in.parse(date_string);
		} catch (ParseException e) {
			return "";
		}
		//make new one
		SimpleDateFormat sdf_out = new SimpleDateFormat(format_out);
		return sdf_out.format(date);
	}
	/**
	 * Convert input date (with given format) to ISO 8601 format: yyyy-MM-dd.
	 */
	public static String convertToIso8601(String date_string, String format_in){
		return convertDateFormat(date_string, format_in, "yyyy-MM-dd");
	}
	/**
	 * Convert a given date to a language dependent date that can be spoken well by TTS.
	 * @param dateString - date to convert
	 * @param formatIn - format of date to convert
	 * @param language - language code of target language
	 */
	public static String getSpeakableDate(String dateString, String formatIn, String language){
		if (language.equals(LANGUAGES.EN)){
			return convertDateFormat(dateString, formatIn, "MM/dd/yyyy");
		}else{
			return convertDateFormat(dateString, formatIn, "dd.MM.yyyy");
		}
	}
	/**
	 * Convert a given date to a language dependent date that can be spoken well by TTS.<br>
	 * This special version also takes distances into account like, if its <7 days in the future call it by names like 'Monday' etc.
	 * @param targetDate - target date to convert
	 * @param addedDays - days added to the current user date to get to target date (usually taken from DIFF field in TIME parameter). Note: you might need to correct "addedDays" by "getIntuitiveDaysDifference(..)" before using this!
	 * @param formatIn - format of date to convert
	 * @param nluInput - for language and maybe user local time
	 */
	public static String getSpeakableDateSpecial(String targetDate, long addedDays, String formatIn, NluInput nluInput){
		return getSpeakableDateSpecial(targetDate, addedDays, formatIn, nluInput, true);
	}
	/**
	 * Convert a given date to a language dependent date that can be spoken well by TTS.<br>
	 * This special version also takes distances into account like, if its <7 days in the future call it by names like 'Monday' etc.
	 * @param targetDate - target date to convert
	 * @param addedDays - days added to the current user date to get to target date (usually taken from DIFF field in TIME parameter). Note: you might need to correct "addedDays" by "getIntuitiveDaysDifference(..)" before using this!
	 * @param formatIn - format of date to convert
	 * @param nluInput - for language and maybe user local time
	 * @param useRelativeNames - use things like "tomorrow"? If you store names in the UI you might want to avoid relative names ...
	 */
	public static String getSpeakableDateSpecial(String targetDate, long addedDays, String formatIn, NluInput nluInput, boolean useRelativeNames){
		String language = nluInput.language;
		
		if (useRelativeNames && addedDays == 0){
			//today
			return DateAndTime.getDay("+0", language); 
		}else if (useRelativeNames && addedDays == 1){
			//tomorrow
			return DateAndTime.getDay("+1", language);
		}else if (useRelativeNames && addedDays == 2){
			//the day after tomorrow
			return DateAndTime.getDay("+2", language);
		
		//within this week
		}else if (addedDays < 7){
			//parse input date
			SimpleDateFormat def_sdf = new SimpleDateFormat(formatIn);
			Date date;
			try {
				date = def_sdf.parse(targetDate);
				Calendar c = Calendar.getInstance();
				c.setTime(date);
				return DateAndTime.getDay(String.valueOf(c.get(Calendar.DAY_OF_WEEK)), language);
				
			} catch (ParseException e) {
				//fall back to conventional date on error
				Debugger.println("speakableDateSpecial() - failed to parse: " + targetDate, 1);
				return getSpeakableDate(targetDate, formatIn, language);
			}
			
		//else just get a nice speakable date
		}else{
			return getSpeakableDate(targetDate, formatIn, language);
		}
	}
	/**
	 * Get a speakable string built out of the durations given, like "1 hour and 54 minutes" 
	 * @param language - ISO code
	 * @param dd - days
	 * @param hh - hours
	 * @param mm - minutes
	 * @param ss - seconds
	 */
	public static String getSpeakableDuration(String language, long dd, long hh, long mm, long ss){
		String fullDuration = "";
		if (dd == 1){
			fullDuration += ("1 " + DateAndTime.getTimeNames("<day>", language) + ", ");
		}else if (dd > 0){
			fullDuration += (dd + " " + DateAndTime.getTimeNames("<days>", language) + ", ");
		}
		if (hh == 1){
			fullDuration += ("1 " + DateAndTime.getTimeNames("<hour>", language) + ", ");
		}else if (hh > 0){
			fullDuration += (hh + " " + DateAndTime.getTimeNames("<hours>", language) + ", ");
		}
		if (mm == 1){
			fullDuration += ("1 " + DateAndTime.getTimeNames("<minute>", language) + ", ");
		}else if (mm > 0){
			fullDuration += (mm + " " + DateAndTime.getTimeNames("<minutes>", language) + ", ");
		}
		if (ss == 1){
			fullDuration += ("1 " + DateAndTime.getTimeNames("<second>", language) + ", ");
		}else if (ss > 0){
			fullDuration += (ss + " " + DateAndTime.getTimeNames("<seconds>", language) + ", ");
		}
		fullDuration = fullDuration.replaceFirst(", $", "").trim();
		fullDuration = fullDuration.replaceFirst("(.*)(, )(.*?$)", "$1 " + AnswerStatics.get(AnswerStatics.AND, language) + " $3").trim();
		return fullDuration;
	}
	
	/**
	 * Convert a time (usually in 'HH:mm:ss' format) to a speakable time like 6:00 PM. or 18:00.
	 * @param timeIn - string with time in given format, usually 'HH:mm:ss'
	 * @param formatIn - usually 'HH:mm:ss'
	 * @param language - ISO language code, e.g. 'en'
	 * @return
	 */
	public static String getSpeakableTime(String timeIn, String formatIn, String language){
		if (language.equals(LANGUAGES.EN)){
			return convertDateFormat(timeIn, formatIn, "h:mm a");
		}else{
			if (formatIn.equals("HH:mm:ss")){
				return timeIn.replaceFirst(":\\d\\d$", "").trim();
			}else{
				return timeIn;
			}
		}
	}
	
	/**
	 * Get the user's today-date in the desired format. If users system time is not available it returns an empty string.
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param nlu_input - NLU_Input containing user_time_local
	 * @return todays date at user location as string or empty string (if client does not submit it)
	 */
	public static String getToday(String format, NluInput nlu_input){
		String user_time = nlu_input.userTimeLocal;
		return getToday(format, user_time);
	}	
	/**
	 * Get today by parsing user local time string (defined by Config.default_sdf)
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param user_time_local - submitted by client, should be: 2016.12.31_22:44:11
	 * @return todays date at user location as string or empty string (if client does not submit it)
	 */
	public static String getToday(String format, String user_time_local){
		if (user_time_local != null && !user_time_local.isEmpty()){
			//parse local date
			SimpleDateFormat def_sdf = new SimpleDateFormat(Config.defaultSdf);
			Date date;
			try {
				date = def_sdf.parse(user_time_local);
			} catch (ParseException e) {
				return "";
			}
			//make new one
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			String today = sdf.format(date);
			return today;
		}else{
			return "";
		}
	}
	/**
	 * Get today plus x minutes by parsing user local time string (defined by Config.default_sdf) and adding minutes.
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param nlu_input - NLU_Input containing user_time_local
	 * @param X - add this many minutes
	 * @return todays date + x at user location as string or empty string (if client does not submit it)
	 */
	public static String getTodayPlusX_minutes(String format, NluInput nlu_input, long X){
		String user_time_local = nlu_input.userTimeLocal;
		if (user_time_local != null && !user_time_local.isEmpty()){
			return getDatePlusX_minutes(format, user_time_local, X);
		}else{
			return "";
		}
	}
	/**
	 * Get today plus x seconds by parsing user local time string (defined by Config.default_sdf) and adding seconds.
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param nlu_input - NLU_Input containing user_time_local
	 * @param X - add this many minutes
	 * @return todays date + x at user location as string or empty string (if client does not submit it)
	 */
	public static String getTodayPlusX_seconds(String format, NluInput nlu_input, long X){
		String user_time_local = nlu_input.userTimeLocal;
		if (user_time_local != null && !user_time_local.isEmpty()){
			return getDatePlusX_seconds(format, user_time_local, X);
		}else{
			return "";
		}
	}
	/**
	 * Adding x minutes to a date by parsing a date_input string in default format (Config.default_sdf) and adding minutes.
	 * @param format - desired output e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param date_input - individual date in default format (Config.default_sdf)
	 * @param X - add this many minutes
	 * @return todays date + x at user location as string or empty string (if client does not submit it)
	 */
	public static String getDatePlusX_minutes(String format, String date_input, long X){
		long S = X * 60;
		return getDatePlusX_seconds(format, date_input, S);
	}
	/**
	 * Adding x seconds to a date by parsing a date_input string in default format (Config.default_sdf) and adding seconds.
	 * @param format - desired output e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param date_input - individual date in default format (Config.default_sdf)
	 * @param X - add this many seconds
	 * @return todays date + x at user location (local time) as string or empty string (if client does not submit it)
	 */
	public static String getDatePlusX_seconds(String format, String date_input, long X){
		if (date_input != null && !date_input.isEmpty()){
			//parse local date
			SimpleDateFormat def_sdf = new SimpleDateFormat(Config.defaultSdf);
			Date def_date, new_date;
			try {
				def_date = def_sdf.parse(date_input);
				new_date = new Date(def_date.getTime() + (X * 1000));
			} catch (ParseException e) {
				return "";
			}
			//make new one
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			String today_plus = sdf.format(new_date);
			return today_plus;
		}else{
			return "";
		}
	}
	
	/**
	 * Get the user's tomorrow-date in the desired format. If users system time is not available it returns an empty string.
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param nlu_input - NLU_Input containing user_time
	 * @return tomorrows date at user location as string
	 */
	public static String getTomorrow(String format, NluInput nlu_input){
		return getTodayPlusX_minutes(format, nlu_input, 1440);
	}	
	/**
	 * Get the user's day-after-tomorrow-date in the desired format. If users system time is not available it returns an empty string.
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param nlu_input - NLU_Input containing user_time
	 * @return day-after-tomorrows date at user location as string
	 */
	public static String getDayAfterTomorrow(String format, NluInput nlu_input){
		return getTodayPlusX_minutes(format, nlu_input, 2880);
	}
	
	/**
	 * Get the coming Saturday.
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param nluInput - NLU_Input containing user_time
	 * @return Saturday of the coming weekend
	 */
	public static String getWeekend(String format, NluInput nluInput){
		int dow = Calendar.SATURDAY;
		return getDateForDayOfWeek(dow, format, nluInput);
	}
	
	/**
	 * Get the date of a day of the week. If the day is equal to today or in the past it adds +7 for the week after.
	 * @param day - integer value of day of week as given by Calendar.DAY_OF_WEEK
	 * @param format - e.g.: "dd.MM.yyyy" or "HH:mm:ss" or "MM/dd/yy"
	 * @param nluInput - NluInput containing user_time 
	 * @return date as string
	 */
	public static String getDateForDayOfWeek(int day, String format, NluInput nluInput){
		Calendar c = getUserCalendar(nluInput);
		if (c != null){
			int currentDay = c.get(Calendar.DAY_OF_WEEK);
			int daysInFuture = day - currentDay;
			if (daysInFuture < 1) daysInFuture += 7;
			//make new one
			String targetDate = getTodayPlusX_minutes(format, nluInput, daysInFuture*24*60);
			//System.out.println("TODAY IS " + currentDay + " (" + getToday(Config.defaultSdf, nluInput) + ")");
			//System.out.println("TARGET IS " + day + " (" + targetDate + ")");
			return targetDate;
		}else{
			return "";
		}
	}
	
	/**
	 * Get the UNIX time (ms) of the given GMT date in the given format.
	 * @param dateString - any string containing a date that can be parsed
	 * @param format - format to parse
	 * @return UNIX time or Long.MIN_VALUE
	 */
	public static long getUnixTimeOfDateGMT(String dateString, String format){
		//parse input
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date;
		try {
			date = sdf.parse(dateString);
			return date.getTime();
						
		} catch (ParseException e) {
			return Long.MIN_VALUE;
		}
	}
	/**
	 * Get the UNIX time (ms) of the given date in the given format and timezone.
	 * @param dateString - any string containing a date that can be parsed
	 * @param format - format to parse
	 * @param timezone - {@link ZoneId} of TimeZone, either an abbreviation such as "PST", a full name such as "America/Los_Angeles", or a custom ID such as "GMT-8:00". 
	 * @return UNIX time or Long.MIN_VALUE
	 */
	public static long getUnixTimeOfDateAtZone(String dateString, String format, String timezone){
		//parse input
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of(timezone)));
		Date date;
		try {
			date = sdf.parse(dateString);
			return date.getTime();
						
		} catch (ParseException e) {
			return Long.MIN_VALUE;
		}
	}
	
	/**
	 * Calculate the difference of two dates in default string format (Config.default_sdf). Result is a HashMap with
	 * "dd", "hh", "mm", "ss", "ms", "total_ms" (from days to milliseconds). Second date is the reference. 
	 * @returns HashMap or null of parsing fails 
	 */
	public static HashMap<String, Long> dateDifference(String default_date1, String default_date2){
		//parse input
		SimpleDateFormat sdf = new SimpleDateFormat(Config.defaultSdf);
		Date date1, date2;
		try {
			date1 = sdf.parse(default_date1);
			date2 = sdf.parse(default_date2);
			return dateDifference(date1, date2);
			
		} catch (ParseException e) {
			return null;
		}
	}
	/**
	 * Calculate the difference of two dates. Result is a HashMap with
	 * "dd", "hh", "mm", "ss", "ms", "total_ms" (from days to milliseconds). Second date is the reference.  
	 */
	public static HashMap<String, Long> dateDifference(Date date1, Date date2){
		HashMap<String, Long> diffMap = new HashMap<>();
		long d1 = date1.getTime();
		long d2 = date2.getTime();
		//long diff = Math.abs(d2 - d1);
		long diff = (d2 - d1);
		
		long dd = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
		diff = diff - (dd * 24 * 60 * 60 * 1000);
		long hh = TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS);
		diff = diff - (hh * 60 * 60 * 1000);
		long mm = TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS);
		diff = diff - (mm * 60 * 1000);
		long ss = TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS);
		long ms = diff - (ss * 1000);
		
		diffMap.put("dd", dd);
		diffMap.put("hh", hh);
		diffMap.put("mm", mm);
		diffMap.put("ss", ss);
		diffMap.put("ms", ms);
		diffMap.put("total_ms", dd*24*60*60*1000 + hh*60*60*1000 + mm*60*1000 + ss*1000 + ms);
		return diffMap;
	}
	/**
	 * Calculate the difference of two dates given as UNIX times. Result is a HashMap with
	 * "dd", "hh", "mm", "ss", "ms", "total_ms" (from days to milliseconds). Second date is the reference.  
	 */
	public static HashMap<String, Long> dateDifference(long unixTime1, long unixTime2){
		HashMap<String, Long> diffMap = new HashMap<>();
		
		long diff = unixTime2 - unixTime1;
		diffMap.put("total_ms", diff);
		long dd = (long) Math.floor(diff / (double) DAY_MS);	diff = diff - (dd * DAY_MS);
		diffMap.put("dd", dd);
		long hh = (long) Math.floor(diff / (double) HOUR_MS);	diff = diff - (hh * HOUR_MS);
		diffMap.put("hh", hh);
		long mm = (long) Math.floor(diff / (double) MINUTE_MS);	diff = diff - (mm * MINUTE_MS);
		diffMap.put("mm", mm);
		long ss = (long) Math.floor(diff / (double) SECOND_MS);	diff = diff - (ss * SECOND_MS);
		diffMap.put("ss", ss);
		diffMap.put("ms", diff);
		
		return diffMap;
	}
	
	/**
	 * Calculate the difference of the user local time until midnight. Result is a HashMap with "hh", "mm", "ss", "total_s".
	 * @param userLocalTime - format HH:mm:ss
	 */
	public static HashMap<String, Long> getTimeUntilMidnight(String userLocalTime){
		String[] time = userLocalTime.split(":");
		long hh = 23l - Converters.obj2LongOrDefault(time[0], null);
		long mm = 59l - Converters.obj2LongOrDefault(time[1], null);
		long ss = 59l - Converters.obj2LongOrDefault(time[2], null);
		long totalSec = (hh * 60l * 60l) + (mm * 60) + (ss);
		
		HashMap<String, Long> diffMap = new HashMap<>();
		diffMap.put("hh", hh);
		diffMap.put("mm", mm);
		diffMap.put("ss", ss);
		diffMap.put("total_s", totalSec);
		return diffMap;
	}
	/**
	 * If you want to know how many days in the future an event lies it is more intuitive to say "1 day" even if its maybe only 6 hours. This methods calculates the difference to midnight
	 * and takes the difference in hours, minutes and seconds into account when you want to know if some event is actually "tomorrow" (+1) for example.  
	 * @param nluInput - mainly for local user time
	 * @param diffDays - days in the sense of 24h blocks
	 * @param diffHours - ... 60min blocks
	 * @param diffMinutes - ... 60s blocks
	 * @param diffSeconds - ... plain seconds
	 * @return the "intuitive" value when speaking of things like tomorrow (+1) etc. ...
	 */
	public static long getIntuitiveDaysDifference(NluInput nluInput, long diffDays, long diffHours, long diffMinutes, long diffSeconds){
		//check time-difference to next day
		HashMap<String, Long> diffToMidnight = DateTimeConverters.getTimeUntilMidnight(nluInput.userTimeLocal.split(Config.defaultSdfSeparatorRegex)[1]);
		long secToMidnight = diffToMidnight.get("total_s");
		long correctedDiffDays = diffDays;
		if ((diffHours * 60l * 60l + diffMinutes * 60l + diffSeconds) > secToMidnight){
			correctedDiffDays++;
		}
		return correctedDiffDays;
	}

}
