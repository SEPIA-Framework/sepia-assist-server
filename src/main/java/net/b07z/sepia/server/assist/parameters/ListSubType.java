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
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Handler that is supposed to find a sub-type aka. "name" of a list like my ->BEST<- shopping list
 * 
 * @author Florian Quirin
 *
 */
public class ListSubType implements Parameter_Handler{
	
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
		String subType = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.LIST_SUBTYPE);
		if (pr != null){
			subType = pr.getExtracted();
			this.found = pr.getFound();
			
			return subType;
		}
		
		//String[] subTypeRes = null;
		String extractedRawTerm = "";
		String genericListWithPrefix;
		
		//German
		if (language.equals(LANGUAGES.DE)){
			//old: (\\w+ |)(to do |\\w+(-| |)(\\w+-|))
			genericListWithPrefix = "(\\w+ |)((to do )|(\\w+-\\w+(-| ))|(\\w+(-| |)))(list(en|e|)|zettel|note(s|)|notiz(en|))(?! (\\w+ |)(auf|von|zu(r|)))";
			extractedRawTerm = NluTools.stringFindFirst(input, genericListWithPrefix);
			extractedRawTerm = extractedRawTerm.replaceFirst("\\b(auf |von |zu(r|) )\\b", "").trim();
			extractedRawTerm = extractedRawTerm.replaceFirst(".*\\b(meine(n|r|m|)|die|der|den|eine|einer|einen|einem|mir)\\b", "").trim();
			
		//English and other
		}else{
			genericListWithPrefix = "(\\w+ |)((to do )|(\\w+-\\w+(-| ))|(\\w+(-| )))(list(s|)|note(s|))(?! (\\w+ |)(onto|on|to|at|from))";
			extractedRawTerm = NluTools.stringFindFirst(input, genericListWithPrefix);
			extractedRawTerm = extractedRawTerm.replaceFirst("\\b(on |onto |to |at |from )\\b", "").trim();
			extractedRawTerm = extractedRawTerm.replaceFirst(".*\\b(my|a|the)\\b", "").trim();
		}
		//System.out.println("subType e2.: " + extractedRawTerm);		//DEBUG
		
		//clean sub-type of action
		if (!extractedRawTerm.isEmpty()){
			extractedRawTerm = ParameterResult.cleanInputOfParameter(nluInput, PARAMETERS.ACTION, input, extractedRawTerm);
		}
		//clean sub-type of parent type
		if (!extractedRawTerm.isEmpty()){
			ParameterResult prListType = ParameterResult.getResult(nluInput, PARAMETERS.LIST_TYPE, input);
			String exListType = prListType.getExtracted();
			if (exListType.isEmpty() || exListType.equals("<" + UserDataList.IndexType.unknown + ">")){
				//discard result
			}else{
				extractedRawTerm = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.LIST_TYPE, prListType, extractedRawTerm);
			}
			//extractedRawTerm = ParameterResult.cleanInputOfParameter(nluInput, PARAMETERS.LIST_TYPE, input, extractedRawTerm);
		}
		//System.out.println("subType e3.: " + extractedRawTerm);		//DEBUG
		
		//German
		if (language.equals(LANGUAGES.DE)){
			extractedRawTerm = extractedRawTerm.replaceFirst("\\b(\\w*)(list(en|e|)|zettel|note(s|)|notiz(en|))\\b", "$1").trim();
		
		//English and other
		}else{
			extractedRawTerm = extractedRawTerm.replaceFirst("\\b(\\w*)(list(s|)|note(s|))\\b", "$1").trim();
		}
		
		subType = extractedRawTerm;
		found = subType;
		//System.out.println("subType 1: " + subType);				//DEBUG
		
		//reconstruct original phrase to get proper item names
		if (!subType.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(language);
			subType = normalizer.reconstructPhrase(nluInput.textRaw, subType);
		}

		//store it
		pr = new ParameterResult(PARAMETERS.LIST_SUBTYPE, subType, found);
		nluInput.addToParameterResultStorage(pr);
		
		return subType;
	}
	/**
	 * Get a sub-type of a known list type ... or at least try to ;-)
	 * @param input - full input
	 * @param typeExtracted - extracted known type
	 * @param language - ...
	 * @return - 0: subType, 1: found
	 */
	/*
	public static String[] getSubType(String input, String typeExtracted, String language){
		String subType = "";
		String found = "";
		
		//German
		if (language.matches(LANGUAGES.DE)){
			String pre = "meine(n|)|die|den|eine|einen";
			subType = NLU_Tools.stringFindFirst(input, "((" + pre + ") .*?|^\\w+ )\\b(" + Pattern.quote(typeExtracted) + ")");
			found = subType;
			subType = subType.replaceFirst("(" + pre + ")", "");
			subType = subType.replaceFirst(Pattern.quote(typeExtracted), "").trim();
			
		//English
		}else{
			String pre = "my|a|the";
			subType = NLU_Tools.stringFindFirst(input, "(" + pre + "|^\\w+) .*?\\b(" + Pattern.quote(typeExtracted) + ")");
			found = subType;
			subType = subType.replaceFirst("(" + pre + ")", "");
			subType = subType.replaceFirst(Pattern.quote(typeExtracted), "").trim();
		}
		
		return new String[]{subType, found};
	}
	*/
	
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
			return NluTools.stringRemoveFirst(input, "(?i)(auf |zu(r|) |)(die |den |meine |meinen |)" + Pattern.quote(found) + "(list(en|e|)|zettel|)");
		
		}else{
			return NluTools.stringRemoveFirst(input, "(?i)(on |to |)(a |my |)" + Pattern.quote(found) + "(list(s|)|)");
		}
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
