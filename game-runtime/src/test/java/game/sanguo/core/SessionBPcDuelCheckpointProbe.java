package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Read-only actual saved model; no fixture phase/health/outcome mutation. */
public final class SessionBPcDuelCheckpointProbe {
 public static void main(String[]args)throws Exception{
  byte[]raw=Files.readAllBytes(Path.of(args[0]));World w=SaveCodec.decode(raw);var d=w.contests.session.nativeDuel;var m=d.state.model;var src=PcScenarioIdentity.saved(w);List<String>fighters=new ArrayList<>();
  for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){int at=m.fighter(side,slot);fighters.add("{\"side\":"+side+",\"slot\":"+slot+",\"native\":"+d.state.natives[3*side+slot]+",\"health\":"+m.get(at+4)+",\"spirit\":"+m.get(at+8)+",\"injury\":"+m.get(at+12)+",\"stance\":"+m.get(at+16)+",\"status\":"+m.get(at+20)+"}");}
  String result="{\"sourceId\":\""+src.scenarioId+"\",\"sourceSha\":\""+src.sha+"\",\"phase\":"+m.get(4)+",\"next\":"+m.get(8)+",\"sub\":"+m.get(12)+",\"round\":"+m.get(16)+",\"roundLimit\":"+m.get(24)+",\"frames\":"+d.frames+",\"inputs\":"+d.inputs+",\"rng\":"+Integer.toUnsignedLong(d.state.random.state)+",\"natives\":"+Arrays.toString(d.state.natives)+",\"modelHex\":\""+HexFormat.of().formatHex(m.state)+"\",\"managerHex\":\""+HexFormat.of().formatHex(d.state.manager)+"\",\"fighters\":["+String.join(",",fighters)+"]}";
  if(!Arrays.equals(raw,SaveCodec.encode(w)))throw new AssertionError("Inspection changes complete save");Files.writeString(Path.of(args[1]),result+"\n");if(args.length>2){
   var runtime=PcDuelRuntimeFacts.saved(w);PcDuelKernel.Actor[][]actors=new PcDuelKernel.Actor[2][3];for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){int id=d.state.officers[side*3+slot];actors[side][slot]=id<0?new PcDuelKernel.Actor(-1,0,0,0,false,false):PcDuelBindings.actor(w,id,d.settings);}
   PcDuelCampaign.SupportBinding support=model->{var facts=new PcDuelKernel.SupportFacts[2][3];for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++){int id=d.state.officers[side*3+slot];facts[side][slot]=id<0?new PcDuelKernel.SupportFacts(new int[14]):PcDuelBindings.support(w,id,d.state.officers[side*3+model.activeIndex(side)],d.state.officers[(1-side)*3+model.activeIndex(1-side)],runtime,PcDuelRawLoyalty.current(w,id));}return facts;};var relations=PcDuelKinship.terminal(w,d.state.officers);
   for(int i=0;i<20;i++){System.out.println("frame="+i+" phase="+m.get(4)+" next="+m.get(8)+" sub="+m.get(12)+" pending="+m.get(20)+" selected="+m.get(0x598)+" move="+m.get(0x580)+" round="+m.get(16)+" rng="+Integer.toUnsignedLong(d.state.random.state));d.frame(actors,support,relations,false,0,-1);}
  }System.out.println(result.substring(0,result.indexOf(",\"modelHex")));System.out.println(fighters);
 }
}
