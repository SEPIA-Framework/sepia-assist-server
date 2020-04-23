package net.b07z.sepia.server.assist.tts;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.URLBuilder;

public class Test_MaryTtsStream {
	
	// get output option
	public static final String baseFolder = Config.ttsWebServerPath;

	public static void main(String[] args) throws MalformedURLException, IOException{
		
		String text = "Albert Einstein war ein deutscher Physiker. Albert Einstein was a German-born theoretical physicist.";
		String file = "test.wav";
		
		//URL
		String serverUrl = "http://127.0.0.1:59125/process";
		String fullUrl = URLBuilder.getString(serverUrl, 
				"?INPUT_TEXT=", text,
				"&INPUT_TYPE=", "TEXT",
				"&LOCALE=", "en_GB",
				"&VOICE=", "dfki-prudence-hsmm",	//bits1-hsmm (de), bits3-hsmm (de), cmu-bdl-hsmm, dfki-prudence-hsmm (en_GB), dfki-spike-hsmm (en_GB)
				"&OUTPUT_TYPE=", "AUDIO",
				"&AUDIO=", "WAVE_FILE",
				"&effect_FIRFilter_parameters=", "type:4;fc1:240.0;fc2:1000.0;tbw:1280.0",
				"&effect_FIRFilter_selected=", "off",
				"&effect_Robot_parameters=", "amount:100.0;",
				"&effect_Robot_selected=", "off",
				"&effect_F0Scale_parameters=", "f0Scale:0.33;",
				"&effect_F0Scale_selected=", "on",
				"&effect_F0Add_parameters=", "f0Add:-50.0;",
				"&effect_F0Add_selected=", "on"
		);
		System.out.println("url: " + fullUrl); 		//DEBUG
		
		long tic = System.currentTimeMillis();
		FileUtils.copyURLToFile(
				new URL(fullUrl), 
				new File(baseFolder + file), 
				7500, 7500
		);
		System.out.println("took: " + (System.currentTimeMillis() - tic) + "ms");
	}


}
