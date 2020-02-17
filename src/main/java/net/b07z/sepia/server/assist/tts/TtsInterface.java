package net.b07z.sepia.server.assist.tts;

import java.util.Collection;

/**
 * Interface for all TTS Classes. Required methods are:<br>
 * - setLanguage<br>
 * - setGender<br>
 * - setVoice<br>
 * - setMood (using mood_index 0:neutral, 1:happy, 2:sad, 3:angry, ...) <br>
 * - setSpeedFactor<br>
 * - setToneFactor<br>
 * - getAudioURL
 * 
 * @author Florian Quirin
 *
 */
public interface TtsInterface {
	
	/**
	 * If you need to run code at server start, put it here (e.g. clean-up 'tts' folder, load voices etc.).<br>
	 * NOTE: runs ONLY ONCE at server start.
	 */
	public boolean setup();
	
	/**
	 * Set input obtained from user client. Can be used internally to optimize parameters like sound format if the client does not 
	 * support all formats etc. ...
	 * 
	 * @param input - TTS_Input from client
	 */
	public void setInput(TtsInput input);
	
	/**
	 * Check if this TTS supports playing sound files on the server (e.g. for smart home systems)
	 * @return true / false
	 */
	public boolean supportsPlayOnServer();
	
	/**
	 * Get a list of supported sound formats.
	 * @return
	 */
	public Collection<String> getSoundFormats();
	
	/**
	 * Get active sound format.
	 * @return
	 */
	public String getActiveSoundFormat();
	
	/**
	 * Set sound format obtained by getSoundFormats().
	 * @param format - format as given by the get..() method, e.g. "MP3", "OGG" or "WAV".
	 * @return actually set format (might not be what you wish ^^)
	 */
	public String setSoundFormat(String format);
	
	/**
	 * Main method to get the URL linking to the audio stream. This URL should be called by the client interface. 
	 * @param message - what do you want the assistant to say?
	 * @return URL to mp3/ogg/wave stream
	 */
	public String getAudioURL(String message);
	
	/**
	 * Set the desired language. This will automatically call setVoice() with the default settings for every language.
	 * @param language - ISO 639-1 language code  like "en" for English, "de" for German ...
	 */
	public boolean setLanguage(String language);
	
	/**
	 * Set a certain voice manually. Use the getVoices() method to get a list of available voices.
	 * @param voice - voice names defined by getVoices()
	 */
	public boolean setVoice(String voice);
	
	/**
	 * @return ArrayList with possible voice names
	 */
	public Collection<String> getVoices();
	
	/**
	 * @return ArrayList with available languages
	 */
	public Collection<String> getLanguages();
	
	/**
	 * @return ArrayList with available genders (where as "gender" can be male, female, creature, child, old, etc. ...)
	 */
	public Collection<String> getGenders();
	
	/**
	 * @return maximum mood index available. Typically: 0-neutral, 1-happy, 2-sad/apologetic, ... enu_will has 7 available variations.
	 */
	public int getMaxMoodIndex();
	
	/**
	 * @return maximum chunk length (characters + spaces) possible for this engine. 
	 */
	public int getMaxChunkLength();
	
	/**
	 * Sets the gender of the voice if available for the active language.
	 * @param gender - options "f" or "female" and "m" or "male"
	 */
	public boolean setGender(String gender);
	
	/**
	 * Sets the mood index to choose the emotional voice. Options are (if supported):<br><br>
	 * 0 - neutral<br>
	 * 1 - happy<br>
	 * 2 - sad<br>
	 * 3 - angry<br>
	 * 4 - shout<br>
	 * 5 - whisper<br>
	 * 6 - fun1 (e.g. old)<br>
	 * 7 - fun2 (e.g. Yoda)<br>
	 * <br>
	 * Note that currently only the English voice supports all of them.
	 * Voices that support less emotions default to neutral.<br>
	 * Note as well that smileys in the text will overwrite this index!. 
	 */
	public boolean setMood(int mood);
	
	/**
	 * Sets the speed of the TTS output (how fast the voice talks).
	 * @param speed_factor - a factor from 0.0 (very slow) to e.g. 3.0 (3 times faster?) modifying the default speed.
	 */
	public void setSpeedFactor(double speed_factor);
	
	/**
	 * Sets the tone of the voice (darker or brighter).
	 * @param tone_factor - a factor from e.g. 0.0 (maximum dark) to 10.0 (very bright) modifying the tone.
	 */
	public void setToneFactor(double tone_factor);
	
	/**
	 * Set settings with settings class (voice, speed, tone, etc.).
	 */
	public void setSettings(TtsSettings settings);
	
	/**
	 * Get current settings (voice, speed, tone, etc.).
	 */
	public TtsSettings getSettings();

}
