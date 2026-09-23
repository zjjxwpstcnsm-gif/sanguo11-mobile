package game.sanguo.core;
import game.sanguo.core.map.TerrainCode;
import game.sanguo.core.map.SourceGridCoord;
import game.sanguo.core.army.MarchScale;

import java.io.*;
import java.util.*;

public final class MapTap57Test {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        check(CityArtCatalog.ASSET_REVISION==56&&NationalMap.REVISION==61,"art ABI independent of terrain revision");
        for(World.Terrain t:World.Terrain.values()) {
            TerrainPresentation.Definition d=TerrainPresentation.of(t);
            check(!d.name().isBlank()&&!d.description().isBlank(),"complete presentation "+t);
            check(TerrainCode.decode(TerrainCode.encode(t))==t,"stable source code "+t);
        }
        check(TerrainCode.decode('D')==World.Terrain.MOUNTAIN_PATH,"D is NOT DAM");
        check(TerrainCode.decode('H')==World.Terrain.DAM,"H is DAM");
        check(TerrainCode.decode('R')==World.Terrain.ROAD,"R is ROAD");
        String[] ids={"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"};
        for(String id:ids) {
            World w=ScenarioCatalog.load(id,0,57L);
            Map<World.Terrain,Integer> counts=new EnumMap<>(World.Terrain.class);int padding=0;
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++) {
                Hex h=new Hex(q,r);World.Terrain t=w.terrain[q][r];check(t!=null,"no null terrain");
                if(!w.sourceInside(h)) {padding++;check(t==World.Terrain.VOID&&!w.inside(h),"padding cannot render/select/move");continue;}
                TerrainPresentation.Detail d=TerrainPresentation.detail(w,h);check(d.terrain()==t,"all-source detail");
                check(MapCoordinates.axial(w,d.local()).equals(h),"local roundtrip");
                check(MapCoordinates.fromNationalSource(w,d.national()).equals(h),"national crop roundtrip");
                check(w.inside(h)==(t!=World.Terrain.VOID),"unwalkable terrain remains selectable; only VOID excluded");
                counts.merge(t,1,Integer::sum);
            }
            Set<Hex> structures=new HashSet<>();for(War.Structure s:w.war.structures())check(structures.add(s.hex),"no duplicate structure cells");
            check(w.war.structures().stream().noneMatch(s->s.kind==War.StructureKind.DAM),"no H source -> zero natural dams");
            if(w.sourceColumns()==200) {
                check(padding==19800,"source grid is not axial padding");
                check(counts.get(World.Terrain.VOID)==1065,"audited v061 exact VOID count; inherited fixes retained");
                check(counts.get(World.Terrain.ROAD)==7280&&counts.get(World.Terrain.MOUNTAIN_PATH)==161,"roads/mountain paths not deleted as dams");
                check(w.cities.size()==87,"87 sites preserved");
                check(w.cities.stream().filter(c->c.kind==World.SiteKind.CITY).count()==42,"42 cities preserved");
                for(int x:new int[]{77,81}){Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,76));check(w.terrain[h.q][h.r]==World.Terrain.ROAD&&w.war.at(h)==null&&w.cityAt(h)==null,"actual crash candidates are bare ROAD");}
            }
            byte[] encoded=SaveCodec.encode(w);check(Arrays.equals(encoded,SaveCodec.encode(SaveCodec.decode(encoded))),"current save exact roundtrip "+id);
            System.out.println(id+" source="+w.sourceColumns()+"x"+w.sourceRows()+" padding="+padding+" counts="+counts+" structures="+structures.size());
        }
        World w=ScenarioCatalog.load("coalition-190",0,57L);Hex[] h=MapTap57Fixture.addDangerousObjects(w);
        check(w.war.at(h[0]).owner==-1,"neutral legal owner");check(NaturalStructures.ownerLabel(w,-1).equals("中立设施"),"neutral label");
        check(w.war.at(h[1])==null&&w.terrain[h[1].q][h[1].r]==World.Terrain.DAM,"bare DAM detail fixture");
        check(SaveCodec.decode(SaveCodec.encode(w)).war.structures().size()==3,"save restores, does not materialize bare DAM");
        w.terrain[h[1].q][h[1].r]=World.Terrain.PLAIN;
        check(NaturalStructures.seedOpening(w)==0,"opening seed is idempotent");
        MapTap57Fixture.destroyDam(w,h[0]);
        World loaded=SaveCodec.decode(SaveCodec.encode(w));check(loaded.war.at(h[0])==null&&loaded.terrain[h[0].q][h[0].r]==World.Terrain.SHALLOWS,"destroyed dam does not respawn after save/read");
        w=ScenarioCatalog.load("coalition-190",0,57L);Reference58Test.restore57(w);w.mapRevision=56;
        int[][] fixed={{26,1},{21,3},{30,5},{28,6},{22,9},{30,9},{10,11},{22,11},{20,16},{47,155},{43,158}};
        for(int[] xy:fixed){Hex at=MapCoordinates.axial(w,new SourceGridCoord(xy[0],xy[1]));w.terrain[at.q][at.r]=World.Terrain.VOID;}
        byte[] legacy=SaveCodec.encode(w);World old=SaveCodec.decode(legacy);
        check(Arrays.equals(legacy,SaveCodec.encode(old)),"codec31/revision56 preserved byte-for-byte, no hidden migration");
        check(MarchScale.base(old,7)==14,"legacy native200 preserves march budget despite terrain revision");
        check(old.mapRevision==56&&!NationalMap.compatibilityNotice(old).isEmpty(),"legacy explicit notice");
        check(NationalMap.compatibilityNotice(ScenarioCatalog.load("heroes-250",0,57L)).isEmpty(),"no old-map warning on new world");
        System.out.println("MAP57 CORE PASS: "+checks);
    }
}
