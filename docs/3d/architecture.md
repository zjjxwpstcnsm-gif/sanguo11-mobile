# S01 renderer foundation

## Production entry and ownership

MainActivity -> MapHost -> either MapView (2D) or FilamentMapView (3D).
`视图 → 2D / 3D 试验模式` is available after loading an ordinary scenario/save.
MainActivity retains all command handlers; MapHost forwards taps to the same onTile.
TurnPlayback now targets MapHost and still consumes the one TurnWork visual world and cursor.
No core rules, saves, map coordinates, deployment footprint or AI were changed.

MapPresentation contains world/selection/camera operations. MapHost owns the additional
shared route, target, battle feedback, playback and lifecycle forwarding. MapView remains
usable directly by the existing scenario preview and map editor. The hidden 2D View is
removed from its parent during 3D, cancelling its camera and overview work. There is no
second continuously drawing map. Terrain/commander/bar/navigation controls and displacement
preview currently return to 2D to retain their complete semantics; S07 should implement
those in 3D, not invent replacement command validation.

## Coordinates and camera

GridWorldTransform delegates to the existing TileGeometry. One world unit = 40 old map
pixels; X = projectedX / DX, Z = projectedY / DY, positive Y is visual height. Ground is
Y=0 in S01. The old column-staggered storage-axis transpose and integer mapOffset are
preserved exactly. Six-neighbor topology is untouched. Rectangular staggered cells have
split collinear boundary edges; these are not a replacement square-neighbor rule.
Exact ties use floor(value + 0.5), including negative coordinates. VOID/padding rejects
picks; valid cells are passed unchanged to World.cityAt, preserving seven-cell cities.

SceneCamera is orthographic with fixed default elevation 55 degrees, no yaw. Camera
position = (focusX, 300 sin(tilt), focusZ + 300 cos(tilt)); lookAt = ground focus.
Screen X = width/2 + (X-focusX)*pixels; screen Y = height/2 +
((Z-focusZ)*sin(tilt) - Y*cos(tilt))*pixels. Its inverse is the parallel ray/ground-plane
intersection. Pan and pinch preserve the ground point under the gesture. Future terrain
height picking must replace the plane intersection consistently, with tests.

Switching transfers the ground focus and selected object without changing the World.
2D scale and 3D span are retained independently; switching during a turn is blocked with
an explanatory message, so no event cursor or command can be duplicated. Initial launch
and Activity recreation default to 2D. Orientation resizing keeps the 3D camera focus.

## Snapshot and resources

MapHost creates detached MapSceneSnapshot values only on world revision, selection,
turn, player or explicit visual invalidation. Ground contains terrain bytes and the
projection convention, no World. A world clone with identical terrain reuses Ground;
changed terrain/map load invalidates it. Dynamic objects use disjoint stable keys:
site:id, unit:id (including transports), domestic:id, structure:id. Existing renderables
are retained; position changes update transforms; owner/type changes replace the proxy.

A cancellable single CPU worker builds 16x16 storage-cell chunks. Each valid tile has
four vertices and six indices. GPU uploads happen on the engine owner thread, capped
at two visible chunks/frame. Offscreen chunks are excluded from Scene and distant GPU
chunks are released; CPU meshes remain cached until terrain replacement. Shared proxy
geometry is reused per type/color; this is resource sharing, NOT a claim of instancing.

All Filament calls run on the Android main Looper, including initialization, uploads,
rendering, resize and destruction. CPU generation never touches World or the engine.
Generation tokens reject obsolete worker results. SurfaceHolder owns the swapchain;
DisplayHelper records display timing; resize synchronizes pending native frames. Swapchain
completion callbacks control the loading label (submission is not presentation).
onPause cancels Choreographer callbacks, onResume restores a single loop, and release
cancels workers and destroys entities/buffers/materials/camera/view/scene/renderer/engine.
Native-session health is committed before library loading and cleared only after normal
release. A previous interrupted session starts in 2D with a manual-retry message. Java
exceptions/LinkageError fall back immediately; native process crashes are NOT caught by
Java. OS process death can also cause this conservative startup message.

