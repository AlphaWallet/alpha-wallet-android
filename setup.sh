#!/usr/bin/env bash

# .o
cc -c -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin app/src/main/cpp/keys.c

# get  xxx.dylib
g++ -dynamiclib -undefined suppress -flat_namespace *.o -o libkeys.dylib

# OUTPUT=`pwd`/app/src/test/libs/
OUTPUT=~/Library/Java/Extensions
mkdir -p ${OUTPUT}
mv libkeys.dylib ${OUTPUT}/
