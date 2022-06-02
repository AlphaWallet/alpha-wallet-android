#!/bin/sh

version_name=$(./gradlew -q printVersionName)
tag=v$version_name

version_code=$(./gradlew -q printVersionCode)
change_logs_dir=./fastlane/metadata/android/en-US/changelogs
change_log_file=$change_logs_dir/$version_code.txt

fastlane android beta
if [ $? -eq 0 ]; then
  echo "New release candidate $tag published on internal testing track, visit link on your Android device to install:"
  echo "https://play.google.com/apps/test/io.stormbird.wallet/$version_code"
  echo "\nUpdates:"
  cat $change_log_file
fi
