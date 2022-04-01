package net.b07z.sepia.server.assist.workers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.smarthome.SmartHomeHub.AuthType;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.ThreadManager;
import net.b07z.sepia.websockets.mqtt.SepiaMqttClient;
import net.b07z.sepia.websockets.mqtt.SepiaMqttClientOptions;
import net.b07z.sepia.websockets.mqtt.SepiaMqttMessage;

/**
 * Worker that keeps connection to a MQTT broker.
 * 
 * @author Florian Quirin
 *
 */
public class MqttConnection implements DuplexConnectionInterface {
	
	//Settings - worker will only run if token and domain are set 
	public static final String NAME = "MQTT-connect"; 
	private static AtomicInteger connLastId = new AtomicInteger(0);
	
	//common
	private String name;
	private int connectionStatus = 2;			//-1: error, 0: connected, 1: connecting, 2: offline, 3: disconnecting
	private String statusDesc = "";				//text description of status
	private String statusInfo = "";				//any additional info for current status (e.g. error data)
	private boolean abort = false;
	
	private long defaultStartDelay = 5400;		//start worker after this delay
	private long waitInterval = 100;			//wait interval during kill or status change request
	private long maxStatusChangeWait = 5000;	//maximum wait for status change
	
	private long lastActivity = 0l;				//when has the worker last done anything
	//private long minTimeToLog = (60*60*1000);	//minimum time to wait until next log entry (handy when refreshes are made frequently, errors are always logged)
	//private long lastLog = 0;
		
	//MQTT broker
	private String mqttBroker = "tcp://localhost:1883";
	private String mqttUserName = "";
	private String mqttPassword = "";
	private SepiaMqttClient mqttClient;
	private boolean automaticReconnect = false;
	
	//variables
	public JSONObject workerData;
	
	/**
	 * Create new connection to MQTT broker.
	 * @param brokerAdr - URL like 'tcp://localhost:1883' or 'ws://broker.hivemq.com:8000'
	 * @param userName - custom name or null (null: use MQTT client and server ID)
	 * @param password - password or null
	 * @param autoReconnect - automatic reconnect after connection loss? 
	 */
	public MqttConnection(String brokerAdr, String userName, String password, boolean autoReconnect){
		this.name = NAME + "-" + connLastId.incrementAndGet();
		this.mqttBroker = brokerAdr;
		this.mqttUserName = (userName == null)? ("SEPIA-" + name + "-" + Config.localName) : userName;
		this.mqttPassword = password;
		this.automaticReconnect = autoReconnect;
	}
	
	@Override
	public void setup() throws Exception{
		connectionStatus = 2;
				
		//create
		SepiaMqttClientOptions mqttOptions = new SepiaMqttClientOptions()
			.setAutomaticReconnect(automaticReconnect)
			.setCleanSession(true)
			.setConnectionTimeout(6);
		if (Is.notNullOrEmpty(this.mqttUserName)){
			mqttOptions.setUserName(this.mqttUserName);
		}
		if (Is.notNullOrEmpty(this.mqttPassword)){
			mqttOptions.setPassword(this.mqttPassword);
		}
		mqttClient = new SepiaMqttClient(mqttBroker, mqttOptions);
	}
	
	@Override
	public long getLastActivity(){
		return lastActivity;
	}
	
	@Override
	public String getName(){
		return name;
	}
	
	@Override
	public int getStatus(){
		return connectionStatus;
	}
	
	@Override
	public String getStatusDescription(){
		if (connectionStatus == -1){
			statusDesc = "error";
		}else if (connectionStatus == 0){
			statusDesc = "connected";
		}else if (connectionStatus == 1){
			statusDesc = "connecting";
		}else if (connectionStatus == 2){
			statusDesc = "offline";
		}else if (connectionStatus == 3){
			statusDesc = "disconnecting";
		}else{
			statusDesc = "unknown";
		}
		if (Is.notNullOrEmpty(statusInfo)){
			return statusDesc + " - " + statusInfo;
		}else{
			return statusDesc;
		}
	}
	
