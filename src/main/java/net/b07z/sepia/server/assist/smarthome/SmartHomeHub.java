package net.b07z.sepia.server.assist.smarthome;

import java.util.Map;

public interface SmartHomeHub {
	
	/**
	 * Register SEPIA Framework in specific smart home HUB software. This can for example be the creation of new attributes 
	 * inside the HUB to be able to use the SEPIA tags 'sepia-name', 'sepia-room', ... etc. (e.g. FHEM).
	 * @return true if registration was successful, false if failed (print errors to log)
	 */
	public default boolean registerSepiaFramework(){
		return true;
	}
	
	/**
	 * Get devices from HUB and convert them to SEPIA compatible {@link SmartHomeDevice}.
	 * @return devices, empty (no devices received) or null (request error)
	 */
	public Map<String, SmartHomeDevice> getDevices();
	
	/**
	 * Get device data via direct access.
	 * @param device - {@link SmartHomeDevice} taken from getDevices()
	 * @return device response or null (connection error or no device link)
	 */
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device);
	
	/**
	 * Push new status to device (e.g. via direct access link (URL) given in object).
	 * @param device - {@link SmartHomeDevice} taken from getDevices()
	 * @param state - new status value
	 * @return true IF no error was thrown after request
	 */
	public boolean setDeviceState(SmartHomeDevice device, String state);
	
	/**
	 * Set the state memory for a device (e.g. a brightness setting to remember as default).
	 * @param device - {@link SmartHomeDevice} taken from getDevices()
	 * @param stateMemory - any state as string
	 * @return true IF no error was thrown after request
	 */
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory);

}
