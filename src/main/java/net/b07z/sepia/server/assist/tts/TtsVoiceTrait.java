package net.b07z.sepia.server.assist.tts;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.Is;

/**
 * Class representing a TTS voice trait or one specific feature of a dynamic voice (e.g. happy version of XY).
 * 
 * @author Florian Quirin
 *
 */
public class TtsVoiceTrait {
	
	private String systemName;
	private int moodIndex;	//0: neutral, 1: happy, 2: sad
	private int speed;
	private int pitch;
	private int volume;
	private String type; 	//used e.g. to determine system cmd or HTTP call etc. 
	private String languageCode;
	private String genderCode;
	private JSONObject data;
	
	/**
	 * Default constructor for class binding or custom setup.
	 */
	public TtsVoiceTrait(){}
	
	/**
	 * Create a voice trait aka a voice object used for certain properties of a complete voice object (e.g. happy version of XY). 
	 * @param systemName - name given by the system to identify the voice
	 * @param type - type of system or engine, e.g. "espeak"
	 * @param langCode - simple language code (2 letters)
	 * @param genCode - gender code (male, female, x?)
	 * @param speed - default speed of voice
	 * @param pitch - default pitch of voice
	 * @param volume - default volume of voice
	 */
	public TtsVoiceTrait(String systemName, String type, String langCode, String genCode, int speed, int pitch, int volume){
		this.systemName = systemName;
		this.type = type;
		this.languageCode = langCode;
		this.genderCode = genCode;
		this.speed = speed;
		this.pitch = pitch;
		this.volume = volume;
		this.data = new JSONObject();
	}
	/**
	 * Create a voice trait aka a voice object used for certain properties of a complete voice object (e.g. happy version of XY). 
	 * @param systemName - name given by the system to identify the voice
	 * @param type - type of system or engine, e.g. "marytts"
	 * @param langCode - simple language code (2 letters)
	 * @param genCode - gender code (male, female, x?)
	 * @param data - any service specific data that might be required
	 */
	public TtsVoiceTrait(String systemName, String type, String langCode, String genCode, JSONObject data){
		this.systemName = systemName;
		this.type = type;
		this.languageCode = langCode;
		this.genderCode = genCode;
		this.data = data;
	}
	
	/**
	 * Fill some info with defaults from voice.
	 */
	public void fillDefaults(TtsVoice voice){
		if (Is.nullOrEmpty(this.type)) this.type = voice.getType().name(); 
		if (Is.nullOrEmpty(this.languageCode)) this.languageCode = voice.getLanguageCode();
		if (Is.nullOrEmpty(this.genderCode)) this.genderCode = voice.getGenderCode().name();
	}
	
	public TtsVoiceTrait setData(JSONObject data){
		this.data = data;
		return this;
	}
	public JSONObject getData(){
		return this.data;
	}

	/**
	 * Name of the voice, usually given by voice engine.
	 */
	public String getSystemName(){
		return systemName;
	}
	public void setSystemName(String systemName){
		this.systemName = systemName;
	}
	
	/**
	 * Mood index 0 (neutral), 1 (happy), 2 (sad), ... tbd..
	 */
	public int getMoodIndex(){
		return moodIndex;
	}
	public void setMoodIndex(int moodIndex){
		this.moodIndex = moodIndex;
	}
	
	/**
	 * Type of voice aka voice engine (e.g. 'espeak').
	 */
	public String getType(){
		return type;
	}
	public void setType(String type){
		this.type = type;
	}

	public int getSpeed(){
		return speed;
	}
	public void setSpeed(int speed){
		this.speed = speed;
	}

	public int getPitch(){
		return pitch;
	}
	public void setPitch(int pitch){
		this.pitch = pitch;
	}

	public int getVolume(){
		return volume;
	}
	public void setVolume(int volume){
		this.volume = volume;
	}
	
	/**
	 * ISO language code like "en".
	 * @return
	 */
	public String getLanguageCode(){
		return languageCode;
	}
	public void setLanguageCode(String languageCode){
		this.languageCode = languageCode;
	}

	/**
	 * One of {@link TtsTools.GenderCode}.
	 */
	public String getGenderCode(){
		return genderCode;
	}
	public void setGenderCode(String genderCode){
		this.genderCode = genderCode;
	}
	
	@Override
	public String toString(){
		return type + "-" + "-" + languageCode + "-" + genderCode + "-" + systemName;
	}
}
