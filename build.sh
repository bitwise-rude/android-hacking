#!/bin/bash

echo "-----Uninstalling on device-----"
adb uninstall com.minimal.app

echo "-----Linking With AAPT2-------"
mkdir output
aapt2 link -o output/base.apk --manifest AndroidManifest.xml -I requirements/android.jar



echo "-----Compiling Java-------"
mkdir obj
javac -d obj -classpath requirements/android.jar src/com/minimal/MainActivity.java


echo "-----Compiling to Dalvik-------"
d8 --output . --lib requirements/android.jar obj/com/minimal/app/MainActivity.class


echo "------Zipping and Aligning------"
zip -uj output/base.apk classes.dex
zipalign -v 4 output/base.apk output/final_app.apk


echo "------Signing With Key------"
if [ ! -f "signing.jks" ]; then
    echo "Keystore not found generating"
    keytool -genkey -v -keystore "signing.jks" -alias my-key -keyalg RSA -keysize 2048 -validity 10000 -storepass password -keypass password -dname "CN=Example, OU=Dev, O=Dev, L=City, S=State, C=US"
fi

apksigner sign -ks "signing.jks" --ks-pass pass:password --out output/final_install.apk output/final_app.apk


echo "------------------------------------"
echo "Success! APK created at $OUT_DIR/base.apk Now Cleaning..."

mv output/final_install.apk base.apk
rm -rf output
rm  classes.dex
rm -rf obj

echo "------------------------------------"
echo "Installing on Android"

adb install base.apk
adb shell am start -n com.minimal.app/com.minimal.app.MainActivity

