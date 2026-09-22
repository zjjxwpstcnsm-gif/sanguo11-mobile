package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MapRevisionLedgerTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    interface IoAction{void run()throws IOException;}
    static void reject(IoAction a,String message)throws Exception{try{a.run();throw new AssertionError(message);}catch(IOException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        String id="3df0f771-29fd-4bc3-8b4d-94ff7e1f6067";
        MapRevisionLedger ledger=new MapRevisionLedger();
        check(ledger.reserve(id,1,0)==1,"first revision");check(ledger.deleted(id),"not visible until file committed");
        // Crash after reservation: the interrupted revision is never reallocated.
        ledger=MapRevisionLedger.decode(ledger.encode());check(ledger.reserve(id,1,0)==2,"crash consumes reserved identity");ledger.complete(id,2);
        check(!ledger.deleted(id),"completed publication visible");ledger.hide(id,2);
        ledger=MapRevisionLedger.decode(ledger.encode());check(ledger.deleted(id),"deletion persists");
        check(ledger.reserve(id,1,0)==3,"deleted identity cannot be reused");check(ledger.deleted(id),"failed republish cannot resurrect deleted files");ledger.complete(id,3);
        check(ledger.reserve(id,1,8)==9,"legacy/backup maximum included");ledger.complete(id,9);
        check(Arrays.equals(ledger.encode(),MapRevisionLedger.decode(ledger.encode()).encode()),"deterministic round trip");
        final MapRevisionLedger l=ledger;
        reject(()->l.reserve(id,0,0),"zero revision");reject(()->l.reserve("../map",1,0),"path-like ID");reject(()->l.complete(id,99),"unreserved completion");
        reject(()->MapRevisionLedger.decode(("sanguo-map-revisions-v1\n"+id+"=1,A\n"+id+"=2,A\n").getBytes(StandardCharsets.UTF_8)),"duplicate ID");
        reject(()->MapRevisionLedger.decode(new byte[]{(byte)0xc0,(byte)0xaf}),"malformed UTF-8");
        reject(()->MapRevisionLedger.decode("sanguo-map-revisions-v1".getBytes(StandardCharsets.UTF_8)),"torn index");
        MapRevisionLedger full=new MapRevisionLedger();full.reserve(id,1000000,0);reject(()->full.reserve(id,1,0),"revision exhaustion");
        System.out.println("PASS "+checks+" durable map revision checks");
    }
}
