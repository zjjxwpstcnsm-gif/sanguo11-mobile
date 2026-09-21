package game.sanguo.core;

import java.util.*;
import java.io.IOException;
import java.util.function.Consumer;

/** Shared JVM/installed-Android checks of the production World, Army, UnitOrders and AI.
 * Fixtures assign ownership/resources only to isolate geography from diplomacy. No alternate rules. */
public final class Geography63Checks {
    private Geography63Checks(){}
    public static final int[][] PORTS={
        {20060,148,110,149,110,152,110,20068},
        {20061,123,66,123,65,123,62,20055},
        {20070,177,101,177,100,178,98,20059},
        {20074,116,137,115,136,112,135,20077},
        {20079,70,110,70,109,70,107,20078},
        {20080,99,133,100,133,103,133,20081},
        {20062,101,72,101,71,101,68,20055}};
    public static final int[][] CELLS={{18,24},{29,24},{30,24},{33,15},{34,15},{34,18},{35,14},{37,15},{37,20},{37,21},{37,22},{37,23},{37,24},{38,15}};
    public static final String AFTER="ARRMMMMMMMMMMM";
    public static Hex at(World w,int x,int y){return MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,y));}
    public static final class Result {
        public int checks,steps,rounds;public final StringBuilder log=new StringBuilder();
        public void check(boolean pass,String label){checks++;if(!pass){log.append("FAIL ").append(label).append('\n');throw new AssertionError(label);} }
        void note(String text){log.append(text).append('\n');}
    }
    /** Water-only graph: no ports, land bridges, Q, padding or exterior surfaces are vertices. */
    public static List<Hex> waterPath(World w,Hex start,Collection<Hex> goals){
        if(!w.army.water(start))return Collections.emptyList();
        Set<Hex> target=new HashSet<>(goals);Map<Hex,Hex> previous=new HashMap<>();ArrayDeque<Hex> queue=new ArrayDeque<>();
        World.Unit probe=new World.Unit(-1,0,-1,World.Weapon.SWORD,start,1000,2000);
        previous.put(start,null);queue.add(start);Hex end=null;
        while(!queue.isEmpty()){
            Hex h=queue.remove();if(target.contains(h)){end=h;break;}
            for(Hex n:h.neighbors())if(w.army.water(n)&&!previous.containsKey(n)&&w.army.moveCost(probe,h,n)>0){previous.put(n,h);queue.add(n);}
        }
        if(end==null)return Collections.emptyList();LinkedList<Hex> path=new LinkedList<>();
        for(Hex h=end;h!=null;h=previous.get(h))path.addFirst(h);return path;
    }
    public static List<Hex> waterNeighbors(World w,World.City port){List<Hex> out=new ArrayList<>();for(Hex h:port.hex.neighbors())if(w.army.water(h))out.add(h);return out;}
    private static Set<Hex> waterComponent(World w,Hex start){
        Set<Hex> seen=new HashSet<>();if(!w.army.water(start))return seen;ArrayDeque<Hex> todo=new ArrayDeque<>();todo.add(start);seen.add(start);
        while(!todo.isEmpty())for(Hex n:todo.remove().neighbors())if(w.army.water(n)&&seen.add(n))todo.add(n);return seen;
    }
    public static Hex landDock(World w,World.City port,Hex water,World.Unit unit){
        for(Hex h:port.hex.neighbors())if(!w.army.water(h)&&w.cityAt(h)==null&&w.unitAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null&&h.distance(water)==1&&w.army.moveCost(unit,h,water)>0&&w.army.moveCost(unit,water,h)>0)return h;
        return null;
    }
    private static boolean inland(World w,World.City port,Hex start,World.Unit unit){
        Set<Hex> seen=new HashSet<>();ArrayDeque<Hex> todo=new ArrayDeque<>();todo.add(start);seen.add(start);
        while(!todo.isEmpty()){
            Hex h=todo.remove();for(World.City c:w.cities)if(c.kind==World.SiteKind.CITY&&SiteFootprint.distance(c,h)<=1)return true;
            for(Hex n:h.neighbors())if(!w.army.water(n)&&w.army.moveCost(unit,h,n)>0&&seen.add(n))todo.add(n);
        }return false;
    }
    private static boolean landToCropBoundary(World w,Hex start,World.Unit unit){
        if(w.sourceColumns()==200)return false;Set<Hex> seen=new HashSet<>();ArrayDeque<Hex> todo=new ArrayDeque<>();todo.add(start);seen.add(start);
        while(!todo.isEmpty()){Hex h=todo.remove();SourceGridCoord local=MapCoordinates.source(w,h);
            if(seen.size()>1&&(local.x==0||local.y==0||local.x==w.sourceColumns()-1||local.y==w.sourceRows()-1))return true;
            for(Hex n:h.neighbors())if(!w.army.water(n)&&w.army.moveCost(unit,h,n)>0&&seen.add(n))todo.add(n);
        }return false;
    }
    public static Result map(World w)throws IOException{
        Result r=new Result();r.check(w.mapRevision==63&&CityArtCatalog.ASSET_REVISION==56,"independent map/art versions");
        int voids=0,exterior=0,padding=0,changed=0;
        for(int q=0;q<w.width;q++)for(int y=0;y<w.height;y++){
            Hex h=new Hex(q,y);if(!w.sourceInside(h)){padding++;r.check(!w.inside(h)&&NationalExterior.surface(w,h)==null,"padding excluded");continue;}
            SourceGridCoord s=MapCoordinates.nationalSource(w,h);r.check(at(w,s.x,s.y).equals(h),"source/world/crop roundtrip");
            if(w.terrain[q][y]==World.Terrain.VOID){voids++;r.check(NationalExterior.surface(w,h)!=null,"every residual source VOID is explicitly reviewed exterior");}
            if(NationalExterior.surface(w,h)!=null){exterior++;r.check(!w.inside(h),"exterior never becomes traversable");World.Terrain prior=w.terrain[q][y];w.terrain[q][y]=World.Terrain.PLAIN;r.check(NationalExterior.surface(w,h)==null,"valid saved terrain wins over inherited exterior");w.terrain[q][y]=prior;}
        }
        for(int i=0;i<CELLS.length;i++){
            Hex h=at(w,CELLS[i][0],CELLS[i][1]);if(!w.sourceInside(h))continue;changed++;
            r.check(w.inside(h)&&TerrainCode.encode(w.terrain[h.q][h.r])==AFTER.charAt(i)&&NationalExterior.surface(w,h)==null,"actual reviewed effective surface, not exterior fallback");
            r.check(NationalMap.restricted(w,h),"explicit v063 per-coordinate blocking");
            for(Hex n:h.neighbors())if(w.sourceInside(n))for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()){
                World.Unit u=new World.Unit(-1,0,-1,weapon,n,1000,2000);u.ship=ship;
                r.check(w.army.moveCost(u,n,h)<0&&w.army.moveCost(u,h,n)<0&&w.army.entryCost(u,n,h)<0&&w.army.entryCost(u,h,n)<0,"reconstructed surface is neither route destination nor phantom origin");
            }
            r.check(w.cityAt(h)==null&&w.development.cityAt(h)==null,"no site or development overwritten");
        }
        List<Hex> anchors=new ArrayList<>();for(int[] p:PORTS){Hex a=at(w,p[5],p[6]);if(w.sourceInside(a)&&w.army.water(a))anchors.add(a);World.City c=w.city(p[0]);if(c!=null)r.check(c.hex.equals(at(w,p[3],p[4])),"stable logical site position in each scenario");}
        int ports=0;World parent=null;
        for(World.City port:w.cities)if(port.kind==World.SiteKind.PORT){
            ports++;int owner=port.owner;port.owner=0;World.Unit probe=new World.Unit(-1,0,-1,World.Weapon.SWORD,port.hex,1000,2000);
            List<Hex> waters=waterNeighbors(w,port);r.check(!waters.isEmpty(),"port has real water neighbor: "+port.name);boolean dock=false,landConnected=false,cropExit=false,waterCropExit=false,main=false;int component=0;
            for(Hex water:waters){Hex land=landDock(w,port,water,probe);if(land!=null){dock=true;landConnected|=inland(w,port,land,probe);cropExit|=landToCropBoundary(w,land,probe);}
                Set<Hex> waterCells=waterComponent(w,water);component=Math.max(component,waterCells.size());
                if(w.sourceColumns()!=200)for(Hex h:waterCells){SourceGridCoord local=MapCoordinates.source(w,h);if(local.x==0||local.y==0||local.x==w.sourceColumns()-1||local.y==w.sourceRows()-1)waterCropExit=true;}
                if(!waterPath(w,water,anchors).isEmpty())main=true;}
            r.check(dock,"owned-port bidirectional land/water conversion: "+port.name);r.check(landConnected||cropExit,"land-side reaches listed city or an actual local-crop boundary: "+port.id+" "+port.name+" / "+w.scenarioId);if(!landConnected)r.note("LOCAL_CROP_LAND_LIMIT id="+port.id+" scenario="+w.scenarioId+" reachesCropBoundary="+cropExit+"; inland city is absent from this crop, full national geography separately checked");
            // Crops only assert paths to anchors actually inside their playable raster.
            if(!main&&w.sourceColumns()!=200&&waterCropExit){
                if(parent==null)parent=ScenarioCatalog.load("coalition-190",0,630L);
                List<Hex> nationalAnchors=new ArrayList<>();for(int[] point:PORTS)nationalAnchors.add(at(parent,point[5],point[6]));
                boolean parentMain=false;for(Hex water:waterNeighbors(parent,parent.city(port.id)))if(!waterPath(parent,water,nationalAnchors).isEmpty())parentMain=true;
                r.check(parentMain,"cropped water retains actual nationally connected source geography");
                r.note("LOCAL_CROP_WATER_LIMIT id="+port.id+" scenario="+w.scenarioId+"; playable water reaches crop edge, route to named anchor leaves crop; no in-bounds full passage claimed");
            }else r.check(main,"continuous in-bounds main-waterway anchor path: "+port.name+" / "+w.scenarioId);
            r.note("PORT_STATIC id="+port.id+" name="+port.name+" source="+MapCoordinates.nationalSource(w,port.hex)+" world="+port.hex+" component="+component+" inBoundsAnchor="+main);
            port.owner=owner;
        }
        if(w.sourceColumns()==200){r.check(voids==1051&&exterior==1051&&padding==19800&&changed==14,"full native200 counts");r.check(w.cities.size()==87&&ports==35&&w.cities.stream().mapToInt(c->w.development.parcels(c.id).size()).sum()==591,"protected city/gate/port/development counts");}
        r.note("MAP scenario="+w.scenarioId+" ports="+ports+" cells="+changed+" exterior="+exterior+" padding="+padding);
        return r;
    }
    private static World travelFixture(int[] p)throws IOException{
        World w=ScenarioCatalog.load("coalition-190",0,630L);w.active=w.player=0;
        World.City a=w.city(p[0]),b=w.city(p[7]);a.owner=b.owner=0;a.troops=8000;a.food=30000;a.gold=5000;b.troops=0;b.food=1000;b.gold=0;
        Arrays.fill(a.equipment,0);for(int i=0;i<4;i++)a.equipment[i]=8000;Arrays.fill(b.equipment,0);Arrays.fill(a.ships,3);Arrays.fill(b.ships,0);
        w.officers.add(new World.Officer(900000,"水路验证官",0,a.id,90,90,90,90,90));w.actionPoints[0]=60;return w;
    }
    private static World.Unit field(World w,World.City a,Army.Ship ship){
        World.Result deployed=w.army.deploy(a.id,900000,new int[0],World.Weapon.SWORD,ship,1000,2000,0);
        if(!deployed.ok)throw new AssertionError(deployed.message);return w.unit(w.officer(900000).unitId);
    }
    private static void step(World w,World.Unit u,Hex to,Result r){
        int cost=w.army.moveCost(u,u.hex,to);r.check(cost>0&&u.hex.distance(to)==1,"legal actual movement edge");
        if(w.orders.remaining(u)<cost||u.acted){w.turn++;w.orders.reset(u);r.rounds++;}
        int before=u.movementSpent;World.Result moved=w.orders.executeImmediateRoute(u.id,Arrays.asList(u.hex,to));r.check(moved.ok,moved.message);
        r.check(u.hex.equals(to)&&u.movementSpent==before+cost,"actual execution spent exactly the preview edge cost");r.steps++;
    }
    public static Result trip(int[] p,Army.Ship ship,boolean transport,boolean reverse)throws Exception{
        Result r=new Result();World w=travelFixture(p);World.City a=w.city(p[0]),b=w.city(p[7]);if(reverse){World.City tmp=a;a=b;b=tmp;w.officer(900000).cityId=a.id;a.troops=8000;a.food=30000;a.gold=5000;Arrays.fill(a.ships,3);}
        World.Unit u;
        if(transport){World.Result d=w.domestic.transport(a.id,b.id,900000,new int[0],10,2000,1000,new int[4],true,false);r.check(d.ok,d.message);u=w.domestic.missions.get(0);}
        else u=field(w,a,ship);
        Hex waterStart=null;List<Hex> path=Collections.emptyList();
        List<Hex> landingWaters=new ArrayList<>();for(Hex h:waterNeighbors(w,b))if(landDock(w,b,h,u)!=null)landingWaters.add(h);
        for(Hex h:waterNeighbors(w,a)){List<Hex> candidate=waterPath(w,h,landingWaters);if(!candidate.isEmpty()&&(path.isEmpty()||candidate.size()<path.size())){waterStart=h;path=candidate;}}
        r.check(!path.isEmpty(),"actual static water path to second port");
        MarchOrders.Plan approach=w.marches.previewMove(u.id,waterStart);r.check(approach.valid(),approach.error);
        r.check(w.marches.execute(approach).ok&&u.hex.equals(waterStart),"actual sortie and embarkation reaches the main-waterway start");
        for(int i=1;i<path.size();i++)step(w,u,path.get(i),r);
        Hex land=landDock(w,b,u.hex,u);r.check(land!=null,"destination port has a legal landing lane");step(w,u,land,r);r.check(!w.army.water(u.hex),"actual other-port disembarkation");
        World loaded=SaveCodec.decode(SaveCodec.encode(w));r.check(loaded.unit(u.id).hex.equals(u.hex)&&loaded.unit(u.id).movementSpent==u.movementSpent,"naval/transport endpoint and budget survive save");
        r.note("PORT_RUNTIME id="+p[0]+" ship="+ship+" transport="+transport+" reverse="+reverse+" waterEdges="+(path.size()-1)+" actualSteps="+r.steps+" commandBudgetResets="+r.rounds);
        return r;
    }
    public static Result ai(int[] p,boolean reverse)throws Exception{
        Result r=new Result();World w=travelFixture(p);World.City a=w.city(p[0]),b=w.city(p[7]);if(reverse){World.City tmp=a;a=b;b=tmp;w.officer(900000).cityId=a.id;a.troops=8000;a.food=30000;}
        World.Unit u=field(w,a,Army.Ship.BOAT);Hex water=waterNeighbors(w,a).get(0);MarchOrders.Plan approach=w.marches.previewMove(u.id,water);r.check(approach.valid()&&w.marches.execute(approach).ok,"AI fixture embarks through actual port command");u.food=0;
        final int destination=b.id;CampaignAi ai=new CampaignAi(w);
        for(int n=0;n<20&&w.unit(u.id)!=null;n++){w.turn++;w.orders.reset(u);ai.runUnit(u,false,c->false,c->c.id==destination);r.rounds++;}
        r.check(w.unit(u.id)==null&&w.officer(900000).cityId==destination,"official AI reaches and garrisons at other main-waterway port");r.note("PORT_AI id="+p[0]+" reverse="+reverse+" decisionRounds="+r.rounds);return r;
    }
    public static Result negatives()throws IOException{
        Result r=new Result();World w=new World(15,15,new String[]{"甲","乙"});for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.MOUNTAIN);
        Hex a=new Hex(4,4),b=new Hex(4,8);w.terrain[a.q][a.r]=w.terrain[b.q][b.r]=World.Terrain.WATER;
        r.check(waterPath(w,a,Arrays.asList(b)).isEmpty(),"isolated one-cell water is not main-waterway access");w.terrain[4][5]=World.Terrain.WATER;
        r.check(waterPath(w,a,Arrays.asList(b)).isEmpty(),"isolated two-cell water is not main-waterway access");
        w.terrain[4][6]=World.Terrain.NON_NAVIGABLE_WATER;w.terrain[4][7]=World.Terrain.WATER;
        r.check(waterPath(w,a,Arrays.asList(b)).isEmpty(),"Q break is not a water connection");w.terrain[4][6]=World.Terrain.PLAIN;
        r.check(waterPath(w,a,Arrays.asList(b)).isEmpty(),"land bridge is not a water connection");w.terrain[4][6]=World.Terrain.WATER;
        r.check(!waterPath(w,a,Arrays.asList(b)).isEmpty(),"continuous legal water positive control");w.terrain[a.q][a.r]=World.Terrain.NON_NAVIGABLE_WATER;
        r.check(waterPath(w,a,Arrays.asList(b)).isEmpty(),"Q cannot be a valid water origin");
        World national=ScenarioCatalog.load("coalition-190",0,630L);boolean tested=false;
        for(int y=0;y<200&&!tested;y++)for(int x=0;x<200&&!tested;x++)if(NationalExterior.sourceSurface(x,y)==NationalExterior.Surface.SEA){Hex h=at(national,x,y);r.check(NationalExterior.appearance(national,h)==World.Terrain.NON_NAVIGABLE_WATER&&!national.army.water(h)&&waterPath(national,h,Arrays.asList(at(national,123,62))).isEmpty(),"exterior water is not a playable water origin");tested=true;}
        World.City qa=national.city(20070);r.check(qa.hex.distance(at(national,178,99))==2&&national.army.water(at(national,178,99)),"odd-q wrong phase looks near water but has no six-direction adjacency");
        return r;
    }
}
