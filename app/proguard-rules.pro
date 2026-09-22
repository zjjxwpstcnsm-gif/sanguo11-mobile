# Optimize executable bodies while retaining the development/public model ABI.
# Installed probes use private reflection; explicit save codecs use stable enum names.
# Do not remove or rename fields/classes/methods. No gameplay data or atlas is stripped.
-dontobfuscate
-keep,allowoptimization class game.sanguo.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
# Installed texture-format verification invokes this API from the separate test APK.
# Keep its binary signature even when normal rendering does not query it.
-keepclassmembers class com.google.android.filament.Texture {
    public com.google.android.filament.Texture$InternalFormat getFormat();
}
