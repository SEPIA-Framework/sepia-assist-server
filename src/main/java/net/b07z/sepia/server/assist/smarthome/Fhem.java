package net.b07z.sepia.server.assist.smarthome;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
	private String csrfToken = "";
	public static final String NAME = "fhem";
	
	private static final String TAG_NAME = SmartHomeDevice.SEPIA_TAG_NAME;
	private static final String TAG_TYPE = SmartHomeDevice.SEPIA_TAG_TYPE;
	private static final String TAG_ROOM = SmartHomeDevice.SEPIA_TAG_ROOM;
	private static final String TAG_DATA = SmartHomeDevice.SEPIA_TAG_DATA;
	private static final String TAG_MEM_STATE = SmartHomeDevice.SEPIA_TAG_MEM_STATE;
	
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
	
	@Override
	public void setHostAddress(String hostUrl){
		this.host = hostUrl;
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
			JSONObject resultGet = Connectors.httpGET(getUrl);
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
					"?cmd=", "attr global userattr " + foundAttributes + " " + TAG_NAME + " " + TAG_TYPE + " " + TAG_ROOM + " " + TAG_DATA + " " + TAG_MEM_STATE,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			JSONObject resultSet = Connectors.httpGET(setUrl);
			if (Connectors.httpSuccess(resultSet)){
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
	public Map<String, SmartHomeDevice> getDevices(String optionalNameFilter, String optionalTypeFilter, String optionalRoomFilter){
		//TODO: we currently ignore result filtering
		String url = URLBuilder.getString(this.host, 
				"?cmd=", "jsonlist2",
				"&XHR=", "1",
				"&fwcsrf=", this.csrfToken
		);
		JSONObject result = Connectors.httpGET(url);
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
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		String fhemId = device.getMetaValueAsString("fhem-id");
		String deviceCmdLink = device.getLink(); 
		if (Is.nullOrEmpty(fhemId) || Is.nullOrEmpty(deviceCmdLink)){
			return false;
		}else{
			String cmdUrl = URLBuilder.getString(
					deviceCmdLink, "=", "attr " + fhemId + " " + attrName + " " + attrValue,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			JSONObject response = Connectors.httpGET(cmdUrl);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			return Connectors.httpSuccess(response);
		}
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		String fhemId = device.getMetaValueAsString("fhem-id");
		if (Is.nullOrEmpty(fhemId)){
			return null;
		}else{
			String deviceURL = URLBuilder.getString(this.host, 
					"?cmd=", "jsonlist2 " + fhemId,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			JSONObject response = Connectors.httpGET(deviceURL);
			if (Connectors.httpSuccess(response)){
				//build shd from response
				SmartHomeDevice shd = buildDeviceFromResponse(response);
				return shd;
			}else{
				return null;
			}
		}
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		String fhemId = device.getMetaValueAsString("fhem-id");
		String deviceCmdLink = device.getLink(); 
		if (Is.nullOrEmpty(fhemId) || Is.nullOrEmpty(deviceCmdLink)){
			return false;
		}else{
			//check stateType
			if (stateType.equals(SmartHomeDevice.STATE_TYPE_NUMBER_PERCENT)){
				//percent
				state = "pct " + state;
			}
			//TODO: we could check mem-state here if state is e.g. SmartHomeDevice.STATE_ON
			String cmdUrl = URLBuilder.getString(
					deviceCmdLink, "=", "set " + fhemId + " " + state,
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			//System.out.println("URL: " + cmdUrl); 			//DEBUG
			JSONObject response = Connectors.httpGET(cmdUrl);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			return Connectors.httpSuccess(response);
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
		String memoryState = "";
		if (attributes != null){
			//try to find self-defined SEPIA tags first
			name = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_NAME, null);
			type = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_TYPE, null);
			room = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_ROOM, null);
			memoryState = JSON.getStringOrDefault(attributes, SmartHomeDevice.SEPIA_TAG_MEM_STATE, null);
		}
		//smart-guess if missing sepia-specific settings
		if (name == null && internals != null){
			name = JSON.getStringOrDefault(internals, "name", null);		//NOTE: has to be unique!
		}
		if (name == null){
			//we only accept devices with name
			return null;
		}
		if (type == null && internals != null){
			String fhemType = JSON.getString(internals, "type").toLowerCase(); 
			if (fhemType.matches("(.*\\s|^|,)(light.*|lamp.*)")){
				type = SmartDevice.Types.light.name();		//LIGHT
			}else if (fhemType.matches("(.*\\s|^|,)(heat.*|thermo.*)")){
				type = SmartDevice.Types.heater.name();		//HEATER
			}else{
				type = fhemType;		//take this if we don't have a specific type yet
			}
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
		String stateType = null;
		//TODO: clean up state and set stateType according to values like 'dim50%'
		Object linkObj = (fhemObjName != null)? (this.host + "?cmd." + fhemObjName) : null;
		JSONObject meta = JSON.make(
				"fhem-id", fhemObjName
		);
		//note: we need fhem-id for commands although it is basically already in 'link'
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, 
				state, stateType, memoryState, 
				(linkObj != null)? linkObj.toString() : null, meta);
		return shd;
	}
}
