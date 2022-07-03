package net.b07z.sepia.server.assist.tts;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tts.TtsTools.EngineType;
import net.b07z.sepia.server.assist.tts.TtsTools.GenderCode;
import net.b07z.sepia.server.core.tools.Connectors;
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
	
	//support lists
	private static Map<String, TtsVoice> voices = new TreeMap<>();
	private static Map<EngineType, Boolean> supportedEngines = new HashMap<>();
	private static Set<String> genderList = new HashSet<String>();
	private static Set<String> languageList = new HashSet<String>();
	private static Set<String> soundFormatList = new HashSet<String>();
	private static int maxMoodIndex = 0;
	
	//voices
	
	//defaults - filled during setup
	private static Map<String, String> defaultTtsVoicesForLang = new HashMap<>();
		
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
    private String language = LANGUAGES.EN;
	private String gender = "male";
	private String activeVoice = null; 	//name of voice set as seen in get_voices (not necessarily the same as the actual selected voice (enu_will != will22k)
	private int mood_index = 0;			//0 - neutral/default, 1 - happy, 2 - sad, 3 - angry, 4 - shout, 5 - whisper, 6 - fun1 (e.g. old), 7 - fun2 (e.g. Yoda)
	private double speedFactor = 1.0d;	//global modifier - multiply speed with this 	
	private double toneFactor = 1.0d;	//global modifier - multiply tone with this
	
	int charLimit = 600;				//limit text length
	String soundFormat = "WAV";
	
	public String client = "";			//client info
	public String environment = "";		//client environment
	
	public TtsOpenEmbedded(){}
	
	@Override
	public boolean setup(){
		//clean - in case of server reload
		languageList.clear();
		soundFormatList.clear();
		genderList.clear();
		voices.clear();
		supportedEngines.clear();
		defaultTtsVoicesForLang.clear();
		
		//load settings from JSON file
		String ttsSettingsFile = Config.ttsEngines + "settings.json";
		JSONObject ttsSettings = JSON.readJsonFromFile(ttsSettingsFile);
		Map<String, TtsVoice> ttsVoicesMap;
		if (ttsSettings == null){
			return false;
		}else{
			try{
				ttsVoicesMap = TtsSettings.loadVoicesFromSettings(ttsSettings);			
			}catch (Exception e){
				Debugger.println("TTS module - setup - failed to parse settings!", 1);
				e.printStackTrace();
				return false;
			}
		}
		
		//test support for engines (and load some stuff if required)
		
		//ESPEAK
		supportedEngines.put(EngineType.espeak, false);
		if (Is.systemWindows()){
			//test support
			if (new File(Config.ttsEngines + "espeak-ng/espeak-ng.exe").exists()){
				supportedEngines.put(EngineType.espeak, true);
			}
		}else{
			//test support
			//RuntimeResult rtr = RuntimeInterface.runCommand(new String[]{"command", "-v", "espeak-ng"}, 5000, false);		//TODO: why not working?
			RuntimeResult rtr = RuntimeInterface.runCommand(new String[]{"command -v espeak-ng"}, 5000, false);
			int code = rtr.getStatusCode();
			if (code == 0 && Is.notNullOrEmpty(rtr.getOutput())){
				supportedEngines.put(EngineType.espeak, true);
			}
		}
		if (!supportedEngines.get(EngineType.espeak)){
			Debugger.println("TTS module - Espeak engine not found. Support has been deactivated for now.", 1);
		}
		//ESPEAK-MBROLA
		supportedEngines.put(EngineType.espeak_mbrola, false);
		if (supportedEngines.get(EngineType.espeak)){
			if (Is.systemWindows()){
				//test support
				if (new File(Config.ttsEngines + "espeak-ng/mbrola.dll").exists()){
					supportedEngines.put(EngineType.espeak_mbrola, true);		//TODO: this should work but I haven't been able to get it working :-/
				}
			}else{
				//test support
				RuntimeResult rtr = RuntimeInterface.runCommand(new String[]{"command -v mbrola"}, 5000, false);
				int code = rtr.getStatusCode();
				if (code == 0 && Is.notNullOrEmpty(rtr.getOutput())){
					supportedEngines.put(EngineType.espeak_mbrola, true);
				}
			}
			if (supportedEngines.get(EngineType.espeak_mbrola)){
				Debugger.println("TTS module - MBROLA found. Please make sure you comply with the LICENSE conditions!", 3);
			}
		}
		//TXT2PHO-MBROLA
		supportedEngines.put(EngineType.txt2pho_mbrola, false);
		if (Is.systemWindows()){
			//test support - no Windows support (yet ... technically it's probably possible)
			supportedEngines.put(EngineType.txt2pho_mbrola, false);
		}else{
			//test support
			if (new File(Config.ttsEngines + "txt2pho/txt2pho").exists() && new File(Config.ttsEngines + "txt2pho/txt2pho-speak.sh").exists()){
				RuntimeResult rtr = RuntimeInterface.runCommand(new String[]{"command -v mbrola"}, 5000, false);
				int code = rtr.getStatusCode();
				if (code == 0 && Is.notNullOrEmpty(rtr.getOutput())){
					supportedEngines.put(EngineType.txt2pho_mbrola, true);
				}
			}
		}
		if (supportedEngines.get(EngineType.txt2pho_mbrola)){
			Debugger.println("TTS module - txt2pho and MBROLA found. Please make sure you comply with the LICENSE conditions!", 3);
		}
		//PICO
		supportedEngines.put(EngineType.pico, false);
		if (Is.systemWindows()){
			//test support - no Windows support
			supportedEngines.put(EngineType.pico, false);
		}else{
			//test support
			//RuntimeResult rtr = RuntimeInterface.runCommand(new String[]{"command", "-v", "pico2wave"}, 5000, false);		//TODO: why not working?
			RuntimeResult rtr = RuntimeInterface.runCommand(new String[]{"command -v pico2wave"}, 5000, false);
			int code = rtr.getStatusCode();
			if (code == 0 && Is.notNullOrEmpty(rtr.getOutput())){
				supportedEngines.put(EngineType.pico, true);
			}
		}
		if (!supportedEngines.get(EngineType.pico)){
			if (Is.systemWindows()){
				Debugger.println("TTS module - Pico engine is not support on Windows and has been deactivated.", 3);
			}else{
				Debugger.println("TTS module - Pico engine not found. Support has been deactivated for now.", 1);
			}
		}
		//MARY-TTS API
		supportedEngines.put(EngineType.marytts, false);
		String[] maryTtsVoicesRes = null;
		Map<String, String[]> maryTtsVoicesMap = new HashMap<>();	//e.g: name: "cmu-slt-hsmm" - array: [0:cmu-slt-hsmm, 1:en_US 2:female, 3:hmm]
		Map<String, Boolean> maryTtsVoiceIsMappedAtLeastOnce = new HashMap<>();
		try{
			//get voices from MaryTTS server
			maryTtsVoicesRes = Connectors.simpleHtmlGet(Config.marytts_server + "/voices").split("(\\r\\n|\\n)");
			supportedEngines.put(EngineType.marytts, true);
		}catch (Exception e){
			maryTtsVoicesRes = null;
		}
		if (maryTtsVoicesRes == null){
			Debugger.println("TTS module - MaryTTS server (" + Config.marytts_server + ") did not answer or had no voices installed. Support has been deactivated for now.", 1);
		}else{
			//map Mary-TTS API voices
			try{
				for (int i=0; i<maryTtsVoicesRes.length; i++){
					String[] voiceInfo = maryTtsVoicesRes[i].split("\\s+");		//example - 0:cmu-slt-hsmm, 1:en_US 2:female, 3:hmm
					maryTtsVoicesMap.put(voiceInfo[0], voiceInfo);
					//System.out.println(voiceInfo[0]); 		//DEBUG
				}
			}catch (Exception e){
				Debugger.println("TTS module - Failed to map MaryTTS voices. Error: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
			}
		}
		
		//supported maximum mood index
		maxMoodIndex = 3;
		
		//map voices
		Map<EngineType, Integer> countEngineVoices = new HashMap<>();
		for (TtsVoice voice : ttsVoicesMap.values()){
			String name = voice.getName();
			String lang = voice.getLanguageCode();
			if (Is.nullOrEmpty(name) || Is.nullOrEmpty(lang)){
				Debugger.println("TTS module - One voice has been skipped because 'name' or 'languageCode' was missing!", 1);
			}else{
				if (voice.getType() != null && supportedEngines.getOrDefault(voice.getType(), false)){
					//check type-specific stuff
					if (voice.getType().equals(EngineType.marytts)){
						//check if Mary-TTS voice actually exists
						boolean allMaryVoicesFound = true;
						for (TtsVoiceTrait vt : voice.getMoods()){
							if (!maryTtsVoicesMap.containsKey(vt.getSystemName())){
								//this voice is not on the server
								Debugger.println("TTS module - MaryTTS-API voice skipped: '" + name + "' - not found on server: " + vt.getSystemName(), 3);
								allMaryVoicesFound = false;
								break;
							}else{
								maryTtsVoiceIsMappedAtLeastOnce.put(vt.getSystemName(), true);
							}
						}
						if (!allMaryVoicesFound){
							continue;
						}
					}
					//store
					voices.put(name, voice);
					//add supported languages and language default
					languageList.add(lang);
					if (!defaultTtsVoicesForLang.containsKey(lang)) defaultTtsVoicesForLang.put(lang, name);	//default voice for this language (random)
					//add supported genders
					genderList.add(voice.getGenderCode().name());
					//count
					countEngineVoices.put(voice.getType(), countEngineVoices.getOrDefault(voice.getType(), 0) + 1);
				}
			}
		}
		//add remaining Mary-TTS API voices as default voices
		if (maryTtsVoicesRes != null && !maryTtsVoicesMap.isEmpty()){
			for (String maryVoiceName : maryTtsVoicesMap.keySet()){
				//not used so far?
				if (!maryTtsVoiceIsMappedAtLeastOnce.getOrDefault(maryVoiceName, false)){
					String[] voiceInfo = maryTtsVoicesMap.get(maryVoiceName);
					//create default mapping
					try{
						if (voiceInfo.length >= 3){
							//build basic voice configuration - NOTE: expects the format given above (name locale gender other)
							TtsVoiceTrait vt = new TtsVoiceTrait(voiceInfo[0], EngineType.marytts.name(), 
									voiceInfo[1].split("_")[0].toLowerCase(), voiceInfo[2], JSON.make("LOCALE", voiceInfo[1]));
							TtsVoice v = new TtsVoice();
							String name = voiceInfo[1].replace("_", "-") + " marytts " + voiceInfo[0];
							//try to parse gender or fallback to 'undefined'
							GenderCode gc;
							try {
								gc = GenderCode.valueOf(vt.getGenderCode());
							}catch (Exception e){
								gc = GenderCode.undefined;
							}
							v.setName(name);
							v.setLanguageCode(vt.getLanguageCode());
							v.setGenderCode(gc);
							v.setType(EngineType.marytts);
							v.setMoods(Arrays.asList(vt));
							voices.put(name , v);
							Debugger.println("TTS module - Mapped this MaryTTS voice via default: " + maryVoiceName, 3);
							//count
							countEngineVoices.put(v.getType(), countEngineVoices.getOrDefault(v.getType(), 0) + 1);
						}else{
							Debugger.println("TTS module - Don't know how to map this MaryTTS voice: " + maryVoiceName, 1);
						}
					}catch (Exception e){
						Debugger.println("TTS module - Failed to map remaining MaryTTS voices. Error: " + e.getMessage(), 1);
						Debugger.printStackTrace(e, 3);
					}
				}
			}
		}
		
		//some logging
		countEngineVoices.keySet().forEach(type -> {
			Debugger.println("TTS module - Added " + countEngineVoices.get(type) + " '" + type + "' voices.", 3);
		});
		
		//supported sound formats - NOTE: only format atm
		soundFormatList.add("WAV");
		
		//get defaults
		if (voices.keySet().size() > 0){
			this.activeVoice = voices.keySet().iterator().next();
			TtsVoice v = voices.get(this.activeVoice);
			this.language = v.getLanguageCode();
			this.gender = v.getGenderCode().name();
		}else{
			this.activeVoice = null;
			this.language = LANGUAGES.EN;
			this.gender = GenderCode.male.name();
		}
		
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
	
	//return list with available genders
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
		if (Is.nullOrEmpty(language)) return false;
		if (defaultTtsVoicesForLang.containsKey(language)){
			setVoice(defaultTtsVoicesForLang.get(language));
			this.language = language;
			return true;
		}else{
			this.language = "";
			return false;
		}
	}
	
	//set a voice according to default gender selection if this language has one
	public boolean setGender(String gender) {
		if (Is.nullOrEmpty(gender)) return false;
		for (TtsVoice v : voices.values()){
			if (v.getLanguageCode().equals(this.language) && v.getGenderCode().name().equals(gender)){
				setVoice(v.getName());
				this.gender = gender;
				return true;
			}
		}
		this.gender = "";
		return false;
	}
	
	//sets the voice set with voice name, speed and tone.
	public boolean setVoice(String voiceName) {
		if (Is.nullOrEmpty(voiceName) || !voices.containsKey(voiceName)){
			//default
			this.activeVoice = "";
			return false;
		}
		//new voice
		TtsVoice v = voices.get(voiceName);
		this.activeVoice = voiceName;
		this.language = v.getLanguageCode();
		this.gender = v.getGenderCode().name();
		return true;
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
		
		//test available voice
		if (Is.nullOrEmpty(this.activeVoice) || voices.isEmpty() || voices.get(this.activeVoice) == null){
			Debugger.println("TTS FAILED: voice NOT found!", 1);
			return "";
		}
		
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
		
		// - get new ID and prepare files
		int ID = fileID.addAndGet(1);
		if (ID >= MAX_FILES){
			fileID.set(1);
		}
		String audioFileName = ID + "-speak-" + Security.getRandomUUID().replace("-", "") + ".wav";
		String audioFilePath = ttsOutFolder + audioFileName;
		
		try{
			//set parameters
			TtsVoice voice = voices.get(this.activeVoice);
			final int moodSearch = modMoodIndex;
			Optional<TtsVoiceTrait> moodVt = voice.getMoods().stream()
				.filter(vt -> vt.getMoodIndex() == moodSearch)
				.findFirst();
			//in theory this cannot fail because all has been validated before
			TtsVoiceTrait voiceTrait = moodVt.orElse(voices.get(this.activeVoice).getMoods().get(0));
			voiceTrait.fillDefaults(voice); 	//NOTE: important!
			Debugger.println("TTS LOG - Voice trait: " + voiceTrait, 2);		//debug
			//update for endpoint
			this.language = voiceTrait.getLanguageCode();
			this.gender = voiceTrait.getGenderCode();
			
			//optimize pronunciation
			readThis = TtsTools.optimizePronunciation(readThis, language, voiceTrait.getType());
			
			//create files:
						
			// - try to write file
			String audioURL = Config.ttsWebServerUrl + audioFileName;
			Debugger.println("TTS LOG - URL: " + audioURL, 2);		//debug
			
			//build command line action
			boolean generatedFile;
			String typeActive = voiceTrait.getType();
			//ESPEAK / ESPEAK-MBROLA
			if (typeActive.equals(EngineType.espeak.name()) || typeActive.equals(EngineType.espeak_mbrola.name())){
				//run process
				String[] command = buildEspeakCmd(readThis, voiceTrait, this.speedFactor, this.toneFactor, audioFilePath);
				generatedFile = runRuntimeProcess(command, audioFilePath);
			//TXT2PHO-MBROLA
			}else if (typeActive.equals(EngineType.txt2pho_mbrola.name())){
				//run process
				String[] command = buildTxt2PhoMbrolaCmd(readThis, voiceTrait, this.speedFactor, this.toneFactor, audioFilePath);
				generatedFile = runRuntimeProcess(command, audioFilePath);
			//PICO
			}else if (typeActive.equals(EngineType.pico.name())){
				//run process
				String[] command = buildPicoCmd(readThis, voiceTrait, this.speedFactor, this.toneFactor, audioFilePath);
				generatedFile = runRuntimeProcess(command, audioFilePath);
			//MARY-TTS
			}else if (typeActive.equals(EngineType.marytts.name())){
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
				ThreadManager.scheduleBackgroundTaskAndForget(CLEAN_UP_DELAY_MS, cleanUpTask);
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
			fullUrl += URLBuilder.getStringP20("", "&" + key + "=", (String) dataMod.get(key));
		}
		//System.out.println("fullURL: " + fullUrl); 		//DEBUG
		return fullUrl;
	}
	public static JSONObject maryTtsData(String locale, String volume, String f0add, String f0scale, String firFilter, String robot){
		JSONObject data = new JSONObject();
		if (Is.notNullOrEmpty(locale)){
			JSON.put(data, "LOCALE", locale);
		}
		if (Is.notNullOrEmpty(volume)){
			JSON.put(data, "effect_Volume_selected", "on");
			JSON.put(data, "effect_Volume_parameters", volume);
		}
		if (Is.notNullOrEmpty(firFilter)){
			JSON.put(data, "effect_FIRFilter_selected", "on");
			JSON.put(data, "effect_FIRFilter_parameters", firFilter);
		}
		if (Is.notNullOrEmpty(f0add)){
			JSON.put(data, "effect_F0Add_selected", "on");
			JSON.put(data, "effect_F0Add_parameters", f0add);
		}
		if (Is.notNullOrEmpty(f0scale)){
			JSON.put(data, "effect_F0Scale_selected", "on");
			JSON.put(data, "effect_F0Scale_parameters", f0scale);
		}
		if (Is.notNullOrEmpty(robot)){
			JSON.put(data, "effect_Robot_selected", "on");
			JSON.put(data, "effect_Robot_parameters", robot);
		}
		return data;
	}
	
	//------- Runtime command process builder and execution -------
	
	private boolean runRuntimeProcess(String[] command, String audioFilePath){
		if (command == null){
			throw new RuntimeException("TTS procces failed! Msg: Command was 'null', probably because the engine or engine-settings are not supported.");
		}
		Debugger.println("TTS LOG - Command: " + String.join(" ", command), 2);		//debug
		boolean restrictVar = false;		//NOTE: we remove ENV variables in advance, but is this safe enough?
		RuntimeResult res = RuntimeInterface.runCommand(command, PROCESS_TIMEOUT_MS, restrictVar);
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
			return true;
		}
	}
	
	public static String[] buildEspeakCmd(String text, TtsVoiceTrait voiceTrait, double globalSpeedFactor, double globalToneFactor, String filePath){
		String systemVoiceName = voiceTrait.getSystemName();
		int speed = (int) (voiceTrait.getSpeed() * globalSpeedFactor);
		int tone = (int) (voiceTrait.getPitch() * globalToneFactor);
		int volume = voiceTrait.getVolume();
		String cmd;
		//check text safety (prevent any injections) for Windows
		//text = RuntimeInterface.escapeVar(text);		//note: we replace critical characters in "optimizePronunciation"
		//get cmd
		if (Is.systemWindows()){
			//Windows
			cmd = (Config.ttsEngines + "espeak-ng/espeak-ng.exe").replace("/", File.separator);
			//encoding conversion
			text = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
			/* -- this should work .. but does not --
			if (RuntimeInterface.windowsShellCodepage != null && !RuntimeInterface.windowsShellCodepage.equals(StandardCharsets.UTF_8)){
				text = new String(text.getBytes(StandardCharsets.UTF_8), RuntimeInterface.windowsShellCodepage);
			}*/
		}else{
			//Other
			cmd = "espeak-ng";
		}
		//cheap hack to avoid breaking command with input like "-1" (because its interpreted as option O_o)
		text = " " + text;
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
		//TODO: implement
		String[] cmd = new String[]{};
		return cmd;
	}
	
	public static String[] buildPicoCmd(String text, TtsVoiceTrait voiceTrait, double globalSpeedFactor, double globalToneFactor, String filePath){
		JSONObject dataMod = voiceTrait.getData();
		//TODO: global volume, speed and tone not supported yet
		String cmd;
		//get cmd
		if (Is.systemWindows()){
			return null;		//NOT AVAILABLE
		}else{
			//Other
			cmd = "pico2wave";
		}
		//cheap hack to avoid breaking command with input like "-1" (because its interpreted as option O_o)
		text = " " + text;
		//Languages: en-US en-GB de-DE es-ES fr-FR it-IT
		return new String[]{ cmd,
				"-l", JSON.getStringOrDefault(dataMod, "l", "en-GB"),
				"-w", filePath,
				text
		};
	}
	
	public static String[] buildTxt2PhoMbrolaCmd(String text, TtsVoiceTrait voiceTrait, double globalSpeedFactor, double globalToneFactor, String filePath){
		String systemVoiceName = voiceTrait.getSystemName();
		String gender = voiceTrait.getGenderCode();
		String txt2phoGender = "m";
		if (gender.equals(GenderCode.female.name())){
			txt2phoGender = "f";
		}else if (gender.equals(GenderCode.male.name())){
			txt2phoGender = "m";
		}else{
			//diverse and robot need to define gender extra
			JSONObject data = voiceTrait.getData();
			if (data != null){
				txt2phoGender = JSON.getStringOrDefault(data, "genderOverride", "m");
			}
		}
		String cmd;
		//get cmd
		if (Is.systemWindows()){
			//Windows
			throw new RuntimeException("Txt2pho for Windows is not implemented yet!");
		}else{
			//Other
			cmd = (Config.ttsEngines + "txt2pho/txt2pho-speak.sh");
		}
		return new String[]{cmd, txt2phoGender, systemVoiceName, text, filePath};
	}
}
