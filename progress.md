# v0.61 · 两角边界与连续外景
- DONE：安全合并指定PR48，基线049652d；原图实际解压核验；206格V→Q；1051格明确白名单外景，未修改逻辑有效性；全国源坐标展示统一；定向本地模型/工具/投影通过。
- DONE：真实v060安装对照 run35554158663，完整两角/两局部截图与四份真实60存档。
- IN PROGRESS：本轮功能源提交、APK构建/安装验收、同哈希发布、待审PR、最终前后目视记录。
- TODO：西北14格岸线/墙遮挡仍未解决，无外景遮罩。原版精确enabled-hex/航行表未取得；标题下材质仅估计；不宣称100%还原。
- 验证与证据：docs/validation/v061/README.md；data/map/reference-v061/。

# v0.60 — 批量水面恢复 / 岸线与通行验收

- DONE: actual latest-main tree and uploaded original fingerprint verified; all54 nonempty v059 VOID blocks, 42 city and45 gate/port neighborhoods reviewed.
- DONE:987 unique V→Q (504 eastern gridded sea +483 inland water); full strict per-cell ledger, source revision60 and exact map SHA. No old corrections recounted, no W made Q, no raw reference redistributed.
- DONE: remaining1271 VOID individually classified; source topology/internal357/boundary914 is not a defect/legality count. Padding19800 separate. Old7 candidates:2 surface-only resolutions,5 retained; old26 estimates not counted again.
- DONE: actual Q invalid-origin cost failure reproduced and fixed in common Army movement/entry query; port/research/ship rules preserved. Local directed models, mixed terrain/water projection, protected sites/plots, saves and24 apply-guard negative cases passed.
- DONE: exact published059 APK four-scenario baseline sampling run35549692460 passed and generated raw old saves/before screenshots.
- DONE: source fec13cba0906af101c3e76bbc397801797a9dfc9 built/installed/published in SUCCESS run35551707443; Reference60 Android978 + retained native200 gameplay92 assertions, eight real058/059 saves, signed in-place upgrade with old manual bytes unchanged. APK SHA25650b2ed1df0dd405e6cc14b907c1968a69e837aee08f3af5f41c7fe34df88b412,22335219bytes.
- DONE:11 actual before/after paired-coordinate screenshots reviewed plus Q/local/march/new-game/labelled-DAM/LOD samples. Standalone APK and separate screenshot/log Release archive downloaded back and byte-verified. See docs/validation/v060/final-publication.json and visual-review.json.
- A: confirmed987 water cells restored; retained1271 VOID classified but not all proven legal exterior. B: directed installed scope PASS, not physical ARM/FPS/full-history certification. C: original nav/old-estimate/plank/outline uncertainties remain.
- Documentation/publication commits are not BUILD_COMMIT; no functional code/map/art changed after installation. PR48 targets main, not merged by this task.
- TODO: original navigation permissions, remaining outline/occlusion/subtype evidence and physical ARM/full historical CI. No nationwide100% claim.
- BLOCKER (local environment only): no direct GitHub network/Android SDK; connected Actions are the active delivery path, not a reason to stop.

---

# v0.59 — five regional corrections / water and interaction acceptance

- DONE: main894fb377 tree verified, original reference fingerprint actually read; exact released058 APK four-scenario baseline run35521913246 passed with raw saves and paired-coordinate screenshots.
- DONE:109 new unique source cells in five regions (85 V→Q,24 R→M), strict reversible ledger, source revision59 and exact digest; D161/B158/H0 unchanged. VOID2258/internal803/boundary1455; padding19800 separately retained.
- DONE: 26 prior estimates reviewed separately, no geographical edits/promotions overall; full national priority index and219 unresolved VOID components. No blanket fill, original navigation claims or invented cache fix.
- DONE: model/coordinate/gameplay/water checks locally pass, including all9 scenarios, old58 geography preservation, city topology, dock-only W/SEA access, Difficult March prerequisite, Q exclusion, actual CampaignAi transit and1,418 port/replay assertions. Updated invalid pre-seven-cell test fixtures after reproducing the same failure on untouched058.
- DONE (v060 rechecked final v059 addendum): source b67c14718ce3, installed/published run35523920466, APK9b3bb71444b50add045c8b8113c013f4ab7bc6aeaa0492c7e1e9be8c9b5672a8; details in docs/validation/v059/final-publication.json.
- TODO:2258 remaining source VOID, unresolved prior sand/ground/navigation evidence; ARM physical devices; full historical CI outside directed lane.
- BLOCKER: local direct network and Android SDK unavailable; using connected GitHub Actions for actual commit/push/build/install, not deferring execution.

---

# v0.58 — reference terrain / display-hit consistency

