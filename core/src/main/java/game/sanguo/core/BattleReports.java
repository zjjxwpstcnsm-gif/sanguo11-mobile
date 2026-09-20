package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Authoritative result history. Independent of camera, animation playback and the short HUD log. */
public final class BattleReports {
    public static final int WINDOW=9;
    public enum Kind {
        COMBAT("战斗计谋"), MOVEMENT("行军运输"), ECONOMY("内政收支"), PERSONNEL("人员外交"), RESEARCH("研究事件"), OTHER("其他");
        public final String label; Kind(String label){this.label=label;}
    }
    public enum Scope { ALL, RELATED, INITIATED, RECEIVED }
    public static final class Entry {
        public final long id,related;
        public final int turn,actor;
        public final Kind kind;
        public final String title,detail;
        public final Hex location;
        Entry(long id,int turn,int actor,long related,Kind kind,String title,String detail,Hex location){
            this.id=id;this.turn=turn;this.actor=actor;this.related=related;this.kind=kind;this.title=title;this.detail=detail;this.location=location;
        }
        public boolean involves(int side){return side>=0&&side<32&&(related&(1L<<side))!=0;}
        public boolean matches(int side,Scope scope){
            switch(scope){case RELATED:return involves(side);case INITIATED:return actor==side;case RECEIVED:return actor!=side&&involves(side);default:return true;}
        }
    }
    private final World w;
    private final ArrayDeque<Entry> entries=new ArrayDeque<>();
    private final Map<String,State> previous=new LinkedHashMap<>();
    private final List<Change> pending=new ArrayList<>();
    private long nextId=1;
    private boolean primed,global;
    private int fixedTurn=-1,actionOwner=-1;
    private long actionRelated;
    private Hex actionLocation;
    private Kind actionKind;
    private String actionName="";
    BattleReports(World w){this.w=w;}
    public synchronized String date(int turn){int month=w.startMonth-1+Math.max(0,turn)/3;return (w.startYear+month/12)+"年"+(month%12+1)+"月"+new String[]{"上旬","中旬","下旬"}[Math.max(0,turn)%3];}
    /** Saves a parent command's deltas before a nested facility counter mutates its attacker. */
    static final class ActionContext {
        int actor;long related;Hex location;Kind kind;String name;List<Change> changes;
    }
    synchronized ActionContext beginCounter(War.Structure source,Hex target){
        prepare();ActionContext context=new ActionContext();context.actor=actionOwner;context.related=actionRelated;context.location=actionLocation;context.kind=actionKind;context.name=actionName;context.changes=takeChanges();
        facility(source,target,TurnJournal.Kind.FACILITY_COUNTER,source.kind.label+"反击");return context;
    }
    synchronized void finishCounter(ActionContext context,String result){
        try{note(result);}finally{actionOwner=context.actor;actionRelated=context.related;actionLocation=context.location;actionKind=context.kind;actionName=context.name;pending.clear();pending.addAll(context.changes);}
    }
    private List<Change> takeChanges(){List<Change> result=new ArrayList<>(pending);pending.clear();result.addAll(changes());return result;}
    private static long bit(int owner){return owner>=0&&owner<32?1L<<owner:0;}
    private int eventTurn(){return fixedTurn>=0?fixedTurn:w.turn;}
    /** Called at command entry, before any mutation. Repeated nested calls are free. */
    public synchronized void prepare(){if(!primed){previous.clear();snapshot(previous);primed=true;}}
    public synchronized void rebase(){previous.clear();snapshot(previous);primed=true;clearAction();}
    public synchronized void beginTurn(){prepare();fixedTurn=w.turn;global=false;}
    public synchronized void globalPhase(){global=true;clearAction();}
    public synchronized void nextPlayer(){checkpoint("全局结算");fixedTurn=-1;global=false;clearAction();prune();}
    public synchronized void endTurn(){fixedTurn=-1;global=false;clearAction();prune();}
    public synchronized void action(TurnJournal.Kind kind,int id,Hex target,String label){
        prepare();World.Unit source=w.unit(id);actionOwner=source==null?(global?-1:w.active):source.owner;
        actionRelated=bit(actionOwner);actionLocation=target;actionName=label;
        actionKind=kind==TurnJournal.Kind.MOVE||kind==TurnJournal.Kind.ENTER||kind==TurnJournal.Kind.DEPLOY?Kind.MOVEMENT:Kind.COMBAT;
        World.Unit unit=w.unitAt(target);World.City city=w.cityAt(target);
        if(unit!=null)actionRelated|=bit(unit.owner);if(city!=null)actionRelated|=bit(city.owner);
        for(War.Structure s:w.war.structures())if(Objects.equals(s.hex,target))actionRelated|=bit(s.owner);
        Domestic.Facility facility=w.domestic.at(target);if(facility!=null){World.City c=w.city(facility.cityId);if(c!=null)actionRelated|=bit(c.owner);}
    }
    public synchronized void facility(War.Structure source,Hex target,TurnJournal.Kind kind,String label){
        action(kind,-1,target,label);actionOwner=source.owner;actionRelated|=bit(source.owner);
    }
    public synchronized void clearAction(){actionOwner=-1;actionRelated=0;actionLocation=null;actionKind=null;actionName="";pending.clear();}
    public synchronized void note(String text){
        prepare();List<Change> changes=takeChanges();long related=actionRelated;Hex location=actionLocation;
        int actor=actionOwner>=0?actionOwner:global?-1:w.active;
        if(!global||actionOwner>=0)related|=bit(actor);
        StringBuilder details=new StringBuilder(text);Kind inferred=classify(text);
        for(Change change:changes){related|=change.related;if(location==null)location=change.location;details.append("\n").append(change.text);if(inferred==Kind.OTHER)inferred=change.kind;}
        Kind kind=actionKind==null?inferred:actionKind;
        if(!actionName.isEmpty())details.insert(0,actionName+" · "+w.faction(actor)+"\n");
        append(eventTurn(),actor,related,kind,text,details.toString(),location);
    }
    /** Captures non-command effects at rule boundaries, separately per changed entity. */
    public synchronized void checkpoint(String label){
        prepare();for(Change c:changes())append(eventTurn(),-1,c.related,classify(label)==Kind.OTHER?c.kind:classify(label),label+" · "+c.name,c.text,c.location);clearAction();
    }
    private void append(int turn,int actor,long related,Kind kind,String title,String detail,Hex location){
        entries.addLast(new Entry(nextId++,turn,actor,related,kind,title,detail,location));prune();
    }
    private void prune(){int oldest=Math.max(0,w.turn-WINDOW+1);while(!entries.isEmpty()&&entries.peekFirst().turn<oldest)entries.removeFirst();}
    public synchronized List<Entry> query(int turn,int side,Scope scope,Kind kind,String query){
        prune();String needle=query==null?"":query.trim().toLowerCase(Locale.ROOT);List<Entry> result=new ArrayList<>();
        for(Iterator<Entry> it=entries.descendingIterator();it.hasNext();){Entry e=it.next();if((turn<0||e.turn==turn)&&e.matches(side,scope)&&(kind==null||e.kind==kind)&&(needle.isEmpty()||(e.title+"\n"+e.detail).toLowerCase(Locale.ROOT).contains(needle)))result.add(e);}
        return Collections.unmodifiableList(result);
    }
    public synchronized int size(){prune();return entries.size();}
    private static Kind classify(String text){
        if(contains(text,"攻","战法","反击","计谋","火","混乱","击破","损失","逃兵"))return Kind.COMBAT;
        if(contains(text,"研究","技巧","培养","事件","灾害","丰收"))return Kind.RESEARCH;
        if(contains(text,"登用","搜索","发现","忠诚","武将","俘虏","外交","同盟","停战","继承","负伤","死亡"))return Kind.PERSONNEL;
        if(contains(text,"出征","行军","运输","入城","进驻","抵达","运抵","调动","卸货"))return Kind.MOVEMENT;
        if(contains(text,"金","粮","征兵","生产","建","开发","治安","训练","制造","拆除"))return Kind.ECONOMY;
        return Kind.OTHER;
    }
    private static boolean contains(String text,String... terms){for(String s:terms)if(text.contains(s))return true;return false;}
    private static final class State {
        final int owner;final String name;final Hex location;final Kind kind;final int[] values;final String[] labels;final String extra;
        State(int owner,String name,Hex h,Kind kind,int[] values,String[] labels,String extra){this.owner=owner;this.name=name;location=h;this.kind=kind;this.values=values;this.labels=labels;this.extra=extra;}
        boolean same(State other){return owner==other.owner&&Objects.equals(location,other.location)&&Arrays.equals(values,other.values)&&extra.equals(other.extra);}
    }
    private static final class Change {
        final String name,text;final Hex location;final Kind kind;final long related;
        Change(String name,String text,Hex location,Kind kind,long related){this.name=name;this.text=text;this.location=location;this.kind=kind;this.related=related;}
    }
    private List<Change> changes(){
        Map<String,State> next=new LinkedHashMap<>();snapshot(next);List<Change> result=new ArrayList<>();
        for(Map.Entry<String,State> item:next.entrySet()){
            State after=item.getValue(),before=previous.get(item.getKey());if(before!=null&&before.same(after))continue;
            StringBuilder text=new StringBuilder(after.name).append("〔").append(w.faction(after.owner)).append("〕：");
            if(before==null){text.append("新增 / 到场");for(int i=0;i<after.values.length;i++)if(after.values[i]!=0)text.append(" · ").append(after.labels[i]).append(' ').append(after.values[i]);if(!after.extra.isEmpty())text.append(" · ").append(after.extra);}
            else{
                if(before.owner!=after.owner)text.append("所属 ").append(w.faction(before.owner)).append(" → ").append(w.faction(after.owner)).append("；");
                if(!Objects.equals(before.location,after.location))text.append("位置 ").append(point(before.location)).append(" → ").append(point(after.location)).append("；");
                for(int i=0;i<after.values.length;i++)if(before.values[i]!=after.values[i]){long delta=(long)after.values[i]-before.values[i];text.append(after.labels[i]).append(' ').append(before.values[i]).append(" → ").append(after.values[i]).append("（").append(delta>0?"+":"").append(delta).append("）；");}
                if(!before.extra.equals(after.extra))text.append(before.extra).append(" → ").append(after.extra);
            }
            result.add(new Change(after.name,text.toString(),after.location,after.kind,bit(after.owner)|(before==null?0:bit(before.owner))));
        }
        for(Map.Entry<String,State> item:previous.entrySet())if(!next.containsKey(item.getKey())){State s=item.getValue();result.add(new Change(s.name,s.name+"〔"+w.faction(s.owner)+"〕：离场 / 消失",s.location,s.kind,bit(s.owner)));}
        previous.clear();previous.putAll(next);return result;
    }
    private static String point(Hex h){return h==null?"无":"("+h.q+","+h.r+")";}
    private static final String[] CITY={"金","粮","兵力","治安","气力","耐久","枪","戟","弩","马","剑","冲车","井阑","木兽","投石","楼船","斗舰"};
    private static final String[] UNIT={"兵力","粮","金","气力","状态剩余旬","燃烧剩余旬"};
    private static final String[] OFFICER={"统率","武力","智力","政治","魅力","忠诚","任务剩余旬"};
    private void snapshot(Map<String,State> out){
        for(World.City c:w.cities){int[] v=new int[17];v[0]=c.gold;v[1]=c.food;v[2]=c.troops;v[3]=c.order;v[4]=c.morale;v[5]=c.defense;System.arraycopy(c.equipment,0,v,6,9);System.arraycopy(c.ships,0,v,15,2);out.put("c"+c.id,new State(c.owner,c.name,c.hex,Kind.ECONOMY,v,CITY,""));}
        for(World.Unit u:w.units)unit(out,u);
        for(Domestic.Mission u:w.domestic.missions)unit(out,u);
        for(World.Officer o:w.officers){World.City c=o.cityId<0?null:w.city(o.cityId);
            // Unit movement is already recorded above; don't duplicate it for all three crew members.
            String place=o.unitId>=0?"部队#"+o.unitId:c!=null?c.name:"在途 / 无驻地";
            out.put("o"+o.id,new State(o.owner,o.name,null,Kind.PERSONNEL,new int[]{o.leadership,o.war,o.intelligence,o.politics,o.charm,o.loyalty,o.otherTaskTurns},OFFICER,place+" · "+Skill.label(o.skillId)+(o.otherTask.isEmpty()?"":" · "+o.otherTask)));}
        for(Domestic.Facility f:w.domestic.facilities){World.City c=w.city(f.cityId);out.put("d"+f.id,new State(c==null?-1:c.owner,(c==null?"":c.name)+f.kind.label,f.hex,Kind.ECONOMY,new int[]{f.hp,f.level,f.remaining},new String[]{"耐久","等级","剩余旬"},""));}
        for(War.Structure s:w.war.structures())out.put("s"+s.id,new State(s.owner,s.kind.label,s.hex,Kind.COMBAT,new int[]{s.hp,s.complete?1:0},new String[]{"耐久","完工"},""));
        for(War.Fire f:w.war.fires)out.put("f"+point(f.hex),new State(f.owner,"火场",f.hex,Kind.COMBAT,new int[]{f.remaining},new String[]{"剩余旬"},""));
        for(int side=0;side<w.factions.length;side++){
            StringBuilder learned=new StringBuilder();for(Campaign.Tech t:Campaign.Tech.values())if(w.campaign.has(side,t))learned.append(t.label).append('、');
            out.put("r"+side,new State(side,w.faction(side),null,Kind.RESEARCH,new int[]{w.campaign.points(side)},new String[]{"技巧点"},"已掌握："+learned));
        }
    }
    private void unit(Map<String,State> out,World.Unit u){World.Officer o=w.officer(u.officerId);out.put("u"+u.id,new State(u.owner,(o==null?"部队#"+u.id:o.name)+(u instanceof Domestic.Mission?"运输 / 调动队":u.weapon.label),u.hex,Kind.MOVEMENT,new int[]{u.troops,u.food,u.gold,u.energy,u.statusTurns,u.burning},UNIT,u.status.label+" · "+u.ship.label));}
    /** Length-prefixed UTF-8 and bounded gzip, not writeUTF's 64KB per-string limit. */
    public synchronized void write(DataOutputStream out)throws IOException{
        prune();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream d=new DataOutputStream(new GZIPOutputStream(bytes))){d.writeLong(nextId);d.writeInt(entries.size());for(Entry e:entries){d.writeLong(e.id);d.writeInt(e.turn);d.writeInt(e.actor);d.writeLong(e.related);d.writeByte(e.kind.ordinal());string(d,e.title);string(d,e.detail);d.writeBoolean(e.location!=null);if(e.location!=null){d.writeInt(e.location.q);d.writeInt(e.location.r);}}}
        byte[] payload=bytes.toByteArray();if(payload.length>16*1024*1024)throw new IOException("三个月战报超过存档安全上限；未丢弃任何记录");out.writeInt(payload.length);out.write(payload);
    }
    public synchronized void read(DataInputStream in)throws IOException{
        int length=in.readInt();if(length<0||length>16*1024*1024)throw new IOException("战报数据长度异常");byte[] packed=new byte[length];in.readFully(packed);
        ByteArrayOutputStream raw=new ByteArrayOutputStream();try(InputStream gzip=new GZIPInputStream(new ByteArrayInputStream(packed))){byte[] b=new byte[8192];int n;while((n=gzip.read(b))!=-1){if(raw.size()+n>64*1024*1024)throw new IOException("战报解压超过安全上限");raw.write(b,0,n);}}
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(raw.toByteArray()));nextId=d.readLong();int count=d.readInt();if(nextId<1||count<0||count>200000)throw new IOException("战报数量异常");entries.clear();long last=0;int lastTurn=-1;
        for(int i=0;i<count;i++){long id=d.readLong();int turn=d.readInt(),actor=d.readInt();long related=d.readLong();int kind=d.readUnsignedByte();String title=string(d),detail=string(d);Hex location=d.readBoolean()?new Hex(d.readInt(),d.readInt()):null;
            if(id<=last||id>=nextId||turn<lastTurn||turn<0||turn>w.turn||actor< -1||actor>=w.factions.length||kind>=Kind.values().length||location!=null&&!w.inside(location))throw new IOException("战报字段异常");
            entries.addLast(new Entry(id,turn,actor,related,Kind.values()[kind],title,detail,location));last=id;lastTurn=turn;}
        if(d.available()!=0)throw new IOException("战报尾部异常");prune();rebase();
    }
    private static void string(DataOutputStream d,String text)throws IOException{byte[] bytes=text.getBytes(StandardCharsets.UTF_8);d.writeInt(bytes.length);d.write(bytes);}
    private static String string(DataInputStream d)throws IOException{int n=d.readInt();if(n<0||n>8*1024*1024||n>d.available())throw new IOException("战报文本长度异常");byte[] b=new byte[n];d.readFully(b);return new String(b,StandardCharsets.UTF_8);}
}
