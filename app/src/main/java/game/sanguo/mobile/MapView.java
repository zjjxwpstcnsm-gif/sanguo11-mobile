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
    private final GestureDetector gestures;
    private final ScaleGestureDetector scaler;
    private World world;
    private Hex[][] tiles;
    private Hex selected;
    private final Map<Integer,List<Object>> objectBuckets=new HashMap<>();
    private final Map<Integer,World.Officer> officerIndex=new HashMap<>();
    private final Map<Integer,World.City> cityIndex=new HashMap<>();
    private final ArrayList<Object> visibleObjects=new ArrayList<>();
    private final RectF miniRect=new RectF();
    private Bitmap miniTerrain;
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
    private final MapCamera camera=new MapCamera();
    private boolean multiTouch;
    private int moving=-1;
    private Bundle pendingCamera;
    private final Typeface font=Typeface.create("sans-serif",Typeface.NORMAL);
    private final float density;
    private static final float RADIUS=25,SQRT3=1.7320508f;
    private static final int PAPER=Color.rgb(232,224,199),GOLD=Color.rgb(230,191,119);
    public MapView(Context context,TileListener listener){
        super(context);this.listener=listener;density=getResources().getDisplayMetrics().density;setContentDescription("六角格战略地图。拖动平移，双指缩放，点选城池或部队。");setFocusable(true);
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
        if(newTerrain){
            if(miniTerrain!=null)miniTerrain.recycle();
            miniTerrain=Bitmap.createBitmap(world.width,world.height,Bitmap.Config.ARGB_8888);
            for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++)miniTerrain.setPixel(q,r,world.terrain[q][r]==World.Terrain.WATER?0xff36586c:world.terrain[q][r]==World.Terrain.MOUNTAIN?0xff5b635b:world.terrain[q][r]==World.Terrain.FOREST?0xff415a49:0xff7e8463);
        }
        reachable=world.reachable(world.unit(moving));if(changed&&getWidth()>0){resizeCamera();fit();}invalidate();}
    private float x(Hex h){return RADIUS*SQRT3*(h.q+h.r*.5f);}
    private float y(Hex h){return RADIUS*1.5f*h.r;}
    private float worldWidth(){return RADIUS*SQRT3*(world.width-1+(world.height-1)*.5f)+RADIUS*2;}
    private float worldHeight(){return RADIUS*1.5f*(world.height-1)+RADIUS*2;}
    private void resizeCamera(){if(world!=null&&getWidth()>0&&getHeight()>0)camera.resize(getWidth(),Math.max(1,getHeight()-36*density),worldWidth(),worldHeight(),RADIUS,density);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);resizeCamera();applyPendingCamera();}
    public void fit(){if(world==null||getWidth()==0)return;camera.fit();invalidate();}
    public void focus(Hex h){if(world==null||h==null)return;if(getWidth()==0){post(()->focus(h));return;}camera.focus(x(h),y(h));invalidate();}
    private void zoom(float scale,float fx,float fy){camera.zoom(scale,fx,fy);invalidate();}
    void saveCamera(Bundle b){b.putBoolean("mapNavigator",showMini);b.putFloat("cameraRatio",camera.scale/camera.minScale);b.putFloat("cameraX",camera.centerX());b.putFloat("cameraY",camera.centerY());}
    void restoreCamera(Bundle b){showMini=b.getBoolean("mapNavigator",false);pendingCamera=new Bundle(b);applyPendingCamera();}
    private void applyPendingCamera(){if(pendingCamera!=null&&getWidth()>0&&getHeight()>0){camera.restore(pendingCamera.getFloat("cameraRatio",1),pendingCamera.getFloat("cameraX"),pendingCamera.getFloat("cameraY"));pendingCamera=null;invalidate();}}
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
        if(moving>=0)return world.inside(exact)?exact:null;
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
    private int factionColor(int owner){if(owner<0)return Color.rgb(153,146,128);
        int[] palette={Color.rgb(98,175,143),Color.rgb(102,156,197),Color.rgb(216,135,105),Color.rgb(185,143,205),Color.rgb(205,183,94),Color.rgb(91,184,184)};return palette[owner%palette.length];}
    @Override protected void onDraw(Canvas canvas){
        long drawStart=System.nanoTime();lastTilesVisited=0;lastObjectsVisited=0;super.onDraw(canvas);canvas.drawColor(Color.rgb(23,44,46));if(world==null)return;
        float scale=camera.scale,offsetX=camera.x,offsetY=camera.y;
        boolean detail=scale*RADIUS>=12*density;collectVisible();
        canvas.save();canvas.translate(offsetX,offsetY);canvas.scale(scale,scale);
        int r0=camera.firstRow(world.height,RADIUS*2),r1=camera.lastRow(world.height,RADIUS*2);
        for(int r=r0;r<=r1;r++)for(int q=camera.firstColumn(r,world.width,RADIUS*2);q<=camera.lastColumn(r,world.width,RADIUS*2);q++){
            lastTilesVisited++;Hex h=tiles[q][r];float cx=x(h),cy=y(h);float sx=cx*scale+offsetX,sy=cy*scale+offsetY;
            if(sx<-RADIUS*scale||sy<-RADIUS*scale||sx>getWidth()+RADIUS*scale||sy>getHeight()+RADIUS*scale)continue;
            World.Terrain t=world.terrain[q][r];int color;
            switch(t){case FOREST:color=Color.rgb(65,90,73);break;case WATER:color=Color.rgb(54,88,108);break;case MOUNTAIN:color=Color.rgb(91,99,91);break;case MOUNTAIN_PATH:color=Color.rgb(124,106,80);break;case SHALLOWS:color=Color.rgb(91,142,151);break;case PLANK_ROAD:color=Color.rgb(145,117,84);break;default:color=(q+r)%2==0?Color.rgb(126,132,99):Color.rgb(120,127,94);}
            polygon(cx,cy,RADIUS-.3f);fill(canvas,color);stroke(canvas,Color.argb(40,13,37,35),.7f);
            if(t==World.Terrain.MOUNTAIN){path.reset();path.moveTo(cx-13,cy+9);path.lineTo(cx-3,cy-12);path.lineTo(cx+5,cy+2);path.lineTo(cx+11,cy-7);path.lineTo(cx+18,cy+9);path.close();fill(canvas,Color.rgb(158,159,137));}
            if(t==World.Terrain.FOREST){paint.setColor(Color.rgb(40,77,60));canvas.drawCircle(cx-6,cy-3,7,paint);canvas.drawCircle(cx+6,cy+5,8,paint);}
            if(t==World.Terrain.WATER){paint.setColor(Color.argb(65,163,195,198));paint.setStrokeWidth(1);canvas.drawLine(cx-10,cy-3,cx+8,cy-3,paint);canvas.drawLine(cx-5,cy+5,cx+12,cy+5,paint);}
            if(reachable.containsKey(h)){polygon(cx,cy,RADIUS-1);fill(canvas,Color.argb(55,197,227,158));stroke(canvas,Color.argb(110,229,235,182),1);}
        }
        if(selected!=null){polygon(x(selected),y(selected),RADIUS-2);stroke(canvas,GOLD,Math.max(2,2*density/scale));}
        if(detail)for(Object object:visibleObjects)if(object instanceof War.Fire){War.Fire f=(War.Fire)object;
            float cx=x(f.hex),cy=y(f.hex);polygon(cx,cy,RADIUS-2);fill(canvas,Color.argb(145,227,81,28));label(canvas,"火",cx,cy+5,18,PAPER);
        }
        if(detail)for(Object object:visibleObjects)if(object instanceof War.Structure){War.Structure s=(War.Structure)object;
            float cx=x(s.hex),cy=y(s.hex);paint.setColor(factionColor(s.owner));canvas.drawRect(cx-14,cy-13,cx+14,cy+13,paint);
            label(canvas,s.kind.label.substring(0,1),cx,cy+5,15,Color.rgb(18,34,34));label(canvas,(s.complete?"":"建")+s.hp,cx,cy-17,10,PAPER);
        }
        if(detail)for(Object object:visibleObjects)if(object instanceof Domestic.Facility){Domestic.Facility f=(Domestic.Facility)object;float cx=x(f.hex),cy=y(f.hex);paint.setColor(factionColor(cityIndex.get(f.cityId).owner));canvas.drawRect(cx-12,cy-12,cx+12,cy+12,paint);label(canvas,f.kind.label.substring(0,1),cx,cy+5,15,Color.rgb(18,34,34));if(f.remaining>0)label(canvas,"剩"+f.remaining,cx,cy-16,10,PAPER);}
        for(Object object:visibleObjects)if(object instanceof World.City)drawCity(canvas,(World.City)object);
        if(detail||moving>=0)for(Object object:visibleObjects)if(object instanceof World.Unit)drawUnit(canvas,(World.Unit)object);
        if(detail)for(Object object:visibleObjects)if(object instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)object;if(m.owner!=world.player&&!m.transport)continue;float cx=x(m.hex)+15,cy=y(m.hex)-8;paint.setColor(Color.rgb(30,42,43));canvas.drawCircle(cx,cy,10,paint);label(canvas,m.transport?"运":"调",cx,cy+4,12,m.owner==world.player?GOLD:factionColor(m.owner));}
        canvas.restore();
        if(showMini)drawNavigator(canvas);
        paint.setColor(Color.argb(190,17,32,37));canvas.drawRoundRect(10*density,getHeight()-34*density,getWidth()-10*density,getHeight()-8*density,5*density,5*density,paint);
        label(canvas,detail?"设施 / 部队 / 在途  ·  双击城池聚焦":"势力总览  ·  放大查看设施与在途",getWidth()/2f,getHeight()-17*density,10*density,PAPER);
        lastDrawNanos=System.nanoTime()-drawStart;
    }
    private void drawNavigator(Canvas c){
        float mw=Math.min(144*density,getWidth()*.3f),mh=Math.min(100*density,getHeight()*.3f);
        miniRect.set(getWidth()-mw-12*density,12*density,getWidth()-12*density,12*density+mh);
        paint.setColor(0xee10272b);c.drawRect(miniRect.left-3,miniRect.top-3,miniRect.right+3,miniRect.bottom+3,paint);
        c.drawBitmap(miniTerrain,null,miniRect,paint);
        for(World.City city:world.cities){paint.setColor(factionColor(city.owner));c.drawCircle(miniRect.left+(city.hex.q+.5f)/world.width*mw,miniRect.top+(city.hex.r+.5f)/world.height*mh,2*density,paint);}
        float r=camera.centerY()/(RADIUS*1.5f),q=camera.centerX()/(RADIUS*SQRT3)-r*.5f;
        float px=miniRect.left+q/world.width*mw,py=miniRect.top+r/world.height*mh;
        paint.setStyle(Paint.Style.STROKE);paint.setColor(GOLD);paint.setStrokeWidth(2*density);c.drawCircle(px,py,5*density,paint);paint.setStyle(Paint.Style.FILL);
        label(c,"当前战场 · 拖动定位",miniRect.centerX(),miniRect.bottom+13*density,10*density,PAPER);
    }
    private void drawCity(Canvas c,World.City city){float scale=camera.scale;float cx=x(city.hex),cy=y(city.hex);int owner=factionColor(city.owner);
        paint.setColor(owner);c.drawCircle(cx,cy,22,paint);paint.setColor(Color.rgb(32,44,40));c.drawRect(cx-17,cy-8,cx+17,cy+12,paint);paint.setColor(Color.rgb(210,196,161));
        c.drawRect(cx-15,cy-6,cx+15,cy+9,paint);for(int i=-15;i<15;i+=6)c.drawRect(cx+i,cy-10,cx+i+4,cy-4,paint);
        paint.setColor(Color.rgb(61,62,46));c.drawRect(cx-4,cy+1,cx+4,cy+12,paint);paint.setColor(owner);c.drawRect(cx,cy-29,cx+13,cy-17,paint);paint.setStrokeWidth(1.5f);c.drawLine(cx,cy-30,cx,cy-9,paint);
        boolean labelVisible=scale*RADIUS>=7*density||city.hex.equals(selected)||(world.home()!=null&&city.id==world.home().id);
        if(!labelVisible)return;
        float sz=12*density/scale;paint.setColor(Color.argb(225,22,37,37));c.drawRoundRect(cx-sz*1.8f,cy+14,cx+sz*1.8f,cy+14+sz*1.6f,3,3,paint);
        label(c,city.name,cx,cy+14+sz*1.15f,sz,PAPER);
        if(scale>=camera.minScale*1.55f)label(c,world.faction(city.owner),cx,cy-34,10*density/scale,PAPER);

        if(scale>=camera.minScale*2.1f){float fs=10*density/scale;float baseline=cy+14+sz*1.6f+fs*1.3f;String info="金 "+city.gold+" · 粮 "+city.food+" · 兵 "+city.troops;
            paint.setTextSize(fs);float half=paint.measureText(info)/2+4*density/scale;paint.setColor(Color.argb(235,17,32,37));c.drawRoundRect(cx-half,baseline-fs,cx+half,baseline+fs*.3f,2,2,paint);label(c,info,cx,baseline,fs,PAPER);}
        paint.setColor(owner);c.drawRect(cx-16,cy+10,cx-16+32*Math.min(1,city.defense/3000f),cy+13,paint);
    }
    private void drawUnit(Canvas c,World.Unit u){float scale=camera.scale;float cx=x(u.hex),cy=y(u.hex);paint.setColor(Color.rgb(24,38,37));c.drawCircle(cx+1,cy+2,15,paint);paint.setColor(factionColor(u.owner));c.drawCircle(cx,cy,14,paint);
        label(c,world.army.equipmentLabel(u).substring(0,1),cx,cy+5,16,Color.rgb(18,34,34));
        if(u.acted){paint.setColor(Color.argb(140,17,32,37));c.drawCircle(cx,cy,14,paint);label(c,"✓",cx,cy+4,14,PAPER);}
        if(u.burning>0)label(c,"火",cx-16,cy+15,12,Color.rgb(255,120,60));
        if(u.status!=game.sanguo.core.War.Status.NORMAL)label(c,u.status.label.substring(0,1),cx+16,cy+15,12,Color.rgb(255,194,100));
        float sz=10*density/scale;label(c,officerIndex.get(u.officerId).name+" "+u.troops,cx,cy-19,sz,PAPER);
    }
}
