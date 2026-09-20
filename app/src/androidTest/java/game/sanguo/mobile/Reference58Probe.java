package game.sanguo.mobile;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Test APK only. Fresh shipped worlds, real window pointer dispatch and raw old saves. */
final class Reference58Probe extends MapTap57Harness {
    Reference58Probe(Instrumentation t){super(t);}
    private Hex national(int x,int y)throws Exception{return MapCoordinates.fromNationalSource(world(),new SourceGridCoord(x,y));}
    void run()throws Exception {
        try {
            oldSaves();
            for(String id:new String[]{"coalition-190","heroes-250"}) {
                launch(ScenarioCatalog.load(id,5,580L));World w=world();byte[] before=SaveCodec.encode(w);
                require(w.mapRevision==58&&BuildConfig.VERSION_CODE==58,"fresh revision58 from this APK");
                require(w.war.structures().isEmpty(),"shipped natural dam count remains zero");
                // Same coordinates, camera scales and seed as the independent v057 sampler.
                for(int[] xy:new int[][]{{110,79},{182,109},{156,77},{46,128},{47,155},{26,1},{109,53},{77,76}}) {
                    Hex h=national(xy[0],xy[1]);focus(h,3.4f);navigator(false);record(h,screen(h));
                    shot("after-"+id+"-"+xy[0]+"-"+xy[1]+"-near");
                    if(w.inside(h))pick(h,3.4f,"paired coordinate");else reject(h,"paired source VOID");
                    focus(h,.9f);shot("after-"+id+"-"+xy[0]+"-"+xy[1]+"-mid");
                }
                for(int[] xy:new int[][]{{110,75},{102,85},{174,107},{46,121},{47,155},{43,158},{108,42},{112,84},{101,81}}) {
                    Hex h=national(xy[0],xy[1]);pick(h,3.4f,"reviewed correction "+Arrays.toString(xy));
                    showPanel();shot("v058-"+id+"-corrected-"+xy[0]+"-"+xy[1]);
                    if(w.terrain[h.q][h.r]==World.Terrain.NON_NAVIGABLE_WATER)require(panelText().contains("不可航水域")&&w.cost(h,World.Weapon.SPEAR)<0,"water is visible/selectable but not a new passage");
                }
                MapTap57Probe samples=new MapTap57Probe(test);
                for(World.Terrain t:new World.Terrain[]{World.Terrain.ROAD,World.Terrain.MOUNTAIN_PATH,World.Terrain.PLANK_ROAD,World.Terrain.MOUNTAIN}) {
                    Hex h=samples.find(w,t,false);require(h!=null,"actual shipped terrain sample "+t);pick(h,3.4f,"actual "+t);showPanel();shot("v058-"+id+"-"+t);
                }
                Hex road=samples.find(w,World.Terrain.ROAD,true);require(road!=null,"unobscured distant road sample");
                for(float lod:new float[]{.15f,.9f,3.4f}) {pick(road,lod,"retained ROAD at LOD "+lod);shot("v058-"+id+"-LOD-"+lod);}
                navigator(false);
                for(int corner=0;corner<4;corner++)pick(samples.corner(w,corner),3.4f,"national valid corner "+corner);
                for(int edge=0;edge<4;edge++)pick(samples.edge(w,edge),3.4f,"national valid edge "+edge);
                for(int[] xy:new int[][]{{0,0},{199,0},{0,199},{199,199},{199,100},{100,199}}) {
                    Hex h=national(xy[0],xy[1]);if(w.inside(h))pick(h,3.4f,"exact boundary");else reject(h,"exact boundary VOID");
                }
                Hex hole=samples.find(w,World.Terrain.VOID,false);reject(hole,"remaining source VOID");
                Hex padding=new Hex(0,0);require(!w.sourceInside(padding),"explicit axial-padding sample");
                // MapView inverse guard is checked via actual touches for reachable source VOID;
                // off-screen padding coordinates are exhaustively covered by pure projection tests.
                minimap(national(174,107),hole);
                focus(road,.9f);navigator(false);float cx=camera().centerX();pan();require(Math.abs(camera().centerX()-cx)>1,"real pointer pan changes camera");
                float scale=camera().scale;pinch();require(camera().scale>scale,"real two-finger pinch changes scale");
                pick(road,3.4f,"after real pan/pinch");
                for(int mode=0;mode<3;mode++){final int m=mode;ui(()->{map().setTerritoryMode(m);map().invalidate();});settle();shot("v058-"+id+"-territory-"+mode);}
                require(Arrays.equals(before,SaveCodec.encode(w)),"sampling/LOD/minimap/territory did not change authoritative state");
            }
            for(String id:new String[]{"central-mobile-sandbox","jingxiang-mobile-sandbox"}) {
                launch(ScenarioCatalog.load(id,0,580L));World w=world();navigator(false);MapTap57Probe samples=new MapTap57Probe(test);
                for(int edge=0;edge<4;edge++)pick(samples.edge(w,edge),3.4f,"crop "+id+" edge="+edge);
                for(int corner=0;corner<4;corner++)pick(samples.corner(w,corner),3.4f,"crop corner "+corner);
                Hex water=samples.find(w,World.Terrain.NON_NAVIGABLE_WATER,false);require(water!=null,"crop includes reviewed water");pick(water,3.4f,"crop corrected water");
                minimap(water,samples.find(w,World.Terrain.VOID,false));shot("v058-"+id+"-crop");
            }
            World fixture=ScenarioCatalog.load("coalition-190",5,580L);Hex[] objects=MapTap57Fixture.addDangerousObjects(fixture);
            report.append("SYNTHETIC TEST-ONLY: DAM entity / bare DAM terrain / EARTH_WALL / STONE_WALL. Not naturally found in shipped geography, not packaged in app.\n");
            launch(fixture);for(int i=0;i<objects.length;i++){pick(objects[i],3.4f,"labelled fixture "+i);showPanel();shot("v058-fixture-"+i);if(i==0)require(panelText().contains("中立设施"),"neutral DAM identity retained");}
            World saved=SaveCodec.decode(SaveCodec.encode(world()));require(saved.war.structures().size()==3,"fixture structures save exactly once");
            launch(saved);for(Hex h:objects)pick(h,3.4f,"fixture after save/read");
            correctedMarch();
            report.append("TESTED APK "+BuildConfig.VERSION_NAME+"/"+BuildConfig.VERSION_CODE+" source="+BuildConfig.SOURCE_REVISION+"; API29 x86_64, not physical ARM.\n");
        } finally {flush("v058-installed-checks.txt");}
    }
    private void oldSaves()throws Exception {
        for(String id:new String[]{"coalition-190","heroes-250"}) {
            File source=new File(test.getTargetContext().getExternalFilesDir(null),"legacy58/baseline-v057-"+id+".sg11");byte[] original;
            try(InputStream in=new FileInputStream(source);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[16384];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);original=out.toByteArray();}
            World old=SaveCodec.decode(original);byte[] expected=SaveCodec.encode(old);
            if(activity!=null){ui(activity::finish);settle();}
            writeInternal("manual1.sg11",original);writeInternal("auto.sg11",original);
            activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();ui(this::page);settle();
            require(world().mapRevision==57&&Arrays.equals(expected,SaveCodec.encode(world())),"actual baseline APK raw save loads without terrain/units/cities migration "+id);
            require(world().terrain[national(110,75).q][national(110,75).r]==World.Terrain.VOID,"old black-channel terrain is not silently replaced");
            require(MarchScale.base(world(),7)==14,"old native200 movement budget retained");
            View notice=activity.getWindow().getDecorView().findViewWithTag("map.revision.notice");require(notice!=null&&notice.getVisibility()==View.VISIBLE,"old-map new-game notice visible");
            pick(national(77,76),3.4f,"actual legacy ROAD");require(Arrays.equals(expected,SaveCodec.encode(world())),"legacy selection leaves canonical state unchanged");
            require(Arrays.equals(original,readInternal("manual1.sg11")),"original manual file is not overwritten/deleted");shot("v058-legacy-"+id);
        }
    }
    private void pick(Hex h,float scale,String label)throws Exception {
        focus(h,scale);navigator(false);tap(h);require(h.equals(field(activity,"selected")),"actual MapView callback "+label+" -> "+h);
        World w=world();World.Unit unit=w.unitAt(h);World.City city=w.cityAt(h);Domestic.Facility facility=w.domestic.at(h);War.Structure structure=w.war.at(h);
        String expected=unit!=null?w.officer(unit.officerId).name:city!=null?city.name:facility!=null?facility.kind.label:structure!=null?structure.kind.label:TerrainPresentation.of(w.terrain[h.q][h.r]).name();
        String panel=panelText();report.append("CALLBACK "+field(activity,"selected")+" expected="+expected+" city="+(city==null?"none":city.id)+" PANEL "+panel+"\n");
        if(!panel.contains(expected))shot("v058-detail-failure");
        require(panel.contains(expected),"actual object-aware detail title "+expected);
    }
    private void reject(Hex h,String label)throws Exception {
        focus(h,3.4f);navigator(false);Object prior=field(activity,"selected");tap(h);
        Object actual=field(activity,"selected"),hit=field(map(),"touchHex");
        report.append("REJECT "+label+" prior="+prior+" actual="+actual+" MapView.touchHex="+hit+"\n");
        if(actual!=null||hit!=null)shot("v058-rejected-hit-failure");
        require(hit==null,"MapView delivers null, never a VOID/padding or label target "+label);
        require(actual==null,"existing blank-map tap clears selection "+label);
    }
    private void minimap(Hex target,Hex hole)throws Exception {
        focus(target,3.4f);navigator(true);MapRaster raster=(MapRaster)field(map(),"miniRaster");RectF r=(RectF)field(map(),"miniRect");
        float expectedX=camera().centerX(),expectedY=camera().centerY();
        require(target.equals(raster.at(raster.rasterX(raster.worldX(target)),raster.rasterY(raster.worldY(target)))),"navigator projected point identifies the exact source tile before camera clamping");
        // Move away first, then use an actual navigator pointer at the shared projected center.
        ui(()->{camera().pan(120,-160);map().invalidate();});settle();
        float mx=r.left+raster.rasterX(raster.worldX(target))/raster.width*r.width(),my=r.top+raster.rasterY(raster.worldY(target))/raster.height*r.height();
        tapPoint(mx,my);require(Math.abs(camera().centerX()-expectedX)<.2f&&Math.abs(camera().centerY()-expectedY)<.2f,"minimap jump equals direct exact-tile focus including column parity and legitimate camera clamps");
        if(hole!=null){float cx=camera().centerX(),cy=camera().centerY();tapPoint(r.left+raster.rasterX(raster.worldX(hole))/raster.width*r.width(),r.top+raster.rasterY(raster.worldY(hole))/raster.height*r.height());require(Math.abs(camera().centerX()-cx)<.2f&&Math.abs(camera().centerY()-cy)<.2f,"minimap does not jump to source VOID");}
        shot("v058-"+world().scenarioId+"-shared-minimap");navigator(false);
    }
    private void correctedMarch()throws Exception {
        World w=ScenarioCatalog.load("coalition-190",5,580L);World.City home=w.home();World.Officer officer=w.idle(home).get(0);
        require(w.army.deploy(home.id,officer.id,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,12000,0).ok,"march fixture uses legal actual deployment");
        World.Unit unit=w.unit(officer.unitId);Hex target=MapCoordinates.fromNationalSource(w,new SourceGridCoord(108,42)),start=null;
        for(Hex h:target.neighbors())if(w.inside(h)&&w.cityAt(h)==null&&w.unitAt(h)==null&&w.army.moveCost(unit,h,target)>0&&w.cost(h,unit.weapon)>0){start=h;break;}
        require(start!=null,"reviewed old plank-road has valid adjacent land");unit.hex=start;
        report.append("SYNTHETIC MARCH SETUP: legally deployed army relocated to adjacent unused land solely to exercise the actual MOVE UI across reviewed (108,42); no terrain/site substitutions.\n");
        launch(w);final int id=unit.id;ui(()->{activity.selectUnitAndFocus(id);page();});settle();focus(target,3.4f);click("行军");tap(target);
        MarchOrders.Plan plan=(MarchOrders.Plan)field(activity,"pendingMarch");require(plan!=null&&plan.valid(),"real MOVE preview crosses corrected source cell");shot("v058-corrected-march-preview");click("确认任务");
        require(world().unit(id)!=null&&world().unit(id).hex.equals(target),"real MOVE confirmation reaches reviewed terrain");
        World loaded=SaveCodec.decode(readInternal("auto.sg11"));require(loaded.unit(id).hex.equals(target)&&loaded.mapRevision==58,"actual saved march retains revised map and unit position");shot("v058-corrected-march-completed");
    }
    private void click(String label)throws Exception {View v=button(activity.getWindow().getDecorView(),label);require(v!=null,"real button "+label);ui(v::performClick);settle();}
    private View button(View v,String label){if(v instanceof Button&&((Button)v).getText().toString().equals(label)&&v.getVisibility()==View.VISIBLE)return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View b=button(g.getChildAt(i),label);if(b!=null)return b;}}return null;}
}
