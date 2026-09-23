package game.sanguo.core;
import game.sanguo.core.map.TerrainCode;
import game.sanguo.core.map.SourceGridCoord;
import game.sanguo.core.army.MarchScale;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Only actual resource differences are reversed in historical TEST worlds. No production migration. */
public final class Reference61Test {
 static long checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static List<String[]> changes()throws IOException {
  try(InputStream in=Reference61Test.class.getResourceAsStream("/reference61-corrections.tsv")){
   if(in==null)throw new IOException("Missing audited v061 cells");
   return new BufferedReader(new InputStreamReader(in,"UTF-8")).lines().map(s->s.split("\t")).toList();
  }
 }
 static void restore60(World w)throws IOException {
  check(w.mapRevision==61,"reverse only exact known v061 TEST map");
  for(String[] c:changes()){
   Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1])));
   if(w.sourceInside(h)){check(w.terrain[h.q][h.r]==World.Terrain.NON_NAVIGABLE_WATER,"strict v061 historical terrain postimage");w.terrain[h.q][h.r]=World.Terrain.VOID;}
  }
  w.mapRevision=60;
 }
 static Set<SourceGridCoord> unresolved()throws IOException {
  Set<SourceGridCoord> out=new HashSet<>();
  try(InputStream in=Reference61Test.class.getResourceAsStream("/reference61-unresolved.tsv")){
   if(in==null)throw new IOException("Missing unresolved ledger");
   for(String line:new BufferedReader(new InputStreamReader(in,"UTF-8")).lines().toList()){
    String[] c=line.split("\t");check(out.add(new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1]))),"unique unresolved cell");
   }
  }return out;
 }
 public static void main(String[] args)throws Exception {
  check(NationalMap.REVISION==61&&NationalExterior.REVISION==61&&CityArtCatalog.ASSET_REVISION==56,"versions independent");
  Set<SourceGridCoord> revised=new HashSet<>(),unknown=unresolved();
  for(String[] c:changes()){check(c[2].equals("V")&&c[3].equals("Q"),"no new traversable land or navigation rule change");check(revised.add(new SourceGridCoord(Integer.parseInt(c[0]),Integer.parseInt(c[1]))),"unique source correction");}
  check(revised.size()==206&&unknown.size()==14,"exact ledger, no multiplied era counts");
  int full=0,crops=0;
  for(ScenarioCatalog.Summary scenario:ScenarioCatalog.summaries()){
   World w=ScenarioCatalog.load(scenario.id,0,610L),old=ScenarioCatalog.load(scenario.id,0,610L);restore60(old);
   byte[] snapshot=SaveCodec.encode(w);int changes=0,padding=0,ext=0,voids=0,unresolved=0;
   EnumMap<NationalExterior.Surface,Integer> styles=new EnumMap<>(NationalExterior.Surface.class);
   for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
    Hex h=new Hex(q,r);World.Terrain t=w.terrain[q][r];
    check(t!=null&&TerrainCode.decode(TerrainCode.encode(t))==t,"terrain ABI and all characters");
    if(!w.sourceInside(h)){padding++;check(!w.inside(h)&&NationalExterior.appearance(w,h)==World.Terrain.VOID&&NationalExterior.surface(w,h)==null,"axial padding never scenery, selection or effective source");continue;}
    SourceGridCoord s=MapCoordinates.nationalSource(w,h);check(MapCoordinates.fromNationalSource(w,s).equals(h),"world-aware source roundtrip, including crop parity");
    check(w.inside(h)==(t!=World.Terrain.VOID),"no mutation of sourceInside/inside semantics");
    check(MapCoordinates.display(w,h).equals("全国源坐标 "+s),"one national coordinate label, crops included");
    check(TerrainPresentation.detail(w,h).terrain()==t,"surface detail uses real terrain, not exterior appearance");
    NationalExterior.Surface exterior=NationalExterior.surface(w,h);
    if(t==World.Terrain.VOID)voids++;
    if(exterior!=null){
     ext++;styles.merge(exterior,1,Integer::sum);
     check(t==World.Terrain.VOID&&!w.inside(h)&&old.terrain[q][r]==t,"explicit exterior is NOT restored source terrain");
     check(NationalExterior.surface(old,h)==null&&NationalExterior.appearance(old,h)==World.Terrain.VOID,"new border NOT applied to older saves");
     check(!unknown.contains(s)&&!revised.contains(s),"unknown/corrected never hidden by exterior");
     check(w.cityAt(h)==null&&w.development.cityAt(h)==null&&w.unitAt(h)==null&&w.war.at(h)==null,"no objects/plots/footprints covered");
     World.Terrain prior=t;w.terrain[q][r]=World.Terrain.PLAIN;
     check(NationalExterior.surface(w,h)==null&&NationalExterior.appearance(w,h)==World.Terrain.PLAIN,"valid saved/live terrain always takes priority over mask");w.terrain[q][r]=prior;
    }else if(unknown.contains(s)){unresolved++;check(t==World.Terrain.VOID&&NationalExterior.appearance(w,h)==t,"unresolved holes stay explicit; no all-VOID fallback");}
    else check(NationalExterior.appearance(w,h)==t,"all ordinary terrain kept exact");
    if(revised.contains(s)){
     changes++;check(t==World.Terrain.NON_NAVIGABLE_WATER&&old.terrain[q][r]==World.Terrain.VOID&&w.inside(h),"V->Q actual editable map, selectable but blocked");
     check(w.cityAt(h)==null&&w.development.cityAt(h)==null&&w.unitAt(h)==null&&w.war.at(h)==null,"protected objects retain positions");
    }else check(t==old.terrain[q][r],"every non-ledger character unchanged");
    if(exterior!=null||revised.contains(s)||unknown.contains(s))for(Hex n:h.neighbors())if(w.sourceInside(n))for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()){
     World.Unit u=new World.Unit(-1,0,-1,weapon,n,8000,16000);u.ship=ship;
     check(w.army.moveCost(u,n,h)<0&&w.army.moveCost(u,h,n)<0,"no land/ship/transport route through blocked/restored/exterior/unknown cell");
     check(w.army.entryCost(u,n,h)<0&&w.army.entryCost(u,h,n)<0,"entry cannot bypass blocked endpoint or origin");
    }
   }
   check(Arrays.equals(snapshot,SaveCodec.encode(w)),"render/coordinate/mask queries and tests leave exact world bytes unchanged");
   check(snapshot[7]==31&&Arrays.equals(snapshot,SaveCodec.encode(SaveCodec.decode(snapshot))),"new Codec31 roundtrip");
   byte[] historic=SaveCodec.encode(old);World loaded=SaveCodec.decode(historic);
   check(Arrays.equals(historic,SaveCodec.encode(loaded))&&loaded.mapRevision==60,"old60 terrain/revision/units/state preserved, no relabelling");
   check(NationalMap.compatibilityNotice(loaded).contains("修订61")&&MarchScale.base(loaded,7)==14,"old60 notice, original march budget");
   check(w.war.structures().isEmpty(),"no fabricated natural DAM/wall or duplicate neutral entities");
   if(w.sourceColumns()==200){full++;check(changes==206&&ext==1051&&voids==1065&&unresolved==14&&padding==19800,"exact full-map dispositions");check(styles.get(NationalExterior.Surface.SEA)==869&&styles.get(NationalExterior.Surface.ARID)==126&&styles.get(NationalExterior.Surface.ROCK)==56,"separate exterior styles");
    check(w.cities.size()==87&&w.cities.stream().filter(c->c.kind==World.SiteKind.CITY).count()==42&&w.cities.stream().filter(c->c.kind==World.SiteKind.GATE).count()==10,"all protected sites, city seven-cell mechanism preserved");Reference59Test.siteConnectivity(old,w);
   }else{crops++;check(ext==0,"local crop edges are not national exterior masks");}
   System.out.println("REFERENCE61 SCENARIO "+scenario.id+" changes="+changes+" exterior="+ext+" unresolved="+unresolved+" padding="+padding);
  }
  check(full==7&&crops==2,"all shipped scenarios");
  for(int rev:new int[]{55,62,999}){World w=ScenarioCatalog.load("coalition-190",0,610L);w.mapRevision=rev;boolean reject=false;try{SaveCodec.validate(w);}catch(IOException e){reject=true;}check(reject,"unknown map revision rejected: "+rev);}
  Reference60Test.invalidOrigins();Reference59Test.waterEdges();realLegacy();
  System.out.println("REFERENCE61 CORE PASS: "+checks+" inherited port/connectivity checks="+Reference59Test.checks);
 }
 static void realLegacy()throws Exception {
  String root=System.getProperty("reference61.legacy","");
  if(root.isEmpty()){check(!Boolean.getBoolean("reference61.requireLegacy"),"CI requires actual old APK files");System.out.println("REFERENCE61 REAL OLD FILES NOT RUN locally; CI must use actual 58/59/60 APK artifacts");return;}
  for(int rev:new int[]{58,59,60})for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}){
   String name="baseline-v0"+rev+"-"+id+".sg11";Path p=Path.of(root,"raw",name);byte[] raw=Files.readAllBytes(p);World old=SaveCodec.decode(raw);
   check(old.mapRevision==rev&&old.scenarioId.equals(id),"authentic save identity");check(Arrays.equals(SaveCodec.encode(old),Files.readAllBytes(Path.of(root,"canonical",name))),"same-JVM exact old/new encoder for actual APK save "+name);
   for(int q=0;q<old.width;q++)for(int r=0;r<old.height;r++)check(NationalExterior.surface(old,new Hex(q,r))==null,"no new mask on real old saves");
   check(MarchScale.base(old,7)==14&&Arrays.equals(raw,Files.readAllBytes(p)),"old movement budget and raw file retained");
  }
  System.out.println("REFERENCE61 REAL OLD APK FILES PASS: 12 actual Android saves, 3 independent original encoders, no migration");
 }
}
