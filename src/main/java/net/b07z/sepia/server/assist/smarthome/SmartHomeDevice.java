package net.b07z.sepia.server.assist.smarthome;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.parameters.Room;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.tools.Calculator;
import net.b07z.sepia.server.assist.parameters.Number;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class that represents a smart home device with e.g.: name, type, room, state, etc..<br>
 * Includes several helper functions as well to search and filter devices or convert states etc..
 * 
 * @author Florian Quirin
 *
 */
public class SmartHomeDevice {
	
	private String name;
	private String type; 		//see: net.b07z.sepia.server.assist.parameters.SmartDevice.Types
	private String room;		//see: net.b07z.sepia.server.assist.parameters.Room.Types
	private String roomIndex;	//e.g. 1, 212, etc. ... maybe "1st floor" ... thats why its a string
	private String state;		//e.g.: ON, OFF, 1-100, etc.
	private String stateType;	//e.g.: STATE_TYPE_NUMBER_PERCENT
	private String stateMemory;		//state storage for e.g. default values after restart etc.
	private String link;		//e.g. HTTP direct URL to device
	private String interfaceId;	//e.g. openhab, fhem, ... - NOTE: this is usually only set for internal HUB
	//meta data required to operate the device properly:
	private JSONObject meta;	//e.g. see below META_...
	
	//meta variables
	public static final String META_SET_CMDS = "setCmds";
	public static final String META_INTERFACE_DEVICE = "interfaceDeviceId"; //the proxy ID in remote HUB
	public static final String META_INTERFACE_CONFIG = "interfaceConfig";	//custom config for remote HUB
	public static final String META_ID = "id";		//the actual ID inside main HUB (internal or remote)
	public static final String META_ORIGIN = "origin";
	public static final String META_TYPE_GUESSED = "typeGuessed";
	public static final String META_NAMED_BY_SEPIA = "namedBySepia";
	
	//filter options
	public static final String FILTER_NAME = "name";
	public static final String FILTER_TYPE = "type";
	public static final String FILTER_TYPE_ARRAY = "typeArray";
	public static final String FILTER_ROOM = "room";
	public static final String FILTER_ROOM_INDEX = "roomIndex";
	
	//global tags used to store SEPIA specific device data in other HUB systems
	public static final String SEPIA_TAG_NAME = "sepia-name";
	public static final String SEPIA_TAG_TYPE = "sepia-type";
	public static final String SEPIA_TAG_ROOM = "sepia-room";
	public static final String SEPIA_TAG_ROOM_INDEX = "sepia-room-index";
	public static final String SEPIA_TAG_DATA = "sepia-data";
	public static final String SEPIA_TAG_MEM_STATE = "sepia-mem-state";
	public static final String SEPIA_TAG_STATE_TYPE = "sepia-state-type";
	//stored in meta:
	public static final String SEPIA_TAG_SET_CMDS = "sepia-set-cmds";
	public static final String SEPIA_TAG_INTERFACE_DEVICE = "sepia-interface-device";
	public static final String SEPIA_TAG_INTERFACE_CONFIG = "sepia-interface-config";
	
	public static final String SEPIA_TAG_INTERFACE = "sepia-interface";
	public static final String SEPIA_TAG_LINK = "sepia-link";
	
	//generalized device states
	public static enum State {
		on,
		off,
		open,
		closed,
		//up,
		//down,
		connected,
		disconnected
	}
	public static final String REGEX_STATE_ENABLE = "(?i)on|open|connected|up|enabled";
	public static final String REGEX_STATE_DISABLE = "(?i)off|closed|disconnected|down|disabled";
	
	//device state types
	public static enum StateType {
		text_binary,			//ON, OFF, OPEN, CLOSED, ...
		text_raw,				//just raw text as given by device/request
		number_plain,
		number_percent,
		number_temperature,
		number_temperature_c,
		number_temperature_f,
		custom
	}
	public static final String REGEX_STATE_TYPE_TEXT = "^text_.*";
	public static final String REGEX_STATE_TYPE_NUMBER = "^number_.*";
	
