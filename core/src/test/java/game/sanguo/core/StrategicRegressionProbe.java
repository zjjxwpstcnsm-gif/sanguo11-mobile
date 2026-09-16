package game.sanguo.core;

/** Public v0.24 APIs only, so the identical probe can run on both sides of this change. */
public final class StrategicRegressionProbe {
    public static void main(String[] args){
        World w=new World(40,24,"我军","敌军");
        w.cities.add(new World.City(10,"主城",new Hex(2,2),0));w.cities.add(new World.City(11,"后城",new Hex(3,17),0));w.cities.add(new World.City(12,"空缺城",new Hex(14,17),0));w.cities.add(new World.City(20,"敌城",new Hex(35,4),1));w.cities.add(new World.City(21,"攻略城",new Hex(35,20),1));
        for(World.City c:w.cities){c.gold=30000;c.food=150000;c.troops=30000;c.morale=100;}
        for(int i=0;i<9;i++)w.officers.add(new World.Officer(i,"将"+i,0,i==0?10:11,80,80,80,80,80));w.officers.add(new World.Officer(20,"敌将",1,20,80,80,80,80,80));w.strategy.initializeOffices();
        if(args.length==0||args[0].equals("staff")){
            if(!w.districts.configure(-1,"二军",new int[]{11,12},Districts.Policy.ECONOMY,-1,-1,false,true).ok)throw new AssertionError("fixture configure");
            w.turn=1;w.districts.reset(0);w.districts.run();
            if(w.domestic.missions.stream().noneMatch(m->!m.transport&&m.targetCity==12))throw new AssertionError("vacant district city never receives an actual officer transfer");
        }else{
            World.Unit bow=new World.Unit(w.nextUnitId++,0,1,World.Weapon.CROSSBOW,new Hex(32,4),6000,30000);bow.energy=0;w.units.add(bow);w.officer(1).cityId=-1;w.officer(1).unitId=bow.id;
            Domestic.Facility farm=new Domestic.Facility(w.domestic.nextFacilityId++,20,Domestic.Kind.FARM,new Hex(34,4),-1,0);w.domestic.facilities.add(farm);
            new CampaignAi(w).runUnit(bow,true,c->c.id==21,c->c.owner==0);
            if(farm.hp<1000)throw new AssertionError("city-target restriction bypassed by economic facility raid");
        }
        System.out.println("PASS: "+(args.length==0?"staff":args[0]));
    }
}
