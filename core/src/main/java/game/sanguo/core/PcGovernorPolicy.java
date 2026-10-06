package game.sanguo.core;

import java.util.*;
import java.io.*;

/** Explicit fresh-source election strategy; unknown original campaign assignments remain bounded. */
public final class PcGovernorPolicy {
    static final String NAMESPACE="pc-governor-election-v1";
    static final int MAGIC=0x50474531;
    static final String RESOURCE_SHA="909254e0e7e8801e8e505e4c0be3c633f808101fcdc461c06759e4f82cfd8ef6";
    static final class Person {
        int nativeId,id,army,home,current,status; String sha; boolean allowed,mask,resident;
    }
    static final class Site {int nativeId,id,army,owner,winner;}
    static final class Source {
        String id,path,variant,sha,shared;
        final SortedMap<Integer,Person> people=new TreeMap<>();
        final SortedMap<Integer,Site> sites=new TreeMap<>();
        final Map<Integer,Integer> armyOwners=new TreeMap<>(),armyLeaders=new TreeMap<>(),armyDisplays=new TreeMap<>();
    }
    static Map<String,Source> sources;
    static synchronized Map<String,Source> sources()throws IOException {
        if(sources!=null)return sources;
        InputStream resource=PcGovernorPolicy.class.getResourceAsStream("/pc-governor-rosters/rosters.bin.gz");
        if(resource==null)throw new IOException("Governor original input absent");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=new java.util.zip.GZIPInputStream(resource)){
            byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(bytes.size()+n>2*1024*1024)throw new IOException("Governor input too large");bytes.write(block,0,n);}
        }
        byte[] raw=bytes.toByteArray();
        try{if(!PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)).equals(RESOURCE_SHA))throw new IOException("Governor input SHA differs");}
        catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));
        if(in.readInt()!=0x50474f31||!PcOfficerInfo.text(in,64).equals(PcScenarioIdentity.EXE_SHA))throw new IOException("Governor executable differs");
        for(int i=0;i<3;i++)PcOfficerInfo.text(in,64);
        if(in.readInt()!=16)throw new IOException("Governor source domain differs");
        Map<String,Source> all=new TreeMap<>();
        for(int s=0;s<16;s++){
            Source src=new Source();src.id=PcOfficerInfo.text(in,80);src.path=PcOfficerInfo.text(in,1024);src.variant=PcOfficerInfo.text(in,300);src.sha=PcOfficerInfo.text(in,64);src.shared=PcOfficerInfo.text(in,64);
            if(in.readInt()!=87)throw new IOException("Governor site domain differs");
            for(int j=0;j<87;j++){Site site=new Site();site.nativeId=in.readInt();site.id=in.readInt();site.army=in.readInt();site.owner=in.readInt();site.winner=in.readInt();if(site.nativeId!=j||src.sites.put(site.id,site)!=null)throw new IOException("Governor site join differs");}
            if(in.readInt()!=850)throw new IOException("Governor person domain differs");
            for(int j=0;j<850;j++){Person p=new Person();p.nativeId=in.readInt();p.id=in.readInt();p.sha=PcOfficerInfo.text(in,64);int[] v=new int[12];for(int k=0;k<12;k++)v[k]=in.readInt();p.army=v[1];p.home=v[2];p.current=v[3];p.status=v[4];p.allowed=v[9]!=0;p.resident=v[10]!=0;p.mask=v[11]!=0;if(p.nativeId!=j)throw new IOException("Governor native ordering differs");src.people.put(j,p);}
            if(in.readInt()!=47)throw new IOException("Governor army domain differs");
            for(int j=0;j<47;j++){if(in.readInt()!=j)throw new IOException("Governor army ordering differs");in.readInt();int owner=in.readInt();src.armyOwners.put(j,owner);src.armyDisplays.put(j,in.readInt());src.armyLeaders.put(j,in.readInt());for(int k=0;k<5;k++)in.readInt();}
            if(all.put(src.id,src)!=null)throw new IOException("Governor source duplicate");
        }
        if(in.available()!=0)throw new IOException("Governor input trailing bytes");return sources=Collections.unmodifiableMap(all);
    }
    static Source source(World w)throws IOException {
        PcScenarioIdentity.Source identity=PcScenarioIdentity.saved(w);Source src=identity==null?null:sources().get(identity.scenarioId);
        if(src==null||!src.path.equals(identity.path)||!src.variant.equals(identity.sourceVariant)||!src.sha.equals(identity.sha)||!src.shared.equals(identity.sharedSha))throw new IOException("Governor source identity differs");
        if(!w.pcSourceFrame||!PcScenarioIdentity.DATA_SOURCE.equals(w.dataSource)||!src.id.equals(w.scenarioId)||!src.sha.equals(w.dataHash))throw new IOException("Governor live identity differs");
        return src;
    }
    static final class Assignment {int home,army,lastCity;Assignment(int home,int army,int lastCity){this.home=home;this.army=army;this.lastCity=lastCity;}}
    static final class Data {Source source;long revision,identityRevision,peopleRevision;final SortedMap<Integer,Assignment> assignments=new TreeMap<>();final SortedMap<Integer,Integer> siteArmies=new TreeMap<>(),armyLeaders=new TreeMap<>(),unitArmies=new TreeMap<>();final SortedSet<Integer> unknownSites=new TreeSet<>();}
    static final Map<World,Data> caches=Collections.synchronizedMap(new WeakHashMap<>());
    static boolean recognized(World w){byte[] raw=w.extensions.get(NAMESPACE);return w.pcSourceFrame&&raw!=null&&raw.length>=4&&java.nio.ByteBuffer.wrap(raw).getInt()==MAGIC;}
    static void initializeOpening(World w)throws IOException {
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.extensions.get(NAMESPACE)!=null)throw new IOException("Governor policy requires explicit fresh source game");
        Source src=source(w);Data data=new Data();data.source=src;
        Map<Integer,PcScenarioPeople.Person> people=new HashMap<>();for(var p:PcScenarioPeople.saved(w))people.put(p.nativeId,p);
        for(Person p:src.people.values())if(p.id>=0){
            var original=people.get(p.nativeId);World.Officer o=w.officer(p.id);
            if(original==null||original.officerId!=p.id||!original.recordSha.equals(p.sha)||o==null)throw new IOException("Governor person record join differs");
            data.assignments.put(p.id,new Assignment(p.home,p.army,o.cityId));
            if(p.status==1&&o.owner>=0&&w.life.present(o.id))o.role=Strategy.Role.DISTRICT;
        }
        for(Site site:src.sites.values())data.siteArmies.put(site.id,site.army);
        data.armyLeaders.putAll(src.armyLeaders);
        write(w,data);reconcile(w,false);
    }
    static void write(World w,Data data)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(MAGIC);out.writeInt(1);for(String s:new String[]{data.source.id,data.source.sha,data.source.variant,data.source.shared,PcScenarioIdentity.EXE_SHA,RESOURCE_SHA})PcOfficerInfo.text(out,s);
        out.writeInt(data.assignments.size());for(var e:data.assignments.entrySet()){out.writeInt(e.getKey());out.writeInt(e.getValue().home);out.writeInt(e.getValue().army);out.writeInt(e.getValue().lastCity);}
        out.writeInt(data.siteArmies.size());for(var e:data.siteArmies.entrySet()){out.writeInt(e.getKey());out.writeInt(e.getValue());}
        out.writeInt(data.armyLeaders.size());for(var e:data.armyLeaders.entrySet()){out.writeInt(e.getKey());out.writeInt(e.getValue());}
        out.writeInt(data.unitArmies.size());for(var e:data.unitArmies.entrySet()){out.writeInt(e.getKey());out.writeInt(e.getValue());}
        out.writeInt(data.unknownSites.size());for(int id:data.unknownSites)out.writeInt(id);
        w.extensions.put(NAMESPACE,bytes.toByteArray());data.revision=w.extensions.revision(NAMESPACE);data.identityRevision=w.extensions.revision(PcScenarioIdentity.NAMESPACE);data.peopleRevision=w.extensions.revision(PcScenarioPeople.NAMESPACE);caches.put(w,data);
    }
    static Data read(World w)throws IOException {
        Data data=new Data();data.source=source(w);byte[] raw=w.extensions.get(NAMESPACE);
        if(raw==null||raw.length>65536)throw new IOException("Governor policy size differs");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readInt()!=MAGIC||in.readInt()!=1)throw new IOException("Governor policy version differs");
        for(String s:new String[]{data.source.id,data.source.sha,data.source.variant,data.source.shared,PcScenarioIdentity.EXE_SHA,RESOURCE_SHA})if(!PcOfficerInfo.text(in,300).equals(s))throw new IOException("Governor policy provenance differs");
        int count=PcOfficerInfo.bounded(in.readInt(),0,850),last=-1;
        for(int i=0;i<count;i++){int id=in.readInt(),home=in.readInt(),army=in.readInt(),lastCity=in.readInt();if(id<=last||w.officer(id)==null||home< -1||home>=87||army< -1||army>=47||lastCity< -1||lastCity>=0&&w.city(lastCity)==null)throw new IOException("Governor assignment differs");data.assignments.put(id,new Assignment(home,army,lastCity));last=id;}
        Set<Integer> expected=new TreeSet<>();Map<Integer,PcScenarioPeople.Person> joined=new HashMap<>();for(var p:PcScenarioPeople.saved(w))joined.put(p.nativeId,p);
        for(Person p:data.source.people.values())if(p.id>=0){var original=joined.get(p.nativeId);if(original==null||original.officerId!=p.id||!original.recordSha.equals(p.sha))throw new IOException("Governor saved person record differs");expected.add(p.id);}
        int siteCount=PcOfficerInfo.bounded(in.readInt(),87,87);last=-1;
        for(int i=0;i<siteCount;i++){int id=in.readInt(),army=in.readInt();if(id<=last||!data.source.sites.containsKey(id)||army< -1||army>=47)throw new IOException("Governor live site army differs");data.siteArmies.put(id,army);last=id;}
        if(!data.siteArmies.keySet().equals(data.source.sites.keySet()))throw new IOException("Governor live site coverage differs");
        if(in.readInt()!=47)throw new IOException("Governor army leader domain differs");
        for(int i=0;i<47;i++){int army=in.readInt(),leader=in.readInt();if(army!=i||leader< -1||leader>=1100)throw new IOException("Governor native army leader differs");data.armyLeaders.put(army,leader);}
        int units=PcOfficerInfo.bounded(in.readInt(),0,1000);last=-1;
        for(int i=0;i<units;i++){int id=in.readInt(),army=in.readInt();if(id<=last||army< -1||army>=47||w.unit(id)==null)throw new IOException("Governor source unit army differs");data.unitArmies.put(id,army);last=id;}
        int unknown=PcOfficerInfo.bounded(in.readInt(),0,1000);last=-1;
        for(int i=0;i<unknown;i++){int id=in.readInt();if(id<=last||w.city(id)==null)throw new IOException("Governor unknown site assignment differs");data.unknownSites.add(id);last=id;}
        if(in.available()!=0||!data.assignments.keySet().equals(expected))throw new IOException("Governor assignment coverage differs");
        data.revision=w.extensions.revision(NAMESPACE);data.identityRevision=w.extensions.revision(PcScenarioIdentity.NAMESPACE);data.peopleRevision=w.extensions.revision(PcScenarioPeople.NAMESPACE);return data;
    }
    static Data data(World w)throws IOException {
        Data data=caches.get(w);if(data==null||data.revision!=w.extensions.revision(NAMESPACE)||data.identityRevision!=w.extensions.revision(PcScenarioIdentity.NAMESPACE)||data.peopleRevision!=w.extensions.revision(PcScenarioPeople.NAMESPACE)) {data=read(w);caches.put(w,data);}return data;
    }
    static void validate(World w)throws IOException {
        if(recognized(w))caches.put(w,read(w));
        else for(World.Officer o:w.officers)if(o.role==Strategy.Role.DISTRICT)throw new IOException("District role requires recognized explicit source policy");
    }
    static void deployed(World w,World.Unit unit,World.City city){
        if(!recognized(w))return;
        try{Data data=data(w);int army=data.siteArmies.getOrDefault(city.id,-1);
            if(!Objects.equals(data.source.armyOwners.get(army),unit.owner))army=-1;
            for(World.Officer officer:w.army.crew(unit)){Assignment a=data.assignments.get(officer.id);if(a==null||a.army!=army)army=-1;}
            // A template/custom/mixed-army unit remains explicitly unknown.
            data.unitArmies.put(unit.id,army);write(w,data);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    static void joined(World w,World.Officer target,World.Officer actor,int cityId){
        if(!recognized(w))return;
        try{Data data=data(w);Assignment t=data.assignments.get(target.id),a=data.assignments.get(actor.id);Site site=data.source.sites.get(cityId);
            if(t==null)return;
            int previous=t.army,army=a==null?-1:a.army;
            if(!Objects.equals(data.source.armyOwners.get(army),target.owner))army=-1;
            t.army=army;if(site!=null)t.home=site.nativeId;t.lastCity=target.cityId;
            if(target.unitId>=0)data.unitArmies.put(target.unitId,army);
            if(previous!=army){reconcileArmy(w,data,previous);reconcileArmy(w,data,army);}
            write(w,data);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    static void unitRemoved(World w,int id){
        if(!recognized(w))return;
        try{Data data=data(w);if(data.unitArmies.remove(id)!=null)write(w,data);}
        catch(IOException e){throw new IllegalStateException(e);}
    }
    static void captured(World w,World.City city,World.Unit unit){
        if(!recognized(w))return;
        try{Data data=data(w);Integer army=data.unitArmies.get(unit.id);
            if(army==null||army<0||!Objects.equals(data.source.armyOwners.get(army),city.owner)){
                data.siteArmies.put(city.id,-1);data.unknownSites.add(city.id);
                w.note(city.name+"\u539f\u519b\u56e2\u5206\u914d\u672a\u77e5\uff0c\u4f7f\u7528\u73b0\u6709\u5de5\u7a0b\u592a\u5b88\u7b56\u7565");
            }else{data.siteArmies.put(city.id,army);data.unknownSites.remove(city.id);}
            write(w,data);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    /** Called only after an actual field/civil arrival, including return to the same station. */
    static void arrived(World w,World.Officer officer,World.City city){
        if(!recognized(w))return;
        try{
            Data data=data(w);Assignment a=data.assignments.get(officer.id);Site site=data.source.sites.get(city.id);
            if(a==null||site==null||officer.unitId>=0||officer.cityId!=city.id||city.owner!=officer.owner)return;
            // Captured/new army allocation remains unknown; do not synthesize primary army IDs.
            int army=data.siteArmies.get(site.id);
            if(!Objects.equals(data.source.armyOwners.get(army),officer.owner))return;
            if(a.home==site.nativeId&&a.army==army&&a.lastCity==city.id)return;
            int previousArmy=a.army;
            a.home=site.nativeId;a.army=army;a.lastCity=city.id;
            if(previousArmy!=army){reconcileArmy(w,data,previousArmy);reconcileArmy(w,data,army);}
            write(w,data);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    /** Original4cef90 ordering is distinct from city governor4cf160 ordering. */
    static int nativeRank(World w,World.Officer officer){Government.Rank rank=w.government.office(officer.id);return rank==null?80:rank.nativeId;}
    static boolean armyEarlier(World w,Person a,Person b){
        World.Officer x=w.officer(a.id),y=w.officer(b.id);
        if((x.role==Strategy.Role.RULER)!=(y.role==Strategy.Role.RULER))return x.role==Strategy.Role.RULER;
        int xc=w.government.commandLimit(x.id),yc=w.government.commandLimit(y.id);if(xc!=yc)return xc>yc;
        int xr=nativeRank(w,x),yr=nativeRank(w,y);if(xr!=yr)return xr<yr;
        if(x.leadership!=y.leadership)return x.leadership>y.leadership;return a.nativeId<b.nativeId;
    }
    static void reconcileArmy(World w,Data data,int army){
        if(army<0||army>=47)return;
        int owner=data.source.armyOwners.get(army);Person best=null;
        for(Person p:data.source.people.values())if(p.id>=0){World.Officer o=w.officer(p.id);Assignment a=data.assignments.get(p.id);
            if(a.army!=army||o.owner!=owner||owner<0||!w.life.present(o.id)||w.government.captive(o.id))continue;
            if(best==null||armyEarlier(w,p,best))best=p;
        }
        int oldNative=data.armyLeaders.get(army),nextNative=best==null?-1:best.nativeId;
        if(oldNative!=nextNative){Person previous=data.source.people.get(oldNative);World.Officer old=previous==null||previous.id<0?null:w.officer(previous.id);
            if(old!=null&&old.role==Strategy.Role.DISTRICT){World.City c=w.city(old.cityId);old.role=c!=null&&c.governorId==old.id&&w.governance.resident(old,c)?Strategy.Role.GOVERNOR:Strategy.Role.OFFICER;}
            data.armyLeaders.put(army,nextNative);
        }
        if(best!=null){World.Officer officer=w.officer(best.id);if(officer.role!=Strategy.Role.RULER)officer.role=Strategy.Role.DISTRICT;}
    }
    /** Source station/army assignment remains independent from appearance-home and delegated AP. */
    static void reconcile(World w,boolean announce)throws IOException {
        Data data=data(w);List<Candidate> candidates=new ArrayList<>();
        for(Person p:data.source.people.values())if(p.id>=0){World.Officer o=w.officer(p.id);Assignment a=data.assignments.get(p.id);World.City city=w.city(o.cityId);Site site=city==null?null:data.source.sites.get(city.id);
            boolean resident=city!=null&&w.governance.resident(o,city)&&o.otherTaskTurns==0&&!w.domestic.busy(o.id);
            int status=o.role==Strategy.Role.RULER?0:o.role==Strategy.Role.DISTRICT?1:o.role==Strategy.Role.GOVERNOR?2:3;
            candidates.add(new Candidate(p.nativeId,p.id,o.owner,a.home,site==null?-1:site.nativeId,a.army,status,w.government.commandLimit(o.id),o.leadership,o.war,w.government.merit(o.id),w.life.present(o.id)&&o.owner>=0,o.owner>=0&&w.life.present(o.id)&&!w.government.captive(o.id),resident));
        }
        Set<Integer> selected=new HashSet<>();
        for(Site site:data.source.sites.values()){World.City city=w.city(site.id);Candidate winner=elect(site.nativeId,city.owner,data.siteArmies.get(site.id),candidates);int id=winner==null?-1:winner.officerId;
            if(data.unknownSites.contains(city.id)){
                World.Officer legacy=w.officer(city.governorId);if(!w.governance.resident(legacy,city))legacy=null;
                if(legacy==null)for(World.Officer officer:w.officers)if(w.governance.resident(officer,city)&&(legacy==null||officer.politics>legacy.politics||officer.politics==legacy.politics&&officer.id<legacy.id))legacy=officer;
                id=legacy==null?-1:legacy.id;
            }
            if(city.governorId!=id){city.governorId=id;if(announce&&id>=0)w.note(city.name+"\u81ea\u52a8\u4efb\u547d"+w.officer(id).name+"\u4e3a\u592a\u5b88\uff08\u539f\u6765\u6e90\u9009\u4efb\u7b56\u7565\uff09");}if(id>=0)selected.add(id);
        }
        for(World.Officer o:w.officers){if(o.role==Strategy.Role.GOVERNOR&&!selected.contains(o.id))o.role=o.owner<0?Strategy.Role.UNAFFILIATED:Strategy.Role.OFFICER;
            if(selected.contains(o.id)&&o.role!=Strategy.Role.RULER&&o.role!=Strategy.Role.DISTRICT)o.role=Strategy.Role.GOVERNOR;}
    }

    public static final class ArmyFact {
        public final int nativeId,owner,display,leaderNativeId,leaderOfficerId,openingLeaderNativeId;
        ArmyFact(int id,int owner,int display,int leader,int runtime,int opening){nativeId=id;this.owner=owner;this.display=display;leaderNativeId=leader;leaderOfficerId=runtime;openingLeaderNativeId=opening;}
    }
    public static final class View {
        public final boolean enabled;public final List<ArmyFact> armies;
        public final Map<Integer,Integer> siteArmies,unitArmies,officerArmies,officerAdministrativeHomeNative,siteNativeIds;
        public final Set<Integer> unknownSites;
        View(boolean enabled,List<ArmyFact> armies,Map<Integer,Integer> sites,Map<Integer,Integer> units,Map<Integer,Integer> officers,Map<Integer,Integer> homes,Set<Integer> unknown,Map<Integer,Integer> nativeSites){
            this.enabled=enabled;this.armies=List.copyOf(armies);siteNativeIds=Collections.unmodifiableMap(new TreeMap<>(nativeSites));siteArmies=Collections.unmodifiableMap(new TreeMap<>(sites));unitArmies=Collections.unmodifiableMap(new TreeMap<>(units));officerArmies=Collections.unmodifiableMap(new TreeMap<>(officers));officerAdministrativeHomeNative=Collections.unmodifiableMap(new TreeMap<>(homes));unknownSites=Collections.unmodifiableSet(new TreeSet<>(unknown));
        }
    }
    /** Detached current administrative facts. No appointment, reconciliation, RNG or serialization. */
    public static View view(World w){
        if(!recognized(w))return new View(false,List.of(),Map.of(),Map.of(),Map.of(),Map.of(),Set.of(),Map.of());
        try{Data data=data(w);List<ArmyFact> armies=new ArrayList<>();Map<Integer,Integer> officers=new TreeMap<>(),homes=new TreeMap<>();
            for(int id=0;id<47;id++){int leader=data.armyLeaders.get(id);Person p=data.source.people.get(leader);armies.add(new ArmyFact(id,data.source.armyOwners.get(id),data.source.armyDisplays.get(id),leader,p==null?-1:p.id,data.source.armyLeaders.get(id)));}
            for(var e:data.assignments.entrySet()){officers.put(e.getKey(),e.getValue().army);homes.put(e.getKey(),e.getValue().home);}
            Map<Integer,Integer> nativeSites=new TreeMap<>();for(Site site:data.source.sites.values())nativeSites.put(site.id,site.nativeId);
            return new View(true,armies,data.siteArmies,data.unitArmies,officers,homes,data.unknownSites,nativeSites);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    private PcGovernorPolicy(){}
    static final class Candidate {
        final int nativeId,officerId,owner,home,current,army,status,capacity,leadership,war,merit;
        final boolean allowed,mask15,resident;
        Candidate(int nativeId,int officerId,int owner,int home,int current,int army,int status,
                  int capacity,int leadership,int war,int merit,boolean allowed,boolean mask15,boolean resident){
            this.nativeId=nativeId;this.officerId=officerId;this.owner=owner;this.home=home;this.current=current;
            this.army=army;this.status=status;this.capacity=capacity;this.leadership=leadership;this.war=war;this.merit=merit;
            this.allowed=allowed;this.mask15=mask15;this.resident=resident;
        }
    }
    static Candidate elect(int nativeSite,int owner,int army,List<Candidate> input){
        List<Candidate> candidates=new ArrayList<>();Set<Integer> seen=new HashSet<>();
        for(Candidate c:input){
            if(!seen.add(c.nativeId))throw new IllegalArgumentException("Duplicate original election identity");
            if(c.allowed&&c.mask15&&c.resident&&c.owner>=0&&c.owner<=41&&c.owner==owner
                    &&c.home==nativeSite&&c.army==army)candidates.add(c);
        }
        candidates.sort(Comparator.comparingInt(c->c.nativeId));
        Candidate priority=null;for(Candidate c:candidates)if(c.status<=1)priority=c;
        if(priority!=null)return priority;
        Candidate best=null;for(Candidate c:candidates)if(best==null||earlier(c,best))best=c;
        return best;
    }
    static boolean earlier(Candidate a,Candidate b){
        if(a.capacity!=b.capacity)return a.capacity>b.capacity;
        if(a.leadership!=b.leadership)return a.leadership>b.leadership;
        if(a.war!=b.war)return a.war>b.war;
        if(a.merit!=b.merit)return a.merit>b.merit;
        return a.nativeId<b.nativeId;
    }
}
