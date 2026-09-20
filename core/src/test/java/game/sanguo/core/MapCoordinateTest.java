package game.sanguo.core;
import java.util.*;
import static game.sanguo.core.Native56Checks.*;
public final class MapCoordinateTest {
 public static void main(String[] args)throws Exception {
  World w=world();Set<Hex> unique=new HashSet<>();
  for(int y=0;y<200;y++)for(int x=0;x<200;x++){
   SourceGridCoord s=new SourceGridCoord(x,y);Hex h=s.toAxial(w);check(unique.add(h),"unique source slot");
   check(MapCoordinates.source(w,h).equals(s),"all source parities roundtrip "+s);
   for(Hex n:h.neighbors()){
    SourceGridCoord ns=MapCoordinates.source(w,n);check(MapCoordinates.axial(w,ns).equals(n),"neighbor outside/inside roundtrip");
    check(h.distance(n)==1&&n.distance(h)==1,"six neighbors distance");
    check(n.neighbors().contains(h),"symmetric adjacency");
   }
  }
  Random random=new Random(56);for(int i=0;i<16384;i++){
   int x=random.nextInt(200),y=random.nextInt(200);Hex h=MapCoordinates.axial(w,new SourceGridCoord(x,y));
   for(int d=0;d<6;d++){Hex n=h.neighbors().get(d);check(n.neighbors().get((d+3)%6).equals(h),"opposite direction");}
  }
  check(unique.size()==40000,"40,000 independent slots");pass("MapCoordinateTest");
 }
}
