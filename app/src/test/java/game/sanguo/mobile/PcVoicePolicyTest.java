package game.sanguo.mobile;

import java.nio.file.Files;
import java.nio.file.Path;

/** Compares production raw selectors against executed original x86 vectors, not generated mobile expectations. */
public final class PcVoicePolicyTest {
    public static void main(String[] args) throws Exception {
        int checks = 0;
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            String[] cells = line.split("\\t");
            if (cells.length != 10) throw new AssertionError("native vector extent");
            int[] v = new int[9];
            for (int i=0;i<v.length;i++) v[i]=Integer.parseInt(cells[i+1]);
            int result;
            switch (cells[0]) {
                case "4d1290": result=PcVoicePolicy.abilityVoice(v[0],v[1],v[2]!=0,v[4],v[5],v[6],v[7]); break;
                case "4d13b0": result=PcVoicePolicy.feedbackAVoice(v[0],v[1],v[2]!=0,v[3]); break;
                case "4d1490": result=PcVoicePolicy.feedbackBVoice(v[0],v[1],v[2]!=0,v[3]); break;
                default: throw new AssertionError("unknown native selector");
            }
            checks++;
            if (result != v[8]) throw new AssertionError("native vector " + checks + ": " + result + " != " + v[8]);
        }
        if (checks != 8240) throw new AssertionError("complete native row acceptance " + checks);
        if (PcVoicePolicy.abilityVoice(0,0,true,-1,50,50,50)!=-1 || PcVoicePolicy.abilityVoice(0,0,true,50,256,50,50)!=-1)
            throw new AssertionError("unrepresented current byte rejected");
        System.out.println("PASS original voice selector vectors="+checks+"; raw profile/type/feedback, no playback/event inference");
    }
}
