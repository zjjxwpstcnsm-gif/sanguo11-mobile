package game.sanguo.mobile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/** EOF/intro/partial-loop and invalid extents. Actual native30-track bytes are tested separately on Android. */
public final class PcMusicFrameStreamTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void rejects(Throwing operation,String label)throws Exception{try{operation.run();throw new AssertionError(label);}catch(IOException expected){checks++;}}
    interface Throwing {void run()throws Exception;}
    public static void main(String[] args)throws Exception{
        File file=Files.createTempFile("pc-frame-stream-",".pcm").toFile();byte[] source=new byte[160];for(int i=0;i<source.length;i++)source[i]=(byte)i;Files.write(file.toPath(),source);
        try{
            byte[] buffer=new byte[16];
            try(var stream=new PcMusicFrameStream(file,40,2,10,40,true,36)){
                check(stream.read(buffer)==16&&Arrays.equals(buffer,Arrays.copyOfRange(source,144,160)),"original ending unchanged");
                check(stream.read(buffer)==16&&Arrays.equals(buffer,Arrays.copyOfRange(source,40,56)),"partial loop excludes original intro");
                check(stream.loops()==1&&stream.position()==14,"exact loop counters");
            }
            try(var stream=new PcMusicFrameStream(file,40,2,-1,-1,true,36)){
                stream.read(buffer);stream.read(buffer);check(Arrays.equals(buffer,Arrays.copyOfRange(source,0,16)),"explicit repeat without source loop uses full track");
            }
            try(var stream=new PcMusicFrameStream(file,40,2,10,40,false,36)){
                check(stream.read(buffer)==16&&stream.read(buffer)==-1&&stream.read(buffer)==-1&&stream.loops()==0,"one-shot source directive never loops");
            }
            try(var stream=new PcMusicFrameStream(file,40,2,10,40,true,0)){
                byte[] original=new byte[160];check(stream.read(original)==160&&Arrays.equals(source,original),"whole original intro remains intact");
                rejects(()->stream.read(new byte[3]),"incomplete frame rejected");
            }
            rejects(()->new PcMusicFrameStream(file,41,2,-1,-1,true,0),"extent mismatch rejected");
            rejects(()->new PcMusicFrameStream(file,40,2,40,40,true,0),"empty partial loop rejected");
            rejects(()->new PcMusicFrameStream(file,40,2,10,39,true,0),"unproved loop end rejected");
            rejects(()->new PcMusicFrameStream(file,40,2,-1,-1,true,40),"outside initial frame rejected");
        }finally{check(file.delete(),"own fixture released");}
        System.out.println("PC music frame stream PASS checks="+checks+"; EOF/intro/partial loops; not audio playback evidence");
    }
}
