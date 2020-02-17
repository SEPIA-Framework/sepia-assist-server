package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.JSON;

public class FashionBrand implements ParameterHandler{
	
	//-------data-------
	public static String fashionBrandsSearch = 
			"(a2|aerosoles|adidas|asics|bandolino|bare traps|bass|bates|bearpaw|billabong|birkenstock|" +
			"blowfish|bogs|bostonian|burton|bzees|carlos by carlos santana|cars|carters|caterpillar|chucks|circus by sam edelman|clarks|" + 
			"cliffs by white mountain|converse|crocs|dockers|doc martens|dr martens|dr scholls|earth origins|eurosoft|fergalicious|fergie|fila|florsheim|" +
			"g by guess|gbx|giorgio brutini|goodyear|grasshoppers|hanna andersson|harley davidson|heelys|highpoint|" +
			"impo|isotoner|izod|j-41|jbu|jellypop|john deere|jsport|kamik|keds|kenneth cole reaction|kensie girl|khombu|k-swiss|" +
			"la gear|laforst|laredo|levis|lifestride|lugz|madden girl|magnum|march of dimes|margaritaville|mark nason skechers|" +
			"merrell|minnetonka moccasin|miranda by miranda lambert|mountain gear|muk luks|nanette lepore|natural soul|naturalizer|" +
			"nautica|new balance|nike free|nike|nine west kids|nunn bush|oshkosh bgosh|papell studio|patrizia|paw patrol|peds|perry ellis|" +
			"polo by ralph lauren|propet|puma|rachel shoes|reebok duty|reebok work|reebok|reef|report|rock & candy|rock & soda|rocket dog|" +
			"rockport works|rockport|rocky|roxy|ryka|salomon|saucony|skechers work|skechers|sof sole|sperry top-sider|sporto|spring step|" +
			"streetcars|stride rite|teva|thomas & friends|timberland pro|timberland|tommy hilfiger|touch of nina|vans|white mountain|" +
			"wigwam|xoxo|zigi soho|zodiac american original|"
			+ "volcom)(s|)";
	
	public static String moreBrands = 
			"(a2|abercrombie & fitch|abercrombie and fitch|adidas (original|kaiser|samba)(s|)|adidas|aerosoles|alice and olivia|all saints|allsaints|american apparel|"
			+ "american eagle|armani exchange|asics|badgley mischka|banana republic|bandolino|bare traps|bass|bates|bcbg|bearpaw|ben sherman|"
			+ "betsey johnson|bianca mosca|billabong|birkenstock|blowfish|bogs|bostonian|brioni|burberry|burton|bzees|canali|capita|carlos by carlos santana|"
			+ "carlos|cars|carters|caterpillar|chanel|chaos|charles tyrwhitt|charvet place vendome|chloe|christian dior|chucks|circus by sam edelman|clarks|"
			+ "cliffs by white mountain|coach|converse|crocs|cynthia rowley|dc|diane von furstenberg|diesel|dkny|dockers|dolce & gabbana|dolce and gabbana|"
			+ "donna karan|doc martens|dr martens|dr scholls|drakes of london|drakes|earth origins|elie tahari|ellen tracy|ermenegildo zegna|eurosoft|fergalicious|"
			+ "fergie|feri|fila|florsheim|forum|g by guess|gabriel brothers|gbx|giorgio brutini|givenchy|gnu|goodyear|grasshoppers|gucci|guess|haggar clothing|"
			+ "hanna andersson|harley davidson|havren|heelys|hermes|highpoint|hollister|hugo boss|impo|isotoner|izod|j crew|j 41|j-41|jack spade|jbu|"
			+ "jean paul gaultier|jellypop|jimmy choo|john deere|john galliano|john varvatos|jollychic|jsport|k2|k-swiss|k swiss|kswiss|kamik|karl lagerfeld|kate spade|"
			+ "kathy ireland|keds|kenneth cole reaction|kenneth cole|kennethcole|kensie girl|khombu|kiton|la gear|la perla|lacoste|laforst|laredo|"
			+ "leon max|levi strauss|levis|lib tech|lifestride|louis vuitton|lugz|madden girl|madewell|magnum|map|marc jacobs|march of dimes|marcheesa|margaritaville|"
			+ "mark nason skechers|max studio|merrell|michael bastian|michael kors|minnetonka moccasin|miranda lambert|miranda|miu miu|moschino|"
			+ "mountain gear|muk luks|nanette lepore|natural soul|naturalizer|nautica|new balance|nicole miller|nike free|nike|nine west kids|nougat london|"
			+ "nunn bush|oshkosh bgosh|ozwald boateng|papell studio|patrizia|paul smith|paw patrol|peds|perry ellis|philipp plein|polo by ralph lauren|"
			+ "polo|prada|propet|puma|pull and bear|pull & bear|rachel shoes|rag & bone |rag and bone |ralph lauren|reebok duty|reebok work|reebok|reef|"
			+ "reiss|report|ride|roberto cavalli|rock & candy|rock and candy|rock & republic|rock & soda|rock and soda|rock and republic|rocket dog|"
			+ "rockport works|rockport|rocky|rome|rossignol|roxy|ryka|salomon|saucony|skechers work|skechers|sof sole|sperry top-sider|sporto|spring step|stella mccartney|"
			+ "stone island|streetcars|stride rite|ted baker|teva|theory|thom browne|thomas & friends|thomas and friends|timberland pro|timberland|tom ford|"
			+ "tommy hilfiger|topman|tory burch|touch of nina|true religion|turnbull & asser|turnbull and asser|umbro|uniqlo|valentino spa|valentino|vans|"
			+ "vera wang|versace|victorias secret|vineyard vines|vivienne westwood|volcom|white mountain|wigwam|woolrich clothing|woolrich clothing|wrangler|xoxo|"
			+ "zac posen|zara|zigi soho|zodiac american original|zodiac|zuhair murad)(s|)";
	
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
			String brand = NluTools.stringFindFirst(input, moreBrands);
			this.found = brand;
			return brand;
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
		return NluTools.stringRemoveFirst(input, "\\b(von (der |)marke |marke |von |)\\b" + found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			return input.replaceAll(".*\\b(von)\\b", "").trim();
		}else{
			return input.replaceAll(".*\\b(from|of|by)\\b", "").trim();
		}
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
