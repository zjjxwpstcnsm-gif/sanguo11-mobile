package game.sanguo.mobile;

import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Independent native record/quad evidence, not merely a parser roundtrip. */
final class PcWaterRestorationTest {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static int checkMesh(SceneMesh mesh,MapSceneSnapshot.Ground ground,Set<Integer> seen){
        check(mesh.pcWater&&mesh.uv==null&&mesh.surfaceData!=null,"source water owns independent stream");
        check(mesh.vertices.length%28==0&&mesh.surfaceData.length==mesh.vertices.length/7*8,"four vertices per native quad");
        int quads=mesh.vertices.length/28;
        check(mesh.indices.length==quads*6,"source triangle-strip equivalent index count");
        for(int quad=0;quad<quads;quad++){
            int first=quad*4,offset=first*7;
            float x=mesh.vertices[offset],z=mesh.vertices[offset+2];
            int sourceX=Math.round(x+ground.sourceOriginX+28.5f),sourceY=Math.round(z+ground.sourceOriginY+28.5f);
            int key=sourceX*256+sourceY;
            if(seen!=null)check(seen.add(key),"coarse quad appears in exactly one terrain owner: "+key);
            PcMap map=ground.pcMap;
            check(map.coarseWaterByte(sourceX,sourceY)>0,"only original nonzero water records render");
            int mask=map.coarseWaterMask(sourceX,sourceY),sheet=map.coarseWaterSheet(sourceX,sourceY);
            for(int corner=0;corner<4;corner++){
                int v=(first+corner)*7,s=(first+corner)*8;
                check(mesh.vertices[v]==x+(corner&1)&&mesh.vertices[v+2]==z+(corner>>1),"native20-unit corner spacing and order");
                check(Float.floatToRawIntBits(mesh.vertices[v+1])==Float.floatToRawIntBits(map.coarseWaterPlane(sourceX,sourceY)),"exact biased source water plane");
                check(mesh.vertices[v+3]==1&&mesh.vertices[v+4]==1&&mesh.vertices[v+5]==1,"original diffuse RGB white");
                check(mesh.vertices[v+6]==((mask&(1<<corner))!=0?1f:32/255f),"original wet/dry corner alpha255/32");
                check(mesh.surfaceData[s]==(corner&1)+2*sheet&&mesh.surfaceData[s+1]==(corner>>1),"local UV and original texture channel");
                check(mesh.surfaceData[s+6]==map.coarseWaterPhase(sourceX,sourceY)&&mesh.surfaceData[s+7]==map.coarseWaterPeriod(sourceX,sourceY),"native independent period and phase");
            }
            int[] strip={first,first+1,first+2,first+2,first+1,first+3};
            for(int i=0;i<6;i++)check(mesh.indices[quad*6+i]==strip[i],"native strip topology");
        }
        return quads;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Provide independent PCWCO001 native reference");
        PcMap map=PcMap.get();int wet=0;
        try(DataInputStream in=new DataInputStream(new FileInputStream(args[0]))){
            check(in.readLong()==0x504357434f303031L,"native reference header");
            for(int x=0;x<256;x++)for(int y=0;y<256;y++){
                byte[] record=new byte[10];in.readFully(record);
                int phase=(record[0]&255)|((record[1]&255)<<8),period=(record[2]&255)|((record[3]&255)<<8);
                check(map.coarseWaterByte(x,y)==(record[7]&255)&&map.coarseWaterMask(x,y)==(record[8]&255)
                    &&map.coarseWaterSheet(x,y)==(record[9]&3)&&map.coarseWaterPhase(x,y)==phase
                    &&map.coarseWaterPeriod(x,y)==period,"independent native coarse initialization "+x+","+y);
                if((record[7]&255)>0)wet++;
            }
            check(in.read()==-1&&wet==20076,"complete native reference with all wet records");
        }
        World world=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(world);
        MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(world);
        Set<Integer> seen=new HashSet<>();int quads=0;
        for(SceneMesh mesh:SceneMesh.ground(ground,Collections.emptyList())){
            check(mesh.landIndexCount==mesh.indices.length,"source owner is land-only");
            if(mesh.sourceWater!=null)quads+=checkMesh(mesh.sourceWater,ground,seen);
        }
        SceneMesh background=SceneMesh.backdrop(ground);
        if(background.sourceWater!=null)quads+=checkMesh(background.sourceWater,ground,seen);
        int required=0,filledFineZero=0;
        for(int x=0;x<256;x++)for(int y=0;y<256;y++)if(map.coarseWaterByte(x,y)>0){
            Hex owner=ground.grid.cell(x-28f-ground.sourceOriginX,y-28f-ground.sourceOriginY);
            if(ground.valid(owner)){
                required++;check(seen.contains(x*256+y),"no missing coarse quad within national rectangle: source="+x+","+y+" owner="+owner+" valid="+ground.valid(owner));
                for(int dx=0;dx<4;dx++)for(int dy=0;dy<4;dy++){
                    float sx=x-28.5f+(dx+.5f)*.25f,sz=y-28.5f+(dy+.5f)*.25f;
                    if(map.waterHeight(sx,sz)==0){filledFineZero++;check(map.coarseWaterHeight(sx,sz)>0,"native coarse coverage fills formerly empty fine faces");}
                }
            }
        }
        check(required>10000&&filledFineZero>10000,"national source coastline exercises repaired holes");
        check(Arrays.equals(before,SaveCodec.encode(world)),"source water never changes authority/RNG/save bytes");
        System.out.println("PASS native coarse water "+checks+" checks; uploadedQuads="+quads+" nationalRequired="+required+" formerFineHoles="+filledFineZero);
    }
}
