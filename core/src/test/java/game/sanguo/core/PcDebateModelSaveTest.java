package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Complete native-trace model persistence and bounded original human branch. */
public final class PcDebateModelSaveTest {
    private static List<String> lines(String name)throws IOException{
        try(InputStream in=PcDebateModelSaveTest.class.getResourceAsStream("/pc-debate/"+name)){if(in==null)throw new IOException("Original fixture missing "+name);return new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().toList();}
    }
    private static int[] numbers(String line){return Arrays.stream(line.split("\t")).mapToInt(v->(int)Long.parseLong(v)).toArray();}
    private static void require(boolean pass,String detail){if(!pass)throw new AssertionError(detail);}
    public static void main(String[]args)throws Exception{
        PcDebateModel model=null;int id=-1,frame=-1,saves=0,humanCases=0;
        for(String line:lines("original-model.tsv")){
            int[] n=numbers(line);if(n[0]!=id){id=n[0];frame=-1;model=new PcDebateModel(new PcDebateState(90,82,id/4,id%4,31,31,23),0);}
            while(frame<n[1]){model.frame();frame++;}
            byte[] bytes=PcDebateModelSave.write(model);PcDebateModel restored=PcDebateModelSave.read(bytes);
            require(Arrays.equals(bytes,PcDebateModelSave.write(restored)),"Original model restoration differs "+id+":"+frame);
            restored.frame();model.frame();frame++;
            require(Arrays.equals(PcDebateModelSave.write(restored),PcDebateModelSave.write(model)),"Original live-control/restore continuation differs "+id+":"+frame);saves++;
        }
        for(String line:lines("original-human-input.tsv")){
            int[] n=numbers(line);int side=n[1];PcDebateState state=new PcDebateState(90,82,n[0]/4,n[0]%4,31,31,23);model=new PcDebateModel(state,side);model.phase=model.previousPhase=4;model.human[side]=true;state.random.state=23;state.random.draws=0;
            byte[] before=PcDebateModelSave.write(model);boolean accepted=model.selectHuman(side,n[2]);
            require(accepted==(n[3]!=0),"Original input acceptance differs "+line);model.frame();
            require(model.sub==n[4]&&model.selected[side]==n[5]&&Arrays.equals(state.speaker(side).hand,Arrays.copyOfRange(n,6,13))&&state.random.state==n[13],"Original input branch state/RNG differs "+line);
            if(!accepted)require(Arrays.equals(before,PcDebateModelSave.write(model)),"Rejected input changed model");
            else{byte[] after=PcDebateModelSave.write(model);require(!model.selectHuman(side,n[2])&&Arrays.equals(after,PcDebateModelSave.write(model)),"Repeated human input changed state/RNG");}
            PcDebateModel restored=PcDebateModelSave.read(PcDebateModelSave.write(model));model.frame();restored.frame();require(Arrays.equals(PcDebateModelSave.write(model),PcDebateModelSave.write(restored)),"Human mid-state save continuation differs");humanCases++;
        }
        byte[] valid=PcDebateModelSave.write(new PcDebateModel(new PcDebateState(90,82,0,1,31,31,23),0));int rejected=0;
        for(int i=0;i<valid.length;i++){byte[] damaged=valid.clone();damaged[i]^=1;try{PcDebateModelSave.read(damaged);throw new AssertionError("Corrupt model accepted at "+i);}catch(IOException expected){rejected++;}}
        for(int length:new int[]{0,1,8,63,valid.length-1})try{PcDebateModelSave.read(Arrays.copyOf(valid,length));throw new AssertionError("Truncated model accepted");}catch(IOException expected){rejected++;}
        System.out.println("PASS PcDebateModelSaveTest "+saves+" complete native-frame save/live-control continuations, "+humanCases+" original bounded human input/duplicate/restore cases, "+rejected+" corruption/truncation rejects; World save and actual UI integration pending");
    }
}
