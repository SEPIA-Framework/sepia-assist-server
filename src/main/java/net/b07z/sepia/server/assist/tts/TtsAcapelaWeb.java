package net.b07z.sepia.server.assist.tts;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * NOTE: This is pretty old code and might need some upgrades ^^<br>
 * <br>
 * This is the TTS interface implementation for Acapela supporting multiple languages and emotional voices.<br>
 * Note that there is a certain order in which to call the settings to avoid overwriting parameters:<br><br>
 * setLanguage -> setGender -> setVoice -> setMood -> setSpeedFactor -> setToneFactor <br>
 * <br>
 * Note as well that smileys in a text will have a higher priority and overwrite mood :-)
 * 
 * @author Florian Quirin
 *
 */
public class TtsAcapelaWeb implements TtsInterface {
	
    //private String VAAS_URL = "https://vaas.acapela-group.com/Services/Synthesizer/";
    //private String VAAS_URL = "http://vaas.acapela-group.com/Services/UrlMaker.json";
	private String VAAS_URL = "http://vaas.acapela-group.com/webservices/1-60-00/synthesizer.php";
    private String VAAS_LOGIN = "EVAL_VAAS";
    
    //Voices - 1) default, 2) happy, 3) sad, 4) angry, 5) shout, 6) whisper, 7) fun1 (e.g. old), 8) fun2 (e.g. Yoda)
    String[] DE_VOICE_CLAUDIA = { "claudia22k", "claudiasmile22k", "claudia22k", "claudia22k", "claudia22k", "claudia22k", "claudia22k", "claudia22k"};
    int[] DE_SPEED_CLAUDIA = { 			110, 		100, 			90,			110,			110,		110,			110,		110};
    int[] DE_TONE_CLAUDIA = { 			101, 		98, 			99,			100,			100,		100,			100,		100};
    
    String[] DE_VOICE_JULIA = { "julia22k", "julia22k", "julia22k", "julia22k", "julia22k", "julia22k", "julia22k", "julia22k"};
    int[] DE_SPEED_JULIA = { 		105, 		108, 		87,			100,		100,		100,		100,		100};
    int[] DE_TONE_JULIA = { 		100, 		100, 		99,			100,		100,		100,		100,		100};
    
    String[] DE_VOICE_KLAUS = { "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k"};
    int[] DE_SPEED_KLAUS = { 		100, 		100, 		100,		100,		100,		100,		100,		100};
    int[] DE_TONE_KLAUS = { 		100, 		100, 		100,		100,		100,		100,		100,		100};
    
    String[] DE_VOICE_CREATURE = { "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k", "klaus22k"};
    int[] DE_SPEED_CREATURE = { 		110, 		110, 		110,		100,		100,		100,		100,		100};
    int[] DE_TONE_CREATURE = { 			85, 		85, 		85,			100,		100,		100,		100,		100};
    
    String[] DE_VOICE_LEA = { "lea22k", "lea22k", "lea22k", "lea22k", "lea22k", "lea22k", "lea22k", "lea22k"};
    int[] DE_SPEED_LEA = { 		100, 	105, 		88,		100,	100,		100,		100,	100};
    int[] DE_TONE_LEA = { 		100, 	101, 		98,		100,	100,		100,		100,	100};
    
    String[] EN_VOICE_SHARON = {"sharon22k", "sharon22k", "sharon22k", "sharon22k", "sharon22k", "sharon22k", "sharon22k", "sharon22k"};
    int[] EN_SPEED_SHARON = { 		100, 		105, 			90,		100,		100,		100,			100,		100};
    int[] EN_TONE_SHARON = { 		100, 		100, 			98,		100,		100,		100,			100,		100};
    
    String[] EN_VOICE_ELLA = {"ella22k", "ella22k", "ella22k", "ella22k", "ella22k", "ella22k", "ella22k", "ella22k"};
    int[] EN_SPEED_ELLA = { 	90, 		95, 		95,		100,	100,		100,		100,		100};
    int[] EN_TONE_ELLA = { 		98, 		99, 		97,		100,	100,		100,		100,		100};
    
    String[] EN_VOICE_WILL = {"will22k", "willhappy22k", "willsad22k", "willbadguy22k", "willfromafar22k", "willupclose22k", "willoldman22k", "willlittlecreature22k"};
    int[] EN_SPEED_WILL = {		110, 			108, 		135,			110,				110,			110,				118,			115};
    int[] EN_TONE_WILL = {		100, 			100, 		100,			100,				100,			100,				100,			100};
    
