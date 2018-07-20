package net.b07z.sepia.server.assist.parameters;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Handler for items on a list.
 * 
 * @author Florian Quirin
 *
 */
public class ListItem implements ParameterHandler{

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
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.LIST_ITEM);
		if (pr != null){
			String item = pr.getExtracted();
			this.found = pr.getFound();
			
			return item;
		}
		
		//Items make only sense with certain commands (it makes life easier)
		pr = ParameterResult.getResult(nluInput, PARAMETERS.ACTION, input);
		String action = pr.getExtracted().replaceAll("^<|>$", "").trim();
		String add = Action.Type.add.name();
		String set = Action.Type.set.name();
		String remove = Action.Type.remove.name();
		if (!action.equals(add) && !action.equals(set) && !action.equals(remove)){
			//store it and return empty
			pr = new ParameterResult(PARAMETERS.LIST_ITEM, input.trim(), found);
			nluInput.addToParameterResultStorage(pr);
			return "";
		}
		
		//DE
		if (language.matches(LANGUAGES.DE)){
			input = input.replaceAll(".*?\\b(setze|fuege|loesche|entferne)( auch|)( bitte|)( noch|)\\b", "").trim();
			input = input.replaceAll("(.*)\\b(auf |von |in |der |dem |den |zu(r|) |meiner ).*?\\b(list|\\w*liste|\\w*zettel|(einkaufs|shopping|to-do|todo|to do)(-| |)(list(e|)|zettel))\\b.*", "$1").trim();
			//handle some exceptions
			if (input.matches("etwas|was|das|dieses|es|dinge|sachen|eine sache|zeug|ein paar dinge|ein paar sachen")){
				input = "";
			}
			
			this.found = input;
			
			//reconstruct original phrase to get proper item names
			Normalizer normalizer = Config.inputNormalizers.get(language);
			input = normalizer.reconstructPhrase(nluInput.textRaw, input);
			if (!input.isEmpty()){
				input = input.replaceAll("\\b(und|,)\\b"," && ");
			}
		
		//EN
		}else{
			input = input.replaceAll(".*?\\b(add|put|set|remove|take|delete)\\b", "").trim();
			input = input.replaceAll("(.*)\\b(on |off |from |to |in |inside ).*?\\b(list|(shopping|to-do|todo|to do)(-| |)(list))\\b.*", "$1").trim();
			//handle some exceptions
			if (input.matches("stuff|it|something|this|items|an item|a item|a thing|things|a few things|a few items")){
				input = "";
			}
			
			this.found = input;
			
			//reconstruct original phrase to get proper item names
			Normalizer normalizer = Config.inputNormalizers.get(language);
			input = normalizer.reconstructPhrase(nluInput.textRaw, input);
			
			if (!input.isEmpty()){
				input = input.replaceAll("\\b(and|,)\\b"," && ");
			}
		}
		
		//TODO: one could do a action, listTyp, listSubType clean-up here
		
		//store it
		pr = new ParameterResult(PARAMETERS.LIST_ITEM, input.trim(), found);
		nluInput.addToParameterResultStorage(pr);
		
		return input.trim();
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
		return NluTools.stringRemoveFirst(input, Pattern.quote(found));
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.INPUT_RAW, nluInput.textRaw);
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
			//System.out.println("IS NOT VALID: " + input); 	//debug
			return false;
		}
	}

	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}

}