- DONE: parent main29c18ec verified; original image actual hash/size and regional crops checked; fresh unchanged v057 Android baseline captured.
- DONE: 89 unique reviewed map corrections (72 visible /17 estimated), explicit blocked-water identity, no inferred boat passages, resource revision58 and exact digest pins; 19800 padding kept separate.
- DONE: shared projected minimap raster and inverse, reject VOID/padding before label fallback; city atlas56 and source grid retained.
- DONE: local inherited map/city/gameplay checks and new model/projection/guard checks passed; actual v057 saves match independently compiled old encoder on same JVM.
- DONE (final verified addendum): source82749b539bb9 built/installed in35518640828; standalone APK hash93e4f9b1ff0f32f1d336a98623ba2e2bc12e33f514a0465d3ad33325b9ff1c9f published in35520413843. PR46 merged as894fb377; historical all-CI/ARM coverage not implied.
- TODO: remaining 888 internal source VOID and1455 boundary-connected VOID need further reference review; prior nine NW sand estimates remain unconfirmed; ARM physical device coverage.
- BLOCKER: local network/Android SDK absent; using authenticated GitHub connector + Actions for real source push and installed build, not replacing work with a plan.

---

# v0.57 ROAD tap, void triage and terrain presentation

Branch `agent/map-tap-void-terrain-fix-v057`; base main `7e3d88e5d91f3e398d843d6f8f44c7cab23ea1b5`.
Previous progress is preserved in `docs/history/progress-through-v056.md`. No v055/map/art rollback.

## DONE
- Run35512916565 on819dcb43c7a7 passed real old-save ROAD details, coalition Luoyang ROAD taps, all available terrain details, three ROAD zoom levels, and 92 installed checks for new CityAtlas/seven-cell touch, actual sortie/transit/entry/attack/next-turn/minimap/saves. The full new map lane stopped at its injected pinch assertion; later fixtures were not claimed as passed.
- Preserve revision56 native200 march budgets by checking map identity/layout rather than the current terrain revision. Model regression passes; follow-up Android also checks the real baseline save budget.
- Actual Android run35512506691 reproduced the baseline ROAD fatal through normal MapView touch. Its initial v057 candidate exposed a startup atlas/map revision coupling; CityArtCatalog.ASSET_REVISION56 now validates the unchanged art independently of terrain revision57. First candidate is rejected, not delivered as tested. Follow-up installed status remains evidence-driven.
- Replace the ROAD ordinal-name crash with an exhaustive, shared TerrainPresentation used by details and legend. All 14 terrain codes and display definitions have targeted coverage; enum ordering and SaveCodec31 unchanged.
- Verify actual parser alphabet: D = MOUNTAIN_PATH (161), H = DAM (0), R = ROAD (7315). Both national openings contain zero natural DAM entities, not 161. No real wall/dam blanket deletion.
- Separate flat ROAD rendering from narrow mountain trails and elevated plank decks. Use one ground plan in near view and overview; a real DAM entity owns its physical model, avoiding duplicate terrain/model bodies. Neutral entity headers/icons use their own owner, not the player's.
- Explicit opening-only, idempotent natural-dam seeding. Saves restore entity state; destroyed dams never regenerate on loading. Source/axial coordinate conversion is shared, not replaced.
- Change only 11 isolated VOID cells with all six neighbors of one terrain: 9 to SAND and 2 to MOUNTAIN, each ESTIMATED, not image-confirmed. New revision57; all sites, 591 plots and 30 city PNG hashes retained. Independent audit tool verifies pre/post hashes and connectivity.
- Accept revision56 saves without silently transplanting terrain or moving units; visible old-map notice explains that the 11 data edits require a new game. Saved files are not deleted; old political/army state remains intact.
- Local targeted regressions passed: terrain detail/code/save checks, 1986739 projection/connection checks, native200 formal paths and seven-cell city movement/garrison/combat/AI regressions. All 50 pinned map/art resources verified.

## IN PROGRESS
- Geographic acceptance: 955 interior VOID cells remain unresolved after the 11 small estimated corrections. Existing road/plank digitization density is not certified against the unavailable original image.
- Remaining installed samples (second national scenario, all corner/crop edges, bare/neutral DAM and walls) are enforced by the final targeted workflow. Pinch uses paced events and logs device minimum span and actual before/after scale; raw logs plus explicit phase markers prevent old baseline FATAL records being attributed to the new APK.

## TODO
- Review remaining interior VOID clusters, road/plank over-detection and original outer silhouette against the actual MAP_SAN11 reference, region by region. Do not rerun materialize_native56.py.
- Physical ARM device testing and full historical regressions are not claimed. The emergency lane runs only the targeted map/city regressions plus real installed touch/sortie/turn/save paths.

## BLOCKER
- Current shell has no GitHub DNS or Android SDK; complete baseline source recovered from authenticated Actions source artifact, exact tree verified. GitHub connector/Actions provide source push, build and emulator verification.
- MAP_SAN11 attachment is not mounted in this session. Its recorded 7200x6752/hash is NOT independently verified here; no geographic edit is labelled CONFIRMED_VISIBLE.
- See `docs/validation/v057/README.md`, `source-grid-audit.json`, and `data/map/reference-v057/terrain-corrections.json` for diagnosis, coordinates, provenance and unresolved work.

## Exact-build evidence
`.github/workflows/map57-delivery.yml` rebuilds the final source commit and reruns both installed lanes. BUILD_COMMIT, APK_IDENTITY.json, VALIDATION_STATUS.json, ANDROID_STATUS.json, complete raw logs and phase-bounded logs are authoritative. No prior APK or screenshot is used as proof for a new source commit.
