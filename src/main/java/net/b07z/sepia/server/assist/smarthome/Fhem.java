package net.b07z.sepia.server.assist.smarthome;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

/**
* FHEM integration for smart home HUB interface.
* 
* @author Florian Quirin
 */
public class Fhem implements SmartHomeHub {
	
	private String host;
	private String authType;
	private String authData;
	private String csrfToken = "";
	public static final String NAME = "fhem";
	
	private static final String TAG_NAME = SmartHomeDevice.SEPIA_TAG_NAME;
	private static final String TAG_TYPE = SmartHomeDevice.SEPIA_TAG_TYPE;
	private static final String TAG_ROOM = SmartHomeDevice.SEPIA_TAG_ROOM;
	private static final String TAG_ROOM_INDEX = SmartHomeDevice.SEPIA_TAG_ROOM_INDEX;
	private static final String TAG_DATA = SmartHomeDevice.SEPIA_TAG_DATA;
	private static final String TAG_MEM_STATE = SmartHomeDevice.SEPIA_TAG_MEM_STATE;
	private static final String TAG_STATE_TYPE = SmartHomeDevice.SEPIA_TAG_STATE_TYPE;
	
	/**
	 * Create new FHEM instance and automatically get CSRF token.
	 * @param host - e.g.: http://localhost:8083/fhem
	 */
	public Fhem(String host){
		if (Is.nullOrEmpty(host)){
			throw new RuntimeException("No host address found for FHEM integration!");
		}else{
			this.host = host;
			this.csrfToken = getCsrfToken(this.host);
		}
	}
	/**
	 * Create new FHEM instance with given CSRF token.
	 * @param host - e.g.: http://localhost:8083/fhem
	 * @param csrfToken - e.g.: csrf_12345...
	 */
	public Fhem(String host, String csrfToken){
		if (Is.nullOrEmpty(host)){
			throw new RuntimeException("No host address found for FHEM integration!");
		}else{
			this.host = host;
			this.csrfToken = csrfToken;
		}
	}
	
	//HTTP call methods for HUB
	private Map<String, String> addAuthHeader(Map<String, String> headers){
		return Connectors.addAuthHeader(headers, this.authType, this.authData);
	}
	private JSONObject httpGET(String url){
		if (Is.notNullOrEmpty(this.authData)){
			return Connectors.httpGET(url, null, addAuthHeader(null));
		}else{
			return Connectors.httpGET(url);
		}
	}
	
	//-------INTERFACE IMPLEMENTATIONS---------
	
	@Override
	public void setHostAddress(String hostUrl){
		this.host = hostUrl;
	}
	
	@Override
	public void setAuthenticationInfo(String authType, String authData){
		this.authType = authType;
		this.authData = authData;
	}
	
