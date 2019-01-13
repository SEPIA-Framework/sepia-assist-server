package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Handles the control command before its send to the individual control scripts.
 * 
 * @author Florian Quirin
 *
 */
public class ControlPreprocessor implements ServiceInterface{
	
	//TODO: do a complete rework with integration of all new features and interfaces (Parameters etc.)
	
	//info
	public ServiceInfo getInfo(String language){
		return new ServiceInfo(Type.plain, Content.data, true);
	}

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		//get parameters
		String action = nluResult.getParameter(PARAMETERS.ACTION);
		String type = nluResult.getParameter(PARAMETERS.TYPE);
		String info = nluResult.getParameter(PARAMETERS.INFO);
		String number = nluResult.getParameter(PARAMETERS.NUMBER);
		Debugger.println("cmd: chat, type=" + type + ", action=" + action + ", info=" + info + ", number= " + number, 2);		//debug
		//System.out.println("Control dialog stage: " + NLU_result.input.dialog_stage);
		
		//System.out.println("Last CMD N: " + NLU_result.input.last_cmd_N);
		if (nluResult.input.lastCmdN > 1){
			//abort
			api.answer = Answers.getAnswerString(nluResult, "control_0b");
			api.status = "success";
		}else{
			
			//device control
			if (type.matches("<device.*")){
				
				nluResult.setCommand(CMD.SMARTDEVICE);
				nluResult.setParameter(PARAMETERS.SMART_DEVICE, type);
				nluResult.setParameter(PARAMETERS.ROOM, info);
				nluResult.removeParameter(PARAMETERS.TYPE);
				nluResult.removeParameter(PARAMETERS.INFO);
				return ConfigServices.buildServices(CMD.SMARTDEVICE).get(0).getResult(nluResult); //.smartDevices.getResult(NLU_result);
			}
			
			//app control
			else if (type.matches("<app.*")){
				api.answer = Answers.getAnswerString(nluResult, "control_0a");
				api.status = "success";
			}
			
			//other control
			else {
				//what to control?
				if (!action.isEmpty()){
					//please specify
					api.answer = Answers.getAnswerString(nluResult, "control_0c");
					api.status = "success";
				}	
				/*
				}else if (type.isEmpty() || type.matches("<unknown>.*")){
					return Ask_Client.question("control_1a", PARAMETER.TYPE, NLU_result);
				}
				*/
				else{
					//no command yet
					api.answer = Answers.getAnswerString(nluResult, "control_0a");
					api.status = "success";
				}
			}
		}
		
		//finish:
		
		//get clean answer
		api.answerClean = Converters.removeHTML(api.answer);
		
		//anything else?
		api.context = CMD.CONTROL;				//how do we handle chat contexts? Just like that and do the rest with cmd_summary?
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
				
		//return result_JSON.toJSONString();
		return result;
	}

}
