package game.sanguo.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** v060 exact batch, directed movement edges and true old-map preservation. */
public final class Reference60Test {
    static long checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static List<String[]> changes()throws IOException {
        try(InputStream in=Reference60Test.class.getResourceAsStream("/reference60-corrections.tsv")) {
            if(in==null)throw new IOException("Missing v060 audited source cells");
            return new BufferedReader(new InputStreamReader(in,"UTF-8")).lines().map(s->s.split("\t")).toList();
        }
    }
    /** Only test reconstruction, reversing actual terrain edits; never production migration. */
    static void restore59(World w)throws IOException {
        if(w.mapRevision==61)Reference61Test.restore60(w);
        check(w.mapRevision==60,"only reverse a known revision60 test map");
        for(String[] c:changes()){
            Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1])));
            if(w.sourceInside(h)){check(w.terrain[h.q][h.r]==World.Terrain.NON_NAVIGABLE_WATER,"historical exact postimage");w.terrain[h.q][h.r]=World.Terrain.VOID;}
        }
        w.mapRevision=59;
    }
    public static void main(String[] args)throws Exception {
        check(NationalMap.REVISION==61&&CityArtCatalog.ASSET_REVISION==56,"independent map/art revisions");
        List<String[]> rows=changes();Set<SourceGridCoord> changed=new HashSet<>();
        for(String[] c:rows){check(changed.add(new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1]))),"unique national source coordinate");check(c[2].equals("V")&&c[3].equals("Q"),"no existing water/rule change");}
        check(changed.size()==987,"987 new cells, not inherited109 or multiplied by eras");
        int scenarios=0,full=0,crops=0;
        for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries()) {
            World current=ScenarioCatalog.load(s.id,0,590L);Reference61Test.restore60(current);World before=ScenarioCatalog.load(s.id,0,590L);restore59(before);
            int applied=0,padding=0;
            for(int q=0;q<current.width;q++)for(int r=0;r<current.height;r++) {
                Hex h=new Hex(q,r);World.Terrain terrain=current.terrain[q][r];
                check(terrain!=null&&TerrainCode.decode(TerrainCode.encode(terrain))==terrain,"all source characters retain ABI");
                if(!current.sourceInside(h)){padding++;check(terrain==World.Terrain.VOID&&!current.inside(h),"padding never water/render/hit");continue;}
                SourceGridCoord source=MapCoordinates.nationalSource(current,h);
                check(MapCoordinates.fromNationalSource(current,source).equals(h),"world-aware national coordinate roundtrip");
                check(current.inside(h)==(terrain!=World.Terrain.VOID),"valid blocked terrain is selectable");
                check(TerrainPresentation.detail(current,h).terrain()==terrain,"same source/projection/detail identity");
                if(!changed.contains(source)){check(terrain==before.terrain[q][r],"every non-ledger cell unchanged, including W/Q/R/D/B");continue;}
                applied++;check(terrain==World.Terrain.NON_NAVIGABLE_WATER&&before.terrain[q][r]==World.Terrain.VOID,"exact before/after");
                check(current.cityAt(h)==null&&current.development.cityAt(h)==null&&current.unitAt(h)==null&&current.war.at(h)==null,"all occupied/protected cells retained");
                check(!current.army.water(h),"Q not a boat launch shortcut");
                for(Hex n:h.neighbors())if(current.sourceInside(n)) {
                    check(MapCoordinates.fromNationalSource(current,MapCoordinates.nationalSource(current,n)).equals(n),"six-direction neighborhood including crop parity");
                    for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()) {
                        World.Unit unit=new World.Unit(-1,0,-1,weapon,n,8000,16000);unit.ship=ship;
                        check(current.army.moveCost(unit,n,h)<0,"land/ship/transport cannot enter restored Q");
                        check(current.army.moveCost(unit,h,n)<0,"Q cannot be an invalid route origin/intermediate");
                    }
                }
            }
            check(current.war.structures().isEmpty(),"zero natural H; no duplicate facility identities");
            if(current.sourceColumns()==200){full++;check(applied==987&&padding==19800,"full source edits and padding counted once");check(current.cities.size()==87,"all87 sites");check(current.cities.stream().filter(c->c.kind==World.SiteKind.CITY).count()==42,"42 cities");Reference59Test.siteConnectivity(before,current);}else{crops++;check((current.sourceOriginX&1)==0,"local crop odd-q phase");}
            byte[] bytes=SaveCodec.encode(current);check(bytes[7]==31&&Arrays.equals(bytes,SaveCodec.encode(SaveCodec.decode(bytes))),"new Codec31 exact roundtrip");
            for(int oldRevision:new int[]{59,58}){
                if(oldRevision==58)Reference59Test.restore58(before);
                byte[] old=SaveCodec.encode(before);World loaded=SaveCodec.decode(old);
                check(loaded.mapRevision==oldRevision&&Arrays.equals(old,SaveCodec.encode(loaded)),"old terrain/revision/units retained, never migrated");
                check(NationalMap.compatibilityNotice(loaded).contains("修订61")&&MarchScale.base(loaded,7)==14,"new-game notice and old native200 march budget");
            }
            System.out.println("REFERENCE60 SCENARIO "+s.id+" revised="+applied+" padding="+padding+" old58/59-preserved=true");scenarios++;
        }
        check(scenarios==9&&full==7&&crops==2,"all nine shipped scenarios");
        for(int revision:new int[]{55,62,999}){World w=ScenarioCatalog.load("coalition-190",0,590L);w.mapRevision=revision;boolean rejected=false;try{SaveCodec.validate(w);}catch(IOException e){rejected=true;}check(rejected,"unknown revision explicitly rejected "+revision);}
        Reference59Test.waterEdges();invalidOrigins();rawOldSaves();
        System.out.println("REFERENCE60 CORE PASS: "+checks+" + inherited directed water/port/site checks="+Reference59Test.checks);
    }
    static void invalidOrigins(){
        World w=new World(12,12,"甲","乙");Hex from=new Hex(5,5),to=new Hex(6,5);World.Unit u=new World.Unit(1,0,0,World.Weapon.SPEAR,from,8000,16000);
        for(World.Terrain t:new World.Terrain[]{World.Terrain.VOID,World.Terrain.NON_NAVIGABLE_WATER}){
            w.terrain[from.q][from.r]=t;w.terrain[to.q][to.r]=World.Terrain.PLAIN;
            check(w.army.moveCost(u,from,to)<0,"invalid Q/VOID origin must not escape into land "+t);
            check(w.army.entryCost(u,from,to)<0,"garrison entry also rejects invalid origin "+t);
        }
        check(w.army.moveCost(u,new Hex(-1,0),to)<0,"outside origin rejected");
    }
    static void rawOldSaves()throws Exception {
        String root=System.getProperty("reference60.legacy","");
        if(root.isEmpty()){check(!Boolean.getBoolean("reference60.requireLegacy"),"CI requires actual released APK saves");System.out.println("REFERENCE60 RAW OLD APK SAVES NOT RUN locally: baseline artifacts required");return;}
        Path canonical=Path.of(root,"canonical");
        for(int revision:new int[]{58,59})for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}){
            String name="baseline-v0"+revision+"-"+id+".sg11";Path file=Path.of(root,"raw",name);byte[] raw=Files.readAllBytes(file);World old=SaveCodec.decode(raw);
            check(old.mapRevision==revision&&old.scenarioId.equals(id),"real old APK save identity");
            check(Arrays.equals(SaveCodec.encode(old),Files.readAllBytes(canonical.resolve(name))),"old/new encoder same-JVM canonical bytes "+name);
            check(Arrays.equals(raw,Files.readAllBytes(file)),"no write/delete of real old save");
        }
        System.out.println("REFERENCE60 REAL OLD APK SAVES PASS: eight original v058/v059 Android saves, independent matching JVM encoders");
    }
}
