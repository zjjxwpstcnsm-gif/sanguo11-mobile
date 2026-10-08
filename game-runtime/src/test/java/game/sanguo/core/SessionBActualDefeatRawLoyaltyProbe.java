package game.sanguo.core;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;

/** Exact Android failed normal defeat. Reads only, preserving full World and dual RNG. */
public final class SessionBActualDefeatRawLoyaltyProbe {
 public static void main(String[]args)throws Exception{
  Path file=Path.of(args[0]);byte[] original=Files.readAllBytes(file);World w=SaveCodec.decode(original);byte[] before=SaveCodec.encode(w);var s=w.contests.current();
  if(s==null||s.nativeDuel==null||!s.nativeDuel.terminal())throw new AssertionError("Actual native terminal required");var c=s.nativeDuel;var f=c.facts();
  int winner=PcDuelKernel.readManager(c.state.manager,0x54),loser=PcDuelKernel.readManager(c.state.manager,0x58),winSlot=PcDuelKernel.readManager(c.state.manager,0x5c),loseSlot=PcDuelKernel.readManager(c.state.manager,0x60);
  int target=c.state.officers[loser*3+loseSlot],captor=c.state.officers[winner*3+winSlot];
  System.out.println("ACTUAL sha="+PcCommandCapacityPolicy.hex(MessageDigest.getInstance("SHA-256").digest(original))+" round="+f.round+" frames="+f.frames+" inputs="+f.inputs+" winner="+winner+" target="+target+" captor="+captor);
  try(var in=new DataInputStream(new ByteArrayInputStream(w.extensions.get(PcDuelRawLoyalty.NAMESPACE)))){
   in.readInt();int format=in.readInt();for(int i=0;i<4;i++)in.readUTF();int count=in.readInt();
   for(int i=0;i<count;i++){int id=in.readInt(),nativeId=in.readInt(),owner=in.readInt(),display=in.readInt(),raw=in.readUnsignedByte();boolean trusted=in.readBoolean();if(id==target||id==captor){var o=w.officer(id);System.out.println("RAW_ROW format="+format+" stable="+id+" native="+nativeId+" storedOwner="+owner+" currentOwner="+o.owner+" storedDisplay="+display+" currentDisplay="+o.loyalty+" raw="+raw+" trusted="+trusted+" editorEdited="+w.editor.edited());}}
  }
  String error;try{PcDuelRecruitmentAdmission.preview(w,target,captor,1);error="NONE";}catch(IOException e){error=e.getMessage();}System.out.println("CURRENT_ADMISSION "+error);
  if(!Arrays.equals(before,SaveCodec.encode(w))||!Arrays.equals(original,Files.readAllBytes(file)))throw new AssertionError("Diagnostic changes original actual file/World/RNG");
  System.out.println("PASS exact failed normal defeat/read-only raw trust diagnosis; no value backfill or probability changed");
 }
}
