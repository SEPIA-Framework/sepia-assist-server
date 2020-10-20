package net.b07z.sepia.server.assist.services;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.Room;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.assist.smarthome.SmartHomeHub;
import net.b07z.sepia.server.assist.tools.StringCompare;
import net.b07z.sepia.server.assist.tools.StringCompare.StringCompareResult;
import net.b07z.sepia.server.core.assistant.ACTIONS;
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
				+ SmartDevice.lightRegEx_de + "|"
				+ SmartDevice.heaterRegEx_de + "|"
				+ SmartDevice.tvRegEx_de + "|"
				+ SmartDevice.musicPlayerRegEx_de + "|"
				+ SmartDevice.fridgeRegEx_de + "|"
				+ SmartDevice.ovenRegEx_de + "|"
				+ SmartDevice.coffeeMakerRegEx_de + "|"
				+ SmartDevice.rollerShutterRegEx_de + "|"
				+ SmartDevice.powerOutletRegEx_de + "|"
				+ SmartDevice.sensorRegEx_de + "|"
				+ SmartDevice.genericDeviceRegEx_de + "|"
				+ "^smart( |)home|"
				+ "(smart( |)home|geraet(e|)|sensor(en|))( |)(control|kontrolle|steuer(ung|n)|status|zustand)"
				+ "", LANGUAGES.DE);
		info.setCustomTriggerRegX(""
				+ SmartDevice.lightRegEx_en + "|"
				+ SmartDevice.heaterRegEx_en + "|"
				+ SmartDevice.tvRegEx_en + "|"
				+ SmartDevice.musicPlayerRegEx_en + "|"
				+ SmartDevice.fridgeRegEx_en + "|"
				+ SmartDevice.ovenRegEx_en + "|"
				+ SmartDevice.coffeeMakerRegEx_en + "|"
				+ SmartDevice.rollerShutterRegEx_en + "|"
				+ SmartDevice.powerOutletRegEx_en + "|"
				+ SmartDevice.sensorRegEx_en + "|"
				+ SmartDevice.genericDeviceRegEx_en + "|"
				+ "^smart( |)home|"
				+ "(smart( |)home|device|sensor) (control|stat(us|e))"
				+ "", LANGUAGES.EN);
		//info.setCustomTriggerRegXscoreBoost(2);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//required
		Parameter p1 = new Parameter(PARAMETERS.SMART_DEVICE)
				.setRequired(true)
				.setQuestion(askDevice);
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
			.addCustomAnswer("notAllowed", notAllowed)
			.addCustomAnswer("notWithAdmin", notWithAdmin)
			.addCustomAnswer("notYetControllable", notYetControllable)
			.addCustomAnswer("noDeviceMatchesFound", noDeviceMatchesFound)
			.addCustomAnswer("actionNotPossible", actionNotPossible)
			.addCustomAnswer("actionCurrentlyNotWorking", actionCurrentlyNotWorking)
			.addCustomAnswer("askStateValue", askStateValue)
			.addCustomAnswer("askFirstOfMany", askFirstOfMany)
			.addCustomAnswer("okDoNothing", okDoNothing)
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
	private static final String notWithAdmin = "default_not_with_admin_0a";
	private static final String notYetControllable = "smartdevice_0e";
	private static final String noDeviceMatchesFound = "smartdevice_0f";
	private static final String actionNotPossible = "default_not_possible_0a";
	private static final String actionCurrentlyNotWorking = "error_0a";
	private static final String askDevice = "smartdevice_1a";
	private static final String askRoom = "smartdevice_1c";
	private static final String askStateValue = "smartdevice_1d";
	private static final String askFirstOfMany = "default_ask_first_of_many_0a";
	private static final String okDoNothing = "ok_0a";
	
	@Override
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//check user role 'smarthomeguest' for this skill (because it controls devices in the server's network)
		if (nluResult.input.user.hasRole(Role.superuser)){
			//allow or not?
			
			//add button that links to help
			service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
			service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-docs/wiki/Create-and-Edit-Users");
			service.putActionInfo("title", "Info: Create Users");
			
			service.setStatusOkay();
			service.setCustomAnswer(notWithAdmin);			//"soft"-fail with "not allowed" answer
			return service.buildResult();
			
		}else if (!nluResult.input.user.hasRole(Role.smarthomeguest) && !nluResult.input.user.hasRole(Role.smarthomeadmin)){
			//add button that links to help
			service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
			service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-docs/wiki/Smart-Home-Controls");
			service.putActionInfo("title", "Info: Smart Home Setup");
			
			service.setStatusOkay();
			service.setCustomAnswer(notAllowed);			//"soft"-fail with "not allowed" answer
			return service.buildResult();
		}
		
		//TODO: can/should we check if the user is in the same network? (proper proxy forwarding?)
		
		//check if we know a Smart Home HUB (e.g. openHAB, FHEM, internal, etc.)
		SmartHomeHub smartHomeHUB = SmartHomeHub.getHubFromSeverConfig();
		if (smartHomeHUB == null){
			service.setStatusOkay(); 				//"soft"-fail (no error just missing info)
			return service.buildResult();
		}
		
		//get required parameters:
		Parameter device = nluResult.getRequiredParameter(PARAMETERS.SMART_DEVICE);
		String deviceType = device.getValueAsString();
		String deviceTag = JSON.getStringOrDefault(device.getData(), InterviewData.SMART_DEVICE_TAG, "");
		boolean isDeviceNameKnownButNoType = false;
		if (Is.typeEqual(deviceType, SmartDevice.Types.unknown)){
			//unknown type with known tag/name?
			if (Is.nullOrEmpty(deviceTag)){
				//Abort - THIS should NEVER happen!
				service.setStatusFail();
				service.setCustomAnswer(noDeviceMatchesFound);
				return service.buildResult();
			}else{
				isDeviceNameKnownButNoType = true;
			}
		}
		String deviceTypeLocal = JSON.getStringOrDefault(device.getData(), InterviewData.VALUE_LOCAL, deviceType);
		int deviceNumber = JSON.getIntegerOrDefault(device.getData(), InterviewData.ITEM_INDEX, Integer.MIN_VALUE);
		
		//get optional parameters:
		Parameter action = nluResult.getOptionalParameter(PARAMETERS.ACTION, "");
		
		Parameter deviceValue = nluResult.getOptionalParameter(PARAMETERS.SMART_DEVICE_VALUE, "");	//a number of type plain, percent, temperature, ...
		String targetSetValue = deviceValue.getValueAsString();
		String targetValueType = JSON.getStringOrDefault(deviceValue.getData(), 
				InterviewData.SMART_DEVICE_VALUE_TYPE, SmartHomeDevice.StateType.number_plain.name());
		
		//Default user temperature unit - used during state adaptation in set
		//String userPrefTempUnit = (String) nluResult.input.getCustomDataObject("prefTempUnit");
				
		Parameter room = nluResult.getOptionalParameter(PARAMETERS.ROOM, "");
		String roomType = room.getValueAsString();
		String roomTypeLocal = JSON.getStringOrDefault(room.getData(), InterviewData.VALUE_LOCAL, roomType);
		int roomNumber = JSON.getIntegerOrDefault(room.getData(), InterviewData.ITEM_INDEX, Integer.MIN_VALUE);
		//String roomTag = JSON.getStringOrDefault(room.getData(), InterviewData.ROOM_TAG, "");
		//Client local site/position/room
		Object deviceLocalSite = nluResult.input.getCustomDataObject(NluInput.DATA_DEVICE_LOCAL_SITE);
		if (Is.nullOrEmpty(roomType) && deviceLocalSite != null && deviceLocalSite.getClass().equals(JSONObject.class)){
			//{"name":"livingroom","index":"","location":"home","type":"room","updates":"off"}
			JSONObject dlsJson = (JSONObject) deviceLocalSite;
			if (JSON.getString(dlsJson, "location").equals("home") && JSON.getString(dlsJson, "type").equals("room")){
				roomType = JSON.getStringOrDefault(dlsJson, "name", "");
				//should be something else than "unassigned"
				if (Is.notNullOrEmpty(roomType) && !Is.typeEqual(roomType, Room.Types.unassigned)){
					roomNumber = JSON.getIntegerOrDefault(dlsJson, "index", Integer.MIN_VALUE);
					roomTypeLocal = Room.getLocal(roomType, nluResult.language);
				}else{
					roomType = "";
				}
			}
		}
		
		//get background parameters
		String reply = nluResult.getParameter(PARAMETERS.REPLY);	//a custom reply (defined via Teach-UI)
		
		//check if device is supported
		/*if (Is.typeEqual(deviceType, SmartDevice.Types....)){
			service.setStatusOkay();
			service.setCustomAnswer(notYetControllable);	//"soft"-fail with "not yet controllable" answer
			return service.buildResult();
		}*/
		
		//find device - NOTE: the HUB implementation is responsible for caching the data
		Map<String, Object> filters = new HashMap<>();
		//if (Is.notNullOrEmpty(deviceName)) filters.put(SmartHomeDevice.FILTER_NAME, deviceName);  //not supported by all HUBs! Need to filter ourself
		if (!isDeviceNameKnownButNoType) filters.put(SmartHomeDevice.FILTER_TYPE, deviceType);
		if (Is.notNullOrEmpty(roomType)) filters.put(SmartHomeDevice.FILTER_ROOM, roomType);
		if (roomNumber != Integer.MIN_VALUE){
			filters.put(SmartHomeDevice.FILTER_ROOM_INDEX, Integer.toString(roomNumber));		//NOTE: we use String because actually room-index is not restricted to numbers
		}
		filters.put("limit", -1);
		List<SmartHomeDevice> matchingDevices = smartHomeHUB.getFilteredDevicesList(filters);
		
		//abort with no result?
		if (matchingDevices == null){
			Debugger.println(SmartHomeHub.class.getSimpleName() + " failed to get devices! Connection to smart home HUB might be broken.", 1);		//debug
			service.setStatusFail(); 						//"hard"-fail (probably HUB connection error)
			return service.buildResult();
		}
		
		//check device name if type was unknown
		if (isDeviceNameKnownButNoType && !matchingDevices.isEmpty()){
			matchingDevices = SmartHomeDevice.findDevicesWithMatchingTagIgnoreCase(matchingDevices, deviceTag);
		}
			
		//check device index number
		if (!matchingDevices.isEmpty()){
			if (deviceNumber != Integer.MIN_VALUE){
				matchingDevices = SmartHomeDevice.findDevicesWithNumberInName(matchingDevices, deviceNumber);
			}
		}
		
		//have found any?
		if (matchingDevices.isEmpty()){
			Debugger.println(SmartHomeHub.class.getSimpleName() + " failed to find any match for "
					+ "device: " + deviceType + ", index: " + deviceNumber + ", room: " + roomType, 1);	//debug
			service.setStatusOkay();
			service.setCustomAnswer(noDeviceMatchesFound);	//"soft"-fail with "no matching devices found" answer
			return service.buildResult();
		}
		
		//find the best match using the device tag (name)
		SmartHomeDevice selectedDevice;
		int matchesN = matchingDevices.size();
		boolean didTagMatch = false;
		String bestTagMatch = null;
		int bestTagMatchScore = 0;
		//do we know the tag already?
		if (isDeviceNameKnownButNoType){
			didTagMatch = true;
			bestTagMatchScore = 100;
			bestTagMatch = deviceTag;
		
		//we try to match the tag first
		}else if (Is.notNullOrEmpty(deviceTag)){
			//can we find a known device name in the extracted device tag
			if (matchesN > 1){
				Map<String, String> possibleTagsMap = new HashMap<>();
				for (SmartHomeDevice shd : matchingDevices){
					String nameTag = shd.getName();
					possibleTagsMap.put(SmartHomeDevice.getBaseName(nameTag), nameTag);		//NOTE: we assume device index is already applied here				
				}
				//System.out.println("deviceTag: " + deviceTag); 							//DEBUG
				//System.out.println("possibleTags: " + possibleTagsMap.values()); 			//DEBUG
				StringCompareResult scr = StringCompare.findMostSimilarMatch(
						deviceTag, possibleTagsMap.keySet(), nluResult.input.language
				);
				bestTagMatchScore = scr.getResultPercent();
				bestTagMatch = possibleTagsMap.get(scr.getResultString());
				//System.out.println("Best " + bestTagMatchScore + "% tag: " + bestTagMatch); 		//DEBUG
				if (bestTagMatchScore == 100){
					//set as new deviceTag
					deviceTag = bestTagMatch;
				}
			}
			//find exact tag match
			List<SmartHomeDevice> matchingDevicesWithSameTag = SmartHomeDevice.findDevicesWithMatchingTagIgnoreCase(matchingDevices, deviceTag);
			int matchesWithTagN = matchingDevicesWithSameTag.size();
			if (matchesWithTagN > 0){
				matchingDevices = matchingDevicesWithSameTag;
				didTagMatch = true;
				matchesN = matchesWithTagN;
			}
		}
		
		//multiple results but no room?
		if (matchesN > 1 && roomType.isEmpty()){
			//RETURN with question
			service.resultInfoPut("device", deviceTypeLocal);
			service.setIncompleteAndAsk(PARAMETERS.ROOM, askRoom);
			return service.buildResult();
			
		//run through all? get best result?
		}else{
			if (didTagMatch && matchesN == 1){
				//this should be the correct device
				selectedDevice = matchingDevices.get(0);
			}else if (matchesN == 1){
				//this is just a guess (potentially correct tag, room and index)
				selectedDevice = matchingDevices.get(0);
				//teach UI button
				service.addAction(ACTIONS.BUTTON_TEACH_UI);
				service.putActionInfo("info", JSON.make(
						"input", nluResult.input.textRaw,
						"service", CMD.SMARTDEVICE
				));
			}else{
				//it is not 100% clear which device we should take
				//TODO: we simplify and take only best match if possible - we should check singular/plural, trigger all or ask for top 3
				if (bestTagMatchScore > 0){
					List<SmartHomeDevice> bestMatchingDevices = SmartHomeDevice.findDevicesWithMatchingTagIgnoreCase(matchingDevices, bestTagMatch);
					if (Is.notNullOrEmpty(bestMatchingDevices)){
						matchingDevices = bestMatchingDevices;
						matchesN = bestMatchingDevices.size();
					}
				}
				//selectedDevice = matchingDevices.get(0);
				int confirmState = service.getConfirmationStatusOf("use_first_device");
				if (confirmState == 0){
					//ASK FIRST
					service.resultInfoPut("device", matchingDevices.get(0).getName());
					service.confirmActionOrParameter("use_first_device", askFirstOfMany);
					//teach UI button
					service.addAction(ACTIONS.BUTTON_TEACH_UI);
					service.putActionInfo("info", JSON.make(
							"input", nluResult.input.textRaw,
							"service", CMD.SMARTDEVICE
					));
					return service.buildResult();
				}else if (confirmState == 1){
					//OK
					selectedDevice = matchingDevices.get(0);
				}else{
					//NO selection, abort - TODO: improve
					service.setStatusOkay();
					service.setCustomAnswer(okDoNothing);	//"soft"-fail with "ok" answer and just do nothing
					return service.buildResult();
				}
			}
		}
		//reassign roomType from device?
		if (roomType.isEmpty()){
			String selectedDeviceRoom = selectedDevice.getRoom();
			if (Is.notNullOrEmpty(selectedDeviceRoom)){
				roomType = selectedDeviceRoom;
				roomTypeLocal = Room.getLocal(roomType, service.language);
			}
		}
		String selectedDeviceState = selectedDevice.getState();
		String selectedDeviceStateType = selectedDevice.getStateType();
		
		//assign selected device name - NOTE: we remove info in brackets
		String selectedDeviceName = selectedDevice.getName().trim();	//cannot be null?
		selectedDeviceName = SmartHomeDevice.getCleanedUpName(selectedDeviceName);
		service.resultInfoPut("device", selectedDeviceName);		//TODO: or use deviceTypeLocal? - If the name is not the same language this might sound strange
		
		//ACTIONS
		
		String actionValue = action.getValueAsString().replaceAll("^<|>$", "").trim(); 		//TODO: NOTE! There is an inconsistency in parameter format with device and room here
		Debugger.println("cmd: smartdevice, action: " + actionValue + ", device: " + deviceType + ", room: " + roomType, 2);		//debug
		
		//response info
		boolean hasRoom = !roomTypeLocal.isEmpty();		//NOTE: to be precise this means "has a room with localized name?"
		if (hasRoom){
			String roomIndex = selectedDevice.getRoomIndex();
			if (Is.notNullOrEmpty(roomIndex)){
				service.resultInfoPut("room", roomTypeLocal + " " + roomIndex);
			}else{
				service.resultInfoPut("room", roomTypeLocal);
			}
		}else{
			service.resultInfoPut("room", "");
		}
		
		//Convert TOGGLE and ON (with value) to specific action
		if (!targetSetValue.isEmpty() && (actionIs(actionValue, Action.Type.toggle) || actionIs(actionValue, Action.Type.on))){
			actionValue = Action.Type.set.name();
		}
		if (actionIs(actionValue, Action.Type.toggle)){
			if (Is.notNullOrEmpty(selectedDeviceState) && !selectedDeviceState.equals("0") && (selectedDeviceState.matches("\\d+") 
					|| Is.typeEqualIgnoreCase(selectedDeviceState, SmartHomeDevice.State.on))){
				actionValue = Action.Type.off.name();
			}else{
				actionValue = Action.Type.on.name();
			}
		}
		//TODO: OPEN and CLOSE = ON and OFF ... this might be wrong for some devices (see roller shutter below)
		
		//SHOW
		if (actionIs(actionValue, Action.Type.show)){
			//response info
			service.resultInfoPut("state", SmartHomeDevice.getStateLocal(selectedDeviceState, service.language));
			//System.out.println("type: " + stateType); 		//DEBUG
			//answer
			if (hasRoom){
				service.setCustomAnswer(showDeviceStateWithRoom);
			}else{
				service.setCustomAnswer(showDeviceState);
			}
			
		//ON
		}else if (actionIs(actionValue, Action.Type.on)){
			String targetState;
			boolean hasStateAlready = false;
			if (Is.typeEqual(deviceType, SmartDevice.Types.roller_shutter)){
				targetState = SmartHomeDevice.State.open.name();
				hasStateAlready = Is.notNullOrEmpty(selectedDeviceState) && selectedDeviceState.equalsIgnoreCase(targetState); 		//NOTE: we skip the 100 check here because HUBs don't agree if 100 is open or closed
			}else{
				targetState = SmartHomeDevice.State.on.name();
				hasStateAlready = Is.notNullOrEmpty(selectedDeviceState) && (selectedDeviceState.equals("100") || selectedDeviceState.equalsIgnoreCase(targetState));
			}
			
			//already on?
			if (hasStateAlready){
				//response info
				service.resultInfoPut("state", SmartHomeDevice.getStateLocal(selectedDeviceState, service.language));
				//answer
				if (hasRoom){
					service.setCustomAnswer(showDeviceStateWithRoom);
				}else{
					service.setCustomAnswer(showDeviceState);
				}
			//request state
			}else{
				boolean setSuccess = smartHomeHUB.setDeviceState(selectedDevice, targetState, SmartHomeDevice.StateType.text_binary.name());
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
					service.setStatusFail(); 						//"hard"-fail (probably HUB connection error)
					service.setCustomAnswer(actionCurrentlyNotWorking);
					return service.buildResult();
				}
			}
		
		//OFF	
		}else if (actionIs(actionValue, Action.Type.off)){
			String targetState;
			boolean hasStateAlready = false;
			if (Is.typeEqual(deviceType, SmartDevice.Types.roller_shutter)){
				targetState = SmartHomeDevice.State.closed.name();
				hasStateAlready = Is.notNullOrEmpty(selectedDeviceState) && selectedDeviceState.equalsIgnoreCase(targetState); 		//NOTE: we skip the 100 check here because HUBs don't agree if 100 is open or closed
			}else{
				targetState = SmartHomeDevice.State.off.name();	//TODO: depending on device 0 might be ON
				hasStateAlready = Is.notNullOrEmpty(selectedDeviceState) && (selectedDeviceState.equals("0") || selectedDeviceState.equalsIgnoreCase(targetState));
			}
			
			//already off?
			if (hasStateAlready){
				//response info
				service.resultInfoPut("state", SmartHomeDevice.getStateLocal(selectedDeviceState, service.language));
				//answer
				if (hasRoom){
					service.setCustomAnswer(showDeviceStateWithRoom);
				}else{
					service.setCustomAnswer(showDeviceState);
				}
			//request state
			}else{
				boolean setSuccess = smartHomeHUB.setDeviceState(selectedDevice, targetState, SmartHomeDevice.StateType.text_binary.name());
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
					service.setStatusFail(); 						//"hard"-fail (probably HUB connection error)
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
				if (Is.notNullOrEmpty(selectedDeviceState) && selectedDeviceState.equalsIgnoreCase(targetSetValue)){
					//response info
					service.resultInfoPut("state", SmartHomeDevice.getStateLocal(selectedDeviceState, service.language));
					//answer
					if (hasRoom){
						service.setCustomAnswer(showDeviceStateWithRoom);
					}else{
						service.setCustomAnswer(showDeviceState);
					}
				//request state
				}else{
					//adapt state and type
					if (Is.notNullOrEmpty(selectedDeviceStateType)){
						try{
							SimpleEntry<String, String> adaptedStateAndType = SmartHomeDevice.adaptToDeviceStateTypeOrFail(
									targetSetValue, targetValueType, 
									deviceType, selectedDeviceStateType, 
									nluResult.input
							);
							targetSetValue = adaptedStateAndType.getValue();
							targetValueType = adaptedStateAndType.getKey();
						}catch (Exception e){
							Debugger.printStackTrace(e, 3);
							//action not supported or makes no sense
							service.setStatusOkay();
							service.setCustomAnswer(actionNotPossible);			//"soft"-fail with "action not possible" answer
							return service.buildResult();
						}
					}else{
						if (Is.typeEqual(targetValueType, SmartHomeDevice.StateType.number_plain)){
							targetValueType = SmartHomeDevice.makeSmartTypeAssumptionForPlainNumber(SmartDevice.Types.valueOf(deviceType));
						}
					}
					//send
					//System.out.println("send to device: " + targetSetValue + " - " + targetValueType);		//DEBUG
					boolean setSuccess = smartHomeHUB.setDeviceState(selectedDevice, targetSetValue, targetValueType);
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
						service.setStatusFail(); 						//"hard"-fail (probably HUB connection error)
						service.setCustomAnswer(actionCurrentlyNotWorking);
						return service.buildResult();
					}
				}
			}
			
		//NOT POSSIBLE
		}else{
			//response info
			service.resultInfoPut("state", SmartHomeDevice.getStateLocal(selectedDeviceState, service.language));
			
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
		
		//custom reply?
		if (!reply.isEmpty()){
			reply = AnswerTools.handleUserAnswerSets(reply);
			service.setCustomAnswer(reply);
		}
		
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
	
}
