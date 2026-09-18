package game.sanguo.mobile;

import android.graphics.*;
import game.sanguo.core.World;
import java.util.Random;

/** Original cached terrain art. Stable tile variants never shimmer when panning. */
final class TerrainTiles {
    private final Bitmap[][] tiles=new Bitmap[World.Terrain.values().length][3];
    private final Bitmap[][] connections=new Bitmap[2][64];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint coast=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF destination=new RectF();
    static int color(World.Terrain t){
        switch(t){
            case FOREST:return 0xff395c43;
            case MOUNTAIN:return 0xff717774;
            case WATER:return 0xff427f99;
            case SEA:return 0xff244b6c;
            case VOID:return 0xff152331;
            case MOUNTAIN_PATH:return 0xff97836a;
            case SHALLOWS:return 0xff6da6a7;
            case PLANK_ROAD:return 0xff5f6a61;
            case POISON:return 0xff65566f;
            case SWAMP:return 0xff566158;
            case DAM:return 0xffa09079;
            default:return 0xff8a9569;
        }
    }
    void draw(Canvas c,World world,int q,int r,float x,float y){
        World.Terrain terrain=world.terrain[q][r];
        int variant=Math.floorMod(q*31+r*17,3);
        Bitmap tile=tiles[terrain.ordinal()][variant];
        if(tile==null)tiles[terrain.ordinal()][variant]=tile=create(terrain,variant);
        destination.set(x-25,y-25,x+25,y+25);c.drawBitmap(tile,null,destination,paint);
        if(TerrainConnections.road(terrain)){
            int kind=terrain==World.Terrain.PLANK_ROAD?0:1,mask=TerrainConnections.mask(world,q,r);
            Bitmap overlay=connections[kind][mask];
            if(overlay==null)connections[kind][mask]=overlay=connection(kind==0,mask);
            c.drawBitmap(overlay,null,destination,paint);
        }else if(TerrainConnections.water(terrain)){
            int mask=TerrainConnections.mask(world,q,r);coast.setStrokeWidth(1.2f);coast.setColor(0xffb5c3a0);
            for(int d=0;d<6;d++)if((mask&(1<<d))==0){
                int nq=q+TerrainConnections.DQ[d],nr=r+TerrainConnections.DR[d];
                if(!TerrainConnections.inside(world,nq,nr)||world.terrain[nq][nr]==World.Terrain.VOID)continue;
                double a=-d*Math.PI/3;float ex=TerrainConnections.edgeX(d),ey=TerrainConnections.edgeY(d),px=(float)-Math.sin(a)*12.5f,py=(float)Math.cos(a)*12.5f;
                c.drawLine(x+ex-px,y+ey-py,x+ex+px,y+ey+py,coast);
            }
        }
    }
    private Bitmap connection(boolean plank,int mask){
        Bitmap b=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.scale(1.92f,1.92f);c.translate(25,25);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStrokeCap(Paint.Cap.ROUND);
        Path deck=new Path();
        if(mask==0){deck.moveTo(-7,0);deck.lineTo(7,0);}
        else for(int d=0;d<6;d++)if((mask&(1<<d))!=0){deck.moveTo(0,0);deck.lineTo(TerrainConnections.edgeX(d)*1.03f,TerrainConnections.edgeY(d)*1.03f);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(plank?13:8);p.setColor(0xcc283b35);c.drawPath(deck,p);
        p.setStrokeWidth(plank?10:5.5f);p.setColor(plank?0xff715739:0xff887b5f);c.drawPath(deck,p);
        p.setStrokeWidth(plank?8.5f:3.5f);p.setColor(plank?0xffc4a46e:0xffd9c38e);c.drawPath(deck,p);
        if(plank){
            p.setStrokeCap(Paint.Cap.BUTT);p.setStrokeWidth(.8f);p.setColor(0xff68513a);
            for(int d=0;d<6;d++)if((mask&(1<<d))!=0){
                c.save();c.rotate(-60*d);
                for(float pos=4;pos<22;pos+=3.5f){c.drawLine(pos,-4.1f,pos,4.1f,p);}
                p.setColor(0xffe5c994);c.drawLine(6,-5,21.7f,-5,p);c.drawLine(6,5,21.7f,5,p);
                p.setColor(0xff68513a);c.restore();
            }
            p.setStyle(Paint.Style.FILL);p.setColor(0xffc4a46e);c.drawCircle(0,0,3,p);
        }
        return b;
    }
    private Bitmap create(World.Terrain t,int variant){
        Bitmap bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.scale(2.56f,2.56f);c.translate(25,25);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Path hex=new Path();
        for(int i=0;i<6;i++){double a=Math.toRadians(i*60-30);float x=(float)Math.cos(a)*25.05f,y=(float)Math.sin(a)*25.05f;if(i==0)hex.moveTo(x,y);else hex.lineTo(x,y);}hex.close();c.clipPath(hex);
        c.drawColor(color(t));
        if(VisualAssets.terrainReady()&&t!=World.Terrain.VOID){
            VisualAssets.texture(c,TerrainConnections.road(t)?World.Terrain.MOUNTAIN:t,variant);return bitmap;
        }
        Random random=new Random(t.ordinal()*101+variant*29);
        for(int i=0;i<70;i++){p.setColor(i%2==0?0x11242e22:0x17d5d6ae);c.drawCircle(random.nextFloat()*50-25,random.nextFloat()*50-25,random.nextFloat()*2+1,p);}
        switch(t){
            case PLAIN:
                p.setColor(0xff657b47);p.setStrokeWidth(.55f);
                for(int i=0;i<15;i++){float x=random.nextFloat()*38-19,y=random.nextFloat()*36-18;c.drawLine(x-1,y,x,y-2,p);c.drawLine(x,y,x+1.3f,y-1.6f,p);}break;
            case FOREST:
                for(int i=0;i<9;i++){float x=(i%3-1)*12+random.nextFloat()*4-2,y=(i/3-1)*12+random.nextFloat()*4-2;p.setColor(0xff30402c);c.drawOval(x-5,y+2,x+7,y+6,p);p.setColor(0xff80684a);c.drawRect(x-.8f,y-1,x+.8f,y+6,p);p.setColor(i%2==0?0xff244f39:0xff42764c);c.drawCircle(x,y-2,5.6f,p);p.setColor(0xff73925c);c.drawCircle(x-1.7f,y-3.8f,2.6f,p);}break;
            case MOUNTAIN:case MOUNTAIN_PATH:case PLANK_ROAD:
                for(int i=0;i<3;i++){float x=(i-1)*15,y=i%2*10-4;Path mountain=new Path();mountain.moveTo(x-12,y+15);mountain.lineTo(x,y-15);mountain.lineTo(x+14,y+15);mountain.close();p.setColor(0xffadb09d);c.drawPath(mountain,p);mountain.reset();mountain.moveTo(x,y-15);mountain.lineTo(x+14,y+15);mountain.lineTo(x-1,y+7);mountain.close();p.setColor(0xff535f5d);c.drawPath(mountain,p);}
                break;
            case SEA:case WATER:case SHALLOWS:
                if(t==World.Terrain.SHALLOWS){p.setColor(0xffb9bea0);c.drawOval(-24,-8,-4,12,p);c.drawOval(5,5,20,19,p);}
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.75f);p.setColor(t==World.Terrain.WATER?0xff779da9:0xffc4dfcc);
                for(int i=0;i<6;i++){float y=i*8-20,x=(i+variant)%2*8-23;Path wave=new Path();wave.moveTo(x,y);wave.cubicTo(x+5,y-3,x+11,y+3,x+17,y);c.drawPath(wave,p);}p.setStyle(Paint.Style.FILL);break;
            case POISON:
                p.setColor(0xff353e42);c.drawOval(-17,-10,16,14,p);p.setColor(0xff88945a);c.drawOval(-13,-8,12,9,p);p.setColor(0xffb6c379);for(int i=0;i<6;i++)c.drawCircle(random.nextFloat()*20-10,random.nextFloat()*12-6,1.2f,p);p.setColor(0xffd1c3d5);p.setStrokeWidth(.8f);c.drawLine(-5,-12,-3,-17,p);c.drawLine(5,-11,7,-19,p);break;
        }
        return bitmap;
    }
}
