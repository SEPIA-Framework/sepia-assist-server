package net.b07z.sepia.server.assist.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.b07z.sepia.server.assist.answers.AnswerLoaderElasticsearch;
import net.b07z.sepia.server.assist.answers.AnswerLoader;
import net.b07z.sepia.server.assist.answers.AnswerLoaderFile;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.database.ConfigDynamoDB;
import net.b07z.sepia.server.assist.database.ConfigElasticSearch;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.DataLoader;
import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.assist.email.SendEmailBasicSmtp;
import net.b07z.sepia.server.assist.interpreters.InterpretationChain;
import net.b07z.sepia.server.assist.interpreters.InterpretationStep;
import net.b07z.sepia.server.assist.interpreters.NluInterface;
import net.b07z.sepia.server.assist.interpreters.NluApproximateMatcher;
import net.b07z.sepia.server.assist.interpreters.NluKeywordAnalyzer;
import net.b07z.sepia.server.assist.interpreters.NluKeywordAnalyzerDE;
import net.b07z.sepia.server.assist.interpreters.NluKeywordAnalyzerEN;
import net.b07z.sepia.server.assist.interpreters.NluSentenceMatcher;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interpreters.NormalizerLight;
import net.b07z.sepia.server.assist.interpreters.NormalizerLightDE;
import net.b07z.sepia.server.assist.interpreters.NormalizerLightEN;
import net.b07z.sepia.server.assist.interpreters.NormalizerLightTR;
import net.b07z.sepia.server.assist.services.ServiceAccessManager;
import net.b07z.sepia.server.assist.smarthome.OpenHAB;
import net.b07z.sepia.server.assist.smarthome.SmartDevicesElasticsearch;
import net.b07z.sepia.server.assist.tools.RssFeedReader;
import net.b07z.sepia.server.assist.tools.SpotifyApi;
import net.b07z.sepia.server.assist.tts.TtsInterface;
import net.b07z.sepia.server.assist.tts.TtsOpenEmbedded;
import net.b07z.sepia.server.assist.users.AccountDynamoDB;
import net.b07z.sepia.server.assist.users.AccountElasticsearch;
import net.b07z.sepia.server.assist.users.AuthenticationDynamoDB;
import net.b07z.sepia.server.assist.users.AuthenticationElasticsearch;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataDefault;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.server.ConfigDefaults;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.Is;

/**
 * Server configuration class.
 * 
 * @author Florian Quirin
 *
 */
public class Config {
	public static final String SERVERNAME = "SEPIA-Assist-API"; 		//public server name
	public static final String apiVersion = "v2.5.2";					//API version
	public static String privacyPolicyLink = "";						//Link to privacy policy
	
	//helper for dynamic class creation (e.g. from strings in config-file) - TODO: reduce dependencies further 
	public static final String parentPackage = Config.class.getPackage().getName().substring(0, Config.class.getPackage().getName().lastIndexOf('.'));
	
	//Server settings (port, web-server, folders etc.)
	public static String configFile = "Xtensions/assist.properties";		//external configuration file - note: this will be overwritten in "Setup" and "Start"
	public static String xtensionsFolder = "Xtensions/";					//folder for all sorts of data
	public static String pluginsFolder = xtensionsFolder + "Plugins/";		//folder for plugins
	public static String sdkClassesFolder = pluginsFolder + "net/b07z/sepia/sdk/";		//folder for SDK plugin classes - NOTE: has to be sub-folder of plugins
	public static String servicePropertiesFolder = xtensionsFolder + "ServiceProperties/";		//folder for service properties and static data
	public static String dbSetupFolder = xtensionsFolder + "Database/";		//folder for database stuff
	public static String webServerFolder = xtensionsFolder + "WebContent";	//folder for web-server (NOTE: it's given without '/' at the end)
	public static String ttsEngines = xtensionsFolder + "TTS/";				//folder for TTS engines if not given by system
	public static String ttsWebServerUrl = "/tts/";							//URL for TTS when accessing web-server root
	public static String ttsWebServerPath = webServerFolder + ttsWebServerUrl;		//folder for TTS generated on server
	public static boolean ttsModuleEnabled = true;									//is TTS module available (can be set to false by module setup)
	public static boolean hostFiles = true;									//use web-server?
	public static boolean allowFileIndex = true;							//allow web-server index
	public static String fileMimeTypes = "mp4=video/mp4, mp3=audio/mpeg";	//MIME Types for files when loaded from static web server
	public static String localName = "sepia-assist-server-1";				//**user defined local server name - should be unique inside cluster because it might be used as serverId
	public static String localSecret = "123456";							//**user defined secret to validate local server
	public static int serverPort = 20721;									//**server port
	public static boolean enableCORS = true;								//enable CORS (set access-control headers)
	public static boolean enableFileCORS = true;							//enable CORS for static files (hostFiles = true)
	public static String clusterKey = "KantbyW3YLh8jTQPs5uzt2SzbmXZyphW";	//**one step of inter-API communication security
	public static String clusterKeyLight = "KantbyW3YLh8jTQPs";				//used as private secret for lower priority/more risky tasks to keep full key safe
	public static int cklHashIterations = ((int) clusterKeyLight.charAt(clusterKeyLight.length()-1)) + 5;		//well defined but "random" hash iterations due to random key
	public static boolean allowInternalCalls = true;			//**allow API-to-API authentication via cluster-key
	public static boolean allowGlobalDevRequests = false;		//**restrict certain developer-specific requests to private network
	public static boolean improveSecurityForProtectedAccounts = true;		//apply 'number of failed login attempts' check to protected accounts
	public static long protectedAccountsBlockTimeout = 30000;				//blocked login timeout due to 'too many failed attempts'
	public static Map<String, String> protectedAccounts = new HashMap<>();	//a map with UID, EMAIL pairs of "a few" more protected accounts (in addition to admin and assistant)
	
