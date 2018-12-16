package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.List;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.parameters.Test_Parameters.TestResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_RadioStation_MusicGenre {

	public static void main(String[] args) {
		
		Start.setupServicesAndParameters();
		String[] parametersToTest = new String[]{ PARAMETERS.MUSIC_GENRE, PARAMETERS.RADIO_STATION };
		
		String language = "de";
		System.out.println("---------- DE ----------");
				
		ArrayList<String> texts = new ArrayList<>();
		texts.add("Spiele ein Rock Radio");
		texts.add("Spiele ein rock Radio");
		texts.add("Spiele ein Rockradio");
		texts.add("Spiele Rockradio1");
		texts.add("Spiele Rockradio FM");
		texts.add("Spiele delta Radio");
		texts.add("Spiele Radio delta Föhnfrisur");
		texts.add("Spiele delta Radio Föhnfrisur");
		texts.add("delta Radio Föhnfrisur");
		texts.add("Spiele ein Radio mit Rock");
		texts.add("Spiele ein Radio mit Rockmusik");
		texts.add("Spiele ein Radio mit Metallica");
		texts.add("Spiele ein Radio mit Musik von Metallica");
		texts.add("Spiele Rockradio");
		
		printTestResults(texts, parametersToTest, language);
		
		language = "en";
		System.out.println("---------- EN ----------");
		
		texts = new ArrayList<>();
		texts.add("play a rock radio");
		texts.add("start Rockradio FM");
		texts.add("play delta Radio");
		texts.add("play radio delta Föhnfrisur");
		texts.add("play delta Radio Föhnfrisur");
		texts.add("delta Radio Föhnfrisur");
		texts.add("play a radio with Rock");
		texts.add("play a radio with rockmusic");
		texts.add("play a radio with Metallica");
		texts.add("play a radio with songs of Metallica");
		texts.add("play rockstation");

		printTestResults(texts, parametersToTest, language);
	}
	
	static void printTestResults(List<String> texts, String[] parametersToTest, String language){
		for (String text : texts){
			NluInput input = ConfigTestServer.getFakeInput("test", language);
			
			//normalize text
			Normalizer normalizer = Config.inputNormalizers.get(language);
			if (normalizer != null){
				input.textRaw = text;
				text = normalizer.normalizeText(text);
				input.text = text;
			}
			
			System.out.println("\ntext: " + text);
			TestResult tr = Test_Parameters.testAbstractParameterSearch(input, true, parametersToTest);
			System.out.println("score: " + tr.score);
			System.out.println("EXTRACTED: ");
			Debugger.printMap(tr.pv);
			System.out.println("BUILT: ");
			Debugger.printMap(tr.pvBuild);
			System.out.println("");
		}
	}

}
