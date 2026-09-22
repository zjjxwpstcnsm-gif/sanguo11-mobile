from pathlib import Path
root=Path('.')
def change(path,old,new):
    p=root/path;s=p.read_text();assert s.count(old)==1,(path,s.count(old));p.write_text(s.replace(old,new))
core='core/src/main/java/game/sanguo/core/'
mobile='app/src/main/java/game/sanguo/mobile/'
# Exact-context edits to already tested large production files. Removed after integration.
change(core+'BattleReports.java','    synchronized void finishCounter(ActionContext context,String result){','''    synchronized ActionContext beginSite(World.City source,Hex target,TurnJournal.Kind kind,String label){
        prepare();ActionContext context=new ActionContext();context.actor=actionOwner;context.related=actionRelated;
        context.location=actionLocation;context.kind=actionKind;context.name=actionName;context.changes=takeChanges();
        action(kind,-1,target,label);actionOwner=source.owner;
        actionRelated=bit(source.owner);World.Unit enemy=w.unitAt(target);
        if(enemy!=null)actionRelated|=bit(enemy.owner);
        return context;
    }
    synchronized void finishCounter(ActionContext context,String result){''')
change(core+'TurnJournal.java','    public void close(){checkpoint("阶段结算");w.turnJournal=null;}','''    void site(World.City source,Hex target,Kind kind,String label){
        checkpoint("阶段结算");this.kind=kind;this.actorId=-1;this.sourceHex=source.hex;
        this.sourceOwner=source.owner;this.target=target;this.label=label;
    }
    public void close(){checkpoint("阶段结算");w.turnJournal=null;}''')
change(core+'Domestic.java','''    public int goldIncome(int city,int turn){
        int base''','''    public int goldIncome(int city,int turn){return goldIncome(city,turn,SiegeRules.blockaded(w,w.city(city)));}
    int goldIncome(int city,int turn,boolean blockaded){
        int base''')
change(core+'Domestic.java','        return monthly&&w.skills.city(city,Skill.FUHAO)?amount*3/2:amount;','        return SiegeRules.adjusted(monthly&&w.skills.city(city,Skill.FUHAO)?amount*3/2:amount,blockaded?SiegeRules.INCOME_PERCENT:100);')
change(core+'Domestic.java','''    public int foodIncome(int city,int turn){
        if(turn''','''    public int foodIncome(int city,int turn){return foodIncome(city,turn,SiegeRules.blockaded(w,w.city(city)));}
    int foodIncome(int city,int turn,boolean blockaded){
        if(turn''')
change(core+'Domestic.java','        return season&&w.skills.city(city,Skill.MIDAO)?amount*3/2:amount;','        return SiegeRules.adjusted(season&&w.skills.city(city,Skill.MIDAO)?amount*3/2:amount,blockaded?SiegeRules.INCOME_PERCENT:100);')
change(core+'Domestic.java','public String incomeSchedule(int city){return "月初基准金 "','public String incomeSchedule(int city){return "未计围城的月初基准金 "')
change(core+'Strategy.java','''return c==null||o==null?0:Math.min(c.recruitReserve,StrategyRules.enlistment(w.domestic.recruitAmount(cityId),c.order,o.charm,c.recruitReserve)*(w.skills.has(o,Skill.MINGSHENG)?150:100)/100);''','''return c==null||o==null?0:Math.min(c.recruitReserve,SiegeRules.recruitment(w,c,
            StrategyRules.enlistment(w.domestic.recruitAmount(cityId),c.order,o.charm,1000000)*(w.skills.has(o,Skill.MINGSHENG)?150:100)/100));''')
change(core+'Conscription.java','''    public static int quarterlyRecovery(World w,World.City c){
        return c.kind==World.SiteKind.CITY?QUARTERLY_RECOVERY+FARM_RECOVERY*w.domestic.capacity(c.id,Domestic.Kind.FARM):0;
    }''','''    public static int quarterlyRecovery(World w,World.City c){return quarterlyRecovery(w,c,SiegeRules.blockaded(w,c));}
    static int quarterlyRecovery(World w,World.City c,boolean blockaded){
        int raw=c.kind==World.SiteKind.CITY?QUARTERLY_RECOVERY+FARM_RECOVERY*w.domestic.capacity(c.id,Domestic.Kind.FARM):0;
        return SiegeRules.adjusted(raw,blockaded?SiegeRules.RECRUIT_PERCENT:100);
    }''')
change(core+'Conscription.java','''    static void settle(World w){
        if(!quarterBegins(w,w.turn))return;
        for(World.City c:w.cities){int gain=recovery(w,c);if(gain==0)continue;''','''    static void settle(World w){settle(w,SiegeRules.snapshot(w));}
    static void settle(World w,java.util.Map<Integer,SiegeRules.State> states){
        if(!quarterBegins(w,w.turn))return;
        for(World.City c:w.cities){if(c.owner<0)continue;
            int gain=Math.max(0,Math.min(quarterlyRecovery(w,c,SiegeRules.blocked(states,c)),reserveCap(w,c)-c.recruitReserve));if(gain==0)continue;''')
change(core+'World.java','turn++;Conscription.settle(this);contests.tick();','turn++;contests.tick();')
change(core+'World.java','war.tick();reports.checkpoint("火场设施结算");cityDefense.tick();','''war.tick();reports.checkpoint("火场设施结算");
        Map<Integer,SiegeRules.State> siege=SiegeRules.snapshot(this);
        Conscription.settle(this,siege);SiegeRules.settleAttrition(this,siege);reports.checkpoint("围城与季度兵源结算");
        cityDefense.tick();''')
