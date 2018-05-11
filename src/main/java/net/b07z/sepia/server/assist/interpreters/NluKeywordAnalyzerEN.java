package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.parameters.AbstractParameterSearch;
import net.b07z.sepia.server.assist.parameters.FashionBrand;
import net.b07z.sepia.server.assist.parameters.FashionItem;
import net.b07z.sepia.server.assist.parameters.FashionShopping;
import net.b07z.sepia.server.assist.parameters.FoodClass;
import net.b07z.sepia.server.assist.parameters.FoodItem;
import net.b07z.sepia.server.assist.parameters.Language;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * Simple (at least from the idea) yet effective keyword analyzer for English to interpret user input.<br>
 * The order of checks does matter, put commands with less priority at the bottom. If commands score the same probability the first is taken.<br>
 * Note: Never use this as a static interpreter! Always create new instances of it when needed (compared to the sentence matchers
 * that can be used globally).
 *   
 * @author Florian Quirin
 *
 */
public class NluKeywordAnalyzerEN implements NluInterface {

	double certainty_lvl = 0.0d;		//how certain is ILA about a result
	
	HashMap<String, String> locations;	//store locations in text
	HashMap<String, String> dates;		//store dates in text
	HashMap<String, String> numbers;		//store numbers in text
	HashMap<String, String> websearches;	//store web search in text
	HashMap<String, String> controls;		//store control parameters in text
	HashMap<String, String> music_parameters;		//store music parameters in text
	HashMap<String, String> vehicle_parameters;		//store vehicle search parameters
	HashMap<String, String> retail_parameters;		//store retail search parameters
	
	public NluResult interpret(NluInput input) {
		
		//get parameters from input
		String text = input.text;
		String language = input.language;
				
		//normalize text, e.g.:
		// all lowerCase - remove all ',!? - handle ä ö ü ß ... trim
		Normalizer normalizer = Config.inputNormalizers.get(language);
		if (normalizer != null){
			text = normalizer.normalize_text(text);
			input.text = text; 				//TODO: is this ok here? Do it before?
		}
		
		//first rough check for main keywords
		ArrayList<String> possibleCMDs = new ArrayList<String>();		//make a list of possible interpretations of the text
		ArrayList<HashMap<String, String>> possibleParameters = new ArrayList<HashMap<String, String>>();		//possible parameters
		ArrayList<Integer> possibleScore = new ArrayList<Integer>();	//make scores to decide which one is correct command
		int index = -1;
		
		//track parameters
		//HashMap<String, String> checkedPrameters = new HashMap<String, String>();
		
		//some small-talk stuff
		if (NluTools.stringContains(text, "^" + Pattern.quote(Config.assistantName.toLowerCase()) + "$")){
			//String this_text = text;
			possibleCMDs.add(CMD.CHAT);
			possibleScore.add(1);	index++;
			//type
			String type = "greeting";
			possibleScore.set(index, possibleScore.get(index)+1);
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.TYPE, type);
			possibleParameters.add(pv);
		}
		
		//news
		if (NluTools.stringContains(text, "news|whats new|whats up|whats going on|headline|headlines|"
				+ "results|result|score|scores|baseball|hockey|basketball|football|tennis|golf|soccer|"
				+ "did .* (play|score|win|winning|lost|losing)|^(baseball|football|soccer)(game|)\\b|"
				+ "when .* (play|playing)|"
				+ "bundesliga|champions( |-|)league|euro( |-|)league|europa( |-|)league|premier league|primera division|la liga|"
				+ "(serie|seria|series) a|eredivisie|ligue 1|primeira liga|sueperlig|sueper lig(a|)|(fa|dfb)(-| )(pokal|cup)"
				+ "")){
			//String this_text = text;
			possibleCMDs.add(CMD.NEWS);
			possibleScore.add(1);	index++;
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.NEWS_SECTION, PARAMETERS.NEWS_TYPE, PARAMETERS.SPORTS_TEAM, PARAMETERS.SPORTS_LEAGUE)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//weather
		if (NluTools.stringContains(text, "weather|temperature|rain|raining|snow|snowing|be sunny|umbrella|sunscreen|"
				+ "how (hot|cold|warm) (is|will)|degree(s|)")){

			possibleCMDs.add(CMD.WEATHER);
			possibleScore.add(1);	index++;

			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.TIME, PARAMETERS.PLACE)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//directions
		if (NluTools.stringContains(text, "directions|navigation|show me the way|navigate me|(?<!cheapest )way |"
							+ "(bring|get|take) me (?!(a|the)\\b)|"
							+ "(how do i get (to|home)\\b)|"
							+ "(show|find) .* (from|to) .* (map|maps)\\b|"
							+ "\\b(from) .* (to) |"
							+ "how far |how long |distance |duration .*\\b(to)|to go to |to drive to |to travel to ")){
			//String this_text = text;
			possibleCMDs.add(CMD.DIRECTIONS);
			possibleScore.add(1);	index++;
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.TIME, PARAMETERS.TRAVEL_TYPE, PARAMETERS.TRAVEL_REQUEST_INFO, 
							PARAMETERS.LOCATION_END, PARAMETERS.LOCATION_WAYPOINT, PARAMETERS.LOCATION_START)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//fashion shopping
		if (NluTools.stringContains(text, "(buy|fashion|shopping)") || 
				((NluTools.stringContains(text, FashionItem.fashionItems_en) || NluTools.stringContains(text, FashionBrand.fashionBrandsSearch)) 
					&& !NluTools.stringContains(text, "(i have |there are |my )"))
			){
			possibleCMDs.add(CMD.FASHION);
			possibleScore.add(1);	index++;
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			FashionShopping fs = new FashionShopping().setup(input, pv);
			fs.getParameters();
			possibleScore.set(index, possibleScore.get(index) + fs.getScore());
			possibleParameters.add(pv);
		}
		
