package game.sanguo.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Real terminal model corpus, explicit pending decisions and no old-format adoption. */
public final class PcDuelDispositionTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static byte[]bytes(PcDuelCampaign c)throws IOException{var b=new ByteArrayOutputStream();c.write(new DataOutputStream(b));return b.toByteArray();}
    public static void main(String[]args)throws Exception {
        String model=null,manager=null;int seed=0;int[]natives=null;
        for(String line:Files.readAllLines(Path.of(args[0]))){String[]p=line.split("\t");if(p[0].equals("MODEL"))model=p[1];if(p[0].equals("MANAGER"))manager=p[1];if(p[0].equals("SEED"))seed=(int)Long.parseLong(p[1]);if(p[0].equals("NATIVES"))natives=Arrays.stream(p[1].split(",")).mapToInt(Integer::parseInt).toArray();}
        var state=PcDuelModelSaveTest.normalized(PcDuelKernel.hex(model),PcDuelKernel.hex(manager),seed,0,natives,natives);
        var b=new ByteArrayOutputStream();var out=new DataOutputStream(b);out.writeInt(1);out.writeBoolean(false);out.writeInt(-1);out.writeBoolean(false);out.writeInt(-1);out.writeInt(1000);out.writeInt(0);byte[]raw=PcDuelModelSave.write(state);out.writeInt(raw.length);out.write(raw);
        byte[]old=b.toByteArray();PcDuelCampaign c=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(old)));check(Arrays.equals(old,bytes(c))&&c.disposition==null,"actual prior campaign format1 remains exact and absent");
        check(PcDuelDisposition.initialMask(false,false)==15&&PcDuelDisposition.initialMask(false,true)==15&&PcDuelDisposition.initialMask(true,false)==15&&PcDuelDisposition.initialMask(true,true)==12,"original4b2380 initial masks");
        c.disposition=new PcDuelDisposition(List.of(new PcDuelDisposition.Row(natives[3],natives[3],15,4)));check(!c.disposition.ready(),"source pending4 has no automatic disposition");
        byte[]saved=bytes(c);var cold=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(saved)));check(Arrays.equals(saved,bytes(cold))&&!cold.disposition.ready(),"format2 pending model/fullmanager/RNG roundtrip");
        int rng=cold.state.random.state,draws=cold.state.random.draws;
        for(int action=0;action<4;action++){cold.disposition.select(natives[3],action);check(cold.disposition.ready()&&cold.disposition.rows.get(0).choice==action,"all original selections retained without effects");}
        cold.disposition.recruitmentFailed(natives[3]);check(cold.disposition.rows.get(0).mask==14&&cold.disposition.rows.get(0).choice==4,"original4b2714 removes recruit bit after failure");
        byte[]before=bytes(cold);boolean failed=false;try{cold.disposition.select(natives[3],0);}catch(IOException e){failed=true;}check(failed&&Arrays.equals(before,bytes(cold)),"rejected unavailable choice bytepure");
        check(rng==cold.state.random.state&&draws==cold.state.random.draws,"selection changes neither native RNG state nor draws");
        var ruler=new PcDuelDisposition(List.of(new PcDuelDisposition.Row(517,517,12,4)));check(ruler.error(517,0)!=null&&ruler.error(517,1)!=null&&ruler.error(517,2)==null&&ruler.error(517,3)==null,"ruler with original owned city excludes first two operations");
        var success=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(saved)));success.disposition.select(natives[3],0);success.disposition.rows.get(0).recruitmentAdmitted=true;byte[]admitted=bytes(success);check(java.nio.ByteBuffer.wrap(admitted).getInt()==3,"explicit format3 only for proven admission");var reopened=PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(admitted)));check(reopened.disposition.rows.get(0).recruitmentAdmitted&&Arrays.equals(admitted,bytes(reopened)),"format3 admission/full model/RNG exact cold save");byte[]corrupt=admitted.clone();corrupt[corrupt.length-1]=2;boolean rejected=false;try{PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(corrupt)));}catch(IOException e){rejected=true;}check(rejected,"unknown admission flag rejects");corrupt[corrupt.length-1]=0;rejected=false;try{PcDuelCampaign.read(new DataInputStream(new ByteArrayInputStream(corrupt)));}catch(IOException e){rejected=true;}check(rejected,"format3 cannot contain no successful admission");
        System.out.println("PASS saved native disposition "+checks+" checks; full callback effects and normal campaign/APK pending");
    }
}
