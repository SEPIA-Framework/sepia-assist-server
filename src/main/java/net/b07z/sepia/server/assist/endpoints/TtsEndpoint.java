package net.b07z.sepia.server.assist.endpoints;

import java.util.Collection;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.InterpretationStep;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.tts.TtsInput;
import net.b07z.sepia.server.assist.tts.TtsInterface;
import net.b07z.sepia.server.assist.tts.TtsResult;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.SoundPlayer;
import spark.Request;
import spark.Response;

/**
 * Endpoint that handles Text-To-Speech.
 * 
 * @author Florian Quirin
 *
 */
public class TtsEndpoint {

	/**---TEXT-TO-SPEECH---<br>
	 * End-point that converts text to a sound-file and typically returns the link to stream the file.  
	 */
	public static String ttsAPI(Request request, Response response){
		
		Statistics.add_TTS_hit();					//hit counter
		long tic = System.currentTimeMillis();
		
		//check module
		if (!Config.ttsModuleEnabled){
			Statistics.add_TTS_error();
			return SparkJavaFw.returnResult(request, response, JSON.make(
					"result", "fail",
					"error", "TTS module not active!"
			).toJSONString(), 200);
		}
		
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);
				
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}
		
		Statistics.save_Auth_total_time(tic);
		
		tic = System.currentTimeMillis();			//prepare timer for TTS
		
		//get parameters
		TtsInput input = TtsEndpoint.getInputTTS(params, token);
		String message = input.text;
		String language = input.language;
		String voice = input.voice;
		String gender = input.gender;
		double speed = input.speed;
		double tone = input.tone;
		int mood = input.mood;
		String format = input.format;
				
		//handle mood - if you change this check the TTS implementations for consistency!!!
		int mood_index = 0;				//mood_index as seen in TTS_Interface 0-neutral, 1-happy, 2-sad, 3-angry ...
		if (mood == -1)		mood_index = 0;		//neutral
		else if (mood < 3)	mood_index = 2;		//sad or angry
		else if (mood > 7)	mood_index = 1;		//happy
		
		//get URL answer
		String answer;
		
		if (Is.nullOrEmpty(message)){
			answer = "{\"result\":\"fail\",\"url\":\"" + "" + "\",\"error\":\"no message\"}";
			Statistics.add_TTS_error();
			return SparkJavaFw.returnResult(request, response, answer, 200);
			
		}else{
			//SET source:
			TtsInterface speaker = (TtsInterface) ClassBuilder.construct(Config.ttsModule); //new TTS_Acapela();
			String service = Config.ttsName;			//name
			speaker.setInput(input);					//pass down all input - auto check for client sound format 
			if (!format.matches("default")){ 	speaker.setSoundFormat(format);		}		//manually set format
			//configuration - the order is important!
			speaker.setLanguage(language);
			if (!gender.matches("default") && !gender.isEmpty()){	speaker.setGender(gender);	}
			if (!voice.matches("default") && !voice.isEmpty()){		speaker.setVoice(voice);	}
			speaker.setMood(mood_index);
			if (speed > 0.0d){			speaker.setSpeedFactor(speed); 	}
			if (tone > 0.0d){			speaker.setToneFactor(tone); 	}
			//result
			String audioURL = speaker.getAudioURL(message);
			//System.out.println("Audio: " + audioURL); 		//debug
			
			//check
			if (audioURL.trim().isEmpty()){
				answer = "{\"result\":\"fail\",\"error\":\"no URL generated\"}";
				Statistics.add_TTS_error();
				return SparkJavaFw.returnResult(request, response, answer, 200);
			}
			
			//check if sound can and should be played on server
			boolean playingOnServer = false;
			if (input.playOn.toLowerCase().equals("server") && speaker.supportsPlayOnServer()){
				//play
				SoundPlayer player = new SoundPlayer();
				player.useThread = true;
				player.play(audioURL.replaceFirst("https", "http"));
				playingOnServer = true;
			}
			
			//answer = "{\"result\":\"success\",\"url\":\"" + audioURL + "\"}";
			TtsResult res = new TtsResult("success", audioURL, speaker.getSettings());
			res.more.put("service", service);
			res.more.put("soundFormat", speaker.getActiveSoundFormat());
			res.more.put("playingOnServer", String.valueOf(playingOnServer));
			answer = res.get_result_JSON();
			
			//success stats
			Statistics.add_TTS_hit_authenticated();
			Statistics.save_TTS_total_time(tic);			//store TTS request time
			
			//return URL in requested format
			return SparkJavaFw.returnResult(request, response, answer, 200);
		}
	}

	/**TTS INFO<br>
	 * End-point that returns some info about the TTS engine in use.
	 */
	public static String ttsInfo(Request request, Response response){
		//check module
		if (!Config.ttsModuleEnabled){
			return SparkJavaFw.returnResult(request, response, JSON.make(
					"result", "fail",
					"error", "TTS module not active!"
			).toJSONString(), 200);
		}
		
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);		//TODO: because of form-post?
		
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}
		
		TtsInterface speaker = (TtsInterface) ClassBuilder.construct(Config.ttsModule);
		Collection<String> voiceList = speaker.getVoices();
		Collection<String> genderList = speaker.getGenders();
		Collection<String> languagesList = speaker.getLanguages();
		Collection<String> soundFormatsList = speaker.getSoundFormats();
		JSONObject info = new JSONObject();
		JSON.add(info, "result", "success");
		JSON.add(info, "interface", speaker.getClass().getSimpleName());
		JSON.add(info, "voices", JSON.stringCollectionToJSONArray(voiceList));
		JSON.add(info, "genders", JSON.stringCollectionToJSONArray(genderList));
		JSON.add(info, "languages", JSON.stringCollectionToJSONArray(languagesList));
		JSON.add(info, "formats", JSON.stringCollectionToJSONArray(soundFormatsList));
		JSON.add(info, "maxMoodIndex", speaker.getMaxMoodIndex());
		JSON.add(info, "maxChunkLength", speaker.getMaxChunkLength());
		String answer = info.toJSONString();
				
		//return URL in requested format
		return SparkJavaFw.returnResult(request, response, answer, 200);
	}

	/**
	 * Get input for TTS from API request send to server. See TTS_Input for possible parameters.
	 * @param params
	 * @param token
	 * @return
	 */
	public static TtsInput getInputTTS(RequestParameters params, Authenticator token){
		//get parameters
		//-defaults:
		String client_info = params.getString("client");
		String env = params.getString("env");
		//String text = params.getString(":text");
		String text = params.getString("text");
		String language = params.getString("lang");
		String mood_str = params.getString("mood");
		int mood = -1;
		//-voice specific
		String voice = params.getString("voice");
		String gender = params.getString("gender");
		String speed_str = params.getString("speed");
		double speed = -1.0d; 
		String tone_str = params.getString("tone");
		double tone = -1.0d;
		String playOn = params.getString("playOn");
		String format = params.getString("format");
		
		//check for special TTS command input - Improve this in later versions! - TODO: clean this up
		if (text.startsWith("<weather>") || text.startsWith("<wikipedia>")){
			String question = text.replaceAll(".*?>", "").trim();
			if (!question.isEmpty()){
				NluInput input = new NluInput(text, language, "default", mood, "default");
				input.inputType = "direct_cmd";
				input.user = new User(input, token);
				if (text.startsWith("<weather>")){
					input.text = "weather;" + PARAMETERS.PLACE + "=" + question;
					NluResult res = InterpretationStep.getDirectCommand(input);
					ServiceResult api = ConfigServices.getMasterService(CMD.WEATHER).getResult(res);
					text = api.getAnswerStringClean();
				}else if (text.startsWith("<wikipedia>")){
					input.text = "knowledgebase;" + PARAMETERS.SEARCH + "=" + question;
					NluResult res = InterpretationStep.getDirectCommand(input);
					ServiceResult api = ConfigServices.getMasterService(CMD.KNOWLEDGEBASE).getResult(res);
					text = api.getAnswerStringClean();
				}
			}
		}
		
		//build
		TtsInput input = new TtsInput(text);
		if (client_info!=null)		input.client_info = client_info;
		if (env!=null)				input.environment = env;
		if (language!=null)			input.language = language;
		if (voice!=null)			input.voice = voice;
		if (gender!=null)			input.gender = gender;
		input.playOn = (playOn == null)? "client" : playOn;		//default is client
		if (format!=null)			input.format = format;		//default is "default"
		//
		if (speed_str!=null){
			try {					speed = Double.parseDouble(speed_str);			input.speed = speed;
			}catch (Exception e){	input.speed = -1.0d;							e.printStackTrace();			}
		}
		if (tone_str!=null){
			try {					tone = Double.parseDouble(tone_str);			input.tone = tone;
			}catch (Exception e){	input.tone = -1.0d;								e.printStackTrace();			}
		}
		if (mood_str!=null){
			try {					mood = Integer.parseInt(mood_str);				input.mood = mood;
			}catch (Exception e){	input.mood = -1;								e.printStackTrace();			}
		}
		//System.out.println("l:"+ language +", c:"+ context +", m:"+ mood +", e:"+ env);		//debug
		
		return input;
	}

}
