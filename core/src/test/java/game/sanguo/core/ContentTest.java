package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Checks values used by gameplay, not just the presence of packaged files. */
public final class ContentTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        long start=System.nanoTime();ContentCatalog c=ContentCatalog.get();long load=System.nanoTime()-start;ContentIntegrationTest.main(args);
        check(c.officers().size()==670&&c.rows("sites").size()==87,"source catalog coverage");
        check(c.rows("scenarios").size()==14&&c.rows("scenarios").stream().allMatch(e->e.fields.get(5).equals("incomplete")),"no invented official start");
        check(c.officer(3001).stat(0)==97&&c.officer(3001).aptitudeText().equals("AASCCS"),"Zhou Yu cross-source values");
        check(c.officer(1004).stat(2)==100&&c.officer(1004).aptitudeText().equals("BBSCSA"),"Zhuge Liang cross-source values");
        check(c.officer(2000).stat(0)==96&&c.officer(2000).aptitudeText().equals("SSAABC"),"Cao Cao cross-source values");
        check(c.officer(1000).stat(0)==75&&c.officer(1000).aptitudeText().equals("ABABCC"),"Liu Bei cross-source values");
        check(c.officers().stream().map(o->o.id).distinct().count()==670,"unique stable officer IDs");
        try{c.officers().clear();throw new AssertionError("mutable catalog");}catch(UnsupportedOperationException expected){checks++;}
        for(int side=0;side<3;side++){
            World old=ScenarioCatalog.load("regional-sandbox",side),w=ScenarioCatalog.load("officer-reference-drill",side);
            for(World.Officer o:w.officers){ContentCatalog.Officer input=c.officer(o.id);int[] actual={o.leadership,o.war,o.intelligence,o.politics,o.charm};for(int i=0;i<5;i++)check(actual[i]==input.stat(i),"actual stat "+o.id+":"+i);for(int i=0;i<6;i++)check(o.aptitude[i]==input.aptitude(i),"actual aptitude "+o.id+":"+i);check(o.owner==old.officer(o.id).owner&&o.cityId==old.officer(o.id).cityId,"explicit engineering ownership unchanged");}
            for(World.City city:w.cities){World.City original=old.city(city.id);check(city.hex.equals(original.hex)&&city.gold==original.gold&&city.food==original.food&&Arrays.equals(city.equipment,original.equipment)&&Arrays.equals(city.ships,original.ships),"real resource and position match fixture");}
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)check(w.terrain[q][r]==old.terrain[q][r],"engineering terrain not renamed national");
            World.City home=w.home();check(w.deploy(home.id,w.idle(home).get(0).id,World.Weapon.SPEAR,3000).ok,"new pack deploy side "+side);
            check(w.nextTurn().ok&&w.turn==1,"new pack actual AI turn");byte[] save=SaveCodec.encode(w);World restored=SaveCodec.decode(save);check(Arrays.equals(save,SaveCodec.encode(restored)),"new pack exact v7 snapshot");
            w.nextTurn();restored.nextTurn();check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"loaded gameplay deterministic");
        }
        // A retired/updated content package must never replace a progressed snapshot with a new opening.
        World retired=ScenarioCatalog.load("officer-reference-drill",2);retired.nextTurn();retired.scenarioId="removed-source-pack";retired.dataRevision=900;retired.city(300).gold=1234;retired.officer(3001).aptitude[0]=3;
        World restored=SaveCodec.decode(SaveCodec.encode(retired));check(restored.turn==1&&restored.city(300).gold==1234&&restored.officer(3001).aptitude[0]==3&&restored.dataRevision==900,"removed/revised pack does not reset snapshot");
        // Load SaveCodec in a fresh class loader with no data resources at all.
        java.net.URL classes=SaveCodec.class.getProtectionDomain().getCodeSource().getLocation();
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new java.net.URL[]{classes},ClassLoader.getPlatformClassLoader())){
            check(loader.getResource("scenarios/index.txt")==null&&loader.getResource("content/index.txt")==null,"isolated reader has no data packages");
            Class<?> codec=loader.loadClass("game.sanguo.core.SaveCodec"),worldType=loader.loadClass("game.sanguo.core.World");byte[] bytes=SaveCodec.encode(retired);
            Object snapshot=codec.getMethod("decode",byte[].class).invoke(null,(Object)bytes);
            check(Arrays.equals(bytes,(byte[])codec.getMethod("encode",worldType).invoke(null,snapshot)),"exact old snapshot reads with all packs removed");
        }
        String source;try(InputStream in=ContentTest.class.getResourceAsStream("/scenarios/officer-reference-drill.properties")){source=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        for(String bad:new String[]{source.replace("reference=rlu-officers","reference=missing"),source.replace("|97|71|96|86|93","|96|71|96|86|93"),source.replace("aptitude.0=1000|","aptitude.0=9999|")}){
            check(!bad.equals(source),"mutation actually changes source");try{ScenarioData.read(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8)),2);throw new AssertionError("invalid sourced pack accepted");}catch(IOException expected){checks++;}
        }
        // AI marches on the real fixture and exchanges attacks; no teleport or injected damage.
        World battle=ScenarioCatalog.load("officer-reference-drill",0);int before=battle.cities.stream().mapToInt(city->city.defense).sum();boolean fought=false;
        for(int i=0;i<60&&!battle.gameOver();i++){battle.nextTurn();SaveCodec.validate(battle);if(battle.cities.stream().mapToInt(city->city.defense).sum()!=before||battle.units.stream().anyMatch(u->u.troops<3000)){fought=true;break;}}
        check(fought,"new sourced start reaches real combat");
        System.out.printf(Locale.ROOT,"PASS: %d content assertions; cold catalog %.3f ms; sourced opening combat at turn %d.%n",checks,load/1e6,battle.turn);
    }
}
