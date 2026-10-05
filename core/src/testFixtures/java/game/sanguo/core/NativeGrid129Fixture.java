package game.sanguo.core;

/** Explicit, saveable grid/research fixtures; test source only, never production maps. */
public final class NativeGrid129Fixture {
    private NativeGrid129Fixture(){}
    public static World world(){
        World w=Realm52Fixture.criticalWorld(false);w.scenarioName="v129 difficult-march grid fixture";
        w.unit(2).hex=new Hex(17,10);
        w.terrain[6][7]=World.Terrain.MOUNTAIN_PATH;w.terrain[8][7]=World.Terrain.SHALLOWS;w.terrain[10][7]=World.Terrain.PLANK_ROAD;
        w.terrain[12][7]=World.Terrain.MOUNTAIN;w.terrain[14][7]=World.Terrain.NON_NAVIGABLE_WATER;
        w.terrain[6][9]=World.Terrain.POISON;w.terrain[8][9]=World.Terrain.WATER;
        return w;
    }
    /** Explicit fixture stages the last research turn; completion still runs nextTurn. */
    public static World researchReadyWorld(){
        World w=world();w.campaign.finishTech(0,Campaign.Tech.LOGISTICS);w.campaign.points.put(0,10000);w.city(0).gold=20000;
        World.Result result=w.campaign.research(0,0,Campaign.Tech.DIFFICULT_MARCH);
        if(!result.ok)throw new AssertionError(result.message);
        w.officer(0).otherTaskTurns=1;return w;
    }
    public static void learn(World w,int force){learn(w,force,Campaign.Tech.DIFFICULT_MARCH);}
    private static void learn(World w,int force,Campaign.Tech tech){
        if(tech.prerequisite!=null)learn(w,force,tech.prerequisite);
        w.campaign.finishTech(force,tech);
    }
}
