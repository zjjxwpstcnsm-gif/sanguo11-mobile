package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Saved intentions only: movement, payment and combat stay in the authoritative commands. */
public final class AiOrders {
    static final class Order {
        int target=-1,home=-1,stalled,turn=-1; boolean staging,defending;Hex last;
    }
    private final World w;
    final SortedMap<Integer,Order> orders=new TreeMap<>();
    AiOrders(World w){this.w=w;}
    Order get(World.Unit u){return orders.computeIfAbsent(u.id,id->new Order());}
    public String describe(World.Unit u){Order o=orders.get(u.id);if(o==null)return "待评估";World.City c=w.city(o.target);return (c==null?"守备 / 归城":"集结攻略 "+c.name)+(o.stalled>0?" · 连续无进展"+o.stalled+"旬":"");}
    void cleanup(){orders.keySet().removeIf(id->w.unit(id)==null);}
    void write(DataOutputStream d)throws IOException{
        d.writeInt(orders.size());for(Map.Entry<Integer,Order> e:orders.entrySet()){Order o=e.getValue();d.writeInt(e.getKey());d.writeInt(o.target);d.writeInt(o.home);d.writeInt(o.stalled);d.writeInt(o.turn);d.writeBoolean(o.staging);d.writeBoolean(o.defending);d.writeBoolean(o.last!=null);if(o.last!=null){d.writeInt(o.last.q);d.writeInt(o.last.r);}}
        d.writeInt(w.districts.groups.size());for(Districts.District g:w.districts.groups.values()){
            d.writeInt(g.id);d.writeInt(g.reserveTroops);d.writeInt(g.reserveGold);d.writeInt(g.reserveFood);d.writeBoolean(g.transfer);d.writeBoolean(g.supplyEnabled);d.writeUTF(g.report);d.writeInt(g.reportTurn);
        }
    }
    void read(DataInputStream d)throws IOException{
        int n=range(d.readInt(),0,10000);for(int i=0;i<n;i++){int id=d.readInt();Order o=new Order();o.target=d.readInt();o.home=d.readInt();o.stalled=d.readInt();o.turn=d.readInt();o.staging=d.readBoolean();o.defending=d.readBoolean();if(d.readBoolean())o.last=new Hex(d.readInt(),d.readInt());if(orders.put(id,o)!=null)throw new IOException("重复AI任务");}
        n=range(d.readInt(),0,7);Set<Integer> seen=new HashSet<>();for(int i=0;i<n;i++){int id=d.readInt();Districts.District g=w.districts.get(id);if(g==null||!seen.add(id))throw new IOException("无效军团设置");g.reserveTroops=d.readInt();g.reserveGold=d.readInt();g.reserveFood=d.readInt();g.transfer=d.readBoolean();g.supplyEnabled=d.readBoolean();g.report=d.readUTF();g.reportTurn=d.readInt();}
        if(n!=w.districts.groups.size())throw new IOException("缺少军团设置");
    }
    void validate()throws IOException{
        for(Map.Entry<Integer,Order> e:orders.entrySet()){Order o=e.getValue();if(w.unit(e.getKey())==null||o.target!=-1&&w.city(o.target)==null||o.home!=-1&&w.city(o.home)==null||o.last!=null&&!w.inside(o.last))throw new IOException("AI任务引用无效");range(o.stalled,0,100);range(o.turn,-1,w.turn);}
        for(Districts.District g:w.districts.groups.values()){range(g.reserveTroops,0,100000);range(g.reserveGold,0,1000000);range(g.reserveFood,0,1000000);range(g.reportTurn,-1,w.turn);if(g.report==null||g.report.length()>6000)throw new IOException("军团报告越界");}
    }
    private static int range(int v,int min,int max)throws IOException{if(v<min||v>max)throw new IOException("AI设置字段越界");return v;}
}
