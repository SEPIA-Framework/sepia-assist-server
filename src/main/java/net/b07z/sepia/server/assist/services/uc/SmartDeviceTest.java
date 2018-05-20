package net.b07z.sepia.server.assist.services.uc;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.KodiControl;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Control smart devices.
 * 
 * @author Florian Quirin
 *
 */
public class SmartDeviceTest implements ServiceInterface{
	
	//info
	public ServiceInfo getInfo(String language){
		return new ServiceInfo(Type.other, Content.data, true);
	}

	//result
	public ServiceResult getResult(NluResult NLU_result){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result);
		
		//get parameters
		String device = NLU_result.getParameter(PARAMETERS.SMART_DEVICE);
		String action = NLU_result.getParameter(PARAMETERS.ACTION);
		String room = NLU_result.getParameter(PARAMETERS.SMART_LOCATION);
		String number = NLU_result.getParameter(PARAMETERS.NUMBER);
		Debugger.println("cmd: smartDevice, device=" + device + ", action=" + action + ", place= " + room + ", number= " + number, 2);		//debug
		//System.out.println("Last CMD N: " + NLU_result.input.last_cmd_N);
		
		if (NLU_result.input.lastCmdN > 1){
			//abort
			api.answer = Config.answers.getAnswer(NLU_result, "control_0b");
			api.answerClean = Converters.removeHTML(api.answer);
			api.status = "success";
		}else{
		
			//ask for a device
			if (device.isEmpty()){
				return AskClient.question("smartdevice_1a", PARAMETERS.SMART_DEVICE, NLU_result);
			}
			String device_name = device.replaceFirst("<.*?>", "").trim();
			if (device_name.isEmpty()){
				//TODO: this works only for English and German
				device_name = "device";
				if (NLU_result.language.matches("de")){
					device_name = "Gerät";
				}
			}
			
			//ask for an action
			if (action.isEmpty()){
				return AskClient.question("smartdevice_1b", PARAMETERS.ACTION, NLU_result, device_name);
			}
			
			//ask for a room
			if (room.isEmpty()){
				//do we need a room?
				if (!device.matches("<device_other.*")){
					return AskClient.question("smartdevice_1c", PARAMETERS.SMART_LOCATION, NLU_result, device_name);
				}
			}
			
			//ask for number if needed
			if (number.isEmpty()){
				//set needs number
				if (action.equals("<set>")){
					//-check again for a number because users tend to give multiple info although it was not asked
					if (NLU_result.input.isAnswerToQuestion()){
						number = RegexParameterSearch.get_number(NLU_result.input.text);
					}
					//-still empty?
					if (number.isEmpty()){
						return AskClient.question("smartdevice_1d", PARAMETERS.NUMBER, NLU_result, device_name);
					}
				}
			}
			
			//music player?
			if(device.contains("<device_music>")){
				ServiceInterface kodi = new KodiControl();
				return kodi.getResult(NLU_result);
			}
			
			//make answer - if more than one direct answer choose randomly
			api.answer = Config.answers.getAnswer(NLU_result, "smartdevice_0a");
			api.answerClean = Converters.removeHTML(api.answer);
			
			//little html response
			api.htmlInfo = "<div><b>Smart system control: </b><br>" 
							+ "<br><b>Device:</b> " + device.replaceAll("<|>.*", "") 
							+ "<br><b>Action:</b> " + action.replaceAll("<|>", "")
							+ "<br><b>Number:</b> " + number.replaceAll("<|>", "")
							+ "<br><b>Room:</b> " + room.replaceAll("<|>", "")
						+"</div>";
			api.hasInfo = true;	
			
			api.status = "success";
		}
				
		//finally build the API_Result
		ServiceResult result = api.buildResult();
				
		//return result_JSON.toJSONString();
		return result;
		
	}

}
