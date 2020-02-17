package net.b07z.sepia.server.assist.tts;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.ContentBuilder;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * IMPORTANT: This is an implementation of the TTS interface that is actually calling THIS server. So if you use that
 * as the default TTS service and load it to the server you end up in an endless loop ;-)
 * It is meant to be used as an example for other clients or different servers.
 * <br><br>
 * This is the interface implementation calling the SEPIA TTS server itself.<br>
 * Note that there is a certain order in which to call the settings to avoid overwriting parameters:<br><br>
 * setLanguage -> setGender -> setVoice -> setMood -> setSpeedFactor -> setToneFactor <br>
 * <br>
 * Note as well that smileys in a text will have a higher priority and overwrite mood :-)
 * 
 * @author Florian Quirin
 *
 */
public class TtsSepiaWeb implements TtsInterface{
	
	//server
	private String server = "http://localhost:20721/";		//TODO: get from config
	private String key = "";								//TODO: make variable and get from user or config
	
	//client and environment
	private String clientInfo = "java_app_v2.0.0";
	
	//default settings
	private String language = "en";
	private String gender = "male";
	private String activeVoice = "enu_will";		//name of voice set as seen in get_voices
	private int moodIndex = 0;
	private double speedFactor = 1.0;
	private double toneFactor = 1.0;
	
	public String environment = "";		//client environment
	
	//support lists
	ArrayList<String> voiceList;
	ArrayList<String> genderList;
	ArrayList<String> languageList;
	ArrayList<String> soundFormatList;
	String soundFormat = "default";
	int maxMoodIndex = 0;
	int charLimit = 600;
	
	//CONSTRUCTOR
	public TtsSepiaWeb(){
		//get TTS service info - is this too early here?
		getInfo();
	}
	
	@Override
	public boolean setup(){
		return true;
	}
	
	//set TTS input and check it for special stuff like environment based format (web-browser != android app)
	public void setInput(TtsInput input){
		if (input != null){
			this.environment = input.environment;
		}
	}
	
	//play sound on server?
	public boolean supportsPlayOnServer(){
		return false;
	}
	
	//return supported sound formats
	public ArrayList<String> getSoundFormats(){
		return soundFormatList;
	}
	//return active sound format
	public String getActiveSoundFormat(){
		return soundFormat;
	}
	//set sound format
	public String setSoundFormat(String format){
		if (format != null && !format.isEmpty() && soundFormatList.contains(format) ){
			soundFormat = format;
		}
		return soundFormat;
	}
	
	//set language
	public boolean setLanguage(String language) {
		if (languageList != null && languageList.contains(language)){
			this.language = language;
			setVoice("default");
			return true;
		}else{
			return false;
		}
	}
	//set voice
	public boolean setVoice(String voice) {
		if (voiceList != null && (voiceList.contains(voice) || voice.matches("default")) ){
			this.activeVoice = voice;
			return true;
		}else{
			return false;
		}
	}
	//set gender
	public boolean setGender(String gender) {
		if (genderList != null && genderList.contains(gender)){
			this.gender = gender;
			setVoice("default");	//overwrite voice with "default" to avoid reassignment and gender change
			return true;
		}else{
			return false;
		}
	}
	//set mood index: (0-neutral, 1-happy, 2-sad/excusing)
	public boolean setMood(int mood) {
		if (maxMoodIndex != 0 && mood < maxMoodIndex){
			this.moodIndex = mood;
			return true;
		}else{
			return false;
		}
	}

	//set speed of currently selected voice
	public void setSpeedFactor(double speed_factor) {
		this.speedFactor = speed_factor;
	}
	//set tone of currently selected voice
	public void setToneFactor(double tone_factor) {
		this.toneFactor = tone_factor;
	}

