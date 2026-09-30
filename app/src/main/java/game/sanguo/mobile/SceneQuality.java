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
    boolean msaaSupported(int requestedGlesVersion){return this==HIGH&&requestedGlesVersion>=0x30001;}
    static final class Thermal {
        static final long RECOVERY_NANOS=30_000_000_000L;
        boolean constrained;private int status=-1;private long coolSince=-1;int transitions;
        // Severe heat reacts immediately. Recovery requires 30 continuous seconds
        // at LIGHT/NONE, including when Android sends no further status callback.
        boolean update(int next,long now){status=next;return tick(now);}
        boolean tick(long now){
            boolean before=constrained;
            if(status>=3){constrained=true;coolSince=-1;}
            else if(status>=0&&status<=1&&constrained){
                if(coolSince<0||now<coolSince)coolSince=now;
                if(now-coolSince>=RECOVERY_NANOS){constrained=false;coolSince=-1;}
            }else coolSince=-1;
            if(before!=constrained)transitions++;
            return before!=constrained;
        }
        int fps(SceneQuality quality){return constrained?30:quality.fps;}
        float scale(SceneQuality quality){return constrained?Math.min(.70f,quality.scale):quality.scale;}
    }
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
