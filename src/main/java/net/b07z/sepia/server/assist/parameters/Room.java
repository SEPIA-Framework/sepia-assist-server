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
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Parameter to find rooms typically found in a smart home like living-room etc.
 * 
 * @author Florian Quirin
 * 
 */
public class Room implements ParameterHandler{

	//-----data-----
	
	/**
	 * Rooms typically found in a smart home. 
	 */
	public static enum Types{
		livingroom,
		diningroom,
		kitchen,
		bedroom,
		bath,
		study,
		office,
		childsroom,
		garage,
		basement,
		garden,
		shack,
		hallway,
		other,
		unassigned	//must be assigned directly
		//TODO: entrance/front door, veranda(h)/patio/porch/lanai
	}
	
	//Parameter local type names
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		types_de.put("livingroom", "im Wohnzimmer");
		types_de.put("diningroom", "im Esszimmer");
		types_de.put("kitchen", "in der Küche");
		types_de.put("bedroom", "im Schlafzimmer");
		types_de.put("bath", "im Badezimmer");
		types_de.put("study", "im Arbeitszimmer");
		types_de.put("office", "im Büro");
		types_de.put("childsroom", "im Kinderzimmer");
		types_de.put("garage", "in der Garage");
		types_de.put("basement", "im Keller");
		types_de.put("garden", "im Garten");
		types_de.put("shack", "im Schuppen");
		types_de.put("hallway", "im Flur");
		types_de.put("other", "im anderen Zimmer");
		types_de.put("unassigned", "");
		
		types_en.put("livingroom", "in the living room");
		types_en.put("diningroom", "in the dining room");
		types_en.put("kitchen", "in the kitchen");
		types_en.put("bedroom", "in the bedroom");
		types_en.put("bath", "in the bath");
		types_en.put("study", "in the study room");
		types_en.put("office", "in the office");
		types_en.put("childsroom", "in the child's room");
		types_en.put("garage", "in the garage");
		types_en.put("basement", "in the basement");
		types_en.put("garden", "in the garden");
		types_en.put("shack", "in the shack");
		types_en.put("hallway", "in the hallway");
		types_en.put("other", "in the other room");
		types_en.put("unassigned", "");
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
			type = NluTools.stringFindFirst(input, 
					"wohn( |-|)zimmer(n|)|"
					+ "esszimmer(n|)|"
					+ "kueche(n|)|"
					+ "badezimmer(n|)|bad|"
					+ "schlaf( |-|)zimmer(n|)|"
					+ "(arbeits|studier|herren)( |-|)(zimmer|raum|raeumen)|"
					+ "buero(s|)|office|"
					+ "(kinder|baby|wickel)( |-|)(zimmer|stube)(n|)|"
					+ "garage|auto(-| |)schuppen|"
					+ "keller|"
					+ "schuppen|gartenhaus|"
					+ "garten|"
					+ "(haus|)flur|korridor|diele|"
					+ "andere(n|es|r|)( |-|)(zimmer|raum|raeumen)"
				+ "");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, 
					"living( |-|)room(s|)|parlo(u|)r(s|)|lounge(s|)|family(-| )room(s|)|"
					+ "dining( |-|)room(s|)|"
					+ "kitchen(s|)|"
					+ "bath(ing|)( |-|)room(s|)|bath|powder(-|)room(s|)|"
					+ "bed(-|)(room|chamber)(s|)|"
					+ "(study|work)(-|)(room|chamber)(s|)|study|"
					+ "office|"
					+ "(child(s|)|children(s|)|baby)( |-|)room(s|)|nursery|"
					+ "garage|carhouse|"
					+ "basement|"
					+ "shack(s|)|shed(s|)|"
					+ "garden|"
					+ "hallway|corridor|"
					+ "other room(s|)"
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
		
		if (NluTools.stringContains(type, "wohn( |-|)zimmer(n|)|"
				+ "living( |-|)room(s|)|parlo(u|)r(s|)|lounge(s|)|family(-| )room")){
			return "<" + Types.livingroom.name() + ">";
			
		}else if (NluTools.stringContains(type, "esszimmer(n|)|"
				+ "dining( |-|)room(s|)")){
			return "<" + Types.diningroom.name() + ">";
			
		}else if (NluTools.stringContains(type, "kueche(n|)|"
				+ "kitchen(s|)")){
			return "<" + Types.kitchen.name() + ">";
			
		}else if (NluTools.stringContains(type, "badezimmer(n|)|bad|"
				+ "bath(ing|)( |-|)room(s|)|bath|powder(-|)room(s|)")){
			return "<" + Types.bath.name() + ">";
			
		}else if (NluTools.stringContains(type, "schlaf( |-|)zimmer(n|)|"
				+ "bed(-|)(room|chamber)(s|)")){
			return "<" + Types.bedroom.name() + ">";
			
		}else if (NluTools.stringContains(type, "(arbeits|studier|herren)( |-|)(zimmer(n|)|raum|raeumen)|"
				+ "(study|work)(-|)(room|chamber)(s|)|study")){
			return "<" + Types.study.name() + ">";
			
		}else if (NluTools.stringContains(type, "buero(s|)|"
				+ "office")){
			return "<" + Types.office.name() + ">";
			
		}else if (NluTools.stringContains(type, "(kinder|baby|wickel)( |-|)(zimmer|stube)|"
				+ "(child(s|)|children(s|)|baby)( |-|)room(s|)|nursery")){
			return "<" + Types.childsroom.name() + ">";
			
		}else if (NluTools.stringContains(type, "garage|auto(-| |)schuppen|"
				+ "carhouse")){
			return "<" + Types.garage.name() + ">";
			
		}else if (NluTools.stringContains(type, "keller|"
				+ "basement")){
			return "<" + Types.basement.name() + ">";
			
		}else if (NluTools.stringContains(type, "schuppen|gartenhaus|"
				+ "shack(s|)|shed(s|)")){
			return "<" + Types.shack.name() + ">";
			
		}else if (NluTools.stringContains(type, "garten|"
				+ "garden")){
			return "<" + Types.garden.name() + ">";
			
		}else if (NluTools.stringContains(type, "(haus|)flur|korridor|diele|"
				+ "hallway|corridor")){
			return "<" + Types.hallway.name() + ">";
			
		}else if (NluTools.stringContains(type, "andere(n|es|r|)( |-|)(zimmer|raum|raeumen)|"
				+ "other room(s|)")){
			return "<" + Types.other.name() + ">";

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
			found = "(der |die |das |den |dem |(m|)ein(en|em|er|e) |)" + found;
		}else{
			found = "(a |the |)" + found;
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			return input.replaceAll(".*\\b((m|)ein(en|em|er|e|)|der|die|das|den|dem|ne|ner)\\b", "").trim();
		}else{
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers)
		if (Is.notNullOrEmpty(input) && !input.startsWith("<")){
			input = extract(input);
		}
		
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
