package game.sanguo.core;

import java.io.*;
import java.security.*;
import java.util.*;

/** Complete numerical duel storage. Does not enable or migrate a World policy.
 * All references must already be stable officer/unit IDs and a model self tag;
 * native addresses and renderer objects are never accepted as saved references. */
final class PcDuelModelSave {
    private static final int MAGIC=0x5044554d,VERSION=1,MAX_BYTES=4096;
    static final class State {
        final PcDuelKernel model;
        final byte[] manager;
        final PcDuelKernel.Random random;
        final int[] officers,natives;
        State(PcDuelKernel model,byte[] manager,PcDuelKernel.Random random,int[] officers,int[] natives){
            this.model=model;this.manager=manager.clone();this.random=random;
            this.officers=officers.clone();this.natives=natives.clone();
        }
    }
    private PcDuelModelSave(){}
    private static byte[] hash(byte[] bytes)throws IOException{
        try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException e){throw new IOException(e);}
    }
    static byte[] write(State state)throws IOException{
        validate(state);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(MAGIC);out.writeInt(VERSION);out.writeUTF(PcScenarioIdentity.EXE_SHA);
        for(int i=0;i<6;i++){out.writeInt(state.officers[i]);out.writeInt(state.natives[i]);}
        out.writeInt(state.random.state);out.writeInt(state.random.draws);
        out.writeInt(state.model.state.length);out.write(state.model.state);out.writeInt(state.manager.length);out.write(state.manager);
        byte[] body=bytes.toByteArray();out.write(hash(body));return bytes.toByteArray();
    }
    static State read(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length<128||bytes.length>MAX_BYTES)throw new IOException("原单挑模型长度无效");
        byte[] body=Arrays.copyOf(bytes,bytes.length-32),digest=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length);
        if(!MessageDigest.isEqual(hash(body),digest))throw new IOException("原单挑模型校验失败");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(body));
        if(in.readInt()!=MAGIC||in.readInt()!=VERSION||!in.readUTF().equals(PcScenarioIdentity.EXE_SHA))throw new IOException("原单挑模型来源或策略未知");
        int[] officers=new int[6],natives=new int[6];for(int i=0;i<6;i++){officers[i]=in.readInt();natives[i]=in.readInt();}
        PcDuelKernel.Random random=new PcDuelKernel.Random(in.readInt());random.draws=in.readInt();
        if(in.readInt()!=0x59c)throw new IOException("原单挑规则模型段无效");byte[] model=new byte[0x59c];in.readFully(model);
        if(in.readInt()!=0xd0)throw new IOException("原单挑管理器段无效");byte[] manager=new byte[0xd0];in.readFully(manager);
        if(in.available()!=0)throw new IOException("原单挑模型尾部未知");
        State state=new State(new PcDuelKernel(model),manager,random,officers,natives);validate(state);return state;
    }
    private static void range(int value,int min,int max)throws IOException{if(value<min||value>max)throw new IOException("原单挑模型字段越界");}
    static void validate(State s)throws IOException{
        if(s==null||s.model==null||s.random==null||s.manager.length!=0xd0||s.officers.length!=6||s.natives.length!=6)throw new IOException("原单挑完整状态缺失");
        PcDuelKernel m=s.model;range(s.random.draws,0,10000000);if(m.get(0)!=1)throw new IOException("原单挑模型引用不是稳定标记");
        for(int at=0x220;at<=0x234;at+=4)if(m.get(at)!=0)throw new IOException("渲染对象不能写入原单挑保存");
        range(m.get(4),-1,12);range(m.get(8),-1,12);range(m.get(12),-1,8);range(m.get(0x10),0,1000);range(m.get(0x18),1,1000);range(m.get(0x1c),0,1);range(m.get(0x20),0,3);range(m.get(0x4c4),0,11);
        Set<Integer> identities=new HashSet<>(),nativeIds=new HashSet<>();
        for(int side=0;side<2;side++){
            int team=0x24+side*0xec,active=m.get(team+0xc4),count=m.get(team+0xc0),context=0x238+24*side;
            range(count,1,3);range(active,0,2);range(m.get(team+0xd8),0,1);range(m.get(team+0xc8),-1,7);
            if(m.get(context)!=1||m.get(context+4)!=1||m.get(context+8)!=side||m.get(context+12)!=1-side)throw new IOException("原单挑AI上下文引用无效");
            range(m.get(context+16),0,2);range(m.get(context+20),0,2);
            for(int slot=0;slot<3;slot++){
                int i=side*3+slot,at=m.fighter(side,slot),id=s.officers[i];range(id,-1,999999);range(s.natives[i],-1,1099);
                if(id<0){if(s.natives[i]!=-1||slot<count)throw new IOException("原单挑缺失人物槽位无效");continue;}
                if(slot>=count||!identities.add(id)||!nativeIds.add(s.natives[i])||s.natives[i]<0||m.get(at)!=id||PcDuelKernel.readManager(s.manager,12*side+4*slot)!=id)throw new IOException("原单挑人物引用不一致");
                range(PcDuelKernel.readManager(s.manager,0x64+12*side+4*slot),0,2);
                range(PcDuelKernel.readManager(s.manager,0x7c+12*side+4*slot),0,100);
                range(PcDuelKernel.readManager(s.manager,0xac+12*side+4*slot),0,3);
                range(m.get(at+4),0,100);range(m.get(at+8),0,300);range(m.get(at+12),0,3);range(m.get(at+16),0,3);range(m.get(at+20),0,2);range(m.get(at+28),0,255);
                for(int move=0;move<8;move++)range(m.get(at+32+4*move),-1,1);
            }
            if(active>=count)throw new IOException("原单挑当前人物缺失");
            range(PcDuelKernel.readManager(s.manager,0x18+4*side),0,999999);
        }
        range(m.get(0x588),-1,1);range(m.get(0x58c),-1,1);range(m.get(0x590),-1,4);
    }
}
