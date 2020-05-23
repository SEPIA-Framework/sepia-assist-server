package net.b07z.sepia.server.assist.tts;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tts.TtsTools.EngineType;
import net.b07z.sepia.server.assist.workers.ThreadManager;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * NOTE: This is pretty old code and might need some upgrades ^^<br>
 * <br>
 * This is the TTS interface implementation for embedded Acapela supporting multiple languages and emotional voices.<br>
 * Note that there is a certain order in which to call the settings to avoid overwriting parameters:<br><br>
 * setLanguage -> setVoice -> setGender -> setMood -> setSpeedFactor -> setToneFactor <br>
 * <br>
 * Note as well that smileys in a text will have a higher priority and overwrite mood :-)
 * 
 * @author Florian Quirin
 *
 */
public class TtsAcapelaEmbedded implements TtsInterface {
	
	//track files
	private static int MAX_FILES = 30;
	private static AtomicInteger fileID = new AtomicInteger(0);
	private static String acapelaPath = Config.ttsEngines + "acapela/";
	private static String audioPath = Config.ttsWebServerPath;
	private static String textPath = acapelaPath + "text_in/";
	
	//track process
	private Process process;
	//private ThreadInfo worker;
	public boolean isProcessing = false;
	public boolean abortProcess = false;
	public boolean isTimedOut = false;
	long maxWait_ms = 5000;
    
    //Voices - 1) default, 2) happy, 3) sad, 4) angry, 5) shout, 6) whisper, 7) fun1 (e.g. old), 8) fun2 (e.g. Yoda)
    String[] DE_VOICE_CLAUDIA = { "ged_claudia", "ged_claudia", "ged_claudia", "ged_claudia", "ged_claudia", "ged_claudia", "ged_claudia", "ged_claudia"};
    int[] DE_SPEED_CLAUDIA = { 			110, 		110, 			90,			110,			110,		110,			110,		110};
    int[] DE_TONE_CLAUDIA = { 			101, 		100, 			99,			100,			100,		100,			100,		100};
    
    String[] DE_VOICE_JULIA = { "ged_julia", "ged_julia", "ged_julia", "ged_julia", "ged_julia", "ged_julia", "ged_julia", "ged_julia"};
    int[] DE_SPEED_JULIA = { 		105, 		110, 		87,			100,		100,		100,		100,		100};
    int[] DE_TONE_JULIA = { 		100, 		100, 		99,			100,		100,		100,		100,		100};
    
    String[] DE_VOICE_KLAUS = { "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus"};
    int[] DE_SPEED_KLAUS = { 		100, 		100, 			100,		100,		100,		100,		100,		100};
    int[] DE_TONE_KLAUS = { 		100, 		100, 			100,		100,		100,		100,		100,		100};
    
    String[] DE_VOICE_CREATURE = { "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus", "ged_klaus"};
    int[] DE_SPEED_CREATURE = { 		110, 		110, 		110,			100,		100,		100,		100,		100};
    int[] DE_TONE_CREATURE = { 			85, 		85, 		85,				100,		100,		100,		100,		100};
    
    String[] DE_VOICE_LEA = { "ged_lea", "ged_lea", "ged_lea", "ged_lea", "ged_lea", "ged_lea", "ged_lea", "ged_lea"};
    int[] DE_SPEED_LEA = { 		100, 	105, 		88,		100,	100,		100,		100,	100};
    int[] DE_TONE_LEA = { 		100, 	101, 		98,		100,	100,		100,		100,	100};
    
    String[] EN_VOICE_SHARON = {"enu_sharon", "enu_sharon", "enu_sharon", "enu_sharon", "enu_sharon", "enu_sharon", "enu_sharon", "enu_sharon"};
    int[] EN_SPEED_SHARON = { 		100, 		105, 			90,		100,		100,		100,			100,		100};
    int[] EN_TONE_SHARON = { 		100, 		100, 			98,		100,		100,		100,			100,		100};
    
    String[] EN_VOICE_ELLA = {"enu_ella", "enu_ella", "enu_ella", "enu_ella", "enu_ella", "enu_ella", "enu_ella", "enu_ella"};
    int[] EN_SPEED_ELLA = { 	90, 		95, 		95,		100,	100,		100,		100,		100};
    int[] EN_TONE_ELLA = { 		98, 		99, 		97,		100,	100,		100,		100,		100};
    
