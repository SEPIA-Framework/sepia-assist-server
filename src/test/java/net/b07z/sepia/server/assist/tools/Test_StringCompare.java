package net.b07z.sepia.server.assist.tools;

import java.util.Arrays;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.tools.StringCompare.StringCompareResult;

public class Test_StringCompare {

	public static void main(String[] args){
		
		//Test phrase matching
		matchPhrase("Deckenlampe", "Deckenlampe");
		matchPhrase("Deckenlampe", "Deckenlampe 1");
		matchPhrase("Deckenlampe 2", "Deckenlampe 1");
		matchPhrase("Lampe Ecke", "Schalte die Lampe in der Ecke ein");
		matchPhrase("Ecke", "Schalte die Lampe in der Ecke ein");
		
		String sentence = "Schalte die Deckenlampe im Wohnzimmer an.";
		System.out.println("\nScan: " + sentence);
		StringCompareResult scr = StringCompare.scanSentenceForBestPhraseMatch(
				sentence, 
				Arrays.asList("Ecklampe", "Ecklampe 2", "Deckenlampe 2"),
				LANGUAGES.DE
		);
		System.out.println("Scan result. Best match: " + scr.getResultString() + " - score: " + scr.getResultPercent());
	}

	private static void matchPhrase(String phrase, String sentence){
		System.out.println("Search: " + phrase + " - in: " + sentence + " - res: " + StringCompare.scanSentenceForPhrase(phrase, sentence));
	}
}
