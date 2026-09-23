package game.sanguo.runtime.bridge;

import game.sanguo.core.*;
import game.sanguo.api.bridge.BridgeEntity;
import game.sanguo.api.bridge.BridgeMessage;

import java.security.MessageDigest;
import java.util.*;

/** Same production scenario, same Java rules and save bytes through both entry paths. */
public final class BridgeSessionTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static String hash(World w)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w));
        return HexFormat.of().formatHex(bytes);
    }
    public static void main(String[] args)throws Exception{
        World direct=ScenarioCatalog.load("coalition-190",0,20260923L);
        World bridged=ScenarioCatalog.load("coalition-190",0,20260923L);
        check(hash(direct).equals(hash(bridged)),"seed baseline");
        BridgeSession session=new BridgeSession(bridged);
        session.snapshot();List<BridgeMessage> initial=session.drain();
        check(initial.size()==1&&initial.get(0).terrain.length()==bridged.width*bridged.height,"real full map");
        check(initial.get(0).entities.size()>=bridged.cities.size(),"real sites");
        World.City c=null;World.Officer officer=null;
        for(World.City site:bridged.cities)if(site.owner==bridged.active&&site.order<100&&!bridged.idle(site).isEmpty()){
            c=site;officer=bridged.idle(site).get(0);break;
        }
        check(c!=null,"real patrol entry");
        String before=hash(bridged);
        BridgeMessage rejected=session.command("bad",1,0,"patrol",c.id,-1);
        check(rejected.error!=null&&before.equals(hash(bridged)),"invalid officer leaves save unchanged");
        session.drain();
        World.Result old=direct.patrol(c.id,officer.id);
        BridgeMessage accepted=session.command("one",2,0,"patrol",c.id,officer.id);
        check(old.ok&&accepted.error==null,"both paths accept real command");
        check(hash(direct).equals(hash(bridged)),"canonical SaveCodec state and RNG bytes match");
        List<BridgeMessage> update=session.drain();
        check(update.stream().anyMatch(m->m.type.equals("delta")),"incremental change emitted");
        check(update.stream().anyMatch(m->m.type.equals("event")&&m.detail!=null),"confirmed Java event emitted");
        String after=hash(bridged);
        check(session.command("one",2,0,"patrol",c.id,officer.id).error==null,"idempotent receipt");
        check(after.equals(hash(bridged)),"duplicate cannot execute twice");
        check("CLIENT_SEQUENCE".equals(session.command("late",1,session.revision(),"patrol",c.id,officer.id).error),"out of order rejected");
        check("STALE_REVISION".equals(session.command("stale",3,0,"patrol",c.id,officer.id).error),"stale revision rejected");
        check(after.equals(hash(bridged)),"rejected commands leave rules and RNG unchanged");
        BridgeSession other=new BridgeSession(bridged);
        check(!other.sessionId.equals(session.sessionId),"session identity rotates");
        for(int i=0;i<BridgeSession.MAX_PENDING+1;i++)session.reject("flood:"+i,"TEST");
        check(session.drain().stream().anyMatch(m->m.type.equals("resync")),"bounded queue requires resync after overflow");
        check(session.droppedCount()>0,"overflow counted");
        System.out.println("BridgeSessionTest passed; actual scenario, rule parity, dedup, reorder and stale revision");
    }
}
