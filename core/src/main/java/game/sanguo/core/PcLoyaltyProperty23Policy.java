package game.sanguo.core;

import java.io.*;

/** Explicit current-property input representation, distinct from trusted raw writes.
 * Original4c8a8b reads byteAC and returns min(raw,100). Saturated100 remains unknown.
 * Does not certify inherited engineering season changes as original PC mutations. */
final class PcLoyaltyProperty23Policy {
    static final String NAMESPACE="pc-loyalty-property23-input-v1";
    static final String GETTER_SHA="9ef1697731d3b3de7fb21198781befd8fbd9b08ae9aa60750f6322469493e5b5";
    static final String RECEIPT_SHA="e35032bad3de497218adea144fe7573584f712a0094ad645725be0ea15dee3dc";
    private static final int MAGIC=0x504c5031;
    static boolean enabled(World w){byte[] b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
    static void validate(World w)throws IOException{
        if(!enabled(w))return;
        byte[] b=w.extensions.get(NAMESPACE);if(b.length>2048)throw new IOException("忠诚输入解析策略过大");
        var source=PcScenarioIdentity.saved(w);if(source==null||!PcDuelRawLoyalty.enabled(w))throw new IOException("忠诚输入解析来源缺失");
        try(var in=new DataInputStream(new ByteArrayInputStream(b))){
            if(in.readInt()!=MAGIC||in.readInt()!=1||!in.readUTF().equals(PcScenarioIdentity.EXE_SHA)||!in.readUTF().equals(source.scenarioId)||!in.readUTF().equals(source.sha)||!in.readUTF().equals(source.sourceVariant)||in.readInt()!=0x4c8a8b||!in.readUTF().equals(GETTER_SHA)||!in.readUTF().equals(RECEIPT_SHA)||in.available()!=0)throw new IOException("忠诚输入解析的原函数或保存来源不同");
        }
    }
    static String adoptionError(World w){
        if(w.extensions.get(NAMESPACE)!=null)return enabled(w)?"当前保存已采用忠诚输入解析":"保存已有未知忠诚输入策略，请保留原档";
        try{PcDuelRawLoyalty.validate(w);if(!PcDuelRawLoyalty.enabled(w)||PcScenarioIdentity.saved(w)==null)return "保存没有原忠诚人物来源，不会自动补填";}
        catch(IOException e){return e.getMessage();}
        return null;
    }
    static void adopt(World w)throws IOException{
        String error=adoptionError(w);if(error!=null)throw new IOException(error);var source=PcScenarioIdentity.saved(w);
        var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){out.writeInt(MAGIC);out.writeInt(1);out.writeUTF(PcScenarioIdentity.EXE_SHA);out.writeUTF(source.scenarioId);out.writeUTF(source.sha);out.writeUTF(source.sourceVariant);out.writeInt(0x4c8a8b);out.writeUTF(GETTER_SHA);out.writeUTF(RECEIPT_SHA);}
        w.extensions.put(NAMESPACE,bytes.toByteArray());validate(w);
    }
    static void initializeOpening(World w)throws IOException{
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy())throw new IOException("忠诚输入解析仅在明确新局启用");adopt(w);
    }
    static int uniqueInput(int display)throws IOException{
        if(display<0||display>100)throw new IOException("当前忠诚显示值超出原属性范围");
        if(display==100)throw new IOException("忠诚显示100对应原始100至255，不能唯一解析");
        return display;
    }
    private PcLoyaltyProperty23Policy(){}
}
