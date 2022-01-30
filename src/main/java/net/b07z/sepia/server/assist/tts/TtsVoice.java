package net.b07z.sepia.server.assist.tts;

import java.util.List;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.tts.TtsTools.EngineType;
import net.b07z.sepia.server.assist.tts.TtsTools.GenderCode;

/**
 * Class representing a voice with several traits (currently: neutral, happy, sad).
 * 
 * @author Florian Quirin
 *
 */
public class TtsVoice {
	
	private String id;
	private EngineType type;
	private String name;
	private String languageCode;		//TODO: add region?
	private GenderCode genderCode;
	private List<TtsVoiceTrait> moods;
	private JSONObject data;
	
	/**
	 * Default constructor for class binding or custom setup.
	 */
	public TtsVoice(){}
	
	/**
	 * Unique voice ID (mostly for internal use).
	 */
	public String getId(){
		return id;
	}
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * A type of {@link EngineType}.
	 * @return
	 */
	public EngineType getType(){
		return type;
	}
	public void setType(EngineType type){
		this.type = type;
	}
	
	/**
	 * Readable name for interfaces.
	 */
	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * ISO language code, e.g. "en".
	 * @return
	 */
	public String getLanguageCode(){
		return languageCode;
	}
	public void setLanguageCode(String languageCode){
		this.languageCode = languageCode;
	}
	
	/**
	 * String representing the gender
	 */
	public GenderCode getGenderCode(){
		return genderCode;
	}
	public void setGenderCode(GenderCode genderCode){
		this.genderCode = genderCode;
	}
	
	/**
	 * Array of {@link TtsVoiceTrait} for different moods.
	 */
	public List<TtsVoiceTrait> getMoods(){
		return moods;
	}
	public void setMoods(List<TtsVoiceTrait> moods){
		this.moods = moods;
	}
	
	/**
	 * Additional data required for custom engine settings etc..
	 */
	public JSONObject getData(){
		return data;
	}
	public void setData(JSONObject data){
		this.data = data;
	}
	
	@Override
	public String toString(){
		return name + " - type: " + type + " - language: " + languageCode;
	}
}
