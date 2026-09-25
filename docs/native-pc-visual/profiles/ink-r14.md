# ink-landscape-r14-v1

Production parameters: `SeasonStyle`, `EnvironmentProfile`, four `tools/3d/*.mat`
materials. Rebuild with `MATC=<official v1.56.0 matc> bash tools/3d/build_season_materials.sh`.
These are project art decisions, not a reconstruction of proprietary KOEI shaders.

R09 V2 review found weak relief, hard stepped shores, sparse scenery and coarse
architecture. This stage cannot fix those silhouettes with a filter. The profile
keeps muted green/ochre ground, cool blue-green water, pale warm stone, matte rough
surfaces and softer directional/ambient contrast. Assets remain transitional.

The normal immutable map snapshot copies the authoritative month using the same
startMonth + turn/3 arithmetic as World.date. January–March is spring, matching the
known January spring icon in PC-MANUAL-07; other seasonal reference/camera matching
is UNKNOWN. No invented original seasonal palette is asserted. Four static profiles
avoid allocations and repeated light construction. Same-quarter snapshots do not
change any season uniform; date changes never enter Ground identity or mesh keys.
Scenario activation, restore and session changes all use that normal snapshot path.

Ground grass weights, shoreline grass weights, green foliage/crop pixels and water
pigment respond separately. Stone/wood/skin/faction markings keep their identity.
All existing albedos retain sRGB decoding, normals/data remain linear; constants
are linear RGB. Existing framebuffer sRGB/compatibility encoding is unchanged.
Unit/site roughness remains .88, ground keeps its authored normal/roughness maps.
No fullscreen grading, outline pass, UI recoloring or duplicate textures are added.

Spring/summer/autumn/winter sun: 47k/50k/45k/42k lux; fill: 17k/18k/17k/18k lux.
Fixed exposure remains 11, 1/125, ISO100. Direction remains (-1,-2,-1).
Stable one-cascade shadows retain 512 standard /1024 high maps and GLES3.1 capability
gating. shadowFar is corrected from28 to380 for the existing300-unit camera offset.
LOW/GLES3.0 retains exact terrain/contact anchoring and matte material response;
no claim of dynamic contact shadows there. Shadow acne/peter-panning and temporal
stability require actual device review; a corrected range is not that acceptance.

Aerial pigment uses existing opaque surface fragments only: camera distance320–410,
maximum8% gray-green blend. Main local operating area (~300 distance) is clear.
No clouds, transparent fog sheets, added draw calls or added sampler reads. Additional
ALU is one distance/smoothstep/blend per fragment. Transparent overdraw added=0 by
construction, not a measured total GPU overdraw claim. LOW retains identical style.

Winter is dormant/cool, never universal snow or frozen water: available references
do not establish geographic snow coverage. Navigability and terrain types never
change. Display-only repeated-season test snapshots never write a date to GameSession.
Full saved-date fixtures are explicitly labelled test fixtures; not claimed to be
months of actually played turns. A separate normal turn crosses March→April.

Screenshot baseline: coalition-190/player5, centre(134,82), spans5/12/22, yaw0/90,
tilt48, grid off/on, all four seasons; LOW/HIGH middle span supplements MEDIUM.
Baseline assets, architecture and core remain inherited. PC similarity and physical
Adreno/Mali/thermal acceptance remain open, and no R15 expansion is authorized.
