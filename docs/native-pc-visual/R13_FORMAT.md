# R13 editor, storage and cache contract

The existing MapPatch format is retained: format 1 is a logical geographic delta;
format 2 adds `visual.version=1`, mapId and a logical contentHash. Heights are
integer 0..2600 thousandths of one world cell; site appearance uses bundled variant
0..2 and yaw 0..359. No paths, remote model URLs or unbounded new profiles are added.
Existing LandscapeProfile remains the bundled, versioned regional style. There is
no second editor or new save version. SaveCodec v33 and SessionSaves are unchanged.

Logical fingerprint deliberately includes map UUID/name/revision, preview scenario,
base/scenario fingerprints and logical changes, excluding visual heights/appearance.
Map copy/rename re-encodes the visual binding. Campaign saves embed geography and
pin immutable map identity. Missing styles use defaults; corrupt optional sidecars
try the verified pinned revision, otherwise defaults. They never modify save bytes.
Campaign AtomicFile save completes independently of optional visual sidecar failure.

MapEditSession.persist wraps the existing replace/paint/undo/redo transactions and
AtomicFile draft writer under the same monitor. Failure restores draft, detached
World, history, history byte count, generation and saved fingerprint. Rendering
keeps its preceding immutable snapshot; success alone refreshes the host. The editor
no longer hides the Surface for every save. Worker queues still accept at most one
running and one replaceable waiting mesh job, with epoch rejection of stale results.

Existing continuous fields/chunk fingerprints are reused, including the eight-cell
halo for terrain/material/shore, road/scenery exclusions, site bases and visual
heights. A one-cell water edit is compared against a clean build, including vertices,
indices and surface attributes, and unrelated chunks must retain object identity.
This does not claim every possible editing dependency has been exhaustively proved.
Water geometry is locally derived; authoritative MapWater connectivity is resolved
by the existing custom-map transaction, not by a new renderer flood fill.

Brushes and selection use the displayed TerrainSurface picker; editor single taps
no longer prefer entity model triangles. Source-coordinate labels now use actual
national source coordinates, including crop origin, rather than floored world axes.

Imports retain 4 MiB/depth/node/UTF-8/duplicate-key checks and strict numeric/enumerated
fields. Android editor import/export accepts only system-document content URIs.
Private draft/revision/sidecar files use AtomicFile. External SAF exports use the
chosen document provider: cross-provider atomic replacement is NOT guaranteed;
failed exports cannot mutate the local draft or active campaign. Full provider-level
interruption and manual SAF tests remain open. No broad runtime art pass is claimed.
