package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Expected choices read from original queue writes, not computed by the port. */
public final class PcDebateUiRandomTest {
    private static void require(boolean pass,String detail){if(!pass)throw new AssertionError(detail);}
    public static void main(String[]args)throws Exception{
        List<String> lines;
        try(InputStream in=PcDebateUiRandomTest.class.getResourceAsStream("/pc-debate/ui-callback-original.tsv")){
            if(in==null)throw new IOException("Original callback fixture missing");lines=new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList();
        }
        int choices=0;
        for(String line:lines){
            String[] f=line.split("\t");int[] n=new int[f.length-1];for(int i=1;i<f.length;i++)n[i-1]=(int)Long.parseLong(f[i]);
            PcDebateState state=new PcDebateState(90,82,n[0],n[1],31,31,n[4],90,82);
            state.random.state=n[4];state.random.draws=0;
            state.left.fury=state.right.fury=n[2];state.left.health=n[10];state.right.health=n[11];
            PcDebateModel model=new PcDebateModel(state,0);byte[] before=PcDebateModelSave.write(model);
            List<PcDebateUiRandom.Choice> actual=PcDebateUiRandom.choices(state,PcDebateUiRandom.Callback.valueOf(f[0]),n[3],n[5],n[6]!=0,n[7],new int[]{n[8],n[9]});
            require(state.random.draws==n[12]&&state.random.state==n[13],"Original callback RNG differs "+line);
            require(actual.size()==n[14],"Original callback action count differs "+line);
            for(int i=0;i<actual.size();i++)require(actual.get(i).side==n[15+i*2]&&actual.get(i).action==n[16+i*2],"Original queued action differs "+line);
            state.random.state=n[4];state.random.draws=0;
            require(Arrays.equals(before,PcDebateModelSave.write(model)),"Queue RNG protocol changed other model fields");choices+=actual.size();
        }
        PcDebateState state=new PcDebateState(90,82,0,1,31,31,23,90,82);
        byte[] before=PcDebateModelSave.write(new PcDebateModel(state,0));int rejected=0;
        for(int side:new int[]{-1,2})try{PcDebateUiRandom.choices(state,PcDebateUiRandom.Callback.ORDINARY,side,1,false,-1,null);throw new AssertionError("Invalid side accepted");}catch(IllegalArgumentException expected){rejected++;}
        for(int card:new int[]{0,10})try{PcDebateUiRandom.choices(state,PcDebateUiRandom.Callback.ORDINARY,0,card,false,-1,null);throw new AssertionError("Invalid ordinary card accepted");}catch(IllegalArgumentException expected){rejected++;}
        for(int counter:new int[]{0,12,15})try{PcDebateUiRandom.choices(state,PcDebateUiRandom.Callback.COUNTER,0,1,false,counter,null);throw new AssertionError("Invalid counter accepted");}catch(IllegalArgumentException expected){rejected++;}
        require(Arrays.equals(before,PcDebateModelSave.write(new PcDebateModel(state,0))),"Rejected callback consumed RNG or changed state");
        System.out.println("PASS PcDebateUiRandomTest "+lines.size()+" original callbacks, "+choices+" actual original queued choices and full RNG, "+rejected+" invalid input rejects; production and headed scheduling pending");
    }
}
