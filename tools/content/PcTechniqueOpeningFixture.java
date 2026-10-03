package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** A newly initialized project scenario on the installed APK, never a migrated old save. */
public final class PcTechniqueOpeningFixture {
 public static void main(String[] args)throws Exception{
  if(args.length!=1)throw new IllegalArgumentException("fresh output path required");
  World w=ScenarioCatalog.load("coalition-190",0,20261003L);
  if(!w.pcTechniquePoints.enabled()||!w.pcProduction.enabled()||!w.officerAbilities.enabled()||!w.merchantMarket.enabled())throw new AssertionError("normal new scenario profile");
  byte[] raw=SaveCodec.encode(w);if(java.nio.ByteBuffer.wrap(raw).getInt(4)!=37||!Arrays.equals(raw,SaveCodec.encode(SaveCodec.decode(raw))))throw new AssertionError("exact new save roundtrip");
  Files.write(new java.io.File(args[0]).toPath(),raw);System.out.println("PASS new project coalition190 production opening v37 bytes="+raw.length+" officers="+w.officers.size()+" cities="+w.cities.size()+" official identity pending");
 }
}
