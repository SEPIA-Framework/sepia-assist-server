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

public class OpenHAB implements SmartHomeHub {
	
	private String host;
	public static String NAME = "openhab";
	
	public OpenHAB(String host){
		if (Is.nullOrEmpty(host)){
			throw new RuntimeException("No host address found for openHAB integration!");
		}else{
			this.host = host;
		}
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices() {
		JSONObject response = Connectors.httpGET(this.host + "/rest/items");
		//System.out.println("openHAB REST response: " + response); 									//DEBUG
		if (Connectors.httpSuccess(response)){
			JSONArray devicesArray = null;
			if (response.containsKey("JSONARRAY")){
				devicesArray = JSON.getJArray(response, "JSONARRAY");		//this should usually be the one triggered
			}else if (response.containsKey("STRING")){
				devicesArray = JSON.parseStringToArrayOrFail(response.toJSONString());
			}
			if (devicesArray.isEmpty()){
				//Fail with empty array
				Debugger.println("Service:SmartOpenHAB - devices array was empty!", 1);
				return new HashMap<String, SmartHomeDevice>();
			}
			//Build devices map
			Map<String, SmartHomeDevice> devices = new HashMap<>();
			try{
				for (Object o : devicesArray){
					JSONObject hubDevice = (JSONObject) o;
					
					//Build unified object for SEPIA
					SmartHomeDevice shd = buildDeviceFromResponse(hubDevice);
					
					//devices
					devices.put(shd.getName(), shd);
				}
				return devices;
				
			}catch (Exception e){
				//Fail with faulty array
				Debugger.println("Service:SmartOpenHAB - devices array seems to be broken!", 1);
				return new HashMap<String, SmartHomeDevice>();
			}
			
		}else{
			//Fail with server contact error
			Debugger.println("Service:SmartOpenHAB - failed to get devices from server!", 1);
			return null;
		}
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state) {
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL)){
			return false;
		}else{
			return setDeviceState(deviceURL, state);
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory) {
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL)){
			return false;
		}
		//get fresh data first
		JSONObject response = Connectors.httpGET(deviceURL);
		if (Connectors.httpSuccess(response)){
			//clean up old tags first if needed (how annoying that we have to deal with arrays here)
			String newTag = "sepia-mem-state=" + stateMemory;
			JSONArray allTags = JSON.getJArray(response, "tags");
			List<String> oldMemStateTags = new ArrayList<>();
			for (Object tagObj : allTags){
				String t = (String) tagObj;
				if (t.startsWith("sepia-mem-state=")){
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
						String delURL =  deviceURL + ("/tags/" + URLEncoder.encode(delTag, "UTF-8"));
						if (!Connectors.httpSuccess(Connectors.httpDELETE(delURL))){
							throw new RuntimeException("Connection or response error.");
						}
					}
				} catch (Exception e) {
					Debugger.println("Service:SmartOpenHAB - failed to delete item tag: " + delTag + "Msg: " + e.getMessage(), 1);
					return false;
				}
			}
			//build new tag
			try {
				deviceURL += ("/tags/" + URLEncoder.encode(newTag, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Debugger.println("Service:SmartOpenHAB - failed to set item tag: " + newTag + "Msg: " + e.getMessage(), 1);
				return false;
			}
			//set tag
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "text/plain");
			headers.put("Accept", "application/json");
			String body = ""; 		//request body is empty, value set via URL (strange btw. this could be done via GET)
			JSONObject responseWrite = Connectors.httpPUT(deviceURL, body, headers);
			//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
			return Connectors.httpSuccess(responseWrite);
			
		}else{
			return false;
		}
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device) {
		String deviceURL = device.getLink();
		if (Is.nullOrEmpty(deviceURL)){
			return null;
		}else{
			return loadDeviceData(deviceURL);
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
		for (Object tagObj : tags){
			String t = (String) tagObj;
			if (t.startsWith("sepia-name=")){
				name = t.split("=")[1];						//NOTE: has to be unique
			}else if (t.startsWith("sepia-type=")){
				type = t.split("=")[1];						//NOTE: as defined in device parameter
			}else if (t.startsWith("sepia-room=")){
				room = t.split("=")[1];						//NOTE: as defined in room parameter
			}else if (t.startsWith("sepia-mem-state=")){
				memoryState = t.split("=")[1];				//A state to remember like last non-zero brightness of a light 
			} 
		}
		//smart-guess if missing sepia-specific settings
		if (name == null){
			name = JSON.getString(hubDevice, "name");			//NOTE: has to be unique
		}
		if (type == null){
			String openHabCategory = JSON.getString(hubDevice, "category").toLowerCase();	//NOTE: we check category, not type 
			if (openHabCategory.matches("light.*|lamp.*")){
				type = SmartDevice.Types.light.name();		//LIGHT
			}else if (openHabCategory.matches("heat.*|thermo.*")){
				type = SmartDevice.Types.heater.name();		//HEATER
			}else{
				type = openHabCategory;		//take this if we don't have a specific type yet
			}
		}
		if (room == null){
			room = "";
		}
		//create common object
		Object stateObj = hubDevice.get("state");
		Object linkObj = hubDevice.get("link");
		JSONObject meta = null;
		//TODO: we could add some stuff to meta when we need other data from response.
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, 
				(stateObj != null)? stateObj.toString() : null, memoryState, 
				(linkObj != null)? linkObj.toString() : null, meta);
		return shd;
	}
	
	/**
	 * Load data via device URL.
	 * @param deviceURL
	 * @return
	 */
	public static SmartHomeDevice loadDeviceData(String deviceURL) {
		JSONObject response = Connectors.httpGET(deviceURL);
		if (Connectors.httpSuccess(response)){
			//add tags to meta
			SmartHomeDevice shd = buildDeviceFromResponse(response);
			return shd;
		}else{
			return null;
		}
	}
	
	/**
	 * Push new status to device given by direct access URL.
	 * @param deviceURL - link given in getDevices()
	 * @param state - new status value
	 * @return true IF no error was thrown after request
	 */
	public static boolean setDeviceState(String deviceURL, String state){
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "text/plain");
		headers.put("Accept", "application/json");
		JSONObject response = Connectors.httpPOST(deviceURL, state, headers);
		//System.out.println("RESPONSE: " + response); 		//this is usually empty if there was no error
		return Connectors.httpSuccess(response);
	}

}
