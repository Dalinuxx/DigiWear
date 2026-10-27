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

# DigiWear Core Models & Wear Data (Serializable & Room Entities are kept via annotations)
-keep class com.digiwear.core.model.** { *; }
-dontwarn androidx.wear.**
# AmbientController reflectively loads androidx.wear.ambient.SharedLibraryVersion (Class.forName + getDeclaredMethod)
-keep class androidx.wear.ambient.** { *; }

# AndroidX Room
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Wear OS Tiles, Complications & WatchFace Services
-dontwarn androidx.wear.tiles.**
-dontwarn androidx.wear.protolayout.**
-dontwarn androidx.wear.watchface.**
-keep class com.digiwear.wear.tile.*Service { *; }
-keep class com.digiwear.wear.complication.*Service { *; }
-keep class com.digiwear.wear.watchface.*Service { *; }
-keep class com.digiwear.wear.datalayer.*Service { *; }

# WorkManager Workers
-dontwarn androidx.work.**
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

