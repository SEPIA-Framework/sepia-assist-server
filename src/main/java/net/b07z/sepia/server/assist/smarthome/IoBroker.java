package net.b07z.sepia.server.assist.smarthome;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

/**
 * IoBroker integration for smart home HUB interface.
 * 
 * @author Florian Quirin
 *
 */
public class IoBroker implements SmartHomeHub {
	
	public static final String NAME = "iobroker";
	
	private String host;
	private String authType;
	private String authData;
	private JSONObject info;
	
	private static Map<String, Map<String, Set<String>>> bufferedDevicesOfHostByType = new ConcurrentHashMap<>();
	private Map<String, Set<String>> bufferedDevicesByType;

	/**
	 * Build ioBroker connector with given host address.
	 * @param host - e.g. http://localhost:8080
	 */
	public IoBroker(String hubHost){
		if (Is.nullOrEmpty(this.host)){
			throw new RuntimeException("No host address found for ioBroker integration!");
		}else{
			this.host = hubHost;
			this.bufferedDevicesByType = bufferedDevicesOfHostByType.get(this.host);
		}
	}
	
	//HTTP call methods for HUB
	private Map<String, String> addAuthHeader(Map<String, String> headers){
		return Connectors.addAuthHeader(headers, this.authType, this.authData);
	}
	private JSONObject httpGET(String url){
		if (Is.notNullOrEmpty(this.authData)){
			return Connectors.httpGET(url, null, addAuthHeader(null));
		}else{
			return Connectors.httpGET(url);
		}
	}
	
	//-------INTERFACE IMPLEMENTATIONS---------

	@Override
	public void setHostAddress(String hostUrl){
		this.host = hostUrl;
	}
	
	@Override
	public void setAuthenticationInfo(String authType, String authData){
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
	public boolean registerSepiaFramework(){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, SmartHomeDevice> getDevices(){
		//http://192.168.178.10:8087/objects?pattern=*.command&prettyPrint
		String url = URLBuilder.getString(this.host, 
				"/objects?pattern=", "*.command"
		);
		JSONObject result = httpGET(url);
		if (Connectors.httpSuccess(result)){
			try {
				Map<String, SmartHomeDevice> devices = new HashMap<>();
				Set<String> devicesURIs = JSON.getKeys(result);
				
				//use the chance to update the "names by type" buffer
				this.bufferedDevicesByType = new ConcurrentHashMap<>();
				
				//convert all to 'SmartHomeDevice' and collect
				for (String key : devicesURIs){
					JSONObject hubDevice = JSON.getJObject(result, key);
					
					//Build unified object for SEPIA
					SmartHomeDevice shd = buildDeviceFromResponse(hubDevice);
					
					//devices
					if (shd != null){
						devices.put(shd.getMetaValueAsString("id"), shd);
						
						//fill buffer
						if ((boolean) shd.getMeta().get("namedBySepia")){
							Set<String> deviceNamesOfType = this.bufferedDevicesByType.get(shd.getType());
							if (deviceNamesOfType == null){
								deviceNamesOfType = new HashSet<>();
								this.bufferedDevicesByType.put(shd.getType(), deviceNamesOfType);
							}
							deviceNamesOfType.add(SmartHomeDevice.getCleanedUpName(shd.getName()));		//NOTE: use "clean" name!
						}
					}
				}
				
				//store new buffer
				bufferedDevicesOfHostByType.put(this.host, this.bufferedDevicesByType);
				
				return devices;
				
			}catch (Exception e){
				Debugger.println("ioBroker - getDevices FAILED with msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				return null;
			}
		}else{
			Debugger.println("ioBroker - getDevices FAILED with msg.: " + result.toJSONString(), 1);
			return null;
		}
	}

	@Override
	public List<SmartHomeDevice> getFilteredDevicesList(Map<String, Object> filters){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean writeDeviceAttribute(SmartHomeDevice device, String attrName, String attrValue){
		// TODO Auto-generated method stub
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

	//------------- ioBroker specific helper methods --------------
	
	//build device from JSON response
	private SmartHomeDevice buildDeviceFromResponse(JSONObject hubDevice){
		/*Object linkObj = (fhemObjName != null)? (this.host + "?cmd." + fhemObjName) : null;
		JSONObject meta = JSON.make(
				"id", fhemObjName,
				"origin", NAME,
				"setOptions", JSON.getStringOrDefault(hubDevice, "PossibleSets", null), 		//FHEM specific
				"setCmds", setCmds,
				"typeGuessed", typeGuessed
		);
		JSON.put(meta, "namedBySepia", namedBySepia);
		//note: we need 'id' for commands although it is basically already in 'link'
		SmartHomeDevice shd = new SmartHomeDevice(name, type, room, 
				state, stateType, memoryState, 
				(linkObj != null)? linkObj.toString() : null, meta);
		//specify more
		if (Is.notNullOrEmpty(roomIndex)){
			shd.setRoomIndex(roomIndex);
		}
		return shd;
		*/
		return null;
	}
}
