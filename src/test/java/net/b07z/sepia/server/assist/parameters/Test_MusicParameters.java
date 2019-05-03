package net.b07z.sepia.server.assist.parameters;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.parameters.Test_Parameters.TestResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_MusicParameters {

	private static String[] parametersToTest = new String[]{ 
			PARAMETERS.MUSIC_SERVICE, PARAMETERS.MUSIC_GENRE, 
			PARAMETERS.MUSIC_ARTIST, PARAMETERS.SONG, 
			PARAMETERS.PLAYLIST_NAME, PARAMETERS.MUSIC_ALBUM
	};
	
	public static void main(String[] args) {
		long tic = Debugger.tic();
		
		Start.setupServicesAndParameters();

		//warm up
		System.out.println("-----SENTENCE TESTING------");
		
		String lang = "de";
		System.out.println("\n----- de -----");
		testMusicParameters("Starte heavy-metal Lieder",						"[, heavy-metal, , , , ]", lang);
		testMusicParameters("Starte The Metal von Tenacious D",					"[, , Tenacious D, The Metal, , ]", lang);
		testMusicParameters("Spiele Songs von Jimi Hendrix",					"[, , Jimi Hendrix, , , ]", lang);
		testMusicParameters("Spiele den Titel Purple Haze von Jimi Hendrix",	"[, , Jimi Hendrix, Purple Haze, , ]", lang);
		testMusicParameters("Spiele das Lied Purple Haze von Jimi Hendrix",		"[, , Jimi Hendrix, Purple Haze, , ]", lang);
		testMusicParameters("Spiele den Song mit dem Titel Purple Haze von Jimi Hendrix",		"[, , Jimi Hendrix, Purple Haze, , ]", lang);
		testMusicParameters("Spiele etwas von Prince",							"[, , Prince, , , ]", lang);
		testMusicParameters("Spiele All along the Watchtower von Jimi Hendrix",	"[, , Jimi Hendrix, All along the Watchtower, , ]", lang);
		testMusicParameters("starte meine morning Playlist",					"[, meine, , , morning, ]", lang);
		testMusicParameters("starte die Playlist mit Namen die Beste",			"[, , , , die Beste, ]", lang);
		testMusicParameters("spiele Beatsteaks über Spotify",					"[<spotify>, , Beatsteaks, , , ]", lang);
		testMusicParameters("spiele Beatsteaks Songs über Spotify",				"[<spotify>, , Beatsteaks, , , ]", lang);
		testMusicParameters("spiele 1/2 Love Song von den Ärzten",				"[, , Ärzten, 1/2 Love Song, , ]", lang);
		testMusicParameters("spiele Eric Clapton Musik auf Apple Music",		"[<apple_music>, , Eric Clapton, , , ]", lang);
		testMusicParameters("starte Against them All von Stick to your Guns",	"[, , Stick to your Guns, Against them All, , ]", lang);
		testMusicParameters("Spiele Songs von Jimi Hendrix auf YouTube",		"[<youtube>, , Jimi Hendrix, , , ]", lang);
		testMusicParameters("Kannst du von Prince Purple Rain spielen",			"[, , Prince, Purple Rain, , ]", lang);
		testMusicParameters("Spiele Purple Rain von dem Künstler Prince",		"[, , Prince, Purple Rain, , ]", lang);
		testMusicParameters("Öffne Stairway to Heaven von Led Zeppelin",		"[, , Led Zeppelin, Stairway to Heaven, , ]", lang);
		testMusicParameters("Starte das Album Homework von Daft Punk",			"[, , Daft Punk, , , Homework]", lang);
		testMusicParameters("Starte Around the World von Daft Punk vom Album Homework",		"[, , Daft Punk, Around the World, , Homework]", lang);
		testMusicParameters("Spiele etwas vom Album Homework",					"[, , , , , Homework]", lang);
		testMusicParameters("Suche auf YouTube nach Metallica",					"[<youtube>, , Metallica, , , ]", lang);
		
		lang = "en";
		System.out.println("\n----- en -----");
		testMusicParameters("Start heavy-metal songs",							"[, heavy-metal, , , , ]", lang);
		testMusicParameters("Start The Metal by Tenacious D",					"[, , Tenacious D, The Metal, , ]", lang);
		testMusicParameters("play songs by Jimi Hendrix",						"[, , Jimi Hendrix, , , ]", lang);
		testMusicParameters("play the title Purple Haze by Jimi Hendrix",		"[, , Jimi Hendrix, Purple Haze, , ]", lang);
		testMusicParameters("play the song Purple Haze from Jimi Hendrix",		"[, , Jimi Hendrix, Purple Haze, , ]", lang);
		testMusicParameters("play the song with the title Purple Haze by Jimi Hendrix",		"[, , Jimi Hendrix, Purple Haze, , ]", lang);
		testMusicParameters("play anything by Prince",							"[, , Prince, , , ]", lang);
		testMusicParameters("play All along the Watchtower by Jimi Hendrix",	"[, , Jimi Hendrix, All along the Watchtower, , ]", lang);
		testMusicParameters("play my morning playlist",							"[, my, , , morning, ]", lang);
		testMusicParameters("start my playlist called the best",				"[, my, , , the best, ]", lang);
		testMusicParameters("play Beatsteaks via Spotify",						"[<spotify>, , Beatsteaks, , , ]", lang);
		testMusicParameters("play Beatsteaks songs via Spotify",				"[<spotify>, , Beatsteaks, , , ]", lang);
		testMusicParameters("play 1/2 Love Song by Die Ärzte",					"[, , Die Ärzte, 1/2 Love Song, , ]", lang);
		testMusicParameters("play Eric Clapton music on Apple Music",			"[<apple_music>, , Eric Clapton, , , ]", lang);
		testMusicParameters("play Against them All by Stick to your Guns",		"[, , Stick to your Guns, Against them All, , ]", lang);
		testMusicParameters("play Songs by Jimi Hendrix with YouTube",			"[<youtube>, , Jimi Hendrix, , , ]", lang);
		testMusicParameters("can you play Prince with Purple Rain",				"[, , Prince, Purple Rain, , ]", lang);
		testMusicParameters("play Purple Rain by the artist Prince",			"[, , Prince, Purple Rain, , ]", lang);
		testMusicParameters("open Stairway to Heaven by Led Zeppelin",			"[, , Led Zeppelin, Stairway to Heaven, , ]", lang);
		testMusicParameters("play the album Homework by Daft Punk",				"[, , Daft Punk, , , Homework]", lang);
		testMusicParameters("play Around the World by Daft Punk from the album Homework",		"[, , Daft Punk, Around the World, , Homework]", lang);
		testMusicParameters("play something from the record Homework",			"[, , , , , Homework]", lang);
		testMusicParameters("Search YouTube for Metallica",						"[<youtube>, , Metallica, , , ]", lang);
				
		System.out.println("-----------");
				
		System.out.println("Took: " + Debugger.toc(tic) + "ms");
	}

	private static boolean testMusicParameters(String text, String shouldBe, String language){
		
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
		//System.out.println("score: " + tr.score);
		System.out.println("EXTRACTED: ");
		Debugger.printMap(tr.pv);
		String res = "[";
		for (String p : parametersToTest){
			res += tr.pv.get(p) + ", ";
		}
		res = res.trim().replaceFirst(",$", "");
		res += "]";
		System.out.println("Result: " + res);
		//System.out.println("BUILT: ");
		//Debugger.printMap(tr.pvBuild);
		System.out.println("");

		if (res.equals(shouldBe)){
			return true;
		}else{
			try{ Thread.sleep(20); }catch(Exception e){}
			System.err.println("Found: " + res + " - should be: " + shouldBe);
			try{ Thread.sleep(20); }catch(Exception e){}
			return false;
		}
	}
}
