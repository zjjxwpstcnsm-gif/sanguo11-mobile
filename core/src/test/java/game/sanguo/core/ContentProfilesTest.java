package game.sanguo.core;

import java.util.*;

/** Source-to-game integration, ambiguity isolation, preview atomicity and persisted gameplay outcomes. */
public final class ContentProfilesTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static int id(ContentCatalog c,String name){return c.officers().stream().filter(o->o.name.equals(name)).findFirst().orElseThrow().id;}
    private static World empty(){
        World w=new World(20,20,"资料营","对手");w.startYear=208;
        w.cities.add(new World.City(10,"资料城",new Hex(3,3),0));w.cities.add(new World.City(20,"对手城",new Hex(16,16),1));
        w.officers.add(new World.Officer(900001,"原营主",0,10,70,70,70,70,70));
        w.officers.add(new World.Officer(900002,"对手主",1,20,70,70,70,70,70));w.strategy.initializeOffices();return w;
    }
    public static void main(String[] args)throws Exception {
        ContentCatalog c=ContentCatalog.get();check(c.relations().size()==869,"869 usable relation rows, no self group marker");
        int liu=id(c,"劉備"),guan=id(c,"關羽"),zhang=id(c,"張飛"),cao=id(c,"曹操"),pi=id(c,"曹丕");
        World reference=TestScenarios.load("officer-reference-drill",0);
        check(reference.relations.sworn(liu,guan)&&reference.relations.sworn(guan,zhang),"source group closes in actual opening");
        check(reference.relations.contribution(liu,guan,75,97)==97,"source relationship changes formation contribution");
        check(reference.contests.profile(zhang).temper==Debate.Temper.RASH,"source temperament reaches debate");
        check(reference.life.life(liu)!=null&&!reference.life.enabled(),"biographies loaded without silently enabling mortality");
        World w=empty();byte[] before=bytes(w);Editor.Draft preview=w.editor.sourceOfficer(cao,10,true,true);
        check(preview.valid()&&Arrays.equals(before,bytes(w)),"valid import preview and cancellation are pure");
        check(w.editor.apply(preview).ok&&w.officer(cao).skillId.equals(Skill.XUSHI.id)&&w.life.life(cao)!=null,"source officer is playable with skill and lifespan");
        before=bytes(w);check(!w.editor.apply(preview).ok&&Arrays.equals(before,bytes(w)),"stale/repeated import rejected without mutation");
        check(!w.editor.sourceOfficer(cao,10,true,true).valid(),"duplicate stable identity rejected");
        check(w.editor.apply(w.editor.sourceOfficer(pi,10,true,true)).ok&&w.relations.parent(pi,false)==cao,"later child joins existing father by ID");
        World conflict=empty();ContentProfiles.add(conflict,c,pi,10,false,false);conflict.relations.link(pi,900001,Relations.Kind.FATHER);
        byte[] conflictBefore=bytes(conflict);check(!conflict.editor.sourceOfficer(cao,10,false,true).valid()&&Arrays.equals(conflictBefore,bytes(conflict)),"conflicting source relationship rejects whole import without clobbering edits");
        check(w.editor.edited()&&SaveCodec.decode(bytes(w)).relations.parent(pi,false)==cao,"editor marker and source relations survive save");
        int future=c.officers().stream().filter(o->o.appearance>208).findFirst().orElseThrow().id;
        Editor.Draft futureDraft=w.editor.sourceOfficer(future,10,true,true);check(futureDraft.valid(),"unappeared preview supports absent city position");
        check(w.editor.apply(futureDraft).ok&&w.life.state(future)==Lifecycle.State.UNAPPEARED&&w.officer(future).owner==-1,"future appearance is not premature enlistment");
        World restored=SaveCodec.decode(bytes(w));restored.startYear=c.officer(future).appearance;restored.startMonth=12;restored.turn=3;restored.life.tick();
        check(restored.life.present(future)&&restored.officer(future).cityId==10&&restored.officer(future).owner==-1,"scheduled source officer actually appears as unaffiliated");
        int violent=c.officers().stream().filter(o->!c.profile(o.id).naturalDeath&&o.appearance<=208).findFirst().orElseThrow().id;
        check(w.editor.apply(w.editor.sourceOfficer(violent,10,true,false)).ok&&w.life.life(violent).expectedDeath==0,"violent death year never becomes natural lifespan");
        World all=empty();
        for(ContentCatalog.Officer o:c.officers())ContentProfiles.add(all,c,o.id,10,false,true);
        check(all.officers.size()==672,"all 670 source profiles can enter actual world without ID collisions");
        for(ContentCatalog.Relation r:c.relations())check(all.relations.links(r.officer,r.kind).contains(r.target),"resolved source relation applied: "+r.officer+"/"+r.target);
        byte[] full=bytes(all);check(Arrays.equals(full,bytes(SaveCodec.decode(full))),"full roster and relations round trip independently of catalog");
        Collections.reverse(all.officers);check(all.relations.sworn(guan,zhang),"identity binding survives roster reordering");
        World old=TestScenarios.load("regional-sandbox",0);check(old.life.people().isEmpty()&&old.relations.people.isEmpty(),"old original opening gains no source state implicitly");
        World changed=SaveCodec.decode(bytes(reference));changed.relations.unlink(guan,liu,Relations.Kind.SWORN);changed.contests.configure(zhang,new Contests.Profile(Debate.Temper.CALM,0,0));
        changed=SaveCodec.decode(bytes(changed));check(!changed.relations.sworn(guan,liu)&&changed.contests.profile(zhang).temper==Debate.Temper.CALM,"loading never reapplies static relationships or personality");
        System.out.println("PASS: "+checks+" sourced-profile assertions: full 670 roster, relations, birth/appearance, temperament, atomic imports and saved edits.");
    }
}
