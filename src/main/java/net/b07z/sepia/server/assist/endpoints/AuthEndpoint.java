package net.b07z.sepia.server.assist.endpoints;

import java.util.List;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.email.SendEmail;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.users.Account;
import net.b07z.sepia.server.core.users.AuthenticationInterface;
import spark.Request;
import spark.Response;

/**
 * API endpoint to handle authentication.
 *  
 * @author Florian Quirin
 *
 */
public class AuthEndpoint {
	
	//temporary token is valid for
	public static final long TEMP_TOKEN_VALID_FOR = 300000l; 	//5minutes
	//see 'Authenticator' and 'AuthenticationInterface' implementations for other token times (usually key-token is 24h and app token 1y)
	
	/**
	 * Input parameters required for authentication. Usually combined with {@link AssistEndpoint.InputParameters}. 
	 */
	public static enum InputParameters {
		PWD,
		GUUID,
		KEY,
		client		//NOTE: this is identical to AssistEndpoint.InputParameters.client
	}
	
	/**
	 * This class can be used to create and restore a temporary token. A temporary token is issued by the
	 * server to validate a request that follows within the next ~5min and can only be checked with secret
	 * server info.
	 */
	public static class TemporaryToken {
		public String userId;
		public List<String> userRoles;
		public long timestamp;
		public String tKey;
		public String tKeySubmitted;
		/**
		 * Get the expected temporary token (from the submitted parameters and the server specific secrets)
		 * and compare its 'tKey' to the submitted data.
		 * @param params - server input parameters (e.g. from POST request)
		 * @throws Exception
		 */
		public TemporaryToken(RequestParameters params) throws Exception{
			JSONObject tToken = params.getJson("tToken"); 		//MUST BE SAME AS NOTED IN getJson()
			if (tToken != null){
				this.userId = JSON.getString(tToken, "userId");
				this.userRoles = Converters.jsonArrayToStringList(JSON.getJArray(tToken, "userRoles"));
				this.timestamp = JSON.getLongOrDefault(tToken, "timestamp", 0);
				this.tKey = Account.getTemporaryValidationToken(
						userId, userRoles, 
						Config.clusterKeyLight, String.valueOf(timestamp), 
						Config.cklHashIterations
				);
				this.tKeySubmitted = JSON.getString(tToken, "tKey");
			}
		}
		/**
		 * Create a temporary, short-lived token from some user data that can be used to make an "allow"
		 * request to the authentication endpoint. An "allow" request is the simplest form of authentication
		 * that can be used to confirm that an user is allowed to do a certain action because he has been 
		 * given the rights temporary by the server. Usually the token is valid for ~5 minutes. The token can
		 * only be validated with server specific secret info.
		 * @param user - user data
		 * @throws Exception
		 */
		public TemporaryToken(User user) throws Exception{
			this.userId = user.getUserID();
			this.userRoles = user.getUserRoles();
			this.timestamp = System.currentTimeMillis();
			this.tKey = Account.getTemporaryValidationToken(
					userId, userRoles, 
					Config.clusterKeyLight, String.valueOf(timestamp), 
					Config.cklHashIterations
			);
		}
		/**
		 * Add this to the request body as key "tToken" (e.g. JSON.put(reqBody, "tToken", thisJson)) when you want to use the 
		 * temp. token to authenticate a call to the "allow" section of this endpoint.
		 */
		public JSONObject getJson(){
			return JSON.make(
					"userId", userId,
					"userRoles", userRoles,
					"timestamp", timestamp,
					"tKey", tKey
			);
		}
		/**
		 * Is the token still valid? (checks e.g. if the timestamp is 'fresh' enough).
		 */
		public boolean isStillValid() {
			return (Is.notNullOrEmpty(this.tKey) &&
					((System.currentTimeMillis() - this.timestamp) < TEMP_TOKEN_VALID_FOR));
		}
		/**
		 * Is the submitted token the same as the expected token?
		 */
		public boolean isSameAsSubmitted(){
			if (Is.notNullOrEmpty(this.tKeySubmitted)){
				//NOTE: this will only work if tKeySubmitted was generated with SAME CLUSTER KEY!
				if (isStillValid() && this.tKey.equals(this.tKeySubmitted)){
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * ---AUTHENTICATION API---<br>
	 * Endpoint that handles the user creation, authentication process, login, logout, password reset, etc.<br>
	 * Returns e.g. a JSON object with login-token or registration link.
	 */
	public static String authenticationAPI(Request request, Response response) {
		long tic = System.currentTimeMillis();
		
		//get parameters (or throw error)
		RequestParameters params = new RequestPostParameters(request);
		
		//get action - validate/logout/createUser/deleteUser
		String action = params.getString("action");
		String clientInfo = params.getString("client");
		
		//no action
		if (action == null || action.trim().isEmpty()){
			return SparkJavaFw.returnResult(request, response, "no action", 204);
		}
		//get permission by server to execute a certain task by validating the given information using local cluster key (light)
		else if (action.trim().equals("allow")){
			//authenticate via temporary token
			try{
				TemporaryToken tToken = new TemporaryToken(params);
				//NOTE: this will only work if tKeyInput was generated with SAME CLUSTER KEY!
				if (tToken.isSameAsSubmitted()){
					//success
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "success");
					JSON.add(msg, "access_level", 0);
					JSON.add(msg, "request", "allowed");
					//basic info is accepted
					JSON.add(msg, Authenticator.GUUID, tToken.userId);
					JSON.add(msg, Authenticator.USER_ROLES, tToken.userRoles);
					JSON.add(msg, "duration_ms", Debugger.toc(tic));
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}else{
					return SparkJavaFw.returnNoAccess(request, response);
				}
			}catch (Exception e){
				e.printStackTrace();
				return SparkJavaFw.returnNoAccess(request, response);
			}
		}
		//check user - this is mainly a service for other APIs - basically same as validate but without token generation
		else if (action.trim().equals("check")){
			//authenticate
			Authenticator token = Start.authenticate(params, request);
			if (!token.authenticated()){
				return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
			}else{
				//success
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "success");
				JSON.add(msg, "access_level", token.getAccessLevel());
				//basic info
				token.addBasicInfoToJsonObject(msg);
				JSON.add(msg, "duration_ms", Debugger.toc(tic));
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
		//validate user and create new token
		else if (action.trim().equals("validate")){
			//authenticate
			Authenticator token = Start.authenticate(params, request);
			if (!token.authenticated()){
				return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
			}else{
				//success: make a new token and save it in the database 
				//TODO: restrict this to access level 1 (not 0)? - the client currently uses the token for refresh as well
				long timeStamp = System.currentTimeMillis();
				String newToken = token.getKeyToken(clientInfo);
				long validUntil = token.getKeyTokenValidTime();
				if (token.getErrorCode() != 0 && !newToken.isEmpty()){
					String msg = "{\"result\":\"fail\",\"error\":\"cannot create token, maybe invalid client info?\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "success");
				JSON.add(msg, "access_level", token.getAccessLevel());
				JSON.add(msg, "keyToken", newToken);
				JSON.add(msg, "keyToken_TS", timeStamp);
				JSON.add(msg, "validUntil", validUntil);
				JSON.add(msg, "duration_ms", Debugger.toc(tic));
				//basic info
				token.addBasicInfoToJsonObject(msg);
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
		//logout
		else if (action.trim().equals("logout")){
			//authenticate
			Authenticator token = Start.authenticate(params, request);
			if (!token.authenticated()){
				return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
			}else{
				//success: logout user
				boolean success = token.logoutUser(clientInfo);
				JSONObject msg = new JSONObject();
				if (success){
					JSON.add(msg, "result", "success");
					JSON.add(msg, "msg", "user successfully logged out.");
				}else{
					JSON.add(msg, "result", "fail");
					JSON.add(msg, "error", "user logout failed!");
					JSON.add(msg, "code", token.getErrorCode());
				}
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
		//logout
		else if (action.trim().equals("logoutAllClients")){
			//authenticate
			Authenticator token = Start.authenticate(params, request);
			if (!token.authenticated()){
				return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
			}else{
				//success: logout user
				boolean success = token.logoutAllClients();
				JSONObject msg = new JSONObject();
				if (success){
					JSON.add(msg, "result", "success");
					JSON.add(msg, "msg", "successfully logged out from all clients.");
				}else{
					JSON.add(msg, "result", "fail");
					JSON.add(msg, "error", "logout from all clients failed!");
					JSON.add(msg, "code", token.getErrorCode());
				}
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
		
		//register new user
		else if (action.trim().equals("register")){
			Statistics.add_Registration_hit();				//hit counter
			
			String userID = params.getString("userid");		//any of the allowed unique IDs, e.g. email address
			String type = params.getString("type");			//type of registration, e.g. "email"
			String language = params.getString("lang");		//language for email
			if (language == null){
				language = LANGUAGES.DE;
			}
			
			//check type
			if (type == null || !type.equals(ID.Type.email)){
				String msg = "{\"result\":\"fail\",\"error\":\"'type' of registration not supported!\"}";
				return SparkJavaFw.returnResult(request, response, msg, 200);
			}
			
			//V1: Email registration:
			
			//user id must be at least 4 chars, better: email
			if (userID != null && userID.length() > 4 && userID.contains("@")){
				String email = userID.trim();
				//-log
				Debugger.println("registration attempt - ID: " + email + " - timestamp: " + System.currentTimeMillis(), 3);
				
				AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
				auth.setRequestInfo(request);
				JSONObject result = auth.registrationByEmail(email);
				//check result for user-exists or server communication error
				if (((String) result.get("result")).equals("fail")){
					return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
				}
				//Send via email:
				//-create message
				SendEmail emailClient = (SendEmail) ClassBuilder.construct(Config.emailModule);
				
				String subject = "Please confirm your e-mail address and off we go";
				if (language.equals(LANGUAGES.DE)){
					subject = "Bitte bestätige deine E-Mail Adresse und los geht's";
				}
				
				String message = emailClient.loadDefaultRegistrationMessage(language, email,
									(String) result.get("ticketid"),
									(String) result.get("token"),
									(String) result.get("time")
				);
				//-send
				int code = emailClient.send(email, message, subject, null);
	
				//-check result
				if (code == 0){
					//-overwrite token and return
					JSON.add(result, "token", "sent via email to " + userID);
					return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
				}else{
					//-error
					if (code == 1){
						return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"could not send email! Invalid address? Server problem?\",\"code\":\"418\"}", 200);
					}else if (code == 2){
						return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"could not send email! Invalid address?\",\"code\":\"422\"}", 200);
					}else{
						return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"could not send email! Server problem?\",\"code\":\"500\"}", 200);
					}
				}
			}
			return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"no valid user ID found!\",\"code\":\"422\"}", 200);
		}
		//create new user
		else if (action.trim().equals("createUser")){
			String userID = params.getString("userid");
			String password = params.getString("pwd");
			String token = params.getString("token");
			String type = params.getString("type");
			String timestamp = params.getString("time");
			String ticketID = params.getString("ticketid");
			if (userID != null && ticketID != null && password != null && token != null && timestamp != null && type != null){
				AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
				auth.setRequestInfo(request);
				JSONObject in = new JSONObject();
				JSON.add(in, "userid", userID.trim());		JSON.add(in, "pwd", password.trim());
				JSON.add(in, "token", token.trim());		JSON.add(in, "type", type);	
				JSON.add(in, "time", timestamp);			JSON.add(in, "ticketid", ticketID);
				boolean success = auth.createUser(in);
				if (success){
					String msg = "{\"result\":\"success\",\"msg\":\"new user created\"}";
					//-log
					Debugger.println("new user created - ID: " + userID + " - timestamp: " + System.currentTimeMillis(), 3);
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}else{
					String msg = "{\"result\":\"fail\",\"error\":\"failed to create user!\",\"code\":\"" + auth.getErrorCode() + "\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}
			}else{
				String msg = "{\"result\":\"fail\",\"error\":\"401 not authorized to create user (wrong token? missing parameters?)!\",\"code\":\"" + "2" + "\"}";
				return SparkJavaFw.returnResult(request, response, msg, 200);
			}
		}
		
		//request password change
		else if (action.trim().equals("forgotPassword") || action.trim().equals("requestPasswordChange")){
			Statistics.add_forgot_pwd_hit();				//hit counter
			
			String userID = params.getString("userid");		//any of the allowed unique IDs, e.g. email address
			String type = params.getString("type");			//type to use for recovery, e.g. "email"
			String language = params.getString("lang");		//language for email
			if (language == null){
				language = LANGUAGES.EN;
			}
			boolean sendEmail = false;
			boolean superUserOverwrite = false;
			String superUserId = "";
			
			//check type
			if (Is.nullOrEmpty(type) || Is.nullOrEmpty(userID)){
				String msg = "{\"result\":\"fail\",\"error\":\"request 'type' (e.g. email, oldPassword) or 'userid' is missing!\"}";
				return SparkJavaFw.returnResult(request, response, msg, 200);
			
			}else if (type.equals(ID.Type.email)){
				sendEmail = true;
				
			}else if (type.equals("oldPassword")){
				//get extra authentication
				String authKey = params.getString("authKey");
				RequestParameters authParams = new RequestPostParameters(JSON.make(
						AuthEndpoint.InputParameters.KEY.name(), authKey,
						AuthEndpoint.InputParameters.client.name(), "tmp_web_app" 	//choosing a client that will usually not be used in any real client
				));
				Authenticator token = Start.authenticate(authParams, request);
				String email = null;
				if (!token.authenticated() || !token.getUserID().equals(userID.toLowerCase())){
					//check requesting user
					Authenticator reqUserToken = Start.authenticate(params, request);
					if (reqUserToken.authenticated()){
						User reqUser = new User(null, reqUserToken);
						if (reqUser.hasRole(Role.superuser)){
							//SUPERUSER ACCESS
							email = (String) DB.getAccountInfos(userID, ACCOUNT.EMAIL).get(ACCOUNT.EMAIL);
							superUserOverwrite = true;
							superUserId = reqUser.getUserID();
						}else{
							return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
						}
					}else{
						return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
					}
				}else{
					//check access level and get email
					if (token.getAccessLevel() >= 1){ 		//this means: logged in with 'real' password
						email = (String) token.getBasicInfo().get(Authenticator.EMAIL);
					}
				}
				if (Is.nullOrEmpty(email)){
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail",
							"error", "missing basic info of user account: email!"
					).toJSONString(), 200);
				}
				
				//revert back to email but only transfer data (don't send an actual mail)
				type = ID.Type.email;
				userID = email;
				sendEmail = false;
			
			}else{
				String msg = "{\"result\":\"fail\",\"error\":\"this 'type' to reset password is not supported (try 'email' or 'oldPassword')!\"}";
				return SparkJavaFw.returnResult(request, response, msg, 200);
			}
			
			//V1: Email recovery:
			
			//user id must be at least 4 chars, better: email
			if (userID != null && userID.length() > 4  && userID.contains("@")){
				String email = userID.trim();
				
				AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
				auth.setRequestInfo(request);
				JSONObject in = new JSONObject();
				JSON.add(in, "userid", email);		JSON.add(in, "type", type);
				JSONObject result = auth.requestPasswordChange(in);
				//-log
				if (superUserOverwrite){
					Debugger.println("password reset attempt by SUPERUSER '" + superUserId + "' for ID: " + userID + " - timestamp: " + System.currentTimeMillis(), 3);
				}else{
					Debugger.println("password reset attempt - ID: " + userID + " - timestamp: " + System.currentTimeMillis(), 3);
				}
				//check result for user-exists or server communication error
				if (((String) result.get("result")).equals("fail")){
					return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
				}
				//Send via email:
				if (sendEmail){
					//-create message
					SendEmail emailClient = (SendEmail) ClassBuilder.construct(Config.emailModule);
					
					String subject = "Here is the link to change your password";
					if (language.equals(LANGUAGES.DE)){
						subject = "Hier der Link zum Ändern deines Passworts";
					}
					
					String message = emailClient.loadPasswordResetMessage(language, email,
										(String) result.get("ticketid"),
										(String) result.get("token"),
										(String) result.get("time")
					);
					//-send
					int code = emailClient.send(email, message, subject, null);
		
					//-check result
					if (code == 0){
						//-overwrite token and return
						JSON.add(result, "token", "sent via email to " + userID);
						return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
					}else{
						//-error
						if (code == 1){
							return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"could not send email! Invalid address? Server problem?\"}", 200);
						}else if (code == 2){
							return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"could not send email! Invalid address?\"}", 200);
						}else{
							return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"could not send email! Server problem?\"}", 200);
						}
					}
					
				//Return token via result
				}else{
					return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
				}
			}else{
				return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"no valid user ID found!\"}", 200);
			}
		}
		//change password
		else if (action.trim().equals("changePassword")){
			String userID = params.getString("userid");
			String password = params.getString("new_pwd");
			String token = params.getString("token");
			String type = params.getString("type");
			String timestamp = params.getString("time");
			String ticketID = params.getString("ticketid");
			if (userID != null && password != null && token != null && timestamp != null && type != null){
				AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
				auth.setRequestInfo(request);
				JSONObject in = new JSONObject();
				JSON.add(in, "userid", userID.trim());		JSON.add(in, "new_pwd", password.trim()); 		//NOTE: new_pwd has to be client-hashed!
				JSON.add(in, "token", token.trim());		JSON.add(in, "type", type);	
				JSON.add(in, "time", timestamp);			JSON.add(in, "ticketid", ticketID);
				boolean success = auth.changePassword(in);
				if (success){
					String msg = "{\"result\":\"success\",\"msg\":\"new password has been set.\"}";
					//-log
					Debugger.println("password reset - ID: " + userID + " - timestamp: " + System.currentTimeMillis(), 3);
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}else{
					String msg = "{\"result\":\"fail\",\"error\":\"failed to change password!\",\"code\":\"" + auth.getErrorCode() + "\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}
			}else{
				String msg = "{\"result\":\"fail\",\"error\":\"401 not authorized to change password (token?)!\",\"code\":\"" + "2" + "\"}";
				return SparkJavaFw.returnResult(request, response, msg, 200);
			}
		}
		
		//delete user
		else if (action.trim().equals("deleteUser")){
			//TODO: improve to operate more like "createUser"?
			//TODO: Delete ALL user data (commands, services, answers, etc.) not just the account !!
			
			//authenticate
			Authenticator token = Start.authenticate(params, request);
			if (!token.authenticated()){
				return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
			}else{
				//create user and get IDs
				User user = new User(null, token);
				String userId = token.getUserID();
				String userIdToBeDeleted = params.getString("userid");		//NOTE: this parameter is called "userid" not "userId" ... O_o
				if (Is.notNullOrEmpty(userIdToBeDeleted)){
					userIdToBeDeleted = ID.clean(userIdToBeDeleted);
					//superusers are allowed to use this...
					if (user.hasRole(Role.superuser)){
						userId = userIdToBeDeleted;
					}else{
						return SparkJavaFw.returnResult(request, response, JSON.make(
								"result", "fail", 
								"error", "must be 'superuser' to use parameter 'userid'!"
						).toJSONString(), 200);
					}
				}
							
				//prevent deletion of core accounts
				if (userId.equals(Config.superuserId) || userId.equals(Config.assistantId)){
					Debugger.println("deleteUser for account: " + userId + " FAILED. Core account cannot be deleted! As 'superuser' use parameter 'userid' to delete accounts.", 1);
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "account cannot be deleted! As 'superuser' use parameter 'userid' to delete users"
					).toJSONString(), 200);
				}
				
				//request info
				JSONObject info = JSON.make("userid", userId);
				//TODO: i'd prefer to improve this here with additional pwd check
				
				AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
				auth.setRequestInfo(request);
				boolean success = auth.deleteUser(info);
				
				//check result
				if (!success){
					//fail
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "fail");
					JSON.add(msg, "error", "account '" + userId + "' could not be deleted. Please try again or contact user support!");
					JSON.add(msg, "code", auth.getErrorCode());
					Debugger.println("deleteUser for account: " + userId + " could not finish successfully!?!", 1);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}else{
					//success
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "success");
					JSON.add(msg, "message", "account has been deleted! Goodbye " + userId + " :-(");
					JSON.add(msg, "duration_ms", Debugger.toc(tic));
					Debugger.println("deleteUser: " + userId + " has successfully been deleted :-(", 3);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}
			}
		}
		//no action
		else{
			return SparkJavaFw.returnResult(request, response, "", 204);
		}
	}
	
	

}
