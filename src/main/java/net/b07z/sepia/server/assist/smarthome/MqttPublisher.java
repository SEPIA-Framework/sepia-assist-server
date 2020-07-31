package net.b07z.sepia.server.assist.smarthome;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.workers.DuplexConnectionInterface;
import net.b07z.sepia.server.assist.workers.MqttConnection;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

public class MqttPublisher implements SmartHomeHub {
	
	public static final String NAME = "mqtt_pub";
	public static final String ROOT_TOPIC = "sepia/smart-devices/";
	
	private static Map<String, DuplexConnectionInterface> knownConnections = new ConcurrentHashMap<>(); //<host, DC>
	private static Map<String, JSONObject> topicStateTracker = new ConcurrentHashMap<>(); 				//<topic, state JSON>
	
	private String hubId;
	private String host;
	private String authType;
	private String authData;
	private JSONObject info;
		
	/**
	 * Create new MQTT instance.
	 * @param host - e.g.: 'tcp://localhost:1883' or 'ws://broker.hivemq.com:8000'
	 */
	public MqttPublisher(String host){
		if (Is.nullOrEmpty(host)){
			throw new RuntimeException("No host address found for MQTT integration!");
		}else{
			this.host = host.replaceFirst("/$", "").trim();
		}
	}
	
	//Methods to handle data exchanged via MQTT topics
	private void collectData(String topic, JSONObject data){
		//store globally
		topicStateTracker.put(topic, data);
	}
	private void assignData(SmartHomeDevice device, JSONObject data){
		String state = Converters.obj2StringOrDefault(data.get("state"), null);
		String stateType = device.getStateType();
		if (state != null){
			if (stateType != null){
				//generalize state according to stateType
				state = SmartHomeDevice.convertAnyStateToGeneralizedState(state, stateType);
			}
			//assign state
			device.setState(state);
		}
	}
	private void updateDataBeforeSetState(String topic, String state, String stateType){
		//NOTE: since the (own) published data will be analyzed as well this is just a fallback for "lost" messages
		JSONObject data = topicStateTracker.get(topic);
		if (data != null){
			JSON.put(data, "state", state);
		}
	}
	private JSONObject getPayloadForSetState(SmartHomeDevice device, String state, String stateType){
		//publish this via MQTT
		return JSON.make(
			"state", state
		);
	}
	
	//-------INTERFACE IMPLEMENTATIONS-------
	
	@Override
	public JSONObject toJson(){
		return JSON.make(
			"id", this.hubId,
			"type", NAME,
			"host", this.host,
			"authType", this.authType,
			"authData", this.authData,
			"info", this.info
		);
	}
	
	@Override
	public boolean activate(){
		//handle permanent MQTT connection
		try{
			DuplexConnectionInterface duplexConn = knownConnections.get(this.host);
			if (duplexConn == null || duplexConn.getStatus() != 0){
				//create (new)
				if (duplexConn != null){
					//make sure old one is closed ... then forget it
					duplexConn.disconnect();
				}	
				boolean autoReconnect = true;
				duplexConn = MqttConnection.createAndRegisterMqttConnection(
						this.host, this.authType, this.authData, autoReconnect
				);
				knownConnections.put(this.host, duplexConn);
			
			}else{
				//already active
				return true;
			}
			//connect soon, don't wait
			boolean isAtServerStart = (System.currentTimeMillis() - Start.lastStartUNIX) < 10000; 
			long delay = isAtServerStart? -1l : 500;		//is at server start or on-the-fly?
			duplexConn.connect(delay, () -> {
				//register general topic
				DuplexConnectionInterface dCon = knownConnections.get(this.host);
				if (dCon != null && dCon.getStatus() == 0){
					String wildcardTopic = ROOT_TOPIC + "#";
					dCon.addMessageHandler(wildcardTopic, msg -> {	
						//NOTE: this will overwrite any existing handlers, but thats OK because we track this state globally
						String topic = JSON.getString(msg, "topic");
						JSONObject data = JSON.getJObject(msg, "payload");
						if (Is.notNullOrEmpty(topic) && data != null){
							collectData(topic, data);
						}
						//System.out.println("dCon1 saw: " + msg);			//DEBUG
					});
					Debugger.println("MqttPublisher - ID: " + this.hubId + " is now ACTIVE (topic: " + wildcardTopic + ")", 3);
				}
			});
			return true; 	//we did what we can
			
		}catch (Exception e){
			Debugger.println("MqttPublisher - ID: " + this.hubId + " - Activation FAILED, msg.: " + e.getMessage(), 1);
			return false;
		}
	}
	@Override
	public boolean deactivate(){
		try{
			DuplexConnectionInterface duplexConn = knownConnections.get(this.host);
			if (duplexConn != null && duplexConn.getStatus() != 2){
				duplexConn.disconnect();
				duplexConn.waitForState(2, -1);
				Debugger.println("MqttPublisher - ID: " + this.hubId + " has been DEACTIVATED", 3);
			}
			return true;
			
		}catch(Exception e){
			Debugger.println("MqttPublisher - ID: " + this.hubId + " - Deactivation FAILED, msg.: " + e.getMessage(), 1);
			return false;
		}
	}
	
