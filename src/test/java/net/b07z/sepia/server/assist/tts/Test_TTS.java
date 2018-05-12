package net.b07z.sepia.server.assist.tts;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tts.TtsInterface;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.SoundPlayer;

public class Test_TTS {

	public static void main(String[] args) {
		
		//load stuff
		Config.setupAnswers();		//answers
		NluInput NLU_input = new NluInput();
		NLU_input.lastCmd = "empty";
		//NLU_Result NLU_result = new NLU_Result(NLU_input);
		
		long tic = System.currentTimeMillis();
		
		//create TTS interface
		//TTS_Interface acapela_emb = new TTS_Acapela_Embedded();
		//TTS_Interface acapela = new TTS_Acapela();
		TtsInterface acapela = (TtsInterface) ClassBuilder.construct(Config.ttsModule);
		
		//test sentences
		String text = "Hello! This is a test to see if it works ;-)";
		//String text = "Hello! This is a test to see if it works :-)";
		//String text = "And a second test to see if the ID is set properly.";
		//String text = "And a third test to see what happens ^^.";
		//String text = Statics.answers.getAnswer(NLU_result, "test_0a");		System.out.println(text4);
		//String text = "Hallo, mein Name ist Claudia und dies ist ein Test. Schade dass du jetzt schon los musst Florian, ich würde gerne länger mit dir reden!";
		//String text = "Hello, my name is Will and this is a test. Too bad you have to go already Florian, I'd really like to speak with you more!";

		//create audio
		//acapela.setLanguage("de");
		acapela.setLanguage("en");
		//acapela.setVoice("enu_sharon");
		acapela.setVoice("ged_claudia");
		acapela.setMood(0);
		acapela.setSoundFormat("MP3");
		String url1 = acapela.getAudioURL(text);
		//String url2 = acapela.getAudioURL(text4 + ":-|");
		//String url3 = acapela.getAudioURL(text4 + ":-)");
		//String url4 = acapela.getAudioURL(text4 + ":-(");
		
		System.out.println("Time to finish: " + (System.currentTimeMillis()-tic) + "ms");
		System.out.println("URL: " + url1);
		
		//play
		SoundPlayer player = new SoundPlayer();
		player.useThread = true;
		//player.play("Xtensions/AcapelaTTS/audio_out/test1.wav");
		//player.waitForSound(5000);
		//player.stop();
		player.play(url1.replaceFirst("https", "http"));
		//player.play(url3.replaceFirst("https", "http"));
		//player.play(url4.replaceFirst("https", "http"));
		
		//get audio info
		while(player.isPlaying()){
			try {	Thread.sleep(500);	} catch (Exception e) {}
			System.out.println("played: " + player.getElapsedTime() + "s of: " + player.getDuration() + "s");
		}
		
		//mass production
		/*
		acapela.getAudioURL(text1);
		acapela.getAudioURL(text2);
		acapela.getAudioURL(text3);
		acapela.getAudioURL(text1);
		acapela.getAudioURL(text2);
		acapela.getAudioURL(text3);
		acapela.getAudioURL(text1);
		acapela.getAudioURL(text2);
		acapela.getAudioURL(text3);
		acapela.getAudioURL(text1);
		acapela.getAudioURL(text2);
		acapela.getAudioURL(text3);
		acapela.getAudioURL(text1);
		acapela.getAudioURL(text2);
		acapela.getAudioURL(text3);
		acapela.getAudioURL(text1);
		acapela.getAudioURL(text2);
		acapela.getAudioURL(text3);
		acapela.getAudioURL(text1);
		acapela.getAudioURL(text2);
		acapela.getAudioURL(text3);
		*/
	}
	
}
