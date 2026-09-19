package game.sanguo.core;

/** Unit energy changes share caps and return the actual change, never a requested over/underflow. */
public final class EnergyRules {
    public enum Reason { COMMAND, WEIFENG, SAOTAO, NUFA, ANGYANG, MUSIC, ZOUYUE, WAIT, AMBUSH, DUEL }
    public static final class Change {
        public final Reason reason; public final int before,after,actual;
        Change(Reason reason,int before,int after){this.reason=reason;this.before=before;this.after=after;actual=after-before;}
    }
    private final World w;
    EnergyRules(World w){this.w=w;}
    public Change preview(World.Unit u,int amount,Reason reason){return new Change(reason,u.energy,(int)Math.max(0,Math.min(w.campaign.energyCap(u.owner),(long)u.energy+amount)));}
    Change change(World.Unit u,int amount,Reason reason){Change result=preview(u,amount,reason);u.energy=result.after;return result;}
    public int hitDrain(World.Unit source){return w.skills.has(source,Skill.WEIFENG)?20:w.skills.has(source,Skill.SAOTAO)?5:0;}
    public String hitPreview(World.Unit source,World.Unit target){int drain=hitDrain(source);return drain==0?"命中无特技扣气。":"造成物理伤害时，目标气力至多−"+Math.min(target.energy,drain)+"（每目标一次；威风优先扫讨）。";}
    /** Live local structure query; no stale cache after movement, destruction or ownership changes. */
    public int recovery(World.Unit u){
        for(War.Structure s:w.war.structures)if(s.complete&&s.kind==War.StructureKind.MUSIC&&s.owner==u.owner&&s.hex.distance(u.hex)<=2)
            return w.skills.has(u,Skill.SHIXIANG)?20:10;
        return w.skills.has(u,Skill.ZOUYUE)?5:0;
    }
    /** World calls this once per global旬, never from owner reset or UI/restore. */
    void settleTurn(){for(World.Unit u:w.units){
        int gain=recovery(u);Reason reason=gain>=10?Reason.MUSIC:Reason.ZOUYUE;
        if(preview(u,gain,reason).actual==0)continue;
        if(w.turnJournal!=null){
            War.Structure source=null;
            for(War.Structure s:w.war.structures)if(s.complete&&s.kind==War.StructureKind.MUSIC&&s.owner==u.owner&&s.hex.distance(u.hex)<=2){source=s;break;}
            if(source!=null)w.turnJournal.facility(source,u.hex,TurnJournal.Kind.RECOVER,"军乐台恢复");
            else w.turnJournal.mark(TurnJournal.Kind.RECOVER,u.id,u.hex,"奏乐恢复");
        }
        Change result=change(u,gain,reason);
        if(w.turnJournal!=null)w.turnJournal.checkpoint((reason==Reason.MUSIC?"军乐台":"奏乐")+"：气力+"+result.actual);
    }}
}
