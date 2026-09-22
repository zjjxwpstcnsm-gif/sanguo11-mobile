> Remote-delivery blocker below is historical: these local S02/S03 changes were recovered and integrated with S04 facility-state fixes on PR #62. See progress.md and PR for the new remote head. Device/art acceptance is still PARTIAL.

# S03 — PARTIAL / remote delivery BLOCKED

Production changes: modular textured GLB architecture in the real Filament game map. Five
families x three LODs, 87 site IDs, unknown custom IDs default by kind. Atlas and buffers
are shared. One continuous city model on one logical seven-cell foundation; model walls
add no collision. Port direction uses World.army.water, excluding non-navigable water and
land-passable shallows. Site selection outlines all legal cells, and unit ground rings
remain visible over structures. Long press reverses the camera. No core/map files changed.

Evidence:
- 3D foundation + terrain regression: all checks pass (see s03-cpu.txt).
- Site mapping, custom defaults, ownership/deletion/damage snapshots, forward/reverse
  height picking and LOD hysteresis pass. Exact GLB loader reads all 15 runtime assets;
  malformed headers, truncation and nonfinite position data are rejected.
- CityFootprint55Test: 11,070 assertions, formal movement/garrison/combat/AI/maps/turns/saves.
- Re-running the model exporter reproduces every asset SHA256 exactly.
- Debug APK, test APK and lint passed at an intermediate checkpoint; final build results
  are recorded alongside final APK (which contains its source SHA in BuildConfig).
- Native SceneInstrumentation was extended with city/port/gate captures from both camera
  directions and GLB/material/no-fallback checks. It is compiled, NOT executed this turn.

Unfinished / not claimed:
- No adb device and no local emulator/KVM. Installed loading, graphics driver behavior,
  ownership deletion GPU inspection, actual touch gestures, port boarding and gate capture
  UI flows need device rerun. Existing core behavior is covered but not an installed test.
- No genuine S03 game screenshots. Offline mesh inspection checked both sides only; it
  cannot stand in for actual game evidence or final art quality acceptance.
- Modest original architecture with a 192x64 atlas; no claim of final SAN11 art parity.
  UV density, gate/mountain transition quality, high-angle occlusion and damaged silhouettes
  need art review. Damage currently darkens surfaces; it does not collapse geometry.
- Frame P50/P95/P99, stalls, RAM/PSS, GPU memory, cold start, battery, installed size/cache,
  dense-city actual draw calls and 2D/3D same-device performance deltas: NOT MEASURED.
- Reuse is not GPU instancing: nominal 3 draws/city (body/flag/base), 2/port or gate. No
  measured draw-call reduction claim. Mesh/texture allocation report is an asset budget only.
- S02 remains PARTIAL, including static water and pending shoreline/device acceptance.
- Push and PR updates blocked by Git HTTPS authentication and connector metadata errors.
  Do not call this local APK a PR-head APK until remote head verification succeeds.
