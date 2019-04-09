package net.b07z.sepia.server.assist.database;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import net.b07z.sepia.server.assist.data.Address;
import net.b07z.sepia.server.assist.data.Word;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.AccountInterface;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.data.Command;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.data.SentenceBuilder;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.IndexType;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.database.DatabaseInterface;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.EsQueryBuilder;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Security;
import net.b07z.sepia.server.core.tools.Timer;
import net.b07z.sepia.server.core.tools.EsQueryBuilder.QueryElement;
import net.b07z.sepia.server.core.users.AuthenticationInterface;

/**
 * Access to all kinds of databases used for the assistant.<br>
 * The methods defined here can be very generic and lack certain security checks so make sure that you add your own checks if you 
 * use any of the methods in your service or end-point!<br>
 * I've tried to use interfaces as good as possible but certain requests are very specific for Elasticsearch and cannot
 * easily be replaced with other databases (hopefully we don't have to ^^).
 * 
 * @author Florian Quirin
 *
 */
public class DB {
	
	//statics
	public static final String USERS = "users";				//essential user data like account
	public static final String TICKETS = "tickets";			//tickets with unique IDs that can be a short info like reg. token or some event 
	public static final String STORAGE = "storage";			//unsorted data for later processing
	public static final String KNOWLEDGE = "knowledge";		//processed and sorted data for queries - TODO: unused?
	public static final String USERDATA = "userdata";		//all kinds of user data entries like cmd-mapping, lists, alarms, ... - Type: services, alarms, lists, ..., ID: userID
	public static final String COMMANDS = "commands";		//system and personal commands - Type: Command.COMMANDS_TYPE
	public static final String ANSWERS = Answer.ANSWERS_INDEX; 		//system and personal answers - Type: Answer.ANSWERS_TYPE;
	public static final String WHITELIST = "whitelist";		//white-lists of e.g. users
	
	//----------Database Implementations----------

	private static AccountInterface accounts = (AccountInterface) ClassBuilder.construct(Config.accountModule);				//USER ACCOUNT STUFF
	//note: you can use Account_DynamoDB.setTable(...) to access other tables in DynamoDB
	private static DatabaseInterface knowledge = (DatabaseInterface) ClassBuilder.construct(Config.knowledgeDbModule);		//BASICALLY EVERYTHING ELSE
	
	/**
	 * Refresh the settings for accounts and knowledge database.
	 */
	public static void refreshSettings(){
		GUID.setup();
		accounts = (AccountInterface) ClassBuilder.construct(Config.accountModule);
		knowledge = (DatabaseInterface) ClassBuilder.construct(Config.knowledgeDbModule);
	}
	
