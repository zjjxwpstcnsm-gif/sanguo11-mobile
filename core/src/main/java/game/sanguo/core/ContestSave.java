package game.sanguo.core;

import java.io.*;
import java.util.*;

/** v9 extension; all old payload fields are unchanged. Counts and references are validated before use. */
final class ContestSave {
    private static final int MARKER=0x43545339;
    static void write(World w,DataOutputStream d)throws IOException{
        Contests c=w.contests;d.writeInt(MARKER);d.writeInt(c.nextId);d.writeUTF(c.lastResult);
        d.writeInt(c.profiles.size());for(Map.Entry<Integer,Contests.Profile> e:c.profiles.entrySet()){
            d.writeInt(e.getKey());d.writeByte(e.getValue().temper.ordinal());d.writeByte(e.getValue().talkMask);d.writeByte(e.getValue().gearMask);
        }
        d.writeInt(c.injuries.size());for(Map.Entry<Integer,Contests.Injury> e:c.injuries.entrySet()){d.writeInt(e.getKey());d.writeByte(e.getValue().severity);d.writeInt(e.getValue().until);}
        Contests.Session s=c.session;d.writeBoolean(s!=null);if(s==null)return;
        d.writeInt(s.id);d.writeInt(s.owner);d.writeInt(s.turn);d.writeInt(s.leftRef);d.writeInt(s.rightRef);d.writeInt(s.city);d.writeInt(s.revision);d.writeBoolean(s.isDuel());
        if(s.isDuel()){
            Duel duel=s.duel;d.writeInt(duel.round);d.writeInt(duel.winner);d.writeInt(duel.escaped);d.writeInt(duel.leftIndex);d.writeInt(duel.rightIndex);d.writeUTF(duel.report);
            for(int side=0;side<2;side++){d.writeInt(duel.team(side).size());for(Duel.Fighter f:duel.team(side)){
                d.writeInt(f.officer);d.writeInt(f.hp);d.writeInt(f.spirit);d.writeInt(f.attackBuff);d.writeInt(f.guardBuff);d.writeInt(f.invulnerable);d.writeInt(f.streak);d.writeInt(f.wounds);d.writeByte(f.stance.ordinal());d.writeBoolean(f.joined);d.writeBoolean(f.hiddenUsed);d.writeBoolean(f.feignUsed);
            }}
        }else{
            Debate b=s.debate;d.writeInt(b.round);d.writeInt(b.winner);d.writeInt(b.leader);d.writeInt(b.aiCard);d.writeByte(b.topic.ordinal());d.writeUTF(b.report);
            for(int side=0;side<2;side++){Debate.Speaker p=b.speaker(side);d.writeInt(p.officer);d.writeByte(p.temper.ordinal());d.writeInt(p.hp);d.writeInt(p.anger);d.writeInt(p.fury);d.writeInt(p.stage);d.writeBoolean(p.rethink);d.writeInt(p.hand.size());for(Debate.Card card:p.hand){d.writeByte(card.topic==null?-1:card.topic.ordinal());d.writeByte(card.size);d.writeByte(card.talk==null?-1:card.talk.ordinal());}}
        }
    }
    static void read(World w,DataInputStream d)throws IOException{
        require(d.readInt()==MARKER,"对局扩展标记错误");Contests c=w.contests;c.nextId=d.readInt();c.lastResult=d.readUTF();
        int n=range(d.readInt(),0,w.officers.size());for(int i=0;i<n;i++){
            int id=d.readInt();Contests.Profile p=new Contests.Profile(Debate.Temper.values()[range(d.readUnsignedByte(),0,3)],range(d.readUnsignedByte(),0,31),range(d.readUnsignedByte(),0,63));
            require(c.profiles.put(id,p)==null,"武将对局配置重复");
        }
        n=range(d.readInt(),0,w.officers.size());for(int i=0;i<n;i++){int id=d.readInt();Contests.Injury injury=new Contests.Injury(d.readUnsignedByte(),d.readInt());require(c.injuries.put(id,injury)==null,"伤病记录重复");}
        if(!d.readBoolean())return;
        Contests.Session s=new Contests.Session(d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt());s.revision=d.readInt();c.session=s;
        if(d.readBoolean()){
            Duel duel=new Duel();s.duel=duel;duel.round=d.readInt();duel.winner=d.readInt();duel.escaped=d.readInt();duel.leftIndex=d.readInt();duel.rightIndex=d.readInt();duel.report=d.readUTF();
            for(int side=0;side<2;side++){n=range(d.readInt(),1,3);for(int i=0;i<n;i++){
                Duel.Fighter f=new Duel.Fighter(d.readInt(),d.readInt());f.spirit=d.readInt();f.attackBuff=d.readInt();f.guardBuff=d.readInt();f.invulnerable=d.readInt();f.streak=d.readInt();f.wounds=d.readInt();f.stance=Duel.Stance.values()[range(d.readUnsignedByte(),0,3)];f.joined=d.readBoolean();f.hiddenUsed=d.readBoolean();f.feignUsed=d.readBoolean();duel.team(side).add(f);
            }}
        }else{
            Debate b=new Debate();s.debate=b;b.round=d.readInt();b.winner=d.readInt();b.leader=d.readInt();b.aiCard=d.readInt();b.topic=Debate.Topic.values()[range(d.readUnsignedByte(),0,2)];b.report=d.readUTF();
            for(int side=0;side<2;side++){
                Debate.Speaker p=new Debate.Speaker(d.readInt(),Debate.Temper.values()[range(d.readUnsignedByte(),0,3)]);if(side==0)b.left=p;else b.right=p;
                p.hp=d.readInt();p.anger=d.readInt();p.fury=d.readInt();p.stage=d.readInt();p.rethink=d.readBoolean();n=range(d.readInt(),0,6);
                for(int i=0;i<n;i++){int topic=range(d.readByte(),-1,2),size=range(d.readUnsignedByte(),0,3),talk=range(d.readByte(),-1,4);require((topic>=0&&size>=1&&talk==-1)||(topic==-1&&size==0&&talk>=0),"手牌组合无效");p.hand.add(new Debate.Card(topic<0?null:Debate.Topic.values()[topic],size,talk<0?null:Debate.Talk.values()[talk]));}
            }
        }
    }
    static void validate(World w)throws IOException{
        Contests c=w.contests;range(c.nextId,1,10000000);text(c.lastResult,2000,true);
        for(Map.Entry<Integer,Contests.Profile> e:c.profiles.entrySet())require(w.officer(e.getKey())!=null&&e.getValue()!=null,"对局配置引用无效");
        for(Map.Entry<Integer,Contests.Injury> e:c.injuries.entrySet()){require(w.officer(e.getKey())!=null,"伤病武将不存在");range(e.getValue().severity,1,3);require(e.getValue().until>w.turn&&e.getValue().until<=w.turn+3,"伤病恢复时间无效");}
        Contests.Session s=c.session;if(s==null)return;
        range(s.id,1,c.nextId-1);range(s.revision,0,250);require(s.owner==w.player&&s.owner==w.active&&s.turn==w.turn&&!w.gameOver(),"对局势力或时序无效");
        require((s.duel==null)!=(s.debate==null),"对局类型无效");
        if(s.isDuel()){
            Duel b=s.duel;range(b.round,0,49);require(b.winner==-2&&b.escaped==-1&&s.city==-1&&s.revision==b.round,"单挑阶段无效");text(b.report,2000,false);
            World.Unit a=w.unit(s.leftRef),enemy=w.unit(s.rightRef);require(a!=null&&enemy!=null&&a.owner==s.owner&&w.campaign.hostile(a.owner,enemy.owner)&&a.hex.distance(enemy.hex)==1&&a.acted&&enemy.acted,"单挑部队引用无效");
            require(a.status==War.Status.NORMAL&&enemy.status==War.Status.NORMAL&&!w.army.water(a.hex)&&!w.army.water(enemy.hex)&&!Army.siegeWeapon(a.weapon)&&!Army.siegeWeapon(enemy.weapon),"单挑场地无效");
            for(int side=0;side<2;side++){
                List<Duel.Fighter> team=b.team(side);World.Unit u=side==0?a:enemy;List<World.Officer> crew=w.army.crew(u);require(team.size()==crew.size(),"单挑编队人数不同");
                range(side==0?b.leftIndex:b.rightIndex,0,team.size()-1);Set<Integer> ids=new HashSet<>();
                for(int i=0;i<team.size();i++){
                    Duel.Fighter f=team.get(i);require(ids.add(f.officer)&&f.officer==crew.get(i).id,"单挑武将引用不同");
                    range(f.hp,1,100);range(f.spirit,0,300);range(f.attackBuff,0,6);range(f.guardBuff,0,6);range(f.invulnerable,0,3);range(f.streak,0,50);range(f.wounds,0,3);require(f.stance!=null,"单挑方针缺失");
                    require(!f.hiddenUsed||c.profile(f.officer).has(Contests.Gear.HIDDEN),"暗器使用记录无携物");require(!f.feignUsed||c.profile(f.officer).has(Contests.Gear.BOW)&&b.round>=16,"伪退使用记录无效");
                }
                require(b.active(side).joined&&team.get(0).joined,"当前武将未参战");
            }
        }else{
            Debate b=s.debate;range(b.round,0,100);range(b.winner,-2,1);range(b.leader,0,1);text(b.report,2000,false);require(b.topic!=null,"话题缺失");
            require(s.revision>=b.round&&s.revision<=b.round*2+1,"舌战操作序号无效");
            World.Officer a=w.officer(s.leftRef),target=w.officer(s.rightRef);World.City city=w.city(s.city);
            require(city!=null&&city.owner==s.owner&&a!=null&&a.owner==s.owner&&a.cityId==city.id&&a.unitId==-1&&a.acted&&target!=null&&target.owner!=s.owner&&target.unitId==-1&&target.cityId>=0&&target.acted,"舌战人物引用无效");
            require(!w.government.captive(a.id)&&!w.government.captive(target.id)&&!w.domestic.busy(a.id)&&!w.domestic.busy(target.id)&&!w.strategy.busy(a.id)&&!w.strategy.busy(target.id),"舌战人物任务冲突");
            if(s.diplomatic())require(s.foreign>=0&&s.foreign<w.factions.length&&s.foreign!=s.owner&&target.owner==s.foreign&&w.campaign.treaty(s.owner,s.foreign)==null&&(s.duration==3||s.duration==6||s.duration==12)&&w.skills.has(a,Skill.LUNKE),"外交舌战引用无效");
            else {require(target.role!=Strategy.Role.RULER,"不能舌战登用君主");require(target.owner<0?target.cityId==s.city:target.loyalty<=Strategy.MAX_ENEMY_LOYALTY&&city.hex.distance(w.city(target.cityId).hex)<=Strategy.RECRUIT_RANGE,"舌战登用目标超出范围");}
            for(int side=0;side<2;side++){
                Debate.Speaker p=b.speaker(side);require(p!=null&&p.officer==(side==0?s.leftRef:s.rightRef)&&p.temper==c.profile(p.officer).temper,"论辩性格引用无效");
                range(p.hp,0,100);range(p.anger,0,b.winner==-2?99:200);range(p.fury,0,3);range(p.stage,0,4);require(p.stage==(100-p.hp)/25,"心理台阶无效");
                int size=Math.min(6,3+w.officer(p.officer).intelligence/30);require(p.hand.size()<=size&&(b.winner!=-2||p.hand.size()==size),"手牌数量无效");
                boolean topic=false;for(Debate.Card card:p.hand){require(card!=null,"手牌为空");if(card.talk==null){require(card.topic!=null&&card.size>=1&&card.size<=3,"话题牌无效");topic=true;}else require(card.topic==null&&card.size==0&&(c.profile(p.officer).has(Contests.Gear.BOOK)||(c.profile(p.officer).talkMask&(1<<card.talk.ordinal()))!=0),"未掌握的话术牌");}
                require(b.winner!=-2||topic,"手牌缺少可用话题牌");
            }
            if(b.winner==-2){require(b.round<100&&b.left.hp>0&&b.right.hp>0,"舌战未结算结束状态");range(b.aiCard,0,b.right.hand.size()-1);require(b.cardError(b.right,b.right.hand.get(b.aiCard))==null,"电脑预选手牌不可用");}
            else if(b.winner==0)require(b.right.hp==0&&b.left.hp>0,"舌战胜负与心理不符");
            else if(b.winner==1)require(b.left.hp==0&&b.right.hp>0,"舌战胜负与心理不符");
            else require(b.round==100||b.left.hp==0&&b.right.hp==0,"舌战平手状态无效");
        }
    }
    private static int range(int n,int min,int max)throws IOException{require(n>=min&&n<=max,"对局字段越界");return n;}
    private static void text(String s,int max,boolean empty)throws IOException{require(s!=null&&s.length()<=max&&(empty||!s.isEmpty()),"对局文本无效");}
    private static void require(boolean ok,String message)throws IOException{if(!ok)throw new IOException(message);}
}
