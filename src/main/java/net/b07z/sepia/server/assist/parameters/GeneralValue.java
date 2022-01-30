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
 * A parameter handler that extracts a general, unspecific value as text like in "... set X to value Y". 
 * 
 * @author Florian Quirin
 *
 */
public class GeneralValue implements ParameterHandler {

	public User user;
	public NluInput nluInput;
	public String language;
	public boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput){
		this.nluInput = nluInput;
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult){
		this.nluInput = nluResult.input;
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	@Override
	public String extract(String input){
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.GENERAL_VALUE);
		if (pr != null){
			String item = pr.getExtracted();
			this.found = pr.getFound();
			
			return item;
		}
		
		String genValue = "";
		//NOTE: we are rather strict here and force the format "... X to (the) value Y END" to avoid a broad matching
		if (language.equals(LANGUAGES.DE)){
			//GERMAN
			genValue = NluTools.stringFindFirst(input, "(auf |zu )(den |der |dem |)wert .+");
			if (!genValue.isEmpty()){
				genValue = genValue.replaceAll(".*?\\b(wert)\\b", "");
				genValue = genValue.replaceAll("\\b((fest|)setzen|(ein|)stellen)$", "").trim();
			}
		}else if (language.equals(LANGUAGES.EN)){
			//ENGLISH
			genValue = NluTools.stringFindFirst(input, "(to )(the |a |)value .+");
			if (!genValue.isEmpty()){
				genValue = genValue.replaceAll(".*?\\b(value)\\b", "").trim();
			}
		}
		this.found = genValue;
		
		//reconstruct original phrase to get proper value
		if (!genValue.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(this.language);
			genValue = normalizer.reconstructPhrase(nluInput.textRaw, genValue);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.GENERAL_VALUE, genValue.trim(), found);
		nluInput.addToParameterResultStorage(pr);
		
		return genValue;
	}
	
	@Override
	public String guess(String input){
		return "";
	}
	
	@Override
	public String getFound(){
		return found;
	}

	@Override
	public String remove(String input, String found){
		if (language.equals(LANGUAGES.DE)){
			found = "(auf |zu )(den |der |dem |)wert " + Pattern.quote(found) + "(| (fest|)setzen$| (ein|)stellen$)";
		}else{
			found = "(to )(the |a |)value " + Pattern.quote(found);
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceFirst(".* wert( ist |)\\b", "");
		}else{
			input = input.replaceFirst(".* value( is |)\\b", "");
		}
		return input.trim();
	}

	@Override
	public String build(String input){
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			//JSON.add(itemResultJSON, InterviewData.INPUT_RAW, nluInput.textRaw);
			//JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
			//JSON.add(itemResultJSON, InterviewData.EXTRAS, extras);
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input){
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
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
