package game.sanguo.core;
import java.io.*;
/** Verified linked original 4a93b0/4a5d90 branch. More original capture cases
 * remain required before the native campaign capability is initialized. */
final class PcDuelCapture {
    static void validate(World w,World.Unit winner,World.Unit loser,int target)throws IOException {validate(w,winner,loser,target,winner==null?-1:winner.officerId);}
    static void validate(World w,World.Unit winner,World.Unit loser,int target,int victor)throws IOException {
        World.Officer o=w.officer(target);if(winner==null||loser==null||w.unit(winner.id)!=winner||w.unit(loser.id)!=loser||o==null||!w.life.present(target)||w.government.captive(target)||o.unitId!=loser.id||!w.army.contains(loser,target)||!w.campaign.hostile(winner.owner,loser.owner))throw new IOException("原单挑俘虏的当前部队或人物不同");
        if(o.role==Strategy.Role.RULER)throw new IOException("原单挑君主被俘的势力回调尚未闭合");
        if(!w.army.contains(winner,victor)||w.officer(victor)==null||w.officer(victor).unitId!=winner.id)throw new IOException("原单挑获胜武将的当前编队不同");
        PcNativeItemPolicy.held(w,target);PcNativeItemPolicy.held(w,winner.officerId);
        // Original4a97bd ->4813e0 clears matching force+8 adviser only.
        // Government.capture/allegianceChanged already performs this clearing;
        // original full source466 capture confirms no replacement election.
        if(loser.officerId==target&&loser.deputies.length>0)PcDuelReplacement.select(w,loser,target);
        if(!PcGovernorPolicy.recognized(w))throw new IOException("原单挑俘虏行政来源缺失");
        var data=PcGovernorPolicy.data(w);var home=data.assignments.get(winner.officerId);if(!data.assignments.containsKey(target)||home==null||data.source.sites.values().stream().noneMatch(site->site.nativeId==home.home))throw new IOException("原单挑胜方行政驻点未知");
        int primary=PcArmyActionPolicy.primaryArmy(w,loser.owner);if(primary<0)throw new IOException("原被俘人物的旧势力第一军团未知");
        var previous=data.assignments.get(target);var nativeTarget=PcDuelSourceFacts.saved(w).get(target);if(nativeTarget==null)throw new IOException("原被俘人物军团稳定连接缺失");
        if(data.armyLeaders.getOrDefault(previous.army,-1)==nativeTarget.nativeId){
            for(var person:data.source.people.values())if(person.id<0&&person.allowed&&person.army==previous.army)throw new IOException("原军团继任存在未连接的有效人物");
            boolean cityPresent=cityPresent(w,data,previous.army,loser.owner);
            boolean remaining=false;for(var entry:data.assignments.entrySet()){var candidate=w.officer(entry.getKey());if(entry.getKey()!=target&&entry.getValue().army==previous.army&&candidate!=null&&candidate.owner==loser.owner&&w.life.present(candidate.id)&&!w.government.captive(candidate.id)){remaining=true;break;}}
            if(!cityPresent||!remaining)validateMerge(w,data,previous.army,primary,loser.owner,cityPresent);
        }

    }
    /** Caller validates all terminal branches before writing. Original captive
     * stays escorted by victor; old allegiance remains explicit in Prisoner. */
    static int apply(World w,World.Unit winner,World.Unit loser,int target)throws IOException {return apply(w,winner,loser,target,winner.officerId);}
    static int apply(World w,World.Unit winner,World.Unit loser,int target,int victor)throws IOException {
        validate(w,winner,loser,target,victor);World.Officer o=w.officer(target);var data=PcGovernorPolicy.data(w);int home=data.assignments.get(winner.officerId).home;
        var held=new java.util.ArrayList<>(w.treasures.held(target));w.government.capture(o,winner);for(var item:held)w.treasures.place(item.definition,Treasures.Place.OFFICER,winner.officerId);var assigned=data.assignments.get(target);if(assigned==null)throw new IOException("原单挑俘虏人物连接缺失");int previousArmy=assigned.army;boolean commander=data.armyLeaders.getOrDefault(previousArmy,-1)==PcDuelSourceFacts.saved(w).get(target).nativeId;
        // Original4a9538 moves a deployed captive to its OLD force's first
        // army, independently of the winner's home/escort location. Original
        // 4a9777/4be2a0 then selects the nonempty old army's next commander.
        assigned.home=home;assigned.army=PcArmyActionPolicy.primaryArmy(w,loser.owner);assigned.lastCity=-1;if(commander){boolean hasPerson=data.assignments.entrySet().stream().anyMatch(e->e.getValue().army==previousArmy&&w.life.present(e.getKey())&&!w.government.captive(e.getKey()));if(cityPresent(w,data,previousArmy,loser.owner)&&hasPerson)PcGovernorPolicy.reconcileArmy(w,data,previousArmy);else merge(w,data,previousArmy,assigned.army);}PcGovernorPolicy.write(w,data);
        int leader=PcDuelReplacement.remove(w,loser,target);w.governance.reconcile(false);return leader;
    }
    private static boolean cityPresent(World w,PcGovernorPolicy.Data data,int army,int owner){return w.cities.stream().anyMatch(city->city.kind==World.SiteKind.CITY&&city.owner==owner&&data.siteArmies.getOrDefault(city.id,-1)==army&&!data.unknownSites.contains(city.id));}
    static void validateMerge(World w,PcGovernorPolicy.Data data,int from,int to,int owner,boolean cityPresent)throws IOException {
        if(data.format!=3||!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3)throw new IOException(cityPresent?"旧保存没有明确空军团合并策略":"旧保存没有明确无城市军团合并策略");
        if(from==to||data.source.armyDisplays.getOrDefault(from,0)<=1)throw new IOException("原第一军团自身重整回调尚未闭合");
        if(data.mergedArmies.containsKey(from)||data.mergedArmies.containsKey(to)||!data.source.armyOriginalValid.get(to)||data.source.armyOwners.get(to)!=owner||data.source.armyDisplays.get(to)!=1)throw new IOException("原空军团合并目标来源无效");
        for(var e:data.assignments.entrySet())if(e.getValue().army==from&&w.life.present(e.getKey())&&w.officer(e.getKey()).owner!=owner)throw new IOException("原合并军团当前人员归属未闭合");
        for(var e:data.siteArmies.entrySet())if(e.getValue()==from&&(data.unknownSites.contains(e.getKey())||w.city(e.getKey()).owner!=owner))throw new IOException("原合并軍團当前据点归属未闭合");
        for(var e:data.unitArmies.entrySet())if(e.getValue()==from&&w.unit(e.getKey()).owner!=owner)throw new IOException("原合并军团当前部队归属未闭合");
    }
    private static void merge(World w,PcGovernorPolicy.Data data,int from,int to)throws IOException {
        PcArmyActionPolicy.clearArmy(w,from);int oldNative=data.armyLeaders.get(from);var original=data.source.people.get(oldNative);var old=original==null?null:w.officer(original.id);if(old!=null&&old.role==Strategy.Role.DISTRICT)old.role=Strategy.Role.OFFICER;
        for(var e:data.assignments.entrySet())if(e.getValue().army==from&&w.life.present(e.getKey()))e.getValue().army=to;
        for(var e:data.siteArmies.entrySet())if(e.getValue()==from){var city=w.city(e.getKey());var governor=w.officer(city.governorId);if(governor!=null&&governor.role==Strategy.Role.GOVERNOR)governor.role=Strategy.Role.OFFICER;city.governorId=-1;data.vacantSites.add(city.id);e.setValue(to);}data.unitArmies.replaceAll((id,army)->army==from?to:army);data.armyLeaders.put(from,-1);data.mergedArmies.put(from,to);
    }
    private PcDuelCapture(){}
}
