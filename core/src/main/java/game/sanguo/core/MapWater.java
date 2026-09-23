package game.sanguo.core;
import game.sanguo.core.map.SourceGridCoord;

import java.util.*;

/** Rebuilt for a resolved geographic revision. Water labels come only from explicit
 * national anchors inherited from Geography63Checks, never a component-size threshold. */
public final class MapWater {
    public static final int[][] ANCHORS={{152,110},{123,62},{178,98},{112,135},{70,107},{103,133},{101,68}};
    private final World w;
    private final Map<Hex,Integer> component=new HashMap<>();
    private final Map<Integer,SortedSet<String>> labels=new TreeMap<>();
    private final Map<Integer,Integer> sizes=new TreeMap<>();
    public MapWater(World w){this.w=w;int next=0;World.Unit probe=new World.Unit(-1,0,-1,World.Weapon.SWORD,new Hex(0,0),1,1);
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex start=new Hex(q,r);if(!w.army.water(start)||component.containsKey(start))continue;int id=next++;ArrayDeque<Hex> todo=new ArrayDeque<>();component.put(start,id);todo.add(start);int size=0;
            while(!todo.isEmpty()){Hex h=todo.remove();size++;for(Hex n:h.neighbors())if(w.army.water(n)&&!component.containsKey(n)&&w.army.entryCost(probe,h,n)>0){component.put(n,id);todo.add(n);}}sizes.put(id,size);}
        for(int i=0;i<ANCHORS.length;i++){Hex h=MapCoordinates.fromNationalSource(w,new SourceGridCoord(ANCHORS[i][0],ANCHORS[i][1]));Integer id=component.get(h);if(id!=null)labels.computeIfAbsent(id,k->new TreeSet<>()).add("全国主航道锚点"+(i+1)+"（"+ANCHORS[i][0]+","+ANCHORS[i][1]+"）");}
    }
    public boolean main(Hex h){Integer id=component.get(h);return id!=null&&labels.containsKey(id);}
    public String description(Hex h){Integer id=component.get(h);return id==null?"非可通航水格":"连通水格 "+sizes.get(id)+" · "+(labels.containsKey(id)?String.join(" / ",labels.get(id)):"未连接已配置主水系锚点；不按面积猜测水域身份");}
    public Map<Integer,Integer> isolated(){Map<Integer,Integer> result=new TreeMap<>();for(var e:sizes.entrySet())if(!labels.containsKey(e.getKey()))result.put(e.getKey(),e.getValue());return result;}
    public Hex representative(int id){for(var e:component.entrySet())if(e.getValue()==id)return e.getKey();return null;}
    public static Hex landDock(World w,World.City port,Hex water){
        World.Unit probe=new World.Unit(-1,port.owner,-1,World.Weapon.SWORD,port.hex,1,1);
        for(Hex land:port.hex.neighbors())if(w.inside(land)&&!w.army.water(land)&&w.cityAt(land)==null&&w.domestic.at(land)==null&&w.war.at(land)==null&&land.distance(water)==1&&w.army.moveCost(probe,land,water)>0&&w.army.moveCost(probe,water,land)>0){
            for(Hex further:land.neighbors())if(!further.equals(port.hex)&&!w.army.water(further)&&w.army.moveCost(probe,land,further)>0)return land;
        }return null;
    }
    public String portError(World.City port){boolean water=false,main=false,dock=false;for(Hex n:port.hex.neighbors())if(w.army.water(n)){water=true;boolean usable=landDock(w,port,n)!=null;dock|=usable;main|=usable&&main(n);}return !water?"港口没有邻接可通航水格":!dock?"港口缺少符合正式上下水规则的陆地出入口":!main?"港口未连接全国主水系锚点（不是根据水域面积判断）":null;}
}
