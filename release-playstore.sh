#!/bin/sh

version_name=$(./gradlew -q printVersionName)
tag=v$version_name

version_code=$(./gradlew -q printVersionCode)
change_log_file=./fastlane/metadata/android/en-US/changelogs/$version_code.txt
gh release view $tag --json body -q .body > $change_log_file

chars=$(wc -c $change_log_file | awk '{print $1}')
if (($chars > 500));
then
  echo "The release created has notes in language en-US with length $chars, which is too long (max: 500)."
else
  fastlane android beta
fi

