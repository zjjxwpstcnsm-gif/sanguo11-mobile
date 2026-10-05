package game.sanguo.mobile;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import game.sanguo.core.*;

/** Cross-language source geometry, source-state binding, fractional curves and bounded input. */
public final class PcUnitsRestorationTest {
    private static int checks;private static float maximumError;
    private static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("raw-source PCUREF path required");
        Path asset=Path.of("app/src/main/assets/3d/pc-units/units.pcz");PcUnits units=new PcUnits(Files.newInputStream(asset));
        ByteBuffer r=ByteBuffer.wrap(Files.readAllBytes(Path.of(args[0]))).order(ByteOrder.LITTLE_ENDIAN);
        check(r.getLong()==0x3230464552554350L,"independent raw source reference header");int cases=r.getInt();check(cases>300,"all table-bound source model/clip pairs and fractional source frames");
        for(int i=0;i<cases;i++){
            int model=r.getInt(),clip=r.getInt();float frame=r.getFloat();int nv=r.getInt(),ni=r.getInt(),opaque=r.getInt();SceneMesh mesh=units.pose(model,clip,frame);
            check(mesh.pcUnit&&mesh.authoredTangentFrame,"native skin tagged; no legacy geometry");check(mesh.vertices.length==nv*7&&mesh.indices.length==ni,"all source vertices/triangles retained");
            for(int v=0;v<nv;v++)for(int k=0;k<3;k++){
                float expected=r.getFloat(),actual=mesh.vertices[v*7+k],error=Math.abs(actual-expected);maximumError=Math.max(maximumError,error);
                check(Float.isFinite(actual)&&error<2e-5f,"source pose coordinate model="+model+" clip="+clip+" frame="+frame+" vertex="+v+" axis="+k+" error="+error);
            }
            for(int v=0;v<nv;v++)for(int k=0;k<4;k++)check(Float.floatToRawIntBits(r.getFloat())==Float.floatToRawIntBits(mesh.vertices[v*7+3+k]),"raw PC BGRA color retained exactly");
            check(mesh.pcUnitOpaqueIndices==opaque,"original opaque/transparent index split retained");
            for(int v=0;v<nv;v++)for(int k=0;k<2;k++)check(Float.floatToRawIntBits(r.getFloat())==Float.floatToRawIntBits(mesh.uv[v*2+k]),"raw PC UV retained exactly for independent original sheet");
            for(int n=0;n<ni;n++)check(r.getInt()==mesh.indices[n],"original source strip topology/winding retained exactly");
            for(float f:mesh.tangents)check(Float.isFinite(f),"skinned original normals produce finite tangent frame");
        }
        check(!r.hasRemaining(),"all independent reference records consumed");
        int[] expected={0,1,2,3,4,5,10,11,12,13,7,8,9,6};
        for(int kind=0;kind<14;kind++)for(int state=0;state<8;state++){
            check(units.model(kind,state)==(kind==3&&state==2?0:kind==4&&state==3?5:expected[kind]),"EXE per-state source model selection including crossbow/cavalry override");
            check(units.select(kind,state,-10).frame()==0&&units.select(kind,state,99999).frame()==units.frames(kind,state)-1,"source duration clamps");
        }
        for(int troops=0;troops<=20000;troops+=125){check(PcUnits.members(troops,false)==Math.min(76,24+Math.min(15000,troops)/250),"source foot/cavalry count");check(PcUnits.members(troops,true)==1,"source single transport/siege/ship");}
        for(String kind:new String[]{"SWORD","CROSSBOW","crossbow-melee","mounted-archery"}){
            var prepared=PcUnitsFixture.prepare(kind,"attack");World before=SaveCodec.decode(SaveCodec.encode(prepared.world()));World command=SaveCodec.decode(SaveCodec.encode(before));
            TurnJournal journal=new TurnJournal(command);check(prepared.command(command).ok,"normal recorded source combat");journal.close();
            MapSceneSnapshot snapshot=new MapSceneSnapshot(new MapSceneSnapshot.Ground(before),before,null,-1);byte[] authority=SaveCodec.encode(before);
            for(TurnJournal.Event event:journal.events())for(int hit=0;hit<event.strikes.size();hit++){
                TurnJournal.Strike strike=event.strikes.get(hit);final int actor=strike.actorId;
                MapSceneSnapshot.Item item=snapshot.items.stream().filter(i->i.unit!=null&&i.unit.id==actor).findFirst().orElseThrow();
                float fraction=(hit+.81f)/event.strikes.size();UnitAnimation animation=new UnitAnimation();animation.sample(item,snapshot.ground,event,fraction,1000,0);
                PcUnits.Selection selected=units.animation(item.unit,animation,1000,event,fraction);check(animation.clip.equals("attack"),"actual actor executes its own strike phase");
                int sourceKind=PcUnits.kind(item.unit,animation.naval);boolean ranged=(sourceKind==3||sourceKind==4)&&strike.start.distance(strike.target)>1;
                check(selected.state()==(ranged?3:2),"normal ranged or melee journal selects original rig state");
                if(ranged)check(selected.model()==(sourceKind==4?5:3),"actual mounted archery/crossbow keeps original ranged source model");
                else if(sourceKind==3)check(selected.model()==0,"actual close crossbow attack uses EXE sword override");
                float local=(.81f-CombatVisual.LAUNCH)/(1-CombatVisual.LAUNCH);
                check(Math.abs(selected.frame()-(int)(local*(units.frames(sourceKind,selected.state())-1)))<=1,"each counter/support source motion uses local strike phase");
            }
            check(java.util.Arrays.equals(authority,SaveCodec.encode(before)),"native visual event queries preserve authority and RNG");
        }
        for(String kind:new String[]{"BOAT","TOWER_SHIP","WARSHIP"}){
            var prepared=PcUnitsFixture.prepare(kind,"move");World boatWorld=prepared.world();byte[] authority=SaveCodec.encode(boatWorld);
            MapSceneSnapshot snapshot=new MapSceneSnapshot(new MapSceneSnapshot.Ground(boatWorld),boatWorld,prepared.focus(),-1);
            MapSceneSnapshot.Item boat=snapshot.items.stream().filter(i->i.unit!=null&&i.unit.id==prepared.actor()).findFirst().orElseThrow();
            float x=snapshot.ground.grid.x(boat.hex),z=snapshot.ground.grid.z(boat.hex),water=snapshot.ground.surface.pcWaterHeight(x,z);
            PcUnitFormation formation=new PcUnitFormation(units);formation.sample(boat.unit,boat.unit.troops,true,snapshot.ground,x,z,0);
            check(water>0&&formation.rootY==water+.02f,"source "+kind+" hull follows authored water plane instead of old constant root");
            check(formation.count==1&&formation.members[1]==0,"original ship remains one source member on the water plane");
            check(!formation.sample(boat.unit,boat.unit.troops,true,snapshot.ground,x,z,0),"unchanged ship contact uses its cached original inputs");
            check(java.util.Arrays.equals(authority,SaveCodec.encode(boatWorld)),"source ship contact does not change rules/RNG/save");
        }
        byte[] truncated=Files.readAllBytes(asset);truncated=java.util.Arrays.copyOf(truncated,truncated.length-10);boolean rejected=false;try{new PcUnits(new ByteArrayInputStream(truncated));}catch(IOException e){rejected=true;}check(rejected,"truncated original unit asset rejected");
        System.out.println("PASS PC UNITS raw source cases="+cases+" checks="+checks+" max display-space float error="+maximumError+"; GPU/PC visual reference separately required");
    }
}
