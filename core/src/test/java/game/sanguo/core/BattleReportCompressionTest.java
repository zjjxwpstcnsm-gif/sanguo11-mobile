package game.sanguo.core;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/** Compare actual report/save output with the unbuffered writer frozen at06db767.
 * The oracle retains its original field ordering and compression calls. */
public final class BattleReportCompressionTest {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    private static void string(DataOutputStream d,String text)throws IOException{byte[] raw=text.getBytes(StandardCharsets.UTF_8);d.writeInt(raw.length);d.write(raw);}
    private static byte[] original(BattleReports reports)throws Exception {
        List<BattleReports.Entry> entries=new ArrayList<>(reports.query(-1,0,BattleReports.Scope.ALL,null,""));Collections.reverse(entries);
        Field next=BattleReports.class.getDeclaredField("nextId");next.setAccessible(true);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream d=new DataOutputStream(new GZIPOutputStream(bytes))){d.writeLong(next.getLong(reports));d.writeInt(entries.size());for(BattleReports.Entry e:entries){d.writeLong(e.id);d.writeInt(e.turn);d.writeInt(e.actor);d.writeLong(e.related);d.writeByte(e.kind.ordinal());string(d,e.title);string(d,e.detail);d.writeBoolean(e.location!=null);if(e.location!=null){d.writeInt(e.location.q);d.writeInt(e.location.r);}}}
        ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(out);byte[] payload=bytes.toByteArray();d.writeInt(payload.length);d.write(payload);return out.toByteArray();
    }
    private static byte[] current(BattleReports reports)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();reports.write(new DataOutputStream(bytes));return bytes.toByteArray();}
    private static void verify(World w,String name)throws Exception{
        long rng=w.strategy.getRandomState();int count=w.reports.size();byte[] save=SaveCodec.encode(w);
        check(Arrays.equals(original(w.reports),current(w.reports)),name+" exact original compressed fields/CRC/header");
        World restored=SaveCodec.decode(save);
        check(Arrays.equals(save,SaveCodec.encode(restored)),name+" whole save byte exact after restore");
        check(restored.reports.size()==count&&restored.strategy.getRandomState()==rng,name+" all reports and rule RNG retained");
        check(Arrays.equals(save,SaveCodec.encode(w)),name+" comparison changes no authority");
    }
    public static void main(String[] args)throws Exception{
        for(ScenarioCatalog.Summary row:ScenarioCatalog.summaries())verify(ScenarioCatalog.load(row.id,0,12345),"opening "+row.id);
        World w=ScenarioCatalog.load("coalition-190",0,12345);
        for(int turn=1;turn<=6;turn++){check(w.nextTurn().ok,"normal complete turn "+turn);verify(w,"actual turn "+turn);}
        // A real report note uses UTF8 length prefixes and authoritative deltas;
        // include text beyond writeUTF's 64KiB limit and gzip buffer boundaries.
        StringBuilder text=new StringBuilder();for(int n=0;n<12000;n++)text.append("制造·技巧：").append(n).append('汉');
        w.reports.note(text.toString());verify(w,"long unicode note");
        for(int n=0;n<1000;n++){w.home().gold=(w.home().gold+1)%100000;w.reports.note("实际资源变化 "+n);}
        verify(w,"many actual resource notes");
        long began=System.nanoTime();byte[] old=original(w.reports);long oldNanos=System.nanoTime()-began;
        began=System.nanoTime();byte[] buffered=current(w.reports);long bufferedNanos=System.nanoTime()-began;
        check(Arrays.equals(old,buffered),"benchmark still checks every output byte");
        System.out.println("PASS BattleReportCompressionTest checks="+checks+" reports="+w.reports.size()+" packedBytes="+buffered.length+" originalNs="+oldNanos+" bufferedNs="+bufferedNanos+" timing is observed, not an assertion or ARM claim");
    }
}
