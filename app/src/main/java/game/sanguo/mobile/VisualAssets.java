package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import game.sanguo.core.*;
import java.io.*;

/** Shared, bounded v0.40 artwork. Decode once; no allocations while drawing the map. */
final class VisualAssets {
    private static final String[] FILES={"map/units-v040.png","map/items-facilities-v040.png","map/terrain-v040.png","map/military-v040.png","map/units-v043.png"};
    private static final Bitmap[] SHEETS=new Bitmap[FILES.length];
    private static final Rect[][] CELLS=new Rect[FILES.length][16];
    private static final int[] MILITARY_ROWS={0,345,645,921,1254};
    // Reviewed transparent gutters; preserve complete spear tips, boots and cavalry hooves.
    private static final int[][] UNIT_ROWS={{0,344,693,969,1254},{0,374,671,955,1254},{0,345,660,973,1254},{0,361,662,979,1254}};
    private static Bitmap teamMask;
    private static final android.util.SparseArray<LightingColorFilter> TEAM_COLORS=new android.util.SparseArray<>();
    private static final Paint PAINT=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private static final RectF TARGET=new RectF();
    private static int[][] samples;
    private static boolean loaded;

    static synchronized void load(Context context){
        if(loaded)return;
        for(int sheet=0;sheet<FILES.length;sheet++){
            try(InputStream input=context.getAssets().open(FILES[sheet])){
                BitmapFactory.Options options=new BitmapFactory.Options();if(sheet==4)options.inSampleSize=2;
                Bitmap bitmap=BitmapFactory.decodeStream(input,null,options);
                if(bitmap==null)continue;
                int w=bitmap.getWidth(),h=bitmap.getHeight();
                int[] pixels=new int[w*h];bitmap.getPixels(pixels,0,w,0,0,w,h);
                if(sheet==2)samples=new int[16][64*64];
                for(int cell=0;cell<16;cell++){
                    int l=(cell%4)*w/4,r=(cell%4+1)*w/4;
                    int t=(cell/4)*h/4,b=(cell/4+1)*h/4;
                    if(sheet==3){t=MILITARY_ROWS[cell/4]*h/1254;b=MILITARY_ROWS[cell/4+1]*h/1254;}
                    if(sheet==4){t=UNIT_ROWS[cell%4][cell/4]*h/1254;b=UNIT_ROWS[cell%4][cell/4+1]*h/1254;}
                    if(sheet!=2){
                        int left=r,top=b,right=l,bottom=t;
                        for(int y=t;y<b;y++)for(int x=l;x<r;x++)if((pixels[y*w+x]>>>24)>12){
                            left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);
                        }
                        if(right>left){l=left;r=right;t=top;b=bottom;}
                    }
                    CELLS[sheet][cell]=new Rect(l,t,r,b);
                    if(sheet==2)for(int y=0;y<64;y++)for(int x=0;x<64;x++)
                        samples[cell][y*64+x]=pixels[(t+(b-t)*y/64)*w+l+(r-l)*x/64];
                }
                if(sheet==2)for(int cell=1;cell<3;cell++)for(int i=0;i<4096;i++)samples[cell][i]=mix(samples[0][i],samples[cell][i]);
                SHEETS[sheet]=bitmap;
                if(sheet==4){
                    float[] hsv=new float[3];
                    for(int i=0;i<pixels.length;i++){
                        int pixel=pixels[i];Color.colorToHSV(pixel,hsv);
                        // Only blue cloth/pennants: steel, skin, wood and horses keep their materials.
                        float coverage=hsv[0]>=195&&hsv[0]<=255?Math.min(1,Math.max(0,(hsv[1]-.20f)/.25f)):0;
                        int alpha=Math.round((pixel>>>24)*coverage),shade=Math.min(255,Math.round(hsv[2]*340));
                        pixels[i]=Color.argb(alpha,shade,shade,shade);
                    }
                    teamMask=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
                }
            }catch(IOException e){android.util.Log.e("VisualAssets","Missing packaged artwork: "+FILES[sheet],e);}
        }
        loaded=true;
    }
    static boolean ready(){for(Bitmap sheet:SHEETS)if(sheet==null)return false;return true;}
    static boolean terrainReady(){return SHEETS[2]!=null;}
    static int[][] terrainSamples(){return samples;}
    static long bytes(){long bytes=teamMask==null?0:teamMask.getAllocationByteCount();for(Bitmap b:SHEETS)if(b!=null)bytes+=b.getAllocationByteCount();return bytes;}
    static boolean drawUnit(Canvas c,int cell,float width,float height,float baseline,int color){
        if(!draw(c,4,cell,width,height,baseline))return false;
        if(teamMask!=null){
            LightingColorFilter filter=TEAM_COLORS.get(color);
            if(filter==null){if(TEAM_COLORS.size()>=40)TEAM_COLORS.clear();filter=new LightingColorFilter(color,0);TEAM_COLORS.put(color,filter);}
            PAINT.setColorFilter(filter);c.drawBitmap(teamMask,CELLS[4][cell],TARGET,PAINT);PAINT.setColorFilter(null);
        }
        return true;
    }
    static boolean draw(Canvas c,int sheet,int cell,float width,float height,float baseline){
        if(sheet<0||sheet>=SHEETS.length||cell<0||cell>=16||SHEETS[sheet]==null)return false;
        Rect source=CELLS[sheet][cell];float scale=Math.min(width/source.width(),height/source.height());
        float w=source.width()*scale,h=source.height()*scale;
        TARGET.set(-w/2,baseline-h,w/2,baseline);c.drawBitmap(SHEETS[sheet],source,TARGET,PAINT);return true;
    }
    static int weapon(World.Weapon w){
        switch(w){case SWORD:return 0;case SPEAR:return 1;case HALBERD:return 2;case CROSSBOW:return 3;
            case CAVALRY:return 4;case RAM:return 5;case SIEGE_TOWER:return 6;case WOODEN_BEAST:return 7;case CATAPULT:return 8;default:return -1;}
    }
    static int ship(Army.Ship s){return s==Army.Ship.BOAT?9:s==Army.Ship.TOWER_SHIP?10:11;}
    static int facility(Domestic.Kind k){
        switch(k){case MARKET:case BLACK_MARKET:return 8;case GRANARY:return 9;case BARRACKS:return 10;
            case SMITH:return 11;case STABLE:return 12;case WORKSHOP:return 13;case SHIPYARD:return 14;case MINT:return 15;default:return -1;}
    }
    static int structure(War.StructureKind k){
        switch(k){case CAMP:return 1;case FORT:return 2;case CROSSBOW_TOWER:return 3;case CATAPULT_TOWER:return 4;
            case STONE_MAZE:return 5;case EARTH_WALL:return 6;case STONE_WALL:return 7;case FIRE_SEED:return 8;
            case FLAME_SEED:return 9;case INFERNO_SEED:return 10;case FIRE_BALL:return 11;case FLAME_BALL:return 12;
            case INFERNO_BALL:return 13;case FIRE_SHIP:return 14;case DAM:return 15;default:return -1;}
    }
    static int terrainCell(World.Terrain t,int variant){
        switch(t){case PLAIN:return variant;case FOREST:return 3+variant%2;case MOUNTAIN:return 5+variant%2;
            case WATER:return 7;case SEA:return 8;case SHALLOWS:return 9;case SWAMP:return 10;case MOUNTAIN_PATH:return 11;
            case PLANK_ROAD:return 12;case POISON:return 13;case DAM:return 14;default:return -1;}
    }
    static void texture(Canvas c,World.Terrain t,int variant){
        int cell=terrainCell(t,variant);if(!terrainReady()||cell<0)return;
        TARGET.set(-25,-25,25,25);
        if(t==World.Terrain.PLAIN&&cell>0){
            c.drawBitmap(SHEETS[2],CELLS[2][0],TARGET,PAINT);PAINT.setAlpha(64);
            c.drawBitmap(SHEETS[2],CELLS[2][cell],TARGET,PAINT);PAINT.setAlpha(255);
        }else c.drawBitmap(SHEETS[2],CELLS[2][cell],TARGET,PAINT);
    }
    private static int mix(int a,int b){
        return 0xff000000|(((a>>16&255)*191+(b>>16&255)*64)/255)<<16
            |(((a>>8&255)*191+(b>>8&255)*64)/255)<<8|((a&255)*191+(b&255)*64)/255;
    }
    private VisualAssets(){}
}
