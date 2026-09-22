package game.sanguo.mobile;

/** Orthographic triangle picking on the actual visible mesh. No gameplay hitbox expansion. */
final class ScenePicking {
    static float hit(SceneCamera camera,SceneMesh mesh,float x,float y,float z,float yaw,float scale,float sx,float sy){
        float c=(float)Math.cos(yaw)*scale,s=(float)Math.sin(yaw)*scale,best=Float.NEGATIVE_INFINITY;
        float[] v=mesh.vertices;int[] ids=mesh.indices;
        for(int i=0;i<ids.length;i+=3){
            int a=ids[i]*7,b=ids[i+1]*7,d=ids[i+2]*7;
            float ax=camera.screenX(x+c*v[a]+s*v[a+2]),ay=camera.screenY(z-s*v[a]+c*v[a+2],y+scale*v[a+1]);
            float bx=camera.screenX(x+c*v[b]+s*v[b+2]),by=camera.screenY(z-s*v[b]+c*v[b+2],y+scale*v[b+1]);
            float dx=camera.screenX(x+c*v[d]+s*v[d+2]),dy=camera.screenY(z-s*v[d]+c*v[d+2],y+scale*v[d+1]);
            float det=(by-dy)*(ax-dx)+(dx-bx)*(ay-dy);if(Math.abs(det)<.0001f)continue;
            float u=((by-dy)*(sx-dx)+(dx-bx)*(sy-dy))/det,t=((dy-ay)*(sx-dx)+(ax-dx)*(sy-dy))/det;
            if(u>=0&&t>=0&&u+t<=1)best=Math.max(best,y+scale*(u*v[a+1]+t*v[b+1]+(1-u-t)*v[d+1]));
        }
        return best;
    }
}
