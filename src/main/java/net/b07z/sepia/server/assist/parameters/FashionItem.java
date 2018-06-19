package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class FashionItem implements Parameter_Handler{
	
	//-------data-------
	public static final String fashionItems_de = 
			"westen|weste|visitenkartenetuis|visitenkartenetui|visitenkarten-etui|unterwaesche|tunikas|tunika|trikots|trikot|trenchcoats|"
			+ "trenchcoat|taschen|tasche|tanktops|tanktop|t-shirts|t-shirt|stulpen|struempfen|struempfe|stiefeln|stiefel|snowboard\\w*|snowboard boots|"
			+ "snowboard boot|sneakers|sneakern|sneaker|skinny jeans|skiern|skier|ski protektoren|ski protektor|ski|shapewear|schlauchschal|schal|"
			+ "(ein|einen|der|den) rock|pyjamas|pyjama|parkas|parka|outfits|outfit|ohrringen|ohrringe|leggins|klamotten|kleidung|jumpsuits|jumpsuit|jeansrock|jeanskleid|jeans rock|"
			+ "jeans jacke|jeans|hoody|hoodies|hoodie|highheels|hemdblusen|hemdbluse|gutscheine|gutschein|guertel|fahrradhelme|fahrradhelm|chinos|"
			+ "chino-hosen|chino hosen|chino hose|cardigans|cardigan|bleistiftrock|boxershorts|blazer|armband|armbaender|\\w*taschen|\\w*tasche|\\w*socken|"
			+ "\\w*shirts|\\w*shirt|\\w*schuhen|\\w*schuhe|\\w*schuh|\\w*roecken|\\w*roecke|\\w*pullovern|\\w*pullover|\\w*pullis|\\w*pulli|\\w*mode|\\w*mantel|"
			+ "\\w*maenteln|\\w*maentel|\\w*koffern|\\w*koffer|\\w*koffer|\\w*kleidern|\\w*kleider|\\w*kleid|\\w*jeans|\\w*jacken|\\w*jacke|\\w*hosen|"
			+ "\\w*hose|\\w*hemden|\\w*hemd|\\w*helmen|\\w*helme|\\w*helm|\\w*brillen|\\w*brille|\\w*blusen|\\w*bluse|\\w*bekleidung|\\w*anzug|"
			+ "\\w*anzuegen|\\w*anzuege";
	
	public static final String fashionItems_en = 
			"vest|cardigan|underwear|tricot|tricots|trenchcoats|trenchcoat|dress|dresses|grown|frock|glasses|suit|suits|"
			+ "bags|bag|tanktops|tanktop|t-shirts|t-shirt|stockings|stiefeln|high boots|snowboard\\w*|snowboard boots|"
			+ "snowboard boot|boots|shoes|shoe|boot|sneakers|sneaker|skinny jeans|skier|ski protectors|ski protector|ski|shapewear|scarf|"
			+ "a rock|pyjamas|pyjama(s|)|pajama(s|)|parkas|parka|outfits|outfit|earrings|earring|leggins|leggings|clothes|clothing|jumpsuits|jumpsuit|jeansrock|denim dress|jeans rock|"
			+ "jeans jacket|jeans|hoody|hoodies|hoodie|highheels|voucher(s|)|belt|belts|bike helmet(s|)|helmet(s|)|chinos|"
			+ "chino-pants|chino pants|panties|cardigans|cardigan|boxershorts|blazer|armband|armbaender|"
			+ "\\w*slacks|\\w*trousers|\\w*pants|\\w*shirts|\\w*shirt|\\w*boots|\\w*boot|\\w*skirts|\\w*skirt|\\w*pullover|\\w*pullovers|\\w*wear|\\w*coat|"
			+ "\\w*coats|\\w*jeans|\\w*jacket|\\w*jackets";
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
			String item = NluTools.stringFindFirst(input, fashionItems_de);
			this.found = item;
			return item;
		}else{
			String item = NluTools.stringFindFirst(input, fashionItems_en);
			this.found = item;
			return item;
		}
	}
	
	@Override
	public String guess(String input) {
		if (language.equals(LANGUAGES.DE)){
			String guess = NluTools.stringFindFirst(input, "(suche|brauche|will|gibt es|zeig|moechte|bekomme ich|finde|benoetige|haette gerne|kann ich) .*|"
					+ 										"(mag) .* (haben)");
			if (!guess.isEmpty()){
				guess = guess.replaceFirst(".*\\b(suche|brauche|will|gibt es|zeig mir|zeig|moechte|bekomme ich|finde|benoetige|haette gerne|kann ich)\\b", "");
				guess = guess.replaceFirst("\\b(kaufen|erwerben|online bestellen|bestellen|suchen|finden|zeigen|bekommen|haben|umsehen)( kann|)\\b.*", "");
				guess = guess.replaceFirst(".*\\b(nach)\\b", "");
				guess = guess.replaceFirst(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner|was|etwas)\\b", "");
				guess = guess.replaceFirst("alles|neuste|neues","");
				guess = guess.trim();
				if (!guess.isEmpty()){
					Debugger.println("GUESS: " + guess + " = " + PARAMETERS.FASHION_ITEM + " (FashionItem)", 3);
				}
				return guess;
			}else{
				return "";
			}
		//TODO: improve
		}else{
			return "";
		}
	}

	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			return NluTools.stringRemoveFirst(input, "\\b(nach |)(einen|einem|einer|eine|ein|der|die|das|den|dem|ne|ner|)\\b" + found);
		}else{
			return NluTools.stringRemoveFirst(input, "\\b(for |)(a|the|)\\b" + found);
		}
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll(".*\\b(nach)\\b", "").trim();
			return input.replaceAll(".*\\b(einen|einem|einer|eine|ein|der|die|das|den|ne|ner)\\b", "").trim();
		}else{
			input = input.replaceAll(".*\\b(for)\\b", "").trim();
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, input); 		//TODO: add local names
		
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
