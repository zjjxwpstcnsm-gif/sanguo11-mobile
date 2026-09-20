# v0.57 ROAD tap, void triage and terrain presentation

Branch `agent/map-tap-void-terrain-fix-v057`; base main `7e3d88e5d91f3e398d843d6f8f44c7cab23ea1b5`.
Previous progress is preserved in `docs/history/progress-through-v056.md`. No v055/map/art rollback.

## DONE
- Replace the ROAD ordinal-name crash with an exhaustive, shared TerrainPresentation used by details and legend. All 14 terrain codes and display definitions have targeted coverage; enum ordering and SaveCodec31 unchanged.
- Verify actual parser alphabet: D = MOUNTAIN_PATH (161), H = DAM (0), R = ROAD (7315). Both national openings contain zero natural DAM entities, not 161. No real wall/dam blanket deletion.
- Separate flat ROAD rendering from narrow mountain trails and elevated plank decks. Use one ground plan in near view and overview; a real DAM entity owns its physical model, avoiding duplicate terrain/model bodies. Neutral entity headers/icons use their own owner, not the player's.
- Explicit opening-only, idempotent natural-dam seeding. Saves restore entity state; destroyed dams never regenerate on loading. Source/axial coordinate conversion is shared, not replaced.
- Change only 11 isolated VOID cells with all six neighbors of one terrain: 9 to SAND and 2 to MOUNTAIN, each ESTIMATED, not image-confirmed. New revision57; all sites, 591 plots and 30 city PNG hashes retained. Independent audit tool verifies pre/post hashes and connectivity.
- Accept revision56 saves without silently transplanting terrain or moving units; visible old-map notice explains that the 11 data edits require a new game. Saved files are not deleted; old political/army state remains intact.
- Local targeted regressions passed: 671998 detail/code/save checks, 1986739 projection/connection checks, native200 formal paths and seven-cell city movement/garrison/combat/AI regressions. All 50 pinned map/art resources verified.

## IN PROGRESS
- Geographic acceptance: 955 interior VOID cells remain unresolved after the 11 small estimated corrections. Existing road/plank digitization density is not certified against the unavailable original image.
- Exact-commit Android delivery is evaluated by `.github/workflows/map57-delivery.yml`. Its BUILD_COMMIT, APK_IDENTITY.json, VALIDATION_STATUS.json, before/after logcat and screenshots are authoritative; this source document does not predeclare a future CI result.

## TODO
- Review remaining interior VOID clusters, road/plank over-detection and original outer silhouette against the actual MAP_SAN11 reference, region by region. Do not rerun materialize_native56.py.
- Physical ARM device testing and full historical regressions are not claimed. The emergency lane runs only the targeted map/city regressions plus real installed touch/sortie/turn/save paths.

## BLOCKER
- Current shell has no GitHub DNS or Android SDK; complete baseline source recovered from authenticated Actions source artifact, exact tree verified. GitHub connector/Actions provide source push, build and emulator verification.
- MAP_SAN11 attachment is not mounted in this session. Its recorded 7200x6752/hash is NOT independently verified here; no geographic edit is labelled CONFIRMED_VISIBLE.
- See `docs/validation/v057/README.md`, `source-grid-audit.json`, and `data/map/reference-v057/terrain-corrections.json` for diagnosis, coordinates, provenance and unresolved work.
