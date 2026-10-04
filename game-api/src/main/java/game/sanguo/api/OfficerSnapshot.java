package game.sanguo.api;

import java.util.*;

/** Immutable stored-world facts. Missing historical/source fields stay absent. */
public final class OfficerSnapshot {
    public static final class Officer {
        public final int id,owner,cityId,unitId,loyalty,merit,commandLimit,treasureCount,injury,injuryTurns;
        public final boolean present;
        public final String name,sex,role,faction,location,status,office,skillId,skillName,skillDescription;
        public final String lifeDescription,loyaltyDescription,relationsDescription,treasuresDescription;
        public final List<Integer> current,base,growth,experience,aptitudes;
        public final List<String> unknown;
        public Officer(int id,int owner,int cityId,int unitId,int loyalty,int merit,int commandLimit,int treasureCount,
                       int injury,int injuryTurns,boolean present,String name,String sex,String role,String faction,String location,
                       String status,String office,String skillId,String skillName,String skillDescription,
                       String lifeDescription,String loyaltyDescription,String relationsDescription,String treasuresDescription,
                       List<Integer> current,List<Integer> base,List<Integer> growth,List<Integer> experience,
                       List<Integer> aptitudes,List<String> unknown){
            this.id=id;this.owner=owner;this.cityId=cityId;this.unitId=unitId;this.loyalty=loyalty;this.merit=merit;
            this.commandLimit=commandLimit;this.treasureCount=treasureCount;this.injury=injury;this.injuryTurns=injuryTurns;
            this.present=present;
            this.name=name;this.sex=sex;this.role=role;this.faction=faction;this.location=location;this.status=status;
            this.office=office;this.skillId=skillId;this.skillName=skillName;this.skillDescription=skillDescription;
            this.lifeDescription=lifeDescription;this.loyaltyDescription=loyaltyDescription;
            this.relationsDescription=relationsDescription;this.treasuresDescription=treasuresDescription;
            this.current=List.copyOf(current);this.base=List.copyOf(base);this.growth=List.copyOf(growth);
            this.experience=List.copyOf(experience);this.aptitudes=List.copyOf(aptitudes);this.unknown=List.copyOf(unknown);
        }
        public String searchText(){return name+" "+role+" "+office+" 功绩"+merit+" 指挥"+commandLimit+" 宝物"+treasureCount+
            " "+skillName+" "+faction+" "+location+" "+status;}
    }
    public final StateToken state;
    public final List<Officer> officers;
    private final Map<Integer,Officer> byId;
    public OfficerSnapshot(StateToken state,List<Officer> officers){
        this.state=Objects.requireNonNull(state);this.officers=List.copyOf(officers);
        Map<Integer,Officer> index=new HashMap<>();
        for(Officer o:officers)if(index.put(o.id,o)!=null)throw new IllegalArgumentException("Duplicate officer ID");
        byId=Collections.unmodifiableMap(index);
    }
    public Officer officer(int id){return byId.get(id);}
}
