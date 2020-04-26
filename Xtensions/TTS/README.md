# TTS Engines Folder

Put your TTS engines here to use them via 'tts' endpoint.

## Espeak

Espeak is a very light-weight, free speech synthesis engine that is available for Windows and Linux. 
It generates a rather robotic (male-ish) voice but fits very well to SEPIA :-). It has support for many languages though the SEPIA integration will only use the DE and EN voice at the moment.  
  
See the [espeak-ng readme](espeak-ng/README.md) for more info. If you install TTS via the SEPIA setup on Linux it will be available automatically.

## Pico TTS

Pico is a voice engine by SVOX that was initially developed as free version and used in Android on-device TTS. 
They have since moved on to do commercial stuff but the old system is still available and gives solid results at low resource requirements.  
  
Pico is only available for Linux at the moment. If you install TTS via the SEPIA setup it will be available automatically.

## MaryTTS

[MaryTTS](http://mary.dfki.de/) is an open-source engine developed by the german AI research center DFKI. 
It has been around for many years and proven to be a very popular, versatile and solid TTS system that can be extended with custom voices (in theory ^^).
Quality of the voices can vary from 'quite high' to 'rather basic' so be sure to check-out all the options. The SEPIA edition comes with a pre-selected set of two DE and four EN voices (male and female) + an easter-egg ;-).
  
See the [mary-tts readme](marytts/INSTALL.md) for more info.  
NOTE: This extension requires at least 2GB of memory when running in parallel to the SEPIA server, but you can run the server on a different machine as well (see SEPIA settings 'marytts_server').

## Acapela

[Acapela](https://www.acapela-group.com/demos/) is a commercial system that generates state-of-the-art high quality voices. There is basic support for Acapela but it has NOT been tested for a long time.
Consider this to be experimental, experience reports are welcome ;-)
