# ==============================================================================
# SkillConnect R8 / ProGuard Obfuscation, Shrinking & Security Rules
# ==============================================================================

# 1. Obfuscation & Package Hierarchy Hardening
# Collapse obfuscated classes into a single short package namespace to reduce
# metadata leakage and reverse-engineering surface area.
-repackageclasses 'sc'
-allowaccessmodification

# Preserve line numbers for production crash triage while stripping real source file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,*Annotation*

# 2. Strip Debug, Verbose & Info Logging from Release Bytecode
# Prevents sensitive runtime state or diagnostic messages from leaking to logcat in production.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 3. Preserve Explicitly Annotated @Keep Classes & Members
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 4. Firestore Serialization Models (Reflection via DocumentSnapshot.toObject / Map)
# Keep only the data model POJOs/KClasses and their no-arg constructors, getters, and setters
# while allowing R8 to obfuscate repositories, ViewModels, UI screens, and security utilities.
-keep class com.example.data.model.** {
    <init>(...);
    public <methods>;
    *;
}
-keepclassmembers class com.example.data.model.** {
    <init>(...);
    <fields>;
    public <methods>;
}

# Keep Kotlin Enums values/valueOf required by reflection and Firestore mapping
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 5. Firebase Firestore, Auth & App Check (Play Integrity) Reflection Entrypoints
# Rely on consumer rules bundled in Firebase AARs instead of keeping all com.google.firebase.**,
# preserving only custom annotations, PropertyName mappings, and AppCheck registrar entrypoints.
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class * implements com.google.firebase.components.ComponentRegistrar
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.play.core.integrity.**

# 6. Jetpack Credential Manager & Google Identity Services
# Preserve service-loader and Binder entrypoints required for Google Sign-In
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# 7. Kotlin Coroutines & Compose Runtime
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
