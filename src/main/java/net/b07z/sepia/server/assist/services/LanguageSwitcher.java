package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;

/**
 * A service that can trigger the language switch action inside the client.
 * 
 * @author Florian Quirin
 *
 */
public class LanguageSwitcher implements ServiceInterface{
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Ändere die Sprache zu Englisch.");
			
		//OTHER
		}else{
			samples.add("Switch language to German.");
		}
		return samples;
	}
	
	//Custom answer pool:
	
	@Override
	public ServiceAnswers getAnswersPool(String language) {
		ServiceAnswers answerPool = new ServiceAnswers(language);
		
		//Build German answers
		if (language.equals(LANGUAGES.DE)){
			answerPool
				.addAnswer(whatLanguage, 0, "Auf welche Sprache soll ich wechseln?")
				.addAnswer(whatLanguage, 1, "Sorry welche Sprache war das? Englisch oder Deutsch wäre zum Beispiel möglich.")
				.addAnswer(whatLanguage, 2, "Sorry hab es immer noch nicht verstanden. Sag noch mal die Sprache bitte?")
				.addAnswer(followUpTest, 0, "Bin jetzt im deutsch Modus <user_name>.")
				.addAnswer(languageIsSame, 0, "<user_name>, ich glaube das sprechen wir bereits.")
				;
			return answerPool;
		
		//Fall back to English
		}else{
			answerPool
				.addAnswer(whatLanguage, 0, "To which language should I switch?")
				.addAnswer(whatLanguage, 1, "Sorry what was the language? I can speak English and German for example.")
				.addAnswer(whatLanguage, 2, "Sorry I still didn't get it. Say the language once again, please!")
				.addAnswer(followUpTest, 0, "English is now active <user_name>.")
				.addAnswer(languageIsSame, 0, "<user_name>, I think this is already what we are speaking right now.")
				;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers need to start with a certain prefix
	private static final String whatLanguage = ServiceAnswers.ANS_PREFIX + "language_switch_ask_0a";
	private static final String languageIsSame = ServiceAnswers.ANS_PREFIX + "language_switch_0b";
	private static final String followUpTest = ServiceAnswers.ANS_PREFIX + "language_switch_ft_test_0a";

	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		ServiceInfo info = new ServiceInfo(Type.plain, Content.action, false);
		
		//Command
		String CMD_NAME = CMD.LANGUAGE_SWITCH; 		//parameters:	language
		info.setIntendedCommand(CMD_NAME);
		
		//Direct-match trigger sentences in different languages:
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentence("Change language.", EN);
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentence("Sprache ändern.", DE);
		
		//Regular expression triggers:
		info.setCustomTriggerRegX(".*\\b("
					+ "(change|switch|set)( the | )language( to|$)|"
					+ "^((change|switch) to |speak )(german|english)$"
				+ ")\\b.*", EN);
		info.setCustomTriggerRegX(".*\\b("
					+ "(aendere|setze|stelle|wechs(le|el)) die sprache( auf| zu|$)|"
					+ "sprache (aendern|setzen|stellen|wechseln)( auf| zu|$)|"
					+ "sprache (auf|zu) (\\w+ ){1,2}(aendern|setzen|stellen|wechseln)|"
					+ "^(wech(seln|sel|sle) (auf|zu) |sprich |spreche )(deutsch|englisch)$"
				+ ")\\b.*", DE);
		info.setCustomTriggerRegXscoreBoost(3);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//required
		Parameter p1 = new Parameter(PARAMETERS.LANGUAGE)
				.setRequired(true)
				.setQuestion(whatLanguage);
		
		info.addParameter(p1);
		
		//Default answers
		info.addSuccessAnswer("ok_0a") 	//TODO: timing is tricky here because switch action can come before TTS triggers. Thats why we use the simplest answer: "ok" ;-)
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
		Parameter languageP = nluResult.getRequiredParameter(PARAMETERS.LANGUAGE);
		String targetLang = languageP.getValueAsString();
		String targetLangShort = targetLang.replaceFirst("-.*", "").trim();
		
		//is same language?
		if (targetLangShort.equals(nluResult.input.language)) {
			//Abort
			api.setCustomAnswer(languageIsSame);
			api.setStatusSuccess();
			ServiceResult result = api.buildResult();
			return result;
		}
		
		//Tell client to perform this platform action);
		api.addAction(ACTIONS.SWITCH_LANGUAGE);
		api.putActionInfo("language_code", targetLang);
		//api.putActionInfo("skip_save", true);
		
		//Schedule a test sentence as follow-up result after 3s if possible
		if (nluResult.input.isDuplexConnection()){
			//Some info about the connection and message:
			//System.out.println(nluResult.input.connection);
			//System.out.println(nluResult.input.msgId);
			//System.out.println(nluResult.input.duplexData);
			api.runOnceInBackground(3000, () -> {
				//set new language for following dialog
				nluResult.input.language = targetLangShort;
				nluResult.language = nluResult.input.language;
				
				//build follow-up result
				ServiceBuilder service = new ServiceBuilder(nluResult);
				service.answer = Answers.getAnswerString(nluResult, followUpTest);
				service.status = "success";
				/*boolean wasSent =*/ service.sendFollowUpMessage(service.buildResult());
				return;
			});
		}
				
		//build the API_Result - cannot fail anymore at this point
		api.setStatusSuccess();
		ServiceResult result = api.buildResult();
		return result;
	}
}
