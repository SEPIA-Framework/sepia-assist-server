# TTS Engine: MBROLA Voices for Espeak-NG

Info: https://github.com/espeak-ng/espeak-ng/blob/master/docs/mbrola.md  
  
In this folder you will find the install script for MBROLA voices. The voices are used as plugins for Espeak-NG so you don't have to switch the engine. They are equally efficient and fast, but can sound considerably more natural.  
  
Please read the [LICENSE](https://github.com/numediart/MBROLA-voices/blob/master/LICENSE.md) file! While the **voices are open and free not all of them can be used commercially** without asking the creator for permission.
In addition [MBROLA](https://github.com/numediart/MBROLA/) itself is AGPL-3.0 license meaning if you build a product your code needs to stay open!  

## Installation

### Linux

Use the install script: `bash install.sh`  
  
The script will do the following steps:
- Clone MBROLA repository from https://github.com/numediart/MBROLA/
- Build MBROLA and copy binary to '/usr/bin/mbrola'
- Download voices and copy to '/usr/share/mbrola/'

### Windows

Process is unclear atm :-/. Some steps:

- Check this issue for Mbrola.dll: https://github.com/espeak-ng/espeak-ng/issues/723
- Download voices from: https://github.com/numediart/MBROLA-voices
- ???