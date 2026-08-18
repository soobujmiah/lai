# Release diagnostics: keep source file names and line numbers in stack traces so crashes can be
# de-obfuscated with the mapping.txt produced by R8 (see docs/LOGGING.md). Minification stays on.
-keepattributes SourceFile,LineNumberTable

# Centralized diagnostic logger and redactor are referenced from app code, but keep them
# explicitly so future R8 updates can never strip the logging call sites or redaction.
-keep class dev.lai.runtime.core.LaiLog { *; }
-keep class dev.lai.runtime.core.LaiLogRedactor { *; }

# JNI entry points and callback methods are resolved by their Java names.
-keep class dev.lai.runtime.inference.NativeBindings { *; }
-keep interface dev.lai.runtime.inference.NativeTokenCallback { *; }
-keep class * implements dev.lai.runtime.inference.NativeTokenCallback { *; }

# Shizuku provider, binder APIs, and remotely instantiated UserService.
-keep class rikka.shizuku.** { *; }
-keep class dev.lai.runtime.shell.PrivilegedUserService { public <init>(...); *; }
-keep interface dev.lai.runtime.shell.IPrivilegedService { *; }
-dontwarn rikka.shizuku.**

# androidx.work + Room — WorkDatabase_Impl is instantiated via reflection; R8 was stripping its <init>.
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class * extends androidx.work.impl.WorkDatabase { <init>(...); *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(...); *; }
-dontwarn androidx.work.**
-dontwarn androidx.room.**

# kotlinx.serialization generated serializers.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers,allowoptimization,includedescriptorclasses class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
