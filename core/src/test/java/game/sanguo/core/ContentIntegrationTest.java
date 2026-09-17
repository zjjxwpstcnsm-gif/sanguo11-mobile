package game.sanguo.core;

import java.util.*;

/** Cross-branch contract: actual sourced officers trigger rules and stay independent of later static data. */
public final class ContentIntegrationTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        ContentCatalog catalog=ContentCatalog.get();ContentRuntime.validate(catalog);
        for(ContentCatalog.Entry e:catalog.rows("skills"))check(ContentRuntime.skill(e.id)!=null,"explicit mapping "+e.id);
        World w=TestScenarios.load("officer-reference-drill",0);
        for(World.Officer o:w.officers){ContentCatalog.Officer d=catalog.officer(o.id);check(o.skillId.equals(d.skillId.equals("none")?"none":ContentRuntime.skill(d.skillId).id),"source skill becomes runtime state");check(o.sex!=World.Sex.UNKNOWN,"known gender binds");}
        check(w.officer(1004).skillId.equals(Skill.SHENSUAN.id)&&w.officer(3001).skillId.equals(Skill.HUOSHEN.id),"sourced Zhuge Liang and Zhou Yu retain distinct effects");
        World.Unit source=new World.Unit(w.nextUnitId++,0,1004,World.Weapon.SPEAR,new Hex(4,4),3000,12000);
        World.Unit target=new World.Unit(w.nextUnitId++,1,2000,World.Weapon.SPEAR,new Hex(5,4),3000,12000);
        for(World.Unit unit:Arrays.asList(source,target)){World.Officer o=w.officer(unit.officerId);w.strategy.releaseGovernor(o.id);o.cityId=-1;o.unitId=unit.id;w.units.add(unit);}
        check(w.war.plotChance(source.id,target.hex,War.Plot.CONFUSE)==100,"sourced 神算 beats sourced 虚实 intelligence");
        byte[] preview=SaveCodec.encode(w);w.war.plotChance(source.id,target.hex,War.Plot.CONFUSE);check(Arrays.equals(preview,SaveCodec.encode(w)),"combined data/rule preview is pure");
        World copy=SaveCodec.decode(preview);check(w.war.plot(source.id,target.hex,War.Plot.CONFUSE).ok&&copy.war.plot(source.id,target.hex,War.Plot.CONFUSE).ok,"real sourced plot executes");
        check(target.statusTurns==2&&source.energy==65&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"sourced critical/cost and restored result agree");
        w.officer(1004).skillId="future.retrained";w.officer(1004).sex=World.Sex.FEMALE;w.scenarioId="retired-pack";
        World restored=SaveCodec.decode(SaveCodec.encode(w));check(restored.officer(1004).skillId.equals("future.retrained")&&restored.officer(1004).sex==World.Sex.FEMALE,"load never rebinds static original skill/gender");
        World old=TestScenarios.load("regional-sandbox",0);check(old.officers.stream().allMatch(o->o.skillId.equals("none")&&o.sex==World.Sex.UNKNOWN),"original scenarios do not silently acquire new static people/skills");
        System.out.println("PASS: "+checks+" content/runtime integration assertions.");
    }
}
