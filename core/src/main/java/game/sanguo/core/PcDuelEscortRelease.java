package game.sanguo.core;
import java.io.*;import java.util.*;
/** Original4a5d90 deletion releases every carried captive using its OWN
 * first-army HQ and task37. The winning unit never takes these prisoners.
 * The immutable allegiance and current native army link remain distinct. */
final class PcDuelEscortRelease {
    static final class Row {final int id,army,origin,home;Row(int id,int army,int origin,int home){this.id=id;this.army=army;this.origin=origin;this.home=home;}}
    static final class Plan {final List<Row> rows;Plan(List<Row> rows){this.rows=List.copyOf(rows);}}
    static boolean current(World w)throws IOException {return PcDuelCampaignPolicy.enabled(w)&&PcDuelCampaignPolicy.read(w).version==3;}
    static Plan plan(World w,World.Unit unit)throws IOException {
        List<Row> rows=new ArrayList<>();if(w.government.escorted(unit.id).isEmpty())return new Plan(rows);
        if(!current(w)||w.unit(unit.id)!=unit)throw new IOException("旧保存或原押送部队连接尚未覆盖");var data=PcGovernorPolicy.data(w);var facts=PcDuelSourceFacts.saved(w);int origin=PcPersonnelReturnRules.cityAt(w,unit.hex);
        for(var prisoner:w.government.escorted(unit.id)){var person=w.officer(prisoner.officerId);var a=data.assignments.get(prisoner.officerId);if(person==null||!facts.containsKey(person.id)||a==null||person.owner<0||!w.life.present(person.id)||person.unitId>=0||prisoner.captor!=unit.owner||prisoner.cityId!=-1||person.otherTaskTurns!=0||w.domestic.busy(person.id)||PcDuelRelease.busy(w,person.id))throw new IOException("原押送释放人物/所属/任务连接不同");
            int army=PcArmyActionPolicy.primaryArmy(w,person.owner);if(army<0||a.army!=army||data.mergedArmies.containsKey(army))throw new IOException("原押送释放非第一军团的归队分支尚未覆盖");int nativeLeader=data.armyLeaders.getOrDefault(army,-1);int leader=PcDuelRecruitmentAdmission.stable(facts,nativeLeader);var la=data.assignments.get(leader);if(la==null||w.officer(leader).owner!=person.owner||!w.life.present(leader)||w.government.captive(leader))throw new IOException("原押送释放当前第一军团长缺失");int home=la.home;var site=w.city(PcPersonnelReturnRules.projectSite(w,home));if(site==null||site.owner!=person.owner||data.unknownSites.contains(site.id))throw new IOException("原押送释放第一军团驻点失陷/未知分支未覆盖");PcDuelRelease.validateReturn(w,origin,home);rows.add(new Row(person.id,army,origin,home));
        }return new Plan(rows);
    }
    static void apply(World w,World.Unit unit,Plan plan)throws IOException {
        var data=PcGovernorPolicy.data(w);for(var row:plan.rows){var person=w.officer(row.id);var prisoner=w.government.prisoner(row.id);if(prisoner==null||prisoner.unitId!=unit.id)throw new IOException("原押送释放待办已经变化");w.government.prisoners.remove(row.id);person.unitId=-1;person.cityId=PcPersonnelReturnRules.projectSite(w,row.origin);person.acted=true;var a=data.assignments.get(row.id);a.army=row.army;a.home=row.home;a.lastCity=person.cityId;PcGovernorPolicy.write(w,data);PcDuelRelease.startReturn(w,row.id,row.origin,row.home);w.note(person.name+"因押送部队解散获释，按原驻点返程");}
    }
    private PcDuelEscortRelease(){}
}
