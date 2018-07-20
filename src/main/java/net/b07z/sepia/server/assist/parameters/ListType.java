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
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.IndexType;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Handler for different types of lists.
 * 
 * @author Florian Quirin
 *
 */
public class ListType implements ParameterHandler{
	
	//-------data-------
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		//note: should be identical to UserDataList Types
		types_de.put("<shopping>", "Shopping Liste");
		types_de.put("<todo>", "To-do Liste");
		types_de.put("<reminders>", "Erinnerungen");
		types_de.put("<alarms>", "Wecker");
		types_de.put("<newsFavorites>", "Favoriten");
		types_de.put("<unknown>", "Liste");
		types_de.put("<list>", "Liste");
				
		types_en.put("<shopping>", "shopping list");
		types_en.put("<todo>", "to-do list");
		types_en.put("<reminders>", "reminders");
		types_en.put("<alarms>", "alarms");
		types_en.put("<newsFavorites>", "favorites");
		types_en.put("<unknown>", "list");
		types_en.put("<list>", "list");
	}
	/**
	 * Translate generalized value.
	 * If generalized value is unknown returns empty string.
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = types_de.get(value);
		}else if (language.equals(LANGUAGES.EN)){
			localName = types_en.get(value);
		}
		if (localName == null){
			Debugger.println("ListType.java - getLocal() has no '" + language + "' version for '" + value + "'", 3);
			return "";
		}
		return localName;
	}
	//------------------

	User user;
	NluInput nluInput;
	String language;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	//String subType = ""; 	//TODO: consider to move list subType to an extra parameter handler - new: MOVED
	
	@Override
	public void setup(NluInput nluInput) {
		this.nluInput = nluInput;
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.nluInput = nluResult.input;
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	@Override
	public boolean isGeneric(){
		return false;
	}

	@Override
	public String extract(String input) {
		String type = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.LIST_TYPE);
		if (pr != null){
			type = pr.getExtracted();
			this.found = pr.getFound();
			
			return type;
		}
		
		//String[] subTypeRes = null;
		String extracted = "";
		String todo, todoOther, shopping, shopOther, unknownList;
		
		//note: if you add stuff here check ListSubType as well!
		
		//German
		if (language.matches(LANGUAGES.DE)){
			todo = "(to(-| |)do(-| |)|(zu |)erledigen)(list(en|e|)|note(s|)|notiz(en|))";
			todoOther = "to(-| |)do|(zu |)erledigen";
			shopping = "einkaufsliste|einkaufszettel|kaufliste|kaufsliste|(shopping|shop)(-| |)(list(en|e|)|note(s|)|notiz(en|))";
			shopOther = "einkaufen|einzukaufen|kaufen|shopping|shop";
			unknownList = "(\\w+(-|)|)(list(en|e|)|zettel|note(s|)|notiz(en|))";
			
		//English and other
		}else{
			todo = "to(-| |)do(-| |)(list(s|)|note(s|))";
			todoOther = "to(-| |)do";
			shopping = "(shopping|shop)(-| |)(list(s|)|note(s|))";
			shopOther = "shopping|shop|buy";
			unknownList = "(\\w+(-|)|)(list(s|)|note(s|))";
		}
		
		extracted = NluTools.stringFindFirst(input, todo + "|" + todoOther + "|" + shopping + "|" + shopOther);
		if (extracted.isEmpty()){
			extracted = NluTools.stringFindFirst(input, unknownList);
		}
		
		if (NluTools.stringContains(extracted, todo)){
			type = ("<" + UserDataList.IndexType.todo + ">");
			//subTypeRes = getSubType(input, extracted, language);
		
		}else if (NluTools.stringContains(extracted, todoOther)){
			type = ("<" + UserDataList.IndexType.todo + ">");
		
		}else if (NluTools.stringContains(extracted, shopping)){
			type = ("<" + UserDataList.IndexType.shopping + ">");
			//subTypeRes = getSubType(input, extracted, language);
		
		}else if (NluTools.stringContains(extracted, shopOther)){
			type = ("<" + UserDataList.IndexType.shopping + ">");
		
		}else if (NluTools.stringContains(extracted, unknownList)){
			type = ("<" + UserDataList.IndexType.unknown + ">");
			//subTypeRes = getSubType(input, extracted, language);
			//Debugger.println("Parameter - ListType is unknown: " + input, 3); 	//<- this is most of the time the case because you usually create named lists
		}
		
		found = extracted;
		/*
		if (subTypeRes == null){
			found = extracted;
		}else{
			found = subTypeRes[1].trim();
			subType = subTypeRes[0].trim();
		}
		*/
		//store it
		pr = new ParameterResult(PARAMETERS.LIST_TYPE, type, found);
		nluInput.addToParameterResultStorage(pr);
		
		return type;
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
		if (language.matches(LANGUAGES.DE)){
			return NluTools.stringRemoveFirst(input, "(auf |zu(r|) |)(die |den |meine |meinen |)" + Pattern.quote(found));
		
		}else{
			return NluTools.stringRemoveFirst(input, "(on |to |)(a |my |)" + Pattern.quote(found));
		}
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//is accepted result?
		if (!input.startsWith("<") || input.equals("<" + IndexType.unknown + ">") 
				|| !UserDataList.indexTypeContains(input.replaceAll("^<|>$", "").trim())){

			return "";
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.LIST_TYPE, input);
			JSON.add(itemResultJSON, InterviewData.LIST_TYPE_LOCALE, getLocal(input, language));
			//JSON.add(itemResultJSON, InterviewData.LIST_SUBTYPE, subType);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\":.+\\}$") && input.contains("\"" + InterviewData.LIST_TYPE + "\"")){
			//System.out.println("ListType IS VALID: " + input); 		//debug
			return true;
		}else{
			//System.out.println("ListType IS NOT VALID: " + input); 		//debug
			return false;
		}
	}

	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}

}
