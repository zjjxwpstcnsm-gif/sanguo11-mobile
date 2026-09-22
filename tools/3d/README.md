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

## S03 reproducible architecture

Run `python3 tools/3d/build_sites.py` (Python 3 + Pillow) then use the same v1.56.0 matc:

```
matc -p mobile -a opengl -o app/src/main/assets/3d/sites/site.filamat tools/3d/site.mat
```

The script is the complete modeling source: boxes for structural timber/stone, open gate
passages, hip roofs, flared eaves, ridges, crenellations, halls, housing, corner towers,
pier piles and a small moored boat. It exports 15 valid GLB 2.0 files, three levels for
three city compositions, a port and a gate. Original geometry and atlas are CC0-1.0.
No sourced game art, external service, Blender, .blend, model compiler, or script enters APK.
One unit is one projected tile span. Origin is ground center; Y up; port +Z faces water.
City foundations are generated separately from the seven authoritative footprint cells.

SiteGlb is intentionally a restricted loader, not gltfio: one baked node/mesh/material,
TRIANGLES, float POSITION/COLOR_0/TEXCOORD_0, uint32 indices, embedded PNG, no sparse,
compression, skinning, animation or node transforms. The shared atlas shipped alongside
GLBs is used by the renderer; the embedded same PNG makes exports independently viewable.
Runtime material is unlit texture times baked directional colors with per-instance damage
multiplier. GLB viewers may use their own PBR lighting; do not confuse that with game lighting.
Assets have UVs; large walls stretch the small atlas, so final texture-density/art review is
still required. Near geometry is intentionally modest strategic-scale architecture.

`JSON_TEST_JAR=/path/to/json-20240303.jar bash scripts/test-3d-assets.sh` tests the exact loader
on host (org.json:json:20240303, reference implementation; Android provides its own org.json).
The test jar is not a runtime dependency. Counts, SHA256, bounds, materials, texture size and
LOD reports: docs/3d/site-assets.json. National IDs: docs/3d/site-mapping.tsv. Unknown IDs map
by type; malformed/missing individual GLBs fall back visibly to S01 geometry with diagnostics.
