package net.b07z.sepia.server.assist.apis;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.apis.API;
import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.InterpretationChain;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class that handles NPS feedback
 * 
 * @author Florian Quirin
 *
 */
public class SentenceConnect implements ApiInterface{
	
	//---data---
	public static String getButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "Befehle verwalten";
		}else{
			return "Manage commands";
		}
	}
	//----------
	
	@Override
	public ApiInfo getInfo(String language) {
		//type
		ApiInfo info = new ApiInfo(Type.other, Content.redirect, false);
		
		//Parameters:
		//required
		Parameter p1 = new Parameter(PARAMETERS.SENTENCES)
				.setRequired(true)
				.setQuestion(askSentence);
		info.addParameter(p1);
		
		//Answers:
		info.addSuccessAnswer(successAns)
			.addFailAnswer(failAns);
		
		return info;
	}
	private static final String askSentence = "<direct>Wie lautet der Satz?";
	private static final String successAns = "<silent>";
	private static final String failAns = "<direct>Fehlgeschlagen";
	
	@Override
	public ApiResult getResult(NluResult nluResult) {
		//initialize result
		API api = new API(nluResult, getInfo(""));
		
		//get interview parameters
		JSONObject sentenceJson = nluResult.getRequiredParameter(PARAMETERS.SENTENCES).getData();
		JSONArray sentencesArray = (JSONArray) sentenceJson.get(InterviewData.ARRAY);
		
		//get background parameters
		String reply = nluResult.getParameter(PARAMETERS.REPLY);
		
		Debugger.println("cmd: sentence connect: " + sentencesArray, 2);		//debug
		
		if (sentencesArray == null || sentencesArray.isEmpty()){
			api.status = "fail";
			
		}else{
			//Normalizer_Interface normalizer = Config.input_normalizers.get(api.language);
			
			for (Object o : sentencesArray){
				String s = (String) o;
				//TODO: making a clean input would be better
				nluResult.input.clearParameterResultStorage();
				nluResult.input.input_type = "question";
				nluResult.input.text = s;
				nluResult.input.text_raw = s;
				//norm - we don't need this, its in the interpretation chain
				//nluResult.input.text = normalizer.normalize_text(s);
				
				//interpret
				NluResult thisRes = new InterpretationChain()
						.setSteps(Config.nluInterpretationSteps).getResult(nluResult.input);
				//System.out.println("cmd_sum: " + thisRes.cmd_summary); 	//DEBUG
				
				//push - filter no_results and chained sentence_connect commands (to prevent endless loop)
				if (!thisRes.getCommand().equals(CMD.SENTENCE_CONNECT) 
					&& !thisRes.getCommand().equals(CMD.NO_RESULT) && !thisRes.getCommand().equals(CMD.RESULT_REDIRECT)
						){
					//build action
					api.actionInfo_add_action(ACTIONS.QUEUE_CMD);
					api.actionInfo_put_info("info", "direct_cmd");
					api.actionInfo_put_info("cmd", thisRes.cmd_summary);
					//api.actionInfo_put_info("options", JSON.make(ACTIONS.SKIP_TTS, true));
				}
				//use new result for next command? TODO: I think we need some context handling here
				//nluResult = thisRes;
			}
			
			//reply - silent or custom
			if (!reply.isEmpty()){
				reply = AnswerTools.handleUserAnswerSets(reply);
				api.setCustomAnswer(reply);
			}
			
			api.status = "success";
		}
		
		//finally build the API_Result
		ApiResult result = api.build_API_result();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
