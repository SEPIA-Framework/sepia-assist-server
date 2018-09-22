package net.b07z.sepia.server.assist.server;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.DynamoDB;
import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.assist.database.GUID;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.assist.workers.DuckDnsWorker;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.database.AnswerImporter;
import net.b07z.sepia.server.core.database.DatabaseInterface;
import net.b07z.sepia.server.core.server.ConfigDefaults;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.InputPrompt;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Security;

/**
 * Initializes the server with all its settings and database entries.
 * This setup is required to run before other APIs (teach, chat, ...) are configured the first time.
 * 
 * @author Florian Quirin
 *
 */
public class Setup {
	
	//Cluster paths - note: keep in sync with all server configs
	//private static String pathToAssistConfig = "Xtensions/";
	private static String pathToAssistConfig = "../sepia-assist-server/" + Config.xtensionsFolder;
	private static String pathToTeachConfig = "../sepia-teach-server/" + Config.xtensionsFolder;
	private static String pathToWebSocketConfig = "../sepia-websocket-server-java/" + Config.xtensionsFolder;
	private static String pathToLetsEncryptScripts = "../letsencrypt/";
	
	private enum ServerType{
		custom,
		live,
		test
	}
	private static class CreateUserResult{
		String guuid;
		String pwdHash;
		public String email;
		
		public CreateUserResult(JSONObject json){
			this.guuid = JSON.getString(json, ACCOUNT.GUUID);
			this.pwdHash = JSON.getString(json, ACCOUNT.PASSWORD);
			this.email = JSON.getString(json, ACCOUNT.EMAIL);
		}
		public CreateUserResult(String email, String guuid, String pwdHash){
			this.guuid = guuid;
			this.pwdHash = pwdHash;
			this.email = email;
		}
	}
	private static class ServerConfigFiles{
		String assist;
		String teach;
		String webSocket;
		
		String duckdns;
		
		public List<String> getAllServers(){
			return Arrays.asList(assist, teach, webSocket);
		}
	}
	
