package net.b07z.sepia.server.assist.interviews;

import java.util.ArrayList;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.apis.API;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.Confirm;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Interview that builds itself according to info data.
 * 
 * @author Florian Quirin
 *
 */
public class AbstractInterview implements Interview_Interface{
		
	//info
	String command;							//command connected to this interview
	ArrayList<ApiInterface> services;		//the services used (order matters!)
		
	@Override
	public void setCommand(String command) {
		this.command = command;
	}
	@Override
	public void setServices(ArrayList<ApiInterface> services){
		this.services = services;
	}
	
	@Override
	public InterviewInfo getInfo(String language) {
		//check command
		if (command == null || command.isEmpty()){
			throw new RuntimeException(DateTime.getLogDate() + " ERROR - AbstractInterview / getInfo() - NO COMMAND CONNECTED!");
		}
		//check services and try to load from mapping if necessary
		if (services == null){
			throw new RuntimeException(DateTime.getLogDate() + " ERROR - AbstractInterview / getInfo() - NO SERVICES ADDED!");
		}
		//first service wins
		InterviewInfo iInfo = new InterviewInfo(command, services.get(0).getInfo(language));	//use the MASTER service to get info
		iInfo.setServices(services);
		return iInfo;
	}
	
	@Override
	public InterviewResult getMissingParameters(NluResult NLU_result) {
		//make interview assistant
		Interview assistant = new Interview(NLU_result);
		
		//Get info
		InterviewInfo iInfo = getInfo(NLU_result.language);
		
		//required parameters
		for (Parameter p : iInfo.requiredParameters){
			String input = assistant.getParameterInput(p);
			
			//check parameter
			if (input.isEmpty() && !assistant.isFinal(p.getName())){
				//empty means ask at least once, max 3 times, after that abort
				ApiResult question = assistant.ask(p);
				return new InterviewResult(question);
			
			}else if (!assistant.isFinal(p.getName())){
				ApiResult comment = assistant.buildParameterOrComment(p, iInfo);
				//null is the expected 'all good' result ^^
				if (comment != null){
					return new InterviewResult(comment);
				}
			}
			//System.out.println("Parameter: " + assistant.nlu_result.get_parameter(p.getName())); 	//debug
		}
		
		//optional parameters - they need to be validated and checked for given default values
		boolean gotEmptyOptionals = false;
		for (Parameter p : iInfo.optionalParameters){
			String input = assistant.getParameterInput(p);
			
			//check parameter
			if (input.isEmpty()){
				//empty means check for default
				input = p.getDefaultValue().toString();
				p.setInput(input);
			}
			boolean isFinal = assistant.isFinal(p.getName());
			if (!isFinal && !input.isEmpty()){
				ApiResult comment = assistant.buildParameterOrComment(p, iInfo);
				if (comment != null){
					return new InterviewResult(comment);
				}
			}else if (!isFinal){
				gotEmptyOptionals = true; 	//TODO: fits here now?
			}
			//System.out.println("Parameter: " + assistant.nlu_result.get_parameter(p.getName())); 	//debug
		}
		
		//check if some combination of optional parameters requires a choice
		if (gotEmptyOptionals){
			int i = -1;
			if (iInfo.listOfRequiredChoices != null && !iInfo.listOfRequiredChoices.isEmpty()){
				for (ArrayList<Parameter> reqChoices : iInfo.listOfRequiredChoices){
					i++;
					if (reqChoices == null || reqChoices.isEmpty()){
						continue;
					}
					//check if all of the required choices are empty
					boolean allChoicesEmpty = true;
					for (Parameter reqChoiceP : reqChoices){
						if (assistant.isFinal(reqChoiceP.getName())){
							allChoicesEmpty = false;
							break;
						}
					}
					//... and if they are ask for the first
					if (allChoicesEmpty){
						Parameter choiceToAskFor = reqChoices.get(0);
						if (choiceToAskFor != null){
							//update question?
							String newQuestion = iInfo.listOfChoiceQuestions.get(i); 
							if (newQuestion != null && !newQuestion.isEmpty()){
								choiceToAskFor.setQuestion(newQuestion);
							}
							//interrupt loop and return question
							ApiResult question = assistant.ask(choiceToAskFor);
							return new InterviewResult(question);
						}
					}
				}
			}
		}
		
		//check dynamic parameters
		Set<String> dynamicParameters = assistant.getDynamicParameters();
		for (String dp : dynamicParameters){
			
			String input = NLU_result.getParameter(dp);
			boolean isFinal = assistant.isFinal(dp);
			if (!isFinal && !input.isEmpty()){
				Parameter p = null;
				
				//confirmation parameter?
				if (dp.startsWith(Confirm.PREFIX)){
					p = new Parameter(dp);
					p.setHandler(PARAMETERS.CONFIRMATION)
					 .setInput(input);
				}

				if (p != null){
					//System.out.println("dynamic parameter build: " + dynamicParameters); 		//DEBUG
					ApiResult comment = assistant.buildParameterOrComment(p, iInfo);
					if (comment != null){
						return new InterviewResult(comment);
					}
				}
			}
		}
		
		//"anything else" check for optional parameters
		if (gotEmptyOptionals){
			//TODO: ask for all optional parameters with one question if there are some of them empty
		}
		
		//interview done
		assistant.isFinished = true;
		
		InterviewResult iResult = new InterviewResult(assistant);
		iResult.setInterviewInfo(iInfo);
		
		return iResult;
	}
	
	@Override
	public ApiResult getServiceResults(InterviewResult interviewResult){
		InterviewInfo iInfo = interviewResult.getInterviewInfo();
		Interview assistant = new Interview(interviewResult);
				
		//get single service results
		ArrayList<ApiResult> results = assistant.getServiceResults(iInfo.getServices());
		
		//LEGACY SUPPORT: if service is tagged as 'stand-alone' simply take the result
		if ((results.size() == 1) && results.get(0).getApiInfo().worksStandalone){
			//System.out.println("STAND-ALONE SERVICE-MODULE RESULT: " + results.get(0).getResultJSON()); 					//debug
			return results.get(0);
		}
		
		//build result - cards are collected from services, answer(s) (parameters) too, the rest is build here 
		API servicesResult = new API(assistant.nlu_result);
		String status = "";
		String customAnswer = "";
		JSONArray cards = new JSONArray();
		JSONArray actions = new JSONArray();
		JSONObject resultInfo = new JSONObject();
		int n = 0;
		// - collect
		for (ApiResult r : results){
			//first service is the MASTER - get answer parameters ...
			if (n == 0){
				//the MASTER service MUST supply these parameters:
				resultInfo = r.getResultInfo();
				status = r.getStatus();
				//the MASTER can take over full control here using the "stand-alone" features:
				if (status.equals(API.INCOMPLETE)){
					Parameter p = r.getIncompleteParameter();
					if (p != null){
						ApiResult question = assistant.ask(p);
						//an answer should be able to use data and actions as well, so copy them over from result
						JSONArray resultCards = r.getCardInfo();
						if (resultCards != null && !resultCards.isEmpty()){
							JSON.addAll(question.getCardInfo(), resultCards);
						}
						JSONArray resultActions = r.getActionInfo();
						if (resultActions != null && !resultActions.isEmpty()){
							JSON.addAll(question.getActionInfo(), resultActions);
						}
						return question;
					}else{
						Debugger.println("AbstractInterview - used status 'incomplete' without setting incomplete parameter! CMD: " + assistant.nlu_result.getCommand(), 3);
					}
				}
				customAnswer = r.getCustomAnswerWorkpiece();
				actions = r.getActionInfo();
				//transfer more data from master (default stuff like language from input will be lost though during API.build)
				servicesResult.more = r.getMore();
			}
			
			//collect cards
			if (!r.getCardInfo().isEmpty()){
				JSON.addAll(cards, r.getCardInfo());
				//JSON.add(cards, r.getCardInfo().get(0));		//.get(0) is the JSONObject we are interested in, it is just packed in the Array
			}
			
			n++;
			//System.out.println("SERVICE RESULT " + n + ": " + r.getResultJSON()); 		//debug
		}
		// - adjust states
		if (!cards.isEmpty()){
			servicesResult.cardInfo = cards;
			servicesResult.hasCard = true;
		}
		
		// - Answer and resultInfo
		//resultInfo
		servicesResult.resultInfo_addAll(resultInfo);
		//answer:
		String ansTag;
		if (!customAnswer.isEmpty()){
			ansTag = customAnswer;
		}else if (status.equals(API.SUCCESS)){
			ansTag = iInfo.getSuccessAnswer();
		}else if (status.equals(API.OKAY)){
			ansTag = iInfo.getOkayAnswer();
		}else{
			ansTag = iInfo.getFailAnswer();
		}
		boolean isSilent = false;
		if (ansTag.startsWith("<silent>")){
			isSilent = true;
			ansTag = ansTag.replaceFirst("^<silent>", "").trim();
		}
		if (!ansTag.isEmpty()){
			ArrayList<String> ansParams = iInfo.getAnswerParameters();
			Object[] aps = new Object[ansParams.size()];
			for (int i=0; i<aps.length; i++){
				aps[i] = servicesResult.resultInfo_get(ansParams.get(i));
			}
			servicesResult.answer = Config.answers.getAnswer(assistant.nlu_result, ansTag, aps);
			if (isSilent){
				servicesResult.answer_clean = "<silent>";
			}else{
				servicesResult.answer_clean = Converters.removeHTML(servicesResult.answer);
			}
			//clean HTML-answer of vocalSmilies
			servicesResult.answer = AnswerTools.cleanHtmlAnswer(servicesResult.answer);
		}else{
			//no answer
			servicesResult.answer = "";
			servicesResult.answer_clean = "";
		}

		// - Action
		if (actions == null || !actions.isEmpty()){
			//servicesResult.actionInfo_add_action(ACTIONS.OPEN_CARDS);
			servicesResult.actionInfo = actions;
			servicesResult.hasAction = true;
		}
		// - HTML info (can this be considered deprecated?)
		// . . .
		
		//TODO: add success criteria
		if (!cards.isEmpty() || status.equals(API.SUCCESS)){
			servicesResult.status = API.SUCCESS;
		}
		
		//clean up to minimize data stream
		servicesResult.removeParameter(PARAMETERS.FINAL); 		//TODO: replace with "all" tag?
		
		//finally build the meta API_Result
		ApiResult result = servicesResult.build_API_result();
				
		//System.out.println("INTERVIEW-MODULE RESULT: " + result.getResultJSON()); 					//debug
		return result;
	}
}
