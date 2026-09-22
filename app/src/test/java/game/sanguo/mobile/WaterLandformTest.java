package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.security.MessageDigest;

/** Actual national water topology, batch coverage and adversarial custom edit regressions. */
public final class WaterLandformTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void mesh(MapSceneSnapshot.Ground g,List<SceneMesh> meshes){
        int water=0,land=0;Map<String,Float> flow=new HashMap<>();
        for(SceneMesh fine:meshes){
            SceneMesh m=fine.distant;check(m.landIndexCount>=0&&m.landIndexCount%3==0,"valid land range");
            check(fine.landIndexCount==m.landIndexCount*3,"LOD keeps disjoint material partition");
            for(int t=0;t<m.indices.length;t+=3){
                float x=0,z=0;boolean wet=t>=m.landIndexCount;
                for(int j=0;j<3;j++){int i=m.indices[t+j];x+=m.vertices[i*7];z+=m.vertices[i*7+2];
                    if(wet){check(m.vertices[i*7+1]==0,"water at exact pick plane");
                        String key=m.vertices[i*7]+":"+m.vertices[i*7+2];Float old=flow.put(key,m.surfaceData[i*8+7]);
                        if(old!=null)check(old==m.surfaceData[i*8+7],"flow identical on shared water edges");}}
                Hex h=g.grid.cell(x/3,z/3);check(g.valid(h)&&g.surface.water(h)==wet,"disjoint batch owns original mask, no overlap or dropped cell");
                if(wet)water++;else land++;
            }
        }
        int wc=0,lc=0;WaterVisualField field=new WaterVisualField(g);
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);if(!g.valid(h))continue;boolean wet=g.surface.water(h);if(wet)wc++;else lc++;
            float x=g.grid.x(h),z=g.grid.z(h),d=field.distance(x,z);
            check(wet?d>0:d<0,"all centers preserve water/land semantics including single cells");
            if(wet){check(Float.isFinite(field.flowAngle(x,z)),"finite flow");
                check(Math.abs(field.flowAngle(x,z)-field.flowAngle(x+.00001f,z))<.001,"continuous flow");}
        }
        check(water==wc*8&&land==lc*8,"exact valid-region area coverage");
    }
    static List<Integer> components(MapSceneSnapshot.Ground g){
        Set<Hex> seen=new HashSet<>();List<Integer> sizes=new ArrayList<>();
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);if(!g.surface.water(h)||!seen.add(h))continue;
            int n=0;ArrayDeque<Hex> pending=new ArrayDeque<>();pending.add(h);
            while(!pending.isEmpty()){Hex a=pending.remove();n++;for(Hex b:a.neighbors())if(g.surface.water(b)&&seen.add(b))pending.add(b);}
            sizes.add(n);
        }Collections.sort(sizes);return sizes;
    }
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.all().get(0);byte[] save=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        List<Integer> topology=components(g);List<SceneMesh> meshes=SceneMesh.ground(g);mesh(g,meshes);
        check(topology.equals(components(new MapSceneSnapshot.Ground(w))),"water components unchanged");
        int ports=0;for(World.City c:w.cities)if(c.kind==World.SiteKind.PORT){ports++;int contacts=0;for(Hex n:c.hex.neighbors())if(g.surface.water(n)){contacts++;check(g.surface.at(n)==0,"port connection surface fixed");}check(contacts>0,"national port retains water contact "+c.name);}
        check(Arrays.equals(save,SaveCodec.encode(w)),"full core save unchanged by landform/water/flow");
        System.out.println("national terrain SHA256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(g.terrain))+" water components="+topology+" ports="+ports);
        for(boolean staggered:new boolean[]{false,true})for(int offset:new int[]{0,1}){
            World custom=new World(48,40);custom.columnStaggered=staggered;custom.sourceMapWidth=offset==0?0:48;
            for(int q=0;q<48;q++)for(int r=0;r<40;r++)custom.terrain[q][r]=q==16||r==17?World.Terrain.WATER:World.Terrain.MOUNTAIN;
            custom.terrain[16][17]=World.Terrain.PLAIN; // tiny island splits the crossing; never blur it away
            custom.terrain[17][17]=World.Terrain.NON_NAVIGABLE_WATER;
            MapSceneSnapshot.Ground a=new MapSceneSnapshot.Ground(custom);List<SceneMesh> old=SceneMesh.ground(a);mesh(a,old);
            Hex painted=new Hex(30,30);TerrainSurface surface=new TerrainSurface(a,Map.of(painted,2.4f));
            check(Math.abs(surface.at(painted)-2.4)<1e-5,"explicit legal height center wins over defaults");
            custom.terrain[16][16]=World.Terrain.PLAIN;custom.terrain[15][16]=World.Terrain.WATER;
            MapSceneSnapshot.Ground edit=new MapSceneSnapshot.Ground(custom);List<SceneMesh> cached=SceneMesh.ground(edit,old),fresh=SceneMesh.ground(edit);
            int reused=0;for(int i=0;i<fresh.size();i++){SceneMesh x=cached.get(i),y=fresh.get(i);
                check(Arrays.equals(x.vertices,y.vertices)&&Arrays.equals(x.surfaceData,y.surfaceData)&&Arrays.equals(x.indices,y.indices)&&x.landIndexCount==y.landIndexCount,"cross-chunk land/water edit equals complete rebuild");if(old.contains(x))reused++;}
            check(reused>0&&reused<old.size(),"bounded edit invalidation");mesh(edit,cached);
            custom.terrain[16][16]=World.Terrain.WATER;custom.terrain[15][16]=World.Terrain.MOUNTAIN;
            List<SceneMesh> undone=SceneMesh.ground(new MapSceneSnapshot.Ground(custom),cached);
            for(int i=0;i<old.size();i++)check(Arrays.equals(old.get(i).surfaceData,undone.get(i).surfaceData)&&Arrays.equals(old.get(i).indices,undone.get(i).indices),"undo restores water/shore/flow");
        }
        System.out.println("PASS S11: "+checks+" topology, coverage, flow, LOD, override and edit assertions");
    }
}
