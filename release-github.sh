#!/bin/sh

./gradlew assembleRelease bundleRelease

version_name=$(./gradlew -q printVersionName)
tag=v$version_name

src_apk=$(pwd)/app/build/outputs/apk/noAnalytics/release/app-noAnalytics-release.apk
dst_apk=$(pwd)/AlphaWallet-$tag.apk

cp $src_apk $dst_apk
gh release create -d $tag $dst_apk
rm $dst_apk
