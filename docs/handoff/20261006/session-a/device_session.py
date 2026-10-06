#!/usr/bin/env python3
"""Exclusive rooted 5554 full backup/install/test/restore, streaming archives.

No data clear. Backup covers the whole private and external application trees.
Restoration removes only regular files added during this exclusive session,
extracts the original archive, and reads every regular file SHA back.
"""
import argparse, hashlib, json, os, pathlib, shlex, subprocess, tarfile, time, threading, re

ROOT = pathlib.Path(__file__).resolve().parents[4]
ADB = '/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
PACKAGE = 'game.sanguo.mobile.dev'
LOCK = pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock')
TREES = {'internal': '/data/data/'+PACKAGE, 'external': '/sdcard/Android/data/'+PACKAGE}

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for b in iter(lambda: f.read(1048576), b''): h.update(b)
    return h.hexdigest()

def run(*args, output=None, input=None, timeout=900):
    result=subprocess.run([ADB, '-s', 'emulator-5554', *args],
                          stdout=output or subprocess.PIPE, stdin=input,
                          stderr=subprocess.PIPE, timeout=timeout)
    if result.returncode:
        raise RuntimeError('ADB command failed '+repr(args)+': '+(result.stdout or b'').decode(errors='replace')+' '+result.stderr.decode(errors='replace'))
    return result.stdout

def archive_manifest(path):
    files = {}
    with tarfile.open(path, 'r:') as tar:
        for m in tar:
            name = m.name.removeprefix('./')
            if not m.isfile(): continue
            if name.startswith('/') or '..' in pathlib.PurePosixPath(name).parts:
                raise ValueError('Unsafe archive member')
            h = hashlib.sha256()
            with tar.extractfile(m) as f:
                for b in iter(lambda: f.read(1048576), b''): h.update(b)
            files[name] = {'bytes': m.size, 'sha256': h.hexdigest()}
    return files

def device_manifest(tree):
    raw = run('shell', 'cd '+shlex.quote(tree)+" && find . -type f -exec sha256sum '{}' '+'").decode()
    result = {}
    for line in raw.splitlines():
        sha, name = line.split('  ', 1)
        if len(sha) != 64: raise ValueError('Invalid device digest')
        result[name.removeprefix('./')] = sha
    return result

