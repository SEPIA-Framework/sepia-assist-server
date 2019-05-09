package net.b07z.sepia.server.assist.tools;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_SpotifyApi {

	public static void main(String[] args) {
		
		//load custom config
		Start.loadSettings(new String[]{"--test"});
		
		SpotifyApi sapi = new SpotifyApi(Config.spotify_client_id, Config.spotify_client_secret);
		int code = sapi.getTokenViaClientCredentials();
		
		System.out.println("SpotifyApi auth. result code: " + code);
		System.out.println("Token: " + sapi.token);
		System.out.println("Token type: " + sapi.tokenType);
		System.out.println("Token expires in (ms): " + (sapi.tokenValidUntil - System.currentTimeMillis()));
		
		Debugger.sleep(1000);
		
		JSONObject s1 = sapi.searchBestItem("Paradise City", "Guns n Roses", "", "", "");
		System.out.println(s1.toJSONString());
		
		Debugger.sleep(1000);
		
		JSONObject s2 = sapi.searchBestItem("", "Guns n Roses", "Appetite For Destruction", "", "");
		System.out.println(s2.toJSONString());
		
		Debugger.sleep(1000);
		
		JSONObject s3 = sapi.searchBestItem("", "Guns n Roses", "", "", "");
		System.out.println(s3.toJSONString());
		
		Debugger.sleep(1000);
		
		JSONObject s4 = sapi.searchBestItem("", "", "", "", "Rock");
		System.out.println(s4.toJSONString());
		
		Debugger.sleep(1000);
		
		JSONObject s5 = sapi.searchBestItem("", "", "", "Party", "");
		System.out.println(s5.toJSONString());
		
		Debugger.sleep(1000);
		
		JSONObject s6 = sapi.searchBestItem("Foxy Lady", "Jimi Hendrix", "", "", "");	//note the typo in foxy (not foxey)
		System.out.println(s6.toJSONString());
	}

}
