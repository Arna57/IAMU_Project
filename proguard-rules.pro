# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==============================================================================
# Retrofit 2.11 - Keep Retrofit interfaces and annotations
# ==============================================================================
-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ==============================================================================
# Gson - Keep TypeToken and fields with @SerializedName
# ==============================================================================
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# ==============================================================================
# OkHttp and OkIo - Suppress warnings for internal APIs
# ==============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==============================================================================
# App JSON model packages - network DTOs and domain models
# ==============================================================================
-keep class hr.alg.iamu_project_bp.data.network.** { *; }
-keepclassmembers class hr.alg.iamu_project_bp.domain.model.** { *; }