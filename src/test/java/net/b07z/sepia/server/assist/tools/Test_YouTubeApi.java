package net.b07z.sepia.server.assist.tools;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_YouTubeApi {

	public static void main(String[] args) throws Exception {
		
		//load custom config
		Start.loadSettings(new String[]{"--test"});
				
		JSONObject s1 = YouTubeApi.searchVideoForEmbedding("Queen Playlist", 3);
		JSON.prettyPrint(s1);
		
		Debugger.sleep(500);
		
		JSONObject s2 = YouTubeApi.searchVideoForEmbedding("Jimi Hendrix All along the watchtower", 3);
		JSON.prettyPrint(s2);
	}

}
