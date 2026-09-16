package game.sanguo.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;

/** Rules from the official manual, command atomicity, campaign settlement, corrupt saves and replay. */
public final class ContestTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    private static void reject(World w,Supplier<World.Result> call)throws Exception{byte[] before=bytes(w);check(!call.get().ok,"invalid command rejected");check(Arrays.equals(before,bytes(w)),"rejection changes neither state nor RNG");}
    static World fixture(){
        World w=new World(12,10,"文武营","对阵营");w.scenarioId="contest-test";
        w.cities.add(new World.City(10,"东营",new Hex(2,3),0));w.cities.add(new World.City(20,"西营",new Hex(8,3),1));
        for(int i=0;i<6;i++){int owner=i<3?0:1;World.Officer o=new World.Officer(i,"演练将"+i,owner,owner==0?10:20,80,80,85,80,80);w.officers.add(o);}
        World.Officer target=new World.Officer(6,"隐士",-1,10,60,65,78,70,80);w.officers.add(target);
        World.Unit a=new World.Unit(1,0,0,World.Weapon.SPEAR,new Hex(3,3),5000,10000);a.deputies=new int[]{1};
        World.Unit b=new World.Unit(2,1,3,World.Weapon.SPEAR,new Hex(4,3),5000,10000);b.deputies=new int[]{4};
        w.units.add(a);w.units.add(b);w.nextUnitId=3;
        for(World.Unit u:w.units)for(World.Officer o:w.army.crew(u)){o.cityId=-1;o.unitId=u.id;}
        for(World.Officer o:w.officers)w.contests.configure(o.id,new Contests.Profile(Debate.Temper.values()[o.id%4],31,0));
        w.strategy.initializeOffices();return w;
    }
    private static void start(World w)throws Exception{
        for(long seed=0;seed<100;seed++){w.strategy.setSeed(seed);World probe=copy(w);ok(probe.contests.challenge(1,2));if(probe.contests.busy()){ok(w.contests.challenge(1,2));return;}}
        throw new AssertionError("acceptance seed missing");
    }
    private static World.Result duel(World w,Duel.Move move,int replacement){Contests.Session s=w.contests.current();return w.contests.duelMove(s.id(),s.revision(),Duel.Stance.SPIRIT,move,replacement);}
    private static World.Result play(World w,int index){Contests.Session s=w.contests.current();return w.contests.debateCard(s.id(),s.revision(),index);}
    private static World.Result rethink(World w){Contests.Session s=w.contests.current();return w.contests.rethink(s.id(),s.revision());}
    private static World.Result finish(World w,boolean mercy){Contests.Session s=w.contests.current();return w.contests.finishDebate(s.id(),s.revision(),mercy);}
    private static void locks()throws Exception{
        World w=fixture();reject(w,()->w.contests.challenge(1,999));w.unit(2).hex=new Hex(6,3);reject(w,()->w.contests.challenge(1,2));w.unit(2).hex=new Hex(4,3);
        start(w);Contests.Session s=w.contests.current();int id=s.id(),rev=s.revision();
        reject(w,w::nextTurn);reject(w,()->w.train(10,2));reject(w,()->w.move(1,new Hex(3,2)));reject(w,()->w.attack(1,2));reject(w,()->w.enter(1,10));
        reject(w,()->w.contests.persuade(10,2,6));reject(w,()->w.contests.challenge(1,2));reject(w,()->w.contests.duelMove(id,rev,null,Duel.Move.EXCHANGE,-1));
        reject(w,()->duel(w,Duel.Move.SPECIAL,-1));reject(w,()->duel(w,Duel.Move.HIDDEN,-1));reject(w,()->duel(w,Duel.Move.FEIGN,-1));reject(w,()->duel(w,Duel.Move.SWAP,1));
        ok(duel(w,Duel.Move.EXCHANGE,-1));reject(w,()->w.contests.duelMove(id,rev,Duel.Stance.ATTACK,Duel.Move.EXCHANGE,-1));
        World restored=copy(w);reject(restored,restored::nextTurn);check(restored.contests.current().duel().round()==1,"active duel round survives save");
    }
    private static void mechanics()throws Exception{
        World w=fixture();start(w);Duel d=w.contests.current().duel();d.active(0).spirit=300;
        ok(duel(w,Duel.Move.MUSOU,-1));check(d.active(0).spirit<100,"musou spends all 300 spirit");
        World v=fixture();v.contests.configure(0,new Contests.Profile(Debate.Temper.CALM,31,(1<<Contests.Gear.BOW.ordinal())|(1<<Contests.Gear.HIDDEN.ordinal())));start(v);
        ok(duel(v,Duel.Move.HIDDEN,-1));reject(v,()->duel(v,Duel.Move.HIDDEN,-1));
        Duel vd=v.contests.current().duel();vd.round=15;v.contests.current().revision=15;
        vd.active(0).hp=100;vd.active(1).hp=100;ok(duel(v,Duel.Move.FEIGN,-1));check(vd.active(1).wounds==1,"feigned retreat injures");
        reject(v,()->duel(v,Duel.Move.FEIGN,-1));
        World swap=fixture();start(swap);Duel sd=swap.contests.current().duel();sd.left.get(1).joined=true;sd.left.get(0).hp=40;
        ok(duel(swap,Duel.Move.SWAP,1));check(sd.active(0).officer==1&&sd.left.get(0).hp==40,"switch preserves injured reserve health");
        World loss=fixture();start(loss);Duel ld=loss.contests.current().duel();ld.active(0).hp=1;ld.left.get(1).joined=true;ld.active(1).spirit=300;
        int id=loss.contests.current().id(),rev=loss.contests.current().revision();ok(loss.contests.concede(id,rev));
        check(!loss.contests.busy()&&loss.unit(1)==null&&loss.government.captive(0)&&!loss.government.captive(1)&&loss.officer(1).cityId==10,"leader loss ends battle, captures only loser, retires healthy deputy");
        reject(loss,()->loss.contests.concede(id,rev));bytes(loss);
        World deputy=fixture();start(deputy);Duel dd=deputy.contests.current().duel();dd.leftIndex=1;dd.left.get(1).joined=true;
        ok(deputy.contests.concede(deputy.contests.current().id(),0));check(deputy.unit(1)!=null&&deputy.unit(1).deputies.length==0&&deputy.government.captive(1),"deputy defeat removes only deputy");bytes(deputy);
        World horse=fixture();horse.contests.configure(0,new Contests.Profile(Debate.Temper.CALM,0,1<<Contests.Gear.HORSE.ordinal()));start(horse);horse.contests.current().duel().active(0).spirit=100;
        ok(duel(horse,Duel.Move.RETREAT,-1));check(!horse.contests.busy()&&horse.unit(1)!=null&&!horse.government.captive(0),"horse retreat preserves unit");
        World draw=fixture();start(draw);Duel drawD=draw.contests.current().duel();drawD.round=49;draw.contests.current().revision=49;drawD.active(0).hp=100;drawD.active(1).hp=100;
        ok(duel(draw,Duel.Move.EXCHANGE,-1));check(!draw.contests.busy()&&draw.unit(1)!=null&&draw.unit(2)!=null&&draw.contests.lastResult().contains("平手"),"fifty rounds is draw");
    }
    private static Debate.Card topic(Debate.Topic topic,int size){return new Debate.Card(topic,size,null);}
    private static Debate.Card talk(Debate.Talk talk){return new Debate.Card(null,0,talk);}
    private static void hand(Debate.Speaker s,Debate.Card first){s.hand.clear();s.hand.add(first);while(s.hand.size()<5)s.hand.add(topic(Debate.Topic.HISTORY,1));}
    private static World debate(Debate.Temper temper)throws Exception{
        World w=fixture();w.contests.configure(2,new Contests.Profile(temper,31,0));w.contests.configure(6,new Contests.Profile(Debate.Temper.RASH,31,0));ok(w.contests.persuade(10,2,6));return w;
    }
    private static void cards()throws Exception{
        check(Debate.compare(topic(Debate.Topic.REASON,1),topic(Debate.Topic.HISTORY,3),Debate.Topic.REASON)>0,"on-topic small beats off-topic big");
        check(Debate.compare(topic(Debate.Topic.HISTORY,3),topic(Debate.Topic.HISTORY,2),Debate.Topic.REASON)>0,"same topic compares size");
        check(Debate.compare(topic(Debate.Topic.HISTORY,3),topic(Debate.Topic.TIMING,1),Debate.Topic.REASON)==0,"different off-topic cards draw");
        List<Debate.Card> order=Arrays.asList(talk(Debate.Talk.IGNORE),talk(Debate.Talk.SHOUT),talk(Debate.Talk.GUILE),topic(Debate.Topic.REASON,1),talk(Debate.Talk.CALM),talk(Debate.Talk.RAGE));
        for(int i=0;i<order.size();i++)for(int j=0;j<order.size();j++)check(Integer.signum(Debate.compare(order.get(i),order.get(j),Debate.Topic.REASON))==Integer.signum(j-i),"all topic/special priorities");
        World w=debate(Debate.Temper.CALM);Debate d=w.contests.current().debate();hand(d.left,talk(Debate.Talk.RAGE));hand(d.right,talk(Debate.Talk.SHOUT));d.aiCard=0;d.left.anger=0;
        ok(play(w,0));check(d.left.hp<100&&d.left.anger>=50,"rage applies after incoming damage");
        World reflect=debate(Debate.Temper.CALM);Debate rd=reflect.contests.current().debate();hand(rd.left,talk(Debate.Talk.RAGE));hand(rd.right,talk(Debate.Talk.GUILE));rd.aiCard=0;
        ok(play(reflect,0));check(rd.left.anger==0&&rd.right.anger==50,"guile reflects rage rather than damaging");
        World rethink=debate(Debate.Temper.CALM);ok(rethink(rethink));reject(rethink,()->rethink(rethink));
        World sealed=debate(Debate.Temper.CALM);Debate sealedD=sealed.contests.current().debate();sealedD.right.fury=2;
        // RASH does not seal; only CALM does. Use the opponent profile before constructing a fresh game.
        World calm=fixture();calm.contests.configure(6,new Contests.Profile(Debate.Temper.CALM,31,0));ok(calm.contests.persuade(10,2,6));Debate cd=calm.contests.current().debate();hand(cd.left,talk(Debate.Talk.SHOUT));cd.right.fury=2;
        reject(calm,()->play(calm,0));check(cd.cardError(1)==null,"topic remains playable under seal");
    }
    private static void fury()throws Exception{
        for(Debate.Temper temper:Debate.Temper.values()){
            World w=debate(temper);Debate d=w.contests.current().debate();hand(d.left,topic(Debate.Topic.HISTORY,1));hand(d.right,talk(Debate.Talk.IGNORE));d.aiCard=0;d.left.anger=70;
            int right=d.right.hp;ok(play(w,0));check(d.left.anger==0,"fury consumes anger");
            if(temper==Debate.Temper.CALM||temper==Debate.Temper.BOLD)check(d.left.fury==3,"sustained personality fury");
            else check(d.right.hp<right,"burst personality fury inflicts damage");
            bytes(w);
        }
        World counter=debate(Debate.Temper.RASH);Debate c=counter.contests.current().debate();hand(c.left,topic(Debate.Topic.HISTORY,1));hand(c.right,talk(Debate.Talk.IGNORE));c.right.hand.set(1,talk(Debate.Talk.CALM));c.aiCard=0;c.left.anger=70;
        ok(play(counter,0));check(c.right.hp==100&&c.report.contains("平息"),"held calm stops burst before damage");
        World reverse=debate(Debate.Temper.RASH);Debate r=reverse.contests.current().debate();hand(r.left,topic(Debate.Topic.HISTORY,1));hand(r.right,talk(Debate.Talk.IGNORE));r.right.hand.set(1,talk(Debate.Talk.RAGE));r.right.hand.set(2,talk(Debate.Talk.CALM));r.aiCard=0;r.left.anger=70;
        ok(play(reverse,0));check(r.left.hp<100&&r.right.hp==100&&r.report.contains("逆上反制"),"held rage has priority over calm and reverses fury");bytes(reverse);
    }
    private static void settlement()throws Exception{
        World w=debate(Debate.Temper.CALM);check(w.city(10).gold==4900&&w.actionPoints[0]==50&&w.officer(2).acted,"start pays city action once");
        reject(w,w::nextTurn);reject(w,()->w.strategy.recruitOfficer(10,2,6));reject(w,()->finish(w,true));
        Debate d=w.contests.current().debate();hand(d.left,talk(Debate.Talk.SHOUT));hand(d.right,topic(Debate.Topic.HISTORY,1));d.aiCard=0;d.right.hp=1;d.right.stage=3;
        ok(play(w,0));check(d.winner==0&&w.officer(6).owner==-1&&w.contests.busy(),"winning waits for explicit result choice");
        World restored=copy(w);Contests.Session s=restored.contests.current();int id=s.id(),rev=s.revision(),tp=restored.campaign.points(0);ok(finish(restored,true));
        check(restored.officer(6).owner==0&&restored.officer(6).cityId==10&&restored.officer(6).acted&&restored.campaign.points(0)==tp+50,"result applies actual allegiance and mercy points");
        World settled=restored;reject(settled,()->settled.contests.finishDebate(id,rev,true));check(Arrays.equals(bytes(restored),bytes(copy(restored))),"settled result round trip");
    }
    private static void simulations()throws Exception{
        for(int seed=0;seed<32;seed++){
            World w=fixture();w.strategy.setSeed(seed);ok(w.contests.challenge(1,2));
            while(w.contests.busy()){
                World clone=copy(w);Duel d=w.contests.current().duel();Duel.Move move=d.active(0).spirit>=100?Duel.Move.SPECIAL:Duel.Move.EXCHANGE;
                ok(duel(w,move,-1));ok(duel(clone,move,-1));check(Arrays.equals(bytes(w),bytes(clone)),"duel deterministic after save each exchange");
            }
            World b=fixture();b.contests.configure(2,new Contests.Profile(Debate.Temper.values()[seed%4],31,63));b.strategy.setSeed(seed);ok(b.contests.persuade(10,2,6));
            int guard=0;while(b.contests.current().debate().winner()==-2){
                check(guard++<101,"debate terminates");Debate state=b.contests.current().debate();int index=0;while(state.cardError(index)!=null)index++;
                World clone=copy(b);ok(play(b,index));ok(play(clone,index));check(Arrays.equals(bytes(b),bytes(clone)),"debate deterministic after save each round");
            }
            ok(finish(b,seed%2==0));bytes(b);
        }
    }
    private static void saveAndCorruption()throws Exception{
        World drill=ScenarioCatalog.load("contest-drill",0);check(drill.units.size()==2&&drill.contests.profiles.size()==10,"real drill has deployed units and explicit profiles");
        check(drill.unit(1).hex.distance(drill.unit(2).hex)==1&&drill.army.crew(drill.unit(1)).size()==2,"drill is immediately playable");
        byte[] drillSave=bytes(drill);check(Arrays.equals(drillSave,bytes(SaveCodec.decode(drillSave))),"scenario profiles and initial units survive standalone save");
        String opening;try(InputStream in=ContestTest.class.getResourceAsStream("/scenarios/contest-drill.properties")){opening=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        for(String malformed:Arrays.asList(opening.replace("SPEAR|BOAT|4|3", "SPEAR|BOAT|3|3"),opening.replace("contest-profile.1=1|", "contest-profile.1=0|"),opening.replace("CALM|31|32", "CALM|32|32"),opening.replace("2|1|3|4|SPEAR", "2|1|3|1|SPEAR"))){
            try{ScenarioData.read(new ByteArrayInputStream(malformed.getBytes(java.nio.charset.StandardCharsets.UTF_8)),0);throw new AssertionError("invalid contest opening accepted");}catch(IOException expected){checks++;}
        }
        for(int version=1;version<=8;version++){
            try(InputStream in=ContestTest.class.getResourceAsStream("/legacy-v"+version+".sg11.b64")){
                if(in==null)continue;World w=SaveCodec.decode(Base64.getMimeDecoder().decode(in.readAllBytes()));check(!w.contests.busy()&&w.contests.profiles.isEmpty(),"old save gains no invented profiles");check(ByteBuffer.wrap(bytes(w),4,4).getInt()==20,"old save writes v19");
            }
        }
        World w=debate(Debate.Temper.BOLD);w.contests.current().debate().left.hand.add(talk(Debate.Talk.IGNORE));try{bytes(w);throw new AssertionError("oversized hand accepted");}catch(IOException expected){checks++;}
        World bad=fixture();start(bad);bad.contests.current().duel().leftIndex=9;try{bytes(bad);throw new AssertionError("bad active index accepted");}catch(IOException expected){checks++;}
        World injury=fixture();injury.contests.injuries.put(0,new Contests.Injury(2,3));check(injury.army.war(injury.unit(1))==70,"healthy unrelated deputy contributes half the advantage");injury.contests.injuries.put(1,new Contests.Injury(2,3));check(injury.army.war(injury.unit(1))==60,"injuries affect army stat");
        injury.turn=3;injury.contests.tick();check(injury.contests.injuries.isEmpty()&&injury.army.war(injury.unit(1))==80,"injuries recover at saved deadline");bytes(injury);
    }
    public static void main(String[] args)throws Exception{locks();mechanics();cards();fury();settlement();simulations();saveAndCorruption();System.out.println("PASS: "+checks+" contest assertions: official duel/debate structures, guard/gear/fury/counters, campaign settlement, locks, stale inputs, real v8 migration and save-per-round replay.");}
}