    String[] EN_VOICE_OLDMAN = {"willoldman22k", "willoldman22k", "willoldman22k", "willoldman22k", "willoldman22k", "willoldman22k", "willoldman22k", "willoldman22k"};
    int[] EN_SPEED_OLDMAN = {		118, 		120,		110,		115,		115,		115,		115,		115};		
    int[] EN_TONE_OLDMAN = { 		100, 		100, 		100,		100,		100,		100,		100,		100};
    
    String[] EN_VOICE_CREATURE = {"willlittlecreature22k", "willlittlecreature22k", "willlittlecreature22k", "willlittlecreature22k", "willlittlecreature22k", "willlittlecreature22k", "willlittlecreature22k", "willlittlecreature22k"};
    int[] EN_SPEED_CREATURE = {		115, 		115,		115,		115,		115,		115,		115,		115};		
    int[] EN_TONE_CREATURE = { 		100, 		100, 		100,		100,		100,		100,		100,		100};		

    String[] FR_VOICE_MANON = {"manon22k", "manon22k", "manon22k", "manon22k", "manon22k", "manon22k", "manon22k", "manon22k"};
    int[] FR_SPEED_MANON = {		100, 		100, 		100,		100,		100,		100,		100,		100};
    int[] FR_TONE_MANON =  {		100, 		100, 		100,		100,		100,		100,		100,		100};
    
    String[] FR_VOICE_ANTOINE = {"antoine22k", "antoinehappy22k", "antoinesad22k", "antoine22k", "antoinefromafar22k", "antoineupclose22k", "antoine22k", "antoine22k"};
    int[] FR_SPEED_ANTOINE = {		100, 			100, 				100,			100,			100,				100,				100,		100};
    int[] FR_TONE_ANTOINE =  {		100, 			100, 				100,			100,			100,				100,				100,		100};

