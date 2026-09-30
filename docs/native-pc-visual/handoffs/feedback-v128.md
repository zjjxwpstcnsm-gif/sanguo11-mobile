# v128 refined landscape continuation — PARTIAL, validation in progress

This is the explicitly requested landscape refinement after R00–R18, not a new automatically started roadmap stage. Input PR67 remains draft/unmerged at beb9ae3584b36dd8689ae5aa3b242274b4104bf2; remote main remains ac29b458325b52d6e302ca44270d16552de4ed7f with architecture PR66 merged.

## Production changes

Twenty actual Blender4.3.2 exported near/far GLBs refine Hukou/southwest/narrow waterfall families, rammed-earth wall/beacon, sandstone/granite/karst cliffs and low Taihu shore reeds/rocks. Existing 476 assets are byte-identical. The new opaque512x64 atlas retains panels1/2/6/7, changes mineral/earth/water/foam panels0/3/4/5 and uses the existing pinned Filament1.56 scenery material. Full ETC2 mip chain, original scene source/script and exact hashes are retained.

Taihu dressing uses only the existing non-navigable lake west of Wu, source171..179/103..111. The normal View → Landscape landmarks menu adds the lake. This is an interpretation of the recovered project map, not a verified exact PC label/anchor. No new lake/island, terrain-height, pathing, city, save or rule change. Full face footprint guards keep shore dressing in original blocked water away from ports, paths and buildings. Custom maps never inherit the national decorations.

The v127 waterfall fitting was independently found to tear equal source corners because the GLB exporter expands triangle corners and the correction used independent indices. Original input fails the new regression at Taishan LOD0 with0.008022726world-unit seam. Candidate welds only the terrain correction across equal positions. It does not change the canonical height field.

Normal landmark entry now sets its actual target/span before first CPU publication, waits for real layout dimensions and retains the latest target if a second selection arrives before layout. Camera-dependent CPU requests drain/queue before beginFrame, while all GPU work remains behind successful admission. One worker/one in-flight job/latest immutable results are preserved.

## Evidence so far

- Actual v127 input host suite and v128 affected suites executed locally with full Java17, including complete SaveCodec/RNG comparison. Candidate affected checks PASS; exact totals/logs will be finalized with CI.
- New duplicated-corner test: input FAIL, candidate PASS4536 comparisons.
- Frame control-flow harness extracts actual production methods; PASS admission,43 rejected frames, one-in-flight window scheduling, deferred/latest focus. Five negative mutations independently fail. This is host control flow, not GPU throughput.
- Architecture boundary/GameSession/BridgeSession/projection119600/GridLayout32 PASS.
- Khronos355GLBs PASS0errors/0warnings.
- CoreTest.logistics:75 and DisplacementTest.boundaries:27 independently reproduce same FAIL on input and candidate. No assertion or rule was changed.
- Exact input CI APK independently downloaded: aa8d5e6841502cba330d31c3d29f53275dcee33abc6a3511ef1a99bfd2f35054, sourcebeb9ae3. Not delivered as this candidate.

## Honest limits

Offline matched-camera Blender previews show improved granite/earth-wall silhouette and material; waterfall shape/impact still under art review. They are expressly not APK screenshots. The pre-existing southwest/API29 evidence demonstrates742begin attempts/738rejections/4submissions with CPU work completed; the cause of the underlying GPU/driver delay is not established. Scheduling fixes are not declared a cure until installed tests pass.

Candidate APK build/lint/install, six regions on API29/35, normal-menu gestures, actual Surface/UI screenshots, complete authority comparison and exact APK identity are pending current-source CI. PC same-camera reference remains REFERENCE_MISSING; physical ARM64, Adreno/Mali and30-minute performance remain NOT_RUN. No main merge or next phase.
