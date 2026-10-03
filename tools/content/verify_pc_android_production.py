#!/usr/bin/env python3
"""Run source production crews and saved policies against the exact installed APK on ART.

The DEX contains only named test/fixture classes. Production rules, runtime and
resources come from the installed APK. Never install/clear app data here; use
the existing installation harness first, with an idle explicitly owned serial.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import zipfile
from verify_pc_age_install import ROOT, PACKAGE, members


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run(args):
    output = args.output.resolve()
    if ROOT / 'out' not in output.parents:
        raise ValueError('Output must be a fresh directory inside project out/')
    output.mkdir(parents=True, exist_ok=False)
    sdk = ROOT / 'out/toolchain/android-sdk'
    adb = [str(sdk / 'platform-tools/adb'), '-s', args.serial]

    def command(*parts, timeout=60):
        return subprocess.run(adb + list(parts), check=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=timeout).stdout

    core_names=['PcProductionFlowTest','PcProductionCrewTest','PcDelayedCounterTest','PcDelayedProductionFlowTest','PcProductionSavePolicyTest']
    runtime_names=['PcProductionCrewSessionTest','PcDelayedProductionSessionTest']
    if getattr(args,'native_technique_policy',False):
        core_names.append('PcTechniquePointsTest');runtime_names.append('PcTechniquePointsSessionTest')
    if getattr(args,'technique_facts',False):runtime_names.append('TechniquePointsFactsTest')
    suites={ 'game.sanguo.core.'+name: ROOT/module/'build/classes/java/test/game/sanguo/core'/ (name+'.class') for module,names in [('core',core_names),('game-runtime',runtime_names)] for name in names }
    if getattr(args,'suites',None):
        wanted={'game.sanguo.core.'+name for name in args.suites}
        if not wanted.issubset(suites):raise ValueError('Requested suite is not in this exact probe configuration')
        suites={name:path for name,path in suites.items() if name in wanted}
    fixtures=[('core','CityCommandRewardsTest'),('core','CityActionPlanTest'),('core','ProductionPlanTest'),('game-runtime','CityActionSessionTest'),('game-runtime','ProductionSessionTest')]
    if getattr(args,'native_technique_policy',False):fixtures.extend([('core','PcDelayedProductionFlowTest'),('game-runtime','PcProductionCrewSessionTest')])
    inputs=list(suites.values())+[ROOT/module/'build/classes/java/test/game/sanguo/core'/ (name+'.class') for module,name in fixtures]
    inputs=list(dict.fromkeys(inputs))
    # Java17 enum switches/nested helpers are nest mates, not production rules.
    for test_class in list(inputs):
        inputs.extend(sorted(test_class.parent.glob(test_class.stem+'$*.class')))
    if not all(p.is_file() for p in inputs):
        raise ValueError('Compile both registered test suites before using the device')
    fixture_classes=output/'fixture-classes';fixture_classes.mkdir()
    fixture_name='PcTechniqueOpeningFixture' if getattr(args,'native_technique_policy',False) else 'PcProductionOpeningFixture'
    fixture_source=ROOT/'tools/content'/(fixture_name+'.java')
    subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javac'),'--release','17','-encoding','UTF-8','-cp',str(ROOT/'core/build/libs/core.jar'),'-d',str(fixture_classes),str(fixture_source)],check=True)
    inputs.append(fixture_classes/'game/sanguo/core'/(fixture_name+'.class'))
    expected = sha(args.apk.read_bytes())
    installed = command('shell', 'pm', 'path', PACKAGE).decode().strip().removeprefix('package:')
    if not installed.startswith('/data/app/') or '\n' in installed:
        raise ValueError('Expected one installed APK')
    if sha(command('exec-out', 'cat', installed, timeout=180)) != expected:
        raise ValueError('Installed APK differs from candidate')
    if command('shell', 'id', '-u').strip() != b'0':
        raise ValueError('Root required for full external user-file verification')
    command('shell', 'am', 'force-stop', PACKAGE)
    backup = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
    (output / 'user-before.tar').write_bytes(backup)
    original = members(backup)
    remote_backup = '/data/local/tmp/source-production-backup-' + str(time.time_ns()) + '.tar'
    command('push', str(output / 'user-before.tar'), remote_backup)
    report = dict(passed=False, serial=args.serial, apk_sha256=expected,
                  scope='Exact installed APK production rules/runtime on ART; UI gestures verified separately',
                  class_sources={str(p.relative_to(ROOT)): sha(p.read_bytes()) for p in inputs}, results=[])

    def save():
        (output / 'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    save()
    try:
        probe = output / 'probe.zip'
        cmd = [str(Path(os.environ['JAVA_HOME']) / 'bin/java'), '-cp', str(sdk / 'build-tools/35.0.0/lib/d8.jar'),
               'com.android.tools.r8.D8', '--min-api', '26', '--lib', str(sdk / 'platforms/android-35/android.jar')]
        for module in ('core', 'game-api', 'game-runtime'):
            cmd += ['--classpath', str(ROOT / module / 'build/libs' / (module + '.jar'))]
        cmd += ['--output', str(probe)] + [str(p) for p in inputs]
        with (output / 'd8.log').open('w') as log:
            subprocess.run(cmd, check=True, stdout=log, stderr=log)
        report['test_resources']={}
        with zipfile.ZipFile(probe,'a') as archive:
            for name in ['pc-production-native.tsv','pc-delayed-counter-native.tsv']:
                raw=(ROOT/'core/src/test/resources'/name).read_bytes();archive.writestr(name,raw);report['test_resources'][name]=sha(raw)
            if getattr(args,'native_technique_policy',False):
                for name in ['pc-technique-gain-native.tsv','legacy-production-v36/coalition-190-art.sg11']:
                    raw=(ROOT/'core/src/test/resources'/name).read_bytes();archive.writestr(name,raw);report['test_resources'][name]=sha(raw)
        report['probe_sha256'] = sha(probe.read_bytes())
        remote = '/data/local/tmp/source-production-' + report['probe_sha256'][:20] + '.zip'
        command('push', str(probe), remote)
        command('shell', 'chmod', '644', remote)
        if sha(command('exec-out', 'cat', remote)) != report['probe_sha256']:
            raise ValueError('Probe readback differs')
        for suite in suites:
            started = time.monotonic()
            process = subprocess.run(adb+['shell','env','CLASSPATH='+installed+':'+remote,
                                         'app_process','/system/bin',suite],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=300)
            result = process.stdout
            (output / (suite.rsplit('.', 1)[1] + '.log')).write_bytes(result)
            if process.returncode!=0:
                report['probe_error']=dict(suite=suite,exit_code=process.returncode,output=result.decode(errors='replace'))
                save();raise RuntimeError('Probe exited '+str(process.returncode)+'; original log preserved')
            if b'PASS' not in result:
                raise ValueError('Missing suite pass marker: ' + suite)
            report['results'].append(dict(suite=suite, seconds=round(time.monotonic() - started, 2), output=result.decode()))
            save()
        fixture_data=[]
        for n in range(2):
            remote_save='/data/local/tmp/source-production-opening-'+report['probe_sha256'][:20]+'-'+str(n)+'.sg11'
            process=subprocess.run(adb+['shell','env','CLASSPATH='+installed+':'+remote,'app_process','/system/bin','game.sanguo.core.'+fixture_name,remote_save],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=300)
            result=process.stdout
            (output/('new-opening-'+str(n)+'.log')).write_bytes(result)
            if process.returncode!=0:
                report['probe_error']=dict(suite=fixture_name,exit_code=process.returncode,output=result.decode(errors='replace'));save();raise ValueError('New opening probe failed')
            if b'PASS' not in result:raise ValueError('Missing new opening pass')
            raw=command('exec-out','cat',remote_save);(output/('new-opening-'+str(n)+'.sg11')).write_bytes(raw);fixture_data.append(raw)
        if fixture_data[0]!=fixture_data[1]:raise ValueError('New ART openings must be reproducible')
        report['new_project_opening']=dict(path=str(output/'new-opening-0.sg11'),sha256=sha(fixture_data[0]),bytes=len(fixture_data[0]),same_twice=True,official_identity_verified=False)
        report['rules_passed'] = True
    finally:
        after = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
        (output / 'user-after.tar').write_bytes(after)
        report['user_files_unchanged'] = members(after) == original
        if not report['user_files_unchanged']:
            command('shell', 'am', 'force-stop', PACKAGE)
            command('shell', 'tar', '-C', '/data/data/' + PACKAGE, '-xf', remote_backup)
            restored = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
            (output / 'user-restored.tar').write_bytes(restored)
            report['all_original_files_restored'] = all(members(restored).get(k) == v for k, v in original.items())
        report['installed_apk_unchanged'] = sha(command('exec-out', 'cat', installed, timeout=180)) == expected
        report['passed'] = bool(report.get('rules_passed') and report['user_files_unchanged'] and report['installed_apk_unchanged'])
        save()
    print(json.dumps(report, ensure_ascii=False))
    if not report['passed']:
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--suites',nargs='+',help='Run only named existing suites, retaining all their assertions; prior independent evidence remains separate')
    parser.add_argument('--native-technique-policy',action='store_true',help='Require v37 source rewards and genuine old v36 preservation on ART')
    parser.add_argument('--technique-facts',action='store_true',help='Also verify immutable commit/journal point facts from the installed candidate')
    run(parser.parse_args())
