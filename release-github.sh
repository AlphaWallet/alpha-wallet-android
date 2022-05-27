#!/bin/sh

./gradlew assembleRelease bundleRelease

version_name=$(./gradlew -q printVersionName)
tag=v$version_name

src_apk=$(pwd)/app/build/outputs/apk/noAnalytics/release/app-noAnalytics-release.apk
dst_apk=$(pwd)/AlphaWallet-$tag.apk

cp $src_apk $dst_apk
gh release create --generate-notes -d $tag $dst_apk

temp_file=build/temp_release_notes.txt
gh release view $tag --json body -q .body > $temp_file

sed -i '' -n '2,/^$/p' $temp_file
sed -i '' 's/\ by\ @.*$//' $temp_file
sed -i '' 's/*\ /-\ /' $temp_file
gh release edit $tag -F $temp_file

rm $dst_apk
rm $temp_file
