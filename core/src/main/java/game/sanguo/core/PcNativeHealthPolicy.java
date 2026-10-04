package game.sanguo.core;
import java.io.*;import java.util.*;

/** Explicit fresh-game saved health strategy; missing old saves remain legacy.
 * Mode3 recovery is verified separately from healthy-age illness/settings. */
final class PcNativeHealthPolicy {
    static final String NAMESPACE="pc-native-health-recovery-v1";
    private static final int MAGIC=0x50485231;
    private static final class State {int lastTurn=-1;final SortedMap<Integer,Integer> injuries=new TreeMap<>();}
    private static final class Cache {final byte[] bytes;final State state;Cache(byte[] bytes,State state){this.bytes=bytes.clone();this.state=state;}}
    private static final Map<World,Cache> caches=Collections.synchronizedMap(new WeakHashMap<>());
    static boolean enabled(World w){return w.extensions.get(NAMESPACE)!=null;}
    static void initializeOpening(World w)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||enabled(w)||PcOfficerCampaignFacts.saved(w).isEmpty())throw new IOException("Original recovery strategy requires explicit fresh source game");
        write(w,new State());
    }
    private static State read(World w)throws IOException{
        byte[] raw=w.extensions.get(NAMESPACE);if(raw==null||raw.length>32768)throw new IOException("Original recovery strategy missing/invalid");
        Cache cached=caches.get(w);if(cached!=null&&Arrays.equals(raw,cached.bytes))return cached.state;
        return parse(w,raw);
    }
    private static State mutable(World w)throws IOException{State prior=read(w),copy=new State();copy.lastTurn=prior.lastTurn;copy.injuries.putAll(prior.injuries);return copy;}
    private static State parse(World w,byte[] raw)throws IOException{
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);
        if(in.readInt()!=MAGIC||in.readInt()!=3||source==null||!in.readUTF().equals(source.scenarioId)||!in.readUTF().equals(source.sha)||!in.readUTF().equals(source.sourceVariant))throw new IOException("Original recovery source/mode differs");
        State s=new State();s.lastTurn=PcOfficerInfo.bounded(in.readInt(),-1,w.turn);int n=PcOfficerInfo.bounded(in.readInt(),0,w.officers.size());
        Map<Integer,PcContestProfiles.Fact> facts=PcContestProfiles.saved(w);
        for(int i=0;i<n;i++){int id=in.readInt(),injury=PcOfficerInfo.bounded(in.readUnsignedByte(),1,3);if(!facts.containsKey(id)||w.officer(id)==null||s.injuries.put(id,injury)!=null)throw new IOException("Original recovery participant reference differs");}
        if(in.available()!=0)throw new IOException("Original recovery unknown saved tail");caches.put(w,new Cache(raw,s));return s;
    }
    private static void write(World w,State s)throws IOException{
        PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);if(source==null)throw new IOException("Original recovery source missing");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeInt(3);out.writeUTF(source.scenarioId);out.writeUTF(source.sha);out.writeUTF(source.sourceVariant);out.writeInt(s.lastTurn);out.writeInt(s.injuries.size());for(Map.Entry<Integer,Integer> e:s.injuries.entrySet()){out.writeInt(e.getKey());out.writeByte(e.getValue());}w.extensions.put(NAMESPACE,bytes.toByteArray());
    }
    static int injury(World w,int id){if(!enabled(w))return 0;try{return read(w).injuries.getOrDefault(id,0);}catch(IOException e){throw new IllegalStateException(e);}}
    static int remainingTurns(World w,int id){if(!enabled(w))return 0;try{
        int severity=read(w).injuries.getOrDefault(id,0);if(severity==0)return 0;
        if(!w.life.present(id)||w.government.captive(id))return -1;
        PcContestProfiles.Fact f=PcContestProfiles.saved(w).get(id);PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);int days=source.day-1+w.turn*10;
        int nextMonth=(source.month+days/30)%12+1,gap=(30-days%30+9)/10;
        if((nextMonth&1)!=(f.nativeId&1))gap+=3;return gap+6*(severity-1);
    }catch(IOException e){throw new IllegalStateException(e);}}
    static void setInjury(World w,int id,int severity)throws IOException{
        if(severity<0||severity>3||!PcContestProfiles.saved(w).containsKey(id))throw new IOException("Original injury value/identity invalid");State s=mutable(w);if(severity==0)s.injuries.remove(id);else s.injuries.put(id,severity);write(w,s);w.officerAbilities.refresh(w.officer(id));
    }
    static void tick(World w){if(!enabled(w))return;try{
        State s=mutable(w);if(s.lastTurn==w.turn)return;s.lastTurn=w.turn;
        PcScenarioIdentity.Source source=PcScenarioIdentity.saved(w);int days=source.day-1+w.turn*10;
        if(days%30==0){int month=(source.month-1+days/30)%12+1;List<PcContestProfiles.Fact> ordered=new ArrayList<>(PcContestProfiles.saved(w).values());ordered.sort(Comparator.comparingInt(f->f.nativeId));int seed=PcNativeDebatePolicy.seed(w);
            for(PcContestProfiles.Fact f:ordered){int old=s.injuries.getOrDefault(f.officerId,0);if(old==0)continue;
                boolean present=w.life.present(f.officerId),allowed=present&&!w.government.captive(f.officerId);
                PcHealthRules.Recovery r=PcHealthRules.recover(f.nativeId,month,old,present,f.nativeId>=700&&f.nativeId<=799,allowed,seed);seed=r.seed;
                if(r.injury==0)s.injuries.remove(f.officerId);else s.injuries.put(f.officerId,r.injury);
            }
            PcNativeDebatePolicy.setSeed(w,seed);
        }
        write(w,s);
    }catch(IOException e){throw new IllegalStateException(e);}}
    static void validate(World w)throws IOException{if(enabled(w)){if(!PcNativeDebatePolicy.enabled(w))throw new IOException("Original recovery requires saved native RNG strategy");parse(w,w.extensions.get(NAMESPACE));}}
    private PcNativeHealthPolicy(){}
}
