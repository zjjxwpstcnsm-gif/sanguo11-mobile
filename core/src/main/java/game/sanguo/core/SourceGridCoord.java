package game.sanguo.core;

/** Source-image grid coordinate, deliberately distinct from an axial Hex. */
public final class SourceGridCoord {
    public final int x, y;
    public SourceGridCoord(int x, int y) { this.x=x; this.y=y; }
    public boolean isInside(int columns, int rows) { return x>=0 && y>=0 && x<columns && y<rows; }
    public boolean inBounds(int columns,int rows){return columns>0&&rows>0&&x>=0&&y>=0&&x<columns&&y<rows;}
    public SourceGridCoord requireWithin(int columns,int rows){if(!inBounds(columns,rows))throw new IllegalArgumentException("源格坐标越界: "+this);return this;}
    public Hex toNativeAxial(int columns) { return MapCoordinates.axialColumn(x,y,columns); }
    public Hex toAxial(World world) { return MapCoordinates.axial(world,this); }
    public static SourceGridCoord fromAxial(World world, Hex hex) { return MapCoordinates.source(world,hex); }
    /** Legacy row-staggered grids only; native maps use toNativeAxial or World. */
    public Hex toAxial(int rows) { return MapCoordinates.axial(x,y,rows); }
    public static SourceGridCoord fromAxial(Hex axial, int rows) {
        Hex source=MapCoordinates.source(axial,rows); return new SourceGridCoord(source.q,source.r);
    }
    @Override public boolean equals(Object o) { return o instanceof SourceGridCoord && ((SourceGridCoord)o).x==x && ((SourceGridCoord)o).y==y; }
    @Override public int hashCode() { return 1009*x+y; }
    @Override public String toString() { return x+","+y; }
}
