package game.sanguo.core;
import java.io.*;
/** Explicit new-game native technique point policy. No inference/migration from old saves. */
public final class PcTechniquePoints {
    private static final int MAGIC=0x50545031;
    private final World w;private boolean enabled;
    PcTechniquePoints(World w){this.w=w;}
    public boolean enabled(){return enabled;}
    public void initializeOpening(){
        if(!w.pcProduction.enabled())throw new IllegalStateException("技巧来源模式需要原生产配置");
        for(int owner=0;owner<w.factions.length;owner++)if(w.campaign.points(owner)>10000)throw new IllegalStateException("新局技巧点超过原来源上限");
        enabled=true;
    }
    /** Original4b6460: positive requests half/min1; signed deductions unscaled; clamp0..10000. */
    public static int after(int before,int request){
        if(before<0)throw new IllegalArgumentException("技巧点不能为负");
        long delta=request>0?Math.max(1,request/2):request;
        return (int)Math.max(0,Math.min(10000,(long)before+delta));
    }
    public static int productionRequest(int credited){
        if(credited<0)throw new IllegalArgumentException("入库数量不能为负");
        return Math.min(10,credited/300+1);
    }
    public int productionAfter(int owner,int credited){return enabled?after(w.campaign.points(owner),productionRequest(credited)):w.campaign.points(owner);}
    void production(int owner,int credited){production(owner,credited,-1,-1);}
    void production(int owner,int credited,int city,int officer){if(enabled)w.campaign.setPoints(owner,productionAfter(owner,credited),TechniquePointsJournal.Cause.NATIVE_PRODUCTION,city,officer);}
    void completion(int owner){completion(owner,-1,-1);}
    void completion(int owner,int city,int officer){if(enabled)w.campaign.setPoints(owner,after(w.campaign.points(owner),20),TechniquePointsJournal.Cause.NATIVE_MANUFACTURE,city,officer);}
    void write(DataOutputStream out)throws IOException{validate();out.writeInt(MAGIC);out.writeUTF(PcProduction.SOURCE);}
    void read(DataInputStream in)throws IOException{if(in.readInt()!=MAGIC||!PcProduction.SOURCE.equals(in.readUTF()))throw new IOException("未知技巧来源策略");enabled=true;}
    void validate()throws IOException{
        if(!enabled)return;if(!w.pcProduction.enabled())throw new IOException("技巧来源策略缺少原生产配置");
        for(int owner=0;owner<w.factions.length;owner++)if(w.campaign.points(owner)>10000)throw new IOException("原技巧点来源范围无效");
    }
}
