package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Color;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Shopping test.
 * 
 * @author Florian Quirin
 *
 */
public class ShoppingFashion_Affilinet implements ApiInterface{
	
	//----------------------Filter mappings-------------------------
	private static String ZALANDO = "zalando";
	private static String PLANET = "planetSports";
	private static String[] shops = {ZALANDO, PLANET};
	
	private static HashMap<String, String> zalandoFilter = new HashMap<>();
	private static HashMap<String, String> planetSportsFilter = new HashMap<>();
	static{
		zalandoFilter.put("<unisex>", "Property_CF_Gender:Unisex");
		zalandoFilter.put("<male>", "Property_CF_Gender:Herren");
		zalandoFilter.put("<female>", "Property_CF_Gender:Damen");
		zalandoFilter.put("<boy>", "Property_CF_Gender:Jungen");
		zalandoFilter.put("<girl>", "Property_CF_Gender:Mädchen");
		zalandoFilter.put("<child>", "Property_CF_Gender:Kinder");
		zalandoFilter.put("<baby>", "");
		zalandoFilter.put("<adult>", "");
		
		planetSportsFilter.put("<unisex>", "Property_CF_gender:Unisex");
		planetSportsFilter.put("<male>", "Property_CF_gender:Men");
		planetSportsFilter.put("<female>", "Property_CF_gender:Women");
		planetSportsFilter.put("<boy>", "Property_CF_gender:Kids Boys");
		planetSportsFilter.put("<girl>", "Property_CF_gender:Kids Girls");
		planetSportsFilter.put("<child>", "Property_CF_gender:Kids Unisex");
		planetSportsFilter.put("<baby>", "");
		planetSportsFilter.put("<adult>", "");
	}
	private static String getShop(long programId){
		if (programId == 5643){
			return ZALANDO;
		}else if (programId == 1943){
			return PLANET;
		}
		return "";
	}
	private static String getProgramIdFilter(String shop){
		if (shop.equals(ZALANDO)){
			return "ProgramId:5643";
		}else if (shop.equals(PLANET)){
			return "ProgramId:1943";
		}
		return "";
	}
	private static String getGenderFilter(String genderTag, String shop){
		if (genderTag.trim().isEmpty()){
			return "";
		}
		if (shop.equals(ZALANDO)){
			return zalandoFilter.get(genderTag);
		}else if (shop.equals(PLANET)){
			return planetSportsFilter.get(genderTag);
		}
		return "";
	}
	private static String getColorFilter(String colorBase, String shop){
		if (colorBase.trim().isEmpty()){
			return "";
		}
		String filter = "";
		if (shop.equals(ZALANDO)){		//NOTE: colorBase is colorGerman for Zalando!
			filter = "Property_CF_Color:" + colorBase.replaceAll("ß", "ss").toLowerCase().trim();
		}else if (shop.equals(PLANET)){
			filter = "Property_CF_colour:" + colorBase.replaceAll("^<|>$", "").toLowerCase().trim();
		}
		return filter;
	}
	
	/**
	 * Save what can be saved.
	 */
	private static String handleExceptions(String product){
		if (product.matches(".*\\b(hoodie|hoodies|kapuzenpullover|kapuzenpulli(s|))\\b.*")){
			product = product.replaceFirst("\\b(hoodie|hoodies|kapuzenpullover|kapuzenpulli(s|))\\b", "(hoodie OR kapuzenpullover)");
		}
		else if (product.matches(".*\\b(unterhose|unterhosen)\\b.*")){
			product = product.replaceFirst("\\b(unterhose|unterhosen)\\b", "(unterwaesche OR unterhose)");
		}
		return product;
	}
	
	/**
	 * Try to make the description more readable.
	 */
	private static String tweakDescription(String desc, String product, String shop){
		if (shop.equals(ZALANDO)){
			desc = desc.replaceFirst("^(?i)" + Pattern.quote(product) + ".*?\\|", "")
					.replaceFirst("\\|.*?bestellen!", "");
		}else if (shop.equals(PLANET)){
			desc = desc.replaceFirst(".*?Features:", "Features: ");
			desc = desc.replaceFirst("Farbe:", "<br>Farbe:");
			desc = desc.replaceFirst("Material:", "<br>Material:");
		}
		desc = desc.replaceAll("([a-z])([A-Z])", "$1 $2").trim();
		desc = desc.replaceAll(":", ":" + "<br>");
		desc = desc.replaceAll(",", ",<br>");			//System.getProperty("line.separator")
		desc = desc.replaceAll("(ä|Ä|ö|Ö|ü|Ü) ", "$1");
		return desc;
	}
	//---------------------------------------------------------------------
	
