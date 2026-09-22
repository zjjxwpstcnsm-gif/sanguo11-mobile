package game.sanguo.mobile;

/** Presentation-only presets; one vsync scheduler, no changes to simulation time. */
enum SceneQuality {
    LOW("低 · 30 帧",30,.70f,256,1,1,64),
    MEDIUM("中 · 30 帧",30,.85f,512,0,1,96),
    HIGH("高 · 60 帧",60,1f,1024,0,0,128);
    final String label;final int fps,atlasSize,minSiteLod,minUnitLod,poseCache;final float scale;
    SceneQuality(String label,int fps,float scale,int atlasSize,int sites,int units,int cache){
        this.label=label;this.fps=fps;this.scale=scale;this.atlasSize=atlasSize;minSiteLod=sites;minUnitLod=units;poseCache=cache;
    }
    static SceneQuality from(Object value){try{return value instanceof String?valueOf((String)value):MEDIUM;}catch(IllegalArgumentException e){return MEDIUM;}}
    static final class Pacer {
        private long deadline;
        void reset(){deadline=0;}
        boolean due(long now,int fps){
            long period=1_000_000_000L/fps;
            if(deadline!=0&&now+500_000L<deadline)return false;
            if(deadline==0||now-deadline>period*2)deadline=now+period;
            else deadline+=period;
            return true;
        }
    }
}
