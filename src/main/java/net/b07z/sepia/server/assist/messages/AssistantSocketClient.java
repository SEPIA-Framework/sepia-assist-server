package net.b07z.sepia.server.assist.messages;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.endpoints.AssistEndpoint;
import net.b07z.sepia.server.assist.endpoints.AssistEndpoint.InputParameters;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.core.server.FakeRequest;
import net.b07z.sepia.server.core.server.FakeResponse;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.websockets.client.SepiaSocketClient;
import net.b07z.sepia.websockets.common.SocketConfig;
import net.b07z.sepia.websockets.common.SocketMessage;
import net.b07z.sepia.websockets.common.SocketMessage.DataType;
import net.b07z.sepia.websockets.common.SocketMessage.SenderType;
import net.b07z.sepia.websockets.common.SocketMessage.TextType;
import spark.Request;

/**
 * Assistant specific version of SepiaSocketClient.
 * 
 * @author Florian Quirin
 *
 */
@WebSocket
public class AssistantSocketClient extends SepiaSocketClient {
	
	private String sepiaUserId = ""; 		//should be in sync. with getUserId()
	private String sepiaGivenName = "";
	private String regExToMatchIdOrName = "";
	private String regExToMatchTriggerPrefix = "(?i)^(hi|hello|hallo|hey|ok|alo)";
	
	/**
     * Create a SEPIA assistant client for the WebSocket server with credentials to authenticate against server.
     * Parameters are set as well here so you can give a specific client info, environment, device id etc..
     * @param credentials - JSONObject with "userId" and "pwd" (parameters like client info will not be sent)
     * @param clientParameters - things that specify the client like info, environment, location, time etc.
     */
	public AssistantSocketClient(JSONObject credentials, JSONObject clientParameters){
		super(credentials, clientParameters);
		SocketConfig.isSSL = Start.isSSL;
		SocketConfig.keystorePwd = Start.keystorePwd;
	}
	
    //Triggered when this client gets a private message.
	@Override
	public void replyToMessage(SocketMessage msg){
		//JSON.prettyPrint(msg.getJSON()); 			//DEBUG
		//System.out.println("AssistantSocketClient text: " + msg.text); 		//DEBUG
		//String deviceId = msg.getDataParameterAsString(AssistEndpoint.InputParameters.device_id.name());
		boolean isPrivate = true;
		SocketMessage reply = getReply(msg, isPrivate);
		sendMessage(reply, 3000);
    }
	
	/**
	 * See: {@link Clients#sendAssistantFollowUpMessage(NluInput, ServiceResult)}
	 */
	public boolean sendFollowUpMessage(NluInput nluInput, ServiceResult serviceResult){
		//use duplex data for channel?
		//String channelId = JSON.getString(JSON.parseString(nluInput.duplexData), "channelId");
		//or use receiver id?
		//String channelId = nluInput.user.getUserID()
		//We use the user ID to post directly into user channel
		String channelId = "<auto>"; 	//Note: we need to put something here - "<auto>" will find the receiver active channel if the sender is "omnipresent" (assistant is)
		String receiver = nluInput.user.getUserID();
		String deviceId = nluInput.deviceId;		//TODO: use it or not?
		SocketMessage msg = buildFollowUp(serviceResult.getResultJSONObject(), channelId, receiver, deviceId);
		if (Is.notNullOrEmpty(nluInput.msgId)) msg.setMessageId(nluInput.msgId); 		//add old ID as reference
		return sendMessage(msg, 3000);		//TODO: is this timeout to short?
	}
	/**
	 * See: {@link Clients#sendAssistantRemoteAction(String, String, String, String, String, String)}
	 */
	public boolean sendRemoteAction(String receiver, String actionType, String action, 
			String targetDeviceId, String targetChannelId, String skipDeviceId){
		
		SocketMessage msg = buildRemoteActionMessage(actionType, action, 
				targetDeviceId, targetChannelId, skipDeviceId, 
				receiver
		);
		return sendMessage(msg, 3000);		//TODO: is this timeout to short?
	}