    String[] EN_VOICE_WILL = {"enu_will", "enu_will_happy", "enu_will_sad", "willbadguy22k", "willfromafar22k", "willupclose22k", "willoldman22k", "willlittlecreature22k"};
    int[] EN_SPEED_WILL = {		110, 			108, 		135,			110,				110,			110,				118,			115};
    int[] EN_TONE_WILL = {		100, 			100, 		100,			100,				100,			100,				100,			100};
    
    String[] EN_VOICE_OLDMAN = {"enu_will_oldman", "enu_will_oldman", "enu_will_oldman", "enu_will_oldman", "enu_will_oldman", "enu_will_oldman", "enu_will_oldman", "enu_will_oldman"};
    int[] EN_SPEED_OLDMAN = {		118, 		120,		110,		115,		115,		115,		115,		115};		
    int[] EN_TONE_OLDMAN = { 		100, 		100, 		100,		100,		100,		100,		100,		100};
    
    String[] EN_VOICE_CREATURE = {"enu_littlecreature", "enu_littlecreature", "enu_littlecreature", "enu_littlecreature", "enu_littlecreature", "enu_littlecreature", "enu_littlecreature", "enu_littlecreature"};
    int[] EN_SPEED_CREATURE = {		115, 		115,		115,		115,		115,		115,		115,		115};		
    int[] EN_TONE_CREATURE = { 		100, 		100, 		100,		100,		100,		100,		100,		100};			

    /*
    String[] FR_VOICE_MANON = {"manon22k", "manon22k", "manon22k", "manon22k", "manon22k", "manon22k", "manon22k", "manon22k"};
    int[] FR_SPEED_MANON = {		100, 		100, 		100,		100,		100,		100,		100,		100};
    int[] FR_TONE_MANON =  {		100, 		100, 		100,		100,		100,		100,		100,		100};

    String[] ES_VOICE_INES = {"ines22k", "ines22k", "ines22k", "ines22k", "ines22k", "ines22k", "ines22k", "ines22k"};
    int[] ES_SPEED_INES = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] ES_TONE_INES = {		100, 		100, 		100,		100,	100,		100,		100,		100};

    String[] TR_VOICE_IPEK = {"ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k"};
    int[] TR_SPEED_IPEK = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] TR_TONE_IPEK = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    */
    
    int[] DEFAULT_SPEED = {		100, 		100,		100,		100,		100,		100,		100,		100};		//100% relative speed
    int[] DEFAULT_TONE = { 		100, 		100, 		100,		100,		100,		100,		100,		100};		//100% pitch
    int[] DEFAULT_VOL = {		32768, 		32768, 		32768,		32768,		32768,		32768,		32768,		32768};		//50-65535

    //defaults
    private String language = "en";
	private String gender = "male";
	private String activeVoice = "enu_will";		//name of voice set as seen in get_voices (not necessarily the same as the actual selected voice (enu_will != will22k)
	private String[] voice_set = EN_VOICE_WILL;
	private int[] speed_set = EN_SPEED_WILL;
	private int[] tone_set = EN_TONE_WILL;
	//private int[] volume_set = DEFAULT_VOL;
	private int mood_index=0;			//0 - neutral/default, 1 - happy, 2 - sad, 3 - angry, 4 - shout, 5 - whisper, 6 - fun1 (e.g. old), 7 - fun2 (e.g. Yoda)
	private double speedFactor = 1.0d;	//multiply speed with this 	
	private double toneFactor = 1.0d;	//multiply tone with this
	
	int charLimit = 600;			//limit text length
	//String soundQuali = "CBR_32";	//default sound quality
	String soundFormat = "WAV";
	
	public String client = "";		//client info
	public String environment = "";		//client environment
	
	//support lists
	ArrayList<String> voiceList = new ArrayList<String>();
	ArrayList<String> genderList = new ArrayList<String>();
	ArrayList<String> languageList = new ArrayList<String>();
	ArrayList<String> soundFormatList = new ArrayList<String>();
	int maxMoodIndex = 0;
	