	//test and other configurations
	public static boolean restrictRegistration = true; 			//check new registrations against white-list?
	public static boolean enableSDK = false;					//enable or disable SDK uploads
	//public static boolean useSandboxPolicy = true;				//use security sandbox for server (should always be true in production systems! Can only be set via commandline argument)
	public static boolean connectToWebSocket = true;					//**connect assistant to WebSocket chat-server?
	public static boolean collectGeoData = true;						//save anonymous geo-data on every API call?
	public static boolean useSentencesDB = true;						//use sentences in the database to try to match user input
	public static List<String> backgroundWorkers = new ArrayList<>(); 	//workers to activate on server-start
	public static boolean cacheRssResults = true;						//cache results for RSS worker
	public static boolean checkElasticsearchMappingsOnStart = true;		//test Elasticsearch mappings
	
	//General and assistant settings
	public static String assistantName = "Sepia";				//**Name - this might become user-specific once ...
	public static boolean assistantAllowFollowUps = true;		//**allow the assistant to send follow-up messages to requests (plz don't spam the user!)
	public static String defaultClientInfo = ConfigDefaults.defaultClientInfo;	//in case the client does not submit the info use this.
	
	public static String userIdPrefix = "uid";					//**prefix used when generating for example user ids or checking them
	public static long guidOffset = 1000;						//**offset used for global unique IDs (e.g. user and tickets)
	
	public static String defaultSdf = "yyyy.MM.dd_HH:mm:ss";			//default date format expected at client submission
	public static String defaultSdfRegex = "\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d_\\d\\d:\\d\\d:\\d\\d";		//regex matching expected date
	public static String defaultSdfSeparator = "_";						//date and time separator
	public static String defaultSdfSeparatorRegex = "_";				//regex to split date and time
	
	public static String answersPath = xtensionsFolder + "Assistant/answers/";		//where to find the answers text files ...
	public static String commandsPath = xtensionsFolder + "Assistant/commands/";	//where to find predefined sentences/commands ...
	
	//URLs
	public static String endpointUrl = "http://localhost:20721/";			//**this API URL
	public static String teachApiUrl = "http://localhost:20722/";			//**teach API URL
	public static String urlCreateUser = "/#!/create-user"; 					//**Create user page
	public static String urlChangePassword = "/#!/reset-password"; 				//**Change password
	public static String urlWebImages = "http://localhost:20721/files/images/";	//**Graphics than can be used in plug-ins
	public static String urlWebFiles = "http://localhost:20721/files/";			//**Files that can be used
	public static String urlDashboard = "/#!/dashboard";						//**User dashboard page
	
	//**Email - default SMTP with authentication (port 25)
	public static boolean redirectEmail = false; 				//set this to true for email message testing
	public static String emailHost = "smtp...com";
	public static String emailAccount = "account@...com";
	public static String emailAccountKey = "";
	public static String emailBCC = ""; 		//ONLY FOR DEBUGGING!
	
	//Region
	public static final String REGION_US = "us";				//Region tag for US
	public static final String REGION_EU = "eu";				//Region tag for EU
	public static final String REGION_CUSTOM = "custom";		//Region tag for custom or local server
	public static String defaultRegion = REGION_CUSTOM;			//**Region for different cloud services (e.g. AWS)
		
