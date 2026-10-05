package game.sanguo.core;
import game.sanguo.api.*;import game.sanguo.runtime.*;import java.util.*;import java.nio.file.*;
/** Actual source fresh games, stable IDs, same officer DTO and cold saved identity. */
public final class PcGaijiIdentityTest {
 static int checks;static void require(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static final int[] IDS={156234,844857,598828,850922},CANONICAL={10184,10229,10249,10616};
 static final String[] NAMES={"孔伷","司馬伷","朱儁","劉璝"};
 static void verify(GameSession game)throws Exception{
  byte[] before=game.captureSave();World w=SaveCodec.decode(before);Map<Integer,PcOfficerIdentities.Fact> facts=PcOfficerIdentities.saved(w);require(facts.size()==4,"four saved identity links");OfficerSnapshot snapshot=game.officers();int verified=0;
  for(OfficerSnapshot.Officer row:snapshot.officers)if(row.source!=null&&row.source.identityStatus.equals("canonical-identity-verified"))verified++;
  require(verified==670,"strict identity count with per-person evidence, not appearance count");
  for(int i=0;i<4;i++){OfficerSnapshot.Officer row=snapshot.officer(IDS[i]);PcOfficerIdentities.Fact f=facts.get(IDS[i]);require(row!=null&&row.name.equals(NAMES[i])&&f.name.equals(row.name),"actual original Unicode name and stable runtime ID");require(row.source!=null&&row.source.canonicalOfficerId!=null&&row.source.canonicalOfficerId==CANONICAL[i]&&f.canonicalOfficerId==CANONICAL[i],"unique validated canonical identity");require(row.source.recordSha.equals(f.recordSha)&&row.source.nativeId==f.nativeId,"same source record identity");require(row.source.biography.equals(f.biography)&&!f.biography.isEmpty()&&row.source.courtesy.equals(f.courtesy),"actual original biography/courtesy same DTO");require(!row.source.unknown.contains("canonicalIdentityUnmapped")&&!row.source.originalInformation.contains("尚未映射"),"resolved identity gap only");}
 require(Arrays.equals(before,game.captureSave()),"queries preserve entire saved World and RNG");
 }
 static void turn(GameSession game,World control)throws Exception{
  TurnTicket ticket=game.beginTurn();World computed=SaveCodec.decode(ticket.initial());
  require(control.nextTurn().ok&&computed.nextTurn().ok&&game.commitTurn(ticket,computed),"normal complete turn transaction");
  require(Arrays.equals(game.captureSave(),SaveCodec.encode(control)),"full World and both RNGs match independent control");
 }
 static void rejectCorruption(byte[] world,int offset)throws Exception{
  World w=SaveCodec.decode(world);byte[] changed=w.extensions.get(PcOfficerIdentities.NAMESPACE).clone();changed[offset]^=1;w.extensions.put(PcOfficerIdentities.NAMESPACE,changed);
  boolean rejected=false;try{SaveCodec.encode(w);}catch(java.io.IOException expected){rejected=true;}require(rejected,"corrupt saved attestation rejected at "+offset);
 }
 static void attest(byte[] bytes)throws Exception{
  World w=SaveCodec.decode(bytes);byte[] raw=w.extensions.get(PcOfficerIdentities.NAMESPACE);
  java.io.ByteArrayInputStream buffer=new java.io.ByteArrayInputStream(raw);java.io.DataInputStream in=new java.io.DataInputStream(buffer);in.readInt();
  for(int i=0;i<3;i++)PcOfficerInfo.text(in,1024);int proof=raw.length-buffer.available()+4;PcOfficerInfo.text(in,64);in.readInt();int fact=raw.length-buffer.available();
  for(int offset:new int[]{proof,fact+3,fact+7,fact+11,fact+15,fact+19,fact+24})rejectCorruption(bytes,offset);
  require(Arrays.equals(bytes,SaveCodec.encode(SaveCodec.decode(bytes))),"rejected corrupted copies leave original complete save unchanged");
 }
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args.length>1?args[1]:"out/session1/gaiji25/cold");String mode=args.length>0?args[0]:"emit";Files.createDirectories(out);
  if(mode.equals("previous")){for(int source=0;source<16;source++){byte[] bytes=Files.readAllBytes(out.resolve(source+".sg11"));try(GameSession game=new GameSession(SaveCodec.decode(bytes))){verify(game);require(Arrays.equals(bytes,game.captureSave()),"previous identity text/faction/save bytes remain unchanged");}}System.out.println("PASS PcGaijiIdentityPrevious "+checks);return;}
  if(mode.equals("continue")){for(int source=0;source<16;source++){byte[] bytes=Files.readAllBytes(out.resolve(source+".sg11"));try(GameSession game=new GameSession(SaveCodec.decode(bytes))){require(Arrays.equals(bytes,game.captureSave()),"independent JVM full source saved bytes");verify(game);World control=SaveCodec.decode(bytes);turn(game,control);require(Arrays.equals(Files.readAllBytes(out.resolve(source+"-expected.sg11")),game.captureSave()),"cold fourth turn equals live continuation");verify(game);}}System.out.println("PASS PcGaijiIdentityCold "+checks);return;}
  int source=0;for(PcScenarioCatalog.Source summary:PcScenarioCatalog.all()){
   require(summary.strictCount==670,"new source menu strict identity count");World w=PcScenarioCatalog.preview(summary.identity.scenarioId);byte[] bytes=SaveCodec.encode(w);require(bytes[7]==38,"source format not upgraded");
   for(World.Officer ruler:w.officers)if(ruler.role==Strategy.Role.RULER&&ruler.owner>=0)require(w.factions[ruler.owner].equals(ruler.name),"fresh faction label uses same verified ruler name");
   if(source==0)attest(bytes);
   try(GameSession game=new GameSession(w)){
    verify(game);World control=SaveCodec.decode(bytes);World.City city=control.home();World.Officer actor=control.idle(city).get(0);
    require(control.patrol(city.id,actor.id).ok,"normal source patrol control");StateToken token=game.state();
    require(game.execute(new GameCommand(GameCommand.Operation.PATROL,token,city.id,actor.id)).ok(),"typed normal source patrol");
    require(!game.execute(new GameCommand(GameCommand.Operation.PATROL,token,city.id,actor.id)).ok(),"stale patrol rejected");
    require(Arrays.equals(SaveCodec.encode(control),game.captureSave()),"full command World and RNG parity");
    for(int n=0;n<3;n++)turn(game,control);verify(game);Files.write(out.resolve(source+".sg11"),game.captureSave());
    turn(game,control);Files.write(out.resolve(source+"-expected.sg11"),game.captureSave());
   }
   byte[] legacy=Files.readAllBytes(out.resolve("old").resolve(source+".sg11"));
   try(GameSession game=new GameSession(SaveCodec.decode(legacy))){require(PcOfficerIdentities.saved(SaveCodec.decode(game.captureSave())).isEmpty(),"missing old namespace stays missing");for(int id:IDS)require(game.officers().officer(id).source.canonicalOfficerId==null&&game.officers().officer(id).source.identityStatus.equals("source-only-gaiji"),"old source identity/text not backfilled");require(Arrays.equals(legacy,game.captureSave()),"old save/query full bytes and RNG unchanged");}
   source++;
  }
  require(source==16,"all16 distinct sources");System.out.println("PASS PcGaijiIdentityTest "+checks+" all16 source games/64 strict linked identities/original text/stable IDs/save DTO/old no-backfill");
 }
}
