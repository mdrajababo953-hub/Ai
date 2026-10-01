# Keep models and entities for Moshi & Room
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.audio.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonClass <fields>;
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
