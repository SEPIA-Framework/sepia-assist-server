package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class NewsSection implements ParameterHandler{

	//-----data-----
	//some statics (TODO: should be moved somewhere where every news service has access to them)
	public static enum NSection{
		main,
		science,
		tech,
		politics,
		sports,
		soccer,
		economy,
		games,
		music,
		cinema,
		tv,
		startup;
		//entertainment
		//style
	}
	
	/**
	 * Translate generalized news value (e.g. &lt;science&gt;) to local name (e.g. Wissenschaft).
	 * If generalized value is unknown returns empty string
	 * @param newsValue - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String newsValue, String language){
		String localName = "";
		/*
		if (language.equals(LANGUAGES.DE)){
			localName = colors_de.get(colorValue);
		}else if (language.equals(LANGUAGES.EN)){
			localName = colors_en.get(colorValue);
		}
		if (localName == null){
			Debugger.println("Color.java - getLocal() has no '" + language + "' version for '" + colorValue + "'", 3);
			return "";
		}
		*/
		return localName;
	}
	//----------------
	
	User user;
	String language;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	/**
	 * Search string for news type.
	 */
	public static String get_News_type(String input, String language){
		String type = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "(politik|wirtschaft|wissenschaft|sport|technologie|technik\\w*|tech|kultur)\\w*|it news|pc games|pc news|computer|"
							+ "(fussball|bundesliga|regionalliga|handball|baseball|football|basketball|golf|eishockey|tennis|wrestling)\\w*|"
							+ "champions( |)league|europaliga|euro( |)league|europa( |)league|premier league|primera division|"
							+ "wm|em|(europa|welt)(meister| meister)(schaft|)|"
							+ "racing|auto rennen|rennen|race|raceing|formel 1|formel eins|"
							+ "games|lol|league of legends|dota|overwatch|heroes|"
							+ "musik(er|)|music|band(s|)|kino(s|)|cinema(s|)|film(e|)|movie(s|)|serie(n|)|tv|television|fernseh(serie(n|)|sendung(en|))|fernseh|shows|"
							+ "start( |-|)up(s|)|gruender|gruenderszene");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "politics|economy|science|technology|tech|culture|cultural|sport|sports|computer|it news|pc news|pc games|"
							+ "soccer|"
							+ "bundesliga|champions( |)league|europaliga|euro( |)league|europa( |)league|premier league|primera division|"
							+ "wm|em|(european|world)(champion| champion)(ship|)|"
							+ "baseball|football|basketball|golf|ice hockey|hockey|tennis|handball|wrestling|"
							+ "racing|race|formula one|formula 1|"
							+ "games|lol|league of legends|dota|overwatch|heroes|"
							+ "music|band(s|)|cinema(s|)|movie(s|)|film(s|)|tv|series|serial|television|shows|"
							+ "start( |-|)up(s|)|founder(s|)");
			
		}
		//System.out.println("NewsType: " + type); 		//debug
		return type;
	}

	@Override
	public String extract(String input) {
		String newsFound = get_News_type(input, language);
		this.found = newsFound;
		//TODO: fix this for language and more cases
		if (NluTools.stringContains(newsFound, "(sport|sports|baseball|football|basketball|golf|eishockey|hockey|tennis|handball|wrestling|"
				+ "formula one|formula 1|formel 1|formel eins)\\w*")
				){
			return "<" + NSection.sports.name() + ">";
		}else if (NluTools.stringContains(newsFound, "champions( |)league|europa( |)liga|euro( |)league|europa( |)league|"
				+ "premier league|primera division|(fussball|soccer|bundesliga)\\w*")
				){
			return "<" + NSection.soccer.name() + ">";
		}else if (NluTools.stringContains(newsFound, "politics|politik\\w*")){
			return "<" + NSection.politics.name() + ">";
		}else if (NluTools.stringContains(newsFound, "economy|wirtschaft\\w*")){
			return "<" + NSection.economy.name() + ">";
		}else if (NluTools.stringContains(newsFound, "wissenschaft\\w*|science")){
			return "<" + NSection.science.name() + ">";
		}else if (NluTools.stringContains(newsFound, "tech|technik\\w*|technology|technologie|pc|it|hardware|computer")){
			return "<" + NSection.tech.name() + ">";
		}else if (NluTools.stringContains(newsFound, "games|lol|league of legends|dota|overwatch|heroes")){
			return "<" + NSection.games + ">";
		}else if (NluTools.stringContains(newsFound, "music|musik(er|)|band(s|)")){
			return "<" + NSection.music + ">";
		}else if (NluTools.stringContains(newsFound, "start( |-|)up(s|)|gruender|gruenderszene|founder(s|)")){
			return "<" + NSection.startup + ">";
		}else if (NluTools.stringContains(newsFound, "kino(s|)|cinema(s|)|film(e|s|)|movie(s|)")){
			return "<" + NSection.cinema + ">";
		}else if (NluTools.stringContains(newsFound, "serie(n|)|tv|fernseh(serie(n|)|sendung(en|))|fernseh|series|serial|television")){
			return "<" + NSection.tv + ">";
		}else{
			return "";
		}
		//kultur, cultural, culture
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
			found = "(von |zu |ueber |)(der |die |das |)" + found;
		}else{
			found = "(of |about |for |)(a |the |)" + found;
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(von|zu|ueber|nach)\\b", "").trim();
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)\\b", "").trim();
		}else{
			input = input.replaceAll(".*\\b(of|about|for)\\b", "").trim();
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		
		//expects a section tag!
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
		
		buildSuccess = true;
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
