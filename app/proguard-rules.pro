# ============================================
# NewsSpeech Auto - ProGuard Rules (FIXED)
# ============================================

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,Exception

# ============================================
# COMPOSE - FIX LOCK VERIFICATION ⚠️ CRITICAL
# ============================================

# KHÔNG được optimize Compose runtime (gây lock verification failed)
-keep,allowshrinking,allowobfuscation class androidx.compose.** { *; }
#-keep class androidx.compose.runtime.** { *; }
#-keep class androidx.compose.runtime.snapshots.** { *; }

# Đặc biệt giữ nguyên SnapshotStateList
-keepclassmembers class androidx.compose.runtime.snapshots.SnapshotStateList {
    *;
}

# TẮT optimization cho Compose
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }

# Disable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ============================================
# APP-SPECIFIC CLASSES
# ============================================

# Android Auto Service và Screen
-keep class com.newsspeech.auto.service.AutoSpeechService { *; }
-keep class com.newsspeech.auto.presentation.car.** { *; }

# NewsPlayer singleton (TTS)
-keep class com.newsspeech.auto.service.NewsPlayer {
    public <methods>;
    public <fields>;
}

# Data models cho GSON
-keep class com.newsspeech.auto.domain.model.** { *; }
-keepclassmembers class com.newsspeech.auto.domain.model.** {
    <fields>;
    <init>(...);
}

# Application class
-keep class com.newsspeech.auto.NewsApp { *; }

# ============================================
# GSON
# ============================================

-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================
# ANDROID AUTO
# ============================================

-keep class * extends androidx.car.app.Screen {
    public <init>(...);
}

# ============================================
# HILT DEPENDENCY INJECTION
# ============================================

-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# ============================================
# KOTLIN COROUTINES
# ============================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================
# STANDARD ANDROID CLASSES
# ============================================

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================
# R8 SETTINGS
# ============================================

-allowaccessmodification
-repackageclasses ''

# ⚠️ Bật khi release production
# -dontobfuscate

# ============================================
# REMOVE LOGGING (BẬT KHI RELEASE)
# ============================================

# Uncomment trước khi release:
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }