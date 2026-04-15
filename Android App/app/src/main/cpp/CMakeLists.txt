cmake_minimum_required(VERSION 3.22.1)

project("quantummessenger")

add_library(
        native-lib       # name of the .so library that gets built
        SHARED           # shared = dynamically loaded at runtime
        native-lib.cpp   # your source file
)

find_library(
        log-lib          # variable name
        log              # Android's built-in logging library
)

target_link_libraries(
        native-lib
        ${log-lib}
)