    String[] ES_VOICE_INES = {"ines22k", "ines22k", "ines22k", "ines22k", "ines22k", "ines22k", "ines22k", "ines22k"};
    int[] ES_SPEED_INES = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] ES_TONE_INES = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    
    String[] ES_VOICE_MARIA = {"maria22k", "maria22k", "maria22k", "maria22k", "maria22k", "maria22k", "maria22k", "maria22k"};
    int[] ES_SPEED_MARIA = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] ES_TONE_MARIA = {			100, 		100, 		100,		100,	100,		100,		100,		100};

    String[] TR_VOICE_IPEK = {"ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k", "ipek22k"};
    int[] TR_SPEED_IPEK = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] TR_TONE_IPEK = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    
    String[] SV_VOICE_ELIN = {"elin22k", "elin22k", "elin22k", "elin22k", "elin22k", "elin22k", "elin22k", "elin22k"};
    int[] SV_SPEED_ELIN = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] SV_TONE_ELIN = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    
    String[] ZH_VOICE_LULU = {"lulu22k", "lulu22k", "lulu22k", "lulu22k", "lulu22k", "lulu22k", "lulu22k", "lulu22k"};
    int[] ZH_SPEED_LULU = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] ZH_TONE_LULU = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    
    String[] AR_VOICE_LEILA = {"leila22k", "leila22k", "leila22k", "leila22k", "leila22k", "leila22k", "leila22k", "leila22k"};
    int[] AR_SPEED_LEILA = {		100, 		100, 		100,		100,		100,		100,		100,		100};
    int[] AR_TONE_LEILA = {			100, 		100, 		100,		100,		100,		100,		100,		100};
    
    String[] EL_VOICE_DIMITRIS = {"dimitris22k", "dimitrishappy22k", "dimitrissad22k", "dimitris22k", "dimitris22k", "dimitris22k", "dimitris22k", "dimitris22k"};
    int[] EL_SPEED_DIMITRIS = {			100, 			100, 				100,			100,			100,		100,			100,		100};
    int[] EL_TONE_DIMITRIS = {			100, 			100, 				100,			100,			100,		100,			100,		100};
    
    String[] IT_VOICE_FABIANA = {"fabiana22k", "fabiana22k", "fabiana22k", "fabiana22k", "fabiana22k", "fabiana22k", "fabiana22k", "fabiana22k"};
    int[] IT_SPEED_FABIANA = {		100, 		100, 			100,			100,		100,			100,		100,		100};
    int[] IT_TONE_FABIANA = {		100, 		100, 			100,			100,		100,			100,		100,		100};
    
    String[] JA_VOICE_SAKURA = {"sakura22k", "sakura22k", "sakura22k", "sakura22k", "sakura22k", "sakura22k", "sakura22k", "sakura22k"};
    int[] JA_SPEED_SAKURA = {		100, 		100, 		100,			100,		100,		100,		100,		100};
    int[] JA_TONE_SAKURA = {		100, 		100, 		100,			100,		100,		100,		100,		100};
    
    String[] KO_VOICE_MINJII = {"minji22k", "minji22k", "minji22k", "minji22k", "minji22k", "minji22k", "minji22k", "minji22k"};
    int[] KO_SPEED_MINJII = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    int[] KO_TONE_MINJII = {		100, 		100, 		100,		100,	100,		100,		100,		100};
    
    String[] NL_VOICE_JASMIJN = {"jasmijn22k", "jasmijn22k", "jasmijn22k", "jasmijn22k", "jasmijn22k", "jasmijn22k", "jasmijn22k", "jasmijn22k"};
    int[] NL_SPEED_JASMIJN = {		100, 		100, 			100,		100,			100,		100,			100,		100};
    int[] NL_TONE_JASMIJN = {		100, 		100, 			100,		100,			100,		100,			100,		100};
    
    String[] PL_VOICE_MONIKA = {"monika22k", "monika22k", "monika22k", "monika22k", "monika22k", "monika22k", "monika22k", "monika22k"};
    int[] PL_SPEED_MONIKA = {		100, 			100, 		100,		100,		100,		100,		100,		100};
    int[] PL_TONE_MONIKA = {		100, 			100, 		100,		100,		100,		100,		100,		100};
    
    String[] PT_VOICE_CELIA = {"celia22k", "celia22k", "celia22k", "celia22k", "celia22k", "celia22k", "celia22k", "celia22k"};
    int[] PT_SPEED_CELIA = {		100, 		100, 		100,		100,		100,		100,		100,		100};
    int[] PT_TONE_CELIA = {			100, 		100, 		100,		100,		100,		100,		100,		100};
    
    String[] RU_VOICE_ALYONA = {"alyona22k", "alyona22k", "alyona22k", "alyona22k", "alyona22k", "alyona22k", "alyona22k", "alyona22k"};
    int[] RU_SPEED_ALYONA = {		100, 		100, 		100,		100,		100,		100,		100,		100};
    int[] RU_TONE_ALYONA = {		100, 		100, 		100,		100,		100,		100,		100,		100};
    
    int[] DEFAULT_SPEED = {		100, 		100,		100,		100,		100,		100,		100,		100};		//60-360
    int[] DEFAULT_TONE = { 		100, 		100, 		100,		100,		100,		100,		100,		100};		//50-150
    int[] DEFAULT_VOL = {		32768, 		32768, 		32768,		32768,		32768,		32768,		32768,		32768};		//50-65535

    //defaults
    private String language = "en";
    private String gender = "male";
    private String activeVoice = "enu_will";		//name of voice set as seen in get_voices (not necessarily the same as the actual selected voice (enu_will != will22k)
    private String[] voice_set = EN_VOICE_WILL;
    private int[] speed_set = EN_SPEED_WILL;
    private int[] tone_set = EN_TONE_WILL;
    private int[] volume_set = DEFAULT_VOL;
    private int mood_index=0;			//0 - neutral/default, 1 - happy, 2 - sad, 3 - angry, 4 - shout, 5 - whisper, 6 - fun1 (e.g. old), 7 - fun2 (e.g. Yoda)
    private double speedFactor = 1.0d;	//multiply speed with this 	
    private double toneFactor = 1.0d;	//multiply tone with this
	
	String client = "";				//client info
    String environment = "";		//client environment (browser, android app, car, home?)		
	
    int charLimit = 600;			//limit text length
	String soundQuali = "CBR_48";	//default sound quality is CBR_48, CBR_32 is ok too ;-) - deactivated, see code below
	String soundFormat = "OGG";		//default sound format, MP3, OGG, WAV, RAW
	
	//support lists
	ArrayList<String> voiceList = new ArrayList<String>();
	ArrayList<String> genderList = new ArrayList<String>();
	ArrayList<String> languageList = new ArrayList<String>();
	ArrayList<String> soundFormatList = new ArrayList<String>();
	int maxMoodIndex = 0;
	
	//CONSTRUCTOR - configuration
	public TtsAcapelaWeb(){
		//supported languages:
		languageList.add(LANGUAGES.DE);
		languageList.add(LANGUAGES.EN);
		languageList.add(LANGUAGES.FR);
		languageList.add(LANGUAGES.ES);
		languageList.add(LANGUAGES.TR);
		languageList.add(LANGUAGES.SV);
		languageList.add(LANGUAGES.ZH);
		//supported sound formats
		soundFormatList.add("MP3");
		soundFormatList.add("MP3_CBR_48");
		soundFormatList.add("MP3_CBR_32");
		soundFormatList.add("OGG");
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
		voiceList.add("manon_fr");				//TODO: adjust names ...
		voiceList.add("antoine_fr");
		voiceList.add("ines_es");
		voiceList.add("maria_es");
		voiceList.add("ipek_tr");
		voiceList.add("elin_sv");
		voiceList.add("lulu_zh");
		voiceList.add("leila_ar");
		voiceList.add("dimitris_el");
		voiceList.add("fabiana_it");
		voiceList.add("sakura_ja");
		voiceList.add("minji_ko");
		voiceList.add("jasmijn_nl");
		voiceList.add("monika_pl");
		voiceList.add("celia_pt");
		voiceList.add("alyona_ru");
		
		//supported maximum mood index
		maxMoodIndex = 7;
	}
	
	//set TTS input and check it for special stuff like environment based format (web-browser != android app)
	public void setInput(TtsInput input){
		if (input != null){
			//AUTO SOUND FORMAT CHECK
			if (input.format.equals("default")){
				this.environment = input.environment;
				this.client = input.client_info;
				//overwrite soundFormat for web browsers
				if (!CLIENTS.canPlayOGG(this.client)){
					soundFormat = "MP3";
				}
				//System.out.println("SF: " + soundFormat); 		//debug
			}
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
	//return active (last used) sound format
	public String getActiveSoundFormat(){
		return soundFormat;
	}
	//set sound format
	public String setSoundFormat(String format){
		format = format.toUpperCase();
		if (soundFormatList.contains(format)){
			if (format.matches("MP3_.*")){
				soundFormat = "MP3" + "&req_snd_kbps=" + format.replaceFirst(".*?_", "").trim();
			}else{
				soundFormat = format;
			}
			return soundFormat;
		}else{
			return soundFormat;
		}
	}
	
	//create mp3 file from text via google
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
		
		//trim text
		read_this = TtsTools.trimText(read_this);
		
		//optimize pronunciation
		read_this = TtsTools.optimizePronunciation(read_this, language);
		
		try
		{
			//set parameters
			String voice = voice_set[mod_mood_index];
			int speed = (int) (speed_set[mod_mood_index] * speedFactor);
			int tone = (int) (tone_set[mod_mood_index] * toneFactor);
			int volume = volume_set[mod_mood_index];
						
			//modify string - I moved the speed and pitch here to use the relative speed tag and make it consistent with embedded
			read_this = "\\rspd=" + speed + "\\ " + "\\vct=" + tone + "\\ " + read_this;
			//System.out.println(read_this);
			
			//make URL and connect to service
			String url = VAAS_URL
					+ "?cl_login=" + VAAS_LOGIN + "&cl_app=" + Config.acapela_vaas_app + "&cl_pwd=" + Config.acapela_vaas_key
					+ "&req_voice=" + voice 
					+ "&req_text=" + URLEncoder.encode(read_this, "UTF-8").replace("+", "%20") 
					//+ "&req_spd=" + speed 
					//+ "&req_vct=" + tone 
					+ "&req_vol=" + volume
					//+ "&req_snd_kbps=" + soundQuali;
					+ "&req_snd_type=" + soundFormat;
					/*
					+ "&prot_vers=2"
					+ "&cl_env=APACHE_2.2.9_PHP_5.5"
					+ "&cl_vers=1-00"
					+ "&req_timeout=120";
					*/
			
			//System.out.println("TTS URL: " + url);		//debug
			
			//run process - note: its thread blocking but this should be considered "intended" here ;-) 
			JSONObject response = Connectors.httpGET(url);
			//System.out.println(response.toJSONString());		//debug
			
			//response ok?
			if (!Connectors.httpSuccess(response)){
				Debugger.println("TTS ACAPELA - connection failed! - " + Connectors.httpError(response), 1);
				return "";
			}
			
			//check if it is a JSON response or a JSONified string
			String audioURL = "";
			if (response.containsKey("STRING")){
				String[] params = response.get("STRING").toString().split("&");
				for (String p : params){
					if (p.startsWith("res=")){
						if (p.matches("res=NOK")){
							Debugger.println("TTS ACAPELA - connection error! Info: " + params.toString(), 1);
							return "";
						}
					}
					else if (p.startsWith("snd_url=")){
						audioURL = p.replaceFirst("^snd_url=", "").trim();
					}
				}
			}else{
				String status = (String) response.get("res");
				if (status != null && status.matches("OK") || status.matches("ok")){
					audioURL = (String) response.get("snd_url");
				}else{
					Debugger.println("TTS ACAPELA - failed with error: " + status, 1);
				}
				//replace http with https
				audioURL = audioURL.replaceFirst("http", "https");
			}
			//check result
			if (audioURL.isEmpty()){
				Debugger.println("TTS ACAPELA - failed with empty URL!", 1);
			}
			
			//System.out.println("ACAPELA VAAS URL: " + audioURL); 		//debug
			return audioURL;
			
		//ERROR
		}catch (Exception e) {
			Debugger.printStackTrace(e, 5);
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
		//fr
		}else if (voiceName.matches("manon_fr")){
			voice_set = FR_VOICE_MANON;			speed_set = FR_SPEED_MANON;			tone_set = FR_TONE_MANON;
			gender = "female";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("antoine_fr")){
			voice_set = FR_VOICE_ANTOINE;		speed_set = FR_SPEED_ANTOINE;			tone_set = FR_TONE_ANTOINE;
			gender = "male";
			activeVoice = voiceName;	return true;
		//es
		}else if (voiceName.matches("ines_es")){
			voice_set = ES_VOICE_INES;			speed_set = ES_SPEED_INES;			tone_set = ES_TONE_INES;
			gender = "female";
			activeVoice = voiceName;	return true;
		}else if (voiceName.matches("maria_es")){
			voice_set = ES_VOICE_MARIA;			speed_set = ES_SPEED_MARIA;			tone_set = ES_TONE_MARIA;
			gender = "female";
			activeVoice = voiceName;	return true;
		//tr
		}else if (voiceName.matches("ipek_tr")){
			voice_set = TR_VOICE_IPEK;			speed_set = TR_SPEED_IPEK;			tone_set = TR_TONE_IPEK;
			gender = "male";
			activeVoice = voiceName;	return true;
		//tr
		}else if (voiceName.matches("elin_sv")){
			voice_set = SV_VOICE_ELIN;			speed_set = SV_SPEED_ELIN;			tone_set = SV_TONE_ELIN;
			gender = "female";
			activeVoice = voiceName;	return true;
		//zh - Chinese
		}else if (voiceName.matches("lulu_zh")){
			voice_set = ZH_VOICE_LULU;			speed_set = ZH_SPEED_LULU;			tone_set = ZH_TONE_LULU;
			gender = "female";
			activeVoice = voiceName;	return true;
		//ar - Arabic
		}else if (voiceName.matches("leila_ar")){
			voice_set = AR_VOICE_LEILA;			speed_set = AR_SPEED_LEILA;			tone_set = AR_TONE_LEILA;
			gender = "female";
			activeVoice = voiceName;	return true;
		//el - Greek
		}else if (voiceName.matches("dimitris_el")){
			voice_set = EL_VOICE_DIMITRIS;		speed_set = EL_SPEED_DIMITRIS;		tone_set = EL_TONE_DIMITRIS;
			gender = "male";
			activeVoice = voiceName;	return true;
		//it - Italian
		}else if (voiceName.matches("fabiana_it")){
			voice_set = IT_VOICE_FABIANA;		speed_set = IT_SPEED_FABIANA;		tone_set = IT_TONE_FABIANA;
			gender = "female";
			activeVoice = voiceName;	return true;
		//ja - Italian
		}else if (voiceName.matches("sakura_ja")){
			voice_set = JA_VOICE_SAKURA;		speed_set = JA_SPEED_SAKURA;		tone_set = JA_TONE_SAKURA;
			gender = "female";
			activeVoice = voiceName;	return true;
		//ko - Korean
		}else if (voiceName.matches("minji_ko")){
			voice_set = KO_VOICE_MINJII;		speed_set = KO_SPEED_MINJII;		tone_set = KO_TONE_MINJII;
			gender = "female";
			activeVoice = voiceName;	return true;
		//nl - Dutch
		}else if (voiceName.matches("jasmijn_nl")){
			voice_set = NL_VOICE_JASMIJN;		speed_set = NL_SPEED_JASMIJN;		tone_set = NL_TONE_JASMIJN;
			gender = "female";
			activeVoice = voiceName;	return true;
		//pl - Polish
		}else if (voiceName.matches("monika_pl")){
			voice_set = PL_VOICE_MONIKA;		speed_set = PL_SPEED_MONIKA;		tone_set = PL_TONE_MONIKA;
			gender = "female";
			activeVoice = voiceName;	return true;
		//pt - Portuguese
		}else if (voiceName.matches("celia_pt")){
			voice_set = PT_VOICE_CELIA;			speed_set = PT_SPEED_CELIA;			tone_set = PT_TONE_CELIA;
			gender = "female";
			activeVoice = voiceName;	return true;
		//ru - Russian
		}else if (voiceName.matches("alyona_ru")){
			voice_set = RU_VOICE_ALYONA;		speed_set = RU_SPEED_ALYONA;		tone_set = RU_TONE_ALYONA;
			gender = "female";
			activeVoice = voiceName;	return true;
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
		if (language.matches(LANGUAGES.DE)){			setVoice("ged_claudia");		return true;}
		else if (language.matches(LANGUAGES.EN)){		setVoice("enu_will");		return true;}	
		else if (language.matches(LANGUAGES.FR)){		setVoice("manon_fr");		return true;}		//TODO: adjust names ...
		else if (language.matches(LANGUAGES.ES)){		setVoice("maria_es");		return true;}		
		else if (language.matches(LANGUAGES.TR)){		setVoice("ipek_tr");		return true;}
		else if (language.matches(LANGUAGES.SV)){		setVoice("elin_sv");		return true;}
		else if (language.matches(LANGUAGES.ZH)){		setVoice("lulu_zh");		return true;}
		else if (language.matches(LANGUAGES.AR)){		setVoice("leila_ar");		return true;}
		else if (language.matches(LANGUAGES.EL)){		setVoice("dimitris_el");	return true;}
		else if (language.matches(LANGUAGES.IT)){		setVoice("fabiana_it");		return true;}
		else if (language.matches(LANGUAGES.JA)){		setVoice("sakura_ja");		return true;}
		else if (language.matches(LANGUAGES.KO)){		setVoice("minji_ko");		return true;}
		else if (language.matches(LANGUAGES.NL)){		setVoice("jasmijn_nl");		return true;}
		else if (language.matches(LANGUAGES.PL)){		setVoice("monika_pl");		return true;}
		else if (language.matches(LANGUAGES.PT)){		setVoice("celia_pt");		return true;}
		else if (language.matches(LANGUAGES.RU)){		setVoice("alyona_ru");		return true;}
		else{									setVoice("enu_will");		return false;}
	}
	
	//set a voice according to default gender selection
	public boolean setGender(String gender) {
		if (language.matches(LANGUAGES.DE)){
			//de - female
			if (gender.matches("female")){				setVoice("ged_claudia");	return true;
			//de - male
			}else if (gender.matches("male")){			setVoice("ged_klaus");		return true;
			//de - child
			}else if (gender.matches("child")){			setVoice("ged_lea");		return true;
			//de - creature
			}else if (gender.matches("creature")){		setVoice("ged_creature");	return true;
			}
		}else if (language.matches(LANGUAGES.EN)){
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
		}else if (language.matches("fr")){
			//fr - female
			if (gender.matches("female")){				setVoice("manon_fr");			return true;		//TODO: adjust names ...
			//fr - male
			}else if (gender.matches("male")){			setVoice("antoine_fr");			return true;
			}
		}
		//else if (language.matches("es")){		}
		//else if (language.matches("tr")){		}
		return false;
	}

	//return list with voice names
	public ArrayList<String> getVoices() {
		/* old names (still supported)
		vList.add("claudia_de");	
		vList.add("julia_de");
		vList.add("klaus_de");		
		vList.add("lea_de");			//German child
		vList.add("creature_de");		//German creature
		vList.add("sharon_en");
		vList.add("will_en");
		vList.add("ella_en");			//English child
		vList.add("oldman_en");			//English old man
		vList.add("littlecreature_en");	//English little creature ;-)
		*/
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
}
