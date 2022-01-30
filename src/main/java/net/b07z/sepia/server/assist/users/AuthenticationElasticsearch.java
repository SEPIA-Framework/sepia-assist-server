package net.b07z.sepia.server.assist.users;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.assist.database.GUID;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Security;
import net.b07z.sepia.server.core.users.AuthenticationInterface;
import spark.Request;

/**
 * User authentication implemented with Elasticsearch.
 * 
 * @author Florian Quirin
 *
 */
public class AuthenticationElasticsearch implements AuthenticationInterface {
	
	private static final long access_lvl_token_valid_time = 1800000L;		//that token is valid 30min
	private static final long registration_token_valid_time = 86400000L;	//that token is valid 24h
	private static final long reset_token_valid_time = 1200000L;		//that token is valid 20min
	private static final long key_token_valid_time = Authenticator.SESSION_TOKEN_VALID_FOR;	//that token is valid for one day
	private static final long app_token_valid_time = Authenticator.APP_TOKEN_VALID_FOR;		//that token is valid for one year
	
	private static final String TICKET_TYPE_REG = "registry";
	private static final String TICKET_TYPE_SUPP = "support";
	
	//temporary secrets
	//private static String temporaryTokenSalt = Security.getRandomUUID().replaceAll("-", "").trim();
	
	Request metaInfo;
	
	//authentication constants
	private String userID = "-1";
	private int accessLvl = -1;
	private int errorCode = 0;
	private HashMap<String, Object> basicInfo;
	
	/**
	 * Get the database to query. Loads the Elasticsearch connection defined by settings. 
	 */
	private Elasticsearch getDB(){
		Elasticsearch es = new Elasticsearch();
		return es;
	}
	
	@Override
	public void setRequestInfo(Object request){
		this.metaInfo = (Request) request;	
	}
	
