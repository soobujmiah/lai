# JNI classes and callbacks are resolved by native names.
-keep class dev.lai.runtime.inference.NativeBindings { *; }
-keep interface dev.lai.runtime.inference.NativeTokenCallback { *; }
-keep class * implements dev.lai.runtime.inference.NativeTokenCallback { *; }
