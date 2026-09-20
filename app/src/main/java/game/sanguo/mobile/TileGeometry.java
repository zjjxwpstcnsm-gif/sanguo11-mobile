package game.sanguo.mobile;

/** Rectangular, staggered cells with SIX neighbors, not an eight-way square grid.
 * Existing odd-r scenario topology and axial coordinates remain authoritative.
 * The reference uses staggered columns; this conservative presentation correction
 * retains the shipped row-oriented indexing rather than rotating/shearing geography.
 */
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
    static int start(int direction){return (6-direction)%6;}
    static int end(int direction){return (start(direction)+1)%6;}
    static float edgeX(int direction){return RADIUS*(CORNER_X[start(direction)]+CORNER_X[end(direction)])*.5f;}
    static float edgeY(int direction){return RADIUS*(CORNER_Y[start(direction)]+CORNER_Y[end(direction)])*.5f;}
}