		//food
		if (NluTools.stringContains(text, "^food$|"
				+ "(food|foodstuff|meal|breakfast|lunch|dinner|groceries|nourishments) .*\\b(order|buy|deliver|eat)|"
				+ "(" + Language.languageClasses_en + "|" + FoodClass.foodClasses_en + ") .*\\b(order|buy|deliver|eat)|"
				+ "(order|buy|deliver|eat|what) .*\\b(food|foodstuff|meal|breakfast|lunch|dinner|groceries|nourishments|eat)|"
				+ "(order|buy|deliver|eat) .*\\b(" + Language.languageClasses_en + "|" + FoodClass.foodClasses_en + ")|"
				+ "hunger|hungry|appetite|delivery|"
				+ "(from) .*\\b(\\w*restaurant(s|))") 
				||	NluTools.stringContains(text, FoodItem.foodItems_en)
			){
			possibleCMDs.add(CMD.FOOD);
			possibleScore.add(1);	index++;
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.FOOD_ITEM, PARAMETERS.FOOD_CLASS)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//wikipedia/knowledgebase
		if (NluTools.stringContains(text, "wiki|wikipedia|information|informations|"
							+ "who (is|are|was|were) .*|"
							+ "meaning of .*|what (is|was|are|were) .*|"
							+ "when (is|was|are|were) .*|"
							+ "do you know .*|tell me .* about|define|"
							+ "how (high|large) (is|are|was|were) .*|how old (is|are|was|were) .*|how many .* (are|has|were|had|live|lived) ")){
			String this_text = text;
			possibleCMDs.add(CMD.KNOWLEDGEBASE);
			possibleScore.add(1);	index++;
			//kb search term
			String kb_search="";
			if (NluTools.stringContains(this_text, "how (high|large) (is|are|was|were)|how old (is|are|was|were)|how many .* (are|has|were|had|live|lived) ")){
				kb_search = this_text.replaceFirst(".*\\b(how (high|large)|how old|how many)\\b", "$1").trim();
			}else{
				kb_search=this_text.replaceFirst(".*?\\b(search for|search|who|what|where|when|information on|about|do you know)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(^the meaning of|^who|^where|^what|^when)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(^is|^was|^were|^are|^about|^on)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(^a|^an|^the|^wiki|^wikipedia)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(is$|was$|were$|are$|on wikipedia$|in wikipedia$)", "").trim();
			}
			//recover original
			kb_search = normalizer.reconstructPhrase(input.text_raw, kb_search);
			//score
			if (!kb_search.matches("")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//kb additional query (time, place, person, thing)
			String kb_add_info="";
			if (NluTools.stringContains(this_text, "when|time")){
				kb_add_info = "time";
			}else if (NluTools.stringContains(this_text, "who|person")){
				kb_add_info = "person";
			}else if (NluTools.stringContains(this_text, "where|place")){
				kb_add_info = "place";
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, kb_search.trim());
				pv.put(PARAMETERS.TYPE, kb_add_info.trim());
			possibleParameters.add(pv);
		}
		
		//web search
		//TODO: optimize exceptions
		if (NluTools.stringContains(text, "websearch|web search|search the web|"
						+ "^google|^bing|^yahoo|^duck duck|^duck duck go|"
						+ "^(picture(s|)|recipe(s|)|video(s|)|movie(s|)|film(s|)|share(s|)|stock(s|)|book(s|))|"
						+ "what is the (stock|share) (value|price)|"
						+ "(search|find|show|look|searching|looking)( | .* )((on |)the (web|internet))|"
						+ "(search|find|show|look|searching|looking)( | .* )(with|on|via|per|over|by) (google|bing|duck duck go|duck duck|yahoo)|"
						+ "(search|find|show|look|searching|looking)( | .* )(picture(s|)|recipe(s|)|video(s|)|youtube|book(s|)|share(s|)|stock(s|))")
					//|| NLU_Tools.stringContains(text, "search|find|show|look for|searching for|looking for") 
					//|| (NLU_Tools.stringContains(text, "search|find|show") 
					//	&& !NLU_Tools.stringContains(text, "(search|find|show)\\b.*\\b(music|song(s|)|news|wikipedia|"
					//										+ "(action|fantasy|)movie(s|)|ticket(s|))"))
				){
			//String this_text = text;
			possibleCMDs.add(CMD.WEB_SEARCH);
			possibleScore.add(1);	index++;
			//score a bit extra if you made it till here ^^
			possibleScore.set(index, possibleScore.get(index)+1);
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.WEBSEARCH_ENGINE, PARAMETERS.WEBSEARCH_REQUEST, PARAMETERS.SEARCH_SECTION)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}		
		
