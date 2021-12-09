#!/bin/sh

# disable animations or test may not stable
adb shell settings put global window_animation_scale 0.0
adb shell settings put global transition_animation_scale 0.0
adb shell settings put global animator_duration_scale 0.0

./gradlew :app:uninstallAll :app:connectedNoAnalyticsDebugAndroidTest -x lint -PdisablePreDex
