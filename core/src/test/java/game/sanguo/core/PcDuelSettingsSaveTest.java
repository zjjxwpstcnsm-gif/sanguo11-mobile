package game.sanguo.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Separate original ageROOT38/deathROOT24/difficultyROOT20. Old policies
 * continue their saved conflated behavior; no silent version upgrade. */
public final class PcDuelSettingsSaveTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static byte[]bytes(PcDuelCampaign c)throws IOException{var b=new ByteArrayOutputStream();c.write(new DataOutputStream(b));return b.toByteArray();}
    public static void main(String[]args)throws Exception {
        String model=null,manager=null;int seed=0;int[]natives=null;for(String line:Files.readAllLines(Path.of(args[0]))){String[]p=line.split("\t");if(p[0].equals("MODEL"))model=p[1];if(p[0].equals("MANAGER"))manager=p[1];if(p[0].equals("SEED"))seed=(int)Long.parseLong(p[1]);if(p[0].equals("NATIVES"))natives=Arrays.stream(p[1].split(",")).mapToInt(Integer::parseInt).toArray();}
        var state=PcDuelModelSaveTest.normalized(PcDuelKernel.hex(model),PcDuelKernel.hex(manager),seed,0,natives,natives);byte[]numeric=PcDuelModelSave.write(state);
        for(int life=0;life<4;life++)for(int death=0;death<3;death++)for(int difficulty=0;difficulty<3;difficulty++)for(int fate:new int[]{0,2,3}){
            var b=new ByteArrayOutputStream();var out=new DataOutputStream(b);out.writeInt(4);out.writeBoolean(true);out.writeInt(life);out.writeBoolean(true);out.writeInt(difficulty);out.writeBoolean(true);out.writeInt(death);out.writeInt(611);out.writeInt(0);out.writeInt(numeric.length);out.write(numeric);out.writeInt(fate);if(fate!=0){var row=new PcDuelDisposition.Row(natives[3],natives[3],15,fate==3?0:4);row.recruitmentAdmitted=fate==3;new PcDuelDisposition(List.of(row)).write(out,fate==3);}byte[]saved=b.toByteArray();var reopened=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(saved)));check(Arrays.equals(saved,bytes(reopened)),"format4 explicit age/death/difficulty/fate roundtrip");check(reopened.settings.rawLifeOption==life&&reopened.settings.rawDeathOption==death&&reopened.settings.rawDifficulty==difficulty&&reopened.settings.separateDeath,"independent saved settings");check(reopened.settings.deathValid&&reopened.settings.lifeValid,"explicit original setting validity");
        }
        for(int life=-1;life<=3;life++){var b=new ByteArrayOutputStream();var out=new DataOutputStream(b);out.writeInt(1);out.writeBoolean(life>=0);out.writeInt(life);out.writeBoolean(false);out.writeInt(-1);out.writeInt(611);out.writeInt(0);out.writeInt(numeric.length);out.write(numeric);byte[]old=b.toByteArray();var c=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(old)));check(Arrays.equals(old,bytes(c)),"historical format1 bytes preserved");check(!c.settings.separateDeath&&c.settings.rawDeathOption==life&&c.settings.deathValid==c.settings.lifeValid,"historical saved rule not silently corrected");}
        System.out.println("PASS separate age/death/difficulty and historical policy "+checks+" checks; fresh actual source menu/normal duel creator remains required");
    }
}
