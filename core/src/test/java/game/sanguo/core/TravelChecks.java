package game.sanguo.core;

/** Advance only mission clocks when testing command outcomes independently of economy/AI. */
final class TravelChecks {
    static void arrive(World w){
        int remaining=100;
        while(w.envoys.missions().stream().anyMatch(m->m.remaining>m.travel)){
            if(--remaining<0)throw new AssertionError("envoy failed to arrive");w.envoys.tick();w.strategy.tick();
        }
        SaveCheck.validate(w);
    }
    static void complete(World w){
        int remaining=101;
        while(!w.envoys.missions().isEmpty()||!w.recruitment.missions().isEmpty()){
            if(--remaining<0)throw new AssertionError("mission failed to return");w.envoys.tick();w.recruitment.tick();w.strategy.tick();
        }
        SaveCheck.validate(w);
    }
    private static class SaveCheck {static void validate(World w){try{SaveCodec.validate(w);}catch(java.io.IOException e){throw new AssertionError(e);}}}
}
