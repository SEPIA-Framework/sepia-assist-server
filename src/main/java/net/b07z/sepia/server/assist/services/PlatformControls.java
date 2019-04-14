package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.ClientFunction;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.CLIENTS.Platform;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A service to trigger client control actions, usually called via direct commands 
 * defined in the teach-UI or by pre-defined system/SDK sentences.
 * Compared to {@link ClientControls} this class supports different actions for each
 * client platform so that you can use e.g. the same command to either call and Android Intent
 * or a browser link. Example: 'open camera'.
 * 
 * @author Florian Quirin
 *
 */
public class PlatformControls implements ServiceInterface{
	
	public static enum FunctionTypes {
		androidIntent,
		iosIntent,
		windowsIntent,
		browserIntent,
		url
	}
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Kamera öffnen.");
			
		//OTHER
		}else{
			samples.add("Open camera.");
		}
		return samples;
	}
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Command
		String CMD_NAME = CMD.PLATFORM_CONTROLS; 		//parameters:	android_fun, ios_fun, browser_fun, device_fun
		info.setIntendedCommand(CMD_NAME);
		
		//NOTE: check ClientControls for examples if you want to add some pre-defined triggers 
		
		//Parameters:
		
		//all optional (for now, because we trigger this with custom functions)
		Parameter p1 = new Parameter(PARAMETERS.ANDROID_FUN);
		Parameter p2 = new Parameter(PARAMETERS.IOS_FUN);
		Parameter p3 = new Parameter(PARAMETERS.BROWSER_FUN);
		Parameter p4 = new Parameter(PARAMETERS.DEVICE_FUN);
		Parameter p5 = new Parameter(PARAMETERS.WINDOWS_FUN);
				
		info.addParameter(p1).addParameter(p2).addParameter(p3).addParameter(p4).addParameter(p5);
		
		//Default answers
		info.addSuccessAnswer("ok_0b")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a");
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//platform data
		Platform platform = CLIENTS.getPlatform(nluResult.input.clientInfo);
		String userDeviceId = nluResult.input.deviceId;
		
		//get parameters
		Parameter androidFunP = nluResult.getOptionalParameter(PARAMETERS.ANDROID_FUN, "");
		String androidFun = androidFunP.getValueAsString();
		
		Parameter iosFunP = nluResult.getOptionalParameter(PARAMETERS.IOS_FUN, "");
		String iosFun = iosFunP.getValueAsString();
		
		Parameter browserFunP = nluResult.getOptionalParameter(PARAMETERS.BROWSER_FUN, "");
		String browserFun = browserFunP.getValueAsString();
		
		Parameter windowsFunP = nluResult.getOptionalParameter(PARAMETERS.WINDOWS_FUN, "");
		String windowsFun = windowsFunP.getValueAsString();
		
		Parameter deviceFunP = nluResult.getOptionalParameter(PARAMETERS.DEVICE_FUN, "");
		String deviceFun = deviceFunP.getValueAsString();
		JSONObject deviceFunJson = null;
		if (Is.notNullOrEmpty(userDeviceId) && !deviceFun.isEmpty()){
			deviceFunJson = JSON.parseString(deviceFun);
		}
		
		//Find best match for function type
		String foundFunString = null;
		if (Is.notNullOrEmpty(deviceFunJson)){
			foundFunString = JSON.getString(deviceFunJson, userDeviceId);
		}
		if (Is.nullOrEmpty(foundFunString)){
			if (platform.equals(Platform.browser)){
				foundFunString = browserFun;
			}else if (platform.equals(Platform.android)){
				foundFunString = androidFun;
			}else if (platform.equals(Platform.ios)){
				foundFunString = iosFun;
			}else if (platform.equals(Platform.windows)){
				foundFunString = windowsFun;
			}
		}
		/* DEBUG
		System.out.println("DeviceId: " + userDeviceId);
		System.out.println("Platform: " + platform);
		System.out.println("Fun: " + foundFunString);
		*/
				
		//This service fails when no device function or platform is found that fits
		if (Is.nullOrEmpty(foundFunString)){
			api.setStatusOkay();
			return api.buildResult();
		
		}else{
			//Tell client to perform this platform action);
			String controlFun = ClientFunction.Type.platformFunction.name();
			JSONObject controlData = JSON.make(
					"platform", platform.toString(),
					"data", foundFunString
			);
			api.addAction(ACTIONS.CLIENT_CONTROL_FUN);
			api.putActionInfo("fun", controlFun);
			api.putActionInfo("controlData", controlData);
			
			//action button
			api.addAction(ACTIONS.BUTTON_CUSTOM_FUN);
			api.putActionInfo("fun", "controlFun;;" + controlFun + ";;" + controlData.toJSONString());
			api.putActionInfo("title", "Button");
			
			//build the API_Result - cannot fail anymore at this point
			api.setStatusSuccess();
			ServiceResult result = api.buildResult();
			return result;
		}
	}
}
