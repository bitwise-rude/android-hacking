#!/bin/bash

$SDK_PATH = "/opt/android-sdk/build-tools"
$BUILD_VERSION = "35.0.0"
$PLATFORM_VERSION = "android-35"

AAPT2="$SDK_PATH/build-tools/$BUILD_TOOLS_VERSION/aapt2"
D8="$SDK_PATH/build-tools/$BUILD_TOOLS_VERSION/d8"
ZIPALIGN="$SDK_PATH/build-tools/$BUILD_TOOLS_VERSION/zipalign"
APKSIGNER="$SDK_PATH/build-tools/$BUILD_TOOLS_VERSION/apksigner"
ANDROID_JAR="requirements/android.jar"

MANIFEST="AndroidManifest.xml"
SRC_DIR="src" 
OUT_DIR="build"
GEN_DIR="gen"
KEYSTORE="my-release-key.jks"

rm -rf $OUT_DIR $GEN_DIR
mkdir -p $OUT_DIR/obj $GEN_DIR

$AAPT2 link -o $OUT_DIR/base.apk --manifest $MANIFEST -I $ANDROID_JAR
find $SRC_DIR -name "*.java" > sources.txt
javac -d $OUT_DIR/obj -classpath $ANDROID_JAR @sources.txt
rm sources.txt

$D8 --output $OUT_DIR --lib $ANDROID_JAR $(find $OUT_DIR/obj -name "*.class")
cd $OUT_DIR
zip -uj base.apk classes.dex
cd ..

$ZIPALIGN -f -v 4 $OUT_DIR/base.apk $OUT_DIR/app-aligned.apk
if [ ! -f $KEYSTORE ]; then
    echo "Keystore not found! Generating a temporary one..."
    keytool -genkey -v -keystore $KEYSTORE -alias my-key -keyalg RSA -keysize 2048 -validity 10000 -storepass password -keypass password -dname "CN=Example, OU=Dev, O=Dev, L=City, S=State, C=US"
fi

$APKSIGNER sign --ks $KEYSTORE --ks-pass pass:password --out $OUT_DIR/final_app.apk $OUT_DIR/app-aligned.apk
echo "------------------------------------"
echo "Success! APK created at $OUT_DIR/final_app.apk"

