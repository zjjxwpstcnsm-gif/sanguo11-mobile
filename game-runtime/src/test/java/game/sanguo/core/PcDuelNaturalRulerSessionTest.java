package game.sanguo.core;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.GameSession;

/** Original natural AI terminal plus actual typed human creator/inputs.
 * Declared adjacent formations remain distinct from ordinary deployment/APK. */
public final class PcDuelNaturalRulerSessionTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static int stable(World w,int nativeId)throws Exception{return PcDuelNaturalDeathSessionTest.stable(w,nativeId);}
    static void cold(GameSession g)throws Exception{
        byte[] saved=g.captureSave();g.replace(SaveCodec.decode(saved));
        check(Arrays.equals(saved,g.captureSave()),"whole World/model/dual RNG cold exact");
    }
    static void corruptPending(byte[] pending,java.util.function.Consumer<PcDuelCampaign> change,String reason)throws Exception{
        World copy=SaveCodec.decode(pending);change.accept(copy.contests.session.nativeDuel);boolean rejected=false;
        try{SaveCodec.decode(SaveCodec.encode(copy));}catch(IOException e){rejected=true;}
        check(rejected,reason);
    }
    static World formation(int player,int seed,int leftNative,int rightNative)throws Exception{
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,player,seed,new PcDuelOptions(0,2,0));
        Hex first=null;
        outer:for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
            Hex a=new Hex(q,r),b=new Hex(q+1,r);
            if(w.inside(b)&&w.cityAt(a)==null&&w.cityAt(b)==null&&!w.army.water(a)&&!w.army.water(b)&&w.cost(a,World.Weapon.SWORD)>0&&w.cost(b,World.Weapon.SWORD)>0){first=a;break outer;}
        }
        check(first!=null,"legal declared adjacent cells");
        for(int side=0;side<2;side++){
            var o=w.officer(stable(w,side==0?leftNative:rightNative));var home=w.city(o.cityId);
            var u=new World.Unit(side+1,o.owner,o.id,World.Weapon.SWORD,new Hex(first.q+side,first.r),5000,17000);
            u.gold=997;u.energy=100;w.units.add(u);w.strategy.releaseGovernor(o.id);o.unitId=u.id;o.cityId=-1;
            w.districts.deployed(home.id,u);PcGovernorPolicy.deployed(w,u,home);
        }
        w.nextUnitId=3;w.governance.reconcile(false);return w;
    }
    static World endpoint(Path path)throws Exception{
        Map<String,String>d=new HashMap<>();for(String line:Files.readAllLines(path)){if(line.startsWith("#"))continue;var p=line.split("\t");d.put(p[0],p[1]);}
        World w=formation(2,1,365,517);int[] natives={365,-1,-1,517,-1,-1},ids={stable(w,365),-1,-1,stable(w,517),-1,-1};
        check(d.get("NATIVES").equals("365,-1,-1,517,-1,-1"),"pinned solo identities");
        int seed=(int)Long.parseLong(d.get("SEED"));
        var state=PcDuelSettlementSessionTest.normalized(PcDuelKernel.hex(d.get("MODEL")),PcDuelKernel.hex(d.get("MANAGER")),seed,0,ids,natives);
        for(int side=0;side<2;side++){PcDuelKernel.writeManager(state.manager,0x18+4*side,side+1);w.unit(side+1).acted=true;w.officer(ids[side*3]).acted=true;}
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeInt(5);out.writeBoolean(true);out.writeInt(0);out.writeBoolean(true);out.writeInt(0);out.writeBoolean(true);out.writeInt(2);
        out.writeBoolean(true);out.writeInt(-1);out.writeInt(198);out.writeInt(0);
        byte[] model=PcDuelModelSave.write(state);out.writeInt(model.length);out.write(model);out.writeInt(0);
        var duel=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        PcNativeDebatePolicy.setSeed(w,seed);w.contests.nextId=2;w.contests.session=new Contests.Session(1,w.player,w.turn,1,2,-1);w.contests.session.nativeDuel=duel;
        return w;
    }
    static void future(GameSession game,int dead,int heir)throws Exception{
        for(int turn=0;turn<3;turn++){
            var ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());var next=computed.nextTurn();
            check(next.ok,"complete AI/personnel turn "+next.message);check(game.commitTurn(ticket,computed),"serial turn commit");cold(game);
            var w=SaveCodec.decode(game.captureSave());check(w.life.state(dead)==Lifecycle.State.DEAD&&w.loyalty.ruler(3).id==heir,"death and actual crown persist");
        }
    }
    static void ai(Path path,Path original)throws Exception{
        World w=endpoint(path);int dead=stable(w,517),heir=stable(w,109),actor=stable(w,365);var expected=PcDuelNaturalDeathSessionTest.original(original);
        byte[] before=SaveCodec.encode(w);Files.write(Path.of("out/session-b/natural-ruler-ai-pending-v1.sg11"),before);
        try(GameSession game=new GameSession(SaveCodec.decode(before))){
            var f=game.contest();check(f.nativeDuel.terminal&&f.settlementAvailable&&f.nativeDuel.inheritance==null&&f.nativeDuel.disposition.isEmpty(),"AI natural death preview available with no captive choice");
            check(Arrays.equals(before,game.captureSave()),"terminal query byte pure");var token=game.state();
            var r=game.execute(ContestCommand.finishNativeDuel(token,f.contestId,f.revision));check(r.ok(),"actual natural AI crown "+r.detail);
            byte[] finished=game.captureSave();World after=SaveCodec.decode(finished);Files.write(Path.of("out/session-b/natural-ruler-ai-finished-v1.sg11"),finished);
            check(!after.contests.busy()&&after.life.state(dead)==Lifecycle.State.DEAD&&after.loyalty.ruler(3).id==heir,"source native109 crown and517 death");
            check(after.unit(2)==null&&after.unit(1).troops==5000&&after.unit(1).energy==100&&after.unit(1).gold==997&&after.unit(1).food==17000,"source lone losing unit deletion; no cargo transfer or troop draw");
            check(Integer.toUnsignedLong(PcNativeDebatePolicy.seed(after))==773516763L&&after.strategy.getRandomState()==w.strategy.getRandomState(),"source final dual RNG exact");
            for(int n:new int[]{365,517,109,14,558}){
                int id=stable(after,n);byte[] raw=expected.get(n);var b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
                check(PcDuelHealthPolicy.health(after,id)==(raw[0x128]&255),"source HP "+n);
                check(after.government.merit(id)==Short.toUnsignedInt(b.getShort(0xae)),"source merit "+n);
                check(after.officerAbilities.experience(id,1)==Short.toUnsignedInt(b.getShort(0x12c)),"source WAR XP "+n);
            }
            var a=PcGovernorPolicy.data(after).assignments.get(dead);check(a.army==-1&&a.home==-1&&after.officer(dead).owner==-1&&after.officer(dead).unitId==-1,"full dead administration cleanup");
            check(after.government.merit(actor)==w.government.merit(actor)+100,"natural winner100 merit, no capture200");
            check(!game.execute(ContestCommand.finishNativeDuel(token,f.contestId,f.revision)).ok()&&Arrays.equals(finished,game.captureSave()),"old token double settlement pure");
            check(!game.execute(ContestCommand.finishNativeDuel(game.state(),f.contestId,f.revision)).ok()&&Arrays.equals(finished,game.captureSave()),"fresh token cannot repeat dead terminal");cold(game);future(game,dead,heir);
        }
    }
    static boolean human(int seed,int preferred)throws Exception{
        World w=formation(3,seed,517,365);int ruler=stable(w,517),enemy=stable(w,365);
        try(GameSession game=new GameSession(w)){
            var r=game.execute(ContestCommand.startNativeDuel(game.state(),1,2,ruler));check(r.ok(),"normal typed human creator "+r.detail);
            if(game.contest().nativeDuel==null){System.out.println("Original challenge refusal human seed "+seed);cold(game);return false;}
            int inputs=0;
            while(!game.contest().nativeDuel.terminal&&inputs++<300){
                var f=game.contest();var legal=f.nativeDuel.choices.stream().filter(c->c.enabled()&&c.special==-1&&c.replacement==-1).toList();
                var choice=legal.stream().filter(c->c.stance==preferred).findFirst().orElse(legal.get(0));
                r=game.execute(ContestCommand.nativeDuelInput(f.state,f.contestId,f.revision,choice.stance,choice.special,choice.replacement));check(r.ok(),"actual legal human frame input "+r.detail);
            }
            var f=game.contest();check(f.nativeDuel.terminal,"actual human natural terminal");cold(game);f=game.contest();
            World terminal=SaveCodec.decode(game.captureSave());var duel=terminal.contests.session.nativeDuel;int dead=duel.naturalDeathOfficer();
            System.out.println("Human source517 seed "+seed+" stance "+preferred+" inputs "+inputs+" winner "+f.winner+" outcomes "+PcDuelKernel.readManager(duel.state.manager,0x64)+" / "+PcDuelKernel.readManager(duel.state.manager,0x70));
            if(dead!=ruler){
                r=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(r.ok(),"other actual outcome completes or prepares "+r.detail);
                f=game.contest();if(f.nativeDuel!=null){
                    for(var row:f.nativeDuel.disposition){
                        if(row.choice<4)continue;
                        var now=game.contest();int action=-1;for(int candidate:new int[]{2,1,3})if(row.enabled(candidate)){action=candidate;break;}
                        check(action>=0,"other natural captured ruler has a verified enabled disposition "+row.actionErrors);
                        r=game.execute(ContestCommand.nativeDuelDisposition(now.state,now.contestId,now.revision,row.officerId,action));check(r.ok(),"real enabled non-death human disposition "+r.detail);
                    }
                    f=game.contest();if(f.nativeDuel.inheritance!=null){
                        var heir=f.nativeDuel.inheritance.candidates.stream().filter(h->h.enabled()).findFirst().orElseThrow();
                        r=game.execute(ContestCommand.nativeDuelHeir(f.state,f.contestId,f.revision,heir.officerId));check(r.ok(),"other original captured EXECUTE human choice "+r.detail);
                    }
                    f=game.contest();check(game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision)).ok(),"complete non-death terminal");
                }
                cold(game);return false;
            }
            check(f.settlementAvailable&&f.nativeDuel.inheritance==null,"first confirmation offers natural human choice");
            r=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(r.ok(),"actual natural human pending "+r.detail);
            f=game.contest();byte[] pending=game.captureSave();Files.write(Path.of("out/session-b/natural-ruler-human-pending-v1.sg11"),pending);
            check(f.nativeDuel.inheritance!=null&&f.nativeDuel.inheritance.rulerId==ruler&&f.nativeDuel.inheritance.selectedHeir==-1&&!f.settlementAvailable&&f.nativeDuel.disposition.isEmpty(),"natural format7 pending, no captive disposition");
            World p=SaveCodec.decode(pending);check(p.contests.session.nativeDuel.naturalHeir&&p.life.present(ruler)&&p.unit(1)!=null&&p.unit(1).gold==997,"no premature death/cargo/unit mutation");
            int rng=PcNativeDebatePolicy.seed(p);long other=p.strategy.getRandomState();
            var nativeCandidates=f.nativeDuel.inheritance.candidates.stream().map(h->h.nativeId).sorted().toList();check(nativeCandidates.equals(List.of(14,109,313,558)),"original current candidates match full source human callback");
            corruptPending(pending,d->d.naturalHeir=false,"format6 cannot disguise natural pending as captured EXECUTE");
            corruptPending(pending,d->d.heirRuler=enemy,"pending natural ruler cannot refer to winner");
            corruptPending(pending,d->d.chosenHeir=enemy,"saved foreign heir rejects");
            corruptPending(pending,d->PcDuelKernel.writeManager(d.state.manager,0x64,1),"format7 cannot relabel natural death as capture");
            check(Arrays.equals(pending,game.captureSave()),"malformed save probes cannot touch live pending");
            for(int i=0;i<3;i++){game.contest();check(Arrays.equals(pending,game.captureSave()),"pending DTO/RNG pure");}
            var pendingToken=game.state();var pendingRevision=f.revision;int pendingId=f.contestId;int validHeir=stable(p,14);cold(game);f=game.contest();
            r=game.execute(ContestCommand.nativeDuelHeir(pendingToken,pendingId,pendingRevision,validHeir));check(!r.ok()&&Arrays.equals(pending,game.captureSave()),"cold restore fences previous valid-heir StateToken");
            r=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(!r.ok()&&Arrays.equals(pending,game.captureSave()),"no-heir finish rejects byte pure");
            r=game.execute(ContestCommand.nativeDuelHeir(f.state,f.contestId,f.revision,enemy));check(!r.ok()&&Arrays.equals(pending,game.captureSave()),"foreign heir rejects byte pure");
            var heir=f.nativeDuel.inheritance.candidates.stream().filter(h->h.nativeId==14&&h.enabled()).findFirst().orElseThrow();
            var command=ContestCommand.nativeDuelHeir(f.state,f.contestId,f.revision,heir.officerId);r=game.execute(command);check(r.ok(),"valid original choice14 "+r.detail);
            byte[] chosen=game.captureSave();Files.write(Path.of("out/session-b/natural-ruler-human-chosen-v1.sg11"),chosen);
            check(!game.execute(command).ok()&&Arrays.equals(chosen,game.captureSave()),"double/stale choice rejects byte pure");cold(game);p=SaveCodec.decode(game.captureSave());
            check(p.life.present(ruler)&&p.unit(1)!=null&&PcNativeDebatePolicy.seed(p)==rng&&p.strategy.getRandomState()==other,"selection preserves life/units/both RNG");
            f=game.contest();check(f.settlementAvailable,"chosen natural human terminal enabled");r=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(r.ok(),"actual natural human one production terminal "+r.detail);
            byte[] finished=game.captureSave();Files.write(Path.of("out/session-b/natural-ruler-human-finished-v1.sg11"),finished);p=SaveCodec.decode(finished);
            check(!p.contests.busy()&&p.life.state(ruler)==Lifecycle.State.DEAD&&p.loyalty.ruler(3).id==heir.officerId&&p.unit(1)==null,"actual chosen14 crown/death/unit deletion");
            check(p.government.merit(ruler)==0&&p.officerAbilities.experience(ruler,1)==0&&p.government.merit(enemy)==w.government.merit(enemy)+100,"natural cleanup/winner100 merit");
            check(PcNativeDebatePolicy.seed(p)==rng&&p.strategy.getRandomState()==other,"removed lone unit no final troop RNG");
            check(!game.execute(ContestCommand.finishNativeDuel(game.state(),f.contestId,f.revision)).ok()&&Arrays.equals(finished,game.captureSave()),"fresh token cannot double terminal");cold(game);future(game,ruler,heir.officerId);
            System.out.println("PASS actual human natural ruler seed "+seed+" inputs "+inputs);return true;
        }
    }
    /** Fast bounded fresh-case discovery only. Each seed creates a separate
     * World and follows production creator/inputs; no branch rewinds or result
     * edits. The selected case must then run through typed transactions. */
    static void probe()throws Exception{
        for(int seed=0;seed<64;seed++){
            World w=formation(3,seed,517,365);int ruler=stable(w,517);
            var started=w.contests.nativeDuelChallenge(1,2,ruler);check(started.ok,"production discovery creator "+started.message);
            if(w.contests.session==null){System.out.println("Discovery fresh seed "+seed+" refused");continue;}
            int inputs=0;var session=w.contests.session;
            while(!session.nativeDuel.terminal()&&inputs++<300){
                var legal=session.nativeDuel.facts().choices.stream().filter(c->c.error.isEmpty()&&c.special==-1&&c.replacement==-1).toList();
                var choice=legal.stream().filter(c->c.stance==2).findFirst().orElse(legal.get(0));
                var r=w.contests.nativeDuelInput(session.id,session.revision,choice.stance,choice.special,choice.replacement);check(r.ok,"production discovery input "+r.message);
            }
            check(session.nativeDuel.terminal(),"bounded discovery terminal");var d=session.nativeDuel;
            System.out.println("Discovery fresh seed "+seed+" stance2 inputs "+inputs+" winner "+d.state.model.get(0x588)+" outcomes "+PcDuelKernel.readManager(d.state.manager,0x64)+" / "+PcDuelKernel.readManager(d.state.manager,0x70));
            if(d.naturalDeathOfficer()==ruler){byte[]s=SaveCodec.encode(w);check(Arrays.equals(s,SaveCodec.encode(SaveCodec.decode(s))),"actual discovery terminal full Save cold exact");Files.write(Path.of("out/session-b/natural-ruler-probe-terminal-v1.sg11"),s);System.out.println("PASS discovered fresh human natural death seed "+seed+"; typed replay required");return;}
        }
        throw new AssertionError("No natural ruler death in bounded64 independently fresh cases; retain all actual outcomes");
    }
    public static void main(String[]args)throws Exception{
        if(args[0].equals("--probe")){probe();return;}
        ai(Path.of(args[0]),Path.of(args[1]));
        int[] seeds=args.length>2?Arrays.stream(args[2].split(",")).mapToInt(Integer::parseInt).toArray():new int[]{1};
        int preferred=args.length>3?Integer.parseInt(args[3]):0;
        boolean done=false;for(int seed:seeds){if(human(seed,preferred)){done=true;break;}}
        check(done,"actual typed human naturally generated death reached in explicitly bounded cases");
        System.out.println("PASS natural ruler AI/human campaign/save/3 whole turns "+checks+" checks; declared formations; normal deployment/menu/APK pending");
    }
}