	//Default modules (implementations of certain interfaces) - TODO: add to config file (partially done)
	public static String authAndAccountDB = "elasticsearch";	//overwritten during loading of settings (same for next 2 variables) 																	
	public static String accountModule = AccountElasticsearch.class.getCanonicalName();					
	public static String authenticationModule = AuthenticationElasticsearch.class.getCanonicalName();
	public static String userDataModule = UserDataDefault.class.getCanonicalName();
	public static String knowledgeDbModule = Elasticsearch.class.getCanonicalName();
	public static String answerModule = AnswerLoaderFile.class.getCanonicalName(); 			//TODO: switch to Elasticsearch by default?
	public static String ttsModule = TtsOpenEmbedded.class.getCanonicalName();
	public static String ttsName = "Open Embedded";
	public static String emailModule = SendEmailBasicSmtp.class.getCanonicalName();
	public static String smartDevicesModule = SmartDevicesElasticsearch.class.getCanonicalName();	//TODO: not variable yet
	
	//toggles and switches:
	
	/**
	 * Toggle answer module and return entry value for config file.
	 */
	public static String toggleAnswerModule(){
		String settingsEntry;
		if (answerModule.equals(AnswerLoaderElasticsearch.class.getCanonicalName())){
			answerModule = AnswerLoaderFile.class.getCanonicalName();
			settingsEntry = "file";
		}else{
			answerModule = AnswerLoaderElasticsearch.class.getCanonicalName();
			settingsEntry = "elasticsearch";
		}
		setupAnswers();
		return settingsEntry;
	}
	public static void setAnswerModule(AnswerLoader module){
		answerModule = module.getClass().getCanonicalName();
		setupAnswers();
	}
	
	//Some performance settings (NLU)
	public static int parameterPerformanceMode = 0; 	//0: skip profiled methods for a few seconds if they exceed threshold too often, 1: always run, 2: never run
	
	//Default users and managers
	public static ServiceAccessManager superuserServiceAccMng = new ServiceAccessManager("API_BOSS"); 	//universal API manager for internal procedures
	private static Authenticator superuserToken;
	private static User superUser;
	public static String superuserId = "uid1000";						//**for DB sentences check also Defaults.USER
	public static String superuserEmail = "admin@sepia.localhost";		//**pseudo-email to be blocked for super-user, cannot be created or used for password reset
	private static String superuserPwd = "4be708b703c518d10a97e4db421f0f75b66f9ff8c0ae65b6c5c13684a01f804d";		//**
	private static Authenticator assistantToken;
	private static User assistantUser;
	public static String assistantId = "uid1001";						//**the assistant itself also has an account
	public static String assistantEmail = "assistant@sepia.localhost";	//**pseudo-email to be blocked for assistant, cannot be created or used for password reset
	public static String assistantPwd = "4be708b703c518d10a97e4db421f0f75b66f9ff8c0ae65b6c5c13684a01f804d";				//**
	public static String assistantDeviceId = "serv1";
	public static String assistantClientInfo = assistantDeviceId + "_" + CLIENTS.JAVA_APP + "_" + apiVersion;
	
	public static boolean validateUniversalToken(){
		superuserToken = new Authenticator(superuserId, superuserPwd, ID.Type.uid, defaultClientInfo, null);
		return superuserToken.authenticated();
	}
	public static User getUniversalUser(){
		if (superUser == null){
			return new User(null, superuserToken);
		}else{
			return superUser;
		}
	}
	public static boolean validateAssistantToken(){
		assistantToken = new Authenticator(assistantId, assistantPwd, ID.Type.uid, defaultClientInfo, null);
		return assistantToken.authenticated();
	}
	public static User getAssistantUser(){
		if (assistantUser == null){
			return new User(null, assistantToken);
		}else{
			return assistantUser;
		}
	}
		
	//Languages
	// - array of supported languages - TODO: this needs a deeper integration!
	public static String[] supportedLanguages = {
			LANGUAGES.DE,			LANGUAGES.EN,
			LANGUAGES.TR,			LANGUAGES.SV,
			LANGUAGES.FR,			LANGUAGES.ES,
			LANGUAGES.ZH,			LANGUAGES.AR,
			LANGUAGES.EL,			LANGUAGES.IT,
			LANGUAGES.JA,			LANGUAGES.KO,
			LANGUAGES.NL,			LANGUAGES.PL,
			LANGUAGES.PT,			LANGUAGES.RU
	};
	
	//NLU related configuration
	
