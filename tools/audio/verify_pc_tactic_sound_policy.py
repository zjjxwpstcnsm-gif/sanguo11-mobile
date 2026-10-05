#!/usr/bin/env python3
"""Compare live production numeric policy to original callback source vectors."""
import argparse,hashlib,json,subprocess
from pathlib import Path

def verify(source,output,java_home):
    if output.exists():raise ValueError('Fresh evidence directory required')
    raw=source.read_bytes();proof=json.loads(raw)
    if proof['sourceExecutableSha256']!='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb' or proof['checks']!=504:raise ValueError('Complete source chain required')
    rows=[r for r in proof['rows'] if r['orderTypeRaw']==4 and r['alreadyProducedChoiceRaw']==0 and r['record0Raw'] in [0,1] and r['soundIds']]
    if len(rows)!=8:raise ValueError('Original four action callbacks missing')
    output.mkdir(parents=True);vectors=output/'source.tsv';vectors.write_text('\n'.join('\t'.join(map(str,[r['record50Raw'],r['record0Raw'],r['soundIds'][0]])) for r in rows)+'\n')
    root=Path(__file__).resolve().parents[2];files=[root/'app/src/main/java/game/sanguo/mobile/PcTacticSoundPolicy.java',root/'app/src/test/java/game/sanguo/mobile/PcTacticSoundPolicyTest.java'];classes=output/'classes';classes.mkdir()
    for cmd,name in [([str(java_home/'bin/javac'),'-d',str(classes),*map(str,files)],'compile.log'),([str(java_home/'bin/java'),'-cp',str(classes),'game.sanguo.mobile.PcTacticSoundPolicyTest',str(vectors)],'test.log')]:
        run=subprocess.run(cmd,capture_output=True,text=True);(output/name).write_text(run.stdout+run.stderr);run.check_returncode()
    report=dict(status='PASS',sourceReportSha256=hashlib.sha256(raw).hexdigest(),sourceCallbackVectors=len(rows),normalEventBindingOrAndroidPlaybackProven=False,paths={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files});(output/'acceptance.json').write_text(json.dumps(report,indent=2)+'\n');print((output/'test.log').read_text().strip())

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);a=p.parse_args();verify(a.source,a.output,a.java_home)
