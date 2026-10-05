package game.sanguo.mobile;

/** Pure display transform from source EXE 0x41c510, including its height shear. */
final class PcWallGeometry {
    private PcWallGeometry(){}
    static final float SOURCE_LENGTH_SCALE=.04330126941204071f;
    static SceneMesh segment(SceneMesh source,float dx,float dz,float rise){
        float length=(float)Math.hypot(dx,dz);
        if(!Float.isFinite(length)||length<.00001f||!Float.isFinite(rise))throw new IllegalArgumentException("PC wall endpoints");
        float cs=dx/length,sn=dz/length,scale=length/.05f*SOURCE_LENGTH_SCALE,shear=rise/length;
        float[] v=source.vertices.clone(),normals=new float[v.length/7*3];float radius=0;
        for(int j=0;j<v.length/7;j++){
            int a=j*7;float x=v[a],y=v[a+1],z=v[a+2];
            v[a]=cs*scale*x-sn*z;v[a+1]=y+shear*x;v[a+2]=sn*scale*x+cs*z;
            float[] q=source.tangents;int t=j*4;float qx=q[t],qy=q[t+1],qz=q[t+2],qw=q[t+3];
            float nx=2*(qx*qz+qw*qy),ny=2*(qy*qz-qw*qx),nz=1-2*(qx*qx+qy*qy);
            // A^-T for source X stretch + height shear + XZ basis rotation.
            float along=(nx-shear*ny)/scale;
            normals[j*3]=cs*along-sn*nz;normals[j*3+1]=ny;normals[j*3+2]=sn*along+cs*nz;
            radius=Math.max(radius,(float)Math.hypot(v[a],v[a+2]));
        }
        SceneMesh mesh=new SceneMesh(v,source.indices.clone(),0,0,radius+.2f);mesh.uv=source.uv.clone();mesh.setNormals(normals);mesh.authoredTangentFrame=true;mesh.pcFacility=true;mesh.pcCliffWall=true;return mesh;
    }
}
