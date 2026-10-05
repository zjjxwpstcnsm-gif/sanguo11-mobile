package game.sanguo.core;

import java.nio.file.*;
import java.util.*;

/** Emit and continue in separate JVMs. This is not Android/GameSession proof. */
public final class PcDebateModelColdTest {
    public static void main(String[]args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("emit|continue isolated-directory");Path folder=Path.of(args[1]);
        if(args[0].equals("emit")){
            if(Files.exists(folder))throw new IllegalArgumentException("Preserve existing fixtures");Files.createDirectories(folder);
            for(int i=0;i<16;i++){PcDebateModel model=new PcDebateModel(new PcDebateState(90,82,i/4,i%4,31,31,23),0);for(int frame=0;frame<120;frame++)model.frame();Files.write(folder.resolve("case"+i+"-mid.bin"),PcDebateModelSave.write(model));for(int frame=0;frame<80;frame++)model.frame();Files.write(folder.resolve("case"+i+"-expected.bin"),PcDebateModelSave.write(model));}
            System.out.println("EMIT 16 actual staged model mid-states and live-control continuations; exit this JVM before continue");
        }else if(args[0].equals("continue")){
            for(int i=0;i<16;i++){byte[] bytes=Files.readAllBytes(folder.resolve("case"+i+"-mid.bin"));PcDebateModel model=PcDebateModelSave.read(bytes);if(!Arrays.equals(bytes,PcDebateModelSave.write(model)))throw new AssertionError("Cold load differs "+i);for(int frame=0;frame<80;frame++)model.frame();if(!Arrays.equals(Files.readAllBytes(folder.resolve("case"+i+"-expected.bin")),PcDebateModelSave.write(model)))throw new AssertionError("Cold continuation differs "+i);}
            System.out.println("PASS PcDebateModelColdTest 16 independent JVM load/80-frame continuations byte-equal to actual live controls; Android/World integration pending");
        }else throw new IllegalArgumentException("Unknown model cold-test mode");
    }
}
