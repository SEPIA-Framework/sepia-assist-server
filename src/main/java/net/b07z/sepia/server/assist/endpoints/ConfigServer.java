package net.b07z.sepia.server.assist.endpoints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Setup;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.JSON;
import spark.Request;
import spark.Response;

/**
 * Endpoint to configure server.
 * 
 * @author Florian Quirin
 *
 */
public class ConfigServer {
	
	/**
	 * ---CONFIG SERVER---<br>
	 * End-point to remotely switch certain settings at run-time.
	 */
	public static String run(Request request, Response response){
		//check request origin
		if (!Config.allowGlobalDevRequests){
			if (!SparkJavaFw.requestFromPrivateNetwork(request)){
				JSONObject result = new JSONObject();
				JSON.add(result, "result", "fail");
				JSON.add(result, "error", "Not allowed to access service from outside the private network!");
				return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
			}
		}
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);		//TODO: because of form-post?
		
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}else{
			//create user
			User user = new User(null, token);
			
			//check role
			if (!user.hasRole(Role.superuser)){
				Debugger.println("Unauthorized access attempt to server config! User: " + user.getUserID(), 3);
				return SparkJavaFw.returnNoAccess(request, response);
			}
			
			//soft-restart server
			String restartServer = params.getString("restartServer");
			if (restartServer != null && restartServer.equals("true")){
				JSONObject msg = JSON.make(
						"result", "success",
						"msg", "scheduled server restart, plz wait 10-20s until you continue."
				);
				Start.restartServer(3000l); 	//will run in separate thread after 3s
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
			
			//read and write directly to settings
			String setConfig = params.getString("setConfig");
			String lastSetKey = null;
			boolean changedConfig = false;
			if (setConfig != null){
				try{
					if (setConfig.startsWith("[")){
						JSONArray kvArray = JSON.parseStringToArrayOrFail(setConfig);
						for (Object o : kvArray){
							JSONObject kv = (JSONObject) o;
							String k = (String) kv.keySet().toArray()[0];
							if (!k.isEmpty()){
								Config.replaceSettings(Config.configFile, k, JSON.getString(kv, k));
								Debugger.println("Remotely changed server settings for '" + k + "' (" + Start.serverType + ")", 3);
								lastSetKey = k;
							}
						}
					}else if (setConfig.startsWith("{")){
						JSONObject kv = JSON.parseStringOrFail(setConfig);
						String k = (String) kv.keySet().toArray()[0];
						if (!k.isEmpty()){
							Config.replaceSettings(Config.configFile, k, JSON.getString(kv, k));
							Debugger.println("Remotely changed server settings for '" + k + "' (" + Start.serverType + ")", 3);
							lastSetKey = k;
						}
					}
					changedConfig = true;
					
				}catch (Exception e){
					JSONObject result = new JSONObject();
					JSON.add(result, "result", "fail");
					JSON.add(result, "error", "Failed to write config entry! Invalid data.");
					return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
				}
			}
			String getConfig = params.getString("getConfig");
			if (changedConfig || (getConfig != null)){
				boolean showAll = (getConfig != null && getConfig.equals("all"));
				String singleKey = (getConfig != null && !showAll)? getConfig : (lastSetKey != null)? lastSetKey : ""; 
				try{
					List<String> configFileClean;
					//use actual active settings
					Map<String, String> loadedConfig = Config.getLoadedSettings();
					if (loadedConfig != null){
						configFileClean = new ArrayList<>();
						for (Map.Entry<String, String> es : loadedConfig.entrySet()){
							String k = es.getKey();
							String v = es.getValue();
							//some filters and hide passwords/keys
							if (showAll || k.equals(singleKey)){
								if (v == null || v.trim().isEmpty()) v = "";
								else if (k.matches(".*(_pwd|_secret|_auth_data|_key)")) v = "[HIDDEN]";
								configFileClean.add(k + "=" + v);
							}
						}
					//fallback to file (this probably never happens)
					}else{
						List<String> configFile = FilesAndStreams.readFileAsList(Config.configFile);
						configFileClean = new ArrayList<>();
						for (String l : configFile){
							//some filters and hide passwords/keys
							if (!l.startsWith("#") && (showAll || l.startsWith(singleKey))){
								configFileClean.add(l.replaceFirst("(.*?)(_pwd|_secret|_auth_data|_key)(.*?=)(.+)", "$1$2$3[HIDDEN]"));
							}
						}
					}
					JSONObject msg = JSON.make(
							"config", configFileClean,
							"serverType", Start.serverType,
							"serverName", Config.SERVERNAME
					);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					
				}catch (IOException e){
					JSONObject result = new JSONObject();
					JSON.add(result, "result", "fail");
					JSON.add(result, "error", "Failed to load config file!");
					Debugger.println("Failed to load config file for server config endpoint! - Msg.: " + e.getMessage(), 1);
					return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
				}
			}
			
			//check actions (direct triggers and stuff)
			
			//-answers
			String toggleAnswerModule = params.getString("answers");
			if (toggleAnswerModule != null && toggleAnswerModule.equals("toggle")){
				String newConfigEntryValue = Config.toggleAnswerModule();
				Config.replaceSettings(Config.configFile, "module_answers", newConfigEntryValue);
				Debugger.println("Config - answers module changed by user: " + user.getUserID(), 3);
			}
			//-commands
			String toggleSentencesDB = params.getString("useSentencesDB");
			if (toggleSentencesDB != null && toggleSentencesDB.equals("toggle")){
				if (Config.useSentencesDB){
					Config.useSentencesDB = false;
				}else{
					Config.useSentencesDB = true;
				}
				Config.replaceSettings(Config.configFile, "enable_custom_commands", Boolean.toString(Config.useSentencesDB));
				Debugger.println("Config - loading of DB commands was changed by user: " + user.getUserID(), 3);
			}
			//-email bcc
			String setEmailBCC = params.getString("setEmailBCC");
			if (setEmailBCC != null && setEmailBCC.equals("remove")){
				Config.emailBCC = "";
			}
			//-sdk
			String toggleSdk = params.getString("sdk");
			if (toggleSdk != null && toggleSdk.equals("toggle")){
				if (Config.enableSDK){
					Config.enableSDK = false;
				}else{
					Config.enableSDK = true;
				}
				Config.replaceSettings(Config.configFile, "enable_sdk", Boolean.toString(Config.enableSDK));
				Debugger.println("Config - sdk status changed by user: " + user.getUserID(), 3);
			}
			//-database
			String reloadDB = params.getString("reloadDB");
			String dbReloadMsg = "no-update"; 
			if (reloadDB != null && !reloadDB.isEmpty()){
				String[] dbCmd = reloadDB.split("-");
				if (dbCmd.length == 2){
					if (dbCmd[0].equals("es")){
						if (!dbCmd[1].isEmpty()){
							try{
								Setup.writeElasticsearchMapping(dbCmd[1]);
								dbReloadMsg = "reloaded:" + reloadDB;
							}catch(Exception e){
								dbReloadMsg = "reload-error:" + reloadDB;
							}
						}
					}
				}
			}
			
			JSONObject msg = new JSONObject();
			JSON.add(msg, "answerModule", Config.answerModule);
			JSON.add(msg, "useSentencesDB", Config.useSentencesDB);
			JSON.add(msg, "emailBCC", Config.emailBCC);
			JSON.add(msg, "sdk", Config.enableSDK);
			JSON.add(msg, "dbUpdate", dbReloadMsg);
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}
	}
}
