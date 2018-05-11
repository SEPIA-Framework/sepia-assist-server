package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.NormalizerAddStrongDE;
import net.b07z.sepia.server.assist.interpreters.NormalizerAddStrongEN;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSON;

public class RadioStation implements Parameter_Handler{
	
	//------data-----
	public static HashMap<String, JSONArray> radioStationsMap = new HashMap<>();
	public static HashMap<String, String> radioStationsPlaylist = new HashMap<>();
	static {
		//genre mix
		radioStationsMap.put("night", makeStationArray(JSON.make("name", "DELUXE LOUNGE RADIO", "streamURL", "http://46.245.183.120:8000/0010"),
								JSON.make("name", "ANTENNE BAYERN Chillout", "streamURL", "http://mp3channels.webradio.antenne.de/chillout"),
								JSON.make("name", "egoFM Flash", "streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmflash_192/livestream.mp3"),
								JSON.make("name", "YOU FM Club","streamURL", "http://hr-youfm-club.cast.addradio.de/hr/youfm/club/mp3/128/stream.mp3"),
								JSON.make("name", "1LIVE DJ Session", "streamURL", "http://wdr-1live-djsession.icecast.wdr.de/wdr/1live/djsession/mp3/128/stream.mp3"),
								JSON.make("name", "egoFM Soul", "streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmsoul_192/livestream.mp3"),
								JSON.make("name", "ANTENNE BAYERN Lovesongs", "streamURL", "http://mp3channels.webradio.antenne.de/lovesongs"),
								JSON.make("name", "1LIVE Fiehe", "streamURL", "http://wdr-1live-fiehe.icecast.wdr.de/wdr/1live/fiehe/mp3/128/stream.mp3")) );
		//Sport
		radioStationsMap.put("sport1", makeStationArray(JSON.make("name", "Sport1 - 24/7", "streamURL", "http://stream.sport1.fm/api/livestream-redirect/SPORT1FM_24_7.mp3"),
								JSON.make("name", "Sport1 - Spezial1", "streamURL","http://stream.sport1.fm/api/livestream-redirect/SPORT1FM_Einzel1.mp3"),
								JSON.make("name", "Sport1 - Spezial2", "streamURL","http://stream.sport1.fm/api/livestream-redirect/SPORT1FM_Einzel2.mp3"),
								JSON.make("name", "Sport1 - Spezial3", "streamURL","http://stream.sport1.fm/api/livestream-redirect/SPORT1FM_Einzel3.mp3"),
								JSON.make("name", "Sport1 - Spezial4", "streamURL","http://stream.sport1.fm/api/livestream-redirect/SPORT1FM_Einzel4.mp3"),
								JSON.make("name", "Sport1 - Spezial5", "streamURL","http://stream.sport1.fm/api/livestream-redirect/SPORT1FM_Einzel5.mp3"),
								JSON.make("name", "Sport1 - Spezial6", "streamURL","http://stream.sport1.fm/api/livestream-redirect/SPORT1FM_Einzel6.mp3")) );
		//---------
		radioStationsMap.put("deluxe_lounge", makeStationArray(JSON.make("name", "DELUXE LOUNGE RADIO", "streamURL", "http://46.245.183.120:8000/0010")) );
		radioStationsPlaylist.put("DELUXE LOUNGE RADIO", "http://onlineradiobox.com/de/radiodeluxelounge/playlist/");
		radioStationsMap.put("1live", makeStationArray(JSON.make("name","1LIVE","streamURL", "http://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3"),
								JSON.make("name", "1LIVE diGGi","streamURL","http://wdr-1live-diggi.icecast.wdr.de/wdr/1live/diggi/mp3/128/stream.mp3"),
								JSON.make("name", "1LIVE DJ Session", "streamURL", "http://wdr-1live-djsession.icecast.wdr.de/wdr/1live/djsession/mp3/128/stream.mp3"),
								JSON.make("name", "1LIVE HipHop","streamURL", "http://wdr-1live-hiphop.icecast.wdr.de/wdr/1live/hiphop/mp3/128/stream.mp3"),
								JSON.make("name", "1LIVE Plan B", "streamURL", "http://wdr-1live-planb.icecast.wdr.de/wdr/1live/planb/mp3/128/stream.mp3"),
								JSON.make("name", "1LIVE Neu für den Sektor", "streamURL", "http://wdr-1live-neufuerdensektor.icecast.wdr.de/wdr/1live/neufuerdensektor/mp3/128/stream.mp3"),
								JSON.make("name", "1LIVE Fiehe", "streamURL", "http://wdr-1live-fiehe.icecast.wdr.de/wdr/1live/fiehe/mp3/128/stream.mp3")) );
		radioStationsPlaylist.put("1LIVE", "http://www1.wdr.de/radio/1live/on-air/1live-playlist/index.html");
		radioStationsMap.put("antenne_bayern", makeStationArray(JSON.make("name","ANTENNE BAYERN","streamURL", "http://mp3channels.webradio.antenne.de:80/antenne"),
								JSON.make("name", "ANTENNE BAYERN Fresh", "streamURL", "http://mp3channels.webradio.antenne.de/fresh"),
								JSON.make("name", "ROCK ANTENNE","streamURL","http://mp3channels.webradio.rockantenne.de/rockantenne"),
								JSON.make("name", "ANTENNE BAYERN Workout Hits", "streamURL", "http://mp3channels.webradio.antenne.de/workout-hits"),
								JSON.make("name", "ANTENNE BAYERN Black Beatz", "streamURL", "http://mp3channels.webradio.antenne.de/black-beatz"),
								JSON.make("name", "ANTENNE BAYERN Chillout", "streamURL", "http://mp3channels.webradio.antenne.de/chillout"),
								JSON.make("name", "ANTENNE BAYERN Classic Rock Live", "streamURL", "http://mp3channels.webradio.antenne.de/classic-rock-live"),
								JSON.make("name", "ANTENNE BAYERN 80er Kulthits", "streamURL", "http://mp3channels.webradio.antenne.de/80er-kulthits"),
								JSON.make("name", "ANTENNE BAYERN 90er Hits","streamURL", "http://mp3channels.webradio.antenne.de/90er-hits"),
								JSON.make("name", "ANTENNE BAYERN Lovesongs", "streamURL", "http://mp3channels.webradio.antenne.de/lovesongs"),
								JSON.make("name", "ANTENNE BAYERN Alpensound", "streamURL", "http://mp3channels.webradio.antenne.de/alpensound")) );
		radioStationsPlaylist.put("ANTENNE BAYERN", "http://www.antenne.de/musik/song-suche/?station=antenne");
		radioStationsMap.put("wdr2", makeStationArray(JSON.make("name", "WDR 2 - Ruhrgebiet","streamURL", "http://wdr-wdr2-ruhrgebiet.icecast.wdr.de/wdr/wdr2/ruhrgebiet/mp3/128/stream.mp3"),
								JSON.make("name", "WDR 2 - Rheinland","streamURL", "http://wdr-wdr2-rheinland.icecast.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3"),
								JSON.make("name", "WDR 2 - Ostwestfalen-Lippe", "streamURL", "http://wdr-wdr2-ostwestfalenlippe.icecast.wdr.de/wdr/wdr2/ostwestfalenlippe/mp3/128/stream.mp3"),
								JSON.make("name", "WDR 2 - Südwestfalen", "streamURL", "http://wdr-wdr2-suedwestfalen.icecast.wdr.de/wdr/wdr2/suedwestfalen/mp3/128/stream.mp3"),
								JSON.make("name", "WDR 2 - Münsterland", "streamURL", "http://wdr-wdr2-muensterland.icecast.wdr.de/wdr/wdr2/muensterland/mp3/128/stream.mp3")));
		radioStationsPlaylist.put("WDR 2", "http://www1.wdr.de/radio/wdr2/titelsuche-wdrzwei-124.html");
		radioStationsMap.put("wdr3", makeStationArray(
								JSON.make("name", "WDR 3", "streamURL", "http://wdr-wdr3-live.icecast.wdr.de/wdr/wdr3/live/mp3/128/stream.mp3")));
		radioStationsPlaylist.put("WDR 3", "http://www1.wdr.de/radio/wdr3/titelsuche-wdrdrei-104.html");
		radioStationsMap.put("wdr4", makeStationArray(
								JSON.make("name", "WDR 4", "streamURL", "http://wdr-wdr4-live.icecast.wdr.de/wdr/wdr4/live/mp3/128/stream.mp3")));
		radioStationsPlaylist.put("WDR 4", "http://www1.wdr.de/radio/wdr4/titelsuche-wdrvier-102.html");
		radioStationsMap.put("wdr5", makeStationArray(
								JSON.make("name", "WDR 5", "streamURL", "http://wdr-wdr5-live.icecast.wdr.de/wdr/wdr5/live/mp3/128/stream.mp3")));
		radioStationsPlaylist.put("WDR 5", "http://www1.wdr.de/radio/wdr5/musik/titelsuche-wdrfuenf-104.html");
		radioStationsMap.put("swr1", null);
		radioStationsMap.put("swr2", null);
		radioStationsMap.put("swr3", null);
		radioStationsMap.put("ndr1", null);
		radioStationsMap.put("ndr2", null);
		radioStationsMap.put("ndr3", null);
		radioStationsMap.put("bayern1", null);
		radioStationsMap.put("bayern2", null);
		radioStationsMap.put("bayern3", null);
		radioStationsMap.put("hr1", null);
		radioStationsMap.put("hr2", null);
		radioStationsMap.put("hr3", null);
		radioStationsMap.put("rpr1", null);
		radioStationsMap.put("rpr2", null);
		radioStationsMap.put("ffn", null);
		radioStationsMap.put("regenbogen", null);
		radioStationsMap.put("jam_fm", null);
		radioStationsMap.put("big_fm", null);
		radioStationsMap.put("jump", null);
		radioStationsMap.put("hamburg", null);
		radioStationsMap.put("rock_antenne", null);
		radioStationsMap.put("antenne_ac", null);
		radioStationsMap.put("arabella", null);
		radioStationsMap.put("gong", null);
		radioStationsMap.put("ego_fm", makeStationArray(JSON.make("name", "egoFM Pure","streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmraw_192/livestream.mp3"),
								JSON.make("name", "egoFM","streamURL","http://mp3ad.egofm.c.nmdn.net/ps-egofm_192/livestream.mp3"),
								JSON.make("name", "egoFM Riff","streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmriff_192/livestream.mp3"),
								JSON.make("name", "egoFM Flash", "streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmflash_192/livestream.mp3"),
								JSON.make("name", "egoFM Soul", "streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmsoul_192/livestream.mp3"),
								JSON.make("name", "egoFM Rap", "streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmrap_192/livestream.mp3"),
								JSON.make("name", "egoFM Snow", "streamURL", "http://mp3ad.egofm.c.nmdn.net/ps-egofmsnow_192/livestream.mp3")) );
		radioStationsPlaylist.put("egoFM", "http://www.egofm.de/musik/play-history");
		radioStationsPlaylist.put("egoFM Pure", "http://www.egofm.de/musik/play-history-raw");
		radioStationsMap.put("ballermann", null);
		radioStationsMap.put("das_ding", null);
		radioStationsMap.put("paloma", null);
		radioStationsMap.put("you_fm", makeStationArray(JSON.make("name", "YOU FM","streamURL", "http://hr-youfm-live.cast.addradio.de/hr/youfm/live/mp3/128/stream.mp3"),
								JSON.make("name", "YOU FM Club","streamURL", "http://hr-youfm-club.cast.addradio.de/hr/youfm/club/mp3/128/stream.mp3"),
								JSON.make("name", "YOU FM Just Music", "streamURL", "http://hr-youfm-justmusic.cast.addradio.de/hr/youfm/justmusic/mp3/128/stream.mp3")));
		radioStationsPlaylist.put("YOU FM", "http://www.you-fm.de/playlist/youfm.html");
		radioStationsMap.put("rtl_104_6_berlin", null);	
		radioStationsMap.put("sunshine_live", null);
		radioStationsMap.put("charivari_party_hitmix", null);
		radioStationsMap.put("antenne_niedersachsen", null);
		radioStationsMap.put("deutschlandfunk", null);
	}
	
