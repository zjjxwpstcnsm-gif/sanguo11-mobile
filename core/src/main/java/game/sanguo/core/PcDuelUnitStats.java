package game.sanguo.core;

/** Original496570 combat bytes after current crew/terrain/template binding.
 * The x87 multiplication order and stored single precision coefficients are
 * retained. No engineering Army.attackPower or mutable presentation cache. */
final class PcDuelUnitStats {
    private static int[][]templates;
    private static synchronized int[][] templates()throws java.io.IOException {
        if(templates!=null)return templates;byte[]raw;
        try(var in=PcDuelUnitStats.class.getResourceAsStream("/pc-duel/unit-templates.tsv");var out=new java.io.ByteArrayOutputStream()){if(in==null)throw new java.io.IOException("原兵装模板资源缺失");byte[]block=new byte[1024];int n;while((n=in.read(block))!=-1){if(out.size()+n>4096)throw new java.io.IOException("原兵装模板资源过大");out.write(block,0,n);}raw=out.toByteArray();}
        try{String digest=PcCommandCapacityPolicy.hex(java.security.MessageDigest.getInstance("SHA-256").digest(raw));if(!digest.equals("1ad67f7ca4fb441a18c56ed9fb9faf8449083db992ff0112a7c4db77e754c7cf"))throw new java.io.IOException("原兵装模板SHA不同");}catch(java.security.NoSuchAlgorithmException e){throw new java.io.IOException(e);}
        String[]lines=new String(raw,java.nio.charset.StandardCharsets.UTF_8).split("\n");if(lines.length!=6||!lines[0].equals("# original496570 receipt 55adce37a51d52fd199dc8eed816a255bca4597a47fb6b185cbf6357984c02a2"))throw new java.io.IOException("原兵装模板来源不同");
        int[][]out=new int[5][2];for(int i=0;i<5;i++){String[]p=lines[i+1].split("\t");try{if(p.length!=4||Integer.parseInt(p[0])!=i||!p[3].matches("[0-9a-f]{64}"))throw new java.io.IOException("原兵装模板连接不同");out[i][0]=Integer.parseInt(p[1]);out[i][1]=Integer.parseInt(p[2]);if(out[i][0]<0||out[i][0]>255||out[i][1]<0||out[i][1]>255)throw new java.io.IOException("原兵装模板属性无效");}catch(NumberFormatException e){throw new java.io.IOException(e);}}
        return templates=out;
    }
    /** Verified current land normal-unit adapter. The caller supplies the
     * original4811e0 elite-tech fact; its source binding remains separate. */
    static Combat currentCombat(World w,World.Unit unit,boolean originalEliteTech)throws java.io.IOException {
        if(unit instanceof Domestic.Mission||unit==null||w.army.water(unit.hex)||Army.siegeWeapon(unit.weapon))throw new java.io.IOException("原当前运输、水军或兵器数值尚未接入");
        int nativeItem=PcProduction.nativeItem(unit.weapon);if(nativeItem<0||nativeItem>4)throw new java.io.IOException("原当前兵装范围未知");
        int[]crew=currentCrew(w,unit),apt=currentAptitudes(w,unit);int[][]source=templates();
        float coefficient=nativeItem==0?Float.intBitsToFloat(0x3f19999a):(float)((apt[nativeItem-1]+7)*(double)Float.intBitsToFloat(0x3dcccccd));
        int extra=nativeItem>0&&originalEliteTech?10:0;
        return combat(crew[0],crew[1],source[nativeItem][0]+extra,source[nativeItem][1]+extra,Float.floatToRawIntBits(coefficient),0x3f800000,unit.status==War.Status.CONFUSED?0x3ecccccd:0x3f800000);
    }
    static Combat currentCombat(World w,World.Unit unit)throws java.io.IOException {
        if(unit==null)throw new java.io.IOException("原部队不存在");int item=PcProduction.nativeItem(unit.weapon);
        return currentCombat(w,unit,item>0&&item<=4&&PcSourceTechnologyPolicy.has(w,unit.owner,item*4-1));
    }
    /** Original495ab0: directional sworn/spouse/like then internal root,
     * with the later reverse-like test actually self-like in this executable. */
    static int contribution(int leader,int deputy,boolean sworn,boolean spouse,boolean like,boolean deputySelfLike,boolean internalFamily,boolean dislikes){
        if(leader<0||leader>255||deputy<0||deputy>255)throw new IllegalArgumentException("原编队当前能力无效");
        if(leader>=deputy)return leader;
        if(sworn||spouse)return deputy;
        if(like||deputySelfLike)return leader+(deputy-leader)/2;
        if(internalFamily)return leader+(deputy-leader)/3;
        return dislikes?leader:leader+(deputy-leader)/4;
    }
    /** Original496570 first checks every pair for mutual dislike. Any such
     * pair discards all deputies' five attributes; six aptitudes stay separate. */
    static int[] currentCrew(World w,World.Unit unit)throws java.io.IOException {
        if(unit==null||w.unit(unit.id)!=unit)throw new java.io.IOException("原编队当前来源未核实");
        var crew=w.army.crew(unit);if(crew.isEmpty()||crew.size()>3)throw new java.io.IOException("原编队人数无效");
        var runtime=PcDuelRuntimeFacts.saved(w);var sources=PcDuelSourceFacts.saved(w);if(runtime==null)throw new java.io.IOException("原编队内部关系来源缺失");
        for(var o:crew)if(o==null||!w.life.present(o.id)||!sources.containsKey(o.id)||o.unitId!=unit.id||o.abilityProfile==null)throw new java.io.IOException("原编队人物稳定连接无效");
        boolean hate=false;for(var a:crew)for(var b:crew)if(a!=b&&w.relations.dislikes(a.id,b.id))hate=true;
        var leader=crew.get(0);int[] values={leader.leadership,leader.war,leader.intelligence,leader.politics,leader.charm};
        if(hate)return values;
        var own=runtime.people.get(sources.get(leader.id).nativeId);if(own==null)throw new java.io.IOException("原主将内部关系缺失");
        for(int index=1;index<crew.size();index++){
            var deputy=crew.get(index);var p=runtime.people.get(sources.get(deputy.id).nativeId);if(p==null)throw new java.io.IOException("原副将内部关系缺失");
            PcDuelRecruitmentAdmission.unchangedGroup(w,leader.id,runtime,sources);PcDuelRecruitmentAdmission.unchangedGroup(w,deputy.id,runtime,sources);
            boolean family=PcDuelRecruitmentAdmission.sameInternalFather(w,leader.id,deputy.id);
            int[] other={deputy.leadership,deputy.war,deputy.intelligence,deputy.politics,deputy.charm};
            for(int stat=0;stat<5;stat++)values[stat]=Math.max(values[stat],stat>=2?other[stat]:contribution(stat==0?leader.leadership:leader.war,other[stat],PcDuelSwornPolicy.current(w,own.nativeId)>=0&&PcDuelSwornPolicy.current(w,own.nativeId)==PcDuelSwornPolicy.current(w,p.nativeId),w.relations.spouse(leader.id)==deputy.id,w.relations.likes(leader.id,deputy.id),w.relations.likes(deputy.id,deputy.id),family,false));
        }
        return values;
    }
    static int[] currentAptitudes(World w,World.Unit unit)throws java.io.IOException {
        if(unit==null||w.unit(unit.id)!=unit)throw new java.io.IOException("原部队当前引用不同");
        int[]values=new int[6];for(var o:w.army.crew(unit)){
            if(o==null||!w.life.present(o.id)||o.unitId!=unit.id||!PcDuelSourceFacts.saved(w).containsKey(o.id)||o.aptitude.length!=6)throw new java.io.IOException("原部队适性人物来源未知");
            for(int i=0;i<6;i++){if(o.aptitude[i]<0||o.aptitude[i]>3)throw new java.io.IOException("原部队适性范围未知");values[i]=Math.max(values[i],o.aptitude[i]);}
        }
        return values;
    }
    static final class Combat {
        final int attack,defense;
        Combat(int attack,int defense){this.attack=attack;this.defense=defense;}
    }
    static Combat combat(int leadership,int war,int templateAttack,int templateDefense,int aptitudeBits,int conditionBits,int offenseBits){
        if(leadership<0||leadership>255||war<0||war>255||templateAttack<0||templateAttack>265||templateDefense<0||templateDefense>265)throw new IllegalArgumentException("原部队当前属性范围未知");
        double aptitude=Float.intBitsToFloat(aptitudeBits),condition=Float.intBitsToFloat(conditionBits),offense=Float.intBitsToFloat(offenseBits);
        if(!Double.isFinite(aptitude)||aptitude<=0||aptitude>10||condition!=1.0&&condition!=(double)Float.intBitsToFloat(0x3f4ccccd)||offense!=1.0&&offense!=(double)Float.intBitsToFloat(0x3ecccccd))throw new IllegalArgumentException("原部队系数来源未知");
        double scalar=Float.intBitsToFloat(0x3c23d70a);
        int attack=(int)Math.max(1.0,war*(double)templateAttack*aptitude*scalar*offense*condition);
        // Original status1 defense uses float1/3 rather than offense0.4.
        double defenseStatus=offense==1.0?1.0:Float.intBitsToFloat(0x3eaaaaab);
        int defense=(int)Math.max(1.0,templateDefense*(double)leadership*aptitude*scalar*defenseStatus*condition);
        return new Combat(attack&255,defense&255);
    }
    private PcDuelUnitStats(){}
}
