#!/bin/sh

image='cimg/android:2022.04.1-ndk'
cmd='./gradlew testNoAnalyticsDebugUnitTest'
if [[ "$(docker images -q $image 2> /dev/null)" == "" ]]; then
  docker pull $image
fi
docker run --platform linux/amd64 -it --rm -v $(pwd):/code $image bash -c "cd /code && $cmd"
