package net.b07z.sepia.server.assist.tts;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.signalproc.filter.BandRejectFilter;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.MaryAudioUtils;

public class MaryTtsTest {

	public static void main(String[] args) throws MaryConfigurationException {

		// get output option
		String outputFileName = "Xtensions/TTS/maryTTS_test.wav";
		boolean useFilter = true;

		// get input
		//String inputText = "Hallo Mary, dies ist ein Test. Klappts?";
		String inputText = "Albert Einstein war ein deutscher Physiker mit Schweizer und US-amerikanischer Staatsbürgerschaft. Er gilt als einer der bedeutendsten theoretischen Physiker der Wissenschaftsgeschichte und weltweit als bekanntester Wissenschaftler der Neuzeit.";

		// init mary
		LocalMaryInterface mary = null;
		try {
			mary = new LocalMaryInterface();
			System.out.println("Voices: " + mary.getAvailableVoices());
			mary.setVoice("bits1-hsmm");
		} catch (MaryConfigurationException e) {
			System.err.println("Could not initialize MaryTTS interface: " + e.getMessage());
			throw e;
		}
		
		//create filter
		AudioInputStream audio = null;
		double[] samples;
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
		try {
			MaryAudioUtils.writeWavFile(samples, outputFileName, audio.getFormat());
			System.out.println("Output written to " + outputFileName);
		} catch (IOException e) {
			System.err.println("Could not write to file: " + outputFileName + "\n" + e.getMessage());
			System.exit(1);
		}
	}
}