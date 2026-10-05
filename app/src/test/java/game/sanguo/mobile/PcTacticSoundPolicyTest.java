package game.sanguo.mobile;

import java.nio.file.Files;
import java.nio.file.Path;

/** Independent original native callback vectors, including explicit unbound tactics. */
public final class PcTacticSoundPolicyTest {
    public static void main(String[] args)throws Exception{
        int checks=0,bound=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            String[] cells=line.split("\t");int action=Integer.parseInt(cells[0]),critical=Integer.parseInt(cells[1]),sound=Integer.parseInt(cells[2]);
            int actual=PcTacticSoundPolicy.choose(action,critical!=0);
            if(action>=0&&action<=2){if(actual!=sound)throw new AssertionError("source callback differs");bound++;}
            else if(actual!=-1)throw new AssertionError("unverified native action admitted");
            checks++;
        }
        if(checks!=8||bound!=6)throw new AssertionError("source vectors incomplete");
        for(int action:new int[]{-1,3,16,18,19,32,Integer.MAX_VALUE})for(boolean critical:new boolean[]{false,true}){
            if(PcTacticSoundPolicy.choose(action,critical)!=-1)throw new AssertionError("unknown action not silent");checks++;
        }
        System.out.println("PASS original spear sound policy checks="+checks+" boundVectors="+bound);
    }
}