    //Triggered when this client reads an arbitrary chat message
    @Override
    public void commentChat(SocketMessage msg){
    	//JSON.printJSONpretty(msg.getJSON()); 			//debug
    	
    	//we need sender and channel to answer messages
    	if (msg.sender != null && msg.channelId != null){
    		//is private channel?
    		if (msg.sender.equals(msg.channelId)){
    			replyToMessage(msg);
        		return;
    		}
    		//only Sepia and user are in this channel?
    		/*
        	JSONArray ulj = getUserList(msg.channelId);
        	System.out.println("Users: " + ulj.size()); 		//DEBUG
        	if (ulj.size() > 2){
        		//check if multiple same accounts of the user are present
        		Set<String> uniqueIds = new HashSet<>();
        		for (Object o : ulj){
        			JSONObject uj = (JSONObject) o;
        			uniqueIds.add((String) uj.get("id"));
        		}
        		if (uniqueIds.size() == 2){
        			replyToMessage(msg);
        		}
        		System.out.println("Unique users: " + uniqueIds); 		//DEBUG
        	}else{
        		replyToMessage(msg);
        		return;
        	}
        	*/
        	if (!regExToMatchIdOrName.isEmpty()){
    	    	String text = msg.text.trim().toLowerCase();
    	    	if (text.matches(regExToMatchTriggerPrefix + " " + regExToMatchIdOrName + "\\b.*") ||
    	    			text.matches(regExToMatchIdOrName + " saythis\\b.*")){
    	    	//if (text.matches("(^|hi |hello |hallo |hey |ok |alo )" + regExToMatchIdOrName + "\\b.*")){
    	    		//text = msg.text.replaceAll("\\b" + regExToMatchIdOrName + "\\b", "<assistant_name>").trim();		//TODO: enable?
    	    		msg.text = msg.text.trim().replaceFirst(regExToMatchTriggerPrefix + " " + regExToMatchIdOrName + " ", "").trim();	//Important: space at the end!
    	    		//System.out.println("TEXT: " + msg.text);
    	    		boolean isPrivate = false;
    	    		SocketMessage reply = getReply(msg, isPrivate);
    	    		sendMessage(reply, 3000);
    			}
        	}
    	}
    }
    
    //Triggered when the server sends a status message.
    @Override
    public void checkStatusMessage(SocketMessage msg){
    	//analyze status messages like someone joined the channel
    }
    
    //Triggered on channel join (after successful authentication)
    @Override
    public void joinedChannel(String activeChannel, String givenName){
    	//user joined channel (triggered usually before user list is updated)
    	sepiaUserId = getUserId();
    	sepiaGivenName = givenName;
    	regExToMatchIdOrName = "(" + (!sepiaGivenName.isEmpty()? (Pattern.quote(sepiaGivenName.toLowerCase()) + "|") : "") + Pattern.quote(sepiaUserId.toLowerCase()) + ")";
    	//System.out.println(sepiaGivenName + "(" + sepiaUserId + ") joined channel '" + getActiveChannel() + "'"); 		//DEBUG
    }
    @Override
    public void welcomeToChannel(String channelId){
    	/*
    	JSONArray userList = getUserList(channelId);
    	if (userList != null){
	    	for (Object o : userList){
	    		JSONObject user = (JSONObject) o;
	    		if (((String) user.get("id")).equals(sepiaUserId)){
	    			sepiaGivenName = (String) user.get("name");
	    		}
	    	}
    	}
    	*/   	
    }

    //--------
    
