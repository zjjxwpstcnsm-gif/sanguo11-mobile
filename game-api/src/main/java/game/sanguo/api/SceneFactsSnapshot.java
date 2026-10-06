package game.sanguo.api;

import java.util.*;

/** Detached current rule facts and stored provenance. No render objects or media manifests. */
public final class SceneFactsSnapshot {
    public static final class Cell {
        public final int q,r,sourceX,sourceY;
        public Cell(int q,int r,int sourceX,int sourceY){this.q=q;this.r=r;this.sourceX=sourceX;this.sourceY=sourceY;}
    }
    public static final class Source {
        public final String id,name,variant,path,sha,sharedSha;
        public final List<String> unknown;
        public Source(String id,String name,String variant,String path,String sha,String sharedSha,List<String> unknown){
            this.id=id;this.name=name;this.variant=variant;this.path=path;this.sha=sha;this.sharedSha=sharedSha;this.unknown=List.copyOf(unknown);
        }
    }
    public static final class Fire {
        public final Cell cell;public final int owner,remaining,power;public final boolean trap;
        public Fire(Cell cell,int owner,int remaining,int power,boolean trap){this.cell=cell;this.owner=owner;this.remaining=remaining;this.power=power;this.trap=trap;}
    }
    public static final class Military {
        public final int id,owner,hp,maxHp,builderUnitId,direction;public final Cell cell;
        public final String kind;public final boolean complete;
        public Military(int id,int owner,String kind,Cell cell,int hp,int maxHp,int builderUnitId,int direction,boolean complete){
            this.id=id;this.owner=owner;this.kind=kind;this.cell=cell;this.hp=hp;this.maxHp=maxHp;this.builderUnitId=builderUnitId;this.direction=direction;this.complete=complete;
        }
    }
    public static final class Domestic {
        public final int id,cityId,owner,hp,maxHp,builderOfficerId,remaining,level,upgradeTo,lastUseTurn;
        public final Cell cell;public final String kind;
        public Domestic(int id,int cityId,int owner,String kind,Cell cell,int hp,int maxHp,int builderOfficerId,int remaining,int level,int upgradeTo,int lastUseTurn){
            this.id=id;this.cityId=cityId;this.owner=owner;this.kind=kind;this.cell=cell;this.hp=hp;this.maxHp=maxHp;this.builderOfficerId=builderOfficerId;this.remaining=remaining;this.level=level;this.upgradeTo=upgradeTo;this.lastUseTurn=lastUseTurn;
        }
    }
    public static final class Site {
        public final int id,owner,governorOfficerId,districtId,gold,food,troops,hp,maxHp;
        public final String name,kind;public final Cell center;public final List<Cell> footprint;
        public Site(int id,int owner,String name,String kind,Cell center,List<Cell> footprint,int governorOfficerId,int districtId,int gold,int food,int troops,int hp,int maxHp){
            this.id=id;this.owner=owner;this.name=name;this.kind=kind;this.center=center;this.footprint=List.copyOf(footprint);this.governorOfficerId=governorOfficerId;this.districtId=districtId;this.gold=gold;this.food=food;this.troops=troops;this.hp=hp;this.maxHp=maxHp;
        }
    }
    public static final class Faction {
        public final int id,rulerOfficerId;public final String name,nation,title;public final boolean alive;
        /** Source fields are original opening fields, not current force mutation state. */
        public final Map<Integer,Integer> originalFields;
        public Faction(int id,String name,String nation,String title,int rulerOfficerId,boolean alive,Map<Integer,Integer> originalFields){
            this.id=id;this.name=name;this.nation=nation;this.title=title;this.rulerOfficerId=rulerOfficerId;this.alive=alive;this.originalFields=Collections.unmodifiableMap(new TreeMap<>(originalFields));
        }
    }
    public static final class NativeArmy {
        public final int nativeId,owner,display,leaderNativeId,leaderOfficerId,openingLeaderNativeId;
        public NativeArmy(int nativeId,int owner,int display,int leaderNativeId,int leaderOfficerId,int openingLeaderNativeId){this.nativeId=nativeId;this.owner=owner;this.display=display;this.leaderNativeId=leaderNativeId;this.leaderOfficerId=leaderOfficerId;this.openingLeaderNativeId=openingLeaderNativeId;}
    }
    public static final class Administration {
        public final boolean originalElectionEnabled;public final List<NativeArmy> armies;
        public final Map<Integer,Integer> siteArmies,unitArmies,officerArmies,officerAdministrativeHomeNative,siteNativeIds;
        public final Set<Integer> unknownSites;public final List<String> unknown;
        public Administration(boolean enabled,List<NativeArmy> armies,Map<Integer,Integer> sites,Map<Integer,Integer> units,Map<Integer,Integer> officers,Map<Integer,Integer> homes,Set<Integer> unknownSites,List<String> unknown){this(enabled,armies,sites,units,officers,homes,unknownSites,unknown,Map.of());}
        public Administration(boolean enabled,List<NativeArmy> armies,Map<Integer,Integer> sites,Map<Integer,Integer> units,Map<Integer,Integer> officers,Map<Integer,Integer> homes,Set<Integer> unknownSites,List<String> unknown,Map<Integer,Integer> nativeSites){siteNativeIds=Collections.unmodifiableMap(new TreeMap<>(nativeSites));originalElectionEnabled=enabled;this.armies=List.copyOf(armies);siteArmies=Collections.unmodifiableMap(new TreeMap<>(sites));unitArmies=Collections.unmodifiableMap(new TreeMap<>(units));officerArmies=Collections.unmodifiableMap(new TreeMap<>(officers));officerAdministrativeHomeNative=Collections.unmodifiableMap(new TreeMap<>(homes));this.unknownSites=Collections.unmodifiableSet(new TreeSet<>(unknownSites));this.unknown=List.copyOf(unknown);}
        public static Administration unavailable(){return new Administration(false,List.of(),Map.of(),Map.of(),Map.of(),Map.of(),Set.of(),List.of("originalAdministrativeStrategyAbsent"));}
    }
    public static final class Relationship {
        public final int a,b,value,expires;public final boolean hostile;public final String treaty;
        public Relationship(int a,int b,int value,boolean hostile,String treaty,int expires){this.a=a;this.b=b;this.value=value;this.hostile=hostile;this.treaty=treaty;this.expires=expires;}
    }
    public final Administration administration;
    public final StateToken state;public final boolean available;
    public final String mapId,scenarioId,scenarioName,dataSource,dataHash;
    public final int mapRevision,terrainRevision,turn,player,year,month,period;
    public final Source source;public final List<Fire> fires;public final List<Military> military;
    public final List<Domestic> domestic;public final List<Site> sites;public final List<Faction> factions;
    public final List<Relationship> relationships;public final List<String> unknown;
    public SceneFactsSnapshot(StateToken state,boolean available,String mapId,int mapRevision,int terrainRevision,String scenarioId,String scenarioName,String dataSource,String dataHash,int turn,int player,int year,int month,int period,Source source,List<Fire> fires,List<Military> military,List<Domestic> domestic,List<Site> sites,List<Faction> factions,List<Relationship> relationships,List<String> unknown){
        this(state,available,mapId,mapRevision,terrainRevision,scenarioId,scenarioName,dataSource,dataHash,turn,player,year,month,period,source,fires,military,domestic,sites,factions,relationships,unknown,Administration.unavailable());
    }
    public SceneFactsSnapshot(StateToken state,boolean available,String mapId,int mapRevision,int terrainRevision,String scenarioId,String scenarioName,String dataSource,String dataHash,int turn,int player,int year,int month,int period,Source source,List<Fire> fires,List<Military> military,List<Domestic> domestic,List<Site> sites,List<Faction> factions,List<Relationship> relationships,List<String> unknown,Administration administration){
        this.administration=Objects.requireNonNull(administration);this.state=Objects.requireNonNull(state);this.available=available;this.mapId=mapId;this.mapRevision=mapRevision;this.terrainRevision=terrainRevision;this.scenarioId=scenarioId;this.scenarioName=scenarioName;this.dataSource=dataSource;this.dataHash=dataHash;this.turn=turn;this.player=player;this.year=year;this.month=month;this.period=period;this.source=source;this.fires=List.copyOf(fires);this.military=List.copyOf(military);this.domestic=List.copyOf(domestic);this.sites=List.copyOf(sites);this.factions=List.copyOf(factions);this.relationships=List.copyOf(relationships);this.unknown=List.copyOf(unknown);
    }
    public static SceneFactsSnapshot unsupported(StateToken state){return new SceneFactsSnapshot(state,false,"",0,0,"","","","",0,-1,0,0,0,null,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of("sceneFactsUnsupported"));}
}
