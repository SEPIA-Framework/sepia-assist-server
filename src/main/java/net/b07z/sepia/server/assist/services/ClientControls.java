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
import net.b07z.sepia.server.core.tools.Converters;
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
				+ "(.* |)lautstaerke( .*|)|"
				+ "(.* |)(medi(a|en)(-| |)player|medienwiedergabe)( .*|)|"
				+ "(.* |)(naechste(\\w|)|vorherige(\\w|)) (musik|song|lied|medien|media|titel)( .*|)|"
				+ "(naechste(\\w|)|vorherige(\\w|)|vor|zurueck|stop(pen|p|)|play|abspielen|lauter|leiser)|"
				+ "(.* |)(musik|song|lied|medien|media|titel|player) (anhalten|stoppen|stop(p|)|beenden|schliessen)( .*|)|"
				+ "(.* |)(stoppe|stop(p|)|schliesse)( .* | )(musik|song|lied|medien|media|titel|player|sound)( .*|)"
				+ ")$", DE);
		info.setCustomTriggerRegX("^("
				+ "(.* |)open setting(s|)( .*|)|"
				+ "(.* |)always(-| |)on( .*|)|"
				+ "(.* |)(music|sound|radio) (quieter|louder)( .*|)|"
				+ "(.* |)(volume|turn (up|down))( .*|)|"
				+ "(.* |)(media(-| |)player)( .*|)|"
				+ "(.* |)(next|previous) (media|music|song|track|title)( .*|)|"
				+ "(next|previous|back|forward|stop|play|louder|quieter)|"
				+ "(.* |)(media|music|song|track|title|player|sound) (stop|close|end)( .*|)|"
				+ "(.* |)(stop|close|end)( .* | )(media|music|song|track|title|player|sound)( .*|)"
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
		Parameter p4 = new Parameter(PARAMETERS.NUMBER);
		Parameter p5 = new Parameter(PARAMETERS.MEDIA_CONTROLS);
		
		info.addParameter(p1).addParameter(p2).addParameter(p3).addParameter(p4).addParameter(p5);
		
		//Default answers
		info.addSuccessAnswer("ok_0b")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a")
			.addCustomAnswer("volume_exceeded", "client_controls_volume_exceeded_0a")
			.addCustomAnswer("volume_eleven", "client_controls_volume_eleven_0a");
		
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
		boolean isActionEdit = (action.equals(Action.Type.set.name()) || action.equals(Action.Type.edit.name()));
		
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
		boolean isClexi = controlFun.equals(ClientFunction.Type.clexi.name());
		boolean isMedia = controlFun.equals(ClientFunction.Type.media.name());
		
		Parameter dataP = nluResult.getOptionalParameter(PARAMETERS.DATA, "");
		String data = dataP.getValueAsString();
		
		Parameter numberP = nluResult.getOptionalParameter(PARAMETERS.NUMBER, "");
		String num = numberP.getValueAsString();
				
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
		}else if (isMedia){
			//media support - TODO: limited to stop currently 
			if (isActionClose){
				actionName = "stop";
			}else{
				//TODO: implement, ask or fail?
			}
		}else if (isVolume){
			//check data for volume
			if (num.isEmpty() && !Is.nullOrEmpty(data)){
				if (data.startsWith("{")){
					//JSON with number
					Object numO = JSON.parseString(data).get("number");
					if (numO != null){
						long numL = Converters.obj2LongOrDefault(numO, -1l);
						if (numL > -1){
							num = String.valueOf(numL);
						}
					}
				}else if (data.matches("\\d+")){
					//number as string
					num = data;
				}
			}
			//volume support
			if (!num.isEmpty() && (isActionEdit || isActionIncrease || isActionDecrease)){
				long vol = Converters.obj2LongOrDefault(num, -1l);
				if (vol > 11){
					api.setCustomAnswer("client_controls_volume_exceeded_0a");
				}else if (vol == 11){
					api.setCustomAnswer("client_controls_volume_eleven_0a");
				}
				actionName = ("volume;;" + num); 		//we take the shortcut here =)
			}else if (num.isEmpty() && isActionEdit){
				//abort with generic question
				api.setIncompleteAndAsk(PARAMETERS.NUMBER, "default_ask_parameter_0b");
				ServiceResult result = api.buildResult();
				return result;
			}else if (isActionIncrease){
				actionName = "up";
			}else if (isActionDecrease){
				actionName = "down";
			}else{
				//TODO: implement, ask or fail?
			}
		}else if (isAlwaysOn){
			//Always-On mode support
			actionName = "toggle";	//we simply use a toggle command, no matter what action 
			
		}else if (isMeshNode || isClexi){
			//Mesh-Node & CLEXI support
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
			api.putActionInfo("title", "Button");
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
