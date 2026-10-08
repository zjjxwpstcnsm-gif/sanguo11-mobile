package game.sanguo.core;
import game.sanguo.core.map.TerrainCode;
import java.io.*;
import java.util.*;

/** Explicit installation-candidate import. No saved world calls this initializer. */
final class PcScenarioOpening {
    private PcScenarioOpening(){}
    static World create(PcScenarioCatalog.Source source,int player,long seed)throws IOException{
        Properties map=new Properties();map.setProperty("map",NationalMap.RESOURCE);
        for(PcScenarioCatalog.Site site:source.sites)map.setProperty("city."+site.nativeId,site.id+"|"+site.name+"|0|0");
        NationalMap.Selection geography=NationalMap.attach(map);int sourceRows=Integer.parseInt(map.getProperty("height"));int sourceColumns=MapCoordinates.normalize(map);
        Map<Integer,PcScenarioPeople.Person> byNative=new HashMap<>();for(PcScenarioPeople.Person p:source.people)byNative.put(p.nativeId,p);
        String[] factions=new String[47];
        for(PcScenarioCatalog.Force force:source.forces){
            PcScenarioPeople.Person ruler=force.valid?byNative.get(force.value(3)):null;
            factions[force.nativeId]=ruler==null||ruler.originalName.isEmpty()?"未启用势力槽 "+force.nativeId:
                ruler.officerId>=0?PcOfficerIdentities.openingName(source,ruler):ruler.originalName;
        }
        World w=PcScenarioIdentity.create(Integer.parseInt(map.getProperty("width")),Integer.parseInt(map.getProperty("height")),factions,source.identity);
        w.sourceMapWidth=sourceColumns;w.sourceMapHeight=sourceRows;w.columnStaggered=true;geography.apply(w);
        for(int row=0;row<w.height;row++){String line=map.getProperty("terrain."+row);for(int column=0;column<w.width;column++)w.terrain[column][row]=TerrainCode.decode(line.charAt(column));}
        for(PcScenarioCatalog.Site site:source.sites){
            Hex original=MapCoordinates.axial(w,new game.sanguo.core.map.SourceGridCoord(site.value(9),site.value(10)));
            Hex hex=original; // Source coordinates, never the community name/coordinate table.
            World.City city=new World.City(site.id,site.name,hex,site.value(4));city.kind=World.SiteKind.values()[site.value(3)];
            city.gold=site.value(17);city.food=site.value(18);city.troops=site.value(19);city.defense=site.value(5);city.baseDefense=site.value(11);city.morale=site.value(20);city.merchantRate=site.rate;
            // Gate/port order is not an original applicable field: keep the raw-1 in source catalog, core placeholder0.
            city.order=city.kind==World.SiteKind.CITY?site.value(32):0;
            for(World.Weapon weapon:World.Weapon.values())city.equipment[weapon.ordinal()]=site.value(45+PcProduction.nativeItem(weapon));
            city.ships[0]=site.value(55);city.ships[1]=site.value(56);w.cities.add(city);
        }
        for(Map.Entry<Integer,List<int[]>> entry:source.plots.entrySet()){
            List<Hex> parcels=new ArrayList<>();for(int[] xy:entry.getValue())parcels.add(MapCoordinates.axial(w,new game.sanguo.core.map.SourceGridCoord(xy[0],xy[1])));w.development.configure(entry.getKey(),parcels);
        }
        for(PcScenarioCatalog.Site site:source.sites)if(site.nativeId>=42)w.siteParents.put(site.id,site.parent);
        Map<Integer,World.Officer> officers=new HashMap<>();
        for(PcScenarioPeople.Person original:source.people)if(original.officerId>=0){
            int status=original.field(20),owner=original.field(75),location=original.field(4),home=original.field(3);
            boolean present=status<=5;if(status==7)home=location;
            if(status<0||status>8)throw new IOException("原人物状态未支持："+original.nativeId);
            if(home<-1||home>=source.sites.size()||present&&(location<0||location>=source.sites.size()))throw new IOException("原人物位置不是已核实据点："+original.nativeId);
            int city=present?source.sites.get(location).id:-1;
            World.Officer o=new World.Officer(original.officerId,PcOfficerIdentities.openingName(source,original),present?owner:-1,city,original.field(25),original.field(26),original.field(27),original.field(28),original.field(29));
            o.sex=original.field(6)==0?World.Sex.MALE:original.field(6)==1?World.Sex.FEMALE:World.Sex.UNKNOWN;
            o.role=status==0?Strategy.Role.RULER:owner>=0&&present?Strategy.Role.OFFICER:Strategy.Role.UNAFFILIATED;
            o.loyalty=o.owner<0?0:original.field(23);o.acted=!present;
            for(int i=0;i<6;i++)o.aptitude[i]=original.field(116+i);
            int skill=original.field(42);o.skillId=skill<0?"none":Objects.requireNonNull(ContentRuntime.skill(String.format(Locale.ROOT,"skill-%03d",skill)),"Unmapped source skill").id;
            o.affinity=original.field(17);o.honor=original.field(44)+1; // Native0..4, existing saved/editor scale1..5; raw ordinal remains in source facts.
            w.officers.add(o);officers.put(original.nativeId,o);
            Lifecycle.State state=status==8?Lifecycle.State.DEAD:status==7?Lifecycle.State.UNDISCOVERED:status==6?Lifecycle.State.SOURCE_WAIT:Lifecycle.State.ACTIVE;
            Lifecycle.Life life=new Lifecycle.Life(o.id,original.field(8),original.field(7),original.field(9),home<0?-1:source.sites.get(home).id,state);w.life.people.put(o.id,life);
            if(original.field(24)>0)w.government.merits.put(o.id,original.field(24));
        }
        // Native references are joined through the checked per-source map. No10000+native shortcut.
        for(PcScenarioPeople.Person original:source.people){World.Officer o=officers.get(original.nativeId);if(o==null)continue;
            for(int field:new int[]{12,13,15,16,106,107,108,109,110,111,112,113,114,115}){
                int target=original.field(field);if(target<0||field==16&&target==original.nativeId)continue;World.Officer other=officers.get(target);
                if(other==null)continue; // Kept verbatim in saved source facts; never invent/cast an external person.
                Relations.Kind kind=field==12?Relations.Kind.FATHER:field==13?Relations.Kind.MOTHER:field==15?Relations.Kind.SPOUSE:field==16?Relations.Kind.SWORN:field<=110?Relations.Kind.LIKE:Relations.Kind.DISLIKE;
                if(!w.relations.links(o.id,kind).contains(other.id))w.relations.link(o.id,other.id,kind);
            }
            int rank=original.field(21);
            if(rank>=0&&rank<80&&o.owner>=0&&o.role!=Strategy.Role.RULER)w.government.ranks.put(o.id,PcOfficerRanks.all().get(rank).projectId);
        }
        for(PcScenarioCatalog.Force force:source.forces)if(force.valid&&force.nativeId<42){
            World.Officer ruler=officers.get(force.value(3));if(ruler==null||ruler.owner!=force.nativeId||ruler.role!=Strategy.Role.RULER)throw new IOException("原势力君主连接未闭合："+force.nativeId);
            int advisor=force.value(4);if(advisor>=0){World.Officer o=officers.get(advisor);if(o==null)throw new IOException("原军师身份未闭合");w.government.advisors.put(force.nativeId,o.id);}
            w.campaign.points.put(force.nativeId,force.value(16));w.governance.grades.put(force.nativeId,9-force.value(5));
        }
        for(PcScenarioCatalog.Site site:source.sites){int governor=site.value(14);if(governor<0)continue;World.Officer o=officers.get(governor);World.City city=w.city(site.id);
            if(o==null||o.cityId!=city.id||o.owner!=city.owner)throw new IOException("原太守位置/所属连接未闭合："+site.name);
            city.governorId=o.id;if(o.role!=Strategy.Role.RULER)o.role=Strategy.Role.GOVERNOR;
        }
        PcContestProfiles.initializeOpening(w,source);
        w.officerAbilities.initializeOpening(null,false,false);
        for(PcScenarioPeople.Person original:source.people){World.Officer o=officers.get(original.nativeId);if(o==null)continue;OfficerAbilities.Profile p=o.abilityProfile;
            p.sourceId=o.id;p.nativeIds=Integer.toString(original.nativeId);p.sourceBirth=original.field(8);
            for(int i=0;i<5;i++){p.growth[i]=original.field(35+i);p.experience[i]=original.field(30+i);}
        }
        w.officerAbilities.refresh();w.merchantMarket.initializeSource();w.pcProduction.initializeOpening();w.pcTechniquePoints.initializeOpening();
        w.abilities.initialize(seed);PcScenarioPeople.attach(w,source.people);PcScenarioPeople.attachOpening(w,source);PcOfficerSources.attachOpening(w,source.identity.scenarioId);
        PcOfficerCampaignFacts.initializeOpening(w,source);PcOfficerIdentities.initializeOpening(w,source);PcMilitaryCostPolicy.initializeOpening(w);PcCommandCapacityPolicy.initializeOpening(w);
        NaturalStructures.seedOpening(w);w.invalidateSiteIndex();
        if(player<0){for(int side=0;side<42;side++)if(w.alive(side)){player=side;break;}}
        if(player<0||player>=42||!source.forces.get(player).valid||!w.alive(player))throw new IOException("原来源没有该可选势力");w.player=player;w.active=player;
        PcGovernorPolicy.initializeOpening(w);
        PcArmyActionPolicy.initializeOpening(w);
        PcDebateCampaignPolicy.initializeOpening(w);
        PcSearchPolicy.initializeOpening(w);
        PcDirectRecruitmentPolicy.initializeOpening(w);
        PcDuelSourceFacts.initializeOpening(w,source);
        PcDuelHealthPolicy.initializeOpening(w);
        PcDuelRuntimeFacts.initializeOpening(w);
        PcNativeItemPolicy.initializeOpening(w);
        PcDuelRawLoyalty.initializeOpening(w);
        PcRecruitmentBanPolicy.initializeOpening(w);
        PcDuelKinship.initializeOpening(w);
        PcSourceTechnologyPolicy.initializeOpening(w,source);
        SaveCodec.validate(w);
        w.note("安装来源候选开局："+source.identity.path+"；已导入据点/库存与人物记录，完整原事件和部分规则仍未核实。");return w;
    }
}
