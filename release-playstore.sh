#!/bin/sh

version_name=$(./gradlew -q printVersionName)
tag=v$version_name
release_notes=$(gh release view $tag --json body -q .body)

version_code=$(./gradlew -q printVersionCode)
echo $release_notes > ./fastlane/metadata/android/en-US/changelogs/$version_code.txt 
fastlane android beta
