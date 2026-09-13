# Agent 3 — mobile UI / map v0.4

## Scope and baseline

- Based on main `f23d656a92781573226fbcf03aa92c79da46f2bf`. Branch: `agent/mobile-ui-map-v04`.
- Keeps Java 17, native Android Views and Canvas, sensorLandscape, minSdk 26 / targetSdk 35. No Compose/framework migration, commercial artwork or engine rule changes.
- The workspace initially had no checkout, SDK, Gradle or emulator. Retrieved the exact main objects via the authenticated GitHub connector and verified tree/commit hashes. SDK 35 and Gradle 8.11.1 were later installed locally. Emulator/KVM remains unavailable locally.
- Before editing: ran all 1,196 existing core assertions; inspected baseline CI run `34755189627` instrumentation output and screenshots for launch/faction/city/build/transfer/cargo/turn/save/load. This is a review of recorded execution, not a claim of a local hands-on emulator session.
- Baseline issues: long vertical city command stack, tasks in a large modal mixing completed facilities and transit, no searchable/filterable officer list, no slot save timestamp, no turn confirmation/settlement report, city targets shrink with map, permissive camera bounds, always-visible detail clutter.

## Pages, navigation and components

Bottom navigation: **地图 / 城市 / 武将 / 任务 / 菜单**, plus **下一旬**. Map stays mounted. Tapping 地图 toggles the side panel; selecting a map entity or list item reopens it. Top HUD has faction/date/AP plus 全图 and 定位. System bars/cutouts are inset. Side panel takes about 38% of landscape width (240–344 dp). Controls are at least 48 dp high.

City details: **概览 / 内政 / 武将 / 军事 / 调动**. Shared resource header, core stationed officer, equipment, facilities/income, existing commands. Core officer is the highest-leadership stationed officer, explicitly not an invented governor appointment. `CityCommand` entries register military labels/actions; new engine commands can be appended without changing map/navigation.

`OverviewUi`: recycled native ListView rows with stable city/officer/task IDs, plus empty-state views. City sorting: friendly first / gold / food / troops / stationed officers; tapping locates the city. Officers: live name search, independent city/faction filters, name or five ability sorts, readable compact stats and detailed state/location. Tasks unify active construction, transfers and cargo with executor, source, destination, status/remaining turns, cargo and focus/management actions. ETA comes from `Domestic.eta`, not copied pathfinding. Completed facilities remain in city 内政.

`UiModels`: read-only projection helpers. `ClientState`: page, group, filters, query, summary and panel state saved in Activity Bundle. `TurnWork`: detached engine snapshot on a worker, retained across Activity recreation; main-thread delivery detaches old Activity observers. Game saves still use `SaveCodec` v3 and `AtomicFile`, preserving previous filenames and v1/v2 compatibility. No core/model changes.

## Map interaction

`MapCamera`: pure Java transform, minScale = fitted map, maxScale = max(4 × minScale, 2 × density). Pan bounds keep the map within the viewport; small maps center. Pinch preserves focal world point except at bounds. Resize preserves camera center/zoom ratio. Selected tile, camera, filters and page survive Activity recreation.

`MapView`: one-finger pan, pinch, confirmed single taps, double-tap city focus. Two-finger sequences suppress selection. City hit targets at least 48 dp diameter, labels also tappable, nearest city wins overlapping targets. Precise tile hits retained while issuing unit commands so target expansion cannot convert adjacent movement into entering a city.

At overview zoom: cities/names/faction rings. At 1.55 × fitted scale: facilities, faction labels, units, missions. At 2.1 ×: city resource summaries. Selected units stay visible for tactical commands even when zoomed out. No per-frame `nextTurn`, `route`, `eta`, `reachable` or world recalculation. `reachable` remains computed on UI state refresh, not during gestures/draw. Typeface is cached. Large-map tile geometry still loops/culls visible cells; future national maps may benefit from cached terrain layers if profiling demonstrates a bottleneck.

## Feedback and confirmations

End turn shows unused officers/AP and next-step effects, then a scrollable settlement summary with engine before/after net resource changes, building progress/completion and actual mission arrival/cargo. Net changes are explicitly not gross income. Last summary can be reopened from 菜单.

Three manual save slots show scenario, faction, in-game date, ordinal turn and last file-save time. Empty slots save immediately. Occupied/corrupt slots require overwrite confirmation. Load/overwrite/turn cancellation leave state unchanged; empty/corrupt loads give understandable messages. Manual/auto saves remain atomic.

Large cargo review: gold >= 1,000 OR food >= 10,000 OR troops/any equipment >= 3,000. This is a UI confirmation threshold only. Final validity/deduction still belongs to `Domestic.transport`. Existing demolition/cancel-build confirmation retained. No stack traces shown to players.

## Validation and delivery

- Local core regression: 189 core + 282 scenario + 725 domestic assertions passed.
- Presentation tests: camera limits/pinch/resize over 1920, 2340, 2400 pixel widths; search/filter/sort; task identity/ETA/cargo; immutable projections; summary actual net changes/completion/arrival.
- Full Android Java source and instrumentation source compilation against SDK 35 passed.
- Added official Gradle 8.11.1 wrapper pinned to distribution SHA-256 `f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6`.
- Instrumentation extends the previous actual-touch install flow with map hit/pinch/pan, filters/recreation, empty states, cancel turn, cancel overwrite, large cargo cancel/confirm. CI tests 1080×1920, 1080×2340 and 1080×2400 at density 420 on Android API 29 x86_64 (landscape); collects screenshots and crash/ANR logs per profile.
- Final Gradle/lint/emulator/APK results and digest: recorded below after verification.

## Merge guidance

Primary changes: `app/src/main/java/game/sanguo/mobile/` and Android instrumentation/scripts; minimal build/CI support. No engine, save codec, AI, resource or combat changes. Agent 1/2 can merge their core APIs independently. Resolve `MainActivity`, `DomesticUi`, `MapView` conflicts by preserving the navigation shell/read-only projections and wiring new engine command methods. Do not import the other branches' unpublished models to make this UI compile. Formal governor/officer-status/battle APIs should replace the relevant projection once available. Keep `manual.sg11`, `manual2.sg11`, `manual3.sg11`, `auto.sg11` filenames.
