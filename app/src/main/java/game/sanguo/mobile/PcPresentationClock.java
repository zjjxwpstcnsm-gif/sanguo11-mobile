package game.sanguo.mobile;

/** Visual playback alone. A rejected/delayed source pose earns no next step.
 * Normal frames retain the750ms nominal cue; stalls slow it instead of skipping
 * its original portrait/brush layers. This is not a frame-rate/performance fix. */
final class PcPresentationClock {
    static final int MAX_VISUAL_STEP_MILLIS=50;
    static long elapsedMillis(long elapsed,int speed,boolean submitted){
        if(!submitted)return 0;
        int multiplier=speed<=1?1:speed<=2?2:4;
        return Math.min(Math.max(0,elapsed),MAX_VISUAL_STEP_MILLIS/multiplier);
    }
    private PcPresentationClock(){}
}
