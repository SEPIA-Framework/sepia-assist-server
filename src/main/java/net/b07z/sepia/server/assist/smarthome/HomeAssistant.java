package net.b07z.sepia.server.assist.smarthome;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.SmartDevice.Types;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice.StateType;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
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
	
	private static final String SEPIA_STATE = "sepia-state";	//custom state for Home Assistant
	private static final String SEPIA_INTERFACE_DEVICE = SmartHomeDevice.SEPIA_TAG_INTERFACE_DEVICE;
	private static final String SEPIA_INTERFACE_CONFIG = SmartHomeDevice.SEPIA_TAG_INTERFACE_CONFIG;
	private static final String SEPIA_NAME = SmartHomeDevice.SEPIA_TAG_NAME;
	private static final String SEPIA_TYPE = SmartHomeDevice.SEPIA_TAG_TYPE;
	private static final String SEPIA_ROOM = SmartHomeDevice.SEPIA_TAG_ROOM;
	private static final String SEPIA_ROOM_INDEX = SmartHomeDevice.SEPIA_TAG_ROOM_INDEX;
	private static final String SEPIA_MEM_STATE = SmartHomeDevice.SEPIA_TAG_MEM_STATE;
	private static final String SEPIA_STATE_TYPE = SmartHomeDevice.SEPIA_TAG_STATE_TYPE;
	private static final String SEPIA_SET_CMDS = SmartHomeDevice.SEPIA_TAG_SET_CMDS;
	
	/**
	 * Default constructor.
	 */
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
	private JSONObject httpPOST(String url, JSONObject data, Map<String, String> headers){
		if (Is.notNullOrEmpty(this.authData)){
			headers = addAuthHeader(headers);
		}
		return Connectors.httpPOST(url, data.toJSONString(), headers, CONNECT_TIMEOUT);
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
		//System.out.println("HomeAssistant REST response: " + response);				//DEBUG
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
				Debugger.println("HomeAssistant - devices array was empty!", 1);
				return new HashMap<String, SmartHomeDevice>();
			}
			//Build devices map
			Map<String, SmartHomeDevice> devices = new HashMap<>();
			try{
				for (Object o : devicesArray){
					JSONObject hubDevice = (JSONObject) o;
					//System.out.println("HomeAssistant device JSON: " + hubDevice); 	//DEBUG
					
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
				Debugger.println("HomeAssistant - devices array seems to be broken! Msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				return new HashMap<String, SmartHomeDevice>();
			}
			
		}else{
			//Fail with server contact error
			Debugger.println("HomeAssistant - failed to get devices from server!", 1);
			return null;
		}
	}
	
	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device) {
		long tic = System.currentTimeMillis();
		String id = device.getId();
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL) && Is.notNullOrEmpty(id)){
			deviceURL = this.host + "/api/states/" + id;
		}
		if (Is.nullOrEmpty(deviceURL)){
			Debugger.println("HomeAssistant - 'loadDeviceData' FAILED with msg.: Missing device link", 1);
			return null;
		}else{
			JSONObject response = httpGET(deviceURL);
			if (Connectors.httpSuccess(response)){
				Statistics.addExternalApiHit("homeAssistant loadDevice");
				Statistics.addExternalApiTime("homeAssistant loadDevice", tic);
				SmartHomeDevice shd = null;
				if (device.hasInterface()){
					//use given data defined via internal SEPIA HUB
					shd = device;
					//make sure again that this is not null (can happen via
					JSONObject attributes = JSON.getJObject(response, "attributes");
					if (attributes == null){
						//we require non-null for following checks
						attributes = new JSONObject();
					}
					addMoreFromResponse(shd, response, attributes);
				}else{
					//build device from result
					shd = buildDeviceFromResponse(response);
				}
				return shd;
			}else{
				Statistics.addExternalApiHit("homeAssistant loadDevice ERROR");
				Statistics.addExternalApiTime("homeAssistant loadDevice ERROR", tic);
				Debugger.println("HomeAssistant - 'loadDeviceData' FAILED with msg.: " + response, 1);
				return null;
			}
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
	public Map<String, Set<String>> getBufferedDeviceNamesByType() {
		if (this.bufferedDevicesByType == null){
			//first run, fill buffer
			getDevices();
		}
		return this.bufferedDevicesByType;
	}

	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType) {
		long tic = System.currentTimeMillis();
		loadPredefinedInterfaceConfigIfGiven(device);
		String haDevice = device.getInterfaceDeviceId();	//TODO: what if it's not internal interface?
		JSONObject haSetup = device.getInterfaceConfig();
		/*
		System.out.println("haDevice: " + haDevice);		//DEBUG
		System.out.println("haSetup: " + haSetup);			//DEBUG
		System.out.println("stateType set: " + stateType);	//DEBUG
		System.out.println("state set: " + state);			//DEBUG
		*/
		if (Is.nullOrEmpty(haSetup)){
			Debugger.println("HomeAssistant - 'setDeviceState' FAILED with msg.: Missing interface config!", 1);
			return false;
		}else{
			String action = "enable";	//best guess: enable - current options: enable, disable, number, raw
			//set command overwrite?
			JSONObject setCmds = device.getCustomCommands();
			if (Is.notNullOrEmpty(setCmds)){
				//if (givenType != null && Is.typeEqual(givenType, SmartDevice.Types.roller_shutter)){
				SimpleEntry<String, String> se = SmartHomeDevice.getKeyAndStateFromCustomSetCommands(state, stateType, setCmds);
				String newState = se != null? se.getValue() : null;
				if (newState != null){
					action = se.getKey();
					state = newState;
				}
				//TODO: fix roller shutter mapping for enable/disable (here or in SmartHomeHubConnector?)
				
			//check deviceType to find correct set command
			}else{
				String givenStateType = device.getStateType();
				if (stateType != null){
					/*//ROLLER SHUTTER
					String givenType = device.getType();
					if (givenType != null && Is.typeEqual(givenType, Types.roller_shutter)){
						if (Is.typeEqualIgnoreCase(state, SmartHomeDevice.State.open)){
							state = "UP";
						}else if (Is.typeEqualIgnoreCase(state, SmartHomeDevice.State.closed)){
							state = "DOWN";
						}
					}*/
					if (stateType.equals(StateType.text_binary.name())){
						if (Is.typeEqualIgnoreCase(state, SmartHomeDevice.State.on)){
							action = "enable";
							if (givenStateType.equals(StateType.number_percent.name())){
								state = device.getStateMemory();	//TODO: is this ever set?
								if (Is.nullOrEmpty(state)){
									//assume ON is 100 percent
									state = "100";
								}
							}
						}else if (Is.typeEqualIgnoreCase(state, SmartHomeDevice.State.off)){
							action = "disable";
							if (givenStateType.equals(StateType.number_percent.name())){
								//assume OFF is 0 percent
								state = "0";
							}
						}
						//state = ...
					}else if (stateType.startsWith("number_")){
						action = "number";
					}
				}
			}
			//convert action to HA service
			JSONObject haAction;
			if (action.equals("disable")){
				haAction = JSON.getJObject(haSetup, "off");
			}else if (action.equals("enable")){
				haAction = JSON.getJObject(haSetup, "on");
			}else{
				haAction = JSON.getJObject(haSetup, "set");
			}
			if (haAction == null){
				Debugger.println("HomeAssistant - 'setDeviceState' FAILED with msg.: Missing interface config entry!", 1);
				return false;
			}
			//intent or service
			String intentName = JSON.getStringOrDefault(haAction, "intent", null);
			String serviceName = JSON.getStringOrDefault(haAction, "service", null);
			boolean useService = Is.notNullOrEmpty(serviceName);
			if (!useService && Is.nullOrEmpty(intentName)){
				Debugger.println("HomeAssistant - 'setDeviceState' FAILED with msg.: Missing 'service' or 'intent' in config!", 1);
				return false;
			}
			//data expression
			JSONObject reqData = JSON.make("entity_id", haDevice);
			String writeExpression = JSON.getStringOrDefault(haAction, "write", null);
			if (Is.notNullOrEmpty(writeExpression)){
				JSONObject data = SmartHomeDevice.buildStateDataFromWriteExpression(writeExpression, state);
				if (useService){
					//requires attributes
					if (data != null && data.containsKey("attributes")){
						JSONObject attr = JSON.getJObject(data, new String[]{"attributes"});
						if (attr != null){
							JSON.forEach(attr, (key, val) -> {
								JSON.put(reqData, key, val);
							});
						}
					}
				}else{
					//everything is allowed for intents
					if (data != null){
						JSON.deepMerge(data, reqData);
					}
				}
			}
			JSONObject reqBody;
			String haActionURL = this.host;
			if (useService){
				//service
				haActionURL += "/api/services/" + serviceName;
				reqBody = reqData;
			}else{
				//intent
				haActionURL += "/api/intent/handle";
				reqBody = JSON.make("name", intentName, "data", reqData);
			}
			/*
			System.out.println("haActionURL: " + haActionURL);	//DEBUG
			System.out.println("haAction: " + haAction);		//DEBUG
			System.out.println("reqBody: " + reqBody);			//DEBUG
			*/
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json");
			headers.put("Accept", "application/json");
			JSONObject response = httpPOST(haActionURL, reqBody, headers);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			boolean success = Connectors.httpSuccess(response); 
			if (!success){
				Statistics.addExternalApiHit("homeAssistant setDeviceState ERROR");
				Statistics.addExternalApiTime("homeAssistant setDeviceState ERROR", tic);
				Debugger.println("HomeAssistant - 'setDeviceState' FAILED - Sent '" + state + "' got response: " + response, 1);
			}else{
				Statistics.addExternalApiHit("homeAssistant setDeviceState");
				Statistics.addExternalApiTime("homeAssistant setDeviceState", tic);
			}
			return success;
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory) {
		// TODO Auto-generated method stub
		return false;
	}

	//-----------------
	
	//helper class to hold custom interface config
	private static class HaInterfaceConfig {
		public final JSONObject onConfig;	//e.g.: {"service": "light/turn_on"} OR {"intent": "TurnOnLight"} 
		public final JSONObject setConfig;	//e.g.: {"service": "light/turn_on", "write": "<attributes.brightness_pct>"}
		public final JSONObject offConfig;	//e.g.: {"service": "light/turn_off"}
		public final String readExpression;	//e.g.: "round(<attributes.brightness>*0.39)"
		public final JSONObject defaultState;
		//public final StateType stateType;
		
		public HaInterfaceConfig(JSONObject onConfig, JSONObject setConfig, JSONObject offConfig,
				String readExpression, JSONObject defaultState){
			this.onConfig = onConfig;
			this.setConfig = setConfig;
			this.offConfig = offConfig;
			this.readExpression = readExpression;
			this.defaultState = defaultState;
		}
		public HaInterfaceConfig(JSONObject jsonConfig){
			//try 'on' for 'set' and vice versa if one is null
			this(jsonConfig.containsKey("on")? JSON.getJObject(jsonConfig, "on") : JSON.getJObject(jsonConfig, "set"),
				jsonConfig.containsKey("set")? JSON.getJObject(jsonConfig, new String[]{"set"}) : JSON.getJObject(jsonConfig, "on"),
				JSON.getJObject(jsonConfig, "off"),
				JSON.getStringOrDefault(jsonConfig, "read", null),
				JSON.getJObject(jsonConfig, "default")
			);
		}
		public JSONObject getJson(){
			JSONObject json = JSON.make(
				"on", this.onConfig,
				"set", this.setConfig,
				"off", this.offConfig,
				"read", this.readExpression,
				"default", this.defaultState
			);
			return json;
			//NOTE: 'stateType' is not included but set via 'device' when a predefined HA config is used
		}
		
		public static HaInterfaceConfig getFromName(String configName){
			return haInterfaceConfigMap.get(configName);
		}
	}
	private static Map<String, HaInterfaceConfig> haInterfaceConfigMap;
	private static Map<String, StateType> haInterfaceConfigStateTypeMap;
	//private static Map<String, JSONObject> haSetCommandsMap;
	static {
		haInterfaceConfigMap = new HashMap<>();
		haInterfaceConfigStateTypeMap = new HashMap<>();
		//haSetCommandsMap = new HashMap<>();
		
		//Lights - on/off
		haInterfaceConfigMap.put("light.onoff", new HaInterfaceConfig(
			JSON.make("service", "light/turn_on"), JSON.make("service", "light/turn_on"), JSON.make("service", "light/turn_off"),
			"<state>", JSON.make("state", "off", "value", "off")));
		haInterfaceConfigStateTypeMap.put("light.onoff", StateType.text_binary);
		//haSetCommandsMap.put("light.onoff", JSON.make("enable", "on", "disable", "off", "number", "on"));
		
		//Lights - brightness
		haInterfaceConfigMap.put("light.brightness", new HaInterfaceConfig(
			JSON.make("service", "light/turn_on"), JSON.make("service", "light/turn_on", "write", "<attributes.brightness_pct>"), JSON.make("service", "light/turn_off"),
			"round(<attributes.brightness>*0.392)", JSON.make("state", "off", "value", "0")));
		haInterfaceConfigStateTypeMap.put("light.brightness", StateType.number_percent);
		//haSetCommandsMap.put("light.brightness", JSON.make("enable", "100", "disable", "0", "number", "<val>"));
	
		//Sensor - state
		haInterfaceConfigMap.put("sensor.state", new HaInterfaceConfig(
			JSON.make("service", ""), JSON.make("service", "", "write", ""), JSON.make("service", ""),
			"<state>", JSON.make("state", "unknown")));
		haInterfaceConfigStateTypeMap.put("sensor.state", StateType.text_raw);
		//haSetCommandsMap.put("sensor.state", JSON.make("enable", "", "disable", "", "raw", "<val>"));
		
		//Intent - demo
		haInterfaceConfigMap.put("intent.demo", new HaInterfaceConfig(
			JSON.make("intent", "SepiaDemoOn", "write", "<state>"), JSON.make("intent", "SepiaDemoSet", "write", "<state>"), JSON.make("intent", "SepiaDemoOff", "write", "<state>"),
			"<state>", JSON.make("state", "unknown")));
		haInterfaceConfigStateTypeMap.put("intent.demo", StateType.number_plain);
		//haSetCommandsMap.put("intent.demo", JSON.make("enable", "", "disable", "", "number", "<val>"));
	}
	
	/**
	 * Build unified object for SEPIA from HUB device data.
	 * @param hubDevice - data gotten from e.g. call to devices endpoint of HUB
	 * @return
	 */
	private static SmartHomeDevice buildDeviceFromResponse(JSONObject hubDevice){
		//Home Assistant SEPIA settings
		String haProxyEntity = null;
		JSONObject sepiaHaSetup = null;
		//Build unified object for SEPIA
		String name = null;
		String type = null;
		String room = null;
		String roomIndex = null;
		String memoryState = "";
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
			//SEPIA config exists?
			haProxyEntity = JSON.getStringOrDefault(attributes, SEPIA_INTERFACE_DEVICE, null);
			String sepiaHaSetupStr = JSON.getStringOrDefault(attributes, SEPIA_INTERFACE_CONFIG, null);
			if (sepiaHaSetupStr != null && sepiaHaSetupStr.trim().startsWith("{")){
				try {
					sepiaHaSetup = JSON.parseStringOrFail(sepiaHaSetupStr);
				}catch (Exception e){
					Debugger.println("HomeAssistant - tried to parse '" + SEPIA_INTERFACE_CONFIG 
							+ "' for entity '" + entityId + "' but failed!", 1);
				}
			}
			//try to find SEPIA tags first
			name = JSON.getStringOrDefault(attributes, SEPIA_NAME, null);
			if (Is.notNullOrEmpty(name)){
				namedBySepia = true;
			}else{
				name = JSON.getStringOrDefault(attributes, "friendly_name", null);
			}
			type = JSON.getStringOrDefault(attributes, SEPIA_TYPE, null);
			room = JSON.getStringOrDefault(attributes, SEPIA_ROOM, null);
			roomIndex = JSON.getStringOrDefault(attributes, SEPIA_ROOM_INDEX, null);
			memoryState = JSON.getStringOrDefault(attributes, SEPIA_MEM_STATE, null);
			stateType = JSON.getStringOrDefault(attributes, SEPIA_STATE_TYPE, null);
			String setCmdsStr = JSON.getStringOrDefault(attributes, SEPIA_SET_CMDS, null);
			if (setCmdsStr != null && setCmdsStr.trim().startsWith("{")){
				try {
					setCmds = JSON.parseStringOrFail(setCmdsStr);
				}catch (Exception e){
					Debugger.println("HomeAssistant - tried to parse '" + SEPIA_SET_CMDS 
							+ "' for entity '" + entityId + "' but failed!", 1);
				}
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
				typeGuessed = true;
			}else{
				//TODO: device with unknown type - break or use unknown?
				dt = Types.unknown;
			}
			type = dt.name();
		}else{
			dt = Types.valueOf(type);
		}
		
		//extract interface config using predefined setups
		if (sepiaHaSetup == null){
			String configName = extractInterfaceConfigFromAttributes(dt, attributes);
			if (configName != null){
				HaInterfaceConfig haic = HaInterfaceConfig.getFromName(configName);
				if (haic != null){
					sepiaHaSetup = haic.getJson();
					//add stateType info?
					if (stateType == null){
						stateType = haInterfaceConfigStateTypeMap.get(configName).name();
					}
				}
			}
		}
		//NOTE: else 'haic' restore from predefined "config" and stateType mismatch is handled below
		
		//build
		Object linkObj = null;		//since read and write don't use the same URL we don't set it
		JSONObject meta = JSON.make(
			SmartHomeDevice.META_ID, entityId,
			SmartHomeDevice.META_ORIGIN, NAME,
			SmartHomeDevice.META_TYPE_GUESSED, typeGuessed,
			SmartHomeDevice.META_NAMED_BY_SEPIA, namedBySepia
		);
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, null, stateType, memoryState, 
			(linkObj != null)? linkObj.toString() : null, meta);
		//specify more
		if (Is.notNullOrEmpty(roomIndex)){
			shd.setRoomIndex(roomIndex);
		}
		if (Is.notNullOrEmpty(setCmds)){
			shd.setCustomCommands(setCmds);
		}
		//interface config for HA
		if (Is.notNullOrEmpty(haProxyEntity)){
			shd.setInterfaceDeviceId(haProxyEntity);
		}else{
			shd.setInterfaceDeviceId(entityId);
		}
		if (Is.notNullOrEmpty(sepiaHaSetup)){
			shd.setInterfaceConfig(sepiaHaSetup);
		}
		
		//add rest
		addMoreFromResponse(shd, hubDevice, attributes);
		
		return shd;
	}
	private static void addMoreFromResponse(SmartHomeDevice shd, JSONObject hubDevice, JSONObject attributes){
		//add URL
		//Object linkObj = null;	//NOTE: since read and write don't use the same URL we don't set it
		//TODO: we could add some stuff to meta that is only found in response (not configurable via UI/internal HUB).
		
		//check if config is individual or name reference
		loadPredefinedInterfaceConfigIfGiven(shd);
				
		//get state
		String state = getCommonState(shd, hubDevice, attributes);
		shd.setState(state);
	}
	private static String getCommonState(SmartHomeDevice shd, JSONObject hubDevice,	JSONObject attributes){
		String state = null;
		String deviceType = shd.getType();
		String stateType = shd.getStateType();
		String stateBasic = JSON.getStringOrDefault(hubDevice, "state", null);
		if (stateBasic == null || stateBasic.equalsIgnoreCase("unknown") || stateBasic.equalsIgnoreCase("unavailable")){
			//device not reachable?
			return null;
		}
		//specifically defined field for "clean" state?
		state = getStateFromCustomField(attributes);	//if exists we fully trust the HA script to give good values 
		if (Is.nullOrEmpty(state)){
			//use interface config?
			JSONObject sepiaHaSetup = shd.getInterfaceConfig();
			if (Is.notNullOrEmpty(sepiaHaSetup)){
				state = getStateUsingConfig(new HaInterfaceConfig(sepiaHaSetup), hubDevice);
				if (state == null && sepiaHaSetup.containsKey("default")){
					//fallback to default
					Object refState = JSON.getObject(sepiaHaSetup, new String[]{"default", "state"});
					if (refState != null &&  Converters.obj2StringOrDefault(refState, "").equalsIgnoreCase(stateBasic)){
						Object refValue = JSON.getObject(sepiaHaSetup, new String[]{"default", "value"});
						if (refValue != null){
							//always make sure we don't accidentally cast number to string and crash ^^
							state = Converters.obj2StringOrDefault(refValue, null);
						}
					}
				}
			
			//use best guess
			}else{
				switch (deviceType){
					case "light":
						state = getStateForLights(stateBasic, stateType, attributes);
						break;
					case "sensor":
						state = stateBasic;
						break;
					case "unknown":
						state = stateBasic;
						
					//TODO: implement more
					default:
						break;
				}
			}
		}
		if (state != null){
			if (stateType != null){
				state = SmartHomeDevice.convertAnyStateToGeneralizedState(state, stateType);
			}
		}
		return state;
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
	
	private static String extractInterfaceConfigFromAttributes(Types deviceType, JSONObject attributes) {
		if (deviceType == null) return null;
		switch (deviceType){
			//LIGHT
			case light:
				JSONArray scm = JSON.getJArray(attributes, "supported_color_modes");
				if (Is.nullOrEmpty(scm)){
					return "light.onoff";
				}else if (scm.contains("brightness")){
					return "light.brightness";
				}else if (scm.contains("onoff")){
					return "light.onoff";
				}
				//TODO: check more (e.g. something with color)
				return null;
			
			//TODO: implement more
			default:
				return null;
		}
	}
	private static void loadPredefinedInterfaceConfigIfGiven(SmartHomeDevice shd){
		JSONObject sepiaHaSetup = shd.getInterfaceConfig();
		if (sepiaHaSetup != null && sepiaHaSetup.containsKey("config")){
			String configName = JSON.getStringOrDefault(sepiaHaSetup, "config", null);
			if (Is.notNullOrEmpty(configName)){
				HaInterfaceConfig haic = HaInterfaceConfig.getFromName(configName);
				if (haic != null){
					//replace with predefined config
					sepiaHaSetup = haic.getJson();
					shd.setInterfaceConfig(sepiaHaSetup);
					//stateType
					//TODO: what if there is a mismatch between shd.getStateType() and haic.stateType??
				}
			}
		}
	}
	
	//state via config
	private static String getStateUsingConfig(HaInterfaceConfig haic, JSONObject hubDevice){
		if (haic == null) return null;
		String readExpression = Is.notNullOrEmpty(haic.readExpression)? haic.readExpression : "<state>";	//"state" is HA default
		return SmartHomeDevice.getStateFromJsonViaExpression(readExpression, hubDevice);
	}
	
	//state via CUSTOM
	private static String getStateFromCustomField(JSONObject attributes){
		return Converters.obj2StringOrDefault(attributes.get(SEPIA_STATE), null);
	}

	//state for LIGHTS
	private static String getStateForLights(String basicState, String stateType, JSONObject attributes){
		switch (StateType.valueOf(stateType)){
			case number_percent:
				if (basicState.equalsIgnoreCase("on")){
					int brightness = JSON.getIntegerOrDefault(attributes, "brightness", -1);
					if (brightness > 0){
						return String.valueOf(Math.round(100.0f*brightness/255.0f));
					}else{
						return null;
					}
				}else{
					return "0";
				}
			case text_binary:
				return basicState;
			default:
				return basicState;
		}
	}
}
