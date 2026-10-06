package game.sanguo.core;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;

/** Reused ICU decoders must recover after failure and never share state across threads. */
public final class PcOfficerTextDecoderTest {
    private static byte[] packet(byte[] bytes)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(out);d.writeInt(bytes.length);d.write(bytes);return out.toByteArray();}
    private static String read(byte[] bytes)throws Exception{return PcOfficerInfo.text(new DataInputStream(new ByteArrayInputStream(packet(bytes))),32768);}
    private static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    private static void sequence()throws Exception{
        for(int i=0;i<100;i++){
            for(byte[] bad:List.of(new byte[]{(byte)0xc0,(byte)0xaf},new byte[]{(byte)0xe4,(byte)0xb8},new byte[]{(byte)0xff})){
                boolean rejected=false;try{read(bad);}catch(java.nio.charset.CharacterCodingException expected){rejected=true;}check(rejected,"malformed original UTF8 rejected");
                String value="原字形𠮷\u0001\n傳記";check(read(value.getBytes(StandardCharsets.UTF_8)).equals(value),"valid after malformed retains exact codepoints/control bytes");
            }
            check(read(new byte[0]).isEmpty(),"empty source strings remain valid");
        }
    }
    public static void main(String[] args)throws Exception{
        sequence();ExecutorService pool=Executors.newFixedThreadPool(4);
        try{List<Future<?>> work=new ArrayList<>();for(int i=0;i<4;i++)work.add(pool.submit(()->{try{sequence();}catch(Exception e){throw new RuntimeException(e);}}));for(Future<?> f:work)f.get();}finally{pool.shutdown();}
        System.out.println("PASS PcOfficerTextDecoder malformed/valid/empty/control/nonBMP recovery and four concurrent independent decoders");
    }
}
