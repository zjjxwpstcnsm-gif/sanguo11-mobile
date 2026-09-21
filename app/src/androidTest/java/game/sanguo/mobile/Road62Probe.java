package game.sanguo.mobile;

import android.app.Instrumentation;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Debug;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** The same test APK runs against the exact released v061 and the road-only candidate.
 * The candidate reads four saves actually encoded by the installed old APK.
 * This is NOT approval of the unfinished VOID or generated-art scope. */
final class Road62Probe extends MapTap57Harness {
    private final boolean candidate;
    Road62Probe(Instrumentation test, boolean candidate){super(test);this.candidate=candidate;}
    private String phase(){return candidate?"after":"before";}
    void run() throws Exception {
        try {
            PackageInfo installed=test.getTargetContext().getPackageManager().getPackageInfo(test.getTargetContext().getPackageName(),0);
            require(installed.getLongVersionCode()==(candidate?62:61)&&(candidate?"0.62.0-road-candidate":"0.61.0").equals(installed.versionName),"actual installed APK identity for phase="+phase());
            require(TerrainArt.ground(World.Terrain.ROAD)==World.Terrain.PLAIN,"ROAD shares PLAIN ground mapping");
            require(TerrainConnections.road(World.Terrain.ROAD),"ROAD remains a neighbor connection identity");
            require(TerrainArt.connection(World.Terrain.ROAD)==(candidate?TerrainArt.Connection.NONE:TerrainArt.Connection.ROAD),"installed visual connection matches expected phase");
            require(TerrainArt.connection(World.Terrain.MOUNTAIN_PATH)==TerrainArt.Connection.MOUNTAIN_PATH,"mountain-path artwork retained");
            require(TerrainArt.connection(World.Terrain.PLANK_ROAD)==TerrainArt.Connection.PLANK,"plank artwork retained");
            for(String id:new String[]{"coalition-190","heroes-250","central-mobile-sandbox","jingxiang-mobile-sandbox"}) {
                byte[] installedOld=null;
                if(candidate){
                    installedOld=readInternal("road62-real-v061-"+id+".sg11");
                    require(installedOld!=null,"real old APK save survived installation: "+id);
                    World decoded=SaveCodec.decode(installedOld);
                    require(id.equals(decoded.scenarioId)&&decoded.mapRevision==61,"actual v061 save decodes with original scenario and geography");
                    require(Arrays.equals(installedOld,SaveCodec.encode(decoded)),"actual v061 save has exact binary round trip");
                    launch(decoded);
                }else launch(ScenarioCatalog.load(id,id.endsWith("sandbox")?0:5,620L));
                World w=world();byte[] original=SaveCodec.encode(w);
                if(candidate)require(Arrays.equals(installedOld,original),"loading actual old save does not move entities or replace geography");
                require(w.mapRevision==61,"road-only candidate truthfully retains map revision61");
                require(CityArtCatalog.ASSET_REVISION==56,"unapproved generated art is NOT presented as installed art62");
                List<Hex> roads=new ArrayList<>();
                for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
                    Hex h=new Hex(q,r);
                    if(w.sourceInside(h)&&w.terrain[q][r]==World.Terrain.ROAD&&w.war.at(h)==null&&!occupied(w,h))roads.add(h);
                }
                require(!roads.isEmpty(),"actual ROAD exists in "+id);
                Set<Integer> variants=new HashSet<>();
                for(Hex h:roads){int v=Math.floorMod(h.q*31+h.r*17,3);if(!variants.add(v))continue;
                    tileComparison(w,h,v);
                    if(variants.size()==3)break;
                }
                require(variants.size()==3,"all three stable texture variants actually compared in "+id);
                Hex h=roads.get(roads.size()/2);
                SourceGridCoord source=MapCoordinates.nationalSource(w,h);
                overviewComparison(w,h);
                for(float scale:new float[]{.30f,1.2f,3.4f}){
                    focus(h,scale);navigator(false);record(h,screen(h));
                    shot("road62-"+phase()+"-"+id+"-source-"+source.x+"-"+source.y+"-lod-"+scale);
                }
                focus(h,3.4f);tap(h);showPanel();
                require(panelText().contains("道路"),"real pointer opens true ROAD details, not PLAIN");
                shot("road62-"+phase()+"-"+id+"-road-details");
                ui(this::page);settle();
                navigator(true);ui(()->{camera().fit();map().invalidate();});settle();
                shot("road62-"+phase()+"-"+id+"-full-minimap");
                for(int mode=1;mode<=2;mode++){
                    final int m=mode;ui(()->map().setTerritoryMode(m));settle();
                    shot("road62-"+phase()+"-"+id+"-territory-"+mode);
                }
                ui(()->map().setTerritoryMode(0));settle();
                drawTiming(id);
                require(Arrays.equals(original,SaveCodec.encode(w)),"all visual probes preserve world/save bytes "+id);
                require(w.terrain[h.q][h.r]==World.Terrain.ROAD,"source ROAD identity restored after controlled comparison");
                if(!candidate)writeInternal("road62-real-v061-"+id+".sg11",original);
                try(OutputStream out=new FileOutputStream(new File(dir(),"road62-"+phase()+"-"+id+".sg11"))){out.write(original);}
            }
        } finally { flush("road62-"+phase()+"-checks.txt"); }
    }
    private static boolean occupied(World w,Hex h){
        for(World.City city:w.cities)for(Hex cell:SiteFootprint.cells(city))if(cell.equals(h))return true;
        for(Army.Unit unit:w.army.units)if(unit.hex.equals(h))return true;
        return false;
    }
    private void tileComparison(World w,Hex h,int variant)throws Exception{
        Bitmap[] pair=new Bitmap[2];
        ui(()->{
            World.Terrain old=w.terrain[h.q][h.r];
            try {
                TerrainTiles tiles=new TerrainTiles();
                for(int i=0;i<2;i++){
                    w.terrain[h.q][h.r]=i==0?World.Terrain.ROAD:World.Terrain.PLAIN;
                    pair[i]=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);
                    Canvas canvas=new Canvas(pair[i]);canvas.translate(96,96);canvas.scale(3,3);
                    tiles.draw(canvas,w,h.q,h.r,0,0);
                }
            }finally{w.terrain[h.q][h.r]=old;}
        });
        try {
            require(pair[0].sameAs(pair[1])==candidate,"actual TerrainTiles ROAD/PLAIN pixel comparison, variant="+variant+" expectedEqual="+candidate);
            for(int i=0;i<2;i++)try(OutputStream out=new FileOutputStream(new File(dir(),"road62-"+phase()+"-"+w.scenarioId+"-variant-"+variant+"-"+(i==0?"ROAD":"PLAIN")+".png"))){pair[i].compress(Bitmap.CompressFormat.PNG,100,out);}
        }finally{for(Bitmap bitmap:pair)if(bitmap!=null)bitmap.recycle();}
    }
    private void overviewComparison(World w,Hex h)throws Exception{
        // The Territory snapshot is fixed; substituting terrain solely for this comparison
        // must not recompute gameplay catchments or change owner colors.
        Territory territory=new Territory(w);
        MapOverview[] pair=new MapOverview[2];
        ui(()->{
            World.Terrain old=w.terrain[h.q][h.r];
            try {
                for(int i=0;i<2;i++){
                    w.terrain[h.q][h.r]=i==0?World.Terrain.ROAD:World.Terrain.PLAIN;
                    pair[i]=new MapOverview(w,territory,40f*(w.height+1),40f*(w.width+w.height+1),0);
                }
            }finally{w.terrain[h.q][h.r]=old;}
        });
        try {
            byte[] roadBits=(byte[])field(pair[0],"roads");
            int index=h.r*w.width+h.q;
            require((roadBits[index]==0)==candidate,"overview has no ordinary-road stroke in candidate");
            require(Arrays.equals((int[])field(pair[0],"terrain"),(int[])field(pair[1],"terrain")),"overview ROAD/PLAIN base colors identical");
            require(Arrays.equals((int[])field(pair[0],"styles"),(int[])field(pair[1],"styles")),"overview ROAD/PLAIN texture indices identical");
            if(candidate){
                // Preserve mountain/plank connection semantics. Complete images must match
                // whenever this controlled substitution leaves those neighbor masks unchanged.
                boolean sameConnections=Arrays.equals((byte[])field(pair[0],"roads"),(byte[])field(pair[1],"roads"));
                if(sameConnections){
                    Bitmap[] a=(Bitmap[])field(pair[0],"preview"),b=(Bitmap[])field(pair[1],"preview");
                    for(int mode=0;mode<3;mode++)require(a[mode].sameAs(b[mode]),"actual overview preview pixel equivalence mode="+mode);
                }else report.append("INFO neighboring mountain/plank connection semantics differ when gameplay identity is intentionally substituted; no such substitution is shipped\n");
            }
        }finally{for(MapOverview overview:pair)if(overview!=null){overview.cancel();for(Bitmap bitmap:(Bitmap[])field(overview,"preview"))bitmap.recycle();}}
    }
    private void drawTiming(String id)throws Exception{
        long[] times=new long[20];
        ui(()->{
            Bitmap bitmap=Bitmap.createBitmap(map().getWidth(),map().getHeight(),Bitmap.Config.ARGB_8888);
            try {
                Canvas canvas=new Canvas(bitmap);
                for(int i=0;i<5;i++)map().draw(canvas);
                for(int i=0;i<times.length;i++){long start=System.nanoTime();map().draw(canvas);times[i]=System.nanoTime()-start;}
            }finally{bitmap.recycle();}
        });
        Arrays.sort(times);
        report.append("MEASUREMENT phase=").append(phase()).append(" scenario=").append(id)
            .append(" softwareCanvasDrawMedianMs=").append(times[10]/1000000.0)
            .append(" softwareCanvasDrawP95Ms=").append(times[18]/1000000.0)
            .append(" processPssKiB=").append(Debug.getPss())
            .append(" notHardwareFps=true physicalARM=false\n");
    }
}
