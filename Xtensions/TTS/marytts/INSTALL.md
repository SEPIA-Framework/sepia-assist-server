# TTS Engine: Mary-TTS

Original: https://github.com/marytts
Fork release: https://github.com/fquirin/marytts/releases/latest/download/marytts.zip  
  
This is the folder for MaryTTS (Text-To-Speech) voice server that can be used by the TTS module of SEPIA to generate speech.
It has been integrated as server extension due to it's memory requirements of around 250MB that will not be available on all supported hardware (e.g. RPi3 or smaller).  
  
To install the extension simply use one of the dowload scripts. The start scripts of SEPIA will automatically load the server if the downloaded files are available.

## Server endpoints

Some useful endpoints of the original MaryTTS server. 
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
* process - requests the synthesis of some text (see below).
