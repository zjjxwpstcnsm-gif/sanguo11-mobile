package game.sanguo.mobile;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class SceneRenderGateTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        List<Boolean> transitions=new ArrayList<>();SceneRenderGate gate=new SceneRenderGate(transitions::add);
        check(gate.active(),"foreground default");
        for(int i=0;i<20;i++){
            gate.focused(false);check(!gate.active(),"dialog covers main window");
            gate.resumed(false);gate.resumed(true);check(!gate.active(),"Activity resume must not restart covered map");
            gate.focused(true);check(gate.active(),"dialog dismissal restores foreground renderer");
        }
        check(transitions.size()==40,"exactly one stop/start per dialog, duplicate callbacks are coalesced");
        gate.resumed(false);gate.focused(false);gate.focused(true);check(!gate.active(),"focus return while Activity paused is not a resume");
        gate.resumed(true);gate.visible(false);gate.focused(false);gate.focused(true);check(!gate.active(),"hidden preview stays stopped");
        gate.visible(true);check(gate.active(),"visible focused owner resumes");
        for(int mask=0;mask<8;mask++){
            gate.resumed((mask&1)!=0);gate.focused((mask&2)!=0);gate.visible((mask&4)!=0);
            check(gate.active()==(mask==7),"all lifecycle conjunctions "+mask);
        }
        AtomicReference<Throwable> error=new AtomicReference<>();Thread other=new Thread(()->{try{gate.focused(false);}catch(Throwable e){error.set(e);}});other.start();other.join();
        check(error.get() instanceof IllegalStateException,"foreign thread rejected");check(gate.active(),"foreign thread cannot mutate gate");
        gate.close();int closedTransitions=transitions.size();
        gate.resumed(true);gate.visible(true);gate.focused(true);gate.close();
        check(!gate.active()&&transitions.size()==closedTransitions,"released host never resurrects on late window callbacks");
        System.out.println("PASS SceneRenderGate "+checks+" assertions; 20 modal cycles, lifecycle matrix, terminal release, owner confinement");
    }
}
