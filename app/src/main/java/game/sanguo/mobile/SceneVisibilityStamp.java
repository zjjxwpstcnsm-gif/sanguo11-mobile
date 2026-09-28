package game.sanguo.mobile;

/** Exact view-pixel camera stamp; no epsilon can hide a real pick/culling change. */
final class SceneVisibilityStamp {
    private float x,z,span,yaw,tilt;private int width,height,facing;private boolean valid;
    boolean matches(SceneCamera c){return valid&&x==c.x&&z==c.z&&span==c.span&&yaw==c.yaw&&tilt==c.tilt&&width==c.width&&height==c.height&&facing==c.facing;}
    void set(SceneCamera c){x=c.x;z=c.z;span=c.span;yaw=c.yaw;tilt=c.tilt;width=c.width;height=c.height;facing=c.facing;valid=true;}
    void invalidate(){valid=false;}
}
