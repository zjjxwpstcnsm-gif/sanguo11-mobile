package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/** Actual commands, failure atomicity, legacy fixture and battle outcomes, not catalog-count claims. */
public final class EstatesTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    private static World fixture()throws Exception{return TestScenarios.load("estates-drill",0,41);}
    private static void reject(World w,Supplier<World.Result> action)throws Exception{byte[] before=bytes(w);check(!action.get().ok,"invalid command rejected");check(Arrays.equals(before,bytes(w)),"no RNG, action, ownership or resource mutation on rejection");}
    private static void rejectDraft(World w,Editor.Draft draft)throws Exception{check(!draft.valid(),"invalid draft rejected: "+draft.summary);reject(w,()->w.editor.apply(draft));}
    private static void edit(World w,Editor.Draft draft){check(draft.valid(),draft.error);ok(w.editor.apply(draft));}
    private static void reset(World w){Arrays.fill(w.actionPoints,60);for(World.Officer o:w.officers)o.acted=false;}
    public static void main(String[] args)throws Exception{
        mediation();relationships();treasures();searchAndCapture();editor();templates();migration();malformed();simulation();
        System.out.println("PASS: "+checks+" estates assertions: mediation/relationships, 43 item identities/ownership/effects, paid search/reward/capture, atomic PK drafts, new-officer templates, real v11 migration and deterministic replay.");
    }
    private static void mediation()throws Exception{
        World w=fixture();int tp=w.campaign.points(0),war=w.officer(0).war;
        byte[] before=bytes(w);check(w.relations.mediateError(10,0,3,Relations.Kind.SPOUSE)==null,"marriage preview");check(Arrays.equals(before,bytes(w)),"preview is pure");
        ok(w.relations.mediate(10,0,3,Relations.Kind.SPOUSE));check(w.relations.spouse(0)==3&&w.relations.spouse(3)==0,"symmetric spouses");check(w.officer(0).war==war+1&&w.officer(3).war==war+1,"inner assistance boosts both once");check(w.campaign.points(0)==tp-500&&w.actionPoints[0]==60&&!w.officer(0).acted,"mediation costs only 500 TP");
        reject(w,()->w.relations.mediate(10,0,3,Relations.Kind.SPOUSE));reject(w,()->w.relations.mediate(10,1,2,Relations.Kind.SPOUSE));
        ok(w.relations.mediate(10,1,2,Relations.Kind.SWORN));ok(w.relations.mediate(10,2,4,Relations.Kind.SWORN));for(int id:new int[]{1,2,4})check(w.relations.links(id,Relations.Kind.SWORN).size()==2,"three-person group closes");
        reject(w,()->w.relations.mediate(10,0,1,Relations.Kind.SWORN));check(Arrays.equals(bytes(w),bytes(copy(w))),"marriage and sworn group roundtrip");
        World low=fixture();low.government.merits.put(1,499);reject(low,()->low.relations.mediate(10,1,2,Relations.Kind.SWORN));low.government.merits.put(1,500);low.campaign.points.put(0,499);reject(low,()->low.relations.mediate(10,1,2,Relations.Kind.SWORN));
        w.officer(3).loyalty=30;w.government.capture(w.officer(3),w.city(20));check(w.government.recruitChance(10,3)==0,"captured spouse loyal to living faction cannot be recruited");
    }
    private static void relationships()throws Exception{
        World w=fixture();World.Unit u=w.unit(1);w.officer(6).war=50;w.officer(7).war=100;check(w.army.war(u)==62,"ordinary one-quarter contribution");
        edit(w,w.editor.relation(6,7,Relations.Kind.LIKE,false));check(w.army.war(u)==75,"like one-half contribution");check(w.relations.supportChance(7,6)==0,"one-way liking does not grant reverse support");
        edit(w,w.editor.relation(7,6,Relations.Kind.LIKE,false));check(w.relations.supportChance(7,6)==30,"directed support");
        edit(w,w.editor.relation(6,7,Relations.Kind.SWORN,false));check(w.army.war(u)==100&&w.relations.supportChance(7,6)==50,"bonded maximum stat/support");
        rejectDraft(w,w.editor.relation(6,7,Relations.Kind.DISLIKE,false));
        edit(w,w.editor.relation(1,0,Relations.Kind.FATHER,false));check(w.relations.blood(0,1),"parent blood link");rejectDraft(w,w.editor.relation(0,1,Relations.Kind.MOTHER,false));rejectDraft(w,w.editor.relation(1,0,Relations.Kind.MOTHER,false));
        reject(w,()->w.relations.mediate(10,0,1,Relations.Kind.SWORN));
        World dislike=fixture();edit(dislike,dislike.editor.relation(11,0,Relations.Kind.DISLIKE,false));dislike.government.capture(dislike.officer(11),dislike.city(10));check(dislike.government.recruitChance(1,11)==0,"disliked ruler blocks recruitment by another envoy");
        World loyal=fixture();ok(loyal.relations.mediate(10,1,2,Relations.Kind.SWORN));loyal.city(10).order=0;loyal.city(10).gold=0;loyal.turn=3;int value=loyal.officer(1).loyalty;loyal.strategy.tick();check(loyal.officer(1).loyalty==value,"bond protects normal monthly loyalty decay");
    }
    private static void treasures()throws Exception{
        World w=fixture();check(w.treasures.items().size()==43,"all explicit placements are live");Set<String> ids=new HashSet<>();for(Treasures.Item item:w.treasures.items()){check(ids.add(item.definition.id),"unique item id");check(item.definition.value>=0,"item value");}
        int loyalty=w.officer(2).loyalty;ok(w.treasures.award(10,1,"item-000",2));check(w.treasures.has(2,Treasures.Kind.HORSE)&&w.contests.profile(2).has(Contests.Gear.HORSE),"horse gear derived from ownership");check(w.officer(2).loyalty==Math.min(100,loyalty+30),"award loyalty");
        final World initial=w;reject(initial,()->initial.treasures.award(10,4,"item-000",3));reset(w);ok(w.treasures.confiscate(10,1,"item-000"));check(!w.contests.profile(2).has(Contests.Gear.HORSE),"confiscation removes effect without stale mask");
        for(Treasures.Kind kind:Treasures.Kind.values())if(kind.gear!=null){Treasures.Item selected=null;for(Treasures.Item i:w.treasures.items())if(i.definition.kind==kind&&i.place==Treasures.Place.TREASURY){selected=i;break;}check(selected!=null,"test item exists");reset(w);ok(w.treasures.award(10,1,selected.definition.id,2));check(w.contests.profile(2).has(kind.gear),"runtime gear "+kind);w=copy(w);check(w.contests.profile(2).has(kind.gear),"saved gear "+kind);}
        reset(w);int tp=w.campaign.points(0);w.turn=3;w.treasures.tick();check(w.campaign.points(0)==tp+100,"seal monthly TP");
        reset(w);Hex site=w.domestic.buildSites(10).get(0);ok(w.domestic.build(10,1,Domestic.Kind.BRONZE_TERRACE,site));check(w.domestic.facilities.get(0).kind==Domestic.Kind.BRONZE_TERRACE,"bronze permits actual facility");
        World none=fixture();none.treasures.items.remove("item-042");Hex at=none.domestic.buildSites(10).get(0);reject(none,()->none.domestic.build(10,1,Domestic.Kind.BRONZE_TERRACE,at));
    }
    private static void searchAndCapture()throws Exception{
        World w=fixture();boolean found=false;for(int seed=0;seed<100&&!found;seed++){World p=copy(w);p.strategy.setSeed(seed);Strategy.SearchResult r=p.strategy.searchTalent(10,1);if(r.outcome==Strategy.SearchOutcome.TREASURE){check(p.actionPoints[0]==50&&p.officer(1).acted,"real search pays once");check(p.treasures.item("item-015").place==Treasures.Place.TREASURY,"discovery enters treasury");World q=copy(p);reset(q);q.strategy.search(10,1);check(q.treasures.items().size()==43,"no repeated spawn");found=true;}}
        check(found,"seeded actual treasure discovery");
        for(int seed=0;seed<30;seed++){World p=fixture();p.officer(6).skillId=Skill.BOFU.id;p.strategy.setSeed(seed);p.defeatUnit(p.unit(2),p.unit(1));check(!p.government.captive(11),"horse precedes capture guarantee");check(p.treasures.item("item-001").holder==11,"horse retained on protected retreat");}
        World captive=fixture();captive.treasures.place(Treasures.definition("item-001"),Treasures.Place.TREASURY,0);captive.treasures.place(Treasures.definition("item-007"),Treasures.Place.OFFICER,11);captive.officer(6).skillId=Skill.BOFU.id;captive.defeatUnit(captive.unit(2),captive.unit(1));check(captive.government.captive(11)&&captive.treasures.item("item-007").place==Treasures.Place.TREASURY&&captive.treasures.owner(captive.treasures.item("item-007"))==0,"capture transfers actual item once");bytes(captive);
        boolean robbed=false;for(int seed=0;seed<200&&!robbed;seed++){World p=fixture();p.officer(6).skillId=Skill.QIANGDUO.id;p.strategy.setSeed(seed);p.defeatUnit(p.unit(2),p.unit(1));if(p.treasures.item("item-001").holder==6){robbed=true;check(!p.government.captive(11),"robbery cannot retroactively remove horse protection");check(p.treasures.has(6,Treasures.Kind.HORSE),"strong robbery transfers real effect");bytes(p);}}check(robbed,"strong robbery can actually fire");
        World fallen=fixture();fallen.city(20).owner=0;fallen.government.cityCaptured(fallen.city(20),1,fallen.unit(1));fallen.treasures.place(Treasures.definition("item-040"),Treasures.Place.TREASURY,1);
        check(!fallen.alive(1),"remaining army does not keep a cityless faction alive");fallen.treasures.fallenTreasury(1,0);fallen.checkVictory();
        check(!fallen.alive(1)&&fallen.treasures.item("item-040").holder==0,"last army defeat transfers fallen treasury");bytes(fallen);
    }
    private static void editor()throws Exception{
        World w=fixture();Editor.Draft d=w.editor.faction(0,20,1500);byte[] before=bytes(w);check(d.valid()&&d.summary.contains("5000 → 1500"),"preview shows exact change");check(Arrays.equals(before,bytes(w)),"preview is pure");ok(w.editor.apply(d));reject(w,()->w.editor.apply(d));check(w.editor.edited()&&w.editor.revision()==1,"edited provenance saved");
        Editor.Draft stale=w.editor.faction(0,30,2000);w.city(10).gold++;reject(w,()->w.editor.apply(stale));rejectDraft(w,w.editor.faction(0,61,1));rejectDraft(w,w.editor.faction(9,30,1));
        World.City c=w.city(10);rejectDraft(w,w.editor.city(10,-1,c.food,c.troops,c.order,c.morale,c.defense,c.recruitReserve,c.equipment,c.ships));
        rejectDraft(w,w.editor.treasure("item-000",Treasures.Place.OFFICER,999));rejectDraft(w,w.editor.treasure("item-000",Treasures.Place.HIDDEN,999));
        int[] gear=c.equipment.clone();Editor.Draft frozen=w.editor.city(10,12345,c.food,c.troops,c.order,c.morale,c.defense,c.recruitReserve,gear,c.ships);gear[0]=-1;edit(w,frozen);check(c.gold==12345&&c.equipment[0]==30000,"caller array cannot mutate prepared edit");
        edit(w,w.editor.unit(1,7000,60000,4321,60,War.Status.CONFUSED,2));check(w.unit(1).troops==7000&&w.unit(1).gold==4321,"unit values changed");rejectDraft(w,w.editor.unit(1,0,1,1,1,War.Status.NORMAL,0));
        edit(w,w.editor.learnTechnology(0,Campaign.Tech.ELITE_SPEAR));check(w.campaign.has(0,Campaign.Tech.ELITE_SPEAR)&&w.campaign.has(0,Campaign.Tech.SUPPLY_RAID),"prerequisites actually granted");check(Arrays.equals(bytes(w),bytes(copy(w))),"all editor changes roundtrip");
        World busy=fixture();for(int seed=0;seed<100&&!busy.contests.busy();seed++){busy.strategy.setSeed(seed);busy.unit(1).acted=false;busy.unit(1).energy=90;busy.contests.challenge(1,2);}check(busy.contests.busy(),"real contest starts");rejectDraft(busy,busy.editor.faction(0,40,2000));
    }
    private static void templates()throws Exception{
        World w=fixture();Editor.Template t=new Editor.Template("子衿",new int[]{81,82,83,84,85},new int[]{0,1,2,3,1,2},World.Sex.FEMALE,Skill.NEIZHU.id,Debate.Temper.BOLD,31);
        byte[] data=OfficerTemplateCodec.encode(t);Editor.Template read=OfficerTemplateCodec.read(new ByteArrayInputStream(data));check(read.name.equals("子衿")&&read.stat(3)==84&&read.aptitude(3)==3&&read.talkMask==31,"portable template roundtrip");
        edit(w,w.editor.createOfficer(read,10,0));World.Officer o=w.officers.get(w.officers.size()-1);check(o.owner==0&&o.cityId==10&&o.war==82&&w.editor.custom(o.id),"new officer actually joins world");w=copy(w);check(w.editor.custom(o.id)&&w.idle(w.city(10)).contains(w.officer(o.id)),"created officer saved and playable");
        final World state=w;rejectDraft(state,state.editor.createOfficer(read,10,1));
        try{OfficerTemplateCodec.read(new ByteArrayInputStream(Arrays.copyOf(data,data.length+1)));throw new AssertionError("unknown tail");}catch(IOException e){checks++;}
        try{OfficerTemplateCodec.read(new ByteArrayInputStream(new byte[5000]));throw new AssertionError("oversize");}catch(IOException e){checks++;}
        for(int n=0;n<data.length;n+=7)try{OfficerTemplateCodec.read(new ByteArrayInputStream(Arrays.copyOf(data,n)));throw new AssertionError("truncated");}catch(IOException e){checks++;}
        reset(w);ok(w.deploy(10,o.id,World.Weapon.SPEAR,1000));check(w.officer(o.id).unitId>=0,"new officer deploys using real command");
    }
    private static void migration()throws Exception{
        byte[] raw;try(InputStream in=EstatesTest.class.getResourceAsStream("/legacy-v11.sg11.b64")){raw=Base64.getMimeDecoder().decode(in.readAllBytes());}
        check(raw[7]==11,"fixture is genuine previous writer");World w=SaveCodec.decode(raw);check(w.treasures.items().isEmpty()&&w.relations.people.isEmpty()&&!w.editor.edited(),"legacy gets no invented treasures or kinship");check(w.unit(1).gold==8500&&w.war.structures().size()==1&&w.war.structures().get(0).builder==1,"real v11 carried gold and ongoing construction survive");
        check(bytes(w)[7]==25&&Arrays.equals(bytes(w),bytes(copy(w))),"v11 migrates to v17 without loss");
    }
    private static void malformed()throws Exception{
        World w=fixture();w.relations.link(0,1,Relations.Kind.SPOUSE);w.relations.people.get(1).spouse=-1;try{bytes(w);throw new AssertionError("asymmetric spouse");}catch(IOException e){checks++;}
        w=fixture();w.treasures.place(Treasures.definition("item-000"),Treasures.Place.OFFICER,999);try{bytes(w);throw new AssertionError("missing holder");}catch(IOException e){checks++;}
        w=fixture();w.relations.link(0,1,Relations.Kind.FATHER);w.relations.link(1,0,Relations.Kind.FATHER);try{bytes(w);throw new AssertionError("parent cycle");}catch(IOException e){checks++;}
    }
    private static void simulation()throws Exception{
        World a=fixture();ok(a.relations.mediate(10,0,3,Relations.Kind.SPOUSE));ok(a.treasures.award(10,1,"item-007",2));World b=copy(a);
        for(int i=0;i<24&&!a.gameOver();i++){ok(a.nextTurn());ok(b.nextTurn());check(Arrays.equals(bytes(a),bytes(b)),"save-per-turn deterministic replay");b=copy(b);}
    }
}
