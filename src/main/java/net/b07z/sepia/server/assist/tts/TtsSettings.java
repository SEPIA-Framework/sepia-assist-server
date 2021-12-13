package net.b07z.sepia.server.assist.tts;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to easily get and set settings for TTS.
 * 
 * @author Florian Quirin
 *
 */
public class TtsSettings {
	
	public String voice = "";
	public String language = "";
	public String gender = "";
	public int mood = -1;
	public double speed = -1.0;
	public double tone = -1.0;
	public double volume = -1.0;		//not implemented!(?)

	/**
	 * Load voices from JSON settings to map.
	 * @param settingsJson - TTS settings
	 * @return
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	public static Map<String, TtsVoice> loadVoicesFromSettings(JSONObject settingsJson) 
			throws JsonMappingException, JsonProcessingException {
		
		Map<String, TtsVoice> voicesMap = new HashMap<>();
				
		ObjectMapper objectMapper = new ObjectMapper();
		JSONObject voices = JSON.getJObject(settingsJson, "voices");
		for (Object voiceIdObj : voices.keySet()){
			String id = (String) voiceIdObj;
			TtsVoice voice = objectMapper.readValue(JSON.getString(voices, id), TtsVoice.class);
			voicesMap.put(id, voice);
		}
		
		return voicesMap;
	}
}
