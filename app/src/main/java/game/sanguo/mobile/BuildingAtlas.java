package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import java.io.*;

/** Original alpha sprites. Measured rectangles follow each complete silhouette, avoiding atlas-cell clipping. */
final class BuildingAtlas {
    private static Bitmap bitmap;private static boolean loaded;
    private static final Paint PAINT=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private static final RectF TARGET=new RectF();
    private static final Rect[] SPRITES={
        new Rect(7,11,359,299),new Rect(361,48,629,287),new Rect(650,76,961,317),new Rect(967,95,1246,303),
        new Rect(15,344,313,586),new Rect(329,338,605,584),new Rect(630,329,944,586),new Rect(965,315,1243,577),
        new Rect(10,646,307,884),new Rect(326,651,624,893),new Rect(642,619,938,893),new Rect(955,641,1244,892),
        new Rect(48,957,329,1196),new Rect(391,905,573,1203),new Rect(673,910,906,1210),new Rect(973,940,1225,1211)
    };
    static void load(Context context){if(loaded)return;loaded=true;try(InputStream input=context.getAssets().open("map/buildings.png")){bitmap=BitmapFactory.decodeStream(input);}catch(IOException ignored){/* Native silhouettes remain available. */}}
    static boolean draw(Canvas canvas,int index){
        if(bitmap==null||index<3||index>=SPRITES.length)return false;
        Rect source=SPRITES[index];float scale=Math.min(54f/source.width(),46f/source.height()),width=source.width()*scale,height=source.height()*scale;
        TARGET.set(-width/2,14-height,width/2,14);canvas.drawBitmap(bitmap,source,TARGET,PAINT);return true;
    }
}
