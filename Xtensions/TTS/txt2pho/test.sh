#!/bin/bash
echo "Creating a few txt2pho and espeak MBROLA samples ..."
echo "Hallo Welt, dies ist ein Test." |./txt2pho -m | mbrola /usr/share/mbrola/de4/de4 - test.wav
echo "1000 1/2 am 1.5.2021 un 2°C" | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -m | mbrola /usr/share/mbrola/de4/de4 - test2.wav
echo "Äpfel und Birnen sind lecker" | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -m | mbrola /usr/share/mbrola/de4/de4 - test3.wav
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -m | mbrola /usr/share/mbrola/de3/de3 - test4.wav
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -f | mbrola /usr/share/mbrola/de3/de3 - test4_f.wav
espeak-ng -v mb-de3 -w test4_es.wav "Hallo Welt, dies ist ein Test."
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -m | mbrola /usr/share/mbrola/de4/de4 - test5.wav
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -f | mbrola /usr/share/mbrola/de4/de4 - test5_f.wav
espeak-ng -v mb-de4 -w test5_es.wav "Hallo Welt, dies ist ein Test."
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -m | mbrola /usr/share/mbrola/de5/de5 - test6.wav
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -f | mbrola /usr/share/mbrola/de5/de5 - test6_f.wav
espeak-ng -v mb-de5 -w test6_es.wav "Hallo Welt, dies ist ein Test."
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -m | mbrola /usr/share/mbrola/de6/de6 - test7.wav
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -f | mbrola /usr/share/mbrola/de6/de6 - test7_f.wav
espeak-ng -v mb-de6 -w test7_es.wav "Hallo Welt, dies ist ein Test."
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -m | mbrola /usr/share/mbrola/de7/de7 - test8.wav
echo "Hallo Welt, dies ist ein Test." | iconv -cs -f UTF-8 -t ISO-8859-1 | ./preproc data/PPRules/rules.lst data/hadifix.abk | ./txt2pho -f | mbrola /usr/share/mbrola/de7/de7 - test8_f.wav
espeak-ng -v mb-de7 -w test8_es.wav "Hallo Welt, dies ist ein Test."
echo "DONE"