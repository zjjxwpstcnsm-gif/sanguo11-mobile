package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Persisted city development parcels. Occupancy never changes the parcel boundary. */
public final class Development {
    private final World w;
    private final Map<Integer,List<Hex>> parcels=new TreeMap<>();
    Development(World w){this.w=w;}
    public void configure(int city,List<Hex> sites){
        if(parcels.containsKey(city))throw new IllegalArgumentException("开发地重复");
        parcels.put(city,Collections.unmodifiableList(new ArrayList<>(sites)));
    }
    public boolean configured(int city){return parcels.containsKey(city);}
    public List<Hex> parcels(int city){return parcels.getOrDefault(city,Collections.emptyList());}
    public int capacity(int city){return configured(city)?parcels(city).size():Domestic.CITY_SLOTS;}
    public boolean contains(World.City city,Hex h){return configured(city.id)?parcels(city.id).contains(h):h.distance(city.hex)>=1&&h.distance(city.hex)<=2;}
    public World.City cityAt(Hex h){
        if(h==null)return null;
        for(Map.Entry<Integer,List<Hex>> e:parcels.entrySet())if(e.getValue().contains(h))return w.city(e.getKey());
        // Older saves and small scenarios have implicit radius-two development land.
        World.City nearest=null;
        for(World.City c:w.cities)if(c.kind==World.SiteKind.CITY&&!configured(c.id)&&contains(c,h)
            &&(nearest==null||c.hex.distance(h)<nearest.hex.distance(h)||c.hex.distance(h)==nearest.hex.distance(h)&&c.id<nearest.id))nearest=c;
        return nearest;
    }
    void write(DataOutputStream d)throws IOException {
        d.writeInt(parcels.size());for(Map.Entry<Integer,List<Hex>> e:parcels.entrySet()){
            d.writeInt(e.getKey());d.writeInt(e.getValue().size());for(Hex h:e.getValue()){d.writeInt(h.q);d.writeInt(h.r);}
        }
    }
    void read(DataInputStream d)throws IOException {
        int count=bounded(d.readInt(),0,1000);for(int i=0;i<count;i++){
            int city=d.readInt(),n=bounded(d.readInt(),1,36);List<Hex> sites=new ArrayList<>();
            for(int j=0;j<n;j++)sites.add(new Hex(d.readInt(),d.readInt()));
            if(parcels.containsKey(city))throw new IOException("开发地城市重复");configure(city,sites);
        }
    }
    private static int bounded(int n,int lo,int hi)throws IOException {if(n<lo||n>hi)throw new IOException("开发地数据越界");return n;}
    void validate()throws IOException {
        Set<Hex> all=new HashSet<>();for(Map.Entry<Integer,List<Hex>> e:parcels.entrySet()){
            World.City c=w.city(e.getKey());if(c==null||c.kind!=World.SiteKind.CITY)throw new IOException("开发地所属城池无效");
            bounded(e.getValue().size(),1,36);
            for(Hex h:e.getValue())if(!w.inside(h)||w.terrain[h.q][h.r]!=World.Terrain.PLAIN||w.cityAt(h)!=null||!all.add(h)||h.distance(c.hex)>12)
                throw new IOException("开发地越界、重叠或地形无效");
        }
    }
}
