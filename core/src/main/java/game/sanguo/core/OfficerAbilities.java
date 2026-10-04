package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Saved base/experience/growth; Officer's public five values are current caches.
 * Legacy saves remain explicitly unmanaged: their historical bases are unknown. */
public final class OfficerAbilities {
    private static final int MAGIC=0x4f415331;
    static final class Profile {
        final OfficerAbilities owner;final int[] base=new int[5],growth=new int[5],experience=new int[5];
        int sourceBirth,sourceId=-1;String nativeIds="";boolean special;
        Profile(OfficerAbilities owner,World.Officer o){this.owner=owner;for(int i=0;i<5;i++){base[i]=raw(o,i);growth[i]=-1;}}
    }
    private final World w;
    private boolean enabled,fixedAge,requestedGrowthDisabled;
    OfficerAbilities(World w){this.w=w;}
    public boolean enabled(){return enabled;}
    public boolean fixedAge(){return fixedAge;}
    public boolean growthDisabled(){return PcOfficerAbilityRules.growthDisabled(requestedGrowthDisabled,fixedAge);}
    public int base(int officer,int stat){World.Officer o=w.officer(officer);if(o==null||stat<0||stat>4)throw new IllegalArgumentException("武将或能力无效");return base(o,stat);}
    static int base(World.Officer o,int stat){return o.abilityProfile==null?raw(o,stat):o.abilityProfile.base[stat];}
    public int experience(int officer,int stat){World.Officer o=w.officer(officer);if(o==null||stat<0||stat>4)throw new IllegalArgumentException("武将或能力无效");return o.abilityProfile==null?0:o.abilityProfile.experience[stat];}
    public int growthCode(int officer,int stat){World.Officer o=w.officer(officer);if(o==null||stat<0||stat>4)throw new IllegalArgumentException("武将或能力无效");return o.abilityProfile==null?-1:o.abilityProfile.growth[stat];}
    /** New scenario setup only. Never called by save decoding or by a preview. */
    void initializeOpening(ContentCatalog catalog,boolean fixedAge,boolean requestedDisabled){
        if(enabled)throw new IllegalStateException("能力状态已经初始化");this.fixedAge=fixedAge;requestedGrowthDisabled=requestedDisabled;enabled=true;BasicCityPolicy.managed(w);
        for(World.Officer o:w.officers){
            register(o);
            if(catalog!=null)bindSource(o,catalog,true);
        }
        refresh();
    }
    /** Explicit source import only: never infer identity when an arbitrary roster entry is added. */
    void bindSource(World.Officer o,ContentCatalog catalog,boolean dated){
        if(!enabled||!ContentProfiles.matches(w,catalog,o.id))return;
        PcOfficerGrowth.Definition d=PcOfficerGrowth.get(o.id);if(d==null)return;
        Profile p=o.abilityProfile;p.sourceId=o.id;p.nativeIds=d.nativeIds;p.special=d.special;p.sourceBirth=dated?catalog.officer(o.id).birth:0;System.arraycopy(d.curves,0,p.growth,0,5);refresh(o);
    }
    void register(World.Officer o){if(enabled&&(o.abilityProfile==null||o.abilityProfile.owner!=this))o.abilityProfile=new Profile(this,o);}
    void refresh(){if(enabled)for(World.Officer o:w.officers)refresh(o);}
    void refresh(World.Officer o){if(o.abilityProfile!=null)for(int stat=0;stat<5;stat++)raw(o,stat,calculate(o,stat,o.abilityProfile.experience[stat]));}
    private int calculate(World.Officer o,int stat,int experience){
        Profile p=o.abilityProfile;if(p==null)return raw(o,stat);
        Lifecycle.Life life=w.life.life(o.id);int birth=life==null?p.sourceBirth:life.birth;
        int age=PcOfficerAbilityRules.age(w.startYear,w.startMonth,1,w.turn,birth,fixedAge);
        Government.Rank rank=w.government.office(o.id);World.Officer spouse=w.officer(w.relations.spouse(o.id));
        boolean spouseBonus=spouse!=null&&w.life.present(spouse.id)&&(w.skills.has(o,Skill.NEIZHU)||w.skills.has(spouse,Skill.NEIZHU));
        // No curve is invented for an authored/unknown person or unknown birth.
        int curve=birth==0?-1:p.growth[stat];
        return PcOfficerAbilityRules.current(p.base[stat],curve,age,experience,stat,w.contests.injury(o.id),rank==null?-1:rank.abilityStat,rank==null?0:rank.abilityBonus,spouseBonus,p.special,growthDisabled());
    }
    int afterExperience(int officer,int stat,int amount){World.Officer o=w.officer(officer);return calculate(o,stat,experienceAfter(officer,stat,amount));}
    int experienceAfter(int officer,int stat,int amount){World.Officer o=w.officer(officer);if(o.abilityProfile==null)return 0;return Math.max(0,Math.min(3000,o.abilityProfile.experience[stat]+amount));}
    void gainExperience(int officer,int stat,int amount){World.Officer o=w.officer(officer);if(o.abilityProfile==null)return;o.abilityProfile.experience[stat]=experienceAfter(officer,stat,amount);refresh(o);}
    static void setBase(World.Officer o,int stat,int value){
        if(stat<0||stat>4||value<0||value>255)throw new IllegalArgumentException("基础能力无效");
        if(o.abilityProfile==null)raw(o,stat,value);else{o.abilityProfile.base[stat]=value;o.abilityProfile.owner.refresh(o);}
    }
    static int raw(World.Officer o,int stat){switch(stat){case 0:return o.leadership;case 1:return o.war;case 2:return o.intelligence;case 3:return o.politics;case 4:return o.charm;default:throw new IllegalArgumentException();}}
    private static void raw(World.Officer o,int stat,int value){switch(stat){case 0:o.leadership=value;break;case 1:o.war=value;break;case 2:o.intelligence=value;break;case 3:o.politics=value;break;case 4:o.charm=value;break;default:throw new IllegalArgumentException();}}
    void write(DataOutputStream out)throws IOException{
        out.writeInt(MAGIC);out.writeBoolean(fixedAge);out.writeBoolean(requestedGrowthDisabled);out.writeInt(w.officers.size());
        for(World.Officer o:w.officers){Profile p=o.abilityProfile;out.writeInt(o.id);out.writeInt(p.sourceBirth);out.writeInt(p.sourceId);out.writeUTF(p.nativeIds);out.writeBoolean(p.special);for(int i=0;i<5;i++){out.writeInt(p.base[i]);out.writeInt(p.growth[i]);out.writeInt(p.experience[i]);}}
    }
    void read(DataInputStream in)throws IOException{
        if(in.readInt()!=MAGIC)throw new IOException("人物能力段标记无效");fixedAge=in.readBoolean();requestedGrowthDisabled=in.readBoolean();int n=in.readInt();if(n!=w.officers.size())throw new IOException("人物能力条数无效");
        for(World.Officer o:w.officers){if(in.readInt()!=o.id)throw new IOException("人物能力身份不匹配");Profile p=new Profile(this,o);p.sourceBirth=in.readInt();p.sourceId=in.readInt();p.nativeIds=in.readUTF();p.special=in.readBoolean();for(int i=0;i<5;i++){p.base[i]=in.readInt();p.growth[i]=in.readInt();p.experience[i]=in.readInt();}o.abilityProfile=p;}enabled=true;
    }
    void validate()throws IOException{
        for(World.Officer o:w.officers){Profile p=o.abilityProfile;if(!enabled){require(p==null,"旧档能力模式冲突");continue;}
            require(p!=null&&p.owner==this,"人物基础能力缺失");require(p.sourceBirth>=0&&p.sourceBirth<=9999&&p.sourceId>=-1,"人物能力来源无效");
            if(p.sourceId<0)require(p.nativeIds.isEmpty()&&!p.special,"自定义人物不可继承原编号");
            else {require(p.nativeIds.length()>0&&p.nativeIds.length()<=100,"原人物编号集合无效");try{for(String value:p.nativeIds.split(",",-1)){int id=Integer.parseInt(value);require(id>=0&&id<1100&&(id>=700&&id<=799)==p.special,"原人物特殊槽位不一致");}}catch(NumberFormatException e){throw new IOException("原人物编号格式无效",e);}}
            for(int i=0;i<5;i++){require(p.base[i]>=0&&p.base[i]<=255&&p.growth[i]>=-1&&p.growth[i]<=8&&p.experience[i]>=0&&p.experience[i]<=3000,"人物基础/成长/经验越界");require(raw(o,i)==calculate(o,i,p.experience[i]),"人物当前能力与基础状态不一致");}
        }
    }
    private static void require(boolean ok,String message)throws IOException{if(!ok)throw new IOException(message);}
}
