package net.b07z.sepia.server.assist.server;

import static spark.Spark.*;

import java.security.Policy;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.endpoints.AccountEndpoint;
import net.b07z.sepia.server.assist.endpoints.AssistEndpoint;
import net.b07z.sepia.server.assist.endpoints.AuthEndpoint;
import net.b07z.sepia.server.assist.endpoints.ConfigServer;
import net.b07z.sepia.server.assist.endpoints.IntegrationsEndpoint;
import net.b07z.sepia.server.assist.endpoints.RemoteActionEndpoint;
import net.b07z.sepia.server.assist.endpoints.SdkEndpoint;
import net.b07z.sepia.server.assist.endpoints.TtsEndpoint;
import net.b07z.sepia.server.assist.endpoints.UserDataEndpoint;
import net.b07z.sepia.server.assist.endpoints.UserManagementEndpoint;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.parameters.ParameterConfig;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.workers.ServiceBackgroundTaskManager;
import net.b07z.sepia.server.assist.workers.Workers;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.endpoints.CoreEndpoints;
import net.b07z.sepia.server.core.server.ConfigDefaults;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.server.Validate;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.SandboxSecurityPolicy;
import net.b07z.sepia.server.core.tools.Security;
import spark.Request;
import spark.Response;

/**
 * Main class that starts the server.
 * 
 * @author Florian Quirin
 *
 */
public class Start {

	//stuff
	public static String startGMT = "";
	public static long lastStartUNIX = 0l;
	public static String serverType = "";
	public static String[] startUpArgs = null;
	
	public static final String LIVE_SERVER = "live";
	public static final String TEST_SERVER = "test";
	public static final String CUSTOM_SERVER = "custom";
	
	public static boolean isSSL = false;
	public static boolean useSecurityPolicy = true;
	public static boolean useSandboxBlacklist = true;
	public static String keystorePwd = "13371337";
	
	/**
	 * Load configuration file.
	 * @param serverType - live, test, custom
	 * @throws Exception  
	 */
	public static void loadConfigFile(String serverType) throws Exception {
		if (serverType.equals(TEST_SERVER)){
			Config.configFile = "Xtensions/assist.test.properties";
		}else if (serverType.equals(CUSTOM_SERVER)){
			Config.configFile = "Xtensions/assist.custom.properties";
		}else if (serverType.equals(LIVE_SERVER)){
			Config.configFile = "Xtensions/assist.properties";
		}else{
			throw new RuntimeException("INVALID SERVER TYPE: " + serverType);
		}
		Config.loadSettings(Config.configFile);
	}
	
