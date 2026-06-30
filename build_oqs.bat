@echo off
setlocal

set NDK=C:\Users\spnsh\AppData\Local\Android\Sdk\ndk\30.0.14904198
set TOOLCHAIN=%NDK%\build\cmake\android.toolchain.cmake
set SRC=c:\Users\spnsh\Desktop\Quantum Messenger\oqs_src
set TARGET=c:\Users\spnsh\Desktop\Quantum Messenger\Android App\app\src\main\cpp\liboqs

echo === Building liboqs for arm64-v8a ===
cd /d "%SRC%"
if exist build_arm64 rmdir /S /Q build_arm64
mkdir build_arm64
cd build_arm64
cmake -GNinja -DCMAKE_TOOLCHAIN_FILE="%TOOLCHAIN%" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 -DOQS_USE_OPENSSL=OFF -DOQS_BUILD_ONLY_LIB=ON -DOQS_DIST_BUILD=ON ..
ninja
if not exist "%TARGET%\lib\arm64-v8a" mkdir "%TARGET%\lib\arm64-v8a"
copy /Y lib\liboqs.a "%TARGET%\lib\arm64-v8a\"

echo === Building liboqs for armeabi-v7a ===
cd /d "%SRC%"
if exist build_arm32 rmdir /S /Q build_arm32
mkdir build_arm32
cd build_arm32
cmake -GNinja -DCMAKE_TOOLCHAIN_FILE="%TOOLCHAIN%" -DANDROID_ABI=armeabi-v7a -DANDROID_PLATFORM=android-26 -DOQS_USE_OPENSSL=OFF -DOQS_BUILD_ONLY_LIB=ON -DOQS_DIST_BUILD=ON ..
ninja
if not exist "%TARGET%\lib\armeabi-v7a" mkdir "%TARGET%\lib\armeabi-v7a"
copy /Y lib\liboqs.a "%TARGET%\lib\armeabi-v7a\"

echo === Copying fresh headers ===
xcopy /Y /S "%SRC%\build_arm64\include\oqs\*" "%TARGET%\include\oqs\"

echo === Done ===
