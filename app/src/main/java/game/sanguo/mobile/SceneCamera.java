package game.sanguo.mobile;

/** Orthographic strategy camera. All input/projection uses view pixels, never buffer pixels. */
final class SceneCamera {
    float x,z,span=35,tilt=55,yaw; int width=1,height=1,facing=1;
    double sin(){return Math.sin(Math.toRadians(tilt));}
    double cos(){return Math.cos(Math.toRadians(tilt));}
    double rightX(){return Math.cos(Math.toRadians(yaw))*facing;}
    double backX(){return Math.sin(Math.toRadians(yaw))*facing;}
    float pixels(){return height/(2*span);}
    float screenX(float wx,float wz){return (float)(width*.5+((wx-x)*rightX()-(wz-z)*backX())*pixels());}
    float screenY(float wx,float wz,float y){return (float)(height*.5+(((wx-x)*backX()+(wz-z)*rightX())*sin()-y*cos())*pixels());}
    // Legacy zero-azimuth probes; production callers always supply both world axes.
    float screenX(float wx){return screenX(wx,z);}
    float screenY(float wz,float y){return screenY(x,wz,y);}
    float worldX(float sx,float sy,float y){double u=(sx-width*.5)/pixels(),v=((sy-height*.5)/pixels()+y*cos())/sin();return (float)(x+u*rightX()+v*backX());}
    float worldZ(float sx,float sy,float y){double u=(sx-width*.5)/pixels(),v=((sy-height*.5)/pixels()+y*cos())/sin();return (float)(z-u*backX()+v*rightX());}
    float worldX(float sx){return worldX(sx,height*.5f,0);}
    float worldZ(float sy){return worldZ(width*.5f,sy,0);}
    void pan(float dx,float dy){x-=(dx*rightX()+dy/sin()*backX())/pixels();z-=(-dx*backX()+dy/sin()*rightX())/pixels();}
    void sanitize(){x=Float.isFinite(x)?x:0;z=Float.isFinite(z)?z:0;span=Float.isFinite(span)?Math.max(3,Math.min(160,span)):15;tilt=Float.isFinite(tilt)?Math.max(40,Math.min(70,tilt)):55;yaw=Float.isFinite(yaw)?((yaw%360)+360)%360:0;facing=facing<0?-1:1;}
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
    float extentX(){return (float)(Math.abs(rightX())*span*width/height+Math.abs(backX())*(span/sin()+TerrainSurface.MAX_HEIGHT*cos()/sin()));}
    float extentZ(){return (float)(Math.abs(backX())*span*width/height+Math.abs(rightX())*(span/sin()+TerrainSurface.MAX_HEIGHT*cos()/sin()));}
}