	// - input normalizers
	public static HashMap<String, Normalizer> inputNormalizers = new HashMap<String, Normalizer>();
	static
    {
		//use these for more advanced normalizing 
		inputNormalizers.put(LANGUAGES.DE, new NormalizerLightDE());
		inputNormalizers.put(LANGUAGES.EN, new NormalizerLightEN());
		inputNormalizers.put(LANGUAGES.TR, new NormalizerLightTR());
		inputNormalizers.put(LANGUAGES.SV, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.FR, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.ES, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.ZH, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.AR, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.EL, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.IT, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.JA, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.KO, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.NL, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.PL, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.PT, new NormalizerLight());
		inputNormalizers.put(LANGUAGES.RU, new NormalizerLight());
    }
	public static HashMap<String, Normalizer> inputNormalizersLight = new HashMap<String, Normalizer>();
	static
    {
		//use this for save and minimal normalizing
		inputNormalizersLight.put(LANGUAGES.DE, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.EN, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.TR, new NormalizerLightTR());
		inputNormalizersLight.put(LANGUAGES.SV, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.FR, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.ES, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.ZH, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.AR, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.EL, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.IT, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.JA, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.KO, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.NL, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.PL, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.PT, new NormalizerLight());
		inputNormalizersLight.put(LANGUAGES.RU, new NormalizerLight());
    }
	// - keyword analyzers
	public static HashMap<String, String> keywordAnalyzers = new HashMap<String, String>();
	static
    {
		keywordAnalyzers.put(LANGUAGES.DE, NluKeywordAnalyzerDE.class.getCanonicalName());
		keywordAnalyzers.put(LANGUAGES.EN, NluKeywordAnalyzerEN.class.getCanonicalName());
		keywordAnalyzers.put("default", NluKeywordAnalyzer.class.getCanonicalName());		//KEEP THIS! needs to spit out "No_Result" and slash CMDs
    }
	// - interpretation chain (default, can be overwritten by config file)
	public static List<String> nluInterpretationStepsCustomChain =  Arrays.asList(
			"getPersonalCommand",
			"getFixCommandsExactMatch",
			"getChatSmallTalkMatch",
			"getPublicDbSentenceMatch",
			"getKeywordAnalyzerResult",
			"tryPersonalCommandAsFallback",
			"tryChatSmallTalkAsFallback"
	);
	public static List<InterpretationStep> nluInterpretationSteps = new ArrayList<>(); 		//holds the default list for the interpretation chain
	/**
	 * Prepare interpretation chain by adding the default modules in the proper order to 'nluInterpretationSteps' list.
	 */
	public static void setupNluSteps(){
		//add core steps
		nluInterpretationSteps.add(InterpretationChain.coreSteps.get("applyInputModifiers"));	//things like i18n:...
		nluInterpretationSteps.add(InterpretationChain.coreSteps.get("getDirectCommand"));		//predefined direct commands like events;;...
		nluInterpretationSteps.add(InterpretationChain.coreSteps.get("getResponse"));			//response to assistant question
		nluInterpretationSteps.add(InterpretationChain.coreSteps.get("getSlashCommand"));		//"slash-commands" like "\saythis"
		//add custom steps
		for (String stepName : nluInterpretationStepsCustomChain){
			stepName = stepName.trim();
			if (stepName.startsWith("WEB:")){
				String webApiUrl = stepName.replace("WEB:", "").trim();
				nluInterpretationSteps.add((input, cachedResults) -> InterpretationStep.getWebApiResult(webApiUrl, input, cachedResults));
			}else if (stepName.startsWith("CLASS:")){
				String className = stepName.replace("CLASS:", "").trim();
				nluInterpretationSteps.add((input, cachedResults) -> InterpretationStep.getClassResult(className, input, cachedResults));
			}else{
				nluInterpretationSteps.add(InterpretationChain.availableSteps.get(stepName));
			}
		}
		Debugger.println("Loaded NLU interpretation-chain with " + nluInterpretationStepsCustomChain.size() 
			+ " steps: " + nluInterpretationStepsCustomChain.toString(), 3);
	}
	// - sentence matcher thresholds
	public static double threshold_chats_match = 0.70;			//approximate chats identity threshold - first check 
	public static double threshold_chats_last_chance = 0.55;	//approximate chats threshold if nothing else gave a result
	public static double threshold_personal_cmd = 0.85;			//approximate personal commands threshold - first check 
	public static double threshold_personal_cmd_2nd = 0.55;		//approximate personal commands threshold on second chance

	//API Keys - loaded from config file
	public static String amazon_dynamoDB_access = "";
	public static String amazon_dynamoDB_secret = "";
	public static String google_maps_key = "";
	public static String graphhopper_key = "";
	public static String forecast_io_key = "";
	public static String spotify_client_id = "";
	public static String spotify_client_secret = "";
	public static String dirble_key = "";
	public static String acapela_vaas_app = "";
	public static String acapela_vaas_key = "";
	public static String affilinet_pubID = "";
	public static String affilinet_key = "";
	public static String deutscheBahnOpenApi_key = "";
	
