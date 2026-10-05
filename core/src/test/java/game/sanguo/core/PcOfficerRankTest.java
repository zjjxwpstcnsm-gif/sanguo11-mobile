package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native source definitions consumed by actual appointment/deployment/turn/save paths. */
public final class PcOfficerRankTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static void ok(World.Result result){check(result.ok,result.message);}
    static World copy(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    public static void main(String[] args)throws Exception {
        List<String> legacy=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(PcOfficerRankTest.class.getResourceAsStream("/pc-rank-legacy-ak.tsv"),StandardCharsets.UTF_8))){
            check(in.readLine().equals("order\tproject_id\tmerit\tcommand\tsalary\tcivilian\ttitle_grade"),"frozen compiled AK rank contract");
            String line;while((line=in.readLine())!=null)legacy.add(line);
        }
        check(legacy.size()==80&&Government.ranks().size()==80&&PcOfficerRanks.all().size()==81,"complete old/native scope");
        Set<Integer> nativeIds=new HashSet<>();
        for(int index=0;index<80;index++){
            Government.Rank rank=Government.ranks().get(index);
            String actual=index+"\t"+rank.id+"\t"+rank.merit+"\t"+rank.troops+"\t"+rank.salary+"\t"+rank.civilian+"\t"+rank.requiredTitle.grade();
            check(actual.equals(legacy.get(index)),"preserve every compiled old ID/value/order: "+rank.id);
            check(nativeIds.add(rank.nativeId),"unique native identity bridge");
            PcOfficerRanks.Definition nativeRank=PcOfficerRanks.all().get(rank.nativeId);
            check(rank.id.equals(nativeRank.projectId)&&rank.abilityStat==nativeRank.abilityStat&&rank.abilityBonus==nativeRank.abilityBonus,"native bonus reference");
            for(int stat=0;stat<5;stat++)check(PcOfficerAbilityRules.current(80,-1,30,0,stat,0,rank.abilityStat,rank.abilityBonus,false,false,true)==80+(stat==rank.abilityStat?rank.abilityBonus:0),"resolved rank works in ability formula");
            World w=world();w.government.earn(2,60000);
            long rng=w.strategy.getRandomState();ok(w.government.appointRank(10,1,2,rank.id));
            check(w.government.office(2)==rank&&w.government.baseCommandLimit(2)==rank.troops,"ordinary appointment consumes imported command limit");
            check(w.strategy.getRandomState()==rng,"appointment does not consume RNG");
            byte[] appointed=SaveCodec.encode(w);check(Arrays.equals(appointed,SaveCodec.encode(copy(w))),"legacy string ID and complete save preserved");
            ok(w.nextTurn());ok(w.army.deploy(10,2,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,rank.troops,30000));
            check(w.unit(1).troops==rank.troops,"ordinary deployment admits native command capacity");
            World restored=copy(w),withoutOffice=copy(w);withoutOffice.government.ranks.remove(2);
            for(int turn=0;turn<3;turn++){
                ok(w.nextTurn());ok(restored.nextTurn());ok(withoutOffice.nextTurn());
                check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"full turn/save/RNG continuation");
            }
            check(withoutOffice.city(10).gold-w.city(10).gold==rank.salary,"normal monthly settlement consumes imported salary");
        }
        check(nativeIds.size()==80&&!nativeIds.contains(80),"unassigned is not a selectable office");
        check(PcOfficerRanks.unassigned().command==5000&&PcOfficerRanks.unassigned().salary==5,"retain source unassigned values without inventing payroll application");
        System.out.println("PASS PcOfficerRankTest checks="+checks+" native=81 legacy=80 (title gate/unassigned payroll parity pending)");
    }
    static World world(){
        World w=new World(20,12,"甲","乙");
        w.cities.add(new World.City(10,"甲城",new Hex(2,2),0));w.cities.add(new World.City(20,"乙城",new Hex(16,8),1));
        for(World.City c:w.cities){c.gold=c.owner==0?50000:0;c.food=200000;c.troops=c.owner==0?50000:0;for(int i=0;i<4;i++)c.equipment[i]=c.owner==0?50000:0;}
        w.officers.add(new World.Officer(1,"君主",0,10,80,80,80,80,80));w.officers.add(new World.Officer(2,"任官",0,10,80,80,80,80,80));w.officers.add(new World.Officer(3,"敌君",1,20,80,80,80,80,80));
        w.officer(1).role=Strategy.Role.RULER;w.officer(1).loyalty=100;w.officer(3).role=Strategy.Role.RULER;w.officer(3).loyalty=100;
        w.governance.grades.put(0,RulerTitles.Title.EMPEROR.grade());return w;
    }
}
