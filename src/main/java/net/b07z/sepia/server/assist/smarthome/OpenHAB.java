package net.b07z.sepia.server.assist.smarthome;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice.StateType;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * OpenHAB integration for smart home HUB interface.
 * 
 * @author Florian Quirin
 *
 */
public class OpenHAB implements SmartHomeHub {
	
	public static final String NAME = "openhab";
	
	public static int CONNECT_TIMEOUT = 4000;
	
	private String hubId;
	private String host;
	private String authType;
	private String authData;
	private JSONObject info;
	
	private static Map<String, Map<String, Set<String>>> bufferedDevicesOfHostByType = new ConcurrentHashMap<>();
	private Map<String, Set<String>> bufferedDevicesByType;
	
	/**
	 * Build OpenHAB connector with given host address.
	 * @param host - e.g. http://localhost:8080
	 */
	public OpenHAB(String host){
		if (Is.nullOrEmpty(host)){
			throw new RuntimeException("No host address found for openHAB integration!");
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
	private JSONObject httpPUT(String url, String tagData, Map<String, String> headers){
		if (Is.notNullOrEmpty(this.authData)){
			headers = addAuthHeader(headers);
		}
		return Connectors.httpPUT(url, tagData, headers);
	}
	private JSONObject httpDELETE(String url){
		if (Is.notNullOrEmpty(this.authData)){
			return Connectors.httpDELETE(url, addAuthHeader(null));
		}else{
			return Connectors.httpDELETE(url);
		}
	}
	
	//-------INTERFACE IMPLEMENTATIONS---------
	
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
			if (authType.equalsIgnoreCase(AuthType.Basic.name())){
				//OK
				this.authType = AuthType.Basic.name();
			
			}else if (authType.equalsIgnoreCase(AuthType.Bearer.name())){
				//OK
				this.authType = AuthType.Bearer.name();
				
			}else{
				throw new RuntimeException("Invalid auth. type. Try 'Basic' or 'Bearer'.");
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
	public Map<String, SmartHomeDevice> getDevices(){
		JSONObject response = httpGET(this.host + "/rest/items");
		//System.out.println("openHAB REST response: " + response); 									//DEBUG
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
				Debugger.println("Service:OpenHAB - devices array was empty!", 1);
				return new HashMap<String, SmartHomeDevice>();
			}
			//Build devices map
			Map<String, SmartHomeDevice> devices = new HashMap<>();
			try{
				for (Object o : devicesArray){
					JSONObject hubDevice = (JSONObject) o;
					//System.out.println("openHAB device JSON: " + hubDevice); 			//DEBUG
					
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
				Debugger.println("Service:OpenHAB - devices array seems to be broken! Msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				return new HashMap<String, SmartHomeDevice>();
			}
			
		}else{
			//Fail with server contact error
			Debugger.println("Service:OpenHAB - failed to get devices from server!", 1);
			return null;
		}
	}
	
	@Override
	public Map<String, Set<String>> getBufferedDeviceNamesByType(){
		if (this.bufferedDevicesByType == null){
			//first run, fill buffer
			getDevices();
		}
		return this.bufferedDevicesByType;
	}
	
	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		//TODO: make this more effective by filtering before instead of loading all devices first
		Map<String, SmartHomeDevice> devices = getDevices();
		if (devices == null){
			return null;
		}else{
			return SmartHomeDevice.getMatchingDevices(devices, filters);
		}
	}
	
	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL)){
			Debugger.println("OpenHAB - 'writeDeviceAttribute' FAILED with msg.: Missing device link!", 1);
			return false;
		}
		//get fresh data first
		JSONObject response = httpGET(deviceURL);
		if (Connectors.httpSuccess(response)){
			//clean up old tags first if needed (how annoying that we have to deal with arrays here - other options?)
			String newTag = attrName + "=" + attrValue;
			JSONArray allTags = JSON.getJArray(response, "tags");
			List<String> oldMemStateTags = new ArrayList<>();
			for (Object tagObj : allTags){
				String t = (String) tagObj;
				if (t.startsWith(attrName + "=")){
					oldMemStateTags.add(t);
				}
			}
			//state is already fine?
			if (oldMemStateTags.size() == 1 && oldMemStateTags.get(0).equals(newTag)){
				return true;
			}else{
				//clean up old tags in case we got some junk left over from any random write request
				String delTag = "";
				try {
					for (String t : oldMemStateTags){
						delTag = t;
						String delURL =  deviceURL + ("/tags/" + URLEncoder.encode(delTag, "UTF-8").replace("+", "%20"));
						if (!Connectors.httpSuccess(httpDELETE(delURL))){
							throw new RuntimeException("Connection or response error.");
						}
					}
				} catch (Exception e) {
					Debugger.println("OpenHAB - 'writeDeviceAttribute' FAILED to delete item tag: " + delTag + " - Msg: " + e.getMessage(), 1);
					return false;
				}
			}
			//build new tag
			try {
				deviceURL += ("/tags/" + URLEncoder.encode(newTag, "UTF-8").replace("+", "%20"));
			} catch (UnsupportedEncodingException e) {
				Debugger.println("OpenHAB - 'writeDeviceAttribute' FAILED to set item tag: " + newTag + " - Msg: " + e.getMessage(), 1);
				return false;
			}
			//set tag
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "text/plain");
			headers.put("Accept", "application/json");
			String body = ""; 		//request body is empty, value set via URL (strange btw. this could be done via GET)
			JSONObject responseWrite = httpPUT(deviceURL, body, headers);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			boolean success = Connectors.httpSuccess(responseWrite); 
			if (!success){
				Debugger.println("OpenHAB - 'writeDeviceAttribute' FAILED - Device: " + device.getName() + " - Response: " + responseWrite, 1);
			}
			return success;
			
		}else{
			return false;
		}
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		long tic = System.currentTimeMillis();
		String id = device.getId();
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL) && Is.notNullOrEmpty(id)){
			deviceURL = this.host + "/rest/items/" + id;
		}
		//System.out.println("state: " + state); 				//DEBUG
		//System.out.println("stateType: " + stateType); 		//DEBUG
		if (Is.nullOrEmpty(deviceURL)){
			Debugger.println("OpenHAB - 'setDeviceState' FAILED with msg.: Missing device link!", 1);
			return false;
		}else{
			//set command overwrite?
			JSONObject setCmds = device.getCustomCommands();
			if (Is.notNullOrEmpty(setCmds)){
				String newState = SmartHomeDevice.getStateFromCustomSetCommands(state, stateType, setCmds);
				if (newState != null){
					state = newState;
				}
				
			//check deviceType to find correct set command
			}else{
				String givenType = device.getType();
				if (stateType != null){
					//ROLLER SHUTTER
					if (givenType != null && Is.typeEqual(givenType, SmartDevice.Types.roller_shutter)){
						if (Is.typeEqualIgnoreCase(state, SmartHomeDevice.State.open)){
							state = "UP";
						}else if (Is.typeEqualIgnoreCase(state, SmartHomeDevice.State.closed)){
							state = "DOWN";
						}
					//ELSE
					}else{
						if (stateType.equals(SmartHomeDevice.StateType.text_binary.name())){
							//all upper case text for openHAB
							state = state.toUpperCase();
						}
					}
					//TODO: improve stateType check (temp. etc)
				}
			}
			//TODO: we could check mem-state here if state is e.g. SmartHomeDevice.STATE_ON
			
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "text/plain");
			headers.put("Accept", "application/json");
			JSONObject response = httpPOST(deviceURL, state, headers);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			boolean success = Connectors.httpSuccess(response); 
			if (!success){
				Statistics.addExternalApiHit("openHAB setDeviceState ERROR");
				Statistics.addExternalApiTime("openHAB setDeviceState ERROR", tic);
				Debugger.println("OpenHAB - 'setDeviceState' FAILED - Sent '" + state + "' got response: " + response, 1);
			}else{
				Statistics.addExternalApiHit("openHAB setDeviceState");
				Statistics.addExternalApiTime("openHAB setDeviceState", tic);
			}
			return success;
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory){
		return writeDeviceAttribute(device, SmartHomeDevice.SEPIA_TAG_MEM_STATE, stateMemory);
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		long tic = System.currentTimeMillis();
		String id = device.getId();
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL) && Is.notNullOrEmpty(id)){
			deviceURL = this.host + "/rest/items/" + id;
		}
		if (Is.nullOrEmpty(deviceURL)){
			Debugger.println("OpenHAB - 'loadDeviceData' FAILED with msg.: Missing device link", 1);
			return null;
		}else{
			JSONObject response = httpGET(deviceURL);
			if (Connectors.httpSuccess(response)){
				Statistics.addExternalApiHit("openHAB loadDevice");
				Statistics.addExternalApiTime("openHAB loadDevice", tic);
				//build device from result
				SmartHomeDevice shd = buildDeviceFromResponse(response);
				return shd;
			}else{
				Statistics.addExternalApiHit("openHAB loadDevice ERROR");
				Statistics.addExternalApiTime("openHAB loadDevice ERROR", tic);
				Debugger.println("OpenHAB - 'loadDeviceData' FAILED with msg.: " + response, 1);
				return null;
			}
		}
	}
	
	/**
	 * Build unified object for SEPIA from HUB device data.
	 * @param hubDevice - data gotten from e.g. call to devices endpoint of HUB
	 * @return
	 */
	private static SmartHomeDevice buildDeviceFromResponse(JSONObject hubDevice){
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
		
		String originalName = JSON.getStringOrDefault(hubDevice, "name", null);	//NOTE: has to be unique
		if (Is.nullOrEmpty(originalName)){
			//we need the ID
			return null;
		}
		JSONArray tags = JSON.getJArray(hubDevice, "tags");
		if (tags != null){
			//try to find self-defined SEPIA tags first
			for (Object tagObj : tags){
				String t = (String) tagObj;
				if (t.startsWith(SmartHomeDevice.SEPIA_TAG_NAME + "=")){
					name = t.split("=", 2)[1];
					if (Is.notNullOrEmpty(name)){
						namedBySepia = true;
					}
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_TYPE + "=")){
					type = t.split("=", 2)[1];
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_ROOM + "=")){
					room = t.split("=", 2)[1];
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_ROOM_INDEX + "=")){
					roomIndex = t.split("=", 2)[1];
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_MEM_STATE + "=")){
					memoryState = t.split("=", 2)[1];				//A state to remember like last non-zero brightness of a light 
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_STATE_TYPE + "=")){
					stateType = t.split("=", 2)[1];
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_SET_CMDS + "=")){
					String setCmdsStr = t.split("=", 2)[1];
					if (setCmdsStr != null && setCmdsStr.trim().startsWith("{")){
						setCmds = JSON.parseString(setCmdsStr);
					}
				}
			}
		}
		//smart-guess if missing sepia-specific settings
		if (name == null){
			//we only accept devices with name
			name = originalName;
		}
		if (type == null){
			String openHabCategory = JSON.getString(hubDevice, "category").toLowerCase();	//NOTE: we prefer category, not type
			String openHabType = JSON.getString(hubDevice, "type").toLowerCase();
			//TODO: category might not be defined
			//TODO: 'type' can give possible set options
			if (Is.notNullOrEmpty(openHabCategory)){
				if (openHabCategory.matches("(.*\\s|^|,)(light.*|lamp.*)")){
					type = SmartDevice.Types.light.name();		//LIGHT
					typeGuessed = true;
				}else if (openHabCategory.matches("(.*\\s|^|,)(heat.*|thermo.*)")){
					type = SmartDevice.Types.heater.name();		//HEATER
					typeGuessed = true;
				}else{
					type = openHabCategory;		//take this if we don't have a specific type yet
				}
			}else if (Is.notNullOrEmpty(openHabType)){
				if (openHabType.equals("rollershutter")){
					type = SmartDevice.Types.roller_shutter.name();		//ROLLER SHUTTER
					typeGuessed = true;
				}
				//TODO: add more
			}
		}
		if (room == null){
			room = "";
		}
		//create common object
		String state = JSON.getStringOrDefault(hubDevice, "state", null);
		//try to deduce state type if not given
		if (Is.nullOrEmpty(stateType) && state != null){
			stateType = SmartHomeDevice.findStateType(state);
			if (stateType != null && type != null && stateType.equals(StateType.number_plain.name())){
				try{
					SmartDevice.Types sdt = SmartDevice.Types.valueOf(type);
					stateType = SmartHomeDevice.makeSmartTypeAssumptionForPlainNumber(sdt);
				}catch (Exception e){
					//ignore. Probably HUB specific, unknown type
				}
			}
		}
		if (state != null){
			if (stateType != null){
				state = SmartHomeDevice.convertAnyStateToGeneralizedState(state, stateType);		
				//TODO: this might require deviceType (see comment inside method)
			}
		}
		//TODO: for temperature we need to check more info (temp. unit? percent? etc...)
		//TODO: clean up stateObj properly and check special format?
		Object linkObj = hubDevice.get("link");
		JSONObject meta = JSON.make(
				"id", originalName,
				"origin", NAME,
				"typeGuessed", typeGuessed,
				"namedBySepia", namedBySepia
		);
		//TODO: we could add some stuff to meta when we need other data from response.
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, 
				state, stateType, memoryState, 
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
}
