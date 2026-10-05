package game.sanguo.mobile;

import game.sanguo.core.Hex;
import game.sanguo.core.War;
import game.sanguo.core.TurnJournal;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Original map-unit skins and FCVD curves. Detached visual data; no RNG or World mutation. */
final class PcUnits {
    record Model(int resource,int[] parents,int[] nodes,float[] binds,float[] attributes,float[] weights,int[] bones,int[] indices,int opaqueIndices) {}
    record Curve(int node,int kind,int[] upper,float[] coefficients) {
        float at(float frame){
            int lo=0,hi=upper.length-1;
            while(lo<hi){int mid=(lo+hi)>>>1;if(upper[mid]+(double)1e-6f>=frame)hi=mid;else lo=mid+1;}
            int p=lo*3;
            return (float)(((double)frame*coefficients[p+2]+coefficients[p+1])*frame+coefficients[p]);
        }
    }
    record Clip(int frames,int nodes,Curve[] curves) {}
    record Selection(int kind,int state,int model,int clip,int frame) {
        String key(){return "pc-unit:"+model+":"+clip+":"+frame;}
    }
    private final Model[] models=new Model[14];
    private final Clip[] clips=new Clip[75];
    private final int[] kindModels=new int[14];
    private final int[][] motions=new int[14][8];
    private final float[][] layouts=new float[8][76*2];

