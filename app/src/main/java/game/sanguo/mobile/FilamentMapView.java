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
    private com.google.android.filament.View view; private Camera lens; private Material material;
    private SwapChain swap; private int cameraEntity,light;
    private boolean released,resumed=true,queued,diagnostics;
    private long lastFrame; private double callbackMillis;
    private MapSceneSnapshot snapshot;
    private List<SceneMesh> chunks=Collections.emptyList();
    private final Map<SceneMesh,GpuMesh> terrain=new HashMap<>();
    private final Map<String,Proxy> objects=new HashMap<>();
    private final Map<String,GpuMesh> shapes=new HashMap<>();
    private int pending,visibleChunks,visibleObjects;
    private Set<Hex> targets=Collections.emptySet();
    private MarchOrders.Plan route;
    private final GestureDetector gestures; private final ScaleGestureDetector scaler;
    private boolean multi;
    private TurnJournal.Event replay;private float replayFraction;
    FilamentMapView(Context context,MapView.TileListener listener,Consumer<Throwable> failure) throws Exception {
        super(context);this.listener=listener;this.failure=failure;
        surface=new SurfaceView(context);addView(surface,new LayoutParams(-1,-1));overlay=new Overlay(context);addView(overlay,new LayoutParams(-1,-1));
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!scaler.isInProgress()){camera.pan(-dx,-dy);clampCamera();}return true;}
            @Override public boolean onSingleTapUp(MotionEvent e){
                if(!multi&&snapshot!=null){Hex h=snapshot.ground.grid.cell(camera.worldX(e.getX()),camera.worldZ(e.getY()));if(snapshot.ground.valid(h)){performClick();listener.tap(h);}}return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e){camera.zoom(1.7f,e.getX(),e.getY());return true;}
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){camera.zoom(d.getScaleFactor(),d.getFocusX(),d.getFocusY());clampCamera();return true;}});
        try{
            Filament.init();engine=Engine.create(Engine.Backend.OPENGL);
            renderer=engine.createRenderer();scene=engine.createScene();view=engine.createView();
            cameraEntity=EntityManager.get().create();lens=engine.createCamera(cameraEntity);view.setScene(scene);view.setCamera(lens);view.setPostProcessingEnabled(false);
            Renderer.ClearOptions clear=new Renderer.ClearOptions();clear.clear=true;clear.clearColor=new double[]{.075f,.10f,.11f,1};renderer.setClearOptions(clear);
            byte[] bytes;try(java.io.InputStream in=context.getAssets().open("3d/terrain.filamat")){bytes=in.readAllBytes();}
            ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();material=new Material.Builder().payload(payload,bytes.length).build(engine);
            light=EntityManager.get().create();new LightManager.Builder(LightManager.Type.DIRECTIONAL).direction(-1,-2,-1).color(1,.95f,.85f).intensity(50000).castShadows(false).build(engine,light);scene.addEntity(light);
            surface.getHolder().addCallback(this);
        }catch(Exception|LinkageError e){release();throw e;}
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(!isEnabled())return true;if(e.getActionMasked()==MotionEvent.ACTION_DOWN)multi=false;if(e.getPointerCount()>1)multi=true;scaler.onTouchEvent(e);gestures.onTouchEvent(e);return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    void snapshot(MapSceneSnapshot next){
        boolean groundChanged=snapshot==null||snapshot.ground!=next.ground;snapshot=next;
        if(groundChanged){
            int token=++generation;if(meshTask!=null)meshTask.cancel(true);
            for(GpuMesh m:terrain.values())m.destroy();terrain.clear();chunks=Collections.emptyList();pending=1;
            meshTask=worker.submit(()->{List<SceneMesh> built=SceneMesh.ground(next.ground);post(()->{if(released||token!=generation)return;chunks=built;pending=0;schedule();});});
        }
        syncObjects();overlay.invalidate();schedule();
    }
    void setTargets(Set<Hex> value){targets=value==null?Collections.emptySet():new HashSet<>(value);overlay.invalidate();}
    void setRoute(MarchOrders.Plan value){route=value;overlay.invalidate();}
    void replay(TurnJournal.Event e,float fraction){replay=e;replayFraction=fraction;}
    boolean visible(TurnJournal.Event e){return visible(e.start)||visible(e.target);}
    private boolean visible(Hex h){if(h==null||snapshot==null)return false;GridWorldTransform g=snapshot.ground.grid;return Math.abs(camera.screenX(g.x(h))-camera.width/2f)<camera.width*.6&&Math.abs(camera.screenY(g.z(h),0)-camera.height/2f)<camera.height*.6;}
    void diagnostics(boolean value){diagnostics=value;overlay.invalidate();}
    String report(){return "Filament 1.77.0 / OpenGL ES\n"+camera.width+" × "+camera.height+" · chunks "+visibleChunks+" / GPU "+terrain.size()+" · objects "+visibleObjects+"\n帧回调间隔 "+String.format(java.util.Locale.ROOT,"%.1f",callbackMillis)+" ms（非 GPU/FPS 实测）\n待装载 "+pending+" · S01 过渡代理资源";}
    void resume(boolean value){resumed=value;if(value)schedule();else cancelFrame();}
    private void cancelFrame(){Choreographer.getInstance().removeFrameCallback(this);queued=false;lastFrame=0;}
    private void schedule(){if(!released&&resumed&&swap!=null&&!queued){queued=true;Choreographer.getInstance().postFrameCallback(this);}}
    @Override public void surfaceCreated(SurfaceHolder holder){if(released)return;try{swap=engine.createSwapChain(holder.getSurface());schedule();}catch(RuntimeException|LinkageError e){failure.accept(e);}}
    @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int height){if(released)return;camera.width=Math.max(1,w);camera.height=Math.max(1,height);view.setViewport(new Viewport(0,0,w,height));schedule();}
    @Override public void surfaceDestroyed(SurfaceHolder holder){cancelFrame();if(engine!=null&&swap!=null){engine.destroySwapChain(swap);swap=null;engine.flushAndWait();}}
    @Override public void doFrame(long time){
        queued=false;if(released||!resumed||swap==null)return;
        try{
            if(lastFrame!=0)callbackMillis=(time-lastFrame)/1e6;lastFrame=time;
            double aspect=camera.width/(double)camera.height;
            lens.setProjection(Camera.Projection.ORTHO,-camera.span*aspect,camera.span*aspect,-camera.span,camera.span,.1,1000);
            lens.lookAt(camera.x,300*camera.sin(),camera.z+300*camera.cos(),camera.x,0,camera.z,0,1,0);
            loadVisible();animateReplay();
            if(renderer.beginFrame(swap,time)){renderer.render(view);renderer.endFrame();}
            overlay.invalidate();schedule();
        }catch(RuntimeException|LinkageError e){cancelFrame();failure.accept(e);}
    }
    private boolean inView(float x,float z,float radius){return Math.abs(x-camera.x)<camera.span*camera.width/camera.height+radius+2&&Math.abs(z-camera.z)<camera.span/camera.sin()+radius+2;}
    private void loadVisible(){
        if(snapshot==null)return;int budget=2;visibleChunks=0;pending=meshTask!=null&&!meshTask.isDone()?1:0;
        for(SceneMesh chunk:chunks){
            boolean shown=inView(chunk.x,chunk.z,chunk.radius);GpuMesh gpu=terrain.get(chunk);
            if(shown){visibleChunks++;if(gpu==null){if(budget-->0){gpu=new GpuMesh(chunk);terrain.put(chunk,gpu);}else pending++;}}
            if(gpu!=null){gpu.show(shown);if(!shown&&!inView(chunk.x,chunk.z,chunk.radius+20)){gpu.destroy();terrain.remove(chunk);}}
        }
        visibleObjects=0;
        for(Proxy p:objects.values()){boolean shown=inView(snapshot.ground.grid.x(p.item.hex),snapshot.ground.grid.z(p.item.hex),2);if(shown)visibleObjects++;if(shown!=p.shown){if(shown)scene.addEntity(p.entity);else scene.removeEntity(p.entity);p.shown=shown;}}
    }
    private void syncObjects(){
        if(engine==null||snapshot==null)return;Set<String> alive=new HashSet<>();
        for(MapSceneSnapshot.Item item:snapshot.items){
            alive.add(item.key);Proxy p=objects.get(item.key);
            if(p!=null&&(p.item.kind!=item.kind||p.item.color!=item.color)){p.destroy();objects.remove(item.key);p=null;}
            if(p==null){String shape=item.kind+":"+item.color;GpuMesh mesh=shapes.get(shape);if(mesh==null){mesh=new GpuMesh(SceneMesh.proxy(item.kind,item.color));shapes.put(shape,mesh);}p=new Proxy(item,mesh);objects.put(item.key,p);}
            p.item=item;p.position(snapshot.ground.grid.x(item.hex),snapshot.ground.grid.z(item.hex));
        }
        Iterator<Map.Entry<String,Proxy>> it=objects.entrySet().iterator();while(it.hasNext()){Map.Entry<String,Proxy> e=it.next();if(!alive.contains(e.getKey())){e.getValue().destroy();it.remove();}}
    }
    private void animateReplay(){
        if(replay==null||snapshot==null)return;World.Unit actor=replay.actorCopy();if(actor==null)return;Proxy p=objects.get("unit:"+actor.id);if(p==null)return;
        Hex a=replay.start,b=replay.target;if(!replay.path.isEmpty()){float t=replayFraction*Math.max(0,replay.path.size()-1);int i=Math.min((int)t,replay.path.size()-1);a=replay.path.get(i);b=replay.path.get(Math.min(i+1,replay.path.size()-1));t-=i;if(a!=null&&b!=null){GridWorldTransform g=snapshot.ground.grid;p.position(g.x(a)+(g.x(b)-g.x(a))*t,g.z(a)+(g.z(b)-g.z(a))*t);}}
    }
    void focus(Hex h){if(snapshot==null||h==null)return;camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);camera.span=10;}
    void center(Hex h){if(snapshot!=null&&h!=null){camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);}}
    void fit(){if(snapshot==null)return;MapSceneSnapshot.Ground g=snapshot.ground;float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){Hex h=new Hex(q,r);if(g.valid(h)){float x=g.grid.x(h),z=g.grid.z(h);minX=Math.min(minX,x);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxZ=Math.max(maxZ,z);}}if(minX==Float.MAX_VALUE)return;camera.x=(minX+maxX)/2;camera.z=(minZ+maxZ)/2;camera.span=(float)Math.max((maxX-minX+3)*camera.height/camera.width,(maxZ-minZ+3)*camera.sin())*.52f;}
    private void clampCamera(){if(snapshot==null)return;float extent=Math.max(snapshot.ground.width,snapshot.ground.height)*1.5f;camera.x=Math.max(-extent,Math.min(extent,camera.x));camera.z=Math.max(-extent,Math.min(extent,camera.z));}
    void saveCamera(Bundle b){b.putFloat("cameraX",camera.x*TileGeometry.DX);b.putFloat("cameraY",camera.z*TileGeometry.DY);b.putFloat("sceneSpan",camera.span);b.putFloat("sceneTilt",camera.tilt);}
    void restoreCamera(Bundle b){camera.x=b.getFloat("cameraX")/TileGeometry.DX;camera.z=b.getFloat("cameraY")/TileGeometry.DY;camera.span=b.getFloat("sceneSpan",15);camera.tilt=b.getFloat("sceneTilt",55);}
    void release(){
        if(released)return;released=true;generation++;cancelFrame();if(meshTask!=null)meshTask.cancel(true);worker.shutdownNow();surface.getHolder().removeCallback(this);
        if(engine==null)return;
        if(swap!=null){engine.destroySwapChain(swap);swap=null;}
        for(Proxy p:objects.values())p.destroy();objects.clear();for(GpuMesh m:terrain.values())m.destroy();terrain.clear();for(GpuMesh m:shapes.values())m.destroy();shapes.clear();
        if(light!=0){scene.removeEntity(light);engine.destroyEntity(light);EntityManager.get().destroy(light);}
        if(material!=null)engine.destroyMaterial(material);
        if(view!=null)engine.destroyView(view);if(scene!=null)engine.destroyScene(scene);if(renderer!=null)engine.destroyRenderer(renderer);
        if(cameraEntity!=0){engine.destroyCameraComponent(cameraEntity);EntityManager.get().destroy(cameraEntity);}engine.flushAndWait();engine.destroy();engine=null;
    }
    private final class GpuMesh {
        final VertexBuffer vb;final IndexBuffer ib;final int entity;final SceneMesh source;boolean shown;
        GpuMesh(SceneMesh m){source=m;
            vb=new VertexBuffer.Builder().vertexCount(m.vertices.length/7).bufferCount(1).attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,28).attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,12,28).build(engine);
            FloatBuffer v=ByteBuffer.allocateDirect(m.vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();v.put(m.vertices).flip();vb.setBufferAt(engine,0,v);
            ib=new IndexBuffer.Builder().indexCount(m.indices.length).bufferType(IndexBuffer.Builder.IndexType.UINT).build(engine);IntBuffer i=ByteBuffer.allocateDirect(m.indices.length*4).order(ByteOrder.nativeOrder()).asIntBuffer();i.put(m.indices).flip();ib.setBuffer(engine,i);
            entity=EntityManager.get().create();build(entity);
        }
        void build(int target){new RenderableManager.Builder(1).boundingBox(new Box(source.x,.5f,source.z,source.radius,1,source.radius)).material(0,material.getDefaultInstance()).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vb,ib).castShadows(false).receiveShadows(false).build(engine,target);}
        void show(boolean value){if(shown==value)return;shown=value;if(value)scene.addEntity(entity);else scene.removeEntity(entity);}
        void destroy(){scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);engine.destroyVertexBuffer(vb);engine.destroyIndexBuffer(ib);}
    }
    private final class Proxy {
        final int entity;MapSceneSnapshot.Item item;boolean shown;
        Proxy(MapSceneSnapshot.Item item,GpuMesh shape){this.item=item;entity=EntityManager.get().create();shape.build(entity);}
        void position(float x,float z){float[] matrix={1,0,0,0,0,1,0,0,0,0,1,0,x,.02f,z,1};TransformManager tm=engine.getTransformManager();tm.setTransform(tm.getInstance(entity),matrix);}
        void destroy(){scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);}
    }
    private final class Overlay extends android.view.View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Overlay(Context c){super(c);setClickable(false);}
        void cell(Canvas c,Hex h,int color){if(h==null||snapshot==null)return;float x=camera.screenX(snapshot.ground.grid.x(h)),y=camera.screenY(snapshot.ground.grid.z(h),0),rx=camera.pixels()*.48f,ry=(float)(rx*camera.sin());p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(x-rx,y-ry,x+rx,y+ry,p);}
        @Override protected void onDraw(Canvas c){
            if(snapshot==null)return;
            if(route!=null)for(Hex h:route.path)cell(c,h,0xffffd576);
            for(Hex h:snapshot.reachable)cell(c,h,0x884ed7c2);for(Hex h:snapshot.siege)cell(c,h,0x9975a8fa);for(Hex h:targets)cell(c,h,0xffdd7661);cell(c,snapshot.selected,0xffffd576);
            p.setStyle(Paint.Style.FILL);p.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);p.setShadowLayer(2,0,1,0xff000000);
            for(MapSceneSnapshot.Item item:snapshot.items)if((item.kind<3||camera.span<18)&&visible(item.hex)){float x=camera.screenX(snapshot.ground.grid.x(item.hex)),y=camera.screenY(snapshot.ground.grid.z(item.hex),1);p.setColor(item.color);c.drawText(item.label,x,y,p);}
            p.setColor(0xfff0e5c8);c.drawText("3D 试验 · S01 过渡资源",12,24*getResources().getDisplayMetrics().density,p);
            if(diagnostics){int y=65;for(String line:report().split("\n")){c.drawText(line,12,y,p);y+=22*getResources().getDisplayMetrics().density;}}
            p.clearShadowLayer();
        }
    }
}
