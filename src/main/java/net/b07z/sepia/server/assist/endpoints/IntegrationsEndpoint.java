package net.b07z.sepia.server.assist.endpoints;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
			//stats
			Statistics.addOtherApiHit("Integrations endpoint");
			Statistics.addOtherApiTime("Integrations endpoint", 1);
			
			//create user
			User user = new User(null, token);
			
			//integration request
			return integrationsMap.getOrDefault(integration, (req, res, f, u, p) -> {
				//Not found
				return SparkJavaFw.returnPathNotFound(request, response);
			}).request(request, response, fun, user, params);
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
			}
			
			//Build interface
			String hubName = params.getString("hubName");
			String hubHost = params.getString("hubHost");
			SmartHomeHub shh = null;
			if (Is.notNullOrEmpty(hubHost) && Is.notNullOrEmpty(hubName)){
				if (Config.smarthome_hub_host.equalsIgnoreCase(hubHost)){
					//use server data including auth. etc. if its the system HUB
					shh = SmartHomeHub.getHubFromSeverConfig();
				}else{
					//TODO: how can we validate if this is a correct address BEFORE calling it? - FOR NOW: we prevent this call
					//shh = SmartHomeHub.getHub(hubName, hubHost);	//NOTE: we might add 'shh.setAuthenticationInfo' here at some point
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "host address is unknown to server and call has been blocked! Please set 'smarthome_hub_host' in your server settings first."
					).toJSONString(), 200);
				}
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
				Map<String, SmartHomeDevice> devicesMap = shh.getDevices(null, deviceTypeFilter, null);
				if (Is.nullOrEmptyMap(devicesMap)){
					//FAIL
					return SparkJavaFw.returnResult(request, response, JSON.make(
							"result", "fail", 
							"error", "no devices found or failed to contact HUB"
					).toJSONString(), 200);
				}
				//convert to JSONArray
				JSONArray devicesArray = new JSONArray();
				for (Map.Entry<String, SmartHomeDevice> entry : devicesMap.entrySet()){
					SmartHomeDevice data = entry.getValue();
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
							"error", ("one or more attributes could not be set! Success: " + goodN + " of " + expectedGood + " - Did you 'register' SEPIA already?")
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
				String stateValue = JSON.getStringOrDefault(stateJson, "value", null);
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