	//info
	public ApiInfo getInfo(String language){
		//type
		ApiInfo info = new ApiInfo(Type.REST, Content.data, false);
		
		//Parameters:
		//required
		Parameter p1 = new Parameter(PARAMETERS.FASHION_ITEM)
				.setRequired(true)
				.setQuestion("fashion_ask_item_0a");
		info.addParameter(p1);
		//optional
		Parameter p2 = new Parameter(PARAMETERS.FASHION_SIZE, "");
		info.addParameter(p2);
		Parameter p3 = new Parameter(PARAMETERS.GENDER, "");
		info.addParameter(p3);
		Parameter p4 = new Parameter(PARAMETERS.COLOR, "");
		info.addParameter(p4);
		Parameter p5 = new Parameter(PARAMETERS.FASHION_BRAND, "");
		info.addParameter(p5);
		
		//Answers:
		info.addSuccessAnswer("fashion_1a")
			.addFailAnswer("fashion_0a")
			.addAnswerParameters("item");
		
		return info;
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result, getInfo(""));
		
		//get interview parameters
		JSONObject itemJSON = NLU_result.getRequiredParameter(PARAMETERS.FASHION_ITEM).getData();
		JSONObject colorJSON = NLU_result.getOptionalParameter(PARAMETERS.COLOR, "").getData();
		JSONObject genderJSON = NLU_result.getOptionalParameter(PARAMETERS.GENDER, "").getData();
		JSONObject sizeJSON = NLU_result.getOptionalParameter(PARAMETERS.FASHION_SIZE, "").getData();
		JSONObject brandJSON = NLU_result.getOptionalParameter(PARAMETERS.FASHION_BRAND, "").getData();
				
		//parameter adaptation to service format
		
		//SEARCH
		String itemToSay = "";
		String search = "";
		
		//product - required
		String product;
		if (api.language.equals(LANGUAGES.DE)){
			product = (String) itemJSON.get(InterviewData.VALUE_LOCAL);
		}else{
			product = (String) itemJSON.get(InterviewData.VALUE_LOCAL);
			//TODO: we need to translate it to German for the API (I think)
		}
		itemToSay += product + " ";
		
		//brand
		String brand = "";
		if (!brandJSON.isEmpty()){
			brand = (String) brandJSON.get(InterviewData.VALUE);
		}
		if (!product.contains(brand)){
			itemToSay += brand + " ";
		}
		
		search = handleExceptions(itemToSay);
		//System.out.println(search);
		
		//color
		String colorLocal = "";
		String colorTag = "";
		String colorGerman = "";
		if (!colorJSON.isEmpty()){
			colorTag = (String) colorJSON.get(InterviewData.VALUE); 
			colorLocal = (String) colorJSON.get(InterviewData.VALUE_LOCAL);
			colorGerman = Color.getLocal(colorTag, LANGUAGES.DE); 	//same as local if input language is DE
			
			itemToSay += colorLocal + " ";
		}		
		
		//gender
		String genderTag = "";
		String genderLocal = "";
		if (!genderJSON.isEmpty()){
			genderTag = (String) genderJSON.get(InterviewData.VALUE);
			genderLocal = (String) genderJSON.get(InterviewData.VALUE_LOCAL);
			
			itemToSay += genderLocal + " ";
		}
		
		//size - checked inside API answer
		String size = "";
		if (!sizeJSON.isEmpty()){
			size = (String) sizeJSON.get(InterviewData.VALUE);
			size = size.toLowerCase(); //just in case
			
			itemToSay += "in " + size;
		}
		
		itemToSay = itemToSay.replaceAll("\\s+", " ").trim();
		api.resultInfoPut("item", itemToSay);
		
		Debugger.println("cmd: shopping, item: " + itemToSay, 2);		//debug
		
		//build data
		
