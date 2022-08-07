package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.interviews.InterviewMetaData;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Funny count till 3 to get going with something :-)
 * 
 * @author Florian Quirin
 *
 */
public class FunCountToThree implements ServiceInterface {
	
	//info
	public ServiceInfo getInfo(String language){
		return new ServiceInfo(Type.plain, Content.data, true);
	}

	//result
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		//get parameters
		String number = nluResult.getParameter(PARAMETERS.NUMBER);
		String memory = nluResult.getParameter(PARAMETERS.MEMORY);
		Debugger.println("cmd: funny_count, number=" + number + ", memory=" + memory, 2);		//debug
		
		//check number
		if (number.isEmpty() && nluResult.input.isAnswerToQuestion()){
			//keep counting ^^ - what is the next number?
			if (memory.isEmpty()){
				nluResult.setParameter(PARAMETERS.MEMORY, "1");
				InterviewMetaData metaData = null;	//NOTE: we could add dialogTask 'numbers' or something here
				return AskClient.question("<direct>1", PARAMETERS.NUMBER, metaData, nluResult);
			
			//search biggest number that has been said already and add something
			}else{
				String[] num_array = memory.split("\\|\\|");
				double max = 0.0d;
				for (String num_str : num_array){
					double d = Converters.obj2DoubleOrDefault(num_str, Double.NEGATIVE_INFINITY);
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
					api.answer = Answers.getAnswerString(nluResult, "<direct>3! go go go!");
					api.answerClean = Converters.removeHTML(api.answer);
				}else{
					String new_num = String.valueOf(res).replaceAll("[0]*$", "").replaceAll("\\.$", "");
					//save new num in memory
					nluResult.setParameter(PARAMETERS.MEMORY, memory + "||" + new_num);
					InterviewMetaData metaData = null;
					return AskClient.question("<direct>" + new_num, PARAMETERS.NUMBER, metaData, nluResult);
				}
			}
			
		}else if (number.isEmpty()){
			//start with one
			nluResult.setParameter(PARAMETERS.MEMORY, "1");
			InterviewMetaData metaData = null;
			return AskClient.question("<direct>1", PARAMETERS.NUMBER, metaData, nluResult);
		}
		
		//valid number - use it:
		double d_user = Converters.obj2DoubleOrDefault(number, Double.NEGATIVE_INFINITY);
		
		//bigger or equal to 3 is "go go go"
		if (d_user >= 3.0d){
			//make answer
			api.answer = Answers.getAnswerString(nluResult, "<direct>go go go!");
			api.answerClean = Converters.removeHTML(api.answer);
		
		//else increase number but check what has been said before
		}else{
			String[] num_array = memory.split("\\|\\|");
			double max = d_user;
			for (String num_str : num_array){
				double d = Converters.obj2DoubleOrDefault(num_str, Double.NEGATIVE_INFINITY);
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
				api.answer = Answers.getAnswerString(nluResult, "<direct>3! go go go!");
				api.answerClean = Converters.removeHTML(api.answer);
			}else{
				String new_num = String.valueOf(res).replaceAll("[0]*$", "").replaceAll("\\.$", "").trim();
				//save new num in memory
				nluResult.setParameter(PARAMETERS.MEMORY, memory + "||" + new_num);
				InterviewMetaData metaData = null;
				return AskClient.question("<direct>" + new_num, PARAMETERS.NUMBER, metaData, nluResult);
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
		ServiceResult result = api.buildResult();
				
		//return result_JSON.toJSONString();
		return result;
	}

}
