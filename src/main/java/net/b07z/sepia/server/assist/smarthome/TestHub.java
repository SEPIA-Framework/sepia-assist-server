package net.b07z.sepia.server.assist.smarthome;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.Room;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
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
	
	private JSONObject info;
	private String hubId;
	
	//fake auth.
	private String authType;
	private String authData;
	
	//------ test devices ------
	
	private static SmartHomeDevice light = new SmartHomeDevice(
			"Lamp (1)", 
			SmartDevice.Types.light.name(), 
			Room.Types.livingroom.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.number_percent.name(), "", 
			"link", JSON.make(
					"id", "Light_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
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
					"typeGuessed", true,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice heater = new SmartHomeDevice(
			"Heater (1)", 
			SmartDevice.Types.heater.name(), 
			Room.Types.livingroom.name(), 
			"22", SmartHomeDevice.StateType.number_temperature_c.name(), "", 
			"link", JSON.make(
					"id", "Heater_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice airConditioner = new SmartHomeDevice(
			"Air Conditioner (1)", 
			SmartDevice.Types.air_conditioner.name(), 
			Room.Types.livingroom.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.number_temperature_c.name(), "", 
			"link", JSON.make(
					"id", "Aircon_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice tempControl = new SmartHomeDevice(
			"Temperature", 
			SmartDevice.Types.temperature_control.name(), 
			Room.Types.livingroom.name(), 
			"21", SmartHomeDevice.StateType.number_temperature_c.name(), "", 
			"link", JSON.make(
					"id", "Temp_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
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
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice garageDoor = new SmartHomeDevice(
			"Garage Door", 
			SmartDevice.Types.garage_door.name(), 
			Room.Types.garage.name(), 
			"100", SmartHomeDevice.StateType.number_percent.name(), "", 
			"link", JSON.make(
					"id", "Garage_Door_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice tv = new SmartHomeDevice(
			"TV (1)", 
			SmartDevice.Types.tv.name(), 
			Room.Types.livingroom.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.text_raw.name(), "", 
			"link", JSON.make(
					"id", "TV_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice fan = new SmartHomeDevice(
			"Fan (1)", 
			SmartDevice.Types.fan.name(), 
			Room.Types.livingroom.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.number_percent.name(), "", 
			"link", JSON.make(
					"id", "Fan_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice sensor1 = new SmartHomeDevice(
			"Gas", 
			SmartDevice.Types.sensor.name(), 
			Room.Types.basement.name(), 
			"1000 kWh", SmartHomeDevice.StateType.text_raw.name(), "", 
			"link", JSON.make(
					"id", "Gas_Sensor_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice sensor2 = new SmartHomeDevice(
			"Electricity", 
			SmartDevice.Types.sensor.name(), 
			Room.Types.basement.name(), 
			"3500 kWh", SmartHomeDevice.StateType.text_raw.name(), "", 
			"link", JSON.make(
					"id", "Electricity_Sensor_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static SmartHomeDevice genericDevice = new SmartHomeDevice(
			"Device A", 
			SmartDevice.Types.device.name(), 
			Room.Types.garage.name(), 
			SmartHomeDevice.State.off.name(), SmartHomeDevice.StateType.text_binary.name(), "", 
			"link", JSON.make(
					"id", "Device_Test_A",
					"origin", NAME,
					"typeGuessed", false,
					"namedBySepia", true
			)
	);
	private static List<SmartHomeDevice> devicesList = Arrays.asList(
			light, light2, heater, airConditioner, tempControl, fan, 
			rollerShutter, garageDoor, tv, sensor1, sensor2, genericDevice
	);
	
	//--------------------------
	
	@Override
	public JSONObject toJson(){
		return JSON.make(
			"id", this.hubId,
			"type", NAME,
			"host", "SEPIA",
			"authType", this.authType,
			"authData", this.authData,
			"info", this.info
		);
	}
	
	@Override
	public boolean activate(){
		return true;
	}
	@Override
	public boolean deactivate(){
		return true;
	}
	
	@Override
	public void setId(String id){
		this.hubId = id; 
	}
	
	@Override
	public String getId(){
		return this.hubId;
	}

	@Override
	public void setHostAddress(String hostUrl){
		//Not required
	}

	@Override
	public void setAuthenticationInfo(String authType, String authData){
		//Not required
		this.authType = authType;
		this.authData = authData;
	}
	
	@Override
	public void setInfo(JSONObject info){
		this.info = info;
	}
	@Override
	public JSONObject getInfo(){
		return this.info;
	}

	@Override
	public boolean requiresRegistration(){
		return false;
	}
	@Override
	public boolean registerSepiaFramework(){
		return true;
	}
	
	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		//get reference to stored object from internal list (the given 'device' can be imported from JSON or created)
		SmartHomeDevice deviceFound = getDevices().get(device.getMetaValueAsString("id"));
		//modify object reference
		if (deviceFound != null){
			Debugger.println(TestHub.class.getSimpleName() + " - writeDeviceAttribute - attrName: " 
					+ attrName + ", attrValue: " + attrValue, 3);
			if (attrName.equals(SmartHomeDevice.SEPIA_TAG_NAME)){
				deviceFound.setName(attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_TYPE)){
				deviceFound.setType(attrValue);
				JSON.put(deviceFound.getMeta(), "typeGuessed", false);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_ROOM)){
				deviceFound.setRoom(attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_ROOM_INDEX)){
				deviceFound.setRoomIndex(attrValue);
			}else if (attrName.equals(SmartHomeDevice.SEPIA_TAG_SET_CMDS)){
				if (attrValue != null && attrValue.trim().startsWith("{")){
					deviceFound.setCustomCommands(JSON.parseString(attrValue));
				}
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
			devices.put(shd.getMetaValueAsString("id"), shd);
		}
		return devices;
	}
	
	@Override
	public Map<String, Set<String>> getBufferedDeviceNamesByType(){
		Map<String, Set<String>> devicesByType = new HashMap<>();
		for (SmartHomeDevice shd : devicesList){
			String type = shd.getType();
			Set<String> devices = devicesByType.get(type);
			if (devices == null){
				devices = new HashSet<>();
				devicesByType.put(type, devices);
			}
			devices.add(SmartHomeDevice.getCleanedUpName(shd.getName()));		//NOTE: use "clean" name!
		}
		return devicesByType;
	}

	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		Map<String, SmartHomeDevice> devices = getDevices();
		if (devices == null){
			return null;
		}else{
			return SmartHomeDevice.getMatchingDevices(devices, filters);
		}
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		String id = device.getId();
		return getDevices().get(id);
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		//get reference to stored object from internal list (the given 'device' can be imported from JSON or created)
		SmartHomeDevice deviceFound = getDevices().get(device.getId());
		//modify object reference
		Debugger.println(TestHub.class.getSimpleName() + " - setDeviceState - name: " 
				+ device.getName() + ", stateType: " + stateType + ", state: " + state, 3);
		if (deviceFound != null){
			//set command overwrite?
			JSONObject setCmds = device.getCustomCommands();
			if (Is.notNullOrEmpty(setCmds)){
				String newState = SmartHomeDevice.getStateFromCustomSetCommands(state, stateType, setCmds);
				if (newState != null){
					state = newState;
					Debugger.println(TestHub.class.getSimpleName() + " - setDeviceState - using custom state: " + state, 3);
				}
			//check deviceType to find correct set command
			}else{
				//not necessary for Test-HUB since it uses the generalized SEPIA states directly
			}
			deviceFound.setState(state);
			return true;
		}else{
			Debugger.println(TestHub.class.getSimpleName() + " - setDeviceState FAILED - name: " 
					+ device.getName() + " NOT FOUND!", 1);
			return false;
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory){
		SmartHomeDevice deviceFound = getDevices().get(device.getId());
		if (deviceFound != null){
			deviceFound.setStateMemory(stateMemory);
			return true;
		}else{
			return false;
		}
	}

}
