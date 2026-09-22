package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.os.Bundle;
import java.util.*;
import game.sanguo.core.*;

/** Cached national renderer with independent scene, selection and camera updates. */
public final class MapView extends View {
    public interface TileListener {void tap(Hex tile);}
    private final TileListener listener;
    /** Editor-only gesture stream. Existing campaign gestures and hit rules remain unchanged. */
    interface EditorStroke {void event(int action,Hex tile);}
    private EditorStroke editorStroke;
    private boolean editorMode,editorDrawing,editorCapturing;
    private float editorFocusX,editorFocusY;
    private boolean editorTwoFingers;
    private Set<Hex> editorPreview=Collections.emptySet();
    private boolean editorPreviewValid=true,editorGrid,editorCoordinates,editorPassability,editorFootprints;
    void editorMode(EditorStroke listener){editorMode=true;editorStroke=listener;}
    void editorDrawing(boolean value){editorDrawing=value;editorCapturing=false;}
    void editorPreview(Set<Hex> hexes,boolean valid){editorPreview=hexes;editorPreviewValid=valid;postInvalidateOnAnimation();}
    void editorLayers(boolean grid,boolean coordinates,boolean passability,boolean footprints){editorGrid=grid;editorCoordinates=coordinates;editorPassability=passability;editorFootprints=footprints;invalidate();}
    private void drawEditor(Canvas canvas,int firstRow,int lastRow,boolean detail){
        if(!editorMode)return;
        for(int r=firstRow;r<=lastRow;r++)for(int q=camera.firstColumn(r,world.width,RADIUS*2);q<=camera.lastColumn(r,world.width,RADIUS*2);q++){
            Hex h=tiles[q][r];if(!world.sourceInside(h))continue;
            if(editorPreview.contains(h)){polygon(x(h),y(h),RADIUS-1);fill(canvas,editorPreviewValid?0x8057dfa1:0x80f66c65);stroke(canvas,editorPreviewValid?0xff83ffc2:0xffff8c83,Math.max(1,density/camera.scale));}
            if(detail&&editorGrid){polygon(x(h),y(h),RADIUS);stroke(canvas,0xaaffffff,Math.max(.5f,.5f*density/camera.scale));}
            if(detail&&editorPassability){int cost=world.cost(h,World.Weapon.SPEAR);if(cost<1){polygon(x(h),y(h),RADIUS-2);fill(canvas,0x559c2530);}}
            if(detail&&editorCoordinates&&camera.scale*RADIUS>=24*density){SourceGridCoord source=MapCoordinates.nationalSource(world,h);label(canvas,source.x+","+source.y,x(h),y(h)+4,8*density/camera.scale,0xffffffff);}
        }
        if(editorFootprints)for(Object o:visibleObjects)if(o instanceof World.City){siteBoundary((World.City)o);stroke(canvas,0xff89e5ff,Math.max(1,1.5f*density/camera.scale));}
    }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final MapModels models=new MapModels();
    private final TerrainTiles terrainTiles=new TerrainTiles();
    private Displacement.Preview tacticPreview;
    void setTacticPreview(Displacement.Preview p){tacticPreview=p;invalidate();}
    private Hex impactHex;
    private long impactUntil;
    private boolean defeatImpact;
    void battleFeedback(World.Result result,boolean haptics){
        impactHex=result.impact;defeatImpact=result.feedback==World.Feedback.DEFEAT;
        impactUntil=android.os.SystemClock.uptimeMillis()+450;
        if(haptics&&result.feedback!=World.Feedback.NONE)post(()->{
            int effect=defeatImpact?(android.os.Build.VERSION.SDK_INT>=30?HapticFeedbackConstants.CONFIRM:HapticFeedbackConstants.LONG_PRESS):HapticFeedbackConstants.CONTEXT_CLICK;
            performHapticFeedback(effect);
        });
        invalidate();
    }
    private final GestureDetector gestures;
    private final ScaleGestureDetector scaler;
    private World world;
    private Hex[][] tiles;
    private Hex selected;
    private TurnJournal.Event replayEvent;
    private World.Unit replayActor;
    private float replayFraction;
    private CriticalScene criticalScene;
    private CriticalHit criticalHit;
    private Runnable criticalSkip;
    void setCriticalSkip(Runnable skip){criticalSkip=skip;}
    private float criticalPhase;
    void criticalFrame(CriticalHit hit,float phase){
        criticalHit=hit;criticalPhase=phase;
        if(criticalScene==null&&hit!=null)criticalScene=new CriticalScene(getContext());
        if(criticalScene!=null)criticalScene.set(world,hit);invalidate();
    }
    void replayFrame(TurnJournal.Event event,float fraction){
        if(replayEvent!=event){replayEvent=event;replayActor=event==null?null:event.actorCopy();}
        replayFraction=fraction;postInvalidateOnAnimation();
    }
    boolean replayVisible(TurnJournal.Event event){
        if(replayPoint(event.start)||replayPoint(event.target))return true;
        for(Hex h:event.path)if(replayPoint(h))return true;
        for(TurnJournal.Impact hit:event.impacts)if(replayPoint(hit.hex))return true;
        return false;
    }
    private boolean replayPoint(Hex h){
        if(h==null)return false;
        float sx=x(h)*camera.scale+camera.x,sy=y(h)*camera.scale+camera.y;
        return sx>=-12*density&&sy>=0&&sx<=getWidth()-occludedRight&&sy<=getHeight()-occludedBottom;
    }
    private boolean replayHides(World.Unit u){return replayEvent!=null&&replayActor!=null&&replayActor.id==u.id;}

