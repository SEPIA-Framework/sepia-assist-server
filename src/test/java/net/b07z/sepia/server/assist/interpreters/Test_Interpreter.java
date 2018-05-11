package net.b07z.sepia.server.assist.interpreters;

import java.awt.Desktop;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.Open_CustomLink;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluInterface;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluKeywordAnalyzerDE;
import net.b07z.sepia.server.assist.interviews.AbstractInterview;
import net.b07z.sepia.server.assist.interviews.InterviewResult;
import net.b07z.sepia.server.assist.interviews.InterviewInterface;
import net.b07z.sepia.server.assist.interviews.NoResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.tts.TtsInterface;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Converters;

/**
 * Use this to test the NL-Processor and API results during development (means NOT on server ;-) ).
 * @author Florian Quirin
 *
 */
public class Test_Interpreter {

	public static void main(String[] args) {
		
		//load statics
		Config.setupAnswers();
		Start.setupServicesAndParameters();
		
		String text;
			//text = "Hello, can you please tell me the way to the next supermarket by walking?";
			//text = "do you have some news on the weather in Essen";
			//text = "show me the weather in Essen for tomorrow";
			//text = "at monday, show me the way with the bus from Essen to Dortmund";
			//text = "at 12.06.2015, show me the way with the bus from Essen to Dortmund";
			//text = "websearch for some music by Jimi Hendrix?";
			//text = "play songs of sorrow by the sorrow band";		//this is tricky and fails!
			//text = "who was Gaddafi?";
			//text = "search movies with Bruce Willis please";
			//text = "search the movie Iron Man";
			//text = "who was Albert Einstein?";
			//text = "what is a test?";
			//text = "what is a smirgosmag?";
			//text = "search the movie smirgosmag";
			//text = "movies with pam";
			//text = "test";
			//text = "start navigation";
			//text = "how is the weather in Berlin tomorrow?";
			//text = "where is the closest hospital?";
			//text = "where can i find a good bar here?";
			//text = "search the map for the eifel tower";
			//text = "show me the tv program for tonight";
			//text = "show me the tv program for tomorrow night";
			//text = "whats on tv right now?";
		//NLU_Interface NLP = new NLU_keyword_analyzer_en();			//interpreters implement the NLU_interface
			//text = "wo ist der nächste supermarkt";
			//text = "wo ist hier ein aldi?";
			//text = "kannst du mir sagen wo ich hier bin";
			//text = "gibt es gute bars in der Nähe?";
			//text = "wo ist die freiheitsstatue?";
			//text = "suche auf der Karte nach Ayers Rock";
			//text = "wo finde ich das nächste Krankenhaus";
			//text = "wo finde ich hier eine gute bar?";
			//text = "suche Apotheke";
			//text = "suche die schnellste Verbindung von Asien nach Amerika";
			//text = "finde mir die günstigste Verbindung nach Hause am Sonntag Abend";
			//text = "zeig mir den weg von Frankfurt nach München mit der bahn am montag";
			//text = "suche die beste verbindung von Essen";
			//text = "wer ist Lukas Podolski";
			//text = "was gibts neues";
			//text = "was läuft heute im TV?";
			//text = "zeig mir die edeka shoppingliste";
			//text = "zeig edeka shoppingliste";
			//text = "edeka liste";
			//text = "zeig liste";
			//text = "setze Milch und Honig auf die EDEKA Liste";
			//text = "kannst du bitte Milch auf die Aldi Liste setzen?";
			text = "und setze auch bitte noch Käse auf die Liste";
		NluInterface NLP = new NluKeywordAnalyzerDE();			//interpreters implement the NLU_interface
		
		//command reconstructor tests:
			//text = "no_result;text=test;";
			//text = "movies;search=adam sandler;type=;info=actor;";
			//text = "knowledgebase;search=albert einstein;type=person;";
			//text = "directions;start=Essen;end=Munich;type=car;time=today";
			//text = "chat;type=greeting;";
			//text = "chat;type=question;info=how_are_you;";
			//text = "chat;type=question;info=name;";
			//text = "chat;type=compliment";
			//text = "chat;	reply=<test_0a>;";
			//text = "chat;	reply=Auf zum Atom! :-);";
			//text = "chat; type=complain";
			//text = "weather; place=Munich; time=today";
			//text = "weather;";
		//NLU_Interface NLP = new NLU_cmd_reconstructor();			//interpreters implement the NLU_interface
		
		System.out.println("INPUT: " + text);
		
		//parameters
		String language = "de";
		String context = "default";
		int mood = -1;
		String environment = "web_app";
		
		//get the result of the natural-language-processor (i will usually call it either NLP or NLU and mix at will ;-) )
		NluInput input = new NluInput(text, language, context, mood, environment);
		User user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		input.user_location = "<city>Berlin City<latitude>52.518616<longitude>13.404636";
		//System.out.println("Today: " + Tools_DateTime.getToday("HH:mm:ss yyyy.MM.dd", input));
		input.user = user;
		NluResult result = NLP.interpret(input);
		
		//show all possible results
		System.out.println("CMDs identified by NL-Proc: " + result.showAllPossibleCMDs());
		System.out.println("CMD selected: " + result.getCommand());
		
		//internally results are usually passed around as NLU_results, but clients can ask for a JSON string via HTTP GET
		//JSON example as seen by clients:
		JSONObject bestResult = result.getBestResultJSON();				//convert best interpreter result to JSON
		System.out.println(bestResult.toJSONString());						//full result as JSON string
		if (((String)bestResult.get("result")).matches("success")){
			System.out.println("BEST CMD: " + bestResult.get("command"));		//JSON version of result.get_command()
			JSONObject params = (JSONObject) bestResult.get("parameters");		//all parameters
			System.out.println("PARAMETERS: " + params.toJSONString());			//JSON version of result.get_parameter("xy")
		}
		
		String res ="";
		
		System.out.println("---command: " + result.getCommand());
		System.out.println("---parameters:");
		for (Map.Entry<String, String> entry : result.parameters.entrySet()) {
			System.out.println("----" + entry.getKey() + " = " + entry.getValue());
		}
		
		/*
		//interview module with services 
		ArrayList<API_Interface> services = ConfigServices.getCustomOrSystemServices(user, result.get_command());
		if (!services.isEmpty()){
			Interview_Interface interview = new AbstractInterview();
			interview.setCommand(result.get_command());
			interview.setServices(services);
			InterviewResult iResult = interview.getMissingParameters(result);
			if (iResult.isComplete()){
				res = interview.getServiceResults(iResult).getResultJSON();
			}else{
				res = iResult.getApiComment().getResultJSON();
			}
			
		//get command
		//ConfigServices.load_command_map();
		//API_Interface plugin = ConfigServices.command_map.get(result.get_command());
		//if (plugin != null){
		//	res = plugin.getResult(result).getResultJSON();

		//Open Link
		} else if (result.get_command().matches(CMD.OPEN_LINK)){
			res = Open_CustomLink.get(result).getResultJSON();
			
		//Custom no result
		} else if (result.get_command().matches("custom_no_result")){
			res = NoResult.get(result, "no_answer_0a").getResultJSON();
			
		//Demo Commands
		//} else if (result.get_command().matches("demo_cmds")){
		//	res = Demo.get(result).getResultJSON();

			
		//no API available - or other reason for No_Result
		}else{
			res = NoResult.get(result).getResultJSON();
		}
		//test and show
		System.out.println(res);
		testResult(res);
		*/
	}
	
	
	/**
	 * Creates a HTML test file using the API result and opens it with the default browser.
	 * 
	 * @param json - JSON formatted string result created by API_Result class
	 */
	public static void testResult(String json){
		//parse JSON String
		JSONObject json_obj = Converters.str2Json(json);
		String htmlInfo = (String) json_obj.get("htmlInfo");
		String answer = (String) json_obj.get("answer");
		boolean hasInfo = (boolean) json_obj.get("hasInfo");
		boolean hasCard = (boolean) json_obj.get("hasInfo");
		System.out.println("hasInfo: " + hasInfo + ", hasCard: " + hasCard);		//debug
		
		//replace client parameters
		answer = answer.replaceAll("<user_name>", "Boss");
		
		//if (((String)json_obj.get("result")).matches("success")){
			//create Html content (and answer)
			String htmlCont="";
			if (hasInfo){
				htmlCont = "<!DOCTYPE html><meta charset='utf-8'/><body>";
				//css
				htmlCont += "<style>* { font-family: sans-serif; box-sizing: border-box; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; }</style>";
				htmlCont += "<style>.htmlInfo { padding:5px 5px; border:1px solid black; background-color: #FFFFFF; overflow:hidden; overflow-y:auto; }</style>";
				htmlCont += "<style>.htmlInfo { position: absolute; left: 5px; top: 5px; width: 320px; height: 560px; }</style>";
				htmlCont += "<style>.cardInfo { padding:5px 5px; border:1px solid black; background-color: #EEEEEE; overflow:hidden; overflow-y:auto; }</style>";
				htmlCont += "<style>.cardInfo { position: absolute; left: 335px; top: 215px; width: 320px; height: 350px; }</style>";
				htmlCont += "<style>.answer { padding:5px 5px; border:1px solid black; background-color: #5577FF; color:#FFFFFF; overflow:hidden; overflow-y:auto; }</style>";
				htmlCont += "<style>.answer { position: absolute; left: 335px; top: 5px; width: 320px; height: 200px; }</style>";
				htmlCont += "<style>.answer a:link{ color:#FFFFFF; }</style>";
				//body
				htmlCont += "<div class='htmlInfo'>" + htmlInfo + "</div>";
				htmlCont += "<div class='answer'>" + answer + "</div>";
				htmlCont += "<div class='cardInfo'>" + "TODO: 'cards' implementation" + "</div>";
				htmlCont += "</body>";
	        	//System.out.println(htmlCont);		//debug
	        }else{
	        	htmlCont = "<!DOCTYPE html><body>";
				//css
	        	htmlCont += "<style>* { font-family: sans-serif; box-sizing: border-box; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; }</style>";
	        	htmlCont += "<style>.answer { padding:5px 5px; border:1px solid black; background-color: #5577FF; color:#FFFFFF; overflow:hidden; overflow-y:auto; }</style>";
				htmlCont += "<style>.answer { position: absolute; left: 335px; top: 5px; width: 320px; height: 200px; }</style>";
				//body
				htmlCont += "<div class='answer'>" + answer + "</div>";
				htmlCont += "</body>";
	        }
	        //make html file
	        try {
	        	PrintWriter writer = new PrintWriter("test.html", "UTF-8");
	        	writer.println(htmlCont);
	        	writer.close();
				Desktop.getDesktop().browse(new URI("test.html"));
				
			} catch (Exception e) {
				System.out.println("Some error happened while creating html test file.");
				e.printStackTrace();
			}
		//}else{
		//	System.out.println("ERROR: JSON String indicates fail!");
		//	System.out.println("ANSWER: " + answer);
		//}
		
		//TTS
		speak(answer);
		
	}
	
	/**
	 * Test TTS
	 * @param message - message to speak
	 */
	public static void speak(String message){
		TtsInterface speaker = (TtsInterface) ClassBuilder.construct(Config.ttsModule); //new TTS_Acapela();
		speaker.setLanguage("en");
		String audioURL = speaker.getAudioURL(message);
		System.out.println("Audio: " + audioURL);
	}

}
