package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Current raw64 anchors are distinct from the immutable opening snapshot.
 * Only verified PDU3 death cleanup writes trusted anchors. Engineering
 * relationship changes invalidate trust without making the save unreadable. */
final class PcDuelSwornPolicy {
    static final String NAMESPACE="pc-duel-current-sworn-v1";
    private static final int MAGIC=0x50535731;
    static final class Row {
        final int anchor;boolean trusted;
        Row(int anchor,boolean trusted){this.anchor=anchor;this.trusted=trusted;}
    }
    static final class Plan {
        final SortedMap<Integer,Integer> anchors,loyalty;
        Plan(SortedMap<Integer,Integer> anchors,SortedMap<Integer,Integer> loyalty){this.anchors=anchors;this.loyalty=loyalty;}
    }
    private static SortedMap<Integer,Row> read(World w)throws IOException {
        var out=new TreeMap<Integer,Row>();byte[] bytes=w.extensions.get(NAMESPACE);if(bytes==null)return out;
        if(bytes.length>16384)throw new IOException("当前结义保存过大");
        var d=new DataInputStream(new ByteArrayInputStream(bytes));var identity=PcScenarioIdentity.saved(w);var runtime=PcDuelRuntimeFacts.saved(w);
        if(identity==null||runtime==null||!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3||d.readInt()!=MAGIC||d.readInt()!=1||!d.readUTF().equals(PcScenarioIdentity.EXE_SHA)||!d.readUTF().equals(identity.scenarioId)||!d.readUTF().equals(identity.sha)||!d.readUTF().equals(identity.sourceVariant))throw new IOException("当前结义保存来源或策略不同");
        int count=d.readInt();if(count<1||count>670)throw new IOException("当前结义保存人数无效");
        for(int i=0;i<count;i++){int nativeId=d.readInt(),anchor=d.readInt(),flag=d.readUnsignedByte();if(!runtime.people.containsKey(nativeId)||anchor< -1||anchor>=1100||anchor>=0&&!runtime.people.containsKey(anchor)||flag>1||out.put(nativeId,new Row(anchor,flag==1))!=null)throw new IOException("当前结义保存人物或锚点未知");}
        if(d.available()!=0)throw new IOException("当前结义保存尾部未知");return out;
    }
    private static void write(World w,SortedMap<Integer,Row> rows)throws IOException {
        if(rows.isEmpty())return;var identity=PcScenarioIdentity.saved(w);var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);
        d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(PcScenarioIdentity.EXE_SHA);d.writeUTF(identity.scenarioId);d.writeUTF(identity.sha);d.writeUTF(identity.sourceVariant);d.writeInt(rows.size());
        for(var entry:rows.entrySet()){d.writeInt(entry.getKey());d.writeInt(entry.getValue().anchor);d.writeBoolean(entry.getValue().trusted);}w.extensions.put(NAMESPACE,b.toByteArray());
    }
    private static int value(PcDuelRuntimeFacts.State runtime,Map<Integer,Row> rows,int nativeId)throws IOException {
        var person=runtime==null?null:runtime.people.get(nativeId);if(person==null)throw new IOException("当前结义原人物连接缺失");var row=rows.get(nativeId);if(row!=null&&!row.trusted)throw new IOException("当前结义编辑后的原锚点尚未核实");return row==null?person.sworn:row.anchor;
    }
    static int current(World w,int nativeId)throws IOException {return value(PcDuelRuntimeFacts.saved(w),read(w),nativeId);}
    static SortedMap<Integer,Integer> current(World w,PcDuelRuntimeFacts.State runtime)throws IOException {
        var rows=read(w);var out=new TreeMap<Integer,Integer>();for(int nativeId:runtime.people.keySet())out.put(nativeId,rows.containsKey(nativeId)?rows.get(nativeId).anchor:runtime.people.get(nativeId).sworn);return out;
    }
    static void unchangedGroup(World w,int id,PcDuelRuntimeFacts.State runtime,Map<Integer,PcDuelSourceFacts.Fact> facts)throws IOException {
        var f=facts.get(id);if(f==null)throw new IOException("当前结义稳定人物连接缺失");var rows=read(w);checkGroup(w,id,f.nativeId,runtime,rows,facts);
    }
    private static void checkGroup(World w,int id,int nativeId,PcDuelRuntimeFacts.State runtime,Map<Integer,Row> rows,Map<Integer,PcDuelSourceFacts.Fact> facts)throws IOException {
        int anchor=value(runtime,rows,nativeId);Set<Integer> expected=new TreeSet<>();
        if(anchor>=0){PcDuelRecruitmentAdmission.stable(facts,anchor);for(var member:runtime.people.values())if(member.nativeId!=nativeId&&(rows.containsKey(member.nativeId)?rows.get(member.nativeId).anchor:member.sworn)==anchor){if(rows.containsKey(member.nativeId)&&!rows.get(member.nativeId).trusted)throw new IOException("当前结义组含未核实的关系编辑");expected.add(PcDuelRecruitmentAdmission.stable(facts,member.nativeId));}}
        if(!expected.equals(w.relations.links(id,Relations.Kind.SWORN)))throw new IOException("当前义兄弟关系与原锚点不同");
    }
    static Plan death(World w,int id,int prospectiveHeir)throws IOException {
        var crown=prospectiveHeir<0?null:PcRulerCoronation.preview(w,PcRulerSuccession.preview(w,id).choose(prospectiveHeir));return death(w,id,crown);
    }
    static Plan death(World w,int id,PcRulerCoronation.Plan crown)throws IOException {
        int prospectiveHeir=crown==null?-1:crown.succession.selected;
        if(crown!=null&&crown.succession.ruler!=id)throw new IOException("原结义预览继承计划的死亡君主不同");
        if(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3)throw new IOException("当前存档未启用原死亡重整策略");
        var runtime=PcDuelRuntimeFacts.saved(w);var facts=PcDuelSourceFacts.saved(w);var f=facts.get(id);if(f==null||runtime==null)throw new IOException("原死亡结义人物连接缺失");
        var anchors=current(w,runtime);unchangedGroup(w,id,runtime,facts);int anchor=anchors.get(f.nativeId);var changes=new TreeMap<Integer,Integer>();var loyalty=new TreeMap<Integer,Integer>();if(anchor<0)return new Plan(changes,loyalty);
        PcDuelRecruitmentAdmission.stable(facts,anchor);var remaining=new ArrayList<Integer>();
        for(var entry:anchors.entrySet())if(entry.getKey()!=f.nativeId&&entry.getValue()==anchor){int member=PcDuelRecruitmentAdmission.stable(facts,entry.getKey());unchangedGroup(w,member,runtime,facts);remaining.add(entry.getKey());}
        if(remaining.size()>2)throw new IOException("原死亡结义人数超过已核实三人组");int next=-1;
        if(remaining.size()>=2){
            if(remaining.contains(anchor))next=anchor;
            else {for(int nativeId:remaining)if(w.life.age(PcDuelRecruitmentAdmission.stable(facts,nativeId))<0)throw new IOException("原死亡结义排序年龄未知");
                remaining.sort((a,b)->{int ai=stableUnchecked(facts,a),bi=stableUnchecked(facts,b);int c=Integer.compare(w.life.age(bi),w.life.age(ai));if(c==0)c=Boolean.compare(ruler(w,bi,prospectiveHeir),ruler(w,ai,prospectiveHeir));if(c==0)c=Integer.compare(w.government.merit(bi),w.government.merit(ai));return c==0?Integer.compare(a,b):c;});next=remaining.get(0);}
        }
        if(remaining.size()>=2){
            Set<Integer> owners=new TreeSet<>();for(int nativeId:remaining){var person=w.officer(PcDuelRecruitmentAdmission.stable(facts,nativeId));if(ruler(w,person.id,prospectiveHeir)&&person.owner>=0)owners.add(person.owner);}
            for(int nativeId:remaining){int member=PcDuelRecruitmentAdmission.stable(facts,nativeId);var person=w.officer(member);if(ruler(w,person.id,prospectiveHeir)||!owners.contains(person.owner))continue;
                if(!w.life.present(member)||w.government.captive(member))throw new IOException("原结义忠诚回调的未出场/俘虏有效状态尚未覆盖");
                int old=crown!=null&&crown.loyalty.containsKey(member)?crown.loyalty.get(member):PcDuelRawLoyalty.current(w,member);
                boolean future=crown!=null&&crown.succession.owner==person.owner;var reference=future?w.officer(prospectiveHeir):w.loyalty.ruler(person.owner);if(reference==null)throw new IOException("原结义忠诚回调的当前君主缺失");
                int charm=future?w.officerAbilities.currentWithoutRank(reference.id,4):reference.charm;loyalty.put(member,Math.max(old,loyalty(w,member,reference,old,charm)));
            }
        }
        changes.put(f.nativeId,-1);for(int member:remaining)changes.put(member,next);return new Plan(changes,loyalty);
    }
    private static boolean ruler(World w,int id,int prospective){return id==prospective||w.officer(id).role==Strategy.Role.RULER;}
    /** Original4ab770 runs after4b78f0. Read projected raw loyalty/rank80
     * charm without mutating either the ruler or the whole World in preview. */
    private static int loyalty(World w,int id,World.Officer ruler,int raw,int charm)throws IOException {
        var target=w.officer(id);var t=PcDebateCampaignRules.source(w,id);var r=PcDebateCampaignRules.source(w,ruler.id);int gap=Loyalty.distance(target,ruler);if(gap<0)throw new IOException("原结义忠诚相性缺失");boolean root=PcDuelRecruitmentAdmission.sameInternalFather(w,id,ruler.id);
        return PcOfficerJoinRules.loyalty(raw,gap,target.honor-1,t.field(45),charm,false,false,w.relations.spouse(id)==ruler.id,w.relations.sworn(id,ruler.id),w.relations.likes(id,ruler.id),w.relations.dislikes(id,ruler.id),root,PcDebateCampaignPolicy.sameHan(w,t.field(49),r.field(49)),t.field(41)==r.field(41));
    }
    private static int stableUnchecked(Map<Integer,PcDuelSourceFacts.Fact> facts,int nativeId){for(var f:facts.values())if(f.nativeId==nativeId)return f.officerId;throw new IllegalStateException("Validated native ID missing");}
    static void apply(World w,int id,Plan plan)throws IOException {
        if(plan.anchors.isEmpty())return;var rows=read(w);w.relations.unlink(id,-1,Relations.Kind.SWORN);
        for(var entry:plan.anchors.entrySet())rows.put(entry.getKey(),new Row(entry.getValue(),true));write(w,rows);
        for(var entry:plan.loyalty.entrySet()){int member=entry.getKey(),raw=entry.getValue();PcDuelRawLoyalty.invalidate(w,member);w.officer(member).loyalty=Math.min(100,raw);PcDuelRawLoyalty.originalWrite(w,member,raw);}
    }
    static void invalidate(World w,Collection<Integer> ids){
        if(w.extensions.get(NAMESPACE)==null)return;
        try{var rows=read(w);var facts=PcDuelSourceFacts.saved(w);boolean changed=false;for(int id:ids){var f=facts.get(id);var row=f==null?null:rows.get(f.nativeId);if(row!=null&&row.trusted){row.trusted=false;changed=true;}}if(changed)write(w,rows);}catch(IOException e){throw new IllegalStateException(e);}
    }
    static void validate(World w)throws IOException {
        if(w.extensions.get(NAMESPACE)==null)return;var rows=read(w);var runtime=PcDuelRuntimeFacts.saved(w);var facts=PcDuelSourceFacts.saved(w);
        for(var entry:rows.entrySet())if(entry.getValue().trusted)checkGroup(w,PcDuelRecruitmentAdmission.stable(facts,entry.getKey()),entry.getKey(),runtime,rows,facts);
    }
    private PcDuelSwornPolicy(){}
}
