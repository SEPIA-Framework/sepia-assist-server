package net.b07z.sepia.server.assist.smarthome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DB;
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
	public boolean registerSepiaFramework(){
		//not required
		return true;
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices(){
		//load from database
		Map<String, SmartHomeDevice> devices = DB.getSmartDevicesDb().getCustomDevices(null);
		return devices;
	}

	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		//load from database
		Map<String, SmartHomeDevice> devices = DB.getSmartDevicesDb().getCustomDevices(filters);
		if (devices == null){
			return null;
		}else{
			List<SmartHomeDevice> filteredDevices = new ArrayList<>();
			filteredDevices.addAll(devices.values());
			return filteredDevices;
		}
	}

	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		// TODO Auto-generated method stub
		System.out.println("device: " + device);
		System.out.println("attrName: " + attrName);
		System.out.println("attrValue: " + attrValue);
		return false;
	}

	@Override
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, Set<String>> getBufferedDeviceNamesByType(){
		// TODO Auto-generated method stub
		return null;
	}

}
