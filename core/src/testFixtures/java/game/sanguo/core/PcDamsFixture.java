package game.sanguo.core;

/** Exact source dam setup. All subsequent damage and flooding are normal commands. */
public final class PcDamsFixture {
    public static World legacy64()throws Exception{World w=ScenarioCatalog.load("heroes-250",0);w.mapRevision=64;w.war.structures.removeIf(d->d.kind==War.StructureKind.DAM);return w;}
    public static final class Case {
        private final World world;private final Hex target;private final int actor,ally,enemy;private final String mode;
        Case(World w,Hex h,int a,int friend,int opponent,String mode){world=w;target=h;actor=a;ally=friend;enemy=opponent;this.mode=mode;}
        public World world(){return world;}public Hex target(){return target;}
        public String mode(){return mode;}public int actor(){return actor;}public int ally(){return ally;}public int enemy(){return enemy;}
        public World.Result command(World w){return w.war.attackStructure(actor,target);}
    }
    public static Case prepare(int index,String mode)throws Exception {
        if(index<0||index>=4||!mode.equals("damage")&&!mode.equals("destroy"))throw new IllegalArgumentException("dam case");
        World w=ScenarioCatalog.load("heroes-250",0);Hex target=MapCoordinates.fromNationalSource(w,PcDamCatalog.cells().get(index));
        War.Structure dam=w.war.at(target);if(dam==null||dam.kind!=War.StructureKind.DAM)throw new AssertionError("Missing source dam");
        // Legal persisted states prepared before the installed transaction, never patched mid-command.
        dam.hp=mode.equals("damage")?510:400;
        World.Officer leader=available(w,0);leader.war=100;
        Hex actorHex=null;for(Hex h:target.neighbors())if(free(w,h)){actorHex=h;break;}
        if(actorHex==null)throw new AssertionError("No legal source dam attacker "+index);
        World.Unit actor=add(w,leader,actorHex);if(w.war.structureAttackError(actor.id,target)!=null)throw new AssertionError("Illegal source dam attack");
        java.util.List<Hex> low=new java.util.ArrayList<>();
        // An independent breadth-two lowland traversal provides one unit of each allegiance.
        java.util.Set<Hex> reached=new java.util.LinkedHashSet<>();reached.add(target);java.util.List<Hex> edge=new java.util.ArrayList<>(reached);
        for(int depth=0;depth<2;depth++){
            java.util.List<Hex> next=new java.util.ArrayList<>();for(Hex h:edge)for(Hex n:h.neighbors())if(w.inside(n)&&!reached.contains(n)){
                World.Terrain t=w.terrain[n.q][n.r];if(t==World.Terrain.PLAIN||t==World.Terrain.SWAMP||t==World.Terrain.SHALLOWS||t==World.Terrain.WATER){reached.add(n);next.add(n);if(free(w,n))low.add(n);}
            }edge=next;
        }
        if(low.size()<2)throw new AssertionError("Source dam flood test lacks two legal lowland victims "+index);
        World.Unit ally=add(w,available(w,0),low.get(0));World.Unit enemy=add(w,available(w,1),low.get(1));
        return new Case(w,target,actor.id,ally.id,enemy.id,mode);
    }
    private static World.Officer available(World w,int owner){
        for(World.Officer o:w.officers)if(o.owner==owner&&o.unitId<0){w.strategy.releaseGovernor(o.id);return o;}
        throw new AssertionError("No commander "+owner);
    }
    private static World.Unit add(World w,World.Officer leader,Hex h){
        World.Unit u=new World.Unit(w.nextUnitId++,leader.owner,leader.id,World.Weapon.SWORD,h,5000,10000);w.units.add(u);leader.unitId=u.id;leader.cityId=-1;return u;
    }
    private static boolean free(World w,Hex h){return w.inside(h)&&w.cost(h,World.Weapon.SWORD)>0&&!w.army.water(h)&&w.cityAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null&&w.events.at(h)==null;}
}
