# txt2pho - German TTS

"A TTS frontend for the German inventories of the MBROLA project."  
  
Alternative to 'espeak-ng-mbrola' for German voices ONLY that can give noticeable better quality with approx. same resource requirements (low), but you may encounter a few more artifacts.  
Official respository: https://github.com/GHPS/txt2pho  
  
## Install

- Install MBROLA voices first (see '../espeak-ng-mbrola' folder)
- Run: `bash install.sh` to build txt2pho
- SEPIA will use 'txt2pho-speak.sh' by default to create audio files

## Settings

See `$HOME/.config/txt2phorc` after installation to change common speed of voices (e.g.: 0.9 faster, 1.1 slower) and some other settings.

## License

AGPL-3.0 License