	/**
	 * Check arguments and load settings correspondingly.
	 * @param args - parameters submitted to main method
	 * @throws Exception  
	 */
	public static void loadSettings(String[] args) throws Exception {
		//check arguments
		serverType = TEST_SERVER;
		for (String arg : args){
			if (arg.equals("setup")){
				//Start setup
				try {
					Setup.main(args);
					System.exit(0);
					return;
				}catch (Exception e){
					System.err.println("Setup ERROR: " + e.toString());
					System.exit(1);
					return;
				}
			}else if (arg.equals("--test")){
				//Test system
				serverType = TEST_SERVER;
			}else if (arg.equals("--live")){
				//Live system
				serverType = LIVE_SERVER;
			}else if (arg.equals("--my") || arg.equals("--custom")){
				//Custom system
				serverType = CUSTOM_SERVER;
			}else if (arg.equals("--ssl")){
				//SSL
				isSSL = true;
			}else if (arg.equals("--nosecuritypolicy")){
				//No security manager will be set
				useSecurityPolicy = false;
			}else if (arg.equals("--nosandbox")){
				//Sandbox blacklist will be empty
				useSandboxBlacklist = false;
			}else if (arg.startsWith("keystorePwd=")){
				//Java key-store password - TODO: maybe not the best way to load the pwd ...
				keystorePwd = arg.replaceFirst(".*?=", "").trim();
			}
		}
		//set security
		if (useSecurityPolicy){
			Policy.setPolicy(new SandboxSecurityPolicy());
			System.setSecurityManager(new SecurityManager());
			Debugger.println("Security policy and manager set.", 3);
			if (useSandboxBlacklist){
				ConfigServices.setupSandbox();
			}else{
				Debugger.println("NO SANDBOX BLACKLIST LOADED.", 3);
			}
		}else{
			Debugger.println("NO SECURITY POLICY ACTIVE.", 3);
		}
		if (isSSL){
			secure(Config.xtensionsFolder + "SSL/ssl-keystore.jks", keystorePwd, null, null);
		}
		Debugger.println("JAVA_HOME: " + System.getProperty("java.home"), 3);
		
		//load configuration
		loadConfigFile(serverType);
		Debugger.println("--- Running " + Config.SERVERNAME + " with " + serverType.toUpperCase() + " settings ---", 3);
		
		//host files?
		if (Config.hostFiles){
			//TODO: use custom 'StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();' instead?
			staticFiles.externalLocation(Config.webServerFolder);
			Debugger.println("Web-server is active and uses folder: " + Config.webServerFolder 
					+ " - CORS (files): " + (Config.enableFileCORS? "*" : "same page"), 3);
			if (Is.notNullOrEmpty(Config.fileMimeTypes)){
				for (String ft : Config.fileMimeTypes.split(",")){
					String[] fileAndType = ft.split("=", 2);
					String f = fileAndType[0].trim();
					String t = fileAndType[1].trim();
					staticFiles.registerMimeType(f, t);
					Debugger.println("Web-server MIME type overwrite: " + f + "=" + t, 3);
				}
			}
		}
		
		//SETUP CORE-TOOLS
		JSONObject coreToolsConfig = JSON.make(
				"defaultAssistAPI", Config.endpointUrl,
				"defaultTeachAPI", Config.teachApiUrl,
				"clusterKey", Config.clusterKey,
				"defaultAssistantUserId", Config.assistantId,
				"privacyPolicy", Config.privacyPolicyLink
		);
		//common micro-services API-Keys
		//TODO: replace or remove!
		JSON.put(coreToolsConfig, "DeutscheBahnOpenApiKey", Config.deutscheBahnOpenApi_key);
		ConfigDefaults.setupCoreTools(coreToolsConfig);
		
		//Check core-tools settings
		if (!ConfigDefaults.areCoreToolsSet()){
			throw new RuntimeException("Core-tools are NOT set properly!");
		}
	}
	
	/**
	 * Setup server with port, cors, error-handling etc.. 
	 */
	public static void setupServer(){
		//start by getting GMT date
		Date date = new Date();
		startGMT = DateTime.getGMT(date, "dd.MM.yyyy' - 'HH:mm:ss' - GMT'");
		Debugger.println("Starting Assistant-API server " + Config.apiVersion + " (" + serverType + ")", 3);
		Debugger.println("date: " + startGMT, 3);
		
		//email warnings?
		if (!Config.emailBCC.isEmpty()){
			Debugger.println("WARNING: Emails are sent to " + Config.emailBCC + " for debugging issues!", 3);
		}
				
		/*
		//TODO: do we need to set this? https://wiki.eclipse.org/Jetty/Howto/High_Load
		int maxThreads = 8;
		int minThreads = 2;
		int timeOutMillis = 30000;
		threadPool(maxThreads, minThreads, timeOutMillis)
		 */
		
		try {
			port(Integer.valueOf(System.getenv("PORT")));
			Debugger.println("server running on port: " + Integer.valueOf(System.getenv("PORT")), 3);
		}catch (Exception e){
			int port = Config.serverPort; 	//default is 20721
			port(port);
			Debugger.println("server running on port "+ port, 3);
		}
		
		//set access-control headers to enable CORS
		if (Config.enableCORS){
			SparkJavaFw.enableCORS("*", "*", "*");
		}
		if (Config.enableFileCORS){
			SparkJavaFw.enableFileCORS("*", "*", "*");
		}

		//do something before end-point evaluation - e.g. authentication
		before((request, response) -> {
			//System.out.println("BEFORE TEST 1"); 		//DEBUG
			//System.out.println("request: " + request.pathInfo()); 		//DEBUG
		});
		
		//ERROR handling - TODO: improve
		SparkJavaFw.handleError();
	}
	
	/**
	 * Setup services and parameters by connecting commands to service modules etc.
	 */
	public static void setupServicesAndParameters(){
		InterviewServicesMap.load();		//services connected to interviews
		InterviewServicesMap.test();		//test if all services can be loaded
		ParameterConfig.setup(); 			//connect parameter names to handlers and other stuff
		ParameterConfig.test();				//test if all parameters can be loaded
	}
	
