package game.sanguo.core;

/** Small authored scenario for real Editor command payload stress, not official content. */
public final class BridgeFactsFixture {
    public static World create(){
        World world=new World(8,8);
        world.cities.add(new World.City(1,"队列验收城",new Hex(2,2),0));
        world.cities.add(new World.City(2,"对照城",new Hex(6,6),1));
        world.governance.reconcile(false);world.reports.rebase();return world;
    }
}
