package net.b07z.sepia.server.assist.users;

import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Address;
import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.services.ServiceAccessManager;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Security;
import net.b07z.sepia.server.core.tools.Timer;
import net.b07z.sepia.server.core.users.AuthenticationInterface;

public class Test_AuthAndAccount {

	static AuthenticationInterface auth;
	static AccountInterface acc;
	
	//Complete test-run of authentication and account access:
	
	public static void main(String[] args) throws Exception {
		
		//setup database configuration
		setup();
		
		//check if DB is ready to be used
		if (!auth.checkDatabaseConnection()){
			throw new RuntimeException("Database is not setup properly!");
		}
		
		//new user data
		String email = "testJ@b07z.net";
		String pwd = "test-12345";
		String newPwd = "test12345";
		
		//delete old test user
		String testId = auth.userExists(email, ID.Type.email);
		if(!testId.isEmpty()){
			if (auth.deleteUser(JSON.make("userid", testId))){
				System.out.println("\nDeleted old user.\n");
				Timer.threadSleep(1000);
			}
		}
		
		//create new user
		createUser(email, pwd);
		System.out.println("User created.");
		Timer.threadSleep(1000);
		
		//test if user exists
		String guuid = testUserExistenceByEmail(email);
		System.out.println("User GUUID: " + guuid);
		
		//authenticate user
		System.out.println("\nAuthentication by email:");
		boolean authenticatedEmail = authentication(email, pwd, ID.Type.email);
		System.out.println("\nAuthentication by ID:");
		boolean authenticatedGuuid = authentication(guuid, pwd, ID.Type.uid);
		if (!authenticatedEmail || !authenticatedGuuid){
			throw new RuntimeException("Password authentication failed with: " + authenticatedEmail + ", " + authenticatedGuuid);
		}
		
		//use auth. token object class:
		
		//test auth.
		String client = Config.defaultClientInfo;
		String client2 = "my_app_v0.9.9";
		Authenticator authToken = getAuthToken(guuid, pwd, "", client);
		System.out.println("\nAuth.Token test (expect true): " + authToken.authenticated());
		
		//get login token and new auth. object
		String loginToken = authToken.getKeyToken(client);
		Timer.threadSleep(500);
		if (loginToken.isEmpty()){
			throw new RuntimeException("Failed to create login-token!");
		}else{
			System.out.println("Login-token: " + loginToken);
		}
		authToken = getAuthToken(guuid, "", loginToken, client);
		System.out.println("Auth.Token test with login-token (expect true): " + authToken.authenticated());
		
		//get 2nd login token with different client
		String loginToken2 = authToken.getKeyToken(client2);
		Timer.threadSleep(500);
		if (loginToken2.isEmpty()){
			throw new RuntimeException("Failed to create login-token!");
		}else{
			System.out.println("Login-token2: " + loginToken2);
		}
		//and check both at same time
		authToken = getAuthToken(guuid, "", loginToken2, client2);
		System.out.println("Auth.Token test with 2nd login-token (expect true): " + authToken.authenticated());
		authToken = getAuthToken(guuid, "", loginToken, client);
		System.out.println("Auth.Token test with 1st login-token again (expect true): " + authToken.authenticated());
		
		//logout one client and test 
		if (!authToken.logoutUser(client)){
			throw new RuntimeException("Logout failed!");
		}
		authToken = getAuthToken(guuid, "", loginToken, client);
		System.out.println("Auth.Token test with 1st login-token after log-out (expect false): " + authToken.authenticated());
		
		//logout all clients
		authToken = getAuthToken(guuid, "", loginToken2, client2);
		if (!authToken.logoutAllClients()){
			throw new RuntimeException("Logout all clients failed!");
		}
		authToken = getAuthToken(guuid, "", loginToken2, client2);
		System.out.println("Auth.Token test with 2nd login-token after log-out all (expect false): " + authToken.authenticated());
		
		//get new login-token (just to see if it gets invalid after change) and request new password
		authToken = getAuthToken(guuid, pwd, "", client);
		loginToken = authToken.getKeyToken(client);
		changePassword(email, newPwd);
		Timer.threadSleep(1000);
		System.out.println("\nChanged password.\n");
		authToken = getAuthToken(guuid, "", loginToken, client);
		System.out.println("Auth.Token test with 1st login-token after password change (expect false): " + authToken.authenticated());
		authToken = getAuthToken(guuid, pwd, "", client);
		System.out.println("Auth.Token test with old pwd after password change (expect false): " + authToken.authenticated());
		authToken = getAuthToken(guuid, newPwd, "", client);
		System.out.println("Auth.Token test with new pwd after password change (expect true): " + authToken.authenticated());
		
		//check account:
		
		//read data directly
		List<String> roles = DB.readUserRolesDirectly(guuid);
		if (roles == null || roles.isEmpty()){
			throw new RuntimeException("\nReading user roles directly from DB failed!");
		}
		System.out.println("\nUser roles: " + roles);
		
		//write data directly
		roles.add(Role.developer.name());
		if (!DB.writeAccountDataDirectly(guuid, JSON.make(ACCOUNT.ROLES, roles))){
			throw new RuntimeException("\nWriting user roles directly from DB failed!");
		}else{
			System.out.println("Added role: developer");
		}
		
		//create a fresh token
		loginToken = authToken.getKeyToken(client);
		authToken = getAuthToken(guuid, "", loginToken, client);
		System.out.println("\nBasic-info of account: ");
		Debugger.printMap(authToken.getBasicInfo());
				
		//create user and API manager
		User user = new User(null, authToken);
		ServiceAccessManager apiMan = Config.superuserApiMng; 		//TODO: this is not really implemented yet
		
		//get roles previously defined
		System.out.println("\n" + user.userName.nick + " has roles: " + user.getUserRoles().toString());
		if (!user.hasRole(Role.developer)){
			throw new RuntimeException("\nMissing role!");
		}
		
		//write basic statistics for user
		if (!acc.writeBasicStatistics(guuid)){
			throw new RuntimeException("\nWriting user statistics failed!");
		}
		
		//load data
		System.out.println("\nSome data on request: ");
		acc.getInfos(user, apiMan, ACCOUNT.USER_NAME_NICK);
		System.out.println("User nick-name: " + user.getInfo(ACCOUNT.USER_NAME_NICK));
		acc.getInfos(user, apiMan, ACCOUNT.PASSWORD, ACCOUNT.TOKENS);
		System.out.println("Password (expect null): " + user.getInfo(ACCOUNT.PASSWORD));

		//write data
		System.out.println("\nWrite and show data: ");
		JSONObject data = new JSONObject();
		JSON.putWithDotPath(data, ACCOUNT.USER_NAME_NICK, "");
		JSON.putWithDotPath(data, ACCOUNT.USER_NAME_FIRST, "MIster");
		if (acc.setInfos(user, apiMan, data) != 0){
			throw new RuntimeException("\nWriting user data failed!");
		}
		Timer.threadSleep(500);
		user.clearInfo();
		System.out.println("User name: " + acc.getInfoObject(user, apiMan, ACCOUNT.USER_NAME));
		System.out.println("User name nick (expect null/empty): " + acc.getInfoObject(user, apiMan, ACCOUNT.USER_NAME_NICK));
		System.out.println("User name first (expect MIster): " + acc.getInfoObject(user, apiMan, ACCOUNT.USER_NAME_FIRST));
		
		//try to write restricted data
		System.out.println("\nTry to write restricted data: ");
		data = new JSONObject();
		JSON.putWithDotPath(data, ACCOUNT.PASSWORD, "easy");
		JSON.putWithDotPath(data, ACCOUNT.TOKENS + ".my_app", "easy2");
		if (acc.setInfos(user, apiMan, data) != 0){
			System.out.println("\nWriting user data failed ... as planned :-)");
		}else{
			throw new RuntimeException("\nWriting user data showed no error ... put it should!");
		}
		
		//--------- USER TESTS -----------
		
		System.out.println("\nTest user class:");
		ServiceAccessManager apiManager = Config.superuserApiMng;
		
		//create fresh user
		authToken = getAuthToken(guuid, "", loginToken, client);
		user = new User(null, authToken);
		
		//load name
		String name = user.getName(apiManager);
		System.out.println("\nName: " + name);
		System.out.println("Pref. language: " + user.getInfo_String(ACCOUNT.USER_PREFERRED_LANGUAGE));
		
		//save some stuff
		int code = user.saveInfoToAccount(apiManager, JSON.make(
				ACCOUNT.USER_NAME, JSON.make(Name.NICK, "Bossy"),
				ACCOUNT.INFOS, JSON.make("lang_code", LANGUAGES.DE)
		));
		if (code != 0){
			throw new RuntimeException("\nFailed to write account data!");
		}
		Timer.threadSleep(1000);
		
		//load same data
		user.clearInfo();
		if (user.loadInfoFromAccount(apiManager, ACCOUNT.USER_NAME_NICK, ACCOUNT.USER_PREFERRED_LANGUAGE) != 0){
			throw new RuntimeException("\nFailed to load account data!");
		}
		System.out.println("\nNew nick-name: " + user.getInfo_String(ACCOUNT.USER_NAME_NICK));
		System.out.println("New pref. language: " + user.getInfo_String(ACCOUNT.USER_PREFERRED_LANGUAGE));
		
		//load address
		Address userHome = user.getTaggedAddress(Address.USER_HOME_TAG, true);
		System.out.println("Home: " + userHome);
		if (userHome != null && !userHome.isEmpty()){
			throw new RuntimeException("\nHome address should be null at this point!");
		}
		
		//set address and load again
		userHome = new Address("Germany", "NRW", "Essen", "45127", 
				"Porschplatz", "1", 
				"51.458", "7.015",
				"Cityhall Essen", "The cityhall of Essen");
		JSONObject res = user.getUserDataAccess().setOrUpdateSpecialAddress(user, 
				Address.USER_HOME_TAG, null, userHome.buildJSON());
		Timer.threadSleep(1000);
		System.out.println("\nSet home address answer: " + res);
		userHome = user.getTaggedAddress(Address.USER_HOME_TAG, true);
		if (userHome == null || userHome.city == null){
			throw new RuntimeException("\nHome address should have data at this point!");
		}
		System.out.println("Home address again (expect data): " + userHome.toString());
		System.out.println("Home address DB-ID: " + userHome.dbId);
		System.out.println("Home address tag: " + userHome.userSpecialTag);
		
		//DONE
		System.out.println("\nDONE");
	}
	
