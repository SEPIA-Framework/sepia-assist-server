package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.parameters.AbstractParameterSearch;
import net.b07z.sepia.server.assist.parameters.FashionBrand;
import net.b07z.sepia.server.assist.parameters.FashionItem;
import net.b07z.sepia.server.assist.parameters.FashionShopping;
import net.b07z.sepia.server.assist.parameters.FoodClass;
import net.b07z.sepia.server.assist.parameters.FoodItem;
import net.b07z.sepia.server.assist.parameters.Language;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * Simple (at least from the idea) yet effective keyword analyzer for German to interpret user input.<br>
 * The order of checks does matter, put commands with less priority at the bottom. If commands score the same probability the first is taken.<br>
 * Note: Never use this as a static interpreter! Always create new instances of it when needed (compared to the sentence matchers
 * that can be used globally). 
 *   
 * @author Florian Quirin
 *
 */
public class NluKeywordAnalyzerDE implements NluInterface {

	double certainty_lvl = 0.0d;		//how certain is ILA about a result
	
	Map<String, String> locations;		//store locations in text
	Map<String, String> dates;			//store dates in text
	Map<String, String> numbers;		//store numbers in text
	Map<String, String> websearches;	//store web searches in text
	Map<String, String> controls;		//store control parameters in text
	Map<String, String> music_parameters;		//store music parameters in text
	Map<String, String> vehicle_parameters;		//store vehicle search parameters
	Map<String, String> retail_parameters;		//store retail search parameters
	
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
		List<String> possibleCMDs = new ArrayList<>();		//make a list of possible interpretations of the text
		List<Map<String, String>> possibleParameters = new ArrayList<>();		//possible parameters
		List<Integer> possibleScore = new ArrayList<>();	//make scores to decide which one is correct command
		int index = -1;

