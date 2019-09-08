package net.b07z.sepia.server.assist.messages;

import java.util.ArrayList;
import java.util.List;
import net.b07z.sepia.websockets.common.SocketMessage;

/**
 * Class to handle message queues.
 * 
 * @author Florian Quirin
 *
 */
public class Queues {
	
	private static List<SocketMessage> socketMessagesToSend = new ArrayList<>();
	
	public static List<SocketMessage> getSocketMessagesToSend(){
		return socketMessagesToSend;
	}
	
	public static void addSocketMessageToSend(SocketMessage message){
		//TODO: not yet fully implemented so we better deactivate it for now 
		//socketMessagesToSend.add(message);
	}
	
	public static void removeSocketMessageToSend(SocketMessage message){
		//TODO: is this working or does it have to be more complex with message id?
		socketMessagesToSend.remove(message);
	}

}
