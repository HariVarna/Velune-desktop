# Keep Velune Desktop entry point and serialization models
-keep class com.nikhil.yt.desktop.MainKt { *; }
-keep class com.nikhil.yt.desktop.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlinx Serialization
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.nikhil.yt.**$$serializer { *; }
-keepclassmembers class com.nikhil.yt.** {
    *** Companion;
}

# Ktor / OkHttp / Coroutines / NewPipeExtractor / Rhino
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.schabi.newpipe.**
-dontwarn org.mozilla.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.StringConcatFactory

-keep class org.schabi.newpipe.** { *; }
-keep class org.mozilla.** { *; }
-keep class io.ktor.** { *; }
-keep class com.kyant.m3color.** { *; }
