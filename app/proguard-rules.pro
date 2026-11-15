# KOCHWeather R8 rules for the release-like benchmark build.
# Compose, OkHttp, Coil and DataStore ship their own consumer rules; below are
# the app-specific reflection/serialization touch points.

-keepattributes *Annotation*,InnerClasses,Signature,Exceptions,AnnotationDefault

# App entry point.
-keep class com.koch.weather.MainActivity { *; }

# kotlinx.serialization: generated serializers and companions must survive R8.
-keep,includedescriptorclasses class com.koch.weather.**$$serializer { *; }
-keepclassmembers class com.koch.weather.** {
    *** Companion;
}
-keepclasseswithmembers class com.koch.weather.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.koch.weather.data.model.** { *; }
-keep class kotlinx.serialization.** { *; }

# Enum names are persisted in DataStore (current_theme, source ids, symbols).
-keepclassmembers enum com.koch.weather.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# osmdroid uses reflection for tile providers/cache and is referenced by XML.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-dontwarn org.osm.**

# Keep line numbers useful in crash reports without bloating the profile.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
