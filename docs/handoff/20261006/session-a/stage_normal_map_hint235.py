#!/usr/bin/env python3
"""Stage only A Canvas hint readability, preserve live canonical cohort."""
from pathlib import Path
import hashlib,json,difflib,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/normal-map-hint-stage235'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    path='app/src/main/java/game/sanguo/mobile/FilamentMapView.java';original=ROOT/path;before=original.read_text();beforeSha=sha(original)
    assert beforeSha=='6c01e6f0dabd0ad6ae73e6bd58401976b3fb8d3fd49f75dfbe4657cd8cf69c09'
    old='''            p.setColor(0xfff0e5c8);c.drawText((snapshot.ground.pcMap!=null?"原版美术恢复中 · 部分演出暂缺 | ":"")+((pending>0)?"3D 地形装载中… · 请稍候":(draggingUnit?(dragPlan==null?"移出范围 · 松手取消":"松手移动 · 消耗"+dragPlan.cost):editorGrid?"编辑网格临时显示 · 不修改游戏网格设置":"长按己方选中部队拖动 · 双指缩放/旋转")),12,24*getResources().getDisplayMetrics().density,p);'''
    new='''            String hint=pending>0?"地图载入中… · 请稍候":draggingUnit?(dragPlan==null?"移出范围 · 松手取消":"松手移动 · 消耗"+dragPlan.cost):editorGrid?"编辑网格临时显示 · 不修改游戏网格设置":"长按己方选中部队拖动 · 双指缩放/旋转";
            float hintY=24*getResources().getDisplayMetrics().density;
            float hintRight=Math.max(12,Math.min(getWidth()-8,12+p.measureText(hint)+pad));
            // The map can be bright terrain/cloud pixels. An opaque HUD strip
            // gives the instruction stable contrast without changing the map.
            p.setColor(0xff0e1b24);c.drawRoundRect(8,hintY-font-pad,hintRight,hintY+pad,pad,pad,p);
            p.setColor(UiTheme.TEXT);c.save();c.clipRect(12,hintY-font-pad,hintRight,hintY+pad);c.drawText(hint,12,hintY,p);c.restore();'''
    assert before.count(old)==1;after=before.replace(old,new);target=OUT/path;target.parent.mkdir(parents=True);target.write_text(after)
    patch=DOC/'NORMAL_MAP_HINT_STAGE235.patch';patch.write_text(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+path,tofile='b/'+path)))
    compileBase=ROOT/'out/session-a/native-opening-stage224/compile';jars=ROOT/'out/session-a/native-opening-stage224/dependencies'
    android=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar')
    java=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java')
    classes=OUT/'classes';classes.mkdir();cp=':'.join(map(str,[android,compileBase/'filament.jar',compileBase/'classes']+sorted(jars.glob('*.jar'))))
    command=[str(java),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(target)]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()
    assert sha(original)==beforeSha
    evidence=ROOT/'out/session-a/registered-normal182/normal/remaining-normal-callers/source-03/evidence/source-3-map.png'
    report={'path':path,'owner':'A','beforeSha256':beforeSha,'afterSha256':sha(target),'stagePath':str(target),'patch':str(patch),'patchSha256':sha(patch),
            'sourceScreenshot':str(evidence),'sourceScreenshotSha256':sha(evidence),'sourceAcceptance':'REGISTERED_SOURCE03_ACCEPTANCE232.json',
            'compileCommand':command,'compileLogSha256':sha(OUT/'compile.log'),'compiled':True,'canonicalSourceUnchanged':True,
            'changedScope':'Only final Canvas instruction text/background. Source-map controller/state/labels/terrain/original assets and lifecycle untouched; no extra per-frame bitmap or RectF allocation.',
            'noNewPerFrameObjectExceptExistingHintString':True,'textArgb':'ffeaf5f8','opaqueBackgroundArgb':'ff0e1b24',
            'installed':False,'normalAndroidPixelOrTouchAccepted':False,'wholeGoalComplete':False,
            'integration':'Frozen A follow-up after closure227: exact Filament before required. Does not modify immutable227 or B current combined61. New APK and actual normal pan/zoom/drag cancel-cost/loading/editor/SaveRNGToken checks required.'}
    # The actual shared theme color is recorded, rather than inferred from text.
    import re
    match=re.search(r'TEXT\s*=\s*(0x[0-9a-fA-F]+)',(ROOT/'app/src/main/java/game/sanguo/mobile/UiTheme.java').read_text());assert match
    report['textArgb']=match.group(1)[2:]
    (DOC/'NORMAL_MAP_HINT_STAGE235.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ('compileCommand',)},ensure_ascii=False))
if __name__=='__main__':main()
