package game.sanguo.mobile;

import game.sanguo.api.StateToken;
import java.nio.file.Files;
import java.nio.file.Path;

/** Full native caller vectors plus nullable directive rejection, no playback or authority mutation. */
public final class PcCommittedVoiceValidityTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        int rows=0;
        for(String row:Files.readAllLines(Path.of(args[0]))){
            String[] parts=row.split("\t");check(parts.length==11,"native vector extent");
            int[] v=new int[11];for(int i=0;i<v.length;i++)v[i]=Integer.parseInt(parts[i]);
            boolean valid=v[0]>=0&&v[0]<1100&&v[8]!=0&&PcVoicePolicy.actorValidFromRaw(v[6],v[7]);
            int voice=PcVoicePolicy.abilityVoice(v[1],v[2],valid,v[3],v[4],v[5],v[9]);
            check(voice==v[10],"full native validity/voice result at"+rows);rows++;
        }
        check(rows==4020,"all native invalid/silent/available cases included");
        // Explicit syntactic identity fixture; catalog provenance/playback not claimed.
        String hash="0".repeat(64);
        PortraitMediaIdentity identity=new PortraitMediaIdentity(10005,5,"fixture","fixture",hash,hash);
        StateToken state=new StateToken("fixture",9007199254740993L,9007199254740995L);
        Integer[][] facts={{null,null},{null,0},{9,null},{9,0},{-1,0},{0,null},{8,0},{null,1},{9,-1}};
        boolean[] allowed={false,false,false,false,false,true,true,true,true};
        for(int i=0;i<facts.length;i++)for(PcVoiceDirective.Selector selector:PcVoiceDirective.Selector.values()){
            boolean accepted=false;
            try{
                PcVoiceDirective directive=new PcVoiceDirective(state,"fact","parent","phase",identity,0,0,0,false,1,
                    selector,new int[]{50,50,50,50},facts[i][0],facts[i][1]);
                accepted=true;check(directive.state==state&&directive.actorStatusRaw==facts[i][0]&&directive.actor17cRaw==facts[i][1],"raw source validity remains immutable/nullable");
            }catch(IllegalArgumentException expected){}
            check(accepted==allowed[i],"nullable committed actor admission "+i+"/"+selector);
        }
        System.out.println("PASS committed voice native validity vectors="+rows+" checks="+checks);
    }
}
