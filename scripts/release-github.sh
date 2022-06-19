#!/bin/sh

./gradlew assembleRelease bundleRelease

version_name=$(./gradlew -q printVersionName)
tag=v$version_name

src_apk=$(pwd)/app/build/outputs/apk/noAnalytics/release/app-noAnalytics-release.apk
dst_apk=$(pwd)/AlphaWallet-$tag.apk
cp $src_apk $dst_apk

latest_release_tag=$(gh release list -L 1 | cut -d$'\t' -f3)
is_draft=$(gh release view $latest_release_tag --json isDraft | jq .isDraft)

temp_file=build/temp_release_notes.txt
if [ "$is_draft" = "true" ]; then
  gh release view $latest_release_tag --json body -q .body > $temp_file
  gh release delete $latest_release_tag
  echo "Creating new release with tag $tag"
  gh release create $tag $dst_apk -t $tag -F $temp_file -d
  echo "Click above link to edit change log, make it short than 500 chars."
else
  echo "Creating new release with tag $tag"
  gh release create $tag $dst_apk --generate-notes -d > /dev/null
  gh release view $tag --json body -q .body > $temp_file
  sed -i '' -n '2,/^$/p' $temp_file
  sed -i '' 's/\ by\ @.*$//' $temp_file
  sed -i '' 's/*\ /-\ /' $temp_file
  gh release edit $tag -F $temp_file
  echo "Click above link to edit change log, make it short than 500 chars."
fi

rm $dst_apk
rm $temp_file
