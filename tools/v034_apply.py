from pathlib import Path
import re

# Version.
p=Path('app/build.gradle'); s=p.read_text()
s=s.replace('versionCode 33','versionCode 34',1).replace("versionName '0.33.0-architecture-rules-dev'","versionName '0.34.0-mobile-ux-geography-turn-dev'",1)
p.write_text(s)

# True AI faction progress + clone timing surface.
p=Path('app/src/main/java/game/sanguo/mobile/TurnWork.java'); s=p.read_text()
if 'volatile World working;' not in s:
    s=s.replace('    boolean done;\n', '    volatile boolean done;\n    volatile World working;\n    volatile long cloneMillis;\n',1)
    s=s.replace('            try {computed=SaveCodec.decode(SaveCodec.encode(before));computed.nextTurn();SaveCodec.validate(computed);report=UiModels.turnSummary(before,computed);}',
      '            try {long cloneStart=System.nanoTime();computed=SaveCodec.decode(SaveCodec.encode(before));cloneMillis=(System.nanoTime()-cloneStart)/1000000L;working=computed;computed.nextTurn();SaveCodec.validate(computed);report=UiModels.turnSummary(before,computed);}',1)
    s=s.replace('    }\n}', '    }\n    String status() {\n        if(done)return "";\n        World w=working;\n        if(w==null)return "正在准备旬结算…";\n        int owner=w.active;\n        if(owner==w.player)return "正在执行全局旬结算 · 局面复制 "+cloneMillis+"ms";\n        return "电脑行动中 · "+w.faction(owner)+" · "+w.date();\n    }\n}',1)
p.write_text(s)

# AI hot path: only sort/evaluate current faction's bases.
p=Path('core/src/main/java/game/sanguo/core/World.java'); s=p.read_text()
old='''        List<City> ordered=new ArrayList<>(cities);\n        ordered.sort(Comparator.comparingInt((City c)->-ai.incoming(c)).thenComparingInt(c->c.id));\n        for(City c:ordered)if(c.owner==active)ai.replenish(c.id);\n        for(City c:ordered)if(c.owner==active)ai.support(c.id);\n        for(City c:ordered)if(c.owner==active)ai.deploy(c.id,6000);'''
new='''        // Hot path: each AI faction only evaluates and sorts its own bases. With the\n        // full historical faction set, sorting the national city list per faction made\n        // end-of-turn time grow much faster than the actual amount of AI work.\n        List<City> ordered=new ArrayList<>();\n        for(City c:cities)if(c.owner==active)ordered.add(c);\n        ordered.sort(Comparator.comparingInt((City c)->-ai.incoming(c)).thenComparingInt(c->c.id));\n        for(City c:ordered)ai.replenish(c.id);\n        for(City c:ordered)ai.support(c.id);\n        for(City c:ordered)ai.deploy(c.id,6000);'''
if old in s:
    s=s.replace(old,new,1)
    s=s.replace('        for(City c:ordered)if(c.owner==active)ai.prepare(c.id);','        for(City c:ordered)ai.prepare(c.id);',1)
elif 'Hot path: each AI faction only evaluates' not in s:
    raise SystemExit('World.runAi anchor missing')
p.write_text(s)

