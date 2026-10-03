package game.sanguo.mobile;

/** Exact view-pixel camera stamp; no epsilon can hide a real pick/culling change. */
final class SceneVisibilityStamp {
    private float x,z,span,yaw,tilt,heightLimit;private int width,height,facing;private boolean valid,perspective;
    boolean matches(SceneCamera c){return valid&&x==c.x&&z==c.z&&span==c.span&&yaw==c.yaw&&tilt==c.tilt&&width==c.width&&height==c.height&&facing==c.facing&&perspective==c.perspective&&heightLimit==c.heightLimit;}
    void set(SceneCamera c){x=c.x;z=c.z;span=c.span;yaw=c.yaw;tilt=c.tilt;width=c.width;height=c.height;facing=c.facing;perspective=c.perspective;heightLimit=c.heightLimit;valid=true;}
    void invalidate(){valid=false;}
}
