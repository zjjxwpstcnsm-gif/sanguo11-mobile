package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Explicit new-source initial technologies. Existing saves never backfill. */
final class PcSourceTechnologyPolicy {
    static final String NAMESPACE="pc-source-technologies-v1";
    private static final int MAGIC=0x50535431;
    private static final Campaign.Tech[]NATIVE={
        Campaign.Tech.SPEAR_DRILL,Campaign.Tech.SUPPLY_RAID,Campaign.Tech.FOREST_AMBUSH,Campaign.Tech.ELITE_SPEAR,
        Campaign.Tech.HALBERD_DRILL,Campaign.Tech.SHIELD,Campaign.Tech.LARGE_SHIELD,Campaign.Tech.ELITE_HALBERD,
        Campaign.Tech.CROSSBOW_DRILL,Campaign.Tech.RETURN_FIRE,Campaign.Tech.STRONG_BOW,Campaign.Tech.ELITE_CROSSBOW,
        Campaign.Tech.CAVALRY_DRILL,Campaign.Tech.HORSE_BREEDING,Campaign.Tech.MOUNTED_ARCHERY,Campaign.Tech.ELITE_CAVALRY,
        Campaign.Tech.LOGISTICS,Campaign.Tech.DIFFICULT_MARCH,Campaign.Tech.MILITARY_REFORM,Campaign.Tech.SIEGE_LADDERS,
        Campaign.Tech.AXLE,Campaign.Tech.STONE_BUILDING,Campaign.Tech.CATAPULT,Campaign.Tech.THUNDERBOLT,
        Campaign.Tech.ENGINEERING,Campaign.Tech.FACILITY_REINFORCEMENT,Campaign.Tech.WALLS,Campaign.Tech.DEFENSE_REINFORCEMENT,
        Campaign.Tech.WOODEN_BEAST,Campaign.Tech.FIRE_MASTERY,Campaign.Tech.GUNPOWDER,Campaign.Tech.EXPLOSIVES,
        Campaign.Tech.WOODEN_OX,Campaign.Tech.PORT_EXPANSION,Campaign.Tech.ADMINISTRATION,Campaign.Tech.POPULAR_SUPPORT};
    static Campaign.Tech technology(int nativeId){return nativeId<0||nativeId>=36?null:NATIVE[nativeId];}
    static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
    static void initializeOpening(World w,PcScenarioCatalog.Source source)throws IOException {
        if(!w.pcSourceFrame||w.turn!=0||w.commandRevision()!=0||w.contests.busy()||w.extensions.get(NAMESPACE)!=null||!w.campaign.learned.isEmpty())throw new IOException("原技巧只能在明确新局初始化");
        byte[]raw;try(var in=PcSourceTechnologyPolicy.class.getResourceAsStream("/pc-duel/source-technologies.tsv");var out=new ByteArrayOutputStream()){if(in==null)throw new IOException("原技巧资源缺失");byte[]block=new byte[8192];int n;while((n=in.read(block))!=-1){if(out.size()+n>262144)throw new IOException("原技巧资源过大");out.write(block,0,n);}raw=out.toByteArray();}
        try{if(!PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)).equals("28239e64d1e8808fd8eda496b6e5c91c809d207fe678db5e22a7afbad89d0f42"))throw new IOException("原技巧资源SHA不同");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
        String[]lines=new String(raw,java.nio.charset.StandardCharsets.UTF_8).split("\n");if(lines.length!=753||!lines[0].equals("# original4811e0 receipt 23b0a9741c0966f96e746f996fbd4d6be2ca0ca949786b60b24cc8ba9ac6a0a1"))throw new IOException("原技巧覆盖或来源不同");
        long[]bits=new long[47];String[]sha=new String[47];
        for(int i=1;i<lines.length;i++){String[]p=lines[i].split("\t",-1);if(p.length!=6)throw new IOException("原技巧资源列数不同");if(!p[0].equals(source.identity.scenarioId))continue;
            try{int n=Integer.parseInt(p[3]);long b=Long.parseLong(p[4]);if(n<0||n>=47||sha[n]!=null||b<0||b>>>36!=0||!p[1].equals(source.identity.sha)||!p[2].equals(source.identity.sourceVariant)||!p[5].matches("[0-9a-f]{64}"))throw new IOException("原技巧来源或编号不同");bits[n]=b;sha[n]=p[5];}catch(NumberFormatException e){throw new IOException(e);}}
        if(source.forces.size()!=47)throw new IOException("原势力来源缺失");
        for(int n=0;n<47;n++){if(sha[n]==null)throw new IOException("原技巧来源覆盖不足");var f=source.forces.get(n);if(f.nativeId!=n)throw new IOException("原技巧势力连接不同");
            if(n<42&&f.valid){EnumSet<Campaign.Tech>set=EnumSet.noneOf(Campaign.Tech.class);for(int t=0;t<36;t++)if((bits[n]&(1L<<t))!=0)set.add(NATIVE[t]);if(!set.isEmpty())w.campaign.learned.put(n,set);}}
        var bytes=new ByteArrayOutputStream();var d=new DataOutputStream(bytes);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(PcScenarioIdentity.EXE_SHA);d.writeUTF(source.identity.scenarioId);d.writeUTF(source.identity.sha);d.writeUTF(source.identity.sourceVariant);d.writeInt(47);for(int n=0;n<47;n++){d.writeInt(n);d.writeLong(bits[n]);d.writeUTF(sha[n]);}w.extensions.put(NAMESPACE,bytes.toByteArray());
    }
    /** Current learned state comes from the existing save/research mechanism;
     * initial source bits are provenance, never reapplied after loading. */
    static boolean has(World w,int owner,int nativeId)throws IOException {
        if(!enabled(w))throw new IOException("当前存档没有明确原技巧策略");validate(w);Campaign.Tech tech=technology(nativeId);return owner>=0&&owner<42&&tech!=null&&w.campaign.learned.getOrDefault(owner,EnumSet.noneOf(Campaign.Tech.class)).contains(tech);
    }
    static void validate(World w)throws IOException {
        if(!enabled(w))return;byte[]raw=w.extensions.get(NAMESPACE);if(raw.length>8192)throw new IOException("原技巧保存过大");var d=new DataInputStream(new ByteArrayInputStream(raw));var s=PcScenarioIdentity.saved(w);
        if(d.readInt()!=MAGIC||d.readInt()!=1||!d.readUTF().equals(PcScenarioIdentity.EXE_SHA)||s==null||!d.readUTF().equals(s.scenarioId)||!d.readUTF().equals(s.sha)||!d.readUTF().equals(s.sourceVariant)||d.readInt()!=47)throw new IOException("原技巧保存来源不同");
        for(int n=0;n<47;n++){if(d.readInt()!=n)throw new IOException("原技巧保存势力连接不同");long b=d.readLong();if(b<0||b>>>36!=0||!d.readUTF().matches("[0-9a-f]{64}"))throw new IOException("原技巧保存来源位无效");}if(d.available()!=0)throw new IOException("原技巧保存尾部未知");
    }
    private PcSourceTechnologyPolicy(){}
}
