#!/usr/bin/env bash

#No DMZ until we fix the Gradle 8 build. Deprecate util build
#cd dmz && ../gradlew -i build && ../gradlew -i test && cd ..
cd lib && ../gradlew -i build && ../gradlew -i test && cd ..
#cd util && ../gradlew -i build && ../gradlew -i test && cd ..
#./gradlew clean jacocoTestNoAnalyticsDebugUnitTestReport -x lint -x detekt
