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
# The source-ground probe verifies the compiled material, not just the .mat
# text. Its public ABI is invoked only by the separately installed test APK.
-keepclassmembers class com.google.android.filament.Material {
    public com.google.android.filament.Material$Shading getShading();
    public boolean hasParameter(java.lang.String);
}
-keep enum com.google.android.filament.Material$Shading { *; }
-keepclassmembers enum com.google.android.filament.Texture$InternalFormat {
    public static com.google.android.filament.Texture$InternalFormat RGBA8;
}
# The installed source-placement probe reads the actual GPU model matrix.
# These calls live in a separate APK; retain precisely their public signatures.
-keepclassmembers class com.google.android.filament.Engine {
    public com.google.android.filament.TransformManager getTransformManager();
    public com.google.android.filament.RenderableManager getRenderableManager();
}
-keepclassmembers class com.google.android.filament.TransformManager {
    public int getInstance(int);
    public float[] getTransform(int,float[]);
}
# Unity Player is opened through a class name after an opt-in export. Retain its
# Android/JNI entry points when the host's debug minifier is enabled.
-keep class com.unity3d.player.** { *; }

# Source unit installed verification inspects actual opaque/alpha primitives.
-keepclassmembers class com.google.android.filament.RenderableManager {
    public int getInstance(int);
    public int getPrimitiveCount(int);
}
-keepclassmembers class com.google.android.filament.Camera {
    public void setCustomProjection(double[],double,double);
}
