package net.b07z.sepia.server.assist.messages;

import java.util.Properties;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.endpoints.AssistEndpoint;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.websockets.client.SocketClientHandler;
import net.b07z.sepia.websockets.common.SocketConfig;

/**
 * Class to setup and collect clients handling all kinds of messages, e.g. listening from and sending to a webSocket server.
 *  
 * @author Florian Quirin
 *
 */
public class Clients {
	
	private static long waitBeforeClientStart = 5555;
	private static long maxWaitOnFail = 30000;
	private static long activeClientId = 0l;
	
	public static SocketClientHandler webSocketMessenger;
	public static Thread webSocketMessengerThread;
	
	private static AssistantSocketClient assistantSocket;
	
	/**
	 * Setup and run webSocket messenger. Creates a new thread to maintain the connection.
	 */
	public static void setupSocketMessenger(){
		//setup messenger - TODO: get this directly from webSocketServer
		loadConfig();
		activeClientId++;
		long thisClientId = activeClientId;
		
		webSocketMessengerThread = new Thread(() -> {
			try{ Thread.sleep(waitBeforeClientStart); } catch (InterruptedException e) {}
			int fails = 0;
			long newWait = waitBeforeClientStart;
			while (thisClientId == activeClientId && !pingSocketMessenger()){
				fails++;
				newWait = Math.min(maxWaitOnFail, waitBeforeClientStart * fails); 		//never wait longer than maxWait
				try{ Thread.sleep(newWait); } catch (InterruptedException e) {}
			}
			if (thisClientId == activeClientId){
				//note: assistant name has to be stored in the account just like with normal users
				assistantSocket = new AssistantSocketClient(
					JSON.make(
						"userId", Config.assistantId, "pwd", Config.assistantPwd
					),
					JSON.make(
						AssistEndpoint.InputParameters.client.name(), Config.assistantClientInfo,
						AssistEndpoint.InputParameters.device_id.name(), Config.assistantDeviceId
					)
				);
				webSocketMessenger = new SocketClientHandler(assistantSocket);
				webSocketMessenger.setTryReconnect(true);
				webSocketMessenger.connect();
			}else{
				Debugger.println("Clients: Skipped setup of client with ID: " + thisClientId + " (currently active ID: " + activeClientId + ").", 3);
			}
		});
		webSocketMessengerThread.start();
	}
	/**
	 * Kill webSocket messenger and close thread.
	 */
	public static void killSocketMessenger(){
		activeClientId++; 		//this will abort any pending connections
		if (webSocketMessenger != null){
			Debugger.println("Clients: Closing connection to socket messenger...", 3);
			webSocketMessenger.setTryReconnect(false);
			webSocketMessenger.close();
		}
	}
	
	/**
	 * Get statistics about WebSocket client used for the assistant.
	 */
	public static String getAssistantSocketClientStats(){
		if (assistantSocket != null){
			return assistantSocket.getStats(); 
		}else{
			return "No active assistant sockets.";
		}
	}
	
	/**
	 * WebSockets support duplex communication which means you can send an answer first and after a few seconds send a follow-up
	 * message to add more info/data to the previous reply.
	 * @param nluInput - initial {@link NluInput} to follow-up
	 * @param serviceResult - {@link ServiceResult} as produced by services to send as follow-up
	 * @return true if sent, false if not
	 */
	public static boolean sendAssistantFollowUpMessage(NluInput nluInput, ServiceResult serviceResult){
		if (assistantSocket != null && Config.assistantAllowFollowUps && nluInput.isDuplexConnection()){
			assistantSocket.sendFollowUpMessage(nluInput, serviceResult);
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Ping webSocket messenger to see if we can connect.
	 * @return true if server answered
	 */
	public static boolean pingSocketMessenger(){
		JSONObject res = Connectors.httpGET(SocketConfig.webSocketAPI + "online");
		Debugger.println("Clients: Checking connection to socket messenger...", 3);
		if (Connectors.httpSuccess(res)){
			Debugger.println("Clients: Socket messenger found.", 3);
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Get config for webSocketServer.
	 */
	private static void loadConfig(){
		try{
			Properties settings = FilesAndStreams.loadSettings(Config.configFile);
			//server
			SocketConfig.assistAPI = Config.endpointUrl;								//link to Assistant-API e.g. for authentication
			SocketConfig.webSocketEP = settings.getProperty("server_webSocket");		//link to webSocket server
			SocketConfig.webSocketSslEP = settings.getProperty("server_webSocketSSL");	//link to secure webSocket server
			SocketConfig.webSocketAPI = settings.getProperty("server_webSocket_api");	//link to webSocket API (support API for socket server)
			
			Debugger.println("loading webSocket settings from " + Config.configFile + "... done." , 3);
		}catch (Exception e){
			Debugger.println("loading webSocket settings from " + Config.configFile + "... failed!" , 1);
		}
	}
}
