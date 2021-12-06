package net.b07z.sepia.server.assist.endpoints;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import spark.Request;
import spark.Response;

/**
 * Endpoint to handle remote actions and broadcasting to other devices.
 * 
 * @author Florian Quirin
 *
 */
public class RemoteActionEndpoint {

	/**
	 * ---REMOTE ACTIONS---<br>
	 * End-point to submit remote actions that are broadcasted to other devices.
	 */
	public static String remoteActionAPI(Request request, Response response){
		//Requirements
		if (!Config.connectToWebSocket){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "Remote actions not enabled on this server");
			return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
		}
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);
		
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response);
		}else{
			//stats
			Statistics.addOtherApiHit("RemoteAction endpoint");
			Statistics.addOtherApiTime("RemoteAction endpoint", 1);
			
			//sender and receiver
			String sender = token.getUserID();
			String receiver = params.getString("receiver");
			String originalSender = null;
			//cross-account actions?
			if (Is.nullOrEmpty(receiver)){
				receiver = sender;
			}else if (!receiver.equals(sender)){
				//check if access is allowed
				boolean allowAccess = false;
				Map<String, Object> accPermsData = DB.getAccountInfos(receiver, ACCOUNT.SHARED_ACCESS_PERMISSIONS);
				JSONObject accPerms = (accPermsData != null)? ((JSONObject) accPermsData.get(ACCOUNT.SHARED_ACCESS_PERMISSIONS)) : null;
				if (accPerms != null && accPerms.containsKey("remoteActions")){
					JSONArray remoteAccPerms = (JSONArray) accPerms.get("remoteActions");
					//System.out.println("remoteAccPerms: " + remoteAccPerms);		//DEBUG
					if (remoteAccPerms != null){
						for (Object obj : remoteAccPerms){
							String allowedUser = JSON.getString((JSONObject) obj, "user");
							if (allowedUser != null && allowedUser.equals(sender)){
								//allow sender to execute RA for receiver
								allowAccess = true;
								originalSender = sender;
								break;
							}
						}
					}
				}
				//allow?
				if (!allowAccess){
					//FAIL
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "fail");
					JSON.add(msg, "error", "'receiver' ID not allowed");
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 403);
				}
			}
			
			//get action
			String type = params.getString("type");			//e.g.: RemoteActionType.hotkey.name()
			//get action info
			String action = params.getString("action");
			//get target channel
			String targetChannelId = params.getString("targetChannelId");
			if (targetChannelId == null || targetChannelId.isEmpty())	targetChannelId = "<auto>";
			//get target device
			String targetDeviceId = params.getString("targetDeviceId");
			if (targetDeviceId == null || targetDeviceId.isEmpty())		targetDeviceId = "<auto>";
			String skipDeviceId = params.getString("skipDeviceId");
			
			//validate
			if (type == null || type.isEmpty() || action == null || action.isEmpty()){
				//FAIL
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "'type' and 'action' missing or invalid");
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
			
			//send socket message
			/*
			SocketMessage sMessage = new SocketMessage("<auto>", Config.assistantId, Config.assistantDeviceId, "", "", data);
			boolean msgSent = Clients.webSocketMessenger.send(sMessage, 2000);
			*/
			boolean msgSent = Clients.sendAssistantRemoteAction(receiver, 
					type, action, targetDeviceId, targetChannelId, skipDeviceId, originalSender
			);
			
			JSONObject msg;		//response to api call
			if (!msgSent){
				//Queues.addSocketMessageToSend(sMessage);		//a: remote actions should probably not be delayed, b: queue is not implemented yet
				Debugger.println("RemoteAction: failed to send message to webSocket messenger!", 1);
				msg = JSON.make(
						"result", "fail", 
						"error", "Socket message could not be delivered - Unknown error"
				);
			}else{
				msg = JSON.make(
						"result", "success", 
						"info", "Action sent"
				);
			}
			
			//respond
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}
	}
	
}
