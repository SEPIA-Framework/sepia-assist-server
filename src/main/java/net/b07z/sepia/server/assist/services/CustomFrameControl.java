package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.ActionBuilder;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A simple service that can open custom frames/views optionally with some data.
 * 
 * @author Florian Quirin
 *
 */
public class CustomFrameControl implements ServiceInterface {
	
	private static final String DEMO_FRAME_URL = "<assist_server>/views/demo-view.html";
	private static final JSONObject DEMO_FRAME_DATA = new JSONObject();  //Note: not data required for default events
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Öffne das benutzerdefiniert Demo Frame");
			
		//OTHER
		}else{
			samples.add("Open the custom demo frame");
		}
		return samples;
	}
	
	//Basic service setup:
	
	@Override
	public ServiceAnswers getAnswersPool(String language) {
		ServiceAnswers answerPool = new ServiceAnswers(language);
		
		//Build German answers
		if (language.equals(LANGUAGES.DE)){
			answerPool
				.addAnswer(missingFilePath, 	0, "Mir fehlt leider der Dateipfad für dieses Frame.")
				.addAnswer(missingActionData, 	0, "Mir fehlen leider die Daten für diese Frame Aktion.")
			;
			return answerPool;
		
		//Or default to English
		}else{
			answerPool	
				.addAnswer(missingFilePath, 	0, "I'm missing a file path for this frame, sorry.")
				.addAnswer(missingActionData, 	0, "I'm missing the data for this frame action, sorry.")
			;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult
	private static final String missingFilePath = ServiceAnswers.ANS_PREFIX + CMD.FRAME_CONTROL + ".missing_file_path_0a";
	private static final String missingActionData = ServiceAnswers.ANS_PREFIX + CMD.FRAME_CONTROL + ".missing_action_data_0a";

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.systemModule, Content.data, false);
		
		//Command
		info.setIntendedCommand(CMD.FRAME_CONTROL);
				
		String DE = LANGUAGES.DE;
		String EN = LANGUAGES.EN;
		
		//Regular expression triggers
		info.setCustomTriggerRegX("^("
					+ "oeffne (.* |)(?<url>demo_frame)|"			//TODO: move this demo somewhere else?
					+ "_demo_frame_ setze daten auf (?<data>.*)|"
					+ "(_demo_frame_ |)frame schließen"
				+ ")$", DE);
		info.setCustomTriggerRegX("^("
					+ "open (.* |)(?<url>demo_frame)|"
					+ "_demo_frame_ set data to (?<data>.*)|"
					+ "(_demo_frame_ |)close frame"
				+ ")$", EN);
		info.setCustomTriggerRegXscoreBoost(2);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//required
		Parameter p1 = new Parameter(PARAMETERS.ACTION)
			.setRequired(true)
			.setQuestion("default_ask_action_0a");
		
		//optional
		Parameter p2 = new Parameter(PARAMETERS.URL); 				//NOTE: these are GenericEmptyParameters and ...
		Parameter p3 = new Parameter(PARAMETERS.DATA);				//... can only be set via direct commands (e.g. Teach-UI) or RegExp named groups
		info.addParameter(p1).addParameter(p2).addParameter(p3);
		
		//Default answers
		info.addSuccessAnswer("ok_0b")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a")
			.addCustomAnswer("missingFilePath", missingFilePath);
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get required parameters
		Parameter actionP = nluResult.getRequiredParameter(PARAMETERS.ACTION);
		String action = actionP.getValueAsString().replaceAll("^<|>$", "").trim();
		
		//get optional parameters
		Parameter urlP = nluResult.getOptionalParameter(PARAMETERS.URL, "");
		String frameUrl = urlP.getValueAsString().trim();
		
		Parameter dataP = nluResult.getOptionalParameter(PARAMETERS.DATA, "");
		JSONObject data = dataP.getValueAsJson(); 		//NOTE: needs to be a JSON object!
		
		//get background parameters
		String reply = nluResult.getParameter(PARAMETERS.REPLY);	//a custom reply (defined via Teach-UI)
		
		boolean isActionOpen = (Is.typeEqual(action, Action.Type.show) || Is.typeEqual(action, Action.Type.on)
				|| Is.typeEqual(action, Action.Type.open));
		boolean isActionClose = (Is.typeEqual(action, Action.Type.remove) || Is.typeEqual(action, Action.Type.off)
				|| Is.typeEqual(action, Action.Type.close));
		boolean isActionSet = Is.typeEqual(action, Action.Type.set);
		
		//build result
		ActionBuilder actionBuilder = new ActionBuilder(service);
		
		if (isActionOpen){
			if (frameUrl.isEmpty()){
				//FAIL - missing frame file
				
				//Help
				service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
				service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-docs/wiki#customizing-sepia");
				service.putActionInfo("title", "SEPIA Docs");
				
				service.setStatusFail();
				service.setCustomAnswer(missingFilePath);
				
				//build the API_Result
				ServiceResult result = service.buildResult();
				return result;
				
			}else if (frameUrl.equals("demo_frame")){
				//DEMO
				frameUrl = DEMO_FRAME_URL;
				data = DEMO_FRAME_DATA;
			}
			//OPEN
			if (data == null) data = new JSONObject();
			JSON.put(data, "pageUrl", frameUrl);	//make sure URL is set
			actionBuilder.addAction(
				ACTIONS.OPEN_FRAMES_VIEW, JSON.make(
					"info", data
				)
			);
		}else if (isActionClose){
			//CLOSE
			actionBuilder.addAction(ACTIONS.CLOSE_FRAMES_VIEW, new JSONObject());
		
		}else if (isActionSet){
			if (data == null || data.isEmpty()){
				//FAIL - missing action data
				
				//Help
				service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
				service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-docs/wiki#customizing-sepia");
				service.putActionInfo("title", "SEPIA Docs");
				
				service.setStatusFail();
				service.setCustomAnswer(missingActionData);
				
				//build the API_Result
				ServiceResult result = service.buildResult();
				return result;
			}
			//SET
			actionBuilder.addAction(
				ACTIONS.FRAMES_VIEW_ACTION, JSON.make(
					"info", data
				)
			);
		}
				
		//all good
		service.setStatusSuccess();
		
		//custom reply?
		if (!reply.isEmpty()){
			reply = AnswerTools.handleUserAnswerSets(reply);
			service.setCustomAnswer(reply);
		}
		
		//build the API_Result
		ServiceResult result = service.buildResult();
		return result;
	}
}
