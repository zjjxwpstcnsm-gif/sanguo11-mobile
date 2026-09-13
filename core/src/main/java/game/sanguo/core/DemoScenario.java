package game.sanguo.core;

import java.io.IOException;

/** Compatibility entry point for the original M0 engineering fixture. */
public final class DemoScenario {
    private DemoScenario() {}
    public static World create() {
        try{return ScenarioCatalog.load("m0-skirmish",0);}
        catch(IOException e){throw new IllegalStateException("内置基础演练数据损坏",e);}
    }
}
