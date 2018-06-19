package net.b07z.sepia.server.assist.messages;

import java.util.regex.Pattern;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.endpoints.AssistEndpoint;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.server.FakeRequest;
import net.b07z.sepia.server.core.server.FakeResponse;
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
public class AssistantSocketClient extends SepiaSocketClient{
	
	private String sepiaUserId = ""; 		//should be in sync. with getUserId()
	private String sepiaGivenName = "";
	private String regExToMatchIdOrName = "";
	
	public AssistantSocketClient(){
		super();
		SocketConfig.isSSL = Start.isSSL;
		SocketConfig.keystorePwd = Start.keystorePwd;
	}
	public AssistantSocketClient(JSONObject credentials){
		super(credentials);
		SocketConfig.isSSL = Start.isSSL;
		SocketConfig.keystorePwd = Start.keystorePwd;
	}
	
    //Triggered when this client gets a private message.
	@Override
	public void replyToMessage(SocketMessage msg){
    	//JSON.printJSONpretty(msg.getJSON()); 			//debug
    	SocketMessage reply = getReply(msg, msg.sender, msg.sender);
		sendMessage(reply, 3000);
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
    	    	if (text.matches("(hi |hello |hallo |hey |ok |alo )" + regExToMatchIdOrName + "\\b.*") ||
    	    			text.matches(regExToMatchIdOrName + " saythis\\b.*")){
    	    	//if (text.matches("(^|hi |hello |hallo |hey |ok |alo )" + regExToMatchIdOrName + "\\b.*")){
    	    		//text = msg.text.replaceAll("\\b" + regExToMatchIdOrName + "\\b", "<assistant_name>").trim();		//TODO: enable?
    	    		//System.out.println("TEXT: " + text);
    	    		SocketMessage reply = getReply(msg, "", msg.sender);
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
     * Get a reply and send it to receiver.
     * @param msg - message received
     * @param receiver - receiver when everything works
     * @param receiverOnError - receiver when there is an error (can be the same but maybe better only sender)
     */
    public SocketMessage getReply(SocketMessage msg, String receiver, String receiverOnError){
    	SocketMessage reply;
    	String msgId = msg.msgId;
    	String channelId = msg.channelId;
    	if (msg.data != null && msg.data.get("credentials") != null){
	    	JSONObject credentials = (JSONObject) msg.data.get("credentials");
	    	JSONObject parameters = (JSONObject) msg.data.get("parameters");
	    	
	    	//Convert to request
	    	String[] params = new String[parameters.size() + 2];
	    	params[0] = "KEY=" + credentials.get("userId") + ";" + credentials.get("pwd");
	    	params[1] = "text=" + msg.text;
	    	/*
    		"lang=" + parameters.get("lang"),
    		"time=" + parameters.get("time"),
    		"time_local=" + parameters.get("time_local"),
    		"user_location=" + parameters.get("user_location"
    		...
    		 */
	    	int i=2;
	    	for (Object o : parameters.keySet()){
	    		params[i] = o.toString() + "=" + parameters.get(o);
	    		//System.out.println(params[i]); 		//DEBUG
	    		i++;
	    	}
	    	//TODO: why not use JSON here instead of converting everything and then convert it back ^^
	    	Request request = new FakeRequest(params);
	    	JSONObject answer = JSON.parseString(AssistEndpoint.answerAPI(request, new FakeResponse()));
	    	//System.out.println(answer.toJSONString()); 		//debug
	    	if (!JSON.getString(answer, "result").equals("fail")){
		    	String answerText = (String) answer.get("answer");
		    	if (answerText == null){
		    		reply = new SocketMessage(channelId, getUserId(), receiverOnError, "Error?", TextType.status.name());
		    	}else{
		    		//The 'real' message:
		    		JSONObject data = new JSONObject();
			        JSON.add(data, "dataType", DataType.assistAnswer.name());
			        JSON.add(data, "assistAnswer", answer);
			        reply = new SocketMessage(channelId, getUserId(), receiver, data);
		    		//reply = new SocketMessage(getUserName(), receiver, answerText, "default");
		    		//reply.addData("assistAnswer", answer);
		    		//reply.addData("dataType", DataType.assistAnswer.name());
		    	}
	    	
	    	//no login or error
	    	}else{
	    		reply = new SocketMessage(channelId, getUserId(), receiverOnError, "Login? Error?", TextType.status.name());
	    	}
    	}else{
    		reply = new SocketMessage(channelId, getUserId(), receiverOnError, "Login?", TextType.status.name());
    	}
    	reply.setSenderType(SenderType.assistant.name());
    	if (msgId != null) reply.setMessageId(msgId);
    	
    	return reply;
    }
}
