#!/usr/bin/env python3
"""Run installed editor acceptance while preserving its preexisting external artifacts.
The proven installer separately backs up/restores all private saves and preferences.
"""
import argparse,hashlib,json,subprocess,sys,time
from pathlib import Path
from verify_pc_age_install import ROOT,PACKAGE,members
p=argparse.ArgumentParser(description=__doc__)
for name in ['apk','test-apk','fixture','output']:p.add_argument('--'+name,required=True,type=Path)
p.add_argument('--serial',required=True);p.add_argument('--fixture-sha256',required=True);a=p.parse_args()
sha=lambda b:hashlib.sha256(b).hexdigest()
assert sha(a.fixture.read_bytes())==a.fixture_sha256,'Pinned editor input differs; device untouched'
json.loads(a.fixture.read_text());assert a.apk.is_file() and a.test_apk.is_file()
out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
adb=[str(ROOT/'out/toolchain/android-sdk/platform-tools/adb'),'-s',a.serial]
def cmd(*parts,timeout=60):return subprocess.run(adb+list(parts),check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=timeout).stdout
assert cmd('shell','id','-u').strip()==b'0'
parent='/sdcard/Android/data/'+PACKAGE+'/files';directory=parent+'/editor67'
try:exists=cmd('shell','stat','-c','%F',directory).strip()==b'directory'
except subprocess.CalledProcessError:exists=False
backup=cmd('exec-out','tar','-C',parent,'-cf','-','editor67') if exists else None
original=members(backup) if backup is not None else {}
report={'serial':a.serial,'external_directory':directory,'existed_before':exists,'fixture_sha256':a.fixture_sha256,'private_verification':'delegated to verify_pure3d_install','passed':False}
if backup is not None:(out/'external-before.tar').write_bytes(backup)
def save():(out/'results.json').write_text(json.dumps(report,indent=2)+'\n')
save();remote='/data/local/tmp/editor-external-'+str(time.time_ns())+'.tar'
if backup is not None:cmd('push',str(out/'external-before.tar'),remote)
try:
    cmd('shell','mkdir','-p',directory);cmd('push',str(a.fixture.resolve()),directory+'/fixture.json')
    assert sha(cmd('exec-out','cat',directory+'/fixture.json'))==a.fixture_sha256
    with (out/'installed-driver.log').open('wb') as log:
        run=subprocess.run([sys.executable,str(ROOT/'tools/android/verify_pure3d_install.py'),'--serial',a.serial,'--apk',str(a.apk.resolve()),'--test-apk',str(a.test_apk.resolve()),'--output',str(out/'installed'),'--runner','MapEditor67Instrumentation','--pass-marker','MAP_EDITOR67 ANDROID PASS','--timeout','600','--reuse-installed'],stdout=log,stderr=subprocess.STDOUT)
    report['installer_exit_code']=run.returncode
    if run.returncode==0:
        installed=json.loads((out/'installed/results.json').read_text());report['private_original_files_exact']=installed['restoration']['all_original_files_byte_equal'];report['installed_apk_sha256']=installed['installed_sha256'];report['ui_seconds']=installed['seconds']
finally:
    captured=cmd('exec-out','tar','-C',parent,'-cf','-','editor67');(out/'external-test-evidence.tar').write_bytes(captured)
    cmd('pull',directory,str(out/'evidence'))
    current=members(captured);generated=sorted(current.keys()-original.keys())
    if backup is not None:cmd('shell','tar','-C',parent,'-xf',remote)
    for name in generated:
        if not name.startswith('editor67/') or '..' in Path(name).parts:raise ValueError('Unexpected external path')
        # Retain bytes in evidence; remove only this exclusive test's new files.
        cmd('shell','rm','--',parent+'/'+name)
    restored=cmd('exec-out','tar','-C',parent,'-cf','-','editor67');(out/'external-restored.tar').write_bytes(restored)
    report['external_original_files_byte_equal']=members(restored)==original
    report['external_original_count']=len(original);report['test_generated_files']=generated
    report['passed']=bool(report.get('installer_exit_code')==0 and report.get('private_original_files_exact') and report['external_original_files_byte_equal']);save()
    assert report['external_original_files_byte_equal'],'Original external editor artifacts changed'
    if not exists:cmd('shell','rmdir',directory)
print(json.dumps(report))
if not report['passed']:raise SystemExit(1)
