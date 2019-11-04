package net.b07z.sepia.server.assist.smarthome;

import java.util.Map;

import net.b07z.sepia.server.assist.parameters.Room;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.SmartHomeHubConnector;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;

public interface SmartHomeHub {
	
	/**
	 * Get HUB from server config (smarthome_hub_name, smarthome_hub_host).
	 * @return HUB or null
	 */
	public static SmartHomeHub getHubFromSeverConfig(){
		return getHub(Config.smarthome_hub_name, Config.smarthome_hub_host);
	}
	/**
	 * Get HUB with custom data (name and host).
	 * @param hubName - name like "openhab" or "fhem". A full class name is possible as well.
	 * @param hubHist - address of HUB, e.g. http://localhost:8083/fhem
	 * @return HUB or null
	 */
	public static SmartHomeHub getHub(String hubName, String hubHost){
		SmartHomeHub smartHomeHUB;
		if (Is.nullOrEmpty(hubName)){
			return null;
		}else if (hubName.trim().equalsIgnoreCase(OpenHAB.NAME)){
			smartHomeHUB = new OpenHAB(hubHost);
		}else if (hubName.trim().equalsIgnoreCase(Fhem.NAME)){
			smartHomeHUB = new Fhem(hubHost);
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
	 * Set or overwrite host address.
	 * @param hostUrl - e.g. http://localhost:8080
	 */
	public void setHostAddress(String hostUrl);
	
	/**
	 * Register SEPIA Framework in specific smart home HUB software. This can for example be the creation of new attributes 
	 * inside the HUB to be able to use the SEPIA tags 'sepia-name', 'sepia-room', ... etc. (e.g. FHEM).
	 * @return true if registration was successful, false if failed (print errors to log)
	 */
	public boolean registerSepiaFramework();
	
	/**
	 * Get devices from HUB and convert them to SEPIA compatible {@link SmartHomeDevice}. Apply optional filters to reduce results in advance.
	 * @param optionalNameFilter - name of device (any string) as filter or null
	 * @param optionalTypeFilter - type of device as filter or null, see {@link SmartDevice.Types}
	 * @param optionalRoomFilter - type of room as filter or null, see {@link Room.Types}
	 * @return devices, empty (no devices received) or null (request error)
	 */
	public Map<String, SmartHomeDevice> getDevices(String optionalNameFilter, String optionalTypeFilter, String optionalRoomFilter);
	
	/**
	 * Write attribute of specific device. This is usually used to register the SEPIA Framework and to tag devices as SEPIA items.
	 * @param device - generalized SEPIA smart home device
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

}
