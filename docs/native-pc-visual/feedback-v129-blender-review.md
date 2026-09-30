# v129 original Blender landmark refinement

Status: PARTIAL overall. Source generation/format/budget verification PASS; combined-source APK validation, same-view PC acceptance, and ARM64 performance are separate and are not claimed here.

## What actually changed

Twenty new GLBs in `app/src/main/assets/3d/field/v129/`, ten families × two LODs, created by Blender 4.3.2. v128 and all 392 preexisting files in this isolated clone remain byte-identical. The final integrated host check additionally preserves all 476 input asset files. No production Java, gameplay, map, coordinates, RNG, shaders or routing changed in this clone.

- Waterfalls: 18-column coherent folded curtain with authored lip/foot rows; nine-band battered cliff support with deep irregular side recesses; broad rooted feet, six unequal splinters/boulders and five shallow foam volumes
- Granite: continuous 24×16 unequal ridge surface with crossing recessed fractures, four battered perimeter bands and detached talus
- Limestone: three unequal towers with twelve flute bands, alternating smooth/hard crease walls, uneven crowns and grounded foot stones
- Sandstone: forty-point irregular plan, fourteen eroded courses, undercut ledges and actual vertical fissures
- Earth wall: 26 longitudinal samples, nine compressed-earth bands, rain gullies, chipped crest and spalls
- Beacon: 28-sided squared battered body, twelve eroded courses, recessed hearth, unequal broken rim and fuel bed
- Shoreline: nineteen bent tapered reeds, thirty-eight ridged curled leaves and five seed heads; five rounded jointed shoreline stones

No subdivision modifier is used. A BVH comparison rejects pure surface-preserving subdivision for every family: 28%–98% of new unique positions lie over 0.3% of the bounding diagonal away from the old surface. This verifies changed shape, not artistic fidelity.

## Reasoned geometry budgets

The user's request for materially finer actual geometry supersedes the old 750-triangle starting cap. Main LOD0 models remain 1,248–2,332 triangles; low shoreline stones use 880. LOD1 stays 264–650 triangles and below 45% of its near model. Full-attribute float32-exact indexing preserves hard-normal, color and UV seams rather than welding by position. Actual triangle counts come from index counts, not vertex count assumptions.

| Family | v128 → v129 near triangles | Far triangles | Near exported vertices | Near GLB bytes | Near array bytes |
|---|---:|---:|---:|---:|---:|
| beacon-han | 286 → 1506 | 378 | 2807 | 188,024 | 164,036 |
| cliff-granite | 233 → 1576 | 352 | 2849 | 190,884 | 167,060 |
| cliff-karst | 308 → 1462 | 328 | 2337 | 164,940 | 139,068 |
| cliff-sandstone | 198 → 1488 | 324 | 3985 | 244,360 | 225,076 |
| fall-hukou | 642 → 2332 | 650 | 4082 | 259,140 | 240,248 |
| fall-narrow | 642 → 2332 | 650 | 4084 | 259,236 | 240,352 |
| fall-wide | 642 → 2332 | 650 | 4082 | 259,140 | 240,248 |
| shore-reeds | 406 → 1648 | 336 | 1191 | 112,164 | 81,708 |
| shore-rock | 240 → 880 | 280 | 485 | 69,048 | 35,780 |
| wall-earth | 564 → 1248 | 264 | 1272 | 111,220 | 81,120 |

Whole 20-model totals: 5,700 → 21,016 triangles; 1,593,064 → 2,637,768 GLB bytes; 957,600 → 2,073,648 retained mesh-array bytes. Shared atlas remains the exact 33,886-byte v128 PNG, 512×64 sRGB. No new texture blob or atlas orientation changes. Authored row convention remains PNG row0 = UV.v0; integrated material fix uses explicit flipUV:false plus legacy panel handling.

## Actual host measurements

`Landmark129Benchmark.java` uses the real project `SiteGlb.read` and unchanged baseline `Vegetation.buildWindow`. Its Asset Source remaps only the ten landmark GLB families; no rules/map alteration. Java17 ThreadMXBean records actual current-thread CPU and allocated bytes, excluding file I/O from parser timing. Parser results use seven measured rounds after three warmups, ten repetitions of twenty models per round; window results use three measured rounds after one warmup with asset/terrain caches warm but mesh lists empty.

Parser median per twenty models: 2.462 → 2.996 ms CPU, 8,905,464 → 13,353,528 allocated bytes. Host-only values are not Android/GPU timings. Timings are environment-sensitive and not guaranteed speed claims.

Real span10 windows include full scenery and the existing streaming halo. First coordinate is the actual Taishan anchor; fourth is a northern granite-region sample.

