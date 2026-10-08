package game.sanguo.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Original resource bytes/bounds, fragmented providers, no overread or close. */
public final class PcResourceBytesTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception{
        for(var resource:Map.of("source-header-flags.tsv",16385,"newgame-options.tsv",4097).entrySet()){
            byte[] actual=Files.readAllBytes(Path.of("core/src/main/resources/pc-duel/"+resource.getKey()));
            for(int fragment:new int[]{1,7,8192}){
                var bytes=new ByteArrayInputStream(actual);
                InputStream provider=new FilterInputStream(bytes){boolean zero=true;
                    @Override public int read(byte[] b,int off,int len)throws IOException{if(zero){zero=false;return 0;}return super.read(b,off,Math.min(len,fragment));}
                };
                check(Arrays.equals(actual,PcResourceBytes.readUpTo(provider,resource.getValue())),"original resource byte exact "+resource.getKey()+" fragment "+fragment);
                check(bytes.available()==0,"original resource consumed through EOF");
            }
        }
        for(int length:new int[]{0,1,4096,4097,16384,16385,20000}){
            byte[] raw=new byte[length];for(int k=0;k<length;k++)raw[k]=(byte)(k*31);
            for(int count:new int[]{0,1,4097,16385}){
                var stream=new ByteArrayInputStream(raw);int size=Math.min(length,count);
                check(Arrays.equals(Arrays.copyOf(raw,size),PcResourceBytes.readUpTo(stream,count)),"exact limit or EOF "+length+"/"+count);
                check(stream.available()==length-size,"tail not consumed beyond cap "+length+"/"+count);
            }
        }
        IOException expected=new IOException("provider failure");boolean propagated=false;
        try{PcResourceBytes.readUpTo(new InputStream(){public int read()throws IOException{throw expected;}},1);}catch(IOException e){propagated=e==expected;}
        check(propagated,"same provider IO error propagated");
        boolean negative=false;try{PcResourceBytes.readUpTo(new ByteArrayInputStream(new byte[0]),-1);}catch(IllegalArgumentException e){negative=true;}check(negative,"negative count rejected");
        final boolean[] closed={false};var owned=new ByteArrayInputStream(new byte[]{4,5}){@Override public void close(){closed[0]=true;}};
        check(Arrays.equals(new byte[]{4},PcResourceBytes.readUpTo(owned,1))&&!closed[0]&&owned.read()==5,"caller retains stream ownership and next byte");
        System.out.println("PASS original resource bytes/caps/fragmented stream/no overread/error/ownership "+checks+" checks; installed API29 still required");
    }
}
