package game.sanguo.mobile;

import game.sanguo.api.GameApi;
import game.sanguo.mobile.bridge.AndroidGameBridge;

/** Stable full-qualified JNI facade referenced by Unity C#. Do not rename without an ABI migration. */
public final class UnityBridge {
    private UnityBridge(){}
    static void bind(GameApi game){AndroidGameBridge.bind(game);}
    static String sessionId(){return AndroidGameBridge.sessionId();}
    public static String request(String json){return AndroidGameBridge.request(json);}
    public static String poll(String sessionId){return AndroidGameBridge.poll(sessionId);}
}
