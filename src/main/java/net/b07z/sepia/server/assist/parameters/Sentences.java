package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Converts an array of sentences represented as strings with ";;" as separator to a parameter ARRAY output. 
 * This is for example used to connect known sentences with a user custom input in the SentenceConnect service. 
 * 
 * @author Florian Quirin
 *
 */
public class Sentences implements ParameterHandler{
	
	User user;
	NluInput nluInput;
	String language;
	boolean buildSuccess = false;
	
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
	public boolean isGeneric(){
		return false;
	}

	@Override
	public String extract(String input) {
		String seperatedText = input
				.replaceAll("(\\.|\\?|;;|;)(\\s|$)", " && ")
				.replaceAll("(\\&\\&\\s)$", "").trim();
		found = seperatedText;
		return seperatedText;
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
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		JSONArray sentencesArray = new JSONArray();
		if (input != null && !input.isEmpty()){
			String[] sentences = input.split("&&");
			for (String s : sentences){
				JSON.add(sentencesArray, s);
			}
		}
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.ARRAY, sentencesArray);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.ARRAY + "\"")){
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
