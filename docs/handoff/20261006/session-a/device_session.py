#!/usr/bin/env python3
"""Exclusive rooted 5554 full backup/install/test/restore, streaming archives.

No data clear. Backup covers the whole private and external application trees.
Restoration removes only regular files added during this exclusive session,
extracts the original archive, and reads every regular file SHA back.
"""
import argparse, hashlib, json, os, pathlib, shlex, subprocess, tarfile, time, threading, re, sys, uuid

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

def target_process_evidence(log):
    """Actual Android launch evidence, independent of renderer readiness."""
    launches = re.findall(r'ActivityManager: Start proc (\d+):' + re.escape(PACKAGE) + r'/[^\s]+ for added application ' + re.escape(PACKAGE) + r'(?:\n|$)', log)
    if not launches:
        raise ValueError('No exact target instrumentation process launch evidence')
    pid = launches[-1]
    submissions = re.findall(r'\s(\d+)\s+\d+\s+I Sanguo3D: First submission', log)
    return {'pid': pid, 'source': 'ActivityManager exact package instrumentation launch',
            'rendererFirstSubmissionObserved': pid in submissions}

def allocate_run_id(output_name,original_external):
    # Long case labels must leave space for _init48000 and _cold suffixes.
    # Keep the complete UUID; shorten only the display label, never identity.
    label=re.sub(r'[^A-Za-z0-9_]', '_', output_name)[:10]
    while True:
        identity='session_a_'+label+'_'+uuid.uuid4().hex
        roots=('files/session-a-map/','files/uiux/','files/session-a-game-mix/')
        if not any(k.startswith(root+identity) for k in original_external for root in roots):return identity

