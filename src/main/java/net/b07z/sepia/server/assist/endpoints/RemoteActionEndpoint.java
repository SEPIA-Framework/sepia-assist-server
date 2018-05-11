package net.b07z.sepia.server.assist.endpoints;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.messages.Queues;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.websockets.common.SocketMessage;
import net.b07z.sepia.websockets.common.SocketMessage.DataType;
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
			
			//get action
			String type =  params.getString("type");			//e.g.: RemoteActionType.hotkey.name()
			//get action info
			String action =  params.getString("action");
			//get channel
			String channelId =  params.getString("channelId");
			if (channelId == null || channelId.isEmpty())	channelId = "<auto>";
			//get device
			String deviceId =  params.getString("deviceId");
			if (deviceId == null || deviceId.isEmpty())		deviceId = "<auto>";
			
			//validate
			if (type == null || type.isEmpty() || action == null || action.isEmpty()){
				//FAIL
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "'type' and 'action' missing or invalid");
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
	
			JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "info", "Action received");
			
			//try to broadcast to socket messenger
			JSONObject data = JSON.make("dataType", DataType.remoteAction.name(), 
					"user", token.getUserID(),
					"targetDeviceId", deviceId,
					"type", type, "action", action);
			SocketMessage sMessage = new SocketMessage(channelId, token.getUserID(), token.getUserID(), data);		//TODO: select proper parameters? Receiver will be set by server depending on action
			boolean msgSent = Clients.webSocketMessenger.send(sMessage, 2000);
			if (!msgSent){
				Queues.addSocketMessageToSend(sMessage);
				Debugger.println("RemoteAction: failed to send message to webSocket messenger!", 1);
			}
			
			return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
		}
	}

}
