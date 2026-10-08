package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Original4b2380 pending selection and4b01b0 masks; no automatic capture. */
final class PcDuelDisposition {
    static final int RECRUIT=0,DETAIN=1,RELEASE=2,EXECUTE=3,PENDING=4;
    static final class Row {
        final int officerId,nativeId;int mask,choice;boolean recruitmentAdmitted;
        Row(int id,int nativeId,int mask,int choice){officerId=id;this.nativeId=nativeId;this.mask=mask;this.choice=choice;}
        String error(int action){return action<0||action>3?"请选择有效处置":(mask&(1<<action))==0?"该人物当前不能采用此处置":null;}
    }
    final List<Row> rows;
    PcDuelDisposition(List<Row> rows)throws IOException{this.rows=new ArrayList<>(rows);validate();}
    static int initialMask(boolean ruler,boolean retreatCity){return ruler&&retreatCity?12:15;}
    static PcDuelDisposition create(World w,PcDuelCampaign duel)throws IOException {
        if(!duel.terminal())throw new IOException("单挑尚未到终局");List<Row> rows=new ArrayList<>();
        for(int side=0;side<2;side++)for(int slot=0;slot<3;slot++)if(PcDuelKernel.readManager(duel.state.manager,0x64+12*side+4*slot)==1){
            int id=duel.state.officers[side*3+slot],nativeId=duel.state.natives[side*3+slot];World.Officer o=w.officer(id);
            if(o==null||nativeId<0||nativeId>=670||w.government.captive(id))throw new IOException("原终局处置人物来源无效");
            // Original4ce2f0 enumerates exactly42 owned cities, excluding gates/ports.
            boolean city=w.cities.stream().anyMatch(c->c.kind==World.SiteKind.CITY&&c.owner==o.owner);
            rows.add(new Row(id,nativeId,initialMask(o.role==Strategy.Role.RULER,city),PENDING));
        }
        return new PcDuelDisposition(rows);
    }
    boolean ready(){return rows.stream().allMatch(r->r.choice!=PENDING);}
    String confirmationError(){return !ready()?"请先选择每位人物的终局处置":rows.stream().anyMatch(r->r.choice==RECRUIT&&!r.recruitmentAdmitted)?"已保存的登用选择尚未完成原成功判定，完整待办已保留":null;}
    String error(int officer,int action){Row row=rows.stream().filter(r->r.officerId==officer).findFirst().orElse(null);return row==null?"当前没有该人物的终局处置":row.error(action);}
    void select(int officer,int action)throws IOException{String error=error(officer,action);if(error!=null)throw new IOException(error);rows.stream().filter(r->r.officerId==officer).findFirst().orElseThrow().choice=action;}
    void recruitmentFailed(int officer)throws IOException{
        Row row=rows.stream().filter(r->r.officerId==officer).findFirst().orElseThrow(()->new IOException("登用处置人物不存在"));
        row.mask&=~1;row.choice=PENDING;row.recruitmentAdmitted=false;validate();
    }
    void validate()throws IOException{
        if(rows.isEmpty()||rows.size()>6)throw new IOException("原终局处置人物数量无效");Set<Integer> ids=new HashSet<>(),natives=new HashSet<>();
        for(Row r:rows)if(r.officerId<0||r.officerId>999999||r.nativeId<0||r.nativeId>=670||r.mask<1||r.mask>15||r.choice<0||r.choice>4||r.choice!=PENDING&&(r.mask&(1<<r.choice))==0||r.recruitmentAdmitted&&r.choice!=RECRUIT||!ids.add(r.officerId)||!natives.add(r.nativeId))throw new IOException("原终局处置保存无效");
    }
    void write(DataOutputStream out)throws IOException{write(out,false);}
    void write(DataOutputStream out,boolean admission)throws IOException{validate();out.writeInt(rows.size());for(Row r:rows){out.writeInt(r.officerId);out.writeInt(r.nativeId);out.writeInt(r.mask);out.writeInt(r.choice);if(admission)out.writeBoolean(r.recruitmentAdmitted);}}
    static PcDuelDisposition read(DataInputStream in)throws IOException{
        return read(in,false);
    }
    static PcDuelDisposition read(DataInputStream in,boolean admission)throws IOException{
        int n=in.readInt();if(n<1||n>6)throw new IOException("原终局处置保存数量无效");List<Row> rows=new ArrayList<>();for(int i=0;i<n;i++){var r=new Row(in.readInt(),in.readInt(),in.readInt(),in.readInt());if(admission){int flag=in.readUnsignedByte();if(flag>1)throw new IOException("原登用准入标志无效");r.recruitmentAdmitted=flag!=0;}rows.add(r);}return new PcDuelDisposition(rows);
    }
}
