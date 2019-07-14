package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.List;

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
import net.b07z.sepia.server.core.tools.Is;

/**
 * The sentence-connect service can execute multiple other existing commands (non-custom) in a row and supports flexible parameters.
 * It will add all actions to one queue.
 * 
 * @author Florian Quirin
 *
 */
public class SentenceConnect implements ServiceInterface{
	
	//flexible parameter replacements (e.g. for TeachUI)
	public static final String VAR_BASE = "var";
	public static final int VAR_N = 5;	//-> var1, var2 ... varN
	
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
		
		//optional
		//see below at 'background parameters'
		
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
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get interview parameters
		JSONObject sentenceJson = nluResult.getRequiredParameter(PARAMETERS.SENTENCES).getData();
		JSONArray sentencesArray = (JSONArray) sentenceJson.get(InterviewData.ARRAY);
		
		//get background parameters
		String reply = nluResult.getParameter(PARAMETERS.REPLY);	//a reply
		List<String> flexParameters = new ArrayList<>();			//flex parameters
		for (int i=0; i<VAR_N; i++){
			String var = nluResult.getParameter(VAR_BASE + (i+1));
			if (Is.notNullOrEmpty(var)){
				flexParameters.add(var);
			}
		}
		
		Debugger.println("cmd: sentence connect: " + sentencesArray, 2);		//debug
		
		if (sentencesArray == null || sentencesArray.isEmpty()){
			api.status = "fail";
			
		}else{
			//Normalizer_Interface normalizer = Config.input_normalizers.get(api.language);
			int goodResults = 0;
			for (Object o : sentencesArray){
				String s = (String) o;
				
				//Replace flex parameters (variables)
				for (int i=0; i<flexParameters.size(); i++){
					String tag = "<" + VAR_BASE + (i+1) + ">";
					if (s.contains(tag)){
						s = s.replace(tag, flexParameters.get(i));
					}
				}
				
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
				//System.out.println("cmd_sum: " + thisRes.cmdSummary + " - in: " + thisRes.language);		//DEBUG
				
				//push - filter no_results and chained sentence_connect commands (to prevent endless loop)
				if (!thisRes.getCommand().equals(CMD.SENTENCE_CONNECT) 
					&& !thisRes.getCommand().equals(CMD.NO_RESULT) && !thisRes.getCommand().equals(CMD.RESULT_REDIRECT)
						){
					//build action
					api.addAction(ACTIONS.QUEUE_CMD);
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", thisRes.cmdSummary);
					api.putActionInfo("lang", thisRes.language);
					//api.actionInfo_put_info("options", JSON.make(ACTIONS.SKIP_TTS, true));
					goodResults++;
				}
				//use new result for next command? TODO: I think we need some context handling here
				//nluResult = thisRes;
			}
			
			//reply - silent or custom
			if (!reply.isEmpty()){
				reply = AnswerTools.handleUserAnswerSets(reply);
				api.setCustomAnswer(reply);
			}
			
			if (goodResults == 0){
				api.status = "fail";
			}else{
				api.status = "success";
			}
		}
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		//return result.result_JSON.toJSONString();
		return result;
	}

}
