package net.b07z.sepia.server.assist.endpoints;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.assist.smarthome.SmartHomeHub;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import spark.Request;
import spark.Response;

/**
 * Endpoint to offer interface for external and internal integrations like smart home HUBs.
 * 
 * @author Florian Quirin
 *
 */
public class IntegrationsEndpoint {
	
	//dummy class for integrations
	private interface IntegrationEndpointInterface {
		public String request(Request request, Response response, String fun, User user, RequestParameters params);
	}
	private static Map<String, IntegrationEndpointInterface> integrationsMap = new HashMap<>();
	static {
		//Integrations
		integrationsMap.put("smart-home", (request, response, fun, user, params) -> smartHomeIntegration(request, response, fun, user, params));
	}

	/**
	 * ---INTEGRATIONS---<br>
	 * End-point to handle requests to specific external or internal integrations like smart home HUBs.
	 */
	public static String handle(Request request, Response response){
		//path
		String[] path = request.splat();
		if (path == null || path.length != 2){
			//Not found
			return SparkJavaFw.returnPathNotFound(request, response);
		}
		String integration = path[0];
		String fun = path[1];
		
		//prepare parameters
		RequestParameters params = new RequestPostParameters(request);
		
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response);
		}else{
			//create user
			User user = new User(null, token);
			
			//integration request
			long tic = System.currentTimeMillis();
			String re = integrationsMap.getOrDefault(integration, (req, res, f, u, p) -> {
				//Not found
				return SparkJavaFw.returnPathNotFound(request, response);
			}).request(request, response, fun, user, params);
			
			//stats
			Statistics.addOtherApiHit("Integrations endpoint");
			Statistics.addOtherApiTime("Integrations endpoint", tic);
			
			return re;
		}
	}
	
	//Smart Home Integration
	private static String smartHomeIntegration(Request request, Response response, String fun, User user, RequestParameters params){
		//check role
		if (!user.hasRole(Role.superuser) && !user.hasRole(Role.smarthomeadmin)){
			Debugger.println("Unauthorized access attempt to integration: smart-home - User: " + user.getUserID(), 3);
			return SparkJavaFw.returnNoAccess(request, response);
		}else{
			//Basic info (no HUB data required):
			
			//Config
			if (fun.equalsIgnoreCase("getConfiguration")){
				//respond
				JSONObject msg = JSON.make(
						"result", "success",
						"hubName", Config.smarthome_hub_name,
						"hubHost", Config.smarthome_hub_host
				);
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			
			//Interfaces
			//-load
			}else if (fun.equalsIgnoreCase("getInterfaces")){
				Map<String, SmartHomeHub> interfacesMap = DB.getSmartDevicesDb().loadInterfaces();
				if (interfacesMap == null){
					//FAIL
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "failed to load interfaces. Check assist-server logs for more info."
					).toJSONString(), 200);
				}
				//convert to JSONArray
				JSONArray interfacesArray = new JSONArray();
				for (Map.Entry<String, SmartHomeHub> entry : interfacesMap.entrySet()){
					JSON.add(interfacesArray, entry.getValue().toJson());
				}
				//respond
				JSONObject msg = JSON.make(
						"result", "success", 
						"interfaces", interfacesArray,
						"types", SmartHomeHub.interfaceTypes
				);
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			//-create
			}else if (fun.equalsIgnoreCase("createOrUpdateInterface")){
				JSONObject msg;
				JSONObject interfaceJson = params.getJson("interface");
				if (Is.nullOrEmpty(interfaceJson)){
					msg = JSON.make(
						"result", "fail",
						"error", "Could not create new HUB interface. Missing or invalid 'interface' data."
					);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}
				//System.out.println("createOrUpdateInterface");		//DEBUG
				//JSON.prettyPrint(interfaceJson); 			//DEBUG
				int code = DB.getSmartDevicesDb().addOrUpdateInterface(interfaceJson);
				if (code == 0){
					msg = JSON.make(
						"result", "success",
						"msg", "New HUB interface was stored successfully in database."
					);
				}else{
					msg = JSON.make(
						"result", "fail",
						"code", code,
						"error", "Could not create new interface. See assist-server log for errors."
					);
				}
				//respond
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			//-delete
			}else if (fun.equalsIgnoreCase("removeInterface")){
				JSONObject msg;
				String interfaceId = params.getString("id");
				if (Is.nullOrEmpty(interfaceId)){
					msg = JSON.make(
						"result", "fail",
						"error", "Could not remove HUB interface. Missing or invalid 'id'."
					);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}
				int code = DB.getSmartDevicesDb().removeInterface(interfaceId);
				if (code == 0){
					msg = JSON.make(
						"result", "success",
						"msg", "HUB interface was removed successfully from database."
					);
				}else{
					msg = JSON.make(
						"result", "fail",
						"code", code,
						"error", "Could not remove HUB interface. See assist-server log for errors."
					);
				}
				//respond
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
			
			//Build interface
			String hubName = params.getString("hubName");
			String hubHost = params.getString("hubHost");
			String hubInterfaceId = params.getString("hubInterfaceId");
			SmartHomeHub shh = null;
			if (Is.notNullOrEmpty(hubHost) && Is.notNullOrEmpty(hubName)){
				if (Is.notNullOrEmpty(Config.smarthome_hub_host) && Config.smarthome_hub_host.equalsIgnoreCase(hubHost) 
						&& Is.notNullOrEmpty(Config.smarthome_hub_name) && Config.smarthome_hub_name.equalsIgnoreCase(hubName)){
					//use server data including auth. etc. if its the system HUB
					shh = SmartHomeHub.getHubFromSeverConfig();
				}else{
					//TODO: how can we validate if this is a correct address BEFORE calling it? - FOR NOW: we prevent this call
					// ... we could check the custom interfaces maybe?
					//shh = SmartHomeHub.getHub(hubName, hubHost);	//NOTE: we might add 'shh.setAuthenticationInfo' here at some point
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "HUB name and/or host address are unknown to server! Call has been BLOCKED! Please check 'smarthome_hub_...' in your server settings."
					).toJSONString(), 200);
				}
			}else if (Is.notNullOrEmpty(hubInterfaceId) && fun.equalsIgnoreCase("getDevices")){
				//NOTE: we currently allow this for getDevices only
				shh = DB.getSmartDevicesDb().getCachedInterfaces().get(hubInterfaceId);
			}
			if (shh == null){
				//FAIL
				return SparkJavaFw.returnResult(request, response, JSON.make(
						"result", "fail", 
						"error", "missing or invalid HUB data"
				).toJSONString(), 200);
			}
			
			//GET:
			
			//Devices
			if (fun.equalsIgnoreCase("getDevices")){
				//get devices
				String deviceTypeFilter = params.getString("deviceTypeFilter");
				if (deviceTypeFilter != null && deviceTypeFilter.trim().isEmpty()){
					deviceTypeFilter = null; 	//make sure this is null not empty
				}
				Collection<SmartHomeDevice> devicesList = null;
				
				//we try optimizing here by using 'getFilteredDevicesList'
				Map<String, Object> filters = null;
				if (deviceTypeFilter != null && !deviceTypeFilter.equalsIgnoreCase("all")){
					filters = new HashMap<>();
					filters.put("type", deviceTypeFilter);
				}
				devicesList = shh.getFilteredDevicesList(filters);
				/* alternatively we could load all:
				Map<String, SmartHomeDevice> devicesMap = shh.getDevices();
				if (Is.notNullOrEmptyMap(devicesMap)){
					devicesList = devicesMap.values();
				} */
				
				if (devicesList == null || devicesList.isEmpty()){
					//FAIL
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "no devices found or failed to contact HUB"
					).toJSONString(), 200);
				}
				
				//filter and convert to JSONArray - NOTE: we filter again because 'getFilteredDevicesList' is not guaranteed to filter correctly
				JSONArray devicesArray = new JSONArray();
				for (SmartHomeDevice data : devicesList){
					if (deviceTypeFilter != null){
						String shdType = data.getType();
						if (shdType != null && shdType.equalsIgnoreCase(deviceTypeFilter)){
							JSON.add(devicesArray, data.getDeviceAsJson());
						}
					}else{
						JSON.add(devicesArray, data.getDeviceAsJson());
					}
				}
				//respond
				JSONObject msg = JSON.make(
						"result", "success", 
						"devices", devicesArray
				);
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			
			//SET:
				
			//Register
			}else if (fun.equalsIgnoreCase("registerFramework")){
				//register
				boolean wasRegistered = shh.registerSepiaFramework();
				JSONObject msg;
				if (wasRegistered){
					msg = JSON.make(
						"result", "success",
						"msg", "SEPIA Framework was registered successfully inside smart home HUB."
					);
				}else{
					msg = JSON.make(
						"result", "fail",
						"error", "Could not register SEPIA Framework inside smart home HUB. See assist-server log for errors."
					);
				}
				//respond
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			
			//Attributes
			}else if (fun.equalsIgnoreCase("setDeviceAttributes")){
				JSONObject deviceJson = params.getJson("device");
				JSONObject attributesJson = params.getJson("attributes");
				if (Is.nullOrEmpty(deviceJson) || Is.nullOrEmpty(attributesJson)){
					//FAIL
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "missing 'device' or 'attributes' JSON object"
					).toJSONString(), 200);
				}
				//set attributes
				SmartHomeDevice shd = new SmartHomeDevice().importJsonDevice(deviceJson);
				/*/DEBUG
				JSON.printJSONpretty(deviceJson);
				JSON.printJSONpretty(attributesJson);
				//-----*/
				int goodN = 0;
				for (Object k : attributesJson.keySet()){
					if (shh.writeDeviceAttribute(shd, (String) k, (String) attributesJson.get(k))){
						goodN++;
					}
				}
				int expectedGood = attributesJson.size();
				if (goodN != expectedGood){
					return SparkJavaFw.returnResult(request, response,JSON.make(
							"result", "fail", 
							"error", ("one or more attributes could not be set! Success: " + goodN + " of " + expectedGood 
									+ (shh.requiresRegistration()? " - Did you 'register' SEPIA already?" : " - see server log"))
					).toJSONString(), 200);
				}else{
					//respond
					return SparkJavaFw.returnResult(request, response,JSON.make(
							"result", "success", 
							"msg", ("successfully set " + goodN + " of " + expectedGood + " attributes")
					).toJSONString(), 200);
				}
				
			//State
			}else if (fun.equalsIgnoreCase("setDeviceState")){
				JSONObject deviceJson = params.getJson("device");
				JSONObject stateJson = params.getJson("state");
				if (Is.nullOrEmpty(deviceJson) || Is.nullOrEmpty(stateJson)){
					//FAIL
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "missing 'device' or 'state' JSON object"
					).toJSONString(), 200);
				}
				//set attributes
				String stateValue = Converters.obj2StringOrDefault(stateJson.get("value"), null);
				//String stateValue = JSON.getStringOrDefault(stateJson, "value", null); 	//this can fail for numbers
				String stateType = JSON.getStringOrDefault(stateJson, "type", null);
				if (stateValue == null){
					//FAIL
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "invalid state data for key 'value'"
					).toJSONString(), 200);
				}
				SmartHomeDevice shd = new SmartHomeDevice().importJsonDevice(deviceJson);
				/*/DEBUG
				JSON.printJSONpretty(deviceJson);
				JSON.printJSONpretty(stateJson);
				//-----*/
				boolean setSuccess = shh.setDeviceState(shd, stateValue, stateType);
				if (!setSuccess){
					return SparkJavaFw.returnResult(request, response,JSON.make(
							"result", "fail", 
							"error", "could NOT set new state. Check server log for more info"
					).toJSONString(), 200);
				}else{
					//respond
					return SparkJavaFw.returnResult(request, response,JSON.make(
							"result", "success", 
							"msg", ("successfully set state: " + stateValue)
					).toJSONString(), 200);
				}
				
			//CREATE/DELETE:
				
			//Create new custom device (item)
			}else if (fun.equalsIgnoreCase("createDevice")){
				JSONObject msg;
				JSONObject deviceJson = params.getJson("device");
				if (Is.nullOrEmpty(deviceJson)){
					msg = JSON.make(
						"result", "fail",
						"error", "Could not create new device (item). Missing or invalid 'device' data."
					);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}
				//System.out.println("createDevice");		//DEBUG
				//JSON.prettyPrint(deviceJson); 			//DEBUG
				JSONObject res = DB.getSmartDevicesDb().addOrUpdateCustomDevice(deviceJson);
				int code = JSON.getIntegerOrDefault(res, "code", 2);
				if (code == 0){
					msg = JSON.make(
						"result", "success",
						"data", res,
						"msg", "New device (item) was stored successfully in database."
					);
				}else{
					msg = JSON.make(
						"result", "fail",
						"code", code,
						"error", "Could not create new device (item). See assist-server log for errors."
					);
				}
				//respond
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				
			//Delete custom device (item)
			}else if (fun.equalsIgnoreCase("removeDevice")){
				JSONObject msg;
				String deviceId = params.getString("id");
				if (Is.nullOrEmpty(deviceId)){
					msg = JSON.make(
						"result", "fail",
						"error", "Could not remove device (item). Missing or invalid 'id'."
					);
					return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				}
				int code = DB.getSmartDevicesDb().removeCustomDevice(deviceId);
				if (code == 0){
					msg = JSON.make(
						"result", "success",
						"msg", "Device (item) has been removed from database."
					);
				}else{
					msg = JSON.make(
						"result", "fail",
						"code", code,
						"error", "Could not remove device (item). See assist-server log for errors."
					);
				}
				//respond
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
				
			
			//ERRORS:
				
			//FAIL
			}else{
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "path not mapped for this integration");
				return SparkJavaFw.returnResult(request, response, msg.toJSONString(), 200);
			}
		}
	}
}
