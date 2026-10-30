#!/usr/bin/env bash
# Runs inside the emulator job: smoke-tests the release APK the way a person would (install, open,
# no crash, reopen), then runs the screen test at one of the Fold's screen sizes and keeps screenshots.
set -uo pipefail
apkdir="$1"     # folder with SnakesAndLadders-ci.apk, SnakesAndLadders-debug.apk, SnakesAndLadders-androidTest.apk
results="$2"
screen="$3"     # cover-screen or inner-screen
pkg=com.pandyabhavik.snakesandladders
mkdir -p "$results"
status=0

adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 1; done'
sleep 5

if [ "$screen" = cover-screen ]; then
  adb shell wm size 1080x2520
  adb shell wm density 420
  # Larger text, as many people use on the small screen: nothing should be cut off.
  adb shell settings put system font_scale 1.15
else
  adb shell wm size 1968x2184
  adb shell wm density 374
fi
sleep 3

echo "== Smoke test of the release APK =="
adb logcat -c
adb install -r "$apkdir/SnakesAndLadders-ci.apk"
adb shell am start -W -n $pkg/com.pandyabhavik.snakes.MainActivity
sleep 6
adb exec-out screencap -p > "$results/release-1-first-run.png"
adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell am start -W -n $pkg/com.pandyabhavik.snakes.MainActivity
sleep 4
adb exec-out screencap -p > "$results/release-2-reopened.png"
adb logcat -d -b crash > "$results/crash-release.txt"
if grep -q "$pkg" "$results/crash-release.txt"; then
  echo "::error::The release APK crashed"
  cat "$results/crash-release.txt"
  status=1
elif ! adb shell pidof $pkg >/dev/null; then
  echo "::error::The release APK isn't running after launch"
  status=1
else
  echo "Release APK: opens and stays open"
fi
adb logcat -d > "$results/logcat-release.txt"
adb uninstall $pkg >/dev/null || true

echo "== Screen test on the debug APK =="
adb logcat -c
adb install -r "$apkdir/SnakesAndLadders-debug.apk"
adb install -r "$apkdir/SnakesAndLadders-androidTest.apk"
adb shell am instrument -w -r -e class com.pandyabhavik.snakes.ScreensTest \
  $pkg.test/androidx.test.runner.AndroidJUnitRunner | tee "$results/instrument.txt"
if ! grep -q "OK (" "$results/instrument.txt"; then
  echo "::error::The screen test failed"
  status=1
fi
# The test saves its screenshots in the app's external files folder, which adb can read.
adb pull "/sdcard/Android/data/$pkg/files/screenshots" "$results/" 2>/dev/null || echo "No test screenshots found"
adb logcat -d > "$results/logcat-test.txt"
adb logcat -d -b crash > "$results/crash-test.txt"

ls -la "$results"
exit $status
