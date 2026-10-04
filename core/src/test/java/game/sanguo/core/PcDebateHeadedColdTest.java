package game.sanguo.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Two actual JVMs; complete explicit-effects policy and event storage. */
public final class PcDebateHeadedColdTest {
    public static void main(String[]args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("emit|continue isolated-directory");Path folder=Path.of(args[1]);
        if(args[0].equals("emit")){
            if(Files.exists(folder))throw new IllegalArgumentException("Preserve existing fixtures");Files.createDirectories(folder);Map<Integer,int[]> preferences=new HashMap<>();
            try(InputStream in=PcDebateHeadedColdTest.class.getResourceAsStream("/pc-debate/headed-original.tsv")){
                for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList()){
                    String[] f=line.split("\t");preferences.putIfAbsent(Integer.parseInt(f[0]),new int[]{Integer.parseInt(f[f.length-2]),Integer.parseInt(f[f.length-1])});
                }
            }
            for(int i=0;i<16;i++){
                PcDebateModel model=new PcDebateModel(new PcDebateState(90,82,i/4,i%4,31,31,23),0);model.state.ui=new PcDebateUiEffects();System.arraycopy(preferences.get(i),0,model.state.ui.terminalPreferences,0,2);
                for(int f=0;f<120;f++)model.frame();Files.write(folder.resolve("case"+i+"-mid.bin"),PcDebateModelSave.write(model));
                for(int f=0;f<80;f++)model.frame();Files.write(folder.resolve("case"+i+"-expected.bin"),PcDebateModelSave.write(model));
            }
            System.out.println("EMIT 16 headed-effects mid-states and actual live-control continuations; exit this JVM before loading");
        }else if(args[0].equals("continue")){
            for(int i=0;i<16;i++){
                byte[] mid=Files.readAllBytes(folder.resolve("case"+i+"-mid.bin"));PcDebateModel model=PcDebateModelSave.read(mid);
                if(model.state.ui==null||!Arrays.equals(mid,PcDebateModelSave.write(model)))throw new AssertionError("Headed cold policy/events differed "+i);
                for(int f=0;f<80;f++)model.frame();if(!Arrays.equals(Files.readAllBytes(folder.resolve("case"+i+"-expected.bin")),PcDebateModelSave.write(model)))throw new AssertionError("Headed cold continuation differs "+i);
            }
            System.out.println("PASS PcDebateHeadedColdTest 16 independent JVM load/80-frame byte-exact live-control continuations with UI numeric policy/relationships/events; World/Android pending");
        }else throw new IllegalArgumentException("Unknown cold mode");
    }
}