    PcUnits(InputStream source)throws IOException {
        byte[] bytes;
        try(InputStream raw=source;InputStream in=new GZIPInputStream(raw);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(out.size()+n>2_000_000)throw new IOException("PC unit budget");out.write(block,0,n);}bytes=out.toByteArray();
        }
        try{
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(b.getLong()!=0x323054494e554350L||b.getInt()!=14||b.getInt()!=75||b.getInt()!=14||b.getInt()!=8)throw new IOException("PC unit header");
            for(int i=0;i<14;i++){kindModels[i]=bounded(b.getInt(),0,13,"model binding");for(int s=0;s<8;s++)motions[i][s]=bounded(b.getInt(),0,74,"motion binding");}
            for(float[] layout:layouts)for(int i=0;i<layout.length;i++)layout[i]=number(b,32);
            for(int i=0;i<14;i++)models[i]=readModel(b,2207+i*2);
            for(int i=0;i<75;i++)clips[i]=readClip(b);
            if(b.hasRemaining())throw new IOException("PC unit trailing bytes");
            for(int k=0;k<14;k++)for(int s=0;s<8;s++)if(models[model(k,s)].parents.length!=clips[motions[k][s]].nodes)throw new IOException("PC unit binding rig mismatch");
        }catch(BufferUnderflowException|IllegalArgumentException e){throw new IOException("PC units truncated",e);}
    }
    static Model readModel(ByteBuffer b,int expectedResource)throws IOException {
        int resource=b.getInt(),nodes=bounded(b.getInt(),1,64,"nodes"),palettes=bounded(b.getInt(),1,128,"palettes"),nv=bounded(b.getInt(),1,30000,"vertices"),ni=bounded(b.getInt(),3,100000,"indices");
        int opaque=bounded(b.getInt(),0,ni,"opaque draw indices");
        if(resource!=expectedResource||ni%3!=0||opaque%3!=0)throw new IOException("PC unit model contract");
        int[] parents=new int[nodes],targets=new int[palettes];
        for(int j=0;j<nodes;j++)parents[j]=bounded(b.getInt(),-1,j-1,"parent");
        for(int j=0;j<palettes;j++)targets[j]=bounded(b.getInt(),-1,nodes-1,"palette node");
        float[] binds=floats(b,palettes*16,256),attributes=floats(b,nv*12,256),weights=floats(b,nv*4,1.000002f);
        int[] bones=new int[nv*4],indices=new int[ni];
        for(int j=0;j<bones.length;j++)bones[j]=bounded(b.getShort()&65535,0,palettes-1,"palette index");
        for(int j=0;j<ni;j++)indices[j]=bounded(b.getInt(),0,nv-1,"triangle index");
        for(int j=0;j<nv;j++){float sum=0;for(int k=0;k<4;k++){float w=weights[j*4+k];if(w< -2e-6f)throw new IOException("PC unit negative weight");sum+=w;}if(Math.abs(sum-1)>2e-6f)throw new IOException("PC unit weight sum");}
        return new Model(resource,parents,targets,binds,attributes,weights,bones,indices,opaque);
    }
    static Clip readClip(ByteBuffer b)throws IOException {
        int frames=bounded(b.getInt(),2,1000,"frames"),nodes=bounded(b.getInt(),1,64,"clip nodes"),count=b.getInt();
        if(count!=nodes*7)throw new IOException("PC motion channels");
        Curve[] curves=new Curve[count];Set<Integer> seen=new HashSet<>();
        for(int j=0;j<count;j++){
            int node=bounded(b.getInt(),0,nodes-1,"curve node"),kind=b.getInt(),n=bounded(b.getInt(),1,frames,"keys");
            if(!(kind>=6&&kind<=8||kind>=34&&kind<=37)||!seen.add(node*64+kind))throw new IOException("PC motion duplicate/type");
            int[] upper=new int[n];float[] coefficients=new float[n*3];int previous=-1;
            for(int k=0;k<n;k++){upper[k]=bounded(b.getInt(),previous+1,frames-1,"keyframe");previous=upper[k];for(int c=0;c<3;c++)coefficients[k*3+c]=number(b,100000);}
            if(previous!=frames-1)throw new IOException("PC motion uncovered duration");curves[j]=new Curve(node,kind,upper,coefficients);
        }
        return new Clip(frames,nodes,curves);
    }
    private static int bounded(int n,int min,int max,String name)throws IOException{if(n<min||n>max)throw new IOException("PC unit "+name);return n;}
    private static float number(ByteBuffer b,float limit)throws IOException{float v=b.getFloat();if(!Float.isFinite(v)||Math.abs(v)>limit)throw new IOException("PC unit numeric range");return v;}
    private static float[] floats(ByteBuffer b,int n,float limit)throws IOException{float[] values=new float[n];for(int i=0;i<n;i++)values[i]=number(b,limit);return values;}
    int model(int kind,int state){return kind==3&&state==2?0:kind==4&&state==3?5:kindModels[kind];}
    static int kind(UnitVisual u,boolean naval){
        if(naval)return u.mission?10:switch(u.ship){case BOAT->10;case TOWER_SHIP->11;case WARSHIP->12;};
        if(u.mission)return 13;
        return switch(u.weapon){case SWORD->0;case SPEAR->1;case HALBERD->2;case CROSSBOW->3;case CAVALRY->4;case RAM->6;case SIEGE_TOWER->7;case CATAPULT->8;case WOODEN_BEAST->9;};
    }
    Selection select(int kind,int state,float frame){int clip=motions[kind][state];return new Selection(kind,state,model(kind,state),clip,Math.max(0,Math.min(clips[clip].frames-1,(int)frame)));}
    int frames(int kind,int state){return clips[motions[kind][state]].frames;}
    Selection idle(UnitVisual unit){int kind=kind(unit,unit.naval);return select(kind,kind<=5?1:0,0);}
    Selection animation(UnitVisual unit,UnitAnimation animation,long tick,TurnJournal.Event event,float fraction){
        int kind=kind(unit,animation.naval),state=kind<=5?1:0;
        float frame;
        if(animation.clip.equals("idle")){
            if(unit.status!=War.Status.NORMAL)state=7;
            int end=frames(kind,state)-1;
            frame=end>0?(tick*60/1000)%end:0;
        }else{
            // Source pose trajectories distinguish standing, stride, weapon swing,
            // crouch/reaction, collapse and confusion. These event semantics remain
            // provisional until crosschecked against a running PC capture.
            state=animation.clip.equals("walk")||animation.clip.equals("turn")||animation.clip.equals("enter")?1:animation.clip.equals("defeat")?6:animation.clip.equals("hit")?4:animation.clip.equals("prepare")?0:event!=null&&event.kind==TurnJournal.Kind.TACTIC?5:2;
            float f=CombatVisual.phase(event,fraction);
            if(animation.clip.equals("attack")){
                TurnJournal.Strike strike=CombatVisual.strike(event,fraction);
                Hex from=strike!=null?strike.start:event==null?null:event.start;
                Hex to=strike!=null?strike.target:event==null?null:event.target;
                // The EXE switches crossbows to the sword model only for its
                // close-combat state. Keep ranged fire on the original bow rig.
                if((kind==3||kind==4)&&from!=null&&to!=null&&from.distance(to)>1)state=3;
                float launch=event!=null&&!event.strikes.isEmpty()?CombatVisual.LAUNCH:.25f;
                f=CombatVisual.fraction((f-launch)/(1-launch));
            }else if(animation.clip.equals("hit")||animation.clip.equals("defeat")){
                f=CombatVisual.fraction((f-CombatVisual.HIT)/(1-CombatVisual.HIT));
            }else if(animation.clip.equals("prepare")){
                float launch=event!=null&&!event.strikes.isEmpty()?CombatVisual.LAUNCH:.25f;
                f=CombatVisual.fraction(f/launch);
            }
            if(animation.clip.equals("walk"))frame=(tick*60/1000)%Math.max(1,frames(kind,state)-1);
            else frame=f*(frames(kind,state)-1);
        }
        return select(kind,state,frame);
    }
    float[] layout(int index){return layouts[index].clone();}
    static int members(int troops,boolean single){return single?1:Math.min(76,24+Math.max(0,Math.min(troops,15000))/250);}
    int sourceResource(Selection s){return models[s.model].resource;}
    SceneMesh pose(Selection s)throws InterruptedException{return pose(s.model,s.clip,s.frame);}
    SceneMesh pose(int modelIndex,int clipIndex,float frame)throws InterruptedException {
        SceneMesh mesh=skin(models[modelIndex],clips[clipIndex],frame);mesh.pcUnit=true;mesh.pcUnitModel=modelIndex;return mesh;
    }
    static SceneMesh skin(Model model,Clip clip,float frame)throws InterruptedException {
        if(model.parents.length!=clip.nodes)throw new IllegalArgumentException("PC unit rig mismatch");
        frame=Math.max(0,Math.min(clip.frames-1,frame));
        float[][] values=new float[clip.nodes][7];
        for(Curve c:clip.curves)values[c.node][c.kind<10?c.kind-6:c.kind-31]=c.at(frame);
        float[][] globals=new float[clip.nodes][];
        for(int i=0;i<globals.length;i++){
            float[] v=values[i];float x=v[3],y=v[4],z=v[5],w=v[6];
            float[] m={1-2*(y*y+z*z),2*(x*y+z*w),2*(x*z-y*w),0,2*(x*y-z*w),1-2*(x*x+z*z),2*(y*z+x*w),0,2*(x*z+y*w),2*(y*z-x*w),1-2*(x*x+y*y),0,v[0],v[1],v[2],1};
            globals[i]=model.parents[i]<0?m:multiply(m,globals[model.parents[i]]);
        }
        float[][] palette=new float[model.nodes.length][];
        for(int i=0;i<palette.length;i++){float[] bind=Arrays.copyOfRange(model.binds,i*16,i*16+16);palette[i]=model.nodes[i]<0?bind:multiply(bind,globals[model.nodes[i]]);}
        int count=model.attributes.length/12;float[] vertices=new float[count*7],normals=new float[count*3],uv=new float[count*2];float radius=0;
        for(int i=0;i<count;i++){
            if((i&255)==0&&Thread.currentThread().isInterrupted())throw new InterruptedException("PC unit superseded");
            int p=i*12;float[] a=model.attributes;float px=0,py=0,pz=0,nx=0,ny=0,nz=0;
            for(int k=0;k<4;k++){
                float weight=model.weights[i*4+k];if(weight==0)continue;float[] m=palette[model.bones[i*4+k]];
                px+=(a[p]*m[0]+a[p+1]*m[4]+a[p+2]*m[8]+m[12])*weight;
                py+=(a[p]*m[1]+a[p+1]*m[5]+a[p+2]*m[9]+m[13])*weight;
                pz+=(a[p]*m[2]+a[p+1]*m[6]+a[p+2]*m[10]+m[14])*weight;
                nx+=(a[p+3]*m[0]+a[p+4]*m[4]+a[p+5]*m[8])*weight;
                ny+=(a[p+3]*m[1]+a[p+4]*m[5]+a[p+5]*m[9])*weight;
                nz+=(a[p+3]*m[2]+a[p+4]*m[6]+a[p+5]*m[10])*weight;
            }
            // Uniform source world scale, shared with original map and objects.
            vertices[i*7]=px*game.sanguo.core.PcMap.WORLD_SCALE;vertices[i*7+1]=py*game.sanguo.core.PcMap.WORLD_SCALE;vertices[i*7+2]=pz*game.sanguo.core.PcMap.WORLD_SCALE;
            System.arraycopy(a,p+8,vertices,i*7+3,4);uv[i*2]=a[p+6];uv[i*2+1]=a[p+7];
            normals[i*3]=nx/game.sanguo.core.PcMap.WORLD_SCALE;normals[i*3+1]=ny/game.sanguo.core.PcMap.WORLD_SCALE;normals[i*3+2]=nz/game.sanguo.core.PcMap.WORLD_SCALE;
            radius=Math.max(radius,(float)Math.hypot(vertices[i*7],vertices[i*7+2]));
        }
        SceneMesh mesh=new SceneMesh(vertices,model.indices,0,0,radius+.01f);mesh.uv=uv;mesh.setNormals(normals);mesh.authoredTangentFrame=true;mesh.pcUnitOpaqueIndices=model.opaqueIndices;return mesh;
    }
    private static float[] multiply(float[] a,float[] b){float[] c=new float[16];for(int row=0;row<4;row++)for(int col=0;col<4;col++)for(int k=0;k<4;k++)c[row*4+col]+=a[row*4+k]*b[k*4+col];return c;}
}
