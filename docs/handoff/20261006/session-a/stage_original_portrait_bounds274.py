#!/usr/bin/env python3
"""Preserve full verified original portrait bounds in an independent stage."""
from pathlib import Path
import hashlib,json,difflib,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/original-portrait-bounds274'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);name='app/src/main/java/game/sanguo/mobile/OfficerPortrait.java';source=ROOT/name;before=source.read_text();after=before
 needle='''        area.set(getBounds());c.save();path.reset();path.addRoundRect(area,area.width()*.1f,area.width()*.1f,Path.Direction.CW);c.clipPath(path);''';assert after.count(needle)==1;after=after.replace(needle,'        area.set(getBounds());')
 needle='''        Bitmap pixel=original==null?null:original.get(sourceIdentity,sourceYear,0,this);''';assert after.count(needle)==1;after=after.replace(needle,needle+'''
        boolean sourceImage=customImage==null&&pixel!=null;
        c.save();
        if(sourceImage)c.clipRect(area);
        else{path.reset();path.addRoundRect(area,area.width()*.1f,area.width()*.1f,Path.Direction.CW);c.clipPath(path);}''')
 needle='''        c.restore();fill(0xffc9ae73);paint.setStyle(Paint.Style.STROKE);''';assert after.count(needle)==1;after=after.replace(needle,'''        c.restore();
        if(sourceImage)return; // Verified source pixels have no invented corner mask or overlaid frame.
        fill(0xffc9ae73);paint.setStyle(Paint.Style.STROKE);''')
 target=OUT/name;target.parent.mkdir(parents=True);target.write_text(after);patch=DOC/'ORIGINAL_PORTRAIT_BOUNDS274.patch';patch.write_text(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+name,tofile='b/'+name)))
 compile_base=ROOT/'out/session-a/map-fire-upload-build269/source/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';baseline=ROOT/'out/session-a/native-opening-stage224/compile';dependencies=ROOT/'out/session-a/native-opening-stage224/dependencies';android=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar');java=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java');assert compile_base.is_dir();cp=':'.join(map(str,[android,baseline/'filament.jar',compile_base]+sorted(dependencies.glob('*.jar'))));classes=OUT/'classes';classes.mkdir();command=[str(java),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(target)]
 with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'compile.log').read_text();readback=OUT/'readback';copy=readback/name;copy.parent.mkdir(parents=True);copy.write_text(before);subprocess.run(['git','apply','--no-index',str(patch)],cwd=readback,check=True);assert sha(copy)==sha(target) and source.read_text()==before
 report={'path':name,'beforeSha256':sha(source),'afterSha256':sha(target),'patchSha256':sha(patch),'patchPath':str(patch),'stagePath':str(target),'compiled':True,'compileCommand':command,'compileLogSha256':sha(OUT/'compile.log'),'patchReadbackExact':True,'canonicalUnchanged':True,'apk269Unchanged':True,'actualInstalled':False,'scope':'Only verified original PcPortraitLoader bitmap drawing preserves its complete rectangular bounds: remove mobile10-percent corner clip and overlaid invented gold frame. Current group0, identity/sex/source/year/dynamic form, decoder/cache/scaling/filter/custom/fallback behavior unchanged. Does not prove original ordinary small-family caller, PC UI frame/shape/scaling/color/gamma/all-age/fullscreen or actual Android pixels. Normal roster/detail output corner+full image acceptance still required in a later full APK; current269 fire queue untouched.','wholeGoalComplete':False};(DOC/'ORIGINAL_PORTRAIT_BOUNDS274.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='compileCommand'}))
if __name__=='__main__':main()
