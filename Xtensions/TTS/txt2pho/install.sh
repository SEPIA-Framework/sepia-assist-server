#!/bin/bash
set -e
if command -v mbrola &> /dev/null;
then
	echo "Found: MBROLA"
else
	echo "Please install MBROLA with voices first!"
	exit 1
fi
SCRIPT_PATH="$(realpath "$BASH_SOURCE")"
SCRIPT_FOLDER="$(dirname "$SCRIPT_PATH")"
cd $SCRIPT_FOLDER
echo "Cloning txt2pho..."
git clone https://github.com/GHPS/txt2pho.git build
cd build
if [ -x "$(command -v apt)" ]; then
	echo "Checking packages: g++"
	sudo apt update
	sudo apt install -y g++
else
	echo "Recommended packages to build: g++"
fi
make clean
make all
mv data ../
mv txt2pho ../
mv preproc ../
mv pipefilt ../
mv settings/txt2phorc ../txt2phorc
mv license ../
mv doc ../
cd ..
echo "txt2pho path: $SCRIPT_FOLDER"
echo "Preparing settings file..."
sed -i "s|DATAPATH=.*|DATAPATH=""$SCRIPT_FOLDER""/data/|" txt2phorc
sed -i "s|INVPATH=.*|INVPATH=""$SCRIPT_FOLDER""/data/|" txt2phorc
sed -i "s|TEMPPATH=.*|TEMPPATH=""$SCRIPT_FOLDER""/tmp/|" txt2phorc
sed -i "s|SPEECHRATE=.*|SPEECHRATE=1.06|" txt2phorc
echo "Copying settings to '$HOME/.config/txt2phorc'..."
mkdir -p "$HOME/.config"
cp txt2phorc "$HOME/.config/txt2phorc"
mkdir -p tmp
chmod +x txt2pho-speak-optimize.sh
chmod +x txt2pho-speak.sh
chmod +x txt2pho
chmod +x preproc
chmod +x pipefilt

