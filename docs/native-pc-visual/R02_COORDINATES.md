# R02 coordinates and gestures

The rule coordinate contract remains GridLayout. Source (national x,y) is converted
by MapCoordinates.fromNationalSource/nationalSource, never by the camera. Storage
is terrain[q][r]; immutable presentation terrain uses index r*width+q. For the
column-staggered national map world X=r, Z=q+r/2-offset; otherwise X=q+r/2-offset,
Z=r. Source-origin metadata is not added again. Hex adjacency is unchanged.
World Y is presentation height only. Units are existing dimensionless map units.

All camera projection, Android overlay and MotionEvent values use local view
pixels, top-left origin. Filament orthographic projection uses the same view aspect
and camera basis. The Surface buffer uses 0.70/0.85/1.00 quality scale, independently
of UI pixels. No input multiplication by buffer scale. Width/height changes retain
world focus, selection, span, tilt and yaw; the view aspect changes naturally.

Default: span 35 (restoration default 15), tilt 55 degrees, azimuth 0, orthographic.
This retains the previous project strategy view; REFERENCE_MISSING, not a claim
about KOEI's FOV. Tilt clamps 40–70, span 3–160. The legacy facing flag multiplies
the horizontal basis by -1 for the existing instant 180-degree shortcut. Yaw is
continuous modulo 360. Reset restores yaw=0, facing=1, tilt=55.

- Single finger drag: pan. Confirmed single tap: existing select/command route.
- Two fingers: centroid pan, pinch anchored to the visible terrain/water height,
  twist for continuous rotation. Releasing a finger cannot issue a command.
- Three fingers: vertical centroid movement adjusts bounded tilt (0.12 degrees/px).
- Double tap: 1.7x height-anchored zoom. Long press: centre the hit cell with haptic
  feedback, never call the potentially armed gameplay-command tap handler.
- Panel/right/bottom occlusion and visible/system-inset bounds reject input.
  Crossing into excluded UI cancels the whole gesture, including an editor stroke.

The ray is parameterized by world Y using the same camera basis as lookAt. It
intersects every candidate eight-triangle fan within the bounded height range
0..2.6 and returns the largest Y (nearest for the 40–70 degree downward camera).
VOID does not make an actionable hit. Water uses its actual Y=0 fan. Near LOD is
coplanar subdivision of the same far fan: geometric LOD picking error is zero,
with numerical tolerance 0.001 world units in the independent mesh oracle.
Height anchoring is checked within 0.01 view pixel (float world coordinates).

Model selection uses current visible mesh, motion position, yaw and animated
scale, rejecting a hit behind foreground terrain. Label rectangles assist entity
selection and are terrain-occlusion filtered; no occupied or reachable cell is
added. Selection resolves to stable entity's authoritative hex, including a moving
unit. Armed move/attack/reachable modes bypass object/label assistance and choose
the actual ground cell, preserving existing command semantics. Seven city cells
remain SiteFootprint's exact authority. Decorations/vegetation are noninteractive;
they do not extend range or occupancy. Labels are readable overlays, not geometry.

Camera settings live in view Bundle and map-renderer preferences, not SaveCodec.
3D -> 2D -> 3D retains native span/tilt/yaw/facing; 2D panning can update the shared
focus. Activity recreation while 2D also retains native parameters in its Bundle.
Diagnostics remain off by default; when enabled they expose view/buffer sizes,
world/grid hit, terrain/water/entity/label, height and camera yaw/tilt/span.

National golden centres: 38,949 valid cells, FNV-style q/r/float-X/float-Z cohort
hash 15475946728566902866. Independently recomputed from fa78324 baseline, not a
changed map expectation. Boundary samples cover negative coordinates, positive
half-tie convention and the transposed national origin. 3x4x3 matrix compares
production ray intersections against separately projected actual mesh triangles.
