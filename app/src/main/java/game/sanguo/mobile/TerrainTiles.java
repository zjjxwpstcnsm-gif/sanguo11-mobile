package game.sanguo.mobile;

import android.graphics.*;
import game.sanguo.core.World;
import java.util.Random;

/** Original cached terrain art. Stable tile variants never shimmer when panning. */
final class TerrainTiles {
    private final Bitmap[][] tiles=new Bitmap[World.Terrain.values().length][3];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF destination=new RectF();
    static int color(World.Terrain t){
        switch(t){
            case FOREST:return 0xff395c43;
            case MOUNTAIN:return 0xff717774;
            case WATER:return 0xff315f79;
            case MOUNTAIN_PATH:return 0xff97836a;
            case SHALLOWS:return 0xff6da6a7;
            case PLANK_ROAD:return 0xff5f6a61;
            case POISON:return 0xff65566f;
            default:return 0xff8a9569;
        }
    }
    void draw(Canvas c,World.Terrain terrain,int q,int r,float x,float y){
        int variant=Math.floorMod(q*31+r*17,3);
        Bitmap tile=tiles[terrain.ordinal()][variant];
        if(tile==null)tiles[terrain.ordinal()][variant]=tile=create(terrain,variant);
        destination.set(x-25,y-25,x+25,y+25);c.drawBitmap(tile,null,destination,paint);
    }
    private Bitmap create(World.Terrain t,int variant){
        Bitmap bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.scale(2.56f,2.56f);c.translate(25,25);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Path hex=new Path();
        for(int i=0;i<6;i++){double a=Math.toRadians(i*60-30);float x=(float)Math.cos(a)*24.7f,y=(float)Math.sin(a)*24.7f;if(i==0)hex.moveTo(x,y);else hex.lineTo(x,y);}hex.close();c.clipPath(hex);
        c.drawColor(color(t));Random random=new Random(t.ordinal()*101+variant*29);
        for(int i=0;i<70;i++){p.setColor(i%2==0?0x11242e22:0x17d5d6ae);c.drawCircle(random.nextFloat()*50-25,random.nextFloat()*50-25,random.nextFloat()*2+1,p);}
        switch(t){
            case PLAIN:
                p.setColor(0xff657b47);p.setStrokeWidth(.55f);
                for(int i=0;i<15;i++){float x=random.nextFloat()*38-19,y=random.nextFloat()*36-18;c.drawLine(x-1,y,x,y-2,p);c.drawLine(x,y,x+1.3f,y-1.6f,p);}break;
            case FOREST:
                for(int i=0;i<9;i++){float x=(i%3-1)*12+random.nextFloat()*4-2,y=(i/3-1)*12+random.nextFloat()*4-2;p.setColor(0xff30402c);c.drawOval(x-5,y+2,x+7,y+6,p);p.setColor(0xff80684a);c.drawRect(x-.8f,y-1,x+.8f,y+6,p);p.setColor(i%2==0?0xff244f39:0xff42764c);c.drawCircle(x,y-2,5.6f,p);p.setColor(0xff73925c);c.drawCircle(x-1.7f,y-3.8f,2.6f,p);}break;
            case MOUNTAIN:case MOUNTAIN_PATH:case PLANK_ROAD:
                for(int i=0;i<3;i++){float x=(i-1)*15,y=i%2*10-4;Path mountain=new Path();mountain.moveTo(x-12,y+15);mountain.lineTo(x,y-15);mountain.lineTo(x+14,y+15);mountain.close();p.setColor(0xffadb09d);c.drawPath(mountain,p);mountain.reset();mountain.moveTo(x,y-15);mountain.lineTo(x+14,y+15);mountain.lineTo(x-1,y+7);mountain.close();p.setColor(0xff535f5d);c.drawPath(mountain,p);}
                if(t==World.Terrain.MOUNTAIN_PATH){Path road=new Path();road.moveTo(-20,22);road.cubicTo(8,8,-9,2,18,-23);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);p.setColor(0xff675944);c.drawPath(road,p);p.setStrokeWidth(3.5f);p.setColor(0xffd4bb85);c.drawPath(road,p);p.setStyle(Paint.Style.FILL);}
                if(t==World.Terrain.PLANK_ROAD){p.setColor(0xff333e3c);c.drawRect(-8,-26,8,26,p);for(int y=-25;y<26;y+=4){p.setColor(y%3==0?0xffd4b177:0xffb89760);c.drawRect(-7,y,7,y+3,p);}p.setColor(0xffe1c895);p.setStrokeWidth(.8f);c.drawLine(-9,-25,-9,25,p);c.drawLine(9,-25,9,25,p);}break;
            case WATER:case SHALLOWS:
                if(t==World.Terrain.SHALLOWS){p.setColor(0xffb9bea0);c.drawOval(-24,-8,-4,12,p);c.drawOval(5,5,20,19,p);}
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.75f);p.setColor(t==World.Terrain.WATER?0xff779da9:0xffc4dfcc);
                for(int i=0;i<6;i++){float y=i*8-20,x=(i+variant)%2*8-23;Path wave=new Path();wave.moveTo(x,y);wave.cubicTo(x+5,y-3,x+11,y+3,x+17,y);c.drawPath(wave,p);}p.setStyle(Paint.Style.FILL);break;
            case POISON:
                p.setColor(0xff353e42);c.drawOval(-17,-10,16,14,p);p.setColor(0xff88945a);c.drawOval(-13,-8,12,9,p);p.setColor(0xffb6c379);for(int i=0;i<6;i++)c.drawCircle(random.nextFloat()*20-10,random.nextFloat()*12-6,1.2f,p);p.setColor(0xffd1c3d5);p.setStrokeWidth(.8f);c.drawLine(-5,-12,-3,-17,p);c.drawLine(5,-11,7,-19,p);break;
        }
        return bitmap;
    }
}
