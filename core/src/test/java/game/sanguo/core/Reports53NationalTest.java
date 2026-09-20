package game.sanguo.core;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.CRC32;

/** The new append-only history must not alter any of the v52 game-state/RNG goldens. */
public final class Reports53NationalTest {
    public static void main(String[] args)throws Exception{
        String[] expected={"1ae4521c16defb8c43250e35da3cc81019112816e6271e4f84b66c38b8e0826a","2374c4549c7ab176cfda91566486fad5e69da955c153b2cfda13d8b656e7438b","da27cfd72068a454ee5b43b196c68387b5451a3a0db83e4eafa4f92e2c5dce68","032a45c1054516902ea6fea62fdfd03e686bd3981f1b0881560e3d9cd5fa0851","d3cf0274fa0ed2b00d2a1fca56d43a02979783755cbb6700f0f56051b7c32d2a","ee828a3292c29b67ef3c78f5d5b0ffb3673ef55667a8e94c8e284502d78bd079"};
        World world=ScenarioCatalog.load("heroes-250",0,12345L);
        if(world.factions.length!=28)throw new AssertionError("expected actual 28 faction scenario");
        for(int i=0;i<expected.length;i++){
            world=SaveCodec.decode(SaveCodec.encode(world));long started=System.nanoTime();TurnJournal journal=new TurnJournal(world);
            World.Result result=world.nextTurn(p->{journal.checkpoint(p.phase);if(p.boundary)journal.drainEvents();});journal.close();if(!result.ok)throw new AssertionError(result.message);
            long millis=(System.nanoTime()-started)/1000000;byte[] save=SaveCodec.encode(world);
            ByteArrayOutputStream extension=new ByteArrayOutputStream();world.reports.write(new DataOutputStream(extension));
            byte[] payload=Arrays.copyOfRange(save,20,save.length-extension.size());CRC32 crc=new CRC32();crc.update(payload);
            ByteArrayOutputStream old=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(old);out.writeInt(0x53473131);out.writeInt(28);out.writeInt(payload.length);out.writeLong(crc.getValue());out.write(payload);
            String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(old.toByteArray()));
            if(!actual.equals(expected[i]))throw new AssertionError("gameplay/RNG changed at turn "+(i+1)+": "+actual);
            int count=world.reports.size();if(count<40)throw new AssertionError("national report history unexpectedly empty");
            if(SaveCodec.decode(save).reports.size()!=count)throw new AssertionError("national reports lost in save");
            System.out.printf(Locale.ROOT,"NATIONAL53 turn=%d computeMs=%d units=%d reports=%d saveBytes=%d gameplay_sha256=%s%n",i+1,millis,world.units.size(),count,save.length,actual);
        }
        System.out.println("NATIONAL53 PASS: six complete 250/28 turns preserve exact v52 game state and RNG, and all report history survives saves");
    }
}
