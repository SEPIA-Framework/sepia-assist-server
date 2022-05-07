#!/bin/bash
if [ "$#" -ne 4 ]; then
  echo "Usage: bash speak.sh [gender] [voice] [text] [output file]"
  echo "Example: bash speak.sh f de3/de3 \"Hallo Welt\" hallo_welt.wav"
  exit 1
fi
script_path=$(dirname "$0")
cd "$script_path"
echo "$3" | iconv -cs -f UTF-8 -t ISO-8859-1 | ./txt2pho "-$1" | mbrola /usr/share/mbrola/"$2" - "../../../""$4"
#echo "$1 - $2 - $3 - ../../../$4" >> log.out