	@Override
	public boolean checkDatabaseConnection(){
		try{
			//Elasticsearch mappings
			JSONObject mappings = getDB().getMappings();
			boolean elasticOk = (
					mappings.containsKey(DB.USERS) && mappings.containsKey(DB.TICKETS) && 
					mappings.containsKey(DB.WHITELIST) && mappings.containsKey(GUID.INDEX) &&
					mappings.containsKey(DB.USERDATA)
			);
			//System.out.println(mappings);
			if (!elasticOk){
				Debugger.println("checkDatabaseConnection() - Elasticsearch mappings are incomplete or not valid!", 1);
				return false;
			}else{
				return true;
			}
			
		}catch (Exception e){
			errorCode = 3;
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public String userExists(String identifier, String idType) throws RuntimeException{
		
		String key = "";
		if (idType.equals(ID.Type.uid)){
			key = ACCOUNT.GUUID; 
		}else if (idType.equals(ID.Type.email)){
			key = ACCOUNT.EMAIL;
		}else if (idType.equals(ID.Type.phone)){
			key = ACCOUNT.PHONE;
		}else{
			errorCode = 4;
			throw new RuntimeException("userExists(...) reports 'unsupported ID type' " + idType);
		}
		//all primary user IDs need to be lowerCase in DB!!!
		identifier = ID.clean(identifier);
		
		//search parameters:
		JSONObject response = searchUserIndex(key, identifier);
		//System.out.println("RESPONSE: " + response.toJSONString());				//debug
		if (!Connectors.httpSuccess(response)){
			errorCode = 4;
			throw new RuntimeException("Authentication.userExists(...) reports 'DB query failed! Result unclear!'");
		}
		JSONArray hits = JSON.getJArray(response, new String[]{"hits", "hits"});
		if (hits != null && !hits.isEmpty()){
			return JSON.getJObject((JSONObject) hits.get(0), "_source").get(ACCOUNT.GUUID).toString();
		}else{
			errorCode = 4;
			return "";
		}
	}
	
	@Override
	public JSONObject registrationByEmail(String email){
		//all primary user IDs need to be lowerCase in DB!!!
		email = ID.clean(email);
		
		//never request an account for superuser - TODO: what about assistant user?
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
			if (!writeTicketIndex(TICKET_TYPE_REG, ticketId, ticketId, storeToken, time)){		//Note: ticketId is also used at guid
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

	@Override
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
				JSONObject res = readTicketIndex(TICKET_TYPE_REG, ticketId);
				if (res == null || !res.containsKey("token")){
					errorCode = 5;
					Debugger.println("createUser(...) can access tokens or is missing the token with required ID", 1); 	//debug
					return false;
				}
				String sendToken = Security.hashClientPassword(email + token + time + ticketId);
				String tokenTarget = JSON.getString(res, "token");
				long timeTarget = JSON.getLongOrDefault(res, "ts", -1l);
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
			if (!deleteFromTicketIndex(TICKET_TYPE_REG, ticketId)){
				Debugger.println("createUser(...) failed to delete used token!", 1); 	//debug
			}
			
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
		JSONObject account = new JSONObject();
		
		JSON.put(account, ACCOUNT.PASSWORD, pwd);
		JSON.put(account, ACCOUNT.PWD_SALT, salt);
		JSON.put(account, ACCOUNT.PWD_ITERATIONS, iterations);
		
		JSON.put(account, ACCOUNT.GUUID, guuid);
		JSON.put(account, ACCOUNT.EMAIL, ids[0]);
		JSON.put(account, ACCOUNT.PHONE, ids[1]);
		
		JSON.put(account, ACCOUNT.ROLES, JSON.makeArray(Role.user.name())); 	//default role
		
		JSON.put(account, ACCOUNT.USER_NAME, JSON.make(Name.NICK, "Boss"));		//Note: we could put this in settings
		JSON.put(account, ACCOUNT.INFOS, JSON.make("lang_code", LANGUAGES.EN));	//... so one can configure defaults
		
		JSON.put(account, ACCOUNT.STATISTICS, JSON.make("totalCalls", 1, "lastLogin", System.currentTimeMillis()));
		
		/*
		basicKeys.add(ACCOUNT.TOKENS);
		//basicKeys.add(ACCOUNT.APIS);
		basicKeys.add(ACCOUNT.STATISTICS);
		basicKeys.add(ACCOUNT.USER_NAME);
		basicKeys.add(ACCOUNT.INFOS);
		*/
		//-------------------------------------------------------------------------------------------
		
		//write values and return true/false - error codes can be checked afterwards if necessary
		boolean success = writeUserIndex(guuid, ID.Type.uid, account);
		
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
	
	@Override
	public JSONArray listUsers(Collection<String> keys, int from, int size){
		//validate keys
		keys.retainAll(ACCOUNT.allowedToShowAdmin);
		if (keys.isEmpty()){
			errorCode = 2;
			return null;
		}
		JSONObject response = getDB().getDocuments(DB.USERS, "all", from, size, keys);
		if (response == null){
			errorCode = 4;
			return null;
		}
		JSONArray hits = JSON.getJArray(response, new String[]{"hits", "hits"});
		if (hits != null && !hits.isEmpty()){
			JSONArray res = new JSONArray();
			for (Object o : hits){
				JSON.add(res, JSON.getJObject((JSONObject) o, "_source"));
			}
			return res;
		}else{
			errorCode = 4;
			return new JSONArray();
		}
	}
	
	@Override
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
			if (!writeTicketIndex(TICKET_TYPE_SUPP, ticketId, ticketId, storeToken, time)){		//Note: ticketId is also used at guid
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
	
	@Override
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
			JSONObject res = readTicketIndex(TICKET_TYPE_SUPP, ticketId);
			if (res == null || !res.containsKey("token")){
				errorCode = 5;
				Debugger.println("changePassword(...) can access tokens or is missing the token with required ID", 1); 	//debug
				return false;
			}
			String sendToken = Security.hashClientPassword(email + token + time + ticketId);
			String tokenTarget = JSON.getString(res, "token");
			long timeTarget = JSON.getLongOrDefault(res, "ts", -1l);
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
		if (!deleteFromTicketIndex(TICKET_TYPE_SUPP, ticketId)){
			Debugger.println("changePassword(...) failed to delete used token!", 1); 	//debug - warning
		}
		
		//-------------------------------------------------------------------------------------------

		//change password 
		JSONObject account = new JSONObject();
		JSON.put(account, ACCOUNT.PASSWORD, pwd);
		JSON.put(account, ACCOUNT.PWD_SALT, salt);
		JSON.put(account, ACCOUNT.PWD_ITERATIONS, iterations);
		if (!updateUserIndex(guuid, ID.Type.uid, account)){
			Debugger.println("changePassword(...) reports 'could not set new password for " + guuid + ", account may not be safe!'", 1);
			return false;
		}
		//delete old tokens
		if (deleteFromUserIndex(guuid, ACCOUNT.TOKENS) != 0){
			Debugger.println("changePassword(...) reports 'could not delete old tokens for " + guuid + ", account may not be safe!'", 1);
			return false;
		}
		//if you made it until here return true
		return true;
	}
	
	@Override
	public boolean deleteUser(JSONObject info) {
		//TODO: make it like "createUser" ... or is a credentials check before enough?
		//TODO: clean up DB.USERDATA as well
		
		//get user ID
		String userid = (String) info.get("userid");
		if (userid == null || userid.isEmpty()){
			errorCode = 4;
			return false;
		}else{
			userid = ID.clean(userid);
		}
		//delete
		return deleteIdFromUserIndex(userid);
	}

	@Override
	public boolean authenticate(JSONObject info) {
		String userId = (String) info.get("userId");
		String password = (String) info.get("pwd");
		String idType = (String) info.get("idType");
		String client = (String) info.get("client");
		String incToken = (String) info.get(ACCOUNT.ACCESS_LVL_TOKEN);
		int tokenType = -1;
		
		//use the DynamoDB access to read ---BASICS--- (you can add these basics to USER.checked to avoid double-loading!)
		userId = ID.clean(userId);
		
		//get basic fields
		String[] basics = AuthenticationCommons.getBasicReadFields();
		JSONObject item = readUserIndex(userId, idType, basics);
		//System.out.println("Auth. req: " + userId + ", " + idType + ", " + password); 		//debug
		//System.out.println("Auth. res: " + item.toJSONString()); 		//debug
		//Status?
		if (item == null || item.isEmpty()){
			errorCode = 3;
			return false;
		
		}else{
			String userIdTmp = JSON.getString(item, ACCOUNT.GUUID); 	//unconfirmed ID (before pwd check)
			boolean isAdminId = (userIdTmp != null && !userIdTmp.isEmpty() && userIdTmp.equals(Config.superuserId));
			String pwd = null;
			//token key
			if (password.length() == 65){
				tokenType = 0;
				String tokenId = getAppTokenId(client);
				JSONObject storedTokenObj = JSON.getJObject(item, new String[]{ ACCOUNT.TOKENS, tokenId });
				if (storedTokenObj != null && !storedTokenObj.isEmpty()){
					pwd = JSON.getString(storedTokenObj, "token");
					//check time stamp
					long valid_time = app_token_valid_time;
					if (isAdminId || CLIENTS.isRatherUnsafe(client)){
						valid_time = key_token_valid_time;		//admin and unsafe clients have short-lived token
					}
					long ts = JSON.getLongOrDefault(storedTokenObj, "ts", 0);
					if ((System.currentTimeMillis() - ts) > valid_time){
						//token became invalid
						if (pwd.equals(password)){
							errorCode = 5;
							return false;
						}else{
							errorCode = 2;
							return false;
						}
					}
				}
				
			//original key - this is the login that generates the token later and should not be abused for client-authentication via real password
			}else{
				tokenType = 1;
				try {
					pwd = JSON.getString(item, ACCOUNT.PASSWORD);
					String salt = JSON.getString(item, ACCOUNT.PWD_SALT);
					int iterations = JSON.getIntegerOrDefault(item, ACCOUNT.PWD_ITERATIONS, 1);
					ID.Generator gen = new ID.Generator(password, salt, iterations);
					password = gen.pwd;
				} catch (Exception e) {
					Debugger.println("Authentication_Elasticsearch.authenticate(...) - using original password failed due to: " + e.getMessage(), 1); 	//debug
					if (e.getStackTrace() != null && e.getStackTrace().length > 0){
						Debugger.println("Authentication_Elasticsearch.authenticate(...) - error last trace: " + e.getStackTrace()[0], 1);
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
					//TODO: currently there is no way to ask for this token, but you can create it with:
					//writeAccessLvlToken method
					try{
						if (incToken != null){
							String storedIncToken = null;
							JSONObject storedIncTokenObj = JSON.getJObject(item, new String[]{ ACCOUNT.TOKENS, ACCOUNT.ACCESS_LVL_TOKEN });
							if (storedIncTokenObj != null){
								storedIncToken = JSON.getString(storedIncTokenObj, "token");
							}							
							if (storedIncToken !=null && !incToken.isEmpty() && !storedIncToken.isEmpty()){
								String newAccessLvl = storedIncToken.substring(0, 2);
								if ((newAccessLvl+incToken).equals(storedIncToken)){
									//check time stamp
									long storedIncToken_TS = JSON.getLongOrDefault(storedIncTokenObj, "ts", 0);
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
					
					//--------- now get basic info (abstraction layer for account data) ----------
					
					basicInfo = new HashMap<String, Object>();
					AuthenticationCommons.mapBasicInfo(basicInfo, userID, item, (json, key) -> {
						return json.get(key);
					}); 
										
					//-----------------------------------------
					
					return true;
				}
				
			}else{
				errorCode = 2;
				return false;
			}
		}
	}
	
	@Override
	public String writeKeyToken(String userid, String client) {
		//get token ID
		String tokenId = getAppTokenId(client);
		if (tokenId == null || tokenId.isEmpty()){
			Debugger.println("writeKeyToken(..) - failed to create tokenId from client: " + client, 1);
			errorCode = 3;
			return "";
		}
		//write server token
		String userToken = writeToken(userid, tokenId);
		if (userToken.isEmpty()){
			errorCode = 3;
			return "";
		}else{
			return userToken;
		}
	}
	public String writeAccessLvlToken(String userid) {
		//get token ID
		String tokenId = ACCOUNT.ACCESS_LVL_TOKEN;
		if (tokenId == null || tokenId.isEmpty()){
			Debugger.println("writeAccessLvlToken(..) - access level token ID is missing!", 1);
			errorCode = 3;
			return "";
		}
		//write server token
		String userToken = writeToken(userid, tokenId);
		if (userToken.isEmpty()){
			errorCode = 3;
			return "";
		}else{
			return userToken;
		}
	}
	/**
	 * Create token and write with tokenId to user account.
	 * @param userId - guuid of user
	 * @param tokenId - ID used to store that token
	 * @return token or empty string
	 */
	private String writeToken(String userId, String tokenId){
		if (userId == null || userId.isEmpty() || tokenId == null || tokenId.length() < 4){
			Debugger.println("writeToken(..) - invalid user or token ID! UserId: " + userId + ", tokenId: " + tokenId, 1);
			errorCode = 3;
			return "";
		}
		//create a random string
		String userToken;
		try {
			userToken = getRandomSecureToken();
			
		} catch (Exception e) {
			Debugger.println("writeToken(..) - failed to create secure token! Id: " + userId, 1);
			e.printStackTrace();
			errorCode = 3;
			return "";
		}
		JSONObject tokenObj = JSON.make(
			ACCOUNT.TOKENS, JSON.make(tokenId, 
				JSON.make(
					"token", userToken,
					"ts", System.currentTimeMillis()
				)
			)
		);
		if (!updateUserIndex(userId, ID.Type.uid, tokenObj)){
			Debugger.println("writeToken(..) - failed to write token '" + tokenId + "' for: " + userId, 1);
			errorCode = 3;
			return "";
		}else{
			return userToken;
		}
	}

	@Override
	public boolean logout(String userid, String client) {
		//get token id from client name
		String tokenId = getAppTokenId(client);
		if (tokenId == null || tokenId.length() < 4){
			Debugger.println("logout(..) - failed to create token ID from client! Id: " + userid + ", client: " + client, 1);
			errorCode = 3;
			return false;
		}
		String tokenPath = ACCOUNT.TOKENS + "." + tokenId;
		//delete specific token
		if (deleteFromUserIndex(userid, tokenPath) != 0){
			Debugger.println("logout(...) reports 'could not delete '" + tokenPath + "' for " 
							+ userid + ", account may not be safe!'", 1);
			return false;
		}else{
			return true;
		}
	}
	@Override
	public boolean logoutAllClients(String userid) {
		if (userid == null || userid.isEmpty()){
			Debugger.println("logoutAllClients(...) reports 'missing user ID'", 1);
			errorCode = 4;
			return false;
		}
		//delete all tokens
		if (deleteFromUserIndex(userid, ACCOUNT.TOKENS) != 0){
			Debugger.println("logoutAllClients(...) reports 'could not delete old tokens for " 
							+ userid + ", account may not be safe!'", 1);
			errorCode = 4;
			return false;
		}else{
			return true;
		}
	}
	
	@Override
	public String getUserID() {
		return userID;
	}

	@Override
	public int getAccessLevel() {
		return accessLvl;
	}
	
	@Override
	public HashMap<String, Object> getBasicInfo() {
		return basicInfo;
	}

	@Override
	public int getErrorCode() {
		return errorCode;
	}
	
	/**
	 * Get the proper ID for the token used by this client (based on client_info).
	 * @param client - client_info as sent by user
	 * @return
	 */
	private String getAppTokenId(String client){
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
		String pepper = "5cd18a83ec46";
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
	 * Response of Elasticsearch to user index request. This is the only method that should be able to retrieve secret info like passwords and tokens. 
	 * Connectors.httpSuccess(result) can be used to check for GET status.
	 * @param userID - unique id, often email address
	 * @param idType - ID type 
	 * @param lookUp - array of strings to retrieve (only applies when using idType "uid"
	 * @return JSONObject with response (or null/empty)
	 */
	private JSONObject readUserIndex(String userID, String idType, String[] lookUp){
		if (idType.equals(ID.Type.uid)){
			//UID
			JSONObject response;
			if (lookUp == null || lookUp.length == 0){
				response = getDB().getItemFiltered(DB.USERS, "all", userID, lookUp);
			}else{
				response = getDB().getItem(DB.USERS, "all", userID);
			}
			return JSON.getJObject(response, "_source");
			
		}else if (idType.equals(ID.Type.email)){
			//EMAIL
			JSONObject result = getDB().searchSimple(DB.USERS + "/all", ACCOUNT.EMAIL + ":" + userID);
			JSONArray hits = JSON.getJArray(result, new String[]{"hits", "hits"});
			if (hits != null && !hits.isEmpty()){
				return JSON.getJObject((JSONObject) hits.get(0), "_source");
			}else{
				return null;
			}
		}else if (idType.equals(ID.Type.phone)){
			//PHONE
			JSONObject result = getDB().searchSimple(DB.USERS + "/all", ACCOUNT.PHONE + ":" + userID);
			JSONArray hits = JSON.getJArray(result, new String[]{"hits", "hits"});
			if (hits != null && !hits.isEmpty()){
				return JSON.getJObject((JSONObject) hits.get(0), "_source");
			}else{
				return null;
			}
		}else{
			throw new RuntimeException("Authentication.readUserIndex(...) reports 'unsupported identifier type': " + idType);
		}
		//System.out.println("Time needed: " + Debugger.toc(tic) + "ms");		//debug
		
	}
	private JSONObject searchUserIndex(String key, String value){
		return getDB().searchSimple(DB.USERS + "/all", key + ":" + value);
	}
	/**
	 * Write a protected account attribute. For server operations only!!!
	 * @param userID - account ID, sometimes email address
	 * @param idType - ID type
	 * @param jsonData - data as JSON to write (fields will be overwritten)
	 * @return write success true/false
	 */
	private boolean writeUserIndex(String userID, String idType, JSONObject jsonData){
		if (idType.equals(ID.Type.uid)){
			//UID
			errorCode = getDB().writeDocument(DB.USERS, "all", userID, jsonData);
		/* TODO: fix!
		}else if (idType.equals(ID.Type.email)){
			//EMAIL
			//
		}else if (idType.equals(ID.Type.phone)){
			//PHONE
			//
		*/
		}else{
			throw new RuntimeException("Authentication.writeUserIndex(...) reports 'unsupported identifier type': " + idType);
		}
		if (errorCode == 0){
			return true;
		}else{
			return false;
		}	
	}
	/**
	 * Update a protected account attribute. For server operations only!!!
	 * @param userID - account ID, sometimes email address
	 * @param idType - ID type
	 * @param jsonData - data as JSON to write (fields will be updated)
	 * @return write success true/false
	 */
	private boolean updateUserIndex(String userID, String idType, JSONObject jsonData){
		if (idType.equals(ID.Type.uid)){
			//UID
			errorCode = getDB().updateDocument(DB.USERS, "all", userID, jsonData);
		/* TODO: fix!
		}else if (idType.equals(ID.Type.email)){
			//EMAIL
			//
		}else if (idType.equals(ID.Type.phone)){
			//PHONE
			//
		*/
		}else{
			throw new RuntimeException("Authentication.updateUserIndex(...) reports 'unsupported identifier type': " + idType);
		}
		if (errorCode == 0){
			return true;
		}else{
			return false;
		}	
	}
	/**
	 * Delete field from user index.
	 * @param guuid - unique user ID
	 * @param fieldToDelete - field to delete (can be inside an object, e.g. uname.first)
	 * @return errorCode - 0: OK, 1: connection error - note: we can't check here for successful delete :-(
	 */
	private int deleteFromUserIndex(String guuid, String fieldToDelete){
		return getDB().deleteFromDocument(DB.USERS, "all", guuid, fieldToDelete);
	}
	/**
	 * Delete ID from user index.
	 */
	private boolean deleteIdFromUserIndex(String guuid){
		errorCode = getDB().deleteDocument(DB.USERS, "all", guuid);
		if (errorCode == 0){
			return true;
		}else{
			return false;
		}	
	}
	
	
	/**
	 * Make request to ticket index.
	 * @param ticketType - currently TICKET_TYPE_REG or TICKET_TYPE_SUPP
	 * @param ticketID - id given during ticket creation
	 * @return JSONObject with response (or null/empty)
	 */
	private JSONObject readTicketIndex(String ticketType, String ticketID){
		JSONObject response;
		if (!ticketType.equals(TICKET_TYPE_REG) && !ticketType.equals(TICKET_TYPE_SUPP)){
			throw new RuntimeException("Authentication.readTicketIndex(...) reports 'unsupported identifier type': " + ticketType);
		}
		response = getDB().getItem(DB.TICKETS, ticketType, ticketID);
		return JSON.getJObject(response, "_source");
	}
	/**
	 * Write to ticket index.
	 */
	private boolean writeTicketIndex(String ticketType, String ticketID, String guid, String token, long ts){
		if (!ticketType.equals(TICKET_TYPE_REG) && !ticketType.equals(TICKET_TYPE_SUPP)){
			throw new RuntimeException("Authentication.writeTicketIndex(...) reports 'unsupported identifier type': " + ticketType);
		}
		errorCode = getDB().writeDocument(DB.TICKETS, ticketType, ticketID, JSON.make(
				"guid", guid,
				"token", token,
				"ts", ts
		));
		if (errorCode == 0){
			return true;
		}else{
			return false;
		}	
	}
	/**
	 * Delete ticket from index.
	 */
	private boolean deleteFromTicketIndex(String ticketType, String ticketId){
		if (!ticketType.equals(TICKET_TYPE_REG) && !ticketType.equals(TICKET_TYPE_SUPP)){
			throw new RuntimeException("Authentication.deleteFromTicketIndex(...) reports 'unsupported identifier type': " + ticketType);
		}
		errorCode = getDB().deleteDocument(DB.TICKETS, ticketType, ticketId);
		if (errorCode == 0){
			return true;
		}else{
			return false;
		}	
	}
}
