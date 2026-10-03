package game.sanguo.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;

/** Same probe runs against frozen and candidate APK production classes in ART. */
public final class SaveCopyProbe {
    private static int checks;
    private static void check(boolean valid,String reason){checks++;if(!valid)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception {
        byte[] original=Files.readAllBytes(Paths.get(args[0]));
        StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(original))hash.append(String.format("%02x",b&255));
        check(hash.toString().equals("02ddb3d44d98fbebe763a82551b5cb5eb68da6e70b6087568c0a73943bde5d69"),"normal UI opening fixture provenance");
        long start=System.nanoTime();World world=SaveCodec.decode(original);
        long cold=System.nanoTime()-start;
        check(world.officers.size()==656&&world.startYear==190,"normal campaign scope");
        check(Arrays.equals(original,SaveCodec.encode(world)),"cold decode preserves complete save and RNG");
        start=System.nanoTime();
        for(int i=0;i<10;i++){
            world=SaveCodec.decode(SaveCodec.encode(world));
            check(Arrays.equals(original,SaveCodec.encode(world)),"repeated transaction copies preserve every saved byte");
        }
        long copies=System.nanoTime()-start;
        start=System.nanoTime();for(int i=0;i<20;i++)RulesSave.validate(world);
        long rules=System.nanoTime()-start;
        World.Officer officer=world.officers.get(0);String savedSkill=officer.skillId;
        for(String id:new String[]{"none","skill-001","z.x_9-",repeat('a',80)}){
            officer.skillId=id;byte[] bytes=SaveCodec.encode(world);
            check(SaveCodec.decode(bytes).officer(officer.id).skillId.equals(id),"valid unknown skill survives save");
        }
        for(String id:new String[]{null,"",".","A","a\n","é","ａ","\ud800",repeat('a',81)}){
            officer.skillId=id;boolean rejected=false;
            try{SaveCodec.encode(world);}catch(IOException expected){rejected=expected.getMessage().equals("特技标识或性别无效");}
            check(rejected,"malformed skill is rejected with original error");
        }
        officer.skillId=savedSkill;
        check(Arrays.equals(original,SaveCodec.encode(world)),"validation attempts preserve other state/RNG");
        System.out.println("PASS SaveCopyProbe checks="+checks+" officers=656 coldDecodeNs="+cold+" tenCopiesNs="+copies+" twentyRulesNs="+rules);
    }
    private static String repeat(char value,int count){char[] chars=new char[count];Arrays.fill(chars,value);return new String(chars);}
}
