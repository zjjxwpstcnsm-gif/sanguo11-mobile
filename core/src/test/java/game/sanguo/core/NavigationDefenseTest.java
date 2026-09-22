package game.sanguo.core;

import java.util.*;

/** Real save/command tests for northern land routes, historical openings and bounded city defense. */
public final class NavigationDefenseTest {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    static World copy(World w)throws Exception{return SaveCodec.decode(SaveCodec.encode(w));}
    public static World defenseFixture(){
        World w=new World(32,24);w.cities.add(new World.City(10,"守城",new Hex(10,10),0));w.cities.add(new World.City(20,"敌城",new Hex(28,19),1));
        for(int i=0;i<9;i++)w.officers.add(new World.Officer(i,"将"+i,i<2?0:1,i<2?10:20,90,85,75,70,70));
        w.city(10).troops=24000;w.city(10).food=50000;w.city(10).morale=100;return w;
    }
    static World.Unit enemy(World w,int id,Hex h,World.Weapon weapon){World.Officer o=w.officer(id+1);World.Unit u=new World.Unit(id,1,o.id,weapon,h,6000,12000);w.units.add(u);w.nextUnitId=Math.max(w.nextUnitId,id+1);o.unitId=id;o.cityId=-1;return u;}
    static void defense()throws Exception{
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.CROSSBOW,World.Weapon.RAM,World.Weapon.SIEGE_TOWER,World.Weapon.CATAPULT}){
            World w=defenseFixture();int distance=weapon==World.Weapon.CATAPULT?3:weapon==World.Weapon.SPEAR||weapon==World.Weapon.RAM?1:2;
            World.Unit u=enemy(w,1,new Hex(10+distance,10),weapon);w.active=1;int energy=u.energy;
            World.Result r=Army.siegeWeapon(weapon)?w.army.tactic(u.id,w.city(10).hex,w.army.tactics(u).get(0)):w.siege(u.id,10);
            check(r.ok,"real city attack "+weapon);check(w.city(10).troops<24000,"garrison damaged "+weapon);
            check(u.troops<6000&&u.troops>=5640,"bounded footprint-aware counter including outer-ring catapult "+weapon);
            check(u.acted&&u.energy==(Army.siegeWeapon(weapon)?energy-w.army.tactics(u).get(0).energy:energy),"counter charges no extra action or energy");
            SaveCodec.validate(w);
        }
        World w=defenseFixture();for(int i=1;i<=6;i++)enemy(w,i,w.city(10).hex.neighbors().get(i-1),World.Weapon.SPEAR);
        int before=w.units.stream().mapToInt(u->u.troops).sum(),power=w.cityDefense.strength(w.city(10));byte[] snapshot=SaveCodec.encode(w);
        for(int i=0;i<5;i++)w.cityDefense.preview(w.city(10),w.unit(1));check(Arrays.equals(snapshot,SaveCodec.encode(w)),"defense preview pure");
        World replay=copy(w);w.cityDefense.tick();replay.cityDefense.tick();int loss=before-w.units.stream().mapToInt(u->u.troops).sum();
        check(loss>0&&loss<=power*2,"six-way encirclement shares one capped volley");check(w.units.stream().allMatch(u->u.troops<6000),"every in-range hostile hit");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"volley deterministic after save");
        World safe=defenseFixture();World.Unit target=enemy(safe,1,new Hex(11,10),World.Weapon.SPEAR);
        safe.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,12);safe.cityDefense.tick();check(target.troops==6000,"allies immune");safe.campaign.treaties.clear();
        safe.city(10).food=0;safe.cityDefense.tick();check(target.troops==6000,"starved city cannot shoot");safe.city(10).food=10000;safe.city(10).troops=0;safe.cityDefense.tick();check(target.troops==6000,"empty garrison cannot shoot");
        safe.city(10).troops=24000;safe.city(10).kind=World.SiteKind.GATE;target.hex=new Hex(12,10);safe.cityDefense.tick();check(target.troops<6000,"gate second exterior ring now defended");
        target.hex=new Hex(11,10);target.troops=1;safe.cityDefense.tick();check(safe.unit(1)==null&&safe.officer(2).unitId==-1,"lethal volley releases tile and crew once");SaveCodec.validate(safe);
        World weak=defenseFixture();int strong=weak.cityDefense.strength(weak.city(10));weak.city(10).troops=1000;weak.city(10).defense=100;weak.city(10).morale=20;check(weak.cityDefense.strength(weak.city(10))<strong/3,"damaged small garrison weakens defense");
        World capture=defenseFixture();World.Unit conqueror=enemy(capture,1,new Hex(11,10),World.Weapon.SPEAR);capture.city(10).defense=1;capture.active=1;check(capture.siege(1,10).ok&&capture.city(10).owner==1&&conqueror.troops==6000,"fallen city cannot counter its conqueror");
        // A completed real global turn must shoot exactly once even though every faction resets.
        World round=defenseFixture();World.Unit fixed=enemy(round,1,new Hex(11,10),World.Weapon.SPEAR);fixed.status=War.Status.CONFUSED;fixed.statusTurns=3;
        for(World.City c:round.cities)c.gold=0;round.city(20).troops=0;round.city(20).food=0;
        int garrison=round.city(10).troops;round.city(10).troops-=SiegeRules.attrition(round.city(10),SiegeRules.state(round,round.city(10)));
        int expected=round.cityDefense.counterDamage(round.city(10),fixed);round.city(10).troops=garrison;int troops=fixed.troops;
        check(round.nextTurn().ok,"normal end turn runs defense");check(round.unit(1).troops==troops-expected,"one automatic volley per global turn");
        check(copy(round).unit(1).troops==round.unit(1).troops,"loading does not fire again");
    }
    static int landDistance(World w,Hex start,Hex target){
        Map<Hex,Integer> seen=new HashMap<>();ArrayDeque<Hex> queue=new ArrayDeque<>();queue.add(start);seen.put(start,0);
        while(!queue.isEmpty()){Hex p=queue.remove();if(p.equals(target))return seen.get(p);for(Hex h:p.neighbors())if(w.cost(h,World.Weapon.SPEAR)>0&&!w.army.water(h)&&!seen.containsKey(h)&&(w.cityAt(h)==null||h.equals(target))){seen.put(h,seen.get(p)+1);queue.add(h);}}
        return -1;
    }
    static void geography()throws Exception{
        for(String id:new String[]{"heroes-mobile-sandbox","huangjin-184","coalition-190","warlords-194","guandu-200","chibi-207","heroes-250"}){
            World w=TestScenarios.load(id,0);check(w.cities.size()==87,"87 live sites "+id);
            check(w.cities.stream().filter(c->c.kind==World.SiteKind.PORT).count()==35&&w.cities.stream().filter(c->c.kind==World.SiteKind.GATE).count()==10,"35 ports, 10 gates "+id);
            for(World.City c:w.cities)if(c.kind==World.SiteKind.PORT){check(c.hex.neighbors().stream().anyMatch(w.army::water),"port touches navigable water "+c.name);check(c.hex.neighbors().stream().filter(h->w.cost(h,World.Weapon.SPEAR)>0&&w.cityAt(h)==null).count()>=2,"port has land approach and spare exit "+c.name);}
            for(int[] pair:new int[][]{{20036,20017},{20037,20036},{20036,20020},{20039,20037}}){int distance=landDistance(w,w.city(pair[0]).hex,w.city(pair[1]).hex);check(distance>0&&distance<75,"direct passable northern land route "+Arrays.toString(pair)+" got "+distance);}
            for(World.City c:w.cities)if(c.kind==World.SiteKind.CITY)check(w.domestic.buildSites(c.id).size()==w.development.capacity(c.id),"all assigned development plots buildable "+c.name);
            check(w.development.capacity(20017)==22&&w.development.capacity(20006)==20,"retained city capacities");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy(w))),"full site map roundtrip "+id);
        }
        World w=TestScenarios.load("chibi-207",0);Hex lu=w.city(20025).hex;boolean south=false;for(int r=3;r<=12;r++)if(w.army.water(new Hex(lu.q-r/2,lu.r+r)))south=true;check(south,"Yangtze south of Lujiang");
        // Run actual deployment and route preview through the rebuilt passes.
        World n=TestScenarios.load("heroes-mobile-sandbox",1);World.City source=n.city(20036);World.Officer leader=n.idle(source).get(0);
        check(n.army.deploy(source.id,leader.id,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,20000,0).ok,"deploy in Hanzhong");
        MarchOrders.Plan p=n.marches.preview(leader.unitId,n.city(20017).hex);check(p.valid()&&p.path.size()<80,"actual northbound march preview");check(n.marches.execute(p).ok,"actual march starts");SaveCodec.validate(n);
    }
    static World.Officer named(World w,String name){return w.officers.stream().filter(o->o.name.equals(name)||catalogName(o.id).equals(name)).findFirst().orElseThrow();}
    static String catalogName(int id){try{return ContentCatalog.get().officer(id).name;}catch(Exception e){throw new RuntimeException(e);}}
    static String owner(World w,String name){World.Officer o=named(w,name);return w.faction(o.owner);}
    static void scenarios()throws Exception{
        for(ScenarioCatalog.Summary item:ScenarioCatalog.summaries()){World w=TestScenarios.load(item.id,0);check(item.sites==w.cities.size()&&item.officers==w.officers.size()&&item.factions==w.factions.length&&item.name.equals(w.scenarioName),"lightweight catalog matches world "+item.id);}
        String[] ids={"huangjin-184","coalition-190","warlords-194","guandu-200","chibi-207","heroes-250"};int[] years={184,190,194,200,207,250};
        for(int k=0;k<ids.length;k++){
            World w=TestScenarios.load(ids[k],0);check(w.startYear==years[k],"requested date");
            for(int side=0;side<w.factions.length;side++){
                int selected=side;World.Officer ruler=w.officers.stream().filter(o->o.owner==selected&&o.role==Strategy.Role.RULER).findFirst().orElseThrow();
                ContentCatalog.Officer source=ContentCatalog.get().officer(ruler.id);check(w.faction(side).equals(source.name+"军"),"explicit ruler matches faction "+w.faction(side));
                check(w.life.present(ruler.id)&&w.city(ruler.cityId).owner==side,"live ruler in owned city");
            }
            check(w.officers.stream().noneMatch(o->o.owner>=0&&!w.life.present(o.id)),"no future officers serving");
            if(years[k]<250){World.Officer future=named(w,"姜維");check(!w.life.present(future.id)&&future.owner==-1,"future general not active early");}
        }
        World first=TestScenarios.load("huangjin-184",0);check(named(first,"曹操").owner==named(first,"何進").owner,"184 Cao serves court");
        World c=TestScenarios.load("coalition-190",0);check(c.city(20012).owner==named(c,"曹操").owner&&c.city(20015).owner==named(c,"董卓").owner,"190 Chenliu/Luoyang");check(named(c,"郭嘉").owner==-1,"190 Guo Jia not prematurely assigned to Cao");
        World g=TestScenarios.load("guandu-200",0);check(named(g,"張郃").owner==named(g,"袁紹").owner&&named(g,"張遼").owner==named(g,"曹操").owner,"200 transferred officers");
        World ch=TestScenarios.load("chibi-207",0);check(ch.city(20028).owner==named(ch,"劉備").owner&&named(ch,"趙雲").owner==named(ch,"劉備").owner,"207 Xinye and Zhao Yun");check(named(ch,"諸葛亮").owner==-1,"207 Zhuge Liang available to recruit");
        World f=TestScenarios.load("heroes-250",0);check(f.officers.size()==670&&f.life.people().isEmpty()&&f.factions.length==28,"all-era fantasy without life gates");
    }
    static void campaigns()throws Exception{
        for(String id:new String[]{"huangjin-184","coalition-190","warlords-194","guandu-200","chibi-207","heroes-250"}){
            World w=TestScenarios.load(id,0);for(int turn=0;turn<3;turn++){World replay=copy(w);check(w.nextTurn().ok&&replay.nextTurn().ok,"new historical campaign advances "+id);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"historical turn replays exactly "+id);SaveCodec.validate(w);}
        }
    }
    public static void main(String[] args)throws Exception{defense();geography();scenarios();campaigns();System.out.println("PASS: "+checks+" navigation/defense/historical opening assertions.");}
}
