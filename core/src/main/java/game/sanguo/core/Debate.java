package game.sanguo.core;

import java.util.*;

/** Hand/topic/anger state machine based on the official manual pp.34-35.
 * Deck weights, damage, hand thresholds, fury duration and round cap are engineering values. */
public final class Debate {
    public enum Temper {
        CALM("冷静"), BOLD("刚胆"), TIMID("小心"), RASH("莽撞");
        public final String label;Temper(String label){this.label=label;}
    }
    public enum Topic {
        REASON("道理"), HISTORY("故事"), TIMING("时节");
        public final String label;Topic(String label){this.label=label;}
    }
    public enum Talk {
        IGNORE("无视"), SHOUT("大喝"), GUILE("诡辩"), CALM("镇静"), RAGE("逆上");
        public final String label;Talk(String label){this.label=label;}
    }
    public static final class Card {
        public final Topic topic;public final int size;public final Talk talk;
        Card(Topic topic,int size,Talk talk){this.topic=topic;this.size=size;this.talk=talk;}
        public String label(){return talk!=null?talk.label:topic.label+"·"+new String[]{"","小","中","大"}[size];}
        int power(){return talk==null?size:talk==Talk.SHOUT?4:talk==Talk.GUILE?3:0;}
    }
    public static final class Speaker {
        final int officer;final Temper temper;
        final List<Card> hand=new ArrayList<>();
        int hp=100,anger,fury,stage;
        boolean rethink=true;
        Speaker(int officer,Temper temper){this.officer=officer;this.temper=temper;}
        public int officerId(){return officer;} public Temper temper(){return temper;}
        public int hp(){return hp;} public int anger(){return anger;} public int fury(){return fury;}
        public boolean canRethink(){return rethink;}
        public List<Card> hand(){return Collections.unmodifiableList(hand);}
    }
    Speaker left,right;
    Topic topic=Topic.REASON;
    int round,leader,aiCard,winner=-2;
    String report="选择手牌开始辩论";
    Debate(){}
    Debate(World w,int actor,int target){
        left=new Speaker(actor,w.contests.profile(actor).temper);right=new Speaker(target,w.contests.profile(target).temper);
        refill(w,left);refill(w,right);topic=Topic.values()[w.strategy.nextInt(3)];
        leader=w.officer(actor).intelligence>=w.officer(target).intelligence?0:1;commitAi(w);
    }
    public Speaker speaker(int side){return side==0?left:right;}
    public Topic topic(){return topic;} public int round(){return round;} public int winner(){return winner;}
    public int leader(){return leader;} public String report(){return report;}
    public String opponentOpening(){if(winner!=-2)return "论辩结束";return leader==1?(right.hand.get(aiCard).talk==null?"对方先手：话题牌":"对方先手：话术牌"):"我方先手";}
    boolean sealed(Speaker s){Speaker other=s==left?right:left;return other.fury>0&&other.temper==Temper.CALM;}
    String error(int index){
        if(winner!=-2)return "舌战已经结束";
        if(index<0||index>=left.hand.size())return "手牌已变化，请重新选择";
        return cardError(left,left.hand.get(index));
    }
    String cardError(Speaker s,Card card){
        if(card.talk!=null&&sealed(s))return "对方冷静憤激中，话术被封印";
        if(s.fury>0&&(card.talk==Talk.CALM||card.talk==Talk.RAGE))return "憤激中不能使用镇静或逆上";
        return null;
    }
    public String cardError(int index){return error(index);}
    void rethink(World w){
        left.hand.clear();refill(w,left);left.rethink=false;
        report="已重新思考并换掉全部手牌；对方已选手牌不变。";
    }
    private int capacity(World w,Speaker s){return Math.min(6,3+w.officer(s.officer).intelligence/30);}
    private void refill(World w,Speaker s){
        int mask=w.contests.profile(s.officer).talkMask;
        if(w.contests.profile(s.officer).has(Contests.Gear.BOOK))mask=31;
        List<Talk> allowed=new ArrayList<>();for(Talk t:Talk.values())if((mask&(1<<t.ordinal()))!=0)allowed.add(t);
        while(s.hand.size()<capacity(w,s)){
            // Always keep a legal topic option; a full special hand must not deadlock under a seal/fury.
            boolean forceTopic=s.hand.size()==capacity(w,s)-1&&s.hand.stream().noneMatch(c->c.talk==null);
            if(!forceTopic&&!allowed.isEmpty()&&w.strategy.nextInt(100)<30)s.hand.add(new Card(null,0,allowed.get(w.strategy.nextInt(allowed.size()))));
            else s.hand.add(new Card(Topic.values()[w.strategy.nextInt(3)],1+w.strategy.nextInt(3),null));
        }
    }
    private void commitAi(World w){
        int best=Integer.MIN_VALUE;aiCard=0;
        for(int i=0;i<right.hand.size();i++){
            Card c=right.hand.get(i);if(cardError(right,c)!=null)continue;
            int score=c.talk==null?c.size*4+(c.topic==topic?18:0):c.talk==Talk.SHOUT?32:c.talk==Talk.GUILE?24:c.talk==Talk.IGNORE?(left.anger>65?8:28):c.talk==Talk.CALM?left.anger/3:right.anger/3;
            score+=w.strategy.nextInt(5);if(score>best){best=score;aiCard=i;}
        }
    }
    /** Positive: a wins. Different off-topic topics tie; special ordering includes post-hit calm/rage. */
    static int compare(Card a,Card b,Topic topic){
        int ar=rank(a),br=rank(b);if(ar!=br)return Integer.compare(ar,br);
        if(a.talk!=null)return 0;
        if(a.topic==b.topic)return Integer.compare(a.size,b.size);
        if(a.topic==topic)return 1;if(b.topic==topic)return -1;return 0;
    }
    private static int rank(Card c){return c.talk==null?2:c.talk==Talk.IGNORE?5:c.talk==Talk.SHOUT?4:c.talk==Talk.GUILE?3:c.talk==Talk.CALM?1:0;}
    void play(World w,int index){
        StringBuilder text=new StringBuilder();
        Card a=left.hand.remove(index),b=right.hand.remove(aiCard);
        text.append(a.label()).append(" 对 ").append(b.label()).append("；");
        int result=compare(a,b,topic);
        if(left.fury>0&&left.temper==Temper.BOLD&&b.talk!=Talk.IGNORE&&b.talk!=Talk.SHOUT)result=1;
        if(right.fury>0&&right.temper==Temper.BOLD&&a.talk!=Talk.IGNORE&&a.talk!=Talk.SHOUT)result=-1;
        if(left.fury>0&&right.fury>0&&left.temper==Temper.BOLD&&right.temper==Temper.BOLD)result=compare(a,b,topic);
        if(result==0){left.anger+=10;right.anger+=10;text.append("本合平手；");}
        else{
            Speaker victor=result>0?left:right,loser=result>0?right:left;
            Card win=result>0?a:b,lose=result>0?b:a;leader=result>0?0:1;
            if(win.talk==Talk.IGNORE){loser.anger+=45;text.append("无视令对手怒气上升；");}
            else if(win.talk!=Talk.CALM&&win.talk!=Talk.RAGE){
                int power=win.talk==Talk.GUILE?lose.power():win.power();
                if(power>0)hit(w,victor,loser,power,text);
                if(win.talk==Talk.SHOUT)loser.anger+=10;
                if(win.talk==Talk.GUILE){topic=Topic.values()[(topic.ordinal()+1+w.strategy.nextInt(2))%3];text.append("话题改变；");}
                else if(win.topic!=null)topic=win.topic;
            }
        }
        // Calm/rage act after incoming damage, even when losing. Guile reflects these effects.
        post(a,left,right,result<0&&b.talk==Talk.GUILE);
        post(b,right,left,result>0&&a.talk==Talk.GUILE);
        for(Speaker s:Arrays.asList(left,right))if(s.fury>0)s.fury--;
        // A burst can fill the other speaker's meter in the same exchange. Resolve that chain now.
        for(int reactions=0;reactions<4&&left.hp>0&&right.hp>0;reactions++){
            if(left.anger>=100)enrage(w,left,right,true,text);
            else if(right.anger>=100)enrage(w,right,left,true,text);
            else break;
        }
        round++;
        if(left.hp==0||right.hp==0)winner=left.hp==right.hp?-1:left.hp>right.hp?0:1;
        else if(round>=100){winner=-1;text.append("百合未分胜负，双方结束论辩。");}
        if(winner==-2){
            for(Speaker s:Arrays.asList(left,right))if(s.fury>0&&s.temper==Temper.CALM)s.rethink=true;
            refill(w,left);refill(w,right);commitAi(w);}
        report="第"+round+"合："+text;
    }
    private void post(Card c,Speaker self,Speaker other,boolean reflected){
        if(c.talk==Talk.CALM){Speaker s=reflected?self:other;s.anger=Math.max(0,s.anger-50);}
        if(c.talk==Talk.RAGE){Speaker s=reflected?other:self;s.anger+=50;}
    }
    private void hit(World w,Speaker from,Speaker to,int power,StringBuilder text){
        int damage=Math.max(2,4+power*4+(w.officer(from.officer).intelligence-w.officer(to.officer).intelligence)/10);
        if(from.fury>0&&from.temper==Temper.CALM)damage*=2;
        damage=Math.min(to.hp,damage);to.hp-=damage;to.anger+=20;
        stage(to);text.append(w.officer(to.officer).name).append("心理−").append(damage).append("；");
    }
    private void stage(Speaker s){int next=(100-s.hp)/25;if(next>s.stage){s.stage=next;s.rethink=true;}}
    private boolean consume(Speaker s,Talk talk){
        if(s.fury>0||sealed(s))return false;
        for(int i=0;i<s.hand.size();i++)if(s.hand.get(i).talk==talk){s.hand.remove(i);return true;}return false;
    }
    private void enrage(World w,Speaker self,Speaker other,boolean counter,StringBuilder text){
        self.anger=0;
        if(counter&&consume(other,Talk.RAGE)){
            self.anger=50;other.anger=0;text.append("逆上反制！");enrage(w,other,self,false,text);return;
        }
        if(counter&&consume(other,Talk.CALM)){text.append("镇静平息憤激；");return;}
        text.append(w.officer(self.officer).name).append("憤激（").append(self.temper.label).append("）；");
        if(self.temper==Temper.CALM||self.temper==Temper.BOLD){self.fury=3;return;}
        int damage;
        if(self.temper==Temper.RASH)damage=30;
        else {damage=0;for(Iterator<Card> it=self.hand.iterator();it.hasNext();){Card c=it.next();if(c.talk==null){damage+=c.size*5;it.remove();}}}
        damage=Math.min(other.hp,damage);other.hp-=damage;other.anger+=20;stage(other);
        text.append(w.officer(other.officer).name).append("心理−").append(damage).append("；");
    }
}
