# Rebuild the S01 material

Use `matc` from the exact **v1.56.0** official Filament release:
https://github.com/google/filament/releases/tag/v1.56.0

```
matc -p mobile -a opengl -o app/src/main/assets/3d/terrain.filamat tools/3d/terrain.mat
```

No runtime shader compiler is shipped. Terrain and the six proxy silhouettes are generated
by SceneMesh from real snapshot values. They are original project transitional geometry,
not final art, imported models, textured UV assets, or a tilted national screenshot.
Filament's release license is distributed in assets/3d/filament-LICENSE.txt.

Validation: `bash scripts/test-3d-foundation.sh`; `./gradlew :app:assembleDebug
:app:assembleDebugAndroidTest :app:lintDebug`; then run SceneInstrumentation on an installed
OpenGL ES 3 emulator/device. MainActivity's normal view menu provides manual 3D access.
