package game.sanguo.core;
import static game.sanguo.core.Native56Checks.*;
public final class NationalSiteCountTest {
 public static void main(String[] args)throws Exception {
  World w=world();int city=0,gate=0,port=0;for(World.City c:w.cities){if(c.kind==World.SiteKind.CITY)city++;else if(c.kind==World.SiteKind.GATE)gate++;else port++;}
  check(city==42&&gate==10&&port==35&&w.cities.size()==87,"42+10+35");pass("NationalSiteCountTest");
 }
}
