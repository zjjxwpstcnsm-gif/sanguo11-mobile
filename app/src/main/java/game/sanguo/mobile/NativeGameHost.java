package game.sanguo.mobile;

import android.content.Context;
import game.sanguo.core.World;
import game.sanguo.runtime.GameSession;
import game.sanguo.runtime.save.SessionSaves;
import game.sanguo.mobile.platform.save.AndroidSaveStore;
import java.io.IOException;

/** Android assembly and retained native playback work. Never references an Activity or View. */
final class NativeGameHost {
    private final AndroidSaveStore store;
    private GameSession session;
    TurnWork turn;
    final CombatReplayLedger combatLedger=new CombatReplayLedger();
    NativeGameHost(Context context){store=new AndroidSaveStore(context);}
    GameSession session(){return session;}
    AndroidSaveStore store(){return store;}
    void install(World prepared)throws IOException{
        combatLedger.clear();
        if(session==null){session=new GameSession(prepared,e->android.util.Log.e("GameSession","Observer failed",e));UnityBridge.bind(session);}
        else session.replace(prepared);
        if(turn!=null){turn.cancel();turn=null;}
    }
    byte[] capture()throws IOException{if(session==null)throw new IOException("No active session");return new SessionSaves(session).capture();}
    TurnWork beginTurn(World presentation)throws IOException{
        if(session==null||turn!=null)throw new IllegalStateException("HOST_BUSY");
        turn=new TurnWork(presentation,session,session.beginTurn(),this::saveCompletedTurn);return turn;
    }
    private void saveCompletedTurn(){
        try{store.write("auto",capture());}catch(IOException error){android.util.Log.e("GameSession","Autosave after turn failed; authority retained",error);}
    }
    void playbackFinished(TurnWork completed){if(turn==completed)turn=null;}
    void exit(){
        combatLedger.clear();
        if(turn!=null){turn.cancel();turn=null;}
        if(session!=null){session.close();session=null;}UnityBridge.bind(null);
    }
}
