package game.sanguo.mobile;

/** Restricted orthographic strategic camera; ray/ground intersection matches Filament lookAt. */
final class SceneCamera {
    float x,z,span=35,tilt=55; int width=1,height=1,facing=1;
    double sin(){return Math.sin(Math.toRadians(tilt));}
    double cos(){return Math.cos(Math.toRadians(tilt));}
    float pixels(){return height/(2*span);}
    float screenX(float wx){return width*.5f+(wx-x)*pixels()*facing;}
    float screenY(float wz,float y){return (float)(height*.5f+((wz-z)*sin()*facing-y*cos())*pixels());}
    float worldX(float sx){return x+(sx-width*.5f)/pixels()*facing;}
    float worldZ(float sy){return (float)(z+(sy-height*.5f)/pixels()/sin()*facing);}
    void pan(float dx,float dy){x-=dx/pixels()*facing;z-=dy/pixels()/sin()*facing;}
    void sanitize(){x=Float.isFinite(x)?x:0;z=Float.isFinite(z)?z:0;span=Float.isFinite(span)?Math.max(3,Math.min(160,span)):15;tilt=Float.isFinite(tilt)?Math.max(40,Math.min(70,tilt)):55;facing=facing<0?-1:1;}
    void zoom(float factor,float sx,float sy){if(!Float.isFinite(factor)||factor<=0)return;sanitize();float ox=worldX(sx),oz=worldZ(sy);span=Math.max(3,Math.min(160,span/factor));x+=ox-worldX(sx);z+=oz-worldZ(sy);}
}
