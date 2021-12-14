#!/bin/bash
set -e
echo "This script will download and install MBROLA for espeak-ng and download a few selected voices."
echo "If you need more voices feel free to edit this script ;-)."
echo "MBROLA and the included voices can be used freely for private, non-commercial SEPIA projects."
echo ""
echo "If you are planning to use MBROLA in a public or commercial project please read the following"
echo "LICENSE files carefully:"
echo ""
echo "MBROLA AGPL 3.0 license: https://github.com/numediart/MBROLA/blob/master/LICENSE"
echo "MBROLA voices terms of use: https://github.com/numediart/MBROLA-voices/blob/master/LICENSE.md"
echo ""
read -p "Enter 'agree' to continue: " agreeornot
echo ""
if [ -n "$agreeornot" ] && [ $agreeornot = "agree" ]; then
	echo "Ty, let's go!"
else
	echo "Ok. See you :-)"
	exit
fi
# Install MBROLA
echo "Downloading and building MBROLA..."
sudo apt-get update
sudo apt-get install make gcc
git clone https://github.com/numediart/MBROLA.git
cd MBROLA
make
sudo cp Bin/mbrola /usr/bin/mbrola
echo ""
echo "Downloading voices..."
mkdir -p "voices/de4"
wget -O voices/de4/de4 "https://github.com/numediart/MBROLA-voices/blob/master/data/de4/de4?raw=true"
mkdir -p "voices/de5"
wget -O voices/de5/de5 "https://github.com/numediart/MBROLA-voices/blob/master/data/de5/de5?raw=true"
mkdir -p "voices/de6"
wget -O voices/de6/de6 "https://github.com/numediart/MBROLA-voices/blob/master/data/de6/de6?raw=true"
mkdir -p "voices/de7"
wget -O voices/de7/de7 "https://github.com/numediart/MBROLA-voices/blob/master/data/de7/de7?raw=true"
mkdir -p "voices/en1"
wget -O voices/en1/en1 "https://github.com/numediart/MBROLA-voices/blob/master/data/en1/en1?raw=true"
echo "Copying voices to '/usr/share/mbrola/'..."
sudo mkdir -p "/usr/share/mbrola/"
sudo cp -r voices/* /usr/share/mbrola/
cd ..
#echo "Cleaning up ..."
#rm -rf "MBROLA"
echo "DONE."


