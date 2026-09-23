package game.sanguo.api;

/** Storage (q,r) to dimensionless presentation X/Z. Native source grids transpose axes.
 * Rendering can interpolate a continuous surface; this does not define movement costs. */
public final class GridLayout {
    public final boolean columnStaggered;
    public final double offset;
    public final int sourceOriginX, sourceOriginY;
    public GridLayout(boolean columnStaggered,double offset,int sourceOriginX,int sourceOriginY){
        this.columnStaggered=columnStaggered;this.offset=offset;
        this.sourceOriginX=sourceOriginX;this.sourceOriginY=sourceOriginY;
    }
    public double x(int q,int r){return columnStaggered?r:q+r*.5-offset;}
    public double z(int q,int r){return columnStaggered?q+r*.5-offset:r;}
    public int r(double x,double z){return (int)Math.floor((columnStaggered?x:z)+.5);}
    public int q(double x,double z){int r=r(x,z);return (int)Math.floor((columnStaggered?z:x)-r*.5+offset+.5);}
}
