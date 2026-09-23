package game.sanguo.core;
import game.sanguo.core.map.SourceGridCoord;
import static game.sanguo.core.Native56Checks.*;
public final class ScenarioNationalMapTest {
 public static void main(String[] args)throws Exception {
  World national=world();int count=0,full=0;
  for(ScenarioCatalog.Summary summary:ScenarioCatalog.summaries()){
   World w=ScenarioCatalog.load(summary.id,0,56L);count++;
   check(w.mapId.equals(NationalMap.ID)&&w.mapLayout.equals(NationalMap.LAYOUT)&&w.mapRevision==NationalMap.REVISION,"shared authority "+summary.id);
   if(w.sourceColumns()==200&&w.sourceRows()==200)full++;
   for(int y=0;y<w.sourceRows();y++)for(int x=0;x<w.sourceColumns();x++){
    Hex local=MapCoordinates.axial(w,new SourceGridCoord(x,y));Hex global=MapCoordinates.axial(national,new SourceGridCoord(x+w.sourceOriginX,y+w.sourceOriginY));
    check(w.terrain[local.q][local.r]==national.terrain[global.q][global.r],"same native cell in every era/crop");
   }
   check(SaveCodec.decode(SaveCodec.encode(w)).mapRevision==NationalMap.REVISION,"scenario save revision");
  }
  check(count==9&&full==7,"nine packs share map, two explicit crops");pass("ScenarioNationalMapTest");
 }
}
