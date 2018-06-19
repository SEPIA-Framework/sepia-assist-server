package net.b07z.sepia.server.assist.events;

import java.util.HashMap;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Localized labels for event buttons and actions. 
 * 
 * @author Florian Quirin
 *
 */
public class EventLabels {
	
	public static enum Constants{
		duration_home,
		duration_work,
		lunch,
		recipe_of_the_day,
		brunch,
		bundesliga_results,
		news_common,
		radio, radio_chill, radio_night,
		tv_program, tv_series,
		cinema_news
	}
	
	private static HashMap<Constants, String> de = new HashMap<>();
	private static HashMap<Constants, String> en = new HashMap<>();
	static{
		de.put(Constants.duration_home, "Weg nach Hause");
		de.put(Constants.duration_work, "Weg zur Arbeit");
		de.put(Constants.lunch, "Mittagessen");
		de.put(Constants.recipe_of_the_day, "Rezept des Tages");
		de.put(Constants.brunch, "Brunchen");
		de.put(Constants.bundesliga_results, "Bundesliga");
		de.put(Constants.news_common, "Nachrichten des Tages");
		de.put(Constants.radio, "Radio");
		de.put(Constants.radio_chill, "Chill Radio");
		de.put(Constants.radio_night, "Night Radio");
		de.put(Constants.tv_program, "Tv Programm");
		de.put(Constants.tv_series, "Tv Serien");
		de.put(Constants.cinema_news, "Neu im Kino");
		
		en.put(Constants.duration_home, "Way home");
		en.put(Constants.duration_work, "Way to work");
		en.put(Constants.lunch, "Lunch");
		en.put(Constants.recipe_of_the_day, "Recipe of the day");
		en.put(Constants.brunch, "Brunch");
		en.put(Constants.bundesliga_results, "Bundesliga");
		en.put(Constants.news_common, "News of the day");
		en.put(Constants.radio, "Radio");
		en.put(Constants.radio_chill, "Chill-out radio");
		en.put(Constants.radio_night, "Night radio");
		en.put(Constants.tv_program, "Tv program");
		en.put(Constants.tv_series, "Tv series");
		en.put(Constants.cinema_news, "Cinema news");
	}
	
	/**
	 * Get button/action label or null.
	 * @param label - label as defined by Constants
	 * @param language - ISO language code
	 * @return label or null
	 */
	public static String getLabel(Constants label, String language){
		String localLabel = "";
		if (language.equals(LANGUAGES.DE)){
			localLabel = de.get(label);
		}else if (language.equals(LANGUAGES.EN)){
			localLabel = en.get(label);
		}
		if (localLabel == null || localLabel.isEmpty()){
			Debugger.println("EventLabels - missing local lable for: " + label.name(), 1);
			return null;
		}else{
			return localLabel;
		}
	}

}