	/**
	 * All kinds of things that should be loaded on startup.
	 */
	public static void setupModules(){
		Config.setupDatabases(true);	//DB modules
		Config.setupAnswers();			//answers
		Config.setupCommands();			//predefined commands
		Config.setupChats(); 			//predefined chats
		Config.setupNluSteps(); 		//interpretation chain
		Config.setupTools(); 			//tools like RssFeedReader or SpotifyApi
		if (Config.ttsModuleEnabled){
			Config.setupTts();				//TTS module setup (e.g. clean local folders etc.)
		}
		Workers.setupWorkers(); 		//setup and start selected workers
		if (Config.connectToWebSocket){
			Clients.setupSocketMessenger();		//setup webSocket messenger and connect
		}
	}
	
	/**
	 * Check existence of universal accounts (superuser and assistant).
	 */
	public static void checkCoreAccounts(){
		if (!Config.validateUniversalToken()){
			Debugger.println("Server token not valid!", 1);
			Debugger.println("Administrator account could not be validated, CANNOT proceed! Please check database access and accounts.", 1);
			System.exit(0);
			//throw new RuntimeException("Administrator account could not be validated, CANNOT proceed! Please check database access and accounts.");
		}else{
			Debugger.println("Server token validated", 3);
		}
		if (!Config.validateAssistantToken()){
			Debugger.println("Assistant token not valid!", 1);
			Debugger.println("Assistant account could not be validated, CANNOT proceed! Please check database access and accounts.", 1);
			System.exit(0);
			//throw new RuntimeException("Assistant account could not be validated, CANNOT proceed! Please check database access and accounts.");
		}else{
			Debugger.println("Assistant token validated", 3);
		}
	}
	/**
	 * Setup accounts that allow only very limited number of failed logins until blocked for a while.
	 */
	public static void setupProtectedAccounts(){
		Authenticator.addProtectedAccount(Config.superuserId, Config.superuserEmail);
		Authenticator.addProtectedAccount(Config.assistantId, Config.assistantEmail);
		Debugger.println("Added 'admin' and 'assistant' to protected accounts list.", 3);
		if (!Config.protectedAccounts.isEmpty()){
			for (Entry<String, String> e : Config.protectedAccounts.entrySet()){
				String uid = e.getKey();
				String email = e.getValue();
				if (uid.startsWith(Config.userIdPrefix) && email.contains("@")){
					Authenticator.addProtectedAccount(e.getKey(), e.getValue());
				}else{
					throw new RuntimeException("Found INVALID format in protected accounts list for: " + uid + " - " + email);
				}
			}
			Debugger.println("Added few more accounts to protected list: " + Config.protectedAccounts.keySet().toString(), 3);
		}
	}
	
	/**
	 * Defines the end-points that this server supports.
	 */
	public static void loadEndpoints(){
		//Server
		get("/online", (request, response) -> 				CoreEndpoints.onlineCheck(request, response));
		get("/ping", (request, response) -> 				CoreEndpoints.ping(request, response, Config.SERVERNAME));
		get("/validate", (request, response) -> 			CoreEndpoints.validateServer(request, response, 
																Config.SERVERNAME, Config.apiVersion, Config.localName, Config.localSecret));
		post("/hello", (request, response) -> 				helloWorld(request, response));
		post("/cluster", (request, response) ->				clusterData(request, response));
		post("/config", (request, response) -> 				ConfigServer.run(request, response));
		
		//Web-server content
		get("/web-content-index/*", (request, response) ->	CoreEndpoints.getWebContentIndex(request, response, 
																Config.webServerFolder, "/web-content-index/", Config.allowFileIndex));
		
		//Accounts and assistant
		post("/user-management", (request, response) ->		UserManagementEndpoint.userManagementAPI(request, response));
		post("/authentication", (request, response) -> 		AuthEndpoint.authenticationAPI(request, response));
		post("/account", (request, response) ->				AccountEndpoint.accountAPI(request, response));
		post("/userdata", (request, response) ->			UserDataEndpoint.userdataAPI(request, response));
		post("/interpret", (request, response) -> 			AssistEndpoint.interpreterAPI(request, response));
		post("/understand", (request, response) ->			AssistEndpoint.interpreterV2(request, response));
		post("/interview", (request, response) ->			AssistEndpoint.interview(request, response));
		post("/answer", (request, response) -> 				AssistEndpoint.answerAPI(request, response));
		post("/events", (request, response) -> 				AssistEndpoint.events(request, response));
		
		//TTS
		post("/tts", (request, response) -> 				TtsEndpoint.ttsAPI(request, response));
		get("/tts-stream/:file", (request, response) ->		TtsEndpoint.ttsStream(request, response));
		post("/tts-info", (request, response) -> 			TtsEndpoint.ttsInfo(request, response));
		
		//SDK
		get("/upload-service", (request, response) -> 		SdkEndpoint.uploadServiceGet(request, response));
		post("/upload-service", (request, response) -> 		SdkEndpoint.uploadServicePost(request, response));
		post("/get-services", (request, response) -> 		SdkEndpoint.getServicesPost(request, response));
		post("/get-service-source", (request, response) -> 	SdkEndpoint.getServiceSourceCodePost(request, response));
		post("/delete-service", (request, response) -> 		SdkEndpoint.deleteServicePost(request, response));
		
		//Remote controls
		post("/remote-action", (request, response) ->		RemoteActionEndpoint.remoteActionAPI(request, response));
		
		//Integrations
		post("/integrations/*/*", (request, response) ->	IntegrationsEndpoint.handle(request, response));
	}
	
