package net.b07z.sepia.server.assist.endpoints;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.email.SendEmail;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
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
		String client_info = params.getString("client");
		
		//no action
		if (action == null || action.trim().isEmpty()){
			return SparkJavaFw.returnResult(request, response, "no action", 204);
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
				long timeStamp = System.currentTimeMillis();
				String new_token = token.getKeyToken(client_info);
				if (token.getErrorCode() != 0 && !new_token.isEmpty()){
					String msg = "{\"result\":\"fail\",\"error\":\"cannot create token, maybe invalid client info?\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "success");
				JSON.add(msg, "access_level", token.getAccessLevel());
				JSON.add(msg, "keyToken", new_token);
				JSON.add(msg, "keyToken_TS", new Long(timeStamp));
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
				boolean success = token.logoutUser(client_info);
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
		else if (action.trim().equals("forgotPassword")){
			Statistics.add_forgot_pwd_hit();				//hit counter
			
			String userID = params.getString("userid");		//any of the allowed unique IDs, e.g. email address
			String type = params.getString("type");			//type to use for recovery, e.g. "email"
			String language = params.getString("lang");		//language for email
			if (language == null){
				language = LANGUAGES.DE;
			}
			
			//check type
			if (type == null || !type.equals(ID.Type.email)){
				String msg = "{\"result\":\"fail\",\"error\":\"this 'type' to reset password is not supported!\"}";
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
				//check result for user-exists or server communication error
				if (((String) result.get("result")).equals("fail")){
					return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
				}
				//Send via email:
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
					//-log
					Debugger.println("password reset attempt - ID: " + userID + " - timestamp: " + System.currentTimeMillis(), 3);
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
			}
			return SparkJavaFw.returnResult(request, response, "{\"result\":\"fail\",\"error\":\"no valid user ID found!\"}", 200);
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
				JSON.add(in, "userid", userID.trim());		JSON.add(in, "new_pwd", password.trim());
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
			//return returnResult(request, response, "{\"result\":\"fail\",\"error\":\"not yet implemented oO\"}", 200);
			//TODO: improve to operate more like "createUser" and delete ALL user data including the one in ElasticSearch
			
			//authenticate
			Authenticator token = Start.authenticate(params, request);
			if (!token.authenticated()){
				return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
			}else{
				//request info
				String userID = token.getUserID();
				JSONObject info = new JSONObject();
				JSON.add(info, "userid", userID);
				//TODO: i'd prefer to improve this here with additional pwd check
				
				AuthenticationInterface auth = (AuthenticationInterface) ClassBuilder.construct(Config.authenticationModule);
				auth.setRequestInfo(request);
				boolean success = auth.deleteUser(info);
				
				//check result
				if (!success){
					//fail
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "fail");
					JSON.add(msg, "error", "account could not be deleted. Please try again or contact user support!");
					JSON.add(msg, "code", auth.getErrorCode());
					Debugger.println("deleteUser for account: " + userID + " could not finish successfully!?!", 1);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}else{
					//success
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "success");
					JSON.add(msg, "message", "account has been deleted! Goodbye :-(");
					JSON.add(msg, "duration_ms", Debugger.toc(tic));
					Debugger.println("Account: " + userID + " has successfully been deleted :-(", 3);
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
