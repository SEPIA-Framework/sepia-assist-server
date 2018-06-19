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

public class Test_LocationXY {

	public static void main(String[] args) {
		
		Start.setupServicesAndParameters();
		String[] parametersToTest = new String[]{ PARAMETERS.LOCATION_START, PARAMETERS.LOCATION_END, PARAMETERS.TIME};
		
		String language = "de";
				
		ArrayList<String> texts = new ArrayList<>();
		texts.add("Weg von Aachen nach Frankfurt Hauptbahnhof");
		texts.add("Weg von Aachen nach Frankfurt Hauptbahnhof am Freitag um 9 Uhr");

		printTestResults(texts, parametersToTest, language);
	}
	
	static void printTestResults(List<String> texts, String[] parametersToTest, String language){
		for (String text : texts){
			NluInput input = ConfigTestServer.getFakeInput("test", language);
			
			//normalize text
			Normalizer normalizer = Config.inputNormalizers.get(language);
			if (normalizer != null){
				input.textRaw = text;
				text = normalizer.normalize_text(text);
				input.text = text;
			}
			
			TestResult tr = Test_Parameters.testAbstractParameterSearch(input, true, parametersToTest);
			System.out.println("\ntext: " + text);
			System.out.println("score: " + tr.score);
			System.out.println("EXTRACTED: ");
			Debugger.printMap_SS(tr.pv);
			System.out.println("BUILT: ");
			Debugger.printMap_SS(tr.pvBuild);
			System.out.println("");
		}
	}

}
