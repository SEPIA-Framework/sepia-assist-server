# TTS Engines Folder

Put your TTS engines here to use them via 'tts' endpoint.

## Espeak

Espeak is a very light-weight, free speech synthesis engine that is available for Windows and Linux. 
It generates a rather robotic (male-ish) voice but fits very well to SEPIA :-). It has support for many languages though the SEPIA integration will only use the DE and EN voice at the moment.  
  
See the [espeak-ng readme](espeak-ng/README.md) for more info. If you install TTS via the SEPIA setup on Linux it will be available automatically.

### Espeak-NG MBROLA Extension

MBROLA voices are available for many languages and can sound considerably better than classic espeak while still being very fast and resource friendly.  
Read more about it and about the LICENSE conditions in the [espeak-ng-mbrola readme](espeak-ng-mbrola/README.md).

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

## Txt2pho

[Txt2pho](https://github.com/GHPS/txt2pho) is an alternative to 'espeak-ng-mbrola' for German MBROLA voices ONLY that can give noticeable better quality with approx. same resource requirements (low), but you may encounter a few more artifacts.  
License is AGPL-3.0.
  
See the [txt2pho readme](txt2pho/README.md) for more info.

## Larynx

[Larynx](https://github.com/rhasspy/larynx) is an open-source server for (experimental) high-quality, next-gen voices that is compatible with MaryTTS API and SEPIA since Larynx version 1.0.3..  
It is pretty resource hungry but you can try and experiment with the low and medium quality settings on Raspberry Pi 4. It is recommended to use the 64bit version (aarch64 on RPi) for better performance.

## Mycroft Mimic 3

Mycroft's [Mimic 3](https://github.com/MycroftAI/mimic3) has some high quality voices at reasonable resource requirements. It usually runs about 2 times faster than real-time on a Raspberry Pi 4, which is ok for smaller sentences.  
It can be used as a drop-in replacement for the Mary-TTS server. Just point your SEPIA server to the right URL.  
NOTE: Similar to Mary-TTS you should have enough RAM to run the server if you want to use it right next to your other SEPIA components.

## Coqui TTS

TBD

## Acapela

[Acapela](https://www.acapela-group.com/demos/) is a commercial system that generates state-of-the-art high quality voices. There is basic support for Acapela but it has NOT been tested for a long time.
Consider this to be experimental, experience reports are welcome ;-)
