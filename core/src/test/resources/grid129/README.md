# Pre-gate save compatibility fixture

`pre-gate-plank.sg11` was encoded from the actual v128 production core at
`f11ad0bff299dca21d10d1209d14779905fe1978`, before the v129 rule edit.
SHA-256: `023b2bb02bc7dce50c6d945e6503755345a964dbc9f82fb7752b44e6237cdd06`.

Input is `Realm52Fixture.criticalWorld(false)` with unit 2 moved to (17,10),
PLANK_ROAD at (8,8)/(9,8), and unit 1 on (8,8) carrying an existing MOVE order
to (9,8), without DIFFICULT_MARCH. The old producer explicitly checked that
both `landCost` and `previewMove` allowed entry before encoding. This is an
explicit test map, not modified scenario data.

The new test requires exact decode/re-encode bytes, no position/terrain/research
migration, blocked onward entry and queued order after loading, and legal
exit onto ordinary ground. Research subsequently reopens the destination.
