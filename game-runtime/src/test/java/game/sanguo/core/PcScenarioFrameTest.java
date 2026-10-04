package game.sanguo.core;

import game.sanguo.api.*;
import game.sanguo.runtime.GameSession;
import game.sanguo.runtime.TurnTicket;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Capacity/save/session fixture, NOT proof of a restored PC opening or APK flow. */
public final class PcScenarioFrameTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static PcScenarioIdentity.Source descriptor()throws Exception{
        PcOfficerSources.Source s=PcOfficerSources.all().get(7);
        return new PcScenarioIdentity.Source(s.id,s.name,PcScenarioIdentity.variant(s.path,s.sha),s.path,s.sha,
            "dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f",s.year,s.month,s.day,
            List.of("fixture-layout-not-PC-opening","opening-events-not-executed","external-resource-priority-unknown"));
    }
    private static World fixture(int factions)throws Exception{
        String[] names=new String[factions];for(int i=0;i<factions;i++)names[i]="容量夹具"+i;
        World w=PcScenarioIdentity.create(200,100,names,descriptor());w.player=36;w.active=36;
        for(int i=0;i<factions;i++){
            World.City c=new World.City(20000+i,"测试城"+i,new Hex(3+i*4,20),i);c.gold=3000;c.food=40000;c.troops=6000;c.order=70;w.cities.add(c);
            World.Officer o=new World.Officer(700000+i,"测试将"+i,i,c.id,65,65,65,65,65);w.officers.add(o);
        }
        w.strategy.initializeOffices();w.abilities.initialize(23);w.officerAbilities.initializeOpening(null,false,false);
        w.merchantMarket.initializeOpening();w.pcProduction.initializeOpening();w.pcTechniquePoints.initializeOpening();
        SaveCodec.validate(w);return w;
    }
    private static int version(byte[] bytes){return ByteBuffer.wrap(bytes).getInt(4);}
    private static byte[] header(byte[] bytes,int version){byte[] changed=bytes.clone();ByteBuffer.wrap(changed).putInt(4,version);return changed;}
    private static void rejected(byte[] bytes,String label)throws Exception{
        boolean rejected=false;try{SaveCodec.decode(bytes);}catch(IOException expected){rejected=true;}check(rejected,label);
    }
    public static void main(String[] args)throws Exception{
        String[] tooMany=new String[33];Arrays.fill(tooMany,"legacy");boolean rejected=false;
        try{new World(200,100,tooMany);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"ordinary constructor still refuses33 factions");
        PcScenarioIdentity.Source source=descriptor();World identityControl=ScenarioCatalog.load("coalition-190",0,23);
        PcOfficerSources.attachOpening(identityControl,source.scenarioId);
        check(source.sourceVariant.equals(PcOfficerInfo.saved(identityControl).values().iterator().next().sourceVariant),"source variant matches independently packed source/person identity");
        World w=fixture(47);byte[] start=SaveCodec.encode(w);check(version(start)==38,"explicit new-source frame is38");
        World copy=SaveCodec.decode(start);check(copy.factions.length==47&&copy.player==36&&PcScenarioIdentity.saved(copy).sourceVariant.equals(source.sourceVariant),"47 factions and immutable source survive decoding");
        check(Arrays.equals(start,SaveCodec.encode(copy)),"complete new frame byte/RNG roundtrip");
        rejected(header(start,37),"old header cannot accept47 faction payload");
        byte[] identity=copy.extensions.get(PcScenarioIdentity.NAMESPACE);copy.extensions.put(PcScenarioIdentity.NAMESPACE,null);
        rejected=false;try{SaveCodec.encode(copy);}catch(IOException expected){rejected=true;}check(rejected,"missing source identity never falls back to legacy");copy.extensions.put(PcScenarioIdentity.NAMESPACE,identity);
        World control=SaveCodec.decode(start);
        try(GameSession game=new GameSession(w)){
            StateToken first=game.state();check(control.patrol(20036,700036).ok,"capacity fixture normal patrol");
            check(game.execute(new GameCommand(GameCommand.Operation.PATROL,first,20036,700036)).ok(),"formal typed command at side36");
            check(!game.execute(new GameCommand(GameCommand.Operation.PATROL,first,20036,700036)).ok(),"stale command retains formal token boundary");
            check(Arrays.equals(SaveCodec.encode(control),game.captureSave()),"typed source command preserves complete control save/RNG");
            List<BattleReports.Entry> reports=control.reports.query(control.turn,36,BattleReports.Scope.RELATED,null,"");
            check(!reports.isEmpty()&&reports.get(reports.size()-1).involves(36),"report relation bit works above31");
            for(int i=0;i<3;i++){
                TurnTicket ticket=game.beginTurn();World next=SaveCodec.decode(ticket.initial());
                check(next.nextTurn().ok&&game.commitTurn(ticket,next)&&control.nextTurn().ok,"47-faction complete turn and formal commit");
                byte[] bytes=game.captureSave();check(Arrays.equals(bytes,SaveCodec.encode(control)),"complete turn save/RNG agrees");
                try(GameSession reopened=new GameSession(SaveCodec.decode(bytes))){check(Arrays.equals(bytes,reopened.captureSave()),"full source frame reload after turn");}
            }
        }
        World legacy=ScenarioCatalog.load("coalition-190",0,23);byte[] untouched=SaveCodec.encode(legacy);
        legacy.extensions.put(PcScenarioIdentity.NAMESPACE,new byte[]{1,2,3});legacy.dataSource=PcScenarioIdentity.DATA_SOURCE;
        byte[] opaque=SaveCodec.encode(legacy);check(version(opaque)==version(untouched),"opaque legacy extension and colliding label do not upgrade saved policy");
        World old=SaveCodec.decode(opaque);check(PcScenarioIdentity.saved(old)==null&&Arrays.equals(opaque,SaveCodec.encode(old)),"opaque old extension remains byte-identical and is never parsed as new source");
        byte[] before=opaque.clone();rejected(header(before,38),"header upgrade without valid source identity is rejected");
        System.out.println("PASS PC frame/session capacity "+checks+" checks; fixture only, source opening/APK unverified");
    }
}
