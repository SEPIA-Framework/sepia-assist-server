package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class FashionSize implements ParameterHandler{
	
	//-------data-------
	public static String fashionSizes = "((\\d\\d\\d|\\d\\d|\\d)(\\.\\d+|,\\d+|)|xxxl|xxl|xl|l|m|s|xs|xxs|xxxs)";

	//------------------

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
	
	@Override
	public String extract(String input) {
		if (language.equals(LANGUAGES.DE)){
			String size = NluTools.stringFindFirst(input, "(in |)(\\w+groesse |)" + fashionSizes);
			size = size.replaceFirst("\\b(in |)(\\w+groesse|)\\b", "").trim();
			this.found = size;
			size = size.replaceAll(",", ".");
			return size;
		}else{
			//TODO: this cannot be complete! =)
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
		return NluTools.stringRemoveFirst(input, "(in |)(\\w+groesse |)" + found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(in)\\b", "").trim();
			return input;
		}else{
			input = input.replaceAll(".*\\b(in)\\b", "").trim();
			return input;
		}
	}

	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, "");
		
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
		return buildSuccess = true;
	}

}
