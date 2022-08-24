package net.b07z.sepia.server.assist.smarthome;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.parameters.SmartDevice.Types;
import net.b07z.sepia.server.assist.parameters.SmartDeviceValue;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice.StateType;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Home-Assistant integration for smart home HUB interface.
 * 
 * @author Florian Quirin
 *
 */
public class HomeAssistant implements SmartHomeHub {
	
	public static final String NAME = "home_assistant";
	
	public static int CONNECT_TIMEOUT = 4000;
	
	private String hubId;
	private String host;
	private String authType;
	private String authData;
	private JSONObject info;
	
	private static Map<String, Map<String, Set<String>>> bufferedDevicesOfHostByType = new ConcurrentHashMap<>();
	private Map<String, Set<String>> bufferedDevicesByType;
	
	public HomeAssistant(String host){
		if (Is.nullOrEmpty(host)){
			throw new RuntimeException("No host address found for HomeAssistant integration!");
		}else{
			this.host = host.replaceFirst("/$", "").trim();
			this.bufferedDevicesByType = bufferedDevicesOfHostByType.get(this.host);
		}
	}
	
	//HTTP call methods for HUB
	private Map<String, String> addAuthHeader(Map<String, String> headers){
		return Connectors.addAuthHeader(headers, this.authType, this.authData);
	}
	private JSONObject httpGET(String url){
		if (Is.notNullOrEmpty(this.authData)){
			return Connectors.httpGET(url, null, addAuthHeader(null), CONNECT_TIMEOUT);
		}else{
			return Connectors.httpGET(url, null, null, CONNECT_TIMEOUT);
		}
	}
	private JSONObject httpPOST(String url, String state, Map<String, String> headers){
		if (Is.notNullOrEmpty(this.authData)){
			headers = addAuthHeader(headers);
		}
		return Connectors.httpPOST(url, state, headers, CONNECT_TIMEOUT);
	}

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
		return true;
	}
	@Override
	public boolean deactivate(){
		return true;
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
	public void setHostAddress(String hostUrl){
		this.host = hostUrl.replaceFirst("/$", "").trim();
	}
	
	@Override
	public void setAuthenticationInfo(String authType, String authData){
		this.authType = authType;
		this.authData = authData;
		if (Is.notNullOrEmpty(this.authType) && Is.notNullOrEmpty(this.authData)){
			if (authType.equalsIgnoreCase(AuthType.Bearer.name())){
				//OK
				this.authType = AuthType.Bearer.name();
				
			}else{
				throw new RuntimeException("Invalid auth. type. Try 'Bearer'.");
			}
		}
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
		//Currently no action required - just return true
		return true;
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices() {
		JSONObject response = httpGET(this.host + "/api/states");
		System.out.println("HomeAssistant REST response: " + response); 					//DEBUG
		if (Connectors.httpSuccess(response)){
			//use the chance to update the "names by type" buffer
			this.bufferedDevicesByType = new ConcurrentHashMap<>();
			
			JSONArray devicesArray = null;
			if (response.containsKey("JSONARRAY")){
				devicesArray = JSON.getJArray(response, "JSONARRAY");		//this should usually be the one triggered
			}else if (response.containsKey("STRING")){
				String arrayAsString = JSON.getString(response, "STRING");
				if (arrayAsString.trim().startsWith("[")){
					devicesArray = JSON.parseStringToArrayOrFail(arrayAsString);
				}
			}
			if (devicesArray == null){
				//ERROR
				return null;
			}
			if (devicesArray.isEmpty()){
				//Fail with empty array
				Debugger.println("Service:HomeAssistant - devices array was empty!", 1);
				return new HashMap<String, SmartHomeDevice>();
			}
			//Build devices map
			Map<String, SmartHomeDevice> devices = new HashMap<>();
			try{
				for (Object o : devicesArray){
					JSONObject hubDevice = (JSONObject) o;
					//System.out.println("HomeAssistant device JSON: " + hubDevice); 			//DEBUG
					
					//Build unified object for SEPIA
					SmartHomeDevice shd = buildDeviceFromResponse(hubDevice);
					
					//devices
					if (shd != null){
						devices.put(shd.getMetaValueAsString("id"), shd);
						
						//fill buffer
						String deviceType = shd.getType();
						String deviceName = shd.getName();
						if (Is.notNullOrEmpty(deviceType) && Is.notNullOrEmpty(deviceName)){
							deviceName = SmartHomeDevice.getCleanedUpName(deviceName);		//NOTE: use "clean" name!
							if (!deviceName.isEmpty() && (boolean) shd.getMeta().get("namedBySepia")){
								Set<String> deviceNamesOfType = this.bufferedDevicesByType.get(deviceType);
								if (deviceNamesOfType == null){
									deviceNamesOfType = new HashSet<>();
									this.bufferedDevicesByType.put(deviceType, deviceNamesOfType);
								}
								deviceNamesOfType.add(deviceName);
							}
						}
					}
				}
				
				//store new buffer
				bufferedDevicesOfHostByType.put(this.host, this.bufferedDevicesByType);
				
				return devices;
				
			}catch (Exception e){
				//Fail with faulty array
				Debugger.println("Service:HomeAssistant - devices array seems to be broken! Msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				return new HashMap<String, SmartHomeDevice>();
			}
			
		}else{
			//Fail with server contact error
			Debugger.println("Service:HomeAssistant - failed to get devices from server!", 1);
			return null;
		}
	}

	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters) {
		//TODO: make this more effective by filtering before instead of loading all devices first
		Map<String, SmartHomeDevice> devices = getDevices();
		if (devices == null){
			return null;
		}else{
			return SmartHomeDevice.getMatchingDevices(devices, filters);
		}
	}

	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, Set<String>> getBufferedDeviceNamesByType() {
		// TODO Auto-generated method stub
		return null;
	}

	//-----------------
	
	/**
	 * Build unified object for SEPIA from HUB device data.
	 * @param hubDevice - data gotten from e.g. call to devices endpoint of HUB
	 * @return
	 */
	private static SmartHomeDevice buildDeviceFromResponse(JSONObject hubDevice){
		/*
		 Expected format:
	 	{
			"entity_id": "light.hue_white_lamp_1",
			"state": "on",
			"attributes": {
				"supported_color_modes": [
					"brightness"
				],
				"color_mode": "brightness",
				"brightness": 179,
				"mode": "normal",
				"dynamics": "none",
				"friendly_name": "Hue white lamp 1",
				"supported_features": 40
			},
			"last_changed": "2022-08-21T18:53:53.194335+00:00",
			"last_updated": "2022-08-21T18:53:57.482802+00:00",
			"context": {
				"id": "01GB0W29VA4XNCC983B8ZR8K7M",
				"parent_id": null,
				"user_id": null
			}
		}
		 */
		//Build unified object for SEPIA
		String name = null;
		String type = null;
		String room = null;
		String roomIndex = null;
		String memoryState = "";
		String state = null;
		String stateType = null;
		JSONObject setCmds = null;
		boolean typeGuessed = false;
		boolean namedBySepia = false;
		
		String entityId = JSON.getString(hubDevice, "entity_id");	//NOTE: has to be unique
		if (Is.nullOrEmpty(entityId)){
			//we need the ID
			return null;
		}
		JSONObject attributes = JSON.getJObject(hubDevice, "attributes");
		if (attributes != null){
			//try to find self-defined SEPIA tags first
			name = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_NAME, null);
			if (Is.notNullOrEmpty(name)){
				namedBySepia = true;
			}else{
				name = JSON.getStringOrDefault(attributes, "friendly_name", null);
			}
			type = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_TYPE, null);
			room = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_ROOM, null);
			roomIndex = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_ROOM_INDEX, null);
			memoryState = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_MEM_STATE, null);
			stateType = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_STATE_TYPE, null);
			String setCmdsStr = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_SET_CMDS, null);
			if (setCmdsStr != null && setCmdsStr.trim().startsWith("{")){
				setCmds = JSON.parseString(setCmdsStr);
			}
		}else{
			attributes = new JSONObject();	//this is probably never the case, but just for convenience ...
		}
		//we only accept devices with name
		if (name == null){
			name = entityId;
		}
		
		//extract room
		if (room == null){
			//can we extract this from somewhere?
			room = "";
		}
		
		//extract type
		Types dt;
		if (type == null){
			dt = extractTypeFromEntity(entityId, attributes);
			if (dt != null){
				type = dt.name();
				typeGuessed = true;
			}else{
				//TODO: device with unknown type - break or use unknown?
				dt = Types.unknown;
			}
		}else{
			dt = Types.valueOf(type);
		}
		//extract stateType
		StateType st;
		if (stateType == null){
			st = extractStateTypeFromAttributes(dt, attributes);
			if (st != null){
				stateType = st.name();
			}else{
				//TODO: device with unknown stateType - what now?
				st = StateType.text_raw;
			}
		}else{
			st = StateType.valueOf(stateType);
		}
		//get state
		String stateBasic = JSON.getStringOrDefault(hubDevice, "state", null);
		if (stateBasic == null || stateBasic.equalsIgnoreCase("unknown")){
			state = null;
		}else{
			switch (dt){
				case light:
					state = getStateForLights(stateBasic, st, attributes);
					break;
				case sensor:
					//TODO: improve
					state = stateBasic;
					break;
				case unknown:
					state = stateBasic;
					
				//TODO: implement more
				default:
					break;
			}
		}
		if (state != null){
			if (stateType != null){
				state = SmartHomeDevice.convertAnyStateToGeneralizedState(state, stateType);		
				//TODO: this might require deviceType (see comment inside method)
			}
		}
		
		Object linkObj = hubDevice.get("link");
		JSONObject meta = JSON.make(
			"id", entityId,
			"origin", NAME,
			"typeGuessed", typeGuessed,
			"namedBySepia", namedBySepia
		);
		//TODO: we could add some stuff to meta when we need other data from response.
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, state, stateType, memoryState, 
			(linkObj != null)? linkObj.toString() : null, meta);
		//specify more
		if (Is.notNullOrEmpty(roomIndex)){
			shd.setRoomIndex(roomIndex);
		}
		if (Is.notNullOrEmpty(setCmds)){
			shd.setCustomCommands(setCmds);
		}
		return shd;
	}
	
	private static Types extractTypeFromEntity(String entityId, JSONObject attributes){
		//according to HA docs we can rely on this format: platformType.domain
		//types doc: https://developers.home-assistant.io/docs/core/entity/
		//types list: https://github.com/home-assistant/core/blob/dev/homeassistant/const.py
		String platformType = entityId.split("\\.")[0];
		
		switch (platformType){
			case "light":
				return Types.light;
			case "cover":
				//TODO: in HA this can be for example a garage door as well - check: attributes
				return Types.roller_shutter;
			case "climate":
				//TODO: in HA this can be for example a fan as well - check: attributes
				return Types.heater;
			case "sensor":
			case "binary_sensor":
				//TODO: check 'device_class'
				return Types.sensor;
			
			//TODO: implement more
			default:
				return null;
		}
	}
	private static StateType extractStateTypeFromAttributes(Types deviceType, JSONObject attributes){
		if (deviceType == null) return null;
		//states: https://github.com/home-assistant/core/blob/dev/homeassistant/const.py#L315
		switch (deviceType){
			case light:
				return extractStateTypeForLights(attributes);
			
			//TODO: implement more
			default:
				break;
		}
		return null;
	}
	//LIGHTS
	private static StateType extractStateTypeForLights(JSONObject attributes){
		//https://developers.home-assistant.io/docs/core/entity/light
		//relevant: brightness, onoff, color_temp, ..?
		String cm = JSON.getStringOrDefault(attributes, "color_mode", null);
		if (cm == null){
			//get from supported types
			JSONArray scm = JSON.getJArray(attributes, "supported_color_modes");
			if (scm != null){
				if (scm.contains("brightness")){
					return StateType.number_percent;
				}else if (scm.contains("onoff")){
					return StateType.text_binary;
				}
			}
		}else{
			//get what is explicitly set
			if (cm.equals("brightness")){
				return StateType.number_percent;
			}else if (cm.equals("onoff")){
				return StateType.text_binary;
			}
		}
		return null;
	}
	private static String getStateForLights(String basicState, StateType stateType, JSONObject attributes){
		switch (stateType){
			case number_percent:
				if (basicState.equalsIgnoreCase("on")){
					int brightness = JSON.getIntegerOrDefault(attributes, "brightness", -1);
					if (brightness > 0){
						return String.valueOf(Math.round(brightness/255.0f));
					}else{
						return null;
					}
				}else{
					return "0";
				}
			case text_binary:
				break;
			default:
				return basicState;
		}
		return null;
	}
}
