package net.b07z.sepia.server.assist.services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.InterpretationChain;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
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
public class SentenceConnect implements ServiceInterface{
	
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
	public ServiceInfo getInfo(String language) {
		//type
		ServiceInfo info = new ServiceInfo(Type.other, Content.redirect, false);
		
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
	private static final String askSentence = "sentences_0a";	//"<direct>Wie lautet der Satz?";
	private static final String successAns = "<silent>";
	private static final String failAns = "sentences_1a";		//"<direct>Fehlgeschlagen";
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, getInfo(""));
		
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
				nluResult.input.inputType = "question";
				nluResult.input.text = s;
				nluResult.input.textRaw = s;
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
					api.addAction(ACTIONS.QUEUE_CMD);
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", thisRes.cmdSummary);
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
		ServiceResult result = api.buildResult();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
