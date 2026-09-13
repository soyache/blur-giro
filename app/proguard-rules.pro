# Shizuku UserService and AIDL must stay constructible by name.
-keep class com.soyache.blurgiro.bridge.CaptureBridge { *; }
-keep class com.soyache.blurgiro.bridge.ICrystalBridge { *; }
-keep class com.soyache.blurgiro.bridge.ICrystalBridge$* { *; }
-keep class com.soyache.blurgiro.bridge.ICaptureListener { *; }
-keep class com.soyache.blurgiro.bridge.ICaptureListener$* { *; }
-keep class com.soyache.blurgiro.CrystalService { *; }
