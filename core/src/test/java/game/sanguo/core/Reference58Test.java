package game.sanguo.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Directed current-map contract, not a replacement for historical regression suites. */
public final class Reference58Test {
    private static long checks;
    private static void check(boolean v,String label){checks++;if(!v)throw new AssertionError(label);}
    static List<String[]> changes()throws IOException{
        InputStream in=Reference58Test.class.getResourceAsStream("/reference58-corrections.tsv");
        if(in==null)throw new IOException("Missing audited correction test data");
        try(BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"))){
            List<String[]> rows=new ArrayList<>();for(String s;(s=r.readLine())!=null;)rows.add(s.split("\t"));return rows;
        }
    }
    /** Test-only historical data reconstruction; production never migrates/replaces terrain. */
    static void restore57(World w)throws IOException{
        for(String[] c:changes()){
            Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1])));
            if(w.sourceInside(h))w.terrain[h.q][h.r]=TerrainCode.decode(c[2].charAt(0));
        }
        w.mapRevision=57;
    }
    public static void main(String[] args)throws Exception{
        String[] abi={"PLAIN","FOREST","MOUNTAIN","WATER","MOUNTAIN_PATH","SHALLOWS","PLANK_ROAD","POISON","SEA","VOID","SWAMP","DAM","SAND","ROAD"};
        for(int i=0;i<abi.length;i++)check(World.Terrain.values()[i].name().equals(abi[i]),"old terrain ordinal ABI "+i);
        check(World.Terrain.NON_NAVIGABLE_WATER.ordinal()==14,"new value appended, never renumbered");
        check(NationalMap.REVISION==58&&CityArtCatalog.ASSET_REVISION==56,"separate resource versions");
        check(changes().size()==89,"89 unique source corrections, not multiplied by scenarios");
        World national=ScenarioCatalog.load("coalition-190",0,580L);int full=0,crops=0;
        for(ScenarioCatalog.Summary summary:ScenarioCatalog.summaries()){
            World w=ScenarioCatalog.load(summary.id,0,580L);int valid=0,padding=0,qcount=0;Set<Hex> structures=new HashSet<>();
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
                Hex h=new Hex(q,r);World.Terrain t=w.terrain[q][r];check(t!=null,"no null terrain");
                if(!w.sourceInside(h)){padding++;check(t==World.Terrain.VOID&&!w.inside(h)&&w.cost(h,World.Weapon.SPEAR)<0,"padding is not source, selection or movement");continue;}
                valid++;SourceGridCoord local=MapCoordinates.source(w,h),global=MapCoordinates.nationalSource(w,h);
                check(MapCoordinates.axial(w,local).equals(h)&&MapCoordinates.fromNationalSource(w,global).equals(h),"crop-aware coordinate roundtrip");
                Hex nh=MapCoordinates.fromNationalSource(national,global);
                check(national.terrain[nh.q][nh.r]==t,"all eras/crops use same authoritative source cell");
                check(TerrainPresentation.detail(w,h).terrain()==t,"every source cell has complete detail");
                check(w.inside(h)==(t!=World.Terrain.VOID),"effective impassable terrain remains selectable");
                if(t!=World.Terrain.NON_NAVIGABLE_WATER)continue;qcount++;
                check(w.cityAt(h)==null&&w.development.cityAt(h)==null&&w.war.at(h)==null&&w.unitAt(h)==null,"corrected water never overwrites occupied cells");
                check(!w.army.water(h),"non-navigable water is not a dock/boat spawn shortcut");
                for(World.Weapon weapon:World.Weapon.values()){
                    check(w.cost(h,weapon)<0&&w.fieldworks.landCost(h,weapon,0)<0,"all land types still blocked");
                    for(Army.Ship ship:Army.Ship.values())for(Hex from:h.neighbors())if(w.inside(from)){
                        World.Unit probe=new World.Unit(-1,0,-1,weapon,from,10000,20000);probe.ship=ship;
                        check(w.army.moveCost(probe,from,h)<0,"no new army/ship passage through corrected water");
                    }
                }
            }
            check(valid==w.sourceColumns()*w.sourceRows(),"effective source dimensions");
            for(War.Structure s:w.war.structures())check(structures.add(s.hex),"no duplicate natural facilities");
            check(w.war.structures().stream().noneMatch(s->s.kind==War.StructureKind.DAM),"no natural H dam invented");
            if(w.sourceColumns()==200){full++;check(padding==19800&&qcount==69,"national padding and blocked water exact counts");}
            else{crops++;check((w.sourceOriginX&1)==0,"crop odd-q parity preserved");}
            byte[] saved=SaveCodec.encode(w);check(saved[7]==31,"serialization structure stays Codec31");
            check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"current data exact save/read");
            System.out.println("REFERENCE58 SCENARIO "+summary.id+" source="+valid+" padding="+padding+" blockedWater="+qcount+" natural="+structures.size());
        }
        check(full==7&&crops==2,"seven full + two local");
        World old=ScenarioCatalog.load("coalition-190",0,580L);restore57(old);byte[] oldBytes=SaveCodec.encode(old);
        check(Arrays.equals(oldBytes,SaveCodec.encode(SaveCodec.decode(oldBytes))),"synthetic full rev57 fixture unchanged");
        check(!NationalMap.compatibilityNotice(old).isEmpty()&&MarchScale.base(old,7)==14,"old notice and native march budget");
        String legacy=System.getProperty("reference58.legacy57","");
        if(!legacy.isEmpty())for(String id:new String[]{"coalition-190","heroes-250"}){
            byte[] bytes=Files.readAllBytes(Path.of(legacy,"baseline-v057-"+id+".sg11"));World loaded=SaveCodec.decode(bytes);
            check(loaded.mapRevision==57&&loaded.scenarioId.equals(id),"actual unmodified v057 APK save identity");
            byte[] canonical=SaveCodec.encode(loaded);
            String originalEncoder=System.getProperty("reference58.baselineCanonical","");
            check(!originalEncoder.isEmpty(),"unchanged v057 JVM encoder comparison required for Android-origin save");
            byte[] baseline=Files.readAllBytes(Path.of(originalEncoder,"baseline-v057-"+id+".sg11"));
            check(Arrays.equals(baseline,canonical),"actual Android save: unchanged v057 and v058 JVM encoders produce identical bytes "+id);
            check(Arrays.equals(canonical,SaveCodec.encode(SaveCodec.decode(canonical))),"canonical JVM roundtrip "+id);
            check(Arrays.equals(bytes,Files.readAllBytes(Path.of(legacy,"baseline-v057-"+id+".sg11"))),"original on-disk legacy file unchanged");
        }
        World fixture=ScenarioCatalog.load("coalition-190",0,580L);Hex[] danger=MapTap57Fixture.addDangerousObjects(fixture);
        check(fixture.war.at(danger[0]).owner==-1&&fixture.war.at(danger[2]).kind==War.StructureKind.EARTH_WALL&&fixture.war.at(danger[3]).kind==War.StructureKind.STONE_WALL,"labelled test-only real wall/dam identities survive");
        check(SaveCodec.decode(SaveCodec.encode(fixture)).war.structures().size()==3,"neutral entity save/restore");
        campValidation();
        System.out.println("REFERENCE58 CORE PASS: "+checks+" actualLegacyFiles="+(!legacy.isEmpty()));
    }
    private static void campValidation()throws Exception {
        World w=TestScenarios.load("world-drill",0);check(!w.events.camps().isEmpty(),"actual historical camp scenario loads");
        WorldEvents.Camp camp=w.events.camps().get(0);World.Terrain ground=w.terrain[camp.hex.q][camp.hex.r];
        check(w.cost(camp.hex,World.Weapon.SPEAR)<0,"existing camp blocks movement on itself, not save validity");
        WorldSystemsSave.validate(w);check(true,"occupied camp is valid on existing land");
        try {
            for(World.Terrain forbidden:new World.Terrain[]{World.Terrain.NON_NAVIGABLE_WATER,World.Terrain.MOUNTAIN,World.Terrain.WATER,World.Terrain.SEA}) {
                w.terrain[camp.hex.q][camp.hex.r]=forbidden;boolean rejected=false;
                try{WorldSystemsSave.validate(w);}catch(IOException expected){rejected=true;}
                check(rejected,"camp forbidden on "+forbidden+" without consulting self-occupancy");
            }
        } finally {w.terrain[camp.hex.q][camp.hex.r]=ground;}
        byte[] encoded=SaveCodec.encode(w);check(Arrays.equals(encoded,SaveCodec.encode(SaveCodec.decode(encoded))),"existing camp retains exact save state");
        int count=0;
        for(World scenario:TestScenarios.all()){SaveCodec.validate(scenario);check(true,"all production/test-only scenario validation "+scenario.scenarioId);count++;}
        check(count==18,"all nine production and nine existing test scenarios retained");
        System.out.println("REFERENCE58 CAMP REGRESSION PASS: occupied camp roundtrip; Q/M/W/SEA rejected; all18 scenarios valid");
    }
}
