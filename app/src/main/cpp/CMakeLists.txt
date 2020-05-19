cmake_minimum_required(VERSION 3.4.1)

add_library(keys SHARED
            keys.c)

# Include libraries needed for native-lib lib
target_link_libraries(keys
                      android
                      log)
