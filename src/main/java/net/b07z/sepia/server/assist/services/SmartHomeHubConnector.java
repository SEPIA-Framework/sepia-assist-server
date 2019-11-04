package net.b07z.sepia.server.assist.services;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.Number;
import net.b07z.sepia.server.assist.parameters.Room;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.assist.smarthome.SmartHomeHub;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * This services uses a local smart home HUB server like openHAB or FHEM (same network as SEPIA server) to control smart home devices.
 * 
 * @author Florian Quirin
 *
 */
public class SmartHomeHubConnector implements ServiceInterface {
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Schalte das Licht im Wohnzimmer ein.");
			samples.add("Heizung im Badezimmer auf 21 Grad bitte.");
			
		//OTHER
		}else{
			samples.add("Switch on the lights in the living room.");
			samples.add("Set the heater in the bath to 21 degrees celsius please.");
		}
		return samples;
	}

	@Override
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.REST, Content.apiInterface, false);
		
		//Command
		String CMD_NAME = CMD.SMARTDEVICE;
		info.setIntendedCommand(CMD_NAME);
		
		//Regular expression triggers:
		info.setCustomTriggerRegX(""
				+ "licht(er|es|)|lampe(n|)|beleuchtung|leuchte(n|)|helligkeit|"
				+ "heiz(er|ungen|ung|koerper|luefter|strahler)|temperatur(regler|en|)|thermostat|"
				+ "(smart( |)home|geraet(e|)|sensor(en|))( |)(control|kontrolle|steuer(ung|n)|status|zustand)"
				+ "", LANGUAGES.DE);
		info.setCustomTriggerRegX(""
				+ "light(s|)|lighting|lamp(s|)|illumination|brightness|"
				+ "heater(s|)|temperature(s|)|thermostat(s|)|"
				+ "(smart( |)home|device|sensor) (control|stat(us|e))"
				+ "", LANGUAGES.EN);
		//info.setCustomTriggerRegXscoreBoost(2);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//required
		Parameter p1 = new Parameter(PARAMETERS.SMART_DEVICE)
				.setRequired(true)
				.setQuestion("smartdevice_1a");
		info.addParameter(p1);
		
		//optional
		Parameter p2 = new Parameter(PARAMETERS.ACTION, Action.Type.toggle); 	//toggle seems to be the most reasonable default action "lights" -> "light on"
		Parameter p3 = new Parameter(PARAMETERS.SMART_DEVICE_VALUE, "");
		Parameter p4 = new Parameter(PARAMETERS.ROOM, "");
		info.addParameter(p2).addParameter(p3).addParameter(p4);
		
		//Answers (these are the default answers, you can add a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer("smartdevice_0c")		//a default success answer, usually more specific answers will be set depending on the action
			.addFailAnswer("smartdevice_0b")		//an error occurred or the services crashed (serious fail)
			.addOkayAnswer("smartdevice_0a")		//everything went as planned but gave no result (soft fail, e.g. no permission or no data etc.)
			.addCustomAnswer("setDeviceToState", setDeviceToState)
			.addCustomAnswer("setDeviceToStateWithRoom", setDeviceToStateWithRoom)
			.addCustomAnswer("showDeviceState", showDeviceState)
			.addCustomAnswer("showDeviceStateWithRoom", showDeviceStateWithRoom)
			.addCustomAnswer("notYetControllable", notYetControllable)
			.addCustomAnswer("noDeviceMatchesFound", noDeviceMatchesFound)
			.addCustomAnswer("actionNotPossible", actionNotPossible)
			.addCustomAnswer("actionCurrentlyNotWorking", actionCurrentlyNotWorking)
			.addCustomAnswer("askStateValue", askStateValue)
			;
		info.addAnswerParameters("device", "room", "state"); 	//variables used inside answers: <1>, <2>, ...
		
		return info;
	}
	//collect extra answers for better overview
	private static final String setDeviceToState = "smartdevice_2a";
	private static final String setDeviceToStateWithRoom = "smartdevice_2b";
	private static final String showDeviceState = "smartdevice_2c";
	private static final String showDeviceStateWithRoom = "smartdevice_2d";
	private static final String notAllowed = "smartdevice_0d";
	private static final String notYetControllable = "smartdevice_0e";
	private static final String noDeviceMatchesFound = "smartdevice_0f";
	private static final String actionNotPossible = "default_not_possible_0a";
	private static final String actionCurrentlyNotWorking = "error_0a";
	private static final String askStateValue = "smartdevice_1d";
	
	@Override
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//check user role 'smarthomeguest' for this skill (because it controls devices in the server's network)
		if (!nluResult.input.user.hasRole(Role.smarthomeguest)){
			service.setStatusOkay();
			service.setCustomAnswer(notAllowed);			//"soft"-fail with "not allowed" answer
			return service.buildResult();
		}
		
		//TODO: can/should we check if the user is in the same network? (proper proxy forwarding?)
		
		//check if we know an OpenHAB server
		SmartHomeHub smartHomeHUB = SmartHomeHub.getHubFromSeverConfig();
		if (smartHomeHUB == null){
			service.setStatusOkay(); 				//"soft"-fail (no error just missing info)
			return service.buildResult();
		}
		
		//get required parameters:
		Parameter device = nluResult.getRequiredParameter(PARAMETERS.SMART_DEVICE);
		String deviceType = device.getValueAsString();
		
		//get optional parameters:
		Parameter action = nluResult.getOptionalParameter(PARAMETERS.ACTION, "");
		
		Parameter deviceValue = nluResult.getOptionalParameter(PARAMETERS.SMART_DEVICE_VALUE, "");	//a number of type plain, percent or temperature (more may be added later)
		String targetSetValue = deviceValue.getValueAsString();
		String targetValueType = JSON.getStringOrDefault(deviceValue.getData(), InterviewData.NUMBER_TYPE, Number.Types.plain.name());
		String targetValueParameterName = PARAMETERS.SMART_DEVICE_VALUE;		//required to generalize type ... see below
		
		Parameter room = nluResult.getOptionalParameter(PARAMETERS.ROOM, "");
		String roomType = room.getValueAsString();
		
		//TODO: implement in future:
		String deviceName = null;
		
		//check if device is supported 						TODO: for the test phase we're currently doing lights only
		if (!Is.typeEqual(deviceType, SmartDevice.Types.light)){
			service.setStatusOkay();
			service.setCustomAnswer(notYetControllable);	//"soft"-fail with "not yet controllable" answer
			return service.buildResult();
		}
		
		//find device - we always load a list of all devices (NOTE: the HUB implementation is responsible for caching the data)
		Map<String, SmartHomeDevice> devices = smartHomeHUB.getDevices(deviceName, deviceType, roomType);
		if (devices == null){
			service.setStatusFail(); 						//"hard"-fail (probably HUB connection error)
			return service.buildResult();
		}
		//get all devices with right type and optionally right room
		List<SmartHomeDevice> matchingDevices = SmartHomeDevice.getMatchingDevices(devices, deviceType, roomType, -1);
		
		//have found any?
		if (matchingDevices.isEmpty()){
			service.setStatusOkay();
			service.setCustomAnswer(noDeviceMatchesFound);	//"soft"-fail with "no matching devices found" answer
			return service.buildResult();
		}
		
		//keep only the first match for now - TODO: improve
		SmartHomeDevice selectedDevice = matchingDevices.get(0);
		if (roomType.isEmpty()){
			String selectedDeviceRoom = selectedDevice.getRoom();
			if (Is.notNullOrEmpty(selectedDeviceRoom)){
				roomType = selectedDeviceRoom;
			}
		}
		String state = selectedDevice.getState();
		String stateType = selectedDevice.getStateType();
		
		//ACTIONS for LIGHTS
		
		String actionValue = action.getValueAsString().replaceAll("^<|>$", "").trim(); 		//TODO: NOTE! There is an inconsistency in parameter format with device and room here
		Debugger.println("cmd: smartdevice, action: " + actionValue + ", device: " + deviceType + ", room: " + roomType, 2);		//debug
		
		//response info
		service.resultInfoPut("device", SmartDevice.getLocal(deviceType, service.language));
		boolean hasRoom = !roomType.isEmpty();
		if (hasRoom){
			service.resultInfoPut("room", Room.getLocal(roomType, service.language));
		}else{
			service.resultInfoPut("room", "");
		}
		
		//Convert TOGGLE and ON (with value) to specific action
		if (!targetSetValue.isEmpty() && (actionIs(actionValue, Action.Type.toggle) || actionIs(actionValue, Action.Type.on))){
			actionValue = Action.Type.set.name();
		}
		if (actionIs(actionValue, Action.Type.toggle)){
			if (Is.notNullOrEmpty(state) && !state.equals("0") && (state.matches("\\d+") || state.toUpperCase().equals(SmartHomeDevice.STATE_ON))){
				actionValue = Action.Type.off.name();
			}else{
				actionValue = Action.Type.on.name();
			}
		}
		
		//SHOW
		if (actionIs(actionValue, Action.Type.show)){
			//response info
			service.resultInfoPut("state", SmartHomeDevice.getStateLocal(state, service.language));
			//answer
			if (hasRoom){
				service.setCustomAnswer(showDeviceStateWithRoom);
			}else{
				service.setCustomAnswer(showDeviceState);
			}
			//response info
			service.resultInfoPut("state", SmartHomeDevice.getStateLocal(state, service.language));
			
		//ON
		}else if (actionIs(actionValue, Action.Type.on)){
			String targetState = SmartHomeDevice.STATE_ON;
			
			//already on?
			if (Is.notNullOrEmpty(state) && !state.equals("0") && (state.matches("\\d+") || state.toUpperCase().equals(targetState))){
				//response info
				service.resultInfoPut("state", SmartHomeDevice.getStateLocal(state, service.language));
				//answer
				if (hasRoom){
					service.setCustomAnswer(showDeviceStateWithRoom);
				}else{
					service.setCustomAnswer(showDeviceState);
				}
			//request state
			}else{
				boolean setSuccess = smartHomeHUB.setDeviceState(selectedDevice, targetState, SmartHomeDevice.STATE_TYPE_TEXT_BINARY);
				if (setSuccess){
					//response info
					service.resultInfoPut("state", SmartHomeDevice.getStateLocal(targetState, service.language));
					//answer
					if (hasRoom){
						service.setCustomAnswer(setDeviceToStateWithRoom);
					}else{
						service.setCustomAnswer(setDeviceToState);
					}
				}else{
					//fail answer
					service.setStatusFail(); 						//"hard"-fail (probably openHAB connection error)
					service.setCustomAnswer(actionCurrentlyNotWorking);
					return service.buildResult();
				}
			}
		
		//OFF	
		}else if (actionIs(actionValue, Action.Type.off)){
			String targetState = SmartHomeDevice.STATE_OFF;
			
			//already off?
			if (Is.notNullOrEmpty(state) && (state.matches("0") || state.toUpperCase().equals(targetState))){
				//response info
				service.resultInfoPut("state", SmartHomeDevice.getStateLocal(state, service.language));
				//answer
				if (hasRoom){
					service.setCustomAnswer(showDeviceStateWithRoom);
				}else{
					service.setCustomAnswer(showDeviceState);
				}
			//request state
			}else{
				boolean setSuccess = smartHomeHUB.setDeviceState(selectedDevice, targetState, SmartHomeDevice.STATE_TYPE_TEXT_BINARY);
				if (setSuccess){
					//response info
					service.resultInfoPut("state", SmartHomeDevice.getStateLocal(targetState, service.language));
					//answer
					if (hasRoom){
						service.setCustomAnswer(setDeviceToStateWithRoom);
					}else{
						service.setCustomAnswer(setDeviceToState);
					}
				}else{
					//fail answer
					service.setStatusFail(); 						//"hard"-fail (probably openHAB connection error)
					service.setCustomAnswer(actionCurrentlyNotWorking);
					return service.buildResult();
				}
			}
	
		//SET
		}else if (actionIs(actionValue, Action.Type.set)){
			//check if we have a value or need to ask
			if (targetSetValue.isEmpty()){ 
				service.setIncompleteAndAsk(PARAMETERS.SMART_DEVICE_VALUE, askStateValue);
				return service.buildResult();
			
			//set
			}else{
				//already set?
				if (Is.notNullOrEmpty(state) && state.toUpperCase().equals(targetSetValue.toUpperCase())){
					//response info
					service.resultInfoPut("state", SmartHomeDevice.getStateLocal(state, service.language));
					//answer
					if (hasRoom){
						service.setCustomAnswer(showDeviceStateWithRoom);
					}else{
						service.setCustomAnswer(showDeviceState);
					}
				//request state
				}else{
					String genStateType = SmartHomeDevice.convertStateType(targetValueParameterName, targetSetValue, targetValueType);
					if (genStateType.equals(SmartHomeDevice.STATE_TYPE_NUMBER_PLAIN)){
						genStateType = SmartHomeDevice.makeSmartTypeAssumptionForPlainNumber(SmartDevice.Types.valueOf(deviceType)); 
					}
					
					boolean setSuccess = smartHomeHUB.setDeviceState(selectedDevice, targetSetValue, genStateType);
					if (setSuccess){
						//response info
						service.resultInfoPut("state", SmartHomeDevice.getStateLocal(targetSetValue, service.language));
						//answer
						if (hasRoom){
							service.setCustomAnswer(setDeviceToStateWithRoom);
						}else{
							service.setCustomAnswer(setDeviceToState);
						}
					}else{
						//fail answer
						service.setStatusFail(); 						//"hard"-fail (probably openHAB connection error)
						service.setCustomAnswer(actionCurrentlyNotWorking);
						return service.buildResult();
					}
				}
			}
			
		//NOT POSSIBLE
		}else{
			//response info
			service.resultInfoPut("state", SmartHomeDevice.getStateLocal(state, service.language));
			
			//action not supported or makes no sense
			service.setStatusOkay();
			service.setCustomAnswer(actionNotPossible);			//"soft"-fail with "action not possible" answer
			return service.buildResult();
		}
		//TODO: missing action implementations
		//increase
		//decrease
		
		//in theory the service can't fail here anymore ^^
		service.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = service.buildResult();
		
		//JSON.printJSONpretty(result.resultJson);
		return result;
	}
	
	//----------- helper methods ------------
	
	/**
	 * Shortcut for: actionValue.equals(actionType.name())
	 * @return true/false
	 */
	private static boolean actionIs(String actionValue, Action.Type actionType){
		return actionValue.equals(actionType.name());
	}
	
	/**
	 * @deprecated
	 * use {@link SmartHomeDevice#getMatchingDevices(Map, String, String, int)}
	 */
	public static List<SmartHomeDevice> getMatchingDevices(Map<String, SmartHomeDevice> devices, String deviceType, String roomType, int maxDevices){
		return SmartHomeDevice.getMatchingDevices(devices, deviceType, roomType, maxDevices);
	}
	/**
	 * @deprecated
	 * use {@link SmartHomeDevice#getStateLocal(String, String)}
	 */
	public static String getStateLocal(String state, String language){
		return SmartHomeDevice.getStateLocal(state, language);
	}
}