    private Set<Hex> pickTargets;
    void setPickTargets(Set<Hex> targets){pickTargets=targets;invalidate();}
    private final android.util.SparseArray<List<Object>> objectBuckets=new android.util.SparseArray<>();
    private final Map<Integer,World.Officer> officerIndex=new HashMap<>();
    private final Map<Integer,World.City> cityIndex=new HashMap<>();
    private final ArrayList<Object> visibleObjects=new ArrayList<>();
    private final RectF miniRect=new RectF(),miniButton=new RectF();
    private boolean miniButtonGesture,showCommanders,showUnitBars;
    private android.content.SharedPreferences displayPrefs;
    boolean navigatorShown(){return showMini;}
    boolean commandersShown(){return showCommanders;}
    boolean unitBarsShown(){return showUnitBars;}
    void setCommandersShown(boolean value){showCommanders=value;displayPrefs.edit().putBoolean("commanders",value).apply();invalidate();}
    void setUnitBarsShown(boolean value){showUnitBars=value;displayPrefs.edit().putBoolean("unitBars",value).apply();invalidate();}
    private final ArrayList<RectF> labelBounds=new ArrayList<>();
    private final ArrayList<RectF> labelPool=new ArrayList<>();
    private int labelPoolUsed;
    private RectF labelBox(float left,float top,float right,float bottom){
        if(labelPoolUsed==labelPool.size())labelPool.add(new RectF());
        RectF box=labelPool.get(labelPoolUsed++);box.set(left,top,right,bottom);return box;
    }
    private final Map<Integer,RectF> cityLabelBounds=new HashMap<>();
    private int lastLabels;
    int labelsDrawn(){return lastLabels;}
    private Bitmap miniTerrain;
    private MapRaster miniRaster;
    private int seenTerrainRevision=-1;
    private Bitmap miniTerritory;
    private Territory territory;
    private final Set<Integer> threatenedCities=new HashSet<>();
    private byte[][] factionEdges,siteEdges;
    private MapOverview overview;
    private int overviewBuilds;
    boolean overviewReady(){return overview!=null&&overview.ready();}
    int overviewBuilds(){return overviewBuilds;}
    long overviewBytes(){return overview==null?0:overview.bytes();}
    private Set<Hex> facilityCoverage=Collections.emptySet();
    Set<Hex> facilityCoverage(){return facilityCoverage;}
    private List<Hex> developmentSites=Collections.emptyList();
    private final Map<Integer,String> cityNames=new HashMap<>();
    private final Set<Integer> frontlineCities=new HashSet<>();
    private int territoryMode;
    private boolean openingPreview;
    private int previewFaction=-1;
    void previewMode(){openingPreview=true;showMini=false;}
    void setPreviewFaction(int side){previewFaction=side;invalidate();}
    private void drawPreviewSelection(Canvas c){
        if(!openingPreview||previewFaction<0)return;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f*density);paint.setColor(0xffffdf9a);
        for(World.City site:world.cities)if(site.owner==previewFaction){
            float sx=x(site.hex)*camera.scale+camera.x,sy=y(site.hex)*camera.scale+camera.y;
            if(sx<0||sy<0||sx>getWidth()||sy>getHeight())continue;
            c.drawCircle(sx,sy,Math.max(7*density,RADIUS*camera.scale*(site.kind==World.SiteKind.CITY?1.2f:.7f)),paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    private String territoryOwners="";
    Territory territory(){return territory;}
    int territoryMode(){return territoryMode;}
    void setTerritoryMode(int mode){territoryMode=Math.max(0,Math.min(2,mode));invalidate();}
    private boolean showMini,miniGesture;
    private int lastTilesVisited,lastObjectsVisited;
    private long lastDrawNanos;
    public void toggleNavigator(){showMini=!showMini;displayPrefs.edit().putBoolean("navigator",showMini).apply();invalidate();}
    int tilesVisited(){return lastTilesVisited;}
    int objectsVisited(){return lastObjectsVisited;}
    long drawNanos(){return lastDrawNanos;}
    private void index(Object object,Hex h){
        int key=(h.r/8)*1024+h.q/8;List<Object> bucket=objectBuckets.get(key);
        if(bucket==null){bucket=new ArrayList<>();objectBuckets.put(key,bucket);}bucket.add(object);
    }
    private Hex position(Object o){
        if(o instanceof World.City)return ((World.City)o).hex;
        if(o instanceof World.Unit)return ((World.Unit)o).hex;
        if(o instanceof Domestic.Facility)return ((Domestic.Facility)o).hex;
        if(o instanceof Domestic.Mission)return ((Domestic.Mission)o).hex;
        if(o instanceof War.Fire)return ((War.Fire)o).hex;
        if(o instanceof WorldEvents.Camp)return ((WorldEvents.Camp)o).hex;
        return ((War.Structure)o).hex;
    }
    private void collectVisible(){
        visibleObjects.clear();float screenMargin=Math.max(100*density,80*camera.scale),margin=screenMargin/camera.scale;
        int r0=camera.firstRow(world.height,margin),r1=camera.lastRow(world.height,margin);
        for(int br=r0/8;br<=r1/8;br++){
            int q0=camera.firstColumn(Math.min(world.height-1,br*8+7),world.width,margin)/8;
            int q1=camera.lastColumn(br*8,world.width,margin)/8;
            for(int bq=q0;bq<=q1;bq++){
                List<Object> bucket=objectBuckets.get(br*1024+bq);if(bucket==null)continue;
                for(Object o:bucket){lastObjectsVisited++;Hex h=position(o);if(camera.visible(x(h),y(h),screenMargin))visibleObjects.add(o);}
            }
        }
    }
    private Map<Hex,Integer> reachable=Collections.emptyMap();
    private final Set<Hex> attackTargets=new HashSet<>();
    int reachableCount(){return reachable.size();}
    private void addAttackTarget(World.Unit u,Hex h,boolean normal){
        if(normal){attackTargets.add(h);return;}

    }
    private final MapCamera camera=new MapCamera();
    private final android.widget.OverScroller fling;
    private android.animation.ValueAnimator cameraMotion;
    private int flingX,flingY,occludedRight,occludedBottom;
    private boolean caughtMotion;
    private long renderedRevision=Long.MIN_VALUE;
    private int renderedTurn=-1,renderedPlayer=-1,sceneBuilds,selectionBuilds;
    private Hex touchHex;
    private long touchUntil;
    int sceneBuilds(){return sceneBuilds;}
    int selectionBuilds(){return selectionBuilds;}
    boolean cameraMoving(){return cameraMotion!=null||!fling.isFinished();}
    void invalidateScene(){renderedRevision=Long.MIN_VALUE;}
    void setPanelOcclusion(int right,int bottom){
        right=Math.max(0,right);bottom=Math.max(0,bottom);
        if(occludedRight==right&&occludedBottom==bottom)return;
        occludedRight=right;occludedBottom=bottom;postInvalidateOnAnimation();
    }
    private void stopCamera(){
        if(cameraMotion!=null){cameraMotion.cancel();cameraMotion=null;}
        fling.abortAnimation();
    }
    @Override public void computeScroll(){
        if(!fling.computeScrollOffset())return;
        float ox=camera.x,oy=camera.y;
        int dx=fling.getCurrX()-flingX,dy=fling.getCurrY()-flingY;
        camera.pan(dx,dy);
        flingX=fling.getCurrX();flingY=fling.getCurrY();
        // A zero-distance first frame is not a boundary hit: preserve the fling.
        if((dx!=0||dy!=0)&&Math.abs(camera.x-ox)<.01f&&Math.abs(camera.y-oy)<.01f)fling.abortAnimation();
        postInvalidateOnAnimation();
    }
    private void glide(Runnable target){
        stopCamera();float startScale=camera.scale,startX=camera.centerX(),startY=camera.centerY();
        target.run();float endScale=camera.scale,endX=camera.centerX(),endY=camera.centerY();
        if(!UiMotion.enabled()||!isAttachedToWindow()||Math.abs(endScale-startScale)<.00001f&&Math.abs(endX-startX)+Math.abs(endY-startY)<.01f){invalidate();return;}
        camera.restoreScale(startScale,startX,startY);
        android.animation.ValueAnimator motion=android.animation.ValueAnimator.ofFloat(0,1);cameraMotion=motion;
        motion.setDuration(260);motion.setInterpolator(UiMotion.EASE);
        motion.addUpdateListener(a->{float t=(float)a.getAnimatedValue();float scale=(float)(startScale*Math.pow(endScale/startScale,t));camera.restoreScale(scale,startX+(endX-startX)*t,startY+(endY-startY)*t);postInvalidateOnAnimation();});
        motion.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){if(cameraMotion==a)cameraMotion=null;}});motion.start();
    }
    private boolean draggingUnit;
    private Hex dragTarget;
    private java.util.function.Consumer<MarchOrders.Plan> unitDrop;
    private MarchOrders.Plan dragPlan;
    void setUnitDrop(java.util.function.Consumer<MarchOrders.Plan> drop){unitDrop=drop;}
    private boolean multiTouch;
    private int moving=-1;
    private MarchOrders.Plan route;
    void setRoute(MarchOrders.Plan route){this.route=route;invalidate();}
    private Bundle pendingCamera;
    private final Typeface font=Typeface.create("sans-serif",Typeface.NORMAL);
    private final float density;
    private static final float RADIUS=TileGeometry.RADIUS;
    private static final int PAPER=Color.rgb(232,224,199),GOLD=Color.rgb(230,191,119);
    public MapView(Context context,TileListener listener){
        super(context);this.listener=listener;density=getResources().getDisplayMetrics().density;setContentDescription("错列方格战略地图。拖动平移，双指缩放，点选城池或部队。");setFocusable(true);
        displayPrefs=context.getSharedPreferences("map-display",Context.MODE_PRIVATE);
        showMini=displayPrefs.getBoolean("navigator",true);showCommanders=displayPrefs.getBoolean("commanders",true);showUnitBars=displayPrefs.getBoolean("unitBars",true);
        BuildingAtlas.load(context);VisualAssets.load(context);CityAtlas.load(context);TerrainTiles.loadSand(context);
        fling=new android.widget.OverScroller(context);fling.setFriction(.022f);
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onSingleTapUp(MotionEvent e){
                if(!multiTouch&&!caughtMotion&&!scaler.isInProgress()){
                    Hex h=hit(e.getX(),e.getY());touchHex=h;touchUntil=android.os.SystemClock.uptimeMillis()+180;
                    MapTapTrace.tap(world,h,e.getX(),e.getY(),camera.scale,density);
                    performClick();listener.tap(h);postInvalidateOnAnimation();
                }return true;
            }
            @Override public boolean onSingleTapConfirmed(MotionEvent e){return true;}
            @Override public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){
                if(multiTouch||draggingUnit||scaler.isInProgress()||!UiMotion.enabled())return false;
                flingX=flingY=0;float max=6000*density;
                fling.fling(0,0,(int)Math.max(-max,Math.min(max,vx)),(int)Math.max(-max,Math.min(max,vy)),-1000000,1000000,-1000000,1000000);
                postInvalidateOnAnimation();return true;
            }
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!draggingUnit&&!multiTouch&&!scaler.isInProgress()){camera.pan(-dx,-dy);postInvalidateOnAnimation();}return true;}
            @Override public void onLongPress(MotionEvent e){
                World.Unit u=world==null?null:world.unit(moving);
                if(!multiTouch&&pickTargets==null&&u!=null&&u.hex.equals(hit(e.getX(),e.getY()))&&world.orders.error(u)==null){
                    draggingUnit=true;dragTarget=u.hex;dragPlan=null;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);invalidate();
                }
            }
            @Override public boolean onDoubleTap(MotionEvent e){
                if(!multiTouch&&world!=null&&moving<0&&pickTargets==null){Hex h=hit(e.getX(),e.getY());
                    if(h!=null&&world.cityAt(h)!=null)focus(h);
                    else glide(()->camera.zoom(camera.scale<camera.minScale*1.8f?camera.scale*1.8f:camera.minScale,e.getX(),e.getY()));
                }return true;
            }
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector d){zoom(camera.scale*d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}
        });
    }
    public void setWorld(World world,Hex selected,int moving){
        boolean selectionChanged=!Objects.equals(this.selected,selected),actorChanged=this.moving!=moving;
        if(this.world==world&&renderedRevision==world.commandRevision()&&renderedTurn==world.turn&&renderedPlayer==world.player&&seenTerrainRevision==world.terrainRevision){
            this.selected=selected;this.moving=moving;
            if(selectionChanged||actorChanged)updateSelection(actorChanged);
            invalidate();return;
        }
        sceneBuilds++;renderedRevision=world.commandRevision();renderedTurn=world.turn;renderedPlayer=world.player;
        boolean changed=this.world==null||this.world.width!=world.width||this.world.height!=world.height||!this.world.scenarioId.equals(world.scenarioId);boolean newTerrain=this.world!=world||changed||seenTerrainRevision!=world.terrainRevision;seenTerrainRevision=world.terrainRevision;this.world=world;this.selected=selected;this.moving=moving;CORNER_X=world.columnStaggered?TileGeometry.CORNER_Y:TileGeometry.CORNER_X;CORNER_Y=world.columnStaggered?TileGeometry.CORNER_X:TileGeometry.CORNER_Y;
        if(changed){tiles=new Hex[world.width][world.height];for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++)tiles[q][r]=new Hex(q,r);}
        objectBuckets.clear();officerIndex.clear();cityIndex.clear();
        for(World.Officer o:world.officers)officerIndex.put(o.id,o);
        threatenedCities.clear();CampaignAi threatPlanner=new CampaignAi(world);
        for(World.City c:world.cities){cityIndex.put(c.id,c);index(c,c.hex);if(threatPlanner.incoming(c)>0)threatenedCities.add(c.id);}
        for(World.Unit u:world.units)index(u,u.hex);
        for(Domestic.Facility f:world.domestic.facilities)index(f,f.hex);
        for(Domestic.Mission m:world.domestic.missions)index(m,m.hex);
        for(War.Fire f:world.war.fires())index(f,f.hex);
        for(War.Structure b:world.war.structures())index(b,b.hex);
        for(WorldEvents.Camp c:world.events.camps())index(c,c.hex);
        if(newTerrain){
            territory=new Territory(world);territoryOwners="";miniRaster=new MapRaster(world);
            // Published bitmaps may still be referenced by a hardware display list; let Android release them.
            miniTerrain=Bitmap.createBitmap(miniWidth(),miniHeight(),Bitmap.Config.ARGB_8888);
            miniTerrain.eraseColor(MapOverview.BACKGROUND);
            for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++){Hex h=tiles[q][r];World.Terrain visual=TerrainConnections.appearance(world,q,r);if(visual!=World.Terrain.VOID)paintMini(miniTerrain,h,TerrainTiles.color(visual));}
        }
        StringBuilder owners=new StringBuilder();for(World.City city:world.cities)owners.append(city.id).append(':').append(city.owner).append(';');
        if(!territoryOwners.equals(owners.toString())){
            territoryOwners=owners.toString();
            miniTerritory=Bitmap.createBitmap(miniWidth(),miniHeight(),Bitmap.Config.ARGB_8888);
            factionEdges=new byte[world.width][world.height];siteEdges=new byte[world.width][world.height];
            for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++)if(territory.siteAt(q,r)>=0){
                paintMini(miniTerritory,tiles[q][r],alpha(factionColor(territory.ownerAt(q,r)),155));
                factionEdges[q][r]=(byte)territory.boundary(q,r,false);
                siteEdges[q][r]=(byte)territory.boundary(q,r,true);
            }
            if(overview!=null)overview.cancel();
            overview=new MapOverview(world,territory,worldWidth(),worldHeight(),mapOffset());overviewBuilds++;
            if(isAttachedToWindow())overview.start(this);
        }
        cityNames.clear();frontlineCities.clear();
        for(World.City city:world.cities){
            if(territory.frontline(city.id))frontlineCities.add(city.id);
            String relation=city.owner==world.player?"我":city.owner<0?"空":world.campaign.hostile(world.player,city.owner)?"敌":"友";
            String type=city.kind==World.SiteKind.CITY?"":city.kind==World.SiteKind.GATE?"关·":"港·";
            cityNames.put(city.id,relation+"·"+type+city.name+(threatenedCities.contains(city.id)?" !":""));
        }
        updateSelection(true);
        if(changed&&getWidth()>0){stopCamera();resizeCamera();camera.fit();}invalidate();
    }
    private void updateSelection(boolean actorChanged){
        facilityCoverage=world.fieldworks.coverage(world.war.at(selected));
        World.City development=world.cityAt(selected);if(development==null)development=world.development.cityAt(selected);
        developmentSites=development!=null&&development.owner==world.player?new ArrayList<>(world.domestic.buildSites(development.id)):Collections.emptyList();
        if(!actorChanged)return;selectionBuilds++;
        World.Unit actor=world.unit(moving);reachable=world.orders.marchReachable(actor);attackTargets.clear();
        if(world.orders.error(actor)==null){
            for(World.Unit target:world.fieldUnits())if(target.id!=actor.id)addAttackTarget(actor,target.hex,world.war.attackError(actor.id,target.id)==null);
            for(World.City city:world.cities)if(world.siegeError(actor.id,city.id)==null)
                for(Hex h:SiteFootprint.cells(city))addAttackTarget(actor,h,h.equals(world.siegeHit(actor,city,h)));
            for(Domestic.Facility f:world.domestic.facilities)addAttackTarget(actor,f.hex,world.war.facilityAttackError(actor.id,f.hex)==null);
            for(War.Structure s:world.war.structures())addAttackTarget(actor,s.hex,world.war.structureAttackError(actor.id,s.hex)==null);
        }
    }
    @Override public boolean isOpaque(){return true;}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(overview!=null)overview.start(this);}
    @Override protected void onDetachedFromWindow(){stopCamera();if(overview!=null)overview.cancel();super.onDetachedFromWindow();}
    private float mapOffset(){return world!=null&&world.sourceMapWidth>0?(world.height-1)/2:0;}
    private float x(Hex h){return TileGeometry.projectedX(h.q,h.r,mapOffset(),world.columnStaggered);}
    private float y(Hex h){return TileGeometry.projectedY(h.q,h.r,mapOffset(),world.columnStaggered);}
    private float worldWidth(){if(world.columnStaggered)return TileGeometry.DX*(world.sourceColumns()-1)+RADIUS*2;return TileGeometry.DX*((world.sourceMapWidth>0?world.sourceMapWidth-0.5f:world.width-1+(world.height-1)*.5f))+RADIUS*2;}
    private float worldHeight(){return TileGeometry.DY*(world.columnStaggered?world.sourceRows()-.5f:world.height-1)+RADIUS*2;}
    private void resizeCamera(){if(world!=null&&getWidth()>0&&getHeight()>0){camera.columnOffset=mapOffset();camera.columnStaggered=world.columnStaggered;camera.resize(getWidth(),getHeight(),worldWidth(),worldHeight(),RADIUS,density);}}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);stopCamera();resizeCamera();applyPendingCamera();}
    public void fit(){pendingCamera=null;if(world==null||getWidth()==0)return;resizeCamera();glide(camera::fit);}
    public void focus(Hex h){if(world==null||h==null)return;if(getWidth()==0){post(()->focus(h));return;}glide(()->{camera.focus(x(h),y(h));camera.pan(-occludedRight/2f,-occludedBottom/2f);});}
    public void center(Hex h){if(world!=null&&h!=null){stopCamera();camera.centerOn(x(h),y(h));invalidate();}}
    private void zoom(float scale,float fx,float fy){stopCamera();camera.zoom(scale,fx,fy);postInvalidateOnAnimation();}
    void saveCamera(Bundle b){b.putBoolean("mapNavigator",showMini);b.putFloat("cameraRatio",camera.scale/camera.minScale);b.putFloat("cameraScaleDp",camera.scale/density);b.putFloat("cameraX",camera.centerX());b.putFloat("cameraY",camera.centerY());}
    void restoreCamera(Bundle b){stopCamera();showMini=b.getBoolean("mapNavigator",showMini);pendingCamera=new Bundle(b);applyPendingCamera();}
    private void applyPendingCamera(){if(pendingCamera!=null&&getWidth()>0&&getHeight()>0){if(pendingCamera.containsKey("cameraScaleDp"))camera.restoreScale(pendingCamera.getFloat("cameraScaleDp")*density,pendingCamera.getFloat("cameraX"),pendingCamera.getFloat("cameraY"));else camera.restore(pendingCamera.getFloat("cameraRatio",1),pendingCamera.getFloat("cameraX"),pendingCamera.getFloat("cameraY"));pendingCamera=null;invalidate();}}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return true;
        if(criticalHit!=null&&criticalSkip!=null){if(e.getActionMasked()==MotionEvent.ACTION_UP){criticalSkip.run();performClick();}return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){caughtMotion=cameraMoving();stopCamera();multiTouch=false;layoutNavigator();miniButtonGesture=miniButton.contains(e.getX(),e.getY());miniGesture=!miniButtonGesture&&showMini&&miniRect.contains(e.getX(),e.getY());}
        if(miniButtonGesture){
            if(e.getPointerCount()>1||e.getActionMasked()==MotionEvent.ACTION_CANCEL)multiTouch=true;
            if(e.getActionMasked()==MotionEvent.ACTION_UP){if(!multiTouch&&miniButton.contains(e.getX(),e.getY())){toggleNavigator();performClick();}miniButtonGesture=false;}return true;
        }
        if(miniGesture){
            if(e.getPointerCount()>1||e.getActionMasked()==MotionEvent.ACTION_CANCEL)multiTouch=true;
            if(!multiTouch&&e.getPointerCount()==1&&(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE)){
                float rx=(e.getX()-miniRect.left)/miniRect.width()*miniWidth();
                float ry=(e.getY()-miniRect.top)/miniRect.height()*miniHeight();
                Hex target=miniRaster.at(rx,ry);
                if(target!=null){camera.centerOn(x(target),y(target));invalidate();}
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
        }
        if(editorMode){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){editorTwoFingers=false;editorCapturing=editorDrawing;if(editorCapturing){scaler.onTouchEvent(e);editorStroke.event(MotionEvent.ACTION_DOWN,hit(e.getX(),e.getY()));return true;}}
            if(e.getPointerCount()>1){
                if(editorCapturing){editorStroke.event(MotionEvent.ACTION_CANCEL,null);editorCapturing=false;}
                float fx=0,fy=0;for(int i=0;i<e.getPointerCount();i++){fx+=e.getX(i);fy+=e.getY(i);}fx/=e.getPointerCount();fy/=e.getPointerCount();
                if(editorTwoFingers&&action==MotionEvent.ACTION_MOVE)camera.pan(fx-editorFocusX,fy-editorFocusY);editorFocusX=fx;editorFocusY=fy;editorTwoFingers=true;multiTouch=true;
            }
            if(editorCapturing){scaler.onTouchEvent(e);editorStroke.event(action,hit(e.getX(),e.getY()));if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){editorCapturing=false;performClick();}return true;}
        }
        if(e.getPointerCount()>1){multiTouch=true;draggingUnit=false;dragTarget=null;dragPlan=null;invalidate();}
        if(draggingUnit){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_CANCEL){draggingUnit=false;dragTarget=null;dragPlan=null;invalidate();return true;}
            boolean onMap=e.getX()>=0&&e.getY()>=0&&e.getX()<getWidth()&&e.getY()<getHeight()&&!miniButton.contains(e.getX(),e.getY())&&(!showMini||!miniRect.contains(e.getX(),e.getY()));
            Hex h=onMap?hit(e.getX(),e.getY()):null;
            if(!Objects.equals(h,dragTarget)){
                dragTarget=h;World.Unit u=world.unit(moving);
                dragPlan=u!=null&&h!=null&&!h.equals(u.hex)&&reachable.containsKey(h)?world.marches.previewMove(u.id,h):null;
                if(dragPlan!=null&&(!dragPlan.valid()||dragPlan.stepsNow!=dragPlan.path.size()-1))dragPlan=null;
            }
            if(action==MotionEvent.ACTION_UP){
                MarchOrders.Plan plan=dragPlan;draggingUnit=false;dragTarget=null;dragPlan=null;
                MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);gestures.onTouchEvent(cancel);cancel.recycle();multiTouch=true;
                if(plan!=null&&unitDrop!=null)unitDrop.accept(plan);else announceForAccessibility("移动已取消，请放到本旬可达的空格");
            }
            invalidate();return true;
        }
        scaler.onTouchEvent(e);gestures.onTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)multiTouch=true;
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private Hex hit(float px,float py){
        if(world==null)return null;
        float wx=(px-camera.x)/camera.scale,wy=(py-camera.y)/camera.scale;
        int ir=TileGeometry.projectedRow(wx,wy,world.columnStaggered),iq=TileGeometry.projectedColumn(wx,wy,ir,mapOffset(),world.columnStaggered);
        Hex exact=new Hex(iq,ir);
        // A city label may not turn exterior/padding under the pointer into a tile.
        // Valid impassable terrain remains selectable at every level of detail.
        if(editorMode)return world.sourceInside(exact)?exact:null;
        if(!world.inside(exact))return null;
        // Preserve precise tile commands while a unit is selected, including adjacent movement.
        if(moving>=0||pickTargets!=null)return world.inside(exact)?exact:null;
        if(world.inside(exact)&&(world.cityAt(exact)!=null||world.unitAt(exact)!=null||world.domestic.at(exact)!=null))return exact;
        if(camera.scale*RADIUS>=12*density)return world.inside(exact)?exact:null;
        for(Domestic.Mission mission:world.domestic.missions)if(mission.transport&&mission.hex.equals(exact))return exact;
        if(camera.scale*RADIUS>=12*density&&world.development.cityAt(exact)!=null)return exact;
        for(Map.Entry<Integer,RectF> entry:cityLabelBounds.entrySet())if(entry.getValue().contains(px,py)){World.City c=cityIndex.get(entry.getKey());if(c!=null)return c.hex;}
        return world.inside(exact)?exact:null;
    }
    private float[] CORNER_X=TileGeometry.CORNER_X;
    private float[] CORNER_Y=TileGeometry.CORNER_Y;
    private void polygon(float cx,float cy,float radius){path.rewind();for(int i=0;i<6;i++){float px=cx+CORNER_X[i]*radius,py=cy+CORNER_Y[i]*radius;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();}
    private void fill(Canvas c,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(path,paint);}
    private void stroke(Canvas c,int color,float width){paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}
    private void label(Canvas c,String value,float x,float y,float size,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(font);c.drawText(value,x,y,paint);}
    private int factionColor(int owner){return FactionColors.color(world,owner);}
    private static int alpha(int color,int opacity){return (color&0x00ffffff)|(opacity<<24);}
    private void drawTerritory(Canvas canvas,int q,int r,float cx,float cy,float scale){
        if(territoryMode==0||territory==null||territory.siteAt(q,r)<0)return;
        int color=factionColor(territory.ownerAt(q,r));polygon(cx,cy,RADIUS);
        fill(canvas,alpha(color,territoryMode==2?138+(territory.siteAt(q,r)%3)*10:158));
    }
    private void addBorders(Path borders,int mask,float cx,float cy){
        for(int side=0;side<6;side++)if((mask&(1<<side))!=0){
            int a=(6-side)%6,b=(a+1)%6;
            borders.moveTo(cx+CORNER_X[a]*RADIUS,cy+CORNER_Y[a]*RADIUS);
            borders.lineTo(cx+CORNER_X[b]*RADIUS,cy+CORNER_Y[b]*RADIUS);
        }
    }
    @Override protected void onDraw(Canvas canvas){
        long drawStart=System.nanoTime();lastTilesVisited=0;lastObjectsVisited=0;super.onDraw(canvas);canvas.drawColor(MapOverview.BACKGROUND);if(world==null)return;
        float scale=camera.scale,offsetX=camera.x,offsetY=camera.y;
        boolean detail=scale*RADIUS>=12*density;collectVisible();
        canvas.save();canvas.translate(offsetX,offsetY);canvas.scale(scale,scale);
        int r0=camera.firstRow(world.height,RADIUS*2),r1=camera.lastRow(world.height,RADIUS*2);
        if(!detail&&overview!=null)overview.draw(canvas,territoryMode);
        else for(int r=r0;r<=r1;r++)for(int q=camera.firstColumn(r,world.width,RADIUS*2);q<=camera.lastColumn(r,world.width,RADIUS*2);q++){
            lastTilesVisited++;Hex h=tiles[q][r];float cx=x(h),cy=y(h);float sx=cx*scale+offsetX,sy=cy*scale+offsetY;
            if(sx<-RADIUS*scale||sy<-RADIUS*scale||sx>getWidth()+RADIUS*scale||sy>getHeight()+RADIUS*scale)continue;
            World.Terrain t=TerrainConnections.appearance(world,q,r);
            if(t==World.Terrain.VOID)continue;
            boolean exterior=world.terrain[q][r]==World.Terrain.VOID;
            if(detail)terrainTiles.draw(canvas,world,q,r,cx,cy);
            else {polygon(cx,cy,RADIUS-.3f);fill(canvas,TerrainTiles.color(t));}
            if(!exterior&&TerrainArt.connection(t)==TerrainArt.Connection.NONE){polygon(cx,cy,RADIUS-.3f);stroke(canvas,Color.argb(40,13,37,35),.7f);}
            if(!exterior)drawTerritory(canvas,q,r,cx,cy,scale);
        }
        if(detail&&territoryMode>0){
            // Only visible border segments: never send a continent-sized path to the GPU.
            path.rewind();byte[][] edges=territoryMode==2?siteEdges:factionEdges;
            for(int r=r0;r<=r1;r++)for(int q=camera.firstColumn(r,world.width,RADIUS*2);q<=camera.lastColumn(r,world.width,RADIUS*2);q++)
                if(edges[q][r]!=0)addBorders(path,edges[q][r],x(tiles[q][r]),y(tiles[q][r]));
            stroke(canvas,0xbfe3ebcf,Math.max(1,density/scale));
        }
        if(pickTargets==null)for(Map.Entry<Hex,Integer> entry:reachable.entrySet()){
            Hex h=entry.getKey();if(entry.getValue()<=0||!camera.visible(x(h),y(h),RADIUS*scale))continue;
            polygon(x(h),y(h),RADIUS-1);fill(canvas,0x6e3edad1);stroke(canvas,0xe175f4ea,Math.max(1,1.2f*density/scale));
        }
        if(pickTargets==null)for(Hex h:facilityCoverage){
            if(!camera.visible(x(h),y(h),RADIUS*scale))continue;
            polygon(x(h),y(h),RADIUS-1);fill(canvas,0x55f1bd62);stroke(canvas,0xfff1bd62,Math.max(1,1.4f*density/scale));
        }
        drawRoute(canvas);
        if(draggingUnit&&dragTarget!=null){
            if(dragPlan!=null)drawArrows(canvas,dragPlan.path,0xff6ddcc5);
            polygon(x(dragTarget),y(dragTarget),RADIUS-2);stroke(canvas,dragPlan==null?0xffff7979:0xff6ddcc5,3*density/scale);
            label(canvas,dragPlan==null?"移出范围 · 松手取消":"松手移动 · 消耗"+dragPlan.cost,x(dragTarget),y(dragTarget)-RADIUS-12*density/scale,12*density/scale,PAPER);
        }
        if(detail&&moving<0&&pickTargets==null){
            for(Hex parcel:developmentSites){
                if(!camera.visible(x(parcel),y(parcel),RADIUS*scale))continue;polygon(x(parcel),y(parcel),RADIUS-3);fill(canvas,0x3ae6bf77);stroke(canvas,0xffe6bf77,1.2f*density/scale);label(canvas,"＋",x(parcel),y(parcel)+6,18,0xffffe0a0);
            }
        }
        if(selected!=null&&world.cityAt(selected)==null){polygon(x(selected),y(selected),RADIUS-2);fill(canvas,0x20e6bf77);stroke(canvas,GOLD,Math.max(2,2*density/scale));}
        long touchNow=android.os.SystemClock.uptimeMillis();
        if(touchHex!=null&&touchNow<touchUntil&&UiMotion.enabled()){
            float t=1-(touchUntil-touchNow)/180f;polygon(x(touchHex),y(touchHex),RADIUS*(1+t*.35f));
            stroke(canvas,alpha(0xffa7ffe0,(int)(150*(1-t))),2*density/scale);postInvalidateOnAnimation();
        }
        for(Object object:visibleObjects)if(object instanceof War.Fire){War.Fire f=(War.Fire)object;
            float cx=x(f.hex),cy=y(f.hex);polygon(cx,cy,RADIUS-2);fill(canvas,Color.argb(145,227,81,28));if(detail)label(canvas,"火",cx,cy+5,18,PAPER);
        }
        for(Object object:visibleObjects)if(object instanceof War.Structure){War.Structure s=(War.Structure)object;
            float cx=x(s.hex),cy=y(s.hex);int color=factionColor(s.owner);
            if(detail){canvas.save();canvas.translate(cx,cy);models.structure(canvas,s.kind,color);if(!s.complete)models.scaffolding(canvas);canvas.restore();label(canvas,s.kind.label+(s.complete?"":"·建"),cx,cy+26,9,PAPER);bar(canvas,cx,cy+17,34,s.hp/(float)s.kind.hp,s.complete?color:GOLD);}
            else {paint.setColor(color);canvas.drawRect(cx-12,cy-12,cx+12,cy+12,paint);}
            if(detail)label(canvas,String.valueOf(s.hp),cx,cy-30,9,PAPER);
        }
        for(Object object:visibleObjects)if(object instanceof Domestic.Facility){Domestic.Facility f=(Domestic.Facility)object;float cx=x(f.hex),cy=y(f.hex);int color=factionColor(cityIndex.get(f.cityId).owner);
            if(detail){canvas.save();canvas.translate(cx,cy);models.facility(canvas,f.kind,color);if(f.remaining>0)models.scaffolding(canvas);canvas.restore();label(canvas,f.kind.label+"·"+f.level,cx,cy+26,9,PAPER);bar(canvas,cx,cy+17,34,f.hp/(float)f.maxHp(),f.hp<f.maxHp()?GOLD:color);if(f.remaining>0)label(canvas,"建·剩"+f.remaining,cx,cy-30,9,GOLD);}
            else {paint.setColor(color);canvas.drawRect(cx-10,cy-10,cx+10,cy+10,paint);}
        }
        for(Object object:visibleObjects)if(object instanceof WorldEvents.Camp){WorldEvents.Camp camp=(WorldEvents.Camp)object;float cx=x(camp.hex),cy=y(camp.hex);paint.setColor(Color.rgb(173,77,59));canvas.drawRect(cx-15,cy-15,cx+15,cy+15,paint);label(canvas,"寨",cx,cy+5,16,PAPER);if(detail)label(canvas,camp.tribe.label+" "+camp.troops,cx,cy-20,10,PAPER);}
        for(Object object:visibleObjects)if(object instanceof World.City)drawCity(canvas,(World.City)object,detail);
        World.City selectedSite=world.cityAt(selected);if(selectedSite!=null){siteBoundary(selectedSite);stroke(canvas,GOLD,Math.max(2,2*density/scale));}
        if(selected!=null&&world.unitAt(selected)!=null){polygon(x(selected),y(selected),RADIUS-2);stroke(canvas,0xff70ffca,Math.max(2,2*density/scale));}
        for(Object object:visibleObjects)if(object instanceof World.Unit&&!(object instanceof Domestic.Mission)){World.Unit u=(World.Unit)object;if(replayHides(u))continue;
            if(detail)drawUnit(canvas,u);else {paint.setColor(factionColor(u.owner));canvas.drawCircle(x(u.hex),y(u.hex),14,paint);
                if(scale*RADIUS>=5*density){canvas.save();canvas.translate(x(u.hex),y(u.hex));canvas.scale(.6f,.6f);if(world.army.water(u.hex))models.shipIcon(canvas,u.ship,PAPER);else models.weaponIcon(canvas,u.weapon,PAPER);canvas.restore();}}
        }
        for(Hex h:pickTargets==null?attackTargets:pickTargets)if(camera.visible(x(h),y(h),RADIUS)){
            polygon(x(h),y(h),RADIUS-1);if(pickTargets!=null)fill(canvas,0x555be6bf);stroke(canvas,pickTargets==null?0xffff987a:0xff70ffca,Math.max(2,2*density/scale));
        }
        for(Object object:visibleObjects)if(object instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)object;if(replayHides(m))continue;if(m.owner!=world.player&&!m.transport)continue;float cx=x(m.hex),cy=y(m.hex);if(!detail){polygon(cx,cy,Math.max(8,3*density/scale));fill(canvas,factionColor(m.owner));stroke(canvas,PAPER,density/scale);continue;}paint.setColor(factionColor(m.owner));canvas.drawRoundRect(cx-17,cy-10,cx+17,cy+10,4,4,paint);paint.setColor(PAPER);canvas.drawCircle(cx-11,cy+13,4,paint);canvas.drawCircle(cx+11,cy+13,4,paint);label(canvas,m.transport?"运":"调",cx,cy+5,15,Color.BLACK);if(m.transport){if(m.stopped||!m.waiting.isEmpty())label(canvas,"!",cx+23,cy+4,16,GOLD);}}
        drawReplayWorld(canvas);
        drawTacticPreview(canvas);
        drawEditor(canvas,r0,r1,detail);
        long remaining=impactUntil-android.os.SystemClock.uptimeMillis();
        if(impactHex!=null&&remaining>0){
            float progress=1-remaining/450f;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(defeatImpact?3:2);
            paint.setColor(Color.argb((int)(220*(1-progress)),255,defeatImpact?178:226,106));
            canvas.drawCircle(x(impactHex),y(impactHex),12+progress*22,paint);paint.setStyle(Paint.Style.FILL);postInvalidateOnAnimation();
        }
        canvas.restore();
        drawMapLabels(canvas,detail);
        drawPreviewSelection(canvas);
        if(territoryMode>0){
            String caption=openingPreview?"开局预览 · "+world.governance.label(previewFaction):territoryMode==1?"势力范围 · 橙圈接壤 · 红!敌军逼近":"据点辖区 · 橙圈接壤 · 红!敌军逼近";
            int site=territory.siteAt(selected);World.City city=cityIndex.get(site);
            if(city!=null&&!openingPreview)caption=city.name+"辖区 · "+world.faction(city.owner)+(territory.frontline(site)?" · 前线":"");
            paint.setColor(0xe612272b);canvas.drawRoundRect(8*density,8*density,Math.min(getWidth()-8*density,258*density),38*density,6*density,6*density,paint);
            paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(12*density);paint.setColor(PAPER);canvas.drawText(caption,16*density,28*density,paint);
        }
        if(!openingPreview)drawNavigator(canvas);
        drawReplayCaption(canvas);
        if(criticalHit!=null&&criticalScene!=null)criticalScene.draw(canvas,getWidth()-occludedRight,getHeight()-occludedBottom,criticalPhase);

        lastDrawNanos=System.nanoTime()-drawStart;
    }
    /** Animates detached sprites over the pre-action render state; the actual route is immutable. */
    private void drawReplayWorld(Canvas c){
        TurnJournal.Event e=replayEvent;if(e==null)return;float t=replayFraction,scale=camera.scale;
        Hex from=e.start,to=e.target;float px=from==null?0:x(from),py=from==null?0:y(from);
        if(replayActor!=null){
            Hex tile=from;
            if(e.kind==TurnJournal.Kind.MOVE&&e.path.size()>1){
                float step=t*(e.path.size()-1);int i=Math.min(e.path.size()-2,(int)step);float part=step-i;
                Hex a=e.path.get(i),b=e.path.get(i+1);tile=part<.5f?a:b;px=x(a)+(x(b)-x(a))*part;py=y(a)+(y(b)-y(a))*part;
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density/scale);paint.setColor(0x8873ead2);
                for(int j=1;j<e.path.size();j++)c.drawLine(x(e.path.get(j-1)),y(e.path.get(j-1)),x(e.path.get(j)),y(e.path.get(j)),paint);
                paint.setStyle(Paint.Style.FILL);
            }else if(from!=null&&to!=null&&(e.kind==TurnJournal.Kind.ATTACK||e.kind==TurnJournal.Kind.TACTIC)){
                float pulse=(float)Math.sin(Math.min(1,t/.55f)*Math.PI)*.22f;px+=(x(to)-px)*pulse;py+=(y(to)-py)*pulse;
            }else if(from!=null&&to!=null&&e.kind==TurnJournal.Kind.ENTER){px+=(x(to)-px)*t;py+=(y(to)-py)*t;}
            if(tile!=null){replayActor.hex=tile;c.save();c.translate(px-x(tile),py-y(tile));drawUnit(c,replayActor);c.restore();
                World.Officer officer=officerIndex.get(replayActor.officerId);if(officer!=null)label(c,officer.name,px,py-30-3*density/scale,11*density/scale,GOLD);}
        }
        if(to!=null&&e.kind!=TurnJournal.Kind.MOVE&&e.kind!=TurnJournal.Kind.ENTER){
            float tx=x(to),ty=y(to),pulse=(float)Math.sin(t*Math.PI);
            boolean recovery=e.kind==TurnJournal.Kind.RECOVER;
            int effectColor=recovery?0xff81e2c4:e.kind==TurnJournal.Kind.PLOT?0xffbcadff:0xffffcc8c;
            if(from!=null&&(recovery||e.kind==TurnJournal.Kind.FACILITY_ATTACK||e.kind==TurnJournal.Kind.FACILITY_COUNTER)){
                paint.setColor(effectColor);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density/scale);
                c.drawCircle(x(from),y(from),(14+10*pulse)*density/scale,paint);paint.setStyle(Paint.Style.FILL);
                if(recovery)label(c,"♪",x(from),y(from)-(18+12*t)*density/scale,20*density/scale,effectColor);
            }
            if(from!=null){float travel=Math.min(1,t/.48f);float ax=x(from),ay=y(from);paint.setColor(effectColor);paint.setStrokeWidth(2*density/scale);
                c.drawLine(ax,ay,ax+(tx-ax)*travel,ay+(ty-ay)*travel,paint);c.drawCircle(ax+(tx-ax)*travel,ay+(ty-ay)*travel,4*density/scale,paint);}
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((1+2*pulse)*density/scale);paint.setColor(recovery?0xff81e2c4:e.kind==TurnJournal.Kind.PLOT?0xffbeadff:0xffffae77);
            c.drawCircle(tx,ty,(16+20*t)*density/scale,paint);paint.setStyle(Paint.Style.FILL);
            label(c,e.label,tx,ty-24*density/scale,13*density/scale,e.kind==TurnJournal.Kind.PLOT?0xffd6caff:GOLD);
        }
        if(t>.35f){Map<Hex,Integer> lanes=new HashMap<>();
            for(TurnJournal.Impact hit:e.impacts){if(!replayPoint(hit.hex))continue;int lane=lanes.getOrDefault(hit.hex,0);lanes.put(hit.hex,lane+1);
                float cx=x(hit.hex),cy=y(hit.hex);if(hit.loss){paint.setColor(Color.argb((int)(95*Math.sin(t*Math.PI)),255,80,55));c.drawCircle(cx,cy,23,paint);}
                label(c,hit.text,cx,cy-(35+lane*16+16*t)*density/scale,12*density/scale,hit.loss?0xffffaa91:0xffb6ffdf);
            }
        }
    }
    private void drawReplayCaption(Canvas c){
        TurnJournal.Event e=replayEvent;if(e==null)return;
        float left=8*density,right=getWidth()-occludedRight-8*density,bottom=getHeight()-occludedBottom-8*density;
        if(right-left<100*density||bottom<45*density)return;
        paint.setColor(0xee10242b);c.drawRoundRect(left,bottom-42*density,right,bottom,8*density,8*density,paint);
        paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);paint.setTextAlign(Paint.Align.LEFT);paint.setColor(PAPER);
        String text=world.faction(e.owner)+" · "+e.label+"  "+e.message.replace('\n',' ');
        int fit=paint.breakText(text,true,right-left-22*density,null);if(fit<text.length())text=text.substring(0,Math.max(0,fit-1))+"…";
        c.drawText(text,left+10*density,bottom-16*density,paint);
    }
    private void drawRoute(Canvas c){
        if(route==null)return;float stroke=Math.max(2,3*density/camera.scale);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setStrokeCap(Paint.Cap.ROUND);
        for(int i=1;i<route.path.size();i++){
            Hex a=route.path.get(i-1),b=route.path.get(i);boolean now=i<=route.stepsNow;
            paint.setColor(now?0xff6ddcc5:0xffebc979);paint.setPathEffect(now?null:new android.graphics.DashPathEffect(new float[]{stroke*2,stroke*2},0));
            c.drawLine(x(a),y(a),x(b),y(b),paint);
        }
        paint.setPathEffect(null);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStyle(Paint.Style.FILL);
        if(route.target!=null){float cx=x(route.target),cy=y(route.target);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(route.valid()?0xffebc979:0xffff927d);c.drawCircle(cx,cy,RADIUS*.8f,paint);paint.setStyle(Paint.Style.FILL);}
        if(!route.path.isEmpty()){Hex last=route.path.get(route.path.size()-1);paint.setColor(0xffebc979);c.drawCircle(x(last),y(last),stroke*1.5f,paint);}
    }
    private int miniWidth(){return miniRaster.width;}
    private int miniHeight(){return miniRaster.height;}
    private void paintMini(Bitmap bitmap,Hex h,int color){
        int x=miniRaster.left(h),y=miniRaster.top(h);
        // All writes are derived from a valid source cell, never axial padding.
        bitmap.setPixel(x,y,color);bitmap.setPixel(x+1,y,color);
        bitmap.setPixel(x,y+1,color);bitmap.setPixel(x+1,y+1,color);
    }
    private void layoutNavigator(){
        float mw=Math.min(144*density,getWidth()*.34f),mh=Math.min(126*density,getHeight()*.3f);
        float right=getWidth()-occludedRight-8*density;
        mw=Math.min(mw,Math.max(72*density,(getWidth()-occludedRight)*.42f));right=Math.max(mw+8*density,right);
        miniButton.set(right-Math.max(96*density,mw),6*density,right,46*density);
        miniRect.set(right-mw,48*density,right,48*density+mh);
    }
    private float miniX(float wx,float wy){
        return miniRect.left+miniRaster.rasterX(wx)/miniWidth()*miniRect.width();
    }
    private float miniY(float wy){return miniRect.top+miniRaster.rasterY(wy)/miniHeight()*miniRect.height();}
    private void drawNavigator(Canvas c){
        layoutNavigator();paint.setColor(0xf010272b);c.drawRoundRect(miniButton,6*density,6*density,paint);
        label(c,showMini?"小地图  − 收起":"小地图  ＋ 展开",miniButton.centerX(),miniButton.centerY()+4*density,11*density,PAPER);
        if(!showMini||miniTerrain==null)return;
        paint.setColor(0xee10272b);c.drawRect(miniRect.left-2,miniRect.top-2,miniRect.right+2,miniRect.bottom+2,paint);
        c.drawBitmap(miniTerrain,null,miniRect,paint);
        if(territoryMode>0&&miniTerritory!=null)c.drawBitmap(miniTerritory,null,miniRect,paint);
        for(World.City city:world.cities){
            paint.setColor(factionColor(city.owner));
            for(Hex h:SiteFootprint.cells(city))if(world.inside(h)){
                float cx=miniX(x(h),y(h)),cy=miniY(y(h));
                float hx=miniRect.width()/miniWidth(),hy=miniRect.height()/miniHeight();
                c.drawRect(cx-hx,cy-hy,cx+hx,cy+hy,paint);
            }
            c.drawCircle(miniX(x(city.hex),y(city.hex)),miniY(y(city.hex)),(city.kind==World.SiteKind.CITY?2:1)*density,paint);
        }
        for(World.Unit unit:world.fieldUnits()){paint.setColor(factionColor(unit.owner));c.drawCircle(miniX(x(unit.hex),y(unit.hex)),miniY(y(unit.hex)),1.3f*density,paint);}
        // The actual viewport, projected with the same source/axial transform as taps.
        float left=-camera.x/camera.scale,top=-camera.y/camera.scale,right=(getWidth()-camera.x)/camera.scale,bottom=(getHeight()-camera.y)/camera.scale;
        path.reset();path.moveTo(miniX(left,top),miniY(top));path.lineTo(miniX(right,top),miniY(top));path.lineTo(miniX(right,bottom),miniY(bottom));path.lineTo(miniX(left,bottom),miniY(bottom));path.close();
        c.save();c.clipRect(miniRect);paint.setColor(0x30ffffff);c.drawPath(path,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(GOLD);paint.setStrokeWidth(1.5f*density);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);c.restore();
    }
    private void siteBoundary(World.City city){
        path.rewind();for(Hex h:SiteFootprint.cells(city)){
            List<Hex> neighbors=h.neighbors();for(int d=0;d<6;d++)if(!SiteFootprint.contains(city,neighbors.get(d))){
                int a=TileGeometry.start(d),b=TileGeometry.end(d);
                path.moveTo(x(h)+CORNER_X[a]*RADIUS,y(h)+CORNER_Y[a]*RADIUS);
                path.lineTo(x(h)+CORNER_X[b]*RADIUS,y(h)+CORNER_Y[b]*RADIUS);
            }
        }
    }
    private void drawCity(Canvas c,World.City city,boolean detail){float scale=camera.scale;float cx=x(city.hex),cy=y(city.hex);int owner=factionColor(city.owner);
        // Buildings remain visible at NEAR, MID and FAR. Troops draw later, above roofs.
        CityAtlas.drawMap(c,world,city,cx,cy,scale,owner);
        if(territoryMode>0&&frontlineCities.contains(city.id)){paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffffbb65);paint.setStrokeWidth(2*density/scale);c.drawCircle(cx,cy,Math.max(24,5*density/scale),paint);paint.setStyle(Paint.Style.FILL);}
        if(territoryMode>0&&threatenedCities.contains(city.id))label(c,"!",cx,cy-20*density/scale,16*density/scale,0xffff5555);
        if(detail)bar(c,cx,cy+10,32,city.defense/(float)world.campaign.defenseCap(city),owner);
    }

    /** Text lives in screen coordinates: readable dp/sp size, collision rejection and edge clamping. */
    String siteLabel(World.City city){return openingPreview?(city.owner<0?"":world.governance.label(city.owner)):cityNames.get(city.id);}
    private final Set<Integer> previewLabels=new HashSet<>();
    private void drawMapLabels(Canvas c,boolean detail){
        previewLabels.clear();
        labelBounds.clear();cityLabelBounds.clear();lastLabels=0;labelPoolUsed=0;
        if(territoryMode>0)labelBounds.add(labelBox(0,0,Math.min(getWidth(),266*density),42*density));
        layoutNavigator();labelBounds.add(labelBox(miniButton.left,0,getWidth(),showMini?miniRect.bottom+4*density:miniButton.bottom));
        // Selection first, then cities in stable engine order. No pathfinding or sorting while drawing.
        World.City chosen=selected==null?null:world.cityAt(selected);
        if(chosen!=null)cityName(c,chosen,true,detail);
        for(Object object:visibleObjects)if(object instanceof World.City&&object!=chosen)cityName(c,(World.City)object,false,detail);
        if(detail&&!openingPreview)for(Object object:visibleObjects)if(object instanceof World.Unit){
            World.Unit u=(World.Unit)object;if(replayHides(u))continue;if(u instanceof Domestic.Mission&&!((Domestic.Mission)u).transport)continue;
            boolean active=u.id==moving;World.Officer officer=officerIndex.get(u.officerId);if(officer==null)continue;
            String relation=u.owner==world.player?"我":world.campaign.hostile(world.player,u.owner)?"敌":"友";
            String state=u.acted?"✓":u.food<Logistics.foodUse(world,u)*3?"粮!":u instanceof Domestic.Mission&&(((Domestic.Mission)u).stopped||!((Domestic.Mission)u).waiting.isEmpty())?"!":"";
            String name=relation+"·"+(u instanceof Domestic.Mission?"运":world.army.water(u.hex)?u.ship.label:u.weapon.label)+state;
            if(showCommanders)name+=" "+officer.name;
            if(active)name+=" "+u.troops+"兵 / "+u.energy+"气";
            if(showUnitBars){float sx=x(u.hex)*camera.scale+camera.x,sy=y(u.hex)*camera.scale+camera.y+19*camera.scale;
                unitBar(c,sx,sy,32*density,u.troops/(float)world.government.commandLimit(u.officerId),0xff79cf94);
                unitBar(c,sx,sy+5*density,32*density,u.energy/(float)world.campaign.energyCap(u.owner),0xff64baff);
            }
            placeLabel(c,name,x(u.hex)*camera.scale+camera.x,y(u.hex)*camera.scale+camera.y-30*camera.scale,active?GOLD:PAPER,-1);
        }
    }
    private void cityName(Canvas c,World.City city,boolean active,boolean detail){
        float sx=x(city.hex)*camera.scale+camera.x,sy=y(city.hex)*camera.scale+camera.y;
        if(sx<0||sy<0||sx>getWidth()||sy>getHeight())return;
        if(openingPreview&&(city.owner<0||previewLabels.contains(city.owner)))return;
        String name=siteLabel(city);
        int before=lastLabels;
        placeLabel(c,name,sx,sy+Math.max(8*density,16*camera.scale),active?GOLD:PAPER,city.id);
        if(openingPreview&&lastLabels>before)previewLabels.add(city.owner);
        if(active&&detail&&!openingPreview)placeLabel(c,"金 "+city.gold+" · 粮 "+city.food+" · 兵 "+city.troops,sx,sy+Math.max(8*density,16*camera.scale)+25*density,PAPER,-1);
    }
    private void placeLabel(Canvas c,String text,float sx,float sy,int color,int cityId){
        float fontSize=11*getResources().getDisplayMetrics().scaledDensity;
        paint.setTextSize(fontSize);paint.setTypeface(font);paint.setTextAlign(Paint.Align.LEFT);
        float width=Math.min(getWidth()-8*density,paint.measureText(text)+10*density),height=fontSize*1.5f;
        if(width<=0||height>getHeight()-8*density)return;
        float left=Math.max(4*density,Math.min(getWidth()-4*density-width,sx-width/2));
        for(int lane=0;lane<3;lane++){
            float top=Math.max(4*density,Math.min(getHeight()-4*density-height,sy+lane*(height+2*density)));
            RectF box=labelBox(left,top,left+width,top+height);boolean blocked=false;
            for(RectF occupied:labelBounds)if(RectF.intersects(box,occupied)){blocked=true;break;}if(blocked)continue;
            labelBounds.add(box);if(cityId>=0)cityLabelBounds.put(cityId,box);lastLabels++;
            paint.setColor(0xee10252b);c.drawRoundRect(box,4*density,4*density,paint);
            paint.setColor(color);c.save();c.clipRect(box);c.drawText(text,left+5*density,top+(height-fontSize)/2+fontSize*.83f,paint);c.restore();return;
        }
    }

    private void drawTacticPreview(Canvas c){
        if(tacticPreview==null)return;
        drawArrows(c,tacticPreview.actorPath,0xff70ffca);drawArrows(c,tacticPreview.targetPath,0xffffb261);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density/camera.scale);paint.setColor(0xffffb261);
        for(Hex h:tacticPreview.riskHexes)c.drawCircle(x(h),y(h),RADIUS-2,paint);
        Hex h=tacticPreview.blocked;if(h!=null){paint.setColor(0xffff6767);c.drawLine(x(h)-10,y(h)-10,x(h)+10,y(h)+10,paint);c.drawLine(x(h)+10,y(h)-10,x(h)-10,y(h)+10,paint);}
        paint.setStyle(Paint.Style.FILL);
    }
    private void drawArrows(Canvas c,java.util.List<Hex> points,int color){
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*density/camera.scale);
        for(int i=1;i<points.size();i++){Hex from=points.get(i-1),to=points.get(i);float dx=x(to)-x(from),dy=y(to)-y(from);float len=(float)Math.hypot(dx,dy),ux=dx/len,uy=dy/len;
            c.drawLine(x(from),y(from),x(to),y(to),paint);c.drawLine(x(to),y(to),x(to)-ux*10+uy*6,y(to)-uy*10-ux*6,paint);c.drawLine(x(to),y(to),x(to)-ux*10-uy*6,y(to)-uy*10+ux*6,paint);c.drawCircle(x(to),y(to),8,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    private void bar(Canvas c,float x,float y,float width,float fraction,int color){
        paint.setColor(0xdd12252b);c.drawRoundRect(x-width/2-1,y-1,x+width/2+1,y+4,1,1,paint);
        paint.setColor(color);c.drawRect(x-width/2,y,x-width/2+width*Math.max(0,Math.min(1,fraction)),y+3,paint);
    }
    private void unitBar(Canvas c,float x,float y,float width,float fraction,int color){
        paint.setColor(0xee12252b);c.drawRect(x-width/2-density,y-density,x+width/2+density,y+4*density,paint);
        paint.setColor(color);c.drawRect(x-width/2,y,x-width/2+width*Math.max(0,Math.min(1,fraction)),y+3*density,paint);
    }
    private void drawUnit(Canvas c,World.Unit u){float scale=camera.scale;float cx=x(u.hex),cy=y(u.hex);
        c.save();c.translate(cx,cy);models.unit(c,u,world.army.water(u.hex),factionColor(u.owner));c.restore();
        if(u.acted){paint.setColor(Color.argb(210,17,32,37));c.drawCircle(cx+16,cy-14,7,paint);label(c,"✓",cx+16,cy-11,10,PAPER);}
        if(u.burning>0)label(c,"火",cx-16,cy+15,12,Color.rgb(255,120,60));
        if(u.status!=War.Status.NORMAL)label(c,u.status.label.substring(0,1),cx+16,cy+15,12,Color.rgb(255,194,100));

        if(world.diplomacy.aidForUnit(u.id)!=null)label(c,"援",cx-17,cy-14,11,0xff91d3e0);
        else if(u.food<Logistics.foodUse(world,u)*3)label(c,"粮!",cx-17,cy-14,10,0xffffb077);

    }
}
