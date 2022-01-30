package net.b07z.sepia.server.assist.endpoints;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.SharedAccess;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.users.SharedAccessItem;
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
			
			//get action type
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
			
			//validate parameters
			if (type == null || type.isEmpty() || action == null || action.isEmpty()){
				//FAIL
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "'type' and/or 'action' missing or invalid");
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
			
			//cross-account actions?
			if (Is.nullOrEmpty(receiver)){
				//simple access (own account)
				receiver = sender;
				
			}else if (!receiver.equals(sender)){
				long tic = Debugger.tic(); 
				//check if access is allowed
				boolean allowAccess = SharedAccess.checkPermissions(receiver, SharedAccess.DT_REMOTE_ACTIONS, 
						new SharedAccessItem(sender, targetDeviceId.equals("<auto>")? null : targetDeviceId, JSON.make("type", type)));		
						//TODO: add 'action' to 'details'?
				//allow?
				if (allowAccess){
					originalSender = sender;
				}else{
					Debugger.println("RemoteAction: User '" + sender + "' requested access to '" + receiver 
							+ "' but was NOT allowed (or client was offline)!", 1);
					Statistics.addOtherApiHit("RemoteAction shared-access failed");
					Statistics.addOtherApiTime("RemoteAction shared-access failed", Debugger.toc(tic));
					//FAIL
					JSONObject msg = new JSONObject();
					JSON.add(msg, "result", "fail");
					JSON.add(msg, "error", "'receiver' ID not allowed or no client online. Request has been logged.");
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 403);
				}
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
