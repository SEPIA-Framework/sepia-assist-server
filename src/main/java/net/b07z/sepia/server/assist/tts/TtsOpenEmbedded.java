package net.b07z.sepia.server.assist.tts;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.RuntimeInterface;
import net.b07z.sepia.server.core.tools.Security;
import net.b07z.sepia.server.core.tools.ThreadManager;
import net.b07z.sepia.server.core.tools.URLBuilder;
import net.b07z.sepia.server.core.tools.RuntimeInterface.RuntimeResult;

/**
 * This is the TTS interface implementation for embedded Linux tools like espeak, flite, picotts etc..
 * 
 * @author Florian Quirin
 *
 */
public class TtsOpenEmbedded implements TtsInterface {
	
	public static enum Type {
		espeak,
		flite,
		pico,
		marytts
	}
	
	//support lists
	private static Map<String, TtsVoiceTrait[]> voices = new HashMap<>();
	private static List<String> genderList = new ArrayList<String>();
	private static List<String> languageList = new ArrayList<String>();
	private static List<String> soundFormatList = new ArrayList<String>();
	private static int maxMoodIndex = 0;
	
	//voices
	
	//espeak de-DE male (default, happy, sad)
	private static TtsVoiceTrait deDE_espeak_m1_0 = new TtsVoiceTrait("gmw/de", Type.espeak.name(), "de", "male", 160, 50, 100);
	private static TtsVoiceTrait deDE_espeak_m1_1 = new TtsVoiceTrait("gmw/de", Type.espeak.name(), "de", "male", 160, 60, 100);
	private static TtsVoiceTrait deDE_espeak_m1_2 = new TtsVoiceTrait("gmw/de", Type.espeak.name(), "de", "male", 160, 30, 100);
	//marytts de-DE
	private static TtsVoiceTrait deDE_marytts_m1_0 = new TtsVoiceTrait("bits3-hsmm", Type.marytts.name(), "de", "male", 
			JSON.make("LOCALE", "de")
	);
	private static TtsVoiceTrait deDE_marytts_f1_0 = new TtsVoiceTrait("bits1-hsmm", Type.marytts.name(), "de", "female", 
			JSON.make("LOCALE", "de", "effect_FIRFilter_parameters", "type:4;fc1:240.0;fc2:1000.0;tbw:1000.0", "effect_FIRFilter_selected", "on")
	);
	private static TtsVoiceTrait deDE_marytts_s1_0 = new TtsVoiceTrait("bits3-hsmm", Type.marytts.name(), "de", "male", 
			JSON.make("LOCALE", "de", "effect_Robot_selected=", "on", "effect_Robot_parameters", "amount:100.0;")
	);
	
	//espeak en-GB male (default, happy, sad)
	private static TtsVoiceTrait enGB_espeak_m1_0 = new TtsVoiceTrait("gmw/en", Type.espeak.name(), "en", "male", 160, 50, 100);
	private static TtsVoiceTrait enGB_espeak_m1_1 = new TtsVoiceTrait("gmw/en", Type.espeak.name(), "en", "male", 160, 60, 100);
	private static TtsVoiceTrait enGB_espeak_m1_2 = new TtsVoiceTrait("gmw/en", Type.espeak.name(), "en", "male", 160, 30, 100);
	//marytts en-GB
	private static TtsVoiceTrait enGB_marytts_m1_0 = new TtsVoiceTrait("dfki-spike-hsmm", Type.marytts.name(), "en", "male", 
			JSON.make("LOCALE", "en_GB", 
					"effect_F0Scale_selected", "on", "effect_F0Scale_parameters", "f0Scale:0.66;",
					"effect_F0Add_selected", "on", "effect_F0Add_parameters", "f0Add:-15.0;")
	);
	private static TtsVoiceTrait enGB_marytts_f1_0 = new TtsVoiceTrait("dfki-prudence-hsmm", Type.marytts.name(), "en", "female", 
			JSON.make("LOCALE", "en_GB", 
					"effect_F0Scale_selected", "on", "effect_F0Scale_parameters", "f0Scale:0.33;",
					"effect_F0Add_selected", "on", "effect_F0Add_parameters", "f0Add:-50.0;")
	);
	private static TtsVoiceTrait enGB_marytts_s1_0 = new TtsVoiceTrait("dfki-prudence-hsmm", Type.marytts.name(), "en", "female", 
			JSON.make("LOCALE", "en_GB", "effect_Robot_selected=", "on", "effect_Robot_parameters", "amount:100.0;")
	);
	
