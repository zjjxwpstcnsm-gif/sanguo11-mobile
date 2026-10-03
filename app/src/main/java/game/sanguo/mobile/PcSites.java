package game.sanguo.mobile;

import game.sanguo.core.Hex;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Detached original site geometry and placements; never owns gameplay state. */
final class PcSites {
    record Placement(int kind,float x,float z,float y,float yaw,int anchorX,int anchorZ,int climate) {}
    private record Model(int texture,float[] data,int[] indices) {}
    private final Placement[] placements;
    private final int[][] bindings=new int[8][4];
    private final Map<Integer,Integer> cells=new HashMap<>();
    private final Map<Integer,Model> models=new HashMap<>();
    PcSites(InputStream source)throws IOException {
        byte[] bytes;
        try(InputStream raw=source;InputStream in=new GZIPInputStream(raw);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[8192];int n;
            while((n=in.read(block))!=-1){if(out.size()+n>8_000_000)throw new IOException("PC sites budget");out.write(block,0,n);}bytes=out.toByteArray();
        }
        try{
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(b.getLong()!=0x3430305449534350L||b.getInt()!=87||b.getInt()!=80||b.getInt()!=15)throw new IOException("PC sites header");
            for(int[] row:bindings)for(int j=0;j<4;j++){row[j]=b.getInt();if(row[j]<0||row[j]>82)throw new IOException("PC sites binding");}
            for(int i=0;i<15;i++){int texture=b.getInt();if(texture<0||texture>17||cells.put(texture,i)!=null)throw new IOException("PC sites texture");}
            placements=new Placement[87];
            for(int i=0;i<placements.length;i++){
                int kind=b.getShort()&65535,x=b.getShort()&65535,z=b.getShort()&65535,y=b.getShort()&65535,anchorX=b.getShort()&65535,anchorZ=b.getShort()&65535;float yaw=b.getFloat();int climate=b.getShort()&65535;
                if(kind>7||x>512||z>512||y>255||anchorX>=200||anchorZ>=200||climate>5||!Float.isFinite(yaw))throw new IOException("PC sites placement");
                placements[i]=new Placement(kind,x*.5f-28.5f,z*.5f-28.5f,y*game.sanguo.core.PcMap.SCALE,yaw,anchorX,anchorZ,climate);
            }
            for(int i=0;i<80;i++){
                int index=b.getInt(),texture=b.getInt(),nv=b.getInt(),ni=b.getInt();
                if(index!=(i<72?i:i+4)||!cells.containsKey(texture)||nv<1||nv>30000||ni<3||ni>100000||ni%3!=0)throw new IOException("PC sites model");
                float[] d=new float[nv*12];for(int j=0;j<d.length;j++){d[j]=b.getFloat();if(!Float.isFinite(d[j]))throw new IOException("PC sites vertex");}
                int[] ix=new int[ni];for(int j=0;j<ni;j++){ix[j]=b.getInt();if(ix[j]<0||ix[j]>=nv)throw new IOException("PC sites index");}
                models.put(index,new Model(texture,d,ix));
            }
            if(b.hasRemaining())throw new IOException("PC sites trailing bytes");
            for(int k=0;k<8;k++)for(int n:bindings[k])if(!models.containsKey(n)||!models.containsKey(n+1))throw new IOException("PC sites missing model");
        }catch(BufferUnderflowException|IllegalArgumentException e){throw new IOException("PC sites truncated",e);}
    }
    int placementCount(){return placements.length;}
    Placement placement(MapSceneSnapshot.Ground g,MapSceneSnapshot.Item item){
        if(g.pcMap==null||item.site==null)return null;
        for(Placement p:placements){
            if((p.kind<6?0:p.kind==6?2:1)!=item.kind)continue;
            Hex h=g.grid.cell(p.anchorX-g.sourceOriginX,p.anchorZ+(p.anchorX%2)*.5f-g.sourceOriginY);
            if(h.equals(item.hex))return p;
        }
        return null; // Explicit legacy/custom geometry path for relocated/custom sites.
    }
    /** Original41bbb0: cities use their winter sheet; gates/ports retain
     * vegetation in warm provinces, with an alternate original winter sheet. */
    static int textureDelta(int kind,int month,int climate){
        if(kind<0||kind>7||month<1||month>12||climate<0||climate>5)throw new IllegalArgumentException("PC site texture state");
        if(month<10)return 0;
        return kind<6||climate==0||climate==2?1:2;
    }
    String key(Placement p,SiteVisual site,int month,int lod){return "pc-site:"+p.kind+":"+site.sourceBuildings+":"+site.sourceWalls+":"+textureDelta(p.kind,month,p.climate)+":"+(lod==0?0:1);}
    int modelIndex(int kind,int state,int lod){
        if(kind<0||kind>7||state<0||state>3||lod<0||lod>2)throw new IllegalArgumentException("PC site model state");
        return bindings[kind][state]+(lod==0?0:1);
    }
    SceneMesh mesh(Placement p,SiteVisual site,int month,int lod){
        Model body=models.get(modelIndex(p.kind,site.sourceBuildings,lod));
        Model wall=p.kind<6?models.get(modelIndex(p.kind,site.sourceWalls,lod)+6):null;
        Model[] group=wall==null?new Model[]{body}:new Model[]{body,wall};
        int vertices=0,indices=0;for(Model m:group){vertices+=m.data.length/12;indices+=m.indices.length;}
        float[] v=new float[vertices*7],uv=new float[vertices*2],normals=new float[vertices*3];int[] ix=new int[indices];int vertex=0,index=0;float radius=0;
        for(Model m:group){
            int base=vertex,cell=cells.get(m.texture+textureDelta(p.kind,month,p.climate));
            for(int a=0;a<m.data.length;a+=12){
                float[] d=m.data;System.arraycopy(d,a,v,vertex*7,3);System.arraycopy(d,a+8,v,vertex*7+3,4);System.arraycopy(d,a+3,normals,vertex*3,3);
                float u=Math.max(.5f/256,Math.min(1-.5f/256,d[a+6])),t=Math.max(.5f/256,Math.min(1-.5f/256,d[a+7]));
                uv[vertex*2]=((cell%4)*260+2+u*256)/1040;uv[vertex*2+1]=((cell/4)*260+2+t*256)/1040;
                radius=Math.max(radius,(float)Math.hypot(d[a],d[a+2]));vertex++;
            }
            for(int i:m.indices)ix[index++]=i+base;
        }
        SceneMesh result=new SceneMesh(v,ix,0,0,radius+.2f);result.uv=uv;result.setNormals(normals);result.authoredTangentFrame=true;result.pcSite=true;return result;
    }
}
