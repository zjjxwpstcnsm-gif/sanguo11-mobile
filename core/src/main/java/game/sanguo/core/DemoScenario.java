package game.sanguo.core;

/** Original engineering fixture. Positions, ownership and stats are NOT a historical scenario or original-game data. */
public final class DemoScenario {
    private DemoScenario() {}
    public static World create() {
        World w=new World(15,12);
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++) {
            if((q*13+r*7)%17<3)w.terrain[q][r]=World.Terrain.FOREST;
            if(q<2&&r<7||q>12&&r>5)w.terrain[q][r]=World.Terrain.MOUNTAIN;
            if(r==6&&q!=6&&q!=7)w.terrain[q][r]=World.Terrain.WATER;
        }
        World.City x=new World.City(0,"新野",new Hex(3,9),0);
        World.City c=new World.City(1,"许昌",new Hex(10,2),1);
        World.City wan=new World.City(2,"宛",new Hex(4,4),-1);wan.defense=1400;wan.troops=0;
        w.cities.add(x);w.cities.add(c);w.cities.add(wan);
        for(World.City city:w.cities) {
            w.terrain[city.hex.q][city.hex.r]=World.Terrain.PLAIN;
            for(Hex h:city.hex.neighbors())if(w.inside(h))w.terrain[h.q][h.r]=World.Terrain.PLAIN;
        }
        w.officers.add(new World.Officer(0,"刘备",0,0,78,75,76,80,95));
        w.officers.add(new World.Officer(1,"关羽",0,0,91,96,72,60,80));
        w.officers.add(new World.Officer(2,"张飞",0,0,85,97,35,25,45));
        w.officers.add(new World.Officer(3,"曹操",1,1,94,72,91,92,90));
        w.officers.add(new World.Officer(4,"夏侯惇",1,1,84,89,60,65,73));
        w.officers.add(new World.Officer(5,"夏侯渊",1,1,85,88,58,52,65));
        w.note("M0规则测试场：地图、武将数值和战斗公式均为工程样例");
        w.note("点新野→出征；点己方部队→点高亮空地移动");
        return w;
    }
}
