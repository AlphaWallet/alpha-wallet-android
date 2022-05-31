#!/usr/bin/env bash

cd dmz && ../gradlew -i build && ../gradlew -i test && cd ..
cd lib && ../gradlew -i build && ../gradlew -i test && cd ..
cd util && ../gradlew -i build && ../gradlew -i test && cd ..
./gradlew assembleDebug -x lint