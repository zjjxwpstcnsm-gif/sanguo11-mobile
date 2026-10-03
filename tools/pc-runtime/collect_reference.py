#!/usr/bin/env python3
"""Archive a real original D3D9 frame after optional normal PC menu input.

Requires the owned private-copy observer to be running. No rendered substitute,
game-memory patch, scenario mutation or source-install write is performed.
"""
import argparse,datetime,hashlib,json,os,subprocess,sys,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'out/toolchain/pc-system-python'),str(ROOT/'out/toolchain/pc-emulate')]
from PIL import Image

def collect(label,command=None,destination=None,camera=False):
    if not label or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_' for c in label):
        raise ValueError('Use a simple unique evidence label')
    copy=ROOT/'out/pc-visual/pc-runtime-copy'
    source=copy/'pc-source-frame.bmp';request=copy/'pc-capture.request'
    dest=(destination or ROOT/'out/pc-visual/v152/pc-reference').resolve();dest.mkdir(parents=True,exist_ok=True)
    command_log=None
    env=dict(os.environ,WINEPREFIX=str(ROOT/'out/pc-visual/pc-runtime-prefix'),WINEDLLOVERRIDES='d3d9=b',WINEDEBUG='-all')
    wine=str(ROOT/'out/toolchain/wine/Wine Devel.app/Contents/Resources/wine/bin/wine')
    if command:
        (copy/'pc-ui-command.txt').write_text(command+'\n')
        subprocess.run([wine,'g:\\pc-ui-command.exe'],cwd=copy,env=env,check=True,timeout=20)
        command_log=(copy/'pc-ui-command.log').read_text()
        time.sleep(1)
    camera_before=None
    if camera:
        subprocess.run([wine,'g:\\pc-read-camera.exe'],cwd=copy,env=env,check=True,timeout=20)
        camera_before=(copy/'pc-camera-snapshot.bin').read_bytes()
    before=source.stat().st_mtime_ns if source.exists() else 0
    request.write_text(label+'\n');deadline=time.monotonic()+15
    while request.exists() or not source.exists() or source.stat().st_mtime_ns<=before:
        if time.monotonic()>deadline:raise TimeoutError('No new completed original Present readback within 15s')
        time.sleep(.1)
    data=source.read_bytes();sha=hashlib.sha256(data).hexdigest()
    bitmap=dest/(label+'-'+sha[:12]+'.bmp');png=bitmap.with_suffix('.png');bitmap.write_bytes(data)
    with Image.open(bitmap) as im:
        im.load();im.save(png);width,height=im.size
        extrema=im.convert('RGB').getextrema()
    observer=(copy/'pc-readback-observer.log').read_text()
    camera_evidence=None
    if camera:
        subprocess.run([wine,'g:\\pc-read-camera.exe'],cwd=copy,env=env,check=True,timeout=20)
        camera_after=(copy/'pc-camera-snapshot.bin').read_bytes()
        for suffix,payload in [('before',camera_before),('after',camera_after)]:
            (dest/(label+'-'+sha[:12]+'-camera-'+suffix+'.bin')).write_bytes(payload)
        camera_evidence=dict(before_sha256=hashlib.sha256(camera_before).hexdigest(),after_sha256=hashlib.sha256(camera_after).hexdigest(),
                             stable_bytes=camera_before==camera_after,bytes=len(camera_before),accessor='verified original414fd0',
                             before=str((dest/(label+'-'+sha[:12]+'-camera-before.bin')).relative_to(ROOT)),
                             after=str((dest/(label+'-'+sha[:12]+'-camera-after.bin')).relative_to(ROOT)))
    adapter_evidence=None
    if (copy/'pc-mouse-installed.bin').exists():
        adapter_evidence=dict(installed_record_hex=(copy/'pc-mouse-installed.bin').read_bytes().hex(),
                              dll_sha256=hashlib.sha256((copy/'pc-mouse-input.dll').read_bytes()).hexdigest(),
                              ack_hex=(copy/'pc-mouse-ack.bin').read_bytes().hex() if (copy/'pc-mouse-ack.bin').exists() else None,
                              boundary='original EXE OS mouse polling imports; no authority/camera/render resource changes',
                              limits='Ack does not prove menu action; original UI and image must confirm')
    evidence=dict(label=label,utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),command=command,command_log=command_log,input_adapter=adapter_evidence,
                  bitmap=str(bitmap.relative_to(ROOT)),png=str(png.relative_to(ROOT)),sha256=sha,bytes=len(data),width=width,height=height,
                  rgb_extrema=extrema,observer_log=observer,camera=camera_evidence,source='original D3D9 Present backbuffer in private supplied-PC clone',
                  visual_acceptance=False,reason='Pixel capture requires separate semantic and matching-camera inspection')
    (dest/(label+'-'+sha[:12]+'.json')).write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
    with (dest/'capture-journal.jsonl').open('a') as f:f.write(json.dumps(evidence,ensure_ascii=False)+'\n')
    print(json.dumps({key:evidence[key] for key in ('label','command','png','sha256','width','height','camera','visual_acceptance')},ensure_ascii=False));return evidence

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('label');p.add_argument('--command');p.add_argument('--destination',type=Path);p.add_argument('--camera',action='store_true')
    a=p.parse_args();collect(a.label,a.command,a.destination,a.camera)
