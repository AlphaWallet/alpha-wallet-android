#!/bin/sh

version=$(./gradlew -q printVersionName)
tag=v$version

./gradlew assembleRelease bundleRelease

src_apk=$(pwd)/app/build/outputs/apk/noAnalytics/release/app-noAnalytics-release.apk
dst_apk=$(pwd)/AlphaWallet-$tag.apk

cp $src_apk $dst_apk
gh release create --generate-notes -d $tag $dst_apk
