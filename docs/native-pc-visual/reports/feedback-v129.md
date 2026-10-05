# v129 combined terrain refinement — PARTIAL

This continues PR67 after the explicit 2026-09-30 user requests. Production source is the commit containing this report; the exact build embeds GITHUB_SHA. No APK for this combined source has been verified yet. v128 is historical evidence, not a substitute for v129.

## Production changes
- Twenty actual Blender 4.3.2 near/far GLBs now route through FieldAssets/Vegetation v129: eroded rock faces, crevices, curved reeds, richer water folds and terrain-following earth wall/beacons. Full-attribute indexing preserves normals/UV seams. Near falls have 2,332 triangles, cliffs 1,462–1,576, wall 1,248, beacon 1,506, reeds 1,648, shore stone 880. Far LODs remain 264–650 triangles.
- Actual v128 Taishan Surface showed pale rock supports. Pinned Filament's implicit vertical UV flip was independently reproduced as the cause: versioned scenery material now disables it for authored panels while preserving the previous orientation of legacy panels. Host texture contracts pass; actual new APK confirmation remains pending.
- Versioned ground materials give POISON distinct irregular blue-green mineral pools, dark rims and restrained sulfur edges. TerrainMaterialField carries the existing POISON classification, without new geometry, textures, draw calls or terrain/map changes. Backdrop excludes this fine hazard. The admitted-frame material update follows Ground identity/layout.
- 2D/3D grids use current-force difficult-march eligibility; terrain projection, technology/player/preview switches and grid caching are covered. SHALLOWS, MOUNTAIN_PATH and PLANK_ROAD hide grid before research, show after. Permanently blocked mountains remain hidden.
- Explicit additional user authorization at 12:20 UTC permits the narrow PLANK_ROAD movement change: unresearched units cannot enter. Central Fieldworks, pathfinding, campaign movement, transport, AI, previews and displacement use this gate. Post-research damage and skill immunity remain unchanged. Old saves need no migration; existing occupants can leave but not enter another unresearched plank tile.
- Normal View / Landscape Landmarks menu adds 西南毒泉 at an existing authoritative POISON tile (17,165). It does not alter terrain or create a special rendering path.

## Verification so far
PASS: focused difficult-march 131 core / 75,162 rendering/session assertions; poison 1,494,147; architecture; native R03/R04/R05 and S11 water; frame-admission rejection tests; high-detail landmark/surface-seam host tests. Exact counts and commands are in the focused reports and CI logs. Final tiny menu/instrumentation changes are rechecked before publication.

Eleven legacy rule first-assert failures were independently reproduced on input and candidate in docs/validation/grid129. They remain failures; they do not justify a blanket inherited classification of rendering failures.

The user approved debuggerd on disposable Actions emulators. Paired v127/v128 native stacks in run36714124998 repeatedly show Filament FEngine inside emulator GL program-link / uniform-location / buffer / EGL-sync calls, waiting in QemuPipeStream while CPU mesh workers are idle. This narrows sampled stalls to emulator graphics transport; it does not prove physical-device performance or establish the ultimate host-side cause. No ready timeout, frame-admission or image-validity gate was relaxed.

The v129 workflow builds the exact source, reproduces Blender/ETC2/material bytes, checks GLBs, lint/ABI/APK identity and runs ordinary SwANGLE API29/35 landmark and technology-grid scenarios. A bounded API35 candidate/input lavapipe pair is diagnostic only; capability/shadow differences must be checked before equivalence claims. The app remains Filament1.56 OPENGL.

## Measured cost and visual limits
The twenty decoded arrays rise from 957,600 to 2,073,648 bytes. Observed host dense karst span10 near/far buffers rise from 4.76/3.12 MB to 9.27/4.62 MB; near has 107,147 triangles. Host median construction 23.76→50.96 ms, parser 2.462→2.996 ms for twenty assets. These are host costs, not Android FPS. Do not call extra faces free.

Original PC San11 reference from KOEI was inspected for rock breakup and scale only: region/camera are unknown. Same-region/same-camera comparison remains REFERENCE_MISSING. All Blender and poison diagnostic images are HOST/OFFLINE ONLY, not APK captures. No commercial reference image is redistributed. Final at-scale terrain composition and visual acceptance remain open.

APK build/install, final runtime screenshots, ARM64/30-minute performance and historical full-touch/SAF/lifecycle acceptance are NOT_RUN for v129 at this source checkpoint. Overall PARTIAL. Do not merge main or start another phase.
