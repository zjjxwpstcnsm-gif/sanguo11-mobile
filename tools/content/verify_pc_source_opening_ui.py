#!/usr/bin/env python3
"""Run actual officer/search/new-game widgets on a pinned APK with full user guards.

Uses the inherited real-widget runner. Restores every original external file
including libraries, while preserving new evidence directories. No data reset.
"""
import argparse
import hashlib
import json
import subprocess
import shlex
import os
import tarfile
from pathlib import Path
from types import SimpleNamespace
from verify_pc_installed_flow import run as flow

ROOT=Path(__file__).resolve().parents[2]
PACKAGE='game.sanguo.mobile.dev'


def archive_manifest(path):
    result={}
    with tarfile.open(path,mode='r|') as archive:
        for member in archive:
            if not member.isfile():
                continue
            if member.name.startswith('/') or '..' in Path(member.name).parts:
                raise ValueError('Unsafe backup path')
            stream=archive.extractfile(member);digest=hashlib.sha256()
            for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
            result[member.name]=dict(bytes=member.size,sha256=digest.hexdigest())
    return result


def run(args):
    args.output.mkdir(parents=True,exist_ok=False)
    guard=json.loads(args.source_guard.read_text())
    changed=[name for name,digest in guard.items() if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=digest]
    if changed:raise ValueError('APK source changed: '+repr(changed))
    lock=Path('/tmp/sanguo11-'+args.serial+'.lock')
    # The established5554 lock uses the project-wide serial alias.
    if args.serial=='emulator-5554':lock=Path('/tmp/sanguo11-emulator-5554.lock')
    if not lock.is_dir():raise ValueError('Exclusive serial lock missing')
    if json.loads((lock/'owner.json').read_text()).get('branch')!='codex/scenario-officer-restoration':
        raise ValueError('Serial lock belongs to another session')
    adb=[str(ROOT/'out/toolchain/android-sdk/platform-tools/adb'),'-s',args.serial]
    external='/sdcard/Android/data/'+PACKAGE
    def command(*parts,timeout=180):
        return subprocess.run(adb+list(parts),check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=timeout).stdout
    def backup(name):
        target=args.output/name
        with target.open('wb') as output:
            subprocess.run(adb+['exec-out','tar','-C',external,'-cf','-','files'],stdout=output,stderr=subprocess.PIPE,check=True,timeout=300)
        return archive_manifest(target)
    def current_hashes():
        lines=command('shell','cd '+shlex.quote(external)+' && find files -type f -exec sha256sum {} +',timeout=300).decode().splitlines()
        return {line.split('  ',1)[1]:line.split('  ',1)[0] for line in lines}
    command('shell','am','force-stop',PACKAGE)
    if args.external_backup:
        # Resume a completed host backup only after verifying every current file.
        original=archive_manifest(args.external_backup)
        current=current_hashes()
        if any(current.get(name)!=row['sha256'] for name,row in original.items()):
            raise ValueError('Existing external backup no longer matches device')
        os.link(args.external_backup,args.output/'external-before.tar')
    else:original=backup('external-before.tar')
    report=dict(serial=args.serial,apks={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [args.apk,args.test_apk]},
                externalOriginalFiles=original,passed=False,suites=[])
    def save():
        (args.output/'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    save()
    try:
        campaign_sha=hashlib.sha256(args.campaign_save.read_bytes()).hexdigest()
        for index,suite in enumerate(['officer-info'] if args.info_only else ['opening','general']):
            output=args.output/suite
            flow(SimpleNamespace(serial=args.serial,apk=args.apk,test_apk=args.test_apk,output=output,
                runner=('PcHealthInstrumentation' if getattr(args,'health_flow',False) else 'PcContestInstrumentation' if args.contest_flow else 'PcScenarioOpeningInstrumentation') if args.info_only else 'UiUxInstrumentation',
                test_package='game.sanguo.mobile.pcopeningprobe' if args.info_only else PACKAGE+'.test',
                argument=['suite='+suite,'run=officer-session1-'+args.output.name+'-'+suite,'reuseSlotSha='+campaign_sha,'sourceIndex='+str(args.source_index)]+(['evidenceId='+args.output.name] if getattr(args,'unique_evidence',False) else [])+(['sourceOpening=1'] if args.source_opening else [])+(['sourceResume=1'] if args.source_resume else [])+(['gaijiIdentity=1'] if getattr(args,'gaiji_identity',False) else [])+(['contestResume=1'] if args.contest_resume else []),
                pass_marker='PC SOURCE OPENING PASS' if args.info_only else 'UIUX PASS',timeout=1200,reuse_installed=index>0 or getattr(args,'reuse_installed',False),campaign_save=args.campaign_save,campaign_sha256=campaign_sha))
            report['suites'].append(json.loads((output/'results.json').read_text()));save()
        if args.source_opening:
            # Preserve the actual ART/new-menu result before restoring original
            # external evidence files that can share this historical directory.
            directory='session1-pc-opening-'+str(args.source_index)+('-'+args.output.name if getattr(args,'unique_evidence',False) else '')
            saved=command('exec-out','cat',external+'/files/'+directory+'/source-flow-final.sg11')
            target=args.output/'actual-source-flow-final.sg11';target.write_bytes(saved)
            report['actualSourceFlowSave']=dict(path=str(target.resolve()),sha256=hashlib.sha256(saved).hexdigest(),bytes=len(saved));save()
        report['passed']=True
    except BaseException as error:
        report['error']=dict(type=type(error).__name__,message=str(error));raise
    finally:
        command('shell','am','force-stop',PACKAGE)
        current=current_hashes() if args.info_only else {}
        already_exact=args.info_only and all(current.get(name)==row['sha256'] for name,row in original.items())
        if already_exact:
            restored={name:row for name,row in original.items()}
            for name,digest in current.items():
                if name not in restored:restored[name]=dict(sha256=digest)
        else:
            # Stream host backup directly; do not require another2GiB on userdata.
            with (args.output/'external-before.tar').open('rb') as source:
                subprocess.run(adb+['shell','-T','tar','-C',external,'-xf','-'],stdin=source,
                               stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True,timeout=300)
            restored=backup('external-restored.tar')
        mismatch=[name for name,row in original.items() if restored.get(name)!=row]
        report['externalRestoration']=dict(allOriginalFilesByteEqual=not mismatch,mismatches=mismatch,
            newEvidenceFiles=sorted(set(restored)-set(original)),originalFileCount=len(original))
        report['externalRestoration']['alreadyByteEqualVerifiedOnDevice']=bool(already_exact)
        save()
        if mismatch:raise ValueError('External library/user-file restoration mismatch: '+repr(mismatch))
    print(json.dumps(dict(passed=report['passed'],externalRestoration=report['externalRestoration'],
                          suites=[dict(runner=s['runner'],passed=s['passed'],seconds=s['seconds']) for s in report['suites']])))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial',required=True)
    for name in ('apk','test-apk','source-guard','output'):parser.add_argument('--'+name,type=Path,required=True)
    parser.add_argument('--external-backup',type=Path)
    parser.add_argument('--campaign-save',type=Path,required=True)
    parser.add_argument('--info-only',action='store_true')
    parser.add_argument('--reuse-installed',action='store_true',help='Verify exact installed main/test APK SHA before skipping redundant reinstall')
    parser.add_argument('--source-opening',action='store_true')
    parser.add_argument('--source-index',type=int,default=0)
    parser.add_argument('--source-resume',action='store_true')
    parser.add_argument('--contest-flow',action='store_true')
    parser.add_argument('--contest-resume',action='store_true')
    parser.add_argument('--unique-evidence',action='store_true')
    parser.add_argument('--health-flow',action='store_true');parser.add_argument('--gaiji-identity',action='store_true')
    run(parser.parse_args())
