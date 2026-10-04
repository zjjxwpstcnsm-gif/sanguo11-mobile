#!/usr/bin/env python3
"""Compare existing app ability policy to actual full original unit caller dispatches."""
import argparse,hashlib,json,subprocess
from pathlib import Path

def verify(report,output,java_home):
    if output.exists():raise ValueError('Fresh evidence directory required')
    root=Path(__file__).resolve().parents[2];raw=report.read_bytes();source=json.loads(raw)
    if source['sourceExecutableSha256']!='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb' or source['checks']!=4020:raise ValueError('Missing complete source caller evidence')
    rows=[r for r in source['rows'] if r['dispatch']]
    if len(rows)!=3990:raise ValueError('Incomplete original dispatch vectors')
    output.mkdir(parents=True);vectors=output/'original-unit-voice.tsv'
    vectors.write_text('\n'.join('\t'.join(map(str,[r['profileRaw'],r['voiceTypeRaw'],*r['currentAbilityBytes'],r['dispatch'][0]['voiceId']])) for r in rows)+'\n')
    files=[root/'app/src/main/java/game/sanguo/mobile/PcVoicePolicy.java',root/'app/src/test/java/game/sanguo/mobile/PcUnitVoicePolicyTest.java'];classes=output/'classes';classes.mkdir()
    compiled=subprocess.run([str(java_home/'bin/javac'),'-d',str(classes),*map(str,files)],capture_output=True,text=True);(output/'compile.log').write_text(compiled.stdout+compiled.stderr);compiled.check_returncode()
    tested=subprocess.run([str(java_home/'bin/java'),'-cp',str(classes),'game.sanguo.mobile.PcUnitVoicePolicyTest',str(vectors)],capture_output=True,text=True);(output/'test.log').write_text(tested.stdout+tested.stderr);tested.check_returncode()
    if 'PASS full unit caller voice comparisons=3990' not in tested.stdout:raise AssertionError('Missing full original comparison acceptance')
    proof=dict(status='PASS',nativeReportSha256=hashlib.sha256(raw).hexdigest(),nativeCallerChecks=4020,productionComparisons=3990,
        paths={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files},limits=['No normal event/action-to-profile or installed voice playback claim.','Silent/invalid source cases are covered by native caller, not manufactured as valid host playback.'])
    (output/'acceptance.json').write_text(json.dumps(proof,indent=2)+'\n');print(tested.stdout.strip())

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('report',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args();verify(a.report,a.output,a.java_home)
