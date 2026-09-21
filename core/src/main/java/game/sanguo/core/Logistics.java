package game.sanguo.core;

/** Ten-day field rations. Rounding and starvation percentage are explicit mobile balance choices. */
public final class Logistics {
    private Logistics(){}
    public static int baseUse(int troops,boolean transport){
        if(troops<=0)return 0;
        int divisor=transport?20:10;
        return (int)(((long)troops+divisor-1)/divisor);
    }
    public static int foodUse(World w,World.Unit unit){
        if(unit instanceof Domestic.Mission&&!((Domestic.Mission)unit).transport)return 0;
        int base=baseUse(unit.troops+unit.wounded,unit instanceof Domestic.Mission);
        return base==0?0:w.fieldworks.foodUse(unit,base);
    }
    /** Complete funded turns, assuming unchanged troop count and current supply auras. */
    public static int turns(int food,int use){return use<=0?Integer.MAX_VALUE:Math.max(0,food)/use;}
    public static int defaultFood(int troops,int stock){return (int)Math.min(Math.max(0L,stock),Math.min(1000000L,Math.max(0L,2L*troops)));}
    public static int deserters(int troops){return troops<=0?0:Math.min(troops,Math.max(1,troops/10));}
    public static String describe(World w,World.Unit u){
        int use=foodUse(w,u);
        return "战兵 "+u.troops+" · 伤兵 "+u.wounded+"（入城立即归队）\n旬耗粮 "+use+" · "+(use==0?"无兵员粮耗":"携粮可支撑 "+turns(u.food,use)+" 旬")
            +(use>u.food?" · 下次结算粮不足，逃兵10%（至少1兵）":"")+"\n按当前兵力与设施范围估算；兵力或位置变化后重算。";
    }
}