	//track files
	private static int MAX_FILES = 30;
	private static long PROCESS_TIMEOUT_MS = 5000;
	private static long CLEAN_UP_DELAY_MS = 120000;
	private static AtomicInteger fileID = new AtomicInteger(0);
	private static String ttsOutFolder = Config.ttsWebServerPath;
	
	//file clean-up
	Queue<File> fileCleanUpQueue = new ConcurrentLinkedQueue<>();
	Runnable cleanUpTask = () -> {
		File f = fileCleanUpQueue.poll();
		cleanUpFile(f);
		int n = fileCleanUpQueue.size();
		if (n > MAX_FILES * 10){
			Debugger.println(TtsOpenEmbedded.class.getSimpleName() + " - TTS clean-up queue is too large (" + n + ")! Deactivating TTS until server restart.", 1);
			Config.ttsModuleEnabled = false;
		}
	};
	private static void cleanUpFile(File f){
		if (f != null && f.exists()){
			try{
				f.delete();
				Debugger.println(TtsOpenEmbedded.class.getSimpleName() + " - cleaned up file: " + f.getName(), 2);
			}catch (Exception e){
				Debugger.println(TtsOpenEmbedded.class.getSimpleName() + " - error in clean-up task: " + e.getMessage(), 1);
			}
		}
	}
        
    //defaults
    private String language = "en";
	private String gender = "male";
	private String activeVoice = "en-GB espeak m";		//name of voice set as seen in get_voices (not necessarily the same as the actual selected voice (enu_will != will22k)
	private int mood_index=0;			//0 - neutral/default, 1 - happy, 2 - sad, 3 - angry, 4 - shout, 5 - whisper, 6 - fun1 (e.g. old), 7 - fun2 (e.g. Yoda)
	private double speedFactor = 1.0d;	//global modifier - multiply speed with this 	
	private double toneFactor = 1.0d;	//global modifier - multiply tone with this
	
	int charLimit = 600;				//limit text length
	String soundFormat = "WAV";
	
	public String client = "";			//client info
	public String environment = "";		//client environment
	
	public TtsOpenEmbedded(){}
	
	@Override
	public boolean setup(){
		//supported languages:
		languageList.add("de");
		languageList.add("en");
		
		//supported sound formats
		soundFormatList.add("WAV");
		
		//supported genders:
		genderList.add("male");
		//genderList.add("female");
		
		//supported voices:
		if (Is.systemWindows()){
			//TODO: test if voices are really there
		}else{
			//TODO: test if voices are really there
		}
		voices.put("de-DE espeak m", new TtsVoiceTrait[]{ deDE_espeak_m1_0, deDE_espeak_m1_1, deDE_espeak_m1_2 });
		voices.put("en-GB espeak m", new TtsVoiceTrait[]{ enGB_espeak_m1_0, enGB_espeak_m1_1, enGB_espeak_m1_2 });
		voices.put("de-DE marytts m", new TtsVoiceTrait[]{ deDE_marytts_m1_0, deDE_marytts_m1_0, deDE_marytts_m1_0 });
		voices.put("de-DE marytts f", new TtsVoiceTrait[]{ deDE_marytts_f1_0, deDE_marytts_f1_0, deDE_marytts_f1_0 });
		voices.put("de-DE marytts r", new TtsVoiceTrait[]{ deDE_marytts_s1_0, deDE_marytts_s1_0, deDE_marytts_s1_0 });
		voices.put("en-GB marytts m", new TtsVoiceTrait[]{ enGB_marytts_m1_0, enGB_marytts_m1_0, enGB_marytts_m1_0 });
		voices.put("en-GB marytts f", new TtsVoiceTrait[]{ enGB_marytts_f1_0, enGB_marytts_f1_0, enGB_marytts_f1_0 });
		voices.put("en-GB marytts r", new TtsVoiceTrait[]{ enGB_marytts_s1_0, enGB_marytts_s1_0, enGB_marytts_s1_0 });
		
		//supported maximum mood index
		maxMoodIndex = 3;
		
		//clean-up 'tts' folder
		try{
			List<File> ttsFiles = FilesAndStreams.directoryToFileList(Config.ttsWebServerPath, null, false);
			int i = 0;
			for (File f : ttsFiles){
				if (!f.getName().equalsIgnoreCase("no-index") && !f.getName().toLowerCase().startsWith("readme")){
					f.delete();
					i++;
				}
			}
			Debugger.println("TTS module setup has cleaned up '" + i + "' leftover files.", 3);
			
		}catch (Exception e){
			Debugger.println("TTS module setup error during folder clean-up: " + e.getMessage(), 1);
			return false;
		}
		
		return true;
	}
	
