package net.b07z.sepia.server.assist.users;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.users.AuthenticationInterface;
import spark.Request;

/**
 * This class was formerly called "AuthenticationToken" but now it's more of an access point to authentication.
 * The "Authenticator" will be stored in "User" for the duration of the session. 
 * You can use it to check authentication, validate database requests etc. by using the methods included. 
 * It is another abstraction layer to simplify access to account basics. 
 * 
 * @author Florian Quirin
 *
 */
public class Authenticator {
	
	public static final long APP_TOKEN_VALID_FOR = 3153600000L; 	//1 year
	public static final long SESSION_TOKEN_VALID_FOR = 86400000L; 	//24h
	
	private String tokenHash = "";				//token created on demand
	private long timeCreated = 0;				//System time on creation
	private long tokenValidUntil = 0;			//token valid time
	private boolean authenticated = false;		//is the user authenticated?
	private String userID = "";					//user ID received from authenticator
	private String key = "-1";					//key to access micro-services and other APIs
	private String client = "web_app";			//client
	private int accessLvl = -1;					//user access level received from authenticator
	private HashMap<String, Object> basicInfo;		//basic info of the user acquired during authentication
	private int errorCode;						//errorCode passed down from authenticator 
	
	//Basic info fields - Note: this is another abstraction layer for the core account values ... it's different from ACCOUNT!
	public static final String GUUID = "uid";
	public static final String EMAIL = "email";
	public static final String PHONE = "phone";
	public static final String USER_ROLES = "user_roles";
	public static final String USER_NAME = "user_name";
	public static final String USER_LANGUAGE = "user_lang_code";
	public static final String USER_BIRTH = "user_birth";
	public static final String BOT_CHARACTER = "bot_character";
	public static final String USER_PREFERRED_UNIT_TEMP = "unit_pref_temp";
	public static final String SHARED_ACCESS_PERMISSIONS = "shared_access";
	
	//Account protection against brute force login-attempts 
	private static Map<String, AtomicInteger> protectedAccountsFailedLoginAttempts = new HashMap<>();
	private static Map<String, AtomicLong> protectedAccountsLastLoginAttempt = new HashMap<>();
	private static Map<String, String> protectedAccountsEmailToIdMap = new HashMap<>();
	private static int failedLoginAttemptThreshold = 5;
	
	public static void addProtectedAccount(String uid, String email){
		if (!protectedAccountsFailedLoginAttempts.containsKey(uid)){
			protectedAccountsFailedLoginAttempts.put(uid, new AtomicInteger(0));
			protectedAccountsLastLoginAttempt.put(uid, new AtomicLong(0));
		}
		if (!protectedAccountsEmailToIdMap.containsKey(email)){
			protectedAccountsEmailToIdMap.put(email, uid);
		}
	}
	
