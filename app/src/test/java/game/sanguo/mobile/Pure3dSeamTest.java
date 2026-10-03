package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
/** Continuous source scenery, exact shared attributes and immutable authority. */
public final class Pure3dSeamTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        check(SceneCamera.legacySpan(1000,2,1.25f)==5,"legacy density-independent zoom migrates");check(SceneCamera.legacySpan(1000,2,0)==15,"invalid old zoom has explicit native default");check(SceneCamera.legacySpan(1000,2,100)==3,"old zoom respects native close bound");
        World world=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(world);MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(world);
        check(SceneMesh.backdrop(ground)==null,"PC must have one canonical surface without overlapping coarse triangles");
        int totalShared=0,exterior=0;
        for(float[] window:new float[][]{{156,77},{174,88},{199,100},{0,0}}){
            List<SceneMesh> meshes=SceneMesh.ground(ground,Collections.emptyList(),new SceneMesh.TerrainWindow(window[0],window[1],9,9,5));
            Map<Long,float[]> samples=new HashMap<>();int shared=0;
            for(SceneMesh mesh:meshes){
                check(mesh.pcGround&&mesh.landIndexCount==mesh.indices.length,"every source region uses original ground material");
                for(int i=0;i<mesh.vertices.length/7;i++){
                    int v=i*7,s=i*8;float x=mesh.vertices[v],y=mesh.vertices[v+1],z=mesh.vertices[v+2];
                    check(Float.floatToRawIntBits(y)==Float.floatToRawIntBits(ground.surface.pcLandHeight(x,z)),"source land height exact including shore and exterior");
                    if(!ground.valid(ground.grid.cell(x,z)))exterior++;
                    long key=((long)Float.floatToRawIntBits(x)<<32)|(Float.floatToRawIntBits(z)&0xffffffffL);
                    float[] payload=new float[12];System.arraycopy(mesh.vertices,v+1,payload,0,6);System.arraycopy(mesh.surfaceData,s+2,payload,6,6);
                    float[] prior=samples.putIfAbsent(key,payload);if(prior!=null){check(Arrays.equals(prior,payload),"shared chunk/cell height, normal and material attributes are exact");shared++;}
                }
                for(int t=0;t<mesh.indices.length;t+=3){float minX=Float.MAX_VALUE,maxX=-minX,minZ=minX,maxZ=-minX;
                    for(int j=0;j<3;j++){int v=mesh.indices[t+j]*7;minX=Math.min(minX,mesh.vertices[v]);maxX=Math.max(maxX,mesh.vertices[v]);minZ=Math.min(minZ,mesh.vertices[v+2]);maxZ=Math.max(maxZ,mesh.vertices[v+2]);}
                    check(maxX-minX<=.25001f&&maxZ-minZ<=.25001f,"no coarse alternate color/height interpolation near city or coast");
                }
            }
            check(shared>1000,"multiple chunk and cell joins examined in each orientation/edge window");totalShared+=shared;
        }
        check(exterior>1000,"nonplayable source scenery joins the same surface");check(Arrays.equals(before,SaveCodec.encode(world)),"rendering cannot change terrain, passability, RNG or save");
        System.out.println("PASS pure3D seam "+checks+" checks; shared="+totalShared+" exterior="+exterior);
    }
}
