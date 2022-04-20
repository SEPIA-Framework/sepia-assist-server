package net.b07z.sepia.server.assist.server;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.b07z.sepia.server.core.tools.DateTime;
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
	private static String pathToAutoSetupFolder = "../automatic-setup/";
	private static String pathToAutoSetupFile = pathToAutoSetupFolder + "config.yaml";
	private static String autoSetupResultLog = "results.log";
	
	private enum ServerType {
		custom,
		live,
		test
	}
	private static class CreateUserResult {
		String guuid;
		String pwdClientHash;
		public String email;
		
		public CreateUserResult(JSONObject json){
			this.guuid = JSON.getString(json, ACCOUNT.GUUID);
			this.pwdClientHash = JSON.getString(json, ACCOUNT.PASSWORD);
			this.email = JSON.getString(json, ACCOUNT.EMAIL);
		}
		public CreateUserResult(String email, String guuid, String pwdHash){
			this.guuid = guuid;
			this.pwdClientHash = pwdHash;
			this.email = email;
		}
	}
	private static class ServerConfigFiles {
		String assist;
		String teach;
		String webSocket;
		
		String duckdns;
		
		public List<String> getAllServers(){
			return Arrays.asList(assist, teach, webSocket);
		}
	}
	private static class SetupTasks {
		boolean all = true;			//if this is true we do everything (except duckdns setup) and ignore below booleans
		boolean database = false;
		String dbIndex = "";		//write all (empty) or a specific index of the database (e.g. answers, users, chat, ...)?
		boolean cluster = false;
		boolean accounts = false;
		boolean answers = false;
		boolean commands = false;
		boolean duckDns = false;	//NOTE: not part of "all"
		
		void setTasks(String taskArg){
			if (taskArg.equals("database")){
				this.database = true;
			}else if (this.database && taskArg.startsWith("index=")){
				this.dbIndex = taskArg.replaceFirst(".*?=", "").trim();
			}else if (taskArg.equals("cluster")){
				this.cluster = true;
			}else if (taskArg.equals("accounts")){
				this.accounts = true;
			}else if (taskArg.equals("answers")){
				this.answers = true;
			}else if (taskArg.equals("commands")){
				this.commands = true;
			}else if (taskArg.toLowerCase().equals("duckdns")){
				this.duckDns = true;
			}
		}
		void checkAll(){
			if (this.database || this.cluster || this.accounts || this.answers || this.commands || this.duckDns){
				this.all = false;
			}
		}
		String tasksToString(){
			String tasks = "";
			if (this.all || this.database) tasks += "database, ";
			if (!this.dbIndex.isEmpty()) tasks += "dbIndex=" + this.dbIndex + ", ";
			if (this.all || this.cluster) tasks += "cluster, ";
			if (this.all || this.accounts) tasks += "accounts, ";
			if (this.all || this.answers) tasks += "answers, ";
			if (this.all || this.commands) tasks += "commands, ";
			if (this.duckDns) tasks += "duckDns, ";
			return tasks.replaceAll(", $", "").trim();
		}
	}
	
	private static void writeAutoSetupLog(String msg) throws IOException {
		writeAutoSetupMessage("LOG", msg);
	}
	private static void writeAutoSetupMessage(String logType, String msg) throws IOException {
		//logType: ERROR, LOG, INFO, DEBUG
		msg = DateTime.getLogDate() + " " + logType + " - " + msg;
		FilesAndStreams.appendLineToFile(pathToAutoSetupFolder, autoSetupResultLog, msg);
	}
	
	/**
	 * CAREFUL! THIS WILL OVERWRITE THE DATABASE!
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		//automatic?
		boolean automaticSetup = false;
		
		//components to setup
		SetupTasks tasks = new SetupTasks();
		
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
			}else if (arg.equals("--unattended") || arg.equals("--automatic")){
				//Auto-Setup
				automaticSetup = true;
			}else{
				//tasks arg?
				tasks.setTasks(arg);
			}
		}
		System.out.println("Setup for '" + st.name() + "' server (" + Config.configFile + ")");
		System.out.println("Is unattended (--unattended): " + automaticSetup);
		if (automaticSetup){
			writeAutoSetupLog("Running auto-setup script for server type: " + st.name());
		}
		
		loadConfigFile(st);
		ServerConfigFiles scf = getConfigFiles(st);
		
		//prep. users
		Map<String, SetupUserData> userData = null;
		SetupUserData adminData = null;
		SetupUserData assistantData = null;
		//prep. DNS data
		Map<String, String> dnsData = null;
		
		//check automatic setup file
		System.out.println("\nReading file for unattended/automatic setup: " + pathToAutoSetupFile);
		try{
			SetupYamlConfig setupYaml = FilesAndStreams.readYamlFile(pathToAutoSetupFile, SetupYamlConfig.class);
			if (setupYaml.getTasks() != null){
				for (String t : setupYaml.getTasks()){
					tasks.setTasks(t);
				}
			}
			userData = setupYaml.getUsers();
			if (userData != null){
				adminData = userData.get("admin");
				userData.remove("admin");
				assistantData = userData.get("assistant");
				userData.remove("assistant");
			}
			dnsData = setupYaml.getDns();
		}catch(Exception e){
			System.out.println("ERROR reading YAML file! Msg.: " + e.getMessage());
			throw e;
		}
		
		//update tasks
		tasks.checkAll();
		if (automaticSetup){
			writeAutoSetupLog("Tasks: " + tasks.tasksToString());
		}
		
		//database
		if (tasks.all || tasks.database){
			//prepare Elasticsearch
			System.out.println("\nPreparing Elasticsearch: ");
			writeElasticsearchMapping(tasks.dbIndex); 	//writes all indices if dbIndex is empty or null
			if (automaticSetup){
				writeAutoSetupLog("Database - Elasticsearch: mappings (re)written");
			}
			
			//prepare DynamoDB (optional)
			if (Config.authAndAccountDB.equals("dynamo_db")){
				System.out.println("\nPreparing DynamoDB: ");
				writeDynamoDbIndices();				//note: dbIndex not supported yet for DynamoDB
				if (automaticSetup){
					writeAutoSetupLog("Database - DynamoDB: indices (re)written");
				}
			}
		}
		
		//cluster
		if (tasks.all || tasks.cluster){
			System.out.println("\nSetting up cluster: ");
			generateAndStoreClusterKey(scf);
			if (automaticSetup){
				writeAutoSetupLog("Cluster - Generated and stored cluster key");
			}
		}
		
		//accounts
		if (tasks.all || tasks.accounts){
			System.out.println("\nSetting up accounts: ");
			
			//manual setup?
			if (!automaticSetup){
				//TEST ACCOUNTS
				if (st.equals(ServerType.test)){
					//admin
					adminData = new SetupUserData();
					adminData.setEmail(Config.superuserEmail);		//"admin@sepia.localhost";
					adminData.setPassword("test12345");
					//assistant
					assistantData = new SetupUserData();
					assistantData.setEmail(Config.assistantEmail);	//"assistant@sepia.localhost";
					assistantData.setPassword("test12345");
					//test user
					SetupUserData testUser1 = new SetupUserData();
					testUser1.setNickname("Testy");
					testUser1.setEmail("test@sepia.localhost");
					testUser1.setPassword("test12345");
					userData = new HashMap<>();
					userData.put("user1", testUser1);
				
				//USER INPUT
				}else{
					//core accounts
					adminData = new SetupUserData();
					adminData.setEmail(Config.superuserEmail);		//"admin@sepia.localhost";
					assistantData = new SetupUserData();
					assistantData.setEmail(Config.assistantEmail);	//"assistant@sepia.localhost";
					//ask for passwords:
					System.out.println("\nPlease define safe passwords for SEPIA admin and assistant (and remember them well!).");
					System.out.println("Use AT LEAST 8 characters and combine lower/upper case letters with numbers and special characters:");
					String adminPwd = "";
					String assistantPwd = "";
					while (adminPwd.length() < 8){
						adminPwd = InputPrompt.askString("Admin password: ", false);
						if (adminPwd.length() < 8){
							System.out.println("Password is too short! Try again. (CRTL+C to abort)");
						}
					}
					while (assistantPwd.length() < 8){
						assistantPwd = InputPrompt.askString("Assistant password: ", false);
						if (assistantPwd.length() < 8){
							System.out.println("Password is too short! Try again. (CRTL+C to abort)");
						}
					}
					adminData.setPassword(adminPwd);
					assistantData.setPassword(assistantPwd);
				}
			}
			int created = 0;
			
			//create admin and assistant user
			if (adminData != null){
				System.out.println("\nCreating admin... ");
				try{
					String password = adminData.getPasswordOrRandom();
					String email = adminData.getEmailOrDefault("admin");
					writeSuperUser(scf, email, password);
					created++;
					if (automaticSetup){
						writeAutoSetupLog("Accounts - Created/updated admin account - email: " + email + ", passw.: " + password);
					}
				}catch(Exception e){
					System.out.println("ERROR in admin data! Msg.: " + e.getMessage());
					throw e;
				}
			}
			if (assistantData != null){
				System.out.println("\nCreating assistant... ");
				try{
					String password = assistantData.getPasswordOrRandom();
					String email = assistantData.getEmailOrDefault("assistant");
					writeAssistantUser(scf, email, password);
					created++;
					if (automaticSetup){
						writeAutoSetupLog("Accounts - Created/updated assistant account - email: " + email + ", passw.: " + password);
					}
				}catch(Exception e){
					System.out.println("ERROR in assistant data! Msg.: " + e.getMessage());
					throw e;
				}
			}
			if (userData != null){
				//create additional users
				if (st.equals(ServerType.test)){
					System.out.println("\nCreating users... ");
					for (String key : userData.keySet()){
						try{
							SetupUserData sud = userData.get(key);
							String password = sud.getPasswordOrRandom();
							String email = sud.getEmailOrDefault(key);
							String nickname = sud.getNickname();
							if (Is.nullOrEmpty(nickname)){
								nickname = key;
							}
							List<String> roles = sud.getRolesOrDefault();
							writeBasicUser(email, password, nickname, roles);
							created++;
							if (automaticSetup){
								writeAutoSetupLog("Accounts - Created/updated user account - email: " + email + ", passw.: " + password);
							}
						}catch(Exception e){
							System.out.println("ERROR in user data! Msg.: " + e.getMessage());
							throw e;
						}
					}
				}
			}
			if (created > 0 && automaticSetup){
				File logFile = new File(pathToAutoSetupFolder + autoSetupResultLog);
				System.out.println("\nIMPORTANT: " + created + " users have been created/updated and PASSWORDS");
				System.out.println("have been WRITTEN to: " + logFile.getAbsolutePath());
				System.out.println("Please REMOVE it when you're done to KEEP ALL PASSWORDS SAFE!\n");
			}
		}
		
		//answers
		if (tasks.all || tasks.answers){
			System.out.println("\nImporting answers from resource files to Elasticsearch: ");
			importAnswers();
			if (automaticSetup){
				writeAutoSetupLog("Answers - Import task finished.");
			}
		}
		
		//commands
		if (tasks.all || tasks.commands){
			//TODO: implement command import
			System.out.println("\nImporting sentences/commands from resource files to Elasticsearch: ");
			importSentences();
			if (automaticSetup){
				writeAutoSetupLog("Commands - Task finished.");
			}
		}
		
		//DuckDNS
		if (tasks.duckDns){
			System.out.println("\nSetting up DuckDNS: ");
			
			String duckDnsDomain = "";
			String duckDnsToken = "";
				
			//Ask for passwords
			if (!automaticSetup){
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
			}else if (dnsData != null){
				duckDnsDomain = dnsData.get("domain");
				duckDnsToken = dnsData.get("token");
			}
			if (Is.nullOrEmpty(duckDnsDomain) || Is.nullOrEmpty(duckDnsToken)){
				System.out.println("ERROR in DuckDNS data! Missing or invalid 'dns.domain' or 'dns.token'");
				System.exit(1);
			}
			//write duck-dns config and add DuckDNS-worker to assist-server
			writeDuckDnsSettings(duckDnsDomain, duckDnsToken, scf);
			System.out.println("\nSetup of DuckDNS worker complete.");
			System.out.println("Domain: " + duckDnsDomain);
			//System.out.println("Token: " + duckDnsToken);
			if (automaticSetup){
				writeAutoSetupLog("DuckDns - Worker setup complete - Domain: " + duckDnsDomain);
			}
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
		writeElasticsearchMapping(indexToMap, false);
	}
	/**
	 * Clean and rewrite Elasticsearch mapping.
	 * @param indexToMap - index to map, mapping is loaded from folder set in config. If null all mappings are cleared and written again.
	 * @param skipIndexClean - if you know for sure that this index is missing set this to true
	 * @throws Exception
	 */
	public static void writeElasticsearchMapping(String indexToMap, boolean skipIndexClean) throws Exception{
		Elasticsearch db = new Elasticsearch();
		List<File> mappingFiles = FilesAndStreams.directoryToFileList(Config.dbSetupFolder + "ElasticsearchMappings/", null, false);
		boolean writeSingleIndex = (indexToMap != null && !indexToMap.isEmpty() && !indexToMap.equals("_all"));
		//Clean up before write
		if (!skipIndexClean){
			if (writeSingleIndex){
				//clean one index
				int resCode = db.deleteAny(indexToMap);
				if (resCode == 0){
					Debugger.println("Elasticsearch: cleaning index '" + indexToMap + "'", 3);
					Debugger.sleep(1500);
				}else{
					Debugger.println("Elasticsearch: ERROR in cleaning index '" + indexToMap + "' - maybe because it did not exist before? We'll see!", 1);
					//throw new RuntimeException("Elasticsearch: communication error!");
				}
			}else{
				//clean all
				int resCode = db.deleteAny("_all");
				if (resCode == 0){
					Debugger.println("Elasticsearch: cleaning old indices ...", 3);
					Debugger.sleep(1500);
				}else{
					throw new RuntimeException("Elasticsearch: communication error!");
				}
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
		Debugger.sleep(1500);
		if (writeSingleIndex){
			Debugger.println("Elasticsearch: Index ready for work.", 3);
		}else{
			Debugger.println("Elasticsearch: DB ready for work.", 3);
		}
	}
	
	/**
	 * Test if Elasticsearch index exists and (optionally) create when missing.
	 * @param createWhenMissing - true/false
	 * @throws Exception
	 */
	public static void testAndUpdateElasticsearchMapping(boolean createWhenMissing) throws Exception{
		Elasticsearch db = new Elasticsearch();
		List<File> mappingFiles = FilesAndStreams.directoryToFileList(Config.dbSetupFolder + "ElasticsearchMappings/", null, false);
		//Test mappings
		JSONObject mappings = db.getMappings();
		if (mappings == null){
			throw new RuntimeException("ElasticSearch - Failed to check mappings! Is ES running and reachable?");
		}else if (mappings.isEmpty()){
			throw new RuntimeException("ElasticSearch - Failed to check mappings! Did you already run SEPIA setup?");
		}
		int mr = 0;
		int mf = 0;
		for (File f : mappingFiles){
			if (!f.getName().contains(".json")){
				//File has to be a .json map
				continue;
			}
			mr++;
			String index = f.getName().replaceFirst("\\.json$", "").trim();
			if (!mappings.containsKey(index)){
				//missing index
				Debugger.println("Elasticsearch - Missing index: " + index, 1);
				if (createWhenMissing){
					Debugger.println("Elasticsearch - Trying to create missing index: " + index, 3);
					writeElasticsearchMapping(index, true);
					mf++;
				}
			}else{
				mf++;
			}
		}
		if (mr == mf){
			Debugger.println("Elasticsearch: found " + mf + " of " + mr + " mapped indices. All good.", 3);
		}else{
			throw new RuntimeException("Elasticsearch: missing " + (mr-mf) + " of " + mr + " mapped indices. Please check database setup.");
		}
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
			String pwdClientHashed = Security.hashClientPassword(pwd);
			if (DB.writeAccountDataDirectly(guuid, newData)){
				Debugger.println("Stored new password in database for: " + email, 3);
				//store data in config file
				if (!FilesAndStreams.replaceLineInFile(scf.assist, "^universal_superuser_pwd=.*", 
								"universal_superuser_pwd=" + pwdClientHashed)
					){
					throw new RuntimeException("Failed to write new password to config-file: " + scf.assist);
				}else{
					Debugger.println("Stored new password in config: " + scf.assist, 3);
				}
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
								"universal_superuser_pwd=" + cr.pwdClientHash)
					){
					throw new RuntimeException("Failed to write data to config-file: " + scf.assist);
				}else{
					Debugger.println("Stored data in database and config: " + scf.assist, 3);
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
			String pwdClientHashed = Security.hashClientPassword(pwd);
			if (DB.writeAccountDataDirectly(guuid, newData)){
				Debugger.println("Stored new password in database for: " + email, 3);
				//store data in config file
				if (!FilesAndStreams.replaceLineInFile(scf.assist, "^assistant_pwd=.*", 
								"assistant_pwd=" + pwdClientHashed)
					){
					throw new RuntimeException("Failed to write new password to config-file: " + scf.assist);
				}else{
					Debugger.println("Stored new password in config: " + scf.assist, 3);
				}
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
								"assistant_pwd=" + cr.pwdClientHash)
					){
					throw new RuntimeException("Failed to write data to config-file: " + scf.assist);
				}else{
					Debugger.println("Stored data in database and config: " + scf.assist, 3);
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
	private static CreateUserResult writeBasicUser(String email, String pwd, String nickName, List<String> roles) throws Exception{
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
			if (Is.nullOrEmpty(roles)){
				roles = Arrays.asList(Role.user.name());
			}
			JSONObject data = JSON.make(
				ACCOUNT.ROLES, roles,
				ACCOUNT.USER_NAME, JSON.make(
					Name.NICK, nickName
				)
			);
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
		JSON.put(pwdAccountData, ACCOUNT.PASSWORD, gen.pwd); 		//note: server hashed
		JSON.put(pwdAccountData, ACCOUNT.PWD_SALT, gen.salt);
		JSON.put(pwdAccountData, ACCOUNT.PWD_ITERATIONS, gen.iterations);
		return pwdAccountData;
	}
	
	/**
	 * Import sentences (commands) the assistant should understand (e.g. used in the sentence matcher).
	 */
	private static void importSentences(){
		//TODO: right now we still read them simply from file combined with the ones the user "teaches" in the app.
		System.out.println("Import not supported yet - Currently only loaded at server start.");
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
				"^background_workers=.*", (oldLine) -> {
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
	 * @throws Exception 
	 */
	private static void loadConfigFile(ServerType st) throws Exception{
		Start.loadConfigFile(st.name());
		//setup database(s)
		Config.setupDatabases(false);
		
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
