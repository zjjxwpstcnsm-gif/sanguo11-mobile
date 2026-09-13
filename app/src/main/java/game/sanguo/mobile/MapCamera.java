package game.sanguo.mobile;

/** Pixel-space camera independent of engine work and Android gesture dispatch. */
final class MapCamera {
    float scale=1, minScale=1, maxScale=4, x, y;
    private float width,height,worldWidth,worldHeight,density=1,radius;
    void resize(float width,float height,float worldWidth,float worldHeight,float radius,float density) {
        float cx=this.width>0?(this.width/2-x)/scale:0,cy=this.height>0?(this.height/2-y)/scale:0;
        float ratio=scale/minScale;boolean first=this.width==0;
        this.width=width;this.height=height;this.worldWidth=worldWidth;this.worldHeight=worldHeight;this.radius=radius;this.density=density;
        minScale=Math.max(.05f,Math.min(Math.max(1,width-32*density)/worldWidth,Math.max(1,height-32*density)/worldHeight));
        maxScale=Math.max(minScale*4,2*density);
        if(first)fit();else{scale=bounded(minScale*ratio);x=width/2-cx*scale;y=height/2-cy*scale;clamp();}
    }
    void fit(){scale=minScale;x=(width-worldWidth*scale)/2+radius*scale;y=(height-worldHeight*scale)/2+radius*scale;clamp();}
    void focus(float wx,float wy){scale=bounded(Math.max(minScale*2,1.25f*density));x=width/2-wx*scale;y=height/2-wy*scale;clamp();}
    void zoom(float value,float fx,float fy){float old=scale;scale=bounded(value);x=fx-(fx-x)*scale/old;y=fy-(fy-y)*scale/old;clamp();}
    void pan(float dx,float dy){x+=dx;y+=dy;clamp();}
    void restore(float ratio,float cx,float cy){scale=bounded(minScale*ratio);x=width/2-cx*scale;y=height/2-cy*scale;clamp();}
    float centerX(){return (width/2-x)/scale;} float centerY(){return (height/2-y)/scale;}
    private float bounded(float s){return Math.max(minScale,Math.min(maxScale,s));}
    private float axis(float offset,float view,float extent){
        float size=extent*scale,margin=16*density;
        if(size<=view-2*margin)return (view-size)/2+radius*scale;
        return Math.max(view-margin-(extent-radius)*scale,Math.min(margin+radius*scale,offset));
    }
    private void clamp(){x=axis(x,width,worldWidth);y=axis(y,height,worldHeight);}
}
