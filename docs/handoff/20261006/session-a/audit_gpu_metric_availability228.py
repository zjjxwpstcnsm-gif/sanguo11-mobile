#!/usr/bin/env python3
"""Read-only metric availability: zero Android mtrack is not zero GPU memory."""
from pathlib import Path
import json,subprocess,time,hashlib,re
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a'
ADB=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb')
SERIAL='emulator-5554';OUT=ROOT/'out/session-a/gpu-metric-availability228'
def shell(*args):
    p=subprocess.run([str(ADB),'-s',SERIAL,'shell',*args],capture_output=True,text=True,timeout=30)
    return {'command':list(args),'exit':p.returncode,'stdout':p.stdout,'stderr':p.stderr}
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    lock=json.loads(Path('/tmp/sanguo11-emulator-5554-session-a.lock/owner.json').read_text())
    assert str(ROOT) in json.dumps(lock),lock
    before=shell('pidof','game.sanguo.mobile.dev');pids=before['stdout'].split();assert len(pids)==1,pids
    pid=pids[0];samples=[]
    for label,args in [('meminfo',('dumpsys','meminfo',pid)),('surfaceflinger',('dumpsys','SurfaceFlinger')),('egl_property',('getprop','ro.hardware.egl')),('memtrack_property',('getprop','ro.hardware.memtrack'))]:
        v=shell(*args);v['label']=label;v['observedUnix']=time.time();path=OUT/(label+'.txt');path.write_text(v['stdout']+v['stderr']);v['path']=str(path);v['sha256']=hashlib.sha256(path.read_bytes()).hexdigest();samples.append(v)
    after=shell('pidof','game.sanguo.mobile.dev');assert after['stdout'].split()==pids,'Changed PID: sample attribution invalid'
    mem=samples[0]['stdout'];flinger=samples[1]['stdout']
    relevant=[line for line in mem.splitlines() if any(x in line for x in ['Graphics:','GL mtrack','EGL mtrack','Other mtrack','TOTAL PSS','TOTAL RSS','Native Heap','Dalvik Heap'])]
    graphics=re.search(r'Graphics:\s+(\d+)',mem)
    driver=[line[:500] for line in flinger.splitlines() if any(x in line.lower() for x in ['gles:','gl_vendor','gl_renderer','graphic buffer','total allocated','renderengine'])]
    report={'serial':SERIAL,'pid':pid,'pidBeforeAfterSame':True,'lock':lock,'samples':[{k:v for k,v in x.items() if k not in ('stdout','stderr')} for x in samples],
            'meminfoRelevant':relevant,'reportedGraphicsKiB':int(graphics.group(1)) if graphics else None,'surfaceFlingerDriverOrBufferLines':driver,
            'appAttributedGpuPeakProven':False,'scope':'Single read-only metric availability sample during live source03 cohort. meminfo mtrack and SurfaceFlinger global compositor buffers cannot establish app Java/native/GPU peak; a reported zero is not absence of GPU allocation. No save/RNG/UI mutation, tracing or additional recording. No ARM claim.'}
    (DOC/'GPU_METRIC_AVAILABILITY228.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ('lock','samples')},ensure_ascii=False))
if __name__=='__main__':main()
