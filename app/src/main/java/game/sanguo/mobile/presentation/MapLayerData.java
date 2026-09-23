package game.sanguo.mobile.presentation;

import java.util.*;

/** Detached display projection, intentionally not a game-api or save object. */
public final class MapLayerData {
    public final Map<String,String> factionLabels;
    public final Map<String,Integer> siteOwners;
    private final int[] colors;
    public MapLayerData(Map<String,String> labels,Map<String,Integer> owners,int[] colors){
        factionLabels=Collections.unmodifiableMap(new HashMap<>(labels));
        siteOwners=Collections.unmodifiableMap(new HashMap<>(owners));this.colors=colors==null?null:colors.clone();
    }
    public int[] colors(){return colors==null?null:colors.clone();}
    public static long cellKey(int q,int r){return ((long)q<<32)^(r&0xffffffffL);}
}
