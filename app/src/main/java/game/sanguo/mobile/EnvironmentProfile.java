package game.sanguo.mobile;

import com.google.android.filament.*;

/** Versioned daylight environment, shared by land, water, architecture, vegetation and units. */
final class EnvironmentProfile {
    static final int VERSION=1;
    static final float SUN_LUX=50000, SKY_LUX=16000;
    static void exposure(Camera camera){camera.setExposure(11f,1f/125f,100f);}
    static IndirectLight sky(Engine engine){
        return new IndirectLight.Builder().irradiance(1,new float[]{.72f,.80f,1f}).intensity(SKY_LUX).build(engine);
    }
    static void sun(Engine engine,int entity,SceneQuality quality,boolean shadows){
        LightManager.ShadowOptions options=new LightManager.ShadowOptions();
        options.mapSize=quality==SceneQuality.HIGH?1024:512;options.shadowCascades=1;
        options.shadowFar=28;options.normalBias=.4f;options.constantBias=.001f;options.stable=true;
        new LightManager.Builder(LightManager.Type.DIRECTIONAL).direction(-1,-2,-1)
            .color(1,.95f,.85f).intensity(SUN_LUX).castShadows(shadows).shadowOptions(options).build(engine,entity);
    }
}
