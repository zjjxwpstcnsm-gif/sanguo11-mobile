package game.sanguo.mobile;

import game.sanguo.core.Hex;
import game.sanguo.api.GridLayout;

/** One world unit per projected tile span. Z grows along the old screen Y axis.
 * Exact shared edges choose the positive row/column via floor(value + 0.5). */
final class GridWorldTransform {
    private final GridLayout layout;
    final float offset;
    final boolean staggered;
    GridWorldTransform(float offset, boolean staggered) {
        this.offset=offset; this.staggered=staggered;layout=new GridLayout(staggered,offset,0,0);
    }
    float x(Hex h) { return x(h.q,h.r); }
    float x(int q,int r) { return (float)layout.x(q,r); }
    float z(Hex h) { return z(h.q,h.r); }
    float z(int q,int r) { return (float)layout.z(q,r); }
    Hex cell(float x,float z) {
        return new Hex(layout.q(x,z),layout.r(x,z));
    }
}
