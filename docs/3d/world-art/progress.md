# S10 continuous terrain

Status: PARTIAL (implementation and host validation landed; native/device acceptance tracked separately).
User-selected branch: `agent/3d-s01-renderer-foundation`; PR #63 targets main. Do not merge.
Baseline: `322bcacd4166edd78c9c7a5496c49f6077f48edc`, S01–S09; main contains that head through `dafda0e3c3f240205a8b336dd1f6398e33cdeea6`.
Runtime candidate: `1be4ddc2642c63a04a4ab0bd4fafeb0c57eed1ca`, version 0.77.0-3d-s10-rc1 / code 77.

Implemented: four periodic albedos + normal/roughness textures; compact global normalized material field; dedicated ground lit material and tangent stream; world UV; stable shared cell/chunk samples; slope exposure; exactly interpolated near LOD data; bounded halo invalidation and unchanged ground/picking geometry. Existing unlit proxy/site materials and the GLES MSAA gate remain intact.

Validation and remaining work: see `acceptance/s10.md`. Read `handoffs/s10.md` before S11.

## S11 candidate

Continues e64992d on the user-requested PR #63 branch. Regional constrained landforms and disjoint water material batches are implemented in the normal game renderer. Exact footprint, material shoreline bands and local shape-derived flow preserve water topology. Host geometry/material/water/editor/port regressions and clean APK/signature/lint checks PASS. Native SwANGLE PASS:681 assertions and54 same-runtime Surface captures inspected across all three qualities; stage remains PARTIAL due to conservative exact shore silhouettes, physical-device and manual acceptance gaps. See handoffs/s11.md and acceptance/s11.md. No main merge.
