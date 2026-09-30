package game.sanguo.mobile;

/** Projects the actual shared unit mesh and GPU formation/contact transform into
 * the tactical overlay. No synthetic unit artwork, GPU copies or rule state. */
final class UnitSilhouette {
    static float[] project(SceneMesh mesh,SceneCamera camera,UnitFormation formation,
                           float x,float z,float yaw,float scale) {
        int vertices=mesh.vertices.length/7;
        float[] points=new float[vertices*formation.count*2];
        float c=(float)Math.cos(yaw)*scale,s=(float)Math.sin(yaw)*scale;
        for(int member=0;member<formation.count;member++) {
            float[] m=formation.placement[member],g=formation.grade[member];
            for(int i=0;i<vertices;i++) {
                int v=i*7,out=(member*vertices+i)*2;
                float lx=mesh.vertices[v]*m[3],ly=mesh.vertices[v+1]*m[3],lz=mesh.vertices[v+2]*m[3];
                float wx=x+c*(m[0]+lx)+s*(m[2]+lz),wz=z-s*(m[0]+lx)+c*(m[2]+lz);
                float wy=formation.rootY+scale*(m[1]+ly+g[0]*lx+g[1]*lz);
                points[out]=camera.screenX(wx,wz);points[out+1]=camera.screenY(wx,wz,wy);
            }
        }
        return points;
    }
    private UnitSilhouette(){}
}