	//API URLs - loaded from config file
	public static String marytts_server = "";
	public static String smarthome_hub_host = "";
	public static String smarthome_hub_name = "";
	public static String smarthome_hub_auth_type = "";
	public static String smarthome_hub_auth_data = "";
	
	//------Database loading and default interpreters------
	
	/**
	 * Setup the modules for database access.
	 * @param preLoadData - certain database instances might be able to cache some data (similar to the NLU system)
	 */
	public static void setupDatabases(boolean preLoadData){
		//refresh settings
		DB.refreshSettings();
		//pre-load some data
		if (preLoadData){
			DB.preLoadData();
		}
	}
	/**
	 * Test databases, e.g. Elasticsearch mappings etc.
	 * @throws Exception
	 */
	public static void testDatabases() throws Exception{
		//check Elasticsearch mappings
		if (checkElasticsearchMappingsOnStart){
			Setup.testAndUpdateElasticsearchMapping(true);
		}
	}
	
	//set answer loader - this is for common answers, API answers can also be loaded inside the API itself
	public static AnswerLoader answers; 	//e.g. new ANS_Loader_txt();
	/**
	 * Load answers to memory for super fast access or prepare live DB access (depending on answer interface implementation).
	 */
	public static void setupAnswers(){
		long tic = Debugger.tic();
		answers = (AnswerLoader) ClassBuilder.construct(answerModule);
		answers.setupAnswers();
		Debugger.println("Finished loading answers module in "+ Debugger.toc(tic) + " ms.", 3);
	}
	
	//set predefined commands data (e.g. teachIt_de.txt) and NLP
	//TODO: remove this from config
	public static NluInterface fixCommands_NLP;
	/**
	 * Load all predefined commands to memory for super fast access.
	 */
	public static void setupCommands(){
		long tic = Debugger.tic();
		//TODO: streamline with answers loading
		DataLoader dl = new DataLoader(); 		
		fixCommands_NLP = new NluSentenceMatcher(dl.loadCommandsFromFilebase(commandsPath + "teachIt"));	//identity match
		int iv = dl.getValidEntries();
		//int is = dl.getStoredEntries();
		int is = dl.getUniqueEntries();
		Debugger.println("Finished loading " + is + "(" + iv + ") predefined commands in "+ Debugger.toc(tic) + " ms.", 3);
	}
	
	//set predefined chats data (e.g. chats_de.txt) and NLP
	//TODO: remove this from config
	public static NluInterface fixChats_NLP;
	/**
	 * Load all predefined chats to memory for super fast access.
	 */
	public static void setupChats(){
		long tic = Debugger.tic();
		//TODO: streamline with answers loading
		DataLoader dl = new DataLoader();
		fixChats_NLP = new NluApproximateMatcher(dl.loadCommandsFromFilebase(commandsPath + "chats"));	//approximation match
		int iv = dl.getValidEntries();
		//int is = dl.getStoredEntries();
		int is = dl.getUniqueEntries();
		Debugger.println("Finished loading " + is + "(" + iv + ") predefined chats in "+ Debugger.toc(tic) + " ms.", 3);
	}
	
	//set personal commands data
	//Personal commands NLP is set in interpretation script locally
	
	//------other tools------

	//RSS feed reader, Spotify API, etc. ...
	public static RssFeedReader rssReader;
	public static SpotifyApi spotifyApi;
	
	/**
	 * Setup tools like RssFeedReader or SpotifyApi.
	 */
	public static void setupTools(){
		//RSS
		rssReader = new RssFeedReader();
		//Spotify
		if (Is.notNullOrEmpty(spotify_client_id) && Is.notNullOrEmpty(spotify_client_secret)){
			spotifyApi = new SpotifyApi(spotify_client_id, spotify_client_secret);
		}
	}
	
	/**
	 * Setup TTS module (e.g. clean-up 'tts' folder, load voices etc.).
	 */
	public static void setupTts(){
		boolean setupOk = false;
		try{
			Debugger.println("Running TTS module setup ...", 3);
			TtsInterface tts = (TtsInterface) ClassBuilder.construct(Config.ttsModule);
			setupOk = tts.setup();
		}catch (Exception e){
			setupOk = false;
			Debugger.println("TTS module setup failed with message: " + e.getMessage(), 1);
		}
		if (!setupOk){
			Config.ttsModuleEnabled = false;
			Debugger.println("TTS module setup failed and module was deactivated!", 1);
		}else{
			Debugger.println("TTS module setup successful.", 3);
		}
	}
	
