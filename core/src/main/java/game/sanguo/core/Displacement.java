package game.sanguo.core;

import java.util.*;

/** Campaign displacement only. Pure inspection and execution share each step's terrain/occupancy rules. */
public final class Displacement {
    enum Kind {
        NONE(0,false,false), THRUST(1,false,true), DOUBLE(2,false,true),
        HOOK(1,false,false), CHARGE(1,true,false), BREAKTHROUGH(1,false,false),
        ADVANCE(2,true,false), NAVAL(1,false,true);
        final int steps;final boolean follow,collision;
        Kind(int steps,boolean follow,boolean collision){this.steps=steps;this.follow=follow;this.collision=collision;}
    }
    public static final class Preview {
        public final String error,text;
        public final List<Hex> actorPath,targetPath,riskHexes;
        public final Hex blocked;
        public final boolean friendlyRisk;
        Preview(String error,String text,List<Hex> a,List<Hex> b,List<Hex> risks,Hex blocked,boolean friendlyRisk){
            this.error=error;this.text=text;actorPath=Collections.unmodifiableList(new ArrayList<>(a));targetPath=Collections.unmodifiableList(new ArrayList<>(b));riskHexes=Collections.unmodifiableList(new ArrayList<>(risks));this.blocked=blocked;this.friendlyRisk=friendlyRisk;
        }
        public boolean valid(){return error==null;}
    }
    private final World w;
    Displacement(World w){this.w=w;}
    static Kind kind(War.Tactic t){
        if(t==null)return Kind.NONE;
        switch(t){case THRUST:return Kind.THRUST;case DOUBLE_THRUST:return Kind.DOUBLE;case HOOK:return Kind.HOOK;
            case CHARGE:return Kind.CHARGE;case BREAKTHROUGH:return Kind.BREAKTHROUGH;case ADVANCE:return Kind.ADVANCE;default:return Kind.NONE;}
    }
    private static Hex step(Hex h,int q,int r){return new Hex(h.q+q,h.r+r);}
    private boolean affects(int source,int owner){return source==owner||w.campaign.hostile(source,owner);}
    private boolean trigger(War.Structure s,World.Unit source){return s!=null&&s.complete&&w.fieldworks.trap(s.kind)&&affects(source.owner,s.owner);}
    private String terrainError(World.Unit u,Hex from,Hex to){
        if(to==null||!w.inside(to))return "地图边缘或不存在的地块";
        if(w.army.water(from)!=w.army.water(to))return "水陆边界，强制位移不换乘";
        if(w.army.moveCost(u,from,to)<1)return "该部队不能进入"+w.terrain[to.q][to.r]+"（检查难所行军与部队通行能力）";
        return null;
    }
    private String stepError(World.Unit mover,Hex from,Hex to,int ignoredUnit,boolean traps,World.Unit source){
        String error=terrainError(mover,from,to);if(error!=null)return error;
        World.Unit other=w.unitAt(to);if(other!=null&&other.id!=ignoredUnit&&other.id!=mover.id)return "被"+w.officer(other.officerId).name+"部队占据（"+w.campaign.relationLabel(source.owner,other.owner)+"）";
        World.City c=w.cityAt(to);if(c!=null)return "被"+c.name+"据点占据";
        Domestic.Facility f=w.domestic.at(to);if(f!=null)return "被"+f.kind.label+"占据";
        War.Structure s=w.war.at(to);if(s!=null&&!(traps&&trigger(s,source)))return "被"+s.kind.label+(affects(source.owner,s.owner)?"占据":"占据，受协定保护");
        return null;
    }
    String requiredError(World.Unit a,World.Unit b,Kind kind){
        int dq=b.hex.q-a.hex.q,dr=b.hex.r-a.hex.r;
        if(kind==Kind.HOOK){String error=stepError(a,a.hex,step(a.hex,-dq,-dr),-1,false,a);return error==null?null:"熊手：己方退路"+error;}
        if(kind==Kind.BREAKTHROUGH){String error=stepError(a,b.hex,step(b.hex,dq,dr),-1,false,a);return error==null?null:"突破：敌军身后落点"+error;}
        return null;
    }
    Preview preview(World.Unit a,World.Unit b,Kind kind,String error,String heading){
        List<Hex> ap=new ArrayList<>(),bp=new ArrayList<>(),risks=new ArrayList<>();Hex blocked=null;boolean friendly=false;
        if(a!=null)ap.add(a.hex);if(b!=null)bp.add(b.hex);
        StringBuilder text=new StringBuilder(heading);
        if(error!=null)return new Preview(error,text+"\n不能发动："+error,ap,bp,risks,null,false);
        if(a==null||b==null||kind==Kind.NONE)return new Preview(null,text.toString(),ap,bp,risks,null,false);
        Hex ah=a.hex,bh=b.hex;int dq=bh.q-ah.q,dr=bh.r-ah.r;
        text.append("\n以下位置以命中且目标存活为条件：");
        if(kind==Kind.HOOK){
            ap.add(step(ah,-dq,-dr));String stop=stepError(b,bh,ah,a.id,false,a);
            if(stop==null)bp.add(ah);else{blocked=ah;text.append("\n目标拉动停止：").append(stop);}
        }else if(kind==Kind.BREAKTHROUGH)ap.add(step(bh,dq,dr));
        else for(int n=0;n<kind.steps;n++){
            Hex next=step(bh,dq,dr);String stop=stepError(b,bh,next,a.id,true,a);
            if(stop!=null){
                blocked=next;text.append("\n后方受阻：").append(stop).append("；主伤害仍结算，位移在此停止");
                if(kind.collision){text.append("；部队碰撞沿用现行工程参数");World.Unit hit=w.unitAt(next);if(hit!=null&&hit.owner==a.owner){friendly=true;risks.add(next);}}
                if(w.cityAt(next)!=null||w.domestic.at(next)!=null||w.war.at(next)!=null)text.append("。设施/据点碰撞损伤待核，本版不额外扣耐久");
                break;
            }
            bp.add(next);
            if(kind.follow){String stopFollow=stepError(a,ah,bh,b.id,false,a);if(stopFollow==null){ap.add(bh);ah=bh;}else text.append("\n己方无法跟进：").append(stopFollow);}
            bh=next;
            War.Structure trap=w.war.at(next);
            if(trigger(trap,a)){
                risks.addAll(w.fieldworks.ignitionArea(next,a.owner));text.append("\n可能触发").append(trap.kind.label).append("及连锁；生死和后续落点在执行时逐格重验");break;
            }
        }
        for(Hex h:ap)if(!h.equals(a.hex)&&w.war.fireAt(h)!=null){risks.add(h);friendly=true;text.append("\n己方落点有火场，存活至旬结算时可能受火伤");}
        for(Hex h:bp)if(!h.equals(b.hex)&&w.war.fireAt(h)!=null){risks.add(h);text.append("\n目标落点有火场，存活至旬结算时可能受火伤");}
        for(World.Unit u:w.fieldUnits())if(u.owner==a.owner&&risks.contains(u.hex))friendly=true;
        for(Hex h:ap)if(!h.equals(a.hex)&&risks.contains(h))friendly=true;
        if(friendly)text.append("\n警示：可能波及己方部队/运输，或己方落点危险");
        text.append("\n己方 ").append(ap).append("；目标 ").append(bp);
        if(kind.follow)text.append("\n主伤害击破时仅检查目标原格跟进；不会额外获得行动。");
        return new Preview(null,text.toString(),ap,bp,risks,blocked,friendly);
    }
    private boolean present(World.Unit u){return u!=null&&w.unit(u.id)==u;}
    private void move(World.Unit u,Hex to){
        if(!present(u))return;Hex from=u.hex;u.hex=to;
        if(u instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)u;m.legacyOverlap=false;m.waiting="受战法位移，路线将从当前位置重算";}
        // The existing travel hazards execute once per actual step. Fire tiles tick on the normal world clock.
        w.fieldworks.traveled(u,Arrays.asList(from,to));
    }
    void execute(World.Unit a,World.Unit b,Hex origin,Hex target,Kind kind){
        if(kind==Kind.NONE)return;
        int dq=target.q-origin.q,dr=target.r-origin.r,moved=0,followed=0;String stop="完成";
        if(!present(a))return;
        if(kind==Kind.HOOK){
            Hex retreat=step(origin,-dq,-dr);String error=stepError(a,a.hex,retreat,-1,false,a);
            if(error==null){move(a,retreat);followed++;if(present(b)){error=stepError(b,b.hex,origin,-1,false,a);if(error==null){move(b,origin);moved++;}}}
            if(error!=null)stop=error;
        }else if(kind==Kind.BREAKTHROUGH){
            Hex behind=step(target,dq,dr);String error=stepError(a,target,behind,-1,false,a);
            if(error==null){move(a,behind);followed++;}else stop=error;
        }else if(!present(b)){
            stop="目标被主伤害击破";
            if(kind.follow&&stepError(a,a.hex,target,-1,false,a)==null){move(a,target);followed++;}
        }else for(int n=0;n<kind.steps&&present(b);n++){
            Hex old=b.hex,next=step(old,dq,dr);String error=stepError(b,old,next,-1,true,a);
            if(error!=null){
                stop=error;
                if(kind.collision){w.war.collision(b,a);World.Unit obstacle=w.unitAt(next);if(obstacle!=null&&obstacle!=b&&affects(a.owner,obstacle.owner))w.war.collision(obstacle,a);}
                break;
            }
            War.Structure trap=w.war.at(next);
            // Remove/detonate a trap before occupying its cell, then revalidate every identity and cell.
            // The target receives the center blast after moving, so Fieldworks performs that move atomically.
            if(trigger(trap,a)){
                w.fieldworks.displaceIntoTrap(b,next,a);moved++;
            }else{move(b,next);moved++;}
            if(kind.follow&&present(a)){
                String following=stepError(a,a.hex,old,-1,false,a);
                if(following==null){move(a,old);followed++;}else stop="己方跟进停止："+following;
            }
            if(!present(b)){stop="目标在位移连锁中被击破";break;}
            if(!present(a)){stop="施放部队在连锁中被击破";break;}
        }
        w.battleOutcome("实际位移：目标"+moved+"格、己方"+followed+"格；"+stop);
    }
}