	/**
	 * Create invalid, empty token.
	 */
	public Authenticator(){}
	/**
	 * Default constructor for token.
	 */
	public Authenticator(String username, String password, String idType, String client, Request requestMetaInfo){
		//no password at all?
		if (Is.nullOrEmpty(password)){
			errorCode = 6;
			return;
		}
		try {
			AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule); 	//e.g.: new Authentication_Demo();
			auth.setRequestInfo(requestMetaInfo);
			JSONObject info = new JSONObject();
				JSON.add(info, "userId", username);
				JSON.add(info, "pwd", password);
				JSON.add(info, "idType", idType);
				JSON.add(info, "client", client);
			//Global login protection for special accounts
			String protectedUid = null;
			boolean blocked = false;
			if (Config.improveSecurityForProtectedAccounts){
				if (idType.equals(ID.Type.uid) && protectedAccountsLastLoginAttempt.containsKey(username)){
					protectedUid = username;
				}else if (protectedAccountsEmailToIdMap.containsKey(username)){
					protectedUid = protectedAccountsEmailToIdMap.get(username);
				}
				if (protectedUid != null){
					//check if there were too many failed login attempts recently
					int attempts = protectedAccountsFailedLoginAttempts.get(protectedUid).get();
					long timeSinceLastFail = System.currentTimeMillis() - protectedAccountsLastLoginAttempt.get(protectedUid).get();
					if (attempts > 0){
						if (timeSinceLastFail < Config.protectedAccountsBlockTimeout){
							if (attempts >= failedLoginAttemptThreshold && timeSinceLastFail < Config.protectedAccountsBlockTimeout){
								//block login try
								blocked = true;
								Debugger.println("Authenticator - temporarily blocked login for '" + protectedUid 
										+ "' because of too many failed attempts!", 1);
							}
						}else{
							protectedAccountsFailedLoginAttempts.get(protectedUid).decrementAndGet();
						}
					}
				}
			}
			//Validate
			if (blocked){
				errorCode = 10;
			}else if (auth.authenticate(info)){
				timeCreated = System.currentTimeMillis();
				authenticated = true;
				userID = auth.getUserID();
				key = password;
				this.client = client;
				accessLvl = auth.getAccessLevel();
				basicInfo = auth.getBasicInfo();
				errorCode = 0;
			}else{
				errorCode = auth.getErrorCode();
				if (protectedUid != null && (errorCode == 2 || errorCode == 3)){
					protectedAccountsFailedLoginAttempts.get(protectedUid).incrementAndGet();
					protectedAccountsLastLoginAttempt.get(protectedUid).set(System.currentTimeMillis());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Constructor for test tokens.
	 */
	public Authenticator(String id, Request request){
		timeCreated = System.currentTimeMillis();
		authenticated = true;
		userID = id;
		key = "pwd";
		this.client = "web_app";
		accessLvl = 0;
		basicInfo = new HashMap<>();
		errorCode = 0;
	}
	
	/**
	 * Passed authentication and token are still valid?
	 * @return true/false
	 */
	public boolean authenticated(){
		if (isValid()){
			return authenticated;
		}else{
			return false;
		}
	}
	
	/**
	 * Check if the token is still valid. It remains valid for 5 minutes only!
	 * @return true/false
	 */
	public boolean isValid(){
		//TODO: is that all?
		if (timeCreated == 0){
			return false;
		}
		boolean upToDate = (System.currentTimeMillis()-timeCreated) < 300000;
		if (upToDate){
			return true;
		}else{
			Debugger.println("isValid() in authentication token check failed! Something must have timed out somewhere.", 1);
			return false;
		}
	}
	
	/**
	 * Get user ID of this user. This is the unique identifier of the user like a number or email address.
	 * Usually it is acquired during authentication.
	 * @return
	 */
	public String getUserID(){
		return userID;
	}
	
	/**
	 * Get key. Note: I have a bad feeling about this!
	 * @return key
	 */
	public String getUserKey(){
		return key;
	}
	
	/**
	 * Get user client info
	 * @return
	 */
	public String getClientInfo(){
		return client;
	}
	
	/**
	 * Get access level set during authentication.
	 * @return
	 */
	public int getAccessLevel(){
		return accessLvl;
	}
	
	/**
	 * Get basic info of the user acquired during authentication. 
	 * This typically includes GUUID, EMAIL, USER_ROLES, USER_NAME, USER_LANGUAGE, etc., but might depend on specific auth. module settings.
	 * @return
	 */
	public HashMap<String, Object> getBasicInfo() {
		return basicInfo;
	}
	/**
	 * Get all (safe) core fields received during authentication and add them to a JSONObject. 
	 * @param msg - JSONObject to add the data to
	 */
	public void addBasicInfoToJsonObject(JSONObject msg) {
		HashMap<String, Object> basics = getBasicInfo();
		if (basics != null){
			//IDs
			String guuid = getUserID();
			JSON.add(msg, GUUID, guuid);
			String email = (String) basics.get(EMAIL);
			if (email != null && !email.equals("-")){
				JSON.add(msg, EMAIL, email);
			}
			String phone = (String) basics.get(PHONE);
			if (phone != null && !phone.equals("-")){
				JSON.add(msg, PHONE, phone);
			}				
			//ROLES
			Object user_roles = basics.get(USER_ROLES);
			if (user_roles != null){
				JSON.add(msg, USER_ROLES, user_roles);
			}
			//NAME
			@SuppressWarnings("unchecked")
			Map<String, Object> user_name = (Map<String, Object>) basics.get(USER_NAME);
			if (user_name != null){
				JSON.add(msg, USER_NAME, user_name);
			}
			//LANGUAGE
			String user_lang_code = (String) basics.get(USER_LANGUAGE);
			if (user_lang_code != null && !user_lang_code.isEmpty()){
				JSON.add(msg, USER_LANGUAGE, user_lang_code);
			}
			//BIRTH
			String user_birth = (String) basics.get(USER_BIRTH);
			if (user_birth != null && !user_birth.isEmpty()){
				JSON.add(msg, USER_BIRTH, user_birth);
			}
			//BOT CHARACTER
			String bot_char = (String) basics.get(BOT_CHARACTER);
			if (bot_char != null && !bot_char.isEmpty()){
				JSON.add(msg, BOT_CHARACTER, bot_char);
			}
			//PREFERRED UNIT for TEMPERATURE
			String unit_pref_temp = (String) basics.get(USER_PREFERRED_UNIT_TEMP);
			if (unit_pref_temp != null && !unit_pref_temp.isEmpty()){
				JSON.add(msg, USER_PREFERRED_UNIT_TEMP, unit_pref_temp);
			}
			//SHARED ACCESS PERMISSIONS
			JSONObject shared_acc_perm = (JSONObject) basics.get(SHARED_ACCESS_PERMISSIONS);
			if (shared_acc_perm != null && !shared_acc_perm.isEmpty()){
				JSON.add(msg, SHARED_ACCESS_PERMISSIONS, shared_acc_perm);
			}
		}		
	}
	
	/**
	 * Get a secure key token for user authentication and write it to database.
	 * @param client - depending on the client different tokens can be used  
	 * @return 65char token or empty string
	 */
	public String getKeyToken(String client){
		AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
		tokenHash = auth.writeKeyToken(userID, client);
		tokenValidUntil = System.currentTimeMillis() + (CLIENTS.isRatherUnsafe(client)? (SESSION_TOKEN_VALID_FOR) : (APP_TOKEN_VALID_FOR));
		errorCode = auth.getErrorCode(); 
		return tokenHash;
	}
	
	/**
	 * Get UNIX time stamp that indicates when the key token expires.
	 */
	public long getKeyTokenValidTime(){
		return this.tokenValidUntil;
	}
	
	/**
	 * Logout user.
	 * @param client - depending on the client different tokens can be used
	 * @return false/true
	 */
	public boolean logoutUser(String client){
		AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
		boolean res = auth.logout(userID, client);
		errorCode = auth.getErrorCode(); 
		return res;
	}
	/**
	 * Logout user from all clients.
	 * @return false/true
	 */
	public boolean logoutAllClients(){
		AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
		boolean res = auth.logoutAllClients(userID);
		errorCode = auth.getErrorCode(); 
		return res;
	}

	/**
	 * Error code passed down from authentication.
	 * 0 - no errors <br>
	 * 1 - communication error (like server did not respond)	<br>
	 * 2 - access denied (due to wrong credentials or whatever reason)	<br>
	 * 3 - might be 1 or 2 whereas 2 can also be that the parameters were wrong<br>
	 * 4 - unknown error <br>
	 * 5 - during registration: user already exists; during createUser: invalid token or time stamp	<br>
	 * 10 - blocked due to too many failed attempts
	 * @return
	 */
	public int getErrorCode(){
		return errorCode;
	}
}
