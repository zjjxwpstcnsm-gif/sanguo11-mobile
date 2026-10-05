package game.sanguo.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native frame effects including callback order, UI stage bookkeeping and RNG. */
public final class PcDebateHeadedTest {
    private static List<String> lines(String name)throws IOException{
        try(InputStream in=PcDebateHeadedTest.class.getResourceAsStream("/pc-debate/"+name)){if(in==null)throw new IOException("Native fixture missing");return new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList();}
    }
    private static void require(boolean value,String detail){if(!value)throw new AssertionError(detail);}
    public static void main(String[]args)throws Exception{
        int caseId=-1,frame=-1,checks=0;PcDebateModel model=null;
        for(String line:lines("headed-original.tsv")){
            int[] n=Arrays.stream(line.split("\t")).mapToInt(x->(int)Long.parseLong(x)).toArray();
            if(caseId!=n[0]){caseId=n[0];frame=-1;model=new PcDebateModel(new PcDebateState(90,82,caseId/4,caseId%4,31,31,23),0);model.state.ui=new PcDebateUiEffects();model.state.ui.terminalPreferences[0]=n[n.length-2];model.state.ui.terminalPreferences[1]=n[n.length-1];}
            while(frame<n[1]){model.frame();frame++;}
            int[] actual=PcDebateModelTest.values(model);
            for(int i=0;i<actual.length;i++)require(actual[i]==n[6+i],"Native headed field case="+caseId+" frame="+frame+" field="+i+" expected="+n[6+i]+" actual="+actual[i]);
            require(model.state.random.state==n[2]&&model.state.random.draws==n[3],"Native headed RNG case="+caseId+" frame="+frame+" expected="+Integer.toUnsignedLong(n[2])+"/"+n[3]+" actual="+Integer.toUnsignedLong(model.state.random.state)+"/"+model.state.random.draws);
            require(model.state.ui.stages[0]==n[4]&&model.state.ui.stages[1]==n[5],"Native UI stages case="+caseId+" frame="+frame);
            require(model.state.ui.events.size()==n[101],"Native callback count case="+caseId+" frame="+frame+" expected="+n[101]+" actual="+model.state.ui.events.size());
            for(int i=0;i<n[101];i++)require(model.state.ui.events.get(i).nativeFunction==n[102+i],"Native callback order case="+caseId+" frame="+frame);
            byte[] saved=PcDebateModelSave.write(model);PcDebateModel restored=PcDebateModelSave.read(saved);
            require(restored.state.ui!=null&&Arrays.equals(saved,PcDebateModelSave.write(restored)),"Headed save lost policy/events");
            restored.frame();model.frame();frame++;require(Arrays.equals(PcDebateModelSave.write(model),PcDebateModelSave.write(restored)),"Headed native-frame live-control/restore differs");checks++;
        }
        int v1=0;
        for(String line:lines("headed-v1-goldens.tsv")){
            String[] f=line.split("\t");int id=Integer.parseInt(f[0]);byte[] mid=HexFormat.of().parseHex(f[1]),end=HexFormat.of().parseHex(f[2]);
            PcDebateModel old=PcDebateModelSave.read(mid);require(old.state.ui==null&&Arrays.equals(mid,PcDebateModelSave.write(old)),"Old model version1 upgraded");
            PcDebateModel fresh=new PcDebateModel(new PcDebateState(90,82,id/4,id%4,31,31,23),0);for(int i=0;i<120;i++)fresh.frame();require(Arrays.equals(mid,PcDebateModelSave.write(fresh)),"Version1 changed from exact prior batch bytes");
            for(int i=0;i<80;i++){old.frame();fresh.frame();}require(Arrays.equals(end,PcDebateModelSave.write(old))&&Arrays.equals(end,PcDebateModelSave.write(fresh)),"Version1 continuation changed");v1++;
        }
        PcDebateModel sample=new PcDebateModel(new PcDebateState(90,82,0,1,31,31,23),0);sample.state.ui=new PcDebateUiEffects();sample.state.ui.terminalPreferences[1]=0;for(int i=0;i<140;i++)sample.frame();byte[] valid=PcDebateModelSave.write(sample);int corrupt=0;
        for(int i=0;i<valid.length;i++){byte[] bad=valid.clone();bad[i]^=1;try{PcDebateModelSave.read(bad);throw new AssertionError("Headed corrupt model accepted "+i);}catch(IOException expected){corrupt++;}}
        for(int length:new int[]{0,1,8,63,valid.length-1})try{PcDebateModelSave.read(Arrays.copyOf(valid,length));throw new AssertionError("Headed truncated model accepted");}catch(IOException expected){corrupt++;}
        System.out.println("PASS PcDebateHeadedTest "+checks+" original bounded frame fields/callback order/UI stages/full RNG/live-control saves, "+v1+" prior batch model-v1 byte-exact controls, "+corrupt+" v2 corruption/truncation rejects; GUI/campaign/production pending");
    }
}
