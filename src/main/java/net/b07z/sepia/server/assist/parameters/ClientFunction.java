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
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A parameter to find client functions like "settings" in "open settings".<br> 
 * This is probably as close as it gets to a reference implementation of a {@link ParameterHandler}.
 * 
 * @author Florian Quirin
 *
 */
public class ClientFunction implements ParameterHandler {
	
	public static enum Type {
		settings,
		volume,
		alwaysOn,
		meshNode,
		clexi,
		platformFunction, 	//this is handled by PlatformControls service (we use it just for the action)
		searchForMusic		//this is handled by MusicSearch service (we use it just for the action)
	}
	
	//-------data-------
	public static HashMap<String, String> local_de = new HashMap<>();
	public static HashMap<String, String> local_en = new HashMap<>();
	static {
		local_de.put("<settings>", "die Einstellungen");
		local_de.put("<volume>", "die Lautstärke");
		local_de.put("<alwaysOn>", "der Always-On Modus");
		local_de.put("<meshNode>", "die Mesh-Node");
		local_de.put("<clexi>", "CLEXI");
		
		local_en.put("<settings>", "the settings");
		local_en.put("<volume>", "the volume");
		local_en.put("<alwaysOn>", "the Always-On mode");
		local_en.put("<meshNode>", "the mesh-node");
		local_en.put("<clexi>", "CLEXI");
	}
	/**
	 * Translate generalized value.
	 * If generalized value is unknown returns empty string.
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
		String localName = "";
		String valueWithBrackets = value;
		if (!value.startsWith("<")){
			valueWithBrackets = "<" + value + ">";
		}
		if (language.equals(LANGUAGES.DE)){
			localName = local_de.get(valueWithBrackets);
		}else if (language.equals(LANGUAGES.EN)){
			localName = local_en.get(valueWithBrackets);
		}
		if (localName == null){
			Debugger.println("Action.java - getLocal() has no '" + language + "' version for '" + value + "'", 3);
			return "";
		}
		return localName;
	}
	//------------------

	public User user;
	public NluInput nluInput;
	public String language;
	public boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
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
	public String extract(String input) {
		String clientFun = "";
		
		//check storage first - currently we assume this is only used in client-control service
		/*
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.CLIENT_FUN);
		if (pr != null){
			clientFun = pr.getExtracted();
			this.found = pr.getFound();
			
			return clientFun;
		}
		*/
		
		String settings, volume, alwaysOn, meshNode, clexi;
		//German
		if (language.matches(LANGUAGES.DE)){
			settings = "einstellung(en|)|setting(s|)|menue|option(en|)";
			volume = "lautstaerke|musik|radio|sound";
			alwaysOn = "always(-| |)on";
			meshNode = "mesh(-| |)node";
			clexi = "clexi";
			
		//English and other
		}else{
			settings = "setting(s|)|menu(e|)|option(s|)";
			volume = "volume|music|radio|sound";
			alwaysOn = "always(-| |)on";
			meshNode = "mesh(-| |)node";
			clexi = "clexi";
		}
		
		String extracted = NluTools.stringFindFirst(input, 
				settings + "|" + 
				volume + "|" + 
				alwaysOn + "|" +
				meshNode + "|" +
				clexi
		);
		
		if (!extracted.isEmpty()){
			//SETTINGS
			if (NluTools.stringContains(extracted, settings)){
				clientFun = "<" + Type.settings + ">";
			//VOLUME
			}else if (NluTools.stringContains(extracted, volume)){
				clientFun = "<" + Type.volume + ">";
			//ALWAYS-ON
			}else if (NluTools.stringContains(extracted, alwaysOn)){
				clientFun = "<" + Type.alwaysOn + ">";
			//MESH-NODE
			}else if (NluTools.stringContains(extracted, meshNode)){
				clientFun = "<" + Type.meshNode + ">";
			//CLEXI
			}else if (NluTools.stringContains(extracted, clexi)){
				clientFun = "<" + Type.clexi + ">";
			}else{
				clientFun = "";
			}
		}
		this.found = extracted;
		
		//store it - currently we assume this is only used in client-control service
		/*
		pr = new ParameterResult(PARAMETERS.CLIENT_FUN, clientFun, found);
		nluInput.addToParameterResultStorage(pr);
		*/
		
		if (Is.notNullOrEmpty(this.found) && Is.notNullOrEmpty(clientFun)){
			return (clientFun + ";;" + this.found);
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
		return NluTools.stringRemoveFirst(input, "(the |a |der |die |das |eine |einen |)" + Pattern.quote(found));
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		String ex = "";
		String found = "";
		if (input.contains(";;")){
			String[] array = input.split(";;");
			ex = array[0];
			found = array[1];
		}else{
			ex = input;
		}
		//is accepted result?
		String inputLocal = getLocal(ex, language);
		if (inputLocal.isEmpty()){
			return "";
		}
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			//JSON.add(itemResultJSON, InterviewData.INPUT_RAW, nluInput.textRaw);
			JSON.add(itemResultJSON, InterviewData.VALUE, ex);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, inputLocal);
			JSON.add(itemResultJSON, InterviewData.FOUND, found);
		
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
