package game.sanguo.runtime.bridge;

import game.sanguo.api.*;
import game.sanguo.api.bridge.BridgeEntity;
import game.sanguo.api.bridge.BridgeMessage;

import java.util.*;

/** One presentation channel over an existing authoritative World. Call on the host logic thread. */
public final class BridgeSession implements AutoCloseable {
    public static final int SCHEMA = 1, MAX_PENDING = 128;
    private final GameApi game;
    private final GameApi.Subscription subscription;
    private GameSnapshot facts;
    private boolean closed;
    public final String sessionId;
    private final ArrayDeque<BridgeMessage> pending=new ArrayDeque<>();
    private final LinkedHashMap<String,BridgeMessage> receipts=new LinkedHashMap<String,BridgeMessage>(256,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,BridgeMessage> e){return size()>256;}
    };
    private Map<String,BridgeEntity> last=new LinkedHashMap<>();
    private long sequence, revision, lastClientSequence;
    private int mapRevision=Integer.MIN_VALUE, terrainRevision=Integer.MIN_VALUE, lastTurn, lastPlayer, dropped, pendingBytes;
    private static final int MAX_PENDING_BYTES=1024*1024;
    public BridgeSession(GameApi game){
        this.game=Objects.requireNonNull(game);facts=game.snapshot();sessionId=facts.state.sessionId;revision=facts.state.revision;
        subscription=game.subscribe(event->{
            if(!sessionId.equals(event.state.sessionId)||event.kind==GameEvent.Kind.CLOSED){closed=true;return;}
            changed();
        });
    }
    @Override public void close(){subscription.close();synchronized(this){closed=true;pending.clear();pendingBytes=0;receipts.clear();}}
    public synchronized long revision(){return revision;}
    public synchronized int pendingCount(){return pending.size();}
    public synchronized int droppedCount(){return dropped;}

    private BridgeMessage message(String type,String commandId,String error,String terrain,List<BridgeEntity> changes,List<String> removed){
        return message(type,commandId,error,null,terrain,changes,removed);
    }
    private BridgeMessage message(String type,String commandId,String error,String detail,String terrain,List<BridgeEntity> changes,List<String> removed){
        return new BridgeMessage(type,sessionId,++sequence,revision,facts.mapRevision,facts.width,facts.height,facts.turn,
            facts.player,commandId,error,detail,terrain,changes,removed);
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
        for(BridgeEntity entity:facts.entities)result.put(entity.entityId,entity);
        return result;
    }
    /** Full facts are captured on the serial game thread, never by the polling/render thread. */
    public synchronized void snapshot(){
        if(closed)return;
        facts=game.snapshot();revision=facts.state.revision;
        last=entities();mapRevision=facts.mapRevision;terrainRevision=facts.terrainRevision;
        lastTurn=facts.turn;lastPlayer=facts.player;
        pending.clear();pendingBytes=0;
        offer(message("snapshot",null,null,facts.terrain,new ArrayList<>(last.values()),List.of()));
    }
    /** Invoked by authoritative commit notification only. Empty deltas still carry a new version. */
    private synchronized void changed(){
        if(closed)return;
        facts=game.snapshot();revision=facts.state.revision;
        if(facts.mapRevision!=mapRevision||facts.terrainRevision!=terrainRevision){snapshot();return;}
        Map<String,BridgeEntity> current=entities();List<BridgeEntity> updates=new ArrayList<>();List<String> removed=new ArrayList<>();
        for(BridgeEntity e:current.values())if(!e.equals(last.get(e.entityId)))updates.add(e);
        for(String key:last.keySet())if(!current.containsKey(key))removed.add(key);
        last=current;lastTurn=facts.turn;lastPlayer=facts.player;
        offer(message("delta",null,null,null,updates,removed));
    }
    public synchronized BridgeMessage command(String commandId,long clientSequence,long expectedRevision,String operation,int cityId,int officerId){
        if(closed||!sessionId.equals(game.state().sessionId)){
            BridgeMessage stale=message("receipt",commandId,"STALE_SESSION",null,List.of(),List.of());offer(stale);return stale;
        }
        BridgeMessage previous=receipts.get(commandId);
        if(previous!=null){BridgeMessage replay=message("receipt",commandId,previous.error,null,List.of(),List.of());offer(replay);return replay;}
        String error=null;
        if(commandId==null||commandId.isEmpty()||commandId.length()>80)error="BAD_COMMAND_ID";
        else if(clientSequence<=lastClientSequence)error="CLIENT_SEQUENCE";
        else if(expectedRevision!=revision)error="STALE_REVISION";
        if(closed||!sessionId.equals(game.state().sessionId))error="STALE_SESSION";
        if(error==null){
            lastClientSequence=clientSequence;
            GameCommand.Operation kind="recruit".equals(operation)?GameCommand.Operation.RECRUIT:
                "patrol".equals(operation)?GameCommand.Operation.PATROL:null;
            if(kind==null)error="UNKNOWN_COMMAND";
            else {
                CommandResult result=game.execute(new GameCommand(kind,game.state(),cityId,officerId));
                if(result.ok())offer(message("event",commandId,null,result.detail,null,List.of(),List.of()));
                else error=result.error==CommandResult.Error.RULE_REJECTED?result.detail:result.error.name();
            }
        }
        BridgeMessage receipt=message("receipt",commandId,error,null,List.of(),List.of());
        if(commandId!=null&&!commandId.isEmpty()&&commandId.length()<=80)receipts.put(commandId,receipt);
        offer(receipt);return receipt;
    }
    public synchronized void reject(String commandId,String error){offer(message("receipt",commandId,error,null,List.of(),List.of()));}
    public synchronized List<BridgeMessage> drain(){List<BridgeMessage> list=new ArrayList<>(pending);pending.clear();pendingBytes=0;return list;}
}
