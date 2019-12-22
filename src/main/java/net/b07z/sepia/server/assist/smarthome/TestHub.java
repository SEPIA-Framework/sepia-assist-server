package net.b07z.sepia.server.assist.smarthome;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.assist.parameters.Room;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to test smart home service.
 * 
 * @author Florian Quirin
 *
 */
public class TestHub implements SmartHomeHub {
	
	public static final String NAME = "test";
	
	public TestHub(String host){
		//Not required
	}
	
	//------ test devices ------
	
	private static SmartHomeDevice light = new SmartHomeDevice(
			"Lamp (1)", 
			SmartDevice.Types.light.name(), 
			Room.Types.livingroom.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.number_percent.name(), "", 
			"link", JSON.make(
					"id", "Light_Test_A",
					"origin", NAME,
					"typeGuessed", false
			)
	);
	private static SmartHomeDevice light2 = new SmartHomeDevice(
			"Lamp 2", 
			SmartDevice.Types.light.name(), 
			Room.Types.bath.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.number_percent.name(), "", 
			"link", JSON.make(
					"id", "Light_Test_B",
					"origin", NAME,
					"typeGuessed", true
			)
	);
	private static SmartHomeDevice heater = new SmartHomeDevice(
			"Thermostat (1)", 
			SmartDevice.Types.heater.name(), 
			Room.Types.livingroom.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.number_temperature_c.name(), "", 
			"link", JSON.make(
					"id", "Heater_Test_A",
					"origin", NAME,
					"typeGuessed", false
			)
	);
	private static SmartHomeDevice rollerShutter = new SmartHomeDevice(
			"Roller Shutter 1", 
			SmartDevice.Types.roller_shutter.name(), 
			Room.Types.office.name(), 
			"50", SmartHomeDevice.StateType.number_percent.name(), "", 
			"link", JSON.make(
					"id", "Shutter_Test_A",
					"origin", NAME,
					"typeGuessed", false
			)
	);
	private static List<SmartHomeDevice> devicesList = Arrays.asList(
			light, light2, heater, rollerShutter
	);
	
	//--------------------------

	@Override
	public void setHostAddress(String hostUrl){
		//Not required
	}

	@Override
	public void setAuthenticationInfo(String authType, String authData){
		//Not required
	}

	@Override
	public boolean registerSepiaFramework(){
		return true;
	}
	
	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		SmartHomeDevice deviceFound = getDevices().get(device.getName());
		if (deviceFound != null){
			Debugger.println(TestHub.class.getSimpleName() + " - writeDeviceAttribute - attrName: " 
					+ attrName + ", attrValue: " + attrValue, 3);
			if (attrName.equals(SmartHomeDevice.SEPIA_TAG_NAME)){
				deviceFound.setName(attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_TYPE)){
				deviceFound.setType(attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_ROOM)){
				deviceFound.setRoom(attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_ROOM_INDEX)){
				deviceFound.setRoomIndex(attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_SET_CMDS)){
				JSON.put(deviceFound.getMeta(), "setCmds", attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_STATE_TYPE)){
				deviceFound.setStateType(attrValue);
			}else{
				return false;
			}
			return true;
		}else{
			return false;
		}
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices(){
		Map<String, SmartHomeDevice> devices = new HashMap<>();
		for (SmartHomeDevice shd : devicesList){
			devices.put(shd.getName(), shd);
		}
		return devices;
	}

	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		//filters
		String deviceType = (String) filters.get("type");
		String roomType = (String) filters.get("room");
		String roomIndex = Converters.obj2StringOrDefault(filters.get("roomIndex"), null);
		Object limitObj = filters.get("limit");
		int limit = -1;
		if (limitObj != null){
			limit = (int) limitObj;
		}
		//get all devices with right type and optionally right room
		List<SmartHomeDevice> matchingDevices = SmartHomeDevice.getMatchingDevices(getDevices(), deviceType, roomType, roomIndex, limit);
		return matchingDevices;
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		return getDevices().get(device.getName());
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		SmartHomeDevice deviceFound = getDevices().get(device.getName());
		Debugger.println(TestHub.class.getSimpleName() + " - setDeviceState - name: " 
				+ device.getName() + ", stateType: " + stateType + ", state: " + state, 3);
		if (deviceFound != null){
			deviceFound.setState(state);
			return true;
		}else{
			return false;
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory){
		SmartHomeDevice deviceFound = getDevices().get(device.getName());
		if (deviceFound != null){
			deviceFound.setStateMemory(stateMemory);
			return true;
		}else{
			return false;
		}
	}

}
