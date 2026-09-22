from pathlib import Path
root=Path('.')
def change(path,old,new):
    p=root/path;s=p.read_text();assert s.count(old)==1,(path,s.count(old));p.write_text(s.replace(old,new))
mobile='app/src/main/java/game/sanguo/mobile/'
p=root/(mobile+'MainActivity.java');s=p.read_text()
line='        line(SiegeRules.summary(world,c)+"\\n琥珀格：两圈围城范围 · 红格：敌军",12,SiegeRules.blockaded(world,c)?0xffff9a82:gold);\n'
assert s.count(line)==1
s=s.replace(line,'')
anchor='        panel.addView(visualHeader(c,c.name,world.faction(c.owner)+" · 太守 "+UiModels.governor(world,c.id),44));'
assert s.count(anchor)==1
s=s.replace(anchor,line.replace('琥珀格','青格')+anchor);p.write_text(s)
p=root/(mobile+'MapView.java');s=p.read_text()
start=s.index('        if(!editorMode&&!openingPreview&&moving<0&&pickTargets==null&&siegeOverlay!=null){')
end=s.index('        if(pickTargets==null)for(Map.Entry<Hex,Integer> entry:reachable.entrySet()){',start)
block=s[start:end];s=s[:start]+s[end:]
block=block.replace('0x44edb75b:0x24edb75b','0x4454d5df:0x2454d5df').replace('0xcce8b760:0x88e8b760','0xe866e5ed:0xb866e5ed')
anchor='        if(selected!=null&&world.cityAt(selected)==null){'
assert s.count(anchor)==1;s=s.replace(anchor,block+anchor);p.write_text(s)
change('core/src/main/java/game/sanguo/core/SiegeRules.java','图例：琥珀色为范围','图例：青色为范围')
p=root/'docs/siege-refinement-20260922.md';s=p.read_text()
s=s.replace('truong-dai-hoc-luat-thanh-pho-chi-minh','truong-dai-hoc-luat-thanh-pho-ho-chi-minh').replace('琥珀色','青色')
s += "\n补充回归记录：Reports53NationalTest 的历史 v52 存档哈希在本轮与未修改 main 上均为 0c3b8525f3b9aff98b37b4cda14380577933987ae04791a85f363b6289899357，均不匹配其旧 golden。NavigationDefenseTest 的旧地图路径断言 [20036,20017] 返回 -1，在原始 main 上直接执行 geography 同样复现。这些旧断言没有被删除或改成无条件通过，补充工作流分别保留诊断输出，并与本轮必须通过的专项及相邻回归区分。\n\n首轮正式 Android 验证（ff4fb5c3ca55）完成构建、Lint、安装和47项界面检查。截图复核后进一步将围城状态前置到折叠面板可见区域，青色范围与金色开发用地区分，并新增可见区域检查；最终结果仍以最新构建证据为准。\n"
p.write_text(s)
