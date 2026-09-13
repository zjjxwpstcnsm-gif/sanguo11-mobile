package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import android.view.*;
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
    private Hex selected;
    private Map<Hex,Integer> reachable=Collections.emptyMap();
    private float scale=1,fitScale=1,offsetX=0,offsetY=0;
    private final float density;
    private static final float RADIUS=25,SQRT3=1.7320508f;
    private static final int PAPER=Color.rgb(232,224,199),GOLD=Color.rgb(230,191,119);
    public MapView(Context context,TileListener listener){
        super(context);this.listener=listener;density=getResources().getDisplayMetrics().density;setContentDescription("六角格战略地图。拖动平移，双指缩放，点选城池或部队。");setFocusable(true);
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onSingleTapUp(MotionEvent e){if(!scaler.isInProgress()){performClick();listener.tap(hit(e.getX(),e.getY()));}return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){if(!scaler.isInProgress()){offsetX-=dx;offsetY-=dy;clamp();invalidate();}return true;}
            @Override public boolean onDoubleTap(MotionEvent e){zoom(scale<fitScale*1.8f?scale*1.8f:fitScale,e.getX(),e.getY());return true;}
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector d){zoom(scale*d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}
        });
    }
    public void setWorld(World world,Hex selected,int moving){boolean changed=this.world==null||this.world.width!=world.width||this.world.height!=world.height||!this.world.scenarioId.equals(world.scenarioId);this.world=world;this.selected=selected;
        reachable=world.reachable(world.unit(moving));if(changed&&getWidth()>0)fit();invalidate();}
    private float x(Hex h){return RADIUS*SQRT3*(h.q+h.r*.5f);}
    private float y(Hex h){return RADIUS*1.5f*h.r;}
    private float worldWidth(){return RADIUS*SQRT3*(world.width-1+(world.height-1)*.5f)+RADIUS*2;}
    private float worldHeight(){return RADIUS*1.5f*(world.height-1)+RADIUS*2;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);fit();}
    public void fit(){if(world==null||getWidth()==0||getHeight()==0)return;
        fitScale=Math.min((getWidth()-40*density)/worldWidth(),(getHeight()-55*density)/worldHeight());fitScale=Math.max(.05f,fitScale);scale=fitScale;
        offsetX=(getWidth()-worldWidth()*scale)/2+RADIUS*scale;offsetY=(getHeight()-worldHeight()*scale)/2+RADIUS*scale-10*density;invalidate();}
    public void focus(Hex h){if(world==null||h==null)return;if(getWidth()==0){post(()->focus(h));return;}
        scale=Math.max(fitScale,1.25f*density);offsetX=getWidth()/2f-x(h)*scale;offsetY=getHeight()/2f-y(h)*scale;clamp();invalidate();}
    private void zoom(float newScale,float fx,float fy){float old=scale;scale=Math.max(fitScale,Math.min(Math.max(fitScale*4,2*density),newScale));offsetX=fx-(fx-offsetX)*scale/old;offsetY=fy-(fy-offsetY)*scale/old;clamp();invalidate();}
    private void clamp(){if(world==null)return;offsetX=Math.max(60*density-worldWidth()*scale,Math.min(getWidth()-60*density,offsetX));offsetY=Math.max(50*density-worldHeight()*scale,Math.min(getHeight()-50*density,offsetY));}
    @Override public boolean onTouchEvent(MotionEvent event){scaler.onTouchEvent(event);gestures.onTouchEvent(event);return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    private Hex hit(float px,float py){if(world==null)return null;float wx=(px-offsetX)/scale,wy=(py-offsetY)/scale;
        float r=wy/(RADIUS*1.5f),q=wx/(RADIUS*SQRT3)-r*.5f;float z=-q-r;
        int iq=Math.round(q),ir=Math.round(r),iz=Math.round(z);float dq=Math.abs(iq-q),dr=Math.abs(ir-r),dz=Math.abs(iz-z);
        if(dq>dr&&dq>dz)iq=-ir-iz;else if(dr>dz)ir=-iq-iz;
        Hex result=new Hex(iq,ir);return world.inside(result)?result:null;
    }
    private void polygon(float cx,float cy,float radius){path.reset();for(int i=0;i<6;i++){double a=Math.toRadians(60*i-30);float px=cx+(float)Math.cos(a)*radius,py=cy+(float)Math.sin(a)*radius;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();}
    private void fill(Canvas c,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(path,paint);}
    private void stroke(Canvas c,int color,float width){paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);}
    private void label(Canvas c,String value,float x,float y,float size,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(value,x,y,paint);}
    private int factionColor(int owner){if(owner<0)return Color.rgb(153,146,128);
        int[] palette={Color.rgb(98,175,143),Color.rgb(102,156,197),Color.rgb(216,135,105),Color.rgb(185,143,205),Color.rgb(205,183,94),Color.rgb(91,184,184)};return palette[owner%palette.length];}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(Color.rgb(23,44,46));if(world==null)return;
        canvas.save();canvas.translate(offsetX,offsetY);canvas.scale(scale,scale);
        for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++){
            Hex h=new Hex(q,r);float cx=x(h),cy=y(h);float sx=cx*scale+offsetX,sy=cy*scale+offsetY;
            if(sx<-RADIUS*scale||sy<-RADIUS*scale||sx>getWidth()+RADIUS*scale||sy>getHeight()+RADIUS*scale)continue;
            World.Terrain t=world.terrain[q][r];int color;
            switch(t){case FOREST:color=Color.rgb(65,90,73);break;case WATER:color=Color.rgb(54,88,108);break;case MOUNTAIN:color=Color.rgb(91,99,91);break;default:color=(q+r)%2==0?Color.rgb(126,132,99):Color.rgb(120,127,94);}
            polygon(cx,cy,RADIUS-.3f);fill(canvas,color);stroke(canvas,Color.argb(40,13,37,35),.7f);
            if(t==World.Terrain.MOUNTAIN){path.reset();path.moveTo(cx-13,cy+9);path.lineTo(cx-3,cy-12);path.lineTo(cx+5,cy+2);path.lineTo(cx+11,cy-7);path.lineTo(cx+18,cy+9);path.close();fill(canvas,Color.rgb(158,159,137));}
            if(t==World.Terrain.FOREST){paint.setColor(Color.rgb(40,77,60));canvas.drawCircle(cx-6,cy-3,7,paint);canvas.drawCircle(cx+6,cy+5,8,paint);}
            if(t==World.Terrain.WATER){paint.setColor(Color.argb(65,163,195,198));paint.setStrokeWidth(1);canvas.drawLine(cx-10,cy-3,cx+8,cy-3,paint);canvas.drawLine(cx-5,cy+5,cx+12,cy+5,paint);}
            if(reachable.containsKey(h)){polygon(cx,cy,RADIUS-1);fill(canvas,Color.argb(55,197,227,158));stroke(canvas,Color.argb(110,229,235,182),1);}
        }
        if(selected!=null){polygon(x(selected),y(selected),RADIUS-2);stroke(canvas,GOLD,Math.max(2,2*density/scale));}
        for(Domestic.Facility f:world.domestic.facilities){float cx=x(f.hex),cy=y(f.hex);paint.setColor(factionColor(world.city(f.cityId).owner));canvas.drawRect(cx-12,cy-12,cx+12,cy+12,paint);label(canvas,f.kind.label.substring(0,1),cx,cy+5,15,Color.rgb(18,34,34));if(f.remaining>0)label(canvas,"剩"+f.remaining,cx,cy-16,10,PAPER);}
        for(World.City city:world.cities)drawCity(canvas,city);
        for(World.Unit u:world.units)drawUnit(canvas,u);
        for(Domestic.Mission m:world.domestic.missions)if(m.owner==world.player){float cx=x(m.hex)+15,cy=y(m.hex)-8;paint.setColor(Color.rgb(30,42,43));canvas.drawCircle(cx,cy,10,paint);label(canvas,m.transport?"运":"调",cx,cy+4,12,GOLD);}
        canvas.restore();
        paint.setColor(Color.argb(190,17,32,37));canvas.drawRoundRect(10*density,getHeight()-34*density,getWidth()-10*density,getHeight()-8*density,5*density,5*density,paint);
        label(canvas,"执掌 "+world.faction(world.player)+"    山 / 水不可通行    双指缩放 · 城池列表定位",getWidth()/2f,getHeight()-17*density,10*density,PAPER);
    }
    private void drawCity(Canvas c,World.City city){float cx=x(city.hex),cy=y(city.hex);int owner=factionColor(city.owner);
        paint.setColor(Color.rgb(32,44,40));c.drawRect(cx-17,cy-8,cx+17,cy+12,paint);paint.setColor(Color.rgb(210,196,161));
        c.drawRect(cx-15,cy-6,cx+15,cy+9,paint);for(int i=-15;i<15;i+=6)c.drawRect(cx+i,cy-10,cx+i+4,cy-4,paint);
        paint.setColor(Color.rgb(61,62,46));c.drawRect(cx-4,cy+1,cx+4,cy+12,paint);paint.setColor(owner);c.drawRect(cx,cy-29,cx+13,cy-17,paint);paint.setStrokeWidth(1.5f);c.drawLine(cx,cy-30,cx,cy-9,paint);
        float sz=Math.min(13*density/scale,22);paint.setColor(Color.argb(225,22,37,37));c.drawRoundRect(cx-sz*1.8f,cy+14,cx+sz*1.8f,cy+14+sz*1.6f,3,3,paint);
        label(c,city.name,cx,cy+14+sz*1.15f,sz,PAPER);
        paint.setColor(owner);c.drawRect(cx-16,cy+10,cx-16+32*Math.min(1,city.defense/3000f),cy+13,paint);
    }
    private void drawUnit(Canvas c,World.Unit u){float cx=x(u.hex),cy=y(u.hex);paint.setColor(Color.rgb(24,38,37));c.drawCircle(cx+1,cy+2,15,paint);paint.setColor(factionColor(u.owner));c.drawCircle(cx,cy,14,paint);
        label(c,u.weapon.label.substring(0,1),cx,cy+5,16,Color.rgb(18,34,34));
        if(u.acted){paint.setColor(Color.argb(140,17,32,37));c.drawCircle(cx,cy,14,paint);label(c,"✓",cx,cy+4,14,PAPER);}
        float sz=10*density/scale;label(c,world.officer(u.officerId).name+" "+u.troops,cx,cy-19,sz,PAPER);
    }
}
