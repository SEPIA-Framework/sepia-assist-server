package net.b07z.sepia.server.assist.tts;

import static org.junit.Assert.*;

import org.junit.Test;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.tts.TtsTools.EngineType;

public class TestTtsTools {

	@Test
	public void test(){
		String engine = EngineType.espeak.name();
		
		String language = LANGUAGES.DE;
				
		assertTrue(TtsTools.optimizePronunciation("😂  😃 😁 😆😉 😊 😣😞", language, engine)
				.trim().equals("."));
		
		assertTrue(TtsTools.optimizePronunciation("Es ist 12:30 Uhr", language, engine)
				.equals("Es ist 12 Uhr 30."));
		assertTrue(TtsTools.optimizePronunciation("Es ist 8:15 Uhr", language, engine)
				.equals("Es ist 8 Uhr 15."));
		
		assertTrue(TtsTools.optimizePronunciation("24.12.2021", language, engine)
				.equals("24.12.2021."));
		
		assertTrue(TtsTools.optimizePronunciation("Licht auf 70% ich meine 50 %", language, engine)
				.equals("Licht auf 70 Prozent ich meine 50 Prozent."));
		
		assertTrue(TtsTools.optimizePronunciation("Der Preis ist 2,5€", language, engine)
				.equals("Der Preis ist 2,5 Euro."));
		assertTrue(TtsTools.optimizePronunciation("Der Preis ist $2", language, engine)
				.equals("Der Preis ist 2 Dollar."));
		assertTrue(TtsTools.optimizePronunciation("€2.50", language, engine)
				.equals("2,50 Euro."));
		
		assertTrue(TtsTools.optimizePronunciation("1/2 1/3 1/4 2/3 3/4", language, engine)
				.equals("ein halb ein drittel ein viertel zwei drittel drei viertel."));
		assertTrue(TtsTools.optimizePronunciation("Nummer 1,2.", language, engine)
				.equals("Nummer 1,2."));
		assertTrue(TtsTools.optimizePronunciation("1,2 ist die Nummer", language, engine)
				.equals("1,2 ist die Nummer."));
		
		assertTrue(TtsTools.optimizePronunciation("3000 kWh", language, engine)
				.equals("3000 Kilowattstunden."));
		assertTrue(TtsTools.optimizePronunciation("5 kW", language, engine)
				.equals("5 Kilowatt."));
		assertTrue(TtsTools.optimizePronunciation("300 W", language, engine)
				.equals("300 Watt."));
		
		assertTrue(TtsTools.optimizePronunciation("Eine %%WINDOWS%% Variable", language, engine)
				.equals("Eine Prozent Prozent WINDOWS Prozent Prozent Variable."));
		assertTrue(TtsTools.optimizePronunciation("Eine $LINUX Variable", language, engine)
				.equals("Eine Dollar LINUX Variable."));
		assertTrue(TtsTools.optimizePronunciation("$(LINUX)", language, engine)
				.equals("Dollar (LINUX)."));
		assertTrue(TtsTools.optimizePronunciation("`pwd`", language, engine)
				.equals("`pwd`."));
		
		//System.out.println(TtsTools.optimizePronunciation("Heute ist der 19.12.2021", language, engine));
		//System.out.println(TtsTools.optimizePronunciation("Heute ist er 19", language, engine));
		
		language = LANGUAGES.EN;
		
		//System.out.println(TtsTools.optimizePronunciation("24.12.2021", language, engine));
		//System.out.println(TtsTools.optimizePronunciation("It's 8.30 p.m.", language, engine));
		
		assertTrue(TtsTools.optimizePronunciation("It's 8.30pm", language, engine)
				.equals("It's 8 30 pm."));
		assertTrue(TtsTools.optimizePronunciation("It's 8.30 p.m.", language, engine)
				.equals("It's 8 30 p.m."));
		assertTrue(TtsTools.optimizePronunciation("It's 12 o'clock", language, engine)
				.equals("It's 12 o'clock."));
		assertTrue(TtsTools.optimizePronunciation("It's 12:30 o'clock", language, engine)
				.equals("It's 12 30 o'clock."));
		
		assertTrue(TtsTools.optimizePronunciation("lights to 70% I mean 50 %", language, engine)
				.equals("lights to 70 percent I mean 50 percent."));
		assertTrue(TtsTools.optimizePronunciation("%70", language, engine)
				.equals("percent 70."));
		
		assertTrue(TtsTools.optimizePronunciation("the price is 2,5€", language, engine)
				.equals("the price is 2.5 euro."));
		assertTrue(TtsTools.optimizePronunciation("the price is $2.50", language, engine)
				.equals("the price is 2.50 dollar."));
		assertTrue(TtsTools.optimizePronunciation("$2,50 is the price", language, engine)
				.equals("2.50 dollar is the price."));
		
		assertTrue(TtsTools.optimizePronunciation("1/2 1/3 1/4 2/3 3/4", language, engine)
				.equals("one half one third one quater two thirds three quaters."));
		assertTrue(TtsTools.optimizePronunciation("Number 1.2.", language, engine)
				.equals("Number 1.2."));
		assertTrue(TtsTools.optimizePronunciation("1.2 is the number", language, engine)
				.equals("1.2 is the number."));
		
		assertTrue(TtsTools.optimizePronunciation("3000 kWh", language, engine)
				.equals("3000 kilowatt hours."));
		assertTrue(TtsTools.optimizePronunciation("5 kW", language, engine)
				.equals("5 kilowatt."));
		assertTrue(TtsTools.optimizePronunciation("300 W", language, engine)
				.equals("300 watt."));
		
		assertTrue(TtsTools.optimizePronunciation("The proof[1] is there [2].", language, engine)
				.equals("The proof 1 is there 2."));
		
		language = LANGUAGES.FR;
		
		//System.out.println(TtsTools.optimizePronunciation("%%", language, engine));
		assertTrue(TtsTools.optimizePronunciation("%%", language, engine)
				.equals("% %."));
		assertTrue(TtsTools.optimizePronunciation("$$", language, engine)
				.equals("$ $."));
		
		//----------------------------
		
		engine = EngineType.txt2pho_mbrola.name();
		
		assertTrue(TtsTools.optimizePronunciation("Licht steht auf 70.", language, engine)
				.equals("Licht steht auf 70"));
		assertTrue(TtsTools.optimizePronunciation("Licht steht auf 70%.", language, engine)
				.equals("Licht steht auf 70 %."));
	}

}