change(core+'World.java','domestic.goldIncome(c.id,turn));int foodIncome=Math.min(Math.max(0,campaign.foodCap(c)-c.food),domestic.foodIncome(c.id,turn));','domestic.goldIncome(c.id,turn,SiegeRules.blocked(siege,c)));int foodIncome=Math.min(Math.max(0,campaign.foodCap(c)-c.food),domestic.foodIncome(c.id,turn,SiegeRules.blocked(siege,c)));')
change(core+'World.java','c.defense+=cityDefense.recovery(c);','c.defense+=cityDefense.recovery(c,SiegeRules.blocked(siege,c));')
change(core+'World.java','reports.note(c.name+"本旬收支：金收入+"+goldIncome+"，粮收入+"+foodIncome+"，驻军实际粮耗"+reportFoodUse);','reports.note(c.name+"本旬收支：金收入+"+goldIncome+"，粮收入+"+foodIncome+"，驻军实际粮耗"+reportFoodUse+(SiegeRules.blocked(siege,c)?"（围城：本次钱粮收入已减25%）":""));')
change(mobile+'MapView.java','    private Set<Hex> facilityCoverage=Collections.emptySet();','''    private SiegeOverlay siegeOverlay;
    List<Hex> siegeCoverage(){return siegeOverlay==null?Collections.emptyList():siegeOverlay.cells;}
    Set<Hex> siegeEnemies(){return siegeOverlay==null?Collections.emptySet():siegeOverlay.enemies;}
    private Set<Hex> facilityCoverage=Collections.emptySet();''')
change(mobile+'MapView.java','''    private void updateSelection(boolean actorChanged){
        facilityCoverage''','''    private void updateSelection(boolean actorChanged){
        siegeOverlay=SiegeOverlay.selected(world,selected);
        facilityCoverage''')
change(mobile+'MapView.java','        if(pickTargets==null)for(Map.Entry<Hex,Integer> entry:reachable.entrySet()){','''        if(!editorMode&&!openingPreview&&moving<0&&pickTargets==null&&siegeOverlay!=null){
            for(Hex h:siegeOverlay.cells){
                if(!camera.visible(x(h),y(h),RADIUS*scale))continue;
                boolean enemy=siegeOverlay.enemies.contains(h);
                boolean inner=SiteFootprint.distance(siegeOverlay.site,h)==1;
                polygon(x(h),y(h),RADIUS-1);
                fill(canvas,enemy?0x66ea625d:inner?0x44edb75b:0x24edb75b);
                stroke(canvas,enemy?0xfff77870:inner?0xcce8b760:0x88e8b760,Math.max(1,1.2f*density/scale));
            }
        }
        if(pickTargets==null)for(Map.Entry<Hex,Integer> entry:reachable.entrySet()){''')
change(mobile+'MainActivity.java','        String[] groups={"概览","内政","武将","军事","调动","外交","研究"};','''        line(SiegeRules.summary(world,c)+"\\n琥珀格：两圈围城范围 · 红格：敌军",12,SiegeRules.blockaded(world,c)?0xffff9a82:gold);
        String[] groups={"概览","内政","武将","军事","调动","外交","研究"};''')
change(mobile+'MainActivity.java','''        if(ui.group.equals("概览")){
            line(Conscription.description(world,c),13,gold);''','''        if(ui.group.equals("概览")){
            action("围城与守备详情",v->message(c.name+" · 围城",world.cityDefense.describe(c)));
            line(world.domestic.incomeSchedule(c.id),13,muted);
            line(Conscription.description(world,c),13,gold);''')
change('core/src/test/java/game/sanguo/core/BalanceTest.java','w.unit(1).hex=new Hex(11,6);check(w.cityDefense.besieged(c),"ranged siege at three tiles blocks repair");','''w.unit(1).hex=new Hex(11,6);check(!w.cityDefense.besieged(c),"third exterior ring does not blockade a harbor");
        w.unit(1).hex=new Hex(12,6);check(w.cityDefense.besieged(c),"second exterior ring blocks repair");''')
p=root/'app/src/androidTest/AndroidManifest.xml'
s=p.read_text().replace('</manifest>','    <instrumentation android:name="game.sanguo.mobile.SiegeInstrumentation"\n        android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true" />\n</manifest>');p.write_text(s)
p=root/'core/build.gradle';p.write_text(p.read_text()+"\n// Unified siege simulation and UI contracts.\ntasks.register('verifySiege', JavaExec) {\n    dependsOn testClasses\n    classpath = sourceSets.test.runtimeClasspath\n    mainClass = 'game.sanguo.core.SiegeRulesTest'\n}\ntasks.named('check') { dependsOn 'verifySiege' }\n")
p=root/'scripts/test-ui-models.sh';p.write_text(p.read_text()+"\n# Shared selected-base overlay.\nbash scripts/test-siege.sh\n")
change('core/src/test/java/game/sanguo/core/NavigationDefenseTest.java', 'check(distance==3?u.troops==6000:u.troops<6000&&u.troops>=5640,"bounded counter including tactic/crossbow "+weapon);', 'check(u.troops<6000&&u.troops>=5640,"bounded footprint-aware counter including outer-ring catapult "+weapon);')
change('core/src/test/java/game/sanguo/core/NavigationDefenseTest.java', 'check(target.troops==6000,"gate range is one");', 'check(target.troops<6000,"gate second exterior ring now defended");')
change('core/src/test/java/game/sanguo/core/NavigationDefenseTest.java', 'int expected=Math.min(round.cityDefense.strength(round.city(10)),round.cityDefense.strength(round.city(10))*150/190);int troops=fixed.troops;', 'int garrison=round.city(10).troops;round.city(10).troops-=SiegeRules.attrition(round.city(10),SiegeRules.state(round,round.city(10)));\n        int expected=round.cityDefense.counterDamage(round.city(10),fixed);round.city(10).troops=garrison;int troops=fixed.troops;')
