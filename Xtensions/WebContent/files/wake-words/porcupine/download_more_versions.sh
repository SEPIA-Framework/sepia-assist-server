#!/bin/bash
set -e
v20en="https://sepia-framework.github.io/files/porcupine/2.0_en/pv_porcupine.wasm"
v20de="https://sepia-framework.github.io/files/porcupine/2.0_de/pv_porcupine.wasm"
echo "Downloading Porcupine WASM files for 2.0 (en, de)..."
echo
mkdir -p "2.0_en"
if [ -f "2.0_en/pv_porcupine.wasm" ]; then
	echo "v2.0_en WASM already exists"
else
	wget -O "2.0_en/pv_porcupine.wasm" "$v20en"
fi
mkdir -p "2.0_de"
if [ -f "2.0_de/pv_porcupine.wasm" ]; then
	echo "v2.0_de WASM already exists"
else
	wget -O "2.0_de/pv_porcupine.wasm" "$v20de"
fi
echo "DONE"
