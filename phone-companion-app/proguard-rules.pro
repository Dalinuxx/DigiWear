# DigiWear Phone Companion ProGuard / R8 Rules

# Kotlin & Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontnote kotlinx.serialization.SerializationKt
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable class *;
}
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers enum * {
    kotlinx.serialization.KSerializer serializer(...);
}

# DigiWear Core Models & Companion Data (Serializable & Room Entities are kept via annotations)
-keep class com.digiwear.core.model.** { *; }
-keep class com.digiwear.companion.data.*Entity { *; }

# AndroidX Room
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Cryptography, BouncyCastle, Conscrypt & Wireless ADB JNI/Reflection Entrypoints
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.conscrypt.OpenSSLProvider { *; }
-keep class org.conscrypt.NativeCrypto { *; }
-keepclassmembers class org.conscrypt.** {
    native <methods>;
}
-dontwarn org.conscrypt.**
-keep class io.github.muntashirakon.adb.** { *; }
-dontwarn io.github.muntashirakon.adb.**
-keep class dadb.** { *; }
-dontwarn dadb.**

# MLKit Barcode Scanning & Google Play Services Vision JNI Entrypoints
-keep class com.google.mlkit.common.internal.CommonComponentRegistrar { *; }
-keep class com.google.mlkit.vision.**Registrar { *; }
-keepclassmembers class com.google.mlkit.** {
    native <methods>;
}
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.vision.**

