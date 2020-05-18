package net.b07z.sepia.server.assist.smarthome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A HUB that uses the internal database to store devices and executes actions by using the other known HUBs or simple actions.
 * 
 * @author Florian Quirin
 *
 */
public class InternalHub implements SmartHomeHub {
	
	public static final String NAME = "internal";
	
	private JSONObject info;
	private String hubId;
	
	/**
	 * Create internal HUB for smart devices.
	 */
	public InternalHub(){}
	
	@Override
	public JSONObject toJson(){
		return JSON.make(
			"id", this.hubId,
			"type", NAME,
			"host", "SEPIA",
			"authType", "",
			"authData", "",
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
		//not required
		return true;
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices(){
		//load from database
		Map<String, SmartHomeDevice> devices = new HashMap<>();
		List<SmartHomeDevice> devicesList = getFilteredDevicesList(null);
		if (devicesList == null){
			return null;
		}else{
			for (SmartHomeDevice shd : devicesList){
				String id = shd.getMetaValueAsString("id");
				if (Is.notNullOrEmpty(id)){		//actually this can never be null or empty ... in theory
					devices.put(shd.getMetaValueAsString("id"), shd);
				}
			}
		}
		return devices;
	}

	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		//load from database
		Map<String, SmartHomeDevice> devices = DB.getSmartDevicesDb().getCustomDevices(filters);
		if (devices == null){
			return null;
		}else{
			//get data of each device from HUB interface
			List<SmartHomeDevice> filteredDevices = new ArrayList<>();
			for (SmartHomeDevice shd : devices.values()){
				//filter again! (because the DB is more tolerant)
				boolean filterMatch = true;
				if (filters != null){
					//currently we only check these filters
					for (String filter : filters.keySet()){
						//System.out.println("filter: " + filter + ", val.: " + filters.get(filter));		//DEBUG
						if (filter.equals(SmartHomeDevice.FILTER_TYPE)){
							if (!((String) filters.get(filter)).equals(shd.getType())){
								filterMatch = false;
								break;
							}
						}else if (filter.equals(SmartHomeDevice.FILTER_ROOM)){
							if (!((String) filters.get(filter)).equals(shd.getRoom())){
								filterMatch = false;
								break;
							}
						}else if (filter.equals(SmartHomeDevice.FILTER_ROOM_INDEX)){
							if (!((String) filters.get(filter)).equals(shd.getRoomIndex())){
								filterMatch = false;
								break;
							}
						}else if (filter.equals(SmartHomeDevice.FILTER_NAME)){
							if (!((String) filters.get(filter)).equals(shd.getName())){
								filterMatch = false;
								break;
							}
						}
					}
				}
				if (filterMatch){
					shd = loadDeviceData(shd);
					filteredDevices.add(shd);
				}
			}
			return filteredDevices;
		}
	}

	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		if (attrName.equals("type")){
			device.setMetaValue("typeGuessed", false);
		}else if (attrName.equals("name")){
			device.setMetaValue("namedBySepia", true);
		}
		JSONObject res = DB.getSmartDevicesDb().addOrUpdateCustomDevice(device.getDeviceAsJson());
		return (JSON.getIntegerOrDefault(res, "code", 2) == 0);
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		String interfaceId = device.getInterface();
		String interfaceDeviceId = device.getMetaValueAsString("interfaceDeviceId");
		//System.out.println("Device: " + device.getName() + " " + interfaceId + " " + interfaceDeviceId); 		//DEBUG
		boolean isIncompleteOrFaulty;
		if (Is.nullOrEmpty(interfaceId) || Is.nullOrEmpty(interfaceDeviceId)){
			//Debugger.println("Smart Home HUB Interface - getFilteredDevicesList"
			//		+ " - Missing interface or interface device ID for: " + shd.getName(), 1);
			isIncompleteOrFaulty = true;
		}else{
			try{
				SmartHomeHub shh = DB.getSmartDevicesDb().getCachedInterfaces().get(interfaceId);
				SmartHomeDevice shdFromHub = shh.loadDeviceData(device);		
				//NOTE: the HUB is responsible for checking and using interfaceId and interfaceDeviceId
				//import data
				if (importHubDeviceDataToInternal(device, shdFromHub)){
					isIncompleteOrFaulty = false;
				}else{
					Debugger.println("Smart Home HUB Interface - loadDeviceData"
							+ " - Failed to get device data: " + interfaceId + " " + interfaceDeviceId, 1);
					isIncompleteOrFaulty = true;
				}
			
			}catch (Exception e){
				Debugger.println("Smart Home HUB Interface - loadDeviceData"
						+ " - Failed to get device: " + interfaceId + " " + interfaceDeviceId + " - msg: " + e, 1);
				Debugger.printStackTrace(e, 3);
				isIncompleteOrFaulty = true;
			}
		}
		if (isIncompleteOrFaulty){
			//add anyway so user can edit it, but tag it
			device.setMetaValue("isIncomplete", true);
		}else{
			device.removeMetaField("isIncomplete");
		}
		return device;
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		String interfaceId = device.getInterface();
		String interfaceDeviceId = device.getMetaValueAsString("interfaceDeviceId");
		if (Is.nullOrEmpty(interfaceId) || Is.nullOrEmpty(interfaceDeviceId)){
			return false;
		}else{
			try{
				SmartHomeHub shh = DB.getSmartDevicesDb().getCachedInterfaces().get(interfaceId);
				//NOTE: the HUB is responsible for checking and using interfaceId and interfaceDeviceId
				return shh.setDeviceState(device, state, stateType);		
			
			}catch (Exception e){
				Debugger.println("Smart Home HUB Interface - setDeviceState"
						+ " - Failed to set state for device: " + interfaceId + " " + interfaceDeviceId + " - msg: " + e, 1);
				Debugger.printStackTrace(e, 3);
				return false;
			}
		}
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory){
		//we store this in the internal database
		device.setStateMemory(stateMemory);
		return writeDeviceAttribute(device, "stateMemory", stateMemory);
	}

	@Override
	public Map<String, Set<String>> getBufferedDeviceNamesByType(){
		return DB.getSmartDevicesDb().getBufferedDeviceNamesByType();
	}
	
	//----- internal HUB specific helpers -----
	
	/**
	 * The internal database stores basic info but is usually missing up-to-date info about device state for example.
	 * This method imports the missing data from the external HUB (or any HUB given by the HUB interface).
	 * @param internalDevice - device loaded from internal DB
	 * @param hubDevice - device loaded via HUB interface (usually external HUB)
	 * return true if import worked (which does not mean there was actual data)
	 */
	public static boolean importHubDeviceDataToInternal(SmartHomeDevice internalDevice, SmartHomeDevice hubDevice){
		if (internalDevice != null && hubDevice != null){
			internalDevice.setState(hubDevice.getState());
			//internalDevice.setStateType(hubDevice.getStateType());
			//memory state?
			return true;
		}else{
			return false;
		}
	}

}
