package net.b07z.sepia.server.assist.services;

import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerStatics;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.ListType;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class handling lists.
 * 
 * @author Florian Quirin
 *
 */
public class Lists implements ServiceInterface{
	
	private static int lists_max_load = 20;		//maximum number of lists loaded with one call from server
	private static int list_limit = 50;			//this many items are allowed in the list
	private static int element_limit = 160;		//maximum length of an item
	
	//--- data ---
	public static String getButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "Liste öffnen";
		}else{
			return "Open list";
		}
	}
	public static String makeNewListName(String language, String indexType){
		if (language.equals(LANGUAGES.DE)){
			if (indexType.equals(UserDataList.IndexType.todo.name())){
				return "Zu erledigen";
			}else if (indexType.equals(UserDataList.IndexType.shopping.name())){
				return "Einkaufen";
			}else{
				return "Verschiedenes";
			}
		}else{
			if (indexType.equals(UserDataList.IndexType.todo.name())){
				return "To do";
			}else if (indexType.equals(UserDataList.IndexType.shopping.name())){
				return "Shopping";
			}else{
				return "Mixed";
			}
		}
	}
	/**
	 * Default item for to-do and shopping lists.
	 */
	public static JSONObject makeProductivityListItem(String name, String indexType){
		JSONObject item = new JSONObject();
		String type = UserDataList.EleType.checkable.name();
		String state = null;
		if (indexType.equals(UserDataList.IndexType.todo.name())){
			//type = UserDataList.EleType.checkable.name();
			state = UserDataList.EleState.open.name();			//usually only to-do lists have a state in addition to 'checked'
		}
		JSON.put(item, "name", name.trim());
		JSON.put(item, "eleType", type);
		JSON.put(item, "lastChange", System.currentTimeMillis());
		JSON.put(item, "checked", Boolean.FALSE);
		if (state != null){
			JSON.put(item, "state", state);
		}
		//JSON.put(item, "metaData", new JSONObject());
		return item;
	}
	//-------------
	
	//info
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.account, Content.data, false);
		
		//Parameters:
		//required
		/*
		Parameter p1 = new Parameter(PARAMETERS.LIST_TYPE)
				.setRequired(true)
				.setQuestion("lists_ask_type_0a");
		info.addParameter(p1);
		*/
		//optional
		Parameter p1 = new Parameter(PARAMETERS.LIST_TYPE).setQuestion("lists_ask_type_0a");
		Parameter p2 = new Parameter(PARAMETERS.ACTION, "");
		Parameter p3 = new Parameter(PARAMETERS.LIST_ITEM, "");
		Parameter p4 = new Parameter(PARAMETERS.LIST_SUBTYPE, "");
		info.addParameter(p1).addParameter(p2).addParameter(p3).addParameter(p4);
		
		//... but one of these optional parameters is required
		info.getAtLeastOneOf(askTypeCommon, p1, p4); 		//either type or sub-type is required, if both are missing ask for type
				
		//Answers:
		info.addSuccessAnswer("lists_0a")		//here is <1>
			.addOkayAnswer("lists_0b")			//not found
			.addFailAnswer("lists_0c")			//no access
			.addCustomAnswer("askType", askTypeCommon)
			.addCustomAnswer("askTypeOrTitle", askTypeOrTitle) 		//overwrites "lists_ask_type_0a"
			.addCustomAnswer("askTitleCreate", askTitleCreate)
			.addCustomAnswer("askItemAdd", askItemAdd)
			.addCustomAnswer("askItemRemove", askItemRemove)
			.addCustomAnswer("askListTypeAgain", askListTypeAgain)
			.addCustomAnswer("askListTypeAgainShow", askListTypeAgainShow)
			.addCustomAnswer("listTooLong", listTooLong)
			.addCustomAnswer("removeByHand", removeByHand)
			.addCustomAnswer("addedStuff", addedStuff)
			.addCustomAnswer("listNotFound", listNotFound)
			.addCustomAnswer("unknownAction", unknownAction);
		info.addAnswerParameters("listType", "listItem");
		
		return info;
	}
	static final String askTypeCommon = "lists_ask_type_1a";		//shop or todo?
	static final String askTypeOrTitle = "lists_ask_name_1a";		//name for search?
	static final String askTitleCreate = "lists_ask_name_1b";		//name for creation?
	static final String askItemAdd = "lists_ask_put_1a";			//ask put on list
	static final String askItemRemove = "lists_ask_remove_1a";		//ask remove
	static final String askListTypeAgain = "lists_ask_type_2a";  	//shop or todo?		<-- potential endless loop? see below
	static final String askListTypeAgainShow = "lists_ask_name_2a";	//name and show?
	static final String listTooLong = "lists_full_0a";				//full
	static final String removeByHand = "lists_remove_0a";			//remove by hand plz
	static final String addedStuff = "lists_put_1a"; 			//"<direct>Alles klaro, habe <2> deiner <1> hinzugefügt.";
	static final String listNotFound = "lists_not_found_0a"; 	//"<direct>Sorry, aber ich konnte keine Liste finden mit dem Namen <1>.";
	static final String unknownAction = "lists_unknown_0a";		//action not found

	
	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		User user = nluResult.input.user;
		
		//get parameters
		
		//required - moved all to optional and check for title or indexType first
		/*
		Parameter listTypeP = NLU_result.getRequiredParameter(PARAMETERS.LIST_TYPE);
		String listType = JSON.getStringOrDefault(listTypeP.getData(), InterviewData.LIST_TYPE, "");
		String listTypeLocal = JSON.getStringOrDefault(listTypeP.getData(), InterviewData.LIST_TYPE_LOCALE, "");
		*/
		
		//optional
		Parameter listTypeP = nluResult.getOptionalParameter(PARAMETERS.LIST_TYPE, "");
		String listType = JSON.getStringOrDefault(listTypeP.getData(), InterviewData.LIST_TYPE, "");
		String listTypeLocal = JSON.getStringOrDefault(listTypeP.getData(), InterviewData.LIST_TYPE_LOCALE, "");
		
		Parameter listSubTypeP = nluResult.getOptionalParameter(PARAMETERS.LIST_SUBTYPE, "");
		String listSubType = JSON.getStringOrDefault(listSubTypeP.getData(), InterviewData.VALUE, "");
		
		if (listTypeLocal.isEmpty()){
			api.resultInfoPut("listType", (listSubType + " " + ListType.getLocal("<list>", api.language)));
		}else{
			api.resultInfoPut("listType", (listSubType + " " + listTypeLocal).trim());
		}
		
		Parameter actionP = nluResult.getOptionalParameter(PARAMETERS.ACTION, Action.Type.show.name());
		String action = ((String) actionP.getDataFieldOrDefault(InterviewData.VALUE)).replaceAll("^<|>$", "").trim();
		boolean isActionAdd = (action.equals(Action.Type.set.name()) || action.equals(Action.Type.add.name()));
		boolean isActionShow = (action.equals(Action.Type.show.name()) || action.equals(Action.Type.edit.name()) || action.equals(Action.Type.on.name()));
		boolean isActionRemove = action.equals(Action.Type.remove.name());
		boolean isActionCreate = action.equals(Action.Type.create.name());
		
		Parameter listItemP = nluResult.getOptionalParameter(PARAMETERS.LIST_ITEM, "");
		String listItem = (String) listItemP.getDataFieldOrDefault(InterviewData.VALUE);
		
		api.resultInfoPut("listItem", listItem); 	//might get overwritten later
		
		Debugger.println("cmd: Lists, type=" + listType + ", subType=" + listSubType + ", action=" + action + ", listItem=" + listItem, 2);		//debug
		
		//check combined requirements - part I (pre list-load):
		
		String indexType = listType.replaceAll("^<|>$", "").trim();
		String title = listSubType;
		boolean missingTitle = title.isEmpty();
		boolean missingIndexType = indexType.isEmpty() || indexType.equals(UserDataList.IndexType.unknown.name());
		
		//------------------------------------------------------------------------------------------------------->
		//this might never be triggered anymore since we have 'info.getAtLeastOneOf(askTypeOrTitle, p1, p4)' now
		if (isActionShow && missingTitle && missingIndexType){
			//ask for type or name
			api.setIncompleteAndAsk(PARAMETERS.LIST_SUBTYPE, askListTypeAgainShow);
			ServiceResult result = api.buildResult();
			return result;
		
		}else if (isActionAdd && missingTitle && missingIndexType){
			//ask for type or name
			api.setIncompleteAndAsk(PARAMETERS.LIST_SUBTYPE, askTypeOrTitle);
			ServiceResult result = api.buildResult();
			return result;
		//<-------------------------------------------------------------------------------------------------------
			
		}else if (isActionAdd && listItem.isEmpty()){
			//ask for item
			api.setIncompleteAndAsk(PARAMETERS.LIST_ITEM, askItemAdd);
			ServiceResult result = api.buildResult();
			return result;
			
		}else if (isActionRemove && listItem.isEmpty()){
			//ask for item
			api.setIncompleteAndAsk(PARAMETERS.LIST_ITEM, askItemRemove);
			ServiceResult result = api.buildResult();
			return result;
		
		}else if (isActionCreate && missingTitle){
			//ask for title
			api.setIncompleteAndAsk(PARAMETERS.LIST_SUBTYPE, askTitleCreate);
			ServiceResult result = api.buildResult();
			return result;
		
		}else if (isActionCreate && missingIndexType){
			//ask for indexType
			api.setIncompleteAndAsk(PARAMETERS.LIST_TYPE, askListTypeAgain);
			ServiceResult result = api.buildResult();
			return result;
		}
				
		//Load list
		UserDataInterface userData = user.getUserDataAccess();
		List<UserDataList> udlList;
		HashMap<String, Object> filters = new HashMap<>();
		
		//list title
		if (!title.isEmpty()){
			//we trust the database to find multiple lists that fit to the search term (because it uses a OR b OR c for an 'a b c' list)
			//so we can remove and, or, ... from search term ...
			if (nluResult.language.equals(LANGUAGES.DE)){
				title = title.replaceAll("\\b(und|oder)\\b", " ").replaceAll("\\s+", " ");
			}else if (nluResult.language.equals(LANGUAGES.EN)){
				title = title.replaceAll("\\b(and|or)\\b", " ").replaceAll("\\s+", " ");
			}
			filters.put("title", title);
		}
		
		//results size and pagination
		filters.put("resultsFrom", 0);					//start at first result
		filters.put("resultsSize", lists_max_load);		//we support 20 lists with one call for now
		
		//list section (a classification for this type of lists)
		Section section = Section.productivity;
		
		//get
		udlList = userData.getUserDataList(user, section, indexType, filters);
				
		//server communication error?
		if (udlList == null){
			api.setStatusFail();
			ServiceResult result = api.buildResult();
			return result;
			
		//no list result found ... (search can be title only, title+index or index only at this point)
		}else if (udlList.isEmpty()){
			
			//check combined requirements - part II (post list-load):
			
			//...  and no indexType?
			if (missingIndexType){
				if (isActionAdd || isActionRemove){
					//ask for type since the "add" action also became a "create" action now
					api.setIncompleteAndAsk(PARAMETERS.LIST_TYPE, askListTypeAgain);
				}
				ServiceResult result = api.buildResult();
				return result;
			}
		}
		
		//continue:

		UserDataList activeList = null;
		String _id = "";
		//check the list result titles for the correct one
		if (udlList.size() > 1){
			//if title is empty and action is 'add' then take the default name as reference
			if (title.isEmpty() && (isActionAdd || isActionRemove)){
				String ref = makeNewListName(api.language, indexType);
				boolean foundRef = false;
				int bestIndex = 0;
				/*
				int bestDistance = Integer.MAX_VALUE; 	//smaller is better
				*/
				for (int i=0; i<udlList.size(); i++){
					//search for best match to ref. name
					/*
					int thisDistance = (new SentenceMatch(ref, udlList.get(i).title)).getEditDistance().editDistance;
					if (thisDistance < bestDistance){
						bestDistance = thisDistance;
						bestIndex = i;
					}
					*/
					//search for a match equal to ref. name
					if (udlList.get(i).title.equalsIgnoreCase(ref)){
						bestIndex = i;
						foundRef = true;
					}
				}
				//found list?
				if (foundRef){
					activeList = udlList.get(bestIndex);
					_id = activeList._id;
					//show only this list
					udlList.clear();
					udlList.add(activeList);
				//not found?
				}else{
					udlList.clear();
					//let action handle the situation (e.g. create new list)
				}
				
			//else trust the ES
			}else{
				activeList = udlList.get(0);
				_id = activeList._id;
			}
		
		}else if (udlList.size() == 1){
			//TODO: what if its the wrong list? Assume that if there is only one it will be the correct one?
			activeList = udlList.get(0);
			_id = activeList._id;
		
		}else{
			//handled empty individually by action
		}

		//-CREATE
		if (isActionCreate){
			if (activeList == null){
				//create a list (title cannot be empty here)
				activeList = new UserDataList(user.getUserID(), Section.productivity, indexType, title, new JSONArray());
				udlList.add(activeList);
				
				//save empty list
				JSONObject writeResult = userData.setUserDataList(user, Section.productivity, activeList);
				//System.out.println("RESULT CODE: " + code); 		//debug
				if (writeResult.containsKey("status") && JSON.getString(writeResult, "status").equals("success")){
											
					//add _id to result in case it was a new list
					_id = (String) writeResult.get("_id");
					if (_id != null && !_id.isEmpty()){
						activeList._id = _id;
					}
				
				}else{
					//build answer - server communication error
					api.setStatusFail();
					ServiceResult result = api.buildResult();
					return result;
				}
			}
			api.setStatusSuccess();
		
		//-ADD
		}else if (isActionAdd){
			if (activeList == null){
				//create a list before adding the item
				if (title.isEmpty()){
					title = makeNewListName(api.language, indexType);
				}
				activeList = new UserDataList(user.getUserID(), Section.productivity, indexType, title, new JSONArray());
				udlList.add(activeList);
			
			}else if (activeList.data == null){
				activeList.data = new JSONArray();
			}
			
			//trim size
			if (listItem.length() > element_limit){
				listItem = listItem.substring(0, element_limit-2) + "...";
			}
			//split info and construct a nice sentence
			String[] items = listItem.split(" && ");
			String items_to_say = listItem;
			String and = " " + AnswerStatics.get(AnswerStatics.AND, api.language) + " ";
			if (items.length == 2){
				items_to_say = items[0] + and + items[1];
			}else if (items.length == 3){
				items_to_say = items[0] + ", " + items[1] + and + items[2];
			}else if (items.length > 3){
				items_to_say = items[0] + ", ";
				for (int i=1; i<=(items.length-2); i++){
					items_to_say += items[i] + ", "; 
				}
				items_to_say += and + items[items.length-1]; 
			}
			api.resultInfoPut("listItem", items_to_say); 	//overwrite initial
			
			//check size of list
			if ((activeList.data.size() + items.length) >= list_limit){
				//build answer - list is too big
				api.setStatusOkay();
				api.setCustomAnswer(listTooLong);
				ServiceResult result = api.buildResult();
				return result;

			//modify and save
			}else{
				for (String i : items){
					JSON.add(activeList.data, makeProductivityListItem(i, indexType));
				}
				//System.out.println("DATA: " + activeList.data); 		//debug
				JSONObject writeResult = userData.setUserDataList(user, Section.productivity, activeList);
				//System.out.println("RESULT CODE: " + code); 		//debug
				if (writeResult.containsKey("status") && JSON.getString(writeResult, "status").equals("success")){
											
					//add _id to result in case it was a new list
					_id = (String) writeResult.get("_id");
					if (_id != null && !_id.isEmpty()){
						activeList._id = _id;
					}
					
					//build answer - all fine I've added stuff
					api.setStatusSuccess();
					api.setCustomAnswer(addedStuff);
					
					//END - wait for build
				
				}else{
					//build answer - server communication error
					api.setStatusFail();
					ServiceResult result = api.buildResult();
					return result;
				}
			}
		
		//-SHOW / EDIT
		}else if (isActionShow){
			//build answer - ok let me open it
			if (udlList.isEmpty()){
				api.resultInfoPut("listType", listSubType);
				api.setStatusOkay();
				api.setCustomAnswer(listNotFound);
			}else{
				api.setStatusSuccess();
			}
			
			//END - wait for build
		
		//-REMOVE
		}else if (isActionRemove){
			//build answer - do it by hand
			api.setStatusOkay();
			if (udlList.isEmpty()){
				api.resultInfoPut("listType", listSubType);
				api.setCustomAnswer(listNotFound);
			}else{
				api.setCustomAnswer(removeByHand);
			}
			
			//END - wait for build
		
		//-UNKNOWN ACTION?
		}else{
			api.setStatusOkay();
			api.setCustomAnswer(unknownAction);
			Debugger.println("List_Basic - is missing handler for action: " + action, 3);
		}
				
		//ACTION and CARD
		if (udlList != null && !udlList.isEmpty()){
			//build action - for apps indicate direct triggering of info view
			api.addAction(ACTIONS.OPEN_LIST);
			api.putActionInfo("listInfo", JSON.make("indexType", indexType, "title", title, "_id", _id));
			api.hasAction = true;
		
			//make card
			Card card = new Card(Card.TYPE_UNI_LIST);
			for (UserDataList udl : udlList){
				card.addGroupeElement(ElementType.userDataList, udl.indexType, udl.getJSON());
			}
			api.addCard(card);
			api.hasCard = true;
		}
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		//System.out.println(result.getResultJSON()); 		//debug
		return result;
	}	

}
