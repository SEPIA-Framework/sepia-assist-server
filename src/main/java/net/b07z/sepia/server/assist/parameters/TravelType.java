package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class TravelType implements Parameter_Handler{

	//-----data-----
	public static HashMap<String, String> travelTypes_de = new HashMap<>();
	public static HashMap<String, String> travelTypes_en = new HashMap<>();
	static {
		travelTypes_de.put("<transit>", "mit öffentlichen Verkehrsmitteln");
		travelTypes_de.put("<bicycling>", "mit dem Fahrrad");
		travelTypes_de.put("<driving>", "mit dem Auto");
		travelTypes_de.put("<walking>", "zu Fuß");
		travelTypes_de.put("<flying>", "mit dem Flugzeug");
		travelTypes_de.put("<shared>", "mit der Mitfahrgelegenheit");
		travelTypes_de.put("<all>", "");
		
		travelTypes_en.put("<transit>", "by public transport");
		travelTypes_en.put("<bicycling>", "by bike");
		travelTypes_en.put("<driving>", "by car");
		travelTypes_en.put("<walking>", "by walking");
		travelTypes_en.put("<flying>", "by plane");
		travelTypes_en.put("<shared>", "by a shared ride");
		travelTypes_en.put("<all>", "");
	}
	/**
	 * Translate generalized value (e.g. &lttransit&gt) to local speakable name (e.g. "by public transport").
	 * If generalized value is unknown returns empty string
	 * @param travelValue - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String travelValue, String language){
		if (travelValue == null || travelValue.isEmpty()){
			return "";
		}
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = travelTypes_de.get(travelValue);
		}else if (language.equals(LANGUAGES.EN)){
			localName = travelTypes_en.get(travelValue);
		}
		if (localName == null){
			Debugger.println("TravelType.java - getLocal() has no '" + language + "' version for '" + travelValue + "'", 3);
			return "";
		}
		return localName;
	}
	//----------------
		
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
		String travelType = "";
		if (language.equals(LANGUAGES.DE)){
			travelType = NluTools.stringFindFirst(input, "zug|fernbus|reisebus|bus|ice|deutsche(n|) bahn|(s-|u-|s|u|)bahn|strassenbahn|subway|tram|nahverkehr|oeffentlichen|"
														+ "(\\w*bahn|bus|zug)verbindung|"
														+ "fahrrad|bike|"
														+ "flugzeug|fluege|flug|fliegen|"
														+ "auto|fahren|automobil|"
														+ "zu fuss|laufen|"
														+ "mitfahrgelegenheit");
			found = travelType;
			if (!travelType.isEmpty()){
				//transform to google default types
				if (NluTools.stringContains(travelType, "zug|fernbus|reisebus|bus|ice|deutsche(n|) bahn|(s-|u-|s|u|)bahn|strassenbahn|subway|tram|nahverkehr|oeffentlichen|"
														+ "(\\w*bahn|bus|zug)verbindung")){
					travelType = "<transit>";
				}else if (NluTools.stringContains(travelType, "fahrrad|bike")){
					travelType = "<bicycling>";
				}else if (NluTools.stringContains(travelType, "auto|automobil|fahren")){
					travelType = "<driving>";
				}else if (NluTools.stringContains(travelType, "zu fuss|laufen")){
					travelType = "<walking>";
				}else if (NluTools.stringContains(travelType, "fliegen|flug|fluege|flugzeug")){
					travelType = "<flying>";
				}else if (NluTools.stringContains(travelType, "mitfahrgelegenheit")){
					travelType = "<shared>";
				//default
				}else{
					travelType = "";
				}
			}
		}else{
			travelType = NluTools.stringFindFirst(input, "train|tram|bus|rail|transit|public transport|ice|deutsche bahn|s-bahn|bike|bicycle|bicycling|plane|flights|flight|fly|flying|car|drive|driving|automobile|by foot|on foot|afoot|walk|walking|shared ride|ride");
			found = travelType;
			if (!travelType.isEmpty()){
				//transform to google default types
				if (NluTools.stringContains(travelType, "train|tram|bus|rail|transit|public transport")){
					travelType = "<transit>";
				}else if (NluTools.stringContains(travelType, "bike|bicycle|bicycling")){
					travelType = "<bicycling>";
				}else if (NluTools.stringContains(travelType, "car|automobile|driving")){
					travelType = "<driving>";
				}else if (NluTools.stringContains(travelType, "by foot|on foot|afoot|walk|walking")){
					travelType = "<walking>";
				}else if (NluTools.stringContains(travelType, "plane|flights|flight|fly|flying")){
					travelType = "<flying>";
				}else if (NluTools.stringContains(travelType, "shared ride|ride")){
					travelType = "<shared>";
				//default
				}else{
					travelType = "";
				}
			}
		}
		return travelType;
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
			input = input.replaceFirst("\\b(und |)(mit dem |mit der |mit |)(" + Pattern.quote(found) + ")", "").trim();
		}else{
			input = input.replaceFirst("\\b(and |)(by |with the |with |)(" + Pattern.quote(found) + ")", "").trim();
		}
		return null;
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(mit)\\b", "").trim();
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|dem|ne|ner|meiner|meinem|meine)\\b", "").trim();
		}else{
			input = input.replaceAll(".*\\b(with|by)\\b", "").trim();
			return input.replaceAll(".*\\b(a|the|my)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//expects a color tag!
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
			//TODO: add ACCOUNT here?
		
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
