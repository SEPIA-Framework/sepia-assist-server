package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.ClientFunction;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A service to trigger client control actions, usually called via direct commands 
 * defined in the teach-UI or by pre-defined system/SDK sentences.
 * 
 * @author Florian Quirin
 *
 */
public class ClientControls implements ServiceInterface{
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Einstellungen öffnen.");
			
		//OTHER
		}else{
			samples.add("Open settings.");
		}
		return samples;
	}
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Command
		String CMD_NAME = CMD.CLIENT_CONTROLS;
		info.setIntendedCommand(CMD_NAME);
				
		String DE = LANGUAGES.DE;
		String EN = LANGUAGES.EN;
		
		//Direct-match trigger sentences in different languages:
		/* -- This only works in SDK services (because it is written into user account on upload --
		info.addCustomTriggerSentence("Einstellungen öffnen.", DE)
			;
		info.addCustomTriggerSentence("Open settings.", EN)
			;
		*/
		//Regular expression triggers:
		info.setCustomTriggerRegX("^("
				+ "(.* |)(einstellung(en|)|settings) oeffnen( .*|)|"
				+ "(.* |)always(-| |)on( .*|)|"
				+ "(.* |)(musik|sound|radio) (lauter|leiser)( .*|)|"
				+ "(.* |)lautstaerke( .*|)"
				+ ")$", DE);
		info.setCustomTriggerRegX("^("
				+ "( .*|)open setting(s|)( .*|)|"
				+ "(.* |)always(-| |)on( .*|)|"
				+ "( .*|)(music|sound|radio) (quieter|louder)( .*|)|"
				+ "(.* |)volume( .*|)"
				+ ")$", EN);
		info.setCustomTriggerRegXscoreBoost(2);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//required
		Parameter p1 = new Parameter(PARAMETERS.ACTION)
				.setRequired(true)
				.setQuestion("default_ask_action_0a");
		Parameter p2 = new Parameter(PARAMETERS.CLIENT_FUN)
				.setRequired(true)
				.setQuestion("client_controls_ask_fun_0a");
		
		//optional
		Parameter p3 = new Parameter(PARAMETERS.DATA);
		
		info.addParameter(p1).addParameter(p2).addParameter(p3);
		
		//Default answers
		info.addSuccessAnswer("ok_0b")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a");
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get parameters
		Parameter actionP = nluResult.getRequiredParameter(PARAMETERS.ACTION);
		String action = actionP.getValueAsString().replaceAll("^<|>$", "").trim();
		boolean isActionOpen = (action.equals(Action.Type.show.name()) || action.equals(Action.Type.on.name()));
		boolean isActionClose = (action.equals(Action.Type.remove.name()) || action.equals(Action.Type.off.name()));
		boolean isActionIncrease = (action.equals(Action.Type.increase.name()) || action.equals(Action.Type.add.name()));
		boolean isActionDecrease = (action.equals(Action.Type.decrease.name()) || action.equals(Action.Type.remove.name())); 	//note: remove is close and decrease here
		
		Parameter controlFunP = nluResult.getRequiredParameter(PARAMETERS.CLIENT_FUN);
		String controlFun = controlFunP.getValueAsString().replaceAll("^<|>$", "").trim();
		String controlFunLocal = (String) controlFunP.getDataFieldOrDefault(InterviewData.VALUE_LOCAL);
		if (Is.nullOrEmpty(controlFunLocal)){
			controlFunLocal = "Control";
		}else{
			try{
				controlFunLocal = NluTools.capitalizeAll(controlFunLocal.split("\\s")[1]);
			}catch(Exception e){}
		}
		boolean isSettings = controlFun.equals(ClientFunction.Type.settings.name());
		boolean isVolume = controlFun.equals(ClientFunction.Type.volume.name());
		boolean isAlwaysOn = controlFun.equals(ClientFunction.Type.alwaysOn.name());
		boolean isMeshNode = controlFun.equals(ClientFunction.Type.meshNode.name());
		
		Parameter dataP = nluResult.getOptionalParameter(PARAMETERS.DATA, "");
		String data = dataP.getValueAsString();
				
		//This service basically cannot fail here ... only inside client
		
		//split by control function
		String actionName = "";
		if (isSettings){
			//settings support
			if (isActionOpen){
				actionName = "open";
			}else if (isActionClose){
				actionName = "close";
			}else{
				//TODO: implement, ask or fail?
			}
		}else if (isVolume){
			//volume support
			if (isActionIncrease){
				actionName = "up";
			}else if (isActionDecrease){
				actionName = "down";
			}else{
				//TODO: implement, ask or fail?
			}
		}else if (isAlwaysOn){
			//Always-On mode support
			actionName = "toggle";	//we simply use a toggle command, no matter what action 
			
		}else if (isMeshNode){
			//Mesh-Node support
			actionName = data; 		//this call requires a custom data block
		}
		JSONObject a = JSON.make("action", actionName);
		api.addAction(ACTIONS.CLIENT_CONTROL_FUN);
		api.putActionInfo("fun", controlFun);
		api.putActionInfo("controlData", a);
		
		//some buttons - we use the custom function button but the client needs to parse the string itself!
		if (isSettings && isActionOpen){
			//open settings button
			api.addAction(ACTIONS.BUTTON_CUSTOM_FUN);
			api.putActionInfo("fun", "controlFun;;" + controlFun + ";;" + a.toJSONString());
			api.putActionInfo("title", controlFunLocal);
		}else if (isVolume){
			//volume up/down buttons
			api.addAction(ACTIONS.BUTTON_CUSTOM_FUN);
			api.putActionInfo("fun", "controlFun;;" + controlFun + ";;{\"action\":\"up\"}");
			api.putActionInfo("title", controlFunLocal + " +");
			//
			api.addAction(ACTIONS.BUTTON_CUSTOM_FUN);
			api.putActionInfo("fun", "controlFun;;" + controlFun + ";;{\"action\":\"down\"}");
			api.putActionInfo("title", controlFunLocal + " -");
		}else{
			//e.g. Mesh-Node action button
			api.addAction(ACTIONS.BUTTON_CUSTOM_FUN);
			api.putActionInfo("fun", "controlFun;;" + controlFun + ";;" + a.toJSONString());
			api.putActionInfo("title", "Action");
		}
		
		//Cards
		/* Cards should be generated by client ...
		Card card = new Card(Card.TYPE_SINGLE);
		card.addElement(ElementType.link, 
				JSON.make("title", "S.E.P.I.A." + ":", "desc", "Client Controls"),
				null, null, "", 
				"https://sepia-framework.github.io/", 
				"https://sepia-framework.github.io/img/icon.png", 
				null, null);
		//JSON.put(linkCard, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(card.getJSON());
		*/
		
		//all good
		if (actionName.isEmpty()){
			api.setStatusOkay();
		}else{
			api.setStatusSuccess();
		}
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
}
