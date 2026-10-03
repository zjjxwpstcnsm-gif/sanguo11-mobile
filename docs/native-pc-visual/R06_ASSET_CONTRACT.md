# R06 production asset contract

Normal path: MainActivity → MapHost → FilamentMapView → existing SiteGlb / FieldAssets.
Filament 1.56.0, OpenGL; no gltfio/general-purpose second loader, no Unity changes.
Main ac29b458 includes architecture PR66; serial predecessor6066003 is preserved.

## Inventory and supported subset

167 existing baked GLB2 models:15 site /152 field outputs. Reuse all mappings from
SiteVisual and FieldAssets. Environment terrain remains R04/R05. Original generator
scripts and rigid clips are the sources, under tools/3d; app/src/main/assets/3d is
committed runtime output. Catalog lists stable ID/version/source/hash/license/LOD,
pivot/bounds/units, atlas/color space/animation and production mapping for each GLB.
Historical asset manifests describe their original stage; asset-catalog.json is the
R06 complete current site/field inventory. These are stylized procedural assets;
soldiers, horses and many facilities remain coarse transitional art, not final PC art.

One scene/root, one baked node/mesh/opaque double-sided atlas material, TRIANGLES;
POSITION float VEC3, COLOR_0 float VEC4, TEXCOORD_0 float VEC2, optional unit NORMAL
float VEC3, uint32 indices, tightly packed float-aligned accessors. No transform,
hierarchy, external URI, sparse, skin, morph, glTF animation, extension payload,
alpha blend/mask or extra material semantics. Reject unsupported input; authors must
bake/convert it offline. This is deliberately not complete glTF support.

2MB/GLB,30k vertices/100k accessor elements,one primitive, PNG<=2048 per side and
<=1M pixels; finite positions within64 local units, UV/color0..1, normalized normals.
Embedded PNG signature/chunks/CRC and texture budget validated before GPU upload.
Offline gate verifies embedded PNG equals the separately shipped shared atlas.
Khronos Validator2.0.0-dev.3.10 checks glTF specification separately from SiteGlb's
actual project subset tests. No network URI resolver is installed.

Rigid animation version1 is GLB rest data + rigs.json contiguous vertex parts,
ordered parent indices/pivots + clips.json12 frames at12fps, bounded angle vectors;
formation1..8. No skinning/root motion; journal/clock drives existing UnitAnimation.

## Toolchain and reproduction

Python3.11+, Pillow12.3.0, numpy2.3.5, etcpak0.9.15, texture2ddecoder1.0.6;
Node22 / npm lockfile; matc official v1.56.0 `--version`56. No Blender is required.
`pip install -r tools/3d/texture-requirements.txt`
`npm ci --prefix tools/3d --ignore-scripts`
`python3 tools/3d/asset_pipeline.py` exports into temporary clean trees twice,
compares exact outputs, copies production files and generates catalog.
`python3 tools/3d/asset_pipeline.py --check` also compares committed outputs.
`npm run validate --prefix tools/3d` writes out/r06/khronos.json.
`JSON_TEST_JAR=/path/json-20240303.jar bash scripts/test-native-r06.sh`
CI also recompiles ground/water materials with matc56, unchanged from R05.
All modeling sources and exports are original project CC0-1.0; no KOEI game art.

## Runtime ownership

Site/field mesh and rigid pose decode uses one bounded CPU worker,64 outstanding
keys maximum,shared24MiB result LRU; FieldAssets shares24MiB immutable rest LRU.
Tree decode runs in existing terrain worker. No native API on either worker.
Owner polls completed jobs and uploads at most two asset meshes per frame;
terrain's existing separate two-upload budget is unchanged. This is a count cap,
not a measured2ms guarantee. Missing/invalid individual object asset is magenta
proxy with asset ID/error log; failed animation keeps the previous good pose.
GPU geometry uses shared buffers with explicit Proxy reference counts; only
unreferenced cache entries may be evicted. This is shared geometry, NOT instancing.
Atlases upload once per view and are shared by all instances; destruction follows
material instances and geometry. Immutable bundled rest data may survive same-view
world changes; decoded pose queue and all GPU objects are invalidated on session/
generation replacement. Close cancels CPU work and clears result ownership; stale
Future results have no route to GPU even if decoding ignores interruption.

Remaining implementation limits: small rig JSON/atlas texture setup still occurs
at renderer initialization on owner, not the geometry worker; texture streaming
and texture ref-count leases are not introduced. Global vegetation retention and
pose CPU double-cache budgets need device profiling. Same-world revisions can
reuse geometry safely because the cohort is immutable and contains no positions
from the authoritative world. There is no hot asset reload API.

## Actual R06 model changes

Gate LOD0: open gateway surround, inset lintel, pillars and second hipped roof.
Tree: opaque authored leaf clusters/veins atlas and branching; both tree families
use the actual shared field atlas, ETC2 mip chain regenerated.
Catapult: widened lever, basket and counterweight within the existing moving lever
part, including LOD1; chassis/axles remain unchanged. All three retain their normal
mapping and stable IDs; no debug-only model replacement.

## Open acceptance

R05 coast stair-step, reference mismatch, fixture UI compositor gap remain open.
Physical ARM64 Adreno/Mali, thermal/GPU budgets and full20-cycle matrix are NOT_RUN
until separately evidenced. Three sample assets are not nationwide final art.
Official docs used: https://github.com/KhronosGroup/glTF-Validator and
https://github.com/KhronosGroup/glTF/tree/main/specification/2.0 ;
https://github.com/google/filament/tree/v1.56.0 .
