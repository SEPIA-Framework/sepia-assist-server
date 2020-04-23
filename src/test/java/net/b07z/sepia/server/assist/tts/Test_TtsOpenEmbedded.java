package net.b07z.sepia.server.assist.tts;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;

public class Test_TtsOpenEmbedded {

	public static void main(String[] args){
		
		Start.loadConfigFile("test");
		Config.setupTts();
		
		TtsInterface tts = new TtsOpenEmbedded();
		
		System.out.println("Voices: " + tts.getVoices());
		
		tts.setVoice("de-DE marytts f");
		
		String url = tts.getAudioURL("Dies ist ein Test. Hallo Welt :-).");
		
		System.out.println(url);
	}

}
