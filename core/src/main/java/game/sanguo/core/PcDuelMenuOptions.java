package game.sanguo.core;
import java.io.*;import java.util.*;
/** Source menu choices, independent of a World. Defaults and the sourceflag18
 * automatic override are not inferred from this source0 callback receipt. */
public final class PcDuelMenuOptions {
 public static final String RECEIPT_SHA="ec3aa4c89e4edd0a78f4b7c92031e09ff0f46036f3a52c51034c18f0323539fc";
 private static final String SHA="9f3378a20f400498c5660dc1f7ea8641600b24054ae4975e31ebeef4fb11a424";
 public static final class Choice {
  public final String field,label,rawHex;public final int value,controlId;
  private Choice(String[]p){field=p[0];value=Integer.parseInt(p[1]);label=p[2];rawHex=p[3];controlId=Integer.parseInt(p[4]);}
 }
 private static List<Choice> cached;
 public static synchronized List<Choice> choices()throws IOException {
  if(cached!=null)return cached;byte[]raw;try(var in=PcDuelMenuOptions.class.getResourceAsStream("/pc-duel/newgame-options.tsv")){if(in==null)throw new IOException("原新局选项资源缺失");raw=PcResourceBytes.readUpTo(in,4097);if(raw.length>4096)throw new IOException("原新局选项资源过大");}
  try{if(!PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)).equals(SHA))throw new IOException("原新局选项资源SHA不同");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
  String[]lines=new String(raw,java.nio.charset.StandardCharsets.UTF_8).split("\n");if(lines.length!=11||!lines[0].equals("# original newgame option callbacks "+RECEIPT_SHA)||!lines[1].equals("# executable "+PcScenarioIdentity.EXE_SHA))throw new IOException("原新局选项来源不同");List<Choice>list=new ArrayList<>();String[]fields={"difficulty","death","life"},ui={"0x214","0x218","0x22c"},root={"0x20","0x24","0x38"};
  for(int k=0;k<9;k++){String[]p=lines[k+2].split("\t",-1);if(p.length!=7||!p[0].equals(fields[k/3])||!p[1].equals(Integer.toString(k%3))||p[2].isEmpty()||!p[3].matches("[0-9a-f]+")||!p[5].equals(ui[k/3])||!p[6].equals(root[k/3]))throw new IOException("原新局选项列或编号不同");try{list.add(new Choice(p));}catch(NumberFormatException e){throw new IOException(e);}}
  return cached=List.copyOf(list);
 }
 private PcDuelMenuOptions(){}
}
