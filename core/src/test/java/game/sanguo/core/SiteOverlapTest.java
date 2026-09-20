package game.sanguo.core;
import java.util.*;
import static game.sanguo.core.Native56Checks.*;
public final class SiteOverlapTest {
 public static void main(String[] args)throws Exception {
  World w=world();Set<Hex> all=new HashSet<>();int plots=0;
  for(World.City c:w.cities)for(Hex h:SiteFootprint.cells(c))check(all.add(h),"no site overlap");
  for(World.City c:w.cities)for(Hex h:w.development.parcels(c.id)){check(all.add(h),"no parcel/body overlap");check(w.terrain[h.q][h.r]==World.Terrain.PLAIN,"no road/water plot replacement");plots++;}
  check(plots==591,"591, never 4x parcels");pass("SiteOverlapTest");
 }
}
