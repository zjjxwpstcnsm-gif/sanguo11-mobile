package game.sanguo.mobile;
public final class SceneQualityTest {
    public static void main(String[] args){
        if(SceneQuality.from("obsolete")!=SceneQuality.MEDIUM||SceneQuality.from(5)!=SceneQuality.MEDIUM)throw new AssertionError("preference recovery");
        for(int hz:new int[]{60,90,120,144})for(SceneQuality q:SceneQuality.values()){
            SceneQuality.Pacer p=new SceneQuality.Pacer();int frames=0;
            for(int i=0;i<hz*60;i++)if(p.due(i*1_000_000_000L/hz,q.fps))frames++;
            if(Math.abs(frames-q.fps*60)>1)throw new AssertionError(hz+"Hz "+q+" frames="+frames);
            if(!p.due(120_000_000_000L,q.fps))throw new AssertionError("resume gap");
            p.reset();if(!p.due(0,q.fps))throw new AssertionError("reset");
        }
        System.out.println("PASS S08 pacing: 30/60fps on 60/90/120/144Hz; gap, reset and invalid preferences");
    }
}
