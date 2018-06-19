package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.interpreters.NluResult;
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
 * A plug-in that simply repeats what the user said.
 * 
 * @author Florian Quirin
 *
 */
public class Repeat_Me implements ServiceInterface{
	
	//info
	public ServiceInfo getInfo(String language){
		return new ServiceInfo(Type.other, Content.action, true);
	}

	//result
	public ServiceResult getResult(NluResult NLU_result){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result);
		
		//get text
		String text = NLU_result.getParameter(PARAMETERS.REPEAT_THIS);
		Debugger.println("cmd: Repeat me, text=" + text, 2);		//debug
		
		//is there something to repeat
		if (text.isEmpty()){
			return NoResult.get(NLU_result);
		}
		
		//get answer
		api.answer = text;
		api.answerClean = Converters.removeHTML(api.answer);
		
		api.status = "success";
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
