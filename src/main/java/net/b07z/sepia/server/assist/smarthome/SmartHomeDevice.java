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
	private String interfaceId;	//e.g. openhab, fhem
	private JSONObject meta;	//space for custom stuff
	
	//global tags used to store SEPIA specific device data in other HUB systems
	public static final String SEPIA_TAG_NAME = "sepia-name";
	public static final String SEPIA_TAG_TYPE = "sepia-type";
	public static final String SEPIA_TAG_ROOM = "sepia-room";
	public static final String SEPIA_TAG_ROOM_INDEX = "sepia-room-index";
	public static final String SEPIA_TAG_DATA = "sepia-data";
	public static final String SEPIA_TAG_MEM_STATE = "sepia-mem-state";
	public static final String SEPIA_TAG_STATE_TYPE = "sepia-state-type";
	public static final String SEPIA_TAG_SET_CMDS = "sepia-set-cmds";
	
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
	 */
	public static String getStateLocal(String state, String language){
		String localName = "";
		state = state.toLowerCase();
		if (language.equals(LANGUAGES.DE)){
			localName = states_de.get(state);
		}else if (language.equals(LANGUAGES.EN)){
			localName = states_en.get(state);
		}
		if (localName == null){
			if (!state.matches("\\d+")){
				Debugger.println(SmartHomeDevice.class.getSimpleName() + 
					" - getStateLocal() has no '" + language + "' version for '" + state + "'", 3);
			}
			return state;
		}else{
			return localName;
		}
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
	 * Device interface id
	 * @return
	 */
	public String getInterface() {
		return interfaceId;
	}
	/**
	 * Set class variable 'interfaceId' (no write to HUB!)
	 */
	public void setInterface(String interfaceId) {
		this.interfaceId = interfaceId;
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
	 * If no interface is defined it will return the "normal" ID (meta.id).
	 * @return
	 */
	public String getId(){
		if (Is.notNullOrEmpty(this.interfaceId)){
			return getMetaValueAsString("interfaceDeviceId");
		}else{
			return getMetaValueAsString("id");
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
			return JSON.getJObject(meta, "setCmds");
		}
	}
	/**
	 * Set custom commands object (aka 'setCmds'; no write to HUB!)
	 */
	public void setCustomCommands(JSONObject setCmds){
		setMetaValue("setCmds", setCmds);
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
	 * @param deviceType - type of device (or null), see {@link SmartDevice.Types}
	 * @param roomType - type of room (or null), see {@link Room.Types}
	 * @param roomIndex - e.g. a number (as string) or null
	 * @param maxDevices - maximum number of matches (0 or negative for all possible)
	 * @return list of devices (can be empty)
	 */
	public static List<SmartHomeDevice> getMatchingDevices(Map<String, SmartHomeDevice> devices, 
				String deviceType, String roomType, String roomIndex, int maxDevices){
		List<SmartHomeDevice> matchingDevices = new ArrayList<>();
		//get all devices with right type and optionally room
		int found = 0;
		for (Map.Entry<String, SmartHomeDevice> entry : devices.entrySet()){
			//check type
			SmartHomeDevice data = entry.getValue();
			String thisType = data.getType();
			if (Is.nullOrEmpty(thisType)){
				continue;
			}
			if (Is.notNullOrEmpty(deviceType) && !thisType.equals(deviceType)){
				continue;
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
		//TODO: add more?
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
				|| deviceType.equals(SmartDevice.Types.roller_shutter)){
			return StateType.number_percent.name();
		}else if (deviceType.equals(SmartDevice.Types.heater)){
			return StateType.number_temperature.name();
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
	 * @param deviceType - type of smart device that might be used to make smart assumptions, e.g. {@link SmartDevice.Types#light}
	 * @param deviceStateType - expected state type of device, e.g. {@link StateType#number_temperature_c} (string)
	 * @param inputStateType - state type given as input, e.g. {@link StateType#number_plain} (string)
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
			//TODO: need to properly convert state to newInputState?
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
	 * Get state set command from custom object, e.g. defined in control HUB for specific device.
	 * @param state - state as given by NLU, e.g. on, off, 50%
	 * @param stateType - type of state to set, e.g. text (on,off) or number (50%), as seen in {@link StateType}
	 * @param setCmds - JSONObject usually taken from device meta data
	 * @return new set command or null
	 */
	public static String getStateFromCustomSetCommands(String state, String stateType, JSONObject setCmds){
		if (stateType != null){
			if (stateType.matches(REGEX_STATE_TYPE_NUMBER)){
				String cmd = (String) setCmds.get("number");
				if (Is.notNullOrEmpty(cmd)){
					return cmd.replaceAll("<val>|<value>", state);
				}
			}else if (stateType.matches(REGEX_STATE_TYPE_TEXT)){
				if (state.matches(REGEX_STATE_ENABLE)){
					String cmd = (String) setCmds.get("enable");
					if (Is.notNullOrEmpty(cmd)){
						return cmd;
					}
				}else if (state.matches(REGEX_STATE_DISABLE)){
					String cmd = (String) setCmds.get("disable");
					if (Is.notNullOrEmpty(cmd)){
						return cmd;
					}
				}
			}
		}
		return null;
	}
}
