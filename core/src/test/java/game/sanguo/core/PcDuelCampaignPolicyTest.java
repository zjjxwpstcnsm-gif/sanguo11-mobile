package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Numerical model availability must not silently activate native gameplay. */
public final class PcDuelCampaignPolicyTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);
        check(!PcDuelCampaignPolicy.enabled(w),"fresh numerical facts do not enable unfinished duel settlement");byte[]before=SaveCodec.encode(w);World copy=SaveCodec.decode(before);check(Arrays.equals(before,SaveCodec.encode(copy)),"allWorld/allRNG absent capability preserved");
        copy.extensions.put(PcDuelCampaignPolicy.NAMESPACE,new byte[]{1,2,3,4});byte[]opaque=SaveCodec.encode(copy);World future=SaveCodec.decode(opaque);check(!PcDuelCampaignPolicy.enabled(future)&&Arrays.equals(opaque,SaveCodec.encode(future)),"unknown saved capability inert");
        future.extensions.put(PcDuelCampaignPolicy.NAMESPACE,new byte[]{0x50,0x44,0x55,0x31});boolean bad=false;try{SaveCodec.encode(future);}catch(IOException e){bad=true;}check(bad,"known malformed capability rejected");
        boolean missing=false;try{PcDuelCampaignPolicy.lastFinished(w);}catch(IOException e){missing=true;}check(missing&&Arrays.equals(before,SaveCodec.encode(w)),"missing settlement refuses without mutation");
        check(!w.contests.busy(),"no unsolicited native session");
        var src=PcScenarioIdentity.saved(w);var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(0x50445531);out.writeInt(1);out.writeUTF(src.scenarioId);out.writeUTF(src.sha);out.writeUTF(src.sourceVariant);out.writeInt(-1);
        World historical=SaveCodec.decode(before);historical.extensions.put(PcDuelCampaignPolicy.NAMESPACE,bytes.toByteArray());byte[]v1=SaveCodec.encode(historical);World v1copy=SaveCodec.decode(v1);check(PcDuelCampaignPolicy.read(v1copy).version==1&&PcDuelCampaignPolicy.options(v1copy)==null&&!v1copy.contests.nativeDuelConfigured(),"historical settlement v1 never creates new native command");check(Arrays.equals(v1,SaveCodec.encode(v1copy)),"v1 not upgraded by cold load");
        out=new DataOutputStream(new ByteArrayOutputStream());var old2=new ByteArrayOutputStream();out=new DataOutputStream(old2);out.writeInt(0x50445531);out.writeInt(2);out.writeUTF(src.scenarioId);out.writeUTF(src.sha);out.writeUTF(src.sourceVariant);out.writeInt(-1);for(int value:new int[]{0,0,0,1})out.writeInt(value);World prior=SaveCodec.decode(before);prior.extensions.put(PcDuelCampaignPolicy.NAMESPACE,old2.toByteArray());byte[]v2=SaveCodec.encode(prior);World priorCold=SaveCodec.decode(v2);check(PcDuelCampaignPolicy.read(priorCold).version==2&&PcGovernorPolicy.data(priorCold).format==1&&Arrays.equals(v2,SaveCodec.encode(priorCold)),"saved PDU2/PGO1 byte exact without merge adoption");
        boolean already=false;try{PcDuelCampaignPolicy.initializeOpening(v1copy,new PcDuelOptions(0,0,0));}catch(IOException e){already=true;}check(already&&Arrays.equals(v1,SaveCodec.encode(v1copy)),"existing v1 cannot be adopted through opening initializer");
        boolean futureRejected=false;try{PcDuelCampaignPolicy.initializeOpening(copy,new PcDuelOptions(0,0,0));}catch(IOException e){futureRejected=true;}check(futureRejected&&Arrays.equals(opaque,SaveCodec.encode(copy)),"opaque strategy cannot be overwritten");
        for(int life=0;life<4;life++)for(int death=0;death<3;death++)for(int difficulty=0;difficulty<3;difficulty++){
            World fresh=SaveCodec.decode(before);PcDuelCampaignPolicy.initializeOpening(fresh,new PcDuelOptions(life,death,difficulty));PcGovernorPolicy.initializeCurrentArmies(fresh);byte[]full=SaveCodec.encode(fresh);World cold=SaveCodec.decode(full);var options=PcDuelCampaignPolicy.options(cold);check(options.life==life&&options.death==death&&options.difficulty==difficulty&&cold.contests.nativeDuelConfigured(),"explicit selected options survive");check(Arrays.equals(full,SaveCodec.encode(cold)),"explicit fullWorld/bothRNG cold exact");
            byte[]policy=cold.extensions.get(PcDuelCampaignPolicy.NAMESPACE).clone();policy[policy.length-1]=2;cold.extensions.put(PcDuelCampaignPolicy.NAMESPACE,policy);boolean unknown=false;try{SaveCodec.encode(cold);}catch(IOException e){unknown=true;}check(unknown,"unknown presentation policy rejects without guessing");
        }
        World late=SaveCodec.decode(before);late.turn=1;boolean lateRejected=false;try{PcDuelCampaignPolicy.initializeOpening(late,new PcDuelOptions(0,0,0));}catch(IOException e){lateRejected=true;}check(lateRejected&&late.extensions.get(PcDuelCampaignPolicy.NAMESPACE)==null,"in-flight old world not adopted");
        System.out.println("PASS explicit native duel capability compatibility "+checks+" checks; ordinary fullnative gameplay still incomplete");
    }
}
