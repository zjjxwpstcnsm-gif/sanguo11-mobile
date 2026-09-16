package game.sanguo.core;
import java.util.*;
import java.lang.reflect.*;
/** Same fixture and Java source compiled against b75e249 and v027; desktop JVM, not ARM. */
public final class LogisticsPerformance {
 static World large()throws Exception {
  World w=new World(200,200,"甲","乙");w.strategy.setSeed(27016);
  for(int i=0;i<40;i++){int owner=i<20?0:1;Hex h=new Hex(10+i%8*24,10+i/8*40);World.City c=new World.City(i,"城"+i,h,owner);c.gold=30000;c.food=180000;c.troops=40000;w.cities.add(c);World.Officer o=new World.Officer(i,"将"+i,owner,-1,80,80,70,70,70);World.Unit u=new World.Unit(w.nextUnitId++,owner,i,World.Weapon.SPEAR,new Hex(h.q+2,h.r),6000,60000);o.unitId=u.id;w.officers.add(o);w.units.add(u);}
  for(int i=0;i<4;i++){World.Officer o=new World.Officer(100+i,"运"+i,0,i,80,80,70,70,70);w.officers.add(o);if(!w.domestic.transport(i,i+1,o.id,300,10000,1000,new int[4]).ok)throw new AssertionError("dispatch");try{w.domestic.missions.get(i).getClass().getField("escortId").setInt(w.domestic.missions.get(i),i+1);}catch(NoSuchFieldException oldVersion){/* old version has the same available field armies, no escort intent */}}
  w.strategy.initializeOffices();SaveCodec.validate(w);return w;
 }
 static void measure(String name,World original)throws Exception {
  byte[] state=SaveCodec.encode(original);double[] turns=new double[4];for(int i=0;i<4;i++){World w=SaveCodec.decode(state);long t=System.nanoTime();if(!w.nextTurn().ok)throw new AssertionError();turns[i]=(System.nanoTime()-t)/1e6;SaveCodec.validate(w);}
  World w=SaveCodec.decode(state);CampaignAi ai=new CampaignAi(w);long t=System.nanoTime();for(World.Unit u:w.units)ai.bestAction(u.id,true);double score=(System.nanoTime()-t)/1e6;
  double path=0;if(!w.units.isEmpty()){Method routes=CampaignAi.class.getDeclaredMethod("routes",World.Unit.class,Collection.class,int.class);routes.setAccessible(true);List<Hex> goals=new ArrayList<>();for(World.City c:w.cities)if(c.owner==1)goals.add(c.hex);t=System.nanoTime();routes.invoke(ai,w.units.get(0),goals,1);path=(System.nanoTime()-t)/1e6;}
  t=System.nanoTime();CityOverview ui=new CityOverview(w);int rows=0;for(int i=0;i<9;i++)rows+=ui.cities(i,-1,6,"",-1).size();double lists=(System.nanoTime()-t)/1e6;
  System.out.printf(Locale.ROOT,"%s: warmup %.2f ms; real turns %.2f / %.2f / %.2f ms; separate AI action scoring %.2f ms; separate multi-goal path %.2f ms (%d searches/%d cells); nine UI filters %.2f ms (%d rows)%n",name,turns[0],turns[1],turns[2],turns[3],score,path,ai.routeSearches,ai.routeExpanded,lists,rows);
 }
 public static void main(String[] args)throws Exception {measure("42 cities / 670 officers",ScenarioCatalog.load("heroes-mobile-sandbox",0,27016));measure("synthetic 200x200 / 40 cities / 40 armies / 4 transports / 4 available guards",large());}
}
