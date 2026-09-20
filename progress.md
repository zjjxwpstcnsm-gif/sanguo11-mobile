# v0.56 native200 + city atlas — IN PROGRESS

Branch `agent/native-map-200-city-art-v056`, draft PR #44; base main bb8e58ebe32e96ddf453ee68fd23e21509f71247. Previous complete history is retained in docs/history/progress-through-v055.md. The Actions materialization commit contains the real source/data/PNG files; BUILD_COMMIT in its artifact is the exact APK source.

## DONE
- Native200 shared source map, seven nationwide scenarios plus two explicit crops; 87 sites, 591 national plots, 42 real seven-cell cities. Story fields preserved.
- Column-staggered source/axial/Android projection, source bounds/camera/culling, map revision and SaveCodec31 old-save rejection.
- CityArtCatalog/CityAtlas integrated in actual MapView at all LODs, army layer above cities; 30 real RGBA city/gate/port PNGs with pivots and alpha/SHA audits.
- Full typed odd-r checkpoint API/tests retained after recovering the 48 complete source files from the previously truncated transport.
- Local validation: native map contracts 1,230,589 assertions; legacy coordinate checkpoint 1,168,801; exact native Android projection 400,101 (353 near-view culling candidates); v055 formal footprint/move/entry/combat/AI/save 11,070; legacy UI projection 49,017, terrain connections 451, geometry 1,136,811. Counts include parameterized iterations, not distinct user workflows.
- Actual old/new march benchmarks documented for Guan-Luo, Jing-Xiang and Shu road; turns 4/4, 3/3, 8/7.

## IN PROGRESS
- Actions compilation, installed Native56Probe, screenshots, source/signature/APK identity. No success is claimed until the returned run/artifact is inspected.
- Nationwide reference review: current records distinguish calibrated anchors from estimated gate/port locations.

## TODO
- Complete manual per-cell terrain review, including noisy road/mountain/forest classification and small shoal/plank/dam features.
- Further port/ship route benchmarks, physical ARM-device performance and final quality acceptance.
- Reconcile historical all-core assertions with current rules. ContentIntegrationTest failure was reproduced on unmodified main too; full suite is not reported green.

## BLOCKER / limits
- Shell GitHub DNS and local Android SDK unavailable; authenticated connector plus Actions used. Main is never force-pushed.
- Original compressed transport lacked its tail. Only the full-SHA-verified first 48 sections were applied; missing original generator/calibration was not fabricated. New evidence describes its uncertainty explicitly.
- This is a real native-map/art build checkpoint, not certification of perfect geographic fidelity or completed Android runtime validation.
