package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.DynamoDB;
import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.assist.database.GUID;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Security;
import net.b07z.sepia.server.core.users.AuthenticationInterface;
import spark.Request;

/**
 * User authentication implemented with AWS DynamoDB.
 * 
 * @author Florian Quirin
 *
 */
public class AuthenticationDynamoDB implements AuthenticationInterface{
	
	private static final String tableName = DB.USERS;					//table of the users
	private static final String ticketsTable = DB.TICKETS;				//table of tickets (used here to temporary store reg. tokens)
	private static final long access_lvl_token_valid_time = 1800000L;		//that token is valid 30min
	private static final long registration_token_valid_time = 86400000L;	//that token is valid 24h
	private static final long reset_token_valid_time = 1200000L;		//that token is valid 20min
	private static final long key_token_valid_time = 86400000L;			//that token is valid for one day
	private static final long app_token_valid_time = 3153600000L;		//that token is valid for one year
	
	private static final String TOKENS_SUPP = "tokens_supp";
	private static final String TOKENS_REG = "tokens_reg";
	private static final String TOKENS_SUPP_TS = "tokens_supp_ts";		//keep this name
	private static final String TOKENS_REG_TS = "tokens_reg_ts";		//keep this name
	
	//temporary secrets
	//private static String temporaryTokenSalt = Security.getRandomUUID().replaceAll("-", "").trim();
	
	Request metaInfo;		//reserved for request meta info like request headers etc. 
	
	//authentication constants
	private String userID = "-1";
	private int accessLvl = -1;
	private int errorCode = 0;
	private HashMap<String, Object> basicInfo;
	
	@Override
	public void setRequestInfo(Object request) {
		this.metaInfo = (Request) request;	
	}
		
