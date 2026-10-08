package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Original4a9120 then4acbe0 ordinary/supported ruler branches. Native unit removal
 * precedes lifecycle, so engineering replacement/cargo rules cannot run. */
final class PcDuelExecution {
    static void validate(World w,World.Unit winner,World.Unit loser,int target)throws IOException {
        validate(w,winner,loser,target,-1);
    }
    static void validate(World w,World.Unit winner,World.Unit loser,int target,int heir)throws IOException {
        var o=w.officer(target);var facts=PcDuelSourceFacts.saved(w);if(winner==null||loser==null||w.unit(winner.id)!=winner||w.unit(loser.id)!=loser||o==null||!w.life.present(target)||w.government.captive(target)||!facts.containsKey(target)||o.unitId!=loser.id||!w.army.contains(loser,target)||!w.campaign.hostile(winner.owner,loser.owner))throw new IOException("原单挑处斩当前人物或部队不同");
        if(o.role==Strategy.Role.DISTRICT||!currentDeath(w)&&w.government.advisor(o.owner)==o)throw new IOException("原都督/旧军师处斩的额外军团回调尚未闭合");
        var crown=o.role==Strategy.Role.RULER?PcRulerCoronation.validateExecution(w,target,heir):null;
        if(w.life.life(target)==null||o.otherTaskTurns!=0||w.domestic.busy(target)||PcDuelRelease.busy(w,target))throw new IOException("原处斩人物存在未闭合的其他任务");
        if(!PcGovernorPolicy.recognized(w)||!PcGovernorPolicy.data(w).assignments.containsKey(target))throw new IOException("原处斩人物行政连接缺失");
        PcNativeItemPolicy.held(w,target);PcNativeItemPolicy.held(w,winner.officerId);if(loser.officerId==target&&PcDuelReplacement.select(w,loser,target)<0){if(currentDeath(w))PcDuelEscortRelease.plan(w,loser);else if(!w.government.escorted(loser.id).isEmpty())throw new IOException("原处斩押送部队的其他俘虏回调尚未闭合");}
        // Original4ab9a0 also rewrites sworn-group anchors; public symmetric
        // links alone cannot encode that change. Keep this branch explicit.
        if(currentDeath(w)){
            PcDuelSwornPolicy.death(w,target,crown);
            // Original4aa680 enumerates remaining held items (4cdf40), not
            // recruitment-refusal or revenge records. Full source493 death
            // leaves child133's parent/refusal fields unchanged as it inherits.
        }
    }
    static void apply(World w,World.Unit winner,World.Unit loser,int target)throws IOException {
        apply(w,winner,loser,target,-1);
    }
    static void apply(World w,World.Unit winner,World.Unit loser,int target,int heir)throws IOException {
        apply(w,winner,loser,target,heir,false,-1);
    }
    /** Natural caller validates its separate admission and item callback
     * before entering the shared unit/administration/lifecycle cleanup. */
    static void applyNatural(World w,World.Unit winner,World.Unit loser,int target,int recipient,int heir)throws IOException {
        apply(w,winner,loser,target,heir,true,recipient);
    }
    private static void apply(World w,World.Unit winner,World.Unit loser,int target,int heir,boolean natural,int recipient)throws IOException {
        validate(w,winner,loser,target,heir);var o=w.officer(target);var crown=o.role==Strategy.Role.RULER?PcRulerCoronation.validateExecution(w,target,heir):null;var admin=PcGovernorPolicy.data(w);var assigned=admin.assignments.get(target);int previousArmy=assigned.army;var sworn=currentDeath(w)?PcDuelSwornPolicy.death(w,target,crown):null;
        if(natural&&!w.treasures.held(target).isEmpty()&&recipient<0)throw new IOException("原阵亡携物接收者缺失，完整终局已保留");
        if(!natural)for(var item:new ArrayList<>(w.treasures.held(target)))w.treasures.place(item.definition,Treasures.Place.OFFICER,winner.officerId);
        PcDuelReplacement.remove(w,loser,target);o.unitId=-1;o.cityId=-1;
        if(crown!=null)PcRulerCoronation.apply(w,crown);
        if(natural)for(var item:new ArrayList<>(w.treasures.held(target)))w.treasures.place(item.definition,Treasures.Place.OFFICER,recipient);
        // Original4acae0 clears merit, ability experience and all incoming
        // likes/dislikes/spouse references while retaining the parent graph.
        if(currentDeath(w)){
            PcDuelSwornPolicy.apply(w,target,sworn);
            for(var person:w.officers){if(w.relations.likes(person.id,target))w.relations.unlink(person.id,target,Relations.Kind.LIKE);if(w.relations.dislikes(person.id,target))w.relations.unlink(person.id,target,Relations.Kind.DISLIKE);if(w.relations.spouse(person.id)==target)w.relations.unlink(person.id,target,Relations.Kind.SPOUSE);}
            w.government.merits.remove(target);if(o.abilityProfile!=null)Arrays.fill(o.abilityProfile.experience,0);
        }
        admin=PcGovernorPolicy.data(w);assigned=admin.assignments.get(target);
        assigned.army=-1;assigned.home=-1;assigned.lastCity=-1;PcGovernorPolicy.write(w,admin);
        w.life.die(target,natural?"单挑阵亡":"单挑终局处斩");PcDuelRawLoyalty.originalWrite(w,target,0);
        admin=PcGovernorPolicy.data(w);PcGovernorPolicy.reconcileArmy(w,admin,previousArmy);PcGovernorPolicy.write(w,admin);w.governance.reconcile(false);
        // Full4b78f0 explicitly assigns even a deployed ruler at its home.
        // Do not let the extra engineering global election undo that endpoint.
        if(crown!=null){var heirOfficer=w.officer(crown.succession.selected);var home=w.city(crown.homeCity);if(PcGovernorPolicy.deployedRulerGovernor(w,heirOfficer,home)){var old=w.officer(home.governorId);if(old!=null&&old.id!=heirOfficer.id&&old.role==Strategy.Role.GOVERNOR)old.role=Strategy.Role.OFFICER;home.governorId=heirOfficer.id;}}
    }
    private static boolean currentDeath(World w)throws IOException {return PcDuelCampaignPolicy.enabled(w)&&PcDuelCampaignPolicy.read(w).version==3;}
    private PcDuelExecution(){}
}
