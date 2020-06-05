package net.b07z.sepia.server.assist.tools;

import java.util.Arrays;
import java.util.Collection;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.tools.StringCompare.StringCompareResult;

public class Test_StringCompare {
	
	public static void main(String[] args){
		test(false);
		try{ Thread.sleep(1000);	}catch (InterruptedException e){}
		
		long tic = System.currentTimeMillis();
		int N = 0; //1000;
		for (int i=0; i<N; i++){
			test(true);
		}
		System.out.println("\nTook (ms): " + (System.currentTimeMillis() - tic)); 	//~650ms with xdrop, ~500ms with Kotlin
	}

	public static void test(boolean skipPrint){
		
		//Test phrase matching
		matchPhrase("Deckenlampe", "Deckenlampe", 100, skipPrint);
		matchPhrase("Deckenlampe", "Deckenlampe 1", 100, skipPrint);
		matchPhrase("Deckenlamp", "Deckenlampe", 99, skipPrint);
		matchPhrase("wit", "with it", 99, skipPrint);
		matchPhrase("with", "wit", 86, skipPrint);
		matchPhrase("Deckenlampe 2", "Deckenlampe 1", 92, skipPrint);
		matchPhrase("Lampe Ecke", "Schalte die Lampe in der Ecke ein", 60, skipPrint);
		matchPhrase("Ecke", "Schalte die Lampe in der Ecke ein", 100, skipPrint);
		matchPhrase("Licht", "RGB Licht", 100, skipPrint);
		matchPhrase("RGB Licht", "Licht", 71, skipPrint);
		matchPhrase("Licht", "Ambiente Licht", 100, skipPrint);
		matchPhrase("Ambiente Licht", "Licht", 53, skipPrint);
		/*
		otherFuzzyMatch("Licht", "RGB Licht");
		otherFuzzyMatch("RGB Licht", "Licht");
		otherFuzzyMatch("Licht", "Ambiente Licht");
		otherFuzzyMatch("Ambiente Licht", "Licht");
		*/
		
		matchPhrases("Schalte die Deckenlampe im Wohnzimmer an.", 
				Arrays.asList("Ecklampe", "Ecklampe 2", "Deckenlampe", "Deckenlampe 2"),
				"Deckenlampe", 100, skipPrint
		);
		matchPhrases("Schalte das Licht im Wohnzimmer an.", 
				Arrays.asList("RGB Licht", "Ambiente Licht", "Ambiente Licht 2", "Licht"),
				"Licht", 100, skipPrint
		);
		matchPhrases("Schalte das Abinente Licht im Wohnzimmer an.", 
				Arrays.asList("RGB Licht", "Ambiente Licht", "Ambiente Licht 2"),
				"Ambiente Licht", 93, skipPrint
		);
		matchPhrases("Licht.", 
				Arrays.asList("RGB Licht", "Ambiente Licht", "Ambiente Licht 2", "Lampe", "Deckenlicht"),
				"RGB Licht", 71, skipPrint
		);
	}

	private static void matchPhrase(String phrase, String sentence, int assertVal, boolean skipPrint){
		int val = StringCompare.scanSentenceForPhrase(phrase, sentence);
		if (!skipPrint){
			if (val == assertVal){
				System.out.println("matchPhrase: " + phrase + " - in: " + sentence + " - res: " + val);
			}else{
				System.err.println("matchPhrase: " + phrase + " - in: " + sentence + " - res: " + val);
			}
		}
	}
	private static void matchPhrases(String sentence, Collection<String> phrases, String assertRes, int assertVal, boolean skipPrint){
		if (!skipPrint){
			System.out.println("\nScan (w/o norm.): " + sentence);
		}
		for (String p : phrases){
			int score = StringCompare.scanSentenceForPhrase(p, sentence);
			if (!skipPrint) System.out.println("phrase: " + p + " - score: " + score); 		//DEBUG
		}
		StringCompareResult scr = StringCompare.scanSentenceForBestPhraseMatch(
				sentence, phrases, LANGUAGES.DE
		);
		if (!skipPrint){
			if (scr.getResultString().equals(assertRes) && scr.getResultPercent() == assertVal){
				System.out.println("Scan result. Best match (with norm.): " + scr.getResultString() + " - score: " + scr.getResultPercent());
			}else{
				System.err.println("Scan result. Best match (with norm.): " + scr.getResultString() + " - score: " + scr.getResultPercent());
			}
			try{ Thread.sleep(50);	}catch (InterruptedException e){}
		}
	}
	
	/*private static void otherFuzzyMatch(String phrase, String sentence){
		System.out.println("weightedMatch: " + phrase + " - in: " + sentence + " - res: " + FuzzySearch.tokenSetRatio(phrase, sentence));
	}*/
}
