package net.b07z.sepia.server.assist.smarthome;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.SmartDevice;
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
	
	private String host;
	private String authType;
	private String authData;
	public static String NAME = "openhab";
	
	/**
	 * Build OpenHAB connector with given host address.
	 * @param host - e.g. http://localhost:8080
	 */
	public OpenHAB(String host){
		if (Is.nullOrEmpty(host)){
			throw new RuntimeException("No host address found for openHAB integration!");
		}else{
			this.host = host;
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
	private JSONObject httpPOST(String url, String queryJson, Map<String, String> headers){
		if (Is.notNullOrEmpty(this.authData)){
			headers = addAuthHeader(headers);
		}
		return Connectors.httpPOST(url, queryJson, headers);
	}
	private JSONObject httpPUT(String url, String queryJson, Map<String, String> headers){
		if (Is.notNullOrEmpty(this.authData)){
			headers = addAuthHeader(headers);
		}
		return Connectors.httpPUT(url, queryJson, headers);
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
		//Currently no action required - just return true
		return true;
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices(String optionalNameFilter, String optionalTypeFilter, String optionalRoomFilter){
		//TODO: we currently ignore result filtering
		JSONObject response = httpGET(this.host + "/rest/items");
		//System.out.println("openHAB REST response: " + response); 									//DEBUG
		if (Connectors.httpSuccess(response)){
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
						devices.put(shd.getName(), shd);
					}
				}
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
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL)){
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
					Debugger.println("Service:OpenHAB - failed to delete item tag: " + delTag + "Msg: " + e.getMessage(), 1);
					return false;
				}
			}
			//build new tag
			try {
				deviceURL += ("/tags/" + URLEncoder.encode(newTag, "UTF-8").replace("+", "%20"));
			} catch (UnsupportedEncodingException e) {
				Debugger.println("Service:OpenHAB - failed to set item tag: " + newTag + "Msg: " + e.getMessage(), 1);
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
				Debugger.println("OpenHAB interface error in 'writeDeviceAttribute' - Device: " + device.getName() + " - Response: " + responseWrite, 1);
			}
			return success;
			
		}else{
			return false;
		}
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType) {
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL)){
			return false;
		}else{
			//System.out.println("state: " + state); 				//DEBUG
			//System.out.println("stateType: " + stateType); 		//DEBUG
			String givenType = device.getType();

			//ROLLER SHUTTER
			if (givenType != null && Is.typeEqual(givenType, SmartDevice.Types.roller_shutter)){
				//check stateType
				if (stateType != null){
					if (state.equalsIgnoreCase(SmartHomeDevice.STATE_OPEN)){
						state = "UP";
					}else if (state.equalsIgnoreCase(SmartHomeDevice.STATE_CLOSED)){
						state = "DOWN";
					}
				}
			//ELSE
			}else{
				//check stateType
				if (stateType != null){
					if (stateType.equals(SmartHomeDevice.STATE_TYPE_TEXT_BINARY)){
						//all upper case text for openHAB
						state = state.toUpperCase();
					}
				}
			}
			//TODO: we could check mem-state here if state is e.g. SmartHomeDevice.STATE_ON
			//TODO: improve stateType check (temp. etc)
			
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "text/plain");
			headers.put("Accept", "application/json");
			JSONObject response = httpPOST(deviceURL, state, headers);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			boolean success = Connectors.httpSuccess(response); 
			if (!success){
				Debugger.println("OpenHAB interface error in 'setDeviceState' - Sent '" + state + "' got response: " + response, 1);
			}
			return success;
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory) {
		return writeDeviceAttribute(device, SmartHomeDevice.SEPIA_TAG_MEM_STATE, stateMemory);
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device) {
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL)){
			return null;
		}else{
			JSONObject response = httpGET(deviceURL);
			if (Connectors.httpSuccess(response)){
				//build device from result
				SmartHomeDevice shd = buildDeviceFromResponse(response);
				return shd;
			}else{
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
		JSONArray tags = JSON.getJArray(hubDevice, "tags");
		String name = null;
		String type = null;
		String room = null;
		String memoryState = "";
		if (tags != null){
			//try to find self-defined SEPIA tags first
			for (Object tagObj : tags){
				String t = (String) tagObj;
				if (t.startsWith(SmartHomeDevice.SEPIA_TAG_NAME + "=")){
					name = t.split("=", 2)[1];						//NOTE: has to be unique
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_TYPE + "=")){
					type = t.split("=", 2)[1];						//NOTE: as defined in device parameter
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_ROOM + "=")){
					room = t.split("=", 2)[1];						//NOTE: as defined in room parameter
				}else if (t.startsWith(SmartHomeDevice.SEPIA_TAG_MEM_STATE + "=")){
					memoryState = t.split("=", 2)[1];				//A state to remember like last non-zero brightness of a light 
				} 
			}
		}
		//smart-guess if missing sepia-specific settings
		String originalName = JSON.getStringOrDefault(hubDevice, "name", null);
		if (name == null && originalName != null){
			name = originalName;			//NOTE: has to be unique
		}
		if (name == null){
			//we only accept devices with name
			return null;
		}
		if (type == null){
			String openHabCategory = JSON.getString(hubDevice, "category").toLowerCase();	//NOTE: we prefer category, not type
			String openHabType = JSON.getString(hubDevice, "type").toLowerCase();
			//TODO: category might not be defined
			//TODO: 'type' can give possible set options
			if (Is.notNullOrEmpty(openHabCategory)){
				if (openHabCategory.matches("(.*\\s|^|,)(light.*|lamp.*)")){
					type = SmartDevice.Types.light.name();		//LIGHT
				}else if (openHabCategory.matches("(.*\\s|^|,)(heat.*|thermo.*)")){
					type = SmartDevice.Types.heater.name();		//HEATER
				}else{
					type = openHabCategory;		//take this if we don't have a specific type yet
				}
			}else if (Is.notNullOrEmpty(openHabType)){
				if (openHabType.equals("rollershutter")){
					type = SmartDevice.Types.roller_shutter.name();		//ROLLER SHUTTER
				}
			}
		}
		if (room == null){
			room = "";
		}
		//create common object
		Object stateObj = hubDevice.get("state");
		String state = null;
		if (stateObj != null){
			state = stateObj.toString();
		}
		String stateType = null;
		if (state != null){
			stateType = SmartHomeDevice.convertStateType(null, state, null);
			if (stateType != null){
				state = SmartHomeDevice.convertState(state, stateType);		//TODO: this might require deviceType (see comment inside method)
			}
		}
		//TODO: for temperature we need to check more info (temp. unit? percent? etc...)
		//TODO: clean up stateObj properly and check special format?
		Object linkObj = hubDevice.get("link");
		JSONObject meta = JSON.make(
				"id", originalName,
				"origin", NAME
		);
		//TODO: we could add some stuff to meta when we need other data from response.
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, 
				state, stateType, memoryState, 
				(linkObj != null)? linkObj.toString() : null, meta);
		return shd;
	}
}
