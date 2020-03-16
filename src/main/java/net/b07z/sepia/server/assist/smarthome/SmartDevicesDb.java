package net.b07z.sepia.server.assist.smarthome;

import java.util.Map;

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
	 * Add a custom smart device.
	 * @param data - object with e.g. name, type, room, custom_commands, etc.
	 * @return result code
	 */
	public int addOrUpdateCustomDevice(JSONObject data);
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
	 * Get all custom smart devices that correspond to filter set.
	 * @param filters - Map with filters, e.g. type=light, room=livingroom, etc.
	 * @return
	 */
	public Map<String, SmartHomeDevice> getCustomDevices(Map<String, Object> filters);
	
}
