import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.*;
import game.sanguo.runtime.query.DeploymentQuery;
import java.util.*;

/** Host-only performance evidence; no timing assertions or device-performance claims. */
public final class DeploymentQueryBenchmark {
    public static void main(String[] args)throws Exception{
        World w=ScenarioCatalog.load("coalition-190",0,20261003L);World.City c=w.home();int leader=w.idle(c).get(0).id;
        try(GameSession s=new GameSession(w)){
            byte[] before=s.captureSave();StateToken token=s.state();long[] cached=new long[40],uncached=new long[40];
            long begin=System.nanoTime();s.preview(new DeploymentCommand(token,c.id,leader,new int[0],"SPEAR","BOAT",1000,5000,0));long first=System.nanoTime()-begin;
            for(int i=0;i<40;i++){
                DeploymentCommand command=new DeploymentCommand(token,c.id,leader,new int[0],"SPEAR","BOAT",1000+i*10,5000,0);
                begin=System.nanoTime();DeploymentPreview a=s.preview(command);cached[i]=System.nanoTime()-begin;
                begin=System.nanoTime();DeploymentPreview b=DeploymentQuery.capture(SaveCodec.decode(SaveCodec.encode(w)),token,command);uncached[i]=System.nanoTime()-begin;
                if(!a.allowed()||!b.allowed()||a.unit.attackRating!=b.unit.attackRating||a.unit.foodUse!=b.unit.foodUse||a.remaining.troops!=b.remaining.troops)throw new AssertionError("Query result divergence");
            }
            if(!Arrays.equals(before,s.captureSave())||!token.equals(s.state()))throw new AssertionError("Query changed authority/RNG/revision");
            Arrays.sort(cached);Arrays.sort(uncached);
            System.out.printf(Locale.ROOT,"PASS 40 cached vs independent full-copy queries; full save/RNG/revision unchanged. Host only. First %.3f ms; cached median %.3f p95 %.3f max %.3f ms; uncached median %.3f p95 %.3f max %.3f ms.%n",first/1e6,cached[20]/1e6,cached[37]/1e6,cached[39]/1e6,uncached[20]/1e6,uncached[37]/1e6,uncached[39]/1e6);
        }
    }
}
