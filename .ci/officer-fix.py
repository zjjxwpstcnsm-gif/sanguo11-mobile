from pathlib import Path

def replace(path, old, new):
    p=Path(path);s=p.read_text();assert s.count(old)==1,(path,s.count(old));p.write_text(s.replace(old,new))

replace('app/src/main/java/game/sanguo/mobile/MainActivity.java', '''        line(Conscription.description(world,c),13,gold);
        if(c.kind!=World.SiteKind.CITY)line(world.districts.affiliation(c.id),13,muted);

''', '''            line(Conscription.description(world,c),13,gold);
            if(c.kind!=World.SiteKind.CITY){
                line(world.districts.affiliation(c.id),13,muted);
            }
''')
replace('app/src/main/java/game/sanguo/mobile/CustomOfficerPack.java', 'if(root.getInt("packageVersion")!=1)throw new IOException("数据包版本不支持");', 'Object packageVersion=root.get("packageVersion");if(!(packageVersion instanceof Number)||((Number)packageVersion).doubleValue()!=1)throw new IOException("数据包版本不支持");')
replace('core/src/test/java/game/sanguo/core/CustomOfficerTest.java', '        int order=w.city(77).order;', '''        int loyalty=o.loyalty,gold=w.city(77).gold;
        ok(w.strategy.rewardOfficer(77,10,o.id));check(o.loyalty>loyalty&&w.city(77).gold<gold,"custom officer really rewarded with ordinary resource cost");reset(w);
        check(!w.strategy.rewardOfficer(77,10,o.id).ok,"custom officer cannot repeat same-turn reward");
        Government.Rank rank=Government.ranks().get(0);ok(w.government.appointRank(77,10,o.id,rank.id));check(w.government.commandLimit(o.id)==rank.troops&&o.acted,"custom officer rank enforces real command ceiling and action");reset(w);
        ok(w.government.appointAdvisor(77,10,o.id));check(w.government.advisor(0).id==o.id&&o.acted,"custom officer appointed through real advisor command");reset(w);
        ok(w.strategy.appointGovernor(77,10,o.id));check(w.city(77).governorId==o.id&&w.strategy.governorPolitics(77)==o.politics,"custom governor contributes actual city income politics");reset(w);
        World appointments=SaveCodec.decode(SaveCodec.encode(w));check(appointments.government.office(o.id).id.equals(rank.id)&&appointments.government.advisor(0).id==o.id&&appointments.city(77).governorId==o.id,"all appointment references survive a real save roundtrip");
        int order=w.city(77).order;''')
replace('app/src/androidTest/java/game/sanguo/mobile/CustomOfficerProbe.java', '        for(String field:new String[]{"honor","talkMask"}){', '''        JSONObject wrongVersion=new JSONObject(p.root.toString());wrongVersion.put("packageVersion",1.5);
        try{CustomOfficerPack.preview(context(),new ByteArrayInputStream(wrongVersion.toString().getBytes("UTF-8")),library);throw new AssertionError("fractional package version accepted");}catch(IOException expected){check(unchanged.equals(library.snapshot().toString()),"fractional package version rejected atomically");}
        for(String field:new String[]{"honor","talkMask"}){''')
