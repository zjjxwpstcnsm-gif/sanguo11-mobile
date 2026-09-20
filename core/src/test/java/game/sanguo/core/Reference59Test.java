package game.sanguo.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Current regional contract plus old-map immutability. Test fixtures are not app assets. */
public final class Reference59Test {
    static long checks;
    static void check(boolean ok,String what){checks++;if(!ok)throw new AssertionError(what);}
    static List<String[]> changes()throws IOException{
        try(InputStream in=Reference59Test.class.getResourceAsStream("/reference59-corrections.tsv")){
            if(in==null)throw new IOException("Missing v059 audited cells");
            return new BufferedReader(new InputStreamReader(in,"UTF-8")).lines().map(s->s.split("\t")).toList();
        }
    }
    static void restore58(World w)throws IOException{
        check(w.mapRevision==59,"only reverse a known test revision59, never migrate production");
        for(String[] c:changes()){
            Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1])));
            if(w.sourceInside(h)){
                check(w.terrain[h.q][h.r]==TerrainCode.decode(c[3].charAt(0)),"historical reconstruction post-image");
                w.terrain[h.q][h.r]=TerrainCode.decode(c[2].charAt(0));
            }
        }
        w.mapRevision=58;
    }
    public static void main(String[] args)throws Exception{
        check(NationalMap.REVISION==59&&CityArtCatalog.ASSET_REVISION==56,"map59, unchanged CityAtlas56");
        check(changes().size()==109,"109 unique current corrections, not previous89");
        Set<SourceGridCoord> unique=new HashSet<>();int vq=0,rm=0;
        for(String[] c:changes()){
            SourceGridCoord s=new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1]));check(unique.add(s),"unique national coordinate");
            if(c[2].equals("V")&&c[3].equals("Q"))vq++;else if(c[2].equals("R")&&c[3].equals("M"))rm++;else throw new AssertionError("unreviewed transition");
        }
        check(vq==85&&rm==24,"net changes exact");int scenarios=0;
        for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries()){
            World w=ScenarioCatalog.load(s.id,0,590L);int applied=0;
            for(String[] c:changes()){
                SourceGridCoord source=new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1]));Hex h=MapCoordinates.fromNationalSource(w,source);
                if(!w.sourceInside(h))continue;applied++;
                check(w.terrain[h.q][h.r]==TerrainCode.decode(c[3].charAt(0)),"real scenario post-image "+s.id+" "+source);
                check(w.inside(h)&&TerrainPresentation.detail(w,h).terrain()==w.terrain[h.q][h.r],"impassable still selectable/details");
                check(w.cityAt(h)==null&&w.development.cityAt(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null,"no relocation or occupied-cell overwrite");
                for(Hex n:h.neighbors())if(w.sourceInside(n)){
                    check(MapCoordinates.fromNationalSource(w,MapCoordinates.nationalSource(w,n)).equals(n),"six-direction crop-aware neighborhood");
                    for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()){
                        World.Unit probe=new World.Unit(-1,0,-1,weapon,n,8000,16000);probe.ship=ship;
                        check(w.army.moveCost(probe,n,h)<0,"all crews/ships still reject Q/M");
                    }
                }
            }
            check(w.war.structures().isEmpty(),"zero natural structures retained in "+s.id);
            if(w.sourceColumns()==200)check(applied==109,"seven eras do not multiply geographic edits");
            byte[] current=SaveCodec.encode(w);World loaded=SaveCodec.decode(current);
            check(current[7]==31&&Arrays.equals(current,SaveCodec.encode(loaded)),"Codec31 current exact roundtrip");
            restore58(w);byte[] historical=SaveCodec.encode(w);loaded=SaveCodec.decode(historical);
            check(loaded.mapRevision==58&&Arrays.equals(historical,SaveCodec.encode(loaded)),"Q-containing rev58 remains exact, no silent migration");
            check(NationalMap.compatibilityNotice(loaded).contains("修订59")&&MarchScale.base(loaded,7)==14,"rev58 notice and movement budgets");
            check(w.war.structures().isEmpty(),"legacy structure count unchanged");
            if(w.sourceColumns()==200)siteConnectivity(w,ScenarioCatalog.load(s.id,0,590L));
            scenarios++;System.out.println("REFERENCE59 SCENARIO "+s.id+" corrected="+applied+" natural=0 old58Preserved=true");
        }
        check(scenarios==9,"all nine shipped scenarios");
        for(int revision:new int[]{55,60,999}){
            World w=ScenarioCatalog.load("coalition-190",0,590L);w.mapRevision=revision;boolean rejected=false;
            try{SaveCodec.validate(w);}catch(IOException e){rejected=true;}check(rejected,"unknown revision rejected, not relabelled "+revision);
        }
        rawOldSaves();waterEdges();
        System.out.println("REFERENCE59 CORE PASS: "+checks);
    }
    static void rawOldSaves()throws Exception{
        String legacy=System.getProperty("reference59.legacy58","");if(legacy.isEmpty())return;
        String canonical=System.getProperty("reference59.baselineCanonical","");check(!canonical.isEmpty(),"actual old encoder comparison required");
        for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}){
            String name="baseline-v058-"+id+".sg11";Path file=Path.of(legacy,name);byte[] raw=Files.readAllBytes(file);World w=SaveCodec.decode(raw);
            check(w.mapRevision==58&&w.scenarioId.equals(id),"original v058 APK save identity");
            check(Arrays.equals(SaveCodec.encode(w),Files.readAllBytes(Path.of(canonical,name))),"old058/new059 encoders same JVM canonical bytes "+id);
            check(Arrays.equals(raw,Files.readAllBytes(file)),"original raw save not overwritten");
        }
    }
    // Topological comparison ignores faction locks equally before and after. Actual
    // executable dock/AI edges are tested separately, not inferred from this graph.
    static int[] labels(World w){
        int[] ids=new int[w.width*w.height];int next=0;ArrayDeque<Hex> queue=new ArrayDeque<>();
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
            Hex start=new Hex(q,r);int index=q*w.height+r;if(ids[index]!=0||!land(w,start))continue;
            ids[index]=++next;queue.add(start);
            while(!queue.isEmpty())for(Hex h:queue.remove().neighbors())if(land(w,h)&&ids[h.q*w.height+h.r]==0){ids[h.q*w.height+h.r]=next;queue.add(h);}
        }return ids;
    }
    static boolean land(World w,Hex h){return w.inside(h)&&!w.army.water(h)&&w.terrain[h.q][h.r]!=World.Terrain.MOUNTAIN&&w.terrain[h.q][h.r]!=World.Terrain.NON_NAVIGABLE_WATER;}
    static void siteConnectivity(World before,World after){
        int[] a=labels(before),b=labels(after);int pairs=0;
        for(World.City x:before.cities)for(World.City y:before.cities){
            int xi=x.hex.q*before.height+x.hex.r,yi=y.hex.q*before.height+y.hex.r;
            check((a[xi]>0&&a[xi]==a[yi])==(b[xi]>0&&b[xi]==b[yi]),"regional edits must not sever city/gate/port land topology "+x.name+"/"+y.name);pairs++;
        }
        System.out.println("REFERENCE59 SITE CONNECTIVITY "+pairs+" ordered site pairs unchanged; not a substitute for dock ownership/path validation");
    }
    static void waterEdges(){
        World w=new World(12,12,"甲","乙");World.Officer o=new World.Officer(0,"测试将",0,-1,90,90,90,90,90);w.officers.add(o);
        w.campaign.learned.put(0,EnumSet.of(Campaign.Tech.DIFFICULT_MARCH));
        Hex from=new Hex(5,5),to=new Hex(6,5),dock=new Hex(5,6);World.Unit u=new World.Unit(1,0,0,World.Weapon.SPEAR,from,8000,16000);
        World.City port=new World.City(10,"测试港",dock,0);port.kind=World.SiteKind.PORT;
        for(World.Terrain wet:new World.Terrain[]{World.Terrain.WATER,World.Terrain.SEA})for(World.Terrain dry:new World.Terrain[]{World.Terrain.PLAIN,World.Terrain.ROAD,World.Terrain.MOUNTAIN_PATH,World.Terrain.PLANK_ROAD,World.Terrain.SHALLOWS}){
            w.terrain[from.q][from.r]=dry;w.terrain[to.q][to.r]=wet;
            for(Army.Ship ship:Army.Ship.values()){
                u.ship=ship;w.cities.clear();check(w.army.moveCost(u,from,to)<0&&w.army.moveCost(u,to,from)<0,"no wild embark/land "+wet+" "+dry);
                w.cities.add(port);port.owner=0;
                check(w.army.moveCost(u,from,to)>0&&w.army.moveCost(u,to,from)>0,"owned port both directions "+wet+" "+dry+" "+ship+" costs="+w.army.moveCost(u,from,to)+"/"+w.army.moveCost(u,to,from));
                w.terrain[to.q][to.r]=World.Terrain.NON_NAVIGABLE_WATER;
                check(!w.army.water(to)&&w.army.moveCost(u,from,to)<0,"Q never becomes a boat lane at port");w.terrain[to.q][to.r]=wet;
                port.owner=1;check(w.army.moveCost(u,from,to)<0&&w.army.moveCost(u,to,from)<0,"enemy port no shortcut");
            }
        }
        w.cities.clear();w.cities.add(port);port.owner=0;w.campaign.learned.clear();w.terrain[to.q][to.r]=World.Terrain.WATER;
        for(World.Terrain gated:new World.Terrain[]{World.Terrain.MOUNTAIN_PATH,World.Terrain.SHALLOWS}){
            w.terrain[from.q][from.r]=gated;
            check(w.army.moveCost(u,to,from)<0,"owned dock does not bypass difficult-march research "+gated);
        }
    }
}