	private static AuthenticationInterface getAuthDb(){
		return (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
	}
	
	//----------Account methods----------
		
	//GET
	/**
	 * Get account info for id. Respects account access restrictions.
	 * @param account_id - user id aka email
	 * @param keys - account info to load
	 * @return HashMap<String, Object> with key values (user.info)
	 */
	public static Map<String, Object> getAccountInfos(String account_id, String... keys) {
		Map<String, Object> info = new HashMap<String, Object>();
		//create superuser
		User user = createSuperuser(account_id);
		//get info
		int res_code = accounts.getInfos(user, Config.superuserApiMng, keys);
		if (res_code == 0){
			info = user.info;
		}
		return info;
	}
	/**
	 * Get object from account by using id. Respects account access restrictions.
	 * @param account_id - user id aka email
	 * @param key - account info to load
	 * @return object loaded from account
	 */
	public static Object getAccountObject(String account_id, String key) {
		//create superuser
		User user = createSuperuser(account_id);
		return accounts.getInfoObject(user, Config.superuserApiMng, key);
	}
	
	//SET
	/**
	 * Set account info of user id via "super" API-manager. Respects account access restrictions.
	 * @param account_id - user id
	 * @param data - JSON with (full or partial) document data to set/update
	 * @return error code (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error (e.g. wrong key combination))
	 */
	public static int setAccountInfos(String account_id, JSONObject data) {
		//create superuser
		User user = createSuperuser(account_id);
		return accounts.setInfos(user, Config.superuserApiMng, data);
	}
	/**
	 * Set account info of user id via "super" API-manager. Respects account access restrictions.
	 * @param account_id - user id
	 * @param key - keys to set
	 * @param object - values to keys
	 * @return error code (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error (e.g. wrong key combination))
	 */
	public static int setAccountInfoObject(String account_id, String key, Object object) {
		//create superuser
		User user = createSuperuser(account_id);
		return accounts.setInfoObject(user, Config.superuserApiMng, key, object);
	}
	
	/**
	 * Read user-roles directly from DB without security restrictions.
	 * @param userId - ID of user to edit (NOT email! GUUID)
	 * @return null or empty on error else List
	 */
	@SuppressWarnings("unchecked")
	public static List<String> readUserRolesDirectly(String userId){
		//TODO: I'm not happy with splitting this up here by database. We could add it to the account-interface
		//but in principle we don't want to have unsecured methods there :-(
		
		//Elasticsearch
		if (Config.authAndAccountDB.equals("elasticsearch")){
			Elasticsearch es = new Elasticsearch();
			//Connect
			JSONObject res = es.getItemFiltered(DB.USERS, "all", userId, new String[]{ACCOUNT.ROLES});
			if (!Connectors.httpSuccess(res)){
				return null;
			}else{
				try {
					JSONObject item = JSON.getJObject(res, "_source");
					JSONArray foundRoles = JSON.getJArray(item, new String[]{ ACCOUNT.ROLES });
					return foundRoles;
					
				}catch (Exception e){
					return new ArrayList<>();
				}
			}
		//DynamoDB
		}else if (Config.authAndAccountDB.equals("dynamo_db")){
			//Connect
			JSONObject res = DynamoDB.getItem(DB.USERS, DynamoDB.PRIMARY_USER_KEY, userId, ACCOUNT.ROLES);
			if (!Connectors.httpSuccess(res)){
				return null;
			}else{
				try {
					JSONObject item = (JSONObject) res.get("Item");
					Object o = DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.ROLES));
					return Converters.object2ArrayListStr(o);
					
				}catch (Exception e){
					return new ArrayList<>();
				}
			}
		//ERROR
		}else{
			throw new RuntimeException("Reading account data directly failed due to missing DB implementation!");
		}
	}
	/**
	 * Write data for a user directly without security restrictions (we need this to set user-roles for example).<br>
	 * NOTE: use only if you KNOW what you are doing!
	 * @param userId - ID of user to edit (NOT email! GUUID)
	 * @param data - JSON with (full or partial) document data to set/update
	 * @return success/fail
	 * @throws Exception
	 */
	public static boolean writeAccountDataDirectly(String userId, JSONObject data) throws Exception{
		//TODO: I'm not happy with splitting this up here by database. We could add it to the account-interface
		//but in principle we don't want to have unsecured methods there :-(
		int code;
		
		//Elasticsearch
		if (Config.authAndAccountDB.equals("elasticsearch")){
			Elasticsearch es = new Elasticsearch();
			//Connect
			code = es.updateDocument(DB.USERS, "all", userId, data);
		
		//DynamoDB
		}else if (Config.authAndAccountDB.equals("dynamo_db")){
			JSONObject flatJson = JSON.makeFlat(data, "", null);
			ArrayList<String> keys = new ArrayList<>();
			ArrayList<Object> objects = new ArrayList<>();
			for (Object kO : flatJson.keySet()){
				String k = kO.toString();
				keys.add(k);
				objects.add(flatJson.get(k));
			}
			//Connect
			code = DynamoDB.writeAny(DB.USERS, DynamoDB.PRIMARY_USER_KEY, userId, 
					keys.toArray(new String[0]), objects.toArray(new Object[0]));
		//ERROR
		}else{
			throw new RuntimeException("Writing account data directly failed due to missing DB implementation!");
		}
		if (code != 0){
			throw new RuntimeException("Writing account data directly for user '" + userId + "' failed with code: " + code);
		}
		return true;
	}
	
	/**
	 * Create a user by using an email address (fake or real, the email will not actually be sent).
	 * @param email - email address
	 * @param pwd - password with at least 8 characters (unhashed)
	 * @return - JSON with GUUID, EMAIL and PASSWORD (client hashed) 
	 */
	public static JSONObject createUserDirectly(String email, String pwd) throws Exception{
		if (pwd.length() < 8){
			throw new RuntimeException("Password has to have at least 8 characters!");
		}
		String pass = Security.hashClientPassword(pwd);
		//String idType = ID.Type.email;
		AuthenticationInterface auth = getAuthDb();
		
		JSONObject requestRes = auth.registrationByEmail(email);
		//System.out.println(requestRes); 			//DEBUG
		if (!requestRes.containsKey("token")){
			int code = auth.getErrorCode();
			if (code == 5){
				throw new RuntimeException("Registration request failed (C5): User already exists or is not on white-list (if active)!");
			}else{
				throw new RuntimeException("Registration request failed (C" + auth.getErrorCode() + ")");
			}
		}
		JSON.put(requestRes, "pwd", pass);
		if (!auth.createUser(requestRes)){
			throw new RuntimeException("Creating user failed (C" + auth.getErrorCode() + ")");
		}
		Timer.threadSleep(1100);
		//test if user exists
		String guuid = auth.userExists(email, ID.Type.email);
		if (guuid.isEmpty()){
			throw new RuntimeException("ERROR! Could not create user: " + email);
		}else{
			Debugger.println("Direct-write: new user successfully created - GUUID: " + guuid + ", EMAIL: " + email, 3);
		}
		JSONObject res = JSON.make(
				ACCOUNT.GUUID, guuid,
				ACCOUNT.EMAIL, email,
				ACCOUNT.PASSWORD, pass
		);
		return res;
	}
	/**
	 * Check if a user exists to the given email. If so return GUUID else return empty string or error.
	 * @param email - email address used at user registration
	 * @return
	 * @throws Exception
	 */
	public static String checkUserExistsByEmail(String email) throws Exception{
		AuthenticationInterface auth = getAuthDb();
		String guuid = auth.userExists(email, ID.Type.email);
		return guuid;
	}
	
	//------------Knowledge / User-data--------------
	
	
	//------- SERVICE-MAPPINGS START --------
	
	/**
	 * Get mapping of a certain command to one or more services (and permissions). Filters are e.g.:<br>
	 * "customOrSystem" (custom,system) and "userIds" (for now just one user ID).
	 * @return null if there was an error, else list (can be empty)
	 */
	public static List<CmdMap> getCommandMappings(Map<String, Object> filters){
		long tic = Debugger.tic();
		
		String customOrSystem = (filters.containsKey("customOrSystem"))? (String) filters.get("customOrSystem") : CmdMap.SYSTEM;
		String userIds = (filters.containsKey("userIds"))? (String) filters.get("userIds") : "";
		if (userIds.isEmpty()){
			userIds = Config.assistantId;
		}else if (userIds.contains(",")){
			//List<String> userIdList = new ArrayList<>();
			//userIdList.addAll(Arrays.asList(userIds.split(",\\s*"))); 	//not yet supported
			throw new RuntimeException("getCommandMappings - MULTIPLE userIDs are not (yet) supported!");
		}
		String id = userIds;

		JSONObject data = new JSONObject();
		data = knowledge.getItem(DB.USERDATA, CmdMap.MAP_TYPE, id + "/_source");
		
		List<CmdMap> map = null;
		if (Connectors.httpSuccess(data)){
			map = CmdMap.makeMapList((JSONArray) data.get(customOrSystem));
		}
		
		//statistics
		Statistics.addOtherApiHit("getCommandMappingsFromDB");
		Statistics.addOtherApiTime("getCommandMappingsFromDB", tic);
		      	
		return map;
	}
	/**
	 * Set mapping of certain commands to one or more services (and permissions). Filters are e.g.:<br>
	 * "customOrSystem" (custom,system) and "userIds" (for now just one user ID).<br>
	 * Use 'loadExisting=true' to load the existing mappings before otherwise they are lost.
	 * @return error code (0=all good)
	 */
	public static int setCommandMappings(boolean loadExisting, Set<CmdMap> mappings, Map<String, Object> filters){
		long tic = Debugger.tic();
		
		String customOrSystem = (filters.containsKey("customOrSystem"))? (String) filters.get("customOrSystem") : CmdMap.SYSTEM;
		String userIds = (filters.containsKey("userIds"))? (String) filters.get("userIds") : "";
		if (userIds.isEmpty()){
			userIds = Config.assistantId;
		}else if (userIds.contains(",")){
			//List<String> userIdList = new ArrayList<>();
			//userIdList.addAll(Arrays.asList(userIds.split(",\\s*"))); 	//not yet supported
			throw new RuntimeException("setCommandMappings - MULTIPLE userIDs are not (yet) supported!");
		}
		String id = userIds;
		
		if (loadExisting){
			List<CmdMap> cmdMappings = getCommandMappings(filters);
			if (cmdMappings == null){
				//ERROR (maybe it did not exists before)
				cmdMappings = new ArrayList<>();
			}
			mappings.addAll(cmdMappings);
		}
		JSONObject customOrSystemData = new JSONObject();
		JSONArray mappingsArray = new JSONArray();
		for (CmdMap cm : mappings){
			JSON.add(mappingsArray, cm.getJSON());
		}
		JSON.put(customOrSystemData, customOrSystem, mappingsArray);
		//JSON.printJSONpretty(customOrSystemData);
		
		int code = knowledge.updateItemData(DB.USERDATA, CmdMap.MAP_TYPE, id, customOrSystemData);
		
		//statistics
		Statistics.addOtherApiHit("setCommandMappingsInDB");
		Statistics.addOtherApiTime("setCommandMappingsInDB", tic);
				
		return code;
	}
	/**
	 * Clear all command->services mappings for a user.
	 * @return error code (0=all good)
	 */
	public static int clearCommandMappings(String userId){
		JSONObject customAndSystemData = new JSONObject();
		JSON.put(customAndSystemData, CmdMap.CUSTOM, new JSONArray());
		JSON.put(customAndSystemData, CmdMap.SYSTEM, new JSONArray());
		
		int code = knowledge.setItemData(DB.USERDATA, CmdMap.MAP_TYPE, userId, customAndSystemData);
		return code;
	}
	
	//------- SERVICE-MAPPINGS END --------
	
	//------- COMMANDS START --------
	
	/**
	 * Get a command from the database with certain filters like:<br>
	 * "language" (String/en), "includePublic" (boolean/true), "searchText" (String/""), "userIds" (List as String like "a,b,..."), "matchExactText" (true/false) 
	 * @return JSONArray of commands as saved in db
	 */
	public static JSONArray getCommands(Map<String, Object> filters){
		long tic = Debugger.tic();
		
		String userIds = (filters.containsKey("userIds"))? (String) filters.get("userIds") : "";
		if (userIds.isEmpty() && filters.containsKey("userId")){
			userIds = (String) filters.get("userId");
		}
		if (userIds.isEmpty()){
			userIds = Config.assistantId;
		}
		List<String> userIdList = new ArrayList<>();
		userIdList.addAll(Arrays.asList(userIds.split(",\\s*")));
		Set<String> userIdSet = new HashSet<>(userIdList);
		String language = (filters.containsKey("language"))? (String) filters.get("language") : "en";
		boolean includePublic = (filters.containsKey("includePublic"))? (boolean) filters.get("includePublic") : true;
		String searchText = (filters.containsKey("searchText"))? (String) filters.get("searchText") : "";
		boolean matchExactText = (filters.containsKey("matchExactText"))? (boolean) filters.get("matchExactText") : false;
		
		//this is heavily depending on elasticSearch specific code ...
		//TODO: replace with EsQueryBuilder.getMixedRootAndNestedBoolMustMatch
		
		StringWriter sw = new StringWriter();
		try {
			try (JsonGenerator g = new JsonFactory().createGenerator(sw)){
				startNestedQuery(g, 0);

				//TODO: add info about the "size" of results somewhere here?

				// match at least one of the users:
				g.writeArrayFieldStart("should");
				for (String userId : userIdSet) {
					g.writeStartObject();
						g.writeObjectFieldStart("match");
							g.writeObjectFieldStart("sentences.user");
								g.writeStringField("query", userId);
								g.writeStringField("analyzer", "keylower");
							g.writeEndObject();
						g.writeEndObject();
					g.writeEndObject();
				}
				g.writeEndArray();
				g.writeNumberField("minimum_should_match", 1);
				
				g.writeArrayFieldStart("must");

				g.writeStartObject();
					g.writeObjectFieldStart("match");
						g.writeStringField("sentences.language", language);
					g.writeEndObject();
				g.writeEndObject();

				if (!includePublic){
					g.writeStartObject();
						g.writeObjectFieldStart("match");
							g.writeBooleanField("sentences.public", false);
						g.writeEndObject();
					g.writeEndObject();
				}
				
				if (!searchText.isEmpty()){
					g.writeStartObject();
						g.writeObjectFieldStart("multi_match");
							g.writeStringField("query", searchText);
							g.writeStringField("analyzer", "standard"); 		//use: asciifolding filter?
							if (matchExactText){
								g.writeStringField("operator", "and");
							}else{
								g.writeStringField("operator", "or");
							}
							g.writeArrayFieldStart("fields");
								g.writeString("sentences.text");
								g.writeString("sentences.tagged_text");		//use this with "and" and the right pattern to replace <...> tags
							g.writeEndArray();
						g.writeEndObject();
					g.writeEndObject();
				}

				endNestedQuery(g);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//System.out.println(sw.toString()); 	//debug
		
		JSONObject result = knowledge.searchByJson(COMMANDS + "/" + Command.COMMANDS_TYPE, sw.toString());
		//System.out.println(result); 			//debug
		JSONArray output = new JSONArray();
		JSONArray hits = JSON.getJArray(result, new String[]{"hits", "hits"});
		if (hits != null){
			for (Object hitObj : hits) {
				JSONObject hit = (JSONObject) hitObj;
				JSONObject hitSentence = new JSONObject();
				JSONObject source = (JSONObject) hit.get("_source");
				String id = (String) hit.get("_id");
				JSON.add(hitSentence, "sentence", source.get("sentences"));
				JSON.add(hitSentence, "id", id);
				JSON.add(output, hitSentence);
			}
		}
		
		//statistics
		Statistics.addOtherApiHit("getCommandsFromDB");
      	Statistics.addOtherApiTime("getCommandsFromDB", tic);
		
		return output;
	}
	/**
	 * Search a command defined by the userId, language and a text to match.
	 * @return ID or empty string
	 */
	public static String getIdOfCommand(String userId, String language, String textToMatch){
		Map<String, Object> getFilters = new HashMap<String, Object>();
		getFilters.put("userIds", userId);
		getFilters.put("language", language);
		getFilters.put("searchText", textToMatch);
		getFilters.put("matchExactText", new Boolean(true));
		
		JSONArray matchingSentences = getCommands(getFilters);
		String itemId = "";
		try{
			for (Object o : matchingSentences){
				JSONObject jo = (JSONObject) o;
				JSONObject sentence = (JSONObject) JSON.getJArray(jo, new String[]{"sentence"}).get(0);
				String text = (String) sentence.get("text");
				String textTagged = (String) sentence.get("tagged_text");
				if (textToMatch.equalsIgnoreCase(text) || textToMatch.equalsIgnoreCase(textTagged)){
					itemId = (String) jo.get("id");
					break;
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return itemId;
	}
	/**
	 * Write a new command to the database.
	 * @param command - the command to connect to ... 
	 * @param sentence - ... this sentence. Can contain 'variables' (will be used as 'tagged_sentence' in that case)
	 * @param language - 'de', 'en', ...
	 * @param overwriteExisting - search for a sentence match before writing to database
	 * @param filters - Essential filters are e.g.:<br>'tagged_sentence', 'userId', 'cmd_summary', 'source' ...
	 * @return code (0=all good)
	 */
	public static int setCommand(String command, String sentence, String language, boolean overwriteExisting, Map<String, Object> filters){
		//TODO: accept multiple sentences at once
		
		long tic = Debugger.tic();
		
		String taggedSentence = (String) filters.get("tagged_sentence");
		if (taggedSentence == null || taggedSentence.isEmpty()){
			if (Word.hasTag(sentence)){
				taggedSentence = sentence;
				sentence = "";
			}
		}
		
		String userId = (filters.containsKey("userIds"))? (String) filters.get("userIds") : "";
		if (userId.isEmpty() && filters.containsKey("userId")){
			userId = (String) filters.get("userId");
		}
		if (userId.isEmpty()){
			userId = Config.assistantId;
		}else if (userId.contains(",")){
			throw new RuntimeException("setCommand - MULTIPLE userIDs are not (yet) supported!");
		}

		String source = (filters.containsKey("source"))? (String) filters.get("source") : "assistAPI";
		
		String publicStr = (filters.containsKey("public"))? (String) filters.get("public") : "yes";
		boolean isPublic = !publicStr.equals("no");
		String localStr = (filters.containsKey("local"))? (String) filters.get("local") : "no";
		boolean isLocal = localStr.equals("yes");
		String explicitStr = (filters.containsKey("explicit"))? (String) filters.get("explicit") : "no";
		boolean isExplicit = explicitStr.equals("yes");

		JSONObject params = null;
		if (filters.containsKey("params")){
			params = JSON.parseString((String) filters.get("params"));
		}
		String cmdSummary = (String) filters.get("cmd_summary");
		if ((cmdSummary == null || cmdSummary.isEmpty()) && (params != null && !params.isEmpty())){
			cmdSummary = Converters.makeCommandSummary(command, params);
		}
		String environment = (filters.containsKey("environment"))? (String) filters.get("environment") : "all";
		String userLocation = (String) filters.get("user_location");
		String[] repliesArr = (String[]) filters.get("reply");
		List<String> replies = repliesArr == null ? new ArrayList<>() : Arrays.asList(repliesArr);
		JSONObject dataJson = null;				//e.g.: custom button data
		if (filters.containsKey("data")){
			dataJson = JSON.parseString((String) filters.get("data"));
		}else{
			dataJson = new JSONObject();	 	//NOTE: If no data is submitted it will kill all previous data info (anyway the whole object is overwritten)
		}
		
		//build sentence
		List<Command.Sentence> sentenceList = new ArrayList<>();
		Command.Sentence sentenceObj = new SentenceBuilder(sentence, userId, source)
				.setLanguage(Language.valueOf(language.toUpperCase()))
				.setParams(params)
				.setCmdSummary(cmdSummary)
				.setTaggedText(taggedSentence)
				.setPublic(isPublic)
				.setLocal(isLocal)
				.setExplicit(isExplicit)
				.setEnvironment(environment)
				.setUserLocation(userLocation)
				.setData(dataJson)
				//TODO: keep it or remove it? The general answers should be stored in an index called "answers"
				//and the connector is the command. For chats, custom answers are inside parameter "reply". But I think its still useful here ...
				.setReplies(new ArrayList<>(replies))
				.build();
		sentenceList.add(sentenceObj);

		//build command
		Command cmd = new Command(command);
		cmd.add(sentenceList);
		//System.out.println(cmd.toJson()); 		//debug
		
		//submit to DB
		int code = -1;
		//get ID if sentence exists
		if (overwriteExisting){
			//search existing:
			String itemId = "";
			if (taggedSentence != null && !taggedSentence.isEmpty()){
				itemId = getIdOfCommand(userId, language, taggedSentence);
			}else if (!sentence.isEmpty()){
				itemId = getIdOfCommand(userId, language, sentence);
			}
			if (itemId == null || itemId.isEmpty()){
				//not found
				code = JSON.getIntegerOrDefault(knowledge.setAnyItemData(COMMANDS, Command.COMMANDS_TYPE, cmd.toJson()), "code", -1);
			}else{
				//overwrite
				code = knowledge.setItemData(COMMANDS, Command.COMMANDS_TYPE, itemId, cmd.toJson());
			}
		}else{
			code = JSON.getIntegerOrDefault(knowledge.setAnyItemData(COMMANDS, Command.COMMANDS_TYPE, cmd.toJson()), "code", -1);
		}
		
		//statistics b
		Statistics.addOtherApiHit("setCommandInDB");
      	Statistics.addOtherApiTime("setCommandsInDB", tic);
      	
      	return code;
	}
	/**
	 * Delete a command with given id. You can use getIdOfCommand... to find the ID.
	 * @return true/false, false means either id problem or communication error
	 */
	public static boolean deleteCommand(String id){
		//TODO: note that this is UNSAFE because the userId check is missing and currently NOT USED! (See 'deleteSdkCommands' and 'deleteByJson')
		long tic = Debugger.tic();
		
		if (id == null || id.isEmpty()){
			return false;
		}
		int code = knowledge.deleteItem(COMMANDS, Command.COMMANDS_TYPE, id);
		
		//statistics
		Statistics.addOtherApiHit("deleteCommandFromDB");
      	Statistics.addOtherApiTime("deleteCommandFromDB", tic);
		
		return (code == 0);
	}
	/**
	 * Delete all commands (trigger sentences) connected to a SDK command and previously added by SDK-service upload. 
	 * @param userId - id of developer who uploaded the service
	 * @param command - e.g. uid1010.demo
	 * @param filters - optional filters (currently not used)
	 * @return number of deleted commands, -1 means error
	 */
	public static long deleteSdkCommands(String userId, String command, Map<String, Object> filters){
		long tic = Debugger.tic();
		
		if (userId == null || userId.isEmpty()){
			throw new RuntimeException("deleteSdkCommands - 'userId' is MISSING!");
		}
		if (command == null || command.isEmpty()){
			throw new RuntimeException("deleteSdkCommands - 'command' is MISSING!");
		}
		
		List<QueryElement> matches = new ArrayList<>(); 
		matches.add(new QueryElement("sentences.source", "SDK"));
		matches.add(new QueryElement("sentences.source", command));
		matches.add(new QueryElement("sentences.user", userId));
		String query = EsQueryBuilder.getNestedBoolMustMatch("sentences", matches).toJSONString();
		
		JSONObject data = new JSONObject();
		data = knowledge.deleteByJson(COMMANDS + "/" + Command.COMMANDS_TYPE, query);

		long deletedObjects = -1;
		if (Connectors.httpSuccess(data)){
			Object o = data.get("deleted");
			if (o != null){
				deletedObjects = (long) o;
			}
		}
		
		//statistics
		Statistics.addOtherApiHit("deleteSdkCommandFromDB");
      	Statistics.addOtherApiTime("deleteSdkCommandFromDB", tic);
      	
		return deletedObjects;
	}
	
	//------- COMMANDS END --------
	
	//------- ANSWERS START -------
	
	/**
	 * Get answer by type, language and user(s). If you load answers for other users than "self" you have to make sure
	 * that the calling function is allowed to access them (e.g. do a role-check for superuser).
	 * @param answerType - type of answer, e.g. "chat_hello_0a"
	 * @param languageOrNull - language code
	 * @param usersOrSelf - one or more users (string separated by comma, e.g. uid01,uid02,...). 
	 * @return JSON with "entries" (field can be empty array) or null on error
	 */
	public static JSONObject getAnswersByType(String answerType, String languageOrNull, String usersOrSelf) {
		//multiple users?
		String[] users = usersOrSelf.split(","); 		//assume that user IDs can never have commas as characters!
		
		//must match
		List<QueryElement> mustMatches = new ArrayList<>(); 
		mustMatches.add(new QueryElement("type", answerType.toLowerCase()));
		if (languageOrNull != null) {
			mustMatches.add(new QueryElement("language", languageOrNull.toLowerCase()));
		}
		//should match (at least one)
		List<QueryElement> shouldMatches = new ArrayList<>(); 
		for (String u : users){
			shouldMatches.add(new QueryElement("user", u.trim().toLowerCase()));
		}
		//add size
		JSONObject queryJson = EsQueryBuilder.getBoolMustAndShoudMatch(mustMatches, shouldMatches);
		JSON.put(queryJson, "size", 10000);  // let's read the maximum possible
		//System.out.println("JSON QUERY: " + queryJson.toString()); 		//debug
		
		JSONObject result = knowledge.searchByJson(ANSWERS + "/" + Answer.ANSWERS_TYPE, queryJson.toString());
		if (result.containsKey("error")){
			Debugger.println("getAnswersByType - error: " + result.get("error"), 1);
			return null;
		}
		JSONObject o = new JSONObject();
		JSONObject hits = (JSONObject) result.get("hits");
		if (hits != null){
			JSONArray innerHits = (JSONArray) hits.get("hits");
			JSON.put(o, "entries", innerHits);
		}else{
			JSON.put(o, "entries", new JSONArray());
		}
		return o;
	}
	
	//-------- ANSWERS END --------
	
	//------- ADDRESSES START --------
	
	/**
	 * Get addresses of user that fit to one or more tags.
	 * @param userId - ID as usual
	 * @param tags - 'specialTag' given by user to this address (e.g. user_home)
	 * @param filters - additional filters tbd
	 * @return list with addresses (can be empty), null for connection-error or throws an error
	 */
	public static List<Address> getAddressesByTag(String userId, List<String> tags, Map<String, Object> filters){
		long tic = Debugger.tic();
		//TODO: add version with specialName instead of tag
		
		//validate input
		if (userId == null || userId.isEmpty() || tags == null || tags.isEmpty()){
			throw new RuntimeException("getAddressesByTag - userId or tags invalid!");
		}
		if (userId.contains(",")){
			throw new RuntimeException("getListData - MULTIPLE userIds are not (yet) supported!");
		}
		
		//build query
		List<QueryElement> matches = new ArrayList<>(); 
		matches.add(new QueryElement("user", userId));
		List<QueryElement> oneOf = new ArrayList<>();
		for (String t : tags){
			oneOf.add(new QueryElement("specialTag", t));
		}
		JSONObject query = EsQueryBuilder.getBoolMustAndShoudMatch(matches, oneOf);
		//System.out.println("query: " + query);
		
		//call
		JSONObject data = new JSONObject();
		data = knowledge.searchByJson(USERDATA + "/" + Address.ADDRESSES_TYPE, query.toJSONString());
		
		List<Address> addresses = null;
		if (Connectors.httpSuccess(data)){
			JSONArray addressesArray = JSON.getJArray(data, new String[]{"hits", "hits"});
			addresses = new ArrayList<>();
			if (addressesArray != null){
				for (Object o : addressesArray){
					JSONObject jo = (JSONObject) o;
					if (jo.containsKey("_source")){
						JSONObject adrJson = (JSONObject) jo.get("_source");
						Address adr = new Address(Converters.json2HashMap(adrJson)); 	//we trust that this works ^^
						//get some additional info
						adr.user = JSON.getString(adrJson, "user");
						adr.userSpecialTag = JSON.getString(adrJson, "specialTag");
						adr.userSpecialName = JSON.getString(adrJson, "specialName");
						adr.dbId = JSON.getString(jo, "_id");
						//add
						addresses.add(adr);
					}
				}
			}
		}
		
		//statistics
		Statistics.addOtherApiHit("getAddressesByTagFromDB");
		Statistics.addOtherApiTime("getAddressesByTagFromDB", tic);
		      	
		return addresses;
	}
	/**
	 * Delete one or all addresses of the user with certain conditions.
	 * @return null if there was an error, else the number of deleted items
	 */
	public static long deleteAddress(String userId, String docId, Map<String, Object> filters){
		long tic = Debugger.tic();
		//TODO: support delete by tag?
		if (userId.isEmpty()){
			throw new RuntimeException("deleteAddress - userId missing or invalid!");
		}
		if (userId.contains(",")){
			throw new RuntimeException("deleteAddress - MULTIPLE userIds are not (yet) supported!");
		}
		if (docId == null || docId.isEmpty()){
			throw new RuntimeException("deleteAddress - document id is missing!");
		}

		List<QueryElement> matches = new ArrayList<>(); 
		matches.add(new QueryElement("user", userId));
		matches.add(new QueryElement("_id", docId));
		String query = EsQueryBuilder.getBoolMustMatch(matches).toJSONString();
		//System.out.println("query: " + query);
		
		JSONObject data = new JSONObject();
		data = knowledge.deleteByJson(USERDATA + "/" + Address.ADDRESSES_TYPE, query);

		long deletedObjects = -1;
		if (Connectors.httpSuccess(data)){
			Object o = data.get("deleted");
			if (o != null){
				deletedObjects = (long) o;
			}
		}
		
		//statistics
		Statistics.addOtherApiHit("deleteAddressFromDB");
		Statistics.addOtherApiTime("deleteAddressFromDB", tic);
		      	
		return deletedObjects;
	}
	/**
	 * Set a user specific address by either overwriting the doc at 'docId' or creating a new one.<br>
	 * NOTE: since this is an update call you should make sure that you set ALL required fields properly.<br> 
	 * When you change an address completely set some fields to empty strings if necessary or delete the entry first!
	 * @return JSONObject with "code" and optionally "_id" if the doc was newly created
	 */
	public static JSONObject setAddressWithTagAndName(String docId, String userId, String tag, String name, JSONObject adr){
		long tic = Debugger.tic();
		if (userId.isEmpty() || (tag.isEmpty() && name.isEmpty())){
			throw new RuntimeException("setAddressWithTagAndName - required 'userId' and one of 'specialTag' or 'specialName'!");
		}
		//safety overwrite
		JSON.put(adr, "user", userId);
		JSON.put(adr, "specialTag", tag);
		JSON.put(adr, "specialName", name);
		
		//NOTE: since this is an update call you should make sure that you set ALL required fields properly. 
		//When you change an address completely set some fields to empty string if necessary or delete the entry first!
				
		//simply write when no docId is given
		JSONObject setResult;
		if (docId == null || docId.isEmpty()){
			JSON.put(adr, "lastEdit", System.currentTimeMillis());
			setResult = knowledge.setAnyItemData(DB.USERDATA, Address.ADDRESSES_TYPE, adr);
		
		}else{
			adr.remove("_id"); //prevent to have id twice, just in case ...
			JSON.put(adr, "lastEdit", System.currentTimeMillis());
			JSONObject newAdrData = new JSONObject();
			//double-check if someone tampered with the docID by checking userID via script
			String dataAssign = "";
			for(Object keyObj : adr.keySet()){
				String key = (String) keyObj;
				dataAssign += ("ctx._source." + key + "=params." + key + "; ");
			}
			JSONObject script = JSON.make("lang", "painless",
					"inline", "if (ctx._source.user != params.user) { ctx.op = 'noop'} " + dataAssign.trim(),
					"params", adr);
			JSON.put(newAdrData, "script", script);//"ctx.op = ctx._source.user == " + userId + "? 'update' : 'none'");
			JSON.put(newAdrData, "scripted_upsert", true);
			
			int code = knowledge.updateItemData(DB.USERDATA, Address.ADDRESSES_TYPE, docId, newAdrData);
			setResult = JSON.make("code", code);
		}
		
		//statistics
		Statistics.addOtherApiHit("setAddressWithTagAndNameInDB");
		Statistics.addOtherApiTime("setAddressWithTagAndNameInDB", tic);
				
		return setResult;
	}
	
	//-------- ADDRESSES END ---------
	
	//------- LISTS START --------
	
	/**
	 * Get one or all lists of the user with a certain type and optionally title.
	 * @return null if there was an error, else a list (that can be empty)
	 */
	public static List<UserDataList> getListData(String userId, Section section, String indexType, Map<String, Object> filters){
		long tic = Debugger.tic();
		//validate
		if (indexType != null && !indexType.isEmpty() && indexType.equals(IndexType.unknown.name())){
			//TODO: think about that again
			indexType = "";
		}
		String title = (filters.containsKey("title"))? (String) filters.get("title") : "";
		if (userId.isEmpty() || (indexType.isEmpty() && title.isEmpty())){
			throw new RuntimeException("getListData - userId or (indexType and title) invalid!");
		}
		if (userId.contains(",")){
			throw new RuntimeException("getListData - MULTIPLE userIds are not (yet) supported!");
		}
		if (section == null || section.name().isEmpty()){
			throw new RuntimeException("getListData - section is missing!");
		}
		String sectionName = section.name();
		
		//results pagination?
		int resultsFrom = (filters.containsKey("resultsFrom"))? (int) filters.get("resultsFrom") : 0;
		int resultsSize = (filters.containsKey("resultsSize"))? (int) filters.get("resultsSize") : 10;

		List<QueryElement> matches = new ArrayList<>(); 
		matches.add(new QueryElement("user", userId));
		if (!sectionName.equals("all")) matches.add(new QueryElement("section", sectionName));
		if (!indexType.isEmpty()){
			matches.add(new QueryElement("indexType", indexType));
		}
		if (!title.isEmpty()){
			matches.add(new QueryElement("title", filters.get("title"), ""));
		}
		JSONObject queryJson = EsQueryBuilder.getBoolMustMatch(matches);
		JSON.put(queryJson, "from", resultsFrom);
		JSON.put(queryJson, "size", resultsSize);
		//System.out.println("query: " + queryJson.toJSONString());
		
		JSONObject data = new JSONObject();
		data = knowledge.searchByJson(USERDATA + "/" + UserDataList.LISTS_TYPE, queryJson.toJSONString());
		
		List<UserDataList> lists = null;
		if (Connectors.httpSuccess(data)){
			JSONArray listsArray = JSON.getJArray(data, new String[]{"hits", "hits"});
			lists = new ArrayList<>();
			if (listsArray != null){
				for (Object o : listsArray){
					JSONObject jo = (JSONObject) o;
					if (jo.containsKey("_source")){
						lists.add(new UserDataList((JSONObject) jo.get("_source"), (String) jo.get("_id")));
					}
				}
			}
		}
		
		//statistics
		Statistics.addOtherApiHit("getListDataFromDB");
		Statistics.addOtherApiTime("getListDataFromDB", tic);
		      	
		return lists;
	}
	/**
	 * Delete one or all lists of the user with certain conditions.
	 * @return null if there was an error, else the number of deleted items
	 */
	public static long deleteListData(String userId, String docId, Map<String, Object> filters){
		long tic = Debugger.tic();
		//TODO: support delete by index and title?
		//String title = (filters.containsKey("title"))? (String) filters.get("title") : "";
		//String indexType = (filters.containsKey("indexType"))? (String) filters.get("indexType") : "";
		if (userId.isEmpty()){
			throw new RuntimeException("deleteListData - userId missing or invalid!");
		}
		if (userId.contains(",")){
			throw new RuntimeException("deleteListData - MULTIPLE userIds are not (yet) supported!");
		}
		if (docId == null || docId.isEmpty()){
			throw new RuntimeException("deleteListData - document id is missing!");
		}

		List<QueryElement> matches = new ArrayList<>(); 
		matches.add(new QueryElement("user", userId));
		matches.add(new QueryElement("_id", docId));
		String query = EsQueryBuilder.getBoolMustMatch(matches).toJSONString();
		//System.out.println("query: " + query);
		
		JSONObject data = new JSONObject();
		data = knowledge.deleteByJson(USERDATA + "/" + UserDataList.LISTS_TYPE, query);

		long deletedObjects = -1;
		if (Connectors.httpSuccess(data)){
			Object o = data.get("deleted");
			if (o != null){
				deletedObjects = (long) o;
			}
		}
		
		//statistics
		Statistics.addOtherApiHit("deleteListDataFromDB");
		Statistics.addOtherApiTime("deleteListDataFromDB", tic);
		      	
		return deletedObjects;
	}
	/**
	 * Set a user data list by either overwriting the doc at 'docId' or creating a new one.
	 * @return JSONObject with "code" and optionally "_id" if the doc was newly created
	 */
	public static JSONObject setListData(String docId, String userId, Section section, String indexType, JSONObject listData){
		long tic = Debugger.tic();
		if (userId.isEmpty() || indexType.isEmpty()){
			throw new RuntimeException("setListData - 'userId' or 'indexType' invalid!");
		}
		//safety overwrite
		JSON.put(listData, "user", userId);
		JSON.put(listData, "section", section.name());
		JSON.put(listData, "indexType", indexType);
		
		//Note: if the 'title' is empty this might unintentionally overwrite a list or create a new one
		String title = (String) listData.get("title");
		if ((docId == null || docId.isEmpty()) && (title == null || title.isEmpty())){
			throw new RuntimeException("setUserDataList - 'title' AND 'id' is MISSING! Need at least one.");
		}
		if (section == null || section.name().isEmpty()){
			throw new RuntimeException("setUserDataList - 'section' is MISSING!");
		}
		
		//simply write when no docId is given
		JSONObject setResult;
		if (docId == null || docId.isEmpty()){
			JSON.put(listData, "lastEdit", System.currentTimeMillis());
			setResult = knowledge.setAnyItemData(DB.USERDATA, UserDataList.LISTS_TYPE, listData);
		
		}else{
			listData.remove("_id"); //prevent to have id twice
			JSON.put(listData, "lastEdit", System.currentTimeMillis());
			JSONObject newListData = new JSONObject();
			//double-check if someone tampered with the docID by checking userID via script
			String dataAssign = "";
			for(Object keyObj : listData.keySet()){
				String key = (String) keyObj;
				dataAssign += ("ctx._source." + key + "=params." + key + "; ");
			}
			JSONObject script = JSON.make("lang", "painless",
					"inline", "if (ctx._source.user != params.user) { ctx.op = 'noop'} " + dataAssign.trim(),
					"params", listData);
			JSON.put(newListData, "script", script);//"ctx.op = ctx._source.user == " + userId + "? 'update' : 'none'");
			JSON.put(newListData, "scripted_upsert", true);
			
			int code = knowledge.updateItemData(DB.USERDATA, UserDataList.LISTS_TYPE, docId, newListData);
			setResult = JSON.make("code", code);
		}
		
		//statistics
		Statistics.addOtherApiHit("setListDataInDB");
		Statistics.addOtherApiTime("setListDataInDB", tic);
				
		return setResult;
	}
	
	//------- LISTS END --------
	
	//------- WHITELISTS START --------
	
	/**
	 * Add a user to the white-list.
	 */
	public static int saveWhitelistUserEmail(String email){
		//System.out.println("save whitelist user attempt - email: " + email); 		//debug
		if (email == null || email.isEmpty()){
			return -1;
		}
		
		JSONObject data = new JSONObject();
		JSON.add(data, "uid", ID.clean(email));
		JSON.add(data, "info", "-");
		
		int code = JSON.getIntegerOrDefault(knowledge.setAnyItemData(WHITELIST, "users", data), "code", -1);
		//System.out.println("save whitelist user result - code: " + code); 		//debug
		return code;
	}
	/**
	 * Search a user on the white-list.
	 */
	public static boolean searchWhitelistUserEmail(String email){
		if (email == null || email.isEmpty()){
			return false;
		}
		JSONObject data = knowledge.searchSimple(WHITELIST + "/" + "users", "uid:" + ID.clean(email));
		//System.out.println("whitelist user search: " + data.toJSONString());
		try{
			int hits = Converters.obj2IntOrDefault(((JSONObject) data.get("hits")).get("total"), -1);
			if (hits > 0){
				return true;
			}else{
				return false;
			}
		}catch (Exception e){
			Debugger.println("Whitelist search failed! ID: " + email + " - error: " + e.getMessage(), 1);
			return false;
		}
	}
	
	//------ WHITELISTS END ------
	
	
	//------ Asynchronous methods ------
	
	//TODO: rewrite the asynchronous write methods to collect data and write all at the same time as batchWrite when finished collecting
	
	/**
	 * Save stuff to database without waiting for reply, making this save method UNSAVE so keep that in mind when using it.
	 * Errors get written to log.
	 * @param index - index or table name like e.g. "account" or "knowledge"
	 * @param type - subclass name, e.g. "user", "lists", "banking" (for account) or "geodata" and "dictionary" (for knowledge)
	 * @param item_id - unique item/id name, e.g. user email address, dictionary word or geodata location name
	 * @param data - JSON string with data objects that should be stored for index/type/item, e.g. {"name":"john"}
	 */
	public static void saveKnowledgeAsync(String index, String type, String item_id, JSONObject data){
		Thread thread = new Thread(){
		    public void run(){
		    	//time
		    	long tic = Debugger.tic();
		    	
		    	int code = knowledge.setItemData(index, type, item_id, data);
				if (code != 0){
					Debugger.println("KNOWLEDGE DB ERROR! - PATH: " + index + "/" + type + "/" + item_id + " - TIME: " + System.currentTimeMillis(), 1);
				}else{
					//Debugger.println("KNOWLEDGE DB UPDATED! - PATH: " + index + "/" + type + "/" + item_id + " - TIME: " + System.currentTimeMillis(), 1);
					Statistics.add_KDB_write_hit();
					Statistics.save_KDB_write_total_time(tic);
				}
		    }
		};
		thread.start();
	}
	/**
	 * Save stuff to database without waiting for reply, making this save method UNSAVE so keep that in mind when using it.
	 * Errors get written to log. This method does not require an ID, it is auto-generated.
	 * @param index - index or table name like e.g. "account" or "knowledge"
	 * @param type - subclass name, e.g. "user", "lists", "banking" (for account) or "geodata" and "dictionary" (for knowledge)
	 * @param data - JSON string with data objects that should be stored for index/type/item, e.g. {"name":"john"}
	 */
	public static void saveKnowledgeAsyncAnyID(String index, String type, JSONObject data){
		Thread thread = new Thread(){
		    public void run(){
		    	//time
		    	long tic = Debugger.tic();
		    	
		    	int code = JSON.getIntegerOrDefault(knowledge.setAnyItemData(index, type, data), "code", -1);
				if (code != 0){
					Debugger.println("KNOWLEDGE DB ERROR! - PATH: " + index + "/" + type + "/[rnd] - TIME: " + System.currentTimeMillis(), 1);
				}else{
					//Debugger.println("KNOWLEDGE DB UPDATED! - PATH: " + index + "/" + type + "/[rnd] - TIME: " + System.currentTimeMillis(), 1);
					Statistics.add_KDB_write_hit();
					Statistics.save_KDB_write_total_time(tic);
				}
		    }
		};
		thread.start();
	}
		
	//--------------Tools----------------
	
	/**
	 * Create super user for account access.
	 * @param account_id - account id to access
	 * @return
	 */
	private static User createSuperuser(String account_id){
		//super token
		final class SuperToken extends Authenticator{
			private String userID = "";
			public SuperToken(String user_id){
				userID = user_id;
			}
			public boolean authenticated(){
				return true;
			}
			public String getUserID(){
				return userID;
			}
		}
		//super user
		User user = new User(null, new SuperToken(account_id));
		return user;
	}
	
	//-- elastic search helpers:
	
	//JSON string writer helpers for ElasticSearch queries
	//- nested sentences:
	private static void startNestedQuery(JsonGenerator g, int from) throws IOException {
		g.writeStartObject();
		g.writeNumberField("from", from);
		g.writeObjectFieldStart("query");
		g.writeObjectFieldStart("nested");
		g.writeStringField("path", "sentences");
		g.writeObjectFieldStart("query");
		g.writeObjectFieldStart("bool");
	}
	private static void endNestedQuery(JsonGenerator g) throws IOException {
		g.writeEndArray();
		g.writeEndObject();
		g.writeEndObject();
		g.writeEndObject();
		g.writeEndObject();
	}
}