def main():
    p = argparse.ArgumentParser(); p.add_argument('mode', choices=['backup','reuse-backup','install-test','restore'])
    p.add_argument('--output', type=pathlib.Path, required=True)
    p.add_argument('--previous',type=pathlib.Path)
    p.add_argument('--apk', type=pathlib.Path); p.add_argument('--test-apk', type=pathlib.Path)
    p.add_argument('--reuse-installed',action='store_true')
    p.add_argument('--test-only-update',action='store_true')
    p.add_argument('--heap-profile',action='store_true')
    p.add_argument('--observe-workers',action='store_true')
    p.add_argument('--menu-music',action='store_true')
    p.add_argument('--clean-menu-music',action='store_true')
    p.add_argument('--audio-capture-rate',type=int,choices=[44100,48000])
    p.add_argument('--mixer-observation',choices=['periodic','off'],default='periodic')
    p.add_argument('--menu-capture-seconds',type=int,choices=[130,180],default=130)
    p.add_argument('--pause-fire',action='store_true')
    p.add_argument('--fresh-process-reopen',action='store_true')
    p.add_argument('--portrait-native',type=int,default=-1)
    p.add_argument('--search-diagnostics',action='store_true')
    p.add_argument('--suite', default='cold3D'); p.add_argument('--runner',default='GameSmokeRunner'); p.add_argument('--begin',default='0'); p.add_argument('--end',default='16'); a = p.parse_args()
    if a.clean_menu_music and not a.menu_music:raise ValueError('Clean menu requires actual established menu music fixture')
    if a.menu_music and (a.runner!='UiUxInstrumentation' or a.suite!='audio'):raise ValueError('Menu music requires the real UiUx audio runner')
    if a.audio_capture_rate and not a.menu_music:raise ValueError('Actual music capture needs the normal menu flow')
    if a.pause_fire and a.runner!='SessionAFireFlowInstrumentation':raise ValueError('Fire pause requires real Fire flow runner')
    if a.portrait_native>=0 and (a.runner!='SessionAMapRepairInstrumentation' or a.suite!='mediaAll16' or int(a.end)!=int(a.begin)+1):raise ValueError('Targeted native portrait requires exactly one actual normal source')
    if a.search_diagnostics and (a.runner!='SessionAMapRepairInstrumentation' or a.suite!='mediaAll16' or int(a.end)!=int(a.begin)+1):raise ValueError('Search observations require one normal media source')
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
        # Historical evidence is part of the user's full archive. Never reuse
        # a basename-only device directory across independent host batches.
        original_external=r['trees']['external']['files']
        run_id=allocate_run_id(out.name,original_external)
        r['runIdAllocation']='Fresh complete UUID checked against original external roots; ASCII display label max10, base length<=53, all capture/cold suffixes<=64'
        r['runId']=run_id;r['reuseInstalled']=a.reuse_installed;r['heapProfileDiagnostic']=a.heap_profile
        r['menuMusicNormalFlow']=a.menu_music;r['normalMenuMusicOnlyCondition']=a.clean_menu_music;r['menuCaptureSeconds']=a.menu_capture_seconds;r['mixerObservationEnabled']=a.mixer_observation=='periodic';r['stage']='installing'; report_path.write_text(json.dumps(r,indent=2))
        stop=threading.Event()
        def observe():
            with (out/'meminfo-timeline.txt').open('wb') as f:
                while not stop.is_set():
                    try:
                        f.write(('\nSAMPLE '+str(time.time())+' local-only; no process dump/GC request\n').encode()); f.write(run('shell','dumpsys','meminfo','--local',PACKAGE,timeout=30));f.flush()
                    except Exception as error: f.write(str(error).encode());f.flush()
                    stop.wait(2)
        observer=threading.Thread(target=observe);observer.start()
        worker_observer=None;worker_log=None;video_observer=None;video_log=None
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
            # User-authorized evidence for the exact frozen default cohort only.
            # Normal384 and its GC diagnostic never receive encoder pressure.
            default_receipt=pathlib.Path(__file__).with_name('FIRE_OVERRIDE_DEFAULT_BUILD90.json')
            default_build=json.loads(default_receipt.read_text())
            default_pairs=[{entry['path']:entry['sha256'] for entry in default_build['apks']}]
            for release_name in ('PICKER_RELEASE_BUILD116.json','NORMAL_VIEW_OPTION_TEST_BUILD124.json','CURRENT_NORMAL_TARGET_BUILD139.json','NORMAL_PREPARATION_BUILD145.json','OVERLAY_ADMISSION_BUILD155.json','EMPTY_PRESENTATION_BUILD165.json','B_LEGACY39_COMBINED_TEST_BUILD168.json','LEGACY39_REGISTERED_TEST_BUILD176.json','SEARCH_OBSERVATION_TEST_BUILD239.json','MAP_FIRE_UPLOAD_BUILD269.json','FIRE_CACHE_APK296.json','PREPARE_TEST_BUILD310.json','MESH_ALLOCATION_BUILD316.json','GPU_INDEX_BUILD326.json','REPEAT_TEST_BUILD330.json','MUSIC_RESERVE_BUILD369.json','SERIAL_BUILD411.json','SERIAL_TEST_BUILD416.json','PREPARE_BUILD424.json','REQUIRED_BUILD437.json','MAIN_BUILD446.json'):
                release_receipt=pathlib.Path(__file__).with_name(release_name)
                if release_receipt.is_file():
                    release_build=json.loads(release_receipt.read_text())
                    if release_build['buildSuccessful'] and release_build['gameLargeHeap'] is True:
                        default_pairs.append({entry['path']:entry['sha256'] for entry in release_build['apks']})
            video_requested=(r['apks'] in default_pairs
                and a.runner in ['SessionAMapRepairInstrumentation','SessionAScenePresentationInstrumentation',
                                 'SessionAFireFlowInstrumentation','SessionANativeDuelOpeningInstrumentation','SessionAAttackTaskInstrumentation','SessionBLegacy39Instrumentation','SessionAScenarioPrepareInstrumentation'])
            r['exactDefaultVideoRequested']=video_requested
            report_path.write_text(json.dumps(r,indent=2))
            if video_requested:
                video_log=(out/'video-observer-driver.log').open('wb')
                video_observer=subprocess.Popen([sys.executable,str(pathlib.Path(__file__).with_name('observe_flow_video.py')),
                    '--session',str(report_path),'--output',str(out/'video'),'--max-parts','80',
                    '--remove-verified-device-parts'],stdout=video_log,stderr=subprocess.STDOUT)
                r['videoObservation']={'hostPid':video_observer.pid,'path':str(out/'video/video.json'),
                    'scope':'Exact default APK raw recording; own temporary UUID segments removed only after matching pre/post-pull SHA; host originals retained. Encoder perturbs performance; startup/segment gaps/audio/PC/ARM unknown.'}
                report_path.write_text(json.dumps(r,indent=2))
            if a.observe_workers:
                worker_log=(out/'native-workers-driver.log').open('wb')
                worker_observer=subprocess.Popen([sys.executable,str(pathlib.Path(__file__).with_name('observe_worker_memory.py')),
                    '--session',str(report_path),'--output',str(out/'native-workers.jsonl'),'--max-seconds','14400'],
                    stdout=worker_log,stderr=subprocess.STDOUT)
                r['workerObservation']={'hostPid':worker_observer.pid,'path':str(out/'native-workers.jsonl'),
                    'scope':'Independent source-child smaps_rollup after exact installed APK verification; no GC, not native allocator bytes/GPU or memory-budget acceptance'}
                report_path.write_text(json.dumps(r,indent=2))
            if a.audio_capture_rate:
                for capture_rate in [44100,48000]:
                    trial=capture_support.start(out,r,capture_rate,run_id+'_init'+str(capture_rate),3)
                    capture_support.collect(out,r,trial,15)
                selected=next(t for t in r['audioCapture']['captures'] if t['rate']==a.audio_capture_rate)
                if selected['result']['failure'] or selected['result']['frames']<=0:raise ValueError('Requested raw capture input unavailable; initialization results retained')
                music_capture=capture_support.start(out,r,a.audio_capture_rate,run_id+'_menu',a.menu_capture_seconds)
                if not music_capture['ready']:raise ValueError('Normal menu capture did not initialize')
            with (out/'instrumentation.txt').open('wb') as f:
                run('shell','am','instrument','-w','-e','suite',a.suite,'-e','run',run_id,'-e','begin',a.begin,'-e','end',a.end,'-e','portraitNative',str(a.portrait_native),'-e','searchDiagnostics','true' if a.search_diagnostics else 'false','-e','heapProfile','true' if a.heap_profile else 'false','-e','mode','normal','-e','menuMusic','1' if a.menu_music else '0','-e','menuCleanWhole','true' if a.clean_menu_music else 'false','-e','pauseFire','true' if a.pause_fire else 'false',PACKAGE+'.test/game.sanguo.mobile.'+a.runner,output=f,timeout=14400 if a.runner=='SessionAMapRepairInstrumentation' and (a.suite in ['factions16','mediaAll16','fastPreview16'] or a.suite in ['all','media16'] and int(a.end)-int(a.begin)>1) else 3600)
            r['testOutput']=(out/'instrumentation.txt').read_text(); r['passed']=('UIUX PASS' in r['testOutput'] or 'SESSION_A_MAP PASS' in r['testOutput'] or 'PASS SESSION B FIELDWORKS normal' in r['testOutput'] or 'PASS SESSION A FIRE normal' in r['testOutput'] or 'PASS SESSION A OPENING normal' in r['testOutput'] or 'PASS SESSION A ATTACK normal' in r['testOutput'] or 'PASS SESSION A SEARCH ordinary' in r['testOutput'] or 'PASS SESSION A DIRECT normal' in r['testOutput'] or 'PASS SESSION A ARMY normal' in r['testOutput'] or 'PASS SESSION A DEBATE ordinary' in r['testOutput'] or 'PASS SESSION B CAPACITY source0' in r['testOutput'] or 'PASS SESSION B LEGACY39 genuine' in r['testOutput']) and 'FAIL' not in r['testOutput']
            folder='session-a-map' if a.runner=='SessionAMapRepairInstrumentation' else 'uiux'
            b_folder='legacy39' if a.runner=='SessionBLegacy39Instrumentation' else 'direct' if a.runner=='SessionADirectRecruitmentPresentationInstrumentation' else 'search' if a.runner=='SessionASearchPresentationInstrumentation' else 'capacity' if a.runner=='SessionBCapacityInstrumentation' else 'governor' if a.runner=='SessionAArmyBudgetInstrumentation' else 'debate' if a.runner=='SessionADebatePresentationInstrumentation' else 'fieldworks'
            b_runner=a.runner in ['SessionBFieldworksInstrumentation','SessionAScenePresentationInstrumentation','SessionAFireFlowInstrumentation','SessionANativeDuelOpeningInstrumentation','SessionAAttackTaskInstrumentation','SessionADebatePresentationInstrumentation','SessionAArmyBudgetInstrumentation','SessionASearchPresentationInstrumentation','SessionADirectRecruitmentPresentationInstrumentation','SessionBCapacityInstrumentation','SessionBLegacy39Instrumentation']
            relative='session-b/'+b_folder if b_runner else folder+'/'+run_id
            run('pull','/sdcard/Android/data/'+PACKAGE+'/files/'+relative,str(out/'evidence'))
            # Preserve completed normal-game evidence before a longer capture wait.
            if a.audio_capture_rate:capture_support.collect(out,r,music_capture,a.menu_capture_seconds+30)
            if a.fresh_process_reopen:
                if a.runner not in ['SessionAMapRepairInstrumentation','SessionBFieldworksInstrumentation','SessionAScenePresentationInstrumentation','SessionAFireFlowInstrumentation','SessionANativeDuelOpeningInstrumentation','SessionAAttackTaskInstrumentation','SessionADebatePresentationInstrumentation','SessionAArmyBudgetInstrumentation','SessionASearchPresentationInstrumentation','SessionADirectRecruitmentPresentationInstrumentation','SessionBCapacityInstrumentation','SessionBLegacy39Instrumentation'] or not r['passed']:raise ValueError('Completed normal source/save workflow required before cold reopen')
                normal_pass=r['passed'];r['normalPassed']=normal_pass;r['passed']=False;r['stage']='cold-process-running';report_path.write_text(json.dumps(r,indent=2))
                expected=run('shell','sha256sum','/data/data/'+PACKAGE+'/files/auto.sg11').decode().split()[0]
                if b_runner:
                    expected_file='actual-source.sg11' if b_folder in ['capacity','governor'] else 'actual-mid.sg11' if b_folder in ['debate','search','direct','legacy39'] else 'actual-build.sg11'
                    expected_build=run('shell','sha256sum','/sdcard/Android/data/'+PACKAGE+'/files/session-b/'+b_folder+'/'+expected_file).decode().split()[0]
                    if expected_build!=expected:raise ValueError('B actual build and normal autosave differ')
                # Instrumentation may finish before a cold renderer submits.
                # Process identity comes from the exact Android target launch;
                # render submission is retained as a separate observation.
                prior_log=(out/'logcat.txt').read_text(errors='replace')
                old_evidence=target_process_evidence(prior_log)
                old_pid=old_evidence['pid'];cold_log_offset=len(prior_log)
                r['coldProcess']={'beforePid':old_pid,'beforeProcessEvidence':old_evidence,
                                  'expectedStartupSaveSha256':expected,'passed':False}
                report_path.write_text(json.dumps(r,indent=2))
                run('shell','am','force-stop',PACKAGE)
                with (out/'cold-instrumentation.txt').open('wb') as f:
                    run('shell','am','instrument','-w','-e','run',run_id+'_cold','-e','begin','0','-e','end','0','-e','expectedStartupSha',expected,'-e','suite','mediaAll16' if a.portrait_native>=0 else 'cold3D','-e','portraitNative',str(a.portrait_native),'-e','coldPortraitSource',a.begin if a.portrait_native>=0 else '-1','-e','mode','cold',PACKAGE+'.test/game.sanguo.mobile.'+a.runner,output=f,timeout=900)
                cold=(out/'cold-instrumentation.txt').read_text()
                cold_relative='session-b/'+b_folder+'-cold' if b_runner else 'session-a-map/'+run_id+'_cold'
                run('pull','/sdcard/Android/data/'+PACKAGE+'/files/'+cold_relative,str(out/'cold-evidence'))
                # The logcat reader is asynchronous: briefly await its launch
                # record without requiring an unrelated frame submission.
                deadline=time.monotonic()+5
                while True:
                    after_log=(out/'logcat.txt').read_text(errors='replace')[cold_log_offset:]
                    try:
                        new_evidence=target_process_evidence(after_log)
                        break
                    except ValueError:
                        if time.monotonic()>=deadline:raise
                        time.sleep(0.1)
                new_pid=new_evidence['pid']
                r['coldProcess']={'beforePid':old_pid,'afterPid':new_pid,'beforeProcessEvidence':old_evidence,'afterProcessEvidence':new_evidence,'differentPid':old_pid!=new_pid,'expectedStartupSaveSha256':expected,'passed':('PASS SESSION A OPENING COLD' in cold if a.runner=='SessionANativeDuelOpeningInstrumentation' else 'SESSION_A_MAP PASS' in cold if a.runner=='SessionAMapRepairInstrumentation' else ('PASS SESSION B LEGACY39 COLD' if b_folder=='legacy39' else 'PASS SESSION B CAPACITY COLD' if b_folder=='capacity' else ('PASS SESSION A FIRE COLD' if a.runner=='SessionAFireFlowInstrumentation' else ('PASS SESSION A DIRECT COLD' if b_folder=='direct' else 'PASS SESSION A SEARCH COLD' if b_folder=='search' else 'PASS SESSION A ATTACK COLD' if a.runner=='SessionAAttackTaskInstrumentation' else 'PASS SESSION A DEBATE COLD' if b_folder=='debate' else 'PASS SESSION A ARMY COLD' if b_folder=='governor' else 'PASS SESSION B FIELDWORKS COLD'))) in cold) and 'FAIL' not in cold and old_pid!=new_pid}
                r['passed']=normal_pass and r['coldProcess']['passed']

        except BaseException as interrupted:
            # Preserve real evidence even when the outer multi-source supervisor
            # stops before the instrumentation terminal bundle. Never count PASS.
            r['instrumentationInterrupted']={'type':type(interrupted).__name__,'error':str(interrupted),'passed':False}
            r['passed']=False
            if a.runner in ['SessionAMapRepairInstrumentation','UiUxInstrumentation']:
                folder='session-a-map' if a.runner=='SessionAMapRepairInstrumentation' else 'uiux'
                remote='/sdcard/Android/data/'+PACKAGE+'/files/'+folder+'/'+run_id
                try:
                    destination=out/'interrupted-evidence'
                    run('pull',remote,str(destination),timeout=180)
                    r['interruptedEvidence']={'path':str(destination),'files':[{'path':str(p.relative_to(destination)),'sha256':digest(p),'bytes':p.stat().st_size} for p in sorted(destination.rglob('*')) if p.is_file()],'scope':'Raw own fresh UUID evidence salvaged before rollback; incomplete runner/cold not accepted'}
                except Exception as salvage:r['interruptedEvidenceError']=str(salvage)
            report_path.write_text(json.dumps(r,indent=2))
            raise
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
            report_path.write_text(json.dumps(r,indent=2))
            try:restore(out,r)
            finally:
                video_stop_error=None
                if video_observer is not None:
                    # SIGTERM requests orderly stop in our host recorder. It
                    # checks its own encoder PID/unique target before SIGINT;
                    # never signals a game/native worker or clears app data.
                    if video_observer.poll() is None:video_observer.terminate()
                    try:video_observer.wait(timeout=60)
                    except subprocess.TimeoutExpired:
                        r['videoObservation']['orderlyStopTimedOut']=True
                        report_path.write_text(json.dumps(r,indent=2))
                        video_stop_error=RuntimeError('Own video recorder did not finish; no next install')
                    r['videoObservation']['exitCode']=video_observer.returncode
                    if video_observer.returncode not in (0,None):
                        video_stop_error=RuntimeError('Own video recording failed; no next install')
                    if video_observer.returncode==0:
                        try:
                            video_path=out/'video/video.json'
                            video_result=json.loads(video_path.read_text())
                            if video_result['apks']!=r['apks'] or not video_result['parts']:
                                raise ValueError('Missing exact-cohort recorded parts')
                            for part in video_result['parts']:
                                if 'error' in part or part['sha256']!=part['deviceSha256']:
                                    raise ValueError('Original recorded part capture/SHA failed')
                                if digest(pathlib.Path(part['path']))!=part['sha256']:
                                    raise ValueError('Original host recorded part changed')
                                if part.get('verifiedDevicePartRemovedAfterPull') and part['deviceSha256AfterPull']!=part['sha256']:
                                    raise ValueError('Own temporary removal lacked exact post-pull SHA')
                            r['videoObservation']['completedOriginalParts']=len(video_result['parts'])
                            r['videoObservation']['finalIndexSha256']=digest(video_path)
                            r['videoObservation']['captureLimitReachedBeforeRestoration']=video_result['captureLimitReachedBeforeRestoration']
                            if video_result['captureLimitReachedBeforeRestoration']:
                                raise ValueError('Recorder reached its cap before actual restoration')
                        except Exception as error:
                            r['videoObservation']['verificationError']=repr(error)
                            video_stop_error=RuntimeError('Raw video evidence incomplete; no next install')
                    video_log.close();report_path.write_text(json.dumps(r,indent=2))
                if worker_observer is not None:
                    # Stop only our read-only host observer if restoration itself
                    # fails. The device ownership lock and failure remain intact.
                    if r['stage']!='restored-verified' and worker_observer.poll() is None:
                        worker_observer.terminate()
                    try:worker_observer.wait(timeout=40)
                    except subprocess.TimeoutExpired:
                        worker_observer.terminate();worker_observer.wait(timeout=10)
                        r['workerObservation']['hostObserverStoppedAfterRestoration']=True
                    r['workerObservation']['exitCode']=worker_observer.returncode
                    worker_log.close();report_path.write_text(json.dumps(r,indent=2))
                if video_stop_error is not None:raise video_stop_error
                if (a.observe_workers and a.runner=='SessionAMapRepairInstrumentation'
                    and r['stage']=='restored-verified' and r.get('passed')
                    and r.get('coldProcess',{}).get('passed')):
                    # MapRepair writes actual phase CSVs. Other command runners
                    # retain their local meminfo/worker logs without fabricating
                    # missing CSV telemetry. Freeze the final session before the
                    # memory report hashes it; do not mutate that receipt after.
                    memory_report=out/'memory-evidence.json'
                    r['memoryEvidencePath']=str(memory_report)
                    report_path.write_text(json.dumps(r,indent=2))
                    with (out/'memory-evidence-driver.log').open('wb') as memory_log:
                        subprocess.run([sys.executable,str(pathlib.Path(__file__).with_name('audit_session_memory.py')),
                            '--session',str(out),'--output',str(memory_report)],
                            stdout=memory_log,stderr=subprocess.STDOUT,check=True)
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
