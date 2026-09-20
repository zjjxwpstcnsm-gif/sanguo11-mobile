package game.sanguo.mobile;
import android.app.Instrumentation;
import android.view.View;
import game.sanguo.core.*;
import java.util.*;

/** Stratified actual touches on shipped scenarios; synthetic DAM/walls explicitly labelled. */
final class MapTap57Probe extends MapTap57Harness {
    MapTap57Probe(Instrumentation t){super(t);}
    void run()throws Exception{
        try{
            byte[] old=readInternal("manual1.sg11");World legacy=SaveCodec.decode(old);
            require(legacy.mapRevision==56&&Arrays.equals(old,SaveCodec.encode(legacy)),"real baseline APK save accepted byte-for-byte");
            launch(legacy);View notice=activity.getWindow().getDecorView().findViewWithTag("map.revision.notice");
            require(notice!=null&&notice.getVisibility()==View.VISIBLE,"visible old-map notice, no silent migration");
            pick(MapCoordinates.fromNationalSource(world(),new SourceGridCoord(77,76)),3.4f,"legacy-ROAD");showPanel();shot("v057-legacy-ROAD-fixed");
            require(Arrays.equals(old,SaveCodec.encode(world())),"old world unmoved after actual fixed ROAD touch");
            require(Arrays.equals(old,readInternal("manual1.sg11")),"original manual save file not overwritten");
            for(String id:new String[]{"coalition-190","heroes-250"}){
                launch(ScenarioCatalog.load(id,5,57L));World w=world();byte[] before=SaveCodec.encode(w);
                require(w.mapRevision==57&&w.war.structures().isEmpty(),"fresh native200 has zero dams; D is mountain path");
                for(int x:new int[]{77,81})pick(MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,76)),3.4f,id+"-original-ROAD");
                for(World.Terrain t:World.Terrain.values()){
                    if(t==World.Terrain.VOID||t==World.Terrain.DAM||t==World.Terrain.SEA)continue;
                    Hex h=find(w,t,false);if(h==null){report.append("NOT_PRESENT "+id+" "+t+'\n');continue;}
                    pick(h,3.4f,id+"-"+t);if(t==World.Terrain.ROAD||t==World.Terrain.MOUNTAIN_PATH||t==World.Terrain.PLANK_ROAD){showPanel();shot("v057-"+id+"-"+t);}
                }
                Hex road=find(w,World.Terrain.ROAD,true);require(road!=null,"distant ROAD sample avoiding far city-label hit areas");
                for(float lod:new float[]{.15f,.9f,3.4f}){pick(road,lod,id+"-LOD"+lod);shot("v057-"+id+"-road-lod-"+lod);}
                focus(road,.9f);float previous=camera().centerX();pan();require(Math.abs(camera().centerX()-previous)>1,"actual pointer drag pans");float scale=camera().scale;pinch();require(camera().scale>scale,"actual two-finger pinch zooms");
                pick(road,3.4f,id+"-after-pan-pinch");
                for(int[] xy:new int[][]{{26,1},{21,3},{30,5},{28,6},{22,9},{30,9},{10,11},{22,11},{20,16},{47,155},{43,158}}){
                    Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(xy[0],xy[1]));require(w.inside(h),"explicit repaired source visible");pick(h,3.4f,id+"-estimated-repair");
                }
                shot("v057-"+id+"-repaired-mountain");
                Hex voidCell=find(w,World.Terrain.VOID,false);require(voidCell!=null,"remaining VOID sample");focus(voidCell,3.4f);tap(voidCell);require(!voidCell.equals(field(activity,"selected")),"true source VOID still excluded, not made playable");
                require(Arrays.equals(before,SaveCodec.encode(w)),"terrain/UI/gestures never alter authoritative world");
            }
            for(String id:new String[]{"central-mobile-sandbox","jingxiang-mobile-sandbox"}){
                launch(ScenarioCatalog.load(id,0,57L));World w=world();
                for(int edge=0;edge<4;edge++){Hex h=edge(w,edge);pick(h,3.4f,id+"-edge-"+edge);}
                shot("v057-"+id+"-crop-edge");
            }
            World fixture=ScenarioCatalog.load("coalition-190",5,57L);Hex[] danger=MapTap57Fixture.addDangerousObjects(fixture);
            report.append("SYNTHETIC_FIXTURE: neutral DAM entity, bare DAM terrain, EARTH_WALL and STONE_WALL, on unchanged real map except four explicitly injected test cells. No official H source exists.\n");
            launch(fixture);
            for(int i=0;i<danger.length;i++){pick(danger[i],3.4f,"fixture-"+i);showPanel();shot("v057-fixture-danger-"+i);if(i==0)require(panelText().contains("中立设施"),"neutral owner=-1 correct label and icon");}
            World saved=SaveCodec.decode(SaveCodec.encode(world()));require(saved.war.structures().size()==3,"saved fixtures restored once; bare DAM not auto duplicated");
            launch(saved);for(Hex h:danger)pick(h,3.4f,"fixture-after-save");
            report.append("Final build "+BuildConfig.VERSION_NAME+"/"+BuildConfig.VERSION_CODE+" source="+BuildConfig.SOURCE_REVISION+"; API29 x86_64 emulator, not physical ARM.\n");
        }finally{flush("v057-installed-checks.txt");}
    }
    void pick(Hex h,float scale,String label)throws Exception{
        focus(h,scale);tap(h);require(h.equals(field(activity,"selected")),"normal MapView callback selects "+label+" "+h);
        World w=world();War.Structure s=w.war.at(h);String expected=s==null?TerrainPresentation.of(w.terrain[h.q][h.r]).name():s.kind.label;
        require(panelText().contains(expected),"real detail panel contains "+expected+" "+label);
    }
    Hex find(World w,World.Terrain t,boolean away)throws Exception{
        for(int y=0;y<w.sourceRows();y++)for(int x=0;x<w.sourceColumns();x++){
            Hex h=MapCoordinates.axial(w,new SourceGridCoord(x,y));if(w.terrain[h.q][h.r]!=t||w.cityAt(h)!=null||w.unitAt(h)!=null||w.domestic.at(h)!=null||w.war.at(h)!=null)continue;
            if(away){boolean near=false;for(World.City c:w.cities)if(c.hex.distance(h)<32){near=true;break;}if(near||x<20||y<20||x>=w.sourceColumns()-20||y>=w.sourceRows()-20)continue;}
            return h;
        }return null;
    }
    Hex edge(World w,int edge){
        for(int k=0;k<Math.max(w.sourceColumns(),w.sourceRows());k++)for(int d=0;d<12;d++){
            int x=edge==0?d:edge==1?w.sourceColumns()-1-d:k;
            int y=edge==2?d:edge==3?w.sourceRows()-1-d:k;
            if(x<0||y<0||x>=w.sourceColumns()||y>=w.sourceRows())continue;
            Hex h=MapCoordinates.axial(w,new SourceGridCoord(x,y));if(w.inside(h)&&w.cityAt(h)==null&&w.unitAt(h)==null&&w.domestic.at(h)==null&&w.war.at(h)==null)return h;
        }throw new AssertionError("no valid edge "+edge);
    }
}
