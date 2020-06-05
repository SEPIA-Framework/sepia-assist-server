package net.b07z.sepia.server.assist.smarthome;

import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

/**
 * Interface that manages the database for smart home/device interfaces and custom devices/items.
 * 
 * @author Florian Quirin
 *
 */
public interface SmartDevicesDb {
	
	public static String INTERFACES = "smart-interfaces";
	public static String DEVICES = "smart-devices";
	
	/**
	 * Add a new interface for smart devices, e.g. a smart home HUB or MQTT broker.
	 * @param data - object with e.g. id, type, name, host, auth_info, auth_data, etc.
	 * @return result code (0 - all good, 1 - no connection or error, 2 - invalid data)
	 */
	public int addOrUpdateInterface(JSONObject data);
	/**
	 * Remove interface with given ID.
	 * @param id - ID of the interface, e.g. fhem2
	 * @return result code (0 - all good, 1 - no connection or error)
	 */
	public int removeInterface(String id);
	/**
	 * Load all known interfaces.
	 * @return Map with interface IDs as keys, empty map or null (error during load)
	 */
	public Map<String, SmartHomeHub> loadInterfaces();
	/**
	 * Return a cached map of interfaces (to reduce database i/o).	
	 */
	public Map<String, SmartHomeHub> getCachedInterfaces();
	
	/**
	 * Add a custom smart device.
	 * @param data - object with e.g. name, type, room, custom_commands, etc.
	 * @return JSON with device (doc) "id" (if new) and result "code" (0=good, 1=connection err, 2=other err)
	 */
	public JSONObject addOrUpdateCustomDevice(JSONObject data);
	/**
	 * Remove custom smart device with given ID.
	 * @param id - ID of the device
	 * @return result code
	 */
	public int removeCustomDevice(String id);
	/**
	 * Get custom smart device with given ID.
	 * @param id - ID of the device
	 * @return
	 */
	public SmartHomeDevice getCustomDevice(String id);
	/**
	 * Get all custom smart devices that correspond to filter set.<br>
	 * NOTE: If you add multiple filters the method will try to get the best fit but may return partial fits! Be sure to check the result again afterwards.  
	 * @param filters - Map with filters, e.g. type=light, room=livingroom, etc.
	 * @return
	 */
	public Map<String, SmartHomeDevice> getCustomDevices(Map<String, Object> filters);
	
	/**
	 * Returns a map with device type as key and a set of device names for this type as value.<br> 
	 * The method is meant to be used for example by NLU parameters to extract entities. It should return a buffered result for super fast
	 * access.<br>
	 * Note for developers: AVOID RELOADING during the call (except on first call) since this can slow down SEPIA's NLU chain dramatically!
	 * @return set of device names by type
	 */
	public Map<String, Set<String>> getBufferedDeviceNamesByType();
	/**
	 * Clear device name buffer.
	 * @return success/fail
	 */
	public boolean clearDeviceTypeAndNameBuffer();
	
}