| Source X,Y | Chunks | Near triangles old → new | Near array bytes old → new | Far array bytes old → new | Host CPU ms old → new |
|---|---:|---:|---:|---:|---:|
| 147,59 | 49 | 70,579 → 87,042 | 3,741,528 → 5,281,100 | 3,213,808 → 3,514,608 | 35.11 → 36.95 |
| 78,43 | 52 | 58,479 → 78,961 | 4,643,504 → 7,374,784 | 3,962,408 → 4,571,192 | 37.08 → 58.69 |
| 31,175 | 49 | 48,961 → 107,147 | 4,757,152 → 9,274,992 | 3,122,752 → 4,615,288 | 23.76 → 50.96 |
| 146,47 | 52 | 53,371 → 58,444 | 2,812,024 → 3,318,436 | 2,544,048 → 2,844,848 | 14.89 → 30.46 |
| 92,15 | 53 | 56,944 → 72,804 | 5,974,744 → 6,552,776 | 2,890,520 → 3,316,792 | 28.15 → 29.66 |

All ten window builds preserve cache identity and the full SaveCodec/RNG bytes. Costs are materially higher, especially limestone. They require actual device validation before performance acceptance. Integrated v129 terrain/support changes can alter both counts and timing; rerun there for final APK attribution.

Hypothetical dense repetition is recorded separately in `asset-validation.json`: fifteen cliffs per 8×8 chunk, with 1/4/9/16 chunks. Nine fully dense chunks alone reach 197,370–212,760 cliff triangles. Those are honest cost scenarios, not observed national map counts; they exclude terrain, trees, units and GPU overhead.

## Verification

- PASS: 20 strict single-primitive GLBs, controlled float attributes/uint32 indices, no URI/extension, finite unit normals/colors/UV, actual nonzero triangle area
- PASS: legacy XZ footprint envelopes and source anchors; no map-coordinate changes
- PASS: normalized waterfall lip .34 and foot .50 UV rows in both LODs, with >.93 normalized drop
- PASS: authored panel4 stone/foam UV ranges remain separated; atlas byte-identical v128
- PASS: wall anchor vertex color; no accidental overwrite of legacy assets
- PASS: Khronos 2.0.0-dev.3.10 over all 375 GLBs in isolated clone, zero errors/warnings
- PASS: deterministic regenerate produces all 21 runtime files byte-identically
- PASS: real Java parser plus five near-window regions, cache/full save/RNG checks
- NOT_RUN here: actual APK screenshots; integrated host footprint/cascade checks separately pass
- NOT_RUN: ARM64 real-device FPS, memory, thermal/30-minute run

## Visual review and limits

`hero-comparison.png`, `earth-comparison.png`, `shore-comparison.png` show before/near/far at the exact same camera/light/atlas; each family also has a two-column before/after sheet. All are clearly labeled OFFLINE and NOT APK. `map-scale-before-after.png` is an explicitly illustrative fixed 15-cliff arrangement at span10 with cliff scale0.8, matching the production 0.65–0.95 scatter scale range. It is not an actual game map.

Pixel inspection confirms smoother organic silhouettes plus deeper granite/limestone crevices, stratified/battered support and denser curved reeds. Normal map-scale improvements are selective because these models remain small. The support still has a compact, steep cliff profile within its locked footprint. These assets alone do not recreate the PC game's continuous mountain massing, rich terrain texture, or water treatment. No full-PC-fidelity claim is justified.

Primary PC style reference inspected: [KOEI original San11 play report](https://www.gamecity.ne.jp/sangokushi/11/playreport/image04/img06.htm), [original screenshot](https://www.gamecity.ne.jp/sangokushi/11/playreport/image04/06.jpg). It is 480×360 and shows strongly broken gray-brown cliffs, deep recesses and irregular feet. Region and camera are UNKNOWN; same-view comparison remains REFERENCE_MISSING. Commercial screenshot is research-only and excluded from runtime and the delivery archive. The publisher [PC Steam listing](https://store.steampowered.com/app/628070/_/?l=schinese) also confirms the PC title and intended ink-painting 3D setting. No commercial model or texture was copied.

## Reproduction and artifacts

1. `blender -b --python-exit-code 1 --python tools/3d/refine_blender129.py -- .`
2. `python3 tools/3d/check_landmarks129.py`
3. `node tools/3d/validate_gltf.cjs`
4. `blender -b --python-exit-code 1 --python tools/3d/analyze_landmarks129.py -- .`
5. `blender -b --python-exit-code 1 --python tools/3d/review_blender129.py -- .`
6. Compile `Landmark129Benchmark.java` with the project field-check sources and org.json host jar; run with Java17, core resources, and enough heap (measured with -Xmx1800m)

Editable source: `out/feedback129/blender/landmarks-v129.blend`. Generation source, strict checker, BVH analyzer, matched renderer and real Java benchmark live in `tools/3d/`. Manifest: `docs/native-pc-visual/feedback-v129-blender-assets.json`. Reports, full timing data, hashes, thirty matched images and map-scale frames live in `out/feedback129/blender/`.

This report records isolated art-generation evidence; the combined report owns production source/APK attribution.
