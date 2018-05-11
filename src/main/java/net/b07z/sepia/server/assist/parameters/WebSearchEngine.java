package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class WebSearchEngine implements Parameter_Handler{
	
	//-----data-----
	/* may cause confusion
	public static final String GOOGLE = "Google";
	public static final String YAHOO = "Yahoo";
	public static final String BING = "Bing";
	public static final String DUCK_DUCK_GO = "DuckDuckGo"; */
	
	public static final String names = "(google|bing|duck duck go|duck duck|duckduckgo|yahoo)";
	public static ArrayList<String> list = new ArrayList<>();
	static{
		list.add("google");
		list.add("bing");
		list.add("duck duck go");
		list.add("yahoo");
	}
	//--------------

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
		String engine = NluTools.stringFindFirst(input, names);
		found = engine;
		if (engine.equals("duck duck") || engine.equals("duckduckgo")){
			engine = "duck duck go";
		}
		return engine;
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
			input = input.replaceFirst("\\b(mit |via |per |uber |mittels |auf |ueber |)(" + Pattern.quote(found) + ")\\b", "").trim();
		}else{
			input = input.replaceFirst("\\b(with |on |via |per |over |by |)(" + Pattern.quote(found) + ")\\b", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
		
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