	//set TTS input and check it for special stuff like environment based format (web-browser != android app)
	public void setInput(TtsInput input){
		if (input != null){
			this.environment = input.environment;
			this.client = input.client_info;
		}
	}
	
	//play sound on server?
	public boolean supportsPlayOnServer(){
		return false;	//TODO: not yet, but we can implement this
	}
	
	//return supported sound formats
	public Collection<String> getSoundFormats(){
		return soundFormatList;
	}
	//return active (last used) sound format
	public String getActiveSoundFormat(){
		return soundFormat;
	}
	//set sound format
	public String setSoundFormat(String format){
		return soundFormat;
	}
	
	//return list with voice names
	public Collection<String> getVoices() {
		return voices.keySet();
	}
	
	//return list with available languages
	public Collection<String> getLanguages(){
		return languageList;
	}
	
	//return list with available languages
	public Collection<String> getGenders(){
		return genderList;
	}
	
	//return max mood index
	public int getMaxMoodIndex(){
		return maxMoodIndex;
	}
	
	@Override
	public int getMaxChunkLength(){
		return charLimit;
	}
	
	//set language and default voice sets (voice, speed, tune, vol)
	public boolean setLanguage(String language) {
		if (language.matches(LANGUAGES.DE)){
			setVoice("de-DE espeak m");
			this.language = language;
			return true;
		}else if (language.matches(LANGUAGES.EN)){
			setVoice("en-GB espeak m");
			this.language = language;
			return true;
		}else{
			this.language = "";
			return false;
		}
	}
	
	//set a voice according to default gender selection
	public boolean setGender(String gender) {
		if (language.matches(LANGUAGES.DE)){
			if (gender.matches("male")){
				setVoice("de-DE espeak m");
				this.gender = gender;
				return true;
			}
		}else if (language.matches(LANGUAGES.EN)){
			if (gender.matches("male")){
				setVoice("en-GB espeak m");
				this.gender = gender;
				return true;
			}
		}else{
			this.gender = "";
		}
		return false;
	}
	
	//sets the voice set with voice name, speed and tone.
	public boolean setVoice(String voiceName) {
		//new voice
		if (voices.containsKey(voiceName)){
			this.activeVoice = voiceName;
			return true;
		//default
		}else{
			this.activeVoice = "";
			return false;
		}
	}
	
	//sets the mood index to choose the emotional voice.
	public boolean setMood(int mood) {
		if (mood > maxMoodIndex || mood < 0){
			this.mood_index = 0;
			return false;
		}else{
			this.mood_index = mood;
			return true;
		}
	}

	//sets the speed to a new value by the specific factor
	public void setSpeedFactor(double speed_factor) {
		/*
		for (int i=0; i<speed_set.length; i++){
			speed_set[i] = (int) (speed_factor * speed_set[i]);
		}*/
		/*
		speedFactor = speed_set[mood_index] * speed_factor;
		*/
		this.speedFactor = speed_factor;
	}

