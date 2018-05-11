# How to create a keystore (e.g. from "Let's encrypt" certificate):
https://uwesander.de/using-your-ssl-certificate-for-your-spark-web-application.html

# Sample bash script:

#!/bin/bash

CERT_PATH=/opt/psa/var/modules/letsencrypt/etc/live/mydomain/
CERT_ALIAS=mydomain_letsencrypt
PKCS_FILENAME=pkcs.p12
PKCS_PW='my_pkcs_pw'
KEYSTORE_FILENAME=ssl-keystore.jks
KEYSTORE_PW='my_keystore_pw'

# generate PKCS12 file
openssl pkcs12 -export -in $CERT_PATH/fullchain.pem -inkey $CERT_PATH/privkey.pem -out $PKCS_FILENAME -name $CERT_ALIAS -passout pass:$PKCS_PW

# delete existing entry in Java keystore
keytool -delete -keystore $KEYSTORE_FILENAME -alias $CERT_ALIAS -storepass $KEYSTORE_PW

# add new Java keystore entry from PKCS12 file
keytool -importkeystore -deststorepass $KEYSTORE_PW -destkeypass $KEYSTORE_PW -destkeystore $KEYSTORE_FILENAME -srckeystore $PKCS_FILENAME -srcstoretype PKCS12 -srcstorepass $PKCS_PW -alias $CERT_ALIAS

rm $PKCS_FILENAME

