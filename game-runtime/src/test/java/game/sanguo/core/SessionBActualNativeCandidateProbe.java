package game.sanguo.core;

import game.sanguo.runtime.GameSession;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;

/** Real turn18 save: candidate DTO order/values, immutability and full-state purity. */
public final class SessionBActualNativeCandidateProbe {
    public static void main(String[]args)throws Exception{
        Path p=Path.of(args[0]);byte[] raw=Files.readAllBytes(p);
        if(!PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(raw)).equals("06329224846382ae8cea34988458622ce13f8756c1912ac6f10f9e00e4fb2661"))throw new AssertionError("Exact actual Android checkpoint required");
        World w=SaveCodec.decode(raw);
        try(GameSession game=new GameSession(w)){
            byte[] before=game.captureSave();var token=game.state();
            if(w.contests.duelError(16,25)!=null)throw new AssertionError("Actual adjacent ordinary entry no longer legal");
            var candidates=w.contests.nativeDuelCandidates(16,25);
            if(candidates.isEmpty())throw new AssertionError("No actual candidate");
            for(var c:candidates)System.out.println("CANDIDATE stable="+c.officerId+" name="+c.name+" chance="+c.chance);
            try{candidates.clear();throw new AssertionError("Mutable candidates");}catch(UnsupportedOperationException expected){}
            try{candidates.set(0,null);throw new AssertionError("Mutable candidate slot");}catch(UnsupportedOperationException expected){}
            if(!token.equals(game.state())||!Arrays.equals(before,game.captureSave()))throw new AssertionError("Candidate query mutates entire World/RNG/StateToken");
            if(!Arrays.equals(raw,Files.readAllBytes(p)))throw new AssertionError("Actual original file changed");
            System.out.println("PASS actual normal candidate DTO/order/immutable/fullWorld-allRNG-StateToken purity; Host diagnosis, Android full terminal separate");
        }
    }
}