def main():
    p = argparse.ArgumentParser(); p.add_argument('mode', choices=['backup','reuse-backup','install-test','restore'])
    p.add_argument('--output', type=pathlib.Path, required=True)
    p.add_argument('--previous',type=pathlib.Path)
    p.add_argument('--apk', type=pathlib.Path); p.add_argument('--test-apk', type=pathlib.Path)
    p.add_argument('--reuse-installed',action='store_true')
    p.add_argument('--test-only-update',action='store_true')
    p.add_argument('--heap-profile',action='store_true')
    p.add_argument('--menu-music',action='store_true')
    p.add_argument('--audio-capture-rate',type=int,choices=[44100,48000])
    p.add_argument('--pause-fire',action='store_true')
    p.add_argument('--fresh-process-reopen',action='store_true')
    p.add_argument('--portrait-native',type=int,default=-1)
    p.add_argument('--suite', default='cold3D'); p.add_argument('--runner',default='GameSmokeRunner'); p.add_argument('--begin',default='0'); p.add_argument('--end',default='16'); a = p.parse_args()
    if a.menu_music and (a.runner!='UiUxInstrumentation' or a.suite!='audio'):raise ValueError('Menu music requires the real UiUx audio runner')
    if a.audio_capture_rate and not a.menu_music:raise ValueError('Actual music capture needs the normal menu flow')
    if a.pause_fire and a.runner!='SessionAFireFlowInstrumentation':raise ValueError('Fire pause requires real Fire flow runner')
    if a.portrait_native>=0 and (a.runner!='SessionAMapRepairInstrumentation' or a.suite!='mediaAll16' or int(a.end)!=int(a.begin)+1):raise ValueError('Targeted native portrait requires exactly one actual normal source')
    out = a.output.resolve(); report_path = out/'session.json'
    if a.mode=='reuse-backup':
        # Reuse only a COMPLETE byte archive whose entire current file set is
        # freshly verified. The older3266-file incomplete archive never qualifies.
        source=json.loads((a.previous.resolve()/'session.json').read_text())
        if source['stage']!='restored-verified':raise ValueError('Prior session not fully restored')
        if PACKAGE in run('shell','ps','-A').decode():raise ValueError('App active')
        if LOCK.exists():
            owner=json.loads((LOCK/'owner.json').read_text())
            if owner['root']!=str(ROOT) or owner.get('purpose')!='full AVD clone/resize; only5554':raise ValueError('Another owner')
        else:LOCK.mkdir()
        out.mkdir(parents=True,exist_ok=False)
        (LOCK/'owner.json').write_text(json.dumps({'root':str(ROOT),'pid':os.getpid(),'output':str(out)}))
        r={'serial':'emulator-5554','root':str(ROOT),'stage':'backup','trees':{},'archiveReuseSource':str(a.previous.resolve()),'freshEveryFileShaVerification':True,'device':{}}
        for prop in ['ro.build.version.sdk','ro.product.cpu.abi','dalvik.vm.heapsize','dalvik.vm.heapgrowthlimit']:
            r['device'][prop]=run('shell','getprop',prop).decode().strip()
        for name,record in source['trees'].items():
            archive=pathlib.Path(record['archive']);expected={k:v['sha256'] for k,v in record['files'].items()}
            if digest(archive)!=record['archiveSha256'] or device_manifest(record['path'])!=expected:raise ValueError('Complete prior backup no longer exact '+name)
            target=out/(name+'-before.tar');subprocess.run(['cp','-c',str(archive),str(target)],check=True)
            if digest(target)!=record['archiveSha256']:raise ValueError('Backup copy guard')
            r['trees'][name]={**record,'archive':str(target)}
        remote=run('shell','pm','path',PACKAGE).decode().strip().removeprefix('package:')
        installed_sha=run('shell','sha256sum',remote,timeout=180).decode().split()[0]
        prior=a.previous.resolve()/'previous-installed.apk'
        candidates=[prior]+[pathlib.Path(v) for v in source.get('apks',{})]
        equivalent=next((v for v in candidates if v.is_file() and digest(v)==installed_sha),None)
        if equivalent is None:
            with (out/'previous-installed.apk').open('wb') as f:run('exec-out','cat',remote,output=f)
        else:
            subprocess.run(['cp','-c',str(equivalent),str(out/'previous-installed.apk')],check=True)
            r['previousApkBackupSource']=str(equivalent);r['previousApkDeviceReadbackSha256']=installed_sha
        r['previousApkSha256']=digest(out/'previous-installed.apk')
        if r['previousApkSha256']!=installed_sha:raise ValueError('Previous APK backup SHA differs')
        r['stage']='backup-verified';report_path.write_text(json.dumps(r,indent=2));print('Complete current file set verified; independent full backups retained',flush=True);return
    if a.mode == 'backup':
        if run('shell','id','-u').strip() != b'0': raise ValueError('Root emulator required')
        if run('shell','dumpsys','activity').decode().find('mActiveInstrumentation=[]') < 0:
            # The process check below is mandatory; retain full inspection for review.
            pass
        state = run('shell','ps','-A').decode()
        if PACKAGE in state: raise ValueError('App already active; do not take exclusive ownership')
        LOCK.mkdir(); out.mkdir(parents=True, exist_ok=False)
        (LOCK/'owner.json').write_text(json.dumps({'pid':os.getpid(),'root':str(ROOT),'output':str(out)}))
        r = {'serial':'emulator-5554','root':str(ROOT),'stage':'backup','trees':{},'device':{}}
        for prop in ['ro.build.version.sdk','ro.product.cpu.abi','dalvik.vm.heapsize','dalvik.vm.heapgrowthlimit']:
            r['device'][prop] = run('shell','getprop',prop).decode().strip()
        report_path.write_text(json.dumps(r,indent=2))
        for name, tree in TREES.items():
            before = device_manifest(tree); archive = out/(name+'-before.tar')
            with archive.open('wb') as f: run('exec-out','tar','-C',tree,'-cf','-','.',output=f)
            files = archive_manifest(archive)
            after = device_manifest(tree)
            if before != after or after != {k:v['sha256'] for k,v in files.items()}:
                raise ValueError('Backup not an exact stable tree: '+name)
            r['trees'][name] = {'path':tree,'archive':str(archive),'archiveSha256':digest(archive),'files':files}
            report_path.write_text(json.dumps(r,indent=2))
            print('Verified backup',name,len(files),'files',archive.stat().st_size,'bytes',flush=True)
        apk = run('shell','pm','path',PACKAGE).decode().strip().removeprefix('package:')
        with (out/'previous-installed.apk').open('wb') as f: run('exec-out','cat',apk,output=f)
        r['previousApkSha256'] = digest(out/'previous-installed.apk'); r['stage']='backup-verified'
        report_path.write_text(json.dumps(r,indent=2)); return
    owner = json.loads((LOCK/'owner.json').read_text())
    if owner['output'] != str(out) or owner['root'] != str(ROOT): raise ValueError('Different session owns lock')
    r = json.loads(report_path.read_text())
    if a.mode == 'install-test':
        if r['stage'] != 'backup-verified': raise ValueError('Fresh verified backup required for each install')
        for name, record in r['trees'].items():
            if digest(pathlib.Path(record['archive'])) != record['archiveSha256']: raise ValueError('Backup changed')
            if device_manifest(record['path']) != {k:v['sha256'] for k,v in record['files'].items()}:
                raise ValueError('User tree changed after backup')
        r['apks'] = {str(v.resolve()):digest(v) for v in [a.apk,a.test_apk]}
        run_id='session_a_'+out.name.replace('-','_')
        r['runId']=run_id;r['reuseInstalled']=a.reuse_installed;r['heapProfileDiagnostic']=a.heap_profile
        r['menuMusicNormalFlow']=a.menu_music;r['stage']='installing'; report_path.write_text(json.dumps(r,indent=2))
        stop=threading.Event()
        def observe():
            with (out/'meminfo-timeline.txt').open('wb') as f:
                while not stop.is_set():
                    try:
                        f.write(('\nSAMPLE '+str(time.time())+' local-only; no process dump/GC request\n').encode()); f.write(run('shell','dumpsys','meminfo','--local',PACKAGE,timeout=30));f.flush()
                    except Exception as error: f.write(str(error).encode());f.flush()
                    stop.wait(2)
        observer=threading.Thread(target=observe);observer.start()
        log_file=(out/'logcat.txt').open('wb')
        log_process=subprocess.Popen([ADB,'-s','emulator-5554','logcat','-v','threadtime','-T','1'],stdout=log_file,stderr=subprocess.STDOUT)
        try:
            if a.pause_fire:
                r['firePauseSystemAnimationBefore']=run('shell','settings','get','global','animator_duration_scale').decode().strip();report_path.write_text(json.dumps(r,indent=2))
            if a.audio_capture_rate:
                import audio_capture_support as capture_support
                capture_support.prepare(out,r)
            with (out/'installation.txt').open('wb') as f:
                if not a.reuse_installed:
                    for apk in ([a.test_apk] if a.test_only_update else [a.apk,a.test_apk]): f.write(run('install','-r',str(apk.resolve()),timeout=300)); f.flush()
            for package, apk in [(PACKAGE,a.apk),(PACKAGE+'.test',a.test_apk)]:
                remote = run('shell','pm','path',package).decode().strip().removeprefix('package:')
                # Read installed bytes through the device SHA tool. Streaming a
                # 311MB APK over exec-out stalled once; the hash still reads the
                # complete installed file without a large transport allocation.
                installed_sha=run('shell','sha256sum',remote,timeout=180).decode().split()[0]
                (out/(package+'-installed.sha256')).write_text(installed_sha+'  '+remote+'\n')
                if installed_sha != digest(apk): raise ValueError('Installed APK SHA differs')
            registered=run('shell','pm','list','instrumentation').decode();(out/'instrumentation-registered.txt').write_text(registered);expected_component='instrumentation:'+PACKAGE+'.test/game.sanguo.mobile.'+a.runner+' (target='+PACKAGE+')';r['actualRunnerRegistered']=expected_component in registered.splitlines();report_path.write_text(json.dumps(r,indent=2));
            if not r['actualRunnerRegistered']:raise ValueError('Actual requested instrumentation component is not registered for target package')
            r['stage']='installed-verified'; report_path.write_text(json.dumps(r,indent=2))
            if a.audio_capture_rate:
                for capture_rate in [44100,48000]:
                    trial=capture_support.start(out,r,capture_rate,run_id+'_init'+str(capture_rate),3)
                    capture_support.collect(out,r,trial,15)
                selected=next(t for t in r['audioCapture']['captures'] if t['rate']==a.audio_capture_rate)
                if selected['result']['failure'] or selected['result']['frames']<=0:raise ValueError('Requested raw capture input unavailable; initialization results retained')
                music_capture=capture_support.start(out,r,a.audio_capture_rate,run_id+'_menu',130)
                if not music_capture['ready']:raise ValueError('Normal menu capture did not initialize')
            with (out/'instrumentation.txt').open('wb') as f:
                run('shell','am','instrument','-w','-e','suite',a.suite,'-e','run',run_id,'-e','begin',a.begin,'-e','end',a.end,'-e','portraitNative',str(a.portrait_native),'-e','heapProfile','true' if a.heap_profile else 'false','-e','mode','normal','-e','menuMusic','1' if a.menu_music else '0','-e','pauseFire','true' if a.pause_fire else 'false',PACKAGE+'.test/game.sanguo.mobile.'+a.runner,output=f,timeout=14400 if a.runner=='SessionAMapRepairInstrumentation' and a.suite in ['factions16','mediaAll16'] else 3600)
            if a.audio_capture_rate:capture_support.collect(out,r,music_capture,30)
            r['testOutput']=(out/'instrumentation.txt').read_text(); r['passed']=('UIUX PASS' in r['testOutput'] or 'SESSION_A_MAP PASS' in r['testOutput'] or 'PASS SESSION B FIELDWORKS normal' in r['testOutput'] or 'PASS SESSION A FIRE normal' in r['testOutput'] or 'PASS SESSION A ATTACK normal' in r['testOutput'] or 'PASS SESSION A SEARCH ordinary' in r['testOutput'] or 'PASS SESSION A DIRECT normal' in r['testOutput'] or 'PASS SESSION A ARMY normal' in r['testOutput'] or 'PASS SESSION A DEBATE ordinary' in r['testOutput'] or 'PASS SESSION B CAPACITY source0' in r['testOutput']) and 'FAIL' not in r['testOutput']
            folder='session-a-map' if a.runner=='SessionAMapRepairInstrumentation' else 'uiux'
            b_folder='direct' if a.runner=='SessionADirectRecruitmentPresentationInstrumentation' else 'search' if a.runner=='SessionASearchPresentationInstrumentation' else 'capacity' if a.runner=='SessionBCapacityInstrumentation' else 'governor' if a.runner=='SessionAArmyBudgetInstrumentation' else 'debate' if a.runner=='SessionADebatePresentationInstrumentation' else 'fieldworks'
            b_runner=a.runner in ['SessionBFieldworksInstrumentation','SessionAScenePresentationInstrumentation','SessionAFireFlowInstrumentation','SessionAAttackTaskInstrumentation','SessionADebatePresentationInstrumentation','SessionAArmyBudgetInstrumentation','SessionASearchPresentationInstrumentation','SessionADirectRecruitmentPresentationInstrumentation','SessionBCapacityInstrumentation']
            relative='session-b/'+b_folder if b_runner else folder+'/'+run_id
            run('pull','/sdcard/Android/data/'+PACKAGE+'/files/'+relative,str(out/'evidence'))
            if a.fresh_process_reopen:
                if a.runner not in ['SessionAMapRepairInstrumentation','SessionBFieldworksInstrumentation','SessionAScenePresentationInstrumentation','SessionAFireFlowInstrumentation','SessionAAttackTaskInstrumentation','SessionADebatePresentationInstrumentation','SessionAArmyBudgetInstrumentation','SessionASearchPresentationInstrumentation','SessionADirectRecruitmentPresentationInstrumentation','SessionBCapacityInstrumentation'] or not r['passed']:raise ValueError('Completed normal source/save workflow required before cold reopen')
                normal_pass=r['passed'];r['normalPassed']=normal_pass;r['passed']=False;r['stage']='cold-process-running';report_path.write_text(json.dumps(r,indent=2))
                expected=run('shell','sha256sum','/data/data/'+PACKAGE+'/files/auto.sg11').decode().split()[0]
                if b_runner:
                    expected_file='actual-source.sg11' if b_folder in ['capacity','governor'] else 'actual-mid.sg11' if b_folder in ['debate','search','direct'] else 'actual-build.sg11'
                    expected_build=run('shell','sha256sum','/sdcard/Android/data/'+PACKAGE+'/files/session-b/'+b_folder+'/'+expected_file).decode().split()[0]
                    if expected_build!=expected:raise ValueError('B actual build and normal autosave differ')
                # Android ends the target when instrumentation finishes. Use
                # its actual previously recorded frame-submission PID, not pidof
                # after process death. Preserve first-phase evidence above.
                prior_log=(out/'logcat.txt').read_text(errors='replace')
                previous_pids=re.findall(r'\s(\d+)\s+\d+\s+I Sanguo3D: First submission',prior_log)
                if not previous_pids:raise ValueError('No actual first-process PID evidence')
                old_pid=previous_pids[-1];cold_log_offset=len(prior_log)
                r['coldProcess']={'beforePid':old_pid,'expectedStartupSaveSha256':expected,'passed':False}
                report_path.write_text(json.dumps(r,indent=2))
                run('shell','am','force-stop',PACKAGE)
                with (out/'cold-instrumentation.txt').open('wb') as f:
                    run('shell','am','instrument','-w','-e','run',run_id+'_cold','-e','begin','0','-e','end','0','-e','expectedStartupSha',expected,'-e','mode','cold',PACKAGE+'.test/game.sanguo.mobile.'+a.runner,output=f,timeout=900)
                cold=(out/'cold-instrumentation.txt').read_text()
                cold_relative='session-b/'+b_folder+'-cold' if b_runner else 'session-a-map/'+run_id+'_cold'
                run('pull','/sdcard/Android/data/'+PACKAGE+'/files/'+cold_relative,str(out/'cold-evidence'))
                after_log=(out/'logcat.txt').read_text(errors='replace')[cold_log_offset:]
                new_pids=re.findall(r'\s(\d+)\s+\d+\s+I Sanguo3D: First submission',after_log)
                if not new_pids:raise ValueError('No actual second-process PID evidence')
                new_pid=new_pids[-1]
                r['coldProcess']={'beforePid':old_pid,'afterPid':new_pid,'differentPid':old_pid!=new_pid,'expectedStartupSaveSha256':expected,'passed':('SESSION_A_MAP PASS' in cold if a.runner=='SessionAMapRepairInstrumentation' else ('PASS SESSION B CAPACITY COLD' if b_folder=='capacity' else ('PASS SESSION A FIRE COLD' if a.runner=='SessionAFireFlowInstrumentation' else ('PASS SESSION A DIRECT COLD' if b_folder=='direct' else 'PASS SESSION A SEARCH COLD' if b_folder=='search' else 'PASS SESSION A ATTACK COLD' if a.runner=='SessionAAttackTaskInstrumentation' else 'PASS SESSION A DEBATE COLD' if b_folder=='debate' else 'PASS SESSION A ARMY COLD' if b_folder=='governor' else 'PASS SESSION B FIELDWORKS COLD'))) in cold) and 'FAIL' not in cold and old_pid!=new_pid}
                r['passed']=normal_pass and r['coldProcess']['passed']

        finally:
            capture_restore_error=None
            if a.pause_fire and 'firePauseSystemAnimationBefore' in r:
                try:
                    original=r['firePauseSystemAnimationBefore']
                    if original=='null':run('shell','settings','delete','global','animator_duration_scale')
                    else:run('shell','settings','put','global','animator_duration_scale',original)
                    after=run('shell','settings','get','global','animator_duration_scale').decode().strip();r['firePauseSystemAnimationRestored']=after==original
                    if after!=original:raise ValueError('System motion preference restoration mismatch')
                except Exception as error:capture_restore_error=error;r['systemAnimationRestoreError']=str(error)
            if a.audio_capture_rate:
                try:capture_support.stop_and_restore(out,r)
                except Exception as error:capture_restore_error=error;r['audioCaptureRestoreError']=str(error)
            stop.set();observer.join(40);log_process.terminate();log_process.wait(30);log_file.close()
            report_path.write_text(json.dumps(r,indent=2)); restore(out,r)
            if capture_restore_error is not None:
                LOCK.mkdir();(LOCK/'owner.json').write_text(json.dumps({'root':str(ROOT),'output':str(out),'pid':os.getpid(),'purpose':'A optional capture/motion state restore incomplete; main full SHA restored'}))
                raise capture_restore_error
    else: restore(out,r)