	@Override
	public boolean registerSepiaFramework(){
		//Find attributes first
		String foundAttributes = "";
		String getUrl = URLBuilder.getString(this.host, 
				"?cmd=", "jsonlist2 global",
				"&XHR=", "1",
				"&fwcsrf=", this.csrfToken
		);
		try {
			//Call and check
			JSONObject resultGet = httpGET(getUrl);
			if (Connectors.httpSuccess(resultGet) && JSON.getIntegerOrDefault(resultGet, "totalResultsReturned", 0) == 1){
				foundAttributes = (String) JSON.getJObject((JSONObject) JSON.getJArray(resultGet, "Results").get(0), "Attributes").get("userattr");
				if (Is.nullOrEmpty(foundAttributes)){
					Debugger.println("FHEM - registerSepiaFramework: Failed! No existing 'userattr' found in 'global' attributes. Please add manually.", 1);
					return false;
				}else{
					//System.out.println("foundAttributes: " + foundAttributes); 			//DEBUG
					//check if attributes are already there (if one is there all should be there ...)
					if (foundAttributes.matches(".*\\b" + TAG_NAME + "\\b.*")){
						return true;
					}
				}
			}else{
				Debugger.println("FHEM - registerSepiaFramework: Failed! Could not load global attributes. Msg.: " + resultGet.toJSONString(), 1);
				return false;
			}
			//Register FHEM means adding the SEPIA tags to global attributes
			String setUrl = URLBuilder.getString(this.host, 
					"?cmd=", "attr global userattr " + foundAttributes + " " 
							+ TAG_NAME + " " + TAG_TYPE + " " + TAG_ROOM + " " + TAG_ROOM_INDEX + " " 
							+ TAG_DATA + " " 
							+ TAG_MEM_STATE + " " + TAG_STATE_TYPE,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			JSONObject resultSet = httpGET(setUrl);
			boolean gotErrorMessage = false;
			if (resultSet != null && resultSet.containsKey("STRING")){
				String msg = JSON.getString(resultSet, "STRING");
				gotErrorMessage = !msg.isEmpty() && !msg.toLowerCase().contains("ok") && !msg.toLowerCase().contains("success");
			}
			if (Connectors.httpSuccess(resultSet) && !gotErrorMessage){
				//all good
				return true;
			}else{
				Debugger.println("FHEM - registerSepiaFramework: Failed! Could not set global attributes. Msg.: " + resultSet.toJSONString(), 1);
				return false;
			}
		}catch (Exception e){
			Debugger.println("FHEM - registerSepiaFramework: Failed! Error: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
			return false;
		}
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices(){
		String url = URLBuilder.getString(this.host, 
				"?cmd=", "jsonlist2",
				"&XHR=", "1",
				"&fwcsrf=", this.csrfToken
		);
		JSONObject result = httpGET(url);
		if (Connectors.httpSuccess(result)){
			try {
				Map<String, SmartHomeDevice> devices = new HashMap<>();
				JSONArray devicesArray = JSON.getJArray(result, "Results");
				for (Object o : devicesArray){
					JSONObject hubDevice = (JSONObject) o;
					
					//Build unified object for SEPIA
					SmartHomeDevice shd = buildDeviceFromResponse(hubDevice);
					
					//devices
					if (shd != null){
						devices.put(shd.getName(), shd);
					}
				}
				return devices;
				
			}catch (Exception e){
				Debugger.println("FHEM - getDevices FAILED with msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				return null;
			}
		}else{
			Debugger.println("FHEM - getDevices FAILED with msg.: " + result.toJSONString(), 1);
			return null;
		}
	}
	
	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		//TODO: make this more effective by filtering before instead of loading all devices first
		Map<String, SmartHomeDevice> devices = getDevices();
		if (devices == null){
			return null;
		}else{
			//filters
			String deviceType = (String) filters.get("type");
			String roomType = (String) filters.get("room");
			String roomIndex = (String) filters.get("roomIndex");
			Object limitObj = filters.get("limit");
			int limit = -1;
			if (limitObj != null){
				limit = (int) limitObj;
			}
			//get all devices with right type and optionally right room
			List<SmartHomeDevice> matchingDevices = SmartHomeDevice.getMatchingDevices(devices, deviceType, roomType, roomIndex, limit);
			return matchingDevices;
		}
	}
	
	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		String fhemId = device.getMetaValueAsString("id");
		String deviceCmdLink = device.getLink(); 
		if (Is.nullOrEmpty(fhemId) || Is.nullOrEmpty(deviceCmdLink)){
			return false;
		}else{
			String cmdUrl = URLBuilder.getString(
					deviceCmdLink, "=", "attr " + fhemId + " " + attrName + " " + attrValue,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			JSONObject response = httpGET(cmdUrl);
			boolean gotErrorMessage = false;
			if (response != null && response.containsKey("STRING")){
				String msg = JSON.getString(response, "STRING");
				gotErrorMessage = (msg.toLowerCase().contains("unknown attribute"));
				if (gotErrorMessage){
					Debugger.println("FHEM interface error in 'writeDeviceAttribute': " + msg, 1);
				}
			}
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			return (Connectors.httpSuccess(response) && !gotErrorMessage);
		}
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		String fhemId = device.getMetaValueAsString("id");
		if (Is.nullOrEmpty(fhemId)){
			return null;
		}else{
			String deviceURL = URLBuilder.getString(this.host, 
					"?cmd=", "jsonlist2 " + fhemId,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			JSONObject response = httpGET(deviceURL);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			if (Connectors.httpSuccess(response)){
				try {
					JSONArray devicesArray = JSON.getJArray(response, "Results");
					if (devicesArray.size() != 1){
						throw new RuntimeException("Result was null or not unique! Result size: " + devicesArray.size());
					}
					JSONObject hubDevice = JSON.getJObject(devicesArray, 0);
					//build shd from response
					SmartHomeDevice shd = buildDeviceFromResponse(hubDevice);
					return shd;
					
				}catch (Exception e){
					Debugger.println("FHEM - loadDeviceData FAILED with msg.: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
					return null;
				}
			}else{
				return null;
			}
		}
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		String fhemId = device.getMetaValueAsString("id");
		String setOptions = device.getMetaValueAsString("setOptions");
		if (Is.nullOrEmpty(setOptions)){
			//this is required info
			return false;
		}
		String deviceCmdLink = device.getLink(); 
		if (Is.nullOrEmpty(fhemId) || Is.nullOrEmpty(deviceCmdLink)){
			return false;
		}else{
			//check deviceType
			String givenType = device.getType();
			if (stateType != null){
				//LIGHT
				if (givenType != null && Is.typeEqual(givenType, SmartDevice.Types.light)){
					//check stateType
					if (stateType.equals(SmartHomeDevice.STATE_TYPE_NUMBER_PERCENT)){
						String cmd = NluTools.stringFindFirst(setOptions, "\\b(pct|dim|bri)(?=(\\b|\\d))");
						//percent via pct, dim or bri
						if (!cmd.isEmpty()){
							state = cmd + " " + state;	//TODO: this can FAIL if device uses discrete states
						}
					}else{
						state = state.toLowerCase();	//on, off, etc is usually lower-case in FHEM
					}
				//ROLLER SHUTTER
				}else if (givenType != null && Is.typeEqual(givenType, SmartDevice.Types.roller_shutter)){
					//check stateType
					if (stateType.equals(SmartHomeDevice.STATE_TYPE_NUMBER_PERCENT)){
						//percent via "pct" - TODO: does that work?
						state = "pct " + state;
					}else if (state.equalsIgnoreCase(SmartHomeDevice.STATE_OPEN)){
						state = "up";
					}else if (state.equalsIgnoreCase(SmartHomeDevice.STATE_CLOSED)){
						state = "down";
					}else{
						state = state.toLowerCase();	//on, off, etc is usually lower-case in FHEM
					}
				//ELSE
				}else{
					//check stateType
					if (stateType.equals(SmartHomeDevice.STATE_TYPE_NUMBER_PERCENT)){
						String cmd = NluTools.stringFindFirst(setOptions, "\\b(pct|dim)(?=(\\b|\\d))");
						//percent via pct or dim - TODO: does that work?
						if (!cmd.isEmpty()){
							state = cmd + " " + state;	//TODO: this can FAIL if device uses discrete states
						}
					}else{
						state = state.toLowerCase();	//on, off, etc is usually lower-case in FHEM
					}
				}
			}
			//TODO: add temperature
			//TODO: we could check mem-state here if state is e.g. SmartHomeDevice.STATE_ON
			
			String cmdUrl = URLBuilder.getString(
					deviceCmdLink, "=", "set " + fhemId + " " + state,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			//System.out.println("URL: " + cmdUrl); 			//DEBUG
			JSONObject response = httpGET(cmdUrl);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			boolean gotErrorMessage = false;
			if (response != null && response.containsKey("STRING")){
				String msg = JSON.getString(response, "STRING");
				gotErrorMessage = !msg.isEmpty() && !msg.toLowerCase().contains("ok") && !msg.toLowerCase().contains("success");
				if (gotErrorMessage){
					Debugger.println("FHEM interface error in 'setDeviceState': " + msg, 1);
				}
			}
			
			return (Connectors.httpSuccess(response) && !gotErrorMessage);
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String memState){
		return writeDeviceAttribute(device, SmartHomeDevice.SEPIA_TAG_MEM_STATE, memState);
	}
	
	//------------- FHEM specific helper methods --------------
	
	/**
	 * Get CSRF token from FHEM server.
	 * @param smartHomeHubHost - host base URL
	 * @return token or null
	 */
	public static String getCsrfToken(String smartHomeHubHost){
		try{
			//HttpClientResult httpRes = Connectors.apacheHttpGET(smartHomeHubHost + "?XHR=1", null);
			//service.addToExtendedLog(httpRes.headers);		//NOTE: you may use this in SEPIA-Home v2.3.2+
			URLConnection conn = new URL(smartHomeHubHost + "?XHR=1").openConnection();
			//read header
			return conn.getHeaderField("X-FHEM-csrfToken").toString();
			
		}catch (Exception e){
			Debugger.println("FHEM - getCsrfToken FAILED with msg.: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
			return null;
		}
	}
	
	//build device from JSON response
	private SmartHomeDevice buildDeviceFromResponse(JSONObject hubDevice){
		//Build unified object for SEPIA
		JSONObject internals = JSON.getJObject(hubDevice, "Internals");
		JSONObject attributes = JSON.getJObject(hubDevice, "Attributes");
		String name = null;
		String type = null;
		String room = null;
		String roomIndex = null;
		String memoryState = "";
		String stateType = null;
		boolean typeGuessed = false;
		if (attributes != null){
			//try to find self-defined SEPIA tags first
			name = JSON.getStringOrDefault(attributes, TAG_NAME, null);
			type = JSON.getStringOrDefault(attributes, TAG_TYPE, null);
			room = JSON.getStringOrDefault(attributes, TAG_ROOM, null);
			roomIndex = JSON.getStringOrDefault(attributes, TAG_ROOM_INDEX, null);
			memoryState = JSON.getStringOrDefault(attributes, TAG_MEM_STATE, null);
			stateType = JSON.getStringOrDefault(attributes, TAG_STATE_TYPE, null);
		}
		//smart-guess if missing sepia-specific settings
		if (name == null && internals != null){
			name = JSON.getStringOrDefault(internals, "name", JSON.getStringOrDefault(internals, "NAME", null));		//NOTE: has to be unique!
		}
		if (name == null){
			//we only accept devices with name
			return null;
		}
		if (type == null && internals != null){
			String fhemType = JSON.getStringOrDefault(internals, "type", JSON.getStringOrDefault(internals, "TYPE", "")).toLowerCase();
			//filter
			if (fhemType.equalsIgnoreCase("FHEMWEB") 
					|| fhemType.equalsIgnoreCase("Global") 
					|| fhemType.equalsIgnoreCase("fileLog") 
					|| fhemType.equalsIgnoreCase("eventTypes")
					|| fhemType.equalsIgnoreCase("notify")){
				return null;
			}
			if (fhemType.matches("(.*\\s|^|,)(light.*|lamp.*)")){
				type = SmartDevice.Types.light.name();		//LIGHT
			}else if (fhemType.matches("(.*\\s|^|,)(heat.*|thermo.*)")){
				type = SmartDevice.Types.heater.name();		//HEATER
			}else{
				type = fhemType;		//take this if we don't have a specific type yet
			}
			typeGuessed = true;
		}
		if (room == null && attributes != null){
			String fhemRoom = JSON.getString(attributes, "room").toLowerCase();
			room = fhemRoom;
		}
		//create common object
		String fhemObjName = JSON.getStringOrDefault(hubDevice, "Name", null);
		//JSONObject stateObj = JSON.getJObject(hubDevice, new String[]{"Readings", "state"});
		//String state = (stateObj != null)? JSON.getString(stateObj, "Value") : null;
		String state = JSON.getStringOrDefault(internals, "STATE", null);
		//try to deduce state type if not given
		if (Is.nullOrEmpty(stateType) && state != null){
			stateType = SmartHomeDevice.convertStateType(null, state, null);
		}
		if (state != null){
			if (stateType != null){
				state = SmartHomeDevice.convertState(state, stateType);		//TODO: this might require deviceType (see comment inside method)
			}
		}
		//TODO: for temperature we need to check more info (temp. unit? percent? etc...)
		//TODO: clean up state and set stateType according to values like 'dim50%'
		Object linkObj = (fhemObjName != null)? (this.host + "?cmd." + fhemObjName) : null;
		JSONObject meta = JSON.make(
				"id", fhemObjName,
				"origin", NAME,
				"setOptions", JSON.getStringOrDefault(hubDevice, "PossibleSets", null),
				"typeGuessed", typeGuessed
		);
		//note: we need 'id' for commands although it is basically already in 'link'
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, 
				state, stateType, memoryState, 
				(linkObj != null)? linkObj.toString() : null, meta);
		//specify more
		if (Is.notNullOrEmpty(roomIndex)){
			shd.setRoomIndex(roomIndex);
		}
		return shd;
	}
}
