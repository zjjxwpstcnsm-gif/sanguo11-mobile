package game.sanguo.mobile;

import java.nio.file.Files;
import java.nio.file.Path;

/** Compares production policy to full source unit caller output with real original getters. */
public final class PcUnitVoicePolicyTest {
    public static void main(String[] args) throws Exception {
        int checks=0;
        for(String row:Files.readAllLines(Path.of(args[0]))){
            String[] c=row.split("\t");
            if(c.length!=7)throw new AssertionError("original unit caller vector extent");
            int[] v=new int[7];for(int i=0;i<7;i++)v[i]=Integer.parseInt(c[i]);
            int actual=PcVoicePolicy.abilityVoice(v[0],v[1],true,v[2],v[3],v[4],v[5]);
            if(actual!=v[6])throw new AssertionError("original unit caller mismatch at"+checks);
            checks++;
        }
        if(checks!=3990)throw new AssertionError("Incomplete full unit caller dispatches "+checks);
        System.out.println("PASS full unit caller voice comparisons="+checks);
    }
}
