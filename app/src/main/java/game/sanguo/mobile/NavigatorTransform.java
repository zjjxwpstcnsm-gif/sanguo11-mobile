package game.sanguo.mobile;

/** North-up overview coordinates, independent of the main camera's yaw and buffer scale. */
final class NavigatorTransform {
    final float minX,minZ,width,depth;
    NavigatorTransform(float minX,float minZ,float maxX,float maxZ){
        this.minX=minX;this.minZ=minZ;width=Math.max(.001f,maxX-minX);depth=Math.max(.001f,maxZ-minZ);
    }
    float u(float x){return (x-minX)/width;}
    float v(float z){return (z-minZ)/depth;}
    float x(float u){return minX+Math.max(0,Math.min(1,u))*width;}
    float z(float v){return minZ+Math.max(0,Math.min(1,v))*depth;}
}
