# ============================================
# NewsSpeech Auto - ProGuard Rules
# ============================================

# ========================================
# COMPOSE - FIX LOCK VERIFICATION
# ========================================
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.snapshots.** { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.SnapshotStateList {
    *;
}

# Không tối ưu Compose snapshots (gây lock verification error)
-keep,allowoptimization class androidx.compose.runtime.snapshots.** { *; }
-dontwarn androidx.compose.runtime.snapshots.**

# ========================================
# KOTLIN COROUTINES
# ========================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ========================================
# HILT / DAGGER
# ========================================
-dontwarn com.google.errorprone.annotations.**
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ========================================
# ANDROID AUTO / CAR APP LIBRARY
# ========================================
-keep class androidx.car.app.** { *; }
-keep interface androidx.car.app.** { *; }
-keepclassmembers class * extends androidx.car.app.Screen {
    public <init>(...);
}
-keepclassmembers class * extends androidx.car.app.CarAppService {
    public <init>(...);
}

# ========================================
# TEXT-TO-SPEECH
# ========================================
-keep class android.speech.tts.** { *; }
-keep interface android.speech.tts.** { *; }

# ========================================
# DATA CLASSES (Gson)
# ========================================
-keep class com.newsspeech.auto.domain.model.** { *; }
-keepclassmembers class com.newsspeech.auto.domain.model.** {
    <fields>;
    <init>(...);
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ========================================
# GENERAL ANDROID
# ========================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep view constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========================================
# LOGGING (Xóa logs trong release build)
# ========================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}