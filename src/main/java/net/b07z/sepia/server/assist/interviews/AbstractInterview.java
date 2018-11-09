package net.b07z.sepia.server.assist.interviews;

import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.Confirm;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
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
public class AbstractInterview implements InterviewInterface{
		
	//info
	String command;						//command connected to this interview
	List<ServiceInterface> services;		//the services used (order matters!)
		
	@Override
	public void setCommand(String command) {
		this.command = command;
	}
	@Override
	public void setServices(List<ServiceInterface> services){
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
	public InterviewResult getMissingParameters(NluResult nluResult) {
		//make interview assistant
		Interview assistant = new Interview(nluResult);
		
		//Get info
		InterviewInfo iInfo = getInfo(nluResult.language);
		
		//required parameters
		for (Parameter p : iInfo.requiredParameters){
			String input = assistant.getParameterInput(p);
			
			//check parameter
			if (input.isEmpty() && !assistant.isFinal(p.getName())){
				//empty means ask at least once, max 3 times, after that abort
				ServiceResult question = assistant.ask(p);
				return new InterviewResult(question);
			
			}else if (!assistant.isFinal(p.getName())){
				ServiceResult comment = assistant.buildParameterOrComment(p, iInfo);
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
				ServiceResult comment = assistant.buildParameterOrComment(p, iInfo);
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
				for (List<Parameter> reqChoices : iInfo.listOfRequiredChoices){
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
							ServiceResult question = assistant.ask(choiceToAskFor);
							return new InterviewResult(question);
						}
					}
				}
			}
		}
		
		//check dynamic parameters
		Set<String> dynamicParameters = assistant.getDynamicParameters();
		for (String dp : dynamicParameters){
			
			String input = nluResult.getParameter(dp);
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
					ServiceResult comment = assistant.buildParameterOrComment(p, iInfo);
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
	public ServiceResult getServiceResults(InterviewResult interviewResult){
		InterviewInfo iInfo = interviewResult.getInterviewInfo();
		Interview assistant = new Interview(interviewResult);
		
		//get single service results
		List<ServiceResult> results = assistant.getServiceResults(iInfo.getServices());
		
		//LEGACY SUPPORT: if service is tagged as 'stand-alone' simply take the result
		if ((results.size() == 1) && results.get(0).getApiInfo().worksStandalone){
			//System.out.println("STAND-ALONE SERVICE-MODULE RESULT: " + results.get(0).getResultJSON()); 					//debug
			return results.get(0);
		}

		//BUILD COLLECTED RESULT - cards are collected from services, answer(s) (parameters) too, the rest is build here
		
		//TODO: if there is only one service per command (which is usually the case) this leads to double-execution of build result ...
		
		//We build a new result but keep the apiInfo data from master service
		ServiceInfo servicesInfo;
		if (!results.isEmpty()){
			//Get master data and adapt
			servicesInfo = results.get(0).getApiInfo();
			servicesInfo.setServiceType(Type.systemModule);
			servicesInfo.setContentType(Content.apiInterface);
			servicesInfo.setWorksStandalone(false);
			
			//If there was a service redirect/switch inside 'getServiceResults' we loose the correct InterviewInfo! So we need to rebuild it:
			ServiceInfo primaryResultServiceInfo = results.get(0).getApiInfo();
			String primaryResultIntendedCommand = primaryResultServiceInfo.intendedCommand;
			if (primaryResultIntendedCommand != null && !primaryResultIntendedCommand.equals(command)){
				/*
				System.out.println("Intended command of primary result: " + primaryResultIntendedCommand); 		//debug
				System.out.println("Previously set command: " + command); 										//debug
				System.out.println("Result customAnswerMap: ");			 										//debug
				Debugger.printMap(primaryResultServiceInfo.customAnswerMap);									//debug
				*/
				iInfo = new InterviewInfo(primaryResultIntendedCommand, primaryResultServiceInfo);	//use the MASTER service to get info
				iInfo.setServices(services);
			}
		}else{
			servicesInfo = new ServiceInfo(Type.systemModule, Content.apiInterface, false);
		}
		ServiceBuilder servicesResult = new ServiceBuilder(assistant.nluResult, servicesInfo);
		
		String status = "";
		String customAnswer = "";
		JSONArray cards = new JSONArray();
		JSONArray actions = new JSONArray();
		JSONObject resultInfo = new JSONObject();
		int n = 0;
		// - collect
		for (ServiceResult r : results){
			//first service is the MASTER - get answer parameters ...
			if (n == 0){
				//the MASTER service MUST supply these parameters:
				resultInfo = r.getResultInfo();
				status = r.getStatus();
				//the MASTER can take over full control here using the "stand-alone" features:
				if (status.equals(ServiceBuilder.INCOMPLETE)){
					Parameter p = r.getIncompleteParameter();
					if (p != null){
						ServiceResult question = assistant.ask(p);
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
						Debugger.println("AbstractInterview - used status 'incomplete' without setting incomplete parameter! CMD: " + assistant.nluResult.getCommand(), 3);
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
		servicesResult.resultInfoAddAll(resultInfo);
		//answer:
		String ansTag;
		if (!customAnswer.isEmpty()){
			ansTag = customAnswer;
		}else if (status.equals(ServiceBuilder.SUCCESS)){
			ansTag = iInfo.getSuccessAnswer();
		}else if (status.equals(ServiceBuilder.OKAY)){
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
			List<String> ansParams = iInfo.getAnswerParameters();
			Object[] aps = new Object[ansParams.size()];
			for (int i=0; i<aps.length; i++){
				aps[i] = servicesResult.resultInfoGet(ansParams.get(i));
			}
			servicesResult.answer = Config.answers.getAnswer(assistant.nluResult, ansTag, aps);
			if (isSilent){
				servicesResult.answerClean = "<silent>";
			}else{
				servicesResult.answerClean = Converters.removeHTML(servicesResult.answer);
			}
			//clean HTML-answer of vocalSmilies
			servicesResult.answer = AnswerTools.cleanHtmlAnswer(servicesResult.answer);
		}else{
			//no answer
			servicesResult.answer = "";
			servicesResult.answerClean = "";
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
		if (!cards.isEmpty() || status.equals(ServiceBuilder.SUCCESS)){
			servicesResult.status = ServiceBuilder.SUCCESS;
		}
		
		//clean up to minimize data stream
		servicesResult.removeParameter(PARAMETERS.FINAL); 		//TODO: replace with "all" tag?
		
		//finally build the meta ServiceResult - this is the result of all collected services to one command (can be more than 1)
		ServiceResult result = servicesResult.buildResult();
				
		//System.out.println("INTERVIEW-MODULE RESULT: " + result.getResultJSON()); 					//debug
		return result;
	}
}