		//some small-talk stuff
		if (NluTools.stringContains(text, "^" + Pattern.quote(Config.assistantName.toLowerCase()) + "$")){
			//String this_text = text;
			possibleCMDs.add(CMD.CHAT);
			possibleScore.add(1);	index++;
			//type
			String type = "greeting";
			possibleScore.set(index, possibleScore.get(index)+1);
			
			Map<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.TYPE, type);
			possibleParameters.add(pv);
		}
		
		//TODO: We can simplify this class by moving all regular expressions to the getInfo method of the services and then
		//we could simply iterate over the InterviewServicesMap and use the abstract regular expressions matcher to build 
		//the result for each command, e.g.:
		/*
		for (Entry<String, List<String>> es : InterviewServicesMap.get().entrySet()){
			String cmd = es.getKey();
			List<ApiInterface> defaultServicesForCmd = ConfigServices.buildServices(cmd);
			for (ApiInterface service : defaultServicesForCmd){
				index = NluKeywordAnalyzer.abstractRegExAnalyzer(text, input, service,
						possibleCMDs, possibleScore, possibleParameters, index);
			}
		}
		*/
		//... because the order of the commands matters and exceptions might apply we could create a custom list as well 
		
		//--------------------------------------------------
		
		//What follows now is a list of regular expressions for certain commands that trigger parameter searches ...
		
		//news + sports results
		if (NluTools.stringContains(text, "\\w*news|nachrichten|neuigkeiten|was gibt es neues|was geht ab|schlagzeile|schlagzeilen|"
				+ "los in der welt|(bring|moechte) .* (neusten stand|up to date|up-to-date)|wissen .* mitreden|"
				+ "regenbogenpresse|yellow press|boulevardpresse|das neuste|neues (aus|zu)|was .* wissen|"
				+ "tagesschau|spiegel online|frankfurter allgemeine|sueddeutsche|bild zeitung|"
				+ "(fussball|tennis|basketball|hockey|handball|football|golf|baseball|eishockey)\\w*|"
				+ "bundesliga\\w*|champions( |-|)league|europaliga|euro( |-|)league|europa( |-|)league|premier league|primera division|la liga|"
				+ "(serie|seria|series) a|eredivisie|ligue 1|primeira liga|sueperlig|sueper lig(a|)|(fa|dfb)(-| )(pokal|cup)|"
				+ ".*(ergebnis(se|)|resultat(e|))|(hat|haben)\\b.* (gespielt|gewonnen|verloren|getroffen|tor)|wie (steht es|stehts) (beim|bei) |^(fussball|bundesliga|tennis)(spiel|)\\b|"
				+ "spielstand|(wie|wann) (spielt|spielen) (?!man)")){
			//String this_text = text;
			possibleCMDs.add(CMD.NEWS);
			possibleScore.add(1);	index++;

			Map<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.NEWS_SECTION, PARAMETERS.NEWS_TYPE, PARAMETERS.SPORTS_TEAM, PARAMETERS.SPORTS_LEAGUE)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//weather
		if (NluTools.stringContains(text, "wetter\\w*|\\w+wetter|temperatur|regen|regnet|regnen|sonne|sonnig|(regen|sonnen)schirm|sonnencreme|"
						+ "schnee|schneit|schneien|hageln|hagel|stuermen|sturm|orkan|tornado|nebel|neblig|(regen|niederschlags)wahrscheinlichkeit|"
						+ "unwetter|gewitter|frost|(wieviel|wie viel) grad|"
						+ "(wird|wie) (.* |)(warm|kalt|heiss|eisig|frostig|sonnig|windig|bewoelkt|regnerisch|neblig|stuermisch|fruehlingshaft|winterlich|sommerlich|herbstlich)(er|)|"
						+ "(soll|sollte|brauche|darf|muss|muesste)( ich|) .*\\b(lange|dicke|)((regen|)jacke|sonnenbrille|schirm|(unter|)hose(n|)|socken|pulli|handschuhe|schal|muetze)"
			)){
			//String this_text = text;
			possibleCMDs.add(CMD.WEATHER);
			possibleScore.add(1);	index++;

			Map<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.TIME, PARAMETERS.PLACE)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//directions
		if (NluTools.stringContains(text, "die richtung|geht es richtung|navigation|zeig mir den weg|navigiere mich|(?<!(guenstigste|guenstigsten) )weg |"
							+ "bring mich |fahre mich |wie weit |entfernung |ich muss nach |"
							+ "wie (lange|lang) (noch|muss|muessen|dauert|brauch|brauche|von)|"
							+ "(wann|wo|wie) (.* |)(faehrt|geht|kommt|komme) (.* |)(nach|zu|zum|zur)|"
							+ "(\\w*bahn|zug|bus)verbindung(en|)|"
							+ "\\b(von|vom) .* (nach|zu|zum|zur) |komme ich (nach|zu|zum|zur) .* (von|vom) |"
							+ " (nach|zu|zum|zur)\\b.* (fahren|laufen|gehen|kommen|komme)|"
							+ "(zeig|finde) .* (von|vom|nach|bis) .* (map|karte|maps)")){
			//String this_text = text;
			possibleCMDs.add(CMD.DIRECTIONS);
			possibleScore.add(1);	index++;

			Map<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.TIME, PARAMETERS.TRAVEL_TYPE, PARAMETERS.TRAVEL_REQUEST_INFO, 
							PARAMETERS.LOCATION_END, PARAMETERS.LOCATION_WAYPOINT, PARAMETERS.LOCATION_START)
					.setup(input, pv);
			aps.getParameters();
			possibleScore.set(index, possibleScore.get(index) + aps.getScore());
			possibleParameters.add(pv);
		}
		
		//fashion shopping
		if (NluTools.stringContains(text, "(kaufen|erwerben|mode|fashion|shoppen|shopping|(sind|ist) kaputt)") || 
				((NluTools.stringContains(text, FashionItem.fashionItems_de) || NluTools.stringContains(text, FashionBrand.fashionBrandsSearch)) 
					&& !NluTools.stringContains(text, "(ich habe |da sind |es gibt |meine )"))
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
		if (NluTools.stringContains(text, "^essen$|mittagessen|"
				+ "(\\w*essen|nahrung|fruehstueck|lebensmittel|nahrungsmittel) .*\\b(bestellen|kaufen|ordern|liefern|essen)|"
				+ "(" + Language.languageClasses_de + "|" + FoodClass.foodClasses_de + ") .*\\b(bestellen|kaufen|ordern|liefern|essen)|"
				+ "(ordere|bestelle|kaufe|gerne|was) .*\\b(\\w*essen|\\w*nahrung|fruehstueck|lebensmittel|nahrungsmittel)|"
				+ "(ordere|bestelle|kaufe|gerne) .*\\b(" + Language.languageClasses_de + "|" + FoodClass.foodClasses_de + ")|"
				+ "\\w*hunger|hungrig|appetit|lieferservice(s|)|(was|etwas) zwischen .*\\b(zaehne|beisser)|"
				+ "(aus|von) .*\\b(\\w*restaurant)") 
				||	NluTools.stringContains(text, FoodItem.foodItems_de)
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
		if (NluTools.stringContains(text, "wiki|wikipedia|information|informationen|"
							+ "wer (ist|sind|war|waren) .*|"
							+ "was (ist|sind|war|waren) .*|"
							+ "wann (ist|sind|war|waren) .*|"
							+ "wofuer steht .*|was bedeutet .*|"
							+ "kennst du .*|erzaehl mir .* ueber|definiere|"
							+ "wie hoch (ist|sind|war|waren) .*|wie alt (ist|sind|war|waren) .*|(wieviele|wie viele) .* (hat|hatte|hatten|ist|sind|war|waren|leben|lebten)")){
			String this_text = text;
			possibleCMDs.add(CMD.KNOWLEDGEBASE);
			possibleScore.add(1);	index++;
			//kb search term
			String kb_search = "";
			if (NluTools.stringContains(this_text, "wie hoch (ist|sind|war|waren)|wie alt (ist|sind|war|waren)|(wieviele|wie viele) .* (hat|hatte|hatten|ist|sind|war|waren|leben|lebten)")){
				kb_search = this_text.replaceFirst(".*\\b(wie hoch|wie alt|wieviele|wie viele)\\b", "$1").trim();
			}else{
				kb_search=this_text.replaceFirst(".*?\\b(suche nach|suchen nach|suche|was bedeutet|wofuer steht|wer|was|wo|wann|informationen ueber|ueber|kennst du)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(^wer|^wo|^was|^wann)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(^ist|^war|^waren|^sind|^ueber)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(^ein|^einer|^einen|^eine|^der|^die|^das|^wiki|^wikipedia)\\s", "").trim();
				kb_search=kb_search.replaceFirst("\\b(ist$|war$|waren$|sind$|suchen$|finden$|bei wikipedia$|bei wiki$)", "").trim();
			}
			//recover original
			kb_search = normalizer.reconstructPhrase(input.textRaw, kb_search);
			//score
			if (!kb_search.matches("")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//kb additional query (time, place, person, thing)
			String kb_add_info="";
			if (NluTools.stringContains(this_text, "wann|zeit|wie alt")){
				kb_add_info = "time";
			}else if (NluTools.stringContains(this_text, "wer|person")){
				kb_add_info = "person";
			}else if (NluTools.stringContains(this_text, "wo|ort")){
				kb_add_info = "place";
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, kb_search.trim());
				pv.put(PARAMETERS.TYPE, kb_add_info.trim());
			possibleParameters.add(pv);
		}
		
		//web search
		if (NluTools.stringContains(text, "(websuche|web suche|"
						+ "(durchsuche|suche|schau|finde|zeig)( mir|)( mal| bitte|)( bitte| mal|)( im| das) (web|internet))|"
						+ "^google|^bing|^yahoo|^duck duck|^duck duck go|"
						+ "^(bild(ern|er|)|rezept(en|e|)|video(s|)|movie(s|)|film(en|e|)|aktie(n|)|aktien(wert|kurs)|buecher(n|)|buch)|"
						+ "wie (ist|steht|stehen) (der|die) (aktienkurs|aktienwert|aktie(n|)|kurs|wert) (von|vom|der)|wie (steht|stehen) .* aktien|"
						+ "(durchsuche|suche|schau|finde|zeig)( | .* )(im (web|internet))|"
						+ "(durchsuche|suche|schau|finde|zeig)( | .* )(mit|per|via|auf|ueber|mittels|bei) (google|bing|duck duck go|duck duck|yahoo)|"
						+ "(durchsuche|suche|schau|finde|zeig)( | .* )(bilder(n|)|rezepte(n|)|video(s|)|youtube|movies|filme(n|)|buecher(n|)|aktie(n|)|aktien(wert|kurs))")
					//|| NLU_Tools.stringContains(text, "suche(n|)|finde(n|)|zeig(en|)")
					//|| (NLU_Tools.stringContains(text, "suche(n|)|finde(n|)|zeig(en|)") 
					//	&& !NLU_Tools.stringContains(text, "(suche(n|)|finde(n|)|zeig(en|))\\b.*\\b(musik|song(s|)|news|nachrichten|wikipedia|"
					//									+ "(action|fantasy|)movie(s|)|(action|fantasy|)film(e|en|)|ticket(s|)|karte(n|))"))
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
		if (NluTools.stringContains(text, "(\\b)(die |)(schnellste|schnellsten|schnellster|beste|besten|bester|guenstigste|guenstigsten|guenstigster) (verbindung|verbindungen|weg)|"
									+ "(option|optionen).*?\\b(von|vom)\\b.*?\\b(nach|zu|zum|zur)|"
									+ "(option|optionen).*?\\b(zwischen)\\b.*?\\b(und)")){
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
		if (NluTools.stringContains(text, "(flug|fluege|fluegen) (nach|zu|zur|von)|(suche|finde|brauche|zeig mir|buche|wie)\\b.* (flug|fluege|fluegen|fliegen|flugticket(s|)|flugzeug(en|e|))|"
				+ "(flug|fluege) (suchen|finden|buchen|zeigen)|flugsuche|"
				+ " mit( dem| einem|) flugzeug")
				&& !NluTools.stringContains(text, "auf (der|einer) karte")){
			String this_text = text;
			possibleCMDs.add(CMD.FLIGHTS);
			possibleScore.add(1);	index++;
			//double score for flight
			if (NluTools.stringContains(this_text, "flug|fluege|fluegen|fliegen|flugzeug|flugticket")){
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
				travel_destination = travel_destination.replaceAll("\\b(fluege(n|)|flug|flugzeug(en|e|)|flugticket(s|))\\b", "");
				travel_destination = travel_destination.replaceAll("\\b^(der|die|das|einer|eine|einem|ein|einen)\\b", "").trim();
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
		if (NluTools.stringContains(text, "(\\w+(-|)|)timer(s|)|(\\w+(-|)|)counter(s|)|(\\w+(-|)|)countdown(s|)|(\\w+(-|)|)stoppuhr(en|)|stop uhr(en|)|zeitnehmer|zeitgeber|zeitmesser|"
							+ "(\\w+(-|)|)alarm(e|s|)|weck (mich|uns)|(\\w+(-|)|)wecker|wecken|aufstehen|aus dem bett|"
							+ "erinnere|(\\w+(-|)|)erinnerung(en|)|erinnern|"
							+ "(\\w+(-|)|)termin(e|)|kalender")){
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
		if (NluTools.stringContains(text, "hotel|hotels") && !NluTools.stringContains(text, "wo(?! ich|wir|man)|map|maps|karte")){
			String this_text = text;
			possibleCMDs.add(CMD.HOTELS);
			possibleScore.add(1);	index++;
			//analyze
			search_locations(this_text, language);
			//place to search
			String place = locations.get("location").trim();			//String poi = locations.get("poi");
			//clean up place
			place = place.replaceFirst("(.*)( fuer )(.*)", "$1").trim();
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
		if (NluTools.stringContains(text, "radio|radiokanal|radiostation|radiosender|radiostream|musikkanal|musikstation") 
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
		if (NluTools.stringContains(text, "musik|spiele .*|spiel .*|song|songs|lied|lieder") 
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
		if (NluTools.stringContains(text, "(action|fantasy|phantasie|)(movies|movie|filme|film|\\w+filmen)") 
										&& !possibleCMDs.contains(CMD.KNOWLEDGEBASE)){
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
			if (this_text.matches(".*(movie |movies |film |filme |filmen )(von .*)")){
				info = "director";
			}else if (this_text.matches(".*(movie |movies |film |filme |filmen )(mit dem namen .*|namens .*|genannt .*)")){
				info = "title";
			}else if (this_text.matches(".*(movie |movies |film |filme |filmen )(mit dem schauspieler .*|mit der schauspielerin .*|mit .*)")){
				info = "actor";
			}
			//title
			String movie = "";
			if (this_text.matches(".*(movie |movies |film |filme |filmen )(mit dem schauspieler|mit der schauspielerin|mit dem namen|namens|genannt|von|mit) .*")){
				movie = this_text.replaceFirst(".*?(movie |movies |film |filme |filmen )(mit dem schauspieler |mit der schauspielerin |mit dem namen |namens |genannt |von |mit )", "").trim();
				possibleScore.set(index, possibleScore.get(index)+1);
			}else{
				if (this_text.matches(".*(movie |movies |film |filme |filmen ).*"))
					movie = this_text.replaceFirst(".*?(movie |movies |film |filme |filmen )", "").trim();
				else {
					movie = this_text.replaceFirst("\\b(action|)(movies$|filme$|filmen$)", "").trim();
					movie = movie.replaceFirst("^(zeig mir |zeig |finde mir |finde |suche nach |suche )", "").trim();
				}
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, movie);
				pv.put(PARAMETERS.TYPE, movie_type);
				pv.put(PARAMETERS.INFO, info);
			possibleParameters.add(pv);
		}
		
		//event tickets
		if (NluTools.stringContains(text, "ticket|tickets|(karten|karte) (fuer|fuers)|"
						+ "(event|kino|movie|film|filmtheater|lichtspielhaus|konzert|theater|"
							+ "opern|oper|festival|sport|fussball)(tickets| tickets|karten| karten)")){
			String this_text = text;
			possibleCMDs.add(CMD.TICKETS);
			possibleScore.add(1);	index++;
			//type
			String ticket_type = RegexParameterSearch.get_event_type(this_text, language);
			//search event/movie/etc. ...
			String search = "";
			if (this_text.matches(".*\\b(fuer |fuers ).*")){
				search = this_text.replaceFirst(".*\\b(fuer |fuers )", "").trim();
				search = search.replaceFirst("^(die|den|das|eine|einen|ein)\\b", "").trim();
				search = search.replaceFirst("\\b(kaufen|bestellen|besorgen|suchen|haben|bekommen)\\b.*", "").trim();
				search = search.replaceFirst("\\b(movie|film|konzert|auftritt)$\\b", "").trim();
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
		if (NluTools.stringContains(text, "woerterbuch|wort (suche(n|)|finde(n|))|uebersetzung|"
							+ "uebersetzen|uebersetze|"
							+ ".* (auf|ins|in) (deutsch|tuerkisch|englisch|franzoesisch|italienisch|spanisch)")){
			String this_text = text;
			possibleCMDs.add(CMD.DICT_TRANSLATE);
			possibleScore.add(1);	index++;
			
			//TODO: make real parameter out of this
			
			//dictionary target language
			String target_lang="";
			if (NluTools.stringContains(this_text, "ins englische|(nach|in|auf) englisch")){
				target_lang = "en";
			}else if (NluTools.stringContains(this_text, "ins spanische|(nach|in|auf) spanisch")){
				target_lang = "es";
			}else if (NluTools.stringContains(this_text, "ins tuerkische|(nach|in|auf) tuerkisch")){
				target_lang = "tr";
			}else if (NluTools.stringContains(this_text, "ins franzoesische|(nach|in|auf) franzoesisch")){
				target_lang = "fr";
			}else if (NluTools.stringContains(this_text, "ins deutsche|(nach|in|auf) deutsch")){
				target_lang = "de";
			}
			if (!target_lang.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//remove language tag
			this_text = this_text.replaceFirst("(ins|nach|in|auf) (englische|englisch|spanische|spanisch|"
					+ "tuerkische|tuerkisch|franzoesische|franzoesisch|deutsche|deutsch)", "").trim();
			
			//dictionary search term
			String dict_search=this_text.replaceFirst(".*?\\b(woerterbuch nach|woerterbuch suchen)\\s", "").trim();
			dict_search=dict_search.replaceFirst(".*?\\b(uebersetzen von|uebersetzung von|uebersetze)\\s", "").trim();
			dict_search=dict_search.replaceFirst(".*?\\b(suchen nach|suche nach|suche)\\s", "").trim();
			dict_search=dict_search.replaceFirst("\\b(^kannst du mir|^kannst du|^was heisst|^zeig mir|^was bedeutet|^was ist)\\b", "").trim();
			dict_search=dict_search.replaceFirst("\\b(^der|^die|^das|^mir)\\b", "").trim();
			dict_search=dict_search.replaceFirst("\\b(fuer mich$)", "").trim();
			dict_search=dict_search.replaceFirst("\\b(durchsuchen$|suchen$|finden$|gucken$|zeigen$)", "").trim();
			dict_search=dict_search.replaceFirst("\\b(suchen |)(im woerterbuch$|uebersetzen$|uebersetzung$)", "").trim();
			dict_search=dict_search.replaceFirst("\\b(zu$)", "").trim();
			if (!dict_search.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			//recover original
			dict_search = normalizer.reconstructPhrase(input.textRaw, dict_search);
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.SEARCH, dict_search.trim());
				pv.put(PARAMETERS.LANGUAGE, target_lang.trim());
			possibleParameters.add(pv);
		}
		
		//Lists
		if (NluTools.stringContains(text, "\\w*liste(n|)|list(s|)|zettel|to(-| |)do(-\\w+|)|"
												+ "einkaufsliste(n|)|einkaufszettel|shopping(-| |)list(en|e|)|"
												+ "einzukaufen|muss (.* |)(ein|)kaufen|(ein|)kaufen (.* |)muss")){
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
		if (NluTools.stringContains(text, "(tv|televisions|fernseh) programm|fernsehprogramm|(was laeuft|was gibt es|was kommt).* (tv|fernsehn|fernsehen|glotze)")){
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
			String channel = NluTools.stringFindFirst(this_text, "ard|zdf|wdr|ndr");
			if (!channel.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.TIME, tv_time.trim());
				pv.put(PARAMETERS.CHANNEL, channel.trim());
			possibleParameters.add(pv);
		}
		
		//My favorites
		if (NluTools.stringContains(text, "(mein |meine |meinen |meinem |meiner )(persoenliche(r|s|n|)|private(r|s|n|)|lieblings\\w*)( .*|) (ist|sind|lautet|lauten|heisst|heissen)|"
						+ "(was (ist|sind)|kennst du|entferne|vergiss|loesche) (mein|meine|meinen|meinem|meiner) (lieblings.*|private.*|persoenlich.*)|"
						+ "(oeffne|zeig mir|zeig|was (ist|sind)|wie (lautet|lauten)|kennst du) (mein|meine|meinen|meinem|meiner) (favoriten|infos|lieblingssachen)")){
			String this_text = text;
			possibleCMDs.add(CMD.MY_FAVORITE);
			possibleScore.add(1);	index++;
			//item
			String info = "";
			String item = RegexParameterSearch.get_my_info_item(this_text, language);
			//item = this_text.replaceFirst(".*?\\b(mein |meine |meinen )(persoenliche(r|s|n|)|private(r|s|n|)|)\\b", "").trim();
			//item = item.replaceFirst("\\b(ist|sind|lautet|lauten)\\b.*", "").trim();
			if (!item.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
				//info
				info = this_text.replaceFirst(".*?\\b(" + item + ")\\b", "");
				info = RegexParameterSearch.get_my_info(info, language);
				//info = info.replaceFirst(".*?\\b(ist|sind|lautet|lauten)\\b", "").trim();
				//info = info.replaceFirst("\\b^(ein|eine)\\s", "").trim();
				if (!info.isEmpty()){
					possibleScore.set(index, possibleScore.get(index)+1);
				}
				//clean up some items
				item = item.replaceFirst("^(favoriten|infos|lieblingssachen)", "").trim();
			}
			//action 
			String action = NluTools.stringFindFirst(this_text, "^vergiss|^entferne|^loesche|^oeffne|^zeig|^was ist|^was sind|^kennst du");
			if (!action.isEmpty()){
				possibleScore.set(index, possibleScore.get(index)+1);
				action = (action.matches("vergiss|entferne|loesche")? "remove":"open");
			}
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.TYPE, item.trim());
				pv.put(PARAMETERS.INFO, info.trim());
				pv.put(PARAMETERS.ACTION, action.trim());
			possibleParameters.add(pv);
		}
		
		//insurance
		if (NluTools.stringContains(text, "versicherung(en|)|\\w+versicherung(en|)|"
						+ "versichern|versichere")){
			String this_text = text;
			possibleCMDs.add(CMD.INSURANCE);
			possibleScore.add(1);	index++;
			
			//increase score for these
			if (NluTools.stringContains(this_text, "(versichern|versichere)")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}

			//action
			String action = NluTools.stringFindFirst(this_text, "hilfe|info(s|)|information(en|)|angebot(e|)|versichern|versichere|buche(n|)|bestelle(n|)|kuendige(n|)|kuendigung(en|)|wechsel(n|)");
			if (!action.isEmpty()){
				//check for explicit help tags
				if (!action.isEmpty() && NluTools.stringContains(action, "hilfe|info(s|)|information(en|)|kuendige(n|)|kuendigung(en|)|wechsel(n|)")){
					action = "<get_help>";		//maybe this should be default
				}else{
					action = "<get_offer>"; 		//default and the only thing supported yet
				}
				possibleScore.set(index, possibleScore.get(index)+1);
			}else{
				action = "<get_offer>"; 		//default and the only thing supported yet
			}
			
			//info - what to insure
			String info = NluTools.stringFindFirst(this_text, "motorrad|motorraeder|rad|raeder|auto(s|)|haus|haeuser|wohnung(en|)|leben|"
								+ "lebensversicherung(en|)|hausratversicherung(en|)");
			if (!info.isEmpty()){
				if (NluTools.stringContains(info, "motorrad|motorraeder")){
					info = "<motorcycle>"; 		//the only thing supported yet
				}
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			//type - types like "vollkasko"
			String type = NluTools.stringFindFirst(this_text, "vollkasko(versicherung(en|)|)|teilkasko(versicherung(en|)|)");
			if (!type.isEmpty() && type.contains("vollkasko")){
				type = "<fc>";
			}else if (!type.isEmpty() && type.contains("teilkasko")){
				type = "<pc>";
			}
			//type does not score (?)
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.ACTION, action);
				pv.put(PARAMETERS.INFO, info);
				pv.put(PARAMETERS.TYPE, type);
			possibleParameters.add(pv);
		}
		
		//customer service - telecommunication
		if (NluTools.stringContains(text, "(handy|mobilfunk|mobil|festnetz|internet|pre(-| |)paid)( |)(vertrag|vertraege(n|)|angebot(en|e|)|anschluss|anschluesse(n|))|"
							+ "(angebot(e|)|vertrag|vertraege(n|))( | .* )(handy|mobilfunk|mobil|festnetz|internet).*")){
			String this_text = text;
			possibleCMDs.add(CMD.CS_TELCOM);
			possibleScore.add(1);	index++;
			
			//increase score for these
			if (NluTools.stringContains(this_text, "(mobilfunk.*|handy|festnetz.*)")){
				possibleScore.set(index, possibleScore.get(index)+1);
			}

			//action
			String action = NluTools.stringFindFirst(this_text, "hilfe|info(s|)|information(en|)|angebot(e|)|versichern|versichere|buche(n|)|bestelle(n|)|kuendige(n|)|kuendigung(en|)|wechsel(n|)");
			if (!action.isEmpty()){
				//check for explicit help tags
				if (!action.isEmpty() && NluTools.stringContains(action, "hilfe|info(s|)|information(en|)|kuendige(n|)|kuendigung(en|)|wechsel(n|)")){
					action = "<get_help>";		//maybe this should be default
				}else{
					action = "<get_offer>"; 		//default and the only thing supported yet
				}
				possibleScore.set(index, possibleScore.get(index)+1);
			}else{
				action = "<get_offer>"; 		//default and the only thing supported yet
			}
			
			//info - what to offer
			String info = NluTools.stringFindFirst(this_text, "handy.*|mobil.*|festnetz.*|internet.*");
			if (!info.isEmpty()){
				if (NluTools.stringContains(info, "handy.*|mobil.*")){
					info = "<mobile>"; 		//the only thing supported yet
				}
				possibleScore.set(index, possibleScore.get(index)+1);
			}
			
			//type - types like "prepaid" and "landline"
			String type = NluTools.stringFindFirst(this_text, "(pre|pre-|pre )paid.*|festnetz.*|internet.*|normal(er|e|)");
			if (!type.isEmpty() && type.matches("^pre.*")){
				type = "<prepaid>";
			}else if (!type.isEmpty()){
				type = "<landline>";
			}
			//type does not score (?)
			
			HashMap<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.ACTION, action);
				pv.put(PARAMETERS.INFO, info);
				pv.put(PARAMETERS.TYPE, type);
			possibleParameters.add(pv);
		}
		
		//locations
		if (NluTools.stringContains(text, "wo (ist|sind|bin|gibt es|liegt|liegen)|wo (befindet|befinden) sich| wo wir (hier |)sind| wo ich (hier |)bin|wo .* (ist|sind|wohnt|wohne)|"
				+ "(gibt es(?! neues)|kriege ich|finde ich|kann man)\\b.*\\b(hier|in)|"
				+ "(suche|zeig|finde) .*(\\b)("+ RegexParameterSearch.get_POI_list(language) +")|"
				+ "(suche|zeig|finde) .*(\\b)auf (der karte|der map|maps)|"
				//+ "(suche|zeig|finde) .*(\\b)in der naehe$|"			//TODO: broken
				+ "(suche|zeig|finde) auf der (karte|map) .*|"
				+ "adresse .*") 
					&& !possibleCMDs.contains(CMD.HOTELS)){
			String this_text = text;
			possibleCMDs.add(CMD.LOCATION);
			possibleScore.add(1);	index++;
			
			//TODO: make real parameter out of this!
			
			//analyze
			search_locations(this_text, language);
			//place to search
			String place = locations.get("location").trim();
			String poi = locations.get("poi");
			if (place.isEmpty() && poi.isEmpty()){
				if (NluTools.stringContains(this_text, "wo bin ich|wo ich (hier |)bin|wo sind wir|wo wir (hier |)sind")){
					place = "<user_location>";
				}else{
					this_text = this_text.replaceFirst(".*?\\b(wo (ist|sind|liegt|liegen|gibt es|finde ich)( hier|)|wo (befindet|befinden) sich( hier|)|"
							+ "(suche|zeig|finde) auf der (karte|map)|adresse |"
							+ "wo|(suche|zeig|finde))\\b", "").trim();
					this_text = this_text.replaceFirst("^(nach|von)\\b", "").trim();
					this_text = this_text.replaceFirst("^(der|die|das|des|einer|eine|ein|einen|den|dem)\\b", "").trim();
					this_text = this_text.replaceFirst("\\b(ist$|sind$|liegt$|liegen$)", "").trim();
					this_text = this_text.replaceFirst("\\b(auf der karte$|auf der map$|auf maps$)", "").trim();
					this_text = this_text.replaceFirst("\\b(in der naehe$)", "").trim();
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
		if (NluTools.stringContains(text, "saturn|media(\\s|)(markt|maerkte)|amazon") || (NluTools.stringContains(text, "tv(?! (programm|zeitung))|fernseher")	
							&& NluTools.stringContains(text, "suche(n|)|finde(n|)|zeig(en|)|kauf(en|)"))
			){
			String this_text = text;
			possibleCMDs.add(CMD.SEARCH_RETAIL);
			possibleScore.add(1);	index++;
			//increase score for these by 2
			if (NluTools.stringContains(this_text, "(saturn|media(\\s|)(markt|maerkte)|amazon)")){
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
			}else if (NluTools.stringContains(this_text, "media(\\s|)(markt|maerkte)")){
				retail_type = "<media_markt>";
			}else if (NluTools.stringContains(this_text, "amazon")){
				retail_type = "<amazon>";
			}
			
			//convert units to default - handle special chars
			//--price to €
			price = RegexParameterSearch.convert_amount_to_default("money", price_type, price, language);
			
			//search term
			String search = NluTools.stringFindFirst(this_text, "(nach |ein |einen |einem |eine ).*");
			search = search.replaceAll("^(nach|ein|einen|einem|eine)\\b", "");
			search = search.replaceAll("\\b(und|mit|fuer|unter|ueber|bei|in|von|bis|der|die|das|nicht|weniger|mehr|maximum|maximal|minimum|minimal) .*", "").trim();
			String junk = NluTools.stringFindFirst(search, "saturn|media(\\s|)(markt|maerkte)|amazon");
			if (!junk.isEmpty()){
				search = search.replaceAll("(ein |einem |eine |einen |das |dem |)" + Pattern.quote(junk), "").replaceAll("\\s+", " ").trim();
			}
			search = search.replaceAll("^(ein|einem|eine|einen|der|die|das|dem|den)\\b","").trim();
			
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
		if (NluTools.stringContains(text, "autoscout|autoscout24") || (NluTools.stringContains(text, "auto(s|)|motorrad|motorraeder(n|)")	
							&& NluTools.stringContains(text, "suche(n|)|finde(n|)|zeig(en|)|kauf(en|)|brauch(en|e|)"))
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
			if (NluTools.stringContains(this_text, "motorrad|motorraeder(n|)")){
				vehicle_type = "B";
			}else if (NluTools.stringContains(this_text, "auto(s|)")){
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
			String search = NluTools.stringFindFirst(this_text, "(nach |ein |einen |einem |eine ).*");
			search = search.replaceAll("^(nach|ein|einen|einem|eine)\\b", "");
			search = search.replaceAll("\\b(und|mit|fuer|unter|ueber|in|bei|von|bis|der|die|das|nicht|weniger|mehr|maximum|maximal|minimum|minimal) .*", "").trim();
			String junk = NluTools.stringFindFirst(search, "motorrad|motorraeder(n|)|auto(s|)");
			if (!junk.isEmpty()){
				search = search.replaceAll("(ein |einem |eine |einen |das |dem |)" + Pattern.quote(junk), "").replaceAll("\\s+", " ").trim();
			}
			if (NluTools.stringContains(this_text, "(modell|marke)( ist| sind|) (egal|unwichtig|spielt keine rolle)")){
				search = "<all>";
			}else if (search.isEmpty() && NluTools.stringContains(this_text, "(modell|marke)( ist| sind|) \\w+")){
				search = this_text.replaceFirst(".*\\b(modell|marke)( ist| sind| soll|) ((\\w+)(-\\w+|))\\b.*", "$3");
			}
			search = search.replaceAll("^(ein|einem|eine|einen|der|die|das|dem|den)\\b","").trim();
			
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
		if (NluTools.stringContains(text, "bank|banking|bankkonto|kontostand|banktransfer|bankueberweisung|geld|"
						+ "ueberweisen|ueberweise|ueberweisung|bezahlen|bezahle|bezahlung|zahle(n|)|"
						+ "(sende|schicke) .*(€|$|eur\\b|euro(s|)|dollar|pfund)|"
						+ "(€|$|eur\\b|euro(s|)|dollar|pfund).* (senden|gesendet|ueberwiesen|schicken)")){
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
			
			Map<String, String> pv = new HashMap<String, String>();
				pv.put(PARAMETERS.ACTION, action);
				pv.put(PARAMETERS.NUMBER, num);
				pv.put(PARAMETERS.CURRENCY, currency);
				pv.put(PARAMETERS.RECEIVER, receiver);
			possibleParameters.add(pv);
		}
		
		//status update - TODO: GERMAN EXCLUSIVE RIGHT NOW
		if (NluTools.stringContains(text, "lagebericht|statusbericht|statusupdate|(lage|status) bericht|status update|"
							+ "(aktueller|aktuellen) ueberblick|aktuelle lage|ueberblick .* lage")){
			//add
			possibleCMDs.add(CMD.CAR_WELCOME_UPDATE);
			possibleScore.add(1);	index++;
			//no parameters ... yet
			Map<String, String> pv = new HashMap<>();
			possibleParameters.add(pv);
		}
		
		//----- CUSTOM SERVICES -----
		
		//Abstract analyzer (should come at the end because of lower priority?)
		List<ServiceInterface> customServices = ConfigServices.getCustomServicesList(input, input.user);
		for (ServiceInterface service : customServices){
			index = NluKeywordAnalyzer.abstractRegExAnalyzer(text, input, service,
					possibleCMDs, possibleScore, possibleParameters, index);
		}
		
		//----- ASSISTANT SDK SERVICES -----
		
		//Abstract analyzer (should come at the end because of lower priority?)
		if (Config.enableSDK){
			List<ServiceInterface> assistantServices = ConfigServices.getCustomServicesList(input, Config.getAssistantUser());
			for (ServiceInterface service : assistantServices){
				index = NluKeywordAnalyzer.abstractRegExAnalyzer(text, input, service,
						possibleCMDs, possibleScore, possibleParameters, index);
			}
		}
		
		//---------------------------
		
		//Control Devices/Programs --- 
		
		//TODO: this is kind of a "if everything else failed try this" method 
		//we need a complete rework here ... IMPROVE IT!
		
		if (!possibleCMDs.contains(CMD.KNOWLEDGEBASE) && !possibleCMDs.contains(CMD.WEB_SEARCH)  
										&& !possibleCMDs.contains(CMD.DIRECTIONS) 	&& !possibleCMDs.contains(CMD.TV_PROGRAM)){
										//&& !possibleCMDs.contains(CMD.MUSIC) 		&& !possibleCMDs.contains(CMD.MUSIC_RADIO)){
			String this_text = text;
			//search for control parameters
			search_control_parameters(this_text, language);
			String info = controls.get("control_info"); 		//e.g. location
			String action = controls.get("control_action");
			String number = controls.get("control_number");
			String item = controls.get("control_type"); 		//e.g. type
			if (!item.isEmpty()){
				//add
				possibleCMDs.add(CMD.CONTROL);
				possibleScore.add(1);	index++;
				//scores
				if (!info.isEmpty()){
					possibleScore.set(index, possibleScore.get(index)+1);
				}
				if (!number.isEmpty()){
					//if (item.contains("<device_music>") && !info.isEmpty()){	//as there is a lot of cross-talk with music/radio we require at least a room here
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
		if (NluTools.stringContains(text, "suche(n|)|finde(n|)|zeig(en|)")
				){
			//String this_text = text;
			possibleCMDs.add(CMD.WEB_SEARCH);
			possibleScore.add(1);	index++;

			HashMap<String, String> pv = new HashMap<String, String>(); 		//TODO: pass this down to avoid additional checking
			AbstractParameterSearch aps = new AbstractParameterSearch()
					.setParameters(PARAMETERS.WEBSEARCH_ENGINE, PARAMETERS.WEBSEARCH_REQUEST, PARAMETERS.SEARCH_SECTION)
					.setup(input, pv);
			aps.getParameters();
			//possibleScore.set(index, possibleScore.get(index) + aps.getScore());		//Score stays 1 here!
			possibleParameters.add(pv);
		}
		
		//DEMO Mode
		if (input.demoMode){
			String demoElements = "sportschuhe|sport schuhe|fussballschuhe|"
					+ "t-shirt|shirt|tshirts|t-shirts|shirts|"
					+ "zalando|"
					+ "hotel|hotels|zimmer|"
					+ "fussball|fussballergebnisse|fortuna|"
					+ "angebot|angebote|"
					+ "bei rewe|rewe nach|" 
					+ "brauche|brauchen|bestellung|bestelle|bestellen|kaufen|kaufe|besorgen|"
					+ "zutaten suchen|"
					+ "rezept|rezepte|(zu |man |ein )essen|abendessen|mittagessen|"
					+ "florian";
			
			if (NluTools.stringContains(text, demoElements)){
				String this_text = text;
				possibleCMDs.add("demo_cmds");
				possibleScore.add(1);	index++;
				//demo command
				String info = NluTools.stringFindFirst(this_text, demoElements);
				if (!info.isEmpty()){
					possibleScore.set(index, possibleScore.get(index)+4);
				}
				
				HashMap<String, String> pv = new HashMap<String, String>();
					pv.put("info", info.trim());
				possibleParameters.add(pv);
			}
		}
		
		//--set certainty_lvl--
		int bestScoreIndex = 0;
		if (possibleScore.size()>0){
			int bestScore = Collections.max(possibleScore);
			int totalScore = 0;
			//kind'a stupid double loop but I found no better way to first get total score 
			for (int i=0; i<possibleScore.size(); i++){
				totalScore += possibleScore.get(i);
				//System.out.println("CMD: " + possibleCMDs.get(i)); 		//debug
				//System.out.println("SCORE: " + possibleScore.get(i)); 	//debug
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
		result.certaintyLvl = certainty_lvl;
		//copy the default variables from input (like environment, mood etc.) and add input to result:
		result.setInput(input);
		result.normalizedText = text;	//input has the real text, result has the normalized text
		//you can set some of the default result variables manually if the interpreter changes them:
		result.language = language;		// might well be analyzed and changed by the interpreter, in this case here it must be English
		//result.context = context;		// is auto-set inside the constructor to best command 
		//result.mood = mood;			// typically only APIs change the mood
		
		return result;
	}

	//certainty
	public double getCertaintyLevel(NluResult result) {
		return result.certaintyLvl;
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
			String info = RegexParameterSearch.get_control_location(text, language);
			String action = RegexParameterSearch.get_control_action(text, language)[0];
			String number = RegexParameterSearch.get_number(text);
			String item = RegexParameterSearch.get_control_type(text, language);
			controls.put("control_type", (item!=null)? item : "");
			controls.put("control_action", (action!=null)? action : "");
			controls.put("control_info", (info!=null)? info : "");
			controls.put("control_number", (number!=null)? number : "");
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
			Map<String, String> map = new HashMap<>();
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
			Map<String, String> map = new HashMap<>();
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
