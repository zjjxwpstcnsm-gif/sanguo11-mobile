package game.sanguo.mobile;
import game.sanguo.core.*;
import game.sanguo.api.*;
import game.sanguo.runtime.query.*;
import java.nio.file.*;
import java.util.*;

/** Read-only projection of an actual installed save; not normal Android acceptance. */
public final class SceneFactsPresentationProbe {
 static int checks;
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception {
  World world=SaveCodec.decode(Files.readAllBytes(Path.of(args[0])));byte[] before=SaveCodec.encode(world);
  StateToken token=new StateToken("5910add4-111a-45b6-b8c8-947b90826337",1,10); // B21 recorded live token.
  SceneFactsSnapshot facts=SceneFactsQuery.capture(world,token);
  check(SceneFactsPresentation.accept(world.mapId,world.mapRevision,world.terrainRevision,world.scenarioId,world.dataHash,world.turn,world.player,token,facts)==facts,"accept exact recorded context");
  for(StateToken stale:List.of(new StateToken(token.sessionId,1,9),new StateToken(token.sessionId,2,10),new StateToken("other",1,10),new StateToken(token.sessionId,1,10+(1L<<32)))){
   try{SceneFactsPresentation.accept(world.mapId,world.mapRevision,world.terrainRevision,world.scenarioId,world.dataHash,world.turn,world.player,stale,facts);throw new AssertionError("accepted stale token");}catch(IllegalArgumentException expected){checks++;}
  }
  MapSceneSnapshot scene=new MapSceneSnapshot(new MapSceneSnapshot.Ground(world),world,null,-1,token,facts);
  check(scene.authoritativeSceneFacts&&scene.state.equals(token),"bound authority fact identity");
  check(scene.fires.size()==2,"actual two campaign fires");
  for(SceneFactsSnapshot.Fire fire:facts.fires){
   MapSceneSnapshot.FireState shown=scene.fires.stream().filter(f->f.hex.equals(new Hex(fire.cell.q,fire.cell.r))).findFirst().orElseThrow();
   check(shown.remaining==fire.remaining&&shown.owner==fire.owner&&shown.power==fire.power&&shown.trap==fire.trap,"fire exact live facts");
   check(shown.sourceX==fire.cell.sourceX&&shown.sourceY==fire.cell.sourceY,"source coords copied, not reinterpreted");
  }
  for(SceneFactsSnapshot.Military f:facts.military){
   MapSceneSnapshot.Item item=scene.items.stream().filter(x->x.key.equals("structure:"+f.id)).findFirst().orElseThrow();
   check(item.hex.equals(new Hex(f.cell.q,f.cell.r))&&item.facility.hp==f.hp&&item.facility.maxHp==f.maxHp&&item.facility.complete==f.complete&&item.facility.direction==f.direction&&item.facility.builderUnitId==f.builderUnitId,"military builder/status/HP copied");
  }
  StateToken wide=new StateToken(token.sessionId,Long.MAX_VALUE-1,Long.MAX_VALUE-2);
  check(SceneFactsPresentation.accept(world.mapId,world.mapRevision,world.terrainRevision,world.scenarioId,world.dataHash,world.turn,world.player,wide,SceneFactsQuery.capture(world,wide)).state.equals(wide),"exact64bit token");
  OfficerSnapshot.Officer builder=OfficerQuery.capture(world,token).officers.stream().filter(o->o.id==2003).findFirst().orElseThrow();
  OfficerSnapshot.SourceInfo source=builder.source;
  PortraitMediaIdentity media=new PortraitMediaIdentity(builder.id,source.nativeId,source.sourceVariant,source.sourcePath,source.sourceSha,source.recordSha,source.canonicalOfficerId,source.originalVoiceProfile,source.originalFields);
  check(media.nativeId==78&&media.originalVoiceProfile==2,"actual stored native/voice connection, not caller inference");
  check(media.originalFields.equals(source.originalFields)&&Objects.equals(media.canonicalOfficerId,source.canonicalOfficerId),"original fields/canonical preserved");
  try{media.originalFields.put(48,5);throw new AssertionError("mutable fields");}catch(UnsupportedOperationException expected){checks++;}
  check(Arrays.equals(before,SaveCodec.encode(world)),"complete world/bothRNG untouched by projection");
  System.out.println("PASS actual installed-source projection "+checks+" checks; host only, no normal APK/fire13/caller acceptance");
 }
}
