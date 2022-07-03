# TTS Engine: Mary-TTS

Original: https://github.com/marytts  
Fork release: https://github.com/fquirin/marytts/releases/latest/download/marytts.zip  
  
This is the folder for MaryTTS (Text-To-Speech) voice server that can be used by the TTS module of SEPIA to generate speech.
It has been integrated as server extension due to it's memory requirements of around 250MB that will not be available on all supported hardware (e.g. RPi3 or smaller).  

## Installation

Note: It is required to use a system with 2GB memory or more to add this extension, but you can run the server on a different machine as well (see comment below). 
  
* To install the extension simply use one of the download scripts. The start scripts of SEPIA will automatically load the server if the downloaded files are available.  
* To remove the server simply delete the downloaded files.
* Alternatively you can use the **Docker containers** available at: [sepia/marytts](https://registry.hub.docker.com/r/sepia/marytts)

## Mary-TTS info and endpoints

### Direct access to server

When you run Mary-TTS via SEPIA setup (non-Docker) direct access to the Mary-TTS server is only possible via `localhost`. You can open up Mary-TTS for direct access from any machine and apply some other settings via then environmental variable `MARYTTS_SERVER_OPTS`.
Here is an example command that will remove the localhost restriction, to be run BEFORE the SEPIA server starts up Mary-TTS: `export MARYTTS_SERVER_OPTS="-Dsocket.addr=0.0.0.0 -Dsocket.port=59125"`.  
Keep in mind that you can of cause always use the SEPIA TTS endpoint to access Mary-TTS from anywhere ... with the proper authentication.

### Running Mary-TTS on a different machine

When your SEPIA server doesn't have enough resources left to run the Mary-TTS server you can move it to a different system by simply copying the `marytts` folder.  
Note: Your target system has to have **Java installed**.  
After you've copied the files run your Mary-TTS server without 'localhost' restriction, e.g.:
```
cd marytts/bin
export MARYTTS_SERVER_OPTS="-Dsocket.addr=0.0.0.0 -Dsocket.port=59125"
bash marytts-server
```

After you've set up Mary-TTS on a different machine **update your 'marytts_server' field in SEPIA settings** to point to the new location (URL).

### Solving CORS Problems

If you want to access your Mary-TTS server from the web browser (different domain) or SEPIA client via the direct-access setting (custom TTS voice options) you will likely get CORS errors because the Mary-TTS server only allows requests from its own domain.
So far there is no known settings option to prevent this, but we can use a reverse-proxy to solve the problem. If you've installed Nginx during your SEPIA server setup add the following lines (replacing [mary-tts-server-IP]) to any of your '.conf' files:

```
location /marytts/ {
	add_header Access-Control-Allow-Origin "$http_origin" always;
	add_header Access-Control-Allow-Headers "Origin, Content-Type, Accept" always;
	add_header Access-Control-Allow-Methods "GET, POST, PUT, OPTIONS, DELETE" always;
	proxy_pass http://[mary-tts-server-IP]:59125/;
}
```

**Alternatively** just use the Docker container. It has an integrated proxy to avoid CORS problems.

### Server endpoints

Some useful endpoints of the original MaryTTS server in case you want to use it for other projects. 
Default address is "http://localhost:59125/" followed e.g. by:

* version - requests the version of the MARY server
* datatypes - requests the list of available data types
* locales - requests the list of available locales / language components
* voices - requests the list of available voices
* audioformats - requests the list of supported audio file format types
* exampletext?voice=hmm-slt - requests the example text for the given voice
* exampletext?datatype=RAWMARYXML&amp;locale=de - requests an example text for data of the given type and locale
* audioeffects - requests the list of default audio effects
* audioeffect-default-param?effect=Robot - requests the default parameters of the given audio effect
* audioeffect-full?effect=Robot&amp;params=amount:100.0 - requests a full description of the given audio effect, including effect name, parameters and help text
* audioeffect-help?effect=Robot - requests a help text describing the given audio effect
* audioeffect-is-hmm-effect?effect=Robot - requests a boolean value (plain text "yes" or "no") indicating whether or not the given effect is an effect that operates on HMM-based voices only
* features?locale=de - requests the list of available features that can be computed for the given locale
* features?voice=hmm-slt - requests the list of available features that can be computed for the given voice
* vocalizations?voice=dfki-poppy - requests the list of vocalization names that are available with the given voice
* styles?voice=dfki-pavoque-styles - requests the list of style names that are available with the given voice
* process - requests the synthesis of some text, e.g.: `/process?INPUT_TEXT=test&INPUT_TYPE=TEXT&LOCALE=en_GB&VOICE=dfki-prudence-hsmm&OUTPUT_TYPE=AUDIO&AUDIO=WAVE_FILE`
