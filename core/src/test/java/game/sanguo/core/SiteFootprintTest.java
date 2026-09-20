package game.sanguo.core;
import java.util.*;
import static game.sanguo.core.Native56Checks.*;
public final class SiteFootprintTest {
 public static void main(String[] args)throws Exception {
  World w=world();for(World.City c:w.cities){
   check(SiteFootprint.cells(c).size()==(c.kind==World.SiteKind.CITY?7:1),"exact footprint "+c.name);
   for(Hex h:SiteFootprint.cells(c))check(w.cityAt(h)==c&&w.sourceInside(h),"all rim cells resolve to same city");
   if(c.kind==World.SiteKind.CITY){Map<Integer,Integer> columns=new TreeMap<>();for(Hex h:SiteFootprint.cells(c)){SourceGridCoord s=MapCoordinates.source(w,h);columns.merge(s.x,1,Integer::sum);}check(new ArrayList<>(columns.values()).equals(Arrays.asList(2,3,2)),"native odd-q 2/3/2 equivalent");}
  }pass("SiteFootprintTest");
 }
}
