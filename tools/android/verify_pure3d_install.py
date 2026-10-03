#!/usr/bin/env python3
"""Install a frozen candidate and run an existing Android acceptance runner, preserving user files.

Explicit rooted emulator serial; no clear-data, no source APK mutation, no other devices.
Use only after any other task on this serial has finished. Output must be a fresh directory.
"""
import argparse
import json
import re
import subprocess
import time
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"content"))
from verify_pc_age_install import ROOT, PACKAGE, sha, members


def run(args):
    # Reject missing candidates before touching any device or creating a backup.
    expected_test_package='game.sanguo.mobile.gridallocationprobe' if args.runner=='GridAllocationInstrumentation' else PACKAGE+'.test'
    if args.test_package!=expected_test_package:
        raise ValueError('Acceptance package does not match the selected runner; device untouched')
    apks = {str(p.resolve()):sha(p.read_bytes()) for p in (args.apk,args.test_apk)}
    campaign=None
    if bool(args.campaign_save)!=bool(args.campaign_sha256):
        raise ValueError('A temporary campaign requires both its source file and pinned SHA256')
    if args.campaign_save:
        campaign=args.campaign_save.read_bytes()
        if sha(campaign)!=args.campaign_sha256:
            raise ValueError('Temporary campaign differs from pinned source; device untouched')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    adb = [str(ROOT / 'out/toolchain/android-sdk/platform-tools/adb'), '-s', args.serial]
    def command(*parts, timeout=60):
        return subprocess.run(adb + list(parts), check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout).stdout
    if command('shell', 'id', '-u').strip() != b'0':
        raise ValueError('Root required for external restore; no installation attempted')
    command('shell', 'am', 'force-stop', PACKAGE)
    before = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
    (output / 'user-before.tar').write_bytes(before)
    original = members(before)
    remote = '/data/local/tmp/pc-flow-backup-' + str(time.time_ns()) + '.tar'
    command('push', str(output / 'user-before.tar'), remote)
    report = dict(serial=args.serial, runner=args.runner, test_package=args.test_package, arguments=args.argument,
                  device=command('shell', 'getprop').decode(),
                  apks=apks,
                  backup={k:dict(bytes=len(v),sha256=sha(v)) for k,v in original.items()}, passed=False)
    def save():
        (output / 'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    save()
    try:
        report['stage']='installation'
        save()
        # Cold-boot dex optimization can exceed the ordinary ADB query timeout.
        # Preserve each completed install log even if the following operation fails.
        if not args.reuse_installed:
            with (output / 'installation.txt').open('wb') as log:
                for apk in (args.apk,args.test_apk):
                    log.write(command('install','-r',str(apk.resolve()),timeout=180))
                    log.flush()
        report['reused_installed'] = args.reuse_installed
        test_path = command('shell','pm','path',args.test_package).decode().strip().removeprefix('package:')
        report['installed_test_sha256'] = sha(command('exec-out','cat',test_path,timeout=180))
        if report['installed_test_sha256'] != sha(args.test_apk.read_bytes()):
            raise ValueError('Installed test APK differs from frozen candidate')
        report['stage']='installed_readback'
        installed_path = command('shell','pm','path',PACKAGE).decode().strip().removeprefix('package:')
        report['installed_sha256'] = sha(command('exec-out','cat',installed_path,timeout=180))
        if report['installed_sha256'] != sha(args.apk.read_bytes()):
            raise ValueError('Installed APK differs from frozen candidate')
        if campaign is not None:
            # Reuse existing inodes: preserve app ownership/mode. Only these two
            # externally backed-up slots change, never libraries or app data reset.
            slots=('files/auto.sg11','files/manual3.sg11')
            if any(name not in original for name in slots):
                raise ValueError('Campaign override requires both existing backed-up slots')
            remote_campaign=remote+'.campaign'
            command('push',str(args.campaign_save.resolve()),remote_campaign)
            for name in slots:
                target='/data/data/'+PACKAGE+'/'+name
                command('shell','cat',remote_campaign,'>',target)
                if sha(command('exec-out','cat',target))!=args.campaign_sha256:
                    raise ValueError('Temporary campaign readback differs')
            report['temporary_campaign']=dict(path=str(args.campaign_save.resolve()),
                                              sha256=args.campaign_sha256,bytes=len(campaign),slots=slots)
        save()
        values=[]
        for value in args.argument:
            key,sep,text=value.partition('=')
            if not sep or not re.fullmatch(r'[A-Za-z0-9_]+',key) or not re.fullmatch(r'[A-Za-z0-9_.-]+',text):
                raise ValueError('Only simple instrumentation key=value arguments supported')
            values += ['-e',key,text]
        started=time.monotonic()
        report['stage']='instrumentation'
        save()
        try:
            with (output / 'instrumentation.txt').open('wb') as log:
                process=subprocess.run(adb+['shell','am','instrument','-w']+values+[args.test_package+'/game.sanguo.mobile.'+args.runner],stdout=log,stderr=subprocess.STDOUT,timeout=args.timeout)
            text=(output / 'instrumentation.txt').read_text()
            report.update(exit_code=process.returncode,passed=process.returncode==0 and args.pass_marker in text and 'FAIL' not in text)
        except subprocess.TimeoutExpired:
            report['timeout']=True
        finally:
            report['seconds']=round(time.monotonic()-started,2)
        report['stage']='finished'
    except Exception as error:
        report['exception']=dict(type=type(error).__name__,message=str(error))
        raise
    finally:
        command('shell','am','force-stop',PACKAGE)
        command('shell','tar','-C','/data/data/'+PACKAGE,'-xf',remote)
        current=members(command('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs'))
        for name in current.keys()-original.keys():
            # Only files created by this exclusive test run; preserve all original bytes.
            if not name.startswith(('files/','shared_prefs/')) or '..' in Path(name).parts:
                raise ValueError('Unexpected restore path '+name)
            command('shell','rm','--','/data/data/'+PACKAGE+'/'+name)
        restored=command('exec-out','tar','-C','/data/data/'+PACKAGE,'-cf','-','files','shared_prefs')
        (output / 'user-restored.tar').write_bytes(restored)
        files=members(restored)
        mismatches=[key for key,value in original.items() if files.get(key)!=value]
        report['restoration']=dict(all_original_files_byte_equal=not mismatches,mismatches=mismatches,
            added_files=sorted(files.keys()-original.keys()),auto_sha256=sha(files['files/auto.sg11']) if 'files/auto.sg11' in files else None)
        save()
        if mismatches:
            raise ValueError('User file restore mismatch: '+repr(mismatches))
    print(json.dumps({k:v for k,v in report.items() if k not in ('device','backup')},ensure_ascii=False))
    if not report['passed']:
        raise SystemExit(1)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial',required=True)
    parser.add_argument('--apk',type=Path,required=True)
    parser.add_argument('--test-apk',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--runner',choices=['SceneInstrumentation','GameSmokeRunner','UiUxInstrumentation','PcPresentationsInstrumentation','Pure3dInstrumentation','MapEditor67Instrumentation','GridAllocationInstrumentation'],required=True)
    parser.add_argument('--test-package',default=PACKAGE+'.test',help='Standalone acceptance package; main target remains fixed')
    parser.add_argument('--argument',action='append',default=[])
    parser.add_argument('--pass-marker',required=True)
    parser.add_argument('--timeout',type=int,default=1200)
    parser.add_argument('--reuse-installed',action='store_true',help='Skip installation only after exact main and test APK readback verification')
    parser.add_argument('--campaign-save',type=Path,help='Optional provenance-pinned normal campaign; original auto/manual3 restored in finally')
    parser.add_argument('--campaign-sha256',help='Required expected SHA256 paired with campaign-save')
    run(parser.parse_args())
