# ProGuard rules for release builds

# Kotlin serialization — required for Ktor
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lamadb.android.**$$serializer { *; }
-keepclassmembers class com.lamadb.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.lamadb.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# SLF4J — Ktor transitive dependency, no Android binding needed
-dontwarn org.slf4j.impl.StaticLoggerBinder

# WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
