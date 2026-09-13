package game.sanguo.core;

import java.util.ArrayList;
import java.util.List;

/** Axial coordinates; rendering never defines game adjacency. */
public final class Hex {
    public final int q, r;
    private static final int[][] DIRECTIONS = {{1,0},{1,-1},{0,-1},{-1,0},{-1,1},{0,1}};
    public Hex(int q, int r) { this.q=q; this.r=r; }
    public int distance(Hex b) { return (Math.abs(q-b.q)+Math.abs(r-b.r)+Math.abs(q+r-b.q-b.r))/2; }
    public List<Hex> neighbors() {
        List<Hex> result=new ArrayList<>();
        for (int[] d:DIRECTIONS) result.add(new Hex(q+d[0],r+d[1]));
        return result;
    }
    @Override public boolean equals(Object o) { return o instanceof Hex && ((Hex)o).q==q && ((Hex)o).r==r; }
    @Override public int hashCode() { return 1009*q+r; }
    @Override public String toString() { return q+","+r; }
}
