package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Original589f70 and50de30 field-command input. The nominated fighter is
 * first; other crew join only through original589ba0 health/dislike rules. */
final class PcDuelEntryRules {
    static List<Integer> crew(World w,World.Unit unit,int nominated)throws IOException {
        if(unit==null||w.unit(unit.id)!=unit||!w.army.contains(unit,nominated)||!w.life.present(nominated))throw new IOException("原单挑指定人物不在当前编队");
        PcDuelAdmissionRules.current(w,nominated);var ids=new ArrayList<Integer>();ids.add(nominated);
        for(var o:w.army.crew(unit))if(o.id!=nominated&&w.life.present(o.id)&&!w.government.captive(o.id)&&!w.relations.dislikes(o.id,nominated)&&PcDuelAdmissionRules.score(PcDuelAdmissionRules.current(w,o.id),false)>0)ids.add(o.id);
        if(ids.size()>3)throw new IOException("原单挑编队超过三人");return List.copyOf(ids);
    }
    /** Original589f70 scene: rings1..2 of native city0/gate1/fort4/5
     * take precedence; forest5 at target or ring1 selects scene2. Native
     * validity does not require the construction-complete flag here. */
    static int scene(boolean nearSceneSite,boolean targetForest,boolean adjacentForest){return nearSceneSite?0:targetForest||adjacentForest?2:1;}
    static int currentScene(World w,Hex target)throws IOException {
        if(!w.pcSourceFrame||!NationalMap.ID.equals(w.mapId)||PcScenarioIdentity.saved(w)==null||target==null||!w.inside(target))throw new IOException("原单挑场景当前地图来源无效");
        boolean near=false;for(var city:w.cities)if(city.kind==World.SiteKind.CITY||city.kind==World.SiteKind.GATE){int distance=target.distance(city.hex);near|=city.defense>0&&distance>0&&distance<=2;}
        for(var structure:w.war.structures)if(structure.hp>0&&(structure.kind==War.StructureKind.FORT||structure.kind==War.StructureKind.FORTRESS)){int distance=target.distance(structure.hex);near|=distance>0&&distance<=2;}
        boolean forest=false;for(Hex hex:target.neighbors())forest|=w.inside(hex)&&w.terrain[hex.q][hex.r]==World.Terrain.FOREST;
        return scene(near,w.terrain[target.q][target.r]==World.Terrain.FOREST,forest);
    }
    /** Existing platform supports one human force and directly controlled
     * field units. Original player slot0 and AI slot-1 remain separate from
     * the original inverse manual flags. Command/RNG/action owner is separate. */
    static Input prepareCurrent(World w,World.Unit left,World.Unit right,int own,int other,PcDuelKernel.OriginalSettings settings)throws IOException {
        if(left==null||right==null||left.owner!=w.player||right.owner==w.player||!w.districts.directUnit(left.id)||!w.campaign.hostile(left.owner,right.owner))throw new IOException("当前原单挑人控编队无效");
        return prepare(w,left,right,own,other,0,-1,true,false,currentScene(w,right.hex),settings);
    }
    static final class Input {
        final int[]officers=new int[6],natives=new int[6];
        final byte[]manager=new byte[0xd0];
        final PcDuelKernel.Actor[][]actors=new PcDuelKernel.Actor[2][3];
        final int[][][]held=new int[6][][];
    }
    /** Context values are current original force+60 (player slot), not army
     * control or strength. Scene is a separately bound original background. */
    static Input prepare(World w,World.Unit left,World.Unit right,int own,int other,int leftContext,int rightContext,boolean leftManual,boolean rightManual,int scene,PcDuelKernel.OriginalSettings settings)throws IOException {
        if(leftContext< -1||leftContext>7||rightContext< -1||rightContext>7||scene<0||scene>2)throw new IOException("原单挑当前势力/场景输入无效");
        var input=new Input();Arrays.fill(input.officers,-1);Arrays.fill(input.natives,-1);
        for(int at:new int[]{0x20,0x24,0x28,0x2c,0x34,0x38,0x3c,0x4c,0x50,0x54,0x58,0x5c,0x60})PcDuelKernel.writeManager(input.manager,at,-1);
        PcDuelKernel.writeManager(input.manager,0x30,29);PcDuelKernel.writeManager(input.manager,0x34,2);PcDuelKernel.writeManager(input.manager,0x40,50);PcDuelKernel.writeManager(input.manager,0x44,scene);
        var sides=List.of(crew(w,left,own),crew(w,right,other));var units=new World.Unit[]{left,right};int[]contexts={leftContext,rightContext};boolean[]manual={leftManual,rightManual};
        for(int side=0;side<2;side++){
            PcDuelKernel.writeManager(input.manager,0x18+4*side,units[side].id);PcDuelKernel.writeManager(input.manager,0x20+4*side,0);PcDuelKernel.writeManager(input.manager,0x28+4*side,contexts[side]);PcDuelKernel.writeManager(input.manager,0x38+4*side,manual[side]?0:1);
            for(int slot=0;slot<3;slot++){
                int index=3*side+slot,id=slot<sides.get(side).size()?sides.get(side).get(slot):-1;
                PcDuelKernel.writeManager(input.manager,0x7c+12*side+4*slot,100);PcDuelKernel.writeManager(input.manager,0xac+12*side+4*slot,-1);
                if(id<0){input.actors[side][slot]=new PcDuelKernel.Actor(-1,0,0,0,false,false);input.held[index]=new int[0][2];continue;}
                input.officers[index]=id;input.natives[index]=PcDuelSourceFacts.saved(w).get(id).nativeId;input.held[index]=PcNativeItemPolicy.held(w,id);input.actors[side][slot]=PcDuelBindings.actor(w,id,input.held[index],settings);
                PcDuelKernel.writeManager(input.manager,index*4,id);PcDuelKernel.writeManager(input.manager,0x7c+12*side+4*slot,PcDuelHealthPolicy.health(w,id));PcDuelKernel.writeManager(input.manager,0xac+12*side+4*slot,w.contests.injury(id));
            }
        }
        return input;
    }
    private PcDuelEntryRules(){}
}
