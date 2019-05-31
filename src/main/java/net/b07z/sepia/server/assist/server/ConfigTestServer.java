package net.b07z.sepia.server.assist.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Is;
import spark.Request;

/**
 * Use this to set up the test configuration.
 * 
 * @author Florian Quirin
 *
 */
public class ConfigTestServer {
	
	//NOTE: see test data further down please ...
	
	/**
	 * Load answers and parameter configurations. Useful for service testing.
	 */
	public static void loadAnswersAndParameters(){
		Config.setupAnswers();
		Start.setupServicesAndParameters();
	}
	
	/**
	 * Will set some objects so that data is not loaded from database, 
	 * e.g.: custom command mapping buffers for users etc..<br>
	 * Note: If you need this DB data obviously you should not use this here .. ;-)
	 */
	public static void reduceDatabaseAccess(String userId){
		ConfigServices.assistantCustomCommandsMap = new ArrayList<>();
		ConfigServices.userCustomCommandsMaps.put(userId, new ArrayList<>());
	}

	/**
	 * Check if "skipAuth" is submitted as parameter and if the ID is allowed to skip database authentication.
	 * If all is clear check locally if the password is correct and create a test token otherwise the result is null.<br>
	 * Note: works only with test-server.   
	 */
	public static Authenticator getTestToken(Request request){
		String skip = request.queryParams("skipAuth");
		boolean skipAuth = Boolean.parseBoolean(skip);
		Authenticator token = null;
		if (skipAuth){
			String keyInput = request.queryParams("KEY");
			String[] info = keyInput.split(";",2);
			String username = info[0].toLowerCase();
			String password = info[1];
			//we need email here:
			if (username.startsWith(Config.userIdPrefix)){
				username = testAccountEmails.get(username);
			}
			if (testAccountIDs.containsKey(username) && key.equals(password)){
				token = getTestToken(username, false);
			}
		}
		return token;
	}
	
	//---------------------------- TEST DATA CREATION --------------------------------
	
	public static String email_id1 = "test@example.com"; 		//TODO: fix
	public static String email_id2 = "test2@example.com";		//TODO: fix
	public static User user1, user2;
	public static String key = "e4d8caa2c57116103c0517f66ce535f9d28e8345b23662dcd08b6be958e476a0"; 		//TODO: fix
	public static HashMap<String, String> testAccountIDs;
	public static HashMap<String, String> testAccountEmails;
	static {
		testAccountIDs = new HashMap<>();
		testAccountIDs.put(email_id1, Config.userIdPrefix + "101");		//TODO: fix
		testAccountIDs.put(email_id2, Config.userIdPrefix + "102");		//TODO: fix
	}
	static {
		testAccountEmails = new HashMap<>();
		testAccountEmails.put(Config.userIdPrefix + "101", email_id1);	//TODO: fix
		testAccountEmails.put(Config.userIdPrefix + "102", email_id2);	//TODO: fix
	}
	
	/**
	 * Make a fake input with current local time, a defined user_location and some other default parameters. 
	 */
	public static NluInput getFakeInput(String text, String language){
		String context = "default";
		int mood = 5;
		String environment = "web_app";
		NluInput input = new NluInput(text, language, context, mood, environment);
		input.userLocation = LOCATION.makeLocation("Germany", "NRW", "Munich", "80331", "Platzl", "9", "48.137", "11.580").toJSONString();
		//input.user_location = "<city>Berlin City<latitude>52.518616<longitude>13.404636";
		input.userTime = System.currentTimeMillis();
		input.userTimeLocal = DateTime.getFormattedDate(Config.defaultSdf);
		User user = getTestUser(ConfigTestServer.email_id1, input, false, false);
		input.user = user;
		return input;
	}
	public static String getFakeUserId(String email){
		if (Is.notNullOrEmpty(email)){
			return testAccountIDs.get(email);
		}else{
			return testAccountIDs.get(ConfigTestServer.email_id1);
		}
	}
	
	/**
	 * Make a test token with "real" or "fake" database data. Use one of the test-email IDs to create it.
	 */
	public static Authenticator getTestToken(String email, boolean useRealData){
		Authenticator test_token;
		if (useRealData){
			Request req = new FakeRequest();
			test_token = new Authenticator(email, key, ID.Type.email, Config.defaultClientInfo, req);
		}else{
			test_token = new Authenticator(testAccountIDs.get(email), null);
		}
		return test_token;
	}
	
	/**
	 * Make a test user with "fake" input and "real" or "fake" database data (useRealData). Use one of the test-email IDs to create it.
	 * Note: if the user was already created before the "input" will be ignored unless you set "forceNew=true".
	 */
	public static User getTestUser(String email, NluInput input, boolean forceNew, boolean useRealData){
		User user;
		//avoid multiple authentication processes
		if (email.equals(email_id1) && user1 != null && !forceNew){
			user = user1;
		}else if (email.equals(email_id2) && user2 != null && !forceNew){
			user = user2;
		}else{
			Authenticator test_token = getTestToken(email, useRealData);
			user = new User(input, test_token);
		}
		//save for next use
		if (email.equals(email_id1)){
			user1 = user;
		}else if (email.equals(email_id2)){
			user2 = user;
		}
		input.user = user;
		return user;
	}
	
	//-----------FAKE REQUEST-----------
	
	/**
	 * Fake server request for testing purposes.
	 *
	 */
	private static class FakeRequest extends Request {
		private final Map<String, String> params = new HashMap<>();
		
		FakeRequest(String... params) {
			for (String param : params) {
				String[] parts = param.split("=",2);
				this.params.put(parts[0], parts[1]);
			}
		}

		@Override
		public String headers(String header) {
			if (header.equalsIgnoreCase("Content-type")) {
				return "application/json";
			}
			return super.headers(header);
		}
		
		@Override
		public String contentType() {
			return "application/json";
		}

		@Override
		public String queryParams(String queryParam) {
			return params.get(queryParam);
		}

		@Override
		public String[] queryParamsValues(String queryParam) {
			return new String[]{params.get(queryParam)};
		}
	}
}
