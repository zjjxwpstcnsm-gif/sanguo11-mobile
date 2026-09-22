package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Exact facility snapshot / invalidation regression, not a model-art acceptance test. */
public final class SceneFacilityStateTest {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    static MapSceneSnapshot snapshot(World w,MapSceneSnapshot.Ground g,Hex h){return new MapSceneSnapshot(g,w,h,-1);}
    static MapSceneSnapshot.Item item(MapSceneSnapshot s,String key){return s.items.stream().filter(i->i.key.equals(key)).findFirst().orElse(null);}
    public static void main(String[] args)throws Exception{
        World w=SceneFacilityFixture.create();MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
        MapSceneSnapshot first=snapshot(w,g,null);Set<String> types=new HashSet<>();
        World actual=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(actual);
        snapshot(actual,new MapSceneSnapshot.Ground(actual),actual.cities.get(0).hex);
        for(Domestic.Facility f:w.domestic.facilities){
            MapSceneSnapshot.Item i=item(first,"domestic:"+f.id);types.add(i.facility.type);
            check(i.facility.level==f.level&&i.facility.hp==f.hp,"all domestic levels preserved");
            check(i.facility.owner==w.city(f.cityId).owner,"domestic owner follows home city");
            check(i.facility.type.equals("domestic/"+f.kind.name()),"domestic type preserved");
        }
        for(War.Structure s:new ArrayList<>(w.war.structures())){
            MapSceneSnapshot.Item i=item(first,"structure:"+s.id);types.add(i.facility.type);
            check(i.facility.type.equals("military/"+s.kind.name()),"military type preserved");
            check(i.facility.maxHp==s.kind.hp,"military durability scale");
            check(snapshot(w,g,s.hex).coverage.equals(new HashSet<>(w.fieldworks.coverage(s))),"core coverage reused for "+s.kind);
        }
        check(types.size()==Domestic.Kind.values().length+War.StructureKind.values().length,"no silent enum omission");
        check(Arrays.equals(before,SaveCodec.encode(actual)),"snapshots and coverage preserve authoritative state");
        Domestic.Facility f=w.domestic.facilities.get(0);String key="domestic:"+f.id;
        MapSceneSnapshot.Item original=item(first,key);
        f.remaining=2;f.upgradeTo=2;f.hp=340;SceneFacilityFixture.ignite(w,f.hex);
        MapSceneSnapshot.Item changed=item(snapshot(w,g,f.hex),key);
        check(!changed.facility.complete&&changed.facility.upgradeTo==2&&changed.facility.remaining==2,"upgrade in progress");
        check(changed.facility.burning&&changed.facility.hp==340,"fire and damage synchronise");
        check(changed.displayLabel().contains("起火")&&changed.displayLabel().contains("340/1000"),"production label exposes status");
        check(original.facility.hp==1000&&!original.facility.burning&&original.facility.complete,"old snapshot immutable");
        check(original.shapeKey().equals(changed.shapeKey()),"status changes do not expand shared mesh cache");
        f.remaining=0;f.upgradeTo=0;f.level=2;f.hp=f.maxHp();SceneFacilityFixture.extinguish(w);
        changed=item(snapshot(w,g,null),key);
        check(changed.facility.complete&&changed.facility.level==2&&!changed.facility.burning&&changed.facility.hp==1000,"completed repaired state");
        w.city(f.cityId).owner=(w.city(f.cityId).owner+1)%w.factions.length;
        changed=item(snapshot(w,g,null),key);
        check(changed.facility.owner==w.city(f.cityId).owner&&changed.color==FactionColors.color(w,changed.facility.owner),"ownership changes color");
        w.domestic.facilities.remove(f);check(item(snapshot(w,g,null),key)==null,"deleted facility disappears");
        War.Structure s=w.war.structures().get(0);key="structure:"+s.id;s.kind=War.StructureKind.FORTRESS;s.complete=false;s.hp=210;s.direction=4;
        changed=item(snapshot(w,g,s.hex),key);
        check(changed.facility.type.equals("military/FORTRESS")&&!changed.facility.complete&&changed.facility.direction==4&&changed.facility.hp==210,"military upgrade and direction synchronise");
        SceneFacilityFixture.remove(w,s);check(item(snapshot(w,g,null),key)==null,"deleted structure disappears");
        check(g.matches(w),"facility changes reuse static terrain");
        System.out.println("PASS facility snapshot: "+checks+" assertions; "+types.size()+" actual types; models remain S01 placeholders");
    }
}
