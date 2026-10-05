package game.sanguo.mobile;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Immutable source facility geometry. Selection reads detached presentation values. */
final class PcFacilities {
    private record TextureCell(int cell,int width,int height) {}
    private record Model(int texture,float[] data,int[] indices) {}
    private final Map<Integer,int[]> bindings=new HashMap<>();
    private final Map<Integer,TextureCell> cells=new HashMap<>();
    private final Map<Integer,Model> models=new HashMap<>();
    PcFacilities(InputStream source)throws IOException {
        byte[] bytes;
        try(InputStream raw=source;InputStream in=new GZIPInputStream(raw);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[8192];int n;
            while((n=in.read(block))!=-1){if(out.size()+n>16_000_000)throw new IOException("PC facility budget");out.write(block,0,n);}bytes=out.toByteArray();
        }
        try{
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(b.getLong()!=0x3230304341464350L||b.getInt()!=55)throw new IOException("PC facility header");
            int modelCount=b.getInt(),textureCount=b.getInt();
            if(modelCount<230||modelCount>388||textureCount<56||textureCount>64)throw new IOException("PC facility catalog budget");
            for(int i=0;i<55;i++){
                int kind=b.getInt();int[] row=new int[4];
                if(kind<8||!(kind<46||kind>=62&&kind<82)||bindings.containsKey(kind))throw new IOException("PC facility kind");
                for(int j=0;j<4;j++){row[j]=b.getInt();if(row[j]<72||row[j]>386)throw new IOException("PC facility binding");}bindings.put(kind,row);
            }
            for(int i=0;i<textureCount;i++){
                int texture=b.getInt(),width=b.getInt(),height=b.getInt();
                if(texture<0||texture>=88||(width!=128&&width!=256)||(height!=128&&height!=256)||cells.put(texture,new TextureCell(i,width,height))!=null)throw new IOException("PC facility texture");
            }
            for(int i=0;i<modelCount;i++){
                int index=b.getInt(),texture=b.getInt(),nv=b.getInt(),ni=b.getInt();
                if(index<72||index>387||models.containsKey(index)||!cells.containsKey(texture)||nv<1||nv>30000||ni<3||ni>100000||ni%3!=0)throw new IOException("PC facility model");
                float[] d=new float[nv*12];for(int j=0;j<d.length;j++){d[j]=b.getFloat();if(!Float.isFinite(d[j]))throw new IOException("PC facility vertex");}
                int[] ix=new int[ni];for(int j=0;j<ni;j++){ix[j]=b.getInt();if(ix[j]<0||ix[j]>=nv)throw new IOException("PC facility index");}
                models.put(index,new Model(texture,d,ix));
            }
            if(b.hasRemaining())throw new IOException("PC facility trailing bytes");
            for(var entry:bindings.entrySet())for(int state=0;state<4;state++)for(int lod=0;lod<2;lod++){
                Model m=models.get(entry.getValue()[state]+lod);if(m==null)throw new IOException("PC facility missing model");
                for(int quarter=0;quarter<4;quarter++)for(int climate=0;climate<6;climate++)if(!cells.containsKey(m.texture+textureOffset(entry.getKey(),state,quarter,climate,entry.getValue()[state])))throw new IOException("PC facility missing seasonal sheet");
            }
        }catch(BufferUnderflowException|IllegalArgumentException e){throw new IOException("PC facility truncated",e);}
    }
    /** Source labels and EXE facility→object switch are pinned in facility-bindings.json. */
    static int kind(MapSceneSnapshot.FacilityState f){
        if(f==null)return -1;
        return switch(f.type){
            case "domestic/MARKET"->f.level==1?23:f.level==2?72:77;
            case "domestic/FARM"->f.level==1?24:f.level==2?73:78;
            case "domestic/BARRACKS"->f.level==1?25:f.level==2?74:79;
            case "domestic/SMITH"->f.level==1?26:f.level==2?75:80;
            case "domestic/STABLE"->f.level==1?32:f.level==2?76:81;
            case "domestic/MINT"->36;case "domestic/GRANARY"->27;
            case "domestic/BLACK_MARKET"->70;case "domestic/WORKSHOP"->33;
            case "domestic/SHIPYARD"->34;case "domestic/BRONZE_TERRACE"->19;
            case "military/CAMP"->10;case "military/FORT"->11;case "military/FORTRESS"->12;
            case "military/ARROW_TOWER"->43;case "military/CROSSBOW_TOWER"->35;
            case "military/CATAPULT_TOWER"->44;case "military/MUSIC"->38;case "military/DRUM"->37;
            case "military/STONE_MAZE"->13;case "military/EARTH_WALL"->17;case "military/STONE_WALL"->28;
            case "military/FIRE_SEED"->29;case "military/FLAME_SEED"->30;case "military/INFERNO_SEED"->42;
            case "military/FIRE_BALL"->39;case "military/FLAME_BALL"->40;case "military/INFERNO_BALL"->41;
            case "military/FIRE_SHIP"->31;case "military/DAM"->20;
            default->-1;
        };
    }
    static int state(MapSceneSnapshot.FacilityState f){
        // Existing upgrades remain operational and keep their prior level until
        // the authoritative completion changes it. Do not invent HP progression.
        if(!f.complete&&f.upgradeTo==0)return f.type.startsWith("domestic/")||f.type.equals("military/DAM")||f.hp>=f.maxHp*.3f?2:3;
        return f.hp<Math.min(f.maxHp/2,500)?1:0;
    }
    static int quarter(int month){if(month<1||month>12)throw new IllegalArgumentException("month");return (month-1)/3;}
    static int textureOffset(int kind,int state,int quarter,int climate,int model){
        if(kind==71&&state<2||(kind==73||kind==78)&&state<1)return quarter;
        if((kind==74||kind==79||kind==76||kind==81)&&state<1)return quarter==3?2:quarter==2?1:0;
        if(kind==24&&state<2)return quarter==3?4:quarter;
        return quarter!=3?0:climate==0||climate==2||model==72?1:2;
    }
    boolean supports(MapSceneSnapshot.Ground ground,MapSceneSnapshot.Item item){return ground.pcMap!=null&&bindings.containsKey(kind(item.facility));}
    String key(MapSceneSnapshot.Item item,int month,int lod){return "pc-facility:"+kind(item.facility)+":"+state(item.facility)+":"+quarter(month)+":"+(lod==0?0:1);}
    SceneMesh mesh(MapSceneSnapshot.Item item,int month,int lod){
        return mesh(kind(item.facility),state(item.facility),quarter(month),lod==0?0:1,0);
    }
    /** Shared original geometry for immutable map objects; no authority is created. */
    SceneMesh mesh(int kind,int state,int quarter,int lod,int climate){
        if(!bindings.containsKey(kind)||state<0||state>3||quarter<0||quarter>3||lod<0||lod>1||climate<0||climate>5)throw new IllegalArgumentException("PC object variant");
        return meshIndex(kind,state,bindings.get(kind)[state],quarter,lod,climate);
    }
    /** Original skin UVs use the same source seasonal sheet as its static body. */
    void rigTexture(SceneMesh mesh,int texture){
        TextureCell cell=cells.get(texture);if(cell==null)throw new IllegalArgumentException("PC rig original sheet");
        for(int i=0;i<mesh.uv.length;i+=2){
            float u=Math.max(.5f/cell.width,Math.min(1-.5f/cell.width,mesh.uv[i]));
            float v=Math.max(.5f/cell.height,Math.min(1-.5f/cell.height,mesh.uv[i+1]));
            mesh.uv[i]=((cell.cell%8)*260+2+u*cell.width)/2080;
            mesh.uv[i+1]=((cell.cell/8)*260+2+v*cell.height)/2080;
        }
    }
    /** EXE connected-wall model selection is independent of the generic table. */
    SceneMesh meshIndex(int kind,int state,int near,int quarter,int lod,int climate){
        if(!bindings.containsKey(kind)||state<0||state>3||quarter<0||quarter>3||lod<0||lod>1||climate<0||climate>5||!models.containsKey(near+lod))throw new IllegalArgumentException("PC original model reference");
        Model m=models.get(near+lod);
        // Climate 0 is explicit provisional region selection. Other source
        // winter sheets are converted and validated for subsequent calibration.
        TextureCell cell=cells.get(m.texture+textureOffset(kind,state,quarter,climate,near));
        int count=m.data.length/12;float[] v=new float[count*7],uv=new float[count*2],normals=new float[count*3];float radius=0;
        for(int vertex=0;vertex<count;vertex++){
            int a=vertex*12;float[] d=m.data;
            System.arraycopy(d,a,v,vertex*7,3);System.arraycopy(d,a+8,v,vertex*7+3,4);System.arraycopy(d,a+3,normals,vertex*3,3);
            float u=Math.max(.5f/cell.width,Math.min(1-.5f/cell.width,d[a+6])),t=Math.max(.5f/cell.height,Math.min(1-.5f/cell.height,d[a+7]));
            uv[vertex*2]=((cell.cell%8)*260+2+u*cell.width)/2080;uv[vertex*2+1]=((cell.cell/8)*260+2+t*cell.height)/2080;
            radius=Math.max(radius,(float)Math.hypot(d[a],d[a+2]));
        }
        SceneMesh result=new SceneMesh(v,m.indices.clone(),0,0,radius+.2f);result.uv=uv;result.setNormals(normals);result.authoredTangentFrame=true;result.pcFacility=true;return result;
    }
}
