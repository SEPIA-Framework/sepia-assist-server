package net.b07z.sepia.server.assist.tts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.signalproc.filter.BandRejectFilter;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.MaryAudioUtils;
import net.b07z.sepia.server.assist.server.Config;

public class MaryTtsTest {
	
	// get output option
	public static final String baseFolder = Config.ttsWebServerPath;

	public static void main(String[] args) throws MaryConfigurationException {

		// get input
		Map<String, String> texts = new HashMap<>();
		//texts.put("de", "Albert Einstein war ein deutscher Physiker mit Schweizer und US-amerikanischer Staatsbürgerschaft. Er gilt als einer der bedeutendsten theoretischen Physiker der Wissenschaftsgeschichte und weltweit als bekanntester Wissenschaftler der Neuzeit.");
		//texts.put("de", "Er gilt als einer der bedeutendsten theoretischen Physiker der Wissenschaftsgeschichte und weltweit als bekanntester Wissenschaftler der Neuzeit.");
		texts.put("de", "Das Wetter in Berlin ist klar mit blauem Himmel bei einer Temperatur von 20 Grad Celsius. Bye bye :-) .");
		//texts.put("en", "Albert Einstein was a German-born theoretical physicist who developed the theory of relativity, one of the two pillars of modern physics alongside quantum mechanics.");
		texts.put("en", "The weather in Berlin is clear with blue sky and temperatures of 20 degrees celsius. Good Bye :-) .");
		//String inputText = "Hallo Mary, dies ist ein Test. Klappts?";
		texts.put("mix", "Albert Einstein war ein deutscher Physiker mit Schweizer und US-amerikanischer Staatsbürgerschaft. One of the two pillars of modern physics alongside quantum mechanics.");

		// init mary
		LocalMaryInterface mary = null;
		try {
			mary = new LocalMaryInterface();
			Set<String> voices = mary.getAvailableVoices();
			System.out.println("Voices: " + voices);
			System.out.println("Locales: " + mary.getAvailableLocales());
			//mary.setVoice("bits1-hsmm");
			//mary.setVoice("bits3-hsmm");
			//mary.setVoice("dfki-pavoque-neutral-hsmm");
			for (String v : voices) {
				mary.setVoice(v);
				String text = texts.get(mary.getLocale().toString().split("_")[0]);
				if (text == null) text = texts.get("mix");
				generateWav(mary, text, v, false);
			}
		} catch (MaryConfigurationException e) {
			System.err.println("Could not initialize MaryTTS interface: " + e.getMessage());
			throw e;
		}
	}
	
	public static void generateWav(LocalMaryInterface mary, String inputText, String voice, boolean useFilter) {
		//create filter
		AudioInputStream audio = null;
		double[] samples;
		if (voice.equals("bits1-hsmm")) useFilter = true;
		if (useFilter){
			int samplingRate = 16000;
			int lowerCutoffFreq = 240;
			int upperCutoffFreq = 1000;
			double lowerNormalisedCutoffFrequency = (double) lowerCutoffFreq / samplingRate;
			double upperNormalisedCutoffFrequency = (double) upperCutoffFreq / samplingRate;
			BandRejectFilter filter = new BandRejectFilter(lowerNormalisedCutoffFrequency, upperNormalisedCutoffFrequency,
					1280 / (double) samplingRate);
			System.out.println("Created " + filter.toString() + " with reject band from " + lowerCutoffFreq + " Hz to "
					+ upperCutoffFreq + " Hz and transition band width " + ((int) filter.getTransitionBandWidth(samplingRate))
					+ " Hz");
	
			// synthesize
			DoubleDataSource filteredSignal = null;
			try {
				mary.setStreamingAudio(true);
				audio = mary.generateAudio(inputText);
				AudioDoubleDataSource source = new AudioDoubleDataSource(audio);
				filteredSignal = filter.apply(source);
				
			} catch (SynthesisException e) {
				System.err.println("Synthesis failed: " + e.getMessage());
				System.exit(1);
			}
			samples = filteredSignal.getAllData();
			
		}else{
			try {
				audio = mary.generateAudio(inputText);
				
			} catch (SynthesisException e) {
				System.err.println("Synthesis failed: " + e.getMessage());
				System.exit(1);
			}
			samples = MaryAudioUtils.getSamplesAsDoubleArray(audio);
		}

		// write to output
		String outputFileName = baseFolder + voice + (useFilter? "_f" : "") + ".wav";
		try {
			MaryAudioUtils.writeWavFile(samples, outputFileName, audio.getFormat());
			System.out.println("Output written to " + outputFileName);
		} catch (IOException e) {
			System.err.println("Could not write to file: " + outputFileName + "\n" + e.getMessage());
			System.exit(1);
		}
	}
}