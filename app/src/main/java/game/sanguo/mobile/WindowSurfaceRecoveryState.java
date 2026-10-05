package game.sanguo.mobile;

/** One recovery per proven loss, not one per timer tick. Re-arm only on real backing readback. */
final class WindowSurfaceRecoveryState {
    boolean latched;long epoch,losses,relayouts;
    static boolean missingBacking(String message){return message!=null&&message.contains("Window")&&message.contains("backing surface");}
    boolean missing(){if(latched)return false;latched=true;losses++;relayouts++;return true;}
    void copied(){latched=false;}
    void invalidateCallbacks(){epoch++;}
    boolean accepts(long ticket){return epoch==ticket;}
}
