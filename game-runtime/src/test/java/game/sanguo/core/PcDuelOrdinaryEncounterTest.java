package game.sanguo.core;
import java.util.*;
import game.sanguo.api.*;
import game.sanguo.runtime.GameSession;
/** Ordinary host campaign encounter: source factory, actual deployment,
 * admitted movement and full AI turns. No injected unit/position/resources.
 * Android menus and original initial-event parity are separate evidence. */
public final class PcDuelOrdinaryEncounterTest {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[]args)throws Exception {
        boolean crew=Arrays.asList(args).contains("--crew");boolean swapped=false;int sourceIndex=args.length==0?14:Integer.parseInt(args[0]);int seed=1;for(String arg:args)if(arg.startsWith("--seed="))seed=Integer.parseInt(arg.substring(7));String source=PcScenarioCatalog.all().get(sourceIndex).identity.scenarioId;
        World preview=PcScenarioCatalog.load(source,-1,seed,new PcDuelOptions(0,0,0));
        World.City selected=null;int shortest=Integer.MAX_VALUE;
        for(var city:preview.cities)if(city.owner>=0)for(var other:preview.cities)if(other.owner>=0&&preview.campaign.hostile(city.owner,other.owner)&&city.hex.distance(other.hex)<shortest){shortest=city.hex.distance(other.hex);selected=city;}
        check(selected!=null,"source must have hostile cities");int player=selected.owner,cityId=selected.id;
        World opening=PcScenarioCatalog.load(source,player,seed,new PcDuelOptions(0,0,0));var city=opening.city(cityId);
        var candidates=new ArrayList<>(opening.idle(city));candidates.sort(Comparator.comparingInt((World.Officer o)->o.war).reversed());
        World.Officer commander=null;for(var person:candidates)if(person.role!=Strategy.Role.RULER&&opening.army.previewDeployment(cityId,person.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,5000,20000,1000).failure==null){commander=person;break;}
        check(commander!=null,"real original stocks/army/idle/exit admit ordinary deployment");final int leader=commander.id;int[]deputies=crew?candidates.stream().filter(o->o.id!=leader&&o.role!=Strategy.Role.RULER).limit(2).mapToInt(o->o.id).toArray():new int[0];if(crew)check(deputies.length==2,"three real same-city idle native people");
        System.out.println("SOURCE "+source+" player "+player+" city "+city.name+" commander "+commander.name+" closest hostile city distance "+shortest);
        try(GameSession game=new GameSession(opening)){
            var deploy=game.execute(new DeploymentCommand(game.state(),cityId,commander.id,deputies,"SWORD","BOAT",5000,20000,1000));check(deploy.ok(),"typed real deployment "+deploy.detail);int unitId=opening.nextUnitId;
            boolean encountered=false;
            for(int count=0;count<24;count++){
                var view=game.legacyView();World w=view.draft;var unit=w.unit(unitId);check(unit!=null,"ordinary unit survives approach at turn "+w.turn);
                var enemies=w.units.stream().filter(u->w.campaign.hostile(player,u.owner)).sorted(Comparator.comparingInt(u->unit.hex.distance(u.hex))).toList();
                boolean declined=false;for(var enemy:enemies)if(w.contests.duelError(unit.id,enemy.id)==null){
                    byte[]before=game.captureSave();var choices=w.contests.nativeDuelCandidates(unit.id,enemy.id);check(!choices.isEmpty()&&Arrays.equals(before,game.captureSave()),"ordinary candidate query pure");
                    var start=game.execute(ContestCommand.startNativeDuel(game.state(),unit.id,enemy.id,choices.get(0).officerId));check(start.ok(),"ordinary encounter creator "+start.detail);System.out.println("PASS ordinary encounter turn "+w.turn+" unit "+unit.id+" target "+enemy.id+" positions "+unit.hex+" / "+enemy.hex+" detail "+start.detail);if(game.contest().nativeDuel==null){declined=true;System.out.println("Ordinary opponent declined; whole campaign continues");break;}
                    int inputs=0;while(!game.contest().nativeDuel.terminal&&inputs++<250){var facts=game.contest();var choice=facts.nativeDuel.choices.stream().filter(p->p.enabled()&&p.special==-1&&p.replacement==-1).findFirst().orElseThrow();if(crew){var replacement=facts.nativeDuel.choices.stream().filter(p->p.enabled()&&p.replacement>=0).findFirst();if(!swapped&&replacement.isPresent()){choice=replacement.get();swapped=true;System.out.println("ACTUAL legal human swap slot "+choice.replacement);}else{var defense=facts.nativeDuel.choices.stream().filter(p->p.enabled()&&p.stance==1&&p.special==-1&&p.replacement==-1).findFirst();if(defense.isPresent())choice=defense.get();}}var input=game.execute(ContestCommand.nativeDuelInput(facts.state,facts.contestId,facts.revision,choice.stance,choice.special,choice.replacement));check(input.ok(),"ordinary generated human/AI input "+input.detail);byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"ordinary full battle input cold exact");}
                    check(game.contest().nativeDuel.terminal,"ordinary generated battle reaches terminal");var facts=game.contest();System.out.println("NATURAL terminal winner "+facts.winner+" inputs "+inputs);var finish=game.execute(ContestCommand.finishNativeDuel(facts.state,facts.contestId,facts.revision));check(finish.ok(),"ordinary terminal callbacks "+finish.detail);
                    if(game.contest().nativeDuel!=null){for(var row:game.contest().nativeDuel.disposition){var pending=game.contest();var choice=game.execute(ContestCommand.nativeDuelDisposition(pending.state,pending.contestId,pending.revision,row.officerId,1));check(choice.ok(),"ordinary human detention "+choice.detail);}facts=game.contest();finish=game.execute(ContestCommand.finishNativeDuel(facts.state,facts.contestId,facts.revision));check(finish.ok(),"ordinary confirmed terminal "+finish.detail);}
                    check(!game.contest().nativeRules,"ordinary production contest ends");for(int future=0;future<3;future++)futureTurn(game);encountered=true;System.out.println("PASS actual ordinary campaign battle/full terminal/3 whole turns/cold");break;
                }
                if(encountered)break;
                Hex target=enemies.isEmpty()?w.cities.stream().filter(c->c.owner>=0&&w.campaign.hostile(player,c.owner)).min(Comparator.comparingInt(c->unit.hex.distance(c.hex))).orElseThrow().hex:enemies.get(0).hex;
                var reachable=w.orders.reachable(unit);Hex destination=reachable.keySet().stream().filter(h->!h.equals(unit.hex)&&w.cityAt(h)==null).min(Comparator.comparingInt((Hex h)->h.distance(target)).thenComparingInt(h->reachable.get(h))).orElse(null);
                if(!declined&&destination!=null&&destination.distance(target)<unit.hex.distance(target)){var move=game.legacy(w,()->w.move(unitId,destination));check(move.ok,"ordinary admitted movement "+move.message);System.out.println("MOVE turn "+w.turn+" "+unit.hex+" enemies "+enemies.size());}
                futureTurn(game);
            }
            if(crew)check(swapped,"naturally arrived ordinary supporter allows actual human replacement");check(encountered,"ordinary campaign did not reach native duel admission within24 whole turns; preserve actual flow for next investigation");
        }
    }
    static void futureTurn(GameSession game)throws Exception{var ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());var turn=computed.nextTurn();check(turn.ok,"complete AI campaign turn "+turn.message);check(game.commitTurn(ticket,computed),"full turn commit");byte[]saved=game.captureSave();game.replace(SaveCodec.decode(saved));check(Arrays.equals(saved,game.captureSave()),"whole ordinary World/allRNG cold exact");}
}
