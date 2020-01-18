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
	
	public TtsVoiceTrait(String systemName, String type, int speed, int pitch, int volume){
		this.systemName = systemName;
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
}
