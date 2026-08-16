# JNI entry points are resolved by their Java names.
-keep class dev.lai.runtime.inference.NativeBindings { *; }

# Shizuku provider, binder APIs, and remotely instantiated UserService.
-keep class rikka.shizuku.** { *; }
-keep class dev.lai.runtime.shell.PrivilegedUserService { public <init>(...); *; }
-keep interface dev.lai.runtime.shell.IPrivilegedService { *; }
-dontwarn rikka.shizuku.**

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
