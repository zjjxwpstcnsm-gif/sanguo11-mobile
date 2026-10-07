package game.sanguo.mobile;

import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.util.*;

/** Explicit detached fire-fact fixture; never a normal game/rule/Android test. */
public final class CellFireAdmissionProbe {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static SceneFactsSnapshot.Fire fire(World w,SourceGridCoord source,int remaining){
        Hex h=MapCoordinates.fromNationalSource(w,source);
        return new SceneFactsSnapshot.Fire(new SceneFactsSnapshot.Cell(h.q,h.r,source.x,source.y),w.player,remaining,10,false);
    }
    static MapSceneSnapshot snapshot(World w,StateToken token,List<SceneFactsSnapshot.Fire> fires){
        SceneFactsSnapshot facts=new SceneFactsSnapshot(token,true,w.mapId,w.mapRevision,w.terrainRevision,w.scenarioId,w.scenarioName,"fixture",w.dataHash,w.turn,w.player,0,1,0,null,fires,List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
        return new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1,token,facts);
    }
    public static void main(String[] args)throws Exception{
        int sources=0;
        for(var source:PcScenarioCatalog.all()){
            World w=PcScenarioCatalog.preview(source.identity.scenarioId);
            byte[] before=SaveCodec.encode(w);
            List<SourceGridCoord> land=new ArrayList<>();SourceGridCoord water=null;
            MapSceneSnapshot.Ground original=new MapSceneSnapshot.Ground(w);
            for(int x=0;x<200;x++)for(int y=0;y<200;y++){
                SourceGridCoord c=new SourceGridCoord(x,y);Hex h=MapCoordinates.fromNationalSource(w,c);
                if(!w.inside(h))continue;
                int t=original.pcMap.terrain(x,y);
                if(t==7||t==8){if(water==null&&original.surface.water(h))water=c;}
                else if(land.size()<3&&!original.surface.water(h))land.add(c);
            }
            check(land.size()==3&&water!=null,"actual source cells available");
            StateToken token=new StateToken("explicit-fixture-"+sources,1,2);
            var first=fire(w,land.get(0),3);var second=fire(w,land.get(1),2);var wet=fire(w,water,1);
            List<SceneFactsSnapshot.Fire> fires=List.of(first,second,wet,fire(w,land.get(2),0));
            MapSceneSnapshot base=snapshot(w,token,fires);
            PcCellFireSet baseline=PcCellFireSet.from(base,token);
            check(baseline.total==3&&baseline.cells.size()==3&&baseline.omitted==0,"baseline all actual source positions admitted");
            check(PcCellFireSet.from(base,new StateToken(token.sessionId,token.generation,token.revision+1))==null,"wrong full token rejected");
            w.visualMap=new MapPatch("explicit visual fixture","");
            SourceGridCoord distant=land.get(2);w.visualMap.heights.put(distant.x*200+distant.y,2300);
            PcCellFireSet unrelated=PcCellFireSet.from(snapshot(w,token,fires),token);
            check(unrelated.total==3&&unrelated.cells.size()==3&&unrelated.omitted==0,"unrelated height does not suppress all original fires");
            for(int i=0;i<baseline.cells.size();i++){
                var a=baseline.cells.get(i);var b=unrelated.cells.get(i);
                check(a.x==b.x&&a.y==b.y&&Float.floatToRawIntBits(a.position.x)==Float.floatToRawIntBits(b.position.x)&&Float.floatToRawIntBits(a.position.y)==Float.floatToRawIntBits(b.position.y)&&Float.floatToRawIntBits(a.position.z)==Float.floatToRawIntBits(b.position.z),"unchanged original native positions raw-bit exact");
            }
            w.visualMap.heights.put(land.get(0).x*200+land.get(0).y,2300);
            PcCellFireSet affected=PcCellFireSet.from(snapshot(w,token,fires),token);
            check(affected.total==3&&affected.cells.size()==2&&affected.omitted==1,"only affected land fire keeps unknown original height guard");
            check(affected.cells.stream().noneMatch(c->c.x==first.cell.sourceX&&c.y==first.cell.sourceY),"affected land cell not silently placed at invented height");
            w.visualMap.heights.put(water.x*200+water.y,2300);
            PcCellFireSet ignoredWater=PcCellFireSet.from(snapshot(w,token,fires),token);
            check(ignoredWater.cells.size()==2&&ignoredWater.omitted==1,"water paint ignored by actual terrain surface does not suppress water fire");
            check(Arrays.equals(before,SaveCodec.encode(w)),"fixture renderer leaves full Save/both RNG unchanged");
            sources++;
        }
        System.out.println("PASS explicit source fire admission fixture: "+checks+" checks / "+sources+" original sources; normal Android/rules/ARM not accepted");
    }
}