	//sets the tone to a new value by the specific factor
	public void setToneFactor(double tone_factor) {
		/*
		for (int i=0; i<tone_set.length; i++){
			tone_set[i] = (int) (tone_factor * tone_set[i]);
		}*/
		/*
		tone_set[mood_index] = (int) (tone_factor * 100);
		*/
		this.toneFactor = tone_factor;
	}
	
	//create sound file from text
	public String getAudioURL(String readThis) {
		
		//characters limit
		if (readThis.length() > charLimit){
			readThis = readThis.substring(0, charLimit);
		}
		
		//set mood - sets mood_index and overwrites the automatic mood tracking (emojis have higher priority than input.mood)
		int modMoodIndex = TtsTools.getMoodIndex(readThis, mood_index);
		if (modMoodIndex > maxMoodIndex) modMoodIndex = 0;
		//set language	- sets language and default Sets (voice, tone, speed, volume)
		//set gender	- sets male/female/creature ^^ overwrites Sets
		//set voice		- overwrites all (except mood_index) and sets voice name directly (with corresponding Sets)
		
		//trim text - removing emojis etc.
		readThis = TtsTools.trimText(readThis);
		
		//optimize pronunciation
		readThis = TtsTools.optimizePronunciation(readThis, language);
		
		// - get new ID and prepare files
		int ID = fileID.addAndGet(1);
		if (ID >= MAX_FILES){
			fileID.set(1);
		}
		String audioFileName = ID + "-speak-" + Security.getRandomUUID().replace("-", "") + ".wav";
		String audioFilePath = ttsOutFolder + audioFileName;
		
		try{
			//set parameters
			TtsVoiceTrait voiceTrait = voices.get(this.activeVoice)[modMoodIndex];		//in theory this cannot fail because all has been validated before
			Debugger.println("TTS LOG - Voice trait: " + voiceTrait, 2);		//debug
			//update for endpoint
			this.language = voiceTrait.getLanguageCode();
			this.gender = voiceTrait.getGenderCode();
			
			//create files:
						
			// - try to write file
			String audioURL = Config.ttsWebServerUrl + audioFileName;
			Debugger.println("TTS LOG - URL: " + audioURL, 2);		//debug
			
			//build command line action
			boolean generatedFile;
			//ESPEAK
			if (voiceTrait.getType().equals(Type.espeak.name())){
				//run process - note: its thread blocking but this should be considered "intended" here ;-)
				String[] command = buildEspeakCmd(readThis, voiceTrait, this.speedFactor, this.toneFactor, audioFilePath);
				generatedFile = runRuntimeProcess(command, audioFilePath);
			//MARY-TTS
			}else if (voiceTrait.getType().equals(Type.marytts.name())){
				//call server
				String url = buildMaryTtsUrl(readThis, voiceTrait, this.speedFactor, this.toneFactor);
				generatedFile = callServerProcess(url, audioFilePath);
			//UNKNOWN
			}else{
				throw new RuntimeException("TTS voice type not known: " + voiceTrait.getType());
			}
			//successfully generated file?
			if (generatedFile){
				//Success
				fileCleanUpQueue.add(new File(audioFilePath));
				ThreadManager.scheduleTaskToRunOnceInBackground(CLEAN_UP_DELAY_MS, cleanUpTask);
				return audioURL;
			}else{
				//Error (failed without exception)
				cleanUpFile(new File(audioFilePath));		//make sure its not left over
				return "";
			}

		//ERROR
		}catch (Exception e){
			Debugger.printStackTrace(e, 4);
			Debugger.println("TTS FAILED: " + e.toString(), 1);
			cleanUpFile(new File(audioFilePath));		//make sure its not left over
			return "";
		}
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
		settings.language = this.language;
		settings.gender = this.gender;
		settings.voice = this.activeVoice;
		settings.mood = this.mood_index;
		settings.speed = this.speedFactor;
		settings.tone = this.toneFactor;
		//settings.volume = volume_set[mood_index];
		return settings;
	}
	
