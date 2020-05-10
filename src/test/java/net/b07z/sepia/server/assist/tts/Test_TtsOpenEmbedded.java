package net.b07z.sepia.server.assist.tts;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.tts.TtsTools.EngineType;

public class Test_TtsOpenEmbedded {

	public static void main(String[] args){
		
		Start.loadConfigFile("test");
		Config.setupTts();
		
		TtsInterface tts = new TtsOpenEmbedded();
		
		System.out.println("Voices: " + tts.getVoices());
		
		//tts.setVoice("de-DE marytts f");
		tts.setVoice("de-DE espeak m");
		
		String url = tts.getAudioURL("Dies ist ein Test. Hallo Welt :-).");
		System.out.println(url);
		
		String emojis = ":-) :) ;) ;-[ ;] ^_^ -_- :-| ;| O_o, o_O OO_OO :-)) :-), :-). :-)";
		System.out.println(emojis + " -----> " + TtsTools.trimText(emojis));
		System.out.println(TtsTools.trimText("Cool :-), I like :)!"));
		System.out.println("Cool :-), I like! - MI: " + TtsTools.getMoodIndex("Cool :-), I like!", -99));
		System.out.println("Cool :-], I like! - MI: " + TtsTools.getMoodIndex("Cool :-], I like!", -99));
		System.out.println("Cool ;), I like! - MI: " + TtsTools.getMoodIndex("Cool ;), I like!", -99));
		System.out.println("No :-( - MI: " + TtsTools.getMoodIndex("No :-(", -99));
		System.out.println("No :[ - MI: " + TtsTools.getMoodIndex("No :[", -99));
		System.out.println("OK :-|? - MI: " + TtsTools.getMoodIndex("OK :-|?", -99));
		System.out.println("OK ^_^ - MI: " + TtsTools.getMoodIndex("OK ^_^", -99));
		System.out.println("OK -_- - MI: " + TtsTools.getMoodIndex("OK -_-", -99));
		
		System.out.println(TtsTools.optimizePronunciation("Licht auf 70% ich meine 50 %", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("%70", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("Der Preis ist 2,5€", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("Der Preis ist $2", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("Der Preis ist $2.50", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("Preis $2.50 ok?", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("€2.50", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("%%TEST%%", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("Eine %%WINDOWS%% Variable", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("$(LINUX)", LANGUAGES.DE, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("Eine $LINUX Variable", LANGUAGES.DE, EngineType.espeak.name()));
		
		System.out.println(TtsTools.optimizePronunciation("lights to 70% I mean 50 %", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("%70", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("the price is 2,5€", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("$2 is the price", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("$2,50 is the price", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("the price is $2.50", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("price $2.50 ok?", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("€2.50 is the price", LANGUAGES.EN, EngineType.espeak.name()));
		System.out.println(TtsTools.optimizePronunciation("The proof[1] is there [2].", LANGUAGES.EN, EngineType.espeak.name()));
	}

}
