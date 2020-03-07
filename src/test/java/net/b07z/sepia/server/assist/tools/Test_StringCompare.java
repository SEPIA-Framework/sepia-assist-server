package net.b07z.sepia.server.assist.tools;

import java.util.Arrays;
import java.util.Collection;

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
		matchPhrase("Licht", "RGB Licht");
		matchPhrase("RGB Licht", "Licht");
		matchPhrase("Licht", "Ambiente Licht");
		matchPhrase("Ambiente Licht", "Licht");
		/*
		otherFuzzyMatch("Licht", "RGB Licht");
		otherFuzzyMatch("RGB Licht", "Licht");
		otherFuzzyMatch("Licht", "Ambiente Licht");
		otherFuzzyMatch("Ambiente Licht", "Licht");
		*/
		
		matchPhrases("Schalte die Deckenlampe im Wohnzimmer an.", Arrays.asList("Ecklampe", "Ecklampe 2", "Deckenlampe", "Deckenlampe 2"));
		matchPhrases("Schalte das Licht im Wohnzimmer an.", Arrays.asList("RGB Licht", "Ambiente Licht", "Ambiente Licht 2", "Licht"));
		matchPhrases("Schalte das Abinente Licht im Wohnzimmer an.", Arrays.asList("RGB Licht", "Ambiente Licht", "Ambiente Licht 2"));
		matchPhrases("Licht.", Arrays.asList("RGB Licht", "Ambiente Licht", "Ambiente Licht 2", "Lampe", "Deckenlicht"));
	}

	private static void matchPhrase(String phrase, String sentence){
		System.out.println("matchPhrase: " + phrase + " - in: " + sentence + " - res: " + StringCompare.scanSentenceForPhrase(phrase, sentence));
	}
	private static void matchPhrases(String sentence, Collection<String> phrases){
		System.out.println("\nScan: " + sentence);
		for (String p : phrases){
			int score = StringCompare.scanSentenceForPhrase(p, sentence);
			System.out.println("phrase: " + p + " - score: " + score); 		//DEBUG
		}
		StringCompareResult scr = StringCompare.scanSentenceForBestPhraseMatch(
				sentence, phrases, LANGUAGES.DE
		);
		System.out.println("Scan result. Best match: " + scr.getResultString() + " - score: " + scr.getResultPercent());
	}
	
	/*private static void otherFuzzyMatch(String phrase, String sentence){
		System.out.println("weightedMatch: " + phrase + " - in: " + sentence + " - res: " + FuzzySearch.tokenSetRatio(phrase, sentence));
	}*/
}
