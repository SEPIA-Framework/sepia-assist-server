package net.b07z.sepia.server.assist.smarthome;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.Room;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.SmartHomeHubConnector;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A Smart Home HUB is the software abstraction layer between physical and virtual devices/items. This class represents the interface
 * that connects SEPIA to one of these HUBs. Implementations of the interface need to translate SEPIA items and actions into requests 
 * to the HUB. Typically the HUB's HTTP REST API is used to do so but MQTT or custom connections can be used as well.   
 *  
 * @author Florian Quirin
 *
 */
public interface SmartHomeHub {
	
	//possible custom interfaces
	public static JSONArray interfaceTypes  = JSON.makeArray(
		JSON.make("value", OpenHAB.NAME, 	"name", "openHAB"),
		JSON.make("value", Fhem.NAME, 		"name", "FHEM"),
		JSON.make("value", IoBroker.NAME, 	"name", "ioBroker"),
		JSON.make("value", MqttHub.NAME,	"name", "MQTT"),
		JSON.make("value", TestHub.NAME, 	"name", "Test")
	);
	
	/**
	 * Get HUB from server config (smarthome_hub_name, smarthome_hub_host).
	 * @return HUB or null
	 */
	public static SmartHomeHub getHubFromSeverConfig(){
		SmartHomeHub shh = getHub(Config.smarthome_hub_name, Config.smarthome_hub_host);
		if (Is.notNullOrEmpty(Config.smarthome_hub_auth_data)){
			shh.setAuthenticationInfo(Config.smarthome_hub_auth_type, Config.smarthome_hub_auth_data);
		}
		return shh;
	}
	/**
	 * Get HUB with custom data (name and host). Use 'setAuthenticationInfo' later if you need to add auth. data.
	 * @param hubName - name like "openhab" or "fhem". A full class name is possible as well.
	 * @param hubHost - address of HUB, e.g. http://localhost:8083/fhem
	 * @return HUB or null
	 */
	public static SmartHomeHub getHub(String hubName, String hubHost){
		SmartHomeHub smartHomeHUB;
		hubName = hubName.trim();
		if (Is.nullOrEmpty(hubName)){
			return null;
		}else if (hubName.equalsIgnoreCase(OpenHAB.NAME)){
			smartHomeHUB = new OpenHAB(hubHost);
		}else if (hubName.equalsIgnoreCase(Fhem.NAME)){
			smartHomeHUB = new Fhem(hubHost);
		}else if (hubName.equalsIgnoreCase(IoBroker.NAME)){
			smartHomeHUB = new IoBroker(hubHost);
		}else if (hubName.equalsIgnoreCase(MqttHub.NAME)){
			smartHomeHUB = new MqttHub(hubHost);
		}else if (hubName.equalsIgnoreCase(InternalHub.NAME) || hubName.equalsIgnoreCase("sepia")){
			smartHomeHUB = new InternalHub();
		}else if (hubName.equalsIgnoreCase(TestHub.NAME)){
			smartHomeHUB = new TestHub(hubHost);
		}else{
			try {
				smartHomeHUB = (SmartHomeHub) ClassBuilder.construct(hubName);
				smartHomeHUB.setHostAddress(hubHost);
			}catch (Exception e){
				Debugger.println(SmartHomeHubConnector.class.getSimpleName() + " - Error trying to load smart home HUB data: " + hubName, 1);
				Debugger.printStackTrace(e, 3);
				return null;
			}
		}
		return smartHomeHUB;
	}
	
	/**
	 * Convert HUB data to JSON. This must conform to the format: {<br>
	 *  "id": ..,<br>
	 *  "type": ..,<br>
	 *  "host": ..,<br>
	 *  "authType": ..,<br>
	 *  "authData": ..<br>,
	 *  "info": {...}
	 * }
	 */
	public JSONObject toJson();
	/**
	 * Import JSON data to create HUB. 
	 * @param jsonData - HUB data previously exported
	 */
	public static SmartHomeHub importJson(JSONObject jsonData){
		//re-mapping (mostly to validate code - add importJson method?)
		String id = JSON.getString(jsonData, "id");
		String type = JSON.getString(jsonData, "type");
		String host = JSON.getString(jsonData, "host");
		SmartHomeHub shh = SmartHomeHub.getHub(type, host);
		if (shh != null){
			shh.setId(id);
			shh.setAuthenticationInfo(JSON.getString(jsonData, "authType"), JSON.getString(jsonData, "authData"));
			shh.setInfo(JSON.getJObject(jsonData, "info"));
		}
		return shh;
	}
	
