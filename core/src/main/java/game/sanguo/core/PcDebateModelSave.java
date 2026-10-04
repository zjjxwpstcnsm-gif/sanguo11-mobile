package game.sanguo.core;

import java.io.*;
import java.security.*;
import java.util.*;

/** Complete portable model storage. Not a World/legacy save migration. */
final class PcDebateModelSave {
    private static final int MAGIC=0x5043444d,VERSION=1,MAX_BYTES=4096;
    private PcDebateModelSave(){}
    private static byte[] hash(byte[] bytes)throws IOException{try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    static byte[] write(PcDebateModel m)throws IOException{
        validate(m);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);PcDebateState s=m.state;
        d.writeInt(MAGIC);d.writeInt(VERSION);d.writeUTF(PcScenarioIdentity.EXE_SHA);
        for(PcDebateState.Speaker p:new PcDebateState.Speaker[]{s.left,s.right}){d.writeInt(p.intelligence);d.writeInt(p.war);d.writeInt(p.temper);d.writeInt(p.talkMask);}
        for(int n:new int[]{m.phase,m.previousPhase,m.sub,m.mercy,m.outcome,m.terminalChoicePreference,m.selected[0],m.selected[1],s.topic,s.leader,s.selectedWinner,s.furySide,s.burstSide,s.burstIndex,s.terminalWinner,s.random.state,s.random.draws})d.writeInt(n);
        d.writeBoolean(m.human[0]);d.writeBoolean(m.human[1]);d.writeBoolean(m.externalTerminalChoiceSuppressed);d.writeBoolean(s.burstActive);
        for(int i=0;i<2;i++){d.writeBoolean(s.reconsiderAvailable[i]);d.writeInt(s.stage[i]);d.writeBoolean(s.stageChanged[i]);PcDebateState.Speaker p=s.speaker(i);d.writeInt(p.health);d.writeInt(p.anger);d.writeInt(p.fury);d.writeInt(p.deckCursor);for(int n:p.hand)d.writeInt(n);for(int n:p.deck)d.writeInt(n);for(int n:p.specialPool)d.writeInt(n);}
        byte[] body=bytes.toByteArray();d.write(hash(body));return bytes.toByteArray();
    }
    static PcDebateModel read(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length<64||bytes.length>MAX_BYTES)throw new IOException("原舌战模型长度无效");
        byte[] body=Arrays.copyOf(bytes,bytes.length-32),digest=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length);
        if(!MessageDigest.isEqual(hash(body),digest))throw new IOException("原舌战模型校验失败");
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(body));
        if(d.readInt()!=MAGIC||d.readInt()!=VERSION||!d.readUTF().equals(PcScenarioIdentity.EXE_SHA))throw new IOException("原舌战模型来源或策略未知");
        int[] inputs=new int[8];for(int i=0;i<inputs.length;i++)inputs[i]=d.readInt();PcDebateState s;
        try{s=new PcDebateState(inputs[0],inputs[4],inputs[2],inputs[6],inputs[3],inputs[7],0,inputs[1],inputs[5],false);}catch(IllegalArgumentException e){throw new IOException("原舌战人物参数无效",e);}
        PcDebateModel m=new PcDebateModel(s,0);m.phase=d.readInt();m.previousPhase=d.readInt();m.sub=d.readInt();m.mercy=d.readInt();m.outcome=d.readInt();m.terminalChoicePreference=d.readInt();m.selected[0]=d.readInt();m.selected[1]=d.readInt();s.topic=d.readInt();s.leader=d.readInt();s.selectedWinner=d.readInt();s.furySide=d.readInt();s.burstSide=d.readInt();s.burstIndex=d.readInt();s.terminalWinner=d.readInt();s.random.state=d.readInt();s.random.draws=d.readInt();
        m.human[0]=bool(d);m.human[1]=bool(d);m.externalTerminalChoiceSuppressed=bool(d);s.burstActive=bool(d);
        for(int i=0;i<2;i++){s.reconsiderAvailable[i]=bool(d);s.stage[i]=d.readInt();s.stageChanged[i]=bool(d);PcDebateState.Speaker p=s.speaker(i);p.health=d.readInt();p.anger=d.readInt();p.fury=d.readInt();p.deckCursor=d.readInt();for(int j=0;j<p.hand.length;j++)p.hand[j]=d.readInt();for(int j=0;j<p.deck.length;j++)p.deck[j]=d.readInt();for(int j=0;j<p.specialPool.length;j++)p.specialPool[j]=d.readInt();}
        if(d.available()!=0)throw new IOException("原舌战模型尾部未知");validate(m);return m;
    }
    private static void range(int n,int min,int max)throws IOException{if(n<min||n>max)throw new IOException("原舌战模型字段越界");}
    private static boolean bool(DataInputStream d)throws IOException{int n=d.readUnsignedByte();if(n>1)throw new IOException("原舌战模型布尔值无效");return n==1;}
    private static void validate(PcDebateModel m)throws IOException{
        if(m==null||m.state==null)throw new IOException("原舌战模型缺失");PcDebateState s=m.state;
        range(m.phase,0,9);range(m.previousPhase,-1,9);range(m.sub,0,9);range(m.mercy,-1,1);range(m.outcome,0,3);range(m.terminalChoicePreference,-1,1);range(s.topic,0,2);range(s.leader,0,1);range(s.selectedWinner,-1,1);range(s.furySide,-1,1);range(s.burstSide,0,1);range(s.burstIndex,0,6);range(s.terminalWinner,-1,1);range(s.random.draws,0,10000000);
        for(int side=0;side<2;side++){
            range(m.selected[side],-1,14);range(s.stage[side],0,3);PcDebateState.Speaker p=s.speaker(side);range(p.health,-100,1000);range(p.anger,0,100);range(p.fury,0,4);range(p.deckCursor,0,18);
            if(p.hand[0]!=0)throw new IOException("原熟虑固定槽缺失");
            for(int i=0;i<7;i++){int card=p.hand[i];range(card,-1,14);if(i>0&&card==0)throw new IOException("原熟虑重复到普通槽");if(i>=p.slots&&card!=-1)throw new IOException("原手牌越过有效槽");if(card>=10&&(p.talkMask&(1<<(card-10)))==0)throw new IOException("原手牌话术未掌握");}
            for(int card:p.deck){range(card,1,14);if(card>=10&&(p.talkMask&(1<<(card-10)))==0)throw new IOException("原牌库话术未掌握");}
            int seen=0;for(int i=0;i<5;i++){int card=p.specialPool[i];if(i>=p.specialCount){if(card!=-1)throw new IOException("原特殊池尾部无效");continue;}range(card,10,14);int bit=1<<(card-10);if((seen&bit)!=0||(p.talkMask&bit)==0)throw new IOException("原特殊池重复或未掌握");seen|=bit;}
            if(seen!=p.talkMask)throw new IOException("原特殊池与话术配置不同");
        }
    }
}