	@Override
	public boolean checkDatabaseConnection(){
		try{
			//DynamoDB
			JSONObject tables = DynamoDB.listTables();
			JSONArray tablesArray = (JSONArray) tables.get("TableNames");
			boolean dynamoDbOk = (tablesArray.contains(DB.USERS) && tablesArray.contains(DB.TICKETS));
			//We also need Elasticsearch for tickets, whitelist and user-data
			Elasticsearch es = new Elasticsearch();
			JSONObject mappings = es.getMappings();
			boolean elasticOk = (mappings.containsKey(DB.WHITELIST) 
					&& mappings.containsKey(GUID.INDEX) && mappings.containsKey(DB.USERDATA));
			//System.out.println(tablesArray);
			if (!dynamoDbOk){
				Debugger.println("checkDatabaseConnection() - DynamoDB indicies are incomplete or not valid!", 1);
				return false;
			}else if (!elasticOk){
				Debugger.println("checkDatabaseConnection() - Elasticsearch mappings are incomplete or not valid!", 1);
				return false;
			}else{
				return true;
			}
			
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	//user exists?
	public String userExists(String identifier, String idType) throws RuntimeException{
		
		if (idType.matches(ID.Type.uid + "|" + ID.Type.email + "|" + ID.Type.phone)){
			//all primary user IDs need to be lowerCase in DB!!!
			identifier = ID.clean(identifier);
			
			//search parameters:
			JSONObject response = read_basics(identifier, idType, new String[]{ACCOUNT.GUUID, ACCOUNT.EMAIL, ACCOUNT.PHONE});
			//System.out.println("RESPONSE: " + response.toJSONString());				//debug
			
			//Status?
			try {
				JSONObject item;
				if (response.containsKey("Items")){
					JSONArray ja = (JSONArray) response.get("Items");
					if (ja.isEmpty()){
						return "";
					}
					item = JSON.getJObject(ja, 0);
				}else{
					item = (JSONObject) response.get("Item");
					if (item == null || item.isEmpty()){
						return "";
					}
				}
				//does the result include the user ID?
				String guuid = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.GUUID));
				if (guuid == null || guuid.isEmpty()){
					return "";
				}
				String otherID;
				if (idType.equals(ID.Type.uid)){
					otherID = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.GUUID));
				}else if (idType.equals(ID.Type.email)){
					otherID = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.EMAIL));
				}else if (idType.equals(ID.Type.phone)){ 
					otherID = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.PHONE));
				}else{
					otherID = null;
				}
				if (otherID != null && otherID.equals(identifier)){
					//System.out.println("RESULT: " + true);								//debug
					return guuid;
				}else{
					//System.out.println("RESULT: " + false);								//debug
					return "";
				}
				
			}catch (Exception ex){
				throw new RuntimeException("Authentication.userExists(...) reports 'DB query failed! Result unclear!'", ex);
			}
		}else{
			throw new RuntimeException("userExists(...) reports 'unsupported ID type' " + idType);
		}
	}
	
	//generate registration info
	public JSONObject registrationByEmail(String email){
		//all primary user IDs need to be lowerCase in DB!!!
		email = ID.clean(email);
		
		//never request an account for superuser
		if (email.equals(Config.superuserEmail.toLowerCase())){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "request not possible, user already exists or email not allowed!");
			JSON.add(result, "code", "901");
			errorCode = 5;
			return result;
		}
		
		//type is fix here
		String type = ID.Type.email;
				
		//check if user exists
		//------------------------------------
		String guuid = userExists(email, type);
		if (!guuid.isEmpty()){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "request not possible, user already exists!");	//REMEMBER: keep "already exists" for client
			JSON.add(result, "code", "901");
			errorCode = 5;
			return result;
		}
		//------------------------------------
		
		//check if user is allowed to register - NOTE: this uses a different database (Elasticsearch)
		//------------------------------------
		if (Config.restrictRegistration){
			if (!DB.searchWhitelistUserEmail(email)){
				JSONObject result = new JSONObject();
				JSON.add(result, "result", "fail");
				JSON.add(result, "error", "request not possible, user is not allowed to create new account!"); //REMEMBER: keep "not allowed" for client
				JSON.add(result, "code", "902");
				errorCode = 5;
				return result;
			}
		}
		//------------------------------------
		
		long time = System.currentTimeMillis();
		String token, ticketId;
		try{
			token = getRandomSecureToken();
			ticketId = GUID.getTicketGUID();
			String storeToken = Security.hashClientPassword(email + token + time + ticketId);
			String[] keys = new String[]{TOKENS_REG, TOKENS_REG_TS};
			Object[] objects = new Object[]{storeToken, new Long(time)};
			if (!write_reg_token(ticketId, keys, objects)){
				throw new RuntimeException("registration token storing failed!");
			}
			
		}catch(Exception e){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "500 - token generation or storing failed! - msg: " + e.getMessage());
			errorCode = 4;
			return result;
		}
		//use this in Email:
		/*
		String url = Config.endpoint_url_createUser 
			+ "?userid=" + encodeURIComponent(userid)
			+ "&ticketid=" + encodeURIComponent(ticketid) 
			+ "&time=" + encodeURIComponent(timeStamp) 
			+ "&token=" + encodeURIComponent(token)
			+ "&type=" + encodeURIComponent(type);
		 */
		JSONObject result = new JSONObject();
		JSON.add(result, "result", "success");
		JSON.add(result, "token", token);
		JSON.add(result, "time", String.valueOf(time));
		JSON.add(result, "userid", email);
		JSON.add(result, "type", type);
		JSON.add(result, "ticketid", ticketId);
		//JSON.add(result, "url", url);
		errorCode = 0;
		return result;
	}

	//create a new user - fields userid, pwd, time, token
	public boolean createUser(JSONObject info) {
		//check type
		String type = (String) info.get("type");
		
		//---V1: create via email registration:
		if (type != null && type.equals(ID.Type.email)){
			String email = "";
			String ticketId = "";
			try{
				String token = (String) info.get("token");		
				String ts = (String) info.get("time");
				email = (String) info.get("userid");		
				ticketId = (String) info.get("ticketid");
				long time = Long.parseLong(ts);
				//System.out.println("createUser - token: " + token + ", timeStamp: " + time);		//debug
				
				//check if the token is still valid - first time, might still be fake but might also block a lot of real calls
				if ((System.currentTimeMillis() - time) > registration_token_valid_time){
					errorCode = 5;
					return false;
				}
			
				//all primary user IDs need to be lowerCase in DB!!!
				email = ID.clean(email);
	
				//check if token is as expected
				Object[] res = readSupportToken(ticketId, TOKENS_REG);
				String sendToken = Security.hashClientPassword(email + token + time + ticketId);
				String tokenTarget = (String) res[0];
				long timeTarget = (long) res[1];
				if (sendToken.length() > 15 && sendToken.equals(tokenTarget) && (timeTarget == time)){
					//continue
				}else{
					errorCode = 5; 
					return false;
				}
				
			}catch (Exception e){					
				errorCode = 5; 
				return false;	
			}
			
			//everything is fine, so create user
			String pwd = (String) info.get("pwd"); 
			
			//check if the password is valid
			if (pwd == null || pwd.length() < 8){ 		//NOTE: since the password should be hashed it will always be much longer than 8 chars
				errorCode = 6;
				return false;
			}
			
			//delete old token or make it invalid
			write_reg_token(ticketId, new String[]{TOKENS_REG_TS}, new Object[]{new Long(0)});
			
			//get a new ID and create a save storage for the password by standard PBKDF2 HMAC-SHA-256 algorithm
			String[] ids = {email, "-"};   	//additionally known ids due to type of login. Order fixed: email, phone, xy, ...
			return createUserAction(pwd, ids);
		
		//other ID types of creating user
		}else{
			throw new RuntimeException("createUser(...) reports 'unsupported registration type' " + type);
		}
	}
	/**
	 * This is the action that executes the createUser request after it has been checked.
	 */
	private boolean createUserAction(String pwd, String... ids){
		//get a new ID and create a save storage for the password by standard PBKDF2 HMAC-SHA-256 algorithm
		String guuid, salt;
		int iterations;
		try{
			ID.Generator gen = new ID.Generator(pwd);
			guuid = gen.guuid;
			iterations = gen.iterations;
			salt = gen.salt;
			pwd = gen.pwd;
			//System.out.println("createUser - userid: " + guuid + ", token: " + token + ", timeStamp: " + time);		//debug
		
		}catch (Exception e){
			Debugger.println(e.getMessage(), 1); 	//debug
			errorCode = 7;
			return false;
		}			
		
		//TODO: we have to check if this ID already exists (due to backup restore or something)
		String testId = userExists(guuid, ID.Type.uid);
		if (!testId.isEmpty()){
			Debugger.println("GUUID: ID '" + testId + "' already exists! Did you restore the database from a backup? You can adjust 'guid_offset' value in config.", 1); 	//debug
			errorCode = 7;
			return false;
		}
		
		//default keys 'n values
		//------------------- THIS DEFINES THE BASIC STRUCTURE OF THE USER ITEM --------------------
		//Dummies
		//String phone = "";
		HashMap<String, Object> dummy_map = new HashMap<String, Object>();		//empty dummy
		ArrayList<String> roles = new ArrayList<>(); 
			roles.add("user");													//default roles						
		HashMap<String, Object> dummy_name = new HashMap<String, Object>();		//name dummy
		dummy_name.put(Name.NICK, "Boss");
		HashMap<String, Object> dummy_lang = new HashMap<String, Object>();		//language dummy
		dummy_lang.put("lang_code", LANGUAGES.EN);
		HashMap<String, Object> dummy_adr = new HashMap<String, Object>();		//address dummy
		dummy_adr.put("uhome", new HashMap<String, Object>());
		//dummy_adr.put("uwork", new HashMap<String, Object>()); 	//removed from account and moved to user-data
		
		ArrayList<String> basicKeys = new ArrayList<>();
		ArrayList<Object> basicObjects = new ArrayList<>();
		//basicKeys.add(ACCOUNT.GUUID);				basicObjects.add(guuid);
		basicKeys.add(ACCOUNT.PASSWORD);			basicObjects.add(pwd);
		basicKeys.add(ACCOUNT.PWD_SALT);			basicObjects.add(salt);
		basicKeys.add(ACCOUNT.PWD_ITERATIONS);		basicObjects.add(iterations);
		basicKeys.add(ACCOUNT.TOKENS);				basicObjects.add(dummy_map);
		//basicKeys.add(ACCOUNT.APIS);				basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.STATISTICS);			basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.USER_NAME);			basicObjects.add(dummy_name);
		basicKeys.add(ACCOUNT.ROLES);				basicObjects.add(roles);
		//basicKeys.add(ACCOUNT.LISTS);				basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.ADDRESSES);			basicObjects.add(dummy_adr);
		basicKeys.add(ACCOUNT.INFOS);				basicObjects.add(dummy_lang);
		/*
		basicKeys.add(ACCOUNT.SOCIALS);				basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.CONTACTS);			basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.CALENDARS);			basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.BANKING);				basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.HEALTH);				basicObjects.add(dummy_map);
		basicKeys.add(ACCOUNT.INSURANCE);			basicObjects.add(dummy_map);
		*/
		//IDs
		basicKeys.add(ACCOUNT.EMAIL);				basicObjects.add(ids[0]); 		//ids[0] = email;
		basicKeys.add(ACCOUNT.PHONE);				basicObjects.add(ids[1]); 		//ids[1] = phone;
		//collect
		String[] keys = basicKeys.toArray(new String[1]);
		Object[] objects = basicObjects.toArray(new Object[1]);
		//-------------------------------------------------------------------------------------------
		
		//write values and return true/false - error codes can be checked afterwards if necessary
		boolean success = write_protected(guuid, ID.Type.uid, keys, objects);
		
		//--- add some more user data entries in other database --
		if (success){
			int mapCode = -1;
			try{
				//user data services
				mapCode = DB.clearCommandMappings(guuid);
				
			}catch (Exception e){
				//can it even crash?
			}
			Debugger.println("Status for new user data - " + guuid + " - cmd-mappings-code: " + mapCode + "", 3);
		}
		//--------------------------------------------------------
		return success;
	}
	
	//request change of password
	public JSONObject requestPasswordChange(JSONObject info){
		//get parameters
		String userid = (String) info.get("userid");
		String type = (String) info.get("type");
		
		//check reset type
		if (type == null || !type.equals(ID.Type.email)){
			throw new RuntimeException("requestPasswordChange(...) reports 'unsupported reset type' " + type);
		}
		
		//---V1: Email reset
		
		//all primary user IDs need to be lowerCase in DB!!!
		userid = ID.clean(userid);
		
		//check if user exists
		//------------------------------------
		String guuid = userExists(userid, type);
		if (guuid.isEmpty()){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "request not possible, user cannot be found!");
			errorCode = 5;
			return result;
		}
		//------------------------------------
		
		long time = System.currentTimeMillis();
		String token, ticketId;
		try{
			token = getRandomSecureToken();
			ticketId = GUID.getTicketGUID();
			String storeToken = Security.hashClientPassword(userid + token + time + ticketId);
			String[] keys = new String[]{TOKENS_SUPP, TOKENS_SUPP_TS};
			Object[] objects = new Object[]{storeToken, new Long(time)};
			if (!write_reg_token(ticketId, keys, objects)){
				throw new RuntimeException("requestPasswordChange token storing failed!");
			}
			
		}catch(Exception e){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "500 - token generation or storing failed!");
			errorCode = 4;
			return result;
		}

		JSONObject result = new JSONObject();
		JSON.add(result, "result", "success");
		JSON.add(result, "token", token);
		JSON.add(result, "time", String.valueOf(time));
		JSON.add(result, "userid", userid);
		JSON.add(result, "type", type);
		JSON.add(result, "ticketid", ticketId);
		//JSON.add(result, "url", url);
		errorCode = 0;
		return result;
	}
	
	//change the password - fields userid, type, new_pwd, time, token
	public boolean changePassword(JSONObject info) {
		//check type
		String type = (String) info.get("type");
		if (type == null || !type.equals(ID.Type.email)){
			throw new RuntimeException("changePassword(...) reports 'unsupported type' " + type);
		}
		
		//---V1: change via email confirmation:
		
		String email = "";
		String guuid = "";
		String ticketId = "";
		try{
			String token = (String) info.get("token");		
			String ts = (String) info.get("time");
			email = (String) info.get("userid");		
			ticketId = (String) info.get("ticketid");
			long time = Long.parseLong(ts);
			//System.out.println("createUser - token: " + token + ", timeStamp: " + time);		//debug
			
			//check if the token is still valid - first time, might still be fake but might also block a lot of real calls
			if ((System.currentTimeMillis() - time) > reset_token_valid_time){
				errorCode = 5;
				return false;
			}
		
			//all primary user IDs need to be lowerCase in DB!!!
			email = ID.clean(email);
			
			//never request a new password for the superuser or assistant itself
			if (email.equals(Config.superuserEmail.toLowerCase()) || email.equals(Config.assistantEmail)){
				errorCode = 5;
				return false;
			}
			
			//check if user exists ... again just to be sure and to get GUUID.
			//------------------------------------
			guuid = userExists(email, type);
			if (guuid.isEmpty()){
				errorCode = 2;
				return false;
			}
			//------------------------------------

			//check if token is as expected
			Object[] res = readSupportToken(ticketId, TOKENS_SUPP);
			String sendToken = Security.hashClientPassword(email + token + time + ticketId);
			String tokenTarget = (String) res[0];
			long timeTarget = (long) res[1];
			if (sendToken.length() > 15 && sendToken.equals(tokenTarget) && (timeTarget == time)){
				//continue
			}else{
				errorCode = 5; 
				return false;
			}
			
		}catch (Exception e){					
			errorCode = 5; 
			return false;	
		}
		
		//everything is fine, so create user
		String pwd = (String) info.get("new_pwd"); 
		
		//check if the password is valid
		if (pwd == null || pwd.length() < 8){
			errorCode = 6;
			return false;
		}
		
		//get a new ID and create a save storage for the new password
		String salt;
		int iterations;
		try{
			ID.Generator gen = new ID.Generator(pwd);
			iterations = gen.iterations;
			salt = gen.salt;
			pwd = gen.pwd;
			//System.out.println("createUser - userid: " + guuid + ", token: " + token + ", timeStamp: " + time);		//debug
		
		}catch (Exception e){
			Debugger.println(e.getMessage(), 1); 	//debug
			errorCode = 7;
			return false;
		}
		
		//delete old token or make it invalid
		write_reg_token(ticketId, new String[]{TOKENS_SUPP_TS}, new Object[]{new Long(0)});
		
		//-------------------------------------------------------------------------------------------
		
		//change password, delete old tokens, write values 
		//and return true/false - error codes can be checked afterwards if necessary
		HashMap<String, Object> emptyTokenDummy = new HashMap<String, Object>();
		String[] keys = new String[]{
				ACCOUNT.PASSWORD, ACCOUNT.PWD_SALT, ACCOUNT.PWD_ITERATIONS, ACCOUNT.TOKENS
		};
		Object[] objects = new Object[]{
				pwd, salt, iterations, emptyTokenDummy
		};
		return write_protected(guuid, ID.Type.uid, keys, objects);
	}
	
	//delete user - fields userid
	public boolean deleteUser(JSONObject info) {
		//TODO: make it like "createUser" or is it enough to check credentials before? ...
		//TODO: clean up DB.USERDATA as well
		
		//get user ID
		String userid = (String) info.get("userid");
		if (userid == null || userid.isEmpty()){
			errorCode = 4;
			return false;
		}else{
			userid = ID.clean(userid);
		}
		
		//operation:
		String operation = "DeleteItem";
		
		//primaryKey:
		JSONObject prime = DynamoDB.getPrimaryUserKey(userid);
		
		//JSON request:
		JSONObject request = new JSONObject();
		JSON.add(request, "TableName", tableName);
		JSON.add(request, "Key", prime);
		JSON.add(request, "ReturnValues", "NONE");
		
		//System.out.println("REQUEST: " + request.toJSONString());		//debug
		
		//Connect
		JSONObject response = DynamoDB.request(operation, request.toJSONString());
		//System.out.println("RESPONSE: " + response.toJSONString());			//debug
		//System.out.println("Time needed: " + Debugger.toc(tic) + "ms");		//debug
		
		if (!Connectors.httpSuccess(response)){
			errorCode = 3;
			Debugger.println("deleteUser() - DynamoDB Response: " + response.toJSONString(), 1);			//debug
			return false;
		}else{
			//note: no stats recorded here
			errorCode = 0;
			return true;
		}	
	}

	//check it!
	public boolean authenticate(JSONObject info) {
		String username = (String) info.get("userId");
		String password = (String) info.get("pwd");
		String idType = (String) info.get("idType");
		String client = (String) info.get("client");
		String incToken = (String) info.get(ACCOUNT.ACCESS_LVL_TOKEN);
		int tokenType = -1;
		
		//use the DynamoDB access to read ---BASICS--- (you can add these basics to USER.checked to avoid double-loading!)
		username = ID.clean(username);
		
		//TODO: its a bit annoying to add more stuff here ...
		//when you add stuff here add it further down AND to the constructor in USER.java as well  (variable 'checked')!
		/*
			ACCOUNT.PASSWORD, ACCOUNT.TOKEN_KEY, ACCOUNT.TOKEN_KEY_TS, ACCOUNT.TOKEN_ACCESS_LVL, ACCOUNT.TOKEN_ACCESS_LVL_TS
		*/
		String[] basics = new String[]{
				ACCOUNT.GUUID, ACCOUNT.PASSWORD, ACCOUNT.PWD_SALT, ACCOUNT.PWD_ITERATIONS, ACCOUNT.TOKENS, 
				ACCOUNT.EMAIL, ACCOUNT.PHONE, ACCOUNT.ROLES,
				ACCOUNT.USER_NAME, ACCOUNT.USER_PREFERRED_LANGUAGE, ACCOUNT.USER_BIRTH,
				ACCOUNT.BOT_CHARACTER,
				ACCOUNT.USER_PREFERRED_UNIT_TEMP
			};
		JSONObject result = read_basics(username, idType, basics);
		//System.out.println("Auth. req: " + username + ", " + idType + ", " + password); 		//debug
		//System.out.println("Auth. res: " + result.toJSONString()); 		//debug
		//Status?
		if (!Connectors.httpSuccess(result)){
			errorCode = 3;
			return false;
		
		}else{
			JSONObject item;
			if (result.containsKey("Items")){
				JSONArray ja = (JSONArray) result.get("Items");
				if (ja.isEmpty()){
					errorCode = 2;
					return false;
				}
				item = JSON.getJObject(ja, 0);
			}else{
				item = (JSONObject) result.get("Item"); 
				if (item == null || item.isEmpty()){
					errorCode = 2;
					return false;
				}
			}
			//check password or key token
			String userIdTmp = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.GUUID)); 	//unconfirmed ID (before pwd check)
			boolean isAdminId = (userIdTmp != null && !userIdTmp.isEmpty() && userIdTmp.equals(Config.superuserId));
			String pwd;
			//token key
			if (password.length() == 65){
				tokenType = 0;
				String token = getAppTokenPath(client);
				String token_ts = token + "_ts";
				long valid_time = app_token_valid_time;
				if (isAdminId || CLIENTS.isRatherUnsafe(client)){
					valid_time = key_token_valid_time;		//admin and unsafe clients have short-lived token
				}
				JSONObject t = DynamoDB.dig(item, token);
				if (t != null){
					pwd = DynamoDB.typeConversion(t).toString();
					//check time stamp
					//long ts = (long)(double)(Account_DynamoDB.typeConversion(Account_DynamoDB.dig(item, ACCOUNT.TOKEN_KEY_TS)));
					JSONObject tts = DynamoDB.dig(item, token_ts);
					long ts = Converters.obj2LongOrDefault(DynamoDB.typeConversion(tts), 0l);
					if ((System.currentTimeMillis() - ts) > valid_time){
						//token became invalid
						pwd = null;
					}
				}else{
					pwd = null;
				}
			//original key - this is the login that generates the token later and should not be abused for client-authentication via real password
			}else{
				tokenType = 1;
				try {
					pwd = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.PASSWORD));
					String salt = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.PWD_SALT));
					int iterations = Converters.obj2IntOrDefault(DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.PWD_ITERATIONS)), -1);
					ID.Generator gen = new ID.Generator(password, salt, iterations);
					password = gen.pwd;
				} catch (Exception e) {
					Debugger.println("Authentication_DynamoDB.authenticate(...) - using original password failed due to: " + e.getMessage(), 1); 	//debug
					if (e.getStackTrace() != null && e.getStackTrace().length > 0){
						Debugger.println("Authentication_DynamoDB.authenticate(...) - error last trace: " + e.getStackTrace()[0], 1);
					}
					errorCode = 7;
					return false;
				}
			}
			if (pwd != null && !pwd.trim().isEmpty() && password != null && !password.trim().isEmpty()){
				//check
				if (!pwd.equals(password)){
					errorCode = 2;
					return false;
				
				}else{
					//authentication successful!
					userID = userIdTmp;
					if (tokenType == 0){
						accessLvl = 0;			//basic auth. with token does level 0
					}else if (tokenType == 1){
						accessLvl = 1;			//basic auth. with real password does level 1
					}else{
						accessLvl = -1;			//unknown type ???
					}
					errorCode = 0;			//no errors so far
					
					//check the access level increase token
					try{
						if (incToken != null){
							String storedIncToken = (String) DynamoDB.typeConversion(DynamoDB.dig(item, ACCOUNT.TOKENS + "." + ACCOUNT.ACCESS_LVL_TOKEN));
							if (storedIncToken !=null && !incToken.isEmpty() && !storedIncToken.isEmpty()){
								String newAccessLvl = storedIncToken.substring(0, 2);
								if ((newAccessLvl+incToken).equals(storedIncToken)){
									//check time stamp
									long storedIncToken_TS = Converters.obj2LongOrDefault(DynamoDB.typeConversion(DynamoDB.dig(item, ACCOUNT.TOKENS + "." + ACCOUNT.ACCESS_LVL_TOKEN_TS)), 0l);
									if ((System.currentTimeMillis()-storedIncToken_TS) < access_lvl_token_valid_time){
										accessLvl = Integer.parseInt(storedIncToken.substring(0, 2));
									}else{
										//System.out.println("Time stamp too old");
										//System.out.println(storedIncToken + " - " + storedIncToken_TS + " - " + sentIncToken + " - " + System.currentTimeMillis());
									}
								}
							}
						}	
					}catch (Exception e) {
						e.printStackTrace();	//no increase because token is bad
					}
					
					//---------now get basic info too----------
					basicInfo = new HashMap<String, Object>();
					
					//GUUID, EMAIL and PHONE
					basicInfo.put(Authenticator.GUUID, userID);
					String email = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.EMAIL));
					String phone = (String) DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.PHONE));
					if (email != null && !email.equals("-")){
						basicInfo.put(Authenticator.EMAIL, email);
					}
					if (phone != null && !phone.equals("-")){
						basicInfo.put(Authenticator.PHONE, phone);
					}
					
					//NAME
					Object found = DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.USER_NAME));
					if (found != null){
						basicInfo.put(Authenticator.USER_NAME, Converters.object2HashMapStrObj(found)); 		//this should work ^^
					}
					//ROLES
					Object found_roles = DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.ROLES));
					if (found_roles != null){
						List<String> all_roles = Converters.object2ArrayListStr(found_roles);
						basicInfo.put(Authenticator.USER_ROLES, all_roles);
					}
					//INFOS
					Object found_infos = DynamoDB.typeConversion((JSONObject) item.get(ACCOUNT.INFOS));
					if (found_infos != null){
						Map<String, Object> infos = Converters.object2HashMapStrObj(found_infos);
						if (infos != null){
							basicInfo.put(Authenticator.USER_BIRTH, infos.get("birth"));
							basicInfo.put(Authenticator.USER_LANGUAGE, infos.get("lang_code"));
							basicInfo.put(Authenticator.BOT_CHARACTER, infos.get("bot_char"));
							basicInfo.put(Authenticator.USER_PREFERRED_UNIT_TEMP, infos.get("unit_pref_temp"));
						}
					}
					
					//-----------------------------------------
					
					return true;
				}
				
			}else{
				errorCode = 2;
				return false;
			}
		}
	}
	
	//create and return key token
	public String writeKeyToken(String userid, String client) {
		//create a random string
		String userToken;
		try {
			userToken = getRandomSecureToken();
			
		} catch (Exception e) {
			Debugger.println("writeKeyToken(..) - failed to create secure token! Id: " + userid + ", client: " + client, 1);
			e.printStackTrace();
			errorCode = 3;
			return "";
		}
		//write server token
		String tokenPath = getAppTokenPath(client);
		boolean success = writeLoginToken(userid, ID.Type.uid, userToken, tokenPath);
		if (success && !userToken.isEmpty()){
			errorCode = 0;
			return userToken;
		}else{
			errorCode = 3;
			return "";
		}
	}

	//logout user and make token invalid 
	public boolean logout(String userid, String client) {
		//delete key token
		String token = getAppTokenPath(client);
		String[] keys = new String[]{token};
		Object[] objects = new Object[]{"-"};
		//String[] keys = new String[]{ACCOUNT.TOKEN_KEY, ACCOUNT.TOKEN_KEY_TS};
		//Object[] objects = new Object[]{"-", new Long(0)};
		boolean success = write_protected(userid, ID.Type.uid, keys, objects); 	//logout should be called with GUUID type
		if (success){
			errorCode = 0;
			return true;
		}else{
			errorCode = 3;
			return false;
		}
	}
	//logout all clients - TODO: untested
	public boolean logoutAllClients(String userid) {
		//delete all key tokens
		HashMap<String, Object> emptyTokenDummy = new HashMap<String, Object>();
		String[] keys = new String[]{ ACCOUNT.TOKENS };
		Object[] objects = new Object[]{ emptyTokenDummy };
		return write_protected(userid, ID.Type.uid, keys, objects);
	}
	
	//return ID
	public String getUserID() {
		return userID;
	}

	//return access level
	public int getAccessLevel() {
		return accessLvl;
	}
	
	//get basic info
	public HashMap<String, Object> getBasicInfo() {
		return basicInfo;
	}

	//return error code
	public int getErrorCode() {
		return errorCode;
	}
	
	/**
	 * Get the proper path to the token used by this client (based on client_info).
	 * @param client - client_info as sent by user
	 * @return
	 */
	private String getAppTokenPath(String client){
		if (client == null || client.isEmpty()){
			client = Config.defaultClientInfo;
		}
		return CLIENTS.getBaseClient(client);
	}
	
	//-----------------------Common tools--------------------------
	
	/**
	 * Generate a token for the user creation and password change process.
	 * @return random secure token
	 * @throws Exception 
	 */
	private static String getRandomSecureToken() throws Exception{
		//create a random string
		String base = UUID.randomUUID().toString().replaceAll("-", "").substring(16);
		String salt = new String(Security.getRandomSalt(32), "UTF-8");
		String pepper = "5cd19a84ec46";
		//add the current time
		long now = System.currentTimeMillis();
		//do the user hash
		String secureToken = Security.hashClientPassword(base + pepper + now + salt);
		//add one character to distinguish
		secureToken = secureToken + "a";
		if (secureToken.length() < 16){
			throw new RuntimeException("Failed to create secure token!");
		}
		return secureToken;
	}
	
	//------------------------Connection---------------------------
	
	/**
	 * Write token to server (userId, idType) at tokenPath with time stamp.
	 * @return true/false
	 */
	private boolean writeLoginToken(String userid, String idType, String token, String tokenPath){
		if (token == null || token.length() < 16 || !tokenPath.startsWith(ACCOUNT.TOKENS)){
			Debugger.println("writeSecureToken(..) failed! Either because of wrong token or wrong path.", 1);
			return false;
		}
		long now = System.currentTimeMillis();
		String tokenPath_ts = tokenPath + "_ts";
		String[] keys = new String[]{tokenPath, tokenPath_ts};
		Object[] objects = new Object[]{token, new Long(now)};
		
		return write_protected(userid, idType, keys, objects);
	}
	/**
	 * Read token from ticket server with ticketId at tokenPath with time stamp.
	 * @return Object[] with [0]:token (string), [1]:token_ts (long) or null on error
	 */
	private Object[] readSupportToken(String ticketid, String tokenPath){
		if (!tokenPath.startsWith(ACCOUNT.TOKENS)){
			Debugger.println("readSecureToken(..) failed because of invalid path!", 1);
			return null;
		}
		String tokenPath_ts = tokenPath + "_ts";
		String[] keys = new String[]{tokenPath, tokenPath_ts};
		JSONObject res = read_reg_token(ticketid, keys);
		if (!Connectors.httpSuccess(res)){
			Debugger.println("readSupportToken(..) failed because the server request was not succesful!", 1);
			return null;
		}
		try{
			JSONObject item = (JSONObject) res.get("Item");
			JSONObject t = DynamoDB.dig(item, tokenPath);
			JSONObject tts = DynamoDB.dig(item, tokenPath_ts);
			String token = DynamoDB.typeConversion(t).toString();
			long token_ts = Converters.obj2LongOrDefault(DynamoDB.typeConversion(tts), 0l);
			return new Object[]{token, token_ts};
			
		}catch (Exception e){
			Debugger.println("readSupportToken(..) failed! Either the ticketID was not found or had no expected tokens!", 1);
			return null;
		}
	}
	
	/**
	 * Response of DynamoDB to basic info POST request. This is the only method that should be able to retrieve secret info like passwords and tokens. 
	 * Connectors.httpSuccess(result) can be used to check for POST status.
	 * @param userID - unique id, often email address
	 * @param idType - ID type 
	 * @param lookUp - array of strings to retrieve
	 * @return JSONObject with result (check yourself for usefulness)
	 */
	private JSONObject read_basics(String userID, String idType, String[] lookUp){
		JSONObject response;
		if (idType.equals(ID.Type.uid)){
			//UID
			response = DynamoDB.getItem(tableName, ACCOUNT.GUUID, userID, lookUp);
		}else if (idType.equals(ID.Type.email)){
			//EMAIL
			response = DynamoDB.queryIndex(tableName, ACCOUNT.EMAIL, userID, lookUp);
		}else if (idType.equals(ID.Type.phone)){
			//PHONE
			response = DynamoDB.queryIndex(tableName, ACCOUNT.PHONE, userID, lookUp);
		}else{
			throw new RuntimeException("Authentication.read_basics(...) reports 'unsupported identifier type': " + idType);
		}
		//System.out.println("Time needed: " + Debugger.toc(tic) + "ms");		//debug
		return response;
	}
	private JSONObject read_reg_token(String ticketID, String[] lookUp){
		return DynamoDB.getItem(ticketsTable, DynamoDB.PRIMARY_TICKET_KEY, ticketID, lookUp);
	}
	
	/**
	 * Write a protected account attribute. For server operations only!!!
	 * @param userID - account ID, often email address
	 * @param idType - ID type
	 * @param keys - keys to write
	 * @param objects - values to put
	 * @return write success true/false
	 */
	private boolean write_protected(String userID, String idType, String[] keys, Object[] objects){
		if (idType.equals(ID.Type.uid)){
			//UID
			errorCode = DynamoDB.writeAny(tableName, ACCOUNT.GUUID, userID, keys, objects);
		/* TODO: fix!
		}else if (idType.equals(ID.Type.email)){
			//EMAIL
			errorCode = DynamoDB.writeAny(tableName, ACCOUNT.EMAIL, userID, keys, objects);
		}else if (idType.equals(ID.Type.phone)){
			//PHONE
			errorCode = DynamoDB.writeAny(tableName, ACCOUNT.PHONE, userID, keys, objects);
		*/
		}else{
			throw new RuntimeException("Authentication.write_protected(...) reports 'unsupported identifier type': " + idType);
		}
		if (errorCode == 0){
			return true;
		}else{
			return false;
		}	
	}
	private boolean write_reg_token(String ticketID, String[] keys, Object[] objects){
		errorCode = DynamoDB.writeAny(ticketsTable, DynamoDB.PRIMARY_TICKET_KEY, ticketID, keys, objects);
		if (errorCode == 0){
			return true;
		}else{
			return false;
		}	
	}
	
}
