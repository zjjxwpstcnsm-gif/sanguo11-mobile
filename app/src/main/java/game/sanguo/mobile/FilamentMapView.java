package game.sanguo.mobile;
import game.sanguo.mobile.presentation.MapLayerData;

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

import java.util.function.Consumer;

/** All Filament calls belong to the main Looper. Only CPU mesh building uses the worker.
 * Surface changes replace the swap chain, never the world, commands or replay cursor. */
final class FilamentMapView extends FrameLayout implements SurfaceHolder.Callback,Choreographer.FrameCallback {
    final SceneCamera camera=new SceneCamera();
    private final SceneQuality quality;
    private final SceneQuality.Pacer pacer=new SceneQuality.Pacer();
    private int bufferWidth,bufferHeight;
    private boolean msaaEnabled;
    private final SceneQuality.Thermal thermal=new SceneQuality.Thermal();
    private android.os.PowerManager thermalManager;
    private android.os.PowerManager.OnThermalStatusChangedListener thermalListener;
    private int thermalStatus=-1;
    private long textureBytes;
    private String textureFormat="ETC2_SRGB8";
    private long textureUploadCpuNanos;
    private long groundLoadCpuNanos,groundTextureBytes;
    private final Set<SceneMesh> activeTerrain=new HashSet<>(),wantedWood=new HashSet<>();
    private final SurfaceView surface;
    private final Overlay overlay;
    private final Consumer<Throwable> failure;
    private final MapView.TileListener listener;
    private final SceneAssetQueue assetWork=new SceneAssetQueue();
    private final SceneWorkQueue<MeshResult> meshWork=new SceneWorkQueue<>();
    private static final class MeshResult {
        final SceneMesh scenery; final List<SceneMesh> ground,trees;
        MeshResult(SceneMesh scenery,List<SceneMesh> ground,List<SceneMesh> trees){this.scenery=scenery;this.ground=ground;this.trees=trees;}
    }
    private game.sanguo.api.StateToken sceneToken;
    void sceneIdentity(game.sanguo.api.StateToken token){
        meshWork.owner();
        if(sceneToken!=null&&(!sceneToken.sessionId.equals(token.sessionId)||sceneToken.generation!=token.generation)){
            meshWork.invalidate();assetWork.invalidate();generation++;pending=0;replay=null;animatedUnit=null;clearEffects();
            for(Proxy p:objects.values())p.destroy();objects.clear();
            for(GpuMesh m:shapes.values())m.destroy();shapes.clear();
            for(GpuMesh m:terrain.values())m.destroy();terrain.clear();
            for(GpuMesh m:vegetation.values())m.destroy();vegetation.clear();
            if(backdrop!=null){backdrop.destroy();backdrop=null;}
            chunks=Collections.emptyList();woods=Collections.emptyList();backdropSource=null;snapshot=null;
            criticalHit=null;criticalPortrait=null;route=null;targets=Collections.emptySet();
        }
        sceneToken=token;
    }
    private int generation;
    private Engine engine; private Renderer renderer; private Scene scene;
    private com.google.android.filament.android.DisplayHelper displayHelper;
    private Skybox skybox;
    private com.google.android.filament.View view; private Camera lens; private Material material;
    private Material waterMaterial;
    private double waterSeconds;private long waterLastTick;
    private Material groundMaterial; private final List<Texture> groundTextures=new ArrayList<>();
    private boolean environmentShadows;
    private IndirectLight skyLight;
    private Material siteMaterial; private Texture siteAtlas,fieldAtlas;private FieldAssets fieldAssets;private MaterialInstance vegetationMaterial;
    private boolean assetSyncPending;private int assetUploadBudget=2;
    private int siteLod=1; private final Set<String> missingAssets=new HashSet<>();
    private boolean srgbSwapChain;
    private boolean outputProbePending,outputVerified;
    private int uniformOutputCount;
    private long lastOutputProbe;
    private String outputStatus="WAITING_SURFACE";
    private long surfaceFrames;
    private SwapChain swap; private int cameraEntity,light;
    private boolean released,resumed=true,queued,diagnostics;
    private final long[] cpuSamples=new long[240];private int cpuCount,cpuCursor;
    private long lastFrame; private long renderedFrames; private double callbackMillis;
    private MapSceneSnapshot snapshot;
    private SceneMesh.TerrainWindow terrainWindow;
    // Actual accepted-mesh summary retained for existing native acceptance probes.
    private boolean distantTerrain;
    private GpuMesh backdrop;private SceneMesh backdropSource;
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
    private boolean multi,panelGesture;private int panelRight,panelBottom;
    void setPanelOcclusion(int right,int bottom){panelRight=Math.max(0,right);panelBottom=Math.max(0,bottom);}
    private float multiX=Float.NaN,multiY,multiAngle;
    private String lastPick="none";
    private long suppressedGesture=-1;
    private Set<Hex> editorCells=Collections.emptySet();
    private boolean editorValid=true,editorDrawing,showCommanders=true,showUnitBars=true;
    private MapView.EditorStroke editorStroke;
    private Displacement.Preview tacticPreview;
    void editorDrawing(boolean value){editorDrawing=value;}
    void editorMode(MapView.EditorStroke value){editorStroke=value;}
    void editorPreview(Set<Hex> cells,boolean valid){editorCells=new HashSet<>(cells);editorValid=valid;overlay.invalidate();}
    void setTacticPreview(Displacement.Preview value){tacticPreview=value;overlay.invalidate();}
    void labels(boolean commanders,boolean bars){showCommanders=commanders;showUnitBars=bars;overlay.invalidate();}

