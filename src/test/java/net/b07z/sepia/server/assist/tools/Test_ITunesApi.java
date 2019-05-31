package net.b07z.sepia.server.assist.tools;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_ITunesApi {

	public static void main(String[] args) {

		//load custom config
		Start.loadSettings(new String[]{"--test"});
		
		ITunesApi api = new ITunesApi("US");
		
		JSONObject s = api.searchBestMusicItem("Paradise City", "Guns n Roses", "Appetite For Destruction", "", "");
		System.out.println(s.toJSONString());
		
		Debugger.sleep(500);
		/*
		s = api.searchBestMusicItem("Paradise City", "Guns n Roses", "Greatest Hits", "", "");
		System.out.println(s.toJSONString());

		Debugger.sleep(500);
		
		s = api.searchBestMusicItem("", "Guns n Roses", "Appetite For Destruction", "", "");
		System.out.println(s.toJSONString());

		Debugger.sleep(500);
		
		s = api.searchBestMusicItem("", "Guns n Roses", "", "", "");
		System.out.println(s.toJSONString());
		
		Debugger.sleep(500);
		
		s = api.searchBestMusicItem("", "", "", "", "Rock");
		System.out.println(s.toJSONString());
		
		Debugger.sleep(500);
		
		s = api.searchBestMusicItem("", "", "", "Party", "");
		System.out.println(s.toJSONString());
		
		Debugger.sleep(500);
		
		s = api.searchBestMusicItem("Foxy Lady", "Jimi Hendrix", "", "", "");	//note the typo in foxy (not foxey)
		System.out.println(s.toJSONString());
		
		Debugger.sleep(500);
		
		s = api.searchBestMusicItem("Immer wieder", "Selig", "", "", "");
		System.out.println(s.toJSONString());
		
		Debugger.sleep(500);
		
		s = api.searchBestMusicItem("Wichtig", "Selig", "", "", "");
		System.out.println(s.toJSONString());
		
		Debugger.sleep(500);
		*/
		
		s = api.searchBestMusicItem("Castles made", "Jimi Hendrix", "", "", "");
		System.out.println(s.toJSONString());
	}

}
