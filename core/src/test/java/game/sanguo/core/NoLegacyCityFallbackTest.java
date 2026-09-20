package game.sanguo.core;
import static game.sanguo.core.Native56Checks.*;
public final class NoLegacyCityFallbackTest {
 public static void main(String[] args)throws Exception {
  for(World.City c:world().cities){String key=CityArtCatalog.key(c);check(!key.contains("legacy")&&!key.equals("buildings"),"new atlas only");}
  World.City imported=new World.City(1,"自制城",new Hex(3,3),0);check(CityArtCatalog.key(imported).equals("standard"),"even custom maps use new art");pass("NoLegacyCityFallbackTest");
 }
}
