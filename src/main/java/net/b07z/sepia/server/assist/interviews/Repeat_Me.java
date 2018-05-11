package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.apis.API;
import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * A plug-in that simply repeats what the user said.
 * 
 * @author Florian Quirin
 *
 */
public class Repeat_Me implements ApiInterface{
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.other, Content.action, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//get text
		String text = NLU_result.getParameter(PARAMETERS.REPEAT_THIS);
		Debugger.println("cmd: Repeat me, text=" + text, 2);		//debug
		
		//is there something to repeat
		if (text.isEmpty()){
			return NoResult.get(NLU_result);
		}
		
		//get answer
		api.answer = text;
		api.answer_clean = Converters.removeHTML(api.answer);
		
		api.status = "success";
		
		//build the API_Result
		ApiResult result = api.build_API_result();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