	//---------------------------------------------------------------
	
	//init - setup DB with config, load class for interface ...
	public static void setup(){
		//deactivate white-list emails
		Config.restrictRegistration = false;
		
		//load custom config
		Start.loadSettings(new String[]{"--test"});
		Config.setupDatabases();
		
		//load modules
		auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
		acc = (AccountInterface) ClassBuilder.construct(Config.accountModule);
	}
	
	public static void createUser(String email, String pwd){
		String pass = Security.hashClientPassword(pwd);
		//String idType = ID.Type.email;

		JSONObject requestRes = auth.registrationByEmail(email);
		//System.out.println(requestRes);
		if (!requestRes.containsKey("token")){
			throw new RuntimeException("Registration request failed with code: " + auth.getErrorCode());
		}
		
		JSON.put(requestRes, "pwd", pass);
		if (!auth.createUser(requestRes)){
			throw new RuntimeException("Creating user failed with code: " + auth.getErrorCode());
		}
	}
	
	public static void changePassword(String email, String newPwd){
		String idType = ID.Type.email;
		String newPass = Security.hashClientPassword(newPwd);

		JSONObject requestRes = auth.requestPasswordChange(JSON.make(
			"userid", email, 
			"type", idType
		));
		//System.out.println(requestRes);
		if (!requestRes.containsKey("token")){
			throw new RuntimeException("Password reset request failed with code: " + auth.getErrorCode());
		}
		
		JSON.put(requestRes, "userid", email);		
		JSON.add(requestRes, "new_pwd", newPass);
		JSON.add(requestRes, "type", idType);	
		
		if (!auth.changePassword(requestRes)){
			throw new RuntimeException("Password reset failed with code: " + auth.getErrorCode());
		}
	}
	
