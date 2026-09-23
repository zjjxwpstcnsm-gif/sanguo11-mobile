package game.sanguo.mobile;

import android.app.Instrumentation;
import game.sanguo.core.*;
import game.sanguo.runtime.TurnTicket;
import java.lang.reflect.*;
import java.util.function.Function;

/** Test adapter for the real session boundary; no alternate rules or success stubs. */
final class SessionProbe {
    static World view(MainActivity activity){
        try{Field f=MainActivity.class.getDeclaredField("world");f.setAccessible(true);return (World)f.get(activity);}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    static void install(MainActivity activity,World prepared){
        try{Method m=MainActivity.class.getDeclaredMethod("activateWorld",World.class);m.setAccessible(true);
            if(!Boolean.TRUE.equals(m.invoke(activity,prepared)))throw new AssertionError("Fixture install rejected");}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    static World.Result command(MainActivity activity,Function<World,World.Result> operation){
        World draft=view(activity);return activity.applyResult(draft,()->operation.apply(draft));
    }
    static TurnWork replay(Instrumentation probe,MainActivity activity,World before,World after){
        TurnWork[] result={null};probe.runOnMainSync(()->{
            try{
                install(activity,before);
                NativeGameHost host=((GameApplication)activity.getApplication()).host();
                TurnWork work=host.beginTurn(view(activity));
                Field f=TurnWork.class.getDeclaredField("ticket");f.setAccessible(true);
                if(!host.session().commitTurn((TurnTicket)f.get(work),after))throw new AssertionError("Replay fixture commit rejected");
                host.store().write("auto",host.capture());result[0]=work;
            }catch(Exception e){throw new IllegalStateException(e);}
        });return result[0];
    }
}