	/**
	 * CAREFUL! THIS WILL OVERWRITE THE DATABASE!
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		//components to setup
		boolean all = true;			//if this is true we do everything (except duckdns setup) and ignore below booleans
		boolean database = false;
		String dbIndex = "";		//write all (empty) or a specific index of the database (e.g. answers, users, chat, ...)?
		boolean cluster = false;
		boolean accounts = false;
		boolean answers = false;
		boolean commands = false;
		boolean duckDns = false;
		
		//setup arguments
		ServerType st = ServerType.test;
		for (String arg : args){
			if (arg.equals("--test")){
				//Test system
				st = ServerType.test;
			}else if (arg.equals("--live")){
				//Live system
				st = ServerType.live;
			}else if (arg.equals("--my") || arg.equals("--custom")){
				//Custom system
				st = ServerType.custom;
			}else if (arg.equals("database")){
				all = false;				database = true;
			}else if (database && arg.startsWith("index=")){
				dbIndex = arg.replaceFirst(".*?=", "").trim();
			}else if (arg.equals("cluster")){
				all = false;				cluster = true;
			}else if (arg.equals("accounts")){
				all = false;				accounts = true;
			}else if (arg.equals("answers")){
				all = false;				answers = true;
			}else if (arg.equals("commands")){
				all = false;				commands = true;
			}else if (arg.toLowerCase().equals("duckdns")){
				all = false;				duckDns = true;
			}
		}
		System.out.println("Setup for '" + st.name() + "' server (" + Config.configFile + ")");
		loadConfigFile(st);
		
		ServerConfigFiles scf = getConfigFiles(st);
		
		//database
		if (all || database){
			//prepare Elasticsearch
			System.out.println("\nPreparing Elasticsearch: ");
			writeElasticsearchMapping(dbIndex); 	//writes all indices if dbIndex is empty or null
			
			//prepare DynamoDB (optional)
			if (Config.authAndAccountDB.equals("dynamo_db")){
				System.out.println("\nPreparing DynamoDB: ");
				writeDynamoDbIndices();				//note: dbIndex not supported yet for DynamoDB
			}
		}
		
		//cluster
		if (all || cluster){
			System.out.println("\nSetting up cluster: ");
			generateAndStoreClusterKey(scf);
		}
		
		//accounts
		if (all || accounts){
			System.out.println("\nSetting up accounts: ");
		
			//Default settings for test-mode
			String adminEmail = "admin@sepia.localhost";
			String adminPwd = "test12345";
			String assistantEmail = "assistant@sepia.localhost";
			String assistantPwd = "test12345";
			String testUserEmail = "test@sepia.localhost";
			String testUserPwd = "test12345";
			String testUserNick = "Testy";
			
			//Ask for individual settings?
			if (!st.equals(ServerType.test)){
				//Get emails from config (gives the user a chance to change it)
				adminEmail = Config.superuserEmail;
				assistantEmail = Config.assistantEmail;
				
				//Ask for passwords
				adminPwd = "";
				assistantPwd = "";
				System.out.println("\nPlease define safe passwords for SEPIA admin and assistant (and remember them well!).");
				System.out.println("Use AT LEAST 8 characters and combine lower/upper case letters with numbers and special characters:");
				while (adminPwd.length() < 8){
					adminPwd = InputPrompt.askString("Admin: ", false);
					if (adminPwd.length() < 8){
						System.out.println("Password is too short! Try again. (CRTL+C to abort)");
					}
				}
				while (assistantPwd.length() < 8){
					assistantPwd = InputPrompt.askString("Assistant: ", false);
					if (assistantPwd.length() < 8){
						System.out.println("Password is too short! Try again. (CRTL+C to abort)");
					}
				}
			}
			//create admin and assistant user
			System.out.println("\nCreating admin: ");
			try{
				writeSuperUser(scf, adminEmail, adminPwd);
			}catch(Exception e){
				System.out.println("ERROR in admin data! Msg.: " + e.getMessage());
				throw e;
			}
			System.out.println("\nCreating assistant user: ");
			try{
				writeAssistantUser(scf, assistantEmail, assistantPwd);
			}catch(Exception e){
				System.out.println("ERROR in assistant data! Msg.: " + e.getMessage());
				throw e;
			}
			if (st.equals(ServerType.test)){
				System.out.println("\nCreating basic test user: ");
				try{
					writeBasicUser(testUserEmail, testUserPwd, testUserNick);
				}catch(Exception e){
					System.out.println("ERROR in test user data! Msg.: " + e.getMessage());
					throw e;
				}
			}
		}
		
		//answers
		if (all || answers){
			System.out.println("\nImporting answers from resource files to Elasticsearch: ");
			importAnswers();
		}
		
		//commands
		if (all || commands){
			//TODO: implement command import
			importSentences();
		}
		
		//DuckDNS
		if (duckDns){
			System.out.println("\nSetting up DuckDNS: ");
				
			//Ask for passwords
			String duckDnsToken = "";
			String duckDnsDomain = "";
			System.out.println("\nPlease enter your DuckDNS domain (defined at https://www.duckdns.org):");
			while (duckDnsDomain.length() < 3){
				duckDnsDomain = InputPrompt.askString("DuckDNS Domain: ", false);
				duckDnsDomain = duckDnsDomain.replaceFirst(".*http(s|)://", "").trim();
				if (duckDnsDomain.length() < 3){
					System.out.println("This domain name seems to be invalid, please try again. (CRTL+C to abort)");
				}
			}
			while (duckDnsToken.length() < 16){
				duckDnsToken = InputPrompt.askString("DuckDNS Token: ", false);
				if (duckDnsToken.length() < 16){
					System.out.println("This token seems to be invalid, please try again. (CRTL+C to abort)");
				}
			}
			//write duck-dns config and add DuckDNS-worker to assist-server
			writeDuckDnsSettings(duckDnsDomain, duckDnsToken, scf);
			System.out.println("\nSetup of DuckDNS worker complete.");
			System.out.println("Domain: " + duckDnsDomain);
			//System.out.println("Token: " + duckDnsToken);
		}
		
		/*
		//get Users
		System.out.println("--- Show all users by user ID ---");
		List<JSONObject> dynamoDbUsers = getDynamoDbUserList();
		for (JSONObject u : dynamoDbUsers){
			System.out.println(u.get("Guuid") + " - " + u.get("Email"));
		}
		
		//prepare (clear) command mappings
		System.out.println("--- Reset command mappings for users ---");
		resetUserCommandMappings(dynamoDbUsers);
		*/
		
