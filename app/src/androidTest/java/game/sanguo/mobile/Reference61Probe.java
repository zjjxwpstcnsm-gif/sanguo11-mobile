package game.sanguo.mobile;
import android.app.*;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;
/** Test APK only. Actual normal MapView, paired whole-region camera poses, not a custom renderer. */
final class Reference61Probe extends Reference60Probe {
 Reference61Probe(Instrumentation test){super(test);}
 @Override void run()throws Exception {
  try{
   for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}){
    launch(ScenarioCatalog.load(id,id.endsWith("sandbox")?0:5,610L));World w=world();byte[] original=SaveCodec.encode(w);
    require(w.mapRevision==61&&BuildConfig.VERSION_CODE==61,"actual final v061 identity");navigator(false);
    int[][] centers={{28,22},{169,167},{12,12},{36,12},{52,9},{26,26},{187,145},{185,174},{174,191},{157,178},{42,134},{47,134}};
    for(int n=0;n<centers.length;n++){
     Hex h=national(centers[n][0],centers[n][1]);if(!w.sourceInside(h))continue;
     float zoom=n<2?.30f:n<10?.9f:3.4f;focus(h,zoom);navigator(false);record(h,screen(h));
     report.append("VIEWPORT "+id+" center="+centers[n][0]+","+centers[n][1]+" camera="+camera().centerX()+","+camera().centerY()+" scale="+camera().scale+" size="+map().getWidth()+","+map().getHeight()+"\n");
     shot("after-"+id+"-"+centers[n][0]+"-"+centers[n][1]+"-"+zoom);
     if(n<2){coverage(n==0);ui(()->map().setTerritoryMode(1));settle();shot("after-"+id+"-"+centers[n][0]+"-"+centers[n][1]+"-territory");ui(()->map().setTerritoryMode(0));settle();}
    }
    ui(()->{camera().fit();map().invalidate();});settle();shot("after-"+id+"-national-fit");
    visualBuffers();
    if(w.sourceColumns()==200){
     for(int[] xy:new int[][]{{0,0},{28,6},{39,11},{195,160},{190,190},{35,14},{37,22}}){Hex h=national(xy[0],xy[1]);if(w.terrain[h.q][h.r]!=World.Terrain.VOID)continue;reject(h,"scoped exterior/explicit unresolved "+Arrays.toString(xy));shot("v061-"+id+"-blocked-"+xy[0]+"-"+xy[1]);}
     for(int[] xy:new int[][]{{35,8},{45,10},{168,163},{145,183},{42,134},{47,134}}){Hex h=national(xy[0],xy[1]);require(w.terrain[h.q][h.r]==World.Terrain.NON_NAVIGABLE_WATER,"actual new v061 water");pick(h,3.4f,"v061 corrected source");showPanel();require(panelText().contains(MapCoordinates.display(w,h)),"detail uses national source coordinate label");shot("v061-"+id+"-Q-"+xy[0]+"-"+xy[1]);}
     // Water/terrain sampling is real; no forced land or navigation flags are introduced.
     Hex water=new MapTap57Probe(test).find(w,World.Terrain.WATER,false);pick(water,3.4f,"existing navigable WATER unchanged");
     minimap(national(168,163),national(195,160));
     // (28,6) is not in the exterior ledger; preserve its real terrain instead of weakening the assertion.
     require(NationalExterior.surface(w,national(28,10))==NationalExterior.Surface.ARID,"reviewed NW arid sample source 28,10");
     require(NationalExterior.surface(w,national(28,21))==NationalExterior.Surface.ROCK,"reviewed NW rock sample source 28,21");
     pixelScenery(national(195,160));pixelScenery(national(28,10));pixelScenery(national(28,21));
     actualPaddingTouch();
    }else{
     for(int edge=0;edge<4;edge++)pick(new MapTap57Probe(test).edge(w,edge),3.4f,"local crop edge, not national exterior");
     Hex h=national(123,101);if(w.inside(h)){pick(h,3.4f,"crop coordinate identity");showPanel();require(panelText().contains(MapCoordinates.display(w,h)),"crop also displays national source, not unlabelled local");shot("v061-"+id+"-source-label");}
    }
    preview(w,id);require(Arrays.equals(original,SaveCodec.encode(w)),"whole-area inspection/preview/LOD/touches leave exact state unchanged");
   }
   regionalMarch();
   report.append("SCOPE: same paired centers/scales as run35554158663, 2 full scenarios + both local crops. Scenery no-owner checks cover every scoped cell in actual overview/minimap buffers. Physical ARM/FPS not certified.\n");
   flush("v061-corners-checks.txt");
   // Reuse the inherited real controls and legal test-only fixtures, not a parallel game engine.
   super.run();
  }finally{flush("v061-installed-checks.txt");}
 }
 void coverage(boolean northwest)throws Exception {
  World w=world();int total=0,visible=0;
  for(int x=0;x<200;x++)for(int y=0;y<200;y++){
   NationalExterior.Surface s=NationalExterior.sourceSurface(x,y);if(s==null||(s==NationalExterior.Surface.SEA)==northwest)continue;
   Hex h=national(x,y);if(!w.sourceInside(h))continue;total++;float[] p=screen(h);
   if(p[0]>=0&&p[0]<map().getWidth()&&p[1]>=0&&p[1]<map().getHeight())visible++;
  }
  require(visible==total,"whole "+(northwest?"NW":"SE")+" explicit exterior in paired viewport "+visible+"/"+total);
 }
 void visualBuffers()throws Exception {
  World w=world();MapRaster raster=(MapRaster)field(map(),"miniRaster");Bitmap mini=(Bitmap)field(map(),"miniTerrain"),ownership=(Bitmap)field(map(),"miniTerritory");Object overview=field(map(),"overview");
  int[] ground=(int[])field(overview,"terrain"),sites=(int[])field(overview,"sites"),owners=(int[])field(overview,"owners");int n=0,unknown=0;
  for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
   Hex h=new Hex(q,r);NationalExterior.Surface s=NationalExterior.surface(w,h);int index=r*w.width+q;
   if(s!=null){n++;int px=raster.left(h),py=raster.top(h);
    require(ground[index]==TerrainTiles.color(s.appearance)&&sites[index]==-1&&owners[index]==-1,"actual overview ground without owner/site at "+MapCoordinates.nationalSource(w,h));
    require(mini.getPixel(px,py)==TerrainTiles.color(s.appearance)&&Color.alpha(ownership.getPixel(px,py))==0,"actual mini terrain painted; no territory overlay");
    require(raster.at(raster.rasterX(raster.worldX(h)),raster.rasterY(raster.worldY(h)))==null,"actual mini raster rejects scenery");
   }else if(w.sourceInside(h)&&w.terrain[q][r]==World.Terrain.VOID){unknown++;require(ground[index]==0,"unknown VOID is not disguised by overview");}
  }
  require(w.sourceColumns()==200?n==1051&&unknown==14:n==0,"actual renderer complete mask count, crop exclusions");
 }
 void pixelScenery(Hex h)throws Exception {
  require(NationalExterior.surface(world(),h)!=null,"pixel sample is reviewed exterior");
  for(float scale:new float[]{.30f,.9f,3.4f}){
   focus(h,scale);navigator(false);
   int[] colors=new int[3];for(int m=0;m<3;m++){final int mode=m;ui(()->map().setTerritoryMode(mode));settle();
    Bitmap b=test.getUiAutomation().takeScreenshot();int[] loc=new int[2];map().getLocationOnScreen(loc);float[] p=screen(h);
    colors[m]=b.getPixel(Math.round(loc[0]+p[0]),Math.round(loc[1]+p[1]));b.recycle();
   }
   require(colors[0]==colors[1]&&colors[1]==colors[2]&&colors[0]!=MapOverview.BACKGROUND,"actual screen exterior pixel no faction tint / no background hole, LOD="+scale);
   shot("v061-"+world().scenarioId+"-exterior-"+MapCoordinates.nationalSource(world(),h)+"-LOD-"+scale);ui(()->map().setTerritoryMode(0));settle();
  }
 }
 void actualPaddingTouch()throws Exception {
  ui(()->{camera().fit();map().invalidate();});settle();navigator(false);boolean tapped=false;
  World w=world();outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
   Hex h=new Hex(q,r);if(w.sourceInside(h))continue;float[] p=screen(h);
   if(p[0]>map().getWidth()*.1f&&p[0]<map().getWidth()*.9f&&p[1]>map().getHeight()*.12f&&p[1]<map().getHeight()*.85f){tap(h);require(field(map(),"touchHex")==null&&field(activity,"selected")==null,"actual reachable padding tap never falls back to city");tapped=true;break outer;}
  }
  report.append("Actual on-screen axial padding tap available="+tapped+"; exhaustive pure source/projection padding checks also retained.\n");
  // Source-frame padding may be outside camera limits. Never manufacture a passing offscreen pointer.
 }
 void preview(World w,String id)throws Exception {
  ScenarioFactionPicker[] p={null};ui(()->{p[0]=new ScenarioFactionPicker(activity,w,side->{});p[0].show();});settle();
  MapView preview=(MapView)field(p[0],"map");World pw=(World)field(preview,"world");require(pw==w,"real production opening picker uses same authoritative world");
  Object ov=field(preview,"overview");int[] colors=(int[])field(ov,"terrain"),sites=(int[])field(ov,"sites");int n=0;
  for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
   NationalExterior.Surface s=NationalExterior.surface(w,new Hex(q,r));if(s!=null){n++;require(colors[r*w.width+q]==TerrainTiles.color(s.appearance)&&sites[r*w.width+q]==-1,"production opening preview shares scenery and territory exclusion");}
  }
  require(w.sourceColumns()==200?n==1051:n==0,"opening preview scopes national versus local edges");shot("v061-"+id+"-production-opening-preview");
  Dialog dialog=(Dialog)field(p[0],"dialog");ui(dialog::dismiss);settle();
 }
 void regionalMarch()throws Exception {
  World w=ScenarioCatalog.load("coalition-190",5,610L);World.City home=w.home();World.Officer officer=w.idle(home).get(0);
  require(w.army.deploy(home.id,officer.id,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,18000,0).ok,"v061 region legal army deployment");World.Unit unit=w.unit(officer.unitId);
  File oldFile=new File(test.getTargetContext().getExternalFilesDir(null),"legacy60/baseline-v060-coalition-190.sg11");byte[] oldRaw;try(InputStream in=new FileInputStream(oldFile);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[16384];for(int k;(k=in.read(b))!=-1;)out.write(b,0,k);oldRaw=out.toByteArray();}World old=SaveCodec.decode(oldRaw);
  Hex target=null,start=null,water=null;outer:for(int x=0;x<60;x++)for(int y=0;y<40;y++){
   Hex q=MapCoordinates.fromNationalSource(w,new SourceGridCoord(x,y));if(w.terrain[q.q][q.r]!=World.Terrain.NON_NAVIGABLE_WATER||old.terrain[q.q][q.r]!=World.Terrain.VOID)continue;
   for(Hex t:q.neighbors())if(w.inside(t)&&w.cost(t,unit.weapon)>0&&w.cityAt(t)==null&&w.unitAt(t)==null&&w.domestic.at(t)==null)
    for(Hex a:t.neighbors())if(w.inside(a)&&w.cost(a,unit.weapon)>0&&w.cityAt(a)==null&&w.unitAt(a)==null&&w.domestic.at(a)==null&&w.army.moveCost(unit,a,t)>0){water=q;target=t;start=a;break outer;}
  }
  require(start!=null,"existing passable approach beside actual new v061 NW water");unit.hex=start;final Hex destination=target,blocked=water;int id=unit.id;
  report.append("TEST-ONLY MARCH PLACEMENT: legal deployed unit moved to empty existing land "+MapCoordinates.nationalSource(w,start)+" -> "+MapCoordinates.nationalSource(w,target)+" beside new61 Q "+MapCoordinates.nationalSource(w,water)+"; no terrain/site/owner changes.\n");
  launch(w);ui(()->{activity.selectUnitAndFocus(id);page();});settle();focus(blocked,3.4f);click("行军");tap(blocked);
  MarchOrders.Plan bad=(MarchOrders.Plan)field(activity,"pendingMarch");require(bad!=null&&!bad.valid()&&!button(activity.getWindow().getDecorView(),"确认任务").isEnabled(),"actual new61 Q cannot be confirmed as path/target");shot("v061-new-water-march-rejected");click("取消");
  focus(destination,3.4f);click("行军");tap(destination);MarchOrders.Plan plan=(MarchOrders.Plan)field(activity,"pendingMarch");require(plan!=null&&plan.valid(),"existing adjacent land remains reachable");
  require(plan.label.contains(MapCoordinates.display(world(),destination)),"march title now national source, same as terrain detail");shot("v061-source-coordinate-march-preview");click("确认任务");
  require(world().unit(id).hex.equals(destination),"actual legal march completed beside new v061 region");World saved=SaveCodec.decode(readInternal("auto.sg11"));require(saved.mapRevision==61&&saved.unit(id).hex.equals(destination),"real autosave preserves current terrain and position");launch(saved);
  require(world().unit(id)!=null&&world().unit(id).hex.equals(destination),"reloaded actual unit retains its saved source position");
  require("select".equals(field(activity,"unitCommand")),"completed march is not restored as an unfinished command");
  ClientState restoredUi=(ClientState)field(activity,"ui");
  report.append("RELOADED UNIT SELECTION: selected="+field(activity,"selected")+" selectedUnit="+restoredUi.selectedUnit+" expectedUnit="+id+"\n");
  // Production restores matching UI hints. A tap on an already-selected unit deliberately toggles it off.
  if(destination.equals(field(activity,"selected"))&&restoredUi.selectedUnit==id){
   focus(destination,3.4f);navigator(false);tap(destination);
   require(field(activity,"selected")==null&&((ClientState)field(activity,"ui")).selectedUnit==-1,"real tap toggles restored unit selection off");
  }
  pick(destination,3.4f,"post-exit reload regional unit");
  require(((ClientState)field(activity,"ui")).selectedUnit==id,"real map tap selects exact reloaded unit identity");
  shot("v061-regional-march-reloaded");
 }
}
