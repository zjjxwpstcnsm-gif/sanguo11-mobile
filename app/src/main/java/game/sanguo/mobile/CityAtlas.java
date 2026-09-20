package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import game.sanguo.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** v056 original city/gate/port art. All LODs retain buildings; missing art is an error,
 * never a fallback to the old city sprite or faction-coloured footprint. */
final class CityAtlas {
    private static final Map<String,Sprite[]> CATALOG=new HashMap<>();
    private static final Paint PAINT=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private static final RectF TARGET=new RectF();
    private static long decodedBytes;
    private static final class Sprite {
        final Bitmap bitmap;final Rect rect;final float px,py,width;
        Sprite(Bitmap bitmap,JSONObject entry)throws JSONException {
            this.bitmap=bitmap;JSONArray r=entry.getJSONArray("rect"),p=entry.getJSONArray("pivot");
            rect=new Rect(r.getInt(0),r.getInt(1),r.getInt(2),r.getInt(3));
            px=(float)p.getDouble(0);py=(float)p.getDouble(1);width=(float)entry.getDouble("world_width");
            if(rect.width()!=bitmap.getWidth()||rect.height()!=bitmap.getHeight()||!bitmap.hasAlpha())throw new IllegalStateException("Invalid city alpha/rect");
        }
    }
    private static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);return out.toByteArray();}
    static synchronized void load(Context context){
        if(!CATALOG.isEmpty())return;
        Map<String,Sprite[]> pending=new HashMap<>();long bytes=0;
        try(InputStream input=context.getAssets().open("map/cities-v056/catalog.json")){
            JSONObject root=new JSONObject(new String(read(input),StandardCharsets.UTF_8));
            if(root.getInt("map_revision")!=CityArtCatalog.ASSET_REVISION)throw new IOException("City atlas asset revision mismatch");
            JSONObject variants=root.getJSONObject("variants");
            for(Iterator<String> keys=variants.keys();keys.hasNext();){String key=keys.next();JSONObject lods=variants.getJSONObject(key);Sprite[] sprites=new Sprite[3];
                String[] names={"near","mid","far"};
                for(int i=0;i<names.length;i++){JSONObject entry=lods.getJSONObject(names[i]);
                    try(InputStream png=context.getAssets().open("map/cities-v056/"+entry.getString("file"))){
                        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inScaled=false;opts.inPreferredConfig=Bitmap.Config.ARGB_8888;
                        Bitmap bitmap=BitmapFactory.decodeStream(png,null,opts);if(bitmap==null)throw new IOException("Cannot decode city "+key+"/"+names[i]);
                        sprites[i]=new Sprite(bitmap,entry);bytes+=bitmap.getAllocationByteCount();
                    }
                }pending.put(key,sprites);
            }
            for(int id=20000;id<=20041;id++)if(!pending.containsKey(CityArtCatalog.key(id)))throw new IOException("Missing city art "+id);
            if(!pending.containsKey("gate")||!pending.containsKey("port"))throw new IOException("Missing gate/port art");
        }catch(IOException|JSONException error){for(Sprite[] set:pending.values())for(Sprite s:set)if(s!=null)s.bitmap.recycle();throw new IllegalStateException("v056 城池资源缺失，请重新安装完整 APK",error);}
        CATALOG.putAll(pending);decodedBytes=bytes;
    }
    static long bytes(){return decodedBytes;}
    static String lod(float projectedWidth){return projectedWidth>=190?"NEAR":projectedWidth>=58?"MID":"FAR";}
    private static Sprite sprite(String key,float projectedWidth){
        Sprite[] sprites=CATALOG.get(key);if(sprites==null)throw new IllegalStateException("City atlas not loaded: "+key);
        return sprites[projectedWidth>=190?0:projectedWidth>=58?1:2];
    }
    private static void draw(Canvas c,String key,float x,float y,float width,float scale,int owner){
        Sprite s=sprite(key,width*scale);float factor=width/s.rect.width();
        TARGET.set(x-s.px*factor,y-s.py*factor,x+(s.rect.width()-s.px)*factor,y+(s.rect.height()-s.py)*factor);
        PAINT.setColor(Color.WHITE);PAINT.setStyle(Paint.Style.FILL);c.drawBitmap(s.bitmap,s.rect,TARGET,PAINT);
        // Faction colour is a small flag, never a colour filter on architecture.
        float flagX=x+width*.19f,flagY=y-width*.16f;
        PAINT.setStrokeWidth(Math.max(.65f,.8f/Math.max(scale,.1f)));PAINT.setColor(0xff65543d);c.drawLine(flagX,flagY,flagX,flagY+width*.085f,PAINT);
        PAINT.setColor(owner);c.drawRect(flagX,flagY,flagX+width*.048f,flagY+width*.030f,PAINT);
    }
    static void drawMap(Canvas c,World.City city,float x,float y,float scale,int owner){
        String key=CityArtCatalog.key(city);Sprite s=sprite(key,100);draw(c,key,x,y,s.width,scale,owner);
    }
    static void drawIcon(Canvas c,World.City city,int owner){draw(c,CityArtCatalog.key(city),0,2,65,2,owner);}
    static void drawIcon(Canvas c,World.SiteKind kind,int owner){draw(c,kind==World.SiteKind.GATE?"gate":kind==World.SiteKind.PORT?"port":"standard",0,2,65,2,owner);}
}
