package game.sanguo.core;

import java.util.*;

/** One presentation channel over an existing authoritative World. Call on the host logic thread. */
public final class BridgeSession {
    public static final int SCHEMA = 1, MAX_PENDING = 128;
    public static final class Entity {
        public final String entityId, kind, name;
        public final int q, r, owner, troops, energy, officerId, gold, food, order;
        Entity(String id,String kind,String name,Hex hex,int owner,int troops,int energy,int officerId,int gold,int food,int order){
            this.entityId=id;this.kind=kind;this.name=name;this.q=hex.q;this.r=hex.r;
            this.owner=owner;this.troops=troops;this.energy=energy;this.officerId=officerId;this.gold=gold;this.food=food;this.order=order;
        }
        @Override public boolean equals(Object other){
            if(!(other instanceof Entity))return false;Entity e=(Entity)other;
            return entityId.equals(e.entityId)&&kind.equals(e.kind)&&name.equals(e.name)&&q==e.q&&r==e.r
                &&owner==e.owner&&troops==e.troops&&energy==e.energy&&officerId==e.officerId&&gold==e.gold&&food==e.food&&order==e.order;
        }
        @Override public int hashCode(){return Objects.hash(entityId,kind,name,q,r,owner,troops,energy,officerId,gold,food,order);}
    }
    public static final class Message {
        public final String type, sessionId, commandId, error, detail, terrain;
        public final long sequence, revision;
        public final int mapRevision, width, height, turn, player;
        public final List<Entity> entities;
        public final List<String> removed;
        Message(String type,String sessionId,long sequence,long revision,int mapRevision,int width,int height,int turn,
                int player,String commandId,String error,String detail,String terrain,List<Entity> entities,List<String> removed){
            this.type=type;this.sessionId=sessionId;this.sequence=sequence;this.revision=revision;
            this.mapRevision=mapRevision;this.width=width;this.height=height;this.turn=turn;this.player=player;
            this.commandId=commandId;this.error=error;this.detail=detail;this.terrain=terrain;
            this.entities=Collections.unmodifiableList(entities);this.removed=Collections.unmodifiableList(removed);
        }
    }
    private final World world;
    public final String sessionId=UUID.randomUUID().toString();
    private final ArrayDeque<Message> pending=new ArrayDeque<>();
    private final LinkedHashMap<String,Message> receipts=new LinkedHashMap<String,Message>(256,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Message> e){return size()>256;}
    };
    private Map<String,Entity> last=new LinkedHashMap<>();
    private long sequence, revision, lastClientSequence;
    private int mapRevision=Integer.MIN_VALUE, terrainRevision=Integer.MIN_VALUE, lastTurn, lastPlayer, dropped, pendingBytes;
    private static final int MAX_PENDING_BYTES=1024*1024;
    public BridgeSession(World world){this.world=Objects.requireNonNull(world);}
    public World world(){return world;}
    public long revision(){return revision;}
    public int pendingCount(){return pending.size();}
    public int droppedCount(){return dropped;}

    private Message message(String type,String commandId,String error,String terrain,List<Entity> changes,List<String> removed){
        return message(type,commandId,error,null,terrain,changes,removed);
    }
    private Message message(String type,String commandId,String error,String detail,String terrain,List<Entity> changes,List<String> removed){
        return new Message(type,sessionId,++sequence,revision,world.mapRevision,world.width,world.height,world.turn,
            world.player,commandId,error,detail,terrain,changes,removed);
    }
    private void offer(Message value){
        int size=(value.terrain==null?0:value.terrain.length()*2)+value.entities.size()*240+256;
        if(pending.size()==MAX_PENDING||pendingBytes+size>MAX_PENDING_BYTES){
            dropped+=pending.size()+1;pending.clear();
            pendingBytes=0;
            // The client must explicitly request a fresh full snapshot after a gap.
            pending.add(message("resync",null,"QUEUE_OVERFLOW",null,List.of(),List.of()));pendingBytes=256;
        }else {pending.add(value);pendingBytes+=size;}
    }
    private Map<String,Entity> entities(){
        Map<String,Entity> result=new LinkedHashMap<>();
        for(World.City c:world.cities){List<World.Officer> idle=world.idle(c);
            Entity e=new Entity("site:"+c.id,c.kind.name(),c.name,c.hex,c.owner,c.troops,c.morale,
                idle.isEmpty()?-1:idle.get(0).id,c.gold,c.food,c.order);result.put(e.entityId,e);
        }
        for(World.Unit u:world.fieldUnits()){
            Entity e=new Entity("unit:"+u.id,"UNIT",world.officer(u.officerId)==null?"部队":world.officer(u.officerId).name,
                u.hex,u.owner,u.troops,u.energy,u.officerId,u.gold,u.food,0);result.put(e.entityId,e);
        }
        return result;
    }
    /** Terrain is a single immutable row-major ordinal string, emitted only with a full snapshot. */
    public void snapshot(){
        Map<String,Entity> current=entities();last=current;mapRevision=world.mapRevision;
        terrainRevision=world.terrainRevision;lastTurn=world.turn;lastPlayer=world.player;
        StringBuilder terrain=new StringBuilder(world.width*world.height);
        for(int r=0;r<world.height;r++)for(int q=0;q<world.width;q++)
            terrain.append((char)('A'+world.terrain[q][r].ordinal()));
        pending.clear();pendingBytes=0;offer(message("snapshot",null,null,terrain.toString(),new ArrayList<>(current.values()),List.of()));
    }
    /** Native commands and turn completion call this once, not each frame. */
    public void changed(){
        if(world.mapRevision!=mapRevision||world.terrainRevision!=terrainRevision){revision++;snapshot();return;}
        Map<String,Entity> current=entities();List<Entity> updates=new ArrayList<>();List<String> removed=new ArrayList<>();
        for(Entity e:current.values())if(!e.equals(last.get(e.entityId)))updates.add(e);
        for(String key:last.keySet())if(!current.containsKey(key))removed.add(key);
        if(updates.isEmpty()&&removed.isEmpty()&&world.turn==lastTurn&&world.player==lastPlayer)return;
        revision++;last=current;lastTurn=world.turn;lastPlayer=world.player;
        offer(message("delta",null,null,null,updates,removed));
    }
    public Message command(String commandId,long clientSequence,long expectedRevision,String operation,int cityId,int officerId){
        Message previous=receipts.get(commandId);
        if(previous!=null){Message replay=message("receipt",commandId,previous.error,null,List.of(),List.of());offer(replay);return replay;}
        String error=null;
        if(commandId==null||commandId.isEmpty()||commandId.length()>80)error="BAD_COMMAND_ID";
        else if(clientSequence<=lastClientSequence)error="CLIENT_SEQUENCE";
        else if(expectedRevision!=revision)error="STALE_REVISION";
        World.Result result=null;
        if(error==null){
            lastClientSequence=clientSequence;
            if("recruit".equals(operation))result=world.recruit(cityId,officerId);
            else if("patrol".equals(operation))result=world.patrol(cityId,officerId);
            else error="UNKNOWN_COMMAND";
            if(result!=null){
                if(result.ok){long before=revision;changed();if(before==revision)revision++;
                    offer(message("event",commandId,null,result.message,null,List.of(),List.of()));
                }else error=result.message;
            }
        }
        Message receipt=message("receipt",commandId,error,null,List.of(),List.of());
        if(commandId!=null&&!commandId.isEmpty()&&commandId.length()<=80)receipts.put(commandId,receipt);
        offer(receipt);return receipt;
    }
    public void reject(String commandId,String error){offer(message("receipt",commandId,error,null,List.of(),List.of()));}
    public List<Message> drain(){List<Message> list=new ArrayList<>(pending);pending.clear();pendingBytes=0;return list;}
}
