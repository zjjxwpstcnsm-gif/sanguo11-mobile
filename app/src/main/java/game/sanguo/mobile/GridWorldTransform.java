package game.sanguo.mobile;

import game.sanguo.core.Hex;

/** One world unit per projected tile span. Z grows along the old screen Y axis.
 * Exact shared edges choose the positive row/column via floor(value + 0.5). */
final class GridWorldTransform {
    final float offset;
    final boolean staggered;
    GridWorldTransform(float offset, boolean staggered) {
        this.offset=offset; this.staggered=staggered;
    }
    float x(Hex h) { return TileGeometry.projectedX(h.q,h.r,offset,staggered)/TileGeometry.DX; }
    float z(Hex h) { return TileGeometry.projectedY(h.q,h.r,offset,staggered)/TileGeometry.DY; }
    Hex cell(float x,float z) {
        int r=TileGeometry.projectedRow(x*TileGeometry.DX,z*TileGeometry.DY,staggered);
        return new Hex(TileGeometry.projectedColumn(x*TileGeometry.DX,z*TileGeometry.DY,r,offset,staggered),r);
    }
}
