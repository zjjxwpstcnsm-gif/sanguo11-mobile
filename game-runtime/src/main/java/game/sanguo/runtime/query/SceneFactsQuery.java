package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;

/** Serial projection only. No command, save, current catalog lookup, RNG or journal creation. */
public final class SceneFactsQuery {
    private SceneFactsQuery(){}
    private static SceneFactsSnapshot.Cell cell(World w,Hex h){
        var s=MapCoordinates.nationalSource(w,h);return new SceneFactsSnapshot.Cell(h.q,h.r,s.x,s.y);
    }
    public static SceneFactsSnapshot capture(World w,StateToken state){
        List<String> unknown=new ArrayList<>(List.of("originalSceneId","originalRegionBoundaries","originalFacilityDurabilityCompleteParity","originalDialogueSpeakers"));
        SceneFactsSnapshot.Source source=null;Map<Integer,Map<Integer,Integer>> forces=Map.of();
        try{
            PcScenarioIdentity.Source saved=PcScenarioIdentity.saved(w);
            if(saved!=null)source=new SceneFactsSnapshot.Source(saved.scenarioId,saved.name,saved.sourceVariant,saved.path,saved.sha,saved.sharedSha,saved.unknown);
            forces=PcScenarioPeople.savedForces(w);
        }catch(java.io.IOException invalid){unknown.add("savedSourceMetadataUnreadable");}
        List<SceneFactsSnapshot.Fire> fires=new ArrayList<>();
        for(War.Fire f:w.war.fires())fires.add(new SceneFactsSnapshot.Fire(cell(w,f.hex),f.owner,f.remaining,f.power,f.trap));
        List<SceneFactsSnapshot.Military> military=new ArrayList<>();
        for(War.Structure s:w.war.structures())military.add(new SceneFactsSnapshot.Military(s.id,s.owner,s.kind.name(),cell(w,s.hex),s.hp,s.kind.hp,s.builder,s.direction,s.complete));
        List<SceneFactsSnapshot.Domestic> domestic=new ArrayList<>();
        for(Domestic.Facility f:w.domestic.facilities){World.City c=w.city(f.cityId);domestic.add(new SceneFactsSnapshot.Domestic(f.id,f.cityId,c==null?-1:c.owner,f.kind.name(),cell(w,f.hex),f.hp,f.maxHp(),f.builderId,f.remaining,f.level,f.upgradeTo,f.lastUseTurn));}
        List<SceneFactsSnapshot.Site> sites=new ArrayList<>();
        for(World.City c:w.cities){List<SceneFactsSnapshot.Cell> footprint=new ArrayList<>();for(Hex h:SiteFootprint.cells(c))footprint.add(cell(w,h));Districts.District d=w.districts.city(c.id);
            sites.add(new SceneFactsSnapshot.Site(c.id,c.owner,c.name,c.kind.name(),cell(w,c.hex),footprint,c.governorId,d==null?-1:d.id,c.gold,c.food,c.troops,c.defense,w.campaign.defenseCap(c)));
        }
        List<SceneFactsSnapshot.Faction> factions=new ArrayList<>();
        for(int i=0;i<w.factions.length;i++){World.Officer ruler=w.loyalty.ruler(i);factions.add(new SceneFactsSnapshot.Faction(i,w.faction(i),w.governance.nation(i),w.governance.title(i),ruler==null?-1:ruler.id,w.alive(i),forces.getOrDefault(i,Map.of())));}
        List<SceneFactsSnapshot.Relationship> relationships=new ArrayList<>();
        for(int a=0;a<w.factions.length;a++)for(int b=a+1;b<w.factions.length;b++){Campaign.Treaty treaty=w.campaign.treaty(a,b);relationships.add(new SceneFactsSnapshot.Relationship(a,b,w.strategy.factionRelation(a,b),w.campaign.hostile(a,b),treaty==null?"":treaty.kind.name(),treaty==null?-1:treaty.expires));}
        var current=PcGovernorPolicy.view(w);List<SceneFactsSnapshot.NativeArmy> nativeArmies=new ArrayList<>();
        for(var army:current.armies)nativeArmies.add(new SceneFactsSnapshot.NativeArmy(army.originalValid,army.nativeId,army.owner,army.display,army.leaderNativeId,army.leaderOfficerId,army.openingLeaderNativeId,PcArmyActionPolicy.points(w,army.nativeId)));
        var administration=new SceneFactsSnapshot.Administration(current.enabled,nativeArmies,current.siteArmies,current.unitArmies,current.officerArmies,current.officerAdministrativeHomeNative,current.unknownSites,List.of("originalArmyActionBudget","originalArmyControllerAndDelegation","originalNewArmyAllocation","originalTemplateUnitArmy"),current.siteNativeIds);
        int monthIndex=w.startMonth-1+w.turn/3;
        return new SceneFactsSnapshot(state,true,w.mapId,w.mapRevision,w.terrainRevision,w.scenarioId,w.scenarioName,w.dataSource,w.dataHash,w.turn,w.player,w.startYear+monthIndex/12,monthIndex%12+1,w.turn%3,source,fires,military,domestic,sites,factions,relationships,unknown,administration);
    }
}
