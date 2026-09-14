package game.sanguo.core;

import java.util.*;

/** Pausable 50-exchange duel. Structural rules: official PC manual pp.28-29.
 * Damage, critical/arrival probabilities and buff durations are explicit engineering parameters. */
public final class Duel {
    public enum Stance {
        ATTACK("重视攻击"), DEFEND("重视防御"), SPIRIT("重视斗志"), FINISHER("重视一击");
        public final String label; Stance(String label){this.label=label;}
    }
    public enum Move {
        EXCHANGE("交锋",0), SPECIAL("必杀技",100), POWER("集气",100), GUARD("坚守",100),
        RETREAT("退却",100), VITAL("急所",200), MUSOU("无双",300),
        HIDDEN("暗器",0), FEIGN("伪退却",0), SWAP("换将",0);
        public final String label; public final int cost;
        Move(String label,int cost){this.label=label;this.cost=cost;}
    }
    public static final class Fighter {
        final int officer;
        int hp,spirit,attackBuff,guardBuff,invulnerable,streak,wounds;
        Stance stance=Stance.SPIRIT;
        boolean joined,hiddenUsed,feignUsed;
        Fighter(int officer,int hp){this.officer=officer;this.hp=hp;}
        public int officerId(){return officer;} public int hp(){return hp;} public int spirit(){return spirit;}
        public int wounds(){return wounds;} public boolean joined(){return joined;}
        public Stance stance(){return stance;}
        public String effects(){return (attackBuff>0?"集气 "+attackBuff+" ":"")+(guardBuff>0?"坚守 "+guardBuff+" ":"")+(invulnerable>0?"完全防御 "+invulnerable:"");}
    }
    final List<Fighter> left=new ArrayList<>(),right=new ArrayList<>();
    int leftIndex,rightIndex,round,winner=-2,escaped=-1;
    String report="双方准备交锋";
    Duel(){}
    Duel(World w,World.Unit a,World.Unit b){
        for(World.Officer o:w.army.crew(a))left.add(new Fighter(o.id,Math.max(40,100-w.contests.injury(o.id)*20)));
        for(World.Officer o:w.army.crew(b))right.add(new Fighter(o.id,Math.max(40,100-w.contests.injury(o.id)*20)));
        left.get(0).joined=true;right.get(0).joined=true;
    }
    public int round(){return round;} public int winner(){return winner;} public int escaped(){return escaped;}
    public String report(){return report;}
    public Fighter active(int side){return team(side).get(side==0?leftIndex:rightIndex);}
    public List<Fighter> fighters(int side){return Collections.unmodifiableList(team(side));}
    List<Fighter> team(int side){return side==0?left:right;}
    String error(World w,int side,Move move,int replacement){
        if(winner!=-2)return "单挑已经结束";
        if(move==null)return "请选择单挑指令";
        Fighter f=active(side);Contests.Profile p=w.contests.profile(f.officer);
        if(f.spirit<move.cost)return "斗志不足";
        if(move==Move.HIDDEN&&(!p.has(Contests.Gear.HIDDEN)||f.hiddenUsed))return "需要暗器且每场只能使用一次";
        if(move==Move.FEIGN&&(!p.has(Contests.Gear.BOW)||f.feignUsed||round<15))return "需要弓、完成15合且每场只能使用一次";
        if(move==Move.SWAP&&(replacement<0||replacement>=team(side).size()||team(side).get(replacement)==f||!team(side).get(replacement).joined||team(side).get(replacement).hp<=0))return "请选择已赶来支援的其他武将";
        return null;
    }
    public String moveError(World w,Move move,int replacement){return error(w,0,move,replacement);}
    void step(World w,Stance stance,Move move,int replacement){
        StringBuilder text=new StringBuilder();
        Fighter ai=active(1);
        Stance aiStance=ai.hp<35?Stance.DEFEND:ai.spirit<100?Stance.SPIRIT:Stance.ATTACK;
        Move aiMove=Move.EXCHANGE;int aiSwap=-1;
        if(ai.hp<35)for(int i=0;i<right.size();i++)if(i!=rightIndex&&right.get(i).joined&&right.get(i).hp>ai.hp+20){aiMove=Move.SWAP;aiSwap=i;break;}
        if(aiMove==Move.EXCHANGE){
            if(ai.spirit>=300)aiMove=Move.MUSOU;
            else if(ai.spirit>=100)aiMove=Move.SPECIAL;
            else if(error(w,1,Move.HIDDEN,-1)==null)aiMove=Move.HIDDEN;
            else if(error(w,1,Move.FEIGN,-1)==null)aiMove=Move.FEIGN;
        }
        prepare(active(0),stance);prepare(ai,aiStance);
        if(move==Move.SWAP){leftIndex=replacement;text.append(name(w,active(0))).append("替换上阵；");}
        if(aiMove==Move.SWAP){rightIndex=aiSwap;text.append(name(w,active(1))).append("替换上阵；");}
        // Choices are committed before initiative; a swap consumes this exchange, without resetting health.
        int first=w.contests.war(w.officer(active(0).officer))+w.strategy.nextInt(21)>=w.contests.war(w.officer(active(1).officer))+w.strategy.nextInt(21)?0:1;
        act(w,first,first==0?move:aiMove,text);
        if(winner==-2)act(w,1-first,first==0?aiMove:move,text);
        round++;
        if(winner==-2){
            for(int side=0;side<2;side++){
                Fighter f=active(side);
                if(f.attackBuff>0)f.attackBuff--;if(f.guardBuff>0)f.guardBuff--;if(f.invulnerable>0)f.invulnerable--;
                if(f.hp<=65)for(Fighter reserve:team(side))if(!reserve.joined&&w.strategy.nextInt(100)<40){reserve.joined=true;text.append(name(w,reserve)).append("赶来支援；");}
            }
            if(round>=50){winner=-1;text.append("五十合已过，双方平手。");}
        }
        report="第"+round+"合："+text;
    }
    private void prepare(Fighter f,Stance stance){f.streak=f.stance==stance?Math.min(50,f.streak+1):1;f.stance=stance;}
    private String name(World w,Fighter f){return w.officer(f.officer).name;}
    private void act(World w,int side,Move move,StringBuilder text){
        Fighter f=active(side),enemy=active(1-side);if(move==Move.SWAP)return;
        f.spirit-=move.cost;
        if(move==Move.POWER){f.attackBuff=6;text.append(name(w,f)).append("集气；");return;}
        if(move==Move.GUARD){f.guardBuff=6;text.append(name(w,f)).append("坚守；");return;}
        if(move==Move.RETREAT){
            boolean escape=w.contests.profile(f.officer).has(Contests.Gear.HORSE)||w.strategy.nextInt(100)<60;
            text.append(name(w,f)).append(escape?"成功退却；":"退却被阻；");
            if(escape){escaped=side;winner=1-side;}return;
        }
        int strength=Math.max(0,w.contests.war(w.officer(f.officer))-f.wounds*10);
        int enemyStrength=Math.max(0,w.contests.war(w.officer(enemy.officer))-enemy.wounds*10);
        int damage=Math.max(2,7+(strength-enemyStrength)/8+w.strategy.nextInt(5));
        Contests.Profile gear=w.contests.profile(f.officer);
        if(gear.has(Contests.Gear.POLEARM))damage+=2;
        if(move==Move.EXCHANGE){
            gain(f,f.stance==Stance.SPIRIT?24:f.stance==Stance.ATTACK?7:14);
            if(gear.has(Contests.Gear.SWORD))gain(f,10);
            boolean critical=f.streak>=3&&w.strategy.nextInt(100)<12;
            if(f.stance==Stance.ATTACK)damage=damage*3/2;
            if(f.stance==Stance.DEFEND)damage=Math.max(1,damage/2);
            if(f.stance==Stance.FINISHER)damage=w.strategy.nextInt(100)<12?damage*4:0;
            if(critical)switch(f.stance){
                case ATTACK:damage*=2;text.append("连续攻击！");break;
                case DEFEND:f.invulnerable=3;text.append("完全防御！");break;
                case SPIRIT:gain(f,100);text.append("斗志激增！");break;
                case FINISHER:damage=Math.max(20,damage);break;
            }
            if(enemy.invulnerable>0)damage=0;
            else if(enemy.stance==Stance.DEFEND){damage=damage*2/3;if(w.strategy.nextInt(100)<30){damage=0;gain(enemy,12);}}
            else if(enemy.stance==Stance.ATTACK)damage=damage*5/4;
        }else{
            if(move==Move.SPECIAL||move==Move.VITAL)damage*=3;
            if(move==Move.MUSOU){damage*=5;enemy.attackBuff=0;enemy.guardBuff=0;enemy.invulnerable=0;}
            if(move==Move.HIDDEN){damage*=2;f.hiddenUsed=true;enemy.attackBuff=0;enemy.guardBuff=0;enemy.invulnerable=0;}
            if(move==Move.FEIGN){damage*=2;f.feignUsed=true;}
            if(move==Move.VITAL||move==Move.FEIGN){enemy.wounds=Math.min(3,enemy.wounds+1);text.append(name(w,enemy)).append("负伤；");}
        }
        if(f.attackBuff>0)damage=damage*3/2;
        if(enemy.guardBuff>0)damage=damage*2/3;
        damage=Math.min(enemy.hp,damage);enemy.hp-=damage;if(damage>0)gain(enemy,18);
        text.append(name(w,f)).append(move.label).append("，").append(name(w,enemy)).append("体力−").append(damage).append("；");
        // The current combatant falling ends the duel, even if reserves still have health (manual p.28).
        if(enemy.hp==0){winner=side;text.append(name(w,enemy)).append("落败。");}
    }
    private void gain(Fighter f,int value){f.spirit=Math.min(300,f.spirit+value);}
}