	@Override
	public void setHostAddress(String hostUrl){
		this.host = hostUrl.replaceFirst("/$", "").trim();
	}

	@Override
	public void setAuthenticationInfo(String authType, String authData){
		this.authType = authType;
		this.authData = authData;
	}

	@Override
	public void setId(String id){
		this.hubId = id;
	}

	@Override
	public String getId(){
		return this.hubId;
	}

	@Override
	public void setInfo(JSONObject info){
		this.info = info;
	}

	@Override
	public JSONObject getInfo(){
		return this.info;
	}

	@Override
	public boolean requiresRegistration(){
		return false;
	}
	@Override
	public boolean registerSepiaFramework(){
		return true;
	}
	
	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		//add last known state
		String id = device.getId();		//e.g. light/1
		JSONObject data = topicStateTracker.get(ROOT_TOPIC + id);	//e.g. sepia/smart-devices/light/1
		if (data != null){
			assignData(device, data);
		}
		return device;
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		//handle state
		//set command overwrite?
		JSONObject setCmds = device.getCustomCommands();
		//System.out.println("setCmds: " + setCmds);		//DEBUG
		if (Is.notNullOrEmpty(setCmds)){
			String newState = SmartHomeDevice.getStateFromCustomSetCommands(state, stateType, setCmds);
			if (newState != null){
				state = newState;
			}
			//System.out.println("state: " + state);		//DEBUG
			
		//check deviceType to find correct set command
		}else{
			//TODO: change/adapt?
			//compare: 'loadDeviceData' -> 'SmartHomeDevice.convertAnyStateToGeneralizedState'
		}
		//write state and publish
		String id = device.getId();			//e.g. light/1
		String topic = ROOT_TOPIC + id;		//e.g. sepia/smart-devices/light/1
		updateDataBeforeSetState(topic, state, stateType);
		//publish
		DuplexConnectionInterface duplexConn = knownConnections.get(this.host);
		if (duplexConn == null){
			Debugger.println("MqttPublisher - setDeviceState FAILED with msg.: MQTT DuplexConnection not existing (NULL)", 1);
			return false;
		}else if (duplexConn.getStatus() != 0){
			Debugger.println("MqttPublisher - setDeviceState FAILED with msg.: MQTT DuplexConnection was NOT connected. Found status: " 
					+ duplexConn.getStatusDescription(), 1);
			return false;
		}else{
			if (duplexConn.sendMessage(getPayloadForSetState(device, state, stateType), topic, null)){
				return true;
			}else{
				Debugger.println("MqttPublisher - setDeviceState FAILED with msg.: " + duplexConn.getStatusDescription(), 1);
				return false;
			}
		}
	}

	//---- below you will find parts of the interface that have not been implemented for this connector ----

	@Override
	public Map<String, SmartHomeDevice> getDevices(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, Set<String>> getBufferedDeviceNamesByType(){
		// TODO Auto-generated method stub
		return null;
	}

}