		System.out.println("\nDONE");
	}
	
	//---------------------------
	
	/**
	 * Clean and rewrite Elasticsearch mapping.
	 * @param indexToMap - index to map, mapping is loaded from folder set in config. If null all mappings are cleared and written again.
	 * @throws Exception
	 */
	public static void writeElasticsearchMapping(String indexToMap) throws Exception{
		Elasticsearch db = new Elasticsearch();
		List<File> mappingFiles = FilesAndStreams.directoryToFileList(Config.dbSetupFolder + "ElasticsearchMappings/", null, false);
		if (indexToMap != null && !indexToMap.isEmpty() && !indexToMap.equals("_all")){
			//clean one index
			int resCode = db.deleteAny(indexToMap);
			if (resCode == 0){
				Debugger.println("Elasticsearch: cleaning index '" + indexToMap + "'", 3);
				Thread.sleep(1500);
			}else{
				Debugger.println("Elasticsearch: ERROR in cleaning index '" + indexToMap + "' - maybe because it did not exist before? We'll see!", 1);
				//throw new RuntimeException("Elasticsearch: communication error!");
			}
		}else{
			//clean all
			int resCode = db.deleteAny("_all");
			if (resCode == 0){
				Debugger.println("Elasticsearch: cleaning old indices ...", 3);
				Thread.sleep(1500);
			}else{
				throw new RuntimeException("Elasticsearch: communication error!");
			}
		}
		//Write mappings
		boolean hasGuidMapping = false; 		//some mappings need to be initialized with an entry
		for (File f : mappingFiles){
			if (!f.getName().contains(".json")){
				//File has to be a .json map
				continue;
			}
			String index = f.getName().replaceFirst("\\.json$", "").trim();
			if (indexToMap == null || indexToMap.isEmpty() || index.equals(indexToMap) || index.equals("_all")){
				JSONObject mapping = JSON.readJsonFromFile(f.getAbsolutePath());
				JSONObject res = db.writeMapping(index, mapping);
				if ((int)res.get("code") == 0){
					Debugger.println("Elasticsearch: created index '" + index + "'", 3);
					if (index.equals(GUID.INDEX)){
						hasGuidMapping = true;
					}
				}else{
					throw new RuntimeException("Elasticsearch: communication error while creating index '" + index + "'");
				}
			}
		}
		//Setup ticket generator - SET FIRST ENTRY so that _update works later
		if (hasGuidMapping){
			int code = db.writeDocument(GUID.INDEX, "sequence", "ticket", JSON.make("near_id", 0, "offset", 0)); 
			if (code != 0){
				throw new RuntimeException("Elasticsearch: writing first entry for GUID generator failed!");
			}else{
				Debugger.println("Elasticsearch: created first entry for '" + GUID.INDEX + "'", 3);
			}
		}
		Thread.sleep(1500);
		Debugger.println("Elasticsearch: ready for work.", 3);
	}
	
	/**
	 * Clean and write DynamoDB indices.
	 */
	public static void writeDynamoDbIndices(){
		//Clean first
		DynamoDB.deleteTable(DB.TICKETS);
		DynamoDB.deleteTable(DB.USERS);
		
		//Tickets
		String primaryKey = DynamoDB.PRIMARY_TICKET_KEY;
		String secondaryIndex = "";
		JSONObject res = DynamoDB.createSimpleTable(DB.TICKETS, primaryKey, secondaryIndex);
		if (!Connectors.httpSuccess(res)){
			throw new RuntimeException("DynamoDB: 'writeDynamoDbIndicies()' FAILED! - msg: " + res);
		}
		//Users
		primaryKey = DynamoDB.PRIMARY_USER_KEY;
		secondaryIndex = ACCOUNT.EMAIL;
		res = DynamoDB.createSimpleTable(DB.USERS, primaryKey, secondaryIndex);
		if (!Connectors.httpSuccess(res)){
			throw new RuntimeException("DynamoDB: 'writeDynamoDbIndicies()' FAILED! - msg: " + res);
		}
	}
	
	//Create admin.
	private static CreateUserResult writeSuperUser(ServerConfigFiles scf, String email, String pwd) throws Exception{
		boolean orgSetting = Config.restrictRegistration;
		Config.restrictRegistration = false; 	//deactivate temporary
		Config.superuserEmail = "";				//deactivate temporary

		//check if user exists
		String guuid = DB.checkUserExistsByEmail(email);
		if (Is.notNullOrEmpty(guuid)){
			//only create new password then ...
			JSONObject newData = createNewPasswordObject(guuid, pwd);
			if (DB.writeAccountDataDirectly(guuid, newData)){
				Debugger.println("Stored new password for: " + email, 3);
				//refresh ID
				Config.superuserId = guuid;
				return new CreateUserResult(email, guuid, JSON.getString(newData, ACCOUNT.PASSWORD));
			}else{
				throw new RuntimeException("Failed to write new password to existing account: " + email);
			}
		}else{
			//proceed with creation
			CreateUserResult cr = new CreateUserResult(DB.createUserDirectly(email, pwd));
			Config.restrictRegistration = orgSetting; 	//reset
			//add user roles
			JSONObject data = JSON.make(ACCOUNT.ROLES, JSON.makeArray(
					Role.user.name(), Role.tester.name(), Role.translator.name(),
					Role.developer.name(), Role.seniordev.name(),
					Role.chiefdev.name(), Role.superuser.name()
			));
			JSON.put(data, ACCOUNT.USER_NAME, JSON.make(
					Name.FIRST, "Admin",
					Name.LAST, "Masters",
					Name.NICK, "MCP"
			));
			if (DB.writeAccountDataDirectly(cr.guuid, data)){ 			//TODO: needs testing for DynamoDB (roles structure changed)
				//store data in config file
				if (!FilesAndStreams.replaceLineInFile(scf.assist, "^universal_superuser_id=.*", 
								"universal_superuser_id=" + cr.guuid)
						|| !FilesAndStreams.replaceLineInFile(scf.assist, "^universal_superuser_email=.*", 
								"universal_superuser_email=" + cr.email)
						|| !FilesAndStreams.replaceLineInFile(scf.assist, "^universal_superuser_pwd=.*", 
								"universal_superuser_pwd=" + cr.pwdHash)
					){
					throw new RuntimeException("Failed to write data to config-file: " + scf.assist);
				}else{
					Debugger.println("Stored data in config: " + scf.assist, 3);
				}
			}else{
				Debugger.println("Writing account data failed! Probably a database error or you need to delete the user first.", 1);
			}
			//refresh ID
			Config.superuserId = cr.guuid;
			return cr;
		}
	}
	//Create assistant user.
	private static CreateUserResult writeAssistantUser(ServerConfigFiles scf, String email, String pwd) throws Exception{
		boolean orgSetting = Config.restrictRegistration;
		Config.restrictRegistration = false; 	//deactivate temporary
		Config.assistantEmail = "";				//deactivate temporary
		
		//check if user exists
		String guuid = DB.checkUserExistsByEmail(email);
		if (Is.notNullOrEmpty(guuid)){
			//only create new password then ...
			JSONObject newData = createNewPasswordObject(guuid, pwd);
			if (DB.writeAccountDataDirectly(guuid, newData)){
				Debugger.println("Stored new password for: " + email, 3);
				//refresh IDs
				Config.assistantId = guuid;
				ConfigDefaults.defaultAssistantUserId = guuid; 
				return new CreateUserResult(email, guuid, JSON.getString(newData, ACCOUNT.PASSWORD));
			}else{
				throw new RuntimeException("Failed to write new password to existing account: " + email);
			}
		}else{
			//proceed with creation
			CreateUserResult cr = new CreateUserResult(DB.createUserDirectly(email, pwd));
			Config.restrictRegistration = orgSetting; 	//reset
			//add user roles
			JSONObject data = JSON.make(ACCOUNT.ROLES, JSON.makeArray(
					Role.user.name(), Role.assistant.name()
			));
			JSON.put(data, ACCOUNT.USER_NAME, JSON.make(
					Name.NICK, Config.assistantName
			));
			if (DB.writeAccountDataDirectly(cr.guuid, data)){ 
				//store data in config file
				if (!FilesAndStreams.replaceLineInFile(scf.assist, "^assistant_id=.*", 
								"assistant_id=" + cr.guuid)
						|| !FilesAndStreams.replaceLineInFile(scf.assist, "^assistant_email=.*", 
								"assistant_email=" + cr.email)
						|| !FilesAndStreams.replaceLineInFile(scf.assist, "^assistant_pwd=.*", 
								"assistant_pwd=" + cr.pwdHash)
					){
					throw new RuntimeException("Failed to write data to config-file: " + scf.assist);
				}else{
					Debugger.println("Stored data in config: " + scf.assist, 3);
				}
			}else{
				Debugger.println("Writing account data failed! Probably a database error or you need to delete the user first.", 1);
			}
			//refresh IDs
			Config.assistantId = cr.guuid;
			ConfigDefaults.defaultAssistantUserId = cr.guuid; 
			return cr;
		}
	}
	//Create basic user.
	private static CreateUserResult writeBasicUser(String email, String pwd, String nickName) throws Exception{
		boolean orgSetting = Config.restrictRegistration;
		Config.restrictRegistration = false; 	//deactivate temporary
		
		//check if user exists
		String guuid = DB.checkUserExistsByEmail(email);
		if (Is.notNullOrEmpty(guuid)){
			//only create new password then ...
			JSONObject newData = createNewPasswordObject(guuid, pwd);
			if (DB.writeAccountDataDirectly(guuid, newData)){
				Debugger.println("Stored new password for: " + email, 3);
				return new CreateUserResult(email, guuid, JSON.getString(newData, ACCOUNT.PASSWORD));
			}else{
				throw new RuntimeException("Failed to write new password to existing account: " + email);
			}
		}else{
			//proceed with creation
			CreateUserResult cr = new CreateUserResult(DB.createUserDirectly(email, pwd));
			Config.restrictRegistration = orgSetting; 	//reset
			//add user roles
			JSONObject data = JSON.make(ACCOUNT.ROLES, JSON.makeArray(
					Role.user.name()
			));
			JSON.put(data, ACCOUNT.USER_NAME, JSON.make(
					Name.NICK, nickName
			));
			if (!DB.writeAccountDataDirectly(cr.guuid, data)){ 
				Debugger.println("Writing account data failed! Probably a database error or you need to delete the user first.", 1);
			}
			return cr;
		}
	}
	//Create new password
	private static JSONObject createNewPasswordObject(String id, String pwdUnhashed) throws Exception{
		String pass = Security.hashClientPassword(pwdUnhashed);
		ID.Generator gen = new ID.Generator(id, pass);
		JSONObject pwdAccountData = new JSONObject();
		JSON.put(pwdAccountData, ACCOUNT.PASSWORD, gen.pwd);
		JSON.put(pwdAccountData, ACCOUNT.PWD_SALT, gen.salt);
		JSON.put(pwdAccountData, ACCOUNT.PWD_ITERATIONS, gen.iterations);
		return pwdAccountData;
	}
	
	/**
	 * Import sentences (commands) the assistant should understand (e.g. used in the sentence matcher).
	 */
	private static void importSentences(){
		//TODO: right now we still read them simply from file combined with the ones the user "teaches" in the app.
	}
	
	/**
	 * Import all answers in the default folder for the assistant user. Throws error if there is any issue.
	 * @throws IOException 
	 */
	private static void importAnswers() throws IOException{
		//TODO: make sure you clean up before importing
		DatabaseInterface db = new Elasticsearch();			//NOTE: hard-coded
		AnswerImporter aim = new AnswerImporter(db);
		aim.loadFolder(Config.answersPath, false); 		//NOTE: make sure this comes after creation of users 		
	}
	
	/**
	 * DuckDNS setup.
	 */
	private static boolean writeDuckDnsSettings(String domain, String token, ServerConfigFiles scf){
		//store data in DuckDNS config file
		boolean duckDnsFileSuccess = FilesAndStreams.replaceLineInFile(
				DuckDnsWorker.configFile,
				"^domain=.*",	
				"domain=" + domain
		) && FilesAndStreams.replaceLineInFile(
				DuckDnsWorker.configFile, 
				"^token=.*",	
				"token=" + token
		);
		if (!duckDnsFileSuccess){
			throw new RuntimeException("Failed to write DuckDNS settings to file! Please check settings and try again.");
		}
		//store data in DuckDNS file for Let's Encrypt
		boolean duckDnsLetsEncryptFileSuccess = FilesAndStreams.replaceLineInFile(
				scf.duckdns,
				"^DOMAIN=.*",	
				"DOMAIN=" + domain
		) && FilesAndStreams.replaceLineInFile(
				scf.duckdns, 
				"^TOKEN=.*",	
				"TOKEN=" + token
		);
		if (!duckDnsLetsEncryptFileSuccess){
			Debugger.println("Failed to write data to Let's Encrypt config (" + scf.duckdns + "). "
					+ "Please add DOMAIN and TOKEN manually!", 1);
		}
		//add worker to assistant config
		boolean assistFileSuccess = FilesAndStreams.replaceLineInFile(scf.assist, 
				"^background_workers=.*", 
				(oldLine) -> {
					//add worker and make sure its not in there twice (remove then add again)
					String newLine = oldLine.replaceFirst("(,|)\\s*" + DuckDnsWorker.workerName + "\\b", "").trim();
					if (newLine.isEmpty()){
						newLine = "DuckDNS-worker";
					}else{
						newLine += ",DuckDNS-worker";
					}
					return newLine;
				}
		);
		if (!assistFileSuccess){
			Debugger.println("Failed to write data to assist config (" + scf.assist + "). "
					+ "Please add this worker manually: " + DuckDnsWorker.workerName, 1);
		}
		return true;
	}
	
	//--------- Helpers ----------
	
	/**
	 * Load config-file by type and setup all the endpoint URLs etc.
	 */
	private static void loadConfigFile(ServerType st){
		Start.loadConfigFile(st.name());
		//setup database(s)
		Config.setupDatabases();
		
		//setup core-tools (especially required for assistant ID)
		JSONObject coreToolsConfig = JSON.make(
				"defaultAssistAPI", Config.endpointUrl,
				"defaultTeachAPI", Config.teachApiUrl,
				"clusterKey", Config.clusterKey,				//requires update after core user creation
				"defaultAssistantUserId", Config.assistantId	//requires update after core user creation
		);
		ConfigDefaults.setupCoreTools(coreToolsConfig);
		
		//Check core-tools settings
		if (!ConfigDefaults.areCoreToolsSet()){
			new RuntimeException("Core-tools are NOT set properly!");
		}
	}
	/**
	 * Get config-file paths for all APIs in cluster (assuming they are on the same server). 
	 */
	private static ServerConfigFiles getConfigFiles(ServerType st){
		//build file names
		String fileNameCenter = "";
		if (st.equals(ServerType.test)){
			fileNameCenter = "test";
		}else if (st.equals(ServerType.custom)){
			fileNameCenter = "custom";
		}
		ServerConfigFiles scf = new ServerConfigFiles();
		scf.assist = pathToAssistConfig + "assist." + fileNameCenter + ".properties"; 	//should be same as Config.configFile here
		scf.teach= pathToTeachConfig + "teach." + fileNameCenter + ".properties";
		scf.webSocket = pathToWebSocketConfig + "websocket." + fileNameCenter + ".properties";
		
		scf.duckdns = pathToLetsEncryptScripts + "duck-dns-settings.sh";
		
		return scf;
	}
	
	/**
	 * Generate shared-key for cluster authentication and try to write it to all servers on this node.
	 */
	private static void generateAndStoreClusterKey(ServerConfigFiles scf){
		//generate key
		String newClusterKey = Security.hashClientPassword(Security.getRandomUUID());
		
		//store data in config files
		for (String filePath : scf.getAllServers()){
			File f = new File(filePath);
			if(!f.exists() || f.isDirectory()) { 
				Debugger.println("Cluster-key - Config-file not found (please update manually): " + filePath, 1);
			}else if (!FilesAndStreams.replaceLineInFile(filePath, "^cluster_key=.*", "cluster_key=" + newClusterKey)){
				Debugger.println("Cluster-key - Error writing config-file (please update manually): " + filePath, 1);
				//throw new RuntimeException("Cluster-key - Error writing config-file (please update manually): " + filePath);
			}else{
				Debugger.println("Cluster-key - Stored new key in config-file: " + filePath, 3);
			}
		}
		
		//refresh key
		ConfigDefaults.clusterKey = newClusterKey;
	}
	
	/**
	 * Generate self-signed SSL certificate.
	 */
	//private static void generateAndStoreSslCertificate(){}
		
	/*
	private static AuthenticationInterface auth;
	private static AccountInterface acc;
	
	private static AuthenticationInterface getAuthDb(){
		if (auth == null){
			auth = (AuthenticationInterface) ClassBuilder.construct(Config.authentication_module);
		}
		return auth;
	}
	private static AccountInterface getAccountDb(){
		if (acc == null){
			acc = (AccountInterface) ClassBuilder.construct(Config.account_module);
		}
		return acc;
	}
	*/
		
	/**
	 * Get users in DynamoDB database as list with JSONObjects {"Guuid", ..., "Email" ...}.
	 */
	/*
	private static List<JSONObject> getDynamoDbUserList(){
		JSONObject requestBody = JSON.make(
				"TableName", DB.USERS,
				"ProjectionExpression", "Guuid, Email",
				"ReturnConsumedCapacity", "NONE",
				"ConsistentRead", false
		);
		JSONObject result = DynamoDB.request("Scan", requestBody.toJSONString());
		//System.out.println("DynamoDB result: " + result); 		//DEBUG
		
		List<JSONObject> users = new ArrayList<>();
		if (Connectors.httpSuccess(result)){
			JSONArray items = (JSONArray) result.get("Items");
			for (Object o : items){
				JSONObject jo = (JSONObject) o;
				String guuid = JSON.getJObject(jo, "Guuid").get("S").toString();
				String email = JSON.getJObject(jo, "Email").get("S").toString();
				users.add(JSON.make("Guuid", guuid, "Email", email));
			}
		}
		return users;
	}
	*/
	
	/**
	 * Clear command mappings for a set of users. User format {"Guuid" : "id1234"}
	 */
	/*
	private static void resetUserCommandMappings(List<JSONObject> users){
		for (JSONObject user : users){
			String id = user.get("Guuid").toString();
			System.out.print("Resetting command mapping for '" + id + "' ... ");
			int code = DB.clearCommandMappings(id);
			if (code == 0){
				System.out.println("success");
			}else{
				System.out.println("failed! - errorCode: " + code);
			}
		}
	}
	*/

}
