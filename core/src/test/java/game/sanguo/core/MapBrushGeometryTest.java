package game.sanguo.core;

import java.util.*;

/** Production-grid regression; runs without Android and without modifying shipped maps. */
public final class MapBrushGeometryTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Random random=new Random(670058L);
        for(int iteration=0;iteration<12000;iteration++){
            Hex a=new Hex(random.nextInt(600)-200,random.nextInt(600)-200);
            Hex b=new Hex(random.nextInt(600)-200,random.nextInt(600)-200);
            List<Hex> line=MapBrushGeometry.line(a,b),reverse=MapBrushGeometry.line(b,a);
            Collections.reverse(reverse);
            check(line.equals(reverse),"direction-independent tie-break");
            check(line.size()==a.distance(b)+1,"complete shortest line");
            check(line.get(0).equals(a)&&line.get(line.size()-1).equals(b),"endpoints");
            check(new HashSet<>(line).size()==line.size(),"no duplicated cells");
            for(int i=1;i<line.size();i++)check(line.get(i-1).neighbors().contains(line.get(i)),"no sparse-touch gap");
        }
        Hex center=new Hex(80,80);
        for(int r=0;r<=8;r++){
            Set<Hex> disk=MapBrushGeometry.disk(center,r);
            check(disk.size()==1+3*r*(r+1),"axial brush area");
            for(Hex h:disk)check(center.distance(h)<=r,"real neighbour radius");
        }
        World w=ScenarioCatalog.load("heroes-250",0);
        Hex a=MapCoordinates.fromNationalSource(w,new SourceGridCoord(0,0));
        Hex b=MapCoordinates.fromNationalSource(w,new SourceGridCoord(199,199));
        for(Hex h:MapBrushGeometry.stroke(w,a,b,2))check(w.sourceInside(h),"no axial padding/outside painting");
        check(MapBrushGeometry.stroke(w,new Hex(-1,-1),b,0).isEmpty(),"out-of-map endpoint");
        check(MapBrushGeometry.line(a,a).equals(Collections.singletonList(a)),"single tap");
        System.out.println("PASS "+checks+" brush geometry assertions");
    }
}
