package game.sanguo.core;

import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class ConstructionSessionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static World fixture()throws Exception{
        World w=new World(24,20);w.cities.add(new World.City(10,"甲",new Hex(4,4),0));w.cities.add(new World.City(20,"乙",new Hex(18,14),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,80,80,80,90,80));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));w.strategy.initializeOffices();
        w.treasures.place(Treasures.definition("item-042"),Treasures.Place.TREASURY,0);return w;
    }
    private static ConstructionCommand command(StateToken token,String kind,Hex site){return new ConstructionCommand(token,10,0,kind,site.q,site.r);}
    public static void main(String[] args)throws Exception{
        for(Domestic.Kind kind:Domestic.Kind.values()){
            World initial=fixture();Hex site=initial.domestic.buildSites(10).get(0);
            if(kind==Domestic.Kind.SHIPYARD)for(Hex near:site.neighbors())if(initial.inside(near)&&initial.cityAt(near)==null){initial.terrain[near.q][near.r]=World.Terrain.WATER;break;}
            try(GameSession game=new GameSession(initial)){
                StateToken token=game.state();byte[] before=game.captureSave();List<GameEvent> events=new ArrayList<>();game.subscribe(events::add);
                ConstructionCommand command=command(token,kind.name(),site);ConstructionPreview p=game.preview(command);
                check(p.allowed(),p.detail);check(p.completion.level==1&&p.resources.goldCost==kind.cost,"source facts in API");
                check(game.preview(command).completion.turns==p.completion.turns&&Arrays.equals(before,game.captureSave())&&events.isEmpty(),"repeat cached query is pure");
                World direct=SaveCodec.decode(before);check(direct.domestic.build(10,0,kind,site).ok,"ordinary build");
                CommandResult result=game.execute(command);check(result.ok(),result.detail);
                check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"typed equals ordinary entire save and RNG");
                check(result.event.kind==GameEvent.Kind.CONSTRUCTION_STARTED&&events.size()==1&&result.state.revision==token.revision+1,"one start event and one commit");
                check(direct.city(10).gold==p.resources.goldRemaining&&direct.domestic.at(site).remaining==p.completion.turns,"real resources and work duration");
                ConstructionPreview fresh=game.preview(command(game.state(),kind.name(),site));
                check(fresh.reasonCode.equals("LEADER_UNAVAILABLE")&&fresh.resources.goldAvailable==p.resources.goldRemaining,"commit invalidates shared query cache");
                byte[] built=game.captureSave();check(game.preview(command).error==CommandResult.Error.STALE_REVISION&&game.execute(command).error==CommandResult.Error.STALE_REVISION&&Arrays.equals(built,game.captureSave()),"double start cannot consume resources again");
                for(int n=0;n<3;n++){
                    TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
                    check(computed.nextTurn().ok&&direct.nextTurn().ok&&game.commitTurn(ticket,computed),"complete ordinary turn");
                    check(Arrays.equals(game.captureSave(),SaveCodec.encode(direct)),"construction continuation full-save equality");
                }
                check(SaveCodec.decode(game.captureSave()).domestic.at(site).level==1,"construction finishes at previewed base level");
            }
        }
        try(GameSession game=new GameSession(fixture())){
            byte[] before=game.captureSave();StateToken token=game.state();
            for(String kind:new String[]{null,"bad","MARKET"}){
                ConstructionCommand c=command(token,kind,new Hex(-1,-1));ConstructionPreview p=game.preview(c);CommandResult r=game.execute(c);
                check(!p.allowed()&&r.error==p.error&&r.reasonCode.equals(p.reasonCode)&&r.detail.equals(p.detail)&&p.completion==null,"structured rejected preview and command");
                check(Arrays.equals(before,game.captureSave())&&token.equals(game.state()),"invalid inputs atomic");
            }
            ConstructionCommand c=command(token,"MARKET",new Hex(-1,-1));TurnTicket ticket=game.beginTurn();
            check(game.preview(c).error==CommandResult.Error.HOST_BUSY&&game.execute(c).error==CommandResult.Error.HOST_BUSY,"in-flight turn blocks query and build");game.cancelTurn(ticket);
            AtomicReference<Throwable> wrong=new AtomicReference<>();Thread thread=new Thread(()->{try{game.preview(c);}catch(Throwable e){wrong.set(e);}});thread.start();thread.join();check(wrong.get() instanceof IllegalStateException,"logic thread required");
            game.replace(fixture());check(game.preview(c).error==CommandResult.Error.STALE_SESSION&&game.execute(c).error==CommandResult.Error.STALE_SESSION,"replace expires request");
            game.close();check(game.preview(c).error==CommandResult.Error.CLOSED&&game.execute(c).error==CommandResult.Error.CLOSED,"closed query and command rejected");
        }
        System.out.println("PASS ConstructionSessionTest checks="+checks);
    }
}
