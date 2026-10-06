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
    private final SceneFrameMetrics frameMetrics=new SceneFrameMetrics();
    private final SceneVisibilityStamp visibilityStamp=new SceneVisibilityStamp();
    private final SceneVisibilityStamp objectVisibilityStamp=new SceneVisibilityStamp();
    private boolean terrainVisibilityDirty=true;
    private long terrainVisibilityPasses,terrainVisibilitySkips;
    private static final long MESH_UPLOAD_BUDGET_NANOS=2_000_000L;
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
    private final MapHost.TileListener listener;
    private final SceneAssetQueue assetWork=new SceneAssetQueue();
    private final SceneWorkQueue<MeshResult> meshWork=new SceneWorkQueue<>();
    private static final class MeshResult {
        final SceneMesh scenery; final List<SceneMesh> ground,trees;
        final TerrainSurface surface;
        final long completedNanos=System.nanoTime();
        MeshResult(SceneMesh scenery,List<SceneMesh> ground,List<SceneMesh> trees,TerrainSurface surface){this.scenery=scenery;this.ground=ground;this.trees=trees;this.surface=surface;}
    }
    private game.sanguo.api.StateToken sceneToken;
    void sceneIdentity(game.sanguo.api.StateToken token){
        meshWork.owner();
        if(sceneToken!=null&&(!sceneToken.sessionId.equals(token.sessionId)||sceneToken.generation!=token.generation)){
            closePcMapEffects();closePcPresentations();
            cancelUnitDrag();
            visibilityStamp.invalidate();terrainVisibilityDirty=true;meshWork.invalidate();assetWork.invalidate();generation++;pending=0;replay=null;animatedUnit=null;clearEffects();
            for(Proxy p:objects.values())p.destroy();objects.clear();
            for(GpuMesh m:shapes.values())m.destroy();shapes.clear();
            for(GpuMesh m:terrain.values())m.destroy();terrain.clear();for(GpuMesh m:gridMeshes.values())m.destroy();gridMeshes.clear();
            for(GpuMesh m:vegetation.values())m.destroy();vegetation.clear();
            if(backdrop!=null){backdrop.destroy();backdrop=null;}
            chunks=Collections.emptyList();woods=Collections.emptyList();backdropSource=null;backdropSurface=null;snapshot=null;pcWaterClocks.clear();pcWaterAdvanced.clear();
            criticalHit=null;criticalPortrait=null;route=null;targets=Collections.emptySet();
        }
        sceneToken=token;
    }
    private int generation;
    private TerrainSurface backdropSurface;
    private Engine engine; private Renderer renderer; private Scene scene;
    private com.google.android.filament.android.DisplayHelper displayHelper;
    private Skybox skybox;
    private com.google.android.filament.View view; private Camera lens; private Material material;
    private Material waterMaterial;
    private MapSceneSnapshot.Ground poisonMaterialGround;
    private Material overviewGroundMaterial,overviewWaterMaterial;
    private boolean overviewTerrain;
    private int lastMaterialBinds;
    private double waterSeconds;private long waterLastTick;
    private Material pcGroundMaterial,pcGroundOutlineMaterial; private Texture pcNearLow,pcNearHigh,pcColor,pcPalette,pcPaletteSizes,pcGroundNormal,pcGroundPaint,pcGroundOutline; private int pcSeason=-1;
    private Material pcWaterMaterial;private Texture pcWaterSheet0,pcWaterSheet1;
    private final Map<String,Long> pcWaterClocks=new HashMap<>();
    private final Set<String> pcWaterAdvanced=new HashSet<>();
    private PcMapEffects pcMapEffects;
    private PcPresentations pcPresentations;private PcPresentationStage presentationStage;
    java.util.function.Consumer<PcPresentationStage> presentationStageObserver; // Installed diagnostic, null in normal play.
    private PcPresentationPlan.Cue presentationCue;private float presentationPhase;
    private PcPresentationPlan.Cue submittedPresentationCue;private float submittedPresentationPhase=Float.NaN;
    private Fence presentationFence;private boolean presentationDriverReady;private long presentationFenceWaits;
    private final Map<Texture,Long> pcTextureSizes=new HashMap<>();
    private Material groundMaterial; private final List<Texture> groundTextures=new ArrayList<>();
    private boolean environmentShadows;
    private IndirectLight skyLight;
    private SeasonStyle season;private int seasonUpdates;
    private Material siteMaterial,unitMaterial,sceneryMaterial; private Texture siteAtlas,fieldAtlas,unitAtlas,sceneryAtlas;private FieldAssets fieldAssets;private MaterialInstance vegetationMaterial;
    private PcSites pcSites;private Texture pcSitesAtlas;
    private PcFacilities pcFacilities;private PcCliffWalls pcCliffWalls;private PcDams pcDams;private Texture pcFacilitiesAtlas;private MaterialInstance pcFacilitiesInstance;
    private PcFacilityRigs pcFacilityRigs;private PcUnits pcUnits;private Material pcUnitMaterial,pcUnitAlphaMaterial;private final Texture[] pcUnitSheets=new Texture[14];
    private PcScenery pcScenery;private Material pcSceneryMaterial;private MaterialInstance pcSceneryInstance;private Texture pcSceneryAtlas;
    private PcEnvironment pcEnvironment;private Texture pcPaint;private int pcLightMonth=-1;private int pcFogMonth=-1;private float pcFogNear,pcFogFar;
    private boolean assetSyncPending;private int assetUploadBudget=2;
    private int siteLod=1; private final Set<String> missingAssets=new HashSet<>();
    private boolean srgbSwapChain;
    private boolean outputProbePending,outputVerified;
    private float pendingLegacyScaleDp=Float.NaN;
    private final long createdWallMillis=android.os.SystemClock.elapsedRealtime();
    private final StringBuilder startupPhaseRows=new StringBuilder("phase,wall_ms,thread_cpu_ms\n");
    private long startupPhaseWall,startupPhaseCpu;
    private void startupPhase(String name){long wall=System.nanoTime(),cpu=android.os.Debug.threadCpuTimeNanos();if(startupPhaseWall!=0){String row=name+","+((wall-startupPhaseWall)/1e6)+","+((cpu-startupPhaseCpu)/1e6)+"\n";startupPhaseRows.append(row);android.util.Log.i("SceneTiming","renderer_init_phase "+row.trim());}startupPhaseWall=wall;startupPhaseCpu=cpu;}
    String startupPhases(){return startupPhaseRows.toString();}
    private long firstSubmittedMillis=-1,firstVerifiedMillis=-1,resumeWallMillis=-1,resumeVerifiedMillis=-1;
    private Runnable verifiedOutputListener;
    void onVerifiedOutput(Runnable listener){verifiedOutputListener=listener;}
    private boolean loadingCovered;
    void loadingCovered(boolean value){loadingCovered=value;}
    private int uniformOutputCount;
    private long lastOutputProbe;
    private String outputStatus="WAITING_SURFACE";
    private long surfaceFrames;
    private long frameCallbacks,beginAttempts,beginSkipped,outputCopies,lastWorkLog,gpuPreparationFrames;
    private int lastMeshUploads;
    private long lastMeshUploadNanos;
    private SwapChain swap; private int cameraEntity,light;
    private boolean released,resumed=true,queued,diagnostics;
    private final long[] cpuSamples=new long[240];private int cpuCount,cpuCursor;
    private long lastFrame; private long renderedFrames; private double callbackMillis;
    private MapSceneSnapshot snapshot;
    private SceneMesh.TerrainWindow terrainWindow;
    // Actual accepted-mesh summary retained for existing native acceptance probes.
    private boolean distantTerrain;
    private boolean pendingFit;
    private Hex pendingInitialFocus;
    private MapSceneSnapshot pendingLayoutSnapshot;
    private GpuMesh backdrop;private SceneMesh backdropSource;
    private List<SceneMesh> chunks=Collections.emptyList(),woods=Collections.emptyList();
    private Set<Hex> woodExcluded=Collections.emptySet();
    private final Map<SceneMesh,GpuMesh> vegetation=new HashMap<>();
    private int visibleWood,unitLod=1;private long animationTick;private boolean effectsPaused;private long pausedEffectTick;
    private final Map<SceneMesh,GpuMesh> terrain=new HashMap<>();
    private final Map<SceneMesh,GpuMesh> gridMeshes=new HashMap<>();
    private long gridUploads;
    private final Map<String,Proxy> objects=new HashMap<>();
    private final Map<String,GpuMesh> shapes=new LinkedHashMap<>(128,.75f,true);
    private int pending,visibleChunks,visibleObjects;
    private Set<Hex> targets=Collections.emptySet();
    private MarchOrders.Plan route;
    private final GestureDetector gestures; private final ScaleGestureDetector scaler;
    private boolean multi,panelGesture;private int panelRight,panelBottom;
    void setPanelOcclusion(int right,int bottom){panelRight=Math.max(0,right);panelBottom=Math.max(0,bottom);overlay.invalidate();}
    private float multiX=Float.NaN,multiY,multiAngle;
    private String lastPick="none";
    private long suppressedGesture=-1;
    private Set<Hex> editorCells=Collections.emptySet();
    private boolean editorValid=true,editorDrawing,showCommanders=true,showUnitBars=true;
    private MapHost.EditorStroke editorStroke;
    private Displacement.Preview tacticPreview;
    void editorDrawing(boolean value){editorDrawing=value;}
    void editorMode(MapHost.EditorStroke value){editorStroke=value;}
    void editorPreview(Set<Hex> cells,boolean valid){editorCells=new HashSet<>(cells);editorValid=valid;overlay.invalidate();}
    void setTacticPreview(Displacement.Preview value){tacticPreview=value;overlay.invalidate();}
    void labels(boolean commanders,boolean bars){showCommanders=commanders;showUnitBars=bars;overlay.invalidate();}

    private boolean navigatorShown=true,miniGesture;
    void navigator(boolean shown){navigatorShown=shown;overlay.invalidate();}
    private boolean commandTargeting;
    void commandTargeting(boolean active){commandTargeting=active;}
    private boolean gridShown;
    void setGridShown(boolean shown){gridShown=shown;terrainVisibilityDirty=true;overlay.invalidate();schedule();}
    private boolean editorGrid,editorCoords,editorFootprints;private Set<Long> impassable=Collections.emptySet();
    private int territoryMode,previewFaction=-1;private boolean openingPreview;
    private int[] territoryColors,territoryBorders;private final Map<String,String> factionLabels=new HashMap<>();
    private final Map<String,Integer> siteOwners=new HashMap<>();
    void editorLayers(Set<Long> blocked,boolean grid,boolean coords,boolean footprints){
        editorGrid=grid;editorCoords=coords;editorFootprints=footprints;
        impassable=Set.copyOf(blocked);terrainVisibilityDirty=true;overlay.invalidate();
    }
    void previewFaction(int side){previewFaction=side;overlay.invalidate();}
    void mapLayers(MapLayerData data,int mode,boolean preview,int side){
        territoryMode=mode;openingPreview=preview;previewFaction=side;
        factionLabels.clear();factionLabels.putAll(data.factionLabels);
        siteOwners.clear();siteOwners.putAll(data.siteOwners);territoryColors=data.colors();territoryBorders=data.boundaries();overlay.miniDirty=true;overlay.invalidate();
    }
    private final CombatVisual combat=new CombatVisual();
    private final GpuMesh[] effectMeshes=new GpuMesh[CombatVisual.MESH_COUNT];
    private final int[] effectEntities=new int[CombatVisual.CAPACITY],effectKinds=new int[CombatVisual.CAPACITY];
    private final boolean[] effectShown=new boolean[CombatVisual.CAPACITY];
    private final float[] effectMatrix=new float[16];
    private android.graphics.drawable.Drawable criticalPortrait;
    private CriticalHit criticalHit;private float criticalPhase;private Runnable criticalSkip;
    private TurnJournal.Event replay;private float replayFraction;private Proxy animatedUnit;
    FilamentMapView(Context context,MapHost.TileListener listener,Consumer<Throwable> failure) throws Exception {
        super(context);this.listener=listener;this.failure=failure;
        quality=SceneQuality.from(context.getSharedPreferences("map-renderer",0).getAll().get("quality"));
        surface=new SurfaceView(context);addView(surface,new LayoutParams(-1,-1));overlay=new Overlay(context);addView(overlay,new LayoutParams(-1,-1));
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!draggingUnit&&!multi&&!scaler.isInProgress()&&!editorDrawing){camera.pan(-dx,-dy,b.getX(),b.getY(),pickHeight(b.getX(),b.getY()));clampCamera();}return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent e){
                if((criticalHit!=null||presentationCue!=null)&&criticalSkip!=null){criticalSkip.run();return true;}
                if(!multi&&e.getDownTime()!=suppressedGesture&&!blocked(e.getX(),e.getY())&&!editorDrawing&&snapshot!=null){Hex h=pick(e.getX(),e.getY(),!commandTargeting&&editorStroke==null);if(snapshot.ground.valid(h)){performClick();if(pickedUnitId>=0)listener.unit(pickedUnitId,h);else listener.tap(h);}}return true;
            }
            @Override public void onLongPress(MotionEvent e){
                if(e.getDownTime()==activeDragGesture&&!multi&&isEnabled()&&!editorDrawing&&!blocked(e.getX(),e.getY())&&snapshot!=null){
                    suppressedGesture=e.getDownTime();Hex h=pick(e.getX(),e.getY(),!commandTargeting&&editorStroke==null);
                    if(h!=null){
                        if(unitDrag!=null&&unitDrag.begin(h)){draggingUnit=true;dragTarget=h;dragPlan=null;dragActorKey="unit:"+unitDrag.actorId();overlay.invalidate();}
                        else center(h);
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    }
                }
            }
            @Override public boolean onDoubleTap(MotionEvent e){if(!multi&&!blocked(e.getX(),e.getY()))zoomAt(1.7f,e.getX(),e.getY());return true;}
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoomAt(d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}});
        try{
            startupPhase("begin");Filament.init();engine=Engine.create(Engine.Backend.OPENGL);
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
            Renderer.ClearOptions clear=new Renderer.ClearOptions();clear.clear=true;clear.clearColor=new float[]{.075f,.10f,.11f,1};renderer.setClearOptions(clear);startupPhase("engine_and_view");
            byte[] bytes=VerifiedMaterial.read("3d/terrain.filamat",context.getAssets().open("3d/terrain.filamat"));
            ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();material=new Material.Builder().payload(payload,bytes.length).build(engine);
            byte[] siteBytes=VerifiedMaterial.read("3d/sites/site.filamat",context.getAssets().open("3d/sites/site.filamat"));
            ByteBuffer sb=ByteBuffer.allocateDirect(siteBytes.length).order(ByteOrder.nativeOrder());sb.put(siteBytes).flip();siteMaterial=new Material.Builder().payload(sb,siteBytes.length).build(engine);
            byte[] unitBytes=VerifiedMaterial.read("3d/field/unit.filamat",context.getAssets().open("3d/field/unit.filamat"));
            ByteBuffer ub=ByteBuffer.allocateDirect(unitBytes.length).order(ByteOrder.nativeOrder());ub.put(unitBytes).flip();unitMaterial=new Material.Builder().payload(ub,unitBytes.length).build(engine);
            loadGroundMaterials(context);
            loadWaterMaterial(context);
            loadOverviewMaterials(context);startupPhase("common_ground_water_materials");
            siteAtlas=loadAtlas(context,"3d/sites/atlas.png");startupPhase("compat_site_atlas");
            fieldAssets=new FieldAssets(name->context.getAssets().open("3d/field/"+name));
            pcUnits=new PcUnits(context.getAssets().open("3d/pc-units/units.pcz"));startupPhase("source_unit_data");
            byte[] pcUnitBytes=VerifiedMaterial.read("3d/pc-units/unit.filamat",context.getAssets().open("3d/pc-units/unit.filamat"));
            pcUnitMaterial=new Material.Builder().payload(ByteBuffer.wrap(pcUnitBytes),pcUnitBytes.length).build(engine);
            byte[] pcUnitAlphaBytes=VerifiedMaterial.read("3d/pc-units/unit-alpha.filamat",context.getAssets().open("3d/pc-units/unit-alpha.filamat"));
            pcUnitAlphaMaterial=new Material.Builder().payload(ByteBuffer.wrap(pcUnitAlphaBytes),pcUnitAlphaBytes.length).build(engine);
            for(int i=0;i<14;i++)pcUnitSheets[i]=sourceTexture(String.format(java.util.Locale.ROOT,"3d/pc-units/unit-%02d.png",i),false);startupPhase("source_unit_materials_and_sheets");
            pcSites=new PcSites(context.getAssets().open("3d/pc-sites/sites.pcz"));
            pcSitesAtlas=sourceTexture("3d/pc-sites/atlas.png",false,true);startupPhase("source_sites");
            pcFacilities=new PcFacilities(context.getAssets().open("3d/pc-facilities/facilities.pcz"));
            pcFacilityRigs=new PcFacilityRigs(context.getAssets().open("3d/pc-facilities/rigs.pcz"));
            pcCliffWalls=new PcCliffWalls(context.getAssets().open("3d/pc-facilities/cliff-walls.pcz"),pcFacilities);
            pcDams=new PcDams(context.getAssets().open("3d/pc-facilities/dams.pcz"));
            pcFacilitiesAtlas=sourceTexture("3d/pc-facilities/atlas.png",false,true);startupPhase("source_facilities");
            pcScenery=new PcScenery(context.getAssets().open("3d/pc-scenery/scenery.pcz"));
            byte[] nativeScenery=VerifiedMaterial.read("3d/pc-scenery/scenery.filamat",context.getAssets().open("3d/pc-scenery/scenery.filamat"));
            pcSceneryMaterial=new Material.Builder().payload(ByteBuffer.wrap(nativeScenery),nativeScenery.length).build(engine);
            pcSceneryAtlas=sourceTexture("3d/pc-scenery/atlas.png",false,true);startupPhase("source_scenery");
            pcEnvironment=new PcEnvironment(context.getAssets().open("3d/pc-environment/environment.bin"));
            pcPaint=sourceTexture("3d/pc-environment/paint.png",true,true);
            pcSceneryInstance=pcSceneryMaterial.createInstance();
            pcSceneryInstance.setParameter("atlas",pcSceneryAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));
            pcFacilitiesInstance=pcSceneryMaterial.createInstance();
            pcFacilitiesInstance.setParameter("atlas",pcFacilitiesAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));
            bindPcPainting(pcSceneryInstance,1);bindPcPainting(pcFacilitiesInstance,1);startupPhase("source_environment");
            fieldAtlas=loadAtlas(context,"3d/field/atlas.png");
            unitAtlas=loadAtlas(context,"3d/field/unit-atlas.png");
            byte[] scenery=VerifiedMaterial.read("3d/field/v128/scenery.filamat",context.getAssets().open("3d/field/v128/scenery.filamat"));
            sceneryMaterial=new Material.Builder().payload(java.nio.ByteBuffer.wrap(scenery),scenery.length).build(engine);
            sceneryAtlas=loadAtlas(context,"3d/field/v129/scenery-atlas.png");
            vegetationMaterial=sceneryMaterial.createInstance();vegetationMaterial.setParameter("atlas",sceneryAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));vegetationMaterial.setParameter("damage",0f);
            vegetationMaterial.setParameter("flowTime",0f);startupPhase("compat_field_and_scenery");
            light=EntityManager.get().create();environmentShadows=quality!=SceneQuality.LOW&&manager!=null&&manager.getDeviceConfigurationInfo().reqGlEsVersion>=0x30001;EnvironmentProfile.sun(engine,light,quality,environmentShadows);scene.addEntity(light);
            skyLight=EnvironmentProfile.sky(engine);scene.setIndirectLight(skyLight);
            applySeason(SeasonStyle.SPRING);
            surface.getHolder().addCallback(this);
            if(android.os.Build.VERSION.SDK_INT>=29){
                thermalManager=context.getSystemService(android.os.PowerManager.class);
                if(thermalManager!=null){thermalListener=this::thermalChanged;thermalManager.addThermalStatusListener(context.getMainExecutor(),thermalListener);thermalChanged(thermalManager.getCurrentThermalStatus());}
            }
        }catch(Exception|LinkageError|OutOfMemoryError e){release();throw e;}
    }
    private void loadGroundMaterials(Context context)throws java.io.IOException {
        long groundStarted=System.nanoTime();
        byte[] bytes=VerifiedMaterial.read("3d/terrain/v129/ground.filamat",context.getAssets().open("3d/terrain/v129/ground.filamat"));
        ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();
        groundMaterial=new Material.Builder().payload(payload,bytes.length).build(engine);
        groundMaterial.getDefaultInstance().setParameter("poisonGrid",0f,0f);
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
    private Texture pcTexture(String name,boolean index)throws java.io.IOException {
        return sourceTexture("3d/pc-map/"+name,index);
    }
    /** Source RGBA sheets cannot use the legacy RGB-only ETC2 atlas path. */
    private Texture sourceTexture(String path,boolean index)throws java.io.IOException {
        return sourceTexture(path,index,index);
    }
    /** Raw encoded channels for original D3D painting arithmetic; numeric maps
     * also use RGBA8. Existing PBR source consumers keep their sRGB boundary. */
    private Texture sourceTexture(String path,boolean index,boolean encoded)throws java.io.IOException {
        android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inScaled=false;
        options.inPremultiplied=false;
        android.graphics.Bitmap bitmap;
        try(java.io.InputStream in=getContext().getAssets().open(path)){bitmap=android.graphics.BitmapFactory.decodeStream(in,null,options);}
        if(bitmap==null)throw new java.io.IOException("PC source texture missing: "+path);
        int levels=index?1:32-Integer.numberOfLeadingZeros(Math.max(bitmap.getWidth(),bitmap.getHeight()));
        Texture texture=null;boolean queuedPixels=false;
        try{
            texture=new Texture.Builder().width(bitmap.getWidth()).height(bitmap.getHeight()).levels(levels).sampler(Texture.Sampler.SAMPLER_2D).format(encoded?Texture.InternalFormat.RGBA8:Texture.InternalFormat.SRGB8_A8).build(engine);
            com.google.android.filament.android.TextureHelper.setBitmap(engine,texture,0,bitmap,new android.os.Handler(android.os.Looper.getMainLooper()),bitmap::recycle);queuedPixels=true;
            if(!index)texture.generateMipmaps(engine);
            long bytes=0;for(int level=0;level<levels;level++)bytes+=(long)Math.max(1,bitmap.getWidth()>>level)*Math.max(1,bitmap.getHeight()>>level)*4;
            pcTextureSizes.put(texture,bytes);
            return texture;
        }catch(RuntimeException error){if(texture!=null)engine.destroyTexture(texture);throw error;}
        finally{if(!queuedPixels)bitmap.recycle();}
    }
    private void destroyPcTexture(Texture texture){if(texture!=null){pcTextureSizes.remove(texture);engine.destroyTexture(texture);}}
    private void bindPcPainting(MaterialInstance instance,int month){
        float[] direction=pcEnvironment.baseDirection(month);
        instance.setParameter("paint",pcPaint,new TextureSampler(TextureSampler.MinFilter.LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));
        instance.setParameter("sourceLight",direction[0],direction[1],direction[2]);
    }
    private void syncPcPainting(){
        int month=snapshot==null?1:snapshot.month;
        if(month==pcLightMonth)return;
        bindPcPainting(pcSceneryInstance,month);bindPcPainting(pcFacilitiesInstance,month);
        for(Proxy p:objects.values())if(p.instance!=null&&(p.shape.source.pcSite||p.shape.source.pcFacility)&&!p.shape.source.pcFacilityRig)bindPcPainting(p.instance,month);
        pcLightMonth=month;
    }
    private void syncPcGround(){
        if(snapshot==null||snapshot.ground.pcMap==null)return;
        try{
            if(pcWaterMaterial==null){
                byte[] water=VerifiedMaterial.read("3d/pc-map/water.filamat",getContext().getAssets().open("3d/pc-map/water.filamat"));
                pcWaterMaterial=new Material.Builder().payload(ByteBuffer.wrap(water),water.length).build(engine);
                pcWaterSheet0=sourceTexture("3d/pc-map/water-0.png",true,true);
                pcWaterSheet1=sourceTexture("3d/pc-map/water-1.png",true,true);
            }
            if(pcGroundMaterial==null){
                byte[] bytes=VerifiedMaterial.read("3d/pc-map/ground.filamat",getContext().getAssets().open("3d/pc-map/ground.filamat"));
                pcGroundMaterial=new Material.Builder().payload(ByteBuffer.wrap(bytes),bytes.length).build(engine);
                byte[] outline=VerifiedMaterial.read("3d/pc-map/ground-outline.filamat",getContext().getAssets().open("3d/pc-map/ground-outline.filamat"));
                pcGroundOutlineMaterial=new Material.Builder().payload(ByteBuffer.wrap(outline),outline.length).build(engine);
                pcNearLow=pcTexture("face-low.png",true);pcNearHigh=pcTexture("face-high.png",true);
                pcGroundNormal=pcTexture("ground-normal.png",true);
                pcGroundOutline=sourceTexture("3d/pc-map/ground-outline.png",true,true);
                pcPaletteSizes=pcTexture("palette-sizes.png",true);
                TextureSampler nearest=new TextureSampler(TextureSampler.MinFilter.NEAREST,TextureSampler.MagFilter.NEAREST,TextureSampler.WrapMode.CLAMP_TO_EDGE);
                pcGroundMaterial.getDefaultInstance().setParameter("pcNearLow",pcNearLow,nearest);
                pcGroundMaterial.getDefaultInstance().setParameter("pcNearHigh",pcNearHigh,nearest);
                pcGroundMaterial.getDefaultInstance().setParameter("pcPaletteSizes",pcPaletteSizes,nearest);
                pcGroundMaterial.getDefaultInstance().setParameter("pcNormal",pcGroundNormal,nearest);
                pcGroundOutlineMaterial.getDefaultInstance().setParameter("pcNormal",pcGroundNormal,nearest);
                pcGroundOutlineMaterial.getDefaultInstance().setParameter("pcOutline",pcGroundOutline,nearest);
            }
            int quarter=(snapshot.month-1)/3,next=quarter==0?1:quarter==1?2:quarter==2?0:3; // PC archive: autumn, spring, summer, winter
            if(pcSeason!=next){
                Texture color=null,palette=null,paint=null;
                try{color=sourceTexture("3d/pc-map/color-"+next+".png",true,true);palette=sourceTexture("3d/pc-map/palette-"+next+".png",false,true);paint=sourceTexture("3d/pc-map/ground-paint-"+next+".png",false,true);}
                catch(java.io.IOException|RuntimeException e){destroyPcTexture(color);destroyPcTexture(palette);destroyPcTexture(paint);throw e;}
                TextureSampler nearest=new TextureSampler(TextureSampler.MinFilter.NEAREST,TextureSampler.MagFilter.NEAREST,TextureSampler.WrapMode.CLAMP_TO_EDGE);
                TextureSampler sampler=new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE);
                pcGroundMaterial.getDefaultInstance().setParameter("pcColor",color,nearest);
                pcGroundMaterial.getDefaultInstance().setParameter("pcPalette",palette,sampler);
                pcGroundMaterial.getDefaultInstance().setParameter("pcPaint",paint,sampler);
                destroyPcTexture(pcColor);destroyPcTexture(pcPalette);destroyPcTexture(pcGroundPaint);
                pcColor=color;pcPalette=palette;pcGroundPaint=paint;pcSeason=next;
            }
            var ground=snapshot.ground;
            pcGroundMaterial.getDefaultInstance().setParameter("pcOrigin",(float)ground.sourceOriginX,(float)ground.sourceOriginY,(float)next);
            pcGroundOutlineMaterial.getDefaultInstance().setParameter("pcOrigin",(float)ground.sourceOriginX,(float)ground.sourceOriginY,(float)next);
            float[] ambient=pcEnvironment.baseAmbient(snapshot.month),direction=pcEnvironment.baseDirection(snapshot.month);
            pcGroundMaterial.getDefaultInstance().setParameter("pcAmbient",ambient[0],ambient[1],ambient[2]);
            pcGroundMaterial.getDefaultInstance().setParameter("pcLight",direction[0],direction[1],direction[2]);
        }catch(java.io.IOException e){throw new IllegalStateException("PC map material unavailable",e);}
    }
    private void syncPcGroundFog(){
        if(pcGroundMaterial==null||snapshot==null||snapshot.ground.pcMap==null)return;
        float near=(float)(camera.nearPlane()/.05),far=(float)(camera.farPlane()/.05);
        if(pcFogMonth==snapshot.month&&pcFogNear==near&&pcFogFar==far)return;
        float[] fog=pcEnvironment.groundFog(snapshot.month,near,far),color=pcEnvironment.baseFogColor(snapshot.month);
        for(Material m:new Material[]{pcGroundMaterial,pcGroundOutlineMaterial}){
            MaterialInstance instance=m.getDefaultInstance();instance.setParameter("pcFog",fog[0],fog[1],fog[2]);instance.setParameter("pcFade",fog[3],fog[4]);instance.setParameter("pcFogColor",color[0],color[1],color[2]);
        }
        pcFogMonth=snapshot.month;pcFogNear=near;pcFogFar=far;
    }
    private MaterialInstance landMaterial(boolean overview){
        return snapshot!=null&&snapshot.ground.pcMap!=null&&pcGroundMaterial!=null?pcGroundMaterial.getDefaultInstance():(overview?overviewGroundMaterial:groundMaterial).getDefaultInstance();
    }
    private void loadWaterMaterial(Context context)throws java.io.IOException {
        byte[] bytes=VerifiedMaterial.read("3d/terrain/water.filamat",context.getAssets().open("3d/terrain/water.filamat"));
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
    /** Camera-LOD programs use the same texture owners and unchanged world geometry.
     * No normal/PBR shader variants at national scale; detailed programs remain intact. */
    private void loadOverviewMaterials(Context context)throws java.io.IOException {
        overviewGroundMaterial=loadOverviewMaterial(context,"ground-overview");
        overviewGroundMaterial.getDefaultInstance().setParameter("poisonGrid",0f,0f);
        overviewWaterMaterial=loadOverviewMaterial(context,"water-overview");
        TextureSampler sampler=new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.REPEAT);
        String[] layers={"grass","soil","sand","rock"};
        for(int i=0;i<layers.length;i++){
            Texture texture=groundTextures.get(i*2); // Borrowed, never duplicated or destroyed here.
            overviewGroundMaterial.getDefaultInstance().setParameter(layers[i]+"Color",texture,sampler);
            overviewWaterMaterial.getDefaultInstance().setParameter(layers[i]+"Color",texture,sampler);
        }
        overviewWaterMaterial.getDefaultInstance().setParameter("waveTime",0f);
    }
    private Material loadOverviewMaterial(Context context,String name)throws java.io.IOException {
        byte[] bytes;
        String path="3d/terrain/"+(name.equals("ground-overview")?"v129/":"")+name+".filamat";
        bytes=VerifiedMaterial.read(path,context.getAssets().open(path));
        ByteBuffer payload=ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());payload.put(bytes).flip();
        return new Material.Builder().payload(payload,bytes.length).build(engine);
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
        if(released)return;thermalStatus=status;
        if(thermal.update(status,System.nanoTime())){terrainVisibilityDirty=true;pacer.reset();resizeSurface();}
    }
    private void resizeSurface(){
        int w=getWidth(),h=getHeight();float scale=thermal.scale(quality);
        if(w>0&&h>0)surface.getHolder().setFixedSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);camera.width=Math.max(1,w);camera.height=Math.max(1,h);if(Float.isFinite(pendingLegacyScaleDp)){camera.span=SceneCamera.legacySpan(h,getResources().getDisplayMetrics().density,pendingLegacyScaleDp);pendingLegacyScaleDp=Float.NaN;}
        resizeSurface();
        if(pendingLayoutSnapshot!=null&&w>0&&h>0){
            MapSceneSnapshot next=pendingLayoutSnapshot;pendingLayoutSnapshot=null;snapshot(next);
        }else if(pendingFit)fit();
    }
    /** The host owns rule previews and the existing MainActivity drop command. */
    interface UnitDrag {
        int actorId();
        boolean begin(Hex h);
        MarchOrders.Plan preview(Hex h);
        void drop(MarchOrders.Plan plan);
    }
    private UnitDrag unitDrag;
    private long activeDragGesture=-1;
    private boolean draggingUnit;
    private Hex dragTarget;
    private MarchOrders.Plan dragPlan;
    private String dragActorKey;
    private SceneMesh dragMesh;
    void setUnitDrag(UnitDrag handler){cancelUnitDrag();unitDrag=handler;}
    private void cancelUnitDrag(){activeDragGesture=-1;draggingUnit=false;dragTarget=null;dragPlan=null;dragActorKey=null;dragMesh=null;if(overlay!=null){overlay.ghost.clear();overlay.invalidate();}}
    private void cancelInteraction(){
        suppressedGesture=activeDragGesture;cancelUnitDrag();multi=true;miniGesture=false;multiX=Float.NaN;
        long now=android.os.SystemClock.uptimeMillis();MotionEvent cancel=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);
        gestures.onTouchEvent(cancel);scaler.onTouchEvent(cancel);cancel.recycle();
    }
    @Override public void setEnabled(boolean enabled){if(!enabled&&gestures!=null)cancelInteraction();super.setEnabled(enabled);}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return true;
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){cancelUnitDrag();activeDragGesture=e.getDownTime();multi=false;}
        overlay.layoutMini();
        if(action==MotionEvent.ACTION_DOWN)miniGesture=!openingPreview&&navigatorShown&&overlay.miniRect.contains(e.getX(),e.getY())&&!blocked(e.getX(),e.getY());
        if(miniGesture){
            suppressedGesture=e.getDownTime();
            if(e.getPointerCount()>1||action==MotionEvent.ACTION_CANCEL)multi=true;
            if(!multi&&e.getPointerCount()==1&&(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE)&&!blocked(e.getX(),e.getY()))overlay.navigate(e.getX(),e.getY());
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)miniGesture=false;
            return true;
        }
        if(action==MotionEvent.ACTION_DOWN){multi=false;multiX=Float.NaN;panelGesture=blocked(e.getX(),e.getY());}
        for(int i=0;i<e.getPointerCount();i++)if(blocked(e.getX(i),e.getY(i)))panelGesture=true;
        if(panelGesture||action==MotionEvent.ACTION_CANCEL){
            cancelUnitDrag();
            suppressedGesture=e.getDownTime();MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
            gestures.onTouchEvent(cancel);scaler.onTouchEvent(cancel);cancel.recycle();
            if(editorDrawing&&editorStroke!=null)editorStroke.event(MotionEvent.ACTION_CANCEL,null);multiX=Float.NaN;return true;
        }
        if(e.getPointerCount()>1){cancelUnitDrag();if(!multi&&editorDrawing&&editorStroke!=null)editorStroke.event(MotionEvent.ACTION_CANCEL,null);multi=true;suppressedGesture=e.getDownTime();}
        if(draggingUnit){
            Hex h=!blocked(e.getX(),e.getY())&&(!navigatorShown||!overlay.miniRect.contains(e.getX(),e.getY()))
                ?pick(e.getX(),e.getY(),false):null;
            if(!Objects.equals(h,dragTarget)){dragTarget=h;dragPlan=unitDrag.preview(h);}
            if(action==MotionEvent.ACTION_UP){
                // Resolve once more against the current host state; never execute a stale preview.
                MarchOrders.Plan plan=unitDrag.preview(h);cancelUnitDrag();
                MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
                gestures.onTouchEvent(cancel);scaler.onTouchEvent(cancel);cancel.recycle();
                if(plan!=null)unitDrag.drop(plan);else announceForAccessibility("移动已取消，请放到本旬可达的空格");
            }
            overlay.invalidate();return true;
        }
        if(e.getPointerCount()>1){
            float cx=(e.getX(0)+e.getX(1))*.5f,cy=(e.getY(0)+e.getY(1))*.5f;
            float angle=(float)Math.toDegrees(Math.atan2(e.getY(1)-e.getY(0),e.getX(1)-e.getX(0)));
            if(action==MotionEvent.ACTION_MOVE&&Float.isFinite(multiX)){
                if(e.getPointerCount()==2){camera.pan(cx-multiX,cy-multiY,cx,cy,pickHeight(cx,cy));camera.orbit(((angle-multiAngle+540)%360)-180,0,cx,cy,pickHeight(cx,cy));}
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
        pickedUnitId=-1;
        if(snapshot==null)return null;
        float height=snapshot.ground.surface.rayHeight(camera,sx,sy);
        Hex ground=snapshot.ground.surface.pick(camera,sx,sy);
        lastPick=ground==null?"MISS":"cell="+ground+" world="+camera.worldX(sx,sy,height)+","+height+","+camera.worldZ(sx,sy,height)+" surface="+(snapshot.ground.surface.water(ground)?"water":"terrain");
        Hex object=objectsAllowed?pickObject(sx,sy):null;return object==null?ground:object;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    void snapshot(MapSceneSnapshot next){
        meshWork.owner();if(released)return;cancelUnitDrag();
        // A requested national fit or landmark focus waits for real dimensions:
        // the default 1x1 camera must never launch a redundant square CPU window.
        if((pendingFit||pendingInitialFocus!=null)&&(getWidth()==0||getHeight()==0)){pendingLayoutSnapshot=next;return;}
        if(pendingInitialFocus!=null){
            Hex target=pendingInitialFocus;pendingInitialFocus=null;pendingFit=false;
            camera.x=next.ground.grid.x(target);camera.z=next.ground.grid.z(target);camera.span=10;
        }
        boolean scenerySeasonChanged=snapshot==null||PcScenery.season(snapshot.month)!=PcScenery.season(next.month);
        boolean groundChanged=snapshot==null||snapshot.ground!=next.ground;
        boolean terrainChanged=snapshot==null||snapshot.ground.surface!=next.ground.surface;
        if(snapshot!=null&&(snapshot.ground.mapIdentity!=next.ground.mapIdentity||snapshot.ground.sourceOriginX!=next.ground.sourceOriginX||snapshot.ground.sourceOriginY!=next.ground.sourceOriginY)){pcWaterClocks.clear();closePcMapEffects();closePcPresentations();}
        snapshot=next;terrainVisibilityDirty=true;
        if(pendingFit)fit();
        Set<Hex> excluded=Vegetation.exclusions(next);boolean woodsChanged=terrainChanged||next.ground.pcMap!=null&&scenerySeasonChanged||!excluded.equals(woodExcluded);
        if(groundChanged||woodsChanged){
            outputVerified=false;outputStatus="WAITING_MESH";uniformOutputCount=0;
            woodExcluded=excluded;generation++;
            List<SceneMesh> previous=chunks,oldWoods=woods;pending=1;
            final FieldAssets assets=fieldAssets;final PcScenery nativeAssets=pcScenery;final PcCliffWalls nativeWalls=pcCliffWalls;final SceneMesh oldScenery=backdropSource;
            terrainWindow=new SceneMesh.TerrainWindow(camera.x,camera.z,camera.extentX(),camera.extentZ(),camera.span);
            SceneMesh.TerrainWindow requested=terrainWindow;
            previous=SceneMesh.windowCoverage(previous,requested);
            List<SceneMesh> coveredPrevious=previous;
            long queuedAt=System.nanoTime();
            meshWork.submitPhased(publish->buildMeshes(next.ground,terrainChanged,oldScenery,coveredPrevious,oldWoods,assets,nativeAssets,nativeWalls,next.month,excluded,requested,queuedAt,publish));
        }
        // The owner accepts immutable state now; GPU updates wait for frame admission.
        assetSyncPending=true;refreshPendingMeshes();overlay.invalidate();schedule();
    }
    private static long runtimeCounter(String key){
        try{String value=android.os.Debug.getRuntimeStat(key);return value==null?-1:Long.parseLong(value);}catch(RuntimeException e){return -1;}
    }
    private static MeshResult buildMeshes(MapSceneSnapshot.Ground ground,boolean changed,SceneMesh scenery,
            List<SceneMesh> previous,List<SceneMesh> trees,FieldAssets assets,PcScenery nativeAssets,PcCliffWalls nativeWalls,int month,Set<Hex> excluded,
            SceneMesh.TerrainWindow window,long queuedAt,Consumer<MeshResult> publish)throws Exception {
        long started=System.nanoTime(),cpu=android.os.Debug.threadCpuTimeNanos();
        long gc=runtimeCounter("art.gc.gc-time"),allocated=runtimeCounter("art.gc.bytes-allocated");
        if(changed)scenery=SceneMesh.backdrop(ground);
        long backgroundDone=System.nanoTime(),backgroundCpu=android.os.Debug.threadCpuTimeNanos();
        List<SceneMesh> forest=null;long sourceForestWall=0,sourceForestCpu=0;
        // Original objects do not depend on completed ground chunks. Deliver
        // their small source batches before terrain fills the driver's queue.
        // Legacy vegetation retains its existing ground-first loading order.
        if(ground.pcMap!=null){
            long forestStart=System.nanoTime(),forestStartCpu=android.os.Debug.threadCpuTimeNanos();
            List<SceneMesh> source=new ArrayList<>(nativeAssets.buildWindow(ground,excluded,trees,window,month));
            source.addAll(nativeWalls.buildWindow(ground,trees,window,month));forest=Collections.unmodifiableList(source);
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Source scenery superseded");
            sourceForestWall=System.nanoTime()-forestStart;sourceForestCpu=android.os.Debug.threadCpuTimeNanos()-forestStartCpu;
            publish.accept(new MeshResult(scenery,previous,forest,ground.surface));
            android.util.Log.i("Sanguo3D","Source scenery CPU ready before terrain chunks="+forest.size()+" sceneryWallMs="+sourceForestWall/1e6+" sceneryCpuMs="+sourceForestCpu/1e6);
        }
        long groundStarted=System.nanoTime(),groundStartCpu=android.os.Debug.threadCpuTimeNanos();
        SceneMesh.BuildStats stats=new SceneMesh.BuildStats();
        final SceneMesh readyScenery=scenery;
        // Overlap CPU construction with bounded GPU uploads on every window.
        // Missing replacement keys retain old coverage; loadVisible keeps their
        // GPU meshes until the new key's upload actually finishes.
        stats.firstLoadBatch=partial->publish.accept(new MeshResult(readyScenery,
            SceneMesh.retainWindowCoverage(previous,partial),null,ground.surface));
        stats.progress=()->android.util.Log.i("Sanguo3D","Ground progress builtChunks="+stats.builtChunks+" elapsedWallMs="+(System.nanoTime()-started)/1e6+" threadCpuMs="+(android.os.Debug.threadCpuTimeNanos()-cpu)/1e6+" "+stats);
        List<SceneMesh> built=SceneMesh.ground(ground,previous,window,stats);
        long groundDone=System.nanoTime(),groundCpu=android.os.Debug.threadCpuTimeNanos();
        android.util.Log.i("Sanguo3D","Ground CPU ready chunks="+built.size()+" ms="+(groundDone-started)/1e6
            +" queueWaitWallMs="+(started-queuedAt)/1e6+" backdropWallMs="+(backgroundDone-started)/1e6
            +" backdropCpuMs="+(backgroundCpu-cpu)/1e6+" groundWallMs="+(groundDone-groundStarted)/1e6
            +" groundCpuMs="+(groundCpu-groundStartCpu)/1e6+" "+stats);
        if(Thread.currentThread().isInterrupted())throw new InterruptedException("Ground superseded");
        // Existing worker/mailbox, same epoch: forest construction cannot hold
        // completed ground CPU meshes hostage. NULL trees means unchanged trees.
        publish.accept(new MeshResult(scenery,built,null,ground.surface));
        long forestStarted=System.nanoTime(),forestCpu=android.os.Debug.threadCpuTimeNanos();
        if(forest==null)forest=Vegetation.buildWindow(ground,excluded,trees,assets,window);
        long done=System.nanoTime(),endGc=runtimeCounter("art.gc.gc-time"),endAllocated=runtimeCounter("art.gc.bytes-allocated");
        android.util.Log.i("Sanguo3D","Field CPU ready forestChunks="+forest.size()+" totalMs="+(done-started)/1e6
            +" sceneryWallMs="+(ground.pcMap!=null?sourceForestWall:done-forestStarted)/1e6+" sceneryCpuMs="+(ground.pcMap!=null?sourceForestCpu:android.os.Debug.threadCpuTimeNanos()-forestCpu)/1e6+" groundDeliveryBackpressureWallMs="+(forestStarted-groundDone)/1e6
            +" processGcMs="+(gc<0||endGc<0?-1:endGc-gc)+" processAllocatedBytes="+(allocated<0||endAllocated<0?-1:endAllocated-allocated)
            +" interrupted="+Thread.currentThread().isInterrupted());
        return new MeshResult(scenery,built,forest,ground.surface);
    }
    private void applySeason(SeasonStyle next){
        syncPcGround();
        syncPcPainting();
        // Ground layout is immutable; upload its pure display context only in
        // this admitted-frame path. Research/force ground wrappers reuse layout.
        if(snapshot!=null&&poisonMaterialGround!=snapshot.ground){
            var grid=snapshot.ground.grid;
            groundMaterial.getDefaultInstance().setParameter("poisonGrid",grid.staggered?1f:0f,grid.offset);
            overviewGroundMaterial.getDefaultInstance().setParameter("poisonGrid",grid.staggered?1f:0f,grid.offset);
            poisonMaterialGround=snapshot.ground;
        }
        if(season==next)return;
        season=next;seasonUpdates++;
        EnvironmentProfile.apply(engine,light,skyLight,next);
        EnvironmentProfile.pigment(groundMaterial.getDefaultInstance(),next,true);
        EnvironmentProfile.pigment(waterMaterial.getDefaultInstance(),next,true);
        waterMaterial.getDefaultInstance().setParameter("waterTint",next.waterR,next.waterG,next.waterB);
        EnvironmentProfile.pigment(overviewGroundMaterial.getDefaultInstance(),next,true);
        EnvironmentProfile.pigment(overviewWaterMaterial.getDefaultInstance(),next,true);
        overviewWaterMaterial.getDefaultInstance().setParameter("waterTint",next.waterR,next.waterG,next.waterB);
        EnvironmentProfile.pigment(vegetationMaterial,next,true);
        for(Proxy p:objects.values())p.updateSeason();
    }
    private void acceptMeshes(MeshResult result){
        if(released)return;terrainVisibilityDirty=true;
        android.util.Log.i("Sanguo3D","Mesh owner delivery waitWallMs="+(System.nanoTime()-result.completedNanos)/1e6+" generation="+generation+" cpuChunks="+result.ground.size()+" phase="+(result.trees==null?"GROUND":"SCENERY_OR_FINAL")+" discarded="+meshWork.discarded()+" delivered="+meshWork.delivered()+" backpressureWallMs="+meshWork.backpressureNanos()/1e6);
        // CPU mailbox delivery must not bypass renderer backpressure.
        // Keep the old backdrop alive until an admitted frame uploads its replacement.
        backdropSource=result.scenery;
        backdropSurface=result.surface;
        // Keep a displayed old level until its replacement finishes the bounded upload.
        // Environment meshes transfer in loadVisible only after replacement upload.
        chunks=result.ground;distantTerrain=!chunks.isEmpty()&&chunks.stream().allMatch(m->m.terrainLod==2);
        if(result.trees!=null)woods=result.trees;clampCamera();refreshPendingMeshes();
    }
    void setTargets(Set<Hex> value){targets=value==null?Collections.emptySet():new HashSet<>(value);overlay.invalidate();}
    void setRoute(MarchOrders.Plan value){route=value;overlay.invalidate();}
    void pauseEffects(boolean value){if(value&&!effectsPaused)pausedEffectTick=animationTick;effectsPaused=value;}
    void replay(TurnJournal.Event e,float fraction){
        if(replay!=e)assetSyncPending=true;
        replay=e;replayFraction=CombatVisual.fraction(fraction);if(e==null)clearEffects();overlay.invalidate();schedule();
    }
    boolean visible(TurnJournal.Event e){if(visible(e.start)||visible(e.target))return true;for(Hex h:e.path)if(visible(h))return true;for(TurnJournal.Impact i:e.impacts)if(visible(i.hex))return true;return false;}
    private boolean visible(Hex h){if(h==null||snapshot==null)return false;GridWorldTransform g=snapshot.ground.grid;float y=snapshot.ground.surface.at(h);return Math.abs(camera.screenX(g.x(h),g.z(h),y)-camera.width/2f)<camera.width*.6&&Math.abs(camera.screenY(g.x(h),g.z(h),y)-camera.height/2f)<camera.height*.6;}
    void diagnostics(boolean value){diagnostics=value;overlay.invalidate();}
    String startupReport(){return "first_submit_wall_ms="+firstSubmittedMillis+" first_verified_wall_ms="+firstVerifiedMillis+" resume_verified_wall_ms="+resumeVerifiedMillis+" source="+BuildConfig.SOURCE_REVISION+" profile="+(BuildConfig.UNITY_ENABLED?"unity-opt-in":"native")
        +" javaHeapUsedBytes="+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())+" javaHeapLimitBytes="+Runtime.getRuntime().maxMemory()+" nativeHeapAllocatedBytes="+android.os.Debug.getNativeHeapAllocatedSize()
        +" device="+android.os.Build.MODEL+" api="+android.os.Build.VERSION.SDK_INT+" abi="+java.util.Arrays.toString(android.os.Build.SUPPORTED_ABIS)
        +"\nsnapshot="+(snapshot!=null)+" surface="+(swap!=null)+" viewport="+bufferWidth+"x"+bufferHeight
        +" session="+(sceneToken==null?"editor":sceneToken.sessionId+":"+sceneToken.generation+":"+sceneToken.revision)+" assetRevision=1.56.0/R06"
        +" asset_pending="+assetWork.pending()+" asset_cpu_bytes="+assetWork.bytes()+" terrain_all_coarse="+distantTerrain+" mapVisualKey="+(snapshot==null?"none":snapshot.ground.mapSeed+":"+snapshot.ground.surface.overrides.hashCode())
        +" environmentCpuChunks="+woods.size()+" environmentMode=source-alpha-paint-or-legacy-merged meshGeneration="+generation+" cpuChunks="+chunks.size()+" pending="+pending+" submitted="+surfaceFrames
        +" terrainMaterialLod="+(overviewTerrain?"OVERVIEW":"DETAIL")+" lastAdmittedMaterialBinds="+lastMaterialBinds+" shadows="+environmentShadows+" shadowFar=380 hazeOpaqueCap=0.08"
        +" season="+(season==null?"none":season.name)+" seasonUpdates="+seasonUpdates+" artProfile="+SeasonStyle.ID+" worldMonth="+(snapshot==null?0:snapshot.month)
        +" gridForce="+(snapshot==null?-1:snapshot.ground.gridForce)+" gridDifficultMarch="+(snapshot!=null&&snapshot.ground.gridDifficultMarch)
        +" gridGpuChunks="+gridMeshes.size()+" gridUploads="+gridUploads+" gridMode=depth-tested-batches"
        +" overlayDraws="+overlay.draws+" territoryBuilds="+overlay.territoryBuilds+" territoryBuildMs="+overlay.territoryBuildNanos/1e6
        +" meshUploads="+lastMeshUploads+" meshUploadCpuMs="+lastMeshUploadNanos/1e6+" meshUploadMax=8 meshUploadBudgetMs="+(MESH_UPLOAD_BUDGET_NANOS/1e6)
        +" workerDeliveries="+meshWork.delivered()+" workerBackpressureWallMs="+meshWork.backpressureNanos()/1e6
        +" frameCallbacks="+frameCallbacks+" beginAttempts="+beginAttempts+" beginSkipped="+beginSkipped+" gpuPreparationFrames="+gpuPreparationFrames+" lifetimeSubmissions="+renderedFrames+" surfaceCopies="+outputCopies
        +" output="+outputStatus+"\ncamera="+camera.x+","+camera.z+" span="+camera.span+" tilt="+camera.tilt+" facing="+camera.facing+" yaw="+camera.yaw+"\npick="+lastPick;}
    String report(){return WindowSurfaceRecovery.report(this)+" | landscape="+LandscapeProfile.ID+"\n"+startupReport()+"\nFilament 1.56.0 / OpenGL ES · "+quality.label+" color="+(srgbSwapChain?"sRGB framebuffer":"post-process gamma")+" MSAA="+(msaaEnabled?"4x":"off / compatibility")+" thermal="+thermalStatus+" cap="+thermal.fps(quality)+"\n内部 "+bufferWidth+" × "+bufferHeight+" / UI "+camera.width+" × "+camera.height+" · chunks "+visibleChunks+" / GPU "+terrain.size()+" · objects "+visibleObjects+"\n帧回调间隔 "+String.format(java.util.Locale.ROOT,"%.1f",callbackMillis)+" ms（非 GPU/FPS 实测）\n待装载 "+pending+" · S06 战斗特效 / 部队 · 林块 "+visibleWood+" · LOD "+siteLod+" · 资产回退 "+missingAssets.size()+" · 特效 "+combat.count+"/"+CombatVisual.CAPACITY+"\n"+resourceReport()+"\n"+frameMetrics.summary()+" visibility_passes="+terrainVisibilityPasses+" visibility_skips="+terrainVisibilitySkips+" thermal_transitions="+thermal.transitions;}
    void resetMetrics(){cpuCount=cpuCursor=0;}
    private String resourceReport(){
        int primitives=0,triangles=0;long bufferBytes=0;
        long pcBytes=0;for(long size:pcTextureSizes.values())pcBytes+=size;
        Set<GpuMesh> resident=new HashSet<>(shapes.values());resident.addAll(terrain.values());resident.addAll(gridMeshes.values());if(backdrop!=null)resident.add(backdrop);resident.addAll(vegetation.values());for(GpuMesh effect:effectMeshes)if(effect!=null)resident.add(effect);
        for(GpuMesh parent:new ArrayList<>(resident))if(parent.waterChild!=null)resident.add(parent.waterChild);
        for(GpuMesh m:resident){bufferBytes+=(long)m.source.vertices.length*4+MeshIndexBuffer.bytes(m.source.vertices.length/7,m.source.indices.length)+(m.source.uv==null?0:(long)m.source.uv.length*4)+(m.source.surfaceData==null?0:(long)m.source.surfaceData.length*4)+(m.source.tangents==null?0:(long)m.source.tangents.length*4);if(m.shown){primitives+=m.source.landIndexCount>0&&m.source.landIndexCount<m.source.indices.length?2:1;triangles+=m.source.indices.length/3;
            // Original outline reuses land buffers but submits another primitive.
            // Count its triangles without double-counting shared geometry bytes.
            if(m.source.pcGround&&m.source.landIndexCount>0&&pcGroundOutlineMaterial!=null){primitives++;triangles+=m.source.landIndexCount/3;}
        }}
        for(Proxy p:objects.values())if(p.shown){primitives+=(p.shape.source.pcUnit||p.shape.source.pcFacilityRig)&&p.shape.source.pcUnitOpaqueIndices>0&&p.shape.source.pcUnitOpaqueIndices<p.shape.source.indices.length?2:1;triangles+=p.shape.source.indices.length/3*p.memberCount;for(GpuMesh m:new GpuMesh[]{p.flagShape,p.baseShape,p.stateShape})if(m!=null){primitives++;triangles+=m.source.indices.length/3;}}
        for(int i=0;i<effectEntities.length;i++)if(effectShown[i]){primitives++;triangles+=effectMeshes[effectKinds[i]].source.indices.length/3;}
        int entities=resident.size()+(cameraEntity==0?0:1)+(light==0?0:1),instances=vegetationMaterial==null?0:1;
        for(GpuMesh m:resident)if(m.waterInstance!=null)instances++;
        for(Proxy p:objects.values()){entities+=1+(p.flag==0?0:1)+(p.base==0?0:1)+(p.state==0?0:1);if(p.instance!=null)instances++;if(p.alphaInstance!=null)instances++;}
        for(int entity:effectEntities)if(entity!=0)entities++;
        long[] times=Arrays.copyOf(cpuSamples,cpuCount);Arrays.sort(times);
        return "场景 primitives="+primitives+" triangles="+triangles+" buffers_bytes="+bufferBytes+" pose_cache="+shapes.size()+
            " entity_live="+entities+" material_instance_live="+instances+" mesh_live="+resident.size()+" pc_water_mode="+(pcWaterMaterial==null?"legacy":"native-coarse4844")+" pc_water_clock_owners="+pcWaterClocks.size()+" texture_live="+(groundTextures.size()+pcTextureSizes.size()+(siteAtlas==null?0:1)+(fieldAtlas==null?0:1)+(unitAtlas==null?0:1)+(sceneryAtlas==null?0:1))+
            " unit_geometry=shared-single-member unit_draw=GPU-instanced unit_pose=CPU-FCVD-skinned-or-legacy-cached unit_lod="+unitLod+" pc_static_shader=source-paint4806/modulate2x-twice/reference-pending pc_light_month="+pcLightMonth+" material_live="+((pcWaterMaterial==null?0:1)+(pcUnitMaterial==null?0:1)+(pcUnitAlphaMaterial==null?0:1)+(pcSceneryMaterial==null?0:1)+(pcGroundMaterial==null?0:1)+(unitMaterial==null?0:1)+(material==null?0:1)+(groundMaterial==null?0:1)+(waterMaterial==null?0:1)+(siteMaterial==null?0:1)+(sceneryMaterial==null?0:1)+(overviewGroundMaterial==null?0:1)+(overviewWaterMaterial==null?0:1))+
            " worker_pending="+meshWork.pending()+" worker_waiting="+meshWork.waiting()+" discarded="+meshWork.discarded()+
            " ground_load_cpu_ms="+groundLoadCpuNanos/1e6+" ground_texture_estimate_bytes="+groundTextureBytes+" ground_uploads="+groundTextures.size()+" ground_fragment_samples="+(pcGroundMaterial!=null?11:overviewTerrain?8:quality==SceneQuality.LOW?8:12)+
            " pc_ground_vertex_samples="+(pcGroundMaterial!=null?2:0)+" pc_ground_outline="+(pcGroundOutlineMaterial!=null)+
            " source_effects="+(snapshot!=null&&snapshot.ground.pcMap!=null?"source-SEFF-map-quads/source-critical-first/other-combat-pending":"legacy-compatible")+(pcMapEffects==null?" pc_map_fx=closed":pcMapEffects.report())+(pcPresentations==null?" pc_presentation=closed":pcPresentations.report())+(presentationStage==null?"":presentationStage.report())+" pc_map="+(snapshot!=null&&snapshot.ground.pcMap!=null)+" pc_scenery="+(snapshot!=null&&snapshot.ground.pcMap!=null?"source-943-objects":"legacy-compatible")+" pc_sites="+(snapshot!=null&&snapshot.ground.pcMap!=null?"source":"legacy-compatible")+" pc_units="+(snapshot!=null&&snapshot.ground.pcMap!=null?"source-rig-14-models-75-clips/event-mapping-provisional":"legacy-compatible")+" pc_season="+pcSeason+" pc_texture_bytes="+pcBytes+
            " frame_queued="+queued+" texture_estimate_bytes="+(textureBytes+pcBytes)+" "+textureFormat+" mip_upload_cpu_ms="+textureUploadCpuNanos/1e6+" CPU提交ms P50/P95/P99="+percentile(times,.50)+"/"+percentile(times,.95)+"/"+percentile(times,.99)+" samples="+cpuCount+"（非驱动DrawCall/GPU帧时）";
    }
    private static String percentile(long[] times,double p){return times.length==0?"N/A":String.format(java.util.Locale.ROOT,"%.2f",times[Math.min(times.length-1,(int)Math.ceil(times.length*p)-1)]/1e6);}
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
    void resume(boolean value){meshWork.owner();if(value&&!resumed){resumeWallMillis=android.os.SystemClock.elapsedRealtime();resumeVerifiedMillis=-1;outputVerified=false;outputStatus="RESUME_WAITING_SURFACE";lastOutputProbe=0;}resumed=value;if(value)schedule();else {cancelInteraction();clearEffects();cancelFrame();}}
    private void cancelFrame(){Choreographer.getInstance().removeFrameCallback(this);queued=false;lastFrame=0;waterLastTick=0;pacer.reset();}
    private void schedule(){if(!released&&resumed&&swap!=null&&!queued){queued=true;Choreographer.getInstance().postFrameCallback(this);}}
    @Override public void surfaceCreated(SurfaceHolder holder){if(released||swap!=null)return;try{outputVerified=false;outputStatus="WAITING_FRAME";surfaceFrames=0;uniformOutputCount=0;lastOutputProbe=0;swap=engine.createSwapChain(holder.getSurface(),srgbSwapChain?SwapChainFlags.CONFIG_SRGB_COLORSPACE:SwapChainFlags.CONFIG_DEFAULT);displayHelper.attach(renderer,surface.getDisplay());schedule();}catch(RuntimeException|LinkageError e){failure.accept(e);}}
    @Override public void surfaceChanged(SurfaceHolder h,int f,int w,int height){
        if(released)return;
        if((w!=bufferWidth||height!=bufferHeight)&&presentationStage!=null){
            // Battle-log layout, rotation and thermal resolution can resize
            // the actual buffer after a command starts. Never stretch a stale
            // captured map or continue its clock before the new raster is ready.
            clearPresentationFence();presentationStage.invalidate();
            submittedPresentationCue=null;submittedPresentationPhase=Float.NaN;
        }
        bufferWidth=w;bufferHeight=height;view.setViewport(new Viewport(0,0,w,height));com.google.android.filament.android.FilamentHelper.synchronizePendingFrames(engine);schedule();
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder){cancelFrame();if(displayHelper!=null)displayHelper.detach();if(engine!=null&&swap!=null){engine.destroySwapChain(swap);swap=null;engine.flushAndWait();}}
    @Override public void doFrame(long time){
        frameCallbacks++;queued=false;if(released||!resumed||swap==null)return;
        if(thermal.tick(System.nanoTime())){terrainVisibilityDirty=true;pacer.reset();resizeSurface();}
        if(!pacer.due(time,thermal.fps(quality))){schedule();return;}
        // Finish already queued source-map commands before the screen clock.
        // Public Filament Fence signals driver processing, not GPU completion;
        // do not keep filling its queue with more copies of the same map view.
        if(presentationFence!=null&&!presentationDriverReady&&!presentationReady()){presentationFenceWaits++;renderer.skipFrame(time);schedule();return;}
        long cpuStart=System.nanoTime(),threadStart=android.os.Debug.threadCpuTimeNanos(),beginWall=0,renderWall=0;boolean admitted=false;
        android.os.Trace.beginSection("R16.ownerFrame");
        lastMeshUploads=0;lastMeshUploadNanos=0;
        try{
            // Bounded CPU delivery is independent of GPU admission. A rejected frame
            // must not strand the producer on its one-slot result mailbox.
            meshWork.drain(this::acceptMeshes,e->{pending=0;failure.accept(e);});
            if(released)return;
            if(assetWork.drain())assetSyncPending=true;
            refreshPendingMeshes();
            requestCameraWindow();
            if(lastFrame!=0)callbackMillis=(time-lastFrame)/1e6;lastFrame=time;
            boolean begun=false;
            if(bufferWidth>0&&bufferHeight>0){beginAttempts++;long start=System.nanoTime();begun=renderer.beginFrame(swap,time);beginWall=System.nanoTime()-start;admitted=begun;if(!begun)beginSkipped++;}
            if(begun){
                boolean contentRendered=false;
                try{
                    // Filament's beginFrame(false) is backpressure, not permission
                    // to enqueue uploads/transforms and only skip render().
                    gpuPreparationFrames++;assetUploadBudget=2;
                    boolean screenStage=presentationCue!=null&&presentationStage!=null&&presentationStage.ready()&&pcPresentations!=null&&pcPresentations.ready();
                    if(!screenStage){if(snapshot!=null)applySeason(SeasonStyle.forMonth(snapshot.month));selectObjectLods();if(assetSyncPending||!objectVisibilityStamp.matches(camera))syncObjects();}
                    double aspect=camera.width/(double)camera.height;
                    if(camera.perspective)lens.setCustomProjection(camera.projection(camera.nearPlane(),camera.farPlane()),camera.nearPlane(),camera.farPlane());
                    else lens.setProjection(Camera.Projection.ORTHO,-camera.span*aspect,camera.span*aspect,-camera.span,camera.span,.1,1000);
                    double eyeDistance=camera.eyeDistance();
                    lens.lookAt(camera.x+camera.backX()*eyeDistance*camera.cos(),camera.targetHeight()+eyeDistance*camera.sin(),camera.z+camera.rightX()*eyeDistance*camera.cos(),camera.x,camera.targetHeight(),camera.z,0,1,0);
                    if(!screenStage){
                    syncPcGroundFog();
                    float waterDelta=waterLastTick!=0?(float)Math.min(.1,(time-waterLastTick)/1e9):0f;
                    if(UiMotion.enabled()&&!effectsPaused&&presentationCue==null)waterSeconds+=waterDelta;
                    if(vegetationMaterial!=null)vegetationMaterial.setParameter("flowTime",(float)(waterSeconds%4096));
                    waterLastTick=time;waterMaterial.getDefaultInstance().setParameter("waveTime",(float)(waterSeconds%4096));
                    animationTick=time/1_000_000;loadVisible();
                    // The screen prelude holds its underlying command at zero.
                    // Keep that map pose intact instead of requesting new idle
                    // meshes every frame while waiting for the source layers.
                    if(presentationCue==null){animateReplay();animatePcWater(UiMotion.enabled()&&!effectsPaused?(int)(waterDelta*1000.0):0);animateUnits();animateFacilityRigs();animateEffects();}
                    // Keep the existing source map layers beneath the short
                    // screen cue; suspend their independent visual VM clock.
                    if(presentationCue==null)animatePcMapEffects(UiMotion.enabled()&&!effectsPaused?waterDelta:0);
                    }
                    if(snapshot!=null&&snapshot.ground.pcMap!=null){
                        // A normal map has no screen presentation to prepare. The
                        // committed cue owns its first decode/upload; playback still
                        // waits for the original assets, capture and driver barriers.
                        if(presentationCue!=null&&pcPresentations==null){try{presentationStage=new PcPresentationStage(getContext(),engine,srgbSwapChain);}catch(java.io.IOException error){throw new IllegalStateException(error);}pcPresentations=new PcPresentations(getContext(),engine,presentationStage.scene);if(presentationStageObserver!=null)presentationStageObserver.accept(presentationStage);}
                        if(pcPresentations!=null){presentationStage.camera(lens,camera.perspective);
                            pcPresentations.set(presentationCue,presentationPhase);pcPresentations.frame(presentationStage.camera);}
                    }else closePcPresentations();
                    long renderStart=System.nanoTime();
                    if(screenStage){
                        presentationStage.render(renderer,bufferWidth,bufferHeight);contentRendered=true;
                        if(pcPresentations.submitted(presentationCue,presentationPhase)){
                            submittedPresentationCue=presentationCue;submittedPresentationPhase=presentationPhase;
                        }
                    }
                    else if(!loadingCovered||(pending==0&&!assetSyncPending&&assetWork.pending()==0)){
                        // The Android loading curtain covers this Surface. Drain admitted uploads
                        // without repeatedly shading an incomplete national scene behind it.
                        // Full-quality rendering resumes before actual pixel verification.
                        renderer.render(view);contentRendered=true;
                        if(presentationStage!=null){presentationStage.warm(renderer);
                            if(presentationCue!=null&&pcPresentations.ready()&&pending==0&&!assetSyncPending&&assetWork.pending()==0)presentationStage.capture(renderer,surface,bufferWidth,bufferHeight);}
                    }
                    renderWall=System.nanoTime()-renderStart;
                    if(contentRendered&&pcPresentations!=null)pcPresentations.afterRender();
                }finally{renderer.endFrame();}
                if(contentRendered){
                    renderedFrames++;surfaceFrames++;
                    if(firstSubmittedMillis<0){firstSubmittedMillis=android.os.SystemClock.elapsedRealtime()-createdWallMillis;}
                    if(surfaceFrames==1)android.util.Log.i("Sanguo3D","First submission (not visibility proof): "+startupReport());
                    checkSurfaceOutput();
                }
            }
            long now=android.os.SystemClock.uptimeMillis();
            if(!outputVerified&&now-lastWorkLog>=1000){lastWorkLog=now;android.util.Log.i("Sanguo3D","Load progress "+startupReport());}
            // HWUI labels/progress and the next opportunity remain live even when
            // the separate Filament Surface cannot accept another frame.
            // Keep Android's Window/Surface composition live independently of
            // native admission, including short original screen presentations.
            overlay.invalidate();WindowSurfaceRecovery.changed(this);schedule();
            cpuSamples[cpuCursor++%cpuSamples.length]=System.nanoTime()-cpuStart;cpuCount=Math.min(cpuSamples.length,cpuCount+1);
        }catch(RuntimeException|LinkageError|OutOfMemoryError e){cancelFrame();failure.accept(e);}
        finally{frameMetrics.record(System.nanoTime()-cpuStart,android.os.Debug.threadCpuTimeNanos()-threadStart,beginWall,lastMeshUploadNanos,renderWall,admitted);android.os.Trace.endSection();}
    }
    String frameSamples(){return frameMetrics.csv();}
    private void animatePcMapEffects(float dt){
        if(snapshot==null||snapshot.ground.pcMap==null){closePcMapEffects();return;}
        // Source startup shares no work with the initial terrain/texture/pose
        // upload burst. The normal map becomes usable before its native visual
        // scene starts; deadlines stay bounded instead of competing with GC.
        if(pcMapEffects==null&&(pending!=0||assetSyncPending||!outputVerified))return;
        if(pcMapEffects==null)pcMapEffects=new PcMapEffects(getContext(),engine,scene);
        // Source441b80 perspective normalizes every coefficient by far. This
        // preserves clip ratios while abs(w)=view distance/far feeds4420a0's
        // strict0..1 depth queue. Orthographic w=1 cannot drive that map queue.
        float[] sourceCamera=PcEffectCoordinates.camera(lens.getViewMatrix(new float[16]),lens.getProjectionMatrix(new double[16]),snapshot.ground.sourceOriginX,snapshot.ground.sourceOriginY,camera.perspective?1/camera.farPlane():1);
        pcMapEffects.frame(dt,sourceCamera,snapshot.ground.sourceOriginX,snapshot.ground.sourceOriginY);
    }
    private void closePcMapEffects(){if(pcMapEffects!=null){pcMapEffects.close();pcMapEffects=null;}}
    private void clearPresentationFence(){if(presentationFence!=null&&engine!=null)engine.destroyFence(presentationFence);presentationFence=null;presentationDriverReady=false;}
    private void closePcPresentations(){clearPresentationFence();if(pcPresentations!=null){pcPresentations.close();pcPresentations=null;}if(presentationStage!=null){presentationStage.close();presentationStage=null;}presentationCue=null;presentationPhase=0;submittedPresentationCue=null;submittedPresentationPhase=Float.NaN;}
    boolean presentationSubmitted(){return presentationCue!=null&&submittedPresentationCue==presentationCue&&Math.abs(submittedPresentationPhase-presentationPhase)<.0001f;}
    boolean presentationReady(){
        if(presentationCue==null)return pending==0&&!assetSyncPending&&assetWork.pending()==0;
        if(pcPresentations==null||!pcPresentations.ready()||pending!=0||assetSyncPending||assetWork.pending()!=0)return false;
        if(presentationStage==null||!presentationStage.ready())return false;
        if(presentationDriverReady)return true;
        if(presentationFence==null){presentationFence=engine.createFence();engine.flush();return false;}
        Fence.FenceStatus status=presentationFence.wait(Fence.Mode.DONT_FLUSH,0);
        if(status==Fence.FenceStatus.ERROR)throw new IllegalStateException("Original presentation preparation driver fence");
        if(status!=Fence.FenceStatus.CONDITION_SATISFIED)return false;
        engine.destroyFence(presentationFence);presentationFence=null;presentationDriverReady=true;return true;
    }
    void presentation(PcPresentationPlan.Cue cue,float phase){
        if(cue!=presentationCue||phase<presentationPhase){submittedPresentationCue=null;submittedPresentationPhase=Float.NaN;}
        if((presentationCue==null)!=(cue==null)){clearPresentationFence();if(presentationStage!=null)presentationStage.invalidate();overlay.invalidate();WindowSurfaceRecovery.changed(this);}
        presentationCue=cue;presentationPhase=phase;
    }
    private void animatePcWater(int milliseconds){
        pcWaterAdvanced.clear();
        for(GpuMesh parent:terrain.values()){
            advancePcWater(parent.waterChild,milliseconds);
        }
        if(backdrop!=null)advancePcWater(backdrop.waterChild,milliseconds);
    }
    private void advancePcWater(GpuMesh water,int milliseconds){
        if(water==null||!water.shown)return;
        String key=water.waterKey;long clock=pcWaterClocks.getOrDefault(key,0L);
        if(pcWaterAdvanced.add(key)){clock+=milliseconds;pcWaterClocks.put(key,clock);}
        water.waterInstance.setParameter("clockMS",(int)clock,(int)(clock>>>32));
    }
    /** Check actual display output once uploads settle. Two real submissions permit
     * PixelCopy to prove visibility; an arbitrary 20-frame wait added seconds under
     * backpressure. All current-generation, surface-valid and nonuniform pixel gates remain. Driver failures can return no Java error.
     * Only repeated, virtually identical extreme pixels trigger the 3D error recovery. */
    private void checkSurfaceOutput(){
        long now=android.os.SystemClock.uptimeMillis();
        if(outputVerified||outputProbePending||surfaceFrames<2||pending!=0||visibleChunks==0
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
                    outputCopies++;
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
                    }else{uniformOutputCount=0;outputVerified=true;if(firstVerifiedMillis<0)firstVerifiedMillis=android.os.SystemClock.elapsedRealtime()-createdWallMillis;if(resumeWallMillis>=0)resumeVerifiedMillis=android.os.SystemClock.elapsedRealtime()-resumeWallMillis;outputStatus="CONTENT_DETECTED_NOT_ART_ACCEPTANCE";android.util.Log.i("Sanguo3D",startupReport());
                        Runnable listener=verifiedOutputListener;verifiedOutputListener=null;if(listener!=null)listener.run();}
                }finally{sample.recycle();}
            },new android.os.Handler(android.os.Looper.getMainLooper()));
        }catch(IllegalArgumentException e){outputProbePending=false;outputStatus="COPY_UNAVAILABLE";uniformOutputCount=0;sample.recycle();}
    }
    private boolean inView(float x,float z,float radius){return Math.abs(x-camera.x)<camera.extentX()+radius+2&&Math.abs(z-camera.z)<camera.extentZ()+radius+2;}
    /** Count real visible CPU results not resident on the GPU, even when frame
     * admission is rejected. No GPU writes, synthetic READY or ignored mailbox. */
    private void refreshPendingMeshes(){
        if(!terrainVisibilityDirty&&visibilityStamp.matches(camera)&&meshWork.pending()==0)return;
        int remaining=meshWork.pending();
        if(snapshot!=null){
            boolean wanted=TerrainMaterialLod.select(overviewTerrain,camera.span);
            if(backdropSource!=null&&(backdrop==null||backdrop.source!=backdropSource||backdrop.overview!=wanted))remaining++;
            for(SceneMesh chunk:chunks)if(inView(chunk.x,chunk.z,chunk.radius)){
                GpuMesh gpu=terrain.get(chunk);if(gpu==null||gpu.overview!=wanted)remaining++;
                if(gridShown&&!editorGrid&&camera.span<48&&chunk.gridMatches(snapshot.ground)&&chunk.grid.indices.length>0&&!gridMeshes.containsKey(chunk))remaining++;
            }
            for(SceneMesh source:woods){
                SceneMesh chunk=quality!=SceneQuality.LOW&&camera.span<14?source:source.distant;
                if(chunk.indices.length>0&&inView(chunk.x,chunk.z,chunk.radius)&&!vegetation.containsKey(chunk))remaining++;
            }
        }
        pending=remaining;
    }
    /** Resolve the camera LOD before the first asset request, including after a
     * view/quality change. Otherwise national previews decode the default middle
     * LOD first and immediately enqueue its replacement on the same owner frame. */
    private void selectObjectLods(){
        int nextSite=Math.max(quality.minSiteLod,SiteVisual.lod(camera.span,siteLod));
        int nextUnit=UnitLod.select(unitLod,camera.span,quality.minUnitLod);
        if(nextSite!=siteLod||nextUnit!=unitLod){
            siteLod=nextSite;unitLod=nextUnit;assetSyncPending=true;
        }
    }
    /** CPU requests keep up with input even while Filament rejects GPU frames.
     * Results still use the same one-slot mailbox/generation and bounded worker. */
    private void requestCameraWindow(){
        if(snapshot==null||released)return;
        // A focus jump or zoom can invalidate an in-flight national window.
        // Replace that request through the existing epoch/cancellation boundary
        // instead of waiting for geometry the camera can no longer see.
        if(terrainWindow==null||!terrainWindow.covers(camera.x,camera.z,camera.extentX(),camera.extentZ(),camera.span)){
            terrainWindow=new SceneMesh.TerrainWindow(camera.x,camera.z,camera.extentX(),camera.extentZ(),camera.span);
            SceneMesh.TerrainWindow requested=terrainWindow;MapSceneSnapshot.Ground ground=snapshot.ground;
            List<SceneMesh> previous=SceneMesh.windowCoverage(chunks,requested),trees=woods;SceneMesh scenery=backdropSource;
            Set<Hex> excluded=woodExcluded;FieldAssets assets=fieldAssets;PcScenery nativeAssets=pcScenery;PcCliffWalls nativeWalls=pcCliffWalls;int month=snapshot.month;
            boolean changed=backdropSurface!=ground.surface;
            long queuedAt=System.nanoTime();
            meshWork.submitPhased(publish->buildMeshes(ground,changed,scenery,previous,trees,assets,nativeAssets,nativeWalls,month,excluded,requested,queuedAt,publish));pending=meshWork.pending();
        }
    }
    private void loadVisible(){
        if(snapshot==null)return;
        overviewTerrain=TerrainMaterialLod.select(overviewTerrain,camera.span);
        overviewWaterMaterial.getDefaultInstance().setParameter("waveTime",(float)(waterSeconds%4096));
        lastMaterialBinds=0;
        if(terrainVisibilityDirty||!visibilityStamp.matches(camera)||meshWork.pending()!=0){
        terrainVisibilityPasses++;
        // One shared bound for new GPU meshes and material switches. A single driver
        // operation is non-preemptible; no meshes/textures are recreated on an LOD switch.
        int budget=8;long uploadNanos=0;int uploads=0;
        if(backdrop!=null&&backdrop.source==backdropSource&&backdrop.overview!=overviewTerrain){
            long started=System.nanoTime();backdrop.bindTerrainMaterial();uploadNanos+=System.nanoTime()-started;budget--;lastMaterialBinds++;
        }
        if(backdropSource!=null&&(backdrop==null||backdrop.source!=backdropSource)){
            long started=System.nanoTime();GpuMesh replacement=new GpuMesh(backdropSource);replacement.show(true);
            GpuMesh old=backdrop;backdrop=replacement;if(old!=null)old.destroy();
            uploadNanos+=System.nanoTime()-started;budget--;uploads++;
        }
        engine.getLightManager().setShadowCaster(engine.getLightManager().getInstance(light),environmentShadows&&!thermal.constrained&&camera.span<22);
        visibleChunks=0;pending=meshWork.pending();
        // Bound upload work by measured owner CPU time and count. Charge only
        // actual uploads, so scanning existing residents cannot starve the queue.
        // A single non-preemptible upload may exceed the time budget.
        Set<SceneMesh> active=activeTerrain;active.clear();for(SceneMesh m:chunks)active.add(m);

        for(SceneMesh source:chunks){
            SceneMesh chunk=source;
            boolean shown=inView(chunk.x,chunk.z,chunk.radius);GpuMesh gpu=terrain.get(chunk);
            if(shown){visibleChunks++;
                if(gpu==null||gpu.overview!=overviewTerrain){
                    if(budget>0&&uploadNanos<MESH_UPLOAD_BUDGET_NANOS){
                        long started=System.nanoTime();
                        if(gpu==null){gpu=new GpuMesh(chunk);terrain.put(chunk,gpu);uploads++;}
                        else {gpu.bindTerrainMaterial();lastMaterialBinds++;}
                        uploadNanos+=System.nanoTime()-started;budget--;
                    }else pending++;
                }
            }
            if(gpu!=null){gpu.show(shown);if(!shown&&!inView(chunk.x,chunk.z,chunk.radius+20)){gpu.destroy();terrain.remove(chunk);}}
        }
        for(SceneMesh old:new ArrayList<>(terrain.keySet()))if(!active.contains(old)){
            SceneMesh replacement=null;for(SceneMesh m:chunks)if(m.chunkQ==old.chunkQ&&m.chunkR==old.chunkR){replacement=m;break;}
            if(replacement==null||terrain.containsKey(replacement)){terrain.remove(old).destroy();}
            else terrain.get(old).show(inView(old.x,old.z,old.radius));
        }
        visibleWood=0;
        wantedWood.clear();
        for(SceneMesh source:woods){
            SceneMesh chunk=quality!=SceneQuality.LOW&&camera.span<14?source:source.distant;if(chunk.indices.length==0)continue;
            if(inView(chunk.x,chunk.z,chunk.radius)){wantedWood.add(chunk);visibleWood++;GpuMesh gpu=vegetation.get(chunk);
                if(gpu==null){if(budget>0&&uploadNanos<MESH_UPLOAD_BUDGET_NANOS){long started=System.nanoTime();gpu=new GpuMesh(chunk);gpu.build(gpu.entity,chunk.pcCliffWall?pcFacilitiesInstance:chunk.pcScenery?pcSceneryInstance:vegetationMaterial);vegetation.put(chunk,gpu);uploads++;uploadNanos+=System.nanoTime()-started;budget--;}else pending++;}
                if(gpu!=null)gpu.show(true);
            }
        }
        for(SceneMesh old:new ArrayList<>(vegetation.keySet()))if(!wantedWood.contains(old)){
            // A national batch may overlap four old close batches (and vice versa).
            // Retain it until every visible overlapping replacement has uploaded.
            if(!Vegetation.replacementPending(old,wantedWood,vegetation.keySet()))vegetation.remove(old).destroy();
            else vegetation.get(old).show(inView(old.x,old.z,old.radius));
        }
        // Depth-tested static lines follow the same resident terrain lifetime and
        // upload budget. A visibility toggle never rebuilds terrain or scans/raycasts the map on UI.
        for(var entry:terrain.entrySet()){
            SceneMesh source=entry.getKey();GpuMesh grid=gridMeshes.get(source);
            // Old terrain can remain while a replacement uploads; its obsolete grid
            // must disappear on this admitted frame, before CPU/GPU replacement finishes.
            boolean shown=gridShown&&!editorGrid&&camera.span<48&&entry.getValue().shown&&source.gridMatches(snapshot.ground);
            if(shown&&source.grid!=null&&source.grid.indices.length>0&&grid==null){
                if(budget>0&&uploadNanos<MESH_UPLOAD_BUDGET_NANOS){
                    long started=System.nanoTime();grid=new GpuMesh(source.grid);gridMeshes.put(source,grid);
                    uploadNanos+=System.nanoTime()-started;budget--;uploads++;gridUploads++;
                }else pending++;
            }
            if(grid!=null)grid.show(shown);
        }
        for(SceneMesh old:new ArrayList<>(gridMeshes.keySet()))if(!terrain.containsKey(old))gridMeshes.remove(old).destroy();
        lastMeshUploads=uploads;lastMeshUploadNanos=uploadNanos;
        visibilityStamp.set(camera);terrainVisibilityDirty=pending!=0;
        }else terrainVisibilitySkips++;
        // Moving units are culled every frame even when static terrain is cached.
        visibleObjects=0;
        for(Proxy p:objects.values()){boolean shown=inView(p.motion.x,p.motion.z,2);if(shown)visibleObjects++;if(shown!=p.shown){p.show(shown);}}
    }
    private static String siteLodFor(String key){return key.substring(key.lastIndexOf(':')+1).split("/",2)[0];}
    private GpuMesh shape(MapSceneSnapshot.Item item){
        String field=item.facility!=null?FieldAssets.facility(item.facility,siteLod):item.unit!=null?FieldAssets.unit(item.unit,item.unit.naval,unitLod):null;
        final MapSceneSnapshot.Ground assetGround=snapshot.ground;
        final PcUnits.Selection nativeUnit=item.unit!=null&&assetGround.pcMap!=null?pcUnits.idle(item.unit):null;
        final PcSites nativeSiteAssets=pcSites;
        final PcSites.Placement nativeSite=nativeSiteAssets.placement(assetGround,item);
        final PcFacilities nativeFacilityAssets=pcFacilities;
        final boolean nativeFacility=nativeFacilityAssets.supports(assetGround,item);
        if(assetGround.pcMap!=null&&nativeUnit==null&&nativeSite==null&&!nativeFacility){
            // An unsupported source object is a recorded restoration gap. Do not
            // load an authored mobile model as if it belonged to the PC pack.
            if(missingAssets.add("pc-unresolved:"+item.key))android.util.Log.w("Sanguo3D","Unrecovered PC object "+item.key);
            return null;
        }
        final boolean nativeRig=PcFacilityRigs.supports(assetGround,item);
        final PcFacilityRigs rigAssets=pcFacilityRigs;
        final int nativeMonth=snapshot.month,nativeLod=siteLod;
        final PcConstructibleWalls.Placement nativeWall=nativeFacility?PcConstructibleWalls.placement(assetGround,item):null;
        final int wallMask=snapshot.wallConnections.getOrDefault(item.hex,0);
        final PcDams.Placement nativeDam=nativeFacility?pcDams.placement(assetGround,item):null;
        String key=nativeSite!=null?pcSites.key(nativeSite,item.site,nativeMonth,nativeLod):nativeWall!=null?PcConstructibleWalls.key(item,nativeMonth,nativeLod,nativeWall,wallMask):nativeFacility?nativeFacilityAssets.key(item,nativeMonth,nativeLod):field!=null?field+(FieldAssets.farm(item)?":"+item.hex+":"+FieldAssets.farmSurfaceKey(assetGround,item.hex):"")+(item.unit==null?"":":idle:0:1"):item.site==null?item.kind+":"+item.color:item.site.model+":"+siteLod+(item.site.model.equals("gate")?"/"+item.hex+"/"+item.site.yaw+"/g"+generation:"");
        if(nativeRig)key=PcFacilityRigs.key(item,nativeMonth,0);
        if(nativeDam!=null)key="pc-source-dam:"+key;
        if(nativeUnit!=null)key=nativeUnit.key();
        final String assetKey=key;
        GpuMesh mesh=shapes.get(key);if(mesh!=null)return mesh;
        final FieldAssets assets=fieldAssets;final int requestedLod=unitLod;final android.content.res.AssetManager manager=getContext().getAssets();
        SceneMesh source=assetWork.request(key,()->{
            if(nativeUnit!=null)return pcUnits.pose(nativeUnit);
            if(nativeRig)return rigAssets.mesh(item,nativeMonth,0,nativeFacilityAssets);
            if(nativeSite!=null)return nativeSiteAssets.mesh(nativeSite,item.site,nativeMonth,nativeLod);
            if(nativeWall!=null)return PcConstructibleWalls.mesh(nativeFacilityAssets,assetGround,item,nativeMonth,nativeLod,nativeWall,wallMask);
            if(nativeFacility){SceneMesh body=nativeFacilityAssets.mesh(item,nativeMonth,nativeLod);body.pcDam=nativeDam!=null;return body;}
            if(field!=null){SceneMesh model=item.unit==null?assets.mesh(field):assets.pose(field,"idle",0,1);return FieldAssets.farm(item)?FieldAssets.conformFarm(model,assetGround,item.hex):model;}
            if(item.site!=null)try(java.io.InputStream in=manager.open("3d/sites/v124/"+item.site.model+"-lod"+siteLodFor(assetKey)+".glb")){SceneMesh model=SiteGlb.read(in);return item.site.model.equals("gate")?SiteVisual.joinGate(model,assetGround,item.hex,item.site.yaw):model;}
            return SceneMesh.proxy(item.kind,item.color);
        });
        if(source==null){
            String error=assetWork.error(key);if(error==null){assetSyncPending=true;return null;}
            if(assetGround.pcMap!=null)throw new IllegalStateException("Original PC asset unavailable: "+key+": "+error);
            if(missingAssets.add(key))android.util.Log.w("Sanguo3D","Asset fallback "+key+": "+error);
            source=SceneMesh.proxy(item.kind,0xffff00ff);
        }
        if(assetUploadBudget<=0){assetSyncPending=true;return null;}assetUploadBudget--;
        mesh=new GpuMesh(source);shapes.put(key,mesh);return mesh;
    }
    private void syncObjects(){
        if(engine==null||snapshot==null)return;assetSyncPending=false;Set<String> alive=new HashSet<>();
        for(MapSceneSnapshot.Item item:snapshot.items){
            // Keep a conservative camera margin for pan/zoom. The immutable snapshot
            // still contains every entity for navigation and authoritative picking.
            // An active replay additionally retains its participants and path cells.
            if(!residentObject(item))continue;
            alive.add(item.key);Proxy p=objects.get(item.key);GpuMesh geometry=shape(item);
            if(geometry==null)continue;
            if(p!=null&&((item.unit==null&&p.shape!=geometry&&(!geometry.source.pcFacilityRig||!PcFacilityRigs.key(item,snapshot.month,0).equals(p.rigRestKey)))||p.shape.source.pcUnit!=geometry.source.pcUnit||p.item.color!=item.color||!p.stateKey().equals(facilityOverlay(item,geometry.source.pcFacility)))){p.destroy();objects.remove(item.key);p=null;}
            if(!facilityOverlay(item,geometry.source.pcFacility).isEmpty()){
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
            // A decode arriving is not a new snapshot. Do not repeatedly settle
            // every existing unit or invalidate its terrain-contact cache on retries.
            if(p.item!=item||p.positionedGround!=snapshot.ground){p.positionedGround=null;p.item=item;p.motion.settle(item.hex,snapshot.ground.grid);p.position(p.motion.x,p.motion.z);p.updateDamage();}
        }
        Iterator<Map.Entry<String,Proxy>> it=objects.entrySet().iterator();while(it.hasNext()){Map.Entry<String,Proxy> e=it.next();if(!alive.contains(e.getKey())){e.getValue().destroy();it.remove();}}
        objectVisibilityStamp.set(camera);
        trimShapes();
        animateReplay();
    }
    private boolean residentObject(MapSceneSnapshot.Item item){
        GridWorldTransform grid=snapshot.ground.grid;
        if(inView(grid.x(item.hex),grid.z(item.hex),18))return true;
        if(replay==null)return false;
        if(item.unit!=null&&item.unit.id==replay.actorId)return true;
        if(item.hex.equals(replay.start)||item.hex.equals(replay.target)||replay.path.contains(item.hex))return true;
        for(TurnJournal.Impact impact:replay.impacts)if(item.hex.equals(impact.hex))return true;
        return false;
    }
    private void trimShapes(){
        if(shapes.size()<=quality.poseCache)return;
        Set<GpuMesh> used=new HashSet<>();for(Proxy p:objects.values()){used.add(p.shape);if(p.flagShape!=null)used.add(p.flagShape);if(p.baseShape!=null)used.add(p.baseShape);if(p.stateShape!=null)used.add(p.stateShape);}
        Iterator<Map.Entry<String,GpuMesh>> it=shapes.entrySet().iterator();
        while(it.hasNext()&&shapes.size()>quality.poseCache){GpuMesh m=it.next().getValue();if(!used.contains(m)&&m.references==0){m.destroy();it.remove();}}
    }
    private String facilityOverlay(MapSceneSnapshot.Item item,boolean nativeFacility){
        // Source construction/damage bodies remain available. Authored mobile fire
        // and scaffolding are compatibility assets, never source restoration.
        if(snapshot!=null&&snapshot.ground.pcMap!=null)return "";
        return item.facility==null?"":item.facility.burning?"fire":!item.facility.complete&&!nativeFacility?"scaffold":"";
    }
    private void animateUnits(){
        if(snapshot==null)return;
        for(Proxy p:objects.values())if(p.item.unit!=null&&p.shown){
            p.animation.sample(p.item,snapshot.ground,replay,replayFraction,animationTick,unitLod);
            if(p.shape.source.pcUnit){
                PcUnits.Selection selected=pcUnits.animation(p.item.unit,p.animation,effectsPaused?pausedEffectTick:animationTick,replay,replayFraction);
                p.animation.scale=1;p.position(p.motion.x,p.motion.z);
                if(!selected.key().equals(p.poseKey)){
                    GpuMesh mesh=shapes.get(selected.key());
                    if(mesh==null){SceneMesh decoded=assetWork.request(selected.key(),()->pcUnits.pose(selected));
                        if(decoded==null){String error=assetWork.error(selected.key());if(error!=null)throw new IllegalStateException("PC unit pose: "+error);continue;}
                        if(assetUploadBudget<=0)continue;assetUploadBudget--;mesh=new GpuMesh(decoded);shapes.put(selected.key(),mesh);}
                    p.replace(mesh);p.poseKey=selected.key();
                }
                continue;
            }
            String model=FieldAssets.unit(p.item.unit,p.animation.naval,unitLod);
            // Root/contact/labels are current even while a new shared pose is being decoded.
            p.position(p.motion.x,p.motion.z);
            String key=model+":"+p.animation.clip+":"+p.animation.frame+":1";
            if(!key.equals(p.poseKey)){
                GpuMesh mesh=shapes.get(key);
                if(mesh==null){
                    final FieldAssets assets=fieldAssets;final String clip=p.animation.clip;final int frame=p.animation.frame;
                    SceneMesh decoded=assetWork.request(key,()->assets.pose(model,clip,frame,1));
                    if(decoded==null){String error=assetWork.error(key);if(error!=null&&missingAssets.add(key))android.util.Log.w("Sanguo3D","Pose fallback "+key+": "+error);continue;}
                    if(assetUploadBudget<=0)continue;assetUploadBudget--;
                    mesh=new GpuMesh(decoded);shapes.put(key,mesh);
                }
                p.replace(mesh);p.poseKey=key;
            }
        }
        trimShapes();
    }
    private void animateFacilityRigs(){
        if(snapshot==null)return;
        for(Proxy p:objects.values())if(p.shown&&p.shape.source.pcFacilityRig){
            int frame=PcFacilityRigs.frame(p.item,replay,replayFraction);
            String key=PcFacilityRigs.key(p.item,snapshot.month,frame);
            if(key.equals(p.poseKey))continue;
            GpuMesh mesh=shapes.get(key);
            if(mesh==null){
                final MapSceneSnapshot.Item item=p.item;final int month=snapshot.month;
                SceneMesh source=assetWork.request(key,()->pcFacilityRigs.mesh(item,month,frame,pcFacilities));
                if(source==null){String error=assetWork.error(key);if(error!=null)throw new IllegalStateException("PC facility pose: "+error);continue;}
                if(assetUploadBudget<=0)continue;assetUploadBudget--;mesh=new GpuMesh(source);shapes.put(key,mesh);
            }
            p.replace(mesh);p.poseKey=key;
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
        if(snapshot!=null&&snapshot.ground.pcMap!=null){hit=null;portrait=null;}
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
        // Keep the immutable gameplay journal and source unit/rig animation.
        // Unrecovered PC emitters must not fall through to mobile approximations.
        if(snapshot.ground.pcMap!=null){clearEffects();return;}
        combat.detail(quality==SceneQuality.LOW||thermal.constrained?0:quality==SceneQuality.MEDIUM?1:2);
        combat.sample(UiMotion.enabled()?replay:null,replayFraction,snapshot.ground);
        int fires=0;for(MapSceneSnapshot.FireState fire:snapshot.fires)if(visible(fire.hex)){
            if(fires++==CombatVisual.FIRE_BUDGET)break;
            combat.fire(fire,snapshot.ground,effectsPaused?pausedEffectTick:animationTick,UiMotion.enabled());
        }
        for(int i=0;i<effectEntities.length;i++){
            if(i>=combat.count){if(effectShown[i]){scene.removeEntity(effectEntities[i]);effectShown[i]=false;}continue;}
            CombatVisual.Particle p=combat.particles[i];
            if(!inView(p.x,p.z,1)){if(effectShown[i]){scene.removeEntity(effectEntities[i]);effectShown[i]=false;}continue;}
            if(effectMeshes[p.mesh]==null){
                SceneMesh source;
                if(p.mesh==2||p.mesh==6){
                    final String name=p.mesh==2?"fire-v121":"smoke-v121";final FieldAssets assets=fieldAssets;
                    source=assetWork.request("effect/"+name,()->{
                        SceneMesh baked=assets.mesh(name);
                        // Authored vertex pigment uses the existing unlit material.
                        // Explicit vertex-color display assets, not a PBR conversion.
                        return new SceneMesh(baked.vertices,baked.indices,0,0,2);
                    });
                    if(source==null){
                        String error=assetWork.error("effect/"+name);
                        if(error==null){if(effectShown[i]){scene.removeEntity(effectEntities[i]);effectShown[i]=false;}continue;}
                        if(missingAssets.add(name))android.util.Log.w("Sanguo3D","Effect fallback "+name+": "+error);
                        source=CombatVisual.mesh(p.mesh);
                    }
                }else source=CombatVisual.mesh(p.mesh);
                if(assetUploadBudget<=0){if(effectShown[i]){scene.removeEntity(effectEntities[i]);effectShown[i]=false;}continue;}
                assetUploadBudget--;effectMeshes[p.mesh]=new GpuMesh(source);
            }
            if(effectEntities[i]==0){effectEntities[i]=EntityManager.get().create();effectKinds[i]=-1;}
            if(effectKinds[i]!=p.mesh){engine.getRenderableManager().destroy(effectEntities[i]);effectMeshes[p.mesh].build(effectEntities[i]);effectKinds[i]=p.mesh;}
            float c=(float)Math.cos(p.yaw)*p.scale,s=(float)Math.sin(p.yaw)*p.scale;
            float cp=(float)Math.cos(p.pitch),sp=(float)Math.sin(p.pitch);
            Arrays.fill(effectMatrix,0);effectMatrix[0]=c;effectMatrix[2]=-s;effectMatrix[4]=-s*sp*p.stretch;effectMatrix[5]=p.scale*cp*p.stretch;effectMatrix[6]=-c*sp*p.stretch;effectMatrix[8]=s*cp;effectMatrix[9]=p.scale*sp;effectMatrix[10]=c*cp;effectMatrix[12]=p.x;effectMatrix[13]=p.y;effectMatrix[14]=p.z;effectMatrix[15]=1;
            TransformManager tm=engine.getTransformManager();tm.setTransform(tm.getInstance(effectEntities[i]),effectMatrix);
            if(!effectShown[i]){scene.addEntity(effectEntities[i]);effectShown[i]=true;}
        }
    }

    private int pickedUnitId=-1;
    private Hex pickObject(float sx,float sy){
        for(Map.Entry<String,android.graphics.RectF> label:overlay.labelHits.entrySet()){
            Proxy p=objects.get(label.getKey());if(p!=null&&p.shown&&label.getValue().contains(sx,sy)&&labelVisible(p)){lastPick="label="+p.item.key+" cell="+p.item.hex;pickedUnitId=p.item.unit==null?-1:p.item.unit.id;return p.item.hex;}
        }
        Proxy chosen=null;float best=Float.NEGATIVE_INFINITY;float foreground=snapshot.ground.surface.rayHeight(camera,sx,sy);
        for(Proxy p:objects.values())if(p.shown){
            MapSceneSnapshot.Item item=p.item;
            PcSites.Placement nativeSite=p.shape.source.pcSite?pcSites.placement(snapshot.ground,item):null;
            float yaw=nativeSite!=null?nativeSite.yaw():item.site!=null?item.site.yaw:item.facility!=null?item.facility.direction*(float)Math.PI/3:p.motion.yaw;
            float scale=nativeSite!=null?1:item.site!=null?item.site.scale:item.unit!=null?p.animation.scale:1;
            float hit=p.shape.source.pcUnit?p.pcFormation.hit(camera,p.shape.source,p.motion.x,p.motion.z,yaw,sx,sy):item.unit!=null&&p.instance!=null?p.formation.hit(camera,p.shape.source,p.motion.x,p.motion.z,yaw,scale,sx,sy):ScenePicking.hit(camera,p.shape.source,p.motion.x,p.y,p.motion.z,yaw,scale,sx,sy);
            if(!Float.isFinite(hit))continue;
            if(Float.isFinite(foreground)&&foreground>hit+.03f)continue;
            if(hit>best||(hit==best&&chosen!=null&&item.key.compareTo(chosen.item.key)<0)){best=hit;chosen=p;}
        }
        if(chosen!=null)pickedUnitId=chosen.item.unit==null?-1:chosen.item.unit.id;
        if(chosen!=null)lastPick="entity="+chosen.item.key+" cell="+chosen.item.hex+" height="+best+" display="+chosen.motion.x+","+chosen.motion.z;
        return chosen==null?null:chosen.item.hex;
    }
    void initialFocus(Hex h){pendingInitialFocus=h;}
    void focus(Hex h){if(h==null)return;if(snapshot==null){pendingInitialFocus=h;pendingFit=false;return;}camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);camera.span=10;}
    void center(Hex h){if(snapshot!=null&&h!=null){camera.x=snapshot.ground.grid.x(h);camera.z=snapshot.ground.grid.z(h);}}
    void fit(){pendingFit=true;if(snapshot==null||getWidth()==0||getHeight()==0)return;pendingFit=false;MapSceneSnapshot.Ground g=snapshot.ground;float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){Hex h=new Hex(q,r);if(g.valid(h)){float x=g.grid.x(h),z=g.grid.z(h);minX=Math.min(minX,x);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxZ=Math.max(maxZ,z);}}if(minX==Float.MAX_VALUE)return;camera.x=(minX+maxX)/2;camera.z=(minZ+maxZ)/2;float dx=maxX-minX+3,dz=maxZ-minZ+3;camera.span=(float)Math.max((dx*Math.abs(camera.rightX())+dz*Math.abs(camera.backX()))*camera.height/camera.width,(dx*Math.abs(camera.backX())+dz*Math.abs(camera.rightX()))*camera.sin())*.52f;camera.sanitize();}
    void resetOrientation(){camera.facing=1;camera.yaw=0;camera.tilt=55;clampCamera();overlay.invalidate();}
    void reverseOrientation(){camera.facing=-camera.facing;overlay.invalidate();}
    private void clampCamera(){
        camera.perspective=snapshot!=null&&snapshot.ground.pcMap!=null;
        camera.sanitize();if(snapshot!=null)camera.clampTo(snapshot.ground);
    }
    void saveCamera(Bundle b){b.putBoolean("mapNavigator",navigatorShown);b.putFloat("cameraX",camera.x*TileGeometry.DX);b.putFloat("cameraY",camera.z*TileGeometry.DY);b.putFloat("sceneSpan",camera.span);b.putFloat("sceneTilt",camera.tilt);b.putInt("sceneFacing",camera.facing);b.putFloat("sceneYaw",camera.yaw);}
    void restoreCamera(Bundle b){pendingFit=false;if(b.get("cameraScaleDp") instanceof Float&&(!b.containsKey("sceneSpan")||Boolean.FALSE.equals(b.get("sceneEnabled")))){pendingLegacyScaleDp=b.getFloat("cameraScaleDp");if(getHeight()>0){camera.span=SceneCamera.legacySpan(getHeight(),getResources().getDisplayMetrics().density,pendingLegacyScaleDp);b=new Bundle(b);b.putFloat("sceneSpan",camera.span);pendingLegacyScaleDp=Float.NaN;}}navigatorShown=b.getBoolean("mapNavigator",navigatorShown);try{camera.x=b.getFloat("cameraX")/TileGeometry.DX;camera.z=b.getFloat("cameraY")/TileGeometry.DY;camera.span=b.getFloat("sceneSpan",15);camera.tilt=b.getFloat("sceneTilt",55);camera.yaw=b.getFloat("sceneYaw",0);camera.facing=b.getInt("sceneFacing",1)<0?-1:1;camera.sanitize();clampCamera();}catch(RuntimeException bad){camera.x=0;camera.z=0;camera.span=15;camera.tilt=55;camera.yaw=0;camera.facing=1;clampCamera();}}
    void release(){
        cancelInteraction();unitDrag=null;
        meshWork.owner();
        if(released)return;released=true;verifiedOutputListener=null;pendingLayoutSnapshot=null;pendingInitialFocus=null;pendingFit=false;replay=null;animatedUnit=null;generation++;cancelFrame();meshWork.close();assetWork.close();pending=0;surface.getHolder().removeCallback(this);
        closePcMapEffects();
        closePcPresentations();presentationStageObserver=null;
        // A detached View may remain referenced by the framework or an outstanding probe.
        // Release heavyweight CPU ownership immediately, rather than waiting for View GC.
        chunks=Collections.emptyList();woods=Collections.emptyList();snapshot=null;poisonMaterialGround=null;fieldAssets=null;backdropSource=null;backdropSurface=null;
        activeTerrain.clear();wantedWood.clear();woodExcluded=Collections.emptySet();
        overlay.territoryBitmap=null;overlay.territoryGround=null;overlay.cachedColors=null;overlay.cachedBorders=null;
        overlay.siteSelectionGround=null;overlay.siteSelectionCells=Collections.emptySet();overlay.siteSelectionStamp=null;overlay.siteSelectionPath.rewind();
        territoryColors=null;targets=Collections.emptySet();editorCells=Collections.emptySet();impassable=Collections.emptySet();
        if(android.os.Build.VERSION.SDK_INT>=29&&thermalManager!=null&&thermalListener!=null){thermalManager.removeThermalStatusListener(thermalListener);thermalListener=null;}
        if(engine==null)return;
        if(displayHelper!=null)displayHelper.detach();
        if(swap!=null){engine.destroySwapChain(swap);swap=null;}
        for(Proxy p:objects.values())p.destroy();objects.clear();for(GpuMesh m:terrain.values())m.destroy();terrain.clear();for(GpuMesh m:gridMeshes.values())m.destroy();gridMeshes.clear();for(GpuMesh m:vegetation.values())m.destroy();vegetation.clear();for(GpuMesh m:shapes.values())m.destroy();shapes.clear();
        if(backdrop!=null){backdrop.destroy();backdrop=null;}
        clearEffects();for(int entity:effectEntities)if(entity!=0){engine.destroyEntity(entity);EntityManager.get().destroy(entity);}
        for(GpuMesh mesh:effectMeshes)if(mesh!=null)mesh.destroy();criticalHit=null;criticalPortrait=null;criticalSkip=null;
        if(skyLight!=null){scene.setIndirectLight(null);engine.destroyIndirectLight(skyLight);skyLight=null;}
        if(light!=0){scene.removeEntity(light);engine.destroyEntity(light);EntityManager.get().destroy(light);}
        if(pcUnitMaterial!=null)engine.destroyMaterial(pcUnitMaterial);if(pcUnitAlphaMaterial!=null)engine.destroyMaterial(pcUnitAlphaMaterial);for(int i=0;i<14;i++){destroyPcTexture(pcUnitSheets[i]);pcUnitSheets[i]=null;}
        if(pcFacilitiesInstance!=null)engine.destroyMaterialInstance(pcFacilitiesInstance);if(pcSceneryInstance!=null)engine.destroyMaterialInstance(pcSceneryInstance);if(pcSceneryMaterial!=null)engine.destroyMaterial(pcSceneryMaterial);destroyPcTexture(pcSceneryAtlas);destroyPcTexture(pcSitesAtlas);destroyPcTexture(pcFacilitiesAtlas);destroyPcTexture(pcPaint);
        if(vegetationMaterial!=null)engine.destroyMaterialInstance(vegetationMaterial);if(unitMaterial!=null)engine.destroyMaterial(unitMaterial);if(unitAtlas!=null)engine.destroyTexture(unitAtlas);if(fieldAtlas!=null)engine.destroyTexture(fieldAtlas);if(sceneryAtlas!=null)engine.destroyTexture(sceneryAtlas);if(sceneryMaterial!=null)engine.destroyMaterial(sceneryMaterial);if(siteMaterial!=null)engine.destroyMaterial(siteMaterial);if(siteAtlas!=null)engine.destroyTexture(siteAtlas);
        if(overviewWaterMaterial!=null)engine.destroyMaterial(overviewWaterMaterial);
        if(overviewGroundMaterial!=null)engine.destroyMaterial(overviewGroundMaterial);
        if(waterMaterial!=null)engine.destroyMaterial(waterMaterial);
        if(pcGroundMaterial!=null)engine.destroyMaterial(pcGroundMaterial);if(pcGroundOutlineMaterial!=null)engine.destroyMaterial(pcGroundOutlineMaterial);
        if(pcWaterMaterial!=null)engine.destroyMaterial(pcWaterMaterial);
        destroyPcTexture(pcWaterSheet0);destroyPcTexture(pcWaterSheet1);
        pcWaterMaterial=null;pcWaterSheet0=null;pcWaterSheet1=null;pcWaterClocks.clear();pcWaterAdvanced.clear();
        destroyPcTexture(pcNearLow);destroyPcTexture(pcNearHigh);destroyPcTexture(pcColor);destroyPcTexture(pcPalette);destroyPcTexture(pcPaletteSizes);destroyPcTexture(pcGroundNormal);destroyPcTexture(pcGroundPaint);destroyPcTexture(pcGroundOutline);
        pcGroundOutlineMaterial=null;pcGroundNormal=null;pcGroundPaint=null;pcGroundOutline=null;
        pcGroundMaterial=null;pcNearLow=null;pcNearHigh=null;pcColor=null;pcPalette=null;pcPaletteSizes=null;pcSeason=-1;
        if(groundMaterial!=null)engine.destroyMaterial(groundMaterial);
        for(Texture texture:groundTextures)engine.destroyTexture(texture);groundTextures.clear();
        if(material!=null)engine.destroyMaterial(material);
        if(skybox!=null){scene.setSkybox(null);engine.destroySkybox(skybox);}
        if(view!=null)engine.destroyView(view);if(scene!=null)engine.destroyScene(scene);if(renderer!=null)engine.destroyRenderer(renderer);
        if(cameraEntity!=0){engine.destroyCameraComponent(cameraEntity);EntityManager.get().destroy(cameraEntity);}engine.flushAndWait();engine.destroy();engine=null;
        renderer=null;scene=null;view=null;lens=null;skybox=null;displayHelper=null;
        pcFacilityRigs=null;pcUnits=null;pcUnitMaterial=null;pcUnitAlphaMaterial=null;overlay.ghostPcFormation=null;
        pcSceneryMaterial=null;pcSceneryInstance=null;pcFacilitiesInstance=null;pcSceneryAtlas=null;pcScenery=null;pcSitesAtlas=null;pcSites=null;pcFacilitiesAtlas=null;pcFacilities=null;pcCliffWalls=null;pcDams=null;
        pcPaint=null;pcEnvironment=null;pcLightMonth=-1;pcFogMonth=-1;
        material=null;waterMaterial=null;groundMaterial=null;overviewGroundMaterial=null;overviewWaterMaterial=null;siteMaterial=null;unitMaterial=null;sceneryMaterial=null;vegetationMaterial=null;
        siteAtlas=null;fieldAtlas=null;unitAtlas=null;sceneryAtlas=null;textureBytes=0;cameraEntity=light=0;
        Arrays.fill(effectMeshes,null);Arrays.fill(effectEntities,0);
    }
    private final class GpuMesh {
        VertexBuffer vb;IndexBuffer ib;int entity;int references;final SceneMesh source;boolean shown,overview;
        GpuMesh waterChild;MaterialInstance waterInstance;String waterKey;
        GpuMesh(SceneMesh m){source=m;try{
            VertexBuffer.Builder builder=new VertexBuffer.Builder().vertexCount(m.vertices.length/7).bufferCount(m.tangents!=null?3:m.surfaceData!=null?2:m.uv==null?1:2).attribute(VertexBuffer.VertexAttribute.POSITION,0,VertexBuffer.AttributeType.FLOAT3,0,28).attribute(VertexBuffer.VertexAttribute.COLOR,0,VertexBuffer.AttributeType.FLOAT4,12,28);
            if(m.surfaceData!=null){
                if(m.pcGround)builder.attribute(VertexBuffer.VertexAttribute.UV0,1,VertexBuffer.AttributeType.FLOAT2,0,8).attribute(VertexBuffer.VertexAttribute.UV1,1,VertexBuffer.AttributeType.FLOAT2,0,8);
                else builder.attribute(VertexBuffer.VertexAttribute.UV0,1,VertexBuffer.AttributeType.FLOAT2,0,32).attribute(VertexBuffer.VertexAttribute.TANGENTS,1,VertexBuffer.AttributeType.FLOAT4,8,32).attribute(VertexBuffer.VertexAttribute.UV1,1,VertexBuffer.AttributeType.FLOAT2,24,32);
            }
            if(m.tangents!=null)builder.attribute(VertexBuffer.VertexAttribute.TANGENTS,2,VertexBuffer.AttributeType.FLOAT4,0,16);
            if(m.uv!=null)builder.attribute(VertexBuffer.VertexAttribute.UV0,1,VertexBuffer.AttributeType.FLOAT2,0,8);vb=builder.build(engine);
            if(m.surfaceData!=null){FloatBuffer data=ByteBuffer.allocateDirect(m.surfaceData.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();data.put(m.surfaceData).flip();vb.setBufferAt(engine,1,data);}
            if(m.uv!=null){FloatBuffer uv=ByteBuffer.allocateDirect(m.uv.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();uv.put(m.uv).flip();vb.setBufferAt(engine,1,uv);}
            if(m.tangents!=null){FloatBuffer t=ByteBuffer.allocateDirect(m.tangents.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();t.put(m.tangents).flip();vb.setBufferAt(engine,2,t);}
            FloatBuffer v=ByteBuffer.allocateDirect(m.vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();v.put(m.vertices).flip();vb.setBufferAt(engine,0,v);
            Buffer indexData=MeshIndexBuffer.encode(m.vertices.length/7,m.indices);
            ib=new IndexBuffer.Builder().indexCount(m.indices.length).bufferType(MeshIndexBuffer.compact(m.vertices.length/7)?IndexBuffer.Builder.IndexType.USHORT:IndexBuffer.Builder.IndexType.UINT).build(engine);ib.setBuffer(engine,indexData);
            entity=EntityManager.get().create();
            if(m.pcWater){
                if(pcWaterMaterial==null)throw new IllegalStateException("PC original water material not ready");
                waterKey=m.chunkQ+":"+m.chunkR;waterInstance=pcWaterMaterial.createInstance();
                TextureSampler linear=new TextureSampler(TextureSampler.MinFilter.LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE);
                waterInstance.setParameter("sheet0",pcWaterSheet0,linear);waterInstance.setParameter("sheet1",pcWaterSheet1,linear);
                long clock=pcWaterClocks.getOrDefault(waterKey,0L);waterInstance.setParameter("clockMS",(int)clock,(int)(clock>>>32));
                build(entity,waterInstance);
            }else if(m.uv==null)build(entity);
            if(m.sourceWater!=null)waterChild=new GpuMesh(m.sourceWater);
        }catch(RuntimeException|LinkageError|OutOfMemoryError error){destroy();throw error;}}
        private Box bounds(int count){
            float radius=source.radius+(count>1?.55f:0);
            boolean pc=source.pcGround||source.pcWater||source.pcScenery||source.pcSite||source.pcFacility||source.pcUnit||source.pcFacilityRig;
            if(!pc)return new Box(source.x,1.3f,source.z,radius,2.7f,radius);
            // Source heights now reach6.375, and original object/rig vertices
            // can extend above that. Bound actual geometry rather than clipping
            // it to the former flattened 2.7 vertical half extent. Animated
            // poses and per-member terrain offsets retain a conservative margin.
            float padding=source.pcUnit||source.pcFacilityRig?Math.max(2.7f,source.maxY-source.minY):.1f;
            if(source.pcUnit&&count>1)padding+=game.sanguo.core.PcMap.MAX_HEIGHT;
            return new Box(source.x,(source.minY+source.maxY)*.5f,source.z,radius,(source.maxY-source.minY)*.5f+padding,radius);
        }
        void build(int target){
            if(source.surfaceData==null||source.landIndexCount<0){build(target,material.getDefaultInstance());return;}
            overview=overviewTerrain;
            int land=source.landIndexCount,water=source.indices.length-land;
            boolean outline=source.pcGround&&land>0&&pcGroundOutlineMaterial!=null;
            RenderableManager.Builder b=new RenderableManager.Builder((land>0?1:0)+(water>0?1:0)+(outline?1:0))
                .boundingBox(bounds(1)).castShadows(false).receiveShadows(environmentShadows);
            if(outline)b.priority(3); // Source terrain passes precede source translucent objects.
            int slot=0;
            if(land>0)b.material(slot,landMaterial(overview)).geometry(slot++,RenderableManager.PrimitiveType.TRIANGLES,vb,ib,0,land);
            if(water>0)b.material(slot,(overview?overviewWaterMaterial:waterMaterial).getDefaultInstance()).geometry(slot++,RenderableManager.PrimitiveType.TRIANGLES,vb,ib,land,water);
            if(outline)b.material(slot,pcGroundOutlineMaterial.getDefaultInstance()).geometry(slot,RenderableManager.PrimitiveType.TRIANGLES,vb,ib,0,land).blendOrder(slot,1);
            b.build(engine,target);
        }
        void bindTerrainMaterial(){
            // Only called by loadVisible inside an admitted owner frame.
            RenderableManager manager=engine.getRenderableManager();int instance=manager.getInstance(entity),slot=0;
            if(source.landIndexCount>0)manager.setMaterialInstanceAt(instance,slot++,landMaterial(overviewTerrain));
            if(source.landIndexCount<source.indices.length)manager.setMaterialInstanceAt(instance,slot,(overviewTerrain?overviewWaterMaterial:waterMaterial).getDefaultInstance());
            overview=overviewTerrain;
        }
        void build(int target,MaterialInstance instance){build(target,instance,1);}
        void build(int target,MaterialInstance instance,int count){build(target,instance,count,null);}
        void build(int target,MaterialInstance instance,int count,MaterialInstance alpha){
            if(source.pcUnit||source.pcFacilityRig){
                int opaque=source.pcUnitOpaqueIndices,transparent=source.indices.length-opaque;
                RenderableManager.Builder b=new RenderableManager.Builder(opaque>0&&transparent>0?2:1).instances(count)
                    .boundingBox(bounds(count))
                    .castShadows(environmentShadows).receiveShadows(environmentShadows);
                int slot=0;if(opaque>0)b.material(slot,instance).geometry(slot++,RenderableManager.PrimitiveType.TRIANGLES,vb,ib,0,opaque);
                if(transparent>0)b.material(slot,alpha).geometry(slot,RenderableManager.PrimitiveType.TRIANGLES,vb,ib,opaque,transparent);
                b.build(engine,target);return;
            }
            new RenderableManager.Builder(1).instances(count).boundingBox(bounds(count)).material(0,instance).geometry(0,RenderableManager.PrimitiveType.TRIANGLES,vb,ib).castShadows(environmentShadows&&source.uv!=null&&(!source.vegetation||quality==SceneQuality.HIGH)).receiveShadows(environmentShadows&&source.uv!=null).build(engine,target);}
        void show(boolean value){if(waterChild!=null)waterChild.show(value);if(shown==value)return;shown=value;if(value)scene.addEntity(entity);else scene.removeEntity(entity);}
        void destroy(){if(waterChild!=null){waterChild.destroy();waterChild=null;}if(entity!=0){scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);entity=0;}if(waterInstance!=null){engine.destroyMaterialInstance(waterInstance);waterInstance=null;}if(vb!=null){engine.destroyVertexBuffer(vb);vb=null;}if(ib!=null){engine.destroyIndexBuffer(ib);ib=null;}}
    }
    private final class Proxy {
        final SceneLabelVisibility labelVisibility=new SceneLabelVisibility();
        final int entity; GpuMesh shape; int flag,base,state; GpuMesh flagShape,baseShape,stateShape; MaterialInstance instance,alphaInstance;
        MapSceneSnapshot.Item item;boolean shown;final UnitMotion motion=new UnitMotion();final UnitAnimation animation=new UnitAnimation();String poseKey,rigRestKey;float y;final UnitFormation formation=new UnitFormation();final PcUnitFormation pcFormation=new PcUnitFormation(pcUnits);int memberCount=1;
        Proxy(MapSceneSnapshot.Item item,GpuMesh shape){this.item=item;if(shape.source.pcFacilityRig)rigRestKey=PcFacilityRigs.key(item,snapshot.month,0);if(item.unit!=null)animation.naval=item.unit.naval;motion.settle(item.hex,snapshot.ground.grid);this.shape=shape;shape.references++;entity=EntityManager.get().create();try{
            if(shape.source.uv!=null){instance=(shape.source.pcUnit||shape.source.pcFacilityRig?pcUnitMaterial:nativeBody()?pcSceneryMaterial:item.unit==null?siteMaterial:unitMaterial).createInstance();instance.setParameter("atlas",shape.source.pcUnit?pcUnitSheets[shape.source.pcUnitModel]:shape.source.pcFacility?pcFacilitiesAtlas:shape.source.pcSite?pcSitesAtlas:item.site!=null?siteAtlas:item.unit!=null?unitAtlas:fieldAtlas,new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE));if(!nativeBody())instance.setParameter("damage",0f);if(shape.source.pcUnit||shape.source.pcFacilityRig){alphaInstance=pcUnitAlphaMaterial.createInstance();bindPcUnitTextures();if(shape.source.pcFacilityRig){float[] single=new float[76*4];single[3]=1;instance.setParameter("members",MaterialInstance.FloatElement.FLOAT4,single,0,76);alphaInstance.setParameter("members",MaterialInstance.FloatElement.FLOAT4,single,0,76);}}shape.build(entity,instance,1,alphaInstance);}else shape.build(entity);
            if(snapshot.ground.pcMap==null&&(item.site!=null||item.unit!=null&&!shape.source.pcUnit||item.facility!=null&&!(shape.source.pcDam&&item.facility.owner<0))){String key="flag:"+item.color;flagShape=shapes.get(key);if(flagShape==null){flagShape=new GpuMesh(SceneMesh.proxy(3,item.color));shapes.put(key,flagShape);}flagShape.references++;flag=EntityManager.get().create();flagShape.build(flag);}
            // Seven-cell occupancy is shown by the selection overlay; ground blends in the terrain field.
            if(!facilityOverlay(item,shape.source.pcFacility).isEmpty()){
                String stateKey=item.facility.burning?"fire":"scaffold";stateShape=shapes.get(stateKey);
                if(stateShape==null)throw new IllegalStateException("state decode not ready");
                stateShape.references++;
                state=EntityManager.get().create();if(stateShape.source.uv!=null)stateShape.build(state,vegetationMaterial);else stateShape.build(state);
            }
            if(instance!=null&&(shape.source.pcSite||shape.source.pcFacility)&&!shape.source.pcFacilityRig)bindPcPainting(instance,snapshot.month);
            updateSeason();position(snapshot.ground.grid.x(item.hex),snapshot.ground.grid.z(item.hex));updateDamage();
        }catch(RuntimeException|LinkageError|OutOfMemoryError error){destroy();throw error;}}
        void replace(GpuMesh mesh){
            engine.getRenderableManager().destroy(entity);shape.references--;shape=mesh;shape.references++;if(instance==null)shape.build(entity);else{if(shape.source.pcUnit||shape.source.pcFacilityRig)bindPcUnitTextures();shape.build(entity,instance,memberCount,alphaInstance);}if(shown)scene.addEntity(entity);
        }
        void bindPcUnitTextures(){TextureSampler sampler=new TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,TextureSampler.MagFilter.LINEAR,TextureSampler.WrapMode.CLAMP_TO_EDGE);Texture texture=shape.source.pcFacilityRig?pcFacilitiesAtlas:pcUnitSheets[shape.source.pcUnitModel];instance.setParameter("atlas",texture,sampler);alphaInstance.setParameter("atlas",texture,sampler);float encoded=shape.source.pcFacilityRig?1f:0f;instance.setParameter("atlasEncoded",encoded);alphaInstance.setParameter("atlasEncoded",encoded);}
        boolean nativeBody(){return shape.source.pcSite||shape.source.pcFacility||shape.source.pcUnit;}
        void updateSeason(){if(instance!=null&&!nativeBody())EnvironmentProfile.pigment(instance,season,item.facility!=null&&item.facility.type.equals("domestic/FARM"));}
        void updateDamage(){if(instance!=null&&!nativeBody())instance.setParameter("damage",item.site!=null?item.site.damage*.5f:item.facility!=null?1-item.facility.hp/(float)Math.max(1,item.facility.maxHp):0);}
        String stateKey(){return facilityOverlay(item,shape.source.pcFacility);}
        private MapSceneSnapshot.Ground positionedGround;
        private float positionedX,positionedZ,positionedYaw,positionedScale;
        private int positionedTroops,positionedLod;
        private boolean positionedNaval;
        long positionUploads,positionSkips;
        void position(float x,float z){
            PcSites.Placement nativeSite=shape.source.pcSite?pcSites.placement(snapshot.ground,item):null;
            PcConstructibleWalls.Placement nativeWall=shape.source.pcWall?PcConstructibleWalls.placement(snapshot.ground,item):null;
            PcDams.Placement nativeDam=shape.source.pcDam?pcDams.placement(snapshot.ground,item):null;
            if(nativeSite!=null){x=nativeSite.x()-snapshot.ground.sourceOriginX;z=nativeSite.z()-snapshot.ground.sourceOriginY;motion.x=x;motion.z=z;}
            if(nativeWall!=null){x=nativeWall.x;z=nativeWall.z;motion.x=x;motion.z=z;}
            if(nativeDam!=null){x=nativeDam.x-snapshot.ground.sourceOriginX;z=nativeDam.z-snapshot.ground.sourceOriginY;motion.x=x;motion.z=z;}
            float angle=nativeSite!=null?nativeSite.yaw():nativeDam!=null?nativeDam.yaw:nativeWall!=null?0:item.site==null?(item.facility!=null?item.facility.direction*(float)Math.PI/3:motion.yaw):item.site.yaw;
            float scale=nativeSite!=null?1:item.site==null?(item.unit==null?1:animation.scale):item.site.scale;
            int troops=item.unit==null?0:UnitAnimation.shownTroops(item.unit,replay,replayFraction);
            // A pose swap keeps the entity transform and material instance. Only
            // contact inputs can invalidate placement; a camera move cannot.
            if(positionedGround==snapshot.ground&&positionedX==x&&positionedZ==z&&positionedYaw==angle&&positionedScale==scale&&positionedTroops==troops&&positionedLod==unitLod&&positionedNaval==animation.naval){positionSkips++;return;}
            positionedGround=snapshot.ground;positionedX=x;positionedZ=z;positionedYaw=angle;positionedScale=scale;positionedTroops=troops;positionedLod=unitLod;positionedNaval=animation.naval;
            positionUploads++;
            y=nativeSite!=null?nativeSite.y():nativeDam!=null?nativeDam.y:nativeWall!=null?nativeWall.y:item.unit!=null&&animation.naval?.02f:snapshot.ground.surface.meshHeight(x,z)+.02f;
            if(shape.source.pcUnit&&instance!=null){
                boolean changed=pcFormation.sample(item.unit,troops,animation.naval,snapshot.ground,x,z,motion.yaw);y=pcFormation.rootY;
                if(memberCount!=pcFormation.count){memberCount=pcFormation.count;replace(shape);}
                if(changed){instance.setParameter("members",MaterialInstance.FloatElement.FLOAT4,pcFormation.members,0,76);alphaInstance.setParameter("members",MaterialInstance.FloatElement.FLOAT4,pcFormation.members,0,76);}
            }else if(item.unit!=null&&instance!=null){
                boolean contactChanged=formation.sample(item.unit,troops,animation.naval,unitLod,snapshot.ground,x,z,motion.yaw,animation.scale);y=formation.rootY;
                if(memberCount!=formation.count){memberCount=formation.count;replace(shape);}
                if(contactChanged)for(int i=0;i<UnitFormation.CAPACITY;i++){
                    float[] m=formation.placement[i],g=formation.grade[i];
                    instance.setParameter("member"+i,m[0],m[1],m[2],m[3]);instance.setParameter("grade"+i,g[0],g[1]);
                }
            }
            float c=(float)Math.cos(angle)*scale,s=(float)Math.sin(angle)*scale;
            float[] matrix={c,0,-s,0,0,scale,0,0,s,0,c,0,x,y,z,1};TransformManager tm=engine.getTransformManager();tm.setTransform(tm.getInstance(entity),matrix);
            if(state!=0)tm.setTransform(tm.getInstance(state),matrix);
            if(base!=0){float[] baseMatrix={1,0,0,0,0,1,0,0,0,0,1,0,x,y,z,1};tm.setTransform(tm.getInstance(base),baseMatrix);}
            if(flag!=0){float[] f={.42f,0,0,0,0,.42f,0,0,0,0,.42f,0,x,y+(item.kind==0?.82f:item.unit!=null?.25f:.46f),z,1};tm.setTransform(tm.getInstance(flag),f);}
        }
        void show(boolean value){shown=value;if(value){scene.addEntity(entity);if(flag!=0)scene.addEntity(flag);if(base!=0)scene.addEntity(base);if(state!=0)scene.addEntity(state);}else{scene.removeEntity(entity);if(flag!=0)scene.removeEntity(flag);if(base!=0)scene.removeEntity(base);if(state!=0)scene.removeEntity(state);}}
        void destroy(){shape.references--;if(flagShape!=null)flagShape.references--;if(stateShape!=null)stateShape.references--;if(baseShape!=null)baseShape.references--;scene.removeEntity(entity);engine.destroyEntity(entity);EntityManager.get().destroy(entity);if(flag!=0){scene.removeEntity(flag);engine.destroyEntity(flag);EntityManager.get().destroy(flag);}if(base!=0){scene.removeEntity(base);engine.destroyEntity(base);EntityManager.get().destroy(base);}if(state!=0){scene.removeEntity(state);engine.destroyEntity(state);EntityManager.get().destroy(state);}if(instance!=null)engine.destroyMaterialInstance(instance);if(alphaInstance!=null)engine.destroyMaterialInstance(alphaInstance);}
    }
    private boolean labelVisible(Proxy object){
        float wx=object.motion.x,wz=object.motion.z,y=object.y+1;
        return object.labelVisibility.visible(camera,snapshot.ground.surface,wx,wz,y);
    }
    private boolean selected(MapSceneSnapshot.Item item){return item.hex.equals(snapshot.selected)||item.site!=null&&item.site.cells.contains(snapshot.selected);}
    private final class Overlay extends android.view.View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Map<String,android.graphics.RectF> labelHits=new LinkedHashMap<>();
        final android.graphics.Path cellPath=new android.graphics.Path();
        final android.graphics.Path siteSelectionPath=new android.graphics.Path();
        Set<Hex> siteSelectionCells=Collections.emptySet();MapSceneSnapshot.Ground siteSelectionGround;float[] siteSelectionStamp;
        long siteSelectionBuilds,siteSelectionFailures;
        final Silhouette ghost=new Silhouette(),forestUnit=new Silhouette();
        final UnitFormation ghostFormation=new UnitFormation();
        PcUnitFormation ghostPcFormation;
        long ghostDraws,forestSilhouetteDraws,selectionDraws;
        /** One cached path per preview/selected forest unit. All triangles wind the
         * same way, so a single translucent fill has no double-dark overlap seams. */
        final class Silhouette {
            final android.graphics.Path path=new android.graphics.Path();
            SceneMesh mesh;float[] stamp;long contact=-1,builds;
            void clear(){path.rewind();mesh=null;stamp=null;contact=-1;}
            void update(SceneMesh source,UnitFormation formation,float x,float z,float yaw,float scale){
                float[] next={x,z,yaw,scale,camera.x,camera.z,camera.span,camera.tilt,camera.yaw,camera.facing,camera.width,camera.height};
                if(mesh==source&&contact==formation.updates&&Arrays.equals(stamp,next))return;
                mesh=source;stamp=next;contact=formation.updates;path.rewind();builds++;
                float[] points=UnitSilhouette.project(source,camera,formation,x,z,yaw,scale);
                projected(source,points,formation.count);
            }
            void update(SceneMesh source,PcUnitFormation formation,float x,float z,float yaw){
                float[] next={x,z,yaw,1,camera.x,camera.z,camera.span,camera.tilt,camera.yaw,camera.facing,camera.width,camera.height};
                if(mesh==source&&contact==formation.updates&&Arrays.equals(stamp,next))return;
                mesh=source;stamp=next;contact=formation.updates;path.rewind();builds++;
                projected(source,UnitSilhouette.project(source,camera,formation,x,z,yaw),formation.count);
            }
            void projected(SceneMesh source,float[] points,int count){
                int vertices=source.vertices.length/7;
                for(int member=0;member<count;member++)for(int i=0;i<source.indices.length;i+=3){
                    int a=(member*vertices+source.indices[i])*2,b=(member*vertices+source.indices[i+1])*2,d=(member*vertices+source.indices[i+2])*2;
                    float area=(points[b]-points[a])*(points[d+1]-points[a+1])-(points[b+1]-points[a+1])*(points[d]-points[a]);
                    if(Math.abs(area)<.001f)continue;
                    if(area<0){int swap=b;b=d;d=swap;}
                    path.moveTo(points[a],points[a+1]);path.lineTo(points[b],points[b+1]);path.lineTo(points[d],points[d+1]);path.close();
                }
            }
        }
        Overlay(Context c){super(c);setClickable(false);}
        final android.graphics.RectF miniRect=new android.graphics.RectF();
        private android.graphics.Bitmap miniBitmap;
        private MapSceneSnapshot.Ground miniGround;
        private NavigatorTransform miniTransform;
        boolean miniDirty=true;
        // Cache only the static territory layer, at exact UI pixel resolution. Labels,
        // tactical ranges, playback and editor overlays remain live. Never cache the UI.
        private android.graphics.Bitmap territoryBitmap;
        private MapSceneSnapshot.Ground territoryGround;
        private int[] cachedColors,cachedBorders;
        private float[] territoryCamera;
        private int cachedTerritoryMode;
        long draws,territoryBuilds,territoryBuildNanos;
        void drawTerritory(Canvas target){
            if(territoryColors==null&&territoryBorders==null){territoryBitmap=null;territoryGround=null;return;}
            int w=getWidth(),h=getHeight();
            if(w<=0||h<=0)return;
            // Oversized windows use the original path instead of an unbounded bitmap.
            if((long)w*h>4194304){territoryBitmap=null;paintTerritory(target);return;}
            float[] pose={camera.x,camera.z,camera.span,camera.yaw,camera.tilt,camera.facing,camera.width,camera.height,w,h};
            if(territoryBitmap==null||territoryGround!=snapshot.ground||cachedColors!=territoryColors
                    ||cachedBorders!=territoryBorders||cachedTerritoryMode!=territoryMode||!Arrays.equals(pose,territoryCamera)){
                long started=System.nanoTime();
                if(territoryBitmap==null||territoryBitmap.getWidth()!=w||territoryBitmap.getHeight()!=h)
                    territoryBitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
                territoryBitmap.eraseColor(android.graphics.Color.TRANSPARENT);
                paintTerritory(new Canvas(territoryBitmap));
                territoryGround=snapshot.ground;cachedColors=territoryColors;cachedBorders=territoryBorders;
                cachedTerritoryMode=territoryMode;territoryCamera=pose;territoryBuilds++;territoryBuildNanos=System.nanoTime()-started;
            }
            target.drawBitmap(territoryBitmap,0,0,null);
        }
        void paintTerritory(Canvas c){
            GridWorldTransform grid=snapshot.ground.grid;
            float rx=camera.extentX()+2,rz=camera.extentZ()+4;
            Hex a=grid.cell(camera.x-rx,camera.z-rz),b=grid.cell(camera.x+rx,camera.z+rz),d=grid.cell(camera.x-rx,camera.z+rz),e=grid.cell(camera.x+rx,camera.z-rz);
            int q0=Math.max(0,Math.min(Math.min(a.q,b.q),Math.min(d.q,e.q))-2),q1=Math.min(snapshot.ground.width-1,Math.max(Math.max(a.q,b.q),Math.max(d.q,e.q))+2);
            int r0=Math.max(0,Math.min(Math.min(a.r,b.r),Math.min(d.r,e.r))-2),r1=Math.min(snapshot.ground.height-1,Math.max(Math.max(a.r,b.r),Math.max(d.r,e.r))+2);
            for(int r=r0;r<=r1;r++)for(int q=q0;q<=q1;q++){
                Hex cell=new Hex(q,r);if(!snapshot.ground.valid(cell))continue;
                if(territoryColors!=null){int color=territoryColors[r*snapshot.ground.width+q];if(color!=0&&cellPath(cell)){p.setColor((color&0xffffff)|0x30000000);p.setStyle(Paint.Style.FILL);c.drawPath(cellPath,p);}}
                if(territoryBorders!=null)border(c,cell,territoryBorders[r*snapshot.ground.width+q]);
            }
        }
        private final android.graphics.DashPathEffect hiddenDash=new android.graphics.DashPathEffect(new float[]{5,5},0);
        private final Map<Hex,Boolean> hiddenCells=new HashMap<>();
        private MapSceneSnapshot.Ground hiddenGround;
        private float hiddenX,hiddenZ,hiddenSpan,hiddenYaw,hiddenTilt;
        private int hiddenWidth,hiddenHeight,hiddenFacing;
        void updateOcclusionCache(){
            if(hiddenGround!=snapshot.ground||hiddenX!=camera.x||hiddenZ!=camera.z||hiddenSpan!=camera.span||hiddenYaw!=camera.yaw||hiddenTilt!=camera.tilt||hiddenWidth!=camera.width||hiddenHeight!=camera.height||hiddenFacing!=camera.facing){
                hiddenCells.clear();hiddenGround=snapshot.ground;hiddenX=camera.x;hiddenZ=camera.z;hiddenSpan=camera.span;hiddenYaw=camera.yaw;hiddenTilt=camera.tilt;hiddenWidth=camera.width;hiddenHeight=camera.height;hiddenFacing=camera.facing;
            }
        }
        void layoutMini(){
            float d=getResources().getDisplayMetrics().density;
            float availableW=getWidth()-panelRight,availableH=getHeight()-panelBottom;
            float width=Math.min(144*d,availableW*.32f),height=Math.min(118*d,availableH*.26f);
            if(availableW<120*d||availableH<140*d){miniRect.setEmpty();return;}
            miniRect.set(availableW-width-8*d,34*d,availableW-8*d,34*d+height);
        }
        void navigate(float sx,float sy){
            if(miniTransform==null||snapshot==null||miniRect.isEmpty())return;
            float x=miniTransform.x((sx-miniRect.left)/miniRect.width()),z=miniTransform.z((sy-miniRect.top)/miniRect.height());
            // Navigation uses the same authoritative validity mask as ground picking.
            // Displayed exterior scenery is not a selectable tile or a navigation target.
            if(!snapshot.ground.valid(snapshot.ground.grid.cell(x,z)))return;
            camera.x=x;camera.z=z;
            clampCamera();invalidate(); // camera only: never select a unit or issue a command
        }
        float miniX(float x){return miniRect.left+miniTransform.u(x)*miniRect.width();}
        float miniY(float z){return miniRect.top+miniTransform.v(z)*miniRect.height();}
        void drawMini(Canvas c){
            layoutMini();if(openingPreview||!navigatorShown||miniRect.isEmpty())return;
            MapSceneSnapshot.Ground g=snapshot.ground;
            if(miniGround!=g||miniDirty){
                miniGround=g;miniDirty=false;miniTransform=new NavigatorTransform(g.minX,g.minZ,g.maxX,g.maxZ);
                if(miniBitmap==null)miniBitmap=android.graphics.Bitmap.createBitmap(256,256,android.graphics.Bitmap.Config.ARGB_8888);
                int[] pixels=new int[256*256];
                for(int y=0;y<256;y++)for(int x=0;x<256;x++){
                    Hex h=g.grid.cell(miniTransform.x((x+.5f)/256),miniTransform.z((y+.5f)/256));
                    int color=0xff18282b;
                    if(g.valid(h)){int index=h.r*g.width+h.q;color=SceneMesh.terrain(g.terrain[index]);
                        if(territoryColors!=null&&territoryColors[index]!=0){int t=territoryColors[index];color=0xff000000|((((color>>16)&255)+((t>>16)&255))/2<<16)|((((color>>8)&255)+((t>>8)&255))/2<<8)|((color&255)+(t&255))/2;}}
                    pixels[y*256+x]=color;
                }
                miniBitmap.setPixels(pixels,0,256,0,0,256,256);
            }
            p.setStyle(Paint.Style.FILL);p.setColor(0xff18282b);c.drawRect(miniRect.left-2,miniRect.top-2,miniRect.right+2,miniRect.bottom+2,p);
            c.drawBitmap(miniBitmap,null,miniRect,p);c.save();c.clipRect(miniRect);
            for(MapSceneSnapshot.Item item:snapshot.items)if(item.site!=null||item.unit!=null){p.setColor(item.color);c.drawCircle(miniX(g.grid.x(item.hex)),miniY(g.grid.z(item.hex)),item.site!=null?3:2,p);}
            android.graphics.Path viewport=new android.graphics.Path();
            float[][] corners={{0,0},{camera.width-panelRight,0},{camera.width-panelRight,camera.height-panelBottom},{0,camera.height-panelBottom}};
            for(int i=0;i<4;i++){float sx=corners[i][0],sy=corners[i][1],h=g.surface.rayHeight(camera,sx,sy);if(!Float.isFinite(h))h=0;
                float x=miniX(camera.worldX(sx,sy,h)),y=miniY(camera.worldZ(sx,sy,h));if(i==0)viewport.moveTo(x,y);else viewport.lineTo(x,y);}
            viewport.close();p.setColor(0xffffd576);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(viewport,p);c.restore();
            p.setStyle(Paint.Style.FILL);p.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);p.setColor(0xfff0e5c8);c.drawText("北 ↑ · 点按/拖动定位",miniRect.left,miniRect.bottom+13*getResources().getDisplayMetrics().density,p);
        }
        boolean cellPath(Hex h){
            if(h==null||snapshot==null||!snapshot.ground.valid(h))return false;
            GridWorldTransform g=snapshot.ground.grid;float x=g.x(h),z=g.z(h);cellPath.rewind();
            // Sample every eighth cell along edges from the same terrain surface as picking.
            int n=0;for(int edge=0;edge<SceneMesh.EDGE.length;edge++){
                float[] a=SceneMesh.EDGE[edge],b=SceneMesh.EDGE[(edge+1)%SceneMesh.EDGE.length];
                for(int step=0,steps=camera.span<24?4:1;step<steps;step++){float t=step/(float)steps,wx=x+a[0]+(b[0]-a[0])*t,wz=z+a[1]+(b[1]-a[1])*t;
                    float y=snapshot.ground.surface.sample(wx,wz)+.015f;float[] shown=snapshot.ground.shoreline.project(wx,wz);
                    float sx=camera.screenX(shown[0],shown[1],y),sy=camera.screenY(shown[0],shown[1],y);
                    if(n++==0)cellPath.moveTo(sx,sy);else cellPath.lineTo(sx,sy);}}
            cellPath.close();return true;
        }
        boolean hidden(Hex h){
            return hiddenCells.computeIfAbsent(h,key->{GridWorldTransform g=snapshot.ground.grid;float x=g.x(key),z=g.z(key),y=snapshot.ground.surface.at(key);
                float front=snapshot.ground.surface.rayHeight(camera,camera.screenX(x,z,y),camera.screenY(x,z,y));return Float.isFinite(front)&&front>y+.06f;});
        }
        void cell(Canvas c,Hex h,int color){
            if(!visible(h)||!cellPath(h))return;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
            // Tactical x-ray is deliberate: dashed means terrain-obscured, never extra reachability.
            p.setPathEffect(hidden(h)?hiddenDash:null);c.drawPath(cellPath,p);p.setPathEffect(null);
        }
        void selectedCell(Canvas c,Hex h,int color){
            if(!visible(h)||!cellPath(h))return;selectionDraws++;
            float density=getResources().getDisplayMetrics().density;
            p.clearShadowLayer();p.setPathEffect(null);p.setStyle(Paint.Style.FILL);
            p.setColor((color&0xffffff)|0x22000000);c.drawPath(cellPath,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);
            p.setPathEffect(hidden(h)?hiddenDash:null);
            p.setColor(0xe0122027);p.setStrokeWidth(6*density);c.drawPath(cellPath,p);
            p.setColor(color);p.setStrokeWidth(3.5f*density);c.drawPath(cellPath,p);
            p.setColor(0xfffff2cc);p.setStrokeWidth(1.05f*density);c.drawPath(cellPath,p);
            p.setPathEffect(null);p.setStrokeJoin(Paint.Join.MITER);
        }
        /** One outline around the selected site's authoritative cells; no internal shared edges. */
        void selectedSite(Canvas canvas,Collection<Hex> cells,int color){
            float[] stamp={camera.x,camera.z,camera.span,camera.tilt,camera.yaw,camera.facing,camera.width,camera.height};
            if(siteSelectionGround!=snapshot.ground||siteSelectionCells.size()!=cells.size()||!siteSelectionCells.containsAll(cells)||!Arrays.equals(siteSelectionStamp,stamp)){
                siteSelectionGround=snapshot.ground;siteSelectionCells=new LinkedHashSet<>(cells);siteSelectionStamp=stamp;siteSelectionBuilds++;
                siteSelectionPath.rewind();
                for(Hex h:cells)if(cellPath(h)&&!siteSelectionPath.op(cellPath,android.graphics.Path.Op.UNION)){
                    siteSelectionFailures++;siteSelectionPath.rewind();break;
                }
            }
            if(siteSelectionPath.isEmpty())return;
            selectionDraws++;float density=getResources().getDisplayMetrics().density;
            p.clearShadowLayer();p.setPathEffect(null);p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(0xe0122027);p.setStrokeWidth(6*density);canvas.drawPath(siteSelectionPath,p);
            p.setColor(color);p.setStrokeWidth(3.5f*density);canvas.drawPath(siteSelectionPath,p);
            p.setColor(0xfffff2cc);p.setStrokeWidth(1.05f*density);canvas.drawPath(siteSelectionPath,p);
            p.setStrokeJoin(Paint.Join.MITER);
        }
        void drawDragGhost(Canvas c){
            if(!draggingUnit||dragTarget==null||!visible(dragTarget)||!snapshot.ground.valid(dragTarget))return;
            Proxy actor=objects.get(dragActorKey);if(actor==null||actor.item.unit==null)return;
            // Freeze the already-loaded CPU pose for this gesture. No asset decoding,
            // GPU ownership or army movement is driven by this preview.
            if(dragMesh==null)dragMesh=actor.shape.source;
            GridWorldTransform grid=snapshot.ground.grid;float x=grid.x(dragTarget),z=grid.z(dragTarget),yaw=actor.motion.yaw;
            if(dragPlan!=null&&dragPlan.path.size()>1){Hex from=dragPlan.path.get(dragPlan.path.size()-2);yaw=(float)Math.atan2(x-grid.x(from),z-grid.z(from));}
            float rootY;
            if(dragMesh.pcUnit){
                if(ghostPcFormation==null)ghostPcFormation=new PcUnitFormation(pcUnits);
                ghostPcFormation.sample(actor.item.unit,actor.item.unit.troops,actor.animation.naval,snapshot.ground,x,z,yaw);
                ghost.update(dragMesh,ghostPcFormation,x,z,yaw);rootY=ghostPcFormation.rootY;
            }else{
                ghostFormation.sample(actor.item.unit,actor.item.unit.troops,actor.animation.naval,unitLod,snapshot.ground,x,z,yaw,1);
                ghost.update(dragMesh,ghostFormation,x,z,yaw,1);rootY=ghostFormation.rootY;
            }
            float sx=camera.screenX(x,z,rootY),sy=camera.screenY(x,z,rootY),r=camera.pixels()*.40f;
            p.clearShadowLayer();p.setPathEffect(null);p.setStyle(Paint.Style.FILL);p.setColor(0x78121e25);
            c.drawOval(sx-r,sy-r*(float)camera.sin(),sx+r,sy+r*(float)camera.sin(),p);
            p.setColor(dragPlan==null?0xc9ff7979:0xc26dffe0);c.drawPath(ghost.path,p);ghostDraws++;
        }
        void drawForestUnit(Canvas c,Proxy object){
            // A faint tactical silhouette keeps the selected formation readable
            // through opaque crowns without removing a tree or changing its mesh.
            Hex at=snapshot.ground.grid.cell(object.motion.x,object.motion.z);
            if(object.instance==null||!Vegetation.forest(snapshot.ground,at))return;
            if(object.shape.source.pcUnit)forestUnit.update(object.shape.source,object.pcFormation,object.motion.x,object.motion.z,object.motion.yaw);
            else forestUnit.update(object.shape.source,object.formation,object.motion.x,object.motion.z,object.motion.yaw,object.animation.scale);
            p.clearShadowLayer();p.setPathEffect(null);p.setStyle(Paint.Style.FILL);p.setColor(0x60ffe4a0);
            c.drawPath(forestUnit.path,p);forestSilhouetteDraws++;
        }
        void border(Canvas c,Hex h,int mask){
            if(mask==0)return;GridWorldTransform g=snapshot.ground.grid;float x=g.x(h),z=g.z(h);
            p.setColor(territoryMode==2?0x997fd1c7:0x88e4c88d);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);
            for(int dir=0;dir<6;dir++)if((mask&(1<<dir))!=0){
                int a=TileGeometry.start(dir),b=TileGeometry.end(dir);cellPath.rewind();
                for(int i=0;i<=8;i++){float t=i/8f,ox=(TileGeometry.CORNER_X[a]*(1-t)+TileGeometry.CORNER_X[b]*t)/TileGeometry.SPAN,oz=(TileGeometry.CORNER_Y[a]*(1-t)+TileGeometry.CORNER_Y[b]*t)/TileGeometry.SPAN;
                    float wx=x+(g.staggered?oz:ox),wz=z+(g.staggered?ox:oz),height=snapshot.ground.surface.sample(wx,wz)+.015f;
                    float[] shown=snapshot.ground.shoreline.project(wx,wz);float sx=camera.screenX(shown[0],shown[1],height),sy=camera.screenY(shown[0],shown[1],height);
                    if(i==0)cellPath.moveTo(sx,sy);else cellPath.lineTo(sx,sy);}
                c.drawPath(cellPath,p);
            }
        }
        void drawCombat(Canvas c){
            float d=getResources().getDisplayMetrics().density;
            TurnJournal.Strike strike=CombatVisual.strike(replay,replayFraction);
            float phase=CombatVisual.phase(replay,replayFraction);
            if(strike!=null&&phase>=CombatVisual.FEEDBACK&&visible(strike.target)&&strike.beforeTroops>strike.afterTroops){
                p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(15*d);p.setColor(0xffffbb90);
                float rootY=snapshot.ground.surface.at(strike.target)+1;
                c.drawText("−"+(strike.beforeTroops-strike.afterTroops)+"兵",camera.screenX(snapshot.ground.grid.x(strike.target),snapshot.ground.grid.z(strike.target),rootY),camera.screenY(snapshot.ground.grid.x(strike.target),snapshot.ground.grid.z(strike.target),rootY)-(phase-CombatVisual.FEEDBACK)*50*d,p);
                p.setTextAlign(Paint.Align.LEFT);
            }
            if(replay!=null&&phase>=CombatVisual.FEEDBACK&&(strike==null||strike==replay.strikes.get(replay.strikes.size()-1))){
                int count=0;float progress=(phase-CombatVisual.FEEDBACK)/(1-CombatVisual.FEEDBACK);
                p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(15*d);
                for(TurnJournal.Impact hit:replay.impacts){
                    String text=CombatVisual.feedback(replay,hit);
                    if(!visible(hit.hex)||text.isEmpty())continue;if(count++>=CombatVisual.TEXT_BUDGET)break;
                    float rootY=snapshot.ground.surface.at(hit.hex)+.9f;
                    float x=camera.screenX(snapshot.ground.grid.x(hit.hex),snapshot.ground.grid.z(hit.hex),rootY);
                    float y=camera.screenY(snapshot.ground.grid.x(hit.hex),snapshot.ground.grid.z(hit.hex),rootY)-progress*28*d;
                    // Different lines at the same tile stay readable (troops, morale, status).
                    int line=0;for(int j=0;j<replay.impacts.indexOf(hit);j++)if(replay.impacts.get(j).hex.equals(hit.hex))line++;
                    p.setColor(hit.loss?0xffffbb90:0xffa5eed0);c.drawText(text,x,y-line*17*d,p);
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
            // Screen source art owns the viewport during its brief cue.
            // Selection outlines and commander labels must not cover the face
            // or calligraphy, or rebuild source silhouette paths behind it.
            if(presentationCue!=null)return;
            draws++;updateOcclusionCache();layoutMini();c.save();c.clipRect(0,0,Math.max(0,camera.width-panelRight),Math.max(0,camera.height-panelBottom));
            drawTerritory(c);
            if((editorGrid&&camera.span<48)||editorCoords||!impassable.isEmpty()){
                GridWorldTransform grid=snapshot.ground.grid;
                float rx=camera.extentX()+2,rz=camera.extentZ()+4;
                Hex a=grid.cell(camera.x-rx,camera.z-rz),b=grid.cell(camera.x+rx,camera.z+rz),d=grid.cell(camera.x-rx,camera.z+rz),e=grid.cell(camera.x+rx,camera.z-rz);
                int q0=Math.max(0,Math.min(Math.min(a.q,b.q),Math.min(d.q,e.q))-2),q1=Math.min(snapshot.ground.width-1,Math.max(Math.max(a.q,b.q),Math.max(d.q,e.q))+2);
                int r0=Math.max(0,Math.min(Math.min(a.r,b.r),Math.min(d.r,e.r))-2),r1=Math.min(snapshot.ground.height-1,Math.max(Math.max(a.r,b.r),Math.max(d.r,e.r))+2);
                for(int r=r0;r<=r1;r++)for(int q=q0;q<=q1;q++){
                    Hex h=new Hex(q,r);if(!snapshot.ground.valid(h))continue;
                    if(editorGrid&&camera.span<48&&snapshot.ground.gridCell(h,true)&&cellPath(h)){
                        // Density-aware ink + light rim stays legible on grass, sand and water.
                        // Fade only the far overview; tactical distances keep full contrast.
                        float fade=Math.min(1,(48-camera.span)/12),density=getResources().getDisplayMetrics().density;
                        p.setStyle(Paint.Style.STROKE);p.setPathEffect(null);
                        p.setColor(((int)(170*fade)<<24)|0x132a29);p.setStrokeWidth(2.3f*density);c.drawPath(cellPath,p);
                        p.setColor(((int)(215*fade)<<24)|(editorGrid?0xffffff:0xe1e5c5));p.setStrokeWidth(1.05f*density);c.drawPath(cellPath,p);
                    }
                    if(impassable.contains(MapLayerData.cellKey(h.q,h.r)))cell(c,h,0x99ff6767);
                    if(editorCoords&&camera.span<7){p.setStyle(Paint.Style.FILL);p.setColor(0xffffffff);p.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);float rootY=snapshot.ground.surface.at(h);c.drawText(snapshot.ground.source(h).toString(),camera.screenX(grid.x(h),grid.z(h),rootY),camera.screenY(grid.x(h),grid.z(h),rootY),p);}
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
            if(draggingUnit){
                if(dragPlan!=null)for(Hex h:dragPlan.path)cell(c,h,0xff6ddcc5);
            }
            if(targets.isEmpty())for(Hex h:snapshot.reachable)cell(c,h,0x884ed7c2);for(Hex h:snapshot.coverage)cell(c,h,0xffcfad6e);for(Hex h:snapshot.siege)cell(c,h,0x9975a8fa);for(Hex h:targets.isEmpty()?snapshot.attackTargets:targets)cell(c,h,0xffdd7661);
            boolean selectedSite=false;
            for(MapSceneSnapshot.Item item:snapshot.items)if(item.site!=null&&selected(item)){
                selectedSite=true;selectedSite(c,item.site.cells,0xffffd576);
            }
            if(!selectedSite&&snapshot.selected!=null)selectedCell(c,snapshot.selected,0xffffd576);
            if(draggingUnit){selectedCell(c,dragTarget,dragPlan==null?0xffff7979:0xff6ddcc5);drawDragGhost(c);}
            // Ground rings remain visible through architecture; transit units cannot disappear behind walls.
            for(Proxy object:objects.values())if(object.item.unit!=null&&object.shown){
                if(selected(object.item)&&!draggingUnit)drawForestUnit(c,object);
                float x=camera.screenX(object.motion.x,object.motion.z,object.y),y=camera.screenY(object.motion.x,object.motion.z,object.y);
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
                    int shownTroops=UnitAnimation.shownTroops(u,replay,replayFraction);
                    first=selected?u.commander+" · "+u.equipment:(showCommanders?u.commander:u.equipment)+(showUnitBars?" · "+shownTroops:"");
                    first=u.identity()+" · "+first;
                    if(selected)second=shownTroops+"兵 · 气"+u.energy+(u.status==War.Status.NORMAL?"":" · "+u.status.label)+(u.burning>0?" · 起火":"");
                }else if(item.facility!=null&&!selected){MapSceneSnapshot.FacilityState f=item.facility;first=item.label+(f.level>0?" Lv"+f.level:"")+(f.burning?" · 火":!f.complete?" · 建":"");}
                float width=p.measureText(first);if(second!=null)width=Math.max(width,p.measureText(second));
                float x=camera.screenX(object.motion.x,object.motion.z,object.y+1)-width/2,y=camera.screenY(object.motion.x,object.motion.z,object.y+1);
                x=Math.max(pad,Math.min(camera.width-width-pad,x));
                android.graphics.RectF box=new android.graphics.RectF(x-pad,y-font-pad,x+width+pad,y+(second==null?pad:font*1.2f+pad));
                if(box.bottom<font*2||box.top>camera.height||box.right<0||box.left>camera.width)continue;
                boolean overlap=false;for(android.graphics.RectF used:occupied)if(android.graphics.RectF.intersects(used,box)){overlap=true;break;}
                if((overlap&&!selected)||!labelVisible(object)||box.right>camera.width-panelRight||box.bottom>camera.height-panelBottom||(!openingPreview&&navigatorShown&&android.graphics.RectF.intersects(box,miniRect)))continue;occupied.add(box);labelHits.put(item.key,box);if(openingPreview&&item.site!=null)namedFactions.add(siteOwners.getOrDefault(item.key,-1));
                p.setColor(selected?0xff1b2f37:FactionColors.LABEL_BACKGROUND);c.drawRoundRect(box,pad,pad,p);
                p.setColor(selected?0xffffd576:item.textColor);c.drawText(first,x,y,p);if(second!=null)c.drawText(second,x,y+font*1.2f,p);
            }
            p.setColor(0xfff0e5c8);c.drawText((snapshot.ground.pcMap!=null?"原版美术恢复中 · 部分演出暂缺 | ":"")+((pending>0)?"3D 地形装载中… · 请稍候":(draggingUnit?(dragPlan==null?"移出范围 · 松手取消":"松手移动 · 消耗"+dragPlan.cost):editorGrid?"编辑网格临时显示 · 不修改游戏网格设置":"长按己方选中部队拖动 · 双指缩放/旋转")),12,24*getResources().getDisplayMetrics().density,p);
            if(diagnostics){float y=48*getResources().getDisplayMetrics().density;for(String line:report().split("\n")){c.drawText(line,12,y,p);y+=22*getResources().getDisplayMetrics().density;}}
            drawCombat(c);drawMini(c);p.clearShadowLayer();c.restore();
        }
    }
}
