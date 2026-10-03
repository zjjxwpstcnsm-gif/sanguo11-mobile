package game.sanguo.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.filament.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/** Original baked controller/GPU packets, on the normal admitted owner frame.
 * Templates/pixels are source assets; lens, encoded-space blend parity, face
 * variant and the remaining critical bindings still require acceptance. */
final class PcPresentations implements AutoCloseable {
    private final Context context;private final Engine engine;private final Scene scene;
    private final ExecutorService loader=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"PC presentation assets");t.setDaemon(true);return t;});
    private volatile Prepared prepared;private volatile boolean closed;private volatile String error="";
    private final Material[] materials=new Material[2];
    private final boolean[] compiled=new boolean[2];private int pendingTextures;
    private boolean warmSubmitted,warmDone;private Fence warmFence;
    private final Map<Integer,Texture> textures=new HashMap<>();
    private final Map<Integer,MaterialInstance[]> instances=new HashMap<>();
    private final List<Integer> entities=new ArrayList<>();
    private VertexBuffer vertices;private IndexBuffer indices;private int shown,visibleBatches,capacity;
    private final ArrayDeque<FloatBuffer> buffers=new ArrayDeque<>();
    private float[] output;private final float[] cameraPose=new float[16];
    private PcPresentationPlan.Cue cue,submittedCue;private float phase,submittedPhase;private long rendered,textureBytes;
    private static final class Prepared {
        final Map<Integer,PcPresentationTimeline> timelines=new HashMap<>();
        final Map<Integer,Bitmap> images=new LinkedHashMap<>();
        void recycle(){for(Bitmap b:images.values())if(!b.isRecycled())b.recycle();images.clear();}
    }
    PcPresentations(Context context,Engine engine,Scene scene){
        this.context=context.getApplicationContext();this.engine=engine;this.scene=scene;
        loader.execute(()->{
            Prepared result=new Prepared();
            try{
                Set<Integer> images=new TreeSet<>();
                for(int template:PcPresentationPlan.TEMPLATES){
                    PcPresentationTimeline timeline=PcPresentationTimeline.read(this.context.getAssets().open("3d/pc-presentations/template-"+template+".pcps"),template);
                    result.timelines.put(template,timeline);
                    for(ByteBuffer frame:timeline.frames)for(int at=0;at<frame.limit();at+=PcPresentationTimeline.RECORD_BYTES){int image=frame.getInt(at);if(image!=32)images.add(image);}
                }
                for(int image:images)result.images.put(image,bitmap(String.format(Locale.ROOT,"3d/pc-effects/image-%02d.png",image)));
                for(int selector:PcPresentationPlan.SELECTORS)result.images.put(1000+selector,bitmap("3d/pc-presentations/selector-"+selector+".png"));
                synchronized(this){if(closed)result.recycle();else prepared=result;}
            }catch(Exception failure){result.recycle();error=failure.toString();android.util.Log.e("Sanguo3D","Original presentation assets failed",failure);}
        });
    }
    private Bitmap bitmap(String path)throws IOException{
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inPremultiplied=false;
        try(InputStream in=context.getAssets().open(path)){Bitmap b=BitmapFactory.decodeStream(in,null,options);if(b==null)throw new IOException("Missing original image "+path);return b;}
    }
    void set(PcPresentationPlan.Cue value,float phase){if(value!=cue||phase<this.phase)submittedCue=null;cue=value;this.phase=Math.min(1,Math.max(0,phase));}
    boolean submitted(PcPresentationPlan.Cue value,float fraction){return rendered>0&&submittedCue==value&&Math.abs(submittedPhase-fraction)<.0001f;}
    private boolean assetsReady(){Prepared p=prepared;return p!=null&&p.images.isEmpty()&&pendingTextures==0&&compiled[0]&&compiled[1]&&capacity>0;}
    boolean ready(){
        if(warmFence!=null){Fence.FenceStatus status=warmFence.wait(Fence.Mode.DONT_FLUSH,0);
            if(status==Fence.FenceStatus.ERROR)throw new IllegalStateException("Source shader warmup driver fence");
            if(status==Fence.FenceStatus.CONDITION_SATISFIED){engine.destroyFence(warmFence);warmFence=null;warmDone=true;hide();}}
        return !error.isEmpty()||assetsReady()&&warmDone;
    }
    // Engine Fence proves processing of the actual warmup draw commands, not
    // GPU completion. Drivers without parallel compilation need a real draw.
    void afterRender(){if(warmSubmitted&&!warmDone&&warmFence==null){warmFence=engine.createFence();engine.flush();}}
    private void warmShaders(){
        if(warmSubmitted)return;
        FloatBuffer zero=ByteBuffer.allocateDirect(capacity*144).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertices.setBufferAt(engine,0,zero);
        MaterialInstance[] pair=instances.values().iterator().next();
        RenderableManager manager=engine.getRenderableManager();
        for(int i=0;i<2;i++){int instance=manager.getInstance(entities.get(i));manager.setGeometryAt(instance,0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices,0,6);manager.setMaterialInstanceAt(instance,0,pair[i]);scene.addEntity(entities.get(i));}
        // Zero-area source buffer produces no pixels and changes no map asset.
        visibleBatches=2;warmSubmitted=true;
    }
    /** Called after beginFrame accepts the frame. At most two startup textures
     * upload; all later buffers are pooled and bounded by three in-flight slots. */
    void frame(Camera camera){
        if(closed)return;if(!error.isEmpty())throw new IllegalStateException(error);
        Prepared p=prepared;if(p==null)return;
        try{
            for(int i=0;i<2;i++)if(materials[i]==null){String path="3d/pc-presentations/"+(i==0?"over-encoded":"add-encoded")+".filamat";byte[] bytes=VerifiedMaterial.read(path,context.getAssets().open(path));materials[i]=new Material.Builder().payload(ByteBuffer.wrap(bytes),bytes.length).build(engine);final int slot=i;materials[i].compile(Material.CompilerPriorityQueue.HIGH,0,new android.os.Handler(android.os.Looper.getMainLooper()),()->{if(!closed)compiled[slot]=true;});engine.flush();}
            int uploads=0;
            for(Iterator<Map.Entry<Integer,Bitmap>> iterator=p.images.entrySet().iterator();iterator.hasNext()&&uploads<2;uploads++){
                Map.Entry<Integer,Bitmap> image=iterator.next();int key=image.getKey();Bitmap pixels=image.getValue();iterator.remove();loadTexture(key,pixels);
            }
            if(capacity==0){int quads=0,batches=0;for(PcPresentationTimeline t:p.timelines.values()){quads=Math.max(quads,t.maximum);batches=Math.max(batches,t.maximumBatches);}ensureCapacity(quads,batches);}
            if(!assetsReady())return;
            if(!ready()){warmShaders();return;}
            if(cue==null||phase>=1){hide();return;}
            PcPresentationTimeline timeline=p.timelines.get(cue.template);if(timeline==null)throw new IOException("Unsupported original template");
            ByteBuffer data=timeline.frame(phase);int count=data.limit()/PcPresentationTimeline.RECORD_BYTES;
            FloatBuffer buffer=buffers.pollFirst();if(buffer==null)return;
            camera.getModelMatrix(cameraPose);
            for(int i=0;i<count;i++)for(int v=0;v<4;v++)PcPresentationTimeline.vertex(data,i*PcPresentationTimeline.RECORD_BYTES,v,cameraPose,output,(i*4+v)*9);
            buffer.clear();buffer.put(output,0,count*36).flip();
            if(count>0)vertices.setBufferAt(engine,0,buffer,0,0,new android.os.Handler(android.os.Looper.getMainLooper()),()->{if(!closed)buffers.addLast(buffer);});else buffers.addLast(buffer);
            RenderableManager manager=engine.getRenderableManager();
            int batch=0;
            for(int i=0;i<count;batch++){
                int end=PcPresentationTimeline.batchEnd(data,i);
                int at=i*PcPresentationTimeline.RECORD_BYTES,image=data.getInt(at);if(image==32)image=1000+cue.selector;
                MaterialInstance[] pair=instances.get(image);if(pair==null)throw new IOException("Unbound original presentation texture "+image);
                int instance=manager.getInstance(entities.get(batch));
                manager.setGeometryAt(instance,0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices,i*6,(end-i)*6);
                manager.setMaterialInstanceAt(instance,0,pair[data.getInt(at+4)==6?0:1]);
                if(batch>=visibleBatches)scene.addEntity(entities.get(batch));i=end;
            }
            for(int i=batch;i<visibleBatches;i++)scene.removeEntity(entities.get(i));visibleBatches=batch;shown=count;submittedPhase=phase;submittedCue=cue;rendered++;
        }catch(IOException failure){error=failure.toString();hide();throw new IllegalStateException(failure);}
    }
    private void loadTexture(int key,Bitmap bitmap){
        Texture texture=new Texture.Builder().width(bitmap.getWidth()).height(bitmap.getHeight()).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RGBA8).build(engine);
        textures.put(key,texture);textureBytes+=(long)bitmap.getWidth()*bitmap.getHeight()*4;
        boolean queued=false;pendingTextures++;
        try{com.google.android.filament.android.TextureHelper.setBitmap(engine,texture,0,bitmap,new android.os.Handler(android.os.Looper.getMainLooper()),()->{bitmap.recycle();if(!closed)pendingTextures--;});queued=true;}
        finally{if(!queued){pendingTextures--;bitmap.recycle();}}
        MaterialInstance[] pair=new MaterialInstance[2];instances.put(key,pair);
        for(int i=0;i<2;i++){pair[i]=materials[i].createInstance();pair[i].setParameter("image",texture,new TextureSampler(TextureSampler.MinFilter.LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.REPEAT));}
    }
    private void ensureCapacity(int count,int batches){
        if(capacity==0){
            capacity=Math.max(1,count);
            output=new float[capacity*36];
            vertices=new VertexBuffer.Builder().vertexCount(capacity*4).bufferCount(1)
                .attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,36)
                .attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,12,36)
                .attribute(VertexBuffer.VertexAttribute.UV0,0,VertexBuffer.AttributeType.FLOAT2,28,36).build(engine);
            indices=new IndexBuffer.Builder().indexCount(capacity*6).bufferType(IndexBuffer.Builder.IndexType.UINT).build(engine);
            IntBuffer data=ByteBuffer.allocateDirect(capacity*24).order(ByteOrder.nativeOrder()).asIntBuffer();
            for(int i=0;i<capacity;i++){int v=i*4;data.put(v).put(v+1).put(v+2).put(v+2).put(v+1).put(v+3);}data.flip();indices.setBuffer(engine,data);
            for(int slot=0;slot<3;slot++)buffers.add(ByteBuffer.allocateDirect(capacity*144).order(ByteOrder.nativeOrder()).asFloatBuffer());
        }
        if(count>capacity)throw new IllegalStateException("Original presentation capacity invariant");
        while(entities.size()<batches){
            int index=entities.size(),entity=EntityManager.get().create();entities.add(entity);
            new RenderableManager.Builder(1).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices,index*6,6)
                .material(0,materials[0].getDefaultInstance()).boundingBox(new Box(0,0,0,1,1,1)).culling(false)
                .castShadows(false).receiveShadows(false).priority(7).blendOrder(0,index).globalBlendOrderEnabled(0,true).build(engine,entity);
        }
    }
    private void hide(){for(int i=0;i<visibleBatches;i++)scene.removeEntity(entities.get(i));visibleBatches=0;shown=0;}
    String report(){return " pc_presentation_ready="+ready()+" pc_presentation_selector="+(cue==null?-1:cue.selector)+" pc_presentation_phase="+phase+" pc_presentation_frames="+rendered+" pc_presentation_quads="+shown+" pc_presentation_batches="+visibleBatches+" pc_presentation_texture_bytes="+textureBytes+" pc_presentation_error="+(error.isEmpty()?"none":error.replace(' ','_'));}
    @Override public void close(){
        synchronized(this){if(closed)return;closed=true;if(prepared!=null)prepared.recycle();prepared=null;}loader.shutdownNow();cue=null;hide();if(warmFence!=null){engine.destroyFence(warmFence);warmFence=null;}
        for(int entity:entities){engine.destroyEntity(entity);EntityManager.get().destroy(entity);}entities.clear();
        if(vertices!=null)engine.destroyVertexBuffer(vertices);if(indices!=null)engine.destroyIndexBuffer(indices);vertices=null;indices=null;buffers.clear();output=null;
        for(MaterialInstance[] pair:instances.values())for(MaterialInstance instance:pair)if(instance!=null)engine.destroyMaterialInstance(instance);instances.clear();
        for(Texture texture:textures.values())engine.destroyTexture(texture);textures.clear();
        for(int i=0;i<2;i++){if(materials[i]!=null)engine.destroyMaterial(materials[i]);materials[i]=null;}capacity=0;textureBytes=0;
    }
}
