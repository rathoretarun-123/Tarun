#!/usr/bin/env bash
# Builds the Kahani Film APK without Gradle (works offline with Debian/Ubuntu android tools).
#   sudo apt-get install -y android-sdk-platform-23 aapt dalvik-exchange zipalign apksigner openjdk-17-jdk
# Output: release/KahaniFilm.apk
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
# Full Android 14 API for compiling (Robolectric's android-all jar from Maven Central).
ALL_JAR=${ALL_JAR:-$HOME/.cache/kahani/android-all-14.jar}
if [ ! -f "$ALL_JAR" ]; then
  mkdir -p "$(dirname "$ALL_JAR")"
  curl -fL -o "$ALL_JAR" https://repo1.maven.org/maven2/org/robolectric/android-all/14-robolectric-10818077/android-all-14-robolectric-10818077.jar
fi
VERSION_CODE=$(grep -o 'versionCode="[0-9]*"' app/src/main/AndroidManifest.xml | grep -o '[0-9]*')
VERSION_NAME=$(grep -o 'versionName="[^"]*"' app/src/main/AndroidManifest.xml | cut -d'"' -f2)

B=build
rm -rf $B && mkdir -p $B/gen $B/classes
echo "[1/6] resources"
aapt2 compile --dir app/src/main/res -o $B/res.zip
aapt2 link -o $B/base.apk -I "$ALL_JAR" --manifest app/src/main/AndroidManifest.xml \
  -A app/src/main/assets --java $B/gen --min-sdk-version 26 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" -0 jpg -0 png $B/res.zip
echo "[2/6] compile java"
javac -nowarn -source 8 -target 8 -encoding UTF-8 -bootclasspath "$ALL_JAR:$ANDROID_JAR" -d $B/classes \
  $(find app/src/main/java -name '*.java') $(find $B/gen -name '*.java') 2>&1 | grep -v "warning\|^Note\|JAVA_TOOL" || true
test -f $B/classes/com/tarun/kahani/app/MainActivity.class
echo "[3/6] dex"
dalvik-exchange --dex --min-sdk-version=26 --output=$B/classes.dex $B/classes
echo "[4/6] package"
cp $B/base.apk $B/unsigned.apk
(cd $B && zip -q -j unsigned.apk classes.dex)
echo "[5/6] align"
zipalign -f 4 $B/unsigned.apk $B/aligned.apk
echo "[6/6] sign"
mkdir -p release
apksigner sign --ks tools/debug.keystore --ks-pass pass:android --key-pass pass:android --ks-key-alias kahani \
  --out release/KahaniFilm.apk $B/aligned.apk
apksigner verify release/KahaniFilm.apk
ls -la release/KahaniFilm.apk