    /**
     * Convert {@link SocketMessage} to {@link Request} for {@link AssistEndpoint} call.
     */
    public FakeRequest buildAssistEndpointRequest(SocketMessage msg){
    	JSONObject data = msg.data;
    	//System.out.println(data); 		//DEBUG
    	return buildAssistEndpointRequest(data, msg.text, msg.msgId, getDuplexData(msg));
    }
    /**
     * Convert "data" and "text" of {@link SocketMessage} to {@link Request} for {@link AssistEndpoint} call.
     */
    public FakeRequest buildAssistEndpointRequest(JSONObject data, String text, String msgId, JSONObject duplexData){
    	JSONObject credentials = (JSONObject) data.get("credentials");
    	JSONObject parameters = (JSONObject) data.get("parameters");
    	
    	//add proper connection type and msg ID if not given (to identify Websocket origin and help with duplex connections)
    	if (!parameters.containsKey(AssistEndpoint.InputParameters.connection.name())){ 
    		JSON.put(parameters, AssistEndpoint.InputParameters.connection.name(), "ws");
    	}
    	if (!parameters.containsKey(AssistEndpoint.InputParameters.msg_id.name()) && Is.notNullOrEmpty(msgId)){ 
    		JSON.put(parameters, AssistEndpoint.InputParameters.msg_id.name(), msgId);
    	}
    	//add some duplex connection data
    	if (duplexData != null){
    		JSON.put(parameters, AssistEndpoint.InputParameters.duplex_data.name(), duplexData);
    	}
    	
    	//build map with parameters
    	Map<String, String> keyTextAndParameters = new HashMap<>();
    	keyTextAndParameters.put("KEY", credentials.get("userId") + ";" + credentials.get("pwd"));	//KEY
    	keyTextAndParameters.put(AssistEndpoint.InputParameters.text.name(), text);					//text
    	Converters.addJsonToMapAsStrings(parameters, keyTextAndParameters); 						//lang, time, time_local, ...
    	
    	//Convert to request
    	return new FakeRequest(keyTextAndParameters);
    }
    /**
     * Build a custom request for {@link AssistEndpoint} call.
     * @param msg - {@link SocketMessage}
     * @param overwriteParameters - parameters to overwrite, see {@link AssistEndpoint.InputParameters}
     * @return
     */
    public FakeRequest buildAssistEndpointCustomCommandRequest(SocketMessage msg, Map<String, String> overwriteParameters){
    	FakeRequest frq = buildAssistEndpointRequest(msg.data, msg.text, msg.msgId, getDuplexData(msg));
    	overwriteParameters.forEach((String k, String v) -> {
    		frq.overwriteParameter(k, v);
    	});
    	return frq;
    }
    private JSONObject getDuplexData(SocketMessage msg) {
    	return JSON.make( 
				"channelId", msg.channelId
		);
	}
    
    /**
     * Build {@link SocketMessage} reply from {@link AssistEndpoint} answer.
     */
    private SocketMessage buildReply(JSONObject answer, SocketMessage requestMsg, boolean isPrivate){
    	SocketMessage reply;
    	String receiver = (isPrivate)? requestMsg.sender : "";
    	String receiverDeviceId = (isPrivate)? requestMsg.senderDeviceId : "";
    	String receiverOnError = requestMsg.sender;
    	String channelId = requestMsg.channelId;
    	if (!JSON.getString(answer, "result").equals("fail")){
	    	String answerText = (String) answer.get("answer");
	    	if (answerText == null){
	    		reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiverOnError, receiverDeviceId, 
	    				"Error?", TextType.status.name());
	    	}else{
	    		//The 'real' message:
	    		JSONObject data = new JSONObject();
		        JSON.add(data, "dataType", DataType.assistAnswer.name());
		        JSON.add(data, "assistAnswer", answer);
		        reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiver, receiverDeviceId, data);
	    		//reply = new SocketMessage(getUserName(), receiver, answerText, "default");
	    		//reply.addData("assistAnswer", answer);
	    		//reply.addData("dataType", DataType.assistAnswer.name());
	    	}
    	
