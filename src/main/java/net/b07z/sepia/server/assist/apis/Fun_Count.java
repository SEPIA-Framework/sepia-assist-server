package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Funny count till 3 to get going with something :-)
 * 
 * @author Florian Quirin
 *
 */
public class Fun_Count implements ApiInterface {
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.plain, Content.data, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//get parameters
		String number = NLU_result.getParameter(PARAMETERS.NUMBER);
		String memory = NLU_result.getParameter(PARAMETERS.MEMORY);
		Debugger.println("cmd: funny_count, number=" + number + ", memory=" + memory, 2);		//debug
		
		//check number
		if (number.isEmpty() && NLU_result.input.isAnswerToQuestion()){
			//keep counting ^^ - what is the next number?
			if (memory.isEmpty()){
				NLU_result.setParameter(PARAMETERS.MEMORY, "1");
				return AskClient.question("<direct>1", PARAMETERS.NUMBER, NLU_result);
			
			//search biggest number that has been said already and add something
			}else{
				String[] num_array = memory.split("\\|\\|");
				double max = 0.0d;
				for (String num_str : num_array){
					double d = Converters.obj2Double(num_str);
					if (d > max){
						max = d;
					}
				}
				//add something random but stay below 3
				double add = 0.0d;
				if (max >= 2.0){
					add = (3.0-max)/ ((double) Converters.random_10_100_1000()) * ((double) Converters.randomInt(1, 11));
				}
				//add something random  but stay below 2
				else {
					add = ((double) Converters.randomInt(1, 10)) / ((double) Converters.random_10_100_1000());
				}
				double res = Math.ceil((max + add)*1000.0)/1000.0;
				
				if (res >= 3.0d){
					//make answer
					api.answer = Config.answers.getAnswer(NLU_result, "<direct>3! go go go!");
					api.answer_clean = Converters.removeHTML(api.answer);
				}else{
					String new_num = String.valueOf(res).replaceAll("[0]*$", "").replaceAll("\\.$", "");
					//save new num in memory
					NLU_result.setParameter(PARAMETERS.MEMORY, memory + "||" + new_num);
					return AskClient.question("<direct>" + new_num, PARAMETERS.NUMBER, NLU_result);
				}
			}
			
		}else if (number.isEmpty()){
			//start with one
			NLU_result.setParameter(PARAMETERS.MEMORY, "1");
			return AskClient.question("<direct>1", PARAMETERS.NUMBER, NLU_result);
		}
		
		//valid number - use it:
		double d_user = Converters.obj2Double(number);
		
		//bigger or equal to 3 is "go go go"
		if (d_user >= 3.0d){
			//make answer
			api.answer = Config.answers.getAnswer(NLU_result, "<direct>go go go!");
			api.answer_clean = Converters.removeHTML(api.answer);
		
		//else increase number but check what has been said before
		}else{
			String[] num_array = memory.split("\\|\\|");
			double max = d_user;
			for (String num_str : num_array){
				double d = Converters.obj2Double(num_str);
				if (d > max){
					max = d;
				}
			}
			//add something random but stay below 3
			double add = 0.0d;
			if (max >= 2.0){
				add = (3.0-max)/ ((double) Converters.random_10_100_1000()) * ((double) Converters.randomInt(1, 11));
			}
			//add something random  but stay below 2
			else {
				add = ((double) Converters.randomInt(1, 10)) / ((double) Converters.random_10_100_1000());
			}
			double res = Math.ceil((max + add)*1000.0)/1000.0;
			
			if (res >= 3.0d){
				//make answer
				api.answer = Config.answers.getAnswer(NLU_result, "<direct>3! go go go!");
				api.answer_clean = Converters.removeHTML(api.answer);
			}else{
				String new_num = String.valueOf(res).replaceAll("[0]*$", "").replaceAll("\\.$", "").trim();
				//save new num in memory
				NLU_result.setParameter(PARAMETERS.MEMORY, memory + "||" + new_num);
				return AskClient.question("<direct>" + new_num, PARAMETERS.NUMBER, NLU_result);
			}
		}
		
		//make action
		//api.actionInfo_add_action(ACTIONS.OPEN_INFO);  
		//api.hasAction = true;
		
		//build html
		//api.htmlInfo = "";
		//api.hasInfo = true;	
		
		api.status = "success";		//kind of success ^^
				
		//finally build the API_Result
		ApiResult result = api.build_API_result();
				
		//return result_JSON.toJSONString();
		return result;
	}

}