	/**
	 * Set or overwrite host address.
	 * @param hostUrl - e.g. http://localhost:8080
	 */
	public void setHostAddress(String hostUrl);
	
	/**
	 * Set authentication info e.g. to do Basic-Authorization against a proxy via request header etc.. 
	 * Different HUBs can have additional layers of security and this
	 * is usually the first. It can be independent of the HUB itself.
	 * @param authType - type of auth. e.g. 'Basic'
	 * @param authData - data for auth. type e.g. a base64 encoded user:password combination 
	 */
	public void setAuthenticationInfo(String authType, String authData);
	
	/**
	 * Add an ID for this HUB if you're using multiple ones of same type or what to use it in custom setups.
	 */
	public void setId(String id);
	/**
	 * Get ID of this HUB.
	 */
	public String getId();
	
	/**
	 * Add any kind of extra info via the info-object, e.g. "name", "description" etc.
	 * @param info
	 */
	public void setInfo(JSONObject info);
	/**
	 * Get extra info (optional data, might not be set).
	 * @return info object with data, empty or null
	 */
	public JSONObject getInfo();
	
	/**
	 * Register SEPIA Framework in specific smart home HUB software. This can for example be the creation of new attributes 
	 * inside the HUB to be able to use the SEPIA tags 'sepia-name', 'sepia-room', ... etc. (e.g. FHEM).
	 * @return true if registration was successful, false if failed (print errors to log)
	 */
	public boolean registerSepiaFramework();
	
	/**
	 * Check if the HUB implementation requires registration (usually once).
	 * @return true if registration is required (in general) 
	 */
	public boolean requiresRegistration();
	
	/**
	 * Get devices from HUB and convert them to SEPIA compatible {@link SmartHomeDevice}. Apply optional filters to reduce results in advance.
	 * @return devices, empty (no devices received) or null (request error)
	 */
	public Map<String, SmartHomeDevice> getDevices();
	
	/**
	 * Get a list of devices with optional filters:<br>
	 * <li>name</li>
	 * <li>type as seen in {@link SmartDevice.Types}</li>
	 * <li>room as seen in {@link Room.Types}</li>
	 * <li>roomIndex</li>
	 * <li>limit</li>
	 * @param filters - map with optional filters as seen above (or null)
	 * @return devices, empty (no devices received) or null (request error)
	 */
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters);
	
	/**
	 * Write attribute of specific device. This is usually used to register the SEPIA Framework and to tag devices as SEPIA items.
	 * Attribute name and value should be included in 'device' but are given as separate parameters to optimize write process (if possible). 
	 * @param device - generalized SEPIA smart home device (with most recent state, that means including attrName with attrValue)
	 * @param attrName - name of attribute, e.g. "sepia-name" (SmartHomeDevice.SEPIA_TAG_...)
	 * @param attrValue - simple string as value
	 * @return success/fail (due to any error, e.g. connection or missing data)
	 */
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue);
	
	/**
	 * Get device data via direct access.
	 * @param device - {@link SmartHomeDevice} taken from getDevices()
	 * @return device response or null (connection error or no device link)
	 */
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device);
	
	/**
	 * Push new status to device (e.g. via direct access link (URL) given in object).
	 * @param device - {@link SmartHomeDevice} taken from getDevices()
	 * @param state - new status value (NOTE: the HUB implementation might have to translate the state value to its own format)
	 * @param stateType - type of state variable, e.g. {@link SmartHomeDevice#STATE_TYPE_NUMBER_PERCENT} = number in percent
	 * @return true IF no error was thrown after request
	 */
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType);
	
	/**
	 * Set the state memory for a device (e.g. a brightness setting to remember as default).
	 * @param device - {@link SmartHomeDevice} taken from getDevices()
	 * @param stateMemory - device state to remember (format can be HUB specific as defined by combining state and stateType info)
	 * @return true IF no error was thrown after request
	 */
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory);
	
	/**
	 * Returns a map with device type as key and a set of device names for this type as value.<br> 
	 * The method is meant to be used for example by NLU parameters to extract entities. It should return a buffered result for super fast
	 * access.<br>
	 * Note for developers: AVOID RELOADING during the call (except on first call) since this can slow down SEPIA's NLU chain dramatically!<br>
	 * Use only names that are defined via SEPIA ('sepia-name' tag), you can check this via 'namedBySepia' in meta info of {@link SmartHomeDevice}!
	 * @return set of device names by type
	 */
	public Map<String, Set<String>> getBufferedDeviceNamesByType();

}
