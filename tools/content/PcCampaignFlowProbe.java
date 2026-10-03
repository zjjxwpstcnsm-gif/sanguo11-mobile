import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Current built-in campaigns through production sessions; no UI or PC-equivalence claim. */
public final class PcCampaignFlowProbe {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectories(output);
        for(var row:ScenarioCatalog.summaries()){
            World initial=ScenarioCatalog.load(row.id,0);GameSession session=new GameSession(initial);
            boolean applied=false;int rejected=0;
            for(World.Officer officer:initial.officers){
                if(officer.owner!=initial.player||officer.cityId<0)continue;
                byte[] before=session.captureSave();StateToken token=session.state();
                GameCommand command=new GameCommand(GameCommand.Operation.PATROL,token,officer.cityId,officer.id);
                CommandResult result=session.execute(command);
                if(!result.ok()){
                    rejected++;check(result.error==CommandResult.Error.RULE_REJECTED,"explicit ordinary rule rejection: "+result.error);
                    check(Arrays.equals(before,session.captureSave()),"rejected order preserves full state/RNG");continue;
                }
                World reference=SaveCodec.decode(before);check(reference.patrol(officer.cityId,officer.id).ok,"normal reference patrol");
                byte[] committed=session.captureSave();check(Arrays.equals(committed,SaveCodec.encode(reference)),"session commits exact normal rule result");
                check(session.execute(command).error==CommandResult.Error.STALE_REVISION,"repeat click with old token rejected");
                check(Arrays.equals(committed,session.captureSave()),"repeat click cannot spend twice");
                applied=true;break;
            }
            check(applied,"new scenario has a real usable patrol command: "+row.id);
            World deploymentView=SaveCodec.decode(session.captureSave());boolean deployed=false;
            for(World.City city:deploymentView.cities){
                for(World.Officer officer:deploymentView.idle(city)){
                    byte[] before=session.captureSave();StateToken token=session.state();
                    DeploymentCommand command=new DeploymentCommand(token,city.id,officer.id,new int[0],"SWORD","BOAT",1000,2000,0);
                    DeploymentPreview preview=session.preview(command);
                    check(Arrays.equals(before,session.captureSave())&&token.equals(session.state()),"new-campaign deployment preview is read only");
                    if(!preview.allowed())continue;
                    World reference=SaveCodec.decode(before);check(reference.army.deploy(city.id,officer.id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,1000,2000,0).ok,"ordinary deployment");
                    check(session.execute(command).ok(),"new-campaign typed deployment");
                    byte[] committed=session.captureSave();check(Arrays.equals(committed,SaveCodec.encode(reference)),"real deployment state and RNG match");
                    check(session.execute(command).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(committed,session.captureSave()),"double deploy cannot spend twice");
                    LegacyView moveView=session.legacyView();World.Unit unit=moveView.draft.unit(moveView.draft.officer(officer.id).unitId);
                    check(unit.hex.q==preview.unit.exitQ&&unit.hex.r==preview.unit.exitR&&moveView.draft.orders.remaining(unit)==preview.unit.movementRemaining,"paid departure predicted on actual scenario");
                    for(Hex target:moveView.draft.reachable(unit).keySet()){
                        if(target.equals(unit.hex)||moveView.draft.cityAt(target)!=null)continue;
                        World moveReference=SaveCodec.decode(committed);check(moveReference.move(unit.id,target).ok,"ordinary march");
                        check(session.legacy(moveView.draft,()->moveView.draft.move(unit.id,target)).ok,"session march");
                        check(Arrays.equals(session.captureSave(),SaveCodec.encode(moveReference)),"march complete save and RNG match");break;
                    }
                    deployed=true;break;
                }
                if(deployed)break;
            }
            check(deployed,"new scenario has a real usable deployment command: "+row.id);
            for(int turn=0;turn<6;turn++){
                byte[] before=session.captureSave();World computed=SaveCodec.decode(before),reference=SaveCodec.decode(before);
                TurnTicket ticket=session.beginTurn();check(Arrays.equals(before,session.captureSave()),"in-flight turn does not save partial state");
                check(computed.nextTurn().ok&&reference.nextTurn().ok,"normal multi-turn calculations succeed");
                check(session.commitTurn(ticket,computed),"complete turn accepted exactly once");
                check(!session.commitTurn(ticket,computed),"duplicate turn completion ignored");
                byte[] after=session.captureSave();check(Arrays.equals(after,SaveCodec.encode(reference)),"next-turn RNG/results deterministic");
                check(SaveCodec.decode(after).turn==initial.turn+turn+1,"turn advances once");
                session.close();session=new GameSession(SaveCodec.decode(after));
                check(Arrays.equals(after,session.captureSave()),"save-load-new-session preserves entire authority");
            }
            byte[] result=session.captureSave();Files.write(output.resolve(row.id+"-turn6.sg11"),result);
            System.out.println("PASS scenario="+row.id+" rejected="+rejected+" turn=6 sha256="+hash(result));session.close();
        }
        System.out.println("PASS "+checks+" current campaign/session/patrol/deployment/march/6-turn/save checks; PC rule parity not certified");
    }
}
