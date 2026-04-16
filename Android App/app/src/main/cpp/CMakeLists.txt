cmake_minimum_required(VERSION 3.22.1)

project("quantummessenger")

# Tell CMake where the liboqs header files are
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/liboqs/include)

# Register liboqs.a as a pre-compiled library
add_library(liboqs STATIC IMPORTED)
set_target_properties(liboqs PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/liboqs/lib/${ANDROID_ABI}/liboqs.a
)

# Build your native-lib as before
add_library(
    native-lib
    SHARED
    native-lib.cpp
)

find_library(log-lib log)

# Link native-lib against both liboqs and Android log
target_link_libraries(
    native-lib
    liboqs
    ${log-lib}
)