# 1. JNI & Native Bridge Hardening
# Prevents R8 from scrambling native method names which would cause UnsatisfiedLinkError
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. Google Tink (Cryptographic Engine)
# Tink uses reflection and dynamic class loading for its primitives
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# 3. Protocol Buffers (Serialization)
# Protobuf classes are accessed via reflection; keep all generated data classes
-keep class com.nigdroid.quantummessenger.proto.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# 4. Hilt / Dagger (Dependency Injection)
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# 5. Room Persistence
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# 6. SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# 7. Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, InnerClasses
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepnames @kotlinx.serialization.Serializable class *
