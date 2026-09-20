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
