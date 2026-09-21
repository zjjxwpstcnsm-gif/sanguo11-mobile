package game.sanguo.core;

/** Isolated model fixture with non-overlapping seven-cell cities. */
public final class Ux64Fixture {
    private Ux64Fixture() {}
    public static World create(){
        World w=new World(36,26);
        w.cities.add(new World.City(10,"首都",new Hex(3,3),0));
        w.cities.add(new World.City(11,"后方",new Hex(3,18),0));
        w.cities.add(new World.City(12,"南城",new Hex(20,20),0));
        w.cities.add(new World.City(20,"敌都",new Hex(32,3),1));
        World.City port=new World.City(13,"附属港",new Hex(7,18),0);
        port.kind=World.SiteKind.PORT;w.cities.add(port);
        World.City gate=new World.City(14,"附属关",new Hex(2,13),0);
        gate.kind=World.SiteKind.GATE;w.cities.add(gate);
        for(World.City c:w.cities){c.gold=30000;c.food=100000;c.troops=10000;c.order=100;c.morale=90;}
        int[] homes={10,11,13,14,12,10};
        for(int i=0;i<homes.length;i++)w.officers.add(new World.Officer(i,"甲将"+i,0,homes[i],80,80,80,80,80));
        w.officers.add(new World.Officer(20,"敌将",1,20,70,70,70,70,70));
        w.strategy.initializeOffices();
        return w;
    }
}
