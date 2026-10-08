package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Ordinary current command creator; no terminal model/answer is injected.
 * Query and submit use the same ordered candidate and opponent rules. */
final class PcDuelChallenge {
    static String error(World w,int actor,int target,int nominee)throws IOException {
        var a=w.unit(actor);var b=w.unit(target);String error=w.orders.combatError(a);if(error!=null)return error;
        if(PcDuelCampaignPolicy.options(w)==null)return "当前存档没有明确新局原单挑策略";
        if(w.active!=w.player||w.contests.nextId>=10000000)return "当前不能发起玩家单挑";
        if(b==null||!w.campaign.hostile(a.owner,b.owner)||a.hex.distance(b.hex)!=1)return "请选择相邻交战部队";
        if(b instanceof Domestic.Mission)return "原运输队应战准入尚未核实";
        if(!w.inside(a.hex)||!w.inside(b.hex))return "当前部队原位置无效";
        if(b.status!=War.Status.NORMAL||w.army.water(a.hex)||w.army.water(b.hex)||Army.siegeWeapon(a.weapon)||Army.siegeWeapon(b.weapon))return "需要双方正常陆上非器械部队";
        if(!w.army.contains(a,nominee))return "请选择当前编队的上阵武将";
        PcDuelAdmissionRules.current(w,nominee);return null;
    }
    static List<PcDuelResponseRules.CandidateChance> menu(World w,int actor,int target)throws IOException {
        var a=w.unit(actor);if(a==null)throw new IOException("原单挑部队不存在");String error=error(w,actor,target,a.officerId);if(error!=null)throw new IOException(error);
        var options=PcDuelCampaignPolicy.options(w);return PcDuelResponseRules.currentOwnMenu(w,a,w.unit(target),PcDuelResponseRules.currentDrumSupport(w,a.officerId,w.unit(target).hex),new PcDuelKernel.Random(PcNativeDebatePolicy.seed(w)),options.settings());
    }
    static World.Result start(World w,int actor,int target,int nominee)throws IOException {
        String error=error(w,actor,target,nominee);if(error!=null)return w.fail(error);var a=w.unit(actor);var b=w.unit(target);var options=PcDuelCampaignPolicy.options(w);var settings=options.settings();var random=new PcDuelKernel.Random(PcNativeDebatePolicy.seed(w));
        var choices=PcDuelResponseRules.currentOwnMenu(w,a,b,PcDuelResponseRules.currentDrumSupport(w,nominee,b.hex),random,settings);if(choices.stream().noneMatch(p->p.officerId==nominee))return w.fail("上阵候补已变化");
        var selection=PcDuelResponseRules.currentOpponent(w,nominee,a,b,random,settings);
        // This saved policy deliberately focuses the confirmed command. Native
        // camera/frustum conditional parity is retained as an explicit gap.
        int voice=PcDuelCommandRules.speech(true,true,true,random);PcDuelCampaign duel=null;
        if(selection.officer>=0){var input=PcDuelEntryRules.prepareCurrent(w,a,b,nominee,selection.officer,settings);duel=PcDuelCampaign.initialize(input.officers,input.natives,input.manager,input.actors,input.held,random.state,settings);duel.commandBoundary(b.acted,voice);
            var runtime=PcDuelRuntimeFacts.saved(w);PcDuelCampaign.SupportBinding support=model->{var out=new PcDuelKernel.SupportFacts[2][3];for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){int id=input.officers[side*3+slot];out[side][slot]=id<0?new PcDuelKernel.SupportFacts(new int[14]):PcDuelBindings.supportCurrent(w,id,input.officers[side*3+model.activeIndex(side)],input.officers[(1-side)*3+model.activeIndex(1-side)],runtime);}return out;};duel.advance(input.actors,support,PcDuelKinship.terminal(w,input.officers),false,-1,-1);
        }
        PcDuelCommandRules.Refusal refusal=duel==null?PcDuelCommandRules.refusal(b.troops,true,true,false,random):null;
        w.marches.supersede(a);PcDuelCommandRules.consumeActor(w,a);
        if(duel==null){w.energy.change(a,refusal.ownEnergyDelta,EnergyRules.Reason.DUEL);w.energy.change(b,refusal.targetEnergyDelta,EnergyRules.Reason.DUEL);b.troops-=refusal.troopLoss;PcNativeDebatePolicy.setSeed(w,random.state);w.contests.lastResult=w.officer(b.officerId).name+"拒绝单挑；挑战方行动已消耗，对方损失"+refusal.troopLoss+"兵";return w.success(w.contests.lastResult);}
        w.contests.session=new Contests.Session(w.contests.nextId++,w.active,w.turn,actor,target,-1);w.contests.session.nativeDuel=duel;PcNativeDebatePolicy.setSeed(w,duel.state.random.state);duel.validate(w,w.contests.session);return w.success(w.officer(nominee).name+"开始原数值单挑");
    }
    private PcDuelChallenge(){}
}
