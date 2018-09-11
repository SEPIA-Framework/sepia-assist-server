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

public class Room implements ParameterHandler{

	//-----data-----
	
	//Parameter types
	public static enum Types{
		livingroom,
		kitchen,
		bedroom,
		bath,
		study,
		garage,
		shack;
	}
	
	//Parameter local type names
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		types_de.put("livingroom", "im Wohnzimmer");
		types_de.put("kitchen", "in der Küche");
		types_de.put("bedroom", "im Schlafzimmer");
		types_de.put("bath", "im Badezimmer");
		types_de.put("study", "im Arbeitszimmer");
		types_de.put("garage", "in der Garage");
		types_de.put("shack", "im Schuppen");
		
		types_en.put("livingroom", "in the living room");
		types_en.put("kitchen", "in the kitchen");
		types_en.put("bedroom", "in the bedroom");
		types_en.put("bath", "in the bath");
		types_en.put("study", "in the study room");
		types_en.put("garage", "in the garage");
		types_en.put("shack", "in the shack");
	}
	
	/**
	 * Translate generalized value (e.g. &kitchen&gt) to a context based, useful local name (e.g. in der Küche).
	 * If generalized value is unknown returns empty string
	 * @param type - generalized type value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String type, String language){
		type = type.replaceAll("^<|>$", "").trim();
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = types_de.get(type);
		}else if (language.equals(LANGUAGES.EN)){
			localName = types_en.get(type);
		}
		if (localName == null){
			Debugger.println(Room.class.getSimpleName() + " - getLocal() has no '" + language + "' version for '" + type + "'", 3);
			return "";
		}
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
	 * Search normalized string for raw type.
	 */
	public static String getType(String input, String language){
		String type = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, "wohnzimmer(n|)|"
					+ "kueche(n|)|"
					+ "badezimmer(n|)|bad|"
					+ "schlafzimmer(n|)|"
					+ "(arbeits|studier|herren)(zimmer|raum|raeumen)|"
					+ "garage|auto(-| |)schuppen|"
					+ "schuppen|gartenhaus"
				+ "");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "living( |-|)room(s|)|parlo(u|)r(s|)|lounge(s|)|family(-| )room|"
					+ "kitchen(s|)|"
					+ "bath(ing|)( |-|)room(s|)|bath|powder(-|)room(s|)|"
					+ "bed(-|)(room|chamber)(s|)|"
					+ "(study|work)(-|)(room|chamber)(s|)|study|"
					+ "garage|carhouse|"
					+ "shack(s|)|shed(s|)"
				+ "");
			
		}
		//System.out.println("Type: " + type); 		//debug
		return type;
	}

	@Override
	public String extract(String input) {
		String type = getType(input, language);
		this.found = type;
		
		//classify into types:
		
		if (NluTools.stringContains(type, "wohnzimmer(n|)|"
				+ "living( |-|)room(s|)|parlo(u|)r(s|)|lounge(s|)|family(-| )room")){
			return "<" + Types.livingroom.name() + ">";
			
		}else if (NluTools.stringContains(type, "kueche(n|)|"
				+ "kitchen(s|)")){
			return "<" + Types.kitchen.name() + ">";
			
		}else if (NluTools.stringContains(type, "badezimmer(n|)|bad|"
				+ "bath(ing|)( |-|)room(s|)|bath|powder(-|)room(s|)")){
			return "<" + Types.bath.name() + ">";
			
		}else if (NluTools.stringContains(type, "schlafzimmer(n|)|"
				+ "bed(-|)(room|chamber)(s|)")){
			return "<" + Types.bedroom.name() + ">";
			
		}else if (NluTools.stringContains(type, "(arbeits|studier|herren)(zimmer|raum|raeumen)|"
				+ "(study|work)(-|)(room|chamber)(s|)|study")){
			return "<" + Types.study.name() + ">";
			
		}else if (NluTools.stringContains(type, "garage|auto(-| |)schuppen|"
				+ "carhouse")){
			return "<" + Types.garage.name() + ">";
			
		}else if (NluTools.stringContains(type, "schuppen|gartenhaus|"
				+ "shack(s|)|shed(s|)")){
			return "<" + Types.shack.name() + ">";
		
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
			found = "(der |die |das |den |)" + found;
		}else{
			found = "(a |the |)" + found;
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)\\b", "").trim();
		}else{
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		
		//expects a type
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