	//connect to SEPIA server and get URL to Mp3 file
	public String getAudioURL(String message) {
		
		//convert mood_index back to mood - mood is actually a 10 steps state, currently (0-2) is "sad" (3-7) is "neutral" and (8-10) is "happy" 
		int mood = 5;
		if (moodIndex == 1){		mood = 10;	}
		else if (moodIndex == 2){	mood = 0;	}	
		
		//make URL and connect to service
		try {			
			//run process - note: its thread blocking but this should be considered "intended" here ;-)
			JSONObject response = Connectors.httpFormPOST(server + "tts", 
					ContentBuilder.postForm("KEY", key, "client", clientInfo,
							"text", message.trim(),
							"lang", language,
							"env", environment,
							"voice", activeVoice,
							"gender", gender,
							"mood", String.valueOf(mood),
							"speed", String.valueOf(speedFactor),
							"tone", String.valueOf(toneFactor),
							"format", soundFormat
					));
			//System.out.println(response.toJSONString());	//debug
			//check HTTP GET result
			if (!Connectors.httpSuccess(response)){
				Debugger.println("TTS - NOT AUTHORIZED OR CONNECTION FAILED!", 1);
				return "";
			}
			//check JSON result
			String status = (String) response.get("result");
			String audioURL = "";
			if (status.matches("success")){
				audioURL = (String) response.get("url");
				//read out used settings
				JSONObject settings = (JSONObject) response.get("settings");
				if (settings != null){
					language = (String) settings.get("language");
					gender = (String) settings.get("gender");
					activeVoice = (String) settings.get("voice");
					moodIndex = Integer.parseInt(settings.get("mood_index").toString());
					speedFactor = Double.parseDouble(settings.get("speed").toString());
					toneFactor = Double.parseDouble(settings.get("tone").toString());
					//more
					JSONObject more = (JSONObject) settings.get("more");
					if (more != null){
						soundFormat = (String) more.get("soundFormat");
					}
				}
			}
			
			return audioURL;
			
		//error - return empty string	
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());
			//throw e;
			return "";
		}
	}

	//connect to server and get tts service info
	public void getInfo() {
		//make URL and connect to service
		try {
			//run process - note: its thread blocking but this should be considered "intended" here ;-) 
			JSONObject response = Connectors.httpFormPOST(server + "tts-info", 
					ContentBuilder.postForm("KEY", key, "client", clientInfo));
			//System.out.println(response.toJSONString());		//debug
			//check HTTP GET result
			if (!Connectors.httpSuccess(response)){
				Debugger.println("TTS - NOT AUTHORIZED OR CONNECTION FAILED!", 1);
			}
			
			String status = (String) response.get("result");
			JSONArray voicesArray, genderArray, languagesArray, formatsArray;
			ArrayList<String> vList = new ArrayList<String>();
			ArrayList<String> gList = new ArrayList<String>();
			ArrayList<String> lList = new ArrayList<String>();
			ArrayList<String> sfList = new ArrayList<String>();
			if (status.matches("success")){
				voicesArray = (JSONArray) response.get("voices");
				for (Object o : voicesArray){
					vList.add((String) o);
				}
				genderArray = (JSONArray) response.get("genders");
				for (Object o : genderArray){
					gList.add((String) o);
				}
				languagesArray = (JSONArray) response.get("languages");
				for (Object o : languagesArray){
					lList.add((String) o);
				}
				formatsArray = (JSONArray) response.get("formats");
				for (Object o : formatsArray){
					sfList.add((String) o);
				}
				voiceList = vList;
				genderList = gList;
				languageList = lList;
				soundFormatList = sfList;
				maxMoodIndex = Integer.parseInt(response.get("maxMoodIndex").toString());
				charLimit = Integer.parseInt(response.get("maxChunkLength").toString());
			}
			
		//error	
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());
			//throw e;
		}
	}
	
	//connect to server and get voice list
	public ArrayList<String> getVoices() {
		if (voiceList == null && genderList == null && languageList == null){
			getInfo();
		}
		return voiceList;
	}
	
	//connect to server and get language list
	public ArrayList<String> getLanguages() {
		if (voiceList == null && genderList == null && languageList == null){
			getInfo();
		}
		return languageList;
	}
	
	//connect to server and get gender list
	public ArrayList<String> getGenders() {
		if (voiceList == null && genderList == null && languageList == null){
			getInfo();
		}
		return genderList;
	}
	
	//connect to server and get max mood index
	public int getMaxMoodIndex() {
		if (voiceList == null && genderList == null && languageList == null){
			getInfo();
		}
		return maxMoodIndex;
	}
	
	@Override
	public int getMaxChunkLength(){
		return charLimit;
	}
	
	//Settings SET
	public void setSettings(TtsSettings settings) {
		/* - setLanguage
		 * - setGender
		 * - setVoice
		 * - setMood (using mood_index 0:neutral, 1:happy, 2:sad, 3:angry, ...)
		 * - setSpeedFactor
		 * - setToneFactor
		 */
		if (!settings.language.isEmpty())	setLanguage(settings.language);
		if (!settings.gender.isEmpty())		setGender(settings.gender);
		if (!settings.voice.isEmpty())		setVoice(settings.voice);
		if (settings.mood > -1)				setMood(settings.mood);
		if (settings.speed > 0)				setSpeedFactor(settings.speed);
		if (settings.tone > 0)				setToneFactor(settings.tone);
	}
	//Settings GET
	public TtsSettings getSettings() {
		TtsSettings settings = new TtsSettings();
		settings.language = language;
		settings.gender = gender;
		settings.voice = activeVoice;
		settings.mood = moodIndex;
		settings.speed = speedFactor;
		settings.tone = toneFactor;
		//settings.volume = volume_set[mood_index];
		return settings;
	}
}
