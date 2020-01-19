package net.b07z.sepia.server.assist.tts;

/**
 * Class representing a TTS voice trait or one specific feature of a dynamic voice (e.g. happy version of XY).
 * 
 * @author Florian Quirin
 *
 */
public class TtsVoiceTrait {
	
	private String systemName;
	private int speed;
	private int pitch;
	private int volume;
	private String type; 	//used e.g. to determine system cmd or HTTP call etc. 
	private String languageCode;
	private String genderCode;
	
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
	}

	public String getSystemName(){
		return systemName;
	}
	
	public String getType(){
		return type;
	}

	public int getSpeed(){
		return speed;
	}

	public int getPitch(){
		return pitch;
	}

	public int getVolume(){
		return volume;
	}
	
	public String getLanguageCode(){
		return languageCode;
	}

	public String getGenderCode(){
		return genderCode;
	}
	
	@Override
	public String toString(){
		return type + "-" + "-" + languageCode + "-" + genderCode + "-" + systemName;
	}
}
