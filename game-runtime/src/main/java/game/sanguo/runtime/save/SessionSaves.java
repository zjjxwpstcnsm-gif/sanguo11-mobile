package game.sanguo.runtime.save;

import game.sanguo.core.SaveCodec;
import game.sanguo.core.World;
import game.sanguo.runtime.GameSession;
import java.io.*;

/** Save/restore coordination is rule-runtime work; choosing files and atomic IO is platform work. */
public final class SessionSaves {
    private final GameSession session;
    public SessionSaves(GameSession session){this.session=session;}
    public byte[] capture()throws IOException{return session.captureSave();}
    public static World readPrepared(InputStream input)throws IOException{return SaveCodec.read(input);}
    public void restore(InputStream input)throws IOException{session.replace(readPrepared(input));}
}
