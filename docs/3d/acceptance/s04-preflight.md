> Historical checkpoint. Superseded for current S04/S05 implementation by
> [s04-s05-runtime.md](s04-s05-runtime.md); retain this file as the earlier record.

> Superseded preflight: S02 and S03 were subsequently recovered from other local workspaces
> and integrated on the same branch. The absence statements below describe the earlier
> remote-only inspection, not the current source tree. Installed/art/performance gates remain.

# S04 preflight and facility state repair — PARTIAL / BLOCKED

2026-09-22. Requested branch: `agent/3d-s01-renderer-foundation`, existing PR #62.
Input branch HEAD `df1c1f94210bd558b1be9abcf8ff564ce4783dc2`.
No merge into main. User's explicit branch instruction overrides the task archive's main/merge instructions.

## Predecessor gate

At checkout the remote contains S01 only. `SceneMesh` still generates flat terrain and
six expressly temporary silhouettes. `asset-manifest.json` lists no S02 terrain assets,
S03 site model format, UVs or textures. PR #62 also identifies itself as S01.
Do not claim that earlier chat requests constitute delivered S02/S03 code.
The task archive requires repairing predecessor blockers before layering later art.
This checkpoint repairs the existing facility presentation contract; it is **not completed S04**.

## Production changes

- All 11 domestic types (including five upgradeable types at levels 1–3) and 19 military types
  retain their actual type, owner, durability, construction and fire state in detached snapshots.
- Domestic city ownership drives faction color. Level, merge target, remaining construction,
  damage and fire appear in labels; selected facilities retain labels at distant zoom.
- Military upgrade type, completion and trap direction are captured without inventing a level.
- Selection/coverage still use the existing core fieldworks result. No rules or map data changed.
- Status-only changes share the existing mesh. After object removal/recolor, unused mesh buffers
  are destroyed after referencing renderables, instead of accumulating until the scene closes.
- Ground projection changes reposition surviving objects even when their cell ID is unchanged.

## Actual facility coverage matrix

Every row below is explicitly unfinished art. Generic silhouettes are **not** final model coverage.

| 正式类型 | 等级 | 当前资源 | 本轮验证 | 美术验收 |
|---|---|---|---|---|
| domestic/MARKET · 市场 | 1–3 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/FARM · 农场 | 1–3 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/BARRACKS · 兵舍 | 1–3 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/SMITH · 锻冶所 | 1–3 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/MINT · 造币 | 1 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/GRANARY · 谷仓 | 1 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/STABLE · 厩舍 | 1–3 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/BLACK_MARKET · 黑市 | 1 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/WORKSHOP · 工房 | 1 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/SHIPYARD · 造船厂 | 1 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| domestic/BRONZE_TERRACE · 铜雀台 | 1 | SceneMesh.proxy(4)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/CAMP · 阵 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/ARROW_TOWER · 箭楼 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/MUSIC · 军乐台 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/FIRE_SEED · 火种 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/FORT · 砦 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/FORTRESS · 城塞 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/CROSSBOW_TOWER · 连弩楼 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/CATAPULT_TOWER · 投石台 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/DRUM · 太鼓台 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/STONE_MAZE · 石兵八阵 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/EARTH_WALL · 土垒 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/STONE_WALL · 石壁 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/FIRE_BALL · 火球 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/FLAME_SEED · 火焰种 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/FLAME_BALL · 火焰球 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/INFERNO_SEED · 业火种 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/INFERNO_BALL · 业火球 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/FIRE_SHIP · 火船 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |
| military/DAM · 堤坝 | 枚举独立类型 | SceneMesh.proxy(5)，S01占位 | 状态快照回归 | **BLOCKED：正式模型/UV/贴图未交付** |

## Verification and limits

`bash scripts/test-3d-foundation.sh` runs the existing national-map regression and the new
`SceneFacilityStateTest`. It enumerates every type/valid domestic level, checks detached
state changes, deletion, ownership, type upgrades, selected core coverage and unchanged
serialized national scenario state. Its comprehensive type fixture uses direct test setup;
it does **not** claim a build/attack/demolish user-command lifecycle or installed visual acceptance.

Android APK/test APK/lint and presentation results are recorded in progress.md after execution.
No device/emulator is attached locally; no new screenshots or installed checks are claimed.
GPU deletion ordering is code reviewed and compiled, not a measured native leak test.

Still blocked: S02/S03 predecessor delivery, actual facility models/textures/LOD,
forest exclusion and stable placement, chunk merging/instancing, local vegetation dirty updates,
three density stress scenes, real command lifecycle, screenshots, GPU submissions/timings,
PSS/texture memory, 30-FPS midrange acceptance. No performance values are invented.
Do not advance to S05 based on this checkpoint. Resume on this same branch after S02/S03
are actually available and verified; retain the snapshot contract and resource cleanup.
