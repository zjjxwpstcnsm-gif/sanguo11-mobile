package game.sanguo.core;

import java.nio.file.*;

/** Compile against the pre-change full-workspace classes to preserve a genuine v33 construction save. */
public final class PcConstructionSaveProbe {
    public static void main(String[] args)throws Exception{
        World w=new World(24,20);w.cities.add(new World.City(10,"甲",new Hex(4,4),0));w.cities.add(new World.City(20,"乙",new Hex(18,14),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,80,80,80,90,80));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));w.city(10).gold=30000;
        w.strategy.initializeOffices();
        if(!w.domestic.build(10,0,Domestic.Kind.MARKET,w.domestic.buildSites(10).get(0)).ok)throw new AssertionError("build");
        if(!w.nextTurn().ok||!w.nextTurn().ok)throw new AssertionError("turns");
        if(!w.domestic.build(10,1,Domestic.Kind.FARM,w.domestic.buildSites(10).get(0)).ok)throw new AssertionError("farm");
        if(w.domestic.facility(1).level!=3||w.domestic.facility(1).remaining!=0||w.domestic.facility(2).level!=3||w.domestic.facility(2).remaining==0)throw new AssertionError("requires original shortcut writer");
        Files.write(Path.of(args[0]),SaveCodec.encode(w));
        System.out.println("PASS actual pre-change v33 save: completed market and pending farm both Lv3");
    }
}
