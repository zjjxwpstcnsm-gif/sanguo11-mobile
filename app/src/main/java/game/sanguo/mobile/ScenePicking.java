package game.sanguo.mobile;

/** Triangle picking on the actual visible mesh. No gameplay hitbox expansion. */
final class ScenePicking {
    static float hit(SceneCamera camera,SceneMesh mesh,float x,float y,float z,float yaw,float scale,float sx,float sy){
        return hit(camera,mesh,x,y,z,yaw,scale,sx,sy,0,0,0,1,0,0);
    }
    static float hit(SceneCamera camera,SceneMesh mesh,float x,float y,float z,float yaw,float scale,float sx,float sy,
                     float ox,float oy,float oz,float memberScale,float gx,float gz){
        float c=(float)Math.cos(yaw)*scale,s=(float)Math.sin(yaw)*scale,best=Float.NEGATIVE_INFINITY;
        float[] v=mesh.vertices;int[] ids=mesh.indices;
        for(int i=0;i<ids.length;i+=3){
            int a=ids[i]*7,b=ids[i+1]*7,d=ids[i+2]*7;
            float awx=x+c*(ox+memberScale*v[a])+s*(oz+memberScale*v[a+2]),awz=z-s*(ox+memberScale*v[a])+c*(oz+memberScale*v[a+2]);
            float awy=y+scale*(oy+memberScale*(v[a+1]+gx*v[a]+gz*v[a+2]));
            float ax=camera.screenX(awx,awz,awy),ay=camera.screenY(awx,awz,awy);
            float bwx=x+c*(ox+memberScale*v[b])+s*(oz+memberScale*v[b+2]),bwz=z-s*(ox+memberScale*v[b])+c*(oz+memberScale*v[b+2]);
            float bwy=y+scale*(oy+memberScale*(v[b+1]+gx*v[b]+gz*v[b+2]));
            float bx=camera.screenX(bwx,bwz,bwy),by=camera.screenY(bwx,bwz,bwy);
            float dwx=x+c*(ox+memberScale*v[d])+s*(oz+memberScale*v[d+2]),dwz=z-s*(ox+memberScale*v[d])+c*(oz+memberScale*v[d+2]);
            float dwy=y+scale*(oy+memberScale*(v[d+1]+gx*v[d]+gz*v[d+2]));
            float dx=camera.screenX(dwx,dwz,dwy),dy=camera.screenY(dwx,dwz,dwy);
            if(camera.perspective){
                double pax=camera.projectedX(awx,awz,awy),pay=camera.projectedY(awx,awz,awy),pbx=camera.projectedX(bwx,bwz,bwy),pby=camera.projectedY(bwx,bwz,bwy),pdx=camera.projectedX(dwx,dwz,dwy),pdy=camera.projectedY(dwx,dwz,dwy);
                double det=(pby-pdy)*(pax-pdx)+(pdx-pbx)*(pay-pdy);if(Math.abs(det)<.0001)continue;
                double u=((pby-pdy)*(sx-pdx)+(pdx-pbx)*(sy-pdy))/det,t=((pdy-pay)*(sx-pdx)+(pax-pdx)*(sy-pdy))/det;
                if(u>=0&&t>=0&&u+t<=1){double wa=u/camera.depth(awx,awz,awy),wb=t/camera.depth(bwx,bwz,bwy),wd=(1-u-t)/camera.depth(dwx,dwz,dwy);best=Math.max(best,(float)((wa*awy+wb*bwy+wd*dwy)/(wa+wb+wd)));}
            }else{
                float det=(by-dy)*(ax-dx)+(dx-bx)*(ay-dy);if(Math.abs(det)<.0001f)continue;
                float u=((by-dy)*(sx-dx)+(dx-bx)*(sy-dy))/det,t=((dy-ay)*(sx-dx)+(ax-dx)*(sy-dy))/det;
                if(u>=0&&t>=0&&u+t<=1)best=Math.max(best,y+scale*(oy+memberScale*(u*(v[a+1]+gx*v[a]+gz*v[a+2])+t*(v[b+1]+gx*v[b]+gz*v[b+2])+(1-u-t)*(v[d+1]+gx*v[d]+gz*v[d+2]))));
            }
        }
        return best;
    }
}
