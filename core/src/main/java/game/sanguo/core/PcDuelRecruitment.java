package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Original4a92c0 then4a8440 ordinary serving-officer callback. This applies
 * an already admitted recruitment; it never substitutes for4af7d0/4afd60. */
final class PcDuelRecruitment {
    static int loyalty(World w,int id,int owner)throws IOException {
        var target=w.officer(id);var ruler=w.loyalty.ruler(owner);if(target==null||ruler==null)throw new IOException("原归属君主缺失");var t=PcDebateCampaignRules.source(w,id);var r=PcDebateCampaignRules.source(w,ruler.id);int gap=Loyalty.distance(target,ruler);if(gap<0)throw new IOException("原登用相性缺失");boolean sameFather=PcDuelRecruitmentAdmission.sameInternalFather(w,id,ruler.id);
        int raw=PcOfficerJoinRules.loyalty(PcDuelRawLoyalty.current(w,id),gap,target.honor-1,t.field(45),ruler.charm,false,false,w.relations.spouse(id)==ruler.id,w.relations.sworn(id,ruler.id),w.relations.likes(id,ruler.id),w.relations.dislikes(id,ruler.id),sameFather,PcDebateCampaignPolicy.sameHan(w,t.field(49),r.field(49)),t.field(41)==r.field(41));
        return raw;
    }
    static void validate(World w,World.Unit winner,World.Unit loser,int id)throws IOException {
        var o=w.officer(id);if(winner==null||loser==null||w.unit(winner.id)!=winner||w.unit(loser.id)!=loser||o==null||!w.life.present(id)||w.government.captive(id)||o.unitId!=loser.id||!w.army.contains(loser,id)||!w.campaign.hostile(winner.owner,loser.owner)||PcDuelRelease.busy(w,id))throw new IOException("原单挑登用人物或当前部队不同");
        if(o.role==Strategy.Role.RULER||o.role==Strategy.Role.DISTRICT||w.government.advisor(o.owner)==o||o.otherTaskTurns!=0||w.domestic.busy(id))throw new IOException("原君主/都督/军师或其他任务登用回调尚未闭合");
        var data=PcGovernorPolicy.data(w);var a=data.assignments.get(winner.officerId);if(!data.assignments.containsKey(id)||a==null||a.army<0||!Objects.equals(data.source.armyOwners.get(a.army),winner.owner))throw new IOException("原胜方军团连接缺失");var home=w.city(PcPersonnelReturnRules.projectSite(w,a.home));if(home==null||home.owner!=winner.owner)throw new IOException("原胜方行政驻点不同");
        PcDuelRelease.validateReturn(w,PcPersonnelReturnRules.cityAt(w,loser.hex),a.home);PcNativeItemPolicy.held(w,id);PcNativeItemPolicy.held(w,PcDuelRecruitItemPolicy.recipient(w,winner).id);loyalty(w,id,winner.owner);if(loser.officerId==id)PcDuelReplacement.select(w,loser,id);
    }
    static void apply(World w,World.Unit winner,World.Unit loser,int id)throws IOException {
        validate(w,winner,loser,id);int recipient=PcDuelRecruitItemPolicy.recipient(w,winner).id;int raw=loyalty(w,id,winner.owner),origin=PcPersonnelReturnRules.cityAt(w,loser.hex);var data=PcGovernorPolicy.data(w);var a=data.assignments.get(winner.officerId);int home=a.home,army=a.army,previous=data.assignments.get(id).army;var o=w.officer(id);
        for(var item:new ArrayList<>(w.treasures.held(id)))if(item.definition.kind.ordinal()==6||item.definition.kind.ordinal()==7)w.treasures.place(item.definition,Treasures.Place.OFFICER,recipient);
        PcDuelReplacement.remove(w,loser,id);w.strategy.releaseGovernor(id);w.government.allegianceChanged(id);o.owner=winner.owner;o.role=Strategy.Role.OFFICER;o.unitId=-1;o.cityId=PcPersonnelReturnRules.projectSite(w,origin);o.loyalty=Math.min(100,raw);o.lastRewardTurn=-1;o.acted=true;PcDuelRawLoyalty.originalWrite(w,id,raw);
        data=PcGovernorPolicy.data(w);var assigned=data.assignments.get(id);assigned.home=home;assigned.army=army;assigned.lastCity=o.cityId;PcGovernorPolicy.reconcileArmy(w,data,previous);PcGovernorPolicy.write(w,data);PcDuelRelease.startReturn(w,id,origin,home);w.governance.reconcile(false);
    }
    private PcDuelRecruitment(){}
}
