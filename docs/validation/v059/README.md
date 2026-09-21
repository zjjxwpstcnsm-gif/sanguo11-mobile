# v0.59: five-region reference corrections and water/interaction acceptance

## Baseline and source identity

Started from main `894fb37729a6425c670026616f0ee32efa795790`, tree `ac234f4986d7cbc2447376acb970c9e0504d420d`. Its tree exactly matches the successful v058 source archive from `82749b539bb912c03fa04519791e5a83776d4d53`. Local archive bytes were compared using `git write-tree`; the local archive checkpoint is not claimed as an upstream commit. No v055-v058 rebase/rollback, rematerialize56 or old CI payload replay.

Original attachment was actually extracted and visually inspected: MAP_SAN11.jpg, 7200×6752, 34,925,627 bytes, SHA256 `a5a4e8c7f9785b6fbadc8487508ff6c937dcef2d782b099f2e876e48d167d4e0`. Original and crops stay outside repository/APK. `data/map/reference-v059/` records crops, coordinates, inherited transform, local grid-phase review, approximate Puyang remeasurement and explicit limitations. Reused fit residuals are not independent accuracy bounds. Multiple cities, Huguan gate, Wuxian port and terrain turns were checked; icon/text occlusion is retained as unresolved, not treated as land classification.

## This round only

109 unique national cells: **85 V→Q**, **24 R→M**; 109 CONFIRMED_VISIBLE surface observations, no new ESTIMATED edits. Regions: Chenliu southeast20, Yongan south42, Ye east17, Wu south17, Wuling south13. Q retains prior obstruction; visual water proof is not proof of original navigability. All85 original navigation conclusions remain UNRESOLVED. No existing W becomes Q and no new ship channel is authorized.

The road-to-mountain edits remove roads on exposed cliff centers, not generic "wall-looking" objects. R/D/B/H resolve through the real parser; D161, B158, H0 remain unchanged. No production wall/dam is hidden or made unclickable. No new MapRaster, cache or projection fix is claimed: inherited projection acceptance is rerun on the changed map.

Resource SHA256: `50ea7331d5b0a70cdb469ace3657f15f949ff645ea0e5f1d7255efab9c188cd3`, mapRevision59. Version0.59.0/code59. CityAtlas56 and all50 release-pinned map/art resources retained; SaveCodec31 unchanged. Every edit is absent from city footprints, development parcels, units and natural structures. Sites42/10/35 and plots591 remain byte-for-byte at their prior coordinates.

VOID: 2343→2258; internal888→803; boundary-connected1455 unchanged. Components234→219; internal219→204; axial padding19800 unchanged. None of these remaining cells is automatically classified as an error or legal outline. The full prioritized 25×25 block index and219 component records are in `source-grid-audit.json`.

## Old estimates and unresolved scope

The prior26 still-estimated edits (nine v057 sand; seventeen v058) are reviewed separately and are not counted as this round's109. None was geographically changed or promoted overall. Eight prior Q surface appearances are visible again, but original navigation remains unresolved; nine Ye ground subtypes and nine northwestern sand estimates remain insufficiently established. Seven explicitly deferred local candidates are listed in `unresolved-local.json`. Remaining2258 source VOID needs regional review; this is not nationwide completion.

## Saves

Only new games receive revision59. `NationalMap` explicitly accepts old58 alongside56/57, preserves embedded terrain and state, and shows the new-game notice for58. No save files are deleted, no units/sites relocated, no revision relabelled in production. Unknown map revisions remain rejected. Synthetic reversed test checkpoints use strict postimages; actual original058 Android saves are separately compared against the unchanged058 encoder on the same JVM, and loaded by the installed059 application. Native200 march budgets remain14 for base7 on old58.

## Verified before final build

- Exact released058 APK baseline installed on API29/x86_64: [run35521913246](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35521913246). Four actual scenarios, original APK hash `93e4f9b1ff0f32f1d336a98623ba2e2bc12e33f514a0465d3ad33325b9ff1c9f`. Real MapView pointer input, coordinate callbacks, before screenshots and four raw old saves. Baseline sampling does not alter world bytes.
- Local `scripts/test-map59.sh` passed: all source/padding cells and coordinate/raster roundtrips; terrain enum/details; all nine scenarios;109 edits and neighbors; all weapons/ships reject corrected Q/M; protected site/plot identities; 7,569 ordered site topology pairs per national era unchanged; camps, neutral objects and save roundtrips.
- Actual dock constraints tested in both directions, all three ship types, five shore terrains and W/SEA. Q never becomes a dock lane; mountain paths/shallows still require Difficult March. Existing `PortReplayTest` now passes1,418 checks including real CampaignAi embark/capture-port/land/resume, capture RNG and replay.
- Seven-cell transit, explicit entry, edge attacks/AoE and turns rerun via inherited native56/city55 tests. Shared MapRaster and all nine mixed R/D/B connections×six directions rerun, not represented as new features.
- Apply guards retain complete pre/post digests, every before value, duplicate/bounds rejection, corruption/different-baseline failure and postimage idempotency. Signed057 and058 checkpoint hashes are reconstructed and checked exactly.

## Failures investigated, not hidden

The first baseline test-APK replacement omitted shared sampler helpers (compile failure); the second used faction5 in local three-faction scenarios (national screenshots succeeded, local load rejected). Fixed test harness only; third baseline run passes all four. Neither is a production crash or an inherited failure claim.

The old PortReplay v47 fixture put a one-cell fake city over a water unit and adjacent owned dock. It fails identically against the untouched058 source tree; `port-baseline58.log` records this. The fixture now uses a legal seven-cell city away from docks. Its capture fixtures now use five legal outer parcels and an external siege cell, preserving the original five-facility probability sample and all assertions. No gameplay rule was relaxed to make the test pass. New dock tests initially omitted Difficult March for D/S shore landing; the explicit research prerequisite is now tested, not bypassed.

## Final installed acceptance status

IN PROGRESS at this functional source commit: build the final pushed059 source, install that exact APK, run Reference59Probe plus retained Native56Probe, inspect paired screenshots, and publish standalone APK. Final run/APK identity and step outcomes are delivered in Release metadata and the PR completion comment; do not reuse058 results as059 acceptance.

Reference59Probe samples both national scenarios and both crop edges/corners, current Q/M fixes, retained ROAD/D/B, real neutral fixtures, LOD/pan/pinch, minimap/territory, four actual old058 saves and an actual MOVE command beside the reviewed Yongan channel. Synthetic entity/march setups are explicitly labelled and confined to androidTest. Native56Probe separately covers actual sortie/city transit/entry/combat/next turn/save. No physical ARM result or full historical CI pass is claimed.