## Toolchain and asset boundary

Pinned runtime and matc: Filament **1.56.0**, OpenGL ES backend. Java source/target 17,
AGP 8.9.2, Gradle 8.11.1, compile/target SDK 35, min SDK 26 remain unchanged. AndroidX is
enabled for the library annotations; no UI migration/Jetifier/runtime shader compiler,
gltfio or utils bundle. Material is offline compiled for mobile/OpenGL and loaded once.
The tested latest 1.77.0 was rejected because its AAR requires compileSdk 37 and Java 21.
Do not silently upgrade runtime without recompiling matching materials and rechecking API/ABI.

Runtime has arm64-v8a, armeabi-v7a, x86 and x86_64 JNI. arm64 ELF LOAD alignment inspected:
0x4000 (16 KiB). APK alignment/signature checks are recorded with build evidence.
Directional light is provisioned, but S01 terrain and proxy shader is intentionally
unlit vertex color with baked face shading. It is not physically based finished art.

## Remaining boundaries

S02 reuses Ground, GridWorldTransform, SceneCamera and the chunk lifecycle for terrain/water.
S03 replaces explicit temporary city/pier/gate silhouettes. S05 expands army models and
movement; S06 adds full critical/battle effects; S07 brings the remaining 2D overlays and
editors into 3D. Existing editors remain 2D and edited maps load through the normal World.
Diagnostic overlay is off by default; view menu toggles it and writes MapRenderer logs.
It reports Surface dimensions, visible/GPU chunks, objects, pending uploads and callback
interval, NOT GPU timing or presented FPS. Physical-device performance and resource leak
profiling remain required before defaulting to 3D.


## S02 update (supersedes S01 flat terrain paragraphs)

TerrainSurface is a CPU-only visual field; Ground snapshots site footprints as well as
terrain. Sample coordinates come exclusively from GridWorldTransform. Surface is bounded
to 0..2.6 world units, deterministic, and constrained to zero throughout water/site cells.
Road and pass corridors have bounded low relief. No world mutators or gameplay RNG are used.

SceneMesh uses eight split boundary vertices, with a center fan and interior subdivisions.
Far geometry uses eight triangles/cell; close geometry uses 24 coplanar triangles/cell for
additional color detail. Thus LOD switches cannot open boundary cracks or move pick surfaces.
Normals are baked directional shading from shared-field derivatives; existing unlit Filament
material is retained. There are no UV textures or realtime normal lighting claims.

Six-cell chunk dependency halos include terrain, invalid mask, bases and projection. Patch
updates reuse unchanged mesh identities, retaining matching GPU resources. Sample caching is
scoped to the immutable Ground; initial generation still prepares both CPU LODs nationwide.

Ray picking intersects the same piecewise-linear fan via bounded top-down ray marching and
bisection. Proxy transforms, labels and polygon outlines use the same field. No nationwide
triangle traversal occurs on a tap. Native/Surface ownership and 2D recovery are unchanged.

## S03 architecture

SiteVisual snapshots model family, terrain-informed variant, scale, water/gate-derived yaw,
logical footprint and quantized durability without changing World. Existing item stable IDs
still control removal/replacement. SiteGlb validates bounded static GLB buffers before GPU
upload. CPU asset geometry/UV and GPU buffers are shared per family/LOD; texture is shared
across all objects. Each site has an independent material instance for damage. Ownership
changes replace the small flag geometry; obsolete shapes/material instances are destroyed.
World replacement, load, custom site type changes and deletion reuse the same sync path.

GPU sharing is NOT instancing: each visible city has model + flag + footprint draws; port
and gate have model + flag. Modular submeshes are baked into a single triangle primitive
per exported model. Runtime draw-call measurement is unavailable; this is the submission
structure, not a measured driver count. LOD hysteresis spans 15/19 and 42/48; LOD uses the
same logical anchor and the same independent seven-cell foundation. There is no new collider.
Core ground picking is retained; selected seven-cell outlines and unit ground rings overlay
the buildings so movement/entry semantics remain legible. Opaque textures avoid alpha sorting.

