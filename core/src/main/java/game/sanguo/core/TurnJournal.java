package game.sanguo.core;

import java.util.*;

/** Transient presentation journal. No rule is executed by playback, and nothing is saved from it.
 * Only dynamic map entities are copied; the national terrain and RNG are never copied per action. */
public final class TurnJournal {
    public enum Kind { CHANGE, MOVE, ATTACK, TACTIC, PLOT, ENTER, DEPLOY, FACILITY_ATTACK, FACILITY_COUNTER, RECOVER }
    public static final class Impact {
        public final Hex hex; public final String text; public final boolean loss;
        Impact(Hex h,String text,boolean loss){hex=h;this.text=text;this.loss=loss;}
    }
    public static final class Event {
        public final Kind kind;
        /** Stable identity and equipment captured before the action; presentation only. */
        public final String sourceKey,sourceType;
        public final CriticalHit critical;
        public final int actorId,owner;
        public final Hex start,target;
        public final String label,message;
        public final List<Hex> path;
        public final List<Impact> impacts;
        private final List<Node> changed;
        private final List<String> removed;
        private final World.Unit actor;
        Event(Kind kind,int id,int owner,Hex start,Hex target,String label,String message,List<Hex> path,
              List<Impact> impacts,List<Node> changed,List<String> removed,World.Unit actor,CriticalHit critical,String sourceKey,String sourceType){
            this.sourceKey=sourceKey;this.sourceType=sourceType;
            this.kind=kind;actorId=id;this.owner=owner;this.start=start;this.target=target;this.label=label;this.message=message;
            this.path=Collections.unmodifiableList(new ArrayList<>(path));this.impacts=Collections.unmodifiableList(impacts);
            this.changed=changed;this.removed=removed;this.actor=actor;this.critical=critical;
        }
        public boolean removesUnit(int id){return removed.contains("u"+id);}
        public World.Unit actorCopy(){return actor==null?null:copyUnit(actor);}
        public boolean visibleAction(){return kind!=Kind.CHANGE||!impacts.isEmpty();}
        /** Apply to a dedicated render World ONLY. Never call command APIs, validate or save it. */
        public void applyVisual(World visual){
            for(String key:removed)remove(visual,key);
            for(Node n:changed){remove(visual,n.key);n.add(visual);}
        }
        public int durationMillis(){return kind==Kind.MOVE?Math.min(1300,Math.max(300,(path.size()-1)*145)):kind==Kind.PLOT?700:600;}
    }
    private final World w;
    private Map<String,Node> previous;
    private final List<Event> events=new ArrayList<>();
    private Kind kind=Kind.CHANGE;
    private CriticalHit critical;
    void critical(CriticalHit hit){if(critical==null)critical=hit;}
    private int actorId=-1;
    private Hex target,sourceHex;
    private int sourceOwner=-1;
    private String label="",sourceKey="",sourceType="";
    private List<Hex> path=Collections.emptyList();
    public TurnJournal(World world){w=world;previous=snapshot(world,Collections.emptyMap());world.turnJournal=this;}
    public List<Event> events(){return Collections.unmodifiableList(events);}
    /** Ownership transfer at a rule boundary: immutable events no longer retained by the producer. */
    public List<Event> drainEvents(){List<Event> batch=Collections.unmodifiableList(new ArrayList<>(events));events.clear();return batch;}
    void facility(War.Structure source,Hex target,Kind kind,String label){
        checkpoint("阶段结算");this.kind=kind;this.actorId=-1;this.sourceHex=source.hex;
        this.sourceOwner=source.owner;this.target=target;this.label=label;
        this.sourceKey="s"+source.id;this.sourceType=source.kind.name();
    }
    void site(World.City source,Hex target,Kind kind,String label){
        checkpoint("阶段结算");this.kind=kind;this.actorId=-1;this.sourceHex=source.hex;
        this.sourceOwner=source.owner;this.target=target;this.label=label;
        this.sourceKey="c"+source.id;this.sourceType=source.kind.name();
    }
    public void close(){checkpoint("阶段结算");w.turnJournal=null;}
    void mark(Kind kind,int actor,Hex target,String label){
        if(this.kind==kind&&this.kind!=Kind.CHANGE&&this.actorId==actor&&Objects.equals(this.target,target))return;
        checkpoint("阶段结算");this.kind=kind;this.actorId=actor;this.target=target;this.label=label;
    }
    void movement(World.Unit u,List<Hex> route){mark(Kind.MOVE,u.id,route.get(route.size()-1),"行军");path=new ArrayList<>(route);}
    void cancel(){critical=null;kind=Kind.CHANGE;actorId=-1;target=null;sourceHex=null;sourceOwner=-1;label="";sourceKey="";sourceType="";path=Collections.emptyList();}
    public void checkpoint(String message){
        Map<String,Node> next=snapshot(w,previous);List<Node> changed=new ArrayList<>();List<String> removed=new ArrayList<>();List<Impact> impacts=new ArrayList<>();
        for(Node n:next.values()){Node old=previous.get(n.key);if(old!=n){changed.add(n);impact(old,n,impacts);}}
        for(Node old:previous.values())if(!next.containsKey(old.key)){removed.add(old.key);impact(old,null,impacts);}
        Node a=previous.get("u"+actorId);World.Unit actor=a!=null&&a.image instanceof World.Unit?(World.Unit)a.image:null;
        Kind eventKind=kind;Hex start=actor==null?sourceHex:actor.hex;Hex end=target;String name=label;List<Hex> route=path;
        // A tick can move transport/forced-displacement units without a player command.
        if(eventKind==Kind.CHANGE){
            for(Node n:changed){Node old=previous.get(n.key);if(n.image instanceof World.Unit&&old!=null&&!n.hex.equals(old.hex)){
                actor=(World.Unit)old.image;start=old.hex;end=n.hex;route=Arrays.asList(start,end);name="位移";eventKind=Kind.MOVE;break;
            }}
        }
        if(eventKind==Kind.ATTACK||eventKind==Kind.TACTIC||eventKind==Kind.PLOT||eventKind==Kind.ENTER||eventKind==Kind.FACILITY_ATTACK||eventKind==Kind.FACILITY_COUNTER){
            for(int i=0;i<impacts.size();i++){Impact hit=impacts.get(i);if(hit.text.equals("离场"))impacts.set(i,new Impact(hit.hex,eventKind==Kind.ENTER?"进驻":"击破",eventKind!=Kind.ENTER));}
        }
        if(!changed.isEmpty()||!removed.isEmpty()||eventKind!=Kind.CHANGE){
            int id=actor==null?actorId:actor.id,owner=actor==null?(sourceOwner>=0?sourceOwner:w.active):actor.owner;
            if(name.isEmpty())name=impacts.isEmpty()?"结算":impacts.get(0).text;
            events.add(new Event(eventKind,id,owner,start,end,name,message,route,impacts,changed,removed,actor,critical,actor==null?sourceKey:"u"+actor.id,actor==null?sourceType:actor.weapon.name()));
        }
        previous=next;cancel();
    }
    private static void impact(Node old,Node n,List<Impact> out){
        Object a=old==null?null:old.image,b=n==null?null:n.image;
        if(a instanceof World.Unit){World.Unit before=(World.Unit)a,after=b instanceof World.Unit?(World.Unit)b:null;
            if(after==null){out.add(new Impact(before.hex,"离场",false));return;}
            int loss=before.troops-after.troops;if(loss>0)out.add(new Impact(after.hex,"−"+loss+"兵",true));
            if(before.status!=after.status)out.add(new Impact(after.hex,after.status.label,false));
            int energy=after.energy-before.energy;if(energy!=0)out.add(new Impact(after.hex,"气力 "+(energy>0?"+":"−")+Math.abs(energy),energy<0));
        }else if(a instanceof World.City&&b instanceof World.City){World.City before=(World.City)a,after=(World.City)b;
            if(before.owner!=after.owner)out.add(new Impact(after.hex,"攻占",true));
            else {int loss=before.defense-after.defense;if(loss>0)out.add(new Impact(after.hex,"城防 −"+loss,true));
                loss=before.troops-after.troops;if(loss>0)out.add(new Impact(after.hex,"守军 −"+loss,true));}
        }else if(a instanceof Domestic.Facility){Domestic.Facility f=(Domestic.Facility)a;
            if(b==null)out.add(new Impact(f.hex,f.kind.label+"损毁",true));
            else if(f.hp>((Domestic.Facility)b).hp)out.add(new Impact(f.hex,"耐久 −"+(f.hp-((Domestic.Facility)b).hp),true));
        }else if(a instanceof War.Structure){War.Structure s=(War.Structure)a;
            if(b==null)out.add(new Impact(s.hex,s.kind.label+"摧毁",true));
            else if(s.hp>((War.Structure)b).hp)out.add(new Impact(s.hex,"耐久 −"+(s.hp-((War.Structure)b).hp),true));
        }else if(a==null&&b instanceof War.Fire)out.add(new Impact(n.hex,"起火",true));
    }
    private static final class Node {
        final String key;final Hex hex;final Object image;
        Node(String key,Hex hex,Object image){this.key=key;this.hex=hex;this.image=image;}
        void add(World w){
            if(image instanceof Domestic.Mission)w.domestic.missions.add((Domestic.Mission)copyUnit((World.Unit)image));
            else if(image instanceof World.Unit)w.units.add(copyUnit((World.Unit)image));
            else if(image instanceof World.City)w.cities.add(copyCity((World.City)image));
            else if(image instanceof Domestic.Facility)w.domestic.facilities.add(copyFacility((Domestic.Facility)image));
            else if(image instanceof War.Structure)w.war.structures.add(copyStructure((War.Structure)image));
            else if(image instanceof War.Fire)w.war.fires.add(copyFire((War.Fire)image));
        }
    }
    private static Map<String,Node> snapshot(World w,Map<String,Node> prior){
        Map<String,Node> map=new LinkedHashMap<>();
        for(World.Unit u:w.units)record(map,prior,"u"+u.id,u.hex,u);
        for(Domestic.Mission m:w.domestic.missions)record(map,prior,"u"+m.id,m.hex,m);
        for(World.City c:w.cities)record(map,prior,"c"+c.id,c.hex,c);
        for(Domestic.Facility f:w.domestic.facilities)record(map,prior,"d"+f.id,f.hex,f);
        for(War.Structure s:w.war.structures)record(map,prior,"s"+s.id,s.hex,s);
        for(War.Fire f:w.war.fires)record(map,prior,"f"+f.hex.q+":"+f.hex.r,f.hex,f);
        return map;
    }
    private static void record(Map<String,Node> map,Map<String,Node> prior,String key,Hex h,Object source){
        Node old=prior.get(key);
        if(old!=null&&sameVisual(old.image,source)){map.put(key,old);return;}
        Object image=source instanceof World.Unit?copyUnit((World.Unit)source):source instanceof World.City?copyCity((World.City)source):
            source instanceof Domestic.Facility?copyFacility((Domestic.Facility)source):source instanceof War.Structure?copyStructure((War.Structure)source):copyFire((War.Fire)source);
        map.put(key,new Node(key,h,image));
    }
    private static boolean sameVisual(Object left,Object right){
        if(left.getClass()!=right.getClass())return false;
        if(left instanceof World.Unit){World.Unit a=(World.Unit)left,b=(World.Unit)right;
            if(a.owner!=b.owner||a.officerId!=b.officerId||!a.hex.equals(b.hex)||a.troops!=b.troops||a.wounded!=b.wounded||a.woundRemainder!=b.woundRemainder||a.energy!=b.energy||a.food!=b.food||a.gold!=b.gold||a.status!=b.status||a.statusTurns!=b.statusTurns||a.acted!=b.acted||a.burning!=b.burning||a.ship!=b.ship||!Arrays.equals(a.deputies,b.deputies))return false;
            if(a instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)a,n=(Domestic.Mission)b;return Objects.equals(m.waiting,n.waiting)&&m.stopped==n.stopped&&m.transport==n.transport&&m.targetCity==n.targetCity;}
            return true;
        }
        if(left instanceof World.City){World.City a=(World.City)left,b=(World.City)right;return a.owner==b.owner&&a.gold==b.gold&&a.food==b.food&&a.troops==b.troops&&a.order==b.order&&a.morale==b.morale&&a.defense==b.defense&&a.kind==b.kind&&Arrays.equals(a.equipment,b.equipment)&&Arrays.equals(a.ships,b.ships);}
        if(left instanceof Domestic.Facility){Domestic.Facility a=(Domestic.Facility)left,b=(Domestic.Facility)right;return a.hp==b.hp&&a.builderId==b.builderId&&a.remaining==b.remaining&&a.level==b.level&&a.upgradeTo==b.upgradeTo;}
        if(left instanceof War.Structure){War.Structure a=(War.Structure)left,b=(War.Structure)right;return a.owner==b.owner&&a.kind==b.kind&&a.hp==b.hp&&a.builder==b.builder&&a.complete==b.complete&&a.direction==b.direction;}
        War.Fire a=(War.Fire)left,b=(War.Fire)right;return a.owner==b.owner&&a.remaining==b.remaining&&a.power==b.power&&a.trap==b.trap;
    }
    private static void remove(World w,String key){
        char type=key.charAt(0);if(type=='f'){w.war.fires.removeIf(f->key.equals("f"+f.hex.q+":"+f.hex.r));return;}
        int id=Integer.parseInt(key.substring(1));
        if(type=='u'){w.units.removeIf(u->u.id==id);w.domestic.missions.removeIf(u->u.id==id);}
        if(type=='c')w.cities.removeIf(c->c.id==id);
        if(type=='d')w.domestic.facilities.removeIf(f->f.id==id);
        if(type=='s')w.war.structures.removeIf(s->s.id==id);
    }
    private static World.Unit copyUnit(World.Unit u){
        World.Unit n;
        if(u instanceof Domestic.Mission){Domestic.Mission m=(Domestic.Mission)u;
            Domestic.Mission v=new Domestic.Mission(m.taskId,m.owner,m.officerId,m.sourceCity,m.targetCity,m.hex,m.transport,m.gold,m.food,m.troops,m.equipment);
            v.sea=m.sea;v.returnOfficers=m.returnOfficers;v.returning=m.returning;v.stopped=m.stopped;v.legacyOverlap=m.legacyOverlap;v.waiting=m.waiting;
            v.cargoShips[0]=m.cargoShips[0];v.cargoShips[1]=m.cargoShips[1];v.escortId=m.escortId;n=v;
        }else n=new World.Unit(u.id,u.owner,u.officerId,u.weapon,u.hex,u.troops,u.food);
        n.wounded=u.wounded;n.woundRemainder=u.woundRemainder;n.gold=u.gold;n.energy=u.energy;n.acted=u.acted;n.status=u.status;n.statusTurns=u.statusTurns;n.burning=u.burning;n.burningOwner=u.burningOwner;n.burningPower=u.burningPower;
        n.deputies=u.deputies.clone();n.ship=u.ship;n.movementBudget=u.movementBudget;n.movementSpent=u.movementSpent;return n;
    }
    private static World.City copyCity(World.City c){World.City n=new World.City(c.id,c.name,c.hex,c.owner);n.gold=c.gold;n.food=c.food;n.troops=c.troops;n.order=c.order;n.morale=c.morale;n.defense=c.defense;n.kind=c.kind;n.baseDefense=c.baseDefense;n.recruitReserve=c.recruitReserve;n.governorId=c.governorId;System.arraycopy(c.equipment,0,n.equipment,0,c.equipment.length);System.arraycopy(c.ships,0,n.ships,0,c.ships.length);return n;}
    private static Domestic.Facility copyFacility(Domestic.Facility f){Domestic.Facility n=new Domestic.Facility(f.id,f.cityId,f.kind,f.hex,f.builderId,f.remaining);n.level=f.level;n.upgradeTo=f.upgradeTo;n.hp=f.hp;n.lastUseTurn=f.lastUseTurn;return n;}
    private static War.Structure copyStructure(War.Structure s){War.Structure n=new War.Structure(s.id,s.owner,s.kind,s.hex,s.hp);n.builder=s.builder;n.direction=s.direction;n.complete=s.complete;return n;}
    private static War.Fire copyFire(War.Fire f){War.Fire n=new War.Fire(f.hex,f.owner,f.remaining);n.power=f.power;n.trap=f.trap;return n;}
}
