# Hukou v130 source refinement — PARTIAL

Scope: the two new Hukou GLBs and their original Blender authoring source. This is focused source evidence, not APK acceptance. No terrain, river mask, height field, gameplay, shader, atlas, old GLB, or another landmark family is modified by this source task.

## Actual composition diagnosis

Reviewed `out/landmarks129/runtime/lavapipe-candidate/images/hukou-menu-span10-surface.png` before modeling. The actual span-10 source-matched APK displays a narrow upright ornament disconnected visually from its bank. Read the production `Vegetation.cascade`, `cascadeFits` and the canonical `TerrainSurface.meshHeight` flow. The v129 support has about 0.579 world-unit overall width; placement forces at least 0.78 rise and adds 0.45 above the highest sampled footprint. Whole-vertex terrain lifting alone cannot turn that tall raised support into a compact gorge. More faces in that same envelope would not address the composition.

Native integration and real APK results are recorded separately in reports/feedback-v130.md. This source intentionally uses normalized y=1, lip z=.34 and foot z=.50 so those placement changes remain explicit in native code. Recommended source presentation is broad, shallow bank shoulders with a compact channel, not another high pillar. The host review applies the same `.44` illustrative vertical scale to both v129 and v130; it is not an assertion that this is the exact terrain-derived APK rise.

## Authored geometry

- Unequal stepped shoulders widen to 0.9132258 units at upstream z≈.17, then narrow toward the neck and foot
- Three main horizontal shoulder terraces, inset oblique fracture cuts and zero-height outer contacts replace the old enclosed upright side slab
- A narrow supporting center bed terminates under the curtain; maximum normalized y remains exactly 1
- The main water sheet curves from broad ochre inflow into a constricted turbulent throat. Its position-sharing UV sections continue through a pale impact apron into a low blue-green river tail
- Attached shallow rolling foam relief and three embedded foot spalls remain; no floating spray, new texture, alpha trick, extra primitive, or generic subdivision modifier
- Existing 512×64 v129 atlas bytes are embedded unchanged. Rock uses existing panel 0; ochre panel 3, pale impact panel 4, river contact panel 5. Explicit V reversal is only in the Blender preview material; runtime uses the existing PNG-row0=UV.v0 convention

Near: 2,606 triangles / 3,992 full-attribute vertices / 258,100 bytes. Budget breakdown: 1,152 water triangles; 880 terraced support triangles; 574 attached foam/spall triangles. Far: 678 triangles / 1,103 vertices / 96,288 bytes, retaining the same broad silhouette, constricted throat and river contact. The far count is 26.0% of near. Both remain one indexed primitive. These are asset counts, not mobile performance measurements.

## Exact source and reproduction

Run from the repository root with Blender 4.3.2:

`blender -b --python-exit-code 1 --python tools/3d/refine_hukou130.py -- .`

The script writes only the new `v130/fall-hukou-lod{0,1}.glb`, focused manifest, and `out/landmarks130/hukou/` source evidence. Its imported low-level constructors/exporter are pinned by hashes in `hukou-v130-source.json`; those previous source files are not executed wholesale and are not modified.

Editable packed source: `out/landmarks130/hukou/hukou-v130-editable.blend`. Runtime meshes remain Y-up in their own collection, with named vertex groups for the continuous water, individual shoulders, supporting bed, impact relief and spalls. The exact baked v129 baseline is present only for offline review. No modifiers or external model dependencies obscure the edited vertices.

Independent second export command:

`blender -b --python-exit-code 1 --python tools/3d/refine_hukou130.py -- /tmp/hukou130-repro`

Both second-export GLBs compare byte-for-byte equal. Near SHA-256: `a25c6db3505882c0df838b4612e05b2b326e5400022cb2d2137be95874f91d77`; far SHA-256: `397a8da0034ecda6ac0cc4e63fe5b7fbd817c3c730feeccffe173e91c2120cac`.

## Verification and limits

PASS, host source only:
- Actual Blender 4.3.2 generation, packed editable source, exact indexed one-primitive GLB
- Independent second export is byte-identical
- Khronos validation: 0 errors / 0 warnings on each GLB (5 non-error unused-object hints each)
- Finite positions/colors/UVs/normals, unit normals, UVs inside [0,1], only the four intended atlas sectors
- Byte-identical embedded v129 atlas; normalized lip y=1 at z=.34 and foot y=.015 at z=.50
- Both authored shoulder span and source max y agree across LODs; all source z≥0
- Fixed-camera near/far and strategy-camera 55-degree tilt review at 61.5 pixels/world-unit

Evidence: `out/landmarks130/hukou/host-checks.json`, `khronos.json`, `build.log`, six `*-OFFLINE.png` source previews. All previews are OFFLINE BLENDER SOURCE, NOT APK. They omit invented terrain and cannot prove bank integration. No artificial background map, screenshot compositing, or off-screen generic ornament is used as runtime proof.

Open: native `cascadeFits` footprint test, source-matched installed APK visual comparison, exact same-region PC reference (`REFERENCE_MISSING`), ARM64 performance. Existing global visual acceptance remains PARTIAL until these separate gates are reviewed. No commit or push was performed by this source task.