# Bottom city/unit quick navigation strips and live AI banner.
p=Path('app/src/main/java/game/sanguo/mobile/MainActivity.java'); s=p.read_text()
if 'quickCityStrip' not in s:
    if 'private TextView turnBanner;' not in s: raise SystemExit('turnBanner field anchor missing')
    s=s.replace('private TextView turnBanner;','private TextView turnBanner,turnProgress;\n    private LinearLayout quickCityStrip,quickUnitStrip;',1)
    anchor='        LinearLayout bottom=new LinearLayout(this);bottom.setPadding(dp(6),0,dp(6),0);bottom.setGravity(Gravity.CENTER_VERTICAL);'
    insert='''        turnProgress=text("",12,gold);turnProgress.setPadding(dp(12),dp(3),dp(12),dp(3));turnProgress.setBackgroundColor(0xff1c3340);turnProgress.setVisibility(View.GONE);turnProgress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(turnProgress,new LinearLayout.LayoutParams(-1,-2));\n        quickCityStrip=new LinearLayout(this);quickCityStrip.setOrientation(LinearLayout.HORIZONTAL);root.addView(quickNavigatorRow("城池",quickCityStrip),new LinearLayout.LayoutParams(-1,dp(42)));\n        quickUnitStrip=new LinearLayout(this);quickUnitStrip.setOrientation(LinearLayout.HORIZONTAL);root.addView(quickNavigatorRow("部队",quickUnitStrip),new LinearLayout.LayoutParams(-1,dp(42)));\n'''+anchor
    if anchor not in s: raise SystemExit('MainActivity bottom anchor missing')
    s=s.replace(anchor,insert,1)
    methods='''    private View quickNavigatorRow(String label,LinearLayout strip){\n        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(6),0,dp(6),0);row.setBackgroundColor(0xff101d28);\n        TextView name=text(label,12,gold);name.setGravity(Gravity.CENTER);row.addView(name,new LinearLayout.LayoutParams(dp(42),-1));\n        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(true);scroll.setFillViewport(false);scroll.addView(strip,new HorizontalScrollView.LayoutParams(-2,-1));row.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));return row;\n    }\n    private void refreshQuickNavigator(){\n        if(quickCityStrip==null||quickUnitStrip==null||world==null)return;\n        quickCityStrip.removeAllViews();quickUnitStrip.removeAllViews();\n        List<World.City> ownCities=new ArrayList<>();for(World.City c:world.cities)if(c.owner==world.player)ownCities.add(c);ownCities.sort(Comparator.comparingInt(c->c.id));\n        for(World.City c:ownCities){Button b=button(c.name+" · "+(c.troops/1000)+"k",v->selectObject(c.hex,-2,true));b.setTextSize(11);b.setContentDescription("定位己方据点 "+c.name);quickCityStrip.addView(b,new LinearLayout.LayoutParams(dp(104),-1));}\n        List<World.Unit> ownUnits=new ArrayList<>();for(World.Unit u:world.fieldUnits())if(u.owner==world.player)ownUnits.add(u);ownUnits.sort(Comparator.comparingInt(u->u.id));\n        for(World.Unit u:ownUnits){World.Officer o=world.officer(u.officerId);String n=o==null?("部队"+u.id):o.name;Button b=button(n+" · "+u.troops,v->selectObject(u.hex,u.id,true));b.setTextSize(11);b.setContentDescription("定位己方部队 "+n);quickUnitStrip.addView(b,new LinearLayout.LayoutParams(dp(116),-1));}\n        View cityRow=(View)quickCityStrip.getParent().getParent();View unitRow=(View)quickUnitStrip.getParent().getParent();cityRow.setVisibility(ownCities.isEmpty()?View.GONE:View.VISIBLE);unitRow.setVisibility(ownUnits.isEmpty()?View.GONE:View.VISIBLE);\n    }\n    private void refreshTurnProgress(){\n        if(turnProgress==null)return;\n        if(aiRunning&&turnWork!=null&&!turnWork.done){turnProgress.setText(turnWork.status());turnProgress.setVisibility(View.VISIBLE);turnProgress.removeCallbacks(turnProgressTicker);turnProgress.postDelayed(turnProgressTicker,120);}\n        else {turnProgress.removeCallbacks(turnProgressTicker);turnProgress.setVisibility(View.GONE);}\n    }\n    private final Runnable turnProgressTicker=()->refreshTurnProgress();\n'''
    nav='    private void showNavigation(){'
    if nav not in s: raise SystemExit('showNavigation anchor missing')
    s=s.replace(nav,methods+nav,1)
    m=re.search(r'(\n\s*private void refresh\(\)\s*\{)',s)
    if not m: raise SystemExit('refresh() anchor missing')
    s=s[:m.end()]+'\n        refreshQuickNavigator();refreshTurnProgress();'+s[m.end():]
p.write_text(s)

# Strategic chokepoint authoring after generic road generation: Luoyang basin.
p=Path('tools/content/national_geography.py'); s=p.read_text()
if 'Luoyang basin must not be bypassable' not in s:
    geo_anchor='''    from strategic_sites import layout\n    extra_sites=layout(terrain,positions)'''
    geo_insert='''    # Luoyang basin must not be bypassable by generic nearest-city roads. The\n    # Yellow River seals the north; mountain walls seal the south and flanks,\n    # leaving the authored western/eastern gate corridors (Tong/Hangu and Hulao).\n    luoyang=(79,76)\n    for yy in range(68,86):\n        for xx in range(68,91):\n            d=hex_distance(luoyang,(xx,yy))\n            if 4<=d<=9 and terrain[yy][xx] not in 'WOV':terrain[yy][xx]='M'\n    def carve_corridor(points):\n        for start,end in zip(points,points[1:]):\n            p=start\n            while True:\n                x,y=p\n                for yy in range(max(0,y-1),min(200,y+2)):\n                    for xx in range(max(0,x-1),min(200,x+2)):\n                        if hex_distance(p,(xx,yy))<=1 and terrain[yy][xx] not in 'WOV':terrain[yy][xx]='P';roads.add((xx,yy))\n                if p==end:break\n                from strategic_sites import neighbors\n                p=min(neighbors(x,y),key=lambda h:(hex_distance(h,end),h))\n    carve_corridor([(79,76),(84,77),(90,77)])       # Hulao east exit\n    carve_corridor([(79,76),(71,78),(65,72)])       # Hangu/Tong west exit\n    for xx in range(73,86):\n        if terrain[84][xx] not in 'WOV':terrain[84][xx]='M'\n\n    from strategic_sites import layout\n    extra_sites=layout(terrain,positions)'''
    if geo_anchor not in s: raise SystemExit('geography anchor missing')
    s=s.replace(geo_anchor,geo_insert,1)
p.write_text(s)

Path('tools/content/test_geography_v034.py').write_text('''import json, pathlib, sys\nsys.path.insert(0,str(pathlib.Path(__file__).parent))\nfrom national_geography import generate_geography\nsites=json.loads(pathlib.Path("data/content/sites-source.json").read_text())\ncities=[x for x in sites if x["kind"]=="city"]\nt,_=generate_geography(cities)\nassert t[69][80]=="W", "Yellow River north of Luoyang must remain water"\nfor x,y in [(79,76),(90,77),(65,72),(71,78)]: assert t[y][x] not in "MWOV", (x,y,t[y][x])\nassert sum(t[84][x]=="M" for x in range(73,86))>=10, "Luoyang southern mountain rim reopened"\nprint("v0.34 geography chokepoints: ok")\n''')
print('v0.34 migration applied')
