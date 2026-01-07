# ============================================
# NewsSpeech Auto - ProGuard Rules
# ============================================

# Giữ lại số dòng (line numbers) để debug khi app bị crash
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Giữ lại các annotation quan trọng
-keepattributes *Annotation*,Signature,Exception

# ============================================
# COMPOSE RUNTIME - SỬA LỖI TREO APP (LOCK VERIFICATION)
# ============================================
# Đây là phần quan trọng nhất để fix lỗi lag 2.8s
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

-keep class androidx.compose.runtime.snapshots.** { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.** { *; }
-dontwarn androidx.compose.runtime.snapshots.**

-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.ui.**

-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# ============================================
# ANDROID AUTO
# ============================================
-keep class androidx.car.app.** { *; }
-keepclassmembers class androidx.car.app.** { *; }
-dontwarn androidx.car.app.**

# Giữ lại Service và Màn hình của xe
-keep class com.newsspeech.auto.presentation.car.** { *; }
-keep class com.newsspeech.auto.service.AutoSpeechService { *; }

# ============================================
# TEXT-TO-SPEECH (TTS)
# ============================================
-keep class android.speech.tts.** { *; }
-keepclassmembers class android.speech.tts.** { *; }
-dontwarn android.speech.tts.**

# Giữ lại Singleton NewsPlayer
-keep class com.newsspeech.auto.service.NewsPlayer { *; }
-keepclassmembers class com.newsspeech.auto.service.NewsPlayer { *; }

# ============================================
# GSON (QUAN TRỌNG CHO CRAWL DATA)
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Giữ lại các Model để Gson không bị lỗi parse JSON
-keep class com.newsspeech.auto.domain.model.** { *; }
-keepclassmembers class com.newsspeech.auto.domain.model.** {
    <fields>;
    <init>(...);
}

# ============================================
# KOTLIN & COROUTINES
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ============================================
# ANDROIDX & LIFECYCLE
# ============================================
-keep class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }
-dontwarn androidx.lifecycle.**

-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.MutableLiveData { *; }

# ============================================
# HILT / DAGGER (DEPENDENCY INJECTION)
# ============================================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

-keepclasseswithmembers class * {
    @dagger.* <methods>;
}

-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}

# ============================================
# FIREBASE
# ============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ============================================
# MEDIA SESSION
# ============================================
-keep class androidx.media.** { *; }
-dontwarn androidx.media.**

# ============================================
# DEBUGGING (ĐÃ TẮT ĐỂ HIỆN LOG KHI DEBUG)
# ============================================
# Lưu ý: Tôi đã comment phần này để bạn thấy log.
# Khi nào release thật sự lên store thì hãy bỏ comment.

# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }

# -assumenosideeffects class android.util.Log {
#     public static *** w(...) return;
#     public static *** e(...) return;
# }

# ============================================
# OPTIMIZATION
# ============================================
#-optimizationpasses 5
#-dontusemixedcaseclassnames
#-dontskipnonpubliclibraryclasses
#-verbose

# ============================================
# CUSTOM APP CLASSES
# ============================================
-keep public class com.newsspeech.auto.** { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ============================================
# PARCELABLE & SERIALIZABLE
# ============================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# ENUMS
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================
# R8 FULL MODE
# ============================================
-allowaccessmodification
-repackageclasses ''

