package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.os.Bundle;
import java.util.*;
import game.sanguo.core.*;

/** Original procedural debug renderer; not SAN11 artwork or final 3D renderer. */
public final class MapView extends View {
    public interface TileListener {void tap(Hex tile);}
    private final TileListener listener;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final MapModels models=new MapModels();
    private final TerrainTiles terrainTiles=new TerrainTiles();
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
    private Set<Hex> pickTargets;
    void setPickTargets(Set<Hex> targets){pickTargets=targets;invalidate();}
    private final Map<Integer,List<Object>> objectBuckets=new HashMap<>();
    private final Map<Integer,World.Officer> officerIndex=new HashMap<>();
    private final Map<Integer,World.City> cityIndex=new HashMap<>();
    private final ArrayList<Object> visibleObjects=new ArrayList<>();
    private final RectF miniRect=new RectF();
    private Bitmap miniTerrain;
    private Bitmap miniTerritory;
    private Territory territory;
    private final Path factionBorders=new Path(),siteBorders=new Path();
    private int territoryMode;
    private String territoryOwners="";
    Territory territory(){return territory;}
    int territoryMode(){return territoryMode;}
    void setTerritoryMode(int mode){territoryMode=Math.max(0,Math.min(2,mode));invalidate();}
    private boolean showMini,miniGesture;
    private int lastTilesVisited,lastObjectsVisited;
    private long lastDrawNanos;
    public void toggleNavigator(){showMini=!showMini;invalidate();}
    int tilesVisited(){return lastTilesVisited;}
    int objectsVisited(){return lastObjectsVisited;}
    long drawNanos(){return lastDrawNanos;}
    private void index(Object object,Hex h){int key=(h.r/8)*1024+h.q/8;objectBuckets.computeIfAbsent(key,k->new ArrayList<>()).add(object);}
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
        visibleObjects.clear();float margin=100*density/camera.scale;
        int r0=camera.firstRow(world.height,margin),r1=camera.lastRow(world.height,margin);
        for(int br=r0/8;br<=r1/8;br++){
            int q0=camera.firstColumn(Math.min(world.height-1,br*8+7),world.width,margin)/8;
            int q1=camera.lastColumn(br*8,world.width,margin)/8;
            for(int bq=q0;bq<=q1;bq++){
                List<Object> bucket=objectBuckets.get(br*1024+bq);if(bucket==null)continue;
                for(Object o:bucket){lastObjectsVisited++;Hex h=position(o);if(camera.visible(x(h),y(h),100*density))visibleObjects.add(o);}
            }
        }
    }
    private Map<Hex,Integer> reachable=Collections.emptyMap();
    private final Set<Hex> attackTargets=new HashSet<>();
    int reachableCount(){return reachable.size();}
    private void addAttackTarget(World.Unit u,Hex h,boolean normal){
        if(normal){attackTargets.add(h);return;}
        for(Army.Tactic tactic:world.army.tactics(u))if(world.army.tacticError(u.id,h,tactic)==null){attackTargets.add(h);return;}
    }
    private final MapCamera camera=new MapCamera();
    private boolean multiTouch;
    private int moving=-1;
    private MarchOrders.Plan route;
    void setRoute(MarchOrders.Plan route){this.route=route;invalidate();}
    private Bundle pendingCamera;
    private final Typeface font=Typeface.create("sans-serif",Typeface.NORMAL);
    private final float density;
    private static final float RADIUS=25,SQRT3=1.7320508f;
    private static final int PAPER=Color.rgb(232,224,199),GOLD=Color.rgb(230,191,119);
    public MapView(Context context,TileListener listener){
        super(context);this.listener=listener;density=getResources().getDisplayMetrics().density;setContentDescription("六角格战略地图。拖动平移，双指缩放，点选城池或部队。");setFocusable(true);
        BuildingAtlas.load(context);
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent e){if(!multiTouch&&!scaler.isInProgress()){performClick();listener.tap(hit(e.getX(),e.getY()));}return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!multiTouch&&!scaler.isInProgress()){camera.pan(-dx,-dy);invalidate();}return true;}
            @Override public boolean onDoubleTap(MotionEvent e){if(!multiTouch){Hex h=hit(e.getX(),e.getY());if(h!=null&&world.cityAt(h)!=null&&moving<0){listener.tap(h);focus(h);}else zoom(camera.scale<camera.minScale*1.8f?camera.scale*1.8f:camera.minScale,e.getX(),e.getY());}return true;}
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector d){zoom(camera.scale*d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}
        });
    }
    public void setWorld(World world,Hex selected,int moving){boolean changed=this.world==null||this.world.width!=world.width||this.world.height!=world.height||!this.world.scenarioId.equals(world.scenarioId);boolean newTerrain=this.world!=world||changed;this.world=world;this.selected=selected;this.moving=moving;
        if(changed){tiles=new Hex[world.width][world.height];for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++)tiles[q][r]=new Hex(q,r);}
        objectBuckets.clear();officerIndex.clear();cityIndex.clear();
        for(World.Officer o:world.officers)officerIndex.put(o.id,o);
        for(World.City c:world.cities){cityIndex.put(c.id,c);index(c,c.hex);}
        for(World.Unit u:world.units)index(u,u.hex);
        for(Domestic.Facility f:world.domestic.facilities)index(f,f.hex);
        for(Domestic.Mission m:world.domestic.missions)index(m,m.hex);
        for(War.Fire f:world.war.fires())index(f,f.hex);
        for(War.Structure b:world.war.structures())index(b,b.hex);
        for(WorldEvents.Camp c:world.events.camps())index(c,c.hex);
        if(newTerrain){
            territory=new Territory(world);territoryOwners="";
            if(miniTerrain!=null)miniTerrain.recycle();
            miniTerrain=Bitmap.createBitmap(world.width,world.height,Bitmap.Config.ARGB_8888);
            for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++)miniTerrain.setPixel(q,r,TerrainTiles.color(world.terrain[q][r]));
        }
        StringBuilder owners=new StringBuilder();for(World.City city:world.cities)owners.append(city.id).append(':').append(city.owner).append(';');
        if(!territoryOwners.equals(owners.toString())){
            territoryOwners=owners.toString();if(miniTerritory!=null)miniTerritory.recycle();
            miniTerritory=Bitmap.createBitmap(world.width,world.height,Bitmap.Config.ARGB_8888);
            factionBorders.reset();siteBorders.reset();
            for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++)if(territory.siteAt(q,r)>=0){
                miniTerritory.setPixel(q,r,alpha(factionColor(territory.ownerAt(q,r)),155));
                addBorders(factionBorders,territory.boundary(q,r,false),x(tiles[q][r]),y(tiles[q][r]));
                addBorders(siteBorders,territory.boundary(q,r,true),x(tiles[q][r]),y(tiles[q][r]));
            }
        }
        World.Unit actor=world.unit(moving);reachable=world.orders.marchReachable(actor);attackTargets.clear();
        if(world.orders.error(actor)==null){
            for(World.Unit target:world.units)if(target.id!=actor.id)addAttackTarget(actor,target.hex,world.war.attackError(actor.id,target.id)==null);
            for(World.City city:world.cities)addAttackTarget(actor,city.hex,world.siegeError(actor.id,city.id)==null);
            for(Domestic.Facility f:world.domestic.facilities)addAttackTarget(actor,f.hex,world.war.facilityAttackError(actor.id,f.hex)==null);
            for(War.Structure s:world.war.structures())addAttackTarget(actor,s.hex,world.army.canAttackUnit(actor)&&world.campaign.hostile(actor.owner,s.owner)&&actor.hex.distance(s.hex)<=world.war.range(actor));
        }
        if(changed&&getWidth()>0){resizeCamera();fit();}invalidate();}
    private float x(Hex h){return RADIUS*SQRT3*(h.q+h.r*.5f);}
    private float y(Hex h){return RADIUS*1.5f*h.r;}
    private float worldWidth(){return RADIUS*SQRT3*(world.width-1+(world.height-1)*.5f)+RADIUS*2;}
    private float worldHeight(){return RADIUS*1.5f*(world.height-1)+RADIUS*2;}
    private void resizeCamera(){if(world!=null&&getWidth()>0&&getHeight()>0)camera.resize(getWidth(),getHeight(),worldWidth(),worldHeight(),RADIUS,density);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);resizeCamera();applyPendingCamera();}
    public void fit(){if(world==null||getWidth()==0)return;camera.fit();invalidate();}
    public void focus(Hex h){if(world==null||h==null)return;if(getWidth()==0){post(()->focus(h));return;}camera.focus(x(h),y(h));invalidate();}
    public void center(Hex h){if(world!=null&&h!=null){camera.centerOn(x(h),y(h));invalidate();}}
    private void zoom(float scale,float fx,float fy){camera.zoom(scale,fx,fy);invalidate();}
    void saveCamera(Bundle b){b.putBoolean("mapNavigator",showMini);b.putFloat("cameraRatio",camera.scale/camera.minScale);b.putFloat("cameraScaleDp",camera.scale/density);b.putFloat("cameraX",camera.centerX());b.putFloat("cameraY",camera.centerY());}
    void restoreCamera(Bundle b){showMini=b.getBoolean("mapNavigator",false);pendingCamera=new Bundle(b);applyPendingCamera();}
    private void applyPendingCamera(){if(pendingCamera!=null&&getWidth()>0&&getHeight()>0){if(pendingCamera.containsKey("cameraScaleDp"))camera.restoreScale(pendingCamera.getFloat("cameraScaleDp")*density,pendingCamera.getFloat("cameraX"),pendingCamera.getFloat("cameraY"));else camera.restore(pendingCamera.getFloat("cameraRatio",1),pendingCamera.getFloat("cameraX"),pendingCamera.getFloat("cameraY"));pendingCamera=null;invalidate();}}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return true;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){multiTouch=false;miniGesture=showMini&&miniRect.contains(e.getX(),e.getY());}
        if(miniGesture){
            if(e.getPointerCount()==1&&(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE)){
                float q=Math.max(0,Math.min(world.width-1,(e.getX()-miniRect.left)/miniRect.width()*world.width));
                float r=Math.max(0,Math.min(world.height-1,(e.getY()-miniRect.top)/miniRect.height()*world.height));
                camera.centerOn(RADIUS*SQRT3*(q+r*.5f),RADIUS*1.5f*r);invalidate();
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
        }
        if(e.getPointerCount()>1)multiTouch=true;
        scaler.onTouchEvent(e);gestures.onTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)multiTouch=true;
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private Hex hit(float px,float py){
        if(world==null)return null;
        float wx=(px-camera.x)/camera.scale,wy=(py-camera.y)/camera.scale;
        float r=wy/(RADIUS*1.5f),q=wx/(RADIUS*SQRT3)-r*.5f,z=-q-r;
        int iq=Math.round(q),ir=Math.round(r),iz=Math.round(z);float dq=Math.abs(iq-q),dr=Math.abs(ir-r),dz=Math.abs(iz-z);
        if(dq>dr&&dq>dz)iq=-ir-iz;else if(dr>dz)ir=-iq-iz;
        Hex exact=new Hex(iq,ir);
        // Preserve precise tile commands while a unit is selected, including adjacent movement.
        if(moving>=0||pickTargets!=null)return world.inside(exact)?exact:null;
        if(world.inside(exact)&&(world.unitAt(exact)!=null||world.domestic.at(exact)!=null))return exact;
        World.City nearest=null;float best=Float.MAX_VALUE;
        for(World.City city:world.cities){float dx=px-(x(city.hex)*camera.scale+camera.x),dy=py-(y(city.hex)*camera.scale+camera.y);float distance=dx*dx+dy*dy;
            float radius=Math.max(24*density,20*camera.scale);
            boolean name=Math.abs(dx)<=28*density&&dy>=12*camera.scale&&dy<=12*camera.scale+24*density;
            if((distance<=radius*radius||name)&&distance<best){nearest=city;best=distance;}
        }
        if(nearest!=null)return nearest.hex;
        return world.inside(exact)?exact:null;
    }
    private void polygon(float cx,float cy,float radius){path.reset();for(int i=0;i<6;i++){double a=Math.toRadians(60*i-30);float px=cx+(float)Math.cos(a)*radius,py=cy+(float)Math.sin(a)*radius;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();}
    private void fill(Canvas c,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(path,paint);}
    private void stroke(Canvas c,int color,float width){paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}
    private void label(Canvas c,String value,float x,float y,float size,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(font);c.drawText(value,x,y,paint);}
    static int factionColor(int owner){if(owner<0)return Color.rgb(153,146,128);
        int[] palette={Color.rgb(98,175,143),Color.rgb(102,156,197),Color.rgb(216,135,105),Color.rgb(185,143,205),Color.rgb(205,183,94),Color.rgb(91,184,184)};
        return owner<palette.length?palette[owner]:Color.HSVToColor(new float[]{(owner*137.508f)%360,.48f,.84f});}
    private static int alpha(int color,int opacity){return (color&0x00ffffff)|(opacity<<24);}
    private void drawTerritory(Canvas canvas,int q,int r,float cx,float cy,float scale){
        if(territoryMode==0||territory==null||territory.siteAt(q,r)<0)return;
        int color=factionColor(territory.ownerAt(q,r));polygon(cx,cy,RADIUS);
        fill(canvas,alpha(color,territoryMode==2?95+(territory.siteAt(q,r)%3)*18:115));
    }
    private void addBorders(Path borders,int mask,float cx,float cy){
        for(int side=0;side<6;side++)if((mask&(1<<side))!=0){
            double a=Math.toRadians(-side*60-30),b=a+Math.PI/3;
            borders.moveTo(cx+(float)Math.cos(a)*RADIUS,cy+(float)Math.sin(a)*RADIUS);
            borders.lineTo(cx+(float)Math.cos(b)*RADIUS,cy+(float)Math.sin(b)*RADIUS);
        }
    }
    @Override protected void onDraw(Canvas canvas){
        long drawStart=System.nanoTime();lastTilesVisited=0;lastObjectsVisited=0;super.onDraw(canvas);canvas.drawColor(Color.rgb(23,44,46));if(world==null)return;
        float scale=camera.scale,offsetX=camera.x,offsetY=camera.y;
        boolean detail=scale*RADIUS>=12*density;collectVisible();
        canvas.save();canvas.translate(offsetX,offsetY);canvas.scale(scale,scale);
        int r0=camera.firstRow(world.height,RADIUS*2),r1=camera.lastRow(world.height,RADIUS*2);
        for(int r=r0;r<=r1;r++)for(int q=camera.firstColumn(r,world.width,RADIUS*2);q<=camera.lastColumn(r,world.width,RADIUS*2);q++){
            lastTilesVisited++;Hex h=tiles[q][r];float cx=x(h),cy=y(h);float sx=cx*scale+offsetX,sy=cy*scale+offsetY;
            if(sx<-RADIUS*scale||sy<-RADIUS*scale||sx>getWidth()+RADIUS*scale||sy>getHeight()+RADIUS*scale)continue;
            World.Terrain t=world.terrain[q][r];
            if(detail)terrainTiles.draw(canvas,t,q,r,cx,cy);
            else {polygon(cx,cy,RADIUS-.3f);fill(canvas,TerrainTiles.color(t));}
            polygon(cx,cy,RADIUS-.3f);stroke(canvas,Color.argb(40,13,37,35),.7f);
            drawTerritory(canvas,q,r,cx,cy,scale);
            if(pickTargets==null&&reachable.containsKey(h)&&reachable.get(h)>0){polygon(cx,cy,RADIUS-1);fill(canvas,Color.argb(110,62,218,209));stroke(canvas,Color.argb(225,117,244,234),Math.max(1,1.2f*density/scale));}
        }
        if(territoryMode>0){
            // Draw after all fills: neighboring tiles must not paint over the shared border.
            paint.setStyle(Paint.Style.STROKE);paint.setColor(0xbfe3ebcf);paint.setStrokeWidth(Math.max(1,density/scale));
            canvas.drawPath(territoryMode==2?siteBorders:factionBorders,paint);paint.setStyle(Paint.Style.FILL);
        }
        drawRoute(canvas);
        if(selected!=null){polygon(x(selected),y(selected),RADIUS-2);stroke(canvas,GOLD,Math.max(2,2*density/scale));}
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
        for(Object object:visibleObjects)if(object instanceof World.City)drawCity(canvas,(World.City)object);
        for(Object object:visibleObjects)if(object instanceof World.Unit){World.Unit u=(World.Unit)object;
            if(detail)drawUnit(canvas,u);else {paint.setColor(factionColor(u.owner));canvas.drawCircle(x(u.hex),y(u.hex),14,paint);
                if(scale*RADIUS>=5*density){canvas.save();canvas.translate(x(u.hex),y(u.hex));canvas.scale(.6f,.6f);if(world.army.water(u.hex))models.shipIcon(canvas,u.ship,PAPER);else models.weaponIcon(canvas,u.weapon,PAPER);canvas.restore();}}
        }
        for(Hex h:pickTargets==null?attackTargets:pickTargets)if(camera.visible(x(h),y(h),RADIUS)){
            polygon(x(h),y(h),RADIUS-1);if(pickTargets!=null)fill(canvas,0x555be6bf);stroke(canvas,pickTargets==null?0xffff987a:0xff70ffca,Math.max(2,2*density/scale));
        }
        if(detail)for(Object object:visibleObjects)if(object instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)object;if(m.owner!=world.player&&!m.transport)continue;float cx=x(m.hex)+15,cy=y(m.hex)-8;paint.setColor(Color.rgb(30,42,43));canvas.drawCircle(cx,cy,10,paint);label(canvas,m.transport?"运":"调",cx,cy+4,12,m.owner==world.player?GOLD:factionColor(m.owner));}
        long remaining=impactUntil-android.os.SystemClock.uptimeMillis();
        if(impactHex!=null&&remaining>0){
            float progress=1-remaining/450f;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(defeatImpact?3:2);
            paint.setColor(Color.argb((int)(220*(1-progress)),255,defeatImpact?178:226,106));
            canvas.drawCircle(x(impactHex),y(impactHex),12+progress*22,paint);paint.setStyle(Paint.Style.FILL);postInvalidateOnAnimation();
        }
        canvas.restore();
        if(territoryMode>0){
            String caption=territoryMode==1?"势力范围 · 红圈为前线":"据点辖区 · 红圈为前线";
            int site=territory.siteAt(selected);World.City city=cityIndex.get(site);
            if(city!=null)caption=city.name+"辖区 · "+world.faction(city.owner)+(territory.frontline(site)?" · 前线":"");
            paint.setColor(0xe612272b);canvas.drawRoundRect(8*density,8*density,Math.min(getWidth()-8*density,258*density),38*density,6*density,6*density,paint);
            paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(12*density);paint.setColor(PAPER);canvas.drawText(caption,16*density,28*density,paint);
        }
        if(showMini)drawNavigator(canvas);

        lastDrawNanos=System.nanoTime()-drawStart;
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
    private void drawNavigator(Canvas c){
        float mw=Math.min(144*density,getWidth()*.3f),mh=Math.min(100*density,getHeight()*.3f);
        miniRect.set(getWidth()-mw-12*density,12*density,getWidth()-12*density,12*density+mh);
        paint.setColor(0xee10272b);c.drawRect(miniRect.left-3,miniRect.top-3,miniRect.right+3,miniRect.bottom+3,paint);
        c.drawBitmap(miniTerrain,null,miniRect,paint);
        if(territoryMode>0&&miniTerritory!=null)c.drawBitmap(miniTerritory,null,miniRect,paint);
        for(World.City city:world.cities){paint.setColor(factionColor(city.owner));c.drawCircle(miniRect.left+(city.hex.q+.5f)/world.width*mw,miniRect.top+(city.hex.r+.5f)/world.height*mh,2*density,paint);}
        float r=camera.centerY()/(RADIUS*1.5f),q=camera.centerX()/(RADIUS*SQRT3)-r*.5f;
        float px=miniRect.left+q/world.width*mw,py=miniRect.top+r/world.height*mh;
        paint.setStyle(Paint.Style.STROKE);paint.setColor(GOLD);paint.setStrokeWidth(2*density);c.drawCircle(px,py,5*density,paint);paint.setStyle(Paint.Style.FILL);
        label(c,"当前战场 · 拖动定位",miniRect.centerX(),miniRect.bottom+13*density,10*density,PAPER);
    }
    private void drawCity(Canvas c,World.City city){float scale=camera.scale;float cx=x(city.hex),cy=y(city.hex);int owner=factionColor(city.owner);
        c.save();c.translate(cx,cy);models.city(c,city.kind,owner);c.restore();
        if(territoryMode>0&&territory.frontline(city.id)){paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffff8570);paint.setStrokeWidth(2*density/scale);c.drawCircle(cx,cy,Math.max(24,5*density/scale),paint);paint.setStyle(Paint.Style.FILL);}
        boolean labelVisible=scale*RADIUS>=7*density||city.hex.equals(selected)||(world.home()!=null&&city.id==world.home().id);
        if(!labelVisible)return;
        float sz=12*density/scale;paint.setColor(Color.argb(225,22,37,37));c.drawRoundRect(cx-sz*1.8f,cy+14,cx+sz*1.8f,cy+14+sz*1.6f,3,3,paint);
        label(c,city.name,cx,cy+14+sz*1.15f,sz,PAPER);
        if(scale>=camera.minScale*1.55f)label(c,world.faction(city.owner),cx,cy-34,10*density/scale,PAPER);

        if(scale>=camera.minScale*2.1f){float fs=10*density/scale;float baseline=cy+14+sz*1.6f+fs*1.3f;String info="金 "+city.gold+" · 粮 "+city.food+" · 兵 "+city.troops;
            paint.setTextSize(fs);float half=paint.measureText(info)/2+4*density/scale;paint.setColor(Color.argb(235,17,32,37));c.drawRoundRect(cx-half,baseline-fs,cx+half,baseline+fs*.3f,2,2,paint);label(c,info,cx,baseline,fs,PAPER);}
        bar(c,cx,cy+10,32,city.defense/(float)world.campaign.defenseCap(city),owner);
    }
    private void bar(Canvas c,float x,float y,float width,float fraction,int color){
        paint.setColor(0xdd12252b);c.drawRoundRect(x-width/2-1,y-1,x+width/2+1,y+4,1,1,paint);
        paint.setColor(color);c.drawRect(x-width/2,y,x-width/2+width*Math.max(0,Math.min(1,fraction)),y+3,paint);
    }
    private void drawUnit(Canvas c,World.Unit u){float scale=camera.scale;float cx=x(u.hex),cy=y(u.hex);
        c.save();c.translate(cx,cy);models.unit(c,u,world.army.water(u.hex),factionColor(u.owner));c.restore();
        if(u.acted){paint.setColor(Color.argb(210,17,32,37));c.drawCircle(cx+16,cy-14,7,paint);label(c,"✓",cx+16,cy-11,10,PAPER);}
        if(u.burning>0)label(c,"火",cx-16,cy+15,12,Color.rgb(255,120,60));
        if(u.status!=War.Status.NORMAL)label(c,u.status.label.substring(0,1),cx+16,cy+15,12,Color.rgb(255,194,100));
        bar(c,cx,cy+17,34,u.troops/(float)world.government.commandLimit(u.officerId),u.owner==world.player?0xff8bc4a0:factionColor(u.owner));
        if(world.diplomacy.aidForUnit(u.id)!=null)label(c,"援",cx-17,cy-14,11,0xff91d3e0);
        else if(u.food<(u.troops+19)/20*3)label(c,"粮!",cx-17,cy-14,10,0xffffb077);
        float sz=Math.min(10,10*density/scale);label(c,officerIndex.get(u.officerId).name+" "+u.troops,cx,cy-31,sz,PAPER);
        label(c,world.army.water(u.hex)?u.ship.label:u.weapon.label,cx,cy+26,9,PAPER);
    }
}
