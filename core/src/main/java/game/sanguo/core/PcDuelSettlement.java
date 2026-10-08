package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Original4d3110/4d3260/4d3340 numeric and distinct casualty callbacks.
 * Unsupported administrative/item branches retain the complete pending state. */
final class PcDuelSettlement {
    static final class Plan {
        final int winner,seed,draws,loserTroopLoss,capturedId;
        final int[] merit=new int[6],warXp=new int[6],energy=new int[2];
        final Map<Integer,Integer> health=new TreeMap<>(),injury=new TreeMap<>();
        Plan(int winner,int seed,int draws,int loss,int captured){this.winner=winner;this.seed=seed;this.draws=draws;loserTroopLoss=loss;capturedId=captured;}
    }
    static Plan preview(PcDuelModelSave.State state,int[]troops,int[]capacity)throws IOException {
        return preview(state,troops,capacity,false,false);
    }
    private static Plan preview(PcDuelModelSave.State state,int[]troops,int[]capacity,boolean allowCapture,boolean removedLoser)throws IOException {
        PcDuelModelSave.validate(state);if(troops.length!=2||troops[0]<0||troops[1]<0||troops[0]>65535||troops[1]>65535)throw new IOException("原单挑部队兵力输入无效");
        if(capacity.length!=2||capacity[0]<1||capacity[1]<1||capacity[0]>65535||capacity[1]>65535)throw new IOException("原單挑目前統兵上限未知");
        byte[]m=state.manager;int winner=PcDuelKernel.readManager(m,0x54),loser=PcDuelKernel.readManager(m,0x58);
        if(winner< -1||winner>1||winner<0&&loser!=-1||winner>=0&&loser!=1-winner)throw new IOException("原单挑胜败引用无效");
        int captured=-1,dead=-1;for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){int outcome=PcDuelKernel.readManager(m,0x64+12*side+4*slot);if(outcome==0)continue;if(!allowCapture||outcome<1||outcome>2||side!=loser||slot!=PcDuelKernel.readManager(m,0x60)||captured>=0||dead>=0)throw new IOException("原单挑此俘虏/死亡回调尚未闭合");if(outcome==1)captured=state.officers[side*3+slot];else dead=state.officers[side*3+slot];}
        var random=new PcDuelKernel.Random(state.random.state);int loss=winner<0||removedLoser?0:Math.min(troops[loser]*30/100,2000+random.uniform(50));if(winner>=0&&!removedLoser)loss=troops[loser]-Math.max(0,Math.min(capacity[loser],troops[loser]-loss));Plan p=new Plan(winner,random.state,random.draws,loss,captured);
        for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){int i=side*3+slot,id=state.officers[i];if(id<0)continue;int hp=PcDuelKernel.readManager(m,0x7c+12*side+4*slot),injury=PcDuelKernel.readManager(m,0xac+12*side+4*slot);if(hp<0||hp>100||injury<0||injury>3)throw new IOException("原单挑终局体力/伤病无效");p.health.put(id,Math.max(1,hp));p.injury.put(id,injury);}
        if(winner<0){for(int side=0;side<2;side++){int slot=PcDuelKernel.readManager(m,0x20+4*side);if(slot<0||slot>2||state.officers[side*3+slot]<0)throw new IOException("原平手最初上阵人物无效");p.merit[side*3+slot]=50;p.warXp[side*3+slot]=3;}}
        else {int own=PcDuelKernel.readManager(m,0x5c),other=PcDuelKernel.readManager(m,0x60);if(own<0||own>2||other<0||other>2||state.officers[winner*3+own]<0||state.officers[loser*3+other]<0)throw new IOException("原终局上阵人物无效");p.merit[winner*3+own]=captured>=0?200:100;p.merit[loser*3+other]=10;p.warXp[winner*3+own]=10;p.warXp[loser*3+other]=1;p.energy[winner]=15;p.energy[loser]=removedLoser?0:-15;}
        return p;
    }
    /** Persist the original AI decision before an asynchronous human heir
     * choice. Repeated preparation cannot repeat a native random draw. */
    static void prepareDisposition(World w,Contests.Session session)throws IOException {
        var duel=session.nativeDuel;if(duel==null||!duel.terminal())throw new IOException("原单挑尚未到终局");duel.validate(w,session);
        if(duel.disposition!=null||!duel.settings.separateDeath)return;
        int winner=PcDuelKernel.readManager(duel.state.manager,0x54),loser=PcDuelKernel.readManager(duel.state.manager,0x58),captured=-1;Map<Integer,Integer>injuries=new TreeMap<>();
        int dead=-1;for(int i=0;i<6;i++)if(duel.state.officers[i]>=0){injuries.put(duel.state.officers[i],PcDuelKernel.readManager(duel.state.manager,0xac+4*i));int outcome=PcDuelKernel.readManager(duel.state.manager,0x64+4*i);if(outcome!=0){if(outcome<1||outcome>2||i/3!=loser||i%3!=PcDuelKernel.readManager(duel.state.manager,0x60)||captured>=0||dead>=0)throw new IOException("原单挑死亡/多人物回调尚未闭合");if(outcome==1)captured=duel.state.officers[i];else dead=duel.state.officers[i];}}
        if(captured<0)return;var own=w.unit(winner==0?session.leftRef:session.rightRef);var other=w.unit(loser==0?session.leftRef:session.rightRef);
        var ai=PcDuelAiDisposition.preview(w,duel,own,other,captured,injuries);var pending=PcDuelDisposition.create(w,duel);pending.select(captured,ai.choice);for(var row:pending.rows)if(row.officerId==captured)row.recruitmentAdmitted=ai.recruitmentAdmitted;duel.disposition=pending;duel.state.random.state=ai.seed;duel.state.random.draws+=ai.draws;PcNativeDebatePolicy.setSeed(w,ai.seed);
    }
    /** Caller must use a transaction copy. All source/strategy validations and
     * pending casualty checks run before any campaign write. */
    static void apply(World w,Contests.Session session)throws IOException {
        PcDuelCampaign duel=session.nativeDuel;if(duel==null||!duel.terminal())throw new IOException("原单挑尚未到终局");duel.validate(w,session);if(!PcNativeHealthPolicy.enabled(w))throw new IOException("原單挑傷病策略未啟用");PcNativeHealthPolicy.validate(w);PcDuelHealthPolicy.validate(w);
        World.Unit[]units={w.unit(session.leftRef),w.unit(session.rightRef)};if(units[0] instanceof Domestic.Mission||units[1] instanceof Domestic.Mission)throw new IOException("原运输队单挑准入尚未核实");int[]troops={units[0].troops,units[1].troops},capacity={w.government.commandLimit(units[0].officerId),w.government.commandLimit(units[1].officerId)};
        int winner=PcDuelKernel.readManager(duel.state.manager,0x54),loser=PcDuelKernel.readManager(duel.state.manager,0x58),captured=-1;Map<Integer,Integer>terminalInjuries=new TreeMap<>();
        int dead=-1;for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){int id=duel.state.officers[side*3+slot];if(id<0)continue;terminalInjuries.put(id,PcDuelKernel.readManager(duel.state.manager,0xac+12*side+4*slot));int outcome=PcDuelKernel.readManager(duel.state.manager,0x64+12*side+4*slot);if(outcome!=0){if(outcome<1||outcome>2||side!=loser||slot!=PcDuelKernel.readManager(duel.state.manager,0x60)||captured>=0||dead>=0)throw new IOException("原单挑此俘虏/死亡回调尚未闭合");if(outcome==1)captured=id;else dead=id;}}
        prepareDisposition(w,session);
        boolean release=captured>=0&&duel.disposition!=null&&duel.disposition.rows.stream().anyMatch(row->row.choice==PcDuelDisposition.RELEASE);
        boolean execute=captured>=0&&duel.disposition!=null&&duel.disposition.rows.stream().anyMatch(row->row.choice==PcDuelDisposition.EXECUTE);
        boolean recruit=captured>=0&&duel.disposition!=null&&duel.disposition.rows.stream().anyMatch(row->row.choice==PcDuelDisposition.RECRUIT);
        if(recruit&&duel.disposition.rows.stream().anyMatch(r->r.choice==PcDuelDisposition.RECRUIT&&!r.recruitmentAdmitted))throw new IOException("原登用终局缺少成功准入记录");
        boolean removedLoser=false;
        if(dead>=0)PcDuelDeath.validate(w,units[winner],units[loser],dead,duel.state.officers[winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)],duel.naturalHeir?duel.chosenHeir:-1);
        if(dead>=0&&units[loser].officerId==dead){int next=PcDuelReplacement.select(w,units[loser],dead,terminalInjuries);if(next<0){PcDuelEscortRelease.plan(w,units[loser]);removedLoser=true;}else{capacity[loser]=w.government.commandLimit(next);troops[loser]=Math.min(troops[loser],capacity[loser]);}}
        if(captured>=0){if(recruit)PcDuelRecruitment.validate(w,units[winner],units[loser],captured);else if(execute)PcDuelExecution.validate(w,units[winner],units[loser],captured,duel.heirProtocol?duel.chosenHeir:-1);else if(release)PcDuelRelease.validateRelease(w,units[winner],units[loser],captured);else PcDuelCapture.validate(w,units[winner],units[loser],captured,duel.state.officers[winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)]);if(units[loser].officerId==captured){int next=PcDuelReplacement.select(w,units[loser],captured,terminalInjuries);if(next<0){if(PcDuelEscortRelease.current(w))PcDuelEscortRelease.plan(w,units[loser]);else if(!w.government.escorted(units[loser].id).isEmpty())throw new IOException("原单挑押送部队删除的其他俘虏回调尚未核实");removedLoser=true;}else{capacity[loser]=w.government.commandLimit(next);troops[loser]=Math.min(troops[loser],capacity[loser]);}}}
        Plan p=preview(duel.state,troops,capacity,true,removedLoser);
        for(var e:p.health.entrySet())if(w.officer(e.getKey())==null||!w.life.present(e.getKey()))throw new IOException("原单挑终局人物不可用");
        PcDuelHealthPolicy.writeDuelResult(w,p.health);for(var e:p.injury.entrySet())PcNativeHealthPolicy.setInjury(w,e.getKey(),e.getValue());
        if(dead>=0)PcDuelDeath.apply(w,units[winner],units[loser],dead,duel.state.officers[winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)],duel.naturalHeir?duel.chosenHeir:-1);
        if(captured>=0){if(recruit)PcDuelRecruitment.apply(w,units[winner],units[loser],captured);else if(execute)PcDuelExecution.apply(w,units[winner],units[loser],captured,duel.heirProtocol?duel.chosenHeir:-1);else if(release)PcDuelRelease.apply(w,units[winner],units[loser],captured);else PcDuelCapture.apply(w,units[winner],units[loser],captured,duel.state.officers[winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)]);}
        for(int i=0;i<6;i++)if(duel.state.officers[i]>=0&&w.life.present(duel.state.officers[i])){int id=duel.state.officers[i];if(p.merit[i]>0)w.government.merits.put(id,Math.min(60000,w.government.merit(id)+p.merit[i]));if(p.warXp[i]>0)w.officerAbilities.gainExperience(id,1,p.warXp[i]*(PcDebateCampaignRules.guided(w,w.officer(id))?2:1));}
        if(p.winner>=0){if(!removedLoser)units[loser].troops-=p.loserTroopLoss;for(int side=0;side<2;side++)if(w.unit(units[side].id)==units[side])w.energy.change(units[side],p.energy[side],EnergyRules.Reason.DUEL);int winnerId=duel.state.officers[p.winner*3+PcDuelKernel.readManager(duel.state.manager,0x5c)];w.campaign.setPoints(units[p.winner].owner,PcTechniquePoints.after(w.campaign.points(units[p.winner].owner),50),TechniquePointsJournal.Cause.DUEL,-1,winnerId);}
        PcNativeDebatePolicy.setSeed(w,p.seed);PcDuelCampaignPolicy.recordFinished(w,session.id);
    }
    private PcDuelSettlement(){}
}
