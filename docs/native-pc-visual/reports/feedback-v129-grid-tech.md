# v129 difficult-march rules and force-aware grid

Status: **PARTIAL**. Host implementation/tests PASS; Android compile, installed
Surface evidence and ARM64 device verification are NOT_RUN in this isolated
host environment. APK/CI results are recorded in the combined report.

## Requested correction

The initial input blocked MOUNTAIN_PATH and SHALLOWS, but allowed PLANK_ROAD
with troop-loss handling. The user explicitly chose to change plank roads to
“未研究就不能进入”. The only new gameplay restriction is that owning force's
DIFFICULT_MARCH requirement on entry into PLANK_ROAD. The terrain enum, map,
SaveCodec and research IDs/formulae remain unchanged.

`Fieldworks.requiresDifficultMarch` is the shared authoritative family. Its
existing `landCost` query now gates all three terrains. Actual player movement,
reachability, immediate/persistent previews, transport routing and paid dispatch,
AI routing, port landing, deployment, and forced displacement all consume this
existing rule boundary. No parallel movement algorithm was added. 踏破 does not
bypass the force requirement. Research retains the previous 3-point plank/path
and 2-point shallows costs; the existing hazard/immunity implementation is
untouched. Technique, terrain and skill descriptions state the corrected rule.

## 2D/3D presentation

- A common terrain filter derives from the core family. Water/sea/poison are
  still potentially gridded; mountains, non-navigable water, VOID and nationally
  restricted cells remain grid-free
- Normal play uses the viewing player, not temporary AI `active` or a selected
  enemy unit. Opening preview explicitly uses its selected force
- `MapHost` checks the force and actual technique bit even with a reused World
  and unchanged terrain revision. A new immutable context shares the exact old
  surface, shore field and terrain bytes; it never mutates an older snapshot
- Grid-bearing SceneMesh fingerprints include the technique bit. Overview
  geometry stays reusable. Force switches with identical capabilities may
  safely reuse identical geometry
- Filament consumes only detached values. Old terrain may stay visible during
  async replacement, but its mismatched grid is hidden on the next admitted
  frame, before replacement meshes have finished uploading
- Editor-specific grids still expose every valid cell for inspection/painting
- 2D's former all-terrain grid now applies the same filter and preview/player
  force. No 2D/3D gameplay divergence is introduced
- Existing `MapProjectionQuery.blocked` remains the editor's static terrain
  inspection layer; it is not the ordinary gameplay grid

## Old saves

The checked-in `core/src/test/resources/grid129/pre-gate-plank.sg11` was produced
with actual v128 input core at f11ad0bff299dca21d10d1209d14779905fe1978,
before the rule edit. It contains an unresearched unit already on a plank and an
old queued move to an adjacent plank. SHA-256:
`023b2bb02bc7dce50c6d945e6503755345a964dbc9f82fb7752b44e6237cdd06`.

The candidate decodes/re-encodes those bytes exactly, without migration or
repositioning. The occupant can leave onto ordinary terrain. Its existing
order pauses before entering another plank, and opens after research. This
preserves a playable exit for historical positions without granting new entry.

## Validation

Commands used Java 17 (`PATH=/tmp/jdk17/bin:$PATH`). Logs are in
`docs/validation/grid129/`.

| Check | Result |
| --- | --- |
| `bash scripts/test-grid129.sh` | PASS: 131 core + 75,162 projection/mesh/session checks |
| `bash scripts/test-grid-coast.sh` | PASS: existing coast/surface/material/seam/topology suites |
| `bash scripts/test-architecture.sh` | PASS: architecture fence, 1,673 GameSession checks, bridge, 119,600 projection comparisons, 32 layout checks |
| `bash scripts/test-ui-models.sh` | PASS |
| `python3 scripts/check-architecture.py` | PASS after final edits |
| `git diff --check` | PASS |
| `bash scripts/test-core.sh` | FAIL inherited: first CoreTest assertion `AI uses deployment commands` |
| `bash scripts/test-native-ground-stream.sh` | FAIL inherited: historical v105 national geometry hash; queue checks pass |
| Android APK/test APK build and installed probe | NOT_RUN: no Android SDK/adb in this host environment |
| Physical ARM64 and visual/art acceptance | NOT_RUN |

The new focused tests independently cover all weapons, 踏破 no-bypass,
researched costs, player/AI/transport paths, paid transport, port landing,
deployment, victim-owned displacement access, real research completion,
GameSession turn tickets/replacement, 2D/3D filter parity, exact per-cell ribbon
counts/interiors, immutable snapshots, before/after/force/preview/AI context,
near/overview cache behavior, and unchanged save/RNG on read-only operations.

To classify broader failures, production and test sources were independently
compiled from the exact v128 input and candidate. These suites fail at the same
first assertion in both, with logs retained and no assertion removal:
CoreTest; ArmyTest; TechnologyFieldworksTest; TacticalLogisticsTest;
MarchOrdersTest; CampaignAiTest; DisplacementTest; ObjectiveOrdersTest;
MapSkillsTest; NavigationDefenseTest; Reference59Test. Streamed national v105
hash failure was likewise independently reproduced on the v128 input. These
failures are not a full regression PASS and remain outside this narrow fix.

## Installed probe prepared for combined-source validation

`NativeGrid129Instrumentation` launches normal MainActivity from an explicit
small save with research one turn from completion. It runs the actual UI turn
path, compares complete headless authority/RNG, then checks six native Surface
phases: before research, after research, unresearched player replacement,
researched force preview, unresearched preview, and grid-toggle restoration.
It also checks actual resident GPU batches and rejects obsolete grid capability
stamps after admission. The original 120-second readiness budget is retained.
The test fixture is not a production map or a gameplay shortcut.

Run after building the integrated candidate and test APK:

```
bash scripts/test-grid129-installed.sh <candidate.apk> <test.apk> out/grid129/runtime
```

The helper verifies installed APK bytes, invokes the registered instrumentation,
collects screenshots/logs/device identity, and fails unless `PASS GRID129` is
present with no failure markers. No installed PASS is claimed here.
