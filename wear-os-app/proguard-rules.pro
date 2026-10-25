# DigiWear Wear OS ProGuard / R8 Rules

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

# DigiWear Core Models & Wear Data
-keep class com.digiwear.core.** { *; }
-keep class com.digiwear.wear.** { *; }
-keep class androidx.wear.** { *; }
-dontwarn androidx.wear.**

# AndroidX Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Wear OS Tiles & Complications
-keep class androidx.wear.tiles.** { *; }
-dontwarn androidx.wear.tiles.**
-keep class androidx.wear.protolayout.** { *; }
-dontwarn androidx.wear.protolayout.**
-keep class androidx.wear.watchface.** { *; }
-dontwarn androidx.wear.watchface.**
-keep class com.digiwear.wear.tile.** { *; }
-keep class com.digiwear.wear.complication.** { *; }
-keep class com.digiwear.wear.watchface.** { *; }

# WorkManager Workers
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Compose for Wear OS
-keep class androidx.wear.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
