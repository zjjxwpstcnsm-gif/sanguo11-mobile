#!/usr/bin/env python3
"""Fresh guarded single-source observation run after exact failed-case restore."""
from pathlib import Path
import json,subprocess,hashlib
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/search-observation240/source04-diagnostic'
def main():
    assert not OUT.exists();build=json.loads((DOC/'SEARCH_OBSERVATION_TEST_BUILD239.json').read_text());assert build['buildSuccessful'] and build['productionRawSourcesEqualTo165']
    for x in build['apks']:assert hashlib.sha256(Path(x['path']).read_bytes()).hexdigest()==x['sha256']
    changed=subprocess.check_output(['git','diff','--name-only',build['sourceRevision'],'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT,text=True).strip();assert not changed,changed
    inheritance=json.loads((DOC/'INHERITANCE.json').read_text());assert subprocess.check_output(['git','-C',inheritance['source'],'rev-parse','main'],text=True).strip()==inheritance['main']
    previous=ROOT/'out/session-a/registered-normal182/normal/remaining-normal-callers/source-04';state=json.loads((previous/'session.json').read_text());assert state['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in state['restoration'].values())
    report={'previousFailedRestoredCase':str(previous),'previousPassed':state['passed'],'newCohort':'SEARCH_OBSERVATION_TEST_BUILD239.json','newApks':build['apks'],'case':str(OUT),'stage':'preparing-full-backup','oldQueuesRestarted':False,'wholeGoalComplete':False}
    (DOC/'SEARCH_OBSERVATION240_LAUNCH.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    helper=DOC/'device_session.py';OUT.parent.mkdir(parents=True,exist_ok=True)
    with (OUT.parent/'backup-driver.log').open('w') as f:r=subprocess.run(['python3',str(helper),'reuse-backup','--output',str(OUT),'--previous',str(previous)],stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT.parent/'backup-driver.log').read_text()[-4000:]
    # Recheck frozen inputs after backup. A rejected cohort is restored, never
    # left holding a backup-only lock.
    try:
        changed=subprocess.check_output(['git','diff','--name-only',build['sourceRevision'],'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT,text=True).strip();assert not changed,changed
        for x in build['apks']:assert hashlib.sha256(Path(x['path']).read_bytes()).hexdigest()==x['sha256']
    except BaseException:
        subprocess.run(['python3',str(helper),'restore','--output',str(OUT)],check=True);raise
    game=next(x['path'] for x in build['apks'] if Path(x['path']).name=='app-debug.apk');test=next(x['path'] for x in build['apks'] if Path(x['path']).name=='app-debug-androidTest.apk')
    command=['python3',str(helper),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--reuse-installed','--observe-workers','--runner','SessionAMapRepairInstrumentation','--suite','mediaAll16','--begin','4','--end','5','--fresh-process-reopen','--search-diagnostics']
    report['stage']='single-source-diagnostic-running';report['command']=command;(DOC/'SEARCH_OBSERVATION240_LAUNCH.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    with (OUT.parent/'install-driver.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
    final=json.loads((OUT/'session.json').read_text());assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values())
    report['stage']='restored-verified';report['diagnosticPassed']=final.get('passed',False);report['helperExit']=r.returncode;report['rootCauseFixed']=False;(DOC/'SEARCH_OBSERVATION240_LAUNCH.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,ensure_ascii=False));raise SystemExit(r.returncode)
if __name__=='__main__':main()
