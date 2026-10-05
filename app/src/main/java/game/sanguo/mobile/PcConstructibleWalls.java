package game.sanguo.mobile;

import game.sanguo.core.map.SourceGridCoord;
import java.util.*;

/** Source connected-wall presentation. Owns no core entities, rules or randomness. */
final class PcConstructibleWalls {
    private static final int[][] STEPS={{-4,-2},{0,-4},{4,-2},{-4,2},{0,4},{4,2}};
    // Axial gameplay directions transpose to the original odd-q direction table.
    private static final int[] SOURCE_DIRECTION={4,3,0,1,2,5};
    private static final float[] SOURCE_YAW={-(float)Math.PI/2,(float)Math.PI,(float)Math.PI/2,-(float)Math.PI/2,0,(float)Math.PI/2};
    static final class Placement {
        final int kind,ix,iz;final float x,z,y,yaw;
        Placement(int k,int a,int b,float x,float z,float y,float yaw){kind=k;ix=a;iz=b;this.x=x;this.z=z;this.y=y;this.yaw=yaw;}
    }
    static Placement placement(MapSceneSnapshot.Ground g,MapSceneSnapshot.Item item){
        int kind=PcFacilities.kind(item.facility);if(g.pcMap==null||kind!=17&&kind!=28)return null;
        SourceGridCoord cell=g.source(item.hex);
        // EXE 5a0160 creates rawX=2*q+57, rawZ=2*r+(q&1)+57.
        int ix=cell.x*4+114,iz=cell.y*4+(cell.x&1)*2+114;
        return new Placement(kind,ix,iz,cell.x-g.sourceOriginX,cell.y+(cell.x&1)*.5f-g.sourceOriginY,
            g.pcMap.vertex(ix,iz),SOURCE_YAW[SOURCE_DIRECTION[Math.floorMod(item.facility.direction,6)]]);
    }
    static String key(MapSceneSnapshot.Item item,int month,int lod,Placement p,int mask){
        return "pc-wall:"+p.kind+":"+PcFacilities.state(item.facility)+":"+PcFacilities.quarter(month)+":"+(lod==0?0:1)+":"+p.ix+":"+p.iz+":"+mask+":"+Float.floatToIntBits(p.yaw);
    }
    static SceneMesh mesh(PcFacilities library,MapSceneSnapshot.Ground g,MapSceneSnapshot.Item item,int month,int lod,Placement p,int mask){
        int state=PcFacilities.state(item.facility),quarter=PcFacilities.quarter(month),far=lod==0?0:1;
        if(state!=0)mask=0;
        List<SceneMesh> parts=new ArrayList<>();
        SceneMesh body=mask==0?library.mesh(p.kind,state,quarter,far,0):library.meshIndex(p.kind,0,p.kind==17?136:208,quarter,far,0);
        parts.add(rotate(body,p.yaw));
        for(int k=3;k<6;k++)if((mask&(1<<k))!=0){
            int dx=STEPS[k][0],dz=STEPS[k][1];float rise=(g.pcMap.heightByte(p.ix+dx,p.iz+dz)-g.pcMap.heightByte(p.ix,p.iz))*game.sanguo.core.PcMap.SCALE;
            SceneMesh segment=PcWallGeometry.segment(library.meshIndex(p.kind,0,p.kind==17?130:196,quarter,far,0),dx*.25f,dz*.25f,rise);
            segment.pcCliffWall=false;parts.add(segment);
        }
        return join(parts);
    }
    private static SceneMesh rotate(SceneMesh source,float yaw){
        float[] v=source.vertices.clone(),normal=new float[v.length/7*3];float cs=(float)Math.cos(yaw),sn=(float)Math.sin(yaw);
        for(int j=0;j<v.length/7;j++){
            int a=j*7,t=j*4;float x=v[a],z=v[a+2];v[a]=cs*x+sn*z;v[a+2]=-sn*x+cs*z;
            float[] q=source.tangents;float nx=2*(q[t]*q[t+2]+q[t+3]*q[t+1]),ny=2*(q[t+1]*q[t+2]-q[t+3]*q[t]),nz=1-2*(q[t]*q[t]+q[t+1]*q[t+1]);
            normal[j*3]=cs*nx+sn*nz;normal[j*3+1]=ny;normal[j*3+2]=-sn*nx+cs*nz;
        }
        SceneMesh m=new SceneMesh(v,source.indices.clone(),0,0,source.radius);m.uv=source.uv.clone();m.setNormals(normal);m.pcFacility=true;return m;
    }
    private static SceneMesh join(List<SceneMesh> parts){
        int nv=0,ni=0;for(SceneMesh m:parts){nv+=m.vertices.length/7;ni+=m.indices.length;}
        float[] v=new float[nv*7],uv=new float[nv*2],tangent=new float[nv*4];int[] ix=new int[ni];int base=0,index=0;float radius=0;
        for(SceneMesh m:parts){int count=m.vertices.length/7;System.arraycopy(m.vertices,0,v,base*7,count*7);System.arraycopy(m.uv,0,uv,base*2,count*2);System.arraycopy(m.tangents,0,tangent,base*4,count*4);for(int n:m.indices)ix[index++]=base+n;base+=count;radius=Math.max(radius,m.radius);}
        SceneMesh m=new SceneMesh(v,ix,0,0,radius);m.uv=uv;m.tangents=tangent;m.authoredTangentFrame=true;m.pcFacility=true;m.pcWall=true;return m;
    }
}
