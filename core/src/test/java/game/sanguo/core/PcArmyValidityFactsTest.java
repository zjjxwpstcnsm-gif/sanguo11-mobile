package game.sanguo.core;
import java.util.*;import java.nio.file.*;
/** Original object-validity mask from full native16source oracles; raw owner0 is retained separately. */
public final class PcArmyValidityFactsTest {
 public static void main(String[] args)throws Exception{int checks=0,inactive=0;for(int source=0;source<16;source++){
  World w=PcScenarioCatalog.preview(PcScenarioCatalog.all().get(source).identity.scenarioId);byte[] before=SaveCodec.encode(w);var view=PcGovernorPolicy.view(w);var expected=PcGovernorPolicy.source(w);
  for(var army:view.armies){if(army.originalValid!=expected.armyOriginalValid.get(army.nativeId))throw new AssertionError("Native validity projection differs");if(!army.originalValid&&army.owner==0)inactive++;checks++;}
  if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("Original validity query mutates whole world/RNG");checks++;
  if(!Arrays.equals(before,SaveCodec.encode(SaveCodec.decode(before))))throw new AssertionError("Original validity changes saved policy");checks++;
 }if(inactive!=387)throw new AssertionError("Expected387 original inactive owner0 records, got "+inactive);System.out.println("PASS original army validity "+checks+" checks/16sources/387inactive raw-owner0 records; raw ownership and fullSave/RNG preserved");}
}
