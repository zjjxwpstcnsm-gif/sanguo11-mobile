package game.sanguo.mobile;

import java.io.*;
import java.nio.*;
import java.util.zip.GZIPInputStream;

/** Bounded original final GPU packets, in the original depth/material order. */
final class PcPresentationTimeline {
    static final int RECORD_BYTES=168;
    final int template,resource,rate;
    final ByteBuffer[] frames;
    final int maximum;
    final int maximumBatches;
    private PcPresentationTimeline(int template,int resource,int rate,ByteBuffer[] frames,int maximum,int maximumBatches){this.template=template;this.resource=resource;this.rate=rate;this.frames=frames;this.maximum=maximum;this.maximumBatches=maximumBatches;}
    static PcPresentationTimeline read(InputStream input,int expected)throws IOException{
        byte[] raw;
        try(InputStream in=new GZIPInputStream(input);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[8192];int n;
            while((n=in.read(block))!=-1){if(out.size()+n>32*1024*1024)throw new IOException("Original presentation extent");out.write(block,0,n);}raw=out.toByteArray();
        }
        try{
            ByteBuffer data=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
            if(data.getLong()!=0x3130305153504350L)throw new IOException("Original presentation header");
            int template=data.getInt(),resource=data.getInt(),rate=data.getInt(),count=data.getInt();
            int expectedResource=template==115?240:template==121?246:template==122?247:-1;
            if(template!=expected||expectedResource<0||resource!=expectedResource||rate!=60||count!=125)throw new IOException("Original presentation template/rate");
            ByteBuffer[] frames=new ByteBuffer[count];int maximum=0,maximumBatches=0;
            for(int f=0;f<count;f++){
                int quads=data.getInt();if(quads<0||quads>4096||quads*RECORD_BYTES>data.remaining())throw new IOException("Original presentation frame bound");
                ByteBuffer frame=data.slice().order(ByteOrder.LITTLE_ENDIAN);frame.limit(quads*RECORD_BYTES);
                for(int i=0;i<quads;i++){
                    int at=i*RECORD_BYTES,image=frame.getInt(at),blend=frame.getInt(at+4);
                    if(image<0||image>=33||(blend!=2&&blend!=6))throw new IOException("Original presentation material");
                    for(int v=0;v<4;v++)for(int scalar:new int[]{0,4,8,16,20})if(!Float.isFinite(frame.getFloat(at+8+v*24+scalar)))throw new IOException("Original presentation vertex");
                    for(int m=0;m<16;m++)if(!Float.isFinite(frame.getFloat(at+104+m*4)))throw new IOException("Original presentation transform");
                }
                frames[f]=frame.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);maximum=Math.max(maximum,quads);data.position(data.position()+quads*RECORD_BYTES);
                int batches=0;for(int first=0;first<quads;first=batchEnd(frames[f],first))batches++;maximumBatches=Math.max(maximumBatches,batches);
            }
            if(data.hasRemaining())throw new IOException("Original presentation trailing data");
            return new PcPresentationTimeline(template,resource,rate,frames,maximum,maximumBatches);
        }catch(BufferUnderflowException|IndexOutOfBoundsException e){throw new IOException("Truncated original presentation",e);}
    }
    ByteBuffer frame(float phase){return frames[Math.min(frames.length-1,Math.max(0,Math.round(phase*(frames.length-1))))];}
    /** Only adjacent original packets with identical texture/blend can share
     * a draw. Vertex/index order remains exactly the original queue order. */
    static int batchEnd(ByteBuffer data,int first){
        int count=data.limit()/RECORD_BYTES;
        if(first<0||first>=count)throw new IllegalArgumentException("Original batch boundary");
        int at=first*RECORD_BYTES,image=data.getInt(at),blend=data.getInt(at+4),end=first+1;
        while(end<count&&data.getInt(end*RECORD_BYTES)==image&&data.getInt(end*RECORD_BYTES+4)==blend)end++;
        return end;
    }
    /** Original row-vector transform followed by the admitted camera's rigid pose.
     * Moving/rotating the map camera cannot move the screen presentation. */
    static void vertex(ByteBuffer data,int offset,int vertex,float[] camera,float[] output,int target){
        int v=offset+8+vertex*24,m=offset+104;
        float x=data.getFloat(v),y=data.getFloat(v+4),z=data.getFloat(v+8);
        float px=(x*data.getFloat(m)+y*data.getFloat(m+16)+z*data.getFloat(m+32)+data.getFloat(m+48))*(float)PcEffectCoordinates.SCALE;
        float py=(x*data.getFloat(m+4)+y*data.getFloat(m+20)+z*data.getFloat(m+36)+data.getFloat(m+52))*(float)PcEffectCoordinates.SCALE;
        float pz=(x*data.getFloat(m+8)+y*data.getFloat(m+24)+z*data.getFloat(m+40)+data.getFloat(m+56))*(float)PcEffectCoordinates.SCALE;
        for(int row=0;row<3;row++)output[target+row]=px*camera[row]+py*camera[4+row]+pz*camera[8+row]+camera[12+row];
        int color=data.getInt(v+12);
        output[target+3]=((color>>>16)&255)/255f;output[target+4]=((color>>>8)&255)/255f;output[target+5]=(color&255)/255f;output[target+6]=(color>>>24)/255f;
        output[target+7]=data.getFloat(v+16);output[target+8]=data.getFloat(v+20);
    }
}