		//mobility
		if (NluTools.stringContains(text, "(\\b)(the |)(fastest|best|cheapest|cheap) (connection|connections|way|ways)|"
						+ "(option|options) .*?\\b(between)\\b.*?\\b(and)|"
						+ "(option|options) .*?\\b(from)\\b.*?\\b(to)")){
			//String this_text = text;
			possibleCMDs.add(CMD.MOBILITY);
			possibleScore.add(1);	index++;
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.TIME, PARAMETERS.TRAVEL_TYPE, PARAMETERS.TRAVEL_REQUEST_INFO, 
							PARAMETERS.LOCATION_END, PARAMETERS.LOCATION_WAYPOINT, PARAMETERS.LOCATION_START)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//flights
		if (NluTools.stringContains(text, "(flights|flight) (from|to)|flightsearch|"
				+ "(search|searching|find|i need|show me|book|look|looking)\\b.* (flight|flights|plane|planes|fly|planeticket|flightticket)|"
				+ "(bring |show |way ).*\\b(plane)") 
				&& !NluTools.stringContains(text, "on (the|a) map")){
			String this_text = text;
			possibleCMDs.add(CMD.FLIGHTS);
			possibleScore.add(1);	index++;
			//double score for flight 'cause the initial filter is already pretty strict
			if (NluTools.stringContains(this_text, "flight|flights|flying|fly|plane|planes|planeticket|flightticket")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//analyze
			search_locations(this_text, language);
			//travel time
			String travel_time = locations.get("travel_time").trim();
			if (!travel_time.matches("")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//destination
			String travel_destination = locations.get("location_end").trim();
			if (travel_destination.isEmpty()){
				travel_destination = locations.get("location").trim();
				//filter some stupid results
				travel_destination = travel_destination.replaceAll("\\b(flight(s|)|plane(s|)|planeticket|flightticket)\\b", "");
				travel_destination = travel_destination.replaceAll("\\b^(a|the)\\b", "").trim();
			}
			if (!travel_destination.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//start
			String travel_start = locations.get("location_start").trim();			
			if (!travel_start.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}else{
				travel_start = "<user_location>";	//default is user location
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.LOCATION_START, travel_start);
				pv.put(PARAMETERS.LOCATION_END, travel_destination);
				pv.put(PARAMETERS.TIME, travel_time);
			possibleParameters.add(pv);
		}
		
		//timer
		if (NluTools.stringContains(text, "(\\w+(-)|)timer(s|)|(\\w+(-)|)counter(s|)|(\\w+(-)|)countdown(s|)|count down|stop watch(es|)|(\\w+(-)|)stopwatch(es|)|"
								+ "(\\w+(-)|)alarm(s|)|wake me|out of bed|get up at|"
								+ "(\\w+(-)|)reminder(s|)|remind (me|us)|"
								+ "(\\w+(-)|)appointment(s|)|calendar(s|)")){
			possibleCMDs.add(CMD.TIMER);
			possibleScore.add(1);	index++;
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.ACTION, PARAMETERS.TIME, PARAMETERS.CLOCK, PARAMETERS.ALARM_TYPE, PARAMETERS.ALARM_NAME)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//hotel search
		if (NluTools.stringContains(text, "hotel|hotels") && !NluTools.stringContains(text, "where(?! i|we|one)|map|maps")){
			String this_text = text;
			possibleCMDs.add(CMD.HOTELS);
			possibleScore.add(1);	index++;
			//analyze
			search_locations(this_text, language);
			//place to search
			String place = locations.get("location").trim();			//String poi = locations.get("poi");
			//clean up place
			place = place.replaceFirst("(.*)( for )(.*)", "$1").trim();
			//time to search
			String time_start = locations.get("travel_time").trim();
			String time_end = locations.get("travel_time_end").trim();
			//empty?
			if (!place.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			if (!time_start.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			if (!time_end.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.PLACE, place);
				pv.put(PARAMETERS.TIME, time_start);
				pv.put(PARAMETERS.TIME_END, time_end);
			possibleParameters.add(pv);
		}

		//radio
		if (NluTools.stringContains(text, "radio|radiochannel|radiostation|radiostream|music station|music channel") 
										&& !possibleCMDs.contains(CMD.KNOWLEDGEBASE)){
			//String this_text = text;
			possibleCMDs.add(CMD.MUSIC_RADIO);
			possibleScore.add(1);	index++;
			
			//it is so obvious, score again
			possibleScore.set(index, possibleScore.get(index)+1);
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.RADIO_STATION, PARAMETERS.MUSIC_GENRE, 
										PARAMETERS.ACTION)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//music
		if (NluTools.stringContains(text, "music|play .*|song|songs") 
							&& !possibleCMDs.contains(CMD.KNOWLEDGEBASE)){
			String this_text = text;
			possibleCMDs.add(CMD.MUSIC);
			possibleScore.add(1);	index++;
			
			search_music_parameters(this_text, language);
			
			//genre
			String genre = music_parameters.get("music_genre");
			if (!genre.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//artist
			String artist = music_parameters.get("music_artist");
			if (!artist.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			String title = music_parameters.get("music_search");
			//a "startable" is to vague to trigger a score increase ...
			//if (!title.matches("")){
			//	possibleScore.set(index, possibleScore.get(index)+1);
			//}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SONG, title);
				pv.put(PARAMETERS.MUSIC_GENRE, genre);
				pv.put(PARAMETERS.MUSIC_ARTIST, artist);
			possibleParameters.add(pv);
		}
		
		//movies
		if (NluTools.stringContains(text, "(action|fantasy|)(movie(s|))") 
							&& !NluTools.stringContains(text, "websearch|web search|search online|google|bing")){
			String this_text = text;
			possibleCMDs.add(CMD.MOVIES);
			possibleScore.add(1);	index++;
			//genre
			String movie_type = "";
			movie_type = RegexParameterSearch.get_movie_genre(this_text, language);
			if (!movie_type.matches("")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//info (actor, director?)
			String info = "";
			if (this_text.matches(".*(movie |movies )(from .*|of .*|by .*)")){
				info = "director";
			}else if (this_text.matches(".*(movie |movies )(with .*)")){
				info = "actor";
			}else if (this_text.matches(".*(movie |movies )(named .*|called .*)")){
				info = "title";
			}
			//title
			String movie = "";
			if (this_text.matches(".*(movie |movies )(named .*|called .*|from .*|of .*|by .*|with .*)")){
				movie = this_text.replaceFirst(".*?(movie |movies )(named |called| from |of |by |with )", "").trim();
				possibleScore.set(index, possibleScore.get(index)+1);
			}else{
				if (this_text.matches(".*(movie |movies ).*"))
					movie = this_text.replaceFirst(".*?(movie |movies )", "").trim();
				else {
					movie = this_text.replaceFirst("\\b(action|)(movies$|movie$)", "").trim();		//TODO: this is problematic for titles like scary movie!
					movie = movie.replaceFirst("^(show me |show |find me |find |search for |search )", "").trim();
				}
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, movie);
				pv.put(PARAMETERS.TYPE, movie_type);
				pv.put(PARAMETERS.INFO, info);
			possibleParameters.add(pv);
		}
		
		//event tickets
		if (NluTools.stringContains(text, "ticket|tickets|entrance card|admission card|voucher")){
			String this_text = text;
			possibleCMDs.add(CMD.TICKETS);
			possibleScore.add(1);	index++;
			//type
			String ticket_type = RegexParameterSearch.get_event_type(this_text, language);
			//search event/movie/etc. ...
			String search = "";
			if (this_text.matches(".*\\b(for ).*")){
				search = this_text.replaceFirst(".*\\b(for )", "").trim();
				search = search.replaceFirst("^(a|an|the)\\b", "").trim();
				search = search.replaceFirst("\\b(movie|concert)$\\b", "").trim();
				//search = search.replaceFirst("\\b(buy|order|get|search)\\b.*", "").trim();
				
				//is the search actually the type?
				if (!ticket_type.isEmpty() && RegexParameterSearch.get_event_type(search, language).equals(ticket_type)){
					search = "";
				}
			}
			if (!search.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
			pv.put(PARAMETERS.SEARCH, search);
			pv.put(PARAMETERS.TYPE, ticket_type);
			possibleParameters.add(pv);
		}
		
		//dictionary
		if (NluTools.stringContains(text, "dictionary|word (search|find|look)|(search|find|look) .*\\b(word)|translation|"
							+ "translate|"
							+ ".* (into|in|to) (english|german|turkish|french|spanish)")){
			String this_text = text;
			possibleCMDs.add(CMD.DICT_TRANSLATE);
			possibleScore.add(1);	index++;
			
			//TODO: make real parameter out of that
			
			//dictionary target language
			String target_lang="";
			if (NluTools.stringContains(this_text, "(into|to|in) german")){
				target_lang = "en";
			}else if (NluTools.stringContains(this_text, "(into|to|in) spanish")){
				target_lang = "es";
			}else if (NluTools.stringContains(this_text, "(into|to|in) turkish")){
				target_lang = "tr";
			}else if (NluTools.stringContains(this_text, "(into|to|in) french")){
				target_lang = "fr";
			}else if (NluTools.stringContains(this_text, "(into|to|in) english")){
				target_lang = "en";
			}
			if (!target_lang.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//remove language tag
			this_text = this_text.replaceFirst("(into|to|in) (german|spanish|"
					+ "turkish|french|english)", "").trim();
			
			//dictionary search term
			String dict_search=this_text.replaceFirst(".*?\\b(dictionary for)\\s", "").trim();
			dict_search=dict_search.replaceFirst(".*?\\b(translation of|translation for|translate)\\s", "").trim();
			dict_search=dict_search.replaceFirst(".*?\\b(search for|search)\\s", "").trim();
			dict_search=dict_search.replaceFirst("\\b(^can you|^what is|^show me|^what means)\\b", "").trim();
			dict_search=dict_search.replaceFirst("\\b(^the|^a)\\b", "").trim();
			dict_search=dict_search.replaceFirst("\\b(from the dictionary$|in the dictionary$|in dictionary$)", "").trim();
			if (!dict_search.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
				//let it score but remove the false search
				if (dict_search.matches("word|words|translation")){
					dict_search = "";
				}
			}
			//recover original
			dict_search = normalizer.reconstructPhrase(input.text_raw, dict_search);
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, dict_search.trim());
				pv.put(PARAMETERS.LANGUAGE, target_lang.trim());
			possibleParameters.add(pv);
		}
		
		//Lists
		if (NluTools.stringContains(text, "list(s|)|to(-| |)do(-\\w+|)|shoppinglist(s|)|note(s|)|(need|have) to buy")){
			//String this_text = text;
			possibleCMDs.add(CMD.LISTS);
			possibleScore.add(1);	index++;
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.ACTION,
							PARAMETERS.LIST_TYPE, PARAMETERS.LIST_SUBTYPE, PARAMETERS.LIST_ITEM)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//TV Program
		if (NluTools.stringContains(text, "(tv|television) program|(whats|what is) (on the|on) (tv|telly)")){
			String this_text = text;
			possibleCMDs.add(CMD.TV_PROGRAM);
			possibleScore.add(1);	index++;
			//tv time
			search_dates(this_text, language);
			String tv_time = dates.get("date_tag");
			if (!tv_time.isEmpty()){
				this_text = RegexParameterSearch.remove_date(this_text, tv_time, language);
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//channel
			String channel = NluTools.stringFindFirst(this_text, "hbo|amc|abc");
			if (!channel.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.TIME, tv_time.trim());
				pv.put(PARAMETERS.CHANNEL, channel.trim());
			possibleParameters.add(pv);
		}
		
		//My favorites
		if (NluTools.stringContains(text, "(my favorite|my personal|my private) .* (is|are)|"
				+ "(what (is|are)|do you know|delete|forget|remove) my (favorite|private|personal)|"
				+ "(open|show me|show|what (is|are)|do you know) my (favorites|info|favorite (stuff|things))")){
			String this_text = text;
			possibleCMDs.add(CMD.MY_FAVORITE);
			possibleScore.add(1);	index++;
			//item
			String info = "";
			String item = RegexParameterSearch.get_my_info_item(this_text, language);
			//item = this_text.replaceFirst(".*?\\b(my personal|my private|my favorites|my info|my)\\b", "");
			//item = item.replaceFirst("\\b(is|are)\\b.*", "").trim();
			if (!item.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
				//info
				info = this_text.replaceFirst(".*?\\b(" + item + ")\\b", "");
				info = RegexParameterSearch.get_my_info(info, language);
				//info = info.replaceFirst(".*?\\b(is|are)\\b", "").trim();
				//info = info.replaceFirst("\\b^(a|an)\\s", "").trim();
				if (!info.isEmpty()){
					possibleScore.set(index, possibleScore.get(index)+1);
				}
				//clean up some items
				item = item.replaceFirst("^(favorites|info)", "").trim();
			}
			//action 
			String action = NluTools.stringFindFirst(this_text, "^forget|^remove|^delete|^open|^show|^what is|^what are|^do you know");
			if (!action.isEmpty()){
				action = (action.matches("forget|remove|delete")? "remove":"open");
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.TYPE, item.trim());
				pv.put(PARAMETERS.INFO, info.trim());
				pv.put(PARAMETERS.ACTION, action.trim());
			possibleParameters.add(pv);
		}
		
		//insurance
		if (NluTools.stringContains(text, "insurance(s|)|assurance(s|)|inssure|assure")
				){
			String this_text = text;
			possibleCMDs.add(CMD.INSURANCE);
			possibleScore.add(1);	index++;
			
			//increase score for these
			if (NluTools.stringContains(this_text, "(inssure)")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}

			//action
			String action = NluTools.stringFindFirst(this_text, "help|info(s|)|information(s|)|offer|inssure|cancle|change|contract");
			if (!action.isEmpty()){
				//check for explicit help tags
				if (!action.isEmpty() && NluTools.stringContains(action, "help|info(s|)|information(s|)|cancle|change|contract")){
					action = "<get_help>";		//maybe this should be default
				}else{
					action = "<get_offer>"; 		//default and the only thing supported yet
				}
				possibleScore.set(index, possibleScore.get(index)+1);
			}else{
				action = "<get_offer>"; 		//default and the only thing supported yet
			}
			
			//info - what to insure
			String info = NluTools.stringFindFirst(this_text, "motorcycle(s|)|motorbike(s|)|bike(s|)|auto(s|)|car(s|)|"
								+ "house(s|)|flat(s|)|life");
			if (!info.isEmpty()){
				if (NluTools.stringContains(info, "motorcycle(s|)|motorbike(s|)")){
					info = "<motorcycle>"; 		//the only thing supported yet
				}
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			//type - types like "vollkasko"
			String type = NluTools.stringFindFirst(this_text, "(fully |partially |)comprehensive");
			if (!type.isEmpty() && type.contains("fully")){
				type = "<fc>";
			}else if (!type.isEmpty() && type.contains("partially")){
				type = "<pc>";
			}
			//type does not score (?)
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.ACTION, action);
				pv.put(PARAMETERS.INFO, info);
				pv.put(PARAMETERS.TYPE, type);
			possibleParameters.add(pv);
		}
		
		//locations
		if (NluTools.stringContains(text, "where (is|are|am) .*|where can i .*| where we are| where i am|where .* (is|are|live|lives)|"
				+ "(is there|are there|can i get) .*\\b(close|near|around|here|in)|"
				+ "(search|show|find|look for|looking for) .*(\\b)("+ RegexParameterSearch.get_POI_list(language) +")|"
				+ "(show|search|find|look for) .*(\\b)on (the map|maps)|"
				//+ "(show|search|find|look for) .*(\\b)(close|near|around)( to| by|)( me|)$|"	//TODO: broken
				+ "(show|search|find|look for) (on |)(the map|maps) .*|"
				+ "(address|location .*)")
					&& !possibleCMDs.contains(CMD.HOTELS)){
			String this_text = text;
			possibleCMDs.add(CMD.LOCATION);
			possibleScore.add(1);	index++;
			
			//TODO: make real parameter out of this!
			
			//analyze
			search_locations(this_text, language);
			//place to search
			String place = locations.get("location");
			String poi = locations.get("poi");
			if (place.isEmpty() && poi.isEmpty()){
				if (NluTools.stringContains(this_text, "where am i|where are we|where we are|where i am")){
					place = "<user_location>";
				}else{
					this_text = this_text.replaceFirst(".*?\\b(where (is|are)|where can i find|"
							+ "(search|show|find|look) (on |)(the map|maps)|address |location |"
							+ "where|(search|show|find|look for))\\b", "").trim();
					this_text = this_text.replaceFirst("^(for)\\b", "").trim();
					this_text = this_text.replaceFirst("^(the|a)\\b", "").trim();
					this_text = this_text.replaceFirst("\\b(is$|are$)", "").trim();
					this_text = this_text.replaceFirst("\\b(on the map$|on maps$)", "").trim();
					this_text = this_text.replaceFirst("\\b(close|near|around)( to| by|)( me|)$", "").trim();
					place = RegexParameterSearch.replace_personal_locations(this_text, language).trim();
				}
			}
			//still empty?
			if (!place.trim().isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			if (!poi.trim().isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, place);		//TODO: change to place instead of search?
				pv.put(PARAMETERS.POI, poi);
			possibleParameters.add(pv);
		}
		
		//retailer product search/buy
		if (NluTools.stringContains(text, "saturn|media(\\s|)(markt|market|markets)|amazon") || (NluTools.stringContains(text, "tv(?! (program))|television")	
							&& NluTools.stringContains(text, "show|search|find|look for|buy"))
			){
			String this_text = text;
			possibleCMDs.add(CMD.SEARCH_RETAIL);
			possibleScore.add(1);	index++;
			//increase score for these by 2
			if (NluTools.stringContains(this_text, "(saturn|media(\\s|)(markt|market|markets)|amazon)")){
				possibleScore.set(index, possibleScore.get(index)+2);
			//increase by 1 'cause we have to believe ^^
			}else{
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//retail parameters
			search_retail_parameters(this_text, language);
			String color = retail_parameters.get("color");
			String age = retail_parameters.get("age");
			String price = retail_parameters.get("price");
			String price_type = retail_parameters.get("price_t");
			
			String retail_type = "";
			if (NluTools.stringContains(this_text, "saturn")){
				retail_type = "<saturn>";
			}else if (NluTools.stringContains(this_text, "media(\\s|)(markt|market|markets)")){
				retail_type = "<media_markt>";
			}else if (NluTools.stringContains(this_text, "amazon")){
				retail_type = "<amazon>";
			}
			
			//convert units to default - handle special chars
			//--price to €
			price = RegexParameterSearch.convert_amount_to_default("money", price_type, price, language);
			
			//search term
			String search = NluTools.stringFindFirst(this_text, "(a|for) .*");
			search = search.replaceAll("^(a|for)\\b", "");
			search = search.replaceAll("\\b(and|with|for|in|from|to|over|under|the|not|less|more|maximum|maximal|minimum|minimal|at) .*", "").trim();
			String junk = NluTools.stringFindFirst(search, "saturn|media(\\s|)(markt|market|markets)|amazon");
			if (!junk.isEmpty()){
				search = search.replaceAll("(a |the |)" + Pattern.quote(junk), "").replaceAll("\\s+", " ").trim();
			}
			search = search.replaceAll("^(a|the)\\b","").trim();
			
			//score
			if (!color.isEmpty()){		possibleScore.set(index, possibleScore.get(index)+1);		}
			if (!age.isEmpty()){		possibleScore.set(index, possibleScore.get(index)+1);		}
			if (!price.isEmpty()){		possibleScore.set(index, possibleScore.get(index)+1);		}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.TYPE, retail_type.trim());
				pv.put(PARAMETERS.SEARCH, search.trim());
				pv.put(PARAMETERS.COLOR, color.trim());
				pv.put(PARAMETERS.AMOUNT_MONEY, price.trim());
				pv.put(PARAMETERS.AGE_YEARS, age.trim());
			possibleParameters.add(pv);
		}		
		
		//vehicle search/buy
		if (NluTools.stringContains(text, "autoscout|autoscout24") 
							|| (NluTools.stringContains(text, "auto(s|)|automobile(s|)|car(s|)|motorcycle(s|)|motorbike(s|)|bike(s|)")	
							&& NluTools.stringContains(text, "search|find|show|look for|searching for|looking for|buy|need"))
			){
			String this_text = text;
			possibleCMDs.add(CMD.SEARCH_VEHICLE);
			possibleScore.add(1);	index++;
			//increase score for these by 2
			if (NluTools.stringContains(this_text, "(autoscout|autoscout24)")){
				possibleScore.set(index, possibleScore.get(index)+2);
			//increase by 1 'cause we have to believe ^^
			}else{
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//vehicle parameters
			search_vehicle_parameters(this_text, language);
			String color = vehicle_parameters.get("vehicle_color");
			String age = vehicle_parameters.get("vehicle_age");
			String price = vehicle_parameters.get("vehicle_price");
			String price_type = vehicle_parameters.get("vehicle_price_t");
			String power = vehicle_parameters.get("vehicle_power");
			String power_type = vehicle_parameters.get("vehicle_power_t");
			String distance = vehicle_parameters.get("vehicle_distance");
			String distance_type = vehicle_parameters.get("vehicle_distance_t");
			
			String vehicle_type = "";
			if (NluTools.stringContains(this_text, "motorcycle(s|)|motorbike(s|)|bike(s|)")){
				vehicle_type = "B";
			}else if (NluTools.stringContains(this_text, "auto(s|)|automobile(s|)|car(s|)")){
				vehicle_type = "C";
			}
			
			//convert units to default - handle special chars
			//--price to €
			price = RegexParameterSearch.convert_amount_to_default("money", price_type, price, language);
			//--power to kW
			power = RegexParameterSearch.convert_amount_to_default("power", power_type, power, language);
			//--distance to km
			distance = RegexParameterSearch.convert_amount_to_default("distance", distance_type, distance, language);
			
			//search term
			String search = NluTools.stringFindFirst(this_text, "(a|for) .*");
			search = search.replaceAll("^(a|for)\\b", "");
			search = search.replaceAll("\\b(and|with|for|in|from|to|over|under|the|not|less|more|maximum|maximal|minimum|minimal|at) .*", "").trim();
			String junk = NluTools.stringFindFirst(search, "motorcycle(s|)|motorbike(s|)|bike(s|)|auto(s|)|automobile(s|)|car(s|)");
			if (!junk.isEmpty()){
				search = search.replaceAll("(a |the |)" + Pattern.quote(junk), "").replaceAll("\\s+", " ").trim();
			}
			if (NluTools.stringContains(this_text, "(model|make|brand|lable)( is| are|) (not important|unimportant|does not matter|doesnt matter)")){
				search = "<all>";
			}else if (search.isEmpty() && NluTools.stringContains(this_text, "(model|make|lable|brand)( is| are|) \\w+")){
				search = this_text.replaceFirst(".*\\b(model|make|lable|brand)( is| are| should be|) ((\\w+)(-\\w+|))\\b.*", "$3");
			}
			search = search.replaceAll("^(a|the)\\b","").trim();
			
			//score
			if (!color.isEmpty()){		possibleScore.set(index, possibleScore.get(index)+1);		}
			if (!age.isEmpty()){		possibleScore.set(index, possibleScore.get(index)+1);		}
			if (!price.isEmpty()){		possibleScore.set(index, possibleScore.get(index)+1);		}
			if (!power.isEmpty()){		possibleScore.set(index, possibleScore.get(index)+1);		}
			if (!distance.isEmpty()){	possibleScore.set(index, possibleScore.get(index)+1);		}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, search.trim());
				pv.put(PARAMETERS.TYPE, vehicle_type.trim());
				pv.put(PARAMETERS.COLOR, color.trim());
				pv.put(PARAMETERS.AMOUNT_MONEY, price.trim());
				pv.put(PARAMETERS.AMOUNT_POWER, power.trim());
				pv.put(PARAMETERS.AMOUNT_DISTANCE, distance.trim());
				pv.put(PARAMETERS.AGE_YEARS, age.trim());
			possibleParameters.add(pv);
		}
		
		//Banking
		if (NluTools.stringContains(text, "bank|bankaccount|banking|banktransfer|money|transfer|payment|pay|"
								+ "send .*(€|$|eur\\b|euro(s|)|dollar(s|)|pound(s|))|(€|$|eur\\b|euro(s|)|dollar(s|)|pound(s|)).* (send|sent)")){
			String this_text = text;
			possibleCMDs.add(CMD.BANKING);
			possibleScore.add(1);	index++;
			//currency
			String currency = RegexParameterSearch.get_currency(this_text, language);
			if (!currency.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//action / receiver
			String action = RegexParameterSearch.get_banking_action(this_text, language);
			String receiver = "";
			if (!action.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
				//check for contact if is <send>
				if (action.contains("<send>")){
					//TODO: find contact in text
				}
			}
			//numbers
			search_numbers(this_text, language);
			String num = numbers.get("value1");
			if (!num.isEmpty() && !currency.isEmpty() && !action.isEmpty() && (action.contains("<send>") || action.contains("<pay>"))){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.ACTION, action);
				pv.put(PARAMETERS.NUMBER, num);
				pv.put(PARAMETERS.CURRENCY, currency);
				pv.put(PARAMETERS.RECEIVER, receiver);
			possibleParameters.add(pv);
		}

		//----- CUSTOM SERVICES -----
		
		//Abstract analyzer (should come at the end because of lower priority?)
		ArrayList<ApiInterface> customServices = ConfigServices.getCustomServicesList(input, input.user);
		for (ApiInterface service : customServices){
			index = NluKeywordAnalyzer.abstractRegExAnalyzer(text, input, service,
					possibleCMDs, possibleScore, possibleParameters, index);
		}
		
		//----- ASSISTANT SDK SERVICES -----
		
		//Abstract analyzer (should come at the end because of lower priority?)
		if (Config.enableSDK){
			ArrayList<ApiInterface> assistantServices = ConfigServices.getCustomServicesList(input, Config.getAssistantUser());
			for (ApiInterface service : assistantServices){
				index = NluKeywordAnalyzer.abstractRegExAnalyzer(text, input, service,
						possibleCMDs, possibleScore, possibleParameters, index);
			}
		}
		
		//---------------------------
		
		//Control Devices/Programs
		//TODO: this is kind of a "if everything else failed try this" method ... IMPROVE IT!
		if (!possibleCMDs.contains(CMD.KNOWLEDGEBASE) && !possibleCMDs.contains(CMD.WEB_SEARCH) && !possibleCMDs.contains(CMD.LISTS)
										&& !possibleCMDs.contains(CMD.DIRECTIONS) && !possibleCMDs.contains(CMD.TV_PROGRAM)){
			String this_text = text;
			//search for control parameters
			search_control_parameters(this_text, language);
			String info = controls.get("control_info");
			String action = controls.get("control_action");
			String number = controls.get("control_number");
			String item = controls.get("control_type");
			if (!item.isEmpty()){
				//add
				possibleCMDs.add(CMD.CONTROL);
				possibleScore.add(1);	index++;
				//scores
				if (!info.isEmpty()){
					possibleScore.set(index, possibleScore.get(index)+1);
				}
				if (!number.isEmpty()){
					possibleScore.set(index, possibleScore.get(index)+1);
				}
				if (!action.isEmpty()){
					possibleScore.set(index, possibleScore.get(index)+1);
				}
				HashMap<String, String> pv = new HashMap<String, String>();
					pv.put(PARAMETERS.TYPE, item.trim());
					pv.put(PARAMETERS.ACTION, action.trim());
					pv.put(PARAMETERS.INFO, info.trim());
					pv.put(PARAMETERS.NUMBER, number.trim());
				possibleParameters.add(pv);
				
			}else if (!action.isEmpty()){
				//add
				possibleCMDs.add(CMD.CONTROL);
				possibleScore.add(1);	index++;
				//
				HashMap<String, String> pv = new HashMap<String, String>();
					pv.put(PARAMETERS.TYPE, item.trim());
					pv.put(PARAMETERS.ACTION, action.trim());
					pv.put(PARAMETERS.INFO, info.trim());
					pv.put(PARAMETERS.NUMBER, number.trim());
				possibleParameters.add(pv);
			}
		}		
		
		//Common search fallback
		if (NluTools.stringContains(text, "search|find|show|look for|searching for|looking for") 
				){
			//String this_text = text;
			possibleCMDs.add(CMD.WEB_SEARCH);
			possibleScore.add(1);	index++;
			
			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.WEBSEARCH_ENGINE, PARAMETERS.WEBSEARCH_REQUEST, PARAMETERS.SEARCH_SECTION)
					.setup(input, pv);
			aps.getParameters();
			//possibleScore.set(index, possibleScore.get(index) + aps.getScore()); 		//Score stays always 1
			possibleParameters.add(pv);
		}
		
		//DEMO Mode
		if (input.demo_mode){
			//you can put stuff here for demos
		}
		
		//--set certainty_lvl--
		int bestScoreIndex = 0;
		if (possibleScore.size()>0){
			int bestScore = Collections.max(possibleScore);
			int totalScore = 0;
			//kind'a stupid double loop but I found no better way to first get total score 
			for (int i=0; i<possibleScore.size(); i++){
				totalScore += possibleScore.get(i);
			}
			for (int i=0; i<possibleScore.size(); i++){
				if (possibleScore.get(i) == bestScore){
					bestScoreIndex = i;
					break;		//take the first if scores are equal
				}
			}
			certainty_lvl = Math.round(((double) bestScore)/((double) totalScore)*100.0d)/100.0d;
		}else{
			certainty_lvl = 0.0d;
		}
		
		//check if there was any result - if not add the no_result API
		if (possibleCMDs.isEmpty()){
			possibleCMDs.add(CMD.NO_RESULT);
			possibleScore.add(1);
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put("text", text);
			possibleParameters.add(pv);
			certainty_lvl = 1.0d;
		}
		
		//create the result with default constructor and add specific variables:
		NluResult result = new NluResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex);
		result.certainty_lvl = certainty_lvl;
		//copy the default variables from input (like environment, mood etc.) and add input to result:
		result.setInput(input);
		result.normalized_text = text;	//input has the real text, result has the normalized text
		//you can set some of the default result variables manually if the interpreter changes them:
		result.language = language;		// might well be analyzed and changed by the interpreter, in this case here it must be English
		//result.context = context;		// is auto-set inside the constructor to best command 
		//result.mood = mood;			// typically only APIs change the mood
		
		return result;
	}

	public double getCertaintyLevel(NluResult result) {
		return result.certainty_lvl;
	}
	
	/**
	 * Search for locations once. If it has been done already this method does nothing. 
	 * @param text - complete text to search
	 * @param language - language code
	 */
	private void search_locations(String text, String language){
		if (locations == null){
			locations = RegexParameterSearch.get_locations(text, language);
			/*
			locations.put("location", locations.get("location"));
			locations.put("location_start", locations.get("location_start"));
			locations.put("location_end", locations.get("location_end"));
			*/
		}
	}
	/**
	 * Search for dates once. If it has been done already this method does nothing. 
	 * @param text - complete text to search
	 * @param language - language code
	 */
	private void search_dates(String text, String language){
		if (dates == null && locations != null){
			dates = new HashMap<String, String>();
			dates.put("date_tag", locations.get("travel_time"));
		}
		else if (dates == null){
			dates = RegexParameterSearch.get_date(text, language);
		}
	}
	/**
	 * Search for numbers once. If it has been done already this method does nothing. 
	 * @param text - complete text to search
	 * @param language - language code
	 */
	private void search_numbers(String text, String language){
		if (numbers == null){
			String num = RegexParameterSearch.get_number(text);
			numbers = new HashMap<String, String>();
			numbers.put("value1", num);
		}
	}
	/**
	 * Get the web search term. If it has been done already this method does nothing. 
	 * @param text - complete text to search
	 * @param language - language code
	 */
	/*
	private void get_websearch(String text, String language){
		if (websearches == null){
			websearches = NLU_parameter_search.get_search(text, language);
		}
	}
	*/
	/**
	 * Get the music parameters artist/creator and genre. 
	 * uses: music_artist, music_genre
	 * @param text - complete text to search
	 * @param language - language code
	 */
	private void search_music_parameters(String text, String language){
		if (music_parameters == null){
			music_parameters = new HashMap<String, String>();
			String artist = RegexParameterSearch.get_creator(text, language);
			String genre = RegexParameterSearch.get_music_genre(text, language);
			String search = RegexParameterSearch.get_startable(text, language);
			music_parameters.put("music_artist", artist);
			music_parameters.put("music_genre", genre);
			music_parameters.put("music_search", search);
			//TODO: maybe remove artist and genre from search?
		}
	}
	/**
	 * Get the control parameters. If it has been done already this method does nothing. 
	 * uses: control_action, control_type, control_info, control_number
	 * @param text - complete text to search
	 * @param language - language code
	 */
	private void search_control_parameters(String text, String language){
		if (controls == null){
			controls = new HashMap<String, String>();
			controls.put("control_type", RegexParameterSearch.get_control_type(text, language));
			controls.put("control_action", RegexParameterSearch.get_control_action(text, language)[0]);
			controls.put("control_info", RegexParameterSearch.get_control_location(text, language));
			controls.put("control_number", RegexParameterSearch.get_number(text));
		}
	}
	/**
	 * Get the vehicle search parameters price/power/age/color. 
	 * uses: vehicle_age, vehicle_color, vehicle_price, vehicle_price_t, vehicle_power, vehicle_power_t
	 * @param text - complete text to search
	 * @param language - language code
	 */
	private void search_vehicle_parameters(String text, String language){
		if (vehicle_parameters == null){
			HashMap<String, String> map = new HashMap<>();
			String age;
			String price, price_t;
			String color;
			String power, power_t;
			String distance, distance_t;
			if (retail_parameters != null){
				age = retail_parameters.get("age");
				price = retail_parameters.get("price");
				price_t = retail_parameters.get("price_t");
				color = retail_parameters.get("color");
			}else{
				map = RegexParameterSearch.get_age(text, language); 	//TODO: fails for "with a price of 2000 euro made in 1999"
				age = map.get("age_y");
				color = RegexParameterSearch.get_color(text, language).replaceFirst("\\s.*", "").trim();
				map = RegexParameterSearch.get_amount_of(text, "money", language);
				price = map.get("amount");
				price_t = map.get("type");
			}
			map = RegexParameterSearch.get_amount_of(text, "power", language);
			power = map.get("amount");
			power_t = map.get("type");
			map = RegexParameterSearch.get_amount_of(text, "distance", language);
			distance = map.get("amount");
			distance_t = map.get("type");
			vehicle_parameters = new HashMap<String, String>();
			vehicle_parameters.put("vehicle_age", age);
			vehicle_parameters.put("vehicle_color", color);
			vehicle_parameters.put("vehicle_price", price);
			vehicle_parameters.put("vehicle_price_t", price_t);
			vehicle_parameters.put("vehicle_power", power);
			vehicle_parameters.put("vehicle_power_t", power_t);
			vehicle_parameters.put("vehicle_distance", distance);
			vehicle_parameters.put("vehicle_distance_t", distance_t);
			
			//Debugger.printHM_SS(vehicle_parameters); 		//debug
		}
	}
	/**
	 * Get the retail search parameters price/age/color. 
	 * uses: age, color, price, price_t
	 * @param text - complete text to search
	 * @param language - language code
	 */
	private void search_retail_parameters(String text, String language){
		if (retail_parameters == null){
			String age;
			String price, price_t;
			String color;
			HashMap<String, String> map = new HashMap<>();
			if (vehicle_parameters != null){
				age = vehicle_parameters.get("vehicle_age");
				price = vehicle_parameters.get("vehicle_price");
				price_t = vehicle_parameters.get("vehicle_price_t");
				color = vehicle_parameters.get("vehicle_color");
			}else{
				map = RegexParameterSearch.get_age(text, language); 	//TODO: fails for "with a price of 2000 euro made in 1999"
				age = map.get("age_y");
				map = RegexParameterSearch.get_amount_of(text, "money", language);
				price = map.get("amount");
				price_t = map.get("type");
				color = RegexParameterSearch.get_color(text, language).replaceFirst("\\s.*", "").trim();
			}
			retail_parameters = new HashMap<String, String>();
			retail_parameters.put("age", age);
			retail_parameters.put("color", color);
			retail_parameters.put("price", price);
			retail_parameters.put("price_t", price_t);
			
			//Debugger.printHM_SS(vehicle_parameters); 		//debug
		}
	}
}