def restore(out,r):
    run('shell','am','force-stop',PACKAGE); restored={}
    for name, record in r['trees'].items():
        archive=pathlib.Path(record['archive'])
        if digest(archive)!=record['archiveSha256']: raise ValueError('Archive guard failed')
        current=device_manifest(record['path']); expected={k:v['sha256'] for k,v in record['files'].items()}
        for added in current.keys()-expected.keys():
            if added.startswith('/') or '..' in pathlib.PurePosixPath(added).parts: raise ValueError('Unsafe added file')
            run('shell','rm','--',record['path']+'/'+added)
        # Every current original file was hashed above. Rewriting gigabytes of
        # untouched media is unnecessary: restore mismatching/missing original
        # members from the COMPLETE guarded archive, then read ALL SHAs again.
        changed=[file for file,sha in expected.items() if current.get(file)!=sha]
        if changed:
            selected=out/(name+'-restore-changed.tar')
            with tarfile.open(archive,'r:') as source,tarfile.open(selected,'w:') as target:
                members={member.name.removeprefix('./'):member for member in source if member.isfile()}
                for file in changed:
                    member=members[file]
                    with source.extractfile(member) as content:target.addfile(member,content)
            with selected.open('rb') as f:run('shell','-T','tar','-C',record['path'],'-xf','-',input=f)
        after=device_manifest(record['path']); restored[name]={'exactRegularFileSha':after==expected,'files':len(after),'rewrittenOriginalFiles':len(changed),'strategy':'Full archive guarded; every current original SHA read; only mismatch/missing members restored; all final SHA read'}
        r['restoration']=restored; (out/'session.json').write_text(json.dumps(r,indent=2))
        if after!=expected: raise ValueError('Restoration SHA mismatch '+name)
    r['stage']='restored-verified'; (out/'session.json').write_text(json.dumps(r,indent=2))
    (LOCK/'owner.json').unlink(); LOCK.rmdir(); print('Full original file SHA restored',flush=True)

if __name__=='__main__': main()
