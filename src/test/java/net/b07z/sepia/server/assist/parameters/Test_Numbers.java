package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.List;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.parameters.Test_Parameters.TestResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_Numbers {

	public static void main(String[] args) {
		
		Start.setupServicesAndParameters();
		String[] parametersToTest = new String[]{ PARAMETERS.NUMBER, PARAMETERS.SMART_DEVICE, PARAMETERS.ROOM, PARAMETERS.SMART_DEVICE_VALUE };
		
		String language = "de";
				
		ArrayList<String> texts = new ArrayList<>();
		texts.add("1LIVE starten bitte oder Kanal 106.7 suchen");
		texts.add("-305.14");
		texts.add("1,75€ ist größr als 1.57$");
		texts.add("Hausnummer 3b");
		texts.add("Hausnummer 20F");
		texts.add("-305.00 leer");
		texts.add("Das kostet $5");
		texts.add("Das kostet 5 Dollar");
		texts.add("Er wiegt 60 kg und läuft schnell");
		texts.add("Man verbraucht ca. 2000 kcal pro Tag");
		texts.add("Das sind ca. 8000 kJ Energie");
		texts.add("Stelle die Heizung auf 20°C bitte");
		texts.add("Heizung auf 20 Grad Celsius bitte");
		texts.add("Licht auf 70% im Wohnzimmer bitte");
		texts.add("Licht auf 10 Prozent bitte im Wohnzimmer");
		texts.add("Lampe 1 auf 50 Prozent");
		texts.add("Setze 50 Prozent für Lampe 1");
		texts.add("Setze 50 Prozent für Lampe 1 in Zimmer 2");
		texts.add("Lampe 1 in Zimmer 2 auf 50%");
		texts.add("Laser in Kammer 1 einschalten");
		texts.add("Heizung auf 20.5F bitte");

		printTestResults(texts, parametersToTest, language);
		
		System.out.println("\nNumber conversion test:\n");
		System.out.println("20°C in C: " + Number.convertTemperature("20", "heizung auf 20 grad celsius", null, "C", LANGUAGES.DE));
		System.out.println("20°C in F: " + Number.convertTemperature("20", "heizung auf 20 grad celsius", null, "F", LANGUAGES.DE));
		System.out.println("20 (C) in C: " + Number.convertTemperature("20", "heizung auf 20 grad", "C", "C", LANGUAGES.DE));
		System.out.println("20 (C) in F: " + Number.convertTemperature("20", "heizung auf 20 grad", "C", "F", LANGUAGES.DE));
		System.out.println("68 (F) in F: " + Number.convertTemperature("68", "heizung auf 68 grad", "F", "F", LANGUAGES.DE));
		System.out.println("68 (F) in C: " + Number.convertTemperature("68", "heizung auf 68 grad", "F", "C", LANGUAGES.DE));
		System.out.println("68f in C: " + Number.convertTemperature("68", "heizung auf 68f", "F", "C", LANGUAGES.DE));
		System.out.println("80°F in F: " + Number.convertTemperature("80", "heater to 80°f", "F", "F", LANGUAGES.EN));
		System.out.println("80°F in C: " + Number.convertTemperature("80", "heater to 80°f", "F", "C", LANGUAGES.EN));
		System.out.println("80f in C: " + Number.convertTemperature("80", "heater to 80f", "F", "C", LANGUAGES.EN));
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
