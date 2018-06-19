package net.b07z.sepia.server.assist.endpoints;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Tools;
import spark.Request;
import spark.Response;

/**
 * Endpoint to access certain admin-services of the server itself like white-list management for user accounts.
 * 
 * @author Florian Quirin
 *
 */
public class UserManagementEndpoint {

	/**
	 * ---Server SERVICES API---<br>
	 * Handle e.g. white-list management, roles for user accounts, direct creation and deletion of users etc.<br>
	 * Not to be confused with service modules (I agree the naming was a bad choice here ;-)).
	 */
	public static String userManagementAPI(Request request, Response response){
		//check request origin
		if (!Config.allowGlobalDevRequests){
			if (!SparkJavaFw.requestFromPrivateNetwork(request)){
				JSONObject result = new JSONObject();
				JSON.add(result, "result", "fail");
				JSON.add(result, "error", "Not allowed to access service from outside the private network!");
				return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
			}
		}
		//get parameters (or throw error)
		RequestParameters params = new RequestPostParameters(request);
		
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}else{
			//get service
			String service = params.getString("service");
			String action = params.getString("action");
			
			//validate request
			if (service == null || action == null){
				String msg = "{\"result\":\"fail\",\"error\":\"parameters are missing or invalid! (service, action)\"}";
				return SparkJavaFw.returnResult(request, response, msg, 200);
			}
			
			//Whitelist service
			if (service.equals("whitelist") && action.equals("addUser")){
				//create user
				User user = new User(null, token);
				
				//check role
				if (!user.hasRole(Role.superuser) && !user.hasRole(Role.inviter)){
					Debugger.println("Access denied to service whitelist! User: " + user.getUserID() + " is missing role.", 3);
					return SparkJavaFw.returnNoAccess(request, response);
				}
				//add user to list
				String email = params.getString("email");
				int code;
				if (email == null){
					String msg = "{\"result\":\"fail\",\"error\":\"parameters are missing or invalid! (email)\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}else{
					code = DB.saveWhitelistUserEmail(email);
				}
				//success?
				if (code != 0){
					String msg = "{\"result\":\"fail\",\"error\":\"user could not be added!\",\"code\":\"" + code + "\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}else{
					Debugger.println("Whitelist user added! User: " + email + " added by: " + user.getUserID(), 3);
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "success");
					JSON.add(msg, "added", email);
					JSON.add(msg, "by", user.getUserID());
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}
			
			//User-role service
			}else if (service.equals("roles")){
				//create user
				User user = new User(null, token);
				
				//check role
				if (!user.hasRole(Role.superuser) && !user.hasRole(Role.chiefdev)){
					Debugger.println("Access denied to service roles! User: " + user.getUserID() + " is missing role.", 3);
					return SparkJavaFw.returnNoAccess(request, response);
				}
				
				//data
				JSONObject data = params.getJson("data");
				if (data == null){
					String msg = "{\"result\":\"fail\",\"error\":\"parameters are missing or invalid! (data)\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}
				
				//set
				if (action.equals("setRoles")){
					//new roles and target user
					JSONArray roles = JSON.getJArray(data, "roles");
					String userId = JSON.getString(data, "userId");
					if (Is.nullOrEmpty(userId) || Is.nullOrEmpty(roles)){
						String msg = "{\"result\":\"fail\",\"error\":\"parameters are missing or invalid! (userId, roles)\"}";
						return SparkJavaFw.returnResult(request, response, msg, 200);
					}
					//check roles
					for (Object o : roles){
						String role = o.toString();
						if (role.equals(Role.superuser.name()) || role.equals(Role.assistant.name())){
							String msg = "{\"result\":\"fail\",\"error\":\"Request is invalid, CANNOT set core-roles!\"}";
							return SparkJavaFw.returnResult(request, response, msg, 200);
						}else if (!Tools.enumContains(Role.values(), role)){
							String msg = "{\"result\":\"fail\",\"error\":\"Request is invalid, " + role + " is not a known role!\"}";
							return SparkJavaFw.returnResult(request, response, msg, 200);
						}
					}
					//call method
					boolean success;
					try {
						success = DB.writeAccountDataDirectly(userId, JSON.make(ACCOUNT.ROLES, roles));
					} catch (Exception e) {
						success = false;
						Debugger.println("userManagement - roles - " + e.getMessage(), 1);
						Debugger.printStackTrace(e, 3);
					}
					if (!success){
						String msg = "{\"result\":\"fail\",\"error\":\"Somethig went wrong during writing of user-roles, sorry!\"}";
						return SparkJavaFw.returnResult(request, response, msg, 200);
						
					}else{
						Debugger.println("User '" + user.getUserID() + "' set new roles for user: " + userId + 
								" - Roles: " + roles, 3);
						JSONObject msg = new JSONObject();
						JSON.add(msg, "result", "success");
						JSON.add(msg, "roles", roles);
						JSON.add(msg, "target", userId);
						JSON.add(msg, "by", user.getUserID());
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
					
				//get
				}else if (action.equals("getRoles")){
					//target user
					String userId = JSON.getString(data, "userId");
					if (Is.nullOrEmpty(userId)){
						String msg = "{\"result\":\"fail\",\"error\":\"parameters are missing or invalid! (userId)\"}";
						return SparkJavaFw.returnResult(request, response, msg, 200);
					}
					//call
					Debugger.println("User '" + user.getUserID() + "' requested roles of user: " + userId, 3);
					List<String> roles = DB.readUserRolesDirectly(userId);
					if (roles == null || roles.isEmpty()){
						String msg = "{\"result\":\"fail\",\"error\":\"Somethig went wrong during reading of user-roles, sorry!\"}";
						return SparkJavaFw.returnResult(request, response, msg, 200);
						
					}else{
						JSONObject msg = new JSONObject();
						JSON.add(msg, "result", "success");
						JSON.add(msg, "roles", roles);
						JSON.add(msg, "target", userId);
						JSON.add(msg, "by", user.getUserID());
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
					}
				}
			
			//User management (create directly from scratch etc.)
			}else if (service.equals("users")){
				//create user
				User user = new User(null, token);
				
				//check role
				if (!user.hasRole(Role.superuser)){
					Debugger.println("Access denied to service roles! User: " + user.getUserID() + " is missing role.", 3);
					return SparkJavaFw.returnNoAccess(request, response);
				}
				
				//data
				JSONObject data = params.getJson("data");
				if (data == null){
					String msg = "{\"result\":\"fail\",\"error\":\"parameters are missing or invalid! (data)\"}";
					return SparkJavaFw.returnResult(request, response, msg, 200);
				}
				
				//create
				if (action.equals("create")){
					//email and password
					String email = JSON.getString(data, "email");
					String pwd = JSON.getString(data, "pwd");
					if (Is.nullOrEmpty(email) || Is.nullOrEmpty(pwd)){
						String msg = "{\"result\":\"fail\",\"error\":\"parameters are missing or invalid! (email, pwd)\"}";
						return SparkJavaFw.returnResult(request, response, msg, 200);
					}
					//call
					Debugger.println("User '" + user.getUserID() + "' requested creation of new user: " + email, 3);
					try {
						JSONObject newUser = DB.createUserDirectly(email, pwd);
						//done
						JSONObject msg = new JSONObject();
						JSON.add(msg, "result", "success");
						JSON.add(msg, "guuid", newUser.get(ACCOUNT.GUUID));
						JSON.add(msg, "by", user.getUserID());
						return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
						
					} catch (Exception e) {
						String msg = JSON.make("result", "fail", "error", "Creation of new user failed!", 
								"msg", e.getMessage()).toJSONString();
						Debugger.println("userManagement - create - " + e.getMessage(), 1);
						Debugger.printStackTrace(e, 3);
						return SparkJavaFw.returnResult(request, response, msg, 200);
					}
				}
			}
			
			//no valid service or faulty parameters
			String msg = "{\"result\":\"fail\",\"error\":\"parameters are invalid!\"}";
			return SparkJavaFw.returnResult(request, response, msg, 200);
		}
	}

}
