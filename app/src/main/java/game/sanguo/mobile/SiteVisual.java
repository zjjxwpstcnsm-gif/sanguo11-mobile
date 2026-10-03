package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Read-only national and custom-site mapping. Does not participate in movement or combat. */
final class SiteVisual {
    final String model; final float yaw,scale; final int damage,sourceBuildings,sourceWalls; final List<Hex> cells;
    SiteVisual(World w,World.City c,GridWorldTransform g){
        // PC VA0x5a03e0: city interior tracks completed domestic facilities,
        // walls track absolute durability; gates/ports use a capped half-HP threshold.
        int completed=0;
        for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==c.id&&(f.remaining<=0||f.upgradeTo>0))completed++;
        sourceBuildings=c.kind==World.SiteKind.CITY?(completed<=4?1:completed<=8?0:2):(c.defense<Math.min(c.baseDefense/2,500)?1:0);
        sourceWalls=c.defense<500?2:c.defense<1000?1:0;
        // Region is derived from actual terrain, never from arbitrary stable entity IDs.
        int mountain=0,water=0;
        Set<Hex> region=new HashSet<>(SiteFootprint.cells(c));
        for(int ring=0;ring<3;ring++)for(Hex h:new ArrayList<>(region))region.addAll(h.neighbors());
        for(Hex h:region)if(w.inside(h)){
            if(w.terrain[h.q][h.r]==World.Terrain.MOUNTAIN)mountain++;
            if(w.army.water(h))water++;
        }
        int variant=mountain>=2?2:water>=2?1:0;
        MapPatch.Appearance appearance=w.visualMap==null?null:w.visualMap.appearances.get(c.id);if(appearance!=null)variant=appearance.variant();
        model=c.kind==World.SiteKind.PORT?"port":c.kind==World.SiteKind.GATE?"gate":"city"+variant;
        scale=c.kind==World.SiteKind.CITY?.96f:1;
        damage=c.defense*3L<c.baseDefense?2:c.defense*3L<c.baseDefense*2L?1:0;
        cells=Collections.unmodifiableList(new ArrayList<>(SiteFootprint.cells(c)));
        float angle=0;
        if(c.kind==World.SiteKind.PORT){
            // Only navigation-authorized water categories; decorative non-navigable water is excluded.
            for(Hex h:c.hex.neighbors())if(w.army.water(h)){
                angle=(float)Math.atan2(g.x(h)-g.x(c.hex),g.z(h)-g.z(c.hex));break;
            }
        }else if(c.kind==World.SiteKind.GATE){
            // Gateway local +Z follows a real open approach, with an opposite exit
            // preferred. Scoring mountain sums lost diagonal mountain-pass axes.
            float best=-Float.MAX_VALUE;
            for(Hex h:c.hex.neighbors())if(openApproach(w,h)){
                float dx=g.x(h)-g.x(c.hex),dz=g.z(h)-g.z(c.hex),length=(float)Math.hypot(dx,dz);
                float score=0;
                for(Hex other:c.hex.neighbors())if(openApproach(w,other)){
                    float ox=g.x(other)-g.x(c.hex),oz=g.z(other)-g.z(c.hex);
                    float dot=(dx*ox+dz*oz)/(length*(float)Math.hypot(ox,oz));
                    score=Math.max(score,-dot);
                }
                if(score>best){best=score;angle=(float)Math.atan2(dx,dz);}
            }
        }
        yaw=appearance==null?angle:(float)Math.toRadians(appearance.degrees());
    }
    private static boolean openApproach(World w,Hex h){
        if(!w.inside(h))return false;
        World.Terrain t=w.terrain[h.q][h.r];
        return t!=World.Terrain.MOUNTAIN&&t!=World.Terrain.VOID&&!w.army.water(h)
            &&t!=World.Terrain.NON_NAVIGABLE_WATER&&t!=World.Terrain.SHALLOWS;
    }
    static boolean navigable(World.Terrain t){return t==World.Terrain.WATER||t==World.Terrain.SEA;}
    static int lod(float span,int previous){
        if(previous==0)return span>19?1:0;
        if(previous==2)return span<42?1:2;
        return span<15?0:span>48?2:1;
    }
    static SceneMesh fallback(int kind){return SceneMesh.proxy(kind,0xff918a75);}
    /** Short retaining wings terminate in real adjacent mountain cells. Nothing is
     * added across a legal approach, and there is no new collision/terrain rule. */
    static SceneMesh joinGate(SceneMesh source,MapSceneSnapshot.Ground g,Hex gate,float yaw){
        List<Float> v=new ArrayList<>(),uv=new ArrayList<>();List<Integer> indices=new ArrayList<>();
        for(float a:source.vertices)v.add(a);for(float a:source.uv)uv.add(a);for(int a:source.indices)indices.add(a);
        float cs=(float)Math.cos(yaw),sn=(float)Math.sin(yaw),gx=g.grid.x(gate),gz=g.grid.z(gate);
        for(int side:new int[]{-1,1}){
            Hex target=null;float best=0,tx=0,tz=0;
            for(Hex h:gate.neighbors())if(g.valid(h)&&g.terrain[h.r*g.width+h.q]==World.Terrain.MOUNTAIN.ordinal()){
                float dx=g.grid.x(h)-gx,dz=g.grid.z(h)-gz,lx=cs*dx-sn*dz,lz=sn*dx+cs*dz;
                float score=side*lx-Math.abs(lz)*.5f;
                if(score>best&&side*lx>.45f){best=score;target=h;tx=lx;tz=lz;}
            }
            if(target==null)continue;
            float ax=side*.40f,az=0,dx=tx-ax,dz=tz-az,length=(float)Math.hypot(dx,dz),nx=-dz/length*.065f,nz=dx/length*.065f;
            boolean safe=true;
            for(int i=0;i<=8;i++)for(int sign:new int[]{-1,1}){
                float t=i/8f,x=ax+dx*t+nx*sign,z=az+dz*t+nz*sign;
                Hex h=g.grid.cell(gx+cs*x+sn*z,gz-sn*x+cs*z);
                if(!h.equals(gate)&&(!g.valid(h)||g.terrain[h.r*g.width+h.q]!=World.Terrain.MOUNTAIN.ordinal()))safe=false;
            }
            if(!safe)continue;
            for(int i=0;i<6;i++){
                float[][] p=new float[8][3];
                for(int end=0;end<2;end++)for(int edge=0;edge<2;edge++){
                    float t=(i+end)/6f,x=ax+dx*t+nx*(edge==0?-1:1),z=az+dz*t+nz*(edge==0?-1:1);
                    float y=g.surface.meshHeight(gx+cs*x+sn*z,gz-sn*x+cs*z)-.045f;
                    int n=end*2+edge;p[n]=new float[]{x,y,z};p[n+4]=new float[]{x,y+.32f*(1-t*.82f),z};
                }
                gateFace(v,uv,indices,p,0,2,6,4);gateFace(v,uv,indices,p,3,1,5,7);gateFace(v,uv,indices,p,4,6,7,5);
                if(i==0)gateFace(v,uv,indices,p,1,0,4,5);
            }
        }
        SceneMesh mesh=new SceneMesh(v,indices,source.x,source.z,source.radius);
        mesh.uv=new float[uv.size()];for(int i=0;i<uv.size();i++)mesh.uv[i]=uv.get(i);mesh.generateTangents();return mesh;
    }
    private static void gateFace(List<Float> v,List<Float> uv,List<Integer> indices,float[][] points,int... corners){
        int start=v.size()/7;
        for(int i=0;i<4;i++){float[] p=points[corners[i]];Collections.addAll(v,p[0],p[1],p[2],.9f,.9f,.87f,1f);
            Collections.addAll(uv,(i==0||i==3?.03f:.31f),i<2?.08f:.92f);}
        Collections.addAll(indices,start,start+1,start+2,start,start+2,start+3);
    }
}