	@Override
	public boolean waitForState(int state, long maxWait){
		if (maxWait == -1) maxWait = maxStatusChangeWait;
		if (state == -1){
			Debugger.println("DuplexConnection, Name: " + getName() + " - 'waitForState' FAILED due to invalid value '-1'.", 1);
			return false;
		}
		long thisWait = 0; 
		while (connectionStatus != state && connectionStatus != -1){
			Debugger.sleep(waitInterval);
			thisWait += waitInterval;
			if (thisWait >= maxWait){
				Debugger.println("DuplexConnection, Name: " + getName() + " - 'waitForState' FAILED due to timeout.", 1);
				return false;
			}
		}
		if (connectionStatus == -1){
			return false;
		}else{
			return true;
		}
	}
	
	@Override
	public void connect(){
		abort = false;
		lastActivity = System.currentTimeMillis();
		connect(0, null);
	}
	@Override
	public void connect(long customStartDelay, Runnable onConnected){
		//start
		final long startDelay = (customStartDelay == -1)? defaultStartDelay : customStartDelay;
		abort = false;
		ThreadManager.run(() -> {
	    	connectionStatus = 1;
	    	statusInfo = "";
	    	Debugger.sleep(startDelay);
	    	lastActivity = System.currentTimeMillis();
	    	long tic = Debugger.tic();
	    	if (!abort){
	    		Debugger.println("DuplexConnection, Name: " + getName() + " - START", 3);
	    	}else{
	    		Debugger.println("DuplexConnection, Name: " + getName() + " - CANCELED before start", 3);
	    	}
	    	abort = false;
	    	try{
				mqttClient.connect();
				if (mqttClient.isConnected()){
					connectionStatus = 0;
					statusInfo = "";
					Statistics.addOtherApiHit("DuplexConn.: " + name + " connect");
					Statistics.addOtherApiTime("DuplexConn.: " + name + " connect", tic);
					if (onConnected != null){
						onConnected.run();
					}
				}else{
					connectionStatus = -1;
					statusInfo = "connect error; client was not connected after 'connect' request but did not trigger error";
					Statistics.addOtherApiHit("DuplexConn. ERRORS: " + name + " connect");
					Statistics.addOtherApiTime("DuplexConn. ERRORS: " + name + " connect", tic);
				}
			}catch (Exception e){
				connectionStatus = -1;
				statusInfo = "connect error; " + e.getMessage();
				Debugger.println("DuplexConnection: " + name + " - 'connect' error: " + e.getMessage(), 1);
				//Debugger.printStackTrace(e, 3);
				Statistics.addOtherApiHit("DuplexConn. ERRORS: " + name + " connect");
				Statistics.addOtherApiTime("DuplexConn. ERRORS: " + name + " connect", tic);
			}
		});
	}
	
	@Override
	public boolean sendMessage(JSONObject msg, String path, String receiver){
		//publish
		long tic = Debugger.tic();
		lastActivity = System.currentTimeMillis();
		try{
			mqttClient.publish(path, new SepiaMqttMessage(msg.toJSONString())
				.setQos(0)
				.setRetained(false)
			);
			Statistics.addOtherApiHit("DuplexConn.: " + name + " send");
			Statistics.addOtherApiTime("DuplexConn.: " + name + " send", tic);
			return true;
			
		}catch (Exception e){
			connectionStatus = -1;
			statusInfo = "sendMessage error; " + e.getMessage();
			Debugger.println("DuplexConnection: " + name + " - 'sendMessage' error: " + e.getMessage(), 1);
			//Debugger.printStackTrace(e, 3);
			Statistics.addOtherApiHit("DuplexConn. ERRORS: " + name + " send");
			Statistics.addOtherApiTime("DuplexConn. ERRORS: " + name + " send", tic);
			return false;
		}
	}
	
