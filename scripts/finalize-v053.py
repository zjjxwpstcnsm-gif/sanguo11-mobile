#!/usr/bin/env python3
from pathlib import Path

def change(path,old,new):
    p=Path(path);s=p.read_text()
    if s.count(old)!=1:raise RuntimeError(f'{path}: expected one anchor: {old[:100]} ({s.count(old)})')
    p.write_text(s.replace(old,new))

p='app/src/androidTest/java/game/sanguo/mobile/GameSmokeRunner.java'
change(p,'private boolean realm52,','private boolean reports53,realm52,')
change(p,'super.onCreate(arguments);realm52=','super.onCreate(arguments);reports53=arguments!=null&&"true".equals(arguments.getString("reports53"));realm52=')
change(p,'if(realm52){new Realm52Probe(this).run();','if(reports53){new Reports53Probe(this).run();result.putString("stream","REPORTS53 PASS: installed battle reports, single-list crew, tab state, rotation and actual sortie.\\n");finish(Activity.RESULT_OK,result);return;}\n            if(realm52){new Realm52Probe(this).run();')
p='app/src/main/java/game/sanguo/mobile/BattleReportUi.java'
change(p,'root.setLayoutParams(new FrameLayout.LayoutParams(-1,a.dp(height)));','ViewGroup.LayoutParams size=root.getLayoutParams();size.height=a.dp(height);root.setLayoutParams(size);')
p='core/src/main/java/game/sanguo/core/BattleReports.java'
change(p,'label+" · "+c.name,c.text,c.location);\n    }','label+" · "+c.name,c.text,c.location);clearAction();\n    }')
change(p,'World.Unit u=o.unitId<0?null:w.unit(o.unitId);World.City c=o.cityId<0?null:w.city(o.cityId);Hex h=u!=null?u.hex:c==null?null:c.hex;','World.City c=o.cityId<0?null:w.city(o.cityId);')
change(p,'String place=u!=null?"部队#"+u.id:c!=null?c.name:"在途 / 无驻地";','String place=o.unitId>=0?"部队#"+o.unitId:c!=null?c.name:"在途 / 无驻地";')
Path(__file__).unlink()
print('Native report/formation probes connected.')
