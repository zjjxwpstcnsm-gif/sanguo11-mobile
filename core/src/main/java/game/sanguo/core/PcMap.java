package game.sanguo.core;

import java.io.*;
import java.util.zip.GZIPInputStream;

/** Authored PC landscape, immutable and separate from mutable gameplay/save data.
 * The source installation is identified in docs/pc-map/source.json. */
public final class PcMap {
    public static final int VERTICES=1025, FACES=1024;
    // Supplied EXE415920: height byte * .5 PC units; scene XYZ * .05.
    public static final float ORIGIN=114, WORLD_SCALE=.05f, SCALE=.025f, MAX_HEIGHT=255*SCALE;
    private final byte[] heights=new byte[VERTICES*VERTICES];
    private final byte[] water=new byte[FACES*FACES];
    private final byte[] cells=new byte[200*200*11];
    // Separate immutable presentation resource; never serialized as rule data.
    private final byte[] coarseWater=new byte[256*256*10];
    private static final class Holder { static final PcMap INSTANCE=read(); }
    public static PcMap get(){return Holder.INSTANCE;}
    private PcMap(){}
    private static PcMap read(){
        PcMap result=new PcMap();
        try(InputStream resource=PcMap.class.getResourceAsStream("/maps/pc-map.bin.gz")){
            if(resource==null)throw new IOException("PC地图资源缺失");
            try(DataInputStream in=new DataInputStream(new GZIPInputStream(resource))){
                if(in.readLong()!=0x50434d4150303032L)throw new IOException("PC地图版本错误");
                in.readFully(result.heights);in.readFully(result.water);in.readFully(result.cells);
                if(in.read()!=-1)throw new IOException("PC地图长度错误");
            }
        }catch(IOException e){throw new IllegalStateException("PC地图读取失败",e);}
        try(InputStream resource=PcMap.class.getResourceAsStream("/maps/pc-water.bin.gz")){
            if(resource==null)throw new IOException("PC原水面资源缺失");
            try(DataInputStream in=new DataInputStream(new GZIPInputStream(resource))){
                if(in.readLong()!=0x504357434f303031L)throw new IOException("PC原水面版本错误");
                in.readFully(result.coarseWater);
                if(in.read()!=-1)throw new IOException("PC原水面长度错误");
            }
            for(int x=0;x<256;x++)for(int y=0;y<256;y++)if(result.coarseWaterByte(x,y)>0){
                int period=result.coarseWaterPeriod(x,y);
                if(period<10000||period>=20000||result.coarseWaterPhase(x,y)>=period
                        ||result.coarseWaterMask(x,y)>15||result.coarseWaterSheet(x,y)>1)
                    throw new IOException("PC原水面记录错误");
            }
        }catch(IOException e){throw new IllegalStateException("PC原水面读取失败",e);}
        return result;
    }
    public int terrain(int x,int y){return x<0||y<0||x>=200||y>=200?15:cells[(x*200+y)*11]&255;}
    public int region(int x,int y){return cells[(x*200+y)*11+1]&255;}
    public boolean development(int x,int y){return cells[(x*200+y)*11+5]!=0;}
    public int heightByte(int x,int y){return heights[Math.max(0,Math.min(1024,y))*VERTICES+Math.max(0,Math.min(1024,x))]&255;}
    public float vertex(int x,int y){return heightByte(x,y)*SCALE;}
    /** The same two triangles per quarter-cell used by the native ground mesh. */
    public float height(float sourceX,float staggeredY){
        // Supplied415cb0 queries the coarse water plane at arbitrary positions.
        return Math.max(landHeight(sourceX,staggeredY),coarseWaterHeight(sourceX,staggeredY));
    }
    public float landHeight(float sourceX,float staggeredY){
        // Preserve the actual scene coordinate when converting back to the
        // quarter-grid. Adding114 in float32 discarded an extra coordinate
        // bit and sampled the dry side of correctly clipped shore vertices.
        double x=Math.max(0,Math.min(1024,(double)sourceX*4+ORIGIN));
        double y=Math.max(0,Math.min(1024,(double)staggeredY*4+ORIGIN));
        int ix=Math.min(1023,(int)Math.floor(x)),iy=Math.min(1023,(int)Math.floor(y));
        float u=(float)(x-ix),v=(float)(y-iy),a=vertex(ix,iy),b=vertex(ix+1,iy),c=vertex(ix,iy+1),d=vertex(ix+1,iy+1);
        return v>=u?a*(1-v)+c*(v-u)+d*u:a*(1-u)+d*v+b*(u-v);
    }
    /** Complete K3ST bits44..51. Supplied EXE415bb0 uses a small source
     * separation bias before conversion to PC world units. A zero byte means
     * no authored water plane; it must not turn into a positive biased plane. */
    public float waterHeight(float sourceX,float staggeredY){
        int x=Math.max(0,Math.min(1023,(int)Math.floor((double)sourceX*4+ORIGIN)));
        int y=Math.max(0,Math.min(1023,(int)Math.floor((double)staggeredY*4+ORIGIN)));
        int value=water[y*FACES+x]&255;
        float pcHeight=value==0?0:(float)((value+(double).0025f)*.5);
        return pcHeight*WORLD_SCALE;
    }
    private int coarseOffset(int x,int y){
        if(x<0||y<0||x>=256||y>=256)throw new IndexOutOfBoundsException("PC coarse water");
        return (x*256+y)*10;
    }
    private int coarseU16(int x,int y,int field){int offset=coarseOffset(x,y)+field;return (coarseWater[offset]&255)|((coarseWater[offset+1]&255)<<8);}
    public int coarseWaterPhase(int x,int y){return coarseU16(x,y,0);}
    public int coarseWaterPeriod(int x,int y){return coarseU16(x,y,2);}
    public int coarseWaterByte(int x,int y){return coarseWater[coarseOffset(x,y)+7]&255;}
    public int coarseWaterMask(int x,int y){return coarseWater[coarseOffset(x,y)+8]&255;}
    public int coarseWaterSheet(int x,int y){return coarseWater[coarseOffset(x,y)+9]&3;}
    public float coarseWaterPlane(int x,int y){
        int value=coarseWaterByte(x,y);
        return value==0?0:(float)((value+(double).0025f)*.5)*WORLD_SCALE;
    }
    public float coarseWaterHeight(float sourceX,float staggeredY){
        int x=Math.max(0,Math.min(255,(int)Math.floor((double)sourceX+ORIGIN*.25)));
        int y=Math.max(0,Math.min(255,(int)Math.floor((double)staggeredY+ORIGIN*.25)));
        return coarseWaterPlane(x,y);
    }
}
