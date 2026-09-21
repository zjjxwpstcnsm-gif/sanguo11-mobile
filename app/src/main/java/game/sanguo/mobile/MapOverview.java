package game.sanguo.mobile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import game.sanguo.core.Territory;
import game.sanguo.core.World;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Immutable geographical snapshot, rasterized off the UI thread. No live World access by workers. */
final class MapOverview {
    static final int BACKGROUND=0xff172c2e;
    private static final float RADIUS=TileGeometry.RADIUS, DX=TileGeometry.DX, DY=TileGeometry.DY;
    // Three complete modes together use at most 15 MiB, irrespective of map size or device density.
    private static final int MAX_PIXELS=1_300_000, MAX_SIDE=1536;
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{
        Thread thread=new Thread(r,"map-overview");thread.setDaemon(true);return thread;
    });
    private final int width,height;
    private final float offset;
    private final boolean columnStaggered;
    private final int[] terrain,sites,owners,colors;
    private final byte[] roads;
    private final int[] styles;
    private final int[][] artwork;
    private final RectF bounds;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Bitmap[] preview;
    private Bitmap[] full;
    private Future<?> task;
    private int generation;

    MapOverview(World world,Territory territory,float worldWidth,float worldHeight,float columnOffset){
        width=world.width;height=world.height;offset=columnOffset;columnStaggered=world.columnStaggered;
        bounds=new RectF(-RADIUS,-RADIUS,worldWidth-RADIUS,worldHeight-RADIUS);
        int count=width*height;
        terrain=new int[count];sites=new int[count];owners=new int[count];colors=new int[count];
        roads=new byte[count];styles=new int[count];Arrays.fill(styles,-1);artwork=VisualAssets.terrainSamples();
        Arrays.fill(sites,-1);Arrays.fill(owners,-1);
        int[] palette=new int[world.factions.length];
        for(int i=0;i<palette.length;i++)palette[i]=FactionColors.color(world,i);
        for(int r=0;r<height;r++)for(int q=0;q<width;q++){
            int index=r*width+q;
            World.Terrain t=TerrainConnections.appearance(world,q,r);
            if(t==World.Terrain.VOID)continue;
            terrain[index]=TerrainTiles.color(t);
            styles[index]=VisualAssets.terrainCell(TerrainArt.ground(t),Math.floorMod(q*31+r*17,3));
            if(world.terrain[q][r]==World.Terrain.VOID)continue; // exterior: no owner/site/road/territory colour
            if(TerrainConnections.road(world.terrain[q][r]))roads[index]=(byte)(64|TerrainConnections.mask(world,q,r));
            sites[index]=territory.siteAt(q,r);owners[index]=territory.ownerAt(q,r);
            colors[index]=owners[index]<0?0xffa6a6a6:palette[owners[index]];
        }
        // A small complete frame is immediately available; never clear the map while a job runs.
        preview=render(256,65_536);
    }

    void start(View view){
        if(full!=null||task!=null)return;
        final int request=++generation;
        task=WORKER.submit(()->{
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Bitmap[] result=render(MAX_SIDE,MAX_PIXELS);
            if(result==null)return;
            view.post(()->{
                if(request!=generation){recycleUnpublished(result);return;}
                full=result;task=null;view.postInvalidateOnAnimation();
            });
        });
    }
    void cancel(){generation++;if(task!=null){task.cancel(true);task=null;}}
    boolean ready(){return full!=null;}
    long bytes(){long n=0;for(Bitmap b:preview)n+=b.getAllocationByteCount();if(full!=null)for(Bitmap b:full)n+=b.getAllocationByteCount();return n;}
    void draw(Canvas canvas,int mode){canvas.drawBitmap((full==null?preview:full)[mode],null,bounds,paint);}

    private Bitmap[] render(int side,int pixelLimit){
        float scale=Math.min(side/Math.max(bounds.width(),bounds.height()),
            (float)Math.sqrt(pixelLimit/(bounds.width()*bounds.height())));
        int w=Math.max(1,(int)(bounds.width()*scale)),h=Math.max(1,(int)(bounds.height()*scale));
        int[] cells=new int[w*h],pixels=new int[w*h],ground=new int[w*h];
        float stepX=bounds.width()/w,stepY=bounds.height()/h;
        for(int y=0;y<h;y++){
            if(Thread.currentThread().isInterrupted())return null;
            float wy=bounds.top+(y+.5f)*stepY;
            for(int x=0;x<w;x++){
                float wx=bounds.left+(x+.5f)*stepX;
                float r=(columnStaggered?wx:wy)/DY;
                int ir=TileGeometry.projectedRow(wx,wy,columnStaggered),iq=TileGeometry.projectedColumn(wx,wy,ir,offset,columnStaggered);
                float q=(columnStaggered?wy:wx)/DX-r*.5f+offset;
                int cell=iq>=0&&iq<width&&ir>=0&&ir<height?ir*width+iq:-1;
                cells[y*w+x]=cell;
                int color=cell<0||terrain[cell]==0?BACKGROUND:terrain[cell];
                if(cell>=0&&styles[cell]>=0&&artwork!=null){
                    int tx=Math.max(0,Math.min(63,(int)((((q-iq)+(r-ir)*.5f)*DX+RADIUS)*64/50)));
                    int ty=Math.max(0,Math.min(63,(int)(((r-ir)*DY+RADIUS)*64/50)));
                    if(columnStaggered){int temp=tx;tx=ty;ty=temp;}
                    color=blend(color,artwork[styles[cell]][ty*64+tx],96);
                }
                ground[y*w+x]=color;
            }
        }
        Bitmap[] images=new Bitmap[3];
        for(int mode=0;mode<3;mode++){
            for(int y=0;y<h;y++){
                if(Thread.currentThread().isInterrupted()){recycleUnpublished(images);return null;}
                for(int x=0;x<w;x++){
                    int i=y*w+x,cell=cells[i];
                    int color=ground[i];
                    if(mode>0&&cell>=0&&sites[cell]>=0){
                        color=blend(color,colors[cell],mode==1?158:138+(sites[cell]%3)*10);
                        if(x>0&&boundary(cell,cells[i-1],mode)||y>0&&boundary(cell,cells[i-w],mode))
                            color=blend(color,0xffe3ebcf,191);
                    }
                    pixels[i]=color;
                }
            }
            images[mode]=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            images[mode].setPixels(pixels,0,w,0,0,w,h);
            Canvas canvas=new Canvas(images[mode]);canvas.scale(w/bounds.width(),h/bounds.height());canvas.translate(-bounds.left,-bounds.top);
            Paint road=new Paint(Paint.ANTI_ALIAS_FLAG);road.setColor(0xffa18f73);road.setStrokeCap(Paint.Cap.ROUND);road.setStrokeWidth(Math.max(2.5f,.55f/scale));
            for(int r=0;r<height;r++)for(int q=0;q<width;q++)if(roads[r*width+q]!=0){
                float cx=TileGeometry.projectedX(q,r,offset,columnStaggered),cy=TileGeometry.projectedY(q,r,offset,columnStaggered);
                int mask=roads[r*width+q]&63;
                for(int d=0;d<6;d++)if((mask&(1<<d))!=0)canvas.drawLine(cx,cy,cx+(columnStaggered?TerrainConnections.edgeY(d):TerrainConnections.edgeX(d)),cy+(columnStaggered?TerrainConnections.edgeX(d):TerrainConnections.edgeY(d)),road);
            }
        }
        return images;
    }
    private boolean boundary(int a,int b,int mode){
        return b<0||sites[b]!=sites[a]&&(mode==2||sites[b]<0||owners[a]<0||owners[a]!=owners[b]);
    }
    private static int blend(int base,int color,int alpha){
        int inverse=255-alpha;
        return 0xff000000|(((base>>16&255)*inverse+(color>>16&255)*alpha)/255)<<16
            |(((base>>8&255)*inverse+(color>>8&255)*alpha)/255)<<8
            |((base&255)*inverse+(color&255)*alpha)/255;
    }
    private static void recycleUnpublished(Bitmap[] bitmaps){for(Bitmap bitmap:bitmaps)if(bitmap!=null)bitmap.recycle();}
}
