#!/bin/bash
set -e
downloadurl="https://github.com/fquirin/marytts/releases/latest/download/marytts.zip"
echo "Downloading MaryTTS server. This might take a few minutes ..."
echo
echo "URL: $downloadurl"
echo
wget -O "marytts.zip" "$downloadurl"
echo "Extracting Zip file ..."
unzip marytts.zip -d $(pwd)
echo "Cleaning up ..."
rm "marytts.zip"
echo "DONE"


