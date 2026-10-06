package game.sanguo.mobile;
import game.sanguo.api.StateToken;
import java.util.*;
/** Immutable admitted B burning facts and source-derived positions, never authority. */
final class PcCellFireSet {
 static final int MAX_CELLS=128;
 final StateToken state;
 final List<Cell> cells;
 final int total,omitted;
 static final class Cell {
  final int x,y;final PcCellFirePosition position;
  Cell(int x,int y,PcCellFirePosition position){this.x=x;this.y=y;this.position=position;}
 }
 private PcCellFireSet(StateToken state,List<Cell> cells,int total){this.state=state;this.cells=Collections.unmodifiableList(cells);this.total=total;omitted=total-cells.size();}
 static PcCellFireSet from(MapSceneSnapshot snapshot,StateToken expected){
  if(snapshot==null||!snapshot.authoritativeSceneFacts||snapshot.state==null||expected==null||!expected.equals(snapshot.state)||snapshot.ground.pcMap==null)return null;
  List<MapSceneSnapshot.FireState> fires=new ArrayList<>(snapshot.fires);
  fires.sort(Comparator.comparingInt((MapSceneSnapshot.FireState f)->f.sourceX==null?Integer.MAX_VALUE:f.sourceX).thenComparingInt(f->f.sourceY==null?Integer.MAX_VALUE:f.sourceY));
  List<Cell> cells=new ArrayList<>();Set<Integer> seen=new HashSet<>();int total=0;
  for(var f:fires){
   if(f.remaining<=0)continue;
   if(f.sourceX==null||f.sourceY==null)throw new IllegalArgumentException("Authoritative fire missing source cell");
   int x=f.sourceX,y=f.sourceY;if(!seen.add(y*200+x))throw new IllegalArgumentException("Duplicate source fire cell");
   var point=PcCellFirePosition.source(snapshot.ground.pcMap,x,y);total++;
   // No original current-height contract for authored visual overrides yet.
   if(!snapshot.ground.surface.overrides.isEmpty()||cells.size()>=MAX_CELLS)continue;
   cells.add(new Cell(x,y,point));
  }
  return new PcCellFireSet(expected,cells,total);
 }
}