    	//no login or error
    	}else{
    		reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiverOnError, receiverDeviceId, 
    				"Login? Error?", TextType.status.name());
    	}
    	if (Is.notNullOrEmpty(receiverDeviceId)){
    		reply.setReceiverDeviceId(receiverDeviceId);
    	}
    	reply.setSenderDeviceId(Config.assistantDeviceId);
    	return reply;
    }
    /**
     * Build {@link SocketMessage} assistant follow-up message from {@link ServiceResult} answer.
     */
    private SocketMessage buildFollowUp(JSONObject answer, String channelId, String receiver, String receiverDeviceId){
    	SocketMessage reply;
    	String receiverOnError = receiver;
    	if (!JSON.getString(answer, "result").equals("fail")){
	    	String answerText = (String) answer.get("answer");
	    	if (answerText == null){
	    		reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiverOnError, receiverDeviceId, 
	    				"Error?", TextType.status.name());
	    	}else{
	    		//The 'real' message:
	    		JSONObject data = new JSONObject();
		        JSON.add(data, "dataType", DataType.assistFollowUp.name());
		        JSON.add(data, "assistAnswer", answer); 		//NOTE: we keep the name 'assistAnswer' here for client ... should have called it 'assistMsg'
		        
		        reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiver, receiverDeviceId, data);
	    	}
    	
    	//no login or error
    	}else{
    		reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiverOnError, receiverDeviceId, 
    				"Login? Error?", TextType.status.name());
    	}
    	reply.setSenderType(SenderType.assistant.name());
    	if (Is.notNullOrEmpty(receiverDeviceId)){
    		reply.setReceiverDeviceId(receiverDeviceId);
    	}
    	reply.setSenderDeviceId(Config.assistantDeviceId);
    	return reply;
    }
    /**
     * Build {@link SocketMessage} assistant remote-action message.
     */
    private SocketMessage buildRemoteActionMessage(String actionType, String action, 
			String targetDeviceId, String targetChannelId, String skipDeviceId, 
			String receiver){
		        
        JSONObject data = JSON.make(
				"dataType", DataType.remoteAction.name(), 
				"remoteUserId", receiver,
				"targetDeviceId", targetDeviceId,
				"targetChannelId", targetChannelId,
				"skipDeviceId", skipDeviceId
		);
		JSON.put(data, "type", actionType);
		JSON.put(data, "action", action);
		
		String channelId = "<auto>";	//we need to put something here but it doesn't really matter since remote-action has its own field
		SocketMessage reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiver, "", data);
    	reply.setSenderType(SenderType.assistant.name());
    	reply.setSenderDeviceId(Config.assistantDeviceId);
    	return reply;
    }
    
    /**
     * Get a reply and build a {@link SocketMessage} for the receiver.
     * @param msg - message received
     * @param isPrivate - is message for single user or channel?
     */
    public SocketMessage getReply(SocketMessage msg, boolean isPrivate){
    	SocketMessage reply;
    	String msgId = msg.msgId;
    	String channelId = msg.channelId;
    	String receiverOnError = msg.sender;
    	String receiverDeviceId = msg.senderDeviceId;
    	if (msg.data != null && msg.data.get("credentials") != null){
    		try{
		    	Request request = buildAssistEndpointRequest(msg);
		    	//System.out.println("AssistantSocketClient - request: " + msg.text); 					//debug
		    	JSONObject answer = JSON.parseString(AssistEndpoint.answerAPI(request, new FakeResponse()));
		    	//System.out.println("AssistantSocketClient - answer: " + answer.toJSONString()); 		//debug
		    	reply = buildReply(answer, msg, isPrivate);

		    //internal error
    		}catch (Exception e){
    			Debugger.println(e.getMessage(), 1);		//DEBUG
    			Debugger.printStackTrace(e, 4);				//DEBUG
    			try{
    				//try again to get at least an error message response while keeping some parameters
    				JSONObject parameters = JSON.getJObject(msg.data, "parameters");
    				JSONObject newParams = JSON.make(
    						InputParameters.input_type.name(), "direct_cmd",
    						InputParameters.lang.name(), JSON.getString(parameters, InputParameters.lang.name()),
    						InputParameters.last_cmd.name(), JSON.getString(parameters, InputParameters.last_cmd.name()),
    						InputParameters.last_cmd_N.name(), JSON.getString(parameters, InputParameters.last_cmd_N.name()),
    						InputParameters.mood.name(), JSON.getString(parameters, InputParameters.mood.name())
    				);
    				JSON.put(newParams, InputParameters.client.name(), JSON.getString(parameters, InputParameters.client.name())); 	//REQUIRED! Need more?
    				JSON.put(msg.data, "parameters", newParams);
    				String text = "chat;;reply=<error_0a>";
    				Request request = buildAssistEndpointRequest(msg.data, text, msgId, getDuplexData(msg));
    				JSONObject answer = JSON.parseString(AssistEndpoint.answerAPI(request, new FakeResponse()));
    				reply = buildReply(answer, msg, isPrivate);
    				
    			}catch (Exception e2){
    				//if that fails as well just make an error status message
    				Debugger.println(e.getMessage(), 1);		//DEBUG
        			Debugger.printStackTrace(e, 4);				//DEBUG
    				reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiverOnError, receiverDeviceId, 
    						"Internal Error!", TextType.status.name());
        			reply.addData("dataType", DataType.errorMessage.name());
    			}
    		}
    	//no login?
    	}else{
    		reply = new SocketMessage(channelId, getUserId(), getDeviceId(), receiverOnError, receiverDeviceId, 
    				"Login?", TextType.status.name());
    	}
    	reply.setSenderType(SenderType.assistant.name());
    	if (msgId != null) reply.setMessageId(msgId);
    	
    	return reply;
    }
}
