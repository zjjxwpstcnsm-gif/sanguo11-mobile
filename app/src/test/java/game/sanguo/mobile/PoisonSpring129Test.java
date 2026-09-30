package game.sanguo.mobile;

import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

/** All real POISON cells plus edited maps: the mask has no authority or RNG access. */
public final class PoisonSpring129Test {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static float legacyTone(MapSceneSnapshot.Ground g,float x,float z){
        float tone=.96f+.04f*(float)(Math.sin(x*.19)*Math.cos(z*.17));
        Hex h=g.grid.cell(x,z);float wet=0;
        for(int r=h.r-1;r<=h.r+1;r++)for(int q=h.q-1;q<=h.q+1;q++){
            Hex n=new Hex(q,r);if(!g.valid(n)||g.isBase(q,r)||g.terrain[r*g.width+q]!=World.Terrain.SWAMP.ordinal())continue;
            float dx=x-g.grid.x(n),dz=z-g.grid.z(n),t=Math.max(0,1-(dx*dx+dz*dz)/.64f);
            wet=Math.max(wet,t*t*(3-2*t));
        }
        return tone*(1-.22f*wet);
    }
    static float decode(float value){float t=Math.max(0,Math.min(1,(value-1.02f)/.20f));return t*t*(3-2*t);}
    static void auditCenters(World w,int expected){
        byte[] before;
        try{before=SaveCodec.encode(w);}catch(Exception e){throw new RuntimeException(e);}
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);TerrainMaterialField f=new TerrainMaterialField(g);
        int poison=0;
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);if(!g.valid(h))continue;
            float x=g.grid.x(h),z=g.grid.z(h),mask=f.poisonMask(x,z);
            boolean spring=g.terrain[r*g.width+q]==World.Terrain.POISON.ordinal();
            check(mask==(spring?1f:0f),"mask follows exact real terrain membership "+q+","+r);
            if(spring){
                poison++;check(decode(f.groundTone(x,z))==1f,"all poison centres have full shader identity");
                check(!g.surface.water(h),"poison stays terrestrial, never creates naval water");
                for(float dx:new float[]{-.5f,-.25f,0,.25f,.5f})for(float dz:new float[]{-.5f,-.25f,0,.25f,.5f}){
                    float px=x+dx,pz=z+dz,m=f.poisonMask(px,pz);
                    check(Float.isFinite(m)&&m>=0&&m<=1,"bounded spring support");
                    if(Math.abs(dx)==.5f||Math.abs(dz)==.5f)check(m==0,"zero shared-edge payload protects safe cells and grid");
                    if(m==0)check(f.groundTone(px,pz)==legacyTone(g,px,pz),"zero support preserves old tone exactly");
                }
            }else{
                check(f.groundTone(x,z)==legacyTone(g,x,z),"all non-poison tones remain bit-identical");
                check(decode(f.groundTone(x,z))==0,"all non-poison terrain remains unmarked");
            }
        }
        check(poison==expected,"authoritative POISON count "+poison+" expected "+expected);
        try{check(Arrays.equals(before,SaveCodec.encode(w)),"full saved authority and RNG unchanged by every-cell field audit");}catch(Exception e){throw new RuntimeException(e);}
        System.out.println("MAP "+w.scenarioId+" / "+w.mapId+" poison="+poison+" valid-identity-audit=PASS");
    }
    static void lods(World w)throws Exception{
        var g=new MapSceneSnapshot.Ground(w);float minX=Float.POSITIVE_INFINITY,maxX=-minX,minZ=minX,maxZ=-minX;
        Set<Hex> expected=new HashSet<>();for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++)if(g.terrain[r*g.width+q]==World.Terrain.POISON.ordinal()){
            Hex h=new Hex(q,r);expected.add(h);minX=Math.min(minX,g.grid.x(h));maxX=Math.max(maxX,g.grid.x(h));minZ=Math.min(minZ,g.grid.z(h));maxZ=Math.max(maxZ,g.grid.z(h));
        }
        byte[] before=SaveCodec.encode(w);
        SceneMesh backdrop=SceneMesh.backdrop(g);
        for(int i=0;i<backdrop.vertices.length/7;i++){
            float bx=backdrop.vertices[i*7],bz=backdrop.vertices[i*7+2];
            check(backdrop.surfaceData[i*8+7]==legacyTone(g,bx,bz),"coarse backdrop retains exact old tone without interpolated hazard ghosts");
        }
        for(float span:new float[]{8,20,55}){
            var window=new SceneMesh.TerrainWindow((minX+maxX)/2,(minZ+maxZ)/2,(maxX-minX)/2,(maxZ-minZ)/2,span);
            List<SceneMesh> meshes=SceneMesh.ground(g,List.of(),window);Set<Hex> found=new HashSet<>();int marked=0;
            for(var m:meshes){
                check(m.surfaceData.length==m.vertices.length/7*8,"existing 32-byte auxiliary stream retained");
                boolean[] land=new boolean[m.vertices.length/7];for(int j=0;j<m.landIndexCount;j++)land[m.indices[j]]=true;
                for(int i=0;i<m.vertices.length/7;i++){
                    if(!land[i])continue; // Water's existing UV1.y flow angle is a different material contract.
                    float x=m.surfaceData[i*8],z=-m.surfaceData[i*8+1];Hex h=g.grid.cell(x,z);
                    if(decode(m.surfaceData[i*8+7])>.99f&&g.valid(h)){
                        marked++;check(g.terrain[h.r*g.width+h.q]==World.Terrain.POISON.ordinal(),"strong spring signal never leaves real poison at LOD "+span);
                        if(x==g.grid.x(h)&&z==g.grid.z(h))found.add(h);
                    }
                    // Exact horizontal payload/geometry contract is unaffected.
                    check(Float.isFinite(m.surfaceData[i*8+7]),"finite material metadata");
                }
            }
            check(found.equals(expected),"all 179 actual poison centres survive near/mid/far mesh production");
            check(meshes.equals(SceneMesh.ground(g,meshes,window)),"same bounded window reuses all immutable terrain meshes");
            System.out.println("LOD span="+span+" chunks="+meshes.size()+" centres="+found.size()+" strong-vertices="+marked);
        }
        check(Arrays.equals(before,SaveCodec.encode(w)),"all production LOD builds preserve complete save and rule RNG");
    }
    static void edits(boolean staggered)throws Exception{
        World w=new World(96,64,"one","two");w.columnStaggered=staggered;w.mapId="poison-edited-map";w.customMapId="00000000-0000-0000-0000-000000000129";w.customMapName="Poison spring visual fixture";w.customMapRevision=1;w.customMapBase="0".repeat(64);w.customMapFingerprint="1".repeat(64);
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=World.Terrain.PLAIN;
        w.cities.add(new World.City(1,"visual fixture city",new Hex(2,2),0));
        w.terrain[20][20]=World.Terrain.POISON;w.terrain[40][20]=World.Terrain.SWAMP;w.terrain[20][21]=World.Terrain.WATER;
        var g=new MapSceneSnapshot.Ground(w);var f=new TerrainMaterialField(g);auditCenters(w,1);
        check(f.poisonMask(Float.NaN,0)==0&&f.poisonMask(0,Float.POSITIVE_INFINITY)==0,"invalid visual coordinates are safe");
        float x=g.grid.x(32,24),z=g.grid.z(32,24);var window=new SceneMesh.TerrainWindow(x,z,200,200,55);
        List<SceneMesh> before=SceneMesh.ground(g,List.of(),window);byte[] save=SaveCodec.encode(w);
        var restored=SaveCodec.decode(save);auditCenters(restored,1);
        w.terrain[20][20]=World.Terrain.PLAIN;w.terrain[21][20]=World.Terrain.POISON;w.mapRevision++;
        check(!g.matches(w),"edited terrain invalidates immutable snapshot");
        var next=new MapSceneSnapshot.Ground(w);var nf=new TerrainMaterialField(next);
        check(nf.poisonMask(next.grid.x(20,20),next.grid.z(20,20))==0,"erasing POISON clears old spring");
        check(nf.poisonMask(next.grid.x(21,20),next.grid.z(21,20))==1,"painting POISON marks custom map immediately");
        var after=SceneMesh.ground(next,before,window);int changed=0,reused=0;
        Map<String,SceneMesh> old=new HashMap<>();for(var m:before)old.put(m.chunkQ+":"+m.chunkR,m);
        for(var m:after){var prior=old.get(m.chunkQ+":"+m.chunkR);if(m==prior)reused++;else{changed++;check(20>=m.chunkQ-8&&20<m.chunkQ+24&&20>=m.chunkR-8&&20<m.chunkR+24,"only existing eight-cell fingerprint halo invalidates");}}
        check(changed>0&&changed<=4&&reused>0,"edit is bounded and distant chunks remain cached");
        System.out.println("CUSTOM staggered="+staggered+" changed="+changed+" reused="+reused);
        w.terrain[2][2]=World.Terrain.POISON;
        var onSite=new MapSceneSnapshot.Ground(w);
        check(new TerrainMaterialField(onSite).poisonMask(onSite.grid.x(2,2),onSite.grid.z(2,2))==1,"custom POISON remains authoritative even beneath an existing site");
        boolean rejected=false;try{SaveCodec.encode(w);}catch(java.io.IOException expected){rejected=true;}
        check(rejected,"renderer does not relax authoritative prohibition on poisoned city footprints");
        w.terrain[2][2]=World.Terrain.PLAIN;
    }
    static float[] shaderCentre(GridWorldTransform grid,float x,float z){
        // Literal float2 poisonGrid fragment formula, compared against production layout.
        float row=(float)Math.floor((grid.staggered?x:z)+.5f);
        float column=(float)Math.floor((grid.staggered?z:x)-row*.5f+grid.offset+.5f);
        return grid.staggered?new float[]{row,column+row*.5f-grid.offset}:new float[]{column+row*.5f-grid.offset,row};
    }
    static void shaderLayout(){
        for(boolean staggered:new boolean[]{false,true})for(float offset:new float[]{0,99,19.5f}){
            var grid=new GridWorldTransform(offset,staggered);
            for(int r=-2;r<=60;r+=3)for(int q=-2;q<=110;q+=7)for(float dx:new float[]{-.50001f,-.5f,-.49f,0,.49f,.5f,.50001f})for(float dz:new float[]{-.50001f,-.5f,-.49f,0,.49f,.5f,.50001f}){
                float x=grid.x(q,r)+dx,z=grid.z(q,r)+dz;Hex h=grid.cell(x,z);float[] centre=shaderCentre(grid,x,z);
                check(centre[0]==grid.x(h)&&centre[1]==grid.z(h),"shader centre exactly follows layout, boundary/corner and offset");
            }
        }
        for(int c=0;c<200;c++)for(int angle=0;angle<360;angle++){
            double a=angle*Math.PI/180,seed=c*.77+(173+c*.5)*.55;
            double radius=.32+.045*Math.sin(a*3+seed)+.025*Math.sin(a*5-seed*1.7);
            check(radius>=.25-1e-7&&radius+.045<.445,"every angular mineral rim remains strictly inside original cell");
        }
    }
    public static void main(String[] args)throws Exception{
        List<World> scenarios=ScenarioCatalog.all();for(World w:scenarios)auditCenters(w,Map.of("central-mobile-sandbox",0,"jingxiang-mobile-sandbox",74).getOrDefault(w.scenarioId,179));
        lods(scenarios.get(0));edits(false);edits(true);shaderLayout();
        String near=Files.readString(Path.of("tools/3d/ground129.mat")),far=Files.readString(Path.of("tools/3d/ground-overview129.mat"));
        String begin="        // POISON is carried",end="        prepareMaterial(material);";
        check(near.substring(near.indexOf(begin),near.indexOf(end)).equals(far.substring(far.indexOf(begin),far.indexOf(end))),"near/overview execute exactly the same pigment and mask decoder");
        check(near.contains("flipUV : false")&&far.contains("flipUV : false"),"material metadata cannot undergo UV flipping");
        check(!near.contains("emissive")||near.contains("never emissive"),"no glowing poison emission");
        check(near.split("type : sampler2d",-1).length==9&&far.split("type : sampler2d",-1).length==5,"no added GPU samplers");
        System.out.println("PASS POISON129 checks="+checks+" all real terrain, custom edits, bounded cache, complete saves/RNG, near/mid/overview payloads");
    }
}
