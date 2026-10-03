package game.sanguo.mobile;

/** Screen-span hysteresis only. The replay clock and authoritative cursor are never reset. */
final class UnitLod {
    private UnitLod() {}
    static int select(int previous,float span,int minimum) {
        if(!Float.isFinite(span))return Math.max(minimum,previous);
        int result=previous;
        if(result==0&&span>8.8f)result=1;
        if(result==1&&span<7.2f)result=0;
        if(result<2&&span>24f)result=2;
        if(result==2&&span<20f)result=span<7.2f?0:1;
        return Math.max(minimum,result);
    }
    static int idleFrame(long millis,int id,int lod) {
        // Quantize ONE clock. Lower detail holds frames, it does not slow or restart the cycle.
        int phase=(int)Math.floorMod(millis/83+id*7L,12);
        int step=lod==0?1:lod==1?2:4;
        return phase/step*step;
    }
}