Long press turns the restricted camera 180 degrees. Projection, panning, zoom anchoring,
terrain rays, overlays, labels and saved direction use the same facing sign as lookAt.
Directions are CPU-tested; actual native screenshots remain pending. View remains opt-in.

## S05 playback contract repair

UnitVisual captures actual core equipment and HUD values; UnitMotion belongs to each
Filament Proxy and only consumes TurnJournal paths/fractions. No root motion feeds core.
Moving hit tests return the snapshot cell of the same stable unit; the existing TileListener
keeps command validation authoritative. Ground, labels and culling use the same pose.
Only the current actor's GPU transform is animated in view; clear/replacement settles the
previous actor. S01 unit silhouettes remain temporary; full asset/clip/formation coverage
is explicitly tracked in acceptance/s05-status.md.


## S04/S05 actual field asset pipeline

`tools/3d/build_field_assets.py` emits original UV-mapped GLBs, opaque atlas,
rig part ranges/pivots and twelve-frame local rotation tracks. Runtime assets and
checksums/bounds/triangle budgets are in `field-assets.json`. `FieldAssets` loads
rest data once; `UnitAnimation` chooses a clip and per-instance phase using the
shared TurnJournal cursor. `UnitMotion` retains authoritative versus visual position.
No root motion, terrain slope cost, command completion callback or model-driven RNG.

Articulation is CPU rigid-part posing, not glTF skeletal animation. Infantry limbs,
horse legs, individual vehicle wheel axles, siege levers/ram and ship oars have actual
vertex motion. Soldiers merge into one formation mesh (8/4/1 infantry, 4/2/1 cavalry).
Posed GPU buffers share a bounded 128-entry LRU excluding live references; each proxy
retains its entity and material. The bound may exceed 128 for distinct live shapes.
Only visible proxies schedule pose changes. Each formation plus flag still contributes
two renderable primitives; shared files do not constitute GPU instancing.

Vegetation merges opaque trees by 8x8 source tile chunk. Stable cell hash and visual
seed control placement; true facilities, site footprints, water, boundary and adjacent
road corridors exclude trees. Two mesh LODs give way to terrain color at far distance.
Snapshot changes fingerprint bounded dependency halos and retain unchanged CPU/GPU
chunks. Camera LOD/culling never rebuilds or scans all terrain cells. Tree/formation
shadows are disabled; there are no transparent leaf billboards or per-tree entities.

Facility selection and ranges still use core coverage. Actual type/level selects three
GLB LODs; construction/fire modules and HP tint reflect live snapshots. Destroyed
facilities release renderables before their shared buffers become eligible for eviction.

## S06 combat presentation

`TurnJournal.StateChange` exposes only detached scalar before/after values;
`Strike` records the existing physical hits in execution order without new business
events. `CombatVisual` is an Android-free bounded sampler. Filament owns 48 reusable
entities and six shared opaque geometry buffers; the existing overlay draws exact
impact values and a compact portrait card. Historical snapshots are not authoritative
saves. See `acceptance/s06-combat.md` for timeline, event matrix, budgets and open gates.


## S08 quality and resource ownership

SceneQuality owns the only vsync deadline policy: LOW/MEDIUM 30fps, HIGH 60fps.
SurfaceHolder fixed-size buffers supply 70/85/100% internal resolution while the
camera and native UI retain view-pixel coordinates. Android29+ severe thermal state
caps 30fps/70%; recovery waits for light/none. HIGH MSAA requires GLES3.1+ because
Filament1.56 blitLow compilation aborts on the GLES3.0 compatibility driver.
CPU rest meshes use a 24MiB LRU budget; GPU pose caches trim only unreferenced meshes.
Retired views clear CPU geometry/snapshot references immediately after canceling work.
Opaque atlases use ETC2_SRGB8 compressed mip uploads, with versioned offline generation
and a separate texture-compression manifest. CPU upload time and geometry/texture byte
estimates are never labeled as measured GPU duration or process memory.
