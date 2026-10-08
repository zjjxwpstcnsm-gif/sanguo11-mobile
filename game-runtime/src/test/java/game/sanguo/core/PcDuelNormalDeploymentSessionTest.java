package game.sanguo.core;

import java.nio.file.*;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.GameSession;

/** Real source stocks/typed deployment/ordinary movement/whole AI campaign.
 * No unit, roster, resource, position, action, HP or terminal is injected.
 * Host acceptance remains separate from A menus and installed APK. */
public final class PcDuelNormalDeploymentSessionTest {
    static int checks;
    static String version="v1";
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void save(GameSession game,String name)throws Exception{
        byte[] raw=game.captureSave();check(Arrays.equals(raw,SaveCodec.encode(SaveCodec.decode(raw))),"complete World/both RNG cold exact "+name);
        Files.write(Path.of("out/session-b/normal-deployed-duel-"+name+"-"+version+".sg11"),raw);
    }
    static void turn(GameSession game)throws Exception{
        var ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
        System.out.println("Begin whole campaign turn "+computed.turn+" armies "+computed.units.size());
        var next=computed.nextTurn();check(next.ok,"whole AI/personnel/logistics turn "+next.message);
        check(game.commitTurn(ticket,computed),"ordinary serial complete turn");
        save(game,"current");byte[] raw=game.captureSave();game.replace(SaveCodec.decode(raw));check(Arrays.equals(raw,game.captureSave()),"actual cold continuation");
        System.out.println("Finished whole turn "+computed.turn+" armies "+computed.units.size());
    }
    static void finish(GameSession game)throws Exception{
        int inputs=0;while(!game.contest().nativeDuel.terminal&&inputs++<300){
            var f=game.contest();var c=f.nativeDuel.choices.stream().filter(p->p.enabled()&&p.special==-1&&p.replacement==-1).findFirst().orElseThrow();
            var r=game.execute(ContestCommand.nativeDuelInput(f.state,f.contestId,f.revision,c.stance,c.special,c.replacement));check(r.ok(),"ordinary deployed human input "+r.detail);
            if(inputs==1||inputs%10==0){save(game,"contest");byte[] raw=game.captureSave();game.replace(SaveCodec.decode(raw));check(Arrays.equals(raw,game.captureSave()),"actual in-battle cold continuation");}
        }
        var f=game.contest();check(f.nativeDuel.terminal,"ordinary deployed actual terminal");save(game,"contest");
        var r=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(r.ok(),"ordinary actual terminal prepare "+r.detail);
        f=game.contest();if(f.nativeDuel!=null){
            for(var row:f.nativeDuel.disposition){
                if(row.choice<4)continue;int action=-1;for(int a:new int[]{1,2,3})if(row.enabled(a)){action=a;break;}
                check(action>=0,"ordinary captured participant has verified disposition "+row.actionErrors);var now=game.contest();
                r=game.execute(ContestCommand.nativeDuelDisposition(now.state,now.contestId,now.revision,row.officerId,action));check(r.ok(),"ordinary actual legal disposition "+r.detail);
            }
            f=game.contest();if(f.nativeDuel.inheritance!=null){var heir=f.nativeDuel.inheritance.candidates.stream().filter(h->h.enabled()).findFirst().orElseThrow();r=game.execute(ContestCommand.nativeDuelHeir(f.state,f.contestId,f.revision,heir.officerId));check(r.ok(),"ordinary deployed actual heir "+r.detail);}
            f=game.contest();r=game.execute(ContestCommand.finishNativeDuel(f.state,f.contestId,f.revision));check(r.ok(),"ordinary deployed once final settlement "+r.detail);
        }
        check(game.contest().nativeDuel==null,"ordinary battle returns to campaign");save(game,"finished");
        byte[] finished=game.captureSave();check(!game.execute(ContestCommand.finishNativeDuel(game.state(),f.contestId,f.revision)).ok()&&Arrays.equals(finished,game.captureSave()),"ordinary terminal cannot repeat");
        for(int i=0;i<3;i++)turn(game);
        System.out.println("PASS ordinary deployed native duel "+inputs+" actual inputs, 3 future full turns, "+checks+" checks; normal Android menu/APK still pending");
    }
    public static void main(String[]args)throws Exception{
        if(args.length>0&&args[0].equals("--battle")){
            version="v5";byte[] raw=Files.readAllBytes(Path.of(args[1]));World actual=SaveCodec.decode(raw);
            check(actual.turn==25&&actual.contests.session!=null&&actual.contests.session.nativeDuel!=null,"actual accepted ordinary25 battle, no reconstructed model");
            try(GameSession game=new GameSession(actual)){check(Arrays.equals(raw,game.captureSave()),"same saved full battle/both RNG before continuation");finish(game);}return;
        }
        boolean redeploy=args.length>0&&args[0].equals("--redeploy"),loaded=args.length>0,resume=loaded&&!redeploy;
        byte[] original=loaded?Files.readAllBytes(Path.of(args[redeploy?1:0])):null;if(loaded)version=redeploy?"v4":"v2";
        World initial=loaded?SaveCodec.decode(original):PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,28,0,new PcDuelOptions(0,2,0));
        int leader=PcDuelNaturalDeathSessionTest.stable(initial,redeploy?503:58);var officer=initial.officer(leader);var city=initial.city(officer.cityId);
        String weapon=redeploy?"SPEAR":"SWORD";int troops=redeploy?3000:13000,food=redeploy?30000:52000;
        check(initial.player==28&&initial.active==28&&PcDuelCampaignPolicy.read(initial).version==3,"same explicit source/player/current policy");
        int[] deputies;
        if(!resume){
            check(city!=null&&city.owner==28&&initial.idle(city).contains(officer),"actual native58 source identity/city/idle, no activation");
            if(!redeploy)check(initial.units.isEmpty(),"actual source opening initial units; AI armies must deploy naturally");else check(initial.turn==25&&initial.unit(1)==null&&city.id==20004,"same lost-unit25 campaign; new ordinary city/crew preference, no reset");
            deputies=redeploy?new int[0]:initial.idle(city).stream().filter(o->o.id!=leader&&o.role!=Strategy.Role.RULER).sorted(Comparator.comparingInt((World.Officer o)->o.war).reversed().thenComparingInt(o->o.id)).limit(2).mapToInt(o->o.id).toArray();
            System.out.println("Source "+PcScenarioIdentity.saved(initial).scenarioId+" player28 city "+city.id+" "+city.name+" leader "+leader+" "+officer.name+" deputies "+Arrays.toString(deputies)+" stocks "+city.troops+"/"+city.food+"/"+city.gold);
        }else{check(initial.turn==12&&officer.unitId>=0&&initial.unit(officer.unitId)!=null,"resume actual surviving turn12 unit");deputies=initial.unit(officer.unitId).deputies;System.out.println("Resume exact actual turn12 World/both RNG "+PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(original)));}
        try(GameSession game=new GameSession(initial)){
            int unitId=resume?officer.unitId:initial.nextUnitId;
            if(loaded)check(Arrays.equals(original,game.captureSave()),"same complete original campaign bytes/RNG before any new command");
            if(!resume){
            var deployment=new DeploymentCommand(game.state(),city.id,leader,deputies,weapon,"BOAT",troops,food,1000);byte[] before=game.captureSave();var preview=game.preview(deployment);
            check(preview.allowed(),"normal source stock/deployment admission "+preview.reasonCode+" "+preview.detail);check(Arrays.equals(before,game.captureSave()),"ordinary deployment preview pure");
            var result=game.execute(deployment);check(result.ok(),"normal typed deployment "+result.detail);
            World deployed=SaveCodec.decode(game.captureSave());var own=deployed.unit(unitId);check(own!=null&&own.officerId==leader&&Arrays.equals(own.deputies,deputies)&&own.gold==1000&&own.troops==troops&&own.food==food&&own.weapon==World.Weapon.valueOf(weapon),"real deployed ordinary formation/resources");
            check(deployed.city(city.id).troops==city.troops-troops&&deployed.city(city.id).food==city.food-food&&deployed.city(city.id).gold==city.gold-1000,"actual stock debit exactly once");save(game,"after-deploy");
            }else check(Arrays.equals(original,game.captureSave()),"resumed full original campaign bytes/RNG not reinitialized");
            for(int attempt=0;attempt<(resume?18:12);attempt++){
                World w=SaveCodec.decode(game.captureSave());var own=w.unit(unitId);check(own!=null,"normal deployed unit survives before encounter; no replacement injected");
                final var current=own;final var observed=w;var enemies=w.units.stream().filter(u->observed.campaign.hostile(current.owner,u.owner)&&u.status==War.Status.NORMAL&&!observed.army.water(u.hex)&&!Army.siegeWeapon(u.weapon)).sorted(Comparator.comparingInt((World.Unit u)->current.hex.distance(u.hex)).thenComparingInt(u->u.id)).toList();
                System.out.println("Campaign turn "+w.turn+" player unit "+unitId+" at "+own.hex+" troops "+own.troops+" energy "+own.energy+" food "+own.food+" acted "+own.acted+" actual enemy armies "+enemies.size());
                if(!own.acted&&own.status==War.Status.NORMAL&&!enemies.isEmpty()){
                    var target=enemies.get(0);System.out.println("Actual nearest enemy unit "+target.id+" owner "+target.owner+" leader "+target.officerId+" at "+target.hex+" distance "+own.hex.distance(target.hex));
                    if(own.hex.distance(target.hex)>1){
                        var view=game.legacyView();var u=view.draft.unit(unitId);final var t=target;
                        Hex destination=view.draft.orders.reachable(u).keySet().stream().filter(h->!h.equals(u.hex)).min(Comparator.comparingInt((Hex h)->h.distance(t.hex)).thenComparingInt(h->h.q).thenComparingInt(h->h.r)).orElse(null);
                        if(destination!=null&&destination.distance(target.hex)<own.hex.distance(target.hex)){
                            byte[] pre=game.captureSave();var p=view.draft.orders.previewMove(unitId,destination);check(p.valid()&&Arrays.equals(pre,game.captureSave()),"ordinary route preview pure");
                            var move=game.legacy(view.draft,()->view.draft.move(unitId,destination));check(move.ok,"actual normal map move "+move.message);save(game,"current");w=SaveCodec.decode(game.captureSave());own=w.unit(unitId);
                        }
                    }
                    if(own.hex.distance(target.hex)==1){
                        var view=game.legacyView();byte[] pre=game.captureSave();var candidates=view.draft.contests.nativeDuelCandidates(unitId,target.id);check(!candidates.isEmpty()&&Arrays.equals(pre,game.captureSave()),"ordinary deployed candidate page pure");
                        var nominee=candidates.stream().max(Comparator.comparingInt(c->c.chance)).orElseThrow();System.out.println("Actual challenge nominee "+nominee.officerId+" chance "+nominee.chance);
                        var result=game.execute(ContestCommand.startNativeDuel(game.state(),unitId,target.id,nominee.officerId));check(result.ok(),"ordinary deployed actual challenge "+result.detail);save(game,"current");
                        if(game.contest().nativeDuel!=null){finish(game);return;}
                        System.out.println("Actual challenge refused; action/resources persisted, continue same campaign");
                    }
                }
                turn(game);
            }
            throw new AssertionError("No accepted adjacent enemy encounter through actual turn"+SaveCodec.decode(game.captureSave()).turn+"; preserve campaign, no units/positions/RNG injected");
        }
    }
}
