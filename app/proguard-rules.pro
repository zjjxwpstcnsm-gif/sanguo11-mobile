# Optimize executable bodies while retaining the development/public model ABI.
# Installed probes use private reflection; explicit save codecs use stable enum names.
# Do not remove or rename fields/classes/methods. No gameplay data or atlas is stripped.
-dontobfuscate
-keep,allowoptimization class game.sanguo.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
