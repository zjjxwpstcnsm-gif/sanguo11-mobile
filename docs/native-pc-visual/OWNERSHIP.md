# R01 ownership and invalidation

The backend remains Filament 1.56.0, Engine.Backend.OPENGL. Engine, Surface,
Choreographer, resource upload/destruction, camera input and snapshot acceptance
remain on the Android main Looper. GameSession still owns the only authority.

SceneWorkQueue is the extracted production CPU submission boundary. It owns one
worker and at most one queued replacement and one completed result. Submit clears
queued requests and results; epoch invalidation rejects uncooperative old work.
No result Runnables carrying whole meshes are posted to the Android MessageQueue.
The normal doFrame drains results on the engine owner. No second rendering loop.
Close is idempotent, cancels waiting work and rejects later results. It does not
wait for a running CPU algorithm on the UI thread; that task releases captured
arrays on completion, without uploading. Host tests deliberately ignore interrupt.

Identity inputs:
- sessionId/worldGeneration: GameSession.state(), passed by MapHost.publish;
  replacement clears native entities, terrain/vegetation, playback, critical image,
  route, target and pending CPU result before submitting the new projection.
- authority revision: retained for diagnostics; ordinary command revisions need not
  rebuild unchanged terrain. They still synchronize copied scene objects.
- map content: Ground identity and its full matches check include terrain, topology,
  bases, mapSeed and layout; mapSeed includes map ID and map revision.
- visual input: Ground.surface.overrides plus Vegetation exclusions; rebuilding
  either increments the local mesh generation/queue epoch.
- assets: bundled S13 assets and Filament 1.56.0, immutable for this process/APK.
  There is no runtime asset hot reload; an asset revision change requires rebuilding
  the APK/recreating the view. Diagnostics name this fixed asset cohort.
- request generation: queue epoch and existing mesh generation; Surface output
  probes additionally check swap-chain identity and mesh generation.

Only full CPU mesh snapshots use latest-wins. TurnPlayback/TurnJournal remain
unchanged, ordered authority-derived events; replacing a session cancels active
replay. This queue is never used for commands, journal events or RNG.

Mesh upload uses newly allocated direct buffers with Filament's native buffer
ownership; application code does not pool/reuse them or recycle texture Bitmaps
after submission. CPU arrays are immutable inputs. Resource destruction is queued
on the owner and release flushes the engine before destruction. No GPU-memory
measurement is claimed. Geometry/texture byte counts are nominal estimates;
entity/material/mesh counts are application ownership counts, not driver probes.

GPU pose LRU continues to evict only shapes absent from live Proxy references.
CPU FieldAssets keeps its existing 24 MiB rest-mesh cache; cache eviction removes
its reference, not arrays still held by users. Chunk uploads keep the existing
per-frame two-item limit and visibility eviction. Whole-map CPU terrain caching
and synchronous asset/pose decode remain limitations, not falsely claimed fixed.

Release covers explicit 2D switch, detach, host exit and constructor failure.
Partial GpuMesh/Proxy and atlas failures now destroy acquired resources.
Pause removes Choreographer callbacks; Surface recreation only replaces swap chain.
Pause/resume does not cancel authoritative commands or save work. Java failures
return through MapHost fallback; native abort cannot be caught. The persisted
health marker prevents automatic restart after interruption. On API 30+ the latest
ApplicationExitInfo distinguishes native/Java crash, user stop and low memory;
API 26–29 explicitly reports unknown interruption. It does not guess that every
process death was a native crash. Manual retry remains available.
