package game.sanguo.core;

import java.util.HashSet;
import java.util.Random;

/** Exhaustive 200x200 coordinate contract; this does NOT certify terrain migration. */
public final class MapCoordinateTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        final int size=200;
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            SourceGridCoord s=new SourceGridCoord(x,y);
            Hex h=MapCoordinates.toAxial(s,size);
            check(s.equals(MapCoordinates.toSource(h,size)),"roundtrip "+s);
            check(s.inBounds(size,size),"bounds "+s);
            check(h.equals(MapCoordinates.axial(x,y,size)),"legacy forward "+s);
            Hex old=MapCoordinates.source(h,size);
            check(old.q==x&&old.r==y,"legacy inverse "+s);
            HashSet<SourceGridCoord> neighbors=new HashSet<>();
            int parity=y&1;
            int[][] expected={{1,0},{parity,-1},{parity-1,-1},{-1,0},{parity-1,1},{parity,1}};
            int direction=0;
            for(Hex n:h.neighbors()){
                SourceGridCoord ns=MapCoordinates.toSource(n,size);
                check(n.equals(MapCoordinates.toAxial(ns,size)),"neighbor roundtrip");
                check(h.distance(n)==1&&n.distance(h)==1,"six-way distance");
                int[] d=expected[direction++];
                check(ns.x==x+d[0]&&ns.y==y+d[1],"direction "+s);
                check(neighbors.add(ns),"unique direction");
            }
            check(neighbors.size()==6,"six directions, not eight");
        }
        Random random=new Random(56);
        for(int i=0;i<4096;i++){
            SourceGridCoord a=new SourceGridCoord(random.nextInt(size),random.nextInt(size));
            SourceGridCoord b=new SourceGridCoord(random.nextInt(size),random.nextInt(size));
            Hex ah=MapCoordinates.toAxial(a,size),bh=MapCoordinates.toAxial(b,size);
            int dq=ah.q-bh.q,dr=ah.r-bh.r;
            int expected=Math.max(Math.max(Math.abs(dq),Math.abs(dr)),Math.abs(dq+dr));
            check(ah.distance(bh)==expected,"cube distance");
            check(ah.distance(bh)==bh.distance(ah),"distance symmetry");
        }
        for(SourceGridCoord s:new SourceGridCoord[]{new SourceGridCoord(-1,0),new SourceGridCoord(0,-1),new SourceGridCoord(size,0),new SourceGridCoord(0,size)}){
            check(!s.inBounds(size,size),"reject out of bounds");
            try{s.requireWithin(size,size);throw new AssertionError("missing bounds exception");}catch(IllegalArgumentException expected){checks++;}
        }
        for(int rows:new int[]{1,2,99,100,199,200})for(int y=0;y<rows;y++){
            SourceGridCoord s=new SourceGridCoord(0,y);
            check(s.equals(MapCoordinates.toSource(MapCoordinates.toAxial(s,rows),rows)),"row parity "+rows);
        }
        System.out.println("MapCoordinateTest PASS: "+checks+" assertions; 40000 source slots; odd-r six-direction topology. Terrain/map revision NOT certified.");
    }
}
