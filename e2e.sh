#!/bin/sh

function startGanache() {
  source ~/.zprofile
  ganache --chain.chainId 2 -h 0.0.0.0 -m "horse light surface bamboo combine item lumber tunnel choose acid mail feature"
}

function stopGanache() {
  kill -9 $(lsof -t -i:8545)
}

# disable animations or test may not stable
adb shell settings put global window_animation_scale 0.0
adb shell settings put global transition_animation_scale 0.0
adb shell settings put global animator_duration_scale 0.0

# Sometimes Android Espresso performs longClick instead of click
# https://stackoverflow.com/questions/32330671/android-espresso-performs-longclick-instead-of-click
adb shell settings put secure long_press_timeout 1500

rm -rf output
mkdir output
adb shell rm /storage/emulated/0/DCIM/*.png

adb logcat -c                             # clear logs
touch output/emulator.log                    # create log file
chmod 666 output/emulator.log                # allow writing to log file
adb logcat >> output/emulator.log &

startGanache &

./gradlew :app:uninstallAll :app:connectedNoAnalyticsDebugAndroidTest -x lint -PdisablePreDex

if [ "$?" == "0" ]; then
  stopGanache
  exit 0
fi

adb pull /storage/emulated/0/DCIM/ output
if [ "$1" != "--CI" ]; then
  open output/DCIM/*.png
fi

# Copy test report
cp -r app/build/reports/androidTests/connected/flavors/noAnalytics/ output/html

stopGanache
exit 1