    private boolean gridShown;
    void setGridShown(boolean shown){gridShown=shown;overlay.invalidate();}
    private boolean editorGrid,editorCoords,editorFootprints;private Set<Long> impassable=Collections.emptySet();
    private int territoryMode,previewFaction=-1;private boolean openingPreview;
    private int[] territoryColors;private final Map<String,String> factionLabels=new HashMap<>();
    private final Map<String,Integer> siteOwners=new HashMap<>();
    void editorLayers(Set<Long> blocked,boolean grid,boolean coords,boolean footprints){
        editorGrid=grid;editorCoords=coords;editorFootprints=footprints;
        impassable=Set.copyOf(blocked);overlay.invalidate();
    }
    void previewFaction(int side){previewFaction=side;overlay.invalidate();}
    void mapLayers(MapLayerData data,int mode,boolean preview,int side){
        territoryMode=mode;openingPreview=preview;previewFaction=side;
        factionLabels.clear();factionLabels.putAll(data.factionLabels);
        siteOwners.clear();siteOwners.putAll(data.siteOwners);territoryColors=data.colors();
    }
    private final CombatVisual combat=new CombatVisual();
    private final GpuMesh[] effectMeshes=new GpuMesh[6];
    private final int[] effectEntities=new int[CombatVisual.CAPACITY],effectKinds=new int[CombatVisual.CAPACITY];
    private final boolean[] effectShown=new boolean[CombatVisual.CAPACITY];
    private final float[] effectMatrix=new float[16];
    private android.graphics.drawable.Drawable criticalPortrait;
    private CriticalHit criticalHit;private float criticalPhase;private Runnable criticalSkip;
    private TurnJournal.Event replay;private float replayFraction;private Proxy animatedUnit;
    FilamentMapView(Context context,MapView.TileListener listener,Consumer<Throwable> failure) throws Exception {
        super(context);this.listener=listener;this.failure=failure;
        quality=SceneQuality.from(context.getSharedPreferences("map-renderer",0).getAll().get("quality"));
        surface=new SurfaceView(context);addView(surface,new LayoutParams(-1,-1));overlay=new Overlay(context);addView(overlay,new LayoutParams(-1,-1));
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!multi&&!scaler.isInProgress()&&!editorDrawing){camera.pan(-dx,-dy);clampCamera();}return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent e){
                if(criticalHit!=null&&criticalSkip!=null){criticalSkip.run();return true;}
                if(!multi&&e.getDownTime()!=suppressedGesture&&!blocked(e.getX(),e.getY())&&!editorDrawing&&snapshot!=null){Hex h=pick(e.getX(),e.getY(),targets.isEmpty()&&snapshot.reachable.isEmpty());if(snapshot.ground.valid(h)){performClick();listener.tap(h);}}return true;
            }
            @Override public void onLongPress(MotionEvent e){if(!multi&&!editorDrawing&&!blocked(e.getX(),e.getY())&&snapshot!=null){suppressedGesture=e.getDownTime();Hex h=pick(e.getX(),e.getY(),false);if(h!=null){center(h);performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);}}}
            @Override public boolean onDoubleTap(MotionEvent e){if(!multi&&!blocked(e.getX(),e.getY()))zoomAt(1.7f,e.getX(),e.getY());return true;}
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoomAt(d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}});
        try{
            Filament.init();engine=Engine.create(Engine.Backend.OPENGL);
            renderer=engine.createRenderer();scene=engine.createScene();view=engine.createView();
            displayHelper=new com.google.android.filament.android.DisplayHelper(context);
            skybox=new Skybox.Builder().color(.075f,.10f,.11f,1).build(engine);scene.setSkybox(skybox);
            cameraEntity=EntityManager.get().create();lens=engine.createCamera(cameraEntity);view.setScene(scene);view.setCamera(lens);// Prefer the supported display framebuffer encoder; this avoids an unnecessary HDR/LUT pass.
            // Pinned SwapChain.h explicitly supports post-processing off with CONFIG_SRGB_COLORSPACE.
            srgbSwapChain=SwapChain.isSRGBSwapChainSupported(engine);
            view.setPostProcessingEnabled(!srgbSwapChain);
            EnvironmentProfile.exposure(lens); // Fixed daylight exposure for the existing 50,000-lux sun.
            view.setAntiAliasing(com.google.android.filament.View.AntiAliasing.NONE);
            android.app.ActivityManager manager=context.getSystemService(android.app.ActivityManager.class);
            // Filament 1.56 blitLow aborts in the GLES3.0 compatibility driver. Native aborts
            // cannot be recovered by Java try/catch. Keep full 3D but gate this optional resolve.
            msaaEnabled=manager!=null&&quality.msaaSupported(manager.getDeviceConfigurationInfo().reqGlEsVersion);
            com.google.android.filament.View.MultiSampleAntiAliasingOptions msaa=new com.google.android.filament.View.MultiSampleAntiAliasingOptions();msaa.enabled=msaaEnabled;msaa.sampleCount=4;view.setMultiSampleAntiAliasingOptions(msaa);
            Renderer.ClearOptions clear=new Renderer.ClearOptions();clear.clear=true;clear.clearColor=new float[]{.075f,.10f,.11f,1};renderer.setClearOptions(clear);
            byte[] bytes;try(java.io.InputStream in=context.getAssets().open("3d/terrain.filamat")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int count;while((count=in.read(block))!=-1)out.write(block,0,count);bytes=out.toByteArray();}
            ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();material=new Material.Builder().payload(payload,bytes.length).build(engine);
            byte[] siteBytes;try(java.io.InputStream in=context.getAssets().open("3d/sites/site.filamat")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);siteBytes=out.toByteArray();}
            ByteBuffer sb=ByteBuffer.allocateDirect(siteBytes.length).order(ByteOrder.nativeOrder());sb.put(siteBytes).flip();siteMaterial=new Material.Builder().payload(sb,siteBytes.length).build(engine);
            loadGroundMaterials(context);
            loadWaterMaterial(context);
            siteAtlas=loadAtlas(context,"3d/sites/atlas.png");
            fieldAssets=new FieldAssets(name->context.getAssets().open("3d/field/"+name));
            fieldAtlas=loadAtlas(context,"3d/field/atlas.png");
            vegetationMaterial=siteMaterial.createInstance();vegetationMaterial.setParameter("atlas",fieldAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));vegetationMaterial.setParameter("damage",0f);
            light=EntityManager.get().create();environmentShadows=quality!=SceneQuality.LOW&&manager!=null&&manager.getDeviceConfigurationInfo().reqGlEsVersion>=0x30001;EnvironmentProfile.sun(engine,light,quality,environmentShadows);scene.addEntity(light);
            skyLight=EnvironmentProfile.sky(engine);scene.setIndirectLight(skyLight);
            surface.getHolder().addCallback(this);
            if(android.os.Build.VERSION.SDK_INT>=29){
                thermalManager=context.getSystemService(android.os.PowerManager.class);
                if(thermalManager!=null){thermalListener=this::thermalChanged;thermalManager.addThermalStatusListener(context.getMainExecutor(),thermalListener);thermalChanged(thermalManager.getCurrentThermalStatus());}
            }
        }catch(Exception|LinkageError|OutOfMemoryError e){release();throw e;}
    }
    private void loadGroundMaterials(Context context)throws java.io.IOException {
        long groundStarted=System.nanoTime();
        byte[] bytes;try(java.io.InputStream in=context.getAssets().open("3d/terrain/ground.filamat")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);bytes=out.toByteArray();}
        ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();
        groundMaterial=new Material.Builder().payload(payload,bytes.length).build(engine);
        TextureSampler sampler=new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.REPEAT);
        for(String layer:new String[]{"grass","soil","sand","rock"})for(boolean normal:new boolean[]{false,true}){
            android.graphics.Bitmap bitmap;
            android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inScaled=false;options.inPremultiplied=false;
            try(java.io.InputStream in=context.getAssets().open("3d/terrain/"+layer+(normal?"_normal_roughness.png":"_color.png"))){bitmap=android.graphics.BitmapFactory.decodeStream(in,null,options);}
            if(bitmap==null)throw new java.io.IOException("Missing terrain texture "+layer);
            int w=bitmap.getWidth(),h=bitmap.getHeight(),levels=1+(int)(Math.log(Math.max(w,h))/Math.log(2));
            Texture texture=new Texture.Builder().width(w).height(h).levels(levels).sampler(Texture.Sampler.SAMPLER_2D).format(normal?Texture.InternalFormat.RGBA8:Texture.InternalFormat.SRGB8_A8).build(engine);
            groundTextures.add(texture); // Own immediately, so initialization failures release partial uploads.
            com.google.android.filament.android.TextureHelper.setBitmap(engine,texture,0,bitmap);
            texture.generateMipmaps(engine);long mipBytes=0;for(int level=0;level<levels;level++)mipBytes+=(long)Math.max(1,w>>level)*Math.max(1,h>>level)*4;
            textureBytes+=mipBytes;groundTextureBytes+=mipBytes;
            groundMaterial.getDefaultInstance().setParameter(layer+(normal?"Normal":"Color"),texture,sampler);
        }
        groundMaterial.getDefaultInstance().setParameter("normalStrength",quality==SceneQuality.LOW?0f:.65f);
        groundLoadCpuNanos=System.nanoTime()-groundStarted;
    }
    private void loadWaterMaterial(Context context)throws java.io.IOException {
        byte[] bytes;try(java.io.InputStream in=context.getAssets().open("3d/terrain/water.filamat")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);bytes=out.toByteArray();}
        ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();
        waterMaterial=new Material.Builder().payload(payload,bytes.length).build(engine);
        // Borrow the four existing sRGB albedos; ownership stays with groundTextures.
        // No duplicate uploads, normal maps, reflection targets or transparent water pass.
        TextureSampler bankSampler=new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.REPEAT);
        String[] layers={"grass","soil","sand","rock"};
        for(int i=0;i<layers.length;i++)waterMaterial.getDefaultInstance().setParameter(layers[i]+"Color",groundTextures.get(i*2),bankSampler);
        waterMaterial.getDefaultInstance().setParameter("waveTime",0f);
        waterMaterial.getDefaultInstance().setParameter("waveStrength",quality==SceneQuality.LOW?.035f:.065f);
    }
    private Texture loadAtlas(Context context,String path)throws java.io.IOException {
        long started=System.nanoTime();
        if(Texture.isTextureFormatSupported(engine,Texture.InternalFormat.ETC2_SRGB8)){
            try(java.io.DataInputStream in=new java.io.DataInputStream(context.getAssets().open(path.replace(".png",".etc2")))){
                if(in.readInt()!=0x45544332)throw new java.io.IOException("ETC2 header");
                int w=in.readInt(),h=in.readInt(),levels=in.readInt();
                if(w<1||h<1||w>2048||h>2048||levels!=32-Integer.numberOfLeadingZeros(Math.max(w,h)))throw new java.io.IOException("ETC2 bounds");
                int skip=0;while(Math.max(w>>skip,h>>skip)>quality.atlasSize)skip++;
                // Validate all lengths before allocating a native resource.
                java.util.List<byte[]> payloads=new ArrayList<>();
                for(int level=0;level<levels;level++){
                    int lw=Math.max(1,w>>level),lh=Math.max(1,h>>level),size=in.readInt();
                    if(size!=((lw+3)/4)*((lh+3)/4)*8)throw new java.io.IOException("ETC2 level size");
                    byte[] bytes=new byte[size];in.readFully(bytes);if(level>=skip)payloads.add(bytes);
                }
                if(in.read()!=-1)throw new java.io.IOException("ETC2 trailing bytes");
                Texture texture=new Texture.Builder().width(Math.max(1,w>>skip)).height(Math.max(1,h>>skip)).levels(levels-skip).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.ETC2_SRGB8).build(engine);
                try{
                    for(int level=0;level<payloads.size();level++){
                        byte[] bytes=payloads.get(level);ByteBuffer buffer=ByteBuffer.allocateDirect(bytes.length);buffer.put(bytes).flip();
                        texture.setImage(engine,level,new Texture.PixelBufferDescriptor(buffer,Texture.CompressedFormat.ETC2_SRGB8,bytes.length));textureBytes+=bytes.length;
                    }
                    textureUploadCpuNanos+=System.nanoTime()-started;return texture;
                }catch(RuntimeException error){engine.destroyTexture(texture);throw error;}
            }
        }
        textureFormat="SRGB8_A8 fallback";
        android.graphics.Bitmap bitmap;
        android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        try(java.io.InputStream in=context.getAssets().open(path)){android.graphics.BitmapFactory.decodeStream(in,null,options);}
        options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>quality.atlasSize)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        try(java.io.InputStream in=context.getAssets().open(path)){bitmap=android.graphics.BitmapFactory.decodeStream(in,null,options);}
        if(bitmap==null)throw new java.io.IOException("atlas missing: "+path);
        int levels=1,w=bitmap.getWidth(),h=bitmap.getHeight();
        for(int size=Math.max(w,h);size>1;size>>=1)levels++;
        Texture texture=new Texture.Builder().width(w).height(h).levels(levels).sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.SRGB8_A8).build(engine);
        try{com.google.android.filament.android.TextureHelper.setBitmap(engine,texture,0,bitmap);
        texture.generateMipmaps(engine);}catch(RuntimeException|OutOfMemoryError error){engine.destroyTexture(texture);throw error;}
        // TextureHelper's native upload retains the Bitmap until consumption; do not recycle early.
        for(int level=0;level<levels;level++)textureBytes+=(long)Math.max(1,w>>level)*Math.max(1,h>>level)*4;
        textureUploadCpuNanos+=System.nanoTime()-started;return texture;
    }
    private void thermalChanged(int status){
        if(released)return;thermalStatus=status;boolean before=thermal.constrained;thermal.update(status);
        if(before!=thermal.constrained){pacer.reset();resizeSurface();}
    }
    private void resizeSurface(){
        int w=getWidth(),h=getHeight();float scale=thermal.scale(quality);
        if(w>0&&h>0)surface.getHolder().setFixedSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);camera.width=Math.max(1,w);camera.height=Math.max(1,h);
        resizeSurface();
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return true;
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){multi=false;multiX=Float.NaN;panelGesture=blocked(e.getX(),e.getY());}
        for(int i=0;i<e.getPointerCount();i++)if(blocked(e.getX(i),e.getY(i)))panelGesture=true;
        if(panelGesture||action==MotionEvent.ACTION_CANCEL){
            suppressedGesture=e.getDownTime();MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
            gestures.onTouchEvent(cancel);scaler.onTouchEvent(cancel);cancel.recycle();
            if(editorDrawing&&editorStroke!=null)editorStroke.event(MotionEvent.ACTION_CANCEL,null);multiX=Float.NaN;return true;
        }
        if(e.getPointerCount()>1){if(!multi&&editorDrawing&&editorStroke!=null)editorStroke.event(MotionEvent.ACTION_CANCEL,null);multi=true;suppressedGesture=e.getDownTime();}
        if(e.getPointerCount()>1){
            float cx=(e.getX(0)+e.getX(1))*.5f,cy=(e.getY(0)+e.getY(1))*.5f;
            float angle=(float)Math.toDegrees(Math.atan2(e.getY(1)-e.getY(0),e.getX(1)-e.getX(0)));
            if(action==MotionEvent.ACTION_MOVE&&Float.isFinite(multiX)){
                if(e.getPointerCount()==2){camera.pan(cx-multiX,cy-multiY);camera.orbit(((angle-multiAngle+540)%360)-180,0,cx,cy,pickHeight(cx,cy));}
                else camera.orbit(0,(cy-multiY)*.12f,cx,cy,pickHeight(cx,cy));
                clampCamera();
            }
            multiX=cx;multiY=cy;multiAngle=angle;
            if(action==MotionEvent.ACTION_POINTER_UP)multiX=Float.NaN;
        }else multiX=Float.NaN;
        if(e.getPointerCount()<=2)scaler.onTouchEvent(e);
        else {MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);scaler.onTouchEvent(cancel);cancel.recycle();}
        if(editorDrawing&&editorStroke!=null&&!multi){
            Hex h=snapshot==null?null:snapshot.ground.surface.pick(camera,e.getX(),e.getY());
            editorStroke.event(action,h);return true;
        }
        gestures.onTouchEvent(e);return true;
    }
    private boolean blocked(float x,float y){
        if(x<0||y<0||x>=getWidth()-panelRight||y>=getHeight()-panelBottom)return true;
        // Global visible bounds include clipping by the host; system UI lies outside this view.
        android.graphics.Rect visible=new android.graphics.Rect();int[] location=new int[2];
        getLocationOnScreen(location);
        if(!getGlobalVisibleRect(visible)||!visible.contains((int)x+location[0],(int)y+location[1]))return true;
        android.view.WindowInsets insets=getRootWindowInsets();
        if(insets!=null){android.view.View root=getRootView();int[] rootAt=new int[2];root.getLocationOnScreen(rootAt);
            float rx=x+location[0]-rootAt[0],ry=y+location[1]-rootAt[1];
            if(rx<insets.getSystemWindowInsetLeft()||ry<insets.getSystemWindowInsetTop()||rx>=root.getWidth()-insets.getSystemWindowInsetRight()||ry>=root.getHeight()-insets.getSystemWindowInsetBottom())return true;
        }
        return false;
    }
    private float pickHeight(float x,float y){float h=snapshot==null?0:snapshot.ground.surface.rayHeight(camera,x,y);return Float.isFinite(h)?h:0;}
    private void zoomAt(float factor,float x,float y){camera.zoom(factor,x,y,pickHeight(x,y));clampCamera();}
    Hex pick(float sx,float sy,boolean objectsAllowed){
        if(snapshot==null)return null;
        float height=snapshot.ground.surface.rayHeight(camera,sx,sy);
        Hex ground=snapshot.ground.surface.pick(camera,sx,sy);
        lastPick=ground==null?"MISS":"cell="+ground+" world="+camera.worldX(sx,sy,height)+","+height+","+camera.worldZ(sx,sy,height)+" surface="+(snapshot.ground.surface.water(ground)?"water":"terrain");
        Hex object=objectsAllowed?pickObject(sx,sy):null;return object==null?ground:object;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    void snapshot(MapSceneSnapshot next){
        meshWork.owner();if(released)return;
        boolean groundChanged=snapshot==null||snapshot.ground!=next.ground;snapshot=next;
        Set<Hex> excluded=Vegetation.exclusions(next);boolean woodsChanged=groundChanged||!excluded.equals(woodExcluded);
        if(groundChanged||woodsChanged){
            outputVerified=false;outputStatus="WAITING_MESH";uniformOutputCount=0;
            woodExcluded=excluded;generation++;
            List<SceneMesh> previous=chunks,oldWoods=woods;pending=1;
            final FieldAssets assets=fieldAssets;
            terrainWindow=new SceneMesh.TerrainWindow(camera.x,camera.z,camera.extentX(),camera.extentZ(),camera.span);
            SceneMesh.TerrainWindow requested=terrainWindow;
            meshWork.submit(()->{
                long started=android.os.SystemClock.elapsedRealtime();

                SceneMesh scenery=SceneMesh.backdrop(next.ground);
                List<SceneMesh> built=SceneMesh.ground(next.ground,previous,requested);
                android.util.Log.i("Sanguo3D","Ground CPU ready chunks="+built.size()+" ms="+(android.os.SystemClock.elapsedRealtime()-started));
                List<SceneMesh> trees=Vegetation.buildWindow(next.ground,excluded,oldWoods,assets,requested);
                android.util.Log.i("Sanguo3D","Field CPU ready forestChunks="+trees.size()+" totalMs="+(android.os.SystemClock.elapsedRealtime()-started));
                return new MeshResult(scenery,built,trees);
            });
        }
        syncObjects();overlay.invalidate();schedule();
    }
    private void acceptMeshes(MeshResult result){
        if(released)return;
        if(backdropSource!=result.scenery){if(backdrop!=null){backdrop.destroy();backdrop=null;}backdropSource=result.scenery;}
        // Keep a displayed old level until its replacement finishes the bounded upload.
        for(SceneMesh old:new ArrayList<>(vegetation.keySet()))if(!result.trees.contains(old)&&result.trees.stream().noneMatch(m->m.distant==old))vegetation.remove(old).destroy();
        chunks=result.ground;distantTerrain=!chunks.isEmpty()&&chunks.stream().allMatch(m->m.terrainLod==2);
        woods=result.trees;pending=0;clampCamera();
    }
    void setTargets(Set<Hex> value){targets=value==null?Collections.emptySet():new HashSet<>(value);overlay.invalidate();}
    void setRoute(MarchOrders.Plan value){route=value;overlay.invalidate();}
    void replay(TurnJournal.Event e,float fraction){
        replay=e;replayFraction=CombatVisual.fraction(fraction);if(e==null)clearEffects();animateReplay();overlay.invalidate();schedule();
    }
    boolean visible(TurnJournal.Event e){if(visible(e.start)||visible(e.target))return true;for(Hex h:e.path)if(visible(h))return true;for(TurnJournal.Impact i:e.impacts)if(visible(i.hex))return true;return false;}
    private boolean visible(Hex h){if(h==null||snapshot==null)return false;GridWorldTransform g=snapshot.ground.grid;return Math.abs(camera.screenX(g.x(h),g.z(h))-camera.width/2f)<camera.width*.6&&Math.abs(camera.screenY(g.x(h),g.z(h),snapshot.ground.surface.at(h))-camera.height/2f)<camera.height*.6;}
    void diagnostics(boolean value){diagnostics=value;overlay.invalidate();}
    String startupReport(){return "source="+BuildConfig.SOURCE_REVISION+" profile="+(BuildConfig.UNITY_ENABLED?"unity-opt-in":"native")
        +" device="+android.os.Build.MODEL+" api="+android.os.Build.VERSION.SDK_INT+" abi="+java.util.Arrays.toString(android.os.Build.SUPPORTED_ABIS)
        +"\nsnapshot="+(snapshot!=null)+" surface="+(swap!=null)+" viewport="+bufferWidth+"x"+bufferHeight
        +" session="+(sceneToken==null?"editor":sceneToken.sessionId+":"+sceneToken.generation+":"+sceneToken.revision)+" assetRevision=1.56.0/R06"
        +" asset_pending="+assetWork.pending()+" asset_cpu_bytes="+assetWork.bytes()+" terrain_all_coarse="+distantTerrain+" mapVisualKey="+(snapshot==null?"none":snapshot.ground.mapSeed+":"+snapshot.ground.surface.overrides.hashCode())
        +" environmentCpuChunks="+woods.size()+" environmentMode=opaque-merged-not-instanced meshGeneration="+generation+" cpuChunks="+chunks.size()+" pending="+pending+" submitted="+surfaceFrames
        +" output="+outputStatus+"\ncamera="+camera.x+","+camera.z+" span="+camera.span+" tilt="+camera.tilt+" facing="+camera.facing+" yaw="+camera.yaw+"\npick="+lastPick;}
    String report(){return startupReport()+"\nFilament 1.56.0 / OpenGL ES · "+quality.label+" color="+(srgbSwapChain?"sRGB framebuffer":"post-process gamma")+" MSAA="+(msaaEnabled?"4x":"off / compatibility")+" thermal="+thermalStatus+" cap="+thermal.fps(quality)+"\n内部 "+bufferWidth+" × "+bufferHeight+" / UI "+camera.width+" × "+camera.height+" · chunks "+visibleChunks+" / GPU "+terrain.size()+" · objects "+visibleObjects+"\n帧回调间隔 "+String.format(java.util.Locale.ROOT,"%.1f",callbackMillis)+" ms（非 GPU/FPS 实测）\n待装载 "+pending+" · S06 战斗特效 / 部队 · 林块 "+visibleWood+" · LOD "+siteLod+" · 资产回退 "+missingAssets.size()+" · 特效 "+combat.count+"/"+CombatVisual.CAPACITY+"\n"+resourceReport();}
    void resetMetrics(){cpuCount=cpuCursor=0;}
    private String resourceReport(){
        int primitives=0,triangles=0;long bufferBytes=0;
        Set<GpuMesh> resident=new HashSet<>(shapes.values());resident.addAll(terrain.values());if(backdrop!=null)resident.add(backdrop);resident.addAll(vegetation.values());for(GpuMesh effect:effectMeshes)if(effect!=null)resident.add(effect);
        for(GpuMesh m:resident){bufferBytes+=(long)m.source.vertices.length*4+(long)m.source.indices.length*4+(m.source.uv==null?0:(long)m.source.uv.length*4)+(m.source.surfaceData==null?0:(long)m.source.surfaceData.length*4)+(m.source.tangents==null?0:(long)m.source.tangents.length*4);if(m.shown){primitives+=m.source.landIndexCount>0&&m.source.landIndexCount<m.source.indices.length?2:1;triangles+=m.source.indices.length/3;}}
        for(Proxy p:objects.values())if(p.shown){primitives++;triangles+=p.shape.source.indices.length/3;for(GpuMesh m:new GpuMesh[]{p.flagShape,p.baseShape,p.stateShape})if(m!=null){primitives++;triangles+=m.source.indices.length/3;}}
        for(int i=0;i<effectEntities.length;i++)if(effectShown[i]){primitives++;triangles+=effectMeshes[effectKinds[i]].source.indices.length/3;}
        int entities=resident.size()+(cameraEntity==0?0:1)+(light==0?0:1),instances=vegetationMaterial==null?0:1;
        for(Proxy p:objects.values()){entities+=1+(p.flag==0?0:1)+(p.base==0?0:1)+(p.state==0?0:1);if(p.instance!=null)instances++;}
        for(int entity:effectEntities)if(entity!=0)entities++;
        long[] times=Arrays.copyOf(cpuSamples,cpuCount);Arrays.sort(times);
        return "场景 primitives="+primitives+" triangles="+triangles+" buffers_bytes="+bufferBytes+" pose_cache="+shapes.size()+
            " entity_live="+entities+" material_instance_live="+instances+" mesh_live="+resident.size()+" texture_live="+(groundTextures.size()+(siteAtlas==null?0:1)+(fieldAtlas==null?0:1))+
            " material_live="+((material==null?0:1)+(groundMaterial==null?0:1)+(waterMaterial==null?0:1)+(siteMaterial==null?0:1))+
            " worker_pending="+meshWork.pending()+" worker_waiting="+meshWork.waiting()+" discarded="+meshWork.discarded()+
            " ground_load_cpu_ms="+groundLoadCpuNanos/1e6+" ground_texture_estimate_bytes="+groundTextureBytes+" ground_uploads="+groundTextures.size()+" ground_fragment_samples="+(quality==SceneQuality.LOW?8:12)+
            " frame_queued="+queued+" texture_estimate_bytes="+textureBytes+" "+textureFormat+" mip_upload_cpu_ms="+textureUploadCpuNanos/1e6+" CPU提交ms P50/P95/P99="+percentile(times,.50)+"/"+percentile(times,.95)+"/"+percentile(times,.99)+" samples="+cpuCount+"（非驱动DrawCall/GPU帧时）";
    }
    private static String percentile(long[] times,double p){return times.length==0?"N/A":String.format(java.util.Locale.ROOT,"%.2f",times[Math.min(times.length-1,(int)Math.ceil(times.length*p)-1)]/1e6);}
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
    void resume(boolean value){meshWork.owner();resumed=value;if(value)schedule();else {clearEffects();cancelFrame();}}
    private void cancelFrame(){Choreographer.getInstance().removeFrameCallback(this);queued=false;lastFrame=0;waterLastTick=0;pacer.reset();}
    private void schedule(){if(!released&&resumed&&swap!=null&&!queued){queued=true;Choreographer.getInstance().postFrameCallback(this);}}
    @Override public void surfaceCreated(SurfaceHolder holder){if(released||swap!=null)return;try{outputVerified=false;outputStatus="WAITING_FRAME";surfaceFrames=0;uniformOutputCount=0;lastOutputProbe=0;swap=engine.createSwapChain(holder.getSurface(),srgbSwapChain?SwapChainFlags.CONFIG_SRGB_COLORSPACE:SwapChainFlags.CONFIG_DEFAULT);displayHelper.attach(renderer,surface.getDisplay());schedule();}catch(RuntimeException|LinkageError e){failure.accept(e);}}
    @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int height){if(released)return;bufferWidth=w;bufferHeight=height;view.setViewport(new Viewport(0,0,w,height));com.google.android.filament.android.FilamentHelper.synchronizePendingFrames(engine);schedule();}
    @Override public void surfaceDestroyed(SurfaceHolder holder){cancelFrame();if(displayHelper!=null)displayHelper.detach();if(engine!=null&&swap!=null){engine.destroySwapChain(swap);swap=null;engine.flushAndWait();}}
    @Override public void doFrame(long time){
        queued=false;if(released||!resumed||swap==null)return;
        if(!pacer.due(time,thermal.fps(quality))){schedule();return;}
        long cpuStart=System.nanoTime();
        try{
            meshWork.drain(this::acceptMeshes,e->{pending=0;failure.accept(e);});
            if(released)return;
            boolean assetsChanged=assetWork.drain();assetUploadBudget=2;
            if(assetsChanged||assetSyncPending)syncObjects();
            if(lastFrame!=0)callbackMillis=(time-lastFrame)/1e6;lastFrame=time;
            double aspect=camera.width/(double)camera.height;
            lens.setProjection(Camera.Projection.ORTHO,-camera.span*aspect,camera.span*aspect,-camera.span,camera.span,.1,1000);
            lens.lookAt(camera.x+camera.backX()*300*camera.cos(),300*camera.sin(),camera.z+camera.rightX()*300*camera.cos(),camera.x,0,camera.z,0,1,0);
            if(waterLastTick!=0&&UiMotion.enabled())waterSeconds+=Math.min(.1,(time-waterLastTick)/1e9);
            waterLastTick=time;waterMaterial.getDefaultInstance().setParameter("waveTime",(float)(waterSeconds%4096));
            animationTick=time/1_000_000;animateReplay();loadVisible();animateUnits();animateEffects();
            if(bufferWidth>0&&bufferHeight>0&&renderer.beginFrame(swap,time)){renderer.render(view);renderer.endFrame();renderedFrames++;surfaceFrames++;if(surfaceFrames==1)android.util.Log.i("Sanguo3D","First submission (not visibility proof): "+startupReport());checkSurfaceOutput();}
            overlay.invalidate();schedule();
            cpuSamples[cpuCursor++%cpuSamples.length]=System.nanoTime()-cpuStart;cpuCount=Math.min(cpuSamples.length,cpuCount+1);
        }catch(RuntimeException|LinkageError|OutOfMemoryError e){cancelFrame();failure.accept(e);}
    }
    /** Check actual display output once uploads settle. Driver failures can return no Java error.
     * Only repeated, virtually identical extreme pixels trigger the existing safe 2D fallback. */
    private void checkSurfaceOutput(){
        long now=android.os.SystemClock.uptimeMillis();
        if(outputVerified||outputProbePending||surfaceFrames<20||pending!=0||visibleChunks==0
                ||now-lastOutputProbe<1000||!surface.getHolder().getSurface().isValid())return;
        lastOutputProbe=now;outputProbePending=true;
        SwapChain probedSwap=swap;int probedGeneration=generation;
        android.graphics.Bitmap sample=android.graphics.Bitmap.createBitmap(32,32,android.graphics.Bitmap.Config.ARGB_8888);
        try{
            PixelCopy.request(surface,sample,result->{
                outputProbePending=false;
                try{
                    if(released||!resumed||swap!=probedSwap||generation!=probedGeneration||pending!=0)return;
                    if(result!=PixelCopy.SUCCESS){outputStatus="COPY_ERROR_"+result;uniformOutputCount=0;return;}
                    int[] pixels=new int[1024];sample.getPixels(pixels,0,32,0,0,32,32);
                    int minR=255,minG=255,minB=255,maxR=0,maxG=0,maxB=0;
                    for(int pixel:pixels){int r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
                        minR=Math.min(minR,r);minG=Math.min(minG,g);minB=Math.min(minB,b);
                        maxR=Math.max(maxR,r);maxG=Math.max(maxG,g);maxB=Math.max(maxB,b);}
                    boolean extreme=maxR+maxG+maxB<=24||minR+minG+minB>=735;
                    boolean uniform=maxR-minR<=2&&maxG-minG<=2&&maxB-minB<=2;
                    if(uniform){
                        outputStatus="UNIFORM_SURFACE";
                        if(++uniformOutputCount==3)android.util.Log.w("Sanguo3D","Uniform output; visibility NOT verified: "+startupReport());
                        if(extreme&&uniformOutputCount>=3)failure.accept(new IllegalStateException("Repeated blank 3D Surface output"));
                    }else{uniformOutputCount=0;outputVerified=true;outputStatus="CONTENT_DETECTED_NOT_ART_ACCEPTANCE";android.util.Log.i("Sanguo3D",startupReport());}
                }finally{sample.recycle();}
            },new android.os.Handler(android.os.Looper.getMainLooper()));
        }catch(IllegalArgumentException e){outputProbePending=false;outputStatus="COPY_UNAVAILABLE";uniformOutputCount=0;sample.recycle();}
    }
    private boolean inView(float x,float z,float radius){return Math.abs(x-camera.x)<camera.extentX()+radius+2&&Math.abs(z-camera.z)<camera.extentZ()+radius+2;}
    private void loadVisible(){
        if(snapshot==null)return;
        if(backdrop==null&&backdropSource!=null){backdrop=new GpuMesh(backdropSource);backdrop.show(true);}
        engine.getLightManager().setShadowCaster(engine.getLightManager().getInstance(light),environmentShadows&&!thermal.constrained&&camera.span<22);
        int nextLod=Math.max(quality.minSiteLod,SiteVisual.lod(camera.span,siteLod));if(nextLod!=siteLod){siteLod=nextLod;syncObjects();}int budget=2;visibleChunks=0;pending=meshWork.pending();
        if(meshWork.pending()==0&&(terrainWindow==null||!terrainWindow.covers(camera.x,camera.z,camera.extentX(),camera.extentZ(),camera.span))){
            terrainWindow=new SceneMesh.TerrainWindow(camera.x,camera.z,camera.extentX(),camera.extentZ(),camera.span);
            SceneMesh.TerrainWindow requested=terrainWindow;MapSceneSnapshot.Ground ground=snapshot.ground;
            List<SceneMesh> previous=chunks,trees=woods;SceneMesh scenery=backdropSource;
            Set<Hex> excluded=woodExcluded;FieldAssets assets=fieldAssets;
            meshWork.submit(()->new MeshResult(scenery,SceneMesh.ground(ground,previous,requested),Vegetation.buildWindow(ground,excluded,trees,assets,requested)));
        }
        Set<SceneMesh> active=activeTerrain;active.clear();for(SceneMesh m:chunks)active.add(m);

        for(SceneMesh source:chunks){
            SceneMesh chunk=source;
            boolean shown=inView(chunk.x,chunk.z,chunk.radius);GpuMesh gpu=terrain.get(chunk);
            if(shown){visibleChunks++;if(gpu==null){if(budget-->0){gpu=new GpuMesh(chunk);terrain.put(chunk,gpu);}else pending++;}}
            if(gpu!=null){gpu.show(shown);if(!shown&&!inView(chunk.x,chunk.z,chunk.radius+20)){gpu.destroy();terrain.remove(chunk);}}
        }
        for(SceneMesh old:new ArrayList<>(terrain.keySet()))if(!active.contains(old)){
            SceneMesh replacement=null;for(SceneMesh m:chunks)if(m.chunkQ==old.chunkQ&&m.chunkR==old.chunkR){replacement=m;break;}
            if(replacement==null||terrain.containsKey(replacement)){terrain.remove(old).destroy();}
            else terrain.get(old).show(inView(old.x,old.z,old.radius));
        }
        int nextUnitLod=Math.max(quality.minUnitLod,camera.span<8?0:camera.span<22?1:2);
        unitLod=nextUnitLod;visibleWood=0;
        wantedWood.clear();
        for(SceneMesh source:woods){
            SceneMesh chunk=quality!=SceneQuality.LOW&&camera.span<14?source:source.distant;if(chunk.indices.length==0)continue;
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
        final MapSceneSnapshot.Ground assetGround=snapshot.ground;
        String key=field!=null?field+(FieldAssets.farm(item)?":"+item.hex+":"+FieldAssets.farmSurfaceKey(assetGround,item.hex):"")+(item.unit==null?"":":idle:0:"+FieldAssets.count(item.unit,item.unit.naval,unitLod)):item.site==null?item.kind+":"+item.color:item.site.model+":"+siteLod;
        GpuMesh mesh=shapes.get(key);if(mesh!=null)return mesh;
        final FieldAssets assets=fieldAssets;final int requestedLod=unitLod;final android.content.res.AssetManager manager=getContext().getAssets();
        SceneMesh source=assetWork.request(key,()->{
            if(field!=null){SceneMesh model=item.unit==null?assets.mesh(field):assets.pose(field,"idle",0,FieldAssets.count(item.unit,item.unit.naval,requestedLod));return FieldAssets.farm(item)?FieldAssets.conformFarm(model,assetGround,item.hex):model;}
            if(item.site!=null)try(java.io.InputStream in=manager.open("3d/sites/"+item.site.model+"-lod"+key.substring(key.lastIndexOf(':')+1)+".glb")){return SiteGlb.read(in);}
            return SceneMesh.proxy(item.kind,item.color);
        });
        if(source==null){
            String error=assetWork.error(key);if(error==null){assetSyncPending=true;return null;}
            if(missingAssets.add(key))android.util.Log.w("Sanguo3D","Asset fallback "+key+": "+error);
            source=SceneMesh.proxy(item.kind,0xffff00ff);
        }
        if(assetUploadBudget<=0){assetSyncPending=true;return null;}assetUploadBudget--;
        mesh=new GpuMesh(source);shapes.put(key,mesh);return mesh;
    }
    private void syncObjects(){
        if(engine==null||snapshot==null)return;assetSyncPending=false;Set<String> alive=new HashSet<>();
        for(MapSceneSnapshot.Item item:snapshot.items){
            alive.add(item.key);Proxy p=objects.get(item.key);GpuMesh geometry=shape(item);
            if(geometry==null)continue;
            if(p!=null&&((item.unit==null&&p.shape!=geometry)||p.item.color!=item.color||!p.stateKey().equals(item.facility==null?"":item.facility.burning?"fire":!item.facility.complete?"scaffold":""))){p.destroy();objects.remove(item.key);p=null;}
            if(item.facility!=null&&(!item.facility.complete||item.facility.burning)){
                String stateKey=item.facility.burning?"fire":"scaffold";
                if(!shapes.containsKey(stateKey)){
                    final FieldAssets assets=fieldAssets;
                    SceneMesh decoded=assetWork.request(stateKey,()->assets.mesh(stateKey));
                    if(decoded==null&&assetWork.error(stateKey)==null){assetSyncPending=true;continue;}
                    if(assetUploadBudget<=0){assetSyncPending=true;continue;}assetUploadBudget--;
                    if(decoded==null){missingAssets.add(stateKey);decoded=SceneMesh.proxy(item.kind,0xffff00ff);}
                    shapes.put(stateKey,new GpuMesh(decoded));
                }
            }
            if(p==null){p=new Proxy(item,geometry);objects.put(item.key,p);}
            p.item=item;p.motion.settle(item.hex,snapshot.ground.grid);p.position(p.motion.x,p.motion.z);p.updateDamage();
        }
        Iterator<Map.Entry<String,Proxy>> it=objects.entrySet().iterator();while(it.hasNext()){Map.Entry<String,Proxy> e=it.next();if(!alive.contains(e.getKey())){e.getValue().destroy();it.remove();}}
        trimShapes();
        animateReplay();
    }
    private void trimShapes(){
        if(shapes.size()<=quality.poseCache)return;
        Set<GpuMesh> used=new HashSet<>();for(Proxy p:objects.values()){used.add(p.shape);if(p.flagShape!=null)used.add(p.flagShape);if(p.baseShape!=null)used.add(p.baseShape);if(p.stateShape!=null)used.add(p.stateShape);}
        Iterator<Map.Entry<String,GpuMesh>> it=shapes.entrySet().iterator();
        while(it.hasNext()&&shapes.size()>quality.poseCache){GpuMesh m=it.next().getValue();if(!used.contains(m)&&m.references==0){m.destroy();it.remove();}}
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
                if(mesh==null){
                    final FieldAssets assets=fieldAssets;final String clip=p.animation.clip;final int frame=p.animation.frame;
                    SceneMesh decoded=assetWork.request(key,()->assets.pose(model,clip,frame,count));
                    if(decoded==null){String error=assetWork.error(key);if(error!=null&&missingAssets.add(key))android.util.Log.w("Sanguo3D","Pose fallback "+key+": "+error);continue;}
                    if(assetUploadBudget<=0)continue;assetUploadBudget--;
                    mesh=new GpuMesh(decoded);shapes.put(key,mesh);
                }
                p.replace(mesh);p.poseKey=key;
            }
            p.position(p.motion.x,p.motion.z);
        }
        trimShapes();
    }
    private void animateReplay(){
        if(snapshot==null)return;
        TurnJournal.Strike strike=CombatVisual.strike(replay,replayFraction);
        Proxy current=replay==null?null:objects.get("unit:"+(strike==null?replay.actorId:strike.actorId));
        if(animatedUnit!=null&&animatedUnit!=current&&objects.get(animatedUnit.item.key)==animatedUnit){
            animatedUnit.motion.sample(null,0,snapshot.ground.grid);
            animatedUnit.position(animatedUnit.motion.x,animatedUnit.motion.z);
        }
        animatedUnit=current;
        if(current!=null){
            if(strike==null)current.motion.sample(replay,replayFraction,snapshot.ground.grid);else current.motion.strike(strike,CombatVisual.phase(replay,replayFraction),snapshot.ground.grid);
            // Keep the cheap CPU pose for culling/hit tests, but avoid off-screen GPU updates.
            if(inView(current.motion.x,current.motion.z,2))current.position(current.motion.x,current.motion.z);
        }
    }

    void critical(CriticalHit hit,float phase,android.graphics.drawable.Drawable portrait){
        criticalPortrait=portrait;
        criticalHit=hit;criticalPhase=phase;overlay.invalidate();
    }
    void criticalSkip(Runnable skip){criticalSkip=skip;}
    private void clearEffects(){
        combat.count=0;if(scene==null)return;
        for(int i=0;i<effectEntities.length;i++)if(effectShown[i]){scene.removeEntity(effectEntities[i]);effectShown[i]=false;}
    }
    private void animateEffects(){
        if(snapshot==null)return;
        combat.sample(UiMotion.enabled()?replay:null,replayFraction,snapshot.ground);
        int fires=0;for(MapSceneSnapshot.FireState fire:snapshot.fires)if(visible(fire.hex)){
            if(fires++==CombatVisual.FIRE_BUDGET)break;
            combat.fire(fire,snapshot.ground,animationTick,UiMotion.enabled());
        }
        for(int i=0;i<effectEntities.length;i++){
            if(i>=combat.count){if(effectShown[i]){scene.removeEntity(effectEntities[i]);effectShown[i]=false;}continue;}
            CombatVisual.Particle p=combat.particles[i];
            if(!inView(p.x,p.z,1)){if(effectShown[i]){scene.removeEntity(effectEntities[i]);effectShown[i]=false;}continue;}
            if(effectMeshes[p.mesh]==null)effectMeshes[p.mesh]=new GpuMesh(CombatVisual.mesh(p.mesh));
            if(effectEntities[i]==0){effectEntities[i]=EntityManager.get().create();effectKinds[i]=-1;}
            if(effectKinds[i]!=p.mesh){engine.getRenderableManager().destroy(effectEntities[i]);effectMeshes[p.mesh].build(effectEntities[i]);effectKinds[i]=p.mesh;}
            float c=(float)Math.cos(p.yaw)*p.scale,s=(float)Math.sin(p.yaw)*p.scale;
            Arrays.fill(effectMatrix,0);effectMatrix[0]=c;effectMatrix[2]=-s;effectMatrix[5]=p.scale;effectMatrix[8]=s;effectMatrix[10]=c;effectMatrix[12]=p.x;effectMatrix[13]=p.y;effectMatrix[14]=p.z;effectMatrix[15]=1;
            TransformManager tm=engine.getTransformManager();tm.setTransform(tm.getInstance(effectEntities[i]),effectMatrix);
            if(!effectShown[i]){scene.addEntity(effectEntities[i]);effectShown[i]=true;}
        }
    }

    private Hex pickObject(float sx,float sy){
        for(Map.Entry<String,android.graphics.RectF> label:overlay.labelHits.entrySet()){
            Proxy p=objects.get(label.getKey());if(p!=null&&p.shown&&label.getValue().contains(sx,sy)&&labelVisible(p)){lastPick="label="+p.item.key+" cell="+p.item.hex;return p.item.hex;}
        }
        Proxy chosen=null;float best=Float.NEGATIVE_INFINITY;float foreground=snapshot.ground.surface.rayHeight(camera,sx,sy);
        for(Proxy p:objects.values())if(p.shown){
            MapSceneSnapshot.Item item=p.item;
            float yaw=item.site!=null?item.site.yaw:item.facility!=null?item.facility.direction*(float)Math.PI/3:p.motion.yaw;
            float scale=item.site!=null?item.site.scale:item.unit!=null?p.animation.scale:1;
            float hit=ScenePicking.hit(camera,p.shape.source,p.motion.x,p.y,p.motion.z,yaw,scale,sx,sy);
            if(!Float.isFinite(hit))continue;
            if(Float.isFinite(foreground)&&foreground>hit+.03f)continue;
            if(hit>best||(hit==best&&chosen!=null&&item.key.compareTo(chosen.item.key)<0)){best=hit;chosen=p;}
        }
        if(chosen!=null)lastPick="entity="+chosen.item.key+" cell="+chosen.item.hex+" height="+best+" display="+chosen.motion.x+","+chosen.motion.z;
        return chosen==null?null:chosen.item.hex;
    }
    void focus(Hex h){if(snapshot==null||h==null)return;camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);camera.span=10;}
    void center(Hex h){if(snapshot!=null&&h!=null){camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);}}
    void fit(){if(snapshot==null)return;MapSceneSnapshot.Ground g=snapshot.ground;float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){Hex h=new Hex(q,r);if(g.valid(h)){float x=g.grid.x(h),z=g.grid.z(h);minX=Math.min(minX,x);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxZ=Math.max(maxZ,z);}}if(minX==Float.MAX_VALUE)return;camera.x=(minX+maxX)/2;camera.z=(minZ+maxZ)/2;float dx=maxX-minX+3,dz=maxZ-minZ+3;camera.span=(float)Math.max((dx*Math.abs(camera.rightX())+dz*Math.abs(camera.backX()))*camera.height/camera.width,(dx*Math.abs(camera.backX())+dz*Math.abs(camera.rightX()))*camera.sin())*.52f;camera.sanitize();}
    void resetOrientation(){camera.facing=1;camera.yaw=0;camera.tilt=55;clampCamera();overlay.invalidate();}
    void reverseOrientation(){camera.facing=-camera.facing;overlay.invalidate();}
    private void clampCamera(){
        camera.sanitize();if(snapshot!=null)camera.clampTo(snapshot.ground);
    }
    void saveCamera(Bundle b){b.putFloat("cameraX",camera.x*TileGeometry.DX);b.putFloat("cameraY",camera.z*TileGeometry.DY);b.putFloat("sceneSpan",camera.span);b.putFloat("sceneTilt",camera.tilt);b.putInt("sceneFacing",camera.facing);b.putFloat("sceneYaw",camera.yaw);}
    void restoreCamera(Bundle b){try{camera.x=b.getFloat("cameraX")/TileGeometry.DX;camera.z=b.getFloat("cameraY")/TileGeometry.DY;camera.span=b.getFloat("sceneSpan",15);camera.tilt=b.getFloat("sceneTilt",55);camera.yaw=b.getFloat("sceneYaw",0);camera.facing=b.getInt("sceneFacing",1)<0?-1:1;camera.sanitize();clampCamera();}catch(RuntimeException bad){camera.x=0;camera.z=0;camera.span=15;camera.tilt=55;camera.yaw=0;camera.facing=1;clampCamera();}}
    void release(){
        meshWork.owner();
        if(released)return;released=true;replay=null;animatedUnit=null;generation++;cancelFrame();meshWork.close();assetWork.close();pending=0;surface.getHolder().removeCallback(this);
        // A detached View may remain referenced by the framework or an outstanding probe.
        // Release heavyweight CPU ownership immediately, rather than waiting for View GC.
        chunks=Collections.emptyList();woods=Collections.emptyList();snapshot=null;fieldAssets=null;backdropSource=null;
        activeTerrain.clear();wantedWood.clear();woodExcluded=Collections.emptySet();
        territoryColors=null;targets=Collections.emptySet();editorCells=Collections.emptySet();impassable=Collections.emptySet();
        if(android.os.Build.VERSION.SDK_INT>=29&&thermalManager!=null&&thermalListener!=null){thermalManager.removeThermalStatusListener(thermalListener);thermalListener=null;}
        if(engine==null)return;
        if(displayHelper!=null)displayHelper.detach();
        if(swap!=null){engine.destroySwapChain(swap);swap=null;}
        for(Proxy p:objects.values())p.destroy();objects.clear();for(GpuMesh m:terrain.values())m.destroy();terrain.clear();for(GpuMesh m:vegetation.values())m.destroy();vegetation.clear();for(GpuMesh m:shapes.values())m.destroy();shapes.clear();
        if(backdrop!=null){backdrop.destroy();backdrop=null;}
        clearEffects();for(int entity:effectEntities)if(entity!=0){engine.destroyEntity(entity);EntityManager.get().destroy(entity);}
        for(GpuMesh mesh:effectMeshes)if(mesh!=null)mesh.destroy();criticalHit=null;criticalPortrait=null;criticalSkip=null;
        if(skyLight!=null){scene.setIndirectLight(null);engine.destroyIndirectLight(skyLight);skyLight=null;}
        if(light!=0){scene.removeEntity(light);engine.destroyEntity(light);EntityManager.get().destroy(light);}
        if(vegetationMaterial!=null)engine.destroyMaterialInstance(vegetationMaterial);if(fieldAtlas!=null)engine.destroyTexture(fieldAtlas);if(siteMaterial!=null)engine.destroyMaterial(siteMaterial);if(siteAtlas!=null)engine.destroyTexture(siteAtlas);
        if(waterMaterial!=null)engine.destroyMaterial(waterMaterial);
        if(groundMaterial!=null)engine.destroyMaterial(groundMaterial);
        for(Texture texture:groundTextures)engine.destroyTexture(texture);groundTextures.clear();
        if(material!=null)engine.destroyMaterial(material);
        if(skybox!=null){scene.setSkybox(null);engine.destroySkybox(skybox);}
        if(view!=null)engine.destroyView(view);if(scene!=null)engine.destroyScene(scene);if(renderer!=null)engine.destroyRenderer(renderer);
        if(cameraEntity!=0){engine.destroyCameraComponent(cameraEntity);EntityManager.get().destroy(cameraEntity);}engine.flushAndWait();engine.destroy();engine=null;
        renderer=null;scene=null;view=null;lens=null;skybox=null;displayHelper=null;
        material=null;waterMaterial=null;groundMaterial=null;siteMaterial=null;vegetationMaterial=null;
        siteAtlas=null;fieldAtlas=null;textureBytes=0;cameraEntity=light=0;
        Arrays.fill(effectMeshes,null);Arrays.fill(effectEntities,0);
    }
    private final class GpuMesh {
        VertexBuffer vb;IndexBuffer ib;int entity;int references;final SceneMesh source;boolean shown;
        GpuMesh(SceneMesh m){source=m;try{
            VertexBuffer.Builder builder=new VertexBuffer.Builder().vertexCount(m.vertices.length/7).bufferCount(m.tangents!=null?3:m.surfaceData!=null?2:m.uv==null?1:2).attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,28).attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,12,28);
            if(m.surfaceData!=null)builder.attribute(VertexBuffer.VertexAttribute.UV0,1,VertexBuffer.AttributeType.FLOAT2,0,32).attribute(VertexBuffer.VertexAttribute.TANGENTS,1,VertexBuffer.AttributeType.FLOAT4,8,32).attribute(VertexBuffer.VertexAttribute.UV1,1,VertexBuffer.AttributeType.FLOAT2,24,32);
            if(m.tangents!=null)builder.attribute(VertexBuffer.VertexAttribute.TANGENTS,2,VertexBuffer.AttributeType.FLOAT4,0,16);
            if(m.uv!=null)builder.attribute(VertexBuffer.VertexAttribute.UV0,1,VertexBuffer.AttributeType.FLOAT2,0,8);vb=builder.build(engine);
            if(m.surfaceData!=null){FloatBuffer data=ByteBuffer.allocateDirect(m.surfaceData.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();data.put(m.surfaceData).flip();vb.setBufferAt(engine,1,data);}
            if(m.uv!=null){FloatBuffer uv=ByteBuffer.allocateDirect(m.uv.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();uv.put(m.uv).flip();vb.setBufferAt(engine,1,uv);}
            if(m.tangents!=null){FloatBuffer t=ByteBuffer.allocateDirect(m.tangents.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();t.put(m.tangents).flip();vb.setBufferAt(engine,2,t);}
            FloatBuffer v=ByteBuffer.allocateDirect(m.vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();v.put(m.vertices).flip();vb.setBufferAt(engine,0,v);
            ib=new IndexBuffer.Builder().indexCount(m.indices.length).bufferType(IndexBuffer.Builder.IndexType.UINT).build(engine);IntBuffer i=ByteBuffer.allocateDirect(m.indices.length*4).order(ByteOrder.nativeOrder()).asIntBuffer();i.put(m.indices).flip();ib.setBuffer(engine,i);
            entity=EntityManager.get().create();if(m.uv==null)build(entity);
        }catch(RuntimeException|LinkageError|OutOfMemoryError error){destroy();throw error;}}
        void build(int target){
            if(source.surfaceData==null||source.landIndexCount<0){build(target,material.getDefaultInstance());return;}
            int land=source.landIndexCount,water=source.indices.length-land;
            RenderableManager.Builder b=new RenderableManager.Builder(land>0&&water>0?2:1)
                .boundingBox(new Box(source.x,1.3f,source.z,source.radius,2.7f,source.radius)).castShadows(false).receiveShadows(environmentShadows);
            int slot=0;
            if(land>0)b.material(slot,groundMaterial.getDefaultInstance()).geometry(slot++,RenderableManager.PrimitiveType.TRIANGLES,vb,ib,0,land);
            if(water>0)b.material(slot,waterMaterial.getDefaultInstance()).geometry(slot,RenderableManager.PrimitiveType.TRIANGLES,vb,ib,land,water);
            b.build(engine,target);
        }
        void build(int target,MaterialInstance instance){new RenderableManager.Builder(1).boundingBox(new Box(source.x,1.3f,source.z,source.radius,2.7f,source.radius)).material(0,instance).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vb,ib).castShadows(environmentShadows&&source.uv!=null&&(!source.vegetation||quality==SceneQuality.HIGH)).receiveShadows(environmentShadows&&source.uv!=null).build(engine,target);}
        void show(boolean value){if(shown==value)return;shown=value;if(value)scene.addEntity(entity);else scene.removeEntity(entity);}
        void destroy(){if(entity!=0){scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);entity=0;}if(vb!=null){engine.destroyVertexBuffer(vb);vb=null;}if(ib!=null){engine.destroyIndexBuffer(ib);ib=null;}}
    }
    private final class Proxy {
        final int entity; GpuMesh shape; int flag,base,state; GpuMesh flagShape,baseShape,stateShape; MaterialInstance instance;
        MapSceneSnapshot.Item item;boolean shown;final UnitMotion motion=new UnitMotion();final UnitAnimation animation=new UnitAnimation();String poseKey;float y;
        Proxy(MapSceneSnapshot.Item item,GpuMesh shape){this.item=item;motion.settle(item.hex,snapshot.ground.grid);this.shape=shape;shape.references++;entity=EntityManager.get().create();try{
            if(shape.source.uv!=null){instance=siteMaterial.createInstance();instance.setParameter("atlas",item.site!=null?siteAtlas:fieldAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));instance.setParameter("damage",0f);shape.build(entity,instance);}else shape.build(entity);
            if(item.site!=null||item.unit!=null||item.facility!=null){String key="flag:"+item.color;flagShape=shapes.get(key);if(flagShape==null){flagShape=new GpuMesh(SceneMesh.proxy(3,item.color));shapes.put(key,flagShape);}flagShape.references++;flag=EntityManager.get().create();flagShape.build(flag);}
            // Seven-cell occupancy is shown by the selection overlay; ground blends in the terrain field.
            if(item.facility!=null&&(!item.facility.complete||item.facility.burning)){
                String stateKey=item.facility.burning?"fire":"scaffold";stateShape=shapes.get(stateKey);
                if(stateShape==null)throw new IllegalStateException("state decode not ready");
                stateShape.references++;
                state=EntityManager.get().create();if(stateShape.source.uv!=null)stateShape.build(state,vegetationMaterial);else stateShape.build(state);
            }
            position(snapshot.ground.grid.x(item.hex),snapshot.ground.grid.z(item.hex));updateDamage();
        }catch(RuntimeException|LinkageError|OutOfMemoryError error){destroy();throw error;}}
        void replace(GpuMesh mesh){
            engine.getRenderableManager().destroy(entity);shape.references--;shape=mesh;shape.references++;if(instance==null)shape.build(entity);else shape.build(entity,instance);if(shown)scene.addEntity(entity);
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
        void destroy(){shape.references--;if(flagShape!=null)flagShape.references--;if(stateShape!=null)stateShape.references--;if(baseShape!=null)baseShape.references--;scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);if(flag!=0){scene.removeEntity(flag);engine.destroyEntity(flag);EntityManager.get().destroy(flag);}if(base!=0){scene.removeEntity(base);engine.destroyEntity(base);EntityManager.get().destroy(base);}if(state!=0){scene.removeEntity(state);engine.destroyEntity(state);EntityManager.get().destroy(state);}if(instance!=null)engine.destroyMaterialInstance(instance);}
    }
    private boolean labelVisible(Proxy object){
        float wx=object.motion.x,wz=object.motion.z,y=object.y+1;
        float sx=camera.screenX(wx,wz),sy=camera.screenY(wx,wz,y);
        float foreground=snapshot.ground.surface.rayHeight(camera,sx,sy);
        return !Float.isFinite(foreground)||foreground<=y+.001f;
    }
    private boolean selected(MapSceneSnapshot.Item item){return item.hex.equals(snapshot.selected)||item.site!=null&&item.site.cells.contains(snapshot.selected);}
    private final class Overlay extends android.view.View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Map<String,android.graphics.RectF> labelHits=new LinkedHashMap<>();
        final android.graphics.Path cellPath=new android.graphics.Path();
        Overlay(Context c){super(c);setClickable(false);}
        boolean cellPath(Hex h){
            if(h==null||snapshot==null||!snapshot.ground.valid(h))return false;GridWorldTransform g=snapshot.ground.grid;float x=g.x(h),z=g.z(h);
            android.graphics.Path path=cellPath;path.rewind();int i=0;
            for(float[] edge:SceneMesh.EDGE){float wx=x+edge[0],wz=z+edge[1],sx=camera.screenX(wx,wz),sy=camera.screenY(wx,wz,snapshot.ground.surface.sample(wx,wz)+.015f);if(i++==0)path.moveTo(sx,sy);else path.lineTo(sx,sy);}
            path.close();return true;
        }
        void cell(Canvas c,Hex h,int color){
            if(!cellPath(h))return;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(cellPath,p);
        }
        void drawCombat(Canvas c){
            float d=getResources().getDisplayMetrics().density;
            TurnJournal.Strike strike=CombatVisual.strike(replay,replayFraction);
            float phase=CombatVisual.phase(replay,replayFraction);
            if(strike!=null&&phase>=CombatVisual.FEEDBACK&&visible(strike.target)&&strike.beforeTroops>strike.afterTroops){
                p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(15*d);p.setColor(0xffffbb90);
                c.drawText("−"+(strike.beforeTroops-strike.afterTroops)+"兵",camera.screenX(snapshot.ground.grid.x(strike.target),snapshot.ground.grid.z(strike.target)),camera.screenY(snapshot.ground.grid.x(strike.target),snapshot.ground.grid.z(strike.target),snapshot.ground.surface.at(strike.target)+1)-(phase-CombatVisual.FEEDBACK)*50*d,p);
                p.setTextAlign(Paint.Align.LEFT);
            }
            if(replay!=null&&replayFraction>=CombatVisual.FEEDBACK&&(strike==null||strike==replay.strikes.get(replay.strikes.size()-1))){
                int count=0;float progress=(replayFraction-CombatVisual.FEEDBACK)/(1-CombatVisual.FEEDBACK);
                p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(15*d);
                for(TurnJournal.Impact hit:replay.impacts){
                    if(!visible(hit.hex)||(strike!=null&&hit.text.endsWith("兵")))continue;if(count++>=CombatVisual.TEXT_BUDGET)break;
                    float x=camera.screenX(snapshot.ground.grid.x(hit.hex),snapshot.ground.grid.z(hit.hex));
                    float y=camera.screenY(snapshot.ground.grid.x(hit.hex),snapshot.ground.grid.z(hit.hex),snapshot.ground.surface.at(hit.hex)+.9f)-progress*28*d;
                    // Different lines at the same tile stay readable (troops, morale, status).
                    int line=0;for(int j=0;j<replay.impacts.indexOf(hit);j++)if(replay.impacts.get(j).hex.equals(hit.hex))line++;
                    p.setColor(hit.loss?0xffffbb90:0xffa5eed0);c.drawText(hit.text,x,y-line*17*d,p);
                }
                p.setTextAlign(Paint.Align.LEFT);
            }
            if(criticalHit!=null&&criticalPortrait!=null){
                float width=Math.min(camera.width-24*d,320*d),height=92*d,left=(camera.width-width)/2,top=42*d;
                p.setStyle(Paint.Style.FILL);p.setColor(0xed14242c);c.drawRoundRect(left,top,left+width,top+height,8*d,8*d,p);
                criticalPortrait.setBounds((int)left,(int)top,(int)(left+height),(int)(top+height));criticalPortrait.draw(c);
                p.setColor(0xffffdf9e);p.setTextSize(18*d);c.drawText(criticalHit.name+" · 暴击",left+height+8*d,top+30*d,p);
                p.setTextSize(13*d);c.drawText(criticalHit.tactic,left+height+8*d,top+54*d,p);
                p.setTextSize(11*d);c.drawText("点击跳过",left+height+8*d,top+77*d,p);
            }
        }
        @Override protected void onDraw(Canvas c){
            if(snapshot==null)return;
            if(territoryColors!=null||((gridShown||editorGrid)&&camera.span<48)||editorCoords||!impassable.isEmpty()){
                GridWorldTransform grid=snapshot.ground.grid;
                float rx=camera.extentX()+2,rz=camera.extentZ()+4;
                Hex a=grid.cell(camera.x-rx,camera.z-rz),b=grid.cell(camera.x+rx,camera.z+rz),d=grid.cell(camera.x-rx,camera.z+rz),e=grid.cell(camera.x+rx,camera.z-rz);
                int q0=Math.max(0,Math.min(Math.min(a.q,b.q),Math.min(d.q,e.q))-2),q1=Math.min(snapshot.ground.width-1,Math.max(Math.max(a.q,b.q),Math.max(d.q,e.q))+2);
                int r0=Math.max(0,Math.min(Math.min(a.r,b.r),Math.min(d.r,e.r))-2),r1=Math.min(snapshot.ground.height-1,Math.max(Math.max(a.r,b.r),Math.max(d.r,e.r))+2);
                for(int r=r0;r<=r1;r++)for(int q=q0;q<=q1;q++){
                    Hex h=new Hex(q,r);if(!snapshot.ground.valid(h))continue;
                    if(territoryColors!=null){int color=territoryColors[r*snapshot.ground.width+q];if(color!=0&&cellPath(h)){p.setColor((color&0xffffff)|0x55000000);p.setStyle(Paint.Style.FILL);c.drawPath(cellPath,p);}}
                    if((gridShown||editorGrid)&&camera.span<48)cell(c,h,editorGrid?0x99ffffff:0x887d928a);
                    if(impassable.contains(MapLayerData.cellKey(h.q,h.r)))cell(c,h,0x99ff6767);
                    if(editorCoords&&camera.span<7){p.setStyle(Paint.Style.FILL);p.setColor(0xffffffff);p.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);c.drawText((int)Math.floor(grid.x(h))+","+(int)Math.floor(grid.z(h)),camera.screenX(grid.x(h),grid.z(h)),camera.screenY(grid.x(h),grid.z(h),snapshot.ground.surface.at(h)),p);}
                }
            }
            if(editorFootprints)for(MapSceneSnapshot.Item item:snapshot.items)if(item.site!=null)for(Hex h:item.site.cells)cell(c,h,0xff89e5ff);
            for(Hex h:editorCells)cell(c,h,editorValid?0xff70ffca:0xffff6767);
            if(tacticPreview!=null){
                for(Hex h:tacticPreview.actorPath)cell(c,h,0xff70ffca);
                for(Hex h:tacticPreview.targetPath)cell(c,h,0xffffb261);
                for(Hex h:tacticPreview.riskHexes)cell(c,h,0xffff6767);
                cell(c,tacticPreview.blocked,0xffff3333);
            }
            if(route!=null)for(Hex h:route.path)cell(c,h,0xffffd576);
            for(Hex h:snapshot.reachable)cell(c,h,0x884ed7c2);for(Hex h:snapshot.coverage)cell(c,h,0xffcfad6e);for(Hex h:snapshot.siege)cell(c,h,0x9975a8fa);for(Hex h:targets)cell(c,h,0xffdd7661);cell(c,snapshot.selected,0xffffd576);
            for(MapSceneSnapshot.Item item:snapshot.items)if(item.site!=null&&item.site.cells.contains(snapshot.selected))for(Hex h:item.site.cells)cell(c,h,0xffffd576);
            // Ground rings remain visible through architecture; transit units cannot disappear behind walls.
            for(Proxy object:objects.values())if(object.item.unit!=null&&object.shown){
                float x=camera.screenX(object.motion.x,object.motion.z),y=camera.screenY(object.motion.x,object.motion.z,object.y);
                float radius=Math.max(3,camera.height/(2*camera.span)*.38f);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
                p.setColor(selected(object.item)?0xffffd576:(object.item.color&0xffffff)|0x88000000);
                c.drawOval(x-radius,y-radius*(float)camera.sin(),x+radius,y+radius*(float)camera.sin(),p);
            }
            p.setStyle(Paint.Style.FILL);p.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);p.setShadowLayer(2,0,1,0xff000000);
            List<Proxy> labels=new ArrayList<>();
            labelHits.clear();
            for(Proxy object:objects.values())if(object.shown&&(object.item.kind<3||camera.span<18||selected(object.item)))labels.add(object);
            labels.sort(Comparator.<Proxy>comparingInt(o->selected(o.item)?0:o.item.site!=null?1:2)
                .thenComparingDouble(o->Math.abs(o.motion.x-camera.x)+Math.abs(o.motion.z-camera.z)).thenComparing(o->o.item.key));
            Set<Integer> namedFactions=new HashSet<>();
            List<android.graphics.RectF> occupied=new ArrayList<>();float font=p.getTextSize(),pad=font*.22f;
            for(Proxy object:labels){
                MapSceneSnapshot.Item item=object.item;boolean selected=selected(item);
                String first=item.displayLabel(),second=null;
                if(openingPreview&&item.site!=null){int owner=siteOwners.getOrDefault(item.key,-1);if(owner<0||namedFactions.contains(owner))continue;first=factionLabels.getOrDefault(item.key,"");selected=owner==previewFaction;}

                if(item.unit!=null){if(!selected&&!showCommanders&&!showUnitBars)continue;UnitVisual u=item.unit;
                    int shownTroops=u.troops;
                    if(replay!=null){int active=Math.min(replay.strikes.size()-1,(int)(CombatVisual.fraction(replayFraction)*replay.strikes.size()));
                        for(int k=0;k<replay.strikes.size();k++){TurnJournal.Strike hit=replay.strikes.get(k);if(hit.targetId==u.id&&(k<active||k==active&&CombatVisual.phase(replay,replayFraction)>=CombatVisual.HIT))shownTroops=hit.afterTroops;}}
                    first=selected?u.commander+" · "+u.equipment:(showCommanders?u.commander:u.equipment)+(showUnitBars?" · "+shownTroops:"");
                    if(selected)second=shownTroops+"兵 · 气"+u.energy+(u.status==War.Status.NORMAL?"":" · "+u.status.label)+(u.burning>0?" · 起火":"");
                }else if(item.facility!=null&&!selected){MapSceneSnapshot.FacilityState f=item.facility;first=item.label+(f.level>0?" Lv"+f.level:"")+(f.burning?" · 火":!f.complete?" · 建":"");}
                float width=p.measureText(first);if(second!=null)width=Math.max(width,p.measureText(second));
                float x=camera.screenX(object.motion.x,object.motion.z)-width/2,y=camera.screenY(object.motion.x,object.motion.z,object.y+1);
                x=Math.max(pad,Math.min(camera.width-width-pad,x));
                android.graphics.RectF box=new android.graphics.RectF(x-pad,y-font-pad,x+width+pad,y+(second==null?pad:font*1.2f+pad));
                if(box.bottom<font*2||box.top>camera.height||box.right<0||box.left>camera.width)continue;
                boolean overlap=false;for(android.graphics.RectF used:occupied)if(android.graphics.RectF.intersects(used,box)){overlap=true;break;}
                if((overlap&&!selected)||!labelVisible(object))continue;occupied.add(box);labelHits.put(item.key,box);if(openingPreview&&item.site!=null)namedFactions.add(siteOwners.getOrDefault(item.key,-1));
                p.setColor(selected?0xe61b2f37:0xb3122027);c.drawRoundRect(box,pad,pad,p);
                p.setColor(selected?0xffffd576:item.color);c.drawText(first,x,y,p);if(second!=null)c.drawText(second,x,y+font*1.2f,p);
            }
            p.setColor(0xfff0e5c8);c.drawText((pending>0)?"3D 地形装载中… · 可在视图切回 2D":"双指缩放/旋转 · 三指倾角 · 长按定位",12,24*getResources().getDisplayMetrics().density,p);
            if(diagnostics){float y=48*getResources().getDisplayMetrics().density;for(String line:report().split("\n")){c.drawText(line,12,y,p);y+=22*getResources().getDisplayMetrics().density;}}
            drawCombat(c);p.clearShadowLayer();
        }
    }
}
