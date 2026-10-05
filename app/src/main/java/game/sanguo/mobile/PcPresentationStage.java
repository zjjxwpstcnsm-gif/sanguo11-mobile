package game.sanguo.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.SurfaceView;
import com.google.android.filament.*;
import java.io.IOException;
import java.nio.*;

/** Bounded presentation scene over one real map framebuffer capture.
 * The map and authority remain intact; no alternative art or simulation clock.
 * PixelCopy completion, texture delivery and actual driver warmup precede playback.
 */
final class PcPresentationStage implements AutoCloseable {
    final Scene scene;
    final Camera camera;
    java.util.function.Consumer<Bitmap> captureObserver; // Optional installed-test evidence, null in normal play.
    java.util.function.BiConsumer<String,Bitmap> rasterObserver; // One source/stage GPU readback per installed diagnostic; null in normal play.
    private int rasterObserved;
    private final Engine engine;
    private final int cameraEntity;private final float[] cameraPose=new float[16];private final double[] projection=new double[16];
    private final com.google.android.filament.View view;
    private final Handler owner=new Handler(Looper.getMainLooper());
    private final Material material;private final MaterialInstance instance;
    private final VertexBuffer vertices;private final IndexBuffer indices;private final int entity;
    private final Texture warmTexture;private final RenderTarget warmTarget;
    private Texture captured,composition;private RenderTarget compositionTarget;private Fence fence;
    private final boolean srgb;private com.google.android.filament.View outputView;private Scene outputScene;private MaterialInstance outputInstance;private int outputEntity;private int compositionWidth,compositionHeight;
    private boolean closed,copyPending,texturePending,warmSubmitted,warmed;
    private final int[] referencePixels=new int[64];
    private int epoch,width,height;private long captures,bytes,screenFrames;private String error="";
    PcPresentationStage(Context context,Engine engine,boolean srgb)throws IOException{
        this.engine=engine;this.srgb=srgb;cameraEntity=EntityManager.get().create();camera=engine.createCamera(cameraEntity);
        scene=engine.createScene();view=engine.createView();view.setScene(scene);view.setCamera(camera);
        view.setAntiAliasing(com.google.android.filament.View.AntiAliasing.NONE);view.setPostProcessingEnabled(false);
        byte[] payload=VerifiedMaterial.read("3d/pc-presentations/backdrop.filamat",context.getAssets().open("3d/pc-presentations/backdrop.filamat"));
        material=new Material.Builder().payload(ByteBuffer.wrap(payload),payload.length).build(engine);instance=material.createInstance();instance.setParameter("decode",false);instance.setParameter("renderTarget",false);
        vertices=new VertexBuffer.Builder().vertexCount(4).bufferCount(1).attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,12).build(engine);
        FloatBuffer quad=ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder()).asFloatBuffer();quad.put(new float[]{-1,-1,0,1,-1,0,-1,1,0,1,1,0}).flip();vertices.setBufferAt(engine,0,quad);
        indices=new IndexBuffer.Builder().indexCount(6).bufferType(IndexBuffer.Builder.IndexType.USHORT).build(engine);
        ShortBuffer triangles=ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder()).asShortBuffer();triangles.put(new short[]{0,1,2,2,1,3}).flip();indices.setBuffer(engine,triangles);
        entity=EntityManager.get().create();new RenderableManager.Builder(1).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices).material(0,instance).boundingBox(new Box(0,0,0,1,1,1)).culling(false).castShadows(false).receiveShadows(false).priority(0).build(engine,entity);
        warmTexture=new Texture.Builder().width(1).height(1).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RGBA8).usage(Texture.Usage.COLOR_ATTACHMENT).build(engine);
        warmTarget=new RenderTarget.Builder().texture(RenderTarget.AttachmentPoint.COLOR,warmTexture).build(engine);
        if(srgb){
            outputScene=engine.createScene();outputView=engine.createView();outputView.setScene(outputScene);outputView.setCamera(camera);outputView.setPostProcessingEnabled(false);outputView.setAntiAliasing(com.google.android.filament.View.AntiAliasing.NONE);
            outputInstance=material.createInstance();outputInstance.setParameter("decode",true);outputInstance.setParameter("renderTarget",true);
            outputEntity=EntityManager.get().create();new RenderableManager.Builder(1).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vertices,indices).material(0,outputInstance).boundingBox(new Box(0,0,0,1,1,1)).culling(false).castShadows(false).receiveShadows(false).build(engine,outputEntity);outputScene.addEntity(outputEntity);
        }
    }
    void camera(Camera mapCamera,boolean perspective){
        mapCamera.getModelMatrix(cameraPose);camera.setModelMatrix(cameraPose);
        mapCamera.getProjectionMatrix(projection);
        // Loading can already publish PC terrain while the host is still
        // switching from its compatible orthographic camera. Warm the stage
        // with that real camera; source-depth conversion applies only once
        // the PC perspective is installed.
        if(perspective){PcPresentationLens.depthRange(projection);camera.setCustomProjection(projection,PcPresentationLens.NEAR,PcPresentationLens.FAR);}
        else camera.setCustomProjection(projection,mapCamera.getNear(),mapCamera.getCullingFar());
    }
    void invalidate(){
        epoch++;screenFrames=0;rasterObserved=0;copyPending=false;warmSubmitted=false;warmed=false;error="";
        if(fence!=null){engine.destroyFence(fence);fence=null;}
        scene.removeEntity(entity);if(captured!=null){engine.destroyTexture(captured);captured=null;}bytes=0;
    }
    void capture(Renderer renderer,SurfaceView surface,int w,int h){
        if(closed||copyPending||captured!=null||w<=0||h<=0)return;
        if(!surface.getHolder().getSurface().isValid())return;
        observeRaster(renderer,1,"source",w,h);
        final int token=epoch;Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);copyPending=true;
        PixelCopy.request(surface,bitmap,result->{
            if(closed||token!=epoch){bitmap.recycle();return;}copyPending=false;
            if(result!=PixelCopy.SUCCESS){bitmap.recycle();error="PixelCopy="+result;return;}
            width=w;height=h;captures++;bytes=(long)w*h*4;
            if(captureObserver!=null)captureObserver.accept(bitmap);
            for(int y=0;y<8;y++)for(int x=0;x<8;x++)referencePixels[y*8+x]=bitmap.getPixel((2*x+1)*w/16,(2*y+1)*h/16);
            captured=new Texture.Builder().width(w).height(h).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RGBA8).build(engine);
            instance.setParameter("image",captured,new TextureSampler(TextureSampler.MinFilter.NEAREST,TextureSampler.MagFilter.NEAREST,TextureSampler.WrapMode.CLAMP_TO_EDGE));
            scene.addEntity(entity);texturePending=true;
            com.google.android.filament.android.TextureHelper.setBitmap(engine,captured,0,bitmap,owner,()->{bitmap.recycle();if(!closed&&token==epoch)texturePending=false;});
        },owner);
    }
    boolean ready(){
        if(!error.isEmpty())throw new IllegalStateException("Original map capture "+error);
        if(fence!=null){Fence.FenceStatus status=fence.wait(Fence.Mode.DONT_FLUSH,0);if(status==Fence.FenceStatus.ERROR)throw new IllegalStateException("Captured map warmup driver fence");if(status==Fence.FenceStatus.CONDITION_SATISFIED){engine.destroyFence(fence);fence=null;warmed=true;}}
        return captured!=null&&!texturePending&&warmed;
    }
    void warm(Renderer renderer){
        // Material parameters are prepared at the frame boundary. Keep the
        // final captured-raster dimensions even while the warm target is1x1;
        // otherwise the first screen frame still divides gl_FragCoord by1
        // and clamps almost every pixel to the captured image's edge.
        instance.setParameter("viewport",(float)Math.max(1,width),(float)Math.max(1,height));view.setRenderTarget(warmTarget);view.setViewport(new Viewport(0,0,1,1));renderer.render(view);
        if(captured!=null&&!texturePending&&!warmSubmitted){warmSubmitted=true;fence=engine.createFence();engine.flush();}
    }
    private void destroyComposition(){if(compositionTarget!=null)engine.destroyRenderTarget(compositionTarget);if(composition!=null)engine.destroyTexture(composition);compositionTarget=null;composition=null;compositionWidth=compositionHeight=0;}
    void render(Renderer renderer,int w,int h){
        // Source SRCALPHA/INVSRCALPHA and SRCALPHA/ONE compose in encoded
        // RGBA8. Only the final presentation converts for an sRGB swap chain.
        if(srgb&&(composition==null||compositionWidth!=w||compositionHeight!=h)){
            destroyComposition();compositionWidth=w;compositionHeight=h;
            composition=new Texture.Builder().width(w).height(h).levels(1).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RGBA8).usage(Texture.Usage.COLOR_ATTACHMENT|Texture.Usage.SAMPLEABLE).build(engine);
            compositionTarget=new RenderTarget.Builder().texture(RenderTarget.AttachmentPoint.COLOR,composition).build(engine);
            outputInstance.setParameter("image",composition,new TextureSampler(TextureSampler.MinFilter.NEAREST,TextureSampler.MagFilter.NEAREST,TextureSampler.WrapMode.CLAMP_TO_EDGE));
        }
        instance.setParameter("viewport",(float)w,(float)h);view.setRenderTarget(srgb?compositionTarget:null);view.setViewport(new Viewport(0,0,w,h));renderer.render(view);
        if(srgb){outputInstance.setParameter("viewport",(float)w,(float)h);outputView.setViewport(new Viewport(0,0,w,h));renderer.render(outputView);}screenFrames++;
        if(screenFrames==1)observeRaster(renderer,4,"stage-first",w,h);
        if(screenFrames>=3)observeRaster(renderer,2,"stage",w,h);
    }
    private void observeRaster(Renderer renderer,int bit,String label,int w,int h){
        if(rasterObserver==null||(rasterObserved&bit)!=0)return;rasterObserved|=bit;
        final int token=epoch;ByteBuffer pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(w,h),4));
        // Filament1.56 readPixels is asynchronous, within an admitted frame.
        // It bypasses Android's Surface/PixelCopy sampling. This diagnostic
        // must never run in normal play or serve as a GPU timing measurement.
        renderer.readPixels(0,0,w,h,new Texture.PixelBufferDescriptor(pixels,Texture.Format.RGBA,Texture.Type.UBYTE,1,0,0,0,owner,()->{
            if(closed||token!=epoch||rasterObserver==null)return;
            int[] colors=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int at=((h-1-y)*w+x)*4;colors[y*w+x]=((pixels.get(at+3)&255)<<24)|((pixels.get(at)&255)<<16)|((pixels.get(at+1)&255)<<8)|(pixels.get(at+2)&255);}
            Bitmap bitmap=Bitmap.createBitmap(colors,w,h,Bitmap.Config.ARGB_8888);
            try{rasterObserver.accept(label,bitmap);}finally{bitmap.recycle();}
        }));
    }
    String report(){return " pc_stage_encoded_rgba8=true pc_stage_srgb_output="+srgb+" pc_stage_screen_frames="+screenFrames+" pc_stage_captures="+captures+" pc_stage_bytes="+bytes+" pc_stage_size="+width+"x"+height+" pc_stage_near_far="+PcPresentationLens.NEAR+":"+PcPresentationLens.FAR+" pc_stage_ready="+ready();}
    @Override public void close(){
        if(closed)return;invalidate();closed=true;captureObserver=null;rasterObserver=null;
        if(outputView!=null){engine.destroyView(outputView);engine.destroyScene(outputScene);engine.destroyEntity(outputEntity);EntityManager.get().destroy(outputEntity);engine.destroyMaterialInstance(outputInstance);}
        engine.destroyEntity(entity);EntityManager.get().destroy(entity);
        engine.destroyVertexBuffer(vertices);engine.destroyIndexBuffer(indices);engine.destroyMaterialInstance(instance);engine.destroyView(view);engine.destroyScene(scene);
        engine.destroyCameraComponent(cameraEntity);EntityManager.get().destroy(cameraEntity);
        destroyComposition();engine.destroyMaterial(material);engine.destroyRenderTarget(warmTarget);engine.destroyTexture(warmTexture);
    }
}
