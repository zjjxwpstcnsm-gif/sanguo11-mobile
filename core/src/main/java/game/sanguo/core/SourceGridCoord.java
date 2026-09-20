package game.sanguo.core;

/** Source-grid coordinates, deliberately distinct from axial Hex and screen pixels. */
public final class SourceGridCoord {
    public final int x, y;
    public SourceGridCoord(int x, int y) { this.x=x; this.y=y; }
    public boolean inBounds(int columns, int rows) {
        return columns>0 && rows>0 && x>=0 && y>=0 && x<columns && y<rows;
    }
    public SourceGridCoord requireWithin(int columns, int rows) {
        if(!inBounds(columns, rows)) throw new IllegalArgumentException("源格坐标越界: "+this);
        return this;
    }
    @Override public boolean equals(Object other) {
        return other instanceof SourceGridCoord && ((SourceGridCoord)other).x==x && ((SourceGridCoord)other).y==y;
    }
    @Override public int hashCode() { return 31*x+y; }
    @Override public String toString() { return "source("+x+","+y+")"; }
}
