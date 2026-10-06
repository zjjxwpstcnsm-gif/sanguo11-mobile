package game.sanguo.mobile;

import android.content.Context;
import com.google.android.filament.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The supplied eight SEFF map templates, original update/draw in a private
 * child, source-order quads on the admitted Filament owner frame. No core API.
 * PC lens/shader/runtime override and sustained performance remain unverified.
 */
final class PcMapEffects implements AutoCloseable {
    private final Context context;
    private final Engine engine;
    private final Scene scene;
    final boolean fireScene;
    private PcCellFireSet acceptedFires;
    private game.sanguo.api.StateToken displayedFireState;
    private final ExecutorService background=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"PC source map effects");t.setDaemon(true);return t;});
    private volatile PcEffectProcess process;
    private volatile boolean closed;
    private boolean busy;
    private double clock,submittedClock;
    private Delivery ready,pending;
    private float[] displayedCamera;
    private volatile String error="";
    private Material material,addMaterial;
    private final Texture[] textures=new Texture[33];
    private final MaterialInstance[][] instances=new MaterialInstance[2][33];
    private final ArrayList<Integer> entities=new ArrayList<>();
    private VertexBuffer vertices;
    private IndexBuffer indices;
    private int capacity,shown,serial;
    private long frames,textureBytes;
    private float sourceElapsed,updateMillis,drawMillis,geometryMillis;
    private static final class Delivery {
        final PcEffectProcess.Frame frame;final float[] camera;final PcCellFireSet fires;
        Delivery(PcEffectProcess.Frame frame,float[] camera,PcCellFireSet fires){this.frame=frame;this.camera=camera;this.fires=fires;}
    }
    PcMapEffects(Context context,Engine engine,Scene scene){this(context,engine,scene,false);}
    PcMapEffects(Context context,Engine engine,Scene scene,boolean fireScene){this.context=context.getApplicationContext();this.engine=engine;this.scene=scene;this.fireScene=fireScene;}

    /** Called only after renderer.beginFrame(true); asynchronous native work
     * has one request and one result slot. No producer can grow a frame queue.
     */
    void frame(float dt,float[] camera,double originX,double originZ){frame(dt,camera,originX,originZ,null);}
    void frame(float dt,float[] camera,double originX,double originZ,PcCellFireSet fires) {
        if(fireScene!=(fires!=null))throw new IllegalArgumentException("Admitted original fire facts required");
        if(acceptedFires!=null&&fires!=null&&!acceptedFires.state.equals(fires.state))hide();
        acceptedFires=fires;
        if(closed)return;
        if(displayedCamera!=null&&!Arrays.equals(displayedCamera,camera))hide();
        synchronized(this) {
            clock+=dt;
            if(ready!=null){pending=ready;ready=null;}
            if(!busy&&ready==null&&error.isEmpty()) {
                float step=dt==0?0:(float)Math.min(.1,Math.max(0,clock-submittedClock));
                submittedClock+=step;busy=true;float[] captured=camera.clone();
                PcCellFireSet capturedFires=fires;
                background.execute(()->{
                    try {
                        PcEffectProcess worker=process;
                        if(worker==null) {
                            worker=PcEffectProcess.open(context,captured,fireScene);
                            synchronized(this){if(closed){worker.close();return;}process=worker;}
                        }
                        PcEffectProcess.Frame result=worker.frame(step,captured,capturedFires);
                        synchronized(this){if(!closed)ready=new Delivery(result,captured,capturedFires);}
                    }catch(Exception failure){error=failure.toString();android.util.Log.e("Sanguo3D","Original map effects stopped",failure);PcEffectProcess worker=process;if(worker!=null)worker.close();}
                    finally{synchronized(this){busy=false;}}
                });
            }
        }
        if(!error.isEmpty()){hide();return;}
        if(pending==null)return;
        if(!Arrays.equals(pending.camera,camera)||fireScene&&!pending.fires.state.equals(fires.state)){pending=null;hide();return;}
        PcEffectProcess.Frame frame=pending.frame;
        ByteBuffer data=frame.records;
        // Source-over map packets and original cell-fire ADD/SRCALPHA/ONE.
        // Unexamined blend modes must remain visible failures, never substitutes.
        for(int i=0;i<frame.count;i++) {
            int p=i*184;
            if(data.getInt(p+8)!=1||data.getInt(p+12)!=5||(data.getInt(p+16)!=6&&data.getInt(p+16)!=2)) {
                error="Unexamined original map blend "+data.getInt(p+8)+"/"+data.getInt(p+12)+"/"+data.getInt(p+16);hide();PcEffectProcess worker=process;if(worker!=null)worker.close();return;
            }
        }
        try {
            if(material==null) {
                byte[] bytes=VerifiedMaterial.read("3d/pc-effects/quad.filamat",context.getAssets().open("3d/pc-effects/quad.filamat"));
                material=new Material.Builder().payload(ByteBuffer.wrap(bytes),bytes.length).build(engine);
            }
            int uploads=0;
            for(int i=0;i<frame.count;i++) {
                int image=data.getInt(i*184+4);
                if(textures[image]==null) {
                    if(uploads==2)return;
                    loadTexture(image);uploads++;
                }
            }
            ensureCapacity(frame.count);
            if(frame.count>0) {
                float[] output=new float[frame.count*4*9];
                for(int i=0;i<frame.count;i++)for(int v=0;v<4;v++)PcEffectCoordinates.vertex(data,i*184,v,originX,originZ,output,(i*4+v)*9);
                FloatBuffer buffer=ByteBuffer.allocateDirect(output.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();buffer.put(output).flip();
                vertices.setBufferAt(engine,0,buffer);
            }
            RenderableManager manager=engine.getRenderableManager();
            for(int i=0;i<frame.count;i++) {
                int entity=entities.get(i),instance=manager.getInstance(entity);
                manager.setMaterialInstanceAt(instance,0,instanceFor(data.getInt(i*184+4),data.getInt(i*184+16)==2));
                if(i>=shown)scene.addEntity(entity);
            }
            for(int i=frame.count;i<shown;i++)scene.removeEntity(entities.get(i));
            shown=frame.count;displayedCamera=pending.camera;displayedFireState=pending.fires==null?null:pending.fires.state;serial=frame.serial;frames++;sourceElapsed=frame.sourceElapsed;
            updateMillis=frame.updateMillis;drawMillis=frame.drawMillis;geometryMillis=frame.geometryMillis;pending=null;
        }catch(java.io.IOException|RuntimeException failure){error=failure.toString();hide();android.util.Log.e("Sanguo3D","Original map effect upload stopped",failure);PcEffectProcess worker=process;if(worker!=null)worker.close();}
    }
    private void loadTexture(int image)throws java.io.IOException {
        android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inScaled=false;options.inPremultiplied=false;
        android.graphics.Bitmap bitmap;
        try(java.io.InputStream in=context.getAssets().open(String.format(java.util.Locale.ROOT,"3d/pc-effects/image-%02d.png",image))){bitmap=android.graphics.BitmapFactory.decodeStream(in,null,options);}
        if(bitmap==null)throw new java.io.IOException("Original effect image "+image);
        boolean queued=false;
        try {
            textures[image]=new Texture.Builder().width(bitmap.getWidth()).height(bitmap.getHeight()).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RGBA8).build(engine);
            com.google.android.filament.android.TextureHelper.setBitmap(engine,textures[image],0,bitmap,new android.os.Handler(android.os.Looper.getMainLooper()),bitmap::recycle);queued=true;
            textureBytes+=(long)bitmap.getWidth()*bitmap.getHeight()*4;
        }finally{if(!queued)bitmap.recycle();}
    }
    private MaterialInstance instanceFor(int image,boolean additive)throws java.io.IOException {
        int blend=additive?1:0;
        if(instances[blend][image]==null) {
            if(additive&&addMaterial==null) {
                String path="3d/pc-effects/quad-add.filamat";
                byte[] bytes=VerifiedMaterial.read(path,context.getAssets().open(path));
                addMaterial=new Material.Builder().payload(ByteBuffer.wrap(bytes),bytes.length).build(engine);
            }
            MaterialInstance instance=(additive?addMaterial:material).createInstance();
            instances[blend][image]=instance;
            // Source linear filtering, D3D9 default WRAP; one shared texture per image.
            instance.setParameter("image",textures[image],new TextureSampler(TextureSampler.MinFilter.LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.REPEAT));
        }
        return instances[blend][image];
    }
    private void ensureCapacity(int count) {
        if(count>capacity) {
            int size=capacity==0?16:capacity;while(size<count)size*=2;
            VertexBuffer next=new VertexBuffer.Builder().vertexCount(size*4).bufferCount(1)
                .attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,36)
                .attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,12,36)
                .attribute(VertexBuffer.VertexAttribute.UV0,0,VertexBuffer.AttributeType.FLOAT2,28,36).build(engine);
            IndexBuffer index=null;
            try {
                IntBuffer data=ByteBuffer.allocateDirect(size*6*4).order(ByteOrder.nativeOrder()).asIntBuffer();
                for(int i=0;i<size;i++){int v=i*4;data.put(v).put(v+1).put(v+2).put(v+2).put(v+1).put(v+3);}data.flip();
                index=new IndexBuffer.Builder().indexCount(size*6).bufferType(IndexBuffer.Builder.IndexType.UINT).build(engine);index.setBuffer(engine,data);
                RenderableManager manager=engine.getRenderableManager();
                for(int i=0;i<entities.size();i++)manager.setGeometryAt(manager.getInstance(entities.get(i)),0,RenderableManager.PrimitiveType.TRIANGLES,next,index,i*6,6);
            }catch(RuntimeException failure){if(index!=null)engine.destroyIndexBuffer(index);engine.destroyVertexBuffer(next);throw failure;}
            if(vertices!=null)engine.destroyVertexBuffer(vertices);if(indices!=null)engine.destroyIndexBuffer(indices);vertices=next;indices=index;capacity=size;
        }
        while(entities.size()<count) {
            int i=entities.size(),entity=EntityManager.get().create();
            try {
                new RenderableManager.Builder(1).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices,i*6,6)
                    .material(0,material.getDefaultInstance()).boundingBox(new Box(0,0,0,1,1,1)).culling(false)
                    .castShadows(false).receiveShadows(false).blendOrder(0,i).globalBlendOrderEnabled(0,true).build(engine,entity);
                entities.add(entity);
            }catch(RuntimeException failure){engine.destroyEntity(entity);EntityManager.get().destroy(entity);throw failure;}
        }
    }
    private void hide(){for(int i=0;i<shown;i++)scene.removeEntity(entities.get(i));shown=0;displayedFireState=null;}
    String report(){return " fire_native="+(process==null?"closed":process.fireSummary().replace(' ','_'))+" fire_scene="+fireScene+" admitted_fire_cells="+(acceptedFires==null?0:acceptedFires.cells.size())+" omitted_fire_cells="+(acceptedFires==null?0:acceptedFires.omitted)+" pc_map_fx_frames="+frames+" pc_map_fx_serial="+serial+" pc_map_fx_quads="+shown+" pc_map_fx_capacity="+capacity+" pc_map_fx_texture_bytes="+textureBytes+" pc_map_fx_source_time="+sourceElapsed+" pc_map_fx_update_ms="+updateMillis+" pc_map_fx_draw_ms="+drawMillis+" pc_map_fx_geometry_ms="+geometryMillis+" pc_map_fx_error="+(error.isEmpty()?"none":error.replace(' ','_'));}
    @Override public void close() {
        synchronized(this){if(closed)return;closed=true;ready=null;pending=null;acceptedFires=null;}
        PcEffectProcess worker=process;if(worker!=null)worker.close();background.shutdownNow();
        hide();for(int entity:entities){engine.destroyEntity(entity);EntityManager.get().destroy(entity);}entities.clear();
        if(vertices!=null)engine.destroyVertexBuffer(vertices);if(indices!=null)engine.destroyIndexBuffer(indices);vertices=null;indices=null;
        for(MaterialInstance[] blend:instances)for(MaterialInstance instance:blend)if(instance!=null)engine.destroyMaterialInstance(instance);
        for(Texture texture:textures)if(texture!=null)engine.destroyTexture(texture);
        for(MaterialInstance[] blend:instances)Arrays.fill(blend,null);Arrays.fill(textures,null);textureBytes=0;capacity=0;displayedCamera=null;
        if(material!=null)engine.destroyMaterial(material);material=null;
        if(addMaterial!=null)engine.destroyMaterial(addMaterial);addMaterial=null;
    }
}
