#!/usr/bin/env python3
"""Check committed source voice admission against all4020 original caller rows."""
import argparse,gzip,hashlib,json,subprocess
from pathlib import Path

def verify(source,output,java_home):
    if output.exists():raise ValueError('Fresh evidence directory required')
    raw=source.read_bytes();data=json.loads(gzip.decompress(raw) if source.suffix=='.gz' else raw)
    if data['checks']!=4020 or data['sourceExecutableSha256']!='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb':raise ValueError('Complete original caller required')
    root=Path(__file__).resolve().parents[2];output.mkdir(parents=True);vectors=output/'native.tsv';rows=[]
    for row in data['rows']:
        abilities=row['currentAbilityBytes'];expected=row['dispatch'][0]['voiceId'] if row['dispatch'] else -1
        rows.append([row['leaderNativeId'],row['profileRaw'],row['voiceTypeRaw'],*abilities[:3],row['statusRaw'],row['actor17cRaw'],int(row['audioAvailable']),abilities[3],expected])
    vectors.write_text('\n'.join('\t'.join(map(str,row)) for row in rows)+'\n')
    files=[root/'game-api/src/main/java/game/sanguo/api/StateToken.java']+[root/'app/src/main/java/game/sanguo/mobile'/name for name in ['MediaHashes.java','PortraitMediaIdentity.java','PcVoicePolicy.java','PcVoiceDirective.java']]+[root/'app/src/test/java/game/sanguo/mobile/PcCommittedVoiceValidityTest.java']
    classes=output/'classes';classes.mkdir()
    for command,name in [([str(java_home/'bin/javac'),'-d',str(classes),*map(str,files)],'compile.log'),([str(java_home/'bin/java'),'-cp',str(classes),'game.sanguo.mobile.PcCommittedVoiceValidityTest',str(vectors)],'test.log')]:
        result=subprocess.run(command,capture_output=True,text=True);(output/name).write_text(result.stdout+result.stderr);result.check_returncode()
    report=dict(status='PASS',nativeReportSha256=hashlib.sha256(raw).hexdigest(),vectors=len(rows),paths={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files},scope='Host actual production actor-validity/directive admission, no normal voice or Android playback assertion')
    (output/'acceptance.json').write_text(json.dumps(report,indent=2)+'\n');print((output/'test.log').read_text().strip())

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args();verify(a.source,a.output,a.java_home)