	//locals
	private static HashMap<String, String> states_de = new HashMap<>();
	private static HashMap<String, String> states_en = new HashMap<>();
	static {
		states_de.put("on", "an");
		states_de.put("off", "aus");
		states_de.put("open", "offen");
		states_de.put("close", "geschlossen");
		states_de.put("closed", "geschlossen");
		states_de.put("unreachable", "nicht erreichbar");
		states_de.put("home", "zu Hause");
		states_de.put("gone", "weg");
		
		states_en.put("on", "on");
		states_en.put("off", "off");
		states_en.put("open", "open");
		states_en.put("close", "closed");
		states_en.put("closed", "closed");
		states_en.put("unreachable", "unreachable");
		states_en.put("home", "home");
		states_en.put("gone", "gone");
	}
	/**
	 * Translate state value.
	 * If state is unknown returns original string.
	 * @param state - generalized state 
	 * @param language - ISO language code
	 * @param stateType - type of state as given by {@link StateType}, usually defined by device, e.g. 'number_plain'
	 */
	public static String getStateLocal(String state, String language, String stateType){
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = states_de.get(state.toLowerCase());
		}else if (language.equals(LANGUAGES.EN)){
			localName = states_en.get(state.toLowerCase());
		}
		if (localName == null){
			if (stateType != null){
				switch (StateType.valueOf(stateType)) {
				case text_raw:
				case number_plain:
					return state;
				case number_percent:
					return state + "%";
				case number_temperature_c:
					return state + " °C";
				case number_temperature_f:
					return state + " °F";
				default:
					break;
				}
			}
			boolean skipWarning = state.matches("\\d+.*");
			if (!skipWarning){
				Debugger.println(SmartHomeDevice.class.getSimpleName() + 
					" - getStateLocal() has no '" + language + "' version for '" + state + "'", 3);
			}
			return state;
		}else{
			return localName;
		}
	}
	/**
	 * Translate state value. If state is unknown returns original string.<br>
	 * Calls {@link #getStateLocal(String, String, String)} with type = null;
	 */
	public static String getStateLocal(String state, String language){
		return getStateLocal(state, language, null);
	}
	
	//------- main -------
	
	/**
	 * Create new generalized SEPIA smart home device object filled with data obtained by calling specific HUBs etc. 
	 * @param name - device name
	 * @param type - type as seen e.g. in {@link SmartDevice.Types}
	 * @param room - room as seen e.g. in {@link Room.Types}
	 * @param state - e.g. ON, OFF, 1-100, etc.
	 * @param stateType - e.g. STATE_TYPE_NUMBER_PERCENT
	 * @param stateMemory - last known active state e.g. for toggle events to restore previously set brightness etc. 
	 * @param link - direct link to device generated by the specific smart home HUB integration
	 * @param meta - any data generated by the specific smart home HUB integration that might be needed in other HUB methods
	 */
	public SmartHomeDevice(String name, String type, String room, String state, String stateType, String stateMemory, String link, JSONObject meta){
		this.name = name;
		this.type = type;
		this.room = room;
		this.state = state;
		this.stateType = stateType;
		this.stateMemory = stateMemory;
		this.link = link;
		this.meta = meta;
	}
	/**
	 * Create new generalized SEPIA smart home device object to be filled with data obtained by calling specific HUBs etc.
	 */
	public SmartHomeDevice(){}
	
	@Override
	public String toString(){
		return ("name: " + this.name + " - type: " + this.type+ " - room: " + this.room);
	}
	
	/**
	 * Get the device interface ID used to connect internal HUB device with a 
	 * HUB configuration (openHAB, FHEM, ...) previously stored in the DB.
	 * @return
	 */
	public String getInterface() {
		return interfaceId;
	}
	/**
	 * Set the device interface ID used to connect internal HUB device with a 
	 * HUB configuration (openHAB, FHEM, ...) previously stored in the DB.
	 */
	public void setInterface(String interfaceId) {
		this.interfaceId = interfaceId;
	}
	/**
	 * Check if this device object has an interface ID and thus is based on the internal SEPIA HUB data config data.
	 */
	public boolean hasInterface(){
		return Is.notNullOrEmpty(this.interfaceId);
	}
	
	/**
	 * Device name
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * Set class variable 'name' (no write to HUB!)
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Device type
	 * @return
	 */
	public String getType() {
		return type;
	}
	/**
	 * Set class variable 'type' (no write to HUB!)
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Device room
	 * @return
	 */
	public String getRoom() {
		return room;
	}
	/**
	 * Set class variable 'room' (no write to HUB!)
	 */
	public void setRoom(String room) {
		this.room = room;
	}
	/**
	 * Device room index
	 * @return
	 */
	public String getRoomIndex() {
		return roomIndex;
	}
	/**
	 * Set class variable 'roomIndex' (no write to HUB!)
	 */
	public void setRoomIndex(String roomIndex) {
		this.roomIndex = roomIndex;
	}
	
	/**
	 * Device state
	 * @return
	 */
	public String getState() {
		return state;
	}
	/**
	 * Set class variable 'state' (no write to HUB!)
	 */
	public void setState(String state) {
		this.state = state;
	}
	/**
	 * Type of device state
	 * @return
	 */
	public String getStateType() {
		return stateType;
	}
	/**
	 * Set state type (no write to HUB!)
	 */
	public void setStateType(String stateType) {
		this.stateType = stateType;
	}
	/**
	 * Write state to HUB.
	 * @param hub - HUB to write to
	 * @param newState - state to write
	 * @param stateType - type of state variable, e.g. {@link SmartHomeDevice#STATE_TYPE_NUMBER_PERCENT} = number in percent
	 * @return success/error
	 */
	public boolean writeState(SmartHomeHub hub, String newState, String stateType){
		return hub.setDeviceState(this, newState, stateType);
	}
	
	/**
	 * Device state memory
	 * @return
	 */
	public String getStateMemory() {
		return stateMemory;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setStateMemory(String stateMemory) {
		this.stateMemory = stateMemory;
	}
	/**
	 * Write state memory to HUB.
	 * @param hub - HUB to write to
	 * @param newStateMem - state to write
	 * @return success/error
	 */
	public boolean writeStateMemory(SmartHomeHub hub, String newStateMem){
		return hub.setDeviceStateMemory(this, newStateMem);
	}
	
	/**
	 * Device direct link
	 * @return
	 */
	public String getLink() {
		return link;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setLink(String link) {
		this.link = link;
	}
	
	/**
	 * Device custom meta data
	 * @return
	 */
	public JSONObject getMeta() {
		return meta;
	}
	/**
	 * Get certain value of meta data as string.
	 * @param key
	 * @return value or null
	 */
	public String getMetaValueAsString(String key){
		if (meta != null){
			return JSON.getString(meta, key);
		}else{
			return null;
		}
	}
	/**
	 * Set whole meta object (no write to HUB!)
	 */
	public void setMeta(JSONObject meta){
		this.meta = meta;
	}
	/**
	 * Set field of meta data (no write to HUB!)
	 */
	public void setMetaValue(String key, Object value){
		if (meta == null){
			meta = new JSONObject();
		}
		JSON.put(meta, key, value);
	}
	/**
	 * Delete field from meta data (no write to HUB!)
	 */
	public void removeMetaField(String key){
		if (meta != null){
			meta.remove(key);
		}
	}
	
	/**
	 * Get the ID usually used to identify the device in the internal database or external HUB.
	 * The method will check for 'interface' first and return 'interfaceDeviceId' if available (or null).
	 * If no interface is defined it will return the "normal" ID (meta.id).<br>
	 * NOTE: 'hasInterface' will only be true for internal HUB but 'interfaceDeviceId' can still be set.
	 * In this case (e.g. for proxy devices) you need to decide yourself what ID you need.
	 * @return
	 */
	public String getId(){
		if (this.hasInterface()){
			return getInterfaceDeviceId();
		}else{
			return getMetaValueAsString(META_ID);
		}
	}
	
	/**
	 * Get custom commands object (aka 'setCmds').
	 * @return
	 */
	public JSONObject getCustomCommands(){
		if (meta == null){
			return null;
		}else{
			return JSON.getJObject(meta, META_SET_CMDS);
		}
	}
	/**
	 * Set custom commands object (aka 'setCmds'; no write to HUB!)
	 */
	public void setCustomCommands(JSONObject setCmds){
		setMetaValue(META_SET_CMDS, setCmds);
	}
	
	/**
	 * Get 'interfaceDeviceId' from meta, e.g. to use it in internal HUB.<br>
	 * NOTE: this can be set for e.g. proxy devices even if its NOT the internal HUB.
	 * @return ID string or null
	 */
	public String getInterfaceDeviceId(){
		return getMetaValueAsString(META_INTERFACE_DEVICE);
	}
	/**
	 * Set 'interfaceDeviceId'.
	 */
	public void setInterfaceDeviceId(String interfaceDeviceId){
		setMetaValue(META_INTERFACE_DEVICE, interfaceDeviceId);
	}
	
	/**
	 * Get interface configuration data for external HUB (aka 'interfaceConfig').
	 * @return
	 */
	public JSONObject getInterfaceConfig(){
		if (meta == null){
			return null;
		}else{
			return JSON.getJObject(meta, META_INTERFACE_CONFIG);
		}
	}
	/**
	 * Set interface configuration data for external HUB (aka 'interfaceConfig'; no write to HUB!)
	 */
	public void setInterfaceConfig(JSONObject interfaceConfig){
		setMetaValue(META_INTERFACE_CONFIG, interfaceConfig);
	}
	
	/**
	 * Export device to JSON.
	 * @return
	 */
	public JSONObject getDeviceAsJson(){
		//create common object
		JSONObject newDeviceObject = JSON.make(
				"interface", interfaceId,
				"name", name, 
				"type", type, 
				"room", room, 
				"state", state,
				"link", link
		);
		JSON.put(newDeviceObject, "roomIndex", roomIndex);
		JSON.put(newDeviceObject, "stateType", stateType);
		JSON.put(newDeviceObject, "stateMemory", stateMemory);		//NOTE: this CAN BE smart HUB specific (not identical to generalized state value)
		JSON.put(newDeviceObject, "meta", meta);
		return newDeviceObject;
	}
	/**
	 * Import device from JSON object.
	 * @param deviceJson
	 */
	public SmartHomeDevice importJsonDevice(JSONObject deviceJson){
		this.interfaceId = JSON.getString(deviceJson, "interface");
		this.name = JSON.getString(deviceJson, "name");
		this.type = JSON.getString(deviceJson, "type");
		this.room = JSON.getString(deviceJson, "room");
		this.roomIndex = JSON.getString(deviceJson, "roomIndex");
		this.state = JSON.getString(deviceJson, "state");
		this.stateType = JSON.getString(deviceJson, "stateType");
		this.stateMemory = JSON.getString(deviceJson, "stateMemory");
		this.link = JSON.getString(deviceJson, "link");
		this.meta = JSON.getJObject(deviceJson, "meta");
		//NOTE: meta includes e.g. "setCmds", "interfaceConfig", "interfaceDeviceId" ...
		return this;
	}
	
	//--------- static helper methods ----------
	
	/**
	 * Clean up a device name, e.g. remove all (..) brackets and trim result.
	 * @param rawName - name of device as defined by user
	 * @return
	 */
	public static String getCleanedUpName(String rawName){
		return rawName.replaceAll("\\(.*?\\)", " ").replaceAll("\\s+", " ").trim();
	}
	/**
	 * Get clean name with no brackets and no number.
	 * @param rawName - name of device as defined by user
	 * @return
	 */
	public static String getBaseName(String rawName){
		return rawName.replaceAll("\\(.*?\\)", " ").replaceAll("\\d+", " ").replaceAll("\\s+", " ").trim();
	}
	
	/**
	 * Get devices from the list that match type and room (optionally).
	 * @param devices - map of devices taken e.g. from getDevices()
	 * @param deviceTypeList - list of device types (or null), see {@link SmartDevice.Types}
	 * @param roomType - type of room (or null), see {@link Room.Types}
	 * @param roomIndex - e.g. a number (as string) or null
	 * @param maxDevices - maximum number of matches (0 or negative for all possible)
	 * @return list of devices (can be empty)
	 */
	public static List<SmartHomeDevice> getMatchingDevices(Map<String, SmartHomeDevice> devices, 
				List<String> deviceTypeList, String roomType, String roomIndex, int maxDevices){
		List<SmartHomeDevice> matchingDevices = new ArrayList<>();
		//get all devices with right type and optionally room
		int found = 0;
		for (Map.Entry<String, SmartHomeDevice> entry : devices.entrySet()){
			//check type
			SmartHomeDevice data = entry.getValue();
			String thisType = data.getType();
			if (Is.notNullOrEmpty(deviceTypeList)){
				if (thisType == null || !deviceTypeList.contains(thisType)){
					continue;
				}
			}
			//check room?
			if (Is.notNullOrEmpty(roomType)){
				String thisRoom = data.getRoom();
				if (thisRoom == null || !thisRoom.equals(roomType)){
					continue;
				}else{
					//check room index
					String thisRoomIndex = data.getRoomIndex();
					if (Is.notNullOrEmpty(roomIndex)){
						if (thisRoomIndex == null && roomIndex.equals("1")){	
							//if no room index is defined and user looks for room 1 this is still ok
							matchingDevices.add(data);
							found++;
						}else if (thisRoomIndex == null || !thisRoomIndex.equals(roomIndex)){
							continue;
						}else{
							matchingDevices.add(data);
							found++;
						}
					}else{
						if (thisRoomIndex == null || thisRoomIndex.equals("1")){
							//if room index is not in search this must be null or 1 (1 is OK because it basically is the default room)
							matchingDevices.add(data);
							found++;
						}else{
							continue;
						}
					}
				}
			}else{
				matchingDevices.add(data);
				found++;
			}
			//max results reached?
			if (maxDevices > 0 && found >= maxDevices){
				break;
			}
			//TODO: we should do a device name check too, but this is not taken into account in SmartDevice parameter yet :-( 
			//e.g. "Light 1", "Lamp A" or "Desk-Lamp" ...
			//I suggest to create an additional parameter called SMART_DEVICE_NAME
		}
		return matchingDevices;
	}
	/**
	 * Get devices from the list that match a set of filters.
	 * @param devices - map of devices taken e.g. from getDevices()
	 * @param filters - map of filters like "type" or "typeArray" (list or comma separated string), "room", "roomIndex", "limit" etc.
	 * @return list of devices (can be empty)
	 */
	public static List<SmartHomeDevice> getMatchingDevices(Map<String, SmartHomeDevice> devices,
			Map<String, Object> filters){
		//filters
		List<String> deviceTypeList = null;
		String roomType = null;
		String roomIndex = null;
		int limit = -1;
		if (filters != null){
			Object typeArrayOrStringObj = filters.getOrDefault("typeArray", filters.get("type"));
			if (typeArrayOrStringObj != null){
				List<String> typeArray = Converters.stringOrCollection2ListStr(typeArrayOrStringObj);
				if (Is.notNullOrEmpty(typeArray)){
					deviceTypeList = typeArray;
				}
			}
			roomType = Converters.obj2StringOrDefault(filters.get("room"), null);
			roomIndex = Converters.obj2StringOrDefault(filters.get("roomIndex"), null);
			Object limitObj = filters.get("limit");
			limit = -1;
			if (limitObj != null){
				limit = (int) limitObj;
			}
		}
		//get all devices with right type and optionally right room
		List<SmartHomeDevice> matchingDevices = getMatchingDevices(devices, deviceTypeList, roomType, roomIndex, limit);
		return matchingDevices;
	}
	
	/**
	 * Search for a given number in device name and return first match. If match is not found return given index or null.
	 * @param devicesList - list of devices, usually already filtered by device type and room
	 * @param number - a number to look for in device name (e.g. 1 for "Light 1 in living-room") 
	 * @param defaultIndex - list index to return when no match was found, typically 0 (first device as fallback) or -1 (return null, no fallback)
	 * @return match, fallback or null
	 */
	public static SmartHomeDevice findFirstDeviceWithNumberInNameOrDefault(List<SmartHomeDevice> devicesList, int number, int defaultIndex){
		String indexRegExp = ".*(^|\\b|\\D)" + number + "(\\b|\\D|$).*";
		for (SmartHomeDevice shd : devicesList){
			//System.out.println(shd.getName() + " - " + shd.getName().matches(indexRegExp));		//DEBUG
			if (shd.getName().matches(indexRegExp)){
				return shd;
			}
		}
		if (defaultIndex == -1 || defaultIndex > (devicesList.size() - 1)){
			return null;
		}else{
			return devicesList.get(defaultIndex);
		}
	}
	/**
	 * Search for a given number in device name and return all matches. If number is 1 return all matches AND all devices without any number.
	 * @param devicesList - list of devices, usually already filtered by device type and room
	 * @param number - a number to look for in device name (e.g. 1 for "Light 1 in living-room")
	 * @return list of matches or empty list
	 */
	public static List<SmartHomeDevice> findDevicesWithNumberInName(List<SmartHomeDevice> devicesList, int number){
		List<SmartHomeDevice> matchingDevices = new ArrayList<>();
		String indexRegExp = ".*(^|\\b|\\D)" + number + "(\\b|\\D|$).*";
		if (number == 1){
			//if number is 1 search all devices with 1 or without number
			for (SmartHomeDevice shd : devicesList){
				String name = shd.getName();
				if (!name.matches(".*\\d.*") || name.matches(indexRegExp)){
					matchingDevices.add(shd);
				}
			}
		}else{
			//get all with this number
			for (SmartHomeDevice shd : devicesList){
				if (shd.getName().matches(indexRegExp)){
					matchingDevices.add(shd);
				}
			}
		}
		return matchingDevices;
	}
	
	/**
	 * Filter list of devices by tag/name ignoring case. NO fuzzy matching!
	 * @param devicesList - list of devices, usually already filtered by device type and room
	 * @param tag - usually the name of a device
	 * @return list with matches or empty list
	 */
	public static List<SmartHomeDevice> findDevicesWithMatchingTagIgnoreCase(List<SmartHomeDevice> devicesList, String tag){
		List<SmartHomeDevice> matches = new ArrayList<>();
		for (SmartHomeDevice shd : devicesList){
			//System.out.println(shd.getName() + " - " + shd.getName().equals(tag));		//DEBUG
			if (shd.getName().equalsIgnoreCase(tag)){
				matches.add(shd);
			}
		}
		return matches;
	}
	
	/**
	 * Find correct, generalized stateType from given state.
	 * @param state - state as given by parameter
	 * @return found state type or null
	 */
	public static String findStateType(String state){
		String genStateType = null;
		//plain
		if (state.matches("[\\d.,]+")){
			genStateType = StateType.number_plain.name();
		//percent
		}else if (state.matches(".*[\\d.,]+(\\s+|)(%|pct)(\\s|\\b|$).*")){
			genStateType = StateType.number_percent.name();
		//temperature
		}else if (state.matches(".*[\\d.,]+(\\s+|)(°|deg|)C\\b.*")){
			genStateType = StateType.number_temperature_c.name();
		}else if (state.matches(".*[\\d.,]+(\\s+|)(°|deg|)F\\b.*")){
			genStateType = StateType.number_temperature_f.name();
		}else if (state.matches(".*[\\d.,]+(\\s+|)(°|deg)(\\s|\\b|$).*")){
			genStateType = StateType.number_temperature.name();
		//ON/OFF
		}else if (state.matches("(?i)(on|off|open|close(d|)|up|down|(dis|)connected|(in|)active)")){
			genStateType = StateType.text_binary.name();
		}
		return genStateType;
	}
	/**
	 * When state type is known try to convert state value itself to generalized SEPIA value.
	 * @param state - found state (as seen by HUB)
	 * @param stateType - predefined or interpreted state type, e.g. StateType.number_precent
	 * @return converted state or original if no conversion possible
	 */
	public static String convertAnyStateToGeneralizedState(String state, String stateType){
		//all numbers
		if (stateType.matches(REGEX_STATE_TYPE_NUMBER) && state.matches(".*\\d.*")){
			//return first number including "," and "." and replace ","
			return state.replaceAll(".*?([\\d.,]+).*", "$1").replaceAll(",", ".").trim();
		//certain texts
		}else if (stateType.equals(StateType.text_binary.name())){
			if (state.equalsIgnoreCase("down")){		//NOTE: we assume a fixed state "down" is closed - TODO: we might need deviceType here
				return State.closed.name();
			}else if (state.equalsIgnoreCase("up")){	//NOTE: we assume a fixed state "up" is open
				return State.open.name();
			}else{
				return state;
			}
		//other
		}else{
			return state;
		}
		//TODO: add more? Use deviceType?
	}
	/**
	 * Check if state is a non-zero number like "100", "50%", "0.1", "13 °C" etc.,
	 * but not "0%", "0.0", "0 °C" etc..
	 * @param state - any state as string
	 * @return
	 */
	public static boolean isStateNonZeroNumber(String state){
		return state.matches(".*\\d.*") && !state.replaceAll("\\D", "").trim().matches("[0]+");
	}
	
	/**
	 * If state type is a plain number make smart assumption about the intended type using device type info.
	 * E.g.: In "set lights to 50" the 50 is most likely intended to be "50 percent".
	 * If no assumption can be made just return StateType.number_plain again. 
	 * @param deviceType - {@link SmartDevice.Types}
	 * @return
	 */
	public static String makeSmartTypeAssumptionForPlainNumber(SmartDevice.Types deviceType){
		if (deviceType.equals(SmartDevice.Types.light) 
				|| deviceType.equals(SmartDevice.Types.roller_shutter)
				|| deviceType.equals(SmartDevice.Types.garage_door)){
			return StateType.number_percent.name();
		}else if (deviceType.equals(SmartDevice.Types.heater)
				|| deviceType.equals(SmartDevice.Types.air_conditioner)
				|| deviceType.equals(SmartDevice.Types.temperature_control)){
			return StateType.number_temperature.name();
		}else if (deviceType.equals(SmartDevice.Types.sensor)){
			return StateType.text_raw.name();
		}else{
			return StateType.number_plain.name();
		}
	}
	
	/**
	 * Compare the input state type (e.g. what the user said) and device state type (e.g. what is configured for the device)
	 * and adapt the input state and state type if necessary/possible using device type, state and NLU-input (e.g. user preferences).<br>
	 * An example: "set lights to 50" is of input state type number_plain, but the device will probably expect number_precent. In this
	 * case a trivial adaptation can be made. If the user says "set lights to 20 degrees celsius" adaptation is not possible and the method
	 * will throw an exception.
	 * @param inputState - state to set, typically a number (as string)
	 * @param inputStateType - state type given as input, e.g. {@link StateType#number_plain} (string)
	 * @param deviceType - type of smart device that might be used to make smart assumptions, e.g. {@link SmartDevice.Types#light}
	 * @param deviceStateType - expected state type of device, e.g. {@link StateType#number_temperature_c} (string)
	 * @param nluInput - {@link NluInput} that can be used to add user/client specific preferences like a temperature unit. Ignored if null.
	 * @return {@link SimpleEntry} with new stateType as key and new state as value.
	 * @throws Exception thrown when adaptation not possible
	 */
	public static SimpleEntry<String, String> adaptToDeviceStateTypeOrFail(String inputState, String inputStateType, 
			String deviceType, String deviceStateType, NluInput nluInput) throws Exception {
		String newInputState = inputState;
		String newInputStateType = inputStateType;
		//identical?
		if (deviceStateType.equals(newInputStateType)){
			//no adaptation required
			return new SimpleEntry<>(newInputStateType, newInputState);
		}
		//make some assumptions for plain input
		if (Is.typeEqual(newInputStateType, StateType.number_plain)){
			//we take selectedDevice stateType since this is definitely defined at this point
			newInputStateType = deviceStateType;
			//TODO: need to properly convert state to newInputState? What if 'deviceStateType' is binary ON/OFF?
		}
		//devices with state value type temp. only accept plain number or temp. number
		if (deviceStateType.startsWith(StateType.number_temperature.name())){
			//NOTE: this has to be either C or F (unknown unit not allowed as device state type)
			if (newInputStateType.startsWith(StateType.number_temperature.name())){
				//check if temp. unit is given. If not try to get it from client
				if (Is.typeEqual(newInputStateType, StateType.number_temperature)){
					//Try to use default user temperature unit - NOTE: is available in the user account too (if client is not submitting it)
					String userPrefTempUnit = (nluInput != null)? (String) nluInput.getCustomDataObject(NluInput.DATA_PREFERRED_TEMPERATURE_UNIT) : null;
					if (Is.notNullOrEmpty(userPrefTempUnit)){
						if (userPrefTempUnit.equals("C")){
							newInputStateType = StateType.number_temperature_c.name();
						}else if (userPrefTempUnit.equals("F")){
							newInputStateType = StateType.number_temperature_f.name();
						}
					//Use device type (note: might still have no C or F unit but user can set it in HUB)
					}else{
						newInputStateType = deviceStateType;
						java.lang.Number newTemp = Converters.stringToNumber(newInputState);
						newInputState = Converters.numberToString(newTemp, "#.#");
					}
				}
				//check if temp. unit is equal now - if not convert it
				if (!deviceStateType.equals(newInputStateType)){
					String deviceUnit = (deviceStateType.matches(".*_(c|f)$"))? 
							deviceStateType.substring(deviceStateType.length() - 1).toUpperCase() : "";
					String inputUnit = (newInputStateType.matches(".*_(c|f)$"))?
							newInputStateType.substring(newInputStateType.length() - 1).toUpperCase() : "";
					double newTemp = Number.convertTemperature(newInputState, inputUnit, null, deviceUnit);
					newInputState = Converters.numberToString(newTemp, "#.#");
				}
			}else{
				throw new RuntimeException("SmartHomeDevice.adaptToDeviceStateTypeOrFail: could not adapt to device state type!");
			}
		}
		return new SimpleEntry<>(newInputStateType, newInputState);
	}
	
	/**
	 * Get set-command for a state from the custom 'setCmds' object, e.g. defined in control HUB for specific device.
	 * @param state - state as given by NLU, e.g. on, off, 50%
	 * @param stateType - type of state to set, e.g. text (on,off) or number (50%), as seen in {@link StateType}
	 * @param setCmds - JSONObject usually taken from device meta data
	 * @return SimpleEntry with (key, command) or null
	 */
	public static SimpleEntry<String, String> getKeyAndStateFromCustomSetCommands(String state, String stateType, JSONObject setCmds){
		if (stateType != null){
			if (stateType.matches(REGEX_STATE_TYPE_NUMBER)){
				String cmd = Converters.obj2StringOrDefault(setCmds.get("number"), null);
				if (Is.notNullOrEmpty(cmd)){
					return new SimpleEntry<>("number", cmd.replaceAll("<val>|<value>", state));
				}
			}else if (stateType.matches(REGEX_STATE_TYPE_TEXT)){
				if (state.matches(REGEX_STATE_ENABLE)){
					String cmd = Converters.obj2StringOrDefault(setCmds.get("enable"), null);
					if (Is.notNullOrEmpty(cmd)){
						return new SimpleEntry<>("enable", cmd);
					}
				}else if (state.matches(REGEX_STATE_DISABLE)){
					String cmd = Converters.obj2StringOrDefault(setCmds.get("disable"), null);
					if (Is.notNullOrEmpty(cmd)){
						return new SimpleEntry<>("disable", cmd);
					}
				}else if (Is.typeEqual(stateType, StateType.text_raw)){
					String cmd = Converters.obj2StringOrDefault(setCmds.get("raw"), null);
					if (Is.notNullOrEmpty(cmd)){
						return new SimpleEntry<>("raw", cmd.replaceAll("<val>|<value>", state));
					}
				}
			}
		}
		return null;
	}
	/**
	 * Get set-command for a state from the custom 'setCmds' object, e.g. defined in control HUB for specific device.
	 * @param state - state as given by NLU, e.g. on, off, 50%
	 * @param stateType - type of state to set, e.g. text (on,off) or number (50%), as seen in {@link StateType}
	 * @param setCmds - JSONObject usually taken from device meta data
	 * @return new set command or null
	 */
	public static String getStateFromCustomSetCommands(String state, String stateType, JSONObject setCmds){
		SimpleEntry<String, String> se = getKeyAndStateFromCustomSetCommands(state, stateType, setCmds);
		if (se == null){
			return null;
		}else{
			return se.getValue();
		}
	}
	
	/**
	 * A more complex way of extracting the state from a given data JSONObject,
	 * supporting a nested path and mathematical expression.
	 * @param readExpression - use &lt;path&gt; to define state location and add math., e.g.: "100 * &lt;attr.brightness&gt; / 255"
	 * @param data - JSON object holding the state, e.g. {"attr": {"brightness": 255}} 
	 * @return
	 */
	public static String getStateFromJsonViaExpression(String readExpression, JSONObject data){
		try {
			//identify variable
			String readVar;
			if (readExpression.contains("<")){
				readVar = readExpression.replaceFirst(".*?(<.*?>).*", "$1")
					.replaceFirst("^<", "").replaceFirst(">$", "").trim();
				//make expression a proper math. string
				readExpression = readExpression.replace("<" + readVar + ">", "x");
			}else{
				readVar = readExpression;
				readExpression = "x";
			}
			//load variable value
			String[] readPath = readVar.split("\\.");
			Object state = JSON.getObject(data, readPath);
			//System.out.println("state=" + state);								//DEBUG
			//System.out.println("readExpression=" + readExpression);			//DEBUG
			//System.out.println("readPath=" + String.join("->", readPath));	//DEBUG
			if (state == null){
				return null;
			}else{
				String stateStr = Converters.obj2StringOrDefault(state, "");
				if (readExpression.equals("x")){
					//just return
					return stateStr;
				}else if (stateStr.matches("(-|)\\d+(\\.\\d+|)")){	//NOTE: we do NOT support 1,2 only 1.2
					//calculate
					Map<String, Double> calcVars = new HashMap<>();
					calcVars.put("x", Double.valueOf(stateStr));
					return Calculator.parseExpression(readExpression, calcVars).toString();
				}else{
					//expression cannot be calculated
					return null;
				}
			}
		}catch(Exception ex){
			Debugger.println("'getStateFromJsonViaExpression' failed! Err.: " + ex.getMessage(), 1);
			return null;
		}
	}
	/**
	 * Calculate the new state and build nested JSON object according to path variable in write expression. 
	 * @param writeExpression - expression with path variable, e.g.: "&lt;attr.brightness&gt;" or "2.55 * &lt;attr.brightness&gt;"
	 * @param state - state to write, optionally used as value for calculation variable
	 * @return JSON object depending on expression, e.g. '{"attr": {"brightness": state}}' for "&lt;attr.brightness&gt;"
	 */
	public static JSONObject buildStateDataFromWriteExpression(String writeExpression, String state){
		try {
			//identify variable
			String writeVar;
			if (writeExpression.contains("<")){
				writeVar = writeExpression.replaceFirst(".*?(<.*?>).*", "$1")
					.replaceFirst("^<", "").replaceFirst(">$", "").trim();
				//make expression a proper math. string
				writeExpression = writeExpression.replace("<" + writeVar + ">", "x");
			}else{
				writeVar = writeExpression;
				writeExpression = "x";
			}
			//build data
			JSONObject data = new JSONObject();
			if (writeExpression.equals("x")){
				//no state change
				return JSON.putWithDotPath(data, writeVar, state);
			}else if (state.matches("(-|)\\d+(\\.\\d+|)")){	//NOTE: we do NOT support 1,2 only 1.2
				//calculate
				Map<String, Double> calcVars = new HashMap<>();
				calcVars.put("x", Double.valueOf(state));
				String calcRes = Calculator.parseExpression(writeExpression, calcVars).toString();	//NOTE: this will be e.g. 10.0 not 10!
				return JSON.putWithDotPath(data, writeVar, calcRes);
			}else{
				//expression cannot be calculated
				return null;
			}
		}catch (Exception ex){
			Debugger.println("'buildStateDataFromWriteExpression' failed! Err.: " + ex.getMessage(), 1);
			return null;
		}
	}
}
