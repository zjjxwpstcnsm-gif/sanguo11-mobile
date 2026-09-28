package game.sanguo.mobile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class VerifiedMaterialTest {
    public static void main(String[] args)throws Exception {
        int checks=0;
        try(var paths=Files.walk(Path.of("app/src/main/assets"))){
            for(Path p:(Iterable<Path>)paths.filter(x->x.toString().endsWith(".filamat"))::iterator){
                String name=Path.of("app/src/main/assets").relativize(p).toString();
                byte[] original=Files.readAllBytes(p);
                if(!Arrays.equals(original,VerifiedMaterial.read(name,new ByteArrayInputStream(original))))throw new AssertionError(name);
                byte[][] bad={new byte[0],Arrays.copyOf(original,original.length-1),original.clone(),Arrays.copyOf(original,original.length+1),new byte[4*1024*1024+1]};
                bad[2][original.length/2]^=1;
                for(byte[] bytes:bad){try{VerifiedMaterial.read(name,new ByteArrayInputStream(bytes));throw new AssertionError("accepted corrupt "+name);}catch(IOException expected){if(!expected.getMessage().contains(name))throw new AssertionError("missing resource identity");}checks++;}
                checks++;
            }
        }
        try{VerifiedMaterial.read("../unknown",new ByteArrayInputStream(new byte[1]));throw new AssertionError();}catch(IOException expected){checks++;}
        System.out.println("PASS VerifiedMaterial "+checks+" production/corrupt/truncated/oversized cases");
    }
}
