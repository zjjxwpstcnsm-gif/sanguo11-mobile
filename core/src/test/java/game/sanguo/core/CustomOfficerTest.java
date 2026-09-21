package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Real command/save integration; no test content enters a shipped scenario. */
public final class CustomOfficerTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void ok(World.Result result){check(result.ok,result.message);}
    interface Action{void run()throws Exception;}
    static void reject(Action action,String why)throws Exception{try{action.run();throw new AssertionError("Expected rejection: "+why);}catch(IOException|IllegalArgumentException expected){checks++;}}
    static World base(){World w=new World(30,20,"甲军","乙军");w.startYear=200;w.cities.add(new World.City(77,"甲都",new Hex(4,5),0));w.cities.add(new World.City(991,"乙都",new Hex(24,5),1));
        w.officers.add(new World.Officer(10,"甲君",0,77,60,60,60,60,60));w.officers.add(new World.Officer(20,"乙君",1,991,60,60,60,60,60));w.strategy.initializeOffices();for(World.City c:w.cities){c.gold=50000;c.food=200000;c.troops=30000;c.morale=90;c.order=60;Arrays.fill(c.equipment,0);c.equipment[0]=30000;}return w;}
    static CustomOfficers.Definition person(int id,String name){CustomOfficers.Definition d=new CustomOfficers.Definition();d.id=UUID.nameUUIDFromBytes(("test:"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();d.runtimeId=id;d.birth=160;d.appearance=180;d.death=270;d.template=new Editor.Template(name,new int[]{95,96,97,98,99},new int[]{3,3,3,3,3,3},World.Sex.MALE,Skill.BAICHU.id,Debate.Temper.BOLD,7);return d;}
    static CustomOfficers.Placement put(CustomOfficers.Definition d,int owner,int city){return new CustomOfficers.Placement(d.id,owner<0?CustomOfficers.Mode.WILD:CustomOfficers.Mode.FACTION,owner,city,90,false);}
    static World apply(World w,List<CustomOfficers.Definition> ds,List<CustomOfficers.Placement> ps)throws IOException{return CustomOfficers.apply(w,ds,ps,"{\"test\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static void reset(World w){Arrays.fill(w.actionPoints,60);for(World.Officer o:w.officers)o.acted=false;for(World.Unit u:w.units)w.orders.reset(u);}
    public static void main(String[] args)throws Exception{
        gameplay();relations();datesAndIds();overrides();snapshots();capacity();
        System.out.println("CUSTOM OFFICERS PASS: "+checks+" assertions; domestic, formation, battle, skill, recruitment, AI, relations, historical isolation, dynamic sites, dates, IDs and self-contained saves.");
    }
    static void gameplay()throws Exception{
        World initial=base();byte[] before=SaveCodec.encode(initial);CustomOfficers.Definition a=person(100000,"自定义主将"),b=person(100001,"自定义在野"),ai=person(100002,"自定义AI");
        World w=apply(initial,Arrays.asList(a,b,ai),Arrays.asList(put(a,0,77),put(b,-1,77),put(ai,1,991)));check(Arrays.equals(before,SaveCodec.encode(initial)),"composition leaves base untouched");
        World.Officer o=w.officer(a.runtimeId);check(o.leadership==95&&o.affinity==75&&o.honor==3&&o.role==Strategy.Role.OFFICER&&!o.acted,"actual officer instance contains base traits and legal defaults");
        check(w.contests.profile(o.id).temper==Debate.Temper.BOLD&&w.contests.profile(o.id).talkMask==7,"personality and talk enter contest model");
        int order=w.city(77).order;ok(w.patrol(77,o.id));check(w.city(77).order>order&&o.acted,"real domestic command changes city and consumes action");check(!w.patrol(77,o.id).ok,"no unlimited actions");reset(w);
        check(w.strategy.recruitmentTargets(77).stream().anyMatch(x->x.id==b.runtimeId),"custom wild officer in ordinary recruitment candidates");
        int chance=w.strategy.recruitmentChance(77,o.id,b.runtimeId);for(int seed=0;seed<10000;seed++){w.strategy.setSeed(seed);if(w.strategy.nextInt(100)<chance){w.strategy.setSeed(seed);break;}}
        ok(w.strategy.recruitOfficer(77,o.id,b.runtimeId));check(w.officer(b.runtimeId).owner==0,"wild officer really recruited via ordinary formula");reset(w);
        ok(w.army.deploy(77,o.id,new int[]{b.runtimeId},World.Weapon.SPEAR,Army.Ship.BOAT,5000,10000));World.Unit u=w.units.get(0);check(u.officerId==o.id&&u.deputies.length==1,"custom crew enters real army");
        check(w.combat.attackRating(u)>0&&w.combat.defenseRating(u)>0,"official attack and defense calculations usable");check(w.war.plotCost(u.id,War.Plot.CONFUSE)==1,"selected baichu has an actual effect");
        double high=w.combat.attackRating(u);int aptitude=o.aptitude[0];o.aptitude[0]=0;w.officer(b.runtimeId).aptitude[0]=0;check(w.combat.attackRating(u)<high,"formation aptitude affects combat rating");o.aptitude[0]=aptitude;
        World.Unit enemy=new World.Unit(w.nextUnitId++,1,ai.runtimeId,World.Weapon.SPEAR,new Hex(14,10),5000,10000);w.units.add(enemy);w.officer(ai.runtimeId).cityId=-1;w.officer(ai.runtimeId).unitId=enemy.id;u.hex=new Hex(13,10);reset(w);int troops=enemy.troops;ok(w.attack(u.id,enemy.id));check(enemy.troops<troops,"actual combat damages opponent");
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(SaveCodec.decode(SaveCodec.encode(w)))),"post-command state serializes with normal unit refs");
        World aiWorld=apply(base(),Collections.singletonList(ai),Collections.singletonList(put(ai,1,991)));ok(aiWorld.nextTurn());check(aiWorld.officer(ai.runtimeId).acted||aiWorld.officer(ai.runtimeId).unitId>=0||aiWorld.officer(ai.runtimeId).otherTaskTurns>0,"AI consumes custom officer through regular planning");
    }
    static void relations()throws Exception{
        CustomOfficers.Definition a=person(100010,"关系甲"),b=person(100011,"关系乙");a.links.add(new CustomOfficers.Link(Relations.Kind.SPOUSE,b.id));World w=apply(base(),Arrays.asList(a,b),Arrays.asList(put(a,0,77),put(b,0,77)));check(w.relations.spouse(a.runtimeId)==b.runtimeId&&w.relations.spouse(b.runtimeId)==a.runtimeId,"symmetric relation closed by authoritative engine");check(w.relations.contribution(a.runtimeId,b.runtimeId,60,95)==95,"relation affects actual deputy contribution");
        a.links.clear();a.links.add(new CustomOfficers.Link(Relations.Kind.LIKE,"h:1004"));World national=ScenarioCatalog.load("heroes-250",0);World.City city=national.cities.stream().filter(c->c.owner==0).findFirst().orElseThrow();a.death=300;World linked=apply(national,Collections.singletonList(a),Collections.singletonList(put(a,0,city.id)));check(linked.relations.likes(a.runtimeId,1004)&&!linked.relations.likes(1004,a.runtimeId),"new-to-historical directed like preserved");
        a.links.clear();a.links.add(new CustomOfficers.Link(Relations.Kind.SPOUSE,b.id));reject(()->apply(base(),Arrays.asList(a,b),Collections.singletonList(put(a,0,77))),"missing enabled relationship dependency");reject(()->CustomOfficers.validateDefinitions(Collections.singletonList(a)),"missing library reference");
        a.links.clear();a.links.add(new CustomOfficers.Link(Relations.Kind.FATHER,b.id));b.links.add(new CustomOfficers.Link(Relations.Kind.FATHER,a.id));a.birth=0;b.birth=0;reject(()->CustomOfficers.validateDefinitions(Arrays.asList(a,b)),"parent cycles");
        b.links.clear();a.links.clear();a.links.add(new CustomOfficers.Link(Relations.Kind.LIKE,a.id));reject(()->CustomOfficers.validateDefinitions(Arrays.asList(a,b)),"self links");
    }
    static void datesAndIds()throws Exception{
        CustomOfficers.Definition d=person(100020,"未登场将");d.birth=210;d.appearance=230;World w=apply(base(),Collections.singletonList(d),Collections.singletonList(new CustomOfficers.Placement(d.id,CustomOfficers.Mode.UNAPPEARED,-1,991,0,false)));check(w.life.state(d.runtimeId)==Lifecycle.State.UNAPPEARED&&w.officer(d.runtimeId).cityId==-1&&!w.life.present(d.runtimeId),"future person not prematurely usable");
        w.startYear=229;w.startMonth=12;w.turn=3;w.life.tick();check(w.life.present(d.runtimeId)&&w.officer(d.runtimeId).cityId==991,"scheduled person appears in stable configured home");
        reject(()->apply(base(),Collections.singletonList(d),Collections.singletonList(put(d,0,77))),"unborn active placement");World ignored=apply(base(),Collections.singletonList(d),Collections.singletonList(new CustomOfficers.Placement(d.id,CustomOfficers.Mode.FACTION,0,77,85,true)));check(ignored.life.present(d.runtimeId)&&ignored.life.life(d.runtimeId).birth==0,"explicit ignore never feeds impossible dates into lifecycle");
        d.birth=160;d.appearance=180;reject(()->apply(base(),Collections.singletonList(d),Collections.singletonList(put(d,0,999999))),"missing actual map site");reject(()->apply(base(),Collections.singletonList(d),Collections.singletonList(put(d,0,991))),"owner-site mismatch");
        World collision=base();collision.officers.add(new World.Officer(d.runtimeId,"其他人物",0,77,50,50,50,50,50));reject(()->apply(collision,Collections.singletonList(d),Collections.singletonList(put(d,0,77))),"runtime ID collision");
        reject(()->apply(base(),Arrays.asList(d,d),Collections.singletonList(put(d,0,77))),"duplicate library IDs");
    }
    static void overrides()throws Exception{
        World source=ScenarioCatalog.load("heroes-250",0);ContentCatalog.Officer h=ContentCatalog.get().officer(1004);CustomOfficers.Definition d=CustomOfficers.historical(h,true);d.runtimeId=100030;d.template=new Editor.Template("自定义诸葛",new int[]{85,84,83,82,81},new int[]{3,3,3,3,3,3},World.Sex.MALE,Skill.BAICHU.id,Debate.Temper.CALM,0);d.replaceRelations=true;
        World.Officer original=source.officer(h.id);CustomOfficers.Placement p=new CustomOfficers.Placement(d.id,CustomOfficers.Mode.KEEP,-1,original.cityId,85,true);World changed=apply(source,Collections.singletonList(d),Collections.singletonList(p));
        check(changed.officer(h.id).name.equals("自定义诸葛")&&changed.officer(h.id).intelligence==83&&changed.officer(h.id).role==original.role&&changed.officer(h.id).owner==original.owner,"override preserves identity and office but changes actual attributes");
        check(source.officer(h.id).name.equals(original.name)&&ScenarioCatalog.load("heroes-250",0).officer(h.id).intelligence==original.intelligence,"defaults and disabled new games unmodified");
        for(Relations.Kind kind:Relations.Kind.values())check(changed.relations.links(h.id,kind).isEmpty(),"explicit relationship replacement removed outgoing "+kind);
        CustomOfficers.Definition other=CustomOfficers.historical(h,true);other.runtimeId=100031;reject(()->apply(source,Arrays.asList(d,other),Arrays.asList(p,new CustomOfficers.Placement(other.id,CustomOfficers.Mode.KEEP,-1,original.cityId,85,true))),"two selected versions of same history");d.baseName="另一人";reject(()->apply(source,Collections.singletonList(d),Collections.singletonList(p)),"stale historical target fingerprint");
    }
    static void snapshots()throws Exception{
        CustomOfficers.Definition d=person(100040,"持久将");d.portrait="builtin:3";World w=apply(base(),Collections.singletonList(d),Collections.singletonList(put(d,0,77)));byte[] file=SaveCodec.encode(w);d.template=person(100040,"后来改名").template;World restored=SaveCodec.decode(file);check(restored.officer(100040).name.equals("持久将")&&CustomOfficers.portrait(restored,100040).ref.equals("builtin:3"),"saved definition and portrait survive template mutation/deletion");
        World vanilla=base();byte[] old=SaveCodec.encode(vanilla);check(new java.io.DataInputStream(new ByteArrayInputStream(Arrays.copyOfRange(old,4,8))).readInt()==32,"global save version stays32");check(Arrays.equals(old,SaveCodec.encode(SaveCodec.decode(old))),"old plain save roundtrip unchanged");
        w.extensions.put("customMaps",new byte[]{1,2,3});World combined=SaveCodec.decode(SaveCodec.encode(w));check(Arrays.equals(combined.extensions.get("customMaps"),new byte[]{1,2,3}),"unknown optional module roundtrip is composable (not map-agent integration)");
        reject(()->{World corrupt=base();corrupt.extensions.put(CustomOfficers.NAMESPACE,new byte[]{0,0,0,9});SaveCodec.encode(corrupt);},"bad module version");
    }
    static void capacity()throws Exception{
        List<CustomOfficers.Definition> defs=new ArrayList<>();List<CustomOfficers.Placement> placements=new ArrayList<>();for(int i=0;i<400;i++){CustomOfficers.Definition d=person(101000+i,"批量将"+i);defs.add(d);placements.add(put(d,0,77));}
        World w=apply(base(),defs,placements);check(w.officers.size()==402,"dynamic roster is not constrained by historical count or small-ID cache");Collections.reverse(w.officers);for(CustomOfficers.Definition d:defs)check(w.officer(d.runtimeId).name.equals(d.template.name),"stable ID after reorder "+d.runtimeId);check(SaveCodec.decode(SaveCodec.encode(w)).officers.size()==402,"batch roster save capacity");
    }
}