	public TtsAcapelaEmbedded(){
		//supported languages:
		languageList.add("de");
		languageList.add("en");
		//supported sound formats
		soundFormatList.add("WAV");
		//supported genders:
		genderList.add("male");
		genderList.add("female");
		genderList.add("child");
		genderList.add("old");
		genderList.add("creature");
		//supported voices:
		voiceList.add("ged_claudia");			//German female1
		voiceList.add("ged_julia");				//German female2
		voiceList.add("ged_klaus");				//German male
		voiceList.add("ged_lea");				//German child
		voiceList.add("ged_creature");			//German creature ("dies ist das Ende aller Taaage")
		voiceList.add("enu_sharon");			//English female1
		voiceList.add("enu_will");				//English male1
		voiceList.add("enu_ella");				//English child
		voiceList.add("enu_will_oldman");		//English old man
		voiceList.add("enu_littlecreature");	//English little creature ;-)
		//supported maximum mood index
		maxMoodIndex = 7;
	}
	
	@Override
	public boolean setup(){
		//TODO
		return false;
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
		return true;
	}
	
	//return supported sound formats
	public ArrayList<String> getSoundFormats(){
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
	
	//create sound file from text
	public String getAudioURL(String read_this) {
		
		//characters limit - does Acapela have a limit?
		if (read_this.length() > charLimit){
			//System.out.println("we need to shorten the text");			//debug
			read_this=read_this.substring(0,charLimit);
		}
		
		//set mood - sets mood_index and overwrites the automatic mood tracking (Smileys have higher priority than input.mood)
		int mod_mood_index = TtsTools.getMoodIndex(read_this, mood_index);
		//set language	- sets language and default Sets (voice, tone, speed, volume)
		//set gender	- sets male/female/creature ^^ overwrites Sets
		//set voice		- overwrites all (except mood_index) and sets voice name directly (with corresponding Sets)
		
		//trim text - removing smileys etc.
		read_this = TtsTools.trimText(read_this);
		
		//optimize pronunciation
		read_this = TtsTools.optimizePronunciation(read_this, language, EngineType.acapela.name());
		
		try
		{
			//set parameters
			String voice = voice_set[mod_mood_index];
			int speed = (int) (speed_set[mod_mood_index] * speedFactor);
			int tone = (int) (tone_set[mod_mood_index] * toneFactor);
			//int volume = volume_set[mod_mood_index];
			
			//modify string - I moved the speed and pitch here to use the relative speed tag and make it consistent with embedded
			read_this = "\\rspd=" + speed + "\\ " + "\\vct=" + tone + "\\ " + read_this;
			//System.out.println(read_this);
			
			//get voice configuration
			String ini_path = "";
			File[] ini_files;
			File voiceDir = new File(acapelaPath + "voices/" + voice);
			ini_files = voiceDir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".ini");
				}
			});
			if (ini_files != null && ini_files.length>0){
				ini_path = ini_files[0].getPath();
				Debugger.println("TTS LOG - Voice INI: " + ini_path, 2);	//debug
				
			}else{
				Debugger.println("TTS LOG - CANNOT FIND VOICE CONFIGURATION (.ini)!", 1);
				return "";
			}
			
			//create files
			
			// - get new ID and prepare files
			int max_files = MAX_FILES;
			int ID = fileID.addAndGet(1);
			if (ID >= max_files){
				fileID.set(1);
			}
			int iteration = 0;
			String audioFile = audioPath + "speak" + ID + ".wav";
			String textFile = textPath + "speak" + ID + ".txt";
			Path pathText = Paths.get(textFile);
			Path pathAudio = Paths.get(audioFile);
			
			// - search for a "free" ID:
			while (((Files.exists(pathText) && !Files.isWritable(pathText)) || (Files.exists(pathAudio) && !Files.isWritable(pathAudio))) && iteration < max_files){
				ID = fileID.addAndGet(1);
				if (ID >= max_files){
					fileID.set(1);
				}
				iteration++;
				audioFile = audioPath + "speak" + ID + ".wav";
				textFile = textPath + "speak" + ID + ".txt";
				pathText = Paths.get(textFile);
				pathAudio = Paths.get(audioFile);
			}
			
			// - got a working file?
			if ((!Files.exists(pathText) && !Files.exists(pathAudio)) || (Files.isWritable(pathText) && Files.isWritable(pathAudio))){
				
				String audioURL = pathAudio.toAbsolutePath().toString();			//TODO: fix
				Debugger.println("TTS LOG - URL: " + audioURL, 3);		//debug
				
				// - save textFile
				ArrayList<String> lines = new ArrayList<String>();
				lines.add(read_this);
				//TODO: check Encoding for different platforms!
				//Files.write(Paths.get(textFile), lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				Files.write(Paths.get(textFile), lines, StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				
				//build command line action --- TODO: fix
				String command = acapelaPath + "sdk/babile/txt_to_wav/generate " + ini_path + " " + textFile + " " + audioFile;
				/* TEST PROCESS
				File zip = new File(Config.xtensionsFolder + "Test/sdk/babile/txt_to_wav/7z.exe");
				File arc = new File(Config.xtensionsFolder + "Test/sdk/babile/txt_to_wav/test_audio.7z");
				File dir = new File(Config.xtensionsFolder + "AcapelaTTS/audio_out");
				String command = zip.getPath() + " e " + arc.getPath() + " -o" + dir.getPath() + " -y";
				*/
				Debugger.println("TTS LOG - Command: " + command, 3);		//debug
				//run process - note: its thread blocking but this should be considered "intended" here ;-) 
				runProcess(command);
				waitForProcess(maxWait_ms);
				if (isTimedOut){
					System.err.println("TTS LOG - PROCESS TIMED OUT!");
					return "";
				}else{
					return audioURL;
				}
				//Files.write(Paths.get(audioFile), lines, StandardCharsets.UTF_8);
			
			//ERROR - no file available
			}else{
				Debugger.println("TTS LOG - CANNOT WRITE AUDIO FILE! ("+Files.exists(pathText)+","+Files.exists(pathAudio)+","+Files.isWritable(pathText) +","+ Files.isWritable(pathAudio)+")", 1);
				return "";
			}

		//ERROR
		}catch (Exception e) {
			e.printStackTrace();
			Debugger.println(e.toString(), 1);
			//throw e;
			
			return "";
		}
	}
	

	//sets the voice set with voice name, speed and tone.
	public boolean setVoice(String voiceName) {
		voiceName = voiceName.toLowerCase();
		//de
		if (voiceName.matches("claudia_de|ged_claudia")){
			voice_set = DE_VOICE_CLAUDIA;		speed_set = DE_SPEED_CLAUDIA;		tone_set = DE_TONE_CLAUDIA;
			gender = "female";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("julia_de|ged_julia")){
			voice_set = DE_VOICE_JULIA;			speed_set = DE_SPEED_JULIA;			tone_set = DE_TONE_JULIA;
			gender = "female";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("klaus_de|ged_klaus")){
			voice_set = DE_VOICE_KLAUS;			speed_set = DE_SPEED_KLAUS;			tone_set = DE_TONE_KLAUS;
			gender = "male";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("lea_de|ged_lea")){
			voice_set = DE_VOICE_LEA;			speed_set = DE_SPEED_LEA;			tone_set = DE_TONE_LEA;
			gender = "child";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("creature_de|ged_creature")){
			voice_set = DE_VOICE_CREATURE;		speed_set = DE_SPEED_CREATURE;		tone_set = DE_TONE_CREATURE;
			gender = "creature";
			activeVoice = voiceName;	return true;
		//en
		}else if (voiceName.matches("sharon_en|enu_sharon")){
			voice_set = EN_VOICE_SHARON;		speed_set = EN_SPEED_SHARON;		tone_set = EN_TONE_SHARON;
			gender = "female";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("ella_en|enu_ella")){
			voice_set = EN_VOICE_ELLA;			speed_set = EN_SPEED_ELLA;			tone_set = EN_TONE_ELLA;
			gender = "child";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("will_en|enu_will")){
			voice_set = EN_VOICE_WILL;			speed_set = EN_SPEED_WILL;			tone_set = EN_TONE_WILL;
			gender = "male";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("oldman_en|enu_will_oldman")){
			voice_set = EN_VOICE_OLDMAN;		speed_set = EN_SPEED_OLDMAN;		tone_set = EN_TONE_OLDMAN;
			gender = "old";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("littlecreature_en|enu_littlecreature")){
			voice_set = EN_VOICE_CREATURE;		speed_set = EN_SPEED_CREATURE;		tone_set = EN_TONE_CREATURE;
			gender = "creature";
			activeVoice = voiceName;	return true;
		/*
		//fr
		}else if (voiceName.matches("manon_fr")){
			voice_set = FR_VOICE_MANON;			speed_set = FR_SPEED_MANON;			tone_set = FR_TONE_MANON;
			gender = "female";
		//es
		}else if (voiceName.matches("ines_es")){
			voice_set = ES_VOICE_INES;			speed_set = ES_SPEED_INES;			tone_set = ES_TONE_INES;
			gender = "female";
		//tr
		}else if (voiceName.matches("ipek_tr")){
			voice_set = TR_VOICE_IPEK;			speed_set = TR_SPEED_IPEK;			tone_set = TR_TONE_IPEK;
			gender = "male";
		*/
			
		//default
		}else{
			voice_set = EN_VOICE_WILL;			speed_set = EN_SPEED_WILL;			tone_set = EN_TONE_WILL;
			gender = "male";
			activeVoice = "default";	return false;
		}
	}

	//set language and default voice sets (voice, speed, tune, vol)
	public boolean setLanguage(String language) {
		this.language = language;
		if (language.matches("de")){			setVoice("ged_claudia");	return true;}
		else if (language.matches("en")){		setVoice("enu_will");		return true;}
		//else if (language.matches("fr")){		setVoice("manon_fr");		return true;}
		//else if (language.matches("es")){		setVoice("ines_es");		return true;}
		//else if (language.matches("tr")){		setVoice("ipek_tr");		return true;}
		else{									setVoice("enu_will");		return false;}
	}
	
	//set a voice according to default gender selection
	public boolean setGender(String gender) {
		if (language.matches("de")){
			//de - female
			if (gender.matches("female")){				setVoice("ged_claudia");	return true;
			//de - male
			}else if (gender.matches("male")){			setVoice("ged_klaus");		return true;
			//de - child
			}else if (gender.matches("child")){			setVoice("ged_lea");		return true;
			//de - creature
			}else if (gender.matches("creature")){		setVoice("ged_creature");	return true;
			}
		}else if (language.matches("en")){
			//en - female
			if (gender.matches("female")){				setVoice("enu_sharon");			return true;
			//en - male
			}else if (gender.matches("male")){			setVoice("enu_will");			return true;
			//en - child
			}else if (gender.matches("child")){			setVoice("enu_ella");			return true;
			//en - old
			}else if (gender.matches("old")){			setVoice("enu_will_oldman");	return true;
			//en - creature
			}else if (gender.matches("creature")){		setVoice("enu_littlecreature");	return true;
			}
		}
		//else if (language.matches("fr")){		}
		//else if (language.matches("es")){		}
		//else if (language.matches("tr")){		}
		return false;
	}

	//return list with voice names
	public ArrayList<String> getVoices() {
		//TODO: check if folder exists before sending list
		return voiceList;
	}
	
	//return list with available languages
	public ArrayList<String> getLanguages(){
		return languageList;
	}
	
	//return list with available languages
	public ArrayList<String> getGenders(){
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

	//sets the mood index to choose the emotional voice.
	public boolean setMood(int mood) {
		if (mood > maxMoodIndex || mood <0){
			mood_index = 0;
			return false;
		}else{
			mood_index = mood;
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
		speedFactor = speed_factor;
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
		toneFactor = tone_factor;
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
		settings.mood = mood_index;
		settings.speed = speedFactor;
		settings.tone = toneFactor;
		//settings.volume = volume_set[mood_index];
		return settings;
	}
	
	
	//--------Process handler---------
	
	//run process in a thread and capture log and error log
	private void runProcess(String command){
		//avoid multiple calls
		if (isProcessing){
			abortProcess = true;
			waitForProcess(2000);
			if (process.isAlive()){
				System.err.println("TTS LOG - MULTIPLE CALLS OF PROCESS DETECTED AND OLD PROCESS DID NOT STOP IN TIME!");
				return;
			}
		}
		//start new
		isProcessing = true;
		abortProcess = false;
		isTimedOut = false;
		ThreadManager.run(() -> {
	    	try {
				ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
				pb.redirectErrorStream(true);
				process = pb.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null){
				    System.out.println("TTS LOG: " + line);
				}
				process.waitFor();
				isProcessing = false;
				
			} catch (Exception e) {
				e.printStackTrace();
				isProcessing = false;
			}
	    });
	}
	
	//wait for process to end or time-out
	private void waitForProcess(long maxiWait_ms){
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		while (isProcessing && !abortProcess){
			Debugger.sleep(50);
			currentTime = System.currentTimeMillis();
			if ((currentTime-startTime) >= maxiWait_ms){
				isTimedOut = true;
				break;
			}else{
				isTimedOut = false;
			}
		}
		if (isTimedOut || abortProcess){
			process.destroyForcibly();
		}
	}
}
