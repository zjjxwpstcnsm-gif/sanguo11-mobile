package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Current4af7d0/5c4f80 field serving-officer branch. All reads are pure;
 * only decision owns one native percentage draw after the forced gate. */
final class PcDuelRecruitmentAdmission {
    static final class Plan {
        final int forced,probability,honor;Plan(int forced,int probability,int honor){this.forced=forced;this.probability=probability;this.honor=honor;}
        boolean decision(PcDuelKernel.Random random){return forced>=0?forced!=0:PcDuelRecruitmentRules.decision(probability,honor,random);}
    }
    static int stable(Map<Integer,PcDuelSourceFacts.Fact>facts,int nativeId)throws IOException {
        if(nativeId<0)return -1;for(var f:facts.values())if(f.nativeId==nativeId)return f.officerId;throw new IOException("原登用关系引用的人物激活/身份尚未覆盖："+nativeId);
    }
    static void unchangedGroup(World w,int id,PcDuelRuntimeFacts.State runtime,Map<Integer,PcDuelSourceFacts.Fact>facts)throws IOException {
        PcDuelSwornPolicy.unchangedGroup(w,id,runtime,facts);
    }
    static boolean sameInternalFather(World w,int first,int second)throws IOException {
        var facts=PcDuelSourceFacts.saved(w);var runtime=PcDuelRuntimeFacts.saved(w);if(runtime==null||first==second)return false;
        for(int id:new int[]{first,second}){var source=PcDebateCampaignRules.source(w,id);int nativeParent=source.field(12),original=-1;for(var f:facts.values())if(f.nativeId==nativeParent)original=f.officerId;if(w.relations.parent(id,false)!=original)throw new IOException("当前父亲编辑的原内部父系变更尚未核实");}
        var a=runtime.people.get(facts.get(first).nativeId);var b=runtime.people.get(facts.get(second).nativeId);return a.root>=0&&a.root<1100&&a.root==b.root;
    }
    static Plan preview(World w,int targetId,int actorId,int mode)throws IOException {
        var actor=w.officer(actorId);if(actor==null)throw new IOException("原战场登用当前出使者无效");return preview(w,targetId,actorId,mode,actor.charm);
    }
    static Plan preview(World w,int targetId,int actorId,int mode,int currentCharm)throws IOException {
        if(mode!=1&&mode!=2)throw new IOException("原战场登用模式无效");var target=w.officer(targetId);var actor=w.officer(actorId);var ruler=actor==null?null:w.loyalty.ruler(actor.owner);if(target==null||actor==null||ruler==null||!w.life.present(targetId)||!w.life.present(actorId)||w.government.captive(targetId)||target.owner<0)throw new IOException("原战场在职登用当前人物无效");var facts=PcDuelSourceFacts.saved(w);var t=facts.get(targetId);var a=facts.get(actorId);var r=facts.get(ruler.id);var runtime=PcDuelRuntimeFacts.saved(w);if(t==null||a==null||r==null||runtime==null)throw new IOException("原战场登用稳定人物连接缺失");unchangedGroup(w,targetId,runtime,facts);var tp=runtime.people.get(t.nativeId);var oldRuler=w.loyalty.ruler(target.owner);var old=oldRuler==null?null:facts.get(oldRuler.id);if(oldRuler!=null&&old==null)throw new IOException("原旧君主身份未覆盖");
        var g=new PcDuelRecruitmentRules.Gate();g.mode=mode;g.targetNative=t.nativeId;g.actorNative=a.nativeId;g.rulerNative=r.nativeId;g.targetForce=target.owner;g.rulerForce=ruler.owner;g.ruler=target.role==Strategy.Role.RULER;g.refusedRuler=PcRecruitmentBanPolicy.current(w,targetId).ruler;g.oldRuler=old==null?-1:old.nativeId;
        var anchors=PcDuelSwornPolicy.current(w,runtime);int sworn=anchors.get(t.nativeId);if(sworn>=0&&sworn<1100){int id=stable(facts,sworn);var person=w.officer(id);g.swornValid=person!=null;g.swornNative=sworn;g.swornForce=person==null?-1:person.owner;for(var member:runtime.people.values())if(member.nativeId!=tp.nativeId&&anchors.get(member.nativeId)==sworn){var o=w.officer(stable(facts,member.nativeId));if(o!=null){g.swornProperty75Old|=o.owner==target.owner;g.swornProperty75New|=o.owner==ruler.owner;}}}
        int spouseId=w.relations.spouse(targetId);if(spouseId>=0){var sf=facts.get(spouseId);if(sf==null)throw new IOException("当前原配偶身份未覆盖");var spouse=w.officer(spouseId);g.spouseValid=spouse!=null;g.spouseNative=sf.nativeId;g.spouseForce=spouse==null?-1:spouse.owner;}
        g.dislikesRuler=w.relations.dislikes(targetId,ruler.id);g.dislikesActor=w.relations.dislikes(targetId,actorId);g.likesOldRuler=oldRuler!=null&&w.relations.likes(targetId,oldRuler.id);g.likesNewRuler=w.relations.likes(targetId,ruler.id);int forced=PcDuelRecruitmentRules.forced(g);if(forced>=0)return new Plan(forced,0,target.honor-1);
        int family=0;if(old!=null){int parent=w.relations.parent(targetId,false),mother=w.relations.parent(targetId,true);family=parent==oldRuler.id||mother==oldRuler.id||w.relations.parent(oldRuler.id,false)==targetId||w.relations.parent(oldRuler.id,true)==targetId?15:0;}
        int raw=PcDuelRawLoyalty.current(w,targetId),oldGap=oldRuler==null?25:Loyalty.distance(target,oldRuler),newGap=Loyalty.distance(target,ruler);if(oldGap<0||newGap<0)throw new IOException("原战场登用当前相性未覆盖");int probability=PcDuelRecruitmentRules.probability(raw,target.honor-1,mode,oldGap,newGap,currentCharm,0,family,g.likesOldRuler?15:0,oldRuler!=null&&w.relations.dislikes(targetId,oldRuler.id)?15:0,a.nativeId,t.nativeId,r.nativeId,0);return new Plan(-1,probability,target.honor-1);
    }
    private PcDuelRecruitmentAdmission(){}
}