	@Override
	public boolean addMessageHandler(String pathFilter, Consumer<JSONObject> handlerFun){
		try{
			mqttClient.subscribe(pathFilter, msg -> {
				lastActivity = System.currentTimeMillis();
				handlerFun.accept(msg);
			});
			return true;
		}catch (Exception e){
			Debugger.println("DuplexConnection: " + name + " - 'addMessageHandler' FAILED due to: " + e.getMessage(), 1);
			//Debugger.printStackTrace(e, 3);
			return false;
		}
	}
	
	@Override
	public boolean removeMessageHandler(String pathFilter){
		try{
			mqttClient.unsubscribe(pathFilter);
			return true;
		}catch (MqttException e){
			Debugger.println("DuplexConnection: " + name + " - 'removeMessageHandler' FAILED due to: " + e.getMessage(), 1);
			//Debugger.printStackTrace(e, 3);
			return false;
		}
	}
	
	@Override
	public void disconnect(){
		long tic = Debugger.tic();
		lastActivity = System.currentTimeMillis();
		try{
			if (mqttClient.isConnected()){
				mqttClient.disconnect();
			}
			mqttClient.close();
			connectionStatus = 2;
			statusInfo = "";
			Statistics.addOtherApiHit("DuplexConn.: " + name + " disconnect");
			Statistics.addOtherApiTime("DuplexConn.: " + name + " disconnect", tic);
			
		}catch (Exception e){
			connectionStatus = -1;
			statusInfo = "disconnect error; " + e.getMessage();
			Debugger.println("DuplexConnection: " + name + " - 'disconnect' error: " + e.getMessage(), 1);
			//Debugger.printStackTrace(e, 3);
			Statistics.addOtherApiHit("DuplexConn. ERRORS: " + name + " disconnect");
			Statistics.addOtherApiTime("DuplexConn. ERRORS: " + name + " disconnect", tic);
		}
	}
	
	//---------- some static helpers ----------
	
	/**
	 * Create a new {@link DuplexConnectionInterface} for this MQTT class and register it at workers manager. 
	 * Data can for example be taken from smart home Hub.
	 * @param host - URL like 'tcp://localhost:1883' or 'ws://broker.hivemq.com:8000'
	 * @param authType - currently supported: 'plain' (username:password)
	 * @param authData - username:password (for plain type)
	 * @param autoReconnect - automatically reconnect?
	 * @return instance or null (on error)
	 * @throws Exception 
	 */
	public static DuplexConnectionInterface createAndRegisterMqttConnection(String host, 
						String authType, String authData, boolean autoReconnect) throws Exception{
		if (Is.notNullOrEmpty(host)){
			String userName = null;
			String password = null;
			if (Is.notNullOrEmpty(authType) && Is.notNullOrEmpty(authData)){
				//currently we only support one auth type: 'Plain' - format: username:password
				try {
					if (authType.equalsIgnoreCase(AuthType.Plain.name()) || authType.equalsIgnoreCase(AuthType.Custom.name())){
						String[] up = authData.split(":", 2);
						userName = up[0];
						password = up[1];
					}else{
						throw new RuntimeException("Invalid auth. type. Try 'Custom' or 'Plain'.");
					}
				}catch (Exception e) {
					Debugger.println("'createMqttConnection' FAILED to add auth. data - try type 'Custom' or 'Plain' and data 'username:password'", 1);
					Debugger.printStackTrace(e, 3);
					userName = null;
					password = null;
				}
			}
			DuplexConnectionInterface dCon = new MqttConnection(
					host, 
					userName, password, 
					autoReconnect
			);
			if (dCon != null){
				dCon.setup();
				Workers.registerConnection(dCon);
				Debugger.println("Registered MQTT duplex connection: " + dCon.getName(), 3);
			}
			return dCon;
		}else{
			Debugger.println("'createAndRegisterMqttConnection' FAILED due to invalid 'host'", 1);
			return null;
		}
	}
}
