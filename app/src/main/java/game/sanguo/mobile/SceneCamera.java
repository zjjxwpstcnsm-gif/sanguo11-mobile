package game.sanguo.mobile;

/** Strategy camera: source PC perspective, compatible legacy orthographic.
 * All input/projection uses view pixels, never buffer pixels. */
final class SceneCamera {
    float x,z,span=35,tilt=55,yaw,heightLimit=TerrainSurface.MAX_HEIGHT; int width=1,height=1,facing=1;
    boolean perspective;
    double sin(){return Math.sin(Math.toRadians(tilt));}
    double cos(){return Math.cos(Math.toRadians(tilt));}
    /** Supplied PC runtime: original414fd0 camera snapshots before/after
     * normal mouse-wheel zoom, docs/pc-visual/PC_RUNTIME.md. Fixed30degree
     * vertical field, near16 source units, far=1.7*eye distance, targetY16.
     * Scene units are source*.05. Legacy/custom maps keep their own lens.
     */
    private static final double PC_HALF_FOV_TAN=Math.tan(.5235987901687622*.5);
    double eyeDistance(){return perspective?span/PC_HALF_FOV_TAN:300;}
    double targetHeight(){return perspective?.8:0;}
    double nearPlane(){return perspective?.8:.1;}
    double farPlane(){return perspective?eyeDistance()*1.7:1000;}
    double[] projection(double near,double far){
        double d=eyeDistance(),aspect=width/(double)height;
        return new double[]{d/(span*aspect),0,0,0,0,d/span,0,0,0,0,-(far+near)/(far-near),-1,0,0,-2*far*near/(far-near),0};
    }
    double rightX(){return Math.cos(Math.toRadians(yaw))*facing;}
    double backX(){return Math.sin(Math.toRadians(yaw))*facing;}
    float pixels(){return height/(2*span);}
    double depth(float wx,float wz,float y){return eyeDistance()-((wx-x)*backX()+(wz-z)*rightX())*cos()-(y-targetHeight())*sin();}
    private double magnification(float wx,float wz,float y){return perspective?eyeDistance()/depth(wx,wz,y):1;}
    float screenX(float wx,float wz){return screenX(wx,wz,0);}
    double projectedX(float wx,float wz,float y){return width*.5+((wx-x)*rightX()-(wz-z)*backX())*pixels()*magnification(wx,wz,y);}
    double projectedY(float wx,float wz,float y){return height*.5+(((wx-x)*backX()+(wz-z)*rightX())*sin()-(y-targetHeight())*cos())*pixels()*magnification(wx,wz,y);}
    float screenX(float wx,float wz,float y){return (float)projectedX(wx,wz,y);}
    float screenY(float wx,float wz,float y){return (float)projectedY(wx,wz,y);}
    // Legacy zero-azimuth probes; production callers always supply both world axes.
    float screenX(float wx){return screenX(wx,z);}
    float screenY(float wz,float y){return screenY(x,wz,y);}
    private double rayDistance(float sy,float y){double d=eyeDistance(),v=-(sy-height*.5)/pixels()/d;return (y-targetHeight()-d*sin())/(v*cos()-sin());}
    float worldX(float sx,float sy,float y){
        if(perspective){double d=eyeDistance(),t=rayDistance(sy,y),u=t*(sx-width*.5)/pixels()/d,b=d*cos()+t*((sy-height*.5)/pixels()/d*sin()-cos());return (float)(x+u*rightX()+b*backX());}
        double u=(sx-width*.5)/pixels(),v=((sy-height*.5)/pixels()+y*cos())/sin();return (float)(x+u*rightX()+v*backX());}
    float worldZ(float sx,float sy,float y){
        if(perspective){double d=eyeDistance(),t=rayDistance(sy,y),u=t*(sx-width*.5)/pixels()/d,b=d*cos()+t*((sy-height*.5)/pixels()/d*sin()-cos());return (float)(z-u*backX()+b*rightX());}
        double u=(sx-width*.5)/pixels(),v=((sy-height*.5)/pixels()+y*cos())/sin();return (float)(z-u*backX()+v*rightX());}
    float worldX(float sx){return worldX(sx,height*.5f,0);}
    float worldZ(float sy){return worldZ(width*.5f,sy,0);}
    void pan(float dx,float dy){pan(dx,dy,width*.5f,height*.5f,0);}
    void pan(float dx,float dy,float sx,float sy,float y){
        if(!perspective){x-=(dx*rightX()+dy/sin()*backX())/pixels();z-=(-dx*backX()+dy/sin()*rightX())/pixels();return;}
        float wx=worldX(sx-dx,sy-dy,y),wz=worldZ(sx-dx,sy-dy,y);anchor(wx,wz,y,sx,sy);
    }
    static float legacySpan(float viewportHeight,float density,float scaleDp){
        if(!Float.isFinite(viewportHeight)||!Float.isFinite(density)||!Float.isFinite(scaleDp)||viewportHeight<=0||density<=0||scaleDp<=0)return 15;
        return Math.max(3,Math.min(160,viewportHeight/(2*TileGeometry.DY*density*scaleDp)));
    }
    void sanitize(){x=Float.isFinite(x)?x:0;z=Float.isFinite(z)?z:0;span=Float.isFinite(span)?Math.max(3,Math.min(160,span)):15;tilt=Float.isFinite(tilt)?Math.max(40,Math.min(70,tilt)):55;yaw=Float.isFinite(yaw)?((yaw%360)+360)%360:0;facing=facing<0?-1:1;}
    /** Camera bounds belong to the complete immutable map, never the transient chunk cache. */
    void clampTo(MapSceneSnapshot.Ground ground){
        heightLimit=ground.pcMap==null?TerrainSurface.MAX_HEIGHT:game.sanguo.core.PcMap.MAX_HEIGHT;
        sanitize();x=Math.max(ground.minX,Math.min(ground.maxX,x));z=Math.max(ground.minZ,Math.min(ground.maxZ,z));
    }
    void zoom(float factor,float sx,float sy){zoom(factor,sx,sy,0);}
    void zoom(float factor,float sx,float sy,float groundY){
        if(!Float.isFinite(factor)||factor<=0)return;sanitize();
        float ox=worldX(sx,sy,groundY),oz=worldZ(sx,sy,groundY);
        span=Math.max(3,Math.min(160,span/factor));anchor(ox,oz,groundY,sx,sy);
    }
    void orbit(float degrees,float pitch,float sx,float sy,float groundY){
        float ox=worldX(sx,sy,groundY),oz=worldZ(sx,sy,groundY);
        yaw+=degrees;tilt+=pitch;sanitize();anchor(ox,oz,groundY,sx,sy);
    }
    private void anchor(float wx,float wz,float y,float sx,float sy){x+=wx-worldX(sx,sy,y);z+=wz-worldZ(sx,sy,y);}
    private float extent(boolean axisX){float maximum=0;for(int i=0;i<8;i++){float sx=(i&1)==0?0:width,sy=(i&2)==0?0:height,y=(i&4)==0?0:heightLimit;maximum=Math.max(maximum,Math.abs(axisX?worldX(sx,sy,y)-x:worldZ(sx,sy,y)-z));}return maximum;}
    float extentX(){return perspective?extent(true):(float)(Math.abs(rightX())*span*width/height+Math.abs(backX())*(span/sin()+heightLimit*cos()/sin()));}
    float extentZ(){return perspective?extent(false):(float)(Math.abs(backX())*span*width/height+Math.abs(rightX())*(span/sin()+heightLimit*cos()/sin()));}
}
