package net.b07z.sepia.server.assist.smarthome;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

/**
 * Basic ioBroker connector, implementing parts of the smart home HUB interface to get and set states.<br>
 * Device management is handled by SEPIA internal HUB.
 * 
 * @author Florian Quirin
 *
 */
public class IoBrokerConnector implements SmartHomeHub {
	
	public static final String NAME = "iobroker_sapi";
	
	public static int CONNECT_TIMEOUT = 4000;
	
	private String hubId;
	private String host;
	private String authType;
	private String authData;
	private String userName;
	private String password;
	private JSONObject info;
	

	/**
	 * Build ioBroker connector with given host address.
	 * @param host - e.g. http://localhost:8080
	 */
	public IoBrokerConnector(String hubHost){
		if (Is.nullOrEmpty(hubHost)){
			throw new RuntimeException("No host address found for ioBroker integration!");
		}else{
			this.host = hubHost.replaceFirst("/$", "").trim();
		}
	}
	
	//HTTP call methods for HUB
	private Map<String, String> addAuthHeader(Map<String, String> headers){
		return Connectors.addAuthHeader(headers, this.authType, this.authData);
	}
	private JSONObject httpGET(String url){
		if (Is.notNullOrEmpty(this.authData)){
			if (this.authType.equalsIgnoreCase(AuthType.Basic.name())){
				//Basic auth. header
				return Connectors.httpGET(url, null, addAuthHeader(null), CONNECT_TIMEOUT);
				
			}else if ((this.authType.equalsIgnoreCase(AuthType.Plain.name()) || this.authType.equalsIgnoreCase(AuthType.Custom.name())) 
					&& this.userName != null && this.password != null){
				//URL parameters
				if (url.contains("?")){
					url += URLBuilder.getStringP20("", "&user=", this.userName, "&pass=", this.password); 
				}else{
					url += URLBuilder.getStringP20("", "?user=", this.userName, "&pass=", this.password);
				}
				return Connectors.httpGET(url, null, null, CONNECT_TIMEOUT);
			}else{
				throw new RuntimeException("ioBrokerConnector saw invalid auth. type and data. Try type 'Plain' or 'Custom' with data 'username:password'.");
			}
		}else{
			return Connectors.httpGET(url, null, null, CONNECT_TIMEOUT);
		}
	}
	
	//-------INTERFACE IMPLEMENTATIONS-------
	
