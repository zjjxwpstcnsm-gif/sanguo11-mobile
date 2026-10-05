package game.sanguo.runtime;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class DeploymentSessionTest {
    private static int checks;
    private static void check(boolean v,String detail){checks++;if(!v)throw new AssertionError(detail);}
    private static DeploymentCommand command(StateToken token,int city,int leader,int[] deputies,String weapon,String ship,int troops,int food,int gold){return new DeploymentCommand(token,city,leader,deputies,weapon,ship,troops,food,gold);}
    public static void main(String[] args)throws Exception{
        World initial=ScenarioCatalog.load("coalition-190",0,20261003L);World.City city=initial.home();
        city.troops=30000;city.food=200000;city.gold=30000;Arrays.fill(city.equipment,12000);city.equipment[World.Weapon.SWORD.ordinal()]=0;for(int i=5;i<9;i++)city.equipment[i]=2;Arrays.fill(city.ships,2);
        List<World.Officer> idle=initial.idle(city);int leader=idle.get(0).id,deputy=idle.get(1).id;
        for(World.Weapon weapon:World.Weapon.values())for(Army.Ship ship:Army.Ship.values()){
            try(GameSession game=new GameSession(initial)){
                byte[] before=game.captureSave();StateToken token=game.state();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
                int[] deputies={deputy};DeploymentCommand c=command(token,city.id,leader,deputies,weapon.name(),ship.name(),3000,9000,123);
                deputies[0]=-1;c.deputies()[0]=-1;check(c.deputies()[0]==deputy,"input/output array ownership");
                DeploymentPreview p=game.preview(c);check(p.allowed(),"preview "+weapon+" "+ship+" "+p.detail);
                check(p.unit.energy==city.morale&&!p.unit.equipmentLabel.isEmpty(),"API unit details populated");
                boolean immutable=false;try{p.unit.skills.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"API detail list ownership");
                check(game.preview(c).unit.exitQ==p.unit.exitQ,"repeat deterministic query");
                check(Arrays.equals(before,game.captureSave())&&token.equals(game.state())&&events.isEmpty(),"query does not commit, publish, save or consume RNG");
                World direct=SaveCodec.decode(before);check(direct.army.deploy(city.id,leader,new int[]{deputy},weapon,ship,3000,9000,123).ok,"ordinary core command");
                CommandResult result=game.execute(c);check(result.ok(),result.detail);
                check(result.state.revision==token.revision+1&&events.size()==1&&result.event.kind==GameEvent.Kind.DEPLOYED,"one fact after one commit");
                check(Arrays.equals(SaveCodec.encode(direct),game.captureSave()),"typed and ordinary command full-save/RNG parity");
                World after=SaveCodec.decode(game.captureSave());World.Unit u=after.unit(after.officer(leader).unitId);
                check(u.hex.q==p.unit.exitQ&&u.hex.r==p.unit.exitR&&after.orders.remaining(u)==p.unit.movementRemaining,"preview and real departure");
                check(after.home().troops==p.remaining.troops&&after.home().food==p.remaining.food&&after.home().gold==p.remaining.gold,"real resource costs");
                DeploymentPreview fresh=game.preview(command(game.state(),city.id,leader,new int[0],weapon.name(),ship.name(),3000,9000,0));
                check(fresh.reasonCode.equals("LEADER_UNAVAILABLE")&&fresh.stock.troops==p.remaining.troops,"commit invalidates cached query world");
                byte[] committed=game.captureSave();check(game.preview(c).error==CommandResult.Error.STALE_REVISION&&game.execute(c).error==CommandResult.Error.STALE_REVISION,"stale and duplicate rejected");
                check(Arrays.equals(committed,game.captureSave())&&events.size()==1,"no duplicate effects");
            }
        }
        try(GameSession game=new GameSession(initial)){
            byte[] before=game.captureSave();StateToken token=game.state();
            DeploymentCommand bad=command(token,city.id,leader,new int[]{leader},"SPEAR","BOAT",3000,9000,0);
            DeploymentPreview p=game.preview(bad);CommandResult r=game.execute(bad);
            check(p.reasonCode.equals("DEPUTY_UNAVAILABLE")&&r.reasonCode.equals(p.reasonCode)&&r.field.equals("deputies")&&r.detail.equals(p.detail),"structured exact rejection");
            check(Arrays.equals(before,game.captureSave())&&game.state().equals(token),"failed command atomicity");
            DeploymentCommand invalid=command(token,city.id,leader,new int[0],"missing","BOAT",3000,9000,0);
            check(game.preview(invalid).field.equals("weapon")&&game.execute(invalid).reasonCode.equals("FORMATION_INVALID"),"wire enum invalid rejected by rules");
            TurnTicket turn=game.beginTurn();check(game.preview(bad).error==CommandResult.Error.HOST_BUSY&&game.execute(bad).error==CommandResult.Error.HOST_BUSY,"turn in flight serialized");game.cancelTurn(turn);
            AtomicReference<Throwable> wrong=new AtomicReference<>();Thread worker=new Thread(()->{try{game.preview(bad);}catch(Throwable e){wrong.set(e);}});worker.start();worker.join();check(wrong.get() instanceof IllegalStateException,"query requires logic thread");
            game.replace(initial);check(game.preview(bad).error==CommandResult.Error.STALE_SESSION&&game.execute(bad).error==CommandResult.Error.STALE_SESSION,"load invalidates preview tokens");
            game.close();check(game.preview(bad).error==CommandResult.Error.CLOSED&&game.execute(bad).error==CommandResult.Error.CLOSED,"closed host rejects safely");
        }
        System.out.println("PASS DeploymentSessionTest checks="+checks);
    }
}
