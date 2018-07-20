package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class SportsLeague implements ParameterHandler{

	//-----data-----
	
	//TODO: make local variables 
	public static final String BUNDESLIGA = "bundesliga";
	public static final String BUNDESLIGA_2 = "2_bundesliga";
	public static final String DFB_POKAL = "dfb_pokal";
	public static final String CHAMPIONS_LEAGUE = "champions_league";
	public static final String PRIMERA_DIVISION = "primera_division";
	public static final String PREMIER_LEAGUE = "premier_league";
	
	/*
	public static HashMap<String, String> colors_de = new HashMap<>();
	public static HashMap<String, String> colors_en = new HashMap<>();
	static {
		colors_de.put("<blue>", "blau");
				
		colors_en.put("<blue>", "blue");
	}
	*/
	/**
	 * Translate generalized news value (e.g. &ltscience&gt) to local name (e.g. Wissenschaft).
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
	 * Search string for league.
	 */
	public static String getLeague(String input, String language){
		String type = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "(1\\. |1 |erste(n|) |2\\. |2 |zweite(n|) |3\\. |3 |dritte(n|) |)(bundesliga\\w*|deutsche(n|) liga)|"
					+ "regionalliga|(dfb |dfb-|deutsche(r|n|m|) |)pokal|fa cup|englische(m|n|) pokal|champions( |-|)league|europa( |-|)liga|euro( |-|)league|europa( |-|)league|"
					+ "premier league|englische(n|) liga|primera division|la liga|spanische(n|) liga|serie a|seria a|italienische(n|) liga|"
					+ "ligue 1|franzoesische(n|) liga|primeira liga|portugiesische(n|) liga|eredivisie|(hollaendische|niederlaendische)(n|) liga|"
					+ "sueperlig|sueper lig(a|)|tuerkische(n|) liga|"
					+ "wm|em|(europa|welt)(meister| meister)(schaft|)");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "(1\\. |1 |first |2\\. |2 |second |2nd |3\\. |3 |third |3rd |)(bundesliga\\w*|german league)|"
					+ "german regional league|(dfb|german) (pokal|cup)|(fa |english |)cup|champions( |-|)league|euro( |-|)league|europa( |-|)league|"
					+ "premier league|english league|primera division|la liga|spanish league|(serie|seria|series) a|italian league|"
					+ "ligue 1|french league|primeira liga|portuguese league|eredivisie|(dutch|neatherland(s|)) league|"
					+ "sueperlig|sueper (lig|league)|turkish league|"
					+ "wm|em|(european|world)(championship| championchip)");
			
		}
		//System.out.println("NewsType: " + type); 		//debug
		return type;
	}

	@Override
	public String extract(String input) {
		String leagueFound = getLeague(input, language);
		this.found = leagueFound;
		//TODO: add more cases
		if (NluTools.stringContains(leagueFound, "(2\\. |2 |zweite(n|) |second |2nd )(bundesliga\\w*|deutsche(n|) liga|german league)")){
			return "<" + BUNDESLIGA_2 + ">";
		}else if (NluTools.stringContains(leagueFound, "(3\\. |3 |dritte(n|) |third |3rd )(bundesliga\\w*|deutsche(n|) liga|german league)")){
			return "<" + "3_bundesliga" + ">";
		}else if (NluTools.stringContains(leagueFound, "bundesliga\\w*|deutsche(n|) liga|german league")){
			return "<" + BUNDESLIGA + ">";
		}else if (NluTools.stringContains(leagueFound, "regionalliga")){
			return "<" + "regionalliga" + ">";
		}else if (NluTools.stringContains(leagueFound, "(dfb |dfb-|deutsche(r|n|m|) |german )(pokal|cup)|pokal")){
			return "<" + DFB_POKAL + ">";
		}else if (NluTools.stringContains(leagueFound, "(fa |english )cup|englische(m|n|) pokal|cup")){
			return "<" + "fa_cup" + ">";
		}else if (NluTools.stringContains(leagueFound, "champions( |-|)league")){
			return "<" + CHAMPIONS_LEAGUE + ">";
		}else if (NluTools.stringContains(leagueFound, "europa( |-|)liga|euro( |-|)league|europa( |-|)league")){
			return "<" + "euro_league" + ">";
		}else if (NluTools.stringContains(leagueFound, "premier league|englische(n|) liga|english league")){
			return "<" + PREMIER_LEAGUE + ">";
		}else if (NluTools.stringContains(leagueFound, "primera division|la liga|spanische(n|) liga|spanish league")){
			return "<" + PRIMERA_DIVISION + ">";
		}else if (NluTools.stringContains(leagueFound, "(serie|seria|series) a|italian league|italienische(n|) liga")){
			return "<" + "serie_a" + ">";
		}else if (NluTools.stringContains(leagueFound, "ligue 1|franzoesische(n|) liga|french league")){
			return "<" + "ligue_1" + ">";
		}else if (NluTools.stringContains(leagueFound, "primeira liga|portugiesische(n|) liga|portuguese league")){
			return "<" + "primeira_liga" + ">";
		}else if (NluTools.stringContains(leagueFound, "eredivisie|(hollaendische|niederlaendische)(n|) liga|(dutch|neatherland(s|)) league")){
			return "<" + "eredivisie" + ">";
		}else if (NluTools.stringContains(leagueFound, "sueperlig|sueper lig(a|)|tuerkische(n|) liga|sueper (lig|league)|turkish league")){
			return "<" + "sueperlig" + ">";
		}else if (NluTools.stringContains(leagueFound, "wm|welt(meister| meister)(schaft|)|world(championship| championchip)")){
			return "<" + "world_championchip" + ">";
		}else if (NluTools.stringContains(leagueFound, "em|europa(meister| meister)(schaft|)|european(championship| championchip)")){
			return "<" + "european_championchip" + ">";
		}else{
			return "";
		}
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
		//TODO: check for my favorite color?
		
		//expects a color tag!
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
		if (input.matches("^\\{\".*\":.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
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
