package net.b07z.sepia.server.assist.tools;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to search iTunes media library.
 * 
 * @author Florian Quirin
 *
 */
public class ITunesApi {
	
	//https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/Searching.html#//apple_ref/doc/uid/TP40017632-CH5-SW1
	
	private static final String searchUrl = "https://itunes.apple.com/search";
	
	public static final String ENTITY_SONG = "song";
	public static final String ENTITY_ARTIST = "musicArtist";
	public static final String ENTITY_ALBUM = "album";
	public static final String ENTITY_MIX = "mix";
	//music entities:	musicArtist, musicTrack, album, musicVideo, mix, song.
	//please note that  musicTrack can include both songs and music videos in the results.
	
	public static final String ATTRIBUTE_SONG = "songTerm";
	public static final String ATTRIBUTE_ARTIST = "artistTerm";
	public static final String ATTRIBUTE_ALBUM = "albumTerm";
	public static final String ATTRIBUTE_MIX = "mixTerm";
	public static final String ATTRIBUTE_GENRE_INDEX = "genreIndex";
	//music attributes:	mixTerm, genreIndex, artistTerm, composerTerm, albumTerm, ratingIndex, songTerm
	
	//for result:
	public static final String TYPE_TRACK = "track";
	public static final String TYPE_ALBUM = "album";
	public static final String TYPE_ARTIST = "artist";
	public static final String TYPE_PLAYLIST = "playlist";
	
	//Global search parameters
	private String countryCode = "US";		//DE, US, GB, ... https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
	
	/**
	 * Initialize with certain country code (two-letter country codes defined in ISO 3166-1).
	 * @param countryCode - DE, GB, US, ...
	 */
	public ITunesApi(String countryCode){
		this.countryCode = countryCode.toUpperCase();
	}

