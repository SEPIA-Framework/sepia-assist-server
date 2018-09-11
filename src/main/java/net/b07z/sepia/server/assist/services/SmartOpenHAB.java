package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.Is;

public class SmartOpenHAB implements ServiceInterface {
	
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
	public ServiceInfo getInfo(String language) {
		//type
		ServiceInfo info = new ServiceInfo(Type.REST, Content.apiInterface, false);
		
		//Parameters:
		
		//required
		Parameter p1 = new Parameter(PARAMETERS.SMART_DEVICE)
				.setRequired(true)
				.setQuestion("smartdevice_1a");
		info.addParameter(p1);
		
		//optional
		Parameter p2 = new Parameter(PARAMETERS.ACTION, Action.Type.toggle);
		Parameter p3 = new Parameter(PARAMETERS.SMART_DEVICE_VALUE, "");
		Parameter p4 = new Parameter(PARAMETERS.ROOM, "");
		info.addParameter(p2).addParameter(p3).addParameter(p4);
		
		//Answers (these are the default answers, you can add a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer("smartdevice_0c")		//a default success answer, usually more specific answers will be set depending on the action
			.addFailAnswer("smartdevice_0b")		//an error occurred or the services crashed (serious fail)
			.addOkayAnswer("smartdevice_0a")		//everything went as planned but gave no result (soft fail, e.g. no permission or no data etc.)
			.addCustomAnswer("setDeviceToState", setDeviceToState)
			;
		info.addAnswerParameters("device", "state"); 	//variables used inside answers: <1>, <2>, ...
		
		return info;
	}
	//collect extra answers for better overview
	static final String setDeviceToState = "smartdevice_2a";
	static final String notAllowed = "smartdevice_0d";
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, getInfo(nluResult.language));
		
		//check if we know an OpenHAB server
		if (Is.nullOrEmpty(Config.openhab_host)){
			service.setStatusOkay();
			return service.buildResult();
		}
				
		//check user role 'smarthomeguest' for this skill (because it controls devices in the server's network)
		if (!nluResult.input.user.hasRole(Role.smarthomeguest)){
			service.setStatusOkay();
			service.setCustomAnswer(notAllowed);			//fail with "not allowed" answer
			return service.buildResult();
		}
		
		//get required parameters
		Parameter device = nluResult.getRequiredParameter(PARAMETERS.SMART_DEVICE);
		//get optional parameters
		Parameter action = nluResult.getOptionalParameter(PARAMETERS.ACTION, "");
		Parameter deviceValue = nluResult.getOptionalParameter(PARAMETERS.SMART_DEVICE_VALUE, "");
		Parameter room = nluResult.getOptionalParameter(PARAMETERS.ROOM, "");
		
		//in theory the service can't fail here anymore ^^
		service.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = service.buildResult();
		
		//JSON.printJSONpretty(result.resultJson);
		return result;
	}

}
