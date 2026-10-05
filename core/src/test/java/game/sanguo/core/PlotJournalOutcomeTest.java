package game.sanguo.core;

import java.util.*;

/** Real plot resolution with/without the transient journal must save byte-identically. */
public final class PlotJournalOutcomeTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static World fixture(){
        World w=new World(20,16,"甲","乙");
        for(int side=0;side<2;side++){
            int city=10+side;w.cities.add(new World.City(city,"城"+side,new Hex(2+side*14,2),side));
            for(int i=0;i<4;i++)w.officers.add(new World.Officer(side*10+i,"将"+(side*10+i),side,city,80,80,80,80,80));
        }
        w.strategy.initializeOffices();
        unit(w,0,new Hex(6,6));unit(w,10,new Hex(7,6));unit(w,11,new Hex(7,7));
        w.officer(0).skillId=Skill.SHENSUAN.id;w.officer(0).intelligence=99;w.strategy.setSeed(7351);
        return w;
    }
    private static World.Unit unit(World w,int officer,Hex hex){
        World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);
        World.Unit u=new World.Unit(w.nextUnitId++,o.owner,officer,World.Weapon.SPEAR,hex,5000,20000);
        o.cityId=-1;o.unitId=u.id;w.units.add(u);return u;
    }
    private static List<TurnJournal.PlotOutcome> play(World prepared,int actor,Hex target,War.Plot plot)throws Exception{
        byte[] initial=SaveCodec.encode(prepared);
        World w=SaveCodec.decode(initial),plain=SaveCodec.decode(initial),visual=SaveCodec.decode(initial);
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(plain)),"identical authoritative preconditions, including prepared fire/save normalization");
        TurnJournal journal=new TurnJournal(w);
        World.Result actual=w.war.plot(actor,target,plot),expected=plain.war.plot(actor,target,plot);
        check(actual.ok==expected.ok&&actual.message.equals(expected.message),"journal changes no command result: "+plot+" journal="+actual.message+" plain="+expected.message);
        check(actual.ok,actual.message);journal.close();
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(plain)),"entire authority and RNG/save match without journal: "+plot);
        List<TurnJournal.PlotOutcome> outcomes=new ArrayList<>();
        for(TurnJournal.Event e:journal.events()){
            outcomes.addAll(e.plotOutcomes);e.applyVisual(visual);
            try{e.plotOutcomes.clear();throw new AssertionError("mutable outcomes");}catch(UnsupportedOperationException ok){checks++;}
        }
        byte[] before=SaveCodec.encode(w);World.Unit a=w.unit(actor);int oldOfficer=a.officerId;a.officerId=1;
        check(!outcomes.isEmpty()&&outcomes.get(0).officerId==oldOfficer&&outcomes.get(0).officerName.equals(plain.officer(oldOfficer).name),"detached actor identity survives later crew mutations");
        a.officerId=oldOfficer;
        check(Arrays.equals(before,SaveCodec.encode(w)),"inspection/playback changes no saved state");
        return outcomes;
    }
    public static void main(String[] args)throws Exception{
        for(War.Plot plot:new War.Plot[]{War.Plot.FIRE,War.Plot.EXTINGUISH,War.Plot.CONFUSE,War.Plot.MISLEAD,War.Plot.CALM,War.Plot.AMBUSH,War.Plot.INFIGHT}){
            World w=fixture();Hex target=w.unit(2).hex;
            if(plot==War.Plot.EXTINGUISH){w.war.ignite(target,w.unit(1));}
            if(plot==War.Plot.CALM){World.Unit ally=unit(w,1,new Hex(6,7));target=ally.hex;ally.status=War.Status.CONFUSED;ally.statusTurns=2;}
            if(plot==War.Plot.AMBUSH)w.terrain[6][6]=World.Terrain.FOREST;
            List<TurnJournal.PlotOutcome> facts=play(w,1,target,plot);
            check(facts.size()==1&&facts.get(0).success&&facts.get(0).critical,"applied critical captured once: "+plot);
            check(facts.get(0).plot==plot&&facts.get(0).cause==TurnJournal.PlotCause.COMMAND,"precise primary marker");
        }
        World ordinary=fixture();ordinary.officer(0).skillId="none";
        List<TurnJournal.PlotOutcome> facts=play(ordinary,1,ordinary.unit(2).hex,War.Plot.CONFUSE);
        check(facts.size()==1&&!facts.get(0).critical,"ordinary result never invented as critical");
        World failed=fixture();failed.officer(10).skillId=Skill.DONGCHA.id;
        facts=play(failed,1,failed.unit(2).hex,War.Plot.CONFUSE);
        check(facts.size()==1&&!facts.get(0).success&&!facts.get(0).critical,"immune failed attempt produces no critical");
        World reflected=fixture();reflected.officer(0).intelligence=40;reflected.officer(0).skillId="none";
        reflected.officer(10).skillId=Skill.DONGCHA.id;reflected.officer(12).skillId=Skill.FANJI.id;reflected.officer(13).skillId=Skill.SHENSUAN.id;reflected.officer(13).intelligence=99;
        reflected.unit(2).deputies=new int[]{12,13};for(int id:new int[]{12,13}){reflected.officer(id).cityId=-1;reflected.officer(id).unitId=2;}
        facts=play(reflected,1,reflected.unit(2).hex,War.Plot.CONFUSE);
        check(facts.size()==2&&!facts.get(0).success&&facts.get(1).critical,"failed then reflected actual outcomes retained");
        check(facts.get(1).cause==TurnJournal.PlotCause.REFLECTION&&facts.get(1).actorId==2&&facts.get(1).targetId==1,"reflected actor/target cannot borrow command actor");
        World chained=fixture();chained.unit(1).deputies=new int[]{1};chained.officer(1).cityId=-1;chained.officer(1).unitId=1;chained.officer(1).skillId=Skill.LIANHUAN.id;
        facts=play(chained,1,chained.unit(2).hex,War.Plot.CONFUSE);
        check(facts.size()==2&&facts.get(0).critical&&facts.get(1).critical&&facts.get(1).cause==TurnJournal.PlotCause.CHAIN,"successful chain remains ordered independent cue");
        check(facts.get(1).targetId==3,"chain target recorded at actual rule boundary");
        for(War.Plot plot:new War.Plot[]{War.Plot.SORCERY,War.Plot.LIGHTNING}){
            World magic=fixture();magic.unit(1).deputies=new int[]{1};magic.officer(1).cityId=-1;magic.officer(1).unitId=1;magic.officer(1).skillId=Skill.GUIMEN.id;
            magic.strategy.setSeed(1); // Saved test input: first roll32 is below its actual59% chance.
            facts=play(magic,1,magic.unit(2).hex,plot);
            check(facts.size()==1&&facts.get(0).plot==plot&&facts.get(0).success&&facts.get(0).critical,"applied magic critical observed once: "+plot);
            check(facts.get(0).cause==TurnJournal.PlotCause.COMMAND,"magic command identity preserved");
            World immune=fixture();immune.unit(1).deputies=new int[]{1};immune.officer(1).cityId=-1;immune.officer(1).unitId=1;immune.officer(1).skillId=Skill.GUIMEN.id;immune.officer(10).skillId=Skill.DONGCHA.id;
            facts=play(immune,1,immune.unit(2).hex,plot);
            check(facts.size()==1&&!facts.get(0).success&&!facts.get(0).critical,"failed magic has no invented critical: "+plot);
            World magicReflection=fixture();magicReflection.officer(0).skillId="none";magicReflection.officer(0).intelligence=40;
            magicReflection.unit(1).deputies=new int[]{1};magicReflection.officer(1).cityId=-1;magicReflection.officer(1).unitId=1;magicReflection.officer(1).skillId=Skill.GUIMEN.id;
            magicReflection.officer(10).skillId=Skill.FANJI.id;magicReflection.unit(2).deputies=new int[]{12,13};
            for(int id:new int[]{12,13}){magicReflection.officer(id).cityId=-1;magicReflection.officer(id).unitId=2;}
            magicReflection.officer(12).skillId=Skill.GUIMEN.id;magicReflection.officer(13).skillId=Skill.SHENSUAN.id;magicReflection.officer(13).intelligence=99;
            magicReflection.strategy.setSeed(1);
            facts=play(magicReflection,1,magicReflection.unit(2).hex,plot);
            check(facts.size()==2&&!facts.get(0).success&&facts.get(1).cause==TurnJournal.PlotCause.REFLECTION&&facts.get(1).actorId==2&&facts.get(1).targetId==1,"magic reflection keeps real resolution order and actor: "+plot);
        }
        check(chained.war.plot(1,chained.unit(2).hex,War.Plot.CONFUSE).ok,"prepare completed command for repeat rejection");
        TurnJournal j=new TurnJournal(chained);check(!chained.war.plot(1,chained.unit(2).hex,War.Plot.CONFUSE).ok,"repeated command rejected");j.close();
        check(j.events().isEmpty(),"rejected command cannot emit cue or stale previous facts");
        System.out.println("PASS "+checks+" plot journal: nine actual plots, ordinary/failure, magic reflection, chain, immutable facts, complete save/RNG equality");
    }
}
