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

public class Gender implements ParameterHandler{
	
	//-------data-------
	public static String indicatorMale_de = "(mann|maenner|herr|herren|vater|vaeter|bruder|brueder|sohn|soehne|freund)";
	public static String indicatorFemale_de = "(frau|frauen|dame|damen|mutter|muetter|schwester|schwestern|tochter|toechter|freundin|freundinnen)";
	public static String indicatorBoys_de = "(junge|jungs|jungens)";
	public static String indicatorGirls_de = "(maedchen)";
	public static String indicatorChildren_de = "(kinder|kind)";
	public static String indicatorBabies_de = "(babies|baby|kleinkind|kleinkinder)";
	
	public static HashMap<String, String> genderFashion_de = new HashMap<>();
	public static HashMap<String, String> genderFashion_en = new HashMap<>();
	static {
		genderFashion_de.put("<male>", "Herren");
		genderFashion_de.put("<female>", "Damen");
		genderFashion_de.put("<boy>", "Jungen");
		genderFashion_de.put("<girl>", "Mädchen");
		genderFashion_de.put("<child>", "Kinder");
		genderFashion_de.put("<baby>", "Babies");
		genderFashion_de.put("<unisex>", "unisex");
		genderFashion_de.put("<adult>", "Erwachsene");
		
		genderFashion_en.put("<male>", "men");
		genderFashion_en.put("<female>", "women");
		genderFashion_en.put("<boy>", "boys");
		genderFashion_en.put("<girl>", "girls");
		genderFashion_en.put("<child>", "children");
		genderFashion_en.put("<baby>", "babies");
		genderFashion_en.put("<unisex>", "unisex");
		genderFashion_en.put("<adult>", "adults");
	}
	/**
	 * Translate generalized gender value (e.g. male) to local name for fashion (e.g. Herren).
	 * If generalized value is unknown returns empty string.
	 * @param genderValue - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal4Fashion(String genderValue, String language){
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = genderFashion_de.get(genderValue);
		}else if (language.equals(LANGUAGES.EN)){
			localName = genderFashion_en.get(genderValue);
		}
		if (localName == null){
			Debugger.println("Gender.java - getLocal4Fashion() has no '" + language + "' version for '" + genderValue + "'", 3);
			return "";
		}
		return localName;
	}
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
			String gender = "";
			String male = NluTools.stringFindFirst(input, indicatorMale_de);
			String female = NluTools.stringFindFirst(input, indicatorFemale_de);
			String boy = NluTools.stringFindFirst(input, indicatorBoys_de);
			String girl = NluTools.stringFindFirst(input, indicatorGirls_de);
			String child = NluTools.stringFindFirst(input, indicatorChildren_de);
			String baby = NluTools.stringFindFirst(input, indicatorBabies_de);
			if (!male.isEmpty() && !female.isEmpty()){
				gender = "<unisex>";
				this.found = male;		//TODO: solve this properly, change to adult?
			}else if (!male.isEmpty()){
				gender = "<male>";
				this.found = male;		
			}else if (!female.isEmpty()){
				gender = "<female>";
				this.found = female;	
			}else if (!boy.isEmpty() && !girl.isEmpty()){
				gender = "<child>";
				this.found = boy;	
			}else if (!boy.isEmpty()){
				gender = "<boy>";
				this.found = boy;	
			}else if (!girl.isEmpty()){
				gender = "<girl>";
				this.found = girl;	
			}else if (!child.isEmpty()){
				gender = "<child>";
				this.found = child;	
			}else if (!baby.isEmpty()){
				gender = "<baby>";
				this.found = boy;	
			}
			return gender;
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
		return NluTools.stringRemoveFirst(input, "(fuer |for |)(meine |meinen |my |)" + found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(fuer)\\b", "").trim();
			return input.replaceAll(".*\\b(einen|ein|eine|ne)\\b", "").trim();
		}else{
			input = input.replaceAll(".*\\b(for)\\b", "").trim();
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//is accepted result?
		String inputLocal = getLocal4Fashion(input, language);
		if (inputLocal.isEmpty()){
			return "";
		}
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, inputLocal);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
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
