# R8 / ProGuard rules for Lupa Free (release build)

# ---------- CameraX 1.6.x ----------
# CameraX uses ServiceLoader to discover CameraXConfig.Provider implementations
# and reflective access to Camera2 internals. Keep these intact.
-keep class androidx.camera.core.** { *; }
-keep interface androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep interface androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }

# Keep all Camera2 implementation classes that R8 might otherwise strip
-keep class androidx.camera.camera2.internal.** { *; }
-keep class androidx.camera.camera2.config.** { *; }

# Suppress warnings for CameraX optional dependencies
-dontwarn androidx.camera.**
-dontwarn com.google.auto.value.**
-dontwarn javax.annotation.**

# ---------- Kotlin coroutines ----------
# Coroutines internals are accessed reflectively by some debug tools
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---------- AndroidX Lifecycle ----------
# ViewModel constructors are called via reflection
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# ---------- Standard Android ----------
# Keep enums (needed for Parcelable, some library APIs)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable CREATOR fields
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep our Application entry point
-keep class com.example.lupafree.MainActivity { *; }

# ---------- Compose ----------
# Compose ships its own consumer ProGuard rules; no additional rules needed.
# The following is just a safety net for @Composable annotations on lambdas.
-keepclassmembers,allowobfuscation class * {
    @androidx.compose.runtime.Composable <methods>;
}