		//call API for each shop and add results:
		JSONArray products = new JSONArray();
		for (String shop : shops){
			try{
				String url = URLBuilder.getString("https://product-api.affili.net/V3/productservice.svc/JSON/SearchProducts",
						"?publisherId=", Config.affilinet_pubID,
						"&Password=", Config.affilinet_key,
						"&Query=", search,
						"&ImageScales=", "Image60,Image120,Image180,OriginalImage",
						"&SortBy=", "Score",
						"&LogoScales=", "Logo150,Logo468",
						"&PageSize=","20",
						"&FQ=", getProgramIdFilter(shop),
						"&FQ=", getColorFilter(shop.equals(ZALANDO)? colorGerman : colorTag, shop),
						"&FQ=", getGenderFilter(genderTag, shop));
				//System.out.println("fashion URL: " + url);		//debug
				
				long tic = System.currentTimeMillis();
				JSONObject response = Connectors.httpGET(url.trim());
				Statistics.addExternalApiHit("Affilinet");
				Statistics.addExternalApiTime("Affilinet", tic);
				//System.out.println("fashion RESULT: " + response.toJSONString());		//debug
				
				JSONObject summary = (JSONObject) response.get("ProductsSummary");
				if (((long)summary.get("Records")) > 0){
					JSON.addAll(products, (JSONArray) response.get("Products"));
				}
			
			//ERROR
			}catch (Exception e){
				Debugger.println("ShoppingFashion - shop: " + shop + " - error: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
			}
		}
		//check result
		if (products.isEmpty()){
			//build the API_Result and goodbye
			ApiResult result = api.buildApiResult(); 
			return result;
		}
		
		JSONArray planetArray = new JSONArray();
		JSONArray planetArrayTop = new JSONArray();
		JSONArray planetArrayBottom = new JSONArray();
		JSONArray zalandoArray = new JSONArray();
		JSONArray zalandoArrayTop = new JSONArray();
		JSONArray zalandoArrayBottom = new JSONArray();
		ArrayList<String> avoidDublicates = new ArrayList<>();
		for (Object o : products){
			boolean isTopResult = false; 		//top results can either be new, tagged by the shop or simply fit best to the user
			boolean isLowValue = false;			//low price or for some reason rather uninteresting product
			JSONObject p = (JSONObject) o;
			JSONObject card_data = new JSONObject();
		
			//name, description, brand, URL
			long pID = (long) p.get("ProgramId");
			String shop = getShop(pID);
			String pName = p.get("ProductName").toString();
			if (avoidDublicates.contains(pName)){
				//skip duplicates
				continue;
			}else{
				//remember
				avoidDublicates.add(pName);
			}
			String desc = tweakDescription(p.get("Description").toString(), pName, shop);
			String pBrand = p.get("Brand").toString();
			pName = pName.replaceFirst("^(?i)" + Pattern.quote(pBrand), "").trim();
			//score for brand
			if (!brand.isEmpty() && pBrand.toLowerCase().contains(brand)){
				//isTopResult = true;
			}
			JSON.add(card_data, "name", pName);
			JSON.add(card_data, "description", desc); 	//or: DescriptionShort
			JSON.add(card_data, "brand", pBrand);
			JSON.add(card_data, "URL", p.get("Deeplink1"));
			//price
			JSONObject shopPriceInfo = (JSONObject) p.get("PriceInformation");
			JSONObject shopPriceDetails = (JSONObject) shopPriceInfo.get("PriceDetails");
			JSONObject priceDetails = new JSONObject();
				double price = Converters.obj2Double(shopPriceDetails.get("Price"));
				double priceOld = Converters.obj2Double(shopPriceDetails.get("PriceOld"));
				if (priceOld > price){
					JSON.add(priceDetails, "onSale", true);
				}else{
					JSON.add(priceDetails, "onSale", false);
				}
				if (price < 10.0f){
					isLowValue = true;
				}
				JSON.add(priceDetails, "price", price);
				JSON.add(priceDetails, "priceOld", priceOld);
				JSON.add(priceDetails, "currency", shopPriceInfo.get("Currency"));
				JSON.add(priceDetails, "display", shopPriceInfo.get("DisplayPrice").toString().replaceAll("\\bEUR\\b", "€"));
			JSON.add(card_data, "priceDetails", priceDetails);
			//shipping
			JSONObject shippingDetails = new JSONObject();
				JSON.add(shippingDetails, "availability", p.get("Availability")); 		//TODO: take from properties
				//JSON.add(shippingDetails, "deliveryTime", p.get("DeliveryTime"));		//TODO: take from properties
				JSON.add(shippingDetails, "shippingCost", ((JSONObject) shopPriceInfo.get("ShippingDetails")).get("Shipping"));
				JSON.add(shippingDetails, "comment", ((JSONObject) shopPriceInfo.get("ShippingDetails")).get("ShippingSuffix"));
			JSON.add(card_data, "shippingDetails", shippingDetails);
			//images
			try{
				JSONArray shopImages = (JSONArray) ((JSONArray) p.get("Images")).get(0);
				JSONObject images = new JSONObject();
				for (Object io : shopImages){
					JSONObject i = (JSONObject) io;
					String iName = i.get("ImageScale").toString(); 
					if (iName.equals("OriginalImage")){
						JSON.add(images, "big", i.get("URL"));
					}else if (iName.equals("Image180")){
						JSON.add(images, "normal", i.get("URL"));
					}else if (iName.equals("Image60")){
						JSON.add(images, "small", i.get("URL"));
					}
				}
				JSON.add(card_data, "images", images);
			}catch(Exception e){
				JSON.add(card_data, "images", new JSONObject());
			}
			//properties
			JSONArray shopProperties = (JSONArray) p.get("Properties");
			if (shopProperties.isEmpty()){
				JSON.add(card_data, "properties", new JSONObject());
			}else{
				JSONObject properties = new JSONObject();
				for (Object io : shopProperties){
					JSONObject prop = (JSONObject) io;
					String iName = prop.get("PropertyName").toString().toLowerCase(); 
					if (iName.equals("cf_size")){
						//size
						String sizeList = prop.get("PropertyValue").toString().replaceAll(",", ".");
						String[] sizes = sizeList.split(";|\\|");
						boolean doesSizeMatch = false;
						if (!size.isEmpty()){
							for (String s : sizes){
								if (NluTools.stringContains(Pattern.quote(s).toLowerCase(), size)){
									doesSizeMatch = true;
									isTopResult = true;
								}
							}
						}
						JSON.add(properties, "size", sizeList);
						JSON.add(properties, "sizeMatch", doesSizeMatch);
					}else if (iName.equals("cf_colour") || iName.equals("cf_colour")){
						//color
						JSON.add(properties, "color", prop.get("PropertyValue"));
					}else if (iName.equals("cf_gender")){
						//gender
						JSON.add(properties, "gender", prop.get("PropertyValue"));
					}else if (iName.equals("cf_material")){
						//material
						JSON.add(properties, "material", prop.get("PropertyValue"));
						
					}else if (iName.equals("cf_lieferzeit") || iName.equals("cf_shippingtime")){
						//shipping time
						JSON.add(shippingDetails, "deliveryTime", prop.get("PropertyValue"));
					}else if (iName.equals("cf_availability")){
						//availability
						JSON.add(shippingDetails, "availability", prop.get("PropertyValue"));
						
					}else if (iName.equals("cf_top 10 of brand") || iName.equals("cf_top 25") || iName.equals("cf_new")){
						//top 10/25 brand? or new?
						String isOne = prop.get("PropertyValue").toString(); 
						if (!isOne.equals("0")){
							isTopResult = true;
						}
					}
				}
				JSON.add(card_data, "properties", properties);
			}
			
			//sort field
			if (pID == 1943){
				//planet
				if (isLowValue){
					JSON.add(planetArrayBottom, card_data);
				}else if (isTopResult){
					JSON.add(planetArrayTop, card_data);
				}else{
					JSON.add(planetArray, card_data);
				}
				
			}else if (pID == 5643){
				//zalando
				if (isLowValue){
					JSON.add(zalandoArrayBottom, card_data);
				}else if (isTopResult){
					JSON.add(zalandoArrayTop, card_data);
				}else{
					JSON.add(zalandoArray, card_data);
				}
				
			}
		}
		//Planet Sports
		JSONObject planetData = new JSONObject();
		JSON.addAll(planetArrayTop, planetArray);
		JSON.addAll(planetArrayTop, planetArrayBottom);
		JSON.add(planetData, "name", "<span style='color:#009FE3;'><b>Planet Sports</b></span>");
		JSON.add(planetData, "nameClean", "Planet Sports");
		JSON.add(planetData, "data", planetArrayTop);
		JSON.add(planetData, "image", Config.urlWebImages + "brands/" + "planet-sports-logo.png");
		JSON.add(planetData, "logo", Config.urlWebImages + "brands/" + "planet-sports-official.png");
		
		//Zalando
		JSONObject zalandoData = new JSONObject();
		JSON.addAll(zalandoArrayTop, zalandoArray);
		JSON.addAll(zalandoArrayTop, zalandoArrayBottom);
		JSON.add(zalandoData, "name", "<span style='color:#EC6339;'>zalando</span>");
		JSON.add(zalandoData, "nameClean", "zalando");
		JSON.add(zalandoData, "data", zalandoArrayTop);
		JSON.add(zalandoData, "image", Config.urlWebImages + "brands/" + "zalando-logo.png");
		JSON.add(zalandoData, "logo", Config.urlWebImages + "brands/" + "zalando-official.png");
		
		//build card
		Card card = new Card(Card.TYPE_GROUPED_LIST);
		card.addGroupeElement(ElementType.fashion, "1", zalandoData);
		card.addGroupeElement(ElementType.fashion, "2", planetData);
		
		//add it
		api.addCard(card.getJSON());
		api.hasCard = true;
		
		//all clear?
		api.status = "success";
		
		//finally build the API_Result
		ApiResult result = api.buildApiResult();
				
		//return result_JSON.toJSONString();
		return result;
	}

}
