package game.sanguo.mobile;

import java.nio.file.Files;
import java.nio.file.Path;

/** Independent original native callback vectors, including explicit unbound tactics. */
public final class PcTacticSoundPolicyTest {
    public static void main(String[] args)throws Exception{
        int checks=0,bound=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            String[] cells=line.split("\t");int action=Integer.parseInt(cells[0]),critical=Integer.parseInt(cells[1]),sound=Integer.parseInt(cells[2]);
            int actual=PcTacticSoundPolicy.choose(action,true,critical!=0);
            if(action>=0&&action<=11){if(actual!=sound)throw new AssertionError("source callback differs");bound++;}
            else if(actual!=-1)throw new AssertionError("unverified native action admitted");
            checks++;
        }
        if(checks!=26||bound!=24)throw new AssertionError("source vectors incomplete");
        for(int action:new int[]{-1,12,13,14,15,16,17,18,19,32,Integer.MAX_VALUE})for(boolean critical:new boolean[]{false,true}){
            if(PcTacticSoundPolicy.choose(action,true,critical)!=-1)throw new AssertionError("unknown action not silent");checks++;
        }
        for(int action=0;action<=11;action++)for(boolean critical:new boolean[]{false,true}){
            if(PcTacticSoundPolicy.choose(action,false,critical)!=-1)throw new AssertionError("No applied primary hit fact must be silent");checks++;
        }
        System.out.println("PASS original gated infantry/cavalry sound policy checks="+checks+" boundVectors="+bound);
    }
}
