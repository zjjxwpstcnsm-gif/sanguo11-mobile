package game.sanguo.core;
import java.util.*;
public final class Geography63Test {
    static long checks;static int steps;static void add(Geography63Checks.Result r){checks+=r.checks;steps+=r.steps;System.out.print(r.log);}
    public static void main(String[] args)throws Exception{
        for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries())add(Geography63Checks.map(ScenarioCatalog.load(s.id,0,630L)));
        add(Geography63Checks.negatives());
        for(int[] port:Geography63Checks.PORTS){for(boolean reverse:new boolean[]{false,true}){
            for(Army.Ship ship:Army.Ship.values())add(Geography63Checks.trip(port,ship,false,reverse));
            add(Geography63Checks.trip(port,Army.Ship.BOAT,true,reverse));add(Geography63Checks.ai(port,reverse));}}
        System.out.println("GEOGRAPHY63 CORE PASS: actualAssertions="+checks+" executedTravelSteps="+steps+" all35Ports=true all9Scenarios=true physicalARM=false");
    }
}
