cmake_minimum_required(VERSION 3.22.1)

project("quantummessenger")

# 16 KB page size alignment for Android 15+ compliance
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# Tell CMake where the liboqs header files are
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/liboqs/include)

# Register liboqs.a as a pre-compiled library
add_library(liboqs STATIC IMPORTED)
set_target_properties(liboqs PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/liboqs/lib/${ANDROID_ABI}/liboqs.a
)

# Build the quantum_crypto library (formerly native-lib)
add_library(
    quantum_crypto
    SHARED
    native-lib.cpp
)

find_library(log-lib log)

# Link quantum_crypto against both liboqs and Android log
target_link_libraries(
    quantum_crypto
    liboqs
    ${log-lib}
)