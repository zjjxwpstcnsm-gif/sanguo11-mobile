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
