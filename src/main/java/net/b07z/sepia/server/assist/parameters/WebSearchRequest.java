package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class WebSearchRequest implements ParameterHandler{

	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nlu_input;
	
	String found = "";
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nlu_input = nluInput;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nlu_input = nluResult.input;
	}
	
	@Override
	public String extract(String input) {
		String search = "";
		String engine = WebSearchEngine.names;
		
		//common prep.
		input = input.replaceFirst("^www($| )", "").trim();	//"www" is a shortcut for the search service
		
		if (language.equals(LANGUAGES.DE)){
			if (NluTools.stringContains(input, "^" + engine)){
				search = input.replaceFirst("^" + engine + "( suche|)\\b", "").trim();
				
			}else if (NluTools.stringContains(input, "((durch|)suche)( mit(tels|)| auf| ueber| bei| via| per| )" + engine + "( nach| fuer| auf)")){
				search = input.replaceFirst(".*? (nach|fuer|auf)\\b", "").trim();
				
			}else if (NluTools.stringContains(input, "(suche|finde|zeig|schau|suchen nach) .* (mit|per|via|auf|ueber|mittels|bei) " + engine + "$")){
				search = input.replaceFirst(".*?\\b(suche|finde|zeig|schau|suchen nach)\\b", "").trim();
				search = search.replaceFirst("(mit|per|via|auf|ueber|mittels|bei) " + engine + "$", "").trim();
			}else{
				search = input.replaceFirst(".*?\\b(websuche|web suche|(durchsuche|suche|suchen nach|schau|finde|zeig)( mir|)( mal| bitte|)( bitte| mal|))\\b", "").trim();
				search = search.replaceFirst("^(mit|per|via|auf|ueber|mittels|bei) " + engine + "\\b|^(im|das) (web|internet)\\b", "").trim();
				search = search.replaceFirst("^(mit|per|via|auf|ueber|mittels|bei) " + engine + "\\b|^(im|das) (web|internet)\\b", "").trim();
			}
			//clean up
			search = search.replaceFirst("^(mir)", "").trim();
			search = search.replaceFirst("^(mal bitte|bitte mal|mal|bitte)", "").trim();
			search = search.replaceFirst("^(online nach|nach|.*\\b(suchen( (im|das) (web|internet) | )nach))\\b", "").trim();
			//some adaptations
			/*
			search = search.replaceFirst("^(bild(ern|er|)|rezept(en|e|)|video(s|)|movie(s|)|film(en|e|)|aktie(n|)|buecher(n|)|buch) (von|vom|ueber|mit|)|"
					+ " (bild(ern|er|)|rezept(en|e|)|video(s|)|movie(s|)|film(en|e|)|aktie(n|)|buecher(n|)|buch)$", "");
			*/
			
			
		}else if (language.equals(LANGUAGES.EN)){
			if (NluTools.stringContains(input, "^" + engine)){
				search = input.replaceFirst("^" + engine + "( search|)", "").trim();
				
			}else if (NluTools.stringContains(input, "(search)( with| on| over| by| via| per| )" + engine + "( for| after)")){
				search = input.replaceFirst(".*? (for|after)\\b", "").trim();
				
			}else if (NluTools.stringContains(input, "(search|find|show|look|searching|looking) .* (with|on|via|per|over|by) " + engine + "$")){
				search = input.replaceFirst(".*?\\b(search|find|show|look|searching|looking)", "").trim();
				search = search.replaceFirst("(with|on|via|per|over|by) " + engine + "$", "").trim();
			}else{
				search = input.replaceFirst(".*?\\b(websearch|web search|search the web|(search for|look for|show|search|find)( me|))", "").trim();
				search = search.replaceFirst("^(with|on|via|per|over|by) " + engine + "|^(on |)the (web|internet)", "").trim();
				search = search.replaceFirst("^(with|on|via|per|over|by) " + engine + "|^(on |)the (web|internet)", "").trim();
			}
			//clean up
			search = search.replaceFirst("^(me)", "").trim();
			search = search.replaceFirst("^(online for|for)", "").trim();
			//some adaptations
			/*
			search = search.replaceFirst("^(picture(s|)|recipe(s|)|video(s|)|movie(s|)|film(s|)|share(s|)|stock(s|)|book(s|)) (of|with|by|)|"
					+ " (picture(s|)|recipe(s|)|video(s|)|movie(s|)|film(s|)|share(s|)|stock(s|)|book(s|))$", "");
			*/	
			
		}else{
			Debugger.println("WebSearch - missing language support for: " + language, 1);
			return "";
		}
		
		//reconstruct original phrase
		Normalizer normalizer = Config.inputNormalizers.get(language);
		search = normalizer.reconstructPhrase(nlu_input.textRaw, search);
		
		return search;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}

	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst("\\b(" + Pattern.quote(found) + ")\\b", "").trim();
		}else{
			input = input.replaceFirst("\\b(" + Pattern.quote(found) + ")\\b", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//build reduced version
		String reduced = "";
		if (language.equals(LANGUAGES.DE)){
			if (input.matches("(?i)^(wie|wo) .* die .*aktie(n|)$")){
				reduced = input.replaceFirst("(?i).* die (.*?)(-| |)aktie(n|)", "$1");
			}else{
				reduced = input.replaceFirst("(?i)^(bild(ern|er|)|rezept(en|e|)|"
							+ "video(s|)|movie(s|)|film(en|e|)|"
							+ "lied(ern|er|)|musik|song(s|)|"
							+ "aktie(n|)|aktien(kurs|wert)|buecher(n|)|buch)"
							+ " (von|vom|ueber|mit|fuer|)|"
					+ "(?i) (bild(ern|er|)|rezept(en|e|)|"
							+ "video(s|)|movie(s|)|film(en|e|)|"
							+ "lied(ern|er|)|musik|song(s|)|"
							+ "(-|)aktie(n|)|buecher(n|)|buch)"
							+ "$|"
					+ "(?i)^((wie|wo) (ist|steht|stehen) (der|die) (aktienkurs|aktienwert|aktie(n|)|kurs|wert) (von|vom|der|))", "").trim();
			}
		}else if (language.equals(LANGUAGES.EN)){
				reduced = input.replaceFirst("(?i)^(picture(s|)|recipe(s|)|"
							+ "video(s|)|movie(s|)|film(s|)|"
							+ "song(s|)|music|"
							+ "share(s|)|stock(s|)|book(s|))"
							+ " (of|with|by|for|)|"
					+ "(?i) (picture(s|)|recipe(s|)|"
							+ "video(s|)|movie(s|)|film(s|)|"
							+ "song(s|)|music|"
							+ "share(s|)|stock(s|)|book(s|))"
							+ "$|"
					+ "(?i)^(what is the (stock|share) (value|price) (of|))", "").trim();
		}
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			JSON.add(itemResultJSON, InterviewData.VALUE_REDUCED, reduced); 		//TODO: should we get the reduced value already in extract?
		
		buildSuccess = true;
		//System.out.println("build: " + reduced); 	//Debug
		return itemResultJSON.toJSONString();
	}
	
	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
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
	
}
