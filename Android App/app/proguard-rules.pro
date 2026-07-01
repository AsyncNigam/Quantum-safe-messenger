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

# ──────────────────────────────────────────────────────────────────────────────
# 8. Retrofit + OkHttp (CRITICAL for release builds)
# ──────────────────────────────────────────────────────────────────────────────
# Keep Retrofit interface methods (they are resolved by reflection)
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# 9. Gson (used by Retrofit's GsonConverterFactory)
# Gson uses reflection to read/write fields — R8 will strip them without these rules
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep all DTO/model data classes used in API calls
-keep class com.nigdroid.quantummessenger.network.api.** { *; }
-keep class com.nigdroid.quantummessenger.network.model.** { *; }

# 10. Socket.IO Client
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class org.json.** { *; }

# 11. Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# 12. ML Kit Barcode
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# 13. Navigation Compose (type-safe routes use Serializable)
-keep class com.nigdroid.quantummessenger.presentation.navigation.** { *; }

# 14. AndroidX Biometric
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# 15. AndroidX Work (HiltWorker uses reflection)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.CoroutineWorker
-keep @androidx.hilt.work.HiltWorker class * { *; }

