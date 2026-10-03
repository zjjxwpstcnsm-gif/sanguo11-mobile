package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Source-backed base cost/level changes through real commands and untouched old saves. */
public final class PcConstructionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static World fixture()throws IOException{
        World w=new World(24,20);w.cities.add(new World.City(10,"甲",new Hex(4,4),0));w.cities.add(new World.City(20,"乙",new Hex(18,14),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,80,80,80,90,80));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));w.strategy.initializeOffices();
        w.treasures.place(Treasures.definition("item-042"),Treasures.Place.TREASURY,0);return w;
    }
    private static Hex site(World w,Domestic.Kind kind){
        Hex h=w.domestic.buildSites(10).get(0);
        if(kind==Domestic.Kind.SHIPYARD)for(Hex near:h.neighbors())if(w.inside(near)&&w.cityAt(near)==null&&!w.development.contains(w.city(10),near)){
            w.terrain[near.q][near.r]=World.Terrain.WATER;return h;
        }
        if(kind==Domestic.Kind.SHIPYARD)for(Hex near:h.neighbors())if(w.inside(near)&&w.cityAt(near)==null){w.terrain[near.q][near.r]=World.Terrain.WATER;return h;}
        return h;
    }
    public static void main(String[] args)throws Exception{
        int[] sourceCosts={200,200,300,300,400,400,300,50,300,300,1500};
        for(Domestic.Kind kind:Domestic.Kind.values())for(int limit:new int[]{-1,0,10000}){
            World w=fixture();Hex h=site(w,kind);int cost=sourceCosts[kind.ordinal()];w.city(10).gold=cost+limit;
            check(kind.cost==cost,"source cost "+kind);check(Domestic.buildLevel(kind)==1,"ordinary construction starts at base level");
            byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();int ap=w.actionPoints[0];
            ConstructionPlan preview=w.domestic.previewBuild(10,0,kind,h);
            check(Arrays.equals(before,SaveCodec.encode(w)),"preview leaves all state, RNG and facility IDs unchanged");
            World.Result result=w.domestic.build(10,0,kind,h);
            check(preview.allowed()==result.ok&&(result.ok||preview.failure.detail.equals(result.message)),"preview and ordinary command share exact validation");
            if(limit<0){check(!result.ok&&result.message.equals("金不足"),"below source cost rejected");check(Arrays.equals(before,SaveCodec.encode(w)),"rejection preserves all state and RNG");continue;}
            check(result.ok,result.message);Domestic.Facility f=w.domestic.at(h);
            check(f!=null&&f.level==1&&f.upgradeTo==0&&f.builderId==0,"real base facility and worker");
            check(f.remaining==preview.turns&&f.level==preview.level&&f.hp==preview.initialDurability&&f.maxHp()==preview.maximumDurability,"normal command creates forecast construction");
            check(w.city(10).gold==limit&&w.actionPoints[0]==ap-20,"source gold and20 action points charged");
            check(rng==w.strategy.getRandomState(),"construction consumes no RNG");
            byte[] built=SaveCodec.encode(w);check(!w.domestic.build(10,0,kind,h).ok&&Arrays.equals(built,SaveCodec.encode(w)),"repeat construction cannot debit twice");
            World restored=SaveCodec.decode(built);
            for(int n=0;n<3;n++){
                check(w.nextTurn().ok&&restored.nextTurn().ok,"normal complete turn");
                check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"construction continuation is save/RNG identical");
                restored=SaveCodec.decode(SaveCodec.encode(restored));
            }
            check(w.domestic.facility(f.id).level==1&&w.domestic.facility(f.id).remaining==0,"normal completion never promotes to Lv3");
            check(Domestic.facilityEffect(f).equals(Domestic.buildEffect(kind)),"base facility and new-build effects agree");
        }
        for(int points:new int[]{0,19,20,21}){
            World w=fixture();Hex h=site(w,Domestic.Kind.MARKET);w.actionPoints[0]=points;
            byte[] before=SaveCodec.encode(w);ConstructionPlan preview=w.domestic.previewBuild(10,0,Domestic.Kind.MARKET,h);
            check(preview.actionPointsCost==20&&preview.actionPointsRemaining==points-20,"native AP preview including negative shortfall");
            check(Arrays.equals(before,SaveCodec.encode(w)),"AP preview pure");
            World.Result result=w.domestic.build(10,0,Domestic.Kind.MARKET,h);
            check(result.ok==(points>=20)&&preview.allowed()==result.ok,"native AP boundary in preview and command");
            if(points<20){check(preview.failure.code.equals("ACTION_POINTS")&&result.message.equals("行动力不足20"),"specific AP failure");check(Arrays.equals(before,SaveCodec.encode(w)),"AP rejection atomically preserves save/RNG");}
            else check(w.actionPoints[0]==points-20,"native AP debit once");
        }
        try(InputStream stream=PcConstructionTest.class.getResourceAsStream("/pre-base-construction-v33.sg11")){
            check(stream!=null,"genuine old construction fixture exists");byte[] raw=stream.readAllBytes();World old=SaveCodec.decode(raw);
            check(Arrays.equals(raw,SaveCodec.encode(old)),"existing save round trip unchanged");
            check(old.domestic.facility(1).level==3&&old.domestic.facility(1).remaining==0,"old completed Lv3 retained");
            check(Domestic.facilityEffect(old.domestic.facility(1)).equals("每月金 +600"),"old Lv3 description retains old effect");
            check(old.domestic.facility(2).level==3&&old.domestic.facility(2).remaining>0,"old in-progress Lv3 retained");
            int gold=old.city(10).gold;World replay=SaveCodec.decode(raw);
            check(old.domestic.cancelBuild(2).ok&&old.city(10).gold==gold,"cancel old build neither refunds nor charges new cost");
            for(int n=0;n<2;n++)check(replay.nextTurn().ok,"old pending construction normal continuation");
            check(replay.domestic.facility(2).level==3&&replay.domestic.facility(2).remaining==0,"old pending Lv3 completes as saved");
        }
        System.out.println("PASS PcConstructionTest checks="+checks);
    }
}
