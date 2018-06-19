package net.b07z.sepia.server.assist.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.IndexType;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_ElasticSearch {

	static Elasticsearch db;
	
	public static void main(String[] args) {
		
		//init
		Start.loadSettings(new String[]{"--test"});
		Config.setupDatabases();
		db = new Elasticsearch();
		
		/*
		JSONObject res = db.searchSimple(DB.COMMANDS, "sepia");
		System.out.println("res: " + res);
		JSONArray hits = JSON.getJArray(res, new String[]{"hits", "hits"});
		System.out.println("hits: " + ((hits == null)? "0" : hits.size()));
		*/
		
		//listCommands();
		
		//setCommand();
		//Debugger.sleep(2000);
		//getCommand();
		//System.out.println("deleted: " + DB.deleteCommand(DB.getIdOfCommand("uid1002", "de", "ich suche <color> sneakers für <gender>")));
				
		String id = "uid1010";
		
		//MAPPING		
		//writeCustomServiceMapping(id);
		//writeSystemServiceMapping(id);
		//clearCustomServiceMapping(id);
		//readCustomServiceMapping(id);
		
		//WRITE, READ, UPDATE, DELETE
		int code = writeUserIndex(id, JSON.make(
				ACCOUNT.GUUID, id,
				ACCOUNT.USER_NAME, JSON.make(
						Name.FIRST, "Mr",
						Name.LAST, "Testy"
				)
		));
		System.out.println("write: " + code);
		System.out.println(readUserIndex(id, null));
		
		System.out.println("user exists: " + userExists(id, ID.Type.uid));
		System.out.println("user exists: " + userExists("noid1010", ID.Type.uid));
		
		code = updateUserIndex(id, JSON.make( 
				ACCOUNT.INFOS, JSON.make(
						"lang_code", "de",
						"custom", JSON.make("some", "value", "more", "values")
				)
		));
		System.out.println("update1: " + code);
		System.out.println(readUserIndex(id, null));
		System.out.println(readUserIndex(id, new String[]{"infos"}));
		System.out.println(readUserIndex(id, new String[]{"infos.custom"}));
		
		code = updateUserIndex(id, JSON.make( 
				ACCOUNT.USER_NAME, JSON.make(
						Name.FIRST, "Mister"
				)
		));
		System.out.println("update2: " + code);
		System.out.println(JSON.getJObject(readUserIndex(id, null), new String[]{"_source"}));
		
		code = deleteFromUserIndex(id, "infoss");
		System.out.println("delete1: " + code);
		System.out.println(JSON.getJObject(readUserIndex(id, null), new String[]{"_source"}));
		
		code = deleteFromUserIndex(id, "infos.custom.more");
		System.out.println("delete2: " + code);
		System.out.println(JSON.getJObject(readUserIndex(id, null), new String[]{"_source"}));
		
		code = deleteFromUserIndex(id, "infos");
		System.out.println("delete3: " + code);
		System.out.println(JSON.getJObject(readUserIndex(id, null), new String[]{"_source"}));
				
		//int code = db.deleteItem(index, type, id);
		//System.out.println("RES. CODE: " + code);
		
		//int code = db.deleteAnything("storage/feedback_likes");
		//System.out.println("RES. CODE: " + code);
		
		//LIST
		//writeList(id);
		//getList(id);
		
		//WHITELIST
		//String userid = "*";
		//saveWhitelistUser(userid, "test");
		//searchWhitelistUser(userid);		
	}
	
	//---- WRITE, READ, UPDATE, DELETE ----
	
	public static int writeUserIndex(String userID, JSONObject data){
		return db.writeDocument(DB.USERS, "all", userID, data);
	}
	public static int updateUserIndex(String userID, JSONObject data){
		return db.updateDocument(DB.USERS, "all", userID, data);
	}
	public static JSONObject readUserIndex(String userID, String[] filters){
		JSONObject res;
		if (filters == null || filters.length == 0){
			res = db.getItem(DB.USERS, "all", userID);
		}else{
			res = db.getItemFiltered(DB.USERS, "all", userID, filters);
		}
		return res;
	}
	public static int deleteFromUserIndex(String userID, String fieldToDelete){
		return db.deleteFromDocument(DB.USERS, "all", userID, fieldToDelete);
	}
	public static String userExists(String identifier, String idType) throws RuntimeException{
		String key = "";
		if (idType.equals(ID.Type.uid)){
			key = ACCOUNT.GUUID; 
		}else if (idType.equals(ID.Type.email)){
			key = ACCOUNT.EMAIL;
		}else if (idType.equals(ID.Type.phone)){
			key = ACCOUNT.PHONE;
		}else{
			throw new RuntimeException("userExists(...) reports 'unsupported ID type' " + idType);
		}
		//all primary user IDs need to be lowerCase in DB!!!
		identifier = ID.clean(identifier);
		
		//search parameters:
		JSONObject response = db.searchSimple(DB.USERS + "/all", key + ":" + identifier);
		//System.out.println("RESPONSE: " + response.toJSONString());				//debug
		if (!Connectors.httpSuccess(response)){
			throw new RuntimeException("Authentication.userExists(...) reports 'DB query failed! Result unclear!'");
		}
		JSONArray hits = JSON.getJArray(response, new String[]{"hits", "hits"});
		if (hits != null && !hits.isEmpty()){
			return JSON.getJObject((JSONObject) hits.get(0), "_source").get(ACCOUNT.GUUID).toString();
		}else{
			return "";
		}
	}
	
	//------WHITELIST-------
	
	public static void saveWhitelistUser(String userId, String comment){
		JSONObject data = new JSONObject();
		JSON.add(data, "uid", userId);
		JSON.add(data, "info", comment);
		
		int code = JSON.getIntegerOrDefault(db.setAnyItemData(DB.WHITELIST, "users", data), "code", -1);
		System.out.println("save whitelist user - code: " + code);
	}
	public static void searchWhitelistUser(String userId){
		JSONObject data = db.searchSimple(DB.WHITELIST + "/" + "users", "uid:" + userId);
		try{
			System.out.println("whitelist user search: " + data.toJSONString());
			int hits = Converters.obj2Int(((JSONObject) data.get("hits")).get("total"));
			System.out.println("whitelist users found: " + hits);
		}catch (Exception e){
			Debugger.println("Whitelist search failed! ID: " + userId + " - error: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
			System.out.println("error or user not found!");
		}
	}
	
	//--------PERSONAL COMMANDS---------
	
	public static void listCommands(){
		HashMap<String, Object> filters = new HashMap<>();
		filters.put("userIds", "assistant,uid1002,gig1011,gig1012");
		filters.put("language", "de");
		filters.put("includePublic", new Boolean(true));
		//filters.put("searchText", "synonym");
		JSONArray res = DB.getCommands(filters);
		System.out.println("res: " + res);
		for (Object o : res){
			JSONObject jo = JSON.getJObject((JSONArray)((JSONObject)o).get("sentence"), 0);
			String text = (String) jo.get("tagged_text");
			if (text == null || text.isEmpty()) text = (String) jo.get("text"); 
			System.out.println(jo.get("user") + " - " + text + ";;" + jo.get("cmd_summary"));
		}
	}
	
	public static void getCommand(){
		String userId = "uid1002";
		String language = "de";
		String textToMatch = "ich suche sneakers für <gender>";
		HashMap<String, Object> filters = new HashMap<String, Object>();
		filters.put("userIds", userId);
		filters.put("language", language);
		filters.put("searchText", textToMatch);
		filters.put("matchExactText", new Boolean(true));
		JSONArray ja = DB.getCommands(filters);
		System.out.println("JA: " + ja);
		System.out.println("ID Match: " + DB.getIdOfCommand(userId, language, textToMatch));
	}
	
	public static void setCommand(){
		String command = "websearch";
		String userId = "uid1002";
		String language = "de";
		String text = "ich suche <color> sneakers für <gender>";
		
		HashMap<String, Object> filters = new HashMap<String, Object>();
		filters.put("userIds", userId);
		filters.put("cmd_summary", command + ";;");
		filters.put("source", "test"); 
		//filters.put("tagged_sentence", "");
		
		int code = DB.setCommand(command, text, language, true, filters);
		System.out.println("res. code: " + code);
	}
	
	//----------Services mapping-----------
	
	public static void writeCustomServiceMapping(String id){
		Set<CmdMap> cServices = new HashSet<>();
		
		CmdMap cmMap1 = new CmdMap("demo1", 
				Arrays.asList(id + ".Demo_Service1"), 
				Arrays.asList("birthDate", "gender"));
		CmdMap cmMap2 = new CmdMap("demo2", 
				Arrays.asList(id + ".Demo_Service2"), 
				Arrays.asList("locationHome", "contacts"));
		cServices.add(cmMap1);
		cServices.add(cmMap2);
		
		HashMap<String, Object> filters = new HashMap<>();
		filters.put("customOrSystem", CmdMap.CUSTOM);
		filters.put("userIds", id);
		
		int code = DB.setCommandMappings(true, cServices, filters);
		System.out.println("RES. CODE: " + code);
	}
	public static void writeSystemServiceMapping(String id){
		Set<CmdMap> cServices = new HashSet<>();
		
		CmdMap cmMap1 = new CmdMap("demo", 
				Arrays.asList("Demo_Service"), 
				Arrays.asList("all"));
		CmdMap cmMap2 = new CmdMap("demo2", 
				Arrays.asList("Demo_Service2"), 
				Arrays.asList("all"));
		cServices.add(cmMap1);
		cServices.add(cmMap2);
		
		HashMap<String, Object> filters = new HashMap<>();
		filters.put("customOrSystem", CmdMap.SYSTEM);
		filters.put("userIds", id);
		
		int code = DB.setCommandMappings(true, cServices, filters);
		System.out.println("RES. CODE: " + code);
	}
	public static void readCustomServiceMapping(String id){
		HashMap<String, Object> filters = new HashMap<>();
		filters.put("customOrSystem", CmdMap.CUSTOM);
		filters.put("userIds", id);
		
		List<CmdMap> customMap = DB.getCommandMappings(filters);
		if (customMap == null){
			//ERROR (maybe it did not exists before)
			customMap = new ArrayList<>();
			System.out.println("cmd: " + "-" + " - services: " + "nothing found, map was 'null'");
		}
		for (CmdMap cm : customMap){
			System.out.println("cmd: " + cm.getCommand() + " - services: " + cm.getServices() + " - perm.: " + cm.hasPermission("contacts"));
		}
	}
	public static void clearCustomServiceMapping(String id){
		int code = DB.clearCommandMappings(id);
		System.out.println("RES. CODE: " + code);
	}
	
	//----------- LISTS -----------
	
	public static void writeList(String id){
		JSONArray data = new JSONArray();
		/*JSON.add(data, JSON.make("name", "Milk", "checked", false), 
				JSON.make("name", "Honey", "checked", false), 
				JSON.make("name", "Mustard", "checked", false),
				JSON.make("name", "Cappucino", "checked", false));*/
		/*JSON.add(data, JSON.make("name", "Coffee", "checked", false), 
				JSON.make("name", "Noodles", "checked", false), 
				JSON.make("name", "Chicken", "checked", false),
				JSON.make("name", "Water", "checked", false));*/
		JSON.add(data, JSON.make("name", "Kaffee", "checked", false), 
			JSON.make("name", "Nudeln", "checked", false), 
			JSON.make("name", "Hühnchen", "checked", false),
			JSON.make("name", "Wasser", "checked", false));
		//JSON.add(data, JSON.make("name", "Milch"));
		
		String indexType = IndexType.shopping.name();
		UserDataList list1 = new UserDataList(id, Section.productivity, indexType, "Supermarkt", data);
		list1.type = Card.ElementType.userDataList.name();
		
		String docId = "AVtYot7f1TwjX0R0onD6"; 		//list1 for gig1011: AVtFXYfQCJMRJI-6jNm2
													//list2 for gig1011: AVtFZVRyCJMRJI-6jNm4
		
		JSONObject setResult = DB.setListData(docId, id, Section.productivity, indexType, list1.getJSON());
		System.out.println("RES.: " + setResult);
	}
	
	public static void getList(String id){
		String indexType = IndexType.shopping.name();
		HashMap<String, Object> filters = new HashMap<>();
		filters.put("title", "supermarket");

		List<UserDataList> res = DB.getListData(id, Section.productivity, indexType, filters);
		for (UserDataList udl : res){
			System.out.println("udl user: " + udl.user);
			System.out.println("udl title: " + udl.title);
			System.out.println("udl type: " + udl.type);
			System.out.println("udl image: " + udl.icon);
			System.out.println("udl data: " + udl.data);
		}
	}
	
	//-------GEO DATA--------	
	
	public static void saveGeoData(){
		String index = DB.STORAGE;
		String type = "geo_data";
		String id = "-51.9980_+7.770";
		
		JSONObject data = new JSONObject();
		JSON.add(data, "city", "Some City");
		JSON.add(data, "street", "Best street");
		
		int code = db.setItemData(index, type, id, data);
		System.out.println("RES. CODE: " + code);
	}
	public static void getGeoData(){
		String index = DB.STORAGE;
		String type = "geo_data";
		String id = "-51.9980_+7.770";
		
		JSONObject data = new JSONObject();
		data = db.getItemFiltered(index, type, id, new String[]{"name","isDemo"});
		System.out.println("DATA: " + data.toJSONString());
	}

}
