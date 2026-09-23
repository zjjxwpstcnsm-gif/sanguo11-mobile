package game.sanguo.runtime.bridge;

import game.sanguo.core.*;
import game.sanguo.api.bridge.BridgeEntity;
import game.sanguo.api.bridge.BridgeMessage;

import java.util.*;

/** One presentation channel over an existing authoritative World. Call on the host logic thread. */
public final class BridgeSession {
    public static final int SCHEMA = 1, MAX_PENDING = 128;
    private final World world;
    public final String sessionId=UUID.randomUUID().toString();
    private final ArrayDeque<BridgeMessage> pending=new ArrayDeque<>();
    private final LinkedHashMap<String,BridgeMessage> receipts=new LinkedHashMap<String,BridgeMessage>(256,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,BridgeMessage> e){return size()>256;}
    };
    private Map<String,BridgeEntity> last=new LinkedHashMap<>();
    private long sequence, revision, lastClientSequence;
    private int mapRevision=Integer.MIN_VALUE, terrainRevision=Integer.MIN_VALUE, lastTurn, lastPlayer, dropped, pendingBytes;
    private static final int MAX_PENDING_BYTES=1024*1024;
    public BridgeSession(World world){this.world=Objects.requireNonNull(world);}
    public World world(){return world;}
    public long revision(){return revision;}
    public int pendingCount(){return pending.size();}
    public int droppedCount(){return dropped;}

    private BridgeMessage message(String type,String commandId,String error,String terrain,List<BridgeEntity> changes,List<String> removed){
        return message(type,commandId,error,null,terrain,changes,removed);
    }
    private BridgeMessage message(String type,String commandId,String error,String detail,String terrain,List<BridgeEntity> changes,List<String> removed){
        return new BridgeMessage(type,sessionId,++sequence,revision,world.mapRevision,world.width,world.height,world.turn,
            world.player,commandId,error,detail,terrain,changes,removed);
    }
    private void offer(BridgeMessage value){
        int size=(value.terrain==null?0:value.terrain.length()*2)+value.entities.size()*240+256;
        if(pending.size()==MAX_PENDING||pendingBytes+size>MAX_PENDING_BYTES){
            dropped+=pending.size()+1;pending.clear();
            pendingBytes=0;
            // The client must explicitly request a fresh full snapshot after a gap.
            pending.add(message("resync",null,"QUEUE_OVERFLOW",null,List.of(),List.of()));pendingBytes=256;
        }else {pending.add(value);pendingBytes+=size;}
    }
    private Map<String,BridgeEntity> entities(){
        Map<String,BridgeEntity> result=new LinkedHashMap<>();
        for(World.City c:world.cities){List<World.Officer> idle=world.idle(c);
            BridgeEntity e=new BridgeEntity("site:"+c.id,c.kind.name(),c.name,c.hex.q,c.hex.r,c.owner,c.troops,c.morale,
                idle.isEmpty()?-1:idle.get(0).id,c.gold,c.food,c.order);result.put(e.entityId,e);
        }
        for(World.Unit u:world.fieldUnits()){
            BridgeEntity e=new BridgeEntity("unit:"+u.id,"UNIT",world.officer(u.officerId)==null?"部队":world.officer(u.officerId).name,
                u.hex.q,u.hex.r,u.owner,u.troops,u.energy,u.officerId,u.gold,u.food,0);result.put(e.entityId,e);
        }
        return result;
    }
    /** Terrain is a single immutable row-major ordinal string, emitted only with a full snapshot. */
    public void snapshot(){
        Map<String,BridgeEntity> current=entities();last=current;mapRevision=world.mapRevision;
        terrainRevision=world.terrainRevision;lastTurn=world.turn;lastPlayer=world.player;
        StringBuilder terrain=new StringBuilder(world.width*world.height);
        for(int r=0;r<world.height;r++)for(int q=0;q<world.width;q++)
            terrain.append((char)('A'+world.terrain[q][r].ordinal()));
        pending.clear();pendingBytes=0;offer(message("snapshot",null,null,terrain.toString(),new ArrayList<>(current.values()),List.of()));
    }
    /** Native commands and turn completion call this once, not each frame. */
    public void changed(){
        if(world.mapRevision!=mapRevision||world.terrainRevision!=terrainRevision){revision++;snapshot();return;}
        Map<String,BridgeEntity> current=entities();List<BridgeEntity> updates=new ArrayList<>();List<String> removed=new ArrayList<>();
        for(BridgeEntity e:current.values())if(!e.equals(last.get(e.entityId)))updates.add(e);
        for(String key:last.keySet())if(!current.containsKey(key))removed.add(key);
        if(updates.isEmpty()&&removed.isEmpty()&&world.turn==lastTurn&&world.player==lastPlayer)return;
        revision++;last=current;lastTurn=world.turn;lastPlayer=world.player;
        offer(message("delta",null,null,null,updates,removed));
    }
    public BridgeMessage command(String commandId,long clientSequence,long expectedRevision,String operation,int cityId,int officerId){
        BridgeMessage previous=receipts.get(commandId);
        if(previous!=null){BridgeMessage replay=message("receipt",commandId,previous.error,null,List.of(),List.of());offer(replay);return replay;}
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
        BridgeMessage receipt=message("receipt",commandId,error,null,List.of(),List.of());
        if(commandId!=null&&!commandId.isEmpty()&&commandId.length()<=80)receipts.put(commandId,receipt);
        offer(receipt);return receipt;
    }
    public void reject(String commandId,String error){offer(message("receipt",commandId,error,null,List.of(),List.of()));}
    public List<BridgeMessage> drain(){List<BridgeMessage> list=new ArrayList<>(pending);pending.clear();pendingBytes=0;return list;}
}
