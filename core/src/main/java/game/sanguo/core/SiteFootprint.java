package game.sanguo.core;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

/** Authoritative site geometry in axial coordinates. A radius-one city projects to 2/3/2
 * on either odd-row or native odd-column source grids; gates and ports are single cells. No ownership is cached. */
public final class SiteFootprint {
    private SiteFootprint() {}
    private static final Comparator<Hex> ORDER=Comparator.comparingInt((Hex h)->h.r).thenComparingInt(h->h.q);
    public static List<Hex> cells(World.City c) {
        if(c==null)return Collections.emptyList();
        if(c.footprintKind!=c.kind){
            List<Hex> cells=new ArrayList<>();cells.add(c.hex);
            if(c.kind==World.SiteKind.CITY)cells.addAll(c.hex.neighbors());
            cells.sort(ORDER);c.footprint=Collections.unmodifiableList(cells);c.footprintKind=c.kind;
        }
        return c.footprint;
    }
    public static boolean contains(World.City c,Hex h){return c!=null&&h!=null&&cells(c).contains(h);}
    public static List<Hex> edge(World.City c){
        Set<Hex> perimeter=new TreeSet<>(ORDER);
        for(Hex h:cells(c))perimeter.addAll(h.neighbors());perimeter.removeAll(cells(c));
        return new ArrayList<>(perimeter);
    }
    public static int distance(World.City c,Hex h){
        int d=Integer.MAX_VALUE;if(h!=null)for(Hex cell:cells(c))d=Math.min(d,h.distance(cell));return d;
    }
    /** Existential range check, including minimum range and per-cell restrictions. Never min(distance) alone. */
    public static Hex hit(World.City c,Hex from,int min,int max,Hex preferred,Predicate<Hex> legal){
        if(c==null||from==null||min<0||max<min)return null;
        if(contains(c,preferred)&&inRange(from,preferred,min,max)&&legal.test(preferred))return preferred;
        Hex best=null;int nearest=Integer.MAX_VALUE;
        for(Hex h:cells(c)){int d=from.distance(h);
            if(d>=min&&d<=max&&legal.test(h)&&d<nearest){best=h;nearest=d;}}
        return best;
    }
    private static boolean inRange(Hex a,Hex b,int min,int max){int d=a.distance(b);return d>=min&&d<=max;}
    /** Own cities and gates are transit tiles, not implicit garrison commands. Port transit preserves
     * the historical dock-edge rule (ports themselves are reserved, entered explicitly). */
    public static boolean transit(World.City c,int owner){return c==null||c.owner==owner&&c.kind!=World.SiteKind.PORT;}
    public static boolean mayStep(World w,World.Unit u,Hex from,Hex to){
        World.City destination=w.cityAt(to);if(transit(destination,u.owner))return true;
        // Ownership can change underneath field units. Keep them intact, permitting outward escape
        // only: center -> rim -> outside. Never allow another enemy to enter through this exception.
        World.City origin=w.cityAt(from);
        return origin!=null&&origin==destination&&origin.owner!=u.owner&&
            escapeDepth(origin,to)<escapeDepth(origin,from);
    }
    private static int escapeDepth(World.City c,Hex h){return contains(c,h)?c.kind==World.SiteKind.CITY&&h.equals(c.hex)?2:1:0;}
    /** Actual entry cell, not the center. Explicit garrison may enter a single-cell port/gate.
     * The same movement-edge check enforces dock-only water/land conversion. */
    public static Hex entry(World w,World.Unit u,Hex from,World.City c){
        if(u==null||c==null||c.owner!=u.owner||from==null)return null;
        // A city is entered only after movement actually reaches one of its seven cells.
        // The old adjacent-cell shortcut removed a unit without paying its final movement edge.
        // Ports and gates intentionally retain their historical single-cell docking semantics.
        if(c.kind==World.SiteKind.CITY)return contains(c,from)&&legalEntryCell(w,u,from)?from:null;
        if(contains(c,from))return legalEntryCell(w,u,from)?from:null;
        Hex best=null;int cheapest=Integer.MAX_VALUE;
        for(Hex h:cells(c))if(from.distance(h)==1){
            World.Unit occupant=w.unitAt(h);if(occupant!=null&&occupant.id!=u.id)continue;
            int cost=w.army.entryCost(u,from,h);
            if(cost>0&&cost<cheapest){cheapest=cost;best=h;}}
        return best;
    }
    private static boolean legalEntryCell(World w,World.Unit u,Hex h){
        if(u==null||h==null||!w.inside(h)||w.domestic.at(h)!=null||w.war.at(h)!=null||w.war.fireAt(h)!=null)return false;
        World.Unit occupant=w.unitAt(h);
        return (occupant==null||occupant.id==u.id)&&w.army.entryCost(u,h,h)>0;
    }
    /** Candidate goals are fed to the existing Dijkstra, not selected by geometric proximity. */
    public static List<Hex> entryGoals(World w,World.Unit u,World.City c){
        List<Hex> goals=new ArrayList<>();if(u==null||c==null||c.owner!=u.owner)return goals;
        if(c.kind==World.SiteKind.CITY||c.kind==World.SiteKind.GATE){
            for(Hex h:cells(c))if(legalEntryCell(w,u,h))goals.add(h);
        }else for(Hex h:edge(c))if(w.inside(h)&&entry(w,u,h,c)!=null)goals.add(h);
        return goals;
    }
    /** A read-only, weighted deployment from the logical center. A field unit never uses this
     * operation. Its path includes the center and the real spawn, with no visual pivot involved. */
    public static final class Deployment {
        public final List<Hex> path;
        public final int cost, budget;
        public final String error;
        private Deployment(List<Hex> path,int cost,int budget,String error){
            this.path=Collections.unmodifiableList(new ArrayList<>(path));this.cost=cost;this.budget=budget;this.error=error;
        }
        public boolean valid(){return error==null;}
        public Hex origin(){return valid()?path.get(0):null;}
        public Hex exit(){return valid()?path.get(path.size()-1):null;}
        /** Apply once, before publishing the new unit or mission. The preflight is synchronous. */
        void apply(World.Unit u){
            if(!valid())throw new IllegalStateException(error);
            if(!u.hex.equals(origin())||u.movementSpent!=0)throw new IllegalStateException("出征起点或已付移动不一致");
            u.hex=exit();u.movementBudget=budget;u.movementSpent=cost;
        }
    }
    private static final class DepartureStep {
        final Hex h;final int cost;
        DepartureStep(Hex h,int cost){this.h=h;this.cost=cost;}
    }
    public static Deployment deployment(World w,World.City c,World.Unit probe){
        if(c==null||probe==null||probe.weapon==null||probe.ship==null||probe.owner!=c.owner||!c.hex.equals(probe.hex))
            return new Deployment(Collections.emptyList(),0,0,"出征城市或逻辑中心无效");
        int budget=w.officer(probe.officerId)==null?w.army.movement(probe):w.war.movementAt(probe,c.hex);
        // A blocked center is not a license to teleport through the occupying unit.
        if(!legalEntryCell(w,probe,c.hex)||w.unitAt(c.hex)!=null)
            return new Deployment(Collections.emptyList(),0,budget,"城市逻辑中心被占用或不可通行，无法出征");
        Map<Hex,Integer> distance=new HashMap<>();Map<Hex,Hex> previous=new HashMap<>();
        PriorityQueue<DepartureStep> queue=new PriorityQueue<>(Comparator.comparingInt((DepartureStep n)->n.cost)
            .thenComparingInt(n->n.h.q).thenComparingInt(n->n.h.r));
        Army.MovementCosts costs=w.army.movementCosts(probe);
        distance.put(c.hex,0);queue.add(new DepartureStep(c.hex,0));
        while(!queue.isEmpty()){
            DepartureStep step=queue.remove();if(step.cost!=distance.get(step.h))continue;
            if(!contains(c,step.h)){
                LinkedList<Hex> path=new LinkedList<>();for(Hex h=step.h;h!=null;h=previous.get(h))path.addFirst(h);
                // Single-cell gates/ports retain their historical departure budget; cities pay every edge.
                return new Deployment(path,c.kind==World.SiteKind.CITY?step.cost:0,budget,null);
            }
            for(Hex h:step.h.neighbors()){
                if(!w.inside(h)||w.unitAt(h)!=null||w.domestic.at(h)!=null||w.war.at(h)!=null||w.war.fireAt(h)!=null)continue;
                World.City other=w.cityAt(h);if(other!=null&&other!=c)continue;
                int edge=costs.cost(step.h,h);if(edge<1)continue;
                int total=step.cost+edge;
                if(total<=budget&&w.advancedBattle.zone(probe,h))total=budget;
                if(total>budget||total>=distance.getOrDefault(h,Integer.MAX_VALUE))continue;
                distance.put(h,total);previous.put(h,step.h);queue.add(new DepartureStep(h,total));
            }
        }
        return new Deployment(Collections.emptyList(),0,budget,"逻辑中心至城外没有本旬可达的合法出征路径（占格、地形或移动力不足）");
    }
    /** Compatibility query for callers that only need to know whether an exit exists. */
    public static Hex deploymentExit(World w,World.City c,World.Weapon weapon){
        if(c==null||weapon==null)return null;
        return deployment(w,c,new World.Unit(-1,c.owner,-1,weapon,c.hex,1,1)).exit();
    }
    static Hex deploymentExit(World w,World.City c,World.Unit probe){return deployment(w,c,probe).exit();}
    /** Once per effect, not once per occupied cell. Callers with genuine multiple hits create one
     * set per hit, retaining the original multi-hit semantics. Stable ordering helps replays. */
    public static List<World.City> covered(World w,Collection<Hex> area){
        Map<Integer,World.City> unique=new TreeMap<>();
        for(Hex h:area){World.City c=w.cityAt(h);if(c!=null)unique.put(c.id,c);}
        return new ArrayList<>(unique.values());
    }
    public static void ownershipChanged(World w,World.City c){
        for(World.Unit u:w.fieldUnits())if(contains(c,u.hex)&&u.owner!=c.owner){
            u.march=null;w.aiOrders.orders.remove(u.id);
            if(u instanceof Domestic.Mission)((Domestic.Mission)u).stopped=true;
            w.note(w.officer(u.officerId).name+"部队所在的"+c.name+"已易主；保留兵粮，只能向城外撤离，重新下达命令");
        }
    }
    public static void validate(World w)throws IOException{
        Set<Hex> occupied=new HashSet<>();
        for(World.City c:w.cities){
            List<Hex> footprint=cells(c);
            if(footprint.size()!=(c.kind==World.SiteKind.CITY?7:1))throw new IOException("据点占地数量错误："+c.name);
            for(Hex h:footprint)if(!w.inside(h)||!occupied.add(h)||
                (c.kind==World.SiteKind.CITY?w.terrain[h.q][h.r]!=World.Terrain.PLAIN:w.cost(h,World.Weapon.SPEAR)<1))
                throw new IOException("据点占地越界、重叠或地形冲突："+c.name+" @ "+h+"；请使用七格地图重新开局");
        }
    }
}