	/**
	 * Stuff to add to the default statistics output (e.g. from end-point hello).
	 */
	public static String addToStatistics(){
		//add stuff here
		return "";
	}
	
	/**
	 * Load updates to the framework that are placed here to maintain compatibility with projects that use SEPIA.<br>
	 * Stuff in here should be moved to a proper place as soon as all developers have been informed of the changes.
	 */
	public static void loadUpdates(){
		//add stuff here
	}
	
	/**
	 * MAIN METHOD TO START SERVER
	 * @throws Exception 
	 */
	public static void main(String[] args) {
		//activation timestamp
		lastStartUNIX = System.currentTimeMillis();
		
		System.out.println("-----------------------------------------");
		System.out.println("      ___    ___   ___   ___   ___       ");
		System.out.println("     |___   |___  |___]   |   |___|      ");
		System.out.println("     ____|. |___. |    . _|_. |   |.     ");
		System.out.println("                                         ");
		System.out.println("    https://sepia-framework.github.io    ");
		System.out.println("                                         ");
		
		//load settings
		try{
			loadSettings(args);
		}catch (Exception e1){
			e1.printStackTrace();
			System.exit(1);
		}
		
		//test database(s)
		try{
			Config.testDatabases();
		}catch (Exception e){
			Debugger.printStackTrace(e, 3);
			return;
		}
		
		//load statics and workers and setup modules (loading stuff to memory etc.)
		setupModules();
		
		//setup services and parameters by connecting commands etc.
		setupServicesAndParameters();
		
		//check existence of universal accounts (superuser and assistant)
		checkCoreAccounts();
		
		//load protected accounts (e.g. admin and assistant)
		setupProtectedAccounts();
		
		//load updates to the framework that have no specific place yet
		loadUpdates();
		
		//setup server with port, cors and error handling etc. 
		setupServer();
		
		//SERVER END-POINTS
		loadEndpoints();
		
		//remember arguments for restart
		startUpArgs = args;
	}
	
	/**
	 * Stop server and wait for finish signal.
	 */
	public static void stopServer() {
		Debugger.println("Stopping server ...", 3);
		
		//Stop running workers and service-background-tasks
		Workers.stopWorkers();
		ServiceBackgroundTaskManager.cancelAllScheduledTasks();
		
		//Disconnect websocket
		if (Config.connectToWebSocket){
			Clients.killSocketMessenger();
		}
		
		//Stop server and wait
		stop();
		awaitStop();
		Debugger.println("------ SERVER STOPPED ------", 3);
	}
	
	/**
	 * Soft-restart of server using a {@link TimerTask} and a certain delay.<br>
	 * Note: You should probably not rely on this method as your only way of restarting the server, but use a hard reset from time to time.
	 * It tries to clean up workers and clients properly when shutting down but will not reset all static variables!
	 * @return true if restart was scheduled 
	 */
	public static boolean restartServer(long delayMs) {
		if (startUpArgs == null){
			return false;
		}else{
			new Timer().schedule(new TimerTask() {
	            @Override
	            public void run() {
	            	stopServer();
	                main(startUpArgs);
	            }
	        }, delayMs);
			return true;
		}
	}
	
