package game.sanguo.mobile;

/** Six-neighbor staggered source projection. Native odd-q source cells use
 * transposed axial storage; custom odd-r/axial maps retain their legacy projection.
 * Screen X/Y must never be confused with storage q/r or source columns/rows. */
final class TileGeometry {
    static final float RADIUS=25f, SPAN=1.6f, DX=RADIUS*SPAN, DY=RADIUS*SPAN;
    // Collinear vertices split the top/bottom edges between their two neighbors.
    static final float[] CORNER_X={.8f,.8f,0,-.8f,-.8f,0};
    static final float[] CORNER_Y={-.8f,.8f,.8f,.8f,-.8f,-.8f};
    private TileGeometry() {}
    static float x(int q,int r,float offset){return DX*(q+r*.5f-offset);}
    static float y(int r){return DY*r;}
    static int row(float y){return (int)Math.floor(y/DY+.5f);}
    static int column(float x,int row,float offset){return (int)Math.floor(x/DX-row*.5f+offset+.5f);}
    static float projectedX(int q,int r,float offset,boolean columnStaggered){return columnStaggered?y(r):x(q,r,offset);}
    static float projectedY(int q,int r,float offset,boolean columnStaggered){return columnStaggered?x(q,r,offset):y(r);}
    static int projectedRow(float wx,float wy,boolean columnStaggered){return row(columnStaggered?wx:wy);}
    static int projectedColumn(float wx,float wy,int r,float offset,boolean columnStaggered){return column(columnStaggered?wy:wx,r,offset);}
    static int start(int direction){return (6-direction)%6;}
    static int end(int direction){return (start(direction)+1)%6;}
    static float edgeX(int direction){return RADIUS*(CORNER_X[start(direction)]+CORNER_X[end(direction)])*.5f;}
    static float edgeY(int direction){return RADIUS*(CORNER_Y[start(direction)]+CORNER_Y[end(direction)])*.5f;}
}