	public static String testUserExistenceByEmail(String email){
		String idType = ID.Type.email;
		String guid = auth.userExists(email, idType);
		if (guid.isEmpty()){
			throw new RuntimeException("User should exist but does not: " + email);
		}
		return guid;
	}
	
	public static boolean authentication(String userid, String pwd, String idType){
		String pass = Security.hashClientPassword(pwd);
		
		JSONObject info = new JSONObject();
			JSON.add(info, "userId", userid);
			JSON.add(info, "pwd", pass);
			JSON.add(info, "idType", idType);
			JSON.add(info, "client", Config.defaultClientInfo);

		boolean success = auth.authenticate(info);
		String userID = auth.getUserID();
		int accessLvl = auth.getAccessLevel();
		HashMap<String, Object>	basicInfo = auth.getBasicInfo();
		int	errorCode = auth.getErrorCode(); 
		
		if (errorCode != 0){
			throw new RuntimeException("Authenticating user failed with code: " + errorCode);
		}
		System.out.println("id: " + userID);
		System.out.println("accessLvl: " + accessLvl);
		System.out.print("basicInfo: ");
		if (basicInfo != null) Debugger.printMap(basicInfo); else System.out.println("null");
		
		return success;
	}
	
	public static Authenticator getAuthToken(String guuid, String pwd, String loginToken, String client){
		String pass;
		if (loginToken == null || loginToken.isEmpty()){
			pass = Security.hashClientPassword(pwd);
		}else{
			pass = loginToken;
		}
		//note: request=null we assume it is not needed here and skip the fake request part ;-)
		return new Authenticator(guuid, pass, ID.Type.uid, client, null);
	}
	
	//--------------------------------------------------------
	
	

}