	/**
	 * ---HELLO WORLD---<br>
	 * End-point to get statistics of the server.
	 */
	public static String helloWorld(Request request, Response response){
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request); 	//TODO: because of form-post?	
		
		//authenticate
		Authenticator token = authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}else{
			//create user
			User user = new User(null, token);
			//write basic statistics for user
			user.saveStatistics();
			
			//time now
			Date date = new Date();
			String nowGMT = DateTime.getGMT(date, "dd.MM.yyyy' - 'HH:mm:ss' - GMT'");
			String nowLocal = DateTimeConverters.getToday("dd.MM.yyyy' - 'HH:mm:ss' - LOCAL'", params.getString("time_local"));
			
			//get user role
			String reply;
			if (user.hasRole(Role.developer)){
				//stats
				reply = "Hello World!"
						+ "<br><br>"
						+ "Stats:<br>" +
								"<br>api: " + Config.apiVersion +
								"<br>started: " + startGMT +
								"<br>now: " + nowGMT + 
								"<br>local: " + nowLocal + "<br>" +
								"<br>host: " + request.host() +
								"<br>url: " + request.url() + "<br><br>" +
								Statistics.getInfo();
			}else{
				reply = "Hello World!";
			}
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "reply", reply);
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}
	}
	
	/**
	 * ---ASSIST API CLUSTER DATA---<br>
	 * End-point to get some cluster-relevant data of the server.
	 */
	public static String clusterData(Request request, Response response){
		//NOTE: we use cluster-key authentication here 
		//and we DON'T check 'Config.allowInternalCalls' (since this is required data)
		
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);		//TODO: because of form-post?
		
		//authenticate 
		if (Validate.validateInternalCall(request, params.getString("sKey"), Config.clusterKey)){
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "serverName", Config.SERVERNAME);
			JSON.add(msg, "serverId", Config.localName);
			JSON.add(msg, "assistantUserId", Config.assistantId);
			JSON.add(msg, "assistantName", Config.assistantName);
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}else{
			return SparkJavaFw.returnNoAccess(request, response);
		}
	}
	
	//------- common authentication methods -------
	
	/**
	 * Authenticate the user.
	 * @param request - the request (aka url parameters) sent to server.
	 * @return true or false
	 */
	private static Authenticator authenticate(String key, String client, Request metaInfo){
		//System.out.println("Client: " + client); 		//DEBUG
		if (key != null && !key.isEmpty()){
			String[] info = key.split(";",2);
			if (info.length == 2){
				String username = info[0].toLowerCase();
				String password = info[1];
				//password must be 64 or 65 char hashed version - THE CLIENT IS EXPECTED TO DO THAT!
				//65 char is the temporary token
				if ((password.length() == 64) || (password.length() == 65)){
					String idType = ID.autodetectType(username);
					if (idType.isEmpty()){
						return new Authenticator();
					}
					Authenticator token = new Authenticator(username, password, idType, client, metaInfo);
					return token;
				
				//some different auth. procedure?
				}else if (password.length() > 32){
					Authenticator token = new Authenticator(username, password, ID.Type.uid, client, metaInfo);
					return token;
				}
				
			}
		}
		Authenticator token = new Authenticator();
		return token;
	}
	public static Authenticator authenticate(Request request){
		return authenticate(new RequestGetOrFormParameters(request), request);
	}
	protected static Authenticator authenticate(Request request, boolean isFormData){
		if (isFormData){
			return authenticate(new RequestGetOrFormParameters(request), request);
		}else{
			return authenticate(new RequestPostParameters(request), request);
		}
	}
	public static Authenticator authenticate(RequestParameters params, Request metaInfo){
		String key = params.getString(AuthEndpoint.InputParameters.KEY.name());
		if (key == null || key.isEmpty()){
			String guuid = params.getString(AuthEndpoint.InputParameters.GUUID.name());
			String pwd = params.getString(AuthEndpoint.InputParameters.PWD.name());
			if (guuid != null && pwd != null && !guuid.isEmpty() && !pwd.isEmpty()){
				key = guuid + ";" + Security.hashClientPassword(pwd);
			} 
		}
		String client_info = params.getString(AuthEndpoint.InputParameters.client.name());
		if (client_info == null || client_info.isEmpty()){
			client_info = Config.defaultClientInfo;
		}
		return authenticate(key, client_info, metaInfo);
	}
}
