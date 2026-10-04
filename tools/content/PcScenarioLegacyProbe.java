package game.sanguo.core;
import java.nio.file.*;
import java.nio.*;
import java.security.*;
import java.util.*;

/** Same executable against separate parent/current core; not an APK workflow. */
public final class PcScenarioLegacyProbe {
    private static String sha(byte[] bytes)throws Exception{
        StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();
    }
    private static void run(String name,byte[] bytes)throws Exception{
        int version=ByteBuffer.wrap(bytes).getInt(4);if(version<31||version>37)return;
        World w=SaveCodec.decode(bytes);String initial=sha(SaveCodec.encode(w));String patrol="no-eligible-command";
        for(World.City city:w.cities)if(city.owner==w.player&&city.order<100&&!w.idle(city).isEmpty()){
            World.Officer actor=w.idle(city).get(0);patrol=Boolean.toString(w.patrol(city.id,actor.id).ok);break;
        }
        String command=sha(SaveCodec.encode(w));List<String> turns=new ArrayList<>();
        for(int i=0;i<3;i++){boolean ok=w.nextTurn().ok;byte[] saved=SaveCodec.encode(w);w=SaveCodec.decode(saved);turns.add(ok+":"+sha(saved));}
        System.out.println(name+"\t"+version+"\t"+initial+"\t"+patrol+"\t"+command+"\t"+String.join(",",turns));
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);List<Path> files=new ArrayList<>();try(var paths=Files.walk(root)){paths.filter(p->p.toString().endsWith(".sg11")).sorted().forEach(files::add);}
        for(Path file:files){byte[] bytes=Files.readAllBytes(file);if(bytes.length<20||ByteBuffer.wrap(bytes).getInt()!=0x53473131)continue;run(root.relativize(file).toString(),bytes);}
        for(String id:List.of("coalition-190","heroes-250","central-mobile-sandbox"))run("fresh-legacy-"+id,SaveCodec.encode(ScenarioCatalog.load(id,0,23)));
    }
}
