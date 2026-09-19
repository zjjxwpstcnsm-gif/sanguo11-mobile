package game.sanguo.core;

import java.util.*;

public final class Turn48Test {
    private static int checks;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static World clone(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    public static void main(String[] args)throws Exception{terrain();facilities();boundaries();System.out.println("PASS v48: "+checks+" terrain, facility provenance/energy, phase-order and deterministic replay checks");}
    private static void terrain()throws Exception{
        World w=Turn48Fixture.world();w.war.structures.clear();Hex target=new Hex(8,9);World.Unit u=w.unit(1);
        for(Campaign.Tech t:Campaign.Tech.researchable())w.campaign.finishTech(0,t);
        for(World.Terrain t:Arrays.asList(World.Terrain.FOREST,World.Terrain.SWAMP)){
            w.terrain[target.q][target.r]=t;
            for(War.StructureKind kind:w.fieldworks.available(0)){
                byte[] before=SaveCodec.encode(w);
                check(w.fieldworks.buildError(u.id,kind,target,0).contains("森林与湿地"),"shared terrain reason "+t+" "+kind);
                check(!w.fieldworks.sites(u.id,kind).contains(target),"map highlights exclude prohibited terrain");
                check(!w.fieldworks.build(u.id,kind,target,0).ok,"direct command rejects prohibited terrain");
                check(Arrays.equals(before,SaveCodec.encode(w)),"rejection keeps gold/action/RNG unchanged");
            }
        }
        w.terrain[target.q][target.r]=World.Terrain.PLAIN;
        check(w.fieldworks.buildError(1,War.StructureKind.FORTRESS,target,0)==null,"valid plain remains buildable");
        w.terrain[target.q][target.r]=World.Terrain.SAND;
        check(w.fieldworks.buildError(1,War.StructureKind.FORTRESS,target,0)==null,"sand remains buildable");
        w.terrain[target.q][target.r]=World.Terrain.WATER;
        check(w.fieldworks.buildError(1,War.StructureKind.FIRE_SHIP,target,0)==null,"fire ship keeps water exception");
        check(w.fieldworks.buildError(1,War.StructureKind.FORTRESS,target,0)!=null,"land facility cannot build on water");
        Hex legacy=new Hex(4,2);w.terrain[legacy.q][legacy.r]=World.Terrain.FOREST;
        check(!w.war.buildSites(0).contains(legacy),"legacy city helper cannot bypass forest rule");
        w.terrain[legacy.q][legacy.r]=World.Terrain.SWAMP;
        check(!w.war.buildSites(0).contains(legacy),"legacy city helper cannot bypass wetland rule");
    }
    private static void facilities()throws Exception{
        World w=Turn48Fixture.world(),plain=clone(w),visual=clone(w);TurnJournal journal=new TurnJournal(w);
        Turn48Fixture.facilityCommands(w);journal.close();Turn48Fixture.facilityCommands(plain);
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(plain)),"facility recording never alters real rules or RNG");
        List<TurnJournal.Event> events=journal.events();
        TurnJournal.Event tower=events.stream().filter(e->e.kind==TurnJournal.Kind.FACILITY_ATTACK).findFirst().orElseThrow();
        TurnJournal.Event counter=events.stream().filter(e->e.kind==TurnJournal.Kind.FACILITY_COUNTER).findFirst().orElseThrow();
        TurnJournal.Event music=events.stream().filter(e->e.kind==TurnJournal.Kind.RECOVER&&e.label.contains("军乐台")).findFirst().orElseThrow();
        check(tower.owner==0&&tower.start.equals(new Hex(9,7))&&tower.target.equals(new Hex(10,8)),"tower has actual source, owner and target");
        check(counter.owner==1&&counter.start.equals(new Hex(9,8))&&counter.target.equals(new Hex(8,8)),"counter belongs to facility owner, not active player");
        check(music.owner==0&&music.start.equals(new Hex(7,8))&&music.target.equals(new Hex(8,8)),"music pulse comes from real friendly facility");
        check(music.impacts.stream().anyMatch(i->i.text.equals("气力 +5")),"energy popup is actual capped +5, not requested +10");
        int attack=-1,reply=-1;for(int i=0;i<events.size();i++){if(events.get(i).kind==TurnJournal.Kind.ATTACK)attack=i;if(events.get(i).kind==TurnJournal.Kind.FACILITY_COUNTER)reply=i;}
        check(attack>=0&&reply>attack,"structure retaliation follows incoming attack in separate event");
        for(TurnJournal.Event e:events)e.applyVisual(visual);
        check(visual.unit(1).energy==w.unit(1).energy&&visual.unit(1).troops==w.unit(1).troops,"facility replay reaches authoritative HP and energy");
        check(visual.unit(2).troops==w.unit(2).troops,"tower replay applies exact target damage");
        TurnJournal empty=new TurnJournal(w);w.energy.settleTurn();empty.close();
        check(empty.events().stream().noneMatch(e->e.kind==TurnJournal.Kind.RECOVER&&e.target.equals(w.unit(1).hex)),"full-energy unit has no fake recovery animation");
        World destroyed=Turn48Fixture.world();destroyed.war.structures.clear();TurnJournal noSource=new TurnJournal(destroyed);destroyed.energy.settleTurn();noSource.close();
        check(noSource.events().stream().noneMatch(e->e.kind==TurnJournal.Kind.RECOVER),"removed facilities cannot emit an aura");
        World skilled=Turn48Fixture.world();skilled.unit(1).energy=50;skilled.officer(1).skillId=Skill.SHIXIANG.id;
        TurnJournal skill=new TurnJournal(skilled);skilled.energy.settleTurn();skill.close();
        check(skill.events().stream().anyMatch(e->e.impacts.stream().anyMatch(i->i.text.equals("气力 +20"))),"poetry skill keeps actual +20 music recovery");
    }
    private static void boundaries()throws Exception{
        World w=Turn48Fixture.world(),plain=clone(w),visual=clone(w);TurnJournal journal=new TurnJournal(w);
        List<Integer> owners=new ArrayList<>();int[] count={0};
        check(w.nextTurn(p->{journal.checkpoint(p.phase);if(p.boundary){owners.add(p.owner);List<TurnJournal.Event> batch=journal.drainEvents();check(journal.events().isEmpty(),"producer releases completed phase events");for(TurnJournal.Event e:batch){e.applyVisual(visual);count[0]++;}}}).ok,"streamed real turn succeeds");
        journal.close();for(TurnJournal.Event e:journal.drainEvents())e.applyVisual(visual);
        check(owners.equals(Arrays.asList(0,1,2,-1,0)),"delegated -> each enemy -> facilities/global -> player orders, without changing rule order");
        check(count[0]>0,"real actions emitted before final turn completion");
        check(plain.nextTurn().ok,"unobserved reference turn succeeds");
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(plain)),"phase callback/animation wait has zero effect on final save bytes");
        for(World.Unit u:w.units){World.Unit shown=visual.unit(u.id);check(shown!=null&&shown.hex.equals(u.hex)&&shown.troops==u.troops&&shown.energy==u.energy,"all displayed units reach authoritative result "+u.id);}
    }
}
