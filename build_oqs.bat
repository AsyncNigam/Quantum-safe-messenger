git clone https://github.com/open-quantum-safe/liboqs.git --branch main --depth 1 oqs_src
cd oqs_src
mkdir build
cd build
cmake -GNinja -DCMAKE_TOOLCHAIN_FILE="C:\Users\spnsh\AppData\Local\Android\Sdk\ndk\30.0.14904198\build\cmake\android.toolchain.cmake" -DANDROID_ABI=armeabi-v7a -DANDROID_PLATFORM=android-26 -DOQS_USE_OPENSSL=OFF -DOQS_BUILD_ONLY_LIB=ON -DOQS_DIST_BUILD=ON ..
ninja
mkdir "c:\Users\spnsh\Desktop\Quantum Messenger\Android App\app\src\main\cpp\liboqs\lib\armeabi-v7a"
copy lib\liboqs.a "c:\Users\spnsh\Desktop\Quantum Messenger\Android App\app\src\main\cpp\liboqs\lib\armeabi-v7a\"
