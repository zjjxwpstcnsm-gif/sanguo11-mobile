# v0.62 road-only candidate — full task NOT complete

This branch safely inherits merged PR49 and changes ordinary ROAD ground rendering in TerrainArt, TerrainTiles, MapOverview and MapView. TerrainConnections, the R map codes, movement rules, map revision61, exterior1051 and CityAtlas56 are retained.

## Not completed
All14 northwest VOID coordinates remain unresolved and unchanged. New sand/city/gate/port artwork is NOT integrated. Two API representative files were obtained but could not be visually approved after the local runtime and image-view tools stopped responding. A native generated update poster is rejected and MUST NOT be treated as screenshots, an APK link or proof of completion. No generated file is shipped in this candidate.

## Verification
The current workflow tests the same installed probe against the exact released v061 APK and this candidate, then runs inherited map/save/port/native200 probes. Scope and actual outcomes are written to last-run.json. A green candidate build does not mean the full v0.62 work is done.

## Versions
Application 0.62.0-road-candidate/code62; geographic revision61; city asset56; SaveCodec31. Keeping these separate is intentional: no geography or building-art replacement was completed.
