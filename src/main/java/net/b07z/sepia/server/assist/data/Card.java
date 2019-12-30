package net.b07z.sepia.server.assist.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;

/**
 * Helper class to build a Card for cardInfo.
 * 
 * @author Florian Quirin
 *
 */
public class Card {
	
	//card types
	public static final String TYPE_CLASSIC = "classic";		//pre-Alpha cards (a text, a link and an image)
	public static final String TYPE_SINGLE = "single";			//single result card
	public static final String TYPE_UNI_LIST = "uni_list";		//uniform list (all elements are same type)
	public static final String TYPE_VAR_LIST = "var_list";		//variable list (elements can be different types)
	public static final String TYPE_GROUPED_LIST = "grouped_list";	//list where the elements are grouped (elements can be different types)
	public static final String TYPE_CUSTOM = "custom";			//custom card using HTML code instead of JSON
	//element types
	public enum ElementType {
		classic,
		link,
		mobilityR, mobilityD,
		fashion,
		news,
		weatherNow,
		weatherTmo,
		weatherWeek,
		radio,
		userDataList
	}
	
	public int number_of_elements;	//an element of a card. Multiple 
	public String cardType;			//type of card determines the template used to display data. No type means "old" card.
	public JSONArray cardInfo;		//all data of a card
	
	public Card(){
		cardInfo = new JSONArray();
		number_of_elements = 0;
	}
	public Card(String cardType){
		this.cardType = cardType;
		this.cardInfo = new JSONArray();
		this.number_of_elements = 0;
	}
	
	/**
	 * Set the card type.
	 */
	public void setCardType(String type){
		cardType = type;
	}
	
	/**
	 * Get the card as JSON.
	 */
	public JSONObject getJSON(){
		JSONObject jo = new JSONObject();
		JSON.add(jo, "cardType", cardType);
		JSON.add(jo, "N", number_of_elements);
		JSON.add(jo, "info", cardInfo);
		return jo;
	}
	
	/**
	 * Add an element to the card. Elements can be considered sub-cards of the service and might or might not be displayed 
	 * as separate cards depending on cardType and client.
	 * @param type - (optional) set an element type other than default 
	 * @param data - (required) JSON string holding all required data for card type
	 * @param details - (optional) JSON string holding details for an extended view
	 * @param detailsHTML - (optional) HTML block holding details for an extended view  
	 * @param text - (optional) text info (description or short summary)
	 * @param redirectURL - (optional) link to element (e.g. to wikipedia, to homepage, etc.)
	 * @param imageURL - (optional) image to element
	 * @param actionInfo - (optional) JSON array with actions (see assistant.ACTIONS)
	 * @param customHTML - (optional) custom HTML data block that can be used to replace JSON + details + template
	 * @return the JSONObject that has been added to cards
	 */
	@SuppressWarnings("unchecked")
	public JSONObject addElement(ElementType type, JSONObject data, JSONObject details, String detailsHTML,
					String text, String redirectURL, String imageURL, JSONArray actionInfo, String customHTML){
		JSONObject element = new JSONObject();
		element.put("data", data);
		if (type != null && !type.name().isEmpty()) 		element.put("type", type.name());
		if (details != null && !details.isEmpty()) 			element.put("details", details);
		if (detailsHTML != null && !detailsHTML.isEmpty())	element.put("detailsHTML", detailsHTML);
		if (text != null && !text.isEmpty()) 				element.put("text", text);
		if (redirectURL != null && !redirectURL.isEmpty())	element.put("url", redirectURL);
		if (imageURL != null && !imageURL.isEmpty())		element.put("image", imageURL);
		if (actionInfo != null && !actionInfo.isEmpty())	element.put("action", actionInfo);
		if (customHTML != null && !customHTML.isEmpty())	element.put("customHTML", customHTML);
		//add it
		cardInfo.add(element);
		number_of_elements++;
		return element;
	}
	/**
	 * Add a group element to the card. Since the card is an array same group names will not overwrite each other but simply added to the list.<br>
	 * Note: might add fields to data: type, group
	 * @param type - (optional) set an element type other than default
	 * @param groupName - name of the group. If empty field will be skipped
	 * @param data - (required) JSON string holding all required data for card type
	 * @return the JSONObject that has been added to cards
	 */
	public JSONObject addGroupeElement(ElementType type, String groupName, JSONObject data){
		//add type to data
		JSON.put(data, "type", type.name());
		if (!groupName.isEmpty()){
			JSON.put(data, "group", groupName);
		}
		JSON.add(cardInfo, data);
		number_of_elements++;
		return data;
	}
	
	//------------- "old" card element builders, kept here for compatibility ------------------
	
	/**
	 * Add an element with default info to card. Typically you have 1 or 3 elements.
	 * @param text - text info of elements
	 * @param url_link - link to element (e.g. to wikipedia, to homepage, etc.)
	 * @param url_image - image to element
	 * @return the JSONObject that has been added to cards
	 */
	@SuppressWarnings("unchecked")
	public JSONObject addElement(String text, String url_link, String url_image){
		JSONObject element = new JSONObject();
		element.put("text", text);
		element.put("url", url_link);
		element.put("image", url_image);
		//add it
		cardInfo.add(element);
		number_of_elements++;
		return element;
	}
	/**
	 * Add an element with default info to card. Typically you have 1 or 3 elements.
	 * @param text - text info of elements
	 * @param url_link - link to element (e.g. to wikipedia, to homepage, etc.)
	 * @param url_image - image to element
	 * @param action_info - JSON array with actions (see assistant.ACTIONS)
	 * @return the JSONObject that has been added to cards
	 */
	@SuppressWarnings("unchecked")
	public JSONObject addElement(String text, String url_link, String url_image, JSONArray action_info){
		JSONObject element = new JSONObject();
		element.put("text", text);
		element.put("url", url_link);
		element.put("image", url_image);
		element.put("action", action_info);
		//add it
		cardInfo.add(element);
		number_of_elements++;
		return element;
	}
	
	/**
	 * has the card any elements?
	 */
	public boolean isEmpty(){
		return cardInfo.isEmpty();
	}

}