	/**
	 * Search iTunes for music best match.
	 * @param track
	 * @param artist
	 * @param album
	 * @param playlist
	 * @param genre
	 * @return
	 */
	public JSONObject searchBestMusicItem(String track, String artist, String album, String playlist, String genre){
		//Build request
		String entity = "";
		String attribute = "";
		String term = "";
		try{
			//Search
			if (Is.notNullOrEmpty(playlist)){
				//NOTE: this is a poor workaround to find anything useful since there is no playlist search
				//entity = ENTITY_ALBUM;
				//attribute = ATTRIBUTE_ALBUM;
				//term = URLEncoder.encode(playlist, "UTF-8");
				return JSON.make(
						"message", "search type not supported",
						"search_type", "playlist",
						"query", playlist,
						"type", "no_match",
						"status", 200
				);
			
			}else if (Is.notNullOrEmpty(track)){
				entity = ENTITY_SONG;
				attribute = ATTRIBUTE_SONG;
				term = URLEncoder.encode(track, "UTF-8");
			
			}else if (Is.notNullOrEmpty(album)){
				entity = ENTITY_ALBUM;
				attribute = ATTRIBUTE_ALBUM;
				term = URLEncoder.encode(album, "UTF-8");
				
			}else if (Is.notNullOrEmpty(artist)){
				entity = ENTITY_ARTIST;
				attribute = ATTRIBUTE_ARTIST;
				term = URLEncoder.encode(artist, "UTF-8");
				
			}else if (Is.notNullOrEmpty(genre)){
				//NOTE: this is a poor workaround to find anything useful since there is no genre playlist search
				//entity = ENTITY_ALBUM;
				//attribute = ATTRIBUTE_ALBUM;
				//term = URLEncoder.encode(genre, "UTF-8");
				return JSON.make(
						"message", "search type not supported",
						"search_type", "genre",
						"query", genre,
						"type", "no_match",
						"status", 200
				);
			
			}else{
				return JSON.make("error", "combination of search keys invalid", "status", 400);
			}
			int N = 5;
			String url = searchUrl + "?media=music&entity=" + entity + "&attribute=" + attribute + "&term=" + term + "&limit=" + N + "&country=" + countryCode;
			
			//Header
			//String authString = "Bearer  " + this.token;
			
			Map<String, String> headers = new HashMap<>();
			headers.put("Accept", "application/json");
			headers.put("Content-Type", "application/json");
			//headers.put("Authorization", authString);
			
			//Call
			//System.out.println("URL: " + url); 		//DEBUG
			long tic = System.currentTimeMillis();
			JSONObject res = Connectors.httpGET(url, null, headers);
			Statistics.addExternalApiHit("iTunesApi searchBestMusicItem");
			Statistics.addExternalApiTime("iTunesApi searchBestMusicItem", tic);
			
			//Get first item
			if (Connectors.httpSuccess(res)){
				//System.out.println("Result: " + res.toJSONString()); 		//DEBUG
				
				JSONArray items = null;
				items = JSON.getJArray(res, new String[]{"results"});
				
				if (Is.nullOrEmpty(items)){
					if (Is.notNullOrEmpty(res) && res.containsKey("error")){
						JSONObject error = JSON.getJObject(res, "error");
						JSON.put(error, "type", "error");
						return error;
					}else{
						return JSON.make(
								"message", "no item found for query",
								"query", term,
								"type", "no_match",
								"status", 200
						);
					}
				}else{
					//Iterate results
					double bestMatchScore = 0.0d;
					JSONObject bestMatch = null;
					for (Object o : items){
						JSONObject item = (JSONObject) o;
						
						String kind = JSON.getString(item, "kind");
						String wrapperType = JSON.getString(item, "wrapperType");
						String trackName = JSON.getString(item, "trackName");
						String artistName = JSON.getString(item, "artistName");
						String collectionName = JSON.getString(item, "collectionName");
						String primaryGenreName = JSON.getString(item, "primaryGenreName");
						
						//SONG
						if (entity.equals(ENTITY_SONG) && (kind.equals("song") || wrapperType.equals("track"))){
							//If we have only a song name trust the first result
							if (Is.nullOrEmpty(artist) && Is.nullOrEmpty(album)){
								return JSON.make(
										"type", TYPE_TRACK, 
										"name", trackName,
										"uri", JSON.getString(item, "trackViewUrl"),
										"primary_artist", artistName,
										"album", collectionName
								);
							
							//If we have an artist or album score the result
							}else{
								double score = 0.0d;
								if (Is.notNullOrEmpty(artist)){
									score += scoreEntry(artist, artistName);
								}
								if (Is.notNullOrEmpty(album)){
									score += scoreEntry(album, collectionName);
								}
								//System.out.println("Score 2: " + score); 		//DEBUG
								if (score > bestMatchScore){
									bestMatchScore = score;
									bestMatch = JSON.make(
											"type", TYPE_TRACK, 
											"name", trackName,
											"uri", JSON.getString(item, "trackViewUrl"),
											"primary_artist", artistName,
											"album", collectionName
									);
								}
							}
						
						//ALBUM
						}else if (entity.equals(ENTITY_ALBUM) && (kind.equals("album") || wrapperType.equals("collection"))){
							double score = 0.0d;
							if (Is.notNullOrEmpty(artist)){
								score += scoreEntry(artist, artistName);
							}else{
								//If we have only an album name trust the first result
								return JSON.make(
										"type", TYPE_ALBUM, 
										"name", collectionName,
										"uri", JSON.getString(item, "collectionViewUrl"),
										"primary_artist", artistName,
										"total_tracks", JSON.getIntegerOrDefault(item, "trackCount", -1)
								);
							}
							//System.out.println("Score 2: " + score); 		//DEBUG
							if (score > bestMatchScore){
								bestMatchScore = score;
								bestMatch = JSON.make(
										"type", TYPE_ALBUM, 
										"name", collectionName,
										"uri", JSON.getString(item, "collectionViewUrl"),
										"primary_artist", artistName,
										"total_tracks", JSON.getIntegerOrDefault(item, "trackCount", -1)
								);
							}
						
						//ARTIST
						}else if (entity.equals(ENTITY_ARTIST) && (kind.equals("artist") || wrapperType.equals("artist"))){
							//We have to trust the first result
							JSONArray genres = new JSONArray();
							JSON.add(genres, primaryGenreName);
							return JSON.make(
									"type", TYPE_ARTIST, 
									"name", artistName,
									"uri", JSON.getString(item, "artistLinkUrl"),
									"genres", genres
							);
						}
					}
					if (bestMatch != null){
						return bestMatch;
					}else{
						return JSON.make(
								"message", "no item found for query",
								"query", term,
								"type", "no_match",
								"status", 200
						);
					}
					/*
					JSONObject firstItem = JSON.getJObject(items, 0);
					if (firstItem != null){
						String resType = JSON.getString(firstItem, "type");
						if (resType.equals(TYPE_PLAYLIST)){
							return JSON.make(
									"type", TYPE_PLAYLIST, 
									"name", JSON.getString(firstItem, "name"),
									"uri", JSON.getString(firstItem, "uri"),
									"owner_display_name", JSON.getObject(firstItem, new String[]{"owner", "display_name"}),
									"total_tracks", JSON.getObject(firstItem, new String[]{"tracks", "total"})
							);
					}
					*/
				}
			}
			return res;
			
		}catch (Exception e){
			Statistics.addExternalApiHit("iTunesApi-error searchBestMusicItem");
			return JSON.make("error", e.getMessage(), "status", 500);
		}
	}
	
	//Score an entry by comparing it to the search term (converted to lowerCase)
	private static double scoreEntry(String search, String found){
		//System.out.println(search + " - " + found); 		//DEBUG
		if (Is.nullOrEmpty(found)){
			return 0.0d;
		}else{
			found = found.replaceAll("(,|\\.|'|´|`|&|!|\\?)", " ").replaceAll("\\s+", " ").trim().toLowerCase();
			search = search.toLowerCase();
			double score = 1.0d - (StringCompare.editDistance(search, found) / (double) Math.min(search.length(), found.length()));
			if (score < 0.0d) score = 0.0d;
			if (found.contains(search)) score += (search.split(" ").length / (double) found.split(" ").length);
			//System.out.println("Score: " + score); 		//DEBUG
			return score;
		}
	}
}
