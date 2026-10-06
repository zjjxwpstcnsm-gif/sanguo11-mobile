package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.query.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Queries are read-only; command-generated construction/fire/removal have detached facts. */
public final class SceneFactsQueryTest {
    private static int checks;
    private static final StateToken TOKEN=new StateToken("scene-facts-test",1,0);
    private static void check(boolean ok,String detail){checks++;if(!ok)throw new AssertionError(detail);}
    private static void immutable(Runnable edit){boolean rejected=false;try{edit.run();}catch(UnsupportedOperationException expected){rejected=true;}check(rejected,"immutable detached collection");}
    private static void projection(World w)throws Exception{
        byte[] before=SaveCodec.encode(w);
        SceneFactsSnapshot scene=SceneFactsQuery.capture(w,TOKEN);
        OfficerSnapshot officers=OfficerQuery.capture(w,TOKEN);
        check(scene.available&&scene.state.equals(TOKEN),"same authority token");
        check(scene.sites.size()==w.cities.size()&&scene.fires.size()==w.war.fires().size()&&scene.military.size()==w.war.structures().size(),"current entity counts");
        check(scene.turn==w.turn&&scene.mapRevision==w.mapRevision&&scene.terrainRevision==w.terrainRevision,"actual revisions/calendar");
        for(int i=0;i<w.cities.size();i++){
            World.City city=w.cities.get(i);SceneFactsSnapshot.Site fact=scene.sites.get(i);
            check(fact.id==city.id&&fact.hp==city.defense&&fact.gold==city.gold&&fact.food==city.food&&fact.troops==city.troops,"actual site resources");
            check(fact.footprint.size()==SiteFootprint.cells(city).size(),"seven-cell city/single gate-port preserved");
            var source=MapCoordinates.nationalSource(w,city.hex);
            check(fact.center.q==city.hex.q&&fact.center.r==city.hex.r&&fact.center.sourceX==source.x&&fact.center.sourceY==source.y,"exact source and axial coordinates");
        }
        for(PcScenarioPeople.Person person:PcScenarioPeople.saved(w))if(person.officerId>=0){
            OfficerSnapshot.SourceInfo source=officers.officer(person.officerId).source;
            check(source!=null&&source.nativeId==person.nativeId&&source.recordSha.equals(person.recordSha),"stable native/runtime source join");
            check(source.originalFields.equals(person.fields)&&Objects.equals(source.originalVoiceProfile,person.fields.get(48)),"stored opening voice/all numeric fields without guessing");
            immutable(()->source.originalFields.put(48,999));
        }
        var administration=PcGovernorPolicy.view(w);
        check(scene.administration.originalElectionEnabled==administration.enabled,"explicit original administration strategy");
        check(scene.administration.armies.size()==administration.armies.size(),"original army slot coverage");
        for(int i=0;i<administration.armies.size();i++){
            var expected=administration.armies.get(i);var actual=scene.administration.armies.get(i);
            check(actual.originalValid==expected.originalValid&&actual.nativeId==expected.nativeId&&actual.owner==expected.owner,"army validity preserved independently of raw owner0");
            check(actual.actionPoints==PcArmyActionPolicy.points(w,expected.nativeId),"saved original army AP, absent unknown−1 not fabricated60");
        }
        immutable(()->scene.fires.clear());immutable(()->scene.sites.get(0).footprint.clear());
        check(Arrays.equals(before,SaveCodec.encode(w)),"scene/officer reads preserve fullWorld/bothRNG");
    }
    private static World deployed(int seed)throws Exception{
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(14).identity.scenarioId,1,seed);
        World.City city=w.home();World.Officer actor=w.idle(city).stream().max(Comparator.comparingInt(o->o.intelligence)).orElseThrow();
        check(w.army.deploy(city.id,actor.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,3000,6000,1000).ok,"normal source deployment");
        World.Unit unit=w.unit(actor.unitId);check(w.move(unit.id,new Hex(202,45)).ok,"normal exact movement");return w;
    }
    private static void applied()throws Exception{
        World w=deployed(42);World.Unit unit=w.fieldUnits().get(0);Hex target=new Hex(202,44);
        TurnJournal journal=new TurnJournal(w);
        check(w.fieldworks.build(unit.id,War.StructureKind.CAMP,target,0).ok,"normal military construction");
        SceneFactsSnapshot captured=SceneFactsQuery.capture(w,TOKEN);
        SceneFactsSnapshot.Military site=captured.military.stream().filter(s->s.cell.q==target.q&&s.cell.r==target.r).findFirst().orElseThrow();
        check(!site.complete&&site.builderUnitId==unit.id&&site.hp>0&&site.maxHp==War.StructureKind.CAMP.hp,"actual unfinished construction facts");
        TurnJournal.Event build=journal.events().stream().filter(e->e.states.stream().anyMatch(c->c.after!=null&&c.after.key.equals("s"+site.id))).findFirst().orElseThrow();
        byte[] before=SaveCodec.encode(w);
        GameEvent parent=new GameEvent(GameEvent.Kind.LEGACY_COMMITTED,TOKEN,-1,-1,0,0,"test host commit");
        AppliedEventSnapshot event=AppliedEventQuery.capture(w,TOKEN,parent,build);
        check(event.id.equals(build.id)&&event.journalId==build.journalId&&event.sequence==build.sequence&&event.committedParentId.equals(parent.id),"existing journal/API identities copied");
        check(event.speakerOfficerId==null&&event.originalParentId==null&&event.unknown.contains("originalSpeechCaller"),"unknown original caller/parent stays unknown");
        check(event.changes.stream().anyMatch(c->c.after!=null&&c.after.category.equals("MILITARY")&&c.after.builderId==unit.id&&!c.after.complete),"applied construction state");
        immutable(()->event.changes.clear());check(Arrays.equals(before,SaveCodec.encode(w)),"applied projection does not resolve again or draw RNG");
        World.City city=w.home();World.Officer remover=w.idle(city).get(0);
        check(w.war.removeStructure(city.id,remover.id,site.id).ok,"normal own military dismantle");
        TurnJournal.Event removal=journal.events().stream().filter(e->e.states.stream().anyMatch(c->c.before!=null&&c.before.key.equals("s"+site.id)&&c.after==null)).findFirst().orElseThrow();
        AppliedEventSnapshot removed=AppliedEventQuery.capture(w,TOKEN,null,removal);
        check(removed.committedParentId==null&&removed.changes.stream().anyMatch(c->c.removed()&&c.before.category.equals("MILITARY")),"real removal and unknown commit parent");
        check(captured.military.stream().anyMatch(s->s.id==site.id)&&w.war.at(target)==null,"detached snapshot survives later deletion");
        boolean stale=false;try{AppliedEventQuery.capture(w,new StateToken("other",1,0),parent,build);}catch(IllegalArgumentException expected){stale=true;}check(stale,"wrong parent StateToken rejected");
        journal.close();
        World burning=null;
        for(int seed=42;seed<58&&burning==null;seed++){
            World candidate=deployed(seed);World.Unit a=candidate.fieldUnits().get(0);
            check(candidate.war.plot(a.id,target,War.Plot.FIRE).ok,"normal fire command admission");
            if(candidate.war.fireAt(target)!=null)burning=candidate;
        }
        check(burning!=null,"normal applied fire produced");
        byte[] fireBefore=SaveCodec.encode(burning);SceneFactsSnapshot fire=SceneFactsQuery.capture(burning,TOKEN);
        War.Fire actual=burning.war.fireAt(target);SceneFactsSnapshot.Fire fact=fire.fires.stream().filter(f->f.cell.q==target.q&&f.cell.r==target.r).findFirst().orElseThrow();
        check(fact.owner==actual.owner&&fact.remaining==actual.remaining&&fact.power==actual.power&&fact.trap==actual.trap,"actual fire owner/lifetime/power/trap");
        check(Arrays.equals(fireBefore,SaveCodec.encode(burning)),"fire query fullsave/bothRNG pure");
    }
    public static void main(String[] args)throws Exception{
        for(PcScenarioCatalog.Source source:PcScenarioCatalog.all())projection(PcScenarioCatalog.preview(source.identity.scenarioId));
        Path root=Path.of("").toAbsolutePath();if(!Files.isDirectory(root.resolve("core")))root=root.getParent();
        for(String path:List.of("core/src/test/resources/save-v32-central-native.sg11","core/src/test/resources/pre-base-construction-v33.sg11","core/src/test/resources/pre-atomic-ship-cargo-v33.sg11","game-runtime/src/test/resources/architecture/prepared-9548bb35-v33.sg11","core/src/test/resources/pre-merchant-r25-v34.sg11","core/src/test/resources/legacy-market-v35/host/coalition-190.sg11","core/src/test/resources/legacy-production-v36/coalition-190-host.sg11","docs/handoff/20261004/session2/six-turn-authority-29/authority-before.sg11","docs/handoff/20261004/session1/batch19-actual-art-mid.sg11"))projection(SaveCodec.decode(Files.readAllBytes(root.resolve(path))));
        applied();
        try(GameSession session=new GameSession(PcScenarioCatalog.preview(PcScenarioCatalog.all().get(0).identity.scenarioId))){
            byte[] before=session.captureSave();check(session.sceneFacts().state.equals(session.state()),"GameApi serial authority boundary");check(Arrays.equals(before,session.captureSave()),"session scene query pure");
            AtomicReference<Throwable> error=new AtomicReference<>();Thread wrong=new Thread(()->{try{session.sceneFacts();}catch(Throwable t){error.set(t);}});wrong.start();wrong.join();check(error.get() instanceof IllegalStateException,"off-thread read rejected");
        }
        System.out.println("PASS SceneFactsQuery "+checks+" assertions;16 sources/9 actual old saves/source voice/fullWorld/bothRNG/detached construction-fire-removal/identity/thread; APK and A integration separate");
    }
}