	@Override
	public JSONObject toJson(){
		return JSON.make(
			"id", this.hubId,
			"type", NAME,
			"host", this.host,
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
		this.host = hostUrl.replaceFirst("/$", "").trim();
	}
	
	@Override
	public void setAuthenticationInfo(String authType, String authData){
		this.authType = authType;
		this.authData = authData;
		if (Is.notNullOrEmpty(this.authType) && Is.notNullOrEmpty(this.authData)){
			if (authType.equalsIgnoreCase(AuthType.Basic.name())){
				//OK
				this.authType = AuthType.Basic.name();
			}else if (authType.equalsIgnoreCase(AuthType.Plain.name()) || authType.equalsIgnoreCase(AuthType.Custom.name())){
				//handle
				this.authType = AuthType.Custom.name();
				try {
					String[] up = authData.split(":", 2);
					this.userName = up[0];
					this.password = up[1];
				}catch (Exception e) {
					Debugger.println("IoBrokerConnector - 'setAuthenticationInfo' FAILED to add auth. data "
							+ "- try type 'Custom' or 'Plain' and data 'username:password' or 'Basic' with base64 encoding.", 1);
					Debugger.printStackTrace(e, 3);
					this.userName = null;
					this.password = null;
				}
			}else{
				throw new RuntimeException("Invalid auth. type. Try 'Custom', 'Plain' or 'Basic'.");
			}
		}
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
	public SmartHomeDevice loadDeviceData(SmartHomeDevice device){
		long tic = System.currentTimeMillis();
		String iobId = device.getId();
		if (Is.nullOrEmpty(iobId)){
			return null;
		}else{
			String url = URLBuilder.getStringP20(this.host + "/get/",
					"", iobId
			);
			//System.out.println("URL: " + url); 				//DEBUG
			JSONObject result = httpGET(url);
			//System.out.println("RESPONSE: " + result);		//DEBUG
			if (Connectors.httpSuccess(result)){
				Statistics.addExternalApiHit("ioBroker loadDevice");
				Statistics.addExternalApiTime("ioBroker loadDevice", tic);
				try{
					/* simplified result example
					 {
					    "val": 0,
					    "common": {
					        "name": "Hue white lamp 1.level",
					        "read": true,
					        "write": true,
					        "type": "number",
					        "role": "level.dimmer",
					        "min": 0,
					        "max": 100,
					        "def": 0
					    }
					}*/
					if (result.containsKey("val")){
						String state = JSON.getString(result, "val");
						String stateType = device.getStateType();
						if (state != null){
							if (stateType != null){
								//generalize state according to stateType
								state = SmartHomeDevice.convertAnyStateToGeneralizedState(state, stateType);
							}
							device.setState(state);
						}
						return device;
					}else{
						return null;
					}
				}catch (Exception e){
					Debugger.println("IoBrokerConnector - loadDeviceData FAILED with msg.: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
					return null;
				}
			}else{
				Statistics.addExternalApiHit("ioBroker loadDevice ERROR");
				Statistics.addExternalApiTime("ioBroker loadDevice ERROR", tic);
				String error = JSON.getStringOrDefault(result, "error", "");
				if (error.contains("401") || error.contains("java.security") || error.contains("javax.net.ssl")){
					Debugger.println("IoBrokerConnector - loadDeviceData FAILED with msg.: " + result, 1);
				}
				return null;
			}
		}
	}

	@Override
	public boolean setDeviceState(SmartHomeDevice device, String state, String stateType){
		long tic = System.currentTimeMillis();
		String iobId = device.getId(); 
		if (Is.nullOrEmpty(iobId)){
			return false;
		}else{
			//set command overwrite?
			JSONObject setCmds = device.getCustomCommands();
			//System.out.println("setCmds: " + setCmds);		//DEBUG
			if (Is.notNullOrEmpty(setCmds)){
				String newState = SmartHomeDevice.getStateFromCustomSetCommands(state, stateType, setCmds);
				if (newState != null){
					state = newState;
				}
				//System.out.println("state: " + state);		//DEBUG
				
			//check deviceType to find correct set command
			}else{
				//TODO: change/adapt?
				//compare: 'loadDeviceData' -> 'SmartHomeDevice.convertAnyStateToGeneralizedState'
			}
		}
		String cmdUrl = URLBuilder.getStringP20(this.host + "/set/",
				"", iobId,
				"?value=", state
		);
		//System.out.println("URL: " + cmdUrl); 				//DEBUG
		JSONObject response = httpGET(cmdUrl);
		//System.out.println("RESPONSE: " + response);		//DEBUG
		if (Connectors.httpSuccess(response) && response.containsKey("value")){
			//String returnVal = JSON.getString(response, "value");		//check if it is identical to requested value?
			Statistics.addExternalApiHit("ioBroker setDeviceState");
			Statistics.addExternalApiTime("ioBroker setDeviceState", tic);
			return true;
		}else{
			Statistics.addExternalApiHit("ioBroker setDeviceState ERROR");
			Statistics.addExternalApiTime("ioBroker setDeviceState ERROR", tic);
			Debugger.println("IoBrokerConnector interface error in 'setDeviceState': " + response, 1);
			return false;
		}
	}

	//---- below you will find parts of the interface that have not been implemented for this connector ----

	@Override
	public Map<String, SmartHomeDevice> getDevices(){
		return null;
		/*
		//TODO: this is not enough - currently I see no way to get only relevant devices automatically
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
		*/
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
	public Map<String, Set<String>> getBufferedDeviceNamesByType(){
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean setDeviceStateMemory(SmartHomeDevice device, String stateMemory){
		// TODO Auto-generated method stub
		return true;
	}

	//------------- ioBroker specific helper methods --------------
	
	//build device from JSON response
	/*
	private SmartHomeDevice buildDeviceFromResponse(JSONObject hubDevice){
		//TODO: implement ... if possible ... data is very complex and has to be splitted into categories with same format ...
		return null;
	}
	*/
}
