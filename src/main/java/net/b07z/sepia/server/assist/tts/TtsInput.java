package net.b07z.sepia.server.assist.tts;

import java.util.HashMap;

/**
 * Use this to generate the input for any TTS-Processor.
 * You can modify things like language, mood, voice, speed, transformation (pitch) etc.
 * 
 * @author Florian Quirin
 *
 */
public class TtsInput {

	//main input	-						API param /	Description
	public String text = "";				//:text:	input text/question/query
	public String language = "en";			//lang: 	language used for interpretation and results	
	public String context = "default";		//context:	context is what the user did/said before to answer queries like "do that again" or "and in Berlin?"
	public String environment = "default";	//env:		environments can be stuff like home, car, phone etc. to restrict and tweak some results
	public String client_info = "default";	//client:	information about client and client version
	public int mood = -1;					//mood:		mood value 0-10 of ILA (or whatever the name of the assistant is) can be passed around too, 10 is the best. Used e.g. in get_answers and TTS
	//TTS specific
	public String voice = "default";		//voice:	Name of the voice that should be used (when you don't want the default)
	public String gender = "default";		//gender:	toggle between default voices for "male", "female", "child", "old" and "creature"
	public double speed = -1.0d;			//speed:	Adjust voice speed, default is 180 (Acapela), -1 means no change
	public double tone = -1.0d;				//tone:		Adjust voice tone, default is 100 (Acapela), -1 means no change
	//account and access rights
	public int access_lvl = 0;				//level of access depending on user account authentication
	public int user_id = 0;					//unique id of user account
	public String encrypt_key = "";			//might be useful to encrypt/decrypt user account data, could be generated from password
	//device and format
	public String playOn = "client";		//playOn:	play generated sound file on "client" or "server"
	public String format = "default";		//format:	use this sound format
	//... more to come
	public HashMap<String, Object> more;	//anything else here ...
	
	public TtsInput(){
	}
	/**
	 * Create input.
	 * @param text - input text/question/query ...
	 */
	public TtsInput(String text){
		this.text = text;
		this.more = new HashMap<String, Object>();
	}
	/**
	 * Create input.
	 * @param text - input text/question/query ...
	 * @param language - language used to interpret text and inside APIs
	 */
	public TtsInput(String text, String language){
		this.text = text;
		this.language = language;
		this.more = new HashMap<String, Object>();
	}
	/**
	 * Create input.
	 * @param text - input text/question/query ...
	 * @param language - language used to interpret text and inside APIs
	 * @param context - context of a command like "do that again"
	 * @param mood - mood level 0-10 (10: super happy) of the assistant (may change answers ^^)
	 * @param environment - environments like home, car, mobile, watch ...
	 */
	public TtsInput(String text, String language, String context, int mood, String environment){
		this.text = text;
		this.language = language;
		this.context = context;
		this.mood = mood;
		this.environment = environment;
		this.more = new HashMap<String, Object>();
	}
	
	/**
	 * Create input.
	 * @param text - input text/question/query ...
	 * @param language - language used to interpret text and inside APIs
	 * @param context - context of a command like "do that again"
	 * @param mood - mood level 0-10 (10: super happy) of the assistant (may change answers ^^)
	 * @param environment - environments like home, car, mobile, watch ...
	 * @param voice - voice to be used
	 * @param gender - gender to use (overwrites voice by setting default voice for gender)
	 * @param speed - voice speed modifier
	 * @param tone - voice tone modifier
	 */
	public TtsInput(String text, String language, String context, int mood, String environment, String voice, String gender, double speed, double tone){
		this.text = text;
		this.language = language;
		this.context = context;
		this.mood = mood;
		this.environment = environment;
		this.voice = voice;
		this.gender = gender;
		this.speed = speed;
		this.tone = tone;
		this.more = new HashMap<String, Object>();
	}
}
