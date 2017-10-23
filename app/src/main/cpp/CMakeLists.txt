cmake_minimum_required(VERSION 3.4.1)

add_library(native-lib SHARED
            native-lib.c)

# Include libraries needed for native-lib lib
target_link_libraries(native-lib
                      android
                      log)
