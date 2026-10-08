package game.sanguo.core;
/** Explicit caller-selected new-game options. These are original option
 * domains, not certified PC GUI defaults. COMMAND_FOCUS is a declared mobile
 * presentation strategy; original camera/frustum parity remains outstanding. */
public final class PcDuelOptions {
    public final int life,death,difficulty;
    public PcDuelOptions(int life,int death,int difficulty){if(life<0||life>3||death<0||death>2||difficulty<0||difficulty>2)throw new IllegalArgumentException("原单挑新局设置范围无效");this.life=life;this.death=death;this.difficulty=difficulty;}
    /** The three actual menu choices exclude internal source-override value3. */
    public static PcDuelOptions fromMenu(int life,int death,int difficulty){if(life<0||life>2)throw new IllegalArgumentException("原寿命菜单选项无效");return new PcDuelOptions(life,death,difficulty);}
    /** Existing saved choice only; missing/old policies remain absent. */
    public static PcDuelOptions current(World w)throws java.io.IOException{return PcDuelCampaignPolicy.options(w);}
    PcDuelKernel.OriginalSettings settings(){return new PcDuelKernel.OriginalSettings(true,life,true,difficulty,true,death);}
}
