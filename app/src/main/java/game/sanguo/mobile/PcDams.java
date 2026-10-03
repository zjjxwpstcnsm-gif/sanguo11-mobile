package game.sanguo.mobile;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Authored source transforms bound to live saved dam entities, never scenery clones. */
final class PcDams {
    static final class Placement {
        final int slot,sourceX,sourceY;final float x,z,y,yaw;
        Placement(int slot,int rawX,int rawZ,int height,float yaw){this.slot=slot;sourceX=(rawX-57)/2;sourceY=(rawZ-57-(sourceX&1))/2;x=rawX*.5f-28.5f;z=rawZ*.5f-28.5f;y=height*game.sanguo.core.PcMap.SCALE;this.yaw=yaw;}
    }
    private final List<Placement> placements;
    PcDams(InputStream source)throws IOException{
        byte[] bytes;
        try(InputStream raw=source;InputStream in=new GZIPInputStream(raw);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[128];int n;while((n=in.read(block))!=-1){if(out.size()+n>128)throw new IOException("PC dam budget");out.write(block,0,n);}bytes=out.toByteArray();
        }
        try{
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);byte[] header=new byte[8];b.get(header);
            if(!Arrays.equals(header,new byte[]{'P','C','D','A','M','S','0','1'})||b.getInt()!=4)throw new IOException("PC dam header");
            List<Placement> rows=new ArrayList<>();Set<Integer> slots=new HashSet<>();
            for(int i=0;i<4;i++){
                int slot=b.getShort()&65535,x=b.getShort()&65535,z=b.getShort()&65535,reserved=b.getShort()&65535,height=b.get()&255;float yaw=b.getFloat();Placement p=new Placement(slot,x,z,height,yaw);
                if(!slots.add(slot)||reserved!=0||!Float.isFinite(yaw)||p.sourceX<0||p.sourceX>=200||p.sourceY<0||p.sourceY>=200||x!=p.sourceX*2+57||z!=p.sourceY*2+(p.sourceX&1)+57)throw new IOException("PC dam transform");rows.add(p);
            }
            if(b.hasRemaining())throw new IOException("PC dam trailing data");placements=Collections.unmodifiableList(rows);
        }catch(BufferUnderflowException|IllegalArgumentException e){throw new IOException("PC dam truncated",e);}
    }
    List<Placement> placements(){return placements;}
    Placement placement(MapSceneSnapshot.Ground g,MapSceneSnapshot.Item item){
        if(g.pcMap==null||item.facility==null||!item.facility.type.equals("military/DAM"))return null;
        var cell=g.source(item.hex);for(Placement p:placements)if(p.sourceX==cell.x&&p.sourceY==cell.y)return p;return null;
    }
}
