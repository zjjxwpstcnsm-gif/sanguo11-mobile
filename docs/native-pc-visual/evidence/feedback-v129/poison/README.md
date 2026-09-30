# v129 POISON material subtask — PARTIAL until combined APK verification

The input renderer gave `POISON` exactly the forest grass/soil mixture and no spring-specific appearance. The authoritative `POISON` enum, map cells, movement cost, damage, immunity, save format and rule RNG are unchanged.

## Production changes

- Existing `TerrainMaterialField` now adds a bounded presentation-only carrier to the existing UV1.y tone lane for the actual `POISON` type. Its material version increments to invalidate old terrain chunks. Biome weights, terrain height, normal frame, shore distance, geometry, picking and water partition remain unchanged
- New versioned near/overview materials restore ordinary macro tone before drawing opaque mineral springs: subdued blue-green basins, dark wet-rock rims, broken sulfur deposits. The two programs use the identical mask/pigment block. No emission, transparent pass, added texture/sampler, vertex stream, geometry, or draw call
- The `poisonGrid` float2 uniform carries `(staggered ? 1 : 0, grid.offset)`. The fragment mirrors `GridWorldTransform` to recover the owning cell centre in both layouts. Irregular radial support is strictly below 0.445 world units, so it cannot cover an adjacent safe-cell centre or a grid boundary
- Both near and overview retain every canonical cell-centre carrier. Local edits use the existing bounded eight-cell fingerprint halo; the fixture rebuilds 4 chunks and retains 20 distant chunks
- `SceneMesh.backdrop` must use `attachBackdrop`, retaining its original ordinary tone. Coarse exterior triangles must not interpolate gameplay-cell hazard carriers into VOID scenery
- All original material sources/binaries remain byte-identical. Only the new `3d/terrain/v129/` material programs are added

The combined production source integrates `FilamentMapView` paths, `VerifiedMaterial` hashes and admitted-frame `poisonGrid` binding. APK and device evidence are recorded separately in the combined report. These host images do not establish device rendering.

## Host evidence

`poison129-ordinary-host-review.png` and `poison129-near-host-review.png` show input/candidate/optional logical-edge guide using actual production mesh payloads and CPU emulation of the material base color. They are explicitly **NOT APK screenshots**, omit Filament lighting and scenery, and use neutral water fill. The fixed views are world X/Z `(17,164)` over 18 units and `(17,165)` over 5 units.

The export compares the original v128 field with v129 using the same actual map and `SceneMesh.ground` path. Terrain positions, biome weights, normals, UVs, shore values, indices and water partition are byte-identical. Only 1,611 tone-lane vertices change in the 20-chunk window. The offline fragment comparison checks unchanged pixels byte-for-byte: 293,751 ordinary-view and 184,904 near-view pixels. Pixel counts are consistency checks, not aesthetic or device acceptance.

The v128 test fixture is the source at `f11ad0bff299dca21d10d1209d14779905fe1978`, plus a documented compatibility alias `attachBackdrop` that delegates to the original attach function. No sampling formulas are changed in that fixture.

Reproduce:

```
PATH=/tmp/jdk17/bin:$PATH bash scripts/test-poison129.sh
PATH=/tmp/jdk17/bin:$PATH bash scripts/review-poison129-host.sh
MATC=/tmp/filament128/filament/bin/matc bash tools/3d/build_poison129_materials.sh
```

Raw exported binary payloads remain in `out/poison129-host/{before,after}/mesh.bin`; the committed source exporter regenerates them. The field audit covers all nine bundled scenarios: seven full maps each containing all 179 POISON cells; existing central/Jingxiang crops contain 0/74. `all-poison-cells.csv` enumerates the full-map source and storage coordinates.

## Test development corrections

Initial test assumptions incorrectly counted every bundled scenario as a full map; the two legitimate crop counts were established from the authoritative scenarios. Synthetic custom fixtures needed valid UUID/revision fields and a city for SaveCodec. Water's existing UV1.y flow-angle values cannot be checked with the ground decoder, so material payload checks traverse the actual land primitive indices. A proposed poisoned city fixture is correctly rejected by the unchanged authoritative site-footprint validator; this rejection is now asserted rather than weakening the rule. The first backdrop test accidentally attached material attributes twice; the final test calls the actual single-attach `SceneMesh.backdrop` path.

The first host art review rejected square patch darkening from the encoded tone; the final material restores normal tone and uses a rounded, varied rim. These were fixture/design corrections, not rule changes or concealed inherited failures.

## Remaining gates

- Combined-source APK rendering, zoom/pan/grid and actual GPU uniform delivery: device verification pending
- Same-camera PC reference: REFERENCE_MISSING
- Physical ARM64 performance, thermal behavior and long-session stability: NOT_RUN
- This subtask does not claim the full historical core suite passes; previously reported inherited core failures remain outside this visual-only change
