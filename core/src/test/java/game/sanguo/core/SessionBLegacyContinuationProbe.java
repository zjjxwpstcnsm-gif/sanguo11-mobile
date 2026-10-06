package game.sanguo.core;

import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Compare actual inherited saves using the same runner with baseline/candidate rule classes. */
public final class SessionBLegacyContinuationProbe {
    private static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static String extensions(World w)throws Exception{
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        w.extensions.write(new java.io.DataOutputStream(bytes));return sha(bytes.toByteArray());
    }
    public static void main(String[] args)throws Exception{
        List<String> fixtures=List.of(
            "core/src/test/resources/save-v32-central-native.sg11",
            "core/src/test/resources/pre-base-construction-v33.sg11",
            "core/src/test/resources/pre-atomic-ship-cargo-v33.sg11",
            "game-runtime/src/test/resources/architecture/prepared-9548bb35-v33.sg11",
            "core/src/test/resources/pre-merchant-r25-v34.sg11",
            "core/src/test/resources/legacy-market-v35/host/coalition-190.sg11",
            "core/src/test/resources/legacy-production-v36/coalition-190-host.sg11",
            "docs/handoff/20261004/session2/six-turn-authority-29/authority-before.sg11",
            "docs/handoff/20261004/session1/batch19-actual-art-mid.sg11");
        System.out.println("path\tinput_version\tinput_sha\tphase\tresult\twhole_save_sha\tcampaign_rng\tpolicy_sha");
        for(String name:fixtures){
            if(args.length>2&&!name.equals(args[2]))continue;
            byte[] raw=Files.readAllBytes(Path.of(args[0]).resolve(name));int version=ByteBuffer.wrap(raw).getInt(4);
            World w=SaveCodec.decode(raw);byte[] canonical=SaveCodec.encode(w);
            if(!Arrays.equals(canonical,SaveCodec.encode(SaveCodec.decode(canonical))))throw new AssertionError("Full save/RNG roundtrip "+name);
            String prefix=name+"\t"+version+"\t"+sha(raw)+"\t";
            System.out.println(prefix+"decode\ttrue\t"+sha(canonical)+"\t"+w.strategy.getRandomState()+"\t"+extensions(w));
            for(int turn=0;turn<3;turn++){
                if(w.commandsBlocked()){System.out.println(prefix+"blocked-current-contest-or-succession\tfalse\t"+sha(SaveCodec.encode(w))+"\t"+w.strategy.getRandomState()+"\t"+extensions(w));break;}
                World.Result result=w.nextTurn();byte[] after=SaveCodec.encode(w);World restored=SaveCodec.decode(after);
                if(!Arrays.equals(after,SaveCodec.encode(restored)))throw new AssertionError("Whole postturn save/RNG "+name);
                if(args.length>1){Path out=Path.of(args[1]);Files.createDirectories(out);Files.write(out.resolve("turn"+(turn+1)+".sg11"),after);}
                w=restored;System.out.println(prefix+"turn"+(turn+1)+"\t"+result.ok+"\t"+sha(after)+"\t"+w.strategy.getRandomState()+"\t"+extensions(w));
                if(!result.ok)break;
            }
        }
        System.out.println("UNKNOWN\t31/38\tno genuine inherited historical fixture; no fabricated version headers; APK and custom-editor workflows remain separate");
    }
}
