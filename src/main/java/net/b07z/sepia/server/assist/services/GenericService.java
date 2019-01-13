package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * A generic service that implements "Hello World". This is only a demo.
 * 
 * @author Florian Quirin
 *
 */
public class GenericService implements ServiceInterface{
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Hallo Welt!");
			
		//OTHER
		}else{
			samples.add("Hello world!");
		}
		return samples;
	}
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release
		//info.makePublic();
		
		//Command
		String CMD_NAME = "generic";		//Name tag of your service (will be combined with userId to be unique)
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Direct-match trigger sentences in different languages:
		String DE = LANGUAGES.DE;
		info.addCustomTriggerSentence("Hallo Welt!", DE)
			.addCustomTriggerSentence("Hello world!", DE)
			.addCustomTriggerSentence("Teste meinen hallo Welt service.", DE);
		String EN = LANGUAGES.EN;
		info.addCustomTriggerSentence("Hello world!", EN)
			.addCustomTriggerSentence("Test my hello world service.", EN);
		
		//Parameters:
		//This service has no parameters
		
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer( "chat_hello_0a")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a");
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, getInfo(nluResult.language));
		
		//get required parameters
		//NONE
		//get optional parameters
		//NONE
		
		//This service basically cannot fail ... ;-)
		
		//Just for demo purposes we add a button-action with a link to the SDK
		api.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
		api.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-sdk-java");
		api.putActionInfo("title", "SDK info");
		
		//... and we also add a demo card
		Card card = new Card(Card.TYPE_SINGLE);
		card.addElement(ElementType.link, 
				JSON.make("title", "S.E.P.I.A." + ":", "desc", "Hello World!"),
				null, null, "", 
				"https://sepia-framework.github.io/", 
				"https://sepia-framework.github.io/img/icon.png", 
				null, null);
		//JSON.put(linkCard, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(card.getJSON());
		
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
}
