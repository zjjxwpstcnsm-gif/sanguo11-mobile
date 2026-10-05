package game.sanguo.mobile;

/** Exact reuse of immutable terrain occlusion at unchanged view coordinates.
 * One bounded entry per live proxy; no epsilon, gameplay state or time input. */
final class SceneLabelVisibility {
    private final SceneVisibilityStamp cameraStamp=new SceneVisibilityStamp();
    private TerrainSurface previousTerrain;
    private float previousX,previousZ,previousY;
    private boolean result;
    long computations;
    boolean visible(SceneCamera camera,TerrainSurface terrain,float x,float z,float y){
        if(previousTerrain==terrain&&cameraStamp.matches(camera)&&previousX==x&&previousZ==z&&previousY==y)return result;
        float sx=camera.screenX(x,z,y),sy=camera.screenY(x,z,y);
        float foreground=terrain.rayHeight(camera,sx,sy);
        result=!Float.isFinite(foreground)||foreground<=y+.001f;
        computations++;previousTerrain=terrain;previousX=x;previousZ=z;previousY=y;cameraStamp.set(camera);
        return result;
    }
}
