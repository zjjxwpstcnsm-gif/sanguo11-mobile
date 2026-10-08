package game.sanguo.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class PcDuelActionSaveTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static byte[]bytes(PcDuelCampaign c)throws IOException{var b=new ByteArrayOutputStream();c.write(new DataOutputStream(b));return b.toByteArray();}
    public static void main(String[]args)throws Exception {
        String model=null,manager=null;int seed=0;int[]natives=null;for(String line:Files.readAllLines(Path.of(args[0]))){String[]p=line.split("\t");if(p[0].equals("MODEL"))model=p[1];if(p[0].equals("MANAGER"))manager=p[1];if(p[0].equals("SEED"))seed=(int)Long.parseLong(p[1]);if(p[0].equals("NATIVES"))natives=Arrays.stream(p[1].split(",")).mapToInt(Integer::parseInt).toArray();}
        var state=PcDuelModelSaveTest.normalized(PcDuelKernel.hex(model),PcDuelKernel.hex(manager),seed,0,natives,natives);byte[]numeric=PcDuelModelSave.write(state);
        for(boolean acted:new boolean[]{false,true})for(int voice:new int[]{-1,35,36}){
            var b=new ByteArrayOutputStream();var d=new DataOutputStream(b);d.writeInt(5);d.writeBoolean(true);d.writeInt(0);d.writeBoolean(true);d.writeInt(0);d.writeBoolean(true);d.writeInt(0);d.writeBoolean(acted);d.writeInt(voice);d.writeInt(611);d.writeInt(0);d.writeInt(numeric.length);d.write(numeric);d.writeInt(0);byte[]saved=b.toByteArray();var c=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(saved)));check(c.commandBoundary&&c.opponentActed==acted&&c.openingVoice==voice,"explicit command facts restored");check(Arrays.equals(saved,bytes(c)),"full model/manager/RNG/settings/command bytes exact");
            var invalid=saved.clone();invalid[19]=2;boolean refused=false;try{PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(invalid)));}catch(IOException e){refused=true;}check(refused,"invalid saved opponent action bit refused");
        }
        PcDuelSettingsSaveTest.main(args);
        System.out.println("PASS original action-boundary save "+checks+" checks; normal source creator/API/APK pending");
    }
}
