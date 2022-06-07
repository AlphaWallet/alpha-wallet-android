#!/bin/sh

version_name=$(./gradlew -q printVersionName)
tag=v$version_name

version_code=$(./gradlew -q printVersionCode)
change_logs_dir=./fastlane/metadata/android/en-US/changelogs
change_log_file=$change_logs_dir/$version_code.txt

if [ -z "$1" ]; then
  echo "Downloading change log from GitHub"
  gh release view $tag --json body -q .body > $change_log_file
  chars=$(wc -c $change_log_file | awk '{print $1}')
  if (($chars > 500));
  then
    echo "The release created has notes in language en-US with length $chars, which is too long (max: 500)."
    exit 1
  fi
else
  echo "Copy change log from last version"
  cp $change_logs_dir/$(($version_code-1)).txt $change_log_file
fi

echo "Run below command to release:"
echo "sh scripts/release-playstore.sh"
