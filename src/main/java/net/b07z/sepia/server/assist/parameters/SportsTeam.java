package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class SportsTeam implements ParameterHandler{

	//-----data-----
	
	//cities that might be mentioned together with "played" etc.
	public static final String sportCities_de = "("
			+ "bremen|bremer|"
			+ "muenchen(er|)|"
			+ "hamburg(er|)|"
			+ "berlin(er|)|"
			+ "koeln(er|)|koelle|"
			+ "nuernberg(er|)|"
			+ "dortmund(er|)|"
			+ "gelsenkirchen(er|)|"
			+ "frankfurt(er|)|"
			+ "leipzig(er|)|"
			+ "hoffenheim(er|)|"
			+ "leverkusen(er|)|"
			+ "freiburg(er|)|"
			+ "mainz(er|)|"
			+ "(moenchengladbach|gladbach)(er|)|"
			+ "augsburg(er|)|"
			+ "wolfsburg(er|)|"
			+ "bochum(er|)|"
			+ "darmstadt|darmstaedter|"
			+ "ingolstadt|ingolstaedter|"
			+ "stuttgart(er|)|"
			+ "hannover(aner|)|"
			+ "duesseldorf(er|)|"
			+ "paderborn(er|)|"
			+ "essen kray(er|)|essen(er|)|kray(er|)|"
			+ "duisburg(er|)|"
			+ "istanbul|london|liverpool|manchester|leicester|barcelona|madrid"
			+ ")";
	public static final String sportCities_en = "("
			+ "bremen|"
			+ "munich|"
			+ "hamburg|"
			+ "berlin|"
			+ "cologne|"
			+ "nu(e|)rnberg|"
			+ "dortmund|"
			+ "gelsenkirchen|"
			+ "frankfurt|"
			+ "leipzig|"
			+ "hoffenheim|"
			+ "leverkusen|"
			+ "freiburg|"
			+ "mainz|"
			+ "(moenchengladbach|gladbach)|"
			+ "augsburg|"
			+ "wolfsburg|"
			+ "bochum|"
			+ "darmstadt|darmstaedter|"
			+ "ingolstadt|ingolstaedter|"
			+ "stuttgart|"
			+ "hannover|"
			+ "duesseldorf|"
			+ "paderborn|"
			+ "essen kray|essen|kray|"
			+ "duisburg|"
			+ "istanbul|london|liverpool|manchester|leicester|barcelona|madrid"
			+ ")";
	
	//teams 
	public static final String soccerTeams_de = "("
			+ "(sv |)werder( bremen|)|svw|"
			+ "(fc |)bayern( muenchen|)|fcb|"
			+ "hamburger sv|hsv|"
			+ "hertha( bsc|)( berlin|)|"
			+ "(1\\. |1 |erster |)fc koeln|"
			+ "(1\\. |1 |erster |)fc nuernberg|"
			+ "borussia dortmund|bvb|"
			+ "(fc |)schalke( 04|)|"
			+ "eintracht frankfurt|"
			+ "rb leipzig|"
			+ "tsg (1899 |)hoffenheim|"
			+ "bayer( 04|)( leverkusen|)|"
			+ "(sc |sport(-| )club |)freiburg|"
			+ "(1\\. |1 |erster |)fsv mainz( 05|)|fsv|"
			+ "borussia moenchengladbach|"
			+ "fc augsburg|"
			+ "vfl wolfsburg|"
			+ "vfl bochum|vfl|"
			+ "sv darmstadt( 09|)|"
			+ "fc ingolstadt( 09|)|"
			+ "hannover 96|"
			+ "vfb stuttgart|"
			+ "fortuna duesseldorf|"
			+ "(1\\. |1 |erster |)(fc |)union berlin|"
			+ "sc paderborn( 07|)|"
			+ "(fc |)(st |)pauli|"
			+ "(fc |)kray|rot weiss essen|rwe|"
			+ "msv duisburg|msv|"
			+ "galatasaray|cimbom|cim bom|besiktas|fenerbahce|"
			+ "fc liverpool|fc chelsea|manchester united|manu|manunited|manchester city|mancity|leicester city( fc|)|"
			+ "fc barcelona|real madrid|atletico madrid"
			+ ")";
	public static final String soccerTeams_en = "("
			+ "(sv |)werder( bremen|)|svw|"
			+ "(fc |)bayern( munich|)|fcb|"
			+ "hamburger sv|hsv|"
			+ "hertha( bsc|)( berlin|)|"
			+ "(1\\. |1 |)fc cologne|"
			+ "(1\\. |1 |)fc nu(e|)rnberg|"
			+ "borussia dortmund|bvb|"
			+ "(fc |)schalke( 04|)|"
			+ "eintracht frankfurt|"
			+ "rb leipzig|"
			+ "tsg (1899 |)hoffenheim|"
			+ "bayer( 04|)( leverkusen|)|"
			+ "(sc |sport(-| )club |)freiburg|"
			+ "(1\\. |1 |)fsv mainz( 05|)|fsv|"
			+ "borussia moenchengladbach|"
			+ "fc augsburg|"
			+ "vfl wolfsburg|"
			+ "vfl bochum|vfl|"
			+ "sv darmstadt( 09|)|"
			+ "fc ingolstadt( 09|)|"
			+ "hannover 96|"
			+ "vfb stuttgart|"
			+ "fortuna duesseldorf|"
			+ "(1\\. |1 |)(fc |)union berlin|"
			+ "sc paderborn( 07|)|"
			+ "(fc |)(st |st\\. |)pauli|"
			+ "(fc |)kray|rot weiss essen|rwe|"
			+ "msv duisburg|msv|"
			+ "galatasaray|cimbom|cim bom|besiktas|fenerbahce|"
			+ "fc liverpool|fc chelsea|manchester united|manu|manunited|manchester city|mancity|leicester city( fc|)|"
			+ "fc barcelona|real madrid|atletico madrid"
			+ ")";
	
	
	public static HashMap<String, Long> soccerIDs = new HashMap<>();
	public static HashMap<String, Long> soccerIDs_bundesliga = new HashMap<>();
	public static HashMap<Long, String> shortNames = new HashMap<>();
	static {
		//TODO: requires constant update (see https://www.openligadb.de/api/getmatchdata/bl1/2018/10)
		soccerIDs_bundesliga.put("<sv_werder_bremen>", 		134l);		shortNames.put(134l, "SV Werder");
		soccerIDs_bundesliga.put("<fc_bayern_muenchen>", 	40l);		shortNames.put(40l, "FC Bayern");
		soccerIDs_bundesliga.put("<hamburger_sv>", 			100l);		shortNames.put(100l, "HSV");
		soccerIDs_bundesliga.put("<hertha_bsc_berlin>", 	54l);		shortNames.put(54l, "Hertha");
		soccerIDs_bundesliga.put("<1_fc_koeln>", 			65l);		shortNames.put(65l, "FC Köln");
		soccerIDs_bundesliga.put("<borussia_dortmund>", 	7l);		shortNames.put(7l, "BVB");
		soccerIDs_bundesliga.put("<fc_schalke_04>", 		9l);		shortNames.put(9l, "Schalke");
		soccerIDs_bundesliga.put("<eintracht_frankfurt>", 	91l);		shortNames.put(91l, "Frankfurt");
		soccerIDs_bundesliga.put("<rb_leipzig>", 			1635l);		shortNames.put(1635l, "Leipzig");
		soccerIDs_bundesliga.put("<tsg_1899_hoffenheim>", 	123l);		shortNames.put(123l, "Hoffenheim");
		soccerIDs_bundesliga.put("<bayer_04_leverkusen>", 	6l);		shortNames.put(6l, "Leverkusen");
		soccerIDs_bundesliga.put("<sc_freiburg>", 			112l);		shortNames.put(112l, "Freiburg");
		soccerIDs_bundesliga.put("<fsv_mainz_05>", 			81l);		shortNames.put(81l, "Mainz");
		soccerIDs_bundesliga.put("<borussia_moenchengladbach>", 87l);	shortNames.put(87l, "Gladbach");
		soccerIDs_bundesliga.put("<fc_augsburg>", 			95l);		shortNames.put(95l, "Augsburg");
		soccerIDs_bundesliga.put("<vfl_wolfsburg>", 		131l);		shortNames.put(131l, "Wolfsburg");
		soccerIDs_bundesliga.put("<sv_darmstadt_09>", 		118l);		shortNames.put(118l, "Darmstadt");
		soccerIDs_bundesliga.put("<fc_ingolstadt_09>", 		171l);		shortNames.put(171l, "Ingolstadt");
		soccerIDs_bundesliga.put("<hannover_96>", 			55l);		shortNames.put(55l, "Hannover");
		soccerIDs_bundesliga.put("<vfb_stuttgart>", 		16l);		shortNames.put(16l, "Stuttgart");
		soccerIDs_bundesliga.put("<1_fc_nuernberg>", 		79l);		shortNames.put(79l, "Nürnberg");
		soccerIDs_bundesliga.put("<fortuna_duesseldorf>", 	185l);		shortNames.put(185l, "Düsseldorf");
		soccerIDs_bundesliga.put("<sc_paderborn_07>", 		31l);		shortNames.put(31l, "Paderborn");
		soccerIDs_bundesliga.put("<1_fc_union_berlin>", 	80l);		shortNames.put(80l, "Union");
		
		soccerIDs.putAll(soccerIDs_bundesliga);
	}
	/**
	 * Get the team ID that belongs to a generalized team name (e.g. &lt;hertha_bsc_berlin&gt;).
	 * If there is none known return -1
	 */
	public static long getSoccerTeamID(String name){
		if (soccerIDs.containsKey(name)){
			return soccerIDs.get(name);
		}else{
			Debugger.println("SportsTeam - no ID for team: " + name, 1);
			return -1;
		}
	}
	/**
	 * Check if team is part of the German Bundesliga.
	 */
	public static boolean isBundesligaTeam(String name){
		if (soccerIDs_bundesliga.containsKey(name)){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * Get a short name for a team identified by ID. Is empty if unknown.
	 */
	public static String getShortName(long id){
		String shortName = shortNames.get(id);
		if (shortName != null && !shortName.isEmpty()){
			return shortName;
		}else{
			Debugger.println("SportsTeam - no shortName for team ID: " + id, 1);
			return "";
		}
	}
	/**
	 * Translate generalized value (e.g. &lt;science&gt;) to local name (e.g. Wissenschaft).
	 * If generalized value is unknown returns empty string
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
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
	public static String getSportsTeam(String input, String language){
		String item = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			item = NluTools.stringFindFirst(input, soccerTeams_de);
			if (item.isEmpty()){
				item = NluTools.stringFindFirst(input, sportCities_de + " (gespielt|spielt|gewonnen|gewinnt|verloren|verliert|getroffen|ein tor|tore)");
				item = item.replaceFirst("\\b(gespielt|spielt|gewonnen|gewinnt|verloren|verliert|getroffen|ein tor|tore)\\b", "").trim();
			}
			if (item.isEmpty()){
				item = NluTools.stringFindFirst(input, "(wie|wann) (spielt|spielen) " + sportCities_de);
				item = item.replaceFirst(".*\\b(spielt|spielen)\\b", "").trim();
			}
			if (item.isEmpty()){
				item = NluTools.stringFindFirst(input, sportCities_de + " (gegen|vs)|(gegen|vs) " + sportCities_de);
				item = item.replaceFirst("\\b(gegen|vs)\\b", "").trim();
			}
			
		//English and other
		}else{
			item = NluTools.stringFindFirst(input, soccerTeams_en);
			if (item.isEmpty()){
				item = NluTools.stringFindFirst(input, sportCities_en + " (play(s|)|(has |)played|won|lost|scored|(is |)(winning|losing|playing)|scored)");
				item = item.replaceFirst("\\b(play(s|)|(has |)played|won|lost|scored|(is |)(winning|losing|playing|scored))\\b", "").trim();
			}
			if (item.isEmpty()){
				item = NluTools.stringFindFirst(input, sportCities_en + " (against|vs)|(against|vs) " + sportCities_en);
				item = item.replaceFirst("\\b(against|vs)\\b", "").trim();
			}
		}
		return item;
	}

	@Override
	public String extract(String input) {
		String item = getSportsTeam(input, language);
		if (item.isEmpty()){
			return "";
		}else{
			this.found = item;
		}
		//TODO: we should make this more efficient at some point ^^
		if (NluTools.stringContains(item, "svw|werder|bremen|bremer")){				return "<sv_werder_bremen>";	}
		else if (NluTools.stringContains(item, "bayern|muenchen(er|)|munich|fcb")){	return "<fc_bayern_muenchen>";	}
		else if (NluTools.stringContains(item, "hamburg(er|)|hsv")){					return "<hamburger_sv>";		}
		else if (NluTools.stringContains(item, "hertha|(?<!(union) )berlin(er|)")){		return "<hertha_bsc_berlin>";	}
		else if (NluTools.stringContains(item, "koeln(er|)|koelle")){					return "<1_fc_koeln>";			}
		else if (NluTools.stringContains(item, "nuernberg(er|)")){					return "<1_fc_nuernberg>";			}
		else if (NluTools.stringContains(item, "dortmund(er|)|bvb")){					return "<borussia_dortmund>";	}
		else if (NluTools.stringContains(item, "gelsenkirchen(er|)|schalke")){			return "<fc_schalke_04>";		}
		else if (NluTools.stringContains(item, "frankfurt(er|)")){						return "<eintracht_frankfurt>";	}
		else if (NluTools.stringContains(item, "leipzig(er|)")){						return "<rb_leipzig>";		}
		else if (NluTools.stringContains(item, "tsg|hoffenheim(er|)")){				return "<tsg_1899_hoffenheim>";		}
		else if (NluTools.stringContains(item, "bayer|leverkusen(er|)")){				return "<bayer_04_leverkusen>";		}
		else if (NluTools.stringContains(item, "freiburg(er|)")){						return "<sc_freiburg>";		}
		else if (NluTools.stringContains(item, "fsv|mainz")){							return "<fsv_mainz_05>";		}
		else if (NluTools.stringContains(item, "(moenchengladbach|gladbach)(er|)")){	return "<borussia_moenchengladbach>";	}
		else if (NluTools.stringContains(item, "augsburg(er|)")){						return "<fc_augsburg>";		}
		else if (NluTools.stringContains(item, "wolfsburg(er|)")){						return "<vfl_wolfsburg>";		}
		else if (NluTools.stringContains(item, "vfl|bochum(er|)")){					return "<vfl_bochum>";		}
		else if (NluTools.stringContains(item, "darmstadt|darmstaedter")){				return "<sv_darmstadt_09>";		}
		else if (NluTools.stringContains(item, "ingolstadt|ingolstaedter")){			return "<fc_ingolstadt_09>";		}
		else if (NluTools.stringContains(item, "hannover(aner|)")){					return "<hannover_96>";		}
		else if (NluTools.stringContains(item, "stuttgart(er|)")){						return "<vfb_stuttgart>";		}
		else if (NluTools.stringContains(item, "fortuna|duesseldorf(er|)")){			return "<fortuna_duesseldorf>";		}
		else if (NluTools.stringContains(item, "paderborn(er|)")){						return "<sc_paderborn_07>";		}
		else if (NluTools.stringContains(item, "union|(?<!(hertha|bsc) )berlin(er|)")){		return "<1_fc_union_berlin>";		}
		
		else if (NluTools.stringContains(item, "kray(er|)")){							return "<fc_kray>";		}
		else if (NluTools.stringContains(item, "essen(er|)|rwe")){						return "<rot_weiss_essen>";		}
		else if (NluTools.stringContains(item, "duisburg(er|)|msv")){					return "<msv_duisburg>";		}
		
		else if (NluTools.stringContains(item, "galatasaray|cimbom|cim bom|istanbul")){return "<galatasaray_istanbul>";		}
		else if (NluTools.stringContains(item, "besiktas")){							return "<besiktas_istanbul>";		}
		else if (NluTools.stringContains(item, "fenerbahce")){							return "<fenerbahce_istanbul>";		}
		else if (NluTools.stringContains(item, "liverpool(er|)")){						return "<fc_liverpool>";		}
		else if (NluTools.stringContains(item, "london(er|)|chelsea")){				return "<fc_chelsea>";		}
		else if (NluTools.stringContains(item, "manchester|united|mancity|manu")){		return "<manchester_united>";		}
		else if (NluTools.stringContains(item, "manchester city|mancity")){			return "<manchester_city>";		}
		else if (NluTools.stringContains(item, "leicester")){							return "<leicester_city_fc>";		}
		else if (NluTools.stringContains(item, "barcelona")){							return "<fc_barcelona>";		}
		else if (NluTools.stringContains(item, "real|madrid")){						return "<real_madrid>";		}
		else if (NluTools.stringContains(item, "atletico madrid")){					return "<atletico_madrid>";		}
		else{
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
		//German
		if (language.matches(LANGUAGES.DE)){
			found = "(gegen |vs |)" + found + " (gespielt|spielt|gewonnen|gewinnt|verloren|verliert|getroffen|)( gegen| vs|)";
			return NluTools.stringRemoveFirst(input, found);
			
		//English and other
		}else{
			found = "(against |vs |)" + found + " (play(s|)|(has |)played|won|lost|scored|(is |)(winning|losing|playing)|)( against| vs|)";
			return NluTools.stringRemoveFirst(input, found);
		}
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