	//----------helpers----------
	
	/**
	 * Load server settings from properties file. 
	 */
	public static void loadSettings(String confFile){
		if (confFile == null || confFile.isEmpty())	confFile = configFile;
		
		try{
			Properties settings = FilesAndStreams.loadSettings(confFile);
			//server
			endpointUrl = settings.getProperty("server_endpoint_url");	
			teachApiUrl = settings.getProperty("server_teach_api_url");
			localName = settings.getProperty("server_local_name");
			localSecret = settings.getProperty("server_local_secret");
			serverPort = Integer.valueOf(settings.getProperty("server_port"));
			clusterKey = settings.getProperty("cluster_key");
				clusterKeyLight = clusterKey.substring(0, 17);
				cklHashIterations = ((int) clusterKeyLight.charAt(clusterKeyLight.length()-1)) + 5;
			allowInternalCalls = Boolean.valueOf(settings.getProperty("allow_internal_calls"));
			allowGlobalDevRequests = Boolean.valueOf(settings.getProperty("allow_global_dev_requests"));
			enableCORS = Boolean.valueOf(settings.getProperty("enable_cors", Boolean.toString(enableCORS)));
			enableFileCORS = Boolean.valueOf(settings.getProperty("enable_file_cors", Boolean.toString(enableFileCORS)));
			//policies
			privacyPolicyLink =  settings.getProperty("privacy_policy");
			//modules
			String authAndAccountModule = settings.getProperty("module_account");
			if (authAndAccountModule != null){
				//Account and authentication should be used together with the same DB
				authAndAccountDB = authAndAccountModule;
				if (authAndAccountModule.equals("dynamo_db")){
					accountModule = AccountDynamoDB.class.getCanonicalName();
					authenticationModule = AuthenticationDynamoDB.class.getCanonicalName();
				}else if (authAndAccountModule.equals("elasticsearch")){
					accountModule = AccountElasticsearch.class.getCanonicalName();
					authenticationModule = AuthenticationElasticsearch.class.getCanonicalName();
				}else{
					//In case we have more modules we need to change this here:
					accountModule = AccountElasticsearch.class.getCanonicalName();
					authenticationModule = AuthenticationElasticsearch.class.getCanonicalName();
				}
			}
			String answerModuleValue = settings.getProperty("module_answers");
			if (answerModuleValue != null){
				if (answerModuleValue.equals("file")){
					answerModule = AnswerLoaderFile.class.getCanonicalName();
				}else if (answerModuleValue.equals("elasticsearch")){
					answerModule = AnswerLoaderElasticsearch.class.getCanonicalName();
				}else{
					answerModule = answerModuleValue;
				}
			}
			ttsModule = settings.getProperty("module_tts", TtsOpenEmbedded.class.getCanonicalName());
			ttsName = settings.getProperty("tts_engine_name", "Open Embedded");
			ttsModuleEnabled = Boolean.valueOf(settings.getProperty("tts_enabled", "true"));
			enableSDK = Boolean.valueOf(settings.getProperty("enable_sdk"));
			//useSandboxPolicy = Boolean.valueOf(settings.getProperty("use_sandbox_security_policy", "true"));		//NOTE: this will only be accessible via commandline argument
			useSentencesDB = Boolean.valueOf(settings.getProperty("enable_custom_commands"));
			//databases
			defaultRegion = settings.getProperty("db_default_region", "eu");
			ConfigDynamoDB.region_custom = settings.getProperty("db_dynamo_region_custom", "");
			ConfigDynamoDB.region_eu1 = settings.getProperty("db_dynamo_region_eu1", "eu-central-1");
			ConfigDynamoDB.region_us1 = settings.getProperty("db_dynamo_region_us1", "us-east-1");
			ConfigElasticSearch.endpoint_custom = settings.getProperty("db_elastic_endpoint_custom", "");
			ConfigElasticSearch.endpoint_eu1 = settings.getProperty("db_elastic_endpoint_eu1");
			ConfigElasticSearch.endpoint_us1 = settings.getProperty("db_elastic_endpoint_us1");
			ConfigElasticSearch.auth_type = settings.getProperty("db_elastic_auth_type", null);
			ConfigElasticSearch.auth_data = settings.getProperty("db_elastic_auth_data", null);
			checkElasticsearchMappingsOnStart = Boolean.valueOf(settings.getProperty("check_elasticsearch_mappings_on_start", "true")); 	//TODO: not yet in properties file
			//chat
			connectToWebSocket = Boolean.valueOf(settings.getProperty("connect_to_websocket"));
			//NLU chain
			String nluInterpretationChainArr = settings.getProperty("nlu_interpretation_chain", "");
			if (nluInterpretationChainArr != null && !nluInterpretationChainArr.isEmpty()){
				nluInterpretationStepsCustomChain = Arrays.asList(nluInterpretationChainArr.split(","));
			}
			//NLU performance profilers
			parameterPerformanceMode = Integer.valueOf(settings.getProperty("parameter_performance_mode", "0"));
			//workers
			String backgroundWorkersArr = settings.getProperty("background_workers", "");
			if (backgroundWorkersArr != null && !backgroundWorkersArr.isEmpty()){
				backgroundWorkers = Arrays.asList(backgroundWorkersArr.split(","));
			}
			//web content
			urlCreateUser = settings.getProperty("url_createUser"); 	
			urlChangePassword = settings.getProperty("url_changePassword"); 
			urlWebImages = settings.getProperty("url_web_images"); 	
			urlWebFiles = settings.getProperty("url_web_files"); 		
			urlDashboard = settings.getProperty("url_dashboard");
			hostFiles = Boolean.valueOf(settings.getProperty("host_files"));
			allowFileIndex = Boolean.valueOf(settings.getProperty("allow_file_index", "true")); 	//TODO: not yet in properties file
			String fileMimeTypesList = settings.getProperty("file_mime_types");
			if (Is.notNullOrEmpty(fileMimeTypesList)){
				fileMimeTypes = fileMimeTypesList;
			}
			//email account
			emailHost = settings.getProperty("email_host");
			emailAccount = settings.getProperty("email_account");
			emailAccountKey = settings.getProperty("email_account_key");
			emailBCC = settings.getProperty("email_bcc", "");
			//assistant
			assistantName = settings.getProperty("assistant_name");
			assistantId = settings.getProperty("assistant_id");
			assistantEmail = settings.getProperty("assistant_email");
			assistantPwd = settings.getProperty("assistant_pwd");
			String assistDeviceId = settings.getProperty("assistant_device_id");
			if (Is.notNullOrEmpty(assistDeviceId)){
				assistantDeviceId = assistDeviceId;
			}
			String assistantAllowFollowUpsString = settings.getProperty("assistant_allow_follow_ups");
			if (assistantAllowFollowUpsString != null && !assistantAllowFollowUpsString.isEmpty()){
				assistantAllowFollowUps = Boolean.valueOf(assistantAllowFollowUpsString);
			}
			//credentials
			userIdPrefix = settings.getProperty("user_id_prefix");
			guidOffset =  Long.valueOf(settings.getProperty("guid_offset"));
			superuserId = settings.getProperty("universal_superuser_id");
			superuserEmail = settings.getProperty("universal_superuser_email");
			superuserPwd = settings.getProperty("universal_superuser_pwd");
			//protected accounts
			String protectedAccountsStr = settings.getProperty("protected_accounts_list", "").trim().replaceAll("^\\[|\\]$", "").trim();
			if (!protectedAccountsStr.isEmpty()){
				String[] protectedAccountsArray = protectedAccountsStr.split("\\s*,\\s*");
				for (String kv : protectedAccountsArray){
					String[] ue = kv.split(";;");
					protectedAccounts.put(ue[0].trim(), ue[1].trim());
				}
			}
			//API keys
			amazon_dynamoDB_access = settings.getProperty("amazon_dynamoDB_access");
			amazon_dynamoDB_secret = settings.getProperty("amazon_dynamoDB_secret");
			google_maps_key = settings.getProperty("google_maps_key");
			graphhopper_key = settings.getProperty("graphhopper_key");
			forecast_io_key = settings.getProperty("forecast_io_key");
			spotify_client_id = settings.getProperty("spotify_client_id");
			spotify_client_secret = settings.getProperty("spotify_client_secret");
			dirble_key = settings.getProperty("dirble_key");
			acapela_vaas_app = settings.getProperty("acapela_vaas_app");
			acapela_vaas_key = settings.getProperty("acapela_vaas_key");
			affilinet_pubID = settings.getProperty("affilinet_pubID");
			affilinet_key = settings.getProperty("affilinet_key");
			deutscheBahnOpenApi_key = settings.getProperty("deutscheBahnOpenApi_key");
			//API URLs
			marytts_server = settings.getProperty("marytts_server", "http://127.0.0.1:59125").replaceAll("/$", "");
			smarthome_hub_host = settings.getProperty("smarthome_hub_host");
			smarthome_hub_name = settings.getProperty("smarthome_hub_name");
			if (Is.nullOrEmpty(smarthome_hub_host)){
				//try legacy settings
				smarthome_hub_host = settings.getProperty("openhab_host");
				if (Is.notNullOrEmpty(smarthome_hub_host)){
					smarthome_hub_name = OpenHAB.NAME;
				}
			}
			smarthome_hub_auth_type = settings.getProperty("smarthome_hub_auth_type", null);
			smarthome_hub_auth_data = settings.getProperty("smarthome_hub_auth_data", null);
			
			Debugger.println("loading settings from " + confFile + "... done." , 3);
		}catch (Exception e){
			Debugger.println("loading settings from " + confFile + "... failed!" , 1);
		}
	}
	/**
	 * Save server settings to file. Skip security relevant fields.
	 */
	public static void saveSettings(String confFile){
		if (confFile == null || confFile.isEmpty())	confFile = configFile;
		
		//save all personal parameters
		Properties config = new Properties();
		//server
		config.setProperty("server_endpoint_url", endpointUrl);
		config.setProperty("server_teach_api_url", teachApiUrl);
		config.setProperty("server_local_name", localName);
		config.setProperty("server_local_secret", "");
		config.setProperty("server_port", String.valueOf(serverPort));
		config.setProperty("cluster_key", "");
		config.setProperty("allow_internal_calls", String.valueOf(allowInternalCalls));
		config.setProperty("allow_global_dev_requests", String.valueOf(allowGlobalDevRequests));
		//databases
		config.setProperty("db_default_region", defaultRegion);
		config.setProperty("db_dynamo_region_custom", ConfigDynamoDB.region_custom);
		config.setProperty("db_dynamo_region_eu1", ConfigDynamoDB.region_eu1);
		config.setProperty("db_dynamo_region_us1", ConfigDynamoDB.region_us1);
		config.setProperty("db_elastic_endpoint_custom", ConfigElasticSearch.endpoint_custom);
		config.setProperty("db_elastic_endpoint_eu1", ConfigElasticSearch.endpoint_eu1);
		config.setProperty("db_elastic_endpoint_us1", ConfigElasticSearch.endpoint_us1);
		//modules
		config.setProperty("enable_sdk", String.valueOf(enableSDK));
		config.setProperty("enable_custom_commands", String.valueOf(useSentencesDB));
		//chat
		config.setProperty("connect_to_websocket", String.valueOf(connectToWebSocket));
		//NLU chain
		config.setProperty("nlu_interpretation_chain", nluInterpretationStepsCustomChain.toString().replaceAll("^\\[|\\]$", ""));
		//workers
		config.setProperty("background_workers", backgroundWorkers.toString().replaceAll("^\\[|\\]$", ""));
		//web content
		config.setProperty("host_files", String.valueOf(hostFiles));
		config.setProperty("url_createUser", urlCreateUser);
		config.setProperty("url_changePassword", urlChangePassword); 
		config.setProperty("url_web_images", urlWebImages); 	
		config.setProperty("url_web_files", urlWebFiles); 		
		config.setProperty("url_dashboard", urlDashboard);
		//email account
		config.setProperty("email_host", emailHost);
		config.setProperty("email_account", emailAccount);
		config.setProperty("email_account_key", "");
		config.setProperty("email_bcc", emailBCC);
		//assistant
		config.setProperty("assistant_name", assistantName);
		config.setProperty("assistant_id", assistantId);
		config.setProperty("assistant_email", assistantEmail);
		config.setProperty("assistant_pwd", "");
		config.setProperty("assistant_allow_follow_ups", String.valueOf(assistantAllowFollowUps));
		//credentials
		config.setProperty("user_id_prefix", userIdPrefix);
		config.setProperty("guid_offset", String.valueOf(guidOffset));
		config.setProperty("universal_superuser_id", superuserId);
		config.setProperty("universal_superuser_email", superuserEmail);
		config.setProperty("universal_superuser_pwd", "");
		/*
		//API keys
		//...
		//API URLs
		//...
		*/
		
		try{
			FilesAndStreams.saveSettings(confFile, config);
			Debugger.println("saving settings to " + confFile + "... done." , 3);
		}catch (Exception e){
			Debugger.println("saving settings to " + confFile + "... failed!" , 1);
		}
	}
	
	/**
	 * Replace a key-value pair in settings file or append it if not found.
	 * @param confFile
	 * @param keyToReplace
	 * @param newValue
	 */
	public static void replaceSettings(String confFile, String keyToReplace, String newValue){
		if (confFile == null || confFile.isEmpty())	confFile = configFile;
		FilesAndStreams.replaceLineOrAppend(
				configFile,
				("^" + keyToReplace + "=.*"),	
				(oldLine) -> { return (keyToReplace + "=" + newValue); }
		);
	}

}
