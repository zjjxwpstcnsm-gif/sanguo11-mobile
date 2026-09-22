package game.sanguo.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import com.google.android.filament.*;
import game.sanguo.core.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** All Filament calls belong to the main Looper. Only CPU mesh building uses the worker.
 * Surface changes replace the swap chain, never the world, commands or replay cursor. */
final class FilamentMapView extends FrameLayout implements SurfaceHolder.Callback,Choreographer.FrameCallback {
    final SceneCamera camera=new SceneCamera();
    private final SurfaceView surface;
    private final Overlay overlay;
    private final Consumer<Throwable> failure;
    private final MapView.TileListener listener;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Future<?> meshTask;
    private int generation;
    private Engine engine; private Renderer renderer; private Scene scene;
    private com.google.android.filament.android.DisplayHelper displayHelper;
    private Skybox skybox;
    private com.google.android.filament.View view; private Camera lens; private Material material;
    private Material siteMaterial; private Texture siteAtlas,fieldAtlas;private FieldAssets fieldAssets;private MaterialInstance vegetationMaterial;
    private int siteLod=1; private final Set<String> missingAssets=new HashSet<>();
    private SwapChain swap; private int cameraEntity,light;
    private boolean released,resumed=true,queued,diagnostics;
    private final long[] cpuSamples=new long[240];private int cpuCount,cpuCursor;
    private long lastFrame; private long renderedFrames; private double callbackMillis;
    private MapSceneSnapshot snapshot;
    private boolean distantTerrain;
    private List<SceneMesh> chunks=Collections.emptyList(),woods=Collections.emptyList();
    private Set<Hex> woodExcluded=Collections.emptySet();
    private final Map<SceneMesh,GpuMesh> vegetation=new HashMap<>();
    private int visibleWood,unitLod=1;private long animationTick;
    private final Map<SceneMesh,GpuMesh> terrain=new HashMap<>();
    private final Map<String,Proxy> objects=new HashMap<>();
    private final Map<String,GpuMesh> shapes=new LinkedHashMap<>(128,.75f,true);
    private int pending,visibleChunks,visibleObjects;
    private Set<Hex> targets=Collections.emptySet();
    private MarchOrders.Plan route;
    private final GestureDetector gestures; private final ScaleGestureDetector scaler;
    private boolean multi;
    private TurnJournal.Event replay;private float replayFraction;private Proxy animatedUnit;
    FilamentMapView(Context context,MapView.TileListener listener,Consumer<Throwable> failure) throws Exception {
        super(context);this.listener=listener;this.failure=failure;
        surface=new SurfaceView(context);addView(surface,new LayoutParams(-1,-1));overlay=new Overlay(context);addView(overlay,new LayoutParams(-1,-1));
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!scaler.isInProgress()){camera.pan(-dx,-dy);clampCamera();}return true;}
            @Override public boolean onSingleTapUp(MotionEvent e){
                if(!multi&&snapshot!=null){Hex h=pickUnit(e.getX(),e.getY());if(h==null)h=pickFacility(e.getX(),e.getY());if(h==null)h=snapshot.ground.surface.pick(camera,e.getX(),e.getY());if(snapshot.ground.valid(h)){performClick();listener.tap(h);}}return true;
            }
            @Override public void onLongPress(MotionEvent e){camera.facing=-camera.facing;overlay.invalidate();}
            @Override public boolean onDoubleTap(MotionEvent e){camera.zoom(1.7f,e.getX(),e.getY());return true;}
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){camera.zoom(d.getScaleFactor(),d.getFocusX(),d.getFocusY());clampCamera();return true;}});
        try{
            Filament.init();engine=Engine.create(Engine.Backend.OPENGL);
            renderer=engine.createRenderer();scene=engine.createScene();view=engine.createView();
            displayHelper=new com.google.android.filament.android.DisplayHelper(context);
            skybox=new Skybox.Builder().color(.075f,.10f,.11f,1).build(engine);scene.setSkybox(skybox);
            cameraEntity=EntityManager.get().create();lens=engine.createCamera(cameraEntity);view.setScene(scene);view.setCamera(lens);view.setPostProcessingEnabled(false);
            Renderer.ClearOptions clear=new Renderer.ClearOptions();clear.clear=true;clear.clearColor=new float[]{.075f,.10f,.11f,1};renderer.setClearOptions(clear);
            byte[] bytes;try(java.io.InputStream in=context.getAssets().open("3d/terrain.filamat")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int count;while((count=in.read(block))!=-1)out.write(block,0,count);bytes=out.toByteArray();}
            ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();material=new Material.Builder().payload(payload,bytes.length).build(engine);
            byte[] siteBytes;try(java.io.InputStream in=context.getAssets().open("3d/sites/site.filamat")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);siteBytes=out.toByteArray();}
            ByteBuffer sb=ByteBuffer.allocateDirect(siteBytes.length).order(ByteOrder.nativeOrder());sb.put(siteBytes).flip();siteMaterial=new Material.Builder().payload(sb,siteBytes.length).build(engine);
            android.graphics.Bitmap atlas;try(java.io.InputStream in=context.getAssets().open("3d/sites/atlas.png")){atlas=android.graphics.BitmapFactory.decodeStream(in);}
            if(atlas==null)throw new java.io.IOException("site atlas missing");
            siteAtlas=new Texture.Builder().width(atlas.getWidth()).height(atlas.getHeight()).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.SRGB8_A8).build(engine);
            com.google.android.filament.android.TextureHelper.setBitmap(engine,siteAtlas,0,atlas);
            fieldAssets=new FieldAssets(name->context.getAssets().open("3d/field/"+name));
            android.graphics.Bitmap fieldBitmap;try(java.io.InputStream in=context.getAssets().open("3d/field/atlas.png")){fieldBitmap=android.graphics.BitmapFactory.decodeStream(in);}
            if(fieldBitmap==null)throw new java.io.IOException("field atlas missing");
            fieldAtlas=new Texture.Builder().width(fieldBitmap.getWidth()).height(fieldBitmap.getHeight()).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.SRGB8_A8).build(engine);
            com.google.android.filament.android.TextureHelper.setBitmap(engine,fieldAtlas,0,fieldBitmap);
            vegetationMaterial=siteMaterial.createInstance();vegetationMaterial.setParameter("atlas",fieldAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));vegetationMaterial.setParameter("damage",0f);
            light=EntityManager.get().create();new LightManager.Builder(LightManager.Type.DIRECTIONAL).direction(-1,-2,-1).color(1,.95f,.85f).intensity(50000).castShadows(false).build(engine,light);scene.addEntity(light);
            surface.getHolder().addCallback(this);
        }catch(Exception|LinkageError e){release();throw e;}
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(!isEnabled())return true;if(e.getActionMasked()==MotionEvent.ACTION_DOWN)multi=false;if(e.getPointerCount()>1)multi=true;scaler.onTouchEvent(e);gestures.onTouchEvent(e);return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    void snapshot(MapSceneSnapshot next){
        boolean groundChanged=snapshot==null||snapshot.ground!=next.ground;snapshot=next;
        Set<Hex> excluded=Vegetation.exclusions(next);boolean woodsChanged=groundChanged||!excluded.equals(woodExcluded);
        if(groundChanged||woodsChanged){
            woodExcluded=excluded;int token=++generation;if(meshTask!=null)meshTask.cancel(true);
            List<SceneMesh> previous=chunks,oldWoods=woods;pending=1;
            final SceneMesh tree,farTree;
            try{tree=fieldAssets.mesh("tree-lod0");farTree=fieldAssets.mesh("tree-lod1");}catch(Exception e){failure.accept(e);return;}
            meshTask=worker.submit(()->{try{
                long started=android.os.SystemClock.elapsedRealtime();
                List<SceneMesh> built=SceneMesh.ground(next.ground,previous);
                android.util.Log.i("Sanguo3D","Ground CPU ready chunks="+built.size()+" ms="+(android.os.SystemClock.elapsedRealtime()-started));
                List<SceneMesh> trees=Vegetation.build(next.ground,excluded,oldWoods,tree,farTree);
                android.util.Log.i("Sanguo3D","Field CPU ready forestChunks="+trees.size()+" totalMs="+(android.os.SystemClock.elapsedRealtime()-started));
                post(()->{if(released||token!=generation)return;
                    for(SceneMesh old:new ArrayList<>(terrain.keySet()))if(!built.contains(old)&&built.stream().noneMatch(m->m.distant==old)){terrain.remove(old).destroy();}
                    for(SceneMesh old:new ArrayList<>(vegetation.keySet()))if(!trees.contains(old)&&trees.stream().noneMatch(m->m.distant==old)){vegetation.remove(old).destroy();}
                    chunks=built;woods=trees;pending=0;schedule();});
                }catch(RuntimeException|OutOfMemoryError e){post(()->{if(!released&&token==generation)failure.accept(e);});}
            });
        }
        syncObjects();overlay.invalidate();schedule();
    }
    void setTargets(Set<Hex> value){targets=value==null?Collections.emptySet():new HashSet<>(value);overlay.invalidate();}
    void setRoute(MarchOrders.Plan value){route=value;overlay.invalidate();}
    void replay(TurnJournal.Event e,float fraction){
        replay=e;replayFraction=fraction;animateReplay();overlay.invalidate();schedule();
    }
    boolean visible(TurnJournal.Event e){if(visible(e.start)||visible(e.target))return true;for(Hex h:e.path)if(visible(h))return true;return false;}
    private boolean visible(Hex h){if(h==null||snapshot==null)return false;GridWorldTransform g=snapshot.ground.grid;return Math.abs(camera.screenX(g.x(h))-camera.width/2f)<camera.width*.6&&Math.abs(camera.screenY(g.z(h),snapshot.ground.surface.at(h))-camera.height/2f)<camera.height*.6;}
    void diagnostics(boolean value){diagnostics=value;overlay.invalidate();}
    String report(){return "Filament 1.56.0 / OpenGL ES\n"+camera.width+" × "+camera.height+" · chunks "+visibleChunks+" / GPU "+terrain.size()+" · objects "+visibleObjects+"\n帧回调间隔 "+String.format(java.util.Locale.ROOT,"%.1f",callbackMillis)+" ms（非 GPU/FPS 实测）\n待装载 "+pending+" · S04 设施植被 / S05 部队 · 林块 "+visibleWood+" · LOD "+siteLod+" · 资产回退 "+missingAssets.size()+"\n"+resourceReport();}
    void resetMetrics(){cpuCount=cpuCursor=0;}
    private String resourceReport(){
        int primitives=0,triangles=0;long bufferBytes=0;
        Set<GpuMesh> resident=new HashSet<>(shapes.values());resident.addAll(terrain.values());resident.addAll(vegetation.values());
        for(GpuMesh m:resident){bufferBytes+=(long)m.source.vertices.length*4+(long)m.source.indices.length*4+(m.source.uv==null?0:(long)m.source.uv.length*4);if(m.shown){primitives++;triangles+=m.source.indices.length/3;}}
        for(Proxy p:objects.values())if(p.shown){primitives++;triangles+=p.shape.source.indices.length/3;for(GpuMesh m:new GpuMesh[]{p.flagShape,p.baseShape,p.stateShape})if(m!=null){primitives++;triangles+=m.source.indices.length/3;}}
        long[] times=Arrays.copyOf(cpuSamples,cpuCount);Arrays.sort(times);
        return "场景 primitives="+primitives+" triangles="+triangles+" buffers_bytes="+bufferBytes+" pose_cache="+shapes.size()+
            " CPU提交ms P50/P95/P99="+percentile(times,.50)+"/"+percentile(times,.95)+"/"+percentile(times,.99)+" samples="+cpuCount+"（非驱动DrawCall/GPU帧时）";
    }
    private static String percentile(long[] times,double p){return times.length==0?"N/A":String.format(java.util.Locale.ROOT,"%.2f",times[Math.min(times.length-1,(int)Math.ceil(times.length*p)-1)]/1e6);}
    void resume(boolean value){resumed=value;if(value)schedule();else cancelFrame();}
    private void cancelFrame(){Choreographer.getInstance().removeFrameCallback(this);queued=false;lastFrame=0;}
    private void schedule(){if(!released&&resumed&&swap!=null&&!queued){queued=true;Choreographer.getInstance().postFrameCallback(this);}}
    @Override public void surfaceCreated(SurfaceHolder holder){if(released)return;try{swap=engine.createSwapChain(holder.getSurface());displayHelper.attach(renderer,surface.getDisplay());schedule();}catch(RuntimeException|LinkageError e){failure.accept(e);}}
    @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int height){if(released)return;camera.width=Math.max(1,w);camera.height=Math.max(1,height);view.setViewport(new Viewport(0,0,w,height));com.google.android.filament.android.FilamentHelper.synchronizePendingFrames(engine);schedule();}
    @Override public void surfaceDestroyed(SurfaceHolder holder){cancelFrame();if(displayHelper!=null)displayHelper.detach();if(engine!=null&&swap!=null){engine.destroySwapChain(swap);swap=null;engine.flushAndWait();}}
    @Override public void doFrame(long time){
        queued=false;if(released||!resumed||swap==null)return;
        long cpuStart=System.nanoTime();
        try{
            if(lastFrame!=0)callbackMillis=(time-lastFrame)/1e6;lastFrame=time;
            double aspect=camera.width/(double)camera.height;
            lens.setProjection(Camera.Projection.ORTHO,-camera.span*aspect,camera.span*aspect,-camera.span,camera.span,.1,1000);
            lens.lookAt(camera.x,300*camera.sin(),camera.z+camera.facing*300*camera.cos(),camera.x,0,camera.z,0,1,0);
            animationTick=time/1_000_000;animateReplay();loadVisible();animateUnits();
            if(renderer.beginFrame(swap,time)){renderer.render(view);renderer.endFrame();renderedFrames++;}
            overlay.invalidate();schedule();
            cpuSamples[cpuCursor++%cpuSamples.length]=System.nanoTime()-cpuStart;cpuCount=Math.min(cpuSamples.length,cpuCount+1);
        }catch(RuntimeException|LinkageError e){cancelFrame();failure.accept(e);}
    }
    private boolean inView(float x,float z,float radius){return Math.abs(x-camera.x)<camera.span*camera.width/camera.height+radius+2&&Math.abs(z-camera.z)<camera.span/camera.sin()+radius+2;}
    private void loadVisible(){
        if(snapshot==null)return;int nextLod=SiteVisual.lod(camera.span,siteLod);if(nextLod!=siteLod){siteLod=nextLod;syncObjects();}int budget=2;visibleChunks=0;pending=meshTask!=null&&!meshTask.isDone()?1:0;
        if(camera.span>48)distantTerrain=true;else if(camera.span<40)distantTerrain=false;
        Set<SceneMesh> active=new HashSet<>();for(SceneMesh m:chunks)active.add(distantTerrain&&m.distant!=null?m.distant:m);
        for(SceneMesh m:new ArrayList<>(terrain.keySet()))if(!active.contains(m)){terrain.remove(m).destroy();}
        for(SceneMesh source:chunks){
            SceneMesh chunk=distantTerrain&&source.distant!=null?source.distant:source;
            boolean shown=inView(chunk.x,chunk.z,chunk.radius);GpuMesh gpu=terrain.get(chunk);
            if(shown){visibleChunks++;if(gpu==null){if(budget-->0){gpu=new GpuMesh(chunk);terrain.put(chunk,gpu);}else pending++;}}
            if(gpu!=null){gpu.show(shown);if(!shown&&!inView(chunk.x,chunk.z,chunk.radius+20)){gpu.destroy();terrain.remove(chunk);}}
        }
        int nextUnitLod=camera.span<8?0:camera.span<22?1:2;
        unitLod=nextUnitLod;visibleWood=0;
        Set<SceneMesh> wantedWood=new HashSet<>();
        if(camera.span<38)for(SceneMesh source:woods){
            SceneMesh chunk=camera.span<14?source:source.distant;if(chunk.indices.length==0)continue;
            if(inView(chunk.x,chunk.z,chunk.radius)){wantedWood.add(chunk);visibleWood++;GpuMesh gpu=vegetation.get(chunk);
                if(gpu==null){if(budget-->0){gpu=new GpuMesh(chunk);gpu.build(gpu.entity,vegetationMaterial);vegetation.put(chunk,gpu);}else pending++;}
                if(gpu!=null)gpu.show(true);
            }
        }
        for(SceneMesh old:new ArrayList<>(vegetation.keySet()))if(!wantedWood.contains(old)){vegetation.remove(old).destroy();}
        visibleObjects=0;
        for(Proxy p:objects.values()){boolean shown=inView(p.motion.x,p.motion.z,2);if(shown)visibleObjects++;if(shown!=p.shown){p.show(shown);}}
    }
    private GpuMesh shape(MapSceneSnapshot.Item item){
        String field=item.facility!=null?FieldAssets.facility(item.facility,siteLod):item.unit!=null?FieldAssets.unit(item.unit,item.unit.naval,unitLod):null;
        String key=field!=null?field+(item.unit==null?"":":idle:0:"+FieldAssets.count(item.unit,item.unit.naval,unitLod)):item.site==null?item.kind+":"+item.color:item.site.model+":"+siteLod;
        GpuMesh mesh=shapes.get(key);if(mesh!=null)return mesh;
        SceneMesh source;
        if(field!=null){
            try{source=item.unit==null?fieldAssets.mesh(field):fieldAssets.pose(field,"idle",0,FieldAssets.count(item.unit,item.unit.naval,unitLod));}
            catch(Exception e){throw new IllegalStateException("Missing field asset "+field,e);}
        }else if(item.site!=null){
            try(java.io.InputStream in=getContext().getAssets().open("3d/sites/"+item.site.model+"-lod"+siteLod+".glb")){source=SiteGlb.read(in);}
            catch(Exception e){missingAssets.add(key);source=SiteVisual.fallback(item.kind);android.util.Log.w("Sanguo3D","Site fallback "+key,e);}
        }else source=SceneMesh.proxy(item.kind,item.color);
        mesh=new GpuMesh(source);shapes.put(key,mesh);return mesh;
    }
    private void syncObjects(){
        if(engine==null||snapshot==null)return;Set<String> alive=new HashSet<>();
        for(MapSceneSnapshot.Item item:snapshot.items){
            alive.add(item.key);Proxy p=objects.get(item.key);GpuMesh geometry=shape(item);
            if(p!=null&&((item.unit==null&&p.shape!=geometry)||p.item.color!=item.color||!p.stateKey().equals(item.facility==null?"":item.facility.burning?"fire":!item.facility.complete?"scaffold":""))){p.destroy();objects.remove(item.key);p=null;}
            if(p==null){p=new Proxy(item,geometry);objects.put(item.key,p);}
            p.item=item;p.motion.settle(item.hex,snapshot.ground.grid);p.position(p.motion.x,p.motion.z);p.updateDamage();
        }
        Iterator<Map.Entry<String,Proxy>> it=objects.entrySet().iterator();while(it.hasNext()){Map.Entry<String,Proxy> e=it.next();if(!alive.contains(e.getKey())){e.getValue().destroy();it.remove();}}
        trimShapes();
        animateReplay();
    }
    private void trimShapes(){
        Set<GpuMesh> used=new HashSet<>();for(Proxy p:objects.values()){used.add(p.shape);if(p.flagShape!=null)used.add(p.flagShape);if(p.baseShape!=null)used.add(p.baseShape);if(p.stateShape!=null)used.add(p.stateShape);}
        Iterator<Map.Entry<String,GpuMesh>> it=shapes.entrySet().iterator();
        while(it.hasNext()&&shapes.size()>128){GpuMesh m=it.next().getValue();if(!used.contains(m)){m.destroy();it.remove();}}
    }
    private void animateUnits(){
        if(snapshot==null)return;
        for(Proxy p:objects.values())if(p.item.unit!=null&&p.shown){
            p.animation.sample(p.item,snapshot.ground,replay,replayFraction,animationTick,unitLod);
            String model=FieldAssets.unit(p.item.unit,p.animation.naval,unitLod);
            int count=FieldAssets.count(p.item.unit,p.animation.naval,unitLod);
            String key=model+":"+p.animation.clip+":"+p.animation.frame+":"+count;
            if(!key.equals(p.poseKey)){
                GpuMesh mesh=shapes.get(key);
                if(mesh==null){try{mesh=new GpuMesh(fieldAssets.pose(model,p.animation.clip,p.animation.frame,count));}catch(Exception e){throw new IllegalStateException("unit pose "+key,e);}shapes.put(key,mesh);}
                p.replace(mesh);p.poseKey=key;
            }
            p.position(p.motion.x,p.motion.z);
        }
        trimShapes();
    }
    private void animateReplay(){
        if(snapshot==null)return;
        Proxy current=replay==null?null:objects.get("unit:"+replay.actorId);
        if(animatedUnit!=null&&animatedUnit!=current&&objects.get(animatedUnit.item.key)==animatedUnit){
            animatedUnit.motion.sample(null,0,snapshot.ground.grid);
            animatedUnit.position(animatedUnit.motion.x,animatedUnit.motion.z);
        }
        animatedUnit=current;
        if(current!=null){
            current.motion.sample(replay,replayFraction,snapshot.ground.grid);
            // Keep the cheap CPU pose for culling/hit tests, but avoid off-screen GPU updates.
            if(inView(current.motion.x,current.motion.z,2))current.position(current.motion.x,current.motion.z);
        }
    }

    /** Resolve a visible moving model to its stable snapshot unit, never the terrain below it. */
    private Hex pickUnit(float sx,float sy){
        if(snapshot==null)return null;
        float radius=Math.max(14*getResources().getDisplayMetrics().density,
            Math.min(36*getResources().getDisplayMetrics().density,camera.height/(2*camera.span)*.45f));
        Proxy nearest=null;float distance=radius*radius;
        for(Proxy p:objects.values())if(p.item.unit!=null&&p.shown){
            float dx=sx-camera.screenX(p.motion.x),dy=sy-camera.screenY(p.motion.z,p.y+.25f),d=dx*dx+dy*dy;
            if(d<distance||(d==distance&&nearest!=null&&p.item.unit.id<nearest.item.unit.id)){nearest=p;distance=d;}
        }
        return nearest==null?null:nearest.item.hex;
    }
    /** Roof/tower taps select the real facility tile; vegetation never steals input. */
    private Hex pickFacility(float sx,float sy){
        Proxy chosen=null;float nearest=Float.MAX_VALUE;
        for(Proxy p:objects.values())if(p.shown&&p.item.facility!=null){
            float angle=p.item.facility.direction*(float)Math.PI/3,c=(float)Math.cos(angle),s=(float)Math.sin(angle);
            float minX=Float.MAX_VALUE,minY=minX,maxX=-minX,maxY=-minX;
            float[] v=p.shape.source.vertices;
            for(int i=0;i<v.length;i+=7){float x=p.motion.x+c*v[i]+s*v[i+2],z=p.motion.z-s*v[i]+c*v[i+2];
                float px=camera.screenX(x),py=camera.screenY(z,p.y+v[i+1]);minX=Math.min(minX,px);maxX=Math.max(maxX,px);minY=Math.min(minY,py);maxY=Math.max(maxY,py);}
            if(sx>=minX&&sx<=maxX&&sy>=minY&&sy<=maxY){float dx=sx-(minX+maxX)/2,dy=sy-(minY+maxY)/2,d=dx*dx+dy*dy;if(d<nearest){nearest=d;chosen=p;}}
        }
        return chosen==null?null:chosen.item.hex;
    }
    void focus(Hex h){if(snapshot==null||h==null)return;camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);camera.span=10;}
    void center(Hex h){if(snapshot!=null&&h!=null){camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);}}
    void fit(){if(snapshot==null)return;MapSceneSnapshot.Ground g=snapshot.ground;float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){Hex h=new Hex(q,r);if(g.valid(h)){float x=g.grid.x(h),z=g.grid.z(h);minX=Math.min(minX,x);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxZ=Math.max(maxZ,z);}}if(minX==Float.MAX_VALUE)return;camera.x=(minX+maxX)/2;camera.z=(minZ+maxZ)/2;camera.span=(float)Math.max((maxX-minX+3)*camera.height/camera.width,(maxZ-minZ+3)*camera.sin())*.52f;}
    private void clampCamera(){
        if(snapshot==null)return;float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
        for(SceneMesh m:chunks){minX=Math.min(minX,m.x-m.radius);maxX=Math.max(maxX,m.x+m.radius);minZ=Math.min(minZ,m.z-m.radius);maxZ=Math.max(maxZ,m.z+m.radius);}
        if(!chunks.isEmpty()){camera.x=Math.max(minX,Math.min(maxX,camera.x));camera.z=Math.max(minZ,Math.min(maxZ,camera.z));}
    }
    void saveCamera(Bundle b){b.putFloat("cameraX",camera.x*TileGeometry.DX);b.putFloat("cameraY",camera.z*TileGeometry.DY);b.putFloat("sceneSpan",camera.span);b.putFloat("sceneTilt",camera.tilt);b.putInt("sceneFacing",camera.facing);}
    void restoreCamera(Bundle b){camera.x=b.getFloat("cameraX")/TileGeometry.DX;camera.z=b.getFloat("cameraY")/TileGeometry.DY;camera.span=b.getFloat("sceneSpan",15);camera.tilt=b.getFloat("sceneTilt",55);camera.facing=b.getInt("sceneFacing",1)<0?-1:1;}
    void release(){
        if(released)return;released=true;replay=null;animatedUnit=null;generation++;cancelFrame();if(meshTask!=null)meshTask.cancel(true);worker.shutdownNow();surface.getHolder().removeCallback(this);
        if(engine==null)return;
        if(displayHelper!=null)displayHelper.detach();
        if(swap!=null){engine.destroySwapChain(swap);swap=null;}
        for(Proxy p:objects.values())p.destroy();objects.clear();for(GpuMesh m:terrain.values())m.destroy();terrain.clear();for(GpuMesh m:vegetation.values())m.destroy();vegetation.clear();for(GpuMesh m:shapes.values())m.destroy();shapes.clear();
        if(light!=0){scene.removeEntity(light);engine.destroyEntity(light);EntityManager.get().destroy(light);}
        if(vegetationMaterial!=null)engine.destroyMaterialInstance(vegetationMaterial);if(fieldAtlas!=null)engine.destroyTexture(fieldAtlas);if(siteMaterial!=null)engine.destroyMaterial(siteMaterial);if(siteAtlas!=null)engine.destroyTexture(siteAtlas);
        if(material!=null)engine.destroyMaterial(material);
        if(skybox!=null){scene.setSkybox(null);engine.destroySkybox(skybox);}
        if(view!=null)engine.destroyView(view);if(scene!=null)engine.destroyScene(scene);if(renderer!=null)engine.destroyRenderer(renderer);
        if(cameraEntity!=0){engine.destroyCameraComponent(cameraEntity);EntityManager.get().destroy(cameraEntity);}engine.flushAndWait();engine.destroy();engine=null;
    }
    private final class GpuMesh {
        final VertexBuffer vb;final IndexBuffer ib;final int entity;final SceneMesh source;boolean shown;
        GpuMesh(SceneMesh m){source=m;
            VertexBuffer.Builder builder=new VertexBuffer.Builder().vertexCount(m.vertices.length/7).bufferCount(m.uv==null?1:2).attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,28).attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,12,28);
            if(m.uv!=null)builder.attribute(VertexBuffer.VertexAttribute.UV0,1,VertexBuffer.AttributeType.FLOAT2,0,8);vb=builder.build(engine);
            if(m.uv!=null){FloatBuffer uv=ByteBuffer.allocateDirect(m.uv.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();uv.put(m.uv).flip();vb.setBufferAt(engine,1,uv);}
            FloatBuffer v=ByteBuffer.allocateDirect(m.vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();v.put(m.vertices).flip();vb.setBufferAt(engine,0,v);
            ib=new IndexBuffer.Builder().indexCount(m.indices.length).bufferType(IndexBuffer.Builder.IndexType.UINT).build(engine);IntBuffer i=ByteBuffer.allocateDirect(m.indices.length*4).order(ByteOrder.nativeOrder()).asIntBuffer();i.put(m.indices).flip();ib.setBuffer(engine,i);
            entity=EntityManager.get().create();if(m.uv==null)build(entity);
        }
        void build(int target){build(target,material.getDefaultInstance());}
        void build(int target,MaterialInstance instance){new RenderableManager.Builder(1).boundingBox(new Box(source.x,1.3f,source.z,source.radius,2.7f,source.radius)).material(0,instance).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vb,ib).castShadows(false).receiveShadows(false).build(engine,target);}
        void show(boolean value){if(shown==value)return;shown=value;if(value)scene.addEntity(entity);else scene.removeEntity(entity);}
        void destroy(){scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);engine.destroyVertexBuffer(vb);engine.destroyIndexBuffer(ib);}
    }
    private final class Proxy {
        final int entity; GpuMesh shape; int flag,base,state; GpuMesh flagShape,baseShape,stateShape; MaterialInstance instance;
        MapSceneSnapshot.Item item;boolean shown;final UnitMotion motion=new UnitMotion();final UnitAnimation animation=new UnitAnimation();String poseKey;float y;
        Proxy(MapSceneSnapshot.Item item,GpuMesh shape){this.item=item;motion.settle(item.hex,snapshot.ground.grid);this.shape=shape;entity=EntityManager.get().create();
            if(shape.source.uv!=null){instance=siteMaterial.createInstance();instance.setParameter("atlas",item.site!=null?siteAtlas:fieldAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));instance.setParameter("damage",0f);shape.build(entity,instance);}else shape.build(entity);
            if(item.site!=null||item.unit!=null||item.facility!=null){String key="flag:"+item.color;flagShape=shapes.get(key);if(flagShape==null){flagShape=new GpuMesh(SceneMesh.proxy(3,item.color));shapes.put(key,flagShape);}flag=EntityManager.get().create();flagShape.build(flag);}
            if(item.kind==0&&item.site!=null){
                GridWorldTransform grid=snapshot.ground.grid;float x=grid.x(item.hex),z=grid.z(item.hex);StringBuilder key=new StringBuilder("base:");
                for(Hex h:item.site.cells)key.append(grid.x(h)-x).append(',').append(grid.z(h)-z).append(';');
                baseShape=shapes.get(key.toString());if(baseShape==null){SceneMesh.Builder b=new SceneMesh.Builder();
                    for(Hex h:item.site.cells){float cx=grid.x(h)-x,cz=grid.z(h)-z;for(int k=0;k<SceneMesh.EDGE.length;k++){float[] a=SceneMesh.EDGE[k],e=SceneMesh.EDGE[(k+1)%SceneMesh.EDGE.length];int n=b.v.size()/7;b.vertex(cx,.006f,cz,0xff9b9581);b.vertex(cx+a[0],.006f,cz+a[1],0xff9b9581);b.vertex(cx+e[0],.006f,cz+e[1],0xff9b9581);Collections.addAll(b.i,n,n+1,n+2);}}
                    baseShape=new GpuMesh(b.mesh(0,0,2));shapes.put(key.toString(),baseShape);
                }base=EntityManager.get().create();baseShape.build(base);
            }
            if(item.facility!=null&&(!item.facility.complete||item.facility.burning)){
                String stateKey=item.facility.burning?"fire":"scaffold";stateShape=shapes.get(stateKey);
                if(stateShape==null){try{stateShape=new GpuMesh(fieldAssets.mesh(stateKey));}catch(Exception e){throw new IllegalStateException(e);}shapes.put(stateKey,stateShape);}
                state=EntityManager.get().create();stateShape.build(state,vegetationMaterial);
            }
            position(snapshot.ground.grid.x(item.hex),snapshot.ground.grid.z(item.hex));updateDamage();
        }
        void replace(GpuMesh mesh){
            engine.getRenderableManager().destroy(entity);shape=mesh;shape.build(entity,instance);if(shown)scene.addEntity(entity);
        }
        void updateDamage(){if(instance!=null)instance.setParameter("damage",item.site!=null?item.site.damage*.5f:item.facility!=null?1-item.facility.hp/(float)Math.max(1,item.facility.maxHp):0);}
        String stateKey(){return item.facility==null?"":item.facility.burning?"fire":!item.facility.complete?"scaffold":"";}
        void position(float x,float z){
            y=item.unit!=null&&animation.naval?.02f:snapshot.ground.surface.meshHeight(x,z)+.02f;
            float angle=item.site==null?(item.facility!=null?item.facility.direction*(float)Math.PI/3:motion.yaw):item.site.yaw,scale=item.site==null?(item.unit==null?1:animation.scale):item.site.scale;
            float c=(float)Math.cos(angle)*scale,s=(float)Math.sin(angle)*scale;
            float[] matrix={c,0,-s,0,0,scale,0,0,s,0,c,0,x,y,z,1};TransformManager tm=engine.getTransformManager();tm.setTransform(tm.getInstance(entity),matrix);
            if(state!=0)tm.setTransform(tm.getInstance(state),matrix);
            if(base!=0){float[] baseMatrix={1,0,0,0,0,1,0,0,0,0,1,0,x,y,z,1};tm.setTransform(tm.getInstance(base),baseMatrix);}
            if(flag!=0){float[] f={.42f,0,0,0,0,.42f,0,0,0,0,.42f,0,x,y+(item.kind==0?.82f:item.unit!=null?.25f:.46f),z,1};tm.setTransform(tm.getInstance(flag),f);}
        }
        void show(boolean value){shown=value;if(value){scene.addEntity(entity);if(flag!=0)scene.addEntity(flag);if(base!=0)scene.addEntity(base);if(state!=0)scene.addEntity(state);}else{scene.removeEntity(entity);if(flag!=0)scene.removeEntity(flag);if(base!=0)scene.removeEntity(base);if(state!=0)scene.removeEntity(state);}}
        void destroy(){scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);if(flag!=0){scene.removeEntity(flag);engine.destroyEntity(flag);EntityManager.get().destroy(flag);}if(base!=0){scene.removeEntity(base);engine.destroyEntity(base);EntityManager.get().destroy(base);}if(state!=0){scene.removeEntity(state);engine.destroyEntity(state);EntityManager.get().destroy(state);}if(instance!=null)engine.destroyMaterialInstance(instance);}
    }
    private final class Overlay extends android.view.View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Overlay(Context c){super(c);setClickable(false);}
        void cell(Canvas c,Hex h,int color){
            if(h==null||snapshot==null)return;GridWorldTransform g=snapshot.ground.grid;float x=g.x(h),z=g.z(h);
            android.graphics.Path path=new android.graphics.Path();int i=0;
            for(float[] edge:SceneMesh.EDGE){float wx=x+edge[0],wz=z+edge[1],sx=camera.screenX(wx),sy=camera.screenY(wz,snapshot.ground.surface.sample(wx,wz)+.015f);if(i++==0)path.moveTo(sx,sy);else path.lineTo(sx,sy);}
            path.close();p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(path,p);
        }
        @Override protected void onDraw(Canvas c){
            if(snapshot==null)return;
            if(route!=null)for(Hex h:route.path)cell(c,h,0xffffd576);
            for(Hex h:snapshot.reachable)cell(c,h,0x884ed7c2);for(Hex h:snapshot.coverage)cell(c,h,0xffcfad6e);for(Hex h:snapshot.siege)cell(c,h,0x9975a8fa);for(Hex h:targets)cell(c,h,0xffdd7661);cell(c,snapshot.selected,0xffffd576);
            for(MapSceneSnapshot.Item item:snapshot.items)if(item.site!=null&&item.site.cells.contains(snapshot.selected))for(Hex h:item.site.cells)cell(c,h,0xffffd576);
            // Ground rings remain visible through architecture; transit units cannot disappear behind walls.
            for(Proxy object:objects.values())if(object.item.unit!=null&&object.shown){
                float x=camera.screenX(object.motion.x),y=camera.screenY(object.motion.z,object.y);
                float radius=Math.max(3,camera.height/(2*camera.span)*.38f);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
                p.setColor(object.item.hex.equals(snapshot.selected)?0xffffd576:object.item.color);
                c.drawOval(x-radius,y-radius*(float)camera.sin(),x+radius,y+radius*(float)camera.sin(),p);
            }
            p.setStyle(Paint.Style.FILL);p.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);p.setShadowLayer(2,0,1,0xff000000);
            for(Proxy object:objects.values()){
                MapSceneSnapshot.Item item=object.item;
                if((item.kind<3||camera.span<18||item.hex.equals(snapshot.selected))&&object.shown){
                    float x=camera.screenX(object.motion.x),y=camera.screenY(object.motion.z,object.y+1);
                    p.setColor(item.color);c.drawText(item.displayLabel(),x,y,p);
                }
            }
            p.setColor(0xfff0e5c8);c.drawText((pending>0)?"3D 地形装载中… · 可在视图切回 2D":"3D 试验 · 设施植被 / 部队编队 · 长按反向查看",12,24*getResources().getDisplayMetrics().density,p);
            if(diagnostics){float y=48*getResources().getDisplayMetrics().density;for(String line:report().split("\n")){c.drawText(line,12,y,p);y+=22*getResources().getDisplayMetrics().density;}}
            p.clearShadowLayer();
        }
    }
}
