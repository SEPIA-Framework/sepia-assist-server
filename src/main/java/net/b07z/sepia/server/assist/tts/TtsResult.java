package net.b07z.sepia.server.assist.tts;

import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.Converters;

/**
 * The class that handles results of the TTS module. It stores all necessary data and can generate a JSON output. 
 * 
 * @author Florian Quirin
 *
 */
public class TtsResult {
	
	String status = "fail";			//result status (fail/success)
	String url = "";				//URL pointing to generated audio file
	String language = "";			//used language code (ISO 639-1)
	String gender = "";				//used gender
	String voice = "";				//used voice (set)
	int mood_index = 0;				//used mood index
	double speed = 1.0d;			//used speed factor
	double tone = 1.0d;				//used tone factor
	public HashMap<String, String> more;		//room for more
	
	public JSONObject result_JSON = new JSONObject();	//JSON result returned to client
	
	/**
	 * Constructor requires TTS_Settings to read values
	 * @param status - "success" or "fail"
	 * @param url - URL pointing to generated audio file
	 * @param settings - TTS_Settings read from TTS interface implementation
	 */
	@SuppressWarnings("unchecked")
	public TtsResult(String status, String url, TtsSettings settings){
		this.status = status;
		this.url = url;
		language = settings.language;
		gender = settings.gender;
		voice = settings.voice;
		mood_index = settings.mood;
		speed = settings.speed;
		tone = settings.tone;
		//init more
		more = new HashMap<String, String>();
		//create JSON settings
		JSONObject settingsObj = new JSONObject();
		settingsObj.put("language", language);
		settingsObj.put("gender", gender);
		settingsObj.put("voice", voice);
		settingsObj.put("mood_index", new Integer(mood_index));
		settingsObj.put("speed", new Double(speed));
		settingsObj.put("tone", new Double(tone));
		//create JSON result
		result_JSON.put("result", status);
		result_JSON.put("url", url);
		result_JSON.put("settings", settingsObj);
	}
	
	/**
	 * Get JSON result as string.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String get_result_JSON(){
		if (more != null && !more.isEmpty()){
			result_JSON.put("more", Converters.mapStrStr2Json(more));
		}
		return result_JSON.toJSONString();
	}
}
