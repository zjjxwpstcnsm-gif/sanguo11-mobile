package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Original4b1950/task37. Explicit source0 capability, created only by an
 * actual supported release; absent historical saves remain byte-identical. */
final class PcDuelRelease {
    static final String NAMESPACE="pc-duel-return-v1";
    private static final int MAGIC=0x50445231;
    static final class Row {
        final int id,nativeId,home,owner;int current,duration;
        Row(int id,int nativeId,int home,int owner,int current,int duration){this.id=id;this.nativeId=nativeId;this.home=home;this.owner=owner;this.current=current;this.duration=duration;}
    }
    private static boolean enabled(World w){byte[]b=w.extensions.get(NAMESPACE);return b!=null&&b.length>=4&&java.nio.ByteBuffer.wrap(b).getInt()==MAGIC;}
    /** Full source0 direct4b1950 keeps task37/duration0 until the next
     * personnel phase. Historical policies never acquire this capability. */
    private static boolean sameRegionRelease(World w,int origin,int home)throws IOException {
        return home>=0&&home<42&&origin==home&&PcDuelCampaignPolicy.enabled(w)&&PcDuelCampaignPolicy.read(w).version==3&&PcGovernorPolicy.data(w).format==3;
    }
    private static PcScenarioIdentity.Source source(World w)throws IOException {
        var source=PcScenarioIdentity.saved(w);var proven=PcScenarioCatalog.all().get(0).identity;
        if(source==null||!source.scenarioId.equals(proven.scenarioId)||!source.sha.equals(proven.sha)||!source.sourceVariant.equals(proven.sourceVariant))throw new IOException("原释放返程尚未核实此来源的初始化邻接");return source;
    }
    private static SortedMap<Integer,Row> read(World w)throws IOException {
        var rows=new TreeMap<Integer,Row>();if(!enabled(w))return rows;byte[]raw=w.extensions.get(NAMESPACE);if(raw.length>32768)throw new IOException("原返程保存过大");var d=new DataInputStream(new ByteArrayInputStream(raw));var src=source(w);
        if(d.readInt()!=MAGIC||d.readInt()!=1||!d.readUTF().equals(src.scenarioId)||!d.readUTF().equals(src.sha)||!d.readUTF().equals(src.sourceVariant))throw new IOException("原返程来源或保存版本不同");
        int count=d.readInt();if(count<0||count>670)throw new IOException("原返程人物数量无效");var facts=PcDuelSourceFacts.saved(w);var admin=PcGovernorPolicy.data(w);
        for(int i=0;i<count;i++){var r=new Row(d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt(),d.readInt());var o=w.officer(r.id);var f=facts.get(r.id);var a=admin.assignments.get(r.id);
            if(o==null||f==null||f.nativeId!=r.nativeId||a==null||a.home!=r.home||o.owner!=r.owner||o.unitId>=0||!w.life.present(r.id)||w.government.captive(r.id)||r.current<0||r.current>=42||r.home<0||r.home>=87||r.duration!=PcPersonnelReturnRules.duration(r.current,PcPersonnelReturnRules.parent(r.home))||r.duration<0||r.duration==0&&!sameRegionRelease(w,r.current,r.home)||o.cityId!=PcPersonnelReturnRules.projectSite(w,r.current)||rows.put(r.id,r)!=null)throw new IOException("原返程人物/所在地/行政驻点不同");}
        if(d.available()!=0)throw new IOException("原返程保存尾部未知");return rows;
    }
    private static void write(World w,SortedMap<Integer,Row>rows)throws IOException {
        var src=source(w);var bytes=new ByteArrayOutputStream();var d=new DataOutputStream(bytes);d.writeInt(MAGIC);d.writeInt(1);d.writeUTF(src.scenarioId);d.writeUTF(src.sha);d.writeUTF(src.sourceVariant);d.writeInt(rows.size());
        for(var r:rows.values()){d.writeInt(r.id);d.writeInt(r.nativeId);d.writeInt(r.home);d.writeInt(r.owner);d.writeInt(r.current);d.writeInt(r.duration);}w.extensions.put(NAMESPACE,bytes.toByteArray());
    }
    static void validate(World w)throws IOException {if(enabled(w))read(w);}
    /** Shared original task37 also used after a proven allegiance callback. */
    static void validateReturn(World w,int origin,int home)throws IOException {
        source(w);int duration=PcPersonnelReturnRules.duration(origin,PcPersonnelReturnRules.parent(home));
        if(home<0||home>=87||duration<0)throw new IOException("原归队目的地域无效");
        // Original4a8440 in source0 CITY keeps task37/duration0 until the next
        // personnel phase. Only explicit new human strategy adopts this branch;
        // historical strategy and same-parent gate/port remain unchanged.
        if(duration==0&&(!sameRegionRelease(w,origin,home)||!PcDuelHumanActorPolicy.enabled(w)))throw new IOException("当前保存未明确采用已核实的同城登用归队策略");
        if(duration==0)PcDuelHumanActorPolicy.validate(w);
        byte[]opaque=w.extensions.get(NAMESPACE);if(opaque!=null&&!enabled(w))throw new IOException("已有未知返程保存策略，不能覆盖");read(w);
    }
    static void startReturn(World w,int id,int origin,int home)throws IOException {
        validateReturn(w,origin,home);var o=w.officer(id);var f=PcDuelSourceFacts.saved(w).get(id);var rows=read(w);var a=PcGovernorPolicy.data(w).assignments.get(id);
        if(o==null||f==null||a==null||a.home!=home||o.unitId>=0||rows.containsKey(id))throw new IOException("原归队人物行政连接不同");
        o.cityId=PcPersonnelReturnRules.projectSite(w,origin);o.acted=true;rows.put(id,new Row(id,f.nativeId,home,o.owner,origin,PcPersonnelReturnRules.duration(origin,PcPersonnelReturnRules.parent(home))));write(w,rows);
    }
    static boolean busy(World w,int id){try{return enabled(w)&&read(w).containsKey(id);}catch(IOException e){throw new IllegalStateException(e);}}
    static int remaining(World w,int id){try{var r=read(w).get(id);return r==null?0:r.duration;}catch(IOException e){throw new IllegalStateException(e);}}
    static void validateRelease(World w,World.Unit winner,World.Unit loser,int id)throws IOException {
        source(w);var o=w.officer(id);if(winner==null||loser==null||w.unit(winner.id)!=winner||w.unit(loser.id)!=loser||o==null||!w.life.present(id)||w.government.captive(id)||o.unitId!=loser.id||!w.army.contains(loser,id)||!w.campaign.hostile(winner.owner,loser.owner)||busy(w,id))throw new IOException("原释放人物或当前部队不同");
        // Full source0 native5174b1950 retains ruler role/army/home, task37
        // and original unit removal. Other administrative branches stay bounded.
        if(o.role==Strategy.Role.DISTRICT||w.government.advisor(o.owner)==o)throw new IOException("原释放都督/军师额外行政回调尚未闭合");
        if(!PcGovernorPolicy.recognized(w))throw new IOException("原释放行政来源缺失");var data=PcGovernorPolicy.data(w);if(o.role==Strategy.Role.RULER&&(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3||data.format!=3))throw new IOException("旧保存没有明确君主释放策略，完整待办已保留");var assignment=data.assignments.get(id);if(assignment==null)throw new IOException("原释放人物行政连接缺失");int home=assignment.home;var city=w.city(PcPersonnelReturnRules.projectSite(w,home));
        if(city==null||city.owner!=o.owner||home<0||home>=87)throw new IOException("原释放驻点已失去时的备用据点选择尚未核实");int origin=PcPersonnelReturnRules.cityAt(w,loser.hex);
        int duration=PcPersonnelReturnRules.duration(origin,PcPersonnelReturnRules.parent(home));
        if(duration<0)throw new IOException("原释放返程地域无效");
        if(duration==0&&home>=42)throw new IOException("原同地域关港驻点释放回调尚未闭合");
        if(duration==0&&!sameRegionRelease(w,origin,home))throw new IOException("旧保存没有明确同地域释放策略，完整待办已保留");
        PcNativeItemPolicy.held(w,id);PcNativeItemPolicy.held(w,winner.officerId);if(loser.officerId==id)PcDuelReplacement.select(w,loser,id);
        byte[]opaque=w.extensions.get(NAMESPACE);if(opaque!=null&&!enabled(w))throw new IOException("已有未知返程保存策略，不能覆盖");read(w);
    }
    static void apply(World w,World.Unit winner,World.Unit loser,int id)throws IOException {
        validateRelease(w,winner,loser,id);var rows=read(w);var o=w.officer(id);var assignment=PcGovernorPolicy.data(w).assignments.get(id);int origin=PcPersonnelReturnRules.cityAt(w,loser.hex),home=assignment.home;var f=PcDuelSourceFacts.saved(w).get(id);
        var held=new ArrayList<>(w.treasures.held(id));for(var item:held)w.treasures.place(item.definition,Treasures.Place.OFFICER,winner.officerId);
        PcDuelReplacement.remove(w,loser,id);o.unitId=-1;o.cityId=PcPersonnelReturnRules.projectSite(w,origin);o.acted=true;
        // Unit deletion can create task37 rows for carried captives. Read the
        // resulting namespace before adding this officer, preserving all rows.
        rows=read(w);rows.put(id,new Row(id,f.nativeId,home,o.owner,origin,PcPersonnelReturnRules.duration(origin,PcPersonnelReturnRules.parent(home))));write(w,rows);w.governance.reconcile(false);
    }
    /** Global personnel settlement resets action, decrements travel time, then
     * advances one original neighbor. Travel table is recomputed at each hop. */
    static void tick(World w)throws IOException {
        if(!enabled(w))return;var rows=read(w);if(rows.isEmpty())return;
        for(var r:rows.values()){var home=w.city(PcPersonnelReturnRules.projectSite(w,r.home));if(home==null||home.owner!=r.owner)throw new IOException("原释放返程期间驻点失陷的到达回调尚未闭合，原任务已保留");}
        for(var r:new ArrayList<>(rows.values())){var o=w.officer(r.id);int destination=PcPersonnelReturnRules.parent(r.home),next=PcPersonnelReturnRules.nextCitySource0(r.current,destination);if(next<0)throw new IOException("原返程下一城无效");
            if(next==destination){o.cityId=PcPersonnelReturnRules.projectSite(w,r.home);o.acted=false;rows.remove(r.id);PcGovernorPolicy.arrived(w,o,w.city(o.cityId));w.note(o.name+"归队抵达"+w.city(o.cityId).name);}
            else {r.current=next;r.duration=PcPersonnelReturnRules.duration(next,destination);o.cityId=PcPersonnelReturnRules.projectSite(w,next);o.acted=true;}
        }write(w,rows);w.governance.reconcile(false);
    }
    private PcDuelRelease(){}
}