	private static JSONArray makeStationArray(JSONObject... stations){
		JSONArray ja = new JSONArray();
		for (JSONObject station : stations){
			JSON.add(ja,station);
		}
		return ja;
	}
	//--------

	User user;
	String language;
	boolean buildSuccess = false;
	NluInput nluInput;
	
	String found;
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nluInput = nluInput;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nluInput = nluResult.input;
	}
	
	@Override
	public String extract(String input) {
		String station = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.RADIO_STATION);
		if (pr != null){
			station = pr.getExtracted();
			this.found = pr.getFound();
			
			return station;
		}
		
		//-----------------
		//get genre first if present
		ParameterResult prGenre = ParameterResult.getResult(nluInput, PARAMETERS.MUSIC_GENRE, input);
		String genre = prGenre.getExtracted();
		/*
		//one could try and remove it
		if (!genre.isEmpty()){
			input = ParameterResult.cleanInputOfFoundParameter(nluInput, PARAMETERS.MUSIC_GENRE, prGenre, input);
		}
		*/
		
		//German
		if (language.matches("de")){
			String radio = NluTools.stringFindFirst(input, "radiokanal|radio kanal|radiostation|radio station|radiosender|radio sender|radiostream|radio stream|"
														+ "musikstation|musik station|musiksender|musik sender|kanal|sender|radio|station");
			String action1 = NluTools.stringFindFirst(input, "einschalten|anmachen|oeffnen|starten|an|ein|hoeren|spielen|aktivieren|aufdrehen");
			String action2 = NluTools.stringFindFirst(input, "oeffne|starte|spiel|spiele|aktiviere");
			//v1
			if (!radio.isEmpty() && !action1.isEmpty()){
				station = NluTools.stringFindFirst(input, radio + "\\s(.*?\\s.*?|.*?)\\s" + action1);
				station = station.replaceFirst(".*?\\b" + radio + "\\s", "").trim();
				station = station.replaceFirst("\\s" + action1 + "\\b.*", "").trim();
			}
			//v2
			if (station.trim().isEmpty() && !radio.isEmpty() && !action1.isEmpty()){
				station = NluTools.stringFindFirst(input, "\\b(.*?\\s.*?|.*?)\\s" + radio + "\\s" + action1);
				station = station.replaceFirst("\\s" + radio + "\\b.*", "").trim();
			}
			//v3
			if (station.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				station = NluTools.stringFindFirst(input, action2 + "\\s" + radio + "\\s(.*?\\s\\w+|\\w+)$");
				station = station.replaceFirst(".*?\\s" + radio + "\\s", "").trim();
			}
			//v4
			if (station.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				station = NluTools.stringFindFirst(input, action2 + "\\s" + "(.*?)\\s" + radio + "$");
				station = station.replaceFirst(".*?\\b" + action2 + "\\s", "").trim();
				station = station.replaceFirst("\\s" + radio, "").trim();
			}
			//v5
			if (station.trim().isEmpty() && !radio.isEmpty() && action1.isEmpty() && action2.isEmpty()){
				String possibleStation = NluTools.stringRemoveFirst(input, radio);
				if (NluTools.countWords(possibleStation) <= 3){
					station = possibleStation;
				}
			}
			
			//optimize
			if (!station.trim().isEmpty()){
				Normalizer normalizer = new NormalizerAddStrongDE();
				station = normalizer.normalize_text(station);
				station = station.replaceFirst(".*?\\b(einen|ein|eine|die|den|das)\\b", "").trim();
				//is just genre?
				if (station.equals(genre)){
					station = "";
				}
			}
		
		//English
		}else if (language.matches("en")){
			String radio = NluTools.stringFindFirst(input, "radiochannel|radiostation|radio station|radio channel|radiostream|radio stream|"
														+ "music station|music channel|channel|sender|radio|station");
			String action1 = NluTools.stringFindFirst(input, "on");
			String action2 = NluTools.stringFindFirst(input, "open|start|play|activate|tune in to|turn on|switch on");
			//v1
			if (!radio.isEmpty() && !action1.isEmpty()){
				station = NluTools.stringFindFirst(input, radio + "\\s(.*?\\s.*?|.*?)\\s" + action1);
				station = station.replaceFirst(".*?\\b" + radio + "\\s", "").trim();
				station = station.replaceFirst("\\s" + action1 + "\\b.*", "").trim();
			}
			//v2
			if (station.trim().isEmpty() && !radio.isEmpty() && !action1.isEmpty()){
				station = NluTools.stringFindFirst(input, "\\b(.*?\\s.*?|.*?)\\s" + radio + "\\s" + action1);
				station = station.replaceFirst("\\s" + radio + "\\b.*", "").trim();
			}
			//v3
			if (station.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				station = NluTools.stringFindFirst(input, action2 + "\\s" + radio + "\\s(.*?\\s\\w+|\\w+)$");
				station = station.replaceFirst(".*?\\s" + radio + "\\s", "").trim();
			}
			//v4
			if (station.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				station = NluTools.stringFindFirst(input, action2 + "\\s" + "(.*?)\\s" + radio + "$");
				station = station.replaceFirst(".*?\\b" + action2 + "\\s", "").trim();
				station = station.replaceFirst("\\s" + radio, "").trim();
			}
			//v5
			if (station.trim().isEmpty() && !radio.isEmpty() && action1.isEmpty() && action2.isEmpty()){
				String possibleStation = NluTools.stringRemoveFirst(input, radio);
				if (NluTools.countWords(possibleStation) <= 3){
					station = possibleStation;
				}
			}
			
			//optimize
			if (!station.trim().isEmpty()){
				Normalizer normalizer = new NormalizerAddStrongEN();
				station = normalizer.normalize_text(station);
				station = station.replaceFirst(".*?\\b(a|an|the|to)\\b", "").trim();
				//is just genre?
				if (station.equals(genre)){
					station = "";
				}
			}
		
		//no result/missing language support ...
		}else{
			//leave it empty
		}
		//System.out.println("Final station: " + station); 		//debug
		//-----------------
		
		this.found = station;
		
		//reconstruct original phrase to get proper item names
		if (!station.isEmpty()){
			Normalizer normalizer = Config.inputNormalizers.get(language);
			station = normalizer.reconstructPhrase(nluInput.text_raw, station);
		}
		
		//store it
		pr = new ParameterResult(PARAMETERS.RADIO_STATION, station, found);
		nluInput.addToParameterResultStorage(pr);
				
		return station;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}

	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		return NluTools.stringRemoveFirst(input, Pattern.quote(found));
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			input = input.replaceAll("(?i).*\\b(starte|spiele|oeffne)\\b", "").trim();
			return input.replaceAll("(?i)\\b(starten|spielen|oeffnen|hoeren)\\b", "").trim();
		}else{
			input = input.replaceAll("(?i).*\\b(start|play|open|listen to)\\b", "").trim();
			return input;
		}
	}

	@Override
	public String build(String input) {
		//optimize stations
		String station = input.replaceFirst("(?i)\\b((eins|1)( live|live|life))\\b", "1live");
		station = station.replaceFirst("(?i)\\b(ego fm)\\b", "ego FM");
		station = station.replaceFirst("(?i)\\b(sport eins|sport 1|sport1)\\b", "sport1");
		//redirect
		station = station.replaceFirst("(?i)\\b(bundesliga|fussball|sport)\\b", "sport1");
		//string to search in cache
		String chacheStationName = station.toLowerCase().replaceFirst("^(radio)\\w*|\\b(radio)\\w*$", "").replaceAll("fm$", "_fm")
				.replaceAll("\\s+(\\d)", "$1").replaceAll("(\\s+|-|\\.)", "_").replaceAll("__", "_").trim();

		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, station);
			JSON.add(itemResultJSON, InterviewData.CACHE_ENTRY, (radioStationsMap.containsKey(chacheStationName))? chacheStationName : "");
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\":.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
			//System.out.println("IS VALID: " + input); 		//debug
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}
	
}