	//-------- Speech server command builder and execution --------
	
	private boolean callServerProcess(String url, String audioFilePath){
		try{
			FileUtils.copyURLToFile(
					new URL(url), 
					new File(audioFilePath), 
					7500, 7500
			);
			return true;
		}catch(Exception e){
			Debugger.println("TTS server call FAILED. URL: " + url.replaceFirst("\\?.*", "") + " - Msg.: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 3);
			return false;
		}
	}
	
	public static String buildMaryTtsUrl(String text, TtsVoiceTrait voiceTrait, double globalSpeedFactor, double globalToneFactor){
		String serverUrl = Config.marytts_server + "/process";
		String fullUrl = URLBuilder.getStringP20(serverUrl, 
				"?INPUT_TEXT=", text,
				"&INPUT_TYPE=", "TEXT",
				"&OUTPUT_TYPE=", "AUDIO",
				"&AUDIO=", "WAVE_FILE",
				//"&LOCALE=", "en_GB",	//this is in data
				"&VOICE=", voiceTrait.getSystemName()
				//TODO: global speed and tone ignored
		);
		JSONObject dataMod = voiceTrait.getData();
		for (Object key : dataMod.keySet()){
			fullUrl += URLBuilder.getStringP20(fullUrl, "&" + key + "=", (String) dataMod.get(key));
		}
		return fullUrl;
	}
	
	//------- Runtime command process builder and execution -------
	
	private boolean runRuntimeProcess(String[] command, String audioFilePath){
		Debugger.println("TTS LOG - Command: " + String.join(" ", command), 2);		//debug
		RuntimeResult res = RuntimeInterface.runCommand(command, PROCESS_TIMEOUT_MS);
		if (res.getStatusCode() != 0){
			//Error
			Exception e = res.getException();
			if (res.getStatusCode() == 3){
				throw new RuntimeException("TTS procces took too long!");
			}else{
				throw new RuntimeException("TTS procces failed! Msg: " + ((e != null)? e.getMessage() : "unknown"));
			}
		}else{
			//Success
			fileCleanUpQueue.add(new File(audioFilePath));
			ThreadManager.scheduleTaskToRunOnceInBackground(CLEAN_UP_DELAY_MS, cleanUpTask);
			return true;
		}
	}
	
	public static String[] buildEspeakCmd(String text, TtsVoiceTrait voiceTrait, double globalSpeedFactor, double globalToneFactor, String filePath){
		String systemVoiceName = voiceTrait.getSystemName();
		int speed = (int) (voiceTrait.getSpeed() * globalSpeedFactor);
		int tone = (int) (voiceTrait.getPitch() * globalToneFactor);
		int volume = voiceTrait.getVolume();
		String cmd;
		if (Is.systemWindows()){
			//Windows
			cmd = (Config.ttsEngines + "espeak-ng/espeak-ng.exe").replace("/", File.separator);
			//check text safety (prevent any injections) for Windows
			//TODO
			//encoding conversion
			text = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
			/* -- this should work .. but does not --
			if (RuntimeInterface.windowsShellCodepage != null && !RuntimeInterface.windowsShellCodepage.equals(StandardCharsets.UTF_8)){
				text = new String(text.getBytes(StandardCharsets.UTF_8), RuntimeInterface.windowsShellCodepage);
			}*/
		}else{
			//Other
			cmd = "espeak-ng";
			//check text safety (prevent any injections) for UNIX
			//TODO
			//e.g. ?? text = "'" + text.replaceAll("'", "\\'") + "'";
		}
		return new String[]{ cmd,
				"-a", Integer.toString(volume), 
				"-p", Integer.toString(tone), 
				"-s", Integer.toString(speed),	
				"-b", "1",	//UTF-8 text
				"-v", systemVoiceName,
				"-w", filePath,
				text
		};
	}
	
	public static String[] buildFliteCmd(){
		String[] cmd = new String[]{};
		return cmd;
	}
	
	public static String[] buildPicoCmd(){
		String[] cmd = new String[]{};
		return cmd;
	}
}
